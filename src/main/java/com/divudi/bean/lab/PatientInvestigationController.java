/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.bean.report.InstitutionLabSumeryController;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.Department;
import com.divudi.entity.Sms;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.lab.PatientReport;
import com.divudi.entity.lab.ReportItem;
import com.divudi.facade.BillFacade;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.InvestigationItemFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PatientReportFacade;
import com.divudi.facade.ReportItemFacade;
import com.divudi.facade.SmsFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class PatientInvestigationController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private PatientInvestigationFacade ejbFacade;
    @EJB
    PatientReportFacade prFacade;
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
    
    List<Investigation> investSummery;
    Date sampledOutsideDate;
    boolean sampledOutSide;
    boolean listIncludingReceived;
    boolean listIncludingEnteredData;
    private boolean listIncludingSampled;
    boolean listIncludingApproved;
    List<PatientInvestigation> selectedToReceive;
    
    Sms sms;

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

    
    public PatientInvestigation getPatientInvestigationFromBillItem(BillItem bi){
        String j;
        Map m = new HashMap();
        j="select pi "
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
            fromDate = getCommonFunctions().getStartOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
        resetLists();
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
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
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
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

    @Inject
    private InstitutionLabSumeryController labReportSearchByInstitutionController;
    
    public void sendSms() {
        if (current == null) {
            UtilityController.addErrorMessage("Nothing to send sms");
            return;
        }

        Bill bill = current.getBillItem().getBill();

        System.out.println("running the sending sms.");
        if (bill == null) {
            System.out.println("pr is null ");
        }
        String url = "http://www.textit.biz/sendmsg/index.php";
        HttpResponse<String> stringResponse;
        String messageBody;
        String id = "94715812399";
        String pw = "5672";

        if (bill == null || bill.getPatient() == null || bill.getPatient().getPerson() == null || bill.getPatient().getPerson().getPhone() == null) {
            return;
        }

        
        
        String sendingNo = bill.getPatient().getPerson().getPhone();
        if(sendingNo.contains("077") || sendingNo.contains("076")
                || sendingNo.contains("071")||sendingNo.contains("072")||
                sendingNo.contains("075")||sendingNo.contains("078")){
            System.err.println("sending no is " + sendingNo);
        }else{
            System.err.println("sending no is " + sendingNo + ". Returning as number is not valid");
            return;
        }
        
        StringBuilder sb = new StringBuilder(sendingNo);
        sb.deleteCharAt(3);
        sendingNo = sb.toString();

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
            System.out.println("ex = " + ex);
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

        
        System.out.println("Sending Sms Completed. ");
        
        UtilityController.addSuccessMessage("Sms send");

        getLabReportSearchByInstitutionController().createPatientInvestigaationList();
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
            getCurrent().setSampledAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
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
        getCurrent().getSampledAt();
        Map temMap = new HashMap();
        temSql = "SELECT i FROM PatientInvestigation i where i.retired=false  and i.collected = false and i.billItem.bill.billDate between :fromDate and :toDate";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        lstToSamle = getFacade().findBySQL(temSql, temMap, TemporalType.TIMESTAMP);
    }

    List<PatientInvestigation> toReceive;

    public List<PatientInvestigation> getToReceive() {
        return toReceive;
    }

    public void toPrintWorksheets() {
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
            getCurrent().setReceivedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getCurrent().setReceivedCollecter(getSessionController().getLoggedUser());
            getEjbFacade().edit(getCurrent());
        }

        getLabReportSearchByInstitutionController().createPatientInvestigaationList();
    }

    public void markSelectedAsReceived() {
//        ////System.out.println("going to mark as received");
        for (PatientInvestigation pi : getSelectedToReceive()) {
            pi.setReceived(Boolean.TRUE);
            pi.setReceivedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
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
            getCurrent().setDataEntryAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
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
            getCurrent().setApproveAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
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
            getCurrent().setPrintingAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getEjbFacade().edit(getCurrent());
        }
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
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
}
