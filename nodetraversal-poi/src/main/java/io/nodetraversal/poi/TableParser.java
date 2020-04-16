package io.nodetraversal.poi;

import org.apache.poi.ss.usermodel.Workbook;

import java.io.InputStream;
import java.util.List;

public interface TableParser<T> {
    List<T> parse(InputStream io);

    List<T> parse(Workbook workbook);
}
