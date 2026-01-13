package com.divudi.bean.report;


import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.IncomeBundle;
import com.divudi.core.data.IncomeRow;
import com.divudi.core.data.dto.OpdIncomeReportDTO;
import com.divudi.core.data.dto.OpdRevenueDashboardDTO;
import com.divudi.core.data.reports.CommonReports;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.Room;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.facade.PatientRoomFacade;
import com.divudi.core.facade.RoomFacade;
import com.divudi.core.facade.RoomFacilityChargeFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.BillService;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.pie.PieChartDataSet;
import org.primefaces.model.charts.pie.PieChartModel;
import org.primefaces.model.charts.pie.PieChartOptions;

/**
 * @author Ishira Pahasara
 */
@Named
@SessionScoped
public class InwardReportDashboardController implements Serializable{

    private String reportTemplateFileIndexName;
    private int activeIndex = 0;
    private WebUser loggedUser = null;
    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;
    private Department discountDept;
    
    @EJB
    private RoomFacade roomFacade;
    @EJB
    private PatientRoomFacade patientRoomFacade;
    @EJB
    private RoomFacilityChargeFacade roomFacilityChargeFacade;
    @EJB
    private BillService billService;
    
    // Bed occupancy 
    private Long totalRooms;
    private Long occupiedRooms;
    private Long underConstruction;
    private Long availableRooms;
    
    // Opd revenue
    private final List<String> selectionTypesOpdRevenue = List.of("Department Wise", "Site Wise", "Institution Wise");
    private String selectionTypeOpdRevenue;
    private List<OpdRevenueDashboardDTO> opdRevenueDashboardDtos; 
    private IncomeBundle opdRevenueBundle;
    
    // Discount
    private List<OpdRevenueDashboardDTO> discountDashboard;
    private IncomeBundle discountBundle;
    
    // Charts
    private PieChartModel bedOccupancyChart;
    private BarChartModel opdRevenueChart;
    private BarChartModel discountChart;
    
    private final String[] colorPalette = {
                            "rgb(255, 99, 132)",
                            "rgb(54, 162, 235)",
                            "rgb(255, 205, 86)",
                            "rgb(75, 192, 192)",
                            "rgb(153, 102, 255)",
                            "rgb(255, 159, 64)",
                            "rgb(201, 203, 207)",
                            "rgb(255, 99, 71)",
                            "rgb(144, 238, 144)",
                            "rgb(173, 216, 230)"
                        };
    
    public void InwardReportDashboardController(){
       
    }
    
    
    // Bed Occupency Dashboard Navigation
    public String navigateToBedOccupencyDashboard (){
      activeIndex=1;
      setReportTemplateFileIndexName("/reports/index.xhtml");
      return "/reports/dashboard/bedOccupencyDashboard?faces-redirect=true";
    }
    
    // OPD Revenue Dashboard Navigation
    public String navigateToopdRevenueDashboard (){
      activeIndex=2;
      setReportTemplateFileIndexName("/reports/index.xhtml");
      return "/reports/dashboard/opdRevenueDashboard?faces-redirect=true";
    }
    
    // Discount Dashboard Navigation
    public String navigateToDiscountDashboard (){
      activeIndex=3;
      setReportTemplateFileIndexName("/reports/index.xhtml");
      return "/reports/dashboard/discountDashboard?faces-redirect=true";
    }
    
    // HR Dashboard Navigation
    public String navigateToHRDashboard (){
      activeIndex=4;
      setReportTemplateFileIndexName("/reports/index.xhtml");
      return "/reports/dashboard/hrDashboard?faces-redirect=true";
    }   

    private void setReportTemplateFileIndexName(String reportTemplateFileIndexName) {
        this.reportTemplateFileIndexName = reportTemplateFileIndexName;
    }
    
    public int getActiveIndex() {
       return activeIndex ;
    }
    
    public void setActiveIndex(int activeIndex){
      this.activeIndex = activeIndex;
    }
    
    public void processBedOccupancy() {
        System.out.println("process started.... = ");
        bedOccupancyChart = new PieChartModel();
        setOccupiedRooms();
        setTotalRooms();
        setAvailableRooms();
        setUnderConstructionRooms();

        if (totalRooms !=  null && occupiedRooms != null && underConstruction != null){
            setBedOccupancyChart();
            System.out.println("totalRooms = " + totalRooms + "---oR = " + occupiedRooms + "---UR = " + underConstruction);
        }
    }
    
    private void setTotalRooms() {
        String sql = "SELECT COUNT(i) FROM Room i where i.retired=false";
        totalRooms = getRoomFacade().countByJpql(sql);
    }
    
    private void setUnderConstructionRooms() {
        String sql = "SELECT COUNT(i) FROM Room i where i.retired=false and i.filled=true";
        underConstruction = getRoomFacade().countByJpql(sql);
    }
    
    private void setAvailableRooms() {
        String sql = "SELECT COUNT(rcf) FROM RoomFacilityCharge rcf"
                + " Where rcf.room.filled=false "
                + " And rcf.retired=false";
        
        availableRooms = getRoomFacilityChargeFacade().countByJpql(sql);
    }

    private void setOccupiedRooms() {
        String sql = "SELECT COUNT(pr) FROM PatientRoom pr "
            + " where pr.retired=false "
            + " and pr.discharged=false ";

        occupiedRooms = getPatientRoomFacade().countByJpql(sql);
    }
    
    private void setBedOccupancyChart() {        
        PieChartDataSet dataSet = new PieChartDataSet();
        Long vacantRooms;
        
        
        if (availableRooms != null && occupiedRooms != null) {
            vacantRooms = availableRooms - occupiedRooms;
        } else {
            vacantRooms = Long.valueOf(0);
        }
        
        List<Number> rooms = new ArrayList<>();
        rooms.add(occupiedRooms);
        rooms.add(underConstruction);
        rooms.add(vacantRooms);
        rooms.add(totalRooms - (availableRooms+underConstruction));
        dataSet.setData(rooms);
        
        List<String> bgColors = new ArrayList<>();
        bgColors.add("rgb(153, 102, 255)");  // Purple
        bgColors.add("rgb(255, 159, 64)");   // Orange
        bgColors.add("rgb(201, 203, 207)");  // Gray
        bgColors.add("rgb(255, 99, 255)");   // Pink/Magenta
        dataSet.setBackgroundColor(bgColors);
        
        ChartData data = new ChartData();
        data.addChartDataSet(dataSet);
        
        List<String> labels = new ArrayList<>();
        labels.add("Occupied Rooms");
        labels.add("UnderConstruction Rooms");
        labels.add("Vacant Rooms");
        labels.add("Unavailable Rooms");
        data.setLabels(labels);
        
        bedOccupancyChart.setData(data);
    }
    
    private void setOpdRevenueChart() {        
        BarChartDataSet dataSet = new BarChartDataSet();
        dataSet.setLabel("Opd Revenue");

        List<Object> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<String> bgColors = new ArrayList<>();
        
        for (int i = 0; i < opdRevenueBundle.getRows().size(); i++) {
            IncomeRow r = opdRevenueBundle.getRows().get(i);
            values.add(r.getGrossTotal());
            labels.add(r.getRowType());
            bgColors.add(colorPalette[i % colorPalette.length]);
        } 
        
        dataSet.setData(values);
        dataSet.setBackgroundColor(bgColors);
        
        ChartData data = new ChartData();
        data.addChartDataSet(dataSet);
        data.setLabels(labels);
        opdRevenueChart.setData(data);
    }
    
    private void setBarChart(IncomeBundle bd, BarChartModel m, String label) {        
        BarChartDataSet dataSet = new BarChartDataSet();
        dataSet.setLabel(label);

        List<Object> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<String> bgColors = new ArrayList<>();
        
        for (int i = 0; i < bd.getRows().size(); i++) {
            IncomeRow r = bd.getRows().get(i);
            values.add(r.getGrossTotal());
            labels.add(r.getRowType());
            bgColors.add(colorPalette[i % colorPalette.length]);
        } 
        
        dataSet.setData(values);
        dataSet.setBackgroundColor(bgColors);
        
        ChartData data = new ChartData();
        data.addChartDataSet(dataSet);
        data.setLabels(labels);
        m.setData(data);
    }
    
    public RoomFacade getRoomFacade() {
        return roomFacade;
    }

    public PatientRoomFacade getPatientRoomFacade() {
        return patientRoomFacade;
    }
    
    public RoomFacilityChargeFacade getRoomFacilityChargeFacade() {
        return roomFacilityChargeFacade;
    }
    
    public PieChartModel getBedOccupanceChart() {
        return bedOccupancyChart;
    }
    
    public BarChartModel getOpdRevenueChart() {
        return opdRevenueChart;
    }
    
    public BarChartModel getDiscountChart() {
        return discountChart;
    }
    
    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }
    
    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }
    
    public void setFromDate(Date d) {
        this.fromDate = d;
    }
    
    public void setToDate(Date d) {
        this.toDate = d;
    }
    
    public Institution getInstitution() {
        return institution;
    }
    
    public void setInstitution(Institution institution) {
        this.institution = institution;
    }
    
    public Institution getSite() {
        return site;
    }
    
    public void setSite(Institution site) {
        this.site = site;
    }
    
    public Department getDepartment() {
        return department;
    }
    
    public void setDepartment(Department department) {
        this.department = department;
    }
    
    public Department getDiscountDept() {
        return discountDept;
    }
    
    public void setDiscountDept(Department discountDept) {
        this.discountDept = discountDept;
    }
    
    public List<String> getSelectionTypesOpdRevenue() {
        return selectionTypesOpdRevenue;
    }
    
    public String getSelectionTypeOpdRevenue() {
        return selectionTypeOpdRevenue;
    }
    
    public void setSelectionTypeOpdRevenue(String s) {
        this.selectionTypeOpdRevenue = s;
    }
    
    public IncomeBundle getOpdRevenueBundle() {
        return opdRevenueBundle;
    }
    
    public IncomeBundle getDiscountBundle() {
        return discountBundle;
    }
    
    public void generateOpdIncomeReportDto() {
        opdRevenueChart = new BarChartModel();
        
        List<BillTypeAtomic> billTypeAtomics = new ArrayList<>();
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_REFUND);

        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);

        opdRevenueDashboardDtos = billService.fetchOpdRevenueDashboardDTOs(fromDate, toDate, institution, site, department, loggedUser, billTypeAtomics, null, null);

        System.out.println("Results returned: " + (opdRevenueDashboardDtos != null ? opdRevenueDashboardDtos.size() : 0));

        opdRevenueBundle = new IncomeBundle(opdRevenueDashboardDtos);
        System.out.println("selectionTypeOpdRevenue = " + selectionTypeOpdRevenue);
        opdRevenueBundle.generatePaymentDetailsForOpdRevenue(selectionTypeOpdRevenue);
        
        for (IncomeRow i : opdRevenueBundle.getRows()) {
            System.out.println("i.getGrossTotal() = " + i.getGrossTotal());
            System.out.println("RowType: "+i.getRowType());
        }
        
        setOpdRevenueChart();
        setBarChart(opdRevenueBundle, opdRevenueChart, "OPD Revenue");
        
        System.out.println("processing done = ");
    }
    
    public void generateDiscountDashboard() {
        discountChart = new BarChartModel();
        
        discountDashboard = billService.fetchBillDiscounts(fromDate, toDate, discountDept);

        System.out.println("Results returned: " + (discountDashboard != null ? discountDashboard.size() : 0));

        discountBundle = new IncomeBundle(discountDashboard);
        discountBundle.generateDiscountDetailsForDashboard();
        
        for (IncomeRow i : discountBundle.getRows()) {
            System.out.println("i.getGrossTotal() = " + i.getGrossTotal());
            System.out.println("RowType: "+i.getRowType());
        }
        
        setBarChart(discountBundle, discountChart, "Discount Chart Department Wise");
        
        System.out.println("processing done Discount= ");
    }
}
