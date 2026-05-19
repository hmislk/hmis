package com.divudi.service;

import com.divudi.core.data.OptionScope;
import com.divudi.core.entity.AiMessage;
import com.divudi.core.entity.ConfigOption;
import com.divudi.core.facade.ConfigOptionFacade;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
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
    private static final String GITHUB_SEARCH_API = "https://api.github.com/search/code";
    private static final int MAX_TOOL_ITERATIONS = 10;
    private static final int MAX_FILE_CONTENT_CHARS = 8000;

    @EJB
    private ConfigOptionFacade configOptionFacade;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Sends a conversation to Claude API and returns the response text.
     * Enables agentic tool use (GitHub code search, file fetch, config search).
     *
     * @param apiKey              Anthropic API key
     * @param model               Claude model ID
     * @param maxTokens           Maximum response tokens
     * @param systemPrompt        System prompt
     * @param conversationHistory Prior messages (NOT including the new user message)
     * @param userMessage         New user message text
     * @param attachmentBase64    Optional base64-encoded attachment (null if none)
     * @param attachmentMimeType  Optional MIME type (null if none)
     * @param githubToken         Optional GitHub personal access token (empty = unauthenticated)
     * @param githubBranch        GitHub branch for file fetches (e.g. "development")
     * @return AnthropicResponse with content, inputTokens, outputTokens
     */
    public AnthropicResponse sendMessage(
            String apiKey,
            String model,
            int maxTokens,
            String systemPrompt,
            List<AiMessage> conversationHistory,
            String userMessage,
            String attachmentBase64,
            String attachmentMimeType,
            String githubToken,
            String githubBranch,
            String hmisBaseUrl,
            String hmisApiKey) {

        try {
            List<JsonObject> messages = new ArrayList<>();

            for (AiMessage msg : conversationHistory) {
                if (msg.getContent() == null || msg.getContent().trim().isEmpty()) {
                    continue;
                }
                messages.add(Json.createObjectBuilder()
                        .add("role", msg.getRole())
                        .add("content", msg.getContent())
                        .build());
            }

            messages.add(buildUserMessage(userMessage, attachmentBase64, attachmentMimeType));

            JsonArray tools = buildToolsArray();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            long totalInputTokens = 0L;
            long totalOutputTokens = 0L;

            // Total wall-clock deadline: 5 minutes for the entire agentic loop
            final long loopDeadlineMs = System.currentTimeMillis() + (5 * 60 * 1000L);
            final long perRequestMaxMs = 120_000L;

            for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
                long remainingMs = loopDeadlineMs - System.currentTimeMillis();
                if (remainingMs <= 0) {
                    return new AnthropicResponse(
                            "Request timed out: the agentic loop exceeded the 5-minute deadline.",
                            totalInputTokens, totalOutputTokens);
                }
                long requestTimeoutMs = Math.min(perRequestMaxMs, remainingMs);

                JsonArrayBuilder messagesBuilder = Json.createArrayBuilder();
                for (JsonObject msg : messages) {
                    messagesBuilder.add(msg);
                }

                JsonObject requestBody = Json.createObjectBuilder()
                        .add("model", model != null ? model : "claude-opus-4-6")
                        .add("max_tokens", maxTokens > 0 ? maxTokens : 4096)
                        .add("system", systemPrompt != null ? systemPrompt : "")
                        .add("tools", tools)
                        .add("messages", messagesBuilder.build())
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.anthropic.com/v1/messages"))
                        .timeout(Duration.ofMillis(requestTimeoutMs))
                        .header("Content-Type", "application/json")
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", "2023-06-01")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    LOG.log(Level.WARNING, "Anthropic API error {0}: {1}",
                            new Object[]{response.statusCode(), response.body()});
                    return new AnthropicResponse(
                            "Error from AI service (HTTP " + response.statusCode() + "): " + response.body(),
                            totalInputTokens, totalOutputTokens);
                }

                JsonObject responseJson;
                try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                    responseJson = reader.readObject();
                }

                JsonObject usage = responseJson.getJsonObject("usage");
                if (usage != null) {
                    totalInputTokens += usage.getInt("input_tokens", 0);
                    totalOutputTokens += usage.getInt("output_tokens", 0);
                }

                String stopReason = responseJson.getString("stop_reason", "end_turn");
                JsonArray contentArray = responseJson.getJsonArray("content");

                if ("end_turn".equals(stopReason)) {
                    return new AnthropicResponse(
                            extractTextFromContent(contentArray),
                            totalInputTokens, totalOutputTokens);
                }

                if ("tool_use".equals(stopReason) && contentArray != null) {
                    // Append the assistant's response (with tool_use blocks) to messages
                    messages.add(Json.createObjectBuilder()
                            .add("role", "assistant")
                            .add("content", contentArray)
                            .build());

                    // Execute each tool call and collect results
                    JsonArrayBuilder toolResultsBuilder = Json.createArrayBuilder();
                    for (int i = 0; i < contentArray.size(); i++) {
                        JsonObject block = contentArray.getJsonObject(i);
                        if ("tool_use".equals(block.getString("type", ""))) {
                            String toolId = block.getString("id", "");
                            String toolName = block.getString("name", "");
                            JsonObject toolInput = block.containsKey("input")
                                    ? block.getJsonObject("input")
                                    : Json.createObjectBuilder().build();

                            String result = executeToolCall(toolName, toolInput, githubToken, githubBranch, hmisBaseUrl, hmisApiKey);
                            LOG.log(Level.INFO, "Tool {0} returned {1} chars", new Object[]{toolName, result.length()});

                            toolResultsBuilder.add(Json.createObjectBuilder()
                                    .add("type", "tool_result")
                                    .add("tool_use_id", toolId)
                                    .add("content", result));
                        }
                    }

                    // Append user message with tool results
                    messages.add(Json.createObjectBuilder()
                            .add("role", "user")
                            .add("content", toolResultsBuilder.build())
                            .build());
                } else {
                    // Unexpected stop_reason — return whatever text we have
                    return new AnthropicResponse(
                            extractTextFromContent(contentArray),
                            totalInputTokens, totalOutputTokens);
                }
            }

            return new AnthropicResponse(
                    "The AI reached the maximum number of tool-use steps. Please try rephrasing your question.",
                    totalInputTokens, totalOutputTokens);

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
     * Backward-compatible overload — GitHub token/branch supplied, no HMIS base URL or key.
     */
    public AnthropicResponse sendMessage(
            String apiKey,
            String model,
            int maxTokens,
            String systemPrompt,
            List<AiMessage> conversationHistory,
            String userMessage,
            String attachmentBase64,
            String attachmentMimeType,
            String githubToken,
            String githubBranch) {
        return sendMessage(apiKey, model, maxTokens, systemPrompt, conversationHistory,
                userMessage, attachmentBase64, attachmentMimeType, githubToken, githubBranch, "", "");
    }

    /**
     * Backward-compatible overload — no GitHub token, branch, or HMIS credentials supplied.
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
        return sendMessage(apiKey, model, maxTokens, systemPrompt, conversationHistory,
                userMessage, attachmentBase64, attachmentMimeType, "", "development", "", "");
    }

    // -------------------------------------------------------------------------
    // Tool definitions
    // -------------------------------------------------------------------------

    private JsonArray buildToolsArray() {
        JsonObject searchCodeTool = Json.createObjectBuilder()
                .add("name", "search_github_code")
                .add("description",
                        "Search the hmislk/hmis GitHub repository for source files matching a keyword query. "
                        + "Use this to find relevant Java classes, XHTML pages, or configuration keys related to a user's question.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("query", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Search keywords, class names, field names, etc."))
                                .add("extension", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Optional file extension filter, e.g. 'java' or 'xhtml'. Omit to search all files.")))
                        .add("required", Json.createArrayBuilder().add("query")))
                .build();

        JsonObject fetchFileTool = Json.createObjectBuilder()
                .add("name", "fetch_github_file")
                .add("description",
                        "Fetch the full content of a specific file from the hmislk/hmis GitHub repository. "
                        + "Use this after search_github_code to read the actual source code or XHTML of a matched file.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("path", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Repository-relative file path, e.g. 'src/main/webapp/inward/inward_admission.xhtml'"))
                                .add("branch", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Branch name. Defaults to the configured branch if omitted.")))
                        .add("required", Json.createArrayBuilder().add("path")))
                .build();

        JsonObject searchConfigTool = Json.createObjectBuilder()
                .add("name", "search_config_options")
                .add("description",
                        "Search the live HMIS application configuration options by keyword. "
                        + "Returns matching config keys, their types, and current values. "
                        + "Use this to find and explain configuration that controls system behaviour. "
                        + "Sensitive values (API keys, passwords, tokens) are automatically masked.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("keyword", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Keyword to search in config option keys (case-insensitive)")))
                        .add("required", Json.createArrayBuilder().add("keyword")))
                .build();

        JsonObject clinicalMetadataTool = Json.createObjectBuilder()
                .add("name", "manage_clinical_metadata")
                .add("description",
                        "Create, list, update, or delete EMR clinical metadata entries (symptoms, signs, diagnoses, "
                        + "procedures, plans, vocabularies, and clinical entities such as race, religion, blood_group, "
                        + "civil_status, employment, relationship). "
                        + "Use this when the user wants to add, search, modify, or remove clinical master data.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder().add("GET").add("POST").add("PUT").add("DELETE"))
                                        .add("description", "HTTP method: GET=list, POST=create, PUT=update, DELETE=soft-delete"))
                                .add("type", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Metadata type: symptom, sign, diagnosis, procedure, plan, vocabulary, race, religion, blood_group, civil_status, employment, relationship. Required for GET and POST."))
                                .add("id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Record ID as a string. Required for PUT and DELETE."))
                                .add("name", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Name of the entry. Required for POST; optional for PUT."))
                                .add("code", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Short code for the entry (optional)."))
                                .add("description", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Description of the entry (optional)."))
                                .add("query", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Text filter for GET (optional)."))
                                .add("page", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Page number for GET, default 0 (optional)."))
                                .add("size", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Page size for GET, default 20 (optional).")))
                        .add("required", Json.createArrayBuilder().add("method")))
                .build();

        JsonObject collectingCentreFeesTool = Json.createObjectBuilder()
                .add("name", "manage_collecting_centre_fees")
                .add("description",
                        "List, create, update, retire, or recalculate item fees for a collecting centre. "
                        + "Use GET to list active fees for a centre (institutionId required). "
                        + "Use POST to add a new fee (collectingCentreId, itemId, name, feeType, fee required). "
                        + "Use PUT to update a fee (feeId required). "
                        + "Use DELETE_ONE to soft-retire a single fee (feeId required). "
                        + "Use DELETE_ALL to retire all active fees for a collecting centre (institutionId required). "
                        + "Use RECALCULATE to refresh item totals after bulk changes (institutionId required).")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder()
                                                .add("GET").add("POST").add("PUT")
                                                .add("DELETE_ONE").add("DELETE_ALL").add("RECALCULATE"))
                                        .add("description", "Operation: GET=list, POST=create, PUT=update, DELETE_ONE=retire single fee, DELETE_ALL=retire all fees for CC, RECALCULATE=refresh item totals"))
                                .add("institutionId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Collecting centre institution ID. Required for GET, DELETE_ALL, RECALCULATE."))
                                .add("feeId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Fee ID. Required for PUT and DELETE_ONE."))
                                .add("collectingCentreId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Collecting centre institution ID for POST (body field)."))
                                .add("itemId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Item (service/investigation) ID. Required for POST."))
                                .add("name", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Fee name. Required for POST; optional for PUT."))
                                .add("feeType", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Fee type enum value, e.g. OwnInstitution, OtherInstitution, Referral, Staff. Required for POST; optional for PUT."))
                                .add("fee", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Local fee amount. Required for POST; optional for PUT."))
                                .add("ffee", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Foreigner fee amount. Optional; defaults to fee if omitted."))
                                .add("departmentId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Department ID. Required for OwnInstitution/OtherInstitution/Referral fee types."))
                                .add("discountAllowed", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Whether discount is allowed: true or false. Optional."))
                                .add("query", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Filter by item name or code for GET. Optional."))
                                .add("limit", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Max results for GET, default 100. Optional."))
                                .add("retireComments", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Reason for retirement. Optional for DELETE_ONE and DELETE_ALL.")))
                        .add("required", Json.createArrayBuilder().add("method")))
                .build();

        JsonObject inwardDiscountMatrixTool = Json.createObjectBuilder()
                .add("name", "manage_inward_discount_matrix")
                .add("description",
                        "Manage Inward Discount Matrix entries (backs the two UI pages "
                        + "inward_discount_matrix_service_investigation.xhtml and inward_discount_matrix_pharmacy.xhtml). "
                        + "Methods: LIST (filter+list), GET (one), POST (create — rejects duplicates), "
                        + "PUT (update), DELETE (soft-retire). "
                        + "Lookup helpers: LOOKUP_DEPARTMENTS, LOOKUP_SERVICE_CATEGORIES, "
                        + "LOOKUP_PHARMACEUTICAL_ITEM_CATEGORIES, LOOKUP_ADMISSION_TYPES, "
                        + "LOOKUP_PAYMENT_SCHEMES, LIST_PAYMENT_METHODS. "
                        + "Always resolve names to IDs via the lookups before POST/PUT.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder()
                                                .add("LIST").add("GET").add("POST").add("PUT").add("DELETE")
                                                .add("LOOKUP_DEPARTMENTS")
                                                .add("LOOKUP_SERVICE_CATEGORIES")
                                                .add("LOOKUP_PHARMACEUTICAL_ITEM_CATEGORIES")
                                                .add("LOOKUP_ADMISSION_TYPES")
                                                .add("LOOKUP_PAYMENT_SCHEMES")
                                                .add("LIST_PAYMENT_METHODS"))
                                        .add("description", "Operation to perform."))
                                .add("scope", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder().add("service").add("pharmacy"))
                                        .add("description", "Required for POST. Optional filter for LIST."))
                                .add("id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Entry id. Required for GET, PUT, DELETE."))
                                .add("departmentId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Department id. Optional for POST/PUT/LIST."))
                                .add("categoryId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Category id (service/investigation or pharmaceutical). Optional for POST/PUT/LIST."))
                                .add("admissionTypeId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "AdmissionType id. Optional."))
                                .add("paymentSchemeId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "PaymentScheme id. Required for POST."))
                                .add("paymentMethod", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "PaymentMethod enum name, e.g. Cash, Credit, Card. Optional."))
                                .add("discountPercent", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Discount percentage. Required for POST."))
                                .add("query", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Search text for LOOKUP_* operations. Optional."))
                                .add("limit", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Max results (1–200). Optional."))
                                .add("retireComments", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Reason for retirement. Optional for DELETE.")))
                        .add("required", Json.createArrayBuilder().add("method")))
                .build();

        JsonObject inwardRoomsTool = Json.createObjectBuilder()
                .add("name", "manage_inward_rooms")
                .add("description",
                        "Manage inward room master data: room categories, rooms, and room facility charges (room fee configs). "
                        + "Methods: LIST_CATEGORIES, POST_CATEGORY, PUT_CATEGORY, DELETE_CATEGORY, "
                        + "LIST_ROOMS, POST_ROOM, PUT_ROOM, DELETE_ROOM, "
                        + "LIST_CHARGES, POST_CHARGE, PUT_CHARGE, DELETE_CHARGE. "
                        + "Always confirm with the user before creating, updating, or retiring records.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder()
                                                .add("LIST_CATEGORIES").add("GET_CATEGORY").add("POST_CATEGORY").add("PUT_CATEGORY").add("DELETE_CATEGORY")
                                                .add("LIST_ROOMS").add("GET_ROOM").add("POST_ROOM").add("PUT_ROOM").add("DELETE_ROOM")
                                                .add("LIST_CHARGES").add("GET_CHARGE").add("POST_CHARGE").add("PUT_CHARGE").add("DELETE_CHARGE"))
                                        .add("description", "Operation to perform."))
                                .add("id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Record id. Required for PUT and DELETE methods."))
                                .add("name", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Name of the record. Required for POST methods."))
                                .add("code", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Short code. Optional."))
                                .add("description", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Description. Optional."))
                                .add("roomCategoryId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Room category id. Used for POST_ROOM/PUT_ROOM and as filter for LIST_ROOMS/LIST_CHARGES."))
                                .add("roomId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Room id. Used for POST_CHARGE/PUT_CHARGE and as filter for LIST_CHARGES."))
                                .add("departmentId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Department id. Required for POST_CHARGE; required (non-null) when supplied on PUT_CHARGE."))
                                .add("filled", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Whether room is under construction (true/false). Optional for POST_ROOM/PUT_ROOM."))
                                .add("roomCharge", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Room charge per block. Optional for POST_CHARGE/PUT_CHARGE."))
                                .add("maintananceCharge", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Maintenance charge per block. Optional."))
                                .add("linenCharge", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Linen charge per day. Optional."))
                                .add("nursingCharge", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Nursing charge per block. Optional."))
                                .add("moCharge", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "MO charge per block. Optional."))
                                .add("moChargeForAfterDuration", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "MO charge for after duration. Optional."))
                                .add("adminstrationCharge", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Administration charge per block. Optional."))
                                .add("medicalCareCharge", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Medical care charge per block. Optional."))
                                .add("timedItemFeeDurationHours", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Fee calculation block duration in hours. Optional."))
                                .add("timedItemFeeOverShootHours", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Over-shoot hours for last block. Optional."))
                                .add("timedItemFeeDurationDaysForMoCharge", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Duration days for MO charge calculation. Optional."))
                                .add("query", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Search text for LIST methods. Optional."))
                                .add("size", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Max results (1–1000). Optional."))
                                .add("retireComments", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Reason for retirement. Optional for DELETE methods.")))
                        .add("required", Json.createArrayBuilder().add("method")))
                .build();

        JsonObject manageInvestigationsTool = Json.createObjectBuilder()
                .add("name", "manage_investigations")
                .add("description",
                        "Search, retrieve, create, update, activate, or deactivate investigation master records "
                        + "(lab/diagnostic tests such as CBC, blood gas, PCR, X-ray when managed as investigations). "
                        + "Use GET to search by name/code/printName. Use POST to create a new investigation (returns "
                        + "already_exists with the existing id if a duplicate name is found). Use PUT to update metadata. "
                        + "Use ACTIVATE/DEACTIVATE to toggle the inactive flag.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Operation: GET=search, GET_BY_ID=fetch one, POST=create, PUT=update, ACTIVATE=set inactive=false, DEACTIVATE=set inactive=true. Required."))
                                .add("id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Investigation ID. Required for GET_BY_ID, PUT, ACTIVATE, DEACTIVATE."))
                                .add("query", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Search text matched against name, code, and printName (case-insensitive). Used with GET."))
                                .add("inactive", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Filter by active/inactive status: 'true' or 'false'. Omit to return both. Used with GET."))
                                .add("limit", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Max results to return (1–100). Defaults to 20. Used with GET."))
                                .add("name", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Investigation name. Required for POST; optional for PUT."))
                                .add("code", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Short code. Auto-generated from name if omitted on POST. Optional for PUT."))
                                .add("printName", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Print/display name shown on reports and bills. Optional."))
                                .add("reportType", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "InvestigationReportType enum value (e.g. General). Optional."))
                                .add("bypassSampleWorkflow", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "'true' to skip sample collection and allow direct result entry after billing. Optional.")))
                        .add("required", Json.createArrayBuilder().add("method")))
                .build();

        return Json.createArrayBuilder()
                .add(searchCodeTool)
                .add(fetchFileTool)
                .add(searchConfigTool)
                .add(clinicalMetadataTool)
                .add(collectingCentreFeesTool)
                .add(inwardDiscountMatrixTool)
                .add(inwardRoomsTool)
                .add(manageInvestigationsTool)
                .build();
    }

    // -------------------------------------------------------------------------
    // Tool execution
    // -------------------------------------------------------------------------

    private String executeToolCall(String toolName, JsonObject toolInput, String githubToken, String githubBranch,
            String hmisBaseUrl, String hmisApiKey) {
        try {
            switch (toolName) {
                case "search_github_code": {
                    String query = toolInput.getString("query", "");
                    String extension = toolInput.containsKey("extension")
                            ? toolInput.getString("extension", "") : "";
                    return searchGithubCode(query, extension, githubToken);
                }
                case "fetch_github_file": {
                    String path = toolInput.getString("path", "");
                    String branch = toolInput.containsKey("branch")
                            ? toolInput.getString("branch", "") : "";
                    if (branch == null || branch.isEmpty()) {
                        branch = (githubBranch != null && !githubBranch.isEmpty())
                                ? githubBranch : "development";
                    }
                    return fetchGithubFile(path, branch, githubToken);
                }
                case "search_config_options": {
                    String keyword = toolInput.getString("keyword", "");
                    return searchConfigOptions(keyword);
                }
                case "manage_clinical_metadata": {
                    String method = toolInput.getString("method", "GET");
                    String type   = toolInput.containsKey("type") ? toolInput.getString("type", "") : "";
                    String id     = toolInput.containsKey("id")   ? toolInput.getString("id", "")   : "";
                    String name   = toolInput.containsKey("name") ? toolInput.getString("name", "") : null;
                    String code   = toolInput.containsKey("code") ? toolInput.getString("code", "") : null;
                    String desc   = toolInput.containsKey("description") ? toolInput.getString("description", "") : null;
                    String query  = toolInput.containsKey("query") ? toolInput.getString("query", "") : "";
                    String page   = toolInput.containsKey("page")  ? toolInput.getString("page", "0") : "0";
                    String size   = toolInput.containsKey("size")  ? toolInput.getString("size", "20") : "20";
                    return callClinicalMetadataApi(method, type, id, name, code, desc, query, page, size,
                            hmisBaseUrl, hmisApiKey);
                }
                case "manage_collecting_centre_fees": {
                    String method          = toolInput.getString("method", "GET");
                    String institutionId   = toolInput.containsKey("institutionId")     ? toolInput.getString("institutionId", "")     : "";
                    String feeId           = toolInput.containsKey("feeId")             ? toolInput.getString("feeId", "")             : "";
                    String ccId            = toolInput.containsKey("collectingCentreId") ? toolInput.getString("collectingCentreId", "") : "";
                    String itemId          = toolInput.containsKey("itemId")             ? toolInput.getString("itemId", "")            : "";
                    String name            = toolInput.containsKey("name")              ? toolInput.getString("name", null)             : null;
                    String feeType         = toolInput.containsKey("feeType")           ? toolInput.getString("feeType", null)          : null;
                    String fee             = toolInput.containsKey("fee")               ? toolInput.getString("fee", null)              : null;
                    String ffee            = toolInput.containsKey("ffee")              ? toolInput.getString("ffee", null)             : null;
                    String departmentId    = toolInput.containsKey("departmentId")      ? toolInput.getString("departmentId", null)     : null;
                    String discountAllowed = toolInput.containsKey("discountAllowed")   ? toolInput.getString("discountAllowed", null)  : null;
                    String query           = toolInput.containsKey("query")             ? toolInput.getString("query", "")             : "";
                    String limit           = toolInput.containsKey("limit")             ? toolInput.getString("limit", "100")          : "100";
                    String retireComments  = toolInput.containsKey("retireComments")    ? toolInput.getString("retireComments", "")    : "";
                    return callCollectingCentreFeesApi(method, institutionId, feeId, ccId, itemId,
                            name, feeType, fee, ffee, departmentId, discountAllowed,
                            query, limit, retireComments, hmisBaseUrl, hmisApiKey);
                }
                case "manage_inward_discount_matrix": {
                    String method            = toolInput.getString("method", "LIST");
                    String scope             = toolInput.containsKey("scope")            ? toolInput.getString("scope", "")            : "";
                    String id                = toolInput.containsKey("id")               ? toolInput.getString("id", "")               : "";
                    String departmentId      = toolInput.containsKey("departmentId")     ? toolInput.getString("departmentId", "")     : "";
                    String categoryId        = toolInput.containsKey("categoryId")       ? toolInput.getString("categoryId", "")       : "";
                    String admissionTypeId   = toolInput.containsKey("admissionTypeId")  ? toolInput.getString("admissionTypeId", "")  : "";
                    String paymentSchemeId   = toolInput.containsKey("paymentSchemeId")  ? toolInput.getString("paymentSchemeId", "")  : "";
                    String paymentMethodStr  = toolInput.containsKey("paymentMethod")    ? toolInput.getString("paymentMethod", "")    : "";
                    String discountPercent   = toolInput.containsKey("discountPercent")  ? toolInput.getString("discountPercent", "")  : "";
                    String query             = toolInput.containsKey("query")            ? toolInput.getString("query", "")            : "";
                    String limit             = toolInput.containsKey("limit")            ? toolInput.getString("limit", "")            : "";
                    String retireComments    = toolInput.containsKey("retireComments")   ? toolInput.getString("retireComments", "")   : "";
                    return callInwardDiscountMatrixApi(method, scope, id, departmentId, categoryId,
                            admissionTypeId, paymentSchemeId, paymentMethodStr, discountPercent,
                            query, limit, retireComments, hmisBaseUrl, hmisApiKey);
                }
                case "manage_investigations": {
                    String method = toolInput.getString("method", "GET");
                    String id = toolInput.containsKey("id") ? toolInput.getString("id", "") : "";
                    String query = toolInput.containsKey("query") ? toolInput.getString("query", "") : "";
                    String inactive = toolInput.containsKey("inactive") ? toolInput.getString("inactive", "") : "";
                    String limit = toolInput.containsKey("limit") ? toolInput.getString("limit", "20") : "20";
                    String name = toolInput.containsKey("name") ? toolInput.getString("name", "") : "";
                    String code = toolInput.containsKey("code") ? toolInput.getString("code", "") : "";
                    String printName = toolInput.containsKey("printName") ? toolInput.getString("printName", "") : "";
                    String reportType = toolInput.containsKey("reportType") ? toolInput.getString("reportType", "") : "";
                    String bypass = toolInput.containsKey("bypassSampleWorkflow") ? toolInput.getString("bypassSampleWorkflow", "") : "";
                    return callInvestigationApi(method, id, query, inactive, limit, name, code, printName, reportType, bypass, hmisBaseUrl, hmisApiKey);
                }
                case "manage_inward_rooms": {
                    String method         = toolInput.getString("method", "LIST_CATEGORIES");
                    String id             = toolInput.containsKey("id")                             ? toolInput.getString("id", "")                             : "";
                    String name           = toolInput.containsKey("name")                           ? toolInput.getString("name", "")                           : "";
                    String code           = toolInput.containsKey("code")                           ? toolInput.getString("code", "")                           : "";
                    String desc           = toolInput.containsKey("description")                    ? toolInput.getString("description", "")                    : "";
                    String roomCategoryId = toolInput.containsKey("roomCategoryId")                 ? toolInput.getString("roomCategoryId", "")                 : "";
                    String roomId         = toolInput.containsKey("roomId")                         ? toolInput.getString("roomId", "")                         : "";
                    String departmentId   = toolInput.containsKey("departmentId")                   ? toolInput.getString("departmentId", "")                   : "";
                    String filled         = toolInput.containsKey("filled")                         ? toolInput.getString("filled", "")                         : "";
                    String roomCharge     = toolInput.containsKey("roomCharge")                     ? toolInput.getString("roomCharge", "")                     : "";
                    String maintCharge    = toolInput.containsKey("maintananceCharge")              ? toolInput.getString("maintananceCharge", "")              : "";
                    String linenCharge    = toolInput.containsKey("linenCharge")                    ? toolInput.getString("linenCharge", "")                    : "";
                    String nursingCharge  = toolInput.containsKey("nursingCharge")                  ? toolInput.getString("nursingCharge", "")                  : "";
                    String moCharge       = toolInput.containsKey("moCharge")                       ? toolInput.getString("moCharge", "")                       : "";
                    String moAfterCharge  = toolInput.containsKey("moChargeForAfterDuration")       ? toolInput.getString("moChargeForAfterDuration", "")       : "";
                    String adminCharge    = toolInput.containsKey("adminstrationCharge")            ? toolInput.getString("adminstrationCharge", "")            : "";
                    String medCareCharge  = toolInput.containsKey("medicalCareCharge")              ? toolInput.getString("medicalCareCharge", "")              : "";
                    String durationHours  = toolInput.containsKey("timedItemFeeDurationHours")      ? toolInput.getString("timedItemFeeDurationHours", "")      : "";
                    String overShoot      = toolInput.containsKey("timedItemFeeOverShootHours")     ? toolInput.getString("timedItemFeeOverShootHours", "")     : "";
                    String durationDays   = toolInput.containsKey("timedItemFeeDurationDaysForMoCharge") ? toolInput.getString("timedItemFeeDurationDaysForMoCharge", "") : "";
                    String query          = toolInput.containsKey("query")                          ? toolInput.getString("query", "")                          : "";
                    String size           = toolInput.containsKey("size")                           ? toolInput.getString("size", "")                           : "";
                    String retireComments = toolInput.containsKey("retireComments")                 ? toolInput.getString("retireComments", "")                 : "";
                    return callInwardRoomsApi(method, id, name, code, desc, roomCategoryId, roomId,
                            departmentId, filled, roomCharge, maintCharge, linenCharge, nursingCharge,
                            moCharge, moAfterCharge, adminCharge, medCareCharge,
                            durationHours, overShoot, durationDays,
                            query, size, retireComments, hmisBaseUrl, hmisApiKey);
                }
                default:
                    return "Unknown tool: " + toolName;
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Tool execution failed for {0}: {1}", new Object[]{toolName, e.getMessage()});
            return "Tool execution error: " + e.getMessage();
        }
    }

    private String searchGithubCode(String query, String extension, String githubToken) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return "Error: query is required.";
            }
            String q = URLEncoder.encode(query + " repo:hmislk/hmis", StandardCharsets.UTF_8);
            if (extension != null && !extension.isEmpty()) {
                q += "+" + URLEncoder.encode("extension:" + extension, StandardCharsets.UTF_8);
            }
            String url = GITHUB_SEARCH_API + "?q=" + q + "&per_page=10";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/vnd.github+json")
                    .GET();
            if (githubToken != null && !githubToken.isEmpty()) {
                reqBuilder.header("Authorization", "Bearer " + githubToken);
            }

            HttpResponse<String> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return "GitHub search failed (HTTP " + response.statusCode() + "): " + response.body();
            }

            try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                JsonObject json = reader.readObject();
                int totalCount = json.getInt("total_count", 0);
                JsonArray items = json.getJsonArray("items");

                if (items == null || items.isEmpty()) {
                    return "No results found for query: " + query;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("Found ").append(totalCount).append(" result(s). Top matches:\n\n");
                for (int i = 0; i < items.size(); i++) {
                    JsonObject item = items.getJsonObject(i);
                    String path = item.getString("path", "");
                    String name = item.getString("name", "");
                    sb.append("- ").append(path).append(" (").append(name).append(")\n");
                    if (item.containsKey("text_matches")) {
                        JsonArray matches = item.getJsonArray("text_matches");
                        for (int j = 0; j < Math.min(2, matches.size()); j++) {
                            JsonObject match = matches.getJsonObject(j);
                            String fragment = match.getString("fragment", "").trim();
                            if (!fragment.isEmpty()) {
                                int end = Math.min(200, fragment.length());
                                sb.append("  Snippet: ").append(fragment, 0, end).append("\n");
                            }
                        }
                    }
                }
                return sb.toString();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "GitHub search interrupted.";
        } catch (Exception e) {
            return "GitHub search error: " + e.getMessage();
        }
    }

    private String fetchGithubFile(String path, String branch, String githubToken) {
        try {
            if (path == null || path.trim().isEmpty()) {
                return "Error: path is required.";
            }
            String url = GITHUB_RAW_BASE + branch + "/" + path;

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .GET();
            if (githubToken != null && !githubToken.isEmpty()) {
                reqBuilder.header("Authorization", "Bearer " + githubToken);
            }

            HttpResponse<String> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return "File not found: " + path + " (branch: " + branch + ")";
            }
            if (response.statusCode() != 200) {
                return "Failed to fetch file (HTTP " + response.statusCode() + "): " + path;
            }

            String content = response.body();
            if (content.length() > MAX_FILE_CONTENT_CHARS) {
                content = content.substring(0, MAX_FILE_CONTENT_CHARS)
                        + "\n\n[... content truncated at " + MAX_FILE_CONTENT_CHARS + " characters ...]";
            }
            return "File: " + path + " (branch: " + branch + ")\n\n" + content;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "File fetch interrupted.";
        } catch (Exception e) {
            return "File fetch error: " + e.getMessage();
        }
    }

    private String searchConfigOptions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return "Error: keyword is required.";
        }
        try {
            String jpql = "SELECT o FROM ConfigOption o "
                    + "WHERE o.retired = false "
                    + "AND o.scope = :scope "
                    + "AND LOWER(o.optionKey) LIKE :kw "
                    + "ORDER BY o.optionKey";
            Map<String, Object> params = new HashMap<>();
            params.put("scope", OptionScope.APPLICATION);
            params.put("kw", "%" + keyword.toLowerCase() + "%");

            List<ConfigOption> options = configOptionFacade.findByJpql(jpql, params);

            if (options == null || options.isEmpty()) {
                return "No config options found matching: " + keyword;
            }

            final int maxRows = 20;
            final int maxValueChars = 200;
            int displayed = Math.min(options.size(), maxRows);

            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(options.size())
                    .append(" config option(s) matching \"").append(keyword).append("\"");
            if (options.size() > maxRows) {
                sb.append(" (showing first ").append(maxRows).append(")");
            }
            sb.append(":\n\n");

            for (ConfigOption opt : options.subList(0, displayed)) {
                String value = maskSensitiveValue(opt.getOptionKey(), opt.getOptionValue());
                if (value != null && value.length() > maxValueChars) {
                    value = value.substring(0, maxValueChars) + "... (truncated)";
                }
                sb.append("Key: ").append(opt.getOptionKey()).append("\n");
                sb.append("Type: ").append(opt.getValueType()).append("\n");
                sb.append("Value: ").append(value).append("\n\n");
            }
            if (options.size() > maxRows) {
                sb.append("... ").append(options.size() - maxRows).append(" more match(es) omitted. Refine your keyword for fewer results.\n");
            }
            return sb.toString();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Config option search failed", e);
            return "Config search error: " + e.getMessage();
        }
    }

    private String maskSensitiveValue(String key, String value) {
        if (key == null || value == null || value.isEmpty()) {
            return value;
        }
        String lk = key.toLowerCase();
        if (lk.contains("password") || lk.contains("secret")
                || lk.contains("api key") || lk.contains("token")
                || lk.contains("apikey") || lk.contains("private")) {
            return "***masked***";
        }
        return value;
    }

    private String callClinicalMetadataApi(String method, String type, String id,
            String name, String code, String desc, String query, String page, String size,
            String hmisBaseUrl, String hmisApiKey) {
        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured. Cannot call clinical metadata API.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: No active HMIS API key found for the current user.";
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String base = hmisBaseUrl.trim().replaceAll("/+$", "") + "/api/clinical/metadata";
            String url;
            String requestBody = null;
            String httpMethod;

            switch (method.toUpperCase()) {
                case "GET": {
                    StringBuilder urlBuilder = new StringBuilder(base).append("?type=").append(URLEncoder.encode(type, StandardCharsets.UTF_8));
                    if (query != null && !query.isEmpty()) urlBuilder.append("&query=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
                    if (page != null && !page.isEmpty()) urlBuilder.append("&page=").append(page);
                    if (size != null && !size.isEmpty()) urlBuilder.append("&size=").append(size);
                    url = urlBuilder.toString();
                    httpMethod = "GET";
                    break;
                }
                case "POST": {
                    url = base + "?type=" + URLEncoder.encode(type, StandardCharsets.UTF_8);
                    httpMethod = "POST";
                    javax.json.JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();
                    if (name != null) bodyBuilder.add("name", name);
                    if (code != null && !code.isEmpty()) bodyBuilder.add("code", code);
                    if (desc != null && !desc.isEmpty()) bodyBuilder.add("description", desc);
                    requestBody = bodyBuilder.build().toString();
                    break;
                }
                case "PUT": {
                    if (id == null || id.trim().isEmpty()) return "Error: id is required for PUT.";
                    if (type == null || type.trim().isEmpty()) return "Error: type is required for PUT.";
                    url = base + "/" + id.trim() + "?type=" + URLEncoder.encode(type.trim(), StandardCharsets.UTF_8);
                    httpMethod = "PUT";
                    javax.json.JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();
                    if (name != null && !name.isEmpty()) bodyBuilder.add("name", name);
                    if (code != null && !code.isEmpty()) bodyBuilder.add("code", code);
                    if (desc != null && !desc.isEmpty()) bodyBuilder.add("description", desc);
                    requestBody = bodyBuilder.build().toString();
                    break;
                }
                case "DELETE": {
                    if (id == null || id.trim().isEmpty()) return "Error: id is required for DELETE.";
                    url = base + "/" + id.trim();
                    httpMethod = "DELETE";
                    break;
                }
                default:
                    return "Error: Unknown method: " + method;
            }

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Finance", hmisApiKey)
                    .header("Content-Type", "application/json");

            if (requestBody != null) {
                reqBuilder.method(httpMethod, HttpRequest.BodyPublishers.ofString(requestBody));
            } else if ("DELETE".equals(httpMethod)) {
                reqBuilder.DELETE();
            } else {
                reqBuilder.GET();
            }

            HttpResponse<String> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return "HTTP " + response.statusCode() + "\n" + response.body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Clinical metadata API call interrupted.";
        } catch (Exception e) {
            return "Clinical metadata API error: " + e.getMessage();
        }
    }

    private String callCollectingCentreFeesApi(
            String method, String institutionId, String feeId, String ccId, String itemId,
            String name, String feeType, String fee, String ffee, String departmentId,
            String discountAllowed, String query, String limit, String retireComments,
            String hmisBaseUrl, String hmisApiKey) {

        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: No active HMIS API key found for the current user.";
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String base = hmisBaseUrl.trim().replaceAll("/+$", "") + "/api/pricing/collecting_centre_fees";
            String url;
            String requestBody = null;
            String httpMethod;

            switch (method.toUpperCase()) {
                case "GET": {
                    if (institutionId == null || institutionId.trim().isEmpty()) {
                        return "Error: institutionId is required for GET.";
                    }
                    StringBuilder urlBuilder = new StringBuilder(base)
                            .append("?institutionId=").append(URLEncoder.encode(institutionId.trim(), StandardCharsets.UTF_8));
                    if (query != null && !query.isEmpty()) {
                        urlBuilder.append("&query=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
                    }
                    if (limit != null && !limit.isEmpty()) {
                        urlBuilder.append("&limit=").append(limit);
                    }
                    url = urlBuilder.toString();
                    httpMethod = "GET";
                    break;
                }
                case "POST": {
                    url = base;
                    httpMethod = "POST";
                    javax.json.JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();
                    if (ccId != null && !ccId.trim().isEmpty()) bodyBuilder.add("collectingCentreId", Long.parseLong(ccId.trim()));
                    if (itemId != null && !itemId.trim().isEmpty()) bodyBuilder.add("itemId", Long.parseLong(itemId.trim()));
                    if (name != null) bodyBuilder.add("name", name);
                    if (feeType != null) bodyBuilder.add("feeType", feeType);
                    if (fee != null && !fee.trim().isEmpty()) bodyBuilder.add("fee", Double.parseDouble(fee.trim()));
                    if (ffee != null && !ffee.trim().isEmpty()) bodyBuilder.add("ffee", Double.parseDouble(ffee.trim()));
                    if (departmentId != null && !departmentId.trim().isEmpty()) bodyBuilder.add("departmentId", Long.parseLong(departmentId.trim()));
                    if (discountAllowed != null && !discountAllowed.trim().isEmpty()) bodyBuilder.add("discountAllowed", Boolean.parseBoolean(discountAllowed.trim()));
                    requestBody = bodyBuilder.build().toString();
                    break;
                }
                case "PUT": {
                    if (feeId == null || feeId.trim().isEmpty()) return "Error: feeId is required for PUT.";
                    url = base + "/" + feeId.trim();
                    httpMethod = "PUT";
                    javax.json.JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();
                    if (name != null && !name.isEmpty()) bodyBuilder.add("name", name);
                    if (feeType != null && !feeType.isEmpty()) bodyBuilder.add("feeType", feeType);
                    if (fee != null && !fee.trim().isEmpty()) bodyBuilder.add("fee", Double.parseDouble(fee.trim()));
                    if (ffee != null && !ffee.trim().isEmpty()) bodyBuilder.add("ffee", Double.parseDouble(ffee.trim()));
                    if (departmentId != null && !departmentId.trim().isEmpty()) bodyBuilder.add("departmentId", Long.parseLong(departmentId.trim()));
                    if (discountAllowed != null && !discountAllowed.trim().isEmpty()) bodyBuilder.add("discountAllowed", Boolean.parseBoolean(discountAllowed.trim()));
                    requestBody = bodyBuilder.build().toString();
                    break;
                }
                case "DELETE_ONE": {
                    if (feeId == null || feeId.trim().isEmpty()) return "Error: feeId is required for DELETE_ONE.";
                    StringBuilder urlBuilder = new StringBuilder(base).append("/").append(feeId.trim());
                    if (retireComments != null && !retireComments.isEmpty()) {
                        urlBuilder.append("?retireComments=").append(URLEncoder.encode(retireComments, StandardCharsets.UTF_8));
                    }
                    url = urlBuilder.toString();
                    httpMethod = "DELETE";
                    break;
                }
                case "DELETE_ALL": {
                    if (institutionId == null || institutionId.trim().isEmpty()) return "Error: institutionId is required for DELETE_ALL.";
                    StringBuilder urlBuilder = new StringBuilder(base)
                            .append("?institutionId=").append(URLEncoder.encode(institutionId.trim(), StandardCharsets.UTF_8));
                    if (retireComments != null && !retireComments.isEmpty()) {
                        urlBuilder.append("&retireComments=").append(URLEncoder.encode(retireComments, StandardCharsets.UTF_8));
                    }
                    url = urlBuilder.toString();
                    httpMethod = "DELETE";
                    break;
                }
                case "RECALCULATE": {
                    if (institutionId == null || institutionId.trim().isEmpty()) return "Error: institutionId is required for RECALCULATE.";
                    url = base + "/recalculate?institutionId=" + URLEncoder.encode(institutionId.trim(), StandardCharsets.UTF_8);
                    httpMethod = "POST";
                    requestBody = "{}";
                    break;
                }
                default:
                    return "Error: Unknown method: " + method;
            }

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Finance", hmisApiKey)
                    .header("Content-Type", "application/json");

            if (requestBody != null) {
                reqBuilder.method(httpMethod, HttpRequest.BodyPublishers.ofString(requestBody));
            } else if ("DELETE".equals(httpMethod)) {
                reqBuilder.DELETE();
            } else {
                reqBuilder.GET();
            }

            HttpResponse<String> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return "HTTP " + response.statusCode() + "\n" + response.body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Collecting centre fees API call interrupted.";
        } catch (Exception e) {
            return "Collecting centre fees API error: " + e.getMessage();
        }
    }

    private String callInwardDiscountMatrixApi(
            String method, String scope, String id, String departmentId, String categoryId,
            String admissionTypeId, String paymentSchemeId, String paymentMethod,
            String discountPercent, String query, String limit, String retireComments,
            String hmisBaseUrl, String hmisApiKey) {

        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: No active HMIS API key found for the current user.";
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String root = hmisBaseUrl.trim().replaceAll("/+$", "");
            String base = root + "/api/inward-discount-matrix";
            String url;
            String requestBody = null;
            String httpMethod;

            switch (method == null ? "" : method.toUpperCase()) {
                case "LIST": {
                    StringBuilder urlBuilder = new StringBuilder(base);
                    boolean first = true;
                    if (scope != null && !scope.isEmpty()) {
                        urlBuilder.append(first ? "?" : "&").append("scope=")
                                .append(URLEncoder.encode(scope, StandardCharsets.UTF_8));
                        first = false;
                    }
                    if (departmentId != null && !departmentId.isEmpty()) {
                        urlBuilder.append(first ? "?" : "&").append("departmentId=").append(departmentId);
                        first = false;
                    }
                    if (categoryId != null && !categoryId.isEmpty()) {
                        urlBuilder.append(first ? "?" : "&").append("categoryId=").append(categoryId);
                        first = false;
                    }
                    if (admissionTypeId != null && !admissionTypeId.isEmpty()) {
                        urlBuilder.append(first ? "?" : "&").append("admissionTypeId=").append(admissionTypeId);
                        first = false;
                    }
                    if (paymentSchemeId != null && !paymentSchemeId.isEmpty()) {
                        urlBuilder.append(first ? "?" : "&").append("paymentSchemeId=").append(paymentSchemeId);
                        first = false;
                    }
                    if (paymentMethod != null && !paymentMethod.isEmpty()) {
                        urlBuilder.append(first ? "?" : "&").append("paymentMethod=")
                                .append(URLEncoder.encode(paymentMethod, StandardCharsets.UTF_8));
                        first = false;
                    }
                    if (limit != null && !limit.isEmpty()) {
                        urlBuilder.append(first ? "?" : "&").append("limit=").append(limit);
                        first = false;
                    }
                    url = urlBuilder.toString();
                    httpMethod = "GET";
                    break;
                }
                case "GET": {
                    if (id == null || id.trim().isEmpty()) return "Error: id is required for GET.";
                    url = base + "/" + id.trim();
                    httpMethod = "GET";
                    break;
                }
                case "POST": {
                    url = base;
                    httpMethod = "POST";
                    javax.json.JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();
                    if (scope != null && !scope.isEmpty()) bodyBuilder.add("scope", scope);
                    if (departmentId != null && !departmentId.trim().isEmpty()) bodyBuilder.add("departmentId", Long.parseLong(departmentId.trim()));
                    if (categoryId != null && !categoryId.trim().isEmpty()) bodyBuilder.add("categoryId", Long.parseLong(categoryId.trim()));
                    if (admissionTypeId != null && !admissionTypeId.trim().isEmpty()) bodyBuilder.add("admissionTypeId", Long.parseLong(admissionTypeId.trim()));
                    if (paymentSchemeId != null && !paymentSchemeId.trim().isEmpty()) bodyBuilder.add("paymentSchemeId", Long.parseLong(paymentSchemeId.trim()));
                    if (paymentMethod != null && !paymentMethod.isEmpty()) bodyBuilder.add("paymentMethod", paymentMethod);
                    if (discountPercent != null && !discountPercent.trim().isEmpty()) bodyBuilder.add("discountPercent", Double.parseDouble(discountPercent.trim()));
                    requestBody = bodyBuilder.build().toString();
                    break;
                }
                case "PUT": {
                    if (id == null || id.trim().isEmpty()) return "Error: id is required for PUT.";
                    url = base + "/" + id.trim();
                    httpMethod = "PUT";
                    javax.json.JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();
                    if (scope != null && !scope.isEmpty()) bodyBuilder.add("scope", scope);
                    if (departmentId != null && !departmentId.trim().isEmpty()) bodyBuilder.add("departmentId", Long.parseLong(departmentId.trim()));
                    if (categoryId != null && !categoryId.trim().isEmpty()) bodyBuilder.add("categoryId", Long.parseLong(categoryId.trim()));
                    if (admissionTypeId != null && !admissionTypeId.trim().isEmpty()) bodyBuilder.add("admissionTypeId", Long.parseLong(admissionTypeId.trim()));
                    if (paymentSchemeId != null && !paymentSchemeId.trim().isEmpty()) bodyBuilder.add("paymentSchemeId", Long.parseLong(paymentSchemeId.trim()));
                    if (paymentMethod != null && !paymentMethod.isEmpty()) bodyBuilder.add("paymentMethod", paymentMethod);
                    if (discountPercent != null && !discountPercent.trim().isEmpty()) bodyBuilder.add("discountPercent", Double.parseDouble(discountPercent.trim()));
                    requestBody = bodyBuilder.build().toString();
                    break;
                }
                case "DELETE": {
                    if (id == null || id.trim().isEmpty()) return "Error: id is required for DELETE.";
                    StringBuilder urlBuilder = new StringBuilder(base).append("/").append(id.trim());
                    if (retireComments != null && !retireComments.isEmpty()) {
                        urlBuilder.append("?retireComments=")
                                .append(URLEncoder.encode(retireComments, StandardCharsets.UTF_8));
                    }
                    url = urlBuilder.toString();
                    httpMethod = "DELETE";
                    break;
                }
                case "LOOKUP_DEPARTMENTS": {
                    StringBuilder urlBuilder = new StringBuilder(root).append("/api/departments/search");
                    if (query != null && !query.isEmpty()) {
                        urlBuilder.append("?query=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
                        if (limit != null && !limit.isEmpty()) urlBuilder.append("&limit=").append(limit);
                    } else if (limit != null && !limit.isEmpty()) {
                        urlBuilder.append("?limit=").append(limit);
                    }
                    url = urlBuilder.toString();
                    httpMethod = "GET";
                    break;
                }
                case "LOOKUP_SERVICE_CATEGORIES": {
                    StringBuilder urlBuilder = new StringBuilder(root).append("/api/services/categories/search");
                    if (query != null && !query.isEmpty()) {
                        urlBuilder.append("?query=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
                        if (limit != null && !limit.isEmpty()) urlBuilder.append("&limit=").append(limit);
                    } else if (limit != null && !limit.isEmpty()) {
                        urlBuilder.append("?limit=").append(limit);
                    }
                    url = urlBuilder.toString();
                    httpMethod = "GET";
                    break;
                }
                case "LOOKUP_PHARMACEUTICAL_ITEM_CATEGORIES": {
                    url = lookupUrl(base + "/pharmaceutical-item-categories/search", query, limit);
                    httpMethod = "GET";
                    break;
                }
                case "LOOKUP_ADMISSION_TYPES": {
                    url = lookupUrl(base + "/admission-types/search", query, limit);
                    httpMethod = "GET";
                    break;
                }
                case "LOOKUP_PAYMENT_SCHEMES": {
                    url = lookupUrl(base + "/payment-schemes/search", query, limit);
                    httpMethod = "GET";
                    break;
                }
                case "LIST_PAYMENT_METHODS": {
                    url = base + "/payment-methods";
                    httpMethod = "GET";
                    break;
                }
                default:
                    return "Error: Unknown method: " + method;
            }

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Finance", hmisApiKey)
                    .header("Content-Type", "application/json");

            if (requestBody != null) {
                reqBuilder.method(httpMethod, HttpRequest.BodyPublishers.ofString(requestBody));
            } else if ("DELETE".equals(httpMethod)) {
                reqBuilder.DELETE();
            } else {
                reqBuilder.GET();
            }

            HttpResponse<String> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return "HTTP " + response.statusCode() + "\n" + response.body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Inward discount matrix API call interrupted.";
        } catch (Exception e) {
            return "Inward discount matrix API error: " + e.getMessage();
        }
    }

    private String lookupUrl(String base, String query, String limit) {
        StringBuilder urlBuilder = new StringBuilder(base);
        boolean first = true;
        if (query != null && !query.isEmpty()) {
            urlBuilder.append("?query=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
            first = false;
        }
        if (limit != null && !limit.isEmpty()) {
            urlBuilder.append(first ? "?" : "&").append("limit=").append(limit);
        }
        return urlBuilder.toString();
    }

    private String callInwardRoomsApi(
            String method, String id, String name, String code, String description,
            String roomCategoryId, String roomId, String departmentId, String filled,
            String roomCharge, String maintananceCharge, String linenCharge, String nursingCharge,
            String moCharge, String moChargeForAfterDuration, String adminstrationCharge, String medicalCareCharge,
            String timedItemFeeDurationHours, String timedItemFeeOverShootHours, String timedItemFeeDurationDaysForMoCharge,
            String query, String size, String retireComments,
            String hmisBaseUrl, String hmisApiKey) {

        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: HMIS API key is not configured.";
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String baseUrl = hmisBaseUrl.endsWith("/") ? hmisBaseUrl.substring(0, hmisBaseUrl.length() - 1) : hmisBaseUrl;

            HttpRequest request;

            switch (method) {
                case "LIST_CATEGORIES": {
                    StringBuilder url = new StringBuilder(baseUrl).append("/api/inward/room-categories");
                    boolean first = true;
                    if (query != null && !query.isEmpty()) { url.append("?query=").append(java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)); first = false; }
                    if (size != null && !size.isEmpty()) { url.append(first ? "?" : "&").append("size=").append(size); }
                    request = HttpRequest.newBuilder().uri(URI.create(url.toString()))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey).GET().build();
                    break;
                }
                case "GET_CATEGORY": {
                    if (id == null || id.isEmpty()) return "Error: id is required for GET_CATEGORY.";
                    request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/inward/room-categories/" + id))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey).GET().build();
                    break;
                }
                case "POST_CATEGORY": {
                    java.util.Map<String, Object> bodyMap = new java.util.LinkedHashMap<>();
                    bodyMap.put("name", name);
                    if (code != null && !code.isEmpty()) bodyMap.put("code", code);
                    if (description != null && !description.isEmpty()) bodyMap.put("description", description);
                    String bodyJson = new com.google.gson.Gson().toJson(bodyMap);
                    request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/inward/room-categories"))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(bodyJson)).build();
                    break;
                }
                case "PUT_CATEGORY": {
                    if (id == null || id.isEmpty()) return "Error: id is required for PUT_CATEGORY.";
                    java.util.Map<String, Object> bodyMap = new java.util.LinkedHashMap<>();
                    if (name != null && !name.isEmpty()) bodyMap.put("name", name);
                    if (code != null && !code.isEmpty()) bodyMap.put("code", code);
                    if (description != null && !description.isEmpty()) bodyMap.put("description", description);
                    String bodyJson = new com.google.gson.Gson().toJson(bodyMap);
                    request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/inward/room-categories/" + id))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey)
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofString(bodyJson)).build();
                    break;
                }
                case "DELETE_CATEGORY": {
                    if (id == null || id.isEmpty()) return "Error: id is required for DELETE_CATEGORY.";
                    StringBuilder url = new StringBuilder(baseUrl).append("/api/inward/room-categories/").append(id);
                    if (retireComments != null && !retireComments.isEmpty()) url.append("?retireComments=").append(java.net.URLEncoder.encode(retireComments, java.nio.charset.StandardCharsets.UTF_8));
                    request = HttpRequest.newBuilder().uri(URI.create(url.toString()))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey)
                            .DELETE().build();
                    break;
                }
                case "LIST_ROOMS": {
                    StringBuilder url = new StringBuilder(baseUrl).append("/api/inward/rooms");
                    boolean first = true;
                    if (query != null && !query.isEmpty()) { url.append("?query=").append(java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)); first = false; }
                    if (roomCategoryId != null && !roomCategoryId.isEmpty()) { url.append(first ? "?" : "&").append("roomCategoryId=").append(roomCategoryId); first = false; }
                    if (size != null && !size.isEmpty()) { url.append(first ? "?" : "&").append("size=").append(size); }
                    request = HttpRequest.newBuilder().uri(URI.create(url.toString()))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey).GET().build();
                    break;
                }
                case "GET_ROOM": {
                    if (id == null || id.isEmpty()) return "Error: id is required for GET_ROOM.";
                    request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/inward/rooms/" + id))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey).GET().build();
                    break;
                }
                case "POST_ROOM": {
                    java.util.Map<String, Object> bodyMap = new java.util.LinkedHashMap<>();
                    bodyMap.put("name", name);
                    if (code != null && !code.isEmpty()) bodyMap.put("code", code);
                    if (description != null && !description.isEmpty()) bodyMap.put("description", description);
                    if (roomCategoryId != null && !roomCategoryId.isEmpty()) bodyMap.put("roomCategoryId", Long.parseLong(roomCategoryId));
                    if (filled != null && !filled.isEmpty()) bodyMap.put("filled", Boolean.parseBoolean(filled));
                    String bodyJson = new com.google.gson.Gson().toJson(bodyMap);
                    request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/inward/rooms"))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(bodyJson)).build();
                    break;
                }
                case "PUT_ROOM": {
                    if (id == null || id.isEmpty()) return "Error: id is required for PUT_ROOM.";
                    java.util.Map<String, Object> bodyMap = new java.util.LinkedHashMap<>();
                    if (name != null && !name.isEmpty()) bodyMap.put("name", name);
                    if (code != null && !code.isEmpty()) bodyMap.put("code", code);
                    if (description != null && !description.isEmpty()) bodyMap.put("description", description);
                    if (roomCategoryId != null && !roomCategoryId.isEmpty()) bodyMap.put("roomCategoryId", Long.parseLong(roomCategoryId));
                    if (filled != null && !filled.isEmpty()) bodyMap.put("filled", Boolean.parseBoolean(filled));
                    String bodyJson = new com.google.gson.Gson().toJson(bodyMap);
                    request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/inward/rooms/" + id))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey)
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofString(bodyJson)).build();
                    break;
                }
                case "DELETE_ROOM": {
                    if (id == null || id.isEmpty()) return "Error: id is required for DELETE_ROOM.";
                    StringBuilder url = new StringBuilder(baseUrl).append("/api/inward/rooms/").append(id);
                    if (retireComments != null && !retireComments.isEmpty()) url.append("?retireComments=").append(java.net.URLEncoder.encode(retireComments, java.nio.charset.StandardCharsets.UTF_8));
                    request = HttpRequest.newBuilder().uri(URI.create(url.toString()))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey)
                            .DELETE().build();
                    break;
                }
                case "LIST_CHARGES": {
                    StringBuilder url = new StringBuilder(baseUrl).append("/api/inward/room-facility-charges");
                    boolean first = true;
                    if (query != null && !query.isEmpty()) { url.append("?query=").append(java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)); first = false; }
                    if (roomId != null && !roomId.isEmpty()) { url.append(first ? "?" : "&").append("roomId=").append(roomId); first = false; }
                    if (roomCategoryId != null && !roomCategoryId.isEmpty()) { url.append(first ? "?" : "&").append("roomCategoryId=").append(roomCategoryId); first = false; }
                    if (size != null && !size.isEmpty()) { url.append(first ? "?" : "&").append("size=").append(size); }
                    request = HttpRequest.newBuilder().uri(URI.create(url.toString()))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey).GET().build();
                    break;
                }
                case "GET_CHARGE": {
                    if (id == null || id.isEmpty()) return "Error: id is required for GET_CHARGE.";
                    request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/inward/room-facility-charges/" + id))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey).GET().build();
                    break;
                }
                case "POST_CHARGE": {
                    java.util.Map<String, Object> bodyMap = new java.util.LinkedHashMap<>();
                    bodyMap.put("name", name);
                    if (roomId != null && !roomId.isEmpty()) bodyMap.put("roomId", Long.parseLong(roomId));
                    if (roomCategoryId != null && !roomCategoryId.isEmpty()) bodyMap.put("roomCategoryId", Long.parseLong(roomCategoryId));
                    if (departmentId != null && !departmentId.isEmpty()) bodyMap.put("departmentId", Long.parseLong(departmentId));
                    if (roomCharge != null && !roomCharge.isEmpty()) bodyMap.put("roomCharge", Double.parseDouble(roomCharge));
                    if (maintananceCharge != null && !maintananceCharge.isEmpty()) bodyMap.put("maintananceCharge", Double.parseDouble(maintananceCharge));
                    if (linenCharge != null && !linenCharge.isEmpty()) bodyMap.put("linenCharge", Double.parseDouble(linenCharge));
                    if (nursingCharge != null && !nursingCharge.isEmpty()) bodyMap.put("nursingCharge", Double.parseDouble(nursingCharge));
                    if (moCharge != null && !moCharge.isEmpty()) bodyMap.put("moCharge", Double.parseDouble(moCharge));
                    if (moChargeForAfterDuration != null && !moChargeForAfterDuration.isEmpty()) bodyMap.put("moChargeForAfterDuration", Double.parseDouble(moChargeForAfterDuration));
                    if (adminstrationCharge != null && !adminstrationCharge.isEmpty()) bodyMap.put("adminstrationCharge", Double.parseDouble(adminstrationCharge));
                    if (medicalCareCharge != null && !medicalCareCharge.isEmpty()) bodyMap.put("medicalCareCharge", Double.parseDouble(medicalCareCharge));
                    if (timedItemFeeDurationHours != null && !timedItemFeeDurationHours.isEmpty()) bodyMap.put("timedItemFeeDurationHours", Double.parseDouble(timedItemFeeDurationHours));
                    if (timedItemFeeOverShootHours != null && !timedItemFeeOverShootHours.isEmpty()) bodyMap.put("timedItemFeeOverShootHours", Double.parseDouble(timedItemFeeOverShootHours));
                    if (timedItemFeeDurationDaysForMoCharge != null && !timedItemFeeDurationDaysForMoCharge.isEmpty()) bodyMap.put("timedItemFeeDurationDaysForMoCharge", Long.parseLong(timedItemFeeDurationDaysForMoCharge));
                    String bodyJson = new com.google.gson.Gson().toJson(bodyMap);
                    request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/inward/room-facility-charges"))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(bodyJson)).build();
                    break;
                }
                case "PUT_CHARGE": {
                    if (id == null || id.isEmpty()) return "Error: id is required for PUT_CHARGE.";
                    java.util.Map<String, Object> bodyMap = new java.util.LinkedHashMap<>();
                    if (name != null && !name.isEmpty()) bodyMap.put("name", name);
                    if (roomId != null && !roomId.isEmpty()) bodyMap.put("roomId", Long.parseLong(roomId));
                    if (roomCategoryId != null && !roomCategoryId.isEmpty()) bodyMap.put("roomCategoryId", Long.parseLong(roomCategoryId));
                    if (departmentId != null && !departmentId.isEmpty()) bodyMap.put("departmentId", Long.parseLong(departmentId));
                    if (roomCharge != null && !roomCharge.isEmpty()) bodyMap.put("roomCharge", Double.parseDouble(roomCharge));
                    if (maintananceCharge != null && !maintananceCharge.isEmpty()) bodyMap.put("maintananceCharge", Double.parseDouble(maintananceCharge));
                    if (linenCharge != null && !linenCharge.isEmpty()) bodyMap.put("linenCharge", Double.parseDouble(linenCharge));
                    if (nursingCharge != null && !nursingCharge.isEmpty()) bodyMap.put("nursingCharge", Double.parseDouble(nursingCharge));
                    if (moCharge != null && !moCharge.isEmpty()) bodyMap.put("moCharge", Double.parseDouble(moCharge));
                    if (moChargeForAfterDuration != null && !moChargeForAfterDuration.isEmpty()) bodyMap.put("moChargeForAfterDuration", Double.parseDouble(moChargeForAfterDuration));
                    if (adminstrationCharge != null && !adminstrationCharge.isEmpty()) bodyMap.put("adminstrationCharge", Double.parseDouble(adminstrationCharge));
                    if (medicalCareCharge != null && !medicalCareCharge.isEmpty()) bodyMap.put("medicalCareCharge", Double.parseDouble(medicalCareCharge));
                    if (timedItemFeeDurationHours != null && !timedItemFeeDurationHours.isEmpty()) bodyMap.put("timedItemFeeDurationHours", Double.parseDouble(timedItemFeeDurationHours));
                    if (timedItemFeeOverShootHours != null && !timedItemFeeOverShootHours.isEmpty()) bodyMap.put("timedItemFeeOverShootHours", Double.parseDouble(timedItemFeeOverShootHours));
                    if (timedItemFeeDurationDaysForMoCharge != null && !timedItemFeeDurationDaysForMoCharge.isEmpty()) bodyMap.put("timedItemFeeDurationDaysForMoCharge", Long.parseLong(timedItemFeeDurationDaysForMoCharge));
                    String bodyJson = new com.google.gson.Gson().toJson(bodyMap);
                    request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/inward/room-facility-charges/" + id))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey)
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofString(bodyJson)).build();
                    break;
                }
                case "DELETE_CHARGE": {
                    if (id == null || id.isEmpty()) return "Error: id is required for DELETE_CHARGE.";
                    StringBuilder url = new StringBuilder(baseUrl).append("/api/inward/room-facility-charges/").append(id);
                    if (retireComments != null && !retireComments.isEmpty()) url.append("?retireComments=").append(java.net.URLEncoder.encode(retireComments, java.nio.charset.StandardCharsets.UTF_8));
                    request = HttpRequest.newBuilder().uri(URI.create(url.toString()))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey)
                            .DELETE().build();
                    break;
                }
                default:
                    return "Unknown method: " + method;
            }

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return "HTTP " + response.statusCode() + ": " + response.body();

        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "callInwardRoomsApi error: {0}", e.getMessage());
            return "Error calling Inward Rooms API: " + e.getMessage();
        }
    }

    // -------------------------------------------------------------------------
    // Message building helpers
    // -------------------------------------------------------------------------

    private JsonObject buildUserMessage(String userMessage, String attachmentBase64, String attachmentMimeType) {
        if (attachmentBase64 != null && !attachmentBase64.isEmpty() && attachmentMimeType != null) {
            JsonArrayBuilder contentBuilder = Json.createArrayBuilder();

            if (attachmentMimeType.startsWith("image/")) {
                JsonObject imageSource = Json.createObjectBuilder()
                        .add("type", "base64")
                        .add("media_type", attachmentMimeType)
                        .add("data", attachmentBase64)
                        .build();
                contentBuilder.add(Json.createObjectBuilder()
                        .add("type", "image")
                        .add("source", imageSource));
            } else {
                JsonObject docSource = Json.createObjectBuilder()
                        .add("type", "base64")
                        .add("media_type", attachmentMimeType)
                        .add("data", attachmentBase64)
                        .build();
                contentBuilder.add(Json.createObjectBuilder()
                        .add("type", "document")
                        .add("source", docSource));
            }

            if (userMessage != null && !userMessage.trim().isEmpty()) {
                contentBuilder.add(Json.createObjectBuilder()
                        .add("type", "text")
                        .add("text", userMessage));
            }

            return Json.createObjectBuilder()
                    .add("role", "user")
                    .add("content", contentBuilder.build())
                    .build();
        } else {
            return Json.createObjectBuilder()
                    .add("role", "user")
                    .add("content", userMessage != null ? userMessage : "")
                    .build();
        }
    }

    private String extractTextFromContent(JsonArray contentArray) {
        if (contentArray == null) {
            return "";
        }
        for (int i = 0; i < contentArray.size(); i++) {
            JsonObject block = contentArray.getJsonObject(i);
            if ("text".equals(block.getString("type", ""))) {
                return block.getString("text", "");
            }
        }
        return "";
    }

    // -------------------------------------------------------------------------
    // System prompt
    // -------------------------------------------------------------------------

    /**
     * Builds the system prompt with HMIS context and tool-use instructions.
     *
     * @param hmisApiBaseUrl  The HMIS REST API base URL
     * @param userHmisApiKey  The logged-in user's active HMIS API key value
     * @param githubBranch    The GitHub branch for documentation links (e.g. "development")
     */
    private String callInvestigationApi(String method, String id, String query, String inactive, String limit, String name, String code, String printName, String reportType, String bypassSampleWorkflow, String hmisBaseUrl, String hmisApiKey) {
        try {
            String root = (hmisBaseUrl != null) ? hmisBaseUrl.trim().replaceAll("/+$", "") : "";
            if (root.isEmpty()) return "Error: HMIS base URL is not configured.";
            String key = (hmisApiKey != null) ? hmisApiKey.trim() : "";
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest.Builder rb;
            if ("GET".equalsIgnoreCase(method)) {
                String url = root+"/api/investigations/search?query="+URLEncoder.encode(query, StandardCharsets.UTF_8)+"&limit="+URLEncoder.encode(limit, StandardCharsets.UTF_8);
                if(inactive!=null&&!inactive.isEmpty()) url += "&inactive="+URLEncoder.encode(inactive, StandardCharsets.UTF_8);
                rb = HttpRequest.newBuilder().uri(URI.create(url)).GET();
            } else if ("GET_BY_ID".equalsIgnoreCase(method)) { rb = HttpRequest.newBuilder().uri(URI.create(root+"/api/investigations/"+id)).GET(); }
            else if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                javax.json.JsonObjectBuilder b = Json.createObjectBuilder().add("name", name==null?"":name);
                if(code!=null&&!code.isEmpty()) b.add("code", code); if(printName!=null&&!printName.isEmpty()) b.add("printName", printName); if(reportType!=null&&!reportType.isEmpty()) b.add("reportType", reportType); if(bypassSampleWorkflow!=null&&!bypassSampleWorkflow.isEmpty()) b.add("bypassSampleWorkflow", Boolean.parseBoolean(bypassSampleWorkflow));
                String u = "POST".equalsIgnoreCase(method) ? root+"/api/investigations" : root+"/api/investigations/"+id;
                rb = HttpRequest.newBuilder().uri(URI.create(u)).method("POST".equalsIgnoreCase(method)?"POST":"PUT", HttpRequest.BodyPublishers.ofString(b.build().toString())).header("Content-Type", "application/json");
            } else if ("ACTIVATE".equalsIgnoreCase(method) || "DEACTIVATE".equalsIgnoreCase(method)) {
                String u = root + "/api/investigations/" + id
                        + ("ACTIVATE".equalsIgnoreCase(method) ? "/activate" : "/deactivate");
                rb = HttpRequest.newBuilder().uri(URI.create(u))
                        .method("PATCH", HttpRequest.BodyPublishers.noBody());
            } else {
                return "Error: Unsupported method for manage_investigations: " + method
                        + ". Allowed methods are GET, GET_BY_ID, POST, PUT, ACTIVATE, DEACTIVATE.";
            }
            if(!key.isEmpty()) rb.header("Finance", key); HttpResponse<String> resp=client.send(rb.build(), HttpResponse.BodyHandlers.ofString()); return "HTTP "+resp.statusCode()+"\n"+resp.body();
        } catch (Exception e) { return "Investigation API error: "+e.getMessage(); }
    }

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
            sb.append("- 'Finance' header: Pharmacy, Institution, Department, Finance, Users, Login History, Sites, Inward, Membership, and most other modules\n");
            sb.append("- 'FHIR' header: FHIR Patient (/fhir/Patient) — not the Finance header\n");
            sb.append("- 'Token' header: Consultant Management (/channel/consultant) and Channel/Booking (/channel/*)\n");
            sb.append("- 'Config' header: System Configuration (/config)\n");
            sb.append("- LIMS authentication is module-specific: /lims uses URL-embedded credentials, /middleware uses JSON-body credentials, /limsmw uses HTTP Basic Auth — do NOT use the Finance header for LIMS.\n");
            sb.append("Each module description notes its required authentication scheme when it differs from 'Finance'.\n\n");
        }

        sb.append("## Tools Available to You\n");
        sb.append("You have eight tools to ground your answers in the actual codebase, live configuration, clinical master data, collecting-centre fees, inward discount matrix entries, and investigation master records:\n\n");
        sb.append("### search_github_code\n");
        sb.append("Searches the hmislk/hmis repository source code for files matching keywords. ");
        sb.append("Use this first when a user asks about system behaviour, page logic, or wants to understand how something works.\n\n");
        sb.append("### fetch_github_file\n");
        sb.append("Fetches the full content of a specific file from the repository (default branch: ").append(branch).append("). ");
        sb.append("Use after search_github_code to read the actual source or XHTML.\n\n");
        sb.append("### search_config_options\n");
        sb.append("Searches live application configuration options by keyword and returns the key name, type, and current value. ");
        sb.append("Use this to find config keys that control a behaviour the user is asking about. ");
        sb.append("You can then use POST /config/setBoolean, /config/setInteger, or /config/setLongText to change a value if the user asks.\n\n");
        sb.append("### manage_clinical_metadata\n");
        sb.append("Directly create, list, update, or delete EMR clinical metadata entries (symptoms, signs, diagnoses, procedures, plans, vocabularies, ")
          .append("race, religion, blood_group, civil_status, employment, relationship). ")
          .append("Use this when the user wants to add or manage clinical master data without navigating the UI. ")
          .append("Always confirm with the user before creating or deleting entries.\n\n");
        sb.append("### manage_collecting_centre_fees\n");
        sb.append("List, create, update, retire, or recalculate item fees for a collecting centre.\n\n");
        sb.append("### manage_inward_discount_matrix\n");
        sb.append("Manage Inward Discount Matrix entries for services/investigations and pharmacy. ")
          .append("Use scope='service' or scope='pharmacy' to pick the correct category universe. ")
          .append("Resolve names to IDs first using the lookup methods (LOOKUP_DEPARTMENTS, LOOKUP_SERVICE_CATEGORIES, ")
          .append("LOOKUP_PHARMACEUTICAL_ITEM_CATEGORIES, LOOKUP_ADMISSION_TYPES, LOOKUP_PAYMENT_SCHEMES, LIST_PAYMENT_METHODS), ")
          .append("then POST to create, PUT to update, or DELETE to retire. ")
          .append("Always confirm with the user before POST, PUT, or DELETE — these changes affect live inward billing discounts. ")
          .append("POST returns 'already_exists' with the existing id when a duplicate combination already exists.\n\n");
        sb.append("### manage_investigations\n");
        sb.append("Search, retrieve, create, update, activate, or deactivate investigation master records ")
          .append("(lab/diagnostic tests such as CBC, PCR, blood gas, X-ray managed as investigations). ")
          .append("Use GET to search by name, code, or printName. ")
          .append("Use POST to create — returns 'already_exists' with the existing id when a duplicate name is found, ")
          .append("so always check before creating to avoid duplicates. ")
          .append("Use PUT to update name, code, printName, reportType, or bypassSampleWorkflow. ")
          .append("Always confirm with the user before POST or PUT — these changes affect live investigation billing.\n\n");
        sb.append("### manage_inward_rooms\n");
        sb.append("Manage inward room master data: room categories (/inward/room-categories), ")
          .append("rooms (/inward/rooms), and room facility charges — i.e. room fee configurations — (/inward/room-facility-charges). ")
          .append("Use LIST_CATEGORIES / LIST_ROOMS / LIST_CHARGES to browse. ")
          .append("POST_CATEGORY / POST_ROOM / POST_CHARGE to create new records. ")
          .append("PUT_CATEGORY / PUT_ROOM / PUT_CHARGE to update. ")
          .append("DELETE_CATEGORY / DELETE_ROOM / DELETE_CHARGE to soft-retire. ")
          .append("Always confirm with the user before POST, PUT, or DELETE — these changes affect live inward room billing.\n\n");

        sb.append("## How to Use the Tools\n");
        sb.append("- When a user describes a problem or asks why something behaves a certain way, search the source code first.\n");
        sb.append("- Fetch specific files (Java controllers, XHTML pages) to read the logic that controls the behaviour.\n");
        sb.append("- Search config options to find whether a configuration key controls the behaviour.\n");
        sb.append("- Combine tool results to give a precise, grounded answer with the actual config key name and current value.\n");
        sb.append("- If you identify a config fix, offer to apply it via the REST API (ask the user to confirm before writing).\n\n");

        sb.append("## Available API Modules\n");
        sb.append("Each module lists its operations. For detailed parameter documentation, use fetch_github_file on the relevant developer_docs file.\n\n");

        // ── Pharmacy ──────────────────────────────────────────────────────────
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

        appendModule(sb, "Pharmaceutical Items", "/pharmaceutical_items",
                "Manage pharmaceutical master data: VTM (active ingredients), ATM, VMP (generic products), AMP (branded products), VMPP, and AMPP. Supports full CRUD, retire/restore, and activate/deactivate.",
                githubUrl(branch, "developer_docs/API_PHARMACEUTICAL_MANAGEMENT.md"),
                new String[][]{
                    {"GET",    "/pharmaceutical_items/{type}/search",              "Search items by name or code (types: vtm, atm, vmp, amp, vmpp, ampp)"},
                    {"GET",    "/pharmaceutical_items/{type}/{id}",                "Get a pharmaceutical item by ID"},
                    {"POST",   "/pharmaceutical_items/{type}",                     "Create a new pharmaceutical item"},
                    {"PUT",    "/pharmaceutical_items/{type}/{id}",                "Update an existing pharmaceutical item"},
                    {"DELETE", "/pharmaceutical_items/{type}/{id}",                "Retire a pharmaceutical item"},
                    {"POST",   "/pharmaceutical_items/{type}/{id}/restore",        "Restore a retired pharmaceutical item"},
                    {"PATCH",  "/pharmaceutical_items/{type}/{id}/activate",       "Activate a pharmaceutical item"},
                    {"PATCH",  "/pharmaceutical_items/{type}/{id}/deactivate",     "Deactivate a pharmaceutical item"}
                });

        appendModule(sb, "Pharmaceutical Config", "/pharmaceutical_config",
                "Manage pharmaceutical configuration entities: categories, dosage forms, and measurement units.",
                githubUrl(branch, "developer_docs/API_PHARMACEUTICAL_MANAGEMENT.md"),
                new String[][]{
                    {"GET",    "/pharmaceutical_config/{type}/search",  "Search config entries by name or code (types: categories, dosage_forms, units)"},
                    {"GET",    "/pharmaceutical_config/{type}/{id}",    "Get config entry by ID"},
                    {"POST",   "/pharmaceutical_config/{type}",         "Create a new config entry"},
                    {"PUT",    "/pharmaceutical_config/{type}/{id}",    "Update a config entry"},
                    {"DELETE", "/pharmaceutical_config/{type}/{id}",    "Retire a config entry"}
                });

        appendModule(sb, "Pharmacy - Backfill Operations", "/pharmacy",
                "ADMINISTRATIVE/MAINTENANCE ONLY — requires explicit system-administrator authorisation. "
                + "Reconstruct missing BillFinanceDetail (BFD) and BillItemFinanceDetail (BIFD) records "
                + "on historical pharmacy bills. Always supply auditComment and approvedBy. "
                + "Do NOT execute these without administrator approval.",
                githubUrl(branch, "developer_docs/API_PHARMACEUTICAL_MANAGEMENT.md"),
                new String[][]{
                    {"POST", "/pharmacy/backfill_bfd",      "Backfill missing BFD records for historical pharmacy adjustment bills"},
                    {"POST", "/pharmacy/backfill_grn_bifd", "Backfill missing BIFD/BFD records for historical Pharmacy GRN bills"}
                });

        // ── Institution / Department / Sites ──────────────────────────────────
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

        // ── Staff / Consultants ───────────────────────────────────────────────
        appendModule(sb, "Consultant Management", "/channel/consultant",
                "List, create, and update consultant (doctor) records. "
                + "IMPORTANT: Uses the 'Token' header, not 'Finance'.",
                githubUrl(branch, "developer_docs/API_CONSULTANT_MANAGEMENT.md"),
                new String[][]{
                    {"GET",  "/channel/consultant",      "List consultants. Supports query, page, size, specialityId."},
                    {"POST", "/channel/consultant",      "Create a new consultant. Required: name. Optional: title, sex, mobile, phone, fax, address, code, serialNo, specialityId, institutionId, registration, qualification, description. Returns already_exists/409 for duplicates by name+title."},
                    {"PUT",  "/channel/consultant/{id}", "Update an existing consultant by ID. Supports sex and returns 400 for invalid field values, 404 if not found."}
                });

        // ── Channel / Booking ─────────────────────────────────────────────────
        appendModule(sb, "Channel / Booking", "/channel",
                "Manage online doctor appointment bookings end-to-end: browse specialties, hospitals, doctors and sessions, then create, edit, complete or cancel bookings. "
                + "IMPORTANT: Uses the 'Token' header (not 'Finance'). Wrong booking parameters can create bad appointments — always confirm session availability before saving.",
                githubUrl(branch, "developer_docs/API_CHANNEL_BOOKING.md"),
                new String[][]{
                    {"POST", "/channel/specializations",    "List all medical specialties available for booking"},
                    {"POST", "/channel/hospitals",          "List hospitals/institutions available for a booking channel"},
                    {"POST", "/channel/doctors",            "List doctors filtered by speciality and booking channel"},
                    {"POST", "/channel/doctorAvailability", "Check a doctor's available sessions for a given date"},
                    {"POST", "/channel/doctorSessions",     "List all upcoming sessions for a doctor"},
                    {"POST", "/channel/doctorSession",      "Get details of a single session by ID"},
                    {"POST", "/channel/save",               "Create a new appointment booking"},
                    {"POST", "/channel/edit",               "Edit an existing booking"},
                    {"POST", "/channel/complete",           "Mark a booking as complete/attended"},
                    {"POST", "/channel/channelHistoryList", "Get appointment booking history"},
                    {"POST", "/channel/channelHistoryByRef","Get a booking by its reference number"},
                    {"POST", "/channel/cancellation",       "Cancel an existing booking"}
                });

        // ── Users / Roles / Privileges ────────────────────────────────────────
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

        // ── Finance ───────────────────────────────────────────────────────────
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

        appendModule(sb, "Finance - Legacy Bill Query", "/finance",
                "Legacy bill query endpoints. Use for category-based filtering or simple date-range queries. "
                + "Prefer /costing_data for richer detail. Date format: dd-MM-yyyy; for ranges: dd-MM-yyyy-HH:mm:ss.",
                githubUrl(branch, "developer_docs/API_FINANCE_LEGACY.md"),
                new String[][]{
                    {"GET", "/finance/bill",                                              "Get all bills for today"},
                    {"GET", "/finance/bill/{date}",                                       "Get bills for a specific date (dd-MM-yyyy)"},
                    {"GET", "/finance/bill/{from}/{to}",                                  "Get bills for a date range"},
                    {"GET", "/finance/bill_item",                                         "Get all bills with line items for today"},
                    {"GET", "/finance/bill_item/{date}",                                  "Get bills with line items for a specific date"},
                    {"GET", "/finance/bill_item/{from}/{to}",                             "Get bills with line items for a date range"},
                    {"GET", "/finance/bill_item_cat/{bill_category}",                     "Get bills filtered by BillType category (today)"},
                    {"GET", "/finance/bill_item_cat/{date}/{bill_category}",              "Get bills by category for a specific date"},
                    {"GET", "/finance/bill_item_cat/{from}/{to}/{bill_category}",         "Get bills by category for a date range"}
                });

        appendModule(sb, "Finance - QuickBooks Export", "/qb",
                "Export HMIS financial data for QuickBooks synchronisation. All endpoints use incremental sync: supply the last synced record ID and a start date to retrieve the next batch (up to 2500 records). Dates in yyyy-MM-dd format.",
                githubUrl(branch, "developer_docs/API_QUICKBOOKS.md"),
                new String[][]{
                    {"GET", "/qb/last_invoice_id/{institution_code}/{last_date}",              "Get highest bill ID on or after last_date — use as start before paginating"},
                    {"GET", "/qb/cInvList/{institution_code}/{last_invoice_id}/{last_date}",   "Cash-paid invoices"},
                    {"GET", "/qb/invList/{institution_code}/{last_invoice_id}/{last_date}",    "Credit-paid outpatient invoices and inpatient final bills"},
                    {"GET", "/qb/salesRetList/{institution_code}/{last_invoice_id}/{last_date}","Sales return / voided invoices"},
                    {"GET", "/qb/grnList/{institution_code}/{last_grn_id}/{last_date}",         "Pharmacy Goods Received Notes"},
                    {"GET", "/qb/grnRetList/{institution_code}/{last_return_grn_id}/{last_date}","GRN returns"},
                    {"GET", "/qb/wcList/{institution_code}/{last_return_grn_id}/{last_date}",   "Write-off and stock correction entries"},
                    {"GET", "/qb/jurList/{institution_code}/{last_return_grn_id}/{last_date}",  "Journal entries"},
                    {"GET", "/qb/cusPayList/{institution_code}/{last_payment_id}/{last_date}",  "Customer payment records"},
                    {"GET", "/qb/paymentreturn/{institution_code}/{last_return_payment_id}",    "Payment return / refund records"}
                });

        // ── Clinical ──────────────────────────────────────────────────────────
        appendModule(sb, "Clinical - Metadata", "/clinical/metadata",
                "Manage EMR clinical master data. Required param: type. "
                + "Types: symptom, sign, diagnosis, procedure, plan, vocabulary, "
                + "race, religion, blood_group, civil_status, employment, relationship. "
                + "POST returns success/already_exists (with id)/error. "
                + "PUT and DELETE use /{id} and work across all types.",
                null,
                new String[][]{
                    {"GET",    "/clinical/metadata?type=X",    "List entries of the given type. Supports query, page, size"},
                    {"POST",   "/clinical/metadata?type=X",    "Create a new entry. Body: {name, code, description}. Returns already_exists with id if duplicate name"},
                    {"PUT",    "/clinical/metadata/{id}",      "Update an entry by ID. Body: {name, code, description} (all optional)"},
                    {"DELETE", "/clinical/metadata/{id}",      "Soft-delete an entry by ID"}
                });

        appendModule(sb, "Clinical - Favourite Medicines", "/clinical/favourite_medicines",
                "Manage clinician favourite medicine templates. "
                + "/validate (bulk entity validation) is live. "
                + "/parse and /suggest are not yet implemented (return 501).",
                githubUrl(branch, "developer_docs/API_CLINICAL_FAVOURITE_MEDICINES.md"),
                new String[][]{
                    {"GET",    "/clinical/favourite_medicines",              "List favourite medicine templates"},
                    {"POST",   "/clinical/favourite_medicines",              "Create a new template"},
                    {"GET",    "/clinical/favourite_medicines/{id}",         "Get template by ID"},
                    {"PUT",    "/clinical/favourite_medicines/{id}",         "Update a template"},
                    {"DELETE", "/clinical/favourite_medicines/{id}",         "Retire a template"},
                    {"POST",   "/clinical/favourite_medicines/parse",        "Not implemented (501) — reserved for future natural language parsing"},
                    {"POST",   "/clinical/favourite_medicines/suggest",      "Not implemented (501) — reserved for future auto-suggest"},
                    {"POST",   "/clinical/favourite_medicines/validate",     "Bulk-validate a set of medicine entities"},
                    {"GET",    "/clinical/favourite_medicines/entities/vtms","List/search Virtual Therapeutic Moieties"},
                    {"GET",    "/clinical/favourite_medicines/entities/amps", "List/search Actual Medicinal Products"}
                });

        // ── FHIR ──────────────────────────────────────────────────────────────
        appendModule(sb, "FHIR - Financial Data", "/fhir",
                "HL7 FHIR R5-compliant access to invoices, GRN records, payments, and returns. Uses 'Finance' header.",
                githubUrl(branch, "developer_docs/API_FHIR.md"),
                new String[][]{
                    {"GET", "/fhir/cash_invoice/{institution_code}/{last_invoice_id}",           "Get cash invoices newer than last_invoice_id"},
                    {"GET", "/fhir/credit_invoice/{institution_code}/{last_invoice_id}",         "Get credit invoices newer than last_invoice_id"},
                    {"GET", "/fhir/invoicereturn/{institution_code}/{last_return_invoice_id}",   "Get invoice returns newer than last_return_invoice_id"},
                    {"GET", "/fhir/grn/{institution_code}/{last_grn_id}",                        "Get GRN records newer than last_grn_id"},
                    {"GET", "/fhir/grnreturn/{institution_code}/{last_return_grn_id}",           "Get GRN returns newer than last_return_grn_id"},
                    {"GET", "/fhir/payment/{institution_code}/{last_payment_id}",                "Get payment records newer than last_payment_id"},
                    {"GET", "/fhir/paymentreturn/{institution_code}/{last_return_payment_id}",   "Get payment return / refund records"}
                });

        appendModule(sb, "FHIR - Patient", "/fhir/Patient",
                "HL7 FHIR R5 Patient resource. Authentication uses 'FHIR' header (not 'Finance').",
                githubUrl(branch, "developer_docs/API_FHIR.md"),
                new String[][]{
                    {"GET",  "/fhir/Patient",      "Search patients (supported parameters: name, phone, identifier)"},
                    {"GET",  "/fhir/Patient/{id}", "Read a patient by ID"},
                    {"POST", "/fhir/Patient",      "Create a new patient"},
                    {"PUT",  "/fhir/Patient/{id}", "Update a patient"}
                });

        // ── LIMS ──────────────────────────────────────────────────────────────
        appendModule(sb, "LIMS - Laboratory (/lims, /middleware, /limsmw)", "/lims",
                "Three resources for laboratory integration. "
                + "/lims: sample barcodes and legacy credential checks (URL-embedded credentials). "
                + "/middleware: analyzer middleware for test orders and result ingestion (JSON body credentials). "
                + "/limsmw: HL7/Sysmex/observation processing (HTTP Basic Auth). "
                + "CAUTION: result-write endpoints (/middleware/test_results, /limsmw/observation, /limsmw/sysmex, /limsmw/limsProcessAnalyzerMessage) write into patient records — never call manually.",
                githubUrl(branch, "developer_docs/API_LIMS.md"),
                new String[][]{
                    {"POST", "/lims/login/mw",                                          "Authenticate a middleware client (JSON body)"},
                    {"GET",  "/lims/samples/login/{username}/{password}",               "Legacy credential check (URL params)"},
                    {"GET",  "/lims/samples/{billId}/{username}/{password}",            "Get sample barcodes for a bill"},
                    {"GET",  "/lims/samples1/{billId}/{username}/{password}",           "Get sample barcodes (enhanced, preferred)"},
                    {"GET",  "/lims/middleware/{machine}/{message}/{username}/{password}","Send raw analyzer message"},
                    {"GET",  "/middleware",                                              "Middleware health check"},
                    {"POST", "/middleware/test_orders_for_sample_requests",             "Get test orders for sample IDs"},
                    {"POST", "/middleware/test_results",                                "Push analyzer results into HMIS (WRITE — use with care)"},
                    {"GET",  "/limsmw/test",                                            "LIMS middleware health check"},
                    {"POST", "/limsmw/observation",                                     "Submit a single observation result (WRITE)"},
                    {"POST", "/limsmw/sysmex",                                          "Receive Sysmex ASTM message (HTTP Basic Auth, WRITE)"},
                    {"POST", "/limsmw/limsProcessAnalyzerMessage",                      "Process HL7 analyzer message (HTTP Basic Auth, WRITE)"},
                    {"POST", "/limsmw/login",                                           "Authenticate a middleware client"}
                });

        // ── Membership ────────────────────────────────────────────────────────
        appendModule(sb, "Membership", "/apiMembership",
                "Manage membership schemes, patient registration under a membership, and membership billing.",
                githubUrl(branch, "developer_docs/API_MEMBERSHIP.md"),
                new String[][]{
                    {"GET", "/apiMembership/banks",                                                                 "List available bank institutions for payment"},
                    {"GET", "/apiMembership/savePatient/{title}/{name}/{sex}/{dob}/{address}/{phone}/{nic}",         "Register a new patient under the membership scheme"},
                    {"GET", "/apiMembership/patient/{patient_id}",                                                  "Get patient details by internal ID"},
                    {"GET", "/apiMembership/serviceValue",                                                           "Get membership service fee, VAT, and total payable amount"}
                });

        // ── Pricing / Collecting Centre Fees ─────────────────────────────────
        appendModule(sb, "Collecting Centre Fees", "/pricing/collecting_centre_fees",
                "Manage item fees for collecting centres (view from the centre perspective). "
                + "GET lists active fees for a centre. POST adds a new fee. PUT updates a fee. "
                + "DELETE /{feeId} retires a single fee. DELETE ?institutionId=X retires all fees for a centre. "
                + "POST /recalculate?institutionId=X recalculates item totals after bulk changes.",
                null,
                new String[][]{
                    {"GET",    "/pricing/collecting_centre_fees?institutionId=X",             "List active fees for a collecting centre. Optional: query, limit"},
                    {"POST",   "/pricing/collecting_centre_fees",                              "Add a new fee. Body: collectingCentreId, itemId, name, feeType, fee, ffee, departmentId, discountAllowed"},
                    {"PUT",    "/pricing/collecting_centre_fees/{feeId}",                      "Update a fee. Body: name, fee, ffee, feeType, departmentId, discountAllowed (all optional)"},
                    {"DELETE", "/pricing/collecting_centre_fees/{feeId}",                      "Soft-retire a single fee. Optional query param: retireComments"},
                    {"DELETE", "/pricing/collecting_centre_fees?institutionId=X",              "Retire ALL active fees for a collecting centre. Optional query param: retireComments"},
                    {"POST",   "/pricing/collecting_centre_fees/recalculate?institutionId=X",  "Recalculate item totals for all items with fees for this centre"}
                });

        // ── Inward / Admissions ───────────────────────────────────────────────
        appendModule(sb, "Inward / Admissions", "/apiInward",
                "Access inpatient admission records and process payments for admitted patients.",
                githubUrl(branch, "developer_docs/API_INWARD.md"),
                new String[][]{
                    {"GET",  "/apiInward/admissions",                                            "List active inpatient admissions"},
                    {"GET",  "/apiInward/admissions/byPhone/{phone}",                            "Find admission by patient phone number"},
                    {"GET",  "/apiInward/banks",                                                  "List available banks/payment institutions"},
                    {"GET",  "/apiInward/validateAdmission/{bht_no}/{phone}",                    "Validate BHT number and phone before payment"},
                    {"POST", "/apiInward/payment",                                                "Process online settlement payment for admitted patient (fields: bht_no, bank_id, reference_no, amount, payment_date)"},
                    {"GET",  "/apiInward/payment/{bht_no}/{bank_id}/{credit_card_ref}/{amount}", "Legacy GET-based payment endpoint"}
                });

        // ── Inward Discount Matrix ────────────────────────────────────────────
        appendModule(sb, "Inward Discount Matrix", "/inward-discount-matrix",
                "Manage inward discount matrix entries (backs the two UI pages "
                + "inward_discount_matrix_service_investigation.xhtml and inward_discount_matrix_pharmacy.xhtml). "
                + "Use scope=service or scope=pharmacy to choose the category universe. "
                + "POST rejects duplicates with 409 + existing id. "
                + "Lookup sub-paths resolve names to IDs.",
                null,
                new String[][]{
                    {"GET",    "/inward-discount-matrix?scope=X",                               "List entries. Filters: scope, departmentId, categoryId, admissionTypeId, paymentSchemeId, paymentMethod, limit"},
                    {"GET",    "/inward-discount-matrix/{id}",                                   "Fetch one entry"},
                    {"POST",   "/inward-discount-matrix",                                         "Create. Body: scope (required), paymentSchemeId (required), discountPercent (required), departmentId, categoryId, admissionTypeId, paymentMethod"},
                    {"PUT",    "/inward-discount-matrix/{id}",                                   "Update. Body fields all optional; send null to clear a field"},
                    {"DELETE", "/inward-discount-matrix/{id}",                                   "Soft-retire entry. Optional: retireComments"},
                    {"GET",    "/inward-discount-matrix/admission-types/search?query=",          "AdmissionType name → id lookup"},
                    {"GET",    "/inward-discount-matrix/payment-schemes/search?query=",           "PaymentScheme name → id lookup"},
                    {"GET",    "/inward-discount-matrix/pharmaceutical-item-categories/search?query=", "PharmaceuticalItemCategory name → id lookup"},
                    {"GET",    "/inward-discount-matrix/payment-methods",                         "List PaymentMethod enum values (Cash, Credit, Card, ...)"}
                });

        appendModule(sb, "Inward Room Management", "/inward/room-categories, /inward/rooms, /inward/room-facility-charges",
                "Manage inward room master data: room categories, rooms, and room facility charges (fee configurations). "
                + "POST returns 409 with existing id when a duplicate name exists.",
                githubUrl(branch, "developer_docs/API_INWARD_ROOM.md"),
                new String[][]{
                    {"GET",    "/inward/room-categories",          "List room categories. Filters: query, size"},
                    {"GET",    "/inward/room-categories/{id}",     "Fetch one room category"},
                    {"POST",   "/inward/room-categories",          "Create room category. Body: name (required), code, description"},
                    {"PUT",    "/inward/room-categories/{id}",     "Update room category"},
                    {"DELETE", "/inward/room-categories/{id}",     "Soft-retire room category"},
                    {"GET",    "/inward/rooms",                    "List rooms. Filters: query, roomCategoryId, size"},
                    {"GET",    "/inward/rooms/{id}",               "Fetch one room"},
                    {"POST",   "/inward/rooms",                    "Create room. Body: name (required), code, description, roomCategoryId, filled"},
                    {"PUT",    "/inward/rooms/{id}",               "Update room"},
                    {"DELETE", "/inward/rooms/{id}",               "Soft-retire room"},
                    {"GET",    "/inward/room-facility-charges",    "List room facility charges. Filters: query, roomId, roomCategoryId, size"},
                    {"GET",    "/inward/room-facility-charges/{id}", "Fetch one room facility charge"},
                    {"POST",   "/inward/room-facility-charges",    "Create room facility charge. Body: name (required), roomId, roomCategoryId, departmentId, charge fields, timedItemFee fields"},
                    {"PUT",    "/inward/room-facility-charges/{id}", "Update room facility charge"},
                    {"DELETE", "/inward/room-facility-charges/{id}", "Soft-retire room facility charge"}
                });

        // ── Login History / Config ────────────────────────────────────────────
        appendModule(sb, "Login History", "/logins",
                "Query user login history filtered by department, user, and date range.",
                githubUrl(branch, "developer_docs/API_LOGIN_HISTORY.md"),
                new String[][]{
                    {"GET", "/logins",               "List logins. Filters: departmentId, userId, days, fromDate (yyyy-MM-dd), toDate, page, size"},
                    {"GET", "/logins/last-per-user", "Most recent login per unique user. Filters: departmentId, size"}
                });

        appendModule(sb, "System Configuration", "/config",
                "Search and set application configuration options at runtime. "
                + "IMPORTANT: Uses the 'Config' header for authentication, not 'Finance'.",
                githubUrl(branch, "developer_docs/API_CONFIG.md"),
                new String[][]{
                    {"GET",  "/config/search?keyword={keyword}", "Search config options by keyword (returns key, type, current value)"},
                    {"POST", "/config/setBoolean/{key}/{value}",  "Set a boolean config option by key name"},
                    {"POST", "/config/setLongText/{key}/{value}", "Set a text config option by key name"},
                    {"POST", "/config/setInteger/{key}/{value}",  "Set an integer config option by key name"}
                });

        sb.append("## Your Capabilities\n");
        sb.append("- Search the live codebase and configuration to answer questions grounded in actual system behaviour\n");
        sb.append("- Query and search HMIS data via REST API calls\n");
        sb.append("- Adjust stock, pharmacy, and financial data\n");
        sb.append("- Create and update consultant/doctor records\n");
        sb.append("- Manage users, roles, and system privileges\n");
        sb.append("- Create and manage appointment bookings\n");
        sb.append("- Access inpatient admission records and process payments\n");
        sb.append("- Query login history and audit trails\n");
        sb.append("- Analyse reports and uploaded images/documents, including medicine lists\n");
        sb.append("- Troubleshoot and explain system behaviour using the actual source code\n\n");
        sb.append("When making API calls, always explain what you are doing and present results clearly. ");
        sb.append("When answering questions about system behaviour, use the tools to search the actual source code and configuration rather than guessing.\n");

        return sb.toString();
    }

    /**
     * Fetches the full documentation content for a specific module.
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

    // -------------------------------------------------------------------------
    // Response wrapper
    // -------------------------------------------------------------------------

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
