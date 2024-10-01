/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

/**
 * Author: Buddhika
 */
public enum DepartmentType {
    Clinical("Clinical"),
    NonClinical("Non-Clinical"),
    Pharmacy("Pharmacy"),
    Lab("Lab"),
    Channelling("Channelling"),
    Opd("Out Patient Department (OPD)"),
    Inward("Inward"),
    Theatre("Theatre"),
    Etu("Emergency Treatment Unit (ETU)"),
    CollectingCentre("Collecting Centre"),
    Store("Store"),
    Inventry("Inventory"),
    Kitchen("Kitchen"),
    Optician("Optician"),
    Counter("Counter"),
    Cashier("Cashier"),
    Office("Office"),
    Ict("Information and Communication Technology (ICT)"),
    Other("Other");

    private final String label;

    DepartmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
