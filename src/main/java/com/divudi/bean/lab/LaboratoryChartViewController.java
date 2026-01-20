package com.divudi.bean.lab;

import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.lab.InvestigationItem;
import com.divudi.core.entity.lab.PatientReportItemValue;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import org.primefaces.event.ItemSelectEvent;
import com.divudi.core.facade.PatientEncounterFacade;
import java.util.Date;
import software.xdev.chartjs.model.charts.LineChart;
import software.xdev.chartjs.model.color.RGBAColor;
import software.xdev.chartjs.model.data.LineData;
import software.xdev.chartjs.model.options.Title;
import software.xdev.chartjs.model.dataset.LineDataset;
import software.xdev.chartjs.model.enums.ScalesPosition;
import software.xdev.chartjs.model.options.LineOptions;
import software.xdev.chartjs.model.options.Plugins;
import software.xdev.chartjs.model.options.elements.Fill;
import software.xdev.chartjs.model.options.scale.Scales;
import software.xdev.chartjs.model.options.scale.cartesian.linear.LinearScaleOptions;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Named
@RequestScoped
public class LaboratoryChartViewController implements Serializable {

    private static final long serialVersionUID = 1L;

    public LaboratoryChartViewController() {

    }
    
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    
    private String json;
    private String cartesianLinerModel;
    private String lineModel;
    
    private PatientEncounter currtntPatinetEncounter;
    private Investigation currtntPatinetInvestigations;

    private List<Investigation> patinetInvestigations;
    
    // Investigation chart data and methods
    private List<Object[]> investigationChartData = new ArrayList<>();
    private InvestigationItem selectedInvestigationItem;
    
    public void loadPatientReportItemValueInSelectedInvestigation(){
        List<PatientReportItemValue> list = new ArrayList<>();
    
        Map<String, Object> params = new HashMap<>();
        params.put("ins", currtntPatinetInvestigations);
        params.put("invItem", selectedInvestigationItem.getId());
        
        String jpql = "SELECT priv "
                + "FROM PatientReportItemValue priv "
                + "WHERE priv.patient.id = :pt "
                + "AND priv.patientReport.patientInvestigation.investigation =:ins "
                + "AND priv.patientReport.approveAt IS NOT NULL "
                + "AND (priv.doubleValue IS NOT NULL OR priv.strValue IS NOT NULL) "
                + "ORDER BY priv.patientReport.approveAt ASC";

        
        
            List<Object[]> rawData = patientEncounterFacade.findObjectArrayByJpql(jpql, params, null);
            investigationChartData = new ArrayList<>();
    }

    public void generateInvestigationChart() {
        if (currtntPatinetEncounter == null || currtntPatinetEncounter.getPatient() == null || selectedInvestigationItem == null) {
            investigationChartData = new ArrayList<>();
            return;
        }
        

        // High-performance DTO query using selected InvestigationItem
        Map<String, Object> params = new HashMap<>();
        params.put("pt", currtntPatinetEncounter.getPatient().getId());
        params.put("invItem", selectedInvestigationItem.getId());

        String jpql = "SELECT priv.doubleValue, priv.strValue, priv.patientReport.approveAt "
                + "FROM PatientReportItemValue priv "
                + "WHERE priv.patient.id = :pt "
                + "AND priv.investigationItem.id = :invItem "
                + "AND priv.patientReport.approveAt IS NOT NULL "
                + "AND (priv.doubleValue IS NOT NULL OR priv.strValue IS NOT NULL) "
                + "ORDER BY priv.patientReport.approveAt ASC";

        try {
            List<Object[]> rawData = patientEncounterFacade.findObjectArrayByJpql(jpql, params, null);
            investigationChartData = new ArrayList<>();

            for (Object[] row : rawData) {
                try {
                    Double doubleVal = (Double) row[0];
                    String strVal = (String) row[1];
                    Date approveDate = (Date) row[2];

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
                    if (finalValue != null && approveDate != null) {
                        Object[] chartRecord = {approveDate, finalValue};
                        investigationChartData.add(chartRecord);
                    }
                } catch (Exception e) {
                    // Skip problematic records
                    continue;
                }
            }
        } catch (Exception e) {
            investigationChartData = new ArrayList<>();
        }
    }

    public List<Object[]> getInvestigationChartData() {
        return investigationChartData;
    }
    
    

    @PostConstruct
    public void init() {
        createLineModel();
    }

    public void createLineModel() {
        lineModel = new LineChart()
                .setData(new LineData()
                .addDataset(new LineDataset()
                        .setData(65, 59, 80, 81, 36, 55, 40)
                        .setLabel("Maths")
                        .setBorderColor(new RGBAColor(75, 192, 192))
                        .setLineTension(0.1f)
                        .setFill(new Fill<Boolean>(false)))
                .addDataset(new LineDataset()
                        .setData(35, 78, 96, 78, 11, 14, 88)
                        .setLabel("Science")
                        .setBorderColor(new RGBAColor(175, 19, 122))
                        .setLineTension(0.1f)
                        .setFill(new Fill<Boolean>(false)))
                .addDataset(new LineDataset()
                        .setData(78, 68, 40, 51, 55, 63, 18)
                        .setLabel("History")
                        .setBorderColor(new RGBAColor(175, 119, 141))
                        .setLineTension(0.1f)
                        .setFill(new Fill<Boolean>(false)))
                .addDataset(new LineDataset()
                        .setData(63, 20, 45, 86, 61, 74, 88)
                        .setLabel("ICT")
                        .setBorderColor(new RGBAColor(75, 119, 12))
                        .setLineTension(0.1f)
                        .setFill(new Fill<Boolean>(false)))
                 .addDataset(new LineDataset()
                        .setData(14, 48, 91, 70, 34, 48, 28)
                        .setLabel("Music")
                        .setBorderColor(new RGBAColor(15, 19, 12))
                        .setLineTension(0.1f)
                        .setFill(new Fill<Boolean>(false)))
                        
                .setLabels("January", "February", "March", "April", "May", "June", "July"))
                
                .setOptions(new LineOptions()
                        .setResponsive(true)
                        .setMaintainAspectRatio(false)
                        .setPlugins(new Plugins()
                                .setTitle(new Title()
                                        .setDisplay(true)
                                        .setText("Line Chart Subtitle")))
                ).toJson();
    }

    public void createCartesianLinerModel() {
        cartesianLinerModel = new LineChart()
                .setData(new LineData()
                .addDataset(new LineDataset()
                        .setData(20, 50, 100, 75, 25, 0)
                        .setLabel("Left Dataset")
                        .setLineTension(0.5f)
                        .setYAxisID("left-y-axis")
                        .setFill(new Fill<Boolean>(true)))
                .addDataset(new LineDataset()
                        .setData(0.1, 0.5, 1.0, 2.0, 1.5, 0)
                        .setLabel("Right Dataset")
                        .setLineTension(0.5f)
                        .setYAxisID("right-y-axis")
                        .setFill(new Fill<Boolean>(true)))
                .setLabels("Jan", "Feb", "Mar", "Apr", "May", "Jun"))
                .setOptions(new LineOptions()
                        .setResponsive(true)
                        .setMaintainAspectRatio(false)
                        .setScales(new Scales()
                                .addScale("left-y-axis", new LinearScaleOptions().setPosition(ScalesPosition.LEFT))
                                .addScale("right-y-axis", new LinearScaleOptions().setPosition(ScalesPosition.RIGHT)))
                        .setPlugins(new Plugins()
                                .setTitle(new Title()
                                        .setDisplay(true)
                                        .setText("Cartesian Linear Chart")))
                ).toJson();
    }


    public void itemSelect(ItemSelectEvent event) {
        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Item selected",
                "Value: " + event.getData()
                + ", Item Index: " + event.getItemIndex()
                + ", DataSet Index:" + event.getDataSetIndex());

        FacesContext.getCurrentInstance().addMessage(null, msg);
    }
    
    

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getCartesianLinerModel() {
        return cartesianLinerModel;
    }

    public void setCartesianLinerModel(String cartesianLinerModel) {
        this.cartesianLinerModel = cartesianLinerModel;
    }

    public String getLineModel() {
        return lineModel;
    }

    public void setLineModel(String lineModel) {
        this.lineModel = lineModel;
    }

    public PatientEncounter getCurrtntPatinetEncounter() {
        return currtntPatinetEncounter;
    }

    public void setCurrtntPatinetEncounter(PatientEncounter currtntPatinetEncounter) {
        this.currtntPatinetEncounter = currtntPatinetEncounter;
    }

    public Investigation getCurrtntPatinetInvestigations() {
        return currtntPatinetInvestigations;
    }

    public void setCurrtntPatinetInvestigations(Investigation currtntPatinetInvestigations) {
        this.currtntPatinetInvestigations = currtntPatinetInvestigations;
    }

    public List<Investigation> getPatinetInvestigations() {
        return patinetInvestigations;
    }

    public void setPatinetInvestigations(List<Investigation> patinetInvestigations) {
        this.patinetInvestigations = patinetInvestigations;
    }

}
