package com.divudi.service;

import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the manage_item_suppliers AI tool sends the right method, URL and
 * body to /api/pharmacy/item_suppliers (#25206).
 */
class AnthropicApiServiceItemSuppliersToolTest {

    private HttpServer server;
    private String baseUrl;
    private final List<String[]> requests = new ArrayList<>();

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String body;
            try (InputStream in = exchange.getRequestBody()) {
                body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            requests.add(new String[]{
                exchange.getRequestMethod(),
                exchange.getRequestURI().toString(),
                body,
                exchange.getRequestHeaders().getFirst("Finance")});
            byte[] out = "{\"status\":\"success\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(out);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/rh/";
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String call(JsonObject input) throws Exception {
        Method m = AnthropicApiService.class.getDeclaredMethod("executeToolCall",
                String.class, JsonObject.class, String.class, String.class, String.class, String.class);
        m.setAccessible(true);
        return (String) m.invoke(new AnthropicApiService(), "manage_item_suppliers", input, "", "development", baseUrl, "key1");
    }

    private String[] last() {
        return requests.get(requests.size() - 1);
    }

    @Test
    void toolIsListed() throws Exception {
        Method m = AnthropicApiService.class.getDeclaredMethod("buildToolsArray");
        m.setAccessible(true);
        JsonArray tools = (JsonArray) m.invoke(new AnthropicApiService());
        boolean found = false;
        for (JsonValue v : tools) {
            if ("manage_item_suppliers".equals(((JsonObject) v).getString("name"))) {
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    void searchSendsOnlyGivenFilters() throws Exception {
        call(Json.createObjectBuilder().add("method", "SEARCH").add("supplierId", "5").add("limit", "10").build());
        assertEquals("GET", last()[0]);
        assertEquals("/rh/api/pharmacy/item_suppliers?supplierId=5&limit=10", last()[1]);
        assertEquals("key1", last()[3]);
    }

    @Test
    void searchWithoutFiltersHasNoQuery() throws Exception {
        call(Json.createObjectBuilder().add("method", "SEARCH").build());
        assertEquals("/rh/api/pharmacy/item_suppliers", last()[1]);
    }

    @Test
    void getUsesId() throws Exception {
        call(Json.createObjectBuilder().add("method", "GET").add("id", "42").build());
        assertEquals("GET", last()[0]);
        assertEquals("/rh/api/pharmacy/item_suppliers/42", last()[1]);
    }

    @Test
    void postSendsNumericIds() throws Exception {
        call(Json.createObjectBuilder().add("method", "POST").add("supplierId", "5").add("itemId", "7").add("orderNo", "2").build());
        assertEquals("POST", last()[0]);
        assertEquals("/rh/api/pharmacy/item_suppliers", last()[1]);
        assertEquals("{\"supplierId\":5,\"itemId\":7,\"orderNo\":2}", last()[2]);
    }

    @Test
    void bulkSendsItemIdArray() throws Exception {
        call(Json.createObjectBuilder().add("method", "BULK").add("supplierId", "5").add("itemIds", "7, 8,9").build());
        assertEquals("POST", last()[0]);
        assertEquals("/rh/api/pharmacy/item_suppliers/bulk", last()[1]);
        assertEquals("{\"supplierId\":5,\"itemIds\":[7,8,9]}", last()[2]);
    }

    @Test
    void deleteEncodesRetireComments() throws Exception {
        call(Json.createObjectBuilder().add("method", "DELETE").add("id", "42").add("retireComments", "wrong supplier").build());
        assertEquals("DELETE", last()[0]);
        assertEquals("/rh/api/pharmacy/item_suppliers/42?retireComments=wrong+supplier", last()[1]);
    }

    @Test
    void missingRequiredValuesAreReportedWithoutACall() throws Exception {
        String result = call(Json.createObjectBuilder().add("method", "POST").add("supplierId", "5").build());
        assertTrue(result.contains("itemId is required"), result);
        result = call(Json.createObjectBuilder().add("method", "BULK").add("supplierId", "x").add("itemIds", "7").build());
        assertTrue(result.contains("supplierId must be numeric"), result);
        assertTrue(requests.isEmpty());
    }
}
