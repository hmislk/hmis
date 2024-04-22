/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.report;

import com.divudi.bean.common.AuditEventApplicationController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.data.FeeType;
import com.divudi.data.dataStructure.BillsTotals;
import com.divudi.data.table.String1Value1;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.AuditEvent;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Doctor;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.Person;
import com.divudi.entity.PersonInstitution;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.entity.lab.Investigation;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PersonInstitutionFacade;
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
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author www.divudi.com
 */
@Named
@SessionScoped
public class CommonReport1 implements Serializable {

    @Inject
    SessionController sessionController;
    @Inject
    CommonController commonController;
    @Inject AuditEventApplicationController auditEventApplicationController;
    ///////////////////
    @EJB
    private BillFacade billFacade;
    @EJB
    PersonInstitutionFacade personInstitutionFacade;

 
    CommonFunctions commonFunctions;
    ////////////////////
    private Institution collectingIns;
    Institution institution;
    private Date fromDate;
    private Date toDate;
    private WebUser webUser;
    private Department department;
    private BillType billType;
    private Institution creditCompany;
    PaymentScheme paymentScheme;
    Item item;
    Department incomeDepartment;
    Category category;
    /////////////////////
    private BillsTotals billedBills;
    private BillsTotals cancellededBills;
    private BillsTotals refundedBills;
    private BillsTotals billedBillsPh;
    private BillsTotals billedBillsPh2;
    private BillsTotals cancellededBillsPh;
    private BillsTotals cancellededBillsPh2;
    private BillsTotals refundedBillsPh;
    private BillsTotals refundedBillsPh2;
    private BillsTotals paymentBills;
    private BillsTotals paymentCancelBills;
    private BillsTotals pettyPayments;
    private BillsTotals pettyPaymentsCancel;
    private BillsTotals cashRecieves;
    private BillsTotals cashRecieveCancel;
    private BillsTotals agentRecieves;
    private BillsTotals agentCancelBill;
    private BillsTotals inwardPayments;
    private BillsTotals inwardPaymentCancel;
    //////////////////    
    private List<String1Value1> dataTableData;
    List<DocTotal> docTotals;
    ///
    List<Bill> biledBills;
    List<Bill> cancelBills;
    List<Bill> refundBills;

    List<BillItem> billItems;

    double biledBillsTotal;
    double cancelBillsTotal;
    double refundBillsTotal;
    double discount;
    double staffTotal;
    double netTotal;
    double vat;

    Doctor referringDoctor;

    boolean onlyOPD;

    String radio = "1";

    public Doctor getReferringDoctor() {
        return referringDoctor;
    }

    public void setReferringDoctor(Doctor referringDoctor) {
        this.referringDoctor = referringDoctor;
    }

    /**
     * Creates a new instance of CommonReport
     */
    public CommonReport1() {
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
        recreteModal();
    }

    public BillType[] getBillTypes() {
        return BillType.values();
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
        recreteModal();
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
        recreteModal();
    }

    public WebUser getWebUser() {

        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
        recreteModal();
    }

    public Department getDepartment() {
//        if (department == null) {
//            department = getSessionController().getDepartment();
//        }
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
        recreteModal();
    }

    public List<Bill> getBillsByReferingDoc() {

        Map temMap = new HashMap();
        List<Bill> tmp;

        String sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.billType = :bTp and b.createdAt between :fromDate and :toDate  order by b.referredBy.person.name";
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bTp", BillType.OpdBill);

        tmp = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        if (tmp == null) {
            tmp = new ArrayList<Bill>();
        }

        return tmp;

    }

    public List<Bill> getBillsByCollecting() {

        Map temMap = new HashMap();
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        String sql;
        List<Bill> tmp;

        if (collectingIns == null) {
            //sql = "SELECT b FROM BilledBill b WHERE b.retired=false  and b.createdAt between :fromDate and :toDate  order by b.collectingCentre.name";
            sql = "SELECT b FROM BilledBill b WHERE b.retired=false and  b.createdAt between :fromDate and :toDate  order by b.collectingCentre.name";
        } else {
            sql = "SELECT b FROM BilledBill b WHERE b.retired=false   and  b.collectingCentre=:col  and b.createdAt between :fromDate and :toDate  order by b.collectingCentre.name";
            temMap.put("col", getCollectingIns());
        }

        tmp = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        if (tmp == null) {
            tmp = new ArrayList<Bill>();
        }

        return tmp;

    }

    public BillsTotals getUserBills() {
        if (billedBills == null) {
            getBilledBills().setBills(userBills(new BilledBill(), BillType.OpdBill, getWebUser()));
            // calTot(getBilledBills());
        }

        return billedBills;
    }

    public BillsTotals getUserCancelledBills() {
        if (cancellededBills == null) {
            getCancellededBills().setBills(userBills(new CancelledBill(), BillType.OpdBill, getWebUser()));
            //   calTot(getCancellededBills());
        }
        return cancellededBills;
    }

    public BillsTotals getUserRefundedBills() {
        if (refundedBills == null) {
            getRefundedBills().setBills(userBills(new RefundBill(), BillType.OpdBill, getWebUser()));
            //  calTot(getRefundedBills());
        }
        return refundedBills;
    }

    public BillsTotals getUserPaymentBills() {
        if (paymentBills == null) {
            getPaymentBills().setBills(userBills(new BilledBill(), BillType.PaymentBill, getWebUser()));
            //  calTot(getPaymentBills());
        }
        return paymentBills;
    }

    public BillsTotals getUserPaymentCancelBills() {
        if (paymentCancelBills == null) {
            getPaymentCancelBills().setBills(userBills(new CancelledBill(), BillType.PaymentBill, getWebUser()));
            //   calTot(getPaymentCancelBills());
        }
        return paymentCancelBills;
    }

    public BillsTotals getInstitutionBilledBills() {
        if (billedBills == null) {
            getBilledBills().setBills(institutionBill(getInstitution(), new BilledBill(), BillType.OpdBill));
            //  calTot(getBilledBills());
        }
        return billedBills;
    }

    public BillsTotals getInstitutionCancelledBills() {
        if (cancellededBills == null) {
            getCancellededBills().setBills(institutionBill(getInstitution(), new CancelledBill(), BillType.OpdBill));
            //   calTot(getCancellededBills());
        }
        return cancellededBills;
    }

    public BillsTotals getInstitutionRefundedBills() {
        if (refundedBills == null) {
            getRefundedBills().setBills(institutionBill(getInstitution(), new RefundBill(), BillType.OpdBill));
            //  calTot(getRefundedBills());
        }
        return refundedBills;
    }

    public BillsTotals getInstitutionPaymentBills() {
        if (paymentBills == null) {
            getPaymentBills().setBills(institutionBill(getInstitution(), new BilledBill(), BillType.PaymentBill));
            // calTot(getPaymentBills());
        }
        return paymentBills;
    }

    public BillsTotals getInstitutionPaymentCancelBills() {
        if (paymentCancelBills == null) {
            getPaymentCancelBills().setBills(institutionBill(getInstitution(), new CancelledBill(), BillType.PaymentBill));
            // calTot(getPaymentCancelBills());
        }
        return paymentCancelBills;
    }

    public BillsTotals getUserPaymentBillsOwn() {
        if (paymentBills == null) {
            getPaymentBills().setBills(userBillsOwn(new BilledBill(), BillType.PaymentBill, getWebUser()));
        }
        //   calTot(getPaymentBills());
        return paymentBills;
    }

    public BillsTotals getUserInwardPaymentBillsOwn() {
        if (inwardPayments == null) {
            getInwardPayments().setBills(userBillsOwn(new BilledBill(), BillType.InwardPaymentBill, getWebUser()));
        }
        //  calTot(getInwardPayments());
        return inwardPayments;
    }

    public BillsTotals getInstitutionInwardPaymentBillsOwn() {
        if (inwardPayments == null) {
            getInwardPayments().setBills(billsOwn(new BilledBill(), BillType.InwardPaymentBill));
        }
        //  calTot(getInwardPayments());
        return inwardPayments;
    }

    @EJB
    BillItemFacade billItemFacade;
    List<BillItem> referralBillItems;

    public List<BillItem> getReferralBillItems() {
        return referralBillItems;
    }

    public void setReferralBillItems(List<BillItem> referralBillItems) {
        this.referralBillItems = referralBillItems;
    }

    public void listBillItemsByReferringDoctor() {

        Date startTime = new Date();

        referralBillItems = new ArrayList<>();
        Map m = new HashMap();
        String sql;

        sql = "SELECT bi FROM BillItem bi "
                + " join bi.bill b "
                + " join bi.item i "
                + " WHERE b.retired=false "
                + " and b.referredBy is not null "
                + " and bi.retired=false "
                + " and (bi.refunded=false or bi.refunded is null) "
                + " and b.createdAt between :fromDate and :toDate  ";

        if (radio.equals("0")) {
            sql += " and (b.billType=:bt1 or b.billType=:bt2) ";
            m.put("bt1", BillType.OpdBill);
            m.put("bt2", BillType.CollectingCentreBill);
        }

        if (radio.equals("1")) {
            sql += " and b.billType=:bt ";
            m.put("bt", BillType.CollectingCentreBill);
        }

        if (radio.equals("2")) {
            sql += " and b.billType=:bt ";
            m.put("bt", BillType.OpdBill);
        }

        if (department != null) {
            sql += " and i.department=:dept ";
            m.put("dept", department);
        }

        if (referringDoctor != null) {
            m.put("rd", referringDoctor);
            sql = sql + " and b.referredBy=:rd ";
        }
        sql = sql + " order by b.referredBy.person.name";

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        ////// // System.out.println("sql = " + sql);
        ////// // System.out.println("temMap = " + temMap);
        referralBillItems = billItemFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

        biledBillsTotal = 0.0;
        for (BillItem bi : referralBillItems) {
            biledBillsTotal += bi.getNetValue() + bi.getVat();
        }

        

    }

    List<Bill> bill;

    public List<Bill> getBill() {
        return bill;
    }

    public void setBill(List<Bill> bill) {
        this.bill = bill;
    }

    public void listBillItemsByReferringDoctorIncome() {

        Date startTime = new Date();

//        referralBillItems = new ArrayList<>();
        Map m = new HashMap();

        String sql;

        sql = "SELECT b FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.referredBy is not null "
                + " and (b.refunded=false or b.refunded is null) "
                + " and b.createdAt between :fromDate and :toDate  "
                + " and b.billType=:bt ";

        if (department != null) {
            sql += " and b.department=:dept ";
            m.put("dept", department);
        }
        if (referringDoctor != null) {
            m.put("rd", referringDoctor);
            sql = sql + " and b.referredBy=:rd ";
        }
        sql = sql + " group by b.referredBy.person.name";

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        m.put("bt", BillType.OpdBill);
        ////// // System.out.println("sql = " + sql);
        ////// // System.out.println("temMap = " + temMap);
//        referralBillItems = billItemFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        bill = billFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

        biledBillsTotal = 0.0;
        for (Bill bilst : bill) {
            biledBillsTotal += bilst.getNetTotal();
        }

        

    }

    public void listBillItemsByReferringDoctorSummery() {

        Map m = new HashMap();
        String sql;
        docTotals = new ArrayList<>();

        sql = "SELECT b.referredBy,sum(b.netTotal+b.vat) FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.referredBy is not null "
                + " and b.createdAt between :fromDate and :toDate ";

        if (radio.equals("0")) {
            sql += " and (b.billType=:bt1 or b.billType=:bt2) ";
            m.put("bt1", BillType.OpdBill);
            m.put("bt2", BillType.CollectingCentreBill);
        }

        if (radio.equals("1")) {
            sql += " and b.billType=:bt ";
            m.put("bt", BillType.CollectingCentreBill);
        }

        if (radio.equals("2")) {
            sql += " and b.billType=:bt ";
            m.put("bt", BillType.OpdBill);
        }

        sql += " group by b.referredBy "
                + " order by b.referredBy.person.name ";

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());

        List<Object[]> objects = billItemFacade.findAggregates(sql, m, TemporalType.TIMESTAMP);
        biledBillsTotal = 0.0;
        for (Object[] o : objects) {
            Doctor d = (Doctor) o[0];
            double tot = (double) o[1];
            DocTotal row = new DocTotal();
            row.setDoctor(d);
            row.setTotal(tot);
            docTotals.add(row);
            biledBillsTotal += tot;
        }

    }

    public void listBillItemsByReferringDoctorSummeryCount() {

        Map m = new HashMap();
        String sql;
        docTotals = new ArrayList<>();
        if (radio.equals("3")) {
            sql = "SELECT b.patientEncounter.referringDoctor, ";
        } else {
            sql = "SELECT b.referredBy, ";
        }

        sql += " b.billClassType, "
                + " count(bi), "
                + " sum(bi.netValue+bi.vat) "
                + " FROM BillItem bi join bi.bill b "
                + " WHERE b.retired=false "
                + " and b.createdAt between :fromDate and :toDate "
                + " and type(bi.item)=:inv ";

        if (radio.equals("0")) {
            sql += " and b.billType in :bts ";
            m.put("bts", Arrays.asList(new BillType[]{BillType.OpdBill, BillType.CollectingCentreBill}));
        }

        if (radio.equals("1")) {
            sql += " and b.billType=:bt ";
            m.put("bt", BillType.CollectingCentreBill);
        }

        if (radio.equals("2")) {
            sql += " and b.billType=:bt ";
            m.put("bt", BillType.OpdBill);
        }
        if (radio.equals("3")) {
            sql += " and b.billType=:bt ";
            m.put("bt", BillType.InwardBill);
        }

        if (radio.equals("3")) {
            sql += " group by b.patientEncounter.referringDoctor,";
        } else {
            sql += " group by b.referredBy, ";
        }
        sql += " b.billClassType ";

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        m.put("inv", Investigation.class);
        //// // System.out.println("sql = " + sql);
        List<Object[]> objects = billItemFacade.findAggregates(sql, m, TemporalType.TIMESTAMP);
        List<Object[]> obj = fetchReferingDoctoerNull();
        if (objects == null) {
            objects = new ArrayList<>();
            objects.addAll(obj);
        } else {
            objects.addAll(obj);
        }
        biledBillsTotal = 0.0;
        DocTotal row = new DocTotal();
        Doctor lastDoctor = null;
        double total = 0.0;
        long count = 0l;
        for (Object[] o : objects) {
            Doctor d = (Doctor) o[0];
            if (d == null) {
                Doctor doc = new Doctor();
                Person p = new Person();
                p.setName("No Name");
                doc.setPerson(p);
                d = doc;
                continue;
            }
            if (onlyOPD) {
                if (checkDoctor(d)) {
                    continue;
                }
            }
            //// // System.out.println("d.getName() = " + d.getPerson().getName());
            BillClassType billClassType = (BillClassType) o[1];
            long l = (long) o[2];
            if (billClassType == BillClassType.CancelledBill || billClassType == BillClassType.RefundBill) {
                if (l > 0) {
                    l *= -1;
                }
            }
            double tot = 0.0;
            try {
                tot = (double) o[3];
            } catch (Exception e) {
            }
            if (lastDoctor == null) {
                row.setDoctor(d);
                row.setTotal(tot);
                row.setCount(l);
                count += l;
                total += tot;
                lastDoctor = d;
            } else {
                if (lastDoctor == d) {
                    row.setTotal(row.getTotal() + tot);
                    row.setCount(row.getCount() + l);
                    count += l;
                    total += tot;
                } else {
                    docTotals.add(row);
                    row = new DocTotal();
                    row.setDoctor(d);
                    row.setTotal(tot);
                    row.setCount(l);
                    count += l;
                    total += tot;
                    lastDoctor = d;
                }
            }
        }
        docTotals.add(row);
        row = new DocTotal();
        row.setDoctor(null);
        row.setTotal(total);
        row.setCount(count);
        docTotals.add(row);

    }

    private List<Object[]> fetchReferingDoctoerNull() {

        Map m = new HashMap();
        String sql;
        docTotals = new ArrayList<>();
        sql = "SELECT b.billClassType, "
                + " count(bi), "
                + " sum(bi.netValue+bi.vat) "
                + " FROM BillItem bi join bi.bill b "
                + " WHERE b.retired=false "
                + " and b.createdAt between :fromDate and :toDate "
                + " and type(bi.item)=:inv ";

        if (radio.equals("0")) {
            sql += " and b.billType in :bts ";
            m.put("bts", Arrays.asList(new BillType[]{BillType.OpdBill, BillType.CollectingCentreBill}));
        }

        if (radio.equals("1")) {
            sql += " and b.billType=:bt ";
            m.put("bt", BillType.CollectingCentreBill);
        }

        if (radio.equals("2")) {
            sql += " and b.billType=:bt ";
            m.put("bt", BillType.OpdBill);
        }
        if (radio.equals("3")) {
            sql += " and b.billType=:bt ";
            m.put("bt", BillType.InwardBill);
        }

        if (radio.equals("3")) {
            sql += " and b.patientEncounter.referringDoctor is null";
        } else {
            sql += " and b.referredBy is null ";
        }

        sql += " group by b.billClassType ";

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        m.put("inv", Investigation.class);
        List<Object[]> obj = billItemFacade.findAggregates(sql, m, TemporalType.TIMESTAMP);
        List<Object[]> objects = new ArrayList<>();
        if (!obj.isEmpty()) {
            Object[] o = new Object[4];
            o[0] = null;
            o[1] = BillClassType.BilledBill;
            long count = 0l;
            double total = 0.0;
            for (Object[] ob : obj) {
                BillClassType bct = (BillClassType) ob[0];
                long c = (long) ob[1];
                double t = (double) ob[2];
                if (bct == BillClassType.CancelledBill || bct == BillClassType.RefundBill) {
                    if (c < 0) {
                        count += c;
                    } else {
                        count -= c;
                    }
                    total += t;
                } else {
                    count += c;
                    total += t;
                }
            }
            o[2] = count;
            o[3] = total;
            objects.add(o);
        }
        return objects;

    }

    public void listBillItemsByReferringDoctorSummeryIncome() {

        Map m = new HashMap();
        String sql;
        docTotals = new ArrayList<>();

        sql = "SELECT b.referredBy,sum(b.netTotal+b.vat) FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.referredBy is not null "
                + " and b.createdAt between :fromDate and :toDate  "
                + " and b.billType=:bt "
                + " group by b.referredBy "
                + " order by b.referredBy.person.name ";

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        m.put("bt", BillType.OpdBill);

        List<Object[]> objects = billItemFacade.findAggregates(sql, m, TemporalType.TIMESTAMP);
        biledBillsTotal = 0.0;
        for (Object[] o : objects) {
            Doctor d = (Doctor) o[0];
            double tot = (double) o[1];
            DocTotal row = new DocTotal();
            row.setDoctor(d);
            row.setTotal(tot);
            docTotals.add(row);
            biledBillsTotal += tot;
        }

    }

    public List<Bill> getBillsByCollectingOwn() {

        Map temMap = new HashMap();
        List<Bill> tmp;
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bType", BillType.LabBill);
        String sql;

        if (collectingIns == null) {
            sql = "SELECT b FROM BilledBill b WHERE b.retired=false and  b.billType=:bType and  b.institution=:ins and b.createdAt between :fromDate and :toDate  order by b.collectingCentre.name";
        } else {
            sql = "SELECT b FROM BilledBill b WHERE b.retired=false and  b.billType =:bType and b.institution=:ins and b.collectingCentre=:col and b.createdAt between :fromDate and :toDate  order by b.collectingCentre.name";
            temMap.put("col", getCollectingIns());
        }
        tmp = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        if (tmp == null) {
            tmp = new ArrayList<Bill>();
        }

        total = 0.0;

        for (Bill b : tmp) {
            total += b.getNetTotal();
        }

        return tmp;

    }

    public List<Bill> getBillsByLabCreditOwn() {

        Map temMap = new HashMap();
        List<Bill> tmp;
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        //   temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bType", BillType.LabBill);
        temMap.put("col", getCreditCompany());
        String sql;

        sql = "SELECT b FROM BilledBill b WHERE b.retired=false and  b.billType =:bType "
                + "  and b.creditCompany=:col and b.createdAt between :fromDate and :toDate "
                + "order by b.creditCompany.name";

        tmp = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        if (tmp == null) {
            tmp = new ArrayList<>();
        }

        total = 0.0;

        for (Bill b : tmp) {
            total += b.getNetTotal();
        }

        return tmp;

    }

    private double total;

    public BillsTotals getUserRefundedBillsOwn() {
        if (refundedBills == null) {
            getRefundedBills().setBills(userBillsOwn(new RefundBill(), BillType.OpdBill, getWebUser()));
        }
        // calTot(getRefundedBills());
        return refundedBills;
    }

    public BillsTotals getUserRefundedBillsOwnPh() {
        if (refundedBillsPh == null) {
            getRefundedBillsPh().setBills(userPharmacyBillsOwn(new RefundBill(), BillType.PharmacySale, getWebUser()));
        }

        if (refundedBillsPh2 == null) {
            getRefundedBillsPh2().setBills(userPharmacyBillsOther(new RefundBill(), BillType.PharmacySale, getWebUser()));
        }
        // calTot(getRefundedBills());
        return refundedBillsPh;
    }

    public BillsTotals getUserRefundedBillsPhOther() {
        if (refundedBillsPh2 == null) {
            getRefundedBillsPh2().setBills(userPharmacyBillsOther(new RefundBill(), BillType.PharmacySale, getWebUser()));
        }
        // calTot(getRefundedBills());
        return refundedBillsPh2;
    }

    public BillsTotals getUserCancelledBillsOwn() {
        if (cancellededBills == null) {
            getCancellededBills().setBills(userBillsOwn(new CancelledBill(), BillType.OpdBill, getWebUser()));
        }
        //   calTot(getCancellededBills());
        return cancellededBills;
    }

    public BillsTotals getUserCancelledBillsOwnPh() {
        if (cancellededBillsPh == null) {
            getCancellededBillsPh().setBills(userPharmacyBillsOwn(new CancelledBill(), BillType.PharmacySale, getWebUser()));
        }

        if (cancellededBillsPh2 == null) {
            getCancellededBillsPh2().setBills(userPharmacyBillsOther(new CancelledBill(), BillType.PharmacySale, getWebUser()));
        }
        //   calTot(getCancellededBills());
        return cancellededBillsPh;
    }

    public BillsTotals getUserCancelledBillsPhOther() {
        if (cancellededBillsPh2 == null) {
            getCancellededBillsPh2().setBills(userPharmacyBillsOther(new CancelledBill(), BillType.PharmacySale, getWebUser()));
        }
        //   calTot(getCancellededBills());
        return cancellededBillsPh2;
    }

    private List<Bill> userBillsOwn(Bill billClass, BillType billType, WebUser webUser) {
        String sql = "SELECT b FROM Bill b WHERE type(b)=:bill and b.retired=false and b.billType = :btp"
                + " and b.creater=:web and b.institution=:ins and b.createdAt between :fromDate and :toDate ";
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bill", billClass.getClass());
        temMap.put("btp", billType);
        temMap.put("web", webUser);
        temMap.put("ins", getSessionController().getInstitution());

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private List<Bill> userPharmacyBillsOwn(Bill billClass, BillType billType, WebUser webUser) {
        String sql = "SELECT b FROM Bill b WHERE type(b)=:bill and b.retired=false and b.billType = :btp"
                + " and b.creater=:web and b.referenceBill.institution=:ins and b.createdAt between :fromDate and :toDate ";
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bill", billClass.getClass());
        temMap.put("btp", billType);
        temMap.put("web", webUser);
        temMap.put("ins", getSessionController().getInstitution());

//        checkOtherInstiution
        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private List<Bill> userPharmacyBillsOther(Bill billClass, BillType billType, WebUser webUser) {
        String sql = "SELECT b FROM Bill b WHERE type(b)=:bill and b.retired=false and b.billType = :btp"
                + " and b.creater=:web and b.referenceBill.institution!=:ins and b.createdAt between :fromDate and :toDate ";
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bill", billClass.getClass());
        temMap.put("btp", billType);
        temMap.put("web", webUser);
        temMap.put("ins", getSessionController().getInstitution());

        Bill b = getBillFacade().findFirstByJpql(sql, temMap, TemporalType.DATE);

        if (b != null && institution == null) {
            //System.err.println("SYS "+b.getInstitution().getName());
            institution = b.getInstitution();
        }

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private List<Bill> institutionBill(Institution ins, Bill billClass, BillType billType) {
        String sql;
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bill", billClass.getClass());
        temMap.put("btp", billType);

        if (institution == null) {
            sql = "SELECT b FROM Bill b WHERE type(b)=:bill and b.retired=false and b.billType = :btp"
                    + " and  b.createdAt between :fromDate and :toDate ";
        } else {
            sql = "SELECT b FROM Bill b WHERE type(b)=:bill and b.retired=false and b.billType = :btp"
                    + " and b.institution=:ins and b.createdAt between :fromDate and :toDate";

            temMap.put("ins", ins);
        }

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private List<Bill> userBills(Bill billClass, BillType billType, WebUser webUser) {
        String sql = "SELECT b FROM Bill b WHERE type(b)=:bill and b.retired=false and b.billType = :btp"
                + " and b.creater=:web and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bill", billClass.getClass());
        temMap.put("btp", billType);
        temMap.put("web", webUser);

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public BillsTotals getUserBillsOwn() {
        if (billedBills == null) {
            getBilledBills().setBills(userBillsOwn(new BilledBill(), BillType.OpdBill, getWebUser()));
            //   calTot(getBilledBills());
        }
        return billedBills;
    }

    public BillsTotals getUserBillsOwnPh() {
        if (billedBillsPh == null) {
            getBilledBillsPh().setBills(userPharmacyBillsOwn(new BilledBill(), BillType.PharmacySale, getWebUser()));
            //   calTot(getBilledBills());
        }

        if (billedBillsPh2 == null) {
            getBilledBillsPh2().setBills(userPharmacyBillsOther(new BilledBill(), BillType.PharmacySale, getWebUser()));
            //   calTot(getBilledBills());
        }

        return billedBillsPh;
    }

    public BillsTotals getUserBillsPhOther() {
        if (billedBillsPh2 == null) {
            getBilledBillsPh2().setBills(userPharmacyBillsOther(new BilledBill(), BillType.PharmacySale, getWebUser()));
            //   calTot(getBilledBills());
        }
        return billedBillsPh2;
    }

    public BillsTotals getInstitutionPaymentBillsOwn() {
        if (paymentBills == null) {
            getPaymentBills().setBills(billsOwn(new BilledBill(), BillType.PaymentBill));
            //   calTot(getPaymentBills());
        }
        return paymentBills;
    }

    public BillsTotals getInstitutionRefundedBillsOwn() {
        if (refundedBills == null) {
            getRefundedBills().setBills(billsOwn(new RefundBill(), BillType.OpdBill));
        }
        //  calTot(getRefundedBills());
        return refundedBills;
    }

    public BillsTotals getInstitutionRefundedBillsOwnPh() {
        if (refundedBillsPh == null) {
            getRefundedBillsPh().setBills(billsOwn(new RefundBill(), BillType.PharmacySale));
        }
        //  calTot(getRefundedBills());
        return refundedBillsPh;
    }

    public BillsTotals getInstitutionCancelledBillsOwn() {
        if (cancellededBills == null) {
            getCancellededBills().setBills(billsOwn(new CancelledBill(), BillType.OpdBill));
        }
        ///   calTot(getCancellededBills());
        return cancellededBills;
    }

    public BillsTotals getInstitutionCancelledBillsOwnPh() {
        if (cancellededBillsPh == null) {
            getCancellededBillsPh().setBills(billsOwn(new CancelledBill(), BillType.PharmacySale));
        }
        ///   calTot(getCancellededBills());
        return cancellededBillsPh;
    }

    public BillsTotals getUserAgentRecieveBills() {
        if (agentRecieves == null) {
            getAgentRecieves().setBills(userBillsOwn(new BilledBill(), BillType.AgentPaymentReceiveBill, getWebUser()));
            //  calTot(getAgentRecieves());
        }
        return agentRecieves;
    }

    public BillsTotals getUserCashRecieveBills() {
        if (cashRecieves == null) {
            getCashRecieves().setBills(userBillsOwn(new BilledBill(), BillType.CashRecieveBill, getWebUser()));
        }
        // calTot(getCashRecieves());
        return cashRecieves;
    }

    public BillsTotals getUserAgentRecieveBillCancel() {
        if (agentCancelBill == null) {
            getAgentCancelBill().setBills(userBillsOwn(new CancelledBill(), BillType.AgentPaymentReceiveBill, getWebUser()));
            //  calTot(getAgentCancelBill());
        }
        return agentCancelBill;
    }

    public BillsTotals getUserCashRecieveBillCancel() {
        if (cashRecieveCancel == null) {
            getCashRecieveCancel().setBills(userBillsOwn(new CancelledBill(), BillType.CashRecieveBill, getWebUser()));
            // calTot(getCashRecieveCancel());
        }
        return cashRecieveCancel;
    }

    public BillsTotals getUserPettyPaymentBills() {
        if (pettyPayments == null) {
            getPettyPayments().setBills(userBillsOwn(new BilledBill(), BillType.PettyCash, getWebUser()));
            // calTot(getPettyPayments());
        }
        return pettyPayments;
    }

    public BillsTotals getUserPettyPaymentCancelBills() {
        if (pettyPaymentsCancel == null) {
            List<Bill> tmp = userBillsOwn(new CancelledBill(), BillType.PettyCash, getWebUser());
            getPettyPaymentsCancel().setBills(tmp);
            //  calTot(getPettyPaymentsCancel());
        }
        return pettyPaymentsCancel;
    }

    public BillsTotals getUserPaymentCancelBillsOwn() {
        if (paymentCancelBills == null) {
            List<Bill> tmp = userBillsOwn(new CancelledBill(), BillType.PaymentBill, getWebUser());
            getPaymentCancelBills().setBills(tmp);
        }
        //   calTot(getPaymentCancelBills());
        return paymentCancelBills;
    }

    public BillsTotals getUserInwardPaymentCancelBillsOwn() {
        if (inwardPaymentCancel == null) {
            List<Bill> tmp = userBillsOwn(new CancelledBill(), BillType.InwardPaymentBill, getWebUser());
            getInwardPaymentCancel().setBills(tmp);
        }
        //      calTot(getInwardPaymentCancel());
        return inwardPaymentCancel;
    }

    public BillsTotals getInstitutionInwardPaymentCancelBillsOwn() {
        if (inwardPaymentCancel == null) {
            List<Bill> tmp = billsOwn(new CancelledBill(), BillType.InwardPaymentBill);
            getInwardPaymentCancel().setBills(tmp);
        }
        //    calTot(getInwardPaymentCancel());
        return inwardPaymentCancel;
    }

    public BillsTotals getInstitutionPaymentCancelBillsOwn() {
        if (paymentCancelBills == null) {
            List<Bill> tmp = billsOwn(new CancelledBill(), BillType.PaymentBill);

            getPaymentCancelBills().setBills(tmp);
        }
        //   calTot(getPaymentCancelBills());
        return paymentCancelBills;
    }

    public BillsTotals getInstitutionPettyPaymentBillsOwn() {
        if (pettyPayments == null) {
            List<Bill> tmp = billsOwn(new BilledBill(), BillType.PettyCash);

            getPettyPayments().setBills(tmp);
            //   calTot(getPettyPayments());
        }
        return pettyPayments;
    }

//    public List<Bill> getInstitutionPettyPaymentBills() {
//        if (pettyPayments == null) {
//            String sql;
//            sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.billType = :bTp and  b.createdAt between :fromDate and :toDate order by b.id";
//
//            Map temMap = new HashMap();
//            temMap.put("fromDate", getFromDate());
//            temMap.put("toDate", getToDate());
//            temMap.put("bTp", BillType.PettyCash);
//            pettyPayments = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//            if (pettyPayments == null) {
//                pettyPayments = new ArrayList<Bill>();
//            }
//
//        }
//
//        calPettyTotal();
//        return pettyPayments;
//    }
    public BillsTotals getInstitutionPettyCancellBillsOwn() {
        if (pettyPaymentsCancel == null) {
            List<Bill> tmp = billsOwn(new CancelledBill(), BillType.PettyCash);
            getPettyPaymentsCancel().setBills(tmp);
//        calTot(getPettyPaymentsCancel());
        }
        return pettyPaymentsCancel;
    }

//    public List<Bill> getInstitutionPettyCancellBills() {
//        if (pettyPaymentsCancel == null) {
//            String sql;
//
//            sql = "SELECT b FROM CancelledBill b WHERE b.retired=false and b.billType = :bTp and b.createdAt between :fromDate and :toDate order by b.id";
//
//            Map temMap = new HashMap();
//            temMap.put("fromDate", getFromDate());
//            temMap.put("toDate", getToDate());
//            temMap.put("bTp", BillType.PettyCash);
//            pettyPaymentsCancel = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//            if (pettyPaymentsCancel == null) {
//                pettyPaymentsCancel = new ArrayList<Bill>();
//            }
//
//        }
//        calPettyCancelTotal();
//        return pettyPaymentsCancel;
//    }
    public BillsTotals getInstitutionCashRecieveBillsOwn() {
        if (cashRecieves == null) {
            getCashRecieves().setBills(billsOwn(new BilledBill(), BillType.CashRecieveBill));
            //      calTot(getCashRecieves());
        }
        return cashRecieves;
    }

//    public List<Bill> getInstitutionCashRecieveBills() {
//        if (cashRecieves == null) {
//            String sql;
//
//            sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.billType = :bTp and  b.createdAt between :fromDate and :toDate order by b.id";
//
//            Map temMap = new HashMap();
//            temMap.put("fromDate", getFromDate());
//            temMap.put("toDate", getToDate());
//            temMap.put("bTp", BillType.CashRecieveBill);
//            cashRecieves = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//            if (cashRecieves == null) {
//                cashRecieves = new ArrayList<Bill>();
//            }
//
//        }
//        calCashRecieveTot();
//        return cashRecieves;
//    }
    public BillsTotals getInstitutionAgentBillsOwn() {
        if (agentRecieves == null) {
            getAgentRecieves().setBills(billsOwn(new BilledBill(), BillType.AgentPaymentReceiveBill));
        }
        //    calTot(getAgentRecieves());
        return agentRecieves;
    }

//    public List<Bill> getInstitutionAgentBills() {
//        if (agentRecieves == null) {
//            String sql;
//
//            sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.billType = :bTp and  b.createdAt between :fromDate and :toDate order by b.id";
//
//            Map temMap = new HashMap();
//            temMap.put("fromDate", getFromDate());
//            temMap.put("toDate", getToDate());
//            temMap.put("bTp", BillType.AgentPaymentReceiveBill);
//            agentRecieves = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//            if (agentRecieves == null) {
//                agentRecieves = new ArrayList<Bill>();
//            }
//
//        }
//        calAgentTotal();
//        return agentRecieves;
//    }
    public BillsTotals getInstitutionCashRecieveCancellBillsOwn() {
        if (cashRecieveCancel == null) {
            getCashRecieveCancel().setBills(billsOwn(new CancelledBill(), BillType.CashRecieveBill));
            //    calTot(getCashRecieveCancel());
        }
        return cashRecieveCancel;
    }

//    public List<Bill> getInstitutionCashRecieveCancellBills() {
//        if (cashRecieveCancel == null) {
//            String sql;
//
//            sql = "SELECT b FROM CancelledBill b WHERE b.retired=false and b.billType = :bTp and b.createdAt between :fromDate and :toDate order by b.id";
//
//            Map temMap = new HashMap();
//            temMap.put("fromDate", getFromDate());
//            temMap.put("toDate", getToDate());
//            temMap.put("bTp", BillType.CashRecieveBill);
//            cashRecieveCancel = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//            if (cashRecieveCancel == null) {
//                cashRecieveCancel = new ArrayList<Bill>();
//            }
//
//        }
//        calCashRecieveCancelTot();
//        return cashRecieveCancel;
//    }
    public BillsTotals getInstitutionAgentCancellBillsOwn() {
        if (agentCancelBill == null) {
            getAgentCancelBill().setBills(billsOwn(new CancelledBill(), BillType.AgentPaymentReceiveBill));
            //   calTot(getAgentCancelBill());
        }
        return agentCancelBill;
    }

//    public List<Bill> getInstitutionAgentCancellBills() {
//        if (agentCancelBill == null) {
//            String sql;
//
//            sql = "SELECT b FROM CancelledBill b WHERE b.retired=false and b.billType = :bTp  and b.createdAt between :fromDate and :toDate order by b.id";
//
//            Map temMap = new HashMap();
//            temMap.put("fromDate", getFromDate());
//            temMap.put("toDate", getToDate());
//            temMap.put("bTp", BillType.AgentPaymentReceiveBill);
//            agentCancelBill = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//            if (agentCancelBill == null) {
//                agentCancelBill = new ArrayList<Bill>();
//            }
//
//        }
//        calAgentCancelTot();
//        return agentCancelBill;
//    }
    private List<Bill> billsOwn(Bill billClass, BillType billType) {
        String sql = "SELECT b FROM Bill b WHERE type(b)=:bill and b.retired=false and b.billType=:btp and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", billType);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bill", billClass.getClass());

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createGRNBillItemForAsset() {
        billItems = new ArrayList<>();
        billItems = createStoreGRNBillItem(DepartmentType.Inventry);
    }

    public List<BillItem> createStoreGRNBillItem(DepartmentType dt) {
        String sql;
        Map m = new HashMap();
        List<BillItem> bs = new ArrayList<>();

        sql = " SELECT bi FROM BillItem bi WHERE "
                + " type(bi.bill)=:bill "
                + " and bi.bill.retired=false "
                + " and bi.bill.billType = :btp "
                + " and bi.item.departmentType=:dt "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " order by bi.bill.createdAt  ";

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        m.put("bill", BilledBill.class);
        m.put("btp", BillType.StoreGrnBill);
        m.put("dt", dt);

        bs = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        return bs;
    }

    public BillsTotals getInstitutionBilledBillsOwn() {
        if (billedBills == null) {
            List<Bill> tmp = billsOwn(new BilledBill(), BillType.OpdBill);

            getBilledBills().setBills(tmp);
        }
        //    calTot(getBilledBills());
        return billedBills;
    }

    public BillsTotals getInstitutionBilledBillsOwnPh() {
        if (billedBillsPh == null) {
            List<Bill> tmp = billsOwn(new BilledBill(), BillType.PharmacySale);

            getBilledBillsPh().setBills(tmp);
        }
        //    calTot(getBilledBills());
        return billedBillsPh;
    }

    public double getFinalCreditTotal(List<BillsTotals> list) {

        double tmp = 0.0;
        for (BillsTotals bt : list) {
            if (bt != null) {
                tmp += bt.getCredit();
            }
        }

        return tmp;
    }

    public double getFinalCreditCardTotal(List<BillsTotals> list) {

        double tmp = 0.0;
        for (BillsTotals bt : list) {
            if (bt != null) {
                tmp += bt.getCard();
            }
        }

        return tmp;
    }

    public void recreteModal() {
        collectingIns = null;
        billedBills = null;
        cancellededBills = null;
        refundedBills = null;
        billedBillsPh = null;
        cancellededBillsPh = null;
        refundedBillsPh = null;
        billedBillsPh2 = null;
        cancellededBillsPh2 = null;
        refundedBillsPh2 = null;
        pettyPayments = null;
        pettyPaymentsCancel = null;
        agentCancelBill = null;
        agentRecieves = null;
        cashRecieves = null;
        cashRecieveCancel = null;
        paymentBills = null;
        paymentCancelBills = null;
        inwardPayments = null;
        inwardPaymentCancel = null;
        dataTableData = null;
        institution = null;

    }

    public Institution getCollectingIns() {
        return collectingIns;
    }

    public void setCollectingIns(Institution collectingIns) {
        //recreteModal();
        this.collectingIns = collectingIns;
    }

    public double getFinalChequeTot(List<BillsTotals> list) {

        double tmp = 0.0;
        for (BillsTotals bt : list) {
            if (bt != null) {
                tmp += bt.getCheque();
            }
        }

        return tmp;
    }

    public double getFinalSlipTot(List<BillsTotals> list) {

        double tmp = 0.0;
        for (BillsTotals bt : list) {
            if (bt != null) {
                tmp += bt.getSlip();
            }
        }

        return tmp;
    }

    public double getFinalCashTotal(List<BillsTotals> list) {

        double tmp = 0.0;
        for (BillsTotals bt : list) {
            if (bt != null) {
                tmp += bt.getCash();
            }
        }

        return tmp;
    }

    public BillsTotals getBilledBills() {
        if (billedBills == null) {
            billedBills = new BillsTotals();
            //  billedBills.setBillType(BillType.OpdBill);
        }
        return billedBills;
    }

    public void setBilledBills(BillsTotals billedBills) {
        this.billedBills = billedBills;
    }

    public BillsTotals getCancellededBills() {
        if (cancellededBills == null) {
            cancellededBills = new BillsTotals();
            //   cancellededBills.setBillType(BillType.OpdBill);
        }
        return cancellededBills;
    }

    public void setCancellededBills(BillsTotals cancellededBills) {
        this.cancellededBills = cancellededBills;
    }

    public BillsTotals getRefundedBills() {
        if (refundedBills == null) {
            refundedBills = new BillsTotals();
            //    refundedBills.setBillType(BillType.OpdBill);
        }
        return refundedBills;
    }

    public void setRefundedBills(BillsTotals refundedBills) {
        this.refundedBills = refundedBills;
    }

    public BillsTotals getPaymentBills() {
        if (paymentBills == null) {
            paymentBills = new BillsTotals();
        }
        return paymentBills;
    }

    public void setPaymentBills(BillsTotals paymentBills) {
        this.paymentBills = paymentBills;
    }

    public BillsTotals getPaymentCancelBills() {
        if (paymentCancelBills == null) {
            paymentCancelBills = new BillsTotals();
        }
        return paymentCancelBills;
    }

    public void setPaymentCancelBills(BillsTotals paymentCancelBills) {
        this.paymentCancelBills = paymentCancelBills;
    }

    public BillsTotals getPettyPayments() {
        if (pettyPayments == null) {
            pettyPayments = new BillsTotals();
            //    pettyPayments.setBillType(BillType.PettyCash);
        }
        return pettyPayments;
    }

    public void setPettyPayments(BillsTotals pettyPayments) {
        this.pettyPayments = pettyPayments;
    }

    public BillsTotals getPettyPaymentsCancel() {
        if (pettyPaymentsCancel == null) {
            pettyPaymentsCancel = new BillsTotals();
            //     pettyPaymentsCancel.setBillType(BillType.PettyCash);
        }
        return pettyPaymentsCancel;
    }

    public void setPettyPaymentsCancel(BillsTotals pettyPaymentsCancel) {
        this.pettyPaymentsCancel = pettyPaymentsCancel;
    }

    public BillsTotals getCashRecieves() {
        if (cashRecieves == null) {
            cashRecieves = new BillsTotals();
            //    cashRecieves.setBillType(BillType.CashRecieveBill);
        }
        return cashRecieves;
    }

    public void setCashRecieves(BillsTotals cashRecieves) {
        this.cashRecieves = cashRecieves;
    }

    public BillsTotals getCashRecieveCancel() {
        if (cashRecieveCancel == null) {
            cashRecieveCancel = new BillsTotals();
            //   cashRecieveCancel.setBillType(BillType.CashRecieveBill);
        }
        return cashRecieveCancel;
    }

    public void setCashRecieveCancel(BillsTotals cashRecieveCancel) {
        this.cashRecieveCancel = cashRecieveCancel;
    }

    public BillsTotals getAgentRecieves() {
        if (agentRecieves == null) {
            agentRecieves = new BillsTotals();
            //  agentRecieves.setBillType(BillType.AgentPaymentReceiveBill);
        }
        return agentRecieves;
    }

    public void setAgentRecieves(BillsTotals agentRecieves) {
        this.agentRecieves = agentRecieves;
    }

    public BillsTotals getAgentCancelBill() {
        if (agentCancelBill == null) {
            agentCancelBill = new BillsTotals();
            //    agentCancelBill.setBillType(BillType.AgentPaymentReceiveBill);
        }
        return agentCancelBill;
    }

    public void setAgentCancelBill(BillsTotals agentCancelBill) {
        this.agentCancelBill = agentCancelBill;
    }

    public BillsTotals getInwardPayments() {
        if (inwardPayments == null) {
            inwardPayments = new BillsTotals();
            //   inwardPayments.setBillType(BillType.InwardPaymentBill);
        }
        return inwardPayments;
    }

    public void setInwardPayments(BillsTotals inwardPayments) {
        this.inwardPayments = inwardPayments;
    }

    public BillsTotals getInwardPaymentCancel() {
        if (inwardPaymentCancel == null) {
            inwardPaymentCancel = new BillsTotals();
            //    inwardPaymentCancel.setBillType(BillType.InwardPaymentBill);
        }
        return inwardPaymentCancel;
    }

    public void setInwardPaymentCancel(BillsTotals inwardPaymentCancel) {
        this.inwardPaymentCancel = inwardPaymentCancel;
    }

    public List<String1Value1> getCreditSlipSum() {
        List<BillsTotals> list2 = new ArrayList<>();
        list2.add(billedBills);
        list2.add(cancellededBills);
        list2.add(refundedBills);
        list2.add(billedBillsPh);
        list2.add(cancellededBillsPh);
        list2.add(refundedBillsPh);
        list2.add(paymentBills);
        list2.add(paymentCancelBills);
        list2.add(pettyPayments);
        list2.add(pettyPaymentsCancel);
        list2.add(agentRecieves);
        list2.add(agentCancelBill);
        list2.add(inwardPayments);
        list2.add(inwardPaymentCancel);
        list2.add(cashRecieves);
        list2.add(cashRecieveCancel);

        List<String1Value1> list = new ArrayList<>();
        String1Value1 tmp1 = new String1Value1();
        tmp1.setString("Final Credit Total");
        tmp1.setValue(getFinalCreditTotal(list2));
        list.add(tmp1);

        String1Value1 tmp2 = new String1Value1();
        tmp2.setString("Final Slip Total");
        tmp2.setValue(getFinalSlipTot(list2));
        list.add(tmp2);

        String1Value1 tmp3 = new String1Value1();
        tmp3.setString("Total");
        tmp3.setValue(tmp1.getValue() + tmp2.getValue());
        list.add(tmp3);

        return list;
    }

    public List<String1Value1> getCreditSlipSum2() {
        List<BillsTotals> list2 = new ArrayList<>();
        list2.add(billedBillsPh2);
        list2.add(cancellededBillsPh2);
        list2.add(refundedBillsPh2);

        List<String1Value1> list = new ArrayList<>();
        String1Value1 tmp1 = new String1Value1();
        tmp1.setString("Final Credit Total");
        tmp1.setValue(getFinalCreditTotal(list2));
        list.add(tmp1);

        String1Value1 tmp2 = new String1Value1();
        tmp2.setString("Final Slip Total");
        tmp2.setValue(getFinalSlipTot(list2));
        list.add(tmp2);

        String1Value1 tmp3 = new String1Value1();
        tmp3.setString("Total");
        tmp3.setValue(tmp1.getValue() + tmp2.getValue());
        list.add(tmp3);

        return list;
    }

    public List<String1Value1> getDataTableData() {
        List<BillsTotals> list = new ArrayList<>();
        list.add(getBilledBills());
        list.add(getCancellededBills());
        list.add(getRefundedBills());
        list.add(getBilledBillsPh());
        list.add(getCancellededBillsPh());
        list.add(getRefundedBillsPh());
        list.add(getPaymentBills());
        list.add(getPaymentCancelBills());
        list.add(getPettyPayments());
        list.add(getPettyPaymentsCancel());
        list.add(getAgentRecieves());
        list.add(getAgentCancelBill());
        list.add(getInwardPayments());
        list.add(getInwardPaymentCancel());
        list.add(getCashRecieves());
        list.add(getCashRecieveCancel());

        dataTableData = new ArrayList<>();
        String1Value1 tmp1 = new String1Value1();
        tmp1.setString("Final Credit Total");
        tmp1.setValue(getFinalCreditTotal(list));

        String1Value1 tmp2 = new String1Value1();
        tmp2.setString("Final Credit Card Total");
        tmp2.setValue(getFinalCreditCardTotal(list));

        String1Value1 tmp3 = new String1Value1();
        tmp3.setString("Final Cheque Total");
        tmp3.setValue(getFinalChequeTot(list));

        String1Value1 tmp4 = new String1Value1();
        tmp4.setString("Final Slip Total");
        tmp4.setValue(getFinalSlipTot(list));

        String1Value1 tmp5 = new String1Value1();
        tmp5.setString("Final Cash Total");
        tmp5.setValue(getFinalCashTotal(list));

        dataTableData.add(tmp1);
        dataTableData.add(tmp2);
        dataTableData.add(tmp3);
        dataTableData.add(tmp4);
        dataTableData.add(tmp5);

        return dataTableData;
    }

    public List<String1Value1> getDataTableDataByType() {
        List<BillsTotals> list = new ArrayList<>();
        if (billType == BillType.OpdBill) {
            list.add(getBilledBills());
            list.add(getCancellededBills());
            list.add(getRefundedBills());
        }
        if (billType == BillType.PharmacySale) {
            list.add(getBilledBillsPh());
            list.add(getCancellededBillsPh());
            list.add(getRefundedBillsPh());
        }

        if (billType == BillType.PaymentBill) {
            list.add(getPaymentBills());
            list.add(getPaymentCancelBills());
        }
        if (billType == BillType.PettyCash) {
            list.add(getPettyPayments());
            list.add(getPettyPaymentsCancel());
        }
        if (billType == BillType.AgentPaymentReceiveBill) {
            list.add(getAgentRecieves());
            list.add(getAgentCancelBill());
        }
        if (billType == BillType.InwardPaymentBill) {
            list.add(getInwardPayments());
            list.add(getInwardPaymentCancel());
        }
        if (billType == BillType.CashRecieveBill) {
            list.add(getCashRecieves());
            list.add(getCashRecieveCancel());
        }

        List< String1Value1> data = new ArrayList<>();
        String1Value1 tmp1 = new String1Value1();
        tmp1.setString("Final Credit Total");
        tmp1.setValue(getFinalCreditTotal(list));

        String1Value1 tmp2 = new String1Value1();
        tmp2.setString("Final Credit Card Total");
        tmp2.setValue(getFinalCreditCardTotal(list));

        String1Value1 tmp3 = new String1Value1();
        tmp3.setString("Final Cheque Total");
        tmp3.setValue(getFinalChequeTot(list));

        String1Value1 tmp4 = new String1Value1();
        tmp4.setString("Final Slip Total");
        tmp4.setValue(getFinalSlipTot(list));

        String1Value1 tmp5 = new String1Value1();
        tmp5.setString("Final Cash Total");
        tmp5.setValue(getFinalCashTotal(list));

        data.add(tmp1);
        data.add(tmp2);
        data.add(tmp3);
        data.add(tmp4);
        data.add(tmp5);

        return data;
    }

    public List<String1Value1> getCashChequeSum() {
        List<BillsTotals> list2 = new ArrayList<>();
        list2.add(billedBills);
        list2.add(cancellededBills);
        list2.add(refundedBills);
        list2.add(billedBillsPh);
        list2.add(cancellededBillsPh);
        list2.add(refundedBillsPh);
        list2.add(paymentBills);
        list2.add(paymentCancelBills);
        list2.add(pettyPayments);
        list2.add(pettyPaymentsCancel);
        list2.add(agentRecieves);
        list2.add(agentCancelBill);
        list2.add(inwardPayments);
        list2.add(inwardPaymentCancel);
        list2.add(cashRecieves);
        list2.add(cashRecieveCancel);

        List<String1Value1> list = new ArrayList<>();

        String1Value1 tmp1 = new String1Value1();
        tmp1.setString("Final Credit Card Total");
        tmp1.setValue(getFinalCreditCardTotal(list2));

        String1Value1 tmp2 = new String1Value1();
        tmp2.setString("Final Cheque Total");
        tmp2.setValue(getFinalChequeTot(list2));

        String1Value1 tmp3 = new String1Value1();
        tmp3.setString("Final Cash Total");
        tmp3.setValue(getFinalCashTotal(list2));

        String1Value1 tmp4 = new String1Value1();
        tmp4.setString("Total");
        tmp4.setValue(tmp1.getValue() + tmp2.getValue() + tmp3.getValue());

        list.add(tmp1);
        list.add(tmp2);
        list.add(tmp3);
        list.add(tmp4);
        return list;
    }

    public List<String1Value1> getCashChequeSum2() {
        List<BillsTotals> list2 = new ArrayList<>();
        list2.add(billedBillsPh2);
        list2.add(cancellededBillsPh2);
        list2.add(refundedBillsPh2);

        List<String1Value1> list = new ArrayList<>();

        String1Value1 tmp1 = new String1Value1();
        tmp1.setString("Final Credit Card Total");
        tmp1.setValue(getFinalCreditCardTotal(list2));

        String1Value1 tmp2 = new String1Value1();
        tmp2.setString("Final Cheque Total");
        tmp2.setValue(getFinalChequeTot(list2));

        String1Value1 tmp3 = new String1Value1();
        tmp3.setString("Final Cash Total");
        tmp3.setValue(getFinalCashTotal(list2));

        String1Value1 tmp4 = new String1Value1();
        tmp4.setString("Total");
        tmp4.setValue(tmp1.getValue() + tmp2.getValue() + tmp3.getValue());

        list.add(tmp1);
        list.add(tmp2);
        list.add(tmp3);
        list.add(tmp4);
        return list;
    }

    public void createOpdBillList() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();
        
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("createOpdBillList()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        
 

//        if (paymentScheme == null) {
//            JsfUtil.addErrorMessage("Please Select Payment Scheme");
//            return;
//        }
        biledBills = fetchBills(new BilledBill(), BillType.OpdBill, paymentScheme);
        cancelBills = fetchBills(new CancelledBill(), BillType.OpdBill, paymentScheme);
        refundBills = fetchBills(new RefundBill(), BillType.OpdBill, paymentScheme);
        biledBillsTotal = fetchBillsTotal(new BilledBill(), BillType.OpdBill, paymentScheme);
        cancelBillsTotal = fetchBillsTotal(new CancelledBill(), BillType.OpdBill, paymentScheme);
        refundBillsTotal = fetchBillsTotal(new RefundBill(), BillType.OpdBill, paymentScheme);

        
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
    }

    public List<Bill> fetchBills(Bill b, BillType billType, PaymentScheme ps) {
        Map m = new HashMap();
        String sql;

        sql = "SELECT b FROM Bill b WHERE "
                + " type(b)=:class "
                + " and b.retired=false "
                + " and b.billType=:btp "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate";

        if (ps != null) {
            sql += " and b.paymentScheme=:ps ";
            m.put("ps", ps);
        } else {
            sql += " and b.paymentScheme is not null ";
        }

        sql += " order by b.paymentScheme.name ";

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        m.put("btp", billType);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", b.getClass());

        return getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public double fetchBillsTotal(Bill b, BillType billType, PaymentScheme ps) {
        Map m = new HashMap();
        String sql;
        sql = "SELECT sum(b.netTotal) FROM Bill b WHERE "
                + " type(b)=:class "
                + " and b.retired=false "
                + " and b.billType=:btp "
                + " and b.institution=:ins "
                + " and b.paymentScheme=:ps "
                + " and b.createdAt between :fromDate and :toDate";

        if (ps != null) {
            sql += " and b.paymentScheme=:ps ";
            m.put("ps", ps);
        } else {
            sql += " and b.paymentScheme is not null ";
        }

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        m.put("btp", billType);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", b.getClass());

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public void createWithCreditbyDepartmentBilled() {
        Date startTime = new Date();

        if (department == null) {
            JsfUtil.addErrorMessage("Please Select Deparment");
            return;
        }
        biledBills = getLabBillsOwnBilled();
        getLabBillsOwnBilledTotals();

        
    }

    public void createWithCreditbyDepartmentBilledBillItem() {
        Date startTime = new Date();

        if (department == null) {
            JsfUtil.addErrorMessage("Please Select Deparment");
            return;
        }
        billItems = getLabBillItemsOwnBilled();
        total = 0.0;
        discount = 0.0;
        staffTotal = 0.0;
        vat = 0.0;
        netTotal = 0.0;
        for (BillItem bi : billItems) {
            for (BillFee bf : bi.getBillFees()) {
                if (bf.getFee().getFeeType() == FeeType.Staff) {
                    bi.setStaffFee(bi.getStaffFee() + bf.getFeeValue());
                    staffTotal += bf.getFeeValue();
                } else {
                    bi.setHospitalFee(bi.getHospitalFee() + bf.getFeeValue());
                    total += bf.getFeeValue();
                }
                vat += bf.getFeeVat();
                discount += bf.getFeeDiscount();
                netTotal += bf.getFeeValue();
            }
        }
//        getLabBillsOwnBilledTotals();

        
    }

    public List<Bill> getLabBillsOwnBilled() {
        List<BillType> billTypes = new ArrayList<>();
//        if (onlyOPD) {
//            billTypes = Arrays.asList(new BillType[]{BillType.OpdBill});
//        } else {
//            billTypes = Arrays.asList(new BillType[]{BillType.OpdBill, BillType.ChannelCash, BillType.ChannelPaid});
//        }

        if (radio.equals("1")) {
            billTypes = Arrays.asList(new BillType[]{BillType.OpdBill, BillType.ChannelCash, BillType.ChannelPaid, BillType.PharmacySale});
        }
        if (radio.equals("2")) {
            billTypes = Arrays.asList(new BillType[]{BillType.OpdBill});
        }
        if (radio.equals("3")) {
            billTypes = Arrays.asList(new BillType[]{BillType.ChannelPaid, BillType.ChannelCash});
        }
        if (radio.equals("4")) {
            billTypes = Arrays.asList(new BillType[]{BillType.PharmacySale});
        }

        String sql = "select f from Bill f"
                + " where f.retired=false "
                + " and f.billType in :billType "
                + " and f.createdAt between :fromDate and :toDate "
                + " and f.department=:dep ";

        Map tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", billTypes);
        // tm.put("ins", getSessionController().getInstitution());
        tm.put("dep", getDepartment());

        return getBillFacade().findByJpql(sql, tm, TemporalType.TIMESTAMP);
    }

    public List<BillItem> getLabBillItemsOwnBilled() {
        List<BillType> billTypes = Arrays.asList(new BillType[]{BillType.OpdBill});

        Map tm = new HashMap();
        String sql;

        sql = "select bi from BillItem bi join bi.bill b "
                + " where b.retired=false "
                + " and b.billType in :billType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.department=:dep ";

        if (category != null) {
            sql += " and bi.item.category=:cat ";
            tm.put("cat", category);
        }

        if (item != null) {
            sql += " and bi.item=:itm ";
            tm.put("itm", item);
        }

        if (incomeDepartment != null) {
            sql += " and bi.bill.toDepartment=:indept ";
            tm.put("indept", incomeDepartment);
        }

        sql += " order by bi.item.category.name, bi.bill.toDepartment.name";

        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", billTypes);
        // tm.put("ins", getSessionController().getInstitution());
        tm.put("dep", getDepartment());

        return getBillItemFacade().findByJpql(sql, tm, TemporalType.TIMESTAMP);
    }

    public void getLabBillsOwnBilledTotals() {
        List<BillType> billTypes = new ArrayList<>();
//        if (onlyOPD) {
//            billTypes = Arrays.asList(new BillType[]{BillType.OpdBill});
//        } else {
//            billTypes = Arrays.asList(new BillType[]{BillType.OpdBill, BillType.ChannelCash, BillType.ChannelPaid});
//        }

        if (radio.equals("1")) {
            billTypes = Arrays.asList(new BillType[]{BillType.OpdBill, BillType.ChannelCash, BillType.ChannelPaid, BillType.PharmacySale});
        }
        if (radio.equals("2")) {
            billTypes = Arrays.asList(new BillType[]{BillType.OpdBill});
        }
        if (radio.equals("3")) {
            billTypes = Arrays.asList(new BillType[]{BillType.ChannelPaid, BillType.ChannelCash});
        }
        if (radio.equals("4")) {
            billTypes = Arrays.asList(new BillType[]{BillType.PharmacySale});
        }

        String sql = "select sum(b.total),sum(b.discount),sum(b.staffFee),sum(b.vat),sum(b.netTotal) from Bill b "
                + " where b.retired=false "
                + " and b.billType in :billType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.department=:dep ";

        Map tm = new HashMap();
        tm.put("fromDate", fromDate);
        tm.put("toDate", toDate);
        tm.put("billType", billTypes);
        tm.put("dep", getDepartment());

        Object[] ob = (Object[]) getBillFacade().findAggregates(sql, tm, TemporalType.TIMESTAMP).get(0);

        if (ob != null) {
            try {
                total = (double) ob[0];
                //// // System.out.println("total = " + total);
                discount = (double) ob[1];
                staffTotal = (double) ob[2];
                vat = (double) ob[3];
                netTotal = (double) ob[4];
            } catch (Exception e) {
            }
        }

    }

    private boolean checkDoctor(Doctor doctor) {
        String sql;
        Map m = new HashMap();

        sql = " select pi from PersonInstitution pi where pi.retired=false "
                + " and pi.staff=:staff ";

        m.put("staff", doctor);

        PersonInstitution pi = personInstitutionFacade.findFirstByJpql(sql, m);
        if (pi != null) {
            return false;
        } else {
            return true;
        }

    }

    public class DocTotal {

        Doctor doctor;
        double total;
        long count;

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public Doctor getDoctor() {
            return doctor;
        }

        public void setDoctor(Doctor doctor) {
            this.doctor = doctor;
        }

        public double getTotal() {
            return total;
        }

        public void setTotal(double total) {
            this.total = total;
        }
    }

    public void setDataTableData(List<String1Value1> dataTableData) {
        this.dataTableData = dataTableData;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public BillsTotals getBilledBillsPh() {
        if (billedBillsPh == null) {
            billedBillsPh = new BillsTotals();
        }
        return billedBillsPh;
    }

    public void setBilledBillsPh(BillsTotals billedBillsPh) {
        this.billedBillsPh = billedBillsPh;
    }

    public BillsTotals getCancellededBillsPh() {
        if (cancellededBillsPh == null) {
            cancellededBillsPh = new BillsTotals();
        }
        return cancellededBillsPh;
    }

    public void setCancellededBillsPh(BillsTotals cancellededBillsPh) {
        this.cancellededBillsPh = cancellededBillsPh;
    }

    public BillsTotals getRefundedBillsPh() {
        if (refundedBillsPh == null) {
            refundedBillsPh = new BillsTotals();
        }
        return refundedBillsPh;
    }

    public void setRefundedBillsPh(BillsTotals refundedBillsPh) {
        this.refundedBillsPh = refundedBillsPh;
    }

    public BillsTotals getBilledBillsPh2() {
        if (billedBillsPh2 == null) {
            billedBillsPh2 = new BillsTotals();
        }
        return billedBillsPh2;
    }

    public void setBilledBillsPh2(BillsTotals billedBillsPh2) {
        this.billedBillsPh2 = billedBillsPh2;
    }

    public BillsTotals getCancellededBillsPh2() {
        if (cancellededBillsPh2 == null) {
            cancellededBillsPh2 = new BillsTotals();
        }
        return cancellededBillsPh2;
    }

    public void setCancellededBillsPh2(BillsTotals cancellededBillsPh2) {
        this.cancellededBillsPh2 = cancellededBillsPh2;
    }

    public BillsTotals getRefundedBillsPh2() {
        if (refundedBillsPh2 == null) {
            refundedBillsPh2 = new BillsTotals();
        }
        return refundedBillsPh2;
    }

    public void setRefundedBillsPh2(BillsTotals refundedBillsPh2) {
        this.refundedBillsPh2 = refundedBillsPh2;
    }

    public List<Bill> getBiledBills() {
        return biledBills;
    }

    public void setBiledBills(List<Bill> biledBills) {
        this.biledBills = biledBills;
    }

    public List<Bill> getCancelBills() {
        return cancelBills;
    }

    public void setCancelBills(List<Bill> cancelBills) {
        this.cancelBills = cancelBills;
    }

    public List<Bill> getRefundBills() {
        return refundBills;
    }

    public void setRefundBills(List<Bill> refundBills) {
        this.refundBills = refundBills;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public double getBiledBillsTotal() {
        return biledBillsTotal;
    }

    public void setBiledBillsTotal(double biledBillsTotal) {
        this.biledBillsTotal = biledBillsTotal;
    }

    public double getCancelBillsTotal() {
        return cancelBillsTotal;
    }

    public void setCancelBillsTotal(double cancelBillsTotal) {
        this.cancelBillsTotal = cancelBillsTotal;
    }

    public double getRefundBillsTotal() {
        return refundBillsTotal;
    }

    public void setRefundBillsTotal(double refundBillsTotal) {
        this.refundBillsTotal = refundBillsTotal;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public boolean isOnlyOPD() {
        return onlyOPD;
    }

    public void setOnlyOPD(boolean onlyOPD) {
        this.onlyOPD = onlyOPD;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public double getStaffTotal() {
        return staffTotal;
    }

    public void setStaffTotal(double staffTotal) {
        this.staffTotal = staffTotal;
    }

    public List<DocTotal> getDocTotals() {
        return docTotals;
    }

    public void setDocTotals(List<DocTotal> docTotals) {
        this.docTotals = docTotals;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public String getRadio() {
        return radio;
    }

    public void setRadio(String radio) {
        this.radio = radio;
    }

    public double getVat() {
        return vat;
    }

    public void setVat(double vat) {
        this.vat = vat;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Department getIncomeDepartment() {
        return incomeDepartment;
    }

    public void setIncomeDepartment(Department incomeDepartment) {
        this.incomeDepartment = incomeDepartment;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

}
