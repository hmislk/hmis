package com.divudi.bean.inward;

import com.divudi.core.data.dto.InpatientPharmacyItemMovementDTO;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.InpatientPharmacySummaryService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

/**
 * Inpatient Pharmacy Item Summary (issue #24097): per item, the quantity and
 * net value directly issued, issued on request, returned and cancelled for one
 * BHT, all batches combined.
 * <p>
 * Reached two ways: from the Pharmacy menu, where the user picks the BHT, and
 * from the Inpatient dashboard, where it is fixed to that admission.
 */
@Named
@SessionScoped
public class InpatientPharmacyItemMovementController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String PAGE = "/inward/reports/inpatient_pharmacy_item_movement_summary?faces-redirect=true";

    @EJB
    private InpatientPharmacySummaryService inpatientPharmacySummaryService;

    private PatientEncounter patientEncounter;
    private List<InpatientPharmacyItemMovementDTO> items;
    private boolean openedFromDashboard;

    private double directIssueValueTotal;
    private double requestIssueValueTotal;
    private double returnValueTotal;
    private double cancelledValueTotal;
    private double netValueTotal;

    public String navigateToItemMovementSummaryFromPharmacy() {
        openedFromDashboard = false;
        patientEncounter = null;
        clearResults();
        return PAGE;
    }

    public String navigateToItemMovementSummaryForAdmission() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No admission selected");
            return null;
        }
        openedFromDashboard = true;
        if (!process()) {
            return null;
        }
        return PAGE;
    }

    public void processItemMovementSummary() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("Please select a BHT");
            return;
        }
        process();
    }

    public void newSearch() {
        patientEncounter = null;
        clearResults();
    }

    private boolean process() {
        clearResults();
        try {
            items = inpatientPharmacySummaryService.fetchItemMovements(patientEncounter);
        } catch (Exception e) {
            Logger.getLogger(InpatientPharmacyItemMovementController.class.getName()).log(Level.SEVERE, "Error loading inpatient pharmacy item summary", e);
            JsfUtil.addErrorMessage("Error loading pharmacy data");
            return false;
        }
        for (InpatientPharmacyItemMovementDTO dto : items) {
            directIssueValueTotal += dto.getDirectIssueValue();
            requestIssueValueTotal += dto.getRequestIssueValue();
            returnValueTotal += dto.getReturnValue();
            cancelledValueTotal += dto.getCancelledValue();
            netValueTotal += dto.getNetValue();
        }
        return true;
    }

    private void clearResults() {
        items = new ArrayList<>();
        directIssueValueTotal = 0.0;
        requestIssueValueTotal = 0.0;
        returnValueTotal = 0.0;
        cancelledValueTotal = 0.0;
        netValueTotal = 0.0;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public List<InpatientPharmacyItemMovementDTO> getItems() {
        return items;
    }

    public boolean isOpenedFromDashboard() {
        return openedFromDashboard;
    }

    public double getDirectIssueValueTotal() {
        return directIssueValueTotal;
    }

    public double getRequestIssueValueTotal() {
        return requestIssueValueTotal;
    }

    public double getReturnValueTotal() {
        return returnValueTotal;
    }

    public double getCancelledValueTotal() {
        return cancelledValueTotal;
    }

    public double getNetValueTotal() {
        return netValueTotal;
    }
}
