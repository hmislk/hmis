/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillController;
import com.divudi.bean.common.CommonController;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.InstitutionBills;
import com.divudi.data.table.String1Value5;

import com.divudi.ejb.CreditBean;
import com.divudi.entity.Bill;
import com.divudi.entity.Institution;
import com.divudi.facade.BillFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class DealorDueController implements Serializable {

    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private BillFacade billFacade;
    private CommonFunctions commonFunctions;
    @EJB
    private CreditBean creditBean;

    @Inject
    CommonController commonController;
    @Inject
    private BillController billController;
    @Inject
    private PharmacyDealorBill pharmacyDealorBill;

    private Date fromDate;
    private Date toDate;
    private List<InstitutionBills> items;
    private List<String1Value5> dealorCreditAge;
    private List<String1Value5> filteredList;
    private List<Bill> bills;

    public void makeNull() {
        fromDate = null;
        toDate = null;
        items = null;
        dealorCreditAge = null;
        filteredList = null;
    }

    public DealorDueController() {
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = com.divudi.java.CommonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = new Date();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    private void setValues(Institution inst, String1Value5 dataTable5Value, List<BillType> billTypesBilled, List<BillType> billTypesReturned) {

        List<Bill> lst = getCreditBean().getBills(inst, billTypesBilled);
        for (Bill b : lst) {
            double rt = getCreditBean().getGrnReturnValue(b, billTypesReturned);

            //   double dbl = Math.abs(b.getNetTotal()) - (Math.abs(b.getTmpReturnTotal()) + Math.abs(b.getPaidAmount()));
            b.setTmpReturnTotal(rt);

            Long dayCount = getCommonFunctions().getDayCountTillNow(b.getInvoiceDate());

            double finalValue = (b.getNetTotal() + b.getPaidAmount() + b.getTmpReturnTotal());

            if (dayCount < 30) {
                dataTable5Value.setValue1(dataTable5Value.getValue1() + finalValue);
            } else if (dayCount < 60) {
                dataTable5Value.setValue2(dataTable5Value.getValue2() + finalValue);
            } else if (dayCount < 90) {
                dataTable5Value.setValue3(dataTable5Value.getValue3() + finalValue);
            } else {
                dataTable5Value.setValue4(dataTable5Value.getValue4() + finalValue);
            }

        }

    }

    
    public void fillUnsettledCreditPharmacyBills() {
        BillTypeAtomic[] billTypesArrayBilled = {BillTypeAtomic.PHARMACY_GRN, BillTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL , BillTypeAtomic.PHARMACY_DIRECT_PURCHASE};
        List<BillTypeAtomic> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        bills = billController.findUnpaidBills(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01);
    }
    
    
    @Deprecated
    public void fillPharmacyDue() {
        Date startTime = new Date();
        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        bills = billController.findUnpaidBillsOld(fromDate, toDate, billTypesListBilled, PaymentMethod.Credit, 0.01);

        BillType[] billTypesArrayReturn = {BillType.PharmacyGrnReturn, BillType.PurchaseReturn};
        List<BillType> billTypesListReturn = Arrays.asList(billTypesArrayReturn);
        fillIDealorDue(billTypesListBilled, billTypesListReturn);

        commonController.printReportDetails(fromDate, toDate, startTime, "Pharmacy/Dealer Payments/Dealer due search(Process Pharmacy Due)(/faces/dealorPayment/dealor_due.xhtml )");
    }

    public void fillPharmacyDue1() {
        Date startTime = new Date();
        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        bills = billController.findUnpaidBillsOld(fromDate, toDate, billTypesListBilled, null, null);
    }

    public void fillPharmacyDue2() {
        Date startTime = new Date();
        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        bills = billController.findUnpaidBillsOld(fromDate, toDate, null, PaymentMethod.Credit, null);
    }

    public void fillPharmacyDue3() {
        Date startTime = new Date();
        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        bills = billController.findUnpaidBillsOld(fromDate, toDate, null, null, null);
    }

    public void fillStoreDue() {
        Date startTime = new Date();

        BillType[] billTypesArrayBilled = {BillType.StoreGrnBill, BillType.StorePurchase};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        BillType[] billTypesArrayReturn = {BillType.StoreGrnReturn, BillType.StorePurchaseReturn};
        List<BillType> billTypesListReturn = Arrays.asList(billTypesArrayReturn);
        fillIDealorDue(billTypesListBilled, billTypesListReturn);

        commonController.printReportDetails(fromDate, toDate, startTime, "Pharmacy/Dealer Payments/Dealer due search(Process Store Due)(/faces/dealorPayment/dealor_due.xhtml)");
    }

    public void fillPharmacyStoreDue() {
        Date startTime = new Date();

        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill, BillType.StoreGrnBill, BillType.StorePurchase};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        BillType[] billTypesArrayReturn = {BillType.PharmacyGrnReturn, BillType.PurchaseReturn, BillType.StoreGrnReturn, BillType.StorePurchaseReturn};
        List<BillType> billTypesListReturn = Arrays.asList(billTypesArrayReturn);
        fillIDealorDue(billTypesListBilled, billTypesListReturn);

        commonController.printReportDetails(fromDate, toDate, startTime, "Pharmacy/Dealer Payments/Dealer due search(Process All Due)(/faces/dealorPayment/dealor_due.xhtml)");
    }

    private void fillIDealorDue(List<BillType> billTypeBilled, List<BillType> billTypeReturned) {
        Set<Institution> setIns = new HashSet<>();
        List<Institution> list = getCreditBean().getDealorFromBills(getFromDate(), getToDate(), billTypeBilled);

        list.addAll(getCreditBean().getDealorFromReturnBills(getFromDate(), getToDate(), billTypeReturned));

        setIns.addAll(list);
        items = new ArrayList<>();
        for (Institution ins : setIns) {
            //     System.err.println("Ins " + ins.getName());
            InstitutionBills newIns = new InstitutionBills();
            newIns.setInstitution(ins);
            List<Bill> lst = getCreditBean().getBills(ins, getFromDate(), getToDate(), billTypeBilled);

            newIns.setBills(lst);

            for (Bill b : lst) {
                double rt = getCreditBean().getGrnReturnValue(b, billTypeReturned);
                b.setTmpReturnTotal(rt);

                double dbl = Math.abs(b.getNetTotal()) - (Math.abs(b.getTmpReturnTotal()) + Math.abs(b.getPaidAmount()));

                if (dbl > 0.1) {
                    b.setTransBoolean(true);
                    newIns.setReturned(newIns.getReturned() + b.getTmpReturnTotal());
                    newIns.setTotal(newIns.getTotal() + b.getNetTotal());
                    newIns.setPaidTotal(newIns.getPaidTotal() + b.getPaidAmount());

                }
            }

            double finalValue = (newIns.getPaidTotal() + newIns.getTotal() + newIns.getReturned());
            if (finalValue != 0 && finalValue < 0.1) {
                items.add(newIns);
            }
        }
    }

    @Deprecated
    public List<InstitutionBills> getItems() {
        return items;
    }

    public void fillStoreDueAge() {
        Date startTime = new Date();

        BillType[] billTypesArrayBilled = {BillType.StoreGrnBill, BillType.StorePurchase};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        BillType[] billTypesArrayReturn = {BillType.StoreGrnReturn, BillType.StorePurchaseReturn};
        List<BillType> billTypesListReturn = Arrays.asList(billTypesArrayReturn);

        createAgeTable(billTypesListBilled, billTypesListReturn);

        commonController.printReportDetails(fromDate, toDate, startTime, "Pharmacy/Dealer Payments/Dealer due by age(Process Store Due Age)(/faces/inward/discharge_book_no_changes_due.xhtml)");
    }

    public void fillPharmacyDueAge() {
        Date startTime = new Date();

        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        BillType[] billTypesArrayReturn = {BillType.PharmacyGrnReturn, BillType.PurchaseReturn};
        List<BillType> billTypesListReturn = Arrays.asList(billTypesArrayReturn);

        createAgeTable(billTypesListBilled, billTypesListReturn);

        commonController.printReportDetails(fromDate, toDate, startTime, "Pharmacy/Dealer Payments/Dealer due by age(Process Pharmacy Due Age)(/faces/inward/discharge_book_no_changes_due.xhtml)");
    }

    public void fillPharmacyStoreDueAge() {
        Date startTime = new Date();

        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill, BillType.StoreGrnBill, BillType.StorePurchase};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        BillType[] billTypesArrayReturn = {BillType.PharmacyGrnReturn, BillType.PurchaseReturn, BillType.StoreGrnReturn, BillType.StorePurchaseReturn};
        List<BillType> billTypesListReturn = Arrays.asList(billTypesArrayReturn);
        createAgeTable(billTypesListBilled, billTypesListReturn);

        commonController.printReportDetails(fromDate, toDate, startTime, "Pharmacy/Dealer Payments/Dealer due by age(Process All Due Age)(/faces/inward/discharge_book_no_changes_due.xhtml)");
    }

    private void createAgeTable(List<BillType> billTypesBilled, List<BillType> billTypesReturned) {
        makeNull();
        Set<Institution> setIns = new HashSet<>();

        List<Institution> list = getCreditBean().getDealorFromBills(billTypesBilled);
        list.addAll(getCreditBean().getDealorFromReturnBills(billTypesReturned));

        setIns.addAll(list);

        dealorCreditAge = new ArrayList<>();
        for (Institution ins : setIns) {
            if (ins == null) {
                continue;
            }

            String1Value5 newRow = new String1Value5();
            newRow.setString(ins.getName());
            setValues(ins, newRow, billTypesBilled, billTypesReturned);

            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                dealorCreditAge.add(newRow);
            }
        }

    }

    private Institution institution;

    public List<Bill> getItems2() {
        String sql;
        HashMap hm;

        sql = "Select b From Bill b where b.retired=false and b.createdAt "
                + "  between :frm and :to and b.creditCompany=:cc "
                + " and b.paymentMethod= :pm and b.billType=:tp";
        hm = new HashMap();
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());
        hm.put("cc", getInstitution());
        hm.put("pm", PaymentMethod.Credit);
        hm.put("tp", BillType.OpdBill);
        return getBillFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double getCreditTotal() {
        String sql;
        HashMap hm;

        sql = "Select sum(b.netTotal) From Bill b where b.retired=false and b.createdAt "
                + "  between :frm and :to and b.creditCompany=:cc "
                + " and b.paymentMethod= :pm and b.billType=:tp";
        hm = new HashMap();
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());
        hm.put("cc", getInstitution());
        hm.put("pm", PaymentMethod.Credit);
        hm.put("tp", BillType.OpdBill);
        return getBillFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public void setItems(List<InstitutionBills> items) {
        this.items = items;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public List<String1Value5> getDealorCreditAge() {
        return dealorCreditAge;
    }

    public void setDealorCreditAge(List<String1Value5> dealorCreditAge) {
        this.dealorCreditAge = dealorCreditAge;
    }

    public List<String1Value5> getFilteredList() {
        return filteredList;
    }

    public void setFilteredList(List<String1Value5> filteredList) {
        this.filteredList = filteredList;
    }

    public BillController getBillController() {
        return billController;
    }

    public void setBillController(BillController billController) {
        this.billController = billController;
    }

    public PharmacyDealorBill getPharmacyDealorBill() {
        return pharmacyDealorBill;
    }

    public void setPharmacyDealorBill(PharmacyDealorBill pharmacyDealorBill) {
        this.pharmacyDealorBill = pharmacyDealorBill;
    }

    public CreditBean getCreditBean() {
        return creditBean;
    }

    public void setCreditBean(CreditBean creditBean) {
        this.creditBean = creditBean;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

}
