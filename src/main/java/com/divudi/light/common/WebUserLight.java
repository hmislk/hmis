package com.divudi.light.common;

/**
 *
 * @author Dr M H B Ariyaratne
 */
public class WebUserLight {
    private String userName;
    private Long id;

    public WebUserLight() {
    }

    public WebUserLight(String userName, Long id) {
        this.userName = userName;
        this.id = id;
    }
    
    

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    
}
