/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.CommonFunctionsController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.bean.common.ControllerWithPatient;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SearchController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.TokenController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.membership.MembershipSchemeController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.OptionScope;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.data.dataStructure.ComponentDetail;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.StaffBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillFeePayment;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.Payment;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.Person;
import com.divudi.entity.PreBill;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.Staff;
import com.divudi.entity.Token;
import com.divudi.entity.membership.MembershipScheme;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.ItemBatch;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.entity.pharmacy.UserStock;
import com.divudi.entity.pharmacy.UserStockContainer;
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
import com.divudi.facade.StockHistoryFacade;
import com.divudi.facade.TokenFacade;
import com.divudi.facade.UserStockContainerFacade;
import com.divudi.facade.UserStockFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author Buddhika
 */
/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class PharmacySaleController1 implements Serializable, ControllerWithPatient {

    /**
     * Creates a new instance of PharmacySaleController
     */
    public PharmacySaleController1() {
    }

    @Inject
    PaymentSchemeController PaymentSchemeController;
    @Inject
    StockController stockController;
    @Inject
    ConfigOptionController configOptionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    @Inject
    SessionController sessionController;
    @Inject
    SearchController searchController;

    @Inject
    CommonController commonController;

    @Inject
    TokenController tokenController;
////////////////////////
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    ItemFacade itemFacade;
    @EJB
    StockFacade stockFacade;
    @EJB
    PharmacyBean pharmacyBean;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    StaffBean staffBean;
    @EJB
    private UserStockContainerFacade userStockContainerFacade;
    @EJB
    private UserStockFacade userStockFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    TokenFacade tokenFacade;
/////////////////////////
    Item selectedAvailableAmp;
    Item selectedAlternative;
    private PreBill preBill;
    private Bill saleBill;
    Bill printBill;
    Bill bill;
    BillItem billItem;
    private List<BillItem> selectedBillItems;
    //BillItem removingBillItem;
    BillItem editingBillItem;
    Double qty;
    Integer intQty;
    Stock stock;
    Stock replacableStock;

    PaymentScheme paymentScheme;

    int activeIndex;

    private Token token;
    private Patient patient;
    private YearMonthDay yearMonthDay;
    private String patientTabId = "tabPt";
    private String strTenderedValue = "";
    boolean billPreview = false;
    boolean fromOpdEncounter = false;
    String opdEncounterComments = "";
    int patientSearchTab = 0;

    Staff toStaff;
    Institution toInstitution;
    String errorMessage = "";

    /////////////////
    List<Stock> replaceableStocks;
    //List<BillItem> billItems;
    List<Item> itemsWithoutStocks;
    /////////////////////////   
    double cashPaid;
    double netTotal;
    double balance;
    Double editingQty;
    String cashPaidStr;
    String comment;
    ///////////////////
    private UserStockContainer userStockContainer;
    PaymentMethodData paymentMethodData;
    private boolean patientDetailsEditable;

    public String navigateToPharmacySaleWithoutStocks() {
        prepareForPharmacySaleWithoutStock();
        return "/pharmacy/pharmacy_sale_without_stock";
    }

    public String navigateToPharmacyBillForCashier() {
        return "/pharmacy/pharmacy_bill_retail_sale_for_cashier?faces-redirect=true;";
    }

    public String navigateToPharmacyBillForCashierWholeSale() {
        return "/pharmacy_wholesale/pharmacy_bill_retail_sale_for_cashier?faces-redirect=true;";
    }

    private void prepareForPharmacySaleWithoutStock() {
        clearBill();
        clearBillItem();
        searchController.createPreBillsNotPaid();
        billPreview = false;
    }

    public void searchPatientListener() {
        //  createPaymentSchemeItems();
        calculateAllRates();
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

    public void prepareNewPharmacyBillForMembers() {
        clearNewBillForMembers();
    }

    @Override
    public void toggalePatientEditable() {
        patientDetailsEditable = !patientDetailsEditable;
    }

    public void clearForNewBill() {
        selectedAlternative = null;
        preBill = null;
        saleBill = null;
        printBill = null;
        bill = null;
        billItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
        paymentScheme = null;
        paymentMethod = null;
        activeIndex = 0;
        patient = null;
        yearMonthDay = null;
        patientTabId = "tabPt";
        strTenderedValue = "";
        billPreview = false;
        replaceableStocks = null;
        itemsWithoutStocks = null;
        paymentMethodData = null;
        cashPaid = 0;
        netTotal = 0;
        balance = 0;
        editingQty = null;
        cashPaidStr = null;
    }

    public void clearNewBillForMembers() {
        selectedAlternative = null;
        preBill = null;
        saleBill = null;
        printBill = null;
        bill = null;
        billItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
        paymentScheme = null;
        paymentMethod = null;
        activeIndex = 0;
        patient = null;
        yearMonthDay = null;
        patientTabId = "tabSearchPt";
        patientSearchTab = 1;
        strTenderedValue = "";
        billPreview = false;
        replaceableStocks = null;
        itemsWithoutStocks = null;
        paymentMethodData = null;
        cashPaid = 0;
        netTotal = 0;
        balance = 0;
        editingQty = null;
        cashPaidStr = null;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCashPaidStr() {
        if (cashPaid == 0.0) {
            cashPaidStr = "";
        } else {
            cashPaidStr = String.format("%1$,.2f", cashPaid);
        }
        return cashPaidStr;
    }

    public void setCashPaidStr(String cashPaidStr) {
        try {
            setCashPaid(Double.valueOf(cashPaidStr));
        } catch (Exception e) {
            setCashPaid(0);
        }
        this.cashPaidStr = cashPaidStr;
    }

    public Double getEditingQty() {
        return editingQty;
    }

    public void setEditingQty(Double editingQty) {
        this.editingQty = editingQty;
    }

    public void onTabChange(TabChangeEvent event) {
        if (event != null && event.getTab() != null) {
            setPatientTabId(event.getTab().getId());
        }

        if (!getPatientTabId().equals("tabSearchPt")) {
            if (fromOpdEncounter == false) {
                setPatient(null);
            }
        }

//        createPaymentSchemeItems();
        calculateAllRates();

    }

    public double calculatRemainForMultiplePaymentTotal() {

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();

            }
            return getPreBill().getNetTotal() - multiplePaymentMethodTotalValue;
        }
        return getPreBill().getTotal();
    }

    public double getOldQty(BillItem bItem) {
        String sql = "Select b.qty From BillItem b where b.retired=false and b.bill=:b and b=:itm";
        HashMap hm = new HashMap();
        hm.put("b", getPreBill());
        hm.put("itm", bItem);
        return getBillItemFacade().findDoubleByJpql(sql, hm);
    }

    @Inject
    UserStockController userStockController;

    public void onEdit(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();
        if (tmp == null) {
            return;
        }
        onEdit(tmp);
    }

    private void setZeroToQty(BillItem tmp) {
        tmp.setQty(0.0);
        tmp.getPharmaceuticalBillItem().setQtyInUnit(0.0f);

        userStockController.updateUserStock(tmp.getTransUserStock(), 0);
    }

    //Check when edititng Qty
    //
    public boolean onEdit(BillItem tmp) {
        //Cheking Minus Value && Null
        if (tmp.getQty() <= 0 || tmp.getQty() == null) {
            setZeroToQty(tmp);
            onEditCalculation(tmp);
            JsfUtil.addErrorMessage("Can not enter a minus value");
            return true;
        }

        if (tmp.getQty() > tmp.getPharmaceuticalBillItem().getStock().getStock()) {
            setZeroToQty(tmp);
            onEditCalculation(tmp);
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return true;
        }

        //Check Is There Any Other User using same Stock
        if (!userStockController.isStockAvailable(tmp.getPharmaceuticalBillItem().getStock(), tmp.getQty(), getSessionController().getLoggedUser())) {

            setZeroToQty(tmp);
            onEditCalculation(tmp);
            JsfUtil.addErrorMessage("Another User On Change Bill Item Qty value is resetted");
            return true;
        }

        userStockController.updateUserStock(tmp.getTransUserStock(), tmp.getQty());

        onEditCalculation(tmp);

        return false;
    }

    private void onEditCalculation(BillItem tmp) {
        if (tmp == null) {
            return;
        }

        tmp.setGrossValue(tmp.getQty() * tmp.getRate());
        tmp.getPharmaceuticalBillItem().setQtyInUnit((double) (0 - tmp.getQty()));

        calculateBillItemForEditing(tmp);

        calTotal();

    }

    public void quantityInTableChangeEvent(BillItem tmp) {
        if (tmp == null) {
            return;
        }

        tmp.setGrossValue(tmp.getQty() * tmp.getRate());
        tmp.getPharmaceuticalBillItem().setQtyInUnit((double) (0 - tmp.getQty()));

        calculateBillItemForEditing(tmp);

        calTotal();

    }

    public void editQty(BillItem bi) {
        if (bi == null) {
            //////System.out.println("No Bill Item to Edit Qty");
            return;
        }
        if (editingQty == null) {
            //////System.out.println("Editing qty is null");
            return;
        }

        bi.setQty(editingQty);
        bi.getPharmaceuticalBillItem().setQtyInUnit((double) (0 - editingQty));
        calculateBillItemForEditing(bi);

        calTotal();
        editingQty = null;
    }

    private Patient savePatient() {
        if (!getPatient().getPerson().getName().trim().equals("")) {
            getPatient().setCreater(getSessionController().getLoggedUser());
            getPatient().setCreatedAt(new Date());
            getPatient().getPerson().setCreater(getSessionController().getLoggedUser());
            getPatient().getPerson().setCreatedAt(new Date());
            if (getPatient().getPerson().getId() == null) {
                getPersonFacade().create(getPatient().getPerson());
            }
            if (getPatient().getId() == null) {
                getPatientFacade().create(getPatient());
            }
            return getPatient();
        } else {
            return null;
        }
    }

//    private Patient savePatient() {
//        switch (getPatientTabId()) {
//            case "tabPt":
//                if (!getSearchedPatient().getPerson().getName().trim().equals("")) {
//                    getSearchedPatient().setCreater(getSessionController().getLoggedUser());
//                    getSearchedPatient().setCreatedAt(new Date());
//                    getSearchedPatient().getPerson().setCreater(getSessionController().getLoggedUser());
//                    getSearchedPatient().getPerson().setCreatedAt(new Date());
//                    if (getSearchedPatient().getPerson().getId() == null) {
//                        getPersonFacade().create(getSearchedPatient().getPerson());
//                    }
//                    if (getSearchedPatient().getId() == null) {
//                        getPatientFacade().create(getSearchedPatient());
//                    }
//                    return getSearchedPatient();
//                } else {
//                    return null;
//                }
//            case "tabSearchPt":
//                return getSearchedPatient();
//        }
//        return null;
//    }
    public Title[] getTitle() {
        return Title.values();
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    public List<Stock> getReplaceableStocks() {
        return replaceableStocks;
    }

    public Integer getIntQty() {
        if (qty == null) {
            return null;
        }
        return qty.intValue();
    }

    public void setIntQty(Integer intQty) {
        this.intQty = intQty;
        if (intQty == null) {
            setQty(null);
        } else {
            setQty(intQty.doubleValue());
        }
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

    public void selectReplaceableStocks() {
        if (selectedAlternative == null || !(selectedAlternative instanceof Amp)) {
            replaceableStocks = new ArrayList<>();
            return;
        }
        String sql;
        Map m = new HashMap();
        double d = 0.0;
        Amp amp = (Amp) selectedAlternative;
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("s", d);
        m.put("vmp", amp.getVmp());
        sql = "select i from Stock i join treat(i.itemBatch.item as Amp) amp where i.stock >:s and i.department=:d and amp.vmp=:vmp order by i.itemBatch.item.name";
        replaceableStocks = getStockFacade().findByJpql(sql, m);
    }

    public List<Item> getItemsWithoutStocks() {
        return itemsWithoutStocks;
    }

    public void setItemsWithoutStocks(List<Item> itemsWithoutStocks) {
        this.itemsWithoutStocks = itemsWithoutStocks;
    }

    public String newSaleBillWithoutReduceStock() {
        clearBill();
        clearBillItem();
        billPreview = false;
        return "pharmacy_bill_retail_sale";
    }

    public String newSaleBillWithoutReduceStockForCashier() {
        clearBill();
        clearBillItem();
        billPreview = false;
        return "pharmacy_bill_retail_sale_for_cashier";
    }

    public String navigateToPharmacyRetailSale() {
        resetAll();
        return "/pharmacy/pharmacy_bill_retail_sale?faces-redirect=true";
    }

    public void resetAll() {
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
        clearBill();
        clearBillItem();
//        searchController.createPreBillsNotPaid();
        billPreview = false;
    }

    public void prepareForNewPharmacyRetailBill() {
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
        clearBill();
        clearBillItem();
        searchController.createPreBillsNotPaid();
        billPreview = false;
    }

    public String pharmacyRetailSale() {
        return "/pharmacy_wholesale/pharmacy_bill_retail_sale";
    }

    public String toPharmacyRetailSale() {
        return "/pharmacy/pharmacy_bill_retail_sale";
    }

    public List<Item> completeRetailSaleItems(String qry) {
        Map m = new HashMap<>();
        List<Item> items;
        String sql;
        sql = "select i from Item i where i.retired=false "
                + " and (i.name) like :n and type(i)=:t "
                + " and i.id not in(select ibs.id from Stock ibs where ibs.stock >:s and ibs.department=:d and (ibs.itemBatch.item.name) like :n ) order by i.name ";
        m.put("t", Amp.class);
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("n", "%" + qry + "%");
        double s = 0.0;
        m.put("s", s);
        items = getItemFacade().findByJpql(sql, m, 10);
        return items;
    }

    @Deprecated
    public List<Stock> completeAvailableStocks(String qry) {
        Set<Stock> stockSet = new LinkedHashSet<>(); // Preserve insertion order
        List<Stock> initialStocks = completeAvailableStocksStartsWith(qry);
        if (initialStocks != null) {
            stockSet.addAll(initialStocks);
        }

        // No need to check if initialStocks is empty or null anymore, Set takes care of duplicates
        if (stockSet.size() <= 10) {
            List<Stock> additionalStocks = completeAvailableStocksContains(qry);
            if (additionalStocks != null) {
                stockSet.addAll(additionalStocks);
            }
        }

        return new ArrayList<>(stockSet);
    }

    @Deprecated
    public List<Stock> completeAvailableStocksStartsWith(String qry) {
        List<Stock> stockList;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("s", d);
        m.put("n", qry.toUpperCase() + "%");
        if (qry.length() > 4) {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.barcode) like :n )  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n)  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }
        stockList = getStockFacade().findByJpql(sql, m, 20);
//        itemsWithoutStocks = completeRetailSaleItems(qry);
        //////System.out.println("selectedSaleitems = " + itemsWithoutStocks);
        return stockList;
    }

    @Deprecated
    public List<Stock> completeAvailableStocksContains(String qry) {
        List<Stock> stockList;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("s", d);
        m.put("n", "%" + qry.toUpperCase() + "%");
        if (qry.length() > 4) {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.barcode) like :n )  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n)  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }
        stockList = getStockFacade().findByJpql(sql, m, 20);
//        itemsWithoutStocks = completeRetailSaleItems(qry);
        //////System.out.println("selectedSaleitems = " + itemsWithoutStocks);
        return stockList;
    }

    //matara pharmacy auto complete
    public List<Stock> completeAvailableStocksFromNameOrGeneric(String qry) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("s", d);
        qry = qry.replaceAll("\n", "");
        qry = qry.replaceAll("\r", "");
        m.put("n", "%" + qry.toUpperCase().trim() + "%");

        //////System.out.println("qry = " + qry);
        if (qry.length() > 4) {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.barcode) like :n or (i.itemBatch.item.vmp.name) like :n) order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.vmp.name) like :n)  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }

        items = getStockFacade().findByJpql(sql, m, 20);

        if (qry.length() > 5 && items.size() == 1) {
            stock = items.get(0);
            handleSelectAction();
        } else if (!qry.trim().equals("") && qry.length() > 4) {
            itemsWithoutStocks = completeRetailSaleItemsWithoutStocks(qry);
        }
        return items;
    }

    public void handleSelectAction() {
        if (stock == null) {
            //////System.out.println("Stock NOT selected.");
        }
        if (getBillItem() == null || getBillItem().getPharmaceuticalBillItem() == null) {
            //////System.out.println("Internal Error at PharmacySaleController.java > handleSelectAction");
        }

        getBillItem().getPharmaceuticalBillItem().setStock(stock);
        calculateRates(billItem);
        if (stock != null && stock.getItemBatch() != null) {
            fillReplaceableStocksForAmp((Amp) stock.getItemBatch().getItem());
        }
    }

    public List<Item> completeRetailSaleItemsWithoutStocks(String qry) {
        Map m = new HashMap<>();
        List<Item> items;
        String sql;

        if (qry.length() > 4) {
            sql = "select i from Amp i "
                    + "where i.retired=false and "
                    + "((i.name) like :n or (i.code) like :n  or (i.barcode) like :n  or (i.vmp.name) like :n) and "
                    + "i.id not in(select ibs.itemBatch.item.id from Stock ibs where ibs.stock >:s and ibs.department=:d and ((ibs.itemBatch.item.name) like :n or (ibs.itemBatch.item.code) like :n  or (ibs.itemBatch.item.barcode) like :n  or (ibs.itemBatch.item.vmp.name) like :n )  ) "
                    + "order by i.name ";

        } else {

            sql = "select i from Amp i "
                    + "where i.retired=false and "
                    + "((i.name) like :n or (i.code) like :n or (i.vmp.name) like :n) and "
                    + "i.id not in(select ibs.itemBatch.item.id from Stock ibs where ibs.stock >:s and ibs.department=:d and ((ibs.itemBatch.item.name) like :n or (ibs.itemBatch.item.code) like :n or (ibs.itemBatch.item.vmp.name) like :n )  ) "
                    + "order by i.name ";

        }

//        if (qry.length() > 4) {
//            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.barcode) like :n or (i.itemBatch.item.vmp.name) like :n) order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
//        } else {
//            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.vmp.name) like :n)  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
//        }
//        
//        sql = "select i from Amp i "
//                + "where i.retired=false and "
//                + "(i.name) like :n and "
//                + "i.id not in(select ibs.itemBatch.item.id from Stock ibs where ibs.stock >:s and ibs.department=:d and ibs.itemBatch.item.name like :n) "
//                + "order by i.name ";
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("n", "%" + qry + "%");
        double s = 0.0;
        m.put("s", s);
        items = getItemFacade().findByJpql(sql, m, 10);
        return items;
    }

    public void handleSelect(SelectEvent event) {
        handleSelectAction();
    }

//    public void calculateRates(BillItem bi) {
//        ////////System.out.println("calculating rates");
//        if (bi.getPharmaceuticalBillItem().getStock() == null) {
//            ////////System.out.println("stock is null");
//            return;
//        }
//        getBillItem();
//        PharmaceuticalBillItem pharmBillItem = bi.getPharmaceuticalBillItem();
//        if (pharmBillItem != null) {
//            Stock stock = pharmBillItem.getStock();
//            if (stock != null) {
//                ItemBatch itemBatch = stock.getItemBatch();
//                if (itemBatch != null) {
//                    // Ensure that each step in the chain is not null before accessing further.
//                    bi.setRate(itemBatch.getRetailsaleRate());
//                } else {
//                }
//            } else {
//            }
//        } else {
//        }
//
////        bi.setRate(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate());
//        bi.setDiscount(calculateBillItemDiscountRate(bi));
//        //  ////System.err.println("Discount "+bi.getDiscount());
//        bi.setNetRate(bi.getRate() - bi.getDiscount());
//        //  ////System.err.println("Net "+bi.getNetRate());
//    }
    public void fillReplaceableStocksForAmp(Amp ampIn) {
        String sql;
        Map m = new HashMap();
        double d = 0.0;
        Amp amp = (Amp) ampIn;
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("s", d);
        m.put("vmp", amp.getVmp());
        m.put("a", amp);
        sql = "select i from Stock i join treat(i.itemBatch.item as Amp) amp "
                + "where i.stock >:s and "
                + "i.department=:d and "
                + "amp.vmp=:vmp "
                + "and amp<>:a "
                + "order by i.itemBatch.item.name";
        replaceableStocks = getStockFacade().findByJpql(sql, m);
    }

    public void makeStockAsBillItemStock() {
        ////System.out.println("replacableStock = " + replacableStock);
        setStock(replacableStock);
        ////System.out.println("getStock() = " + getStock());
    }

    public void selectReplaceableStocksNew() {
        if (selectedAvailableAmp == null || !(selectedAvailableAmp instanceof Amp)) {
            replaceableStocks = new ArrayList<>();
            return;
        }
        fillReplaceableStocksForAmp((Amp) selectedAvailableAmp);
    }

    public void calculateBillItemListner(AjaxBehaviorEvent event) {
        calculateBillItem();
    }

    public void calculateBillItem() {
        if (stock == null) {
            return;
        }
        if (getPreBill() == null) {
            return;
        }
        if (billItem == null) {
            return;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            return;
        }
        if (billItem.getPharmaceuticalBillItem().getStock() == null) {
            getBillItem().getPharmaceuticalBillItem().setStock(stock);
        }
        if (getQty() == null) {
            qty = 0.0;
        }
        if (getQty() > getStock().getStock()) {
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return;
        }

        //Bill Item
//        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setItem(getStock().getItemBatch().getItem());
        billItem.setQty(qty);

        //pharmaceutical Bill Item
        billItem.getPharmaceuticalBillItem().setDoe(getStock().getItemBatch().getDateOfExpire());
        billItem.getPharmaceuticalBillItem().setFreeQty(0.0f);
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
        billItem.getPharmaceuticalBillItem().setQtyInUnit((double) (0 - qty));

        //Rates
        //Values
        billItem.setGrossValue(getStock().getItemBatch().getRetailsaleRate() * qty);
        billItem.setNetValue(qty * billItem.getNetRate());
        billItem.setDiscount(billItem.getGrossValue() - billItem.getNetValue());

    }

    public void addBillItem() {
        if (configOptionApplicationController.getBooleanValueByKey("Add quantity from multiple batches in pharmacy retail billing")) {
            addBillItemMultipleBatches();
        } else {
            addBillItemSingleItem();
        }
        processBillItems();
        setActiveIndex(1);
    }

    public void processBillItems() {
        calculateAllRates();
        calculateTotals();
    }

    public void calculateAllRates() {
        for (BillItem tbi : getPreBill().getBillItems()) {
            calculateRates(tbi);
//            calculateBillItemForEditing(tbi);
        }
        calculateTotals();
    }

    public void calculateRates(BillItem bi) {
        PharmaceuticalBillItem pharmBillItem = bi.getPharmaceuticalBillItem();
        if (pharmBillItem != null && pharmBillItem.getStock() != null) {
            ItemBatch itemBatch = pharmBillItem.getStock().getItemBatch();
            if (itemBatch != null) {
                bi.setRate(itemBatch.getRetailsaleRate());
            }
            bi.setDiscountRate(calculateBillItemDiscountRate(bi));
            bi.setNetRate(bi.getRate() - bi.getDiscountRate());
            
            
            bi.setGrossValue(bi.getRate() * bi.getQty());
            bi.setDiscount(bi.getDiscountRate() * bi.getQty());
            bi.setNetValue(bi.getGrossValue() - bi.getDiscount());
            
        }
    }

    public void calculateTotals() {
        getPreBill().setTotal(0);
        double netTotal = 0.0, grossTotal = 0.0, discountTotal = 0.0;
        int index = 0;

        for (BillItem b : getPreBill().getBillItems()) {
            if (!b.isRetired()) {
                b.setSearialNo(index++);
                netTotal += b.getNetValue();
                grossTotal += b.getGrossValue();
                discountTotal += b.getDiscount();
//                getPreBill().setTotal(getPreBill().getTotal() + b.getNetValue());
            }
        }

        getPreBill().setNetTotal(netTotal);
        getPreBill().setTotal(grossTotal);
        getPreBill().setGrantTotal(grossTotal);
        getPreBill().setDiscount(discountTotal);
        setNetTotal(getPreBill().getNetTotal());
    }

    public double addBillItemSingleItem() {
        editingQty = null;
        errorMessage = null;
        double addedQty = 0.0;
        if (billItem == null) {
            return addedQty;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            return addedQty;
        }
        if (getStock() == null) {
            errorMessage = "Item ??";
            JsfUtil.addErrorMessage("Please select an Item Batch to Dispense ??");
            return addedQty;
        }
        if (getStock().getItemBatch().getDateOfExpire().before(commonController.getCurrentDateTime())) {
            JsfUtil.addErrorMessage("Please not select Expired Items");
            return addedQty;
        }
        if (getQty() == null) {
            errorMessage = "Quantity?";
            JsfUtil.addErrorMessage("Quantity?");
            return addedQty;
        }
        if (getQty() == 0.0) {
            errorMessage = "Quantity Zero?";
            JsfUtil.addErrorMessage("Quentity Zero?");
            return addedQty;
        }
        if (getQty() > getStock().getStock()) {
            errorMessage = "No sufficient stocks.";
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return addedQty;
        }

        if (checkItemBatch()) {
            errorMessage = "This batch is already there in the bill.";
            JsfUtil.addErrorMessage("Already added this item batch");
            return addedQty;
        }
//        if (CheckDateAfterOneMonthCurrentDateTime(getStock().getItemBatch().getDateOfExpire())) {
//            errorMessage = "This batch is Expire With in 31 Days.";
//            JsfUtil.addErrorMessage("This batch is Expire With in 31 Days.");
//            return;
//        }
        //Checking User Stock Entity
        if (!userStockController.isStockAvailable(getStock(), getQty(), getSessionController().getLoggedUser())) {
            JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
            return addedQty;
        }
        addedQty = qty;
        billItem.getPharmaceuticalBillItem().setQtyInUnit((double) (0 - qty));
        billItem.getPharmaceuticalBillItem().setStock(stock);
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
        calculateBillItem();
        ////System.out.println("Rate*****" + billItem.getRate());
        billItem.setInwardChargeType(InwardChargeType.Medicine);

        billItem.setItem(getStock().getItemBatch().getItem());
        billItem.setBill(getPreBill());

        billItem.setSearialNo(getPreBill().getBillItems().size() + 1);
        getPreBill().getBillItems().add(billItem);

        if (getUserStockContainer().getId() == null) {
            saveUserStockContainer();
        }

        UserStock us = saveUserStock(billItem);
        billItem.setTransUserStock(us);
        clearBillItem();
        getBillItem();
        return addedQty;
    }

    public void addBillItemMultipleBatches() {
        editingQty = null;
        errorMessage = null;

        if (billItem == null) {
            return;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            return;
        }
        if (getStock() == null) {
            errorMessage = "Please select an Item Batch to Dispense?";
            JsfUtil.addErrorMessage("Please select an Item Batch to Dispense?");
            return;
        }
        Stock userSelectedStock = stock;
//        if (getStock().getItemBatch().getDateOfExpire().before(commonController.getCurrentDateTime())) {
//            JsfUtil.addErrorMessage("You are NOT allowed to select Expired Items");
//            return;
//        }
//        if (getStock().getItemBatch().getDateOfExpire().before(commonController.getCurrentDateTime())) {
//            JsfUtil.addErrorMessage("You are NOT allowed to select Expired Items");
//            return;
//        }
//        if (getStock().getItemBatch().getDateOfExpire().before(commonController.getCurrentDateTime())) {
//            JsfUtil.addErrorMessage("You are NOT allowed to select Expired Items");
//            return;
//        }
//        if (getStock().getItemBatch().getDateOfExpire().before(commonController.getCurrentDateTime())) {
//            JsfUtil.addErrorMessage("You are NOT allowed to select Expired Items");
//            return;
//        }
        if (getQty() == null) {
            errorMessage = "Please enter a Quantity";
            JsfUtil.addErrorMessage("Quantity?");
            return;
        }
        if (getQty() == 0.0) {
            errorMessage = "Please enter a Quantity";
            JsfUtil.addErrorMessage("Quentity Zero?");
            return;
        }

        double requestedQty = getQty();
        double addedQty = 0.0;
        double remainingQty = getQty();

        if (getQty() <= getStock().getStock()) {
            double thisTimeAddingQty = addBillItemSingleItem();
            if (thisTimeAddingQty >= requestedQty) {
                return;
            } else {
                addedQty += thisTimeAddingQty;
                remainingQty = remainingQty - thisTimeAddingQty;
            }
        } else {
            qty = getStock().getStock();
            double thisTimeAddingQty = addBillItemSingleItem();
            addedQty += thisTimeAddingQty;
            remainingQty = remainingQty - thisTimeAddingQty;
        }

//        addedQty = addBillItemSingleItem();
//        System.out.println("stock = " + userSelectedStock);
//        System.out.println("stock item batch = " + userSelectedStock.getItemBatch());
//        System.out.println("stock item batch item= " + userSelectedStock.getItemBatch().getItem());
        List<Stock> availableStocks = stockController.findNextAvailableStocks(userSelectedStock);
        for (Stock s : availableStocks) {
            stock = s;
            if (remainingQty < s.getStock()) {
                qty = remainingQty;
            } else {
                qty = s.getStock();
            }
            double thisTimeAddingQty = addBillItemSingleItem();
            addedQty += thisTimeAddingQty;
            remainingQty = remainingQty - thisTimeAddingQty;
            if (remainingQty <= 0) {
                return;
            }
        }

    }

    private void addSingleStock() {
        billItem.getPharmaceuticalBillItem().setQtyInUnit((double) (0 - qty));
        billItem.getPharmaceuticalBillItem().setStock(stock);
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
        calculateBillItem();
        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setItem(getStock().getItemBatch().getItem());
        billItem.setBill(getPreBill());
        billItem.setSearialNo(getPreBill().getBillItems().size() + 1);
        getPreBill().getBillItems().add(billItem);
        if (getUserStockContainer().getId() == null) {
            saveUserStockContainer();
        }
        UserStock us = saveUserStock(billItem);
        billItem.setTransUserStock(us);
    }

    private void addMultipleStock() {
        Double remainingQty = Math.abs(qty) - Math.abs(getStock().getStock());
        addSingleStock();
        List<Stock> availableStocks = stockController.findNextAvailableStocks(getStock());

    }

    private void saveUserStockContainer() {
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());

        getUserStockContainer().setCreater(getSessionController().getLoggedUser());
        getUserStockContainer().setCreatedAt(new Date());

        getUserStockContainerFacade().create(getUserStockContainer());

    }

    private UserStock saveUserStock(BillItem tbi) {
        UserStock us = new UserStock();
        us.setStock(tbi.getPharmaceuticalBillItem().getStock());
        us.setUpdationQty(tbi.getQty());
        us.setCreater(getSessionController().getLoggedUser());
        us.setCreatedAt(new Date());
        us.setUserStockContainer(getUserStockContainer());
        getUserStockFacade().create(us);

        getUserStockContainer().getUserStocks().add(us);

        return us;
    }

//    public void calculateAllRatesNew() {
//        ////////System.out.println("calculating all rates");
//        for (BillItem tbi : getPreBill().getBillItems()) {
//            calculateRates(tbi);
//            calculateBillItemForEditing(tbi);
//        }
//        calTotal();
//    }
    public void calTotalNew() {
        getPreBill().setTotal(0);
        double netTot = 0.0;
        double discount = 0.0;
        double grossTot = 0.0;
        int index = 0;
        for (BillItem b : getPreBill().getBillItems()) {
            if (b.isRetired()) {
                continue;
            }
            b.setSearialNo(index++);

            netTot = netTot + b.getNetValue();
            grossTot = grossTot + b.getGrossValue();
            discount = discount + b.getDiscount();
            getPreBill().setTotal(getPreBill().getTotal() + b.getNetValue());
        }
        ////System.out.println("2.discount = " + discount);
        //   netTot = netTot + getPreBill().getServiceCharge();
        getPreBill().setNetTotal(netTot);
        getPreBill().setTotal(grossTot);
        getPreBill().setGrantTotal(grossTot);
        getPreBill().setDiscount(discount);
        setNetTotal(getPreBill().getNetTotal());

    }

//    Checked
    public BillItem getBillItem() {
        if (billItem == null) {
            billItem = new BillItem();
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
            pbi.setBillItem(billItem);
            billItem.setPharmaceuticalBillItem(pbi);
        }
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

//    private boolean errorCheckForPreBill() {
//        if (getPreBill().getBillItems().isEmpty()) {
//            JsfUtil.addErrorMessage("No Items added to bill to sale");
//            return true;
//        }
//        return false;
//    }
//    private boolean checkPaymentScheme(PaymentScheme paymentScheme) {
//        if (paymentScheme != null && paymentScheme.getPaymentMethod() != null && paymentScheme.getPaymentMethod() == PaymentMethod.Cheque) {
//            if (getSaleBill().getBank() == null || getSaleBill().getChequeRefNo() == null || getSaleBill().getChequeDate() == null) {
//                JsfUtil.addErrorMessage("Please select Cheque Number,Bank and Cheque Date");
//                return true;
//            }
//
//        }
//
//        if (paymentScheme != null && paymentScheme.getPaymentMethod() != null && paymentScheme.getPaymentMethod() == PaymentMethod.Slip) {
//            if (getSaleBill().getBank() == null || getSaleBill().getComments() == null || getSaleBill().getChequeDate() == null) {
//                JsfUtil.addErrorMessage("Please Fill Memo,Bank and Slip Date ");
//                return true;
//            }
//
//        }
//
//        if (paymentScheme != null && paymentScheme.getPaymentMethod() != null && paymentScheme.getPaymentMethod() == PaymentMethod.Card) {
//            if (getSaleBill().getBank() == null || getSaleBill().getCreditCardRefNo() == null) {
//                JsfUtil.addErrorMessage("Please Fill Credit Card Number and Bank");
//                return true;
//            }
//
////            if (getCreditCardRefNo().trim().length() < 16) {
////                JsfUtil.addErrorMessage("Enter 16 Digit");
////                return true;
////            }
//        }
//
//        if (paymentScheme != null && paymentScheme.getPaymentMethod() != null && paymentScheme.getPaymentMethod() == PaymentMethod.Credit) {
//            if (getSaleBill().getCreditCompany() == null) {
//                JsfUtil.addErrorMessage("Please Select Credit Company");
//                return true;
//            }
//
//        }
//
//        if (paymentScheme != null && paymentScheme.getPaymentMethod() != null && paymentScheme.getPaymentMethod() == PaymentMethod.Cash) {
//            if (getPreBill().getCashPaid() == 0.0) {
//                JsfUtil.addErrorMessage("Please select tendered amount correctly");
//                return true;
//            }
//            if (getPreBill().getCashPaid() < getPreBill().getNetTotal()) {
//                JsfUtil.addErrorMessage("Please select tendered amount correctly");
//                return true;
//            }
//        }
//
//        return false;
//
//    }
    @Inject
    PaymentSchemeController paymentSchemeController;
    PaymentMethod paymentMethod;

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    private boolean errorCheckForSaleBill() {

        if (getPaymentSchemeController().checkPaymentMethodError(getPaymentMethod(), paymentMethodData)) {
            return true;
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need to Enter the Cash Tendered Amount to Settle Pharmacy Retail Bill", true)) {
            if (paymentMethod == PaymentMethod.Cash) {
                if (cashPaid == 0.0) {
                    JsfUtil.addErrorMessage("Please enter the paid amount");
                    return true;
                }
                if (cashPaid < getPreBill().getNetTotal()) {
                    JsfUtil.addErrorMessage("Please select tendered amount correctly");
                    return true;
                }
            }
        }

        return false;
    }

    private void savePreBillFinally(Patient pt) {
        if (getPreBill().getId() == null) {
            getBillFacade().create(getPreBill());
        }
        getPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getPreBill().setCreater(getSessionController().getLoggedUser());

        getPreBill().setPatient(pt);
        getPreBill().setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(pt, getSessionController().getApplicationPreference().isMembershipExpires()));
        getPreBill().setToStaff(toStaff);
        getPreBill().setToInstitution(toInstitution);

        getPreBill().setComments(comment);

        getPreBill().setCashPaid(cashPaid);
        getPreBill().setBalance(balance);

        getPreBill().setBillDate(new Date());
        getPreBill().setBillTime(new Date());
        getPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getPreBill().setPaymentMethod(getPaymentMethod());
        getPreBill().setPaymentScheme(getPaymentScheme());

        getBillBean().setPaymentMethodData(getPreBill(), getPaymentMethod(), getPaymentMethodData());

        String insId = getBillNumberBean().institutionBillNumberGenerator(getPreBill().getInstitution(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
        getPreBill().setInsId(insId);
        String deptId = getBillNumberBean().departmentBillNumberGenerator(getPreBill().getDepartment(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
        getPreBill().setDeptId(deptId);
        getPreBill().setInvoiceNumber(billNumberBean.fetchPaymentSchemeCount(getPreBill().getPaymentScheme(), getPreBill().getBillType(), getPreBill().getInstitution()));

    }

    @Inject
    private BillBeanController billBean;

    private void saveSaleBill() {
        //  calculateAllRates();

        getSaleBill().copy(getPreBill());
        getSaleBill().copyValue(getPreBill());

        getSaleBill().setBillType(BillType.PharmacySale);
        getSaleBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE);

        getSaleBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getSaleBill().setInstitution(getSessionController().getLoggedUser().getInstitution());
        getSaleBill().setBillDate(new Date());
        getSaleBill().setBillTime(new Date());
        getSaleBill().setCreatedAt(Calendar.getInstance().getTime());
        getSaleBill().setCreater(getSessionController().getLoggedUser());
        getSaleBill().setReferenceBill(getPreBill());

        getSaleBill().setInsId(getPreBill().getInsId());
        getSaleBill().setDeptId(getPreBill().getDeptId());
        getSaleBill().setInvoiceNumber(getPreBill().getInvoiceNumber());
        getSaleBill().setComments(comment);

        getSaleBill().setCashPaid(cashPaid);
        getSaleBill().setBalance(balance);

        getBillBean().setPaymentMethodData(getSaleBill(), getSaleBill().getPaymentMethod(), getPaymentMethodData());

        if (getSaleBill().getId() == null) {
            getBillFacade().create(getSaleBill());
        }

        updatePreBill();
    }
//

    private void updatePreBill() {
        getPreBill().setReferenceBill(getSaleBill());
        getBillFacade().edit(getPreBill());
    }

    private void savePreBillItemsFinally(List<BillItem> list) {
        for (BillItem tbi : list) {
            if (onEdit(tbi)) {
//If any issue in Stock Bill Item will not save & not include for total
//                continue;
            }
            tbi.setInwardChargeType(InwardChargeType.Medicine);
            tbi.setBill(getPreBill());

            tbi.setCreatedAt(Calendar.getInstance().getTime());
            tbi.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem tmpPh = tbi.getPharmaceuticalBillItem();
            tbi.setPharmaceuticalBillItem(null);

            if (tbi.getId() == null) {
                getBillItemFacade().create(tbi);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            }

            tbi.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(tbi);

            double qtyL = tbi.getPharmaceuticalBillItem().getQtyInUnit() + tbi.getPharmaceuticalBillItem().getFreeQtyInUnit();

            //Deduct Stock
            boolean returnFlag = getPharmacyBean().deductFromStock(tbi.getPharmaceuticalBillItem().getStock(),
                    Math.abs(qtyL), tbi.getPharmaceuticalBillItem(), getPreBill().getDepartment());

            if (!returnFlag) {
                tbi.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(tbi.getPharmaceuticalBillItem());
                getBillItemFacade().edit(tbi);
            }

            getPreBill().getBillItems().add(tbi);
        }

        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());

        calculateAllRates();

        getBillFacade().edit(getPreBill());
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    private void saveSaleBillItems(List<BillItem> list) {
        for (BillItem tbi : list) {

            BillItem newBil = new BillItem();

            newBil.copy(tbi);
            newBil.setReferanceBillItem(tbi);
            newBil.setBill(getSaleBill());
            newBil.setInwardChargeType(InwardChargeType.Medicine);
            //      newBil.setBill(getSaleBill());
            newBil.setCreatedAt(Calendar.getInstance().getTime());
            newBil.setCreater(getSessionController().getLoggedUser());

            if (newBil.getId() == null) {
                getBillItemFacade().create(newBil);
            }

            PharmaceuticalBillItem newPhar = new PharmaceuticalBillItem();
            newPhar.copy(tbi.getPharmaceuticalBillItem());
            newPhar.setBillItem(newBil);

            if (newPhar.getId() == null) {
                getPharmaceuticalBillItemFacade().create(newPhar);
            }

            newBil.setPharmaceuticalBillItem(newPhar);
            getBillItemFacade().edit(newBil);

            getSaleBill().getBillItems().add(newBil);

            tbi.setReferanceBillItem(newBil);
            getBillItemFacade().edit(tbi);
        }

        getBillFacade().edit(getSaleBill());
    }

    private void saveSaleBillItems(List<BillItem> list, Payment p) {
        for (BillItem tbi : list) {

            BillItem newBil = new BillItem();

            newBil.copy(tbi);
            newBil.setReferanceBillItem(tbi);
            newBil.setBill(getSaleBill());
            newBil.setInwardChargeType(InwardChargeType.Medicine);
            //      newBil.setBill(getSaleBill());
            newBil.setCreatedAt(Calendar.getInstance().getTime());
            newBil.setCreater(getSessionController().getLoggedUser());

            if (newBil.getId() == null) {
                getBillItemFacade().create(newBil);
            }

            PharmaceuticalBillItem newPhar = new PharmaceuticalBillItem();
            newPhar.copy(tbi.getPharmaceuticalBillItem());
            newPhar.setBillItem(newBil);

            if (newPhar.getId() == null) {
                getPharmaceuticalBillItemFacade().create(newPhar);
            }

            newBil.setPharmaceuticalBillItem(newPhar);
            getBillItemFacade().edit(newBil);

            getSaleBill().getBillItems().add(newBil);

            tbi.setReferanceBillItem(newBil);
            getBillItemFacade().edit(tbi);
            saveBillFee(newBil, p);
        }

        getBillFacade().edit(getSaleBill());
    }

    private boolean checkAllBillItem() {
        for (BillItem b : getPreBill().getBillItems()) {

            if (onEdit(b)) {
                return true;
            }
        }

        return false;

    }

    public void settlePreBill() {
        editingQty = null;

        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Items added to bill to sale");
            return;
        }

        if (!getPreBill().getBillItems().isEmpty()) {
            for (BillItem bi : getPreBill().getBillItems()) {
                if (!userStockController.isStockAvailable(bi.getPharmaceuticalBillItem().getStock(), bi.getQty(), getSessionController().getLoggedUser())) {
                    setZeroToQty(bi);
                    JsfUtil.addErrorMessage("Another User On Change Bill Item Qty value is resetted");
                    return;
                }
                if (bi.getQty() <= 0.0) {
                    JsfUtil.addErrorMessage("Some BillItem Quntity is Zero or less than Zero");
                    return;
                }
            }
        }
        if (getPreBill().isCancelled() == true) {
            getPreBill().setCancelled(false);
        }
//        if (checkAllBillItem()) {
//            //   Before Settle Bill Current Bills Item Check Agian There is any otheruser change his qty
//            return;
//        }
//        if (errorCheckForPreBill()) {
//            return;
//        }

        if (billPreview) {

        }

        calculateAllRates();

        Patient pt = savePatient();
        List<BillItem> tmpBillItems = new ArrayList<>();
        for (BillItem i : getPreBill().getBillItems()) {
            tmpBillItems.add(i);
        }
        getPreBill().setBillItems(null);

//        savePreBillFinally(pt);
        savePreBillFinallyForRetailSaleForCashier(pt);
        savePreBillItemsFinally(tmpBillItems);
        setPrintBill(getBillFacade().find(getPreBill().getId()));

        if (getToken() != null) {
            getToken().setBill(getPreBill());
            tokenFacade.edit(getToken());
        }

        markToken();
        resetAll();
        billPreview = true;
    }

    public void markToken() {
        Token t = getToken();
        if (t == null) {
            return;
        }
        t.setBill(getPreBill());
        t.setCalled(true);
        t.setCalledAt(new Date());
        t.setInProgress(false);
        t.setCompleted(false);
        tokenController.save(t);
    }
    
    private void savePreBillFinallyForRetailSaleForCashier(Patient pt) {
        if (getPreBill().getId() == null) {
            getBillFacade().create(getPreBill());
        }
        getPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getPreBill().setCreater(getSessionController().getLoggedUser());

        getPreBill().setPatient(pt);
        getPreBill().setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(pt, getSessionController().getApplicationPreference().isMembershipExpires()));
        getPreBill().setToStaff(toStaff);
        getPreBill().setToInstitution(toInstitution);

        getPreBill().setComments(comment);

        getPreBill().setCashPaid(cashPaid);
        getPreBill().setBalance(balance);

        getPreBill().setBillDate(new Date());
        getPreBill().setBillTime(new Date());
        getPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getPreBill().setPaymentMethod(getPaymentMethod());
        getPreBill().setPaymentScheme(getPaymentScheme());

        getBillBean().setPaymentMethodData(getPreBill(), getPaymentMethod(), getPaymentMethodData());

        String insId = getBillNumberBean().institutionBillNumberGenerator(getPreBill().getInstitution(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
        getPreBill().setInsId(insId);
        String deptId = getBillNumberBean().departmentBillNumberGenerator(getPreBill().getDepartment(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
        getPreBill().setDeptId(deptId);
        getPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        getPreBill().setInvoiceNumber(billNumberBean.fetchPaymentSchemeCount(getPreBill().getPaymentScheme(), getPreBill().getBillType(), getPreBill().getInstitution()));

    }

    @EJB
    private CashTransactionBean cashTransactionBean;

    public void settleBillWithPay() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        editingQty = null;

        if (sessionController.getApplicationPreference().isCheckPaymentSchemeValidation()) {
            if (getPaymentScheme() == null) {
                JsfUtil.addErrorMessage("Please select Payment Scheme");
                return;
            }
        }

        if (getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select Payment Method");
            return;
        }

        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Please add items to the bill.");
            return;
        }

        if (getPatient() == null) {
            JsfUtil.addErrorMessage("Please Select a Patient");
            return;
        }

        if (!getPreBill().getBillItems().isEmpty()) {
            for (BillItem bi : getPreBill().getBillItems()) {
                if (!userStockController.isStockAvailable(bi.getPharmaceuticalBillItem().getStock(), bi.getQty(), getSessionController().getLoggedUser())) {
                    setZeroToQty(bi);
//                    onEditCalculation(bi);
                    JsfUtil.addErrorMessage("Another User On Change Bill Item Qty value is resetted");
                    return;
                }
                ////System.out.println("bi.getItem().getName() = " + bi.getItem().getName());
                ////System.out.println("bi.getQty() = " + bi.getQty());
                if (bi.getQty() <= 0.0) {
                    ////System.out.println("bi.getQty() = " + bi.getQty());
                    JsfUtil.addErrorMessage("Some BillItem Quntity is Zero or less than Zero");
                    return;
                }
            }
        }

        Patient pt = savePatient();

        if (errorCheckForSaleBill()) {
            return;
        }
        if (getPaymentMethod() == PaymentMethod.Credit) {
            if (toStaff == null && toInstitution == null) {
                JsfUtil.addErrorMessage("Please select Staff Member under welfare or credit company.");
                return;
            }
            if (toStaff == null && toInstitution == null) {
                JsfUtil.addErrorMessage("Both staff member and a company is selected. Please select either Staff Member under welfare or credit company.");
                return;
            }
            if (toStaff != null) {
                if (toStaff.getAnnualWelfareUtilized() + netTotal > toStaff.getAnnualWelfareQualified()) {
                    JsfUtil.addErrorMessage("No enough walfare credit.");
                    return;
                }
            }
        } else if (getPaymentMethod() == PaymentMethod.PatientDeposit) {
            if (pt == null) {
                JsfUtil.addErrorMessage("Need a Patient");
                return;
            }

            if (pt.getHasAnAccount() == false) {
                JsfUtil.addErrorMessage("Peation have not account");
                return;
            }

            double runningBalance;
            double creditLimit;
            double availableValue;

            if (pt.getRunningBalance() != null) {
                runningBalance = pt.getRunningBalance();
            } else {
                runningBalance = 0;
            }

            if (pt.getCreditLimit() != null) {
                creditLimit = pt.getCreditLimit();
            } else {
                creditLimit = 0;
            }

            availableValue = runningBalance + creditLimit;

            if (availableValue < netTotal) {
                JsfUtil.addErrorMessage("No sufficient balance");
                return;
            }
        } else if (getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            if (getPaymentMethodData() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return;
            }
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                //TODO - filter only relavant value
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
            }
            double differenceOfBillTotalAndPaymentValue = getPreBill().getNetTotal() - multiplePaymentMethodTotalValue;
            differenceOfBillTotalAndPaymentValue = Math.abs(differenceOfBillTotalAndPaymentValue);
            if (differenceOfBillTotalAndPaymentValue > 1.0) {
                JsfUtil.addErrorMessage("Mismatch in differences of multiple payment method total and bill total");
                return;
            }
            if (cashPaid == 0.0) {
                setCashPaid(multiplePaymentMethodTotalValue);
            }

        }

//        if (checkAllBillItem()) {
//            return;
//        }
        calculateAllRates();

        getPreBill().setPaidAmount(getPreBill().getTotal());

        List<BillItem> tmpBillItems = getPreBill().getBillItems();
        getPreBill().setBillItems(null);

        savePreBillFinally(pt);
        savePreBillItemsFinally(tmpBillItems);

        saveSaleBill();
        Payment p = createPayment(getSaleBill(), paymentMethod);
        saveSaleBillItems(tmpBillItems, p);

//        getPreBill().getCashBillsPre().add(getSaleBill());
        getBillFacade().edit(getPreBill());

        setPrintBill(getBillFacade().find(getSaleBill().getId()));

        getCashTransactionBean().saveBillCashInTransaction(getSaleBill(), getSessionController().getLoggedUser());

        if (toStaff != null && getPaymentMethod() == PaymentMethod.Credit) {
            getStaffBean().updateStaffCredit(toStaff, netTotal);
            JsfUtil.addSuccessMessage("User Credit Updated");
        } else if (getPaymentMethod() == PaymentMethod.PatientDeposit) {
            double runningBalance;
            if (pt != null) {
                if (pt.getRunningBalance() != null) {
                    runningBalance = pt.getRunningBalance();
                } else {
                    runningBalance = 0.0;
                }
                runningBalance += netTotal;
                pt.setRunningBalance(runningBalance);
            }

        }

        resetAll();

        billPreview = true;

    }

    public String newPharmacyRetailSale() {
        clearBill();
        clearBillItem();
        billPreview = false;
        return "pharmacy_bill_retail_sale";
    }

//    checked
    private boolean checkItemBatch() {
        for (BillItem bItem : getPreBill().getBillItems()) {
            if (bItem.getPharmaceuticalBillItem().getStock().equals(getBillItem().getPharmaceuticalBillItem().getStock())) {
                return true;
            }
        }
        return false;
    }

    public void addBillItemOld() {
        editingQty = null;

        if (billItem == null) {
            errorMessage = "No Bill Item";
            return;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            errorMessage = "No Pharmaceutical Bill Item";
            return;
        }
        if (getStock() == null) {
            errorMessage = "Please select item";
            return;
        }

        if (billItem.getPharmaceuticalBillItem().getStock() == null) {
            errorMessage = "Pls Select Stock";
            return;
        }

        if (getQty() == null) {
            errorMessage = "Quentity?";
            return;
        }

        if (getQty() > getStock().getStock()) {
            errorMessage = "No Sufficient Stocks";
            return;
        }

        if (checkItemBatch()) {
            errorMessage = "Already added this item batch";
            return;
        }

        //Checking User Stock Entity
        if (!userStockController.isStockAvailable(getStock(), getQty(), getSessionController().getLoggedUser())) {
            errorMessage = "Sorry Already Other User Try to Billing This Stock You Cant Add";
            return;
        }

        billItem.setBill(getPreBill());
        billItem.setSearialNo(getPreBill().getBillItems().size() + 1);
        getPreBill().getBillItems().add(billItem);

        //User Stock Container Save if New Bill
        userStockController.saveUserStockContainer(getUserStockContainer(), getSessionController().getLoggedUser());
        UserStock us = userStockController.saveUserStock(billItem, getSessionController().getLoggedUser(), getUserStockContainer());
        billItem.setTransUserStock(us);

        calculateAllRates();

        calTotal();

        clearBillItem();
        setActiveIndex(1);
    }

    public void calTotal() {
        getPreBill().setTotal(0);
        double netTot = 0.0;
        double discount = 0.0;
        double grossTot = 0.0;
        int index = 0;
        for (BillItem b : getPreBill().getBillItems()) {
            if (b.isRetired()) {
                continue;
            }
            b.setSearialNo(index++);

            netTot = netTot + b.getNetValue();
            grossTot = grossTot + b.getGrossValue();
            discount = discount + b.getDiscount();
            getPreBill().setTotal(getPreBill().getTotal() + b.getGrossValue());
        }
        ////System.out.println("1.discount = " + discount);
        netTot = netTot + getPreBill().getServiceCharge();

        getPreBill().setNetTotal(netTot);
        getPreBill().setTotal(grossTot);
        getPreBill().setGrantTotal(grossTot);
        getPreBill().setDiscount(discount);
        setNetTotal(getPreBill().getNetTotal());

    }

    @EJB
    private StockHistoryFacade stockHistoryFacade;

    public void removeBillItem(BillItem b) {
        userStockController.removeUserStock(b.getTransUserStock(), getSessionController().getLoggedUser());
        getPreBill().getBillItems().remove(b.getSearialNo());

        calTotal();
    }

    public void removeSelectedBillItems() {
        if (selectedBillItems == null || selectedBillItems.isEmpty()) {
            JsfUtil.addErrorMessage("Please select items to delete");
            return;
        }

        Iterator<BillItem> iterator = selectedBillItems.iterator();
        while (iterator.hasNext()) {
            BillItem billItem = iterator.next();
            userStockController.removeUserStock(billItem.getTransUserStock(), getSessionController().getLoggedUser());
            getPreBill().getBillItems().remove(billItem);
            iterator.remove();
        }

        calTotal();
    }

//    Checked
    public void handleQuentityEntryListner(AjaxBehaviorEvent event) {
        if (stock == null) {
            errorMessage = "No stock";
            return;
        }
        if (getPreBill() == null) {
            errorMessage = "No Pre Bill";
            return;
        }
        if (billItem == null) {
            errorMessage = "No Bill Item";
            return;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            errorMessage = "No Pharmaceutical Bill Item";
            return;
        }
        if (billItem.getPharmaceuticalBillItem().getStock() == null) {
            errorMessage = "No Stock. Stock Assigned.";
            getBillItem().getPharmaceuticalBillItem().setStock(stock);
        }
        if (getQty() == null) {
            qty = 0.0;
        }
        billItem.setQty(qty);
        //pharmaceutical Bill Item
        billItem.getPharmaceuticalBillItem().setFreeQty(0.0f);
        billItem.getPharmaceuticalBillItem().setQtyInUnit((double) (0 - qty));
        //Values
        billItem.setGrossValue(getStock().getItemBatch().getRetailsaleRate() * qty);
    }

    public void calculateBillItemForEditing(BillItem bi) {
        //////System.out.println("calculateBillItemForEditing");
        //////System.out.println("bi = " + bi);
        if (getPreBill() == null || bi == null || bi.getPharmaceuticalBillItem() == null || bi.getPharmaceuticalBillItem().getStock() == null) {
            //////System.out.println("calculateItemForEditingFailedBecause of null");
            return;
        }
        //////System.out.println("bi.getQty() = " + bi.getQty());
        //////System.out.println("bi.getRate() = " + bi.getRate());
        bi.setGrossValue(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate() * bi.getQty());
        bi.setNetValue(bi.getQty() * bi.getNetRate());
        bi.setDiscount(bi.getGrossValue() - bi.getNetValue());
        //////System.out.println("bi.getNetValue() = " + bi.getNetValue());

    }

//    Checked
    public void handleStockSelect(SelectEvent event) {
        if (stock == null) {
            JsfUtil.addErrorMessage("Empty Stock");
            return;
        }
        getBillItem();
        billItem.getPharmaceuticalBillItem().setStock(stock);

        if (billItem.getPharmaceuticalBillItem().getStock() == null) {
            JsfUtil.addErrorMessage("Null Stock");
            return;
        }
        if (billItem.getPharmaceuticalBillItem().getStock().getItemBatch() == null) {
            JsfUtil.addErrorMessage("Null Batch Stock");
            return;
        }
//        getBillItem();
        billItem.setRate(billItem.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate());
        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setItem(getStock().getItemBatch().getItem());
        //pharmaceutical Bill Item
        billItem.getPharmaceuticalBillItem().setDoe(getStock().getItemBatch().getDateOfExpire());
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
    }

    public void paymentSchemeChanged(AjaxBehaviorEvent ajaxBehavior) {
        calculateAllRates();
    }

    public void changeListener() {
        calculateAllRates();
    }

//    public void calculateAllRates() {
//        //////System.out.println("calculating all rates");
//        for (BillItem tbi : getPreBill().getBillItems()) {
//            calculateDiscountRates(tbi);
//            calculateBillItemForEditing(tbi);
//        }
//        calTotal();
//    }
    public void calculateRateListner(AjaxBehaviorEvent event) {

    }

//    Checked
    public void calculateDiscountRates(BillItem bi) {
        bi.setDiscountRate(calculateBillItemDiscountRate(bi));
        bi.setDiscount(bi.getDiscountRate() * bi.getQty());
        bi.setNetRate(bi.getRate() - bi.getDiscountRate());
    }

    @Inject
    PriceMatrixController priceMatrixController;
    @Inject
    MembershipSchemeController membershipSchemeController;

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

//    TO check the functionality
    public double calculateBillItemDiscountRate(BillItem bi) {
        if (bi == null) {
            return 0.0;
        }
        if (bi.getPharmaceuticalBillItem() == null) {
            return 0.0;
        }
        if (bi.getPharmaceuticalBillItem().getStock() == null) {
            return 0.0;
        }
        if (bi.getPharmaceuticalBillItem().getStock().getItemBatch() == null) {
            return 0.0;
        }
        bi.setItem(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getItem());
        double retailRate = bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate();
        double discountRate = 0;
        boolean discountAllowed = bi.getItem().isDiscountAllowed();

        MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(getPatient(), getSessionController().getApplicationPreference().isMembershipExpires());

        //MEMBERSHIPSCHEME DISCOUNT
        if (membershipScheme != null && discountAllowed) {
            PaymentMethod tpm = getPaymentMethod();
            if (tpm == null) {
                tpm = PaymentMethod.Cash;
            }
            PriceMatrix priceMatrix = getPriceMatrixController().getPharmacyMemberDisCount(tpm, membershipScheme, getSessionController().getDepartment(), bi.getItem().getCategory());
            if (priceMatrix == null) {
                return 0;
            } else {
                bi.setPriceMatrix(priceMatrix);
                return (retailRate * priceMatrix.getDiscountPercent()) / 100;
            }
        }

        //PAYMENTSCHEME DISCOUNT
        if (getPaymentScheme() != null && discountAllowed) {
            PriceMatrix priceMatrix = getPriceMatrixController().getPaymentSchemeDiscount(getPaymentMethod(), getPaymentScheme(), getSessionController().getDepartment(), bi.getItem());

            //  //System.err.println("tr = " + tr);
            if (priceMatrix != null) {
                bi.setPriceMatrix(priceMatrix);
                discountRate = priceMatrix.getDiscountPercent();
            }

            double dr;
            dr = (retailRate * discountRate) / 100;
            return dr;

        }

        //PAYMENTMETHOD DISCOUNT
        if (getPaymentMethod() != null && discountAllowed) {
            PriceMatrix priceMatrix = getPriceMatrixController().getPaymentSchemeDiscount(getPaymentMethod(), getSessionController().getDepartment(), bi.getItem());

            if (priceMatrix != null) {
                bi.setPriceMatrix(priceMatrix);
                discountRate = priceMatrix.getDiscountPercent();
            }

            double dr;
            dr = (retailRate * discountRate) / 100;

            return dr;

        }

        //CREDIT COMPANY DISCOUNT
        if (getPaymentMethod() == PaymentMethod.Credit && toInstitution != null) {
            discountRate = toInstitution.getPharmacyDiscount();

            double dr;
            dr = (retailRate * discountRate) / 100;

            return dr;
        }

        return 0;

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

    public Payment createPayment(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        setPaymentMethodData(p, pm);
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

    private void clearBill() {
        preBill = null;
        saleBill = null;
        patient = null;
        patient = null;
        toInstitution = null;
        toStaff = null;
//        billItems = null;
        patientTabId = "tabPt";
        cashPaid = 0;
        netTotal = 0;
        balance = 0;
        paymentScheme = null;
        paymentMethod = PaymentMethod.Cash;
        userStockContainer = null;
        fromOpdEncounter = false;
        opdEncounterComments = null;
        patientSearchTab = 0;
        errorMessage = "";
        comment = null;
        token = null;
    }

    private void clearBillItem() {
        replaceableStocks = null;
        billItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
        editingQty = null;
        errorMessage = "";
    }

    public boolean CheckDateAfterOneMonthCurrentDateTime(Date date) {
        Calendar calDateOfExpiry = Calendar.getInstance();
        calDateOfExpiry.setTime(CommonFunctionsController.getEndOfDay(date));
        Calendar cal = Calendar.getInstance();
        cal.setTime(CommonFunctionsController.getEndOfDay(new Date()));
        cal.add(Calendar.DATE, 31);
        if (cal.getTimeInMillis() <= calDateOfExpiry.getTimeInMillis()) {
            return false;
        } else {
            return true;
        }
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

    public BillItem getEditingBillItem() {
        return editingBillItem;
    }

    public void setEditingBillItem(BillItem editingBillItem) {
        this.editingBillItem = editingBillItem;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    @Override
    public Patient getPatient() {
        if (patient == null) {
            patient = new Patient();
            patientDetailsEditable = true;
        }
        return patient;
    }

    @Override
    public void setPatient(Patient patient) {
        this.patient = patient;
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

    public PreBill getPreBill() {
        if (preBill == null) {
            preBill = new PreBill();
            preBill.setBillType(BillType.PharmacyPre);
            preBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
            //   preBill.setPaymentScheme(getPaymentSchemeController().getItems().get(0));
        }
        return preBill;
    }

    public void setPreBill(PreBill preBill) {
        this.preBill = preBill;
    }

    public Bill getSaleBill() {
        if (saleBill == null) {
            saleBill = new BilledBill();
            //   saleBill.setBillType(BillType.PharmacySale);
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

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }

    public Bill getPrintBill() {
        return printBill;
    }

    public void setPrintBill(Bill printBill) {
        this.printBill = printBill;
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return PaymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController PaymentSchemeController) {
        this.PaymentSchemeController = PaymentSchemeController;
    }

    public PaymentScheme getPaymentScheme() {
        //  //System.err.println("GEtting Paymen");
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        //     //System.err.println("Setting Pay");
        this.paymentScheme = paymentScheme;
    }

    public StockHistoryFacade getStockHistoryFacade() {
        return stockHistoryFacade;
    }

    public void setStockHistoryFacade(StockHistoryFacade stockHistoryFacade) {
        this.stockHistoryFacade = stockHistoryFacade;
    }

    public UserStockContainer getUserStockContainer() {
        if (userStockContainer == null) {
            userStockContainer = new UserStockContainer();
        }
        return userStockContainer;
    }

    public void setUserStockContainer(UserStockContainer userStockContainer) {
        this.userStockContainer = userStockContainer;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public StaffBean getStaffBean() {
        return staffBean;
    }

    public void setStaffBean(StaffBean staffBean) {
        this.staffBean = staffBean;
    }

    public boolean isFromOpdEncounter() {
        return fromOpdEncounter;
    }

    public void setFromOpdEncounter(boolean fromOpdEncounter) {
        this.fromOpdEncounter = fromOpdEncounter;
    }

    public String getOpdEncounterComments() {
        return opdEncounterComments;
    }

    public void setOpdEncounterComments(String opdEncounterComments) {
        this.opdEncounterComments = opdEncounterComments;
    }

    public int getPatientSearchTab() {
        return patientSearchTab;
    }

    public void setPatientSearchTab(int patientSearchTab) {
        this.patientSearchTab = patientSearchTab;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Stock getReplacableStock() {
        return replacableStock;
    }

    public void setReplacableStock(Stock replacableStock) {
        this.replacableStock = replacableStock;
    }

    public Item getSelectedAvailableAmp() {
        return selectedAvailableAmp;
    }

    public void setSelectedAvailableAmp(Item selectedAvailableAmp) {
        this.selectedAvailableAmp = selectedAvailableAmp;
    }

    public UserStockContainerFacade getUserStockContainerFacade() {
        return userStockContainerFacade;
    }

    public void setUserStockContainerFacade(UserStockContainerFacade userStockContainerFacade) {
        this.userStockContainerFacade = userStockContainerFacade;
    }

    public UserStockFacade getUserStockFacade() {
        return userStockFacade;
    }

    public void setUserStockFacade(UserStockFacade userStockFacade) {
        this.userStockFacade = userStockFacade;
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

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    @Override
    public boolean isPatientDetailsEditable() {
        return patientDetailsEditable;
    }

    @Override
    public void setPatientDetailsEditable(boolean patientDetailsEditable) {
        this.patientDetailsEditable = patientDetailsEditable;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public List<BillItem> getSelectedBillItems() {
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
    }

}
