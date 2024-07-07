package com.divudi.bean.optician;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;

/**
 * Controller for optician-related navigation.
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named
@SessionScoped
public class OpticianController implements Serializable {

    /**
     * Creates a new instance of OpticianController
     */
    public OpticianController() {
    }
    
    // Navigation to the optician EMR page
    public String navigateToOpticianEmr(){
        return "/optician/emr?faces-redirect=true";
    }

    // Navigation to the optician Patient Management page
    public String navigateToOpticianPatientManagement(){
        return "/optician/patient_management?faces-redirect=true";
    }

    // Navigation to the optician Appointment Management page
    public String navigateToOpticianAppointmentManagement(){
        return "/optician/appointment_management?faces-redirect=true";
    }

    // Navigation to the optician Stock Management page
    public String navigateToOpticianStockManagement(){
        return "/optician/stock_management?faces-redirect=true";
    }

    // Navigation to the optician Product Catalog page
    public String navigateToOpticianProductCatalog(){
        return "/optician/product_catalog?faces-redirect=true";
    }

    // Navigation to the optician Repair Management page
    public String navigateToOpticianRepairManagement(){
        return "/optician/repair_management?faces-redirect=true";
    }
    
    // Navigation to the optician Repair Management page
    public String navigateToRetailSale(){
        return "/optician/retail_sale?faces-redirect=true";
    }
    
    public String navigateToPurchase(){
        return "/optician/purchase?faces-redirect=true";
    }
    
}

