package com.divudi.service;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.PaymentMethod;
import com.divudi.entity.Department;
import com.divudi.entity.Payment;
import com.divudi.entity.WebUser;
import com.divudi.entity.cashTransaction.CashBook;
import com.divudi.entity.cashTransaction.CashBookEntry;
import com.divudi.facade.CashBookEntryFacade;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author Dr M H B Ariyaratne
 *
 */
@Stateless
public class CashbookService {
    
    @EJB
    CashBookEntryFacade cashbookEntryFacade;

    public void writeCashBookEntryAtPaymentCreation(Payment p, WebUser user, CashBook cashbook, Department department) {
        if (p == null) {
            JsfUtil.addErrorMessage("Cashbook Entry Error !");
            return;
        }
        CashBookEntry current = new CashBookEntry();
        current.setInstitution(p.getInstitution());
        current.setDepartment(p.getDepartment());
        current.setCreater(user);
        current.setCreatedAt(new Date());
        current.setPaymentMethod(p.getPaymentMethod());
        current.setEntryValue(p.getPaidValue());
        current.setPayment(p);
        current.setCashBook(cashbook);
        current.setSite(department.getSite());
        current.setBill(p.getBill());
        updateBalances(p.getPaymentMethod(), p.getPaidValue(), current);
        cashbookEntryFacade.create(current);

    }
    
    public void updateBalances(PaymentMethod pm, Double Value, CashBookEntry cbe) {
        Map m = new HashMap<>();
        String jpql = "Select cbe from CashBookEntry cbe where "
                + " cbe.paymentMethod=:pm";

        m.put("pm", pm);

        if (cbe.getDepartment() != null) {
            jpql += " and cbe.department=:dep ";
            m.put("dep", cbe.getDepartment());
        }
        if (cbe.getInstitution() != null) {
            jpql += " and cbe.institution=:ins ";
            m.put("ins", cbe.getInstitution());
        }
        if (cbe.getDepartment() != null) {
            jpql += " and cbe.site=:si ";
            m.put("si", cbe.getSite());
        }

        jpql += "order by cbe.id desc";

        CashBookEntry lastCashBookEntry = cashbookEntryFacade.findFirstByJpql(jpql, m);

        Double lastDepartmentBalance;
        Double lastInstitutionBalance;
        Double lastSiteBalance;

        if (lastCashBookEntry == null) {
            lastDepartmentBalance = 0.0;
            lastInstitutionBalance = 0.0;
            lastSiteBalance = 0.0;
        } else {

        }
    }

}
