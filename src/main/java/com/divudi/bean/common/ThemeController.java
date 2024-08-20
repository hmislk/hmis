/*
 * Author : Dr. M H B Ariyaratne, MO(Health Information), email : buddhika.ari@gmail.com
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.facade.WebUserFacade;
import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import com.divudi.bean.common.util.JsfUtil;
/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class ThemeController implements Serializable {

    @EJB
    WebUserFacade facade;
    /**
     * Managed Properties
     */
    @Inject
    SessionController sessionController;

    /**
     * Creates a new instance of ThemeSwitcherBean
     */
    public ThemeController() {
    }
    private Map<String, String> themes;
    private String theme;

    public Map<String, String> getThemes() {
        return themes;
    }

    public String getTheme() {
        if (getSessionController().getLoggedUser() != null) {
            theme = getSessionController().getLoggedUser().getPrimeTheme();
        }
        if (theme == null) {
            theme = "material-light-outlined"; // Default theme
            return theme;
        }

        switch (theme) {
            case "material-light-outlined":
            case "material-light-filled":
            case "material-dark-outlined":
            case "material-dark-filled":
            case "bootstrap-light-outlined":
            case "bootstrap-light-filled":
            case "bootstrap-dark-outlined":
            case "bootstrap-dark-filled":
            case "primeone-light-outlined":
            case "primeone-light-filled":
            case "primeone-dim-outlined":
            case "primeone-dim-filled":
            case "primeone-dark-outlined":
            case "primeone-dark-filled":
            case "saga":
            case "vela":
            case "arya":
                return theme;
            default:
                theme = "material-light-outlined"; // Default theme
        }
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public void saveTheme() {
        if (getSessionController().getLoggedUser() != null) {
            getSessionController().getLoggedUser().setPrimeTheme(theme);
            getFacade().edit(getSessionController().getLoggedUser());
            JsfUtil.addSuccessMessage("Theme updated");
        }
    }
    
    public String navigateToChangeOwnTheme(){
        return "/user_theme?faces-redirect=true";
    }
    
    public WebUserFacade getFacade() {
        return facade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    @PostConstruct
    public void init() {
        themes = new TreeMap<String, String>();
        themes.put("saga", "saga");
        themes.put("vela", "vela");
        themes.put("arya", "arya");
        themes.put("material-light-outlined", "Material Light Outlined");
        themes.put("material-light-filled", "Material Light Filled");
        themes.put("material-dark-outlined", "Material Dark Outlined");
        themes.put("material-dark-filled", "Material Dark Filled");
        themes.put("bootstrap-light-outlined", "Bootstrap Light Outlined");
        themes.put("bootstrap-light-filled", "Bootstrap Light Filled");
        themes.put("bootstrap-dark-outlined", "Bootstrap Dark Outlined");
        themes.put("bootstrap-dark-filled", "Bootstrap Dark Filled");
        themes.put("primeone-light-outlined", "PrimeOne Light Outlined");
        themes.put("primeone-light-filled", "PrimeOne Light Filled");
        themes.put("primeone-dim-outlined", "PrimeOne Dim Outlined");
        themes.put("primeone-dim-filled", "PrimeOne Dim Filled");
        themes.put("primeone-dark-outlined", "PrimeOne Dark Outlined");
        themes.put("primeone-dark-filled", "PrimeOne Dark Filled");
    }

}
