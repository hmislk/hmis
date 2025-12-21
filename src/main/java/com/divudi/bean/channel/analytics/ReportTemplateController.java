/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.channel.analytics;

import com.divudi.bean.common.*;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.PaymentType;
import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.data.ReportTemplateRowBundle;
import com.divudi.core.data.analytics.ReportTemplateColumn;
import com.divudi.core.data.analytics.ReportTemplateFilter;
import com.divudi.core.data.analytics.ReportTemplateType;
import static com.divudi.core.data.analytics.ReportTemplateType.ITEM_SUMMARY_BY_BILL;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.ReportTemplate;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.ReportTemplateFacade;
import com.divudi.core.util.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Consultant
 * in Health Informatics
 */
@Named
@SessionScoped
public class ReportTemplateController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private ReportTemplateFacade ejbFacade;
    private ReportTemplate current;
    private List<ReportTemplate> items = null;

    private Date date;
    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Department department;
    private WebUser user;
    private Staff staff;
    private Long startId;
    private Long endId;

    private Institution creditCompany;
    private Institution fromInstitution;
    private Department fromDepartment;
    private Institution toInstitution;
    private Department toDepartment;

    private List<ReportTemplateRow> ReportTemplateRows;
    private ReportTemplateRowBundle reportTemplateRowBundle;

    public void save(ReportTemplate reportTemplate) {
        if (reportTemplate == null) {
            return;
        }
        if (reportTemplate.getId() != null) {
            getFacade().edit(reportTemplate);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            reportTemplate.setCreatedAt(new Date());
            reportTemplate.setCreater(getSessionController().getLoggedUser());
            getFacade().create(reportTemplate);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
    }

    public ReportTemplate findReportTemplateByName(String name) {
        if (name == null) {
            return null;
        }
        if (name.trim().equals("")) {
            return null;
        }
        String jpql = "select a "
                + " from ReportTemplate a "
                + " where a.retired=:ret "
                + " and a.name=:n";
        Map m = new HashMap<>();
        m.put("ret", false);
        m.put("n", name);
        return getFacade().findFirstByJpql(jpql, m);
    }

    public List<ReportTemplate> completeReportTemplate(String qry) {
        List<ReportTemplate> list;
        String jpql;
        HashMap<String, Object> params = new HashMap<>();
        jpql = "SELECT c FROM ReportTemplate c "
                + "WHERE c.retired = false "
                + "AND (LOWER(c.name) LIKE :q OR LOWER(c.code) LIKE :q) "
                + "ORDER BY c.name";
        params.put("q", "%" + qry.toLowerCase() + "%");
        list = getFacade().findByJpql(jpql, params);

        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public List<ReportTemplate> completeRootReportTemplate(String qry) {
        List<ReportTemplate> list;
        String jpql;
        HashMap<String, Object> params = new HashMap<>();
        jpql = "SELECT c FROM ReportTemplate c "
                + "WHERE c.retired = false "
                + "AND c.parent is null "
                + "AND (LOWER(c.name) LIKE :q OR LOWER(c.code) LIKE :q) "
                + "ORDER BY c.name";
        params.put("q", "%" + qry.toLowerCase() + "%");
        list = getFacade().findByJpql(jpql, params);

        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public void prepareAdd() {
        current = new ReportTemplate();
    }

    public void recreateModel() {
        items = null;
    }

    public void processOpdItemCounts() {
        ReportTemplateType trr = ITEM_SUMMARY_BY_BILL;
        List<BillTypeAtomic> billTypesAtomics = new ArrayList<>();
        billTypesAtomics.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        billTypesAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);
        billTypesAtomics.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);

        reportTemplateRowBundle = new ReportTemplateRowBundle();
        reportTemplateRowBundle.setName("OPD Services by Categories & Items");
        reportTemplateRowBundle = generateReport(
                trr,
                billTypesAtomics,
                null,
                fromDate,
                toDate,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

    }

    public ReportTemplateRowBundle generateValueByDepartmentReport(
            List<BillTypeAtomic> btas,
            List<PaymentMethod> paymentMethods,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramSite) {

        ReportTemplateRowBundle pb = new ReportTemplateRowBundle();

        Map<String, Object> parameters = new HashMap<>();

        String jpql = "select new com.divudi.core.data.ReportTemplateRow("
                + " bill.department, sum(bill.netTotal)) "
                + " from Payment p"
                + " join p.bill bill "
                + " where bill.retired=false "
                + " and p.retired=false";

        if (btas != null && !btas.isEmpty()) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paymentMethods != null && !paymentMethods.isEmpty()) {
            jpql += " and p.paymentMethod in :pms ";
            parameters.put("pms", paymentMethods);
        }

        if (paramFromDate != null) {
            jpql += " and bill.createdAt >= :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.createdAt <= :td ";
            parameters.put("td", paramToDate);
        }

        if (paramInstitution != null) {
            jpql += " and bill.department.institution = :ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department = :dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramSite != null) {
            jpql += " and bill.department.site = :site ";
            parameters.put("site", paramSite);
        }

        jpql += " group by bill.department";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);
        
        // Assuming you have an EJB or similar service to run the query
        List<ReportTemplateRow> results = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        
        // Properly handle empty or null results
        if (results == null || results.isEmpty()) {
            return pb; // Consider returning an empty ReportTemplateRowBundle instead
        }
        pb.setReportTemplateRows(results);
        return pb;
    }

    public ReportTemplateRowBundle generateBillReport(
            List<BillTypeAtomic> btas,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramSite,
            Boolean excludeCredit,
            Boolean creditOnly) {
        System.out.println("generateBillReport = ");
        System.out.println("creditOnly = " + creditOnly);
        System.out.println("excludeCredit = " + excludeCredit);

        ReportTemplateRowBundle pb = new ReportTemplateRowBundle();

        Map<String, Object> parameters = new HashMap<>();

        String jpql = "select new com.divudi.core.data.ReportTemplateRow("
                + " bill) "
                + " from Bill bill "
                + " left join fetch bill.patient "
                + " left join fetch bill.patient.person "
                + " where bill.retired=false ";

        if (excludeCredit != null && excludeCredit) {
            List<PaymentMethod> pms;
            pms = PaymentMethod.getMethodsByType(PaymentType.NON_CREDIT);
            jpql += " and bill.paymentMethod in :pms";
            parameters.put("pms", pms);
        }
        if (creditOnly != null && creditOnly) {
            List<PaymentMethod> pms;
            pms = PaymentMethod.getMethodsByType(PaymentType.CREDIT);
            jpql += " and bill.paymentMethod in :pms";
            parameters.put("pms", pms);
        }

        if (btas != null && !btas.isEmpty()) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paramFromDate != null) {
            jpql += " and bill.createdAt >= :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.createdAt <= :td ";
            parameters.put("td", paramToDate);
        }

        if (paramInstitution != null) {
            jpql += " and bill.department.institution = :ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department = :dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramSite != null) {
            jpql += " and bill.department.site = :site ";
            parameters.put("site", paramSite);
        }

        System.out.println("jpql = " + jpql);
        System.out.println("DEBUG: Parameters = " + parameters);

        // First, let's count how many bills match the criteria
        String countJpql = jpql.replace("select new com.divudi.core.data.ReportTemplateRow( bill) from Bill bill left join fetch bill.patient left join fetch bill.patient.person", "select count(bill) from Bill bill");
        System.out.println("DEBUG: Count JPQL = " + countJpql);

        try {
            Long billCount = (Long) ejbFacade.findSingleByJpql(countJpql, parameters, TemporalType.TIMESTAMP);
            System.out.println("DEBUG: Bills matching criteria count = " + billCount);

            if (billCount == 0) {
                System.out.println("DEBUG: No bills found matching criteria - returning empty bundle");
                return pb;
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting count: " + e.getMessage());
            e.printStackTrace();
        }

        // Now get the actual results
        System.out.println("DEBUG: Executing main query to get ReportTemplateRow DTOs");
        List<ReportTemplateRow> results = null;
        try {
            results = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);
            System.out.println("DEBUG: Query executed successfully. Results size = " + (results != null ? results.size() : "null"));
        } catch (Exception e) {
            System.out.println("DEBUG: Error executing DTO query: " + e.getMessage());
            e.printStackTrace();
            return pb;
        }

        // Properly handle empty or null results
        if (results == null || results.isEmpty()) {
            System.out.println("DEBUG: Results are null or empty - returning empty bundle");
            return pb; // Consider returning an empty ReportTemplateRowBundle instead
        }
        pb.setReportTemplateRows(results);

        double bundleTotal = pb.getReportTemplateRows().stream()
                .mapToDouble(r -> r.getBill().getNetTotal())
                .sum();
        pb.setTotal(bundleTotal);

        return pb;
    }

    public ReportTemplateRowBundle generateBillReportWithoutProfessionalFees(
            List<BillTypeAtomic> btas,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramSite) {

        ReportTemplateRowBundle pb = new ReportTemplateRowBundle();

        Map<String, Object> parameters = new HashMap<>();

        String jpql = "select new com.divudi.core.data.ReportTemplateRow("
                + " bill) "
                + " from Bill bill "
                + " where bill.retired=false ";

        if (btas != null && !btas.isEmpty()) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paramFromDate != null) {
            jpql += " and bill.createdAt >= :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.createdAt <= :td ";
            parameters.put("td", paramToDate);
        }

        if (paramInstitution != null) {
            jpql += " and bill.department.institution = :ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department = :dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramSite != null) {
            jpql += " and bill.department.site = :site ";
            parameters.put("site", paramSite);
        }

        jpql += " group by bill";

        // Assuming you have an EJB or similar service to run the query
        List<ReportTemplateRow> results = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        // Properly handle empty or null results
        if (results == null || results.isEmpty()) {
            return pb; // Consider returning an empty ReportTemplateRowBundle instead
        }
        pb.setReportTemplateRows(results);

        double bundleTotal = pb.getReportTemplateRows().stream()
                .mapToDouble(r -> r.getBill().getNetTotal())
                .sum();
        pb.setTotal(bundleTotal);

        return pb;
    }

    public ReportTemplateRowBundle generateBillReportWithoutProfessionalFees(
            List<BillTypeAtomic> btas,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramSite,
            Boolean creditBillsOnly) {

        ReportTemplateRowBundle pb = new ReportTemplateRowBundle();

        Map<String, Object> parameters = new HashMap<>();

        String jpql = "select new com.divudi.core.data.ReportTemplateRow("
                + " bill) "
                + " from Bill bill "
                + " where bill.retired=false ";

        if (btas != null && !btas.isEmpty()) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (creditBillsOnly != null && creditBillsOnly) {
            jpql += " and bill.paymentMethod in :pms ";
            parameters.put("pms", PaymentMethod.getMethodsByType(PaymentType.NON_CREDIT));
        }

        if (paramFromDate != null) {
            jpql += " and bill.createdAt >= :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.createdAt <= :td ";
            parameters.put("td", paramToDate);
        }

        if (paramInstitution != null) {
            jpql += " and bill.department.institution = :ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department = :dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramSite != null) {
            jpql += " and bill.department.site = :site ";
            parameters.put("site", paramSite);
        }

        jpql += " group by bill";

        // Assuming you have an EJB or similar service to run the query
        List<ReportTemplateRow> results = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        // Properly handle empty or null results
        if (results == null || results.isEmpty()) {
            return pb; // Consider returning an empty ReportTemplateRowBundle instead
        }
        pb.setReportTemplateRows(results);
        double bundleTotal = pb.getReportTemplateRows().stream()
                .mapToDouble(r -> r.getBill().getNetTotal() - r.getBill().getProfessionalFee())
                .sum();
        pb.setTotal(bundleTotal);

        return pb;
    }

    public ReportTemplateRowBundle generatePaymentReport(
            PaymentMethod pm,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramSite) {

        ReportTemplateRowBundle pb = new ReportTemplateRowBundle();

        Map<String, Object> parameters = new HashMap<>();

        String jpql = "select new com.divudi.core.data.ReportTemplateRow("
                + " p) "
                + " from Payment p "
                + " join p.bill bill "
                + " where bill.retired=false "
                + " and p.retired=false ";

        if (pm != null) {
            jpql += " and p.paymentMethod=:pm ";
            parameters.put("pm", pm);
        }

        if (paramFromDate != null) {
            jpql += " and bill.createdAt >= :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.createdAt <= :td ";
            parameters.put("td", paramToDate);
        }

        if (paramInstitution != null) {
            jpql += " and bill.department.institution = :ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department = :dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramSite != null) {
            jpql += " and bill.department.site = :site ";
            parameters.put("site", paramSite);
        }

        jpql += " group by p";

        // Assuming you have an EJB or similar service to run the query
        List<ReportTemplateRow> results = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        // Properly handle empty or null results
        if (results == null || results.isEmpty()) {
            return pb; // Consider returning an empty ReportTemplateRowBundle instead
        }
        pb.setReportTemplateRows(results);

        double bundleTotal = 0.0; // Initialize total

        for (ReportTemplateRow r : pb.getReportTemplateRows()) {
            // Check if Payment, Bill, and PaidValue are not null
            if (r.getPayment() != null && r.getBill() != null) {
                // Get the absolute value of the paid amount
                double paidValue = Math.abs(r.getPayment().getPaidValue());

                // Add the value for Bill or BilledBill
                if (r.getBill().getBillClassType() == BillClassType.Bill
                        || r.getBill().getBillClassType() == BillClassType.BilledBill) {
                    bundleTotal += paidValue;
                } // Subtract the value for CancelledBill or RefundBill
                else if (r.getBill().getBillClassType() == BillClassType.CancelledBill
                        || r.getBill().getBillClassType() == BillClassType.RefundBill) {
                    bundleTotal -= paidValue;
                }
                // Do nothing for other BillClassTypes
            }
            // If Payment, Bill, or PaidValue is null, skip the row (this else is optional)
        }

        pb.setTotal(bundleTotal);

        return pb;
    }

    public ReportTemplateRowBundle generateReport(
            ReportTemplateType type,
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        ReportTemplateRowBundle bundle;

        switch (type) {
            case BILL_NET_TOTAL:
                bundle = handleBillTypeAtomicTotalUsingBills(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case BILLT_TYPE_AND_PAYMENT_METHOD_SUMMARY_PAYMENTS:
                bundle = handleBillTypeAndPaymentMethodSummaryPayments(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case BILLT_TYPE_AND_PAYMENT_METHOD_SUMMARY_USING_BILLS:
                bundle = handleBillTypeAndPaymentMethodSummaryUsingBills(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case BILL_TYPE_ATOMIC_SUMMARY_USING_FEES:
                bundle = handleBillFeeGroupedByBillTypeAtomic(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case BILL_FEE_GROUPED_BY_TO_DEPARTMENT_AND_CATEGORY:
                bundle = handleBillFeeGroupedByToDepartmentAndCategory(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case BILL_FEE_LIST:
                bundle = handleBillFeeList(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case BILL_ITEM_LIST:
                bundle = handleBillItemList(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case BILL_LIST:
                bundle = handleBillList(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case BILL_TYPE_ATOMIC_SUMMARY_USING_BILLS:
                bundle = handleBillTypeAtomicSummaryUsingBills(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case BILL_TYPE_ATOMIC_SUMMARY_USING_PAYMENTS:
                bundle = handleBillTypeAtomicSummaryUsingPayments(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case ENCOUNTER_LIST:
                bundle = handleEncounterList(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case PATIENT_LIST:
                bundle = handlePatientList(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case PAYMENT_METHOD_SUMMARY_USING_BILLS:
                bundle = handlePaymentMethodSummaryUsingBills(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case PAYMENT_METHOD_SUMMARY_USING_PAYMENTS:
                bundle = handlePaymentMethodSummaryUsingPayments(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case PAYMENT_TYPE_SUMMARY_PAYMENTS:
                bundle = handlePaymentTypeSummaryPayments(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case PAYMENT_TYPE_SUMMARY_USING_BILLS:
                bundle = handlePaymentTypeSummaryUsingBills(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case ITEM_CATEGORY_SUMMARY_BY_BILL_FEE:
                bundle = handleItemCategorySummaryByBillFee(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case ITEM_SUMMARY_BY_BILL:
                bundle = handleItemSummaryByBill(btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case ITEM_DEPARTMENT_SUMMARY_BY_BILL_ITEM:
                bundle = handleItemDepartmentummaryByBill(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case ITEM_CATEGORY_SUMMARY_BY_BILL_ITEM:
            case ITEM_CATEGORY_SUMMARY_BY_BILL:
                bundle = handleItemCategorySummaryByBill(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case TO_DEPARTMENT_SUMMARY_BY_BILL_FEE:
                bundle = handleToDepartmentSummaryByBillFee(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case TO_DEPARTMENT_SUMMARY_BY_BILL_ITEM:
                bundle = handleToDepartmentSummaryByBillItem(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;

            case TO_DEPARTMENT_SUMMARY_BY_BILL:
                bundle = handleToDepartmentSummaryByBill(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            case SESSION_INSTANCE_LIST:
                bundle = handleSessionInstanceList(
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId);
                break;
            default:
                JsfUtil.addErrorMessage("Unknown Report Type");
                return null;
        }
        return bundle;
    }

    public String processReport() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return null;
        }
        if (current.getReportTemplateType() == null) {
            JsfUtil.addErrorMessage("No report Type");
            return "";
        }
        reportTemplateRowBundle = generateReport(current.getReportTemplateType(), current.getBillTypeAtomics(), date, fromDate, toDate, institution, department, fromInstitution, fromDepartment, toInstitution, toDepartment, user, creditCompany, startId, endId);
        reportTemplateRowBundle.setReportTemplate(current);
        return "";
    }

    private ReportTemplateRowBundle handleBillFeeGroupedByBillTypeAtomic(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();
        String jpql;
        Map<String, Object> parameters = new HashMap<>();

        jpql = "select new com.divudi.core.data.ReportTemplateRow("
                + " bill.billTypeAtomic, sum(bf.feeValue)) "
                + " from BillFee bf "
                + " join bf.bill bill "
                + " where bf.retired<>:bfr "
                + " and bf.billItem.retired<>:bir "
                + " and bill.retired<>:br ";
        parameters.put("bfr", true);
        parameters.put("bir", true);
        parameters.put("br", true);

        if (btas != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paramDate != null) {
            jpql += " and bill.createdAt=:bd ";
            parameters.put("bd", paramDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.createdAt < :td ";
            parameters.put("td", paramToDate);
        }

        if (paramFromDate != null) {
            jpql += " and bill.createdAt > :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramStartId != null) {
            jpql += " and bill.id > :sid ";
            parameters.put("sid", paramStartId);
        }

        if (paramEndId != null) {
            jpql += " and bill.id < :eid ";
            parameters.put("eid", paramEndId);
        }

        if (paramInstitution != null) {
            jpql += " and bill.institution=:ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department=:dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramFromInstitution != null) {
            jpql += " and bill.fromInstitution=:fins ";
            parameters.put("fins", paramFromInstitution);
        }

        if (paramFromDepartment != null) {
            jpql += " and bill.fromDepartment=:fdep ";
            parameters.put("fdep", paramFromDepartment);
        }

        if (paramToInstitution != null) {
            jpql += " and bill.toInstitution=:tins ";
            parameters.put("tins", paramToInstitution);
        }

        if (paramToDepartment != null) {
            jpql += " and bill.toDepartment=:tdep ";
            parameters.put("tdep", paramToDepartment);
        }

        if (paramCreditCompany == null) {

        } else {
            if (paramCreditCompany.getId() == null) {
                jpql += " and bill.creditCompany is not null ";
            } else if (paramCreditCompany.getId() == 1l) {
                jpql += " and bill.creditCompany is null ";
            } else {
                jpql += " and bill.creditCompany=:cc ";
                parameters.put("cc", paramCreditCompany);
            }
        }

        jpql += " group by bill.billTypeAtomic";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        if (rs == null || rs.isEmpty()) {
        } else {
        }

        long idCounter = 1;
        for (ReportTemplateRow row : rs) {
            row.setCounter(idCounter++);
            if (row.getBtas() == null) {
                row.setBtas(btas);
            }
            if (row.getDate() == null) {
                row.setDate(paramDate);
            }
            if (row.getFromDate() == null) {
                row.setFromDate(paramFromDate);
            }
            if (row.getToDate() == null) {
                row.setToDate(paramToDate);
            }
            if (row.getInstitution() == null) {
                row.setInstitution(paramInstitution);
            }
            if (row.getDepartment() == null) {
                row.setDepartment(paramDepartment);
            }
            if (row.getFromInstitution() == null) {
                row.setFromInstitution(paramFromInstitution);
            }
            if (row.getFromDepartment() == null) {
                row.setFromDepartment(paramFromDepartment);
            }
            if (row.getToInstitution() == null) {
                row.setToInstitution(paramToInstitution);
            }
            if (row.getToDepartment() == null) {
                row.setToDepartment(paramToDepartment);
            }
            if (row.getUser() == null) {
                row.setUser(paramUser);
            }
            if (row.getCreditCompany() == null) {
                row.setCreditCompany(paramCreditCompany);
            }
            if (row.getStartId() == null) {
                row.setStartId(paramStartId);
            }
            if (row.getEndId() == null) {
                row.setEndId(paramEndId);
            }
        }

        bundle.setReportTemplateRows(rs);
        return bundle;
    }

    private ReportTemplateRowBundle handleBillFeeGroupedByToDepartmentAndCategory(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();
        String jpql;
        Map<String, Object> parameters = new HashMap<>();

        jpql = "select new com.divudi.core.data.ReportTemplateRow("
                + " bill.billTypeAtomic, sum(bf.feeValue)) "
                + " from BillFee bf "
                + " join bf.bill bill "
                + " where bf.retired<>:bfr "
                + " and bf.billItem.retired<>:bir "
                + " and bill.retired<>:br ";
        parameters.put("bfr", true);
        parameters.put("bir", true);
        parameters.put("br", true);

        if (btas != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paramDate != null) {
            jpql += " and bill.createdAt=:bd ";
            parameters.put("bd", paramDate);
        }

        if (paramUser != null) {
            jpql += " and bill.creater=:wu ";
            parameters.put("wu", paramUser);
        }

        if (paramToDate != null) {
            jpql += " and bill.createdAt < :td ";
            parameters.put("td", paramToDate);
        }

        if (paramFromDate != null) {
            jpql += " and bill.createdAt > :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramStartId != null) {
            jpql += " and bill.id > :sid ";
            parameters.put("sid", paramStartId);
        }

        if (paramEndId != null) {
            jpql += " and bill.id < :eid ";
            parameters.put("eid", paramEndId);
        }

        if (paramInstitution != null) {
            jpql += " and bill.institution=:ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department=:dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramFromInstitution != null) {
            jpql += " and bill.fromInstitution=:fins ";
            parameters.put("fins", paramFromInstitution);
        }

        if (paramFromDepartment != null) {
            jpql += " and bill.fromDepartment=:fdep ";
            parameters.put("fdep", paramFromDepartment);
        }

        if (paramToInstitution != null) {
            jpql += " and bill.toInstitution=:tins ";
            parameters.put("tins", paramToInstitution);
        }

        if (paramToDepartment != null) {
            jpql += " and bill.toDepartment=:tdep ";
            parameters.put("tdep", paramToDepartment);
        }

        if (paramCreditCompany == null) {

        } else {
            if (paramCreditCompany.getId() == null) {
                jpql += " and bill.creditCompany is not null ";
            } else if (paramCreditCompany.getId() == 1l) {
                jpql += " and bill.creditCompany is null ";
            } else {
                jpql += " and bill.creditCompany=:cc ";
                parameters.put("cc", paramCreditCompany);
            }
        }

        jpql += " group by bill.billTypeAtomic";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        if (rs == null || rs.isEmpty()) {
        } else {
        }

        long idCounter = 1;
        for (ReportTemplateRow row : rs) {
            row.setCounter(idCounter++);
            if (row.getBtas() == null) {
                row.setBtas(btas);
            }
            if (row.getDate() == null) {
                row.setDate(paramDate);
            }
            if (row.getFromDate() == null) {
                row.setFromDate(paramFromDate);
            }
            if (row.getToDate() == null) {
                row.setToDate(paramToDate);
            }
            if (row.getInstitution() == null) {
                row.setInstitution(paramInstitution);
            }
            if (row.getDepartment() == null) {
                row.setDepartment(paramDepartment);
            }
            if (row.getFromInstitution() == null) {
                row.setFromInstitution(paramFromInstitution);
            }
            if (row.getFromDepartment() == null) {
                row.setFromDepartment(paramFromDepartment);
            }
            if (row.getToInstitution() == null) {
                row.setToInstitution(paramToInstitution);
            }
            if (row.getToDepartment() == null) {
                row.setToDepartment(paramToDepartment);
            }
            if (row.getUser() == null) {
                row.setUser(paramUser);
            }
            if (row.getCreditCompany() == null) {
                row.setCreditCompany(paramCreditCompany);
            }
            if (row.getStartId() == null) {
                row.setStartId(paramStartId);
            }
            if (row.getEndId() == null) {
                row.setEndId(paramEndId);
            }
        }

        bundle.setReportTemplateRows(rs);
        return bundle;
    }

    private ReportTemplateRowBundle handleBillTypeAtomicSummaryUsingBills(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();
        String jpql;
        Map<String, Object> parameters = new HashMap<>();

        jpql = "select new com.divudi.core.data.ReportTemplateRow("
                + " bill.billTypeAtomic, count(bill), sum(bill.netTotal)) "
                + " from Bill bill "
                + " where bill.retired<>:br ";
        parameters.put("br", true);

        if (btas != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paramDate != null) {
            jpql += " and bill.createdAt=:bd ";
            parameters.put("bd", paramDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.createdAt < :td ";
            parameters.put("td", paramToDate);
        }

        if (paramFromDate != null) {
            jpql += " and bill.createdAt > :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramStartId != null) {
            jpql += " and bill.id > :sid ";
            parameters.put("sid", paramStartId);
        }

        if (paramEndId != null) {
            jpql += " and bill.id < :eid ";
            parameters.put("eid", paramEndId);
        }

        if (paramInstitution != null) {
            jpql += " and bill.institution=:ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department=:dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramFromInstitution != null) {
            jpql += " and bill.fromInstitution=:fins ";
            parameters.put("fins", paramFromInstitution);
        }

        if (paramFromDepartment != null) {
            jpql += " and bill.fromDepartment=:fdep ";
            parameters.put("fdep", paramFromDepartment);
        }

        if (paramUser != null) {
            jpql += " and bill.creater=:wu ";
            parameters.put("wu", paramUser);
        }

        if (paramToInstitution != null) {
            jpql += " and bill.toInstitution=:tins ";
            parameters.put("tins", paramToInstitution);
        }

        if (paramToDepartment != null) {
            jpql += " and bill.toDepartment=:tdep ";
            parameters.put("tdep", paramToDepartment);
        }

        if (paramCreditCompany == null) {

        } else {
            if (paramCreditCompany.getId() == null) {
                jpql += " and bill.creditCompany is not null ";
            } else if (paramCreditCompany.getId() == 1l) {
                jpql += " and bill.creditCompany is null ";
            } else {
                jpql += " and bill.creditCompany=:cc ";
                parameters.put("cc", paramCreditCompany);
            }
        }

        jpql += " group by bill.billTypeAtomic";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        if (rs == null || rs.isEmpty()) {
        } else {
        }

        long idCounter = 1;
        for (ReportTemplateRow row : rs) {
            row.setCounter(idCounter++);
            if (row.getBtas() == null) {
                row.setBtas(btas);
            }
            if (row.getDate() == null) {
                row.setDate(paramDate);
            }
            if (row.getFromDate() == null) {
                row.setFromDate(paramFromDate);
            }
            if (row.getToDate() == null) {
                row.setToDate(paramToDate);
            }
            if (row.getInstitution() == null) {
                row.setInstitution(paramInstitution);
            }
            if (row.getDepartment() == null) {
                row.setDepartment(paramDepartment);
            }
            if (row.getFromInstitution() == null) {
                row.setFromInstitution(paramFromInstitution);
            }
            if (row.getFromDepartment() == null) {
                row.setFromDepartment(paramFromDepartment);
            }
            if (row.getToInstitution() == null) {
                row.setToInstitution(paramToInstitution);
            }
            if (row.getToDepartment() == null) {
                row.setToDepartment(paramToDepartment);
            }
            if (row.getUser() == null) {
                row.setUser(paramUser);
            }
            if (row.getCreditCompany() == null) {
                row.setCreditCompany(paramCreditCompany);
            }
            if (row.getStartId() == null) {
                row.setStartId(paramStartId);
            }
            if (row.getEndId() == null) {
                row.setEndId(paramEndId);
            }
        }

        bundle.setReportTemplateRows(rs);
        return bundle;
    }

    private ReportTemplateRowBundle handleBillTypeAtomicTotalUsingBills(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();
        String jpql;
        Map<String, Object> parameters = new HashMap<>();

        // Initialize the total to 0
        double totalNetAmount = 0.0;

        jpql = "select sum(bill.netTotal) "
                + " from Bill bill "
                + " where bill.retired<>:br ";
        parameters.put("br", true);

        if (btas != null && !btas.isEmpty()) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paramDate != null) {
            jpql += " and bill.createdAt=:bd ";
            parameters.put("bd", paramDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.createdAt<:td ";
            parameters.put("td", paramToDate);
        }

        if (paramFromDate != null) {
            jpql += " and bill.createdAt>:fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramStartId != null) {
            jpql += " and bill.id>:sid ";
            parameters.put("sid", paramStartId);
        }

        if (paramEndId != null) {
            jpql += " and bill.id<:eid ";
            parameters.put("eid", paramEndId);
        }

        if (paramInstitution != null) {
            jpql += " and bill.institution=:ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department=:dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramFromInstitution != null) {
            jpql += " and bill.fromInstitution=:fins ";
            parameters.put("fins", paramFromInstitution);
        }

        if (paramFromDepartment != null) {
            jpql += " and bill.fromDepartment=:fdep ";
            parameters.put("fdep", paramFromDepartment);
        }

        if (paramToInstitution != null) {
            jpql += " and bill.toInstitution=:tins ";
            parameters.put("tins", paramToInstitution);
        }

        if (paramToDepartment != null) {
            jpql += " and bill.toDepartment=:tdep ";
            parameters.put("tdep", paramToDepartment);
        }

        if (paramCreditCompany == null) {

        } else {
            if (paramCreditCompany.getId() == null) {
                jpql += " and bill.creditCompany is not null ";
            } else if (paramCreditCompany.getId() == 1l) {
                jpql += " and bill.creditCompany is null ";
            } else {
                jpql += " and bill.creditCompany=:cc ";
                parameters.put("cc", paramCreditCompany);
            }
        }

        if (paramUser != null) {
            jpql += " and bill.creater=:wu ";
            parameters.put("wu", paramUser);
        }

        Double sumResult = ejbFacade.findSingleResultByJpql(jpql, parameters, TemporalType.DATE);

        if (sumResult != null) {
            totalNetAmount = sumResult;
        }

        bundle.setTotal(totalNetAmount);

        return bundle;
    }

    private ReportTemplateRowBundle handleBillTypeAndPaymentMethodSummaryPayments(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleBillTypeAndPaymentMethodSummaryUsingBills(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleBillFeeList(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleBillItemList(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleBillList(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleBillTypeAtomicSummaryUsingPayments(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleEncounterList(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handlePatientList(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handlePaymentMethodSummaryUsingBills(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handlePaymentMethodSummaryUsingPayments(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handlePaymentTypeSummaryPayments(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handlePaymentTypeSummaryUsingBills(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleItemCategorySummaryByBillFee(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleItemCategorySummaryByBillItem(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        String jpql;
        Map<String, Object> parameters = new HashMap<>();
        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();

        jpql = "select new com.divudi.core.data.ReportTemplateRow("
                + " bi.item.category.name, sum(bi.netValue)) "
                + " from BillItem bi"
                + " join bi.bill bill "
                + " where bill.retired<>:br "
                + " and bi.retired<>:br ";
        parameters.put("br", true);

        if (btas != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paramDate != null) {
            jpql += " and bill.createdAt=:bd ";
            parameters.put("bd", paramDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.createdAt < :td ";
            parameters.put("td", paramToDate);
        }

        if (paramFromDate != null) {
            jpql += " and bill.createdAt > :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramStartId != null) {
            jpql += " and bill.id > :sid ";
            parameters.put("sid", paramStartId);
        }

        if (paramEndId != null) {
            jpql += " and bill.id < :eid ";
            parameters.put("eid", paramEndId);
        }

        if (paramInstitution != null) {
            jpql += " and bill.institution=:ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department=:dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramFromInstitution != null) {
            jpql += " and bill.fromInstitution=:fins ";
            parameters.put("fins", paramFromInstitution);
        }

        if (paramFromDepartment != null) {
            jpql += " and bill.fromDepartment=:fdep ";
            parameters.put("fdep", paramFromDepartment);
        }

        if (paramToInstitution != null) {
            jpql += " and bill.toInstitution=:tins ";
            parameters.put("tins", paramToInstitution);
        }

        if (paramToDepartment != null) {
            jpql += " and bill.toDepartment=:tdep ";
            parameters.put("tdep", paramToDepartment);
        }

        if (paramCreditCompany != null) {
            jpql += " and bill.creditCompany=:creditCompany ";
            parameters.put("creditCompany", paramCreditCompany);
        }

        jpql += " and bi.item is not null "
                + " and bi.item.category is not null ";

        jpql += " group by bi.item.category ";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        if (rs == null || rs.isEmpty()) {
        } else {
        }

        long idCounter = 1;
        for (ReportTemplateRow row : rs) {
            row.setCounter(idCounter++);
            if (row.getBtas() == null) {
                row.setBtas(btas);
            }
            if (row.getDate() == null) {
                row.setDate(paramDate);
            }
            if (row.getFromDate() == null) {
                row.setFromDate(paramFromDate);
            }
            if (row.getToDate() == null) {
                row.setToDate(paramToDate);
            }
            if (row.getInstitution() == null) {
                row.setInstitution(paramInstitution);
            }
            if (row.getDepartment() == null) {
                row.setDepartment(paramDepartment);
            }
            if (row.getFromInstitution() == null) {
                row.setFromInstitution(paramFromInstitution);
            }
            if (row.getFromDepartment() == null) {
                row.setFromDepartment(paramFromDepartment);
            }
            if (row.getToInstitution() == null) {
                row.setToInstitution(paramToInstitution);
            }
            if (row.getToDepartment() == null) {
                row.setToDepartment(paramToDepartment);
            }
            if (row.getUser() == null) {
                row.setUser(paramUser);
            }
            if (row.getCreditCompany() == null) {
                row.setCreditCompany(paramCreditCompany);
            }
            if (row.getStartId() == null) {
                row.setStartId(paramStartId);
            }
            if (row.getEndId() == null) {
                row.setEndId(paramEndId);
            }
        }

        bundle.setReportTemplateRows(rs);
        return bundle;
    }

    private ReportTemplateRowBundle handleItemCategorySummaryByBill(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        String jpql;
        Map<String, Object> parameters = new HashMap<>();
        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();

        jpql = "select new com.divudi.core.data.ReportTemplateRow("
                + " bi.item.category, count(bi), sum(bi.netValue)) "
                + " from BillItem bi"
                + " join bi.bill bill "
                + " where bill.retired<>:br "
                + " and bi.retired<>:br ";
        parameters.put("br", true);

        if (btas != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paramDate != null) {
            jpql += " and bill.createdAt=:bd ";
            parameters.put("bd", paramDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.createdAt < :td ";
            parameters.put("td", paramToDate);
        }

        if (paramFromDate != null) {
            jpql += " and bill.createdAt > :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramStartId != null) {
            jpql += " and bill.id > :sid ";
            parameters.put("sid", paramStartId);
        }

        if (paramEndId != null) {
            jpql += " and bill.id < :eid ";
            parameters.put("eid", paramEndId);
        }

        if (paramInstitution != null) {
            jpql += " and bill.institution=:ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department=:dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramFromInstitution != null) {
            jpql += " and bill.fromInstitution=:fins ";
            parameters.put("fins", paramFromInstitution);
        }

        if (paramFromDepartment != null) {
            jpql += " and bill.fromDepartment=:fdep ";
            parameters.put("fdep", paramFromDepartment);
        }

        if (paramToInstitution != null) {
            jpql += " and bill.toInstitution=:tins ";
            parameters.put("tins", paramToInstitution);
        }

        if (paramToDepartment != null) {
            jpql += " and bill.toDepartment=:tdep ";
            parameters.put("tdep", paramToDepartment);
        }

        if (paramUser != null) {
            jpql += " and bill.creater=:wu ";
            parameters.put("wu", paramUser);
        }

        if (paramCreditCompany != null) {
            jpql += " and bill.creditCompany=:creditCompany ";
            parameters.put("creditCompany", paramCreditCompany);
        }

        jpql += " and bi.item is not null "
                + " and bi.item.category is not null ";

        jpql += " group by bi.item.category ";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        if (rs == null || rs.isEmpty()) {
        } else {
        }

        long idCounter = 1;
        Double total = 0.0;
        for (ReportTemplateRow row : rs) {
            row.setCounter(idCounter++);
            if (row.getBtas() == null) {
                row.setBtas(btas);
            }
            if (row.getDate() == null) {
                row.setDate(paramDate);
            }
            if (row.getFromDate() == null) {
                row.setFromDate(paramFromDate);
            }
            if (row.getToDate() == null) {
                row.setToDate(paramToDate);
            }
            if (row.getInstitution() == null) {
                row.setInstitution(paramInstitution);
            }
            if (row.getDepartment() == null) {
                row.setDepartment(paramDepartment);
            }
            if (row.getFromInstitution() == null) {
                row.setFromInstitution(paramFromInstitution);
            }
            if (row.getFromDepartment() == null) {
                row.setFromDepartment(paramFromDepartment);
            }
            if (row.getToInstitution() == null) {
                row.setToInstitution(paramToInstitution);
            }
            if (row.getToDepartment() == null) {
                row.setToDepartment(paramToDepartment);
            }
            if (row.getUser() == null) {
                row.setUser(paramUser);
            }
            if (row.getCreditCompany() == null) {
                row.setCreditCompany(paramCreditCompany);
            }
            if (row.getStartId() == null) {
                row.setStartId(paramStartId);
            }
            if (row.getEndId() == null) {
                row.setEndId(paramEndId);
            }
        }

        bundle.setReportTemplateRows(rs);
        return bundle;
    }

    private ReportTemplateRowBundle handleItemDepartmentummaryByBill(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        String jpql;
        Map<String, Object> parameters = new HashMap<>();
        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();

        jpql = "select new com.divudi.core.data.ReportTemplateRow("
                + " bi.item.department, count(bi), sum(bi.netValue)) "
                + " from BillItem bi"
                + " join bi.bill bill "
                + " where bill.retired<>:br "
                + " and bi.retired<>:br ";
        parameters.put("br", true);

        if (btas != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paramDate != null) {
            jpql += " and bill.createdAt=:bd ";
            parameters.put("bd", paramDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.createdAt < :td ";
            parameters.put("td", paramToDate);
        }

        if (paramFromDate != null) {
            jpql += " and bill.createdAt > :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramStartId != null) {
            jpql += " and bill.id > :sid ";
            parameters.put("sid", paramStartId);
        }

        if (paramEndId != null) {
            jpql += " and bill.id < :eid ";
            parameters.put("eid", paramEndId);
        }

        if (paramInstitution != null) {
            jpql += " and bill.institution=:ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department=:dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramFromInstitution != null) {
            jpql += " and bill.fromInstitution=:fins ";
            parameters.put("fins", paramFromInstitution);
        }

        if (paramFromDepartment != null) {
            jpql += " and bill.fromDepartment=:fdep ";
            parameters.put("fdep", paramFromDepartment);
        }

        if (paramToInstitution != null) {
            jpql += " and bill.toInstitution=:tins ";
            parameters.put("tins", paramToInstitution);
        }

        if (paramToDepartment != null) {
            jpql += " and bill.toDepartment=:tdep ";
            parameters.put("tdep", paramToDepartment);
        }

        if (paramUser != null) {
            jpql += " and bill.creater=:wu ";
            parameters.put("wu", paramUser);
        }

        if (paramCreditCompany != null) {
            jpql += " and bill.creditCompany=:creditCompany ";
            parameters.put("creditCompany", paramCreditCompany);
        }

//        jpql += " and bi.item is not null "
//                + " and bi.item.department is not null ";
        jpql += " group by bi.item.department ";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        if (rs == null || rs.isEmpty()) {
        } else {
        }

        long idCounter = 1;
        Double total = 0.0;
        for (ReportTemplateRow row : rs) {
            row.setCounter(idCounter++);
            if (row.getBtas() == null) {
                row.setBtas(btas);
            }
            if (row.getDate() == null) {
                row.setDate(paramDate);
            }
            if (row.getFromDate() == null) {
                row.setFromDate(paramFromDate);
            }
            if (row.getToDate() == null) {
                row.setToDate(paramToDate);
            }
            if (row.getInstitution() == null) {
                row.setInstitution(paramInstitution);
            }
            if (row.getDepartment() == null) {
                row.setDepartment(paramDepartment);
            }
            if (row.getFromInstitution() == null) {
                row.setFromInstitution(paramFromInstitution);
            }
            if (row.getFromDepartment() == null) {
                row.setFromDepartment(paramFromDepartment);
            }
            if (row.getToInstitution() == null) {
                row.setToInstitution(paramToInstitution);
            }
            if (row.getToDepartment() == null) {
                row.setToDepartment(paramToDepartment);
            }
            if (row.getUser() == null) {
                row.setUser(paramUser);
            }
            if (row.getCreditCompany() == null) {
                row.setCreditCompany(paramCreditCompany);
            }
            if (row.getStartId() == null) {
                row.setStartId(paramStartId);
            }
            if (row.getEndId() == null) {
                row.setEndId(paramEndId);
            }
        }

        bundle.setReportTemplateRows(rs);
        bundle.setTotal(total);
        return bundle;
    }

    private ReportTemplateRowBundle handleItemSummaryByBill(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        String jpql;
        Map<String, Object> parameters = new HashMap<>();
        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();

        jpql = "select new com.divudi.core.data.ReportTemplateRow("
                + " bi.item, count(bi), sum(bi.netValue)) "
                + " from BillItem bi"
                + " join bi.bill bill "
                + " where bill.retired<>:br "
                + " and bi.retired<>:br ";
        parameters.put("br", true);

        if (btas != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", btas);
        }

        if (paramDate != null) {
            jpql += " and bill.createdAt=:bd ";
            parameters.put("bd", paramDate);
        }

        if (paramToDate != null) {
            jpql += " and bill.createdAt < :td ";
            parameters.put("td", paramToDate);
        }

        if (paramFromDate != null) {
            jpql += " and bill.createdAt > :fd ";
            parameters.put("fd", paramFromDate);
        }

        if (paramStartId != null) {
            jpql += " and bill.id > :sid ";
            parameters.put("sid", paramStartId);
        }

        if (paramEndId != null) {
            jpql += " and bill.id < :eid ";
            parameters.put("eid", paramEndId);
        }

        if (paramInstitution != null) {
            jpql += " and bill.institution=:ins ";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and bill.department=:dep ";
            parameters.put("dep", paramDepartment);
        }

        if (paramFromInstitution != null) {
            jpql += " and bill.fromInstitution=:fins ";
            parameters.put("fins", paramFromInstitution);
        }

        if (paramFromDepartment != null) {
            jpql += " and bill.fromDepartment=:fdep ";
            parameters.put("fdep", paramFromDepartment);
        }

        if (paramToInstitution != null) {
            jpql += " and bill.toInstitution=:tins ";
            parameters.put("tins", paramToInstitution);
        }

        if (paramToDepartment != null) {
            jpql += " and bill.toDepartment=:tdep ";
            parameters.put("tdep", paramToDepartment);
        }

        if (paramUser != null) {
            jpql += " and bill.creater=:wu ";
            parameters.put("wu", paramUser);
        }

        if (paramCreditCompany != null) {
            jpql += " and bill.creditCompany=:creditCompany ";
            parameters.put("creditCompany", paramCreditCompany);
        }

        jpql += " and bi.item is not null ";

        jpql += " group by bi.item ";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        if (rs == null || rs.isEmpty()) {
        } else {
        }

        long idCounter = 1;
        Double total = 0.0;
        for (ReportTemplateRow row : rs) {
            row.setCounter(idCounter++);
            if (row.getBtas() == null) {
                row.setBtas(btas);
            }
            if (row.getDate() == null) {
                row.setDate(paramDate);
            }
            if (row.getFromDate() == null) {
                row.setFromDate(paramFromDate);
            }
            if (row.getToDate() == null) {
                row.setToDate(paramToDate);
            }
            if (row.getInstitution() == null) {
                row.setInstitution(paramInstitution);
            }
            if (row.getDepartment() == null) {
                row.setDepartment(paramDepartment);
            }
            if (row.getFromInstitution() == null) {
                row.setFromInstitution(paramFromInstitution);
            }
            if (row.getFromDepartment() == null) {
                row.setFromDepartment(paramFromDepartment);
            }
            if (row.getToInstitution() == null) {
                row.setToInstitution(paramToInstitution);
            }
            if (row.getToDepartment() == null) {
                row.setToDepartment(paramToDepartment);
            }
            if (row.getUser() == null) {
                row.setUser(paramUser);
            }
            if (row.getCreditCompany() == null) {
                row.setCreditCompany(paramCreditCompany);
            }
            if (row.getStartId() == null) {
                row.setStartId(paramStartId);
            }
            if (row.getEndId() == null) {
                row.setEndId(paramEndId);
            }
        }

        bundle.setReportTemplateRows(rs);
        return bundle;
    }

    private ReportTemplateRowBundle handleSessionInstanceList(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        String jpql;
        Map<String, Object> parameters = new HashMap<>();
        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();

        jpql = "select new com.divudi.core.data.ReportTemplateRow(ss) "
                + " from SessionInstance ss "
                + " where ss.retired<>:br ";
        parameters.put("br", true);

        if (paramDate != null) {
            jpql += " and ss.sessionDate=:bd";
            parameters.put("bd", paramDate);
        }

        if (paramToDate != null) {
            jpql += " and ss.sessionDate < :td";
            parameters.put("td", paramToDate);
        }

        if (paramFromDate != null) {
            jpql += " and ss.sessionDate > :fd";
            parameters.put("fd", paramFromDate);
        }

        if (paramInstitution != null) {
            jpql += " and ss.institution=:ins";
            parameters.put("ins", paramInstitution);
        }

        if (paramDepartment != null) {
            jpql += " and ss.department=:dep";
            parameters.put("dep", paramDepartment);
        }

        if (paramUser != null) {
            jpql += " and ss.creater=:wu";
            parameters.put("wu", paramUser);
        }

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        if (rs == null || rs.isEmpty()) {
            return null;
        } else {
        }

        long idCounter = 1;

        for (ReportTemplateRow row : rs) {
            row.setCounter(idCounter++);
            if (row.getBtas() == null) {
                row.setBtas(btas);
            }
            if (row.getDate() == null) {
                row.setDate(paramDate);
            }
            if (row.getFromDate() == null) {
                row.setFromDate(paramFromDate);
            }
            if (row.getToDate() == null) {
                row.setToDate(paramToDate);
            }
            if (row.getInstitution() == null) {
                row.setInstitution(paramInstitution);
            }
            if (row.getDepartment() == null) {
                row.setDepartment(paramDepartment);
            }
            if (row.getFromInstitution() == null) {
                row.setFromInstitution(paramFromInstitution);
            }
            if (row.getFromDepartment() == null) {
                row.setFromDepartment(paramFromDepartment);
            }
            if (row.getToInstitution() == null) {
                row.setToInstitution(paramToInstitution);
            }
            if (row.getToDepartment() == null) {
                row.setToDepartment(paramToDepartment);
            }
            if (row.getUser() == null) {
                row.setUser(paramUser);
            }
            if (row.getCreditCompany() == null) {
                row.setCreditCompany(paramCreditCompany);
            }
            if (row.getStartId() == null) {
                row.setStartId(paramStartId);
            }
            if (row.getEndId() == null) {
                row.setEndId(paramEndId);
            }
        }

        bundle.setReportTemplateRows(rs);
        return bundle;
    }

    private ReportTemplateRowBundle handleToDepartmentSummaryByBillFee(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleToDepartmentSummaryByBillItem(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        return new ReportTemplateRowBundle();
    }

    private ReportTemplateRowBundle handleToDepartmentSummaryByBill(
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        return new ReportTemplateRowBundle();
    }

    public void saveSelected() {
        if (getCurrent().getName().isEmpty() || getCurrent().getName() == null) {
            JsfUtil.addErrorMessage("Please enter Value");
            return;
        }
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public ReportTemplateFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ReportTemplateFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ReportTemplateController() {
    }

    public ReportTemplate getCurrent() {
        if (current == null) {
            current = new ReportTemplate();
        }
        return current;
    }

    public void setCurrent(ReportTemplate current) {
        this.current = current;
    }

    private List<ReportTemplateColumn> getReportTemplateColumns(String input) {
        List<ReportTemplateColumn> columns = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return columns;
        }
        String[] lines = input.split("\\r?\\n");
        for (String line : lines) {
            for (ReportTemplateColumn column : ReportTemplateColumn.values()) {
                if (column.getLabel().equalsIgnoreCase(line.trim())) {
                    columns.add(column);
                    break;
                }
            }
        }
        return columns;
    }

    private List<ReportTemplateFilter> getReportTemplateFilters(String input) {
        List<ReportTemplateFilter> columns = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return columns;
        }
        String[] lines = input.split("\\r?\\n");
        for (String line : lines) {
            for (ReportTemplateFilter column : ReportTemplateFilter.values()) {
                if (column.getLabel().equalsIgnoreCase(line.trim())) {
                    columns.add(column);
                    break;
                }
            }
        }
        return columns;
    }

    private List<BillTypeAtomic> getBillTypeAtomics(String input) {
        List<BillTypeAtomic> columns = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return columns;
        }
        String[] lines = input.split("\\r?\\n");
        for (String line : lines) {
            for (BillTypeAtomic column : BillTypeAtomic.values()) {
                if (column.getLabel().equalsIgnoreCase(line.trim())) {
                    columns.add(column);
                    break;
                }
            }
        }
        return columns;
    }

    public void delete() {
        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private ReportTemplateFacade getFacade() {
        return ejbFacade;
    }

    public String navigateToReportTemplateList() {
        items = getAllItems();
        return "/dataAdmin/report_template_list";
    }

    public String navigateToAddNewReportTemplate() {
        current = new ReportTemplate();
        return "/dataAdmin/report_template";
    }

    public void deleteReportTemplate() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        current.setRetired(true);
        current.setRetirer(sessionController.getLoggedUser());
        current.setRetiredAt(new Date());
        save(current);
        items = getAllItems();
        JsfUtil.addSuccessMessage("Removed");
    }

    public List<ReportTemplateType> getReportTemplateTypes() {
        return Arrays.asList(ReportTemplateType.values());
    }

    public String navigateToEditReportTemplate() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return null;
        }
        return "/dataAdmin/report_template?faces-redirect=true";
    }

    public String navigateToGenerateReport() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return null;
        }
        return "/dataAdmin/report?faces-redirect=true";
    }

    public String navigateToEditGenerateReport() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return null;
        }
        return "/dataAdmin/report";
    }

    public List<ReportTemplate> getAllItems() {
        List<ReportTemplate> allItems;
        String j;
        j = "select a "
                + " from ReportTemplate a "
                + " where a.retired=false "
                + " order by a.name";
        allItems = getFacade().findByJpql(j);
        return allItems;
    }

    public List<ReportTemplate> getItems() {
        return items;
    }

    public Date getDate() {
        if (date == null) {
            date = CommonFunctions.getStartOfDay();
        }
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public WebUser getUser() {
        return user;
    }

    public void setUser(WebUser user) {
        this.user = user;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public List<ReportTemplateRow> getReportTemplateRows() {
        return ReportTemplateRows;
    }

    public void setReportTemplateRows(List<ReportTemplateRow> ReportTemplateRows) {
        this.ReportTemplateRows = ReportTemplateRows;
    }

    public ReportTemplateRowBundle getReportTemplateRowBundle() {
        return reportTemplateRowBundle;
    }

    public void setReportTemplateRowBundle(ReportTemplateRowBundle reportTemplateRowBundle) {
        this.reportTemplateRowBundle = reportTemplateRowBundle;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public Long getStartId() {
        return startId;
    }

    public void setStartId(Long startId) {
        this.startId = startId;
    }

    public Long getEndId() {
        return endId;
    }

    public void setEndId(Long endId) {
        this.endId = endId;
    }

    public static class ReportTemplateConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ReportTemplateController controller = (ReportTemplateController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "reportTemplateController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof ReportTemplate) {
                ReportTemplate o = (ReportTemplate) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ReportTemplate.class.getName());
            }
        }
    }

}
