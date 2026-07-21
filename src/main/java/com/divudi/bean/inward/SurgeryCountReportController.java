package com.divudi.bean.inward;

import com.divudi.core.data.BillType;
import com.divudi.core.data.dto.SurgeryCountTypeWiseDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.inward.SurgeryType;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;
import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.charts.LineChart;
import software.xdev.chartjs.model.color.RGBAColor;
import software.xdev.chartjs.model.data.BarData;
import software.xdev.chartjs.model.data.LineData;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.chartjs.model.dataset.LineDataset;
import software.xdev.chartjs.model.options.BarOptions;
import software.xdev.chartjs.model.options.Legend;
import software.xdev.chartjs.model.options.LineOptions;
import software.xdev.chartjs.model.options.Plugins;
import software.xdev.chartjs.model.options.Title;
import software.xdev.chartjs.model.options.elements.Fill;
import software.xdev.chartjs.model.options.scale.Scales;
import software.xdev.chartjs.model.options.scale.cartesian.linear.LinearScaleOptions;
import software.xdev.chartjs.model.options.scale.cartesian.linear.LinearTickOptions;

/**
 *
 * @author pubudupiyankara
 */
@Named
@SessionScoped
public class SurgeryCountReportController implements Serializable {

    @EJB
    BillFacade billFacade;

    private Institution institution;
    private Institution site;
    private Department department;

    private Date fromYearStartDate;
    private Date toYearEndDate;

    private int selectedYear;

    private SurgeryType surgeryType;
    private Item surgeryItem;

    private String surgeryCountChartType;
    private String surgeryCountBarChartModel;
    private String surgeryCountLineChartModel;

    private List<SurgeryCountTypeWiseDTO> surgeryCountTypeList;
    private List<String> surgeryCategoryNames;
    private Map<String, Integer> totalCategoryCounts;
    private int totalAllSurgeryCount;

    public void processSurgeryCountTypeReport() {
        resetState();

        if (!isDateRangeValid()) {
            return;
        }

        Map<String, Object> params = new HashMap<>();
        String jpql = buildSurgeryCountJpql(params);

        List<Object[]> results = billFacade.findObjectArrayByJpql(
                jpql, params, TemporalType.TIMESTAMP);

        if (results == null || results.isEmpty()) {
            JsfUtil.addErrorMessage("No surgery records found for the selected period.");
            return;
        }

        aggregateResults(results);

        if (surgeryCountChartType != null && !surgeryCountChartType.isEmpty()) {
            createSurgeryCountChartModels();
        }
    }

    private void resetState() {
        surgeryCountTypeList = new ArrayList<>();
        surgeryCategoryNames = new ArrayList<>();
        totalCategoryCounts = new HashMap<>();
        totalAllSurgeryCount = 0;
    }

    private boolean isDateRangeValid() {
        if (fromYearStartDate == null || toYearEndDate == null) {
            JsfUtil.addErrorMessage("Please select both From and To dates.");
            return false;
        }
        if (fromYearStartDate.after(toYearEndDate)) {
            JsfUtil.addErrorMessage("From Date must not be after To Date.");
            return false;
        }

        Calendar from = Calendar.getInstance();
        from.setTime(fromYearStartDate);
        Calendar to = Calendar.getInstance();
        to.setTime(toYearEndDate);

        if (from.get(Calendar.YEAR) != to.get(Calendar.YEAR)) {
            JsfUtil.addErrorMessage(
                    "Please select a date range within a single calendar year. "
                    + "Monthly totals are grouped by month only, so a "
                    + "multi-year range would merge counts from different years.");
            return false;
        }
        return true;
    }

    private String buildSurgeryCountJpql(Map<String, Object> params) {
        StringBuilder jpql = new StringBuilder();
        jpql.append(" select ")
                .append("   coalesce(upper(c.name), 'OTHER'), ")
                .append("   function('MONTH', b.createdAt), ")
                .append("   count(b) ")
                .append(" from BilledBill b ")
                .append(" join b.procedure p ")
                .append(" join p.item i ")
                .append(" left join i.category c ")
                .append(" where b.retired = false ")
                .append(" and b.cancelled = false ")
                .append(" and b.billType = :bt ")
                .append(" and b.createdAt between :fd and :td ");

        params.put("bt", BillType.SurgeryBill);
        params.put("fd", fromYearStartDate);
        params.put("td", toYearEndDate);

        if (institution != null) {
            jpql.append(" and b.institution = :inst ");
            params.put("inst", institution);
        }
        if (department != null) {
            jpql.append(" and b.department = :dept ");
            params.put("dept", department);
        }
        if (site != null) {
            jpql.append(" and b.department.site = :site ");
            params.put("site", site);
        }
        if (surgeryType != null) {
            jpql.append(" and c = :stype ");
            params.put("stype", surgeryType);
        }
        if (surgeryItem != null) {
            jpql.append(" and i = :sitem ");
            params.put("sitem", surgeryItem);
        }

        jpql.append(" group by coalesce(upper(c.name), 'OTHER'), ")
                .append(" function('MONTH', b.createdAt) ")
                .append(" order by 1 ");

        return jpql.toString();
    }

    private void aggregateResults(List<Object[]> results) {
        SurgeryCountTypeWiseDTO[] monthDtos = new SurgeryCountTypeWiseDTO[12];
        for (int i = 0; i < 12; i++) {
            monthDtos[i] = new SurgeryCountTypeWiseDTO(localizedMonthName(i), i);
        }

        Set<String> categorySet = new TreeSet<>(); // sorted, dedupe on insert

        for (Object[] row : results) {
            String categoryName = (String) row[0];
            int month = ((Number) row[1]).intValue();
            int count = ((Number) row[2]).intValue();
            int monthIndex = month - 1;

            if (monthIndex < 0 || monthIndex >= 12) {
                continue; // defensive, shouldn't happen
            }

            categorySet.add(categoryName);
            monthDtos[monthIndex].addCount(categoryName, count);
            totalCategoryCounts.merge(categoryName, count, Integer::sum);
            totalAllSurgeryCount += count;
        }

        surgeryCategoryNames = new ArrayList<>(categorySet);
        surgeryCountTypeList.addAll(Arrays.asList(monthDtos));
    }

    private String localizedMonthName(int monthIndex) {
        return Month.of(monthIndex + 1).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private void createSurgeryCountChartModels() {
        if (surgeryCountTypeList == null || surgeryCountTypeList.isEmpty()) {
            surgeryCountBarChartModel = null;
            surgeryCountLineChartModel = null;
            return;
        }

        createSurgeryCountBarChart();
        createSurgeryCountLineChart();
    }

    private void createSurgeryCountBarChart() {
        if (surgeryCountTypeList == null || surgeryCountTypeList.isEmpty()) {
            surgeryCountBarChartModel = null;
            return;
        }

        BarChart barChart = new BarChart();
        BarData barData = new BarData();
        barData.addLabels(shortMonthLabels());

        int colorIndex = 0;
        for (String categoryName : surgeryCategoryNames) {
            int[] rgb = parseRgb(CHART_COLORS[colorIndex % CHART_COLORS.length]);

            BarDataset dataset = new BarDataset()
                    .setLabel(categoryName)
                    .setBackgroundColor(toRgba(rgb, 0.7))
                    .setBorderColor(toRgba(rgb, 1))
                    .setBorderWidth(1);

            for (SurgeryCountTypeWiseDTO dto : surgeryCountTypeList) {
                dataset.addData(dto.getCount(categoryName));
            }
            barData.addDataset(dataset);
            colorIndex++;
        }

        barChart.setData(barData);

        BarOptions barOptionsObj = new BarOptions();
        barOptionsObj.setPlugins(buildChartPlugins());
        barOptionsObj.setScales(buildChartScales());
        barChart.setOptions(barOptionsObj);

        surgeryCountBarChartModel = barChart.toJson();
    }

    private void createSurgeryCountLineChart() {
        if (surgeryCountTypeList == null || surgeryCountTypeList.isEmpty()) {
            surgeryCountLineChartModel = null;
            return;
        }

        LineChart lineChart = new LineChart();
        LineData lineData = new LineData();
        lineData.addLabels(shortMonthLabels());

        int colorIndex = 0;
        for (String categoryName : surgeryCategoryNames) {
            int[] rgb = parseRgb(CHART_COLORS[colorIndex % CHART_COLORS.length]);

            LineDataset dataset = new LineDataset()
                    .setLabel(categoryName)
                    .setBorderColor(toRgba(rgb, 1))
                    .setFill(new Fill(false))
                    .setTension(0.4f);

            for (SurgeryCountTypeWiseDTO dto : surgeryCountTypeList) {
                dataset.addData(dto.getCount(categoryName));
            }
            lineData.addDataset(dataset);
            colorIndex++;
        }

        lineChart.setData(lineData);

        LineOptions lineOptionsObj = new LineOptions();
        lineOptionsObj.setPlugins(buildChartPlugins());
        lineOptionsObj.setScales(buildChartScales());
        lineChart.setOptions(lineOptionsObj);

        surgeryCountLineChartModel = lineChart.toJson();
    }

    private String[] shortMonthLabels() {
        return new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    }

    private int[] parseRgb(String csv) {
        String[] parts = csv.split(",");
        return new int[]{
            Integer.parseInt(parts[0].trim()),
            Integer.parseInt(parts[1].trim()),
            Integer.parseInt(parts[2].trim())
        };
    }

    private RGBAColor toRgba(int[] rgb, double alpha) {
        return new RGBAColor(rgb[0], rgb[1], rgb[2], alpha);
    }

    private Plugins buildChartPlugins() {
        Plugins plugins = new Plugins();
        plugins.setTitle(new Title().setDisplay(true)
                .setText("Surgery Count Type - Year " + getSelectedYear()));
        plugins.setLegend(new Legend().setDisplay(true).setPosition(Legend.Position.TOP));
        return plugins;
    }

    private Scales buildChartScales() {
        Scales scales = new Scales();
        scales.addScale("y", new LinearScaleOptions()
                .setBeginAtZero(true)
                .setTicks(new LinearTickOptions().setStepSize(1)));
        return scales;
    }

    private static final String[] CHART_COLORS = {
        "75, 192, 192", "255, 99, 132", "54, 162, 235", "255, 206, 86",
        "153, 102, 255", "255, 159, 64", "199, 199, 199", "83, 102, 255",
        "255, 99, 255", "99, 255, 132", "220, 20, 60", "65, 105, 225"
    };
    public Date getFromYearStartDate() {
        if (fromYearStartDate == null) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MONTH, Calendar.JANUARY);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            fromYearStartDate = cal.getTime();
        }
        return fromYearStartDate;
    }

    public void setFromYearStartDate(Date fromYearStartDate) {
        this.fromYearStartDate = fromYearStartDate;
    }

    public Date getToYearEndDate() {
        if (toYearEndDate == null) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MONTH, Calendar.DECEMBER);
            cal.set(Calendar.DAY_OF_MONTH, 31);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);

            toYearEndDate = cal.getTime();
        }
        return toYearEndDate;
    }

    public void setToYearEndDate(Date toYearEndDate) {
        this.toYearEndDate = toYearEndDate;
    }

    public String getSurgeryCountChartType() {
        return surgeryCountChartType;
    }

    public void setSurgeryCountChartType(String surgeryCountChartType) {
        this.surgeryCountChartType = surgeryCountChartType;
    }

    public List<SurgeryCountTypeWiseDTO> getSurgeryCountTypeList() {
        return surgeryCountTypeList;
    }

    public int getSelectedYear() {
        Calendar cal = Calendar.getInstance();
        if (fromYearStartDate != null) {
            cal.setTime(fromYearStartDate);
        }
        selectedYear = cal.get(Calendar.YEAR);
        return selectedYear;
    }

    public String getSurgeryCountBarChartModel() {
        return surgeryCountBarChartModel;
    }

    public void setSurgeryCountBarChartModel(String surgeryCountBarChartModel) {
        this.surgeryCountBarChartModel = surgeryCountBarChartModel;
    }

    public String getSurgeryCountLineChartModel() {
        return surgeryCountLineChartModel;
    }

    public void setSurgeryCountLineChartModel(String surgeryCountLineChartModel) {
        this.surgeryCountLineChartModel = surgeryCountLineChartModel;
    }

    public List<String> getSurgeryCategoryNames() {
        return surgeryCategoryNames;
    }

    public void setSurgeryCategoryNames(List<String> surgeryCategoryNames) {
        this.surgeryCategoryNames = surgeryCategoryNames;
    }

    public Map<String, Integer> getTotalCategoryCounts() {
        return totalCategoryCounts;
    }

    public void setTotalCategoryCounts(Map<String, Integer> totalCategoryCounts) {
        this.totalCategoryCounts = totalCategoryCounts;
    }

    public int getTotalAllSurgeryCount() {
        return totalAllSurgeryCount;
    }

    public void setTotalAllSurgeryCount(int totalAllSurgeryCount) {
        this.totalAllSurgeryCount = totalAllSurgeryCount;
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

    public SurgeryType getSurgeryType() {
        return surgeryType;
    }

    public void setSurgeryType(SurgeryType surgeryType) {
        this.surgeryType = surgeryType;
    }

    public Item getSurgeryItem() {
        return surgeryItem;
    }

    public void setSurgeryItem(Item surgeryItem) {
        this.surgeryItem = surgeryItem;
    }
}
