/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data;

/**
 * Author: Buddhika
 */
public enum DepartmentType {
    Clinical("Clinical", "Clinical"),
    NonClinical("Non-Clinical", "Non-Clinical"),
    Pharmacy("Pharmacy", "Pharmacy"),
    Lab("Hospital Lab", "Lab"),
    External_Lab("Outsource Lab", "Outsource Lab"),
    Channelling("Channelling", "Channelling"),
    Opd("Out Patient Department (OPD)", "OPD"),
    Inward("Inward", "Inward"),
    Theatre("Theatre", "Theatre"),
    Etu("Emergency Treatment Unit (ETU)", "ETU"),
    CollectingCentre("Collecting Centre", "Collecting Centre"),
    Store("Store", "Store"),
    Inventry("Inventory", "Inventory"),
    Kitchen("Kitchen", "Kitchen"),
    Optician("Optician", "Optician"),
    Counter("Counter", "Counter"),
    Cashier("Cashier", "Cashier"),
    Office("Office", "Office"),
    Ict("Information and Communication Technology (ICT)", "ICT"),
    Other("Other", "Other");

    private final String label;
    private final String shortLabel;

    DepartmentType(String label, String shortLabel) {
        this.label = label;
        this.shortLabel = shortLabel;
    }

    public String getLabel() {
        return label;
    }

    public String getShortLabel() {
        return shortLabel;
    }
}
