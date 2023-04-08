package com.divudi.bean.emr;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import javax.faces.context.FacesContext;
import org.primefaces.PrimeFaces;
import org.primefaces.component.accordionpanel.AccordionPanel;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author buddh
 */
@Named(value = "emrController")
@SessionScoped
public class EmrController implements Serializable {

    /**
     * Creates a new instance of EmrController
     */
    public EmrController() {
    }

    public String navigateToClinicalPatientForReceptionist() {
        return "/clinical/clinical_patient_for_receptionist.xhtml?faces-redirect=true";
    }

    public String navigateToPatientRegistration() {
        return "/patient_registration.xhtml?faces-redirect=true";
    }

    public String navigateToMembershipRegistration() {
        return "/clinical/membership_registration.xhtml?faces-redirect=true";
    }

    public String navigateToClinicalQueue() {
        return "/clinical/clinical_queue.xhtml?faces-redirect=true";
    }

    public String navigateToClinicalReportsIndex() {
        return "/emr/reports/index.xhtml?faces-redirect=true";
    }

    public String navigateToClinicalFavouriteIndex() {
        return "/clinical/favourite_index.xhtml?faces-redirect=true";
    }

    public String navigateToClinicalAdministration() {
        return "/clinical/clinical_administration.xhtml?faces-redirect=true";
    }

    public String navigateToEmrAdmin() {
        return "/emr/admin/index.xhtml?faces-redirect=true";
    }

    private int activeIndex = -1;

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }

    public void onTabChange(TabChangeEvent event) {
        String activeIndexStr = ((AccordionPanel) event.getComponent()).getActiveIndex();
        activeIndex = Integer.parseInt(activeIndexStr);
        FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("accordionForm:accordion"); //update accordion
        PrimeFaces.current().ajax().update("accordionForm:accordion"); //update accordion
        PrimeFaces.current().executeScript("PF('accordion').loadState();"); //load accordion state
    }

}
