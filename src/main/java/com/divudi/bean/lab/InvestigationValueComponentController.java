/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.lab;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;

/**
 *
 * @author pasan
 */
@Named(value = "investigationValueComponentController")
@SessionScoped
public class InvestigationValueComponentController implements Serializable {

    /**
     * Creates a new instance of InvestigationValueComponentController
     */
    public InvestigationValueComponentController() {
    }
    
}
