/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data;

/**
 *
 * @author Buddhika
 */
public enum FeeType {

    Staff,
    Member,
    Outpatient,
    OtherInstitution,
    OwnInstitution,
    Department,
    Tax,
    @Deprecated
    Matrix,
    Issue,
    Additional,;

    public String getLabel() {
        switch (this) {
            case Staff:
                return "Staff Fee";
            case OwnInstitution:
                return "Hospital Fee ";
            case OtherInstitution:
                return "Out Side Fee ";
            default:
                return this.toString();
        }

    }
}
