/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.NotificationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.ejb.BillNumberGenerator;

import com.divudi.ejb.PharmacyBean;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.EmailFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.ejb.EmailManagerEjb;
import com.divudi.core.entity.AppEmail;
import com.divudi.core.data.MessageType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.LazyDataModel;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class PurchaseOrderController implements Serializable {

    @Inject
    private SessionController sessionController;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private EmailFacade emailFacade;
    @EJB
    private EmailManagerEjb emailManagerEjb;
    ///////////////
    private Bill requestedBill;
    private Bill aprovedBill;
    private Date fromDate;
    Date toDate;
    private boolean printPreview;
    private String txtSearch;
    /////////////
//    private List<PharmaceuticalBillItem> pharmaceuticalBillItems;
    private List<PharmaceuticalBillItem> filteredValue;
    private List<BillItem> billItems;
    private List<BillItem> selectedItems;
    private List<Bill> billsToApprove;
    private List<Bill> bills;
    private SearchKeyword searchKeyword;
    // private List<BillItem> billItems;
    // List<PharmaceuticalBillItem> pharmaceuticalBillItems;
    //////////

    private LazyDataModel<Bill> searchBills;

    private PaymentMethodData paymentMethodData;
    private double totalBillItemsCount;
    @Inject
    NotificationController notificationController;
    
    private String emailRecipient;

    public void removeSelected() {
        //  //System.err.println("1");
        if (selectedItems == null) {
            JsfUtil.addErrorMessage("Please select items");
            return;
        }

        //System.err.println("3");
        for (BillItem b : selectedItems) {
            //  //System.err.println("4");
            getBillItems().remove(b.getSearialNo());
            calTotal();
        }

        selectedItems = null;
    }

    public void removeItem(BillItem billItem) {
        getBillItems().remove(billItem.getSearialNo());
        calTotal();
    }

    private int maxResult = 50;

    public void clearList() {
        filteredValue = null;
        billsToApprove = null;
        printPreview = true;
        billItems = null;
        aprovedBill = null;
        requestedBill = null;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public String navigateToPurchaseOrderApproval() {
        Bill temRequestedBill = requestedBill;
        clearList();
        requestedBill = temRequestedBill;
        getAprovedBill().setPaymentMethod(getRequestedBill().getPaymentMethod());
        getAprovedBill().setToInstitution(getRequestedBill().getToInstitution());
        getAprovedBill().setCreditDuration(getRequestedBill().getCreditDuration());
        generateBillComponent();
        printPreview = false;
        return "/pharmacy/pharmacy_purhcase_order_approving?faces-redirect=true";
    }

    public String approve() {
        if (getAprovedBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select Paymentmethod");
            return "";
        }

        if (billItems == null || billItems.isEmpty()) {
            JsfUtil.addErrorMessage("Please add bill items");
            return "";
        }

        for (BillItem bis : billItems) {
            PharmaceuticalBillItem pbi = bis.getPharmaceuticalBillItem();
            if (pbi == null) {
                JsfUtil.addErrorMessage("Missing pharmaceutical details for item: " + bis.getItem().getName());
                return "";
            }

            double totalQty = pbi.getQty() + pbi.getFreeQty();
            if (totalQty <= 0) {
                JsfUtil.addErrorMessage("Item '" + bis.getItem().getName() + "' has zero quantity and free quantity");
                return "";
            }

            if (pbi.getPurchaseRate() <= 0) {
                JsfUtil.addErrorMessage("Item '" + bis.getItem().getName() + "' has invalid purchase price");
                return "";
            }
        }

        calTotal();
        saveBill();
        saveBillComponent();

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(
                getSessionController().getDepartment(),
                BillTypeAtomic.PHARMACY_ORDER_APPROVAL
        );

        getAprovedBill().setDeptId(deptId);
        getAprovedBill().setInsId(deptId);
        getAprovedBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER_APPROVAL);

        billFacade.edit(getAprovedBill());
        notificationController.createNotification(getAprovedBill());

        getRequestedBill().setReferenceBill(getAprovedBill());
        getBillFacade().edit(getRequestedBill());

        printPreview = true;
        return "";
    }

    public String viewRequestedList() {
        clearList();
        return "/pharmacy_purhcase_order_list_to_approve?faces-redirect=true";
    }

    @Inject
    private PharmacyController pharmacyController;

    public void onEdit(BillItem tmp) {
        tmp.setNetValue(tmp.getPharmaceuticalBillItem().getQty() * tmp.getPharmaceuticalBillItem().getPurchaseRate());
        calTotal();
    }

    public void onFocus(BillItem ph) {
        getPharmacyController().setPharmacyItem(ph.getItem());
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Bill getRequestedBill() {
        if (requestedBill == null) {
            requestedBill = new BilledBill();
        }
        return requestedBill;
    }

    public void saveBill() {

        getAprovedBill().setCreditDuration(getAprovedBill().getCreditDuration());
//        getAprovedBill().setPaymentMethod(getRequestedBill().getPaymentMethod());
        getAprovedBill().setFromDepartment(getRequestedBill().getDepartment());
        getAprovedBill().setFromInstitution(getRequestedBill().getInstitution());
        getAprovedBill().setReferenceBill(getRequestedBill());
        getAprovedBill().setBackwardReferenceBill(getRequestedBill());

        getAprovedBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getAprovedBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getAprovedBill().setCreater(getSessionController().getLoggedUser());
        getAprovedBill().setCreatedAt(Calendar.getInstance().getTime());

        getAprovedBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER_APPROVAL);

        try {
            if (getAprovedBill().getId() == null) {
                getBillFacade().create(getAprovedBill());
            } else {
                getBillFacade().edit(getAprovedBill());
            }
        } catch (Exception e) {
        }

    }

    public void saveBillComponent() {
        for (BillItem i : getBillItems()) {
            i.setBill(getAprovedBill());
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            i.setNetValue(i.getPharmaceuticalBillItem().getQty() * i.getPharmaceuticalBillItem().getPurchaseRate());

            double qty;
            qty = i.getQty() + i.getPharmaceuticalBillItem().getFreeQty();
            if (qty <= 0.0) {
                i.setRetired(true);
                i.setRetirer(sessionController.getLoggedUser());
                i.setRetiredAt(new Date());
                i.setRetireComments("Retired at Approving PO");

            }
//            totalBillItemsCount = totalBillItemsCount + qty;
            PharmaceuticalBillItem phItem = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);
            try {
                if (i.getId() == null) {
                    getBillItemFacade().create(i);
                } else {
                    getBillItemFacade().edit(i);
                }
            } catch (Exception e) {
            }

            phItem.setBillItem(i);

            try {
                if (phItem.getId() == null) {
                    getPharmaceuticalBillItemFacade().create(phItem);
                } else {
                    getPharmaceuticalBillItemFacade().edit(phItem);
                }
            } catch (Exception e) {
            }

            i.setPharmaceuticalBillItem(phItem);
            try {
                getBillItemFacade().edit(i);
            } catch (Exception e) {

            }
            getAprovedBill().getBillItems().add(i);
        }

        getBillFacade().edit(getAprovedBill());
    }

    public void generateBillComponent() {

        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getRequestedBill())) {
            BillItem bi = new BillItem();
            bi.copy(i.getBillItem());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.setBillItem(bi);

            ph.setFreeQty(i.getFreeQty());
            ph.setQty(i.getQty());
            ph.setPurchaseRate(i.getPurchaseRate());
            ph.setRetailRate(i.getRetailRate());
            bi.setPharmaceuticalBillItem(ph);
//            bi.setTmpQty(ph.getQty());

            getBillItems().add(bi);
        }

        calTotal();

    }

    public void setRequestedBill(Bill requestedBill) {
        // The logic inside getter was taken to the navigator method
//        clearList();
        this.requestedBill = requestedBill;
//        getAprovedBill().setPaymentMethod(getRequestedBill().getPaymentMethod());
//        getAprovedBill().setToInstitution(getRequestedBill().getToInstitution());
//        getAprovedBill().setCreditDuration(getRequestedBill().getCreditDuration());
//        generateBillComponent();
    }

    public Bill getAprovedBill() {
        if (aprovedBill == null) {
            aprovedBill = new BilledBill();
            aprovedBill.setBillType(BillType.PharmacyOrderApprove);
            aprovedBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER_APPROVAL);
            aprovedBill.setConsignment(getRequestedBill() != null && getRequestedBill().isConsignment());
        }
        return aprovedBill;
    }

    public void setAprovedBill(Bill aprovedBill) {
        this.aprovedBill = aprovedBill;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
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

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public void calTotal() {
        double tmp = 0;
        int serialNo = 0;
        for (BillItem bi : getBillItems()) {
            tmp += bi.getPharmaceuticalBillItem().getQty() * bi.getPharmaceuticalBillItem().getPurchaseRate();
            bi.setSearialNo(serialNo++);
        }
        getAprovedBill().setTotal(tmp);
        getAprovedBill().setNetTotal(tmp);
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public List<PharmaceuticalBillItem> getFilteredValue() {
        return filteredValue;
    }

    public void setFilteredValue(List<PharmaceuticalBillItem> filteredValue) {
        this.filteredValue = filteredValue;
    }

    public List<Bill> getBillsToApprove() {
        return billsToApprove;
    }

    public void setBillsToApprove(List<Bill> billsToApprove) {
        this.billsToApprove = billsToApprove;
    }

    public PharmacyController getPharmacyController() {
        return pharmacyController;
    }

    public void setPharmacyController(PharmacyController pharmacyController) {
        this.pharmacyController = pharmacyController;
    }

    public boolean getPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public void makeListNull() {
//        pharmaceuticalBillItems = null;
        filteredValue = null;
        billsToApprove = null;
        searchBills = null;
        billItems = null;
        bills = null;
        maxResult = 50;

    }

    public LazyDataModel<Bill> getSearchBills() {
        return searchBills;
    }

    public void setSearchBills(LazyDataModel<Bill> searchBills) {
        this.searchBills = searchBills;
    }

    public String getTxtSearch() {
        return txtSearch;
    }

    public void setTxtSearch(String txtSearch) {
        this.txtSearch = txtSearch;
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

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
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

    public int getMaxResult() {
        return maxResult;
    }

    public void setMaxResult(int maxResult) {
        this.maxResult = maxResult;
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

    public double getTotalBillItemsCount() {
        return totalBillItemsCount;
    }

    public void setTotalBillItemsCount(double totalBillItemsCount) {
        this.totalBillItemsCount = totalBillItemsCount;
    }

    public void prepareEmailDialog() {
        if (aprovedBill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }
        
        // Set default email if available
        if (aprovedBill.getToInstitution() != null && aprovedBill.getToInstitution().getEmail() != null) {
            emailRecipient = aprovedBill.getToInstitution().getEmail();
        } else {
            emailRecipient = "";
        }
    }

    public void sendPurchaseOrderEmail() {
        if (aprovedBill == null) {
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
        email.setBill(aprovedBill);
        email.setMessageType(MessageType.Marketing);
        email.setSentSuccessfully(false);
        email.setPending(true);
        emailFacade.create(email);

        try {
            boolean success = emailManagerEjb.sendEmail(
                    java.util.Collections.singletonList(recipient),
                    body,
                    "Purchase Order",
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
            if (aprovedBill == null) {
                return null;
            }
            
            StringBuilder html = new StringBuilder();
            html.append("<html><head><title>Purchase Order</title></head><body>");
            html.append("<div style='font-family: Arial, sans-serif; padding: 20px;'>");
            
            // Institution header
            if (aprovedBill.getCreater() != null && aprovedBill.getCreater().getInstitution() != null) {
                html.append("<div style='text-align: center; margin-bottom: 20px;'>");
                html.append("<h2>").append(aprovedBill.getCreater().getInstitution().getName() != null ? aprovedBill.getCreater().getInstitution().getName() : "").append("</h2>");
                if (aprovedBill.getCreater().getInstitution().getAddress() != null) {
                    html.append("<p>").append(aprovedBill.getCreater().getInstitution().getAddress()).append("</p>");
                }
                if (aprovedBill.getCreater().getInstitution().getPhone() != null) {
                    html.append("<p>Phone: ").append(aprovedBill.getCreater().getInstitution().getPhone()).append("</p>");
                }
                html.append("</div>");
            }
            
            html.append("<h3 style='text-align: center; text-decoration: underline;'>Purchase Order</h3>");
            
            // Order details
            html.append("<table style='width: 100%; margin-bottom: 20px;'>");
            html.append("<tr><td><strong>Order No:</strong></td><td>").append(aprovedBill.getDeptId() != null ? aprovedBill.getDeptId() : "").append("</td></tr>");
            if (aprovedBill.getDepartment() != null) {
                html.append("<tr><td><strong>Order Department:</strong></td><td>").append(aprovedBill.getDepartment().getName() != null ? aprovedBill.getDepartment().getName() : "").append("</td></tr>");
            }
            if (aprovedBill.getToInstitution() != null) {
                html.append("<tr><td><strong>Supplier:</strong></td><td>").append(aprovedBill.getToInstitution().getName() != null ? aprovedBill.getToInstitution().getName() : "").append("</td></tr>");
                html.append("<tr><td><strong>Supplier Code:</strong></td><td>").append(aprovedBill.getToInstitution().getCode() != null ? aprovedBill.getToInstitution().getCode() : "").append("</td></tr>");
                if (aprovedBill.getToInstitution().getPhone() != null) {
                    html.append("<tr><td><strong>Supplier Phone:</strong></td><td>").append(aprovedBill.getToInstitution().getPhone()).append("</td></tr>");
                }
                if (aprovedBill.getToInstitution().getAddress() != null) {
                    html.append("<tr><td><strong>Supplier Address:</strong></td><td>").append(aprovedBill.getToInstitution().getAddress()).append("</td></tr>");
                }
            }
            html.append("<tr><td><strong>Payment Method:</strong></td><td>").append(aprovedBill.getPaymentMethod() != null ? aprovedBill.getPaymentMethod().toString() : "").append("</td></tr>");
            html.append("<tr><td><strong>Consignment:</strong></td><td>").append(aprovedBill.isConsignment() ? "Yes" : "No").append("</td></tr>");
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
            html.append("<td style='padding: 8px; text-align: right;'>").append(String.format("%,.2f", aprovedBill.getNetTotal())).append("</td>");
            html.append("</tr></tfoot></table>");
            
            // Footer details
            html.append("<div style='margin-top: 20px;'>");
            if (aprovedBill.getCreater() != null && aprovedBill.getCreater().getWebUserPerson() != null) {
                html.append("<p><strong>Order Initiated By:</strong> ").append(aprovedBill.getCreater().getWebUserPerson().getName() != null ? aprovedBill.getCreater().getWebUserPerson().getName() : "").append("</p>");
            }
            if (aprovedBill.getCheckedBy() != null) {
                html.append("<p><strong>Order Finalized By:</strong> ").append(aprovedBill.getCheckedBy().getName() != null ? aprovedBill.getCheckedBy().getName() : "").append("</p>");
            }
            if (aprovedBill.getCheckeAt() != null) {
                html.append("<p><strong>Order Finalized At:</strong> ").append(CommonFunctions.formatDate(aprovedBill.getCheckeAt(), "dd/MM/yyyy HH:mm:ss")).append("</p>");
            }
            html.append("<p><strong>Generated At:</strong> ").append(CommonFunctions.formatDate(new Date(), "dd/MM/yyyy HH:mm:ss")).append("</p>");
            html.append("<p><strong>Total:</strong> ").append(String.format("%,.2f", aprovedBill.getNetTotal())).append("</p>");
            html.append("</div>");
            
            html.append("</div></body></html>");
            return html.toString();
        } catch (Exception e) {
            return null;
        }
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
