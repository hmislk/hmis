/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.sap;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.dto.sap.SapErrorResponseDTO;
import com.divudi.core.data.dto.sap.SapJournalEntryDTO;
import com.divudi.core.data.dto.sap.SapJournalEntryDTO.SapJournalEntryItemDTO;
import com.divudi.core.data.dto.sap.SapJournalEntryResponseDTO;
import com.divudi.core.entity.BillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.entity.Bill;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 * Pushes a finalized HMIS bill to SAP S/4HANA Cloud Finance as a journal entry.
 *
 * <p>Mapping:
 * <ul>
 *   <li>One debit line (AR account) for the bill total</li>
 *   <li>One credit line per non-retired BillItem (revenue account)</li>
 * </ul>
 *
 * <p>Config keys (all in ConfigOption, managed via admin UI):
 * <ul>
 *   <li>{@code SAP Integration - Company Code}</li>
 *   <li>{@code SAP Integration - AR Account}</li>
 *   <li>{@code SAP Integration - Revenue Account}</li>
 *   <li>{@code SAP Integration - Currency}</li>
 * </ul>
 */
@Stateless
public class SapOutboundService implements Serializable {

    private static final Logger LOG = Logger.getLogger(SapOutboundService.class.getName());
    private static final long serialVersionUID = 1L;

    public static final String KEY_COMPANY_CODE     = "SAP Integration - Company Code";
    public static final String KEY_AR_ACCOUNT       = "SAP Integration - AR Account";
    public static final String KEY_REVENUE_ACCOUNT  = "SAP Integration - Revenue Account";
    public static final String KEY_CURRENCY         = "SAP Integration - Currency";

    private static final String SAP_JOURNAL_PATH =
            "/sap/opu/odata/sap/API_JOURNALENTRYITEMBASIC/JournalEntry";

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    @Inject
    private ConfigOptionApplicationController configController;

    @Inject
    private SapTokenService tokenService;

    @EJB
    private BillFacade billFacade;

    /**
     * Pushes bill {@code billId} to SAP and returns a result map suitable for the REST response.
     *
     * <p>Keys in the returned map:
     * <ul>
     *   <li>{@code sapDocumentNumber} — accounting document number on success</li>
     *   <li>{@code companyCode}</li>
     *   <li>{@code fiscalYear}</li>
     *   <li>{@code billId}</li>
     *   <li>{@code billReference}</li>
     * </ul>
     *
     * @throws SapIntegrationException on token failure, config error, or non-2xx SAP response
     */
    public Map<String, Object> pushBillToSap(Long billId) throws SapIntegrationException {
        if (!tokenService.isEnabled()) {
            throw new SapIntegrationException("SAP integration is disabled or not configured");
        }

        Bill bill = billFacade.find(billId);
        if (bill == null || bill.isRetired()) {
            throw new SapIntegrationException("Bill not found: " + billId);
        }
        if (bill.isCancelled()) {
            throw new SapIntegrationException("Bill " + billId + " is cancelled and cannot be pushed to SAP");
        }

        String companyCode   = requiredConfig(KEY_COMPANY_CODE);
        String arAccount     = requiredConfig(KEY_AR_ACCOUNT);
        String revenueAccount = requiredConfig(KEY_REVENUE_ACCOUNT);
        String currency      = configController.getShortTextValueByKey(KEY_CURRENCY, "LKR");

        SapJournalEntryDTO payload = buildPayload(bill, companyCode, arAccount, revenueAccount, currency);

        String token   = tokenService.getBearerToken();
        String baseUrl = tokenService.getBaseUrl();
        String sapJson = GSON.toJson(payload);

        HttpResponse<String> response = callSapApi(baseUrl, token, sapJson);

        if (response.statusCode() == 401) {
            tokenService.invalidate();
            token = tokenService.getBearerToken();
            response = callSapApi(baseUrl, token, sapJson);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String sapError = extractSapError(response.body());
            throw new SapIntegrationException(
                    "SAP returned HTTP " + response.statusCode() + ": " + sapError);
        }

        SapJournalEntryResponseDTO sapResponse = parseSapResponse(response.body());
        recordPush(billId, sapResponse);

        Map<String, Object> result = new HashMap<>();
        result.put("billId", billId);
        result.put("billReference", bill.getDeptId());
        result.put("sapDocumentNumber", sapResponse.getAccountingDocument());
        result.put("companyCode", sapResponse.getCompanyCode());
        result.put("fiscalYear", sapResponse.getFiscalYear());
        return result;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private SapJournalEntryDTO buildPayload(Bill bill, String companyCode,
            String arAccount, String revenueAccount, String currency) {

        String dateStr = formatDate(bill.getBillDate() != null ? bill.getBillDate() : new Date());

        SapJournalEntryDTO dto = new SapJournalEntryDTO();
        dto.setCompanyCode(companyCode);
        dto.setDocumentDate(dateStr);
        dto.setPostingDate(dateStr);
        dto.setDocumentHeaderText("HMIS Bill " + (bill.getDeptId() != null ? bill.getDeptId() : bill.getId()));
        dto.setReference(bill.getDeptId() != null ? bill.getDeptId() : String.valueOf(bill.getId()));

        List<SapJournalEntryItemDTO> items = new ArrayList<>();

        // Debit: AR receivable for gross total
        items.add(new SapJournalEntryItemDTO(
                arAccount,
                bill.getTotal(),
                currency,
                "S",
                "AR Bill " + bill.getId()));

        // Credit: one line per non-retired bill item
        if (bill.getBillItems() != null) {
            for (BillItem bi : bill.getBillItems()) {
                if (bi == null || bi.isRetired()) continue;
                double amount = bi.getNetValue() > 0 ? bi.getNetValue() : bi.getGrossValue();
                if (amount == 0) continue;
                String itemName = (bi.getItem() != null && bi.getItem().getName() != null)
                        ? bi.getItem().getName() : "Service";
                items.add(new SapJournalEntryItemDTO(
                        revenueAccount,
                        amount,
                        currency,
                        "H",
                        itemName));
            }
        }

        dto.setTo_JournalEntryItem(items);
        return dto;
    }

    private HttpResponse<String> callSapApi(String baseUrl, String token, String jsonBody)
            throws SapIntegrationException {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + SAP_JOURNAL_PATH))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "HTTP call to SAP journal entry endpoint failed", e);
            throw new SapIntegrationException("HTTP call to SAP failed: " + e.getMessage(), e);
        }
    }

    private SapJournalEntryResponseDTO parseSapResponse(String body) throws SapIntegrationException {
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonObject root = reader.readObject();
            // SAP OData v2 wraps single entity as {"d": {...}}
            JsonObject d = root.containsKey("d") ? root.getJsonObject("d") : root;

            SapJournalEntryResponseDTO resp = new SapJournalEntryResponseDTO();
            resp.setAccountingDocument(d.containsKey("AccountingDocument")
                    ? d.getString("AccountingDocument") : null);
            resp.setCompanyCode(d.containsKey("CompanyCode")
                    ? d.getString("CompanyCode") : null);
            resp.setFiscalYear(d.containsKey("FiscalYear")
                    ? d.getString("FiscalYear") : null);
            return resp;
        } catch (Exception e) {
            throw new SapIntegrationException("Could not parse SAP response: " + e.getMessage(), e);
        }
    }

    private String extractSapError(String body) {
        if (body == null || body.isBlank()) return "(empty response)";
        try {
            SapErrorResponseDTO err = GSON.fromJson(body, SapErrorResponseDTO.class);
            return err.extractMessage();
        } catch (Exception e) {
            return body.length() > 300 ? body.substring(0, 300) : body;
        }
    }

    /**
     * Returns the stored SAP push record for {@code billId}, or {@code null} if not yet pushed.
     * The record is a small JSON string stored in ConfigOption under key {@code SAP Bill Push - {billId}}.
     */
    public Map<String, Object> getPushStatus(Long billId) {
        String stored = configController.getShortTextValueByKey("SAP Bill Push - " + billId, "");
        if (stored == null || stored.isBlank()) return null;
        Map<String, Object> result = new HashMap<>();
        result.put("billId", billId);
        result.put("pushRecord", stored);
        return result;
    }

    private void recordPush(Long billId, SapJournalEntryResponseDTO resp) {
        try {
            String value = "{\"sapDocNumber\":\"" + resp.getAccountingDocument()
                    + "\",\"fiscalYear\":\"" + resp.getFiscalYear()
                    + "\",\"sentAt\":\"" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\"}";
            configController.saveShortTextOption("SAP Bill Push - " + billId, value);
        } catch (Exception e) {
            // Non-fatal: log but do not fail the push
            LOG.log(Level.WARNING, "Could not record SAP push result for bill " + billId, e);
        }
    }

    private String requiredConfig(String key) throws SapIntegrationException {
        String value = configController.getShortTextValueByKey(key, "");
        if (value == null || value.trim().isEmpty()) {
            throw new SapIntegrationException("SAP integration config key is not set: " + key);
        }
        return value.trim();
    }

    private static String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }
}
