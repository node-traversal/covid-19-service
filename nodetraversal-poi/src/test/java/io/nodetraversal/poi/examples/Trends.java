package io.nodetraversal.poi.examples;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class Trends {
    private Date date;
    private Integer cumulativeCases;
    private Integer cumulativeFatalities;
}
