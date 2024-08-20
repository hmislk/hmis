/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb.clinical;

import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.facade.PatientEncounterFacade;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author pdhs
 */
@Stateless
public class ClinicalSearch {

    @EJB
    PatientEncounterFacade peFacade;

    String sql;
    Map m;
    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")

    public List<PatientEncounter> listPatientEncounters(Patient pt) {
        sql = "select pe from PatientEncounter pe "
                + "where pe.retired=false and "
                + "pe.patient=:pt "
                + "order by pe.id desc";
        m = new HashMap();
        m.put("pt", pt);
        return getPeFacade().findByJpql(sql, m);
    }

    public PatientEncounterFacade getPeFacade() {
        return peFacade;
    }

    public void setPeFacade(PatientEncounterFacade peFacade) {
        this.peFacade = peFacade;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public Map getM() {
        return m;
    }

    public void setM(Map m) {
        this.m = m;
    }

    
    
    
    
    
    
    
    
}
