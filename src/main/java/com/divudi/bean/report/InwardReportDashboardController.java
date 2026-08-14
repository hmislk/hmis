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
import com.divudi.core.facade.PatientRoomFacade;
import com.divudi.core.facade.RoomFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.BillService;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.persistence.TemporalType;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.optionconfig.legend.Legend;
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
    private BillService billService;

    // Bed occupancy
    private Long totalRooms;
    private Long occupiedRooms;
    private Long underConstructionRooms;
    private Long blockedRooms;
    private Long pendingDischargeRooms;
    private List<BedOccupancySummaryDTO> bedOccupancySummary;
    
    public static class BedOccupancySummaryDTO {
        private String status;
        private Long count;
        
        public BedOccupancySummaryDTO(String status, Long count) {
            this.status = status;
            this.count = count;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }
    
    // Opd revenue
    private final List<String> selectionTypesOpdRevenue = List.of("Department Wise", "Site Wise", "Institution Wise");
    private String selectionTypeOpdRevenue;
    private List<OpdRevenueDashboardDTO> opdRevenueDashboardDtos; 
    private double totalRevenue;
    
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
        bedOccupancyChart = new PieChartModel();
        setTotalRooms();
        setUnderConstructionRooms();
        setBlockedRooms();
        setOccupiedRooms();
        setPendingDischargeRooms();

        if (totalRooms != null && underConstructionRooms != null && blockedRooms != null
                && occupiedRooms != null && pendingDischargeRooms != null) {
            setBedOccupancyChart();
        }
    }

    private void setTotalRooms() {
        String sql = "SELECT COUNT(i) FROM Room i where i.retired=false";
        totalRooms = getRoomFacade().findLongByJpql(sql);
    }

    // "Unavailable" = under construction / out of service for renovation etc.
    // Tracked on the room master record itself (Room.filled), independent of
    // any specific patient stay, so — like Blocked — it reflects today's
    // configuration regardless of the selected date range: there is no
    // history table for room-level status changes to reconstruct a past
    // value from.
    private void setUnderConstructionRooms() {
        String sql = "SELECT COUNT(i) FROM Room i WHERE i.retired=false AND i.filled=true";
        underConstructionRooms = getRoomFacade().findLongByJpql(sql);
    }

    // "Blocked" = shut down, no longer usable (Housekeeping/Contaminated/
    // Isolated/OutOfOrder on the room record) — same bedStatus signal
    // BedBoardController uses for the live bed board. Excludes rooms already
    // counted as Unavailable so the two buckets never overlap. Room-level
    // fact, so — see setUnderConstructionRooms() — not date-scoped.
    private void setBlockedRooms() {
        String sql = "SELECT COUNT(r) FROM Room r WHERE r.retired=false AND r.filled=false "
                + " AND r.bedStatus IS NOT NULL AND r.bedStatus <> com.divudi.core.data.inward.BedStatus.Available";
        blockedRooms = getRoomFacade().findLongByJpql(sql);
    }

    // Occupied = distinct physical rooms with a PatientRoom stay overlapping
    // [fromDate, toDate] — admitted on/before toDate and either still
    // occupying (discharged=false) or discharged on/after fromDate. With the
    // default "today" range this reduces to "occupied right now"; a past
    // range reconstructs which rooms were occupied at any point during it.
    // DISTINCT keeps this in the same unit as totalRooms. Rooms already
    // Blocked or Unavailable are excluded so a room is never double-counted.
    private void setOccupiedRooms() {
        String sql = "SELECT COUNT(DISTINCT pr.roomFacilityCharge.room) FROM PatientRoom pr "
                + " WHERE pr.retired=false "
                + " AND pr.admittedAt <= :toDate "
                + " AND (pr.discharged=false OR pr.dischargedAt >= :fromDate) "
                + " AND pr.roomFacilityCharge.room.id NOT IN ("
                + "     SELECT r.id FROM Room r WHERE r.retired=false "
                + "     AND (r.filled=true OR (r.bedStatus IS NOT NULL AND r.bedStatus <> com.divudi.core.data.inward.BedStatus.Available))"
                + " )";
        Map<String, Object> params = new HashMap<>();
        params.put("fromDate", getFromDate());
        params.put("toDate", getToDate());
        occupiedRooms = getPatientRoomFacade().findLongByJpql(sql, params, TemporalType.TIMESTAMP);
    }

    // Marked for Discharge = clinically discharged (PatientEncounter.clinicalDischargeDateTime
    // set) — a true subset of occupiedRooms, using the same date-range overlap condition.
    private void setPendingDischargeRooms() {
        String sql = "SELECT COUNT(DISTINCT pr.roomFacilityCharge.room) FROM PatientRoom pr "
                + " WHERE pr.retired=false "
                + " AND pr.admittedAt <= :toDate "
                + " AND (pr.discharged=false OR pr.dischargedAt >= :fromDate) "
                + " AND pr.patientEncounter.clinicalDischargeDateTime IS NOT NULL "
                + " AND pr.roomFacilityCharge.room.id NOT IN ("
                + "     SELECT r.id FROM Room r WHERE r.retired=false "
                + "     AND (r.filled=true OR (r.bedStatus IS NOT NULL AND r.bedStatus <> com.divudi.core.data.inward.BedStatus.Available))"
                + " )";
        Map<String, Object> params = new HashMap<>();
        params.put("fromDate", getFromDate());
        params.put("toDate", getToDate());
        pendingDischargeRooms = getPatientRoomFacade().findLongByJpql(sql, params, TemporalType.TIMESTAMP);
    }

    private void setBedOccupancyChart() {
        PieChartDataSet dataSet = new PieChartDataSet();

        long blockedSlice = (blockedRooms != null ? blockedRooms : 0);
        long underConstructionSlice = (underConstructionRooms != null ? underConstructionRooms : 0);
        long pendingDischargeSlice = (pendingDischargeRooms != null ? pendingDischargeRooms : 0);

        // occupiedRooms already excludes blocked/unavailable rooms and includes
        // pendingDischargeRooms as a subset, so this can never go negative in
        // consistent data; the clamp only guards against the counts being read
        // a moment apart under concurrent admissions.
        long occupiedSlice = (occupiedRooms != null ? occupiedRooms : 0) - pendingDischargeSlice;
        if (occupiedSlice < 0) occupiedSlice = 0;

        long vacantSlice = (totalRooms != null ? totalRooms : 0) - (occupiedRooms != null ? occupiedRooms : 0)
                - blockedSlice - underConstructionSlice;
        if (vacantSlice < 0) vacantSlice = 0;

        bedOccupancySummary = new ArrayList<>();
        bedOccupancySummary.add(new BedOccupancySummaryDTO("Occupied Rooms", occupiedSlice));
        bedOccupancySummary.add(new BedOccupancySummaryDTO("Vacant Rooms", vacantSlice));
        bedOccupancySummary.add(new BedOccupancySummaryDTO("Blocked Rooms", blockedSlice));
        bedOccupancySummary.add(new BedOccupancySummaryDTO("Unavailable Rooms", underConstructionSlice));
        bedOccupancySummary.add(new BedOccupancySummaryDTO("Marked for Discharge", pendingDischargeSlice));

        List<Number> rooms = new ArrayList<>();
        for (BedOccupancySummaryDTO dto : bedOccupancySummary) {
            rooms.add(dto.getCount());
        }

        dataSet.setData(rooms);

        List<String> bgColors = new ArrayList<>();
        bgColors.add("rgb(54, 162, 235)");   // Blue (Occupied)
        bgColors.add("rgb(144, 238, 144)");  // Green (Vacant)
        bgColors.add("rgb(255, 205, 86)");   // Orange (Blocked)
        bgColors.add("rgb(201, 203, 207)");  // Gray (Unavailable)
        bgColors.add("rgb(255, 99, 132)");   // Pink/Red (Marked for Discharge)
        dataSet.setBackgroundColor(bgColors);
        
        ChartData data = new ChartData();
        data.addChartDataSet(dataSet);
        
        List<String> labels = new ArrayList<>();
        for (BedOccupancySummaryDTO dto : bedOccupancySummary) {
            labels.add(dto.getStatus());
        }
        data.setLabels(labels);
        
        bedOccupancyChart.setData(data);
    }
    
    private void setOpdRevenueChart() {  
        if (opdRevenueDashboardDtos == null || opdRevenueDashboardDtos.isEmpty()) {
            return;
        }
        
        BarChartDataSet dataSet = new BarChartDataSet();

        List<Object> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<String> bgColors = new ArrayList<>();
        
        for (int i = 0; i < opdRevenueDashboardDtos.size(); i++) {
            OpdRevenueDashboardDTO dto = opdRevenueDashboardDtos.get(i);
            values.add(dto.getTotal());
            labels.add(dto.getServiceCategoryName());
            bgColors.add(colorPalette[i % colorPalette.length]);
        } 
        
        dataSet.setData(values);
        dataSet.setBackgroundColor(bgColors);
        
        ChartData data = new ChartData();
        data.addChartDataSet(dataSet);
        data.setLabels(labels);
        opdRevenueChart.setData(data);
        
        // disable legend
        BarChartOptions options = new BarChartOptions();
        Legend legend = new Legend();
        legend.setDisplay(false);
        options.setLegend(legend);
        opdRevenueChart.setOptions(options);
    }
    
    private void setDiscountChart() {        
        BarChartDataSet dataSet = new BarChartDataSet();

        List<Object> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<String> bgColors = new ArrayList<>();
        
        for (int i = 0; i < discountBundle.getRows().size(); i++) {
            IncomeRow r = discountBundle.getRows().get(i);
            values.add(r.getDiscount());
            labels.add(r.getRowType());
            bgColors.add(colorPalette[i % colorPalette.length]);
        } 
        
        dataSet.setData(values);
        dataSet.setBackgroundColor(bgColors);
        
        ChartData data = new ChartData();
        data.addChartDataSet(dataSet);
        data.setLabels(labels);
        discountChart.setData(data);
        
        // disable the legend
        BarChartOptions options = new BarChartOptions();
        Legend legend = new Legend();
        legend.setDisplay(false);
        options.setLegend(legend);
        discountChart.setOptions(options);
    }
    
    public RoomFacade getRoomFacade() {
        return roomFacade;
    }

    public PatientRoomFacade getPatientRoomFacade() {
        return patientRoomFacade;
    }

    public PieChartModel getBedOccupancyChart() {
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
    
    public IncomeBundle getDiscountBundle() {
        return discountBundle;
    }
    
    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    
    public List<OpdRevenueDashboardDTO> getOpdRevenueDashboardDtos() {
        return opdRevenueDashboardDtos;
    }
    
    public void setOpdRevenueDashboardDtos(List<OpdRevenueDashboardDTO> dtos) {
        this.opdRevenueDashboardDtos = dtos;
    }
    
    public List<BedOccupancySummaryDTO> getBedOccupancySummary() {
        return bedOccupancySummary;
    }
    
    public Long getTotalRooms() {
        return totalRooms;
    }
    
    public void generateOpdIncomeReportDto() {
        opdRevenueChart = new BarChartModel();
        List<BillTypeAtomic> btas = fetchBillTypeAtomicForOpdRevenue();
        List<OpdRevenueDashboardDTO> rawList = billService.fetchOpdRevenueDashboardDTOs(getFromDate(), getToDate(), institution, site, department, btas);

        Map<Object, OpdRevenueDashboardDTO> grouped = new LinkedHashMap<>();
        totalRevenue = 0;

        for (OpdRevenueDashboardDTO dto : rawList) {
            if (dto.getServiceCategoryId() == null) {
                continue; // Skip if service category is null
            }

            Long catId = dto.getServiceCategoryId();
            OpdRevenueDashboardDTO currentCat = grouped.computeIfAbsent(catId, k -> {
                OpdRevenueDashboardDTO cat = new OpdRevenueDashboardDTO();
                cat.setServiceCategoryId(catId);
                cat.setServiceCategoryName(dto.getServiceCategoryName());
                cat.setTotal(0.0);

                return cat;
            });

            currentCat.setTotal(currentCat.getTotal() + dto.getTotal());
            totalRevenue += dto.getTotal();
        }

        opdRevenueDashboardDtos = new ArrayList<>();
        for (Map.Entry<Object, OpdRevenueDashboardDTO> entry : grouped.entrySet()) {
            if (entry.getValue().getTotal() == 0) {
                continue;
            }

            opdRevenueDashboardDtos.add(entry.getValue());
        }
        setOpdRevenueChart();
        
    }
    
    public void generateDiscountDashboard() {
        discountChart = new BarChartModel();
        List<BillTypeAtomic> btas = getDashboardDiscountBillTypes();
        
        discountDashboard = billService.fetchBillDiscounts(getFromDate(), getToDate(), discountDept, btas);
        discountBundle = new IncomeBundle(discountDashboard);
        discountBundle.generateDiscountDetailsForDashboard();
        
        setDiscountChart();
    }
    
    
    private List<BillTypeAtomic> fetchBillTypeAtomicForOpdRevenue() {
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
            
         return billTypeAtomics;
        
    }
    
    public List<BillTypeAtomic> getDashboardDiscountBillTypes() {
        List<BillTypeAtomic> billTypeAtomics = getOpdAndPharmacyIncomeBillTypes();
        
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);
        
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
        
        billTypeAtomics.add(BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL);
        billTypeAtomics.add(BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL_CANCELLATION);
        
        billTypeAtomics.add(BillTypeAtomic.INWARD_PROFESSIONAL_FEE_BILL);
        billTypeAtomics.add(BillTypeAtomic.INWARD_PROFESSIONAL_FEE_BILL_CANCELLATION);
        
        billTypeAtomics.add(BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL);
        billTypeAtomics.add(BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL_CANCELLATION);
        
        return billTypeAtomics;
    }
    
    public List<BillTypeAtomic> getOpdAndPharmacyIncomeBillTypes() {
        List<BillTypeAtomic> billTypeAtomics = new ArrayList<>();
            billTypeAtomics.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
            billTypeAtomics.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
            billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
            billTypeAtomics.add(BillTypeAtomic.OPD_BILL_REFUND);

            // pharmacy income
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_REFUND);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY);

            billTypeAtomics.add(BillTypeAtomic.PHARMACY_SALE_WITHOUT_STOCK);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_SALE_WITHOUT_STOCK_CANCELLED);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_SALE_WITHOUT_STOCK_REFUND);
            
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_WHOLESALE);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_WHOLESALE_PRE);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_WHOLESALE_CANCELLED);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_WHOLESALE_REFUND);

            billTypeAtomics.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE);
            billTypeAtomics.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION);
            billTypeAtomics.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN);
            billTypeAtomics.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE);
            billTypeAtomics.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_CANCELLATION);
            billTypeAtomics.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN);

            billTypeAtomics.add(BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE);
            billTypeAtomics.add(BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION);
            billTypeAtomics.add(BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN);

            billTypeAtomics.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD);
            billTypeAtomics.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION);
            billTypeAtomics.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN);

            billTypeAtomics.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_THEATRE);
            billTypeAtomics.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_THEATRE_CANCELLATION);

            billTypeAtomics.add(BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD);
            billTypeAtomics.add(BillTypeAtomic.ACCEPT_RETURN_MEDICINE_THEATRE);
            
        return billTypeAtomics;
    }
}
