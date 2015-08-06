/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;

/**
 *
 * @author Buddhika
 */
@Named(value = "exportController")
@SessionScoped
public class ExportController implements Serializable {

    /**
     * Creates a new instance of ExportController
     */
    public ExportController() {
    }

    
}
