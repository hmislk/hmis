/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.LazyBillItem;
import com.divudi.entity.RefundBill;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PatientInvestigationFacade;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import org.primefaces.model.LazyDataModel;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class OpdBillItemSearchController implements Serializable {

    String txtSearch;
    Date fromDate;
    Date toDate;
    @EJB
    CommonFunctions commonFunctions;
    List<Bill> labBills;
    Department department;
    private Institution institution;
    @EJB
    BillFacade billFacade;
    double hosTot;
    double disTot;
    double profTot;
    double netTot;
    double labHandover;
    double hosTotB;
    double disTotB;
    double profTotB;
    double netTotB;
    double labHandoverB;
    double hosTotC;
    double disTotC;
    double profTotC;
    double netTotC;
    double labHandoverC;
    double hosTotR;
    double disTotR;
    double profTotR;
    double netTotR;
    double labHandoverR;
    List<BillItem> billItemsOwn;
    List<BillItem> billItemsAll;
    List<PatientInvestigation> patientInvestigations;
    @EJB
    PatientInvestigationFacade piFacade;
    @EJB
    BillItemFacade billItemFacade;
    @Inject
    SessionController sessionController;
    private SearchKeyword searchKeyword;

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public List<BillItem> getBillItemsAll() {
        if (billItemsOwn == null) {
            String sql;
            Map m = new HashMap();
            m.put("toDate", toDate);
            m.put("fromDate", fromDate);
            m.put("bType", BillType.OpdBill);
            if (txtSearch == null || txtSearch.trim().equals("")) {
                sql = "select bi from BillItem "
                        + " bi join bi.bill b join "
                        + " b.patient.person p where b.billType=:bType and b.createdAt between :fromDate "
                        + " and :toDate order by bi.id desc";
                billItemsOwn = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 100);
            } else {
                sql = "select bi from BillItem bi join bi.bill b join b.patient.person p where "
                        + "b.billType=:bType and (upper(p.name) like '%" + txtSearch.toUpperCase() + "%' "
                        + " or upper(b.insId) like '%" + txtSearch.toUpperCase() + "%' "
                        + " or p.phone like '%" + txtSearch + "%' or "
                        + " upper(bi.item.name) like '%" + txtSearch.toUpperCase() + "%' ) and  "
                        + "b.createdAt between :fromDate and :toDate order by bi.id desc";
                billItemsOwn = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
            }
        }
        return billItemsOwn;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    private LazyDataModel<BillItem> searchBillItems;

    public void makeNull() {
        searchBillItems = null;
        fromDate = null;
        toDate = null;
    }

    public void createTable() {
        searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.OpdBill);
        m.put("ins", getSessionController().getInstitution());

        if (txtSearch == null || txtSearch.trim().equals("")) {
            sql = "select bi from "
                    + " BillItem bi join bi.bill b join b.patient.person p "
                    + " where b.institution=:ins and b.billType=:bType "
                    + " and b.createdAt between :fromDate and :toDate order by bi.id desc";
        } else {
            sql = "select bi from BillItem bi join bi.bill b join b.patient.person"
                    + " p where b.institution=:ins and b.billType=:bType and"
                    + "  (upper(b.patient.person.name) like '%" + txtSearch.toUpperCase() + "%' "
                    + " or upper(b.patient.person.phone) like '%" + txtSearch.toUpperCase() + "%' "
                    + "  or upper(b.insId) like '%" + txtSearch.toUpperCase() + "%' or "
                    + " upper(b.toInstitution.name) like '%" + txtSearch.toUpperCase() + "%' "
                    + " or upper(b.paymentMethod) like '%" + txtSearch.toUpperCase() + "%' "
                    + " or upper(b.paymentScheme.name) like '%" + txtSearch.toUpperCase() + "%' "
                    + " or upper(b.netTotal) like '%" + txtSearch.toUpperCase() + "%' "
                    + " or upper(b.total) like '%" + txtSearch.toUpperCase() + "%' ) "
                    + " and b.createdAt between :fromDate and :toDate order by bi.id desc";
        }
        List<BillItem> tmp = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

        searchBillItems = new LazyBillItem(tmp);

    }

    public void createTableByKeyword2() {
        searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.OpdBill);
        m.put("ins", getSessionController().getInstitution());
        m.put("str", "%" + txtSearch.toUpperCase() + "%");

        if (txtSearch == null || txtSearch.trim().equals("")) {
            UtilityController.addErrorMessage("Please enter Patient name,Phone No,Bill No,Bill Total");
            return;
        }
        sql = "select bi from BillItem bi join bi.bill b join b.patient.person"
                + " p where b.institution=:ins and b.billType=:bType and"
                + "  (upper(b.patient.person.name) like :str "
                + " or upper(b.patient.person.phone) like :str "
                + "  or upper(b.insId) like :str or "
                + " or upper(b.netTotal) like :str "
                + " or upper(b.total) like :str ) "
                + " and b.createdAt between :fromDate and :toDate order by bi.id desc";

        List<BillItem> tmp = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

        searchBillItems = new LazyBillItem(tmp);

    }

   

    public List<BillItem> getBillItemsOwn() {
        return billItemsOwn;
    }

    public void setBillItemsAll(List<BillItem> billItemsAll) {
        this.billItemsAll = billItemsAll;
    }

    public void setBillItemsOwn(List<BillItem> billItemsOwn) {
        this.billItemsOwn = billItemsOwn;
    }

    public double getHosTot() {
        return hosTot;
    }

    public void setHosTot(double hosTot) {
        this.hosTot = hosTot;
    }

    public double getDisTot() {
        return disTot;
    }

    public void setDisTot(double disTot) {
        this.disTot = disTot;
    }

    public double getProfTot() {
        return profTot;
    }

    public void setProfTot(double profTot) {
        this.profTot = profTot;
    }

    public double getNetTot() {
        return netTot;
    }

    public void setNetTot(double netTot) {
        this.netTot = netTot;
    }

    public double getLabHandover() {
        return labHandover;
    }

    public void setLabHandover(double labHandover) {
        this.labHandover = labHandover;
    }

    public void clearTotals() {
        hosTot = 0;
        disTot = 0;
        profTot = 0;
        netTot = 0;
        labHandover = 0;
        hosTotC = 0;
        disTotC = 0;
        profTotC = 0;
        netTotC = 0;
        labHandoverC = 0;
        hosTotB = 0;
        disTotB = 0;
        profTotB = 0;
        netTotB = 0;
        labHandoverB = 0;
        hosTotR = 0;
        disTotR = 0;
        profTotR = 0;
        netTotR = 0;
        labHandoverR = 0;
    }

    public void calTotals() {
        clearTotals();
        String sql;
        Map tm;

        sql = "select sum(f.total - f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", BilledBill.class);
        hosTotB = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.total - f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", CancelledBill.class);
        hosTotC = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.total - f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", RefundBill.class);
        hosTotR = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        hosTot = hosTotB + hosTotC + hosTotR;

        sql = "select sum(f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", BilledBill.class);
        profTotB = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", CancelledBill.class);
        profTotC = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", RefundBill.class);
        profTotR = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        profTot = profTotB + profTotC + profTotR;

        sql = "select sum(f.discount) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", BilledBill.class);
        disTotB = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.discount) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", CancelledBill.class);
        disTotC = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.discount) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", RefundBill.class);
        disTotR = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        disTot = disTotB + disTotC + disTotR;

//        hosTot = 0;
//        disTot = 0;
//        profTot = 0;
        netTot = hosTot + profTot - disTot;
        netTotC = hosTotC + profTotC - disTotC;
        netTotB = hosTotB + profTotB - disTotB;
        netTotR = hosTotR + profTotR - disTotR;
        labHandover = netTot - profTot;

    }

    public void calTotalsWithout(Institution ins) {
        clearTotals();
        String sql;
        Map tm;

        sql = "select sum(f.total - f.staffFee) from Bill f where f.institution.id=" + ins.getId() + " and f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", BilledBill.class);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        hosTotB = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.total - f.staffFee) from Bill f where f.institution.id=" + ins.getId() + " and f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", CancelledBill.class);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        hosTotC = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.total - f.staffFee) from Bill f where f.institution.id=" + ins.getId() + " and f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", RefundBill.class);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        hosTotR = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        hosTot = hosTotB + hosTotC + hosTotR;

        sql = "select sum(f.staffFee) from Bill f where f.institution.id=" + ins.getId() + " and f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", BilledBill.class);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        profTotB = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.staffFee) from Bill f where f.institution.id=" + ins.getId() + " and f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", CancelledBill.class);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        profTotC = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.staffFee) from Bill f where f.institution.id=" + ins.getId() + " and f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", RefundBill.class);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        profTotR = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        profTot = profTotB + profTotC + profTotR;

        sql = "select sum(f.discount) from Bill f where f.institution.id=" + ins.getId() + " and f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", BilledBill.class);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        disTotB = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.discount) from Bill f where f.institution.id=" + ins.getId() + " and f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", CancelledBill.class);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        disTotC = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.discount) from Bill f where f.institution.id=" + ins.getId() + " and f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", RefundBill.class);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        disTotR = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        disTot = disTotB + disTotC + disTotR;

//        hosTot = 0;
//        disTot = 0;
//        profTot = 0;
        netTot = hosTot + profTot - disTot;
        netTotC = hosTotC + profTotC - disTotC;
        netTotB = hosTotB + profTotB - disTotB;
        netTotR = hosTotR + profTotR - disTotR;
        labHandover = netTot - profTot;

    }

    public void calTotalsWithout() {
        clearTotals();
        String sql;
        Map tm;

        sql = "select sum(f.total - f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", BilledBill.class);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        hosTotB = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.total - f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", CancelledBill.class);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        hosTotC = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.total - f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", RefundBill.class);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        hosTotR = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        hosTot = hosTotB + hosTotC + hosTotR;

        sql = "select sum(f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", BilledBill.class);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        profTotB = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", CancelledBill.class);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        profTotC = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", RefundBill.class);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        profTotR = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        profTot = profTotB + profTotC + profTotR;

        sql = "select sum(f.discount) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", BilledBill.class);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        disTotB = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.discount) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", CancelledBill.class);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        disTotC = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.discount) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", RefundBill.class);
        tm.put("pm1", PaymentMethod.Cash);
        tm.put("pm2", PaymentMethod.Card);
        tm.put("pm3", PaymentMethod.Cheque);
        disTotR = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        disTot = disTotB + disTotC + disTotR;

//        hosTot = 0;
//        disTot = 0;
//        profTot = 0;
        netTot = hosTot + profTot - disTot;
        netTotC = hosTotC + profTotC - disTotC;
        netTotB = hosTotB + profTotB - disTotB;
        netTotR = hosTotR + profTotR - disTotR;
        labHandover = netTot - profTot;

    }

    public double getHosTotB() {
        return hosTotB;
    }

    public void setHosTotB(double hosTotB) {
        this.hosTotB = hosTotB;
    }

    public double getDisTotB() {
        return disTotB;
    }

    public void setDisTotB(double disTotB) {
        this.disTotB = disTotB;
    }

    public double getProfTotB() {
        return profTotB;
    }

    public void setProfTotB(double profTotB) {
        this.profTotB = profTotB;
    }

    public double getNetTotB() {
        return netTotB;
    }

    public void setNetTotB(double netTotB) {
        this.netTotB = netTotB;
    }

    public double getLabHandoverB() {
        return labHandoverB;
    }

    public void setLabHandoverB(double labHandoverB) {
        this.labHandoverB = labHandoverB;
    }

    public double getHosTotC() {
        return hosTotC;
    }

    public void setHosTotC(double hosTotC) {
        this.hosTotC = hosTotC;
    }

    public double getDisTotC() {
        return disTotC;
    }

    public void setDisTotC(double disTotC) {
        this.disTotC = disTotC;
    }

    public double getProfTotC() {
        return profTotC;
    }

    public void setProfTotC(double profTotC) {
        this.profTotC = profTotC;
    }

    public double getNetTotC() {
        return netTotC;
    }

    public void setNetTotC(double netTotC) {
        this.netTotC = netTotC;
    }

    public double getLabHandoverC() {
        return labHandoverC;
    }

    public void setLabHandoverC(double labHandoverC) {
        this.labHandoverC = labHandoverC;
    }

    public double getHosTotR() {
        return hosTotR;
    }

    public void setHosTotR(double hosTotR) {
        this.hosTotR = hosTotR;
    }

    public double getDisTotR() {
        return disTotR;
    }

    public void setDisTotR(double disTotR) {
        this.disTotR = disTotR;
    }

    public double getProfTotR() {
        return profTotR;
    }

    public void setProfTotR(double profTotR) {
        this.profTotR = profTotR;
    }

    public double getNetTotR() {
        return netTotR;
    }

    public void setNetTotR(double netTotR) {
        this.netTotR = netTotR;
    }

    public double getLabHandoverR() {
        return labHandoverR;
    }

    public void setLabHandoverR(double labHandoverR) {
        this.labHandoverR = labHandoverR;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public List<Bill> getLabBills() {
        if (labBills == null) {
            String sql;
            sql = "select f from Bill f where f.retired=false and f.billType = :billType and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
            Map tm = new HashMap();
            tm.put("fromDate", fromDate);
            tm.put("toDate", toDate);
            tm.put("billType", BillType.OpdBill);
            labBills = getBillFacade().findBySQL(sql, tm, TemporalType.TIMESTAMP);
            calTotals();
        }
        return labBills;
    }

    public List<Bill> getLabBillsWithout() {
        if (labBills == null) {
            String sql;
            Map tm;
            if (getInstitution() == null) {
                sql = "select f from Bill f where f.retired=false and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and  f.createdAt between :fromDate and :toDate order by type(f), f.insId";
                tm = new HashMap();
                tm.put("fromDate", fromDate);
                tm.put("toDate", toDate);
                tm.put("billType", BillType.OpdBill);
                tm.put("pm1", PaymentMethod.Cash);
                tm.put("pm2", PaymentMethod.Card);
                tm.put("pm3", PaymentMethod.Cheque);
                labBills = getBillFacade().findBySQL(sql, tm, TemporalType.TIMESTAMP);
                calTotalsWithout();
            } else {
                sql = "select f from Bill f where f.retired=false and f.billType = :billType and (f.paymentMethod = :pm1 or f.paymentMethod = :pm2 or f.paymentMethod = :pm3 ) and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
                tm = new HashMap();
                tm.put("fromDate", fromDate);
                tm.put("toDate", toDate);
                tm.put("billType", BillType.OpdBill);
                tm.put("pm1", PaymentMethod.Cash);
                tm.put("pm2", PaymentMethod.Card);
                tm.put("pm3", PaymentMethod.Cheque);
                labBills = getBillFacade().findBySQL(sql, tm, TemporalType.TIMESTAMP);
                calTotalsWithout(getInstitution());
            }
        }
        return labBills;
    }

    public List<Bill> getLabBillsIns() {
        if (labBills == null) {
            String sql;
            if (getInstitution() == null) {
                sql = "select f from Bill f where f.retired=false and f.billType = :billType and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
                Map tm = new HashMap();
                tm.put("fromDate", fromDate);
                tm.put("toDate", toDate);
                tm.put("billType", BillType.OpdBill);
                labBills = getBillFacade().findBySQL(sql, tm, TemporalType.TIMESTAMP);
                calTotals();
            } else {
                sql = "select f from Bill f where f.retired=false and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
                Map tm = new HashMap();
                tm.put("fromDate", fromDate);
                tm.put("toDate", toDate);
                tm.put("billType", BillType.OpdBill);
                labBills = getBillFacade().findBySQL(sql, tm, TemporalType.TIMESTAMP);
                calTotalsIns();
            }
        }
        return labBills;
    }

    private void calTotalsIns() {
        clearTotals();
        String sql;
        Map tm;
        if (getInstitution() == null) {
            return;
        }

        sql = "select sum(f.total - f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", BilledBill.class);
        hosTotB = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.total - f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", CancelledBill.class);
        hosTotC = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.total - f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", RefundBill.class);
        hosTotR = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        hosTot = hosTotB + hosTotC + hosTotR;

        sql = "select sum(f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", BilledBill.class);
        profTotB = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", CancelledBill.class);
        profTotC = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.staffFee) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", RefundBill.class);
        profTotR = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        profTot = profTotB + profTotC + profTotR;

        sql = "select sum(f.discount) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", BilledBill.class);
        disTotB = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.discount) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", CancelledBill.class);
        disTotC = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        sql = "select sum(f.discount) from Bill f where f.retired=false and type(f) = :billClass and f.billType = :billType and f.paymentMethod!=com.divudi.data.PaymentMethod.Credit and f.institution.id=" + getInstitution().getId() + " and f.createdAt between :fromDate and :toDate order by type(f), f.insId";
        tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", BillType.OpdBill);
        tm.put("billClass", RefundBill.class);
        disTotR = getBillFacade().findDoubleByJpql(sql, tm, TemporalType.TIMESTAMP);
        disTot = disTotB + disTotC + disTotR;

//        hosTot = 0;
//        disTot = 0;
//        profTot = 0;
        netTot = hosTot + profTot - disTot;
        netTotC = hosTotC + profTotC - disTotC;
        netTotB = hosTotB + profTotB - disTotB;
        netTotR = hosTotR + profTotR - disTotR;
        labHandover = netTot - profTot;

    }

    public void setLabBills(List<Bill> labBills) {
        this.labBills = labBills;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
        recreteModal();
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
        recreteModal();
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
        recreteModal();
    }

    public String getTxtSearch() {
        return txtSearch;
    }

    public void setTxtSearch(String txtSearch) {
        recreteModal();
        this.txtSearch = txtSearch;
    }

    private void recreteModal() {
        patientInvestigations = null;
        labBills = null;
        billItemsOwn = null;
        billItemsAll = null;
    }

    public PatientInvestigationFacade getPiFacade() {
        return piFacade;
    }

    public void setPiFacade(PatientInvestigationFacade piFacade) {
        this.piFacade = piFacade;
    }

    public List<PatientInvestigation> getPatientInvestigations() {
        String sql;
        if (patientInvestigations == null) {
            Map m = new HashMap();
            m.put("toDate", toDate);
            m.put("fromDate", fromDate);
            if (txtSearch == null || txtSearch.trim().equals("")) {
                sql = "select pi from PatientInvestigation pi join pi.investigation i join pi.billItem.bill b join b.patient.person p where b.createdAt between :fromDate and :toDate order by pi.id desc";
                patientInvestigations = getPiFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 100);
            } else {
                sql = "select pi from PatientInvestigation pi join pi.investigation i join pi.billItem.bill b join b.patient.person p where (upper(p.name) like '%" + txtSearch.toUpperCase() + "%' or upper(b.insId) like '%" + txtSearch.toUpperCase() + "%' or p.phone like '%" + txtSearch + "%' or upper(i.name) like '%" + txtSearch.toUpperCase() + "%' ) and b.createdAt between :fromDate and :toDate order by pi.id desc";
                patientInvestigations = getPiFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
            }
        }
        return patientInvestigations;
    }

    public void setPatientInvestigations(List<PatientInvestigation> patientInvestigations) {
        this.patientInvestigations = patientInvestigations;
    }

    /**
     * Creates a new instance of LabReportSearchController
     */
    public OpdBillItemSearchController() {
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public LazyDataModel<BillItem> getSearchBillItems() {
        return searchBillItems;
    }

    public void setSearchBillItems(LazyDataModel<BillItem> searchBillItems) {
        this.searchBillItems = searchBillItems;
    }

    public SearchKeyword getSearchKeyword() {
        if (searchKeyword == null) {
            searchKeyword = new SearchKeyword();
        }
        return searchKeyword;
    }

    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
    }
}
