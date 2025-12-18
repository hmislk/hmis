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
                + "cat.name, item.name, cat.id, item.id, "
                + "bi.grossValue, bi.hospitalFee, bi.discount, bi.staffFee, "
                + "bi.netValue, bi.qty, bill.paymentMethod, bill.billTypeAtomic, "
                + "bill.billClassType, coalesce(dept.name, ''), bill.createdAt) "
                + "from BillItem bi "
                + "left join bi.item item "
                + "left join item.category cat "
                + "left join bi.bill bill "
                + "left join bill.department dept "
                + "where bill.retired = false "
                + "and bi.retired = false "
                + "and bill.createdAt between :fd and :td ";

        // Add OPD service types filter
        List<BillTypeAtomic> btas = BillTypeAtomic.findByServiceType(ServiceType.OPD);
        if (!btas.isEmpty()) {
            jpql += "and bill.billTypeAtomic in :bts ";
        }

        jpql += "order by cat.name, item.name";

        Map<String, Object> params = new HashMap<>();
        params.put("fd", fromDate);
        params.put("td", toDate);
        if (!btas.isEmpty()) {
            params.put("bts", btas);
        }

        return (List<BillItemDetailDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
    }
    
    public List<BillItemDetailDTO> fetchCcCollectionBillItemsForDailyReturn(Date fromDate, Date toDate) {
        String jpql = "select new com.divudi.core.data.dto.BillItemDetailDTO("
                + "cat.name, item.name, cat.id, item.id, "
                + "bi.grossValue, bi.hospitalFee, bi.discount, bi.staffFee, "
                + "bi.netValue, bi.qty, bill.paymentMethod, bill.billTypeAtomic, "
                + "bill.billClassType, coalesce(dept.name, ''), bill.createdAt) "
                + "from BillItem bi "
                + "left join bi.item item "
                + "left join item.category cat "
                + "left join bi.bill bill "
                + "left join bill.department dept "
                + "where bill.retired = false "
                + "and bi.retired = false "
                + "and bill.createdAt between :fd and :td "
                + "and bill.billTypeAtomic in :bts "
                + "order by cat.name, item.name";

        // CC specific BillTypeAtomic values from the original generateCcCollection method
        List<BillTypeAtomic> ccBillTypes = new ArrayList<>();
        ccBillTypes.add(BillTypeAtomic.CC_PAYMENT_RECEIVED_BILL);
        ccBillTypes.add(BillTypeAtomic.CC_PAYMENT_CANCELLATION_BILL);

        Map<String, Object> params = new HashMap<>();
        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("bts", ccBillTypes);

        return (List<BillItemDetailDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
    }
    
    public List<DailyReturnDetailDTO> fetchCcCollectionBillsForDailyReturn(Date fromDate, Date toDate) {
        String jpql = "select new com.divudi.core.data.dto.DailyReturnDetailDTO("
                + "b.deptId, b.billType, b.billClassType, b.netTotal, b.createdAt, "
                + "b.paymentMethod, coalesce(dept.name, ''), "
                + "coalesce(fromDept.name, '')) "
                + "from Bill b "
                + "left join b.department dept "
                + "left join b.fromDepartment fromDept "
                + "where b.retired = false "
                + "and b.createdAt between :fd and :td "
                + "and b.billTypeAtomic in :bts "
                + "order by b.createdAt";

        List<BillTypeAtomic> ccBillTypes = new ArrayList<>();
        ccBillTypes.add(BillTypeAtomic.CC_PAYMENT_RECEIVED_BILL);
        ccBillTypes.add(BillTypeAtomic.CC_PAYMENT_CANCELLATION_BILL);

        Map<String, Object> params = new HashMap<>();
        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("bts", ccBillTypes);

        return (List<DailyReturnDetailDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
    }
    
    public List<PaymentDetailDTO> fetchCreditCardPaymentsForDailyReturn(Date fromDate, Date toDate) {
        List<PaymentDetailDTO> allResults = new ArrayList<>();

        // 1. Get regular Card payments
        String paymentJpql = "select new com.divudi.core.data.dto.PaymentDetailDTO("
                + "bill.deptId, bill.billType, p.paymentMethod, p.paidValue, p.createdAt, "
                + "p.creditCardRefNo, "
                + "coalesce(bank.name, ''), "
                + "coalesce(inst.name, ''), "
                + "coalesce(dept.name, '')) "
                + "from Payment p "
                + "left join p.bill bill "
                + "left join p.bank bank "
                + "left join p.institution inst "
                + "left join bill.department dept "
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
                + "coalesce(dept.name, '')) "  // departmentName
                + "from Bill b "
                + "left join b.department dept "
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
                + "coalesce(pers.name, ''), "  // bankName field used for patient name - using JOIN to avoid UnitOfWork issue
                + "'', "  // institutionName - not applicable
                + "coalesce(dept.name, '')) "  // departmentName
                + "from Bill b "
                + "left join b.patient pat "
                + "left join pat.person pers "
                + "left join b.department dept "
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
