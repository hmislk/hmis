package com.divudi.bean.collectingCentre;

import com.divudi.bean.lab.PatientInvestigationController;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import javax.inject.Inject;

/**
 *
 * @author Buddhika Ariyaratne
 */
@Named
@SessionScoped
public class CourierController implements Serializable {

    @Inject
    PatientInvestigationController patientInvestigationController;

    private int activeIndex = 0;

    /**
     * Creates a new instance of CourierController
     */
    public CourierController() {
    }

    public String navigateToCourierCollectSamples() {
        activeIndex = 1;
        return "/collecting_centre/courier/collectSamples.xhtml?faces-redirect=true";
    }

    public String navigateToCourierHandoverSamplesToLab() {
        activeIndex = 2;
        return "/collecting_centre/courier/handoverSamplesToLab.xhtml?faces-redirect=true";
    }

    public String navigateToCourierViewReports() {
        activeIndex = 3;
        return "/collecting_centre/courier/viewReports.xhtml?faces-redirect=true";
    }

    public String navigateToCourierIndex() {
        activeIndex = 0;
        return "/collecting_centre/courier/index.xhtml?faces-redirect=true";
    }

    public String navigateToCourierPrintReports() {
        activeIndex = 4;
        return patientInvestigationController.navigateToGenerateBarcodesFromCourier();
    }

    public String navigateToCourierViewStatistics() {
        activeIndex = 5;
        return "/collecting_centre/courier/viewStatistics.xhtml?faces-redirect=true";
    }

    public String navigateToCourierViewBillReports() {
        activeIndex = 6;
        return "/collecting_centre/courier/viewBillReports.xhtml?faces-redirect=true";
    }

    public String navigateToCourierViewPaymentReports() {
        activeIndex = 7;
        return "/collecting_centre/courier/viewPaymentReports.xhtml?faces-redirect=true";
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }
}
