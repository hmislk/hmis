package com.divudi.bean.common;

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
}
