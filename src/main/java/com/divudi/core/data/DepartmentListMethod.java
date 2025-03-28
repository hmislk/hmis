/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data;

/**
 * @author Buddhika
 */
public enum DepartmentListMethod {

    LoggedDepartmentOnly,
    AllDepartmentsOfLoggedInstitution,
    AllDepartmentsOfAllInstitutions,
    AllPharmaciesOfLoggedInstitution,
    AllPharmaciesOfAllInstitutions,
    ActiveDepartmentsOfLoggedInstitution,
    ActiveDepartmentsOfAllInstitutions,
}
