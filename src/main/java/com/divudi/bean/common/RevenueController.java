/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.ejb.RevenueBean;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.faces.view.ViewScoped;

/**
 *
 * @author Buddhika
 */
@Named(value = "revenueController")
@ViewScoped
public class RevenueController {

    /**
     * EJBs
     */
    @EJB
    RevenueBean revenueBean;

    /**
     * Constructors
     */
    
    /**
     * Getters & Setters
     */
    
    public RevenueController() {
    }

    public RevenueBean getRevenueBean() {
        return revenueBean;
    }

}
