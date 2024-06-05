package com.divudi.data;

public enum PaperType {

    PosPaper("POS Paper"),
    PosPrintedPaper("POS Paper without headings"),
    FiveFivePaper("5 inch to 5 inch Paper with headings"),
    FiveFivePrintedPaper("5 inch to 5 inch Paper without headings"),
    A4Paper("A4 Paper"),
    A4PrintedPaper("A4 Paper without headings"),
    Paper24_2x9_3("24.2x9.3 Paper"),
    FiveFivePaperBatchBill("5 inch to 5 inch Paper with headings"),
    FiveFivePrintedPaperBatchBill("5 inch to 5 inch Paper without headings"),
    PosBatchPaper("POS Batch Bill"),
    PosPrintedBatchPaper("POS Printed Batch Bill"),
    PosHeaderPaper("POS Bill with Header");

    private String label;

    PaperType(String label) {
        this.label = label;
    }

    public String getLabel() {
        switch (this) {
            case A4Paper:
                label = "A4 Paper";
                break;

            case A4PrintedPaper:
                label = "A4 Printed Paper";
                break;

            case FiveFivePaper:
                label = "5x5 Papaer";
                break;

            case FiveFivePrintedPaper:
                label = "5x5 Printed Paper";
                break;

            case Paper24_2x9_3:
                label = "24.2x9.3 Paper";
                break;

            case PosPaper:
                label = "POS Paper";
                break;
        }

        return label;
    }
}
