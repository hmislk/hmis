package com.divudi.bean.optician;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;

/**
 * Controller for Optician-related navigation.
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
    
    // Navigation to the Optician EMR page
    public String navigateToOpticianEmr(){
        return "/Optician/emr?faces-redirect=true";
    }

    // Navigation to the Optician Patient Management page
    public String navigateToOpticianPatientManagement(){
        return "/Optician/patient_management?faces-redirect=true";
    }

    // Navigation to the Optician Appointment Management page
    public String navigateToOpticianAppointmentManagement(){
        return "/Optician/appointment_management?faces-redirect=true";
    }

    // Navigation to the Optician Stock Management page
    public String navigateToOpticianStockManagement(){
        return "/Optician/stock_management?faces-redirect=true";
    }

    // Navigation to the Optician Product Catalog page
    public String navigateToOpticianProductCatalog(){
        return "/Optician/product_catalog?faces-redirect=true";
    }

    // Navigation to the Optician Repair Management page
    public String navigateToOpticianRepairManagement(){
        return "/Optician/repair_management?faces-redirect=true";
    }
    
    // Navigation to the Optician Repair Management page
    public String navigateToRetailSale(){
        return "/Optician/retail_sale?faces-redirect=true";
    }
    
}

