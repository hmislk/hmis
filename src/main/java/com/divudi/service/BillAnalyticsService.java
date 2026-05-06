package com.divudi.service;

import com.divudi.core.data.BillTypeAtomic;

import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.data.ReportTemplateRowBundle;
import com.divudi.core.entity.Department;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.DenominationTransactionFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import java.util.ArrayList;
import java.util.HashMap;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.*;
import com.google.gson.Gson;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Dr M H B Ariyaratne
 *
 */
@Stateless
public class BillAnalyticsService {

    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private DenominationTransactionFacade denominationTransactionFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private DrawerService drawerService;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;

    private static final Gson gson = new Gson();

    public List<Department> fetchBillItemsItemDepartmentsWithTransactions(List<BillTypeAtomic> billTypes, Date fromDate, Date toDate) {
        if (billTypes == null || billTypes.isEmpty()) {
            return new ArrayList<>();
        }

        String jpql = "select distinct bi.item.department "
                + "from BillItem bi "
                + "where bi.retired = false "
                + "and bi.bill.retired = false "
                + "and bi.bill.createdAt between :fd and :td "
                + "and bi.bill.billTypeAtomic in :bts "
                + "order by bi.item.department.name";

        Map<String, Object> params = new HashMap<>();
        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("bts", billTypes);

        return departmentFacade.findByJpql(jpql, params);
    }

    
    public List<Department> fetchBilledDepartmentsWithTransactions(List<BillTypeAtomic> billTypes, Date fromDate, Date toDate) {
        if (billTypes == null || billTypes.isEmpty()) {
            return new ArrayList<>();
        }
        String jpql = "select distinct b.department "
                + "from Bill b "
                + "where b.retired = false "
                + "and b.createdAt between :fd and :td "
                + "and b.billTypeAtomic in :bts "
                + "order by b.department.name";

        Map<String, Object> params = new HashMap<>();
        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("bts", billTypes);

        return departmentFacade.findByJpql(jpql, params);
    }

    public void fillBundleForOpdServiceCounts(ReportTemplateRowBundle inputBundle, Date fromDate, Date toDate) {
        List<BillTypeAtomic> billedTypes = new ArrayList<>(Arrays.asList(
                BillTypeAtomic.OPD_BILL_WITH_PAYMENT,
                BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER
        ));

        List<BillTypeAtomic> cancelledTypes = new ArrayList<>(Arrays.asList(
                BillTypeAtomic.OPD_BILL_CANCELLATION,
                BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION
        ));

        List<BillTypeAtomic> returnedTypes = new ArrayList<>(Collections.singletonList(
                BillTypeAtomic.OPD_BILL_REFUND
        ));

        List<BillTypeAtomic> allTypes = new ArrayList<>();
        allTypes.addAll(billedTypes);
        allTypes.addAll(cancelledTypes);
        allTypes.addAll(returnedTypes);

        List<Department> transactionDepartments = fetchBillItemsItemDepartmentsWithTransactions(allTypes, fromDate, toDate);

        long totalBilled = 0;
        long totalCancelled = 0;
        long totalReturned = 0;
        long totalNet = 0;

        for (Department txDept : transactionDepartments) {
            ReportTemplateRow row = new ReportTemplateRow();
            row.setDepartment(txDept);

            long billed = countBillsByBillItemDepartment(txDept, billedTypes, fromDate, toDate);
            long cancelled = countBillsByBillItemDepartment(txDept, cancelledTypes, fromDate, toDate);
            long returned = countBillsByBillItemDepartment(txDept, returnedTypes, fromDate, toDate);
            long net = billed - (cancelled + returned);

            row.setBilledCount(billed);
            row.setCancelledCount(cancelled);
            row.setReturnCount(returned);
            row.setNetCount(net);

            inputBundle.getReportTemplateRows().add(row);

            totalBilled += billed;
            totalCancelled += cancelled;
            totalReturned += returned;
            totalNet += net;
        }

        inputBundle.setBilledCount(totalBilled);
        inputBundle.setCancelledCount(totalCancelled);
        inputBundle.setReturnCount(totalReturned);
        inputBundle.setNetCount(totalNet);
    }

    public void fillBundleForCollectionCentreServiceCounts(ReportTemplateRowBundle inputBundle, Date fromDate, Date toDate) {
        List<BillTypeAtomic> billedTypes = new ArrayList<>(Arrays.asList(
                BillTypeAtomic.CC_BILL
        ));

        List<BillTypeAtomic> cancelledTypes = new ArrayList<>(Arrays.asList(
                BillTypeAtomic.CC_BILL_CANCELLATION
        ));

        List<BillTypeAtomic> returnedTypes = new ArrayList<>(Collections.singletonList(
                BillTypeAtomic.CC_BILL_REFUND
        ));

        List<BillTypeAtomic> allTypes = new ArrayList<>();
        allTypes.addAll(billedTypes);
        allTypes.addAll(cancelledTypes);
        allTypes.addAll(returnedTypes);

        List<Department> transactionDepartments = fetchBillItemsItemDepartmentsWithTransactions(allTypes, fromDate, toDate);

        long totalBilled = 0;
        long totalCancelled = 0;
        long totalReturned = 0;
        long totalNet = 0;

        for (Department txDept : transactionDepartments) {
            ReportTemplateRow row = new ReportTemplateRow();
            row.setDepartment(txDept);

            long billed = countBillsByBillItemDepartment(txDept, billedTypes, fromDate, toDate);
            long cancelled = countBillsByBillItemDepartment(txDept, cancelledTypes, fromDate, toDate);
            long returned = countBillsByBillItemDepartment(txDept, returnedTypes, fromDate, toDate);
            long net = billed - (cancelled + returned);

            row.setBilledCount(billed);
            row.setCancelledCount(cancelled);
            row.setReturnCount(returned);
            row.setNetCount(net);

            inputBundle.getReportTemplateRows().add(row);

            totalBilled += billed;
            totalCancelled += cancelled;
            totalReturned += returned;
            totalNet += net;
        }

        inputBundle.setBilledCount(totalBilled);
        inputBundle.setCancelledCount(totalCancelled);
        inputBundle.setReturnCount(totalReturned);
        inputBundle.setNetCount(totalNet);
    }

    public void fillBundleForInpatientServiceCounts(ReportTemplateRowBundle inputBundle, Date fromDate, Date toDate) {
        List<BillTypeAtomic> billedTypes = new ArrayList<>(Arrays.asList(
                BillTypeAtomic.INWARD_SERVICE_BILL
        ));

        List<BillTypeAtomic> cancelledTypes = new ArrayList<>(Arrays.asList(
                BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION,
                BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION
        ));

        List<BillTypeAtomic> returnedTypes = new ArrayList<>(Collections.singletonList(
                BillTypeAtomic.INWARD_SERVICE_BILL_REFUND
        ));

        List<BillTypeAtomic> allTypes = new ArrayList<>();
        allTypes.addAll(billedTypes);
        allTypes.addAll(cancelledTypes);
        allTypes.addAll(returnedTypes);

        List<Department> transactionDepartments = fetchBillItemsItemDepartmentsWithTransactions(allTypes, fromDate, toDate);

        long totalBilled = 0;
        long totalCancelled = 0;
        long totalReturned = 0;
        long totalNet = 0;

        for (Department txDept : transactionDepartments) {
            ReportTemplateRow row = new ReportTemplateRow();
            row.setDepartment(txDept);

            long billed = countBillsByBillItemDepartment(txDept, billedTypes, fromDate, toDate);
            long cancelled = countBillsByBillItemDepartment(txDept, cancelledTypes, fromDate, toDate);
            long returned = countBillsByBillItemDepartment(txDept, returnedTypes, fromDate, toDate);
            long net = billed - (cancelled + returned);

            row.setBilledCount(billed);
            row.setCancelledCount(cancelled);
            row.setReturnCount(returned);
            row.setNetCount(net);

            inputBundle.getReportTemplateRows().add(row);

            totalBilled += billed;
            totalCancelled += cancelled;
            totalReturned += returned;
            totalNet += net;
        }

        inputBundle.setBilledCount(totalBilled);
        inputBundle.setCancelledCount(totalCancelled);
        inputBundle.setReturnCount(totalReturned);
        inputBundle.setNetCount(totalNet);
    }

    public void fillBundleForOpdPharmacyCounts(ReportTemplateRowBundle inputBundle, Date fromDate, Date toDate) {
        List<BillTypeAtomic> billedTypes = new ArrayList<>(Arrays.asList(
                BillTypeAtomic.PHARMACY_RETAIL_SALE,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER,
                BillTypeAtomic.PHARMACY_WHOLESALE,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_WITHOUT_STOCKS
        ));

        List<BillTypeAtomic> cancelledTypes = new ArrayList<>(Arrays.asList(
                BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED,
                BillTypeAtomic.PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION,
                BillTypeAtomic.PHARMACY_WHOLESALE_CANCELLED
        ));

        List<BillTypeAtomic> returnedTypes = new ArrayList<>(Arrays.asList(
                BillTypeAtomic.PHARMACY_RETAIL_SALE_REFUND,
                BillTypeAtomic.PHARMACY_WHOLESALE_REFUND
        ));

        List<BillTypeAtomic> allTypes = new ArrayList<>();
        allTypes.addAll(billedTypes);
        allTypes.addAll(cancelledTypes);
        allTypes.addAll(returnedTypes);

        List<Department> transactionDepartments = fetchBilledDepartmentsWithTransactions(allTypes, fromDate, toDate);

        long totalBilled = 0;
        long totalCancelled = 0;
        long totalReturned = 0;
        long totalNet = 0;

        for (Department txDept : transactionDepartments) {
            ReportTemplateRow row = new ReportTemplateRow();
            row.setDepartment(txDept);

            long billed = countBillsByBilledDepartment(txDept, billedTypes, fromDate, toDate);
            long cancelled = countBillsByBilledDepartment(txDept, cancelledTypes, fromDate, toDate);
            long returned = countBillsByBilledDepartment(txDept, returnedTypes, fromDate, toDate);
            long net = billed - (cancelled + returned);

            row.setBilledCount(billed);
            row.setCancelledCount(cancelled);
            row.setReturnCount(returned);
            row.setNetCount(net);

            inputBundle.getReportTemplateRows().add(row);

            totalBilled += billed;
            totalCancelled += cancelled;
            totalReturned += returned;
            totalNet += net;
        }

        inputBundle.setBilledCount(totalBilled);
        inputBundle.setCancelledCount(totalCancelled);
        inputBundle.setReturnCount(totalReturned);
        inputBundle.setNetCount(totalNet);
    }

    public void fillBundleForInpatientPharmacyCounts(ReportTemplateRowBundle inputBundle, Date fromDate, Date toDate) {
        List<BillTypeAtomic> billedTypes = new ArrayList<>(Arrays.asList(
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_THEATRE,
                BillTypeAtomic.ACCEPT_ISSUED_MEDICINE_INWARD,
                BillTypeAtomic.ACCEPT_ISSUED_MEDICINE_THEATRE
        ));

        List<BillTypeAtomic> cancelledTypes = new ArrayList<>(Arrays.asList(
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_THEATRE_CANCELLATION
        ));

        List<BillTypeAtomic> returnedTypes = new ArrayList<>(Arrays.asList(
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN,
                BillTypeAtomic.RETURN_MEDICINE_INWARD,
                BillTypeAtomic.RETURN_MEDICINE_THEATRE,
                BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD,
                BillTypeAtomic.ACCEPT_RETURN_MEDICINE_THEATRE
        ));

        List<BillTypeAtomic> allTypes = new ArrayList<>();
        allTypes.addAll(billedTypes);
        allTypes.addAll(cancelledTypes);
        allTypes.addAll(returnedTypes);

        List<Department> transactionDepartments = fetchBilledDepartmentsWithTransactions(allTypes, fromDate, toDate);

        long totalBilled = 0;
        long totalCancelled = 0;
        long totalReturned = 0;
        long totalNet = 0;

        for (Department txDept : transactionDepartments) {
            ReportTemplateRow row = new ReportTemplateRow();
            row.setDepartment(txDept);

            long billed = countBillsByBilledDepartment(txDept, billedTypes, fromDate, toDate);
            long cancelled = countBillsByBilledDepartment(txDept, cancelledTypes, fromDate, toDate);
            long returned = countBillsByBilledDepartment(txDept, returnedTypes, fromDate, toDate);
            long net = billed - (cancelled + returned);

            row.setBilledCount(billed);
            row.setCancelledCount(cancelled);
            row.setReturnCount(returned);
            row.setNetCount(net);

            inputBundle.getReportTemplateRows().add(row);

            totalBilled += billed;
            totalCancelled += cancelled;
            totalReturned += returned;
            totalNet += net;
        }

        inputBundle.setBilledCount(totalBilled);
        inputBundle.setCancelledCount(totalCancelled);
        inputBundle.setReturnCount(totalReturned);
        inputBundle.setNetCount(totalNet);
    }

    public long countBillsByBillItemDepartment(Department department, List<BillTypeAtomic> billTypes, Date fromDate, Date toDate) {
        if (department == null || billTypes == null || billTypes.isEmpty()) {
            return 0L;
        }

        String jpql = "select count(distinct bi.bill) "
                + "from BillItem bi "
                + "where bi.retired = false "
                + "and bi.bill.retired = false "
                + "and bi.item.department = :dept "
                + "and bi.bill.createdAt between :fd and :td "
                + "and bi.bill.billTypeAtomic in :bts";

        Map<String, Object> params = new HashMap<>();
        params.put("dept", department);
        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("bts", billTypes);

        return billItemFacade.findLongByJpql(jpql, params); // or use count method from AbstractFacade
    }

    public long countBillsByBilledDepartment(Department department, List<BillTypeAtomic> billTypes, Date fromDate, Date toDate) {
        if (department == null || billTypes == null || billTypes.isEmpty()) {
            return 0L;
        }

        String jpql = "select count(b) "
                + "from Bill b "
                + "where b.retired = false "
                + "and b.department = :dept "
                + "and b.createdAt between :fd and :td "
                + "and b.billTypeAtomic in :bts";

        Map<String, Object> params = new HashMap<>();
        params.put("dept", department);
        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("bts", billTypes);

        return billItemFacade.findLongByJpql(jpql, params); // or use count method from AbstractFacade
    }

}
