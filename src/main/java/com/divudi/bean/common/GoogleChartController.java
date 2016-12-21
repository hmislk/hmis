/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.bean.report.BookKeepingSummery;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.PreBill;
import com.divudi.entity.RefundBill;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author archmage
 */
@Named(value = "googleChartController")
@SessionScoped
public class GoogleChartController implements Serializable {

    @EJB
    private CommonFunctions commonFunctions;
    @EJB
    private BillFacade billFacade;

    @Inject
    private BookKeepingSummery bookKeepingSummery;
    @Inject
    private SessionController sessionController;

    private List<ChartValue> chartValues;
    private JSONArray jsonArray;

    /**
     * Creates a new instance of GoogleChartController
     */
    public GoogleChartController() {
    }

    public String drawChart() {
        chartValues = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        Date toDate = cal.getTime();
        cal.add(Calendar.DATE, -5);
        Date fromDate = cal.getTime();
        System.out.println("fromDate = " + fromDate);
        System.out.println("toDate = " + toDate);

//        chartValues.addAll(countsBetweenDates(fromDate, toDate));
//        JSONArray ar = new JSONArray();
        JSONArray jSONArray1 = new JSONArray();
        JSONObject cols = new JSONObject();
        JSONArray arrays = new JSONArray();
//        JSONArray in= new JSONArray();
        JSONObject ob = new JSONObject();

//        ob.put("id", '1');
//        ob.put("lable", "Date");
//        ob.put("type", "string");
//        arrays.put(ob);
//
//        ob = new JSONObject();
//        ob.put("id", '2');
//        ob.put("lable", "Count");
//        ob.put("type", "number");
//        arrays.put(ob);
        arrays.put(0, "Date");
        arrays.put(1, "Count");
        jSONArray1.put(arrays);
//        jSONArray1.put(countsBetweenDates1(fromDate, toDate));
        Date nowDate = fromDate;
        double btot = 0.0;
        double ctot = 0.0;
        double rtot = 0.0;
        double netTot = 0.0;

//        JSONObject jSONObject=new JSONObject();
        while (nowDate.before(toDate) || nowDate.equals(toDate)) {
            JSONObject in = new JSONObject();
            JSONObject out = new JSONObject();
            JSONArray inarr = new JSONArray();
            String formatedDate;
            Date fd = commonFunctions.getStartOfDay(nowDate);
            Date td = commonFunctions.getEndOfDay(nowDate);
            System.out.println("td = " + td);
            System.out.println("fd = " + fd);
            System.out.println("nowDate = " + nowDate);

            DateFormat df = new SimpleDateFormat("yyyy MMMM dd");
            formatedDate = df.format(fd);
            System.out.println("formatedDate = " + formatedDate);

            double ctot1 = fetchBillsTotal(new BillType[]{BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent}, null, null, null, new CancelledBill(), fd, td, null, null, false, true, null, null, null);
            double rtot1 = fetchBillsTotal(new BillType[]{BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent}, null, null, null, new RefundBill(), fd, td, null, null, false, true, null, null, null);
            double btot1 = fetchBillsTotal(new BillType[]{BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent}, null, null, null, new BilledBill(), fd, td, null, null, false, true, null, null, null);
            ctot += ctot1;
            rtot += rtot1;
            btot += btot1;
            netTot = btot1 - (ctot1 + rtot1);
            System.out.println(" netTot = " + netTot);
//            in.put("v", formatedDate);
//            inarr.put(in);
//            in=new JSONObject();
//            in.put("v", netTot);
//            inarr.put(in);
//            out.put("c", inarr);
//            jarr.put(out);
            inarr.put(0, formatedDate);
            inarr.put(1, netTot);
            jSONArray1.put(inarr);

            Calendar cal1 = Calendar.getInstance();
            cal1.setTime(nowDate);
            cal1.add(Calendar.DATE, 1);
            nowDate = cal1.getTime();

            System.out.println("nowDate = " + nowDate);
        }

//        cols.put("cols", arrays);
//        cols.put("rows", countsBetweenDates1(fromDate, toDate));
//        jSONArray1.put(cols);
        System.out.println("jSONArray1.toString() = " + jSONArray1.toString());
        System.out.println("cols.toString() = " + cols.toString());

//        ar.put(countsBetweenDates1(fromDate,toDate));
        return jSONArray1.toString();

    }

    public String drawPiechartDailyIncome() {
        Date current;
        Calendar cal = Calendar.getInstance();
        current = cal.getTime();
        System.out.println("date = " + current);
        Date fd = commonFunctions.getStartOfDay(current);
        Date td = commonFunctions.getEndOfDay(current);
        System.out.println("fd = " + fd);
        System.out.println("td = " + td);
        JSONArray mainJSONArray = new JSONArray();
        JSONArray subArray = new JSONArray();
        subArray.put(0, "Income Type");
        subArray.put(1, "Total");
        mainJSONArray.put(subArray);
        subArray = new JSONArray();
        BillType[] billTypes = {BillType.ChannelCash,
            BillType.ChannelOnCall,
            BillType.ChannelPaid,
            BillType.ChannelAgent,};
        List<BillType> bts = Arrays.asList(billTypes);

        subArray.put(0, "Docter Fee");
        subArray.put(1, hospitalTotalBillByBillTypeAndFeeTypeWithdocfeeForChart(bts, fd, td, true, false));
        mainJSONArray.put(subArray);
        subArray = new JSONArray();
        subArray.put(0, "Docter Fee vat");
        subArray.put(1, hospitalTotalBillByBillTypeAndFeeTypeWithdocfeeForChart(bts, fd, td, true, true));
        mainJSONArray.put(subArray);
        subArray = new JSONArray();
        subArray.put(0, "Hospital Fee");
        subArray.put(1, hospitalTotalBillByBillTypeAndFeeTypeWithdocfeeForChart(bts, fd, td, false, false));
        mainJSONArray.put(subArray);
        subArray = new JSONArray();
        subArray.put(0, "Hospital Fee vat");
        subArray.put(1, hospitalTotalBillByBillTypeAndFeeTypeWithdocfeeForChart(bts, fd, td, false, true));
        mainJSONArray.put(subArray);

        System.out.println("jSONArray1.length- = " + mainJSONArray.length());
        System.out.println("jSONArray1.toString- = " + mainJSONArray.toString());

        return mainJSONArray.toString();

    }

    public String drawChannelingMethodsChart() {
        Date current;
        Calendar cal = Calendar.getInstance();
        current = cal.getTime();
        System.out.println("date = " + current);
        Date fd = commonFunctions.getStartOfDay(current);
        Date td = commonFunctions.getEndOfDay(current);
        System.out.println("fd = " + fd);
        System.out.println("td = " + td);
        JSONArray mainJSONArray = new JSONArray();
        JSONArray subArray = new JSONArray();
        subArray.put(0, "Income Type");
        subArray.put(1, "Total");
        mainJSONArray.put(subArray);
        subArray = new JSONArray();
        subArray.put(0, "Agents");
        subArray.put(1, channelTotalByBillTypeForChart(BillType.ChannelAgent, fd, td));
        mainJSONArray.put(subArray);
        subArray = new JSONArray();
        subArray.put(0, "Walk in");
        subArray.put(1, channelTotalByBillTypeForChart(BillType.ChannelCash, fd, td)
                + channelTotalByBillTypeForChart(BillType.ChannelStaff, fd, td));
//                + channelTotalByBillTypeForChart(BillType.ChannelCash, PaymentMethod.Cheque, fd, td));
        mainJSONArray.put(subArray);
        subArray = new JSONArray();
        subArray.put(0, "Call Center");
        subArray.put(1, channelTotalByBillTypeForChart(BillType.ChannelOnCall, fd, td));
        mainJSONArray.put(subArray);

        return mainJSONArray.toString();

    }

    public String drawPharmacyIncomeChart() {
        Date current;
        Calendar cal = Calendar.getInstance();
        current = cal.getTime();
        System.out.println("date = " + current);
        Date fd = commonFunctions.getStartOfDay(current);
        Date td = commonFunctions.getEndOfDay(current);
        System.out.println("fd = " + fd);
        System.out.println("td = " + td);
        JSONArray mainJSONArray = new JSONArray();
        JSONArray subArray = new JSONArray();
        subArray.put(0, "Pharmacy");
        subArray.put(1, "Total");
        mainJSONArray.put(subArray);
        subArray = new JSONArray();
        List<Object[]> obArray = new ArrayList<>();
        obArray = fetchDepartmentSale(fd, td, null, BillType.PharmacySale);

        for (Object[] ob : obArray) {

            Department deptname = (Department) ob[0];
            subArray.put(0, deptname.getName());
            subArray.put(1, ob[1]);
            mainJSONArray.put(subArray);
            subArray = new JSONArray();
        }
        System.out.println("mainJSONArray.toString() = " + mainJSONArray.toString());
        return mainJSONArray.toString();

    }

    public String drawOpdIncomeChart() {

        Date current;
        Calendar cal = Calendar.getInstance();
        current = cal.getTime();
        System.out.println("date = " + current);
        Date fd = commonFunctions.getStartOfDay(current);
        Date td = commonFunctions.getEndOfDay(current);
        System.out.println("fd = " + fd);
        System.out.println("td = " + td);
        JSONArray mainJSONArray = new JSONArray();
        JSONArray subArray = new JSONArray();
        subArray.put(0, "Category");
        subArray.put(1, "Total");
        mainJSONArray.put(subArray);
        subArray = new JSONArray();
        List<PaymentMethod> pms = Arrays.asList(new PaymentMethod[]{PaymentMethod.Cash, PaymentMethod.Cheque, PaymentMethod.Slip, PaymentMethod.Card});

        for (Category c : bookKeepingSummery.fetchCategories(pms, fd, td, getSessionController().getInstitution())) {
            subArray.put(0, c.getName());
            subArray.put(1, bookKeepingSummery.fetchCategoryTotal(pms, fd, td, c, true, getSessionController().getInstitution()));
            mainJSONArray.put(subArray);
            subArray = new JSONArray();
        }
        System.out.println("mainJSONArray.toString() = " + mainJSONArray.toString());
        return mainJSONArray.toString();
    }

    public List<Object[]> fetchDepartmentSale(Date fromDate, Date toDate, Institution institution, BillType billType) {
        PaymentMethod[] pms = new PaymentMethod[]{PaymentMethod.Cash, PaymentMethod.Card, PaymentMethod.Cheque, PaymentMethod.Slip};
        HashMap hm = new HashMap();
        String sql = "Select b.referenceBill.department,"
                + " sum(b.netTotal) "
                + " from Bill b "
                + " where b.retired=false"
                + " and  b.billType=:bType";
        if (institution != null) {
            sql += " and b.referenceBill.department.institution=:ins ";
            hm.put("ins", institution);
        }

        sql += " and b.createdAt between :fromDate and :toDate "
                + " and b.paymentMethod in :pm"
                + " and type(b)!=:cl "
                + " group by b.referenceBill.department"
                + " order by b.referenceBill.department.name";

        hm.put("bType", billType);
        hm.put("cl", PreBill.class);

        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        hm.put("pm", Arrays.asList(pms));
        return getBillFacade().findAggregates(sql, hm, TemporalType.TIMESTAMP);

    }

    public double hospitalTotalBillByBillTypeAndFeeTypeWithdocfeeForChart(List<BillType> bts, Date fd, Date td, boolean staff, boolean vat) {

        String sql = "";
        Map m = new HashMap();

        if (vat) {
            sql = " select sum(bf.feeVat) ";
        } else {
            sql = " select sum(bf.feeValue) ";
        }
        sql += " from BillFee bf where bf.bill.retired=false ";
        if (staff) {
            sql += " and bf.fee.feeType=:ft ";
            m.put("ft", FeeType.Staff);
        } else {
            sql += " and bf.fee.feeType!=:ft ";
            m.put("ft", FeeType.Staff);
        }

        sql += " and bf.createdAt between :fromDate and :toDate  "
                + " and bf.bill.billType in :bts  ";

        m.put("bts", bts);
        m.put("fromDate", fd);
        m.put("toDate", td);
        System.out.println("sql = " + sql);
        System.out.println("m = " + m);
        double tot = getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
        System.out.println("tot = " + tot);
        return tot;

    }

    public double channelTotalByBillTypeForChart(BillType bt, Date fd, Date td) {
        //Bill b = new Bill();

        String sql = "";
        Map m = new HashMap();
        sql += "select sum(b.netTotal) from Bill b "
                + " where b.retired=false "
                + " and b.singleBillSession.sessionDate between :fromDate and :toDate  "
                + " and b.billType=:bt "
                + " and b.paidBill is not null ";

        m.put("bt", bt);
        m.put("fromDate", fd);
        m.put("toDate", td);
        System.out.println("sql = " + sql);
        System.out.println("m = " + m);
        double tot = getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
        System.out.println("tot = " + tot);
        return tot;
    }

    public List<String> datesBetween(Date fd, Date td) {

        List<String> dates = new ArrayList<>();
        SimpleDateFormat sm = new SimpleDateFormat("yyyy-MM-dd");

        String startDate = sm.format(fd);
        String endDate = sm.format(td);

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        System.out.println("start = " + start);
        System.out.println("end = " + end);
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            dates.add(String.valueOf(date.getYear()) + " - " + String.valueOf(date.getMonthOfYear()) + " - " + String.valueOf(date.getDayOfMonth()));
        }
        return dates;
    }

    public static String toJavascriptArray(String[] arr) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append("\"").append(arr[i]).append("\"");
            if (i + 1 < arr.length) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public double fetchBillsTotal(BillType[] billTypes, BillType bt, Class[] bills, Class[] nbills, Bill b, Date fd, Date td, Institution billedInstitution, Institution creditCompany, boolean withOutDocFee, boolean count, Staff staff, Speciality sp, WebUser webUser) {

        String sql;
        Map m = new HashMap();
        if (count) {
            sql = " select count(b) ";
        } else if (withOutDocFee) {
            sql = " select sum(b.netTotal-b.staffFee) ";
        } else {
            sql = " select sum(b.netTotal) ";
        }

        sql += " from Bill b "
                + " where b.retired=false ";

        if (b.getClass().equals(BilledBill.class)) {
            sql += " and b.singleBillSession.sessionDate between :fromDate and :toDate ";
        }
        if (b.getClass().equals(CancelledBill.class)) {
            sql += " and b.createdAt between :fromDate and :toDate ";
        }
        if (b.getClass().equals(RefundBill.class)) {
            sql += " and b.createdAt between :fromDate and :toDate ";
        }

        if (billTypes != null) {
            sql += " and b.billType in :bt ";
            List<BillType> bts = Arrays.asList(billTypes);
            m.put("bt", bts);
        }
        if (bt != null) {
            sql += " and b.billType=:bt ";
            m.put("bt", bt);
        }
        if (bills != null) {
            sql += " and type(b) in :class ";
            List<Class> cs = Arrays.asList(bills);
            m.put("class", cs);
        }
        if (nbills != null) {
            sql += " and type(b) not in :nclass ";
            List<Class> ncs = Arrays.asList(nbills);
            m.put("nclass", ncs);
        }
        if (b != null) {
            sql += " and type(b)=:class ";
            m.put("class", b.getClass());
        }
        if (billedInstitution != null) {
            sql += " and b.institution=:ins ";
            m.put("ins", billedInstitution);
        }
        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            m.put("cc", creditCompany);
        }
        if (staff != null) {
            sql += " and b.staff=:s ";
            m.put("s", staff);
        }
        if (webUser != null) {
            sql += " and b.creater=:wu ";
            m.put("wu", webUser);
        }
        if (sp != null) {
            sql += " and b.staff.speciality=:sp ";
            m.put("sp", sp);
        }

        m.put("fromDate", fd);
        m.put("toDate", td);
        System.err.println("Sql " + sql);
        System.out.println("m = " + m);
        if (count) {
            return getBillFacade().findLongByJpql(sql, m, TemporalType.TIMESTAMP);
        } else {
            return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
        }

    }

    public class ChartValue {

//        private List<Long> counts;
//        private List<String> dates;
        String date;
        String channel;
        String scan;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getScan() {
            return scan;
        }

        public void setScan(String scan) {
            this.scan = scan;
        }

    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public BookKeepingSummery getBookKeepingSummery() {
        return bookKeepingSummery;
    }

    public void setBookKeepingSummery(BookKeepingSummery bookKeepingSummery) {
        this.bookKeepingSummery = bookKeepingSummery;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public List<ChartValue> getChartValues() {
        return chartValues;
    }

    public void setChartValues(List<ChartValue> chartValues) {
        this.chartValues = chartValues;
    }

    public JSONArray getJsonArray() {
        return jsonArray;
    }

    public void setJsonArray(JSONArray jsonArray) {
        this.jsonArray = jsonArray;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

}
