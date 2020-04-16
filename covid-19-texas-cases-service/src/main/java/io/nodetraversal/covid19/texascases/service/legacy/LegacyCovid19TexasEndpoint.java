package io.nodetraversal.covid19.texascases.service.legacy;

import io.nodetraversal.poi.RemoteWorkbookProvider;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;

/**
 * LegacyCovid19TexasEndpoint produces county by county data over time
 *
 * @deprecated not being updated anymore by dshs.texas
 */
@Deprecated
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class LegacyCovid19TexasEndpoint {
    private static final String CASES_URL = "https://www.dshs.state.tx.us/coronavirus/TexasCOVID19CaseCountData.xlsx";

    private final RemoteWorkbookProvider remoteWorkbookProvider;
    private final LegacyCasesSpreadsheetParser parser;

    public LegacyCovidStatistics get() {
        return remoteWorkbookProvider.get(CASES_URL, parser::parse);
    }
}
