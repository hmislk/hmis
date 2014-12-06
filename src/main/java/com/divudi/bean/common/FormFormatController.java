/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;

import java.util.TimeZone;
import com.divudi.facade.FormFormatFacade;
import com.divudi.entity.FormFormat;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 Informatics)
 */
@Named
@SessionScoped
public class FormFormatController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private FormFormatFacade ejbFacade;
    List<FormFormat> selectedItems;
    private FormFormat current;
    private List<FormFormat> items = null;
    String selectText = "";

    public List<FormFormat> getSelectedItems() {
        selectedItems = getFacade().findBySQL("select c from FormFormat c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new FormFormat();
    }

    public void setSelectedItems(List<FormFormat> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            UtilityController.addSuccessMessage("savedOldSuccessfully");
        } else {
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("savedNewSuccessfully");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public FormFormatFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(FormFormatFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public FormFormatController() {
    }

    public FormFormat getCurrent() {
        if (current == null) {
            current = new FormFormat();
        }
        return current;
    }

    public void setCurrent(FormFormat current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("DeleteSuccessfull");
        } else {
            UtilityController.addSuccessMessage("NothingToDelete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private FormFormatFacade getFacade() {
        return ejbFacade;
    }

    public List<FormFormat> getItems() {
        // items = getFacade().findAll("name", true);
        String sql = "SELECT i FROM FormFormat i where i.retired=false order by i.name";
        items = getEjbFacade().findBySQL(sql);
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }
    /**
     *
     */
}
