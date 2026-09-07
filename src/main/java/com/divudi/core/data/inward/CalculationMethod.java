package com.divudi.core.data.inward;

/**
 * Identifies how each InwardChargeType value is calculated and which data
 * sources contribute to it. Used by InwardChargeTypeBreakdownController to
 * branch report logic per charge type.
 */
public enum CalculationMethod {
    /** BillItem from InwardBill + InwardOutSideBill — default for most values */
    BILL_ITEM,
    /** BillItem from pharmacy issue bills identified by BillTypeAtomic */
    PHARMACY_BILL,
    /** BillItem from store issue bills (StoreBhtPre) */
    STORE_BILL,
    /** PatientRoom calculated fields (time-based) plus optional service BillItems — 2 fixed columns */
    PATIENT_ROOM,
    /** BillFee.feeValue from InwardProfessional bills, pivot on staff name */
    BILL_FEE,
    /** Flat fee from admissionType.admissionFee — single fixed column */
    ADMISSION_FEE
}
