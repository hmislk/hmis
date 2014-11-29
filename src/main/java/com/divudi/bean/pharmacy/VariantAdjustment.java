/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.LazyBill;
import com.divudi.entity.pharmacy.StockVarientBillItem;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.StockVarientBillItemFacade;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import org.primefaces.model.LazyDataModel;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class VariantAdjustment implements Serializable {

    @Inject
    private SessionController sessionController;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private StockVarientBillItemFacade stockVarientBillItemFacade;
    ///////////////
    private Bill reportedBill;
    private Bill adjustedBill;
    private Date fromDate;
    Date toDate;
    private boolean printPreview;
    private String txtSearch;

    //////////
    @EJB
    private CommonFunctions commonFunctions;
    private LazyDataModel<Bill> searchBills;
    private List<StockVarientBillItem> stockVarientBillItems;

    public void onlyVariantItemBySystemStock() {
        createBillComponent();
        List<StockVarientBillItem> tmp = new ArrayList<>();

        for (StockVarientBillItem i : stockVarientBillItems) {
            if (i.getSystemStock() != i.getPhysicalStock()) {
                tmp.add(i);
            }
        }
        stockVarientBillItems = tmp;

    }

    public void onlyVariantItemByCalculatedStock() {
        createBillComponent();
        List<StockVarientBillItem> tmp = new ArrayList<>();

        for (StockVarientBillItem i : stockVarientBillItems) {
            if (i.getCalCulatedStock() != i.getPhysicalStock()) {
                tmp.add(i);
            }
        }

        stockVarientBillItems = tmp;
    }

    public void createAll() {
        searchBills = null;
        String sql = "";
        HashMap tmp = new HashMap();

        if (txtSearch == null || txtSearch.trim().equals("")) {
            sql = "Select b From PreBill b where b.cancelledBill is null  "
                    + " and b.createdAt between :fromDate and :toDate "
                    + "and b.retired=false and b.billType= :bTp order by b.id desc ";
        } else {
            sql = "Select b From PreBill b where b.cancelledBill is null  "
                    + " and b.createdAt between :fromDate and :toDate and"
                    + " (upper(b.toInstitution.name) like :str "
                    + " or upper(b.creater.webUserPerson.name) like :str "
                    + "  or upper(b.referenceBill.creater.webUserPerson.name) like :str or "
                    + " upper(b.referenceBill.deptId) like :str "
                    + " or upper(b.netTotal) like :str ) "
                    + "and b.retired=false and b.billType= :bTp order by b.id desc ";

            tmp.put("str", "%" + txtSearch.toUpperCase() + "%");
        }

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("bTp", BillType.PharmacyMajorAdjustment);
        List<Bill> lst = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP);

        searchBills = new LazyBill(lst);

    }

    public void clearList() {
        printPreview = false;
//        billItems = null;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return toDate;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return fromDate;
    }

    public void Adjust() {
        getAdjustedBill().setBackwardReferenceBill(getReportedBill());
        getAdjustedBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getReportedBill().getDepartment(), BillType.PharmacyMajorAdjustment, BillClassType.BilledBill, BillNumberSuffix.ADJ));
        getAdjustedBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getReportedBill().getInstitution(), BillType.PharmacyMajorAdjustment, BillClassType.BilledBill, BillNumberSuffix.ADJ));

        getAdjustedBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getAdjustedBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getAdjustedBill().setCreater(getSessionController().getLoggedUser());
        getAdjustedBill().setCreatedAt(Calendar.getInstance().getTime());
        getBillFacade().create(getAdjustedBill());

        for (StockVarientBillItem bi : stockVarientBillItems) {
            bi.setCreatedAt(Calendar.getInstance().getTime());
            bi.setCreater(getSessionController().getLoggedUser());
            bi.setBill(getAdjustedBill());

            getStockVarientBillItemFacade().create(bi);
        }

        //Update Requested Bill Reference
        getReportedBill().setForwardReferenceBill(getAdjustedBill());
        getBillFacade().edit(getReportedBill());

    }

    @Inject
    private PharmacyController pharmacyController;

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Bill getReportedBill() {
        if (reportedBill == null) {
            reportedBill = new BilledBill();
        }
        return reportedBill;
    }

    public void createBill() {
        getAdjustedBill().setBillType(BillType.PharmacyMajorAdjustment);
        getAdjustedBill().setBackwardReferenceBill(getReportedBill());

    }

    public void createBillComponent() {
        stockVarientBillItems = null;
        String sql = "Select p from StockVarientBillItem p where p.bill=:bil order by p.item.name ";
        HashMap hm = new HashMap();
        hm.put("bil", getReportedBill());

        List<StockVarientBillItem> tmp = getStockVarientBillItemFacade().findBySQL(sql, hm);
        //System.err.println("Size "+tmp.size());
        for (StockVarientBillItem i : tmp) {
            StockVarientBillItem bi = new StockVarientBillItem();
            bi.clone(i);
            
            getStockVarientBillItems().add(i);
        }

    }

    public void createAdjustmentBill() {
        createBill();
        createBillComponent();
    }

    public void setReportedBill(Bill reportedBill) {
        this.reportedBill = reportedBill;
        adjustedBill = null;
        printPreview = false;
        stockVarientBillItems = null;
        createAdjustmentBill();
    }

    public Bill getAdjustedBill() {
        if (adjustedBill == null) {
            adjustedBill = new BilledBill();
            adjustedBill.setBillType(BillType.PharmacyMajorAdjustment);
        }
        return adjustedBill;
    }

    public void setAdjustedBill(Bill adjustedBill) {
        this.adjustedBill = adjustedBill;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public PharmacyController getPharmacyController() {
        return pharmacyController;
    }

    public void setPharmacyController(PharmacyController pharmacyController) {
        this.pharmacyController = pharmacyController;
    }

    public boolean getPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public void makeListNull() {
//        pharmaceuticalBillItems = null;   
        searchBills = null;

    }

    public LazyDataModel<Bill> getSearchBills() {
        return searchBills;
    }

    public void setSearchBills(LazyDataModel<Bill> searchBills) {
        this.searchBills = searchBills;
    }

    public String getTxtSearch() {
        return txtSearch;
    }

    public void setTxtSearch(String txtSearch) {
        this.txtSearch = txtSearch;
    }

    public StockVarientBillItemFacade getStockVarientBillItemFacade() {
        return stockVarientBillItemFacade;
    }

    public void setStockVarientBillItemFacade(StockVarientBillItemFacade stockVarientBillItemFacade) {
        this.stockVarientBillItemFacade = stockVarientBillItemFacade;
    }

    public List<StockVarientBillItem> getStockVarientBillItems() {
        if (stockVarientBillItems == null) {
            stockVarientBillItems = new ArrayList<>();
        }
        return stockVarientBillItems;
    }

    public void setStockVarientBillItems(List<StockVarientBillItem> stockVarientBillItems) {
        this.stockVarientBillItems = stockVarientBillItems;
    }
}
