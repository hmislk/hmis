/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.core.data.Icon;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.UserIcon;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.UserIconFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class UserIconController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private UserIconFacade ejbFacade;
    private UserIcon current;
    private List<UserIcon> userIcons = null;
    private List<Icon> icons;
    private WebUser user;
    private Icon icon;
    private Department department;
    private List<Department> departments;
    private TreeNode rootIconTreeNode;
    private TreeNode[] selectedIconNodes;
    private String iconSearchText;
    private boolean iconsLoaded;

// Modified by Dr M H B Ariyaratne with assistance from ChatGPT from OpenAI
    public void addUserIcon() {
        if (icon == null) {
            JsfUtil.addErrorMessage("Select Icon");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("Select Department");
            return;
        }
        if (user == null) {
            JsfUtil.addErrorMessage("Program Error. Cannot have this page without a user. Create an issue in GitHub");
            return;
        }

        // Check for an existing icon for the same user and department
        Map<String, Object> params = new HashMap<>();
        params.put("u", user);
        params.put("i", icon);
        params.put("d", department);
        params.put("ret", false);
        String jpql = "select ui from UserIcon ui where ui.webUser=:u and ui.icon=:i and ui.department=:d and ui.retired=:ret";
        UserIcon duplicate = getFacade().findFirstByJpql(jpql, params);
        if (duplicate != null) {
            JsfUtil.addErrorMessage("Icon already added for this department");
            return;
        }

        double newOrder = getUserIcons().size() + 1;
        UserIcon existingUI = findUserIconByOrder(newOrder);

        if (existingUI == null) {
            UserIcon ui = new UserIcon();
            ui.setWebUser(user);
            ui.setIcon(icon);
            ui.setOrderNumber(newOrder);
            ui.setDepartment(department);
            save(ui);
            JsfUtil.addSuccessMessage("Save Success ");
            fillDepartmentIcon();
            reOrderUserIcons();
            // Clear selected icon after successful addition
            icon = null;
        } else {
            JsfUtil.addErrorMessage("Icon already exists at this position");
        }

    }

    public void fillDepartmentIcon() {
        if (user == null) {
            JsfUtil.addErrorMessage("User?");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("Department?");
            return;
        }
        
        Map m = new HashMap();
        String jpql = "SELECT i "
                + " FROM UserIcon i "
                + " where i.webUser=:u "
                + " and i.retired=:ret ";
        if (department != null) {
            jpql += " and i.department=:dep";
            m.put("dep", department);
        }
        m.put("u", user);
        m.put("ret", false);
        userIcons = getEjbFacade().findByJpql(jpql, m);
    }

    // Modified by Dr M H B Ariyaratne with assistance from ChatGPT from OpenAI
    public void moveSelectedUserIconUp() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        double currentOrder = current.getOrderNumber();
        UserIcon prevIcon = findUserIconByOrder(currentOrder - 1);
        if (prevIcon != null) {
            prevIcon.setOrderNumber(currentOrder);
            current.setOrderNumber(currentOrder - 1);
            save(prevIcon);
            save(current);
        } else {
            JsfUtil.addErrorMessage("Already at the top");
        }
        fillUserIcons();
    }

    public void moveSelectedUserIconDown() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        double currentOrder = current.getOrderNumber();
        UserIcon nextIcon = findUserIconByOrder(currentOrder + 1);
        if (nextIcon != null) {
            nextIcon.setOrderNumber(currentOrder);
            current.setOrderNumber(currentOrder + 1);
            save(nextIcon);
            save(current);
        } else {
            JsfUtil.addErrorMessage("Already at the bottom");
        }
        fillUserIcons();
    }

    private UserIcon findUserIconByOrder(double order) {
        for (UserIcon ui : userIcons) {
            if (ui.getOrderNumber() == order) {
                return ui;
            }
        }
        return null;
    }

    // Method to re-order UserIcons based on orderNumber
    public void reOrderUserIcons() {
        if (userIcons == null || userIcons.isEmpty()) {
            return;
        }
        double order = 0.0;
        for (UserIcon ui : userIcons) {
            ui.setOrderNumber(order++);
            save(ui);
        }
    }

    // Method to validate if the Icon is already added for the user
    public boolean isIconAlreadyAdded() {
        for (UserIcon ui : userIcons) {
            if (ui.getIcon() == icon) {
                JsfUtil.addErrorMessage("Icon already added");
                return true;
            }
        }
        return false;
    }

    private void fillUserIcons() {
        userIcons = fillUserIcons(user, department);
    }

    public List<UserIcon> fillUserIcons(WebUser u, Department dept) {
        List<UserIcon> uis = null;
        if (u == null) {
            userIcons = null;
            return uis;
        }
        String Jpql = "select i "
                + " from UserIcon i "
                + " where i.retired=:ret "
                + " and i.webUser=:u "
                + " and i.department=:dept ";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("u", u);
        m.put("dept", dept);
        uis = getFacade().findByJpql(Jpql, m);
        Collections.sort(uis, new UserIconOrderComparator());
        return uis;
    }

    public void save(UserIcon ui) {
        if (ui == null) {
            return;
        }
        if (ui.getId() != null) {
            getFacade().edit(ui);
        } else {
            getFacade().create(ui);
        }
    }

    private UserIconFacade getEjbFacade() {
        return ejbFacade;
    }

    public UserIconController() {
    }

    @PostConstruct
    public void init() {
        rootIconTreeNode = createIconTreeNodes();
    }

    public UserIcon getCurrent() {
        if (current == null) {
            current = new UserIcon();
        }
        return current;
    }

    public void setCurrent(UserIcon current) {
        this.current = current;
        fillUserIcons();
    }

    public void removeUserIcon() {
        if (current != null) {
            current.setRetired(true);
            save(current);
            JsfUtil.addSuccessMessage("Removed Successfully");
            fillDepartmentIcon();
            reOrderUserIcons();
        } else {
            JsfUtil.addSuccessMessage("Nothing to Remove");
        }
    }

    private UserIconFacade getFacade() {
        return ejbFacade;
    }

    public List<Icon> getIcons() {
        if (icons == null) {
            icons = Arrays.stream(Icon.values())
                    .sorted(Comparator.comparing(Icon::getLabel))
                    .collect(Collectors.toList());
        }
        return icons;
    }

    public List<Icon> getFilteredIcons(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Arrays.asList(Icon.values()); // Return all if no input
        }

        String[] keywords = query.toLowerCase().split("\\s+"); // Split by spaces

        return Arrays.stream(Icon.values())
                .filter(icon -> {
                    String label = icon.getLabel().toLowerCase();
                    return Arrays.stream(keywords).allMatch(label::contains);
                })
                .collect(Collectors.toList());
    }

    public WebUser getUser() {
        return user;
    }

    public void setUser(WebUser user) {
        this.user = user;
        fillDepartmentIcon();
    }

    public List<UserIcon> getUserIcons() {
        if (userIcons == null) {
            userIcons = new ArrayList<>();
        }
        return userIcons;
    }

    public void setUserIcons(List<UserIcon> userIcons) {
        this.userIcons = userIcons;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
        fillDepartmentIcon();
    }

    public void setDepartmentToLoggedUser() {
        if (sessionController != null && sessionController.getDepartment() != null) {
            setDepartment(sessionController.getDepartment());
            JsfUtil.addSuccessMessage("Department set to " + sessionController.getDepartment().getName());
        } else {
            JsfUtil.addErrorMessage("No department found for logged user");
        }
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    // Tree-based bulk icon selection methods

    private TreeNode createIconTreeNodes() {
        TreeNode root = new DefaultTreeNode(new IconHolder(null, "Root"), null);

        TreeNode allNode = new DefaultTreeNode(new IconHolder(null, "All Icons"), root);

        // Patient Management
        TreeNode patientNode = new DefaultTreeNode(new IconHolder(null, "Patient Management"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.Patient_Lookup, Icon.Patient_Lookup.getLabel()), patientNode);
        new DefaultTreeNode(new IconHolder(Icon.Patient_Add_New, Icon.Patient_Add_New.getLabel()), patientNode);

        // OPD Billing
        TreeNode opdBillingNode = new DefaultTreeNode(new IconHolder(null, "OPD Billing"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.Opd_Billing, Icon.Opd_Billing.getLabel()), opdBillingNode);
        new DefaultTreeNode(new IconHolder(Icon.Billing_For_Cashier, Icon.Billing_For_Cashier.getLabel()), opdBillingNode);
        new DefaultTreeNode(new IconHolder(Icon.Collecting_Centre_Billing, Icon.Collecting_Centre_Billing.getLabel()), opdBillingNode);
        new DefaultTreeNode(new IconHolder(Icon.Medical_Package_Billing, Icon.Medical_Package_Billing.getLabel()), opdBillingNode);
        new DefaultTreeNode(new IconHolder(Icon.Search_Opd_Bill, Icon.Search_Opd_Bill.getLabel()), opdBillingNode);
        new DefaultTreeNode(new IconHolder(Icon.Search_Opd_Bill_Item, Icon.Search_Opd_Bill_Item.getLabel()), opdBillingNode);
        new DefaultTreeNode(new IconHolder(Icon.Search_Opd_Payment, Icon.Search_Opd_Payment.getLabel()), opdBillingNode);
        new DefaultTreeNode(new IconHolder(Icon.Search_Collecting_Centre_Bill, Icon.Search_Collecting_Centre_Bill.getLabel()), opdBillingNode);
        new DefaultTreeNode(new IconHolder(Icon.Search_Bill_to_Pay, Icon.Search_Bill_to_Pay.getLabel()), opdBillingNode);
        new DefaultTreeNode(new IconHolder(Icon.Search_Credit_Paid_Bill, Icon.Search_Credit_Paid_Bill.getLabel()), opdBillingNode);
        new DefaultTreeNode(new IconHolder(Icon.Search_Credit_Paid_Bill_with_OPD_Bill, Icon.Search_Credit_Paid_Bill_with_OPD_Bill.getLabel()), opdBillingNode);

        // Payments and Refunds
        TreeNode paymentsNode = new DefaultTreeNode(new IconHolder(null, "Payments and Refunds"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.Scan_to_Pay, Icon.Scan_to_Pay.getLabel()), paymentsNode);
        new DefaultTreeNode(new IconHolder(Icon.Accept_Payments, Icon.Accept_Payments.getLabel()), paymentsNode);
        new DefaultTreeNode(new IconHolder(Icon.Accept_Payments_For_OPD_Bills, Icon.Accept_Payments_For_OPD_Bills.getLabel()), paymentsNode);
        new DefaultTreeNode(new IconHolder(Icon.Accept_Payments_For_OPD_Batch_Bills, Icon.Accept_Payments_For_OPD_Batch_Bills.getLabel()), paymentsNode);
        new DefaultTreeNode(new IconHolder(Icon.Accept_Payments_For_Pharmacy_Bills, Icon.Accept_Payments_For_Pharmacy_Bills.getLabel()), paymentsNode);
        new DefaultTreeNode(new IconHolder(Icon.Refunds, Icon.Refunds.getLabel()), paymentsNode);
        new DefaultTreeNode(new IconHolder(Icon.RefundsforOPDBills, Icon.RefundsforOPDBills.getLabel()), paymentsNode);
        new DefaultTreeNode(new IconHolder(Icon.RefundsforPharmacyBills, Icon.RefundsforPharmacyBills.getLabel()), paymentsNode);
        new DefaultTreeNode(new IconHolder(Icon.Patient_Deposit_Management, Icon.Patient_Deposit_Management.getLabel()), paymentsNode);

        // Doctor and Channel
        TreeNode doctorChannelNode = new DefaultTreeNode(new IconHolder(null, "Doctor and Channel"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.Doctor_Mark_in, Icon.Doctor_Mark_in.getLabel()), doctorChannelNode);
        new DefaultTreeNode(new IconHolder(Icon.Doctor_Mark_out, Icon.Doctor_Mark_out.getLabel()), doctorChannelNode);
        new DefaultTreeNode(new IconHolder(Icon.Doctor_Working_Times, Icon.Doctor_Working_Times.getLabel()), doctorChannelNode);
        new DefaultTreeNode(new IconHolder(Icon.Channel_Booking, Icon.Channel_Booking.getLabel()), doctorChannelNode);
        new DefaultTreeNode(new IconHolder(Icon.Channel_Booking_by_Dates, Icon.Channel_Booking_by_Dates.getLabel()), doctorChannelNode);
        new DefaultTreeNode(new IconHolder(Icon.Channel_Scheduling, Icon.Channel_Scheduling.getLabel()), doctorChannelNode);
        new DefaultTreeNode(new IconHolder(Icon.Manage_Token, Icon.Manage_Token.getLabel()), doctorChannelNode);
        new DefaultTreeNode(new IconHolder(Icon.Manage_Appointment, Icon.Manage_Appointment.getLabel()), doctorChannelNode);

        // Lab and Reports
        TreeNode labReportsNode = new DefaultTreeNode(new IconHolder(null, "Lab and Reports"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.Lab_Report_Print, Icon.Lab_Report_Print.getLabel()), labReportsNode);
        new DefaultTreeNode(new IconHolder(Icon.Investigation_Trace, Icon.Investigation_Trace.getLabel()), labReportsNode);
        new DefaultTreeNode(new IconHolder(Icon.Report_Execution_Logs, Icon.Report_Execution_Logs.getLabel()), labReportsNode);

        // OPD Summaries
        TreeNode opdSummariesNode = new DefaultTreeNode(new IconHolder(null, "OPD Summaries"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.OPD_Summaries, Icon.OPD_Summaries.getLabel()), opdSummariesNode);
        new DefaultTreeNode(new IconHolder(Icon.OPD_Analytics, Icon.OPD_Analytics.getLabel()), opdSummariesNode);

        // Pharmacy Sales
        TreeNode pharmSalesNode = new DefaultTreeNode(new IconHolder(null, "Pharmacy Sales"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_sale, Icon.pharmacy_sale.getLabel()), pharmSalesNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_sale_for_cashier, Icon.pharmacy_sale_for_cashier.getLabel()), pharmSalesNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_sale_without_stock, Icon.pharmacy_sale_without_stock.getLabel()), pharmSalesNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_Search_sale_bill, Icon.pharmacy_Search_sale_bill.getLabel()), pharmSalesNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_search_sale_pre_bill, Icon.pharmacy_search_sale_pre_bill.getLabel()), pharmSalesNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_search_sale_bill_item, Icon.pharmacy_search_sale_bill_item.getLabel()), pharmSalesNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_bill_search, Icon.pharmacy_bill_search.getLabel()), pharmSalesNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_bill_search_new, Icon.pharmacy_bill_search_new.getLabel()), pharmSalesNode);

        // Pharmacy Returns
        TreeNode pharmReturnsNode = new DefaultTreeNode(new IconHolder(null, "Pharmacy Returns"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_return_items_only, Icon.pharmacy_return_items_only.getLabel()), pharmReturnsNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_return_items_and_payments, Icon.pharmacy_return_items_and_payments.getLabel()), pharmReturnsNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_search_return_bill, Icon.pharmacy_search_return_bill.getLabel()), pharmReturnsNode);

        // Pharmacy Stock
        TreeNode pharmStockNode = new DefaultTreeNode(new IconHolder(null, "Pharmacy Stock"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_add_to_stock, Icon.pharmacy_add_to_stock.getLabel()), pharmStockNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_issue, Icon.pharmacy_issue.getLabel()), pharmStockNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_search_issue_bill, Icon.pharmacy_search_issue_bill.getLabel()), pharmStockNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_search_issue_bill_items, Icon.pharmacy_search_issue_bill_items.getLabel()), pharmStockNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_search_issue_return_bill, Icon.pharmacy_search_issue_return_bill.getLabel()), pharmStockNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_uint_issue_margin, Icon.pharmacy_uint_issue_margin.getLabel()), pharmStockNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_direct_issue, Icon.pharmacy_direct_issue.getLabel()), pharmStockNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_receive, Icon.pharmacy_receive.getLabel()), pharmStockNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_request, Icon.pharmacy_request.getLabel()), pharmStockNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_issue_for_request, Icon.pharmacy_issue_for_request.getLabel()), pharmStockNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_disposal_issue, Icon.pharmacy_disposal_issue.getLabel()), pharmStockNode);

        // Pharmacy Reports
        TreeNode pharmReportsNode = new DefaultTreeNode(new IconHolder(null, "Pharmacy Reports"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_disbursement_reports, Icon.pharmacy_disbursement_reports.getLabel()), pharmReportsNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_item_search, Icon.pharmacy_item_search.getLabel()), pharmReportsNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_generate_report, Icon.pharmacy_generate_report.getLabel()), pharmReportsNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_summary_views, Icon.pharmacy_summary_views.getLabel()), pharmReportsNode);
        new DefaultTreeNode(new IconHolder(Icon.pharmacy_analytics, Icon.pharmacy_analytics.getLabel()), pharmReportsNode);

        // Wholesale
        TreeNode wholesaleNode = new DefaultTreeNode(new IconHolder(null, "Wholesale"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.wholesale_sale, Icon.wholesale_sale.getLabel()), wholesaleNode);
        new DefaultTreeNode(new IconHolder(Icon.wholesale_sale_for_cashier, Icon.wholesale_sale_for_cashier.getLabel()), wholesaleNode);
        new DefaultTreeNode(new IconHolder(Icon.wholesale_Search_sale_bill, Icon.wholesale_Search_sale_bill.getLabel()), wholesaleNode);
        new DefaultTreeNode(new IconHolder(Icon.wholesale_Search_sale_bill_to_pay, Icon.wholesale_Search_sale_bill_to_pay.getLabel()), wholesaleNode);
        new DefaultTreeNode(new IconHolder(Icon.wholesale_search_sale_bill_item, Icon.wholesale_search_sale_bill_item.getLabel()), wholesaleNode);
        new DefaultTreeNode(new IconHolder(Icon.wholesale_return_items_only, Icon.wholesale_return_items_only.getLabel()), wholesaleNode);
        new DefaultTreeNode(new IconHolder(Icon.wholesale_return_items_and_payments, Icon.wholesale_return_items_and_payments.getLabel()), wholesaleNode);
        new DefaultTreeNode(new IconHolder(Icon.wholesale_search_return_bill_item, Icon.wholesale_search_return_bill_item.getLabel()), wholesaleNode);
        new DefaultTreeNode(new IconHolder(Icon.wholesale_add_to_stock, Icon.wholesale_add_to_stock.getLabel()), wholesaleNode);
        new DefaultTreeNode(new IconHolder(Icon.wholesale_purchase, Icon.wholesale_purchase.getLabel()), wholesaleNode);

        // Inpatient Direct Issues
        TreeNode inpatientDirectIssueNode = new DefaultTreeNode(new IconHolder(null, "Inpatient Direct Issues"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.direct_issue_to_BHTs, Icon.direct_issue_to_BHTs.getLabel()), inpatientDirectIssueNode);
        new DefaultTreeNode(new IconHolder(Icon.direct_issue_to_theatre, Icon.direct_issue_to_theatre.getLabel()), inpatientDirectIssueNode);
        new DefaultTreeNode(new IconHolder(Icon.BHT_issue_requests, Icon.BHT_issue_requests.getLabel()), inpatientDirectIssueNode);
        new DefaultTreeNode(new IconHolder(Icon.search_inpatint_direct_issue_by_bill, Icon.search_inpatint_direct_issue_by_bill.getLabel()), inpatientDirectIssueNode);
        new DefaultTreeNode(new IconHolder(Icon.search_inpatint_direct_issue_by_item, Icon.search_inpatint_direct_issue_by_item.getLabel()), inpatientDirectIssueNode);
        new DefaultTreeNode(new IconHolder(Icon.search_inpatint_direct_issue_returns_by_bill, Icon.search_inpatint_direct_issue_returns_by_bill.getLabel()), inpatientDirectIssueNode);
        new DefaultTreeNode(new IconHolder(Icon.search_inpatint_direct_issue_returns_by_item, Icon.search_inpatint_direct_issue_returns_by_item.getLabel()), inpatientDirectIssueNode);

        // Purchasing
        TreeNode purchasingNode = new DefaultTreeNode(new IconHolder(null, "Purchasing"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.Create_Purchase_Order, Icon.Create_Purchase_Order.getLabel()), purchasingNode);
        new DefaultTreeNode(new IconHolder(Icon.Auto_Order_P_Model, Icon.Auto_Order_P_Model.getLabel()), purchasingNode);
        new DefaultTreeNode(new IconHolder(Icon.Auto_Order_Q_Model, Icon.Auto_Order_Q_Model.getLabel()), purchasingNode);
        new DefaultTreeNode(new IconHolder(Icon.Direct_Purchase, Icon.Direct_Purchase.getLabel()), purchasingNode);
        new DefaultTreeNode(new IconHolder(Icon.Purchase_Orders_Approval, Icon.Purchase_Orders_Approval.getLabel()), purchasingNode);
        new DefaultTreeNode(new IconHolder(Icon.Purchase_Orders_Finalize, Icon.Purchase_Orders_Finalize.getLabel()), purchasingNode);
        new DefaultTreeNode(new IconHolder(Icon.Multiple_Purchase_Order_Cancellation, Icon.Multiple_Purchase_Order_Cancellation.getLabel()), purchasingNode);
        new DefaultTreeNode(new IconHolder(Icon.Pharmacy_Order_Cancellation, Icon.Pharmacy_Order_Cancellation.getLabel()), purchasingNode);

        // Goods Receipt
        TreeNode goodsReceiptNode = new DefaultTreeNode(new IconHolder(null, "Goods Receipt"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.Goods_Receipt, Icon.Goods_Receipt.getLabel()), goodsReceiptNode);
        new DefaultTreeNode(new IconHolder(Icon.Goods_Receipt_With_Approval, Icon.Goods_Receipt_With_Approval.getLabel()), goodsReceiptNode);
        new DefaultTreeNode(new IconHolder(Icon.Goods_Receipt_Costing, Icon.Goods_Receipt_Costing.getLabel()), goodsReceiptNode);
        new DefaultTreeNode(new IconHolder(Icon.Return_Received_Goods, Icon.Return_Received_Goods.getLabel()), goodsReceiptNode);
        new DefaultTreeNode(new IconHolder(Icon.Return_without_Receipt, Icon.Return_without_Receipt.getLabel()), goodsReceiptNode);

        // Stock Adjustments
        TreeNode stockAdjNode = new DefaultTreeNode(new IconHolder(null, "Stock Adjustments"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.adjustments_Depaetment_stock, Icon.adjustments_Depaetment_stock.getLabel()), stockAdjNode);
        new DefaultTreeNode(new IconHolder(Icon.adjustments_staff_stock, Icon.adjustments_staff_stock.getLabel()), stockAdjNode);
        new DefaultTreeNode(new IconHolder(Icon.adjustments_purchase_rate, Icon.adjustments_purchase_rate.getLabel()), stockAdjNode);
        new DefaultTreeNode(new IconHolder(Icon.adjustments_sale_rate, Icon.adjustments_sale_rate.getLabel()), stockAdjNode);
        new DefaultTreeNode(new IconHolder(Icon.adjustments_wholesale_rate, Icon.adjustments_wholesale_rate.getLabel()), stockAdjNode);
        new DefaultTreeNode(new IconHolder(Icon.adjustments_expiry_date, Icon.adjustments_expiry_date.getLabel()), stockAdjNode);
        new DefaultTreeNode(new IconHolder(Icon.adjustments_search_adjustment_bill, Icon.adjustments_search_adjustment_bill.getLabel()), stockAdjNode);
        new DefaultTreeNode(new IconHolder(Icon.adjustments_transfer_all_stock, Icon.adjustments_transfer_all_stock.getLabel()), stockAdjNode);

        // Supplier Payments
        TreeNode supplierPayNode = new DefaultTreeNode(new IconHolder(null, "Supplier Payments"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.supplier_due_search, Icon.supplier_due_search.getLabel()), supplierPayNode);
        new DefaultTreeNode(new IconHolder(Icon.supplier_payment_management, Icon.supplier_payment_management.getLabel()), supplierPayNode);
        new DefaultTreeNode(new IconHolder(Icon.dealer_due_by_age, Icon.dealer_due_by_age.getLabel()), supplierPayNode);
        new DefaultTreeNode(new IconHolder(Icon.payment_by_supplier, Icon.payment_by_supplier.getLabel()), supplierPayNode);
        new DefaultTreeNode(new IconHolder(Icon.payment_by_bill, Icon.payment_by_bill.getLabel()), supplierPayNode);
        new DefaultTreeNode(new IconHolder(Icon.GRN_payment_approve, Icon.GRN_payment_approve.getLabel()), supplierPayNode);
        new DefaultTreeNode(new IconHolder(Icon.GRN_payment_done_search, Icon.GRN_payment_done_search.getLabel()), supplierPayNode);
        new DefaultTreeNode(new IconHolder(Icon.credit_dues_and_access, Icon.credit_dues_and_access.getLabel()), supplierPayNode);

        // Cashier
        TreeNode cashierNode = new DefaultTreeNode(new IconHolder(null, "Cashier"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.Cashier_Summaries, Icon.Cashier_Summaries.getLabel()), cashierNode);
        new DefaultTreeNode(new IconHolder(Icon.Shift_End_Summary, Icon.Shift_End_Summary.getLabel()), cashierNode);
        new DefaultTreeNode(new IconHolder(Icon.Day_End_Summary, Icon.Day_End_Summary.getLabel()), cashierNode);
        new DefaultTreeNode(new IconHolder(Icon.Cashier_Drawer, Icon.Cashier_Drawer.getLabel()), cashierNode);
        new DefaultTreeNode(new IconHolder(Icon.Financial_Transaction_Manager, Icon.Financial_Transaction_Manager.getLabel()), cashierNode);
        new DefaultTreeNode(new IconHolder(Icon.Manage_Shift_Fund_Bills, Icon.Manage_Shift_Fund_Bills.getLabel()), cashierNode);

        // Inpatient Admissions
        TreeNode inpAdmissionsNode = new DefaultTreeNode(new IconHolder(null, "Inpatient Admissions"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.Admit, Icon.Admit.getLabel()), inpAdmissionsNode);
        new DefaultTreeNode(new IconHolder(Icon.Edit_Admission_Details, Icon.Edit_Admission_Details.getLabel()), inpAdmissionsNode);
        new DefaultTreeNode(new IconHolder(Icon.Change_Patient_for_Admission, Icon.Change_Patient_for_Admission.getLabel()), inpAdmissionsNode);
        new DefaultTreeNode(new IconHolder(Icon.Appointment_Admission, Icon.Appointment_Admission.getLabel()), inpAdmissionsNode);
        new DefaultTreeNode(new IconHolder(Icon.Search_Admissions, Icon.Search_Admissions.getLabel()), inpAdmissionsNode);
        new DefaultTreeNode(new IconHolder(Icon.Inpatient_Appointments, Icon.Inpatient_Appointments.getLabel()), inpAdmissionsNode);

        // Inpatient Rooms
        TreeNode inpRoomsNode = new DefaultTreeNode(new IconHolder(null, "Inpatient Rooms"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.Room_Reservations, Icon.Room_Reservations.getLabel()), inpRoomsNode);
        new DefaultTreeNode(new IconHolder(Icon.Room_Occupancy, Icon.Room_Occupancy.getLabel()), inpRoomsNode);
        new DefaultTreeNode(new IconHolder(Icon.Room_Vacancy, Icon.Room_Vacancy.getLabel()), inpRoomsNode);
        new DefaultTreeNode(new IconHolder(Icon.Admit_Room, Icon.Admit_Room.getLabel()), inpRoomsNode);
        new DefaultTreeNode(new IconHolder(Icon.Room_Change, Icon.Room_Change.getLabel()), inpRoomsNode);
        new DefaultTreeNode(new IconHolder(Icon.Guardian_Room_Change, Icon.Guardian_Room_Change.getLabel()), inpRoomsNode);

        // Inpatient Services
        TreeNode inpServicesNode = new DefaultTreeNode(new IconHolder(null, "Inpatient Services"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.Add_Services_Investigations, Icon.Add_Services_Investigations.getLabel()), inpServicesNode);
        new DefaultTreeNode(new IconHolder(Icon.Add_Services_With_Payments, Icon.Add_Services_With_Payments.getLabel()), inpServicesNode);
        new DefaultTreeNode(new IconHolder(Icon.Add_Outside_Charges, Icon.Add_Outside_Charges.getLabel()), inpServicesNode);
        new DefaultTreeNode(new IconHolder(Icon.Add_Professional_Fee, Icon.Add_Professional_Fee.getLabel()), inpServicesNode);
        new DefaultTreeNode(new IconHolder(Icon.Add_Estimated_Professional_Fee, Icon.Add_Estimated_Professional_Fee.getLabel()), inpServicesNode);
        new DefaultTreeNode(new IconHolder(Icon.Add_Timed_Services, Icon.Add_Timed_Services.getLabel()), inpServicesNode);
        new DefaultTreeNode(new IconHolder(Icon.Request_Medicines_From_Pharmacy, Icon.Request_Medicines_From_Pharmacy.getLabel()), inpServicesNode);
        new DefaultTreeNode(new IconHolder(Icon.View_Pharmacy_Requests, Icon.View_Pharmacy_Requests.getLabel()), inpServicesNode);

        // Inpatient Billing
        TreeNode inpBillingNode = new DefaultTreeNode(new IconHolder(null, "Inpatient Billing"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.Interim_Bill, Icon.Interim_Bill.getLabel()), inpBillingNode);
        new DefaultTreeNode(new IconHolder(Icon.Interim_Bill_Estimated, Icon.Interim_Bill_Estimated.getLabel()), inpBillingNode);
        new DefaultTreeNode(new IconHolder(Icon.Interim_Bill_Search, Icon.Interim_Bill_Search.getLabel()), inpBillingNode);
        new DefaultTreeNode(new IconHolder(Icon.Search_Service_Bill, Icon.Search_Service_Bill.getLabel()), inpBillingNode);
        new DefaultTreeNode(new IconHolder(Icon.Search_Professional_Bill, Icon.Search_Professional_Bill.getLabel()), inpBillingNode);
        new DefaultTreeNode(new IconHolder(Icon.Search_Estimated_Professional_Bill, Icon.Search_Estimated_Professional_Bill.getLabel()), inpBillingNode);
        new DefaultTreeNode(new IconHolder(Icon.Search_Provisional_Bill, Icon.Search_Provisional_Bill.getLabel()), inpBillingNode);
        new DefaultTreeNode(new IconHolder(Icon.Search_Final_Bill, Icon.Search_Final_Bill.getLabel()), inpBillingNode);
        new DefaultTreeNode(new IconHolder(Icon.Search_Final_Bill_By_Discharge_Date, Icon.Search_Final_Bill_By_Discharge_Date.getLabel()), inpBillingNode);

        // Inpatient Analytics
        TreeNode inpAnalyticsNode = new DefaultTreeNode(new IconHolder(null, "Inpatient Analytics"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.Inward_Analytics, Icon.Inward_Analytics.getLabel()), inpAnalyticsNode);

        // Optician
        TreeNode opticianNode = new DefaultTreeNode(new IconHolder(null, "Optician"), allNode);
        new DefaultTreeNode(new IconHolder(Icon.Optician_EMR, Icon.Optician_EMR.getLabel()), opticianNode);
        new DefaultTreeNode(new IconHolder(Icon.Optician_Patient_Management, Icon.Optician_Patient_Management.getLabel()), opticianNode);
        new DefaultTreeNode(new IconHolder(Icon.Optician_Appointment_Management, Icon.Optician_Appointment_Management.getLabel()), opticianNode);
        new DefaultTreeNode(new IconHolder(Icon.Optician_Stock_Management, Icon.Optician_Stock_Management.getLabel()), opticianNode);
        new DefaultTreeNode(new IconHolder(Icon.Optician_Product_Catalog, Icon.Optician_Product_Catalog.getLabel()), opticianNode);
        new DefaultTreeNode(new IconHolder(Icon.Optician_Repair_Management, Icon.Optician_Repair_Management.getLabel()), opticianNode);
        new DefaultTreeNode(new IconHolder(Icon.Optician_Retail_Sale, Icon.Optician_Retail_Sale.getLabel()), opticianNode);

        return root;
    }

    public void fillIconTreeSelections() {
        if (user == null) {
            JsfUtil.addErrorMessage("User?");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("Department?");
            return;
        }
        List<UserIcon> existingIcons = fillUserIcons(user, department);
        List<IconHolder> holders = new ArrayList<>();
        for (UserIcon ui : existingIcons) {
            if (ui.getIcon() != null) {
                holders.add(new IconHolder(ui.getIcon(), ui.getIcon().getLabel()));
            }
        }
        unselectIconTreeNodes(rootIconTreeNode);
        checkIconNodes(rootIconTreeNode, holders);
        iconsLoaded = true;
    }

    public void saveIconsFromTree() {
        if (user == null) {
            JsfUtil.addErrorMessage("User?");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("Department?");
            return;
        }

        // Extract selected icons from tree
        Set<Icon> selectedIcons = new HashSet<>();
        if (selectedIconNodes != null) {
            for (TreeNode node : selectedIconNodes) {
                Object data = node.getData();
                if (data instanceof IconHolder) {
                    IconHolder ih = (IconHolder) data;
                    if (ih.getIcon() != null) {
                        selectedIcons.add(ih.getIcon());
                    }
                }
            }
        }

        // Load existing UserIcon records
        List<UserIcon> existingIcons = fillUserIcons(user, department);
        Map<Icon, UserIcon> existingMap = new HashMap<>();
        for (UserIcon ui : existingIcons) {
            if (ui.getIcon() != null) {
                existingMap.put(ui.getIcon(), ui);
            }
        }

        // Retire unselected existing icons
        List<UserIcon> toEdit = new ArrayList<>();
        for (UserIcon ui : existingIcons) {
            if (ui.getIcon() != null && !selectedIcons.contains(ui.getIcon())) {
                ui.setRetired(true);
                toEdit.add(ui);
            }
        }

        // Determine max order number for appending new icons
        double maxOrder = 0;
        for (UserIcon ui : existingIcons) {
            if (!ui.isRetired() && selectedIcons.contains(ui.getIcon())) {
                if (ui.getOrderNumber() > maxOrder) {
                    maxOrder = ui.getOrderNumber();
                }
            }
        }

        // Create new UserIcon records for newly selected icons
        List<UserIcon> toCreate = new ArrayList<>();
        for (Icon selectedIcon : selectedIcons) {
            if (!existingMap.containsKey(selectedIcon)) {
                maxOrder++;
                UserIcon newUi = new UserIcon();
                newUi.setWebUser(user);
                newUi.setDepartment(department);
                newUi.setIcon(selectedIcon);
                newUi.setOrderNumber(maxOrder);
                toCreate.add(newUi);
            }
        }

        getFacade().batchEdit(toEdit);
        getFacade().batchCreate(toCreate);

        fillIconTreeSelections();
        JsfUtil.addSuccessMessage("Icons Updated");
    }

    public void filterIcons() {
        collapseAllIconNodes(rootIconTreeNode);
        rootIconTreeNode.setExpanded(true);
        if (iconSearchText == null || iconSearchText.trim().isEmpty()) {
            return;
        }
        String st = iconSearchText.trim().toLowerCase();
        expandIconMatches(rootIconTreeNode, st);
    }

    private void collapseAllIconNodes(TreeNode node) {
        if (node == null) {
            return;
        }
        node.setExpanded(false);
        for (Object childObj : node.getChildren()) {
            if (childObj instanceof TreeNode) {
                collapseAllIconNodes((TreeNode) childObj);
            }
        }
    }

    private boolean expandIconMatches(TreeNode node, String search) {
        boolean match = false;
        if (node.getData() instanceof IconHolder) {
            IconHolder ih = (IconHolder) node.getData();
            if (ih.getName() != null && ih.getName().toLowerCase().contains(search)) {
                match = true;
            }
        }
        for (Object childObj : node.getChildren()) {
            if (childObj instanceof TreeNode) {
                if (expandIconMatches((TreeNode) childObj, search)) {
                    match = true;
                }
            }
        }
        if (match) {
            node.setExpanded(true);
        }
        return match;
    }

    private static void checkIconNodes(TreeNode root, List<IconHolder> holdersToCheck) {
        if (root == null || holdersToCheck == null || holdersToCheck.isEmpty()) {
            return;
        }
        for (Object childObject : root.getChildren()) {
            if (childObject instanceof TreeNode) {
                TreeNode childNode = (TreeNode) childObject;
                checkIconNode(childNode, holdersToCheck);
            }
        }
    }

    private static void checkIconNode(TreeNode node, List<IconHolder> holdersToCheck) {
        if (node.getData() instanceof IconHolder) {
            IconHolder holder = (IconHolder) node.getData();
            if (holdersToCheck.contains(holder)) {
                ((DefaultTreeNode) node).setSelected(true);
            }
        }
        for (Object childObject : node.getChildren()) {
            if (childObject instanceof TreeNode) {
                TreeNode childNode = (TreeNode) childObject;
                checkIconNode(childNode, holdersToCheck);
            }
        }
    }

    private static void unselectIconTreeNodes(TreeNode root) {
        if (root == null) {
            return;
        }
        ((DefaultTreeNode) root).setSelected(false);
        for (Object childObject : root.getChildren()) {
            if (childObject instanceof TreeNode) {
                TreeNode childNode = (TreeNode) childObject;
                unselectIconTreeNodes(childNode);
            }
        }
    }

    public TreeNode getRootIconTreeNode() {
        return rootIconTreeNode;
    }

    public void setRootIconTreeNode(TreeNode rootIconTreeNode) {
        this.rootIconTreeNode = rootIconTreeNode;
    }

    public TreeNode[] getSelectedIconNodes() {
        return selectedIconNodes;
    }

    public void setSelectedIconNodes(TreeNode[] selectedIconNodes) {
        this.selectedIconNodes = selectedIconNodes;
    }

    public String getIconSearchText() {
        return iconSearchText;
    }

    public void setIconSearchText(String iconSearchText) {
        this.iconSearchText = iconSearchText;
    }

    public boolean isIconsLoaded() {
        return iconsLoaded;
    }

    public void setIconsLoaded(boolean iconsLoaded) {
        this.iconsLoaded = iconsLoaded;
    }

    // Inner class for tree node data
    public static class IconHolder implements Serializable {

        private static final long serialVersionUID = 1L;

        private Icon icon;
        private String name;

        public IconHolder(Icon icon, String name) {
            this.icon = icon;
            this.name = name;
        }

        public Icon getIcon() {
            return icon;
        }

        public void setIcon(Icon icon) {
            this.icon = icon;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 73 * hash + Objects.hashCode(this.icon);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final IconHolder other = (IconHolder) obj;
            return Objects.equals(this.icon, other.icon);
        }
    }

    /**
     *
     */
    @FacesConverter(forClass = UserIcon.class)
    public static class UserIconConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            UserIconController controller = (UserIconController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "userIconConverter");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof UserIcon) {
                UserIcon o = (UserIcon) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + UserIconController.class.getName());
            }
        }
    }

    public static class UserIconOrderComparator implements Comparator<UserIcon> {

        @Override
        public int compare(UserIcon o1, UserIcon o2) {
            return Double.compare(o1.getOrderNumber(), o2.getOrderNumber());
        }
    }

}
