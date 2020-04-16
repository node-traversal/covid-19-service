package io.nodetraversal.poi;

import com.google.common.collect.ImmutableList;
import io.nodetraversal.poi.examples.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ReflectionTableParserTest {

    private static final String XLSX = "CaseCountData.xlsx";
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final ReflectionTableParser<Cases> casesTableParser = new ReflectionTableParser<>(
            Cases.class,
            "Cases and Fatalities",
            ImmutableList.of("No.", "County", "Positive", "Fatalities"),
            1
    );

    private final ReflectionTableParser<Trends> trendsTableParser = new ReflectionTableParser<>(
            Trends.class,
            "Trends",
            ImmutableList.of(
                    "Date",
                    "Cumulative Cases",
                    "Cumulative Fatalities"
            ),
            2
    );

    private <T> List<T> getData(String resourceName, ReflectionTableParser<T> parser) {
        ClassPathResource resource = new ClassPathResource(resourceName);

        try (InputStream inputStream = resource.getInputStream()) {
            return parser.parse(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void shouldFailWhenNoDefaultConstructor() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("NoDefaultConstructor is missing a default constructor"));

        new ReflectionTableParser<>(
                NoDefaultConstructor.class,
                "Cases and Fatalities",
                ImmutableList.of("No."),
                1
        );
    }

    @Test
    public void shouldFailWhenNoGetterMatchesExpectedColumn() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("Trends did not have the required getter: getNo"));

        new ReflectionTableParser<>(
                Trends.class,
                "Cases and Fatalities",
                ImmutableList.of("No."),
                1
        );
    }

    @Test
    public void shouldFailWhenNoSetterMatchesExpectedColumn() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("SetterMismatch did not have the required setter: setNo(String)"));

        new ReflectionTableParser<>(
                SetterMismatch.class,
                "Cases and Fatalities",
                ImmutableList.of("No."),
                1
        );
    }

    @Test
    public void shouldFailWhenHeadersDoNotMatchExpected() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("Invalid column at index: 0, expected: No., "
                + "found: COVID-19 Positive Cases and Fatalities by County as of 4/15 at 10:45AM CST"));

        ReflectionTableParser<Cases> parser = new ReflectionTableParser<>(
            casesTableParser.getType(),
            casesTableParser.getSheetName(),
            casesTableParser.getExpectedColumns(),
            0
        );

        getData(XLSX, parser);
    }

    @Test
    public void shouldFailWhenSheetNotFound() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("Could not find sheet: Cases and Fatalities, "
                + "found: [Bad Cases, COVID-19 Fatalities]"));

        getData("wrong sheet name.xls", casesTableParser);
    }

    @Test
    public void shouldReadEveryRow() {
        List<Cases> cases = getData(XLSX, casesTableParser);

        assertEquals("Anderson", cases.get(0).getCounty());
        assertEquals("Zapata", cases.get(cases.size() - 1).getCounty());
    }

    @Test
    public void shouldReadEveryRowForAlternateHeaderRow() {
        SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");

        List<Trends> trends = getData(XLSX, trendsTableParser);

        assertEquals("03-04-2020", format.format(trends.get(0).getDate()));
        assertEquals("04-15-2020", format.format(trends.get(trends.size() - 1).getDate()));
    }

    @Test
    public void shouldMatchDataInSpreadsheetForCasesData() {
        List<Cases> cases = getData(XLSX, casesTableParser);

        Cases bexar = cases.get(11);
        assertEquals("Bexar", bexar.getCounty());
        assertEquals(Integer.valueOf(12), bexar.getNo());
        assertEquals(Integer.valueOf(815), bexar.getPositive());
        assertEquals(Integer.valueOf(33), bexar.getFatalities());
    }

    @Test
    public void shouldMatchDataInSpreadsheetForCasesTrends() {
        List<Trends> trends = getData(XLSX, trendsTableParser);

        SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");
        Trends trend = trends.get(13);
        assertEquals("03-17-2020", format.format(trend.getDate()));
        assertEquals(Integer.valueOf(63), trend.getCumulativeCases());
        assertEquals(Integer.valueOf(1), trend.getCumulativeFatalities());
    }

    @Test
    public void shouldIgnoreFormulasUntilSupported() {
        List<String> headers = new ArrayList<>(trendsTableParser.getExpectedColumns());
        headers.add("Daily New Cases");

        final ReflectionTableParser<TrendsWithFormula> parser = new ReflectionTableParser<>(
                TrendsWithFormula.class,
                trendsTableParser.getSheetName(),
                headers,
                trendsTableParser.getHeaderRowIndex()
        );

        List<TrendsWithFormula> data = getData(XLSX, parser);
        assertNull(data.get(0).getDailyNewCases());
        assertNull(data.get(1).getDailyNewCases());
    }
}
