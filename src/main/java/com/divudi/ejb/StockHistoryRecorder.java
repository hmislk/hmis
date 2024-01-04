/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import com.divudi.data.FeeType;
import com.divudi.data.HistoryType;
import com.divudi.data.PersonInstitutionType;
import com.divudi.entity.Department;
import com.divudi.entity.FeeChange;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Staff;
import com.divudi.entity.channel.ArrivalRecord;
import com.divudi.entity.pharmacy.Ampp;
import com.divudi.entity.pharmacy.StockHistory;
import com.divudi.facade.AmpFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.FeeChangeFacade;
import com.divudi.facade.FingerPrintRecordFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.PharmaceuticalItemFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.StockFacade;
import com.divudi.facade.StockHistoryFacade;
import com.divudi.java.CommonFunctions;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author Buddhika
 */
@Stateless
public class StockHistoryRecorder {

    @EJB
    StockFacade StockFacade;
    @EJB
    DepartmentFacade departmentFacade;
    @EJB
    ItemFacade itemFacade;
    @EJB
    PharmaceuticalItemFacade piFacade;
    @EJB
    AmpFacade ampFacade;
    @EJB
    StockHistoryFacade stockHistoryFacade;
    @EJB
    FeeChangeFacade feeChangeFacade;
    @EJB
    ItemFeeFacade itemFeeFacade;
    @EJB
    ServiceSessionFacade serviceSessionFacade;
    @EJB
    ChannelBean channelBean;
    @EJB
    StaffFacade staffFacade;
    @EJB
    FingerPrintRecordFacade fingerPrintRecordFacade;
    @EJB
    FinalVariables finalVariables;


    CommonFunctions commonFunctions;

//    @SuppressWarnings("unused")
//    @Schedule(minute = "1", second = "1", dayOfMonth = "*", month = "*", year = "*", hour = "1", persistent = false)
//    @Schedule(minute = "*", second = "10", dayOfMonth = "*", month = "*", year = "*", hour = "*", persistent = false)
//    @Schedule(minute = "59", second = "59", hour = "23", dayOfMonth = "Last", info = "2nd Scheduled Timer", persistent = false)
//    @Schedule(second="*/1", minute="*",hour="*", persistent=false)
    public void myTimer() {
        Date startTime = new Date();
        ////System.out.println("Start writing stock history: " + startTime);
        for (Department d : fetchStockDepartment()) {
            if (!d.isRetired()) {
                for (Item amp : fetchStockItem(d)) {
                    if (!amp.isRetired()) {
                        StockHistory h = new StockHistory();
                        h.setFromDate(startTime);
                        h.setHistoryType(HistoryType.MonthlyRecord);
                        h.setDepartment(d);
                        h.setItem(amp);
                        h.setStockQty(getStockQty(amp, d));
                        h.setStockPurchaseValue(getStockPurchaseValue(amp, d));
                        h.setStockSaleValue(getStockRetailSaleValue(amp, d));
                        //SET DATE DATAS
                        h.setHxDate(Calendar.getInstance().get(Calendar.DATE));
                        h.setHxMonth(Calendar.getInstance().get(Calendar.MONTH));
                        h.setHxWeek(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
                        h.setHxYear(Calendar.getInstance().get(Calendar.YEAR));
                        h.setStockAt(startTime);
                        h.setCreatedAt(startTime);

                        getStockHistoryFacade().create(h);
                    }
                }
                ////System.out.println("hx finished for = " + d);
            }
        }
        ////System.out.println("End writing stock history: " + new Date());
//        ////System.out.println("TIme taken for Hx is " + (((new Date()) - startTime )/(1000*60*60)) + " minutes.");
    }

    @SuppressWarnings("unused")
//    @Schedule(hour = "00", minute = "15", second = "00", dayOfMonth = "*", info = "Daily Mid Night", persistent = false)
    public void myTimerDaily() {
        Date startTime = new Date();
        for (FeeChange fc : fetchFeeChanges()) {
            if (fc.getFee().getStaff() != null) {
            }
            for (ItemFee f : fetchServiceSessionFees(fc.getFee().getFeeType(), fc.getFee().getName(), fc.getFee().getStaff())) {
                //System.out.println("1.f.getFee() = " + f.getFee());
                f.setFee(f.getFee() + fc.getFee().getFee());
                //System.out.println("2.f.getFee() = " + f.getFee());
                f.setFfee(f.getFfee() + fc.getFee().getFfee());
                getItemFeeFacade().edit(f);
            }
            fc.setDoneAt(new Date());
            fc.setDone(true);
            getFeeChangeFacade().edit(fc);
        }
        ////System.out.println("End writing stock history: " + new Date());
//        ////System.out.println("TIme taken for Hx is " + (((new Date()) - startTime )/(1000*60*60)) + " minutes.");
    }

//    @SuppressWarnings("unused")
//    @Schedule(hour = "03", minute = "15", second = "00", dayOfMonth = "*", info = "Daily Mornining", persistent = false)
//    public void myTimerDailyChannelShedule() {
//        Date startTime = new Date();
//        //System.out.println("Start Create Shedule " + startTime);
//
//        for (Staff s : staffs()) {
//            generateSessions(s);
//        }
//
//        //System.out.println("Start and End Create Shedule " + startTime + " - " + new Date());
//
//        ////System.out.println("End writing stock history: " + new Date());
////        ////System.out.println("TIme taken for Hx is " + (((new Date()) - startTime )/(1000*60*60)) + " minutes.");
//    }

 

    public List<Staff> staffs() {
        String sql;
        Map m = new HashMap();
        List<Staff> consultants = new ArrayList<>();
        sql = " select pi.staff from PersonInstitution pi where pi.retired=false "
                + " and pi.type=:typ "
                + " order by pi.staff.person.name ";

        m.put("typ", PersonInstitutionType.Channelling);

        consultants = staffFacade.findByJpql(sql, m);

        return consultants;
    }

   

    public void checkDoctorArival(ServiceSession s) {
        s.setArival(findArrivals(s));
    }

    public Boolean findArrivals(ServiceSession ss) {
        ArrivalRecord arrivalRecord = new ArrivalRecord();
        String sql = "Select bs From ArrivalRecord bs "
                + " where bs.retired=false"
                + " and bs.serviceSession.id=:ss "
                + " and bs.sessionDate=:ssDate ";
        HashMap hh = new HashMap();
        hh.put("ssDate", ss.getSessionDate());
        hh.put("ss", ss.getId());
        arrivalRecord = (ArrivalRecord) fingerPrintRecordFacade.findFirstByJpql(sql, hh);

        if (arrivalRecord != null) {
            if (arrivalRecord.isApproved()) {
                return true;
            } else {
                return false;
            }
        }
        return null;
    }

    public List<Department> fetchStockDepartment() {
        String sql;
        Map m = new HashMap();
        sql = "select distinct(s.department) from Stock s order by s.department.name";
        return departmentFacade.findByJpql(sql, m);
    }

    public List<Item> fetchStockItem(Department department) {
        String sql;
        Map m = new HashMap();
        sql = "select distinct(s.itemBatch.item) from Stock s"
                + " where s.department=:dep "
                + "  order by s.itemBatch.item.name";
        m.put("dep", department);
        return itemFacade.findByJpql(sql, m);
    }

    public double getStockQty(Item item, Department department) {
        if (item instanceof Ampp) {
            item = ((Ampp) item).getAmp();
        }
        String sql;
        Map m = new HashMap();
        m.put("d", department);
        m.put("i", item);
        sql = "select sum(s.stock) from Stock s where s.department=:d and s.itemBatch.item=:i";
        return getStockFacade().findDoubleByJpql(sql, m);
    }

    public double getStockRetailSaleValue(Item item, Department department) {
        if (item instanceof Ampp) {
            item = ((Ampp) item).getAmp();
        }
        String sql;
        Map m = new HashMap();
        m.put("d", department);
        m.put("i", item);
        sql = "select sum(s.stock * s.itemBatch.retailsaleRate) from Stock s where s.department=:d and s.itemBatch.item=:i";
        return getStockFacade().findDoubleByJpql(sql, m);
    }

    public double getStockPurchaseValue(Item item, Department department) {
        if (item instanceof Ampp) {
            item = ((Ampp) item).getAmp();
        }
        String sql;
        Map m = new HashMap();
        m.put("d", department);
        m.put("i", item);
        sql = "select sum(s.stock * s.itemBatch.purcahseRate) from Stock s where s.department=:d and s.itemBatch.item=:i";
        return getStockFacade().findDoubleByJpql(sql, m);
    }

    public List<FeeChange> fetchFeeChanges() {
        String sql;
        Map m = new HashMap();
        sql = " select fc from FeeChange fc where "
                + " fc.retired=false "
                + " and fc.validFrom=:ed "
                + " and fc.done!=true ";
        m.put("ed", getCommonFunctions().getEndOfDay(new Date()));
        List<FeeChange> changes = getFeeChangeFacade().findByJpql(sql, m, TemporalType.DATE);
        return changes;
    }

    public List<ItemFee> fetchServiceSessionFees(FeeType ft, String s, Staff staff) {
        String sql;
        Map m = new HashMap();
        sql = "Select f from ItemFee f "
                + " where f.retired=false "
                + " and type(f.serviceSession)=:type "
                + " and f.serviceSession.originatingSession is null "
                + " and f.feeType=:ft "
                + " and f.name=:a ";

        if (staff != null) {
            sql += " and f.serviceSession.staff=:staff ";
            m.put("staff", staff);
        }

        if ((ft == FeeType.Service && s.equals("Scan Fee")) || (ft == FeeType.OwnInstitution && s.equals("Hospital Fee"))) {
            sql += " and (f.fee>0 or f.ffee>0) ";
        }

        sql += " order by f.id";
        m.put("type", ServiceSession.class);
        m.put("ft", ft);
        m.put("a", s);
        List<ItemFee> itemFees = getItemFeeFacade().findByJpql(sql, m);
        return itemFees;
    }

// Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    public StockFacade getStockFacade() {
        return StockFacade;
    }

    public void setStockFacade(StockFacade StockFacade) {
        this.StockFacade = StockFacade;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public PharmaceuticalItemFacade getPiFacade() {
        return piFacade;
    }

    public void setPiFacade(PharmaceuticalItemFacade piFacade) {
        this.piFacade = piFacade;
    }

    public AmpFacade getAmpFacade() {
        return ampFacade;
    }

    public void setAmpFacade(AmpFacade ampFacade) {
        this.ampFacade = ampFacade;
    }

    public StockHistoryFacade getStockHistoryFacade() {
        return stockHistoryFacade;
    }

    public void setStockHistoryFacade(StockHistoryFacade stockHistoryFacade) {
        this.stockHistoryFacade = stockHistoryFacade;
    }

    public FeeChangeFacade getFeeChangeFacade() {
        return feeChangeFacade;
    }

    public void setFeeChangeFacade(FeeChangeFacade feeChangeFacade) {
        this.feeChangeFacade = feeChangeFacade;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade itemFeeFacade) {
        this.itemFeeFacade = itemFeeFacade;
    }

}
