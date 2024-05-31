/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

/**
 * @author Buddhika
 */
public enum FeeType {

    Staff,
    Member,
    Outpatient,
    OtherInstitution,
    OwnInstitution,
    Chemical,
    Department,
    Tax,
    Issue,
    Additional,
    Service,
    CollectingCentre,
    Referral;

    public String getLabel() {
        switch (this) {
            case Staff:
                return "Staff Fee";
            case OwnInstitution:
                return "Hospital Fee ";
            case OtherInstitution:
                return "Out Side Fee ";
            case Chemical:
                return "Regent Fee";
            case Service:
                return "Service Fee";
            case CollectingCentre:
                return "Collecting Centre";
            case Referral:
                return "Referral Fee";
            default:
                return this.toString();
        }
    }
}
