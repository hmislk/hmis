/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.lab;

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
                return "None";
            case One:
                return "Normal";
            case Two:
                return "High";
            case Three:
                return "Urgent";
            case Four:
                return "Critical";
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
