/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Staff;
import com.divudi.core.data.dto.StockMovementTimelineDTO;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.StockHistoryFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.line.LineChartDataSet;
import org.primefaces.model.charts.line.LineChartModel;
import org.primefaces.model.charts.line.LineChartOptions;
import org.primefaces.model.charts.optionconfig.legend.Legend;
import org.primefaces.model.charts.optionconfig.title.Title;

/**
 * Graphical stock-movement timeline.
 *
 * Plots, over a time axis, how the stock of an item changes with every
 * pharmacy transaction. Each batch of the item is drawn as its own line
 * (StockHistory.stockQty per ItemBatch) and the item total is drawn as a
 * separate line (StockHistory.itemStock). Every point on a line corresponds
 * to one stock-changing transaction, so the triggering bill is listed in a
 * detail table under each graph (bill no, type and the pbi/bifd quantity).
 *
 * Three independent graphs are stacked so two departments and a staff stock
 * (or any three department/staff scopes) can be visually compared.
 *
 * @author buddhika
 */
@Named
@ViewScoped
public class PharmacyStockMovementAnalyticsController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private StockHistoryFacade stockHistoryFacade;
    @EJB
    private DepartmentFacade departmentFacade;

    @Inject
    private SessionController sessionController;

    // <editor-fold defaultstate="collapsed" desc="Filters">
    private Date fromDate;
    private Date toDate;
    private Item item;

    // Each of the three graphs is scoped by either a Department or a Staff.
    // Scope type: "department" or "staff".
    private String scopeType1 = "department";
    private String scopeType2 = "department";
    private String scopeType3 = "staff";

    private Department department1;
    private Department department2;
    private Department department3;

    private Staff staff1;
    private Staff staff2;
    private Staff staff3;

    // Y-axis metric: "qty", "purchaseValue" or "saleValue"
    private String metric = "qty";
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Results">
    private LineChartModel chart1;
    private LineChartModel chart2;
    private LineChartModel chart3;

    private List<BillMarker> markers1;
    private List<BillMarker> markers2;
    private List<BillMarker> markers3;

    private String scopeLabel1;
    private String scopeLabel2;
    private String scopeLabel3;

    private boolean processed;
    // </editor-fold>

    private static final String[] COLOR_PALETTE = {
        "rgb(54, 162, 235)",
        "rgb(255, 99, 132)",
        "rgb(255, 159, 64)",
        "rgb(75, 192, 192)",
        "rgb(153, 102, 255)",
        "rgb(255, 205, 86)",
        "rgb(201, 203, 207)",
        "rgb(255, 99, 71)",
        "rgb(144, 238, 144)",
        "rgb(173, 216, 230)"
    };
    private static final String TOTAL_LINE_COLOR = "rgb(0, 0, 0)";

    public PharmacyStockMovementAnalyticsController() {
    }

    /**
     * Case-insensitive autocomplete spanning ALL departments across every
     * institution (not just the logged-in institution). Matches on department
     * name or its institution name. Returns up to 100 matches so broad
     * searches are not silently truncated.
     */
    public List<Department> completeAllDepartments(String qry) {
        if (qry == null || qry.trim().isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, Object> m = new HashMap<>();
        m.put("q", "%" + qry.trim().toLowerCase() + "%");
        String jpql = "select c from Department c "
                + " where c.retired=false "
                + " and (lower(c.name) like :q or lower(c.institution.name) like :q) "
                + " order by c.name";
        return departmentFacade.findByJpql(jpql, m, 100);
    }

    public String navigateToStockMovementTimeline() {
        item = null;
        chart1 = null;
        chart2 = null;
        chart3 = null;
        markers1 = null;
        markers2 = null;
        markers3 = null;
        processed = false;
        return "/pharmacy/pharmacy_stock_movement_timeline?faces-redirect=true";
    }

    public void process() {
        processed = false;
        if (item == null) {
            JsfUtil.addErrorMessage("Please select an item");
            return;
        }
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select a date range");
            return;
        }
        if (fromDate.after(toDate)) {
            JsfUtil.addErrorMessage("From date must be before to date");
            return;
        }

        ScopeResult r1 = buildScope(scopeType1, department1, staff1);
        ScopeResult r2 = buildScope(scopeType2, department2, staff2);
        ScopeResult r3 = buildScope(scopeType3, department3, staff3);

        chart1 = r1.model;
        markers1 = r1.markers;
        scopeLabel1 = r1.label;

        chart2 = r2.model;
        markers2 = r2.markers;
        scopeLabel2 = r2.label;

        chart3 = r3.model;
        markers3 = r3.markers;
        scopeLabel3 = r3.label;

        processed = true;
    }

    private ScopeResult buildScope(String scopeType, Department dep, Staff stf) {
        ScopeResult result = new ScopeResult();
        boolean staffScope = "staff".equals(scopeType);

        if (staffScope) {
            result.label = stf != null ? "Staff: " + stf.getPerson().getNameWithTitle() : "Staff: (none selected)";
        } else {
            result.label = dep != null ? "Department: " + dep.getName() : "Department: (none selected)";
        }

        List<StockMovementTimelineDTO> rows = findTimelineRows(dep, stf, staffScope);
        result.model = buildChart(rows);
        result.markers = buildMarkers(rows);
        return result;
    }

    /**
     * Single JPQL DTO-projection query per scope. Joins StockHistory through to
     * Bill / ItemBatch / BillItemFinanceDetails so all marker and chart data is
     * fetched in one round-trip with no lazy per-row loading.
     */
    private List<StockMovementTimelineDTO> findTimelineRows(Department dep, Staff stf, boolean staffScope) {
        Map<String, Object> m = new HashMap<>();
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("i", item);

        StringBuilder jpql = new StringBuilder();
        jpql.append("select new com.divudi.core.data.dto.StockMovementTimelineDTO(")
                .append("s.id, s.createdAt, ")
                .append("ib.id, ib.batchNo, ")
                .append("s.stockQty, s.stockPurchaseValue, s.stockSaleValue, ")
                .append("s.itemStock, s.itemStockValueAtPurchaseRate, s.itemStockValueAtSaleRate, ")
                .append("bill.id, bill.deptId, bill.billTypeAtomic, ")
                .append("pbi.qty, pbi.freeQty, bifd.quantityByUnits) ")
                .append("from StockHistory s ")
                .append("left join s.itemBatch ib ")
                .append("left join s.pbItem pbi ")
                .append("left join pbi.billItem bi ")
                .append("left join bi.bill bill ")
                .append("left join bi.billItemFinanceDetails bifd ")
                .append("where s.retired=false ")
                .append("and s.item=:i ")
                // Only transaction rows carry a PharmaceuticalBillItem. Excluding
                // snapshot rows (no pbItem) keeps this a true transaction timeline
                // and guards the primitive pbi.qty/freeQty constructor args against
                // nulls from the left join.
                .append("and s.pbItem is not null ")
                .append("and s.createdAt between :fd and :td ");

        if (staffScope) {
            if (stf == null) {
                return new ArrayList<>();
            }
            jpql.append("and s.staff=:stf ");
            m.put("stf", stf);
        } else {
            // Department scope: exclude staff-stock history rows so the
            // department line is not contaminated by staff movements.
            jpql.append("and s.staff is null ");
            if (dep == null) {
                return new ArrayList<>();
            }
            jpql.append("and s.department=:dep ");
            m.put("dep", dep);
        }

        jpql.append("order by s.createdAt");
        return (List<StockMovementTimelineDTO>) (List<?>) stockHistoryFacade.findLightsByJpql(
                jpql.toString(), m, TemporalType.TIMESTAMP);
    }

    private LineChartModel buildChart(List<StockMovementTimelineDTO> rows) {
        LineChartModel model = new LineChartModel();
        ChartData data = new ChartData();

        // One chart point per transaction row (rows are already ordered by
        // createdAt). Each row gets its own index so multiple movements within
        // the same minute/second are never collapsed into a single point.
        // Seconds are included in the label so distinct same-minute movements
        // remain visually distinguishable.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<StockMovementTimelineDTO> plottedRows = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (StockMovementTimelineDTO r : rows) {
            if (r.getMovementAt() == null) {
                continue;
            }
            plottedRows.add(r);
            labels.add(sdf.format(r.getMovementAt()));
        }
        data.setLabels(labels);

        int pointCount = plottedRows.size();

        // One dataset per batch (batch-level stock), plus the item total line.
        Map<String, List<Object>> batchSeries = new LinkedHashMap<>();
        List<Object> totalSeries = newNullList(pointCount);

        for (int idx = 0; idx < plottedRows.size(); idx++) {
            StockMovementTimelineDTO r = plottedRows.get(idx);

            String batchKey = batchLabel(r);
            List<Object> series = batchSeries.get(batchKey);
            if (series == null) {
                series = newNullList(pointCount);
                batchSeries.put(batchKey, series);
            }
            series.set(idx, batchMetricValue(r));

            Object total = totalMetricValue(r);
            if (total != null) {
                totalSeries.set(idx, total);
            }
        }

        int colorIdx = 0;
        for (Map.Entry<String, List<Object>> e : batchSeries.entrySet()) {
            LineChartDataSet ds = new LineChartDataSet();
            ds.setLabel("Batch: " + e.getKey());
            ds.setData(e.getValue());
            String color = COLOR_PALETTE[colorIdx % COLOR_PALETTE.length];
            ds.setBorderColor(color);
            ds.setBackgroundColor(color);
            ds.setFill(false);
            ds.setTension(0.1);
            data.addChartDataSet(ds);
            colorIdx++;
        }

        // Item total line (all batches combined) — drawn last, in black, dashed.
        LineChartDataSet totalDs = new LineChartDataSet();
        totalDs.setLabel("Total Item Stock");
        totalDs.setData(totalSeries);
        totalDs.setBorderColor(TOTAL_LINE_COLOR);
        totalDs.setBackgroundColor(TOTAL_LINE_COLOR);
        totalDs.setFill(false);
        totalDs.setTension(0.1);
        data.addChartDataSet(totalDs);

        model.setData(data);

        LineChartOptions options = new LineChartOptions();
        Title title = new Title();
        title.setDisplay(true);
        title.setText(metricTitle());
        options.setTitle(title);

        Legend legend = new Legend();
        legend.setDisplay(true);
        legend.setPosition("bottom");
        options.setLegend(legend);

        options.setMaintainAspectRatio(false);

        model.setOptions(options);
        return model;
    }

    private List<BillMarker> buildMarkers(List<StockMovementTimelineDTO> rows) {
        List<BillMarker> list = new ArrayList<>();
        for (StockMovementTimelineDTO r : rows) {
            BillMarker bm = new BillMarker();
            bm.setStockHistoryId(r.getStockHistoryId());
            bm.setMovementAt(r.getMovementAt());
            bm.setBatchNo(batchLabel(r));
            bm.setStockQty(r.getStockQty());
            bm.setItemStock(r.getItemStock());
            bm.setPbiQty(r.getPbiQty());
            bm.setPbiFreeQty(r.getPbiFreeQty());
            bm.setBifdQty(r.getBifdQty() == null ? null : r.getBifdQty().doubleValue());
            bm.setBillId(r.getBillId());
            bm.setDeptBillNo(r.getDeptBillNo());
            bm.setBillType(r.getBillType());
            list.add(bm);
        }
        return list;
    }

    // <editor-fold defaultstate="collapsed" desc="Metric helpers">
    private Object batchMetricValue(StockMovementTimelineDTO r) {
        switch (metric) {
            case "purchaseValue":
                return r.getStockPurchaseValue();
            case "saleValue":
                return r.getStockSaleValue();
            case "qty":
            default:
                return r.getStockQty();
        }
    }

    private Object totalMetricValue(StockMovementTimelineDTO r) {
        switch (metric) {
            case "purchaseValue":
                return r.getItemStockValueAtPurchaseRate();
            case "saleValue":
                return r.getItemStockValueAtSaleRate();
            case "qty":
            default:
                return r.getItemStock();
        }
    }

    private String metricTitle() {
        switch (metric) {
            case "purchaseValue":
                return "Stock Value (Purchase Rate) over Time";
            case "saleValue":
                return "Stock Value (Sale Rate) over Time";
            case "qty":
            default:
                return "Stock Quantity over Time";
        }
    }
    // </editor-fold>

    private String batchLabel(StockMovementTimelineDTO r) {
        if (r.getBatchNo() != null && !r.getBatchNo().trim().isEmpty()) {
            return r.getBatchNo();
        }
        if (r.getItemBatchId() != null) {
            return "Batch #" + r.getItemBatchId();
        }
        return "No Batch";
    }

    private List<Object> newNullList(int size) {
        List<Object> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(null);
        }
        return list;
    }

    // <editor-fold defaultstate="collapsed" desc="Inner classes">
    private static class ScopeResult {
        LineChartModel model;
        List<BillMarker> markers;
        String label;
    }

    public static class BillMarker implements Serializable {

        private static final long serialVersionUID = 1L;
        private Long stockHistoryId;
        private Long billId;
        private String deptBillNo;
        private String billType;
        private Date movementAt;
        private String batchNo;
        private double stockQty;
        private Double itemStock;
        private double pbiQty;
        private double pbiFreeQty;
        private Double bifdQty;

        public Long getStockHistoryId() {
            return stockHistoryId;
        }

        public void setStockHistoryId(Long stockHistoryId) {
            this.stockHistoryId = stockHistoryId;
        }

        public Long getBillId() {
            return billId;
        }

        public void setBillId(Long billId) {
            this.billId = billId;
        }

        public String getDeptBillNo() {
            return deptBillNo;
        }

        public void setDeptBillNo(String deptBillNo) {
            this.deptBillNo = deptBillNo;
        }

        public String getBillType() {
            return billType;
        }

        public void setBillType(String billType) {
            this.billType = billType;
        }

        public Date getMovementAt() {
            return movementAt;
        }

        public void setMovementAt(Date movementAt) {
            this.movementAt = movementAt;
        }

        public String getBatchNo() {
            return batchNo;
        }

        public void setBatchNo(String batchNo) {
            this.batchNo = batchNo;
        }

        public double getStockQty() {
            return stockQty;
        }

        public void setStockQty(double stockQty) {
            this.stockQty = stockQty;
        }

        public Double getItemStock() {
            return itemStock;
        }

        public void setItemStock(Double itemStock) {
            this.itemStock = itemStock;
        }

        public double getPbiQty() {
            return pbiQty;
        }

        public void setPbiQty(double pbiQty) {
            this.pbiQty = pbiQty;
        }

        public double getPbiFreeQty() {
            return pbiFreeQty;
        }

        public void setPbiFreeQty(double pbiFreeQty) {
            this.pbiFreeQty = pbiFreeQty;
        }

        public Double getBifdQty() {
            return bifdQty;
        }

        public void setBifdQty(Double bifdQty) {
            this.bifdQty = bifdQty;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth();
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

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public String getScopeType1() {
        return scopeType1;
    }

    public void setScopeType1(String scopeType1) {
        this.scopeType1 = scopeType1;
    }

    public String getScopeType2() {
        return scopeType2;
    }

    public void setScopeType2(String scopeType2) {
        this.scopeType2 = scopeType2;
    }

    public String getScopeType3() {
        return scopeType3;
    }

    public void setScopeType3(String scopeType3) {
        this.scopeType3 = scopeType3;
    }

    public Department getDepartment1() {
        return department1;
    }

    public void setDepartment1(Department department1) {
        this.department1 = department1;
    }

    public Department getDepartment2() {
        return department2;
    }

    public void setDepartment2(Department department2) {
        this.department2 = department2;
    }

    public Department getDepartment3() {
        return department3;
    }

    public void setDepartment3(Department department3) {
        this.department3 = department3;
    }

    public Staff getStaff1() {
        return staff1;
    }

    public void setStaff1(Staff staff1) {
        this.staff1 = staff1;
    }

    public Staff getStaff2() {
        return staff2;
    }

    public void setStaff2(Staff staff2) {
        this.staff2 = staff2;
    }

    public Staff getStaff3() {
        return staff3;
    }

    public void setStaff3(Staff staff3) {
        this.staff3 = staff3;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public LineChartModel getChart1() {
        return chart1;
    }

    public LineChartModel getChart2() {
        return chart2;
    }

    public LineChartModel getChart3() {
        return chart3;
    }

    public List<BillMarker> getMarkers1() {
        return markers1;
    }

    public List<BillMarker> getMarkers2() {
        return markers2;
    }

    public List<BillMarker> getMarkers3() {
        return markers3;
    }

    public String getScopeLabel1() {
        return scopeLabel1;
    }

    public String getScopeLabel2() {
        return scopeLabel2;
    }

    public String getScopeLabel3() {
        return scopeLabel3;
    }

    public boolean isProcessed() {
        return processed;
    }

    public SessionController getSessionController() {
        return sessionController;
    }
    // </editor-fold>
}
