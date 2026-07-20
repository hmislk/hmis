package com.divudi.core.data.dto;

import java.io.Serializable;

public class SurgeryCostSummaryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String label1;
    private String label2;

    private long count;

    public SurgeryCostSummaryDTO(String label1, Long count) {
        this.label1 = label1;
        this.label2 = null;
        this.count  = count != null ? count : 0L;
    }

    public SurgeryCostSummaryDTO(String label1, String label2, Long count) {
        this.label1 = label1;
        this.label2 = label2;
        this.count  = count != null ? count : 0L;
    }

    public String getLabel1() {
        return label1;
    }

    public void setLabel1(String label1) {
        this.label1 = label1;
    }

    public String getLabel2() {
        return label2;
    }

    public void setLabel2(String label2) {
        this.label2 = label2;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
