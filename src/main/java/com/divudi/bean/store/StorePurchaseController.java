/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.store;

import com.divudi.bean.common.ApplicationController;
import com.divudi.bean.common.BillItemController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;

import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;

import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Item;
import com.divudi.entity.WebUser;
import com.divudi.entity.pharmacy.ItemBatch;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.AmpFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.StockFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillFeePayment;
import com.divudi.entity.Payment;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillFeePaymentFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class StorePurchaseController implements Serializable {

    @Inject
    private SessionController sessionController;
    private BilledBill bill;
    @EJB
    private BillFacade billFacade;
    @EJB
    StoreBean storeBean;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private AmpFacade ampFacade;
    @EJB
    StockFacade stockFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    private PaymentFacade paymentFacade;

    @Inject
    StoreCalculation storeCalculation;
    @Inject
    ApplicationController applicationController;
    @Inject
    BillItemController billItemController;
    @Inject
    CommonController commonController;
    

    public BillItemController getBillItemController() {
        return billItemController;
    }

    public void setBillItemController(BillItemController billItemController) {
        this.billItemController = billItemController;
    }

    ////////////
    private BillItem currentBillItem;
    BillItem currentExpense;
    //private PharmacyItemData currentPharmacyItemData;
    private boolean printPreview;
    ///////////
    //  private List<PharmacyItemData> pharmacyItemDatas;
    List<BillItem> billExpenses;
    BillItem parentBillItem;


    private CommonFunctions commonFunctions;
    @EJB
    private BillNumberGenerator billNumberGenerator;

    Date frmDate;
    Date toDate;
    double total;

    public BillItem getParentBillItem() {
        return parentBillItem;
    }

    public void setParentBillItem(BillItem parentBillItem) {
        this.parentBillItem = parentBillItem;
    }

    public void createPurchaseExpencess() {

        String sql;
        HashMap m = new HashMap();

        sql = "select bi from BillItem bi where "
                + " bi.expenseBill.createdAt between :fd and :td "
                + " and bi.expenseBill.retired=false and "
                + " (bi.expenseBill.billType=:bt1 or bi.expenseBill.billType=:bt2)";

        if (currentExpense.getItem() != null) {
            sql += " and bi.item=:item ";
            m.put("item", currentExpense.getItem());
        }

        m.put("fd", frmDate);
        m.put("td", toDate);
        m.put("bt1", BillType.StoreGrnBill);
        m.put("bt2", BillType.StorePurchase);

        billExpenses = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        total = 0.0;
        for (BillItem bi : billExpenses) {
            total += bi.getNetValue();
        }

    }

    public void makeNull() {
        //  currentPharmacyItemData = null;
        printPreview = false;
        currentBillItem = null;
        bill = null;
        billItems = null;
    }

    public PaymentMethod[] getPaymentMethods() {
        return PaymentMethod.values();

    }

    public void remove(BillItem b) {
        getBillItems().remove(b);
        calTotal();
        currentBillItem = null;
    }

    public StorePurchaseController() {
    }

    public void onEditPurchaseRate(BillItem tmp) {

        double retail = tmp.getPharmaceuticalBillItem().getPurchaseRate() + (tmp.getPharmaceuticalBillItem().getPurchaseRate() * (getStoreBean().getMaximumRetailPriceChange() / 100));
        tmp.getPharmaceuticalBillItem().setRetailRate((double) retail);

        onEdit(tmp);
    }

    public void onEditPurchaseRate() {

        double retail = getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate() + (getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate() * (getStoreBean().getMaximumRetailPriceChange() / 100));
        getCurrentBillItem().getPharmaceuticalBillItem().setRetailRate((double) retail);

        onEdit(getCurrentBillItem());
    }

    public void onEdit(BillItem tmp) {

        if (tmp.getPharmaceuticalBillItem().getPurchaseRate() > tmp.getPharmaceuticalBillItem().getRetailRate()) {
            tmp.getPharmaceuticalBillItem().setRetailRate(0);
            JsfUtil.addErrorMessage("You cant set retail price below purchase rate");
        }

//        if (tmp.getPharmaceuticalBillItem().getDoe() != null) {
//            if (tmp.getPharmaceuticalBillItem().getDoe().getTime() < Calendar.getInstance().getTimeInMillis()) {
//                tmp.getPharmaceuticalBillItem().setDoe(null);
//                JsfUtil.addErrorMessage("Check Date of Expiry");
//                //    return;
//            }
//        }
        calTotal();
    }

    public void setBatch(BillItem pid) {
        Date date = pid.getPharmaceuticalBillItem().getDoe();
        DateFormat df = new SimpleDateFormat("ddMMyyyy");
        String reportDate = df.format(date);
// Print what date is today!
        //       //System.err.println("Report Date: " + reportDate);
        pid.getPharmaceuticalBillItem().setStringValue(reportDate);

        onEdit(pid);
    }

    public void setBatch() {
        if (getCurrentBillItem().getPharmaceuticalBillItem().getDoe() == null) {
            getCurrentBillItem().getPharmaceuticalBillItem().setDoe(getApplicationController().getStoresExpiery());
        }
        Date date = getCurrentBillItem().getPharmaceuticalBillItem().getDoe();
        DateFormat df = new SimpleDateFormat("ddMMyyyy");
        String reportDate = df.format(date);
// Print what date is today!
        //       //System.err.println("Report Date: " + reportDate);
        getCurrentBillItem().getPharmaceuticalBillItem().setStringValue(reportDate);

        //     onEdit(pid);
    }

    public String errorCheck() {
        String msg = "";

        if (getBill().getFromInstitution() == null) {
            msg = "Please select Dealor";
            return msg;
        }

        if (getBillItems().isEmpty()) {
            msg = "Empty Items";
            return msg;
        }
        if (getBill().getReferenceInstitution() == null) {
            msg = "Select Reference Institution";
            return msg;
        }
        if (getBill().getInvoiceNumber() == null || "".equals(getBill().getInvoiceNumber().trim())) {
            msg = "Please Fill Invoice Number";
            return msg;
        }
        if (getBill().getInvoiceDate() == null) {
            msg = "Please Fill Invoice Date";
            return msg;
        }

        return msg;
    }

    @EJB
    CashTransactionBean cashTransactionBean;

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void saveParentBillItem(BillItem billItem) {

        if (billItem == null) {
            return;
        }

        if (billItem.getParentBillItem() != null) {
            saveParentBillItem(billItem.getParentBillItem());
        }

        if (billItem.getId() == null) {
            billItemFacade.create(billItem);
        }

    }

    public void settle() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (getBill().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Select Dealor");
            return;
        }

        //Need to Add History
        String msg = errorCheck();
        if (!msg.isEmpty()) {
            JsfUtil.addErrorMessage(msg);
            return;
        }

        saveBill();
        //   saveBillComponent();
        storeCalculation.calSaleFreeValue(getBill());

        //Restting IDs
        Payment p = createPayment(getBill());
        for (BillItem i : getBillItems()) {
            i.setId(null);
        }
        
        for (BillItem i : getBillItems()) {
            if (i.getPharmaceuticalBillItem().getQty() == 0.0) {
                continue;
            }

            PharmaceuticalBillItem tmpPh = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getBill());

            saveParentBillItem(i.getParentBillItem());

            if (i.getId() == null) {
                getBillItemFacade().create(i);
            } else {
                getBillItemFacade().edit(i);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            } else {
                getPharmaceuticalBillItemFacade().edit(tmpPh);
            }

            i.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(i);
            saveBillFee(i, p);

            ItemBatch itemBatch = storeCalculation.saveItemBatch(i);
            double addingQty = tmpPh.getQtyInUnit() + tmpPh.getFreeQtyInUnit();

            tmpPh.setItemBatch(itemBatch);
            Stock stock = getStoreBean().addToStock(tmpPh, Math.abs(addingQty), getSessionController().getDepartment());

            tmpPh.setStock(stock);
            getPharmaceuticalBillItemFacade().edit(tmpPh);

            getBill().getBillItems().add(i);
        }

        for (BillItem i : getBillItems()) {

            if (i.getParentBillItem() != null) {
                i.getParentBillItem().getPharmaceuticalBillItem().getStock().getChildStocks().add(i.getPharmaceuticalBillItem().getStock());
                i.getPharmaceuticalBillItem().getStock().setParentStock(i.getParentBillItem().getPharmaceuticalBillItem().getStock());
                getStockFacade().edit(i.getParentBillItem().getPharmaceuticalBillItem().getStock());
                getStockFacade().edit(i.getPharmaceuticalBillItem().getStock());
            }
        }

        for (BillItem i : getBillExpenses()) {
            i.setExpenseBill(getBill());
            getBillItemFacade().create(i);
            getBill().getBillExpenses().add(i);
        }

        getBillFacade().edit(getBill());

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(getBill(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);

        JsfUtil.addSuccessMessage("Successfully Billed");
        printPreview = true;

        
        
    }
    
    public Payment createPayment(Bill bill) {
        Payment p = new Payment();
        p.setBill(bill);
        setPaymentMethodData(p, bill.getPaymentMethod());
        return p;
    }

    public void setPaymentMethodData(Payment p, PaymentMethod pm) {

        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);

        p.setPaidValue(p.getBill().getNetTotal());

        if (p.getId() == null) {
            getPaymentFacade().create(p);
        }

    }

    public void saveBillFee(BillItem bi, Payment p) {
        BillFee bf = new BillFee();
        bf.setCreatedAt(Calendar.getInstance().getTime());
        bf.setCreater(getSessionController().getLoggedUser());
        bf.setBillItem(bi);
        bf.setPatienEncounter(bi.getBill().getPatientEncounter());
        bf.setPatient(bi.getBill().getPatient());
        bf.setFeeValue(bi.getNetValue());
        bf.setFeeGrossValue(bi.getGrossValue());
        bf.setSettleValue(bi.getNetValue());
        bf.setCreatedAt(new Date());
        bf.setDepartment(getSessionController().getDepartment());
        bf.setInstitution(getSessionController().getInstitution());
        bf.setBill(bi.getBill());

        if (bf.getId() == null) {
            getBillFeeFacade().create(bf);
        }
        createBillFeePaymentAndPayment(bf, p);
    }

    public void createBillFeePaymentAndPayment(BillFee bf, Payment p) {
        BillFeePayment bfp = new BillFeePayment();
        bfp.setBillFee(bf);
        bfp.setAmount(bf.getSettleValue());
        bfp.setInstitution(getSessionController().getInstitution());
        bfp.setDepartment(getSessionController().getDepartment());
        bfp.setCreater(getSessionController().getLoggedUser());
        bfp.setCreatedAt(new Date());
        bfp.setPayment(p);
        getBillFeePaymentFacade().create(bfp);
    }

    private List<BillItem> billItems;

    public void createInventoryItemCode() {
        if (getCurrentBillItem() == null) {
            return;
        }
        if (getCurrentBillItem().getPharmaceuticalBillItem() == null) {
            return;
        }
        if (getCurrentBillItem().getItem() == null) {
            return;
        }

    }

    public void createSerialNumber(BillItem billItem) {
        ////System.out.println("In");
        long b = billNumberGenerator.inventoryItemSerialNumberGenerater(getSessionController().getLoggedUser().getInstitution(), getCurrentBillItem().getItem());
        b = b + 1;
        for (BillItem bi : getBillItems()) {
            if (bi.getItem().equals(billItem.getItem())) {
                b++;
            }
        }
        ////System.out.println("b = " + b);
        String code = "";
        code += getSessionController().getInstitution().getCode();
        code += "/";
        code += getSessionController().getDepartment().getCode();
        code += "/";
        if (billItem != null && billItem.getItem() != null && billItem.getItem().getCategory() != null) {
            code += billItem.getItem().getCategory().getCode();
            code += "/";
        }
        if (billItem != null && billItem.getItem() != null) {
            code += billItem.getItem().getCode();
            code += "/";
        }
        code += b;
        ////System.out.println("code = " + code);
        billItem.getPharmaceuticalBillItem().setCode(code);
    }

    public void createSerialNumber() {
        createSerialNumber(getCurrentBillItem());
    }

    public void addBillItem() {
        if (getCurrentBillItem().getItem().getCategory() == null) {
            JsfUtil.addErrorMessage("Please Select Category");
            return;
        }

        if (getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate() <= 0 && getParentBillItem() == null) {
            JsfUtil.addErrorMessage("Please enter Purchase Rate");
            return;
        }

        if (getCurrentBillItem().getPharmaceuticalBillItem().getRetailRate() == 0) {
            JsfUtil.addErrorMessage("Please enter Retail Rate");
            return;
        }

        if (getCurrentBillItem().getPharmaceuticalBillItem().getRetailRate() < getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate()) {
            JsfUtil.addErrorMessage("Please check Retail Rate");
            return;
        }

        if (getCurrentBillItem().getPharmaceuticalBillItem().getQty() <= 0) {
            JsfUtil.addErrorMessage("Please enter Purchase QTY");
            return;
        }

        if (getCurrentBillItem().getItem().getDepartmentType() == DepartmentType.Inventry) {
            if (getCurrentBillItem().getPharmaceuticalBillItem().getQty() != 1) {
                JsfUtil.addErrorMessage("Please Qty must be 1 for Asset");
                return;
            }
        }

//        if (billItem.getPharmaceuticalBillItem().getPurchaseRate() > billItem.getPharmaceuticalBillItem().getRetailRate()) {
//            JsfUtil.addErrorMessage("Please enter Sale Rate Should be Over Purchase Rate");
//            return;
//        }
        if (getCurrentBillItem().getPharmaceuticalBillItem().getRetailRate() <= 0) {
            getCurrentBillItem().getPharmaceuticalBillItem().setRetailRate(getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate() * (1 + (.01 * getCurrentBillItem().getItem().getCategory().getSaleMargin())));
        }

        if (getCurrentBillItem().getPharmaceuticalBillItem().getDoe() == null) {
            getCurrentBillItem().getPharmaceuticalBillItem().setDoe(getApplicationController().getStoresExpiery());
        }

        getCurrentBillItem().setParentBillItem(getParentBillItem());

        getCurrentBillItem().setSearialNo(getBillItems().size() + 1);
        //getCurrentBillItem().setId(getCurrentBillItem().getSearialNoInteger().longValue());

//        billItem.setSearialNo(getBillItems().size() + 1);        
        getBillItems().add(getCurrentBillItem());

        getBillItemController().setItems(getBillItems());
    }

    public void addItem() {
        if (getBill().getId() == null) {
            getBillFacade().create(getBill());
        }

        addBillItem();
        currentBillItem = null;
        calTotal();

    }
    
    public void addItemWithLastRate() {
        if (getCurrentBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("Please select and item from the list");
            return;
        }

        getCurrentBillItem().setSearialNo(getBillItems().size());
        getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRateInUnit(getStoreBean().getLastPurchaseRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));
        getCurrentBillItem().getPharmaceuticalBillItem().setRetailRateInUnit(getStoreBean().getLastRetailRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));

        getBillItems().add(getCurrentBillItem());

        calTotal();

        currentBillItem = null;
    }

    public void purchaseRateListener(PharmaceuticalBillItem pharmaceuticalBillItem) {
        pharmaceuticalBillItem.setRetailRate(pharmaceuticalBillItem.getPurchaseRate());
    }

    public void itemListner(BillItem bi) {
        getCurrentBillItem().setParentBillItem(bi);
    }

    public void addExpense() {
        if (getBill().getId() == null) {
            getBillFacade().create(getBill());
        }
        if (getCurrentExpense().getItem() == null) {
            JsfUtil.addErrorMessage("Expense ?");
            return;
        }
        if (currentExpense.getQty() == null || currentExpense.getQty().equals(0.0)) {
            currentExpense.setQty(1.0);
        }
        if (currentExpense.getNetRate() == 0.0) {
            currentExpense.setNetRate(currentExpense.getRate());
        }

        currentExpense.setNetValue(currentExpense.getNetRate() * currentExpense.getQty());
        currentExpense.setGrossValue(currentExpense.getRate() * currentExpense.getQty());

        getCurrentExpense().setSearialNo(getBillExpenses().size());
        getBillExpenses().add(currentExpense);
        currentExpense = null;
        calTotal();
    }

    public void saveBill() {

        getBill().setDeptId(billNumberGenerator.institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.StorePurchase, BillClassType.BilledBill, BillNumberSuffix.PHPUR));
        getBill().setInsId(billNumberGenerator.institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.StorePurchase, BillClassType.BilledBill, BillNumberSuffix.PHPUR));

        getBill().setInstitution(getSessionController().getInstitution());
        getBill().setDepartment(getSessionController().getDepartment());
        getBill().setBillType(BillType.StorePurchase);
        getBill().setCreatedAt(new Date());
        getBill().setCreater(getSessionController().getLoggedUser());

        getBillFacade().edit(getBill());

    }

    public BillItem getBillItem(Item i) {
        BillItem tmp = new BillItem();
        tmp.setBill(getBill());
        tmp.setItem(i);

        //   getBillItemFacade().create(tmp);
        return tmp;
    }

    public PharmaceuticalBillItem getPharmacyBillItem(BillItem b) {
        PharmaceuticalBillItem tmp = new PharmaceuticalBillItem();
        tmp.setBillItem(b);
        //   tmp.setQty(getStoreBean().getPurchaseRate(b.getItem(), getSessionController().getDepartment()));
        //     tmp.setPurchaseRate(getStoreBean().getPurchaseRate(b.getItem(), getSessionController().getDepartment()));
        tmp.setRetailRate(storeCalculation.calRetailRate(tmp));
//        if (b.getId() == null || b.getId() == 0) {
//            getPharmaceuticalBillItemFacade().create(tmp);
//        } else {
//            getPharmaceuticalBillItemFacade().edit(tmp);
//        }
        return tmp;
    }

    public double getNetTotal() {

        double tmp = getBill().getTotal() + getBill().getTax() - getBill().getDiscount();
        getBill().setNetTotal(0 - tmp);

        return tmp;
    }

    public void calTotal() {
        double tot = 0.0;
        double exp = 0.0;
        int serialNo = 0;
        for (BillItem p : getBillItems()) {
            p.setQty((double) p.getPharmaceuticalBillItem().getQtyInUnit());
            p.setRate(p.getPharmaceuticalBillItem().getPurchaseRateInUnit());
            serialNo++;
            p.setSearialNo(serialNo);
            if (p.getParentBillItem() == null) {
                double netValue = p.getQty() * p.getRate();
                p.setNetValue(0 - netValue);
                tot += p.getNetValue();
            }
        }

        for (BillItem e : getBillExpenses()) {
            double nv = e.getNetRate() * e.getQty();
            e.setNetValue(0 - nv);
            exp += e.getNetValue();
        }

        getBill().setExpenseTotal(exp);
        getBill().setTotal(tot);
        getBill().setNetTotal(tot + exp);

    }
    
    public void calNetTotal() {
        double grossTotal = 0.0;
        if (getBill().getDiscount() > 0 || getBill().getTax()>0) {
            grossTotal = getBill().getTotal() + getBill().getDiscount() - getBill().getTax();
            getBill().setNetTotal(grossTotal);
        }

    }

    public BilledBill getBill() {
        if (bill == null) {
            bill = new BilledBill();
            bill.setBillType(BillType.StorePurchase);
            bill.setReferenceInstitution(getSessionController().getInstitution());
        }
        return bill;
    }

    public void setBill(BilledBill bill) {
        this.bill = bill;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public StoreBean getStoreBean() {
        return storeBean;
    }

    public void setStoreBean(StoreBean storeBean) {
        this.storeBean = storeBean;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public AmpFacade getAmpFacade() {
        return ampFacade;
    }

    public void setAmpFacade(AmpFacade ampFacade) {
        this.ampFacade = ampFacade;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public BillItem getCurrentExpense() {
        if (currentExpense == null) {
            currentExpense = new BillItem();
            currentExpense.setQty(1.0);
        }
        return currentExpense;
    }

    public void setCurrentExpense(BillItem currentExpense) {
        this.currentExpense = currentExpense;
    }

    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
            PharmaceuticalBillItem cuPharmaceuticalBillItem = new PharmaceuticalBillItem();
            currentBillItem.setPharmaceuticalBillItem(cuPharmaceuticalBillItem);
            cuPharmaceuticalBillItem.setBillItem(currentBillItem);
        }
        return currentBillItem;
    }

    public void setCurrentBillItem(BillItem currentBillItem) {
        this.currentBillItem = currentBillItem;
    }

    public List<BillItem> getBillExpenses() {
        if (billExpenses == null) {
            billExpenses = new ArrayList<>();
        }
        return billExpenses;
    }

    public void setBillExpenses(List<BillItem> billExpenses) {
        this.billExpenses = billExpenses;
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

    public ApplicationController getApplicationController() {
        return applicationController;
    }

    public void setApplicationController(ApplicationController applicationController) {
        this.applicationController = applicationController;
    }

    public Date getFrmDate() {
        if (frmDate == null) {
            frmDate = com.divudi.java.CommonFunctions.getStartOfMonth(new Date());
        }
        return frmDate;
    }

    public void setFrmDate(Date frmDate) {
        this.frmDate = frmDate;
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

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public BillNumberGenerator getBillNumberGenerator() {
        return billNumberGenerator;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public StoreCalculation getStoreCalculation() {
        return storeCalculation;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BillFeePaymentFacade getBillFeePaymentFacade() {
        return billFeePaymentFacade;
    }

    public void setBillFeePaymentFacade(BillFeePaymentFacade billFeePaymentFacade) {
        this.billFeePaymentFacade = billFeePaymentFacade;
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }
 
}
