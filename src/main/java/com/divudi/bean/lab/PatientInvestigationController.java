package com.divudi.bean.lab;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.BillController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ItemForItemController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.SmsController;

import com.divudi.bean.report.InstitutionLabSumeryController;
import com.divudi.data.InvestigationItemType;
import com.divudi.data.InvestigationReportType;
import com.divudi.data.ItemType;
import com.divudi.data.MessageType;
import com.divudi.data.lab.Dimension;
import com.divudi.data.lab.DimensionTestResult;
import com.divudi.data.lab.SampleRequestType;
import com.divudi.data.lab.SysMexOld;
import com.divudi.data.lab.SysMexAdf1;
import com.divudi.data.lab.SysMexAdf2;

import com.divudi.ejb.SmsManagerEjb;
import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import com.divudi.entity.BillItem;
import com.divudi.entity.Department;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.Sms;
import com.divudi.entity.UserPreference;
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
import com.divudi.facade.PatientReportItemValueFacade;
import com.divudi.facade.PatientSampleComponantFacade;
import com.divudi.facade.PatientSampleFacade;
import com.divudi.facade.ReportItemFacade;
import com.divudi.facade.SmsFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.lab.Analyzer;
import com.divudi.data.lab.BillBarcode;
import com.divudi.data.lab.ListingEntity;
import com.divudi.data.lab.PatientInvestigationStatus;
import com.divudi.data.lab.PatientInvestigationWrapper;
import com.divudi.data.lab.PatientSampleWrapper;
import com.divudi.data.lab.Priority;
import com.divudi.data.lab.SampleTubeLabel;
import com.divudi.data.lab.SearchDateType;
import static com.divudi.data.lab.SearchDateType.ORDERED_DATE;
import static com.divudi.data.lab.SearchDateType.REPORT_AUTHORIZED;
import static com.divudi.data.lab.SearchDateType.REPORT_PRINTED;
import static com.divudi.data.lab.SearchDateType.SAMPLE_ACCEPTED_DATE;
import static com.divudi.data.lab.SearchDateType.SAMPLE_COLLECTED_DATE;
import static com.divudi.data.lab.SearchDateType.SAMPLE_GENERATED_DATE;
import static com.divudi.data.lab.SearchDateType.SAMPLE_SENT_DATE;
import com.divudi.entity.Institution;
import com.divudi.entity.Route;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.entity.lab.Machine;
import com.divudi.entity.lab.Sample;
import com.divudi.java.CommonFunctions;
import com.divudi.ws.lims.Lims;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
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
    private InvestigationFacade investFacade;
    @EJB
    SmsFacade smsFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PatientSampleFacade patientSampleFacade;
    @EJB
    private PatientSampleComponantFacade patientSampleComponantFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private SmsManagerEjb smsManagerEjb;
    @EJB
    private PatientReportItemValueFacade patientReportItemValueFacade;
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
    private BillBeanController billBeanController;
    @Inject
    private InvestigationItemController investigationItemController;
    @Inject
    private BillController billController;
    @Inject
    private PatientReportController patientReportController;
    @Inject
    private ItemForItemController itemForItemController;
    @Inject
    private SmsController smsController;
    @Inject
    private Lims lims;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    /**
     * Class Variables
     */
    List<PatientInvestigation> selectedItems;
    private PatientInvestigation current;
    Investigation currentInvestigation;
    private PatientSample currentPatientSample;
    List<InvestigationItem> currentInvestigationItems;
    private List<PatientInvestigation> items = null;
    private List<Bill> bills = null;
    private List<Bill> selectedBills = null;
    private List<PatientInvestigation> lstToSamle = null;
    private List<PatientInvestigation> lstToReceive = null;
    private List<PatientInvestigation> lstToEnterData = null;
    private List<PatientReport> lstToApprove = null;
    private List<PatientReport> lstToPrint = null;
    private List<PatientInvestigation> lstForSampleManagement = null;
    List<PatientSample> patientSamples;
    private List<PatientSample> selectedPatientSamples;
    String selectText = "";
    private Institution orderedInstitution;
    private Department orderedDepartment;
    private Institution performingInstitution;
    private Department performingDepartment;
    private Institution collectionCenter;
    private Route route;
    private Priority priority;
    private Sample speciman;
    private Long sampleId;
    private String patientName;
    private Sample specimen;
    private String type;
    private String externalDoctor;
    private Machine equipment;
    private Staff referringDoctor;
    private Investigation investigation;
    private Department department;
    private SearchDateType searchDateType;
    private PatientInvestigationStatus patientInvestigationStatus;
    @Temporal(TemporalType.TIME)
    private Date fromDate;
    Date toDate;

    private int activeIndexOfManageInvestigation;

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

    private List<PatientReportItemValue> patientReportItemValues;

    private List<SampleTubeLabel> sampleTubeLabels;

    private List<BillBarcode> billBarcodes;
    private List<BillBarcode> selectedBillBarcodes;

    private ListingEntity listingEntity;

    public String navigateToPrintBarcodeFromMenu() {
        return "/lab/sample_barcode_printing?faces-redirect=true";
    }

    public String navigateToPatientSampelIndex() {
        return "/lab/sample_index?faces-redirect=true";
    }

    public String navigateToPrintBarcodesFromSampellingPage(PatientInvestigation ptIx) {
        if (ptIx == null) {
            JsfUtil.addErrorMessage("Patient Investigation is NOT selected");
            return null;
        }
        if (ptIx.getBillItem() == null) {
            JsfUtil.addErrorMessage("No Bill Item");
            return null;
        }
        if (ptIx.getBillItem().getBill() == null) {
            JsfUtil.addErrorMessage("No Bill");
            return null;
        }
        if (ptIx.getBillItem().getBill().getIdStr() == null) {
            JsfUtil.addErrorMessage("No Bill id sTRING");
            return null;
        }

        inputBillId = ptIx.getBillItem().getBill().getIdStr();
        prepareSampleCollection();
        return "/lab/sample_barcode_printing";
    }

    public void fillPatientReportItemValues() {
        String j = "select v from PatientReportItemValue v "
                + "where v.patientReport.approved=:app "
                + " and (v.investigationItem.ixItemType=:vtv or v.investigationItem.ixItemType=:vtc)"
                + " and  v.patientReport.patientInvestigation.investigation.reportType=:rt";
        Map m = new HashMap();
        m.put("app", true);
        m.put("vtv", InvestigationItemType.Value);
        m.put("vtc", InvestigationItemType.Calculation);
        m.put("rt", InvestigationReportType.General);
        if (false) {
            PatientReportItemValue v = new PatientReportItemValue();
            if (v.getPatientReport().getPatientInvestigation().getInvestigation().getReportType()
                    == InvestigationReportType.General) {

            }
        }
        patientReportItemValues = getPatientReportItemValueFacade().findByJpql(j, m, 10000000);
    }

    public void sentRequestToAnalyzer() {
        if (currentPatientSample == null) {
            JsfUtil.addErrorMessage("Nothing to Add");
            return;
        }
        currentPatientSample.setReadyTosentToAnalyzer(true);
        currentPatientSample.setSentToAnalyzer(false);
        currentPatientSample.setSampleRequestType(SampleRequestType.A);
        getPatientSampleFacade().edit(currentPatientSample);
    }

    public void stopSendingRequestToAnalyzer() {
        if (currentPatientSample == null) {
            JsfUtil.addErrorMessage("Nothing to Add");
            return;
        }
        currentPatientSample.setReadyTosentToAnalyzer(false);
        currentPatientSample.setSentToAnalyzer(false);
        currentPatientSample.setSampleRequestType(SampleRequestType.A);
        getPatientSampleFacade().edit(currentPatientSample);
    }

    public void sentRequestToDeleteToAnalyzer() {
        if (currentPatientSample == null) {
            JsfUtil.addErrorMessage("Nothing to Delete");
            return;
        }
        currentPatientSample.setReadyTosentToAnalyzer(true);
        currentPatientSample.setSentToAnalyzer(false);
        currentPatientSample.setSampleRequestType(SampleRequestType.D);
        getPatientSampleFacade().edit(currentPatientSample);
    }

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
            apiResponse = msgFromDimension();
            return;
        }
    }

    private String msgFromDimension() {
        String temMsgs = "";
        Dimension dim = new Dimension();
        dim.setInputStringBytesSpaceSeperated(msg);
        dim.analyzeReceivedMessage();

        if (dim.getAnalyzerMessageType() == com.divudi.data.lab.MessageType.Poll) {
            PatientSample nps = nextPatientSampleToSendToDimension();
            dim.setLimsPatientSample(nps);
            if (nps == null) {
                dim.setLimsFoundPatientInvestigationToEnterResults(false);
            } else {
                if (nps.getSampleRequestType() == null || nps.getSampleRequestType() == SampleRequestType.D) {
                    dim.setToDeleteSampleRequest(true);
                } else {
                    dim.setToDeleteSampleRequest(false);
                }
                dim.setLimsFoundPatientInvestigationToEnterResults(true);
                dim.setLimsPatientSampleComponants(getPatientSampleComponents(nps));
            }
            dim.prepareResponseForPollMessages();
        } else if (dim.getAnalyzerMessageType() == com.divudi.data.lab.MessageType.QueryMessage) {
            PatientSample nps = patientSampleFromId(dim.getAnalyzerSampleId());

            dim.setLimsPatientSample(nps);
            if (nps == null) {
                dim.setLimsFoundPatientInvestigationToEnterResults(false);
            } else {
                dim.setToDeleteSampleRequest(false);
                dim.setLimsFoundPatientInvestigationToEnterResults(true);
                dim.setLimsPatientSampleComponants(getPatientSampleComponents(nps));
            }
            dim.prepareResponseForQueryMessages();
        } else if (dim.getAnalyzerMessageType() == com.divudi.data.lab.MessageType.ResultMessage) {
            boolean resultAdded = true;
            for (DimensionTestResult r : dim.getAnalyzerTestResults()) {
                boolean tb = addResultsToReportsForDimension(dim.getAnalyzerSampleId(), r.getTestName(), r.getTestResult(), r.getTestUnit(), r.getErrorCode());
                if (tb = false) {
                    resultAdded = false;
                }
            }
            temMsgs = "Results Added " + resultAdded;
            dim.setLimsFoundPatientInvestigationToEnterResults(resultAdded);
            dim.prepareResponseForResultMessages();

        } else if (dim.getAnalyzerMessageType() == com.divudi.data.lab.MessageType.CaliberationResultMessage) {
            dim.prepareResponseForCaliberationResultMessages();
        } else if (dim.getAnalyzerMessageType() == com.divudi.data.lab.MessageType.RequestAcceptance) {
            temMsgs = "#{success=true|}";
            return temMsgs;
        } else {
            temMsgs = "#{success=true|}";
            return temMsgs;
        }

        dim.createResponseString();
        temMsgs = dim.getResponseString();
        temMsgs = "#{success=true|toAnalyzer=" + temMsgs + "}";
        return temMsgs;
    }

    public boolean addResultsToReportsForDimension(String sampleId, String testStr, String result, String unit, String error) {
        //System.out.println("sampleId = " + sampleId);
        //System.out.println("testStr = " + testStr);
        boolean temFlag = false;
        Long sid;
        try {
            sid = Long.parseLong(sampleId);
        } catch (Exception e) {
            sid = 0l;
        }
        PatientSample ps = getPatientSampleFromId(sid);

        if (ps == null) {
            return temFlag;
        }

        List<PatientSampleComponant> pscs = getPatientSampleComponents(ps);

        if (pscs == null) {
            return temFlag;
        }
        List<PatientInvestigation> ptixs = getPatientInvestigations(pscs);
        if (ptixs == null || ptixs.isEmpty()) {
            return temFlag;
        }
        for (PatientInvestigation pi : ptixs) {
            List<PatientReport> prs = new ArrayList<>();
            if (pi.getInvestigation().getMachine() != null && pi.getInvestigation().getMachine().getName().toLowerCase().contains("dim")) {
                PatientReport tpr;
                tpr = patientReportController.getUnapprovedPatientReport(pi);
                if (tpr == null) {
                    tpr = patientReportController.createNewPatientReport(pi, pi.getInvestigation());
                }
                prs.add(tpr);
            }
            for (PatientReport rtpr : prs) {
                for (PatientReportItemValue priv : rtpr.getPatientReportItemValues()) {
                    if (priv.getInvestigationItem() != null && priv.getInvestigationItem().getTest() != null && priv.getInvestigationItem().getIxItemType() == InvestigationItemType.Value) {
                        String test;
                        test = priv.getInvestigationItem().getResultCode();

                        if (test == null || test.trim().equals("")) {
                            test = priv.getInvestigationItem().getTest().getCode().toUpperCase();
                        }

                        if (test.toLowerCase().equals(testStr.toLowerCase())) {
                            if (ps.getInvestigationComponant() == null || priv.getInvestigationItem().getSampleComponent() == null) {
                                priv.setStrValue(result);
                                Double dbl = 0d;
                                try {
                                    dbl = Double.parseDouble(result);
                                } catch (Exception e) {
                                }
                                priv.setDoubleValue(dbl);
                                temFlag = true;
                            } else if (priv.getInvestigationItem().getSampleComponent().equals(ps.getInvestigationComponant())) {
                                priv.setStrValue(result);
                                Double dbl = 0d;
                                try {
                                    dbl = Double.parseDouble(result);
                                } catch (Exception e) {
                                }
                                priv.setDoubleValue(dbl);
                                temFlag = true;
                            } else {
                            }
                        }
                    }
                }
                rtpr.setDataEntered(true);
                rtpr.setDataEntryAt(new Date());
                rtpr.setDataEntryComments("Initial Results were taken from Analyzer through Middleware");
                getPrFacade().edit(rtpr);
            }
        }

        return temFlag;
    }

    public PatientSample patientSampleFromId(String id) {
        Long pid = 0l;
        try {
            pid = Long.parseLong(id);
        } catch (Exception e) {
        }
        return getPatientSampleFacade().find(pid);
    }

    public PatientSample nextPatientSampleToSendToDimension() {
        String j = "select ps from PatientSample ps "
                + "where ps.readyTosentToAnalyzer=true "
                + " and ps.sentToAnalyzer=false "
                + " and (ps.machine.name) like :ma ";

        Map m = new HashMap();
        m.put("ma", "%dimension%");
        PatientSample ps = getPatientSampleFacade().findFirstByJpql(j, m);
        if (ps != null) {
            ps.setReadyTosentToAnalyzer(false);
            ps.setSentToAnalyzer(true);
            getPatientSampleFacade().edit(ps);
        }
        return ps;
    }

    private String msgFromSysmex() {
        String temMsgs = "";
        SysMexOld sysMex = new SysMexOld();
        sysMex.setInputStringBytesSpaceSeperated(msg);

        if (sysMex.getBytes().size() > 189 && sysMex.getBytes().size() < 200) {
            SysMexAdf1 m1 = new SysMexAdf1();
            m1.setInputStringBytesSpaceSeperated(msg);
            if (m1.isCorrectReport()) {
                return "#{success=true|msg=Received Result Format 1 for sample ID " + m1.getSampleId() + "}";
            }
        } else if (sysMex.getBytes().size() > 253 && sysMex.getBytes().size() < 258) {
            SysMexAdf2 m2 = new SysMexAdf2();
            m2.setInputStringBytesSpaceSeperated(msg);
            if (m2.isCorrectReport()) {
                return "#{success=true|msg=Received Result Format 2 for sample ID " + m2.getSampleId() + "}";
            } else {
                return extractDataFromSysMexAdf2(m2);
            }
        }
        return "#{success=false|msg=Wrong Data Communication}";
    }

    public String extractDataFromSysMexAdf2(SysMexAdf2 adf2) {
        String temMsgs = "";
        Long sampleId = adf2.getSampleId();
        PatientSample ps = getPatientSampleFromId(adf2.getSampleId());
        if (ps == null) {
            return "#{success=false|msg=Wrong Sample ID. Please resent results " + sampleId + "}";
        }
        List<PatientSampleComponant> pscs = getPatientSampleComponents(ps);
        if (pscs == null) {
            return "#{success=false|msg=Wrong Sample Components. Please inform developers. Please resent results " + sampleId + "}";
        }
        List<PatientInvestigation> ptixs = getPatientInvestigations(pscs);
        if (ptixs == null || ptixs.isEmpty()) {
            return "#{success=false|msg=Wrong Patient Investigations. Please inform developers. Please resent results " + sampleId + "}";
        }
        for (PatientInvestigation pi : ptixs) {
            List<PatientReport> prs = new ArrayList<>();
            if (pi.getInvestigation().getMachine() != null && pi.getInvestigation().getMachine().getName().toLowerCase().contains("sysmex")) {
                PatientReport tpr = patientReportController.createNewPatientReport(pi, pi.getInvestigation());
                prs.add(tpr);
            }
            List<Item> temItems = itemForItemController.getItemsForParentItem(pi.getInvestigation());
            for (Item ti : temItems) {
                if (ti instanceof Investigation) {
                    Investigation tix = (Investigation) ti;
                    if (tix.getMachine() != null && tix.getMachine().getName().toLowerCase().contains("sysmex")) {
                        PatientReport tpr = patientReportController.createNewPatientReport(pi, tix);
                        prs.add(tpr);
                    }
                }
            }
            for (PatientReport tpr : prs) {

                for (PatientReportItemValue priv : tpr.getPatientReportItemValues()) {
                    if (priv.getInvestigationItem() != null && priv.getInvestigationItem().getTest() != null && priv.getInvestigationItem().getIxItemType() == InvestigationItemType.Value) {
                        String test = priv.getInvestigationItem().getTest().getCode().toUpperCase();
                        switch (test) {
                            case "WBC":
                                priv.setStrValue(adf2.getWbc());
                                break;
                            case "NEUT%":
                                priv.setStrValue(adf2.getNeutPercentage());
                                break;
                            case "LYMPH%":
                                priv.setStrValue(adf2.getLymphPercentage());
                                break;
                            case "BASO%":
                                priv.setStrValue(adf2.getBasoPercentage());
                                break;
                            case "MONO%":
                                priv.setStrValue(adf2.getMonoPercentage());
                                break;
                            case "EO%":
                                priv.setStrValue(adf2.getEoPercentage());
                                break;
                            case "RBC":
                                priv.setStrValue(adf2.getRbc());
                                break;
                            case "HGB":
                                priv.setStrValue(adf2.getHgb());
                                break;
                            case "HCT":
                                priv.setStrValue(adf2.getHct());
                                break;
                            case "MCV":
                                priv.setStrValue(adf2.getMcv());
                                break;
                            case "MCH":
                                priv.setStrValue(adf2.getMch());
                                break;
                            case "MCHC":
                                priv.setStrValue(adf2.getMchc());
                                break;
                            case "PLT":
                                priv.setStrValue(adf2.getPlt());
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
        return getPatientSampleFacade().findFirstByJpql(j, m);
    }

    public List<PatientSampleComponant> getPatientSampleComponents(PatientSample ps) {
        String j = "select psc from PatientSampleComponant psc where psc.patientSample = :ps";
        Map m = new HashMap();
        m.put("ps", ps);
        return patientSampleComponantFacade.findByJpql(j, m);
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
        PatientInvestigation pi = getFacade().findFirstByJpql(j, m);
        pi.isRetired();
        return pi;
    }

    public boolean sampledForAnyItemInTheBill(Bill bill) {
        ////System.out.println("bill = " + bill);
        String jpql;
        jpql = "select pi from PatientInvestigation pi where pi.billItem.bill=:b";
        Map m = new HashMap();
        m.put("b", bill);
        List<PatientInvestigation> pis = getFacade().findByJpql(jpql, m);
        ////System.out.println("pis = " + pis);
        for (PatientInvestigation pi : pis) {
            ////System.out.println("pi = " + pi);
            if (pi.getCollected() == true || pi.getReceived() == true || pi.getDataEntered() == true) {
                ////System.out.println("can not cancel now." );
                return true;
            }
        }
        return false;
    }

    public boolean sampledForBillItem(BillItem billItem) {
        ////System.out.println("bill = " + billItem);
        String jpql;
        jpql = "select pi from PatientInvestigation pi where pi.billItem=:b";
        Map m = new HashMap();
        m.put("b", billItem);
        List<PatientInvestigation> pis = getFacade().findByJpql(jpql, m);
        ////System.out.println("pis = " + pis);
        for (PatientInvestigation pi : pis) {
            ////System.out.println("pi = " + pi);
            if (pi.getCollected() == true || pi.getReceived() == true || pi.getDataEntered() == true) {
                ////System.out.println("can not return." );
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
//        //////System.out.println("selected to receive");
        if (selectedToReceive != null) {
            for (PatientInvestigation pi : selectedToReceive) {
                for (ReportItem ri : pi.getInvestigation().getReportItems()) {
//                    //////System.out.println("ri is " + ri.getName());
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
            investSummery = getInvestFacade().findByJpql(temSql, temMap, TemporalType.TIMESTAMP);
        }
        if (investSummery == null) {
            investSummery = new ArrayList<>();
        }
        return investSummery;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
        resetLists();
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
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
            setCurrentInvestigationItems(getInvestigationItemFacade().findByJpql(sql));
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
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
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
        currentReportItems = getReportItemFacade().findByJpql(sql, m);
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
            JsfUtil.addErrorMessage("Nothing to send sms");
            return;
        }
        Bill bill = current.getBillItem().getBill();
        if (bill == null || bill.getPatient() == null || bill.getPatient().getPerson() == null || bill.getPatient().getPerson().getSmsNumber() == null) {
            JsfUtil.addErrorMessage("System Error");
            return;
        }

        Sms s = new Sms();
        s.setPending(false);
        s.setBill(bill);
        s.setCreatedAt(new Date());
        s.setCreater(sessionController.getLoggedUser());
        s.setDepartment(sessionController.getLoggedUser().getDepartment());
        s.setInstitution(sessionController.getLoggedUser().getInstitution());
        s.setPatientInvestigation(current);

        s.setReceipientNumber(bill.getPatient().getPerson().getSmsNumber());

        String messageBody = "Dear Sir/Madam, "
                + "Reports bearing bill number " + bill.getInsId() + " is ready for collection at "
                + sessionController.getLoggedUser().getDepartment().getPrintingName() + ".";

        s.setSendingMessage(messageBody);
        s.setSentSuccessfully(true);
        s.setSmsType(MessageType.LabReport);
        getSmsFacade().create(s);

        UserPreference ap = sessionController.getApplicationPreference();

        Boolean sent = smsManagerEjb.sendSms(s);

        if (sent) {
            getCurrent().getBillItem().getBill().setSmsed(true);
            getCurrent().getBillItem().getBill().setSmsedAt(new Date());
            getCurrent().getBillItem().getBill().setSmsedUser(getSessionController().getLoggedUser());
            getFacade().edit(current);
            getCurrent().getBillItem().getBill().getSentSmses().add(s);
            billFacade.edit(getCurrent().getBillItem().getBill());
            JsfUtil.addSuccessMessage("Sms send");
        } else {
            JsfUtil.addErrorMessage("Sending SMS Failed.");
        }
    }

    public void markAsSampled() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to sample");
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
            JsfUtil.addSuccessMessage("Marked as Sampled");
        } else {
            JsfUtil.addErrorMessage("Empty");
        }
        setSampledOutsideDate(Calendar.getInstance().getTime());

        getLabReportSearchByInstitutionController().createPatientInvestigaationList();
    }

    public void revertMarkedSample() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to Revert");
            return;
        }
        getCurrent().setSampleCollecter(getSessionController().getLoggedUser());

        if (getCurrent().getId() != null || getCurrent().getId() != 0) {
            getCurrent().setCollected(Boolean.FALSE);
            getCurrent().setReceived(Boolean.FALSE);
            getCurrent().setDataEntered(Boolean.FALSE);
            getCurrent().setSampleCollecter(getSessionController().getLoggedUser());
            getEjbFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Revert Sample Successfully");
        } else {
            JsfUtil.addErrorMessage("Empty");
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
//            //////System.out.println("Sql to get the receive list is " + temSql);
//            //////System.out.println("FromDate to get the receive list is " + getFromDate());
//            //////System.out.println("ToDate to get the receive list is " + getToDate());
            temMap.put("toDate", getToDate());
            temMap.put("fromDate", getFromDate());
            lstToReceive = getFacade().findByJpql(temSql, temMap, TemporalType.TIMESTAMP);
        }
        if (lstToReceive == null) {
            lstToReceive = new ArrayList<>();
        }
        return lstToReceive;
    }
    List<PatientInvestigation> lstToReceiveSearch;

    public List<PatientInvestigation> getLstToReceiveSearch() {
        if (lstToReceiveSearch == null) {
//            //////System.out.println("getting lst to receive search");
            String temSql;
            Map temMap = new HashMap();
            if (selectText == null || selectText.trim().equals("")) {
                temSql = "SELECT i FROM PatientInvestigation i where i.retired=false and i.collected = true and i.sampledAt between :fromDate and :toDate and i.receiveDepartment.id = " + getSessionController().getDepartment().getId();
                temMap.put("toDate", getToDate());
                temMap.put("fromDate", getFromDate());
                lstToReceiveSearch = getFacade().findByJpql(temSql, temMap, TemporalType.TIMESTAMP);
            } else {
                temSql = "select pi from PatientInvestigation pi join pi.investigation i join pi.billItem.bill b join b.patient.person p   where ((p.name) like '%" + selectText.toUpperCase() + "%' or (b.insId) like '%" + selectText.toUpperCase() + "%' or p.phone like '%" + selectText + "%' or (i.name) like '%" + selectText.toUpperCase() + "%' )  and pi.retired=false and b.createdAt between :fromDate and :toDate and pi.receiveDepartment.id = " + getSessionController().getDepartment().getId();
                temMap.put("toDate", getToDate());
                temMap.put("fromDate", getFromDate());
//                //////System.out.println("sql is " + temSql);
                lstToReceiveSearch = getFacade().findByJpql(temSql, temMap, TemporalType.TIMESTAMP);

            }

        }
        if (lstToReceiveSearch == null) {
//            //////System.out.println("lstToReceiveSearch is null");
            lstToReceiveSearch = new ArrayList<PatientInvestigation>();
        }
//        //////System.out.println("size is " + lstToReceiveSearch.size());
        return lstToReceiveSearch;
    }

    @Deprecated
    public String navigateToSampleManagement() {
        prepareToSample();
        return "/lab/sample_management?faces-redirect=true";
    }

    public String navigateToToCollelct() {
//        prepareToSample();
        listPatientInvestigationAwaitingSamplling();
        return "/lab/patient_investigations_to_collect?faces-redirect=true";
    }

    public String navigateToGenerateBarcodes() {
        boolean searchInvestigationsForLoggedInstitution = configOptionApplicationController.getBooleanValueByKey("For Lab Sample Barcode Generation, Search by Ordered Institution", false);
        if (searchInvestigationsForLoggedInstitution) {
            orderedInstitution = sessionController.getInstitution();
        }
        boolean searchInvestigationsForLoggedDepartment = configOptionApplicationController.getBooleanValueByKey("For Lab Sample Barcode Generation, Search by Ordered Department", false);
        if (searchInvestigationsForLoggedDepartment) {
            orderedDepartment = sessionController.getDepartment();
        }
        listBillsToGenerateBarcodes();
        return "/lab/generate_barcode_p?faces-redirect=true";
    }

    public String navigateToSentToLab() {
        return "/lab/sent_to_lab?faces-redirect=true";
    }

//  Navigation Lab sampling at lab for barcode generate page
    public String navigateToGenerateBarcodesForLab() {
        return "/lab/generate_barcodes_for_lab?faces-redirect=true";
    }

    public String navigateToCollectSamples() {
        listBarcodeGeneratedPatientSamplesYetToCollect();
        return "/lab/samples_to_collect?faces-redirect=true";
    }

    public String navigateToAcceptSamples() {
        return "/lab/accept_samples?faces-redirect=true";
    }

    public String navigateToRejectSamples() {
        return "/lab/reject_samples?faces-redirect=true";
    }

    public String navigateToRevertSamples() {
        return "/lab/revert_samples?faces-redirect=true";
    }

    public String navigateToDivideSamples() {
        return "/lab/divide_samples?faces-redirect=true";
    }

    public void prepareToSample() {
        String temSql;
        Map temMap = new HashMap();
        temSql = "SELECT i FROM PatientInvestigation i where i.retired=false  and i.collected = false and i.billItem.bill.billDate between :fromDate and :toDate";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        lstToSamle = getFacade().findByJpql(temSql, temMap, TemporalType.TIMESTAMP);
        checkRefundBillItems(lstToSamle);
    }

    public void generateBarcodesForSelectedBills() {
        System.out.println("generateBarcodesForSelectedBills");
        System.out.println("selectedBillBarcodes = " + selectedBillBarcodes);
        selectedBillBarcodes = new ArrayList<>();
        billBarcodes = new ArrayList<>();
        if (selectedBills == null) {
            JsfUtil.addErrorMessage("No Bills Seelcted");
            return;
        }
        if (selectedBills.isEmpty()) {
            JsfUtil.addErrorMessage("No Bills Seelcted");
            return;
        }
        listingEntity = ListingEntity.BILL_BARCODES;
        for (Bill b : selectedBills) {
            BillBarcode bb = new BillBarcode(b);
            System.out.println("bb = " + b);
            List<Bill> bs = billBeanController.findValidBillsForSampleCollection(b);
            System.out.println("bs = " + bs);
            List<PatientSampleWrapper> psws = new ArrayList<>();
            List<PatientSample> pss = prepareSampleCollectionByBillsForPhlebotomyRoom(bs, sessionController.getLoggedUser());
            System.out.println("pss = " + pss);
            if (pss != null) {
                for (PatientSample ps : pss) {
                    System.out.println("ps = " + ps);
                    PatientSampleWrapper ptsw = new PatientSampleWrapper(ps);
                    psws.add(ptsw);
                }
            }
            for (PatientInvestigationWrapper piw : bb.getPatientInvestigationWrappers()) {
                piw.getPatientInvestigation().setBarcodeGenerated(true);
                piw.getPatientInvestigation().setBarcodeGeneratedAt(new Date());
                piw.getPatientInvestigation().setBarcodeGeneratedBy(sessionController.getLoggedUser());
                ejbFacade.edit(piw.getPatientInvestigation());
            }
            System.out.println("psws = " + psws);
            bb.setPatientSampleWrappers(psws);
            billBarcodes.add(bb);
        }
        selectedBillBarcodes = billBarcodes;
    }

    public void generateBarcodesForSelectedPatientInvestigations() {
        if (selectedItems == null) {
            JsfUtil.addErrorMessage("No Bills Seelcted");
            return;
        }
        if (selectedItems.isEmpty()) {
            JsfUtil.addErrorMessage("No Bills Seelcted");
            return;
        }
        listingEntity = ListingEntity.BILL_BARCODES;
        billBarcodes = new ArrayList<>();
        selectedBillBarcodes = new ArrayList<>();
        Map<Bill, BillBarcode> billBarcodeMap = new HashMap<>();

        for (PatientInvestigation pi : selectedItems) {
            Bill b = pi.getBillItem().getBill();
            BillBarcode nbb = billBarcodeMap.get(b);

            if (nbb == null) {
                nbb = new BillBarcode();
                nbb.setBill(b);
                billBarcodeMap.put(b, nbb);
            }

            PatientInvestigationWrapper piw = new PatientInvestigationWrapper(pi);
            nbb.getPatientInvestigationWrappers().add(piw);
        }

        selectedBillBarcodes = new ArrayList<>(billBarcodeMap.values());

        for (BillBarcode bb : selectedBillBarcodes) {
            List<PatientSampleWrapper> psws = new ArrayList<>();
            prepareSampleCollectionByBillsForPhlebotomyRoom(bb);
            System.out.println("bb = " + bb.getPatientSampleWrappers());
        }

        billBarcodes = selectedBillBarcodes;

    }

    public void collectSamples() {
        if (selectedItems == null) {
            JsfUtil.addErrorMessage("No Bills Seelcted");
            return;
        }
        if (selectedItems.isEmpty()) {
            JsfUtil.addErrorMessage("No Bills Seelcted");
            return;
        }
        listingEntity = ListingEntity.PATIENT_SAMPLES;

        for (PatientInvestigation bi : selectedItems) {

        }

    }

    public void listPatientInvestigationAwaitingSamplling() {
        String temSql;
        Map temMap = new HashMap();
        temSql = "SELECT i FROM PatientInvestigation i where i.retired=false  and i.collected = false and i.billItem.bill.billDate between :fromDate and :toDate";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        lstForSampleManagement = getFacade().findByJpql(temSql, temMap, TemporalType.TIMESTAMP);
    }

    public void listBillsToGenerateBarcodes() {
        String jpql;
        Map params = new HashMap();
        jpql = "SELECT i "
                + " FROM PatientInvestigation i "
                + " where i.retired=:ret  "
                + " and i.barcodeGenerated=:bg "
                + " and i.billItem.bill.billDate between :fromDate and :toDate ";

        if (orderedInstitution != null) {
            jpql += " and i.billItem.bill.institution=:ins ";
            params.put("ins", getOrderedInstitution());
        }

        if (orderedInstitution != null) {
            jpql += " and i.billItem.bill.department=:dep ";
            params.put("dep", getOrderedDepartment());
        }

        jpql += " order by i.id desc";
        params.put("fromDate", getFromDate());
        params.put("toDate", getToDate());
        params.put("ret", false);
        params.put("bg", false);
        billBarcodes = new ArrayList<>();
        List<PatientInvestigation> pis = getFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);
        billBarcodes = createBilBarcodeObjects(pis);
    }

    public void searchBills() {
        listingEntity = ListingEntity.BILLS;
        String jpql;
        Map<String, Object> params = new HashMap<>();

        jpql = "SELECT b FROM Bill b WHERE b.retired = :ret";

        if (searchDateType == null) {
            searchDateType = SearchDateType.ORDERED_DATE;
        }

        switch (searchDateType) {
            case ORDERED_DATE:
                jpql += " AND b.createdAt BETWEEN :fd AND :td";
                params.put("fd", getFromDate());
                params.put("td", getToDate());
                break;
            // Add other cases if necessary, with appropriate fields from the Bill entity
        }

        if (orderedInstitution != null) {
            jpql += " AND b.institution = :orderedInstitution";
            params.put("orderedInstitution", getOrderedInstitution());
        }

        if (orderedDepartment != null) {
            jpql += " AND b.toDepartment = :orderedDepartment";
            params.put("orderedDepartment", getOrderedDepartment());
        }

        if (performingInstitution != null) {
            // Add logic if needed
        }

        if (performingDepartment != null) {
            // Add logic if needed
        }

        if (collectionCenter != null) {
            jpql += " AND (b.collectingCentre = :collectionCenter OR b.fromInstitution = :collectionCenter)";
            params.put("collectionCenter", getCollectionCenter());
        }

        if (route != null) {
            jpql += " AND (b.collectingCentre.route = :route OR b.fromInstitution.route = :route)";
            params.put("route", getRoute());
        }

        if (priority != null) {
            // Add logic if needed
        }

        if (specimen != null) {
            // Add logic if needed
        }

        if (patientName != null && !patientName.trim().isEmpty()) {
            jpql += " AND b.patient.person.name LIKE :patientName";
            params.put("patientName", "%" + getPatientName().trim() + "%");
        }

        if (type != null && !type.trim().isEmpty()) {
            jpql += " AND b.ipOpOrCc = :type";
            params.put("type", getType().trim());
        }

        if (externalDoctor != null && !externalDoctor.trim().isEmpty()) {
            jpql += " AND b.referredByName = :externalDoctor";
            params.put("externalDoctor", getExternalDoctor().trim());
        }

        if (equipment != null) {
            // Add logic if needed
        }

        if (referringDoctor != null) {
            jpql += " AND b.referredBy = :referringDoctor";
            params.put("referringDoctor", getReferringDoctor());
        }

        if (investigation != null) {
            // Add logic if needed
        }

        if (department != null) {
            jpql += " AND b.toDepartment = :department";
            params.put("department", getDepartment());
        }

        if (patientInvestigationStatus != null) {
            // Add logic if needed
        }

        jpql += " ORDER BY b.id DESC";

        params.put("ret", false);

        System.out.println("params = " + params);
        System.out.println("jpql = " + jpql);

        bills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        System.out.println("items = " + bills);
    }

    @Deprecated
    public void searchBills1() {
        listingEntity = ListingEntity.PATIENT_INVESTIGATIONS;
        String jpql;
        Map params = new HashMap();
        jpql = "SELECT i "
                + " FROM PatientInvestigation i "
                + " where i.retired=:ret  "
                + " and i.billItem.bill.billDate between :fromDate and :toDate ";

        if (orderedInstitution != null) {
            jpql += " and i.billItem.bill.institution=:ins ";
            params.put("ins", getOrderedInstitution());
        }

        if (orderedInstitution != null) {
            jpql += " and i.billItem.bill.department=:dep ";
            params.put("dep", getOrderedDepartment());
        }

        jpql += " order by i.id desc";
        params.put("fromDate", getFromDate());
        params.put("toDate", getToDate());
        params.put("ret", false);
        params.put("bg", false);
        billBarcodes = new ArrayList<>();
        List<PatientInvestigation> pis = getFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);
        billBarcodes = createBilBarcodeObjects(pis);
    }

    public void searchPatientInvestigations() {
        System.out.println("searchPatientInvestigations");
        listingEntity = ListingEntity.PATIENT_INVESTIGATIONS;
        String jpql;
        Map<String, Object> params = new HashMap<>();

        jpql = "SELECT i "
                + " FROM PatientInvestigation i "
                + " WHERE i.retired = :ret ";

        if (searchDateType == null) {
            searchDateType = SearchDateType.ORDERED_DATE;
        }

        switch (searchDateType) {
            case ORDERED_DATE:
                jpql += " AND i.billItem.bill.createdAt BETWEEN :fd AND :td ";
                params.put("fd", getFromDate());
                params.put("td", getToDate());
                break;
            case REPORT_AUTHORIZED:
                jpql += " AND i.approveAt BETWEEN :fd AND :td ";
                params.put("fd", getFromDate());
                params.put("td", getToDate());
                break;
            case REPORT_PRINTED:
                jpql += " AND i.printingAt BETWEEN :fd AND :td ";
                params.put("fd", getFromDate());
                params.put("td", getToDate());
                break;
            case SAMPLE_ACCEPTED_DATE:
                jpql += " AND i.sampleAcceptedAt BETWEEN :fd AND :td ";
                params.put("fd", getFromDate());
                params.put("td", getToDate());
                break;
            case SAMPLE_COLLECTED_DATE:
                jpql += " AND i.sampleCollectedAt BETWEEN :fd AND :td ";
                params.put("fd", getFromDate());
                params.put("td", getToDate());
                break;
            case SAMPLE_GENERATED_DATE:
                jpql += " AND i.sampleGeneratedAt BETWEEN :fd AND :td ";
                params.put("fd", getFromDate());
                params.put("td", getToDate());
                break;
            case SAMPLE_SENT_DATE:
                jpql += " AND i.sampleSentAt BETWEEN :fd AND :td ";
                params.put("fd", getFromDate());
                params.put("td", getToDate());
                break;
        }

        if (orderedInstitution != null) {
            jpql += " AND i.billItem.bill.institution = :orderedInstitution ";
            params.put("orderedInstitution", getOrderedInstitution());
        }

        if (orderedDepartment != null) {
            jpql += " AND i.billItem.bill.department = :orderedDepartment ";
            params.put("orderedDepartment", getOrderedDepartment());
        }

        if (performingInstitution != null) {
            jpql += " AND i.performInstitution = :peformingInstitution ";
            params.put("peformingInstitution", getPerformingInstitution());
        }

        if (performingDepartment != null) {
            jpql += " AND i.performDepartment = :peformingDepartment ";
            params.put("peformingDepartment", getPerformingDepartment());
        }

        if (collectionCenter != null) {
            jpql += " AND (i.billItem.bill.collectingCentre = :collectionCenter OR i.billItem.bill.fromInstitution = :collectionCenter) ";
            params.put("collectionCenter", getCollectionCenter());
        }

        if (route != null) {
            jpql += " AND (i.billItem.bill.collectingCentre.route = :route OR i.billItem.bill.fromInstitution.route = :route) ";
            params.put("route", getRoute());
        }

        if (priority != null) {
            jpql += " AND i.billItem.priority = :priority ";
            params.put("priority", getPriority());
        }

        if (specimen != null) {
            jpql += " AND i.investigation.sample = :specimen ";
            params.put("specimen", getSpecimen());
        }

        if (patientName != null && !patientName.trim().isEmpty()) {
            jpql += " AND i.billItem.bill.patient.person.name LIKE :patientName ";
            params.put("patientName", "%" + getPatientName().trim() + "%");
        }

        if (type != null && !type.trim().isEmpty()) {
            jpql += " AND i.billItem.bill.ipOpOrCC = :type ";
            params.put("type", getType().trim());
        }

        if (externalDoctor != null && !externalDoctor.trim().isEmpty()) {
            jpql += " AND i.billItem.bill.referredByName = :externalDoctor ";
            params.put("externalDoctor", getExternalDoctor().trim());
        }

        if (equipment != null) {
            jpql += " AND i.investigation.machine = :equipment ";
            params.put("equipment", getEquipment());
        }

        if (referringDoctor != null) {
            jpql += " AND i.billItem.bill.referringDoctor = :referringDoctor ";
            params.put("referringDoctor", getReferringDoctor());
        }

        if (investigation != null) {
            jpql += " AND i.investigation = :investigation ";
            params.put("investigation", getInvestigation());
        }

        if (department != null) {
            jpql += " AND i.billItem.bill.toDepartment = :department ";
            params.put("department", getDepartment());
        }

        if (patientInvestigationStatus != null) {
            jpql += " AND i.status = :patientInvestigationStatus ";
            params.put("patientInvestigationStatus", getPatientInvestigationStatus());
        }

        jpql += " ORDER BY i.id DESC";

        params.put("ret", false);

        System.out.println("params = " + params);
        System.out.println("jpql = " + jpql);

        items = getFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);
        System.out.println("items = " + items);
    }

    public void searchPatientSamples() {
        System.out.println("searchPatientInvestigations");
        listingEntity = ListingEntity.PATIENT_SAMPLES;
        String jpql;
        Map<String, Object> params = new HashMap<>();

        jpql = "SELECT ps FROM PatientSample ps JOIN ps.bill b WHERE ps.retired = :ret";

        if (searchDateType == null) {
            searchDateType = SearchDateType.ORDERED_DATE;
        }

        switch (searchDateType) {
            case ORDERED_DATE:
                jpql += " AND b.createdAt BETWEEN :fd AND :td";
                params.put("fd", getFromDate());
                params.put("td", getToDate());
                break;
            case SAMPLE_ACCEPTED_DATE:
                jpql += " AND ps.sampleReceivedAtLabAt BETWEEN :fd AND :td";
                params.put("fd", getFromDate());
                params.put("td", getToDate());
                break;
            case SAMPLE_COLLECTED_DATE:
                jpql += " AND ps.sampleCollectedAt BETWEEN :fd AND :td";
                params.put("fd", getFromDate());
                params.put("td", getToDate());
                break;
            case SAMPLE_GENERATED_DATE:
                jpql += " AND ps.createdAt BETWEEN :fd AND :td";
                params.put("fd", getFromDate());
                params.put("td", getToDate());
                break;
            case SAMPLE_SENT_DATE:
                jpql += " AND ps.sampleSentAt BETWEEN :fd AND :td";
                params.put("fd", getFromDate());
                params.put("td", getToDate());
                break;
            // Add other cases as needed
        }

        if (orderedInstitution != null) {
            jpql += " AND b.institution = :orderedInstitution";
            params.put("orderedInstitution", getOrderedInstitution());
        }

        if (orderedDepartment != null) {
            jpql += " AND b.department = :orderedDepartment";
            params.put("orderedDepartment", getOrderedDepartment());
        }

        if (performingInstitution != null) {
            jpql += " AND ps.institution = :performingInstitution";
            params.put("performingInstitution", getPerformingInstitution());
        }

        if (performingDepartment != null) {
            jpql += " AND ps.department = :performingDepartment";
            params.put("performingDepartment", getPerformingDepartment());
        }

        if (collectionCenter != null) {
            jpql += " AND (b.collectingCentre = :collectionCenter OR b.fromInstitution = :collectionCenter)";
            params.put("collectionCenter", getCollectionCenter());
        }

        if (route != null) {
            jpql += " AND (b.collectingCentre.route = :route OR b.fromInstitution.route = :route)";
            params.put("route", getRoute());
        }

        if (priority != null) {
            jpql += " AND ps.priority = :priority";
            params.put("priority", getPriority());
        }

        if (specimen != null) {
            jpql += " AND ps.sample = :specimen";
            params.put("specimen", getSpecimen());
        }

        if (patientName != null && !patientName.trim().isEmpty()) {
            jpql += " AND b.patient.person.name LIKE :patientName";
            params.put("patientName", "%" + getPatientName().trim() + "%");
        }

        if (type != null && !type.trim().isEmpty()) {
            jpql += " AND b.ipOpOrCc = :type";
            params.put("type", getType().trim());
        }

        if (externalDoctor != null && !externalDoctor.trim().isEmpty()) {
            jpql += " AND b.referredByName = :externalDoctor";
            params.put("externalDoctor", getExternalDoctor().trim());
        }

        if (equipment != null) {
            // Add logic if needed
        }

        if (referringDoctor != null) {
            jpql += " AND b.referringDoctor = :referringDoctor";
            params.put("referringDoctor", getReferringDoctor());
        }

        if (investigation != null) {
            // Add logic if needed
        }

        if (department != null) {
            jpql += " AND ps.department = :department";
            params.put("department", getDepartment());
        }

        if (patientInvestigationStatus != null) {
            jpql += " AND ps.status = :status";
            params.put("status", getPatientInvestigationStatus());
        }

        jpql += " ORDER BY ps.id DESC";

        params.put("ret", false);

        System.out.println("params = " + params);
        System.out.println("jpql = " + jpql);

        patientSamples = patientSampleFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        System.out.println("patientSamples = " + patientSamples);
    }

    public void listBillsWithGeneratedBarcodes() {
        String jpql;
        Map params = new HashMap();
        jpql = "SELECT i "
                + " FROM PatientInvestigation i "
                + " where i.retired=:ret  "
                + " and i.barcodeGenerated=:bg "
                + " and i.billItem.bill.billDate between :fromDate and :toDate ";

        if (orderedInstitution != null) {
            jpql += " and i.billItem.bill.institution=:ins ";
            params.put("ins", getOrderedInstitution());
        }

        if (orderedInstitution != null) {
            jpql += " and i.billItem.bill.department=:dep ";
            params.put("dep", getOrderedDepartment());
        }

        jpql += " order by i.id desc";
        params.put("fromDate", getFromDate());
        params.put("toDate", getToDate());
        params.put("ret", false);
        params.put("bg", true);
        billBarcodes = new ArrayList<>();
        List<PatientInvestigation> pis = getFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);
        billBarcodes = createBilBarcodeObjects(pis);
    }

    private List<BillBarcode> createBilBarcodeObjects(List<PatientInvestigation> ptis) {
        Map<Bill, BillBarcode> billBarcodeMap = new HashMap<>();
        for (PatientInvestigation pi : ptis) {
            Bill b = pi.getBillItem().getBill();
            BillBarcode bb;

            pi.setStatus(PatientInvestigationStatus.SAMPLE_GENERATED);
            pi.setBarcodeGenerated(true);
            pi.setBarcodeGeneratedAt(new Date());
            pi.setBarcodeGeneratedBy(sessionController.getLoggedUser());
            ejbFacade.edit(pi);

            if (billBarcodeMap.containsKey(b)) {
                bb = billBarcodeMap.get(b);
            } else {
                bb = new BillBarcode(b);
                billBarcodeMap.put(b, bb);
            }

            PatientInvestigationWrapper piw = new PatientInvestigationWrapper(pi);
            bb.getPatientInvestigationWrappers().add(piw);
        }

        return new ArrayList<>(billBarcodeMap.values());
    }

    public void listPatientSamples() {
        String jpql = "select ps from PatientSample ps"
                + " where ps.sampleInstitution=:ins "
                + " and ps.sampledAt between :fd and :td "
                + " order by ps.id";
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("ins", sessionController.getLoggedUser().getInstitution());
        patientSamples = getPatientSampleFacade().findByJpql(jpql, m, TemporalType.TIMESTAMP);
        /**
         *
         * ps.setSampleDepartment(sessionController.getLoggedUser().getDepartment());
         * ps.setSampleInstitution(sessionController.getLoggedUser().getInstitution());
         * ps.setSampledAt(ps.getCreatedAt());
         * ps.setSampleCollecter(ps.getCreater());
         */

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
                + "^LH170,10\r\n" // was 150
                + "^FO30,20,^ADN,18,10^FD#{header}^FS\r\n" // was 10
                + "^LH170,30\r\n" // was 150
                + "^FO30,10,^BCN,100,Y,N,N^FD#{barcode}^FS\r\n" // was 10
                + "^LH170,155\r\n" // was 150
                + "^FO30,20,^ADN,18,10^FD#{footer}^FS\r\n" // was 10
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
        try {
            String barcodeJson = lims.generateSamplesForInternalUse(inputBillId, sessionController.getLoggedUser());
            // Parse the string into a JSON object
            JSONObject root = new JSONObject(barcodeJson);

            // Get the "Barcodes" JSON array from the JSON object
            JSONArray barcodes = root.getJSONArray("Barcodes");
            String defaultTemplate = "<div style='width: 5cm; height:2.5cm; padding: 8px; border: 1px solid #000; font-family: Arial, sans-serif; font-size: 11px;'>"
                    + "<div style='margin: 2px 0;'>"
                    + " Name: " + "{name} ({age}) <br/>"
                    + " Gender: " + "{sex} <br/>"
                    + " Date: " + "{billDate} <br/>"
                    + "</div>"
                    + "<div style='font-family: \"Libre Barcode 128\", cursive; font-size: 24px; margin: 5px 0;'>" + "{barcode}" + "</div>"
                    + "<div style='margin: 2px 0;'>"
                    + "Tests: " + "{tests} <br/>"
                    + "Ins ID: " + "{insId} <br/>"
                    + "Dept ID: " + "{deptId}" + "</div>"
                    + "</div>";
            String templateAbove = configOptionApplicationController.getLongTextValueByKey("Template for Sample Tube Sticker Printing - Above Barcode", defaultTemplate);
            String templateBelow = configOptionApplicationController.getLongTextValueByKey("Template for Sample Tube Sticker Printing - Below Barcode", "");
            sampleTubeLabels = new ArrayList<>();
            for (int i = 0; i < barcodes.length(); i++) {
                // Get each object in the array and convert it to a string
                JSONObject singleBarcode = barcodes.getJSONObject(i);
                SampleTubeLabel stl = new SampleTubeLabel();
                stl.setTextAboveBarcode(createLabelsFromJsonAndTemplate(templateAbove, singleBarcode));
                stl.setTextBelowBarcode(createLabelsFromJsonAndTemplate(templateBelow, singleBarcode));
                stl.setBarcode(singleBarcode.optString("barcode", "N/A"));
                sampleTubeLabels.add(stl);
            }
        } catch (Exception e) {
            // Handle the exception appropriately

        }
    }

    public String createLabelsFromJsonAndTemplate(String template, JSONObject singleBarcode) {
        // Assuming template string contains placeholders like {name}, {barcode}, etc.
        // You need to replace these placeholders with actual values from the JSON object
        String label = template
                .replace("{name}", singleBarcode.optString("name", "N/A")) // Use 'optString' to handle missing keys
                .replace("{billDate}", singleBarcode.optString("billDate", "N/A"))
                .replace("{sex}", singleBarcode.optString("sex", "N/A"))
                .replace("{age}", singleBarcode.optString("age", "N/A"))
                .replace("{barcode}", singleBarcode.optString("barcode", "N/A"))
                .replace("{tests}", singleBarcode.optString("tests", "N/A"))
                .replace("{insId}", singleBarcode.optString("insid", "N/A")) // Assumed JSON key as 'insid'
                .replace("{deptId}", singleBarcode.optString("deptid", "N/A"));

        return label;
    }

    public Long stringToLong(String input) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Deprecated
    public List<Bill> prepareSampleCollectionByBillId(Long bill) {
        Bill b = getBillFacade().find(bill);
        List<Bill> bs = billController.validBillsOfBatchBill(b.getBackwardReferenceBill());
        if (bs == null || bs.isEmpty()) {
            JsfUtil.addErrorMessage("Can not find the bill. Please recheck.");
            return null;
        }
        return bs;
    }

    @Deprecated
    public List<Bill> prepareSampleCollectionByBillNumber(String insId) {
        String j = "Select b from Bill b where b.insId=:id";
        Map m = new HashMap();
        m.put("id", insId);
        Bill b = getBillFacade().findFirstByJpql(j, m);
        List<Bill> bs = billController.validBillsOfBatchBill(b.getBackwardReferenceBill());
        if (bs == null || bs.isEmpty()) {
            JsfUtil.addErrorMessage("Can not find the bill. Please recheck.");
            return null;
        }
        return bs;
    }

    public void temUpdatePatientSamplesToMatchCurrentDepartmentAndInstitution() {
        List<PatientSample> pss = getPatientSampleFacade().findAll();
        for (PatientSample ps : pss) {
            ps.setSampleCollectedDepartment(sessionController.getLoggedUser().getDepartment());
            ps.setSampleCollectedInstitution(sessionController.getLoggedUser().getInstitution());
            ps.setSampleCollectedAt(ps.getCreatedAt());
            ps.setSampleCollecter(ps.getCreater());
            getPatientSampleFacade().edit(ps);
        }
    }

    @Deprecated
    public void prepareSampleCollectionByBills(List<Bill> bills) {
        String j = "";
        Map m;
        patientSamplesSet = new HashSet<>();
        patientSamples = null;
        for (Bill b : bills) {
            m = new HashMap();
            m.put("can", false);
            m.put("bill", b);
            j = "Select pi from PatientInvestigation pi "
                    + " where pi.cancelled=:can "
                    + " and pi.billItem.bill=:bill";
            List<PatientInvestigation> pis = getFacade().findByJpql(j, m);
            for (PatientInvestigation ptix : pis) {
                Investigation ix = ptix.getInvestigation();

                ptix.setCollected(true);
                ptix.setSampleCollecter(getSessionController().getLoggedUser());
                ptix.setSampleDepartment(getSessionController().getLoggedUser().getDepartment());
                ptix.setSampleInstitution(getSessionController().getLoggedUser().getInstitution());
                ptix.setSampledAt(new Date());
                getFacade().edit(ptix);

                List<InvestigationItem> ixis = investigationItemController.getItems(ix);
                for (InvestigationItem ixi : ixis) {
                    if (ixi.getIxItemType() == InvestigationItemType.Value || ixi.getIxItemType() == InvestigationItemType.Template) {
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

                        PatientSample pts = getPatientSampleFacade().findFirstByJpql(j, m);
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
                            pts.setSampleCollectedDepartment(sessionController.getLoggedUser().getDepartment());
                            pts.setSampleCollectedInstitution(sessionController.getLoggedUser().getInstitution());
                            pts.setSampleCollecter(sessionController.getLoggedUser());
                            pts.setSampleCollectedAt(new Date());
                            pts.setCreatedAt(new Date());
                            pts.setCreater(sessionController.getLoggedUser());
                            pts.setSampleCollected(false);
                            pts.setReadyTosentToAnalyzer(false);
                            pts.setSentToAnalyzer(false);
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
                        ptsc = getPatientSampleComponantFacade().findFirstByJpql(j, m);
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

        ts = getItemFacade().findByJpql(j, m);
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
        return getItemFacade().findByJpql(j, m);
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
        lstToSamle = getFacade().findByJpql(temSql, temMap, TemporalType.TIMESTAMP);
        checkRefundBillItems(lstToSamle);
    }

    public void listPatientInvestigationsWhereSamplingCompleting() {
        String temSql;
        Map temMap = new HashMap();
        temSql = "SELECT i FROM PatientInvestigation i where i.retired=false  and i.collected = true and i.billItem.bill.billDate between :fromDate and :toDate";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        lstForSampleManagement = getFacade().findByJpql(temSql, temMap, TemporalType.TIMESTAMP);
    }

    public void listBarcodeGeneratedPatientSamplesYetToCollect() {
        patientSamples = findGeneratedSamplesYetToCollect();
    }

    public void listBarcodeGeneratedPatientSamplesCollected() {
        patientSamples = findGeneratedSamplesCollected();
    }

    public List<PatientSample> findGeneratedSamplesYetToCollect() {
        return findGeneratedSamples(true, false, fromDate, toDate);
    }

    public List<PatientSample> findGeneratedSamplesCollected() {
        return findGeneratedSamples(true, true, fromDate, toDate);
    }

    public List<PatientSample> findGeneratedSamples(boolean barcodeGenerated, boolean sampleCollected, Date fromDate, Date toDate) {
        List<PatientSample> pss;
        Map params = new HashMap<>();
        String jpql = "SELECT ps FROM PatientSample ps WHERE ps.retired=:ret AND ps.barcodeGenerated=:bg AND ps.sampleCollected=:sc AND ps.barcodeGeneratedAt between :fromDate and :toDate";
        params.put("ret", false);
        params.put("bg", barcodeGenerated);
        params.put("sc", sampleCollected);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);
        System.out.println("jpql = " + jpql);
        System.out.println("params = " + params);
        pss = patientSampleFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        System.out.println("pss = " + pss);
        return pss;
    }

    public void listComplletedSamples() {
        String jpql;
        Map params = new HashMap();
        jpql = "SELECT i "
                + "FROM PatientSample i "
                + " where i.retired=false "
                + " and i.collected = true "
                + " and i.billItem.bill.billDate between :fromDate and :toDate";
        params.put("toDate", getToDate());
        params.put("fromDate", getFromDate());
        lstForSampleManagement = getFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);
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
        List<BillItem> bis = billItemFacade.findByJpql(sql, m);
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
//        //////System.out.println("Sql is " + temSql);
        toReceive = getFacade().findByJpql(temSql, temMap, TemporalType.TIMESTAMP);

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
//        //////System.out.println("going to mark as received");
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
//            //////System.out.println(temSql);
            temMap.put("toDate", getToDate());
            temMap.put("fromDate", getFromDate());
            lstToEnterData = getFacade().findByJpql(temSql, temMap, TemporalType.TIMESTAMP);
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
            lstToApprove = getPrFacade().findByJpql(temSql, temMap, TemporalType.TIMESTAMP);
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
//        //////System.out.println("getting lst to print");

        String temSql;
        temSql = "SELECT i FROM PatientReport i";
        lstToPrint = getPrFacade().findByJpql(temSql);

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
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
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

    public PatientSample getCurrentPatientSample() {
        return currentPatientSample;
    }

    public void setCurrentPatientSample(PatientSample currentPatientSample) {
        this.currentPatientSample = currentPatientSample;
    }

    public SmsManagerEjb getSmsManagerEjb() {
        return smsManagerEjb;
    }

    public void setSmsManagerEjb(SmsManagerEjb smsManagerEjb) {
        this.smsManagerEjb = smsManagerEjb;
    }

    public SmsController getSmsController() {
        return smsController;
    }

    public List<PatientReportItemValue> getPatientReportItemValues() {
        return patientReportItemValues;
    }

    public void setPatientReportItemValues(List<PatientReportItemValue> patientReportItemValues) {
        this.patientReportItemValues = patientReportItemValues;
    }

    public PatientReportItemValueFacade getPatientReportItemValueFacade() {
        return patientReportItemValueFacade;
    }

    public int getActiveIndexOfManageInvestigation() {
        return activeIndexOfManageInvestigation;
    }

    public void setActiveIndexOfManageInvestigation(int activeIndexOfManageInvestigation) {
        this.activeIndexOfManageInvestigation = activeIndexOfManageInvestigation;
    }

    public List<SampleTubeLabel> getSampleTubeLabels() {
        return sampleTubeLabels;
    }

    public void setSampleTubeLabels(List<SampleTubeLabel> sampleTubeLabels) {
        this.sampleTubeLabels = sampleTubeLabels;
    }

    public Lims getLims() {
        return lims;
    }

    public void setLims(Lims lims) {
        this.lims = lims;
    }

    public List<PatientInvestigation> getLstForSampleManagement() {
        return lstForSampleManagement;
    }

    public void setLstForSampleManagement(List<PatientInvestigation> lstForSampleManagement) {
        this.lstForSampleManagement = lstForSampleManagement;
    }

    public List< BillBarcode> getBillBarcodes() {
        return billBarcodes;
    }

    public void setBillBarcodes(List< BillBarcode> billBarcodes) {
        this.billBarcodes = billBarcodes;
    }

    public List<BillBarcode> getSelectedBillBarcodes() {
        return selectedBillBarcodes;
    }

    public void setSelectedBillBarcodes(List<BillBarcode> selectedBillBarcodes) {
        this.selectedBillBarcodes = selectedBillBarcodes;
    }

    public Institution getOrderedInstitution() {
        return orderedInstitution;
    }

    public void setOrderedInstitution(Institution orderedInstitution) {
        this.orderedInstitution = orderedInstitution;
    }

    public Department getOrderedDepartment() {
        return orderedDepartment;
    }

    public void setOrderedDepartment(Department orderedDepartment) {
        this.orderedDepartment = orderedDepartment;
    }

    public Institution getPerformingInstitution() {
        return performingInstitution;
    }

    public void setPerformingInstitution(Institution performingInstitution) {
        this.performingInstitution = performingInstitution;
    }

    public Department getPerformingDepartment() {
        return performingDepartment;
    }

    public void setPerformingDepartment(Department performingDepartment) {
        this.performingDepartment = performingDepartment;
    }

    public SearchDateType getSearchDateType() {
        return searchDateType;
    }

    public void setSearchDateType(SearchDateType searchDateType) {
        this.searchDateType = searchDateType;
    }

    public PatientInvestigationStatus getPatientInvestigationStatus() {
        return patientInvestigationStatus;
    }

    public void setPatientInvestigationStatus(PatientInvestigationStatus patientInvestigationStatus) {
        this.patientInvestigationStatus = patientInvestigationStatus;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public BillBeanController getBillBeanController() {
        return billBeanController;
    }

    public void setBillBeanController(BillBeanController billBeanController) {
        this.billBeanController = billBeanController;
    }

    public ConfigOptionApplicationController getConfigOptionApplicationController() {
        return configOptionApplicationController;
    }

    public void setConfigOptionApplicationController(ConfigOptionApplicationController configOptionApplicationController) {
        this.configOptionApplicationController = configOptionApplicationController;
    }

    public Institution getCollectionCenter() {
        return collectionCenter;
    }

    public void setCollectionCenter(Institution collectionCenter) {
        this.collectionCenter = collectionCenter;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Sample getSpeciman() {
        return speciman;
    }

    public void setSpeciman(Sample speciman) {
        this.speciman = speciman;
    }

    public Long getSampleId() {
        return sampleId;
    }

    public void setSampleId(Long sampleId) {
        this.sampleId = sampleId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Sample getSpecimen() {
        return specimen;
    }

    public void setSpecimen(Sample specimen) {
        this.specimen = specimen;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExternalDoctor() {
        return externalDoctor;
    }

    public void setExternalDoctor(String externalDoctor) {
        this.externalDoctor = externalDoctor;
    }

    public Machine getEquipment() {
        return equipment;
    }

    public void setEquipment(Machine equipment) {
        this.equipment = equipment;
    }

    public Staff getReferringDoctor() {
        return referringDoctor;
    }

    public void setReferringDoctor(Staff referringDoctor) {
        this.referringDoctor = referringDoctor;
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation(Investigation investigation) {
        this.investigation = investigation;
    }

    public ListingEntity getListingEntity() {
        if (listingEntity == null) {
            listingEntity = ListingEntity.PATIENT_INVESTIGATIONS;
        }
        return listingEntity;
    }

    public void setListingEntity(ListingEntity listingEntity) {
        this.listingEntity = listingEntity;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public List<Bill> getSelectedBills() {
        return selectedBills;
    }

    public void setSelectedBills(List<Bill> selectedBills) {
        this.selectedBills = selectedBills;
    }

    public List<PatientSample> getSelectedPatientSamples() {
        return selectedPatientSamples;
    }

    public void setSelectedPatientSamples(List<PatientSample> selectedPatientSamples) {
        this.selectedPatientSamples = selectedPatientSamples;
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

    public List<PatientSample> prepareSampleCollectionByBillsForPhlebotomyRoom(List<Bill> bills, WebUser wu) {
        System.out.println("wu = " + wu);
        System.out.println("bills = " + bills);
        System.out.println("wu = " + wu);
        String j = "";
        Map m;
        Map<Long, PatientSample> rPatientSamplesMap = new HashMap<>();

        if (bills == null) {
            return null;
        }

        for (Bill b : bills) {
            System.out.println("b = " + b);
            m = new HashMap();
            m.put("can", false);
            m.put("bill", b);
            j = "Select pi from PatientInvestigation pi "
                    + " where pi.cancelled=:can "
                    + " and pi.billItem.bill=:bill";
            List<PatientInvestigation> pis = ejbFacade.findByJpql(j, m);
            System.out.println("pis = " + pis);
            if (pis == null) {
                return null;
            }

            for (PatientInvestigation ptix : pis) {
                System.out.println("ptix = " + ptix);

                Investigation ix = ptix.getInvestigation();

                if (ix == null) {
                    continue;
                }

                ptix.setCollected(true);
                ptix.setSampleCollecter(wu);
                ptix.setSampleDepartment(wu.getDepartment());
                ptix.setSampleInstitution(wu.getInstitution());
                ptix.setSampledAt(new Date());
                ejbFacade.edit(ptix);

                List<InvestigationItem> ixis = getItems(ix);

                if (ixis == null) {
                    continue;
                }

                for (InvestigationItem ixi : ixis) {
                    System.out.println("ixi = " + ixi);

                    if (ixi.getIxItemType() == InvestigationItemType.Value) {
                        System.out.println("ixi.getTube() = " + ixi.getTube());
                        if (ixi.getTube() == null) {
                            continue;
                        }
                        System.out.println("ixi.getSample() = " + ixi.getSample());
                        if (ixi.getSample() == null) {
                            continue;
                        }

                        j = "select ps from PatientSample ps "
                                + " where ps.tube=:tube "
                                + " and ps.sample=:sample "
                                + " and ps.machine=:machine "
                                + " and ps.patient=:pt "
                                + " and ps.bill=:bill ";
//                                + " and ps.collected=:ca
                        m = new HashMap();
                        m.put("tube", ixi.getTube());

                        m.put("sample", ixi.getSample());

                        m.put("machine", ixi.getMachine());

                        m.put("pt", b.getPatient());

                        m.put("bill", b);
//                        m.put("ca", false);
                        if (ix.isHasMoreThanOneComponant()) {
                            j += " and ps.investigationComponant=:sc ";
                            m.put("sc", ixi.getSampleComponent());
                        }
                        System.out.println("j = " + j);
                        System.out.println("m = " + m);
                        PatientSample pts = patientSampleFacade.findFirstByJpql(j, m);
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

                            pts.setBarcodeGenerated(true);
                            pts.setBarcodeGeneratedDepartment(wu.getDepartment());
                            pts.setBarcodeGeneratedInstitution(wu.getInstitution());
                            pts.setBarcodeGenerator(wu);
                            pts.setBarcodeGeneratedAt(new Date());

                            pts.setCreatedAt(new Date());
                            pts.setCreater(wu);

                            pts.setSampleCollected(false);
                            pts.setSampleReceivedAtLab(false);
                            pts.setReadyTosentToAnalyzer(false);
                            pts.setSentToAnalyzer(false);
                            pts.setReceivedFromAnalyzer(false);
                            pts.setRetired(false);
                            patientSampleFacade.create(pts);
                            System.out.println("new pts = " + pts);
                        }
                        rPatientSamplesMap.put(pts.getId(), pts);

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
                        m.put("pts", pts);

                        m.put("bill", b);

                        m.put("pt", b.getPatient());

                        m.put("ptix", ptix);

                        m.put("ixc", ixi.getSampleComponent());

                        System.out.println("j = " + j);
                        System.out.println("m = " + m);

                        ptsc = patientSampleComponantFacade.findFirstByJpql(j, m);
                        System.out.println("ptsc = " + ptsc);
                        if (ptsc == null) {
                            ptsc = new PatientSampleComponant();
                            ptsc.setPatientSample(pts);
                            ptsc.setBill(b);
                            ptsc.setPatient(b.getPatient());
                            ptsc.setPatientInvestigation(ptix);
                            ptsc.setInvestigationComponant(ixi.getSampleComponent());
                            ptsc.setCreatedAt(new Date());
                            ptsc.setCreater(wu);
                            patientSampleComponantFacade.create(ptsc);
                        }
                    }
                }
            }

        }

        List<PatientSample> rPatientSamples = new ArrayList<>(rPatientSamplesMap.values());
        return rPatientSamples;
    }

    public void prepareSampleCollectionByBillsForPhlebotomyRoom(BillBarcode b) {

        String j = "";
        Map m;
        Map<Long, PatientSample> rPatientSamplesMap = new HashMap<>();

        if (b == null) {
            return;
        }

        if (b.getPatientInvestigationWrappers() == null) {
            return;
        }

        for (PatientInvestigationWrapper ptixw : b.getPatientInvestigationWrappers()) {
            PatientInvestigation ptix = ptixw.getPatientInvestigation();
            if (ptix == null) {
                continue;
            }

            Investigation ix = ptix.getInvestigation();

            if (ix == null) {
                continue;
            }

            ptix.setCollected(true);
            ptix.setSampleCollecter(sessionController.getLoggedUser());
            ptix.setSampleDepartment(sessionController.getDepartment());
            ptix.setSampleInstitution(sessionController.getInstitution());
            ptix.setSampledAt(new Date());
            ejbFacade.edit(ptix);

            List<InvestigationItem> ixis = getItems(ix);

            if (ixis == null) {
                continue;
            }

            for (InvestigationItem ixi : ixis) {
                System.out.println("ixi = " + ixi);

                if (ixi.getIxItemType() == InvestigationItemType.Value) {
                    System.out.println("ixi.getTube() = " + ixi.getTube());
                    if (ixi.getTube() == null) {
                        continue;
                    }
                    System.out.println("ixi.getSample() = " + ixi.getSample());
                    if (ixi.getSample() == null) {
                        continue;
                    }

                    j = "select ps from PatientSample ps "
                            + " where ps.tube=:tube "
                            + " and ps.sample=:sample "
                            + " and ps.machine=:machine "
                            + " and ps.patient=:pt "
                            + " and ps.bill=:bill ";
//                                + " and ps.collected=:ca
                    m = new HashMap();
                    m.put("tube", ixi.getTube());

                    m.put("sample", ixi.getSample());

                    m.put("machine", ixi.getMachine());

                    m.put("pt", b.getBill().getPatient());

                    m.put("bill", b.getBill());
//                        m.put("ca", false);
                    if (ix.isHasMoreThanOneComponant()) {
                        j += " and ps.investigationComponant=:sc ";
                        m.put("sc", ixi.getSampleComponent());
                    }
                    System.out.println("j = " + j);
                    System.out.println("m = " + m);
                    PatientSample pts = patientSampleFacade.findFirstByJpql(j, m);
                    System.out.println("pts = " + pts);
                    if (pts == null) {
                        pts = new PatientSample();

                        pts.setTube(ixi.getTube());
                        pts.setSample(ixi.getSample());
                        if (ix.isHasMoreThanOneComponant()) {
                            pts.setInvestigationComponant(ixi.getSampleComponent());
                        }
                        pts.setMachine(ixi.getMachine());
                        pts.setPatient(b.getBill().getPatient());
                        pts.setBill(b.getBill());

                        pts.setBarcodeGenerated(true);
                        pts.setBarcodeGeneratedDepartment(sessionController.getDepartment());
                        pts.setBarcodeGeneratedInstitution(sessionController.getInstitution());
                        pts.setBarcodeGenerator(sessionController.getLoggedUser());
                        pts.setBarcodeGeneratedAt(new Date());

                        pts.setCreatedAt(new Date());
                        pts.setCreater(sessionController.getLoggedUser());

                        pts.setSampleCollected(false);
                        pts.setSampleReceivedAtLab(false);
                        pts.setReadyTosentToAnalyzer(false);
                        pts.setSentToAnalyzer(false);
                        pts.setReceivedFromAnalyzer(false);
                        pts.setRetired(false);
                        patientSampleFacade.create(pts);
                        System.out.println("new pts = " + pts);
                    }
                    rPatientSamplesMap.put(pts.getId(), pts);

                    PatientSampleComponant ptsc;
                    j = "select ps from PatientSampleComponant ps "
                            + " where ps.patientSample=:pts "
                            + " and ps.bill=:bill "
                            + " and ps.patient=:pt "
                            + " and ps.patientInvestigation=:ptix "
                            + " and ps.investigationComponant=:ixc";
                    m = new HashMap();
                    m.put("pts", pts);
                    m.put("bill", b.getBill());
                    m.put("pt", b.getBill().getPatient());
                    m.put("ptix", ptix);
                    m.put("ixc", ixi.getSampleComponent());

                    System.out.println("j = " + j);
                    System.out.println("m = " + m);

                    ptsc = patientSampleComponantFacade.findFirstByJpql(j, m);
                    System.out.println("ptsc = " + ptsc);
                    if (ptsc == null) {
                        ptsc = new PatientSampleComponant();
                        ptsc.setPatientSample(pts);
                        ptsc.setBill(b.getBill());
                        ptsc.setPatient(b.getBill().getPatient());
                        ptsc.setPatientInvestigation(ptix);
                        ptsc.setInvestigationComponant(ixi.getSampleComponent());
                        ptsc.setCreatedAt(new Date());
                        ptsc.setCreater(sessionController.getLoggedUser());
                        patientSampleComponantFacade.create(ptsc);
                    }
                }

            }

        }

        List<PatientSample> rPatientSamples = new ArrayList<>(rPatientSamplesMap.values());

        System.out.println("rPatientSamples = " + rPatientSamples);

        for (PatientSample pts : rPatientSamples) {
            System.out.println("pts = " + pts);
            PatientSampleWrapper psw = new PatientSampleWrapper(pts);
            b.getPatientSampleWrappers().add(psw);
            System.out.println("b.getPatientSampleWrappers() = " + b.getPatientSampleWrappers());
        }

    }

    public List<PatientSampleComponant> getPatientSampleComponentsByPatientSample(PatientSample patientSample) {
        List<PatientSampleComponant> ptsc;
        String j;
        Map m = new HashMap();
        j = "select ps from PatientSampleComponant ps "
                + " where ps.patientSample=:pts "
                + " and ps.retired=false ";
        m = new HashMap();
        m.put("pts", patientSample);
        System.out.println("j = " + j);
        System.out.println("m = " + m);
        ptsc = patientSampleComponantFacade.findByJpql(j, m);
        System.out.println("ptsc = " + ptsc);
        return ptsc;
    }

    public List<InvestigationItem> getItems(Investigation ix) {
        List<InvestigationItem> iis;
        if (ix == null) {
            return new ArrayList<>();
        }
        Investigation temIx = ix;
        if (ix.getReportedAs() != null) {
            if (ix.getReportedAs() instanceof Investigation) {
                temIx = (Investigation) ix.getReportedAs();
            }
        }

        if (ix.getId() != null) {
            String temSql;
            temSql = "SELECT i FROM InvestigationItem i where i.retired<>true and i.item=:item order by i.riTop, i.riLeft";
            Map m = new HashMap();
            m.put("item", temIx);

            iis = investigationItemFacade.findByJpql(temSql, m);
        } else {
            iis = new ArrayList<>();
        }
        return iis;
    }

}
