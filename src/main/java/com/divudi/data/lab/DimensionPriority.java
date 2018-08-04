/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.lab;

/**
 *
 * @author buddhika_ari
 */
public enum DimensionPriority {
    Zero,
    One,
    Two,
    Three,
    Four;

    public String getLabel() {
        switch (this) {
            case Zero:
                return "Routine";
            case One:
                return "STAT";
            case Two:
                return "ASAP";
            case Three:
                return "QC";
            case Four:
                return "XQC";
        }
        return "";
    }

    public Integer getValue() {
        switch (this) {
            case Zero:
                return 0;
            case One:
                return 1;
            case Two:
                return 2;
            case Three:
                return 3;
            case Four:
                return 4;
        }
        return null;
    }

}
