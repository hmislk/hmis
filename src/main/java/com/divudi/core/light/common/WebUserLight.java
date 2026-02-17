package com.divudi.core.light.common;

public class WebUserLight {

    private String userName;
    private Long id;
    private String name;
    private String roleName;
    private String code;
    private String staffNameWithTitle;

    public WebUserLight() {
    }

    public WebUserLight(String userName, Long id) {
        this.userName = userName;
        this.id = id;
    }

    public WebUserLight(String userName, String name, Long id) {
        this.userName = userName;
        this.name = name;
        this.id = id;
    }

    public WebUserLight(String userName, String name, Long id, String code, String staffNameWithTitle) {
        this.userName = userName;
        this.name = name;
        this.id = id;
        this.code = code;
        this.staffNameWithTitle = staffNameWithTitle;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStaffNameWithTitle() {
        return staffNameWithTitle;
    }

    public void setStaffNameWithTitle(String staffNameWithTitle) {
        this.staffNameWithTitle = staffNameWithTitle;
    }
}
