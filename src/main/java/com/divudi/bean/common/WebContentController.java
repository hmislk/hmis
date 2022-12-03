/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.data.Language;
import com.divudi.entity.WebContent;
import com.divudi.facade.WebContentFacade;
import com.divudi.facade.util.JsfUtil;
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
public class WebContentController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private WebContentFacade ejbFacade;
    private WebContent selected;
    private List<WebContent> items = null;
    private Language language = Language.English;
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

    public String toAddNewWebContent() {
        selected = null;
        return "/webcontent/web_content";
    }

    public String toEditWebContent() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select");
            return "";
        }
        return "/webcontent/web_content";
    }
    
    public String toEditWebContentLong() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select");
            return "";
        }
        return "/webcontent/web_content_long";
    }

    public String toDeleteWebContent() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select");
            return "";
        }
        selected.setRetired(true);
        getFacade().edit(selected);
        selected = null;
        listItems();
        return toListWebContent();
    }

    public String toListWebContent() {
        listItems();
        return "/webcontent/web_contents";
    }

    public WebContent findSingleWebContent(String word) {
        WebContent list;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from WebContent c "
                + " where c.retired=:ret "
                + " and c.name = :q "
                + " order by c.id desc";
        hm.put("q", word);
        hm.put("ret", false);
        list = getFacade().findFirstBySQL(sql, hm);
        return list;
    }

    public String findSingleText(String word) {
        WebContent wc = findSingleWebContent(word);
        if (wc == null || getLanguage() == null) {
            return word;
        }
        switch (language) {
            case English:
                return wc.getEnglish();
            case Sinhala:
                return wc.getSinhala();
            case Tamil:
                return wc.getTamil();
            default:
                return word;
        }
    }

    public Language[] getLanguages() {
        return Language.values();
    }

    public boolean isSinhala() {
        return language.equals(Language.Sinhala);
    }

    public boolean isEnglish() {
        return language.equals(Language.English);
    }

    public boolean isTamil() {
        return language.equals(Language.Tamil);
    }

    public void makeLanguageSinhala() {
        language = Language.Sinhala;
    }

    public void makeLanguageTamil() {
        language = Language.Tamil;
    }

    public void makeLanguageEnglish() {
        language = Language.English;
    }

    public List<String> findMultipleWebText(String word) {
        List<WebContent> wcs = findMultipleWebContent(word);
        List<String> strs = new ArrayList<>();
        if (wcs == null || language == null) {
            return strs;
        }
        for (WebContent wc : wcs) {
            switch (language) {
                case English:
                    strs.add(wc.getEnglish());
                    break;
                case Sinhala:
                    strs.add(wc.getSinhala());
                    break;
                case Tamil:
                    strs.add(wc.getTamil());
                    break;
            }
        }

        return strs;
    }

    public List<WebContent> findMultipleWebContent(String word) {
        List<WebContent> list;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from WebContent c "
                + " where c.retired=false "
                + " and c.name = :q "
                + " order by c.name";
        hm.put("q", word);
        list = getFacade().findBySQL(sql, hm);
        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public List<WebContent> completeWebContent(String qry) {
        List<WebContent> list;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from WebContent c "
                + " where c.retired=false "
                + " and c.name like :q "
                + " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        list = getFacade().findBySQL(sql, hm);

        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public void prepwebContentdd() {
        selected = new WebContent();
    }

    private void recreateModel() {
        items = null;
    }

    public void saveWebContent(WebContent w) {
        if (w == null) {
            return;
        }
        if (w.getId() != null && w.getId() > 0) {
            getFacade().edit(w);
        } else {
            getFacade().create(w);
        }
    }

    public void saveSelected() {
        saveWebContent(selected);
        recreateModel();
        listItems();
    }

    public WebContentFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(WebContentFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public WebContentController() {
    }

    public WebContent getSelected() {
        if (selected == null) {
            selected = new WebContent();
        }
        return selected;
    }

    public void setSelected(WebContent selected) {
        this.selected = selected;
    }

    public void delete() {
        if (selected != null) {
            selected.setRetired(true);
            getFacade().edit(selected);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        selected = null;
        getSelected();
    }

    private WebContentFacade getFacade() {
        return ejbFacade;
    }

    public void listItems() {
        String j;
        j = "select a "
                + " from WebContent a "
                + " where a.retired=false "
                + " order by a.name";
        items = getFacade().findBySQL(j);
    }

    public List<WebContent> getItems() {
        return items;
    }

    public Language getLanguage() {
        if (language == null) {
            language = Language.English;
        }
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    /**
     *
     */
    @FacesConverter(forClass = WebContent.class)
    public static class WebContentConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            WebContentController controller = (WebContentController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "webContentController");
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
            if (object instanceof WebContent) {
                WebContent o = (WebContent) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + WebContentController.class.getName());
            }
        }
    }

    @FacesConverter("webContentCon")
    public static class WebContentControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            WebContentController controller = (WebContentController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "webContentController");
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
            if (object instanceof WebContent) {
                WebContent o = (WebContent) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + WebContentController.class.getName());
            }
        }
    }
}
