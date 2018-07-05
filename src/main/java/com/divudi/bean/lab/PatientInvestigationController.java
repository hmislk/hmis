package com.divudi.bean.lab;

import com.divudi.bean.common.BillController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.ItemForItemController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.bean.report.InstitutionLabSumeryController;
import com.divudi.data.ApplicationInstitution;
import com.divudi.data.InvestigationItemType;
import com.divudi.data.InvestigationItemValueType;
import com.divudi.data.ItemType;
import com.divudi.data.SmsType;
import com.divudi.data.lab.SysMex;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import com.divudi.entity.BillItem;
import com.divudi.entity.Department;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.Sms;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.lab.PatientReport;
import com.divudi.entity.lab.PatientReportItemValue;
import com.divudi.entity.lab.PatientSample;
import com.divudi.entity.lab.PatientSampleComponant;
import com.divudi.entity.lab.ReportItem;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.InvestigationItemFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PatientReportFacade;
import com.divudi.facade.PatientSampleComponantFacade;
import com.divudi.facade.PatientSampleFacade;
import com.divudi.facade.ReportItemFacade;
import com.divudi.facade.SmsFacade;
import com.divudi.facade.util.JsfUtil;
import com.itextpdf.text.DocumentException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletRequest;
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class PatientInvestigationController implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * EJBs
     */
    @EJB
    private PatientInvestigationFacade ejbFacade;
    @EJB
    PatientReportFacade prFacade;
    @EJB
    InvestigationItemFacade investigationItemFacade;
    @EJB
    CommonFunctions commonFunctions;
    @EJB
    private InvestigationFacade investFacade;
    @EJB
    SmsFacade smsFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    private PatientSampleFacade patientSampleFacade;
    @EJB
    private PatientSampleComponantFacade patientSampleComponantFacade;
    @EJB
    private ItemFacade itemFacade;

    /*
     * Controllers
     */
    @Inject
    private InstitutionLabSumeryController labReportSearchByInstitutionController;
    @Inject
    SessionController sessionController;
    @Inject
    CommonController commonController;
    @Inject
    private InvestigationItemController investigationItemController;
    @Inject
    private BillController billController;
    @Inject
    private PatientReportController patientReportController;
    @Inject
    private ItemForItemController itemForItemController;
    /**
     * Class Variables
     */
    List<PatientInvestigation> selectedItems;
    private PatientInvestigation current;
    Investigation currentInvestigation;
    List<InvestigationItem> currentInvestigationItems;
    private List<PatientInvestigation> items = null;
    private List<PatientInvestigation> lstToSamle = null;
    private List<PatientInvestigation> lstToReceive = null;
    private List<PatientInvestigation> lstToEnterData = null;
    private List<PatientReport> lstToApprove = null;
    private List<PatientReport> lstToPrint = null;
    String selectText = "";
    private Department department;
    @Temporal(TemporalType.TIME)
    private Date fromDate;
    Date toDate;

    boolean showSamplingPagination;
    List<Investigation> investSummery;
    Date sampledOutsideDate;
    boolean sampledOutSide;
    boolean listIncludingReceived;
    boolean listIncludingEnteredData;
    private boolean listIncludingSampled;
    boolean listIncludingApproved;
    List<PatientInvestigation> selectedToReceive;

    Sms sms;
    private Set<PatientSample> patientSamplesSet;
    private List<PatientSample> patientSamples;

    private String samplingRequestResponse;

    private String inputBillId;
    private String username;
    private String password;

    private String msg;
    private String machine;
    private String shift;
    private String shift1;
    private String shift2;

    private String apiResponse = "#{success=false|msg=No Requests}";

    public void msgFromMiddleware() {
        apiResponse = "";
        if (username == null || username.trim().equals("")) {
            apiResponse += "#{success=false|msg=No Username}";
            return;
        }
        if (password == null || password.trim().equals("")) {
            apiResponse += "#{success=false|msg=No Password}";
            return;
        }
        if (machine == null || machine.trim().equals("")) {
            apiResponse += "#{success=false|msg=No Machine Specified}";
            return;
        }
        if (msg == null || msg.trim().equals("")) {
            apiResponse += "#{success=false|msg=No Request From Analyzer}";
            return;
        }
        if (!sessionController.loginForRequests(username, password)) {
            apiResponse += "#{success=false|msg=Wrong username/password}";
            return;
        }
        if (machine.trim().equals("SysMex")) {
            apiResponse = msgFromSysmex();
            return;
        } else if (machine.trim().equals("Dimension")) {
            apiResponse += "#{success=false|msg=Analyzer not configured}";
            return;
        }
    }

    private String msgFromSysmex() {
        String temMsgs = "";
        SysMex sysMex = new SysMex();
        sysMex.setInputStringBytesSpaceSeperated(msg);

        System.out.println("Checking Report For the First Time");
        if (!sysMex.isCorrectReport()) {
            System.out.println("First TIme Not Correct Report. ");
            sysMex.setShift2(-1);
            sysMex.shiftPositions();
            if (!sysMex.isCorrectReport()) {
                System.out.println("Second TIme Not Correct Report. ");
                sysMex.setShift2(-2);
                sysMex.shiftPositions();
                if (!sysMex.isCorrectReport()) {
                    System.out.println("Thired TIme Not Correct Report. ");
                    sysMex.setShift2(-3);
                    sysMex.shiftPositions();
                    if (!sysMex.isCorrectReport()) {
                        System.out.println("Forth Time Not Correct Report. ");
                        sysMex.setShift2(1);
                        sysMex.shiftPositions();
                        if (!sysMex.isCorrectReport()) {
                            System.out.println("Fifth TIme Not Correct Report. ");
                            sysMex.setShift2(2);
                            sysMex.shiftPositions();
                            if (!sysMex.isCorrectReport()) {
                                System.out.println("Second TIme Not Correct Report. ");
                                sysMex.setShift2(3);
                                sysMex.shiftPositions();
                                return "#{success=false|msg=Wrong Data. Please resent results}";
                            }
                        }
                    }
                }
            }
        }
        System.out.println("sysMex.getSampleId() = " + sysMex.getSampleId());
        PatientSample ps = getPatientSampleFromId(sysMex.getSampleId());
        System.out.println("ps = " + ps);
        if (ps == null) {
            return "#{success=false|msg=Wrong Sample ID. Please resent results}";
        }
        List<PatientSampleComponant> pscs = getPatientSampleComponents(ps);
        System.out.println("pscs = " + pscs);
        if (pscs == null) {
            return "#{success=false|msg=Wrong Sample Components. Please inform developers}";
        }
        List<PatientInvestigation> ptixs = getPatientInvestigations(pscs);
        System.out.println("ptixs = " + ptixs);
        if (ptixs == null || ptixs.isEmpty()) {
            return "#{success=false|msg=Wrong Patient Investigations. Please inform developers}";
        }
        for (PatientInvestigation pi : ptixs) {
            System.out.println("pi = " + pi);
            System.out.println("pi.getBillItem().getBill().getInsId() = " + pi.getBillItem().getBill().getInsId());
            List<PatientReport> prs = new ArrayList<>();
            if (pi.getInvestigation().getMachine() != null && pi.getInvestigation().getMachine().getName().toLowerCase().contains("sysmex")) {
                System.out.println("1. pi.getInvestigation().getMachine().getName() = " + pi.getInvestigation().getMachine().getName());
                PatientReport tpr = patientReportController.createNewPatientReport(pi, pi.getInvestigation());
                prs.add(tpr);
            }
            List<Item> temItems = itemForItemController.getItemsForParentItem(pi.getInvestigation());
            System.out.println("temItems = " + temItems);
            for (Item ti : temItems) {
                if (ti instanceof Investigation) {
                    Investigation tix = (Investigation) ti;
                    if (tix.getMachine() != null && tix.getMachine().getName().toLowerCase().contains("sysmex")) {
                        System.out.println("2. tix.getMachine().getName().toLowerCase() = " + tix.getMachine().getName().toLowerCase());
                        PatientReport tpr = patientReportController.createNewPatientReport(pi, tix);
                        prs.add(tpr);
                    }
                }
            }
            System.out.println("prs = " + prs);
            for (PatientReport tpr : prs) {

                for (PatientReportItemValue priv : tpr.getPatientReportItemValues()) {
                    if (priv.getInvestigationItem() != null && priv.getInvestigationItem().getTest() != null && priv.getInvestigationItem().getIxItemType() == InvestigationItemType.Value) {
                        String test = priv.getInvestigationItem().getTest().getCode().toUpperCase();
                        switch (test) {
                            case "WBC":
                                priv.setStrValue(sysMex.getWbc());
                                break;
                            case "NEUT%":
                                priv.setStrValue(sysMex.getNeutPercentage());
                                break;
                            case "LYMPH%":
                                priv.setStrValue(sysMex.getLymphPercentage());
                                break;
                            case "BASO%":
                                priv.setStrValue(sysMex.getBasoPercentage());
                                break;
                            case "MONO%":
                                priv.setStrValue(sysMex.getMonoPercentage());
                                break;
                            case "EO%":
                                priv.setStrValue(sysMex.getEoPercentage());
                                break;
                            case "RBC":
                                priv.setStrValue(sysMex.getRbc());
                                break;
                            case "HGB":
                                priv.setStrValue(sysMex.getHgb());
                                break;
                            case "HCT":
                                priv.setStrValue(sysMex.getHct());
                                break;
                            case "MCV":
                                priv.setStrValue(sysMex.getMcv());
                                break;
                            case "MCH":
                                priv.setStrValue(sysMex.getMch());
                                break;
                            case "MCHC":
                                priv.setStrValue(sysMex.getMchc());
                                break;
                            case "PLT":
                                priv.setStrValue(sysMex.getPlt());
                                break;

                        }
                    }
                }
                tpr.setDataEntered(true);
                tpr.setDataEntryAt(new Date());
                tpr.setDataEntryComments("Initial Results were taken from Analyzer through Middleware");
                temMsgs += "Patient = " + tpr.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getNameWithTitle() + "\n";
                temMsgs += "Bill No = " + tpr.getPatientInvestigation().getBillItem().getBill().getInsId() + "\n";
                temMsgs += "Investigation = " + tpr.getPatientInvestigation().getInvestigation().getName() + "\n";
                getPrFacade().edit(tpr);
            }
        }

        return "#{success=true|msg=Data Added to LIMS \n" + temMsgs + "}";
    }

    private PatientSample getPatientSampleFromId(Long id) {
        String j = "select ps from PatientSample ps where ps.id = :id";
        Map m = new HashMap();
        m.put("id", id);
        return getPatientSampleFacade().findFirstBySQL(j, m);
    }

    private List<PatientSampleComponant> getPatientSampleComponents(PatientSample ps) {
        String j = "select psc from PatientSampleComponant psc where psc.patientSample = :ps";
        Map m = new HashMap();
        m.put("ps", ps);
        return patientSampleComponantFacade.findBySQL(j, m);
    }

    private List<PatientInvestigation> getPatientInvestigations(List<PatientSampleComponant> pscs) {
        Set<PatientInvestigation> ptixhs = new HashSet<>();
        for (PatientSampleComponant psc : pscs) {
            ptixhs.add(psc.getPatientInvestigation());
        }
        List<PatientInvestigation> ptixs = new ArrayList<>(ptixhs);
        return ptixs;
    }

    public void resetLists() {
        items = null;
        lstToSamle = null;
        lstToReceive = null;
        lstToEnterData = null;
        lstToApprove = null;
        lstToPrint = null;
        selectedToReceive = null;
        investSummery = null;
        lstToReceiveSearch = null;
        toReceive = null;
    }

    public PatientInvestigation getPatientInvestigationFromBillItem(BillItem bi) {
        String j;
        Map m = new HashMap();
        j = "select pi "
                + " from PatientInvestigation pi "
                + " where pi.billItem =:bi "
                + " and pi.retired=false "
                + " order by pi.id";
        m.put("bi", bi);
        PatientInvestigation pi = getFacade().findFirstBySQL(j, m);
        pi.isRetired();
        return pi;
    }

    public boolean sampledForAnyItemInTheBill(Bill bill) {
        //System.out.println("bill = " + bill);
        String jpql;
        jpql = "select pi from PatientInvestigation pi where pi.billItem.bill=:b";
        Map m = new HashMap();
        m.put("b", bill);
        List<PatientInvestigation> pis = getFacade().findBySQL(jpql, m);
        //System.out.println("pis = " + pis);
        for (PatientInvestigation pi : pis) {
            //System.out.println("pi = " + pi);
            if (pi.getCollected() == true || pi.getReceived() == true || pi.getDataEntered() == true) {
                //System.out.println("can not cancel now." );
                return true;
            }
        }
        return false;
    }

    public boolean sampledForBillItem(BillItem billItem) {
        //System.out.println("bill = " + billItem);
        String jpql;
        jpql = "select pi from PatientInvestigation pi where pi.billItem=:b";
        Map m = new HashMap();
        m.put("b", billItem);
        List<PatientInvestigation> pis = getFacade().findBySQL(jpql, m);
        //System.out.println("pis = " + pis);
        for (PatientInvestigation pi : pis) {
            //System.out.println("pi = " + pi);
            if (pi.getCollected() == true || pi.getReceived() == true || pi.getDataEntered() == true) {
                //System.out.println("can not return." );
                return true;
            }
        }
        return false;
    }

    public boolean isListIncludingApproved() {
        return listIncludingApproved;
    }

    public void setListIncludingApproved(boolean listIncludingApproved) {
        this.listIncludingApproved = listIncludingApproved;
        lstToApprove = null;
    }

    public boolean isListIncludingEnteredData() {
        return listIncludingEnteredData;
    }

    public void setListIncludingEnteredData(boolean listIncludingEnteredData) {
        this.listIncludingEnteredData = listIncludingEnteredData;
        lstToEnterData = null;
    }

    public boolean isListIncludingReceived() {
        return listIncludingReceived;
    }

    public void setListIncludingReceived(boolean listIncludingReceived) {
        this.listIncludingReceived = listIncludingReceived;
        lstToReceive = null;
    }

    public List<PatientInvestigation> getSelectedToReceive() {
//        ////System.out.println("selected to receive");
        if (selectedToReceive != null) {
            for (PatientInvestigation pi : selectedToReceive) {
                for (ReportItem ri : pi.getInvestigation().getReportItems()) {
//                    ////System.out.println("ri is " + ri.getName());
                }
            }
        } else {
            selectedToReceive = new ArrayList<>();
        }
        return selectedToReceive;
    }

    public void setSelectedToReceive(List<PatientInvestigation> selectedToReceive) {
        this.selectedToReceive = selectedToReceive;
    }

    public Date getSampledOutsideDate() {
        if (sampledOutsideDate == null) {
            sampledOutsideDate = Calendar.getInstance().getTime();
        }
        return sampledOutsideDate;
    }

    public void setSampledOutsideDate(Date sampledOutsideDate) {
        this.sampledOutsideDate = sampledOutsideDate;
    }

    public boolean isSampledOutSide() {
        return sampledOutSide;
    }

    public void setSampledOutSide(boolean sampledOutSide) {
        this.sampledOutSide = sampledOutSide;
    }

    public List<Investigation> getInvestSummery() {
        if (investSummery == null) {
            String temSql;
            Map temMap = new HashMap();
            temSql = "SELECT inv FROM Investigation inv WHERE inv.id in(SELECT i.investigation.id FROM PatientInvestigation i where i.retired=false and i.collected = false and i.billItem.bill.billDate between :fromDate and :toDate)";
            temMap.put("toDate", getToDate());
            temMap.put("fromDate", getFromDate());
            investSummery = getInvestFacade().findBySQL(temSql, temMap, TemporalType.TIMESTAMP);
        }
        if (investSummery == null) {
            investSummery = new ArrayList<Investigation>();
        }
        return investSummery;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
        resetLists();
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
        resetLists();
    }

    public PatientReportFacade getPrFacade() {
        return prFacade;
    }

    public void setPrFacade(PatientReportFacade prFacade) {
        this.prFacade = prFacade;
    }

    public InvestigationItemFacade getInvestigationItemFacade() {
        return investigationItemFacade;
    }

    public void setInvestigationItemFacade(InvestigationItemFacade investigationItemFacade) {
        this.investigationItemFacade = investigationItemFacade;
    }

    public Investigation getCurrentInvestigation() {
        return currentInvestigation;
    }

    public void setCurrentInvestigation(Investigation currentInvestigation) {
        String sql;
        if (currentInvestigation != null) {

            sql = "select i from InvestigationItem i where i.retired = false and i.item.id = " + currentInvestigation.getId() + " and i.ixItemType = com.divudi.data.InvestigationItemType.Value order by i.cssTop, i.cssLeft";
            setCurrentInvestigationItems(getInvestigationItemFacade().findBySQL(sql));
        } else {
            setCurrentInvestigationItems(new ArrayList<InvestigationItem>());
        }
        this.currentInvestigation = currentInvestigation;
    }

    public List<InvestigationItem> getCurrentInvestigationItems() {
        return currentInvestigationItems;
    }

    public void setCurrentInvestigationItems(List<InvestigationItem> currentInvestigationItems) {
        this.currentInvestigationItems = currentInvestigationItems;
    }

    public List<PatientInvestigation> getSelectedItems() {
        selectedItems = getFacade().findBySQL("select c from PatientInvestigation c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new PatientInvestigation();
    }

    public void setSelectedItems(List<PatientInvestigation> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
        lstToApprove = null;
        lstToSamle = null;
        lstToPrint = null;
        lstToReceiveSearch = null;
    }

    public void saveSelected() {

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        recreateModel();
        this.selectText = selectText;
    }

    public PatientInvestigationFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(PatientInvestigationFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PatientInvestigationController() {
    }

    public PatientInvestigation getCurrent() {
        if (current == null) {
            current = new PatientInvestigation();
        }
        return current;
    }

    List<ReportItem> currentReportItems;
    @EJB
    ReportItemFacade reportItemFacade;

    public ReportItemFacade getReportItemFacade() {
        return reportItemFacade;
    }

    public void setReportItemFacade(ReportItemFacade reportItemFacade) {
        this.reportItemFacade = reportItemFacade;
    }

    public List<ReportItem> getCurrentReportItems() {
        String sql;
        Map m = new HashMap();
        sql = "select i from ReportItem i where i.item=:ix order by i.cssTop";
        m.put("ix", getCurrent().getInvestigation());
        currentReportItems = getReportItemFacade().findBySQL(sql, m);
        return currentReportItems;
    }

    public void setCurrentReportItems(List<ReportItem> currentReportItems) {
        this.currentReportItems = currentReportItems;
    }

    public void setCurrent(PatientInvestigation current) {
        if (current != null) {
            setCurrentInvestigation(current.getInvestigation());
        } else {
            setCurrentInvestigation(null);
        }
        this.current = current;
    }

    private PatientInvestigationFacade getFacade() {
        return ejbFacade;
    }

    public List<PatientInvestigation> getItems() {
        if (items == null) {
            String temSql;
            temSql = "SELECT i FROM PatientInvestigation i where i.retired=false ";
            items = getFacade().findBySQL(temSql);
            if (items == null) {
                items = new ArrayList<>();
            }

        }
        return items;
    }

    public List<PatientInvestigation> getLstToSamle() {
        return lstToSamle;
    }

    public boolean isShowSamplingPagination() {
        if (getLstToSamle().size() > 20) {
            showSamplingPagination = true;
        } else {
            showSamplingPagination = false;
        }
        return showSamplingPagination;
    }

    public void setShowSamplingPagination(boolean showSamplingPagination) {
        this.showSamplingPagination = showSamplingPagination;
    }

    public void sendSms() {
        if (current == null) {
            UtilityController.addErrorMessage("Nothing to send sms");
            return;
        }

        Bill bill = current.getBillItem().getBill();

        System.out.println("running the sending sms.");
        if (bill == null) {
        }

        if (bill == null || bill.getPatient() == null || bill.getPatient().getPerson() == null || bill.getPatient().getPerson().getPhone() == null) {
            return;
        }

        String sendingNo = bill.getPatient().getPerson().getPhone();
        if (sendingNo.contains("077") || sendingNo.contains("076")
                || sendingNo.contains("071") || sendingNo.contains("072")
                || sendingNo.contains("075") || sendingNo.contains("078")) {
        } else {
            return;
        }

        StringBuilder sb = new StringBuilder(sendingNo);
        sb.deleteCharAt(3);
        sendingNo = sb.toString();

        if (getSessionController().getUserPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna) {
            String url = "https://cpsolutions.dialog.lk/index.php/cbs/sms/send?destination=94";
            HttpResponse<String> stringResponse;
            String pw = "&q=14488825498722";
//            String messageBody = "Dear Sir/Madam,\n"
//                    + "Thank you for using RHD services. Report bearing number " + bill.getInsId() + " is ready for collection.\n"
//                    + "\"Ruhunu Hospital Diagnostics your trusted diagnostics partner\"";
//            
//            System.out.println("messageBody = " + messageBody.length());
            String messageBody2 = "Dear Sir/Madam,\n"
                    + "Report bearing number " + bill.getInsId() + " is ready for collection.\n"
                    + "\"RHD your trusted diagnostics partner\"\n"
                    + "Get your report online http://goo.gl/Ae8p6L";

            System.out.println("messageBody2 = " + messageBody2.length());

            final StringBuilder request = new StringBuilder(url);
            request.append(sendingNo.substring(1, 10));
            request.append(pw);
//            request.append(messageBody);
//            System.out.println("request.toString().charAt(105) = " + request.toString().charAt(105));
//            System.out.println("request.toString().charAt(106) = " + request.toString().charAt(106));
//            System.out.println("request.toString().charAt(107) = " + request.toString().charAt(107));
            try {
                System.out.println("pw = " + pw);
                System.out.println("sendingNo = " + sendingNo);
                System.out.println("sendingNo.substring(1, 10) = " + sendingNo.substring(1, 10));
                System.out.println("text = " + messageBody2);

                stringResponse = Unirest.post(request.toString()).field("message", messageBody2).asString();

            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
            sms = new Sms();
//            sms.setUserId(id);
            sms.setPassword(pw);
            sms.setCreatedAt(new Date());
            sms.setCreater(getSessionController().getLoggedUser());
            sms.setBill(bill);
            sms.setSendingUrl(url);
            sms.setSmsType(SmsType.LabReport);
            sms.setSendingMessage(messageBody2);
        } else {
            String url = "http://www.textit.biz/sendmsg/index.php";
            HttpResponse<String> stringResponse;
            String messageBody;
            String id = "94715812399";
            String pw = "5672";

            messageBody = "Reports ready. ";
            messageBody = messageBody + bill.getInstitution().getName() + ". ";
            messageBody = messageBody + bill.getDepartment().getAddress() + ". ";
            messageBody = messageBody + bill.getInstitution().getWeb();

            try {
                System.out.println("id = " + id);
                System.out.println("pw = " + pw);
                System.out.println("sendingNo = " + sendingNo);
                System.out.println("text = " + messageBody);

                stringResponse = Unirest.post(url)
                        .field("id", id)
                        .field("pw", pw)
                        .field("to", sendingNo)
                        .field("text", messageBody)
                        .asString();

                System.out.println("stringResponse = " + stringResponse);

            } catch (Exception ex) {
                return;
            }
            sms = new Sms();
            sms.setUserId(id);
            sms.setPassword(pw);
            sms.setCreatedAt(new Date());
            sms.setCreater(getSessionController().getLoggedUser());
            sms.setBill(bill);
            sms.setSendingUrl(url);
            sms.setSendingMessage(messageBody);
        }

        System.out.println("Updating current PtIx = " + getCurrent());

        System.out.println("SMS status before updating " + getCurrent().getBillItem().getBill().getSmsed());

        getCurrent().getBillItem().getBill().setSmsed(true);
        getCurrent().getBillItem().getBill().setSmsedAt(new Date());
        getCurrent().getBillItem().getBill().setSmsedUser(getSessionController().getLoggedUser());
        getFacade().edit(current);
        getCurrent().getBillItem().getBill().getSentSmses().add(sms);

        System.out.println("SMS status aftr updating " + getCurrent().getBillItem().getBill().getSmsed());

        billFacade.edit(getCurrent().getBillItem().getBill());

        System.out.println("sms before saving = " + sms);
        getSmsFacade().create(sms);
        System.out.println("sms after saving " + sms);

        UtilityController.addSuccessMessage("Sms send");

        getLabReportSearchByInstitutionController().createPatientInvestigaationList();
    }
//    ...............Create PDF.... Jasper.........

    public void create() throws DocumentException, com.lowagie.text.DocumentException {

        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();

        String url = "http://localhost:8080/temp/faces/lab/patient_report_print_email_pfd.xhtml";

        url = req.getRequestURL().toString();
        System.out.println("1" + url);
        url = url.substring(0, url.length() - req.getRequestURI().length()) + req.getContextPath() + "/";
        System.out.println("2" + url);
        url += "faces/lab/patient_report_print_email_pfd.xhtml";
        System.out.println("3" + url);
// 

        try {

            final ITextRenderer iTextRenderer = new ITextRenderer();

            iTextRenderer.setDocument(url);
            iTextRenderer.layout();

            final FileOutputStream fileOutputStream
                    = new FileOutputStream(new File("\\tmp\\LabReport.pdf"));

            iTextRenderer.createPDF(fileOutputStream);
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//    ...............sendEmail...............................................

    public void sendEmail() throws IOException, DocumentException, com.lowagie.text.DocumentException, Exception {

        System.out.println("" + getCurrent());
        System.out.println("" + getCurrent());

        final String username = getCurrent().getBillItem().getBill().getToDepartment().getInstitution().getEmailSendingUsername();
        final String password = getCurrent().getBillItem().getBill().getToDepartment().getInstitution().getEmailSendingPassword();

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
            message.setFrom(new InternetAddress(getCurrent().getBillItem().getBill().getToDepartment().getInstitution().getEmailSendingUsername()));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(getCurrent().getBillItem().getBill().getPatient().getPerson().getEmail()));
            message.setSubject(getCurrent().getInvestigation().getName() + " Results");
            message.setText("Dear " + getCurrent().getBillItem().getBill().getPatient().getPerson().getNameWithTitle()
                    + ",\n\n Please find the results of your " + getCurrent().getInvestigation().getName() + ".");

            //3) create MimeBodyPart object and set your message text     
            BodyPart msbp1 = new MimeBodyPart();
            msbp1.setText("Final Lab report of patient");

            //4) create new MimeBodyPart object and set DataHandler object to this object      
            MimeBodyPart msbp2 = new MimeBodyPart();

            create();

            DataSource source = new FileDataSource("\\tmp\\LabReport.pdf");
            msbp2.setDataHandler(new DataHandler(source));
            msbp2.setFileName("\\tmp\\Labreport.pdf");

            //5) create Multipart object and add Mimdler(soeBodyPart objects to this object      
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(msbp1);
            multipart.addBodyPart(msbp2);

            //6) set the multiplart object to the message object  
            message.setContent(multipart);

            Transport.send(message);

            System.out.println("Send Successfully");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

    }

    public void markAsSampled() {
        if (current == null) {
            UtilityController.addErrorMessage("Nothing to sample");
            return;
        }

        getCurrent().setSampleCollecter(getSessionController().getLoggedUser());
        if (current.getSampleOutside()) {
            getCurrent().setSampledAt(sampledOutsideDate);
        } else {
            getCurrent().setSampledAt(new Date());
            current.setSampleDepartment(getSessionController().getLoggedUser().getDepartment());
            current.setSampleInstitution(getSessionController().getLoggedUser().getInstitution());
        }
        if (getCurrent().getId() != null || getCurrent().getId() != 0) {
            getCurrent().setCollected(Boolean.TRUE);
            getCurrent().setSampleCollecter(getSessionController().getLoggedUser());
            getEjbFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Marked as Sampled");
        } else {
            UtilityController.addErrorMessage("Empty");
        }
        setSampledOutsideDate(Calendar.getInstance().getTime());

        getLabReportSearchByInstitutionController().createPatientInvestigaationList();
    }

    public void revertMarkedSample() {
        if (current == null) {
            UtilityController.addErrorMessage("Nothing to Revert");
            return;
        }
        getCurrent().setSampleCollecter(getSessionController().getLoggedUser());

        if (getCurrent().getId() != null || getCurrent().getId() != 0) {
            getCurrent().setCollected(Boolean.FALSE);
            getCurrent().setReceived(Boolean.FALSE);
            getCurrent().setDataEntered(Boolean.FALSE);
            getCurrent().setSampleCollecter(getSessionController().getLoggedUser());
            getEjbFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Revert Sample Successfully");
        } else {
            UtilityController.addErrorMessage("Empty");
        }
        setSampledOutsideDate(Calendar.getInstance().getTime());

        getLabReportSearchByInstitutionController().createPatientInvestigaationList();
    }

    public void setLstToSamle(List<PatientInvestigation> lstToSamle) {
        this.lstToSamle = lstToSamle;
    }

    public List<PatientInvestigation> getLstToReceive() {
        if (lstToReceive == null) {
            String temSql;
            getCurrent().getSampledAt();
            Map temMap = new HashMap();
            if (listIncludingReceived) {
                temSql = "SELECT i FROM PatientInvestigation i where i.retired=false and i.collected = true and i.sampledAt between :fromDate and :toDate and i.receiveDepartment.id = " + getSessionController().getDepartment().getId();
            } else {
                temSql = "SELECT i FROM PatientInvestigation i where i.retired=false and i.collected = true and i.received=false and i.sampledAt between :fromDate and :toDate and i.receiveDepartment.id = " + getSessionController().getDepartment().getId();
            }
//            ////System.out.println("Sql to get the receive list is " + temSql);
//            ////System.out.println("FromDate to get the receive list is " + getFromDate());
//            ////System.out.println("ToDate to get the receive list is " + getToDate());
            temMap.put("toDate", getToDate());
            temMap.put("fromDate", getFromDate());
            lstToReceive = getFacade().findBySQL(temSql, temMap, TemporalType.TIMESTAMP);
        }
        if (lstToReceive == null) {
            lstToReceive = new ArrayList<>();
        }
        return lstToReceive;
    }
    List<PatientInvestigation> lstToReceiveSearch;

    public List<PatientInvestigation> getLstToReceiveSearch() {
        if (lstToReceiveSearch == null) {
//            ////System.out.println("getting lst to receive search");
            String temSql;
            Map temMap = new HashMap();
            if (selectText == null || selectText.trim().equals("")) {
                temSql = "SELECT i FROM PatientInvestigation i where i.retired=false and i.collected = true and i.sampledAt between :fromDate and :toDate and i.receiveDepartment.id = " + getSessionController().getDepartment().getId();
                temMap.put("toDate", getToDate());
                temMap.put("fromDate", getFromDate());
                lstToReceiveSearch = getFacade().findBySQL(temSql, temMap, TemporalType.TIMESTAMP);
            } else {
                temSql = "select pi from PatientInvestigation pi join pi.investigation i join pi.billItem.bill b join b.patient.person p   where (upper(p.name) like '%" + selectText.toUpperCase() + "%' or upper(b.insId) like '%" + selectText.toUpperCase() + "%' or p.phone like '%" + selectText + "%' or upper(i.name) like '%" + selectText.toUpperCase() + "%' )  and pi.retired=false and b.createdAt between :fromDate and :toDate and pi.receiveDepartment.id = " + getSessionController().getDepartment().getId();
                temMap.put("toDate", getToDate());
                temMap.put("fromDate", getFromDate());
//                ////System.out.println("sql is " + temSql);
                lstToReceiveSearch = getFacade().findBySQL(temSql, temMap, TemporalType.TIMESTAMP);

            }

        }
        if (lstToReceiveSearch == null) {
//            ////System.out.println("lstToReceiveSearch is null");
            lstToReceiveSearch = new ArrayList<PatientInvestigation>();
        }
//        ////System.out.println("size is " + lstToReceiveSearch.size());
        return lstToReceiveSearch;
    }

    public void toCollectSample() {
        prepareToSample();

    }

    public void prepareToSample() {
        String temSql;
        Map temMap = new HashMap();
        temSql = "SELECT i FROM PatientInvestigation i where i.retired=false  and i.collected = false and i.billItem.bill.billDate between :fromDate and :toDate";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        lstToSamle = getFacade().findBySQL(temSql, temMap, TemporalType.TIMESTAMP);
        checkRefundBillItems(lstToSamle);
    }

    public void prepareSampleCollectionByRequest() {
        samplingRequestResponse = "#{";
        if (inputBillId == null || inputBillId.trim().equals("")) {
            samplingRequestResponse += "Login=0|Message=Bill Number not entered.}#";
            return;
        }
        if (!getSessionController().loginForRequests(username, password)) {
            samplingRequestResponse += "Login=0|Message=Username or password error.}#";
            return;
        }
        prepareSampleCollection();
        if (getPatientSamples() == null || getPatientSamples().isEmpty()) {
            samplingRequestResponse += "Login=0|";
            samplingRequestResponse += "Bill Not Found. Pease reenter}";
            return;
        }

        samplingRequestResponse += "Login=1";
        String zplTemplate = "^XA\r\n"
                + "^LH40,10\r\n"
                + "^F010,20,^ADN,18,10^FD#{header}^FS\r\n"
                + "^LH40,30\r\n"
                + "^F010,10,^BCN,100,Y,N,N^FD#{barcode}^FS\r\n"
                + "^LH40,155\r\n"
                + "^F010,20,^ADN,18,10^FD#{footer}^FS\r\n"
                + "^XZ\r\n";
        String ptLabel = "";
        Bill tb;
        tb = patientSamples.get(0).getBill();
        String tbis = "";

        samplingRequestResponse += "|message=";

        if (patientSamplesSet.isEmpty()) {
            ptLabel = zplTemplate;
            ptLabel = ptLabel.replace("#{header}", commonController.shortDate(patientSamples.get(0).getBill().getBillDate())
                    + " "
                    + patientSamples.get(0).getBill().getPatient().getPerson().getName());
            ptLabel = ptLabel.replace("#{barcode}", "" + patientSamples.get(0).getBill().getIdStr());
            List<BillItem> tpiics = patientSamples.get(0).getBill().getBillItems();
            tbis = "";
            for (BillItem i : tpiics) {
                tbis += i.getItem().getName() + ", ";
            }
            tbis = tbis.substring(0, tbis.length() - 2);
            ptLabel = ptLabel.replace("#{footer}", tbis);
            samplingRequestResponse += ptLabel;
        } else {
            for (PatientSample ps : patientSamplesSet) {
                ptLabel = zplTemplate;
                ptLabel = ptLabel.replace("#{header}", commonController.shortDate(ps.getBill().getBillDate())
                        + " "
                        + ps.getPatient().getPerson().getName());
                ptLabel = ptLabel.replace("#{barcode}", "" + ps.getIdStr());
                List<Item> tpiics = testComponantsForPatientSample(ps);
                tbis = "";
                for (Item i : tpiics) {
                    tbis += i.getName() + ", ";
                }
                if (tbis.length() > 2) {
                    tbis = tbis.substring(0, tbis.length() - 2);
                    ptLabel = ptLabel.replace("#{footer}", tbis);
                    samplingRequestResponse += ptLabel;
                }
            }
        }
        samplingRequestResponse += "}#";
    }

    public void prepareSampleCollection() {
        Long billId = stringToLong(inputBillId);
        List<Bill> temBills;
        if (billId != null) {
            temBills = prepareSampleCollectionByBillId(billId);
        } else {
            temBills = prepareSampleCollectionByBillNumber(inputBillId);
        }
        prepareSampleCollectionByBills(temBills);
    }

    public Long stringToLong(String input) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<Bill> prepareSampleCollectionByBillId(Long bill) {
        Bill b = getBillFacade().find(bill);
        List<Bill> bs = billController.validBillsOfBatchBill(b.getBackwardReferenceBill());
        if (bs == null || bs.isEmpty()) {
            JsfUtil.addErrorMessage("Can not find the bill. Please recheck.");
            return null;
        }
        return bs;
    }

    public List<Bill> prepareSampleCollectionByBillNumber(String insId) {
        System.out.println("com.divudi.bean.lab.PatientInvestigationController.prepareSampleCollectionByBillNumber()");
        String j = "Select b from Bill b where b.insId=:id";
        Map m = new HashMap();
        m.put("id", insId);
        Bill b = getBillFacade().findFirstBySQL(j, m);
        System.out.println("b = " + b);
        List<Bill> bs = billController.validBillsOfBatchBill(b.getBackwardReferenceBill());
        System.out.println("b = " + b);
        if (bs == null || bs.isEmpty()) {
            JsfUtil.addErrorMessage("Can not find the bill. Please recheck.");
            return null;
        }
        return bs;
    }

    public void prepareSampleCollectionByBills(List<Bill> bills) {
        System.out.println("prepareSampleCollectionByBills");
        System.out.println("bills = " + bills);
        String j = "";
        Map m;
        patientSamplesSet = new HashSet<>();
        patientSamples = null;
        for (Bill b : bills) {
            System.out.println("b = " + b);
            m = new HashMap();
            m.put("can", false);
            m.put("bill", b);
            j = "Select pi from PatientInvestigation pi "
                    + " where pi.cancelled=:can "
                    + " and pi.billItem.bill=:bill";
            List<PatientInvestigation> pis = getFacade().findBySQL(j, m);
            System.out.println("pis = " + pis);
            for (PatientInvestigation ptix : pis) {
                System.out.println("ptix = " + ptix);
                Investigation ix = ptix.getInvestigation();

                ptix.setCollected(true);
                ptix.setSampleCollecter(getSessionController().getLoggedUser());
                ptix.setSampleDepartment(getSessionController().getLoggedUser().getDepartment());
                ptix.setSampleInstitution(getSessionController().getLoggedUser().getInstitution());
                ptix.setSampledAt(new Date());
                getFacade().edit(ptix);

                List<InvestigationItem> ixis = investigationItemController.getItems(ix);
                System.out.println("ixis = " + ixis);
                for (InvestigationItem ixi : ixis) {
                    System.out.println("ixi = " + ixi);
                    if (ixi.getIxItemType() == InvestigationItemType.Value && ixi.getIxItemValueType() != InvestigationItemValueType.Memo) {
                        j = "select ps from PatientSample ps "
                                + " where ps.tube=:tube "
                                + " and ps.sample=:sample "
                                + " and ps.machine=:machine "
                                + " and ps.patient=:pt "
                                + " and ps.bill=:bill "
                                + " and ps.collected=:ca";
                        m = new HashMap();
                        m.put("tube", ixi.getTube());
                        m.put("sample", ixi.getSample());
                        m.put("machine", ixi.getMachine());
                        m.put("pt", b.getPatient());
                        m.put("bill", b);
                        m.put("ca", false);
                        if (ix.isHasMoreThanOneComponant()) {
                            j += " and ps.investigationComponant=:sc ";
                            m.put("sc", ixi.getSampleComponent());
                        }

                        PatientSample pts = getPatientSampleFacade().findFirstBySQL(j, m);
                        System.out.println("pts = " + pts);
                        if (pts == null) {
                            pts = new PatientSample();
                            pts.setTube(ixi.getTube());
                            pts.setSample(ixi.getSample());
                            if (ix.isHasMoreThanOneComponant()) {
                                pts.setInvestigationComponant(ixi.getSampleComponent());
                            }
                            pts.setMachine(ixi.getMachine());
                            pts.setPatient(b.getPatient());
                            pts.setBill(b);
                            pts.setCreatedAt(new Date());
                            pts.setCreater(sessionController.getLoggedUser());
                            pts.setCollected(false);
                            getPatientSampleFacade().create(pts);
                        }
                        patientSamplesSet.add(pts);

                        PatientSampleComponant ptsc;
                        j = "select ps from PatientSampleComponant ps "
                                + " where ps.patientSample=:pts "
                                + " and ps.bill=:bill "
                                + " and ps.patient=:pt "
                                + " and ps.patientInvestigation=:ptix "
                                + " and ps.investigationComponant=:ixc";
                        m = new HashMap();
                        m.put("pts", pts);
                        m.put("bill", b);
                        m.put("pt", b.getPatient());
                        m.put("ptix", ptix);
                        m.put("ixc", ixi.getSampleComponent());
                        ptsc = getPatientSampleComponantFacade().findFirstBySQL(j, m);
                        System.out.println("ptsc = " + ptsc);
                        if (ptsc == null) {
                            ptsc = new PatientSampleComponant();
                            ptsc.setPatientSample(pts);
                            ptsc.setBill(b);
                            ptsc.setPatient(b.getPatient());
                            ptsc.setPatientInvestigation(ptix);
                            ptsc.setInvestigationComponant(ixi.getSampleComponent());
                            ptsc.setCreatedAt(new Date());
                            ptsc.setCreater(sessionController.getLoggedUser());
                            getPatientSampleComponantFacade().create(ptsc);
                        }
                    }
                }
            }
        }
    }

    public List<Item> testComponantsForPatientSample(PatientSample ps) {
        if (ps == null) {
            return new ArrayList<>();
        }
        List<Item> ts = new ArrayList<>();
        Map m = new HashMap();
        String j = "select ps.investigationComponant from PatientSampleComponant ps "
                + " where ps.patientSample=:pts "
                + " group by ps.investigationComponant";
        m = new HashMap();
        m.put("pts", ps);

        ts = getItemFacade().findBySQL(j, m);
        return ts;
    }

    public List<Item> getCurrentReportComponants(Investigation ix) {
        if (ix == null) {
            JsfUtil.addErrorMessage("Select an investigation");
            return null;
        }
        String j = "select i from Item i where i.itemType=:t and i.parentItem=:m and i.retired=:r order by i.name";
        Map m = new HashMap();
        m.put("t", ItemType.SampleComponent);
        m.put("r", false);
        m.put("m", ix);
        return getItemFacade().findBySQL(j, m);
    }

    private PatientInvestigation patientInvestigationOfBillComponant(List<PatientInvestigation> bcs, BillComponent bc) {
        for (PatientInvestigation pi : bcs) {
            if (pi.getBillComponent() == bc) {
                return pi;
            }
        }
        return null;
    }

    private List<BillComponent> billItemComponents(BillItem bi, Set<BillComponent> bcs) {
        List<BillComponent> bs = new ArrayList<>();
        for (BillComponent b : bcs) {
            if (b.getBillItem() == bi) {
                bs.add(b);
            }
        }
        return bs;
    }

    private List<Bill> patientBills(Patient pt, Set<Bill> bills) {
        List<Bill> bs = new ArrayList<>();
        for (Bill b : bills) {
            if (b.getPatient() == pt) {
                bs.add(b);
            }
        }
        return bs;
    }

    public void prepareSampled() {
        String temSql;
        Map temMap = new HashMap();
        temSql = "SELECT i FROM PatientInvestigation i where i.retired=false  and i.collected = true and i.billItem.bill.billDate between :fromDate and :toDate";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        lstToSamle = getFacade().findBySQL(temSql, temMap, TemporalType.TIMESTAMP);
        checkRefundBillItems(lstToSamle);
    }

    public void checkRefundBillItems(List<PatientInvestigation> pis) {
        for (PatientInvestigation pi : pis) {
            markRefundBillItem(pi);
        }
    }

    public void markRefundBillItem(PatientInvestigation pi) {
        String sql;
        Map m = new HashMap();
        sql = "select bi from BillItem bi "
                + " where bi.referanceBillItem.id=:bi ";
        m.put("bi", pi.getBillItem().getId());
        List<BillItem> bis = billItemFacade.findBySQL(sql, m);
        System.out.println("bis.size() = " + bis.size());
        if (bis.isEmpty()) {
            pi.getBillItem().setTransRefund(false);
        } else {
            pi.getBillItem().setTransRefund(true);
        }
    }

    List<PatientInvestigation> toReceive;

    public List<PatientInvestigation> getToReceive() {
        return toReceive;
    }

    public void toPrintWorksheets() {
        Date startTime = new Date();

        String temSql;
        Map temMap = new HashMap();
        temSql = "SELECT i FROM PatientInvestigation i where "
                + " i.retired=false and i.collected = true "
                + " and (i.received=false or i.received is null) and i.sampledAt between :fromDate "
                + " and :toDate and i.receiveDepartment =:d "
                + " order by i.id";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("d", getSessionController().getDepartment());
//        ////System.out.println("Sql is " + temSql);
        toReceive = getFacade().findBySQL(temSql, temMap, TemporalType.TIMESTAMP);

        commonController.printReportDetails(fromDate, toDate, startTime, "Lab/worksheets(/faces/lab_receive.xhtml)");
    }

    public void markYetToReceiveOnes() {
        selectedToReceive = new ArrayList<>();
        for (PatientInvestigation pi : lstToReceiveSearch) {
            if (pi.getReceived() != true) {
                selectedToReceive.add(pi);
            }
        }
    }

    public void markAsReceived() {

        if (getCurrent().getId() != null || getCurrent().getId() != 0) {
            getCurrent().setReceived(Boolean.TRUE);
            getCurrent().setReceivedAt(new Date());
            getCurrent().setReceivedCollecter(getSessionController().getLoggedUser());
            getEjbFacade().edit(getCurrent());
        }

        getLabReportSearchByInstitutionController().createPatientInvestigaationList();
    }

    public void markSelectedAsReceived() {
//        ////System.out.println("going to mark as received");
        for (PatientInvestigation pi : getSelectedToReceive()) {
            pi.setReceived(Boolean.TRUE);
            pi.setReceivedAt(new Date());
            pi.setReceivedCollecter(getSessionController().getLoggedUser());
            getEjbFacade().edit(pi);
        }
        selectedToReceive = new ArrayList<>();
        listIncludingReceived = false;
        toReceive = null;
        toPrintWorksheets();
    }

    public void setLstToReceive(List<PatientInvestigation> lstToReceive) {
        this.lstToReceive = lstToReceive;
    }

    public List<PatientInvestigation> getLstToEnterData() {
        if (lstToEnterData == null) {
            String temSql;
            Map temMap = new HashMap();
            if (listIncludingEnteredData) {
                temSql = "SELECT i FROM PatientInvestigation i where i.retired=false and i.received=true and i.dataEntered between :fromDate and :toDate and i.performDepartment.id = " + getSessionController().getDepartment().getId();
            } else {
                temSql = "SELECT i FROM PatientInvestigation i where i.retired=false and i.received=true and i.dataEntered=false  and i.sampledAt between :fromDate and :toDate and i.performDepartment.id = " + getSessionController().getDepartment().getId();
            }
//            ////System.out.println(temSql);
            temMap.put("toDate", getToDate());
            temMap.put("fromDate", getFromDate());
            lstToEnterData = getFacade().findBySQL(temSql, temMap, TemporalType.TIMESTAMP);
        }
        if (lstToEnterData == null) {
            lstToEnterData = new ArrayList<PatientInvestigation>();
        }
        return lstToEnterData;
    }

    public void markAsDataEntered() {
        if (getCurrent().getId() != null || getCurrent().getId() != 0) {
            getCurrent().setDataEntered(Boolean.TRUE);
            getCurrent().setDataEntryAt(new Date());
            getCurrent().setDataEntryUser(getSessionController().getLoggedUser());
            getEjbFacade().edit(getCurrent());
        }
    }

    public void setLstToEnterData(List<PatientInvestigation> lstToEnterData) {
        this.lstToEnterData = lstToEnterData;
    }

    public List<PatientReport> getLstToApprove() {
        if (lstToApprove == null) {
            String temSql;
            if (listIncludingApproved == true) {
                temSql = "SELECT i FROM PatientReport i where i.retired=false and i.dataEntered=true and i.dataEntryAt between :fromDate and :toDate";
            } else {
                temSql = "SELECT i FROM PatientReport i where i.retired=false and i.dataEntered=true and i.approved=false and i.dataEntryAt between :fromDate and :toDate";
            }
            Map temMap = new HashMap();
            temMap.put("toDate", getToDate());
            temMap.put("fromDate", getFromDate());
            lstToApprove = getPrFacade().findBySQL(temSql, temMap, TemporalType.TIMESTAMP);
        }
        if (lstToApprove == null) {
            lstToApprove = new ArrayList<PatientReport>();
        }
        return lstToApprove;
    }

    public void markAsApproved() {
        if (getCurrent().getId() != null || getCurrent().getId() != 0) {
            getCurrent().setApproved(Boolean.TRUE);
            getCurrent().setApproveUser(getSessionController().getLoggedUser());
            getCurrent().setApproveAt(new Date());
            getEjbFacade().edit(getCurrent());
        }
    }

    public void setLstToApprove(List<PatientReport> lstToApprove) {
        this.lstToApprove = lstToApprove;
    }

    public List<PatientReport> getLstToPrint() {
//        ////System.out.println("getting lst to print");

        String temSql;
        temSql = "SELECT i FROM PatientReport i";
        lstToPrint = getPrFacade().findBySQL(temSql);

        if (lstToPrint == null) {
            lstToPrint = new ArrayList<PatientReport>();
        }
        return lstToPrint;
    }

    public void markAsPrinted() {
        if (getCurrent().getId() != null || getCurrent().getId() != 0) {
            getCurrent().setPrinted(Boolean.TRUE);
            getCurrent().setPrintingUser(getSessionController().getLoggedUser());
            getCurrent().setPrintingAt(new Date());
            getEjbFacade().edit(getCurrent());
        }
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    public void setLstToPrint(List<PatientReport> lstToPrint) {
        this.lstToPrint = lstToPrint;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public InvestigationFacade getInvestFacade() {
        return investFacade;
    }

    public void setInvestFacade(InvestigationFacade investFacade) {
        this.investFacade = investFacade;
    }

    public boolean isListIncludingSampled() {
        return listIncludingSampled;
    }

    public void setListIncludingSampled(boolean listIncludingSampled) {
        this.listIncludingSampled = listIncludingSampled;

    }

    public InstitutionLabSumeryController getLabReportSearchByInstitutionController() {
        return labReportSearchByInstitutionController;
    }

    public void setLabReportSearchByInstitutionController(InstitutionLabSumeryController labReportSearchByInstitutionController) {
        this.labReportSearchByInstitutionController = labReportSearchByInstitutionController;
    }

    public SmsFacade getSmsFacade() {
        return smsFacade;
    }

    public void setSmsFacade(SmsFacade smsFacade) {
        this.smsFacade = smsFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Sms getSms() {
        return sms;
    }

    public void setSms(Sms sms) {
        this.sms = sms;
    }

    public InvestigationItemController getInvestigationItemController() {
        return investigationItemController;
    }

    public PatientSampleFacade getPatientSampleFacade() {
        return patientSampleFacade;
    }

    public BillController getBillController() {
        return billController;
    }

    public void setBillController(BillController billController) {
        this.billController = billController;
    }

    public String getInputBillId() {
        return inputBillId;
    }

    public void setInputBillId(String inputBillId) {
        this.inputBillId = inputBillId;
    }

    public Set<PatientSample> getPatientSamplesSet() {
        return patientSamplesSet;
    }

    public void setPatientSamplesSet(Set<PatientSample> patientSamplesSet) {
        this.patientSamplesSet = patientSamplesSet;
    }

    public List<PatientSample> getPatientSamples() {
        if (patientSamples == null) {
            patientSamples = new ArrayList<>();
            if (patientSamplesSet != null && !patientSamplesSet.isEmpty()) {
                patientSamples.addAll(patientSamplesSet);
            }
        }
        return patientSamples;
    }

    public void setPatientSamples(List<PatientSample> patientSamples) {
        this.patientSamples = patientSamples;
    }

    public String getSamplingRequestResponse() {
        return samplingRequestResponse;
    }

    public void setSamplingRequestResponse(String samplingRequestResponse) {
        this.samplingRequestResponse = samplingRequestResponse;
    }

    public void setPatientSampleComponantFacade(PatientSampleComponantFacade patientSampleComponantFacade) {
        this.patientSampleComponantFacade = patientSampleComponantFacade;
    }

    public void setPatientSampleFacade(PatientSampleFacade patientSampleFacade) {
        this.patientSampleFacade = patientSampleFacade;
    }

    public void setInvestigationItemController(InvestigationItemController investigationItemController) {
        this.investigationItemController = investigationItemController;
    }

    public PatientSampleComponantFacade getPatientSampleComponantFacade() {
        return patientSampleComponantFacade;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }

    public String getApiResponse() {
        return apiResponse;
    }

    public void setApiResponse(String apiResponse) {
        this.apiResponse = apiResponse;
    }

    public PatientReportController getPatientReportController() {
        return patientReportController;
    }

    public void setPatientReportController(PatientReportController patientReportController) {
        this.patientReportController = patientReportController;
    }

    public ItemForItemController getItemForItemController() {
        return itemForItemController;
    }

    public void setItemForItemController(ItemForItemController itemForItemController) {
        this.itemForItemController = itemForItemController;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public String getShift1() {
        return shift1;
    }

    public void setShift1(String shift1) {
        this.shift1 = shift1;
    }

    public String getShift2() {
        return shift2;
    }

    public void setShift2(String shift2) {
        this.shift2 = shift2;
    }

    /**
     *
     */
    @FacesConverter(forClass = PatientInvestigation.class)
    public static class PatientInvestigationControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PatientInvestigationController controller = (PatientInvestigationController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "patientInvestigationController");
            return controller.getEjbFacade().find(getKey(value));
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
            if (object instanceof PatientInvestigation) {
                PatientInvestigation o = (PatientInvestigation) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PatientInvestigationController.class.getName());
            }
        }
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

}
