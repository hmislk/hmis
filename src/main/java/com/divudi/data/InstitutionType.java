package com.divudi.data;

/**
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
public enum InstitutionType {
    Agency("Agency"),
    Bank("Bank"),
    branch("Branch"), // Original enum spelling retained
    CollectingCentre("Collecting Centre"),
    Company("Company"),
    CreditCompany("Credit Company"),
    Dealer("Dealer"),
    Distributor("Distributor"), // Added new
    EducationalInstitute("Educational Institute"), // Added new
    Government("Government"), // Added new
    Hospital("Hospital"),
    Importer("Importer"),
    Lab("Lab"),
    Manufacturer("Manufacturer"),
    NonProfit("Non-Profit"), // Added new
    Other("Other"),
    Pharmacy("Pharmacy"), // Added new
    PrivatePractice("Private Practice"), // Added new
    StoreDealor("Store Dealer"), // Original enum spelling retained
    Wholesaler("Wholesaler"); // Added new

    private final String label;

    InstitutionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }
}
