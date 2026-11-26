package com.divudi.bean.common;

import com.divudi.core.data.Privileges;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Issue #26090 review: a save may use only the list shown for its own target.
 */
public class PrivilegeListGuardTest {

    private static final Long ROLE_R = 7L;
    private static final Long USER = 11L;
    private static final Long DEPT = 3L;

    /**
     * Reviewer's sequence: list role R and save; list and save on a user page;
     * open the role page for R and click Update without listing. The role
     * save must be refused.
     */
    @Test
    public void roleSaveAfterUserListWithoutRelistingIsRefused() {
        UserPrivilageController controller = new UserPrivilageController();
        PrivilegeListGuard guard = controller.getListGuard();
        String roleKey = PrivilegeListGuard.roleKey(ROLE_R);
        String userKey = PrivilegeListGuard.userKey(USER, DEPT);

        // 1. List role R; the save is allowed; the save re-lists R.
        guard.listed(roleKey, EnumSet.of(Privileges.InwardRoom));
        assertEquals(EnumSet.of(Privileges.InwardRoom), guard.baselineForSave(true, roleKey));
        guard.listed(roleKey, EnumSet.of(Privileges.InwardRoom));

        // 2. Open Manage Privileges for a user (WebUserController sets
        // privilegesLoaded false, which clears), list, save.
        controller.setPrivilegesLoaded(false);
        assertNull(guard.getListedKey());
        guard.listed(userKey, EnumSet.of(Privileges.LabBillSearch));
        assertEquals(EnumSet.of(Privileges.LabBillSearch), guard.baselineForSave(true, userKey));

        // A user list never satisfies a role save, even with the tree loaded.
        assertNull(guard.baselineForSave(true, roleKey));

        // 3. Open the role page for R (WebUserRoleController sets
        // privilegesLoaded false, which clears), click Update without listing.
        controller.setPrivilegesLoaded(false);
        assertNull(guard.getListedKey());
        assertNull(guard.baselineForSave(false, roleKey));
        assertNull(guard.baselineForSave(true, roleKey));
        assertNull(guard.baselineForSave(false, userKey));
    }

    @Test
    public void controllerNavigationClearsTheListedKey() {
        UserPrivilageController controller = new UserPrivilageController();
        PrivilegeListGuard guard = controller.getListGuard();
        String userKey = PrivilegeListGuard.userKey(USER, DEPT);

        guard.listed(userKey, EnumSet.of(Privileges.InwardRoom));
        controller.makePrivilegesNeededToBeReloaded();
        assertNull(guard.getListedKey());
        assertNull(guard.baselineForSave(true, userKey));

        guard.listed(userKey, EnumSet.of(Privileges.InwardRoom));
        controller.setPrivilegesLoaded(true);
        assertEquals(userKey, guard.getListedKey());
        controller.setPrivilegesLoaded(false);
        assertNull(guard.getListedKey());
    }

    @Test
    public void userListForAnotherDepartmentDoesNotMatch() {
        PrivilegeListGuard guard = new PrivilegeListGuard();
        guard.listed(PrivilegeListGuard.userKey(USER, DEPT), Collections.<Privileges>emptySet());
        assertNull(guard.baselineForSave(true, PrivilegeListGuard.userKey(USER, 4L)));
        assertNull(guard.baselineForSave(true, PrivilegeListGuard.userKey(12L, DEPT)));
        Set<Privileges> same = guard.baselineForSave(true, PrivilegeListGuard.userKey(USER, DEPT));
        assertEquals(Collections.emptySet(), same);
    }

    @Test
    public void notLoadedIsRefusedEvenForTheSameTarget() {
        PrivilegeListGuard guard = new PrivilegeListGuard();
        String roleKey = PrivilegeListGuard.roleKey(ROLE_R);
        guard.listed(roleKey, EnumSet.of(Privileges.InwardRoom));
        assertNull(guard.baselineForSave(false, roleKey));
    }
}
