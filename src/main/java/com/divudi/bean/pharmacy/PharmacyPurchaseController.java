/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.BillListWithTotals;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.data.dataStructure.PharmacyStockRow;
import com.divudi.ejb.BillEjb;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillFeePayment;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillNumber;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Payment;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Ampp;
import com.divudi.entity.pharmacy.ItemBatch;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.AmpFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillFeePaymentFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.java.CommonFunctions;
import com.divudi.service.PaymentService;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.event.SelectEvent;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class PharmacyPurchaseController implements Serializable {

    /**
     * EJBs
     */
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private AmpFacade ampFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    BillEjb billEjb;
    @EJB
    PaymentService paymentService;

    /**
     * Controllers
     */
    @Inject
    private SessionController sessionController;
    @Inject
    PharmacyCalculation pharmacyBillBean;
    @Inject
    CommonController commonController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    /**
     * Properties
     */
    private BilledBill bill;
    private BillItem currentBillItem;
    private boolean printPreview;

    private String warningMessage;

    double saleRate;
    double wsRate;
    AmpController ampController;

    Institution institution;
    Department department;
    Date fromDate;
    Date toDate;
    List<PharmacyStockRow> rows;

    BillListWithTotals billListWithTotals;
    private double billItemsTotalQty;

    private PaymentMethodData paymentMethodData;
    private Institution site;
    private Institution toInstitution;
    private PaymentMethod paymentMethod;

    private BillItem currentExpense;
    private List<BillItem> billExpenses;

    public List<BillItem> getBillExpenses() {
        if (billExpenses == null) {
            billExpenses = new ArrayList<>();
        }
        return billExpenses;
    }

    public void setBillExpenses(List<BillItem> billExpenses) {
        this.billExpenses = billExpenses;
    }

    public BillItem getCurrentExpense() {
        if (currentExpense == null) {
            currentExpense = new BillItem();
        }
        return currentExpense;
    }

    public void setCurrentExpense(BillItem currentExpense) {
        this.currentExpense = currentExpense;
    }

    public void onItemSelect(SelectEvent event) {
//        Item selectedItem = (Item) event.getObject();
//        System.out.println("selectedItem = " + selectedItem);
//        System.out.println("getCurrentBillItem().getItem() = " + getCurrentBillItem().getItem());
        getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRate(getPharmacyBean().getLastPurchaseRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));
        getCurrentBillItem().getPharmaceuticalBillItem().setRetailRate(getPharmacyBean().getLastRetailRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));
    }

    public void createGrnAndPurchaseBillsWithCancellsAndReturnsOfSingleDepartment() {
        Date startTime = new Date();

        BillType[] bts = new BillType[]{BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill, BillType.PharmacyGrnReturn, BillType.PurchaseReturn,};
        Class[] bcs = new Class[]{BilledBill.class, CancelledBill.class, RefundBill.class};
        billListWithTotals = billEjb.findBillsAndTotals(fromDate, toDate, bts, bcs, department, null, null);

    }

    public void createOnlyPurchaseBillsWithCancellsAndReturnsOfSingleDepartment() {
        Date startTime = new Date();

        BillType[] bts = new BillType[]{BillType.PharmacyPurchaseBill, BillType.PurchaseReturn,};
        Class[] bcs = new Class[]{BilledBill.class, CancelledBill.class, RefundBill.class};
        billListWithTotals = billEjb.findBillsAndTotals(fromDate, toDate, bts, bcs, department, null, null);

    }

    public void createOnlyGrnBillsWithCancellsAndReturnsOfSingleDepartment() {
        Date startTime = new Date();

        BillType[] bts = new BillType[]{BillType.PharmacyGrnBill, BillType.PurchaseReturn,};
        Class[] bcs = new Class[]{BilledBill.class, CancelledBill.class, RefundBill.class};
        billListWithTotals = billEjb.findBillsAndTotals(fromDate, toDate, bts, bcs, department, null, null);

    }

    public void fillItemVicePurchaseAndGoodReceive() {
        Date startTime = new Date();

        Map m = new HashMap();
        String sql;
        BillItem bi = new BillItem();
        List<BillType> bts = new ArrayList<>();

        bts.add(BillType.PharmacyGrnBill);
        bts.add(BillType.PharmacyGrnReturn);
        bts.add(BillType.PharmacyPurchaseBill);

        sql = "select new com.divudi.data.dataStructure.PharmacyStockRow"
                + " (bi.item.name, "
                + " sum(bi.qty), "
                + " sum(bi.pharmaceuticalBillItem.freeQty)) "
                + " from BillItem bi "
                + " where bi.bill.billType in :bts "
                + " and bi.bill.createdAt between :fd and :td ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bts", bts);

        if (department != null) {
            sql = sql + " and bi.bill.department=:dept ";
            m.put("dept", department);
        }

        if (institution != null) {
            sql = sql + " and bi.bill.institution=:ins ";
            m.put("ins", institution);
        }

        sql = sql + "group by bi.item "
                + "order by bi.item.name";

        List<PharmacyStockRow> lsts = (List) billFacade.findObjects(sql, m, TemporalType.TIMESTAMP);

        rows = lsts;

    }

    public void calculatePurchaseRateAndWholesaleRateFromRetailRate() {
        if (currentBillItem == null || currentBillItem.getPharmaceuticalBillItem() == null || currentBillItem.getPharmaceuticalBillItem().getRetailRate() == 0) {
            return;
        }
        currentBillItem.getPharmaceuticalBillItem().setPurchaseRate(currentBillItem.getPharmaceuticalBillItem().getRetailRate() / 1.15);
        currentBillItem.getPharmaceuticalBillItem().setWholesaleRate(currentBillItem.getPharmaceuticalBillItem().getPurchaseRate() * 1.08);
    }

    public List<PharmacyStockRow> getRows() {
        return rows;
    }

    public void setRows(List<PharmacyStockRow> rows) {
        this.rows = rows;
    }

    public Institution getInstitution() {
        if (institution == null) {
            institution = getSessionController().getInstitution();
        }
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public void makeNull() {
        //  currentPharmacyItemData = null;
        printPreview = false;
        currentBillItem = null;
        bill = null;
        billItems = null;
        billExpenses = null;
        currentExpense = null;
        warningMessage = null;
    }

    public String navigateToAddNewPharmacyWholesaleDirectPurchaseBill() {
        makeNull();
        getBill();
        return "/pharmacy_wholesale/pharmacy_purchase?faces-redirect=true";
    }

    public PaymentMethod[] getPaymentMethods() {
        return PaymentMethod.values();

    }

    public void remove(BillItem b) {
        getBillItems().remove(b.getSearialNo());
    }

    public PharmacyCalculation getPharmacyBillBean() {
        return pharmacyBillBean;
    }

    public void setPharmacyBillBean(PharmacyCalculation pharmacyBillBean) {
        this.pharmacyBillBean = pharmacyBillBean;
    }

    public PharmacyPurchaseController() {
    }

    public void onEditPurchaseRate(BillItem tmp) {

        double retail = tmp.getPharmaceuticalBillItem().getPurchaseRate() + (tmp.getPharmaceuticalBillItem().getPurchaseRate() * (getPharmacyBean().getMaximumRetailPriceChange() / 100));
        tmp.getPharmaceuticalBillItem().setRetailRate((double) retail);

        onEdit(tmp);
    }

    public void onEditPurchaseRate() {

        double retail = getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate() + (getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate() * (getPharmacyBean().getMaximumRetailPriceChange() / 100));
        getCurrentBillItem().getPharmaceuticalBillItem().setRetailRate((double) retail);

        onEdit(getCurrentBillItem());
    }

    public void onEdit(BillItem tmp) {

        if (tmp.getPharmaceuticalBillItem().getPurchaseRate() > tmp.getPharmaceuticalBillItem().getRetailRate()) {
            tmp.getPharmaceuticalBillItem().setRetailRate(0);
            JsfUtil.addErrorMessage("You cant set retail price below purchase rate");
        }

        if (tmp.getPharmaceuticalBillItem().getDoe() != null) {
            if (tmp.getPharmaceuticalBillItem().getDoe().getTime() < Calendar.getInstance().getTimeInMillis()) {
                tmp.getPharmaceuticalBillItem().setDoe(null);
                JsfUtil.addErrorMessage("Check Date of Expiry");
                //    return;
            }
        }

        wsRate = (tmp.getPharmaceuticalBillItem().getPurchaseRate() * 1.08) * (tmp.getTmpQty()) / (tmp.getTmpQty() + tmp.getPharmaceuticalBillItem().getFreeQty());
        wsRate = CommonFunctions.round(wsRate);
        tmp.getPharmaceuticalBillItem().setWholesaleRate(wsRate);
        calTotal();
    }

    public void setBatch(BillItem pid) {
        if (pid.getPharmaceuticalBillItem().getStringValue().trim().equals("")) {
            Date date = pid.getPharmaceuticalBillItem().getDoe();
            DateFormat df = new SimpleDateFormat("ddMMyyyy");
            String reportDate = df.format(date);
// Print what date is today!
            //       //System.err.println("Report Date: " + reportDate);
            pid.getPharmaceuticalBillItem().setStringValue(reportDate);
        }
        onEdit(pid);
    }

    public void setBatch() {
        if (getCurrentBillItem() != null) {
            PharmaceuticalBillItem pharmaceuticalBillItem = getCurrentBillItem().getPharmaceuticalBillItem();

            if (pharmaceuticalBillItem != null) {
                String stringValue = pharmaceuticalBillItem.getStringValue();

                if (stringValue != null && stringValue.trim().equals("")) {
                    Date date = pharmaceuticalBillItem.getDoe();

                    if (date != null) {
                        DateFormat df = new SimpleDateFormat("ddMMyyyy");
                        String reportDate = df.format(date);
                        pharmaceuticalBillItem.setStringValue(reportDate);
                    }
                }
            }
        }
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

    public void calSaleRte() {
        saleRate = 0.0;
        double categoryMarginPercentage = 0;
        if (getCurrentBillItem() == null || getCurrentBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("Bill Item is Null");
        } else {

            Object item = getCurrentBillItem().getItem();

            if (item instanceof Ampp) {
                Ampp tmpAmpp = (Ampp) item;
                if (tmpAmpp.getAmp() != null
                        && tmpAmpp.getCategory() != null
                        && tmpAmpp.getCategory().getSaleMargin() != null) {
                    categoryMarginPercentage = tmpAmpp.getCategory().getSaleMargin() + 100;
                }
            } else if (item instanceof Amp) {
                Amp tmpAmp = (Amp) item;
                if (tmpAmp.getCategory() != null
                        && tmpAmp.getCategory().getSaleMargin() != null) {
                    categoryMarginPercentage = tmpAmp.getCategory().getSaleMargin() + 100;
                }
            }
        }

        double tmpPurchaseRate = 0.0;
        if (getCurrentBillItem().getItem() instanceof Ampp) {
            saleRate = (categoryMarginPercentage * getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRatePack() / getCurrentBillItem().getItem().getDblValue()) / 100;
            tmpPurchaseRate = getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRatePack() / getCurrentBillItem().getItem().getDblValue();
            getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRate(tmpPurchaseRate);
        } else if (getCurrentBillItem().getItem() instanceof Amp) {
            saleRate = (categoryMarginPercentage * getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate()) / 100;
        }

        getCurrentBillItem().getPharmaceuticalBillItem().setRetailRate(saleRate);

        categoryMarginPercentage = 108;
        wsRate = (categoryMarginPercentage * getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate()) / 100;
        if (getCurrentBillItem().getTmpQty() + getCurrentBillItem().getPharmaceuticalBillItem().getFreeQty() != 0) {
            wsRate = wsRate * getCurrentBillItem().getTmpQty() / (getCurrentBillItem().getTmpQty() + getCurrentBillItem().getPharmaceuticalBillItem().getFreeQty());
        }
        wsRate = CommonFunctions.round(wsRate);
        getCurrentBillItem().getPharmaceuticalBillItem().setWholesaleRate(wsRate);

    }

    public void calNetTotal() {
        double grossTotal = 0.0;
        if (getBill().getDiscount() > 0 || getBill().getTax() > 0) {
            grossTotal = getBill().getTotal() + getBill().getDiscount() - getBill().getTax();
            ////// // System.out.println("gross" + grossTotal);
            ////// // System.out.println("net1" + getBill().getNetTotal());
            getBill().setNetTotal(grossTotal);
            ////// // System.out.println("net2" + getBill().getNetTotal());
        }

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

    }

    public void settle() {

        if (getBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select Payment Method");
            return;
        }
        if (getBill().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Select Dealer");
            return;
        }
        if (getBill().getReferenceInstitution() == null) {
            JsfUtil.addErrorMessage("Select Reference Institution");
        }
        if (getBill().getInvoiceNumber() == null || "".equals(getBill().getInvoiceNumber().trim())) {
            boolean autogenerateInvoiceNumber = configOptionApplicationController.getBooleanValueByKey("Autogenerate Invoice Number for Pharmacy Direct Purchase", false);
            if (autogenerateInvoiceNumber) {
                BillNumber bn = billNumberBean.fetchLastBillNumberForYear(sessionController.getInstitution(), sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
                String invoiceNumber = configOptionApplicationController.getShortTextValueByKey("Invoice Number Prefix for Pharmacy Direct Purchase", "") + bn.getLastBillNumber();
                getBill().setInvoiceNumber(invoiceNumber);
            } else {
                JsfUtil.addErrorMessage("Please Enter Invoice Number");
                return;
            }
        }
        if (getBill().getInvoiceDate() == null) {
            boolean useCurrentDataIfInvoiceDataIsNotProvided = configOptionApplicationController.getBooleanValueByKey("If Invoice Number is not provided for Pharmacy Direct Purchase, use the current date", false);
            if (useCurrentDataIfInvoiceDataIsNotProvided) {
                getBill().setInvoiceDate(new Date());
            } else {
                JsfUtil.addErrorMessage("Please Fill Invoice Date");
                return;
            }
        }
        if (getBill().getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            JsfUtil.addErrorMessage("MultiplePayments Not Allowed.");
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

//        Payment p = createPayment(getBill());
        List<Payment> ps = paymentService.createPayment(getBill(), getPaymentMethodData());

        billItemsTotalQty = 0;
        for (BillItem i : getBillItems()) {
            if (i.getPharmaceuticalBillItem().getQty() + i.getPharmaceuticalBillItem().getFreeQty() == 0.0) {
                continue;
            }
            billItemsTotalQty = billItemsTotalQty + i.getPharmaceuticalBillItem().getQty() + i.getPharmaceuticalBillItem().getFreeQty();
            PharmaceuticalBillItem tmpPh = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getBill());
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
            saveBillFee(i);
            ItemBatch itemBatch = getPharmacyBillBean().saveItemBatch(i);
            double addingQty = tmpPh.getQtyInUnit() + tmpPh.getFreeQtyInUnit();

            tmpPh.setItemBatch(itemBatch);
            Stock stock = getPharmacyBean().addToStock(tmpPh, Math.abs(addingQty), getSessionController().getDepartment());

            tmpPh.setStock(stock);
            getPharmaceuticalBillItemFacade().edit(tmpPh);

            getBill().getBillItems().add(i);
        }
        if (billItemsTotalQty == 0.0) {
            JsfUtil.addErrorMessage("Please Add Item Quantities To Bill");
            return;
        }

        //check and calculate expenses separately
        if (billExpenses != null && !billExpenses.isEmpty()) {
            getBill().setBillExpenses(billExpenses);

            double totalForExpenses = 0;
            for (BillItem expense : getBillExpenses()) {
                totalForExpenses += expense.getNetValue();
            }

            getBill().setExpenseTotal(-totalForExpenses);
            getBill().setNetTotal(getBill().getNetTotal() - totalForExpenses);
        }

        getPharmacyBillBean().calculateRetailSaleValueAndFreeValueAtPurchaseRate(getBill());

        getBillFacade().edit(getBill());

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(getBill(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);

        JsfUtil.addSuccessMessage("Successfully Billed");
        printPreview = true;
        //   recreate();

    }

    public void removeItem(BillItem bi) {
        //System.err.println("5 " + bi.getItem().getName());
        //System.err.println("6 " + bi.getSearialNo());
        getBillItems().remove(bi.getSearialNo());

        calTotal();

        currentBillItem = null;

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

    public void saveBillFee(BillItem bi) {
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
//        createBillFeePaymentAndPayment(bf, p);
    }

    @Deprecated
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
//
//    public void recreate() {
//       
////        cashPaid = 0.0;
//        currentPharmacyItemData = null;
//        pharmacyItemDatas = null;
//    }

    private List<BillItem> billItems;

    public void addItemWithLastRate() {
        if (getCurrentBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("Please select and item from the list");
            return;
        }

        getCurrentBillItem().setSearialNo(getBillItems().size());
        getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRateInUnit(getPharmacyBean().getLastPurchaseRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));
        getCurrentBillItem().getPharmaceuticalBillItem().setRetailRateInUnit(getPharmacyBean().getLastRetailRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));

        getBillItems().add(getCurrentBillItem());

        calTotal();

        currentBillItem = null;
    }

    public void addItem() {
        System.out.println("add item = ");
        if (getBill().getId() == null) {
            getBillFacade().create(getBill());
        }
//        if (getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate() <= 0) {
//            JsfUtil.addErrorMessage("Please enter a purchase rate");
//            return;
//        }
        if (getCurrentBillItem().getPharmaceuticalBillItem().getDoe() == null) {
            JsfUtil.addErrorMessage("Please set the date of expiry");
            return;
        }
        if (getCurrentBillItem().getItem() instanceof Amp) {
            if (getCurrentBillItem().getPharmaceuticalBillItem().getQty() <= 0 && getCurrentBillItem().getPharmaceuticalBillItem().getFreeQty() <= 0) {
                JsfUtil.addErrorMessage("Please enter the purchase quantity");
                return;
            }
        }
        if (getCurrentBillItem().getItem() instanceof Ampp) {
            if (getCurrentBillItem().getPharmaceuticalBillItem().getQtyPacks() <= 0 && getCurrentBillItem().getPharmaceuticalBillItem().getFreeQtyPacks() <= 0) {
                JsfUtil.addErrorMessage("Please enter the purchase quantity");
                return;
            }
        }

        if (getCurrentBillItem().getPharmaceuticalBillItem().getRetailRate() <= 0) {
            JsfUtil.addErrorMessage("Please enter the sale rate");
            return;
        }
        //TODO: Calculate when packs
//        if (getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate() > getCurrentBillItem().getPharmaceuticalBillItem().getRetailRate()) {
//            JsfUtil.addErrorMessage("Please enter the sale rate that is grater than the purchase rate");
//            return;
//        }

        if (getCurrentBillItem().getItem() instanceof Ampp) {
            getCurrentBillItem().getPharmaceuticalBillItem().setQty(getCurrentBillItem().getPharmaceuticalBillItem().getQtyPacks() * getCurrentBillItem().getItem().getDblValue());
            getCurrentBillItem().getPharmaceuticalBillItem().setFreeQty(getCurrentBillItem().getPharmaceuticalBillItem().getFreeQtyPacks() * getCurrentBillItem().getItem().getDblValue());
            getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRate(getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRatePack() / getCurrentBillItem().getItem().getDblValue());
        }

        System.out.println("getBillItems().size() = " + getBillItems().size());

        getCurrentBillItem().setSearialNo(getBillItems().size());
        getBillItems().add(currentBillItem);

        currentBillItem = null;

        calulateTotalsWhenAddingItems();
    }

    public void saveBill() {

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);

        getBill().setDeptId(deptId);
        getBill().setInsId(deptId);
        getBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);

        getBill().setInstitution(getSessionController().getInstitution());
        getBill().setDepartment(getSessionController().getDepartment());

        getBill().setCreatedAt(new Date());
        getBill().setCreater(getSessionController().getLoggedUser());

        if (getBill().getId() == null) {
            getBillFacade().create(getBill());
        } else {
            getBillFacade().edit(getBill());
        }

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
        //   tmp.setQty(getPharmacyBean().getPurchaseRate(b.getItem(), getSessionController().getDepartment()));
        //     tmp.setPurchaseRate(getPharmacyBean().getPurchaseRate(b.getItem(), getSessionController().getDepartment()));
        tmp.setRetailRate(getPharmacyBillBean().calRetailRate(tmp));
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
        double saleValue = 0.0;
        int serialNo = 0;
        for (BillItem p : getBillItems()) {
            p.setQty((double) p.getPharmaceuticalBillItem().getQtyInUnit());
            p.setRate(p.getPharmaceuticalBillItem().getPurchaseRateInUnit());
            p.setSearialNo(serialNo++);
            double netValue = p.getQty() * p.getRate();
            p.setNetValue(0 - netValue);
            tot += p.getNetValue();
            saleValue += (p.getPharmaceuticalBillItem().getQtyInUnit() + p.getPharmaceuticalBillItem().getFreeQtyInUnit()) * p.getPharmaceuticalBillItem().getRetailRate();
        }
        getBill().setTotal(tot);
        getBill().setNetTotal(tot);
        getBill().setSaleValue(saleValue);
    }

    public void calulateTotalsWhenAddingItems() {
        double tot = 0.0;
        double saleValue = 0.0;
        int serialNo = 0;
        for (BillItem p : getBillItems()) {
            if (p.getItem() instanceof Ampp) {
                p.setQty(p.getPharmaceuticalBillItem().getQtyPacks());
                p.setRate(p.getPharmaceuticalBillItem().getPurchaseRatePack());
            } else if (p.getItem() instanceof Amp) {
                p.setQty((double) p.getPharmaceuticalBillItem().getQty());
                p.setRate(p.getPharmaceuticalBillItem().getPurchaseRate());
            }
            p.setSearialNo(serialNo++);
            double netValue = p.getQty() * p.getRate();
            p.setNetValue(0 - netValue);
            tot += p.getNetValue();
            saleValue += (p.getPharmaceuticalBillItem().getQty() + p.getPharmaceuticalBillItem().getFreeQty()) * p.getPharmaceuticalBillItem().getRetailRate();
        }
        getBill().setTotal(tot);
        getBill().setNetTotal(tot);
        getBill().setSaleValue(saleValue);
    }

    public BilledBill getBill() {
        if (bill == null) {
            bill = new BilledBill();
            bill.setBillType(BillType.PharmacyPurchaseBill);
            bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
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

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
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

    public List<BillItem> getBillItems() {
        if (billItems == null) {
            billItems = new ArrayList<>();
        }
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public AmpController getAmpController() {
        return ampController;
    }

    public void setAmpController(AmpController ampController) {
        this.ampController = ampController;
    }

    public double getSaleRate() {
        return saleRate;
    }

    public void setSaleRate(double saleRate) {
        this.saleRate = saleRate;
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

    public BillListWithTotals getBillListWithTotals() {
        return billListWithTotals;
    }

    public void setBillListWithTotals(BillListWithTotals billListWithTotals) {
        this.billListWithTotals = billListWithTotals;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public double getBillItemsTotalQty() {
        return billItemsTotalQty;
    }

    public void setBillItemsTotalQty(double billItemsTotalQty) {
        this.billItemsTotalQty = billItemsTotalQty;
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

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

}
