package io.nodetraversal.covid19.texascases.service.functional;

import io.nodetraversal.covid19.texascases.service.CovidStatisticsEndpoint;
import io.nodetraversal.poi.HttpClientRemoteWorkbookProvider;

public class EndpointFunctionalMain {
    public static void main(String... args) {
        CovidStatisticsEndpoint endpoint = new CovidStatisticsEndpoint(new HttpClientRemoteWorkbookProvider());
        endpoint.get();
    }
}
