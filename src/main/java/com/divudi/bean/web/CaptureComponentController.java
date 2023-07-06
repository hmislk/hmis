/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package com.divudi.bean.web;

import com.divudi.entity.web.CaptureComponent;
import com.divudi.facade.util.JsfUtil;
import com.divudi.facade.web.CaptureComponentFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.List;
import javax.ejb.EJB;

/**
 *
 * @author ACER
 */
@Named(value = "captureComponentController")
@SessionScoped
public class CaptureComponentController implements Serializable {

    /**
     * Creates a new instance of CaptureComponentController
     */
    private CaptureComponent current;

    private List<CaptureComponent> items;

    @EJB
    CaptureComponentFacade facade;

    public String navigateToAddCaptureComponent() {

        current = new CaptureComponent();
        return "/webcontent/capture_component.xhtml";

    }

    public String navgateToListCaptureComponents() {

        listItems();
        return "/webcontent/capture_components.xhtml";
    }

    public String navigateToEditCaptureComponent() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return " ";
        }
        return "/webcontent/capture_component";

    }

    public void saveCurrent() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }

        if (current.getId() == null) {
            facade.create(current);
        } else {
            facade.edit(current);
        }
    }

    private void listItems() {

        String jpql = "select c "
                + " from CaptureComponent c";
        items = facade.findBySQL(jpql);

    }

    public CaptureComponentController() {

    }

    public CaptureComponent getCurrent() {
        return current;
    }

    public void setCurrent(CaptureComponent current) {
        this.current = current;
    }

    public List<CaptureComponent> getItems() {
        return items;
    }

    public void setItems(List<CaptureComponent> items) {
        this.items = items;
    }

}
