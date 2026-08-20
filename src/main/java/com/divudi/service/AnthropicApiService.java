package com.divudi.service;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.OptionValueType;
import com.divudi.core.entity.AiMessage;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.ConfigOption;
import com.divudi.core.facade.ApiKeyFacade;
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
import javax.inject.Inject;
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

    @EJB
    private ApiKeyFacade apiKeyFacade;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

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

        JsonObject manageConfigOptionTool = Json.createObjectBuilder()
                .add("name", "manage_config_option")
                .add("description",
                        "Read or update a single HMIS application configuration option by its exact key. "
                        + "Use GET to read the current value; use PUT to update it (flushes in-memory cache immediately). "
                        + "The option must already exist — this tool does not create new keys. "
                        + "Sensitive values (API keys, passwords, tokens) are masked on reads. "
                        + "Use search_config_options first if you need to discover the exact key name.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder().add("GET").add("PUT"))
                                        .add("description", "HTTP method: GET to read, PUT to update"))
                                .add("key", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Exact config option key (case-sensitive)"))
                                .add("value", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "New value (required for PUT)")))
                        .add("required", Json.createArrayBuilder().add("method").add("key")))
                .build();

        JsonObject admissionNumberTool = Json.createObjectBuilder()
                .add("name", "manage_admission_number_counter")
                .add("description",
                        "View or reset the BHT/OPD-card admission-number sequence counter for an admission type. "
                        + "Use GET to check the current counter and next number. Use PUT only when staff have manually "
                        + "corrected a printed BHT/OPD-card number and confirmed what the next number should be. "
                        + "This resets a live, shared numbering sequence used by all staff admitting patients under "
                        + "this admission type — before calling PUT, always state the current last/next number (from "
                        + "GET) and the requested new last/next number back to the user, and wait for their explicit "
                        + "confirmation in the same conversation. Never call PUT speculatively or without that confirmation.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder().add("GET").add("PUT"))
                                        .add("description", "HTTP method: GET to view, PUT to reset"))
                                .add("admissionTypeId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Numeric AdmissionType ID. Required."))
                                .add("institutionId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Numeric Institution ID (optional, only relevant if institution-based numbering is enabled)."))
                                .add("lastAdmissionNumber", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "The corrected last-used number. Required for PUT; the next generated number will be this + 1."))
                                .add("expectedLastAdmissionNumber", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Required for PUT. The lastAdmissionNumber value most recently observed via GET — used as a compare-and-set precondition so the reset is rejected (409) if the counter changed since it was read.")))
                        .add("required", Json.createArrayBuilder().add("method").add("admissionTypeId")))
                .build();

        JsonObject admissionSearchTool = Json.createObjectBuilder()
                .add("name", "search_admissions")
                .add("description",
                        "Search or list hospital admissions. Unlike the inward payment worklist, this is "
                        + "not scoped to unpaid/open admissions — it can list all currently active "
                        + "(not-discharged) admissions, or find a patient's past or current admissions by "
                        + "BHT number, name, MRN/PHN, phone, or NIC. All parameters are optional; omitting "
                        + "status defaults to currently-admitted (not-discharged) patients only.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("status", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder()
                                                .add("ADMITTED_BUT_NOT_DISCHARGED")
                                                .add("DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED")
                                                .add("DISCHARGED_AND_FINAL_BILL_COMPLETED")
                                                .add("ANY_STATUS"))
                                        .add("description", "Admission status filter. Default ADMITTED_BUT_NOT_DISCHARGED (currently active patients). Use ANY_STATUS to search past admissions too."))
                                .add("bhtNo", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Bed Head Ticket number (partial match)."))
                                .add("patientName", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Patient name (partial match)."))
                                .add("mrn", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Patient MRN/PHN or patient code (exact match)."))
                                .add("phone", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Patient or guardian phone/mobile number (exact match)."))
                                .add("nic", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Patient NIC/passport number (exact match)."))
                                .add("admissionTypeId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Numeric AdmissionType ID filter."))
                                .add("institutionId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Numeric Institution ID filter."))
                                .add("departmentId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Numeric Department ID filter."))
                                .add("fromDate", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Admission date range start, format yyyy-MM-dd HH:mm:ss. Must be supplied together with toDate."))
                                .add("toDate", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Admission date range end, format yyyy-MM-dd HH:mm:ss. Must be supplied together with fromDate."))
                                .add("page", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Page number, default 1."))
                                .add("size", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Page size, default 50, max 200.")))
                        .add("required", Json.createArrayBuilder()))
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

        JsonObject itemRequestTool = Json.createObjectBuilder()
                .add("name", "manage_item_requests")
                .add("description",
                        "Submit, look up, list, or cancel item/service requests against a patient's active BHT "
                        + "(meals like Breakfast/Lunch/Dinner, and stock items like Water Bottle/Tea/Milk/Sugar). "
                        + "Use POST to submit a new request. Use GET with id to poll a single request's status "
                        + "(PENDING, APPROVED, REJECTED, CANCELLED). Use GET without id to list/search requests. "
                        + "Use PUT to cancel a still-PENDING request. Approval/rejection happen only in-app via the "
                        + "department's JSF approval queue and are NOT available through this tool.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder().add("GET").add("POST").add("PUT"))
                                        .add("description", "HTTP method: GET=fetch/list, POST=submit new request, PUT=cancel"))
                                .add("id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Request ID as a string. Required for GET-single and PUT (cancel)."))
                                .add("bhtNo", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Patient's active BHT number. Required for POST."))
                                .add("targetDepartmentId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Department ID the request is routed to for approval. Required for POST; optional filter for GET-list."))
                                .add("comments", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Optional comments for the request (POST)."))
                                .add("linesJson", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "JSON array of request lines for POST, e.g. [{\"itemId\":123,\"qty\":2}]"))
                                .add("reason", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Reason for cancellation. Used with PUT."))
                                .add("status", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Filter GET-list by status: PENDING, APPROVED, REJECTED, CANCELLED (optional)."))
                                .add("fromDate", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Filter GET-list from date, format yyyy-MM-dd (optional)."))
                                .add("toDate", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Filter GET-list to date, format yyyy-MM-dd (optional)."))
                                .add("limit", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Max rows to return for GET-list (optional).")))
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
                        "Manage Inward Discount Matrix entries (backs the three UI pages "
                        + "inward_discount_matrix_service_investigation.xhtml, inward_discount_matrix_pharmacy.xhtml, "
                        + "and inward_discount_matrix_room_charges.xhtml). "
                        + "Methods: LIST (filter+list), GET (one), POST (create — rejects duplicates), "
                        + "PUT (update), DELETE (soft-retire). "
                        + "Optional creditCompanyId sets a credit-company-specific discount row; rows without creditCompanyId "
                        + "are the generic fallback used when no CC-specific row matches. "
                        + "Lookup helpers: LOOKUP_DEPARTMENTS, LOOKUP_SERVICE_CATEGORIES, "
                        + "LOOKUP_PHARMACEUTICAL_ITEM_CATEGORIES, LOOKUP_ADMISSION_TYPES, "
                        + "LOOKUP_PAYMENT_SCHEMES, LIST_PAYMENT_METHODS, LOOKUP_CREDIT_COMPANIES. "
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
                                                .add("LIST_PAYMENT_METHODS")
                                                .add("LOOKUP_CREDIT_COMPANIES"))
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
                                .add("creditCompanyId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Institution id of the credit company. Optional. When set, this row applies only when the admission has exactly that one credit company."))
                                .add("retireComments", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Reason for retirement. Optional for DELETE.")))
                        .add("required", Json.createArrayBuilder().add("method")))
                .build();

        JsonObject inwardPriceAdjustmentTool = Json.createObjectBuilder()
                .add("name", "manage_inward_price_adjustment")
                .add("description",
                        "Manage Inward Price Adjustment (margin/service charge) matrix entries for services, investigations, and pharmacy. "
                        + "Each row maps a gross-value price range (fromPrice, toPrice) to a margin %. "
                        + "Methods: LIST, GET, POST (create), PUT (update), DELETE (soft-retire). "
                        + "Optional creditCompanyId creates a CC-specific row; rows without creditCompanyId are the generic fallback. "
                        + "Lookup helpers: LOOKUP_DEPARTMENTS, LOOKUP_CATEGORIES, LIST_PAYMENT_METHODS, LOOKUP_CREDIT_COMPANIES. "
                        + "Always resolve names to IDs via lookups before POST/PUT.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder()
                                                .add("LIST").add("GET").add("POST").add("PUT").add("DELETE")
                                                .add("LOOKUP_DEPARTMENTS").add("LOOKUP_CATEGORIES")
                                                .add("LIST_PAYMENT_METHODS").add("LOOKUP_CREDIT_COMPANIES"))
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
                                        .add("description", "Department id. Optional."))
                                .add("categoryId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Category id. Optional."))
                                .add("paymentMethod", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "PaymentMethod enum name, e.g. Cash, Credit. Optional."))
                                .add("fromPrice", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Lower bound of the gross value range. Required for POST."))
                                .add("toPrice", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Upper bound of the gross value range. Required for POST."))
                                .add("margin", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Margin percentage to apply. Required for POST."))
                                .add("creditCompanyId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Institution id of the credit company. Optional."))
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

        JsonObject priceMatrixInwardTool = Json.createObjectBuilder()
                .add("name", "manage_price_matrix_inward")
                .add("description",
                        "Manage Inward Price Matrix (InwardPriceAdjustment) entries with flat DTO format. "
                        + "Each row maps a price range (fromPrice, toPrice) to a margin % and optional discount %. "
                        + "Methods: LIST, GET, POST (create), PUT (partial update), DELETE (soft-retire). "
                        + "All create/update/retire actions are audit-logged. "
                        + "POST returns HTTP 409 with existing id when a duplicate combination exists. "
                        + "Required for POST: departmentId, categoryId, margin.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder()
                                                .add("LIST").add("GET").add("POST").add("PUT").add("DELETE"))
                                        .add("description", "Operation to perform."))
                                .add("id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Entry id. Required for GET, PUT, DELETE."))
                                .add("departmentId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Department id. Required for POST. Optional filter for LIST."))
                                .add("categoryId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Category id. Required for POST. Optional filter for LIST."))
                                .add("paymentMethod", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "PaymentMethod enum name, e.g. Cash, Credit. Optional."))
                                .add("margin", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Margin percentage. Required for POST."))
                                .add("discountPercent", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Discount percentage (default 0). Optional."))
                                .add("fromPrice", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Lower bound of the gross value range (default 0). Optional."))
                                .add("toPrice", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Upper bound of the gross value range (default 9999999999). Optional."))
                                .add("admissionTypeId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Admission type id. Optional."))
                                .add("creditCompanyId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Credit company institution id. Optional."))
                                .add("limit", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Max results (1-1000, default 50). Optional for LIST."))
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
                        + "LIST_CHARGES, POST_CHARGE, PUT_CHARGE, DELETE_CHARGE, "
                        + "LIST_TIMED_ITEMS, ADD_TIMED_ITEM, REMOVE_TIMED_ITEM (attach/detach TimedItem services that "
                        + "auto-bill by duration of stay alongside a room facility charge's fixed fees; id = the "
                        + "room facility charge id, timedItemId = the TimedItem to attach for ADD_TIMED_ITEM, "
                        + "id/linkId identify the attachment to remove for REMOVE_TIMED_ITEM). "
                        + "Always confirm with the user before creating, updating, or retiring records.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder()
                                                .add("LIST_CATEGORIES").add("GET_CATEGORY").add("POST_CATEGORY").add("PUT_CATEGORY").add("DELETE_CATEGORY")
                                                .add("LIST_ROOMS").add("GET_ROOM").add("POST_ROOM").add("PUT_ROOM").add("DELETE_ROOM")
                                                .add("LIST_CHARGES").add("GET_CHARGE").add("POST_CHARGE").add("PUT_CHARGE").add("DELETE_CHARGE")
                                                .add("LIST_TIMED_ITEMS").add("ADD_TIMED_ITEM").add("REMOVE_TIMED_ITEM"))
                                        .add("description", "Operation to perform."))
                                .add("id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Record id. Required for PUT and DELETE methods. "
                                                + "For LIST_TIMED_ITEMS/ADD_TIMED_ITEM/REMOVE_TIMED_ITEM this is the room facility charge id."))
                                .add("timedItemId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "TimedItem id to attach. Required for ADD_TIMED_ITEM."))
                                .add("linkId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "RoomFacilityTimedItem attachment id to remove. Required for REMOVE_TIMED_ITEM."))
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
                                .add("svgChildView", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Bed-board child-tile SVG markup for the room (issue #21592). Optional for POST_ROOM/PUT_ROOM. A Room is a leaf in the bed-board hierarchy, so it has only this child view (no parent canvas). Or use the dedicated manage_bed_board_svg tool."))
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

        JsonObject bedBoardSvgTool = Json.createObjectBuilder()
                .add("name", "manage_bed_board_svg")
                .add("description",
                        "Read and set the graphical bed-board SVG drawings (issue #21592) used by the "
                        + "Inpatient Bed Board page. Each entity stores drawings on a shared "
                        + "viewBox=\"0 0 1000 600\" grid: svgParentView is the entity's own empty floor-plan "
                        + "canvas (shown when you navigate into it), and svgChildView is the small shape "
                        + "showing how this entity looks as a tile inside its parent's canvas. "
                        + "Sites, institutions, and departments have both views; a room (leaf) has only svgChildView. "
                        + "Methods: GET_SITE, SET_SITE, GET_INSTITUTION, SET_INSTITUTION, GET_DEPARTMENT, SET_DEPARTMENT, GET_ROOM, SET_ROOM. "
                        + "On SET, only the fields you supply are changed; pass an empty string to clear a drawing. "
                        + "SVG is stored verbatim and sanitised when the bed board renders it. "
                        + "Authoring guidance (viewBox, copy-paste examples, draw-your-own primer) is on the "
                        + "wiki page 'Inpatient — Bed Board'. Always confirm with the user before SET.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder()
                                                .add("GET_SITE").add("SET_SITE")
                                                .add("GET_INSTITUTION").add("SET_INSTITUTION")
                                                .add("GET_DEPARTMENT").add("SET_DEPARTMENT")
                                                .add("GET_ROOM").add("SET_ROOM"))
                                        .add("description", "Operation to perform. SITE targets /api/sites, INSTITUTION targets /api/institutions, DEPARTMENT targets /api/departments, ROOM targets /api/inward/rooms."))
                                .add("id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Entity id (site/department/room). Required for all methods."))
                                .add("svgParentView", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Parent-canvas SVG markup. Used by SET_SITE/SET_DEPARTMENT. Ignored for rooms (a room has no parent view). Pass an empty string to clear."))
                                .add("svgChildView", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Child-tile SVG markup. Used by SET_SITE/SET_DEPARTMENT/SET_ROOM. Pass an empty string to clear.")))
                        .add("required", Json.createArrayBuilder().add("method").add("id")))
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
                                        .add("description", "'true' to skip sample collection and allow direct result entry after billing. Optional."))
                                .add("vatable", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "'true' to charge VAT on this investigation, 'false' to exempt it. Optional."))
                                .add("vatPercentage", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "VAT percentage applied when vatable is true (e.g. '18'). Optional."))
                                .add("categoryId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Investigation category ID (e.g. Haematology, Biochemistry). Must reference an existing category or an error is thrown. Optional for POST/PUT — alternative to categoryName."))
                                .add("categoryName", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Investigation category name. Found-or-created by name if no matching category exists. Optional for POST/PUT — alternative to categoryId."))
                                .add("sampleId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Sample type ID (e.g. Blood, Urine). Must reference an existing sample or an error is thrown. Optional for POST/PUT — alternative to sampleName."))
                                .add("sampleName", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Sample type name. Found-or-created by name if no matching sample exists. Optional for POST/PUT — alternative to sampleId."))
                                .add("containerId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Collection container/tube ID (e.g. EDTA Tube, Citrate Tube). Must reference an existing container or an error is thrown. Optional for POST/PUT — alternative to containerName."))
                                .add("containerName", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Collection container/tube name. Found-or-created by name if no matching container exists. Optional for POST/PUT — alternative to containerId."))
                                .add("analyzerId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Analyzer/machine ID (e.g. Sysmex XN-1000). Must reference an existing analyzer or an error is thrown. Optional for POST/PUT — alternative to analyzerName."))
                                .add("analyzerName", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Analyzer/machine name. Found-or-created by name if no matching analyzer exists. Optional for POST/PUT — alternative to analyzerId.")))
                        .add("required", Json.createArrayBuilder().add("method")))
                .build();

        JsonObject manageServicesTool = Json.createObjectBuilder()
                .add("name", "manage_services")
                .add("description",
                        "Search, retrieve, create, update, activate, or deactivate service master records "
                        + "(billable OPD or Inward services, e.g. consultations, procedures, room charges). "
                        + "Use GET to search by name/code/printName, optionally filtered by serviceType or categoryId. "
                        + "Use POST to create a new service (returns already_exists with the existing id if a "
                        + "duplicate name is found). Use PUT to update metadata. "
                        + "Use ACTIVATE/DEACTIVATE to toggle the inactive flag.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Operation: GET=search, GET_BY_ID=fetch one, POST=create, PUT=update, ACTIVATE=set inactive=false, DEACTIVATE=set inactive=true. Required."))
                                .add("id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Service ID. Required for GET_BY_ID, PUT, ACTIVATE, DEACTIVATE."))
                                .add("query", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Search text matched against name, code, and printName (case-insensitive). Used with GET."))
                                .add("serviceType", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "'OPD' or 'Inward'. Required for POST. Optional filter for GET."))
                                .add("categoryId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Filter by service category ID. Used with GET."))
                                .add("inactive", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Filter by active/inactive status: 'true' or 'false'. Omit to return both. Used with GET."))
                                .add("limit", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Max results to return (1–100). Defaults to 20. Used with GET."))
                                .add("name", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Service name. Required for POST; optional for PUT."))
                                .add("code", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Short code. Auto-generated from name if omitted on POST. Optional for PUT."))
                                .add("printName", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Print/display name shown on reports and bills. Optional."))
                                .add("fullName", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Full descriptive name. Optional."))
                                .add("inwardChargeType", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "InwardChargeType enum value. Required when serviceType=Inward on POST."))
                                .add("vatable", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "'true' to charge VAT on this service, 'false' to exempt it. Optional."))
                                .add("vatPercentage", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "VAT percentage applied when vatable is true (e.g. '18'). Optional.")))
                        .add("required", Json.createArrayBuilder().add("method")))
                .build();

        JsonObject manageInvestigationFormatTool = Json.createObjectBuilder()
                .add("name", "manage_investigation_format")
                .add("description",
                        "Manage the report format of an investigation: items (report fields), "
                        + "item values (dropdown options), calculations (formulas), "
                        + "flags (reference ranges), and dynamic labels (conditional text). "
                        + "First use manage_investigations GET to find the investigation ID, "
                        + "then use this tool with the investigation_id.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("resource_type", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Which sub-resource: ITEM, VALUE, CALCULATION, FLAG, DYNAMIC_LABEL. Required."))
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "ITEM supports LIST/GET/POST/PUT/DELETE. VALUE, CALCULATION, FLAG, DYNAMIC_LABEL support LIST/POST/PUT/DELETE (no GET by id). Required."))
                                .add("investigation_id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "ID of the parent investigation. Required for all operations."))
                                .add("item_id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Investigation item ID. Required for GET/PUT/DELETE on ITEM; required for VALUE LIST/POST."))
                                .add("id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Record ID for PUT/DELETE on VALUE, CALCULATION, FLAG, DYNAMIC_LABEL."))
                                .add("name", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Name. Required for ITEM POST and VALUE POST."))
                                .add("code", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Short code. Auto-generated from name if omitted."))
                                .add("description", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Description text for items."))
                                .add("order_no", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Display order number."))
                                .add("ix_item_type", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "InvestigationItemType: Label, Value, Calculation, Flag, DynamicLabel, Css, Barcode, Html, MeasurementUnit, Image. Required for ITEM POST."))
                                .add("ix_item_value_type", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "InvestigationItemValueType: Varchar, Memo, Double, Integer, Long, List. Required for Value-type items."))
                                .add("automated", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "'true' if value comes from an automated analyzer."))
                                .add("result_code", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Result code for analyzer mapping."))
                                .add("format_prefix", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Prefix displayed before the value."))
                                .add("format_suffix", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Suffix displayed after the value (e.g. unit)."))
                                .add("htmltext", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "HTML content for Label/Html type items."))
                                .add("can_not_approve_if_empty", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "'true' to block approval if value is empty."))
                                .add("absolute_low_value", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Absolute minimum acceptable value."))
                                .add("absolute_high_value", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Absolute maximum acceptable value."))
                                .add("cal_ix_item_id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "CALCULATION: ID of the calculated (target) item."))
                                .add("val_ix_item_id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "CALCULATION: ID of the source value item (when calculationType=Value)."))
                                .add("calculation_type", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "CalculationType: Value, Constant, Addition, Substraction, Multiplication, Devision, Power, OpeningBracket, ClosingBracket, AgeInMonths, AgeInYears, AgeInDays, GenderDependentConstant, JavaScript."))
                                .add("constant_value", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Constant value for Constant type."))
                                .add("male_constant_value", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Male constant for GenderDependentConstant."))
                                .add("female_constant_value", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Female constant for GenderDependentConstant."))
                                .add("javascript", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "JavaScript expression for JavaScript calculation type."))
                                .add("investigation_item_of_value_type_id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "FLAG: ID of the value item to monitor."))
                                .add("investigation_item_of_flag_type_id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "FLAG: ID of the flag display item."))
                                .add("investigation_item_of_label_type_id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "DYNAMIC_LABEL: ID of the label item."))
                                .add("sex", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Sex filter: Male, Female, or omit for both. FLAG/DYNAMIC_LABEL."))
                                .add("from_age", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Minimum age in days. FLAG/DYNAMIC_LABEL."))
                                .add("to_age", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Maximum age in days. FLAG/DYNAMIC_LABEL."))
                                .add("from_val", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "FLAG: Lower bound of value range."))
                                .add("to_val", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "FLAG: Upper bound of value range."))
                                .add("flag_message", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Flag/label message text."))
                                .add("high_message", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "FLAG: Message when value is above range."))
                                .add("low_message", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "FLAG: Message when value is below range."))
                                .add("normal_message", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "FLAG: Message when value is within range."))
                                .add("display_flag_message", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "FLAG: 'true' to display the flag message."))
                                .add("display_high_message", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "FLAG: 'true' to display the high message."))
                                .add("display_low_message", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "FLAG: 'true' to display the low message."))
                                .add("display_normal_message", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "FLAG: 'true' to display the normal message.")))
                        .add("required", Json.createArrayBuilder()
                                .add("resource_type").add("method").add("investigation_id")))
                .build();

        JsonObject manageInvestigationComponentsTool = Json.createObjectBuilder()
                .add("name", "manage_investigation_components")
                .add("description",
                        "Manage InvestigationComponent groupings used to organize report items within an investigation's "
                        + "format (e.g. grouping FBC items under a 'White Cell Differential' heading). "
                        + "First use manage_investigations GET to find the investigation ID, then use this tool. "
                        + "method: LIST | POST | PUT | DELETE. PUT and DELETE require component_id; POST and PUT require component_name. "
                        + "DELETE permanently removes the component and is rejected if any report item still references it. "
                        + "Always confirm with the user before POST, PUT, or DELETE.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder().add("LIST").add("POST").add("PUT").add("DELETE"))
                                        .add("description", "Operation to perform."))
                                .add("investigation_id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Investigation ID. Required for all methods."))
                                .add("component_id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Component ID. Required for PUT and DELETE."))
                                .add("component_name", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Component name/label. Required for POST and PUT.")))
                        .add("required", Json.createArrayBuilder().add("method").add("investigation_id")))
                .build();

        JsonObject manageInvestigationPricingTool = Json.createObjectBuilder()
                .add("name", "manage_investigation_pricing")
                .add("description",
                        "Manage investigation pricing (ItemFee) — the fees charged when an investigation is billed. "
                        + "First use manage_investigations GET to find the investigation ID, then use this tool. "
                        + "method: LIST | POST | PUT | DELETE. PUT and DELETE require fee_id. POST requires name, feeType, and fee. "
                        + "DELETE soft-deletes (retires) a fee. All mutations recalculate the investigation's total and are "
                        + "rejected against a retired investigation. Always confirm with the user before POST, PUT, or DELETE "
                        + "— these changes affect live billing.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder().add("LIST").add("POST").add("PUT").add("DELETE"))
                                        .add("description", "Operation to perform."))
                                .add("investigation_id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Investigation ID. Required for all methods."))
                                .add("fee_id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Fee ID. Required for PUT and DELETE."))
                                .add("name", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Fee name (e.g. 'Hospital Fee', 'Professional Fee'). Required for POST."))
                                .add("feeType", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "ItemFeeType enum value (e.g. OwnInstitution, Referral, Professional). Required for POST."))
                                .add("fee", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Fee amount. Required for POST."))
                                .add("ffee", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Foreigner fee amount. Optional; defaults to fee if omitted."))
                                .add("discountAllowed", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "'true' or 'false' — whether discounts can be applied to this fee. Optional."))
                                .add("institutionId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Institution ID this fee applies to. Optional."))
                                .add("departmentId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Department ID this fee applies to. Optional."))
                                .add("specialityId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Speciality ID this fee applies to. Optional."))
                                .add("staffId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Staff ID this fee applies to (e.g. for Professional fees). Optional.")))
                        .add("required", Json.createArrayBuilder().add("method").add("investigation_id")))
                .build();

        JsonObject manageInvestigationValidatorsTool = Json.createObjectBuilder()
                .add("name", "manage_investigation_validators")
                .add("description",
                        "Manage InvestigationValidator result-range checks (min/max value validation) for an investigation. "
                        + "First use manage_investigations GET to find the investigation ID, then use this tool. "
                        + "method: LIST | POST | PUT | DELETE. PUT and DELETE require validator_id; POST requires name. "
                        + "minimumValue and maximumValue are optional but minimumValue cannot exceed maximumValue. "
                        + "DELETE soft-deletes (retires) a validator. Always confirm with the user before POST, PUT, or DELETE.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder().add("LIST").add("POST").add("PUT").add("DELETE"))
                                        .add("description", "Operation to perform."))
                                .add("investigation_id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Investigation ID. Required for all methods."))
                                .add("validator_id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Validator ID. Required for PUT and DELETE."))
                                .add("name", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Validator name. Required for POST."))
                                .add("maximumValue", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Maximum acceptable result value. Optional."))
                                .add("minimumValue", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Minimum acceptable result value. Optional.")))
                        .add("required", Json.createArrayBuilder().add("method").add("investigation_id")))
                .build();

        JsonObject manageInvestigationExportTool = Json.createObjectBuilder()
                .add("name", "manage_investigation_export")
                .add("description",
                        "Retrieve an investigation's complete definition as one nested document: metadata "
                        + "(incl. category/sample/container/analyzer), components, report format (items, item values, "
                        + "calculations, flags, dynamic labels), validators, and fees. Use this to review everything "
                        + "configured for an investigation in a single call — e.g. to confirm a newly-built test is "
                        + "complete, or as a reference when building a similar investigation. Read-only.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("investigation_id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Investigation ID. Required.")))
                        .add("required", Json.createArrayBuilder().add("investigation_id")))
                .build();

        JsonObject manageFormsTool = Json.createObjectBuilder()
                .add("name", "manage_forms")
                .add("description",
                        "Design and manage dynamic clinical form templates (create/update/retire), "
                        + "fields of all input types (text, number, date, calendar, signature, choice lists, boolean, rating, slider, spinner), "
                        + "per-field choice options, and AI-generated HTML layout wrappers (editHtml/viewHtml) for custom C3 hybrid layout. "
                        + "Also query filled form entries and their captured values for a given admission.\n\n"
                        + "resource_type: TEMPLATE | FIELD | CHOICE | ENTRY | VALUE\n"
                        + "method: LIST | GET | POST | PUT | DELETE\n\n"
                        + "TEMPLATE: LIST returns all non-retired forms. GET requires id. POST requires name. PUT requires id. DELETE requires id.\n"
                        + "FIELD: LIST requires form_id. POST requires form_id + name + componentPresentationType. PUT requires id. DELETE requires id.\n"
                        + "  componentPresentationType values: Input_text, Input_text_Area, TextEditor, Input_Number, Spinner, Slider, Rating, Calendar,\n"
                        + "  SelectBooleanCheckBox, SelectBooleanButton, ToggleSwitch, TriStateCheckBox, SelectOneMenu, SelectOneRadio,\n"
                        + "  SelectOneListBox, SelectCheckBoxMenu, SelectManyButton, MultiSelectListBox, AutoComplete, Signature\n"
                        + "  editHtml: wrap the {{INPUT}} placeholder with Bootstrap 5 HTML. {{LABEL}} is the field label.\n"
                        + "  viewHtml: wrap {{LABEL}} and {{VALUE}} for the read-only view.\n"
                        + "CHOICE: LIST requires field_id. POST requires field_id + label. PUT requires id. DELETE requires id.\n"
                        + "ENTRY: LIST requires admission_id. Returns PatientFormEntry records for the admission.\n"
                        + "VALUE: LIST requires entry_id. Returns CaptureComponent values for a filled entry.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("resource_type", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder().add("TEMPLATE").add("FIELD").add("CHOICE").add("ENTRY").add("VALUE"))
                                        .add("description", "Which sub-resource to operate on"))
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder().add("LIST").add("GET").add("POST").add("PUT").add("DELETE"))
                                        .add("description", "CRUD operation"))
                                .add("id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Record ID (string) — required for GET/PUT/DELETE on templates, fields, and choices"))
                                .add("form_id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Form template ID — required when listing or adding fields"))
                                .add("field_id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Field ID — required when listing or adding choices"))
                                .add("admission_id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "PatientEncounter ID — required for ENTRY LIST"))
                                .add("entry_id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "PatientFormEntry ID — required for VALUE LIST"))
                                .add("name", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Display name for a template, field, or choice label"))
                                .add("description", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Description text (optional)"))
                                .add("formCssClass", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Bootstrap CSS class for the form row wrapper (e.g. 'row row-cols-1 row-cols-md-3 g-3')"))
                                .add("componentPresentationType", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Input widget type for a field (e.g. Input_text, Calendar, SelectOneMenu, Signature, etc.)"))
                                .add("componentDataType", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Data type for a field (optional)"))
                                .add("orderNo", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Display order number (integer as string)"))
                                .add("required", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "'true' or 'false' — whether the field is mandatory"))
                                .add("placeholder", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Placeholder text for text input fields"))
                                .add("minValue", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Minimum value for numeric/range fields"))
                                .add("maxValue", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Maximum value for numeric/range fields"))
                                .add("stepSize", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Step increment for Slider/Spinner fields"))
                                .add("maxRating", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Maximum star count for Rating fields"))
                                .add("onLabel", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Label when SelectBooleanButton is ON"))
                                .add("offLabel", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Label when SelectBooleanButton is OFF"))
                                .add("editHtml", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "HTML wrapper for edit mode. Use {{LABEL}} for the field label, {{INPUT}} where the PrimeFaces widget will be rendered. Use Bootstrap 5 col classes."))
                                .add("viewHtml", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "HTML wrapper for view mode. Use {{LABEL}} and {{VALUE}} tokens."))
                                .add("label", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Choice label shown to the user"))
                                .add("value", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Choice value stored (defaults to label if omitted)")))
                        .add("required", Json.createArrayBuilder().add("resource_type").add("method")))
                .build();

        JsonObject manageSubscriptionsTool = Json.createObjectBuilder()
                .add("name", "manage_subscriptions")
                .add("description",
                        "Manage notification trigger subscriptions: who receives which notification, in which department. "
                        + "A subscription links a user to a TriggerType for a department, or — when application-wide — for the whole "
                        + "application (matches every department; useful for hospital-wide roles such as a Guest Relations Officer).\n\n"
                        + "method: LIST | LIST_TRIGGER_TYPES | POST | DELETE\n\n"
                        + "LIST_TRIGGER_TYPES: returns all available TriggerType values (name, label, medium, parent). "
                        + "Call this first to discover valid triggerType names.\n"
                        + "LIST: list subscriptions; optional filters triggerType, userId, departmentId, applicationWide.\n"
                        + "POST: create a subscription. Requires userId, triggerType, and EITHER departmentId OR applicationWide=true. "
                        + "Returns already_exists when an identical non-retired subscription exists.\n"
                        + "DELETE: soft-retire the subscription with the given id.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder().add("LIST").add("LIST_TRIGGER_TYPES").add("POST").add("DELETE"))
                                        .add("description", "Operation to perform"))
                                .add("id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Subscription ID — required for DELETE"))
                                .add("triggerType", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "TriggerType enum name (e.g. INWARD_PATIENT_DISCHARGED). Required for POST; optional filter for LIST"))
                                .add("userId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "WebUser ID. Required for POST; optional filter for LIST"))
                                .add("departmentId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Department ID. For POST, provide this OR applicationWide (not both)"))
                                .add("applicationWide", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "'true' to make the subscription apply across every department (null department)")))
                        .add("required", Json.createArrayBuilder().add("method")))
                .build();

        JsonObject manageStaffTool = Json.createObjectBuilder()
                .add("name", "manage_staff")
                .add("description",
                        "CRUD for HMIS Staff records.\n\n"
                        + "method: LIST | GET | POST | PUT | DELETE | LINK_TO_USER\n\n"
                        + "LIST: search staff (query, departmentId, size).\n"
                        + "GET: get a single staff record by id.\n"
                        + "POST: create staff — required: name; optional: code, designation (string label), departmentId, institutionId. Creates linked Person automatically.\n"
                        + "PUT: partial update (name, code, designation, departmentId, institutionId — only supplied fields change).\n"
                        + "DELETE: soft-retire. Supply retireComments.\n"
                        + "LINK_TO_USER: link an existing Staff to a WebUser — requires id (userId) and staffId.\n\n"
                        + "Always confirm with the user before POST, PUT, DELETE, or LINK_TO_USER.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder().add("type", "string")
                                        .add("enum", Json.createArrayBuilder()
                                                .add("LIST").add("GET").add("POST").add("PUT").add("DELETE").add("LINK_TO_USER")))
                                .add("id", Json.createObjectBuilder().add("type", "string").add("description", "Staff ID (or User ID for LINK_TO_USER)"))
                                .add("staffId", Json.createObjectBuilder().add("type", "string").add("description", "Staff ID to link to a user (LINK_TO_USER only)"))
                                .add("query", Json.createObjectBuilder().add("type", "string").add("description", "Name or code search term"))
                                .add("departmentId", Json.createObjectBuilder().add("type", "string"))
                                .add("institutionId", Json.createObjectBuilder().add("type", "string"))
                                .add("size", Json.createObjectBuilder().add("type", "string"))
                                .add("name", Json.createObjectBuilder().add("type", "string").add("description", "Person name for the staff member"))
                                .add("code", Json.createObjectBuilder().add("type", "string"))
                                .add("designation", Json.createObjectBuilder().add("type", "string").add("description", "Free-text designation label"))
                                .add("retireComments", Json.createObjectBuilder().add("type", "string")))
                        .add("required", Json.createArrayBuilder().add("method")))
                .build();

        JsonObject manageUsersTool = Json.createObjectBuilder()
                .add("name", "manage_users")
                .add("description",
                        "Manage HMIS users, passwords, loggable departments, and department-scoped privileges.\n\n"
                        + "method: LIST | GET | POST | PUT | DELETE | RESET_PASSWORD | CHANGE_PASSWORD | "
                        + "FORCE_PASSWORD_RESET | PASSWORD_STATUS | "
                        + "LIST_PRIVILEGES | ASSIGN_PRIVILEGES | REVOKE_PRIVILEGE | LIST_DEPARTMENTS | ASSIGN_DEPARTMENTS | "
                        + "LIST_AVAILABLE_PRIVILEGES | BULK_ASSIGN_PRIVILEGES | ASSIGN_PRIVILEGE_CATEGORIES | "
                        + "ASSIGN_ALL_PRIVILEGES_MULTI_DEPT\n\n"
                        + "Privilege assignment requires departmentId; category assignment uses /users/{id}/departments/{departmentId}/privileges/category. "
                        + "ASSIGN_ALL_PRIVILEGES_MULTI_DEPT grants every privilege across supplied departmentIds (or all user's loggable depts if omitted). "
                        + "POST supports optional staffId to pre-link a Staff record at creation. "
                        + "Use LIST_AVAILABLE_PRIVILEGES before assigning explicit privilege names. "
                        + "FORCE_PASSWORD_RESET flags the account for a mandatory reset on next login without setting an actual new "
                        + "password (requires id only). PASSWORD_STATUS reports lastPasswordResetAt/needToResetPassword for every "
                        + "active user, optionally filtered by from/to (yyyy-MM-dd) — read-only, no id required. "
                        + "Always confirm with the user before POST, PUT, DELETE, RESET_PASSWORD, CHANGE_PASSWORD, FORCE_PASSWORD_RESET, "
                        + "ASSIGN_PRIVILEGES, REVOKE_PRIVILEGE, ASSIGN_DEPARTMENTS, BULK_ASSIGN_PRIVILEGES, "
                        + "ASSIGN_PRIVILEGE_CATEGORIES, or ASSIGN_ALL_PRIVILEGES_MULTI_DEPT.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder().add("type", "string")
                                        .add("enum", Json.createArrayBuilder()
                                                .add("LIST").add("GET").add("POST").add("PUT").add("DELETE")
                                                .add("RESET_PASSWORD").add("CHANGE_PASSWORD")
                                                .add("FORCE_PASSWORD_RESET").add("PASSWORD_STATUS")
                                                .add("LIST_PRIVILEGES").add("ASSIGN_PRIVILEGES").add("REVOKE_PRIVILEGE")
                                                .add("LIST_DEPARTMENTS").add("ASSIGN_DEPARTMENTS")
                                                .add("LIST_AVAILABLE_PRIVILEGES").add("BULK_ASSIGN_PRIVILEGES")
                                                .add("ASSIGN_PRIVILEGE_CATEGORIES")
                                                .add("ASSIGN_ALL_PRIVILEGES_MULTI_DEPT")))
                                .add("id", Json.createObjectBuilder().add("type", "string").add("description", "User ID for user-specific operations"))
                                .add("privilegeId", Json.createObjectBuilder().add("type", "string").add("description", "Privilege assignment ID for REVOKE_PRIVILEGE"))
                                .add("query", Json.createObjectBuilder().add("type", "string").add("description", "User list/search query"))
                                .add("page", Json.createObjectBuilder().add("type", "string").add("description", "LIST page number"))
                                .add("size", Json.createObjectBuilder().add("type", "string").add("description", "LIST page size"))
                                .add("name", Json.createObjectBuilder().add("type", "string"))
                                .add("code", Json.createObjectBuilder().add("type", "string"))
                                .add("email", Json.createObjectBuilder().add("type", "string"))
                                .add("telNo", Json.createObjectBuilder().add("type", "string"))
                                .add("personName", Json.createObjectBuilder().add("type", "string"))
                                .add("personMobile", Json.createObjectBuilder().add("type", "string"))
                                .add("institutionId", Json.createObjectBuilder().add("type", "string"))
                                .add("siteId", Json.createObjectBuilder().add("type", "string"))
                                .add("departmentId", Json.createObjectBuilder().add("type", "string"))
                                .add("roleId", Json.createObjectBuilder().add("type", "string"))
                                .add("activated", Json.createObjectBuilder().add("type", "string").add("description", "'true' or 'false'"))
                                .add("loginPage", Json.createObjectBuilder().add("type", "string").add("description", "LoginPage enum name"))
                                .add("password", Json.createObjectBuilder().add("type", "string").add("description", "Password for POST"))
                                .add("newPassword", Json.createObjectBuilder().add("type", "string").add("description", "New password for RESET_PASSWORD or CHANGE_PASSWORD"))
                                .add("currentPassword", Json.createObjectBuilder().add("type", "string").add("description", "Current password for own CHANGE_PASSWORD"))
                                .add("privileges", Json.createObjectBuilder().add("type", "string").add("description", "Comma-separated privilege enum names"))
                                .add("categories", Json.createObjectBuilder().add("type", "string").add("description", "Comma-separated Privileges.getCategory() names"))
                                .add("userIds", Json.createObjectBuilder().add("type", "string").add("description", "Comma-separated user IDs for BULK_ASSIGN_PRIVILEGES"))
                                .add("departmentIds", Json.createObjectBuilder().add("type", "string").add("description", "Comma-separated department IDs for ASSIGN_DEPARTMENTS or ASSIGN_ALL_PRIVILEGES_MULTI_DEPT"))
                                .add("staffId", Json.createObjectBuilder().add("type", "string").add("description", "Staff ID to link to the user on POST or via PUT /{id}/staff"))
                                .add("retireComments", Json.createObjectBuilder().add("type", "string"))
                                .add("from", Json.createObjectBuilder().add("type", "string").add("description", "PASSWORD_STATUS filter: lastPasswordResetAt on/after this date (yyyy-MM-dd)"))
                                .add("to", Json.createObjectBuilder().add("type", "string").add("description", "PASSWORD_STATUS filter: lastPasswordResetAt on/before this date (yyyy-MM-dd)")))
                        .add("required", Json.createArrayBuilder().add("method")))
                .build();

        JsonObject userRoleResetTool = Json.createObjectBuilder()
                .add("name", "user_role_reset")
                .add("description",
                        "Reset a user's role-template aspects (privileges/icons/subscriptions/login page) to exactly match a "
                        + "role template for the given departments: retires records the user has that the template doesn't, "
                        + "and adds records the template has that the user lacks. Roles are admin-time templates only — "
                        + "this stamps user-level records; it never changes runtime behavior directly. "
                        + "roleId is optional — omit it to reset to the user's own current role (the API 400s if the user "
                        + "has no role). Set preview=true first to see added/retired counts per aspect without writing "
                        + "anything, then call again with preview omitted/false to apply. Always confirm with the user "
                        + "before applying (preview=false).")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("id", Json.createObjectBuilder().add("type", "string").add("description", "Target WebUser ID"))
                                .add("roleId", Json.createObjectBuilder().add("type", "string").add("description", "Role template ID; omit to use the user's own role"))
                                .add("departmentIds", Json.createObjectBuilder().add("type", "string").add("description", "Comma-separated department IDs to reset"))
                                .add("aspects", Json.createObjectBuilder().add("type", "string").add("description", "Comma-separated aspects: PRIVILEGES, ICONS, SUBSCRIPTIONS, LOGIN_PAGE. Default PRIVILEGES"))
                                .add("updateUserRole", Json.createObjectBuilder().add("type", "string").add("description", "'true' or 'false' — also set WebUser.role to roleId. Default true"))
                                .add("preview", Json.createObjectBuilder().add("type", "string").add("description", "'true' to preview counts without writing. Default false")))
                        .add("required", Json.createArrayBuilder().add("id").add("departmentIds")))
                .build();

        JsonObject userRoleExpandTool = Json.createObjectBuilder()
                .add("name", "user_role_expand")
                .add("description",
                        "Add role-template records (privileges/icons/subscriptions/login page) the user is missing, for the "
                        + "given departments. Existing extra records the user already has beyond the template are left "
                        + "untouched. roleId is required. Set preview=true first to see how many records would be added "
                        + "per aspect, then call again with preview omitted/false to apply. Always confirm with the user "
                        + "before applying.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("id", Json.createObjectBuilder().add("type", "string").add("description", "Target WebUser ID"))
                                .add("roleId", Json.createObjectBuilder().add("type", "string").add("description", "Role template ID (required)"))
                                .add("departmentIds", Json.createObjectBuilder().add("type", "string").add("description", "Comma-separated department IDs"))
                                .add("aspects", Json.createObjectBuilder().add("type", "string").add("description", "Comma-separated aspects: PRIVILEGES, ICONS, SUBSCRIPTIONS, LOGIN_PAGE. Default PRIVILEGES"))
                                .add("preview", Json.createObjectBuilder().add("type", "string").add("description", "'true' to preview counts without writing. Default false")))
                        .add("required", Json.createArrayBuilder().add("id").add("roleId").add("departmentIds")))
                .build();

        JsonObject userRoleNarrowTool = Json.createObjectBuilder()
                .add("name", "user_role_narrow")
                .add("description",
                        "Retire the user's records (privileges/icons/subscriptions/login page) that match a role template, "
                        + "for the given departments. Records the user has that are NOT part of the template are left "
                        + "untouched. roleId is required. Set preview=true first to see how many records would be retired "
                        + "per aspect, then call again with preview omitted/false to apply. Always confirm with the user "
                        + "before applying.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("id", Json.createObjectBuilder().add("type", "string").add("description", "Target WebUser ID"))
                                .add("roleId", Json.createObjectBuilder().add("type", "string").add("description", "Role template ID (required)"))
                                .add("departmentIds", Json.createObjectBuilder().add("type", "string").add("description", "Comma-separated department IDs"))
                                .add("aspects", Json.createObjectBuilder().add("type", "string").add("description", "Comma-separated aspects: PRIVILEGES, ICONS, SUBSCRIPTIONS, LOGIN_PAGE. Default PRIVILEGES"))
                                .add("preview", Json.createObjectBuilder().add("type", "string").add("description", "'true' to preview counts without writing. Default false")))
                        .add("required", Json.createArrayBuilder().add("id").add("roleId").add("departmentIds")))
                .build();

        JsonObject userBulkRoleOperationsTool = Json.createObjectBuilder()
                .add("name", "user_bulk_role_operations")
                .add("description",
                        "Apply RESET, EXPAND, or NARROW role-template operations to many users at once. Target users are "
                        + "either an explicit userIds list (wins if given) or a filter by filterRoleId/filterDepartmentId "
                        + "(users with that role and/or an active loggable department assignment). "
                        + "Two-step safety gate mirroring the UI confirm dialog: first call with preview=true (confirm "
                        + "omitted/false) to see the resolved user count and per-aspect totals (capped at the first 200 "
                        + "users); only after showing this to the user and getting explicit approval, repeat the identical "
                        + "call with confirm=true (preview omitted/false) to actually apply. Calling with neither preview "
                        + "nor confirm set is rejected by the API.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("action", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder().add("RESET").add("EXPAND").add("NARROW"))
                                        .add("description", "Operation to apply to every resolved user"))
                                .add("userIds", Json.createObjectBuilder().add("type", "string").add("description", "Comma-separated WebUser IDs. Wins over filterRoleId/filterDepartmentId if given"))
                                .add("filterRoleId", Json.createObjectBuilder().add("type", "string").add("description", "Only used when userIds is omitted: match users with this role"))
                                .add("filterDepartmentId", Json.createObjectBuilder().add("type", "string").add("description", "Only used when userIds is omitted: match users with an active loggable assignment to this department"))
                                .add("roleId", Json.createObjectBuilder().add("type", "string").add("description", "Target template role ID. Omit to use each user's own current role"))
                                .add("departmentIds", Json.createObjectBuilder().add("type", "string").add("description", "Comma-separated department IDs to operate on (required)"))
                                .add("aspects", Json.createObjectBuilder().add("type", "string").add("description", "Comma-separated aspects: PRIVILEGES, ICONS, SUBSCRIPTIONS, LOGIN_PAGE. Default PRIVILEGES"))
                                .add("updateUserRole", Json.createObjectBuilder().add("type", "string").add("description", "'true' or 'false' — for RESET, also set each user's WebUser.role to roleId. Default true"))
                                .add("preview", Json.createObjectBuilder().add("type", "string").add("description", "'true' for the first, read-only call"))
                                .add("confirm", Json.createObjectBuilder().add("type", "string").add("description", "'true' for the second call that actually applies the operation")))
                        .add("required", Json.createArrayBuilder().add("action").add("departmentIds")))
                .build();

        JsonObject listUserRolesTool = Json.createObjectBuilder()
                .add("name", "list_user_roles")
                .add("description",
                        "List active user roles with their role-template summary: id, name, description, template login "
                        + "page, and counts of active role-level privileges, template icons, and template subscriptions. "
                        + "Use this to discover valid roleId values before calling user_role_reset/expand/narrow or "
                        + "user_bulk_role_operations.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder())
                        .add("required", Json.createArrayBuilder()))
                .build();

        JsonObject setUserLoginPageTool = Json.createObjectBuilder()
                .add("name", "set_user_login_page")
                .add("description",
                        "Set or remove a user's default login page override for one department. This is separate from a "
                        + "role's template login page (admin-time only) and from the legacy WebUser.loginPage fallback. "
                        + "Runtime resolution order: this user+department override, then WebUser.loginPage, then HOME. "
                        + "action SET (default) requires loginPage (a LoginPage enum name); action DELETE removes the "
                        + "override for that department, falling back to the legacy behavior. Always confirm with the "
                        + "user before calling.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("action", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder().add("SET").add("DELETE"))
                                        .add("description", "SET to upsert the override (default), DELETE to remove it"))
                                .add("id", Json.createObjectBuilder().add("type", "string").add("description", "Target WebUser ID"))
                                .add("departmentId", Json.createObjectBuilder().add("type", "string").add("description", "Department ID"))
                                .add("loginPage", Json.createObjectBuilder().add("type", "string").add("description", "LoginPage enum name — required for action SET")))
                        .add("required", Json.createArrayBuilder().add("id").add("departmentId")))
                .build();

        JsonObject managePharmacyItemsTool = Json.createObjectBuilder()
                .add("name", "manage_pharmacy_items")
                .add("description",
                        "Create, search, update, get, or retire dispensable pharmacy PharmaceuticalItem records used by dispensing.\n\n"
                        + "method: SEARCH | GET | POST | PUT | DELETE. "
                        + "For classification hierarchy items such as AMP/VMP, use the pharmaceutical_items API instead. "
                        + "Always confirm with the user before POST, PUT, or DELETE — these changes affect live dispensing and billing.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder().add("SEARCH").add("GET").add("POST").add("PUT").add("DELETE")))
                                .add("id", Json.createObjectBuilder().add("type", "string"))
                                .add("query", Json.createObjectBuilder().add("type", "string"))
                                .add("size", Json.createObjectBuilder().add("type", "string"))
                                .add("name", Json.createObjectBuilder().add("type", "string"))
                                .add("code", Json.createObjectBuilder().add("type", "string"))
                                .add("categoryId", Json.createObjectBuilder().add("type", "string"))
                                .add("dosageFormId", Json.createObjectBuilder().add("type", "string"))
                                .add("ampId", Json.createObjectBuilder().add("type", "string"))
                                .add("institutionId", Json.createObjectBuilder().add("type", "string"))
                                .add("departmentId", Json.createObjectBuilder().add("type", "string"))
                                .add("retailRate", Json.createObjectBuilder().add("type", "string"))
                                .add("allowFractions", Json.createObjectBuilder().add("type", "string").add("description", "'true' or 'false'"))
                                .add("discountAllowed", Json.createObjectBuilder().add("type", "string").add("description", "'true' or 'false'"))
                                .add("retireComments", Json.createObjectBuilder().add("type", "string")))
                        .add("required", Json.createArrayBuilder().add("method")))
                .build();

        JsonObject manageChannelBookingTool = Json.createObjectBuilder()
                .add("name", "manage_channel_booking")
                .add("description",
                        "Call documented Channel / Booking API operations. Uses the HMIS API key as the Token header, not Finance. "
                        + "Confirm doctor/session availability before save/edit/complete/cancel operations.\n\n"
                        + "operation: SPECIALIZATIONS | HOSPITALS | DOCTORS | DOCTOR_AVAILABILITY | DOCTOR_SESSIONS | DOCTOR_SESSION | "
                        + "SAVE | EDIT | COMPLETE | CHANNEL_HISTORY_LIST | CHANNEL_HISTORY_BY_REF | CANCELLATION\n"
                        + "For POST operations, provide requestBody as a JSON object string expected by the endpoint. "
                        + "Always confirm with the user before SAVE, EDIT, COMPLETE, or CANCELLATION.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("operation", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder()
                                                .add("SPECIALIZATIONS").add("HOSPITALS").add("DOCTORS")
                                                .add("DOCTOR_AVAILABILITY").add("DOCTOR_SESSIONS").add("DOCTOR_SESSION")
                                                .add("SAVE").add("EDIT").add("COMPLETE")
                                                .add("CHANNEL_HISTORY_LIST").add("CHANNEL_HISTORY_BY_REF").add("CANCELLATION")))
                                .add("requestBody", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Raw JSON request body for the selected endpoint")))
                        .add("required", Json.createArrayBuilder().add("operation")))
                .build();

        JsonObject manageInpatientTemplates = Json.createObjectBuilder()
                .add("name", "manage_inpatient_templates")
                .add("description",
                        "Create, read, update, and retire document templates stored in the HMIS. "
                        + "Templates are HTML-based with placeholder tokens that are substituted at generation time. "
                        + "Supported types: Prescription, MedicalCertificate, FitnessCertificate, Referral, InpatientDiagnosisCard, InpatientLetter.\n\n"
                        + "method: LIST | GET | POST | PUT | DELETE\n\n"
                        + "LIST: returns all non-retired templates; optional filters: type, query (name search), size.\n"
                        + "GET: returns a single template including the full contents field; requires id.\n"
                        + "POST: creates a new template; requires name, type, contents.\n"
                        + "PUT: updates an existing template; requires id; optional fields: name, type, contents, defaultTemplate, autoGenerate.\n"
                        + "DELETE: soft-retires the template; requires id.\n\n"
                        + "InpatientLetter placeholder tokens available in contents:\n"
                        + "  Patient: {name} {age} {sex} {address} {phone} {bht} {doa} {dod}\n"
                        + "  Clinical: {dx} {past-dx} {allergies} {routine-medicines} {rx} {drx} {ix} {procedures}\n"
                        + "  Vitals: {bp} {pr} {rr} {sat} {height} {weight} {bmi} {pfr}\n"
                        + "  Vital series: {temp-series} {bp-series} {pr-series} {rr-series} {sat-series}\n"
                        + "  Credit company: {credit_company} {credit_company_address} {policy_no} {reference_no} {credit_limit}\n"
                        + "  Institution: {institution} {department} {doctor} {letter_date}\n"
                        + "  Billing: {final_bill} (admission net total) {patient_name} {patient_age} {patient_sex} (aliases of name/age/sex)\n"
                        + "If the admission has more than one credit company, the user selects which one to use on the "
                        + "inward_letters page before generating; the credit company placeholders resolve to the selected company.\n"
                        + "InpatientDiagnosisCard uses the same placeholders (credit company fields resolve to empty if not applicable).")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder().add("LIST").add("GET").add("POST").add("PUT").add("DELETE"))
                                        .add("description", "Operation to perform"))
                                .add("id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Template ID — required for GET, PUT, DELETE"))
                                .add("type", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder().add("InpatientDiagnosisCard").add("InpatientLetter"))
                                        .add("description", "Template type — required for POST; optional filter for LIST"))
                                .add("name", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Template name — required for POST; optional for PUT"))
                                .add("contents", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "HTML template body with placeholder tokens — required for POST; optional for PUT"))
                                .add("defaultTemplate", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "'true' or 'false' — marks this as the default template for its type"))
                                .add("autoGenerate", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "'true' or 'false' — auto-regenerate on encounter changes"))
                                .add("query", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Name search string for LIST"))
                                .add("size", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Max results for LIST (default 200)")))
                        .add("required", Json.createArrayBuilder().add("method")))
                .build();

        JsonObject manageTimedItemsTool = Json.createObjectBuilder()
                .add("name", "manage_timed_items")
                .add("description",
                        "Manage timed item master data (room rent, oxygen, ICU time, etc.) and their tiered fee slots. "
                        + "TimedItems are consumed by the inward timed service page to bill patients for duration-based charges. "
                        + "Fees are ordered by sortOrder; each fee defines a durationHours block with an optional overShootHours grace window. "
                        + "Methods for items: LIST, GET, POST, PUT, DELETE, ACTIVATE, DEACTIVATE. "
                        + "Methods for fees: LIST_FEES, POST_FEE, PUT_FEE, DELETE_FEE. "
                        + "Always confirm with the user before creating, updating, or retiring records.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder()
                                                .add("LIST").add("GET").add("POST").add("PUT").add("DELETE")
                                                .add("ACTIVATE").add("DEACTIVATE")
                                                .add("LIST_FEES").add("POST_FEE").add("PUT_FEE").add("DELETE_FEE"))
                                        .add("description", "Operation to perform."))
                                .add("id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Timed item id. Required for GET, PUT, DELETE, ACTIVATE, DEACTIVATE, LIST_FEES, POST_FEE, PUT_FEE, DELETE_FEE."))
                                .add("feeId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Fee id. Required for PUT_FEE and DELETE_FEE."))
                                .add("name", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Name of the timed item or fee. Required for POST and POST_FEE."))
                                .add("code", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Short code. Optional; auto-generated from name if omitted on POST."))
                                .add("departmentType", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Department type enum value (e.g. Inward, Theatre). Required for POST."))
                                .add("inwardChargeType", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "InwardChargeType enum value (e.g. Room, Oxygen, NursingCharge). Required for POST."))
                                .add("departmentId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Department id. Optional."))
                                .add("institutionId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Institution id. Optional."))
                                .add("categoryId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "TimedItemCategory (Service Group) id. Optional."))
                                .add("inactive", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "true or false — whether item is inactive. Optional."))
                                .add("fee", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Fee amount for this tier block. Required for POST_FEE."))
                                .add("ffee", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Foreigner fee amount. Optional; defaults to fee if omitted."))
                                .add("durationHours", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Block duration in hours this fee tier covers. Required for POST_FEE (must be > 0)."))
                                .add("overShootHours", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Grace hours beyond durationHours before the next tier applies. Optional."))
                                .add("durationDaysForMoCharge", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Duration days for monthly charge calculation. Optional."))
                                .add("sortOrder", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Fee tier ordering (ascending). Optional."))
                                .add("repeating", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "true or false — whether this fee repeats for multiple blocks. Optional."))
                                .add("query", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Search text for LIST. Optional."))
                                .add("size", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Max results (1–100). Optional."))
                                .add("retireComments", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Reason for retirement. Optional for DELETE/DELETE_FEE.")))
                        .add("required", Json.createArrayBuilder().add("method")))
                .build();

        JsonObject lookupFinanceBillTool = Json.createObjectBuilder()
                .add("name", "lookup_finance_bill")
                .add("description",
                        "Look up bills from the HMIS Finance API by bill number (insId or deptId). "
                        + "Returns all bills matching the given bill number, including both PreBill and BilledBill records. "
                        + "Use this when the user asks to find or retrieve a specific bill by its printed number.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("billNumber", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "The bill number to search for, e.g. MP/SPB/26/027264. Matched against both insId and deptId.")))
                        .add("required", Json.createArrayBuilder().add("billNumber")))
                .build();

        JsonObject managePharmacyDiscountsTool = Json.createObjectBuilder()
                .add("name", "manage_pharmacy_discounts")
                .add("description",
                        "Create, list, update, or retire pharmacy payment-scheme discount rows (PaymentSchemeDiscount). "
                        + "Use BULK to set the same discount % across all pharmacy item categories for a payment scheme in one call (idempotent: re-running updates, never duplicates). "
                        + "method: LIST | POST | BULK | PUT | DELETE.\n\n"
                        + "LIST: returns non-retired discount rows; optional filters: paymentSchemeId, paymentSchemeName, billType, limit.\n"
                        + "POST: create a single row; required: discountPercent + paymentMethod; optional: categoryId, paymentSchemeId, paymentSchemeName, billType.\n"
                        + "BULK: upsert across ALL pharmacy item categories; required: discountPercent + paymentMethod + (paymentSchemeId or paymentSchemeName); optional: billType.\n"
                        + "PUT: update a row; required: id + discountPercent.\n"
                        + "DELETE: soft-retire a row; required: id.\n\n"
                        + "Default billType is PharmacySale when omitted. Always confirm with the user before POST, BULK, PUT, or DELETE.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder()
                                                .add("LIST").add("POST").add("BULK").add("PUT").add("DELETE")))
                                .add("id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Discount row id — required for PUT and DELETE"))
                                .add("paymentSchemeId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "PaymentScheme id — use with POST, BULK, LIST"))
                                .add("paymentSchemeName", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "PaymentScheme name (partial match for LIST, exact-then-partial for BULK/POST) — alternative to paymentSchemeId"))
                                .add("categoryId", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Category id — optional for POST (single row)"))
                                .add("paymentMethod", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "PaymentMethod enum value, e.g. Cash, Credit, MultiplePaymentMethods — required for POST and BULK; optional for LIST"))
                                .add("billType", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "BillType enum value, e.g. PharmacySale — defaults to PharmacySale when omitted"))
                                .add("discountPercent", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Discount percentage, e.g. '5.0'"))
                                .add("limit", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Max rows for LIST (default 200)")))
                        .add("required", Json.createArrayBuilder().add("method")))
                .build();

        JsonObject managePaymentSchemesTool = Json.createObjectBuilder()
                .add("name", "manage_payment_schemes")
                .add("description",
                        "List or update PaymentScheme records. "
                        + "method: LIST | UPDATE.\n\n"
                        + "LIST: returns all non-retired payment schemes with all billing-scope flags "
                        + "(validForInpatientBills, validForPharmacy, validForBilledBills, validForChanneling) "
                        + "and eligibility flags. Optional filter: query (name substring).\n"
                        + "UPDATE: partial update of a payment scheme; required: id. "
                        + "Supply only the fields to change (name, printingName, validForInpatientBills, "
                        + "validForPharmacy, validForBilledBills, validForChanneling, staffMemberRequired, "
                        + "membershipRequired, staffRequired, staffOrFamilyRequired, memberRequired, "
                        + "memberOrFamilyRequired, seniorCitizenRequired, pregnantMotherRequired, orderNo). "
                        + "Always confirm with the user before UPDATE.")
                .add("input_schema", Json.createObjectBuilder()
                        .add("type", "object")
                        .add("properties", Json.createObjectBuilder()
                                .add("method", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("enum", Json.createArrayBuilder().add("LIST").add("UPDATE")))
                                .add("id", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "PaymentScheme id — required for UPDATE"))
                                .add("query", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Name substring filter for LIST"))
                                .add("limit", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Max rows for LIST (default 500)"))
                                .add("name", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Scheme name (UPDATE)"))
                                .add("printingName", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Printing name (UPDATE)"))
                                .add("validForInpatientBills", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "true or false (UPDATE)"))
                                .add("validForPharmacy", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "true or false (UPDATE)"))
                                .add("validForBilledBills", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "true or false (UPDATE)"))
                                .add("validForChanneling", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "true or false (UPDATE)"))
                                .add("staffMemberRequired", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "true or false (UPDATE)"))
                                .add("membershipRequired", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "true or false (UPDATE)"))
                                .add("staffRequired", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "true or false (UPDATE)"))
                                .add("staffOrFamilyRequired", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "true or false (UPDATE)"))
                                .add("memberRequired", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "true or false (UPDATE)"))
                                .add("memberOrFamilyRequired", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "true or false (UPDATE)"))
                                .add("seniorCitizenRequired", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "true or false (UPDATE)"))
                                .add("pregnantMotherRequired", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "true or false (UPDATE)"))
                                .add("orderNo", Json.createObjectBuilder()
                                        .add("type", "string")
                                        .add("description", "Integer sort order (UPDATE)")))
                        .add("required", Json.createArrayBuilder().add("method")))
                .build();

        return Json.createArrayBuilder()
                .add(searchCodeTool)
                .add(fetchFileTool)
                .add(searchConfigTool)
                .add(manageConfigOptionTool)
                .add(admissionNumberTool)
                .add(admissionSearchTool)
                .add(clinicalMetadataTool)
                .add(itemRequestTool)
                .add(collectingCentreFeesTool)
                .add(inwardDiscountMatrixTool)
                .add(inwardPriceAdjustmentTool)
                .add(priceMatrixInwardTool)
                .add(inwardRoomsTool)
                .add(bedBoardSvgTool)
                .add(manageInvestigationsTool)
                .add(manageServicesTool)
                .add(manageInvestigationFormatTool)
                .add(manageInvestigationComponentsTool)
                .add(manageInvestigationPricingTool)
                .add(manageInvestigationValidatorsTool)
                .add(manageInvestigationExportTool)
                .add(manageFormsTool)
                .add(manageSubscriptionsTool)
                .add(manageStaffTool)
                .add(manageUsersTool)
                .add(userRoleResetTool)
                .add(userRoleExpandTool)
                .add(userRoleNarrowTool)
                .add(userBulkRoleOperationsTool)
                .add(listUserRolesTool)
                .add(setUserLoginPageTool)
                .add(managePharmacyItemsTool)
                .add(managePharmacyDiscountsTool)
                .add(managePaymentSchemesTool)
                .add(manageChannelBookingTool)
                .add(manageInpatientTemplates)
                .add(manageTimedItemsTool)
                .add(lookupFinanceBillTool)
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
                case "manage_config_option": {
                    String method = toolInput.getString("method", "GET");
                    String key    = toolInput.containsKey("key")   ? toolInput.getString("key", "")   : "";
                    String value  = toolInput.containsKey("value") ? toolInput.getString("value", "") : null;
                    return manageConfigOption(method, key, value, hmisApiKey);
                }
                case "manage_admission_number_counter": {
                    String method = toolInput.getString("method", "GET");
                    String admissionTypeId = toolInput.containsKey("admissionTypeId") ? toolInput.getString("admissionTypeId", "") : "";
                    String institutionId = toolInput.containsKey("institutionId") ? toolInput.getString("institutionId", "") : "";
                    String lastAdmissionNumber = toolInput.containsKey("lastAdmissionNumber") ? toolInput.getString("lastAdmissionNumber", "") : "";
                    String expectedLastAdmissionNumber = toolInput.containsKey("expectedLastAdmissionNumber") ? toolInput.getString("expectedLastAdmissionNumber", "") : "";
                    return callAdmissionNumberApi(method, admissionTypeId, institutionId, lastAdmissionNumber, expectedLastAdmissionNumber, hmisBaseUrl, hmisApiKey);
                }
                case "search_admissions": {
                    Map<String, String> params = new HashMap<>();
                    for (String key : new String[]{"status", "bhtNo", "patientName", "mrn", "phone", "nic",
                        "admissionTypeId", "institutionId", "departmentId", "fromDate", "toDate", "page", "size"}) {
                        if (toolInput.containsKey(key)) {
                            params.put(key, toolInput.getString(key, ""));
                        }
                    }
                    return callAdmissionSearchApi(params, hmisBaseUrl, hmisApiKey);
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
                case "manage_item_requests": {
                    String method             = toolInput.getString("method", "GET");
                    String id                 = toolInput.containsKey("id") ? toolInput.getString("id", "") : "";
                    String bhtNo              = toolInput.containsKey("bhtNo") ? toolInput.getString("bhtNo", "") : "";
                    String targetDepartmentId = toolInput.containsKey("targetDepartmentId") ? toolInput.getString("targetDepartmentId", "") : "";
                    String comments           = toolInput.containsKey("comments") ? toolInput.getString("comments", "") : "";
                    String linesJson          = toolInput.containsKey("linesJson") ? toolInput.getString("linesJson", "") : "";
                    String reason             = toolInput.containsKey("reason") ? toolInput.getString("reason", "") : "";
                    String status             = toolInput.containsKey("status") ? toolInput.getString("status", "") : "";
                    String fromDate           = toolInput.containsKey("fromDate") ? toolInput.getString("fromDate", "") : "";
                    String toDate             = toolInput.containsKey("toDate") ? toolInput.getString("toDate", "") : "";
                    String limit              = toolInput.containsKey("limit") ? toolInput.getString("limit", "") : "";
                    return callItemRequestApi(method, id, bhtNo, targetDepartmentId, comments, linesJson,
                            reason, status, fromDate, toDate, limit, hmisBaseUrl, hmisApiKey);
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
                    String creditCompanyId   = toolInput.containsKey("creditCompanyId")  ? toolInput.getString("creditCompanyId", "")  : "";
                    String query             = toolInput.containsKey("query")            ? toolInput.getString("query", "")            : "";
                    String limit             = toolInput.containsKey("limit")            ? toolInput.getString("limit", "")            : "";
                    String retireComments    = toolInput.containsKey("retireComments")   ? toolInput.getString("retireComments", "")   : "";
                    return callInwardDiscountMatrixApi(method, scope, id, departmentId, categoryId,
                            admissionTypeId, paymentSchemeId, paymentMethodStr, discountPercent,
                            creditCompanyId, query, limit, retireComments, hmisBaseUrl, hmisApiKey);
                }
                case "manage_inward_price_adjustment": {
                    String method         = toolInput.getString("method", "LIST");
                    String scope          = toolInput.containsKey("scope")          ? toolInput.getString("scope", "")          : "";
                    String id             = toolInput.containsKey("id")             ? toolInput.getString("id", "")             : "";
                    String departmentId   = toolInput.containsKey("departmentId")   ? toolInput.getString("departmentId", "")   : "";
                    String categoryId     = toolInput.containsKey("categoryId")     ? toolInput.getString("categoryId", "")     : "";
                    String paymentMethod2 = toolInput.containsKey("paymentMethod")  ? toolInput.getString("paymentMethod", "")  : "";
                    String fromPrice      = toolInput.containsKey("fromPrice")      ? toolInput.getString("fromPrice", "")      : "";
                    String toPrice        = toolInput.containsKey("toPrice")        ? toolInput.getString("toPrice", "")        : "";
                    String margin         = toolInput.containsKey("margin")         ? toolInput.getString("margin", "")         : "";
                    String creditCompanyId2 = toolInput.containsKey("creditCompanyId") ? toolInput.getString("creditCompanyId", "") : "";
                    String query2         = toolInput.containsKey("query")          ? toolInput.getString("query", "")          : "";
                    String limit2         = toolInput.containsKey("limit")          ? toolInput.getString("limit", "")          : "";
                    String retireComments2 = toolInput.containsKey("retireComments") ? toolInput.getString("retireComments", "") : "";
                    return callInwardPriceAdjustmentApi(method, scope, id, departmentId, categoryId,
                            paymentMethod2, fromPrice, toPrice, margin, creditCompanyId2,
                            query2, limit2, retireComments2, hmisBaseUrl, hmisApiKey);
                }
                case "manage_price_matrix_inward": {
                    String method         = toolInput.getString("method", "LIST");
                    String id             = toolInput.containsKey("id")             ? toolInput.getString("id", "")             : "";
                    String departmentId   = toolInput.containsKey("departmentId")   ? toolInput.getString("departmentId", "")   : "";
                    String categoryId     = toolInput.containsKey("categoryId")     ? toolInput.getString("categoryId", "")     : "";
                    String paymentMethod  = toolInput.containsKey("paymentMethod")  ? toolInput.getString("paymentMethod", "")  : "";
                    String margin         = toolInput.containsKey("margin")         ? toolInput.getString("margin", "")         : "";
                    String discountPercent = toolInput.containsKey("discountPercent") ? toolInput.getString("discountPercent", "") : "";
                    String fromPrice      = toolInput.containsKey("fromPrice")      ? toolInput.getString("fromPrice", "")      : "";
                    String toPrice        = toolInput.containsKey("toPrice")        ? toolInput.getString("toPrice", "")        : "";
                    String admissionTypeId = toolInput.containsKey("admissionTypeId") ? toolInput.getString("admissionTypeId", "") : "";
                    String creditCompanyId = toolInput.containsKey("creditCompanyId") ? toolInput.getString("creditCompanyId", "") : "";
                    String limit          = toolInput.containsKey("limit")          ? toolInput.getString("limit", "")          : "";
                    String retireComments = toolInput.containsKey("retireComments") ? toolInput.getString("retireComments", "") : "";
                    return callPriceMatrixInwardApi(method, id, departmentId, categoryId,
                            paymentMethod, margin, discountPercent, fromPrice, toPrice,
                            admissionTypeId, creditCompanyId, limit, retireComments,
                            hmisBaseUrl, hmisApiKey);
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
                    String vatable = toolInput.containsKey("vatable") ? toolInput.getString("vatable", "") : "";
                    String vatPercentage = toolInput.containsKey("vatPercentage") ? toolInput.getString("vatPercentage", "") : "";
                    String categoryId = toolInput.containsKey("categoryId") ? toolInput.getString("categoryId", "") : "";
                    String categoryName = toolInput.containsKey("categoryName") ? toolInput.getString("categoryName", "") : "";
                    String sampleId = toolInput.containsKey("sampleId") ? toolInput.getString("sampleId", "") : "";
                    String sampleName = toolInput.containsKey("sampleName") ? toolInput.getString("sampleName", "") : "";
                    String containerId = toolInput.containsKey("containerId") ? toolInput.getString("containerId", "") : "";
                    String containerName = toolInput.containsKey("containerName") ? toolInput.getString("containerName", "") : "";
                    String analyzerId = toolInput.containsKey("analyzerId") ? toolInput.getString("analyzerId", "") : "";
                    String analyzerName = toolInput.containsKey("analyzerName") ? toolInput.getString("analyzerName", "") : "";
                    return callInvestigationApi(method, id, query, inactive, limit, name, code, printName, reportType, bypass, vatable, vatPercentage,
                            categoryId, categoryName, sampleId, sampleName, containerId, containerName, analyzerId, analyzerName, hmisBaseUrl, hmisApiKey);
                }
                case "manage_investigation_components": {
                    String method = toolInput.getString("method", "LIST");
                    String investigationId = toolInput.getString("investigation_id", "");
                    String componentId = toolInput.containsKey("component_id") ? toolInput.getString("component_id", "") : "";
                    String componentName = toolInput.containsKey("component_name") ? toolInput.getString("component_name", "") : "";
                    return callInvestigationComponentApi(method, investigationId, componentId, componentName, hmisBaseUrl, hmisApiKey);
                }
                case "manage_investigation_pricing": {
                    String method = toolInput.getString("method", "LIST");
                    String investigationId = toolInput.getString("investigation_id", "");
                    String feeId = toolInput.containsKey("fee_id") ? toolInput.getString("fee_id", "") : "";
                    String name = toolInput.containsKey("name") ? toolInput.getString("name", "") : "";
                    String feeType = toolInput.containsKey("feeType") ? toolInput.getString("feeType", "") : "";
                    String fee = toolInput.containsKey("fee") ? toolInput.getString("fee", "") : "";
                    String ffee = toolInput.containsKey("ffee") ? toolInput.getString("ffee", "") : "";
                    String discountAllowed = toolInput.containsKey("discountAllowed") ? toolInput.getString("discountAllowed", "") : "";
                    String institutionId = toolInput.containsKey("institutionId") ? toolInput.getString("institutionId", "") : "";
                    String departmentId = toolInput.containsKey("departmentId") ? toolInput.getString("departmentId", "") : "";
                    String specialityId = toolInput.containsKey("specialityId") ? toolInput.getString("specialityId", "") : "";
                    String staffId = toolInput.containsKey("staffId") ? toolInput.getString("staffId", "") : "";
                    return callInvestigationPricingApi(method, investigationId, feeId, name, feeType, fee, ffee, discountAllowed,
                            institutionId, departmentId, specialityId, staffId, hmisBaseUrl, hmisApiKey);
                }
                case "manage_investigation_validators": {
                    String method = toolInput.getString("method", "LIST");
                    String investigationId = toolInput.getString("investigation_id", "");
                    String validatorId = toolInput.containsKey("validator_id") ? toolInput.getString("validator_id", "") : "";
                    String name = toolInput.containsKey("name") ? toolInput.getString("name", "") : "";
                    String maximumValue = toolInput.containsKey("maximumValue") ? toolInput.getString("maximumValue", "") : "";
                    String minimumValue = toolInput.containsKey("minimumValue") ? toolInput.getString("minimumValue", "") : "";
                    return callInvestigationValidatorApi(method, investigationId, validatorId, name, maximumValue, minimumValue, hmisBaseUrl, hmisApiKey);
                }
                case "manage_investigation_export": {
                    String investigationId = toolInput.getString("investigation_id", "");
                    return callInvestigationFullApi(investigationId, hmisBaseUrl, hmisApiKey);
                }
                case "manage_services": {
                    String method = toolInput.getString("method", "GET");
                    String id = toolInput.containsKey("id") ? toolInput.getString("id", "") : "";
                    String query = toolInput.containsKey("query") ? toolInput.getString("query", "") : "";
                    String serviceType = toolInput.containsKey("serviceType") ? toolInput.getString("serviceType", "") : "";
                    String categoryId = toolInput.containsKey("categoryId") ? toolInput.getString("categoryId", "") : "";
                    String inactive = toolInput.containsKey("inactive") ? toolInput.getString("inactive", "") : "";
                    String limit = toolInput.containsKey("limit") ? toolInput.getString("limit", "20") : "20";
                    String name = toolInput.containsKey("name") ? toolInput.getString("name", "") : "";
                    String code = toolInput.containsKey("code") ? toolInput.getString("code", "") : "";
                    String printName = toolInput.containsKey("printName") ? toolInput.getString("printName", "") : "";
                    String fullName = toolInput.containsKey("fullName") ? toolInput.getString("fullName", "") : "";
                    String inwardChargeType = toolInput.containsKey("inwardChargeType") ? toolInput.getString("inwardChargeType", "") : "";
                    String vatable = toolInput.containsKey("vatable") ? toolInput.getString("vatable", "") : "";
                    String vatPercentage = toolInput.containsKey("vatPercentage") ? toolInput.getString("vatPercentage", "") : "";
                    return callServiceApi(method, id, query, serviceType, categoryId, inactive, limit, name, code, printName, fullName, inwardChargeType, vatable, vatPercentage, hmisBaseUrl, hmisApiKey);
                }
                case "manage_investigation_format": {
                    String resourceType = toolInput.getString("resource_type", "ITEM");
                    String method = toolInput.getString("method", "LIST");
                    String investigationId = toolInput.getString("investigation_id", "");
                    String itemId = toolInput.containsKey("item_id") ? toolInput.getString("item_id", "") : "";
                    String id = toolInput.containsKey("id") ? toolInput.getString("id", "") : "";
                    String name = toolInput.containsKey("name") ? toolInput.getString("name", "") : "";
                    String code = toolInput.containsKey("code") ? toolInput.getString("code", "") : "";
                    String desc = toolInput.containsKey("description") ? toolInput.getString("description", "") : "";
                    String orderNo = toolInput.containsKey("order_no") ? toolInput.getString("order_no", "") : "";
                    String ixItemType = toolInput.containsKey("ix_item_type") ? toolInput.getString("ix_item_type", "") : "";
                    String ixItemValueType = toolInput.containsKey("ix_item_value_type") ? toolInput.getString("ix_item_value_type", "") : "";
                    String automated = toolInput.containsKey("automated") ? toolInput.getString("automated", "") : "";
                    String resultCode = toolInput.containsKey("result_code") ? toolInput.getString("result_code", "") : "";
                    String formatPrefix = toolInput.containsKey("format_prefix") ? toolInput.getString("format_prefix", "") : "";
                    String formatSuffix = toolInput.containsKey("format_suffix") ? toolInput.getString("format_suffix", "") : "";
                    String htmltext = toolInput.containsKey("htmltext") ? toolInput.getString("htmltext", "") : "";
                    String canNotApproveIfEmpty = toolInput.containsKey("can_not_approve_if_empty") ? toolInput.getString("can_not_approve_if_empty", "") : "";
                    String absoluteLowValue = toolInput.containsKey("absolute_low_value") ? toolInput.getString("absolute_low_value", "") : "";
                    String absoluteHighValue = toolInput.containsKey("absolute_high_value") ? toolInput.getString("absolute_high_value", "") : "";
                    String calIxItemId = toolInput.containsKey("cal_ix_item_id") ? toolInput.getString("cal_ix_item_id", "") : "";
                    String valIxItemId = toolInput.containsKey("val_ix_item_id") ? toolInput.getString("val_ix_item_id", "") : "";
                    String calculationType = toolInput.containsKey("calculation_type") ? toolInput.getString("calculation_type", "") : "";
                    String constantValue = toolInput.containsKey("constant_value") ? toolInput.getString("constant_value", "") : "";
                    String maleConstantValue = toolInput.containsKey("male_constant_value") ? toolInput.getString("male_constant_value", "") : "";
                    String femaleConstantValue = toolInput.containsKey("female_constant_value") ? toolInput.getString("female_constant_value", "") : "";
                    String javascript = toolInput.containsKey("javascript") ? toolInput.getString("javascript", "") : "";
                    String valueItemId = toolInput.containsKey("investigation_item_of_value_type_id") ? toolInput.getString("investigation_item_of_value_type_id", "") : "";
                    String flagItemId = toolInput.containsKey("investigation_item_of_flag_type_id") ? toolInput.getString("investigation_item_of_flag_type_id", "") : "";
                    String labelItemId = toolInput.containsKey("investigation_item_of_label_type_id") ? toolInput.getString("investigation_item_of_label_type_id", "") : "";
                    String sex = toolInput.containsKey("sex") ? toolInput.getString("sex", "") : "";
                    String fromAge = toolInput.containsKey("from_age") ? toolInput.getString("from_age", "") : "";
                    String toAge = toolInput.containsKey("to_age") ? toolInput.getString("to_age", "") : "";
                    String fromVal = toolInput.containsKey("from_val") ? toolInput.getString("from_val", "") : "";
                    String toVal = toolInput.containsKey("to_val") ? toolInput.getString("to_val", "") : "";
                    String flagMessage = toolInput.containsKey("flag_message") ? toolInput.getString("flag_message", "") : "";
                    String highMessage = toolInput.containsKey("high_message") ? toolInput.getString("high_message", "") : "";
                    String lowMessage = toolInput.containsKey("low_message") ? toolInput.getString("low_message", "") : "";
                    String normalMessage = toolInput.containsKey("normal_message") ? toolInput.getString("normal_message", "") : "";
                    String displayFlagMessage = toolInput.containsKey("display_flag_message") ? toolInput.getString("display_flag_message", "") : "";
                    String displayHighMessage = toolInput.containsKey("display_high_message") ? toolInput.getString("display_high_message", "") : "";
                    String displayLowMessage = toolInput.containsKey("display_low_message") ? toolInput.getString("display_low_message", "") : "";
                    String displayNormalMessage = toolInput.containsKey("display_normal_message") ? toolInput.getString("display_normal_message", "") : "";
                    return callInvestigationFormatApi(resourceType, method, investigationId, itemId, id,
                            name, code, desc, orderNo, ixItemType, ixItemValueType,
                            automated, resultCode, formatPrefix, formatSuffix, htmltext,
                            canNotApproveIfEmpty, absoluteLowValue, absoluteHighValue,
                            calIxItemId, valIxItemId, calculationType, constantValue,
                            maleConstantValue, femaleConstantValue, javascript,
                            valueItemId, flagItemId, labelItemId,
                            sex, fromAge, toAge, fromVal, toVal,
                            flagMessage, highMessage, lowMessage, normalMessage,
                            displayFlagMessage, displayHighMessage, displayLowMessage, displayNormalMessage,
                            hmisBaseUrl, hmisApiKey);
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
                    // null = caller omitted the field (leave unchanged); a non-null
                    // value, including "", is forwarded ("" clears the drawing).
                    String svgChildView   = toolInput.containsKey("svgChildView")                   ? toolInput.getString("svgChildView", "")                   : null;
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
                    String timedItemId    = toolInput.containsKey("timedItemId")                    ? toolInput.getString("timedItemId", "")                    : "";
                    String linkId         = toolInput.containsKey("linkId")                         ? toolInput.getString("linkId", "")                         : "";
                    return callInwardRoomsApi(method, id, name, code, desc, roomCategoryId, roomId,
                            departmentId, filled, svgChildView, roomCharge, maintCharge, linenCharge, nursingCharge,
                            moCharge, moAfterCharge, adminCharge, medCareCharge,
                            durationHours, overShoot, durationDays,
                            query, size, retireComments, timedItemId, linkId, hmisBaseUrl, hmisApiKey);
                }
                case "manage_bed_board_svg": {
                    String method        = toolInput.getString("method", "GET_SITE");
                    String id            = toolInput.containsKey("id")            ? toolInput.getString("id", "")            : "";
                    String svgParentView = toolInput.containsKey("svgParentView") ? toolInput.getString("svgParentView", "") : null;
                    String svgChildView  = toolInput.containsKey("svgChildView")  ? toolInput.getString("svgChildView", "")  : null;
                    return callBedBoardSvgApi(method, id, svgParentView, svgChildView, hmisBaseUrl, hmisApiKey);
                }
                case "manage_forms": {
                    String resourceType = toolInput.getString("resource_type", "TEMPLATE");
                    String method       = toolInput.getString("method", "LIST");
                    String id           = toolInput.containsKey("id")           ? toolInput.getString("id", "")           : "";
                    String formId       = toolInput.containsKey("form_id")      ? toolInput.getString("form_id", "")      : "";
                    String fieldId      = toolInput.containsKey("field_id")     ? toolInput.getString("field_id", "")     : "";
                    String admissionId  = toolInput.containsKey("admission_id") ? toolInput.getString("admission_id", "") : "";
                    String entryId      = toolInput.containsKey("entry_id")     ? toolInput.getString("entry_id", "")     : "";
                    String name         = toolInput.containsKey("name")         ? toolInput.getString("name", null)       : null;
                    String description  = toolInput.containsKey("description")  ? toolInput.getString("description", null): null;
                    String formCssClass = toolInput.containsKey("formCssClass") ? toolInput.getString("formCssClass", null): null;
                    String cpt          = toolInput.containsKey("componentPresentationType") ? toolInput.getString("componentPresentationType", null) : null;
                    String cdt          = toolInput.containsKey("componentDataType")         ? toolInput.getString("componentDataType", null)         : null;
                    String orderNo      = toolInput.containsKey("orderNo")      ? toolInput.getString("orderNo", null)    : null;
                    String required     = toolInput.containsKey("required")     ? toolInput.getString("required", null)   : null;
                    String placeholder  = toolInput.containsKey("placeholder")  ? toolInput.getString("placeholder", null): null;
                    String minValue     = toolInput.containsKey("minValue")     ? toolInput.getString("minValue", null)   : null;
                    String maxValue     = toolInput.containsKey("maxValue")     ? toolInput.getString("maxValue", null)   : null;
                    String stepSize     = toolInput.containsKey("stepSize")     ? toolInput.getString("stepSize", null)   : null;
                    String maxRating    = toolInput.containsKey("maxRating")    ? toolInput.getString("maxRating", null)  : null;
                    String onLabel      = toolInput.containsKey("onLabel")      ? toolInput.getString("onLabel", null)    : null;
                    String offLabel     = toolInput.containsKey("offLabel")     ? toolInput.getString("offLabel", null)   : null;
                    String editHtml     = toolInput.containsKey("editHtml")     ? toolInput.getString("editHtml", null)   : null;
                    String viewHtml     = toolInput.containsKey("viewHtml")     ? toolInput.getString("viewHtml", null)   : null;
                    String label        = toolInput.containsKey("label")        ? toolInput.getString("label", null)      : null;
                    String value        = toolInput.containsKey("value")        ? toolInput.getString("value", null)      : null;
                    return callFormsApi(resourceType, method, id, formId, fieldId, admissionId, entryId,
                            name, description, formCssClass, cpt, cdt, orderNo, required,
                            placeholder, minValue, maxValue, stepSize, maxRating,
                            onLabel, offLabel, editHtml, viewHtml, label, value,
                            hmisBaseUrl, hmisApiKey);
                }
                case "manage_subscriptions": {
                    String method          = toolInput.getString("method", "LIST");
                    String id              = toolInput.containsKey("id")              ? toolInput.getString("id", "")              : "";
                    String triggerType     = toolInput.containsKey("triggerType")     ? toolInput.getString("triggerType", "")     : "";
                    String userId          = toolInput.containsKey("userId")          ? toolInput.getString("userId", "")          : "";
                    String departmentId    = toolInput.containsKey("departmentId")    ? toolInput.getString("departmentId", "")    : "";
                    String applicationWide = toolInput.containsKey("applicationWide") ? toolInput.getString("applicationWide", "") : "";
                    return callSubscriptionApi(method, id, triggerType, userId, departmentId, applicationWide,
                            hmisBaseUrl, hmisApiKey);
                }
                case "manage_staff": {
                    return callStaffApi(toolInput, hmisBaseUrl, hmisApiKey);
                }
                case "manage_users": {
                    return callUsersApi(toolInput, hmisBaseUrl, hmisApiKey);
                }
                case "user_role_reset": {
                    return callUserRoleOperationApi("reset", toolInput, false, hmisBaseUrl, hmisApiKey);
                }
                case "user_role_expand": {
                    return callUserRoleOperationApi("expand", toolInput, true, hmisBaseUrl, hmisApiKey);
                }
                case "user_role_narrow": {
                    return callUserRoleOperationApi("narrow", toolInput, true, hmisBaseUrl, hmisApiKey);
                }
                case "user_bulk_role_operations": {
                    return callUserBulkRoleOperationsApi(toolInput, hmisBaseUrl, hmisApiKey);
                }
                case "list_user_roles": {
                    return callListUserRolesApi(hmisBaseUrl, hmisApiKey);
                }
                case "set_user_login_page": {
                    return callSetUserLoginPageApi(toolInput, hmisBaseUrl, hmisApiKey);
                }
                case "manage_pharmacy_items": {
                    return callPharmacyItemsApi(toolInput, hmisBaseUrl, hmisApiKey);
                }
                case "manage_pharmacy_discounts": {
                    return callPharmacyDiscountsApi(toolInput, hmisBaseUrl, hmisApiKey);
                }
                case "manage_payment_schemes": {
                    return callPaymentSchemeApi(toolInput, hmisBaseUrl, hmisApiKey);
                }
                case "manage_channel_booking": {
                    return callChannelBookingApi(toolInput, hmisBaseUrl, hmisApiKey);
                }
                case "manage_inpatient_templates": {
                    String method        = toolInput.getString("method", "LIST");
                    String id            = toolInput.containsKey("id")             ? toolInput.getString("id", "")            : "";
                    String templateType  = toolInput.containsKey("type")           ? toolInput.getString("type", "")          : "";
                    String name          = toolInput.containsKey("name")           ? toolInput.getString("name", "")          : "";
                    String contents      = toolInput.containsKey("contents")       ? toolInput.getString("contents", "")      : "";
                    String defTemplate   = toolInput.containsKey("defaultTemplate") ? toolInput.getString("defaultTemplate", "") : "";
                    String autoGenerate  = toolInput.containsKey("autoGenerate")   ? toolInput.getString("autoGenerate", "")  : "";
                    String query         = toolInput.containsKey("query")          ? toolInput.getString("query", "")         : "";
                    String size          = toolInput.containsKey("size")           ? toolInput.getString("size", "")          : "";
                    return callInpatientTemplateApi(method, id, templateType, name, contents, defTemplate, autoGenerate,
                            query, size, hmisBaseUrl, hmisApiKey);
                }
                case "lookup_finance_bill": {
                    String billNumber = toolInput.containsKey("billNumber") ? toolInput.getString("billNumber", "") : "";
                    return lookupFinanceBillByNumber(billNumber, hmisBaseUrl, hmisApiKey);
                }
                case "manage_timed_items": {
                    String method       = toolInput.getString("method", "LIST");
                    String id           = toolInput.containsKey("id")           ? toolInput.getString("id", "")           : "";
                    String feeId        = toolInput.containsKey("feeId")        ? toolInput.getString("feeId", "")        : "";
                    String name         = toolInput.containsKey("name")         ? toolInput.getString("name", "")         : "";
                    String code         = toolInput.containsKey("code")         ? toolInput.getString("code", "")         : "";
                    String deptType     = toolInput.containsKey("departmentType")    ? toolInput.getString("departmentType", "")    : "";
                    String chargeType   = toolInput.containsKey("inwardChargeType")  ? toolInput.getString("inwardChargeType", "")  : "";
                    String departmentId = toolInput.containsKey("departmentId")      ? toolInput.getString("departmentId", "")      : "";
                    String institutionId= toolInput.containsKey("institutionId")     ? toolInput.getString("institutionId", "")     : "";
                    String categoryId   = toolInput.containsKey("categoryId")        ? toolInput.getString("categoryId", "")        : "";
                    String inactive     = toolInput.containsKey("inactive")          ? toolInput.getString("inactive", "")          : "";
                    String fee          = toolInput.containsKey("fee")               ? toolInput.getString("fee", "")               : "";
                    String ffee         = toolInput.containsKey("ffee")              ? toolInput.getString("ffee", "")              : "";
                    String durationHrs  = toolInput.containsKey("durationHours")     ? toolInput.getString("durationHours", "")     : "";
                    String overShoot    = toolInput.containsKey("overShootHours")    ? toolInput.getString("overShootHours", "")    : "";
                    String durationDays = toolInput.containsKey("durationDaysForMoCharge") ? toolInput.getString("durationDaysForMoCharge", "") : "";
                    String sortOrder    = toolInput.containsKey("sortOrder")         ? toolInput.getString("sortOrder", "")         : "";
                    String repeating    = toolInput.containsKey("repeating")         ? toolInput.getString("repeating", "")         : "";
                    String query        = toolInput.containsKey("query")             ? toolInput.getString("query", "")             : "";
                    String size         = toolInput.containsKey("size")              ? toolInput.getString("size", "")              : "";
                    String retireComments = toolInput.containsKey("retireComments")  ? toolInput.getString("retireComments", "")    : "";
                    return callTimedItemsApi(method, id, feeId, name, code, deptType, chargeType,
                            departmentId, institutionId, categoryId, inactive,
                            fee, ffee, durationHrs, overShoot, durationDays, sortOrder, repeating,
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

    private String manageConfigOption(String method, String key, String newValue, String hmisApiKey) {
        if (key == null || key.trim().isEmpty()) {
            return "Error: key is required.";
        }
        String normalizedMethod = method == null ? "GET" : method.trim().toUpperCase();
        if (!"GET".equals(normalizedMethod) && !"PUT".equals(normalizedMethod)) {
            return "Error: Unknown method '" + method + "'. Supported: GET, PUT";
        }
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("scope", OptionScope.APPLICATION);
            params.put("k", key);
            ConfigOption option = configOptionFacade.findFirstByJpql(
                    "SELECT o FROM ConfigOption o WHERE o.retired = false AND o.scope = :scope AND o.optionKey = :k",
                    params);

            if ("PUT".equals(normalizedMethod)) {
                if (newValue == null) {
                    return "Error: value is required for PUT.";
                }
                if (option == null) {
                    return "Error: config option not found: " + key;
                }
                String oldValue = option.getOptionValue();
                OptionValueType vt = option.getValueType();
                String validationError = validateTypedValue(vt, newValue);
                if (validationError != null) {
                    return validationError;
                }
                applyTypedUpdate(key, newValue, option, vt);

                String callerName = resolveCallerName(hmisApiKey);
                LOG.log(Level.INFO,
                        "CONFIG_UPDATED via AI Chat tool key=[{0}] old=[{1}] new=[{2}] by=[{3}] at=[{4}]",
                        new Object[]{key,
                            maskSensitiveValue(key, oldValue),
                            maskSensitiveValue(key, newValue),
                            callerName,
                            new java.util.Date()});
                return "Config option updated.\nKey: " + key
                        + "\nOld value: " + maskSensitiveValue(key, oldValue)
                        + "\nNew value: " + maskSensitiveValue(key, newValue)
                        + "\nUpdated by: " + callerName;
            }

            // GET
            if (option == null) {
                return "Config option not found: " + key;
            }
            String value = maskSensitiveValue(option.getOptionKey(), option.getOptionValue());
            if (value != null && value.length() > 500) {
                value = value.substring(0, 500) + "... (truncated)";
            }
            return "Key: " + option.getOptionKey() + "\nType: " + option.getValueType()
                    + "\nScope: " + option.getScope() + "\nValue: " + value;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "manage_config_option failed", e);
            return "Error: " + e.getMessage();
        }
    }

    private String validateTypedValue(OptionValueType vt, String newValue) {
        if (vt == null) {
            return null;
        }
        switch (vt) {
            case BOOLEAN:
                if (!"true".equalsIgnoreCase(newValue.trim()) && !"false".equalsIgnoreCase(newValue.trim())) {
                    return "Error: Value '" + newValue + "' is not a valid boolean (must be true or false).";
                }
                break;
            case INTEGER:
                try {
                    Integer.parseInt(newValue.trim());
                } catch (NumberFormatException e) {
                    return "Error: Value '" + newValue + "' is not a valid integer.";
                }
                break;
            case LONG:
                try {
                    Long.parseLong(newValue.trim());
                } catch (NumberFormatException e) {
                    return "Error: Value '" + newValue + "' is not a valid long integer.";
                }
                break;
            case DOUBLE:
                try {
                    Double.parseDouble(newValue.trim());
                } catch (NumberFormatException e) {
                    return "Error: Value '" + newValue + "' is not a valid number.";
                }
                break;
            default:
                break;
        }
        return null;
    }

    private void applyTypedUpdate(String key, String newValue, ConfigOption option, OptionValueType vt) {
        if (vt == OptionValueType.LONG_TEXT) {
            configOptionApplicationController.setLongTextValueByKey(key, newValue);
        } else if (vt == OptionValueType.BOOLEAN) {
            configOptionApplicationController.setBooleanValueByKey(key, Boolean.parseBoolean(newValue.trim()));
        } else if (vt == OptionValueType.INTEGER) {
            configOptionApplicationController.setIntegerValueByKey(key, Integer.parseInt(newValue.trim()));
        } else {
            option.setOptionValue(newValue);
            configOptionFacade.edit(option);
            configOptionApplicationController.loadApplicationOptions();
        }
    }

    private String resolveCallerName(String hmisApiKey) {
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "AI Chat (user unknown)";
        }
        try {
            Map<String, Object> p = new HashMap<>();
            p.put("k", hmisApiKey);
            ApiKey ak = apiKeyFacade.findFirstByJpql(
                    "SELECT a FROM ApiKey a WHERE a.keyValue = :k AND a.retired = false", p);
            if (ak != null && ak.getWebUser() != null) {
                return ak.getWebUser().getName() + " (AI Chat)";
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Could not resolve caller name from hmisApiKey", e);
        }
        return "AI Chat (user unknown)";
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

    private String callAdmissionNumberApi(String method, String admissionTypeId, String institutionId,
            String lastAdmissionNumber, String expectedLastAdmissionNumber, String hmisBaseUrl, String hmisApiKey) {
        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured. Cannot call admission number API.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: No active HMIS API key found for the current user.";
        }
        if (admissionTypeId == null || admissionTypeId.trim().isEmpty()) {
            return "Error: admissionTypeId is required.";
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String base = hmisBaseUrl.trim().replaceAll("/+$", "") + "/api/admission-numbers";
            String url;
            String requestBody = null;
            String httpMethod;

            switch (method.toUpperCase()) {
                case "GET": {
                    StringBuilder urlBuilder = new StringBuilder(base)
                            .append("?admissionTypeId=").append(URLEncoder.encode(admissionTypeId.trim(), StandardCharsets.UTF_8));
                    if (institutionId != null && !institutionId.trim().isEmpty()) {
                        urlBuilder.append("&institutionId=").append(URLEncoder.encode(institutionId.trim(), StandardCharsets.UTF_8));
                    }
                    url = urlBuilder.toString();
                    httpMethod = "GET";
                    break;
                }
                case "PUT": {
                    if (lastAdmissionNumber == null || lastAdmissionNumber.trim().isEmpty()) {
                        return "Error: lastAdmissionNumber is required for PUT.";
                    }
                    if (expectedLastAdmissionNumber == null || expectedLastAdmissionNumber.trim().isEmpty()) {
                        return "Error: expectedLastAdmissionNumber is required for PUT (the lastAdmissionNumber "
                                + "value most recently observed via GET).";
                    }
                    StringBuilder urlBuilder = new StringBuilder(base)
                            .append("?admissionTypeId=").append(URLEncoder.encode(admissionTypeId.trim(), StandardCharsets.UTF_8));
                    if (institutionId != null && !institutionId.trim().isEmpty()) {
                        urlBuilder.append("&institutionId=").append(URLEncoder.encode(institutionId.trim(), StandardCharsets.UTF_8));
                    }
                    url = urlBuilder.toString();
                    httpMethod = "PUT";
                    javax.json.JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();
                    bodyBuilder.add("lastAdmissionNumber", Long.parseLong(lastAdmissionNumber.trim()));
                    bodyBuilder.add("expectedLastAdmissionNumber", Long.parseLong(expectedLastAdmissionNumber.trim()));
                    requestBody = bodyBuilder.build().toString();
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
            } else {
                reqBuilder.GET();
            }

            HttpResponse<String> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return "HTTP " + response.statusCode() + "\n" + response.body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Admission number API call interrupted.";
        } catch (NumberFormatException e) {
            return "Error: lastAdmissionNumber and expectedLastAdmissionNumber must be whole numbers.";
        } catch (Exception e) {
            return "Admission number API error: " + e.getMessage();
        }
    }

    private String callAdmissionSearchApi(Map<String, String> params, String hmisBaseUrl, String hmisApiKey) {
        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured. Cannot call admission search API.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: No active HMIS API key found for the current user.";
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            StringBuilder urlBuilder = new StringBuilder(
                    hmisBaseUrl.trim().replaceAll("/+$", "") + "/api/inward/admissions");
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getValue() == null || entry.getValue().trim().isEmpty()) {
                    continue;
                }
                urlBuilder.append(first ? "?" : "&")
                        .append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue().trim(), StandardCharsets.UTF_8));
                first = false;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlBuilder.toString()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Finance", hmisApiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return "HTTP " + response.statusCode() + "\n" + response.body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Admission search API call interrupted.";
        } catch (Exception e) {
            return "Admission search API error: " + e.getMessage();
        }
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

    private String callItemRequestApi(String method, String id, String bhtNo, String targetDepartmentId,
            String comments, String linesJson, String reason, String status, String fromDate, String toDate,
            String limit, String hmisBaseUrl, String hmisApiKey) {
        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured. Cannot call item requests API.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: No active HMIS API key found for the current user.";
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String base = hmisBaseUrl.trim().replaceAll("/+$", "") + "/api/itemrequests";
            String url;
            String requestBody = null;
            String httpMethod;

            switch (method.toUpperCase()) {
                case "POST": {
                    if (bhtNo == null || bhtNo.trim().isEmpty()) {
                        return "Error: bhtNo is required for POST.";
                    }
                    if (targetDepartmentId == null || targetDepartmentId.trim().isEmpty()) {
                        return "Error: targetDepartmentId is required for POST.";
                    }
                    url = base;
                    httpMethod = "POST";
                    javax.json.JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();
                    bodyBuilder.add("bhtNo", bhtNo);
                    bodyBuilder.add("targetDepartmentId", Long.parseLong(targetDepartmentId.trim()));
                    if (comments != null && !comments.isEmpty()) {
                        bodyBuilder.add("comments", comments);
                    }
                    if (linesJson != null && !linesJson.trim().isEmpty()) {
                        try (JsonReader reader = Json.createReader(new StringReader(linesJson))) {
                            bodyBuilder.add("lines", reader.readArray());
                        } catch (Exception e) {
                            return "Error: linesJson is not valid JSON: " + e.getMessage();
                        }
                    } else {
                        return "Error: linesJson is required for POST (JSON array of {itemId, qty}).";
                    }
                    requestBody = bodyBuilder.build().toString();
                    break;
                }
                case "PUT": {
                    if (id == null || id.trim().isEmpty()) {
                        return "Error: id is required for PUT (cancel).";
                    }
                    url = base + "/" + id.trim() + "/cancel";
                    httpMethod = "PUT";
                    javax.json.JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();
                    if (reason != null && !reason.isEmpty()) {
                        bodyBuilder.add("reason", reason);
                    }
                    requestBody = bodyBuilder.build().toString();
                    break;
                }
                case "GET": {
                    if (id != null && !id.trim().isEmpty()) {
                        url = base + "/" + id.trim();
                    } else {
                        StringBuilder urlBuilder = new StringBuilder(base).append("?");
                        boolean first = true;
                        if (targetDepartmentId != null && !targetDepartmentId.isEmpty()) {
                            urlBuilder.append("targetDepartmentId=").append(URLEncoder.encode(targetDepartmentId, StandardCharsets.UTF_8));
                            first = false;
                        }
                        if (status != null && !status.isEmpty()) {
                            urlBuilder.append(first ? "" : "&").append("status=").append(URLEncoder.encode(status, StandardCharsets.UTF_8));
                            first = false;
                        }
                        if (fromDate != null && !fromDate.isEmpty()) {
                            urlBuilder.append(first ? "" : "&").append("fromDate=").append(URLEncoder.encode(fromDate, StandardCharsets.UTF_8));
                            first = false;
                        }
                        if (toDate != null && !toDate.isEmpty()) {
                            urlBuilder.append(first ? "" : "&").append("toDate=").append(URLEncoder.encode(toDate, StandardCharsets.UTF_8));
                            first = false;
                        }
                        if (limit != null && !limit.isEmpty()) {
                            urlBuilder.append(first ? "" : "&").append("limit=").append(URLEncoder.encode(limit, StandardCharsets.UTF_8));
                        }
                        url = urlBuilder.toString();
                    }
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
            } else {
                reqBuilder.GET();
            }

            HttpResponse<String> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return "HTTP " + response.statusCode() + "\n" + response.body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Item request API call interrupted.";
        } catch (Exception e) {
            return "Item request API error: " + e.getMessage();
        }
    }

    private String callSubscriptionApi(String method, String id, String triggerType, String userId,
            String departmentId, String applicationWide, String hmisBaseUrl, String hmisApiKey) {
        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured. Cannot call subscription API.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: No active HMIS API key found for the current user.";
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String base = hmisBaseUrl.trim().replaceAll("/+$", "") + "/api/subscriptions";
            String url;
            String requestBody = null;
            String httpMethod;

            switch (method.toUpperCase()) {
                case "LIST_TRIGGER_TYPES": {
                    url = base + "/trigger-types";
                    httpMethod = "GET";
                    break;
                }
                case "LIST":
                case "GET": {
                    StringBuilder urlBuilder = new StringBuilder(base);
                    boolean first = true;
                    if (triggerType != null && !triggerType.isEmpty()) {
                        urlBuilder.append(first ? "?" : "&").append("triggerType=").append(URLEncoder.encode(triggerType, StandardCharsets.UTF_8));
                        first = false;
                    }
                    if (userId != null && !userId.isEmpty()) {
                        urlBuilder.append(first ? "?" : "&").append("userId=").append(URLEncoder.encode(userId, StandardCharsets.UTF_8));
                        first = false;
                    }
                    if (departmentId != null && !departmentId.isEmpty()) {
                        urlBuilder.append(first ? "?" : "&").append("departmentId=").append(URLEncoder.encode(departmentId, StandardCharsets.UTF_8));
                        first = false;
                    }
                    if (applicationWide != null && !applicationWide.trim().isEmpty()) {
                        String normalizedAw = applicationWide.trim().toLowerCase();
                        if (!"true".equals(normalizedAw) && !"false".equals(normalizedAw)) {
                            return "Error: applicationWide must be 'true' or 'false'.";
                        }
                        urlBuilder.append(first ? "?" : "&").append("applicationWide=").append(normalizedAw);
                        first = false;
                    }
                    url = urlBuilder.toString();
                    httpMethod = "GET";
                    break;
                }
                case "POST": {
                    url = base;
                    httpMethod = "POST";
                    boolean hasDepartmentId = departmentId != null && !departmentId.trim().isEmpty();
                    boolean isApplicationWide = "true".equalsIgnoreCase(applicationWide);
                    if (hasDepartmentId && isApplicationWide) {
                        return "Error: provide exactly one of departmentId or applicationWide=true, not both.";
                    }
                    if (!hasDepartmentId && !isApplicationWide) {
                        return "Error: provide either departmentId or applicationWide=true.";
                    }
                    javax.json.JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();
                    if (triggerType != null && !triggerType.isEmpty()) bodyBuilder.add("triggerType", triggerType);
                    if (userId != null && !userId.isEmpty()) {
                        try {
                            bodyBuilder.add("userId", Long.parseLong(userId.trim()));
                        } catch (NumberFormatException e) {
                            return "Error: userId must be numeric.";
                        }
                    }
                    if (isApplicationWide) {
                        bodyBuilder.add("applicationWide", true);
                    } else {
                        try {
                            bodyBuilder.add("departmentId", Long.parseLong(departmentId.trim()));
                        } catch (NumberFormatException e) {
                            return "Error: departmentId must be numeric.";
                        }
                    }
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
            return "Subscription API call interrupted.";
        } catch (Exception e) {
            return "Subscription API error: " + e.getMessage();
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
            String discountPercent, String creditCompanyId, String query, String limit,
            String retireComments, String hmisBaseUrl, String hmisApiKey) {

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
                    if (creditCompanyId != null && !creditCompanyId.isEmpty()) {
                        urlBuilder.append(first ? "?" : "&").append("creditCompanyId=").append(creditCompanyId);
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
                    if (creditCompanyId != null && !creditCompanyId.trim().isEmpty()) bodyBuilder.add("creditCompanyId", Long.parseLong(creditCompanyId.trim()));
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
                    if (creditCompanyId != null && !creditCompanyId.trim().isEmpty()) bodyBuilder.add("creditCompanyId", Long.parseLong(creditCompanyId.trim()));
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
                case "LOOKUP_CREDIT_COMPANIES": {
                    url = lookupUrl(base + "/credit-companies/search", query, limit);
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

    private String callInwardPriceAdjustmentApi(
            String method, String scope, String id, String departmentId, String categoryId,
            String paymentMethod, String fromPrice, String toPrice, String margin,
            String creditCompanyId, String query, String limit, String retireComments,
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
            String base = root + "/api/inward-price-adjustment";
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
                    if (paymentMethod != null && !paymentMethod.isEmpty()) {
                        urlBuilder.append(first ? "?" : "&").append("paymentMethod=")
                                .append(URLEncoder.encode(paymentMethod, StandardCharsets.UTF_8));
                        first = false;
                    }
                    if (creditCompanyId != null && !creditCompanyId.isEmpty()) {
                        urlBuilder.append(first ? "?" : "&").append("creditCompanyId=").append(creditCompanyId);
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
                    if (paymentMethod != null && !paymentMethod.isEmpty()) bodyBuilder.add("paymentMethod", paymentMethod);
                    if (fromPrice != null && !fromPrice.trim().isEmpty()) bodyBuilder.add("fromPrice", Double.parseDouble(fromPrice.trim()));
                    if (toPrice != null && !toPrice.trim().isEmpty()) bodyBuilder.add("toPrice", Double.parseDouble(toPrice.trim()));
                    if (margin != null && !margin.trim().isEmpty()) bodyBuilder.add("margin", Double.parseDouble(margin.trim()));
                    if (creditCompanyId != null && !creditCompanyId.trim().isEmpty()) bodyBuilder.add("creditCompanyId", Long.parseLong(creditCompanyId.trim()));
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
                    if (paymentMethod != null && !paymentMethod.isEmpty()) bodyBuilder.add("paymentMethod", paymentMethod);
                    if (fromPrice != null && !fromPrice.trim().isEmpty()) bodyBuilder.add("fromPrice", Double.parseDouble(fromPrice.trim()));
                    if (toPrice != null && !toPrice.trim().isEmpty()) bodyBuilder.add("toPrice", Double.parseDouble(toPrice.trim()));
                    if (margin != null && !margin.trim().isEmpty()) bodyBuilder.add("margin", Double.parseDouble(margin.trim()));
                    if (creditCompanyId != null && !creditCompanyId.trim().isEmpty()) bodyBuilder.add("creditCompanyId", Long.parseLong(creditCompanyId.trim()));
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
                    url = lookupUrl(base + "/departments/search", query, limit);
                    httpMethod = "GET";
                    break;
                }
                case "LOOKUP_CATEGORIES": {
                    StringBuilder urlBuilder = new StringBuilder(base + "/categories/search");
                    boolean first = true;
                    if (scope != null && !scope.isEmpty()) {
                        urlBuilder.append("?scope=").append(URLEncoder.encode(scope, StandardCharsets.UTF_8));
                        first = false;
                    }
                    if (query != null && !query.isEmpty()) {
                        urlBuilder.append(first ? "?" : "&").append("query=")
                                .append(URLEncoder.encode(query, StandardCharsets.UTF_8));
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
                case "LIST_PAYMENT_METHODS": {
                    url = base + "/payment-methods";
                    httpMethod = "GET";
                    break;
                }
                case "LOOKUP_CREDIT_COMPANIES": {
                    url = lookupUrl(base + "/credit-companies/search", query, limit);
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
            return "Inward price adjustment API call interrupted.";
        } catch (Exception e) {
            return "Inward price adjustment API error: " + e.getMessage();
        }
    }

    private String callPriceMatrixInwardApi(
            String method, String id, String departmentId, String categoryId,
            String paymentMethod, String margin, String discountPercent,
            String fromPrice, String toPrice, String admissionTypeId,
            String creditCompanyId, String limit, String retireComments,
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
            String base = root + "/api/price-matrix/inward";
            String url;
            String requestBody = null;
            String httpMethod;

            switch (method == null ? "" : method.toUpperCase()) {
                case "LIST": {
                    StringBuilder urlBuilder = new StringBuilder(base);
                    boolean first = true;
                    if (departmentId != null && !departmentId.isEmpty()) {
                        urlBuilder.append(first ? "?" : "&").append("departmentId=").append(departmentId);
                        first = false;
                    }
                    if (categoryId != null && !categoryId.isEmpty()) {
                        urlBuilder.append(first ? "?" : "&").append("categoryId=").append(categoryId);
                        first = false;
                    }
                    if (paymentMethod != null && !paymentMethod.isEmpty()) {
                        urlBuilder.append(first ? "?" : "&").append("paymentMethod=")
                                .append(URLEncoder.encode(paymentMethod, StandardCharsets.UTF_8));
                        first = false;
                    }
                    if (limit != null && !limit.isEmpty()) {
                        urlBuilder.append(first ? "?" : "&").append("limit=").append(limit);
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
                    if (departmentId != null && !departmentId.trim().isEmpty()) bodyBuilder.add("departmentId", Long.parseLong(departmentId.trim()));
                    if (categoryId != null && !categoryId.trim().isEmpty()) bodyBuilder.add("categoryId", Long.parseLong(categoryId.trim()));
                    if (margin != null && !margin.trim().isEmpty()) bodyBuilder.add("margin", Double.parseDouble(margin.trim()));
                    if (paymentMethod != null && !paymentMethod.isEmpty()) bodyBuilder.add("paymentMethod", paymentMethod);
                    if (discountPercent != null && !discountPercent.trim().isEmpty()) bodyBuilder.add("discountPercent", Double.parseDouble(discountPercent.trim()));
                    if (fromPrice != null && !fromPrice.trim().isEmpty()) bodyBuilder.add("fromPrice", Double.parseDouble(fromPrice.trim()));
                    if (toPrice != null && !toPrice.trim().isEmpty()) bodyBuilder.add("toPrice", Double.parseDouble(toPrice.trim()));
                    if (admissionTypeId != null && !admissionTypeId.trim().isEmpty()) bodyBuilder.add("admissionTypeId", Long.parseLong(admissionTypeId.trim()));
                    if (creditCompanyId != null && !creditCompanyId.trim().isEmpty()) bodyBuilder.add("creditCompanyId", Long.parseLong(creditCompanyId.trim()));
                    requestBody = bodyBuilder.build().toString();
                    break;
                }
                case "PUT": {
                    if (id == null || id.trim().isEmpty()) return "Error: id is required for PUT.";
                    url = base + "/" + id.trim();
                    httpMethod = "PUT";
                    javax.json.JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();
                    if (departmentId != null && !departmentId.trim().isEmpty()) bodyBuilder.add("departmentId", Long.parseLong(departmentId.trim()));
                    if (categoryId != null && !categoryId.trim().isEmpty()) bodyBuilder.add("categoryId", Long.parseLong(categoryId.trim()));
                    if (margin != null && !margin.trim().isEmpty()) bodyBuilder.add("margin", Double.parseDouble(margin.trim()));
                    if (paymentMethod != null && !paymentMethod.isEmpty()) bodyBuilder.add("paymentMethod", paymentMethod);
                    if (discountPercent != null && !discountPercent.trim().isEmpty()) bodyBuilder.add("discountPercent", Double.parseDouble(discountPercent.trim()));
                    if (fromPrice != null && !fromPrice.trim().isEmpty()) bodyBuilder.add("fromPrice", Double.parseDouble(fromPrice.trim()));
                    if (toPrice != null && !toPrice.trim().isEmpty()) bodyBuilder.add("toPrice", Double.parseDouble(toPrice.trim()));
                    if (admissionTypeId != null && !admissionTypeId.trim().isEmpty()) bodyBuilder.add("admissionTypeId", Long.parseLong(admissionTypeId.trim()));
                    if (creditCompanyId != null && !creditCompanyId.trim().isEmpty()) bodyBuilder.add("creditCompanyId", Long.parseLong(creditCompanyId.trim()));
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
            return "Price matrix inward API call interrupted.";
        } catch (Exception e) {
            return "Price matrix inward API error: " + e.getMessage();
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
            String roomCategoryId, String roomId, String departmentId, String filled, String svgChildView,
            String roomCharge, String maintananceCharge, String linenCharge, String nursingCharge,
            String moCharge, String moChargeForAfterDuration, String adminstrationCharge, String medicalCareCharge,
            String timedItemFeeDurationHours, String timedItemFeeOverShootHours, String timedItemFeeDurationDaysForMoCharge,
            String query, String size, String retireComments, String timedItemId, String linkId,
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
                    if (svgChildView != null) bodyMap.put("svgChildView", svgChildView);
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
                    if (svgChildView != null) bodyMap.put("svgChildView", svgChildView);
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
                case "LIST_TIMED_ITEMS": {
                    if (id == null || id.isEmpty()) return "Error: id (room facility charge id) is required for LIST_TIMED_ITEMS.";
                    request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/inward/room-facility-charges/" + id + "/timed-items"))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey).GET().build();
                    break;
                }
                case "ADD_TIMED_ITEM": {
                    if (id == null || id.isEmpty()) return "Error: id (room facility charge id) is required for ADD_TIMED_ITEM.";
                    if (timedItemId == null || timedItemId.isEmpty()) return "Error: timedItemId is required for ADD_TIMED_ITEM.";
                    java.util.Map<String, Object> bodyMap = new java.util.LinkedHashMap<>();
                    bodyMap.put("timedItemId", Long.parseLong(timedItemId));
                    String bodyJson = new com.google.gson.Gson().toJson(bodyMap);
                    request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/inward/room-facility-charges/" + id + "/timed-items"))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(bodyJson)).build();
                    break;
                }
                case "REMOVE_TIMED_ITEM": {
                    if (id == null || id.isEmpty()) return "Error: id (room facility charge id) is required for REMOVE_TIMED_ITEM.";
                    if (linkId == null || linkId.isEmpty()) return "Error: linkId is required for REMOVE_TIMED_ITEM.";
                    StringBuilder url = new StringBuilder(baseUrl).append("/api/inward/room-facility-charges/").append(id).append("/timed-items/").append(linkId);
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

    /**
     * Read/set the bed-board SVG drawings of a site, department, or room (issue
     * #21592) via the dedicated /{id}/svg sub-resources. SVG is sent verbatim;
     * the bed board sanitises it at render time.
     */
    private String callBedBoardSvgApi(String method, String id, String svgParentView, String svgChildView,
            String hmisBaseUrl, String hmisApiKey) {

        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: HMIS API key is not configured.";
        }
        if (id == null || id.isEmpty()) {
            return "Error: id is required for " + method + ".";
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String baseUrl = hmisBaseUrl.endsWith("/") ? hmisBaseUrl.substring(0, hmisBaseUrl.length() - 1) : hmisBaseUrl;

            // Resolve the entity path from the method.
            String entityPath;
            boolean isRoom;
            if (method.endsWith("_SITE")) {
                entityPath = "/api/sites/";
                isRoom = false;
            } else if (method.endsWith("_INSTITUTION")) {
                entityPath = "/api/institutions/";
                isRoom = false;
            } else if (method.endsWith("_DEPARTMENT")) {
                entityPath = "/api/departments/";
                isRoom = false;
            } else if (method.endsWith("_ROOM")) {
                entityPath = "/api/inward/rooms/";
                isRoom = true;
            } else {
                return "Unknown method: " + method;
            }

            String url = baseUrl + entityPath + id + "/svg";
            HttpRequest request;

            if (method.startsWith("GET_")) {
                request = HttpRequest.newBuilder().uri(URI.create(url))
                        .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey).GET().build();
            } else if (method.startsWith("SET_")) {
                java.util.Map<String, Object> bodyMap = new java.util.LinkedHashMap<>();
                // Only include fields the caller actually supplied (null = omit, so
                // the entity field is left unchanged). A non-null value — including
                // an empty string, which clears the drawing — is forwarded.
                // A room has no parent canvas, so svgParentView is ignored for rooms.
                if (!isRoom && svgParentView != null) {
                    bodyMap.put("svgParentView", svgParentView);
                }
                if (svgChildView != null) {
                    bodyMap.put("svgChildView", svgChildView);
                }
                // Reject an empty SET so the tool can't silently report success
                // without changing anything.
                if (bodyMap.isEmpty()) {
                    return isRoom
                            ? "Error: svgChildView is required for " + method + " (rooms have no parent canvas)."
                            : "Error: svgParentView or svgChildView is required for " + method + ".";
                }
                String bodyJson = new com.google.gson.Gson().toJson(bodyMap);
                request = HttpRequest.newBuilder().uri(URI.create(url))
                        .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey)
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(bodyJson)).build();
            } else {
                return "Unknown method: " + method;
            }

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return "HTTP " + response.statusCode() + ": " + response.body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Bed Board SVG API call interrupted.";
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "callBedBoardSvgApi error: {0}", e.getMessage());
            return "Error calling Bed Board SVG API: " + e.getMessage();
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
    private String callInvestigationApi(String method, String id, String query, String inactive, String limit, String name, String code, String printName, String reportType, String bypassSampleWorkflow, String vatable, String vatPercentage,
            String categoryId, String categoryName, String sampleId, String sampleName, String containerId, String containerName, String analyzerId, String analyzerName,
            String hmisBaseUrl, String hmisApiKey) {
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
                if(vatable!=null&&!vatable.isEmpty()) b.add("vatable", Boolean.parseBoolean(vatable));
                if(vatPercentage!=null&&!vatPercentage.isEmpty()) b.add("vatPercentage", Double.parseDouble(vatPercentage));
                if(categoryId!=null&&!categoryId.isEmpty()) b.add("categoryId", Long.parseLong(categoryId));
                if(categoryName!=null&&!categoryName.isEmpty()) b.add("categoryName", categoryName);
                if(sampleId!=null&&!sampleId.isEmpty()) b.add("sampleId", Long.parseLong(sampleId));
                if(sampleName!=null&&!sampleName.isEmpty()) b.add("sampleName", sampleName);
                if(containerId!=null&&!containerId.isEmpty()) b.add("containerId", Long.parseLong(containerId));
                if(containerName!=null&&!containerName.isEmpty()) b.add("containerName", containerName);
                if(analyzerId!=null&&!analyzerId.isEmpty()) b.add("analyzerId", Long.parseLong(analyzerId));
                if(analyzerName!=null&&!analyzerName.isEmpty()) b.add("analyzerName", analyzerName);
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

    private String callInvestigationComponentApi(String method, String investigationId, String componentId, String componentName, String hmisBaseUrl, String hmisApiKey) {
        try {
            String root = (hmisBaseUrl != null) ? hmisBaseUrl.trim().replaceAll("/+$", "") : "";
            if (root.isEmpty()) return "Error: HMIS base URL is not configured.";
            if (investigationId == null || investigationId.isEmpty()) return "Error: investigation_id is required.";
            String key = (hmisApiKey != null) ? hmisApiKey.trim() : "";
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            String basePath = root + "/api/investigations/" + investigationId + "/components";
            HttpRequest.Builder rb;
            if ("LIST".equalsIgnoreCase(method)) {
                rb = HttpRequest.newBuilder().uri(URI.create(basePath)).GET();
            } else if ("POST".equalsIgnoreCase(method)) {
                if (componentName == null || componentName.isEmpty()) return "Error: component_name is required for POST.";
                javax.json.JsonObjectBuilder b = Json.createObjectBuilder().add("componentName", componentName);
                rb = HttpRequest.newBuilder().uri(URI.create(basePath)).POST(HttpRequest.BodyPublishers.ofString(b.build().toString())).header("Content-Type", "application/json");
            } else if ("PUT".equalsIgnoreCase(method)) {
                if (componentId == null || componentId.isEmpty()) return "Error: component_id is required for PUT.";
                if (componentName == null || componentName.isEmpty()) return "Error: component_name is required for PUT.";
                javax.json.JsonObjectBuilder b = Json.createObjectBuilder().add("componentName", componentName);
                rb = HttpRequest.newBuilder().uri(URI.create(basePath + "/" + componentId)).method("PUT", HttpRequest.BodyPublishers.ofString(b.build().toString())).header("Content-Type", "application/json");
            } else if ("DELETE".equalsIgnoreCase(method)) {
                if (componentId == null || componentId.isEmpty()) return "Error: component_id is required for DELETE.";
                rb = HttpRequest.newBuilder().uri(URI.create(basePath + "/" + componentId)).DELETE();
            } else {
                return "Error: Unsupported method for manage_investigation_components: " + method + ". Allowed: LIST, POST, PUT, DELETE.";
            }
            rb.timeout(Duration.ofSeconds(15));
            if (!key.isEmpty()) rb.header("Finance", key);
            HttpResponse<String> resp = client.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            return "HTTP " + resp.statusCode() + "\n" + resp.body();
        } catch (Exception e) { return "Investigation Components API error: " + e.getMessage(); }
    }

    private String callInvestigationPricingApi(String method, String investigationId, String feeId, String name, String feeType, String fee, String ffee,
            String discountAllowed, String institutionId, String departmentId, String specialityId, String staffId, String hmisBaseUrl, String hmisApiKey) {
        try {
            String root = (hmisBaseUrl != null) ? hmisBaseUrl.trim().replaceAll("/+$", "") : "";
            if (root.isEmpty()) return "Error: HMIS base URL is not configured.";
            if (investigationId == null || investigationId.isEmpty()) return "Error: investigation_id is required.";
            String key = (hmisApiKey != null) ? hmisApiKey.trim() : "";
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            String basePath = root + "/api/investigations/" + investigationId + "/fees";
            HttpRequest.Builder rb;
            if ("LIST".equalsIgnoreCase(method)) {
                rb = HttpRequest.newBuilder().uri(URI.create(basePath)).GET();
            } else if ("POST".equalsIgnoreCase(method)) {
                if (name == null || name.isEmpty()) return "Error: name is required for POST.";
                if (feeType == null || feeType.isEmpty()) return "Error: feeType is required for POST.";
                if (fee == null || fee.isEmpty()) return "Error: fee is required for POST.";
                javax.json.JsonObjectBuilder b = Json.createObjectBuilder().add("name", name).add("feeType", feeType).add("fee", Double.parseDouble(fee));
                if (ffee != null && !ffee.isEmpty()) b.add("ffee", Double.parseDouble(ffee));
                if (discountAllowed != null && !discountAllowed.isEmpty()) b.add("discountAllowed", Boolean.parseBoolean(discountAllowed));
                if (institutionId != null && !institutionId.isEmpty()) b.add("institutionId", Long.parseLong(institutionId));
                if (departmentId != null && !departmentId.isEmpty()) b.add("departmentId", Long.parseLong(departmentId));
                if (specialityId != null && !specialityId.isEmpty()) b.add("specialityId", Long.parseLong(specialityId));
                if (staffId != null && !staffId.isEmpty()) b.add("staffId", Long.parseLong(staffId));
                rb = HttpRequest.newBuilder().uri(URI.create(basePath)).POST(HttpRequest.BodyPublishers.ofString(b.build().toString())).header("Content-Type", "application/json");
            } else if ("PUT".equalsIgnoreCase(method)) {
                if (feeId == null || feeId.isEmpty()) return "Error: fee_id is required for PUT.";
                javax.json.JsonObjectBuilder b = Json.createObjectBuilder();
                if (name != null && !name.isEmpty()) b.add("name", name);
                if (feeType != null && !feeType.isEmpty()) b.add("feeType", feeType);
                if (fee != null && !fee.isEmpty()) b.add("fee", Double.parseDouble(fee));
                if (ffee != null && !ffee.isEmpty()) b.add("ffee", Double.parseDouble(ffee));
                if (discountAllowed != null && !discountAllowed.isEmpty()) b.add("discountAllowed", Boolean.parseBoolean(discountAllowed));
                if (institutionId != null && !institutionId.isEmpty()) b.add("institutionId", Long.parseLong(institutionId));
                if (departmentId != null && !departmentId.isEmpty()) b.add("departmentId", Long.parseLong(departmentId));
                if (specialityId != null && !specialityId.isEmpty()) b.add("specialityId", Long.parseLong(specialityId));
                if (staffId != null && !staffId.isEmpty()) b.add("staffId", Long.parseLong(staffId));
                rb = HttpRequest.newBuilder().uri(URI.create(basePath + "/" + feeId)).method("PUT", HttpRequest.BodyPublishers.ofString(b.build().toString())).header("Content-Type", "application/json");
            } else if ("DELETE".equalsIgnoreCase(method)) {
                if (feeId == null || feeId.isEmpty()) return "Error: fee_id is required for DELETE.";
                rb = HttpRequest.newBuilder().uri(URI.create(basePath + "/" + feeId)).DELETE();
            } else {
                return "Error: Unsupported method for manage_investigation_pricing: " + method + ". Allowed: LIST, POST, PUT, DELETE.";
            }
            rb.timeout(Duration.ofSeconds(15));
            if (!key.isEmpty()) rb.header("Finance", key);
            HttpResponse<String> resp = client.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            return "HTTP " + resp.statusCode() + "\n" + resp.body();
        } catch (Exception e) { return "Investigation Pricing API error: " + e.getMessage(); }
    }

    private String callInvestigationValidatorApi(String method, String investigationId, String validatorId, String name, String maximumValue, String minimumValue, String hmisBaseUrl, String hmisApiKey) {
        try {
            String root = (hmisBaseUrl != null) ? hmisBaseUrl.trim().replaceAll("/+$", "") : "";
            if (root.isEmpty()) return "Error: HMIS base URL is not configured.";
            if (investigationId == null || investigationId.isEmpty()) return "Error: investigation_id is required.";
            String key = (hmisApiKey != null) ? hmisApiKey.trim() : "";
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            String basePath = root + "/api/investigations/" + investigationId + "/validators";
            HttpRequest.Builder rb;
            if ("LIST".equalsIgnoreCase(method)) {
                rb = HttpRequest.newBuilder().uri(URI.create(basePath)).GET();
            } else if ("POST".equalsIgnoreCase(method)) {
                if (name == null || name.isEmpty()) return "Error: name is required for POST.";
                javax.json.JsonObjectBuilder b = Json.createObjectBuilder().add("name", name);
                if (maximumValue != null && !maximumValue.isEmpty()) b.add("maximumValue", Double.parseDouble(maximumValue));
                if (minimumValue != null && !minimumValue.isEmpty()) b.add("minimumValue", Double.parseDouble(minimumValue));
                rb = HttpRequest.newBuilder().uri(URI.create(basePath)).POST(HttpRequest.BodyPublishers.ofString(b.build().toString())).header("Content-Type", "application/json");
            } else if ("PUT".equalsIgnoreCase(method)) {
                if (validatorId == null || validatorId.isEmpty()) return "Error: validator_id is required for PUT.";
                javax.json.JsonObjectBuilder b = Json.createObjectBuilder();
                if (name != null && !name.isEmpty()) b.add("name", name);
                if (maximumValue != null && !maximumValue.isEmpty()) b.add("maximumValue", Double.parseDouble(maximumValue));
                if (minimumValue != null && !minimumValue.isEmpty()) b.add("minimumValue", Double.parseDouble(minimumValue));
                rb = HttpRequest.newBuilder().uri(URI.create(basePath + "/" + validatorId)).method("PUT", HttpRequest.BodyPublishers.ofString(b.build().toString())).header("Content-Type", "application/json");
            } else if ("DELETE".equalsIgnoreCase(method)) {
                if (validatorId == null || validatorId.isEmpty()) return "Error: validator_id is required for DELETE.";
                rb = HttpRequest.newBuilder().uri(URI.create(basePath + "/" + validatorId)).DELETE();
            } else {
                return "Error: Unsupported method for manage_investigation_validators: " + method + ". Allowed: LIST, POST, PUT, DELETE.";
            }
            rb.timeout(Duration.ofSeconds(15));
            if (!key.isEmpty()) rb.header("Finance", key);
            HttpResponse<String> resp = client.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            return "HTTP " + resp.statusCode() + "\n" + resp.body();
        } catch (Exception e) { return "Investigation Validators API error: " + e.getMessage(); }
    }

    private String callInvestigationFullApi(String investigationId, String hmisBaseUrl, String hmisApiKey) {
        try {
            String root = (hmisBaseUrl != null) ? hmisBaseUrl.trim().replaceAll("/+$", "") : "";
            if (root.isEmpty()) return "Error: HMIS base URL is not configured.";
            if (investigationId == null || investigationId.isEmpty()) return "Error: investigation_id is required.";
            String key = (hmisApiKey != null) ? hmisApiKey.trim() : "";
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest.Builder rb = HttpRequest.newBuilder().uri(URI.create(root + "/api/investigations/" + investigationId + "/full")).GET()
                    .timeout(Duration.ofSeconds(15));
            if (!key.isEmpty()) rb.header("Finance", key);
            HttpResponse<String> resp = client.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            return "HTTP " + resp.statusCode() + "\n" + resp.body();
        } catch (Exception e) { return "Investigation Export API error: " + e.getMessage(); }
    }

    private String callServiceApi(String method, String id, String query, String serviceType, String categoryId, String inactive, String limit, String name, String code, String printName, String fullName, String inwardChargeType, String vatable, String vatPercentage, String hmisBaseUrl, String hmisApiKey) {
        try {
            String root = (hmisBaseUrl != null) ? hmisBaseUrl.trim().replaceAll("/+$", "") : "";
            if (root.isEmpty()) return "Error: HMIS base URL is not configured.";
            String key = (hmisApiKey != null) ? hmisApiKey.trim() : "";
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest.Builder rb;
            if ("GET".equalsIgnoreCase(method)) {
                String url = root+"/api/services/search?query="+URLEncoder.encode(query, StandardCharsets.UTF_8)+"&limit="+URLEncoder.encode(limit, StandardCharsets.UTF_8);
                if(serviceType!=null&&!serviceType.isEmpty()) url += "&serviceType="+URLEncoder.encode(serviceType, StandardCharsets.UTF_8);
                if(categoryId!=null&&!categoryId.isEmpty()) url += "&categoryId="+URLEncoder.encode(categoryId, StandardCharsets.UTF_8);
                if(inactive!=null&&!inactive.isEmpty()) url += "&inactive="+URLEncoder.encode(inactive, StandardCharsets.UTF_8);
                rb = HttpRequest.newBuilder().uri(URI.create(url)).GET();
            } else if ("GET_BY_ID".equalsIgnoreCase(method)) { rb = HttpRequest.newBuilder().uri(URI.create(root+"/api/services/"+id)).GET(); }
            else if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                javax.json.JsonObjectBuilder b = Json.createObjectBuilder().add("name", name==null?"":name);
                if(serviceType!=null&&!serviceType.isEmpty()) b.add("serviceType", serviceType);
                if(code!=null&&!code.isEmpty()) b.add("code", code);
                if(printName!=null&&!printName.isEmpty()) b.add("printName", printName);
                if(fullName!=null&&!fullName.isEmpty()) b.add("fullName", fullName);
                if(categoryId!=null&&!categoryId.isEmpty()) b.add("categoryId", Long.parseLong(categoryId));
                if(inwardChargeType!=null&&!inwardChargeType.isEmpty()) b.add("inwardChargeType", inwardChargeType);
                if(vatable!=null&&!vatable.isEmpty()) b.add("vatable", Boolean.parseBoolean(vatable));
                if(vatPercentage!=null&&!vatPercentage.isEmpty()) b.add("vatPercentage", Double.parseDouble(vatPercentage));
                String u = "POST".equalsIgnoreCase(method) ? root+"/api/services" : root+"/api/services/"+id;
                rb = HttpRequest.newBuilder().uri(URI.create(u)).method("POST".equalsIgnoreCase(method)?"POST":"PUT", HttpRequest.BodyPublishers.ofString(b.build().toString())).header("Content-Type", "application/json");
            } else if ("ACTIVATE".equalsIgnoreCase(method) || "DEACTIVATE".equalsIgnoreCase(method)) {
                String u = root + "/api/services/" + id
                        + ("ACTIVATE".equalsIgnoreCase(method) ? "/activate" : "/deactivate");
                rb = HttpRequest.newBuilder().uri(URI.create(u))
                        .method("PATCH", HttpRequest.BodyPublishers.noBody());
            } else {
                return "Error: Unsupported method for manage_services: " + method
                        + ". Allowed methods are GET, GET_BY_ID, POST, PUT, ACTIVATE, DEACTIVATE.";
            }
            if(!key.isEmpty()) rb.header("Finance", key); HttpResponse<String> resp=client.send(rb.build(), HttpResponse.BodyHandlers.ofString()); return "HTTP "+resp.statusCode()+"\n"+resp.body();
        } catch (Exception e) { return "Service API error: "+e.getMessage(); }
    }

    private String callInvestigationFormatApi(String resourceType, String method,
            String investigationId, String itemId, String id,
            String name, String code, String description, String orderNo,
            String ixItemType, String ixItemValueType,
            String automated, String resultCode, String formatPrefix, String formatSuffix, String htmltext,
            String canNotApproveIfEmpty, String absoluteLowValue, String absoluteHighValue,
            String calIxItemId, String valIxItemId, String calculationType,
            String constantValue, String maleConstantValue, String femaleConstantValue, String javascript,
            String valueItemId, String flagItemId, String labelItemId,
            String sex, String fromAge, String toAge, String fromVal, String toVal,
            String flagMessage, String highMessage, String lowMessage, String normalMessage,
            String displayFlagMessage, String displayHighMessage, String displayLowMessage, String displayNormalMessage,
            String hmisBaseUrl, String hmisApiKey) {
        try {
            String root = (hmisBaseUrl != null) ? hmisBaseUrl.trim().replaceAll("/+$", "") : "";
            if (root.isEmpty()) return "Error: HMIS base URL is not configured.";
            String key = (hmisApiKey != null) ? hmisApiKey.trim() : "";
            if (investigationId == null || investigationId.isEmpty()) return "Error: investigation_id is required.";
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

            String basePath = root + "/api/investigations/" + investigationId + "/format";
            String subPath;
            javax.json.JsonObjectBuilder body = null;

            switch (resourceType.toUpperCase()) {
                case "ITEM": {
                    switch (method.toUpperCase()) {
                        case "LIST": subPath = basePath + "/items"; break;
                        case "GET":
                            if (itemId == null || itemId.isEmpty()) return "Error: item_id is required for ITEM GET.";
                            subPath = basePath + "/items/" + itemId; break;
                        case "POST":
                            subPath = basePath + "/items";
                            body = Json.createObjectBuilder();
                            if (!name.isEmpty()) body.add("name", name);
                            if (!code.isEmpty()) body.add("code", code);
                            if (!description.isEmpty()) body.add("description", description);
                            if (!orderNo.isEmpty()) body.add("orderNo", Integer.parseInt(orderNo));
                            if (!ixItemType.isEmpty()) body.add("ixItemType", ixItemType);
                            if (!ixItemValueType.isEmpty()) body.add("ixItemValueType", ixItemValueType);
                            if (!automated.isEmpty()) body.add("automated", Boolean.parseBoolean(automated));
                            if (!resultCode.isEmpty()) body.add("resultCode", resultCode);
                            if (!formatPrefix.isEmpty()) body.add("formatPrefix", formatPrefix);
                            if (!formatSuffix.isEmpty()) body.add("formatSuffix", formatSuffix);
                            if (!htmltext.isEmpty()) body.add("htmltext", htmltext);
                            if (!canNotApproveIfEmpty.isEmpty()) body.add("canNotApproveIfValueIsEmpty", Boolean.parseBoolean(canNotApproveIfEmpty));
                            if (!absoluteLowValue.isEmpty()) body.add("absoluteLowValue", Double.parseDouble(absoluteLowValue));
                            if (!absoluteHighValue.isEmpty()) body.add("absoluteHighValue", Double.parseDouble(absoluteHighValue));
                            break;
                        case "PUT":
                            if (itemId == null || itemId.isEmpty()) return "Error: item_id is required for ITEM PUT.";
                            subPath = basePath + "/items/" + itemId;
                            body = Json.createObjectBuilder();
                            if (!name.isEmpty()) body.add("name", name);
                            if (!code.isEmpty()) body.add("code", code);
                            if (!description.isEmpty()) body.add("description", description);
                            if (!orderNo.isEmpty()) body.add("orderNo", Integer.parseInt(orderNo));
                            if (!ixItemType.isEmpty()) body.add("ixItemType", ixItemType);
                            if (!ixItemValueType.isEmpty()) body.add("ixItemValueType", ixItemValueType);
                            if (!automated.isEmpty()) body.add("automated", Boolean.parseBoolean(automated));
                            if (!resultCode.isEmpty()) body.add("resultCode", resultCode);
                            if (!formatPrefix.isEmpty()) body.add("formatPrefix", formatPrefix);
                            if (!formatSuffix.isEmpty()) body.add("formatSuffix", formatSuffix);
                            if (!htmltext.isEmpty()) body.add("htmltext", htmltext);
                            if (!canNotApproveIfEmpty.isEmpty()) body.add("canNotApproveIfValueIsEmpty", Boolean.parseBoolean(canNotApproveIfEmpty));
                            if (!absoluteLowValue.isEmpty()) body.add("absoluteLowValue", Double.parseDouble(absoluteLowValue));
                            if (!absoluteHighValue.isEmpty()) body.add("absoluteHighValue", Double.parseDouble(absoluteHighValue));
                            break;
                        case "DELETE":
                            if (itemId == null || itemId.isEmpty()) return "Error: item_id is required for ITEM DELETE.";
                            subPath = basePath + "/items/" + itemId; break;
                        default: return "Error: Unsupported method for ITEM: " + method;
                    }
                    break;
                }
                case "VALUE": {
                    switch (method.toUpperCase()) {
                        case "LIST":
                            if (itemId == null || itemId.isEmpty()) return "Error: item_id is required for VALUE LIST.";
                            subPath = basePath + "/items/" + itemId + "/values"; break;
                        case "POST":
                            if (itemId == null || itemId.isEmpty()) return "Error: item_id is required for VALUE POST.";
                            subPath = basePath + "/items/" + itemId + "/values";
                            body = Json.createObjectBuilder();
                            if (!name.isEmpty()) body.add("name", name);
                            if (!code.isEmpty()) body.add("code", code);
                            if (!orderNo.isEmpty()) body.add("orderNo", Integer.parseInt(orderNo));
                            break;
                        case "PUT":
                            if (id == null || id.isEmpty()) return "Error: id is required for VALUE PUT.";
                            subPath = basePath + "/values/" + id;
                            body = Json.createObjectBuilder();
                            if (!name.isEmpty()) body.add("name", name);
                            if (!code.isEmpty()) body.add("code", code);
                            if (!orderNo.isEmpty()) body.add("orderNo", Integer.parseInt(orderNo));
                            break;
                        case "DELETE":
                            if (id == null || id.isEmpty()) return "Error: id is required for VALUE DELETE.";
                            subPath = basePath + "/values/" + id; break;
                        default: return "Error: Unsupported method for VALUE: " + method;
                    }
                    break;
                }
                case "CALCULATION": {
                    switch (method.toUpperCase()) {
                        case "LIST": subPath = basePath + "/calculations"; break;
                        case "POST":
                            subPath = basePath + "/calculations";
                            body = Json.createObjectBuilder();
                            if (!calIxItemId.isEmpty()) body.add("calIxItemId", Long.parseLong(calIxItemId));
                            if (!valIxItemId.isEmpty()) body.add("valIxItemId", Long.parseLong(valIxItemId));
                            if (!calculationType.isEmpty()) body.add("calculationType", calculationType);
                            if (!constantValue.isEmpty()) body.add("constantValue", Double.parseDouble(constantValue));
                            if (!maleConstantValue.isEmpty()) body.add("maleConstantValue", Double.parseDouble(maleConstantValue));
                            if (!femaleConstantValue.isEmpty()) body.add("femaleConstantValue", Double.parseDouble(femaleConstantValue));
                            if (!javascript.isEmpty()) body.add("javascript", javascript);
                            if (!orderNo.isEmpty()) body.add("orderNo", Integer.parseInt(orderNo));
                            break;
                        case "PUT":
                            if (id == null || id.isEmpty()) return "Error: id is required for CALCULATION PUT.";
                            subPath = basePath + "/calculations/" + id;
                            body = Json.createObjectBuilder();
                            if (!calIxItemId.isEmpty()) body.add("calIxItemId", Long.parseLong(calIxItemId));
                            if (!valIxItemId.isEmpty()) body.add("valIxItemId", Long.parseLong(valIxItemId));
                            if (!calculationType.isEmpty()) body.add("calculationType", calculationType);
                            if (!constantValue.isEmpty()) body.add("constantValue", Double.parseDouble(constantValue));
                            if (!maleConstantValue.isEmpty()) body.add("maleConstantValue", Double.parseDouble(maleConstantValue));
                            if (!femaleConstantValue.isEmpty()) body.add("femaleConstantValue", Double.parseDouble(femaleConstantValue));
                            if (!javascript.isEmpty()) body.add("javascript", javascript);
                            if (!orderNo.isEmpty()) body.add("orderNo", Integer.parseInt(orderNo));
                            break;
                        case "DELETE":
                            if (id == null || id.isEmpty()) return "Error: id is required for CALCULATION DELETE.";
                            subPath = basePath + "/calculations/" + id; break;
                        default: return "Error: Unsupported method for CALCULATION: " + method;
                    }
                    break;
                }
                case "FLAG": {
                    switch (method.toUpperCase()) {
                        case "LIST": subPath = basePath + "/flags"; break;
                        case "POST":
                            subPath = basePath + "/flags";
                            body = Json.createObjectBuilder();
                            if (!valueItemId.isEmpty()) body.add("investigationItemOfValueTypeId", Long.parseLong(valueItemId));
                            if (!flagItemId.isEmpty()) body.add("investigationItemOfFlagTypeId", Long.parseLong(flagItemId));
                            if (!sex.isEmpty()) body.add("sex", sex);
                            if (!fromAge.isEmpty()) body.add("fromAge", Long.parseLong(fromAge));
                            if (!toAge.isEmpty()) body.add("toAge", Long.parseLong(toAge));
                            if (!fromVal.isEmpty()) body.add("fromVal", Double.parseDouble(fromVal));
                            if (!toVal.isEmpty()) body.add("toVal", Double.parseDouble(toVal));
                            if (!flagMessage.isEmpty()) body.add("flagMessage", flagMessage);
                            if (!highMessage.isEmpty()) body.add("highMessage", highMessage);
                            if (!lowMessage.isEmpty()) body.add("lowMessage", lowMessage);
                            if (!normalMessage.isEmpty()) body.add("normalMessage", normalMessage);
                            if (!displayFlagMessage.isEmpty()) body.add("displayFlagMessage", Boolean.parseBoolean(displayFlagMessage));
                            if (!displayHighMessage.isEmpty()) body.add("displayHighMessage", Boolean.parseBoolean(displayHighMessage));
                            if (!displayLowMessage.isEmpty()) body.add("displayLowMessage", Boolean.parseBoolean(displayLowMessage));
                            if (!displayNormalMessage.isEmpty()) body.add("displayNormalMessage", Boolean.parseBoolean(displayNormalMessage));
                            break;
                        case "PUT":
                            if (id == null || id.isEmpty()) return "Error: id is required for FLAG PUT.";
                            subPath = basePath + "/flags/" + id;
                            body = Json.createObjectBuilder();
                            if (!valueItemId.isEmpty()) body.add("investigationItemOfValueTypeId", Long.parseLong(valueItemId));
                            if (!flagItemId.isEmpty()) body.add("investigationItemOfFlagTypeId", Long.parseLong(flagItemId));
                            if (!sex.isEmpty()) body.add("sex", sex);
                            if (!fromAge.isEmpty()) body.add("fromAge", Long.parseLong(fromAge));
                            if (!toAge.isEmpty()) body.add("toAge", Long.parseLong(toAge));
                            if (!fromVal.isEmpty()) body.add("fromVal", Double.parseDouble(fromVal));
                            if (!toVal.isEmpty()) body.add("toVal", Double.parseDouble(toVal));
                            if (!flagMessage.isEmpty()) body.add("flagMessage", flagMessage);
                            if (!highMessage.isEmpty()) body.add("highMessage", highMessage);
                            if (!lowMessage.isEmpty()) body.add("lowMessage", lowMessage);
                            if (!normalMessage.isEmpty()) body.add("normalMessage", normalMessage);
                            if (!displayFlagMessage.isEmpty()) body.add("displayFlagMessage", Boolean.parseBoolean(displayFlagMessage));
                            if (!displayHighMessage.isEmpty()) body.add("displayHighMessage", Boolean.parseBoolean(displayHighMessage));
                            if (!displayLowMessage.isEmpty()) body.add("displayLowMessage", Boolean.parseBoolean(displayLowMessage));
                            if (!displayNormalMessage.isEmpty()) body.add("displayNormalMessage", Boolean.parseBoolean(displayNormalMessage));
                            break;
                        case "DELETE":
                            if (id == null || id.isEmpty()) return "Error: id is required for FLAG DELETE.";
                            subPath = basePath + "/flags/" + id; break;
                        default: return "Error: Unsupported method for FLAG: " + method;
                    }
                    break;
                }
                case "DYNAMIC_LABEL": {
                    switch (method.toUpperCase()) {
                        case "LIST": subPath = basePath + "/dynamic-labels"; break;
                        case "POST":
                            subPath = basePath + "/dynamic-labels";
                            body = Json.createObjectBuilder();
                            if (!labelItemId.isEmpty()) body.add("investigationItemOfLabelTypeId", Long.parseLong(labelItemId));
                            if (!sex.isEmpty()) body.add("sex", sex);
                            if (!fromAge.isEmpty()) body.add("fromAge", Long.parseLong(fromAge));
                            if (!toAge.isEmpty()) body.add("toAge", Long.parseLong(toAge));
                            if (!flagMessage.isEmpty()) body.add("flagMessage", flagMessage);
                            break;
                        case "PUT":
                            if (id == null || id.isEmpty()) return "Error: id is required for DYNAMIC_LABEL PUT.";
                            subPath = basePath + "/dynamic-labels/" + id;
                            body = Json.createObjectBuilder();
                            if (!labelItemId.isEmpty()) body.add("investigationItemOfLabelTypeId", Long.parseLong(labelItemId));
                            if (!sex.isEmpty()) body.add("sex", sex);
                            if (!fromAge.isEmpty()) body.add("fromAge", Long.parseLong(fromAge));
                            if (!toAge.isEmpty()) body.add("toAge", Long.parseLong(toAge));
                            if (!flagMessage.isEmpty()) body.add("flagMessage", flagMessage);
                            break;
                        case "DELETE":
                            if (id == null || id.isEmpty()) return "Error: id is required for DYNAMIC_LABEL DELETE.";
                            subPath = basePath + "/dynamic-labels/" + id; break;
                        default: return "Error: Unsupported method for DYNAMIC_LABEL: " + method;
                    }
                    break;
                }
                default:
                    return "Error: Unsupported resource_type: " + resourceType
                            + ". Allowed: ITEM, VALUE, CALCULATION, FLAG, DYNAMIC_LABEL.";
            }

            HttpRequest.Builder rb;
            if ("DELETE".equalsIgnoreCase(method)) {
                rb = HttpRequest.newBuilder().uri(URI.create(subPath)).DELETE();
            } else if (body != null) {
                String httpMethod = "POST".equalsIgnoreCase(method) ? "POST" : "PUT";
                rb = HttpRequest.newBuilder().uri(URI.create(subPath))
                        .method(httpMethod, HttpRequest.BodyPublishers.ofString(body.build().toString()))
                        .header("Content-Type", "application/json");
            } else {
                rb = HttpRequest.newBuilder().uri(URI.create(subPath)).GET();
            }
            if (!key.isEmpty()) rb.header("Finance", key);
            HttpResponse<String> resp = client.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            return "HTTP " + resp.statusCode() + "\n" + resp.body();
        } catch (Exception e) {
            return "Investigation Format API error: " + e.getMessage();
        }
    }

    private String requireNumericId(String value, String fieldName) {
        if (value == null || !value.trim().matches("\\d+")) {
            throw new IllegalArgumentException("Error: " + fieldName + " must be a numeric id.");
        }
        return value.trim();
    }

    private String callFormsApi(
            String resourceType, String method, String id, String formId, String fieldId,
            String admissionId, String entryId, String name, String description, String formCssClass,
            String cpt, String cdt, String orderNo, String required, String placeholder,
            String minValue, String maxValue, String stepSize, String maxRating,
            String onLabel, String offLabel, String editHtml, String viewHtml,
            String label, String value,
            String hmisBaseUrl, String hmisApiKey) {
        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL not configured.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: HMIS API key not configured.";
        }
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            String base = hmisBaseUrl.trim().replaceAll("/+$", "") + "/api/forms";

            String url;
            String httpMethod;
            String requestBody = null;

            switch (resourceType.toUpperCase()) {
                case "TEMPLATE":
                    switch (method.toUpperCase()) {
                        case "LIST": url = base + "/templates"; httpMethod = "GET"; break;
                        case "GET":  url = base + "/templates/" + requireNumericId(id, "id"); httpMethod = "GET"; break;
                        case "POST": {
                            url = base + "/templates"; httpMethod = "POST";
                            Map<String, Object> body = new HashMap<>();
                            if (name != null) body.put("name", name);
                            if (description != null) body.put("description", description);
                            if (formCssClass != null) body.put("formCssClass", formCssClass);
                            requestBody = new com.google.gson.Gson().toJson(body);
                            break;
                        }
                        case "PUT": {
                            url = base + "/templates/" + requireNumericId(id, "id"); httpMethod = "PUT";
                            Map<String, Object> body = new HashMap<>();
                            if (name != null) body.put("name", name);
                            if (description != null) body.put("description", description);
                            if (formCssClass != null) body.put("formCssClass", formCssClass);
                            requestBody = new com.google.gson.Gson().toJson(body);
                            break;
                        }
                        case "DELETE": url = base + "/templates/" + requireNumericId(id, "id"); httpMethod = "DELETE"; break;
                        default: return "Unknown method: " + method;
                    }
                    break;

                case "FIELD":
                    switch (method.toUpperCase()) {
                        case "LIST": url = base + "/templates/" + requireNumericId(formId, "form_id") + "/fields"; httpMethod = "GET"; break;
                        case "POST": {
                            url = base + "/templates/" + requireNumericId(formId, "form_id") + "/fields"; httpMethod = "POST";
                            Map<String, Object> body = new HashMap<>();
                            if (name != null)        body.put("name", name);
                            if (description != null) body.put("description", description);
                            if (cpt != null)         body.put("componentPresentationType", cpt);
                            if (cdt != null)         body.put("componentDataType", cdt);
                            if (orderNo != null)     body.put("orderNo", orderNo);
                            if (required != null)    body.put("required", required);
                            if (placeholder != null) body.put("placeholder", placeholder);
                            if (minValue != null)    body.put("minValue", minValue);
                            if (maxValue != null)    body.put("maxValue", maxValue);
                            if (stepSize != null)    body.put("stepSize", stepSize);
                            if (maxRating != null)   body.put("maxRating", maxRating);
                            if (onLabel != null)     body.put("onLabel", onLabel);
                            if (offLabel != null)    body.put("offLabel", offLabel);
                            if (editHtml != null)    body.put("editHtml", editHtml);
                            if (viewHtml != null)    body.put("viewHtml", viewHtml);
                            requestBody = new com.google.gson.Gson().toJson(body);
                            break;
                        }
                        case "PUT": {
                            url = base + "/fields/" + requireNumericId(id, "id"); httpMethod = "PUT";
                            Map<String, Object> body = new HashMap<>();
                            if (name != null)        body.put("name", name);
                            if (description != null) body.put("description", description);
                            if (cpt != null)         body.put("componentPresentationType", cpt);
                            if (cdt != null)         body.put("componentDataType", cdt);
                            if (orderNo != null)     body.put("orderNo", orderNo);
                            if (required != null)    body.put("required", required);
                            if (placeholder != null) body.put("placeholder", placeholder);
                            if (minValue != null)    body.put("minValue", minValue);
                            if (maxValue != null)    body.put("maxValue", maxValue);
                            if (stepSize != null)    body.put("stepSize", stepSize);
                            if (maxRating != null)   body.put("maxRating", maxRating);
                            if (onLabel != null)     body.put("onLabel", onLabel);
                            if (offLabel != null)    body.put("offLabel", offLabel);
                            if (editHtml != null)    body.put("editHtml", editHtml);
                            if (viewHtml != null)    body.put("viewHtml", viewHtml);
                            requestBody = new com.google.gson.Gson().toJson(body);
                            break;
                        }
                        case "DELETE": url = base + "/fields/" + requireNumericId(id, "id"); httpMethod = "DELETE"; break;
                        default: return "Unknown method: " + method;
                    }
                    break;

                case "CHOICE":
                    switch (method.toUpperCase()) {
                        case "LIST": url = base + "/fields/" + requireNumericId(fieldId, "field_id") + "/choices"; httpMethod = "GET"; break;
                        case "POST": {
                            url = base + "/fields/" + requireNumericId(fieldId, "field_id") + "/choices"; httpMethod = "POST";
                            Map<String, Object> body = new HashMap<>();
                            if (label != null)   body.put("label", label);
                            if (value != null)   body.put("value", value);
                            if (orderNo != null) body.put("orderNo", orderNo);
                            requestBody = new com.google.gson.Gson().toJson(body);
                            break;
                        }
                        case "PUT": {
                            url = base + "/choices/" + requireNumericId(id, "id"); httpMethod = "PUT";
                            Map<String, Object> body = new HashMap<>();
                            if (label != null)   body.put("label", label);
                            if (value != null)   body.put("value", value);
                            if (orderNo != null) body.put("orderNo", orderNo);
                            requestBody = new com.google.gson.Gson().toJson(body);
                            break;
                        }
                        case "DELETE": url = base + "/choices/" + requireNumericId(id, "id"); httpMethod = "DELETE"; break;
                        default: return "Unknown method: " + method;
                    }
                    break;

                case "ENTRY":
                    if (!"LIST".equalsIgnoreCase(method)) return "Unknown method: " + method + " for ENTRY";
                    url = base + "/entries/" + requireNumericId(admissionId, "admission_id"); httpMethod = "GET"; break;

                case "VALUE":
                    if (!"LIST".equalsIgnoreCase(method)) return "Unknown method: " + method + " for VALUE";
                    url = base + "/entries/" + requireNumericId(entryId, "entry_id") + "/values"; httpMethod = "GET"; break;

                default:
                    return "Unknown resource_type: " + resourceType;
            }

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Finance", hmisApiKey);

            if (requestBody != null) {
                reqBuilder.header("Content-Type", "application/json");
                if ("POST".equals(httpMethod)) {
                    reqBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody));
                } else {
                    reqBuilder.PUT(HttpRequest.BodyPublishers.ofString(requestBody));
                }
            } else if ("DELETE".equals(httpMethod)) {
                reqBuilder.DELETE();
            } else {
                reqBuilder.GET();
            }

            HttpResponse<String> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return "HTTP " + response.statusCode() + ": " + response.body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Forms API call interrupted.";
        } catch (Exception e) {
            return "Forms API error: " + e.getMessage();
        }
    }

    private String callInpatientTemplateApi(String method, String id, String templateType,
            String name, String contents, String defaultTemplate, String autoGenerate,
            String query, String size, String hmisBaseUrl, String hmisApiKey) {
        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: HMIS API key is not configured.";
        }
        try {
            String baseUrl = hmisBaseUrl.replaceAll("/$", "") + "/api/inward/document-templates";
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

            switch (method.toUpperCase()) {
                case "LIST": {
                    StringBuilder url = new StringBuilder(baseUrl).append("?size=").append(size.isEmpty() ? "200" : size);
                    if (!templateType.isEmpty()) url.append("&type=").append(URLEncoder.encode(templateType, StandardCharsets.UTF_8));
                    if (!query.isEmpty()) url.append("&query=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url.toString()))
                            .timeout(Duration.ofSeconds(15))
                            .header("Finance", hmisApiKey).GET().build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    return resp.body();
                }
                case "GET": {
                    if (id.isEmpty()) return "Error: id is required for GET.";
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/" + id))
                            .timeout(Duration.ofSeconds(15))
                            .header("Finance", hmisApiKey).GET().build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    return resp.body();
                }
                case "POST": {
                    if (name.isEmpty()) return "Error: name is required for POST.";
                    if (templateType.isEmpty()) return "Error: type is required for POST.";
                    javax.json.JsonObjectBuilder bodyBuilder = Json.createObjectBuilder()
                            .add("name", name)
                            .add("type", templateType);
                    if (!contents.isEmpty()) bodyBuilder.add("contents", contents);
                    if (!defaultTemplate.isEmpty()) bodyBuilder.add("defaultTemplate", Boolean.parseBoolean(defaultTemplate));
                    if (!autoGenerate.isEmpty()) bodyBuilder.add("autoGenerate", Boolean.parseBoolean(autoGenerate));
                    String bodyStr = bodyBuilder.build().toString();
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl))
                            .timeout(Duration.ofSeconds(15))
                            .header("Finance", hmisApiKey).header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(bodyStr)).build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    return resp.body();
                }
                case "PUT": {
                    if (id.isEmpty()) return "Error: id is required for PUT.";
                    javax.json.JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();
                    if (!name.isEmpty()) bodyBuilder.add("name", name);
                    if (!templateType.isEmpty()) bodyBuilder.add("type", templateType);
                    if (!contents.isEmpty()) bodyBuilder.add("contents", contents);
                    if (!defaultTemplate.isEmpty()) bodyBuilder.add("defaultTemplate", Boolean.parseBoolean(defaultTemplate));
                    if (!autoGenerate.isEmpty()) bodyBuilder.add("autoGenerate", Boolean.parseBoolean(autoGenerate));
                    String bodyStr = bodyBuilder.build().toString();
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/" + id))
                            .timeout(Duration.ofSeconds(15))
                            .header("Finance", hmisApiKey).header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofString(bodyStr)).build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    return resp.body();
                }
                case "DELETE": {
                    if (id.isEmpty()) return "Error: id is required for DELETE.";
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/" + id))
                            .timeout(Duration.ofSeconds(15))
                            .header("Finance", hmisApiKey).DELETE().build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    return resp.body();
                }
                default:
                    return "Unknown method: " + method + ". Valid: LIST, GET, POST, PUT, DELETE";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Inpatient template API call interrupted.";
        } catch (Exception e) {
            return "Inpatient template API error: " + e.getMessage();
        }
    }

    private String callStaffApi(JsonObject input, String hmisBaseUrl, String hmisApiKey) {
        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: HMIS API key is not configured.";
        }
        String method = input.getString("method", "LIST").toUpperCase();
        String id = jsonString(input, "id");
        try {
            String base = hmisBaseUrl.replaceAll("/$", "") + "/api/staff";
            String url = base;
            String httpMethod = "GET";
            String body = null;
            switch (method) {
                case "LIST":
                    url = base + "?" + queryParam("query", jsonString(input, "query"))
                            + "&" + queryParam("departmentId", jsonString(input, "departmentId"))
                            + "&" + queryParam("size", defaultString(jsonString(input, "size"), "50"));
                    break;
                case "GET":
                    url = base + "/" + requireText(id, "id");
                    break;
                case "POST": {
                    httpMethod = "POST";
                    javax.json.JsonObjectBuilder b = Json.createObjectBuilder();
                    addString(b, "name", jsonString(input, "name"));
                    addString(b, "code", jsonString(input, "code"));
                    addString(b, "designation", jsonString(input, "designation"));
                    addLong(b, "departmentId", jsonString(input, "departmentId"));
                    addLong(b, "institutionId", jsonString(input, "institutionId"));
                    if (jsonString(input, "name").isEmpty()) throw new IllegalArgumentException("name is required for POST");
                    body = b.build().toString();
                    break;
                }
                case "PUT": {
                    httpMethod = "PUT";
                    url = base + "/" + requireText(id, "id");
                    javax.json.JsonObjectBuilder b = Json.createObjectBuilder();
                    addString(b, "name", jsonString(input, "name"));
                    addString(b, "code", jsonString(input, "code"));
                    addString(b, "designation", jsonString(input, "designation"));
                    addLong(b, "departmentId", jsonString(input, "departmentId"));
                    addLong(b, "institutionId", jsonString(input, "institutionId"));
                    body = b.build().toString();
                    break;
                }
                case "DELETE":
                    httpMethod = "DELETE";
                    url = base + "/" + requireText(id, "id");
                    String retireComments = jsonString(input, "retireComments");
                    if (!retireComments.isEmpty()) url += "?" + queryParam("retireComments", retireComments);
                    break;
                case "LINK_TO_USER": {
                    // PUT /api/users/{userId}/staff  with body {staffId}
                    httpMethod = "PUT";
                    String usersBase = hmisBaseUrl.replaceAll("/$", "") + "/api/users";
                    url = usersBase + "/" + requireText(id, "id") + "/staff";
                    body = Json.createObjectBuilder()
                            .add("staffId", Long.parseLong(requireText(jsonString(input, "staffId"), "staffId")))
                            .build().toString();
                    break;
                }
                default:
                    return "Unknown method: " + method;
            }
            return callHmisApi(url, httpMethod, body, hmisApiKey);
        } catch (Exception e) {
            return "Staff API error: " + e.getMessage();
        }
    }

    private String callUsersApi(JsonObject input, String hmisBaseUrl, String hmisApiKey) {
        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: HMIS API key is not configured.";
        }
        String method = input.getString("method", "LIST").toUpperCase();
        String id = jsonString(input, "id");
        String departmentId = jsonString(input, "departmentId");
        try {
            String base = hmisBaseUrl.replaceAll("/$", "") + "/api/users";
            String url = base;
            String httpMethod = "GET";
            String body = null;

            switch (method) {
                case "LIST":
                    url = base + "?" + queryParam("query", jsonString(input, "query"))
                            + "&" + queryParam("departmentId", departmentId)
                            + "&" + queryParam("page", defaultString(jsonString(input, "page"), "0"))
                            + "&" + queryParam("size", defaultString(jsonString(input, "size"), "20"));
                    break;
                case "GET":
                    url = base + "/" + requireText(id, "id");
                    break;
                case "POST":
                    httpMethod = "POST";
                    body = userBody(input, true);
                    break;
                case "PUT":
                    httpMethod = "PUT";
                    url = base + "/" + requireText(id, "id");
                    body = userBody(input, false);
                    break;
                case "DELETE":
                    httpMethod = "DELETE";
                    url = base + "/" + requireText(id, "id");
                    String retireComments = jsonString(input, "retireComments");
                    if (!retireComments.isEmpty()) url += "?" + queryParam("retireComments", retireComments);
                    break;
                case "RESET_PASSWORD":
                    httpMethod = "POST";
                    url = base + "/" + requireText(id, "id") + "/reset-password";
                    body = Json.createObjectBuilder().add("newPassword", requireText(jsonString(input, "newPassword"), "newPassword")).build().toString();
                    break;
                case "CHANGE_PASSWORD":
                    httpMethod = "POST";
                    url = base + "/" + requireText(id, "id") + "/change-password";
                    javax.json.JsonObjectBuilder change = Json.createObjectBuilder()
                            .add("newPassword", requireText(jsonString(input, "newPassword"), "newPassword"));
                    addString(change, "currentPassword", jsonString(input, "currentPassword"));
                    body = change.build().toString();
                    break;
                case "FORCE_PASSWORD_RESET":
                    httpMethod = "POST";
                    url = base + "/" + requireText(id, "id") + "/force-password-reset";
                    break;
                case "PASSWORD_STATUS":
                    url = base + "/password-status?" + queryParam("from", jsonString(input, "from"))
                            + "&" + queryParam("to", jsonString(input, "to"));
                    break;
                case "LIST_PRIVILEGES":
                    url = base + "/" + requireText(id, "id") + "/privileges";
                    break;
                case "ASSIGN_PRIVILEGES":
                    httpMethod = "POST";
                    url = base + "/" + requireText(id, "id") + "/privileges";
                    body = Json.createObjectBuilder()
                            .add("departmentId", parseLongRequired(departmentId, "departmentId"))
                            .add("privileges", csvArray(jsonString(input, "privileges")))
                            .build().toString();
                    break;
                case "REVOKE_PRIVILEGE":
                    httpMethod = "DELETE";
                    url = base + "/" + requireText(id, "id") + "/privileges/" + requireText(jsonString(input, "privilegeId"), "privilegeId");
                    break;
                case "LIST_DEPARTMENTS":
                    url = base + "/" + requireText(id, "id") + "/departments";
                    break;
                case "ASSIGN_DEPARTMENTS":
                    httpMethod = "POST";
                    url = base + "/" + requireText(id, "id") + "/departments";
                    body = Json.createObjectBuilder()
                            .add("departmentIds", csvLongArray(jsonString(input, "departmentIds")))
                            .build().toString();
                    break;
                case "LIST_AVAILABLE_PRIVILEGES":
                    url = base + "/privileges/available";
                    break;
                case "BULK_ASSIGN_PRIVILEGES": {
                    httpMethod = "POST";
                    url = base + "/bulk-privileges";
                    javax.json.JsonObjectBuilder bulk = Json.createObjectBuilder()
                            .add("userIds", csvLongArray(jsonString(input, "userIds")))
                            .add("privileges", csvArray(jsonString(input, "privileges")));
                    if (!departmentId.isEmpty()) bulk.add("departmentId", parseLongRequired(departmentId, "departmentId"));
                    body = bulk.build().toString();
                    break;
                }
                case "ASSIGN_PRIVILEGE_CATEGORIES":
                    httpMethod = "POST";
                    url = base + "/" + requireText(id, "id") + "/departments/" + requireText(departmentId, "departmentId") + "/privileges/category";
                    body = Json.createObjectBuilder()
                            .add("categories", csvArray(jsonString(input, "categories")))
                            .build().toString();
                    break;
                case "ASSIGN_ALL_PRIVILEGES_MULTI_DEPT": {
                    httpMethod = "POST";
                    url = base + "/" + requireText(id, "id") + "/privileges/all";
                    String deptIdsStr = jsonString(input, "departmentIds");
                    if (!deptIdsStr.isEmpty()) {
                        javax.json.JsonArrayBuilder deptArr = Json.createArrayBuilder();
                        for (String d : deptIdsStr.split(",")) {
                            d = d.trim();
                            if (!d.isEmpty()) deptArr.add(Long.parseLong(d));
                        }
                        body = Json.createObjectBuilder().add("departmentIds", deptArr).build().toString();
                    }
                    break;
                }
                default:
                    return "Unknown method: " + method;
            }
            return callHmisApi(url, httpMethod, body, hmisApiKey);
        } catch (Exception e) {
            return "Users API error: " + e.getMessage();
        }
    }

    private String callUserRoleOperationApi(String action, JsonObject input, boolean roleRequired, String hmisBaseUrl, String hmisApiKey) {
        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: HMIS API key is not configured.";
        }
        try {
            String id = requireText(jsonString(input, "id"), "id");
            String url = hmisBaseUrl.replaceAll("/$", "") + "/api/users/" + id + "/role/" + action;
            String roleId = jsonString(input, "roleId");
            if (roleRequired && roleId.isEmpty()) {
                return "Error: roleId is required for role " + action + ".";
            }
            javax.json.JsonObjectBuilder b = Json.createObjectBuilder();
            addLong(b, "roleId", roleId);
            b.add("departmentIds", csvLongArray(jsonString(input, "departmentIds")));
            String aspects = jsonString(input, "aspects");
            if (!aspects.isEmpty()) b.add("aspects", csvArray(aspects));
            addBoolean(b, "updateUserRole", jsonString(input, "updateUserRole"));
            addBoolean(b, "preview", jsonString(input, "preview"));
            return callHmisApi(url, "POST", b.build().toString(), hmisApiKey);
        } catch (Exception e) {
            return "User role " + action + " error: " + e.getMessage();
        }
    }

    private String callUserBulkRoleOperationsApi(JsonObject input, String hmisBaseUrl, String hmisApiKey) {
        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: HMIS API key is not configured.";
        }
        try {
            String url = hmisBaseUrl.replaceAll("/$", "") + "/api/users/bulk/role-operations";
            javax.json.JsonObjectBuilder b = Json.createObjectBuilder();
            b.add("action", requireText(jsonString(input, "action"), "action"));
            String userIds = jsonString(input, "userIds");
            if (!userIds.isEmpty()) {
                b.add("userIds", csvLongArray(userIds));
            } else {
                String filterRoleId = jsonString(input, "filterRoleId");
                String filterDepartmentId = jsonString(input, "filterDepartmentId");
                if (!filterRoleId.isEmpty() || !filterDepartmentId.isEmpty()) {
                    javax.json.JsonObjectBuilder filter = Json.createObjectBuilder();
                    addLong(filter, "roleId", filterRoleId);
                    addLong(filter, "departmentId", filterDepartmentId);
                    b.add("filter", filter);
                }
            }
            addLong(b, "roleId", jsonString(input, "roleId"));
            b.add("departmentIds", csvLongArray(jsonString(input, "departmentIds")));
            String aspects = jsonString(input, "aspects");
            if (!aspects.isEmpty()) b.add("aspects", csvArray(aspects));
            addBoolean(b, "updateUserRole", jsonString(input, "updateUserRole"));
            addBoolean(b, "preview", jsonString(input, "preview"));
            addBoolean(b, "confirm", jsonString(input, "confirm"));
            return callHmisApi(url, "POST", b.build().toString(), hmisApiKey);
        } catch (Exception e) {
            return "Bulk role operations error: " + e.getMessage();
        }
    }

    private String callListUserRolesApi(String hmisBaseUrl, String hmisApiKey) {
        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: HMIS API key is not configured.";
        }
        try {
            String url = hmisBaseUrl.replaceAll("/$", "") + "/api/users/roles";
            return callHmisApi(url, "GET", null, hmisApiKey);
        } catch (Exception e) {
            return "List user roles error: " + e.getMessage();
        }
    }

    private String callSetUserLoginPageApi(JsonObject input, String hmisBaseUrl, String hmisApiKey) {
        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: HMIS API key is not configured.";
        }
        try {
            String id = requireText(jsonString(input, "id"), "id");
            String departmentId = requireText(jsonString(input, "departmentId"), "departmentId");
            String action = input.containsKey("action") ? input.getString("action", "SET").toUpperCase() : "SET";
            String base = hmisBaseUrl.replaceAll("/$", "") + "/api/users/" + id + "/login-page";
            if ("DELETE".equals(action)) {
                return callHmisApi(base + "/" + departmentId, "DELETE", null, hmisApiKey);
            }
            String loginPage = requireText(jsonString(input, "loginPage"), "loginPage");
            String body = Json.createObjectBuilder()
                    .add("departmentId", Long.parseLong(departmentId))
                    .add("loginPage", loginPage)
                    .build().toString();
            return callHmisApi(base, "PUT", body, hmisApiKey);
        } catch (Exception e) {
            return "Set user login page error: " + e.getMessage();
        }
    }

    private String callPharmacyItemsApi(JsonObject input, String hmisBaseUrl, String hmisApiKey) {
        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: HMIS API key is not configured.";
        }
        String method = input.getString("method", "SEARCH").toUpperCase();
        String id = jsonString(input, "id");
        try {
            String base = hmisBaseUrl.replaceAll("/$", "") + "/api/pharmacy/items";
            String url = base;
            String httpMethod = "GET";
            String body = null;
            switch (method) {
                case "SEARCH":
                    url = base + "/search?" + queryParam("query", jsonString(input, "query"))
                            + "&" + queryParam("institutionId", jsonString(input, "institutionId"))
                            + "&" + queryParam("departmentId", jsonString(input, "departmentId"))
                            + "&" + queryParam("size", defaultString(jsonString(input, "size"), "50"));
                    break;
                case "GET":
                    url = base + "/" + requireText(id, "id");
                    break;
                case "POST":
                    httpMethod = "POST";
                    body = pharmacyItemBody(input, true);
                    break;
                case "PUT":
                    httpMethod = "PUT";
                    url = base + "/" + requireText(id, "id");
                    body = pharmacyItemBody(input, false);
                    break;
                case "DELETE":
                    httpMethod = "DELETE";
                    url = base + "/" + requireText(id, "id");
                    String retireComments = jsonString(input, "retireComments");
                    if (!retireComments.isEmpty()) url += "?" + queryParam("retireComments", retireComments);
                    break;
                default:
                    return "Unknown method: " + method;
            }
            return callHmisApi(url, httpMethod, body, hmisApiKey);
        } catch (Exception e) {
            return "Pharmacy items API error: " + e.getMessage();
        }
    }

    private String callChannelBookingApi(JsonObject input, String hmisBaseUrl, String hmisApiKey) {
        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: HMIS API key is not configured.";
        }
        String operation = input.getString("operation", "").toUpperCase();
        String path;
        switch (operation) {
            case "SPECIALIZATIONS": path = "/specializations"; break;
            case "HOSPITALS": path = "/hospitals"; break;
            case "DOCTORS": path = "/doctors"; break;
            case "DOCTOR_AVAILABILITY": path = "/doctorAvailability"; break;
            case "DOCTOR_SESSIONS": path = "/doctorSessions"; break;
            case "DOCTOR_SESSION": path = "/doctorSession"; break;
            case "SAVE": path = "/save"; break;
            case "EDIT": path = "/edit"; break;
            case "COMPLETE": path = "/complete"; break;
            case "CHANNEL_HISTORY_LIST": path = "/channelHistoryList"; break;
            case "CHANNEL_HISTORY_BY_REF": path = "/channelHistoryByRef"; break;
            case "CANCELLATION": path = "/cancellation"; break;
            default: return "Unknown operation: " + operation;
        }
        String body = jsonString(input, "requestBody");
        if (body.isEmpty()) body = "{}";
        try {
            String url = hmisBaseUrl.replaceAll("/$", "") + "/api/channel" + path;
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Token", hmisApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return "HTTP " + response.statusCode() + ": " + response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Channel booking API call interrupted.";
        } catch (Exception e) {
            return "Channel booking API error: " + e.getMessage();
        }
    }

    private String userBody(JsonObject input, boolean create) {
        javax.json.JsonObjectBuilder body = Json.createObjectBuilder();
        addString(body, "name", jsonString(input, "name"));
        addString(body, "code", jsonString(input, "code"));
        addString(body, "email", jsonString(input, "email"));
        addString(body, "telNo", jsonString(input, "telNo"));
        addString(body, "personName", jsonString(input, "personName"));
        addString(body, "personMobile", jsonString(input, "personMobile"));
        addLong(body, "institutionId", jsonString(input, "institutionId"));
        addLong(body, "siteId", jsonString(input, "siteId"));
        addLong(body, "departmentId", jsonString(input, "departmentId"));
        addLong(body, "roleId", jsonString(input, "roleId"));
        addBoolean(body, "activated", jsonString(input, "activated"));
        addString(body, "loginPage", jsonString(input, "loginPage"));
        addString(body, "password", jsonString(input, "password"));
        addLong(body, "staffId", jsonString(input, "staffId"));
        if (create && jsonString(input, "password").isEmpty()) {
            throw new IllegalArgumentException("password is required for POST");
        }
        return body.build().toString();
    }

    private String callPharmacyDiscountsApi(JsonObject input, String hmisBaseUrl, String hmisApiKey) {
        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: HMIS API key is not configured.";
        }
        String method = input.getString("method", "LIST").toUpperCase();
        String id = jsonString(input, "id");
        String base = hmisBaseUrl.replaceAll("/$", "") + "/api/pharmacy/discounts";
        try {
            String url = base;
            String httpMethod;
            String body = null;
            switch (method) {
                case "LIST":
                    httpMethod = "GET";
                    url = base + "?" + queryParam("paymentSchemeId", jsonString(input, "paymentSchemeId"))
                            + "&" + queryParam("paymentSchemeName", jsonString(input, "paymentSchemeName"))
                            + "&" + queryParam("billType", jsonString(input, "billType"))
                            + "&" + queryParam("limit", defaultString(jsonString(input, "limit"), "200"));
                    break;
                case "POST": {
                    httpMethod = "POST";
                    javax.json.JsonObjectBuilder b = Json.createObjectBuilder();
                    addLong(b, "categoryId", jsonString(input, "categoryId"));
                    addLong(b, "paymentSchemeId", jsonString(input, "paymentSchemeId"));
                    addString(b, "paymentSchemeName", jsonString(input, "paymentSchemeName"));
                    addString(b, "paymentMethod", jsonString(input, "paymentMethod"));
                    addString(b, "billType", jsonString(input, "billType"));
                    addDouble(b, "discountPercent", jsonString(input, "discountPercent"));
                    body = b.build().toString();
                    break;
                }
                case "BULK": {
                    httpMethod = "POST";
                    url = base + "/bulk";
                    javax.json.JsonObjectBuilder b = Json.createObjectBuilder();
                    addLong(b, "paymentSchemeId", jsonString(input, "paymentSchemeId"));
                    addString(b, "paymentSchemeName", jsonString(input, "paymentSchemeName"));
                    addString(b, "paymentMethod", jsonString(input, "paymentMethod"));
                    addString(b, "billType", jsonString(input, "billType"));
                    addDouble(b, "discountPercent", jsonString(input, "discountPercent"));
                    body = b.build().toString();
                    break;
                }
                case "PUT": {
                    httpMethod = "PUT";
                    url = base + "/" + requireText(id, "id");
                    javax.json.JsonObjectBuilder b = Json.createObjectBuilder();
                    addDouble(b, "discountPercent", jsonString(input, "discountPercent"));
                    body = b.build().toString();
                    break;
                }
                case "DELETE":
                    httpMethod = "DELETE";
                    url = base + "/" + requireText(id, "id");
                    break;
                default:
                    return "Unknown method: " + method + ". Use LIST, POST, BULK, PUT, or DELETE.";
            }
            return callHmisApi(url, httpMethod, body, hmisApiKey);
        } catch (Exception e) {
            return "Pharmacy discounts API error: " + e.getMessage();
        }
    }

    private String callPaymentSchemeApi(JsonObject input, String hmisBaseUrl, String hmisApiKey) {
        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: HMIS API key is not configured.";
        }
        String method = input.getString("method", "LIST").toUpperCase();
        String id = jsonString(input, "id");
        String base = hmisBaseUrl.replaceAll("/$", "") + "/api/payment-scheme";
        try {
            String url;
            String httpMethod;
            String body = null;
            switch (method) {
                case "LIST":
                    httpMethod = "GET";
                    url = base + "?" + queryParam("query", jsonString(input, "query"))
                            + "&" + queryParam("limit", defaultString(jsonString(input, "limit"), "500"));
                    break;
                case "UPDATE": {
                    httpMethod = "PUT";
                    url = base + "/" + requireText(id, "id");
                    javax.json.JsonObjectBuilder b = Json.createObjectBuilder();
                    addString(b, "name", jsonString(input, "name"));
                    addString(b, "printingName", jsonString(input, "printingName"));
                    addBoolean(b, "validForInpatientBills", jsonString(input, "validForInpatientBills"));
                    addBoolean(b, "validForPharmacy", jsonString(input, "validForPharmacy"));
                    addBoolean(b, "validForBilledBills", jsonString(input, "validForBilledBills"));
                    addBoolean(b, "validForChanneling", jsonString(input, "validForChanneling"));
                    addBoolean(b, "staffMemberRequired", jsonString(input, "staffMemberRequired"));
                    addBoolean(b, "membershipRequired", jsonString(input, "membershipRequired"));
                    addBoolean(b, "staffRequired", jsonString(input, "staffRequired"));
                    addBoolean(b, "staffOrFamilyRequired", jsonString(input, "staffOrFamilyRequired"));
                    addBoolean(b, "memberRequired", jsonString(input, "memberRequired"));
                    addBoolean(b, "memberOrFamilyRequired", jsonString(input, "memberOrFamilyRequired"));
                    addBoolean(b, "seniorCitizenRequired", jsonString(input, "seniorCitizenRequired"));
                    addBoolean(b, "pregnantMotherRequired", jsonString(input, "pregnantMotherRequired"));
                    addLong(b, "orderNo", jsonString(input, "orderNo"));
                    body = b.build().toString();
                    break;
                }
                default:
                    return "Unknown method: " + method + ". Use LIST or UPDATE.";
            }
            return callHmisApi(url, httpMethod, body, hmisApiKey);
        } catch (Exception e) {
            return "Payment scheme API error: " + e.getMessage();
        }
    }

    private String pharmacyItemBody(JsonObject input, boolean create) {
        javax.json.JsonObjectBuilder body = Json.createObjectBuilder();
        addString(body, "name", jsonString(input, "name"));
        addString(body, "code", jsonString(input, "code"));
        addLong(body, "categoryId", jsonString(input, "categoryId"));
        addLong(body, "dosageFormId", jsonString(input, "dosageFormId"));
        addLong(body, "ampId", jsonString(input, "ampId"));
        addLong(body, "institutionId", jsonString(input, "institutionId"));
        addLong(body, "departmentId", jsonString(input, "departmentId"));
        addDouble(body, "retailRate", jsonString(input, "retailRate"));
        addBoolean(body, "allowFractions", jsonString(input, "allowFractions"));
        addBoolean(body, "discountAllowed", jsonString(input, "discountAllowed"));
        if (create && jsonString(input, "name").isEmpty()) {
            throw new IllegalArgumentException("name is required for POST");
        }
        return body.build().toString();
    }

    private String callHmisApi(String url, String method, String body, String hmisApiKey) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Finance", hmisApiKey);
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        switch (method) {
            case "POST":
                builder.POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                break;
            case "PUT":
                builder.PUT(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                break;
            case "DELETE":
                builder.DELETE();
                break;
            default:
                builder.GET();
        }
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return "HTTP " + response.statusCode() + ": " + response.body();
    }

    private String jsonString(JsonObject input, String key) {
        return input.containsKey(key) && !input.isNull(key) ? input.getString(key, "").trim() : "";
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    private String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String queryParam(String key, String value) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8) + "="
                + URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private long parseLongRequired(String value, String field) {
        try {
            return Long.parseLong(requireText(value, field));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " must be numeric");
        }
    }

    private javax.json.JsonArrayBuilder csvArray(String csv) {
        javax.json.JsonArrayBuilder array = Json.createArrayBuilder();
        if (csv == null || csv.trim().isEmpty()) {
            throw new IllegalArgumentException("comma-separated values are required");
        }
        for (String value : csv.split(",")) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) array.add(trimmed);
        }
        return array;
    }

    private javax.json.JsonArrayBuilder csvLongArray(String csv) {
        javax.json.JsonArrayBuilder array = Json.createArrayBuilder();
        if (csv == null || csv.trim().isEmpty()) {
            throw new IllegalArgumentException("comma-separated numeric values are required");
        }
        for (String value : csv.split(",")) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) array.add(parseLongRequired(trimmed, "array value"));
        }
        return array;
    }

    private void addString(javax.json.JsonObjectBuilder body, String key, String value) {
        if (value != null && !value.isEmpty()) body.add(key, value);
    }

    private void addLong(javax.json.JsonObjectBuilder body, String key, String value) {
        if (value != null && !value.isEmpty()) body.add(key, parseLongRequired(value, key));
    }

    private void addDouble(javax.json.JsonObjectBuilder body, String key, String value) {
        if (value != null && !value.isEmpty()) {
            try {
                body.add(key, Double.parseDouble(value));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(key + " must be numeric");
            }
        }
    }

    private void addBoolean(javax.json.JsonObjectBuilder body, String key, String value) {
        if (value == null || value.isEmpty()) return;
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        if ("true".equals(normalized)) { body.add(key, true); return; }
        if ("false".equals(normalized)) { body.add(key, false); return; }
        throw new IllegalArgumentException(key + " must be 'true' or 'false', got: " + value);
    }

    private String callTimedItemsApi(String method, String id, String feeId, String name, String code,
            String departmentType, String inwardChargeType, String departmentId, String institutionId,
            String categoryId, String inactive, String fee, String ffee, String durationHours, String overShootHours,
            String durationDaysForMoCharge, String sortOrder, String repeating,
            String query, String size, String retireComments,
            String hmisBaseUrl, String hmisApiKey) {
        if (hmisBaseUrl == null || hmisBaseUrl.trim().isEmpty()) {
            return "Error: HMIS base URL is not configured.";
        }
        if (hmisApiKey == null || hmisApiKey.trim().isEmpty()) {
            return "Error: HMIS API key is not configured.";
        }
        try {
            String baseUrl = hmisBaseUrl.replaceAll("/$", "") + "/api/timed-items";
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

            switch (method.toUpperCase()) {
                case "LIST": {
                    StringBuilder url = new StringBuilder(baseUrl).append("/search?limit=").append(size.isEmpty() ? "30" : size);
                    if (!query.isEmpty()) url.append("&query=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
                    if (!departmentType.isEmpty()) url.append("&departmentType=").append(URLEncoder.encode(departmentType, StandardCharsets.UTF_8));
                    if (!inactive.isEmpty()) url.append("&inactive=").append(URLEncoder.encode(inactive, StandardCharsets.UTF_8));
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url.toString()))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey).GET().build();
                    return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
                }
                case "GET": {
                    if (id.isEmpty()) return "Error: id is required for GET.";
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/" + id))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey).GET().build();
                    return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
                }
                case "POST": {
                    if (name.isEmpty()) return "Error: name is required for POST.";
                    if (departmentType.isEmpty()) return "Error: departmentType is required for POST.";
                    if (inwardChargeType.isEmpty()) return "Error: inwardChargeType is required for POST.";
                    javax.json.JsonObjectBuilder b = Json.createObjectBuilder()
                            .add("name", name)
                            .add("departmentType", departmentType)
                            .add("inwardChargeType", inwardChargeType);
                    if (!code.isEmpty()) b.add("code", code);
                    if (!departmentId.isEmpty()) b.add("departmentId", Long.parseLong(departmentId));
                    if (!institutionId.isEmpty()) b.add("institutionId", Long.parseLong(institutionId));
                    if (!categoryId.isEmpty()) b.add("categoryId", Long.parseLong(categoryId));
                    if (!inactive.isEmpty()) b.add("inactive", Boolean.parseBoolean(inactive));
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(b.build().toString())).build();
                    return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
                }
                case "PUT": {
                    if (id.isEmpty()) return "Error: id is required for PUT.";
                    javax.json.JsonObjectBuilder b = Json.createObjectBuilder();
                    if (!name.isEmpty()) b.add("name", name);
                    if (!code.isEmpty()) b.add("code", code);
                    if (!departmentType.isEmpty()) b.add("departmentType", departmentType);
                    if (!inwardChargeType.isEmpty()) b.add("inwardChargeType", inwardChargeType);
                    if (!departmentId.isEmpty()) b.add("departmentId", Long.parseLong(departmentId));
                    if (!institutionId.isEmpty()) b.add("institutionId", Long.parseLong(institutionId));
                    if (!categoryId.isEmpty()) b.add("categoryId", Long.parseLong(categoryId));
                    if (!inactive.isEmpty()) b.add("inactive", Boolean.parseBoolean(inactive));
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/" + id))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey)
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofString(b.build().toString())).build();
                    return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
                }
                case "DELETE": {
                    if (id.isEmpty()) return "Error: id is required for DELETE.";
                    String url = baseUrl + "/" + id + (retireComments.isEmpty() ? "" : "?retireComments=" + URLEncoder.encode(retireComments, StandardCharsets.UTF_8));
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey).DELETE().build();
                    return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
                }
                case "ACTIVATE": {
                    if (id.isEmpty()) return "Error: id is required for ACTIVATE.";
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/" + id + "/activate"))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey)
                            .method("PATCH", HttpRequest.BodyPublishers.noBody()).build();
                    return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
                }
                case "DEACTIVATE": {
                    if (id.isEmpty()) return "Error: id is required for DEACTIVATE.";
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/" + id + "/deactivate"))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey)
                            .method("PATCH", HttpRequest.BodyPublishers.noBody()).build();
                    return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
                }
                case "LIST_FEES": {
                    if (id.isEmpty()) return "Error: id is required for LIST_FEES.";
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/" + id + "/fees"))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey).GET().build();
                    return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
                }
                case "POST_FEE": {
                    if (id.isEmpty()) return "Error: id is required for POST_FEE.";
                    if (name.isEmpty()) return "Error: name is required for POST_FEE.";
                    if (durationHours.isEmpty()) return "Error: durationHours is required for POST_FEE.";
                    double dh = Double.parseDouble(durationHours);
                    if (dh <= 0) return "Error: durationHours must be > 0.";
                    javax.json.JsonObjectBuilder b = Json.createObjectBuilder()
                            .add("name", name)
                            .add("durationHours", dh)
                            .add("fee", fee.isEmpty() ? 0.0 : Double.parseDouble(fee));
                    if (!ffee.isEmpty()) b.add("ffee", Double.parseDouble(ffee));
                    if (!overShootHours.isEmpty()) b.add("overShootHours", Double.parseDouble(overShootHours));
                    if (!durationDaysForMoCharge.isEmpty()) b.add("durationDaysForMoCharge", Long.parseLong(durationDaysForMoCharge));
                    if (!sortOrder.isEmpty()) b.add("sortOrder", Integer.parseInt(sortOrder));
                    if (!repeating.isEmpty()) b.add("repeating", Boolean.parseBoolean(repeating));
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/" + id + "/fees"))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(b.build().toString())).build();
                    return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
                }
                case "PUT_FEE": {
                    if (id.isEmpty()) return "Error: id is required for PUT_FEE.";
                    if (feeId.isEmpty()) return "Error: feeId is required for PUT_FEE.";
                    javax.json.JsonObjectBuilder b = Json.createObjectBuilder();
                    if (!name.isEmpty()) b.add("name", name);
                    if (!fee.isEmpty()) b.add("fee", Double.parseDouble(fee));
                    if (!ffee.isEmpty()) b.add("ffee", Double.parseDouble(ffee));
                    if (!durationHours.isEmpty()) b.add("durationHours", Double.parseDouble(durationHours));
                    if (!overShootHours.isEmpty()) b.add("overShootHours", Double.parseDouble(overShootHours));
                    if (!durationDaysForMoCharge.isEmpty()) b.add("durationDaysForMoCharge", Long.parseLong(durationDaysForMoCharge));
                    if (!sortOrder.isEmpty()) b.add("sortOrder", Integer.parseInt(sortOrder));
                    if (!repeating.isEmpty()) b.add("repeating", Boolean.parseBoolean(repeating));
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/" + id + "/fees/" + feeId))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey)
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofString(b.build().toString())).build();
                    return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
                }
                case "DELETE_FEE": {
                    if (id.isEmpty()) return "Error: id is required for DELETE_FEE.";
                    if (feeId.isEmpty()) return "Error: feeId is required for DELETE_FEE.";
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/" + id + "/fees/" + feeId))
                            .timeout(Duration.ofSeconds(15)).header("Finance", hmisApiKey).DELETE().build();
                    return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
                }
                default:
                    return "Unknown method: " + method + ". Valid: LIST, GET, POST, PUT, DELETE, ACTIVATE, DEACTIVATE, LIST_FEES, POST_FEE, PUT_FEE, DELETE_FEE";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Timed items API call interrupted.";
        } catch (Exception e) {
            return "Timed items API error: " + e.getMessage();
        }
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
        sb.append("You have many tools to ground your answers in the actual codebase, live configuration, clinical master data, collecting-centre fees, inward discount matrix entries, investigation master records (including category/sample/container/analyzer, components, pricing, validators, and a full-definition export), investigation report formats, dynamic clinical form templates, notification subscriptions, and document templates:\n\n");
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
        sb.append("Manage Inward Discount Matrix entries for services/investigations, pharmacy, and room charges. ")
          .append("Use scope='service' or scope='pharmacy' to pick the correct category universe. ")
          .append("Optional creditCompanyId links a row to a specific credit company; rows without creditCompanyId are the generic fallback. ")
          .append("The system tries credit-company-specific rows first, then falls back to generic rows. ")
          .append("Resolve names to IDs first using the lookup methods (LOOKUP_DEPARTMENTS, LOOKUP_SERVICE_CATEGORIES, ")
          .append("LOOKUP_PHARMACEUTICAL_ITEM_CATEGORIES, LOOKUP_ADMISSION_TYPES, LOOKUP_PAYMENT_SCHEMES, ")
          .append("LIST_PAYMENT_METHODS, LOOKUP_CREDIT_COMPANIES), ")
          .append("then POST to create, PUT to update, or DELETE to retire. ")
          .append("Always confirm with the user before POST, PUT, or DELETE — these changes affect live inward billing discounts. ")
          .append("POST returns 'already_exists' with the existing id when a duplicate combination already exists.\n\n");
        sb.append("### manage_inward_price_adjustment\n");
        sb.append("Manage Inward Price Adjustment (margin) Matrix entries for services/investigations and pharmacy. ")
          .append("Each row defines a gross-value price range (fromPrice, toPrice) and a margin percentage to apply. ")
          .append("Optional creditCompanyId links the row to a specific credit company (0 margin for a CC means no markup). ")
          .append("The system tries credit-company-specific rows first, then falls back to generic rows. ")
          .append("Use scope='service' or scope='pharmacy'. Lookup helpers: LOOKUP_DEPARTMENTS, ")
          .append("LOOKUP_CATEGORIES (/categories/search?scope=service|pharmacy), LIST_PAYMENT_METHODS, LOOKUP_CREDIT_COMPANIES. ")
          .append("Always confirm with the user before POST, PUT, or DELETE.\n\n");
        sb.append("### manage_investigations\n");
        sb.append("Search, retrieve, create, update, activate, or deactivate investigation master records ")
          .append("(lab/diagnostic tests such as CBC, PCR, blood gas, X-ray managed as investigations). ")
          .append("Use GET to search by name, code, or printName. ")
          .append("Use POST to create — returns 'already_exists' with the existing id when a duplicate name is found, ")
          .append("so always check before creating to avoid duplicates. ")
          .append("Use PUT to update name, code, printName, reportType, bypassSampleWorkflow, vatable, or vatPercentage. ")
          .append("Set vatable=true and vatPercentage (e.g. 18) to charge VAT automatically wherever this investigation is billed; ")
          .append("vatable=false or vatPercentage=0 means no VAT. ")
          .append("For POST and PUT you can also set category (categoryId or categoryName), sample type (sampleId or sampleName), ")
          .append("collection container (containerId or containerName), and analyzer/machine (analyzerId or analyzerName) — ")
          .append("passing an *Id must reference an existing row or an error is thrown, while passing a *Name finds-or-creates ")
          .append("a matching row by name. When building a new investigation conversationally, ask the user for these four ")
          .append("(category, sample, container, analyzer) before or alongside the basic name/code so the investigation is ")
          .append("fully identifiable from the start. ")
          .append("Always confirm with the user before POST or PUT — these changes affect live investigation billing.\n\n");
        sb.append("### manage_services\n");
        sb.append("Search, retrieve, create, update, activate, or deactivate service master records ")
          .append("(billable OPD or Inward services, e.g. consultations, procedures, room charges). ")
          .append("Use GET to search by name, code, or printName, optionally filtered by serviceType ('OPD'/'Inward') or categoryId. ")
          .append("Use POST to create — serviceType and name are required, inwardChargeType is required when serviceType=Inward. ")
          .append("Returns 'already_exists' with the existing id when a duplicate name is found, so always check before creating. ")
          .append("Use PUT to update name, code, printName, fullName, categoryId, inwardChargeType, vatable, or vatPercentage. ")
          .append("Set vatable=true and vatPercentage (e.g. 18) to charge VAT automatically wherever this service is billed; ")
          .append("vatable=false or vatPercentage=0 means no VAT. ")
          .append("Always confirm with the user before POST or PUT — these changes affect live service billing.\n\n");
        sb.append("### manage_investigation_format\n");
        sb.append("Manage the internal report format of an investigation: items (report fields like labels, values, ")
          .append("calculations, flags), item values (dropdown options for List-type items), calculations (formulas ")
          .append("that compute results from other fields), flags (reference range flags by age/sex that show high/low/normal), ")
          .append("and dynamic labels (conditional text that changes based on patient age/sex). ")
          .append("First look up the investigation ID using manage_investigations GET, then use this tool. ")
          .append("resource_type: ITEM, VALUE, CALCULATION, FLAG, DYNAMIC_LABEL. ")
          .append("method: LIST, GET, POST, PUT, DELETE. ")
          .append("For ITEM POST, name and ix_item_type are required. For Value-type items, also set ix_item_value_type. ")
          .append("For CALCULATION POST, cal_ix_item_id and calculation_type are required. ")
          .append("For FLAG POST, investigation_item_of_value_type_id and investigation_item_of_flag_type_id are required. ")
          .append("Always LIST items first to get the item IDs before creating calculations, flags, or dynamic labels. ")
          .append("Always confirm with the user before POST, PUT, or DELETE.\n\n");
        sb.append("### manage_investigation_components\n");
        sb.append("Manage InvestigationComponent groupings that organize an investigation's report items under a heading ")
          .append("(e.g. grouping FBC items under 'White Cell Differential'). ")
          .append("First look up the investigation ID using manage_investigations GET, then use this tool. ")
          .append("method: LIST, POST, PUT, DELETE. POST and PUT require component_name; PUT and DELETE require component_id. ")
          .append("DELETE permanently removes the component and fails if any report item still references it — ")
          .append("reassign or remove those items first via manage_investigation_format. ")
          .append("Always confirm with the user before POST, PUT, or DELETE.\n\n");
        sb.append("### manage_investigation_pricing\n");
        sb.append("Manage investigation pricing (ItemFee) — the fees charged when an investigation is billed. ")
          .append("First look up the investigation ID using manage_investigations GET, then use this tool. ")
          .append("method: LIST, POST, PUT, DELETE. POST requires name, feeType, and fee; PUT and DELETE require fee_id. ")
          .append("Optional fields: ffee (foreigner fee, defaults to fee), discountAllowed, institutionId, departmentId, specialityId, staffId. ")
          .append("DELETE soft-deletes (retires) a fee. All mutations recalculate the investigation's total automatically. ")
          .append("Always confirm with the user before POST, PUT, or DELETE — these changes affect live billing.\n\n");
        sb.append("### manage_investigation_validators\n");
        sb.append("Manage InvestigationValidator result-range checks (minimum/maximum acceptable result values) for an investigation. ")
          .append("First look up the investigation ID using manage_investigations GET, then use this tool. ")
          .append("method: LIST, POST, PUT, DELETE. POST requires name; PUT and DELETE require validator_id. ")
          .append("maximumValue and minimumValue are optional but minimumValue cannot exceed maximumValue. ")
          .append("DELETE soft-deletes (retires) a validator. ")
          .append("Always confirm with the user before POST, PUT, or DELETE.\n\n");
        sb.append("### manage_investigation_export\n");
        sb.append("Retrieve an investigation's complete definition as one nested document: metadata (incl. category, sample, ")
          .append("container, analyzer), components, report format (items, item values, calculations, flags, dynamic labels), ")
          .append("validators, and fees. Requires investigation_id. Read-only — use this to review everything configured for ")
          .append("an investigation in one call, e.g. to confirm a newly-built investigation is complete before telling the user ")
          .append("it's ready, or as a reference when building a similar investigation.\n\n");
        sb.append("### manage_inward_rooms\n");
        sb.append("Manage inward room master data: room categories (/inward/room-categories), ")
          .append("rooms (/inward/rooms), and room facility charges — i.e. room fee configurations — (/inward/room-facility-charges). ")
          .append("Use LIST_CATEGORIES / LIST_ROOMS / LIST_CHARGES to browse. ")
          .append("POST_CATEGORY / POST_ROOM / POST_CHARGE to create new records. ")
          .append("PUT_CATEGORY / PUT_ROOM / PUT_CHARGE to update. ")
          .append("DELETE_CATEGORY / DELETE_ROOM / DELETE_CHARGE to soft-retire. ")
          .append("LIST_TIMED_ITEMS / ADD_TIMED_ITEM / REMOVE_TIMED_ITEM manage the TimedItem services attached to a ")
          .append("room facility charge so they auto-bill by duration of stay alongside its fixed fees ")
          .append("(id = room facility charge id; timedItemId required for ADD_TIMED_ITEM; linkId required for REMOVE_TIMED_ITEM). ")
          .append("Always confirm with the user before POST, PUT, or DELETE — these changes affect live inward room billing.\n\n");
        sb.append("### manage_bed_board_svg\n");
        sb.append("Read and set the graphical bed-board SVG drawings used by the Inpatient Bed Board page. ")
          .append("Every bed-board entity stores two drawings on a shared viewBox=\"0 0 1000 600\" grid: ")
          .append("svgParentView (the entity's own empty floor-plan canvas, shown when you navigate into it) and ")
          .append("svgChildView (the small shape showing how this entity looks as a tile inside its parent's canvas). ")
          .append("Sites and departments have both; a room is a leaf and has only svgChildView. ")
          .append("Methods: GET_SITE / SET_SITE / GET_DEPARTMENT / SET_DEPARTMENT / GET_ROOM / SET_ROOM (id required). ")
          .append("On SET, only the fields you pass are changed; pass an empty string to clear a drawing. ")
          .append("SVG is stored verbatim and sanitised when the bed board renders it. ")
          .append("Before authoring drawings, consult the bed-board authoring guidance on the wiki page ")
          .append("'Inpatient — Bed Board' (https://github.com/hmislk/hmis/wiki/Inpatient-Bed-Board); if you cannot reach it, ask the user to paste it. ")
          .append("The guidance documents the viewBox, the site→building→floor→unit hierarchy, copy-paste SVG examples, and a ")
          .append("draw-your-own-shapes primer (rect / ellipse / text / polygon). The same SVG fields are also accepted on ")
          .append("the normal create/update bodies of /api/sites, /api/departments, and /api/inward/rooms, but this tool is the focused way to read or set just the drawings. ")
          .append("Always confirm with the user before any SET.\n\n");
        sb.append("### manage_forms\n");
        sb.append("Design and manage dynamic clinical form templates end-to-end. ")
          .append("resource_type: TEMPLATE | FIELD | CHOICE | ENTRY | VALUE. ")
          .append("method: LIST | GET | POST | PUT | DELETE. ")
          .append("Use TEMPLATE LIST to discover existing forms. ")
          .append("Use TEMPLATE POST to create a form shell (name required, optional formCssClass for Bootstrap row layout). ")
          .append("Use FIELD POST to add fields: required params are form_id, name, and componentPresentationType. ")
          .append("Supported types: Input_text, Input_text_Area, TextEditor, Input_Number, Spinner, Slider, Rating, Calendar, ")
          .append("SelectBooleanCheckBox, SelectBooleanButton, ToggleSwitch, TriStateCheckBox, SelectOneMenu, SelectOneRadio, ")
          .append("SelectOneListBox, SelectCheckBoxMenu, SelectManyButton, MultiSelectListBox, AutoComplete, Signature. ")
          .append("Use CHOICE POST (field_id + label required) to add options for SelectOneMenu, SelectOneRadio, and other choice-type fields. ")
          .append("Use ENTRY LIST (admission_id required) to see filled forms for an inpatient admission. ")
          .append("Use VALUE LIST (entry_id required) to read all captured field values for a specific form submission.\n\n")
          .append("C3 Layout Pattern: When generating editHtml for a field, use these tokens:\n")
          .append("  {{LABEL}} — the field label text\n")
          .append("  {{INPUT}} — replaced at render time with the JSF PrimeFaces input component\n")
          .append("Use Bootstrap 5 grid classes (col-12, col-md-6, col-md-4, col-md-3) for layout. Example:\n")
          .append("  <div class=\"col-12 col-md-6 mb-3\">\n")
          .append("    <label class=\"form-label fw-semibold\">{{LABEL}}</label>\n")
          .append("    {{INPUT}}\n")
          .append("  </div>\n")
          .append("When generating viewHtml, use {{LABEL}} and {{VALUE}} (the formatted stored value).\n")
          .append("Always confirm with the user before POST, PUT, or DELETE.\n\n");
        sb.append("### manage_subscriptions\n");
        sb.append("Manage notification trigger subscriptions — who receives which notification, in which department. ")
          .append("method: LIST | LIST_TRIGGER_TYPES | POST | DELETE. ")
          .append("Use LIST_TRIGGER_TYPES first to discover valid TriggerType names. ")
          .append("Use LIST to see existing subscriptions (filter by triggerType, userId, departmentId, or applicationWide). ")
          .append("Use POST to subscribe a user (userId + triggerType + EITHER departmentId OR applicationWide=true); ")
          .append("an application-wide subscription has a null department and matches every department, which suits hospital-wide roles such as a Guest Relations Officer. ")
          .append("POST returns 'already_exists' with the existing id when an identical non-retired subscription exists. ")
          .append("Use DELETE to soft-retire a subscription by id. ")
          .append("Always confirm with the user before POST or DELETE — these changes affect who receives live notifications.\n\n");
        sb.append("### manage_timed_items\n");
        sb.append("Manage timed item master data (room rent, oxygen, ICU time, etc.) and their tiered fee slots. ")
          .append("TimedItems (DTYPE=TimedItem) are consumed by the inward timed service page to bill patients for duration-based charges. ")
          .append("Use LIST to search items (filter by departmentType e.g. Inward or Theatre). ")
          .append("Use GET to fetch a single item with its fees. ")
          .append("Use POST to create a new timed item — required: name, departmentType, inwardChargeType. ")
          .append("Use PUT to update name, code, departmentType, inwardChargeType, departmentId, institutionId, categoryId, or inactive flag. ")
          .append("Use DELETE to soft-retire an item. Use ACTIVATE / DEACTIVATE to toggle availability without retiring. ")
          .append("For tiered fee management: LIST_FEES lists all fees ordered by sortOrder. ")
          .append("POST_FEE creates a fee tier — required: name, durationHours (> 0). fee, ffee, overShootHours, sortOrder, repeating are optional. ")
          .append("PUT_FEE updates an existing fee tier (requires feeId). DELETE_FEE soft-retires a fee tier. ")
          .append("Always confirm with the user before POST, PUT, or DELETE — changes affect live inward timed billing.\n\n");
        sb.append("### manage_inpatient_templates\n");
        sb.append("Create, read, update, and retire document templates (HTML with placeholder tokens). ")
          .append("Supported types: Prescription, MedicalCertificate, FitnessCertificate, Referral, InpatientDiagnosisCard, InpatientLetter. ")
          .append("method: LIST | GET | POST | PUT | DELETE. ")
          .append("LIST: browse templates by type and name. GET /{id}: retrieve a template including its full HTML contents. ")
          .append("POST: create a new template (name, type, contents required). PUT: update name, type, contents, defaultTemplate, or autoGenerate flags. DELETE: soft-retire. ")
          .append("InpatientLetter placeholders available in contents: {credit_company} {credit_company_address} {policy_no} {reference_no} {credit_limit} ")
          .append("{institution} {department} {doctor} {letter_date} {final_bill} {patient_name} {patient_age} {patient_sex} — plus all Inpatient Diagnosis Card placeholders ")
          .append("({name} {age} {sex} {bht} {doa} {dod} {dx} {past-dx} {allergies} {rx} {drx} {ix} {procedures} {routine-medicines} vitals). ")
          .append("If an admission has multiple credit companies, the user picks one on the inward_letters page before generating. ")
          .append("Always confirm with the user before POST, PUT, or DELETE — these templates appear on the inpatient dashboard Documents page.\n\n");
        sb.append("### manage_payment_schemes\n");
        sb.append("List or update PaymentScheme records (billing-scope flags: validForInpatientBills, validForPharmacy, validForBilledBills, validForChanneling). ")
          .append("Use method=LIST to retrieve all active schemes. Optionally filter by query (name substring). ")
          .append("Use method=UPDATE (with id) for a partial update — only include the fields you want to change. ")
          .append("Always confirm with the user before UPDATE — changes affect live inward and billing flows.\n\n");

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
                githubUrl(branch, "developer_docs/api/using-apis/API_PHARMACY_STOCK_ADJUSTMENTS.md"),
                new String[][]{
                    {"POST", "/pharmacy_adjustments/stock_quantity", "Adjust quantity of a stock batch"},
                    {"POST", "/pharmacy_adjustments/retail_rate",    "Adjust retail sale rate of a stock batch"},
                    {"POST", "/pharmacy_adjustments/purchase_rate",  "Adjust purchase rate of a stock batch"},
                    {"POST", "/pharmacy_adjustments/expiry_date",    "Adjust expiry date of a stock batch"},
                    {"POST", "/pharmacy_adjustments/backfill_finance_details", "Admin-only: backfill BillFinanceDetails for pre-fix adjustment bills (dry-run by default)"}
                });

        appendModule(sb, "Pharmacy - Search", "/pharmacy_adjustments/search",
                "Search pharmacy stocks, departments, and pharmaceutical items.",
                githubUrl(branch, "developer_docs/api/using-apis/API_PHARMACY_STOCK_ADJUSTMENTS.md"),
                new String[][]{
                    {"GET", "/pharmacy_adjustments/search/stocks",      "Search stocks with filters (department, item, quantity, expiry, batch)"},
                    {"GET", "/pharmacy_adjustments/search/departments",  "Search pharmacy departments by name"},
                    {"GET", "/pharmacy_adjustments/search/items",        "Search pharmaceutical items by name or code"}
                });

        appendModule(sb, "Pharmacy - Batches", "/pharmacy_batches",
                "Create and search Active Moiety Products (AMPs) and pharmacy stock batches.",
                githubUrl(branch, "developer_docs/api/using-apis/API_PHARMACY_STOCK_ADJUSTMENTS.md"),
                new String[][]{
                    {"POST", "/pharmacy_batches/amp/search_or_create", "Search for an AMP by name or create one if not found"},
                    {"POST", "/pharmacy_batches/create",               "Create a new pharmacy stock batch"},
                    {"GET",  "/pharmacy_batches/amp/search",           "Search AMPs by name"}
                });

        appendModule(sb, "Pharmacy - F15 Report", "/pharmacy_f15_report",
                "Generate and retrieve pharmacy F15 reports.",
                githubUrl(branch, "developer_docs/api/using-apis/API_F15_REPORT.md"),
                new String[][]{
                    {"GET", "/pharmacy_f15_report", "Retrieve F15 report data with date range and department filters"}
                });

        appendModule(sb, "Pharmacy - Stock History", "/stock_history",
                "Retrieve pharmacy stock movement and history records.",
                githubUrl(branch, "developer_docs/api/using-apis/API_STOCK_HISTORY.md"),
                new String[][]{
                    {"GET", "/stock_history", "Get stock history with date range, item, and department filters. Pass includeArchived=true to also search archived rows beyond the retention window."}
                });

        appendModule(sb, "Pharmaceutical Items", "/pharmaceutical_items",
                "Manage pharmaceutical master data: VTM (active ingredients), ATM, VMP (generic products), AMP (branded products), VMPP, and AMPP. Supports full CRUD, retire/restore, and activate/deactivate.",
                githubUrl(branch, "developer_docs/api/using-apis/API_PHARMACEUTICAL_MANAGEMENT.md"),
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

        appendModule(sb, "Pharmacy Discounts", "/pharmacy/discounts",
                "Manage PaymentSchemeDiscount rows that control per-category discount percentages applied during pharmacy billing for a given payment scheme. "
                + "Use BULK to set the same discount % across all pharmacy item categories at once (idempotent — re-running updates existing rows, never duplicates). "
                + "Default billType is PharmacySale. Always confirm with the user before POST, BULK, PUT, or DELETE.",
                githubUrl(branch, "developer_docs/api/using-apis/pharmacy-discount-api.md"),
                new String[][]{
                    {"GET",    "/pharmacy/discounts",       "List non-retired discount rows. Filters: paymentSchemeId, paymentSchemeName, billType, limit"},
                    {"POST",   "/pharmacy/discounts",       "Create a single discount row. Body: discountPercent + paymentMethod (required), categoryId, paymentSchemeId, paymentSchemeName, billType"},
                    {"POST",   "/pharmacy/discounts/bulk",  "Bulk upsert: set discountPercent for ALL pharmacy item categories under a payment scheme. Body: discountPercent, paymentMethod (required), paymentSchemeId or paymentSchemeName, optional billType"},
                    {"PUT",    "/pharmacy/discounts/{id}",  "Update a discount row (discountPercent)"},
                    {"DELETE", "/pharmacy/discounts/{id}",  "Soft-retire a discount row"}
                });

        appendModule(sb, "Payment Schemes", "/payment-scheme",
                "List and update PaymentScheme records. "
                + "Use LIST to retrieve all active payment schemes with their billing-scope flags "
                + "(validForInpatientBills, validForPharmacy, validForBilledBills, validForChanneling) "
                + "and eligibility flags (staffMemberRequired, membershipRequired, etc.). "
                + "Use UPDATE (method=UPDATE, id required) for a partial update — only fields supplied in the body are changed. "
                + "Always confirm with the user before UPDATE — changes affect live inward and billing flows.",
                null,
                new String[][]{
                    {"GET", "/payment-scheme",      "List all active payment schemes. Optional: query (name filter), limit"},
                    {"PUT", "/payment-scheme/{id}", "Partial update: supply only fields to change (e.g. validForInpatientBills, name)"}
                });

        appendModule(sb, "Pharmacy Items", "/pharmacy/items",
                "Manage dispensable pharmacy PharmaceuticalItem records used by billing and dispensing. This is separate from the pharmaceutical hierarchy API.",
                null,
                new String[][]{
                    {"GET",    "/pharmacy/items/search", "Search dispensable pharmacy items. Filters: query, institutionId, departmentId, size"},
                    {"GET",    "/pharmacy/items/{id}",   "Get one dispensable pharmacy item"},
                    {"POST",   "/pharmacy/items",        "Create a dispensable pharmacy item. Body: name, code, categoryId, dosageFormId, ampId, institutionId, departmentId, retailRate, allowFractions, discountAllowed"},
                    {"PUT",    "/pharmacy/items/{id}",   "Update a dispensable pharmacy item"},
                    {"DELETE", "/pharmacy/items/{id}",   "Retire a dispensable pharmacy item"}
                });

        appendModule(sb, "Pharmaceutical Config", "/pharmaceutical_config",
                "Manage pharmaceutical configuration entities: categories, dosage forms, and measurement units.",
                githubUrl(branch, "developer_docs/api/using-apis/API_PHARMACEUTICAL_MANAGEMENT.md"),
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
                githubUrl(branch, "developer_docs/pharmacy/f15-bfd-backfill-guide.md"),
                new String[][]{
                    {"POST", "/pharmacy/backfill_bfd",      "Backfill missing BFD records for historical pharmacy adjustment bills"},
                    {"POST", "/pharmacy/backfill_grn_bifd", "Backfill missing BIFD/BFD records for historical Pharmacy GRN bills"},
                    {"POST", "/pharmacy/backfill_transfer_department_type", "Backfill missing departmentType on historical pharmacy transfer bills (issue/receive/cancellations/returns). Dry-run by default (apply=false); resolution: unanimous item types, then backwardReferenceBill, then billedBill"}
                });

        // ── Institution / Department / Sites ──────────────────────────────────
        appendModule(sb, "Institution Management", "/institutions",
                "Manage hospitals, clinics, and other healthcare institutions.",
                githubUrl(branch, "developer_docs/api/using-apis/API_INSTITUTION_DEPARTMENT_MANAGEMENT.md"),
                new String[][]{
                    {"GET",    "/institutions/search", "Search institutions by name"},
                    {"GET",    "/institutions/{id}",   "Get institution by ID"},
                    {"POST",   "/institutions",         "Create a new institution"},
                    {"PUT",    "/institutions/{id}",   "Update an institution"},
                    {"DELETE", "/institutions/{id}",   "Retire (soft-delete) an institution"}
                });

        appendModule(sb, "Department Management", "/departments",
                "Manage departments within institutions (wards, pharmacy, outpatient, etc.).",
                githubUrl(branch, "developer_docs/api/using-apis/API_INSTITUTION_DEPARTMENT_MANAGEMENT.md"),
                new String[][]{
                    {"GET",    "/departments/search", "Search departments by name or institution"},
                    {"GET",    "/departments/{id}",   "Get department by ID"},
                    {"POST",   "/departments",         "Create a new department"},
                    {"PUT",    "/departments/{id}",   "Update a department"},
                    {"DELETE", "/departments/{id}",   "Retire a department"},
                    {"GET",    "/departments/{id}/preferences", "Get department UserPreference settings (item-listing strategies)"},
                    {"PUT",    "/departments/{id}/preferences", "Update department UserPreference settings (partial; creates if absent)"}
                });

        appendModule(sb, "Sites", "/sites",
                "Manage hospital sites (physical collection points or satellite locations). "
                + "A site is an Institution with institutionType=Site.",
                githubUrl(branch, "developer_docs/api/using-apis/API_SITES.md"),
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
                githubUrl(branch, "developer_docs/api/using-apis/API_CONSULTANT_MANAGEMENT.md"),
                new String[][]{
                    {"GET",  "/channel/consultant",      "List consultants. Supports query, page, size, specialityId."},
                    {"POST", "/channel/consultant",      "Create a new consultant. Required: name. Optional: title, sex, mobile, phone, fax, address, code, serialNo, specialityId, institutionId, registration, qualification, description. Returns already_exists/409 for duplicates by name+title."},
                    {"PUT",  "/channel/consultant/{id}", "Update an existing consultant by ID. Supports sex and returns 400 for invalid field values, 404 if not found."}
                });

        // ── Channel / Booking ─────────────────────────────────────────────────
        appendModule(sb, "Channel / Booking", "/channel",
                "Manage online doctor appointment bookings end-to-end: browse specialties, hospitals, doctors and sessions, then create, edit, complete or cancel bookings. "
                + "IMPORTANT: Uses the 'Token' header (not 'Finance'). Wrong booking parameters can create bad appointments — always confirm session availability before saving.",
                githubUrl(branch, "developer_docs/api/using-apis/API_CHANNEL_BOOKING.md"),
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

        // ── Staff ─────────────────────────────────────────────────────────────
        appendModule(sb, "Staff", "/staff",
                "CRUD for HMIS Staff records. "
                + "GET lists active staff (query, departmentId, size). "
                + "POST creates a staff member (required: name; optional: code, designation label, departmentId, institutionId) and auto-creates a linked Person. "
                + "PUT partial update (name, code, designation, departmentId, institutionId). "
                + "DELETE soft-retires a staff record. "
                + "Link staff to a user: PUT /users/{userId}/staff with body {staffId}. "
                + "Create user with pre-linked staff: POST /users with optional staffId field.",
                null,
                new String[][]{
                    {"GET",    "/staff",           "List active staff. Filters: query, departmentId, size"},
                    {"GET",    "/staff/{id}",      "Get a single staff record by ID"},
                    {"POST",   "/staff",           "Create staff (required: name; optional: code, designation, departmentId, institutionId)"},
                    {"PUT",    "/staff/{id}",      "Partial update of staff (name, code, designation, departmentId, institutionId)"},
                    {"DELETE", "/staff/{id}",      "Soft-retire a staff record"},
                    {"PUT",    "/users/{id}/staff","Link an existing Staff to a WebUser (body: {staffId})"}
                });

        // ── Users / Roles / Privileges ────────────────────────────────────────
        appendModule(sb, "User Management", "/users",
                "Create, read, update, and retire HMIS web users. Manage passwords, loggable departments, "
                + "and department-scoped privilege assignments. Create/update supports loginPage and optional staffId. "
                + "Use /users/privileges/available to discover valid privilege names. "
                + "DELETE /{id}/departments/{assignmentId} removes one loggable department. "
                + "DELETE /{id}/departments/{deptId}/privileges bulk-revokes all privileges for a department. "
                + "POST /{id}/departments/{deptId}/privileges/all grants every privilege for a department. "
                + "POST /{id}/privileges/all with optional body {departmentIds:[...]} grants every privilege across multiple departments at once.\n\n"
                + "Role-template operations: roles (WebUserRole) are admin-time templates only — runtime behavior "
                + "(privilege checks, login-page resolution, icons, subscriptions) reads user-level records exclusively. "
                + "These endpoints stamp/reset a user's own records FROM a role template; they never change runtime behavior directly. "
                + "aspects (default [\"PRIVILEGES\"]): PRIVILEGES, ICONS, SUBSCRIPTIONS, LOGIN_PAGE. "
                + "RESET converges the user's records to exactly the template (retires extras, adds missing); roleId omitted defaults to the "
                + "user's own WebUser.role and 400s if the user has none. EXPAND adds template records the user lacks, leaving extras untouched "
                + "(roleId required). NARROW retires the user's records that match the template, leaving non-template records untouched "
                + "(roleId required). Any single-user call with preview=true returns counts (added/retired per aspect) without writing. "
                + "Bulk operations (POST /users/bulk/role-operations) target either explicit userIds or a {roleId?, departmentId?} filter "
                + "(userIds wins) and use a two-step safety gate: call once with preview=true to see the resolved user count and per-aspect "
                + "totals (capped at first 200 users), then repeat the identical call with confirm=true to actually apply — calling with "
                + "neither preview nor confirm is rejected. GET /users/roles lists active roles with template summary counts.\n\n"
                + "POST /users/{id}/force-password-reset flags needToResetPassword=true without requiring a new password value — "
                + "use this to force a specific account to reset on next login when you don't want to set (or don't know) an actual "
                + "new password; distinct from /reset-password. GET /users/password-status (optional ?from=&to=, yyyy-MM-dd) reports "
                + "lastPasswordResetAt and needToResetPassword for every active user, for auditing password-expiration policy coverage "
                + "or answering 'who has reset within this period' questions.",
                githubUrl(branch, "developer_docs/api/using-apis/API_USER_MANAGEMENT.md"),
                new String[][]{
                    {"GET",    "/users",                          "List users. Filters: query, departmentId, page, size"},
                    {"POST",   "/users",                          "Create a new user (optional staffId links Staff at creation)"},
                    {"GET",    "/users/{id}",                     "Get user by ID"},
                    {"PUT",    "/users/{id}",                     "Update user details"},
                    {"DELETE", "/users/{id}",                     "Retire (soft-delete) a user"},
                    {"POST",   "/users/{id}/reset-password",      "Admin reset of user password"},
                    {"POST",   "/users/{id}/change-password",     "User changes own password"},
                    {"GET",    "/users/{id}/privileges",          "List privileges for a user"},
                    {"POST",   "/users/{id}/privileges",          "Assign privileges to a user; departmentId is required"},
                    {"POST",   "/users/{id}/departments/{departmentId}/privileges/category", "Assign privileges by category for one department"},
                    {"DELETE", "/users/{id}/privileges/{privilegeId}", "Remove one privilege assignment from a user"},
                    {"GET",    "/users/{id}/departments",         "List loggable departments for a user"},
                    {"POST",   "/users/{id}/departments",         "Assign a loggable department to a user"},
                    {"GET",    "/users/privileges/available",     "List all valid privilege enum names"},
                    {"POST",   "/users/bulk-privileges",                              "Bulk-assign privileges to multiple users at once"},
                    {"DELETE", "/users/{id}/departments/{assignmentId}",             "Revoke a loggable department assignment (by WebUserDepartment id)"},
                    {"DELETE", "/users/{id}/departments/{departmentId}/privileges",  "Bulk-revoke all active privileges for a user scoped to a department"},
                    {"POST",   "/users/{id}/departments/{departmentId}/privileges/all", "Assign every Privileges enum value to a user for a department (skips duplicates)"},
                    {"PUT",    "/users/{id}/staff",               "Link an existing Staff record to the user (body: {staffId})"},
                    {"POST",   "/users/{id}/privileges/all",      "Assign every privilege across supplied departmentIds (or all loggable depts). Returns per-dept summary"},
                    {"POST",   "/users/{id}/role/reset",          "Reset a user's aspects to a role template (roleId optional; defaults to the user's own role)"},
                    {"POST",   "/users/{id}/role/expand",         "Add role-template records the user lacks (roleId required)"},
                    {"POST",   "/users/{id}/role/narrow",         "Retire the user's records that match a role template (roleId required)"},
                    {"POST",   "/users/bulk/role-operations",     "Bulk RESET/EXPAND/NARROW for many users; preview=true then confirm=true"},
                    {"GET",    "/users/roles",                    "List active roles with template summary counts (privileges/icons/subscriptions, template login page)"},
                    {"PUT",    "/users/{id}/login-page",          "Upsert the user's default login page for a department (body: {departmentId, loginPage})"},
                    {"DELETE", "/users/{id}/login-page/{departmentId}", "Retire the user's default login-page override for a department"},
                    {"POST",   "/users/{id}/force-password-reset", "Flag needToResetPassword=true without supplying a new password (distinct from reset-password)"},
                    {"GET",    "/users/password-status",          "Report lastPasswordResetAt and needToResetPassword per active user. Filters: from, to (yyyy-MM-dd)"}
                });

        appendModule(sb, "User Roles", "/user-roles",
                "Create and manage user roles. Assign privileges to roles for role-based access control.",
                githubUrl(branch, "developer_docs/api/using-apis/API_USER_MANAGEMENT.md"),
                new String[][]{
                    {"GET",    "/user-roles",                     "List all user roles"},
                    {"POST",   "/user-roles",                     "Create a new role"},
                    {"GET",    "/user-roles/{id}",                "Get role by ID"},
                    {"PUT",    "/user-roles/{id}",                "Update a role"},
                    {"DELETE", "/user-roles/{id}",                "Retire a role"},
                    {"GET",    "/user-roles/{id}/privileges",     "List privileges assigned to a role"},
                    {"POST",   "/user-roles/{id}/privileges",     "Assign a privilege to a role"}
                });

        appendModule(sb, "Subscriptions", "/subscriptions",
                "Manage notification trigger subscriptions (who receives which notification, in which department). "
                + "An application-wide subscription (null department) matches every department across the whole application.",
                null,
                new String[][]{
                    {"GET",    "/subscriptions",                "List subscriptions (filters: triggerType, userId, departmentId, applicationWide)"},
                    {"GET",    "/subscriptions/trigger-types",  "List all available TriggerType values (name, label, medium, parent)"},
                    {"POST",   "/subscriptions",                "Create a subscription (userId, triggerType, and departmentId OR applicationWide). Returns already_exists on duplicate"},
                    {"DELETE", "/subscriptions/{id}",           "Soft-retire a subscription by ID"}
                });

        // ── Finance ───────────────────────────────────────────────────────────
        appendModule(sb, "Finance - Balance History", "/balance_history",
                "Retrieve financial balance history: drawer entries, patient deposits, agent histories, staff welfare.",
                githubUrl(branch, "developer_docs/api/using-apis/API_BALANCE_HISTORY.md"),
                new String[][]{
                    {"GET", "/balance_history/drawer_entries",         "Get cash drawer entries for a date range"},
                    {"GET", "/balance_history/patient_deposits",        "Get patient deposit records"},
                    {"GET", "/balance_history/agent_histories",         "Get agent financial history records"},
                    {"GET", "/balance_history/staff_welfare_histories", "Get staff welfare financial history"}
                });

        appendModule(sb, "Finance - Bill Data Correction", "/bill_data_correction",
                "Apply corrections and adjustments to financial bill records.",
                githubUrl(branch, "developer_docs/api/using-apis/API_BILL_DATA_CORRECTION.md"),
                new String[][]{
                    {"POST", "/bill_data_correction", "Apply corrections to bill data"}
                });

        appendModule(sb, "Finance - Costing Data", "/costing_data",
                "Retrieve billing and costing data for financial analysis and reporting.",
                githubUrl(branch, "developer_docs/api/using-apis/API_COSTING_DATA.md"),
                new String[][]{
                    {"GET", "/costing_data/last_bill",                    "Get the most recent bill"},
                    {"GET", "/costing_data/bill",                          "Get bills for a date range"},
                    {"GET", "/costing_data/by_bill_number/{bill_number}", "Get a specific bill by bill number"},
                    {"GET", "/costing_data/by_bill_id/{bill_id}",         "Get a specific bill by internal ID"}
                });

        appendModule(sb, "Finance - Legacy Bill Query", "/finance",
                "Legacy bill query endpoints. Use for category-based filtering or simple date-range queries. "
                + "Prefer /costing_data for richer detail. Date format: dd-MM-yyyy; for ranges: dd-MM-yyyy-HH:mm:ss.",
                githubUrl(branch, "developer_docs/api/using-apis/API_FINANCE_LEGACY.md"),
                new String[][]{
                    {"GET", "/finance/bill",                                              "Get all bills for today"},
                    {"GET", "/finance/bill/{date}",                                       "Get bills for a specific date (dd-MM-yyyy)"},
                    {"GET", "/finance/bill/{from}/{to}",                                  "Get bills for a date range"},
                    {"GET", "/finance/bill_item",                                         "Get all bills with line items for today"},
                    {"GET", "/finance/bill_item/{date}",                                  "Get bills with line items for a specific date"},
                    {"GET", "/finance/bill_item/{from}/{to}",                             "Get bills with line items for a date range"},
                    {"GET", "/finance/bill_item_cat/{bill_category}",                     "Get bills filtered by BillType category (today)"},
                    {"GET", "/finance/bill_item_cat/{date}/{bill_category}",              "Get bills by category for a specific date"},
                    {"GET", "/finance/bill_item_cat/{from}/{to}/{bill_category}",         "Get bills by category for a date range"},
                    {"GET", "/finance/bill/search?billNumber={billNumber}",               "Look up bills by bill number (insId or deptId match)"}
                });

        appendModule(sb, "Finance - QuickBooks Export", "/qb",
                "Export HMIS financial data for QuickBooks synchronisation. All endpoints use incremental sync: supply the last synced record ID and a start date to retrieve the next batch (up to 2500 records). Dates in yyyy-MM-dd format.",
                githubUrl(branch, "developer_docs/api/using-apis/API_QUICKBOOKS.md"),
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

        // ── SAP Integration ───────────────────────────────────────────────────
        appendModule(sb, "SAP Integration - Billing", "/sap/billing",
                "Push HMIS bills to SAP S/4HANA Cloud FI as journal entries, and receive payment confirmations from SAP. "
                + "Requires SAP integration to be enabled via ConfigOption keys. "
                + "All endpoints use the Finance header.",
                null,
                new String[][]{
                    {"POST", "/sap/billing/push/{billId}",          "Push an HMIS bill to SAP FI as a journal entry (debit AR, credit revenue per line item)"},
                    {"GET",  "/sap/billing/status/{billId}",        "Get the SAP push status and SAP document number for a bill"},
                    {"POST", "/sap/billing/confirm",                "Receive a payment confirmation webhook from SAP (body: sapDocumentNumber, hmsBillReference, amount, currency, postingDate)"},
                    {"GET",  "/sap/billing/confirm/status/{billId}", "Get the SAP payment confirmation status for a bill"}
                });

        appendModule(sb, "SAP Integration - Inventory", "/sap/inventory",
                "Fetch SAP S/4HANA Cloud MM goods-receipt material documents and match them to HMIS pharmacy items. "
                + "Read-only audit sync — matches SAP materials to HMIS Item master by code or barcode field (configurable). "
                + "Does not create GRN bills; pharmacy GRN workflows handle actual stock updates. "
                + "fromDate defaults to last-sync watermark or N-days-ago fallback; watermark advances only on forward syncs.",
                null,
                new String[][]{
                    {"GET", "/sap/inventory/sync", "Trigger SAP MM goods-receipt sync. Query params: fromDate (yyyy-MM-dd, optional), toDate (yyyy-MM-dd, optional)"}
                });

        appendModule(sb, "Admission Number Counters", "/admission-numbers",
                "View or reset the BHT/OPD-card admission-number sequence counter for a given admission type (and institution, if institution-based numbering is enabled). Use this only when staff have manually corrected a printed BHT/OPD-card number and need the system's next auto-generated number to continue from that correction. This resets a live, shared numbering sequence used by all staff admitting patients under this admission type — before calling PUT, always state the current last/next number (from GET) and the requested new last/next number back to the user, and wait for their explicit confirmation in the same conversation. Never call PUT speculatively or without that confirmation.",
                null,
                new String[][]{
                    {"GET", "/admission-numbers", "View the current counter and what the next generated number would be. Params: admissionTypeId (required), institutionId (optional)"},
                    {"PUT", "/admission-numbers", "Reset the counter to an explicit corrected value so the next generated number is correction+1. Requires explicit user confirmation of the old/new values before calling. Params: admissionTypeId (required), institutionId (optional). Body: {lastAdmissionNumber}"}
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
                "Manage clinician favourite medicine templates and favourite-diagnosis "
                + "medicine suggestions (PrescriptionTemplate types FavouriteMedicine / FavouriteDiagnosis). "
                + "POST/GET accept type=FavouriteMedicine (default) or type=FavouriteDiagnosis. "
                + "For FavouriteDiagnosis, forItemName (resolved via /entities/diagnoses) is required "
                + "and is set as the diagnosis (forItem); itemName/itemType is the suggested medicine. "
                + "/validate (bulk entity validation) is live. "
                + "/parse and /suggest are not yet implemented (return 501).",
                githubUrl(branch, "developer_docs/api/using-apis/API_CLINICAL_FAVOURITE_MEDICINES.md"),
                new String[][]{
                    {"GET",    "/clinical/favourite_medicines",              "List favourite medicine/diagnosis templates. Use type=FavouriteDiagnosis for diagnosis suggestions"},
                    {"POST",   "/clinical/favourite_medicines",              "Create a new template. Set type=FavouriteDiagnosis + forItemName=<diagnosis name> for diagnosis suggestions"},
                    {"GET",    "/clinical/favourite_medicines/{id}",         "Get template by ID"},
                    {"PUT",    "/clinical/favourite_medicines/{id}",         "Update a template"},
                    {"DELETE", "/clinical/favourite_medicines/{id}",         "Retire a template"},
                    {"POST",   "/clinical/favourite_medicines/parse",        "Not implemented (501) — reserved for future natural language parsing"},
                    {"POST",   "/clinical/favourite_medicines/suggest",      "Not implemented (501) — reserved for future auto-suggest"},
                    {"POST",   "/clinical/favourite_medicines/validate",     "Bulk-validate a set of medicine entities"},
                    {"GET",    "/clinical/favourite_medicines/entities/vtms","List/search Virtual Therapeutic Moieties"},
                    {"GET",    "/clinical/favourite_medicines/entities/amps", "List/search Actual Medicinal Products"},
                    {"GET",    "/clinical/favourite_medicines/entities/diagnoses", "List/search diagnoses (ClinicalEntity, Disease_or_Syndrome) for use as forItemName"}
                });

        // ── FHIR ──────────────────────────────────────────────────────────────
        appendModule(sb, "FHIR - Financial Data", "/fhir",
                "HL7 FHIR R5-compliant access to invoices, GRN records, payments, and returns. Uses 'Finance' header.",
                githubUrl(branch, "developer_docs/api/using-apis/API_FHIR.md"),
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
                githubUrl(branch, "developer_docs/api/using-apis/API_FHIR.md"),
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
                githubUrl(branch, "developer_docs/api/using-apis/API_LIMS.md"),
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
                githubUrl(branch, "developer_docs/api/using-apis/API_MEMBERSHIP.md"),
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
                githubUrl(branch, "developer_docs/api/using-apis/API_INWARD.md"),
                new String[][]{
                    {"GET",  "/apiInward/admissions",                                            "List active inpatient admissions"},
                    {"GET",  "/apiInward/admissions/byPhone/{phone}",                            "Find admission by patient phone number"},
                    {"GET",  "/apiInward/banks",                                                  "List available banks/payment institutions"},
                    {"GET",  "/apiInward/validateAdmission/{bht_no}/{phone}",                    "Validate BHT number and phone before payment"},
                    {"POST", "/apiInward/payment",                                                "Process online settlement payment for admitted patient (fields: bht_no, bank_id, reference_no, amount, payment_date)"},
                    {"GET",  "/apiInward/payment/{bht_no}/{bank_id}/{credit_card_ref}/{amount}", "Legacy GET-based payment endpoint"}
                });

        // ── Admission Search ──────────────────────────────────────────────────
        appendModule(sb, "Admission Search", "/inward/admissions",
                "General-purpose admission search — unlike /apiInward/admissions (a financial worklist "
                + "scoped to unpaid/open admissions, capped at 20 rows), this lists all currently active "
                + "(not-discharged) admissions, or searches past or current admissions by BHT, patient "
                + "name, MRN/PHN, phone, or NIC, with no financial scoping and no row cap (paginated).",
                githubUrl(branch, "developer_docs/api/using-apis/API_ADMISSION_DETAILS.md"),
                new String[][]{
                    {"GET", "/inward/admissions", "Search/list admissions. Params: status (default "
                        + "ADMITTED_BUT_NOT_DISCHARGED; also DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED, "
                        + "DISCHARGED_AND_FINAL_BILL_COMPLETED, ANY_STATUS), bhtNo, patientName, mrn, "
                        + "phone, nic, admissionTypeId, institutionId, departmentId, fromDate, toDate "
                        + "(yyyy-MM-dd HH:mm:ss, both required together), page (default 1), size (default 50, max 200)"}
                });

        // ── Inward Discount Matrix ────────────────────────────────────────────
        appendModule(sb, "Inward Discount Matrix", "/inward-discount-matrix",
                "Manage inward discount matrix entries (backs three UI pages for service/investigation, pharmacy, and room charges). "
                + "Use scope=service or scope=pharmacy to choose the category universe. "
                + "Optional creditCompanyId creates a CC-specific row; generic (no-CC) rows are the fallback. "
                + "POST rejects duplicates with 409 + existing id. "
                + "Lookup sub-paths resolve names to IDs.",
                null,
                new String[][]{
                    {"GET",    "/inward-discount-matrix?scope=X",                               "List entries. Filters: scope, departmentId, categoryId, admissionTypeId, paymentSchemeId, paymentMethod, creditCompanyId, limit"},
                    {"GET",    "/inward-discount-matrix/{id}",                                   "Fetch one entry"},
                    {"POST",   "/inward-discount-matrix",                                         "Create. Body: scope (required), discountPercent (required), paymentSchemeId, departmentId, categoryId, admissionTypeId, paymentMethod, creditCompanyId"},
                    {"PUT",    "/inward-discount-matrix/{id}",                                   "Update. Body fields all optional; send null to clear a field"},
                    {"DELETE", "/inward-discount-matrix/{id}",                                   "Soft-retire entry. Optional: retireComments"},
                    {"GET",    "/inward-discount-matrix/admission-types/search?query=",          "AdmissionType name → id lookup"},
                    {"GET",    "/inward-discount-matrix/payment-schemes/search?query=",           "PaymentScheme name → id lookup"},
                    {"GET",    "/inward-discount-matrix/pharmaceutical-item-categories/search?query=", "PharmaceuticalItemCategory name → id lookup"},
                    {"GET",    "/inward-discount-matrix/payment-methods",                         "List PaymentMethod enum values"},
                    {"GET",    "/inward-discount-matrix/credit-companies/search?query=",          "Credit company (Institution) name → id lookup"}
                });

        // ── Inward Price Adjustment (Margin) Matrix ───────────────────────────
        appendModule(sb, "Inward Price Adjustment", "/inward-price-adjustment",
                "Manage inward price adjustment (margin/service charge) matrix entries for service, investigation, and pharmacy. "
                + "Each row maps a gross-value price range (fromPrice, toPrice) to a margin %. "
                + "Optional creditCompanyId creates a CC-specific row; generic (no-CC) rows are the fallback. "
                + "POST rejects duplicates with 409 + existing id.",
                null,
                new String[][]{
                    {"GET",    "/inward-price-adjustment?scope=X",                               "List entries. Filters: scope, departmentId, categoryId, paymentMethod, creditCompanyId, limit"},
                    {"GET",    "/inward-price-adjustment/{id}",                                   "Fetch one entry"},
                    {"POST",   "/inward-price-adjustment",                                         "Create. Body: scope (required), fromPrice (required), toPrice (required), margin (required), departmentId, categoryId, paymentMethod, creditCompanyId"},
                    {"PUT",    "/inward-price-adjustment/{id}",                                   "Update. Body fields all optional"},
                    {"DELETE", "/inward-price-adjustment/{id}",                                   "Soft-retire entry. Optional: retireComments"},
                    {"GET",    "/inward-price-adjustment/categories/search?scope=X&query=",       "Category name → id lookup (requires scope)"},
                    {"GET",    "/inward-price-adjustment/departments/search?query=",              "Department name → id lookup"},
                    {"GET",    "/inward-price-adjustment/payment-methods",                         "List PaymentMethod enum values"},
                    {"GET",    "/inward-price-adjustment/credit-companies/search?query=",          "Credit company name → id lookup"},
                    {"GET",    "/inward-price-adjustment/diagnose?itemId=&departmentId=&paymentMethod=&patientEncounterId=&price=", "Explain whether inward service-charge margin will be applied for an item, with a per-condition pass/fail breakdown"}
                });

        // ── Price Matrix Inward (Flat DTO, audit-logged) ──────────────────────
        appendModule(sb, "Price Matrix Inward", "/price-matrix/inward",
                "Manage InwardPriceAdjustment (margin/service charge) matrix entries with flat DTO format. "
                + "All create/update/retire actions are audit-logged (PRICE_MATRIX_CREATED/UPDATED/RETIRED). "
                + "Supports departmentId, categoryId, paymentMethod, margin, discountPercent, fromPrice, toPrice, admissionTypeId, creditCompanyId. "
                + "POST rejects duplicates with 409 + existing id.",
                null,
                new String[][]{
                    {"GET",    "/price-matrix/inward?departmentId=&categoryId=&paymentMethod=&limit=", "List entries. Filters: departmentId, categoryId, paymentMethod, limit (default 50)"},
                    {"GET",    "/price-matrix/inward/{id}",                                   "Fetch one entry (flat DTO with departmentId/departmentName etc.)"},
                    {"POST",   "/price-matrix/inward",                                         "Create. Body: departmentId (required), categoryId (required), paymentMethod, margin, discountPercent, fromPrice, toPrice, admissionTypeId, creditCompanyId"},
                    {"PUT",    "/price-matrix/inward/{id}",                                   "Update. Body fields all optional — only supplied fields updated"},
                    {"DELETE", "/price-matrix/inward/{id}",                                   "Soft-retire entry. Optional: retireComments (query param)"}
                });

        appendModule(sb, "Inward Room Management", "/inward/room-categories, /inward/rooms, /inward/room-facility-charges",
                "Manage inward room master data: room categories, rooms, and room facility charges (fee configurations). "
                + "POST returns 409 with existing id when a duplicate name exists.",
                githubUrl(branch, "developer_docs/api/using-apis/API_INWARD_ROOM.md"),
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
                    {"DELETE", "/inward/room-facility-charges/{id}", "Soft-retire room facility charge"},
                    {"GET",    "/inward/room-facility-charges/{id}/timed-items", "List TimedItems attached to a room facility charge"},
                    {"POST",   "/inward/room-facility-charges/{id}/timed-items", "Attach a TimedItem. Body: timedItemId (required)"},
                    {"DELETE", "/inward/room-facility-charges/{id}/timed-items/{linkId}", "Soft-retire a TimedItem attachment"}
                });

        appendModule(sb, "Inward - Item Requests", "/itemrequests",
                "External systems submit item/service requests (meals like Breakfast/Lunch/Dinner as InwardService "
                + "items, and stock items like Water Bottle/Tea/Milk/Sugar) against a patient's active BHT. Requests "
                + "are saved Pending (no charge, no stock movement) and routed to a target department's in-app "
                + "approval queue. A department user approves (charges the BHT and deducts stock atomically, failing "
                + "the whole approval if any line has insufficient stock) or rejects (records a reason) the request "
                + "via a JSF approval page — approval/rejection is in-app only and is NOT exposed by this API. "
                + "External systems poll GET /{id} for status: PENDING, APPROVED, REJECTED, CANCELLED.",
                null,
                new String[][]{
                    {"POST", "/itemrequests",           "Submit a new item/service request. Body: {bhtNo, targetDepartmentId, comments, lines:[{itemId, qty}]}"},
                    {"GET",  "/itemrequests/{id}",      "Get a single item/service request by ID, including status and lines. Poll this for status changes"},
                    {"GET",  "/itemrequests",           "List/search item/service requests. Query params: targetDepartmentId, status, fromDate, toDate (yyyy-MM-dd), limit"},
                    {"PUT",  "/itemrequests/{id}/cancel", "Cancel a still-PENDING request. Body: {reason}"}
                });

        appendModule(sb, "Timed Items", "/timed-items",
                "Manage timed item master data and their tiered fee slots for duration-based inward billing. "
                + "Items have departmentType (Inward, Theatre) and inwardChargeType. "
                + "Each item can have multiple TimedItemFee tiers ordered by sortOrder.",
                githubUrl(branch, "developer_docs/api/building-apis/rest-api-development-guide.md"),
                new String[][]{
                    {"GET",    "/timed-items/search?query=&departmentType=&limit=", "Search timed items"},
                    {"GET",    "/timed-items/{id}",          "Fetch one timed item with fees"},
                    {"POST",   "/timed-items",               "Create timed item. Body: name, departmentType, inwardChargeType (all required); code, departmentId, institutionId, categoryId, inactive optional"},
                    {"PUT",    "/timed-items/{id}",          "Update timed item (all fields optional, including categoryId)"},
                    {"DELETE", "/timed-items/{id}",          "Soft-retire timed item"},
                    {"PATCH",  "/timed-items/{id}/activate", "Set inactive=false"},
                    {"PATCH",  "/timed-items/{id}/deactivate", "Set inactive=true"},
                    {"GET",    "/timed-items/{id}/fees",     "List fee tiers for an item (ordered by sortOrder)"},
                    {"POST",   "/timed-items/{id}/fees",     "Add fee tier. Body: name, durationHours (required); fee, ffee, overShootHours, sortOrder, repeating optional"},
                    {"PUT",    "/timed-items/{id}/fees/{feeId}", "Update fee tier"},
                    {"DELETE", "/timed-items/{id}/fees/{feeId}", "Soft-retire fee tier"}
                });

        // ── Login History / Config ────────────────────────────────────────────
        appendModule(sb, "Login History", "/logins",
                "Query user login history filtered by department, user, and date range.",
                githubUrl(branch, "developer_docs/api/using-apis/API_LOGIN_HISTORY.md"),
                new String[][]{
                    {"GET", "/logins",               "List logins. Filters: departmentId, userId, days, fromDate (yyyy-MM-dd), toDate, page, size"},
                    {"GET", "/logins/last-per-user", "Most recent login per unique user. Filters: departmentId, size"}
                });

        appendModule(sb, "System Configuration", "/config",
                "Search and set application configuration options at runtime. "
                + "IMPORTANT: Uses the 'Config' header for authentication, not 'Finance'.",
                githubUrl(branch, "developer_docs/api/using-apis/API_CONFIG.md"),
                new String[][]{
                    {"GET",  "/config?scope={tag}",  "List config options whose key contains {tag} (e.g. scope=inward); omit scope for all"},
                    {"GET",  "/config/{key}",  "Read a single config option by exact key (key, type, scope, current value)"},
                    {"PUT",  "/config/{key}",  "Update a config option value by key. Body {\"value\":\"...\"}. Flushes the cache immediately"},
                    {"GET",  "/config/search?keyword={keyword}", "Search config options by keyword (returns key, type, current value)"},
                    {"POST", "/config/setBoolean/{key}/{value}",  "Set a boolean config option by key name"},
                    {"POST", "/config/setLongText/{key}/{value}", "Set a text config option by key name"},
                    {"POST", "/config/setInteger/{key}/{value}",  "Set an integer config option by key name"}
                });

        appendModule(sb, "Dynamic Forms", "/forms",
                "Design and manage dynamic clinical form templates (create/update/retire), fields of all input types, "
                + "per-field choice options, and AI-generated HTML layout wrappers (editHtml/viewHtml) for the C3 hybrid pattern. "
                + "Query filled form entries and captured values for admissions.",
                githubUrl(branch, "developer_docs/forms/form-api-guide.md"),
                new String[][]{
                    {"GET",    "/forms/templates",                        "List all form templates"},
                    {"GET",    "/forms/templates/{id}",                   "Get a form template by ID"},
                    {"POST",   "/forms/templates",                        "Create a form template (name required)"},
                    {"PUT",    "/forms/templates/{id}",                   "Update a form template"},
                    {"DELETE", "/forms/templates/{id}",                   "Retire a form template"},
                    {"GET",    "/forms/templates/{id}/fields",            "List fields for a form"},
                    {"POST",   "/forms/templates/{id}/fields",            "Add a field to a form"},
                    {"PUT",    "/forms/fields/{id}",                      "Update a field"},
                    {"DELETE", "/forms/fields/{id}",                      "Retire a field"},
                    {"GET",    "/forms/fields/{id}/choices",              "List choices for a choice-type field"},
                    {"POST",   "/forms/fields/{id}/choices",              "Add a choice to a field"},
                    {"PUT",    "/forms/choices/{id}",                     "Update a choice"},
                    {"DELETE", "/forms/choices/{id}",                     "Retire a choice"},
                    {"GET",    "/forms/entries/{admissionId}",            "List filled form entries for an admission"},
                    {"GET",    "/forms/entries/{entryId}/values",         "List captured field values for a form entry"}
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
        sb.append("- Troubleshoot and explain system behaviour using the actual source code\n");
        sb.append("- Design and manage dynamic clinical form templates, fields, choices, and layout wrappers using the C3 hybrid pattern\n\n");
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

    private String lookupFinanceBillByNumber(String billNumber, String hmisBaseUrl, String hmisApiKey) {
        try {
            String root = (hmisBaseUrl != null) ? hmisBaseUrl.trim().replaceAll("/+$", "") : "";
            if (root.isEmpty()) return "Error: HMIS base URL is not configured.";
            if (billNumber == null || billNumber.trim().isEmpty()) return "Error: billNumber is required.";
            String key = (hmisApiKey != null) ? hmisApiKey.trim() : "";
            String url = root + "/api/finance/bill/search?billNumber="
                    + URLEncoder.encode(billNumber.trim(), StandardCharsets.UTF_8);
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest.Builder rb = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET();
            if (!key.isEmpty()) rb.header("Finance", key);
            HttpResponse<String> resp = client.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            return "HTTP " + resp.statusCode() + "\n" + resp.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Finance bill lookup interrupted.";
        } catch (Exception e) {
            return "Finance bill lookup error: " + e.getMessage();
        }
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
