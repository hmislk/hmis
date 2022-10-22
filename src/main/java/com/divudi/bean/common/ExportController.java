/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import java.io.Serializable;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

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
