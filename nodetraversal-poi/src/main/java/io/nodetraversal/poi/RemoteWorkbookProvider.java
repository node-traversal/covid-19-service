package io.nodetraversal.poi;

import org.apache.poi.ss.usermodel.Workbook;

import java.util.function.Function;

public interface RemoteWorkbookProvider {
    <R> R get(String url, Function<Workbook, R> convert);
}
