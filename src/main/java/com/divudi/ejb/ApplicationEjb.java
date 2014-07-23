/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.ejb;

import com.divudi.bean.common.ApplicationController;
import javax.ejb.Singleton;
import javax.inject.Inject;



/**
 *
 * @author Buddhika
 */
@Singleton
public class ApplicationEjb {

    @Inject
    ApplicationController applicationController;

    public ApplicationController getApplicationController() {
        return applicationController;
    }

    public void setApplicationController(ApplicationController applicationController) {
        this.applicationController = applicationController;
    }

    public void recordAppStart() {
        getApplicationController().recordStart();
    }
    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
}
