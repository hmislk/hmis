package com.divudi.bean.Ophthalmology;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;

/**
 * Controller for Ophthalmology-related navigation.
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named
@SessionScoped
public class OphthalmologyController implements Serializable {

    /**
     * Creates a new instance of OphthalmologyController
     */
    public OphthalmologyController() {
    }
    
    // Navigation to the Ophthalmology EMR page
    public String navigateToOphthalmologyEmr(){
        return "/Ophthalmology/emr";
    }

    // Navigation to the Ophthalmology Patient Management page
    public String navigateToOphthalmologyPatientManagement(){
        return "/Ophthalmology/patient_management";
    }

    // Navigation to the Ophthalmology Appointment Management page
    public String navigateToOphthalmologyAppointmentManagement(){
        return "/Ophthalmology/appointment_management";
    }

    // Navigation to the Ophthalmology Stock Management page
    public String navigateToOphthalmologyStockManagement(){
        return "/Ophthalmology/stock_management";
    }

    // Navigation to the Ophthalmology Product Catalog page
    public String navigateToOphthalmologyProductCatalog(){
        return "/Ophthalmology/product_catalog";
    }

    // Navigation to the Ophthalmology Repair Management page
    public String navigateToOphthalmologyRepairManagement(){
        return "/Ophthalmology/repair_management";
    }
    
    // Navigation to the Ophthalmology Repair Management page
    public String navigateToRetailSale(){
        return "/Ophthalmology/retail_sale";
    }
    
}

