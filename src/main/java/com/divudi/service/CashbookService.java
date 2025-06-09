package com.divudi.service;

import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.cashTransaction.CashBook;
import com.divudi.core.entity.cashTransaction.CashBookEntry;
import com.divudi.core.facade.CashBookEntryFacade;
import com.divudi.core.facade.CashBookFacade;
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
    @EJB
    CashBookFacade cashbookFacade;


    public void writeCashBookEntryAtPaymentCreation(Payment p) {
        CashBook cashbook = findAndSaveCashBookBySite(p.getDepartment().getSite(), p.getInstitution(), p.getDepartment());
        writeCashBookEntryAtPaymentCreation(p, p.getCreater(), cashbook, p.getDepartment());
    }

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

    public CashBook findAndSaveCashBookBySite(Institution site, Institution ins, Department dept) {
        if (site==null) {
            return null;
        }

        if (ins==null) {
            return null;
        }

        if (dept==null) {
            return null;
        }

        String sql;
        Map m = new HashMap();
        m.put("ins", ins);
        m.put("dept", dept);
        m.put("site", site);
        m.put("ret", false);
        sql = "select cb "
                + " from CashBook cb "
                + " where cb.institution=:ins"
                + " and cb.department=:dept"
                + " and cb.site=:site"
                + " and cb.retired=:ret";
        CashBook cb = cashbookFacade.findFirstByJpql(sql, m);

        if (cb == null) {
            cb = new CashBook();
            cb.setInstitution(ins);
            cb.setDepartment(dept);
            cb.setSite(site);
            cashbookFacade.create(cb);
        }
        return cb;
    }

}
