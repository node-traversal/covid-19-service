package io.nodetraversal.poi;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @param <T>
 */
@Slf4j
public class ReflectionTableParser<T> implements TableParser<T> {

    @Getter private final String sheetName;
    @Getter private final Class<T> type;
    @Getter private final List<String> expectedColumns;
    @Getter private final int headerRowIndex;

    private final Constructor<T> constructor;
    private final List<Method> setters;

    public ReflectionTableParser(Class<T> type, String sheetName, List<String> expectedColumns, int headerRowIndex) {
        this.sheetName = sheetName;
        this.type = type;
        this.expectedColumns = ImmutableList.copyOf(expectedColumns);
        this.headerRowIndex = headerRowIndex;

        final Function<String, Method> columnToMethod = it -> {
            String getterName = "get" + StringUtils.capitalize(it.replace(".", "")).replace(" ", "");
            String setterName = "set" + StringUtils.capitalize(it.replace(".", "")).replace(" ", "");

            Method getter;
            try {
                getter = type.getMethod(getterName);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(type.getSimpleName()
                        + " did not have the required getter: " + getterName);
            }

            try {
                return type.getMethod(setterName, getter.getReturnType());
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(type.getSimpleName() + " did not have the required setter: "
                       + setterName + "(" + getter.getReturnType().getSimpleName() + ")");
            }
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

    @Override
    public List<T> parse(InputStream io) {
        try {
            Workbook wb = WorkbookFactory.create(io);
            return parse(wb);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<T> parse(Workbook workbook) {
        List<T> items = new ArrayList<>();

        Sheet sheet = findSheet(workbook, sheetName);

        Row headerRow = sheet.getRow(headerRowIndex);
        validateHeaders(headerRow);

        int rowCount = sheet.getLastRowNum();

        for (int rowIndex = headerRowIndex + 1; rowIndex < rowCount; rowIndex++) {
            T instance = readRow(sheet, rowIndex);

            if (instance == null) {
                break;
            }
            items.add(instance);
        }

        return items;
    }

    private T readRow(Sheet sheet, int rowIndex) {
        T instance = newInstance();

        Row row = sheet.getRow(rowIndex);
        if (row == null || row.getLastCellNum() == -1) {
            if (log.isInfoEnabled()) {
                log.info("{} is stopping at row: {} due to empty row", sheetName, rowIndex);
            }
            return null;
        }

        int index = Math.min(row.getLastCellNum(), expectedColumns.size());
        int addedCount = 0;
        for (int cellIndex = 0; cellIndex < index; cellIndex++) {
            Cell cell = row.getCell(cellIndex);
            if (cell.getCellType() == CellType.BLANK || cell.getCellType()  == CellType.ERROR) {
                if (log.isInfoEnabled()) {
                    log.info("{} is stopping at row: {} due to blank/error row-cell", sheetName, rowIndex);
                }
                break;
            }
            addCell(instance, setters.get(cellIndex), cell);
            addedCount++;
        }

        if (addedCount == 0) {
            if (log.isInfoEnabled()) {
                log.info("{} is stopping at row: {} due to empty row", sheetName, rowIndex);
            }
            return null;
        }

        return instance;
    }

    private void validateHeaders(Row headerRow) {
        int headerSize = headerRow.getLastCellNum();

        for (int i = 0; i < Math.min(headerSize, expectedColumns.size()); i++) {
            String headerValue = headerRow.getCell(i).getStringCellValue();
            String expectedColumn = expectedColumns.get(i);

            if (!expectedColumn.equals(headerValue)) {
                throw new IllegalArgumentException("Invalid column at index: " + i + ", expected: " + expectedColumn
                       +  ", found: " + headerValue);
            }
        }
    }

    private T newInstance() {
        try {
            return constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not create a new instance of: " + type, e);
        }
    }

    private void addCell(Object instance, Method setter, Cell cell) {
        try {
            CellType cellType = cell.getCellType();
            Class<?> setterType = setter.getParameters()[0].getType();

            if (CellType.NUMERIC.equals(cellType)) {
                Double value = cell.getNumericCellValue();

                if (Double.class.equals(setterType)) {
                    setter.invoke(instance, value);
                } else if (Integer.class.equals(setterType)) {
                    setter.invoke(instance, value.intValue());
                } else if (Date.class.equals(setterType)) {
                    setter.invoke(instance, cell.getDateCellValue());
                } else {
                    throw new UnsupportedOperationException("Numeric type not supported: "
                            + instance.getClass().getSimpleName()
                            + "." + setter.getName() + "(" + setterType + ")");
                }
            } else if (CellType.STRING.equals(cellType)) {
                if (String.class.equals(setterType)) {
                    setter.invoke(instance, cell.getStringCellValue());
                }
            } else if (CellType.BOOLEAN.equals(cellType)) {
                setter.invoke(instance, cell.getBooleanCellValue());
            } else  {
                log.warn(instance.getClass().getSimpleName()
                        + "." + setter.getName() + " did not support: " + cellType);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(instance.getClass() + "." + setter.toString() + " not accessible", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(instance.getClass() + "." + setter.toString()
                    + " could not be applied to value");
        }
    }

    private Sheet findSheet(Workbook wb, String name) {
        Set<String> names = new HashSet<>();
        for (Sheet sheet : wb) {
            if (name.equals(sheet.getSheetName())) {
                return sheet;
            }
            names.add(sheet.getSheetName());
        }

        throw new IllegalArgumentException("Could not find sheet: " + name + ", found: " + names);
    }
}
