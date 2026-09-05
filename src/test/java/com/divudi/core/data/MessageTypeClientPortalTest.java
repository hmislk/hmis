package com.divudi.core.data;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MessageTypeClientPortalTest {

    @Test
    public void testClientPortalRegistrationOtpValueExists() {
        assertNotNull(MessageType.valueOf("ClientPortalRegistrationOTP"));
    }

    @Test
    public void testDistinctFromExistingPatientPortalOtp() {
        assertNotEquals(MessageType.PatientPortalOTP, MessageType.ClientPortalRegistrationOTP);
    }
}
