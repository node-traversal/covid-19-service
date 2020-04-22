package io.nodetraversal.covid19.jhucsse.service;

import com.google.common.collect.ImmutableList;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import io.nodetraversal.covid19.jhucsse.service.csv.TimeSeriesCsvParser;
import io.nodetraversal.covid19.jhucsse.service.model.LocationTimeSeries;
import io.nodetraversal.covid19.jhucsse.service.csv.TimeSeries;
import io.nodetraversal.covid19.jhucsse.service.model.TimeSeriesGroup;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class TimeSeriesCsvParserTest {

    private final TimeSeriesCsvParser<LocationTimeSeries> defaultParser = new TimeSeriesCsvParser<>(
            LocationTimeSeries.class,
            ImmutableList.of(
                    "UID", "iso2", "iso3", "code3", "FIPS", "Admin2", "Province_State", "Country_Region",
                    "Lat", "Long_", "Combined_Key"),
            0
    );

    private <T extends TimeSeries> TimeSeriesGroup<T> read(String resourceName, TimeSeriesCsvParser<T> parser) {
        ClassPathResource resource = new ClassPathResource(resourceName);

        try (InputStream inputStream = resource.getInputStream()) {
            CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream));
            List<String[]> rows = csvReader.readAll();

            return parser.parse(rows);
        } catch (IOException | CsvException exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    @Test
    public void shouldReadEveryRow() {
        TimeSeriesGroup<LocationTimeSeries> group = read("cases.csv", defaultParser);
        List<LocationTimeSeries> series = group.getSeries();

        Assert.assertEquals(group.getDates().get(0), "2020-01-22");
        Assert.assertEquals(group.getDates().get(group.getDates().size() - 1), "2020-04-18");

        LocationTimeSeries entry = series.get(0);
        List<Integer> values = entry.getValues();
        Assert.assertEquals(entry.getCombinedKey(), "American Samoa, US");
        Assert.assertEquals(entry.getCounty(), "");
        Assert.assertEquals(entry.getUID(), "16");
        Assert.assertEquals(values.get(0), Integer.valueOf(-1));
        Assert.assertEquals(values.get(values.size() - 1), Integer.valueOf(0));

        entry = series.get(10);
        values = entry.getValues();
        Assert.assertEquals(entry.getCombinedKey(), "Bullock, Alabama, US");
        Assert.assertEquals(entry.getCounty(), "Bullock");
        Assert.assertEquals(entry.getUID(), "84001011");
        Assert.assertEquals(values.get(0), Integer.valueOf(0));
        Assert.assertEquals(values.get(values.size() - 1), Integer.valueOf(9));

        entry = series.get(series.size() - 1);
        values = entry.getValues();
        Assert.assertEquals(entry.getCombinedKey(), "Denton, Texas, US");
        Assert.assertEquals(entry.getCounty(), "Denton");
        Assert.assertEquals(entry.getUID(), "84048121");
        Assert.assertEquals(values.toString(), "[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0"
            + ", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0"
            + ", 0, 1, 1, 5, 9, 15, 24, 30, 30, 38, 51, 83, 137, 148, 165, 165, 206, 206, 254, 273, 273, 304, 337, "
            + "366, 366, 426, 426, 454, 474, 507, 507, 521, 547, 564, 585]");
    }
}