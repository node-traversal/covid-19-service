package io.nodetraversal.covid19.texascases.service;

import com.google.common.collect.ImmutableList;
import io.nodetraversal.covid19.texascases.service.model.Cases;
import io.nodetraversal.covid19.texascases.service.model.CovidStatistics;
import io.nodetraversal.covid19.texascases.service.model.Trends;
import io.nodetraversal.poi.ReflectionTableParser;
import io.nodetraversal.poi.RemoteWorkbookProvider;
import io.nodetraversal.poi.TableParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("stats")
@Service("covid-endpoint")
@Produces(APPLICATION_JSON)
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class CovidStatisticsEndpoint {
    private static final String CASES_URL = "https://www.dshs.state.tx.us/coronavirus/TexasCOVID19CaseCountData.xlsx";

    private final RemoteWorkbookProvider remoteWorkbookProvider;

    private final TableParser<Cases> casesTableParser = new ReflectionTableParser<>(
            Cases.class,
            "Cases and Fatalities",
            ImmutableList.of("No.", "County", "Positive", "Fatalities"),
            1
    );

    private final TableParser<Trends> trendsTableParser = new ReflectionTableParser<>(
            Trends.class,
            "Trends",
            ImmutableList.of(
                    "Date",
                    "Cumulative Cases",
                    "Cumulative Fatalities"
            ),
            2
    );

    @GET
    public CovidStatistics get() {
        log.info("Fetching: " + CASES_URL);
        return remoteWorkbookProvider.get(CASES_URL, workbook -> {
            log.info("Processing: " + CASES_URL);
            List<Cases> cases = casesTableParser.parse(workbook);
            log.info("Processed: cases");
            List<Trends> trends = trendsTableParser.parse(workbook);
            log.info("Processed: trends");
            return new CovidStatistics(cases, trends);
        });
    }
}
