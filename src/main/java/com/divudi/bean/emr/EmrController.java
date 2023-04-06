/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package com.divudi.bean.emr;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;

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
        return "/emr/reports_index.xhtml?faces-redirect=true";
    }

    public String navigateToClinicalFavouriteIndex() {
        return "/clinical/favourite_index.xhtml?faces-redirect=true";
    }

    public String navigateToClinicalAdministration() {
        return "/clinical/clinical_administration.xhtml?faces-redirect=true";
    }

}
