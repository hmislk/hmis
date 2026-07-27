package com.divudi.bean.clientportal;

import com.divudi.core.entity.Institution;
import com.divudi.core.facade.InstitutionFacade;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@ViewScoped
public class ClientPortalHomeController implements Serializable {

    @Inject
    private ClientPortalSessionController clientPortalSessionController;
    @EJB
    private InstitutionFacade institutionFacade;

    private Institution institution;

    public Institution getInstitution() {
        if (institution == null) {
            institution = institutionFacade.findDefaultInstitution();
        }
        return institution;
    }

    public String logout() {
        clientPortalSessionController.logout();
        return "/client_portal/login?faces-redirect=true";
    }
}
