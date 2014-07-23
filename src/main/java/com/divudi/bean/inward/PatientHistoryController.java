/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.inward;

import com.divudi.entity.BillItem;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Service;
import com.divudi.entity.inward.TimedItem;
import com.divudi.entity.lab.Investigation;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.TimedItemFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.ejb.EJB;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class PatientHistoryController implements Serializable {

    private Patient searchedPatient;
    private List<PatientEncounter> patientEncounters;
    private List<BillItem> service;
    private List<BillItem> timedItems;
    private List<BillItem> investigations;
    @EJB
    private TimedItemFacade timedItemFacade;
    @EJB
    private BillItemFacade serviceFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;

    /**
     * Creates a new instance of PatientHistoryController
     */
    public PatientHistoryController() {
    }

    public Patient getSearchedPatient() {
        return searchedPatient;
    }

    public void setSearchedPatient(Patient searchedPatient) {
        this.searchedPatient = searchedPatient;
        gettingData();
    }

    private void gettingData() {
        if (searchedPatient == null) {
            return;
        }
        //and TYPE(bt.item)=Service 
        String sql = "SELECT bt FROM BillItem bt WHERE bt.retired=false and  bt.bill.id in (SELECT  b.id FROM BilledBill b WHERE b.retired=false  and b.patient=:pt)";
        HashMap hm = new HashMap();
        hm.put("pt", getSearchedPatient());

        List<BillItem> temp = getServiceFacade().findBySQL(sql, hm);

        for (BillItem b : temp) {
            if (b.getItem() instanceof Service) {
                service.add(b);
            } else if (b.getItem() instanceof TimedItem) {
                timedItems.add(b);
            } else if (b.getItem() instanceof Investigation) {
                investigations.add(b);
            }
        }

        if (service == null) {
            service = new ArrayList<BillItem>();
        }
        if (timedItems == null) {
            timedItems = new ArrayList<BillItem>();
        }
        if (investigations == null) {
            investigations = new ArrayList<BillItem>();
        }

    }

    //   String sql = "SELECT pi FROM PatientInvestigation pi WHERE pi.retired=false AND pi.patient.id=" + getSearchedPatient().getId();
    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }

    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
    }

    public List<PatientEncounter> getPatientEncounters() {
        if (searchedPatient == null) {
            return new ArrayList<PatientEncounter>();
        }


        String sql = "select pe from PatientEncounter pe where pe.retired=false and pe.patient=:pt";
        HashMap hm = new HashMap();
        hm.put("pt", getSearchedPatient());
        patientEncounters = getPatientEncounterFacade().findBySQL(sql, hm);

        if (patientEncounters == null) {
            return new ArrayList<PatientEncounter>();
        }

        return patientEncounters;
    }

    public void setPatientEncounters(List<PatientEncounter> patientEncounters) {



        this.patientEncounters = patientEncounters;
    }

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
    }

    public List<BillItem> getService() {
        if (service == null) {
            service = new ArrayList<BillItem>();
        }

        return service;
    }

    public void setService(List<BillItem> service) {
        this.service = service;
    }

    public BillItemFacade getServiceFacade() {
        return serviceFacade;
    }

    public void setServiceFacade(BillItemFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }

    public List<BillItem> getTimedItems() {
        if (timedItems == null) {
            timedItems = new ArrayList<BillItem>();
        }

        return timedItems;
    }

    public void setTimedItems(List<BillItem> timedItems) {
        this.timedItems = timedItems;
    }

    public TimedItemFacade getTimedItemFacade() {
        return timedItemFacade;
    }

    public void setTimedItemFacade(TimedItemFacade timedItemFacade) {
        this.timedItemFacade = timedItemFacade;
    }

    public List<BillItem> getInvestigations() {
        if (investigations == null) {
            investigations = new ArrayList<BillItem>();
        }

        return investigations;
    }

    public void setInvestigations(List<BillItem> investigations) {
        this.investigations = investigations;
    }
}
