/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.BillListReportDTO;
import com.divudi.core.data.dto.InpatientPharmacyBillItemDTO;
import com.divudi.core.data.dto.InpatientPharmacyDepartmentSummaryDTO;
import com.divudi.core.data.dto.InpatientPharmacyItemSummaryDTO;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Encounter-scoped pharmacy history controller for the redesigned Inpatient
 * Dashboard (issue #21852). Provides 3 filter-free, encounter-scoped lists
 * (Pharmacy Requests, Direct Issues, Issue Returns) that replace 6 old
 * dashboard buttons which wrongly pointed to generic date-filtered,
 * multi-patient Nursing-Workbench search pages. Every list here queries
 * strictly by patientEncounter - no date range, no department filter.
 *
 * Modeled on {@link InwardReportControllerBht}: patientEncounter field,
 * guard-and-fetch-and-navigate methods, DTO-JPQL fetch methods using
 * findLightsByJpqlWithoutCache, and navigateToAdmissionProfile() back-nav.
 *
 * SessionScoped (not ViewScoped): the dashboard button sets patientEncounter
 * via f:setPropertyActionListener then navigates with faces-redirect=true to
 * an entirely new view. A ViewScoped bean is destroyed and recreated for the
 * new view, discarding patientEncounter (and the fetched lists) before the
 * new page can read them - matches InwardReportControllerBht's scope for the
 * same reason.
 *
 * @author Claude Code
 */
@Named
@SessionScoped
public class InwardPharmacyEncounterReportController implements Serializable {

    @EJB
    BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;

    @Inject
    AdmissionController admissionController;

    private PatientEncounter patientEncounter;

    // 1) Pharmacy Requests list (InwardPharmacyRequest bills) - entity-based,
    // since Bill.isFullyIssued() and the nested "issued" bill list are
    // entity-only concerns (not expressible as pure DTO projections).
    private List<Bill> pharmacyRequestBillsToPatientEncounter;
    private double pharmacyRequestBillsToPatientEncounterNetTotal;

    // 2) Direct Issues list (PharmacyBhtPre PreBill sale bills, merged with their
    // RefundBill returns so the whole picture is visible on one page)
    private List<BillListReportDTO> directIssueBillsToPatientEncounter;
    private double directIssueBillsToPatientEncounterNetTotal;
    private double directIssueBillsToPatientEncounterTotal;
    private double directIssueBillsToPatientEncounterMargin;
    private double directIssueBillsToPatientEncounterDiscount;
    private List<InpatientPharmacyBillItemDTO> directIssueBillItemsToPatientEncounter;
    private List<InpatientPharmacyItemSummaryDTO> directIssueItemSummary;
    private List<InpatientPharmacyDepartmentSummaryDTO> directIssueDepartmentSummary;

    // 3) Issue Returns list (PharmacyBhtPre RefundBill bills)
    private List<BillListReportDTO> returnBillsToPatientEncounter;
    private double returnBillsToPatientEncounterNetTotal;
    private List<InpatientPharmacyBillItemDTO> returnBillItemsToPatientEncounter;

    /**
     * Replaces "BHT Issue Requests" + "View Pharmacy Requests". Lists all
     * InwardPharmacyRequest bills for this patientEncounter, each carrying
     * its own isFullyIssued() flag and a per-row "issued sub-bills" list
     * fetched via getIssuedBillsForRequest(billId).
     */
    public String navigateToInpatientPharmacyRequestsList() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No encounter");
            return null;
        }
        pharmacyRequestBillsToPatientEncounter = new ArrayList<>();
        pharmacyRequestBillsToPatientEncounterNetTotal = 0.0;
        try {
            pharmacyRequestBillsToPatientEncounter = fetchPharmacyRequestBills();
            for (Bill b : pharmacyRequestBillsToPatientEncounter) {
                pharmacyRequestBillsToPatientEncounterNetTotal += b.getNetTotal();
            }
        } catch (Exception e) {
            Logger.getLogger(InwardPharmacyEncounterReportController.class.getName())
                    .log(Level.SEVERE, "Error loading pharmacy request bills", e);
            JsfUtil.addErrorMessage("Error loading pharmacy request data");
            return null;
        }
        return "/inward/reports/inpatient_pharmacy_requests_list?faces-redirect=true";
    }

    private List<Bill> fetchPharmacyRequestBills() {
        String jpql = "SELECT b FROM Bill b "
                + "WHERE b.billType = :billType "
                + "AND b.retired = false "
                + "AND b.patientEncounter = :pe "
                + "ORDER BY b.createdAt DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("billType", BillType.InwardPharmacyRequest);
        params.put("pe", patientEncounter);

        List<Bill> result = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        return result != null ? result : new ArrayList<>();
    }

    /**
     * Per-row expansion data for the Pharmacy Requests list: the "issued"
     * sub-bills that reference a given request bill. Same data as
     * InwardReportControllerBht.getBHTIssudBills(Bill) - duplicated here
     * (simple 6-line JPQL) rather than injecting that controller.
     */
    public List<Bill> getIssuedBillsForRequest(Long requestBillId) {
        if (requestBillId == null) {
            return new ArrayList<>();
        }
        String jpql = "SELECT b FROM Bill b "
                + "WHERE b.retired = false "
                + "AND b.billType = :billType "
                + "AND b.referenceBill.id = :refId";
        Map<String, Object> params = new HashMap<>();
        params.put("refId", requestBillId);
        params.put("billType", BillType.PharmacyBhtPre);
        List<Bill> result = billFacade.findByJpql(jpql, params);
        return result != null ? result : new ArrayList<>();
    }

    /**
     * Convenience wrapper so XHTML can bind directly against the request
     * bill's own isFullyIssued() flag without an extra lookup.
     */
    public boolean isRequestFullyIssued(Long billId) {
        if (billId == null) {
            return false;
        }
        Bill b = billFacade.find(billId);
        return b != null && b.isFullyIssued();
    }

    /**
     * Replaces "Search Direct Issues by Bill" + "Search Direct Issues by
     * Item". Lists all PharmacyBhtPre PreBill sale bills for this
     * patientEncounter (bill-level DTO) plus their line items (item-level
     * DTO), so a single page can render both the bill list and the
     * expandable per-bill item rows.
     */
    public String navigateToInpatientPharmacyDirectIssuesList() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No encounter");
            return null;
        }
        directIssueBillsToPatientEncounter = new ArrayList<>();
        directIssueBillsToPatientEncounterNetTotal = 0.0;
        directIssueBillsToPatientEncounterTotal = 0.0;
        directIssueBillsToPatientEncounterMargin = 0.0;
        directIssueBillsToPatientEncounterDiscount = 0.0;
        directIssueBillItemsToPatientEncounter = new ArrayList<>();
        directIssueItemSummary = new ArrayList<>();
        directIssueDepartmentSummary = new ArrayList<>();
        try {
            directIssueBillsToPatientEncounter = fetchDirectIssueBills();
            for (BillListReportDTO dto : directIssueBillsToPatientEncounter) {
                if (dto.getNetTotal() != null) {
                    directIssueBillsToPatientEncounterNetTotal += dto.getNetTotal().doubleValue();
                }
                if (dto.getTotal() != null) {
                    directIssueBillsToPatientEncounterTotal += dto.getTotal().doubleValue();
                }
                if (dto.getServiceCharge() != null) {
                    directIssueBillsToPatientEncounterMargin += dto.getServiceCharge().doubleValue();
                }
                if (dto.getDiscount() != null) {
                    directIssueBillsToPatientEncounterDiscount += dto.getDiscount().doubleValue();
                }
            }

            List<Long> billIds = new ArrayList<>();
            for (BillListReportDTO dto : directIssueBillsToPatientEncounter) {
                billIds.add(dto.getBillId());
            }
            directIssueBillItemsToPatientEncounter = fetchDirectIssueBillItems(billIds);
            directIssueItemSummary = buildItemSummary(directIssueBillItemsToPatientEncounter);
            directIssueDepartmentSummary = buildDepartmentSummary(directIssueBillItemsToPatientEncounter);
        } catch (Exception e) {
            Logger.getLogger(InwardPharmacyEncounterReportController.class.getName())
                    .log(Level.SEVERE, "Error loading direct issue bills", e);
            JsfUtil.addErrorMessage("Error loading direct issue data");
            return null;
        }
        return "/inward/reports/inpatient_pharmacy_direct_issues_list?faces-redirect=true";
    }

    private List<BillListReportDTO> fetchDirectIssueBills() {
        // Bill.retired/cancelled/refunded are primitive boolean and
        // total/discount/netTotal/margin are primitive double - project them
        // directly (no COALESCE) to keep EclipseLink's reflective DTO
        // constructor binding unambiguous. See BillListReportDTO comments.
        String jpql = "SELECT new com.divudi.core.data.dto.BillListReportDTO("
                + "b.id, "
                + "COALESCE(b.deptId, ''), "
                + "b.billTypeAtomic, "
                + "b.paymentMethod, "
                + "COALESCE(b.patientEncounter.patient.person.name, ''), "
                + "b.createdAt, "
                + "COALESCE(b.creater.name, ''), "
                + "b.retired, "
                + "b.cancelled, "
                + "b.refunded, "
                + "b.total, "
                + "b.discount, "
                + "b.netTotal, "
                + "COALESCE(b.patientEncounter.bhtNo, ''), "
                + "COALESCE(b.deptId, ''), "
                + "b.margin) "
                + "FROM Bill b "
                + "WHERE type(b) = PreBill "
                + "AND b.billType = :billType "
                + "AND b.retired = false "
                + "AND b.patientEncounter = :pe "
                + "ORDER BY b.createdAt DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("billType", BillType.PharmacyBhtPre);
        params.put("pe", patientEncounter);

        List<BillListReportDTO> result = (List<BillListReportDTO>) billFacade
                .findLightsByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);
        List<BillListReportDTO> issues = result != null ? result : new ArrayList<>();

        // Merge in returns of these direct issues (RefundBill rows created by
        // BhtIssueReturnController.settle()) so the whole picture - issues and
        // their returns - is visible on one list, per issue #21871.
        List<BillListReportDTO> returns = fetchDirectIssueReturnBills();
        List<BillListReportDTO> merged = new ArrayList<>(issues.size() + returns.size());
        merged.addAll(issues);
        merged.addAll(returns);
        merged.sort(Comparator.comparing(BillListReportDTO::getCreatedAt).reversed());
        return merged;
    }

    private List<BillListReportDTO> fetchDirectIssueReturnBills() {
        // Same DTO shape as fetchReturnBills() (17-arg constructor incl.
        // referenceBillNumber = the original issue bill's printed number), but
        // scoped only to the two direct-issue return atomics so ward
        // issue-on-request returns are not pulled into this report.
        String jpql = "SELECT new com.divudi.core.data.dto.BillListReportDTO("
                + "b.id, "
                + "COALESCE(b.deptId, ''), "
                + "b.billTypeAtomic, "
                + "b.paymentMethod, "
                + "COALESCE(b.patientEncounter.patient.person.name, ''), "
                + "b.createdAt, "
                + "COALESCE(b.creater.name, ''), "
                + "b.retired, "
                + "b.cancelled, "
                + "b.refunded, "
                + "b.total, "
                + "b.discount, "
                + "b.netTotal, "
                + "COALESCE(b.patientEncounter.bhtNo, ''), "
                + "COALESCE(b.deptId, ''), "
                + "b.margin, "
                + "COALESCE(b.billedBill.deptId, '')) "
                + "FROM RefundBill b "
                + "WHERE b.billType = :billType "
                + "AND b.billTypeAtomic IN :returnAtomics "
                + "AND b.retired = false "
                + "AND b.patientEncounter = :pe "
                + "ORDER BY b.createdAt DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("billType", BillType.PharmacyBhtPre);
        params.put("returnAtomics", Arrays.asList(
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN));
        params.put("pe", patientEncounter);

        List<BillListReportDTO> result = (List<BillListReportDTO>) billFacade
                .findLightsByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);
        return result != null ? result : new ArrayList<>();
    }

    private List<InpatientPharmacyBillItemDTO> fetchDirectIssueBillItems(List<Long> billIds) {
        if (billIds == null || billIds.isEmpty()) {
            return new ArrayList<>();
        }
        String jpql = "SELECT new com.divudi.core.data.dto.InpatientPharmacyBillItemDTO("
                + "bi.bill.id, "
                + "bi.item.name, "
                + "COALESCE(bi.item.code, ''), "
                + "bi.qty, "
                + "bi.grossValue, "
                + "bi.marginValue, "
                + "bi.discount, "
                + "bi.netValue, "
                + "COALESCE(bi.bill.department.name, '')) "
                + "FROM BillItem bi "
                + "WHERE bi.bill.id IN :billIds "
                + "AND bi.retired = false "
                + "ORDER BY bi.bill.id, bi.id";

        Map<String, Object> params = new HashMap<>();
        params.put("billIds", billIds);

        List<InpatientPharmacyBillItemDTO> result = (List<InpatientPharmacyBillItemDTO>) billItemFacade
                .findLightsByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);
        return result != null ? result : new ArrayList<>();
    }

    /**
     * In-memory aggregation (no new query) grouping the already-fetched
     * encounter item list by item name+code, for the Items Summary panel
     * (issue #21871). Includes both direct-issue and return line items, so
     * the totals reflect net consumption.
     */
    private List<InpatientPharmacyItemSummaryDTO> buildItemSummary(List<InpatientPharmacyBillItemDTO> items) {
        Map<String, InpatientPharmacyItemSummaryDTO> byItem = new LinkedHashMap<>();
        for (InpatientPharmacyBillItemDTO item : items) {
            String key = item.getItemName() + "|" + item.getItemCode();
            InpatientPharmacyItemSummaryDTO summary = byItem.get(key);
            if (summary == null) {
                summary = new InpatientPharmacyItemSummaryDTO(item.getItemName(), item.getItemCode());
                byItem.put(key, summary);
            }
            summary.setQty(summary.getQty() + nz(item.getQty()));
            summary.setTotal(summary.getTotal() + nz(item.getTotal()));
            summary.setMargin(summary.getMargin() + nz(item.getMargin()));
            summary.setDiscount(summary.getDiscount() + nz(item.getDiscount()));
            summary.setNetTotal(summary.getNetTotal() + nz(item.getNetValue()));
        }
        return new ArrayList<>(byItem.values());
    }

    /**
     * In-memory aggregation (no new query) grouping the already-fetched
     * encounter item list by issuing department, for the Issuing Department
     * Summary panel (issue #21871).
     */
    private List<InpatientPharmacyDepartmentSummaryDTO> buildDepartmentSummary(List<InpatientPharmacyBillItemDTO> items) {
        Map<String, InpatientPharmacyDepartmentSummaryDTO> byDept = new LinkedHashMap<>();
        for (InpatientPharmacyBillItemDTO item : items) {
            String key = item.getDepartmentName();
            InpatientPharmacyDepartmentSummaryDTO summary = byDept.get(key);
            if (summary == null) {
                summary = new InpatientPharmacyDepartmentSummaryDTO(key);
                byDept.put(key, summary);
            }
            summary.setQty(summary.getQty() + nz(item.getQty()));
            summary.setTotal(summary.getTotal() + nz(item.getTotal()));
            summary.setMargin(summary.getMargin() + nz(item.getMargin()));
            summary.setDiscount(summary.getDiscount() + nz(item.getDiscount()));
            summary.setNetTotal(summary.getNetTotal() + nz(item.getNetValue()));
        }
        return new ArrayList<>(byDept.values());
    }

    private double nz(Double value) {
        return value != null ? value : 0.0;
    }

    /**
     * In-memory filter (no new query) for the per-row expansion on the
     * Direct Issues list - the full item list was already fetched once for
     * the whole encounter.
     */
    public List<InpatientPharmacyBillItemDTO> getItemsForDirectIssueBill(Long billId) {
        if (billId == null || directIssueBillItemsToPatientEncounter == null) {
            return new ArrayList<>();
        }
        List<InpatientPharmacyBillItemDTO> result = new ArrayList<>();
        for (InpatientPharmacyBillItemDTO dto : directIssueBillItemsToPatientEncounter) {
            if (billId.equals(dto.getBillId())) {
                result.add(dto);
            }
        }
        return result;
    }

    /**
     * Replaces "Search Issue Returns by Bill" + "Search Issue Returns by
     * Item" (the latter never existed before - built fresh here). Lists all
     * PharmacyBhtPre RefundBill bills for this patientEncounter plus their
     * returned line items.
     */
    public String navigateToInpatientPharmacyReturnsList() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No encounter");
            return null;
        }
        returnBillsToPatientEncounter = new ArrayList<>();
        returnBillsToPatientEncounterNetTotal = 0.0;
        returnBillItemsToPatientEncounter = new ArrayList<>();
        try {
            returnBillsToPatientEncounter = fetchReturnBills();
            for (BillListReportDTO dto : returnBillsToPatientEncounter) {
                if (dto.getNetTotal() != null) {
                    returnBillsToPatientEncounterNetTotal += dto.getNetTotal().doubleValue();
                }
            }

            List<Long> billIds = new ArrayList<>();
            for (BillListReportDTO dto : returnBillsToPatientEncounter) {
                billIds.add(dto.getBillId());
            }
            returnBillItemsToPatientEncounter = fetchReturnBillItems(billIds);
        } catch (Exception e) {
            Logger.getLogger(InwardPharmacyEncounterReportController.class.getName())
                    .log(Level.SEVERE, "Error loading issue return bills", e);
            JsfUtil.addErrorMessage("Error loading issue return data");
            return null;
        }
        return "/inward/reports/inpatient_pharmacy_returns_list?faces-redirect=true";
    }

    private List<BillListReportDTO> fetchReturnBills() {
        String jpql = "SELECT new com.divudi.core.data.dto.BillListReportDTO("
                + "b.id, "
                + "COALESCE(b.deptId, ''), "
                + "b.billTypeAtomic, "
                + "b.paymentMethod, "
                + "COALESCE(b.patientEncounter.patient.person.name, ''), "
                + "b.createdAt, "
                + "COALESCE(b.creater.name, ''), "
                + "b.retired, "
                + "b.cancelled, "
                + "b.refunded, "
                + "b.total, "
                + "b.discount, "
                + "b.netTotal, "
                + "COALESCE(b.patientEncounter.bhtNo, ''), "
                + "COALESCE(b.deptId, ''), "
                + "b.margin, "
                + "COALESCE(b.billedBill.deptId, '')) "
                + "FROM RefundBill b "
                + "WHERE b.billType = :billType "
                + "AND b.retired = false "
                + "AND b.patientEncounter = :pe "
                + "ORDER BY b.createdAt DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("billType", BillType.PharmacyBhtPre);
        params.put("pe", patientEncounter);

        List<BillListReportDTO> result = (List<BillListReportDTO>) billFacade
                .findLightsByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);
        return result != null ? result : new ArrayList<>();
    }

    private List<InpatientPharmacyBillItemDTO> fetchReturnBillItems(List<Long> billIds) {
        if (billIds == null || billIds.isEmpty()) {
            return new ArrayList<>();
        }
        // Returned quantities/values are stored as negative on BillItem for
        // return bills already (consistent with how
        // InwardReportControllerBht.fetchPharmacyIssueDtos/isCancellationOrReturn
        // treat *_RETURN types) - project as-is, no extra sign handling here.
        String jpql = "SELECT new com.divudi.core.data.dto.InpatientPharmacyBillItemDTO("
                + "bi.bill.id, "
                + "bi.item.name, "
                + "COALESCE(bi.item.code, ''), "
                + "bi.qty, "
                + "bi.netValue) "
                + "FROM BillItem bi "
                + "WHERE bi.bill.id IN :billIds "
                + "AND bi.retired = false "
                + "ORDER BY bi.bill.id, bi.id";

        Map<String, Object> params = new HashMap<>();
        params.put("billIds", billIds);

        List<InpatientPharmacyBillItemDTO> result = (List<InpatientPharmacyBillItemDTO>) billItemFacade
                .findLightsByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);
        return result != null ? result : new ArrayList<>();
    }

    /**
     * In-memory filter (no new query) for the per-row expansion on the Issue
     * Returns list.
     */
    public List<InpatientPharmacyBillItemDTO> getItemsForReturnBill(Long billId) {
        if (billId == null || returnBillItemsToPatientEncounter == null) {
            return new ArrayList<>();
        }
        List<InpatientPharmacyBillItemDTO> result = new ArrayList<>();
        for (InpatientPharmacyBillItemDTO dto : returnBillItemsToPatientEncounter) {
            if (billId.equals(dto.getBillId())) {
                result.add(dto);
            }
        }
        return result;
    }

    public String navigateToAdmissionProfile() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No encounter selected");
            return null;
        }
        if (patientEncounter instanceof Admission) {
            admissionController.setCurrent((Admission) patientEncounter);
        }
        return "/inward/admission_profile?faces-redirect=true";
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public List<Bill> getPharmacyRequestBillsToPatientEncounter() {
        return pharmacyRequestBillsToPatientEncounter;
    }

    public void setPharmacyRequestBillsToPatientEncounter(List<Bill> pharmacyRequestBillsToPatientEncounter) {
        this.pharmacyRequestBillsToPatientEncounter = pharmacyRequestBillsToPatientEncounter;
    }

    public double getPharmacyRequestBillsToPatientEncounterNetTotal() {
        return pharmacyRequestBillsToPatientEncounterNetTotal;
    }

    public void setPharmacyRequestBillsToPatientEncounterNetTotal(double pharmacyRequestBillsToPatientEncounterNetTotal) {
        this.pharmacyRequestBillsToPatientEncounterNetTotal = pharmacyRequestBillsToPatientEncounterNetTotal;
    }

    public List<BillListReportDTO> getDirectIssueBillsToPatientEncounter() {
        return directIssueBillsToPatientEncounter;
    }

    public void setDirectIssueBillsToPatientEncounter(List<BillListReportDTO> directIssueBillsToPatientEncounter) {
        this.directIssueBillsToPatientEncounter = directIssueBillsToPatientEncounter;
    }

    public double getDirectIssueBillsToPatientEncounterNetTotal() {
        return directIssueBillsToPatientEncounterNetTotal;
    }

    public void setDirectIssueBillsToPatientEncounterNetTotal(double directIssueBillsToPatientEncounterNetTotal) {
        this.directIssueBillsToPatientEncounterNetTotal = directIssueBillsToPatientEncounterNetTotal;
    }

    public double getDirectIssueBillsToPatientEncounterTotal() {
        return directIssueBillsToPatientEncounterTotal;
    }

    public void setDirectIssueBillsToPatientEncounterTotal(double directIssueBillsToPatientEncounterTotal) {
        this.directIssueBillsToPatientEncounterTotal = directIssueBillsToPatientEncounterTotal;
    }

    public double getDirectIssueBillsToPatientEncounterMargin() {
        return directIssueBillsToPatientEncounterMargin;
    }

    public void setDirectIssueBillsToPatientEncounterMargin(double directIssueBillsToPatientEncounterMargin) {
        this.directIssueBillsToPatientEncounterMargin = directIssueBillsToPatientEncounterMargin;
    }

    public double getDirectIssueBillsToPatientEncounterDiscount() {
        return directIssueBillsToPatientEncounterDiscount;
    }

    public void setDirectIssueBillsToPatientEncounterDiscount(double directIssueBillsToPatientEncounterDiscount) {
        this.directIssueBillsToPatientEncounterDiscount = directIssueBillsToPatientEncounterDiscount;
    }

    public List<InpatientPharmacyBillItemDTO> getDirectIssueBillItemsToPatientEncounter() {
        return directIssueBillItemsToPatientEncounter;
    }

    public void setDirectIssueBillItemsToPatientEncounter(List<InpatientPharmacyBillItemDTO> directIssueBillItemsToPatientEncounter) {
        this.directIssueBillItemsToPatientEncounter = directIssueBillItemsToPatientEncounter;
    }

    public List<InpatientPharmacyItemSummaryDTO> getDirectIssueItemSummary() {
        return directIssueItemSummary;
    }

    public void setDirectIssueItemSummary(List<InpatientPharmacyItemSummaryDTO> directIssueItemSummary) {
        this.directIssueItemSummary = directIssueItemSummary;
    }

    public List<InpatientPharmacyDepartmentSummaryDTO> getDirectIssueDepartmentSummary() {
        return directIssueDepartmentSummary;
    }

    public void setDirectIssueDepartmentSummary(List<InpatientPharmacyDepartmentSummaryDTO> directIssueDepartmentSummary) {
        this.directIssueDepartmentSummary = directIssueDepartmentSummary;
    }

    public List<BillListReportDTO> getReturnBillsToPatientEncounter() {
        return returnBillsToPatientEncounter;
    }

    public void setReturnBillsToPatientEncounter(List<BillListReportDTO> returnBillsToPatientEncounter) {
        this.returnBillsToPatientEncounter = returnBillsToPatientEncounter;
    }

    public double getReturnBillsToPatientEncounterNetTotal() {
        return returnBillsToPatientEncounterNetTotal;
    }

    public void setReturnBillsToPatientEncounterNetTotal(double returnBillsToPatientEncounterNetTotal) {
        this.returnBillsToPatientEncounterNetTotal = returnBillsToPatientEncounterNetTotal;
    }

    public List<InpatientPharmacyBillItemDTO> getReturnBillItemsToPatientEncounter() {
        return returnBillItemsToPatientEncounter;
    }

    public void setReturnBillItemsToPatientEncounter(List<InpatientPharmacyBillItemDTO> returnBillItemsToPatientEncounter) {
        this.returnBillItemsToPatientEncounter = returnBillItemsToPatientEncounter;
    }
}
