package com.divudi.core.data.reports;

import java.util.function.Function;

import com.itextpdf.layout.properties.TextAlignment;

public class ReportColumn<T> {

    String header;
    Object footer;
    Function<T, Object> dataExtractor;
    String format;
    TextAlignment textAlignment;
    Float columnWidth;

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

    public ReportColumn(String header, Function<T, Object> dataExtractor, TextAlignment textAlignment, String format, Float cW, Object footer) {
        this.header = header;
        this.dataExtractor = dataExtractor;
        this.textAlignment = textAlignment;
        this.format = format;
        this.footer = footer;
        this.columnWidth = cW;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public Object getFooter() {
        return footer;
    }

    public void setFooter(Object footer) {
        this.footer = footer;
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
