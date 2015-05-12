/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.data.dataStructure.ServiceSummeryData;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.BillItem;
import com.divudi.entity.Service;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ServiceFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named; import javax.ejb.EJB;
import javax.persistence.TemporalType;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class ServiceMonthSummeryController implements Serializable {

    @EJB
    private CommonFunctions commonFunctions;
    @EJB
    private BillFacade billFacade;
    @EJB
    private ServiceFacade serviceFacade;
    @EJB
    private BillItemFacade billItemFacade;
    private Date fromDate;
    private Date toDate;
    private List<ServiceSummeryData> items;
    private List<ServiceSummeryData> itemDetails;
    private List<Service> services;

    /**
     * Creates a new instance of CashierReportController
     */
    public ServiceMonthSummeryController() {
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public List<ServiceSummeryData> getItems() {
        items = new ArrayList<ServiceSummeryData>();
        for (Service w : getServices()) {
            ServiceSummeryData temp = new ServiceSummeryData();
            temp.setService(w);
            setCountTotal(temp, w);
            items.add(temp);
        }

        return items;
    }

    private void setCountTotal(ServiceSummeryData is, Service w) {
        String sql;
        Map temMap = new HashMap();
        sql = "select b from BillItem b where "
                + "b.item.id=" + w.getId() + " and  b.createdAt between :fromDate and :toDate";
        ////System.out.println(w.getId());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        List<BillItem> temps = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        ////System.out.println("Size : " + temps.size());

        double tot = 0.0;
        for (BillItem b : temps) {
            tot += b.getNetValue();
        }
        ////System.out.println("Total : " + tot);
        is.setCount(temps.size());
        is.setTotal(tot);
    }

    public void setItems(List<ServiceSummeryData> items) {
        this.items = items;
    }

    public List<Service> getServices() {

        String sql;
        Map temMap = new HashMap();
        sql = "select ins from Service ins where ins.id in (select bi.item.id from BillItem bi where bi.bill.id in(select b.id from Bill b where b.billDate between :fromDate and :toDate))";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        services = getServiceFacade().findBySQL(sql, temMap);
        ////System.out.println("Services : "+services.size());
        if (services == null) {
            services = new ArrayList<Service>();

        }

        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    public ServiceFacade getServiceFacade() {
        return serviceFacade;
    }

    public void setServiceFacade(ServiceFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public List<ServiceSummeryData> getItemDetails() {
        ////System.out.println("1");
        itemDetails = new ArrayList<ServiceSummeryData>();

        for (Service w : getServices()) {
            ////System.out.println("2");
            ServiceSummeryData temp = new ServiceSummeryData();
            temp.setService(w);
            setBillItems(temp, w);
            itemDetails.add(temp);
        }

        return itemDetails;
    }

    private void setBillItems(ServiceSummeryData t, Service w) {
        String sql;
        Map temMap = new HashMap();
        sql = "select b from BillItem b where "
                + "b.item.id=" + w.getId() + " and  b.createdAt between :fromDate and :toDate";
        temMap.put("toDate",getToDate());
        temMap.put("fromDate", getFromDate());
        List<BillItem> temps = getBillItemFacade().findBySQL(sql, temMap);
        t.setBillItems(temps);

    }

    public void setItemDetails(List<ServiceSummeryData> itemDetails) {
        this.itemDetails = itemDetails;
    }
}
