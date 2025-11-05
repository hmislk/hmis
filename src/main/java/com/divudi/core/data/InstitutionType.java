package com.divudi.core.data;

/**
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
public enum InstitutionType {

    Agency("Agency"),
    OnlineBookingAgent("Channeling Online Booking Agent"),
    Bank("Bank"),
    Site("Site"),
    branch("Branch"),
    CollectingCentre("Collecting Centre"),
    Company("Company"),
    CreditCompany("Credit Company"),
    Dealer("Dealer"),
    Distributor("Distributor"),
    EducationalInstitute("Educational Institute"),
    Government("Government"),
    Hospital("Hospital"),
    Importer("Importer"),
    Lab("Lab"),
    Manufacturer("Manufacturer"),
    NonProfit("Non-Profit"),
    Other("Other"),
    Pharmacy("Pharmacy"),
    PrivatePractice("Private Practice"),
    StoreDealor("Store Dealer"),
    Wholesaler("Wholesaler");

    private final String label;

    InstitutionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }
}
