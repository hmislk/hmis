/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package com.divudi.bean.common;

import com.divudi.entity.WebUser;
import com.divudi.facade.WebUserFacade;
import java.util.List;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 *
 * @author buddhika
 */
@Named(value = "authenticateController")
@RequestScoped
public class AuthenticateController {
    @EJB
    WebUserFacade uFacade;
    @Inject
    SecurityController securityController;

    /**
     * Creates a new instance of AuthenticateController
     */
    public AuthenticateController() {
    }
    
    public SecurityController getSecurityController() {
        return securityController;
    }
    
    public boolean userAuthenticated(String inputUserName, String inputPassword) {
        String temSQL;
        temSQL = "SELECT u FROM WebUser u WHERE u.retired = false";
        List<WebUser> allUsers = getFacede().findByJpql(temSQL);
        for (WebUser u : allUsers) {
            if ((u.getName()).equalsIgnoreCase(inputUserName)) {
                if (getSecurityController().matchPassword(inputPassword, u.getWebUserPassword())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private WebUserFacade getFacede() {
        return uFacade;
    }
    
}
