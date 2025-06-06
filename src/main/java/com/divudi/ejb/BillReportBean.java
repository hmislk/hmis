/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.facade.BillFacade;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 *
 * @author buddhika
 */
@Stateless
public class BillReportBean {

    @EJB
    BillFacade billFacade;



    public Long calculateRevenueBillItemCount(Date fd, Date td, PaymentMethod pm, Institution institution, Department department, BillType[] billTypes, Class bc) {
        String sql;
        Map<String, Object> m = new HashMap<>();
        sql = "select count(bi) "
                + " from BillItem bi "
                + " where bi.bill.retired=false "
                + " and bi.bill.billType in :billTypes "
                + " and bi.bill.createdAt between :fd and :td "
                + " and type(bi.bill) =:bc ";
        if (pm != null) {
            sql += " and bi.bill.paymentMethod=:pm ";
            m.put("pm", pm);
        }
        if (institution != null) {
            sql += " and bi.bill.toInstitution=:ins ";
            m.put("ins", institution);
        }

        if (department != null) {
            sql += " and bi.bill.toDepartment=:dep ";
            m.put("dep", department);
        }

        List<BillType> bts = Arrays.asList(billTypes);

        m.put("billTypes", bts);
        m.put("bc", bc);
        m.put("fd", fd);
        m.put("td", td);


        return billFacade.findLongByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public Long calculateRevenueBillItemCount(Date fd, Date td, PaymentMethod pm, Institution institution, Department department, BillType[] billTypes) {
        return calculateRevenueBillItemCount(fd, td, pm, institution, department, billTypes, BilledBill.class)
                - calculateRevenueBillItemCount(fd, td, pm, institution, department, billTypes, CancelledBill.class)
                - calculateRevenueBillItemCount(fd, td, pm, institution, department, billTypes, RefundBill.class);
    }

}
