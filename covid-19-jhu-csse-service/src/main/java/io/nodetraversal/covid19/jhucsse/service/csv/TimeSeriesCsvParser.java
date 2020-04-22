package io.nodetraversal.covid19.jhucsse.service.csv;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import io.nodetraversal.covid19.jhucsse.service.model.TimeSeriesGroup;
import io.nodetraversal.poi.ConnectionFailedException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
// TODO lots of duplication with ReflectionTableParser
// PRIORITY: LOW: this data is largely more comprehensive as compared to texas gov.
//                 ReflectionTableParser will probably be shelved ...
public class TimeSeriesCsvParser<T extends TimeSeries> {

    @Getter private final String sheetName;
    @Getter private final Class<T> type;
    @Getter private final List<String> expectedColumns;
    @Getter private final int headerRowIndex;

    private final Constructor<T> constructor;
    private final List<Method> setters;

    public TimeSeriesCsvParser(Class<T> type, List<String> expectedColumns, int headerRowIndex) {
        this.sheetName = type.getSimpleName();
        this.type = type;
        this.expectedColumns = ImmutableList.copyOf(expectedColumns);
        this.headerRowIndex = headerRowIndex;

        Map<String, Method> getterMap = getAccessors(type, "get");
        Map<String, Method> setterMap = getAccessors(type, "set");

        final Function<String, Method> columnToMethod = it -> {
            // TODO form a single regex the does all the replacements
            // PRIORITY: LOW: invoked only once per JVM start
            String getterName = "get" + StringUtils.capitalize(it
                    .replace(".", ""))
                    .replace("_", "")
                    .replace(" ", "");
            String setterName = "set" + StringUtils.capitalize(it
                    .replace(".", ""))
                    .replace("_", "")
                    .replace(" ", "");

            Method getter = getterMap.get(getterName);
            if (getter == null) {
                throw new IllegalArgumentException(type.getSimpleName()
                        + " did not have the required getter: " + getterName + ", found: " + getterMap.keySet());
            }

            Method setter = setterMap.get(setterName);
            if (setter == null) {
                throw new IllegalArgumentException(type.getSimpleName()
                        + " did not have the required setter: " + setterName + ", found: " + setterMap.keySet());
            }

            return setter;
        };

        try {
            constructor = type.getConstructor();
            setters = ImmutableList.copyOf(expectedColumns
                    .stream()
                    .map(columnToMethod)
                    .collect(Collectors.toList()));
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(type.getSimpleName() + " is missing a default constructor");
        }
    }

    private Map<String, Method> getAccessors(Class<T> type, String prefix) {
        Map<String, Method> methodMap = new HashMap<>();

        for (Method method : type.getMethods()) {
            String name = method.getName();
            AccessorAlias alias = method.getAnnotation(AccessorAlias.class);

            if (method.getName().startsWith(prefix)) {
                if (alias != null && !alias.alias().isEmpty()) {
                    name = alias.alias();
                }
                methodMap.put(name, method);
            }
        }

        return ImmutableMap.copyOf(methodMap);
    }

    public TimeSeriesGroup<T> fetch(String url) {
        HttpEntity responseEntity = null;

        try (CloseableHttpClient httpclient =
                     HttpClients.custom()
                             .build()) {
            try (CloseableHttpResponse response = httpclient.execute(new HttpGet(url))) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new ConnectionFailedException(url, response.getStatusLine().getStatusCode());
                }
                responseEntity = response.getEntity();
                if (responseEntity == null) {
                    throw new IllegalStateException(url + " did not return an entity");
                }

                return parse(responseEntity.getContent());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            EntityUtils.consumeQuietly(responseEntity);
        }
    }
    public TimeSeriesGroup<T> parse(InputStream inputStream) throws IOException {
        try {
            CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream));
            List<String[]> rows = csvReader.readAll();

            return parse(rows);
        } catch (CsvException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public TimeSeriesGroup<T> parse(List<String[]> rows) {
        List<T> items = new ArrayList<>();

        int rowCount = rows.size();
        int firstDataRow = headerRowIndex + 1;

        if (rowCount < (firstDataRow + 1)) {
            throw new IllegalArgumentException(sheetName + " does not have enough rows, "
                    + "expected: " + (firstDataRow + 1) +  ", found: " + rowCount);
        }

        SimpleDateFormat srcDateFormat = createDateFormat();
        SimpleDateFormat isoDate = new SimpleDateFormat("yyyy-MM-dd");

        String[] headerRow = rows.get(headerRowIndex);
        List<String> dates = validateHeaders(headerRow, srcDateFormat, isoDate);

        for (int rowIndex = firstDataRow; rowIndex < rowCount; rowIndex++) {
            T instance = readRow(rows, rowIndex);

            if (instance == null) {
                break;
            }
            items.add(instance);
        }

        return new TimeSeriesGroup<T>(dates, items);
    }

    public SimpleDateFormat createDateFormat() {
        return new SimpleDateFormat("M/d/yy");
    }

    private T readRow(List<String[]> rows, int rowIndex) {
        List<Integer> values = new ArrayList<>();

        T instance = newInstance();

        String[] row = rows.get(rowIndex);
        int cellCount = row.length;
        if (isBlankRow(row)) {
            if (log.isInfoEnabled()) {
                log.info("{} is stopping at row: {} due to empty row", sheetName, rowIndex);
            }
            return null;
        }

        int minColumns = Math.max(cellCount, expectedColumns.size());
        int addedCount = 0;
        for (int cellIndex = 0; cellIndex < minColumns; cellIndex++) {
            String cell = row[cellIndex];
            if (!isValid(cell)) {
                if (log.isInfoEnabled()) {
                    log.info("{} is stopping at row: {} due to blank/error row-cell", sheetName, rowIndex);
                }
                break;
            }
            if (cellIndex < expectedColumns.size()) {
                addCell(instance, setters.get(cellIndex), cell);
                addedCount++;
            } else {
                values.add(Integer.valueOf(cell));
                addedCount++;
            }
        }

        if (addedCount == 0) {
            if (log.isInfoEnabled()) {
                log.info("{} is stopping at row: {} due to empty row", sheetName, rowIndex);
            }
            return null;
        }

        instance.setValues(values);

        return instance;
    }

    private boolean isBlankRow(String[] row) {
        return row == null || row.length <= 0;
    }


    private void addCell(Object instance, Method setter, String value) {
        try {
            Class<?> setterType = setter.getParameters()[0].getType();
            if (String.class.equals(setterType)) {
                setter.invoke(instance, value);
            } else if (Double.class.equals(setterType)) {
                setter.invoke(instance, Double.valueOf(value));
            } else if (Integer.class.equals(setterType)) {
                setter.invoke(instance, Integer.valueOf(value));
            } else if (Boolean.class.equals(setterType)) {
                setter.invoke(instance, Boolean.valueOf(value));
            } else if (Date.class.equals(setterType)) {
                throw new UnsupportedOperationException("TODO");
            } else  {
                log.warn(instance.getClass().getSimpleName()
                        + "." + setter.getName() + " did not support: " + setterType);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(instance.getClass() + "." + setter.toString() + " not accessible", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(instance.getClass() + "." + setter.toString()
                    + " could not be applied to value");
        }
    }

    private boolean isValid(String cell) {
        return true;
    }

    private T newInstance() {
        try {
            return constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not create a new instance of: " + type, e);
        }
    }

    private Date toDate(String date, DateFormat srcDateFormat) {
        try {
            return srcDateFormat.parse(date);
        } catch (ParseException e) {
            return null;
        }
    }

    private List<String> validateHeaders(String[] headerRow, DateFormat srcDateFormat, DateFormat isoDate) {
        int headerSize = headerRow.length;
        List<String> remainingHeaders = new ArrayList<>();
        for (int i = 0; i < Math.max(headerSize, expectedColumns.size()); i++) {
            String headerValue = headerRow[i];
            if (i < expectedColumns.size()) {
                String expectedColumn = expectedColumns.get(i);
                if (!expectedColumn.equals(headerValue)) {
                    throw new IllegalArgumentException("Invalid column at index: " + i + ", expected: " + expectedColumn
                            +  ", found: " + headerValue);
                }
            } else {
                Date date = toDate(headerValue, srcDateFormat);
                if (date != null) {
                    remainingHeaders.add(isoDate.format(date));
                } else {
                    throw new IllegalArgumentException("Invalid column at index: " + i + ", expected date"
                            +  ", found: " + headerValue);
                }

            }
        }

        return ImmutableList.copyOf(remainingHeaders);
    }

}
