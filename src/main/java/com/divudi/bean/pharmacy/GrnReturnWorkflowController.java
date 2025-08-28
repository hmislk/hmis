/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.EnumController;
import com.divudi.bean.common.SessionController;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.util.CommonFunctions;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Controller for GRN Return Workflow - handles Create, Finalize, and Approve steps
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class GrnReturnWorkflowController implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(GrnReturnWorkflowController.class.getName());

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

    @Inject
    private SessionController sessionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    EnumController enumController;

    // Main properties
    private Bill currentBill;
    private Bill requestedBill;  // For approval process
    private Bill approvedBill;   // For approval process
    private BillItem currentBillItem;
    private List<BillItem> selectedBillItems;
    private List<BillItem> billItems;
    private List<BillItem> selectedItems;  // For approval process
    private boolean printPreview;
    
    // GRN Return specific properties
    private Bill selectedGrn;
    private Bill originalGrn;
    private List<Item> dealorItems;

    @Inject
    PharmacyCalculation pharmacyBillBean;

    // Navigation methods
    public String navigateToCreateGrnReturn() {
        resetBillValues();
        return "/pharmacy/pharmacy_grn_return_request?faces-redirect=true";
    }

    public String navigateToCreateGrnReturnFromGrn() {
        if (selectedGrn == null) {
            JsfUtil.addErrorMessage("No GRN selected");
            return "";
        }
        // Follow legacy pattern - create return bill from selected GRN
        createReturnBillFromGrn(selectedGrn);
        return "/pharmacy/pharmacy_grn_return_form?faces-redirect=true";
    }

    public String navigateToUpdateGrnReturn() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No GRN Return selected");
            return "";
        }
        generateBillItemsForUpdate();
        return "/pharmacy/pharmacy_grn_return_form?faces-redirect=true";
    }

    public String navigateToFinalizeGrnReturn() {
        makeListNull();
        return "/pharmacy/pharmacy_grn_return_list_to_finalize?faces-redirect=true";
    }

    public String navigateToApproveGrnReturn() {
        makeListNull();
        return "/pharmacy/pharmacy_grn_return_list_to_approve?faces-redirect=true";
    }

    public String navigateToGrnReturnApproval() {
        if (requestedBill == null) {
            JsfUtil.addErrorMessage("No GRN Return selected for approval");
            return "";
        }
        
        // Create approved bill based on requested bill
        approvedBill = new RefundBill();
        approvedBill.copy(requestedBill);
        approvedBill.setBillType(BillType.PharmacyGrnReturn);
        approvedBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_RETURN);
        approvedBill.setReferenceBill(requestedBill.getReferenceBill());
        approvedBill.setBilledBill(requestedBill);
        
        generateBillItemsForApproval();
        
        return "/pharmacy/pharmacy_grn_return_approving?faces-redirect=true";
    }

    // Core workflow methods
    public void saveRequest() {
        if (validateGrnReturn()) {
            saveBill(false);
            JsfUtil.addSuccessMessage("GRN Return Request Saved Successfully");
        }
    }

    public void finalizeRequest() {
        if (validateGrnReturn()) {
            saveBill(true);
            printPreview = true;
            JsfUtil.addSuccessMessage("GRN Return Request Finalized Successfully");
        }
    }

    public void approve() {
        if (validateApproval()) {
            saveApprovedBill();
            updateStock();  // Stock handling happens only at approval stage
            printPreview = true;
            JsfUtil.addSuccessMessage("GRN Return Approved Successfully");
        }
    }

    // Bill management methods
    private void saveBill(boolean finalize) {
        if (currentBill.getId() == null) {
            currentBill.setBillType(BillType.PharmacyGrnReturn);
            currentBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_RETURN);
            currentBill.setInstitution(sessionController.getInstitution());
            currentBill.setDepartment(sessionController.getDepartment());
            currentBill.setCreater(sessionController.getLoggedUser());
            currentBill.setCreatedAt(new Date());

            String billNumber = billNumberBean.departmentBillNumberGeneratorYearly(
                sessionController.getDepartment(), 
                BillTypeAtomic.PHARMACY_GRN_RETURN);
            currentBill.setDeptId(billNumber);
            currentBill.setInsId(billNumber);
        }

        if (finalize) {
            currentBill.setCheckedBy(sessionController.getLoggedUser());
            currentBill.setCheckeAt(new Date());
        }

        calculateTotal();
        billFacade.edit(currentBill);
        saveBillItems();
    }

    private void saveApprovedBill() {
        approvedBill.setInstitution(sessionController.getInstitution());
        approvedBill.setDepartment(sessionController.getDepartment());
        approvedBill.setCreater(sessionController.getLoggedUser());
        approvedBill.setCreatedAt(new Date());
        approvedBill.setCheckedBy(sessionController.getLoggedUser());
        approvedBill.setCheckeAt(new Date());

        String billNumber = billNumberBean.departmentBillNumberGeneratorYearly(
            sessionController.getDepartment(), 
            BillTypeAtomic.PHARMACY_GRN_RETURN);
        approvedBill.setDeptId(billNumber);
        approvedBill.setInsId(billNumber);

        calculateApprovedTotal();
        billFacade.edit(approvedBill);
        saveApprovedBillItems();
        
        // Link the approved bill to the requested bill
        requestedBill.setReferenceBill(approvedBill);
        billFacade.edit(requestedBill);
    }

    private void saveBillItems() {
        for (BillItem bi : billItems) {
            if (bi.getPharmaceuticalBillItem().getQty() == 0 && 
                bi.getPharmaceuticalBillItem().getFreeQty() == 0) {
                continue;
            }

            bi.setBill(currentBill);
            bi.setCreatedAt(new Date());
            bi.setCreater(sessionController.getLoggedUser());

            PharmaceuticalBillItem phi = bi.getPharmaceuticalBillItem();
            phi.setBillItem(bi);

            if (bi.getId() == null) {
                billItemFacade.create(bi);
            } else {
                billItemFacade.edit(bi);
            }

            if (phi.getId() == null) {
                pharmaceuticalBillItemFacade.create(phi);
            } else {
                pharmaceuticalBillItemFacade.edit(phi);
            }
        }
    }

    private void saveApprovedBillItems() {
        for (BillItem bi : billItems) {
            if (bi.getPharmaceuticalBillItem().getQty() == 0 && 
                bi.getPharmaceuticalBillItem().getFreeQty() == 0) {
                continue;
            }

            BillItem approvedBi = new BillItem();
            approvedBi.copy(bi);
            approvedBi.setBill(approvedBill);
            approvedBi.setCreatedAt(new Date());
            approvedBi.setCreater(sessionController.getLoggedUser());

            PharmaceuticalBillItem approvedPhi = new PharmaceuticalBillItem();
            approvedPhi.copy(bi.getPharmaceuticalBillItem());
            approvedPhi.setBillItem(approvedBi);

            billItemFacade.create(approvedBi);
            pharmaceuticalBillItemFacade.create(approvedPhi);
        }
    }

    // Stock handling - only at approval stage
    private void updateStock() {
        for (BillItem bi : billItems) {
            if (bi.getPharmaceuticalBillItem().getQty() == 0 && 
                bi.getPharmaceuticalBillItem().getFreeQty() == 0) {
                continue;
            }

            PharmaceuticalBillItem phi = bi.getPharmaceuticalBillItem();
            double totalQty = phi.getQty() + phi.getFreeQty();

            // Deduct from stock for return
            boolean returnFlag = pharmacyBean.deductFromStock(
                phi.getStock(), 
                Math.abs(totalQty), 
                phi, 
                sessionController.getDepartment()
            );

            if (!returnFlag) {
                LOGGER.log(Level.WARNING, "Unable to deduct stock for item: {0}", bi.getItem().getName());
                // Reset quantities if stock deduction failed
                phi.setQty(0);
                phi.setFreeQty(0);
                pharmaceuticalBillItemFacade.edit(phi);
            }
        }
    }

    // Validation methods
    private boolean validateGrnReturn() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No GRN Return Bill");
            return false;
        }

        if (originalGrn == null) {
            JsfUtil.addErrorMessage("Please select a GRN to return items from");
            return false;
        }

        if (currentBill.getComments() == null || currentBill.getComments().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a return reason");
            return false;
        }

        if (billItems == null || billItems.isEmpty()) {
            JsfUtil.addErrorMessage("No items selected for return");
            return false;
        }

        boolean hasItems = false;
        for (BillItem bi : billItems) {
            if (bi.getPharmaceuticalBillItem().getQty() > 0 || 
                bi.getPharmaceuticalBillItem().getFreeQty() > 0) {
                hasItems = true;
                break;
            }
        }

        if (!hasItems) {
            JsfUtil.addErrorMessage("Please specify quantities to return");
            return false;
        }

        return true;
    }

    private boolean validateApproval() {
        if (approvedBill == null) {
            JsfUtil.addErrorMessage("No GRN Return for approval");
            return false;
        }

        if (requestedBill == null) {
            JsfUtil.addErrorMessage("No requested GRN Return found");
            return false;
        }

        if (billItems == null || billItems.isEmpty()) {
            JsfUtil.addErrorMessage("No items to approve");
            return false;
        }

        return true;
    }

    // Item management methods
    public void addItem() {
        if (currentBillItem.getItem() == null) {
            JsfUtil.addErrorMessage("Please select an item");
            return;
        }

        BillItem newBi = createBillItemFromGrnItem(currentBillItem.getItem());
        if (newBi != null) {
            billItems.add(newBi);
            calculateTotal();
        }

        currentBillItem = new BillItem();
    }

    private BillItem createBillItemFromGrnItem(Item item) {
        // Find the item in the original GRN
        PharmaceuticalBillItem originalPhi = findItemInGrn(item, originalGrn);
        if (originalPhi == null) {
            JsfUtil.addErrorMessage("Item not found in selected GRN");
            return null;
        }

        BillItem bi = new BillItem();
        bi.setItem(item);
        bi.setSearialNo(billItems.size());

        PharmaceuticalBillItem phi = new PharmaceuticalBillItem();
        phi.copy(originalPhi);
        phi.setBillItem(bi);
        phi.setQty(0.0);  // Default return quantity
        phi.setFreeQty(0.0);  // Default free return quantity

        bi.setPharmaceuticalBillItem(phi);

        return bi;
    }

    private PharmaceuticalBillItem findItemInGrn(Item item, Bill grn) {
        String sql = "SELECT phi FROM PharmaceuticalBillItem phi "
                   + "WHERE phi.billItem.bill = :grn "
                   + "AND phi.billItem.item = :item "
                   + "AND phi.billItem.retired = :ret";
        
        Map<String, Object> params = new HashMap<>();
        params.put("grn", grn);
        params.put("item", item);
        params.put("ret", false);
        
        List<PharmaceuticalBillItem> phis = pharmaceuticalBillItemFacade.findByJpql(sql, params);
        return phis.isEmpty() ? null : phis.get(0);
    }

    public void removeItem(BillItem bi) {
        billItems.remove(bi);
        calculateTotal();
    }

    public void removeSelected() {
        if (selectedBillItems != null) {
            billItems.removeAll(selectedBillItems);
            calculateTotal();
        }
    }

    // Calculation methods
    private void calculateTotal() {
        if (currentBill == null) return;
        
        double total = 0.0;
        for (BillItem bi : billItems) {
            PharmaceuticalBillItem phi = bi.getPharmaceuticalBillItem();
            double itemTotal = (phi.getQty() + phi.getFreeQty()) * phi.getPurchaseRateInUnit();
            bi.setNetValue(itemTotal);
            total += itemTotal;
        }
        
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    private void calculateApprovedTotal() {
        if (approvedBill == null) return;
        
        double total = 0.0;
        for (BillItem bi : billItems) {
            PharmaceuticalBillItem phi = bi.getPharmaceuticalBillItem();
            double itemTotal = (phi.getQty() + phi.getFreeQty()) * phi.getPurchaseRateInUnit();
            bi.setNetValue(itemTotal);
            total += itemTotal;
        }
        
        approvedBill.setTotal(total);
        approvedBill.setNetTotal(total);
    }

    // Event handlers
    public void onEdit(BillItem bi) {
        calculateTotal();
    }

    public void onFocus(BillItem bi) {
        // Handle focus events if needed
    }

    public void onGrnSelect() {
        if (selectedGrn != null) {
            originalGrn = selectedGrn;
            currentBill.setToInstitution(originalGrn.getFromInstitution());
            currentBill.setReferenceBill(originalGrn);
            generateItemsFromGrn();
        }
    }

    private void generateItemsFromGrn() {
        billItems.clear();
        
        if (originalGrn == null) return;
        
        String sql = "SELECT phi FROM PharmaceuticalBillItem phi "
                   + "WHERE phi.billItem.bill = :grn "
                   + "AND phi.billItem.retired = :ret";
        
        Map<String, Object> params = new HashMap<>();
        params.put("grn", originalGrn);
        params.put("ret", false);
        
        List<PharmaceuticalBillItem> grnItems = pharmaceuticalBillItemFacade.findByJpql(sql, params);
        
        for (PharmaceuticalBillItem grnPhi : grnItems) {
            BillItem bi = new BillItem();
            bi.setItem(grnPhi.getBillItem().getItem());
            bi.setSearialNo(billItems.size());

            PharmaceuticalBillItem phi = new PharmaceuticalBillItem();
            phi.copy(grnPhi);
            phi.setBillItem(bi);
            phi.setQty(0.0);  // Default return quantity
            phi.setFreeQty(0.0);  // Default free return quantity

            bi.setPharmaceuticalBillItem(phi);
            billItems.add(bi);
        }
    }

    private void generateBillItemsForUpdate() {
        billItems.clear();
        
        if (currentBill == null) return;
        
        String sql = "SELECT phi FROM PharmaceuticalBillItem phi "
                   + "WHERE phi.billItem.bill = :bill "
                   + "AND phi.billItem.retired = :ret";
        
        Map<String, Object> params = new HashMap<>();
        params.put("bill", currentBill);
        params.put("ret", false);
        
        List<PharmaceuticalBillItem> existingItems = pharmaceuticalBillItemFacade.findByJpql(sql, params);
        
        for (PharmaceuticalBillItem phi : existingItems) {
            BillItem bi = phi.getBillItem();
            bi.setSearialNo(billItems.size());
            billItems.add(bi);
        }
        
        originalGrn = currentBill.getReferenceBill();
    }

    private void generateBillItemsForApproval() {
        billItems.clear();
        
        if (requestedBill == null) return;
        
        String sql = "SELECT phi FROM PharmaceuticalBillItem phi "
                   + "WHERE phi.billItem.bill = :bill "
                   + "AND phi.billItem.retired = :ret";
        
        Map<String, Object> params = new HashMap<>();
        params.put("bill", requestedBill);
        params.put("ret", false);
        
        List<PharmaceuticalBillItem> requestedItems = pharmaceuticalBillItemFacade.findByJpql(sql, params);
        
        for (PharmaceuticalBillItem requestedPhi : requestedItems) {
            BillItem bi = new BillItem();
            bi.copy(requestedPhi.getBillItem());
            
            PharmaceuticalBillItem phi = new PharmaceuticalBillItem();
            phi.copy(requestedPhi);
            phi.setBillItem(bi);
            
            bi.setPharmaceuticalBillItem(phi);
            bi.setSearialNo(billItems.size());
            billItems.add(bi);
        }
    }

    // Auto-complete methods
    public List<Bill> completeGrn(String query) {
        String sql = "SELECT b FROM Bill b "
                   + "WHERE b.retired = :ret "
                   + "AND b.cancelled = :can "
                   + "AND b.billType = :bt "
                   + "AND b.institution = :ins "
                   + "AND (b.deptId LIKE :q OR b.fromInstitution.name LIKE :q) "
                   + "ORDER BY b.createdAt DESC";
        
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("can", false);
        params.put("bt", BillType.PharmacyGrnBill);
        params.put("ins", sessionController.getInstitution());
        params.put("q", "%" + query + "%");
        
        return billFacade.findByJpql(sql, params, 50);
    }

    // GRN Return Creation - following legacy pattern
    private void createReturnBillFromGrn(Bill grn) {
        resetBillValues();
        originalGrn = grn;
        
        // Create return bill
        currentBill = new RefundBill();
        currentBill.setBillType(BillType.PharmacyGrnReturn);
        currentBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_RETURN);
        currentBill.setReferenceBill(grn);
        currentBill.setToInstitution(grn.getFromInstitution());
        
        // Generate items from GRN (similar to legacy prepareReturnBill)
        generateItemsFromGrn();
    }

    // Utility methods
    public void displayItemDetails(BillItem bi) {
        // Implementation for displaying item details
    }

    public double getOriginalPurchaseRate(Item item) {
        if (originalGrn == null) return 0.0;
        
        PharmaceuticalBillItem originalPhi = findItemInGrn(item, originalGrn);
        return originalPhi != null ? originalPhi.getPurchaseRateInUnit() : 0.0;
    }

    public void resetBillValues() {
        currentBill = new RefundBill();
        currentBillItem = new BillItem();
        billItems = new ArrayList<>();
        selectedBillItems = new ArrayList<>();
        selectedItems = new ArrayList<>();
        printPreview = false;
        selectedGrn = null;
        originalGrn = null;
        requestedBill = null;
        approvedBill = null;
    }

    public void makeListNull() {
        // Reset search lists - implementation depends on search requirements
    }

    // Getters and Setters
    public Bill getCurrentBill() {
        if (currentBill == null) {
            currentBill = new RefundBill();
        }
        return currentBill;
    }

    public void setCurrentBill(Bill currentBill) {
        this.currentBill = currentBill;
    }

    public Bill getRequestedBill() {
        return requestedBill;
    }

    public void setRequestedBill(Bill requestedBill) {
        this.requestedBill = requestedBill;
    }

    public Bill getApprovedBill() {
        return approvedBill;
    }

    public void setApprovedBill(Bill approvedBill) {
        this.approvedBill = approvedBill;
    }

    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
        }
        return currentBillItem;
    }

    public void setCurrentBillItem(BillItem currentBillItem) {
        this.currentBillItem = currentBillItem;
    }

    public List<BillItem> getSelectedBillItems() {
        if (selectedBillItems == null) {
            selectedBillItems = new ArrayList<>();
        }
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
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

    public List<BillItem> getSelectedItems() {
        if (selectedItems == null) {
            selectedItems = new ArrayList<>();
        }
        return selectedItems;
    }

    public void setSelectedItems(List<BillItem> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public Bill getSelectedGrn() {
        return selectedGrn;
    }

    public void setSelectedGrn(Bill selectedGrn) {
        this.selectedGrn = selectedGrn;
    }

    public Bill getOriginalGrn() {
        return originalGrn;
    }

    public void setOriginalGrn(Bill originalGrn) {
        this.originalGrn = originalGrn;
    }

    public List<Item> getDealorItems() {
        if (dealorItems == null) {
            dealorItems = new ArrayList<>();
        }
        return dealorItems;
    }

    public void setDealorItems(List<Item> dealorItems) {
        this.dealorItems = dealorItems;
    }
}