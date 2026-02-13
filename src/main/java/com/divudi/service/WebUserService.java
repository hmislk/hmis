package com.divudi.service;

import com.divudi.core.data.Privileges;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.WebUserPrivilege;
import com.divudi.core.facade.WebUserFacade;
import com.divudi.core.facade.WebUserPrivilegeFacade;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 *
 * @author buddhika
 */
@Stateless
public class WebUserService {

    @Inject
    WebUserPrivilegeFacade webUserPrivilegeFacade;

    public boolean hasPrivilege(String privilege, WebUser user, Long departmentId) {
        boolean hasPri = false;
        String jpql = "select wup "
                + " from WebUserPrivilege wup "
                + " where wup.retired=false "
                + " and wup.department.id=:deptId"
                + " and wup.webUser=:wu "
                + " and wup.webUser.retired=false "
                + " and wup.privilege=:pri";
        Map m = new HashMap();
        m.put("deptId", departmentId);
        m.put("wu", user);

        try {
            // Convert String privilege to Privileges enum
            m.put("pri", Privileges.valueOf(privilege));
        } catch (IllegalArgumentException e) {
            // If privilege string doesn't match any enum value, deny access
            return false;
        }

        WebUserPrivilege wup = webUserPrivilegeFacade.findFirstByJpql(jpql, m);
        if (wup == null) {
            return false;
        }
        return true;
    }

}
