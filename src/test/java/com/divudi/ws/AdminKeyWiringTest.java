package com.divudi.ws;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

// API key hardening: both APIs must accept a key only through the shared admin-owner check.
class AdminKeyWiringTest {

    private static String validateApiKeyBody(String path) throws Exception {
        String src = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        int start = src.indexOf("private WebUser validateApiKey(");
        assertTrue(start >= 0, path);
        int end = src.indexOf("\n    }", start);
        return src.substring(start, end);
    }

    private static void check(String path) throws Exception {
        String body = validateApiKeyBody(path);
        assertTrue(body.contains("apiKeyController.resolveAdminKeyOwner(apiKey)"), path);
        assertFalse(body.contains("getWebUser()"), path);
        assertFalse(body.contains("getKeyType()"), path);
    }

    @Test
    void subscriptionApiUsesSharedAdminCheck() throws Exception {
        check("src/main/java/com/divudi/ws/common/SubscriptionApi.java");
    }

    @Test
    void clinicalMetadataApiUsesSharedAdminCheck() throws Exception {
        check("src/main/java/com/divudi/ws/clinical/ClinicalMetadataApi.java");
    }
}
