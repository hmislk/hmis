/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Logins;
import com.divudi.facade.LoginsFacade;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author Buddhika
 */
@Named(value = "loginController")
@SessionScoped
public class LoginController implements Serializable {

    Department department;
    Institution institution;
    Date fromDate;
    Date toDate;
    Logins longin;
    List<Logins> logins;
    @Inject
    LoginsFacade facade;

    /**
     * Creates a new instance of LoginController
     */
    public LoginController() {
    }

    private void recreate() {
        logins = null;
    }

    public Date getFromDate() {

        return fromDate;
    }

    public LoginsFacade getFacade() {
        return facade;
    }

    public void setFacade(LoginsFacade facade) {
        this.facade = facade;
    }

    public void setFromDate(Date fromDate) {
        recreate();
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        recreate();
        this.toDate = toDate;
    }

    public Logins getLongin() {
        return longin;
    }

    public void setLongin(Logins longin) {
        this.longin = longin;
    }

    public List<Logins> getLogins() {
        if (logins == null) {
            String sql;
            Map m = new HashMap();
            m.put("fromDate", fromDate);
            m.put("toDate", toDate);
            sql = "select l from Logins l where l.logedAt between :fromDate and :toDate or l.logoutAt between :fromDate and :toDate  order by l.logedAt, l.logoutAt";
            logins = getFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        }
        return logins;
    }

    public void setLogins(List<Logins> logins) {
        this.logins = logins;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        recreate();
        this.department = department;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        recreate();
        this.institution = institution;
    }
}
