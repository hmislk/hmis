package com.divudi.bean.web;

import com.divudi.entity.web.WebTemplate;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.facade.web.WebTemplateFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.List;
import javax.ejb.EJB;

/**
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class WebTemplateController implements Serializable {

    @EJB
    WebTemplateFacade webTemplateFacade;

    private WebTemplate current;
    private List<WebTemplate> items;

    /**
     * Creates a new instance of WebTemplateController
     */
    public WebTemplateController() {
    }

    public String navigateToListWebTemplates() {
        listAllWebTemplates();
        return "/webcontent/templates";
    }

    private void listAllWebTemplates() {
        String jpql = "select i "
                + " from WebTemplate i";
        items = webTemplateFacade.findByJpql(jpql);
    }

    public String navigateToAddNewTemplate() {
        current = new WebTemplate();
        return "/webcontent/template";
    }

    public String navigateToEditTemplate() {
        if(current==null){
            JsfUtil.addErrorMessage("Nothing to Edit");
            return "";
        }
        return "/webcontent/template";
    }

    public void saveCurrent() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }
        if (current.getId() == null) {
            webTemplateFacade.create(current);
        } else {
            webTemplateFacade.edit(current);
        }
        JsfUtil.addSuccessMessage("Saved");
    }

    public WebTemplate getCurrent() {
        return current;
    }

    public void setCurrent(WebTemplate current) {
        this.current = current;
    }

    public List<WebTemplate> getItems() {
        return items;
    }

    public void setItems(List<WebTemplate> items) {
        this.items = items;
    }

}
