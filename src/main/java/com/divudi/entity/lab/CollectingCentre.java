/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.entity.lab;

/**
 *
 * @author acer
 */
public class CollectingCentre {

    private String code = null;
    private String agentName = null;
    private Boolean active = null;
    private Boolean withCommissionStatus = null;
    private String routeName = null;
    private Double percentage = null;
    private String availableInPriceList = null;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getWithCommissionStatus() {
        return withCommissionStatus;
    }

    public void setWithCommissionStatus(Boolean withCommissionStatus) {
        this.withCommissionStatus = withCommissionStatus;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public String getAvailableInPriceList() {
        return availableInPriceList;
    }

    public void setAvailableInPriceList(String availableInPriceList) {
        this.availableInPriceList = availableInPriceList;
    }

}
