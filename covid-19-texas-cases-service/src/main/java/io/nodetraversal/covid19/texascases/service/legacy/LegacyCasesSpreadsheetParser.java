package io.nodetraversal.covid19.texascases.service.legacy;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.right;

/**
 * @deprecated not being updated anymore by dshs.texas
 */
@Deprecated
@Slf4j
public class LegacyCasesSpreadsheetParser {
    private static final int CASES_SHEET_INDEX = 0;
    private static final String SHEET_NAME = "COVID-19 Cases";
    private static final int HEADER_ROW = 2;
    private static final int COUNTRY_COLUMN = 0;
    private static final String COUNTRY_COLUMN_NAME = "County Name";
    private static final int POPULATION_COLUMN = 1;
    private static final String POPULATION_COLUMN_NAME = "Population";

    public LegacyCovidStatistics parse(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(CASES_SHEET_INDEX);

        if (!sheet.getSheetName().equals(SHEET_NAME)) {
            throw new IllegalArgumentException("Expected sheet" + CASES_SHEET_INDEX + " to have name: "
                    + SHEET_NAME + ", found: " + sheet.getSheetName());
        }

        Row header = sheet.getRow(HEADER_ROW);

        String countyColumnName = header.getCell(COUNTRY_COLUMN).getStringCellValue();
        if (!countyColumnName.equals(COUNTRY_COLUMN_NAME)) {
            throw new IllegalArgumentException("Expected A3 value: "
                    + COUNTRY_COLUMN_NAME + ", found: " + countyColumnName);
        }

        String populationColumnName = header.getCell(POPULATION_COLUMN).getStringCellValue();
        if (!populationColumnName.equals(POPULATION_COLUMN_NAME)) {
            throw new IllegalArgumentException("Expected B3 value: "
                    + POPULATION_COLUMN_NAME + ", found: " + populationColumnName);
        }

        int headerSize = header.getLastCellNum();
        List<String> dates = new ArrayList<>();
        for (int i = 2; i < headerSize; i++) {
            String cellValue = header.getCell(i).getStringCellValue();
            String dateColumn = right(cellValue, 5);
            if (!dateColumn.matches("^\\d\\d-\\d\\d$")) {
                throw new IllegalArgumentException("Expected date value to be a date "
                        + "at column index: " +  i + ", found: "
                    + dateColumn);
            }
            dates.add(dateColumn + "-2020");
        }

        return parseStatistics(sheet, headerSize, dates);
    }

    private LegacyCovidStatistics parseStatistics(Sheet sheet, int headerSize, List<String> dates) {
        Map<String, Integer> populationDataByCounty = new HashMap<>();
        Map<String, List<Integer>> newCasesByCounty = new HashMap<>();
        String lastCounty = "?";

        for (int rowIndex = HEADER_ROW + 1; rowIndex < sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);

            if (row.getLastCellNum() != headerSize) {
                if (row.getLastCellNum() < POPULATION_COLUMN) {
                    if (log.isInfoEnabled()) {
                        log.info("Empty row found at: " + rowIndex + ", last county:" + lastCounty);
                    }
                    break;
                } else {
                    String county = row.getCell(COUNTRY_COLUMN).getStringCellValue();
                    throw new IllegalArgumentException("Invalid row size for " + county + " , expected: "
                        + headerSize + ", found: " + row.getLastCellNum());
                }
            }

            String county = row.getCell(COUNTRY_COLUMN).getStringCellValue();
            int population = (int) row.getCell(POPULATION_COLUMN).getNumericCellValue();
            int lastCases = (int) row.getCell(POPULATION_COLUMN + 1).getNumericCellValue();
            List<Integer> newCases = new ArrayList<>();

            for (int cellIndex = POPULATION_COLUMN + 2; cellIndex < row.getLastCellNum(); cellIndex++) {
                int cases = (int) row.getCell(cellIndex).getNumericCellValue();
                int newCaseCount = cases - lastCases;
                newCases.add(newCaseCount);

                lastCases = cases;
            }
            populationDataByCounty.put(county, population);
            newCasesByCounty.put(county, newCases);

            lastCounty = county;
        }

        return new LegacyCovidStatistics(dates, populationDataByCounty, newCasesByCounty);
    }
}
