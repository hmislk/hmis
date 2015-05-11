/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.report;

import com.divudi.data.dataStructure.InvestigationSummeryData;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.BillItem;
import com.divudi.entity.Item;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.ItemFacade;
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
import javax.enterprise.context.RequestScoped;
import javax.persistence.TemporalType;

/**
 *
 * @author Buddhika
 */
@Named
@RequestScoped
public class InvestigationMonthSummeryController implements Serializable {

    @Inject
    private CommonFunctions commonFunctions;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillComponentFacade billComponentFacade;
    @EJB
    private InvestigationFacade investigationFacade;
    @EJB
    private BillItemFacade billItemFacade;
    private Date fromDate;
    private Date toDate;
    private List<InvestigationSummeryData> items;
    private List<InvestigationSummeryData> itemDetails;
    private List<Item> investigations;

    /**
     * Creates a new instance of CashierReportController
     */
    public InvestigationMonthSummeryController() {
    }

    
    
    public BillComponentFacade getBillComponentFacade() {
        return billComponentFacade;
    }

    public void setBillComponentFacade(BillComponentFacade billComponentFacade) {
        this.billComponentFacade = billComponentFacade;
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

    public List<InvestigationSummeryData> getItems() {
        items = new ArrayList<InvestigationSummeryData>();

        for (Item w : getInvestigations()) {
            InvestigationSummeryData temp = new InvestigationSummeryData();
            temp.setInvestigation(w);
            setCountTotal(temp, w);
            items.add(temp);
        }

        return items;
    }

    private void setCountTotal(InvestigationSummeryData is, Item w) {

        String sql;
        Map temMap = new HashMap();
        sql = "select bi FROM BillItem bi where type(bi.item) =:ixtype  and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";

//        sql = "select b from BillItem b where  "
//                + "b.item.id=" + w.getId() + " and  b.bill.createdAt between :fromDate and :toDate";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ixtype", com.divudi.entity.lab.Investigation.class);
        //    temMap.put("pactype", com.divudi.entity.Packege.class);
        List<BillItem> temps = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        double tot = 0.0;
        int c = 0;
        //TODO: add method to resolve in packages
        for (BillItem b : temps) {
            if (b.getBill() != null && b.getBill().isCancelled() == false) {
                if (b.isRefunded() == null || b.isRefunded() == false) {
//                    if (b.getItem() instanceof Packege) {
//                        sql = "Select i from PackageItem p join p.item i where p.packege.id = " + b.getId();
//                        List<Item> packageItems = getItemFacade().findBySQL(sql);
//                        for (Item i : packageItems) {
//                            if (i.getId() == w.getId()) {
//                                tot += i.getTotal();
//                                c++;
//                            }
//                        }
//                    } else {
                    if (b.getItem().getId() == w.getId()) {
                        tot += b.getNetValue();
                        c++;
                    }
//                    }
                }
            }
        }

        is.setCount(c);
        is.setTotal(tot);
    }

    public void setItems(List<InvestigationSummeryData> items) {
        this.items = items;
    }
    @EJB
    private ItemFacade itemFacade;

    public List<Item> getInvestigations() {
      //  investigations = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select distinct ix from BillItem bi join bi.item ix where type(ix) =:ixtype  and bi.bill.createdAt between :fromDate and :toDate order by ix.name";

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ixtype", com.divudi.entity.lab.Investigation.class);

        investigations = getItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        if (investigations == null) {
            investigations = new ArrayList<Item>();
        }

        return investigations;
    }

    public void setInvestigations(List<Item> investigations) {
        this.investigations = investigations;
    }

    public InvestigationFacade getInvestigationFacade() {
        return investigationFacade;
    }

    public void setInvestigationFacade(InvestigationFacade investigationFacade) {
        this.investigationFacade = investigationFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public List<InvestigationSummeryData> getItemDetails() {
        
        itemDetails = new ArrayList<InvestigationSummeryData>();

        for (Item w : getInvestigations()) {

            InvestigationSummeryData temp = new InvestigationSummeryData();
            temp.setInvestigation(w);
            setBillItems(temp, w);
            itemDetails.add(temp);
        }

        return itemDetails;
    }

    private void setBillItems(InvestigationSummeryData t, Item w) {
        String sql;
        Map temMap = new HashMap();
        sql = "select b from BillItem b where "
                + "b.item.id=" + w.getId() + "    and b.createdAt between :fromDate and :toDate and (b.refunded is null or b.bill.cancelledBill is null)";
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        ////System.out.println("Sql iiiiiiiii "+ sql);
        List<BillItem> temps = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        t.setBillItems(temps);

    }

    public void setItemDetails(List<InvestigationSummeryData> itemDetails) {
        this.itemDetails = itemDetails;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }
}
