package com.divudi.core.data.reports;

import java.util.function.Function;

import com.itextpdf.layout.properties.TextAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportColumn<T> {

    String header;
    Function<T, Object> dataExtractor;
    String format;
    TextAlignment textAlignment;
    Float columnWidth;

    private static final Logger logger = LoggerFactory.getLogger(ReportColumn.class.getName());

    public ReportColumn() {
    }

    public ReportColumn(String header) {
        this.header = header;
    }

    public ReportColumn(String header, Function<T, Object> dataExtractor, TextAlignment textAlignment, String format, Float cW) {
        this.header = header;
        this.dataExtractor = dataExtractor;
        this.textAlignment = textAlignment;
        this.format = format;
        this.columnWidth = cW;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public Function<T, Object> getDataExtractor() {
        return dataExtractor;
    }

    public void setDataExtractor(Function<T, Object> extractor) {
        this.dataExtractor = extractor;
    }

    public Object extractData(T row) {
        if (dataExtractor == null || row == null) {
            return null;
        }
        try {
            return dataExtractor.apply(row);
        } catch (Exception e) {
            logger.error(( "ReportColumn [{0}]: dataExtractor threw an exception"), header);
            return null;
        }
    }

    public TextAlignment getTextAlignment(){
        return textAlignment != null ? textAlignment : TextAlignment.LEFT;
    }

    public String getFormat() {
        if (format == null || format.trim().isEmpty()) {
            return "%s";
        }
        return format;
    }

    public Float getColumnWidth() {
        return columnWidth;
    }

    public void setColumnWidth(Float columnWidth) {
        this.columnWidth = columnWidth;
    }
}
