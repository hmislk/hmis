/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.report;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.data.BillType;
import com.divudi.data.dataStructure.InvestigationSummeryData;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.ItemInstitutionCollectingCentreCountRow;
import com.divudi.ejb.BillEjb;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.RefundBill;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PatientInvestigationFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class InvestigationMonthSummeryOwnControllerSession implements Serializable {

    /**
     * Managed Beans
     */
    @Inject
    private SessionController sessionController;
    @Inject
    PatientInvestigationController patientInvestigationController;
    /**
     * EJBs
     */
    @EJB
    private CommonFunctions commonFunctions;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillComponentFacade billComponentFacade;
    @EJB
    private InvestigationFacade investigationFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    BillEjb billEjb;
    /**
     * Properties
     */
    private Date fromDate;
    private Date toDate;
    Institution reportedInstitution;
    Department reportedDepartment;
    private Institution creditCompany;
    Institution institution;
    Department department;
    Institution collectingCentre;
    Item item;
    private List<InvestigationSummeryData> items;
    private List<InvestigationSummeryData> itemDetails;
    private List<Item> investigations;
    List<Bill> bills;
    List<PatientInvestigation> pis;
    List<InvestigationSummeryData> itemsLab;
    Long totalCount;
    int progressValue = 0;
    boolean progressStarted = false;
    boolean stopProgress;
    private boolean paginator = true;
    private int rows = 20;

    /**
     * Creates a new instance of CashierReportController
     */
    public InvestigationMonthSummeryOwnControllerSession() {
    }

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    public Item getItem() {
        return item;
    }

    public void createInvestigationMonthEndSummeryCounts() {
        items = new ArrayList<>();
        List<Item> ixs = billEjb.getItemsInBills(fromDate, toDate, new BillType[]{BillType.OpdBill, BillType.LabBill, BillType.InwardBill}, true, null, true, null, true, null, true, null, false, new Class[]{Investigation.class});
        for (Item w : ixs) {
            System.out.println("w.getName() = " + w.getName());
            if (totalCount == null) {
                totalCount = 0l;
            }
            InvestigationSummeryData temp = setIxSummeryCount(w);
            if (temp.getCount() != 0) {
                totalCount += temp.getCount();
                items.add(temp);
            }
        }
        progressStarted = false;
    }

    public void createInvestigationMonthEndSummeryCountsFilteredByBilledInstitution() {
        items = new ArrayList<>();
        totalCount = null;
        progressStarted = true;
        progressValue = 0;
        List<Item> ixs = billEjb.getItemsInBills(fromDate, toDate, new BillType[]{BillType.OpdBill, BillType.LabBill, BillType.InwardBill}, false, institution, true, null, true, null, true, null, false, new Class[]{Investigation.class});
        double singleItem = 100 / ixs.size();
        for (Item w : ixs) {
            System.out.println("w.getName() = " + w.getName());
            if (totalCount == null) {
                totalCount = 0l;
            }
            if (stopProgress == true) {
                break;
            }
            progressValue += (int) singleItem;
            InvestigationSummeryData temp = setIxSummeryCount(w);
            if (temp.getCount() != 0) {
                totalCount += temp.getCount();
                items.add(temp);
            }
        }
        progressStarted = false;
    }

    public void createInvestigationMonthEndSummeryCountsFilteredByBilledDepartment() {
        items = new ArrayList<>();
        totalCount = null;
        progressStarted = true;
        progressValue = 0;
        List<Item> ixs = billEjb.getItemsInBills(fromDate, toDate, new BillType[]{BillType.OpdBill, BillType.LabBill, BillType.InwardBill}, true, null, false, department, true, null, true, null, false, new Class[]{Investigation.class});
        double singleItem = 100 / ixs.size();
        for (Item w : ixs) {
            System.out.println("w.getName() = " + w.getName());
            if (totalCount == null) {
                totalCount = 0l;
            }
            if (stopProgress == true) {
                break;
            }
            progressValue += (int) singleItem;
            InvestigationSummeryData temp = setIxSummeryCount(w);
            if (temp.getCount() != 0) {
                totalCount += temp.getCount();
                items.add(temp);
            }
        }
        progressStarted = false;
    }

    public void createInvestigationMonthEndSummeryCountsFilteredByReportedInstitution() {
        items = new ArrayList<>();
        totalCount = null;
        progressStarted = true;
        progressValue = 0;
        List<Item> ixs = billEjb.getItemsInBills(fromDate, toDate, new BillType[]{BillType.OpdBill, BillType.LabBill, BillType.InwardBill}, true, null, true, null, false, reportedInstitution, true, null, false, new Class[]{Investigation.class});
        double singleItem = 100 / ixs.size();
        for (Item w : ixs) {
            System.out.println("w.getName() = " + w.getName());
            if (totalCount == null) {
                totalCount = 0l;
            }
            if (stopProgress == true) {
                break;
            }
            progressValue += (int) singleItem;
            InvestigationSummeryData temp = setIxSummeryCount(w);
            if (temp.getCount() != 0) {
                totalCount += temp.getCount();
                items.add(temp);
            }
        }
        progressStarted = false;
    }

    public void createInvestigationMonthEndSummeryCountsFilteredByReportedDepartment() {
        items = new ArrayList<>();
        totalCount = null;
        progressStarted = true;
        progressValue = 0;
        List<Item> ixs = billEjb.getItemsInBills(fromDate, toDate, new BillType[]{BillType.OpdBill, BillType.LabBill, BillType.InwardBill}, true, null, true, null, true, null, false, reportedDepartment, false, new Class[]{Investigation.class});
        double singleItem = 100 / ixs.size();
        for (Item w : ixs) {
            System.out.println("w.getName() = " + w.getName());
            if (totalCount == null) {
                totalCount = 0l;
            }
            if (stopProgress == true) {
                break;
            }
            progressValue += (int) singleItem;
            InvestigationSummeryData temp = setIxSummeryCount(w);
            if (temp.getCount() != 0) {
                totalCount += temp.getCount();
                items.add(temp);
            }
        }
        progressStarted = false;
    }

    public void createInvestigationTurnoverTimeByBills() {
        String sql;
        Map m = new HashMap();
        sql = "Select pi "
                + " from PatientInvestigation pi join pi.billItem bi join bi.bill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.toDepartment=:dep "
                + " and b.createdAt between :fd and :td "
                + " and (bi.refunded is null or bi.refunded=FALSE) "
                + " and bi.retired=false ";
        m.put("dep", department);
        m.put("fd", fromDate);
        m.put("td", toDate);
        pis = patientInvestigationFacade.findBySQL(sql, m, TemporalType.TIMESTAMP);
        System.out.println("m = " + m);
        System.out.println("sql = " + sql);
        System.out.println("pis.size() = " + pis.size());
    }
    
    public void createInvestigationTurnoverTime() {
        System.out.println("createInvestigationTurnoverTime ");
        double averateMins = 0;
        double totalMins = 0;
        double averageCount = 0;
        List<PatientInvestigation> temPis = billEjb.getPatientInvestigations(item,
                fromDate,
                toDate,
                new BillType[]{BillType.OpdBill, BillType.LabBill, BillType.InwardBill},
                new Class[]{BilledBill.class},
                true,
                null,
                true,
                null,
                true,
                null,
                true,
                null);
        System.out.println("pis.size() = " + temPis.size());
        for (PatientInvestigation pi : temPis) {

            System.out.println("pi.getBillItem().getItem().getName() = " + pi.getBillItem().getItem().getName());
            if (pi.getPrintingAt() != null && pi.getBillItem().getBill().getCreatedAt() != null) {
                System.out.println("pi.getPrintingAt().getTime() = " + pi.getPrintingAt().getTime());
                System.out.println("pi.getBillItem().getBill().getCreatedAt().getTime() = " + pi.getBillItem().getBill().getCreatedAt().getTime());
                averateMins = (pi.getPrintingAt().getTime() - pi.getBillItem().getBill().getCreatedAt().getTime()) / (1000 * 60);
                System.out.println("averateMins = " + averateMins);
                totalMins += averateMins;
                averageCount++;
            }
        }
        System.out.println("totalMins = " + totalMins);
        System.out.println("averageCount = " + averageCount);
        totalCount = (long) (totalMins / averageCount);
        progressStarted = false;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public String ixCountByInstitutionAndCollectingCentre() {
        return "/reportLab/ix_count_by_institution_and_collecting_centre";
    }

    public BillComponentFacade getBillComponentFacade() {
        return billComponentFacade;
    }

    public List<InvestigationSummeryData> getItemsLab() {
        return itemsLab;
    }

    public void setItemsLab(List<InvestigationSummeryData> itemsLab) {
        this.itemsLab = itemsLab;
    }

    public void setBillComponentFacade(BillComponentFacade billComponentFacade) {
        this.billComponentFacade = billComponentFacade;
    }

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
            toDate = CommonFunctions.getEndOfDay();
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

    public Institution getReportedInstitution() {
        return reportedInstitution;
    }

    public void setReportedInstitution(Institution reportedInstitution) {
        this.reportedInstitution = reportedInstitution;
    }

    public Department getReportedDepartment() {
        return reportedDepartment;
    }

    public void setReportedDepartment(Department reportedDepartment) {
        this.reportedDepartment = reportedDepartment;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    private Institution collectingIns;

    public List<InvestigationSummeryData> getItems() {
//        items = new ArrayList<>();
//
//        for (Item w : getInvestigations()) {
//            InvestigationSummeryData temp = new InvestigationSummeryData();
//            temp.setInvestigation(w);
//            setCountTotal(temp, w);
//            if (temp.getCount() != 0) {
//                items.add(temp);
//            }
//        }

        return items;
    }

    public List<InvestigationSummeryData> getItems2() {
        items = new ArrayList<>();

        for (Item w : getInvestigations2()) {
            InvestigationSummeryData temp = new InvestigationSummeryData();
            temp.setInvestigation(w);
            setCountTotal2(temp, w);
            if (temp.getCount() != 0) {
                items.add(temp);
            }
        }

        return items;
    }

    private long countTotal;

    List<ItemInstitutionCollectingCentreCountRow> insInvestigationCountRows;

    public void createIxCountByInstitutionAndCollectingCentreOld() {
        String jpql;
        Map m;
        m = new HashMap();

        jpql = "Select new com.divudi.data.dataStructure.ItemInstitutionCollectingCentreCountRow(bi.item, count(bi), bi.bill.institution, bi.bill.collectingCentre) "
                + " from BillItem bi "
                + " join bi.bill b "
                + " join b.institution ins "
                + " join b.collectingCentre cs "
                + " join bi.item item "
                + " where b.createdAt between :fd and :td "
                + " and type(item) =:ixbt "
                + " and bi.retired=false "
                + " and b.retired=false "
                + " and b.cancelled=false ";

        if (institution != null) {
            jpql = jpql + " and ins=:ins ";
            m.put("ins", institution);
        }

        if (collectingCentre != null) {
            jpql = jpql + " and cs=:cs ";
            m.put("cs", collectingCentre);
        }

        if (item != null) {
            jpql = jpql + " and item=:item ";
            m.put("item", item);
        }

        jpql = jpql + " group by item, ins, cs ";
        jpql = jpql + " order by ins.name, cs.name, item.name ";
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("ixbt", Investigation.class);
        insInvestigationCountRows = (List<ItemInstitutionCollectingCentreCountRow>) (Object) billFacade.findAggregates(jpql, m, TemporalType.DATE);

        if (collectingCentre != null) {
            return;
        }

        m = new HashMap();
        jpql = "Select new com.divudi.data.dataStructure.ItemInstitutionCollectingCentreCountRow(bi.item, count(bi), bi.bill.institution) "
                + " from BillItem bi "
                + " join bi.bill b "
                + " join b.institution ins "
                + " join bi.item item "
                + " where b.createdAt between :fd and :td "
                + " and b.collectingCentre is null "
                + " and type(item) =:ixbt "
                + " and bi.retired=false "
                + " and b.retired=false "
                + " and b.cancelled=false ";
        if (institution != null) {
            jpql = jpql + " and ins=:ins ";
            m.put("ins", institution);
        }

        if (item != null) {
            jpql = jpql + " and item=:item ";
            m.put("item", item);
        }

        jpql = jpql + " group by item, ins ";
        jpql = jpql + " order by ins.name, item.name ";
        m.put("fd", fromDate);
        m.put("td", toDate);

        m.put("ixbt", Investigation.class);

        insInvestigationCountRows.addAll((List<ItemInstitutionCollectingCentreCountRow>) (Object) billFacade.findAggregates(jpql, m, TemporalType.DATE));

        int c = 1;
        for (ItemInstitutionCollectingCentreCountRow r : insInvestigationCountRows) {
            r.setId(c);
            c++;
        }

    }

    public void createIxCountByInstitutionAndCollectingCentre() {
        String jpql;
        Map m;
        m = new HashMap();

        jpql = "Select new com.divudi.data.dataStructure.ItemInstitutionCollectingCentreCountRow(bi.item, count(bi), bi.bill.institution, bi.bill.collectingCentre) "
                + " from BillItem bi "
                + " join bi.bill b "
                + " left join b.institution ins "
                + " left join b.collectingCentre cs "
                + " left join bi.item item "
                + " where b.createdAt between :fd and :td "
                + " and type(item) =:ixbt "
                + " and bi.retired=false "
                + " and b.retired=false "
                + " and b.cancelled=false ";

        if (institution != null) {
            jpql = jpql + " and ins=:ins ";
            m.put("ins", institution);
        }

        if (collectingCentre != null) {
            jpql = jpql + " and cs=:cs ";
            m.put("cs", collectingCentre);
        }

        if (item != null) {
            jpql = jpql + " and item=:item ";
            m.put("item", item);
        }

        jpql = jpql + " group by item, ins, cs ";
        jpql = jpql + " order by ins.name, cs.name, item.name ";
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("ixbt", Investigation.class);
        insInvestigationCountRows = (List<ItemInstitutionCollectingCentreCountRow>) (Object) billFacade.findAggregates(jpql, m, TemporalType.TIMESTAMP);

    }

//    public void createIxCountByInstitutionAndCollectingCentreIndividual() {
//        String jpql;
//        Map m = new HashMap();
//        jpql = "Select item, count(bi), ins, cs "
//                + " from BillItem bi "
//                + " join bi.bill b "
//                + " join b.institution ins "
//                + " join b.collectingCentre cs "
//                + " join bi.item item "
//                + " where b.createdAt between :fd and :td "
//                + " and type(item) =:ixbt "
//                + " and bi.retired=false "
//                + " and b.retired=false "
//                + " and b.cancelled=false ";
//
//        if (institution != null) {
//            jpql = jpql + " and ins=:ins ";
//            m.put("ins", institution);
//        }
//        jpql = jpql + " group by item, ins, cs ";
//
//        jpql = jpql + " order by ins.name, cs.name, item.name ";
//
////        New Way
//        jpql = "Select item, count(bi), ins"
//                + " from BillItem bi "
//                + " join bi.bill b "
//                + " join b.institution ins "
//                + " join bi.item item "
//                + " where b.createdAt between :fd and :td "
//                + " and type(item) =:ixbt "
//                + " and bi.retired=false "
//                + " and b.retired=false "
//                + " and b.cancelled=false ";
//        if (institution != null) {
//            jpql = jpql + " and ins=:ins ";
//            m.put("ins", institution);
//        }
//        jpql = jpql + " group by item, ins ";
//        jpql = jpql + " order by ins.name, item.name ";
//
//        m.put("fd", fromDate);
//        m.put("td", toDate);
//        m.put("ixbt", Investigation.class);
//
//        List<Object[]> bojsl = billFacade.findAggregates(jpql, m, TemporalType.DATE);
//        //System.out.println("bojsl = " + bojsl);
//        insInvestigationCountRows = new ArrayList<>();
//
//        Map<Institution, ItemInstitutionCollectingCentreCountRow> map = new HashMap<>();
//
//        for (Object[] bobj : bojsl) {
//            if (bobj.length < 3) {
//                continue;
//            }
//            
//            ItemInstitutionCollectingCentreCountRow r = new ItemInstitutionCollectingCentreCountRow();
//            r.setItem((Item) bobj[0]);
//            r.setCount((Long) bobj[1]);
//            r.setInstitution((Institution) bobj[2]);
////            if(bobj[3]!=null){
////                r.setCollectingCentre((Institution) bobj[3]);
////            }
//            insInvestigationCountRows.add(r);
//        }
//        //System.out.println("sql = " + jpql);
//        //System.out.println("m = " + m);
//        //System.out.println("insInvestigationCountRows.size() = " + insInvestigationCountRows.size());
//    }
    public List<ItemInstitutionCollectingCentreCountRow> getInsInvestigationCountRows() {
        return insInvestigationCountRows;
    }

    public void createItemList3() {
        itemsLab = new ArrayList<>();
        countTotal = 0;
        for (Item w : getInvestigations3()) {
            InvestigationSummeryData temp = new InvestigationSummeryData();
            temp.setInvestigation(w);
            long temCoint = calculateInvestigationBilledCount(w);
            temp.setCount(temCoint);
            countTotal += temCoint;
            if (temp.getCount() != 0) {
                itemsLab.add(temp);
            }
        }
//        countTotal = 0;
//
//        long billed = getCount2(new BilledBill());
//        //System.out.println("billed = " + billed);
//        long cancelled = getCount2(new CancelledBill());
//        //System.out.println("cancelled = " + cancelled);
//        long refunded = getCount2(new RefundBill());
//        //System.out.println("refunded = " + refunded);
//
//        countTotal = billed - (refunded + cancelled);
    }

    public List<InvestigationSummeryData> getItems3() {

        return items;
    }

    public List<InvestigationSummeryData> getItemsWithoutC() {
        items = new ArrayList<>();

        for (Item w : getInvestigationsWithoutC()) {
            InvestigationSummeryData temp = new InvestigationSummeryData();
            temp.setInvestigation(w);
            setCountTotalWithoutC(temp, w);
            items.add(temp);
        }

        return items;
    }

    private void setCountTotalWithoutC(InvestigationSummeryData is, Item w) {

        String sql;
        Map temMap = new HashMap();
        sql = "select bi FROM BillItem bi where  bi.bill.institution=:ins and type(bi.item) =:ixtype "
                + " and (bi.bill.paymentMethod = :pm1 or bi.bill.paymentMethod = :pm2 or"
                + " bi.bill.paymentMethod = :pm3 )    and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("ins", getSessionController().getInstitution());

        temMap.put("ixtype", com.divudi.entity.lab.Investigation.class);
        List<BillItem> temps = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        double tot = 0.0;
        int c = 0;

        for (BillItem b : temps) {
            if (b.getBill() != null && b.getBill().isCancelled() == false) {
                if (b.isRefunded() == null || b.isRefunded() == false) {
                    if (b.getItem().getId() == w.getId()) {
                        tot += b.getNetValue();
                        c++;
                    }
                }
            }
        }

        is.setCount(c);
        is.setTotal(tot);
    }

    private long getCount(Bill bill, Item item) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi "
                + " where (bi.bill.billType=:bType1 or bi.bill.billType=:bType2) and bi.item =:itm"
                + " and type(bi.bill)=:billClass and (bi.bill.toInstitution=:ins or bi.item.department.institution=:ins ) "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", item);
        temMap.put("billClass", bill.getClass());
        temMap.put("bType1", BillType.OpdBill);
        temMap.put("bType2", BillType.InwardBill);
        temMap.put("ins", getSessionController().getInstitution());
        return getBillItemFacade().countBySql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private long getCount2(Bill bill, Item item) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi where bi.bill.billType=:bType and bi.item =:itm"
                + " and type(bi.bill)=:billClass and bi.bill.collectingCentre=:col "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", item);
        temMap.put("billClass", bill.getClass());
        temMap.put("bType", BillType.LabBill);
        temMap.put("col", getCollectingIns());
        return getBillItemFacade().countBySql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private long getCount3(Bill bill, Item item) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi where bi.bill.billType=:bType and bi.item =:itm"
                + " and type(bi.bill)=:billClass and bi.bill.collectingCentre=:col "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", item);
        temMap.put("billClass", bill.getClass());
        temMap.put("bType", BillType.LabBill);
        temMap.put("col", getCreditCompany());
        return getBillItemFacade().countBySql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private long getCount(Bill bill) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi where bi.bill.billType=:bType and type(bi.item)=:ixtype "
                + " and type(bi.bill)=:billClass and bi.bill.toInstitution=:ins "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ixtype", Investigation.class);
        temMap.put("billClass", bill.getClass());
        temMap.put("bType", BillType.OpdBill);
        temMap.put("ins", getSessionController().getInstitution());
        return getBillItemFacade().countBySql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private long getCount2(Bill bill) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi where bi.bill.billType=:bType and type(bi.item)=:ixtype "
                + " and type(bi.bill)=:billClass and bi.bill.collectingCentre=:col "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ixtype", Investigation.class);
        temMap.put("billClass", bill.getClass());
        temMap.put("bType", BillType.LabBill);
        temMap.put("col", getCollectingIns());
        return getBillItemFacade().countBySql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double getTotal(Item item) {
        String sql;
        Map temMap = new HashMap();
        sql = "select sum(bi.netValue) FROM BillItem bi where bi.bill.billType=:bType and bi.item =:itm"
                + " and bi.bill.toInstitution=:ins "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", item);
        temMap.put("bType", BillType.OpdBill);
        temMap.put("ins", getSessionController().getInstitution());
        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private double getTotal2(Item item) {
        String sql;
        Map temMap = new HashMap();
        sql = "select sum(bi.netValue) FROM BillItem bi where bi.bill.billType=:bType and bi.item =:itm"
                + " and bi.bill.collectingCentre=:col "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", item);
        temMap.put("bType", BillType.LabBill);
        temMap.put("col", getCollectingIns());
        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double getTotal3(Item item) {
        String sql;
        Map temMap = new HashMap();
        sql = "select sum(bi.netValue) FROM BillItem bi where bi.bill.billType=:bType and bi.item =:itm"
                + " and bi.bill.creditCompany=:col "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", item);
        temMap.put("bType", BillType.LabBill);
        temMap.put("col", getCreditCompany());
        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private void setCountTotal(InvestigationSummeryData is, Item w) {
        long billed = getCount(new BilledBill(), w);
        long cancelled = getCount(new CancelledBill(), w);
        long refunded = getCount(new RefundBill(), w);
        long net = billed - (cancelled + refunded);
        is.setCount(net);

        is.setTotal(getTotal(w));
        if (net > 0) {
            is.setTurnOverValue(setTurnOverValue(net));
        }

    }

    private InvestigationSummeryData setIxSummeryCount(Item w) {
        InvestigationSummeryData is = new InvestigationSummeryData();
        is.setInvestigation(w);
        long billed = billEjb.getBillItemCount(w, fromDate, toDate, new BillType[]{BillType.InwardBill, BillType.LabBill, BillType.OpdBill}, new Class[]{BilledBill.class}, true, null, true, null, true, null, true, null);
        System.out.println("billed = " + billed);
        long cancelled = billEjb.getBillItemCount(w, fromDate, toDate, new BillType[]{BillType.InwardBill, BillType.LabBill, BillType.OpdBill}, new Class[]{CancelledBill.class}, true, null, true, null, true, null, true, null);
        System.out.println("cancelled = " + cancelled);
        long refunded = billEjb.getBillItemCount(w, fromDate, toDate, new BillType[]{BillType.InwardBill, BillType.LabBill, BillType.OpdBill}, new Class[]{RefundBill.class}, true, null, true, null, true, null, true, null);
        System.out.println("refunded = " + refunded);
        long net = billed - (cancelled + refunded);
        System.out.println("net = " + net);
        is.setCount(net);
        return is;
    }

    private double setTurnOverValue(long count) {

        long timeInMinutes = (getToDate().getTime() - getFromDate().getTime()) / 60000;

        long turnOverTime = timeInMinutes / count;
        System.out.println("turnOverTime = " + turnOverTime);

        return turnOverTime;
    }

    private void setCountTotal2(InvestigationSummeryData is, Item w) {

        long billed = getCount2(new BilledBill(), w);
        long cancelled = getCount2(new CancelledBill(), w);
        long refunded = getCount2(new RefundBill(), w);

        long net = billed - (cancelled + refunded);
        is.setCount(net);

        is.setTotal(getTotal2(w));
    }

    private void setCountTotal3(InvestigationSummeryData is, Item w) {

        long billed = getCount3(new BilledBill(), w);
        long cancelled = getCount3(new CancelledBill(), w);
        long refunded = getCount3(new RefundBill(), w);

        long net = billed - (cancelled + refunded);
        is.setCount(net);

        is.setTotal(getTotal3(w));
    }

    private long calculateInvestigationBilledCount(Item w) {
        long billed = getCount3(new BilledBill(), w);
        long cancelled = getCount3(new CancelledBill(), w);
        long refunded = getCount3(new RefundBill(), w);
        return billed - (cancelled + refunded);
    }

    public void setItems(List<InvestigationSummeryData> items) {
        this.items = items;
    }
    @EJB
    private ItemFacade itemFacade;

    public List<Item> getInvestigationsWithoutC() {
        String sql;
        Map temMap = new HashMap();
        sql = "select distinct ix from BillItem bi join bi.item ix where type(ix) =:ixtype  "
                + "and (bi.bill.paymentMethod = :pm1 or bi.bill.paymentMethod = :pm2 or bi.bill.paymentMethod = :pm3 ) "
                + "and bi.bill.billType=:bTp and bi.bill.institution=:ins and bi.bill.createdAt between :fromDate and :toDate order by ix.name";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("ixtype", com.divudi.entity.lab.Investigation.class);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ins", getSessionController().getInstitution());
        investigations = getItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        return investigations;
    }

    public List<Item> getInvestigations() {
        String sql;
        Map temMap = new HashMap();
        sql = "select distinct ix from BillItem bi join bi.item ix where type(ix) =:ixtype  "
                + "and bi.bill.billType=:bType and bi.bill.toInstitution=:ins "
                + "and bi.bill.createdAt between :fromDate and :toDate order by ix.name";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ixtype", Investigation.class);
        temMap.put("bType", BillType.OpdBill);
        temMap.put("ins", getSessionController().getInstitution());
        investigations = getItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        return investigations;
    }

    public List<Item> getInvestigations2() {
        Map temMap = new HashMap();
        String sql = "select distinct ix from BillItem bi join bi.item ix where type(ix) =:ixtype  "
                + "and bi.bill.billType=:bType and bi.bill.collectingCentre=:col "
                + "and bi.bill.createdAt between :fromDate and :toDate ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ixtype", Investigation.class);
        temMap.put("bType", BillType.LabBill);
        temMap.put("col", getCollectingIns());
        investigations = getItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        return investigations;
    }

    public List<Item> getInvestigations3() {
        Map temMap = new HashMap();
        String sql = "select distinct ix from BillItem bi join bi.item ix "
                + " where type(ix) =:ixtype  "
                + "and bi.bill.billType=:bType and bi.bill.collectingCentre=:col "
                + "and bi.bill.createdAt between :fromDate and :toDate ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ixtype", Investigation.class);
        temMap.put("bType", BillType.LabBill);
        temMap.put("col", getCreditCompany());
        investigations = getItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

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

    public List<InvestigationSummeryData> getItemDetailsWithoutCredit() {

        itemDetails = new ArrayList<InvestigationSummeryData>();

        for (Item w : getInvestigationsWithoutC()) {

            InvestigationSummeryData temp = new InvestigationSummeryData();
            temp.setInvestigation(w);
            setBillItemsWithoutC(temp, w);
            itemDetails.add(temp);
        }

        return itemDetails;
    }

    public List<InvestigationSummeryData> getItemDetails() {

        itemDetails = new ArrayList<>();

        for (Item w : getInvestigations()) {

            InvestigationSummeryData temp = new InvestigationSummeryData();
            temp.setInvestigation(w);
            setBillItems(temp, w);
            itemDetails.add(temp);
        }

        return itemDetails;
    }
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;

    private void setBillItems(InvestigationSummeryData t, Item w) {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BillItem b where b.bill.toInstitution=:ins"
                + " and b.item=:ii and  b.createdAt between :fromDate and :toDate";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("ii", w);

        List<BillItem> temps = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        t.setBillItems(temps);

    }

    private void setBillItemsWithoutC(InvestigationSummeryData t, Item w) {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BillItem b where b.bill.billType=:bTp and b.bill.institution=:ins "
                + " and b.item=:itm and (b.bill.paymentMethod = :pm1 or b.bill.paymentMethod = :pm2 or b.bill.paymentMethod = :pm3 ) and  b.createdAt between :fromDate and :toDate";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("itm", w);
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

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }

    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
    }

    public long getCountTotal() {
        countTotal = 0;

        long billed = getCount(new BilledBill());
        long cancelled = getCount(new CancelledBill());
        long refunded = getCount(new RefundBill());

        countTotal = billed - (refunded + cancelled);

        //  //System.err.println("Billed : " + billed);
        //   //System.err.println("Cancelled : " + cancelled);
        //   //System.err.println("Refunded : " + refunded);
        //   //System.err.println("Gross Tot : " + countTotal);
        return countTotal;
    }

    public long getCountTotal2() {

        //  //System.err.println("Billed : " + billed);
        //  //System.err.println("Cancelled : " + cancelled);
        //  //System.err.println("Refunded : " + refunded);
        //  //System.err.println("Gross Tot : " + countTotal);
        return countTotal;
    }

    public void setCountTotal(long countTotal) {
        this.countTotal = countTotal;
    }

    public Institution getCollectingIns() {
        return collectingIns;
    }

    public void setCollectingIns(Institution collectingIns) {
        this.collectingIns = collectingIns;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public int getProgressValue() {
        return progressValue;
    }

    public void setProgressValue(int progressValue) {
        this.progressValue = progressValue;
    }

    public boolean isProgressStarted() {
        return progressStarted;
    }

    public void setProgressStarted(boolean progressStarted) {
        this.progressStarted = progressStarted;
    }

    public boolean isPaginator() {
        return paginator;
    }

    public void setPaginator(boolean paginator) {
        this.paginator = paginator;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public class institutionInvestigationCountRow {

        Institution institution;
        Institution collectingCentre;
        Item item;
        Investigation investigation;
        Double count;

        public institutionInvestigationCountRow(Institution institution, Institution collectingCentre, Item item, Double count) {
            this.institution = institution;
            this.collectingCentre = collectingCentre;
            this.item = item;
            this.count = count;
        }

        public Institution getInstitution() {
            return institution;
        }

        public void setInstitution(Institution institution) {
            this.institution = institution;
        }

        public Institution getCollectingCentre() {
            return collectingCentre;
        }

        public void setCollectingCentre(Institution collectingCentre) {
            this.collectingCentre = collectingCentre;
        }

        public Item getItem() {
            return item;
        }

        public void setItem(Item item) {
            this.item = item;
        }

        public Investigation getInvestigation() {
            return investigation;
        }

        public void setInvestigation(Investigation investigation) {
            this.investigation = investigation;
        }

        public Double getCount() {
            return count;
        }

        public void setCount(Double count) {
            this.count = count;
        }

    }

    public void prepareForPrint() {
        paginator = false;
        rows = getItems().size();
    }

    public void prepareForView() {
        paginator = true;
        rows = 20;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public List<PatientInvestigation> getPis() {
        return pis;
    }

    public void setPis(List<PatientInvestigation> pis) {
        this.pis = pis;
    }

    
    
}
