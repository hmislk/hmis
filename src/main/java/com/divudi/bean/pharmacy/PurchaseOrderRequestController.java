/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.EnumController;
import com.divudi.bean.common.NotificationController;
import com.divudi.bean.common.SessionController;

import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.ItemsDistributorsFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.EmailFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.util.CommonFunctions;
import com.divudi.ejb.EmailManagerEjb;
import com.divudi.core.entity.AppEmail;
import com.divudi.core.data.MessageType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import java.io.Serializable;
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
 *
 * @author safrin
 */
@Named
@SessionScoped
public class PurchaseOrderRequestController implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(PurchaseOrderRequestController.class.getName());

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
    @EJB
    private EmailFacade emailFacade;
    @EJB
    private EmailManagerEjb emailManagerEjb;

    @Inject
    private SessionController sessionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    EnumController enumController;

    private Bill currentBill;
    private BillItem currentBillItem;
    private List<BillItem> selectedBillItems;
    private List<BillItem> billItems;
    private boolean printPreview;
    private double totalBillItemsCount;
    //private List<PharmaceuticalBillItem> pharmaceuticalBillItems;
    @Inject
    PharmacyCalculation pharmacyBillBean;
    private PaymentMethodData paymentMethodData;

    @Inject
    NotificationController notificationController;
    
    private String emailRecipient;

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
        getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRate(getPharmacyBean().getLastPurchaseRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));
        getCurrentBillItem().getPharmaceuticalBillItem().setRetailRate(getPharmacyBean().getLastRetailRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));

        getBillItems().add(getCurrentBillItem());

        calTotal();

        currentBillItem = null;
    }

    public void removeItem(BillItem bi) {
        Bill currentBill = getCurrentBill();
        if (currentBill == null || bi == null) {
            return;
        }

        Date now = new Date();

        bi.setRetired(true);
        bi.setRetirer(sessionController.getLoggedUser());
        bi.setRetiredAt(now);

        PharmaceuticalBillItem tmpPbi = bi.getPharmaceuticalBillItem();
        if (tmpPbi != null) {
            tmpPbi.setRetired(true);
            tmpPbi.setRetirer(sessionController.getLoggedUser());
            tmpPbi.setRetiredAt(now);
        }

        if (bi.getId() != null) {
            billItemFacade.edit(bi);
        }

        if (tmpPbi != null && tmpPbi.getId() != null) {
            pharmaceuticalBillItemFacade.edit(tmpPbi);
        }

        getBillItems().remove(bi);

        calTotal();

        currentBillItem = null;
    }

    @Inject
    private PharmacyController pharmacyController;

    public void onEdit(BillItem bi) {
        bi.getPharmaceuticalBillItem().setQty(bi.getQty());
        bi.setNetValue(bi.getPharmaceuticalBillItem().getQty() * bi.getPharmaceuticalBillItem().getPurchaseRate());
        calTotal();
    }

    public void displayItemDetails(BillItem bi) {
        getPharmacyController().fillItemDetails(bi.getItem());
    }

    public void saveBill() {

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);

        getCurrentBill().setDeptId(deptId);
        getCurrentBill().setInsId(deptId);

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
        notificationController.createNotification(getCurrentBill());

    }

    public void generateBillComponentsForAllSupplierItems(List<Item> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        if (getBillItems() == null) {
            setBillItems(new ArrayList<>());
        }

        int serialStart = getBillItems().size();

        for (Item i : items) {
            BillItem bi = new BillItem();
            bi.setItem(i);

            PharmaceuticalBillItem tmp = new PharmaceuticalBillItem();
            tmp.setBillItem(bi);
            bi.setPharmaceuticalBillItem(tmp);

            bi.setSearialNo(serialStart++);
            tmp.setPurchaseRate(getPharmacyBean().getLastPurchaseRate(i, getSessionController().getDepartment()));
            tmp.setRetailRate(getPharmacyBean().getLastRetailRate(i, getSessionController().getDepartment()));

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
            b.setRate(b.getPharmaceuticalBillItem().getPurchaseRate());
            b.setNetValue(b.getPharmaceuticalBillItem().getQty() * b.getPharmaceuticalBillItem().getPurchaseRate());
            b.setBill(getCurrentBill());
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            double qty;
            qty = b.getQty() + b.getPharmaceuticalBillItem().getFreeQty();
            if (qty <= 0.0) {
                b.setRetired(true);
                b.setRetirer(sessionController.getLoggedUser());
                b.setRetiredAt(new Date());
                b.setRetireComments("Retired at Finalising PO");

            }
            totalBillItemsCount = totalBillItemsCount + qty;
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
        if (getCurrentBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please Select Paymntmethod");
            return;
        }
        if (getBillItems() == null || getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Please add bill items");
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
        if (getCurrentBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a payment method.");
            return;
        }

        if (getBillItems() == null || getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Please add bill items.");
            return;
        }

        if (!allBillItemsValid(billItems)) {
            JsfUtil.addErrorMessage("Please ensure each item has quantity and purchase price.");
            return;
        }

        finalizeBill();
        totalBillItemsCount = 0;
        finalizeBillComponent();

        if (totalBillItemsCount == 0) {
            JsfUtil.addErrorMessage("Please enter item quantities for the bill.");
            return;
        }

        JsfUtil.addSuccessMessage("Request successfully finalized.");
        printPreview = true;
    }

    public void prepareEmailDialog() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }
        
        // Set default email if available
        if (currentBill.getToInstitution() != null && currentBill.getToInstitution().getEmail() != null) {
            emailRecipient = currentBill.getToInstitution().getEmail();
        } else {
            emailRecipient = "";
        }
    }

    public void sendPurchaseOrderEmail() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }
        
        if (emailRecipient == null || emailRecipient.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter recipient email");
            return;
        }

        String recipient = emailRecipient.trim();
        if (!CommonFunctions.isValidEmail(recipient)) {
            JsfUtil.addErrorMessage("Please enter a valid email address");
            return;
        }

        String body = generatePurchaseOrderHtml();
        if (body == null) {
            JsfUtil.addErrorMessage("Could not generate email body");
            return;
        }

        AppEmail email = new AppEmail();
        email.setCreatedAt(new Date());
        email.setCreater(sessionController.getLoggedUser());
        email.setReceipientEmail(recipient);
        email.setMessageSubject("Purchase Order Request");
        email.setMessageBody(body);
        email.setDepartment(sessionController.getLoggedUser().getDepartment());
        email.setInstitution(sessionController.getLoggedUser().getInstitution());
        email.setBill(currentBill);
        email.setMessageType(MessageType.Marketing);
        email.setSentSuccessfully(false);
        email.setPending(true);
        emailFacade.create(email);

        try {
            boolean success = emailManagerEjb.sendEmail(
                    java.util.Collections.singletonList(recipient),
                    body,
                    "Purchase Order Request",
                    true
            );
            email.setSentSuccessfully(success);
            email.setPending(!success);
            if (success) {
                email.setSentAt(new Date());
                JsfUtil.addSuccessMessage("Email Sent Successfully");
            } else {
                JsfUtil.addErrorMessage("Sending Email Failed");
            }
            emailFacade.edit(email);
        } catch (Exception ex) {
            JsfUtil.addErrorMessage("Sending Email Failed");
        }
    }

    private String generatePurchaseOrderHtml() {
        try {
            if (currentBill == null) {
                LOGGER.log(Level.SEVERE, "Current bill is null when generating purchase order HTML");
                return null;
            }
            
            StringBuilder html = new StringBuilder();
            html.append("<html><head><title>Purchase Order Request</title></head><body>");
            html.append("<div style='font-family: Arial, sans-serif; padding: 20px;'>");
            
            // Institution header
            if (currentBill.getCreater() != null && currentBill.getCreater().getInstitution() != null) {
                html.append("<div style='text-align: center; margin-bottom: 20px;'>");
                html.append("<h2>").append(currentBill.getCreater().getInstitution().getName() != null ? currentBill.getCreater().getInstitution().getName() : "").append("</h2>");
                if (currentBill.getCreater().getInstitution().getAddress() != null) {
                    html.append("<p>").append(currentBill.getCreater().getInstitution().getAddress()).append("</p>");
                }
                if (currentBill.getCreater().getInstitution().getPhone() != null) {
                    html.append("<p>Phone: ").append(currentBill.getCreater().getInstitution().getPhone()).append("</p>");
                }
                html.append("</div>");
            }
            
            html.append("<h3 style='text-align: center; text-decoration: underline;'>Purchase Order Request</h3>");
            
            // Order details
            html.append("<table style='width: 100%; margin-bottom: 20px;'>");
            html.append("<tr><td><strong>Order No:</strong></td><td>").append(currentBill.getDeptId() != null ? currentBill.getDeptId() : "").append("</td></tr>");
            if (currentBill.getDepartment() != null) {
                html.append("<tr><td><strong>Order Department:</strong></td><td>").append(currentBill.getDepartment().getName() != null ? currentBill.getDepartment().getName() : "").append("</td></tr>");
            }
            if (currentBill.getToInstitution() != null) {
                html.append("<tr><td><strong>Supplier:</strong></td><td>").append(currentBill.getToInstitution().getName() != null ? currentBill.getToInstitution().getName() : "").append("</td></tr>");
                html.append("<tr><td><strong>Supplier Code:</strong></td><td>").append(currentBill.getToInstitution().getCode() != null ? currentBill.getToInstitution().getCode() : "").append("</td></tr>");
                if (currentBill.getToInstitution().getPhone() != null) {
                    html.append("<tr><td><strong>Supplier Phone:</strong></td><td>").append(currentBill.getToInstitution().getPhone()).append("</td></tr>");
                }
                if (currentBill.getToInstitution().getAddress() != null) {
                    html.append("<tr><td><strong>Supplier Address:</strong></td><td>").append(currentBill.getToInstitution().getAddress()).append("</td></tr>");
                }
            }
            html.append("<tr><td><strong>Payment Method:</strong></td><td>").append(currentBill.getPaymentMethod() != null ? currentBill.getPaymentMethod().toString() : "").append("</td></tr>");
            html.append("<tr><td><strong>Consignment:</strong></td><td>").append(currentBill.isConsignment() ? "Yes" : "No").append("</td></tr>");
            html.append("</table>");
            
            // Items table
            html.append("<table border='1' style='width: 100%; border-collapse: collapse; margin-bottom: 20px;'>");
            html.append("<thead style='background-color: #f0f0f0;'>");
            html.append("<tr>");
            html.append("<th style='padding: 8px;'>Item Code</th>");
            html.append("<th style='padding: 8px;'>Item Name</th>");
            html.append("<th style='padding: 8px;'>Qty</th>");
            html.append("<th style='padding: 8px;'>Free Qty</th>");
            html.append("<th style='padding: 8px;'>Purchase Rate</th>");
            html.append("<th style='padding: 8px;'>Purchase Value</th>");
            html.append("</tr></thead><tbody>");
            
            if (billItems != null) {
                for (BillItem bi : billItems) {
                    if (bi != null && !bi.isRetired() && bi.getItem() != null) {
                        html.append("<tr>");
                        html.append("<td style='padding: 8px;'>").append(bi.getItem().getCode() != null ? bi.getItem().getCode() : "").append("</td>");
                        html.append("<td style='padding: 8px;'>").append(bi.getItem().getName() != null ? bi.getItem().getName() : "").append("</td>");
                        html.append("<td style='padding: 8px; text-align: right;'>");
                        if (bi.getPharmaceuticalBillItem() != null) {
                            html.append(String.format("%,.0f", bi.getPharmaceuticalBillItem().getQty()));
                        }
                        html.append("</td>");
                        html.append("<td style='padding: 8px; text-align: right;'>");
                        if (bi.getPharmaceuticalBillItem() != null) {
                            html.append(String.format("%,.0f", bi.getPharmaceuticalBillItem().getFreeQty()));
                        }
                        html.append("</td>");
                        html.append("<td style='padding: 8px; text-align: right;'>");
                        if (bi.getPharmaceuticalBillItem() != null) {
                            html.append(String.format("%,.2f", bi.getPharmaceuticalBillItem().getPurchaseRate()));
                        }
                        html.append("</td>");
                        html.append("<td style='padding: 8px; text-align: right;'>").append(String.format("%,.2f", bi.getNetValue())).append("</td>");
                        html.append("</tr>");
                    }
                }
            }
            
            html.append("</tbody>");
            html.append("<tfoot style='font-weight: bold;'>");
            html.append("<tr>");
            html.append("<td colspan='5' style='padding: 8px; text-align: right;'>Net Total:</td>");
            html.append("<td style='padding: 8px; text-align: right;'>").append(String.format("%,.2f", currentBill.getNetTotal())).append("</td>");
            html.append("</tr></tfoot></table>");
            
            // Footer details
            html.append("<div style='margin-top: 20px;'>");
            if (currentBill.getCreater() != null && currentBill.getCreater().getWebUserPerson() != null) {
                html.append("<p><strong>Order Initiated By:</strong> ").append(currentBill.getCreater().getWebUserPerson().getName() != null ? currentBill.getCreater().getWebUserPerson().getName() : "").append("</p>");
            }
            if (currentBill.getCheckedBy() != null) {
                html.append("<p><strong>Order Finalized By:</strong> ").append(currentBill.getCheckedBy().getName() != null ? currentBill.getCheckedBy().getName() : "").append("</p>");
            }
            if (currentBill.getCheckeAt() != null) {
                html.append("<p><strong>Order Finalized At:</strong> ").append(CommonFunctions.formatDate(currentBill.getCheckeAt(), "dd/MM/yyyy HH:mm:ss")).append("</p>");
            }
            html.append("<p><strong>Generated At:</strong> ").append(CommonFunctions.formatDate(new Date(), "dd/MM/yyyy HH:mm:ss")).append("</p>");
            html.append("<p><strong>Total:</strong> ").append(String.format("%,.2f", currentBill.getNetTotal())).append("</p>");
            html.append("</div>");
            
            html.append("</div></body></html>");
            return html.toString();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating purchase order HTML", e);
            return null;
        }
    }

    // Extracted magic number for clarity
    private static final double MIN_PURCHASE_RATE = 0.00001;

    private boolean allBillItemsValid(List<BillItem> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }
        for (BillItem bi : items) {
            // Null‐check to avoid NPE when accessing pharmaceutical details
            if (bi.getPharmaceuticalBillItem() == null) {
                return false;
            }
            if ((bi.getQty() + bi.getPharmaceuticalBillItem().getFreeQty()) < 1) {
                return false;
            }
            // Use named constant instead of hardcoded threshold
            if (bi.getPharmaceuticalBillItem().getPurchaseRate() < MIN_PURCHASE_RATE) {
                return false;
            }
        }
        return true;
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

            String key = "Pharmacy Purchase Order Default Payment Method";
            String strEnumValue = configOptionApplicationController.getEnumValueByKey(key);
            PaymentMethod pm = enumController.getEnumValue(PaymentMethod.class, strEnumValue);

            currentBill.setPaymentMethod(pm);
            boolean consignmentEnabled = configOptionApplicationController.getBooleanValueByKey("Consignment Option is checked in new Pharmacy Purchasing Bills", false);
            currentBill.setConsignment(consignmentEnabled);
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

    public double getTotalBillItemsCount() {
        return totalBillItemsCount;
    }

    public void setTotalBillItemsCount(double totalBillItemsCount) {
        this.totalBillItemsCount = totalBillItemsCount;
    }

    public EmailFacade getEmailFacade() {
        return emailFacade;
    }

    public void setEmailFacade(EmailFacade emailFacade) {
        this.emailFacade = emailFacade;
    }

    public EmailManagerEjb getEmailManagerEjb() {
        return emailManagerEjb;
    }

    public void setEmailManagerEjb(EmailManagerEjb emailManagerEjb) {
        this.emailManagerEjb = emailManagerEjb;
    }

    public String getEmailRecipient() {
        return emailRecipient;
    }

    public void setEmailRecipient(String emailRecipient) {
        this.emailRecipient = emailRecipient;
    }

}
