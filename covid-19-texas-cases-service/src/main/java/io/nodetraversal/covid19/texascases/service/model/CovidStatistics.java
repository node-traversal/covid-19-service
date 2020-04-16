package io.nodetraversal.covid19.texascases.service.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CovidStatistics {

    private List<Cases> cases;
    private List<Trends> trends;

    public CovidStatistics(List<Cases> cases, List<Trends> trends) {
        this.cases = cases;
        this.trends = trends;
    }
}
