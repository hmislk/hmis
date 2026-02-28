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
                .add(resource("Clinical", "/api/clinical",
                        "Clinical APIs including favourites and medicine entities",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Membership", "/api/membership",
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
                        "GET", "POST",
                        subResources(
                                subResource("Balance History", "/api/finance/balance-history", "GET"),
                                subResource("Bill Correction", "/api/finance/bill-correction", "POST"),
                                subResource("Costing", "/api/finance/costing", "GET"),
                                subResource("QuickBooks", "/api/finance/qb", "GET")
                        )))
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
                .add(resource("Inward", "/api/inward",
                        "Inward patient workflows",
                        "API Key",
                        "GET", "POST"))
                .add(resource("LIMS", "/api/lims",
                        "Laboratory Information Management System integrations",
                        "API Key",
                        "GET", "POST",
                        subResources(
                                subResource("Middleware", "/api/lims/middleware", "GET", "POST")
                        )))
                .add(resource("Middleware", "/api/middleware",
                        "General middleware endpoints",
                        "API Key",
                        "GET", "POST"))
                .add(resource("Pharmaceutical Config", "/api/pharmaceutical/config",
                        "Pharmaceutical configuration management",
                        "API Key",
                        "GET", "POST", "PUT", "PATCH"))
                .add(resource("Pharmaceutical Items", "/api/pharmaceutical_items",
                        "Pharmaceutical item master data",
                        "API Key",
                        "GET", "POST", "PUT", "DELETE"))
                .add(resource("Pharmacy", "/api/pharmacy_adjustments",
                        "Pharmacy stock and adjustment operations",
                        "API Key",
                        "GET", "POST",
                        subResources(
                                subResource("Batches", "/api/pharmacy_batches", "GET", "POST"),
                                subResource("F15 Report", "/api/pharmacy/f15", "GET")
                        )))
                .add(resource("Users", "/api/users",
                        "User management operations",
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

    private JsonObject resource(String name, String path, String description, String authentication,
            String[] operations, javax.json.JsonArray subResources) {
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
                .add("subResources", subResources)
                .build();
    }

    private JsonObject resource(String name, String path, String description, String authentication,
            String operationOne, String operationTwo, javax.json.JsonArray subResources) {
        return resource(name, path, description, authentication,
                new String[]{operationOne, operationTwo}, subResources);
    }

    private javax.json.JsonArray subResources(JsonObject... subResources) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (JsonObject subResource : subResources) {
            builder.add(subResource);
        }
        return builder.build();
    }

    private JsonObject subResource(String name, String path, String... operations) {
        JsonArrayBuilder operationArray = Json.createArrayBuilder();
        for (String operation : operations) {
            operationArray.add(operation);
        }
        return Json.createObjectBuilder()
                .add("name", name)
                .add("path", path)
                .add("operations", operationArray)
                .build();
    }
}
