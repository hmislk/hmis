package com.divudi.ws.pharmacy;

import com.divudi.core.data.dto.pharmacy.ItemSupplierRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PharmacyItemSupplierApiTest {

    private PharmacyItemSupplierApi api;

    @BeforeEach
    void setUp() {
        api = new PharmacyItemSupplierApi();
    }

    @Test
    @DisplayName("parseBulkRequests parses array of objects")
    void testParseBulkRequests_Array() {
        String json = "[{\"supplierId\": 100, \"itemId\": 200}, {\"supplierId\": 100, \"itemId\": 201}]";
        List<ItemSupplierRequestDto> list = api.parseBulkRequests(json);
        assertEquals(2, list.size());
        assertEquals(100L, list.get(0).getSupplierId());
        assertEquals(200L, list.get(0).getItemId());
        assertEquals(100L, list.get(1).getSupplierId());
        assertEquals(201L, list.get(1).getItemId());
    }

    @Test
    @DisplayName("parseBulkRequests parses links array in object")
    void testParseBulkRequests_LinksArray() {
        String json = "{\"links\": [{\"supplierId\": 100, \"itemId\": 200}]}";
        List<ItemSupplierRequestDto> list = api.parseBulkRequests(json);
        assertEquals(1, list.size());
        assertEquals(100L, list.get(0).getSupplierId());
        assertEquals(200L, list.get(0).getItemId());
    }

    @Test
    @DisplayName("parseBulkRequests parses fan-out itemIds array")
    void testParseBulkRequests_FanoutSuccess() {
        String json = "{\"supplierId\": 100, \"itemIds\": [200, 300, 400]}";
        List<ItemSupplierRequestDto> list = api.parseBulkRequests(json);
        assertEquals(3, list.size());
        for (ItemSupplierRequestDto req : list) {
            assertEquals(100L, req.getSupplierId());
        }
        assertEquals(200L, list.get(0).getItemId());
        assertEquals(300L, list.get(1).getItemId());
        assertEquals(400L, list.get(2).getItemId());
    }

    @Test
    @DisplayName("parseBulkRequests rejects fractional itemId in fan-out with ArithmeticException")
    void testParseBulkRequests_RejectsFractionalItemId() {
        String json = "{\"supplierId\": 100, \"itemIds\": [200, 301.5]}";
        assertThrows(ArithmeticException.class, () -> api.parseBulkRequests(json));
    }

    @Test
    @DisplayName("parseBulkRequests rejects fractional supplierId in fan-out with ArithmeticException")
    void testParseBulkRequests_RejectsFractionalSupplierId() {
        String json = "{\"supplierId\": 100.5, \"itemIds\": [200, 300]}";
        assertThrows(ArithmeticException.class, () -> api.parseBulkRequests(json));
    }

    @Test
    @DisplayName("parseBulkRequests rejects non-numeric string itemId in fan-out with NumberFormatException")
    void testParseBulkRequests_RejectsStringItemId() {
        String json = "{\"supplierId\": 100, \"itemIds\": [\"invalid\"]}";
        assertThrows(NumberFormatException.class, () -> api.parseBulkRequests(json));
    }
}

