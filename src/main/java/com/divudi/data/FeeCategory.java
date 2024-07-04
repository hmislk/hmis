/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

/**
 * @author Buddhika
 */
public enum FeeCategory {

    Staff,
    Institution,
    CollectingCentre;

    public String getLabel() {
        switch (this) {
            case Staff:
                return "Staff";
            case Institution:
                return "Out Source";
            case CollectingCentre:
                return "Collecting Centre";
            default:
                return this.toString();
        }
    }
}
