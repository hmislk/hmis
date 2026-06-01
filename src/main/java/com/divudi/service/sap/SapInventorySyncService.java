/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.sap;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.dto.sap.SapInventorySyncResultDTO;
import com.divudi.core.data.dto.sap.SapMaterialDocumentDTO;
import com.divudi.core.entity.Item;
import com.divudi.core.facade.ItemFacade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Serializable;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * Fetches SAP MM goods-receipt material document items and matches them to
 * HMIS pharmacy items by code, logging the sync results.
 *
 * <p>This service performs a read-only audit sync — it records what SAP has received
 * against HMIS item master data. Actual stock updates (GRN creation) require
 * additional pricing/department context and are handled by pharmacy workflows.
 *
 * <p>Config keys:
 * <ul>
 *   <li>{@code SAP Integration - Material Code Field} — Item field to match against SAP material number (default: {@code code})</li>
 *   <li>{@code SAP Integration - Inventory Last Sync} — ISO timestamp of last successful sync</li>
 *   <li>{@code SAP Integration - Inventory Sync From Days} — fallback look-back days when no prior sync exists (default: 7)</li>
 * </ul>
 */
@Stateless
public class SapInventorySyncService implements Serializable {

    private static final Logger LOG = Logger.getLogger(SapInventorySyncService.class.getName());
    private static final long serialVersionUID = 1L;

    private static final String SAP_MATERIAL_DOC_PATH =
            "/sap/opu/odata/sap/API_MATERIAL_DOCUMENT_SRV/A_MaterialDocItem";
    private static final String SELECT_FIELDS =
            "MaterialDocument,MaterialDocumentItem,Material,Plant,StorageLocation,Quantity,BaseUnit,PostingDate,MaterialDocumentYear";

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    @Inject
    private ConfigOptionApplicationController configController;

    @Inject
    private SapTokenService tokenService;

    @EJB
    private ItemFacade itemFacade;

    /**
     * Runs the inventory sync for the given date range.
     * If {@code fromDate} is null, uses the stored last-sync timestamp or falls back
     * to {@code SAP Integration - Inventory Sync From Days} days ago.
     *
     * @param fromDate optional override start date (yyyy-MM-dd); null = use stored timestamp
     * @param toDate   optional override end date (yyyy-MM-dd); null = today
     * @return sync result summary
     * @throws SapIntegrationException on token failure or SAP API error
     */
    public SapInventorySyncResultDTO sync(String fromDate, String toDate) throws SapIntegrationException {
        if (!tokenService.isEnabled()) {
            SapInventorySyncResultDTO skipped = new SapInventorySyncResultDTO();
            skipped.addWarning("SAP integration is disabled");
            return skipped;
        }

        String resolvedFrom = resolveFromDate(fromDate);
        String resolvedTo   = toDate != null && !toDate.isBlank() ? toDate : todayString();

        String token   = tokenService.getBearerToken();
        String baseUrl = tokenService.getBaseUrl();

        List<SapMaterialDocumentDTO> docs = fetchMaterialDocItems(baseUrl, token, resolvedFrom, resolvedTo);

        if (docs == null) {
            throw new SapIntegrationException("Failed to fetch material documents from SAP");
        }

        String materialCodeField = configController.getShortTextValueByKey(
                "SAP Integration - Material Code Field", "code");

        SapInventorySyncResultDTO result = new SapInventorySyncResultDTO();
        result.setTotalDocumentItems(docs.size());
        result.setSyncFromDate(resolvedFrom);
        result.setSyncToDate(resolvedTo);

        int matched = 0;
        int unmatched = 0;

        for (SapMaterialDocumentDTO doc : docs) {
            String materialNumber = doc.getMaterial();
            if (materialNumber == null || materialNumber.isBlank()) {
                result.addWarning("Document " + doc.getMaterialDocument() + " item "
                        + doc.getMaterialDocumentItem() + " has no material number — skipped");
                unmatched++;
                continue;
            }

            Item item = findItemByField(materialCodeField, materialNumber);
            if (item == null) {
                result.addUnmatched(materialNumber);
                LOG.log(Level.WARNING, "SAP material {0} not found in HMIS Item master (field: {1})",
                        new Object[]{materialNumber, materialCodeField});
                unmatched++;
            } else {
                matched++;
                LOG.log(Level.FINE, "SAP material {0} matched to HMIS item {1} (id={2})",
                        new Object[]{materialNumber, item.getName(), item.getId()});
            }
        }

        result.setMatchedItems(matched);
        result.setUnmatchedItems(unmatched);

        // Record sync timestamp
        String syncTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        result.setLastSyncTimestamp(syncTimestamp);
        configController.saveShortTextOption("SAP Integration - Inventory Last Sync", syncTimestamp);

        return result;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private List<SapMaterialDocumentDTO> fetchMaterialDocItems(String baseUrl, String token,
            String fromDate, String toDate) throws SapIntegrationException {
        try {
            // SAP OData datetime filter format: PostingDate ge datetime'2024-01-01T00:00:00'
            String filter = "PostingDate ge datetime'" + fromDate + "T00:00:00'"
                    + " and PostingDate le datetime'" + toDate + "T23:59:59'";
            String url = baseUrl + SAP_MATERIAL_DOC_PATH
                    + "?$filter=" + URLEncoder.encode(filter, StandardCharsets.UTF_8)
                    + "&$select=" + URLEncoder.encode(SELECT_FIELDS, StandardCharsets.UTF_8)
                    + "&$format=json";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                tokenService.invalidate();
                token = tokenService.getBearerToken();
                request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json")
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .timeout(Duration.ofSeconds(60))
                        .build();
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new SapIntegrationException(
                        "SAP material document API returned HTTP " + response.statusCode()
                        + ": " + response.body());
            }

            return parseMaterialDocItems(response.body());

        } catch (SapIntegrationException e) {
            throw e;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to fetch SAP material documents", e);
            throw new SapIntegrationException("Failed to fetch SAP material documents: " + e.getMessage(), e);
        }
    }

    private List<SapMaterialDocumentDTO> parseMaterialDocItems(String body) throws SapIntegrationException {
        try {
            // SAP OData v2 JSON: {"d":{"results":[...]}}
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray results;
            if (root.has("d") && root.getAsJsonObject("d").has("results")) {
                results = root.getAsJsonObject("d").getAsJsonArray("results");
            } else if (root.has("value")) {
                results = root.getAsJsonArray("value");
            } else {
                throw new SapIntegrationException("Unexpected SAP response structure: " + body.substring(0, Math.min(300, body.length())));
            }

            List<SapMaterialDocumentDTO> items = new ArrayList<>();
            for (JsonElement el : results) {
                items.add(GSON.fromJson(el, SapMaterialDocumentDTO.class));
            }
            return items;
        } catch (SapIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new SapIntegrationException("Could not parse SAP material document response: " + e.getMessage(), e);
        }
    }

    private Item findItemByField(String fieldName, String value) {
        String jpql;
        switch (fieldName) {
            case "barcode":
                jpql = "SELECT i FROM Item i WHERE i.barcode = :val AND i.retired = false";
                break;
            case "code":
            default:
                jpql = "SELECT i FROM Item i WHERE i.code = :val AND i.retired = false";
                break;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("val", value);
        return itemFacade.findFirstByJpql(jpql, params);
    }

    private String resolveFromDate(String fromDate) {
        if (fromDate != null && !fromDate.isBlank()) {
            return fromDate;
        }
        String stored = configController.getShortTextValueByKey(
                "SAP Integration - Inventory Last Sync", "");
        if (stored != null && !stored.isBlank()) {
            // stored is "yyyy-MM-dd HH:mm:ss" — extract date part
            return stored.substring(0, 10);
        }
        // Fall back to N days ago
        int days = 7;
        try {
            String daysStr = configController.getShortTextValueByKey(
                    "SAP Integration - Inventory Sync From Days", "7");
            if (daysStr != null && !daysStr.isBlank()) {
                days = Integer.parseInt(daysStr.trim());
            }
        } catch (NumberFormatException e) {
            LOG.warning("SAP Integration - Inventory Sync From Days is not a valid integer, using 7");
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        return new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
    }

    private static String todayString() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }
}
