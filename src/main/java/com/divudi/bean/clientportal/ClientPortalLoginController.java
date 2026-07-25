package com.divudi.bean.clientportal;

import com.divudi.bean.common.SecurityController;
import com.divudi.core.entity.ClientAccount;
import com.divudi.core.facade.ClientAccountFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@ViewScoped
public class ClientPortalLoginController implements Serializable {

    @EJB
    private ClientAccountFacade clientAccountFacade;

    @Inject
    private ClientPortalSessionController clientPortalSessionController;

    private String identifier;
    private String password;

    private void clearMessages() {
        java.util.Iterator<javax.faces.application.FacesMessage> iter = javax.faces.context.FacesContext.getCurrentInstance().getMessages();
        while (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
    }

    public void login() {
        clearMessages();
        if (identifier == null || identifier.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Enter your phone number or email.");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Enter your password.");
            return;
        }

        List<ClientAccount> candidates = clientAccountFacade.findByVerifiedPhoneOrEmail(identifier.trim());

        ClientAccount matched = null;
        for (ClientAccount candidate : candidates) {
            if (SecurityController.matchPassword(password, candidate.getPasswordHash())) {
                matched = candidate;
                break;
            }
        }

        if (matched == null) {
            JsfUtil.addErrorMessage("Incorrect phone/email or password.");
            return;
        }

        clientPortalSessionController.login(matched);
        password = null;
    }

    public void logout() {
        clientPortalSessionController.logout();
        identifier = null;
        password = null;
    }

    public boolean isLoggedIn() {
        return clientPortalSessionController.isLoggedIn();
    }

    public ClientAccount getLoggedInAccount() {
        return clientPortalSessionController.getLoggedInAccount();
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
