/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.bean.hr.StaffController;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Model;
import javax.enterprise.inject.Produces;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author Buddhika
 */
@Named
@RequestScoped
public class ImageBean {

    private StreamedContent image;

    @Produces
    @Named
    public StreamedContent getImage() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
            // So, we're rendering the view. Return a stub StreamedContent so that it will generate right URL.
            return new DefaultStreamedContent();
        } else {
            // So, browser is requesting the image. Get ID value from actual request param.
            String id = context.getExternalContext().getRequestParameterMap().get("id");
            return getStaffController().getSignature();
        }
    }

    public ImageBean() {
    }
    @Inject
    StaffController staffController;

    public StaffController getStaffController() {
        return staffController;
    }

    public void setStaffController(StaffController staffController) {
        this.staffController = staffController;
    }
}
