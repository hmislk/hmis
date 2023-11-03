package com.divudi.data;

public enum PaperType {
    PosPaper("POS Paper"),
    FiveFivePaper("5.5 Paper"),
    FiveFivePrintedPaper("5.5 Printed Paper"),
    A4Paper("A4 Paper"),
    Paper24_2x9_3("24.2x9.3 Paper");

    private String label;

    private PaperType(String label) {
        this.label = label;
    }

    public String getLabel() {
        switch (this){
            case A4Paper:
                label="A4 Paper";
                break;
               
            case FiveFivePaper:
                label="5x5 Papaer";
                break;
                
            case FiveFivePrintedPaper:
                label="5x5 Printed Paper";
                break;
               
            case Paper24_2x9_3:
                label="24.2x9.3 Paper";
                break;
                
            case PosPaper:
                label="POS Paper";
                break;
        }
                
        return label;
    }
}
