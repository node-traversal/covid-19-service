package io.nodetraversal.covid19.texascases.service.legacy;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;

public class CasesSpreadsheetParserTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private LegacyCovidStatistics getStats(String resourceName) {
        ClassPathResource resource = new ClassPathResource(resourceName);

        try (InputStream inputStream = resource.getInputStream()) {
            LegacyCasesSpreadsheetParser parser = new LegacyCasesSpreadsheetParser();

            Workbook workbook = WorkbookFactory.create(inputStream);
            return parser.parse(workbook);

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void shouldDetectInvalidCountyColumnHeader() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("Expected A3 value: County Name, found: County Name1"));
        getStats("wrong county column.xls");
    }

    @Test
    public void shouldDetectInvalidPopulationColumnHeader() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("Expected B3 value: Population, found: PopulationX"));
        getStats("wrong population column.xls");
    }

    @Test
    public void shouldDetectDateColumnsNotMatchingDateRegexColumnHeader() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("Expected date value to be a date at column index: 2, found: 03-X4"));
        getStats("wrong date column.xls");
    }

    @Test
    public void shouldDetectInvalidCasesSheetName() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("Expected sheet0 to have name: COVID-19 Cases, found: Bad Cases"));
        getStats("wrong sheet name.xls");
    }

    @Test
    public void populationsShouldMatchSpreadsheet() {
        LegacyCovidStatistics statistics = getStats("Texas COVID-19 Case Count.xlsx");

        assertEquals(Integer.valueOf(2639966), statistics.getPopulationDataByCounty().get("Dallas"));
        assertEquals(Integer.valueOf(62245), statistics.getPopulationDataByCounty().get("Anderson"));
        assertEquals(Integer.valueOf(12895), statistics.getPopulationDataByCounty().get("Zavala"));
    }

    @Test
    public void newCasesShouldMatchSpreadsheet() {
        LegacyCovidStatistics statistics = getStats("Texas COVID-19 Case Count.xlsx");

        assertEquals("[0, 0, 0, 0, 2, 1, 0, 5, 0, 1, 6, 5, 2, 8, 0, 3, 98, 38, 134, 64, 0, 72, 49, 61, 82, "
                + "100, 100, 90, 94, 97, 43]", statistics.getNewCasesByCounty().get("Dallas").toString());
    }

    @Test
    public void datesShouldMatchSpreadsheet() {
        LegacyCovidStatistics statistics = getStats("Texas COVID-19 Case Count.xlsx");

        assertEquals("[03-04-2020, 03-05-2020, 03-06-2020, 03-09-2020, 03-10-2020, 03-11-2020, 03-12-2020, "
                + "03-13-2020, 03-15-2020, 03-16-2020, 03-17-2020, 03-18-2020, 03-19-2020, 03-20-2020, 03-21-2020, "
                + "03-22-2020, 03-23-2020, 03-24-2020, 03-25-2020, 03-26-2020, 03-27-2020, 03-28-2020, 03-29-2020, "
                + "03-30-2020, 03-31-2020, 04-01-2020, 04-02-2020, 04-03-2020, 04-04-2020, 04-05-2020, 04-06-2020, "
                + "04-07-2020]", statistics.getDates().toString());
    }
}
