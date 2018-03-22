package com.divudi.bean.common;

import com.divudi.bean.channel.ChannelReportTempController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Item;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;

/**
 *
 * @author Sniper 619
 */
@Named(value = "report3DController")
@SessionScoped
public class Report3DController implements Serializable {

    @EJB
    CommonFunctions commonFunctions;

    ReportKeyWord reportKeyWord;

    List<ColumnModel> columnModels;
    List<String> headers;
    List<ItemCount> itemCounts;

    public Report3DController() {
    }

    //Methords
    public void createPharmacyItemMovment3D() {
        if (getReportKeyWord().getCategory() == null) {
            JsfUtil.addErrorMessage("Please Select Category.");
            return;
        }

        columnModels = new ArrayList<>();
        fetchHeaders();
        fetchItemSaleMovement();

        int i = itemCounts.size();
        int j = headers.size();
        List<Double> list = new ArrayList<>();
        System.out.println("Time 1 = " + new Date());
        for (int k = 0; k < j; k++) {
            double total = 0.0;
            for (int l = 0; l < i; l++) {
                total += itemCounts.get(l).getCounts().get(k);
            }
            list.add((double) total);
        }
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
//        ItemController
//        ItemCount row = new ItemCount();
//        row.setItem(null);
//        row.setCounts(fetchChannelDocCountsRows(null, null, new BillType[]{BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent}, false, true, getReportKeyWord().getStaff(), byDate, null));
//        System.out.println("row.getCounts().size() = " + row.getCounts().size());
//        channelDoctorCountsRows.add(row);

    }

    private List<String> fetchHeaders() {
        headers = new ArrayList<>();
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
                System.out.println("formatedDate = " + formatedDate);

            } else {
                fd = commonFunctions.getStartOfMonth(nowDate);
                td = commonFunctions.getEndOfMonth(nowDate);

                DateFormat df = new SimpleDateFormat(" yyyy MMM ");
                formatedDate = df.format(fd);
                System.out.println("formatedDate = " + formatedDate);
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
            System.out.println("nowDate = " + nowDate);
        }
        headers.add("Total");

        return headers;
    }

    //Methords
    //Sub Classes
    public class ItemCount {

        Item item;
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
        if (reportKeyWord != null) {
            reportKeyWord = new ReportKeyWord();
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

}
