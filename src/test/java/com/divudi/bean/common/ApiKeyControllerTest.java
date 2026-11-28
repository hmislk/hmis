package com.divudi.bean.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.divudi.core.data.ApiKeyType;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.ApiKey;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiKeyControllerTest {

    @Test
    void nonAdminCannotSelectConfigOrAdmin() {
        List<ApiKeyType> l = ApiKeyController.selectableTypes(false);
        assertFalse(l.contains(ApiKeyType.Config));
        assertFalse(l.contains(ApiKeyType.Admin));
        assertTrue(l.contains(ApiKeyType.Finance));
        assertTrue(l.contains(ApiKeyType.Token));
    }

    @Test
    void adminSelectsEveryType() {
        assertEquals(ApiKeyType.values().length, ApiKeyController.selectableTypes(true).size());
    }

    @Test
    void generatedValueIsLongUrlSafeAndDifferent() {
        String a = ApiKeyController.generateKeyValue();
        String b = ApiKeyController.generateKeyValue();
        assertTrue(a.length() >= 43);
        assertTrue(a.matches("[A-Za-z0-9_-]+"));
        assertNotEquals(a, b);
    }

    @Test
    void ownerCheckRefusesMissingRetiredOrInactiveOwner() {
        ApiKeyController c = new ApiKeyController();
        assertFalse(c.isActiveAdminUser(null));
        WebUser retired = new WebUser();
        retired.setRetired(true);
        retired.setActivated(true);
        assertFalse(c.isActiveAdminUser(retired));
        WebUser inactive = new WebUser();
        inactive.setActivated(false);
        assertFalse(c.isActiveAdminUser(inactive));
    }

    private static ApiKeyController controllerWithAdmin(boolean admin) {
        return new ApiKeyController() {
            @Override
            public boolean isActiveAdminUser(WebUser user) {
                return admin && user != null;
            }
        };
    }

    private static ApiKey configKey(WebUser owner, boolean retired, long expiryOffsetMs) {
        ApiKey k = new ApiKey();
        k.setKeyType(ApiKeyType.Config);
        k.setWebUser(owner);
        k.setRetired(retired);
        k.setDateOfExpiary(new Date(System.currentTimeMillis() + expiryOffsetMs));
        return k;
    }

    @Test
    void configKeyOfActiveAdminOwnerReturnsOwner() {
        WebUser owner = new WebUser();
        assertSame(owner, controllerWithAdmin(true).resolveConfigKeyOwner(configKey(owner, false, 100000)));
    }

    @Test
    void configKeyOfNonAdminOwnerIsRejected() {
        assertNull(controllerWithAdmin(false).resolveConfigKeyOwner(configKey(new WebUser(), false, 100000)));
    }

    @Test
    void retiredOrExpiredConfigKeyIsRejected() {
        ApiKeyController c = controllerWithAdmin(true);
        assertNull(c.resolveConfigKeyOwner(configKey(new WebUser(), true, 100000)));
        assertNull(c.resolveConfigKeyOwner(configKey(new WebUser(), false, -100000)));
    }

    @Test
    void configKeyWithoutOwnerOrNullKeyIsRejected() {
        ApiKeyController c = controllerWithAdmin(true);
        assertNull(c.resolveConfigKeyOwner(configKey(null, false, 100000)));
        assertNull(c.resolveConfigKeyOwner(null));
    }

    @Test
    void nonConfigKeyIsRejectedByConfigCheck() {
        ApiKey k = configKey(new WebUser(), false, 100000);
        k.setKeyType(ApiKeyType.Finance);
        assertNull(controllerWithAdmin(true).resolveConfigKeyOwner(k));
    }

    private static ApiKey keyOfType(ApiKeyType t, WebUser owner, boolean retired, long expiryOffsetMs) {
        ApiKey k = configKey(owner, retired, expiryOffsetMs);
        k.setKeyType(t);
        return k;
    }

    @Test
    void adminOwnerIsAcceptedForEveryKeyType() {
        for (ApiKeyType t : ApiKeyType.values()) {
            WebUser owner = new WebUser();
            assertSame(owner, controllerWithAdmin(true).resolveAdminKeyOwner(keyOfType(t, owner, false, 100000)), t.name());
        }
    }

    @Test
    void nonAdminOwnerIsRejectedForEveryKeyType() {
        for (ApiKeyType t : ApiKeyType.values()) {
            assertNull(controllerWithAdmin(false).resolveAdminKeyOwner(keyOfType(t, new WebUser(), false, 100000)), t.name());
        }
    }

    @Test
    void adminCheckRejectsRetiredExpiredNoExpiryNoOwnerAndNull() {
        ApiKeyController c = controllerWithAdmin(true);
        assertNull(c.resolveAdminKeyOwner(keyOfType(ApiKeyType.General, new WebUser(), true, 100000)));
        assertNull(c.resolveAdminKeyOwner(keyOfType(ApiKeyType.Finance, new WebUser(), false, -100000)));
        ApiKey noExpiry = keyOfType(ApiKeyType.Token, new WebUser(), false, 100000);
        noExpiry.setDateOfExpiary(null);
        assertNull(c.resolveAdminKeyOwner(noExpiry));
        assertNull(c.resolveAdminKeyOwner(keyOfType(ApiKeyType.General, null, false, 100000)));
        assertNull(c.resolveAdminKeyOwner(null));
    }
}
