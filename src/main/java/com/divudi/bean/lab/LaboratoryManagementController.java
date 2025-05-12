package com.divudi.bean.lab;

import com.divudi.core.data.lab.ListingEntity;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import javax.inject.Named;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Named
@SessionScoped
public class LaboratoryManagementController implements Serializable {

    public LaboratoryManagementController() {
        listingEntity = ListingEntity.BILLS; // Set default view
    }

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Controllers">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Variables">
    private ListingEntity listingEntity;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Navigation Method">
    public String navigateToLaboratoryManagementDashboard() {
        return "/lab/laboratory_management_dashboard?faces-redirect=true";
    }

    public void navigateToLaboratoryBills() {
        listingEntity = ListingEntity.BILLS;
    }

    public void navigateToBarcodes() {
        listingEntity = ListingEntity.BILL_BARCODES;
    }

    public void navigateToSamples() {
        listingEntity = ListingEntity.PATIENT_SAMPLES;
    }

    public void navigateToReport() {
        listingEntity = ListingEntity.PATIENT_INVESTIGATIONS;
    }

    public void navigateToReportPrint() {
        listingEntity = ListingEntity.PATIENT_REPORTS;
    }

    public String navigateToLaboratoryAdministration() {
        return "/admin/lims/index?faces-redirect=true";
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Function">
    
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Getter & Setter">
    public ListingEntity getListingEntity() {
        return listingEntity;
    }

    public void setListingEntity(ListingEntity listingEntity) {
        this.listingEntity = listingEntity;
    }

    // </editor-fold>
}
