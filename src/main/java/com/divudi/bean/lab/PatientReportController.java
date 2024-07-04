package com.divudi.bean.lab;

import com.divudi.bean.common.ApplicationController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.ItemForItemController;
import com.divudi.bean.common.SecurityController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.TransferController;

import com.divudi.bean.hr.StaffController;
import com.divudi.data.BooleanMessage;
import com.divudi.data.CalculationType;
import com.divudi.data.InvestigationItemType;
import com.divudi.data.InvestigationItemValueType;
import com.divudi.data.InvestigationReportType;
import com.divudi.data.MessageType;
import com.divudi.data.Sex;
import com.divudi.data.SmsSentResponse;
import com.divudi.data.lab.Selectable;
import com.divudi.ejb.EmailManagerEjb;
import com.divudi.ejb.PatientReportBean;
import com.divudi.ejb.SmsManagerEjb;
import com.divudi.entity.AppEmail;
import com.divudi.entity.Doctor;
import com.divudi.entity.Patient;
import com.divudi.entity.Sms;
import com.divudi.entity.UserPreference;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.entity.lab.IxCal;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.lab.PatientReport;
import com.divudi.entity.lab.PatientReportItemValue;
import com.divudi.entity.lab.TestFlag;
import com.divudi.facade.EmailFacade;
import com.divudi.facade.IxCalFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PatientInvestigationItemValueFacade;
import com.divudi.facade.PatientReportFacade;
import com.divudi.facade.PatientReportItemValueFacade;
import com.divudi.facade.SmsFacade;
import com.divudi.facade.TestFlagFacade;
import com.divudi.facade.UserPreferenceFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.clinical.ClinicalFindingValue;
import com.divudi.facade.ClinicalFindingValueFacade;
import com.divudi.java.CommonFunctions;
import com.lowagie.text.DocumentException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.math.NumberUtils;
import org.primefaces.event.CellEditEvent;
import org.xhtmlrenderer.pdf.ITextRenderer;
import javax.faces.context.FacesContext;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.faces.context.ExternalContext;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpSession;

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
    //Controllers
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
    private CommonController commonController;
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
    //Class Variables
    String selectText = "";
    private PatientInvestigation currentPtIx;
    private PatientReport currentPatientReport;
    Investigation currentReportInvestigation;
    Investigation alternativeInvestigation;
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
    }

    public List<PatientReport> patientReports(PatientInvestigation pi) {
        String j = "select r from PatientReport r "
                + " where r.patientInvestigation=:pi";
        Map m = new HashMap();
        m.put("pi", pi);
        return getFacade().findByJpql(j, m);
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

        if (getCurrentPatientReport().getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getEmail() == null) {
            JsfUtil.addErrorMessage("Email not given");
            return;
        }
        if (!CommonController.isValidEmail(getCurrentPatientReport().getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getEmail())) {
            JsfUtil.addErrorMessage("Given email is NOT valid.");
            return;
        }

        boolean sent = getEmailManagerEjb().sendEmail(
                getCurrentPatientReport().getApproveInstitution().getEmailSendingUsername(),
                getCurrentPatientReport().getApproveInstitution().getEmailSendingPassword(),
                getCurrentPatientReport().getApproveInstitution().getEmail(),
                getCurrentPatientReport().getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getEmail(),
                getCurrentPatientReport().getPatientInvestigation().getInvestigation().getName() + " Results",
                emailMessageBody(currentPatientReport),
                createPDFAndSaveAsaFile());

        if (sent) {
            JsfUtil.addSuccessMessage("Email Sent");
        } else {
            JsfUtil.addErrorMessage("Email Sending Failed");
        }

    }

    public void sendEmailOld() {

        final String username = getCurrentPatientReport().getPatientInvestigation().getBillItem().getItem().getDepartment().getInstitution().getEmailSendingUsername();
        final String password = getCurrentPatientReport().getPatientInvestigation().getBillItem().getItem().getDepartment().getInstitution().getEmailSendingPassword();

        Properties props = new Properties();
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
//        Authenticator auth = new SMTPAuthenticator();
        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(getCurrentPatientReport().getPatientInvestigation().getBillItem().getBill().getToDepartment().getInstitution().getEmailSendingUsername()));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(getCurrentPatientReport().getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getEmail()));
            message.setSubject(getCurrentPatientReport().getPatientInvestigation().getInvestigation().getName() + " Results");
//            message.setText("Dear " + getCurrentPatientReport().getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getNameWithTitle()
//                    + ",\n\n Please find the results of your " + getCurrentPatientReport().getPatientInvestigation().getInvestigation().getName() + ".");

            message.setContent(emailMessageBody(currentPatientReport), "text/html; charset=utf-8");
            //3) create MimeBodyPart object and set your message text     
            BodyPart msbp1 = new MimeBodyPart();
            msbp1.setText("Final Lab report of patient");

            //4) create new MimeBodyPart object and set DataHandler object to this object      
            MimeBodyPart msbp2 = new MimeBodyPart();
//            createHtmlFile();
            DataSource source = new FileDataSource(createPDFAndSaveAsaFile());
            msbp2.setDataHandler(new DataHandler(source));
            msbp2.setFileName("/tmp/report" + getCurrentPatientReport().getId() + ".pdf");

            //5) create Multipart object and add Mimdler(soeBodyPart objects to this object      
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(msbp1);
            multipart.addBodyPart(msbp2);

            //6) set the multiplart object to the message object  
            message.setContent(multipart);

            Transport.send(message);

            JsfUtil.addSuccessMessage("Email Sent SUccessfully");

        } catch (MessagingException e) {
            JsfUtil.addErrorMessage("Error. " + e.getMessage());
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error. " + e.getMessage());
        }

    }

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

    public String toEditTemplate() {
        if (investigationItem == null) {
            JsfUtil.addErrorMessage("Select a template first");
            return "";
        }
        investigationItemController.setCurrent(investigationItem);
        return "/lab/investigation_item_value_path";
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

            if (priv.getInvestigationItem().getFormatString() != null && !priv.getInvestigationItem().getFormatString().trim().equals("")) {
                if (priv.getInvestigationItem().getIxItemValueType() == InvestigationItemValueType.Varchar) {
                    double tmpDbl = CommonController.extractDoubleValue(priv.getStrValue());
                    priv.setStrValue(CommonController.formatNumber(tmpDbl, priv.getInvestigationItem().getFormatString()));
                    priv.setDoubleValue(tmpDbl);
                } else if (priv.getInvestigationItem().getIxItemValueType() == InvestigationItemValueType.Double) {
                    Double numberWithLargeNumberOfDecimals = priv.getDoubleValue();
                    Double numberWithFormatter = CommonController.formatDouble(numberWithLargeNumberOfDecimals, priv.getInvestigationItem().getFormatString());
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

                System.out.println("baseJs = " + baseJs);
                if (baseJs != null) {
                    calString = generateModifiedJavascriptFromBaseJavaScript(currentPatientReport, baseJs);
                    System.out.println("calString = " + calString);
                }

                ScriptEngineManager mgr = new ScriptEngineManager();
                ScriptEngine engine = mgr.getEngineByName("JavaScript");
                try {
                    result = (double) engine.eval(calString);

                } catch (Exception ex) {
                    Logger.getLogger(PatientReportController.class
                            .getName()).log(Level.SEVERE, null, ex);
                    result = 0.0;
                }
                priv.setDoubleValue(result);

            } else if (priv.getInvestigationItem().getIxItemType() == InvestigationItemType.Flag) {
                priv.setStrValue(findFlagValue(priv));
            }

            getPirivFacade().edit(priv);

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
        int ageInYears = pr.getPatientInvestigation().getBillItem().getBill().getPatient().getAgeYearsonBilledDate();

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
//        if (currentPatientReport != null) {
//            for (PatientReportItemValue v : getCurrentPatientReport().getPatientReportItemValues()) {
//                ////System.out.println("saving ptrtiv + " + v);
//                ////System.out.println("saving ptrtiv Stre " + v.getStrValue());
//                ////System.out.println("saving ptrtiv Double " + v.getDoubleValue());
//                ////System.out.println("saving ptrtiv Lob " + v.getLobValue());
//                getPirivFacade().edit(v);
//            }
//        }
        if (currentPatientReport != null) {
            getFacade().edit(currentPatientReport);
        }
    }

    public void savePatientReport() {
        Date startTime = new Date();
        if (currentPatientReport == null || currentPtIx == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }

        getCurrentPtIx().setDataEntered(true);
        currentPtIx.setDataEntryAt(Calendar.getInstance().getTime());
        currentPtIx.setDataEntryUser(getSessionController().getLoggedUser());
        currentPtIx.setDataEntryDepartment(getSessionController().getDepartment());

        ////System.out.println("1. getPatientReportItemValues() = " + getPatientReportItemValues());
        ////System.out.println("2. currentPatientReport.getReportItemValues() = " + currentPatientReport.getPatientReportItemValues());
        ////System.out.println("3. currentPatientReport.getReportItemValues() = " + currentPatientReport.getPatientReportItemValues());
        currentPatientReport.setDataEntered(Boolean.TRUE);

        currentPatientReport.setDataEntryAt(Calendar.getInstance().getTime());
        currentPatientReport.setDataEntryDepartment(getSessionController().getLoggedUser().getDepartment());
        currentPatientReport.setDataEntryInstitution(getSessionController().getLoggedUser().getInstitution());
        currentPatientReport.setDataEntryUser(getSessionController().getLoggedUser());

        getFacade().edit(currentPatientReport);
        getPiFacade().edit(currentPtIx);

        //JsfUtil.addSuccessMessage("Saved");
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

        String ed = commonController.getDateFormat(c.getTime(), "ddMMMMyyyyhhmmss");
        ed = getSecurityController().encrypt(ed);
        try {
            ed = URLEncoder.encode(ed, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
        }

        String url = commonController.getBaseUrl() + "faces/requests/report.xhtml?id=" + temId + "&user=" + ed;
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

        String ed = commonController.getDateFormat(c.getTime(), "ddMMMMyyyyhhmmss");
        ed = getSecurityController().encrypt(ed);
        try {
            ed = URLEncoder.encode(ed, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
        }

        String url = commonController.getBaseUrl() + "faces/requests/report.xhtml?id=" + temId + "&user=" + ed;
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
        String temId = getSecurityController().encryptAlphanumeric(r.getId().toString(), securityKey);
        String url = commonController.getBaseUrl() + "faces/requests/ix.xhtml?id=" + temId;
        String b = "Your "
                + r.getPatientInvestigation().getInvestigation().getName()
                + " is ready. "
                + url;
        return b;
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
        String url = commonController.getBaseUrl() + "faces/requests/report1.xhtml?id=" + temId;
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
        String ed = commonController.getDateFormat(c.getTime(), "ddMMMMyyyyhhmmss");
        ed = getSecurityController().encrypt(ed);
        try {
            ed = URLEncoder.encode(ed, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
        }
        String url = commonController.getBaseUrl() + "faces/requests/report.xhtml?id=" + temId + "&user=" + ed;
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
        String ed = commonController.getDateFormat(c.getTime(), "ddMMMMyyyyhhmmss");
        ed = getSecurityController().encrypt(ed);
        try {
            ed = URLEncoder.encode(ed, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
        }
        String url = commonController.getBaseUrl() + "faces/requests/report.xhtml?id=" + temId + "&user=" + ed;
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

                String ed = commonController.getDateFormat(c.getTime(), "ddMMMMyyyyhhmmss");
                ed = getSecurityController().encrypt(ed);
                try {
                    ed = URLEncoder.encode(ed, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    // Handle the exception
                }

                String url = commonController.getBaseUrl() + "faces/requests/report.xhtml?id=" + temId + "&user=" + ed;

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
        String appMgs = "";
        if (prForApproval == null) {
            bm.setFlag(flag);
            bm.setMessage(appMgs);
            return bm;
        }
        //System.out.println("stage = " + 1);
        for (PatientReportItemValue temIv : prForApproval.getPatientReportItemValues()) {
            InvestigationItem temii = temIv.getInvestigationItem();
            if (temii.getIxItemType() == InvestigationItemType.Value || temii.getIxItemType() == InvestigationItemType.Calculation) {
                if (temii.isCanNotApproveIfValueIsEmpty()) {
                    //System.out.println("stage = " + 3);
                    if (temii.getIxItemValueType() == InvestigationItemValueType.Varchar) {
                        if (temIv.getStrValue() == null || temIv.getStrValue().trim().equals("")) {
                            flag = false;
                            appMgs += temii.getEmptyValueWarning() + "\n";
                        }
                    }
                    if (temii.getIxItemValueType() == InvestigationItemValueType.Double) {
                        if (temIv.getDoubleValue() == null) {
                            flag = false;
                            appMgs += temii.getEmptyValueWarning() + "\n";
                        }
                    }
                    if (temii.getIxItemValueType() == InvestigationItemValueType.Memo) {
                        if (temIv.getLobValue() == null || temIv.getLobValue().trim().equals("")) {
                            flag = false;
                            appMgs += temii.getEmptyValueWarning() + "\n";
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
                        tv = commonController.getDouble(temIv.getStrValue());
                    }
                    //System.out.println("tv = " + tv);
                    if (temii.isCanNotApproveIfValueIsAboveAbsoluteHighValue()) {
                        if (tv > temii.getAbsoluteHighValue()) {
                            flag = false;
                            appMgs += temii.getAboveAbsoluteWarning() + "\n";
                        }
                    }
                    if (temii.isCanNotApproveIfValueIsBelowAbsoluteLowValue()) {
                        if (tv < temii.getAbsoluteLowValue()) {
                            flag = false;
                            appMgs += temii.getBelowAbsoluteWarning() + "\n";
                        }
                    }

                }

            }
        }
        bm.setFlag(flag);
        bm.setMessage(appMgs);
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

    public void approvePatientReport() {
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

        UserPreference pf = getSessionController().getApplicationPreference();

        if (pf != null && pf.getSentEmailWithInvestigationReportApproval()) {
            if (CommonController.isValidEmail(currentPtIx.getBillItem().getBill().getPatient().getPerson().getEmail())) {
                AppEmail e;

                e = new AppEmail();
                e.setCreatedAt(new Date());
                e.setCreater(sessionController.getLoggedUser());

                e.setReceipientEmail(currentPtIx.getBillItem().getBill().getPatient().getPerson().getEmail());
                e.setMessageSubject("Your Report is Ready");
                e.setMessageBody(emailMessageBody(currentPatientReport));
                e.setAttachment1(createPDFAndSaveAsaFile());

                e.setSenderPassword(getCurrentPatientReport().getApproveInstitution().getEmailSendingPassword());
                e.setSenderUsername(getCurrentPatientReport().getApproveInstitution().getEmailSendingUsername());
                e.setSenderEmail(getCurrentPatientReport().getApproveInstitution().getEmail());

                e.setDepartment(getSessionController().getLoggedUser().getDepartment());
                e.setInstitution(getSessionController().getLoggedUser().getInstitution());

                e.setSentSuccessfully(false);

                getEmailFacade().create(e);
            }
        }
        if (currentPtIx.getBillItem().getBill().getPatient().getPatientPhoneNumber() != null) {
            Patient tmp = currentPtIx.getBillItem().getBill().getPatient();
            tmp.getPerson().setSmsNumber(String.valueOf(tmp.getPatientPhoneNumber()));
        }
        if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {
            if (getCurrentPtIx().getBillItem().getBill().getBackwardReferenceBill().getBalance() < 1.0) {
                if (!currentPtIx.getBillItem().getBill().getPatient().getPerson().getSmsNumber().trim().equals("")) {
                    Sms e = new Sms();
                    e.setPending(true);
                    e.setCreatedAt(new Date());
                    e.setCreater(sessionController.getLoggedUser());
                    e.setBill(currentPtIx.getBillItem().getBill());
                    e.setPatientReport(currentPatientReport);
                    e.setPatientInvestigation(currentPtIx);
                    e.setCreatedAt(new Date());
                    e.setCreater(sessionController.getLoggedUser());
                    e.setReceipientNumber(currentPtIx.getBillItem().getBill().getPatient().getPerson().getSmsNumber());
                    e.setSendingMessage(smsBody(currentPatientReport));
                    e.setDepartment(getSessionController().getLoggedUser().getDepartment());
                    e.setInstitution(getSessionController().getLoggedUser().getInstitution());
                    e.setSentSuccessfully(false);
                    getSmsFacade().create(e);
                }
            }
        } else {
            if (!currentPtIx.getBillItem().getBill().getPatient().getPerson().getSmsNumber().trim().equals("")) {
                Sms e = new Sms();
                e.setPending(true);
                e.setCreatedAt(new Date());
                e.setCreater(sessionController.getLoggedUser());
                e.setBill(currentPtIx.getBillItem().getBill());
                e.setPatientReport(currentPatientReport);
                e.setPatientInvestigation(currentPtIx);
                e.setCreatedAt(new Date());
                e.setCreater(sessionController.getLoggedUser());
                e.setReceipientNumber(currentPtIx.getBillItem().getBill().getPatient().getPerson().getSmsNumber());
                e.setSendingMessage(smsBody(currentPatientReport));
                e.setDepartment(getSessionController().getLoggedUser().getDepartment());
                e.setInstitution(getSessionController().getLoggedUser().getInstitution());
                e.setSentSuccessfully(false);
                getSmsFacade().create(e);
            }
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

        }

        JsfUtil.addSuccessMessage("SMS Sent");
//        
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
        getPiFacade().edit(currentPtIx);
        currentPatientReport.setApproved(Boolean.FALSE);
        currentPatientReport.setApproved(Boolean.FALSE);
        currentPatientReport.setCancelledAt(Calendar.getInstance().getTime());
        currentPatientReport.setCancellDepartment(getSessionController().getLoggedUser().getDepartment());
        currentPatientReport.setCancellInstitution(getSessionController().getLoggedUser().getInstitution());
        currentPatientReport.setCancelledUser(getSessionController().getLoggedUser());
        getFacade().edit(currentPatientReport);
        getStaffController().setCurrent(getSessionController().getLoggedUser().getStaff());
        getTransferController().setStaff(getSessionController().getLoggedUser().getStaff());
        JsfUtil.addSuccessMessage("Approval Reversed");

        try {
            if (CommonController.isValidEmail(getSessionController().getLoggedUser().getInstitution().getOwnerEmail())) {
                AppEmail e = new AppEmail();
                e.setCreatedAt(new Date());
                e.setCreater(sessionController.getLoggedUser());

                e.setReceipientEmail(getSessionController().getLoggedUser().getInstitution().getOwnerEmail());
                e.setMessageSubject("This Report is Reversed after Athorizing");
                e.setMessageBody(cancelApprovalEmailMessageBody(currentPatientReport));
                e.setAttachment1(createPDFAndSaveAsaFile());

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

    public void pdfPatientReport() throws DocumentException, com.lowagie.text.DocumentException, IOException {
//        long serialVersionUID = 626953318628565053L;
        //System.out.println("enter 1");
//        long serialVersionUID = 626953318628565053L;
        //        long serialVersionUID = 626953318628565053L;
        //        long serialVersionUID = 626953318628565053L;
        //        long serialVersionUID = 626953318628565053L;
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        //System.out.println("enter 2");
        response.reset();

        String pdf_url = commonController.getBaseUrl() + "faces/lab/patient_report.xhtml";

        response.setHeader(pdf_url, "application/pdf");
        //System.out.println("enter 3");
        OutputStream outputStream = response.getOutputStream();
        //System.out.println("enter 4");
        URL url = new URL(pdf_url);
        InputStream pdfInputStream = url.openStream();
        byte[] byteBuffer = new byte[2048];
        int byteRead;
        while ((byteRead = pdfInputStream.read(byteBuffer)) > 0) {
            outputStream.write(byteBuffer, 0, byteRead);
        }
        outputStream.flush();
        pdfInputStream.close();

        facesContext.responseComplete();

    }

    public void createPDF() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpSession session = (HttpSession) externalContext.getSession(true);
//        String url = "http://localhost:8080/new/faces/lab/lab/patient_report_print.xhtml:jsessionid=" + session.getId() + "?pdf=true";
        String url = commonController.getBaseUrl() + "faces/requests/report.xhtml?id=" + securityController.encrypt(currentPatientReport.getId() + "");
        try {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocument(new URL(url).toString());
            renderer.layout();
            HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
            response.reset();
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=\"print.pdf\"");
            OutputStream outputStream = response.getOutputStream();
            renderer.createPDF(outputStream);
            JsfUtil.addSuccessMessage("PDF Created");
        } catch (Exception e) {
            e.printStackTrace();
        }
        facesContext.responseComplete();
    }

    public String createPDFAndSaveAsaFile() {
        String temId = currentPatientReport.getId() + "";
        temId = securityController.encrypt(temId);
        try {
            temId = URLEncoder.encode(temId, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
        }
        String url = commonController.getBaseUrl() + "faces/requests/report.xhtml?id=" + temId;
        FileOutputStream fop = null;
        File file;
        try {
            try {
                url = URLEncoder.encode(url, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
            }
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocument(new URL(url).toString());

            renderer.layout();
            file = new File("/tmp/" + currentPatientReport.getId() + ".pdf");
            fop = new FileOutputStream(file);
            if (!file.exists()) {
                file.createNewFile();
            }
            OutputStream outputStream = fop;
            renderer.createPDF(outputStream);
//            JsfUtil.addSuccessMessage("PDF Created");
//            JsfUtil.addSuccessMessage("PDF Created");
            return file.getAbsolutePath();
        } catch (DocumentException | IOException e) {
        }
        return "";
    }

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
        return currentPtIx;
    }

    public PatientReport createNewPatientReportForRequests(PatientInvestigation pi, Investigation ix) {
        PatientReport r = null;
        if (pi != null && ix != null) {
            r = new PatientReport();
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
                r.setReportFormat(r.getTransInvestigation().getReportFormat());
            }
            getFacade().create(r);
            r.setPatientInvestigation(pi);
            getPrBean().addPatientReportItemValuesForReport(r);
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
            r.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
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
            getCommonReportItemController().setCategory(ix.getReportFormat());
        } else {
            JsfUtil.addErrorMessage("No ptIx or Ix selected to add");
        }
        return r;
    }

    public void setCurrentPtIx(PatientInvestigation currentPtIx) {
        this.currentPtIx = currentPtIx;
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

    public void createNewReport(PatientInvestigation pi) {
        if (pi == null) {
            JsfUtil.addErrorMessage("No Patient Report");
            return;
        }
        if (pi.getInvestigation() == null) {
            JsfUtil.addErrorMessage("No Investigation for Patient Report");
            return;
        }
        if (pi.getInvestigation().getReportedAs() == null) {
            JsfUtil.addErrorMessage("No Reported as for Investigation for Patient Report");
            return;
        }
        Investigation ix = (Investigation) pi.getInvestigation().getReportedAs();
        currentReportInvestigation = ix;
        currentPtIx = pi;
        if (ix.getReportType() == InvestigationReportType.Microbiology) {
            createNewMicrobiologyReport(pi, ix);
        } else {
            createNewPatientReport(pi, ix);
        }
        getCommonReportItemController().setCategory(ix.getReportFormat());
    }

    public String enterNewReportFormat(PatientInvestigation pi, Investigation ix) {
        //System.out.println("enterNewReportFormat");
        currentReportInvestigation = ix;
        currentPtIx = pi;
        createNewPatientReport(pi, ix);
        getCommonReportItemController().setCategory(ix.getReportFormat());
        return "/lab/patient_report";
    }

    public List<PatientReport> getCustomerReports() {
        return customerReports;
    }

    public void setCustomerReports(List<PatientReport> customerReports) {
        this.customerReports = customerReports;
    }

    public void setCurrentPatientReport(PatientReport currentPatientReport) {

        this.currentPatientReport = currentPatientReport;

        if (currentPatientReport != null) {
            getCommonReportItemController().setCategory(currentPatientReport.getItem().getReportFormat());
            currentPtIx = currentPatientReport.getPatientInvestigation();
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

    public void setSelectables(List<Selectable> selectables) {
        this.selectables = selectables;
    }

    public CommonController getCommonController() {
        return commonController;
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
