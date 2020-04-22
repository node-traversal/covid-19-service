package io.nodetraversal.covid19.jhucsse.service.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class TimeSeriesGroup<T> {
    private final List<String> dates;
    private final List<T> series;
}
