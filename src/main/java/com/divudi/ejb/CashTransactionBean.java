/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import com.divudi.entity.Bill;
import com.divudi.entity.WebUser;
import com.divudi.entity.cashTransaction.CashTransaction;
import com.divudi.entity.cashTransaction.Drawer;
import com.divudi.facade.BillFacade;
import com.divudi.facade.CashTransactionFacade;
import com.divudi.facade.CashTransactionHistoryFacade;
import com.divudi.facade.DrawerFacade;
import com.divudi.facade.WebUserFacade;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author safrin
 */
@Stateless
public class CashTransactionBean {

    @EJB
    CashTransactionHistoryFacade cashTransactionHistoryFacade;
    @EJB
    private DrawerFacade drawerFacade;
    @EJB
    private CashTransactionFacade cashTransactionFacade;
    @EJB
    private WebUserFacade webUserFacade;

    public void updateDrawers() {
//        //   System.err.println("1");
//        List<Drawer> items = getDrawerFacade().findBySQL("Select d from Drawer d where d.retired=false ");
//        //   System.err.println("2");
//        for (Drawer dr : items) {
//            dr.setWebUsers(getUser(dr));
//            getDrawerFacade().edit(dr);
//        }
//        //   System.err.println("3");
    }

    public List<WebUser> getUser(Drawer drawer) {
//        String sql = "select wu from WebUser wu "
//                + " where wu.retired=false "
//                + " and wu.drawer=:d";
//        HashMap hm = new HashMap();
//        hm.put("d", drawer);
//        List<WebUser> wb = getWebUserFacade().findBySQL(sql, hm);
//        if (wb == null) {
//            wb = new ArrayList<>();
//        }

        return new ArrayList<>();
    }

    public CashTransaction setCashTransactionValue(CashTransaction cashTransaction, Bill bill) {
//        if (bill.getPaymentMethod() == null) {
//            return cashTransaction;
//        }
//        switch (bill.getPaymentMethod()) {
//            case Cash:
//                cashTransaction.setCashValue(bill.getNetTotal());
//                break;
//            case Card:
//                cashTransaction.setCreditCardValue(bill.getNetTotal());
//                break;
//            case Cheque:
//                cashTransaction.setChequeValue(bill.getNetTotal());
//                break;
//            case Slip:
//                cashTransaction.setSlipValue(bill.getNetTotal());
//                break;
//        }

        return cashTransaction;
    }

    public CashTransaction saveCashInTransaction(CashTransaction ct, Bill bill, WebUser webUser) {
//        if (ct == null) {
//            return null;
//        }
//        if (webUser == null) {
//            return null;
//        }
//
//        if (ct.getCashValue() != null) {
//            ct.setCashValue(Math.abs(ct.getCashValue()));
//        }
//
//        if (ct.getSlipValue() != null) {
//            ct.setSlipValue(Math.abs(ct.getSlipValue()));
//        }
//
//        if (ct.getCreditCardValue() != null) {
//            ct.setCreditCardValue(Math.abs(ct.getCreditCardValue()));
//        }
//
//        if (ct.getChequeValue() != null) {
//            ct.setChequeValue(Math.abs(ct.getChequeValue()));
//        }
//
//        ct.setInOutType(InOutType.in);
//        ct.setCreatedAt(new Date());
//        ct.setDrawer(getDrawer(webUser));
//        ct.setCreater(webUser);
//        ct.setBill(bill);
//        if (ct.getId() == null) {
//            getCashTransactionFacade().create(ct);
//        }

        return ct;
    }

    public CashTransaction saveCashOutTransaction(CashTransaction ct, Bill bill, WebUser webUser) {
//        if (ct.getCashValue() != null) {
//            ct.setCashValue(0 - Math.abs(ct.getCashValue()));
//        }
//
//        if (ct.getSlipValue() != null) {
//            ct.setSlipValue(0 - Math.abs(ct.getSlipValue()));
//        }
//
//        if (ct.getCreditCardValue() != null) {
//            ct.setCreditCardValue(0 - Math.abs(ct.getCreditCardValue()));
//        }
//
//        if (ct.getChequeValue() != null) {
//            ct.setChequeValue(0 - Math.abs(ct.getChequeValue()));
//        }
//
//        ct.setInOutType(InOutType.out);
//        ct.setDrawer(getDrawer(webUser));
//        ct.setCreatedAt(new Date());
//        ct.setBill(bill);
//        ct.setCreater(webUser);
//
//        if (ct.getId() == null) {
//            getCashTransactionFacade().create(ct);
//        }

        return ct;
    }

    public CashTransaction saveCashAdjustmentTransactionOut(CashTransaction ct, Bill bill, Drawer drawer, WebUser webUser) {
//
//        ct.setInOutType(InOutType.out);
//        ct.setDrawer(drawer);
//        ct.setCreatedAt(new Date());
//        ct.setBill(bill);
//        ct.setCreater(webUser);
//
//        if (ct.getId() == null) {
//            getCashTransactionFacade().create(ct);
//        }

        return ct;
    }

    public CashTransaction saveCashAdjustmentTransactionIn(CashTransaction ct, Bill bill, Drawer drawer, WebUser webUser) {

//        ct.setInOutType(InOutType.in);
//        ct.setDrawer(drawer);
//        ct.setCreatedAt(new Date());
//        ct.setBill(bill);
//        ct.setCreater(webUser);
//
//        if (ct.getId() == null) {
//            getCashTransactionFacade().create(ct);
//        }
        return ct;
    }

    public CashTransactionHistoryFacade getCashTransactionHistoryFacade() {
        return cashTransactionHistoryFacade;
    }

    public void setCashTransactionHistoryFacade(CashTransactionHistoryFacade cashTransactionHistoryFacade) {
        this.cashTransactionHistoryFacade = cashTransactionHistoryFacade;
    }

    public double calTotal(CashTransaction cashTransaction) {
//        double val = 0;
//        if (cashTransaction.getQty1() != null) {
//            val += cashTransaction.getQty1() * 1;
//        }
//        if (cashTransaction.getQty10() != null) {
//            val += cashTransaction.getQty10() * 10;
//        }
//        if (cashTransaction.getQty100() != null) {
//            val += cashTransaction.getQty100() * 100;
//        }
//        if (cashTransaction.getQty1000() != null) {
//            val += cashTransaction.getQty1000() * 1000;
//        }
//        if (cashTransaction.getQty10000() != null) {
//            val += cashTransaction.getQty10000() * 10000;
//        }
//
//        if (cashTransaction.getQty2() != null) {
//            val += cashTransaction.getQty2() * 2;
//        }
//        if (cashTransaction.getQty20() != null) {
//            val += cashTransaction.getQty20() * 20;
//        }
//        if (cashTransaction.getQty200() != null) {
//            val += cashTransaction.getQty200() * 200;
//        }
//        if (cashTransaction.getQty2000() != null) {
//            val += cashTransaction.getQty2000() * 2000;
//        }
//
//        if (cashTransaction.getQty5() != null) {
//            val += cashTransaction.getQty5() * 5;
//        }
//        if (cashTransaction.getQty50() != null) {
//            val += cashTransaction.getQty50() * 50;
//        }
//        if (cashTransaction.getQty500() != null) {
//            val += cashTransaction.getQty500() * 500;
//        }
//        if (cashTransaction.getQty5000() != null) {
//            val += cashTransaction.getQty5000() * 5000;
//        }

//        return val;
        return 0;
    }

    public Drawer getDrawer(WebUser webUser) {
//        if (webUser == null) {
//            return null;
//        }
//
//        Drawer drw = webUser.getDrawer();
//
//        if (drw == null) {
//            drw = new Drawer();
//            drw.setCreatedAt(new Date());
//            drw.setName(webUser.getWebUserPerson().getName() + " 's drawer");
//            drw.getWebUsers().add(webUser);
//
//            if (drw.getId() == null) {
//                getDrawerFacade().create(drw);
//            }
//
//            webUser.setDrawer(drw);
//            getWebUserFacade().edit(webUser);
//        }

//        return drw;
        return new Drawer();
    }

    public void addToTransactionHistory(CashTransaction cashTransaction, Drawer drawer) {
//        if (cashTransaction == null) {
//            return;
//        }
//
//        CashTransactionHistory sh;
//        String sql;
//        sql = "Select sh from CashTransactionHistory sh where sh.cashTransaction=:pbi";
//        Map m = new HashMap();
//        m.put("pbi", cashTransaction);
//        sh = getCashTransactionHistoryFacade().findFirstBySQL(sql, m);
//        if (sh == null) {
//            sh = new CashTransactionHistory();
//        } else {
//            return;
//        }
//
//        sh.setFromDate(new Date());
//        sh.setCashTransaction(cashTransaction);
//        sh.setHxDate(Calendar.getInstance().get(Calendar.DATE));
//        sh.setHxMonth(Calendar.getInstance().get(Calendar.MONTH));
//        sh.setHxWeek(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
//        sh.setHxYear(Calendar.getInstance().get(Calendar.YEAR));
//
//        sh.setCashAt(new Date());
//        sh.setCreatedAt(new Date());
//
//        Drawer fetchedDrw = getDrawerFacade().find(drawer.getId());
//
//        sh.setCashBallance(fetchedDrw.getRunningBallance());
//        sh.setCreditCardBallance(fetchedDrw.getCreditCardBallance());
//        sh.setChequeBallance(fetchedDrw.getChequeBallance());
//        sh.setSlipBallance(fetchedDrw.getSlipBallance());
//
//        if (sh.getId() == null) {
//            getCashTransactionHistoryFacade().create(sh);
//        }
//
//        cashTransaction.setCashTransactionHistory(sh);
//        getCashTransactionFacade().edit(cashTransaction);

    }

    @EJB
    BillFacade billFacade;

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    @Deprecated
    public WebUser saveBillCashInTransaction(Bill bill, WebUser webUser) {
//        if (bill == null) {
//            return null;
//        }
//
//        if (webUser == null) {
//            return null;
//        }
//
//        CashTransaction cashTransaction = setCashTransactionValue(new CashTransaction(), bill);
//        saveCashInTransaction(cashTransaction, bill, webUser);
//
//        bill.setCashTransaction(cashTransaction);
//        getBillFacade().edit(bill);
//
//        addToBallance(webUser.getDrawer(), cashTransaction);
//
//        WebUser w = getWebUserFacade().find(webUser.getId());

        return webUser;
    }

    @Deprecated
    public WebUser saveBillCashOutTransaction(Bill bill, WebUser webUser) {
//        CashTransaction cashTransaction = setCashTransactionValue(new CashTransaction(), bill);
//        saveCashOutTransaction(cashTransaction, bill, webUser);
//
//        bill.setCashTransaction(cashTransaction);
//        getBillFacade().edit(bill);
//
//        deductFromBallance(webUser.getDrawer(), cashTransaction);
//
//        WebUser w = getWebUserFacade().find(webUser.getId());

        return webUser;
    }

    public boolean addToBallance(Drawer drawer, CashTransaction cashTransaction) {
//        if (drawer == null) {
//            return false;
//        }
//
//        if (drawer.getId() == null) {
//            return false;
//        }
//
//        addToTransactionHistory(cashTransaction, drawer);
//
//        Drawer fetchedDrw = getDrawerFacade().find(drawer.getId());
//
//        if (cashTransaction.getCashValue() != null) {
//            fetchedDrw.setRunningBallance(fetchedDrw.getRunningBallance() + Math.abs(cashTransaction.getCashValue()));
//        }
//        if (cashTransaction.getChequeValue() != null) {
//            fetchedDrw.setChequeBallance(fetchedDrw.getChequeBallance() + Math.abs(cashTransaction.getChequeValue()));
//        }
//        if (cashTransaction.getCreditCardValue() != null) {
//            fetchedDrw.setCreditCardBallance(fetchedDrw.getCreditCardBallance() + Math.abs(cashTransaction.getCreditCardValue()));
//        }
//        if (cashTransaction.getSlipValue() != null) {
//            fetchedDrw.setSlipBallance(fetchedDrw.getSlipBallance() + Math.abs(cashTransaction.getSlipValue()));
//        }
//
//        getDrawerFacade().edit(fetchedDrw);

        return true;
    }

    public boolean deductFromBallance(Drawer drawer, CashTransaction cashTransaction) {
//        if (drawer == null) {
//            return false;
//        }
//
//        if (drawer.getId() == null) {
//            return false;
//        }
//
//        addToTransactionHistory(cashTransaction, drawer);
//
//        Drawer fetchedDrw = getDrawerFacade().find(drawer.getId());
//
//        if (cashTransaction.getCashValue() != null) {
//            fetchedDrw.setRunningBallance(fetchedDrw.getRunningBallance() - Math.abs(cashTransaction.getCashValue()));
//        }
//        if (cashTransaction.getChequeValue() != null) {
//            fetchedDrw.setChequeBallance(fetchedDrw.getChequeBallance() - Math.abs(cashTransaction.getChequeValue()));
//        }
//        if (cashTransaction.getCreditCardValue() != null) {
//            fetchedDrw.setCreditCardBallance(fetchedDrw.getCreditCardBallance() - Math.abs(cashTransaction.getCreditCardValue()));
//        }
//        if (cashTransaction.getSlipValue() != null) {
//            fetchedDrw.setSlipBallance(fetchedDrw.getSlipBallance() - Math.abs(cashTransaction.getSlipValue()));
//        }
//
//        getDrawerFacade().edit(fetchedDrw);
//
        return true;
    }

    public boolean resetBallance(Drawer drawer, CashTransaction cashTransaction) {
//        if (drawer == null) {
//            return false;
//        }
//
//        if (drawer.getId() == null) {
//            return false;
//        }
//
//        System.err.println("11 " + drawer);
//        System.err.println("22 " + cashTransaction);
//        addToTransactionHistory(cashTransaction, drawer);
//
//        System.err.println("13 " + drawer);
//        System.err.println("24 " + cashTransaction);
//        Drawer fetchedDrw = getDrawerFacade().find(drawer.getId());
//
//        fetchedDrw.setRunningBallance(cashTransaction.getCashValue());
//        fetchedDrw.setChequeBallance(cashTransaction.getChequeValue());
//        fetchedDrw.setCreditCardBallance(cashTransaction.getCreditCardValue());
//        fetchedDrw.setSlipBallance(cashTransaction.getSlipValue());
//
//        System.err.println("15 " + drawer);
//        System.err.println("26 " + cashTransaction);
//        getDrawerFacade().edit(fetchedDrw);

        return true;
    }

    public CashTransactionFacade getCashTransactionFacade() {
        return cashTransactionFacade;
    }

    public void setCashTransactionFacade(CashTransactionFacade cashTransactionFacade) {
        this.cashTransactionFacade = cashTransactionFacade;
    }

    public DrawerFacade getDrawerFacade() {
        return drawerFacade;
    }

    public void setDrawerFacade(DrawerFacade drawerFacade) {
        this.drawerFacade = drawerFacade;
    }

    public WebUserFacade getWebUserFacade() {
        return webUserFacade;
    }

    public void setWebUserFacade(WebUserFacade webUserFacade) {
        this.webUserFacade = webUserFacade;
    }
}
