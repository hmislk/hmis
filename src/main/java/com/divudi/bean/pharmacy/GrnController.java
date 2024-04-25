/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.ejb.BillNumberGenerator;

import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillFeePayment;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.Payment;
import com.divudi.entity.pharmacy.ItemBatch;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.AmpFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillFeePaymentFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.CategoryFacade;
import com.divudi.facade.ItemBatchFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
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
public class GrnController implements Serializable {

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
    private PharmacyBean pharmacyBean;
    @EJB
    private AmpFacade ampFacade;

    private CommonFunctions commonFunctions;
    @EJB
    BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    PaymentFacade paymentFacade;
    @Inject
    private PharmacyCalculation pharmacyCalculation;
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
    private List<BillItem> selectedBillItems;
    private SearchKeyword searchKeyword;
    private List<Bill> bills;
    private double insTotal;
    private double difference;

    public double calDifference() {
        difference = Math.abs(insTotal) - Math.abs(grnBill.getTotal());
        return difference;
    }

    public String navigateToResive() {
        grnBill = null;
        dealor = null;
        pos = null;
        printPreview = false;
        billItems = null;
        difference = 0;
        insTotal = 0;
        createGrn();
        return "/pharmacy/pharmacy_grn?faces-redirect=true";
    }

    public String navigateToResiveAll() {
        grnBill = null;
        dealor = null;
        pos = null;
        printPreview = false;
        billItems = null;
        createGrnWholesale();
        return "/pharmacy/pharmacy_grn?faces-redirect=true";
    }
    
    public String navigateToReceiveWholesale() {
        grnBill = null;
        dealor = null;
        pos = null;
        printPreview = false;
        billItems = null;
        difference = 0;
        insTotal = 0;
        createGrnWholesale();
        return "/pharmacy/pharmacy_grn_wh?faces-redirect=true";
    }

    public void removeItem(BillItem bi) {
        getBillItems().remove(bi.getSearialNo());
        calGrossTotal();
    }

    public List<BillItem> findAllBillItemsRefernceToOriginalItem(BillItem referenceBillItem){
        List<BillItem> tmpBillItems = new ArrayList<>();
        for (BillItem i : getBillItems()){
            if(i.getReferanceBillItem()==referenceBillItem){
                tmpBillItems.add(i);
            }
        }
        return tmpBillItems;
    }
    
    public void duplicateItem(BillItem originalBillItemToDuplicate) {
        BillItem newBillItemCreatedByDuplication = new BillItem();
        double totalQuantityOfBillItemsRefernceToOriginalItem = 0.0;
        double totalFreeQuantityOfBillItemsRefernceToOriginalItem = 0.0;
        
        double remainFreeQty = 0.0;
        double remainQty = 0.0;
        if (originalBillItemToDuplicate != null) {
            PharmaceuticalBillItem newPharmaceuticalBillItemCreatedByDuplication = new PharmaceuticalBillItem();
            newPharmaceuticalBillItemCreatedByDuplication.copy(originalBillItemToDuplicate.getPharmaceuticalBillItem());
            newPharmaceuticalBillItemCreatedByDuplication.setBillItem(newBillItemCreatedByDuplication);
            newBillItemCreatedByDuplication.setItem(originalBillItemToDuplicate.getItem());
            newBillItemCreatedByDuplication.setReferanceBillItem(originalBillItemToDuplicate.getReferanceBillItem());
            newBillItemCreatedByDuplication.setPharmaceuticalBillItem(newPharmaceuticalBillItemCreatedByDuplication);
            
            List<BillItem> tmpBillItems = findAllBillItemsRefernceToOriginalItem(originalBillItemToDuplicate.getReferanceBillItem());
           
            for(BillItem bi:tmpBillItems){
                totalQuantityOfBillItemsRefernceToOriginalItem += bi.getPharmaceuticalBillItem().getQtyInUnit();
                totalFreeQuantityOfBillItemsRefernceToOriginalItem += bi.getPharmaceuticalBillItem().getFreeQtyInUnit();
            }
            remainQty = originalBillItemToDuplicate.getPreviousRecieveQtyInUnit() - totalQuantityOfBillItemsRefernceToOriginalItem;
            remainFreeQty = originalBillItemToDuplicate.getPreviousRecieveFreeQtyInUnit() - totalFreeQuantityOfBillItemsRefernceToOriginalItem;
            
            newBillItemCreatedByDuplication.getPharmaceuticalBillItem().setQty(remainQty);
            newBillItemCreatedByDuplication.getPharmaceuticalBillItem().setQtyInUnit(remainQty);
            
            newBillItemCreatedByDuplication.getPharmaceuticalBillItem().setFreeQty(remainFreeQty);
            newBillItemCreatedByDuplication.getPharmaceuticalBillItem().setFreeQtyInUnit(remainFreeQty);
            
            newBillItemCreatedByDuplication.setTmpQty(remainQty);
            newBillItemCreatedByDuplication.setTmpFreeQty(remainFreeQty);
            
            newBillItemCreatedByDuplication.setPreviousRecieveQtyInUnit(originalBillItemToDuplicate.getPreviousRecieveQtyInUnit());
            newBillItemCreatedByDuplication.setPreviousRecieveFreeQtyInUnit(originalBillItemToDuplicate.getPreviousRecieveFreeQtyInUnit());
            getBillItems().add(newBillItemCreatedByDuplication);
        }
        calGrossTotal();
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
            calGrossTotal();
        }

        selectedBillItems = null;
    }

    public void clearList() {
        //   pharmacyItems = null;
        pos = null;
        filteredValue = null;
        //  billItems = null;
        grns = null;
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

        Date date = pid.getPharmaceuticalBillItem().getDoe();
        DateFormat df = new SimpleDateFormat("ddMMyyyy");
        String reportDate = df.format(date);
// Print what date is today!
        //       //System.err.println("Report Date: " + reportDate);
        pid.getPharmaceuticalBillItem().setStringValue(reportDate);

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

    public void settle() {
        if (Math.abs(difference) > 1) {
            JsfUtil.addErrorMessage("The invoice does not match..! Check again");
            return;
        }

        String msg = pharmacyCalculation.errorCheck(getGrnBill(), billItems);
        if (!msg.isEmpty()) {
            JsfUtil.addErrorMessage(msg);
            return;
        }
        pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getGrnBill());
        if (getGrnBill().getInvoiceDate() == null) {
            getGrnBill().setInvoiceDate(getApproveBill().getCreatedAt());
        }

        saveBill();

        Payment p = createPayment(getGrnBill(), getGrnBill().getPaymentMethod());

        for (BillItem i : getBillItems()) {
            if (i.getTmpQty() == 0.0 && i.getTmpFreeQty() == 0.0) {
                continue;
            }

            PharmaceuticalBillItem ph = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);

            i.setCreatedAt(new Date());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getGrnBill());
            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            i.setPharmaceuticalBillItem(ph);
            getBillItemFacade().edit(i);

            //     updatePoItemQty(i);
            //System.err.println("1 " + i);
            ItemBatch itemBatch = getPharmacyCalculation().saveItemBatch(i);
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());

            double addingQty = i.getPharmaceuticalBillItem().getQtyInUnit() + i.getPharmaceuticalBillItem().getFreeQtyInUnit();

            i.getPharmaceuticalBillItem().setItemBatch(itemBatch);

            Stock stock = getPharmacyBean().addToStock(
                    i.getPharmaceuticalBillItem(),
                    Math.abs(addingQty),
                    getSessionController().getDepartment());

            i.getPharmaceuticalBillItem().setStock(stock);

            getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
            getPharmacyCalculation().editBillItem(i.getPharmaceuticalBillItem(), getSessionController().getLoggedUser());
            saveBillFee(i, p);
            getGrnBill().getBillItems().add(i);
        }

        getGrnBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN));
        getGrnBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN));

        getGrnBill().setToInstitution(getApproveBill().getFromInstitution());
        getGrnBill().setToDepartment(getApproveBill().getFromDepartment());

        getGrnBill().setInstitution(getSessionController().getInstitution());
        getGrnBill().setDepartment(getSessionController().getDepartment());

        getGrnBill().setCreater(getSessionController().getLoggedUser());
        getGrnBill().setCreatedAt(Calendar.getInstance().getTime());

        calGrossTotal();

        pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getGrnBill());
        updateBalanceForGrn(getGrnBill());

        getBillFacade().edit(getGrnBill());

        //  getPharmacyBillBean().editBill(, , getSessionController());
        printPreview = true;

    }

    public void settleWholesale() {
        if (insTotal == 0 && difference != 0) {
            JsfUtil.addErrorMessage("Fill the invoice Total");
            return;
        }
        if (difference != 0) {
            JsfUtil.addErrorMessage("The invoice does not match..! Check again");
            return;
        }

        String msg = pharmacyCalculation.errorCheck(getGrnBill(), billItems);
        if (!msg.isEmpty()) {
            JsfUtil.addErrorMessage(msg);
            return;
        }
        pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getGrnBill());
        if (getGrnBill().getInvoiceDate() == null) {
            getGrnBill().setInvoiceDate(getApproveBill().getCreatedAt());
        }

        saveWholesaleBill();

        Payment p = createPayment(getGrnBill(), getGrnBill().getPaymentMethod());

        for (BillItem i : getBillItems()) {
            if (i.getTmpQty() == 0.0 && i.getTmpFreeQty() == 0.0) {
                continue;
            }

            PharmaceuticalBillItem ph = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);

            i.setCreatedAt(new Date());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getGrnBill());
            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            i.setPharmaceuticalBillItem(ph);
            getBillItemFacade().edit(i);

            //     updatePoItemQty(i);
            //System.err.println("1 " + i);
            ItemBatch itemBatch = getPharmacyCalculation().saveItemBatch(i);
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());

            double addingQty = i.getPharmaceuticalBillItem().getQtyInUnit() + i.getPharmaceuticalBillItem().getFreeQtyInUnit();

            i.getPharmaceuticalBillItem().setItemBatch(itemBatch);

            Stock stock = getPharmacyBean().addToStock(
                    i.getPharmaceuticalBillItem(),
                    Math.abs(addingQty),
                    getSessionController().getDepartment());

            i.getPharmaceuticalBillItem().setStock(stock);

            getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
            getPharmacyCalculation().editBillItem(i.getPharmaceuticalBillItem(), getSessionController().getLoggedUser());
            saveBillFee(i, p);
            getGrnBill().getBillItems().add(i);
        }

        getGrnBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN));
        getGrnBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN));

        getGrnBill().setToInstitution(getApproveBill().getFromInstitution());
        getGrnBill().setToDepartment(getApproveBill().getFromDepartment());

        getGrnBill().setInstitution(getSessionController().getInstitution());
        getGrnBill().setDepartment(getSessionController().getDepartment());

        getGrnBill().setCreater(getSessionController().getLoggedUser());
        getGrnBill().setCreatedAt(Calendar.getInstance().getTime());

        calGrossTotal();

        pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getGrnBill());
        updateBalanceForGrn(getGrnBill());

        getBillFacade().edit(getGrnBill());
        //  getPharmacyBillBean().editBill(, , getSessionController());
        printPreview = true;

    }

    private void updateBalanceForGrn(Bill grn) {
        if (grn == null) {
            return;
        }
        switch (grn.getPaymentMethod()) {
            case Agent:
            case Card:
            case Cash:
            case Cheque:
            case MultiplePaymentMethods:
            case OnCall:
            case OnlineSettlement:
            case PatientDeposit:
            case Slip:
            case Staff:
            case YouOweMe:
            case ewallet:
                grn.setBalance(0.0);
                break;
            case Credit:
                grn.setBalance(Math.abs(grn.getNetTotal()));
            default:
        }
    }

    public void viewPoList() {
        clearList();

    }

    public GrnController() {
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
        getGrnBill().setBillDate(new Date());
        getGrnBill().setBillTime(new Date());
        getGrnBill().setPaymentMethod(getApproveBill().getPaymentMethod());
        getGrnBill().setReferenceBill(getApproveBill());
        if (getGrnBill().getFromInstitution() == null) {
            getGrnBill().setFromInstitution(getApproveBill().getToInstitution());
        }
        getGrnBill().setDepartment(getSessionController().getDepartment());
        getGrnBill().setInstitution(getSessionController().getInstitution());
        //   getGrnBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyGrnBill, BillNumberSuffix.GRN));
        //   getGrnBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getGrnBill(), BillType.PharmacyGrnBill, BillNumberSuffix.GRN));
        getGrnBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN);
        if (getGrnBill().getId() == null) {
            getBillFacade().create(getGrnBill());
        } else {
            getBillFacade().edit(getGrnBill());
        }
    }

    public void saveWholesaleBill() {
        getGrnBill().setBillDate(new Date());
        getGrnBill().setBillTime(new Date());
        getGrnBill().setPaymentMethod(getApproveBill().getPaymentMethod());
        getGrnBill().setReferenceBill(getApproveBill());
        if (getGrnBill().getFromInstitution() == null) {
            getGrnBill().setFromInstitution(getApproveBill().getToInstitution());
        }
        getGrnBill().setDepartment(getSessionController().getDepartment());
        getGrnBill().setInstitution(getSessionController().getInstitution());
        //   getGrnBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyGrnBill, BillNumberSuffix.GRN));
        //   getGrnBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getGrnBill(), BillType.PharmacyGrnBill, BillNumberSuffix.GRN));
        if (getGrnBill().getId() == null) {
            getBillFacade().create(getGrnBill());
        }
    }

//    public void generateBillComponent() {
//
//        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getApproveBill())) {        
//            double remains = getPharmacyCalculation().calQtyInTwoSql(i);
//            
//            if (i.getQtyInUnit() >= remains && (i.getQtyInUnit() - remains) != 0) {
//                BillItem bi = new BillItem();
//                bi.setSearialNo(getBillItems().size());
//                bi.setItem(i.getBillItem().getItem());
//                bi.setReferanceBillItem(i.getBillItem());
//                bi.setQty(i.getQtyInUnit() - remains);
//                bi.setTmpQty(i.getQtyInUnit() - remains);
//                //Set Suggession
////                bi.setTmpSuggession(getPharmacyCalculation().getSuggessionOnly(bi.getItem()));
//
//                PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
//                ph.setBillItem(bi);
//                double tmpQty = bi.getQty();
//                ph.setQtyInUnit((double) tmpQty);
//                ph.setPurchaseRate(i.getPurchaseRate());
//                ph.setRetailRate(i.getRetailRate());
//                ph.setLastPurchaseRate(getPharmacyBean().getLastPurchaseRate(bi.getItem(), getSessionController().getDepartment()));
//
//                bi.setPharmaceuticalBillItem(ph);
//
//                getBillItems().add(bi);
//                //  getBillItems().r
//            }
//
//        }
//    }
    public void generateBillComponent() {

        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getApproveBill())) {
            double remains = i.getQtyInUnit() - getPharmacyCalculation().calQtyInTwoSql(i);
            double remainFreeQty = i.getFreeQty() - getPharmacyCalculation().calFreeQtyInTwoSql(i);

            if (remains > 0 || remainFreeQty > 0) {
                BillItem bi = new BillItem();
                bi.setSearialNo(getBillItems().size());
                bi.setItem(i.getBillItem().getItem());
                bi.setReferanceBillItem(i.getBillItem());
                bi.setQty(remains);
//                bi.setFreeQty(remainFreeQty);
                bi.setTmpQty(remains);
                bi.setTmpFreeQty(remainFreeQty);
                //Set Suggession
//                bi.setTmpSuggession(getPharmacyCalculation().getSuggessionOnly(bi.getItem()));

                PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
                ph.setBillItem(bi);
                double tmpQty = bi.getQty();
                double tmpFreeQty = remainFreeQty;
                
                bi.setPreviousRecieveQtyInUnit((double) tmpQty);
                bi.setPreviousRecieveFreeQtyInUnit((double) tmpFreeQty);

                ph.setQty(tmpQty);
                ph.setQtyInUnit((double) tmpQty);

                ph.setFreeQtyInUnit((double) tmpFreeQty);
                ph.setFreeQty((double) tmpFreeQty);
                
                ph.setPurchaseRate(i.getPurchaseRate());
                ph.setRetailRate(i.getRetailRate());

                ph.setWholesaleRate((ph.getPurchaseRate() * 1.08) * ph.getQtyInUnit() / (ph.getFreeQtyInUnit() + ph.getQtyInUnit()));

                ph.setLastPurchaseRate(getPharmacyBean().getLastPurchaseRate(bi.getItem(), getSessionController().getDepartment()));

                bi.setPharmaceuticalBillItem(ph);
                

                getBillItems().add(bi);
                //  getBillItems().r
            }

        }
    }

    public void generateBillComponentAll() {

        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getApproveBill())) {
            double remains = i.getQtyInUnit() - getPharmacyCalculation().calQtyInTwoSql(i);

            BillItem bi = new BillItem();
            bi.setSearialNo(getBillItems().size());
            bi.setItem(i.getBillItem().getItem());
            bi.setReferanceBillItem(i.getBillItem());
            bi.setQty(remains);
            bi.setTmpQty(remains);
            //Set Suggession
//                bi.setTmpSuggession(getPharmacyCalculation().getSuggessionOnly(bi.getItem()));

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.setBillItem(bi);
            double tmpQty = bi.getQty();

            ph.setQty(tmpQty);
            ph.setQtyInUnit((double) tmpQty);

            ph.setFreeQty(i.getFreeQty());
            ph.setFreeQtyInUnit(i.getFreeQty());

            ph.setPurchaseRate(i.getPurchaseRate());
            ph.setRetailRate(i.getRetailRate());

            ph.setWholesaleRate((ph.getPurchaseRate() * 1.08) * ph.getQtyInUnit() / (ph.getFreeQtyInUnit() + ph.getQtyInUnit()));

            ph.setLastPurchaseRate(getPharmacyBean().getLastPurchaseRate(bi.getItem(), getSessionController().getDepartment()));
            ph.setFreeQty(i.getFreeQty());

            bi.setPharmaceuticalBillItem(ph);

            getBillItems().add(bi);
            //  getBillItems().r

        }
    }

    public void createGrn() {
        getGrnBill().setPaymentMethod(getApproveBill().getPaymentMethod());
        getGrnBill().setFromInstitution(getApproveBill().getToInstitution());
        getGrnBill().setReferenceInstitution(getSessionController().getLoggedUser().getInstitution());
        generateBillComponent();
        calGrossTotal();
    }

    public void createGrnAll() {
        getGrnBill().setPaymentMethod(getApproveBill().getPaymentMethod());
        getGrnBill().setFromInstitution(getApproveBill().getToInstitution());
        getGrnBill().setReferenceInstitution(getSessionController().getLoggedUser().getInstitution());
        generateBillComponentAll();
        calGrossTotal();
    }
    
    public void createGrnWholesale() {
        getGrnBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_WHOLESALE);
        getGrnBill().setPaymentMethod(getApproveBill().getPaymentMethod());
        getGrnBill().setFromInstitution(getApproveBill().getToInstitution());
        getGrnBill().setReferenceInstitution(getSessionController().getLoggedUser().getInstitution());
        generateBillComponent();
        calGrossTotal();
    }

    private double getRetailPrice(BillItem billItem) {
        String sql = "select (p.retailRate) from PharmaceuticalBillItem p where p.billItem=:b";
        HashMap hm = new HashMap();
        hm.put("b", billItem.getReferanceBillItem());
        return (double) getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public void onEdit(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();

        //    onEditPurchaseRate(tmp);
        setBatch(tmp);
        onEdit(tmp);
    }

    public void onEdit(BillItem tmp) {
        setBatch(tmp);
        double remains = getPharmacyCalculation().getRemainingQty(tmp.getPharmaceuticalBillItem());

//        System.err.println("1 " + tmp.getTmpQty());
//        System.err.println("2 " + tmp.getQty());
//        System.err.println("3 " + tmp.getPharmaceuticalBillItem().getQty());
//        System.err.println("4 " + tmp.getPharmaceuticalBillItem().getQtyInUnit());
//System.out.println("remains = " + remains);
//        System.out.println("tmp.getPharmaceuticalBillItem().getQtyInUnit() = " + tmp.getPharmaceuticalBillItem().getQtyInUnit());
        if (remains < tmp.getPharmaceuticalBillItem().getQtyInUnit()) {
            tmp.setTmpQty(remains);
            JsfUtil.addErrorMessage("You cant Change Qty than Remaining qty");
        }
//        System.out.println("tmp.getPreviousRecieveQtyInUnit() = " + tmp.getPreviousRecieveQtyInUnit());
//        if(tmp.getPreviousRecieveQtyInUnit() < tmp.getPharmaceuticalBillItem().getQtyInUnit()){
//            tmp.setTmpQty(tmp.getPreviousRecieveQtyInUnit());
//            JsfUtil.addErrorMessage("You cant Order Qty than Remaining qty to recieve");
//        }

        if (tmp.getPharmaceuticalBillItem().getPurchaseRate() > tmp.getPharmaceuticalBillItem().getRetailRate()) {
            tmp.getPharmaceuticalBillItem().setRetailRate(getRetailPrice(tmp.getPharmaceuticalBillItem().getBillItem()));
            JsfUtil.addErrorMessage("You cant set retail price below purchase rate");
        }

        if (tmp.getPharmaceuticalBillItem().getDoe() != null) {
            if (tmp.getPharmaceuticalBillItem().getDoe().getTime() < Calendar.getInstance().getTimeInMillis()) {
                tmp.getPharmaceuticalBillItem().setDoe(null);
                JsfUtil.addErrorMessage("Check Date of Expiry");
                //    return;
            }
        }
//        manually set Wholesale Rate**************
//        tmp.getPharmaceuticalBillItem().setWholesaleRate((tmp.getPharmaceuticalBillItem().getPurchaseRate() * 1.08)* (tmp.getPharmaceuticalBillItem().getQtyInUnit())/(tmp.getPharmaceuticalBillItem().getQtyInUnit() + tmp.getPharmaceuticalBillItem().getFreeQtyInUnit())  );

        calGrossTotal();
        calDifference();
    }

    public void onEditPurchaseRate(BillItem tmp) {

        double retail = tmp.getPharmaceuticalBillItem().getPurchaseRate() + (tmp.getPharmaceuticalBillItem().getPurchaseRate() * (getPharmacyBean().getMaximumRetailPriceChange() / 100));
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
    public void calGrossTotal() {
        double tmp = 0.0;
        int serialNo = 0;
        for (BillItem p : getBillItems()) {
            tmp += p.getPharmaceuticalBillItem().getPurchaseRate() * p.getPharmaceuticalBillItem().getQty();
            p.setSearialNo(serialNo++);
        }

        getGrnBill().setTotal(0 - tmp);
        ChangeDiscountLitener();
    }

    public void ChangeDiscountLitener() {
        getGrnBill().setNetTotal(getGrnBill().getTotal() + getGrnBill().getDiscount());

    }

    public void netDiscount() {
        //getGrnBill().setNetTotal(getGrnBill().getTotal() + getGrnBill().getDiscount());
        double grossTotal = 0.0;
        if (getGrnBill().getDiscount() > 0) {
            ChangeDiscountLitener();
        }
        calDifference();
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

//    public double getNetTotal() {
//
//        double tmp = getGrnBill().getTotal() + getGrnBill().getTax() - getGrnBill().getDiscount();
//        getGrnBill().setNetTotal(tmp);
//
//        return tmp;
//    }
    public void setApproveBill(Bill approveBill) {
        this.approveBill = approveBill;
//        grnBill = null;
//        dealor = null;
//        pos = null;
//        printPreview = false;
//        billItems = null;
//        createGrn();
    }

    public Bill getGrnBill() {
        if (grnBill == null) {
            grnBill = new BilledBill();
            grnBill.setBillType(BillType.PharmacyGrnBill);
            grnBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN);
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

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
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

    public PharmacyCalculation getPharmacyCalculation() {
        return pharmacyCalculation;
    }

    public void setPharmacyCalculation(PharmacyCalculation pharmacyCalculation) {
        this.pharmacyCalculation = pharmacyCalculation;
    }

    public List<Bill> getGrns() {
        return grns;
    }

    public void setGrns(List<Bill> grns) {
        this.grns = grns;
    }

    public BillFeePaymentFacade getBillFeePaymentFacade() {
        return billFeePaymentFacade;
    }

    public void setBillFeePaymentFacade(BillFeePaymentFacade billFeePaymentFacade) {
        this.billFeePaymentFacade = billFeePaymentFacade;
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

    public double getInsTotal() {
        return insTotal;
    }

    public void setInsTotal(double insTotal) {
        this.insTotal = insTotal;
    }

    public double getDifference() {
        return difference;
    }

    public void setDifference(double difference) {
        this.difference = difference;
    }

}
