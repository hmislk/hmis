package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.pharmacy.PurchaseOrderRequestLineData;
import com.divudi.core.entity.AppEmail;
import com.divudi.core.data.MessageType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.EmailFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.EmailManagerEjb;
import com.divudi.service.pharmacy.PurchaseOrderApprovingNativeSqlService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * SessionScoped controller for the native-SQL Purchase Order Approving page.
 * Native SQL: the APPROVED bill's own create/update, billitem+PBI writes
 * (via the service). JPA (unchanged): the requested bill (read-only except
 * for the referenceBill cross-link write), rate/email infra.
 * Related issue: #22738
 */
@Named
@SessionScoped
public class PurchaseOrderApprovingNativeSqlController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(PurchaseOrderApprovingNativeSqlController.class.getName());

    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private ConfigOptionController configOptionController;
    @Inject
    private PharmacyController pharmacyController;

    @EJB
    private PurchaseOrderApprovingNativeSqlService purchaseOrderApprovingNativeSqlService;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private EmailFacade emailFacade;
    @EJB
    private EmailManagerEjb emailManagerEjb;
    @EJB
    private com.divudi.core.facade.ItemFacade itemFacade;

    private Bill requestedBill;
    private Bill approvedBill;
    private List<BillItem> billItems;
    private List<BillItem> selectedItems;
    private boolean printPreview;
    private String emailRecipient;

    private boolean isAuthorized(String action, String requiredPrivilege) {
        if (webUserController == null || sessionController == null) {
            LOGGER.log(Level.SEVERE, "Authorization failed - missing controllers: action={0}, userId=null, billId={1}",
                    new Object[]{action, requestedBill != null ? requestedBill.getId() : "null"});
            return false;
        }
        if (!webUserController.hasPrivilege(requiredPrivilege)) {
            Long userId = sessionController.getLoggedUser() != null ? sessionController.getLoggedUser().getId() : null;
            Long billId = requestedBill != null ? requestedBill.getId() : null;
            LOGGER.log(Level.WARNING, "SECURITY: Unauthorized Purchase Order Approving access attempt - action={0}, userId={1}, billId={2}, requiredPrivilege={3}",
                    new Object[]{action, userId, billId, requiredPrivilege});
            JsfUtil.addErrorMessage("You don't have permission to " + action.toLowerCase() + " purchase orders.");
            return false;
        }
        return true;
    }

    public void resetBillValues() {
        requestedBill = null;
        approvedBill = null;
        billItems = new ArrayList<>();
        selectedItems = null;
        printPreview = false;
    }

    public Bill getApprovedBill() {
        if (approvedBill == null) {
            approvedBill = new BilledBill();
            approvedBill.setBillType(BillType.PharmacyOrderApprove);
            approvedBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER_APPROVAL);
            if (requestedBill != null) {
                approvedBill.setConsignment(requestedBill.isConsignment());
                approvedBill.setDepartmentType(requestedBill.getDepartmentType());
            }
        }
        return approvedBill;
    }

    // synchronized: see Task 3 doc comment on approve() for the production
    // incident (GRN item duplication, PO/RH/GSK/26/01093) this guards against.
    public synchronized String navigateToPurchaseOrderApproval(Long requestedBillId) {
        if (requestedBillId == null) {
            JsfUtil.addErrorMessage("No Bill");
            return "";
        }
        Bill bill = billFacade.find(requestedBillId);
        if (bill == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return "";
        }
        if (bill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_ORDER) {
            JsfUtil.addErrorMessage("Bill is not a finalized purchase order request");
            return "";
        }
        if (bill.isRetired() || bill.isCancelled()) {
            JsfUtil.addErrorMessage("Bill is retired or cancelled");
            return "";
        }
        if (bill.getDepartment() == null || sessionController.getLoggedUser() == null
                || !bill.getDepartment().equals(sessionController.getDepartment())) {
            JsfUtil.addErrorMessage("You are not authorized to view this purchase order");
            return "";
        }
        if (bill.getReferenceBill() != null) {
            JsfUtil.addErrorMessage("This purchase order is already approved");
            return "";
        }

        resetBillValues();
        requestedBill = bill;
        getApprovedBill().setPaymentMethod(requestedBill.getPaymentMethod());
        getApprovedBill().setToInstitution(requestedBill.getToInstitution());
        getApprovedBill().setCreditDuration(requestedBill.getCreditDuration());
        generateBillComponent();
        printPreview = false;
        return "/pharmacy/pharmacy_purhcase_order_approving_native?faces-redirect=true";
    }

    public void generateBillComponent() {
        billItems = new ArrayList<>();
        if (requestedBill == null) {
            return;
        }
        List<PurchaseOrderRequestLineData> lines = purchaseOrderApprovingNativeSqlService.loadRequestedLines(requestedBill.getId());
        for (PurchaseOrderRequestLineData line : lines) {
            Item item = itemFacade.find(line.getItemId());
            if (item == null) {
                continue;
            }
            BillItem bi = new BillItem();
            bi.setItem(item);
            bi.setSearialNo(line.getSerialNo());

            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
            pbi.setBillItem(bi);
            pbi.setCreatedAt(new Date());
            pbi.setCreater(sessionController.getLoggedUser());
            bi.setPharmaceuticalBillItem(pbi);

            bi.getBillItemFinanceDetails().setQuantity(line.getQuantity());
            bi.getBillItemFinanceDetails().setFreeQuantity(line.getFreeQuantity());
            bi.getBillItemFinanceDetails().setLineGrossRate(line.getPurchaseRate());
            bi.getBillItemFinanceDetails().setRetailSaleRate(line.getRetailRate());
            bi.getBillItemFinanceDetails().setUnitsPerPack(line.getUnitsPerPack());

            recalculateLineValues(bi);
            billItems.add(bi);
        }
        calculateBillTotals();
    }

    public void onEdit(BillItem bi) {
        if (bi.getBillItemFinanceDetails() != null
                && configOptionController.getBooleanValueByKey("Pharmacy Purchase - Quantity Must Be Integer", true)) {
            BigDecimal qty = bi.getBillItemFinanceDetails().getQuantity();
            BigDecimal freeQty = bi.getBillItemFinanceDetails().getFreeQuantity();
            if (qty != null && qty.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                bi.getBillItemFinanceDetails().setQuantity(BigDecimal.ZERO);
                recalculateLineValues(bi);
                JsfUtil.addErrorMessage("Please enter only whole numbers (integers) for quantity. Decimal values are not allowed.");
                calculateBillTotals();
                return;
            }
            if (freeQty != null && freeQty.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                bi.getBillItemFinanceDetails().setFreeQuantity(BigDecimal.ZERO);
                recalculateLineValues(bi);
                JsfUtil.addErrorMessage("Please enter only whole numbers (integers) for free quantity. Decimal values are not allowed.");
                calculateBillTotals();
                return;
            }
        }
        recalculateLineValues(bi);
        calculateBillTotals();
    }

    private void recalculateLineValues(BillItem bi) {
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }
        BigDecimal qty = bi.getBillItemFinanceDetails().getQuantity();
        BigDecimal purchaseRate = bi.getBillItemFinanceDetails().getLineGrossRate();
        if (qty == null) qty = BigDecimal.ZERO;
        if (purchaseRate == null) purchaseRate = BigDecimal.ZERO;

        BigDecimal grossValue = purchaseRate.multiply(qty);
        bi.setRate(purchaseRate.doubleValue());
        bi.setNetRate(purchaseRate.doubleValue());
        bi.setGrossValue(grossValue.doubleValue());
        bi.setNetValue(grossValue.doubleValue());
        bi.getBillItemFinanceDetails().setLineGrossTotal(grossValue);
        bi.getBillItemFinanceDetails().setLineNetTotal(grossValue);
    }

    private void calculateBillTotals() {
        double total = 0.0;
        int serialNo = 0;
        for (BillItem bi : billItems) {
            if (bi == null || bi.isRetired()) {
                continue;
            }
            bi.setSearialNo(serialNo++);
            total += bi.getNetValue();
        }
        getApprovedBill().setNetTotal(total);
        getApprovedBill().setTotal(total);
    }

    public void removeItem(BillItem billItem) {
        if (billItem == null || !billItems.contains(billItem)) {
            JsfUtil.addErrorMessage("Item not found or already removed");
            return;
        }
        billItems.remove(billItem);
        calculateBillTotals();
    }

    public void removeSelected() {
        if (selectedItems == null || selectedItems.isEmpty()) {
            JsfUtil.addErrorMessage("No items selected to remove");
            return;
        }
        billItems.removeAll(selectedItems);
        calculateBillTotals();
        selectedItems = null;
    }

    // synchronized: a double-submit on the Approve button (no confirm-then-review
    // gap, or a resubmitted ajax="false" postback) let two requests race through
    // the same in-memory billItems list before either had persisted -- both saw
    // BillItem.id == null and created every line twice, duplicating every GRN
    // item (Ruhunu PO/RH/GSK/26/01093, same bug class as Phase 1's #21417 guard).
    public synchronized void approve() {
        if (!isAuthorized("APPROVE", "PurchaseOrdersApprovel")) {
            return;
        }
        if (requestedBill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }
        if (requestedBill.getReferenceBill() != null) {
            JsfUtil.addErrorMessage("This purchase order is already approved");
            return;
        }
        if (getApprovedBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select Paymentmethod");
            return;
        }
        if (billItems == null || billItems.isEmpty()) {
            JsfUtil.addErrorMessage("Please add bill items");
            return;
        }
        for (BillItem bi : billItems) {
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            if (pbi == null) {
                JsfUtil.addErrorMessage("Missing pharmaceutical details for item: " + bi.getItem().getName());
                return;
            }
            double totalQty = bi.getBillItemFinanceDetails().getQuantity().doubleValue()
                    + bi.getBillItemFinanceDetails().getFreeQuantity().doubleValue();
            if (totalQty <= 0) {
                JsfUtil.addErrorMessage("Item '" + bi.getItem().getName() + "' has zero quantity and free quantity");
                return;
            }
            if (bi.getBillItemFinanceDetails().getLineGrossRate() == null
                    || bi.getBillItemFinanceDetails().getLineGrossRate().doubleValue() <= 0) {
                JsfUtil.addErrorMessage("Item '" + bi.getItem().getName() + "' has invalid purchase price");
                return;
            }
        }

        calculateBillTotals();

        String[] billNumbers = createAndAssignBillNumber();
        long approvedBillId = purchaseOrderApprovingNativeSqlService.createApprovedBill(
                requestedBill.getId(),
                sessionController.getLoggedUser().getDepartment().getId(),
                sessionController.getLoggedUser().getDepartment().getInstitution().getId(),
                requestedBill.getDepartment().getId(),
                requestedBill.getInstitution().getId(),
                sessionController.getLoggedUser().getId(),
                billNumbers[0],
                billNumbers[1]);
        approvedBill = billFacade.find(approvedBillId);
        approvedBill.setPaymentMethod(getApprovedBill().getPaymentMethod());
        approvedBill.setToInstitution(getApprovedBill().getToInstitution());
        approvedBill.setCreditDuration(getApprovedBill().getCreditDuration());
        approvedBill.setConsignment(getApprovedBill().isConsignment());
        approvedBill.setDepartmentType(getApprovedBill().getDepartmentType());

        purchaseOrderApprovingNativeSqlService.updateApprovedBillHeader(
                approvedBillId,
                approvedBill.getToInstitution() != null ? approvedBill.getToInstitution().getId() : null,
                approvedBill.getPaymentMethod(),
                approvedBill.getCreditDuration(),
                approvedBill.isConsignment(),
                approvedBill.getDepartmentType(),
                approvedBill.getComments(),
                sessionController.getLoggedUser().getId());

        for (BillItem bi : billItems) {
            PurchaseOrderRequestLineData line = new PurchaseOrderRequestLineData();
            line.setItemId(bi.getItem().getId());
            line.setAmpp(bi.getItem() instanceof Ampp);
            line.setQuantity(bi.getBillItemFinanceDetails().getQuantity());
            line.setFreeQuantity(bi.getBillItemFinanceDetails().getFreeQuantity());
            line.setPurchaseRate(bi.getBillItemFinanceDetails().getLineGrossRate());
            line.setRetailRate(bi.getBillItemFinanceDetails().getRetailSaleRate());
            line.setUnitsPerPack(bi.getBillItemFinanceDetails().getUnitsPerPack());
            line.setSerialNo(bi.getSearialNo());
            line.setCreaterId(sessionController.getLoggedUser().getId());
            purchaseOrderApprovingNativeSqlService.saveApprovedLine(approvedBillId, line);
        }

        purchaseOrderApprovingNativeSqlService.retireZeroQtyApprovedLines(approvedBillId, sessionController.getLoggedUser().getId());

        approvedBill = billFacade.find(approvedBillId);
        approvedBill.setApproveAt(new Date());
        approvedBill.setApproveUser(sessionController.getLoggedUser());
        billFacade.edit(approvedBill);

        // The one JPA write in this phase that touches a bill this controller
        // does not own the writes for -- required to stay JPA merge, never
        // native SQL, per the master issue's L2-cache-coherence rule.
        requestedBill.setReferenceBill(approvedBill);
        billFacade.edit(requestedBill);

        printPreview = true;
        JsfUtil.addSuccessMessage("Purchase order approved successfully.");
    }

    private String[] createAndAssignBillNumber() {
        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_APPROVAL, "");
        if (billSuffix == null || billSuffix.trim().isEmpty()) {
            configOptionApplicationController.setLongTextValueByKey("Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_APPROVAL, "POA");
        }

        boolean stratInsDeptYear = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Purchase Order Approvals - Prefix + Institution Code + Department Code + Year + Yearly Number and Yearly Number", false);
        boolean stratInsYear = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Purchase Order Approvals - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);
        boolean stratInsIdInsYear = configOptionApplicationController.getBooleanValueByKey("Institution Number Generation Strategy for Purchase Order Approvals - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);

        String deptId;
        if (stratInsDeptYear) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_APPROVAL);
        } else if (stratInsYear) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_APPROVAL);
        } else {
            deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_APPROVAL);
        }

        String insId;
        if (stratInsIdInsYear) {
            insId = billNumberBean.institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_APPROVAL);
        } else {
            insId = deptId;
        }

        return new String[]{deptId, insId};
    }

    private static String esc(String value) {
        return value != null ? org.apache.commons.text.StringEscapeUtils.escapeHtml4(value) : "";
    }

    public void prepareEmailDialog() {
        if (approvedBill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }
        if (approvedBill.getToInstitution() != null && approvedBill.getToInstitution().getEmail() != null) {
            emailRecipient = approvedBill.getToInstitution().getEmail();
        } else {
            emailRecipient = "";
        }
    }

    public void sendPurchaseOrderEmail() {
        if (approvedBill == null) {
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
        email.setMessageSubject("Purchase Order");
        email.setMessageBody(body);
        email.setDepartment(sessionController.getLoggedUser().getDepartment());
        email.setInstitution(sessionController.getLoggedUser().getInstitution());
        email.setBill(approvedBill);
        email.setMessageType(MessageType.Marketing);
        email.setSentSuccessfully(false);
        email.setPending(true);
        emailFacade.create(email);

        try {
            boolean success = emailManagerEjb.sendEmail(
                    java.util.Collections.singletonList(recipient), body, "Purchase Order", true);
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
            if (approvedBill == null) {
                return null;
            }
            StringBuilder html = new StringBuilder();
            html.append("<html><head><title>Purchase Order</title></head><body>");
            html.append("<div style='font-family: Arial, sans-serif; padding: 20px;'>");

            if (approvedBill.getCreater() != null && approvedBill.getCreater().getInstitution() != null) {
                html.append("<div style='text-align: center; margin-bottom: 20px;'>");
                html.append("<h2>").append(esc(approvedBill.getCreater().getInstitution().getName())).append("</h2>");
                if (approvedBill.getCreater().getInstitution().getAddress() != null) {
                    html.append("<p>").append(esc(approvedBill.getCreater().getInstitution().getAddress())).append("</p>");
                }
                if (approvedBill.getCreater().getInstitution().getPhone() != null) {
                    html.append("<p>Phone: ").append(esc(approvedBill.getCreater().getInstitution().getPhone())).append("</p>");
                }
                html.append("</div>");
            }

            html.append("<h3 style='text-align: center; text-decoration: underline;'>Purchase Order</h3>");
            html.append("<table style='width: 100%; margin-bottom: 20px;'>");
            html.append("<tr><td><strong>Order No:</strong></td><td>").append(esc(approvedBill.getDeptId())).append("</td></tr>");
            if (approvedBill.getDepartment() != null) {
                html.append("<tr><td><strong>Order Department:</strong></td><td>").append(esc(approvedBill.getDepartment().getName())).append("</td></tr>");
            }
            if (approvedBill.getToInstitution() != null) {
                html.append("<tr><td><strong>Supplier:</strong></td><td>").append(esc(approvedBill.getToInstitution().getName())).append("</td></tr>");
                html.append("<tr><td><strong>Supplier Code:</strong></td><td>").append(esc(approvedBill.getToInstitution().getCode())).append("</td></tr>");
                if (approvedBill.getToInstitution().getPhone() != null) {
                    html.append("<tr><td><strong>Supplier Phone:</strong></td><td>").append(esc(approvedBill.getToInstitution().getPhone())).append("</td></tr>");
                }
                if (approvedBill.getToInstitution().getAddress() != null) {
                    html.append("<tr><td><strong>Supplier Address:</strong></td><td>").append(esc(approvedBill.getToInstitution().getAddress())).append("</td></tr>");
                }
            }
            html.append("<tr><td><strong>Payment Method:</strong></td><td>").append(approvedBill.getPaymentMethod() != null ? approvedBill.getPaymentMethod().toString() : "").append("</td></tr>");
            html.append("<tr><td><strong>Consignment:</strong></td><td>").append(approvedBill.isConsignment() ? "Yes" : "No").append("</td></tr>");
            html.append("</table>");

            html.append("<table border='1' style='width: 100%; border-collapse: collapse; margin-bottom: 20px;'>");
            html.append("<thead style='background-color: #f0f0f0;'>");
            html.append("<tr><th style='padding: 8px;'>Item Code</th><th style='padding: 8px;'>Item Name</th>");
            html.append("<th style='padding: 8px;'>Qty</th><th style='padding: 8px;'>Free Qty</th>");
            html.append("<th style='padding: 8px;'>Purchase Rate</th><th style='padding: 8px;'>Purchase Value</th></tr></thead><tbody>");

            if (billItems != null) {
                for (BillItem bi : billItems) {
                    if (bi != null && !bi.isRetired() && bi.getItem() != null) {
                        html.append("<tr>");
                        html.append("<td style='padding: 8px;'>").append(esc(bi.getItem().getCode())).append("</td>");
                        html.append("<td style='padding: 8px;'>").append(esc(bi.getItem().getName())).append("</td>");
                        html.append("<td style='padding: 8px; text-align: right;'>");
                        if (bi.getPharmaceuticalBillItem() != null) {
                            html.append(String.format("%,.0f", bi.getPharmaceuticalBillItem().getQty()));
                        }
                        html.append("</td><td style='padding: 8px; text-align: right;'>");
                        if (bi.getPharmaceuticalBillItem() != null) {
                            html.append(String.format("%,.0f", bi.getPharmaceuticalBillItem().getFreeQty()));
                        }
                        html.append("</td><td style='padding: 8px; text-align: right;'>");
                        if (bi.getPharmaceuticalBillItem() != null) {
                            html.append(String.format("%,.2f", bi.getPharmaceuticalBillItem().getPurchaseRate()));
                        }
                        html.append("</td><td style='padding: 8px; text-align: right;'>").append(String.format("%,.2f", bi.getNetValue())).append("</td>");
                        html.append("</tr>");
                    }
                }
            }

            html.append("</tbody><tfoot style='font-weight: bold;'><tr>");
            html.append("<td colspan='5' style='padding: 8px; text-align: right;'>Net Total:</td>");
            html.append("<td style='padding: 8px; text-align: right;'>").append(String.format("%,.2f", approvedBill.getNetTotal())).append("</td>");
            html.append("</tr></tfoot></table>");

            html.append("<div style='margin-top: 20px;'>");
            if (approvedBill.getCreater() != null && approvedBill.getCreater().getWebUserPerson() != null) {
                html.append("<p><strong>Order Initiated By:</strong> ").append(esc(approvedBill.getCreater().getWebUserPerson().getName())).append("</p>");
            }
            if (approvedBill.getCheckedBy() != null) {
                html.append("<p><strong>Order Finalized By:</strong> ").append(esc(approvedBill.getCheckedBy().getName())).append("</p>");
            }
            if (approvedBill.getCheckeAt() != null) {
                html.append("<p><strong>Order Finalized At:</strong> ").append(CommonFunctions.formatDate(approvedBill.getCheckeAt(), "dd/MM/yyyy HH:mm:ss")).append("</p>");
            }
            html.append("<p><strong>Generated At:</strong> ").append(CommonFunctions.formatDate(new Date(), "dd/MM/yyyy HH:mm:ss")).append("</p>");
            html.append("<p><strong>Total:</strong> ").append(String.format("%,.2f", approvedBill.getNetTotal())).append("</p>");
            html.append("</div></div></body></html>");
            return html.toString();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating purchase order HTML", e);
            return null;
        }
    }

    public void displayItemDetails(BillItem bi) {
        pharmacyController.fillItemDetails(bi.getItem());
    }

    public void onFocus(BillItem bi) {
        pharmacyController.setPharmacyItem(bi.getItem());
    }

    public Bill getRequestedBill() {
        return requestedBill;
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

    public String getEmailRecipient() {
        return emailRecipient;
    }

    public void setEmailRecipient(String emailRecipient) {
        this.emailRecipient = emailRecipient;
    }
}
