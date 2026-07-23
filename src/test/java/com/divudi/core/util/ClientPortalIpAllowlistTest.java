package com.divudi.core.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ClientPortalIpAllowlistTest {

    @Test
    public void testExactMatchIsAllowed() {
        assertTrue(ClientPortalIpAllowlist.isAllowed("192.168.1.10", "192.168.1.10"));
    }

    @Test
    public void testMatchAmongMultipleCsvEntries() {
        assertTrue(ClientPortalIpAllowlist.isAllowed("192.168.1.11", "192.168.1.10, 192.168.1.11, 192.168.1.12"));
    }

    @Test
    public void testWhitespaceAroundCsvEntriesIsTrimmed() {
        assertTrue(ClientPortalIpAllowlist.isAllowed("10.0.0.5", "  10.0.0.5  ,10.0.0.6"));
    }

    @Test
    public void testNoMatchIsRejected() {
        assertFalse(ClientPortalIpAllowlist.isAllowed("10.0.0.99", "10.0.0.5,10.0.0.6"));
    }

    @Test
    public void testNullRequestIpIsRejected() {
        assertFalse(ClientPortalIpAllowlist.isAllowed(null, "10.0.0.5"));
    }

    @Test
    public void testNullOrEmptyAllowlistIsRejected() {
        assertFalse(ClientPortalIpAllowlist.isAllowed("10.0.0.5", null));
        assertFalse(ClientPortalIpAllowlist.isAllowed("10.0.0.5", ""));
        assertFalse(ClientPortalIpAllowlist.isAllowed("10.0.0.5", "   "));
    }
}
