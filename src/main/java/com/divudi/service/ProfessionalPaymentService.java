package com.divudi.service;

import com.divudi.bean.common.CommonController;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.ReportTemplateRow;
import com.divudi.data.ReportTemplateRowBundle;
import com.divudi.data.ServiceType;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Staff;
import com.divudi.facade.BillFacade;
import com.divudi.java.CommonFunctions;
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
        
        System.out.println("params = " + params);
        System.out.println("jpql = " + jpql);

        Double sum = billFacade.findDoubleByJpql(jpql, params, TemporalType.TIMESTAMP);
        System.out.println("sum = " + sum);
        return sum;
    }

}
