package io.nodetraversal.covid19.texascases.service.legacy;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * @deprecated not being updated anymore by dshs.texas
 */
@Getter
@Deprecated
public class LegacyCovidStatistics {
    private final List<String> dates;
    private final Map<String, Integer> populationDataByCounty;
    private final Map<String, List<Integer>> newCasesByCounty;

    public LegacyCovidStatistics(List<String> dates,
                                 Map<String, Integer> populationDataByCounty, Map<String,
            List<Integer>> newCasesByCounty) {
        this.dates = dates;
        this.populationDataByCounty = ImmutableMap.copyOf(populationDataByCounty);
        this.newCasesByCounty = ImmutableMap.copyOf(newCasesByCounty);
    }
}
