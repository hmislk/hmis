/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.lab;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author buddhika_ari
 */
public class Selectable {
    String name;
    List<String> options;
    String selectedOption;
    boolean inputText;
    boolean selectOneMenu;
    String selectedValue;
    String fullText;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getOptions() {
        if (options == null) {
            options = new ArrayList<>();
        }
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(String selectedOption) {
        this.selectedOption = selectedOption;
    }

    public boolean isInputText() {
        return inputText;
    }

    public void setInputText(boolean inputText) {
        this.inputText = inputText;
    }

    public boolean isSelectOneMenu() {
        return selectOneMenu;
    }

    public void setSelectOneMenu(boolean selectOneMenu) {
        this.selectOneMenu = selectOneMenu;
    }

    public String getSelectedValue() {
        if(selectedValue==null){
            selectedValue = "";
        }
        return selectedValue;
    }

    public void setSelectedValue(String selectedValue) {
        this.selectedValue = selectedValue;
    }

    public String getFullText() {
        return fullText;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText;
    }
    
    
    
}
