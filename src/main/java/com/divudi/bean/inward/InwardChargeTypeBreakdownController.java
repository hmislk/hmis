package com.divudi.bean.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.data.inward.CalculationMethod;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PatientRoomFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Controller for the Inward Charge Type Breakdown by BHT report (Issue #19655).
 *
 * Produces a pivot table: one row per BHT, one column per distinct item/staff
 * found in the period for the selected InwardChargeType. The data source and
 * column structure depend on the charge type's CalculationMethod.
 */
@Named
@SessionScoped
public class InwardChargeTypeBreakdownController implements Serializable {

    // -------------------------------------------------------------------------
    // EJBs / CDI
    // -------------------------------------------------------------------------
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PatientRoomFacade patientRoomFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    // -------------------------------------------------------------------------
    // Filter fields
    // -------------------------------------------------------------------------
    private Date fromDate = startOfCurrentMonth();
    private Date toDate = new Date();

    /** "admissionDate" or "dischargeDate" */
    private String dateBasis = "dischargeDate";

    private AdmissionStatus admissionStatus = AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED;
    private AdmissionType admissionType;
    private Institution institution;
    private Institution site;
    private Department department;

    /** Mandatory — report cannot be generated without this */
    private InwardChargeType selectedChargeType;

    // -------------------------------------------------------------------------
    // Report output
    // -------------------------------------------------------------------------
    private List<String> columnKeys;
    private List<String> columnLabels;
    private List<BhtBreakdownRow> rows;
    private List<Double> columnTotals;   // List<Double>, NOT double[] — EL cannot iterate primitive arrays
    private double grandTotal;
    private String errorMessage;

    // -------------------------------------------------------------------------
    // Inner DTO
    // -------------------------------------------------------------------------

    public static class BhtBreakdownRow implements Serializable {

        private String bhtNo;
        private String patientName;
        private Date dateOfAdmission;
        private Date dateOfDischarge;
        /** columnKey → net amount for that column on this BHT */
        private Map<String, Double> cellValues = new LinkedHashMap<>();
        private double rowTotal;

        public String getBhtNo() { return bhtNo; }
        public void setBhtNo(String bhtNo) { this.bhtNo = bhtNo; }

        public String getPatientName() { return patientName; }
        public void setPatientName(String patientName) { this.patientName = patientName; }

        public Date getDateOfAdmission() { return dateOfAdmission; }
        public void setDateOfAdmission(Date dateOfAdmission) { this.dateOfAdmission = dateOfAdmission; }

        public Date getDateOfDischarge() { return dateOfDischarge; }
        public void setDateOfDischarge(Date dateOfDischarge) { this.dateOfDischarge = dateOfDischarge; }

        public Map<String, Double> getCellValues() { return cellValues; }
        public void setCellValues(Map<String, Double> cellValues) { this.cellValues = cellValues; }

        public double getRowTotal() { return rowTotal; }
        public void setRowTotal(double rowTotal) { this.rowTotal = rowTotal; }
    }

    // -------------------------------------------------------------------------
    // Main generate method
    // -------------------------------------------------------------------------

    public void generateReport() {
        errorMessage = null;
        columnKeys   = null;
        columnLabels = null;
        rows         = null;
        columnTotals = null;
        grandTotal   = 0.0;

        if (selectedChargeType == null) {
            errorMessage = "Please select an Inward Charge Type before generating the report.";
            return;
        }

        if ("dischargeDate".equals(dateBasis)
                && admissionStatus == AdmissionStatus.ADMITTED_BUT_NOT_DISCHARGED) {
            errorMessage = "Cannot filter by discharge date for patients not yet discharged.";
            return;
        }

        if (fromDate != null && toDate != null && fromDate.after(toDate)) {
            errorMessage = "From date must not be after To date.";
            return;
        }

        CalculationMethod method = selectedChargeType.getCalculationMethod();
        switch (method) {
            case BILL_ITEM:
                buildFromBillItems();
                break;
            case PHARMACY_BILL:
                buildFromPharmacyBillItems();
                break;
            case STORE_BILL:
                buildFromStoreBillItems();
                break;
            case PATIENT_ROOM:
                buildFromPatientRoom();
                break;
            case BILL_FEE:
                buildFromBillFees();
                break;
            case ADMISSION_FEE:
                buildFromAdmissionFee();
                break;
            default:
                buildFromBillItems();
        }
    }

    // -------------------------------------------------------------------------
    // Encounter filter helper (same logic as InwardChargeTypeDetailController)
    // -------------------------------------------------------------------------

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
            params.put("toDate", toDate);
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
                    params.put("pf", true);
                    break;
                case DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED:
                    jpql.append(" and ").append(encAlias).append(".discharged = :dis");
                    jpql.append(" and ").append(encAlias).append(".paymentFinalized = :pf");
                    params.put("dis", true);
                    params.put("pf", false);
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

    // -------------------------------------------------------------------------
    // Shared pivot builder for BillItem-based cases
    // -------------------------------------------------------------------------

    private void buildPivotFromBillItems(List<BillItem> rawList) {
        // 1. Collect distinct column keys (item.id → item.name); skip null items
        Map<String, String> keyToLabel = new LinkedHashMap<>();
        for (BillItem bi : rawList) {
            if (bi.getItem() == null) {
                continue;
            }
            String key = String.valueOf(bi.getItem().getId());
            keyToLabel.put(key, bi.getItem().getName());
        }

        // 2. Sort entries alphabetically by label (item name)
        List<Map.Entry<String, String>> sortedEntries = new ArrayList<>(keyToLabel.entrySet());
        sortedEntries.sort(Comparator.comparing(e -> e.getValue() == null ? "" : e.getValue()));

        columnKeys   = new ArrayList<>();
        columnLabels = new ArrayList<>();
        for (Map.Entry<String, String> entry : sortedEntries) {
            columnKeys.add(entry.getKey());
            columnLabels.add(entry.getValue());
        }

        // 3. Group by BHT
        Map<String, BhtBreakdownRow> bhtMap = new LinkedHashMap<>();
        for (BillItem bi : rawList) {
            if (bi.getItem() == null || bi.getBill() == null) {
                continue;
            }
            PatientEncounter enc = bi.getBill().getPatientEncounter();
            if (enc == null || enc.getBhtNo() == null) {
                continue;
            }
            String bhtNo = enc.getBhtNo();
            BhtBreakdownRow row = bhtMap.computeIfAbsent(bhtNo, k -> createRow(enc));
            String key = String.valueOf(bi.getItem().getId());
            double net = bi.getNetValue();
            row.getCellValues().merge(key, net, Double::sum);
        }

        // 4. Sort rows by bhtNo and calculate row totals
        rows = new ArrayList<>(bhtMap.values());
        rows.sort(Comparator.comparing(BhtBreakdownRow::getBhtNo,
                Comparator.nullsLast(Comparator.naturalOrder())));
        for (BhtBreakdownRow row : rows) {
            double total = row.getCellValues().values().stream()
                    .mapToDouble(Double::doubleValue).sum();
            row.setRowTotal(total);
        }

        // 5. Calculate column totals and grand total
        columnTotals = new ArrayList<>();
        grandTotal = 0.0;
        for (String key : columnKeys) {
            double colSum = 0;
            for (BhtBreakdownRow row : rows) {
                Double val = row.getCellValues().get(key);
                if (val != null) {
                    colSum += val;
                }
            }
            columnTotals.add(colSum);
            grandTotal += colSum;
        }
    }

    // -------------------------------------------------------------------------
    // BILL_ITEM
    // -------------------------------------------------------------------------

    private void buildFromBillItems() {
        StringBuilder jpql = new StringBuilder(
                "select bi from BillItem bi join bi.bill b join b.patientEncounter enc"
                + " where bi.retired = false"
                + " and b.retired = false"
                + " and b.cancelled = false"
                + " and b.billType in :btps"
                + " and bi.inwardChargeType = :ct");

        Map<String, Object> params = new HashMap<>();
        List<BillType> btps = new ArrayList<>();
        btps.add(BillType.InwardBill);
        btps.add(BillType.InwardOutSideBill);
        params.put("btps", btps);
        params.put("ct", selectedChargeType);
        appendEncounterFilters(jpql, params, "enc");

        List<BillItem> raw = billItemFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        if (raw == null) {
            raw = new ArrayList<>();
        }
        buildPivotFromBillItems(raw);
    }

    // -------------------------------------------------------------------------
    // PHARMACY_BILL
    // -------------------------------------------------------------------------

    private void buildFromPharmacyBillItems() {
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

        List<BillItem> raw = billItemFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        if (raw == null) {
            raw = new ArrayList<>();
        }
        buildPivotFromBillItems(raw);
    }

    // -------------------------------------------------------------------------
    // STORE_BILL
    // -------------------------------------------------------------------------

    private void buildFromStoreBillItems() {
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

        List<BillItem> raw = billItemFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        if (raw == null) {
            raw = new ArrayList<>();
        }
        buildPivotFromBillItems(raw);
    }

    // -------------------------------------------------------------------------
    // PATIENT_ROOM — two fixed columns
    // -------------------------------------------------------------------------

    private void buildFromPatientRoom() {
        columnKeys   = Arrays.asList("TIME_BASED", "SERVICE_ITEMS");
        columnLabels = Arrays.asList("Time-based Charge", "Service Items");

        // Sub-query 1: PatientRoom time-based calculated charges
        StringBuilder jpql1 = new StringBuilder(
                "select pr from PatientRoom pr join pr.patientEncounter enc"
                + " where pr.retired = false");
        Map<String, Object> params1 = new HashMap<>();
        appendEncounterFilters(jpql1, params1, "enc");

        List<PatientRoom> patientRooms = patientRoomFacade.findByJpql(
                jpql1.toString(), params1, TemporalType.TIMESTAMP);

        Map<String, BhtBreakdownRow> bhtMap = new LinkedHashMap<>();
        if (patientRooms != null) {
            for (PatientRoom pr : patientRooms) {
                PatientEncounter enc = pr.getPatientEncounter();
                if (enc == null || enc.getBhtNo() == null) {
                    continue;
                }
                String bhtNo = enc.getBhtNo();
                BhtBreakdownRow row = bhtMap.computeIfAbsent(bhtNo, k -> createRow(enc));
                double charge = extractPatientRoomCharge(pr);
                if (charge != 0.0) {
                    row.getCellValues().merge("TIME_BASED", charge, Double::sum);
                }
            }
        }

        // Sub-query 2: Service BillItems under the same charge type (InwardBill only), summed per BHT
        StringBuilder jpql2 = new StringBuilder(
                "select bi from BillItem bi join bi.bill b join b.patientEncounter enc"
                + " where bi.retired = false"
                + " and b.retired = false"
                + " and b.cancelled = false"
                + " and b.billType = :btp"
                + " and bi.inwardChargeType = :ct");
        Map<String, Object> params2 = new HashMap<>();
        params2.put("btp", BillType.InwardBill);
        params2.put("ct", selectedChargeType);
        appendEncounterFilters(jpql2, params2, "enc");

        List<BillItem> serviceItems = billItemFacade.findByJpql(
                jpql2.toString(), params2, TemporalType.TIMESTAMP);
        if (serviceItems != null) {
            for (BillItem bi : serviceItems) {
                if (bi.getBill() == null) {
                    continue;
                }
                PatientEncounter enc = bi.getBill().getPatientEncounter();
                if (enc == null || enc.getBhtNo() == null) {
                    continue;
                }
                String bhtNo = enc.getBhtNo();
                BhtBreakdownRow row = bhtMap.computeIfAbsent(bhtNo, k -> createRow(enc));
                row.getCellValues().merge("SERVICE_ITEMS", bi.getNetValue(), Double::sum);
            }
        }

        // Sort and finalise
        rows = new ArrayList<>(bhtMap.values());
        rows.sort(Comparator.comparing(BhtBreakdownRow::getBhtNo,
                Comparator.nullsLast(Comparator.naturalOrder())));
        for (BhtBreakdownRow row : rows) {
            double total = row.getCellValues().values().stream()
                    .mapToDouble(Double::doubleValue).sum();
            row.setRowTotal(total);
        }

        columnTotals = new ArrayList<>();
        grandTotal = 0.0;
        for (String key : columnKeys) {
            double colSum = 0;
            for (BhtBreakdownRow row : rows) {
                Double val = row.getCellValues().get(key);
                if (val != null) {
                    colSum += val;
                }
            }
            columnTotals.add(colSum);
            grandTotal += colSum;
        }
    }

    private double extractPatientRoomCharge(PatientRoom pr) {
        switch (selectedChargeType) {
            case RoomCharges:          return pr.getCalculatedRoomCharge();
            case MOCharges:            return pr.getCalculatedMoCharge();
            case NursingCharges:       return pr.getCalculatedNursingCharge();
            case LinenCharges:         return pr.getCalculatedLinenCharge();
            case AdministrationCharge: return pr.getCalculatedAdministrationCharge();
            case MedicalCareICU:       return pr.getCalculatedMedicalCareCharge();
            case MaintainCharges:      return pr.getCalculatedMaintainCharge();
            default:                   return 0.0;
        }
    }

    // -------------------------------------------------------------------------
    // BILL_FEE — pivot on staff name
    // -------------------------------------------------------------------------

    private void buildFromBillFees() {
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
        if (raw == null) {
            raw = new ArrayList<>();
        }

        // Collect distinct staff names as column keys (sorted alphabetically)
        Map<String, String> keyToLabel = new LinkedHashMap<>();
        for (BillFee bf : raw) {
            String key = resolveStaffName(bf);
            keyToLabel.put(key, key);
        }
        List<String> sortedKeys = new ArrayList<>(keyToLabel.keySet());
        sortedKeys.sort(Comparator.nullsLast(Comparator.naturalOrder()));
        columnKeys   = sortedKeys;
        columnLabels = new ArrayList<>(sortedKeys);

        // Group by BHT
        Map<String, BhtBreakdownRow> bhtMap = new LinkedHashMap<>();
        for (BillFee bf : raw) {
            if (bf.getBill() == null) {
                continue;
            }
            PatientEncounter enc = bf.getBill().getPatientEncounter();
            if (enc == null || enc.getBhtNo() == null) {
                continue;
            }
            String bhtNo = enc.getBhtNo();
            BhtBreakdownRow row = bhtMap.computeIfAbsent(bhtNo, k -> createRow(enc));
            String key = resolveStaffName(bf);
            row.getCellValues().merge(key, bf.getFeeValue(), Double::sum);
        }

        rows = new ArrayList<>(bhtMap.values());
        rows.sort(Comparator.comparing(BhtBreakdownRow::getBhtNo,
                Comparator.nullsLast(Comparator.naturalOrder())));
        for (BhtBreakdownRow row : rows) {
            double total = row.getCellValues().values().stream()
                    .mapToDouble(Double::doubleValue).sum();
            row.setRowTotal(total);
        }

        columnTotals = new ArrayList<>();
        grandTotal = 0.0;
        for (String key : columnKeys) {
            double colSum = 0;
            for (BhtBreakdownRow row : rows) {
                Double val = row.getCellValues().get(key);
                if (val != null) {
                    colSum += val;
                }
            }
            columnTotals.add(colSum);
            grandTotal += colSum;
        }
    }

    private String resolveStaffName(BillFee bf) {
        if (bf.getStaff() != null && bf.getStaff().getPerson() != null
                && bf.getStaff().getPerson().getNameWithTitle() != null) {
            return bf.getStaff().getPerson().getNameWithTitle();
        }
        return "Unknown";
    }

    // -------------------------------------------------------------------------
    // ADMISSION_FEE — single fixed column
    // -------------------------------------------------------------------------

    private void buildFromAdmissionFee() {
        columnKeys   = Arrays.asList("ADMISSION_FEE");
        columnLabels = Arrays.asList("Admission Fee");

        StringBuilder jpql = new StringBuilder(
                "select enc from PatientEncounter enc where enc.retired = false");
        Map<String, Object> params = new HashMap<>();
        appendEncounterFilters(jpql, params, "enc");

        List<PatientEncounter> encounters = patientEncounterFacade.findByJpql(
                jpql.toString(), params, TemporalType.TIMESTAMP);

        Map<String, BhtBreakdownRow> bhtMap = new LinkedHashMap<>();
        if (encounters != null) {
            for (PatientEncounter enc : encounters) {
                if (enc.getBhtNo() == null) {
                    continue;
                }
                double fee = (enc.getAdmissionType() != null)
                        ? enc.getAdmissionType().getAdmissionFee() : 0.0;
                if (fee == 0.0) {
                    continue;
                }
                BhtBreakdownRow row = bhtMap.computeIfAbsent(enc.getBhtNo(), k -> createRow(enc));
                row.getCellValues().put("ADMISSION_FEE", fee);
            }
        }

        rows = new ArrayList<>(bhtMap.values());
        rows.sort(Comparator.comparing(BhtBreakdownRow::getBhtNo,
                Comparator.nullsLast(Comparator.naturalOrder())));
        for (BhtBreakdownRow row : rows) {
            Double val = row.getCellValues().get("ADMISSION_FEE");
            row.setRowTotal(val != null ? val : 0.0);
        }

        double colSum = 0;
        for (BhtBreakdownRow row : rows) {
            Double val = row.getCellValues().get("ADMISSION_FEE");
            if (val != null) {
                colSum += val;
            }
        }
        grandTotal = colSum;
        columnTotals = Arrays.asList(colSum);
    }

    // -------------------------------------------------------------------------
    // Helper: create a new BhtBreakdownRow from a PatientEncounter
    // -------------------------------------------------------------------------

    private BhtBreakdownRow createRow(PatientEncounter enc) {
        BhtBreakdownRow row = new BhtBreakdownRow();
        row.setBhtNo(enc.getBhtNo());
        if (enc.getPatient() != null && enc.getPatient().getPerson() != null) {
            row.setPatientName(enc.getPatient().getPerson().getNameWithTitle());
        }
        row.setDateOfAdmission(enc.getDateOfAdmission());
        row.setDateOfDischarge(enc.getDateOfDischarge());
        return row;
    }

    // -------------------------------------------------------------------------
    // Charge type helpers (delegates to ConfigOptionApplicationController)
    // -------------------------------------------------------------------------

    public List<InwardChargeType> getAllChargeTypes() {
        return Arrays.asList(InwardChargeType.values());
    }

    public List<InwardChargeType> completeInwardChargeType(String query) {
        String lower = query == null ? "" : query.toLowerCase();
        List<InwardChargeType> results = new ArrayList<>();
        for (InwardChargeType ct : InwardChargeType.values()) {
            String label = configOptionApplicationController.getInwardChargeTypeLabel(ct);
            if (label == null) {
                label = "";
            }
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
        columnKeys        = null;
        columnLabels      = null;
        rows              = null;
        columnTotals      = null;
        grandTotal        = 0.0;
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

    public List<String> getColumnKeys() { return columnKeys; }
    public List<String> getColumnLabels() { return columnLabels; }
    public List<BhtBreakdownRow> getRows() { return rows; }
    public List<Double> getColumnTotals() { return columnTotals; }
    public double getGrandTotal() { return grandTotal; }
    public String getErrorMessage() { return errorMessage; }
}
