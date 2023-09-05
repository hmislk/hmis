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
            theme = "cerulean";
            return theme;
        }
        //"cerulean", "darkly", "litera", "simplex", "solar",
//                "minty");
        switch (theme) {
            case "cerulean":
            case "darkly":
            case "litera":
            case "simplex":
            case "solar":
            case "minty":
                return theme;
            default:
                theme = "cerulean";
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
            UtilityController.addSuccessMessage("Theme updated");
        }
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
        themes.put("cerulean", "cerulean");
        themes.put("darkly", "darkly");
        themes.put("litera", "litera");
        themes.put("simplex", "simplex");
        themes.put("solar", "solar");
        themes.put("minty", "minty");
    }
}
