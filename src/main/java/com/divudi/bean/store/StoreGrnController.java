/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.store;

import com.divudi.bean.common.ApplicationController;
import com.divudi.bean.common.SessionController;

import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.ejb.BillNumberGenerator;

import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.pharmacy.ItemBatch;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.AmpFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.CategoryFacade;
import com.divudi.facade.ItemBatchFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.StockFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillTypeAtomic;
import com.divudi.entity.Payment;
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
import org.primefaces.event.RowEditEvent;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class StoreGrnController implements Serializable {

    @Inject
    private SessionController sessionController;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private CategoryFacade categoryFacade;
    @EJB
    private ItemBatchFacade itemBatchFacade;
    @EJB
    StoreBean storeBean;
    @EJB
    private AmpFacade ampFacade;
    @EJB
    PaymentFacade paymentFacade;

    private CommonFunctions commonFunctions;
    @Inject
    StoreCalculation storeCalculation;
    @Inject
    ApplicationController applicationController;
    /////////////////
    private Institution dealor;
    private Bill approveBill;
    private Bill grnBill;
    //   private Double cashPaid;
    private Date fromDate;
    private Date toDate;
    private boolean printPreview;
    //////////////
    //private List<PharmacyItemData> pharmacyItems;
    private List<Bill> pos;
    private List<Bill> grns;
    private List<Bill> filteredValue;
    private List<BillItem> billItems;
    List<BillItem> billExpenses;
    private List<BillItem> selectedBillItems;
    private SearchKeyword searchKeyword;
    private List<Bill> bills;
    BillItem currentExpense;
    BillItem parentBillItem;

    public void removeItem(BillItem bi) {
        getBillItems().remove(bi.getSearialNo());

        calTotal();

    }

    public void removeSelected() {
        //  //System.err.println("1");
        if (selectedBillItems == null) {
            //   //System.err.println("2");
            return;
        }

        //   //System.err.println("3");
        for (BillItem b : selectedBillItems) {
            //  //System.err.println("4");
            getBillItems().remove(b.getSearialNo());
            calTotal();
        }

        selectedBillItems = null;
    }

    public void clearList() {
        //   pharmacyItems = null;
        pos = null;
        filteredValue = null;
        //  billItems = null;
        grns = null;
        currentExpense = null;
        billExpenses = null;
        billItems = null;
          System.out.println("clear");

    }

    public void batchListener() {
        batchListener(getCurrentBillItem());
    }

    public void batchListener(BillItem pid) {

        if (pid.getPharmaceuticalBillItem().getDoe() == null) {
            pid.getPharmaceuticalBillItem().setDoe(getApplicationController().getStoresExpiery());
        }

        Date date = pid.getPharmaceuticalBillItem().getDoe();
        DateFormat df = new SimpleDateFormat("ddMMyyyy");
        String reportDate = df.format(date);
        pid.getPharmaceuticalBillItem().setStringValue(reportDate);
        onEdit(pid);
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public String errorCheck(Bill b, List<BillItem> billItems) {
        String msg = "";

//        for(BillItem bi : billItems){
//            if(bi.getPharmaceuticalBillItem().getPurchaseRate() > bi.getPharmaceuticalBillItem().getRetailRate())
//           msg = "Check Purchase Rate and Retail Rate"; 
//        }
        if (b.getInvoiceNumber() == null || "".equals(b.getInvoiceNumber().trim())) {
            msg = "Please Fill invoice number";
        }

        if (b.getPaymentMethod() != null && b.getPaymentMethod() == PaymentMethod.Cheque) {
            if (b.getBank().getId() == null || b.getChequeRefNo() == null) {
                msg = "Please select Cheque Number and Bank";
            }
        }

        if (b.getPaymentMethod() != null && b.getPaymentMethod() == PaymentMethod.Slip) {
            if (b.getBank().getId() == null || b.getComments() == null) {
                msg = "Please Fill Memo and Bank";
            }
        }

        if (billItems.isEmpty()) {
            msg = "There is no Item to receive";
        }

        return msg;
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

        String msg = errorCheck(getGrnBill(), billItems);

        if (!msg.isEmpty()) {
            JsfUtil.addErrorMessage(msg);
            return;
        }

        for (BillItem bi : billItems) {
            ////// // System.out.println("bi.getPharmaceuticalBillItem().getPurchaseRate() = " + bi.getPharmaceuticalBillItem().getPurchaseRate());
            ////// // System.out.println("bi.getPharmaceuticalBillItem().getRetailRate() = " + bi.getPharmaceuticalBillItem().getRetailRate());
            bi.getPharmaceuticalBillItem().setRetailRate(bi.getPharmaceuticalBillItem().getPurchaseRate());
            ////// // System.out.println("bi.getPharmaceuticalBillItem().getPurchaseRate() = " + bi.getPharmaceuticalBillItem().getPurchaseRate());
            ////// // System.out.println("bi.getPharmaceuticalBillItem().getRetailRate() = " + bi.getPharmaceuticalBillItem().getRetailRate());
        }

        storeCalculation.calSaleFreeValue(getGrnBill());
        if (getGrnBill().getInvoiceDate() == null) {
            getGrnBill().setInvoiceDate(getApproveBill().getCreatedAt());
        }

        saveBill();
        calTotal();

        //Restting IDs
        for (BillItem i : getBillItems()) {
            i.setId(null);
        }
        for (BillItem i : getBillItems()) {
            //// // System.out.println("bi = " + i.getSearialNo());
            //// // System.out.println("i.getTmpQty() = " + i.getTmpQty());
            //// // System.out.println("i.getQty() = " + i.getQty());
            if (i.getTmpQty() == 0.0) {
                continue;
            }
//            createSerialNumber(i);
            PharmaceuticalBillItem ph = i.getPharmaceuticalBillItem();
            ph.setDoe(applicationController.getStoresExpiery());
            i.setPharmaceuticalBillItem(null);
            //// // System.out.println("1");
            i.setCreatedAt(new Date());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getGrnBill());
            //// // System.out.println("2");
            saveParentBillItem(i.getParentBillItem());
            //// // System.out.println("3");
            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }
            //// // System.out.println("4");
            getPharmaceuticalBillItemFacade().create(ph);
            //// // System.out.println("5");
            i.setPharmaceuticalBillItem(ph);
            getBillItemFacade().edit(i);
            //// // System.out.println("6");
            //     updatePoItemQty(i);
            //System.err.println("1 " + i);
            ItemBatch itemBatch = storeCalculation.saveItemBatch(i);
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());
            //// // System.out.println("7");
            double addingQty = i.getPharmaceuticalBillItem().getQtyInUnit() + i.getPharmaceuticalBillItem().getFreeQtyInUnit();

            i.getPharmaceuticalBillItem().setItemBatch(itemBatch);
            //// // System.out.println("8");
            Stock stock = getStoreBean().addToStock(
                    i.getPharmaceuticalBillItem(),
                    Math.abs(addingQty),
                    getSessionController().getDepartment());
            //// // System.out.println("9");
            i.getPharmaceuticalBillItem().setStock(stock);
            //// // System.out.println("10");
            getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
            storeCalculation.editBillItem(i.getPharmaceuticalBillItem(), getSessionController().getLoggedUser());
            for (BillItem bi : getBillItems()) {
                if (bi.getParentBillItem() != null) {
                    if (bi.getParentBillItem().getItem().equals(i.getItem()) && bi.getParentBillItem().getSearialNo() == i.getSearialNo()) {
                        bi.setParentBillItem(i);
                    }
                }
            }
            getGrnBill().getBillItems().add(i);
        }

        //Save Parent Stock
        for (BillItem i : getBillItems()) {

            if (i.getPharmaceuticalBillItem().getRetailRate() < i.getPharmaceuticalBillItem().getPurchaseRate()) {
                JsfUtil.addErrorMessage("Less Sale rate");
                return;
            }

            if (i.getParentBillItem() != null) {

                if (i.getParentBillItem().getPharmaceuticalBillItem().getPurchaseRate() < i.getParentBillItem().getPharmaceuticalBillItem().getRetailRate()) {
                    JsfUtil.addErrorMessage("Less Sale rate");
                    return;
                }

                i.getParentBillItem().getPharmaceuticalBillItem().getStock().getChildStocks().add(i.getPharmaceuticalBillItem().getStock());
                i.getPharmaceuticalBillItem().getStock().setParentStock(i.getParentBillItem().getPharmaceuticalBillItem().getStock());
                stockFacade.edit(i.getParentBillItem().getPharmaceuticalBillItem().getStock());
                stockFacade.edit(i.getPharmaceuticalBillItem().getStock());
            }
        }

        for (BillItem i : getBillExpenses()) {
            i.setExpenseBill(getGrnBill());
            getBillItemFacade().create(i);
            getGrnBill().getBillExpenses().add(i);
        }
        
        Payment p = createPayment(getGrnBill(), getGrnBill().getPaymentMethod());

        getGrnBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.StoreGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN));
        getGrnBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.StoreGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN));

        getGrnBill().setToInstitution(getApproveBill().getFromInstitution());
        getGrnBill().setToDepartment(getApproveBill().getFromDepartment());

        getGrnBill().setInstitution(getSessionController().getInstitution());
        getGrnBill().setDepartment(getSessionController().getDepartment());

        getGrnBill().setCreater(getSessionController().getLoggedUser());
        getGrnBill().setCreatedAt(Calendar.getInstance().getTime());

        ////// // System.out.println("getGrnBill().getBillExpenses() = " + getGrnBill().getBillExpenses());
        ////// // System.out.println("getBillExpenses() = " + getBillExpenses());
        if (getGrnBill().getBillExpenses() == null || getGrnBill().getBillExpenses().isEmpty()) {
            ////// // System.out.println("Adding ");
            getGrnBill().setBillExpenses(getBillExpenses());
        }

        ////// // System.out.println("getGrnBill().getBillExpenses() = " + getGrnBill().getBillExpenses());
        ////// // System.out.println("getBillExpenses() = " + getBillExpenses());
        getBillFacade().edit(getGrnBill());

        //  getPharmacyBillBean().editBill(, , getSessionController());
        printPreview = true;

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
            paymentFacade.create(p);
        }

    }

    public void purchaseRateUpdateListner(BillItem bi) {
        bi.getPharmaceuticalBillItem().setRetailRate(bi.getPharmaceuticalBillItem().getPurchaseRate());
        ////// // System.out.println("Updated Purchase Rate Update Listner");
    }

    @EJB
    StockFacade stockFacade;

    public String viewPoList() {
        clearList();

        return "store_purchase_order_list_for_recieve";
    }

    public StoreGrnController() {
    }

    public Institution getDealor() {
        return dealor;
    }

    public void setDealor(Institution dealor) {
        this.dealor = dealor;
    }

    private String txtSearch;

    public void makeListNull() {

//        pharmacyItems = null;
        pos = null;
        grns = null;
        filteredValue = null;
        bills = null;
        makeNull();
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Bill getApproveBill() {
        if (approveBill == null) {
            approveBill = new BilledBill();
        }
        return approveBill;
    }

    public void saveBill() {
        getGrnBill().setPaymentMethod(getApproveBill().getPaymentMethod());
        getGrnBill().setReferenceBill(getApproveBill());
        getGrnBill().setFromInstitution(getApproveBill().getToInstitution());
        getGrnBill().setDepartment(getSessionController().getDepartment());
        getGrnBill().setInstitution(getSessionController().getInstitution());
        getGrnBill().setBillTypeAtomic(BillTypeAtomic.STORE_GRN);
        //   getGrnBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyGrnBill, BillNumberSuffix.GRN));
        //   getGrnBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getGrnBill(), BillType.PharmacyGrnBill, BillNumberSuffix.GRN));

        getGrnBill().calGrnNetTotal();
        if (getGrnBill().getId() == null) {
            getBillFacade().create(getGrnBill());
        } else {
            getBillFacade().edit(getGrnBill());
        }
    }

    private void createBillItems(PharmaceuticalBillItem i, double qty, double freeQty) {
        BillItem bi = new BillItem();
        bi.setSearialNo(getBillItems().size());
        bi.setItem(i.getBillItem().getItem());
        bi.setReferanceBillItem(i.getBillItem());
        bi.setQty(qty);
        bi.setTmpQty(qty);
        bi.setTmpFreeQty(freeQty);
        //Set Suggession
//                bi.setTmpSuggession(getPharmacyCalculation().getSuggessionOnly(bi.getItem()));

        PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
        ph.setBillItem(bi);
        double tmpQty = bi.getQty();
        double tmpFreeQty = bi.getTmpFreeQty();
        ph.setQtyInUnit((double) tmpQty);
        ph.setFreeQtyInUnit((double) tmpFreeQty);
        ph.setPurchaseRate(i.getPurchaseRate());
        ph.setRetailRate(i.getRetailRate());
        ph.setLastPurchaseRate(getStoreBean().getLastPurchaseRate(bi.getItem(), getSessionController().getDepartment()));

        bi.setPharmaceuticalBillItem(ph);

        addBillItem(bi);
        createSerialNumber(bi);
        //  getBillItems().r
        ////// // System.out.println("getBillItems() = " + getBillItems());

    }
//
//    public void generateBillComponent() {
//
//        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getApproveBill())) {
//            System.err.println("Qty Unit : " + i.getQtyInUnit());
////            System.err.println("Remaining Qty : " + i.getRemainingQty());
//            double remains = storeCalculation.calQtyInTwoSql(i);
//            System.err.println("Tot GRN Qty : " + remains);
////            System.err.println("QTY : " + i.getQtyInUnit());
//            if (i.getQtyInUnit() >= remains && (i.getQtyInUnit() - remains) != 0) {
//                if (i.getBillItem().getItem().getDepartmentType() == DepartmentType.Inventry) {
//                    for (int index = (int) remains; index > 0; index++) {
//                        createBillItems(i, 1);
//                    }
//                } else {
//                    createBillItems(i, (i.getQtyInUnit() - remains));
//                }
//            }
//
//        }
//    }

    public void generateBillComponent() {
        List<PharmaceuticalBillItem> tmpPharmaceuticalBillItemList = getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getApproveBill());

        for (PharmaceuticalBillItem i : tmpPharmaceuticalBillItemList) {
//            System.out.println("i = " + i.getBillItem().getItem().getName());
            double remains = i.getQtyInUnit() - storeCalculation.calQtyInTwoSql(i);
            double remainFreeQty = i.getFreeQty() - storeCalculation.calFreeQtyInTwoSql(i);
            if (remains > 0 || remainFreeQty > 0) {
                createBillItems(i, remains, remainFreeQty);

            }

        }
    }
    
    public String navigateToRecieve() {
        clearList();
        createGrn();
        return "/store/store_grn?faces-redirect=true";
    }

    public void createSerialNumber() {
        createSerialNumber(getCurrentBillItem());
    }

    public void purchaseRateListener(PharmaceuticalBillItem pharmaceuticalBillItem) {
        pharmaceuticalBillItem.setRetailRate(pharmaceuticalBillItem.getPurchaseRate());
    }

    public void createSerialNumber(BillItem billItem) {
        if (billItem == null) {
            return;
        }
        ////// // System.out.println("In");
        long b = billNumberBean.inventoryItemSerialNumberGeneraterForYear(getSessionController().getLoggedUser().getInstitution(), billItem.getItem());
//        long b = billNumberBean.inventoryItemSerialNumberGenerater(getSessionController().getLoggedUser().getInstitution(), billItem.getItem());
//        b = b + 1;
//        long b = billNumberBean.inventoryItemSerialNumberGenerater(getSessionController().getLoggedUser().getInstitution(), billItem.getItem());
//        b = b + 1;
        for (BillItem bi : getBillItems()) {
            if (bi.getItem().equals(billItem.getItem())) {
                b++;
            }
        }
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
        Calendar c = Calendar.getInstance();

        int year = c.get(Calendar.YEAR);
        code += year;
        code += "/";

        int month = c.get(Calendar.MONTH) + 1;
        code += month;
        code += "/";
        code += b;

        ////// // System.out.println("code = " + code);
        billItem.getPharmaceuticalBillItem().setCode(code);
    }

    public void addBillItem(BillItem billItem) {

//        if (billItem.getPharmaceuticalBillItem().getPurchaseRate() > billItem.getPharmaceuticalBillItem().getRetailRate()) {
//            JsfUtil.addErrorMessage("Please enter Sale Rate Should be Over Purchase Rate");
//            return;
//        }
        if (billItem.getPharmaceuticalBillItem().getRetailRate() <= 0) {
            if (billItem.getItem().getCategory() == null) {
                JsfUtil.addErrorMessage("Please Select Item Category for" + billItem.getItem().getName());
            }
            try {
                billItem.getPharmaceuticalBillItem().setRetailRate(billItem.getPharmaceuticalBillItem().getPurchaseRate() * (1 + (.01 * billItem.getItem().getCategory().getSaleMargin())));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (billItem.getPharmaceuticalBillItem().getDoe() == null) {
            billItem.getPharmaceuticalBillItem().setDoe(getApplicationController().getStoresExpiery());
        }

        ////// // System.out.println("****Inventory Code****" + billItem.getPharmaceuticalBillItem().getCode());
        billItem.setSearialNo(getBillItems().size());
        // billItem.setId(billItem.getSearialNoInteger().longValue());
        // billItem.setId(billItem.getSearialNoInteger().longValue());
        // billItem.setId(billItem.getSearialNoInteger().longValue());
        // billItem.setId(billItem.getSearialNoInteger().longValue());

//        billItem.setSearialNo(getBillItems().size() + 1);        
        getBillItems().add(billItem);
//
//        getBillItemController().setItems(getBillItems());
//      
//        getBillItemController().setItems(getBillItems());
    }

    public void createGrn() {
        getGrnBill().setPaymentMethod(getApproveBill().getPaymentMethod());
        getGrnBill().setFromInstitution(getApproveBill().getToInstitution());
        getGrnBill().setReferenceInstitution(getSessionController().getLoggedUser().getInstitution());
//        System.out.println("in");
        generateBillComponent();
        calTotal();
        ////// // System.out.println("out");
    }

    private double getRetailPrice(BillItem billItem) {
        String sql = "select (p.retailRate) from PharmaceuticalBillItem p where p.billItem=:b";
        HashMap hm = new HashMap();
        hm.put("b", billItem.getReferanceBillItem());
        return (double) getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public void setBatch(BillItem pid) {
        if (pid.getPharmaceuticalBillItem().getDoe() == null) {
            return;
        }

        if (pid.getPharmaceuticalBillItem().getDoe() != null) {
            if (pid.getPharmaceuticalBillItem().getDoe().getTime() < Calendar.getInstance().getTimeInMillis()) {
                pid.getPharmaceuticalBillItem().setStringValue(null);
                return;
                //    return;
            }
        }

        if (pid.getPharmaceuticalBillItem().getStringValue().trim().equals("")) {
            Date date = pid.getPharmaceuticalBillItem().getDoe();
            DateFormat df = new SimpleDateFormat("ddMMyyyy");
            String reportDate = df.format(date);
// Print what date is today!
            //       //System.err.println("Report Date: " + reportDate);
            pid.getPharmaceuticalBillItem().setStringValue(reportDate);
        }
    }

    public void onEdit(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();
        onEdit(tmp);
        //   onEditPurchaseRate(tmp);
        setBatch(tmp);
    }

    public void onEdit(BillItem tmp) {
        double remains = storeCalculation.getRemainingQty(tmp.getPharmaceuticalBillItem());

//        System.err.println("1 " + tmp.getTmpQty());
//        System.err.println("2 " + tmp.getQty());
//        System.err.println("3 " + tmp.getPharmaceuticalBillItem().getQty());
//        System.err.println("4 " + tmp.getPharmaceuticalBillItem().getQtyInUnit());
        if (remains < tmp.getPharmaceuticalBillItem().getQtyInUnit()) {
            tmp.setTmpQty(remains);
            JsfUtil.addErrorMessage("You cant Change Qty than Remaining qty");
        }

        if (tmp.getPharmaceuticalBillItem().getPurchaseRate() > tmp.getPharmaceuticalBillItem().getRetailRate()) {
            tmp.getPharmaceuticalBillItem().setRetailRate(getRetailPrice(tmp.getPharmaceuticalBillItem().getBillItem()));
            JsfUtil.addErrorMessage("You cant set retail price below purchase rate");
        }

//        if (tmp.getPharmaceuticalBillItem().getDoe() != null) {
//            if (tmp.getPharmaceuticalBillItem().getDoe().getTime() < Calendar.getInstance().getTimeInMillis()) {
//                tmp.getPharmaceuticalBillItem().setDoe(null);
//                JsfUtil.addErrorMessage("Check Date of Expiry");
//                //    return;
//            }
//        }
        setBatch(tmp);
        calTotal();
    }

    public void onEditPurchaseRate(BillItem tmp) {

        double retail = tmp.getPharmaceuticalBillItem().getPurchaseRate() + (tmp.getPharmaceuticalBillItem().getPurchaseRate() * (getStoreBean().getMaximumRetailPriceChange() / 100));
        tmp.getPharmaceuticalBillItem().setRetailRate((double) retail);

    }

//    private List<Item> getSuggession(Item item) {
//        List<Item> suggessions = new ArrayList<>();
//
//        if (item instanceof Amp) {
//            suggessions = getPharmacyBillBean().findItem((Amp) item, suggessions);
//        } else if (item instanceof Ampp) {
//            suggessions = getPharmacyBillBean().findItem((Ampp) item, suggessions);
//        } else if (item instanceof Vmp) {
//            suggessions = getPharmacyBillBean().findItem((Vmp) item, suggessions);
//        } else if (item instanceof Vmpp) {
//            suggessions = getPharmacyBillBean().findItem((Vmpp) item, suggessions);
//        }
//
//        return suggessions;
//    }
    public void addExpense() {
        if (getGrnBill().getId() == null) {
            getBillFacade().create(getGrnBill());
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

    public void removeItemEx(BillItem bi) {

        if (bi == null) {
            JsfUtil.addErrorMessage("No Item to Remove");
            return;
        }

        getBillExpenses().remove(bi.getSearialNo());

        calTotal();

    }

//    private void calGrossTotal() {
//        double tmp = 0.0;
//        double exp = 0.0;
//        int serialNo = 0;
//        for (BillItem p : getBillItems()) {
//            tmp += p.getPharmaceuticalBillItem().getPurchaseRate() * p.getPharmaceuticalBillItem().getQty();
//            p.setSearialNo(serialNo++);
//        }
//
//        for (BillItem e : getBillExpenses()) {
//            double nv = e.getNetRate() * e.getQty();
//            e.setNetValue(0 - nv);
//            ////// // System.out.println("nv = " + nv);
//            exp += e.getNetValue();
//            ////// // System.out.println("e.getNetValue() = " + e.getNetValue());
//            ////// // System.out.println("exp = " + exp);
//        }
//
//        getGrnBill().setExpenseTotal(exp);
//
//        getGrnBill().setTotal(0 - tmp);
//        getGrnBill().setNetTotal(getGrnBill().getTotal() + exp);
//
//        ChangeDiscountLitener();
//
//    }
    public void addTest() {
        ////// // System.out.println("this = " + this);
    }

    public void addTest1() {
        ChangeDiscountLitener();
    }

    public void ChangeDiscountLitener() {
        getGrnBill().setNetTotal(getGrnBill().getNetTotal() + getGrnBill().getDiscount());
    }

    public void ChangeDiscountLitenerNew() {
        ////// // System.out.println("ChangeDiscountLitener");
        //Example - Case 1
        //Net Total=  -99.50
        //Cash Paid= 100
        //Adjusted Total= 100 - 99.50 = 0.50
        //Net Total = -99.50 -0.50 = -100.00
        double nt;
        double adt;
        double pt;
        double dt;

        nt = getGrnBill().getNetTotal() + getGrnBill().getDiscount();
        dt = getGrnBill().getDiscount();
        ////// // System.out.println("nt = " + nt);
        ////// // System.out.println("dt = " + dt);
        if (getGrnBill().getCashPaid() == null || getGrnBill().getCashPaid().equals(0.0)) {
            adt = 0.0;
            pt = nt;
        } else {
            pt = getGrnBill().getCashPaid();
            adt = pt - Math.abs(nt);
        }

        ////// // System.out.println("nt = " + nt);
        ////// // System.out.println("adt = " + adt);
        ////// // System.out.println("pt = " + pt);
        ////// // System.out.println("dt = " + dt);
        getGrnBill().setNetTotal(nt + dt - adt);

        ////// // System.out.println("dt = " + getGrnBill().getNetTotal());
    }

//    public double getNetTotal() {
//
//        double tmp = getGrnBill().getTotal() + getGrnBill().getTax() - getGrnBill().getDiscount();
//        getGrnBill().setNetTotal(tmp);
//
//        return tmp;
//    }
    public void makeNull() {
        grnBill = null;
        dealor = null;
        pos = null;
        printPreview = false;
        billItems = null;
        parentBillItem = null;
        currentBillItem = null;
        currentExpense = null;
    }

    public void setApproveBill(Bill approveBill) {
        this.approveBill = approveBill;
        makeNull();
        createGrn();
        ////// // System.out.println("set Approvebill.");
    }

    public Bill getGrnBill() {
        if (grnBill == null) {
            grnBill = new BilledBill();
            grnBill.setBillType(BillType.StoreGrnBill);
            grnBill.setBillTypeAtomic(BillTypeAtomic.STORE_GRN);
        }
        return grnBill;
    }

    public void setGrnBill(Bill grnBill) {
        this.grnBill = grnBill;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
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

    public CategoryFacade getCategoryFacade() {
        return categoryFacade;
    }

    public void setCategoryFacade(CategoryFacade categoryFacade) {
        this.categoryFacade = categoryFacade;
    }

    public ItemBatchFacade getItemBatchFacade() {
        return itemBatchFacade;
    }

    public void setItemBatchFacade(ItemBatchFacade itemBatchFacade) {
        this.itemBatchFacade = itemBatchFacade;
    }

    public StoreBean getStoreBean() {
        return storeBean;
    }

    public void setStoreBean(StoreBean storeBean) {
        this.storeBean = storeBean;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    public AmpFacade getAmpFacade() {
        return ampFacade;
    }

    public void setAmpFacade(AmpFacade ampFacade) {
        this.ampFacade = ampFacade;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public List<Bill> getFilteredValue() {
        return filteredValue;
    }

    public void setFilteredValue(List<Bill> filteredValue) {
        this.filteredValue = filteredValue;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public List<Bill> getPos() {
        return pos;
    }

    public void setPos(List<Bill> pos) {
        this.pos = pos;
    }

//    public List<BillItem> getBillItems() {
//        if (billItems == null) {
//            billItems = new ArrayList<>();
//        }
//        return billItems;
//    }
//
//    public void setBillItems(List<BillItem> billItems) {
//        this.billItems = billItems;
//    }
    public String getTxtSearch() {
        return txtSearch;
    }

    public void setTxtSearch(String txtSearch) {
        this.txtSearch = txtSearch;
    }

    public List<BillItem> getBillItems() {
        if (billItems == null) {
            billItems = new ArrayList<>();
            // serialNo = 0;
        }
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public List<BillItem> getSelectedBillItems() {
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
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

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public List<Bill> getGrns() {
        return grns;
    }

    public void setGrns(List<Bill> grns) {
        this.grns = grns;
    }

    public ApplicationController getApplicationController() {
        return applicationController;
    }

    public void setApplicationController(ApplicationController applicationController) {
        this.applicationController = applicationController;
    }

    public List<BillItem> getBillExpenses() {
        if (billExpenses == null) {
            billExpenses = new ArrayList<>();
        }
        return billExpenses;
    }

    public void addChildItemListener(BillItem bi) {
        parentBillItem = new BillItem();
        currentBillItem = new BillItem();
        currentBillItem.setPharmaceuticalBillItem(new PharmaceuticalBillItem());
        parentBillItem.copy(bi);
    }

    public void addDetailItemListener(BillItem bi) {
        parentBillItem = new BillItem();
        currentBillItem = new BillItem();
        currentBillItem.copy(bi);
        parentBillItem.copy(bi);
        currentBillItem.setPharmaceuticalBillItem(new PharmaceuticalBillItem());
        currentBillItem.getPharmaceuticalBillItem().copy(bi.getPharmaceuticalBillItem());
        currentBillItem.getPharmaceuticalBillItem().setCode(bi.getPharmaceuticalBillItem().getCode());
        //// // System.out.println("bi.getSearialNo() = " + bi.getSearialNo());
        //// // System.out.println("currentBillItem.getSearialNo() = " + currentBillItem.getSearialNo());
    }

    public void updateItemDetail() {
        //// // System.out.println("currentBillItem.getSearialNo() = " + currentBillItem.getSearialNo());
        //// // System.out.println("currentBillItem.getTmpQty() = " + currentBillItem.getTmpQty());
        //// // System.out.println("currentBillItem.getQty() = " + currentBillItem.getQty());
        for (BillItem bi : getBillItems()) {
            if (bi.getSearialNo() == parentBillItem.getSearialNo()) {
//                bi = getCurrentBillItem();
                bi.getPharmaceuticalBillItem().setMake(getCurrentBillItem().getPharmaceuticalBillItem().getMake());
                bi.getPharmaceuticalBillItem().setModel(getCurrentBillItem().getPharmaceuticalBillItem().getModel());
                bi.getPharmaceuticalBillItem().setCode(getCurrentBillItem().getPharmaceuticalBillItem().getCode());
                bi.getPharmaceuticalBillItem().setDescription(getCurrentBillItem().getPharmaceuticalBillItem().getDescription());
                bi.getPharmaceuticalBillItem().setBarcode(getCurrentBillItem().getPharmaceuticalBillItem().getBarcode());
                bi.getPharmaceuticalBillItem().setSerialNo(getCurrentBillItem().getPharmaceuticalBillItem().getSerialNo());
                bi.getPharmaceuticalBillItem().setRegistrationNo(getCurrentBillItem().getPharmaceuticalBillItem().getRegistrationNo());
                bi.getPharmaceuticalBillItem().setChassisNo(getCurrentBillItem().getPharmaceuticalBillItem().getChassisNo());
                bi.getPharmaceuticalBillItem().setEngineNo(getCurrentBillItem().getPharmaceuticalBillItem().getEngineNo());
                bi.getPharmaceuticalBillItem().setColour(getCurrentBillItem().getPharmaceuticalBillItem().getColour());
                bi.getPharmaceuticalBillItem().setWarrentyCertificateNumber(getCurrentBillItem().getPharmaceuticalBillItem().getWarrentyCertificateNumber());
                bi.getPharmaceuticalBillItem().setWarrentyDuration(getCurrentBillItem().getPharmaceuticalBillItem().getWarrentyDuration());
                bi.getPharmaceuticalBillItem().setDeprecitionRate(getCurrentBillItem().getPharmaceuticalBillItem().getDeprecitionRate());
                bi.getPharmaceuticalBillItem().setManufacturer(getCurrentBillItem().getPharmaceuticalBillItem().getManufacturer());
                bi.getPharmaceuticalBillItem().setOtherNotes(getCurrentBillItem().getPharmaceuticalBillItem().getOtherNotes());
            }
        }

        JsfUtil.addSuccessMessage("Updated.");

    }

    public void checkItemDetail() {
        for (BillItem bi : getBillItems()) {
            try {
            } catch (Exception e) {
            }
        }
    }

    public void setBillExpenses(List<BillItem> billExpenses) {
        this.billExpenses = billExpenses;
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

    public void addItem() {
        if (getCurrentBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("Please Select Item");
            return;
        }
        if (getCurrentBillItem().getItem().getCategory() == null) {
            JsfUtil.addErrorMessage("Please Select Category");
            return;
        }

//        if (getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate() <= 0 && getParentBillItem() == null) {
//            System.err.println("33");
//            JsfUtil.addErrorMessage("Please enter Purchase Rate");
//            return;
//        }
//
//        if (getCurrentBillItem().getPharmaceuticalBillItem().getRetailRate() == 0) {
//            JsfUtil.addErrorMessage("Please enter Retail Rate");
//            return;
//        }
//
//        if (getCurrentBillItem().getPharmaceuticalBillItem().getRetailRate() < getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate()) {
//            JsfUtil.addErrorMessage("Please check Retail Rate");
//            return;
//        }
        if (getCurrentBillItem().getPharmaceuticalBillItem().getQty() <= 0) {
            JsfUtil.addErrorMessage("Please enter Purchase QTY");
            return;
        }

        if (getCurrentBillItem().getItem().getDepartmentType() == DepartmentType.Inventry) {
//            if (getCurrentBillItem().getPharmaceuticalBillItem().getQty() != 1) {
//                JsfUtil.addErrorMessage("Please Qty must be 1 for Asset");
//                return;
//            }

            int j = (int) getCurrentBillItem().getPharmaceuticalBillItem().getQty();

            for (int i = 0; i < j; i++) {
                BillItem bi = new BillItem();
                bi.copy(getCurrentBillItem());
                bi.setPharmaceuticalBillItem(new PharmaceuticalBillItem());
                bi.getPharmaceuticalBillItem().setBillItem(bi);
                bi.getPharmaceuticalBillItem().copy(getCurrentBillItem().getPharmaceuticalBillItem());
                ////// // System.out.println("Item Number = " + i);
                bi.getPharmaceuticalBillItem().setQty(1);
                bi.setQty(1.0);
//                //// // System.out.println("bi.getPharmaceuticalBillItem().getQty() = " + bi.getPharmaceuticalBillItem().getQty());
//                //// // System.out.println("bi.getPharmaceuticalBillItem().getQty() = " + bi.getPharmaceuticalBillItem().getModel());
//                //// // System.out.println("bi.getPharmaceuticalBillItem().getQty() = " + bi.getPharmaceuticalBillItem().getDescription());
//                //// // System.out.println("getCurrentBillItem().getPharmaceuticalBillItem().getQty() = " + getCurrentBillItem().getPharmaceuticalBillItem().getQty());
//                //// // System.out.println("getCurrentBillItem().getPharmaceuticalBillItem().getQty() = " + getCurrentBillItem().getPharmaceuticalBillItem().getModel());
//                //// // System.out.println("getCurrentBillItem().getPharmaceuticalBillItem().getQty() = " + getCurrentBillItem().getPharmaceuticalBillItem().getDescription());
//                //// // System.out.println("getCurrentBillItem().getPharmaceuticalBillItem().getMake().getName() = " + getCurrentBillItem().getPharmaceuticalBillItem().getMake().getName());
//                //// // System.out.println("getCurrentBillItem().getPharmaceuticalBillItem().getCode() = " + getCurrentBillItem().getPharmaceuticalBillItem().getCode());
//                //// // System.out.println("getCurrentBillItem().getPharmaceuticalBillItem().getBarcode() = " + getCurrentBillItem().getPharmaceuticalBillItem().getBarcode());
                ////// // System.out.println("****Inventory Code 1****" + bi.getPharmaceuticalBillItem().getCode() + "*******");
                createSerialNumber(bi);
                ////// // System.out.println("****Inventory Code 2****" + bi.getPharmaceuticalBillItem().getCode() + "*******");
                //        billItem.setParentBillItem(getParentBillItem());

                bi.setParentBillItem(new BillItem());
                bi.getParentBillItem().copy(getParentBillItem());

                addBillItem(bi);
                calTotal();
            }
            currentBillItem = new BillItem();
            currentBillItem.setPharmaceuticalBillItem(new PharmaceuticalBillItem());
            parentBillItem = new BillItem();
            parentBillItem.setPharmaceuticalBillItem(new PharmaceuticalBillItem());
            JsfUtil.addSuccessMessage("Accessory Added.");
        }
//        ////// // System.out.println("****Inventory Code 1****" + getCurrentBillItem().getPharmaceuticalBillItem().getCode() + "*******");
//        createSerialNumber(getCurrentBillItem());
//        ////// // System.out.println("****Inventory Code 2****" + getCurrentBillItem().getPharmaceuticalBillItem().getCode() + "*******");
//
//        addBillItem(getCurrentBillItem());
//        currentBillItem = null;
//        calTotal();
    }

    BillItem currentBillItem;

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

    public void calTotal() {
        double tot = 0.0;
        double exp = 0.0;
        Integer serialNo = 0;
        for (BillItem p : getBillItems()) {
            p.setQty((double) p.getPharmaceuticalBillItem().getQtyInUnit());
            p.setRate(p.getPharmaceuticalBillItem().getPurchaseRateInUnit());
            p.setSearialNo(serialNo++);
            //p.setId(serialNo.longValue());
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

        getGrnBill().setExpenseTotal(exp);
        getGrnBill().setTotal(tot);
        getGrnBill().setNetTotal(tot + exp);

    }

    public StoreCalculation getStoreCalculation() {
        return storeCalculation;
    }

    public void setStoreCalculation(StoreCalculation storeCalculation) {
        this.storeCalculation = storeCalculation;
    }

    public BillItem getParentBillItem() {
        return parentBillItem;
    }

    public void setParentBillItem(BillItem parentBillItem) {
        this.parentBillItem = parentBillItem;
    }

}
