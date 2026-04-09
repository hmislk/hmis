package com.divudi.bean.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dto.InwardChargeTypeDetailRowDto;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.data.inward.CalculationMethod;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.PatientItem;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientItemFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Controller for the Inward Charge Type Detail report (Issue #19320).
 *
 * Lists individual service/item lines (BillItems from InwardBill and
 * PatientItems for TimedItems) filtered by a mandatory InwardChargeType,
 * date range, and optional encounter filters.
 *
 * Rows are sorted by BHT No then by date/time of the charge.
 */
@Named
@SessionScoped
public class InwardChargeTypeDetailController implements Serializable {

    // -------------------------------------------------------------------------
    // EJBs / CDI
    // -------------------------------------------------------------------------
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PatientItemFacade patientItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    // -------------------------------------------------------------------------
    // Filter fields
    // -------------------------------------------------------------------------
    private Date fromDate = startOfCurrentMonth();
    private Date toDate   = new Date();

    /** "admissionDate" or "dischargeDate" */
    private String dateBasis = "dischargeDate";

    private AdmissionStatus  admissionStatus = AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED;
    private AdmissionType    admissionType;
    private Institution      institution;
    private Institution      site;
    private Department       department;

    /** Mandatory — report cannot be generated without this */
    private InwardChargeType selectedChargeType;

    // -------------------------------------------------------------------------
    // Report output
    // -------------------------------------------------------------------------
    private List<InwardChargeTypeDetailRowDto> reportRows;
    private double totalGross;
    private double totalDiscount;
    private double totalNet;

    /** Error message shown when charge type not selected */
    private String errorMessage;

    // -------------------------------------------------------------------------
    // Main generate method
    // -------------------------------------------------------------------------

    public void generateReport() {
        errorMessage = null;
        reportRows   = null;
        totalGross   = 0;
        totalDiscount = 0;
        totalNet     = 0;

        if (selectedChargeType == null) {
            errorMessage = "Please select an Inward Charge Type before generating the report.";
            return;
        }

        // Reject impossible filter: discharge date range requires discharged patients
        if ("dischargeDate".equals(dateBasis)
                && admissionStatus == AdmissionStatus.ADMITTED_BUT_NOT_DISCHARGED) {
            errorMessage = "Cannot filter by discharge date for patients not yet discharged.";
            return;
        }

        // Reject inverted date range
        if (fromDate != null && toDate != null && fromDate.after(toDate)) {
            errorMessage = "From date must not be after To date.";
            return;
        }

        List<InwardChargeTypeDetailRowDto> rows = new ArrayList<>();

        // --- BillItems (InwardBill, InwardOutSideBill, pharmacy, store) ---
        rows.addAll(fetchBillItemRows());

        // --- PatientItems (TimedItems — timed services only) ---
        rows.addAll(fetchTimedItemRows());

        // --- BillFee rows (InwardProfessional — ProfessionalCharge, DoctorAndNurses) ---
        rows.addAll(fetchProfessionalFeeRows());

        // --- Sort: BHT No (natural string sort), then sortTime ascending ---
        rows.sort(Comparator
                .comparing(InwardChargeTypeDetailRowDto::getBhtNo,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(InwardChargeTypeDetailRowDto::getSortTime,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        // --- Accumulate totals ---
        for (InwardChargeTypeDetailRowDto row : rows) {
            totalGross    += row.getGrossValue();
            totalDiscount += row.getDiscount();
            totalNet      += row.getNetValue();
        }

        reportRows = rows;
    }

    // -------------------------------------------------------------------------
    // Query helpers
    // -------------------------------------------------------------------------

    /**
     * Appends PatientEncounter filter conditions onto a JPQL query that already
     * joins to an encounter alias {@code enc}. All parameters are added to
     * {@code params}.
     */
    private void appendEncounterFilters(StringBuilder jpql, Map<String, Object> params,
            String encAlias) {
        jpql.append(" and ").append(encAlias).append(".retired = false");

        if (fromDate != null && toDate != null) {
            if ("admissionDate".equals(dateBasis)) {
                jpql.append(" and ").append(encAlias).append(".dateOfAdmission between :fromDate and :toDate");
            } else {
                jpql.append(" and ").append(encAlias).append(".dateOfDischarge between :fromDate and :toDate");
            }
            params.put("fromDate", fromDate);
            params.put("toDate",   toDate);
        }

        if (admissionStatus != null && admissionStatus != AdmissionStatus.ANY_STATUS) {
            switch (admissionStatus) {
                case ADMITTED_BUT_NOT_DISCHARGED:
                    jpql.append(" and ").append(encAlias).append(".discharged = :dis");
                    params.put("dis", false);
                    break;
                case DISCHARGED_AND_FINAL_BILL_COMPLETED:
                    jpql.append(" and ").append(encAlias).append(".discharged = :dis");
                    jpql.append(" and ").append(encAlias).append(".paymentFinalized = :pf");
                    params.put("dis", true);
                    params.put("pf",  true);
                    break;
                case DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED:
                    jpql.append(" and ").append(encAlias).append(".discharged = :dis");
                    jpql.append(" and ").append(encAlias).append(".paymentFinalized = :pf");
                    params.put("dis", true);
                    params.put("pf",  false);
                    break;
                default:
                    break;
            }
        }

        if (admissionType != null) {
            jpql.append(" and ").append(encAlias).append(".admissionType = :admType");
            params.put("admType", admissionType);
        }

        if (institution != null) {
            jpql.append(" and ").append(encAlias).append(".institution = :ins");
            params.put("ins", institution);
        }

        if (site != null) {
            jpql.append(" and ").append(encAlias).append(".department.site = :site");
            params.put("site", site);
        }

        if (department != null) {
            jpql.append(" and ").append(encAlias).append(".department = :dept");
            params.put("dept", department);
        }
    }

    /**
     * Dispatches to the correct BillItem fetch method based on the
     * CalculationMethod of the selected charge type.
     *
     * BILL_FEE and PATIENT_ROOM charge types are handled by their own dedicated
     * fetch methods (fetchProfessionalFeeRows and fetchTimedItemRows respectively)
     * so this method returns an empty list for those cases.
     */
    private List<InwardChargeTypeDetailRowDto> fetchBillItemRows() {
        CalculationMethod cm = selectedChargeType.getCalculationMethod();
        switch (cm) {
            case BILL_FEE:
            case PATIENT_ROOM:
                return new ArrayList<>();
            case PHARMACY_BILL:
                return fetchPharmacyBillItemRows();
            case STORE_BILL:
                return fetchStoreBillItemRows();
            default: // BILL_ITEM and ADMISSION_FEE
                return fetchInwardBillItemRows();
        }
    }

    /**
     * Fetches BillItems from InwardBill and InwardOutSideBill for BILL_ITEM /
     * ADMISSION_FEE charge types.
     *
     * Uses an explicit LEFT JOIN on bi.item so that InwardOutSideBill items
     * (where bi.item is null) are not dropped by implicit inner-join semantics
     * when the OR predicate navigates through i.inwardChargeType.
     */
    private List<InwardChargeTypeDetailRowDto> fetchInwardBillItemRows() {
        StringBuilder jpql = new StringBuilder(
                "select bi from BillItem bi join bi.bill b join b.patientEncounter enc"
                + " left join bi.item i"
                + " where bi.retired = false"
                + " and b.retired = false"
                + " and b.cancelled = false"
                + " and b.billType in :btps"
                + " and (bi.inwardChargeType = :ct or i.inwardChargeType = :ct)");
        Map<String, Object> params = new HashMap<>();
        List<BillType> btps = new ArrayList<>();
        btps.add(BillType.InwardBill);
        btps.add(BillType.InwardOutSideBill);
        params.put("btps", btps);
        params.put("ct", selectedChargeType);
        appendEncounterFilters(jpql, params, "enc");
        return mapBillItemsToRows(billItemFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP));
    }

    /**
     * Fetches BillItems from inward pharmacy bills for PHARMACY_BILL charge types.
     */
    private List<InwardChargeTypeDetailRowDto> fetchPharmacyBillItemRows() {
        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE);
        btas.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE_CANCELLED);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION);
        btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD);
        btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN);
        btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION);

        StringBuilder jpql = new StringBuilder(
                "select bi from BillItem bi join bi.bill b join b.patientEncounter enc"
                + " where bi.retired = false"
                + " and b.retired = false"
                + " and b.cancelled = false"
                + " and b.billTypeAtomic in :btas"
                + " and bi.inwardChargeType = :ct");
        Map<String, Object> params = new HashMap<>();
        params.put("btas", btas);
        params.put("ct", selectedChargeType);
        appendEncounterFilters(jpql, params, "enc");
        return mapBillItemsToRows(billItemFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP));
    }

    /**
     * Fetches BillItems from inward store bills for STORE_BILL charge types.
     */
    private List<InwardChargeTypeDetailRowDto> fetchStoreBillItemRows() {
        StringBuilder jpql = new StringBuilder(
                "select bi from BillItem bi join bi.bill b join b.patientEncounter enc"
                + " where bi.retired = false"
                + " and b.retired = false"
                + " and b.cancelled = false"
                + " and b.billType = :btp"
                + " and bi.inwardChargeType = :ct");
        Map<String, Object> params = new HashMap<>();
        params.put("btp", BillType.StoreBhtPre);
        params.put("ct", selectedChargeType);
        appendEncounterFilters(jpql, params, "enc");
        return mapBillItemsToRows(billItemFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP));
    }

    /**
     * Maps a list of BillItem entities to detail row DTOs.
     */
    private List<InwardChargeTypeDetailRowDto> mapBillItemsToRows(List<BillItem> raw) {
        List<InwardChargeTypeDetailRowDto> rows = new ArrayList<>();
        if (raw == null) {
            return rows;
        }
        for (BillItem bi : raw) {
            InwardChargeTypeDetailRowDto row = new InwardChargeTypeDetailRowDto();
            PatientEncounter enc = bi.getBill() != null ? bi.getBill().getPatientEncounter() : null;
            if (enc != null) {
                row.setBhtNo(enc.getBhtNo());
                row.setPatientName(enc.getPatient() != null && enc.getPatient().getPerson() != null
                        ? enc.getPatient().getPerson().getNameWithTitle() : "");
                row.setDateOfAdmission(enc.getDateOfAdmission());
                row.setDateOfDischarge(enc.getDateOfDischarge());
            }
            if (bi.getItem() != null) {
                row.setItemName(bi.getItem().getName());
            } else if (bi.getInwardChargeType() != null) {
                row.setItemName(getChargeTypeLabel(bi.getInwardChargeType()));
            }
            row.setGrossValue(bi.getGrossValue());
            row.setDiscount(bi.getDiscount());
            row.setNetValue(bi.getNetValue());
            // fromTime/toTime left null for regular bill items
            // sortTime = billItem.billTime, falling back to bill.billTime
            Date sortTime = bi.getBillTime();
            if (sortTime == null && bi.getBill() != null) {
                sortTime = bi.getBill().getBillTime();
            }
            row.setSortTime(sortTime);
            rows.add(row);
        }
        return rows;
    }

    /**
     * Fetches BillFee rows from InwardProfessional bills for BILL_FEE charge
     * types (ProfessionalCharge, DoctorAndNurses). Returns empty list for all
     * other charge types.
     */
    private List<InwardChargeTypeDetailRowDto> fetchProfessionalFeeRows() {
        if (selectedChargeType.getCalculationMethod() != CalculationMethod.BILL_FEE) {
            return new ArrayList<>();
        }
        StringBuilder jpql = new StringBuilder(
                "select bf from BillFee bf join bf.bill b join b.patientEncounter enc"
                + " where bf.retired = false"
                + " and b.retired = false"
                + " and b.cancelled = false"
                + " and b.billType = :btp");
        Map<String, Object> params = new HashMap<>();
        params.put("btp", BillType.InwardProfessional);
        appendEncounterFilters(jpql, params, "enc");

        List<BillFee> raw = billFeeFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        List<InwardChargeTypeDetailRowDto> rows = new ArrayList<>();
        if (raw == null) {
            return rows;
        }
        for (BillFee bf : raw) {
            InwardChargeTypeDetailRowDto row = new InwardChargeTypeDetailRowDto();
            PatientEncounter enc = bf.getBill() != null ? bf.getBill().getPatientEncounter() : null;
            if (enc != null) {
                row.setBhtNo(enc.getBhtNo());
                row.setPatientName(enc.getPatient() != null && enc.getPatient().getPerson() != null
                        ? enc.getPatient().getPerson().getNameWithTitle() : "");
                row.setDateOfAdmission(enc.getDateOfAdmission());
                row.setDateOfDischarge(enc.getDateOfDischarge());
            }
            String feeName = "";
            if (bf.getFee() != null && bf.getFee().getName() != null) {
                feeName = bf.getFee().getName();
            } else if (bf.getStaff() != null && bf.getStaff().getPerson() != null) {
                feeName = bf.getStaff().getPerson().getNameWithTitle();
            }
            row.setItemName(feeName);
            double gross = bf.getFeeGrossValue() != null ? bf.getFeeGrossValue() : bf.getFeeValue();
            double disc  = bf.getFeeDiscount();
            row.setGrossValue(gross);
            row.setDiscount(disc);
            row.setNetValue(gross - disc);
            row.setSortTime(bf.getFeeAt());
            rows.add(row);
        }
        return rows;
    }

    /**
     * Fetches PatientItems (TimedItems) whose PatientEncounter matches the
     * current filter criteria, without materialising the encounter list first.
     */
    private List<InwardChargeTypeDetailRowDto> fetchTimedItemRows() {

        StringBuilder jpql = new StringBuilder(
                "select pi from PatientItem pi join pi.patientEncounter enc"
                + " where pi.retired = false"
                + " and type(pi.item) = TimedItem"
                + " and pi.item.inwardChargeType = :ct");

        Map<String, Object> params = new HashMap<>();
        params.put("ct", selectedChargeType);
        appendEncounterFilters(jpql, params, "enc");

        List<PatientItem> raw = patientItemFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        List<InwardChargeTypeDetailRowDto> rows = new ArrayList<>();
        if (raw == null) {
            return rows;
        }

        for (PatientItem pi : raw) {
            InwardChargeTypeDetailRowDto row = new InwardChargeTypeDetailRowDto();

            PatientEncounter enc = pi.getPatientEncounter();
            if (enc != null) {
                row.setBhtNo(enc.getBhtNo());
                row.setPatientName(enc.getPatient() != null && enc.getPatient().getPerson() != null
                        ? enc.getPatient().getPerson().getNameWithTitle() : "");
                row.setDateOfAdmission(enc.getDateOfAdmission());
                row.setDateOfDischarge(enc.getDateOfDischarge());
            }

            row.setItemName(pi.getItem() != null ? pi.getItem().getName() : "");
            row.setFromTime(pi.getFromTime());
            row.setToTime(pi.getToTime());

            double gross = pi.getServiceValue() != null ? pi.getServiceValue() : 0.0;
            double disc  = pi.getDiscount();
            row.setGrossValue(gross);
            row.setDiscount(disc);
            row.setNetValue(gross - disc);

            // sortTime = fromTime for ordering within the BHT
            row.setSortTime(pi.getFromTime());

            rows.add(row);
        }
        return rows;
    }

    // -------------------------------------------------------------------------
    // Charge type helpers
    // -------------------------------------------------------------------------

    public List<InwardChargeType> getAllChargeTypes() {
        return java.util.Arrays.asList(InwardChargeType.values());
    }

    public List<InwardChargeType> completeInwardChargeType(String query) {
        String lower = query == null ? "" : query.toLowerCase();
        List<InwardChargeType> results = new ArrayList<>();
        for (InwardChargeType ct : InwardChargeType.values()) {
            String label = configOptionApplicationController.getInwardChargeTypeLabel(ct);
            if (label.toLowerCase().contains(lower) || ct.name().toLowerCase().contains(lower)) {
                results.add(ct);
            }
        }
        return results;
    }

    public String getChargeTypeLabel(InwardChargeType type) {
        if (type == null) {
            return "";
        }
        return configOptionApplicationController.getInwardChargeTypeLabel(type);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void makeNull() {
        fromDate          = startOfCurrentMonth();
        toDate            = new Date();
        dateBasis         = "dischargeDate";
        admissionStatus   = AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED;
        admissionType     = null;
        institution       = null;
        site              = null;
        department        = null;
        selectedChargeType = null;
        reportRows        = null;
        totalGross        = 0;
        totalDiscount     = 0;
        totalNet          = 0;
        errorMessage      = null;
    }

    private static Date startOfCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------

    public Date getFromDate() { return fromDate; }
    public void setFromDate(Date fromDate) { this.fromDate = fromDate; }

    public Date getToDate() { return toDate; }
    public void setToDate(Date toDate) { this.toDate = toDate; }

    public String getDateBasis() { return dateBasis; }
    public void setDateBasis(String dateBasis) { this.dateBasis = dateBasis; }

    public AdmissionStatus getAdmissionStatus() { return admissionStatus; }
    public void setAdmissionStatus(AdmissionStatus admissionStatus) { this.admissionStatus = admissionStatus; }

    public AdmissionType getAdmissionType() { return admissionType; }
    public void setAdmissionType(AdmissionType admissionType) { this.admissionType = admissionType; }

    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution institution) { this.institution = institution; }

    public Institution getSite() { return site; }
    public void setSite(Institution site) { this.site = site; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public InwardChargeType getSelectedChargeType() { return selectedChargeType; }
    public void setSelectedChargeType(InwardChargeType selectedChargeType) { this.selectedChargeType = selectedChargeType; }

    public List<InwardChargeTypeDetailRowDto> getReportRows() { return reportRows; }

    public double getTotalGross() { return totalGross; }
    public double getTotalDiscount() { return totalDiscount; }
    public double getTotalNet() { return totalNet; }

    public String getErrorMessage() { return errorMessage; }
}
