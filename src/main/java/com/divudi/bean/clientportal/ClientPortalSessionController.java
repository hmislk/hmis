package com.divudi.bean.clientportal;

import com.divudi.core.entity.ClientAccount;
import java.io.Serializable;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

@Named
@SessionScoped
public class ClientPortalSessionController implements Serializable {

    private static final long serialVersionUID = 1L;

    private ClientAccount loggedInAccount;

    public void login(ClientAccount account) {
        this.loggedInAccount = account;
    }

    public void logout() {
        this.loggedInAccount = null;
    }

    public boolean isLoggedIn() {
        return loggedInAccount != null;
    }

    public ClientAccount getLoggedInAccount() {
        return loggedInAccount;
    }

    public void setLoggedInAccount(ClientAccount loggedInAccount) {
        this.loggedInAccount = loggedInAccount;
    }
}
