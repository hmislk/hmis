package com.divudi.bean.common;

import com.divudi.core.entity.WebUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TwoFactorServiceAccountWebLoginTest {

    @Test
    void serviceAccountIsRefusedForWebLogin() {
        WebUser u = new WebUser();
        u.setServiceAccount(true);
        assertTrue(TwoFactorAuthController.isWebLoginRefusedForServiceAccount(u));
    }

    @Test
    void ordinaryAccountAndNullAreNotRefused() {
        assertFalse(TwoFactorAuthController.isWebLoginRefusedForServiceAccount(new WebUser()));
        assertFalse(TwoFactorAuthController.isWebLoginRefusedForServiceAccount(null));
    }
}
