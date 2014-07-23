/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.BillType;
import com.divudi.entity.BillFee;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Patient;
import com.divudi.entity.Staff;
import com.divudi.entity.inward.EncounterComponent;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.inward.Admission;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.EncounterComponentFacade;
import com.divudi.facade.PatientEncounterFacade;
import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class TheatreModuleController implements Serializable {

    /**
     * Properties
     */
    Patient patient;
    Admission admission;
    PatientEncounter procedure;
    EncounterComponent encounterComponent;
    List<EncounterComponent> encounterComponents;
    List<PatientEncounter> procedures;
    Staff staff;
    /**
     * EJBs
     */
    @EJB
    EncounterComponentFacade ecFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    PatientEncounterFacade peFacade;
    /**
     * Managed Beans
     */
    @Inject
    SessionController sessionController;

    public void makeNull() {
        patient = null;
        admission = null;
        procedure = null;
        encounterComponent = null;
        encounterComponents = null;
        procedures = null;
        staff = null;

    }

    /**
     * Creates a new instance of TheatreModuleController
     */
    public TheatreModuleController() {
    }

    /**
     * Methods
     */
    public void saveProcedure() {
        if (admission == null) {
            UtilityController.addErrorMessage("Please select encounter");
            return;
        }
        if (getProcedure() == null) {
            return;
        }
        if (getProcedure().getItem() == null) {
            UtilityController.addErrorMessage("Please select Surgary");
            return;
        }

        if (getProcedure().getId() == null || getProcedure().getId() == 0) {
            getPeFacade().create(procedure);
        } else {
            getPeFacade().edit(procedure);
        }

    }

    public void newProcedure() {
        procedure = null;
        staff = null;
        encounterComponent = null;
    }

    public void addComponantForAdmissions() {
        if (getAdmission() == null) {
            UtilityController.addErrorMessage("Admission ?");
            return;
        }
        if (getProcedure() == null) {
            UtilityController.addErrorMessage("Procedure");
            return;
        }
        if (getEncounterComponent() == null) {
            UtilityController.addErrorMessage("Component");
            return;
        }

        saveProcedure();

        if (encounterComponent.getBillFee() != null && encounterComponent.getBillFee().getFeeValue() != 0) {

            BilledBill bill = new BilledBill();
            bill.setGrantTotal(encounterComponent.getBillFee().getFeeValue());
            bill.setCreatedAt(Calendar.getInstance().getTime());
            bill.setCreater(getSessionController().getLoggedUser());
            bill.setBillTime(Calendar.getInstance().getTime());
            bill.setBillDate(Calendar.getInstance().getTime());
            bill.setPatientEncounter(admission);
            bill.setBillType(BillType.InwardBill);
            bill.setDepartment(getSessionController().getDepartment());
            getBillFacade().create(bill);

            BillFee bf = encounterComponent.getBillFee();
            bf.setBill(bill);
            bf.setCreatedAt(Calendar.getInstance().getTime());
            bf.setCreater(getSessionController().getLoggedUser());
            bf.setFeeAt(Calendar.getInstance().getTime());
            bf.setFeeGrossValue(bf.getFeeValue());
            bf.setDepartment(getSessionController().getDepartment());
            bf.setPatienEncounter(getProcedure());
            bf.setPatient(admission.getPatient());
            bf.setInstitution(getSessionController().getInstitution());
            bf.setStaff(staff);
            getBillFeeFacade().create(bf);

        }
        encounterComponent.setCreatedAt(Calendar.getInstance().getTime());
        encounterComponent.setCreater(getSessionController().getLoggedUser());
        encounterComponent.setPatientEncounter(procedure);
        encounterComponent.setStaff(staff);
        getEcFacade().edit(encounterComponent);

        procedure.getEncounterComponents().add(encounterComponent);
        getPeFacade().edit(procedure);
//        getPeFacade().flush();
//
//        procedure = getPeFacade().find(procedure.getId());

        UtilityController.addSuccessMessage("Added");
        encounterComponent = null;
        staff = null;
    }

    /**
     * Getters and Setters
     *
     * @return
     */
    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Admission getAdmission() {
        return admission;
    }

    public void setAdmission(Admission admission) {
        this.admission = admission;
        procedures = this.admission.getChildEncounters();
    }

    public PatientEncounter getProcedure() {
        if (procedure == null) {
            procedure = new PatientEncounter();
        }
        return procedure;
    }

    public void setProcedure(PatientEncounter procedure) {
        this.procedure = procedure;
    }

    public List<EncounterComponent> getEncounterComponents() {
        return encounterComponents;
    }

    public void setEncounterComponents(List<EncounterComponent> encounterComponents) {
        this.encounterComponents = encounterComponents;
    }

    public EncounterComponent getEncounterComponent() {
        if (encounterComponent == null) {
            encounterComponent = new EncounterComponent();
            if (getEncounterComponents() == null) {
                encounterComponent.setOrderNo(0);
            } else {
                encounterComponent.setOrderNo(getEncounterComponents().size());
            }
            BillFee bf = new BillFee();
            encounterComponent.setBillFee(bf);
        }
        return encounterComponent;
    }

    public void setEncounterComponent(EncounterComponent encounterComponent) {
        this.encounterComponent = encounterComponent;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public EncounterComponentFacade getEcFacade() {
        return ecFacade;
    }

    public void setEcFacade(EncounterComponentFacade ecFacade) {
        this.ecFacade = ecFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public PatientEncounterFacade getPeFacade() {
        return peFacade;
    }

    public void setPeFacade(PatientEncounterFacade peFacade) {
        this.peFacade = peFacade;
    }

    public List<PatientEncounter> getProcedures() {
        return procedures;
    }

    public void setProcedures(List<PatientEncounter> procedures) {
        this.procedures = procedures;
    }

}
