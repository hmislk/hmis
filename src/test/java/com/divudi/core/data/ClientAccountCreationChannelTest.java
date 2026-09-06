package com.divudi.core.data;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ClientAccountCreationChannelTest {

    @Test
    public void testAllFourChannelsExist() {
        ClientAccountCreationChannel[] values = ClientAccountCreationChannel.values();
        assertEquals(4, values.length);
        assertNotNull(ClientAccountCreationChannel.valueOf("SELF_PHONE"));
        assertNotNull(ClientAccountCreationChannel.valueOf("SELF_EMAIL"));
        assertNotNull(ClientAccountCreationChannel.valueOf("STAFF_ASSISTED"));
        assertNotNull(ClientAccountCreationChannel.valueOf("KIOSK"));
    }
}
