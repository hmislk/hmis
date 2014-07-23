/*
 * Author : Dr. M H B Ariyaratne, MO(Health Information), email : buddhika.ari@gmail.com
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.facade.WebUserFacade;
import java.io.Serializable;
import javax.inject.Named; import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Inject;

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
        } else {
            getSessionController().setPrimeTheme(theme);
            UtilityController.addErrorMessage("Theme NOT updated. Please login before you change the theme permamtely");
        }
    }

    public WebUserFacade getFacade() {
        return facade;
    }

    public void setFacade(WebUserFacade facade) {
        this.facade = facade;
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
        themes.put("Aristo", "aristo");
        themes.put("Black-Tie", "black-tie");
        themes.put("Blitzer", "blitzer");
        themes.put("Bluesky", "bluesky");
        themes.put("Casablanca", "casablanca");
        themes.put("Cupertino", "cupertino");
        themes.put("Dark-Hive", "dark-hive");
        themes.put("Dot-Luv", "dot-luv");
        themes.put("Eggplant", "eggplant");
        themes.put("Excite-Bike", "excite-bike");
        themes.put("Flick", "flick");
        themes.put("Glass-X", "glass-x");
        themes.put("Hot-Sneaks", "hot-sneaks");
        themes.put("Humanity", "humanity");
        themes.put("Le-Frog", "le-frog");
        themes.put("Midnight", "midnight");
        themes.put("Mint-Choc", "mint-choc");
        themes.put("Overcast", "overcast");
        themes.put("Pepper-Grinder", "pepper-grinder");
        themes.put("Redmond", "redmond");
        themes.put("Rocket", "rocket");
        themes.put("Sam", "sam");
        themes.put("Smoothness", "smoothness");
        themes.put("South-Street", "south-street");
        themes.put("Start", "start");
        themes.put("Sunny", "sunny");
        themes.put("Swanky-Purse", "swanky-purse");
        themes.put("Trontastic", "trontastic");
        themes.put("UI-Darkness", "ui-darkness");
        themes.put("UI-Lightness", "ui-lightness");
        themes.put("Vader", "vader");
    }
}
