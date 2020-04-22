package io.nodetraversal.covid19.jhucsse.service;

import com.google.common.collect.ImmutableList;
import io.nodetraversal.covid19.jhucsse.service.config.Caches;
import io.nodetraversal.covid19.jhucsse.service.csv.TimeSeriesCsvParser;
import io.nodetraversal.covid19.jhucsse.service.model.LocationTimeSeries;
import io.nodetraversal.covid19.jhucsse.service.model.TimeSeriesGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.cache.annotation.CacheResult;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("time-series/us/confirmed")
@Service("cases-endpoint")
@Produces(APPLICATION_JSON)
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class UsConfirmedCasesEndpoint {
    private static final String US_CASES_URL = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_covid19_confirmed_US.csv";

    private final TimeSeriesCsvParser<LocationTimeSeries> defaultParser = new TimeSeriesCsvParser<>(
        LocationTimeSeries.class,
        ImmutableList.of(
                "UID", "iso2", "iso3", "code3", "FIPS", "Admin2", "Province_State", "Country_Region",
                "Lat", "Long_", "Combined_Key"),
        0
    );

    @GET
    @CacheResult(cacheName = Caches.DEFAULT)
    @Path("top-ten")
    public TimeSeriesGroup<LocationTimeSeries> get() {
        log.info("Fetching: " + US_CASES_URL);

        TimeSeriesGroup<LocationTimeSeries> series = defaultParser.fetch(US_CASES_URL);
        series.getSeries().sort((o2, o1) -> o1.getLastValue().compareTo(o2.getLastValue()));

        return new TimeSeriesGroup<>(series.getDates(), series.getSeries().subList(0, 10));
    }
}
