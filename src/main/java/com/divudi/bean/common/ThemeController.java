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
            case "primefaces-arya-blue":
            case "primefaces-bootstrap4-blue-dark":
            case "primefaces-bootstrap4-blue-light":
            case "primefaces-bootstrap4-dark-common":
            case "primefaces-bootstrap4-light-common":
            case "primefaces-bootstrap4-purple-dark":
            case "primefaces-bootstrap4-purple-light":
            case "primefaces-luna-amber":
            case "primefaces-luna-blue":
            case "primefaces-luna-common":
            case "primefaces-luna-green":
            case "primefaces-luna-pink":
            case "primefaces-material-compact-deeppurple-dark":
            case "primefaces-material-compact-deeppurple-light":
            case "primefaces-material-compact-indigo-dark":
            case "primefaces-material-compact-indigo-light":
            case "primefaces-material-dark-common":
            case "primefaces-material-deeppurple-dark":
            case "primefaces-material-deeppurple-light":
            case "primefaces-material-indigo-dark":
            case "primefaces-material-indigo-light":
            case "primefaces-material-light-common":
            case "primefaces-mytheme":
            case "primefaces-nova-colored":
            case "primefaces-nova-common":
            case "primefaces-nova-dark":
            case "primefaces-nova-light":
            case "primefaces-saga-blue":
            case "primefaces-vela-blue":
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
        themes.put("primefaces-arya-blue", "primefaces-arya-blue");
        themes.put("primefaces-bootstrap4-blue-dark", "primefaces-bootstrap4-blue-dark");
        themes.put("primefaces-bootstrap4-blue-light", "primefaces-bootstrap4-blue-light");
        themes.put("primefaces-bootstrap4-dark-common", "primefaces-bootstrap4-dark-common");
        themes.put("primefaces-bootstrap4-light-common", "primefaces-bootstrap4-light-common");
        themes.put("primefaces-bootstrap4-purple-dark", "primefaces-bootstrap4-purple-dark");
        themes.put("primefaces-bootstrap4-purple-light", "primefaces-bootstrap4-purple-light");
        themes.put("primefaces-luna-amber", "primefaces-luna-amber");
        themes.put("primefaces-luna-blue", "primefaces-luna-blue");
        themes.put("primefaces-luna-common", "primefaces-luna-common");
        themes.put("primefaces-luna-green", "primefaces-luna-green");
        themes.put("primefaces-luna-pink", "primefaces-luna-pink");
        themes.put("primefaces-material-compact-deeppurple-dark", "primefaces-material-compact-deeppurple-dark");
        themes.put("primefaces-material-compact-deeppurple-light", "primefaces-material-compact-deeppurple-light");
        themes.put("primefaces-material-compact-indigo-dark", "primefaces-material-compact-indigo-dark");
        themes.put("primefaces-material-compact-indigo-light", "primefaces-material-compact-indigo-light");
        themes.put("primefaces-material-dark-common", "primefaces-material-dark-common");
        themes.put("primefaces-material-deeppurple-dark", "primefaces-material-deeppurple-dark");
        themes.put("primefaces-material-deeppurple-light", "primefaces-material-deeppurple-light");
        themes.put("primefaces-material-indigo-dark", "primefaces-material-indigo-dark");
        themes.put("primefaces-material-indigo-light", "primefaces-material-indigo-light");
        themes.put("primefaces-material-light-common", "primefaces-material-light-common");
        themes.put("primefaces-mytheme", "primefaces-mytheme");
        themes.put("primefaces-nova-colored", "primefaces-nova-colored");
        themes.put("primefaces-nova-common", "primefaces-nova-common");
        themes.put("primefaces-nova-dark", "primefaces-nova-dark");
        themes.put("primefaces-nova-light", "primefaces-nova-light");
        themes.put("primefaces-saga-blue", "primefaces-saga-blue");
        themes.put("primefaces-vela-blue", "primefaces-vela-blue");
    }
}
