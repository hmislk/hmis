/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.core.data.WebContentType;
import static com.divudi.core.data.WebContentType.ShortText;
import com.divudi.core.entity.WebContent;
import com.divudi.core.entity.WebLanguage;
import com.divudi.core.facade.WebContentFacade;
import com.divudi.core.util.JsfUtil;
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
    @Inject
    WebLanguageController webLanguageController;
    @EJB
    private WebContentFacade ejbFacade;
    private WebContent selected;
    private List<WebContent> items = null;
    private WebLanguage language;
    private WebLanguage selectedlanguage;
    String page;

    public String toHome() {
        page = "/index?faces-redirect=true";
        return page;
    }

    public String toChannel() {
        page = "/channel?faces-redirect=true";
        return page;
    }

    public String toReports() {
        page = "/report_search?faces-redirect=true";
        return page;
    }

    public String toServices() {
        page = "/services?faces-redirect=true";
        return page;
    }

    public String toContact() {
        page = "/contact?faces-redirect=true";
        return page;
    }

    public String toAbout() {
        page = "/about?faces-redirect=true";
        return page;
    }

    public String toAddNewWebContent() {
        selected = new WebContent();
        return "/webcontent/web_content?faces-redirect=true";
    }

    public String toAddNewShortWebContent() {
        selected = new WebContent();
        selected.setType(WebContentType.ShortText);
        return toEditWebContent();
    }

    public String toAddNewLongWebContent() {
        selected = new WebContent();
        selected.setType(WebContentType.LongText);
        return toEditWebContent();
    }

    public String toAddNewListWebContent() {
        selected = new WebContent();
        selected.setType(WebContentType.List);
        return toEditWebContent();
    }

    public String toAddNewImageWebContent() {
        selected = new WebContent();
        selected.setType(WebContentType.Image);
        return toEditWebContent();
    }



    public String toEditWebContent() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        switch (selected.getType()) {
            case Image:
                return "/webcontent/image?faces-redirect=true";
            case List:
                return "/webcontent/list?faces-redirect=true";
            case LongText:
                return "/webcontent/long?faces-redirect=true";
            case ShortText:
                return "/webcontent/short?faces-redirect=true";
            default:
                return "/webcontent/web_content?faces-redirect=true";
        }
    }

    public String toEditWebContentLong() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select");
            return "";
        }
        return "/webcontent/web_content_long?faces-redirect=true";
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
        return "/webcontent/web_contents?faces-redirect=true";
    }

    public void makeSelectedLanguageAsDisplayLanguage(){
        if(selectedlanguage==null){
            JsfUtil.addErrorMessage("No Language Selected");
            return ;
        }
        setLanguage(selectedlanguage);
    }

    public WebContent findSingleWebContent(String word) {
        return findSingleWebContent(word, getLanguage());
    }

    public WebContent findSingleWebContent(String word, WebLanguage lang) {
        WebContent list;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from WebContent c "
                + " where c.retired=:ret "
                + " and c.webLanguage=:wl "
                + " and c.name=:q ";
        hm.put("q", word);
        hm.put("wl", lang);
        hm.put("ret", false);
        list = getFacade().findFirstByJpql(sql, hm);
        return list;
    }

    public String findSingleText(String word) {
        WebContent wc = findSingleWebContent(word, getLanguage());
        if (wc == null || getLanguage() == null) {
            return word;
        }
        String txt;
        if (wc.getType() == null) {
            wc.setType(ShortText);
        }
        switch (wc.getType()) {
            case ShortText:
                txt = wc.getShortContext();
                break;
            case LongText:
                txt = wc.getLongContext();
                break;
            default:
                txt = "ERROR";
        }
        return txt;
    }

    public List<WebLanguage> getLanguages() {
        return webLanguageController.getItems();
    }

    public List<String> findMultipleWebText(String word) {
        List<WebContent> wcs = findMultipleWebContent(word);
        List<String> strs = new ArrayList<>();
        if (wcs == null || language == null) {
            return strs;
        }
        for (WebContent wc : wcs) {
            strs.add(wc.getName());
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
        list = getFacade().findByJpql(sql, hm);
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
        list = getFacade().findByJpql(sql, hm);

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
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
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
        items = getFacade().findByJpql(j);
    }

    public List<WebContent> getItems() {
        return items;
    }

    public WebLanguage getLanguage() {
        if (language == null) {
            language = webLanguageController.getDefaultLanguage();
        }
        return language;
    }

    public void setLanguage(WebLanguage language) {
        this.language = language;
    }

    @Deprecated // Now use Data Administration
    public String navigateToManageWeb(){
        return "/webcontent/index?faces-redirect=true";
    }

    public WebLanguage getSelectedlanguage() {
        return selectedlanguage;
    }

    public void setSelectedlanguage(WebLanguage selectedlanguage) {
        this.selectedlanguage = selectedlanguage;
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


}
