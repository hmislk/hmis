/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Named; import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;

import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class LanguageController implements Serializable {

    /**
     * Creates a new instance of LanguageController
     */
    public LanguageController() {
    }
    private static final long serialVersionUID = 1L;
    private String localeCode;
    private static Map<String, Object> countries;

    static {
        Locale sinhalaLocale = new Locale("si", "LK");
        Locale tamilLocale = new Locale("ta", "LK");
        countries = new LinkedHashMap<String, Object>();
        countries.put("English", Locale.ENGLISH); //label, value
        countries.put("Sinhala", sinhalaLocale);
        countries.put("Tamil", tamilLocale);
    }

    public Map<String, Object> getCountriesInMap() {
        return countries;
    }

    public String getLocaleCode() {
        return localeCode;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    //value change event listener
    public void countryLocaleCodeChanged(ValueChangeEvent e) {

        String newLocaleValue = e.getNewValue().toString();

        //loop country map to compare the locale code
        for (Map.Entry<String, Object> entry : countries.entrySet()) {

            if (entry.getValue().toString().equals(newLocaleValue)) {

                FacesContext.getCurrentInstance()
                        .getViewRoot().setLocale((Locale) entry.getValue());

            }
        }
    }
}
