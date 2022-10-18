/*
 * Author : Dr. M H B Ariyaratne, MO(Health Information), email : buddhika.ari@gmail.com
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.PostConstruct;
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

//    @EJB
//    WebUserFacade facade;
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
            theme = "nova-light";
            return theme;
        }
        switch (theme) {
            case "nova-light":
            case "nova-dark":
            case "nova-colored":
            case "luna-amber":
            case "luna-blue":
            case "luna-green":
            case "luna-pink":
                return theme;
            default:
                theme = "nova-light";
        }
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public void saveTheme() {
        if (getSessionController().getLoggedUser() != null) {
            getSessionController().getLoggedUser().setPrimeTheme(theme);
//            getFacade().edit(getSessionController().getLoggedUser());
            UtilityController.addSuccessMessage("Theme updated");
        }
    }

//    public WebUserFacade getFacade() {
//        return facade;
//    }
//
//    public void setFacade(WebUserFacade facade) {
//        this.facade = facade;
//    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    @PostConstruct
    public void init() {
        themes = new TreeMap<String, String>();
        themes.put("nova-light", "nova-light");
        themes.put("nova-dark", "nova-dark");
        themes.put("nova-colored", "nova-colored");
        themes.put("luna-amber", "luna-amber");
        themes.put("luna-blue", "luna-blue");
        themes.put("luna-green", "luna-green");
        themes.put("luna-pink", "luna-pink");

//        themes.put("Aristo", "aristo");
//        themes.put("Black-Tie", "black-tie");
//        themes.put("Blitzer", "blitzer");
//        themes.put("Bluesky", "bluesky");
//        themes.put("Casablanca", "casablanca");
//        themes.put("Cupertino", "cupertino");
//        themes.put("Dark-Hive", "dark-hive");
//        themes.put("Dot-Luv", "dot-luv");
//        themes.put("Eggplant", "eggplant");
//        themes.put("Excite-Bike", "excite-bike");
//        themes.put("Flick", "flick");
//        themes.put("Glass-X", "glass-x");
//        themes.put("Hot-Sneaks", "hot-sneaks");
//        themes.put("Humanity", "humanity");
//        themes.put("Le-Frog", "le-frog");
//        themes.put("Midnight", "midnight");
//        themes.put("Mint-Choc", "mint-choc");
//        themes.put("Overcast", "overcast");
//        themes.put("Pepper-Grinder", "pepper-grinder");
//        themes.put("Redmond", "redmond");
//        themes.put("Rocket", "rocket");
//        themes.put("Sam", "sam");
//        themes.put("Smoothness", "smoothness");
//        themes.put("South-Street", "south-street");
//        themes.put("Start", "start");
//        themes.put("Sunny", "sunny");
//        themes.put("Swanky-Purse", "swanky-purse");
//        themes.put("Trontastic", "trontastic");
//        themes.put("UI-Darkness", "ui-darkness");
//        themes.put("UI-Lightness", "ui-lightness");
//        themes.put("Vader", "vader");
    }
}
