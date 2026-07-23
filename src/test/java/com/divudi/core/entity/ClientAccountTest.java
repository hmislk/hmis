package com.divudi.core.entity;

import com.divudi.core.data.ClientAccountCreationChannel;
import java.util.Date;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ClientAccountTest {

    @Test
    public void testGettersAndSetters() {
        ClientAccount account = new ClientAccount();
        Person person = new Person();
        person.setName("Test Client");
        Date now = new Date();

        account.setPerson(person);
        account.setPasswordHash("hashed-value");
        account.setVerifiedPhone("0771234567");
        account.setVerifiedEmail("client@example.com");
        account.setPhoneVerified(true);
        account.setEmailVerified(false);
        account.setCreatedVia(ClientAccountCreationChannel.SELF_PHONE);
        account.setCreatedAt(now);
        account.setRetired(false);

        assertEquals(person, account.getPerson());
        assertEquals("hashed-value", account.getPasswordHash());
        assertEquals("0771234567", account.getVerifiedPhone());
        assertEquals("client@example.com", account.getVerifiedEmail());
        assertTrue(account.isPhoneVerified());
        assertFalse(account.isEmailVerified());
        assertEquals(ClientAccountCreationChannel.SELF_PHONE, account.getCreatedVia());
        assertEquals(now, account.getCreatedAt());
        assertFalse(account.isRetired());
    }

    @Test
    public void testDefaultRetiredIsFalse() {
        ClientAccount account = new ClientAccount();
        assertFalse(account.isRetired());
    }
}
