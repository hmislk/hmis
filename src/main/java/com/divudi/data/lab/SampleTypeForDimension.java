/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.lab;

/**
 *
 * @author buddhika_ari
 */
public enum SampleTypeForDimension {
    W,
    One,
    Two,
    Three,
    Four,
    Five,
    Six,
    Seven,
    Eight,
    Nine;

    public String getLabel() {
        switch (this) {
            case W:
                return "Whole Blood";
            case One:
                return "Serum";
            case Two:
                return "Plasma";
            case Three:
                return "Urine";
            case Four:
                return "CSF";
            case Five:
                return "SerumQC1";
            case Six:
                return "SerumQC2";
            case Seven:
                return "SerumQC3";
            case Eight:
                return "UrineQC1";
            case Nine:
                return "UrineQC2";
        }
        return "";
    }
    
    public String getFiledValue() {
        switch (this) {
            case W:
                return "W";
            case One:
                return "1";
            case Two:
                return "2";
            case Three:
                return "3";
            case Four:
                return "4";
            case Five:
                return "5";
            case Six:
                return "6";
            case Seven:
                return "7";
            case Eight:
                return "8";
            case Nine:
                return "9";
        }
        return "";
    }

}
