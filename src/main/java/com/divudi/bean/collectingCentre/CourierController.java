package com.divudi.bean.collectingCentre;

import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.entity.Bill;
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
    
    private Bill currentCCBill;

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
        patientInvestigationController.makeNull();
        activeIndex = 3;
        return "/collecting_centre/courier/viewReports.xhtml?faces-redirect=true";
    }

    public String navigateToCourierIndex() {
        patientInvestigationController.makeNull();
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
    
    public String navigateToCourierViewReciptReports() {
        activeIndex = 7;
        return "/reports/collectionCenterReports/collection_center_recipt_reports_c?faces-redirect=true";
    }

    public String navigateToCourierViewPaymentReports() {
        activeIndex = 8;
        return "/collecting_centre/courier/viewPaymentReports.xhtml?faces-redirect=true";
    }
    
    public String navigateToCCBillViewFormPaymentReports() {
        return "/collecting_centre/courier/cc_bill_reprint_view?faces-redirect=true";
    }
    
    public String navigateToViewReciptFromCCBillView() {
        activeIndex = 7;
        return "/reports/collectionCenterReports/collection_center_recipt_reports_c?faces-redirect=true";
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }

    public Bill getCurrentCCBill() {
        return currentCCBill;
    }

    public void setCurrentCCBill(Bill currentCCBill) {
        this.currentCCBill = currentCCBill;
    }
}
