/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.hr;

/**
 *
 * @author safrin
 */
public enum EmployeeStatus {

   Permanent,
    Temporary,
    Casual,
    Contract,
    Training,
    Volunteer,
    Extended_Leave,
    Retired,;

    public  String getLabel() {
        switch (this) {
            case Permanent:
                return "Permanent";
            case Temporary:
                return "Temporary";
            case Casual:
                return "Casual";
            case Contract:
                return "Contract";
            case Training:
                return "Training";
            case Volunteer:
                return "Volunteer";
            case Extended_Leave:
                return "Extended_Leave";
            case Retired:
                return "Retired";

            default:
                return this.toString();
        }
       
    }
}
