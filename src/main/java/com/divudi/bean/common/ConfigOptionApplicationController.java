package com.divudi.bean.common;

import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.OptionValueType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.ConfigOption;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.ConfigOptionFacade;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named
@ApplicationScoped
public class ConfigOptionApplicationController implements Serializable {

    @EJB
    private ConfigOptionFacade optionFacade;

    private List<ConfigOption> options;
//    private List<Denomination> denominations;

    /**
     * Creates a new instance of OptionController
     */
    public ConfigOptionApplicationController() {
    }

    private Map<String, ConfigOption> applicationOptions;
    private boolean isLoadingApplicationOptions = false;

    private ConfigOption findActiveOptionWithLock(String key, OptionScope scope, Institution institution, Department department, WebUser webUser) {
        StringBuilder jpql = new StringBuilder("SELECT o FROM ConfigOption o WHERE o.retired=false AND o.optionKey=:key AND o.scope=:scope");
        Map<String, Object> params = new HashMap<>();
        params.put("key", key);
        params.put("scope", scope);
        if (institution != null) {
            jpql.append(" AND o.institution = :institution");
            params.put("institution", institution);
        } else {
            jpql.append(" AND o.institution IS NULL");
        }
        if (department != null) {
            jpql.append(" AND o.department = :department");
            params.put("department", department);
        } else {
            jpql.append(" AND o.department IS NULL");
        }
        if (webUser != null) {
            jpql.append(" AND o.webUser = :webUser");
            params.put("webUser", webUser);
        } else {
            jpql.append(" AND o.webUser IS NULL");
        }
        return optionFacade.findFirstByJpqlWithLock(jpql.toString(), params);
    }

    private ConfigOption createApplicationOptionIfAbsent(String key, OptionValueType type, String value) {
        ConfigOption option = optionFacade.createOptionIfNotExists(key, OptionScope.APPLICATION, null, null, null, type, value);
        if (!isLoadingApplicationOptions) {
            loadApplicationOptions();
        }
        return option;
    }

    @PostConstruct
    public void init() {
        loadApplicationOptions();
    }

    public void loadApplicationOptions() {
        isLoadingApplicationOptions = true;
        try {
            applicationOptions = new HashMap<>();
            List<ConfigOption> options = getApplicationOptions();
            for (ConfigOption option : options) {
                applicationOptions.put(option.getOptionKey(), option);
            }
            loadEmailGatewayConfigurationDefaults();
            loadPharmacyConfigurationDefaults();
            loadPharmacyIssueReceiptConfigurationDefaults();
            loadPharmacyTransferIssueReceiptConfigurationDefaults();
            loadPharmacyTransferReceiveReceiptConfigurationDefaults();
            loadPharmacyTransferRequestReceiptConfigurationDefaults();
            loadPharmacyDirectPurchaseWithoutCostingConfigurationDefaults();
            loadPharmacyCommonBillConfigurationDefaults();
            loadPharmacyAdjustmentReceiptConfigurationDefaults();
            loadPatientNameConfigurationDefaults();
            loadSecurityConfigurationDefaults();
            loadPharmacyAnalyticsConfigurationDefaults();
            loadReportMethodConfigurationDefaults();
        } finally {
            isLoadingApplicationOptions = false;
        }
    }

    private void loadEmailGatewayConfigurationDefaults() {
        getIntegerValueByKey("Email Gateway - SMTP Port", 587);
        getBooleanValueByKey("Email Gateway - SMTP Auth Enabled", true);
        getBooleanValueByKey("Email Gateway - StartTLS Enabled", true);
        getBooleanValueByKey("Email Gateway - SSL Enabled", false);
        // DO NOT set defaults for these, just trigger their presence in DB:
        getShortTextValueByKey("Email Gateway - Username", "");
        getShortTextValueByKey("Email Gateway - Password", "");
        getShortTextValueByKey("Email Gateway - SMTP Host", "");
        getShortTextValueByKey("Email Gateway - URL", "");
        //
        getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Send after one minute", false);
        getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Send after two minutes", false);
        getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Send after 5 minutes", false);
        getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Send after 10 minutes", true);
        getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Send after 15 minutes", false);
        getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Send after 20 minutes", false);
        getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Send after half an hour", false);
        getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Send after one hour", false);
        getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Send after two hours", false);
    }

    private void loadPharmacyConfigurationDefaults() {
        getDoubleValueByKey("Wholesale Rate Factor", 1.08);
        getDoubleValueByKey("Retail to Purchase Factor", 1.15);
        getDoubleValueByKey("Maximum Retail Price Change Percentage", 15.0);
        getBooleanValueByKey("Direct Issue Based On Retail Rate", true);
        getBooleanValueByKey("Direct Issue Based On Purchase Rate", false);
        getBooleanValueByKey("Direct Issue Based On Cost Rate", false);
        getBooleanValueByKey("Pharmacy Issue is by Purchase Rate", true);
        getBooleanValueByKey("Pharmacy Issue is by Cost Rate", false);
        getBooleanValueByKey("Pharmacy Issue is by Retail Rate", false);
        getBooleanValueByKey("Purchase Return Based On Purchase Rate", true);
        getBooleanValueByKey("Purchase Return Based On Line Cost Rate", false);
        getBooleanValueByKey("Purchase Return Based On Total Cost Rate", false);
        getBooleanValueByKey("Purchase Return by Quantity and Free Quantity", true);
        getBooleanValueByKey("Purchase Return by Total Quantity", false);
        getBooleanValueByKey("Show Profit Percentage in GRN", true);
        getBooleanValueByKey("Display Colours for Stock Autocomplete Items", true);
        getBooleanValueByKey("Enable Consignment in Pharmacy Purchasing", true);
        getBooleanValueByKey("Consignment Option is checked in new Pharmacy Purchasing Bills", false);
        getBooleanValueByKey("GRN Returns is only after Approval", true);
        getBooleanValueByKey("GRN Return can be done without Approval", true);

        // Bill Numbering Configuration Options - Added for improved bill numbering functionality
        // These options enable configurable bill numbering strategies across different bill types
        // Future development: Apply these patterns to additional bill types as needed

        // Generic bill numbering strategies (for backward compatibility)
        getBooleanValueByKey("Bill Number Generation Strategy for Department ID is Prefix Dept Ins Year Count", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Department ID is Prefix Ins Year Count", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Institution ID is Prefix Ins Year Count", false);

        // Bill-type-specific numbering strategies for Purchase Order Requests (POR)
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Purchase Order Request - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Purchase Order Request - Prefix + Institution Code + Year + Yearly Number", false);

        // Bill-type-specific numbering strategies for Purchase Order Approvals (POA)
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Purchase Order Approval - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Purchase Order Approval - Prefix + Institution Code + Year + Yearly Number", false);

        // Bill-type-specific numbering strategies for GRN
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy GRN - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy GRN - Prefix + Institution Code + Year + Yearly Number", false);

        // Bill-type-specific numbering strategies for Cancelled Purchase Order Requests (C-POR)
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Cancelled Purchase Order Request - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Cancelled Purchase Order Request - Prefix + Institution Code + Year + Yearly Number", false);

        // Bill-type-specific numbering strategies for Cancelled GRN (C-GRN)
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Cancelled GRN - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Cancelled GRN - Prefix + Institution Code + Year + Yearly Number", false);

        // Bill-type-specific numbering strategies for GRN Return (GRNR)
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy GRN Return - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy GRN Return - Prefix + Institution Code + Year + Yearly Number", false);


        // Bill-type-specific numbering strategies for Transfer Issue (TI)
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Transfer Issue - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Transfer Issue - Prefix + Institution Code + Year + Yearly Number", false);

        // Bill-type-specific numbering strategies for Sale Pre Bill (SPB)
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Institution Code + Year + Yearly Number", false);

        // Bill-type-specific numbering strategies for Sale Cashier Pre Bill (SCPB)
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Year + Yearly Number", false);

        // Bill-type-specific numbering strategies for Sale Refund (SR)
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund - Prefix + Institution Code + Year + Yearly Number", false);

        // Bill-type-specific numbering strategies for Sale Refund Pre Bill (SRP)
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund Pre Bill - Prefix + Institution Code + Year + Yearly Number", false);
      
        // Bill-type-specific numbering strategies for Disposal Issue (DI)
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Disposal Issue - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Disposal Issue - Prefix + Institution Code + Year + Yearly Number", false);


        // Bill-type-specific numbering strategies for Transfer Receive (TR)
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Transfer Receive - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Transfer Receive - Prefix + Institution Code + Year + Yearly Number", false);

        // Bill-type-specific numbering strategies for Sale Cancel (SC)
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cancel - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cancel - Prefix + Institution Code + Year + Yearly Number", false);

        // Bill-type-specific numbering strategies for Direct Purchase Refund (DPR)
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Direct Purchase Refund - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Direct Purchase Refund - Prefix + Institution Code + Year + Yearly Number", false);

        // Bill-type-specific numbering strategies for Issue Return (IR)
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Return - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Return - Prefix + Institution Code + Year + Yearly Number", false);


        // Bill-type-specific numbering strategies for Issue Cancelled (IC)
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Cancelled - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Cancelled - Prefix + Institution Code + Year + Yearly Number", false);

        // Bill-type-specific numbering strategies for Transfer Receive (TR)
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Transfer Receive - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Transfer Receive - Prefix + Institution Code + Year + Yearly Number", false);

        // Bill-type-specific numbering strategies for Transfer Request (TRQ)
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Transfer Request - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
        getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Transfer Request - Prefix + Institution Code + Year + Yearly Number", false);

        // Bill Number Suffix Configuration Options - Default suffixes for different bill types
        // These provide default values when bill number suffix configurations are empty
        getShortTextValueByKey("Bill Number Suffix for Purchase Order Request", "POR");
        getShortTextValueByKey("Bill Number Suffix for Purchase Order Approval", "POA");
        getShortTextValueByKey("Bill Number Suffix for Cancelled Purchase Order Request", "C-POR");
        getShortTextValueByKey("Bill Number Suffix for Cancelled GRN", "C-GRN");
        getShortTextValueByKey("Bill Number Suffix for GRN Return", "GRNR");
        getShortTextValueByKey("Bill Number Suffix for GRN", "GRN");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_DIRECT_PURCHASE", "DP");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_DIRECT_PURCHASE_CANCELLED", "C-DP");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_ISSUE", "TI");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_DIRECT_ISSUE", "DTI");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_RETAIL_SALE_PRE", "SPB");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER", "SCPB");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_RETAIL_SALE", "SB");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS", "SR");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS_PREBILL", "SRP");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_RETAIL_SALE_CANCELLED", "SC");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_DIRECT_PURCHASE_REFUND", "DPR");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_ISSUE_CANCELLED", "C-DIS");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_RECEIVE", "TR");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_DISPOSAL_ISSUE", "DIS");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_RETAIL_SALE_CANCELLED", "SC");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_DIRECT_PURCHASE_REFUND", "DPR");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_ISSUE_CANCELLED", "C-DIS");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_RECEIVE", "TR");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_TRANSFER_REQUEST", "PHTRQ");
        getShortTextValueByKey("Bill Number Suffix for PHARMACY_TRANSFER_REQUEST_PRE", "PHTRQ-PRE");
    }

    private void loadPharmacyIssueReceiptConfigurationDefaults() {
        getLongTextValueByKey("Pharmacy Issue Receipt CSS",
                ".receipt-container {\n"
                + "    font-family: Verdana, sans-serif;\n"
                + "    font-size: 12px;\n"
                + "    color: #000;\n"
                + "}\n"
                + ".receipt-header, .receipt-title, .receipt-separator, .receipt-summary {\n"
                + "    margin-bottom: 10px;\n"
                + "}\n"
                + ".receipt-institution-name {\n"
                + "    font-weight: bold;\n"
                + "    font-size: 16px;\n"
                + "    text-align: center;\n"
                + "}\n"
                + ".receipt-institution-contact {\n"
                + "    text-align: center;\n"
                + "    font-size: 11px;\n"
                + "}\n"
                + ".receipt-title {\n"
                + "    text-align: center;\n"
                + "    font-size: 14px;\n"
                + "    font-weight: bold;\n"
                + "    text-decoration: underline;\n"
                + "}\n"
                + ".receipt-details-table, .receipt-items-table, .receipt-summary-table {\n"
                + "    width: 100%;\n"
                + "    border-collapse: collapse;\n"
                + "}\n"
                + ".receipt-items-header {\n"
                + "    font-weight: bold;\n"
                + "    border-bottom: 1px solid #ccc;\n"
                + "}\n"
                + ".item-name, .item-qty, .item-rate, .item-value {\n"
                + "    padding: 4px;\n"
                + "    text-align: left;\n"
                + "}\n"
                + ".item-qty, .item-rate, .item-value {\n"
                + "    text-align: right;\n"
                + "}\n"
                + ".summary-label {\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".summary-value {\n"
                + "    text-align: right;\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".total-amount {\n"
                + "    font-size: 14px;\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".receipt-cashier {\n"
                + "    margin-top: 20px;\n"
                + "    text-align: right;\n"
                + "    text-decoration: overline;\n"
                + "}"
        );
    }

    private void loadPharmacyTransferIssueReceiptConfigurationDefaults() {
        getLongTextValueByKey("Pharmacy Transfer Issue Receipt CSS",
                ".receipt-container {\n"
                + "    font-family: Verdana, sans-serif;\n"
                + "    font-size: 12px;\n"
                + "    color: #000;\n"
                + "    width: 21cm;\n"
                + "    margin: auto;\n"
                + "}\n"
                + ".receipt-header, .receipt-title, .receipt-separator, .receipt-summary {\n"
                + "    margin-bottom: 10px;\n"
                + "}\n"
                + ".receipt-institution-name {\n"
                + "    font-weight: bold;\n"
                + "    font-size: 16px;\n"
                + "    text-align: center;\n"
                + "}\n"
                + ".receipt-institution-contact {\n"
                + "    text-align: center;\n"
                + "    font-size: 11px;\n"
                + "}\n"
                + ".receipt-title {\n"
                + "    text-align: center;\n"
                + "    font-size: 14px;\n"
                + "    font-weight: bold;\n"
                + "    text-decoration: underline;\n"
                + "}\n"
                + ".receipt-details-table, .receipt-items-table, .receipt-summary-table {\n"
                + "    width: 100%;\n"
                + "    border-collapse: collapse;\n"
                + "}\n"
                + ".receipt-items-header {\n"
                + "    font-weight: bold;\n"
                + "    border-bottom: 1px solid #ccc;\n"
                + "}\n"
                + ".item-name, .item-qty, .item-rate, .item-value {\n"
                + "    padding: 4px;\n"
                + "    text-align: left;\n"
                + "}\n"
                + ".item-qty, .item-rate, .item-value {\n"
                + "    text-align: right;\n"
                + "}\n"
                + ".summary-label {\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".summary-value {\n"
                + "    text-align: right;\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".total-amount {\n"
                + "    font-size: 14px;\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".receipt-cashier {\n"
                + "    margin-top: 20px;\n"
                + "    text-align: right;\n"
                + "    text-decoration: overline;\n"
                + "}\n"
                + "@media print {\n"
                + "  .receipt-container {\n"
                + "    width: 21cm;\n"
                + "    margin: auto;\n"
                + "    page-break-after: always;\n"
                + "  }\n"
                + "}"
        );
    }

    private void loadPharmacyTransferReceiveReceiptConfigurationDefaults() {
        getLongTextValueByKey("Pharmacy Transfer Receive Receipt CSS",
                ".receipt-container {\n"
                + "    font-family: Verdana, sans-serif;\n"
                + "    font-size: 12px;\n"
                + "    color: #000;\n"
                + "    width: 21cm;\n"
                + "    margin: auto;\n"
                + "}\n"
                + ".receipt-header, .receipt-title, .receipt-separator, .receipt-summary {\n"
                + "    margin-bottom: 10px;\n"
                + "}\n"
                + ".receipt-institution-name {\n"
                + "    font-weight: bold;\n"
                + "    font-size: 16px;\n"
                + "    text-align: center;\n"
                + "}\n"
                + ".receipt-institution-contact {\n"
                + "    text-align: center;\n"
                + "    font-size: 11px;\n"
                + "}\n"
                + ".receipt-title {\n"
                + "    text-align: center;\n"
                + "    font-size: 14px;\n"
                + "    font-weight: bold;\n"
                + "    text-decoration: underline;\n"
                + "}\n"
                + ".receipt-details-table, .receipt-items-table, .receipt-summary-table {\n"
                + "    width: 100%;\n"
                + "    border-collapse: collapse;\n"
                + "}\n"
                + ".receipt-items-header {\n"
                + "    font-weight: bold;\n"
                + "    border-bottom: 1px solid #ccc;\n"
                + "}\n"
                + ".item-name, .item-qty, .item-rate, .item-value {\n"
                + "    padding: 4px;\n"
                + "    text-align: left;\n"
                + "}\n"
                + ".item-qty, .item-rate, .item-value {\n"
                + "    text-align: right;\n"
                + "}\n"
                + ".summary-label {\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".summary-value {\n"
                + "    text-align: right;\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".total-amount {\n"
                + "    font-size: 14px;\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".receipt-cashier {\n"
                + "    margin-top: 20px;\n"
                + "    text-align: right;\n"
                + "    text-decoration: overline;\n"
                + "}\n"
                + "@media print {\n"
                + "  .receipt-container {\n"
                + "    width: 21cm;\n"
                + "    margin: auto;\n"
                + "    page-break-after: always;\n"
                + "  }\n"
                + "}"
        );
        getLongTextValueByKey("Pharmacy Transfer Receive Receipt Header",
                "<table class=\"receipt-header-table\">\n"
                + "    <!-- Institution Details -->\n"
                + "    <tr>\n"
                + "        <td colspan=\"2\" class=\"receipt-institution-name\">\n"
                + "            {{institution_name}}\n"
                + "        </td>\n"
                + "    </tr>\n"
                + "    <tr>\n"
                + "        <td colspan=\"2\" class=\"receipt-institution-contact\">\n"
                + "            {{institution_address}}\n"
                + "        </td>\n"
                + "    </tr>\n"
                + "    <tr>\n"
                + "        <td colspan=\"2\" class=\"receipt-institution-contact\">\n"
                + "            {{institution_phones}}\n"
                + "        </td>\n"
                + "    </tr>\n"
                + "    <tr>\n"
                + "        <td colspan=\"2\" class=\"receipt-institution-contact\">\n"
                + "            {{institution_fax}}\n"
                + "        </td>\n"
                + "    </tr>\n"
                + "    <tr>\n"
                + "        <td colspan=\"2\" class=\"receipt-institution-contact\">\n"
                + "            {{institution_email}}\n"
                + "        </td>\n"
                + "    </tr>\n"
                + "    <!-- Bill Heading -->\n"
                + "    <tr>\n"
                + "        <td colspan=\"2\" class=\"receipt-title\">\n"
                + "            Transfer Receive Note{{cancelled_status}}\n"
                + "        </td>\n"
                + "    </tr>\n"
                + "    <!-- Bill Details -->\n"
                + "    <tr>\n"
                + "        <td class=\"receipt-details-cell\">\n"
                + "            Location From: {{location_from}}\n"
                + "        </td>\n"
                + "        <td class=\"receipt-details-cell\">\n"
                + "            Location To: {{location_to}}\n"
                + "        </td>\n"
                + "    </tr>\n"
                + "    <tr>\n"
                + "        <td class=\"receipt-details-cell\">\n"
                + "            Received Person: {{received_person}}\n"
                + "        </td>\n"
                + "        <td class=\"receipt-details-cell\">\n"
                + "            Issued Person: {{issued_person}}\n"
                + "        </td>\n"
                + "    </tr>\n"
                + "    <tr>\n"
                + "        <td class=\"receipt-details-cell\">\n"
                + "            Receive No: {{receive_no}}\n"
                + "        </td>\n"
                + "        <td class=\"receipt-details-cell\">\n"
                + "            Issue No: {{issue_no}}\n"
                + "        </td>\n"
                + "    </tr>\n"
                + "    <tr>\n"
                + "        <td class=\"receipt-details-cell\">\n"
                + "            Received Time: {{received_time}}\n"
                + "        </td>\n"
                + "        <td class=\"receipt-details-cell\">\n"
                + "            Issue Time: {{issue_time}}\n"
                + "        </td>\n"
                + "    </tr>\n"
                + "</table>\n");
        getLongTextValueByKey("Pharmacy Transfer Receive Receipt Footer", "");
    }

    private void loadPharmacyTransferRequestReceiptConfigurationDefaults() {
        getLongTextValueByKey("Pharmacy Transfer Request Receipt CSS",
                ".receipt-container {\n"
                + "    font-family: Verdana, sans-serif;\n"
                + "    font-size: 12px;\n"
                + "    color: #000;\n"
                + "    width: 21cm;\n"
                + "    margin: auto;\n"
                + "}\n"
                + ".receipt-header, .receipt-title, .receipt-separator, .receipt-summary {\n"
                + "    margin-bottom: 10px;\n"
                + "}\n"
                + ".receipt-institution-name {\n"
                + "    font-weight: bold;\n"
                + "    font-size: 16px;\n"
                + "    text-align: center;\n"
                + "}\n"
                + ".receipt-institution-contact {\n"
                + "    text-align: center;\n"
                + "    font-size: 11px;\n"
                + "}\n"
                + ".receipt-title {\n"
                + "    text-align: center;\n"
                + "    font-size: 14px;\n"
                + "    font-weight: bold;\n"
                + "    text-decoration: underline;\n"
                + "}\n"
                + ".receipt-details-table, .receipt-items-table, .receipt-summary-table {\n"
                + "    width: 100%;\n"
                + "    border-collapse: collapse;\n"
                + "}\n"
                + ".receipt-items-header {\n"
                + "    font-weight: bold;\n"
                + "    border-bottom: 1px solid #ccc;\n"
                + "}\n"
                + ".item-name, .item-qty, .item-rate, .item-value {\n"
                + "    padding: 4px;\n"
                + "    text-align: left;\n"
                + "}\n"
                + ".item-qty, .item-rate, .item-value {\n"
                + "    text-align: right;\n"
                + "}\n"
                + ".summary-label {\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".summary-value {\n"
                + "    text-align: right;\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".total-amount {\n"
                + "    font-size: 14px;\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".receipt-cashier {\n"
                + "    margin-top: 20px;\n"
                + "    text-align: right;\n"
                + "    text-decoration: overline;\n"
                + "}\n"
                + "@media print {\n"
                + "  .receipt-container {\n"
                + "    width: 21cm;\n"
                + "    margin: auto;\n"
                + "    page-break-after: always;\n"
                + "  }\n"
                + "}"
        );
        getLongTextValueByKey("Pharmacy Transfer Request Receipt Header",
                "<table class=\"receipt-details-table\">\n"
                + "    <tr>\n"
                + "        <td>Request From</td>\n"
                + "        <td>:</td>\n"
                + "        <td>\n"
                + "            {{from_dept}} ({{from_ins}})\n"
                + "        </td>\n"
                + "    </tr>\n"
                + "    <tr>\n"
                + "        <td>Request To</td>\n"
                + "        <td>:</td>\n"
                + "        <td>\n"
                + "            {{to_dept}} ({{to_ins}})\n"
                + "        </td>\n"
                + "    </tr>\n"
                + "    <tr>\n"
                + "        <td>Req No</td>\n"
                + "        <td>:</td>\n"
                + "        <td>{{bill_id}}</td>\n"
                + "    </tr>\n"
                + "    <tr>\n"
                + "        <td>Req By</td>\n"
                + "        <td>:</td>\n"
                + "        <td>{{user}}</td>\n"
                + "    </tr>\n"
                + "    <tr>\n"
                + "        <td>Req Date/Time</td>\n"
                + "        <td>:</td>\n"
                + "        <td>\n"
                + "           {{bill_date}}\n"
                + "        </td>\n"
                + "    </tr>\n"
                + "    <tr>\n"
                + "        <td>Document Status</td>\n"
                + "        <td>:</td>\n"
                + "        <td>{{bill_status}}</td>\n"
                + "    </tr>\n"
                + "</table>\n");
        getBooleanValueByKey("Pharmacy Transfer Request - Show Rate and Value", false);

    }

    private void loadPharmacyDirectPurchaseWithoutCostingConfigurationDefaults() {
        getLongTextValueByKey("Pharmacy Direct Purchase without Costing Receipt CSS",
                ".receipt-container {\n"
                + "    font-family: Verdana, sans-serif;\n"
                + "    font-size: 12px;\n"
                + "    color: #000;\n"
                + "    page-break-inside: avoid;\n"
                + "}\n"
                + ".receipt-header-section {\n"
                + "    padding: 10px 0;\n"
                + "    page-break-after: avoid;\n"
                + "}\n"
                + ".receipt-body-section {\n"
                + "    padding: 10px 0;\n"
                + "    page-break-after: always;\n"
                + "}\n"
                + ".receipt-footer-section {\n"
                + "    padding: 10px 0;\n"
                + "    page-break-before: always;\n"
                + "}\n"
                + ".receipt-institution-name {\n"
                + "    font-weight: bold;\n"
                + "    font-size: 23px;\n"
                + "    text-align: center;\n"
                + "    font-family: monospace;\n"
                + "    text-transform: capitalize;\n"
                + "}\n"
                + ".receipt-institution-contact {\n"
                + "    text-align: center;\n"
                + "    font-size: 16px;\n"
                + "    font-family: monospace;\n"
                + "}\n"
                + ".receipt-title {\n"
                + "    text-align: center;\n"
                + "    font-size: 18px;\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".receipt-details-table {\n"
                + "    font-size: 16px;\n"
                + "    font-family: sans-serif;\n"
                + "    width: 100%;\n"
                + "    border-collapse: collapse;\n"
                + "}\n"
                + ".receipt-items-table {\n"
                + "    font-size: 16px;\n"
                + "    width: 100%;\n"
                + "    border-collapse: collapse;\n"
                + "    margin-left: 3%;\n"
                + "    margin-right: 3%;\n"
                + "}\n"
                + ".receipt-items-table td, .receipt-items-table th {\n"
                + "    padding: 4px;\n"
                + "    text-align: right;\n"
                + "}\n"
                + ".receipt-items-table td:first-child {\n"
                + "    text-align: left;\n"
                + "}\n"
                + ".receipt-cashier {\n"
                + "    margin-top: 20px;\n"
                + "    margin-left: 3%;\n"
                + "    margin-right: 3%;\n"
                + "    text-align: right;\n"
                + "}\n"
                + ".showRetailValue {\n"
                + "    display: table-cell;\n"
                + "}\n"
                + ".hideRetailValue {\n"
                + "    display: none;\n"
                + "}\n"
                + ".showProfit {\n"
                + "    display: table-cell;\n"
                + "}\n"
                + ".hideProfit {\n"
                + "    display: none;\n"
                + "}\n"
                + "@media print {\n"
                + "  .receipt-body-section {\n"
                + "    page-break-after: always;\n"
                + "  }\n"
                + "  .receipt-footer-section {\n"
                + "    page-break-before: always;\n"
                + "  }\n"
                + "}"
        );
    }

    private void loadPharmacyCommonBillConfigurationDefaults() {
        getLongTextValueByKey("Pharmacy Common Bill CSS",
                ".receipt-container {\n"
                + "    font-family: Verdana, sans-serif;\n"
                + "    font-size: 12px;\n"
                + "    color: #000;\n"
                + "    width: 21cm;\n"
                + "    margin: auto;\n"
                + "    page-break-inside: avoid;\n"
                + "}\n"
                + ".receipt-header {\n"
                + "    margin-bottom: 15px;\n"
                + "    text-align: center;\n"
                + "}\n"
                + ".receipt-institution-name {\n"
                + "    font-weight: bold;\n"
                + "    font-size: 18px;\n"
                + "    margin-bottom: 5px;\n"
                + "}\n"
                + ".receipt-institution-contact {\n"
                + "    font-size: 10px;\n"
                + "    margin-bottom: 10px;\n"
                + "}\n"
                + ".receipt-title {\n"
                + "    text-align: center;\n"
                + "    font-size: 16px;\n"
                + "    font-weight: bold;\n"
                + "    margin: 15px 0;\n"
                + "    text-decoration: underline;\n"
                + "}\n"
                + ".receipt-separator {\n"
                + "    margin: 10px 0;\n"
                + "    border-top: 1px solid #333;\n"
                + "}\n"
                + ".receipt-details-table {\n"
                + "    width: 100%;\n"
                + "    margin-bottom: 15px;\n"
                + "    border-collapse: collapse;\n"
                + "}\n"
                + ".receipt-details-table td {\n"
                + "    padding: 3px 5px;\n"
                + "    vertical-align: top;\n"
                + "}\n"
                + ".receipt-details-table td:first-child {\n"
                + "    font-weight: bold;\n"
                + "    width: 20%;\n"
                + "}\n"
                + ".receipt-details-table td:nth-child(2) {\n"
                + "    width: 5%;\n"
                + "    text-align: center;\n"
                + "}\n"
                + ".noBorder, .noBorder td, .noBorder th {\n"
                + "    border: none !important;\n"
                + "}\n"
                + ".normalFont {\n"
                + "    font-size: 12px;\n"
                + "}\n"
                + ".text-end {\n"
                + "    text-align: right;\n"
                + "}\n"
                + "@media print {\n"
                + "    .receipt-container {\n"
                + "        margin: 0;\n"
                + "        page-break-after: always;\n"
                + "    }\n"
                + "}\n"
        );
        getLongTextValueByKey("Pharmacy Common Bill Header", "");
        getLongTextValueByKey("Pharmacy Common Bill Footer", "");
    }

    private void loadPharmacyAdjustmentReceiptConfigurationDefaults() {
        // Purchase Rate Adjustment specific configurations
        getLongTextValueByKey("Pharmacy Adjustment Purchase Rate CSS", "");
        getLongTextValueByKey("Pharmacy Adjustment Purchase Rate Header", "");
        getLongTextValueByKey("Pharmacy Adjustment Purchase Rate Footer", "");

        // Cost Rate Adjustment specific configurations
        getLongTextValueByKey("Pharmacy Adjustment Cost Rate CSS", "");
        getLongTextValueByKey("Pharmacy Adjustment Cost Rate Header", "");
        getLongTextValueByKey("Pharmacy Adjustment Cost Rate Footer", "");

        // Retail Rate Adjustment specific configurations
        getLongTextValueByKey("Pharmacy Adjustment Retail Rate CSS", "");
        getLongTextValueByKey("Pharmacy Adjustment Retail Rate Header", "");
        getLongTextValueByKey("Pharmacy Adjustment Retail Rate Footer", "");

        // Stock Adjustment specific configurations
        getLongTextValueByKey("Pharmacy Adjustment Stock CSS", "");
        getLongTextValueByKey("Pharmacy Adjustment Stock Header", "");
        getLongTextValueByKey("Pharmacy Adjustment Stock Footer", "");

        // Wholesale Rate Adjustment specific configurations
        getLongTextValueByKey("Pharmacy Adjustment Wholesale Rate CSS", "");
        getLongTextValueByKey("Pharmacy Adjustment Wholesale Rate Header", "");
        getLongTextValueByKey("Pharmacy Adjustment Wholesale Rate Footer", "");
    }

    private void loadPatientNameConfigurationDefaults() {
        getBooleanValueByKey("Capitalize Entire Patient Name", false);
        getBooleanValueByKey("Capitalize Each Word in Patient Name", false);
    }

    private void loadSecurityConfigurationDefaults() {
        getBooleanValueByKey("prevent_password_reuse", false);
        // Admin-triggered JPA L2 cache clear is disabled by default
        getBooleanValueByKey("Allow manual JPA cache clear", false);
    }

    private void loadPharmacyAnalyticsConfigurationDefaults() {
        List<String> tabOptions = Arrays.asList(
                "Pharmacy Analytics - Show Pharmacy Analytics Summary Reports Tab",
                "Pharmacy Analytics - Show Pharmacy Analytics Financial Reports Tab",
                "Pharmacy Analytics - Show Pharmacy Analytics Stock Reports Tab",
                "Pharmacy Analytics - Show Pharmacy Analytics Item Reports Tab",
                "Pharmacy Analytics - Show Pharmacy Analytics Movement Reports Tab",
                "Pharmacy Analytics - Show Pharmacy Analytics Retail Sale Reports Tab",
                "Pharmacy Analytics - Show Pharmacy Analytics Wholesale Reports Tab",
                "Pharmacy Analytics - Show Pharmacy Analytics Inpatient Reports Tab",
                "Pharmacy Analytics - Show Pharmacy Analytics Procurement Reports Tab",
                "Pharmacy Analytics - Show Pharmacy Analytics Disbursement Reports Tab",
                "Pharmacy Analytics - Show Pharmacy Analytics Adjustment Reports Tab",
                "Pharmacy Analytics - Show Pharmacy Analytics Disposal Reports Tab"
        );
        tabOptions.forEach(k -> getBooleanValueByKey(k, true));

        List<String> buttonOptions = Arrays.asList(
                "Pharmacy Analytics - Show Pharmacy Income Report",
                "Pharmacy Analytics - Show Pharmacy Income and Cost",
                "Pharmacy Analytics - Show Daily Stock Values",
                "Pharmacy Analytics - Show Bill Types",
                "Pharmacy Analytics - Show Cash In/Out Report",
                "Pharmacy Analytics - Show Cashier Report",
                "Pharmacy Analytics - Show Cashier Summary",
                "Pharmacy Analytics - Show All Cashier Report",
                "Pharmacy Analytics - Show All Cashier Summary",
                "Pharmacy Analytics - Show Cashier Detailed Report by Department",
                "Pharmacy Analytics - Show Pharmacy Sale Summary",
                "Pharmacy Analytics - Show Pharmacy Sale Summary Date",
                "Pharmacy Analytics - Show All Department Sale Summary",
                "Pharmacy Analytics - Show Sale Summary - By Bill Type",
                "Pharmacy Analytics - Show Sale Summary - By Payment Method",
                "Pharmacy Analytics - Show Sale Summary - By Payment Method (By Bill)",
                "Pharmacy Analytics - Show Stock Overview Report",
                "Pharmacy Analytics - Show Stock Report by Batch",
                "Pharmacy Analytics - Show Expiring Stock Report by Batch",
                "Pharmacy Analytics - Show Stock Report by Expiry",
                "Pharmacy Analytics - Show Zero Stock Item Report",
                "Pharmacy Analytics - Show Suppliers Expiring Stocks",
                "Pharmacy Analytics - Show Stock Report by Item",
                "Pharmacy Analytics - Show Stock Report by Item - Order by VMP",
                "Pharmacy Analytics - Show Stock Report by Product",
                "Pharmacy Analytics - Show Stock Report of Single Product",
                "Pharmacy Analytics - Show Supplier Stock Report",
                "Pharmacy Analytics - Show Suppliers Stock Summary",
                "Pharmacy Analytics - Show Category Stock Report",
                "Pharmacy Analytics - Show Category Stock Summary",
                "Pharmacy Analytics - Show All Staff Stock",
                "Pharmacy Analytics - Show Stock History",
                "Pharmacy Analytics - Show Before Stock Taking Report",
                "Pharmacy Analytics - Show After Stock Taking Report",
                "Pharmacy Analytics - Show Stock Taking Report(New)",
                "Pharmacy Analytics - Show Stock With Movement",
                "Pharmacy Analytics - Show Stock Summary (with Suppliers)",
                "Pharmacy Analytics - Show Stock Report (with Suppliers)",
                "Pharmacy Analytics - Show Stock Report by Batch for Export",
                "Pharmacy Analytics - Show Bin Card",
                "Pharmacy Analytics - Show Items (AMP) List",
                "Pharmacy Analytics - Show Medicine (VTM,ATM,VMP,AMP,VMPP and AMPP) List",
                "Pharmacy Analytics - Show Single Items Summary",
                "Pharmacy Analytics - Show All Items Summary",
                "Pharmacy Analytics - Show Items Without Distributor",
                "Pharmacy Analytics - Show Items With Suppliers and Prices",
                "Pharmacy Analytics - Show Items With Distributor",
                "Pharmacy Analytics - Show Items With Multiple Distributor(Items Only)",
                "Pharmacy Analytics - Show Item With Multiple Distributor",
                "Pharmacy Analytics - Show ROL and ROQ Management",
                "Pharmacy Analytics - Show Reorder Analysis",
                "Pharmacy Analytics - Show Movement Report Stock By Date",
                "Pharmacy Analytics - Show Movement Report Stock By Date - By Batch",
                "Pharmacy Analytics - Show Pharmacy All Report",
                "Pharmacy Analytics - Show Movement Out by Sale, Issue, and Consumption with Current Stock Report",
                "Pharmacy Analytics - Show Sale Report",
                "Pharmacy Analytics - Show Prescription Report",
                "Pharmacy Analytics - Show Institution Item Movement",
                "Pharmacy Analytics - Show Fast Moving",
                "Pharmacy Analytics - Show Slow Moving",
                "Pharmacy Analytics - Show Non Moving",
                "Pharmacy Analytics - Show Prescription Summary",
                "Pharmacy Analytics - Show Presciption List",
                "Pharmacy Analytics - Show List of Pharmacy Bills",
                "Pharmacy Analytics - Show Retail Sale Bill List",
                "Pharmacy Analytics - Show Sale Detail - By Bill",
                "Pharmacy Analytics - Show Sale Detail - By Bill Items",
                "Pharmacy Analytics - Show Sale Detail - By Discount Scheme",
                "Pharmacy Analytics - Show Sale Summary By Discount Scheme Summary",
                "Pharmacy Analytics - Show Sale Detail - By Payment Method",
                "Pharmacy Analytics - Show Pharmacy Sale Report",
                "Pharmacy Analytics - Show Pharmacy Wholesale Report",
                "Pharmacy Analytics - Show Pharmacy Wholesale Credit Bills",
                "Pharmacy Analytics - Show BHT Issue - By Bill",
                "Pharmacy Analytics - Show BHT Issue - By Bill Item",
                "Pharmacy Analytics - Show BHT Issue - By Item",
                "Pharmacy Analytics - Show BHT Issue - Staff",
                "Pharmacy Analytics - Show BHT Issue With Margin Report",
                "Pharmacy Analytics - Show Pharmacy Procurement Report",
                "Pharmacy Analytics - Show GRN Summary",
                "Pharmacy Analytics - Show Department Stock By Batch",
                "Pharmacy Analytics - Show Purchase Orders Not Approved",
                "Pharmacy Analytics - Show Department Stock By Batch to Upload",
                "Pharmacy Analytics - Show Item - wise Purchase/Good Receive",
                "Pharmacy Analytics - Show Purcharse Bill with Supplier",
                "Pharmacy Analytics - Show Pharmacy GRN Report",
                "Pharmacy Analytics - Show Pharmacy GRN and purchase Report",
                "Pharmacy Analytics - Show GRN Purchase Items by Supplier",
                "Pharmacy Analytics - Show GRN Summary By Supplier",
                "Pharmacy Analytics - Show GRN Bill Item Report",
                "Pharmacy Analytics - Show GRN Registry",
                "Pharmacy Analytics - Show GRN Return List",
                "Pharmacy Analytics - Show Purchase Order Summary",
                "Pharmacy Analytics - Show Purchase Bills by Department",
                "Pharmacy Analytics - Show Purchase Summary By Supplier",
                "Pharmacy Analytics - Show Purchase Summary (Credit / Cash )",
                "Pharmacy Analytics - Show Purchase and GRN Summary (Credit / Cash )",
                "Pharmacy Analytics - Show Purchase Summary By Supplier (Credit / Cash)",
                "Pharmacy Analytics - Show Purchase Bill Item",
                "Pharmacy Analytics - Show GRN Payment Summary",
                "Pharmacy Analytics - Show GRN Payment Summary By Supplier",
                "Pharmacy Analytics - Show Pharmacy Return Without Traising",
                "Pharmacy Analytics - Show Procurement Bill Item List",
                "Pharmacy Analytics - Show Transfer Issue By Bill Item",
                "Pharmacy Analytics - Show Transfer Receive By Bill Item",
                "Pharmacy Analytics - Show Transfer Issue by Bill",
                "Pharmacy Analytics - Show Transfer Receive by Bill",
                "Pharmacy Analytics - Show Transfer Issue by Bill(Summary)",
                "Pharmacy Analytics - Show Transfer Receive by Bill(Summary)",
                "Pharmacy Analytics - Show Report Transfer Issued not Recieved",
                "Pharmacy Analytics - Show Staff Stock Report",
                "Pharmacy Analytics - Show Transfer Report Summary",
                "Pharmacy Analytics - Show Transfer Issue Summary Report By Date",
                "Pharmacy Analytics - Show Transfer Receive Vs BHT Issue Quntity Totals By Item",
                "Pharmacy Analytics - Show Item-vice adjustments",
                "Pharmacy Analytics - Show Unit Issue by bill",
                "Pharmacy Analytics - Show Unit Issue by Department",
                "Pharmacy Analytics - Show Unit Issue by Item (Batch)",
                "Pharmacy Analytics - Show Unit Issue by Item"
        );

        buttonOptions.forEach(k -> getBooleanValueByKey(k, true));
    }

    private void loadReportMethodConfigurationDefaults() {
        getBooleanValueByKey("Laboratory Income Report - Legacy Method", true);
        getBooleanValueByKey("Laboratory Income Report - Optimized Method", false);
        // OPD Reports
        getBooleanValueByKey("OPD Itemized Sale Summary - Legacy Method", true);
        getBooleanValueByKey("OPD Itemized Sale Summary - Optimized Method", false);

        // Lab Reports
        getBooleanValueByKey("Lab Daily Summary Report - Legacy Method", true);
        getBooleanValueByKey("Lab Daily Summary Report - Optimized Method", false);
        getBooleanValueByKey("Test Wise Count Report - Legacy Method", true);
        getBooleanValueByKey("Test Wise Count Report - Optimized Method", false);
        getBooleanValueByKey("Laboratory Income Report - Legacy Method", true);
        getBooleanValueByKey("Laboratory Income Report - Optimized Method", false);

        // OPD Reports
        getBooleanValueByKey("OPD Itemized Sale Summary - Legacy Method", true);
        getBooleanValueByKey("OPD Itemized Sale Summary - Optimized Method", false);
        getBooleanValueByKey("OPD Income Report - Legacy Method", true);
        getBooleanValueByKey("OPD Income Report - Optimized Method", false);

        // Pharmacy Reports
        getBooleanValueByKey("Pharmacy Transfer Issue Bill Report - Legacy Method", true);
        getBooleanValueByKey("Pharmacy Transfer Issue Bill Report - Optimized Method", false);
        getBooleanValueByKey("Pharmacy Income Report - Legacy Method", true);
        getBooleanValueByKey("Pharmacy Income Report - Optimized Method", false);
        getBooleanValueByKey("Pharmacy Search Sale Bill - Legacy Method", true);
        getBooleanValueByKey("Pharmacy Search Sale Bill - Optimized Method", false);

    }

    public ConfigOption getApplicationOption(String key) {
        if (applicationOptions == null) {
            loadApplicationOptions();
        }
        ConfigOption c = applicationOptions.get(key);
        return c;
    }

    public void saveOption(ConfigOption option) {
        if (option == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }
        if (option.getId() == null) {
            option.setCreatedAt(new Date());
            optionFacade.create(option);
        } else {
            optionFacade.edit(option);
        }
    }

    public void saveShortTextOption(String key, String value) {
        ConfigOption option = getApplicationOption(key);
        if (option == null) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.SHORT_TEXT, value);
        }
    }

    public <E extends Enum<E>> E getEnumValue(ConfigOption option, Class<E> enumClass) {
        if (option.getEnumType() == null || option.getEnumValue() == null) {
            return null; // Or throw an exception if appropriate
        }
        if (!option.getEnumType().equals(enumClass.getName())) {
            throw new IllegalArgumentException("The option does not match the expected enum type.");
        }
        return E.valueOf(enumClass, option.getEnumValue());
    }

    public <E extends Enum<E>> E getEnumValueByKey(String key, Class<E> enumClass) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.ENUM || !option.getEnumType().equals(enumClass.getName())) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.ENUM, "");
            option.setEnumType(enumClass.getName());
            optionFacade.edit(option);
        }

        return getEnumValue(option, enumClass);
    }

    public Integer getIntegerValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.INTEGER) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.INTEGER, "0");
        }
        try {
            return Integer.valueOf(option.getOptionValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Integer getIntegerValueByKey(String key, Integer defaultValue) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.INTEGER) {
            String dv = defaultValue == null ? "" : defaultValue + "";
            option = createApplicationOptionIfAbsent(key, OptionValueType.INTEGER, dv);
        }
        try {
            return Integer.valueOf(option.getOptionValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Double getDoubleValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.DOUBLE) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.DOUBLE, "0.0");
        }
        try {
            return Double.valueOf(option.getOptionValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Double getDoubleValueByKey(String key, Double defaultValue) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.DOUBLE) {
            String dv = defaultValue == null ? "" : defaultValue + "";
            option = createApplicationOptionIfAbsent(key, OptionValueType.DOUBLE, dv);
        }
        try {
            return Double.valueOf(option.getOptionValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getLongTextValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.LONG_TEXT) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.LONG_TEXT, "");
        }
        return option.getOptionValue();
    }

    public String getLongTextValueByKey(String key, String defaultValue) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.LONG_TEXT) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.LONG_TEXT, defaultValue);
        }
        return option.getOptionValue();
    }

    public String getPharmacyBillCSSWithFallback(String specificKey) {
        String specificCSS = getLongTextValueByKey(specificKey);
        if (specificCSS != null && !specificCSS.trim().isEmpty()) {
            return specificCSS;
        }
        return getLongTextValueByKey("Pharmacy Common Bill CSS");
    }

    public String getPharmacyBillHeaderWithFallback(String specificKey) {
        String specificHeader = getLongTextValueByKey(specificKey);
        if (specificHeader != null && !specificHeader.trim().isEmpty()) {
            return specificHeader;
        }
        return getLongTextValueByKey("Pharmacy Common Bill Header");
    }

    public String getPharmacyTransferBillHeaderWithFallback(String specificKey) {
        String specificHeader = getLongTextValueByKey(specificKey);
        if (specificHeader != null && !specificHeader.trim().isEmpty()) {
            return specificHeader;
        }
        return getLongTextValueByKey("Pharmacy Transfer Request Receipt Header");
    }

    public String getPharmacyBillFooterWithFallback(String specificKey) {
        String specificFooter = getLongTextValueByKey(specificKey);
        if (specificFooter != null && !specificFooter.trim().isEmpty()) {
            return specificFooter;
        }
        return getLongTextValueByKey("Pharmacy Common Bill Footer");
    }

    public void setLongTextValueByKey(String key, String value) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.LONG_TEXT) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.LONG_TEXT, "");
        }
        String sanitized = Jsoup.clean(value, Safelist.basic());
        option.setOptionValue(sanitized);
        optionFacade.edit(option);
        loadApplicationOptions();
    }

    public String getShortTextValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.SHORT_TEXT) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.SHORT_TEXT, "");
        }
        return option.getOptionValue();
    }

    public String getShortTextValueByKey(String key, String defaultValue) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.SHORT_TEXT) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.SHORT_TEXT, defaultValue);
        }
        return option.getOptionValue();
    }

    public String getColorValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.COLOR) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.COLOR, "");
        }
        return option.getOptionValue();
    }

    public String getColorValueByKey(String key, String defaultColorHashCode) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.COLOR) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.COLOR, defaultColorHashCode);
        }
        return option.getOptionValue();
    }

    public String getEnumValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.ENUM) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.ENUM, "");
        }
        return option.getOptionValue();
    }

    public Long getLongValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.LONG) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.LONG, "0");
        }

        try {
            // Attempt to convert the option's value to a Long
            return Long.parseLong(option.getOptionValue());
        } catch (NumberFormatException e) {
// Log or handle the case where the value cannot be parsed into a Long
            return null;
        }
    }

    public Long getLongValueByKey(String key, Long defaultValue) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.LONG) {
            String dv = defaultValue != null ? "" + defaultValue : "0";
            option = createApplicationOptionIfAbsent(key, OptionValueType.LONG, dv);
        }
        try {
            return Long.parseLong(option.getOptionValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<String> getListOfCustomOptions(String optionName) {
        // Fetch the string that contains options separated by line breaks
        String listOfOptionSeperatedByLineBreaks = getLongTextValueByKey("Custom option values for " + optionName);
        // Check if the string is not null or empty before processing
        if (listOfOptionSeperatedByLineBreaks == null || listOfOptionSeperatedByLineBreaks.isEmpty()) {
            return Collections.emptyList(); // Return an empty list if there's nothing to process
        }
        // Split the string by any standard line break sequence and convert to a list
        List<String> listOfCustomOptions = Arrays.stream(listOfOptionSeperatedByLineBreaks.split("\\r?\\n|\\r"))
                .map(String::trim) // Trim leading and trailing whitespaces
                .filter(s -> !s.isEmpty()) // Filter out any empty strings
                .collect(Collectors.toList());
        return listOfCustomOptions;
    }

    public Boolean getBooleanValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.BOOLEAN) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.BOOLEAN, "false");
        }
        return Boolean.parseBoolean(option.getOptionValue());
    }

    public Boolean getBooleanValueByKey(String key, boolean defaultValue) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.BOOLEAN) {
            String dv = defaultValue ? "true" : "false";
            option = createApplicationOptionIfAbsent(key, OptionValueType.BOOLEAN, dv);
        }
        return Boolean.parseBoolean(option.getOptionValue());
    }

    public void setBooleanValueByKey(String key, boolean value) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.BOOLEAN) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.BOOLEAN, Boolean.toString(value));
        }
        option.setOptionValue(Boolean.toString(value));
        optionFacade.edit(option);
        loadApplicationOptions();
    }

    public boolean isPreventPasswordReuse() {
        return getBooleanValueByKey("prevent_password_reuse", false);
    }

    public void setPreventPasswordReuse(boolean value) {
        setBooleanValueByKey("prevent_password_reuse", value);
    }

    public ConfigOption getPreventPasswordReuseOption() {
        return getApplicationOption("prevent_password_reuse");
    }

    public int getPasswordHistoryLimit() {
        return getIntegerValueByKey("password_history_limit", 5);
    }

    public void setIntegerValueByKey(String key, int value) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.INTEGER) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.INTEGER, Integer.toString(value));
        }
        option.setOptionValue(Integer.toString(value));
        optionFacade.edit(option);
        loadApplicationOptions();
    }

    public void setPasswordHistoryLimit(int value) {
        setIntegerValueByKey("password_history_limit", value);
    }

    public ConfigOption getPasswordHistoryLimitOption() {
        return getApplicationOption("password_history_limit");
    }

    public List<ConfigOption> getAllOptions(Object entity) {
        String jpql = "SELECT o FROM ConfigOption o WHERE o.retired = false"; // Assuming there's a 'retired' field.
        Map<String, Object> params = new HashMap<>();

        if (entity == null) {
            // Fetch options that are not associated with any specific department, institution, or user.
            jpql += " AND o.department IS NULL AND o.institution IS NULL AND o.webUser IS NULL";
        } else if (entity instanceof Department) {
            jpql += " AND o.department = :entity";
            params.put("entity", entity);
        } else if (entity instanceof Institution) {
            jpql += " AND o.institution = :entity";
            params.put("entity", entity);
        } else if (entity instanceof WebUser) {
            jpql += " AND o.webUser = :entity";
            params.put("entity", entity);
        } else {
            // This could be adjusted if there are more entity types to consider or removed if all types are accounted for.
            throw new IllegalArgumentException("Unsupported entity type provided.");
        }

        return optionFacade.findByJpql(jpql, params);
    }

    public List<ConfigOption> getApplicationOptions() {
        return getAllOptions(null);
    }

    private <T> T convertOptionValue(ConfigOption option, Class<T> type) {
        String value = option.getOptionValue();
        OptionValueType valueType = option.getValueType();

        try {
            switch (valueType) {
                case LONG_TEXT:
                case SHORT_TEXT:
                    if (String.class.equals(type)) {
                        return type.cast(value);
                    }
                    break; // Could add more specific handling for text types if needed
                case BOOLEAN:
                    if (Boolean.class.equals(type) || boolean.class.equals(type)) {
                        return type.cast(Boolean.valueOf(value));
                    }
                    break;
                case LONG:
                    if (Long.class.equals(type) || long.class.equals(type)) {
                        return type.cast(Long.valueOf(value));
                    }
                    break;
                case DOUBLE:
                    if (Double.class.equals(type) || double.class.equals(type)) {
                        return type.cast(Double.valueOf(value));
                    }
                    break;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Failed to convert value to the requested type.", e);
        }

        throw new IllegalArgumentException("Unsupported type conversion requested: " + type.getSimpleName() + " for value type " + valueType);
    }

    public List<ConfigOption> getOptions() {
        return options;
    }

    public void setOptions(List<ConfigOption> options) {
        this.options = options;
    }

    public void listApplicationOptions() {
        options = getApplicationOptions();
    }

}
