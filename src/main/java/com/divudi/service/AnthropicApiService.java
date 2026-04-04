package com.divudi.service;

import com.divudi.core.entity.AiMessage;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;

@Stateless
public class AnthropicApiService implements Serializable {

    private static final Logger LOG = Logger.getLogger(AnthropicApiService.class.getName());
    private static final long serialVersionUID = 1L;

    private static final String GITHUB_RAW_BASE = "https://raw.githubusercontent.com/hmislk/hmis/";

    // No pre-fetched doc list. The capability summary is built inline in buildSystemPrompt()
    // and documentationUrl links are included so Claude can request specific docs on demand.

    /**
     * Sends a conversation to Claude API and returns the response text.
     *
     * @param apiKey The Anthropic API key (from ConfigOption "AI Chat - Claude API Key")
     * @param model The Claude model ID e.g. "claude-opus-4-6" (from ConfigOption "AI Chat - Claude Model")
     * @param maxTokens Maximum tokens for response (from ConfigOption "AI Chat - Max Tokens")
     * @param systemPrompt The system prompt to set context (HMIS URL, user API key, doc URLs etc.)
     * @param conversationHistory List of AiMessage entities representing the conversation so far (NOT including the new user message)
     * @param userMessage The new user message text
     * @param attachmentBase64 Optional base64 encoded image/doc (null if no attachment)
     * @param attachmentMimeType Optional MIME type e.g. "image/png" (null if no attachment)
     * @return AnthropicResponse object with content, inputTokens, outputTokens
     */
    public AnthropicResponse sendMessage(
            String apiKey,
            String model,
            int maxTokens,
            String systemPrompt,
            List<AiMessage> conversationHistory,
            String userMessage,
            String attachmentBase64,
            String attachmentMimeType) {

        try {
            // Build the messages array
            JsonArrayBuilder messagesBuilder = Json.createArrayBuilder();

            // Add conversation history
            for (AiMessage msg : conversationHistory) {
                if (msg.getContent() == null || msg.getContent().trim().isEmpty()) {
                    continue;
                }
                JsonObject historyMsg = Json.createObjectBuilder()
                        .add("role", msg.getRole())
                        .add("content", msg.getContent())
                        .build();
                messagesBuilder.add(historyMsg);
            }

            // Build the new user message - handle attachments
            if (attachmentBase64 != null && !attachmentBase64.isEmpty() && attachmentMimeType != null) {
                // Multi-part content with attachment
                JsonArrayBuilder contentBuilder = Json.createArrayBuilder();

                // Check if it's an image (vision) or document
                if (attachmentMimeType.startsWith("image/")) {
                    // Image - use image block
                    JsonObject imageSource = Json.createObjectBuilder()
                            .add("type", "base64")
                            .add("media_type", attachmentMimeType)
                            .add("data", attachmentBase64)
                            .build();
                    JsonObject imageBlock = Json.createObjectBuilder()
                            .add("type", "image")
                            .add("source", imageSource)
                            .build();
                    contentBuilder.add(imageBlock);
                } else {
                    // Document - use document block (Claude supports PDF, text etc)
                    JsonObject docSource = Json.createObjectBuilder()
                            .add("type", "base64")
                            .add("media_type", attachmentMimeType)
                            .add("data", attachmentBase64)
                            .build();
                    JsonObject docBlock = Json.createObjectBuilder()
                            .add("type", "document")
                            .add("source", docSource)
                            .build();
                    contentBuilder.add(docBlock);
                }

                // Add text part
                if (userMessage != null && !userMessage.trim().isEmpty()) {
                    JsonObject textBlock = Json.createObjectBuilder()
                            .add("type", "text")
                            .add("text", userMessage)
                            .build();
                    contentBuilder.add(textBlock);
                }

                JsonObject userMsg = Json.createObjectBuilder()
                        .add("role", "user")
                        .add("content", contentBuilder.build())
                        .build();
                messagesBuilder.add(userMsg);
            } else {
                // Simple text message
                JsonObject userMsg = Json.createObjectBuilder()
                        .add("role", "user")
                        .add("content", userMessage != null ? userMessage : "")
                        .build();
                messagesBuilder.add(userMsg);
            }

            // Build the full request body
            JsonObject requestBody = Json.createObjectBuilder()
                    .add("model", model != null ? model : "claude-opus-4-6")
                    .add("max_tokens", maxTokens > 0 ? maxTokens : 4096)
                    .add("system", systemPrompt != null ? systemPrompt : "")
                    .add("messages", messagesBuilder.build())
                    .build();

            String requestBodyString = requestBody.toString();

            // Build the HTTP request
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.anthropic.com/v1/messages"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String responseBody = response.body();

            if (response.statusCode() != 200) {
                LOG.log(Level.WARNING, "Anthropic API error {0}: {1}",
                        new Object[]{response.statusCode(), responseBody});
                return new AnthropicResponse("Error from AI service (HTTP " + response.statusCode() + "): " + responseBody, 0L, 0L);
            }

            // Parse the response
            try (JsonReader reader = Json.createReader(new StringReader(responseBody))) {
                JsonObject responseJson = reader.readObject();

                // Extract content text
                String content = "";
                JsonArray contentArray = responseJson.getJsonArray("content");
                if (contentArray != null && !contentArray.isEmpty()) {
                    JsonObject firstContent = contentArray.getJsonObject(0);
                    content = firstContent.getString("text", "");
                }

                // Extract token usage
                long inputTokens = 0L;
                long outputTokens = 0L;
                JsonObject usage = responseJson.getJsonObject("usage");
                if (usage != null) {
                    inputTokens = usage.getInt("input_tokens", 0);
                    outputTokens = usage.getInt("output_tokens", 0);
                }

                return new AnthropicResponse(content, inputTokens, outputTokens);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.log(Level.SEVERE, "Anthropic API call interrupted", e);
            return new AnthropicResponse("Request was interrupted. Please try again.", 0L, 0L);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error calling Anthropic API", e);
            return new AnthropicResponse("Error communicating with AI service: " + e.getMessage(), 0L, 0L);
        }
    }

    /**
     * Builds the system prompt with HMIS context and an inline capability statement.
     *
     * <p>Instead of fetching all documentation files upfront (which is slow and causes
     * request timeouts), this method embeds a compact capability summary with documentation
     * URLs. Claude can request the full content of any specific module's documentation
     * during the conversation.</p>
     *
     * @param hmisApiBaseUrl  The HMIS REST API base URL (auto-detected from request)
     * @param userHmisApiKey  The logged-in user's active HMIS API key value
     * @param githubBranch    The GitHub branch for documentation links (e.g. "development")
     */
    public String buildSystemPrompt(String hmisApiBaseUrl, String userHmisApiKey, String githubBranch) {
        String branch = (githubBranch != null && !githubBranch.trim().isEmpty())
                ? githubBranch.trim() : "development";

        StringBuilder sb = new StringBuilder();
        sb.append("You are an AI assistant integrated into the HMIS (Hospital Management Information System). ");
        sb.append("You help system administrators and users interact with and manage the hospital system.\n\n");

        if (hmisApiBaseUrl != null && !hmisApiBaseUrl.trim().isEmpty()) {
            sb.append("## HMIS REST API\n");
            sb.append("Base URL: ").append(hmisApiBaseUrl.trim()).append("\n");
            sb.append("Capability Statement: ").append(hmisApiBaseUrl.trim()).append("/api/capabilities\n");
            sb.append("All API paths below are relative to this base URL.\n\n");
        }

        if (userHmisApiKey != null && !userHmisApiKey.trim().isEmpty()) {
            sb.append("## Authentication\n");
            sb.append("API key: ").append(userHmisApiKey.trim()).append("\n");
            sb.append("Most endpoints use the 'Finance' header. Some modules use different headers:\n");
            sb.append("- 'Finance' header: Pharmacy, Institution, Department, Finance, Users, Login History, Sites, Inward, and most other modules\n");
            sb.append("- 'Token' header: Consultant Management (/channel/consultant)\n");
            sb.append("- 'Config' header: System Configuration (/config)\n");
            sb.append("Each module description notes its required header when it differs from 'Finance'.\n\n");
        }

        sb.append("## Available API Modules\n");
        sb.append("Each module lists its operations and a documentationUrl for full parameter details.\n");
        sb.append("If you need the complete documentation for a module, say so and it will be provided.\n\n");

        appendModule(sb, "Pharmacy - Stock Adjustments", "/pharmacy_adjustments",
                "Adjust pharmacy stock quantities, purchase rates, retail sale rates, and expiry dates.",
                githubUrl(branch, "developer_docs/API_PHARMACEUTICAL_MANAGEMENT.md"),
                new String[][]{
                    {"POST", "/pharmacy_adjustments/stock_quantity", "Adjust quantity of a stock batch"},
                    {"POST", "/pharmacy_adjustments/retail_rate",    "Adjust retail sale rate of a stock batch"},
                    {"POST", "/pharmacy_adjustments/purchase_rate",  "Adjust purchase rate of a stock batch"},
                    {"POST", "/pharmacy_adjustments/expiry_date",    "Adjust expiry date of a stock batch"}
                });

        appendModule(sb, "Pharmacy - Search", "/pharmacy_adjustments/search",
                "Search pharmacy stocks, departments, and pharmaceutical items.",
                githubUrl(branch, "developer_docs/API_PHARMACEUTICAL_MANAGEMENT.md"),
                new String[][]{
                    {"GET", "/pharmacy_adjustments/search/stocks",      "Search stocks with filters (department, item, quantity, expiry, batch)"},
                    {"GET", "/pharmacy_adjustments/search/departments",  "Search pharmacy departments by name"},
                    {"GET", "/pharmacy_adjustments/search/items",        "Search pharmaceutical items by name or code"}
                });

        appendModule(sb, "Pharmacy - Batches", "/pharmacy_batches",
                "Create and search Active Moiety Products (AMPs) and pharmacy stock batches.",
                githubUrl(branch, "developer_docs/API_PHARMACEUTICAL_MANAGEMENT.md"),
                new String[][]{
                    {"POST", "/pharmacy_batches/amp/search_or_create", "Search for an AMP by name or create one if not found"},
                    {"POST", "/pharmacy_batches/create",               "Create a new pharmacy stock batch"},
                    {"GET",  "/pharmacy_batches/amp/search",           "Search AMPs by name"}
                });

        appendModule(sb, "Pharmacy - F15 Report", "/pharmacy_f15_report",
                "Generate and retrieve pharmacy F15 reports.",
                githubUrl(branch, "developer_docs/API_F15_REPORT.md"),
                new String[][]{
                    {"GET", "/pharmacy_f15_report", "Retrieve F15 report data with date range and department filters"}
                });

        appendModule(sb, "Pharmacy - Stock History", "/stock_history",
                "Retrieve pharmacy stock movement and history records.",
                githubUrl(branch, "developer_docs/API_STOCK_HISTORY.md"),
                new String[][]{
                    {"GET", "/stock_history", "Get stock history with date range, item, and department filters"}
                });

        appendModule(sb, "Institution Management", "/institutions",
                "Manage hospitals, clinics, and other healthcare institutions.",
                githubUrl(branch, "developer_docs/API_INSTITUTION_DEPARTMENT_MANAGEMENT.md"),
                new String[][]{
                    {"GET",    "/institutions/search", "Search institutions by name"},
                    {"GET",    "/institutions/{id}",   "Get institution by ID"},
                    {"POST",   "/institutions",         "Create a new institution"},
                    {"PUT",    "/institutions/{id}",   "Update an institution"},
                    {"DELETE", "/institutions/{id}",   "Retire (soft-delete) an institution"}
                });

        appendModule(sb, "Department Management", "/departments",
                "Manage departments within institutions (wards, pharmacy, outpatient, etc.).",
                githubUrl(branch, "developer_docs/API_INSTITUTION_DEPARTMENT_MANAGEMENT.md"),
                new String[][]{
                    {"GET",    "/departments/search", "Search departments by name or institution"},
                    {"GET",    "/departments/{id}",   "Get department by ID"},
                    {"POST",   "/departments",         "Create a new department"},
                    {"PUT",    "/departments/{id}",   "Update a department"},
                    {"DELETE", "/departments/{id}",   "Retire a department"}
                });

        appendModule(sb, "Finance - Balance History", "/balance_history",
                "Retrieve financial balance history: drawer entries, patient deposits, agent histories, staff welfare.",
                githubUrl(branch, "developer_docs/API_BALANCE_HISTORY.md"),
                new String[][]{
                    {"GET", "/balance_history/drawer_entries",         "Get cash drawer entries for a date range"},
                    {"GET", "/balance_history/patient_deposits",        "Get patient deposit records"},
                    {"GET", "/balance_history/agent_histories",         "Get agent financial history records"},
                    {"GET", "/balance_history/staff_welfare_histories", "Get staff welfare financial history"}
                });

        appendModule(sb, "Finance - Bill Data Correction", "/bill_data_correction",
                "Apply corrections and adjustments to financial bill records.",
                githubUrl(branch, "developer_docs/API_BILL_DATA_CORRECTION.md"),
                new String[][]{
                    {"POST", "/bill_data_correction", "Apply corrections to bill data"}
                });

        appendModule(sb, "Finance - Costing Data", "/costing_data",
                "Retrieve billing and costing data for financial analysis and reporting.",
                githubUrl(branch, "developer_docs/API_COSTING_DATA.md"),
                new String[][]{
                    {"GET", "/costing_data/last_bill",                    "Get the most recent bill"},
                    {"GET", "/costing_data/bill",                          "Get bills for a date range"},
                    {"GET", "/costing_data/by_bill_number/{bill_number}", "Get a specific bill by bill number"},
                    {"GET", "/costing_data/by_bill_id/{bill_id}",         "Get a specific bill by internal ID"}
                });

        appendModule(sb, "Consultant Management", "/channel/consultant",
                "Create new consultant (doctor) records and update existing ones. "
                + "IMPORTANT: This module uses the 'Token' header for authentication, not 'Finance'.",
                githubUrl(branch, "developer_docs/API_CONSULTANT_MANAGEMENT.md"),
                new String[][]{
                    {"POST", "/channel/consultant",      "Create a new consultant. Required: name. Optional: title, mobile, phone, fax, address, code, serialNo, specialityId, institutionId, registration, qualification, description"},
                    {"PUT",  "/channel/consultant/{id}", "Update an existing consultant by ID. Same optional fields as POST. Returns 404 if not found."}
                });

        appendModule(sb, "User Management", "/users",
                "Create, read, update, and retire HMIS web users. Manage passwords, loggable departments, "
                + "and individual privilege assignments. Use /users/privileges/available to discover valid privilege names.",
                githubUrl(branch, "developer_docs/API_USER_MANAGEMENT.md"),
                new String[][]{
                    {"GET",    "/users",                          "List users. Filters: query, departmentId, page, size"},
                    {"POST",   "/users",                          "Create a new user"},
                    {"GET",    "/users/{id}",                     "Get user by ID"},
                    {"PUT",    "/users/{id}",                     "Update user details"},
                    {"DELETE", "/users/{id}",                     "Retire (soft-delete) a user"},
                    {"POST",   "/users/{id}/reset-password",      "Admin reset of user password"},
                    {"POST",   "/users/{id}/change-password",     "User changes own password"},
                    {"GET",    "/users/{id}/privileges",          "List privileges for a user"},
                    {"POST",   "/users/{id}/privileges",          "Assign a privilege to a user"},
                    {"DELETE", "/users/{id}/privileges",          "Remove a privilege from a user"},
                    {"GET",    "/users/{id}/departments",         "List loggable departments for a user"},
                    {"POST",   "/users/{id}/departments",         "Assign a loggable department to a user"},
                    {"GET",    "/users/privileges/available",     "List all valid privilege enum names"},
                    {"POST",   "/users/bulk-privileges",          "Bulk-assign privileges to multiple users at once"}
                });

        appendModule(sb, "User Roles", "/user-roles",
                "Create and manage user roles. Assign privileges to roles for role-based access control.",
                githubUrl(branch, "developer_docs/API_USER_MANAGEMENT.md"),
                new String[][]{
                    {"GET",    "/user-roles",                     "List all user roles"},
                    {"POST",   "/user-roles",                     "Create a new role"},
                    {"GET",    "/user-roles/{id}",                "Get role by ID"},
                    {"PUT",    "/user-roles/{id}",                "Update a role"},
                    {"DELETE", "/user-roles/{id}",                "Retire a role"},
                    {"GET",    "/user-roles/{id}/privileges",     "List privileges assigned to a role"},
                    {"POST",   "/user-roles/{id}/privileges",     "Assign a privilege to a role"}
                });

        appendModule(sb, "Login History", "/logins",
                "Query user login history filtered by department, user, and date range. "
                + "Use /logins/last-per-user to find the most recent login per unique user in a department.",
                githubUrl(branch, "developer_docs/API_LOGIN_HISTORY.md"),
                new String[][]{
                    {"GET", "/logins",               "List logins. Filters: departmentId, userId, days, fromDate (yyyy-MM-dd), toDate, page, size"},
                    {"GET", "/logins/last-per-user", "Most recent login per unique user. Filters: departmentId, size"}
                });

        appendModule(sb, "System Configuration", "/config",
                "Set application configuration options at runtime. "
                + "IMPORTANT: Uses the 'Config' header for authentication, not 'Finance'.",
                githubUrl(branch, "developer_docs/API_CONFIG.md"),
                new String[][]{
                    {"POST", "/config/setBoolean/{key}/{value}", "Set a boolean config option by key name"},
                    {"POST", "/config/setLongText/{key}/{value}", "Set a text config option by key name"},
                    {"POST", "/config/setInteger/{key}/{value}", "Set an integer config option by key name"}
                });

        appendModule(sb, "Sites", "/sites",
                "Manage hospital sites (physical collection points or satellite locations). "
                + "A site is an Institution with institutionType=Site.",
                githubUrl(branch, "developer_docs/API_SITES.md"),
                new String[][]{
                    {"GET",    "/sites/search",  "Search sites by name or code. Params: query, limit"},
                    {"GET",    "/sites/{id}",    "Get site by ID"},
                    {"POST",   "/sites",          "Create a new site. Fields: name, code, address, phone, email"},
                    {"PUT",    "/sites/{id}",    "Update a site"},
                    {"DELETE", "/sites/{id}",    "Retire (soft-delete) a site"}
                });

        appendModule(sb, "Inward / Admissions", "/apiInward",
                "Access inpatient admission records and process payments for admitted patients.",
                githubUrl(branch, "developer_docs/API_INWARD.md"),
                new String[][]{
                    {"GET", "/apiInward/admissions",                                           "List active inpatient admissions"},
                    {"GET", "/apiInward/admissions/byPhone/{phone}",                           "Find admission by patient phone number"},
                    {"GET", "/apiInward/banks",                                                "List available banks/payment institutions"},
                    {"GET", "/apiInward/validateAdmission/{bht_no}/{phone}",                   "Validate a BHT number and phone combination before payment"},
                    {"POST", "/apiInward/payment",                                             "Process a payment for an admitted patient"},
                    {"GET", "/apiInward/payment/{bht_no}/{bank_id}/{credit_card_ref}/{amount}", "Process payment via GET (for integrations that cannot POST)"}
                });

        sb.append("## Your Capabilities\n");
        sb.append("- Query and search HMIS data via REST API calls\n");
        sb.append("- Adjust stock, pharmacy, and financial data\n");
        sb.append("- Create and update consultant/doctor records\n");
        sb.append("- Manage users, roles, and system privileges\n");
        sb.append("- Query login history and audit trails\n");
        sb.append("- Access inpatient admission records and process payments\n");
        sb.append("- Analyse reports and uploaded images/documents\n");
        sb.append("- Troubleshoot and explain system behaviour\n\n");
        sb.append("When making API calls, always explain what you are doing and present results clearly. ");
        sb.append("If you need the full documentation for a specific module, ask and it will be fetched for you.\n");

        return sb.toString();
    }

    /**
     * Fetches the full documentation content for a specific module.
     * Call this when the user or Claude requests detailed documentation for a module.
     *
     * @param documentationUrl The GitHub raw URL for the documentation file
     * @return Documentation content, or null if unavailable
     */
    public String fetchDocumentation(String documentationUrl) {
        return fetchTextFromUrl(documentationUrl);
    }

    private String githubUrl(String branch, String filePath) {
        return GITHUB_RAW_BASE + branch + "/" + filePath;
    }

    private void appendModule(StringBuilder sb, String name, String basePath,
            String description, String docUrl, String[][] operations) {
        sb.append("### ").append(name).append("\n");
        sb.append("BasePath: ").append(basePath).append("\n");
        sb.append("Description: ").append(description).append("\n");
        sb.append("Documentation: ").append(docUrl).append("\n");
        sb.append("Operations:\n");
        for (String[] op : operations) {
            sb.append("  - ").append(op[0]).append(" ").append(op[1])
                    .append(" — ").append(op[2]).append("\n");
        }
        sb.append("\n");
    }

    /**
     * Fetches the text content of a URL (used for GitHub raw API docs).
     * Returns null on failure so callers can skip gracefully.
     */
    private String fetchTextFromUrl(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            }
            LOG.log(Level.WARNING, "Could not fetch doc {0} — HTTP {1}", new Object[]{url, response.statusCode()});
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.log(Level.WARNING, "Interrupted fetching doc: {0}", url);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error fetching doc {0}: {1}", new Object[]{url, e.getMessage()});
        }
        return null;
    }

    /**
     * Simple response wrapper class.
     */
    public static class AnthropicResponse {
        private final String content;
        private final Long inputTokens;
        private final Long outputTokens;

        public AnthropicResponse(String content, Long inputTokens, Long outputTokens) {
            this.content = content;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
        }

        public String getContent() { return content; }
        public Long getInputTokens() { return inputTokens; }
        public Long getOutputTokens() { return outputTokens; }
    }
}
