package com.divudi.bean.lab;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.dto.InvestigationDTO;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.lab.InvestigationItem;
import com.divudi.core.entity.lab.PatientReport;
import com.divudi.core.entity.lab.PatientReportItemValue;
import com.divudi.core.facade.InvestigationFacade;
import com.divudi.core.facade.InvestigationItemFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import com.divudi.core.util.CommonFunctions;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import org.primefaces.event.ItemSelectEvent;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PatientReportFacade;
import com.divudi.core.facade.PatientReportItemValueFacade;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Random;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import software.xdev.chartjs.model.charts.LineChart;
import software.xdev.chartjs.model.color.RGBAColor;
import software.xdev.chartjs.model.data.LineData;
import software.xdev.chartjs.model.options.Title;
import software.xdev.chartjs.model.dataset.LineDataset;
import software.xdev.chartjs.model.options.LineOptions;
import software.xdev.chartjs.model.options.Plugins;
import software.xdev.chartjs.model.options.elements.Fill;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */

@Named
@SessionScoped
public class LaboratoryChartViewController implements Serializable {

    private static final long serialVersionUID = 1L;

    public LaboratoryChartViewController() {

    }

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    PatientReportItemValueFacade patientReportItemValueFacade;
    @EJB
    PatientReportFacade patientReportFacade;
    @EJB
    InvestigationFacade investigationFacade;
    @EJB
    PatientFacade patientFacade;
    @EJB
    InvestigationItemFacade investigationItemFacade;
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Variables">
    private String json;

    private String lineModel;

    private Investigation currtntPatinetInvestigations;
    private Patient patient;

    private List<InvestigationDTO> investigationList;
    private Date fromDate;
    private Date toDate;
    private List<PatientReport> chartReportTimeline;

    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Navigation Methods">
    public String navigateToPatientList() {
        return "/opd/patient_search?faces-redirect=true";
    }

    public String navigateToProcessInvestigations(Patient pt) {
        makeNull();
        setPatient(pt);
        return "/lab/patient_lab_investigation_list?faces-redirect=true";
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Function Methods">
    
    public void makeNull() {
        fromDate = null;
        toDate = null;
        investigationList = new ArrayList<>();
        lineModel = null;
        chartReportTimeline = null;
    }

    public void processInvestigationsInPatient() {
        Map<String, Object> params = new HashMap<>();
        String jpql = "SELECT new com.divudi.core.data.dto.InvestigationDTO("
                + " pr.patientInvestigation.investigation.id, "
                + " pr.patientInvestigation.investigation.name,"
                + " count(pr.id)"
                + " ) "
                + " FROM PatientReport pr "
                + " Where pr.approveAt BETWEEN :fd AND :td "
                + " and pr.approved =:app"
                + " and pr.patientInvestigation.billItem.bill.patient =:pt "
                + " GROUP BY pr.patientInvestigation.investigation.id, pr.patientInvestigation.investigation.name"
                + " ORDER BY pr.patientInvestigation.investigation.name ";

        params.put("fd", getFromDate());
        params.put("td", getToDate());
        params.put("pt", patient);
        params.put("app", true);

        investigationList = (List<InvestigationDTO>) patientReportFacade.findLightsByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);
        lineModel = null;

    }

    public void processChart(Long investigationId, Patient pt) {
        try {
            currtntPatinetInvestigations = investigationFacade.findWithoutCache(investigationId);
            patient = patientFacade.findWithoutCache(pt.getId());
            createLineModel();

        } catch (Exception e) {
        }
    }

    public List<InvestigationItem> loadPatientReportItemValueInSelectedInvestigationForChart() {

        Map<String, Object> params = new HashMap<>();
        params.put("ins", currtntPatinetInvestigations);
        params.put("allow", true);
        params.put("ptId", patient.getId());

        // Modified JPQL to properly group by investigationItem only
        String jpql = "SELECT DISTINCT priv.investigationItem "
                + "FROM PatientReportItemValue priv "
                + "WHERE priv.patient.id = :ptId "
                + "AND priv.patientReport.patientInvestigation.investigation = :ins "
                + "AND priv.patientReport.approveAt IS NOT NULL "
                + "AND priv.allowToExportChart = :allow "
                + "ORDER BY priv.patientReport.id ASC";

        List<InvestigationItem> rawData = investigationItemFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        return rawData;
    }

    public RGBAColor generateRandomRGBAColor(String name) {
        // Use name + timestamp as seed for Random
        Random random = new Random(name.hashCode() + System.nanoTime());

        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);

        RGBAColor color = new RGBAColor(r, g, b);
        return color;
    }

    public Collection<String> getLableList() {
        Collection<String> labels = new ArrayList<>();

        Map<String, Object> params = new HashMap<>();
        params.put("ins", currtntPatinetInvestigations);
        params.put("allow", true);
        params.put("ptId", patient.getId());

        String jpql = "SELECT DISTINCT priv.patientReport "
                + " FROM PatientReportItemValue priv "
                + " WHERE priv.patient.id = :ptId "
                + " AND priv.patientReport.patientInvestigation.investigation =:ins "
                + " AND priv.patientReport.approveAt IS NOT NULL "
                + " AND priv.allowToExportChart =:allow"
                + " ORDER BY priv.patientReport.approveAt ASC";

        List<PatientReport> rawData = patientReportFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        chartReportTimeline = rawData;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM dd hh:mm aa");

        for (PatientReport pr : rawData) {
            Date approveAt = pr.getApproveAt();
            String dateString = sdf.format(approveAt);
            labels.add(dateString);
        }

        return labels;
    }

    public List<LineDataset> generateInvestigationChart() {
        List<LineDataset> lineDataset = new ArrayList<>();
        List<InvestigationItem> reportValues = loadPatientReportItemValueInSelectedInvestigationForChart();

        if (chartReportTimeline == null) {
            getLableList();
        }
        
        List<PatientReport> timeline = chartReportTimeline != null ? chartReportTimeline : new ArrayList<>();
 
        for (InvestigationItem priv : reportValues) {
            LineDataset newLineDataset = new LineDataset();

            newLineDataset.setLabel(priv.getName());
            newLineDataset.setBorderColor(generateRandomRGBAColor(priv.getName()));
            newLineDataset.setLineTension(0.1f);
            newLineDataset.setFill(new Fill<Boolean>(false));

            Map<String, Object> params = new HashMap<>();
            params.put("invItem", priv.getId());
            params.put("ptId", patient.getId());
            params.put("allow", true);

            String jpql = "SELECT priv "
                    + "FROM PatientReportItemValue priv "
                    + "Where priv.patient.id = :ptId "
                    + "AND priv.investigationItem.id = :invItem "
                    + "AND priv.patientReport.approveAt IS NOT NULL "
                    + "AND priv.allowToExportChart =:allow "
                    + "AND (priv.doubleValue IS NOT NULL OR priv.strValue IS NOT NULL) "
                    + "ORDER BY priv.patientReport.approveAt ASC";

            List<PatientReportItemValue> rawData = patientReportItemValueFacade.findByJpqlWithoutCache(jpql, params, null);
            Map<Long, Double> valuesByReportId = new HashMap<>();
            for (PatientReportItemValue row : rawData) {
                try {
                    Double doubleVal = row.getDoubleValue();
                    String strVal = row.getStrValue();

                    // Get numeric value with fallback logic
                    Double finalValue = null;
                    if (doubleVal != null) {
                        finalValue = doubleVal;
                    } else if (strVal != null && !strVal.trim().isEmpty()) {
                        try {
                            finalValue = Double.valueOf(strVal.trim());
                        } catch (NumberFormatException e) {
                            continue; // Skip non-numeric string values
                        }
                    }

                    // Add to chart data if value and date are available
                    if (finalValue != null && row.getPatientReport() != null) {
                        valuesByReportId.put(row.getPatientReport().getId(), finalValue);
                    }
                } catch (Exception e) {
                    // Skip problematic records
                    continue;
                }
            }
            
            Collection<Number> d = new ArrayList<>();
            for (PatientReport pr : timeline) {
                d.add(valuesByReportId.get(pr.getId()));
            }
            newLineDataset.setData(d);
            lineDataset.add(newLineDataset);

        }
        return lineDataset;
    }

    public void createLineModel() {
        try {
            lineModel = new LineChart()
                    .setData(new LineData()
                            .setDatasets(generateInvestigationChart())
                            .setLabels(getLableList())
                    )
                    .setOptions(new LineOptions()
                            .setResponsive(true)
                            .setMaintainAspectRatio(false)
                            .setPlugins(new Plugins()
                                    .setTitle(new Title()
                                            .setDisplay(true)
                                            .setText(currtntPatinetInvestigations.getName())))
                    ).toJson();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage("Error", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Unable to build chart", e.getMessage()));
            System.err.println("ERROR in createLineModel(): " + e.getMessage());
        }
    }

    public void itemSelect(ItemSelectEvent event) {
        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Item selected",
                "Value: " + event.getData()
                + ", Item Index: " + event.getItemIndex()
                + ", DataSet Index:" + event.getDataSetIndex());

        FacesContext.getCurrentInstance().addMessage(null, msg);
    }
    
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Getter & Setter">
    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getLineModel() {
        return lineModel;
    }

    public void setLineModel(String lineModel) {
        this.lineModel = lineModel;
    }

    public Investigation getCurrtntPatinetInvestigations() {
        return currtntPatinetInvestigations;
    }

    public void setCurrtntPatinetInvestigations(Investigation currtntPatinetInvestigations) {
        this.currtntPatinetInvestigations = currtntPatinetInvestigations;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public List<InvestigationDTO> getInvestigationList() {
        return investigationList;
    }

    public void setInvestigationList(List<InvestigationDTO> investigationList) {
        this.investigationList = investigationList;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            Long months = configOptionApplicationController.getLongValueByKey("How many months back should I look at the reports", 6L);
            Date date = CommonFunctions.getDateMonthsAgo(months.intValue());
            fromDate = CommonFunctions.getStartOfDay(date);
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }
    
    // </editor-fold>

    public List<PatientReport> getChartReportTimeline() {
        return chartReportTimeline;
    }

    public void setChartReportTimeline(List<PatientReport> chartReportTimeline) {
        this.chartReportTimeline = chartReportTimeline;
    }

}
