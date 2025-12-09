package com.divudi.service;

import com.divudi.core.data.dto.DailyReturnDTO;
import com.divudi.core.data.dto.DailyReturnDetailDTO;
import com.divudi.core.data.dto.DailyReturnItemDTO;
import com.divudi.core.data.dto.BillItemDetailDTO;
import com.divudi.core.data.dto.PaymentDetailDTO;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.ServiceType;
import com.divudi.core.facade.BillFacade;
import java.util.*;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 * Service executing direct DTO queries for Daily Return reports.
 */
@Stateless
public class DailyReturnDtoService {

    @EJB
    private BillFacade billFacade;

    public List<DailyReturnDTO> fetchDailyReturnByPaymentMethod(Date fromDate, Date toDate) {
        String jpql = "select new com.divudi.core.data.dto.DailyReturnDTO(b.paymentMethod, sum(b.netTotal)) "
                + "from Bill b "
                + "where b.retired=false "
                + "and b.createdAt between :fd and :td "
                + "group by b.paymentMethod";
        Map<String, Object> params = new HashMap<>();
        params.put("fd", fromDate);
        params.put("td", toDate);
        return (List<DailyReturnDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public List<DailyReturnItemDTO> fetchDailyReturnItems(Date fromDate, Date toDate) {
        String jpql = "select new com.divudi.core.data.dto.DailyReturnItemDTO(b.department.name, sum(b.netTotal)) "
                + "from Bill b "
                + "where b.retired=false "
                + "and b.createdAt between :fd and :td "
                + "group by b.department.name";
        Map<String, Object> params = new HashMap<>();
        params.put("fd", fromDate);
        params.put("td", toDate);
        return (List<DailyReturnItemDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public List<DailyReturnDetailDTO> fetchDetailedDailyReturnBills(Date fromDate, Date toDate) {
        String jpql = "select new com.divudi.core.data.dto.DailyReturnDetailDTO("
                + "b.deptId, b.billType, b.billClassType, b.netTotal, b.createdAt, "
                + "b.paymentMethod, b.department.name, "
                + "case when b.fromDepartment is null then '' else b.fromDepartment.name end) "
                + "from Bill b "
                + "where b.retired=false "
                + "and b.createdAt between :fd and :td "
                + "order by b.createdAt";
        Map<String, Object> params = new HashMap<>();
        params.put("fd", fromDate);
        params.put("td", toDate);
        return (List<DailyReturnDetailDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
    }
    
    public List<BillItemDetailDTO> fetchOpdBillItemsForDailyReturn(Date fromDate, Date toDate) {
        String jpql = "select new com.divudi.core.data.dto.BillItemDetailDTO("
                + "bi.item.category.name, bi.item.name, bi.item.category.id, bi.item.id, "
                + "bi.grossValue, bi.hospitalFee, bi.discount, bi.staffFee, "
                + "bi.netValue, bi.qty, bi.bill.paymentMethod, bi.bill.billTypeAtomic, "
                + "bi.bill.billClassType, bi.bill.department.name, bi.bill.createdAt) "
                + "from BillItem bi "
                + "where bi.bill.retired = false "
                + "and bi.retired = false "
                + "and bi.bill.createdAt between :fd and :td ";
        
        // Add OPD service types filter
        List<BillTypeAtomic> btas = BillTypeAtomic.findByServiceType(ServiceType.OPD);
        if (!btas.isEmpty()) {
            jpql += "and bi.bill.billTypeAtomic in :bts ";
        }
        
        jpql += "order by bi.item.category.name, bi.item.name";
        
        Map<String, Object> params = new HashMap<>();
        params.put("fd", fromDate);
        params.put("td", toDate);
        if (!btas.isEmpty()) {
            params.put("bts", btas);
        }
        
        return (List<BillItemDetailDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
    }
    
    public List<BillItemDetailDTO> fetchCcCollectionBillItemsForDailyReturn(Date fromDate, Date toDate) {
        System.out.println("DEBUG: CC Collection - fromDate: " + fromDate + ", toDate: " + toDate);
        
        String jpql = "select new com.divudi.core.data.dto.BillItemDetailDTO("
                + "bi.item.category.name, bi.item.name, bi.item.category.id, bi.item.id, "
                + "bi.grossValue, bi.hospitalFee, bi.discount, bi.staffFee, "
                + "bi.netValue, bi.qty, bi.bill.paymentMethod, bi.bill.billTypeAtomic, "
                + "bi.bill.billClassType, bi.bill.department.name, bi.bill.createdAt) "
                + "from BillItem bi "
                + "where bi.bill.retired = false "
                + "and bi.retired = false "
                + "and bi.bill.createdAt between :fd and :td "
                + "and bi.bill.billTypeAtomic in :bts "
                + "order by bi.item.category.name, bi.item.name";
        
        // CC specific BillTypeAtomic values from the original generateCcCollection method
        List<BillTypeAtomic> ccBillTypes = new ArrayList<>();
        ccBillTypes.add(BillTypeAtomic.CC_PAYMENT_RECEIVED_BILL);
        ccBillTypes.add(BillTypeAtomic.CC_PAYMENT_CANCELLATION_BILL);
        
        System.out.println("DEBUG: CC Collection - looking for billTypeAtomics: " + ccBillTypes);
        
        Map<String, Object> params = new HashMap<>();
        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("bts", ccBillTypes);
        
        List<BillItemDetailDTO> result = (List<BillItemDetailDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        
        return result;
    }
    
    public List<DailyReturnDetailDTO> fetchCcCollectionBillsForDailyReturn(Date fromDate, Date toDate) {
        System.out.println("DEBUG: CC Collection Bill-level - fromDate: " + fromDate + ", toDate: " + toDate);
        
        // First, let's check if ANY bills exist in the date range
        String countJpql = "select count(b) from Bill b where b.retired = false and b.createdAt between :fd and :td";
        Map<String, Object> countParams = new HashMap<>();
        countParams.put("fd", fromDate);
        countParams.put("td", toDate);
        Long totalBills = (Long) billFacade.findSingleScalar(countJpql, countParams);
        System.out.println("DEBUG: CC Collection - Total bills in date range: " + totalBills);
        
        // Check if any bills have CC BillTypeAtomic
        String ccCountJpql = "select count(b) from Bill b where b.retired = false and b.createdAt between :fd and :td and b.billTypeAtomic in :bts";
        List<BillTypeAtomic> ccBillTypes = new ArrayList<>();
        ccBillTypes.add(BillTypeAtomic.CC_PAYMENT_RECEIVED_BILL);
        ccBillTypes.add(BillTypeAtomic.CC_PAYMENT_CANCELLATION_BILL);
        
        Map<String, Object> ccCountParams = new HashMap<>();
        ccCountParams.put("fd", fromDate);
        ccCountParams.put("td", toDate);
        ccCountParams.put("bts", ccBillTypes);
        Long ccBills = (Long) billFacade.findSingleScalar(ccCountJpql, ccCountParams);
        System.out.println("DEBUG: CC Collection - CC bills in date range: " + ccBills);
        
        Map<String, Object> params = new HashMap<>();
        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("bts", ccBillTypes);
        
        System.out.println("DEBUG: CC Collection - Fixed fromDepartment.name issue, testing DTO constructor now...");
        
        // Try without fromDepartment.name entirely first
        String testJpql = "select new com.divudi.core.data.dto.DailyReturnDetailDTO("
                + "b.deptId, b.billType, b.billClassType, b.netTotal, b.createdAt, "
                + "b.paymentMethod, b.department.name, '') "
                + "from Bill b "
                + "where b.retired = false "
                + "and b.createdAt between :fd and :td "
                + "and b.billTypeAtomic in :bts "
                + "order by b.createdAt";
                
        List<DailyReturnDetailDTO> testResult = (List<DailyReturnDetailDTO>) billFacade.findLightsByJpql(testJpql, params, TemporalType.TIMESTAMP);
        System.out.println("DEBUG: CC Collection - Test without fromDepartment: " + (testResult != null ? testResult.size() : "null"));
        
        if (testResult != null && testResult.size() > 0) {
            return testResult;
        }
        
        // If that doesn't work, try COALESCE instead of CASE
        String jpql = "select new com.divudi.core.data.dto.DailyReturnDetailDTO("
                + "b.deptId, b.billType, b.billClassType, b.netTotal, b.createdAt, "
                + "b.paymentMethod, b.department.name, "
                + "coalesce(b.fromDepartment.name, '')) "
                + "from Bill b "
                + "where b.retired = false "
                + "and b.createdAt between :fd and :td "
                + "and b.billTypeAtomic in :bts "
                + "order by b.createdAt";
        
        System.out.println("DEBUG: CC Collection Bill-level - looking for billTypeAtomics: " + ccBillTypes);
        
        List<DailyReturnDetailDTO> result = (List<DailyReturnDetailDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        
        return result;
    }
    
    public List<PaymentDetailDTO> fetchCreditCardPaymentsForDailyReturn(Date fromDate, Date toDate) {
        List<PaymentDetailDTO> allResults = new ArrayList<>();
        
        // 1. Get regular Card payments
        String paymentJpql = "select new com.divudi.core.data.dto.PaymentDetailDTO("
                + "p.bill.deptId, p.bill.billType, p.paymentMethod, p.paidValue, p.createdAt, "
                + "p.creditCardRefNo, "
                + "case when p.bank is null then '' else p.bank.name end, "
                + "case when p.institution is null then '' else p.institution.name end, "
                + "case when p.bill.department is null then '' else p.bill.department.name end) "
                + "from Payment p "
                + "where p.retired = false "
                + "and p.createdAt between :fd and :td "
                + "and p.paymentMethod = :paymentMethod "
                + "order by p.createdAt";
        
        Map<String, Object> paymentParams = new HashMap<>();
        paymentParams.put("fd", fromDate);
        paymentParams.put("td", toDate);
        paymentParams.put("paymentMethod", com.divudi.core.data.PaymentMethod.Card);
        
        List<PaymentDetailDTO> paymentResults = (List<PaymentDetailDTO>) billFacade.findLightsByJpql(paymentJpql, paymentParams, TemporalType.TIMESTAMP);
        allResults.addAll(paymentResults);
        
        // 2. Get Card refund bills (Bills with Card payment method and negative amounts)
        String refundJpql = "select new com.divudi.core.data.dto.PaymentDetailDTO("
                + "b.deptId, b.billType, b.paymentMethod, b.netTotal, b.createdAt, "
                + "'', "  // creditCardRefNo - not applicable for refunds
                + "'', "  // bankName - not applicable for refunds
                + "'', "  // institutionName - not applicable
                + "coalesce(b.department.name, '')) "  // departmentName
                + "from Bill b "
                + "where b.retired = false "
                + "and b.createdAt between :fd and :td "
                + "and b.paymentMethod = :paymentMethod "
                + "and b.netTotal < 0 "
                + "order by b.createdAt";
        
        Map<String, Object> refundParams = new HashMap<>();
        refundParams.put("fd", fromDate);
        refundParams.put("td", toDate);
        refundParams.put("paymentMethod", com.divudi.core.data.PaymentMethod.Card);
        
        List<PaymentDetailDTO> refundResults = (List<PaymentDetailDTO>) billFacade.findLightsByJpql(refundJpql, refundParams, TemporalType.TIMESTAMP);
        allResults.addAll(refundResults);
        
        return allResults;
    }
    
    public List<PaymentDetailDTO> fetchPatientDepositPaymentsForDailyReturn(Date fromDate, Date toDate) {
        String jpql = "select new com.divudi.core.data.dto.PaymentDetailDTO("
                + "b.deptId, b.billType, b.paymentMethod, b.netTotal, b.createdAt, "
                + "'', "  // creditCardRefNo - not applicable for patient deposits
                + "coalesce(b.patient.person.name, ''), "  // bankName field used for patient name
                + "'', "  // institutionName - not applicable
                + "coalesce(b.department.name, '')) "  // departmentName
                + "from Bill b "
                + "where b.retired = false "
                + "and b.createdAt between :fd and :td "
                + "and b.billTypeAtomic in :bts "
                + "order by b.createdAt";
        
        // Patient Deposit specific BillTypeAtomic values
        List<BillTypeAtomic> patientDepositBillTypes = BillTypeAtomic.findByServiceType(ServiceType.PATIENT_DEPOSIT);
        
        Map<String, Object> params = new HashMap<>();
        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("bts", patientDepositBillTypes);
        
        List<PaymentDetailDTO> result = (List<PaymentDetailDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        
        return result;
    }
}
