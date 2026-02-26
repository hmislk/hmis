package com.divudi.service;

import com.divudi.core.entity.AiMessage;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
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

    /**
     * Known API documentation files in developer_docs/ that are relevant for AI agents.
     * These are fetched from GitHub at call time and injected into the system prompt.
     */
    private static final List<String> API_DOC_FILES = Arrays.asList(
            "developer_docs/API_Documentation_For_AI_Agents.md",
            "developer_docs/API_Testing_Guide_For_AI_Agents.md",
            "developer_docs/API_BALANCE_HISTORY.md",
            "developer_docs/API_BILL_DATA_CORRECTION.md",
            "developer_docs/API_COSTING_DATA.md",
            "developer_docs/API_F15_REPORT.md",
            "developer_docs/API_INSTITUTION_DEPARTMENT_MANAGEMENT.md",
            "developer_docs/API_PHARMACEUTICAL_MANAGEMENT.md",
            "developer_docs/API_STOCK_HISTORY.md"
    );

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
                LOG.log(Level.WARNING, "Anthropic API error {0}: {1}", new Object[]{response.statusCode(), responseBody});
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
     * Builds the system prompt injecting HMIS context and fetched API documentation.
     *
     * @param hmisApiBaseUrl  The HMIS REST API base URL (auto-detected or configured)
     * @param userHmisApiKey  The logged-in user's active HMIS API key value
     * @param githubBranch    The GitHub branch to fetch docs from (e.g. "development")
     */
    public String buildSystemPrompt(String hmisApiBaseUrl, String userHmisApiKey, String githubBranch) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an AI assistant integrated into the HMIS (Hospital Management Information System). ");
        sb.append("You help system administrators and users interact with and manage the hospital system.\n\n");

        if (hmisApiBaseUrl != null && !hmisApiBaseUrl.trim().isEmpty()) {
            sb.append("## HMIS REST API\n");
            sb.append("The HMIS REST API base URL is: ").append(hmisApiBaseUrl.trim()).append("\n");
            sb.append("All API endpoints are relative to this base URL.\n\n");
        }

        if (userHmisApiKey != null && !userHmisApiKey.trim().isEmpty()) {
            sb.append("## Authentication\n");
            sb.append("The user's HMIS API key is: ").append(userHmisApiKey.trim()).append("\n");
            sb.append("Use this key in the 'Finance' header for authenticated API requests.\n\n");
        }

        // Fetch each API doc from GitHub and embed its content directly
        String branch = (githubBranch != null && !githubBranch.trim().isEmpty())
                ? githubBranch.trim() : "development";
        sb.append("## API Documentation\n");
        sb.append("The following documentation describes the available REST API endpoints:\n\n");
        for (String docFile : API_DOC_FILES) {
            String rawUrl = GITHUB_RAW_BASE + branch + "/" + docFile;
            String docContent = fetchTextFromUrl(rawUrl);
            if (docContent != null && !docContent.trim().isEmpty()) {
                sb.append("---\n");
                sb.append("### ").append(docFile).append("\n");
                sb.append(docContent).append("\n\n");
            }
        }

        sb.append("## Your Capabilities\n");
        sb.append("- Query and search HMIS data via REST API calls\n");
        sb.append("- Adjust stock, pharmacy, and financial data\n");
        sb.append("- Analyse reports and uploaded images/documents\n");
        sb.append("- Troubleshoot and explain system behaviour\n\n");
        sb.append("When making API calls, always explain what you are doing and present results clearly. ");
        sb.append("If you encounter errors, explain them in plain language and suggest solutions.\n");

        return sb.toString();
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
