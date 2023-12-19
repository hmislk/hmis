package com.divudi.bean.emr;

import com.divudi.bean.clinical.DiagnosisController;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import org.primefaces.PrimeFaces;
import org.primefaces.component.accordionpanel.AccordionPanel;
import org.primefaces.component.tabview.TabView;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author buddh
 */
@Named(value = "emrController")
@SessionScoped
public class EmrController implements Serializable {

    @Inject
    DiagnosisController diagnosisController;

    /**
     * Creates a new instance of EmrController
     */
    public EmrController() {
    }

    private int activeIndexOfReports = 0;
    private int activeIndexOfPatientProfile = 0;
    private int activeIndexOfOpdVisit = 0;
    private int activeIndexOfSettings = 0;

    public String navigateToSettings() {
        return "/emr/settings/index";
    }

    public String navigateToClinicalPatientForReceptionist() {
        return "/clinical/clinical_patient_for_receptionist.xhtml?faces-redirect=true";
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

    public void onTabChange(TabChangeEvent event) {
        TabView tabView = (TabView) event.getComponent();
        this.activeIndexOfOpdVisit = tabView.getActiveIndex();
        // Additional logic if needed
    }

    public String navigateToManageDiagnoses() {
        diagnosisController.fillItems();
        return "/clinical/clinical_diagnosis";
    }

    public String navigateToEmrAdmin() {
        return "/emr/admin/index.xhtml?faces-redirect=true";
    }

    public int getActiveIndexOfReports() {
        return activeIndexOfReports;
    }

    public void setActiveIndexOfReports(int activeIndexOfReports) {
        this.activeIndexOfReports = activeIndexOfReports;
    }

    public int getActiveIndexOfPatientProfile() {
        return activeIndexOfPatientProfile;
    }

    public void setActiveIndexOfPatientProfile(int activeIndexOfPatientProfile) {
        this.activeIndexOfPatientProfile = activeIndexOfPatientProfile;
    }

    public int getActiveIndexOfSettings() {
        return activeIndexOfSettings;
    }

    public void setActiveIndexOfSettings(int activeIndexOfSettings) {
        this.activeIndexOfSettings = activeIndexOfSettings;
    }

    public int getActiveIndexOfOpdVisit() {
        return activeIndexOfOpdVisit;
    }

    public void setActiveIndexOfOpdVisit(int activeIndexOfOpdVisit) {
        this.activeIndexOfOpdVisit = activeIndexOfOpdVisit;
    }

}
