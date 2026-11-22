package com.divudi.bean.common;

import com.divudi.core.entity.WebUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebUserControllerServiceAccountConcurrentEditTest {

    @Test
    void staleUnchangedCheckboxKeepsValueStoredByOtherAdmin() {
        // opened with false, other admin stored true, this admin did not touch the box (submits stale false)
        assertTrue(WebUserController.resolveServiceAccountToSave(false, false, true));
        // opened with true, other admin stored false, box untouched
        assertFalse(WebUserController.resolveServiceAccountToSave(true, true, false));
    }

    @Test
    void changeMadeInThisSessionIsWritten() {
        assertTrue(WebUserController.resolveServiceAccountToSave(true, false, false));
        assertFalse(WebUserController.resolveServiceAccountToSave(false, true, true));
        // both admins changed it: this admin's explicit change wins
        assertTrue(WebUserController.resolveServiceAccountToSave(true, false, true));
    }

    @Test
    void unprivilegedAdminWithStaleCheckboxWritesNothing() {
        WebUserController c = new WebUserController() {
            @Override
            public boolean hasPrivilege(String privilege) {
                return false;
            }
        };
        WebUser u = new WebUser();
        u.setId(7L);
        u.setServiceAccount(false);
        // opened with false; the form still carries the stale false while another admin has stored true
        c.setCurrent(u);
        c.keepStoredServiceAccountUnlessChangedHere(true);
        // value to save equals the stored one, so the privilege check sees no change and shows no refusal
        c.refuseProtectedFlagsWithoutPrivilege(false, false, true);
        assertTrue(u.isServiceAccount());
    }

    @Test
    void settingCurrentRefreshesBaseline() {
        WebUserController c = new WebUserController();
        WebUser a = new WebUser();
        a.setId(1L);
        a.setServiceAccount(true);
        c.setCurrent(a);
        WebUser b = new WebUser();
        b.setId(2L);
        b.setServiceAccount(false);
        c.setCurrent(b);
        // stale-false form for user 2 while stored is true: stored value kept, not user 1's baseline
        c.keepStoredServiceAccountUnlessChangedHere(true);
        assertTrue(b.isServiceAccount());
    }
}
