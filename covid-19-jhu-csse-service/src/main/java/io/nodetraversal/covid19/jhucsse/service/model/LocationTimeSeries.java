package io.nodetraversal.covid19.jhucsse.service.model;

import io.nodetraversal.covid19.jhucsse.service.csv.AccessorAlias;
import io.nodetraversal.covid19.jhucsse.service.csv.TimeSeries;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class LocationTimeSeries implements TimeSeries {
    private String UID;
    private String iso2;
    private String iso3;
    private String code3;
    private String FIPS;
    @Getter(onMethod = @__(@AccessorAlias(alias = "getAdmin2")))
    @Setter(onMethod = @__(@AccessorAlias(alias = "setAdmin2")))
    private String county;
    private String provinceState;
    private String countryRegion;
    @Getter(onMethod = @__(@AccessorAlias(alias = "getLat")))
    @Setter(onMethod = @__(@AccessorAlias(alias = "setLat")))
    private Double latitude;
    @Getter(onMethod = @__(@AccessorAlias(alias = "getLong")))
    @Setter(onMethod = @__(@AccessorAlias(alias = "setLong")))
    private Double longitude;
    private String combinedKey;

    private List<Integer> values;

    public Integer getLastValue() {
        return values.get(values.size() - 1);
    }
}
