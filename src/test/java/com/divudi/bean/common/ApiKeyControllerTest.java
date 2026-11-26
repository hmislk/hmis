package com.divudi.bean.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.divudi.core.data.ApiKeyType;
import com.divudi.core.entity.WebUser;
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
}
