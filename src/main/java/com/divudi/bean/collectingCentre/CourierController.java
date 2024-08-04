package com.divudi.bean.collectingCentre;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;

/**
 *
 * @author Buddhika Ariyaratne
 */
@Named
@SessionScoped
public class CourierController implements Serializable {

    /**
     * Creates a new instance of CourierController
     */
    public CourierController() {
    }

    public String navigateToCourierCollectSamples() {
        return "/collecting_centre/courier/collectSamples.xhtml?faces-redirect=true";
    }

    public String navigateToCourierHandoverSamplesToLab() {
        return "/collecting_centre/courier/handoverSamplesToLab.xhtml?faces-redirect=true";
    }

    public String navigateToCourierViewReports() {
        return "/collecting_centre/courier/viewReports.xhtml?faces-redirect=true";
    }
    
     public String navigateToCourierIndex() {
        return "/collecting_centre/courier/index.xhtml?faces-redirect=true";
    }

    public String navigateToCourierPrintReports() {
        return "/collecting_centre/courier/printReports.xhtml?faces-redirect=true";
    }

    public String navigateToCourierViewStatistics() {
        return "/collecting_centre/courier/viewStatistics.xhtml?faces-redirect=true";
    }

    public String navigateToCourierViewBillReports() {
        return "/collecting_centre/courier/viewBillReports.xhtml?faces-redirect=true";
    }

    public String navigateToCourierViewPaymentReports() {
        return "/collecting_centre/courier/viewPaymentReports.xhtml?faces-redirect=true";
    }
}
