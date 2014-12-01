/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data;

/**
 *
 * @author Buddhika
 */
public enum ReportItemType {
    PatientName,
    PatientAge,
    PatientSex,
    InvestigationName,
    SampledTime,
    BilledDate,
    BilledTime,
    SampledDate,
    BillNo,
    BillItemNo,
    AutherizedSignature,
    DataEntrySignature,
    ReportedDate,
    ReportedTime,
    ReferringDoctor,
    Speciman,
    CollectingCenter,
    NameInFull,
    Surname,
    NamwWithInitials,
    Birthday,
    NicNo,
    Nationality,
    Religion,
    BloodGroup,
    Area,
    Phone,
    Mobile,
    Fax,
    CivilStatus,
    Category,
    ;
    
    public String getLabel(){
        switch (this){
            case AutherizedSignature: return "AutherizedSignature";
        }
        return null;
    }
}
