package com.divudi.service;

import com.divudi.core.entity.WebUserPrivilege;
import com.divudi.core.entity.WebUserRolePrivilege;
import com.divudi.core.facade.WebUserPrivilegeFacade;
import com.divudi.core.facade.WebUserRolePrivilegeFacade;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Writes a privilege-tree save in one transaction (#26090): new rows are
 * created and unticked rows are retired together, so either both commit or
 * both roll back. New rows are written first, as the earlier save did.
 */
@Stateless
public class PrivilegeSaveService {

    @EJB
    private WebUserPrivilegeFacade webUserPrivilegeFacade;
    @EJB
    private WebUserRolePrivilegeFacade webUserRolePrivilegeFacade;

    /**
     * Saves the changes for a user's privileges.
     *
     * @param toCreate new rows to create (may be empty)
     * @param toRetire rows already marked retired, with retirer and time
     */
    public void saveUserPrivilegeChanges(List<WebUserPrivilege> toCreate, List<WebUserPrivilege> toRetire) {
        if (toCreate != null && !toCreate.isEmpty()) {
            webUserPrivilegeFacade.batchCreate(toCreate);
        }
        if (toRetire != null && !toRetire.isEmpty()) {
            webUserPrivilegeFacade.batchEdit(toRetire);
        }
    }

    /**
     * Saves the changes for a user role's privileges.
     *
     * @param toCreate new rows to create (may be empty)
     * @param toRetire rows already marked retired, with retirer and time
     */
    public void saveRolePrivilegeChanges(List<WebUserRolePrivilege> toCreate, List<WebUserRolePrivilege> toRetire) {
        if (toCreate != null && !toCreate.isEmpty()) {
            webUserRolePrivilegeFacade.batchCreate(toCreate);
        }
        if (toRetire != null && !toRetire.isEmpty()) {
            webUserRolePrivilegeFacade.batchEdit(toRetire);
        }
    }
}
