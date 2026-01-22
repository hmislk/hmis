package com.divudi.bean.lab;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.dto.InvestigationDTO;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
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
    
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    private String json;

    private String lineModel;

    private Investigation currtntPatinetInvestigations;
    private Patient patient;

    private List<InvestigationDTO> investigationList;
    private Date fromDate;
    private Date toDate;

    public void makeNull() {
        System.out.println("makeNull() - START");
        fromDate = null;
        toDate = null;
        investigationList = new ArrayList<>();
        lineModel = null;
        System.out.println("makeNull() - END");
    }
    
    public String navigateToPatientList() {
        return "/opd/patient_search?faces-redirect=true";
    }

    public String navigateToProcessInvestigations(Patient pt) {
        System.out.println("navigateToProcessInvestigations() - START");
        System.out.println("pt = " + pt);
        makeNull();
        setPatient(pt);
        System.out.println("patient = " + patient);
        String navigateTo = "/lab/patient_lab_investigation_list?faces-redirect=true";
        System.out.println("navigateToProcessInvestigations() - END, returning: " + navigateTo);
        return navigateTo;
    }

    public void processInvestigationsInPatient() {
        System.out.println("patient = " + patient);
        System.out.println("patient = " + getPatient());
        System.out.println("processInvestigationsInPatient() - START");

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

        // Log JPQL query
        System.out.println("JPQL Query: " + jpql);

        // Log parameters
        System.out.println("Parameters:");
        System.out.println("  fd (fromDate): " + getFromDate());
        System.out.println("  td (toDate): " + getToDate());
        System.out.println("  pt (patient): " + patient);
        System.out.println("  app (approved): " + true);

        try {
            investigationList = (List<InvestigationDTO>) patientReportFacade.findLightsByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);

            // Log results
            System.out.println("Query executed successfully");
            System.out.println("Number of results: " + (investigationList != null ? investigationList.size() : 0));

            if (investigationList != null && !investigationList.isEmpty()) {
                System.out.println("First few results:");
                for (int i = 0; i < Math.min(5, investigationList.size()); i++) {
                    System.out.println("  Result " + (i + 1) + ": " + investigationList.get(i));
                }
            } else {
                System.out.println("No results found");
            }

        } catch (Exception e) {
            System.err.println("ERROR in processInvestigationsInPatient(): " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw if you want the error to propagate
        }
        lineModel = null;

        System.out.println("processInvestigationsInPatient() - END");
    }

    public void navigateToChart(Long investigationId, Patient pt) {
        System.out.println("navigateToChart() - START");
        System.out.println("Parameters:");
        System.out.println("  investigationId: " + investigationId);
        System.out.println("  pt (Patient): " + pt);
        System.out.println("  pt.getId(): " + (pt != null ? pt.getId() : "null"));

        try {
            System.out.println("Fetching investigation from database...");
            currtntPatinetInvestigations = investigationFacade.findWithoutCache(investigationId);
            System.out.println("currtntPatinetInvestigations: " + currtntPatinetInvestigations);

            System.out.println("Fetching patient from database...");
            patient = patientFacade.findWithoutCache(pt.getId());
            System.out.println("patient: " + patient);

            System.out.println("Creating line model...");
            createLineModel();
            System.out.println("Line model created successfully");

            String navigateTo = "/reports/lab/report_data_chart_view?faces-redirect=true";
            
        } catch (Exception e) {
            System.err.println("ERROR in navigateToChart(): " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public List<InvestigationItem> loadPatientReportItemValueInSelectedInvestigationForChart() {
        System.out.println("loadPatientReportItemValueInSelectedInvestigationForChart() - START");

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

        System.out.println("JPQL Query: " + jpql);
        System.out.println("Parameters:");
        System.out.println("  ins (Investigation): " + currtntPatinetInvestigations);
        System.out.println("  ptId (Patient ID): " + patient.getId());
        System.out.println("  allow: " + true);

        try {
            List<InvestigationItem> rawData = investigationItemFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
            System.out.println("Query executed successfully");
            System.out.println("Number of results: " + (rawData != null ? rawData.size() : 0));
            System.out.println("loadPatientReportItemValueInSelectedInvestigationForChart() - END");
            return rawData;
        } catch (Exception e) {
            System.err.println("ERROR in loadPatientReportItemValueInSelectedInvestigationForChart(): " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public RGBAColor generateRandomRGBAColor(String name) {
        System.out.println("generateRandomRGBAColor() - START, name: " + name);

        // Use name + timestamp as seed for Random
        Random random = new Random(name.hashCode() + System.nanoTime());

        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);

        System.out.println("Generated color - R:" + r + " G:" + g + " B:" + b + " A:255");
        RGBAColor color = new RGBAColor(r, g, b);
        System.out.println("generateRandomRGBAColor() - END");
        return color;
    }

    public Collection<String> getLableList() {
        System.out.println("getLableList() - START");
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

        System.out.println("JPQL Query: " + jpql);
        System.out.println("Parameters:");
        System.out.println("  ins (Investigation): " + currtntPatinetInvestigations);
        System.out.println("  ptId (Patient ID): " + patient.getId());
        System.out.println("  allow: " + true);

        List<PatientReport> rawData = patientReportFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        System.out.println("Query executed successfully");
        System.out.println("Number of results (Reports): " + (rawData != null ? rawData.size() : 0));

        for (PatientReport pr : rawData) {
            Date approveAt = pr.getApproveAt();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM dd hh:mm aa");
            String dateString = sdf.format(approveAt);
            labels.add(dateString);
        }

        System.out.println("Total labels created: " + labels.size());
        System.out.println("getLableList() - END");
        return labels;
    }

    public List<LineDataset> generateInvestigationChart() {
        System.out.println("generateInvestigationChart() - START");

        List<LineDataset> lineDataset = new ArrayList<>();
        List<InvestigationItem> reportValues = loadPatientReportItemValueInSelectedInvestigationForChart();
        System.out.println("Processing " + (reportValues != null ? reportValues.size() : 0) + " report values for chart generation");

        for (InvestigationItem priv : reportValues) {
            System.out.println("Processing investigation item: " + priv.getName());
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

            try {
                List<PatientReportItemValue> rawData = patientReportItemValueFacade.findByJpqlWithoutCache(jpql, params, null);
                System.out.println("  Retrieved " + (rawData != null ? rawData.size() : 0) + " data points for item: " + priv.getName());
                Collection<Number> d = new ArrayList<>();
                for (PatientReportItemValue row : rawData) {
                    try {
                        Double doubleVal = row.getDoubleValue();
                        String strVal = row.getStrValue();
                        System.out.println("doubleVal = " + doubleVal);
                        System.out.println("strVal = " + strVal);

                        // Get numeric value with fallback logic
                        Double finalValue = null;
                        if (doubleVal != null) {
                            System.out.println("doubleVal");
                            finalValue = doubleVal;
                            System.out.println("Double finalValue = " + finalValue);
                        } else if (strVal != null && !strVal.trim().isEmpty()) {
                            System.out.println("String");
                            try {
                                System.out.println("Try");
                                finalValue = Double.valueOf(strVal.trim());
                            } catch (NumberFormatException e) {
                                System.out.println("catch");
                                continue; // Skip non-numeric string values
                            }
                            System.out.println("STR finalValue = " + finalValue);
                        }
                        
                        System.out.println("finalValue = " + finalValue);

                        // Add to chart data if value and date are available
                        if (finalValue != null) {
                            d.add(finalValue);
                        }
                        System.out.println("Data Array Size = " + d.size());
                    } catch (Exception e) {
                        // Skip problematic records
                        continue;
                    }
                }
                newLineDataset.setData(d);
                lineDataset.add(newLineDataset);
            } catch (Exception e) {
                System.err.println("  ERROR processing investigation item " + priv.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("Total datasets created: " + lineDataset.size());
        System.out.println("generateInvestigationChart() - END");
        return lineDataset;
    }

    public void createLineModel() {
        System.out.println("createLineModel() - START");
        System.out.println("Current Investigation: " + (currtntPatinetInvestigations != null ? currtntPatinetInvestigations.getName() : "null"));

        try {
            System.out.println("Generating datasets and labels...");
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
            System.out.println("Line model JSON created successfully");
            System.out.println("JSON length: " + (lineModel != null ? lineModel.length() : 0) + " characters");
            System.out.println("createLineModel() - END");
        } catch (Exception e) {
            System.err.println("ERROR in createLineModel(): " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

//    public void createLineModel() {
//        lineModel = new LineChart()
//                .setData(new LineData()
//                        .addDataset(new LineDataset()
//                                .setData(65, 59, 80, 81, 36, 55, 40)
//                                .setLabel("Maths")
//                                .setBorderColor(new RGBAColor(75, 192, 192))
//                                .setLineTension(0.1f)
//                                .setFill(new Fill<Boolean>(false)))
//                        .addDataset(new LineDataset()
//                                .setData(35, 78, 96, 78, 11, 14, 88)
//                                .setLabel("Science")
//                                .setBorderColor(new RGBAColor(175, 19, 122))
//                                .setLineTension(0.1f)
//                                .setFill(new Fill<Boolean>(false)))
//                        .addDataset(new LineDataset()
//                                .setData(78, 68, 40, 51, 55, 63, 18)
//                                .setLabel("History")
//                                .setBorderColor(new RGBAColor(175, 119, 141))
//                                .setLineTension(0.1f)
//                                .setFill(new Fill<Boolean>(false)))
//                        .addDataset(new LineDataset()
//                                .setData(63, 20, 45, 86, 61, 74, 88)
//                                .setLabel("ICT")
//                                .setBorderColor(new RGBAColor(75, 119, 12))
//                                .setLineTension(0.1f)
//                                .setFill(new Fill<Boolean>(false)))
//                        .addDataset(new LineDataset()
//                                .setData(14, 48, 91, 70, 34, 48, 28)
//                                .setLabel("Music")
//                                .setBorderColor(new RGBAColor(15, 19, 12))
//                                .setLineTension(0.1f)
//                                .setFill(new Fill<Boolean>(false)))
//                        .setLabels("January", "February", "March", "April", "May", "June", "July")
//                )
//
//                .setOptions(new LineOptions()
//                        .setResponsive(true)
//                        .setMaintainAspectRatio(false)
//                        .setPlugins(new Plugins()
//                                .setTitle(new Title()
//                                        .setDisplay(true)
//                                        .setText("Line Chart Subtitle")))
//                ).toJson();
//    }
    public void itemSelect(ItemSelectEvent event) {
        System.out.println("itemSelect() - START");
        System.out.println("Event data: " + event.getData());
        System.out.println("Item Index: " + event.getItemIndex());
        System.out.println("DataSet Index: " + event.getDataSetIndex());

        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Item selected",
                "Value: " + event.getData()
                + ", Item Index: " + event.getItemIndex()
                + ", DataSet Index:" + event.getDataSetIndex());

        FacesContext.getCurrentInstance().addMessage(null, msg);
        System.out.println("itemSelect() - END");
    }

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

}
