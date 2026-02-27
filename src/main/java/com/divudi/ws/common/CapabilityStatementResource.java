/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.common;

import com.divudi.bean.common.ConfigOptionApplicationController;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * FHIR-inspired capability statement endpoint.
 *
 * <p>Returns a machine-readable summary of all available HMIS REST API modules,
 * their operations, and links to their full documentation on GitHub.
 * No authentication required — this is a public discovery endpoint.</p>
 *
 * <p>Example: GET {baseUrl}/api/capabilities</p>
 *
 * @author Dr M H B Ariyaratne
 */
@Path("capabilities")
@RequestScoped
public class CapabilityStatementResource {

    private static final String GITHUB_RAW_BASE = "https://raw.githubusercontent.com/hmislk/hmis/";

    @Context
    private UriInfo uriInfo;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    /**
     * Returns the HMIS API capability statement: all modules, their operations,
     * and links to detailed documentation.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCapabilities() {
        String branch = configOptionApplicationController.getShortTextValueByKey(
                "AI Chat - GitHub Branch", "development");
        String baseUrl = resolveBaseUrl();

        JsonObjectBuilder root = Json.createObjectBuilder()
                .add("resourceType", "HmisCapabilityStatement")
                .add("name", "HMIS REST API")
                .add("description",
                        "Hospital Management Information System REST API capability statement. "
                        + "Lists all available modules, their endpoints, and documentation links.")
                .add("baseUrl", baseUrl)
                .add("overviewDocumentation", githubUrl(branch, "developer_docs/API_Documentation_For_AI_Agents.md"))
                .add("testingGuide", githubUrl(branch, "developer_docs/API_Testing_Guide_For_AI_Agents.md"))
                .add("authentication", Json.createObjectBuilder()
                        .add("type", "ApiKey")
                        .add("header", "Finance")
                        .add("note", "Pass your active HMIS API key in the 'Finance' HTTP header for all authenticated endpoints.")
                        .build())
                .add("modules", buildModules(branch));

        return Response.ok(root.build().toString())
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .header("Access-Control-Allow-Origin", "*")
                .build();
    }

    private javax.json.JsonArray buildModules(String branch) {
        String pharmDoc      = githubUrl(branch, "developer_docs/API_PHARMACEUTICAL_MANAGEMENT.md");
        String institutionDoc = githubUrl(branch, "developer_docs/API_INSTITUTION_DEPARTMENT_MANAGEMENT.md");
        String balanceDoc    = githubUrl(branch, "developer_docs/API_BALANCE_HISTORY.md");
        String billCorrDoc   = githubUrl(branch, "developer_docs/API_BILL_DATA_CORRECTION.md");
        String costingDoc    = githubUrl(branch, "developer_docs/API_COSTING_DATA.md");
        String f15Doc        = githubUrl(branch, "developer_docs/API_F15_REPORT.md");
        String stockHistDoc  = githubUrl(branch, "developer_docs/API_STOCK_HISTORY.md");

        return Json.createArrayBuilder()
                .add(module("Pharmacy - Stock Adjustments", "/pharmacy_adjustments",
                        "Adjust pharmacy stock quantities, purchase rates, retail sale rates, and expiry dates.",
                        true, pharmDoc,
                        op("POST", "/pharmacy_adjustments/stock_quantity",   "Adjust quantity of a stock batch in a pharmacy department"),
                        op("POST", "/pharmacy_adjustments/retail_rate",      "Adjust retail sale rate of a stock batch"),
                        op("POST", "/pharmacy_adjustments/purchase_rate",    "Adjust purchase rate of a stock batch"),
                        op("POST", "/pharmacy_adjustments/expiry_date",      "Adjust expiry date of a stock batch")
                ))
                .add(module("Pharmacy - Search", "/pharmacy_adjustments/search",
                        "Search pharmacy stocks, departments, and pharmaceutical items.",
                        true, pharmDoc,
                        op("GET", "/pharmacy_adjustments/search/stocks",      "Search pharmacy stocks with filters (department, item, quantity, expiry, batch)"),
                        op("GET", "/pharmacy_adjustments/search/departments", "Search pharmacy departments by name"),
                        op("GET", "/pharmacy_adjustments/search/items",       "Search pharmaceutical items by name or code")
                ))
                .add(module("Pharmacy - Batches", "/pharmacy_batches",
                        "Create and search Active Moiety Products (AMPs) and pharmacy stock batches.",
                        true, pharmDoc,
                        op("POST", "/pharmacy_batches/amp/search_or_create", "Search for an AMP by name or create a new one if not found"),
                        op("POST", "/pharmacy_batches/create",               "Create a new pharmacy stock batch"),
                        op("GET",  "/pharmacy_batches/amp/search",           "Search AMPs by name")
                ))
                .add(module("Pharmacy - F15 Report", "/pharmacy_f15_report",
                        "Generate and retrieve pharmacy F15 reports.",
                        true, f15Doc,
                        op("GET", "/pharmacy_f15_report", "Retrieve F15 report data with date range and department filters")
                ))
                .add(module("Pharmacy - Stock History", "/stock_history",
                        "Retrieve pharmacy stock movement and history records.",
                        true, stockHistDoc,
                        op("GET", "/stock_history", "Get stock history records with date range, item, and department filters")
                ))
                .add(module("Institution Management", "/institutions",
                        "Manage hospitals, clinics, and other healthcare institutions.",
                        true, institutionDoc,
                        op("GET",    "/institutions/search", "Search institutions by name"),
                        op("GET",    "/institutions/{id}",   "Get institution by ID"),
                        op("POST",   "/institutions",         "Create a new institution"),
                        op("PUT",    "/institutions/{id}",   "Update an institution"),
                        op("DELETE", "/institutions/{id}",   "Retire (soft-delete) an institution")
                ))
                .add(module("Department Management", "/departments",
                        "Manage departments within institutions (wards, pharmacy, outpatient, etc.).",
                        true, institutionDoc,
                        op("GET",    "/departments/search", "Search departments by name or institution"),
                        op("GET",    "/departments/{id}",   "Get department by ID"),
                        op("POST",   "/departments",         "Create a new department"),
                        op("PUT",    "/departments/{id}",   "Update a department"),
                        op("DELETE", "/departments/{id}",   "Retire a department")
                ))
                .add(module("Finance - Balance History", "/balance_history",
                        "Retrieve financial balance history: drawer entries, patient deposits, agent histories, staff welfare.",
                        true, balanceDoc,
                        op("GET", "/balance_history/drawer_entries",        "Get cash drawer entries for a date range"),
                        op("GET", "/balance_history/patient_deposits",       "Get patient deposit records"),
                        op("GET", "/balance_history/agent_histories",        "Get agent financial history records"),
                        op("GET", "/balance_history/staff_welfare_histories","Get staff welfare financial history")
                ))
                .add(module("Finance - Bill Data Correction", "/bill_data_correction",
                        "Apply corrections and adjustments to financial bill records.",
                        true, billCorrDoc,
                        op("POST", "/bill_data_correction", "Apply corrections to bill data")
                ))
                .add(module("Finance - Costing Data", "/costing_data",
                        "Retrieve billing and costing data for financial analysis and reporting.",
                        true, costingDoc,
                        op("GET", "/costing_data/last_bill",                    "Get the most recent bill"),
                        op("GET", "/costing_data/bill",                          "Get bills for a date range"),
                        op("GET", "/costing_data/by_bill_number/{bill_number}", "Get a specific bill by bill number"),
                        op("GET", "/costing_data/by_bill_id/{bill_id}",         "Get a specific bill by internal ID")
                ))
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String resolveBaseUrl() {
        String base = uriInfo.getBaseUri().toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private String githubUrl(String branch, String filePath) {
        return GITHUB_RAW_BASE + branch + "/" + filePath;
    }

    private JsonObject module(String name, String basePath, String description,
            boolean authRequired, String documentationUrl, JsonObject... operations) {
        JsonArrayBuilder opsBuilder = Json.createArrayBuilder();
        for (JsonObject operation : operations) {
            opsBuilder.add(operation);
        }
        return Json.createObjectBuilder()
                .add("name", name)
                .add("basePath", basePath)
                .add("description", description)
                .add("authRequired", authRequired)
                .add("documentationUrl", documentationUrl)
                .add("operations", opsBuilder.build())
                .build();
    }

    private JsonObject op(String method, String path, String description) {
        return Json.createObjectBuilder()
                .add("method", method)
                .add("path", path)
                .add("description", description)
                .build();
    }
}
