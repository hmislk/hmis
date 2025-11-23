package com.divudi.bean.lab;

import com.divudi.core.data.dto.SampleDTO;
import com.divudi.core.data.lab.PatientInvestigationStatus;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.lab.PatientSample;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.PatientReportFacade;
import com.divudi.core.facade.PatientSampleComponantFacade;
import com.divudi.core.facade.PatientSampleFacade;
import com.divudi.core.facade.UploadFacade;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Named
@SessionScoped
public class LaboratoryCommonController implements Serializable {

    private static final long serialVersionUID = 1L;

    public LaboratoryCommonController() {

    }

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private BillFacade billFacade;
    @EJB
    private PatientSampleFacade patientSampleFacade;
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    private PatientSampleComponantFacade patientSampleComponantFacade;
    @EJB
    PatientReportFacade patientReportFacade;
    @EJB
    UploadFacade uploadFacade;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Variables">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Navigation Method">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Function">
    // Convert Date to LocalDate
    public static LocalDate convertDateToLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public static String calculateAge(Date dob, Date billingDate) {
        if (dob == null || billingDate == null) {
            return "Invalid dates";
        }

        LocalDate localDob = convertDateToLocalDate(dob);
        LocalDate localBillingDate = convertDateToLocalDate(billingDate);

        return calculateAge(localDob, localBillingDate);
    }

    // Calculate age with LocalDate parameters
    public static String calculateAge(LocalDate dob, LocalDate billingDate) {
        if (dob == null || billingDate == null) {
            return "Invalid dates";
        }

        if (dob.isAfter(billingDate)) {
            return "DOB cannot be after billing date";
        }

        Period period = Period.between(dob, billingDate);
        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();

        String ageAsShortString;
        if (years > 150) {
            ageAsShortString = "DOB Invalid. ";
        } else if (years > 12) {
            ageAsShortString = years + " Years ";
        } else if (years > 0) {
            ageAsShortString = years + " Years " + months + " Months ";
        } else if (months > 0) {
            ageAsShortString = months + " Months " + days + " Days ";
        } else {
            ageAsShortString = days + " Days ";
        }

        return ageAsShortString;
    }

    public String getCollectingCentrNameFromSampleID(Long sampleId) {
        PatientSample currentSample = patientSampleFacade.findWithoutCache(sampleId);
        if (currentSample == null || currentSample.getBill() == null || currentSample.getBill().getCollectingCentre() == null) {
            return "";
        } else {
            return currentSample.getBill().getCollectingCentre().getName();
        }
    }
    
    public String getCollectingCentrNameFromPatientInvestigationID(Long patientInvestigationId) {
        PatientInvestigation pi = patientInvestigationFacade.findWithoutCache(patientInvestigationId);
        if (pi == null || pi.getBillItem() == null || pi.getBillItem().getBill() == null || pi.getBillItem().getBill().getCollectingCentre() == null) {
            return "";
        } else {
            return pi.getBillItem().getBill().getCollectingCentre().getName();
        }
    }

    public String getBHTNumberFromSampleID(Long sampleId) {
        PatientSample currentSample = patientSampleFacade.findWithoutCache(sampleId);
        if (currentSample == null || currentSample.getBill() == null || currentSample.getBill().getPatientEncounter() == null || currentSample.getBill().getPatientEncounter().getBhtNo() == null) {
            return "";
        } else {
            return currentSample.getBill().getPatientEncounter().getBhtNo();
        }
    }
    
    public String getBHTNumberFromPatientInvestigationId(Long patientInvestigationId) {
        PatientInvestigation pi = patientInvestigationFacade.findWithoutCache(patientInvestigationId);
        
        if (pi == null || pi.getBillItem() == null || pi.getBillItem().getBill() == null || pi.getBillItem().getBill().getPatientEncounter() == null) {
            return "";
        } else {
            return pi.getBillItem().getBill().getPatientEncounter().getBhtNo();
        }
    }

    public List<PatientInvestigation> getPatientInvestigationsFromSample(Long sampleId) {
        String j = "select psc from PatientSampleComponant psc where psc.patientSample.id = :psId and psc.separated =:sept and psc.retired = :ret";

        Map m = new HashMap();
        m.put("psId", sampleId);
        m.put("sept", false);
        m.put("ret", false);
        return patientSampleComponantFacade.findByJpql(j, m);
    }

    public SampleDTO refreshPatientSampleDTO(Long sampleId) {
        System.out.println("refreshPatientSampleDTO");
        String jpql = "SELECT new com.divudi.core.data.dto.SampleDTO( "
                + " COALESCE(ps.id, 0), "
                + " COALESCE(ps.tube.name, ''), "
                + " COALESCE(ps.bill.deptId, ''), "
                + " ps.bill.createdAt, "
                + " COALESCE(ps.bill.patient.id, 0 ), "
                + " ps.bill.patient.person.title,"
                + " COALESCE(ps.bill.patient.person.name, ''), "
                + " ps.bill.patient.person.dob, "
                + " COALESCE(ps.bill.patient.person.sex, ''), "
                + " COALESCE(ps.bill.patient.person.mobile, ''), "
                + " COALESCE(ps.bill.ipOpOrCc, ''), "
                + " ps.status, "
                + " ps.bill.cancelled, "
                + " ps.createdAt, "
                + " ps.barcodeGenerated, "
                + " ps.barcodeGeneratedAt, "
                + " ps.sampleCollected, "
                + " ps.sampleCollectedAt, "
                + " ps.sampleSent, "
                + " ps.sampleSentAt, "
                + " ps.sampleReceivedAtLab, "
                + " ps.sampleReceivedAtLabAt, "
                + " ps.cancelled, "
                + " ps.cancelledAt, "
                + " ps.sampleRejected, "
                + " ps.sampleRejectedAt "
                + " )"
                + "FROM PatientSample ps "
                + "WHERE ps.retired = :ret "
                + "AND ps.id = :id ";

        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("id", sampleId);

        List<SampleDTO> sampleDtos = (List<SampleDTO>) patientSampleFacade.findLightsByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);
        System.out.println("sampleDtos = " + sampleDtos.size());
        SampleDTO dto = sampleDtos.get(0);

        return dto;
    }
    
    

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Getter & Setter">
    // </editor-fold>
}
