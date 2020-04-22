package io.nodetraversal.covid19.jhucsse.service.main;

import io.nodetraversal.covid19.jhucsse.service.UsConfirmedCasesEndpoint;
import io.nodetraversal.covid19.jhucsse.service.model.LocationTimeSeries;
import io.nodetraversal.covid19.jhucsse.service.model.TimeSeriesGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class UsConfirmedCasesEndpointMain {
    public static void main(String... args) {
        UsConfirmedCasesEndpoint endpoint = new UsConfirmedCasesEndpoint();
        TimeSeriesGroup<LocationTimeSeries> group = endpoint.get();
        List<LocationTimeSeries> series = group.getSeries();

        log.info("Last Element: {}", series.get(series.size() -1));
    }
}
