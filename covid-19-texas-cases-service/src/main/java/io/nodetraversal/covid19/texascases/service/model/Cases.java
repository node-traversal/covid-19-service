package io.nodetraversal.covid19.texascases.service.model;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Cases {
    private Integer no;
    private String county;
    private Integer positive;
    private Integer fatalities;
}
