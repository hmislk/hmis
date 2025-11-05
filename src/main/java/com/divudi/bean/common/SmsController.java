/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.core.data.MessageType;
import com.divudi.core.data.Sex;
import com.divudi.core.data.hr.ReportKeyWord;
import com.divudi.ejb.SmsManagerEjb;
import com.divudi.core.entity.Area;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Sms;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.SmsFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.util.CommonFunctions;

import java.util.List;
import java.util.Map;
import java.util.Calendar;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author Dushan
 */
@Named
@SessionScoped
public class SmsController implements Serializable {

    /*
    EJB
     */
    @EJB
    SmsFacade smsFacade;
    @EJB
    PatientFacade patientFacade;
    @EJB
    private SmsManagerEjb smsManager;
    /*
    Controllers
     */
    @Inject
    SessionController sessionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    /*
    Class Variables
     */
    List<Sms> smses;
    List<Sms> faildsms;
    private Sms selectedSms;
    private Boolean bool;

    private String smsMessage;
    private String smsNumber;
    private String smsNumbersInput;
    private String smsOutput;

    // Bulk SMS related fields
    private Integer ageFrom;
    private Integer ageTo;
    private Long maxNumberToList;
    private Sex sex;
    private Area area;
    private String smsTemplate;
    private List<Patient> patientsForSms;
    private List<Patient> selectedPatients;

    // New variable to control SMS sending
    private static boolean doNotSendAnySms = false;

    public List<Sms> getFaildsms() {
        return faildsms;
    }

    public void setFaildsms(List<Sms> faildsms) {
        this.faildsms = faildsms;
    }

    List<SmsSummeryRow> smsSummeryRows;
    ReportKeyWord reportKeyWord;
    private String number;
    private String message;
    private String messageOutput;
    private Date fromDate;
    private Date toDate;

    /**
     * Creates a new instance of SmsController
     */
    public SmsController() {
    }

    public Boolean sendSms(String number, String message, String username, String password, String sendingAlias) {
        if (doNotSendAnySms) {
            return false;
        }
        selectedSms = new Sms();
        selectedSms.setReceipientNumber(number);
        selectedSms.setSendingMessage(message);
        return smsManager.sendSms(selectedSms);
    }

    public void sendSms() {
        if (doNotSendAnySms) {
            return;
        }
        Sms s = new Sms();
        s.setReceipientNumber(smsNumber);
        s.setSendingMessage(smsMessage);
        save(s);
        boolean b = smsManager.sendSms(s);
        selectedSms = s;
    }

    public void sendSmsFromWeb() {
        if (doNotSendAnySms) {
            return;
        }
        Sms s = new Sms();
        s.setReceipientNumber(smsNumber);
        s.setSendingMessage(smsMessage);
        save(s);
        boolean b = smsManager.sendSms(s);
        selectedSms = s;
    }

    public Boolean sendSms(String number, String message) {
        if (doNotSendAnySms) {
            return false;
        }
        Sms s = new Sms();
        s.setReceipientNumber(number);
        s.setSendingMessage(message);
        return smsManager.sendSms(s);
    }

    public void fillAllSms() {
        String j = "select s "
                + " from Sms s "
                + " where s.createdAt between :fd and :td ";
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        smses = smsFacade.findByJpql(j, m, TemporalType.TIMESTAMP);
    }

    public void fillAllFaildSms() {
        // Modified by Dr M H B Ariyaratne with assistance from ChatGPT from OpenAI
        String j = "select s "
                + "from Sms s "
                + "where s.sentSuccessfully <> :suc "
                + "AND s.createdAt between :fd and :td";
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("suc", true);
        faildsms = smsFacade.findByJpql(j, m, TemporalType.TIMESTAMP);
    }

    public void sentUnsentSms() {
        if (doNotSendAnySms) {
            JsfUtil.addErrorMessage("SMS sending is disabled");
            return;
        }
        if (selectedSms == null) {
            JsfUtil.addErrorMessage("No SMS selected");
            return;
        }
        Boolean sendSms = smsManager.sendSms(selectedSms);
        if (sendSms) {
            JsfUtil.addSuccessMessage("Sent Successfully");
        } else {
            JsfUtil.addSuccessMessage("Sending failed");
        }
    }

    public List<Sms> allsms() {
        return getSmsFacade().findAll();
    }

    public List<SmsSummeryRow> getSmsSummeryRows() {
        return smsSummeryRows;
    }

    public void setSmsSummeryRows(List<SmsSummeryRow> smsSummeryRows) {
        this.smsSummeryRows = smsSummeryRows;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageOutput() {
        return messageOutput;
    }

    public void setMessageOutput(String messageOutput) {
        this.messageOutput = messageOutput;
    }

    public SmsManagerEjb getSmsManager() {
        return smsManager;
    }

    public void setSmsManager(SmsManagerEjb smsManager) {
        this.smsManager = smsManager;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Sms getSelectedSms() {
        return selectedSms;
    }

    public void setSelectedSms(Sms selectedSms) {
        this.selectedSms = selectedSms;
    }

    public String getSmsMessage() {
        return smsMessage;
    }

    public void setSmsMessage(String smsMessage) {
        this.smsMessage = smsMessage;
    }

    public String getSmsNumber() {
        return smsNumber;
    }

    public void setSmsNumber(String smsNumber) {
        this.smsNumber = smsNumber;
    }

    public String getSmsOutput() {
        return smsOutput;
    }

    public void setSmsOutput(String smsOutput) {
        this.smsOutput = smsOutput;
    }

    public Boolean getBool() {
        return bool;
    }

    public void setBool(Boolean bool) {
        this.bool = bool;
    }

    private void save(Sms s) {
        if (s == null) {
            JsfUtil.addErrorMessage("No SMS");
            return;
        }
        if (s.getId() == null) {
            s.setCreatedAt(new Date());
            s.setCreater(sessionController.getLoggedUser());
            smsFacade.create(s);
        } else {
            smsFacade.edit(s);
        }
    }

    public void searchPatientsForBulkSms() {
        if (doNotSendAnySms) {
            JsfUtil.addErrorMessage("SMS sending is disabled");
            return;
        }

        String j = "select p from Patient p where p.retired=false";
        Map<String, Object> m = new HashMap<>();
        if (sex != null) {
            j += " and p.person.sex=:sx";
            m.put("sx", sex);
        }
        if (area != null) {
            j += " and p.person.area=:ar";
            m.put("ar", area);
        }
        Date dobFrom = null;
        Date dobTo = null;
        if (ageFrom != null) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, -ageFrom);
            dobTo = cal.getTime();
        }
        if (ageTo != null) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, -ageTo);
            dobFrom = cal.getTime();
        }
        if (dobFrom != null) {
            j += " and p.person.dob >= :df";
            m.put("df", dobFrom);
        }
        if (dobTo != null) {
            j += " and p.person.dob <= :dt";
            m.put("dt", dobTo);
        }
        j += " order by p.person.name";
        patientsForSms = patientFacade.findByJpql(j, m, TemporalType.DATE, maxNumberToList.intValue());
    }

    private String applyPatientPlaceholders(Patient p, String template) {
        if (p == null || p.getPerson() == null) {
            return template;
        }
        String msg = template;
        msg = msg.replace("{name}", nvl(p.getPerson().getName()));
        msg = msg.replace("{phone1}", nvl(p.getPerson().getPhone()));
        msg = msg.replace("{phone2}", nvl(p.getPerson().getMobile()));
        msg = msg.replace("{address}", nvl(p.getPerson().getAddress()));
        msg = msg.replace("{area}", p.getPerson().getArea() != null ? p.getPerson().getArea().getName() : "");
        if (p.getPerson().getSex() != null) {
            msg = msg.replace("{he/she}", p.getPerson().getSex() == Sex.Male ? "he" : "she");
            msg = msg.replace("{sir/madam}", p.getPerson().getSex() == Sex.Male ? "sir" : "madam");
        }
        msg = msg.replace("{title}", p.getPerson().getTitle() != null ? p.getPerson().getTitle().getLabel() : "");
        return msg;
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    public void sendBulkSmsToPatients() {
        if (selectedPatients == null || selectedPatients.isEmpty()) {
            JsfUtil.addErrorMessage("No patients selected");
            return;
        }
        if (smsTemplate == null || smsTemplate.trim().isEmpty()) {
            JsfUtil.addErrorMessage("No SMS template");
            return;
        }
        for (Patient p : selectedPatients) {
            String msg = applyPatientPlaceholders(p, smsTemplate);
            String number = nvl(p.getPerson().getMobile());
            if (number.isEmpty()) {
                number = nvl(p.getPerson().getPhone());
            }
            if (number.isEmpty()) {
                continue;
            }
            Sms s = new Sms();
            s.setReceipientNumber(number);
            s.setSendingMessage(msg);
            save(s);
            smsManager.sendSms(s);
        }
        JsfUtil.addSuccessMessage("SMS sending initiated");
    }

    public String navigateToBillContactNumbers() {
        //to do. completly rewrite this method and page in a seperate issue
        return "/admin/users/sent_bulk_sms_to_bills.xhtml?faces-redirect=true";
    }

    public String navigateToSendBulkSmsToPatients() {
        selectedPatients = null;
        patientsForSms = null;
        smsTemplate = null;
        ageFrom = null;
        ageTo = null;
        sex = null;
        area = null;
        return "/admin/users/send_bulk_sms_patients?faces-redirect=true";
    }

    public String navigateToSendBulkSmsToNumbers() {
        smsNumbersInput = null;
        smsMessage = null;
        return "/admin/users/send_bulk_sms_numbers?faces-redirect=true";
    }

    public void sendBulkSmsToNumbers() {
        if (doNotSendAnySms) {
            JsfUtil.addErrorMessage("SMS sending is disabled");
            return;
        }
        if (smsNumbersInput == null || smsNumbersInput.trim().isEmpty()) {
            JsfUtil.addErrorMessage("No numbers specified");
            return;
        }
        if (smsMessage == null || smsMessage.trim().isEmpty()) {
            JsfUtil.addErrorMessage("No SMS message specified");
            return;
        }
        String[] arr = smsNumbersInput.split("[\n,]+");
        for (String n : arr) {
            String number = n.trim();
            if (number.isEmpty()) {
                continue;
            }
            Sms s = new Sms();
            s.setReceipientNumber(number);
            s.setSendingMessage(smsMessage.trim());
            s.setSmsType(MessageType.BulkNumberSms);
            save(s);
            smsManager.sendSms(s);
        }
        JsfUtil.addSuccessMessage("SMS sending initiated");
    }

    public Long getMaxNumberToList() {
        if (maxNumberToList == null) {
            maxNumberToList = configOptionApplicationController.getLongValueByKey("Maximum Number of Patients to List to Send Bulkl SMS", 100l);
        }
        return maxNumberToList;
    }

    public void setMaxNumberToList(Long maxNumberToList) {
        this.maxNumberToList = maxNumberToList;
    }

    public class SmsSummeryRow {

        MessageType smsType;
        long count;

        public MessageType getSmsType() {
            return smsType;
        }

        public void setSmsType(MessageType smsType) {
            this.smsType = smsType;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    //---------Getters and Setters
    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public SmsFacade getSmsFacade() {
        return smsFacade;
    }

    public void setSmsFacade(SmsFacade smsFacade) {
        this.smsFacade = smsFacade;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public List<Sms> getSmses() {
        return smses;
    }

    public void setSmses(List<Sms> smses) {
        this.smses = smses;
    }

    public Integer getAgeFrom() {
        return ageFrom;
    }

    public void setAgeFrom(Integer ageFrom) {
        this.ageFrom = ageFrom;
    }

    public Integer getAgeTo() {
        return ageTo;
    }

    public void setAgeTo(Integer ageTo) {
        this.ageTo = ageTo;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public String getSmsTemplate() {
        return smsTemplate;
    }

    public void setSmsTemplate(String smsTemplate) {
        this.smsTemplate = smsTemplate;
    }

    public List<Patient> getPatientsForSms() {
        return patientsForSms;
    }

    public void setPatientsForSms(List<Patient> patientsForSms) {
        this.patientsForSms = patientsForSms;
    }

    public List<Patient> getSelectedPatients() {
        return selectedPatients;
    }

    public void setSelectedPatients(List<Patient> selectedPatients) {
        this.selectedPatients = selectedPatients;
    }

    public String getSmsNumbersInput() {
        return smsNumbersInput;
    }

    public void setSmsNumbersInput(String smsNumbersInput) {
        this.smsNumbersInput = smsNumbersInput;
    }

}
