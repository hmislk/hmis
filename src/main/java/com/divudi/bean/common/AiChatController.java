/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.core.entity.AiConversation;
import com.divudi.core.entity.AiMessage;
import com.divudi.core.facade.AiConversationFacade;
import com.divudi.core.facade.AiMessageFacade;
import com.divudi.core.facade.ApiKeyFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.AnthropicApiService;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

/**
 * CDI SessionScoped controller for the AI Chat page.
 *
 * @author buddhika
 */
@Named
@SessionScoped
public class AiChatController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(AiChatController.class.getName());

    @Inject
    private SessionController sessionController;

    @Inject
    private WebUserController webUserController;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    @EJB
    private AiConversationFacade aiConversationFacade;

    @EJB
    private AiMessageFacade aiMessageFacade;

    @EJB
    private ApiKeyFacade apiKeyFacade;

    @EJB
    private AnthropicApiService anthropicApiService;

    // Current state
    private AiConversation currentConversation;
    private List<AiMessage> currentMessages;
    private List<AiConversation> myConversations;
    private List<AiConversation> allConversations;

    // Input state
    private String userInput;
    private String pendingAttachmentBase64;
    private String pendingAttachmentMimeType;
    private String pendingAttachmentName;

    // UI state
    private boolean sending = false;
    private String editingTitle;

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    public String toAiChat() {
        loadMyConversations();
        if (currentConversation == null) {
            startNewConversation();
        }
        return "/ai_chat?faces-redirect=true";
    }

    // -------------------------------------------------------------------------
    // Conversation management
    // -------------------------------------------------------------------------

    public void startNewConversation() {
        currentConversation = new AiConversation();
        String title = "Conversation - "
                + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        currentConversation.setTitle(title);
        currentConversation.setWebUser(sessionController.getLoggedUser());
        currentConversation.setInstitution(sessionController.getInstitution());
        currentConversation.setCreater(sessionController.getLoggedUser());
        currentConversation.setCreatedAt(new Date());
        aiConversationFacade.create(currentConversation);
        currentMessages = new ArrayList<>();
        clearPendingAttachment();
        userInput = null;
        loadMyConversations();
    }

    public void selectConversation(AiConversation conversation) {
        if (conversation == null) {
            return;
        }
        currentConversation = conversation;
        loadCurrentMessages();
        clearPendingAttachment();
        userInput = null;
    }

    public void beginRenameConversation() {
        if (currentConversation != null) {
            editingTitle = currentConversation.getTitle();
        }
    }

    public void saveConversationTitle() {
        if (currentConversation == null) {
            return;
        }
        if (editingTitle == null || editingTitle.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Title cannot be empty.");
            return;
        }
        currentConversation.setTitle(editingTitle.trim());
        aiConversationFacade.edit(currentConversation);
        loadMyConversations();
        JsfUtil.addSuccessMessage("Title saved.");
    }

    public void deleteConversation(AiConversation conversation) {
        if (conversation == null) {
            return;
        }
        conversation.setRetired(true);
        conversation.setRetirer(sessionController.getLoggedUser());
        conversation.setRetiredAt(new Date());
        aiConversationFacade.edit(conversation);
        if (currentConversation != null && currentConversation.equals(conversation)) {
            currentConversation = null;
            currentMessages = new ArrayList<>();
        }
        loadMyConversations();
    }

    // -------------------------------------------------------------------------
    // Messaging
    // -------------------------------------------------------------------------

    public void sendMessage() {
        if (currentConversation == null) {
            startNewConversation();
        }
        if ((userInput == null || userInput.trim().isEmpty()) && pendingAttachmentBase64 == null) {
            JsfUtil.addErrorMessage("Please enter a message or attach a file.");
            return;
        }

        sending = true;

        // Save user message
        AiMessage userMsg = new AiMessage();
        userMsg.setConversation(currentConversation);
        userMsg.setRole("user");
        userMsg.setContent(userInput != null ? userInput.trim() : "");
        userMsg.setAttachmentBase64(pendingAttachmentBase64);
        userMsg.setAttachmentMimeType(pendingAttachmentMimeType);
        userMsg.setAttachmentName(pendingAttachmentName);
        userMsg.setWebUser(sessionController.getLoggedUser());
        userMsg.setCreatedAt(new Date());
        aiMessageFacade.create(userMsg);
        currentMessages.add(userMsg);

        // Collect config values
        String claudeApiKey = configOptionApplicationController.getShortTextValueByKey("AI Chat - Claude API Key", "");
        String model = configOptionApplicationController.getShortTextValueByKey("AI Chat - Claude Model", "claude-opus-4-6");
        Integer maxTokensConfig = configOptionApplicationController.getIntegerValueByKey("AI Chat - Max Tokens", 4096);
        int maxTokens = (maxTokensConfig != null) ? maxTokensConfig : 4096;
        String githubBranch = configOptionApplicationController.getShortTextValueByKey("AI Chat - GitHub Branch", "development");

        // Auto-detect HMIS base URL from the current request (same mechanism used for SMS links)
        String hmisApiBaseUrl = resolveHmisBaseUrl();

        if (claudeApiKey == null || claudeApiKey.trim().isEmpty()) {
            AiMessage errMsg = new AiMessage();
            errMsg.setConversation(currentConversation);
            errMsg.setRole("assistant");
            errMsg.setContent("AI Chat is not configured. Please ask your administrator to set the Claude API Key in Application Configuration.");
            errMsg.setCreatedAt(new Date());
            aiMessageFacade.create(errMsg);
            currentMessages.add(errMsg);
            clearInput();
            sending = false;
            return;
        }

        // Resolve user's active HMIS API key
        String userHmisApiKey = resolveUserHmisApiKey();

        // Build system prompt with auto-fetched API docs from GitHub
        String systemPrompt = anthropicApiService.buildSystemPrompt(hmisApiBaseUrl, userHmisApiKey, githubBranch);

        // History = all messages except the one we just added (the service appends it)
        List<AiMessage> history = new ArrayList<>(currentMessages);
        history.remove(userMsg);

        try {
            AnthropicApiService.AnthropicResponse response = anthropicApiService.sendMessage(
                    claudeApiKey,
                    model,
                    maxTokens,
                    systemPrompt,
                    history,
                    userMsg.getContent(),
                    pendingAttachmentBase64,
                    pendingAttachmentMimeType
            );

            AiMessage assistantMsg = new AiMessage();
            assistantMsg.setConversation(currentConversation);
            assistantMsg.setRole("assistant");
            assistantMsg.setContent(response.getContent());
            assistantMsg.setInputTokens(response.getInputTokens());
            assistantMsg.setOutputTokens(response.getOutputTokens());
            assistantMsg.setCreatedAt(new Date());
            aiMessageFacade.create(assistantMsg);
            currentMessages.add(assistantMsg);

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error getting AI response", e);
            AiMessage errMsg = new AiMessage();
            errMsg.setConversation(currentConversation);
            errMsg.setRole("assistant");
            errMsg.setContent("An unexpected error occurred: " + e.getMessage());
            errMsg.setCreatedAt(new Date());
            aiMessageFacade.create(errMsg);
            currentMessages.add(errMsg);
        }

        clearInput();
        sending = false;
    }

    public void handleFileUpload(FileUploadEvent event) {
        UploadedFile file = event.getFile();
        if (file == null) {
            return;
        }
        try (InputStream is = file.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            pendingAttachmentBase64 = Base64.getEncoder().encodeToString(bytes);
            pendingAttachmentMimeType = file.getContentType();
            pendingAttachmentName = file.getFileName();
            JsfUtil.addSuccessMessage("File attached: " + file.getFileName());
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Error reading uploaded file", e);
            JsfUtil.addErrorMessage("Failed to read uploaded file: " + e.getMessage());
        }
    }

    public void clearAttachment() {
        clearPendingAttachment();
        JsfUtil.addSuccessMessage("Attachment removed.");
    }

    // -------------------------------------------------------------------------
    // Loading
    // -------------------------------------------------------------------------

    public void loadMyConversations() {
        String jpql = "SELECT c FROM AiConversation c "
                + "WHERE c.retired = false "
                + "AND c.webUser = :wu "
                + "ORDER BY c.createdAt DESC";
        Map<String, Object> params = new HashMap<>();
        params.put("wu", sessionController.getLoggedUser());
        myConversations = aiConversationFacade.findByJpql(jpql, params);
    }

    public void loadAllConversations() {
        String jpql = "SELECT c FROM AiConversation c "
                + "WHERE c.retired = false "
                + "ORDER BY c.createdAt DESC";
        allConversations = aiConversationFacade.findByJpql(jpql);
    }

    private void loadCurrentMessages() {
        if (currentConversation == null) {
            currentMessages = new ArrayList<>();
            return;
        }
        String jpql = "SELECT m FROM AiMessage m "
                + "WHERE m.conversation = :conv "
                + "ORDER BY m.createdAt ASC";
        Map<String, Object> params = new HashMap<>();
        params.put("conv", currentConversation);
        currentMessages = aiMessageFacade.findByJpql(jpql, params);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the external base URL of this HMIS instance.
     * Uses the same mechanism as SMS patient report links.
     */
    private String resolveHmisBaseUrl() {
        try {
            return CommonFunctions.getBaseUrl();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not auto-detect HMIS base URL", e);
            return "";
        }
    }

    private String resolveUserHmisApiKey() {
        try {
            String jpql = "SELECT a FROM ApiKey a "
                    + "WHERE a.retired = false "
                    + "AND a.webUser = :wu "
                    + "AND a.dateOfExpiary > :now "
                    + "ORDER BY a.dateOfExpiary DESC";
            Map<String, Object> params = new HashMap<>();
            params.put("wu", sessionController.getLoggedUser());
            params.put("now", new Date());
            List result = apiKeyFacade.findByJpql(jpql, params, javax.persistence.TemporalType.TIMESTAMP);
            if (result != null && !result.isEmpty()) {
                com.divudi.core.entity.ApiKey key = (com.divudi.core.entity.ApiKey) result.get(0);
                return key.getKeyValue();
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not resolve user HMIS API key", e);
        }
        return "";
    }

    private void clearInput() {
        userInput = null;
        clearPendingAttachment();
    }

    private void clearPendingAttachment() {
        pendingAttachmentBase64 = null;
        pendingAttachmentMimeType = null;
        pendingAttachmentName = null;
    }

    public boolean isAdmin() {
        return webUserController.hasPrivilege("Admin");
    }

    public boolean hasAttachment() {
        return pendingAttachmentBase64 != null;
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public AiConversation getCurrentConversation() {
        return currentConversation;
    }

    public void setCurrentConversation(AiConversation currentConversation) {
        this.currentConversation = currentConversation;
    }

    public List<AiMessage> getCurrentMessages() {
        if (currentMessages == null) {
            currentMessages = new ArrayList<>();
        }
        return currentMessages;
    }

    public List<AiConversation> getMyConversations() {
        if (myConversations == null) {
            loadMyConversations();
        }
        return myConversations;
    }

    public List<AiConversation> getAllConversations() {
        if (allConversations == null) {
            loadAllConversations();
        }
        return allConversations;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public String getPendingAttachmentName() {
        return pendingAttachmentName;
    }

    public boolean isSending() {
        return sending;
    }

    public String getEditingTitle() {
        return editingTitle;
    }

    public void setEditingTitle(String editingTitle) {
        this.editingTitle = editingTitle;
    }
}
