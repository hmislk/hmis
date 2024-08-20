/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.report;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;

import com.divudi.bean.lab.InvestigationController;
import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.InvestigationSummeryData;
import com.divudi.data.dataStructure.ItemInstitutionCollectingCentreCountRow;
import com.divudi.ejb.BillEjb;

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
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.ReportItemFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
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
    @Inject
    CommonController commonController;
    /**
     * EJBs
     */

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
    InstitutionFacade institutionFacade;
    @EJB
    BillEjb billEjb;
    @EJB
    ReportItemFacade reportItemFacade;
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
    List<Bill> billedBills;
    List<Bill> cancelledBills;
    List<Bill> refundedBills;
    List<PatientInvestigation> pis;
    List<InvestigationSummeryData> itemsLab;
    BillType billType;
    Long totalCount;
    int progressValue = 0;
    boolean progressStarted = false;
    boolean stopProgress;
    private boolean paginator = true;
    private int rows = 20;
    double grantTotal;
    BillListTotal billlistTotal;

    private String summeryType = "1";
    private String totalType = "2";
    private List<IncomeSummeryRow> incomeSummeryRows;

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

    public void clearList() {
        items = new ArrayList<>();
        totalCount = 0l;
    }

    public void createInvestigationMonthEndSummeryCounts() {
        items = new ArrayList<>();
        List<Item> ixs = billEjb.getItemsInBills(fromDate, toDate, new BillType[]{BillType.OpdBill, BillType.LabBill, BillType.InwardBill, BillType.CollectingCentreBill}, true, null, true, null, true, null, true, null, false, new Class[]{Investigation.class});
        totalCount = 0l;
        for (Item w : ixs) {
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
    @Inject
    InvestigationController investigationController;

    public void createInvestigationExistTable() {
        items = new ArrayList<>();
        List<Investigation> investigations;
        investigations = new ArrayList<>();

        investigations.addAll(investigationController.getInvestigationItems());

        if (investigations.isEmpty()) {
            JsfUtil.addErrorMessage("No Investigations");
            return;
        }

        for (Investigation i : investigations) {
            InvestigationSummeryData temp = new InvestigationSummeryData();

            temp.setInvestigation(i);

            temp.setB(checkAvailabilityOfReport(i));

            if (temp.getInvestigation() != null) {
                items.add(temp);
            }

        }
    }

    public boolean checkAvailabilityOfReport(Investigation investigation) {
        boolean exist = false;
        String sql;
        HashMap hm = new HashMap();

        sql = "Select ri from ReportItem ri "
                + " where ri.retired=false "
                + " and ri.item=:itm ";

        hm.put("itm", investigation);

        if (reportItemFacade.findFirstByJpql(sql, hm) != null) {
            exist = true;
        }

        return exist;
    }

    public void createInvestigationMonthEndSummeryCountsFilteredByBilledInstitution() {
        if (institution == null) {
            JsfUtil.addErrorMessage("Select Institution");
            return;
        }
        items = new ArrayList<>();
        totalCount = null;
        progressStarted = true;
        progressValue = 0;
        List<Item> ixs = billEjb.getItemsInBills(fromDate, toDate, new BillType[]{BillType.OpdBill, BillType.LabBill, BillType.InwardBill, BillType.CollectingCentreBill}, false, institution, true, null, true, null, true, null, false, new Class[]{Investigation.class});
        if (ixs.isEmpty()) {
            JsfUtil.addErrorMessage("No Bills For This Date Range");
            return;
        }
        double singleItem = 100 / ixs.size();
        for (Item w : ixs) {
            if (totalCount == null) {
                totalCount = 0l;
            }
            if (stopProgress == true) {
                break;
            }
            progressValue += (int) singleItem;
            InvestigationSummeryData temp = setIxSummeryCountBilledIns(w, institution);
            if (temp.getCount() != 0) {
                totalCount += temp.getCount();
                items.add(temp);
            }
        }
        progressStarted = false;
    }

    public void createInvestigationMonthEndSummeryCountsFilteredByBilledDepartment() {
        if (department == null) {
            JsfUtil.addErrorMessage("Select Department");
            return;
        }
        items = new ArrayList<>();
        totalCount = null;
        progressStarted = true;
        progressValue = 0;
        List<Item> ixs = billEjb.getItemsInBills(fromDate, toDate, new BillType[]{BillType.OpdBill, BillType.LabBill, BillType.InwardBill, BillType.CollectingCentreBill}, true, null, false, department, true, null, true, null, false, new Class[]{Investigation.class});
        if (ixs.isEmpty()) {
            JsfUtil.addErrorMessage("No Bills For This Date Range");
            return;
        }
        double singleItem = 100 / ixs.size();
        for (Item w : ixs) {
            if (totalCount == null) {
                totalCount = 0l;
            }
            if (stopProgress == true) {
                break;
            }
            progressValue += (int) singleItem;
            InvestigationSummeryData temp = setIxSummeryCountBilledDep(w, department);
            if (temp.getCount() != 0) {
                totalCount += temp.getCount();
                items.add(temp);
            }
        }
        progressStarted = false;
    }

    public void createInvestigationMonthEndSummeryCountsFilteredByReportedInstitution() {
        if (reportedInstitution == null) {
            JsfUtil.addErrorMessage("Select Institution");
            return;
        }
        items = new ArrayList<>();
        totalCount = null;
        progressStarted = true;
        progressValue = 0;
        List<Item> ixs = billEjb.getItemsInBills(fromDate, toDate, new BillType[]{BillType.OpdBill, BillType.LabBill, BillType.InwardBill, BillType.CollectingCentreBill}, true, null, true, null, false, reportedInstitution, true, null, false, new Class[]{Investigation.class});
        if (ixs.isEmpty()) {
            JsfUtil.addErrorMessage("No Bills For This Date Range");
            return;
        }
        double singleItem = 100 / ixs.size();
        for (Item w : ixs) {
            if (totalCount == null) {
                totalCount = 0l;
            }
            if (stopProgress == true) {
                break;
            }
            progressValue += (int) singleItem;
            InvestigationSummeryData temp = setIxSummeryCountReportedIns(w, reportedInstitution);
            if(temp.getCount() == 0){
                continue;
            }
            if (temp.getCount() != 0) {
                totalCount += temp.getCount();
                items.add(temp);
            }
        }
        progressStarted = false;
    }

    public void createInvestigationMonthEndSummeryCountsFilteredByReportedDepartment() {
        if (reportedDepartment == null) {
            JsfUtil.addErrorMessage("Select Department");
            return;
        }
        items = new ArrayList<>();
        totalCount = null;
        progressStarted = true;
        progressValue = 0;
        List<Item> ixs = billEjb.getItemsInBills(fromDate, toDate, new BillType[]{BillType.OpdBill, BillType.LabBill, BillType.InwardBill, BillType.CollectingCentreBill}, true, null, true, null, true, null, false, reportedDepartment, false, new Class[]{Investigation.class});
        if (ixs.isEmpty()) {
            JsfUtil.addErrorMessage("No Bills For This Date Range");
            return;
        }
        double singleItem = 100 / ixs.size();
        for (Item w : ixs) {
            if (totalCount == null) {
                totalCount = 0l;
            }
            if (stopProgress == true) {
                break;
            }
            progressValue += (int) singleItem;
            InvestigationSummeryData temp = setIxSummeryCountReportedDep(w, reportedDepartment);
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
                + " and b.createdAt between :fd and :td "
                + " and (bi.refunded is null or bi.refunded=FALSE) "
                + " and bi.retired=false ";

        if (department != null) {
            sql += " and b.toDepartment=:dep ";
            m.put("dep", department);
        }

        if (item != null) {
            sql += " and bi.item=:i ";
            m.put("i", item);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);
        pis = patientInvestigationFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public void createInvestigationTurnoverTime() {
        double averateMins = 0;
        double totalMins = 0;
        double averageCount = 0;
        BillType[] billTypes=new BillType[]{};
        if (summeryType.equals("1")) {
            billTypes=new BillType[]{BillType.OpdBill, BillType.LabBill, BillType.InwardBill, BillType.CollectingCentreBill};
        } else if (summeryType.equals("2")) {
            billTypes=new BillType[]{BillType.CollectingCentreBill, BillType.LabBill};
        } else if (summeryType.equals("3")) {
            billTypes=new BillType[]{BillType.OpdBill};
        } else if (summeryType.equals("4")) {
            billTypes=new BillType[]{BillType.InwardBill};
        } else if (summeryType.equals("5")) {
            billTypes=new BillType[]{BillType.OpdBill, BillType.InwardBill,};
        }
        boolean flag=true;
        if(item == null){
            JsfUtil.addSuccessMessage("Please Select Investigation");
            return ;
        }
        if (department!=null) {
            flag=false;
        }
        List<PatientInvestigation> temPis = billEjb.getPatientInvestigations(item,
                fromDate,
                toDate,
                billTypes,
                new Class[]{BilledBill.class},
                true,
                null,
                flag,
                department,
                true,
                null,
                true,
                null);
        for (PatientInvestigation pi : temPis) {

            if (pi.getApproveAt() != null && pi.getSampledAt() != null) {
                averateMins = (pi.getApproveAt().getTime() - pi.getSampledAt().getTime()) / (1000 * 60);
                totalMins += averateMins;
                averageCount++;
            }
        }
        totalCount = (long) (totalMins / averageCount);
        progressStarted = false;
    }

    public void createInvestigationTable() {
        billedBills = new ArrayList<>();
        billedBills = fetchInvestigationBillList(new BilledBill());
        cancelledBills = new ArrayList<>();
        cancelledBills = fetchInvestigationBillList(new CancelledBill());
        refundedBills = new ArrayList<>();
        refundedBills = fetchInvestigationBillList(new RefundBill());

        billlistTotal = new BillListTotal();

    }

    public List<Bill> fetchInvestigationBillList(Bill b) {
        List<Bill> billList = new ArrayList();

        String sql;
        Map map = new HashMap();

        sql = "select distinct (bi.bill) from BillItem bi"
                + " where bi.bill.retired=false"
                + " and type(bi.item) =:iclass"
                + " and type(bi.bill) =:biclass"
                + " and bi.bill.institution=:ins"
                + " and bi.bill.createdAt between :fromDate and :toDate ";

        if (billType == null) {
            //1
//            sql += " and (bi.bill.billType=:btype1 or bi.bill.billType=:btype2)";
//            map.put("btype1", BillType.OpdBill);
//            map.put("btype2", BillType.InwardBill);
            //2
            List<BillType> billTypes = new ArrayList<>();
            billTypes.add(BillType.OpdBill);
            billTypes.add(BillType.InwardBill);
            sql += " and bi.bill.billType in :btypes";
            map.put("btypes", billTypes);
            //3
//            BillType[] bt = {BillType.OpdBill, BillType.InwardBill};
//            sql += " and bi.bill.billType in :btypes";
//            map.put("btypes", Arrays.asList(bt));
//            //4
//            sql += " and bi.bill.billType in :btypes";
//            map.put("btypes", Arrays.asList(new BillType[]{BillType.OpdBill, BillType.InwardBill}));
        } else {
            sql += " and bi.bill.billType=:btype";
            map.put("btype", billType);
        }

        map.put("iclass", Investigation.class);
        map.put("biclass", b.getClass());
        map.put("ins", getSessionController().getInstitution());
        map.put("fromDate", getFromDate());
        map.put("toDate", getToDate());

        billList = billFacade.findByJpql(sql, map, TemporalType.TIMESTAMP);
        return billList;

    }

    public BillType[] getBillTypeByInvestigation() {
        BillType[] bt = {BillType.OpdBill, BillType.InwardBill};
        return bt;
    }

    //getsummery type method
    public void createIncomeSummery() {

        String sql = "";
        Map m = new HashMap();
        bills = new ArrayList<>();
        incomeSummeryRows = new ArrayList<>();
        grantTotal = 0.0;

        if (summeryType.equals("1")) {
            sql = "select bi.item,count(bi),sum(bi.netValue) ";
        }
        if (summeryType.equals("2")) {
            if (totalType.equals("1")) {
                sql = "select b.department,count(b),sum(b.netTotal) ";
            }
            if (totalType.equals("2")) {
                sql = "select b.toDepartment,count(b),sum(b.netTotal) ";
            }
        }

        if (summeryType.equals("3")) {
            sql += "select distinct(bi.bill) ";
        }

        if (summeryType.equals("2")) {
            sql += "from Bill b where b.retired=false "
                    + " and b.createdAt between :fd and :td "
                    + " and b.billType=:bt ";
        } else {
            sql += "from BillItem bi where bi.retired=false "
                    + " and bi.bill.createdAt between :fd and :td "
                    + " and bi.bill.billType=:bt ";
        }

        if (!summeryType.equals("2")) {
            if (totalType.equals("1")) {
                if (department != null) {
                    sql += " and bi.bill.department=:d ";
                    m.put("d", department);
                }
            }
            if (totalType.equals("2")) {
                if (department != null) {
                    sql += " and bi.bill.toDepartment=:d ";
                    m.put("d", department);
                }
            }
        }

        if (summeryType.equals("1")) {
            sql += "group by bi.item"
                    + " order by bi.item.name ";
        }
        if (summeryType.equals("2")) {
            if (totalType.equals("1")) {
                sql += "group by b.department"
                        + " order by b.department.name ";
            }
            if (totalType.equals("2")) {
                sql += "group by b.toDepartment"
                        + " order by b.toDepartment.name ";
            }
        }

        m.put("bt", BillType.OpdBill);
        m.put("fd", fromDate);
        m.put("td", toDate);


        if (summeryType.equals("3")) {
            bills = billFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
            for (Bill b : bills) {
                grantTotal += b.getNetTotal();
            }
        }
        if (!summeryType.equals("3")) {
            List<Object[]> objects = billFacade.findObjectsArrayByJpql(sql, m, TemporalType.TIMESTAMP);
            if (!objects.isEmpty()) {
                for (Object[] ob : objects) {
                    IncomeSummeryRow row = new IncomeSummeryRow();
                    if (summeryType.equals("1")) {


                        long count = (long) ob[1];
                        if (count == 0) {
                            continue;
                        }
                        Item i = (Item) ob[0];
                        double tot = (double) ob[2];

                        row.setItem1(i);
                        row.setCount(count);
                        row.setNetValue(tot);

                        grantTotal += row.getNetValue();
                        incomeSummeryRows.add(row);
                    }

                    if (summeryType.equals("2")) {
                        long count = (long) ob[1];
                        if (count == 0) {
                            continue;
                        }
                        Department d = (Department) ob[0];
                        double tot = (double) ob[2];

                        row.setDept(d);
                        row.setCount(count);
                        row.setNetValue(tot);

                        grantTotal += row.getNetValue();
                        incomeSummeryRows.add(row);
                    }

                }
            }

        }

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
    double itemValue = 0.0;

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
        Date startTime = new Date();

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
//        ////// // System.out.println("bojsl = " + bojsl);
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
//        ////// // System.out.println("sql = " + jpql);
//        ////// // System.out.println("m = " + m);
//        ////// // System.out.println("insInvestigationCountRows.size() = " + insInvestigationCountRows.size());
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
//        ////// // System.out.println("billed = " + billed);
//        long cancelled = getCount2(new CancelledBill());
//        ////// // System.out.println("cancelled = " + cancelled);
//        long refunded = getCount2(new RefundBill());
//        ////// // System.out.println("refunded = " + refunded);
//
//        countTotal = billed - (refunded + cancelled);
    }

    public void createItemNewChanges() {
        itemsLab = new ArrayList<>();
        countTotal = 0;
        itemValue = 0;
        BillType[] bts = {BillType.LabBill, BillType.CollectingCentreBill,billType.OpdBill};
        for (Item w : getInvestigations3(bts)) {
            InvestigationSummeryData temp = new InvestigationSummeryData();
            temp.setInvestigation(w);
            long temCoint = calculateInvestigationBilledCount(w, bts);
            temp.setCount(temCoint);
            countTotal += temCoint;
            double tempTotal = calculateInvestigationBilledValue(w, bts);
            temp.setTotal(tempTotal);
            itemValue += tempTotal;
            if (temp.getCount() != 0 || temp.getTotal() != 0) {
                itemsLab.add(temp);
            }

        }
    }

    public void createItemNewChangesSummery() {
        itemsLab = new ArrayList<>();
        countTotal = 0;
        itemValue = 0;
        BillType[] bts = {BillType.LabBill, BillType.CollectingCentreBill,BillType.OpdBill};
        for (Institution i : getInstitution(bts)) {
            InvestigationSummeryData temp = new InvestigationSummeryData();
            temp.setInstitution(i);
            long temCoint = calculateInvestigationBilledCountSummery(i, bts);
            temp.setCount(temCoint);
            countTotal += temCoint;
            double tempTotal = calculateInvestigationBilledValueSummery(i, bts);
            temp.setTotal(tempTotal);
            itemValue += tempTotal;
            if (temp.getCount() != 0 || temp.getTotal() != 0) {
                itemsLab.add(temp);
            }
        }
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
        List<BillItem> temps = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

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

    private long calculateInvestigationBilledCount(Item w, BillType[] bts) {
        long billed = getCount3(new BilledBill(), w, bts);
        long cancelled = getCount3(new CancelledBill(), w, bts);
        long refunded = getCount3(new RefundBill(), w, bts);
        return billed - (cancelled + refunded);
    }

    private long calculateInvestigationBilledCountSummery(Institution i, BillType[] bts) {
        long billed = getCountSummery(new BilledBill(), i, bts);
        long cancelled = getCountSummery(new CancelledBill(), i, bts);
        long refunded = getCountSummery(new RefundBill(), i, bts);
        return billed - (cancelled + refunded);
    }

    private double calculateInvestigationBilledValue(Item w, BillType[] bts) {
        double billed = getTotalValue(new BilledBill(), w, bts);
        double cancelled = getTotalValue(new CancelledBill(), w, bts);
        double refunded = getTotalValue(new RefundBill(), w, bts);
        return billed + (cancelled + refunded);
    }

    private double calculateInvestigationBilledValueSummery(Institution i, BillType[] bts) {
        double billed = getTotalValueSummry(new BilledBill(), i, bts);
        double cancelled = getTotalValueSummry(new CancelledBill(), i, bts);
        double refunded = getTotalValueSummry(new RefundBill(), i, bts);
        return billed + (cancelled + refunded);
    }

    private long getCount3(Bill bill, Item item, BillType[] bts) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi where bi.bill.billType in :bTypes and bi.item =:itm"
                + " and type(bi.bill)=:billClass "
                + " and bi.bill.createdAt between :fromDate and :toDate ";

        if (getCreditCompany() != null) {
            sql += " and (bi.bill.collectingCentre=:col or bi.bill.fromInstitution=:col) ";
            temMap.put("col", getCreditCompany());
        } else {
            sql += " and (bi.bill.collectingCentre is not null or bi.bill.fromInstitution is not null) ";
        }
        sql += " order by bi.item.name";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", item);
        temMap.put("billClass", bill.getClass());
        temMap.put("bTypes", Arrays.asList(bts));
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private long getCountSummery(Bill bill, Institution i, BillType[] bts) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi where bi.bill.billType in :bTypes "
                + " and type(bi.bill)=:billClass "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " and (bi.bill.collectingCentre=:col or bi.bill.fromInstitution=:col) "
                + " order by bi.item.name";

        temMap.put("col", i);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billClass", bill.getClass());
        temMap.put("bTypes", Arrays.asList(bts));
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double getTotalValue(Bill bill, Item item, BillType[] bts) {
        String sql;
        Map temMap = new HashMap();
        sql = "select sum(bi.netValue) FROM BillItem bi where bi.bill.billType in :bTypes and bi.item =:itm"
                + " and type(bi.bill)=:billClass "
                + " and bi.bill.createdAt between :fromDate and :toDate ";

        if (getCreditCompany() != null) {
            sql += " and (bi.bill.collectingCentre=:col or bi.bill.fromInstitution=:col) ";
            temMap.put("col", getCreditCompany());
        } else {
            sql += " and (bi.bill.collectingCentre is not null or bi.bill.fromInstitution is not null) ";
        }
        sql += " order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", item);
        temMap.put("billClass", bill.getClass());
        temMap.put("bTypes", Arrays.asList(bts));
        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double getTotalValueSummry(Bill bill, Institution i, BillType[] bts) {
        String sql;
        Map temMap = new HashMap();
        sql = "select sum(bi.netValue) FROM BillItem bi where bi.bill.billType in :bTypes "
                + " and type(bi.bill)=:billClass "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " and (bi.bill.collectingCentre=:col or bi.bill.fromInstitution=:col) "
                + " order by bi.item.name ";

        temMap.put("col", i);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billClass", bill.getClass());
        temMap.put("bTypes", Arrays.asList(bts));
        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

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
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

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
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

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
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

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
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

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
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

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
        long billed = billEjb.getBillItemCount(w, fromDate, toDate, new BillType[]{BillType.InwardBill, BillType.LabBill, BillType.OpdBill, BillType.CollectingCentreBill}, new Class[]{BilledBill.class}, true, null, true, null, true, null, true, null);
        long cancelled = billEjb.getBillItemCount(w, fromDate, toDate, new BillType[]{BillType.InwardBill, BillType.LabBill, BillType.OpdBill, BillType.CollectingCentreBill}, new Class[]{CancelledBill.class}, true, null, true, null, true, null, true, null);
        long refunded = billEjb.getBillItemCount(w, fromDate, toDate, new BillType[]{BillType.InwardBill, BillType.LabBill, BillType.OpdBill, BillType.CollectingCentreBill}, new Class[]{RefundBill.class}, true, null, true, null, true, null, true, null);
        long net = billed - (cancelled + refunded);
        is.setCount(net);
        return is;
    }

    private InvestigationSummeryData setIxSummeryCountBilledIns(Item w, Institution i) {
        InvestigationSummeryData is = new InvestigationSummeryData();
        is.setInvestigation(w);
        long billed = billEjb.getBillItemCount(w, fromDate, toDate, new BillType[]{BillType.InwardBill, BillType.LabBill, BillType.OpdBill, BillType.CollectingCentreBill}, new Class[]{BilledBill.class}, false, i, true, null, true, null, true, null);
        long cancelled = billEjb.getBillItemCount(w, fromDate, toDate, new BillType[]{BillType.InwardBill, BillType.LabBill, BillType.OpdBill, BillType.CollectingCentreBill}, new Class[]{CancelledBill.class}, false, i, true, null, true, null, true, null);
        long refunded = billEjb.getBillItemCount(w, fromDate, toDate, new BillType[]{BillType.InwardBill, BillType.LabBill, BillType.OpdBill, BillType.CollectingCentreBill}, new Class[]{RefundBill.class}, false, i, true, null, true, null, true, null);
        long net = billed - (cancelled + refunded);
        is.setCount(net);
        return is;
    }

    private InvestigationSummeryData setIxSummeryCountReportedIns(Item w, Institution i) {
        InvestigationSummeryData is = new InvestigationSummeryData();
        is.setInvestigation(w);
        long billed = billEjb.getBillItemCount(w, fromDate, toDate, new BillType[]{BillType.InwardBill, BillType.LabBill, BillType.OpdBill, BillType.CollectingCentreBill}, new Class[]{BilledBill.class}, true, null, true, null, false, i, true, null);
        long cancelled = billEjb.getBillItemCount(w, fromDate, toDate, new BillType[]{BillType.InwardBill, BillType.LabBill, BillType.OpdBill, BillType.CollectingCentreBill}, new Class[]{CancelledBill.class}, true, null, true, null, false, i, true, null);
        long refunded = billEjb.getBillItemCount(w, fromDate, toDate, new BillType[]{BillType.InwardBill, BillType.LabBill, BillType.OpdBill, BillType.CollectingCentreBill}, new Class[]{RefundBill.class}, true, null, true, null, false, i, true, null);
        long net = billed - (cancelled + refunded);
        is.setCount(net);
        return is;
    }

    private InvestigationSummeryData setIxSummeryCountBilledDep(Item w, Department d) {
        InvestigationSummeryData is = new InvestigationSummeryData();
        is.setInvestigation(w);
        long billed = billEjb.getBillItemCount(w, fromDate, toDate, new BillType[]{BillType.InwardBill, BillType.LabBill, BillType.OpdBill, BillType.CollectingCentreBill}, new Class[]{BilledBill.class}, true, null, false, d, true, null, true, null);
        long cancelled = billEjb.getBillItemCount(w, fromDate, toDate, new BillType[]{BillType.InwardBill, BillType.LabBill, BillType.OpdBill, BillType.CollectingCentreBill}, new Class[]{CancelledBill.class}, true, null, false, d, true, null, true, null);
        long refunded = billEjb.getBillItemCount(w, fromDate, toDate, new BillType[]{BillType.InwardBill, BillType.LabBill, BillType.OpdBill, BillType.CollectingCentreBill}, new Class[]{RefundBill.class}, true, null, false, d, true, null, true, null);
        long net = billed - (cancelled + refunded);
        is.setCount(net);
        return is;
    }

    private InvestigationSummeryData setIxSummeryCountReportedDep(Item w, Department d) {
        InvestigationSummeryData is = new InvestigationSummeryData();
        is.setInvestigation(w);
        long billed = billEjb.getBillItemCount(w, fromDate, toDate, new BillType[]{BillType.InwardBill, BillType.LabBill, BillType.OpdBill, BillType.CollectingCentreBill}, new Class[]{BilledBill.class}, true, null, true, null, true, null, false, d);
        long cancelled = billEjb.getBillItemCount(w, fromDate, toDate, new BillType[]{BillType.InwardBill, BillType.LabBill, BillType.OpdBill, BillType.CollectingCentreBill}, new Class[]{CancelledBill.class}, true, null, true, null, true, null, false, d);
        long refunded = billEjb.getBillItemCount(w, fromDate, toDate, new BillType[]{BillType.InwardBill, BillType.LabBill, BillType.OpdBill, BillType.CollectingCentreBill}, new Class[]{RefundBill.class}, true, null, true, null, true, null, false, d);
        long net = billed - (cancelled + refunded);
        is.setCount(net);
        return is;
    }

    private double setTurnOverValue(long count) {

        long timeInMinutes = (getToDate().getTime() - getFromDate().getTime()) / 60000;

        long turnOverTime = timeInMinutes / count;

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
        investigations = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

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
        investigations = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

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
        investigations = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

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
        investigations = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return investigations;
    }
    
    
    
    public List<Item> getBillItemsFromCollectingCentres(BillType[] bts) {
        Map temMap = new HashMap();
        String sql = "select distinct ix from BillItem bi join bi.item ix "
                + " where type(ix) =:ixtype  "
                + " and bi.bill.billType in :bTypes "
                + " and bi.bill.collectingCentre is not null "
                + " and bi.bill.createdAt between :fromDate and :toDate ";

        if (getCreditCompany() != null) {
            sql += " and (bi.bill.collectingCentre=:col or bi.bill.fromInstitution=:col) ";
            temMap.put("col", getCreditCompany());
        } else {
            sql += " and (bi.bill.collectingCentre is not null or bi.bill.fromInstitution is not null) ";
        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ixtype", Investigation.class);
        temMap.put("bTypes", Arrays.asList(bts));

        investigations = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return investigations;
    }
    

    public List<Item> getInvestigations3(BillType[] bts) {
        Map temMap = new HashMap();
        String sql = "select distinct ix from BillItem bi join bi.item ix "
                + " where type(ix) =:ixtype  "
                + " and bi.bill.billType in :bTypes "
                + " and bi.bill.createdAt between :fromDate and :toDate ";

        if (getCreditCompany() != null) {
            sql += " and (bi.bill.collectingCentre=:col or bi.bill.fromInstitution=:col) ";
            temMap.put("col", getCreditCompany());
        } else {
            sql += " and (bi.bill.collectingCentre is not null or bi.bill.fromInstitution is not null) ";
        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ixtype", Investigation.class);
        temMap.put("bTypes", Arrays.asList(bts));

        investigations = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return investigations;
    }

    public List<Institution> getInstitution(BillType[] bts) {
        Map temMap = new HashMap();

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ixtype", Investigation.class);
        temMap.put("bTypes", Arrays.asList(bts));
        List<Institution> institutions = new ArrayList<>();
        List<Institution> institutionsTemp = new ArrayList<>();
        List<Institution> institutionsRemove = new ArrayList<>();

        String sql = "select distinct(bi.bill.collectingCentre) from BillItem bi join bi.item ix "
                + " where type(ix) =:ixtype  "
                + " and bi.bill.billType in :bTypes "
                + " and bi.bill.createdAt between :fromDate and :toDate ";

        institutions = institutionFacade.findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        sql = "select distinct(bi.bill.fromInstitution) from BillItem bi join bi.item ix "
                + " where type(ix) =:ixtype  "
                + " and bi.bill.billType in :bTypes "
                + " and bi.bill.createdAt between :fromDate and :toDate ";

        institutionsTemp = institutionFacade.findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        //// // System.out.println("institutions.size() = " + institutions.size());
        for (Institution i : institutionsTemp) {
            for (Institution in : institutions) {
                if (i.equals(in)) {
                    institutionsRemove.add(i);
                    break;
                }
            }
        }
        institutionsTemp.removeAll(institutionsRemove);
        institutions.addAll(institutionsTemp);
        return institutions;
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

        List<BillItem> temps = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
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
        List<BillItem> temps = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        t.setBillItems(temps);

    }

    public void listnerSummeryTpe() {
        department = null;
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

    public String getSummeryType() {
        return summeryType;
    }

    public void setSummeryType(String summeryType) {
        this.summeryType = summeryType;
    }

    public String getTotalType() {
        return totalType;
    }

    public void setTotalType(String totalType) {
        this.totalType = totalType;
    }

    public List<IncomeSummeryRow> getIncomeSummeryRows() {
        return incomeSummeryRows;
    }

    public void setIncomeSummeryRows(List<IncomeSummeryRow> incomeSummeryRows) {
        this.incomeSummeryRows = incomeSummeryRows;
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

    public double getItemValue() {
        return itemValue;
    }

    public void setItemValue(double itemValue) {
        this.itemValue = itemValue;
    }

    public List<InvestigationSummeryData> getItemsLab() {
        return itemsLab;
    }

    public void setItemsLab(List<InvestigationSummeryData> itemsLab) {
        this.itemsLab = itemsLab;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public List<Bill> getBilledBills() {
        return billedBills;
    }

    public void setBilledBills(List<Bill> billedBills) {
        this.billedBills = billedBills;
    }

    public List<Bill> getCancelledBills() {
        return cancelledBills;
    }

    public void setCancelledBills(List<Bill> cancelledBills) {
        this.cancelledBills = cancelledBills;
    }

    public List<Bill> getRefundedBills() {
        return refundedBills;
    }

    public void setRefundedBills(List<Bill> refundedBills) {
        this.refundedBills = refundedBills;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public double getGrantTotal() {
        return grantTotal;
    }

    public void setGrantTotal(double grantTotal) {
        this.grantTotal = grantTotal;
    }

    public class BillListTotal {

        double hosFee;
        double staffFee;
        double netTotal;

        public double getHosFee() {
            return hosFee;
        }

        public void setHosFee(double hosFee) {
            this.hosFee = hosFee;
        }

        public double getStaffFee() {
            return staffFee;
        }

        public void setStaffFee(double staffFee) {
            this.staffFee = staffFee;
        }

        public double getNetTotal() {
            return netTotal;
        }

        public void setNetTotal(double netTotal) {
            this.netTotal = netTotal;
        }

    }

    //class for income summery
    public class IncomeSummeryRow {

        private Department dept;
        private long count;
        private double netValue;
        private Item item1;

        public Department getDept() {
            return dept;
        }

        public void setDept(Department dept) {
            this.dept = dept;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public double getNetValue() {
            return netValue;
        }

        public void setNetValue(double netValue) {
            this.netValue = netValue;
        }

        public Item getItem1() {
            return item1;
        }

        public void setItem1(Item item1) {
            this.item1 = item1;
        }

    }

    public BillListTotal getBilllistTotal() {
        return billlistTotal;
    }

    public void setBilllistTotal(BillListTotal billlistTotal) {
        this.billlistTotal = billlistTotal;
    }

}
