package com.divudi.service;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.ServiceType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Staff;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.CommonFunctions;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr M H Buddhika Ariyaratne
 *
 */
@Stateless
public class ProfessionalPaymentService {

    @EJB
    BillFacade billFacade;

    public boolean isProfessionalFeePaid(BillFee billFee) {
        if (billFee == null) {
            return false;
        }
        return billFee.getPaidValue() > 0;
    }

    public boolean isProfessionalFeePaid(BillItem billItem) {
        if (billItem == null) {
            return false;
        }

        String jpql = "SELECT SUM(bf.paidValue) FROM BillFee bf WHERE bf.billItem = :billItem";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("billItem", billItem);

        // Using the provided `findDoubleByJpql` method to execute the JPQL
        double totalPaid = billFacade.findDoubleByJpql(jpql, parameters, null); // No TemporalType needed here

        return totalPaid > 0;
    }

    public boolean isProfessionalFeePaid(Bill bill) {
        if (bill == null) {
            return false;
        }
        String jpql = "SELECT SUM(bf.paidValue) FROM BillFee bf WHERE bf.bill = :bill ";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("bill", bill);
        double totalPaid = billFacade.findDoubleByJpql(jpql, parameters, null); // No TemporalType needed here
        return totalPaid > 0;
    }

    public boolean isProfessionalFeePaid(Bill bill, BillItem billItem) {
        if (bill == null) {
            return false;
        }
        String jpql = "SELECT SUM(bf.paidValue) FROM BillFee bf "
                + " WHERE bf.bill = :bill "
                + " and bf.billItem= :item ";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("bill", bill);
        parameters.put("item", billItem);
        double totalPaid = billFacade.findDoubleByJpql(jpql, parameters, null); // No TemporalType needed here
        return totalPaid > 0;
    }

    public boolean isProfessionalFeePaidForBatchBill(Bill batchBill) {
        if (batchBill == null) {
            return false;
        }

        String jpql = "SELECT SUM(bf.paidValue) "
                + "FROM BillFee bf "
                + "WHERE bf.bill IN (SELECT b FROM Bill b WHERE b.backwardReferenceBill = :batchBill)";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("batchBill", batchBill);

        // Use the existing `findDoubleByJpql` method to execute the query
        double totalPaid = billFacade.findDoubleByJpql(jpql, parameters, null); // No TemporalType needed here

        return totalPaid > 0;
    }

    public Double findSumOfProfessionalPaymentsDone(
            Institution ins,
            Staff staff) {
        Date fromDate = CommonFunctions.getStartOfMonth();
        Date toDodate = CommonFunctions.getEndOfDay();
        return findSumOfProfessionalPaymentsDone(ins, null, staff, fromDate, toDodate);
    }

    public Double findSumOfProfessionalPaymentsDone(
            Institution ins,
            Department dep,
            Staff staff,
            Date fromDate,
            Date toDate) {
        List<BillTypeAtomic> billTypesAtomics = BillTypeAtomic.findByServiceType(ServiceType.PROFESSIONAL_PAYMENT);

        String jpql;
        Map params = new HashMap();

        jpql = "select sum(b.netTotal) "
                + " from Bill b "
                + " where b.billTypeAtomic in :billTypesAtomics "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.toStaff=:staff ";

        if (ins != null) {
            jpql += " and b.institution=:ins ";
            params.put("ins", ins);
        }

        if (dep != null) {
            jpql += " and b.department=:dep ";
            params.put("dep", dep);
        }

        params.put("billTypesAtomics", billTypesAtomics);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);
        params.put("staff", staff);


        Double sum = billFacade.findDoubleByJpql(jpql, params, TemporalType.TIMESTAMP);
        return sum;
    }

}
