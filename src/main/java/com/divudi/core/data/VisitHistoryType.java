package com.divudi.core.data;

/**
 * Visit type label shown in the Visit History (issue #25846). This is the
 * "prefix by visit type". The bill-type to label mapping lives only in
 * {@link #resolve(BillTypeAtomic, DepartmentType)}; do not repeat it elsewhere.
 * See developer_docs/billing/visit-history-business-rule.md.
 */
public enum VisitHistoryType {

    OPD("OPD"),
    LAB("Lab"),
    CHANNEL("Channel"),
    INPATIENT("Inpatient"),
    EMERGENCY("Emergency"),
    PHARMACY("Pharmacy"),
    STORE("Store"),
    COLLECTING_CENTRE("Collecting Centre"),
    DEPOSIT("Patient Deposit"),
    OTHER("Other");

    private final String label;

    VisitHistoryType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Resolve the visit type of a bill.
     *
     * @param bta the bill's atomic type; may be null for very old bills
     * @param departmentType type of the department that raised the bill; may be
     * null
     */
    public static VisitHistoryType resolve(BillTypeAtomic bta, DepartmentType departmentType) {
        if (bta == null) {
            return OTHER;
        }
        String name = bta.name();
        // Anything tied to an admission or theatre is an inpatient event, whatever module raised it.
        if (name.contains("INWARD") || name.contains("BHT") || name.contains("THEATRE") || name.contains("INPATIENT")) {
            return INPATIENT;
        }
        ServiceType st = bta.getServiceType();
        if (st == null) {
            return OTHER;
        }
        switch (st) {
            case OPD:
            case COMPANY_CREDIT:
                if (departmentType == DepartmentType.Lab || departmentType == DepartmentType.External_Lab) {
                    return LAB;
                }
                if (departmentType == DepartmentType.Etu) {
                    return EMERGENCY;
                }
                return OPD;
            case CHANNELLING:
                return CHANNEL;
            case INWARD:
            case INWARD_SERVICE:
                return INPATIENT;
            case PHARMACY:
                return PHARMACY;
            case STORE:
                return STORE;
            case COLLECTING_CENTRE:
                return COLLECTING_CENTRE;
            case PATIENT_DEPOSIT:
                return DEPOSIT;
            default:
                return OTHER;
        }
    }

    /**
     * True for bill types that are internal bookkeeping and must not be shown
     * to the patient in the patient portal: drafts and pre-bills, requests and
     * estimates, credit/debit notes, company claims, store, agency, collecting
     * centre, professional-fee payments and other non-patient money movements.
     * Staff see every bill; only the portal applies this filter.
     */
    public static boolean isInternalForPortal(BillTypeAtomic bta) {
        if (bta == null) {
            return true;
        }
        BillCategory cat = bta.getBillCategory();
        if (cat == BillCategory.PREBILL || cat == BillCategory.PAYMENTS) {
            return true;
        }
        String name = bta.name();
        if (name.contains("_PRE") || name.contains("REQUEST") || name.contains("ESTIMATE")
                || name.contains("PROVISIONAL") || name.contains("CREDIT_NOTE") || name.contains("DEBIT_NOTE")
                || name.contains("CLAIM") || name.contains("CREDIT_COMPANY") || name.contains("PROFESSIONAL")
                || name.contains("ADJUSTMENT") || name.contains("REJECTION") || name.contains("ACCEPT_")) {
            return true;
        }
        ServiceType st = bta.getServiceType();
        if (st == null) {
            return true;
        }
        switch (st) {
            case STORE:
            case OTHER:
            case AGENCY:
            case SETTLEMENT:
            case COLLECTING_CENTRE:
            case COMPANY_CREDIT:
            case PROFESSIONAL_PAYMENT:
                return true;
            default:
                return false;
        }
    }
}
