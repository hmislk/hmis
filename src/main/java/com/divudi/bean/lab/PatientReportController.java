package com.divudi.bean.lab;

import com.divudi.bean.common.ApplicationController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ItemForItemController;
import com.divudi.bean.common.PdfController;
import com.divudi.bean.common.SecurityController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.TransferController;
import com.divudi.bean.hr.StaffController;
import com.divudi.core.data.BooleanMessage;
import com.divudi.core.data.CalculationType;
import com.divudi.core.data.InvestigationItemType;
import com.divudi.core.data.InvestigationItemValueType;
import com.divudi.core.data.InvestigationReportType;
import com.divudi.core.data.MessageType;
import com.divudi.core.data.Sex;
import com.divudi.core.data.lab.Selectable;
import com.divudi.ejb.EmailManagerEjb;
import com.divudi.ejb.PatientReportBean;
import com.divudi.ejb.SmsManagerEjb;
import com.divudi.core.entity.AppEmail;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Sms;
import com.divudi.core.entity.UserPreference;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.lab.InvestigationItem;
import com.divudi.core.entity.lab.IxCal;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.lab.PatientReport;
import com.divudi.core.entity.lab.PatientReportItemValue;
import com.divudi.core.entity.lab.TestFlag;
import com.divudi.core.facade.EmailFacade;
import com.divudi.core.facade.IxCalFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.PatientInvestigationItemValueFacade;
import com.divudi.core.facade.PatientReportFacade;
import com.divudi.core.facade.PatientReportItemValueFacade;
import com.divudi.core.facade.SmsFacade;
import com.divudi.core.facade.TestFlagFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.ReportType;
import com.divudi.core.data.UploadType;
import com.divudi.core.data.lab.PatientInvestigationStatus;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.Upload;
import com.divudi.core.entity.clinical.ClinicalFindingValue;
import com.divudi.core.entity.lab.PatientReportGroup;
import com.divudi.core.entity.lab.PatientSample;
import com.divudi.core.entity.lab.PatientSampleComponant;
import com.divudi.core.entity.lab.ReportFormat;
import com.divudi.core.facade.ClinicalFindingValueFacade;
import com.divudi.core.facade.UploadFacade;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import com.divudi.core.util.CommonFunctions;
import org.apache.commons.lang3.math.NumberUtils;
import org.primefaces.event.CellEditEvent;
import javax.faces.context.FacesContext;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class PatientReportController implements Serializable {

    private static final long serialVersionUID = 1L;
    // EJBs
    @EJB
    private PatientReportFacade ejbFacade;
    @EJB
    private EmailManagerEjb emailManagerEjb;
    @EJB
    PatientInvestigationItemValueFacade piivFacade;
    @EJB
    PatientReportItemValueFacade pirivFacade;
    @EJB
    PatientInvestigationFacade piFacade;
    @EJB
    IxCalFacade ixCalFacade;
    @EJB
    private TestFlagFacade testFlagFacade;
    @EJB
    private EmailFacade emailFacade;
    @EJB
    SmsManagerEjb smsManager;
    @EJB
    SmsFacade smsFacade;
    @EJB
    ClinicalFindingValueFacade clinicalFindingValueFacade;
    @EJB
    private UploadFacade uploadFacade;

//Controllers
    @Inject
    PdfController pdfController;
    @Inject
    private PatientReportBean prBean;
    @Inject
    SessionController sessionController;
    @Inject
    StaffController staffController;
    @Inject
    TransferController transferController;
    @Inject
    ItemForItemController itemForItemController;
    @Inject
    private InvestigationItemController investigationItemController;
    @Inject
    CommonReportItemController commonReportItemController;
    @Inject
    ReportFormatController reportFormatController;
    @Inject
    private ApplicationController applicationController;
    @Inject
    private SecurityController securityController;
    @Inject
    PatientInvestigationController patientInvestigationController;
    @Inject
    PatientReportUploadController patientReportUploadController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    LabTestHistoryController labTestHistoryController;

    //Class Variables
    String selectText = "";
    private String groupName;
    private PatientInvestigation currentPtIx;
    private PatientReport currentPatientReport;
    private String receipientEmail;
    private PatientSample currentPatientSample;
    Investigation currentReportInvestigation;
    Investigation alternativeInvestigation;
    private PatientSampleComponant currentPatientSampleComponant;
    List<PatientReportItemValue> patientReportItemValues;
    List<PatientReportItemValue> patientReportItemValuesValues;
    List<PatientReportItemValue> patientReportItemValuesFlags;
    List<PatientReportItemValue> patientReportItemValuesDynamicLabels;
    List<PatientReportItemValue> patientReportItemValuesCalculations;
    List<PatientReport> customerReports = new ArrayList<>();
    List<PatientInvestigation> customerPis;
    InvestigationItem investigationItem;
    private List<Selectable> selectables = new ArrayList<>();
    private String encryptedPatientReportId;
    private String encryptedExpiary;
    private List<PatientReport> recentReportsOrderedByDoctor;
    private String smsNumber;
    private String smsMessage;
    private boolean showBackground = false;
    private ClinicalFindingValue clinicalFindingValue;
    private String comment;

    public StreamedContent getReportAsPdf() {
        StreamedContent pdfSc = null;
        try {
            pdfSc = pdfController.createPdfForPatientReport(currentPatientReport);
        } catch (IOException e) {
            System.err.println("e = " + e);
        }
        return pdfSc;
    }

    public String navigateToViewPatientReport(PatientReport patientReport) {
        if (null == patientReport.getReportType()) {
            setCurrentPatientReport(patientReport);
            return "/lab/patient_report?faces-redirect=true";
        } else {
            switch (patientReport.getReportType()) {
                case GENARATE:
                    System.out.println("GENARATE");
                    setCurrentPatientReport(patientReport);
                    fillReportFormats(patientReport);
                    return "/lab/patient_report?faces-redirect=true";
                case UPLOAD:
                    System.out.println("UPLOAD");

                    Upload u = loadUpload(patientReport);

                    System.out.println("Report = " + patientReport);
                    System.out.println("Patient Investigation = " + patientReport.getPatientInvestigation());

                    if (u != null) {
                        patientReportUploadController.setReportUpload(u);
                        System.out.println("Upload Report = " + u.getPatientReport());
                        System.out.println("Upload Investigation = " + u.getPatientInvestigation());
                    } else {
                        patientReportUploadController.setReportUpload(null);
                    }

                    patientReportUploadController.setPatientInvestigation(patientReport.getPatientInvestigation());
                    return "/lab/upload_patient_report?faces-redirect=true";
                default:
                    return "";
            }
        }
    }

    public Upload loadUpload(PatientReport pr) {
        String jpql = "select u "
                + " from Upload u "
                + " where u.retired=:ret"
                + " and u.patientReport=:pr"
                + " and u.patientReport.retired=:prr"
                + " and u.uploadType=:ut";

        Map params = new HashMap<>();
        params.put("ret", false);
        params.put("pr", pr);
        params.put("ut", UploadType.Lab_Report);
        params.put("prr", false);

        return uploadFacade.findFirstByJpql(jpql, params);
    }

    public String navigateToPrintPatientReport(PatientReport pr) {
        if (pr == null) {
            JsfUtil.addErrorMessage("No Select Patient Report");
            return "";
        }

        System.out.println("pr = " + pr.getReportType());

        if (pr.getReportType() == null) {
            setCurrentPatientReport(pr);
            return "/lab/patient_report_print?faces-redirect=true";
        } else {
            switch (pr.getReportType()) {
                case GENARATE:
                    setCurrentPatientReport(pr);
                    return "/lab/patient_report_print?faces-redirect=true";
                case UPLOAD:
                    Upload currentReportUpload = loadUpload(pr);
                    patientReportUploadController.setReportUpload(currentReportUpload);
                    return "/lab/upload_patient_report_print?faces-redirect=true";
                default:
                    return "";
            }
        }

    }

    public String navigateToPrintPatientReportForCourier(PatientReport pr) {
        if (pr == null) {
            JsfUtil.addErrorMessage("No Select Patient Report");
            return "";
        }

        System.out.println("pr = " + pr.getReportType());

        if (pr.getReportType() == null) {
            setCurrentPatientReport(pr);
            return "/collecting_centre/courier/patient_report_print?faces-redirect=true";
        } else {
            switch (pr.getReportType()) {
                case GENARATE:
                    setCurrentPatientReport(pr);
                    return "/collecting_centre/courier/patient_report_print?faces-redirect=true";
                case UPLOAD:
                    Upload currentReportUpload = loadUpload(pr);
                    patientReportUploadController.setReportUpload(currentReportUpload);
                    return "/collecting_centre/courier/upload_patient_report_print?faces-redirect=true";
                default:
                    return "";
            }
        }

    }

    public String searchRecentReportsOrderedByMyself() {
        Doctor doctor;
        String j = "Select r from PatientReport r "
                + " where r.patientInvestigation.billItem.bill.referredBy=:doc "
                + " and r.approved=:app "
                + " order by r.approveAt";
        Map m = new HashMap();
        if (getSessionController()
                .getLoggedUser().getStaff() instanceof Doctor) {
            doctor = (Doctor) getSessionController().getLoggedUser().getStaff();
        } else {
            JsfUtil.addErrorMessage("You are NOT a Doctor. Only Doctors can view recent Investigations you order.");
            recentReportsOrderedByDoctor = null;
            return "";
        }
        m.put("doc", doctor);
        m.put("app", true);
        recentReportsOrderedByDoctor = getFacade().findByJpql(j, m, 100);
        if (false) {
            PatientReport r = new PatientReport();
            r.getApproved();
            r.getPatientInvestigation().getBillItem().getBill().getReferredBy();
            r.getApproveAt();
        }

        return "/mobile/recent_reports_ordered_by_mysqlf";
    }

    public void preparePatientReportByIdForRequests() {
        currentPatientReport = null;
        if (encryptedPatientReportId == null) {
            return;
        }
        if (encryptedExpiary != null) {
            Date expiaryDate;
            try {
                String ed = encryptedExpiary;
                ed = securityController.decrypt(ed);
                if (ed == null) {
                    return;
                }
                expiaryDate = new SimpleDateFormat("ddMMMMyyyyhhmmss").parse(ed);
            } catch (ParseException ex) {
                return;
            }
            if (expiaryDate.before(new Date())) {
                return;
            }
        }
        String idStr = getSecurityController().decrypt(encryptedPatientReportId);
        Long id = 0l;
        try {
            id = Long.parseLong(idStr);
        } catch (Exception e) {
            return;
        }
        PatientReport pr = getFacade().find(id);
        if (pr == null) {
            return;
        }
        currentPatientReport = pr;
    }

    public void preparePatientReportByIdForRequestsWithoutExpiary() {
        currentPatientReport = null;
        if (encryptedPatientReportId == null) {
            return;
        }

        String decodedIdStr;
        try {
            decodedIdStr = URLDecoder.decode(encryptedPatientReportId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Handle the exception, possibly with logging
            return;
        }

        String idStr = getSecurityController().decrypt(decodedIdStr);
        if (idStr == null || idStr.trim().isEmpty()) {
            // Handle the situation where decryption returns null or an empty string
            return;
        }

        Long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            // Handle the exception, possibly with logging
            return;
        }

        PatientReport pr = getFacade().find(id);
        if (pr != null) {
            currentPatientReport = pr;
        }
    }

    public void preparePatientReportForReportLink() {
        currentPatientReport = null;
        if (encryptedPatientReportId == null) {
            return;
        }

        String decodedIdStr;
        try {
            decodedIdStr = URLDecoder.decode(encryptedPatientReportId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Handle the exception, possibly with logging
            return;
        }
        String securityKey = sessionController.getApplicationPreference().getEncrptionKey();
        if (securityKey == null || securityKey.trim().equals("")) {
            sessionController.getApplicationPreference().setEncrptionKey(securityController.generateRandomKey(10));
            sessionController.savePreferences(sessionController.getApplicationPreference());
        }

        String idStr = getSecurityController().decryptAlphanumeric(encryptedPatientReportId, securityKey);

        if (idStr == null || idStr.trim().isEmpty()) {
            // Handle the situation where decryption returns null or an empty string
            return;
        }

        Long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            // Handle the exception, possibly with logging
            return;
        }

        PatientReport pr = getFacade().find(id);
        if (pr != null) {
            currentPatientReport = pr;
        }
        setShowBackground(true);
    }

    public List<PatientReport> patientReports(PatientInvestigation pi) {
        String j = "select r from PatientReport r "
                + " where r.patientInvestigation=:pi";
        Map m = new HashMap();
        m.put("pi", pi);
        return getFacade().findByJpql(j, m);
    }

    public List<PatientReport> approvedPatientReports(PatientInvestigation pi) {
        String j = "select r from PatientReport r "
                + " where r.patientInvestigation=:pi"
                + " and r.returned =:ret";
        Map m = new HashMap();
        m.put("pi", pi);
        m.put("ret", false);
        return getFacade().findByJpql(j, m);
    }

    public int approvedPatientReportCount(PatientInvestigation pi) {
        int reportCount = 0;
        reportCount = approvedPatientReports(pi).size();
        return reportCount;
    }

    public String toViewMyReports() {
        String j = "select r from PatientReport r "
                + " where r.patientInvestigation.billItem.bill.patient.person=:person";
        Map m = new HashMap();

        if (false) {

            PatientReport pr = new PatientReport();
            pr.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson();
            pr.getPatientInvestigation().getBillItem().getBill().getBillDate();
            //Logged Person
            getSessionController().getLoggedUser().getWebUserPerson();

        }

        m.put("person", getSessionController().getLoggedUser().getWebUserPerson());
        customerReports = getFacade().findByJpql(j, m);
        return "/mobile/my_test_results";

    }

    public void sendEmail() {

//        if (getCurrentPatientReport().getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getEmail() == null) {
//            JsfUtil.addErrorMessage("Email not given");
//            return;
//        }
//        if (!CommonController.isValidEmail(getCurrentPatientReport().getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getEmail())) {
//            JsfUtil.addErrorMessage("Given email is NOT valid.");
//            return;
//        }
//
//        boolean sent = getEmailManagerEjb().sendEmail(
//                getCurrentPatientReport().getApproveInstitution().getEmailSendingUsername(),
//                getCurrentPatientReport().getApproveInstitution().getEmailSendingPassword(),
//                getCurrentPatientReport().getApproveInstitution().getEmail(),
//                getCurrentPatientReport().getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getEmail(),
//                getCurrentPatientReport().getPatientInvestigation().getInvestigation().getName() + " Results",
//                emailMessageBody(currentPatientReport),
//                createPDFAndSaveAsaFile());
//
//        if (sent) {
//            JsfUtil.addSuccessMessage("Email Sent");
//        } else {
//            JsfUtil.addErrorMessage("Email Sending Failed");
//        }
    }
//
//    public void sendEmailOld() {
//
//        final String username = getCurrentPatientReport().getPatientInvestigation().getBillItem().getItem().getDepartment().getInstitution().getEmailSendingUsername();
//        final String password = getCurrentPatientReport().getPatientInvestigation().getBillItem().getItem().getDepartment().getInstitution().getEmailSendingPassword();
//
//        Properties props = new Properties();
//        props.put("mail.smtp.starttls.enable", "true");
//        props.put("mail.smtp.auth", "true");
//        props.put("mail.smtp.host", "smtp.gmail.com");
//        props.put("mail.smtp.port", "587");
////        Authenticator auth = new SMTPAuthenticator();
//        Session session = Session.getInstance(props,
//                new javax.mail.Authenticator() {
//            protected PasswordAuthentication getPasswordAuthentication() {
//                return new PasswordAuthentication(username, password);
//            }
//        });
//
//        try {
//            Message message = new MimeMessage(session);
//            message.setFrom(new InternetAddress(getCurrentPatientReport().getPatientInvestigation().getBillItem().getBill().getToDepartment().getInstitution().getEmailSendingUsername()));
//            message.setRecipients(Message.RecipientType.TO,
//                    InternetAddress.parse(getCurrentPatientReport().getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getEmail()));
//            message.setSubject(getCurrentPatientReport().getPatientInvestigation().getInvestigation().getName() + " Results");
////            message.setText("Dear " + getCurrentPatientReport().getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getNameWithTitle()
////                    + ",\n\n Please find the results of your " + getCurrentPatientReport().getPatientInvestigation().getInvestigation().getName() + ".");
//
//            message.setContent(emailMessageBody(currentPatientReport), "text/html; charset=utf-8");
//            //3) create MimeBodyPart object and set your message text
//            BodyPart msbp1 = new MimeBodyPart();
//            msbp1.setText("Final Lab report of patient");
//
//            //4) create new MimeBodyPart object and set DataHandler object to this object
//            MimeBodyPart msbp2 = new MimeBodyPart();
////            createHtmlFile();
//            DataSource source = new FileDataSource(createPDFAndSaveAsaFile());
//            msbp2.setDataHandler(new DataHandler(source));
//            msbp2.setFileName("/tmp/report" + getCurrentPatientReport().getId() + ".pdf");
//
//            //5) create Multipart object and add Mimdler(soeBodyPart objects to this object
//            Multipart multipart = new MimeMultipart();
//            multipart.addBodyPart(msbp1);
//            multipart.addBodyPart(msbp2);
//
//            //6) set the multiplart object to the message object
//            message.setContent(multipart);
//
//            Transport.send(message);
//
//            JsfUtil.addSuccessMessage("Email Sent SUccessfully");
//
//        } catch (MessagingException e) {
//            JsfUtil.addErrorMessage("Error. " + e.getMessage());
//        } catch (Exception e) {
//            JsfUtil.addErrorMessage("Error. " + e.getMessage());
//        }
//
//    }

    public void addTemplateToReport() {
        if (investigationItem == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (selectables == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPatientReport == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPatientReport.getTemplateItem() == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        String finalText = investigationItem.getHtmltext();
        for (Selectable s : selectables) {

            String patternStart = "#{";
            String patternEnd = "}";
            String toBeReplaced;

            toBeReplaced = patternStart + s.getFullText() + patternEnd;

            finalText = finalText.replace(toBeReplaced, s.getSelectedValue());
        }

        currentPatientReport.getTemplateItem().setLobValue(finalText);
    }

    public void toAddNewTemplate() {
        if (investigationItem == null) {
            JsfUtil.addErrorMessage("Select a template first");
            return;
        }

        selectables = new ArrayList<>();

        String patternStart = "#{";
        String patternEnd = "}";
        String regexString = Pattern.quote(patternStart) + "(.*?)" + Pattern.quote(patternEnd);

        String text = investigationItem.getHtmltext();

        Pattern p = Pattern.compile(regexString);
        Matcher m = p.matcher(text);
        List<String> strBlocks = new ArrayList<>();

        while (m.find()) {
            String block = m.group(1);
            if (!block.trim().equals("")) {
                Selectable s = new Selectable();

                s.setFullText(block);

                if (block.contains("|")) {
                    String[] blockParts = block.split("\\|");
                    boolean hasOptions = false;
                    for (int i = 0; i < blockParts.length; i++) {
                        if (i == 0) {
                            s.setName(blockParts[i]);
                        } else {
                            s.getOptions().add(blockParts[i]);
                            hasOptions = true;
                        }
                    }

                    s.setInputText(false);
                    s.setSelectOneMenu(true);

                } else {
                    s.setName(block);
                    s.setInputText(true);
                    s.setSelectOneMenu(false);
                }
                selectables.add(s);
            }
        }

    }

    public List<PatientInvestigation> getCustomerPis() {
        return customerPis;
    }

    public void setCustomerPis(List<PatientInvestigation> customerPis) {
        this.customerPis = customerPis;
    }

    public String fillPatientReports() {
        String sql;
        Map m = new HashMap();
        m.put("phone", getSessionController().getPhoneNo());
        m.put("billno", getSessionController().getBillNo().toUpperCase());
        sql = "select pr from PatientInvestigation pr where pr.retired=false and "
                + "(pr.billItem.bill.patient.person.phone)=:phone and "
                + " ((pr.billItem.bill.insId)=:billno or (pr.billItem.bill.deptId)=:billno)  "
                + "order by pr.id desc ";
        customerPis = getPiFacade().findByJpql(sql, m, 50);
        return "/reports_list";
    }

    public String patientReportSearch() {
        if (currentPatientReport == null || currentPatientReport.getPatientInvestigation() == null || currentPatientReport.getPatientInvestigation().getPatient() == null) {
            return "";
        }
        getTransferController().setPatient(currentPatientReport.getPatientInvestigation().getPatient());
        return "/lab/search_for_reporting_patient";
    }

    public String lastPatientReport(PatientInvestigation pi) {
        ////System.out.println("last pt rpt");
        if (pi == null) {
            currentPatientReport = null;
            ////System.out.println("pi is null");
            return "";
        }
        Investigation ix;
        ix = (Investigation) pi.getInvestigation().getReportedAs();
        ////System.out.println("ix = " + ix);
        currentReportInvestigation = ix;
        currentPtIx = pi;
        String sql;
        Map m = new HashMap();
        sql = "select r from PatientReport r where r.patientInvestigation=:pi and r.retired=false order by r.id desc";
        //////System.out.println("sql = " + sql);
        m.put("pi", pi);
        //////System.out.println("m = " + m);
        PatientReport r = getFacade().findFirstByJpql(sql, m);
        //////System.out.println("r = " + r);
        if (r == null) {
            ////System.out.println("r is null");
//            if (ix.getReportType()==InvestigationReportType.Microbiology ) {
            if (ix.getReportType() == InvestigationReportType.Microbiology) {
                r = createNewMicrobiologyReport(pi, ix);
            } else {
                r = createNewPatientReport(pi, ix);
            }
            ////System.out.println("r = " + r);
            getCommonReportItemController().setCategory(ix.getReportFormat());
        } else {
            ////System.out.println("r ok");
            ////System.out.println("r = " + r);
            getCommonReportItemController().setCategory(currentReportInvestigation.getReportFormat());
        }
        currentPatientReport = r;
        if (currentPatientReport != null && currentPatientReport.getApproveUser() != null) {
            getStaffController().setCurrent(currentPatientReport.getApproveUser().getStaff());
        } else {
            getStaffController().setCurrent(null);
        }
        return "/lab/patient_report";
    }

    public List<PatientReportItemValue> getPatientReportItemValues() {
        String sql = "Select v from PatientReportItemValue v where v.patientReport=:r "
                + " order by v.investigationItem.cssTop";
        Map m = new HashMap();
        m.put("r", getCurrentPatientReport());
        patientReportItemValues = getPirivFacade().findByJpql(sql, m);
        // getPirivFacade
        return patientReportItemValues;
    }

    public StaffController getStaffController() {
        return staffController;
    }

    public void setStaffController(StaffController staffController) {
        this.staffController = staffController;
    }

    private double findPtReportItemVal(InvestigationItem ii) {
        if (currentPatientReport == null) {
            JsfUtil.addErrorMessage("No Report to calculate");
            return 0;
        }
        if (currentPatientReport.getPatientReportItemValues() == null) {
            JsfUtil.addErrorMessage("Report Items values is null");
            return 0;
        }
        if (currentPatientReport.getPatientReportItemValues().isEmpty()) {
            JsfUtil.addErrorMessage("Report Items values is empty");
            return 0;
        }
        ////System.out.println("currentPatientReport = " + currentPatientReport);
        ////System.out.println("currentPatientReport.getPatientReportItemValues() = " + currentPatientReport.getPatientReportItemValues());

        for (PatientReportItemValue priv : currentPatientReport.getPatientReportItemValues()) {
            if (priv != null) {
                ////System.out.println("priv = " + priv);
                ////System.out.println("priv in finding val is " + priv.getInvestigationItem().getName());
                ////System.out.println("compairing are " + priv.getInvestigationItem().getId() + "  vs " + ii.getId());
                if (Objects.equals(priv.getInvestigationItem().getId(), ii.getId())) {
                    ////System.out.println("double val is " + priv.getDoubleValue());
                    if (priv.getDoubleValue() == null) {
                        return 0.0;
                    }
                    return priv.getDoubleValue();
                }
            }
        }
        return 0.0;
    }

    private double findPtReportItemVal(String nameOfIxItem) {
        if (currentPatientReport == null) {
            JsfUtil.addErrorMessage("No Report to calculate");
            return 0;
        }
        if (currentPatientReport.getPatientReportItemValues() == null) {
            JsfUtil.addErrorMessage("Report Items values is null");
            return 0;
        }
        if (currentPatientReport.getPatientReportItemValues().isEmpty()) {
            JsfUtil.addErrorMessage("Report Items values is empty");
            return 0;
        }
        System.out.println("currentPatientReport = " + currentPatientReport);
        System.out.println("currentPatientReport.getPatientReportItemValues() = " + currentPatientReport.getPatientReportItemValues());

        for (PatientReportItemValue priv : currentPatientReport.getPatientReportItemValues()) {
            if (priv != null) {
                System.out.println("priv = " + priv);
                System.out.println("priv in finding val is " + priv.getInvestigationItem().getName());
                System.out.println("compairing are " + priv.getInvestigationItem().getId() + "  vs " + nameOfIxItem);
                if (Objects.equals(priv.getInvestigationItem().getName(), nameOfIxItem)) {
                    System.out.println("double val is " + priv.getDoubleValue());
                    if (priv.getDoubleValue() == null) {
                        return 0.0;
                    }
                    return priv.getDoubleValue();
                }
            }
        }
        return 0.0;
    }

    public void calculate() {
        System.out.println("calculate");
        Date startTime = new Date();
        if (currentPatientReport == null) {
            JsfUtil.addErrorMessage("No Report to calculate");
            return;
        }
        if (currentPatientReport.getPatientReportItemValues() == null) {
            JsfUtil.addErrorMessage("Report Items values is null");
            return;
        }
        if (currentPatientReport.getPatientReportItemValues().isEmpty()) {
            JsfUtil.addErrorMessage("Report Items values is empty");
            return;
        }
        String calString = "";
        for (PatientReportItemValue priv : currentPatientReport.getPatientReportItemValues()) {

            if (priv.getInvestigationItem().getFormatString() != null && !priv.getInvestigationItem().getFormatString().trim().isEmpty()) {
                if (priv.getInvestigationItem().getIxItemValueType() == InvestigationItemValueType.Varchar) {
                    double tmpDbl = CommonFunctions.extractDoubleValue(priv.getStrValue());
                    priv.setStrValue(CommonFunctions.formatNumber(tmpDbl, priv.getInvestigationItem().getFormatString()));
                    priv.setDoubleValue(tmpDbl);
                } else if (priv.getInvestigationItem().getIxItemValueType() == InvestigationItemValueType.Double) {
                    Double numberWithLargeNumberOfDecimals = priv.getDoubleValue();
                    Double numberWithFormatter = CommonFunctions.formatDouble(numberWithLargeNumberOfDecimals, priv.getInvestigationItem().getFormatString());
                    priv.setDoubleValue(numberWithFormatter);
                    priv.setStrValue(numberWithFormatter + "");
                }
            }

            if (priv.getInvestigationItem().getIxItemType() == InvestigationItemType.Calculation) {
                String sql = "select i "
                        + " from IxCal i "
                        + " where (i.retired=false or i.retired is null) "
                        + " and i.calIxItem = :iii "
                        + " order by i.id";
                Map m = new HashMap();
                m.put("iii", priv.getInvestigationItem());
                List<IxCal> ixCals = getIxCalFacade().findByJpql(sql, m);
                double result = 0;
                String resultStr = "";
                calString = "";

                String baseJs = null;
                for (IxCal c : ixCals) {
                    if (c.getCalculationType() == CalculationType.JavaScript) {
                        baseJs = c.getJavascript();
                        break;
                    }
                    if (c.getCalculationType() == CalculationType.Constant) {
                        calString = calString + c.getConstantValue();
                    }
                    if (c.getCalculationType() == CalculationType.GenderDependentConstant) {
                        if (currentPatientReport.getPatientInvestigation().getPatient().getPerson().getSex() == Sex.Male) {
                            calString = calString + c.getMaleConstantValue();
                        } else {
                            calString = calString + c.getFemaleConstantValue();
                        }
                    }
                    if (c.getCalculationType() == CalculationType.Value) {
                        calString = calString + " " + findPtReportItemVal(c.getValIxItem()) + " ";
                    }

                    if (c.getCalculationType() == CalculationType.Addition) {
                        calString = calString + " + ";
                    }

                    if (c.getCalculationType() == CalculationType.Substraction) {
                        calString = calString + " - ";
                    }

                    if (c.getCalculationType() == CalculationType.Multiplication) {
                        calString = calString + " * ";
                    }

                    if (c.getCalculationType() == CalculationType.Devision) {
                        calString = calString + " / ";
                    }

                    if (c.getCalculationType() == CalculationType.OpeningBracket) {
                        calString = calString + " ( ";
                    }

                    if (c.getCalculationType() == CalculationType.ClosingBracket) {
                        calString = calString + " ) ";
                    }

                    if (c.getCalculationType() == CalculationType.Power) {
                        calString = calString + "Math.pow";
                    }

                    if (c.getCalculationType() == CalculationType.Comma) {
                        calString = calString + ",";
                    }

                    if (c.getCalculationType() == CalculationType.AgeInDays) {
                        calString = calString + currentPatientReport.getPatientInvestigation().getPatient().getAgeDays();
                    }
                    if (c.getCalculationType() == CalculationType.AgeInMonths) {
                        calString = calString + currentPatientReport.getPatientInvestigation().getPatient().getAgeMonths();
                    }
                    if (c.getCalculationType() == CalculationType.AgeInYears) {
                        calString = calString + currentPatientReport.getPatientInvestigation().getPatient().getAgeYears();
                    }
                }
                System.out.println("calString = " + calString);
                System.out.println("baseJs = " + baseJs);
                if (baseJs != null) {
                    calString = generateModifiedJavascriptFromBaseJavaScript(currentPatientReport, baseJs);
                    System.out.println("calString = " + calString);
                }

                ScriptEngineManager mgr = new ScriptEngineManager();
                ScriptEngine engine = mgr.getEngineByName("JavaScript");
                Object resultObj;
                try {
                    resultObj = engine.eval(calString);
                    if (resultObj == null) {
                        System.out.println("resultObj Null = ");
                        result = 0.0;
                        resultStr = "";
                    } else if (resultObj instanceof String) {
                        resultStr = (String) resultObj;
                        System.out.println("resultStr = " + resultObj);
                    } else if (resultObj instanceof Double) {
                        result = (double) resultObj;
                        resultStr = result + "";
                        System.out.println("result = " + result);
                    } else {
                        System.out.println("Else = ");
                        result = 0.0;
                        resultStr = "";
                    }
                    System.out.println("resultStr = " + resultStr);
                    System.out.println("result = " + result);

                } catch (Exception ex) {
                    Logger.getLogger(PatientReportController.class
                            .getName()).log(Level.SEVERE, null, ex);
                    result = 0.0;
                }
                priv.setDoubleValue(result);
                priv.setStrValue(resultStr);

            } else if (priv.getInvestigationItem().getIxItemType() == InvestigationItemType.Flag) {
                priv.setStrValue(findFlagValue(priv));
            }
            try {
                getPirivFacade().edit(priv);
            } catch (Exception e) {
                System.err.println("e = " + e.getMessage());
            }
        }
        if (configOptionApplicationController.getBooleanValueByKey("Lab Test History Enabled", false)) {
            labTestHistoryController.addCalculateHistory(currentPtIx, currentPatientReport);
        }

    }

    private String generateModifiedJavascriptFromBaseJavaScript(PatientReport pr, String baseJs) {
        String modifiedJs = "";
        if (pr == null) {
            System.out.println("Debug: PatientReport is null");
            return modifiedJs;
        }
        if (baseJs == null || baseJs.isEmpty()) {
            System.out.println("Debug: Base JavaScript is null or empty");
            return modifiedJs;
        } else {
            modifiedJs = baseJs;
        }

        if (pr.getPatientInvestigation() == null) {
            System.out.println("Debug: PatientInvestigation is null");
            return modifiedJs;
        }
        if (pr.getPatientInvestigation().getBillItem() == null) {
            System.out.println("Debug: BillItem is null");
            return modifiedJs;
        }
        if (pr.getPatientInvestigation().getBillItem().getBill() == null) {
            System.out.println("Debug: Bill is null");
            return modifiedJs;
        }
        if (pr.getPatientInvestigation().getBillItem().getBill().getPatient() == null) {
            System.out.println("Debug: Patient is null");
            return modifiedJs;
        }

        String sex = pr.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getSex() == Sex.Male ? "male" : "female";
        int ageInYears = pr.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getAgeYearsComponent();

        System.out.println("Debug: Sex is " + sex + ", Age is " + ageInYears);

        Pattern pattern = Pattern.compile("\\{\\{[^}]+\\}\\}");
        Matcher matcher = pattern.matcher(modifiedJs);

        while (matcher.find()) {
            String placeholder = matcher.group();
            String placeholderWithoutBraces = placeholder.substring(2, placeholder.length() - 2); // Remove double braces for matching

            System.out.println("Debug: Found placeholder " + placeholder);

            if ("sex".equalsIgnoreCase(placeholderWithoutBraces)) {
                modifiedJs = modifiedJs.replace(placeholder, sex);
                System.out.println("Debug: Replaced " + placeholder + " with " + sex);
            } else if ("age".equalsIgnoreCase(placeholderWithoutBraces)) {
                modifiedJs = modifiedJs.replace(placeholder, String.valueOf(ageInYears));
                System.out.println("Debug: Replaced " + placeholder + " with " + ageInYears);
            } else {
                double reportItemValue = findPtReportItemVal(placeholderWithoutBraces);
                modifiedJs = modifiedJs.replace(placeholder, String.valueOf(reportItemValue));
                System.out.println("Debug: Replaced " + placeholder + " with " + reportItemValue);
            }
        }

        return modifiedJs;
    }

    private PatientReportItemValue findItemValue(PatientReport pr, InvestigationItem ii) {
//        //////System.out.println("pr is " + pr + " and details");
//        //////System.out.println("ii is " + ii);
        PatientReportItemValue iv = null;

        if (pr != null && ii != null) {
//
//            //////System.out.println("pr ix is " + pr.getItem().getName());
//            //////System.out.println("pr pt is " + pr.getPatientInvestigation().getPatient().getPerson().getName());
//
//            //////System.out.println("ii name is  " + ii.getName());
//
//            //////System.out.println("pr.getPatientReportItemValues() is " + pr.getPatientReportItemValues());
            for (PatientReportItemValue v : pr.getPatientReportItemValues()) {
//                //////System.out.println("v is " + v);
//                //////System.out.println("v str value is " + v.getStrValue());
//                //////System.out.println("v dbl value is " + v.getDoubleValue());
//                //////System.out.println("v iis is " + v.getInvestigationItem());
//                //////System.out.println("v iis name is " + v.getInvestigationItem().getName());

                if (v.getInvestigationItem().equals(ii)) {
//                    //////System.out.println("v equals ii");
                    iv = v;
                } else {
//                    //////System.out.println("v is not compatible");
                }
            }
        }
//        //////System.out.println("iv returning is " + iv);
        return iv;
    }

    private String findFlagValue(PatientReportItemValue v) {
        Map m = new HashMap();
        String sql;
        m.put("s", v.getPatient().getPerson().getSex());
        m.put("f", v.getInvestigationItem());
//        m.put("a", v.getPatient().getAgeInDays());
        sql = "Select f from TestFlag f where f.retired=false and f.investigationItemOfFlagType=:f and f.sex=:s order by f.orderNo";
        List<TestFlag> fs = getTestFlagFacade().findByJpql(sql, m);
        for (TestFlag f : fs) {

            Long a = v.getPatient().getAgeInDays();
            //System.err.println("Age is a" + a);
            //System.err.println("From Age is " + f.getFromAge());
            //System.err.println("To Age is " + f.getToAge());

            //////System.out.println("flah low message " + f.getLowMessage());
            if (f.getFromAge() <= a && f.getToAge() >= a) {
                //////System.out.println("searching val");
                PatientReportItemValue val = findItemValue(currentPatientReport, f.getInvestigationItemOfValueType());
                //////System.out.println("val is " + val);
                if (val == null) {
                    //////System.out.println("val is null");
                    continue;
                }
                Double d = val.getDoubleValue();
                if (d == null || d == 0) {
                    try {
                        if (NumberUtils.isNumber(val.getStrValue())) {
                            d = Double.parseDouble(val.getStrValue());
                        } else {
                            d = 0.0;
                        }

                    } catch (NumberFormatException e) {
                        d = 0.0;
                    }
                }

                //////System.out.println("f is " + f);
                //////System.out.println("d is " + d);
                //////System.out.println("f is not null");
                //////System.out.println("fromVal is " + f.getFromVal());
                //////System.out.println("toVal is " + f.getToVal());
                if (f.getFromVal() > d) {
                    //////System.out.println("dddddddddddddd 1");
                    return f.getLowMessage();
                } else if (f.getToVal() < d) {
                    //////System.out.println("dddddddddddddd 2");
                    return f.getHighMessage();
                } else {
                    //////System.out.println("dddddddddddddd 3");
                    return f.getFlagMessage();
                }
            }
        }
        return "";
    }

    public IxCalFacade getIxCalFacade() {
        return ixCalFacade;
    }

    public void setIxCalFacade(IxCalFacade ixCalFacade) {
        this.ixCalFacade = ixCalFacade;
    }

    public Investigation getAlternativeInvestigation() {
        return alternativeInvestigation;
    }

    public void setAlternativeInvestigation(Investigation alternativeInvestigation) {
        PatientInvestigation pi = new PatientInvestigation();
        this.alternativeInvestigation = alternativeInvestigation;
    }

    public void onCellEdit(CellEditEvent event) {
        try {
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }

    }

    public ItemForItemController getItemForItemController() {
        return itemForItemController;
    }

    public void setItemForItemController(ItemForItemController itemForItemController) {
        this.itemForItemController = itemForItemController;
    }

    public Investigation getCurrentReportInvestigation() {
        return currentReportInvestigation;
    }

    public void setCurrentReportInvestigation(Investigation currentReportInvestigation) {
        //////System.out.println("setting currentReportInvestigation - " + currentReportInvestigation.getName());
        this.currentReportInvestigation = currentReportInvestigation;
    }

    public PatientReportItemValueFacade getPirivFacade() {
        return pirivFacade;
    }

    public void setPirivFacade(PatientReportItemValueFacade pirivFacade) {
        this.pirivFacade = pirivFacade;
    }

    public PatientInvestigationItemValueFacade getPiivFacade() {
        return piivFacade;
    }

    public void setPiivFacade(PatientInvestigationItemValueFacade piivFacade) {
        this.piivFacade = piivFacade;
    }

    public PatientInvestigationFacade getPiFacade() {
        return piFacade;
    }

    public void setPiFacade(PatientInvestigationFacade piFacade) {
        this.piFacade = piFacade;
    }

    public void savePatientReportItemValues() {
        if (currentPatientReport != null && currentPatientReport.getPatientReportItemValues() != null) {
            for (PatientReportItemValue v : currentPatientReport.getPatientReportItemValues()) {
                pirivFacade.edit(v);
            }
        }
    }

    public void savePatientReport() {
        if (currentPatientReport == null || currentPtIx == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }

        getCurrentPtIx().setDataEntered(true);
        currentPtIx.setDataEntryAt(Calendar.getInstance().getTime());
        currentPtIx.setDataEntryUser(getSessionController().getLoggedUser());
        currentPtIx.setDataEntryDepartment(getSessionController().getDepartment());

        currentPatientReport.setDataEntered(Boolean.TRUE);

        currentPatientReport.setDataEntryAt(Calendar.getInstance().getTime());
        currentPatientReport.setDataEntryDepartment(getSessionController().getLoggedUser().getDepartment());
        currentPatientReport.setDataEntryInstitution(getSessionController().getLoggedUser().getInstitution());
        currentPatientReport.setDataEntryUser(getSessionController().getLoggedUser());

        currentPatientReport.setPrinted(false);
        currentPatientReport.setPrintingUser(null);

        updateTemplate();

        getFacade().edit(currentPatientReport);
        getPiFacade().edit(currentPtIx);

        getFacade().edit(currentPatientReport);
        getPiFacade().edit(currentPtIx);

        if (configOptionApplicationController.getBooleanValueByKey("Lab Test History Enabled", false)) {
            labTestHistoryController.addDataEnterHistory(currentPtIx, currentPatientReport);
        }

        JsfUtil.addSuccessMessage("Saved");
    }

    public void removePatientReport() {

        if (currentPatientReport == null) {
            JsfUtil.addErrorMessage("No Patient Report");
            return;
        }
        if (comment == null || comment.trim() == null) {
            JsfUtil.addErrorMessage("Add Comment");
            return;
        }

        currentPatientReport.setRetireComments(comment);
        currentPatientReport.setRetired(Boolean.TRUE);
        currentPatientReport.setRetiredAt(Calendar.getInstance().getTime());
        currentPatientReport.setRetirer(getSessionController().getLoggedUser());

        if (currentPatientReport.getReportType() == ReportType.UPLOAD) {
            Upload currentReportUpload = patientReportUploadController.loadUploads(currentPatientReport);

            if (currentReportUpload != null) {
                currentReportUpload.setRetireComments(comment);
                currentReportUpload.setRetired(true);
                currentReportUpload.setRetiredAt(new Date());
                currentReportUpload.setRetirer(sessionController.getLoggedUser());
                uploadFacade.create(currentReportUpload);
            }
            System.out.println("Upload Report Removed");
        }

        getFacade().edit(currentPatientReport);
        JsfUtil.addSuccessMessage("Successfully Removed");
        patientInvestigationController.searchPatientReports();
    }

    public void updateTemplate() {
        Investigation currentInvestigation = (Investigation) currentPatientReport.getItem();
        if (currentInvestigation == null || currentPatientReport == null) {
            System.out.println("currentInvestigation or currentPatientReport is null.");
            return; // Handle the case where the investigation or report is null
        }

        // Identify the InvestigationItem with placeholders
        InvestigationItem templateItem = null;
        if (currentInvestigation.getReportType() == InvestigationReportType.HtmlTemplate) {
            for (InvestigationItem ixi : currentInvestigation.getReportItems()) {
                if (ixi != null && ixi.getIxItemType() == InvestigationItemType.Html) {
                    templateItem = ixi;
                    System.out.println("Selected InvestigationItem with Html content: " + ixi.getHtmltext());
                    break; // Assume there is only one template; exit loop after finding it
                }
            }
        }

        if (templateItem == null) {
            System.out.println("No HtmlTemplate found in the current investigation.");
            return;
        }

        // Extract placeholders from the identified template
        List<String> placeholders = CommonFunctions.extractPlaceholders(templateItem.getHtmltext());
        System.out.println("Placeholders found: " + placeholders);

        // Store replacements in a map
        Map<String, String> replacementMap = new HashMap<>();

        // Iterate through patient values and match with placeholders
        for (PatientReportItemValue priv : currentPatientReport.getPatientReportItemValues()) {
            if (priv == null || priv.getInvestigationItem() == null) {
                System.out.println("Skipping null PatientReportItemValue or InvestigationItem.");
                continue; // Skip null values to avoid null pointer exceptions
            }

            String itemName = priv.getInvestigationItem().getName();
            if (placeholders.contains(itemName)) {
                String valueToReplacePlaceholder = null;

                switch (priv.getInvestigationItem().getIxItemValueType()) {
                    case Varchar:
                        valueToReplacePlaceholder = priv.getStrValue();
                        break;
                    case Double:
                        Double dbl = priv.getDoubleValue();
                        if (dbl != null) {
                            String formatString = priv.getInvestigationItem().getFormatString();
                            if (formatString == null || formatString.isEmpty()) {
                                formatString = "%.2f"; // Default format if none provided
                            }
                            String suffix = priv.getInvestigationItem().getFormatSuffix() != null ? priv.getInvestigationItem().getFormatSuffix() : "";
                            String prefix = priv.getInvestigationItem().getFormatPrefix() != null ? priv.getInvestigationItem().getFormatPrefix() : "";
                            valueToReplacePlaceholder = prefix + String.format(formatString, dbl) + suffix;
                            System.out.println("dbl = " + dbl + ", formatted value = " + valueToReplacePlaceholder);
                        } else {
                            System.out.println("Double value is null for: " + itemName);
                        }
                        break;
                    case Memo:
                        valueToReplacePlaceholder = priv.getLobValue();
                        break;
                    default:
                        valueToReplacePlaceholder = ""; // Handle other types if necessary
                        System.out.println("No matching value for: " + itemName);
                        break;
                }

                if (valueToReplacePlaceholder != null && !valueToReplacePlaceholder.isEmpty()) {
                    replacementMap.put(itemName, valueToReplacePlaceholder);
                    System.out.println("Mapping placeholder: " + itemName + " to value: " + valueToReplacePlaceholder);
                } else {
                    System.out.println("Value for placeholder " + itemName + " is null or empty.");
                }
            }
        }

        // Replace placeholders in the template with values from the map, or make them blank if no value is found
        String updatedHtmlText = templateItem.getHtmltext();
        for (String placeholder : placeholders) {
            String replacementValue = replacementMap.get(placeholder);
            if (replacementValue != null && !replacementValue.isEmpty()) {
                updatedHtmlText = updatedHtmlText.replace("{" + placeholder + "}", replacementValue);
                System.out.println("Replaced {" + placeholder + "} with " + replacementValue);
            } else {
                // Replace with an empty string if no replacement value is found
                updatedHtmlText = updatedHtmlText.replace("{" + placeholder + "}", "");
                System.out.println("Replaced {" + placeholder + "} with an empty string.");
            }
        }

// Save the updated HTML text back to the patient-specific report item
        for (PatientReportItemValue privHtml : currentPatientReport.getPatientReportItemValues()) {
            System.out.println("privHtml = " + privHtml);

            if (privHtml == null) {
                System.out.println("Skipping null privHtml.");
                continue; // Skip null PatientReportItemValue
            }

            if (privHtml.getInvestigationItem() == null) {
                System.out.println("Skipping privHtml with null InvestigationItem.");
                continue; // Skip if InvestigationItem is null
            }

            System.out.println("privHtml.getInvestigationItem() = " + privHtml.getInvestigationItem());
            System.out.println("templateItem = " + templateItem);

            if (privHtml.getInvestigationItem().equals(templateItem)) {
                System.out.println("InvestigationItem matches the templateItem.");

                // Set the updated HTML content
                privHtml.setLobValue(updatedHtmlText);

                System.out.println("Set updated HTML content for PatientReportItemValue: "
                        + "ID: " + privHtml.getId() + ", Name: "
                        + privHtml.getInvestigationItem().getName());
            } else {
                System.out.println("InvestigationItem does not match the templateItem.");
            }
        }

    }

    public String emailMessageBody(PatientReport r) {
        String b = "<!DOCTYPE html>"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">"
                + "<head>"
                + "<title>"
                + r.getPatientInvestigation().getInvestigation().getName()
                + "</title>"
                + "</head>"
                + "<h:body>";
        b += "<p>Dear " + r.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getNameWithTitle() + ",<br/>";
        b += "Your " + r.getPatientInvestigation().getInvestigation().getName() + " is ready.<br/></p>";
        b += "<table>"
                + "<tr>"
                + "<th>"
                + "Test"
                + "</th>"
                + "<th>"
                + "Result"
                + "</th>"
                + "<th>"
                + "Unit"
                + "</th>"
                + "<th>"
                + "Reference"
                + "</th>"
                + "</tr>"
                + "";
        for (PatientReportItemValue v : r.getPatientReportItemValues()) {
            if (v.getInvestigationItem().getIxItemType() == InvestigationItemType.Value) {
                b += "<tr><td>";
                if (v.getInvestigationItem().getTestLabel() != null) {
                    b += v.getInvestigationItem().getTestLabel().getName();
                } else {
                    b += v.getInvestigationItem().getName();
                }
                b += "</td><td>";
                switch (v.getInvestigationItem().getIxItemValueType()) {
                    case Varchar:
                        b += v.getStrValue() + "\t";
                        break;
                    case Memo:
                        b += v.getLobValue() + "\t";
                        break;
                    case Double:
                        b += v.getDoubleValue() + "\t";
                        break;
                }
                b += "</td><td>";
                if (v.getInvestigationItem().getUnitLabel() != null) {
                    b += v.getInvestigationItem().getUnitLabel().getName();
                }
                b += "</td><td>";
                if (v.getInvestigationItem().getReferenceLabel() != null) {
                    b += v.getInvestigationItem().getReferenceLabel().getName();
                }
                b += "</td></tr>";
            }
        }
        b += "</table>";
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, 1);

        String temId = currentPatientReport.getId() + "";
        temId = getSecurityController().encrypt(temId);
        try {
            temId = URLEncoder.encode(temId, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
        }

        String ed = CommonFunctions.getDateFormat(c.getTime(), "ddMMMMyyyyhhmmss");
        ed = getSecurityController().encrypt(ed);
        try {
            ed = URLEncoder.encode(ed, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
        }

        String url = CommonFunctions.getBaseUrl() + "faces/requests/report.xhtml?id=" + temId + "&user=" + ed;
        b += "<p>"
                + "Your Report is attached"
                + "<br/>"
                + "Please visit "
                + "<a href=\""
                + url
                + "\">this link</a>"
                + " to view or print the report.The link will expire in one month for privacy and confidentially issues."
                + "<br/>"
                + "</p>";

        b += "</h:body></html>";
        return b;
    }

    public String cancelApprovalEmailMessageBody(PatientReport r) {
        String b = "<!DOCTYPE html>"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">"
                + "<head>"
                + "<title>"
                + r.getPatientInvestigation().getInvestigation().getName()
                + "</title>"
                + "</head>"
                + "<h:body>";
        b += "<p>The Following Report's approval was cancelled. </p><br/>";
        b += "<p>Patient Name : " + r.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getNameWithTitle() + "</p><br/>";
        b += "<p>Investigation Name : " + r.getPatientInvestigation().getInvestigation().getName() + " </p><br/>";
        b += "<p>Bill Number : " + r.getPatientInvestigation().getBillItem().getBill().getInsId() + "</p><br/>";
        b += "<p>Approved User : " + r.getPatientInvestigation().getApproveUser().getWebUserPerson().getNameWithTitle() + " </p><br/>";
        b += "<p>Approved at : " + r.getPatientInvestigation().getApproveAt() + "</p><br/>";
        b += "<p>Approval Reversing User : " + getSessionController().getLoggedUser().getWebUserPerson().getNameWithTitle() + " </p><br/>";
        b += "<p>Approval Reversing Time : " + new Date() + " </p><br/>";
        b += "<br/><br/><p>The Results before Reversing is as below. </p><br/>";
        b += "<table>"
                + "<tr>"
                + "<th>"
                + "Test"
                + "</th>"
                + "<th>"
                + "Result"
                + "</th>"
                + "<th>"
                + "Unit"
                + "</th>"
                + "<th>"
                + "Reference"
                + "</th>"
                + "</tr>"
                + "";
        for (PatientReportItemValue v : r.getPatientReportItemValues()) {
            if (v.getInvestigationItem().getIxItemType() == InvestigationItemType.Value) {
                b += "<tr><td>";
                if (v.getInvestigationItem().getTestLabel() != null) {
                    b += v.getInvestigationItem().getTestLabel().getName();
                } else {
                    b += v.getInvestigationItem().getName();
                }
                b += "</td><td>";
                switch (v.getInvestigationItem().getIxItemValueType()) {
                    case Varchar:
                        b += v.getStrValue() + "\t";
                        break;
                    case Memo:
                        b += v.getLobValue() + "\t";
                        break;
                    case Double:
                        b += v.getDoubleValue() + "\t";
                        break;
                }
                b += "</td><td>";
                if (v.getInvestigationItem().getUnitLabel() != null) {
                    b += v.getInvestigationItem().getUnitLabel().getName();
                }
                b += "</td><td>";
                if (v.getInvestigationItem().getReferenceLabel() != null) {
                    b += v.getInvestigationItem().getReferenceLabel().getName();
                }
                b += "</td></tr>";
            }
        }
        b += "</table>";
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, 1);

        String temId = currentPatientReport.getId() + "";
        temId = getSecurityController().encrypt(temId);
        try {
            temId = URLEncoder.encode(temId, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
        }

        String ed = CommonFunctions.getDateFormat(c.getTime(), "ddMMMMyyyyhhmmss");
        ed = getSecurityController().encrypt(ed);
        try {
            ed = URLEncoder.encode(ed, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
        }

        String url = CommonFunctions.getBaseUrl() + "faces/requests/report.xhtml?id=" + temId + "&user=" + ed;
        b += "<p>"
                + "The report before reversing approval is attached. The current report can be viewed at following link"
                + "<br/>"
                + "Please visit "
                + "<a href=\""
                + url
                + "\">this link</a>"
                + " to view or print the report.The link will expire in one month for privacy and confidentially issues."
                + "<br/>"
                + "</p>";

        b += "</h:body></html>";
        return b;
    }

    public String smsBody(PatientReport r) {
        String securityKey = sessionController.getApplicationPreference().getEncrptionKey();
        if (securityKey == null || securityKey.trim().equals("")) {
            sessionController.getApplicationPreference().setEncrptionKey(securityController.generateRandomKey(10));
            sessionController.savePreferences(sessionController.getApplicationPreference());
        }
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, 1);
        String smsBody = "";
        String temId = getSecurityController().encryptAlphanumeric(r.getId().toString(), securityKey);
        String url = CommonFunctions.getBaseUrl() + "faces/requests/ix.xhtml?id=" + temId;

        String template = configOptionApplicationController.getLongTextValueByKey("Custom SMS Body Massage for Lab Report");
        if (!template.equalsIgnoreCase("")) {
            smsBody = replaceReporSMSBody(template, r, url);
        } else {
            smsBody = "Your " + r.getPatientInvestigation().getInvestigation().getName() + " is ready. " + url;
        }
        return smsBody;
    }

    public String replaceReporSMSBody(String template, PatientReport patientReport, String url) {
        String output;
        String processedTemplate = template.replace("\\n", "\n");
        String location = "";
        if ("CC".equalsIgnoreCase(patientReport.getPatientInvestigation().getBillItem().getBill().getIpOpOrCc())) {
            location = patientReport.getPatientInvestigation().getBillItem().getBill().getCollectingCentre().getName();
        } else {
            location = patientReport.getPatientInvestigation().getBillItem().getBill().getDepartment().getPrintingName();
        }
        output = processedTemplate
                .replace("{patient_name}", patientReport.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getNameWithTitle())
                .replace("{patient_report}", patientReport.getPatientInvestigation().getInvestigation().getName())
                .replace("{report_url}", url)
                .replace("{collecting_location}", location);
        return output;
    }

    public String emailBody(PatientReport r) {
        String securityKey = sessionController.getApplicationPreference().getEncrptionKey();
        if (securityKey == null || securityKey.trim().isEmpty()) {
            sessionController.getApplicationPreference().setEncrptionKey(securityController.generateRandomKey(10));
            sessionController.savePreferences(sessionController.getApplicationPreference());
        }
        String temId = getSecurityController().encryptAlphanumeric(r.getId().toString(), securityKey);
        String reportLink = CommonFunctions.getBaseUrl() + "faces/requests/ix.xhtml?id=" + temId;

        String template = configOptionApplicationController.getLongTextValueByKey(
                "Email Body for Lab Report Email",
                "Dear {patient_name},\n\nYour report for {investigation_name} is now available.\n\nPlease use the following link to view your report:\n{report_link}\n\nThank you."
        );

        String patientName = r.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getNameWithTitle();
        String investigationName = r.getPatientInvestigation().getInvestigation().getName();

        return template
                .replace("{patient_name}", patientName)
                .replace("{investigation_name}", investigationName)
                .replace("{report_link}", reportLink);
    }

    public String emailSubject(PatientReport r) {
        String template = configOptionApplicationController.getLongTextValueByKey(
                "Email Subject for Lab Report Email",
                "Your report for {investigation_name} is ready"
        );

        String investigationName = r.getPatientInvestigation().getInvestigation().getName();

        return template.replace("{investigation_name}", investigationName);
    }

    public String smsBody(PatientReport r, String old) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, 1);
        String temId = getSecurityController().encrypt(r.getId().toString());
        try {
            temId = URLEncoder.encode(temId, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // Handle the exception
        }
        String url = CommonFunctions.getBaseUrl() + "faces/requests/report1.xhtml?id=" + temId;
        String b = "Your "
                + r.getPatientInvestigation().getInvestigation().getName()
                + " is ready. "
                + url;
        return b;
    }

    public String smsBody(PatientReport r, boolean old) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, 1);
        String temId = currentPatientReport.getId() + "";
        temId = getSecurityController().encrypt(temId);
        try {
            temId = URLEncoder.encode(temId, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
        }
        String ed = CommonFunctions.getDateFormat(c.getTime(), "ddMMMMyyyyhhmmss");
        ed = getSecurityController().encrypt(ed);
        try {
            ed = URLEncoder.encode(ed, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
        }
        String url = CommonFunctions.getBaseUrl() + "faces/requests/report.xhtml?id=" + temId + "&user=" + ed;
        String b = "Your "
                + r.getPatientInvestigation().getInvestigation().getName()
                + " is ready. "
                + url;
        return b;
    }

    public String generateQrCodeLink(PatientReport r) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, 1);
        String temId = currentPatientReport.getId() + "";
        temId = getSecurityController().encrypt(temId);
        try {
            temId = URLEncoder.encode(temId, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
        }
        String ed = CommonFunctions.getDateFormat(c.getTime(), "ddMMMMyyyyhhmmss");
        ed = getSecurityController().encrypt(ed);
        try {
            ed = URLEncoder.encode(ed, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
        }
        String url = CommonFunctions.getBaseUrl() + "faces/requests/report.xhtml?id=" + temId + "&user=" + ed;
        return url;
    }

    public String generateQrCodeDetails(PatientReport r) {
        if (r != null) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.MONTH, 1);

            // Ensure currentPatientReport is not null
            if (currentPatientReport != null) {
                String temId = currentPatientReport.getId() + "";
                temId = getSecurityController().encrypt(temId);
                try {
                    temId = URLEncoder.encode(temId, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    // Handle the exception
                }
            }

            // Ensure all chained method calls do not result in null
            if (r.getPatientInvestigation() != null
                    && r.getPatientInvestigation().getBillItem() != null
                    && r.getPatientInvestigation().getBillItem().getBill() != null
                    && r.getPatientInvestigation().getBillItem().getBill().getPatient() != null
                    && r.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson() != null) {

                // Initialize variables to store values
                String patientName = "";
                String patientAge = "";
                String patientSex = "";
                String patientAddress = "";
                String identityNumber = "";
                String investigationName = "";

                // Retrieve values if not null
                if (r.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getName() != null) {
                    patientName = r.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getName();
                }

                if (r.getPatientInvestigation().getBillItem().getBill().getBillDate() != null) {
                    patientAge = r.getPatientInvestigation().getBillItem().getBill().getPatient().getAgeOnBilledDate(r.getPatientInvestigation().getBillItem().getBill().getBillDate());
                }

                if (r.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getSex() != null) {
                    patientSex = r.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getSex().name();
                }

                if (r.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getAddress() != null) {
                    patientAddress = r.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getAddress();
                }

                if (r.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getNic() != null) {
                    identityNumber = r.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getNic();
                }

                if (r.getPatientInvestigation().getInvestigation().getName() != null) {
                    investigationName = r.getPatientInvestigation().getInvestigation().getName();
                }

                // Construct the URL
                String temId = ""; // Initialize temId
                if (currentPatientReport != null) {
                    temId = currentPatientReport.getId() + "";
                    temId = getSecurityController().encrypt(temId);
                    try {
                        temId = URLEncoder.encode(temId, "UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        // Handle the exception
                    }
                }

                String ed = CommonFunctions.getDateFormat(c.getTime(), "ddMMMMyyyyhhmmss");
                ed = getSecurityController().encrypt(ed);
                try {
                    ed = URLEncoder.encode(ed, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    // Handle the exception
                }

                String url = CommonFunctions.getBaseUrl() + "faces/requests/report.xhtml?id=" + temId + "&user=" + ed;

                // Create the QR code contents using the variables and URL
                String qrCodeContents = "Patient Name: " + patientName + "\n"
                        + "Patient Age: " + patientAge + "\n"
                        + "Patient Sex: " + patientSex + "\n";

                if (!patientAddress.isEmpty()) {
                    qrCodeContents += "Patient Address: " + patientAddress + "\n";
                }

                if (!identityNumber.isEmpty()) {
                    qrCodeContents += "Identity Number: " + identityNumber + "\n";
                }

                qrCodeContents += "Investigation Name: " + investigationName + "\n";
                qrCodeContents += "URL: " + url;

                return qrCodeContents;
            }
        }

        // Return an empty string if any of the objects is null
        return "";
    }

    public BooleanMessage canApproveThePatientReport(PatientReport prForApproval) {
        //System.out.println("canApproveThePatientReport");
        BooleanMessage bm = new BooleanMessage();
        boolean flag = true;
        StringBuilder appMgs = new StringBuilder();
        if (prForApproval == null) {
            bm.setFlag(flag);
            bm.setMessage(appMgs.toString());
            return bm;
        }
        //System.out.println("stage = " + 1);
        for (PatientReportItemValue temIv : prForApproval.getPatientReportItemValues()) {
            InvestigationItem temii = temIv.getInvestigationItem();
            if (temii.getIxItemType() == InvestigationItemType.Value || temii.getIxItemType() == InvestigationItemType.Calculation) {
                if (temii.isCanNotApproveIfValueIsEmpty()) {
                    //System.out.println("stage = " + 3);
                    if (temii.getIxItemValueType() == InvestigationItemValueType.Varchar) {
                        if (temIv.getStrValue() == null || temIv.getStrValue().trim().isEmpty()) {
                            flag = false;
                            appMgs.append(temii.getEmptyValueWarning()).append("\n");
                        }
                    }
                    if (temii.getIxItemValueType() == InvestigationItemValueType.Double) {
                        if (temIv.getDoubleValue() == null) {
                            flag = false;
                            appMgs.append(temii.getEmptyValueWarning()).append("\n");
                        }
                    }
                    if (temii.getIxItemValueType() == InvestigationItemValueType.Memo) {
                        if (temIv.getLobValue() == null || temIv.getLobValue().trim().isEmpty()) {
                            flag = false;
                            appMgs.append(temii.getEmptyValueWarning()).append("\n");
                        }
                    }
                }
                if (temii.isCanNotApproveIfValueIsAboveAbsoluteHighValue() || temii.isCanNotApproveIfValueIsBelowAbsoluteLowValue()) {
                    //System.out.println("stage = " + 12);
                    Double tv = null;
                    if (temii.getIxItemValueType() == InvestigationItemValueType.Double) {
                        tv = temIv.getDoubleValue();
                    }
                    if (temii.getIxItemValueType() == InvestigationItemValueType.Varchar) {
                        tv = CommonFunctions.getDouble(temIv.getStrValue());
                    }
                    //System.out.println("tv = " + tv);
                    if (temii.isCanNotApproveIfValueIsAboveAbsoluteHighValue()) {
                        if (tv > temii.getAbsoluteHighValue()) {
                            flag = false;
                            appMgs.append(temii.getAboveAbsoluteWarning()).append("\n");
                        }
                    }
                    if (temii.isCanNotApproveIfValueIsBelowAbsoluteLowValue()) {
                        if (tv < temii.getAbsoluteLowValue()) {
                            flag = false;
                            appMgs.append(temii.getBelowAbsoluteWarning()).append("\n");
                        }
                    }

                }

            }
        }
        bm.setFlag(flag);
        bm.setMessage(appMgs.toString());
        return bm;
    }

    public void sendSms() {
        Sms e = new Sms();
        e.setPending(true);
        e.setCreatedAt(new Date());
        e.setCreater(sessionController.getLoggedUser());

        e.setCreatedAt(new Date());
        e.setCreater(sessionController.getLoggedUser());

        e.setReceipientNumber(smsNumber);
        e.setSendingMessage(smsMessage);
        e.setDepartment(getSessionController().getLoggedUser().getDepartment());
        e.setInstitution(getSessionController().getLoggedUser().getInstitution());
        e.setSentSuccessfully(false);
        getSmsFacade().create(e);

    }

    public boolean checkAlreadyGeneratedPatientReportsExists(PatientInvestigation patientInvestigation) {
        boolean allowNewReport = false;
        PatientReport pr = null;

        if (patientInvestigation.getBillItem().getItem().isMultipleReportsAllowed()) {
            allowNewReport = true;
        } else {
            Map params = new HashMap<>();

            String jpql = "SELECT r "
                    + "FROM PatientReport r "
                    + " WHERE r.retired=:ret"
                    + " and r.patientInvestigation=:pi";

            params.put("pi", patientInvestigation);
            params.put("ret", false);
            System.out.println("jpql = " + jpql);

            pr = getFacade().findFirstByJpql(jpql, params);

            if (pr != null) {
                allowNewReport = false;
            } else {
                allowNewReport = true;
            }

        }
        return allowNewReport;
    }

    public boolean checkAlreadyApprovedPatientReportsExists(PatientInvestigation patientInvestigation) {
        boolean allowNewReport = false;
        PatientReport pr = null;

        if (patientInvestigation.getBillItem().getItem().isMultipleReportsAllowed()) {
            allowNewReport = true;
        } else {
            Map params = new HashMap<>();

            String jpql = "SELECT r "
                    + "FROM PatientReport r "
                    + " WHERE r.retired=:ret "
                    + " and r.approved=:app "
                    + " and r.patientInvestigation=:pi";

            params.put("pi", patientInvestigation);
            params.put("ret", false);
            params.put("app", true);
            System.out.println("jpql = " + jpql);

            pr = getFacade().findFirstByJpql(jpql, params);

            if (pr != null) {
                allowNewReport = false;
            } else {
                allowNewReport = true;
            }

        }
        return allowNewReport;
    }

    public void addPatientReportGroupForMicrobiology() {
        if (!validateAndPrepareGroupAddition()) {
            return;
        }

        PatientReportGroup newlyAddedGroup = addPatientReportGroup();

        if (newlyAddedGroup == null) {
            return;
        }

        int groupCount = currentPatientReport.getPatientReportGroups().size();

        if (groupCount == 1) {
            System.out.println("first group");
            for (PatientReportItemValue pvm : currentPatientReport.getPatientReportItemValues()) {
                if (pvm.getInvestigationItem() != null
                        && pvm.getInvestigationItem().getIxItemType() != null
                        && pvm.getInvestigationItem().getIxItemType() == InvestigationItemType.Antibiotic) {
                    pvm.setPatientReportGroup(newlyAddedGroup);
                }
            }
        } else {
            System.out.println("more than one group");
            PatientReportGroup firstGroup = currentPatientReport.getPatientReportGroups().get(0);
            System.out.println("firstGroup = " + firstGroup.getGroupName());
            // Step 1: Filter the base items first
            List<PatientReportItemValue> baseAntibioticItems = new ArrayList<>();
            for (PatientReportItemValue basePvm : currentPatientReport.getPatientReportItemValues()) {
                if (basePvm.getInvestigationItem() != null
                        && basePvm.getInvestigationItem().getIxItemType() != null
                        && basePvm.getInvestigationItem().getIxItemType() == InvestigationItemType.Antibiotic
                        && basePvm.getPatientReportGroup().equals(firstGroup)) {
                    baseAntibioticItems.add(basePvm);
                }
            }

            // Step 2: Safely clone and add
            for (PatientReportItemValue basePvm : baseAntibioticItems) {
                PatientReportItemValue clonedPvm = basePvm.clone();
                clonedPvm.setStrValue(null);
                clonedPvm.setLobValue(null);
                clonedPvm.setDisplayValue(null);
                clonedPvm.setPatientReportGroup(newlyAddedGroup);
                currentPatientReport.getPatientReportItemValues().add(clonedPvm);
            }
        }
        savePatientReport();
        setGroupName(null);
    }

    public PatientReportGroup addPatientReportGroup() {
        if (!validateAndPrepareGroupAddition()) {
            return null;
        }

        PatientReportGroup prg = new PatientReportGroup();
        prg.setGroupName(groupName);
        prg.setPatientReport(currentPatientReport);
        prg.setCreatedAt(new Date());
        prg.setCreater(sessionController.getLoggedUser());

        if (currentPatientReport.getPatientReportGroups() == null) {
            currentPatientReport.setPatientReportGroups(new ArrayList<>());
        }

        currentPatientReport.getPatientReportGroups().add(prg);
        savePatientReport();
        return prg;
    }

    private boolean validateAndPrepareGroupAddition() {
        if (currentPatientReport == null) {
            JsfUtil.addErrorMessage("No patient report available");
            return false;
        }

        if (groupName == null || groupName.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Enter a Group Name");
            return false;
        }

        return true;
    }

    public void approvePatientReport() {
        if (currentPatientReport == null) {
            JsfUtil.addErrorMessage("Nothing to approve");
            return;
        }
        
        if (currentPatientReport.getDataEntered() == false) {
            JsfUtil.addErrorMessage("First Save report");
            return;
        }

        if (!checkAlreadyApprovedPatientReportsExists(currentPatientReport.getPatientInvestigation())) {
            JsfUtil.addErrorMessage("Another Report of this Investigation has been Approved");
            return;
        }

        BooleanMessage tbm = canApproveThePatientReport(currentPatientReport);
        if (!tbm.isFlag()) {
            JsfUtil.addErrorMessage(tbm.getMessage());
            return;
        }

        boolean authorized = configOptionApplicationController.getBooleanValueByKey("The relevant authorized user must approve the test report himself.", false);
        if (authorized) {
            if (currentPatientReport.getPatientInvestigation().getInvestigation().getStaff() != null) {
                System.out.println("Logged User ID = " + sessionController.getLoggedUser().getStaff().getId());
                System.out.println("Item User ID = " + currentPatientReport.getPatientInvestigation().getInvestigation().getStaff().getId());

                if (!(sessionController.getLoggedUser().getStaff().getId().equals(currentPatientReport.getPatientInvestigation().getInvestigation().getStaff().getId()))) {
                    JsfUtil.addErrorMessage("You can't access to Approve this Report");
                    return;
                }
            }
        }

        getCurrentPtIx().setApproved(true);
        currentPtIx.setApproveAt(Calendar.getInstance().getTime());
        currentPtIx.setApproveUser(getSessionController().getLoggedUser());
        currentPtIx.setApproveDepartment(getSessionController().getDepartment());

        currentPtIx.setStatus(PatientInvestigationStatus.REPORT_APPROVED);

        getPiFacade().edit(currentPtIx);

        currentPatientReport.setApproved(Boolean.TRUE);

        currentPatientReport.setApproveAt(Calendar.getInstance().getTime());
        currentPatientReport.setApproveDepartment(getSessionController().getLoggedUser().getDepartment());
        currentPatientReport.setApproveInstitution(getSessionController().getLoggedUser().getInstitution());
        currentPatientReport.setApproveUser(getSessionController().getLoggedUser());
        currentPatientReport.setQrCodeContentsDetailed(generateQrCodeDetails(currentPatientReport));
        currentPatientReport.setQrCodeContentsLink(generateQrCodeLink(currentPatientReport));

        currentPatientReport.setPrinted(Boolean.FALSE);
        currentPatientReport.setPrintingAt(null);
        currentPatientReport.setPrintingUser(null);

        getFacade().edit(currentPatientReport);
        getStaffController().setCurrent(getSessionController().getLoggedUser().getStaff());
        getTransferController().setStaff(getSessionController().getLoggedUser().getStaff());

        if (CommonFunctions.isValidEmail(currentPtIx.getBillItem().getBill().getPatient().getPerson().getEmail())) {
            AppEmail e;
            e = new AppEmail();

            e.setCreatedAt(new Date());
            e.setCreater(sessionController.getLoggedUser());
            e.setReceipientEmail(currentPtIx.getBillItem().getBill().getPatient().getPerson().getEmail());
            e.setMessageSubject(emailSubject(currentPatientReport));
//            e.setMessageBody(emailMessageBody(currentPatientReport)); // No longer User. We do NOT send the report within the email. Instead we send the Link
            e.setMessageBody(emailBody(currentPatientReport));
//                e.setAttachment1(createPDFAndSaveAsaFile());// No longer User. We do NOT send the report within the email. Instead we send the Link

//            e.setSenderPassword(getCurrentPatientReport().getApproveInstitution().getEmailSendingPassword()); // THese are taken from configuration options
//            e.setSenderUsername(getCurrentPatientReport().getApproveInstitution().getEmailSendingUsername());// THese are taken from configuration options
//            e.setSenderEmail(getCurrentPatientReport().getApproveInstitution().getEmail());// THese are taken from configuration options
            e.setDepartment(getSessionController().getLoggedUser().getDepartment());
            e.setInstitution(getSessionController().getLoggedUser().getInstitution());

            e.setBill(currentPtIx.getBillItem().getBill());
            e.setPatientReport(currentPatientReport);
            e.setPatientInvestigation(currentPtIx);
            e.setMessageType(MessageType.LabReport);

            e.setSentSuccessfully(false);
            e.setPending(true);

            getEmailFacade().create(e);
        }

        if (currentPtIx.getBillItem().getBill().getPatient().getPatientPhoneNumber() != null) {
            Patient tmp = currentPtIx.getBillItem().getBill().getPatient();
            tmp.getPerson().setSmsNumber(String.valueOf(tmp.getPatientPhoneNumber()));
        }
// ChatGPT and CodeRabbitAI contributed logic:
// Always create the SMS for lab report approval, regardless of payment status.
// The timer service will handle delayed sending and check full payment before dispatch.
        if (currentPtIx != null
                && currentPtIx.getBillItem() != null
                && currentPtIx.getBillItem().getBill() != null
                && currentPtIx.getBillItem().getBill().getPatient() != null
                && currentPtIx.getBillItem().getBill().getPatient().getPerson() != null
                && currentPtIx.getBillItem().getBill().getPatient().getPerson().getSmsNumber() != null
                && !currentPtIx.getBillItem().getBill().getPatient().getPerson().getSmsNumber().trim().isEmpty()) {

            Sms e = new Sms();
            e.setPending(true);
            e.setCreatedAt(new Date());
            e.setCreater(sessionController.getLoggedUser());
            e.setBill(currentPtIx.getBillItem().getBill());
            e.setPatientReport(currentPatientReport);
            e.setPatientInvestigation(currentPtIx);
            e.setSmsType(MessageType.LabReport);
            e.setReceipientNumber(currentPtIx.getBillItem().getBill().getPatient().getPerson().getSmsNumber());
            e.setSendingMessage(smsBody(currentPatientReport));
            e.setDepartment(sessionController.getLoggedUser().getDepartment());
            e.setInstitution(sessionController.getLoggedUser().getInstitution());
            e.setSentSuccessfully(false);
            getSmsFacade().create(e);
        }
        if (currentPtIx.getBillItem().getBill().getCollectingCentre() != null) {

            if (!currentPtIx.getBillItem().getBill().getCollectingCentre().getPhone().trim().equals("")) {
                Sms e = new Sms();
                e.setCreatedAt(new Date());
                e.setCreater(sessionController.getLoggedUser());
                e.setBill(currentPtIx.getBillItem().getBill());
                e.setPatientReport(currentPatientReport);
                e.setPatientInvestigation(currentPtIx);
                e.setCreatedAt(new Date());
                e.setCreater(sessionController.getLoggedUser());
                e.setReceipientNumber(currentPtIx.getBillItem().getBill().getCollectingCentre().getPhone());
                e.setSendingMessage(smsBody(currentPatientReport));
                e.setDepartment(getSessionController().getLoggedUser().getDepartment());
                e.setInstitution(getSessionController().getLoggedUser().getInstitution());
                e.setSentSuccessfully(false);
                getSmsFacade().create(e);
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Lab Test History Enabled", false)) {
            labTestHistoryController.addApprovalHistory(currentPtIx, currentPatientReport);
        }

        JsfUtil.addSuccessMessage("Approved");

    }

    public void approveEmrPatientReport() {
        Date startTime = new Date();
        if (currentPatientReport == null) {
            JsfUtil.addErrorMessage("Nothing to approve");
            return;
        }
        if (currentPatientReport.getDataEntered() == false) {
            JsfUtil.addErrorMessage("First Save report");
            return;
        }

        BooleanMessage tbm = canApproveThePatientReport(currentPatientReport);
        if (!tbm.isFlag()) {
            JsfUtil.addErrorMessage(tbm.getMessage());
            return;
        }

        getCurrentPtIx().setApproved(true);
        currentPtIx.setApproveAt(Calendar.getInstance().getTime());
        currentPtIx.setApproveUser(getSessionController().getLoggedUser());
        currentPtIx.setApproveDepartment(getSessionController().getDepartment());
        getPiFacade().edit(currentPtIx);
        currentPatientReport.setApproved(Boolean.FALSE);
        currentPatientReport.setApproved(Boolean.TRUE);
        currentPatientReport.setApproveAt(Calendar.getInstance().getTime());
        currentPatientReport.setApproveDepartment(getSessionController().getLoggedUser().getDepartment());
        currentPatientReport.setApproveInstitution(getSessionController().getLoggedUser().getInstitution());
        currentPatientReport.setApproveUser(getSessionController().getLoggedUser());
        currentPatientReport.setQrCodeContentsDetailed(generateQrCodeDetails(currentPatientReport));
        currentPatientReport.setQrCodeContentsLink(generateQrCodeLink(currentPatientReport));
        getFacade().edit(currentPatientReport);
        getStaffController().setCurrent(getSessionController().getLoggedUser().getStaff());
        getTransferController().setStaff(getSessionController().getLoggedUser().getStaff());

        if (clinicalFindingValue != null) {
            getClinicalFindingValue().setPatientInvestigation(currentPtIx);
            if (clinicalFindingValue.getId() != null) {
                clinicalFindingValueFacade.edit(clinicalFindingValue);
            }
            clinicalFindingValue = null;
        }

        JsfUtil.addSuccessMessage("Approved");

    }

    public void sendSmsForPatientReport() {
        if (currentPatientReport == null) {
            JsfUtil.addErrorMessage("Nothing to approve");
            return;
        }
        if (currentPatientReport.getDataEntered() == false) {
            JsfUtil.addErrorMessage("First Save report");
            return;
        }
        if (currentPatientReport.getApproved() == false) {
            JsfUtil.addErrorMessage("First Approve report");
            return;
        }

        if (!currentPtIx.getBillItem().getBill().getPatient().getPerson().getSmsNumber().trim().equals("")) {
            Sms e = new Sms();
            e.setCreatedAt(new Date());
            e.setCreater(sessionController.getLoggedUser());
            e.setBill(currentPtIx.getBillItem().getBill());
            e.setPatientReport(currentPatientReport);
            e.setPatientInvestigation(currentPtIx);
            e.setCreatedAt(new Date());
            e.setSmsType(MessageType.LabReport);
            e.setCreater(sessionController.getLoggedUser());
            e.setReceipientNumber(currentPtIx.getBillItem().getBill().getPatient().getPerson().getSmsNumber());
            e.setSendingMessage(smsBody(currentPatientReport));
            e.setDepartment(getSessionController().getLoggedUser().getDepartment());
            e.setInstitution(getSessionController().getLoggedUser().getInstitution());
            e.setPending(false);
            getSmsFacade().create(e);

            Boolean sent = smsManager.sendSms(e);
            e.setSentSuccessfully(sent);
            getSmsFacade().edit(e);

            if (configOptionApplicationController.getBooleanValueByKey("Lab Test History Enabled", false)) {
                labTestHistoryController.addReportSentSMSHistory(currentPtIx, currentPatientReport, e);
            }

        }

        JsfUtil.addSuccessMessage("SMS Sent");
//
    }

    public void sendEmailForPatientReport() {
        System.out.println("sendEmailForPatientReport");
        if (currentPatientReport == null) {
            JsfUtil.addErrorMessage("No patient report selected");
            return;
        }
        if (currentPatientReport.getPatientInvestigation() == null) {
            JsfUtil.addErrorMessage("Internal Error");
            return;
        }
        if (currentPatientReport.getPatientInvestigation().getBillItem() == null) {
            JsfUtil.addErrorMessage("Internal Error");
            return;
        }
        if (currentPatientReport.getPatientInvestigation().getBillItem().getBill() == null) {
            JsfUtil.addErrorMessage("Internal Error");
            return;
        }

        Bill bill = currentPatientReport.getPatientInvestigation().getBillItem().getBill();
        if (bill.getBalance() > 0.99) {
            JsfUtil.addErrorMessage("Bill is NOT Fully Settled. Please settle the bill first before sending an email");
            return;
        }

        if (!currentPatientReport.getDataEntered()) {
            JsfUtil.addErrorMessage("First Save report");
            return;
        }
        if (!currentPatientReport.getApproved()) {
            JsfUtil.addErrorMessage("First Approve report");
            return;
        }

        if (receipientEmail == null || receipientEmail.isEmpty()) {
            JsfUtil.addErrorMessage("No recipient Email");
            return;
        }
        if (!CommonFunctions.isValidEmail(receipientEmail)) {
            JsfUtil.addErrorMessage("Recipient Email is NOT valid");
            return;
        }

        System.out.println("all checks ok");

        AppEmail email = new AppEmail();
        email.setCreatedAt(new Date());
        email.setCreater(sessionController.getLoggedUser());
        email.setReceipientEmail(receipientEmail);
        email.setMessageSubject(emailSubject(currentPatientReport));
        email.setMessageBody(emailBody(currentPatientReport));
        email.setDepartment(sessionController.getLoggedUser().getDepartment());
        email.setInstitution(sessionController.getLoggedUser().getInstitution());
        email.setBill(bill);
        email.setPatientReport(currentPatientReport);
        email.setPatientInvestigation(currentPatientReport.getPatientInvestigation());
        email.setMessageType(MessageType.LabReport);
        email.setSentSuccessfully(false);
        email.setPending(true);
        getEmailFacade().create(email);

        System.out.println("email = " + email);

        if (configOptionApplicationController.getBooleanValueByKey("Lab Test History Enabled", false)) {
            labTestHistoryController.addReportSentEmailHistory(currentPtIx, currentPatientReport, email);
        }

        try {
            boolean success = emailManagerEjb.sendEmail(
                    Collections.singletonList(email.getReceipientEmail()),
                    email.getMessageBody(),
                    email.getMessageSubject(),
                    true
            );
            email.setSentSuccessfully(success);
            email.setPending(!success);
            if (success) {
                email.setSentAt(new Date());
                JsfUtil.addSuccessMessage("Email Sent Successfully");
            } else {
                JsfUtil.addErrorMessage("Sending Email Failed");
            }
            emailFacade.edit(email);
        } catch (Exception ex) {
            JsfUtil.addErrorMessage("Sending Email Failed");
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
                    "Failed to process Email ID: " + (email != null ? email.getId() : "unknown"), ex);
        }
    }

    public void reverseApprovalOfPatientReport() {
        Date startTime = new Date();
        if (currentPatientReport == null) {
            JsfUtil.addErrorMessage("Nothing to approve");
            return;
        }
        if (currentPatientReport.getDataEntered() == false) {
            JsfUtil.addErrorMessage("First Save report");
            return;
        }
        if (currentPatientReport.getApproved() == false) {
            JsfUtil.addErrorMessage("First Approve report");
            return;
        }
        getCurrentPtIx().setApproved(false);
        currentPtIx.setCancelled(Boolean.FALSE);
        currentPtIx.setCancelledAt(Calendar.getInstance().getTime());
        currentPtIx.setCancelledUser(getSessionController().getLoggedUser());
        currentPtIx.setCancellDepartment(getSessionController().getDepartment());
        
        currentPtIx.setApproveAt(null);
        currentPtIx.setApproveDepartment(null);
        currentPtIx.setApproveUser(null);
        
        getPiFacade().edit(currentPtIx);
        currentPatientReport.setApproved(Boolean.FALSE);
        currentPatientReport.setApproveUser(null);
        currentPatientReport.setCancelledAt(Calendar.getInstance().getTime());
        currentPatientReport.setCancellDepartment(getSessionController().getLoggedUser().getDepartment());
        currentPatientReport.setCancellInstitution(getSessionController().getLoggedUser().getInstitution());
        currentPatientReport.setCancelledUser(getSessionController().getLoggedUser());
        getFacade().edit(currentPatientReport);
        getStaffController().setCurrent(getSessionController().getLoggedUser().getStaff());
        getTransferController().setStaff(getSessionController().getLoggedUser().getStaff());
        JsfUtil.addSuccessMessage("Approval Reversed");

        try {
            if (CommonFunctions.isValidEmail(getSessionController().getLoggedUser().getInstitution().getOwnerEmail())) {
                AppEmail e = new AppEmail();
                e.setCreatedAt(new Date());
                e.setCreater(sessionController.getLoggedUser());

                e.setReceipientEmail(getSessionController().getLoggedUser().getInstitution().getOwnerEmail());
                e.setMessageSubject("This Report is Reversed after Athorizing");
                e.setMessageBody(cancelApprovalEmailMessageBody(currentPatientReport));
//                e.setAttachment1(createPDFAndSaveAsaFile());

                e.setSenderPassword(getCurrentPatientReport().getApproveInstitution().getEmailSendingPassword());
                e.setSenderUsername(getCurrentPatientReport().getApproveInstitution().getEmailSendingUsername());
                e.setSenderEmail(getCurrentPatientReport().getApproveInstitution().getEmail());

                e.setDepartment(getSessionController().getLoggedUser().getDepartment());
                e.setInstitution(getSessionController().getLoggedUser().getInstitution());

                e.setSentSuccessfully(false);

                getEmailFacade().create(e);
            }

        } catch (Exception e) {
        }

        if (configOptionApplicationController.getBooleanValueByKey("Lab Test History Enabled", false)) {
            labTestHistoryController.addApprovalCancelHistory(currentPtIx, currentPatientReport);
        }

    }

    public void printPatientReport() {
        //////System.out.println("going to save as printed");
        if (currentPatientReport == null) {
            JsfUtil.addErrorMessage("Nothing to approve");
            return;
        }
        currentPtIx.setPrinted(true);
        currentPtIx.setPrintingAt(Calendar.getInstance().getTime());
        currentPtIx.setPrintingUser(getSessionController().getLoggedUser());
        currentPtIx.setPrintingDepartment(getSessionController().getDepartment());
        currentPtIx.setPrinted(true);
        getPiFacade().edit(currentPtIx);

        currentPatientReport.setPrinted(Boolean.TRUE);
        currentPatientReport.setPrintingAt(Calendar.getInstance().getTime());
        currentPatientReport.setPrintingDepartment(getSessionController().getLoggedUser().getDepartment());
        currentPatientReport.setPrintingInstitution(getSessionController().getLoggedUser().getInstitution());
        currentPatientReport.setPrintingUser(getSessionController().getLoggedUser());
        getFacade().edit(currentPatientReport);

    }

    public void printPatientLabReport() {
        if (currentPatientReport == null) {
            JsfUtil.addErrorMessage("Nothing to approve");
            return;
        }
        currentPtIx.setPrinted(true);
        currentPtIx.setPrintingAt(Calendar.getInstance().getTime());
        currentPtIx.setPrintingUser(getSessionController().getLoggedUser());
        currentPtIx.setPrintingDepartment(getSessionController().getDepartment());
        currentPtIx.setPrinted(true);
        getPiFacade().edit(currentPtIx);

        currentPatientReport.setPrinted(Boolean.TRUE);
        currentPatientReport.setPrintingAt(Calendar.getInstance().getTime());
        currentPatientReport.setPrintingDepartment(getSessionController().getLoggedUser().getDepartment());
        currentPatientReport.setPrintingInstitution(getSessionController().getLoggedUser().getInstitution());
        currentPatientReport.setPrintingUser(getSessionController().getLoggedUser());
        currentPatientReport.setPrinted(true);

        getFacade().edit(currentPatientReport);
    }

//    public void pdfPatientReport() throws DocumentException, com.lowagie.text.DocumentException, IOException {
////        long serialVersionUID = 626953318628565053L;
//        //System.out.println("enter 1");
////        long serialVersionUID = 626953318628565053L;
//        //        long serialVersionUID = 626953318628565053L;
//        //        long serialVersionUID = 626953318628565053L;
//        //        long serialVersionUID = 626953318628565053L;
//        FacesContext facesContext = FacesContext.getCurrentInstance();
//        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
//        //System.out.println("enter 2");
//        response.reset();
//
//        String pdf_url = commonController.getBaseUrl() + "faces/lab/patient_report.xhtml";
//
//        response.setHeader(pdf_url, "application/pdf");
//        //System.out.println("enter 3");
//        OutputStream outputStream = response.getOutputStream();
//        //System.out.println("enter 4");
//        URL url = new URL(pdf_url);
//        InputStream pdfInputStream = url.openStream();
//        byte[] byteBuffer = new byte[2048];
//        int byteRead;
//        while ((byteRead = pdfInputStream.read(byteBuffer)) > 0) {
//            outputStream.write(byteBuffer, 0, byteRead);
//        }
//        outputStream.flush();
//        pdfInputStream.close();
//
//        facesContext.responseComplete();
//
//    }
    public void createPDF() {
//        FacesContext facesContext = FacesContext.getCurrentInstance();
//        ExternalContext externalContext = facesContext.getExternalContext();
//        HttpSession session = (HttpSession) externalContext.getSession(true);
////        String url = "http://localhost:8080/new/faces/lab/lab/patient_report_print.xhtml:jsessionid=" + session.getId() + "?pdf=true";
//        String url = commonController.getBaseUrl() + "faces/requests/report.xhtml?id=" + securityController.encrypt(currentPatientReport.getId() + "");
//        try {
//            ITextRenderer renderer = new ITextRenderer();
//            renderer.setDocument(new URL(url).toString());
//            renderer.layout();
//            HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
//            response.reset();
//            response.setContentType("application/pdf");
//            response.setHeader("Content-Disposition", "inline; filename=\"print.pdf\"");
//            OutputStream outputStream = response.getOutputStream();
//            renderer.createPDF(outputStream);
//            JsfUtil.addSuccessMessage("PDF Created");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        facesContext.responseComplete();
    }

//    public String createPDFAndSaveAsaFile() {
//        String temId = currentPatientReport.getId() + "";
//        temId = securityController.encrypt(temId);
//        try {
//            temId = URLEncoder.encode(temId, "UTF-8");
//        } catch (UnsupportedEncodingException ex) {
//        }
//        String url = commonController.getBaseUrl() + "faces/requests/report.xhtml?id=" + temId;
//        FileOutputStream fop = null;
//        File file;
//        try {
//            try {
//                url = URLEncoder.encode(url, "UTF-8");
//            } catch (UnsupportedEncodingException ex) {
//            }
//            ITextRenderer renderer = new ITextRenderer();
//            renderer.setDocument(new URL(url).toString());
//
//            renderer.layout();
//            file = new File("/tmp/" + currentPatientReport.getId() + ".pdf");
//            fop = new FileOutputStream(file);
//            if (!file.exists()) {
//                file.createNewFile();
//            }
//            OutputStream outputStream = fop;
//            renderer.createPDF(outputStream);
////            JsfUtil.addSuccessMessage("PDF Created");
////            JsfUtil.addSuccessMessage("PDF Created");
//            return file.getAbsolutePath();
//        } catch (DocumentException | IOException e) {
//        }
//        return "";
//    }
    public String getSelectText() {
        return selectText;
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PatientReportController() {
    }

    private PatientReportFacade getFacade() {
        return ejbFacade;
    }

    public PatientInvestigation getCurrentPtIx() {
        if (currentPtIx == null) {
            if (currentPatientReport != null) {
                currentPtIx = currentPatientReport.getPatientInvestigation();
            }
        }
        updateRecipientEmail();
        return currentPtIx;
    }

    private void updateRecipientEmail() {
        receipientEmail = null; // Reset first
        if (currentPtIx == null) {
            return;
        }
        BillItem bi = currentPtIx.getBillItem();
        if (bi == null) {
            return;
        }
        Bill bill = bi.getBill();
        if (bill == null) {
            return;
        }
        Patient patient = bill.getPatient();
        if (patient == null) {
            return;
        }
        Person person = patient.getPerson();
        if (person != null) {
            receipientEmail = person.getEmail();
        }
    }

    public PatientReport createNewPatientReportForRequests(PatientInvestigation pi, Investigation ix) {
        PatientReport r = null;
        if (pi != null && ix != null) {
            r = new PatientReport();
            Patient pt = pi.getPatient();
            r.setPatientName(pt.getPerson().getNameWithTitle());
            r.setPatientAge(pt.getAgeOnBilledDate(pi.getBillItem().getBill().getCreatedAt()));
            r.setPatientGender(pt.getPerson().getSex().getLabel());
            r.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            r.setCreater(getSessionController().getLoggedUser());
            r.setItem(ix);
            r.setDataEntryDepartment(sessionController.getLoggedUser().getDepartment());
            r.setDataEntryInstitution(sessionController.getLoggedUser().getInstitution());
            getFacade().create(r);
            r.setPatientInvestigation(pi);
            getPrBean().addPatientReportItemValuesForReport(r);
//            getEjbFacade().edit(r);
//            setCurrentPatientReport(r);
            pi.getPatientReports().add(r);
            getPiFacade().edit(pi);
//            getCommonReportItemController().setCategory(ix.getReportFormat());
        } else {
            JsfUtil.addErrorMessage("No ptIx or Ix selected to add");
        }
        return r;
    }

    public PatientReport getUnapprovedPatientReport(PatientInvestigation pi) {
        String j = "select r from PatientReport r "
                + " where r.patientInvestigation = :pi "
                + " and (r.approved = :a or r.approved is null) "
                + " order by r.id desc";

        Map m = new HashMap();
        m.put("pi", pi);
        m.put("a", false);
        PatientReport r = getFacade().findFirstByJpql(j, m);
        return r;
    }

    public PatientReport createNewPatientReport(PatientInvestigation pi, Investigation ix) {
        String sampleIDs = "";
        List<PatientSampleComponant> pscs = patientInvestigationController.getPatientSampleComponentsByInvestigation(pi);
        if (pscs != null) {
            for (PatientSampleComponant psc : pscs) {
                if (!psc.getPatientSample().getSampleRejected()) {
                    sampleIDs += psc.getPatientSample().getIdStr() + " ";
                }

            }
        }
        return createNewPatientReport(pi, ix, sampleIDs);
    }

    public PatientReport createNewPatientReportForUpload(PatientInvestigation pi, Investigation ix) {
        String sampleIDs = "";
        List<PatientSampleComponant> pscs = patientInvestigationController.getPatientSampleComponentsByInvestigation(pi);
        if (pscs != null) {
            for (PatientSampleComponant psc : pscs) {
                sampleIDs += psc.getPatientSample().getIdStr() + " ";
            }
        }
        return createNewPatientReportForUpload(pi, ix, sampleIDs);
    }

    public PatientReport createNewPatientReport(PatientInvestigation pi, Investigation ix, String sampleIds) {
        System.out.println("createNewPatientReport");
        PatientReport r = null;
        if (pi != null && pi.getId() != null && ix != null) {
            r = new PatientReport();
            Patient pt = pi.getPatient();
            r.setPatientName(pt.getPerson().getNameWithTitle());
            r.setPatientAge(pt.getAgeOnBilledDate(pi.getBillItem().getBill().getCreatedAt()));
            r.setPatientGender(pt.getPerson().getSex().getLabel());
            r.setReportType(ReportType.GENARATE);
            r.setSampleIDs(sampleIds);
            r.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            r.setCreater(getSessionController().getLoggedUser());
            r.setItem(ix);
            r.setDataEntryDepartment(sessionController.getLoggedUser().getDepartment());
            r.setDataEntryInstitution(sessionController.getLoggedUser().getInstitution());
            if (r.getTransInvestigation() != null) {
                if (r.getTransInvestigation().getReportFormat() != null) {
                    r.setReportFormat(r.getTransInvestigation().getReportFormat());
                } else {
                    ReportFormat nrf = reportFormatController.getValidReportFormat();
                    r.setReportFormat(nrf);
                }
            }
            getFacade().create(r);
            r.setPatientInvestigation(pi);
            getPrBean().addPatientReportItemValuesForReport(r);
            getFacade().edit(r);
            setCurrentPatientReport(r);
            pi.getPatientReports().add(r);
            pi.setStatus(PatientInvestigationStatus.SAMPLE_INTERFACED);
            pi.setPerformed(true);
            pi.setPerformDepartment(sessionController.getDepartment());
            pi.setPerformInstitution(sessionController.getInstitution());
            pi.setPerformedAt(new Date());
            pi.setPerformedUser(sessionController.getLoggedUser());

            pi.setPrinted(false);
            pi.setPrintingAt(null);
            pi.setPrintingDepartment(null);
            pi.setPrintingInstitution(null);
            pi.setPrintingUser(null);

            piFacade.edit(pi);
            getCommonReportItemController().setCategory(ix.getReportFormat());
        } else {
            JsfUtil.addErrorMessage("No ptIx or Ix selected to add");
        }
        return r;
    }

    public PatientReport createNewPatientReportForUpload(PatientInvestigation pi, Investigation ix, String sampleIds) {
        System.out.println("createNewPatientReport");
        PatientReport r = null;
        if (pi != null && pi.getId() != null && ix != null) {
            r = new PatientReport();
            Patient pt = pi.getPatient();
            r.setPatientName(pt.getPerson().getNameWithTitle());
            r.setPatientAge(pt.getAgeOnBilledDate(pi.getBillItem().getBill().getCreatedAt()));
            r.setPatientGender(pt.getPerson().getSex().getLabel());
            r.setSampleIDs(sampleIds);
            r.setReportType(ReportType.UPLOAD);
            r.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            r.setCreater(getSessionController().getLoggedUser());
            r.setItem(ix);
            r.setDataEntryDepartment(sessionController.getLoggedUser().getDepartment());
            r.setDataEntryInstitution(sessionController.getLoggedUser().getInstitution());
            if (r.getTransInvestigation() != null) {
                if (r.getTransInvestigation().getReportFormat() != null) {
                    r.setReportFormat(r.getTransInvestigation().getReportFormat());
                } else {
                    ReportFormat nrf = reportFormatController.getValidReportFormat();
                    r.setReportFormat(nrf);
                }
            }
            getFacade().create(r);
            System.out.println("r = " + r);
            r.setPatientInvestigation(pi);
            getFacade().edit(r);
            setCurrentPatientReport(r);
            pi.getPatientReports().add(r);
            pi.setStatus(PatientInvestigationStatus.SAMPLE_INTERFACED);
            pi.setPerformed(true);
            pi.setPerformDepartment(sessionController.getDepartment());
            pi.setPerformInstitution(sessionController.getInstitution());
            pi.setPerformedAt(new Date());
            pi.setPerformedUser(sessionController.getLoggedUser());

            pi.setPrinted(false);
            pi.setPrintingAt(null);
            pi.setPrintingDepartment(null);
            pi.setPrintingInstitution(null);
            pi.setPrintingUser(null);

            piFacade.edit(pi);
            getCommonReportItemController().setCategory(ix.getReportFormat());
        } else {
            JsfUtil.addErrorMessage("No ptIx or Ix selected to add");
        }
        return r;
    }

    public PatientReport createNewPatientTemplateReport(PatientInvestigation pi, Investigation ix) {
        System.out.println("createNewPatientTemplateReport");
        System.out.println("pi = " + pi);
        System.out.println("ix = " + ix);
        //System.err.println("creating a new patient report");
        PatientReport r = null;
        if (pi != null && pi.getId() != null && ix != null) {
            r = new PatientReport();
            r.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            r.setCreater(getSessionController().getLoggedUser());
            r.setItem(ix);
            r.setDataEntryDepartment(sessionController.getLoggedUser().getDepartment());
            r.setDataEntryInstitution(sessionController.getLoggedUser().getInstitution());
            if (r.getTransInvestigation() != null) {
                if (r.getTransInvestigation().getReportFormat() != null) {
                    r.setReportFormat(r.getTransInvestigation().getReportFormat());
                } else {
                    ReportFormat nrf = reportFormatController.getValidReportFormat();
                    r.setReportFormat(nrf);
                }
            }
            getFacade().create(r);
            r.setPatientInvestigation(pi);
            getPrBean().addPatientReportItemValuesForTemplateReport(r);
//            getEjbFacade().edit(r);
            setCurrentPatientReport(r);
            pi.getPatientReports().add(r);
            getCommonReportItemController().setCategory(ix.getReportFormat());
        } else {
            JsfUtil.addErrorMessage("No ptIx or Ix selected to add");
        }
        return r;
    }

    public PatientReport createNewMicrobiologyReport(PatientInvestigation pi, Investigation ix) {
        PatientReport r = null;
        if (pi != null && pi.getId() != null && ix != null) {
            r = new PatientReport();
            Patient pt = pi.getPatient();
            r.setPatientName(pt.getPerson().getNameWithTitle());
            r.setPatientAge(pt.getAgeOnBilledDate(pi.getBillItem().getBill().getCreatedAt()));
            r.setPatientGender(pt.getPerson().getSex().getLabel());
            r.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            r.setReportType(ReportType.GENARATE);
            r.setCreater(getSessionController().getLoggedUser());
            r.setItem(ix);
            if (r.getTransInvestigation() != null) {
                r.setReportFormat(r.getTransInvestigation().getReportFormat());
            }
            getFacade().create(r);
            r.setPatientInvestigation(pi);
            getPrBean().addMicrobiologyReportItemValuesForReport(r);
//            getEjbFacade().edit(r);
            setCurrentPatientReport(r);
            pi.getPatientReports().add(r);
            setGroupName("Antibiotic Sensitivity Test");
            addPatientReportGroupForMicrobiology();
            getCommonReportItemController().setCategory(ix.getReportFormat());
        } else {
            JsfUtil.addErrorMessage("No ptIx or Ix selected to add");
        }
        return r;
    }

    public void setCurrentPtIx(PatientInvestigation currentPtIx) {
        this.currentPtIx = currentPtIx;
        updateRecipientEmail();
    }

    public PatientReportBean getPrBean() {
        return prBean;
    }

    public void setPrBean(PatientReportBean prBean) {
        this.prBean = prBean;
    }

    public ReportFormatController getReportFormatController() {
        return reportFormatController;
    }

    public void setReportFormatController(ReportFormatController reportFormatController) {
        this.reportFormatController = reportFormatController;
    }

    public CommonReportItemController getCommonReportItemController() {
        return commonReportItemController;
    }

    public void setCommonReportItemController(CommonReportItemController commonReportItemController) {
        this.commonReportItemController = commonReportItemController;
    }

    public PatientReport getCurrentPatientReport() {
//        PatientReport cpt;
//        if (currentPatientReport != null && currentPatientReport.getId() != null && currentPatientReport.getId() != 0) {
//            cpt = getFacade().find(currentPatientReport.getId());
//            currentPatientReport = cpt;
//        }
//        //System.out.println("currentPatientReport = " + currentPatientReport.toString());
        return currentPatientReport;
    }

    public PatientReport getLastPatientReport(Investigation ix) {
        String j;
        PatientReport pr;
        Map m = new HashMap();
        if (ix.getReportedAs() == null) {
            m.put("ix", ix);
        } else {
            Investigation ixr = (Investigation) ix.getReportedAs();
            m.put("ix", ixr);
        }
        j = "select pr from PatientReport pr"
                + " where pr.item=:ix "
                + " order by pr.id desc";
        pr = getFacade().findFirstByJpql(j, m);
        return pr;
    }

    public String navigateToCreatedPatientReport(PatientInvestigation pi) {
        String link;
        if (pi == null) {
            JsfUtil.addErrorMessage("No Patient Investigation");
            return null;
        }

        if (pi.getInvestigation().isAlternativeReportAllowed()) {
            link = patientInvestigationController.navigateToAlternativeReportSelector(pi);
        } else {
            link = navigateToNewlyCreatedPatientReport(pi);
        }
        return link;
    }

    public String navigateToUploadNewPatientReport(PatientInvestigation pi) {
        if (pi == null) {
            JsfUtil.addErrorMessage("No Patient Investigation");
            return null;
        }
        Investigation ix = null;
        if (pi.getInvestigation() == null) {
            JsfUtil.addErrorMessage("No Investigation for Patient Report");
            return null;
        } else {
            ix = (Investigation) pi.getInvestigation();
        }
        if (ix.getReportedAs() != null) {
            ix = (Investigation) pi.getInvestigation().getReportedAs();
        }
        if (!checkAlreadyGeneratedPatientReportsExists(pi)) {
            JsfUtil.addErrorMessage("Already Created Report for this Investigation");
            return null;
        }

        currentReportInvestigation = ix;
        currentPtIx = pi;
        PatientReport newlyCreatedReport = null;

        newlyCreatedReport = createNewPatientReportForUpload(pi, ix);

        if (newlyCreatedReport == null) {
            JsfUtil.addErrorMessage("Error");
            return null;
        }
        currentPatientReport = newlyCreatedReport;

        System.out.println("New Report = " + currentPatientReport);

        getCommonReportItemController().setCategory(ix.getReportFormat());

        System.out.println("currentPtIx = " + currentPtIx);

        patientReportUploadController.setReportUpload(null);
        patientReportUploadController.setPatientInvestigation(pi);

        return "/lab/upload_patient_report?faces-redirect=true";

    }

    public String navigateToNewlyCreatedPatientReport(PatientInvestigation pi) {
        if (pi == null) {
            JsfUtil.addErrorMessage("No Patient Report");
            return null;
        }

        if (!checkAlreadyGeneratedPatientReportsExists(pi)) {
            JsfUtil.addErrorMessage("Another Report of this Investigation has been Approved");
            return null;
        }

        Investigation ix = null;
        if (pi.getInvestigation() == null) {
            JsfUtil.addErrorMessage("No Investigation for Patient Report");
            return null;
        } else {
            ix = (Investigation) pi.getInvestigation();
        }
        if (ix.getReportedAs() != null) {
            ix = (Investigation) pi.getInvestigation().getReportedAs();
        }

        currentReportInvestigation = ix;
        currentPtIx = pi;
        PatientReport newlyCreatedReport = null;
        if (ix.getReportType() == InvestigationReportType.Microbiology) {
            newlyCreatedReport = createNewMicrobiologyReport(pi, ix);
        } else {
            newlyCreatedReport = createNewPatientReport(pi, ix);
        }

        if (newlyCreatedReport == null) {
            JsfUtil.addErrorMessage("Error");
            return null;
        }

        if (configOptionApplicationController.getBooleanValueByKey("Lab Test History Enabled", false)) {
            labTestHistoryController.addCreateReportHistory(currentPtIx, currentPatientReport);
        }

        currentPatientReport = newlyCreatedReport;
        getCommonReportItemController().setCategory(ix.getReportFormat());

        fillReportFormats(currentPatientReport);

        return "/lab/patient_report?faces-redirect=true";
    }

    private List<ReportFormat> avalilableReportFormats;

    public List<ReportFormat> fillReportFormats(PatientReport patientReport) {
        avalilableReportFormats = new ArrayList<>();
        if (!patientReport.getApproved()) {
            if (configOptionApplicationController.getBooleanValueByKey("Obtaining the report format related to the logged-in department's site.", false)) {
                avalilableReportFormats = reportFormatController.fillReportFormatsForLoggedDepartmentSite(patientReport);

                if (!avalilableReportFormats.isEmpty()) {
                    if (avalilableReportFormats.size() >= 1) {
                        patientReport.setReportFormat(avalilableReportFormats.get(0));
                    }
                } else {
                    ReportFormat currentPatientReportFormat = (ReportFormat) patientReport.getPatientInvestigation().getInvestigation().getReportFormat();
                    avalilableReportFormats.add(currentPatientReportFormat);
                    patientReport.setReportFormat(currentPatientReportFormat);
                }
            } else {
                avalilableReportFormats = reportFormatController.getParentFormat();
            }
        } else {
            avalilableReportFormats.add((ReportFormat) patientReport.getReportFormat());
        }

        return avalilableReportFormats;
    }

    public void createNewReport(PatientInvestigation pi) {
        System.out.println("createNewReport");
        System.out.println("pi = " + pi);
        if (pi == null) {
            JsfUtil.addErrorMessage("No Patient Report");
            return;
        }
        Investigation ix = null;
        System.out.println("pi.getInvestigation() = " + pi.getInvestigation());
        if (pi.getInvestigation() == null) {
            JsfUtil.addErrorMessage("No Investigation for Patient Report");
            return;
        } else {
            ix = (Investigation) pi.getInvestigation();
            System.out.println("ix = " + ix);
        }
        System.out.println("ix.getReportedAs() = " + ix.getReportedAs());
        if (ix.getReportedAs() != null) {
            ix = (Investigation) pi.getInvestigation().getReportedAs();
            System.out.println("ix = " + ix);
        }

        currentReportInvestigation = ix;
        currentPtIx = pi;
        System.out.println("ix.getReportType()  = " + ix.getReportType());
        if (ix.getReportType() == InvestigationReportType.Microbiology) {
            createNewMicrobiologyReport(pi, ix);
        } else if (ix.getReportType() == InvestigationReportType.HtmlTemplate) {
            createNewPatientTemplateReport(pi, ix);
        } else {
            createNewPatientReport(pi, ix);
        }
        getCommonReportItemController().setCategory(ix.getReportFormat());
    }

    public String enterNewReportFormat(PatientInvestigation pi, Investigation ix) {
        currentReportInvestigation = ix;
        currentPtIx = pi;

        createNewPatientReport(pi, ix);
        getCommonReportItemController().setCategory(ix.getReportFormat());
        return "/lab/patient_report";
    }

    public String enterNewAlternativeReportFormat(PatientInvestigation pi) {
        currentPtIx = pi;
        String sampleId = null;

        if (currentPatientSampleComponant != null
                && currentPatientSampleComponant.getPatientSample() != null) {
            sampleId = currentPatientSampleComponant.getPatientSample().getIdStr();
        }

        createNewPatientReport(pi, currentReportInvestigation, sampleId);
        if (currentReportInvestigation != null) {
            getCommonReportItemController().setCategory(currentReportInvestigation.getReportFormat());
        }

        return "/lab/patient_report?faces-redirect=true";
    }

    public List<PatientReport> getCustomerReports() {
        return customerReports;
    }

    public void setCustomerReports(List<PatientReport> customerReports) {
        this.customerReports = customerReports;
    }

    public void setCurrentPatientReport(PatientReport currentPatientReport) {
        System.out.println("Setting current patient report: " + currentPatientReport);
        this.currentPatientReport = currentPatientReport;
        if (currentPatientReport != null) {
            System.out.println("Report format: " + currentPatientReport.getItem().getReportFormat());
            getCommonReportItemController().setCategory(currentPatientReport.getItem().getReportFormat());
            currentPtIx = currentPatientReport.getPatientInvestigation();
            System.out.println("Current patient investigation index: " + currentPtIx);
        } else {
            System.out.println("No current patient report provided, skipping setting category and index.");
        }
    }

    public TestFlagFacade getTestFlagFacade() {
        return testFlagFacade;
    }

    public void setTestFlagFacade(TestFlagFacade testFlagFacade) {
        this.testFlagFacade = testFlagFacade;
    }

    public TransferController getTransferController() {
        return transferController;
    }

    public void setTransferController(TransferController transferController) {
        this.transferController = transferController;
    }

    public InvestigationItem getInvestigationItem() {
        return investigationItem;
    }

    public void setInvestigationItem(InvestigationItem investigationItem) {
        this.investigationItem = investigationItem;
    }

    public List<Selectable> getSelectables() {
        return selectables;
    }

    public String getReceipientEmail() {
        if (receipientEmail == null) {
            updateRecipientEmail();
        }
        return receipientEmail;
    }

    public void setReceipientEmail(String receipientEmail) {
        this.receipientEmail = receipientEmail;
    }

    public void setSelectables(List<Selectable> selectables) {
        this.selectables = selectables;
    }

    public InvestigationItemController getInvestigationItemController() {
        return investigationItemController;
    }

    public ApplicationController getApplicationController() {
        return applicationController;
    }

    public SecurityController getSecurityController() {
        return securityController;
    }

    public EmailFacade getEmailFacade() {
        return emailFacade;
    }

    public EmailManagerEjb getEmailManagerEjb() {
        return emailManagerEjb;
    }

    public String getEncryptedExpiary() {
        return encryptedExpiary;
    }

    public void setEncryptedExpiary(String encryptedExpiary) {
        this.encryptedExpiary = encryptedExpiary;
    }

    public List<PatientReport> getRecentReportsOrderedByDoctor() {
        return recentReportsOrderedByDoctor;
    }

    public void setRecentReportsOrderedByDoctor(List<PatientReport> recentReportsOrderedByDoctor) {
        this.recentReportsOrderedByDoctor = recentReportsOrderedByDoctor;
    }

    public String getSmsNumber() {
        return smsNumber;
    }

    public void setSmsNumber(String smsNumber) {
        this.smsNumber = smsNumber;
    }

    public String getSmsMessage() {
        return smsMessage;
    }

    public void setSmsMessage(String smsMessage) {
        this.smsMessage = smsMessage;
    }

    public boolean isShowBackground() {
        return showBackground;
    }

    public void setShowBackground(boolean showBackground) {
        this.showBackground = showBackground;
    }

    public ClinicalFindingValue getClinicalFindingValue() {
        return clinicalFindingValue;
    }

    public void setClinicalFindingValue(ClinicalFindingValue clinicalFindingValue) {
        this.clinicalFindingValue = clinicalFindingValue;
    }

    public String getComment() {
        if (comment == null) {
            comment = "";
        }
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public PatientSample getCurrentPatientSample() {
        return currentPatientSample;
    }

    public void setCurrentPatientSample(PatientSample currentPatientSample) {
        this.currentPatientSample = currentPatientSample;
    }

    public PatientSampleComponant getCurrentPatientSampleComponant() {
        return currentPatientSampleComponant;
    }

    public void setCurrentPatientSampleComponant(PatientSampleComponant currentPatientSampleComponant) {
        this.currentPatientSampleComponant = currentPatientSampleComponant;

    }

    public List<ReportFormat> getAvalilableReportFormats() {
        return avalilableReportFormats;
    }

    public void setAvalilableReportFormats(List<ReportFormat> avalilableReportFormats) {
        this.avalilableReportFormats = avalilableReportFormats;

    }

    public ConfigOptionApplicationController getConfigOptionApplicationController() {
        return configOptionApplicationController;
    }

    public void setConfigOptionApplicationController(ConfigOptionApplicationController configOptionApplicationController) {
        this.configOptionApplicationController = configOptionApplicationController;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;

    }

    @FacesConverter(forClass = PatientReport.class)
    public static class PatientReportControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PatientReportController controller = (PatientReportController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "patientReportController");
            return controller.getFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            if (NumberUtils.isNumber(value)) {
                key = Long.valueOf(value);
            } else {
                key = 0l;
            }
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
            if (object instanceof PatientReport) {
                PatientReport o = (PatientReport) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PatientReportController.class.getName());
            }
        }
    }

    public String getEncryptedPatientReportId() {
        return encryptedPatientReportId;
    }

    public void setEncryptedPatientReportId(String encryptedPatientReportId) {
        this.encryptedPatientReportId = encryptedPatientReportId;
    }

    public SmsManagerEjb getSmsManager() {
        return smsManager;
    }

    public void setSmsManager(SmsManagerEjb smsManager) {
        this.smsManager = smsManager;
    }

    public SmsFacade getSmsFacade() {
        return smsFacade;
    }

    public void setSmsFacade(SmsFacade smsFacade) {
        this.smsFacade = smsFacade;
    }

}
