/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.common;

import javax.enterprise.context.RequestScoped;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * HMIS API capability statement endpoint.
 *
 * Returns a machine-readable summary of available API resources,
 * their endpoints, operations, and authentication requirements.
 */
@Path("capabilities")
@RequestScoped
public class CapabilityStatementResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCapabilities() {
        JsonObject response = Json.createObjectBuilder()
                .add("status", "success")
                .add("data", Json.createObjectBuilder()
                        .add("name", "HMIS REST API")
                        .add("version", "1.0")
                        .add("description", "Hospital Management Information System API")
                        .add("authentication", "API Key header 'Finance' for protected endpoints")
                        .add("contact", "HMIS Support Team")
                        .add("termsOfUse", "Use according to institutional HMIS API access policies")
                        .add("resources", buildResources())
                        .build())
                .build();

        return Response.ok(response.toString(), MediaType.APPLICATION_JSON)
                .header("Access-Control-Allow-Origin", "*")
                .build();
    }

    private javax.json.JsonArray buildResources() {
        return Json.createArrayBuilder()
                .add(resource("Capabilities", "/api/capabilities",
                        "API discovery endpoint that describes available resources",
                        "None",
                        "GET"))
                .add(resource("Channel", "/api/channel",
                        "Channeling and appointment operations",
                        "API Key",
                        "GET", "POST"))
                .add(resource("Clinical Favourite Medicines", "/api/clinical/favourite_medicines",
                        "Clinical favourite medicine management",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Membership", "/api/apiMembership",
                        "Membership-related operations",
                        "API Key",
                        "GET", "POST"))
                .add(resource("Config", "/api/config",
                        "Application configuration options",
                        "API Key",
                        "GET", "POST", "PUT", "PATCH"))
                .add(resource("FHIR", "/api/fhir",
                        "FHIR and interoperability resources",
                        "API Key",
                        "GET", "POST"))
                .add(resource("Finance", "/api/finance",
                        "Finance operations and billing endpoints",
                        "API Key",
                        "GET", "POST"))
                .add(resource("Balance History", "/api/balance_history",
                        "Financial balance history",
                        "API Key",
                        "GET"))
                .add(resource("Bill Data Correction", "/api/bill_data_correction",
                        "Bill data correction operations",
                        "API Key",
                        "POST"))
                .add(resource("Costing Data", "/api/costing_data",
                        "Cost accounting data",
                        "API Key",
                        "GET"))
                .add(resource("QuickBooks", "/api/qb",
                        "QuickBooks integration",
                        "API Key",
                        "GET"))
                .add(resource("Departments", "/api/departments",
                        "Department management",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Institutions", "/api/institutions",
                        "Institution management",
                        "API Key",
                        "GET", "POST", "PUT", "PATCH", "DELETE"))
                .add(resource("Sites", "/api/sites",
                        "Site management",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Inward", "/api/apiInward",
                        "Inward patient workflows",
                        "API Key",
                        "GET", "POST"))
                .add(resource("LIMS", "/api/lims",
                        "Laboratory Information Management System integrations",
                        "API Key",
                        "GET", "POST"))
                .add(resource("LIMS Middleware", "/api/limsmw",
                        "LIMS middleware integration",
                        "API Key",
                        "GET", "POST"))
                .add(resource("Middleware", "/api/middleware",
                        "General middleware endpoints",
                        "API Key",
                        "GET", "POST"))
                .add(resource("Pharmaceutical Config", "/api/pharmaceutical_config",
                        "Pharmaceutical configuration management",
                        "API Key",
                        "GET", "POST", "PUT", "PATCH"))
                .add(resource("Pharmaceutical Items", "/api/pharmaceutical_items",
                        "Pharmaceutical item master data",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Pharmacy Adjustments", "/api/pharmacy_adjustments",
                        "Pharmacy stock and adjustment operations",
                        "API Key",
                        "GET", "POST"))
                .add(resource("Pharmacy Search", "/api/pharmacy_adjustments/search",
                        "Pharmacy stock search",
                        "API Key",
                        "GET"))
                .add(resource("Pharmacy Batches", "/api/pharmacy_batches",
                        "Pharmacy batch management",
                        "API Key",
                        "GET", "POST"))
                .add(resource("Pharmacy F15 Report", "/api/pharmacy_f15_report",
                        "Pharmacy F15 reporting",
                        "API Key",
                        "GET"))
                .add(resource("Stock History", "/api/stock_history",
                        "Stock movement history",
                        "API Key",
                        "GET"))
                .add(resource("Users", "/api/users",
                        "User management operations",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("User Roles", "/api/user-roles",
                        "User role management operations",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .build();
    }

    private JsonObject resource(String name, String path, String description, String authentication,
            String... operations) {
        JsonArrayBuilder operationArray = Json.createArrayBuilder();
        for (String operation : operations) {
            operationArray.add(operation);
        }
        return Json.createObjectBuilder()
                .add("name", name)
                .add("path", path)
                .add("description", description)
                .add("operations", operationArray)
                .add("authentication", authentication)
                .build();
    }
}
