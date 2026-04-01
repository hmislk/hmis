package com.divudi.bean.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.dto.InwardChargeTypeDetailRowDto;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.PatientItem;
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

        // --- BillItems from InwardFinalBill bills ---
        rows.addAll(fetchBillItemRows());

        // --- PatientItems (TimedItems) ---
        rows.addAll(fetchTimedItemRows());

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
     * Fetches BillItems for InwardFinalBill bills whose PatientEncounter matches
     * the current filter criteria, without materialising the encounter list first.
     */
    private List<InwardChargeTypeDetailRowDto> fetchBillItemRows() {

        StringBuilder jpql = new StringBuilder(
                "select bi from BillItem bi join bi.bill b join b.patientEncounter enc"
                + " where bi.retired = false"
                + " and b.retired = false"
                + " and b.cancelled = false"
                + " and b.billType = :btp"
                + " and bi.inwardChargeType = :ct");

        Map<String, Object> params = new HashMap<>();
        params.put("btp", BillType.InwardFinalBill);
        params.put("ct",  selectedChargeType);
        appendEncounterFilters(jpql, params, "enc");

        List<BillItem> raw = billItemFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

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
