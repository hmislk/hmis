/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.bean.report.BookKeepingSummery;
import com.divudi.bean.report.CommonReport;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;

import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.PreBill;
import com.divudi.entity.RefundBill;
import com.divudi.entity.Service;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.entity.lab.Investigation;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.java.CommonFunctions;
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


    private CommonFunctions commonFunctions;
    @EJB
    private BillFacade billFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @Inject
    private BillBeanController billBean;

    @Inject
    private BookKeepingSummery bookKeepingSummery;
    @Inject
    private SessionController sessionController;
    @Inject
    private CommonReport commonReport;

    private List<ChartValue> chartValues;
    private JSONArray jsonArray;
    private Date fromDate;
    private Date toDate;

    /**
     * Creates a new instance of GoogleChartController
     */
    public GoogleChartController() {
    }

    public String drawChannelAndScanCountChart() {
        Calendar cal = Calendar.getInstance();
        Date toDate = cal.getTime();
        cal.add(Calendar.MONTH, -1);
        Date fromDate = cal.getTime();

        JSONArray jSONArray1 = new JSONArray();
        JSONObject cols = new JSONObject();
        JSONArray arrays = new JSONArray();
        JSONObject ob = new JSONObject();

        arrays.put(0, "Date");
        arrays.put(1, "Channel Count");
        arrays.put(2, "Scan Count");
        jSONArray1.put(arrays);
        Date nowDate = fromDate;
        double btot = 0.0;
        double ctot = 0.0;
        double rtot = 0.0;
        double netTot = 0.0;

        while (nowDate.before(toDate) || nowDate.equals(toDate)) {
            JSONObject in = new JSONObject();
            JSONObject out = new JSONObject();
            JSONArray inarr = new JSONArray();
            String formatedDate;
            Date fd = commonFunctions.getStartOfDay(nowDate);
            Date td = commonFunctions.getEndOfDay(nowDate);
            //// // System.out.println("td = " + td);
            //// // System.out.println("fd = " + fd);

            DateFormat df = new SimpleDateFormat("yy MMM dd");
            formatedDate = df.format(fd);

            double ctot1 = fetchBillsTotal(new BillType[]{BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent}, null, null, null, new CancelledBill(), fd, td, null, null, false, true, null, null, null);
            double rtot1 = fetchBillsTotal(new BillType[]{BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent}, null, null, null, new RefundBill(), fd, td, null, null, false, true, null, null, null);
            double btot1 = fetchBillsTotal(new BillType[]{BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent}, null, null, null, new BilledBill(), fd, td, null, null, false, true, null, null, null);
            ctot += ctot1;
            rtot += rtot1;
            btot += btot1;
            netTot = btot1 - (ctot1 + rtot1);
            BillType[] billTypes = {BillType.ChannelCash,
                BillType.ChannelOnCall,
                BillType.ChannelPaid};
            List<BillType> bts = Arrays.asList(billTypes);

            inarr.put(0, formatedDate);
            inarr.put(1, netTot);
            inarr.put(2, (countBillOfScan(new BilledBill(), FeeType.Service, bts, fd, td, true)
                    - (countBillOfScan(new CancelledBill(), FeeType.Service, bts, fd, td, true)
                    + countBillOfScan(new RefundBill(), FeeType.Service, bts, fd, td, true))));
            jSONArray1.put(inarr);

            Calendar cal1 = Calendar.getInstance();
            cal1.setTime(nowDate);
            cal1.add(Calendar.DATE, 1);
            nowDate = cal1.getTime();

        }


        return jSONArray1.toString();

    }

    public void drawAllChat(){
        drawChannelAndScanCountChart();
        drawChannelingMethodsChart();
        drawCollectionCenterInvestigationCountChart();
        drawInwardCash();
        drawInwardCredit();
        drawOpdIncomeChart();
        drawPharmacyIncomeChart();
        drawPiechartDailyIncome();
        drawTotalIncomeBySectionChart();
    }

    public String drawPiechartDailyIncome() {
//        Date current;
//        Calendar cal = Calendar.getInstance();
//        current = cal.getTime();
//        //// // System.out.println("date = " + current);

//        Date fd = commonFunctions.getStartOfDay(current);
//        Date td = commonFunctions.getEndOfDay(current);
        Date fd;
        Date td;
        if (fromDate == null || toDate == null) {
            Calendar cal = Calendar.getInstance();
            Date current = cal.getTime();
            fd = commonFunctions.getStartOfDay(current);
            td = commonFunctions.getEndOfDay(current);
        } else {
            fd = fromDate;
            td = toDate;
        }
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


        return mainJSONArray.toString();

    }

    public String drawChannelingMethodsChart() {
//        Date current;
//        Calendar cal = Calendar.getInstance();
//        current = cal.getTime();
//        //// // System.out.println("date = " + current);
//        Date fd = commonFunctions.getStartOfDay(current);
//        Date td = commonFunctions.getEndOfDay(current);
        Date fd;
        Date td;
        if (fromDate == null || toDate == null) {
            Calendar cal = Calendar.getInstance();
            Date current = cal.getTime();
            fd = commonFunctions.getStartOfDay(current);
            td = commonFunctions.getEndOfDay(current);
        } else {
            fd = fromDate;
            td = toDate;
        }
        FeeType ft = FeeType.OwnInstitution;
        boolean sessionDate = true;
        BillType[] billTypes = {BillType.ChannelCash,
            BillType.ChannelOnCall,
            BillType.ChannelStaff,
            BillType.ChannelAgent,};
        List<BillType> bts = Arrays.asList(billTypes);
        JSONArray mainJSONArray = new JSONArray();
        JSONArray subArray = new JSONArray();
        subArray.put(0, "Channel Method");
        subArray.put(1, "count");
        mainJSONArray.put(subArray);
        subArray = new JSONArray();
        for (BillType bt : bts) {

            subArray.put(0, bt.getLabel());
            subArray.put(1, countBillByBillTypeAndFeeType(new BilledBill(), ft, bt, fd, td, sessionDate)
                    - (countBillByBillTypeAndFeeType(new CancelledBill(), ft, bt, fd, td, sessionDate)
                    + countBillByBillTypeAndFeeType(new RefundBill(), ft, bt, fd, td, sessionDate)));
            mainJSONArray.put(subArray);
            subArray = new JSONArray();
        }
        return mainJSONArray.toString();

    }

    public String drawPharmacyIncomeChart() {
//        Date current;
//        Calendar cal = Calendar.getInstance();
//        current = cal.getTime();
//        //// // System.out.println("date = " + current);
//        Date fd = commonFunctions.getStartOfDay(current);
//        Date td = commonFunctions.getEndOfDay(current);
        Date fd;
        Date td;
        if (fromDate == null || toDate == null) {
            Calendar cal = Calendar.getInstance();
            Date current = cal.getTime();
            fd = commonFunctions.getStartOfDay(current);
            td = commonFunctions.getEndOfDay(current);
        } else {
            fd = fromDate;
            td = toDate;
        }
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
        return mainJSONArray.toString();

    }

    public String drawOpdIncomeChart() {
//
//        Date current;
//        Calendar cal = Calendar.getInstance();
//        current = cal.getTime();
//        //// // System.out.println("date = " + current);
//        Date fd = commonFunctions.getStartOfDay(current);
//        Date td = commonFunctions.getEndOfDay(current);
        Date fd;
        Date td;
        if (fromDate == null || toDate == null) {
            Calendar cal = Calendar.getInstance();
            Date current = cal.getTime();
            fd = commonFunctions.getStartOfDay(current);
            td = commonFunctions.getEndOfDay(current);
        } else {
            fd = fromDate;
            td = toDate;
        }
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
        return mainJSONArray.toString();
    }

    public String drawCollectionCenterInvestigationCountChart() {
//        //// // System.out.println("this drawCollectionCenterInvestigationCountChart");
//        Date current;
//        Calendar cal = Calendar.getInstance();
//        current = cal.getTime();
//        //// // System.out.println("date = " + current);
//        Date fd = commonFunctions.getStartOfDay(current);
//        Date td = commonFunctions.getEndOfDay(current);
        Date fd;
        Date td;
        if (fromDate == null || toDate == null) {
            Calendar cal = Calendar.getInstance();
            Date current = cal.getTime();
            fd = commonFunctions.getStartOfDay(current);
            td = commonFunctions.getEndOfDay(current);
        } else {
            fd = fromDate;
            td = toDate;
        }
        //// // System.out.println("fd = " + fd);
        //// // System.out.println("td = " + td);
        commonReport.setFromDate(fd);
        commonReport.setToDate(td);
        JSONArray mainJSONArray = new JSONArray();
        JSONArray subArray = new JSONArray();
        subArray.put(0, "Collection Center");
        subArray.put(1, "Count");
        mainJSONArray.put(subArray);
        subArray = new JSONArray();
        BillType billTypes[] = {BillType.LabBill, BillType.CollectingCentreBill};
        List<BillType> types = Arrays.asList(billTypes);
        for (Institution i : commonReport.fetchCollectingCenters(billTypes)) {
            subArray.put(0, i.getName());
            subArray.put(1, countBillsTotalbyInstitution(new BilledBill(), i, types, fd, td)
                    - (countBillsTotalbyInstitution(new CancelledBill(), i, types, fd, td)
                    + countBillsTotalbyInstitution(new RefundBill(), i, types, fd, td)));
            mainJSONArray.put(subArray);
            subArray = new JSONArray();
        }
        if (mainJSONArray.length()<2) {
            mainJSONArray = new JSONArray();
        }
        
        return mainJSONArray.toString();
    }

    public String calInstitutionIncome() {
        Date current;
        Calendar cal = Calendar.getInstance();
        current = cal.getTime();
        Date fd = commonFunctions.getStartOfDay(current);
        Date td = commonFunctions.getEndOfDay(current);

        JSONArray mainJSONArray = new JSONArray();
        JSONArray subArray = new JSONArray();
        subArray.put(0, "Collection Center");
        subArray.put(1, "Hospital Total");
        subArray.put(2, "Collection Center Total");
        subArray.put(3, "Vat Total");
        mainJSONArray.put(subArray);
        subArray = new JSONArray();

        BillType billTypes[] = {BillType.LabBill, BillType.CollectingCentreBill};
        for (Institution i : fetchCollectingCenters(billTypes, fd, td)) {

            List<Bill> bs = new ArrayList<>();
            bs = commonReport.getBillList(billTypes, i);

            double totVat = 0.0;
            double tothos = 0.0;
            double totcc = 0.0;
            for (Bill b : bs) {
                commonReport.createCollectingCenterfees(b);
                tothos += b.getTransTotalWithOutCCFee();
                totcc += b.getTransTotalCCFee();
                totVat += b.getVat();

            }
            subArray.put(0, i.getName());
            subArray.put(1, tothos);
            subArray.put(2, totcc);
            subArray.put(3, totVat);
            mainJSONArray.put(subArray);
            subArray = new JSONArray();
        }
        return mainJSONArray.toString();

    }

    public double countBillByBillTypeAndFeeType(Bill bill, FeeType ft, BillType bt, Date fd, Date td, boolean sessoinDate) {

        String sql;
        Map m = new HashMap();

        sql = " select count(distinct(bf.bill)) from BillFee  bf where "
                + " bf.bill.retired=false "
                + " and bf.bill.billType=:bt "
                + " and type(bf.bill)=:class "
                + " and bf.fee.feeType =:ft "
                + " and bf.feeValue>0 ";

        if (bill.getClass().equals(CancelledBill.class)) {
            sql += " and bf.bill.cancelled=true";
        }
        if (bill.getClass().equals(RefundBill.class)) {
            sql += " and bf.bill.refunded=true";
        }

        if (ft == FeeType.OwnInstitution) {
            sql += " and bf.fee.name =:fn ";
            m.put("fn", "Hospital Fee");
        }

//        if (paid) {
//            sql += " and bf.bill.paidBill is not null "
//                    + " and bf.bill.paidAmount!=0 ";
//        }
        if (sessoinDate) {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and bf.bill.singleBillSession.sessionDate between :fd and :td ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and bf.bill.cancelledBill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and bf.bill.refundedBill.createdAt between :fd and :td ";
            }
        } else {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and bf.bill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and bf.bill.cancelledBill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and bf.bill.refundedBill.createdAt between :fd and :td ";
            }

        }

        m.put("fd", fd);
        m.put("td", td);
        m.put("class", BilledBill.class);
        m.put("ft", ft);
        m.put("bt", bt);
//        m.put("fn", "Scan Fee");

        double d = getBillFeeFacade().findAggregateLong(sql, m, TemporalType.TIMESTAMP);

        return d;
    }

    public double countBillOfScan(Bill bill, FeeType ft, List<BillType> bts, Date fd, Date td, boolean sessoinDate) {

        String sql;
        Map m = new HashMap();

        sql = " select count(distinct(bf.bill)) from BillFee  bf where "
                + " bf.bill.retired=false "
                + " and bf.bill.billType in :bt "
                + " and type(bf.bill)=:class "
                + " and bf.fee.feeType =:ft "
                + " and bf.feeValue>0 ";

        if (bill.getClass().equals(CancelledBill.class)) {
            sql += " and bf.bill.cancelled=true";
        }
        if (bill.getClass().equals(RefundBill.class)) {
            sql += " and bf.bill.refunded=true";
        }

        if (ft == FeeType.OwnInstitution) {
            sql += " and bf.fee.name =:fn ";
            m.put("fn", "Hospital Fee");
        }

//        if (paid) {
//            sql += " and bf.bill.paidBill is not null "
//                    + " and bf.bill.paidAmount!=0 ";
//        }
        if (sessoinDate) {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and bf.bill.singleBillSession.sessionDate between :fd and :td ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and bf.bill.cancelledBill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and bf.bill.refundedBill.createdAt between :fd and :td ";
            }
        } else {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and bf.bill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and bf.bill.cancelledBill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and bf.bill.refundedBill.createdAt between :fd and :td ";
            }

        }

        m.put("fd", fd);
        m.put("td", td);
        m.put("class", BilledBill.class);
        m.put("ft", ft);
        m.put("bt", bts);
//        m.put("fn", "Scan Fee");

        double d = getBillFeeFacade().findAggregateLong(sql, m, TemporalType.TIMESTAMP);

        return d;
    }

    public double countBillsTotalbyInstitution(Bill bill, Institution i, List<BillType> bts, Date fd, Date td) {

        String sql;
        Map m = new HashMap();

        sql = " select count(distinct(bf.bill)) from BillFee  bf where "
                + " bf.bill.retired=false "
                + " and bf.bill.billType in :bt "
                + " and type(bf.bill)=:class "
                + " and bf.bill.institution=:ins";

        if (bill.getClass().equals(CancelledBill.class)) {
            sql += " and bf.bill.cancelled=true";
        }
        if (bill.getClass().equals(RefundBill.class)) {
            sql += " and bf.bill.refunded=true";
        }

        sql += " and bf.bill.createdAt between :fd and :td ";

        m.put("fd", fd);
        m.put("td", td);
        m.put("class", BilledBill.class);
        m.put("bt", bts);
        m.put("ins", i);
//        m.put("fn", "Scan Fee");

        double d = getBillFeeFacade().findAggregateLong(sql, m, TemporalType.TIMESTAMP);

        return d;
    }

    public List<Institution> fetchCollectingCenters(BillType[] bts, Date fd, Date td) {
        Map m = new HashMap();
        String sql = "select distinct(b.fromInstitution) from Bill b "
                + " where b.billType in :bTypes "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.fromInstitution is not null "
                + " order by b.fromInstitution.name ";

        m.put("toDate", td);
        m.put("fromDate", fd);
        m.put("bTypes", Arrays.asList(bts));

        return getInstitutionFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
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

    public double fetchPharmacyTotalSale(Date fromDate, Date toDate, Institution institution, BillType billType) {
        PaymentMethod[] pms = new PaymentMethod[]{PaymentMethod.Cash, PaymentMethod.Card, PaymentMethod.Cheque, PaymentMethod.Slip};
        HashMap hm = new HashMap();
        String sql = "select sum(b.netTotal) "
                + " from Bill b "
                + " where b.retired=false"
                + " and  b.billType=:bType";
        if (institution != null) {
            sql += " and b.referenceBill.department.institution=:ins ";
            hm.put("ins", institution);
        }

        sql += " and b.createdAt between :fromDate and :toDate "
                + " and b.paymentMethod in :pm"
                + " and type(b)!=:cl ";

        hm.put("bType", billType);
        hm.put("cl", PreBill.class);

        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        hm.put("pm", Arrays.asList(pms));
        return getBillFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

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
        double tot = getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
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
        double tot = getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
        return tot;
    }

    public List<String> datesBetween(Date fd, Date td) {

        List<String> dates = new ArrayList<>();
        SimpleDateFormat sm = new SimpleDateFormat("yyyy-MM-dd");

        String startDate = sm.format(fd);
        String endDate = sm.format(td);

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

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
        if (count) {
            return getBillFacade().findLongByJpql(sql, m, TemporalType.TIMESTAMP);
        } else {
            return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
        }

    }

    public String drawInwardCash() {
//        Date current;
//        Calendar cal = Calendar.getInstance();
//        current = cal.getTime();
//        //// // System.out.println("date = " + current);
//        Date fd = commonFunctions.getStartOfDay(current);
//        Date td = commonFunctions.getEndOfDay(current);
        Date fd;
        Date td;
        if (fromDate == null || toDate == null) {
            Calendar cal = Calendar.getInstance();
            Date current = cal.getTime();
            fd = commonFunctions.getStartOfDay(current);
            td = commonFunctions.getEndOfDay(current);
        } else {
            fd = fromDate;
            td = toDate;
        }

        JSONArray mainJSONArraycash = new JSONArray();
        JSONArray subArraycash = new JSONArray();
        subArraycash.put(0, "Admission Type");
        subArraycash.put(1, "Total Income");
        mainJSONArraycash.put(subArraycash);
        subArraycash = new JSONArray();
        // double grantDbl = 0.0;
        List<Object[]> list = getBillBean().calInwardPaymentTotal(fd, td, sessionController.getInstitution());

        for (Object[] obj : list) {
            AdmissionType admissionType = (AdmissionType) obj[0];

            PaymentMethod paymentMethod = (PaymentMethod) obj[1];
            if (paymentMethod == PaymentMethod.Cash) {
                subArraycash.put(0, admissionType.getName());
                subArraycash.put(1, (Double) obj[2]);
                mainJSONArraycash.put(subArraycash);
                subArraycash = new JSONArray();
            }

        }
        return mainJSONArraycash.toString();
    }

    public String drawInwardCredit() {
//        Date current;
//        Calendar cal = Calendar.getInstance();
//        current = cal.getTime();
//        //// // System.out.println("date = " + current);
//        Date fd = commonFunctions.getStartOfDay(current);
//        Date td = commonFunctions.getEndOfDay(current);
        Date fd;
        Date td;
        if (fromDate == null || toDate == null) {
            Calendar cal = Calendar.getInstance();
            Date current = cal.getTime();
            fd = commonFunctions.getStartOfDay(current);
            td = commonFunctions.getEndOfDay(current);
        } else {
            fd = fromDate;
            td = toDate;
        }

        JSONArray mainJSONArraycredit = new JSONArray();
        JSONArray subArraycredit = new JSONArray();
        subArraycredit.put(0, "Admission Type");
        subArraycredit.put(1, "Total Income");
        mainJSONArraycredit.put(subArraycredit);
        subArraycredit = new JSONArray();
        // double grantDbl = 0.0;
        List<Object[]> list = getBillBean().calInwardPaymentTotal(fd, td, sessionController.getInstitution());

        for (Object[] obj : list) {
            AdmissionType admissionType = (AdmissionType) obj[0];

            PaymentMethod paymentMethod = (PaymentMethod) obj[1];
            if (paymentMethod == PaymentMethod.Credit) {

                subArraycredit.put(0, admissionType.getName());
                subArraycredit.put(1, (Double) obj[2]);
                mainJSONArraycredit.put(subArraycredit);
                subArraycredit = new JSONArray();

            }

//            //HEADER
//            String3Value2 newRow = new String3Value2();
//            newRow.setString1(admissionType.getName() + " " + paymentMethod + " : ");
//            newRow.setSummery(true);
//
////            if (grantDbl != 0) {
//            getInwardCollections().add(newRow);
////            }
            //BILLS
//            for (Bill b : getBillBean().fetchInwardPaymentBills(admissionType, paymentMethod, fromDate, toDate, institution)) {
////                System.err.println("Bills "+b);
//                newRow = new String3Value2();
//                newRow.setString1(b.getPatientEncounter().getBhtNo());
//                newRow.setString2(b.getInsId());
//                newRow.setString3(b.getPatientEncounter().getPatient().getPerson().getName());
//
//                Double dbl = b.getNetTotal();
//                newRow.setValue1(dbl);
//
//                getInwardCollections().add(newRow);
//            }
            //FOOTER
//            newRow = new String3Value2();
//            newRow.setString1(admissionType.getName() + " " + paymentMethod + " Total : ");
//            newRow.setSummery(true);
//
//            newRow.setValue2(grantDbl);
//
////            if (grantDbl != 0) {
//            getInwardCollections().add(newRow);
////            }}
        }

        return mainJSONArraycredit.toString();
    }

    public String drawTotalIncomeBySectionChart() {
        double d = 0.0;
        double tot_pharma = 0.0;
        double tot_channel = 0.0;
        double tot_opd = 0.0;
        double tot_inward = 0.0;
        double tot_lab = 0.0;
//        Date current;
//        Calendar cal = Calendar.getInstance();
//        current = cal.getTime();
//        //// // System.out.println("date = " + current);
//        Date fd = commonFunctions.getStartOfDay(current);
//        Date td = commonFunctions.getEndOfDay(current);
        Date fd;
        Date td;
        if (fromDate == null || toDate == null) {
            Calendar cal = Calendar.getInstance();
            Date current = cal.getTime();
            fd = commonFunctions.getStartOfDay(current);
            td = commonFunctions.getEndOfDay(current);
        } else {
            fd = fromDate;
            td = toDate;
        }
        //// // System.out.println("fd = " + fd);
        //// // System.out.println("td = " + td);
        JSONArray mainJSONArray = new JSONArray();
        JSONArray subArray = new JSONArray();
        subArray.put(0, "Income Type");
        subArray.put(1, "Total Income");
        mainJSONArray.put(subArray);
        subArray = new JSONArray();
        tot_pharma = fetchPharmacyTotalSale(fd, td, null, BillType.PharmacySale);
        tot_channel = (fetchBillsTotal(new BillType[]{BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent}, null, null, null, new BilledBill(), fd, td, null, null, false, false, null, null, null)
                - (fetchBillsTotal(new BillType[]{BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent}, null, null, null, new RefundBill(), fd, td, null, null, false, false, null, null, null)
                + fetchBillsTotal(new BillType[]{BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent}, null, null, null, new CancelledBill(), fd, td, null, null, false, false, null, null, null)));
//        List<PaymentMethod> pms = Arrays.asList(new PaymentMethod[]{PaymentMethod.Cash, PaymentMethod.Cheque, PaymentMethod.Slip, PaymentMethod.Card});
//        for (Category c : bookKeepingSummery.fetchCategories(pms, fd, td, getSessionController().getInstitution())) {
//            d = bookKeepingSummery.fetchCategoryTotal(pms, fd, td, c, true, getSessionController().getInstitution());
//        }
//        tot_opd += d;
        tot_opd = fetchOpdAndLabTotalIncome(fd, td, true);
        tot_lab = fetchOpdAndLabTotalIncome(fd, td, false);
        tot_inward = getBillBean().calInwardPaymentTotalValue(fd, td, sessionController.getInstitution());
        subArray.put(0, "Channel Income");
        subArray.put(1, tot_channel);
        mainJSONArray.put(subArray);
        subArray = new JSONArray();
        subArray.put(0, "OPD Income");
        subArray.put(1, tot_opd);
        mainJSONArray.put(subArray);
        subArray = new JSONArray();
        subArray.put(0, "Pharmacy Income");
        subArray.put(1, tot_pharma);
        mainJSONArray.put(subArray);
        subArray = new JSONArray();
        subArray.put(0, "Inward Income");
        subArray.put(1, tot_inward);
        mainJSONArray.put(subArray);
        subArray = new JSONArray();
        subArray.put(0, "Lab Income");
        subArray.put(1, tot_lab);
        mainJSONArray.put(subArray);
        subArray = new JSONArray();
        //// // System.out.println("channel" + tot_channel);
        //// // System.out.println("opd" + tot_opd);

        return mainJSONArray.toString();
    }

    public double fetchOpdAndLabTotalIncome(Date fd, Date td, boolean service) {
        String sql = "";
        Map m = new HashMap();
        BillItem bi;
        sql = "select sum(bi.netValue) from BillItem bi"
                + " where bi.retired=false "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (service) {
            sql += " and type(bi.item)=:btp ";

            m.put("btp", Service.class);
        } else {
            sql += " and type(bi.item)=:btp ";
            m.put("btp", Investigation.class);

        }
        m.put("fromDate", fd);
        m.put("toDate", td);
        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public CommonReport getCommonReport() {
        return commonReport;
    }

    public void setCommonReport(CommonReport commonReport) {
        this.commonReport = commonReport;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
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
