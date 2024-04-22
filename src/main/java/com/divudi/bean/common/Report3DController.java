package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.data.hr.ReportKeyWord;

import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Item;
import com.divudi.entity.PreBill;
import com.divudi.facade.BillFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.java.CommonFunctions;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author Sniper 619
 */
@Named(value = "report3DController")
@SessionScoped
public class Report3DController implements Serializable {


    CommonFunctions commonFunctions;

    @Inject
    SessionController sessionController;

    @EJB
    ItemFacade itemFacade;
    @EJB
    BillFacade billFacade;

    ReportKeyWord reportKeyWord;

    List<ColumnModel> columnModels;
    List<String> headers;
    List<String> headers2;
    List<ItemCount> itemCounts;

    public Report3DController() {
    }

    //Methords
    public void createPharmacyItemMovment3D() {
        if (getReportKeyWord().getDepartment() == null) {
            JsfUtil.addErrorMessage("Please Select Deparment.");
            return;
        }

        if (getReportKeyWord().getCategory() == null) {
            JsfUtil.addErrorMessage("Please Select Category.");
            return;
        }

        columnModels = new ArrayList<>();
        fetchHeaders();
        fetchItemSaleMovement();

        int i = itemCounts.size();
//        //// // System.out.println("i = " + i);
        int j = headers.size();
//        //// // System.out.println("j = " + j);
        List<Double> list = new ArrayList<>();
//        //// // System.out.println("Time 1 = " + new Date());
        double totalNet = 0.0;
        for (int k = 0; k < j; k++) {
//            //// // System.out.println("k = " + k);
            double total = 0.0;
            for (int l = 0; l < i; l++) {
//                //// // System.out.println("l = " + l);
                total += itemCounts.get(l).getCounts().get(k);
            }
            list.add((double) total);
            totalNet += total;
        }
        list.add(totalNet);
        ItemCount row = new ItemCount();
        row.setCounts(list);
        itemCounts.add(row);

        Long l = 0l;
        for (String h : headers) {
            ColumnModel c = new ColumnModel();
            c.setHeader(h);
            c.setProperty(l.toString());
            columnModels.add(c);
            l++;
        }
    }

    private void fetchItemSaleMovement() {
        itemCounts = new ArrayList<>();

        for (Item i : fetchSaleItems(getReportKeyWord().getCategory(),null,getReportKeyWord().getDepartment())) {
            ItemCount row = new ItemCount();
            row.setItem(i);
            row.setStock(fetchCurrentStock(i, getReportKeyWord().getDepartment()));
            row.setCounts(fetchItemSale(i, getReportKeyWord().isBool2(), getReportKeyWord().getDepartment()));
            getItemCounts().add(row);
        }
    }

    private double fetchCurrentStock(Item i, Department dep) {
        double d = 0.0;
        Map m = new HashMap();
        String sql;
        sql = "select sum(s.stock) from Stock s where s.stock>:d "
                + " and s.itemBatch.item=:i "
                + " and s.department=:dep ";
        m.put("d", 0.0);
        m.put("dep", dep);
        m.put("i", i);
        d = getBillFacade().findDoubleByJpql(sql, m);

//        //// // System.out.println("getBillFacade().findDoubleByJpql(sql, m) = " + getBillFacade().findDoubleByJpql(sql, m));
        return d;
    }

    private List<Double> fetchItemSale(Item i, boolean count, Department d) {
        List<Double> ds = new ArrayList<>();

        String sql;
        Map m = new HashMap();

//        sql = " select FUNC('Date',pbi.billItem.bill.createdAt),sum(pbi.qty) "
//                + " from PharmaceuticalBillItem pbi "
//                + " where type(pbi.billItem.bill)=:bc "
//                + " and pbi.billItem.bill.createdAt between :fd and :td "
//                + " and pbi.billItem.bill.billType=:bt "
//                + " and pbi.billItem.bill.department=:dep ";
//
//        if (i != null) {
//            sql += " and pbi.billItem.item=:i ";
//            m.put("i", i);
//        }
//
//        sql += " group by FUNC('Date',pbi.billItem.bill.createdAt) "
//                + " order by pbi.billItem.bill.createdAt ";
//
//
//        m.put("bc", PreBill.class);
//        m.put("bt", BillType.PharmacyPre);
//        m.put("dep", d);
//        m.put("fd", getReportKeyWord().getFromDate());
//        m.put("td", getReportKeyWord().getToDate());
//        List<Object[]> objects = getBillFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
//        //// // System.out.println("objects.size() = " + objects.size());
//        for (Object[] ob : objects) {
//            Date date=(Date) ob[0];
//            //// // System.out.println("date = " + date);
//            double co=(double) ob[1];
//            //// // System.out.println("co = " + co);
//        }
        sql = " select FUNC('Month',pbi.billItem.bill.createdAt),sum(pbi.qty) "
                + " from PharmaceuticalBillItem pbi "
                + " where type(pbi.billItem.bill)=:bc "
                + " and pbi.billItem.bill.createdAt between :fd and :td "
                + " and pbi.billItem.bill.billType=:bt "
                + " and pbi.billItem.bill.department=:dep ";

        if (i != null) {
            sql += " and pbi.billItem.item=:i ";
            m.put("i", i);
        }

        sql += " group by FUNC('Month',pbi.billItem.bill.createdAt) "
                + " order by FUNC('Month',pbi.billItem.bill.createdAt) ";

        m.put("bc", PreBill.class);
        m.put("bt", BillType.PharmacyPre);
        m.put("dep", d);
        m.put("fd", getReportKeyWord().getFromDate());
        m.put("td", getReportKeyWord().getToDate());

        List<Object[]> objects = getBillFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

        double total = 0.0;
        for (String s : headers2) {
            double c = 0.0;
            for (Object[] ob : objects) {
                int in = (int) ob[0];
                double co = (double) ob[1];
//                Calendar cal = Calendar.getInstance();
//                cal.set(Calendar.MONTH, in);
//                //// // System.out.println("cal.getTime() = " + cal.getTime());
                if (Integer.parseInt(s) == in) {
                    c = co;
                    break;
                }
                total += c;
            }
            ds.add(c);
        }

        ds.add(total);


        return ds;
    }

//    private List<Double> fetchItemSale(Item i, boolean count, Department d) {
//        List<Double> ds = new ArrayList<>();
//        Date nowDate = getReportKeyWord().getFromDate();
//        double netTot = 0.0;
//        while (nowDate.before(getReportKeyWord().getToDate())) {
//            String formatedDate;
//            Date fd;
//            Date td;
//            if (getReportKeyWord().isBool1()) {
//                fd = commonFunctions.getStartOfDay(nowDate);
//                td = commonFunctions.getEndOfDay(nowDate);
//
//                DateFormat df = new SimpleDateFormat("yy MM dd");
//                formatedDate = df.format(fd);
//                //// // System.out.println("formatedDate = " + formatedDate);
//
//            } else {
//                fd = commonFunctions.getStartOfMonth(nowDate);
//                td = commonFunctions.getEndOfMonth(commonFunctions.getStartOfMonth(nowDate));
//
//                DateFormat df = new SimpleDateFormat("yy MM");
//                formatedDate = df.format(fd);
//                //// // System.out.println("formatedDate = " + formatedDate);
//            }
//            double tmpTot = fetchItemQty(i, fd, td, count, d);
//            //// // System.out.println("tmpTot = " + tmpTot);
//
//            ds.add(tmpTot);
//            netTot += tmpTot;
//
//            Calendar cal = Calendar.getInstance();
//            cal.setTime(nowDate);
//            if (getReportKeyWord().isBool1()) {
//                cal.add(Calendar.DATE, 1);
//            } else {
//                cal.add(Calendar.MONTH, 1);
//            }
//            nowDate = cal.getTime();
//            //// // System.out.println("nowDate = " + nowDate);
//        }
//        ds.add(netTot);
//        return ds;
//    }
    public double fetchItemQty(Item i, Date fd, Date td, boolean count, Department dep) {
        String sql;
        Map m = new HashMap();

        sql = " select ";
        if (count) {
            sql += " pbi.qty ";
        } else {
            sql += " sum(pbi.itemBatch.purcahseRate*pbi.qty) ";
        }
        sql += " from PharmaceuticalBillItem pbi "
                + " where type(pbi.billItem.bill)=:bc "
                + " and pbi.billItem.bill.createdAt between :fd and :td "
                + " and pbi.billItem.bill.billType=:bt "
                + " and pbi.billItem.bill.department=:dep ";

        if (i != null) {
            sql += " and pbi.billItem.item=:i ";
            m.put("i", i);
        }

        m.put("bc", PreBill.class);
        m.put("bt", BillType.PharmacyPre);
        m.put("dep", dep);
        m.put("fd", fd);
        m.put("td", td);

        if (count) {
//            long d = getBillFacade().findAggregateLong(sql, m, TemporalType.TIMESTAMP);
//            //// // System.out.println("d count = " + d);
//            d = getBillFacade().findLongByJpql(sql, m, TemporalType.TIMESTAMP);
//            //// // System.out.println("d count = " + d);
            double d = getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
            return d;
        } else {
            double d = getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
            return d;
        }
    }

    private List<Item> fetchItems(Category category) {
        List<Item> items = new ArrayList<>();
        Map m = new HashMap();
        String sql = "select c from Amp c "
                + " where c.retired=false"
                + " and c.category=:cat "
                + " and (c.departmentType is null "
                + " or c.departmentType=:dep) "
                + " order by c.code ";

        m.put("dep", DepartmentType.Pharmacy);
        m.put("cat", category);

//        items = getItemFacade().findByJpql(sql, m, 10);
        items = getItemFacade().findByJpql(sql, m);

        return items;
    }

    private List<Item> fetchSaleItems(Category category,Item i,Department d) {
        List<Item> items = new ArrayList<>();
        Map m = new HashMap();
        String sql;
        sql = " select pbi.billItem.item "
                + " from PharmaceuticalBillItem pbi "
                + " where type(pbi.billItem.bill)=:bc "
                + " and pbi.billItem.bill.createdAt between :fd and :td "
                + " and pbi.billItem.bill.billType=:bt "
                + " and pbi.billItem.bill.department=:dep ";

        if (i != null) {
            sql += " and pbi.billItem.item=:i ";
            m.put("i", i);
        }

        sql += " group by pbi.billItem.item "
                + " order by pbi.billItem.item.code ";

        m.put("bc", PreBill.class);
        m.put("bt", BillType.PharmacyPre);
        m.put("dep", d);
        m.put("fd", getReportKeyWord().getFromDate());
        m.put("td", getReportKeyWord().getToDate());
        items = getItemFacade().findByJpql(sql, m);

        return items;
    }

    private List<String> fetchHeaders() {
        headers = new ArrayList<>();
        headers2 = new ArrayList<>();
        Date nowDate = getReportKeyWord().getFromDate();
        double netTot = 0.0;
        while (nowDate.before(getReportKeyWord().getToDate())) {
            String formatedDate;
            Date fd;
            Date td;
            if (getReportKeyWord().isBool1()) {
                fd = commonFunctions.getStartOfDay(nowDate);
                td = commonFunctions.getEndOfDay(nowDate);

                DateFormat df = new SimpleDateFormat(" yy MM dd ");
                formatedDate = df.format(fd);

            } else {
                fd = commonFunctions.getStartOfMonth(nowDate);
                td = commonFunctions.getEndOfMonth(nowDate);

                DateFormat df = new SimpleDateFormat(" yyyy MMM ");
                formatedDate = df.format(fd);
                df = new SimpleDateFormat("MM");
                headers2.add(df.format(fd));
            }
            headers.add(formatedDate);

            Calendar cal = Calendar.getInstance();
            cal.setTime(nowDate);
            if (getReportKeyWord().isBool1()) {
                cal.add(Calendar.DATE, 1);
            } else {
                cal.add(Calendar.MONTH, 1);
            }
            nowDate = cal.getTime();
        }
        headers.add("Total");

        return headers;
    }

    //Methords
    //Sub Classes
    public class ItemCount {

        Item item;
        double stock;
        List<Double> counts;

        public Item getItem() {
            return item;
        }

        public void setItem(Item item) {
            this.item = item;
        }

        public List<Double> getCounts() {
            return counts;
        }

        public void setCounts(List<Double> counts) {
            this.counts = counts;
        }

        public double getStock() {
            return stock;
        }

        public void setStock(double stock) {
            this.stock = stock;
        }

    }

    public class ColumnModel {

        private String header;
        private String property;

        public String getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }
    }

    //Sub Classes
    //Getters and Setters
    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
            reportKeyWord.setDepartment(getSessionController().getDepartment());
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public List<ColumnModel> getColumnModels() {
        return columnModels;
    }

    public void setColumnModels(List<ColumnModel> columnModels) {
        this.columnModels = columnModels;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<ItemCount> getItemCounts() {
        return itemCounts;
    }

    public void setItemCounts(List<ItemCount> itemCounts) {
        this.itemCounts = itemCounts;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

}
