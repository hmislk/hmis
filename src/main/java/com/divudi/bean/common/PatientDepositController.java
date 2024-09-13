/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;
import com.divudi.entity.Department;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientDeposit;
import com.divudi.facade.PatientDepositFacade;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class PatientDepositController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private PatientDepositFacade patientDepositFacade;
    private PatientDeposit current;
    private List<PatientDeposit> items = null;
    

    private int patientDepositManagementIndex=1;
    

    public PatientDeposit getDepositOfThePatient(Patient p , Department d){        
        Map m = new HashMap<>();
        
        String jpql = "select pd from PatientDeposit pd"
                + " where pd.patient=:pt "
                + " and pd.department=:dep "
                + " and pd.retired=:ret";
        
        m.put("pt",p);
        m.put("dep", d);
        m.put("ret", false);
        
        PatientDeposit pd = patientDepositFacade.findFirstByJpql(jpql, m);
        
        if(pd == null){
            pd = new PatientDeposit();
            patientDepositFacade.create(pd);
        }
        return pd;
    }
    

    public PatientDepositFacade getPatientDepositFacade() {
        return patientDepositFacade;
    }

    public void setPatientDepositFacade(PatientDepositFacade patientDepositFacade) {
        this.patientDepositFacade = patientDepositFacade;
    }

    public PatientDeposit getCurrent() {
        return current;
    }

    public void setCurrent(PatientDeposit current) {
        this.current = current;
    }

    public List<PatientDeposit> getItems() {
        return items;
    }

    public void setItems(List<PatientDeposit> items) {
        this.items = items;
    }


    public int getPatientDepositManagementIndex() {
        return patientDepositManagementIndex;
    }

    public void setPatientDepositManagementIndex(int patientDepositManagementIndex) {
        this.patientDepositManagementIndex = patientDepositManagementIndex;
    }


    /**
     *
     */
    @FacesConverter(forClass = PatientDeposit.class)
    public static class PatientDepositConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PatientDepositController controller = (PatientDepositController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "patientDepositController");
            return controller.getPatientDepositFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof PatientDeposit) {
                PatientDeposit o = (PatientDeposit) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PatientDeposit.class.getName());
            }
        }
    }

    
}
