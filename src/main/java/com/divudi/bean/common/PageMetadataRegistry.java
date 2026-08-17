/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.core.data.OptionScope;
import com.divudi.core.data.admin.ConfigOptionInfo;
import com.divudi.core.data.admin.PageMetadata;
import com.divudi.core.data.admin.PrivilegeInfo;
import javax.inject.Named;
import javax.enterprise.context.ApplicationScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;

/**
 * Application-scoped registry for page metadata.
 * Stores configuration options and privileges information for pages.
 * Controllers register their page metadata during initialization.
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@ApplicationScoped
public class PageMetadataRegistry implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, PageMetadata> registry;

    public PageMetadataRegistry() {
    }

    @PostConstruct
    public void init() {
        registry = new ConcurrentHashMap<>();
        registerPagesWithoutDedicatedControllers();
        // Pages will self-register by calling registerPage() method
        // This will be done from individual controllers' @PostConstruct methods
    }

    /**
     * Registers metadata for pages whose Config button is backed by a shared,
     * heavily-reused @SessionScoped controller (e.g. FinancialTransactionController,
     * BillSearch) that must not carry page-specific registration logic.
     * See issue #22995.
     */
    private void registerPagesWithoutDedicatedControllers() {
        PageMetadata shiftShortage = new PageMetadata(
                "cashier/record_shift_shortage",
                "Record Shift Shortage",
                "Record a cash shortage found during a cashier shift and settle it",
                "FinancialTransactionController"
        );
        shiftShortage.addPrivilege(new PrivilegeInfo(
                "Admin",
                "Administrative access to configuration interface",
                "Controls visibility of the Config button"
        ));
        registerPage(shiftShortage);

        PageMetadata opdDoctorPaymentBillReprint = new PageMetadata(
                "opd/professional_payments/payment_bill_reprint",
                "OPD Doctor Payment Bill Reprint",
                "Reprint an OPD doctor professional payment bill in various paper formats",
                "BillSearch"
        );
        opdDoctorPaymentBillReprint.addConfigOption(new ConfigOptionInfo(
                "OPD Doctor payment bill is A4 paper",
                "Prints the OPD doctor payment bill on A4 paper",
                OptionScope.APPLICATION
        ));
        opdDoctorPaymentBillReprint.addConfigOption(new ConfigOptionInfo(
                "OPD Doctor payment bill is five five paper.",
                "Prints the OPD doctor payment bill on five-five paper",
                OptionScope.APPLICATION
        ));
        opdDoctorPaymentBillReprint.addConfigOption(new ConfigOptionInfo(
                "OPD Doctor payment bill is POS paper",
                "Prints the OPD doctor payment bill on POS paper",
                OptionScope.APPLICATION
        ));
        opdDoctorPaymentBillReprint.addPrivilege(new PrivilegeInfo(
                "Admin",
                "Administrative access to configuration interface",
                "Controls visibility of the Config button"
        ));
        opdDoctorPaymentBillReprint.addPrivilege(new PrivilegeInfo(
                "ChangeReceiptPrintingPaperTypes",
                "Access to receipt printing configuration settings",
                "Controls visibility of the Settings button in print preview"
        ));
        registerPage(opdDoctorPaymentBillReprint);
    }

    /**
     * Register a page's metadata in the registry
     * @param metadata The page metadata to register
     */
    public void registerPage(PageMetadata metadata) {
        if (metadata != null && metadata.getPagePath() != null) {
            registry.put(metadata.getPagePath(), metadata);
        }
    }

    /**
     * Get metadata for a specific page
     * @param pagePath The page path (e.g., "inward/pharmacy_bill_issue_bht")
     * @return The page metadata, or null if not found
     */
    public PageMetadata getMetadata(String pagePath) {
        return registry.get(pagePath);
    }

    /**
     * Get all registered pages
     * @return List of all registered page metadata
     */
    public List<PageMetadata> getAllPages() {
        return new ArrayList<>(registry.values());
    }

    /**
     * Check if a page is registered
     * @param pagePath The page path to check
     * @return true if the page is registered
     */
    public boolean isPageRegistered(String pagePath) {
        return registry.containsKey(pagePath);
    }

    /**
     * Get the number of registered pages
     * @return The count of registered pages
     */
    public int getRegisteredPageCount() {
        return registry.size();
    }

    // Note: getRegistry() method removed to prevent exposing mutable internal state.
    // Use getAllPages(), getMetadata(pagePath), or getRegisteredPageCount() instead.
}
