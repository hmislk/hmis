/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

/**
 *
 * @author Buddhika
 */
public enum InstitutionType {
    Agency,
    CollectingCentre,
    CreditCompany,
    Bank,
    Lab,
    Hospital,
    Dealer,
    StoreDealor,
    Importer,
    Manufacturer,
    Company,
    branch;
   

    public String getLabel() {
        switch (this) {
            case Company:
                return "Managed Institution";
            case CollectingCentre:
                return "Collecting Centre";
            case CreditCompany:
                return "Credit Company";
            case StoreDealor:
                return "Store Dealor";
        }
        return this.toString();
    }

}
