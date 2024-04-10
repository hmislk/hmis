/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.bean.common.SessionController;

import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Item;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemsDistributorsFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.OptionScope;
import com.divudi.data.dataStructure.PaymentMethodData;
import java.io.Serializable;
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

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class PurchaseOrderRequestController implements Serializable {

    @EJB
    private ItemFacade itemFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private ItemsDistributorsFacade itemsDistributorsFacade;

    @Inject
    private SessionController sessionController;
    @Inject
    CommonController commonController;
    @Inject
    ConfigOptionController optionController;

    private Bill currentBill;
    private BillItem currentBillItem;
    private List<BillItem> selectedBillItems;
    private List<BillItem> billItems;
    private boolean printPreview;
    //private List<PharmaceuticalBillItem> pharmaceuticalBillItems;   
    @Inject
    PharmacyCalculation pharmacyBillBean;
    private PaymentMethodData paymentMethodData;

    public void removeSelected() {
        if (selectedBillItems == null) {
            return;
        }
        for (BillItem b : selectedBillItems) {
            b.setRetired(true);
            b.setRetirer(sessionController.getLoggedUser());
            b.setRetiredAt(new Date());
            if (getCurrentBill().getId() != null) {
                if (b.getId() != null) {
                    billItemFacade.edit(b);
                }
            }
        }

        getBillItems().removeAll(selectedBillItems);
        calTotal();
        selectedBillItems = null;
    }

    public PharmacyCalculation getPharmacyBillBean() {
        return pharmacyBillBean;
    }

    public String navigateToCreateNewPurchaseOrder() {
        System.out.println("navigateToCreateNewPurchaseOrder");
        resetBillValues();
        getCurrentBill();
        return "/pharmacy/pharmacy_purhcase_order_request?faces-redirect=true";
    }

    public String navigateToUpdatePurchaseOrder() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return "";
        }
        Bill tmpBill = currentBill;
        resetBillValues();
        setCurrentBill(tmpBill);
        setBillItems(generateBillItems(currentBill));
//        for(BillItem bi: getBillItems()){
//            System.out.println("bi = " + bi.getPharmaceuticalBillItem());
//            if(bi.getPharmaceuticalBillItem()==null){
//                bi.setPharmaceuticalBillItem(generatePharmaceuticalBillItem(bi));
//            }
//        }
        calTotal();
        return "/pharmacy/pharmacy_purhcase_order_request?faces-redirect=true";
    }

    public List<BillItem> generateBillItems(Bill bill) {
        String jpql = "select bi "
                + " from BillItem bi "
                + " where bi.retired=:ret "
                + " and bi.bill=:bill";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("bill", bill);
        return billItemFacade.findByJpql(jpql, m);
    }

    public PharmaceuticalBillItem generatePharmaceuticalBillItem(BillItem billItem) {
        String jpql = "select pbi "
                + " from PharmaceuticalBillItem pbi "
                + " where bi.billItem=:bi";
        Map m = new HashMap();
        m.put("bi", billItem);
        return pharmaceuticalBillItemFacade.findFirstByJpql(jpql, m);
    }

    public void setPharmacyBillBean(PharmacyCalculation pharmacyBillBean) {
        this.pharmacyBillBean = pharmacyBillBean;
    }

    public void resetBillValues() {
        currentBill = null;
        currentBillItem = null;
        billItems = null;
        printPreview = false;
    }

    public void addItem() {
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

    public void removeItem(BillItem bi) {
        //System.err.println("5 " + bi.getItem().getName());
        //System.err.println("6 " + bi.getSearialNo());
        getBillItems().remove(bi);

        calTotal();

        currentBillItem = null;

    }

    @Inject
    private PharmacyController pharmacyController;

    public void onFocus(BillItem bi) {
        getPharmacyController().setPharmacyItem(bi.getItem());
    }

    public void onEdit(BillItem bi) {
        bi.getPharmaceuticalBillItem().setQty(bi.getQty());
        bi.setNetValue(bi.getPharmaceuticalBillItem().getQty() * bi.getPharmaceuticalBillItem().getPurchaseRate());
        getPharmacyController().setPharmacyItem(bi.getItem());
        calTotal();
    }

    public void saveBill() {

        getCurrentBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyOrder, BillClassType.BilledBill, BillNumberSuffix.POR));
        getCurrentBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyOrder, BillClassType.BilledBill, BillNumberSuffix.POR));

        getCurrentBill().setCreater(getSessionController().getLoggedUser());
        getCurrentBill().setCreatedAt(Calendar.getInstance().getTime());

        getCurrentBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrentBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getCurrentBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrentBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getCurrentBill().setEditedAt(null);
        getCurrentBill().setEditor(null);

        getCurrentBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER_PRE);

        if (getCurrentBill().getId() == null) {
            getBillFacade().create(getCurrentBill());
        } else {
            getBillFacade().edit(getCurrentBill());
        }

    }

    public void finalizeBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }
        if (currentBill.getId() == null) {
            request();
        }
        getCurrentBill().setEditedAt(new Date());
        getCurrentBill().setEditor(sessionController.getLoggedUser());
        getCurrentBill().setCheckeAt(new Date());
        getCurrentBill().setCheckedBy(sessionController.getLoggedUser());
        getCurrentBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER);

        getBillFacade().edit(getCurrentBill());

    }

    public void generateBillComponentsForAllSupplierItems(List<Item> items) {
        if (items == null) {
            return;
        }
        if (items.isEmpty()) {
            return;
        }
        setBillItems(new ArrayList<>());
        for (Item i : items) {
            BillItem bi = new BillItem();
            bi.setItem(i);

            PharmaceuticalBillItem tmp = new PharmaceuticalBillItem();
            tmp.setBillItem(bi);
//            tmp.setQty(getPharmacyBean().getOrderingQty(bi.getItem(), getSessionController().getDepartment()));
            tmp.setPurchaseRateInUnit(getPharmacyBean().getLastPurchaseRate(bi.getItem(), getSessionController().getDepartment()));
            tmp.setRetailRateInUnit(getPharmacyBean().getLastRetailRate(bi.getItem(), getSessionController().getDepartment()));

//            bi.setTmpQty(tmp.getQty());
            bi.setPharmaceuticalBillItem(tmp);

            getBillItems().add(bi);

        }

        calTotal();

    }

    public void saveBillComponent() {
        for (BillItem b : getBillItems()) {
            b.setRate(b.getPharmaceuticalBillItem().getPurchaseRateInUnit());
            b.setNetValue(b.getPharmaceuticalBillItem().getQtyInUnit() * b.getPharmaceuticalBillItem().getPurchaseRateInUnit());
            b.setBill(getCurrentBill());
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

//            PharmaceuticalBillItem tmpPh = b.getPharmaceuticalBillItem();
//            b.setPharmaceuticalBillItem(null);
            if (b.getId() == null) {
                getBillItemFacade().create(b);
            } else {
                getBillItemFacade().edit(b);
            }

            if (b.getPharmaceuticalBillItem().getId() == null) {
                getPharmaceuticalBillItemFacade().create(b.getPharmaceuticalBillItem());
            } else {
                getPharmaceuticalBillItemFacade().edit(b.getPharmaceuticalBillItem());
            }

        }
    }

    public void finalizeBillComponent() {
        getBillItems().removeIf(BillItem::isRetired);
        for (BillItem b : getBillItems()) {
            b.setRate(b.getPharmaceuticalBillItem().getPurchaseRateInUnit());
            b.setNetValue(b.getPharmaceuticalBillItem().getQtyInUnit() * b.getPharmaceuticalBillItem().getPurchaseRateInUnit());
            b.setBill(getCurrentBill());
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            double qty = 0.0;
            qty = b.getQty() + b.getPharmaceuticalBillItem().getFreeQty();
            if (qty <= 0.0) {
                b.setRetired(true);
                b.setRetirer(sessionController.getLoggedUser());
                b.setRetiredAt(new Date());
                b.setRetireComments("Retired at Finalising PO");

            }

//            PharmaceuticalBillItem tmpPh = b.getPharmaceuticalBillItem();
//            b.setPharmaceuticalBillItem(null);
            if (b.getId() == null) {
                getBillItemFacade().create(b);
            } else {
                getBillItemFacade().edit(b);
            }

            if (b.getPharmaceuticalBillItem().getId() == null) {
                getPharmaceuticalBillItemFacade().create(b.getPharmaceuticalBillItem());
            } else {
                getPharmaceuticalBillItemFacade().edit(b.getPharmaceuticalBillItem());
            }

        }
    }

    public void addAllSupplierItems() {
        if (getCurrentBill().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Please Select Dealor");
            return;
        }
        List<Item> allItems = getPharmacyBillBean().getItemsForDealor(getCurrentBill().getToInstitution());
        generateBillComponentsForAllSupplierItems(allItems);
    }

    public void addAllSupplierItemsBelowRol() {
        if (getCurrentBill().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Please Select Dealer");
            return;
        }

        String jpql = "SELECT i FROM Item i WHERE i IN "
                + "(SELECT id.item FROM ItemsDistributors id WHERE id.institution = :supplier AND id.retired = false AND id.item.retired = false) "
                + "AND i IN "
                + "(SELECT s.itemBatch.item FROM Stock s JOIN Reorder r ON r.item = s.itemBatch.item AND r.department = s.department "
                + "WHERE s.department = :department AND s.stock < r.rol GROUP BY s.itemBatch.item) "
                + "ORDER BY i.name";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("supplier", getCurrentBill().getToInstitution());
        parameters.put("department", getSessionController().getDepartment());
        List<Item> itemsBelowReorderLevel = getItemFacade().findByJpql(jpql, parameters);

        if (itemsBelowReorderLevel == null || itemsBelowReorderLevel.isEmpty()) {
            JsfUtil.addErrorMessage("No items found below reorder level for the selected supplier and department.");
        } else {
            generateBillComponentsForAllSupplierItems(itemsBelowReorderLevel);
        }
    }

    public void request() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (getCurrentBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please Select Paymntmethod");
            return;
        }
//
//        if (checkItemPrice()) {
//            JsfUtil.addErrorMessage("Please enter purchase price for all");
//            return;
//        }

        saveBill();
        saveBillComponent();

        JsfUtil.addSuccessMessage("Request Saved");
//
//        resetBillValues();

        

    }

    public List<Item> getDealorItems() {
        List<Item> lst;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c.item "
                + " from ItemsDistributors c"
                + " where c.retired=false "
                + " and c.item.retired=false "
                + " and c.institution=:ins "
                + " order by c.item.name";
        hm.put("ins", getCurrentBill().getToInstitution());
        lst = itemFacade.findByJpql(sql, hm, 200);
        return lst;
    }

    public void requestFinalize() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (getCurrentBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please Select Paymntmethod");
            return;
        }
        finalizeBill();
        finalizeBillComponent();
        JsfUtil.addSuccessMessage("Request Succesfully Finalized");
        printPreview = true;
        
    }

    public void calTotal() {
        double tmp = 0;
        int serialNo = 0;
        for (BillItem b : getBillItems()) {
            tmp += b.getPharmaceuticalBillItem().getQty() * b.getPharmaceuticalBillItem().getPurchaseRate();
            b.setSearialNo(serialNo++);
        }

        getCurrentBill().setTotal(tmp);
        getCurrentBill().setNetTotal(tmp);

    }

    public PurchaseOrderRequestController() {
    }

    @Inject
    private ItemController itemController;

    public void setInsListener() {
        getItemController().setInstitution(getCurrentBill().getToInstitution());
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

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public Bill getCurrentBill() {
        if (currentBill == null) {
            currentBill = new BilledBill();
            currentBill.setBillType(BillType.PharmacyOrder);
            currentBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER);
            PaymentMethod pm = optionController.getEnumValueByKey("Pharmacy Purchase Order Default Payment Method", PaymentMethod.class, OptionScope.APPLICATION, null, null, null); 
            System.out.println("pm = " + pm);
            currentBill.setPaymentMethod(pm);
        }
        return currentBill;
    }

    public void setCurrentBill(Bill currentBill) {
        this.currentBill = currentBill;
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

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public ItemsDistributorsFacade getItemsDistributorsFacade() {
        return itemsDistributorsFacade;
    }

    public void setItemsDistributorsFacade(ItemsDistributorsFacade itemsDistributorsFacade) {
        this.itemsDistributorsFacade = itemsDistributorsFacade;
    }

    public PharmacyController getPharmacyController() {
        return pharmacyController;
    }

    public void setPharmacyController(PharmacyController pharmacyController) {
        this.pharmacyController = pharmacyController;
    }

    public List<BillItem> getSelectedBillItems() {
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
    }

    public ItemController getItemController() {
        return itemController;
    }

    public void setItemController(ItemController itemController) {
        this.itemController = itemController;
    }

    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            currentBillItem.setPharmaceuticalBillItem(ph);
            ph.setBillItem(currentBillItem);
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

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public PaymentMethodData getPaymentMethodData() {
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

}
