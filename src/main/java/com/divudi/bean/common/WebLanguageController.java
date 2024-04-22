/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.entity.WebLanguage;
import com.divudi.facade.WebLanguageFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class WebLanguageController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private WebLanguageFacade ejbFacade;
    private WebLanguage selected;
    private WebLanguage displayLanguage;
    private List<WebLanguage> items = null;
    String page;

    
    
    
    public String toHome() {
        page = "/index";
        return page;
    }

    public String toChannel() {
        page = "/channel";
        return page;
    }

    public String toReports() {
        page = "/report_search";
        return page;
    }

    public String toServices() {
        page = "/services";
        return page;
    }

    public String toContact() {
        page = "/contact";
        return page;
    }

    public String toAbout() {
        page = "/about";
        return page;
    }

    public String toAddNewWebLanguage() {
        selected = null;
        return "/webcontent/web_language";
    }

    public String toEditWebLanguage() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select");
            return "";
        }
        return "/webcontent/web_language";
    }

    public String toDeleteWebLanguage() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select");
            return "";
        }
        selected.setRetired(true);
        getFacade().edit(selected);
        selected = null;
        listItems();
        return toListWebLanguage();
    }

    public String toListWebLanguage() {
        listItems();
        return "/webcontent/web_languages";
    }

    public WebLanguage findSingleWebLanguage(String word) {
        WebLanguage list;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from WebLanguage c "
                + " where c.retired=:ret "
                + " and c.name = :q "
                + " order by c.id desc";
        hm.put("q", word);
        hm.put("ret", false);
        list = getFacade().findFirstByJpql(sql, hm);
        return list;
    }

    public List<WebLanguage> completeWebLanguage(String qry) {
        List<WebLanguage> list;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from WebLanguage c "
                + " where c.retired=false "
                + " and c.name like :q "
                + " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        list = getFacade().findByJpql(sql, hm);

        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public void prepwebLanguagedd() {
        selected = new WebLanguage();
    }

    private void recreateModel() {
        items = null;
    }

    public void saveWebLanguage(WebLanguage w) {
        if (w == null) {
            return;
        }
        if (w.getId() != null && w.getId() > 0) {
            getFacade().edit(w);
        } else {
            getFacade().create(w);
        }
    }

    public String saveSelected() {
        saveWebLanguage(selected);
        recreateModel();
        listItems();
        return toListWebLanguage();
    }

    public WebLanguageFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(WebLanguageFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public WebLanguageController() {
    }

    public WebLanguage getSelected() {
        if (selected == null) {
            selected = new WebLanguage();
        }
        return selected;
    }

    public void setSelected(WebLanguage selected) {
        this.selected = selected;
    }

    public void delete() {
        if (selected != null) {
            selected.setRetired(true);
            getFacade().edit(selected);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        selected = null;
        getSelected();
    }

    private WebLanguageFacade getFacade() {
        return ejbFacade;
    }

    public WebLanguage getDefaultLanguage() {
        if (getItems() == null || getItems().isEmpty()) {
            WebLanguage wl = new WebLanguage();
            wl.setName("English");
            wl.setCode("en");
            wl.setOrderNo(1.0);
            wl.setDefaultLanguage(true);
            getFacade().create(wl);
            items = new ArrayList<>();
            items.add(wl);
            return wl;
        } else {
            WebLanguage fl = null;
            for (WebLanguage w : getItems()) {
                if (fl == null) {
                    fl = w;
                }
                if (w.isDefaultLanguage()) {
                    return w;
                }
            }
            if (fl != null) {
                fl.setDefaultLanguage(true);
                getFacade().edit(fl);
                return fl;
            }
        }
        WebLanguage wl = new WebLanguage();
        wl.setName("English");
        wl.setCode("en");
        wl.setOrderNo(1.0);
        wl.setDefaultLanguage(true);
        getFacade().create(wl);
        items = new ArrayList<>();
        items.add(wl);
        return wl;

    }

    
    
    public void listItems() {
        String j;
        j = "select a "
                + " from WebLanguage a "
                + " where a.retired=false "
                + " order by a.orderNo";
        items = getFacade().findByJpql(j);
    }

    public List<WebLanguage> getItems() {
        if (items == null) {
            listItems();
        }
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = WebLanguage.class)
    public static class WebLanguageConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            WebLanguageController controller = (WebLanguageController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "webLanguageController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof WebLanguage) {
                WebLanguage o = (WebLanguage) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + WebLanguageController.class.getName());
            }
        }
    }

    
}
