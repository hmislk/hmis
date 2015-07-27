/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.bean.pharmacy.*;
import com.divudi.bean.memberShip.PaymentSchemeController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.data.table.String1Value2;
import com.divudi.data.table.String1Value3;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.BatchBill;
import com.divudi.entity.Bill;
import com.divudi.entity.BillEntry;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillFeePayment;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.Payment;
import com.divudi.entity.Person;
import com.divudi.entity.PreBill;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillFeePaymentFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.StockFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class OpdPreSettleController implements Serializable {

    /**
     * Creates a new instance of PharmacySaleController
     */
    public OpdPreSettleController() {
    }

    @Inject
    SessionController sessionController;
    @Inject
    OpdPreBillController opdPreBillController;
////////////////////////
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    ItemFacade itemFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    BillFeePaymentFacade billFeePaymentFacade;
/////////////////////////
    Item selectedAlternative;

    private Bill preBill;
    private Bill saleBill;
    private Bill billedBill;
    Bill bill;
    BillItem billItem;
    BillItem removingBillItem;
    BillItem editingBillItem;
    Double qty;
    Stock stock;

    private Patient newPatient;
    private Patient searchedPatient;
    private YearMonthDay yearMonthDay;
    private String patientTabId = "tabNewPt";
    private String strTenderedValue = "";
    boolean billPreview = false;
    /////////////////
    List<Stock> replaceableStocks;
    List<BillItem> billItems;
    List<Item> itemsWithoutStocks;
    /////////////////////////
    //   PaymentScheme paymentScheme;
    private PaymentMethodData paymentMethodData;
    PaymentMethod paymentMethod;
    double cashPaid;
    double reminingCashPaid;
    double netTotal;
    double balance;
    Double editingQty;

    public void makeNull() {
        selectedAlternative = null;
        preBill = null;
        saleBill = null;
        bill = null;
        billItem = null;
        removingBillItem = null;
        editingBillItem = null;
        qty = 0.0;
        stock = null;
        newPatient = null;
        searchedPatient = null;
        yearMonthDay = null;
        patientTabId = "tabNewPt";
        strTenderedValue = "";
        billPreview = false;
        replaceableStocks = null;
        billItems = null;
        itemsWithoutStocks = null;
        paymentMethodData = null;
        cashPaid = 0;
        netTotal = 0;
        balance = 0;
        editingQty = null;

    }

    public Double getEditingQty() {
        return editingQty;
    }

    public void setEditingQty(Double editingQty) {
        this.editingQty = editingQty;
    }

    public void onTabChange(TabChangeEvent event) {
        setPatientTabId(event.getTab().getId());

    }

    public Title[] getTitle() {
        return Title.values();
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    public List<Stock> getReplaceableStocks() {
        return replaceableStocks;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public void setReplaceableStocks(List<Stock> replaceableStocks) {
        this.replaceableStocks = replaceableStocks;
    }

    public Item getSelectedAlternative() {
        return selectedAlternative;
    }

    public void setSelectedAlternative(Item selectedAlternative) {
        this.selectedAlternative = selectedAlternative;
    }

    public List<Item> getItemsWithoutStocks() {
        return itemsWithoutStocks;
    }

    public void setItemsWithoutStocks(List<Item> itemsWithoutStocks) {
        this.itemsWithoutStocks = itemsWithoutStocks;
    }

    public BillItem getBillItem() {
        if (billItem == null) {
            billItem = new BillItem();
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
            pbi.setBillItem(billItem);
        }
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public List<BillItem> getBillItems() {
        if (billItems == null) {
            billItems = new ArrayList<>();
        }
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    @Inject
    private PaymentSchemeController paymentSchemeController;

    @SuppressWarnings("empty-statement")
    private boolean errorCheckForSaleBill() {

        if (getPreBill().getPaymentMethod() == null) {
            return true;
        }

        if (getPaymentSchemeController().errorCheckPaymentMethod(getPreBill().getPaymentMethod(), paymentMethodData));

//        if (getPreBill().getPaymentScheme().getPaymentMethod() == PaymentMethod.Cash) {
//            if (cashPaid == 0.0) {
//                UtilityController.addErrorMessage("Please select tendered amount correctly");
//                return true;
//            }
//            if (cashPaid < getNetTotal()) {
//                UtilityController.addErrorMessage("Please select tendered amount correctly");
//                return true;
//            }
//        }
        return false;
    }

    @Inject
    private BillBeanController billBean;

    private void saveSaleBill() {
        getSaleBill().copy(getPreBill());
        getSaleBill().copyValue(getPreBill());
        getSaleBill().setBalance(getSaleBill().getNetTotal());
        getSaleBill().setBillClassType(BillClassType.BilledBill);
        getSaleBill().setBillType(BillType.OpdBill);

        getSaleBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getSaleBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getBillBean().setPaymentMethodData(getSaleBill(), getSaleBill().getPaymentMethod(), paymentMethodData);

        getSaleBill().setBillDate(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        getSaleBill().setBillTime(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        getSaleBill().setCreatedAt(Calendar.getInstance().getTime());
        getSaleBill().setCreater(getSessionController().getLoggedUser());

        getSaleBill().setReferenceBill(getPreBill());

        getSaleBill().setInsId(getPreBill().getInsId());
        getSaleBill().setDeptId(getPreBill().getDeptId());

        if (getSaleBill().getId() == null) {
            getBillFacade().create(getSaleBill());
        }

        updatePreBill();

    }

    private void updatePreBill() {
        getPreBill().setReferenceBill(getSaleBill());

        getBillFacade().edit(getPreBill());

    }

    private void saveSaleBillItems() {
        for (BillItem tbi : getPreBill().getBillItems()) {
            BillItem newBil = new BillItem();
            newBil.copy(tbi);
            newBil.setBill(getSaleBill());
//            newBil.setInwardChargeType(InwardChargeType.Medicine);
            //      newBil.setBill(getSaleBill());
            newBil.setCreatedAt(Calendar.getInstance().getTime());
            newBil.setCreater(getSessionController().getLoggedUser());

            if (newBil.getId() == null) {
                getBillItemFacade().create(newBil);
            }
            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + tbi.getId();
            List<BillFee> tmp = getBillFeeFacade().findBySQL(sql);
            saveBillFee(tmp, newBil);

            getSaleBill().getBillItems().add(newBil);
        }
        getBillFacade().edit(getSaleBill());

    }

    public void saveBillFee(List<BillFee> bfs, BillItem bi) {
        System.out.println("b.getBillFees() = " + bfs.size());
        for (BillFee bfo : bfs) {
            BillFee bf = new BillFee();
            bf.copy(bfo);
            bf.setCreatedAt(Calendar.getInstance().getTime());
            bf.setCreater(sessionController.getLoggedUser());
            bf.setBillItem(bi);
            bf.setBill(bi.getBill());

            if (bf.getId() == null) {
                getBillFeeFacade().create(bf);
                System.err.println("Add");
            }
        }

    }

    public void settleBillWithPay2() {
        editingQty = null;
        if (errorCheckForSaleBill()) {
            return;
        }

        saveSaleBill();
        saveSaleBillItems();

//        getPreBill().getCashBillsPre().add(getSaleBill());
        getBillFacade().edit(getPreBill());

        WebUser wb = getCashTransactionBean().saveBillCashInTransaction(getSaleBill(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        setBill(getBillFacade().find(getSaleBill().getId()));

        makeNull();
        //    billPreview = true;

    }

    public BilledBill createBilledBillForPreBill(Bill preBill) {
        setPreBill(preBill);
        System.out.println("getPreBill() = " + getPreBill());
        if (errorCheckForSaleBill()) {
//            return;
        }

        saveSaleBill();
        saveSaleBillItems();

        return (BilledBill) getSaleBill();
    }

    @EJB
    CashTransactionBean cashTransactionBean;

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    private void clearBill() {
        preBill = null;
        saleBill = null;
        newPatient = null;
        searchedPatient = null;
        billItems = null;
        patientTabId = "tabNewPt";
        cashPaid = 0;
        netTotal = 0;
        balance = 0;
    }

    private void clearBillItem() {
        billItem = null;
        removingBillItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
        editingQty = null;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public BillItem getRemovingBillItem() {
        return removingBillItem;
    }

    public void setRemovingBillItem(BillItem removingBillItem) {
        this.removingBillItem = removingBillItem;
    }

    public BillItem getEditingBillItem() {
        return editingBillItem;
    }

    public void setEditingBillItem(BillItem editingBillItem) {
        this.editingBillItem = editingBillItem;
    }

    public Patient getNewPatient() {
        if (newPatient == null) {
            newPatient = new Patient();
            Person p = new Person();

            newPatient.setPerson(p);
        }
        return newPatient;
    }

    public void setNewPatient(Patient newPatient) {
        this.newPatient = newPatient;
    }

    public Patient getSearchedPatient() {
        return searchedPatient;
    }

    public void setSearchedPatient(Patient searchedPatient) {
        this.searchedPatient = searchedPatient;
    }

    public YearMonthDay getYearMonthDay() {
        if (yearMonthDay == null) {
            yearMonthDay = new YearMonthDay();
        }
        return yearMonthDay;
    }

    public void setYearMonthDay(YearMonthDay yearMonthDay) {
        this.yearMonthDay = yearMonthDay;
    }

    public String toSettle(Bill args) {
        String sql = "Select b from BilledBill b"
                + " where b.referenceBill=:bil"
                + " and b.retired=false "
                + " and b.cancelled=false ";
        HashMap hm = new HashMap();
        hm.put("bil", args);
        Bill b = getBillFacade().findFirstBySQL(sql, hm);

        if (b != null) {
            UtilityController.addErrorMessage("Allready Paid");
            return "";
        } else {
            setPreBill(args);
            return "/opd_bill_pre_settle";
        }
    }

    public String toSettleBatch(Bill preBatchBill) {
        setPreBill(preBatchBill);
        System.out.println("getPreBill(Batch) = " + getPreBill());
        if (getPreBill().getReferenceBill() == null) {
            //billed bill create for pre bills
            BilledBill tmp = createBatchBilledBill(getPreBill());

            for (Bill pb : getPreBill().getForwardReferenceBills()) {
                //create BilledBills For PreBills
                BilledBill bb = createBilledBillForPreBill(pb);
                bb.setBackwardReferenceBill(tmp);
                System.err.println("Bill");
                System.out.println("bb.getCashPaid = " + bb.getCashPaid());
                System.out.println("bb.getBalance = " + bb.getBalance());
                System.out.println("bb.getTotal() = " + bb.getTotal());
                System.out.println("bb.getDiscount() = " + bb.getDiscount());
                System.out.println("bb.getNetTotal() = " + bb.getNetTotal());
                System.err.println("Bill");
                getBillFacade().edit(bb);
                tmp.getForwardReferenceBills().add(bb);
            }
            System.err.println("Batch Bill");
            System.out.println("tmp.getCashPaid = " + tmp.getCashPaid());
            System.out.println("tmp.getBalance = " + tmp.getBalance());
            System.out.println("tmp.getTotal() = " + tmp.getTotal());
            System.out.println("tmp.getDiscount() = " + tmp.getDiscount());
            System.out.println("tmp.getNetTotal() = " + tmp.getNetTotal());
            System.err.println("Batch Bill");
            tmp.setBalance(tmp.getNetTotal());
            getBillFacade().edit(tmp);
            //set batch billed bill
            setBilledBill(tmp);
        } else {
            //billed bills alredy saved
            setBilledBill(getPreBill().getReferenceBill());
        }

        return "/opd_bill_batch_pre_settle";
    }

    public String settle() {
        System.out.println("cashPaid = " + cashPaid);
        System.out.println("getBilledBill() = " + getBilledBill());

        if (cashPaid < 1) {
            JsfUtil.addErrorMessage("Please enter a valid amount");
            return "";
        }

        if (errorCheck()) {
            return "";
        }

        String str = settleBatchBillAfterFiistTime();
        cashPaid = 0.0;

        return str;
    }

    public boolean errorCheck() {
        if (cashPaid == 0.0 && getSessionController().getInstitutionPreference().isPartialPaymentOfOpdPreBillsAllowed()) {
            JsfUtil.addErrorMessage("Please Enter Correct Amount");
            return true;
        }
        if (getBilledBill() == null) {
            UtilityController.addErrorMessage("Nothing To Pay");
            return true;
        }
        return false;
    }

//    public void settleBatchBillFiistTime(Bill bill) {
//        //create Batch BilledBill
//        BilledBill tmp = createBatchBilledBill(bill);
//
//        double dbl = 0;
//        double pid = 0;
//        reminingCashPaid = cashPaid;
//        Payment p = new Payment();
//        p.setBill(tmp);
//        setPaymentMethodData(p, paymentMethod, paymentMethodData);
//        for (Bill b : bill.getForwardReferenceBills()) {
//            //create BilledBills For PreBills
//            BilledBill bb = createBilledBillForPreBill(b);
//            bb.setBackwardReferenceBill(tmp);
//
//            System.out.println("dbl = " + dbl);
//            System.out.println("reminingCashPaid = " + reminingCashPaid);
//            System.out.println("cashPaid = " + cashPaid);
//
//            for (BillItem bi : bb.getBillItems()) {
//
//                System.out.println("bi = " + bi);
//                String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();
//
//                List<BillFee> billFees = getBillFeeFacade().findBySQL(sql);
//                System.out.println("billFees = " + billFees.size());
//                //for payments for billfees
//
//                calculateBillfeePayments(billFees, p);
//            }
//
//            bb.setCashPaid(calBillPaidValue(bb));
//            bb.setBalance(bb.getNetTotal() - bb.getCashPaid());
//            getBillFacade().edit(bb);
//            //for update Batch Bill
//            dbl += bb.getNetTotal();
//            pid += bb.getCashPaid();
//
//            tmp.getForwardReferenceBills().add(bb);
//        }
//
//        tmp.setCashPaid(pid);
//        tmp.setBalance(dbl - pid);
//        tmp.setNetTotal(dbl);
//        getBillFacade().edit(tmp);
//        JsfUtil.addSuccessMessage("Sucessfully Paid");
//    }
    public String settleBatchBillAfterFiistTime() {

        double dbl = 0;
        double pid = 0;
        reminingCashPaid = cashPaid;
        Payment p = new Payment();
        p.setBill(getBilledBill());
        setPaymentMethodData(p, paymentMethod, paymentMethodData);

        for (Bill b : getBilledBill().getForwardReferenceBills()) {
            System.err.println("Bill For In");
            System.out.println("dbl = " + dbl);
            System.out.println("reminingCashPaid = " + reminingCashPaid);
            System.out.println("cashPaid = " + cashPaid);
            System.out.println("b.isCancelled() = " + b.isCancelled());
            System.out.println("b.isRefunded() = " + b.isRefunded());
            if (b.isCancelled()) {
                System.err.println("This Bill cancelled = " + b.getDeptId());
                if (getBilledBill().getForwardReferenceBills().size() == 1) {
                    UtilityController.addErrorMessage("Can't Pay,This Bill cancelled");
                } else {
                    UtilityController.addErrorMessage("Some Bill cancelled This Batch Bill");
                }
                continue;
            }

            if ((reminingCashPaid != 0.0) || !getSessionController().getInstitutionPreference().isPartialPaymentOfOpdPreBillsAllowed()) {
                for (BillItem bi : b.getBillItems()) {
                    System.err.println("BillItem For In");
                    System.out.println("bi = " + bi);

                    String sql = "SELECT bi FROM BillItem bi where bi.retired=false and bi.referanceBillItem.id=" + bi.getId();
                    BillItem rbi = getBillItemFacade().findFirstBySQL(sql);

                    if (rbi != null) {
                        System.err.println("rbi = " + rbi.getId());
                        System.out.println("rbi.getBill().getInsId() = " + rbi.getBill().getInsId());
                        System.out.println("rbi.getBill().getDeptId() = " + rbi.getBill().getDeptId());
                        System.err.println("rbi = " + rbi.getId());
                        UtilityController.addErrorMessage("Some Bill Item Already Refunded");
                        continue;
                    }

                    sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();

                    List<BillFee> billFees = getBillFeeFacade().findBySQL(sql);
                    System.out.println("billFees = " + billFees.size());

                    calculateBillfeePayments(billFees, p);
                    System.err.println("BillItem For Out");
                }
            }

            b.setCashPaid(calBillPaidValue(b));
            b.setBalance(b.getNetTotal() - b.getCashPaid());
            getBillFacade().edit(b);
            dbl += b.getNetTotal();
            pid += b.getCashPaid();
            System.err.println("Bill For Out");
        }

        getBilledBill().setCashPaid(pid);
        getBilledBill().setBalance(dbl - pid);
        getBilledBill().setNetTotal(dbl);
        getBillFacade().edit(getBilledBill());
        if (getBilledBill().getCashPaid() >= getBilledBill().getNetTotal()) {
            System.out.println("getOpdPreBillController().getBills().size() = " + getOpdPreBillController().getBills().size());
            getOpdPreBillController().setBills(getBilledBill().getForwardReferenceBills());
            System.out.println("getOpdPreBillController().getBills().size() = " + getOpdPreBillController().getBills().size());
            JsfUtil.addSuccessMessage("Sucessfully Fully Paid");
            return "/bill_print";
        } else {
            JsfUtil.addSuccessMessage("Sucessfully Paid");
            getOpdPreBillController().setBills(getBilledBill().getForwardReferenceBills());
            return "/bill_print_advance";
        }
    }

    public BilledBill createBatchBilledBill(Bill b) {
        BilledBill tmp = new BilledBill();
        tmp.copy(b);
        tmp.copyValue(b);
        tmp.setReferenceBill(b);
        tmp.setBillType(BillType.OpdBathcBill);
        tmp.setBillClassType(BillClassType.BilledBill);
        tmp.setInstitution(getSessionController().getInstitution());
        tmp.setDepartment(getSessionController().getDepartment());
        tmp.setPaymentScheme(b.getPaymentScheme());
        tmp.setPaymentMethod(b.getPaymentMethod());
        tmp.setCreatedAt(new Date());
        tmp.setCreater(getSessionController().getLoggedUser());
        //Institution ID (INS ID)
        String insId = getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), tmp.getBillType(), tmp.getBillClassType(), BillNumberSuffix.NONE);
        tmp.setInsId(insId);

        //Department ID (DEPT ID)
        String deptId = getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getSessionController().getDepartment(), tmp.getBillType(), tmp.getBillClassType());
        tmp.setDeptId(deptId);

        getBillFacade().create(tmp);
        b.setReferenceBill(tmp);
        getBillFacade().edit(b);

        return tmp;
    }

    public void setPaymentMethodData(Payment p, PaymentMethod paymentMethod, PaymentMethodData paymentMethodData) {

        if (paymentMethod.equals(PaymentMethod.Cheque)) {
            p.setBank(paymentMethodData.getCheque().getInstitution());
            p.setChequeRefNo(paymentMethodData.getCheque().getNo());
            p.setChequeDate(paymentMethodData.getCheque().getDate());

        }
        if (paymentMethod.equals(PaymentMethod.Slip)) {
            p.setBank(paymentMethodData.getSlip().getInstitution());
            p.setChequeDate(paymentMethodData.getSlip().getDate());
            p.setComments(paymentMethodData.getSlip().getComment());
        }

        if (paymentMethod.equals(PaymentMethod.Card)) {
            p.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
            p.setBank(paymentMethodData.getCreditCard().getInstitution());
        }

        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(paymentMethod);

        System.out.println("cashPaid = " + cashPaid);
        if (getSessionController().getInstitutionPreference().isPartialPaymentOfOpdPreBillsAllowed()) {
            if (cashPaid < getBilledBill().getBalance()) {
                p.setPaidValue(cashPaid);
            } else {
                p.setPaidValue(getBilledBill().getBalance());
            }

        } else {
            p.setPaidValue(getBilledBill().getNetTotal());
        }

        if (p.getId() == null) {
            getPaymentFacade().create(p);
        }

    }

    public void setPaymentMethodData(Payment p, PaymentMethod pm) {

        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);

        if (p.getBill().getBillType()==BillType.PaymentBill) {
            p.setPaidValue(p.getBill().getNetTotal());
        }else{
            p.setPaidValue(p.getBill().getCashPaid());
        }
        System.out.println("p.getPaidValue() = " + p.getPaidValue());

        if (p.getId() == null) {
            getPaymentFacade().create(p);
        }

    }

    public void setBillFeePaymentAndPayment(double amount, BillFee bf, Payment p) {
        BillFeePayment bfp = new BillFeePayment();
        bfp.setBillFee(bf);
        bfp.setAmount(amount);
        bfp.setInstitution(bf.getBillItem().getItem().getInstitution());
        bfp.setDepartment(bf.getBillItem().getItem().getDepartment());
        bfp.setCreater(getSessionController().getLoggedUser());
        bfp.setCreatedAt(new Date());
        bfp.setPayment(p);
        getBillFeePaymentFacade().create(bfp);
    }

    public void setBillFeePaymentAndPayment(BillFee bf, Payment p) {
        BillFeePayment bfp = new BillFeePayment();
        bfp.setBillFee(bf);
        bfp.setAmount(bf.getSettleValue());
        if (bf.getBillItem()!=null &&bf.getBillItem().getItem()!=null) {
            bfp.setInstitution(bf.getBillItem().getItem().getInstitution());
            bfp.setDepartment(bf.getBillItem().getItem().getDepartment());
        } else {
            bfp.setInstitution(getSessionController().getInstitution());
            bfp.setDepartment(getSessionController().getDepartment());
        }
        bfp.setCreater(getSessionController().getLoggedUser());
        bfp.setCreatedAt(new Date());
        bfp.setPayment(p);
        getBillFeePaymentFacade().create(bfp);
    }

    public double calBillPaidValue(Bill b) {
        String sql;

        sql = "select sum(bfp.amount) from BillFeePayment bfp where "
                + " bfp.retired=false "
                + " and bfp.billFee.bill.id=" + b.getId();

        double d = getBillFeePaymentFacade().findDoubleByJpql(sql);

        return d;
    }

    public void calculateBillfeePayments(List<BillFee> billFees, Payment p) {
        for (BillFee bf : billFees) {
            System.err.println("BillFee For In");
            System.out.println("reminingCashPaid = " + reminingCashPaid);
            System.out.println("cashPaid = " + cashPaid);

            if (getSessionController().getInstitutionPreference().isPartialPaymentOfOpdPreBillsAllowed()) {
                System.err.println("IF In");
                System.out.println("Math.abs((bf.getFeeValue()-bf.getSettleValue())) = " + Math.abs((bf.getFeeValue() - bf.getSettleValue())));
                if (Math.abs((bf.getFeeValue() - bf.getSettleValue())) > 0.1) {
                    if (reminingCashPaid >= (bf.getFeeValue() - bf.getSettleValue())) {
                        System.err.println("in");
                        System.out.println("In If reminingCashPaid = " + reminingCashPaid);
                        System.out.println("bf.getPaidValue() = " + bf.getSettleValue());
                        double d = (bf.getFeeValue() - bf.getSettleValue());
                        System.out.println("d = " + d);
                        bf.setSettleValue(bf.getFeeValue());
                        System.out.println("d = " + d);
                        setBillFeePaymentAndPayment(d, bf, p);
                        getBillFeeFacade().edit(bf);
                        reminingCashPaid -= d;
                        System.out.println("bf.getPaidValue() = " + bf.getSettleValue());
                        System.out.println("Out If reminingCashPaid = " + reminingCashPaid);
                        System.err.println("out");
                    } else {
                        System.err.println("IN");
                        System.out.println("In E reminingCashPaid = " + reminingCashPaid);
                        System.out.println("bf.getPaidValue() = " + bf.getSettleValue());
                        bf.setSettleValue(bf.getSettleValue() + reminingCashPaid);
                        setBillFeePaymentAndPayment(reminingCashPaid, bf, p);
                        getBillFeeFacade().edit(bf);
                        reminingCashPaid = 0.0;
                        System.out.println("bf.getPaidValue() = " + bf.getSettleValue());
                        System.out.println("In O reminingCashPaid = " + reminingCashPaid);
                        System.err.println("OUT");
                    }
                }
                System.err.println("IF Out");
            } else {
                System.err.println("Else In");
                bf.setSettleValue(bf.getFeeValue());
                setBillFeePaymentAndPayment(bf.getFeeValue(), bf, p);
                getBillFeeFacade().edit(bf);
                System.err.println("Else Out");
            }
            System.err.println("BillFee For Out");
        }
    }

    public void calculateBillfeePaymentsForCancelRefundBill(List<BillFee> billFees, Payment p) {
        for (BillFee bf : billFees) {
            System.err.println("BillFee For In");
            System.out.println("bf = " + bf);
            System.out.println("bf.getPaidValue() = " + bf.getSettleValue());
            setBillFeePaymentAndPayment(bf, p);
            System.err.println("BillFee For Out");
        }
    }

    public void createOpdCancelRefundBillFeePayment(Bill bill, List<BillFee> billFees, Payment p) {
        System.out.println("billFees = " + billFees.size());
        calculateBillfeePaymentsForCancelRefundBill(billFees, p);
        System.err.println("BillItem For Out");

        JsfUtil.addSuccessMessage("Sucessfully Paid");
    }

    public Payment createPayment(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        System.out.println("bill.getNetTotal() = " + bill.getNetTotal());
        System.out.println("bill.getCashPaid() = " + bill.getCashPaid());
        setPaymentMethodData(p, pm);
        return p;
    }

    public Bill getPreBill() {
        if (preBill == null) {
            preBill = new PreBill();
        }
        return preBill;
    }

    public void setPreBill(Bill preBill) {
        makeNull();
        this.preBill = preBill;
        //System.err.println("Setting Bill " + preBill);
        billPreview = false;

    }

    public Bill getSaleBill() {
        if (saleBill == null) {
            saleBill = new BilledBill();
            //  saleBill.setBillType(BillType.PharmacySale);
        }
        return saleBill;
    }

    public void setSaleBill(Bill saleBill) {
        this.saleBill = saleBill;
    }

    public String getPatientTabId() {
        return patientTabId;
    }

    public void setPatientTabId(String patientTabId) {
        this.patientTabId = patientTabId;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public String getStrTenderedValue() {
        return strTenderedValue;
    }

    public void setStrTenderedValue(String strTenderedValue) {
        this.strTenderedValue = strTenderedValue;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public double getCashPaid() {
        return cashPaid;
    }

    public void setCashPaid(double cashPaid) {
        balance = cashPaid - netTotal;
        this.cashPaid = cashPaid;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        balance = cashPaid - netTotal;
        this.netTotal = netTotal;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean isBillPreview() {
        return billPreview;
    }

    public void setBillPreview(boolean billPreview) {
        this.billPreview = billPreview;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {

        this.bill = bill;
    }

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    public BillFeePaymentFacade getBillFeePaymentFacade() {
        return billFeePaymentFacade;
    }

    public void setBillFeePaymentFacade(BillFeePaymentFacade billFeePaymentFacade) {
        this.billFeePaymentFacade = billFeePaymentFacade;
    }

    public Bill getBilledBill() {
        if (billedBill == null) {
            billedBill = new BilledBill();
        }
        return billedBill;
    }

    public void setBilledBill(Bill billedBill) {
        this.billedBill = billedBill;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public OpdPreBillController getOpdPreBillController() {
        return opdPreBillController;
    }

    public void setOpdPreBillController(OpdPreBillController opdPreBillController) {
        this.opdPreBillController = opdPreBillController;
    }

}
