/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.report;

import com.divudi.bean.common.ServiceSubCategoryController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.*;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.dataStructure.BillItemWithFee;
import com.divudi.core.data.table.String1Value5;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.ItemFee;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.ServiceCategory;
import com.divudi.core.entity.ServiceSubCategory;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.lab.InvestigationCategory;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.FeeFacade;
import com.divudi.core.facade.ItemFeeFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.StaffFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.BillService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class ServiceSummery implements Serializable {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private BillService billService;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private ItemFeeFacade itemFeeFacade;
    @EJB
    private FeeFacade feeFacade;
    @EJB
    private StaffFacade staffFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PaymentFacade paymentFacade;
// </editor-fold>  

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private SessionController sessionController;
    // </editor-fold>  

    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    @Temporal(TemporalType.TIMESTAMP)
    private Date fromDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date toDate;

    private Item service;
    private Category category;
    private Staff staff;

    private boolean onlyInwardBills;
    private boolean credit = false;

    private List<BillItem> billItems;
    private List<Payment> payments;
    private List<Staff> staffs;
    private List<Bill> bills;
    private List<String1Value5> string1Value5;

    private double count;
    private double value;

    private double proFeeTotal;
    private double hosFeeTotal;
    private double hosFeeGrossValueTotal;
    private double hosFeeDisTotal;
    private double hosFeeMarginTotal;

    private double outSideFeeTotoal;
    private double outSideFeeGrossTotal;
    private double outSideFeeDiscountTotal;
    private double outSideFeeMarginTotal;

    private double reagentFeeTotal;

    private double proFeeTotalC;
    private double hosFeeTotalC;
    private double reagentFeeTotalC;
    private double outSideFeeTotoalC;

    private double proFeeTotalR;
    private double hosFeeTotalR;
    private double reagentFeeTotalR;
    private double outSideFeeTotoalR;

    private double proFeeTotalGT;
    private double hosFeeTotalGT;
    private double reagentFeeTotalGT;
    private double outSideFeeTotoalGT;

    private double vatFeeTotal;

    private double totalBill;
    private double discountBill;
    private double netTotalBill;

    private Department department;
    private Institution institution;
    private PaymentMethod paymentMethod;

    // </editor-fold>  
    public double getCount() {
        return count;
    }

    public void setCount(double count) {
        this.count = count;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    /**
     * Creates a new instance of ServiceSummery
     */
    public ServiceSummery() {
    }

    public void createStaffWelfare() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        Map<String, Object> parameters = new HashMap<>();
        StringBuilder sql = new StringBuilder();
        sql.append("select s from Staff s where s.retired=false  and s.annualWelfareQualified>0 ");

        if (staff != null) {
            sql.append(" and s.person.name =:name ");
            parameters.put("name", staff.getPerson().getName());
        }
        sql.append(" order by s.codeInterger ");

        staffs = getStaffFacade().findByJpql(sql.toString(), parameters);

    }

    public double calServiceTot(BillType billType, FeeType feeType, boolean discharged) {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(bi.feeValue) FROM BillFee bi "
                + " where bi.bill.institution=:ins "
                + " and bi.bill.billType= :bTp "
                + " and bi.fee.feeType=:ftp "
                + " and bi.billItem.item=:itm "
                + " and bi.retired=false ";

        if (discharged) {
            sql += " and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        } else {
            sql += " and bi.bill.createdAt between :fromDate and :toDate ";
        }

        if (billType != BillType.InwardBill) {
            if (credit) {
                sql += " and bi.bill.paymentMethod = :pm ";

                temMap.put("pm", PaymentMethod.Credit);
            } else {
                sql += " and ( bi.bill.paymentMethod = :pm1 "
                        + " or  bi.bill.paymentMethod = :pm2 "
                        + " or  bi.bill.paymentMethod = :pm3"
                        + " or  bi.bill.paymentMethod = :pm4) ";

                temMap.put("pm1", PaymentMethod.Cash);
                temMap.put("pm2", PaymentMethod.Card);
                temMap.put("pm3", PaymentMethod.Cheque);
                temMap.put("pm4", PaymentMethod.Slip);
            }

        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", billType);
        temMap.put("ftp", feeType);
        temMap.put("itm", getService());
        //     List<BillItem> tmp = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calServiceTotVat(BillType billType, boolean discharged) {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(bi.feeVat) FROM BillFee bi "
                + " where  bi.bill.institution=:ins"
                + " and bi.bill.billType= :bTp "
                + " and bi.billItem.item=:itm "
                + " and bi.retired=false ";

        if (discharged) {
            sql += " and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        } else {
            sql += " and bi.bill.createdAt between :fromDate and :toDate ";
        }

        if (billType != BillType.InwardBill) {
            if (credit) {
                sql += " and bi.bill.paymentMethod = :pm ";

                temMap.put("pm", PaymentMethod.Credit);
            } else {
                sql += " and ( bi.bill.paymentMethod = :pm1 "
                        + " or  bi.bill.paymentMethod = :pm2 "
                        + " or  bi.bill.paymentMethod = :pm3"
                        + " or  bi.bill.paymentMethod = :pm4) ";

                temMap.put("pm1", PaymentMethod.Cash);
                temMap.put("pm2", PaymentMethod.Card);
                temMap.put("pm3", PaymentMethod.Cheque);
                temMap.put("pm4", PaymentMethod.Slip);
            }

        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", billType);
        temMap.put("itm", getService());

        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calServiceTot(BillType billType, FeeType feeType) {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(bi.feeValue) FROM BillFee bi "
                + " where bi.bill.billType= :bTp "
                + " and bi.fee.feeType=:ftp "
                + " and bi.bill.createdAt between :fromDate and :toDate ";

        if (billType != BillType.InwardBill) {
            sql += " and ( bi.bill.paymentMethod = :pm1 "
                    + " or  bi.bill.paymentMethod = :pm2 "
                    + " or  bi.bill.paymentMethod = :pm3"
                    + " or  bi.bill.paymentMethod = :pm4) ";
            temMap.put("pm1", PaymentMethod.Cash);
            temMap.put("pm2", PaymentMethod.Card);
            temMap.put("pm3", PaymentMethod.Cheque);
            temMap.put("pm4", PaymentMethod.Slip);

        }

        if (service != null) {
            sql += " and bi.billItem.item=:itm ";
            temMap.put("itm", getService());
        }

        if (institution != null) {
            sql += " and  bi.bill.institution=:ins";
            temMap.put("ins", getInstitution());
        }

        if (department != null) {
            sql += " and  bi.bill.department=:dep ";
            temMap.put("dep", getDepartment());
        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bTp", billType);
        temMap.put("ftp", feeType);

        //     List<BillItem> tmp = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calMarginTot(BillType billType, FeeType feeType) {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(bi.feeMargin) FROM BillFee bi "
                + " where bi.bill.billType= :bTp "
                + " and bi.fee.feeType=:ftp "
                + " and bi.bill.createdAt between :fromDate and :toDate ";

        if (billType != BillType.InwardBill) {
            sql += " and ( bi.bill.paymentMethod = :pm1 "
                    + " or  bi.bill.paymentMethod = :pm2 "
                    + " or  bi.bill.paymentMethod = :pm3"
                    + " or  bi.bill.paymentMethod = :pm4) ";
            temMap.put("pm1", PaymentMethod.Cash);
            temMap.put("pm2", PaymentMethod.Card);
            temMap.put("pm3", PaymentMethod.Cheque);
            temMap.put("pm4", PaymentMethod.Slip);

        }

        if (service != null) {
            sql += " and bi.billItem.item=:itm ";
            temMap.put("itm", getService());
        }

        if (institution != null) {
            sql += " and  bi.bill.institution=:ins";
            temMap.put("ins", getInstitution());
        }

        if (department != null) {
            sql += " and  bi.bill.department=:dep ";
            temMap.put("dep", getDepartment());
        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bTp", billType);
        temMap.put("ftp", feeType);

        //     List<BillItem> tmp = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calServiceTot(BillType billType, Item item, FeeType feeType, Department department, PaymentMethod paymentMethod, boolean discharged) {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(bi.feeValue) FROM BillFee bi "
                + " where  bi.bill.institution=:ins"
                + " and  bi.bill.billType= :bTp "
                + " and bi.fee.feeType=:ftp ";

        if (item != null) {
            sql += " and bi.item=:itm ";
            temMap.put("itm", item);
        }

        if (department != null) {
            sql += " and bi.bill.department=:dep ";
            temMap.put("dep", department);
        }

        if (paymentMethod != null) {
            sql += " and bi.bill.paymentMethod=:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (discharged) {
            sql += " and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        } else {
            sql += " and bi.bill.createdAt between :fromDate and :toDate ";
        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", billType);
        temMap.put("ftp", feeType);
        //     List<BillItem> tmp = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        ////// // System.out.println("sql = " + sql);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calServiceTotNew(BillType billType, Item item, FeeType feeType, Department department, PaymentMethod paymentMethod, boolean discharged, Bill bill) {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(bi.feeValue) FROM BillFee bi "
                + " where  bi.bill.institution=:ins"
                + " and  bi.bill.billType= :bTp "
                + " and bi.fee.feeType=:ftp "
                + " and type(bi.bill)=:dtype";

        if (item != null) {
            sql += " and bi.billItem.item=:itm ";
            temMap.put("itm", item);
        }

        if (department != null) {
            sql += " and bi.billItem.item.department=:dep ";
            temMap.put("dep", department);
        }

        if (paymentMethod != null) {
            sql += " and bi.bill.paymentMethod=:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (discharged) {
            sql += " and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        } else {
            sql += " and bi.bill.createdAt between :fromDate and :toDate ";
        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", billType);
        temMap.put("ftp", feeType);
        temMap.put("dtype", bill.getClass());
        //     List<BillItem> tmp = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        ////// // System.out.println("sql = " + sql);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public List<BillFee> createBillFees(BillType billType, Item item, FeeType feeType, Department department, PaymentMethod paymentMethod, boolean discharged) {
        String sql;
        Map temMap = new HashMap();

        sql = "select bi FROM BillFee bi "
                + " where  bi.bill.institution=:ins"
                + " and  bi.bill.billType= :bTp "
                + " and bi.fee.feeType=:ftp ";

        if (item != null) {
            sql += " and bi.item=:itm ";
            temMap.put("itm", item);
        }

        if (department != null) {
            sql += " and bi.bill.department=:dep ";
            temMap.put("dep", department);
        }

        if (paymentMethod != null) {
            sql += " and bi.bill.paymentMethod=:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (discharged) {
            sql += " and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        } else {
            sql += " and bi.bill.createdAt between :fromDate and :toDate ";
        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", billType);
        temMap.put("ftp", feeType);
        //     List<BillItem> tmp = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        ////// // System.out.println("sql = " + sql);
        return getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private long calCount(Bill bill, BillType billType, boolean discharged) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi "
                + " where bi.bill.billType=:bType "
                + " and bi.item=:itm "
                + " and type(bi.bill)=:billClass "
                + " and bi.bill.toInstitution=:ins "
                + " and bi.bill.retired=false "
                + " and bi.retired=false ";

        if (billType != BillType.InwardBill) {
            if (credit) {
                sql += " and bi.bill.paymentMethod = :pm ";

                temMap.put("pm", PaymentMethod.Credit);
            } else {
                sql += " and ( bi.bill.paymentMethod = :pm1 "
                        + " or  bi.bill.paymentMethod = :pm2 "
                        + " or  bi.bill.paymentMethod = :pm3"
                        + " or  bi.bill.paymentMethod = :pm4) ";

                temMap.put("pm1", PaymentMethod.Cash);
                temMap.put("pm2", PaymentMethod.Card);
                temMap.put("pm3", PaymentMethod.Cheque);
                temMap.put("pm4", PaymentMethod.Slip);
            }

        }

        if (discharged) {
            sql += " and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        } else {
            sql += " and bi.bill.createdAt between :fromDate and :toDate ";
        }

        sql += " order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", getService());
        temMap.put("billClass", bill.getClass());
        temMap.put("bType", billType);

        temMap.put("ins", getSessionController().getInstitution());
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private long calCount(Bill bill, BillType billType) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi "
                + " where bi.bill.billType=:bType "
                + " and type(bi.bill)=:billClass "
                + " and bi.bill.createdAt between :fromDate and :toDate ";

        if (billType != BillType.InwardBill) {
            sql += " and ( bi.bill.paymentMethod = :pm1 "
                    + " or  bi.bill.paymentMethod = :pm2 "
                    + " or  bi.bill.paymentMethod = :pm3"
                    + " or  bi.bill.paymentMethod = :pm4) ";
            temMap.put("pm1", PaymentMethod.Cash);
            temMap.put("pm2", PaymentMethod.Card);
            temMap.put("pm3", PaymentMethod.Cheque);
            temMap.put("pm4", PaymentMethod.Slip);

        }

        if (service != null) {
            sql += " and bi.item=:itm ";
            temMap.put("itm", getService());
        }

        if (institution != null) {
            sql += " and  bi.bill.institution=:ins";
            temMap.put("ins", getInstitution());
        }

        if (department != null) {
            sql += " and  bi.bill.department=:dep ";
            temMap.put("dep", getDepartment());
        }

        sql += " order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billClass", bill.getClass());
        temMap.put("bType", billType);

        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void calCountTotalItem(BillType billType, boolean discharged) {
        count = 0;

        long billed = calCount(new BilledBill(), billType, discharged);
        long cancelled = calCount(new CancelledBill(), billType, discharged);
        long refunded = calCount(new RefundBill(), billType, discharged);

        count = billed - (refunded + cancelled);

    }

    public void calCountTotalItem(BillType billType) {
        count = 0;

        long billed = calCount(new BilledBill(), billType);
        long cancelled = calCount(new CancelledBill(), billType);
        long refunded = calCount(new RefundBill(), billType);

        count = billed - (refunded + cancelled);

    }

    private List<BillItem> getBillItem(BillType billType, Item item, boolean discharged) {
        String sql;
        Map temMap = new HashMap();

        sql = "select bi FROM BillItem bi "
                + " where  bi.bill.institution=:ins "
                + " and bi.bill.billType= :bTp  "
                + " and bi.item=:itm "
                + " and bi.bill.retired=false "
                + " and bi.retired=false ";

        if (billType != BillType.InwardBill) {

            if (credit) {
                sql += " and bi.bill.paymentMethod = :pm ";

                temMap.put("pm", PaymentMethod.Credit);
            } else {
                sql += " and ( bi.bill.paymentMethod = :pm1 "
                        + " or  bi.bill.paymentMethod = :pm2 "
                        + " or  bi.bill.paymentMethod = :pm3"
                        + " or  bi.bill.paymentMethod = :pm4) ";

                temMap.put("pm1", PaymentMethod.Cash);
                temMap.put("pm2", PaymentMethod.Card);
                temMap.put("pm3", PaymentMethod.Cheque);
                temMap.put("pm4", PaymentMethod.Slip);
            }

        }

        if (discharged) {
            sql += " and  bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        } else {
            sql += " and  bi.bill.createdAt between :fromDate and :toDate ";
        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", billType);
        temMap.put("itm", item);
        List<BillItem> tmp = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return tmp;

    }

    private List<BillItem> getBillItem(BillType billType, Item item) {
        String sql;
        Map temMap = new HashMap();

        sql = "select bi FROM BillItem bi "
                + " where bi.bill.billType= :bTp  "
                + " and  bi.bill.createdAt between :fromDate and :toDate ";

        if (billType != BillType.InwardBill) {
            sql += " and ( bi.bill.paymentMethod = :pm1 "
                    + " or  bi.bill.paymentMethod = :pm2 "
                    + " or  bi.bill.paymentMethod = :pm3"
                    + " or  bi.bill.paymentMethod = :pm4) ";

            temMap.put("pm1", PaymentMethod.Cash);
            temMap.put("pm2", PaymentMethod.Card);
            temMap.put("pm3", PaymentMethod.Cheque);
            temMap.put("pm4", PaymentMethod.Slip);

        }

        if (item != null) {
            sql += " and bi.item=:itm ";
            temMap.put("itm", item);
        }

        if (institution != null) {
            sql += " and  bi.bill.institution=:ins";
            temMap.put("ins", getInstitution());
        }

        if (department != null) {
            sql += " and  bi.bill.department=:dep ";
            temMap.put("dep", getDepartment());
        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        temMap.put("bTp", billType);
        List<BillItem> tmp = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return tmp;

    }

    private List<BillItem> getBillItem(List<BillType> billTypes, Item item, Department department, PaymentMethod paymentMethod, boolean discharged) {
        String sql;
        Map temMap = new HashMap();

        sql = "select bi FROM BillItem bi "
                + " where  bi.bill.institution=:ins ";

        if (item != null) {
            sql += " and bi.item=:itm ";
            temMap.put("itm", item);
        }

        if (department != null) {
            sql += " and bi.bill.toDepartment=:dep ";
            temMap.put("dep", department);
        }

        if (paymentMethod != null) {
            sql += " and bi.bill.paymentMethod=:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (discharged) {
            sql += " and  bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        } else {
            sql += " and  bi.bill.createdAt between :fromDate and :toDate ";
        }

        if (!billTypes.isEmpty()) {
            sql += " and  bi.bill.billType in :bTp  ";
            temMap.put("bTp", billTypes);
        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        List<BillItem> tmp = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return tmp;

    }

    public void findFeeTypeLessBillFees() {
        List<BillFee> billFees = new ArrayList<>();
        HashMap m = new HashMap();
        String sql = "Select f from BillFee f where "
                + " f.retired=false "
                + " and f.fee.feeType is null "
                + " and f.billItem.bill.billType in :bTp";

        BillType billType[] = {BillType.OpdBill, BillType.InwardBill};
        m.put("bTp", Arrays.asList(billType));

        billFees = getBillFeeFacade().findByJpql(sql, m);
//        billFees = getBillFeeFacade().findByJpql(sql, m, 100);
//        billFees = getBillFeeFacade().findByJpql(sql, m, 100);
//        billFees = getBillFeeFacade().findByJpql(sql, m, 100);
//        billFees = getBillFeeFacade().findByJpql(sql, m, 100);

        for (BillFee bf : billFees) {

            //// // System.out.println("bf.getBillItem().getBill().getInsId() = " + bf.getBillItem().getBill().getInsId());
            sql = "Select f from ItemFee f where f.id = " + bf.getFee().getId();
            ItemFee itemFee = itemFeeFacade.findFirstByJpql(sql);

            if (itemFee != null) {
            }
            bf.getFee().setFeeType(FeeType.OwnInstitution);
            feeFacade.edit(bf.getFee());
        }

    }

    private List<BillItem> getBillItemNew(BillType billType, Item item, Department department, PaymentMethod paymentMethod, boolean discharged, Bill bill) {
        String sql;
        Map temMap = new HashMap();

        sql = "select bi FROM BillItem bi where "
                + " bi.bill.institution=:ins "
                + " and bi.bill.billType=:bTp "
                + " and type(bi.bill)=:dtype ";

        if (item != null) {
            sql += " and bi.item=:itm ";
            temMap.put("itm", item);
        }

        if (department != null) {
            sql += " and bi.item.department=:dep ";
            temMap.put("dep", department);
        }

        if (paymentMethod != null) {
            sql += " and bi.bill.paymentMethod=:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (discharged) {
            sql += " and  bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        } else {
            sql += " and  bi.bill.createdAt between :fromDate and :toDate ";
        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", billType);
        temMap.put("dtype", bill.getClass());

        List<BillItem> tmp = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return tmp;

    }

    public void createServiceSummery() {
        Date startTime = new Date();

        serviceSummery = new ArrayList<>();
        for (BillItem i : getBillItem(BillType.OpdBill, service, false)) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution));
            bi.setOutSideFee(calFee(i, FeeType.OtherInstitution));
            bi.setVatFee(calFeeVat(i));
            serviceSummery.add(bi);
        }

        calCountTotalItem(BillType.OpdBill, false);
        proFeeTotal = calServiceTot(BillType.OpdBill, FeeType.Staff, false);
        hosFeeTotal = calServiceTot(BillType.OpdBill, FeeType.OwnInstitution, false);
        outSideFeeTotoal = calServiceTot(BillType.OpdBill, FeeType.OtherInstitution, false);
        vatFeeTotal = calServiceTotVat(BillType.OpdBill, false);

    }

    public void createInvestigationSummery() {
        Date startTime = new Date();

        serviceSummery = new ArrayList<>();
        for (BillItem i : getBillItem(BillType.OpdBill, service, false)) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution) + calFee(i, FeeType.CollectingCentre));
            bi.setVatFee(calFeeVat(i));
            serviceSummery.add(bi);
        }

        calCountTotalItem(BillType.OpdBill, false);
        proFeeTotal = calServiceTot(BillType.OpdBill, FeeType.Staff, false);
        hosFeeTotal = calServiceTot(BillType.OpdBill, FeeType.OwnInstitution, false) + calServiceTot(BillType.OpdBill, FeeType.CollectingCentre, false);
        outSideFeeTotoal = calServiceTot(BillType.OpdBill, FeeType.OtherInstitution, false);
        vatFeeTotal = calServiceTotVat(BillType.OpdBill, false);

    }

    public double getNetTotalBill() {
        return netTotalBill;
    }

    public void setNetTotalBill(double netTotalBill) {
        this.netTotalBill = netTotalBill;
    }

    public double getDiscountBill() {
        return discountBill;
    }

    public void setDiscountBill(double discountBill) {
        this.discountBill = discountBill;
    }

    public double getTotalBill() {
        return totalBill;
    }

    public void setTotalBill(double totalBill) {
        this.totalBill = totalBill;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    @Deprecated // use opdPharmacyStaffWelfarePayments
    public void opdPharmacyStaffWelfarebills() {
        String sql;
        Map m = new HashMap();
        sql = " select b from Bill b where "
                + " b.retired=false "
                + " and b.toStaff is not null "
                + " and b.createdAt between :fd and :td "
                + " and (b.billType=:bt1 or b.billType=:bt2) "
                + " order by b.id ";
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt1", BillType.PharmacySale);
        m.put("bt2", BillType.OpdBill);
        bills = billFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        calTotal(bills);
    }

    /**
     * Loads all valid staff welfare payments related to OPD and Pharmacy Retail
     * Sale bills within the specified date range. Filters are applied to
     * ensure: - Payments are not retired. - Payment method is 'Staff Welfare'.
     * - The associated bill is not retired. - The bill is for a staff member
     * (i.e., b.toStaff is not null). - The bill type is among valid OPD and
     * Pharmacy retail sale types. The method then calculates totals
     * proportionally from payments.
     */
    public void opdPharmacyStaffWelfarePayments() {
        String jpql;
        Map<String, Object> m = new HashMap<>();

        jpql = " select p "
                + " from Payment p join p.bill b "
                + " where p.retired = false "
                + " and p.paymentMethod = :pm "
                + " and b.retired = false "
                + " and b.toStaff is not null "
                + " and b.createdAt between :fd and :td "
                + " and b.billTypeAtomic in :btas "
                + " order by b.id ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("pm", PaymentMethod.Staff_Welfare);

        List<BillTypeAtomic> btas = billService.fetchBillTypeAtomicsForPharmacyRetailSaleAndOpdSaleBills();
        m.put("btas", btas);

        payments = paymentFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);

        fixDiscountsAndMarginsInRows(payments);
        calculateTotalsForPayments(payments);
    }

    public void fixDiscountsAndMarginsInRows(List<Payment> payments) {
        for (Payment ir : payments) {
            if (ir == null) {
                continue;
            }

            Bill bill = ir.getBill();
            if (bill != null && bill.getBillTypeAtomic() != null && bill.getBillTypeAtomic().getBillCategory() != null) {
                switch (bill.getBillTypeAtomic().getBillCategory()) {
                    case BILL:
                        bill.setDiscount(-Math.abs(bill.getDiscount()));
                        bill.setMargin(Math.abs(bill.getMargin()));
                        break;
                    case REFUND:
                        bill.setDiscount(Math.abs(bill.getDiscount()));
                        bill.setMargin(-Math.abs(bill.getMargin()));
                        break;
                    case CANCELLATION:
                        bill.setDiscount(Math.abs(bill.getDiscount()));
                        bill.setMargin(-Math.abs(bill.getMargin()));
                        break;
                }
            }
        }
    }

    public void calTotal(List<Bill> bills) {
        totalBill = 0.0;
        discountBill = 0.0;
        netTotalBill = 0.0;
        for (Bill bill : bills) {
            totalBill += bill.getTotal();
            discountBill += bill.getDiscount();
            netTotalBill += bill.getNetTotal();

        }
    }

    /**
     * Calculates total, discount, and net values based on individual
     * payments.Since discount is only stored at bill level, it is
     * proportionally distributed across payments according to the payment's
     * contribution to the bill's net total.
     *
     * @param payments
     */
    public void calculateTotalsForPayments(List<Payment> payments) {
        totalBill = 0.0;
        discountBill = 0.0;
        netTotalBill = 0.0;

        for (Payment payment : payments) {
            Bill bill = payment.getBill();
            if (bill == null || payment.getPaidValue() == 0.0 || bill.getNetTotal() == 0.0) { // both are double, not Double, so null checks NOT necessary
                continue; // Skip invalid entries to avoid divide-by-zero or nulls
            }

            double paidValue = payment.getPaidValue();
            double billTotal = bill.getTotal();
            double billDiscount = bill.getDiscount();
            double billNetTotal = bill.getNetTotal();

            // Calculate the proportion of this payment relative to the bill's net total
            double proportion = paidValue / billNetTotal;

            // Proportionally allocate total and discount
            totalBill += billTotal * proportion;
            discountBill += billDiscount * proportion;
            netTotalBill += paidValue; // The payment amount is already the proportional net
        }
    }

    public void createServiceSummeryLab() {
        Date startTime = new Date();

        long lng = CommonFunctions.getDayCount(getFromDate(), getToDate());

        if (Math.abs(lng) > 2) {
            JsfUtil.addErrorMessage("Date Range is too Long");
            return;
        }
        List<BillType> bts = new ArrayList<>();
        if (onlyInwardBills) {
            BillType billType[] = {BillType.InwardBill};
            bts = Arrays.asList(billType);
        } else {
            BillType billType[] = {BillType.OpdBill, BillType.InwardBill};
            bts = Arrays.asList(billType);
        }

        serviceSummery = new ArrayList<>();
        proFeeTotal = 0;
        hosFeeTotal = 0;
        outSideFeeTotoal = 0;
        reagentFeeTotal = 0;
        for (BillItem i : getBillItem(bts, service, department, paymentMethod, false)) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setReagentFee(calFee(i, FeeType.Chemical));
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution));
            bi.setOutSideFee(calFee(i, FeeType.OtherInstitution));
            bi.setTotal(calFee(i));
            ////// // System.out.println("bi = " + bi);

            proFeeTotal += bi.getProFee();
            hosFeeTotal += bi.getHospitalFee();
            outSideFeeTotoal += bi.getOutSideFee();
            reagentFeeTotal += bi.getReagentFee();

            serviceSummery.add(bi);
        }

//        calCountTotalItem(BillType.OpdBill, false);
//        ////// // System.out.println("proFeeTotal = " + proFeeTotal);
//        ////// // System.out.println("hosFeeTotal = " + hosFeeTotal);
//        ////// // System.out.println("outSideFeeTotoal = " + outSideFeeTotoal);
//        ////// // System.out.println("reagentFeeTotal = " + reagentFeeTotal);
//        proFeeTotal = calServiceTot(BillType.OpdBill, service, FeeType.Staff, department, paymentMethod, false);
//        hosFeeTotal = calServiceTot(BillType.OpdBill, service, FeeType.OwnInstitution, department, paymentMethod, false);
//        outSideFeeTotoal = calServiceTot(BillType.OpdBill, service, FeeType.OtherInstitution, department, paymentMethod, false);
//        reagentFeeTotal = calServiceTot(BillType.OpdBill, service, FeeType.Chemical, department, paymentMethod, false);
//        List<BillFee> billfees =new ArrayList<>();
//        billfees=createBillFees(BillType.OpdBill, service, FeeType.Staff, department, paymentMethod, false);
//        for (BillFee bf : billfees) {
//            proFeeTotal+=bf.getFeeValue();
//            ////// // System.out.println("bf.getFeeValue = " + bf.getFeeValue());
//            ////// // System.out.println("proFeeTotal = " + proFeeTotal);
//            ////// // System.out.println("date = " + bf.getBill().getCreatedAt());
//        }
//        billfees=createBillFees(BillType.OpdBill, service, FeeType.OwnInstitution, department, paymentMethod, false);
//        for (BillFee bf : billfees) {
//            hosFeeTotal+=bf.getFeeValue();
//            ////// // System.out.println("hosFeeTotal = " + hosFeeTotal);
//        }
//        billfees=createBillFees(BillType.OpdBill, service, FeeType.OtherInstitution, department, paymentMethod, false);
//        for (BillFee bf : billfees) {
//            outSideFeeTotoal+=bf.getFeeValue();
//            ////// // System.out.println("outSideFeeTotoal = " + outSideFeeTotoal);
//        }
//        billfees=createBillFees(BillType.OpdBill, service, FeeType.Chemical, department, paymentMethod, false);
//        for (BillFee bf : billfees) {
//            reagentFeeTotal+=bf.getFeeValue();
//            ////// // System.out.println("reagentFeeTotal = " + reagentFeeTotal);
//        }
    }

    public void createServiceSummeryLabNew() {
        Date startTime = new Date();

        long lng = CommonFunctions.getDayCount(getFromDate(), getToDate());

        if (Math.abs(lng) > 2) {
            JsfUtil.addErrorMessage("Date Range is too Long");
            return;
        }

        serviceSummeryBill = new ArrayList<>();
        serviceSummeryCancelBill = new ArrayList<>();
        serviceSummeryRefundBill = new ArrayList<>();

        createBilList(new BilledBill(), serviceSummeryBill);
        createBilList(new CancelledBill(), serviceSummeryCancelBill);
        createBilList(new RefundBill(), serviceSummeryRefundBill);

        proFeeTotal = calServiceTotNew(BillType.OpdBill, service, FeeType.Staff, department, paymentMethod, false, new BilledBill());
        hosFeeTotal = calServiceTotNew(BillType.OpdBill, service, FeeType.OwnInstitution, department, paymentMethod, false, new BilledBill());
        outSideFeeTotoal = calServiceTotNew(BillType.OpdBill, service, FeeType.OtherInstitution, department, paymentMethod, false, new BilledBill());
        reagentFeeTotal = calServiceTotNew(BillType.OpdBill, service, FeeType.Chemical, department, paymentMethod, false, new BilledBill());

        proFeeTotalC = calServiceTotNew(BillType.OpdBill, service, FeeType.Staff, department, paymentMethod, false, new CancelledBill());
        hosFeeTotalC = calServiceTotNew(BillType.OpdBill, service, FeeType.OwnInstitution, department, paymentMethod, false, new CancelledBill());
        outSideFeeTotoalC = calServiceTotNew(BillType.OpdBill, service, FeeType.OtherInstitution, department, paymentMethod, false, new CancelledBill());
        reagentFeeTotalC = calServiceTotNew(BillType.OpdBill, service, FeeType.Chemical, department, paymentMethod, false, new CancelledBill());

        proFeeTotalR = calServiceTotNew(BillType.OpdBill, service, FeeType.Staff, department, paymentMethod, false, new RefundBill());
        hosFeeTotalR = calServiceTotNew(BillType.OpdBill, service, FeeType.OwnInstitution, department, paymentMethod, false, new RefundBill());
        outSideFeeTotoalR = calServiceTotNew(BillType.OpdBill, service, FeeType.OtherInstitution, department, paymentMethod, false, new RefundBill());
        reagentFeeTotalR = calServiceTotNew(BillType.OpdBill, service, FeeType.Chemical, department, paymentMethod, false, new RefundBill());

        string1Value5 = new ArrayList<>();

        createSummaryTable(new BilledBill());
        createSummaryTable(new CancelledBill());
        createSummaryTable(new RefundBill());
        proFeeTotalGT = 0.0;
        hosFeeTotalGT = 0.0;
        outSideFeeTotoalGT = 0.0;
        reagentFeeTotalGT = 0.0;
        for (String1Value5 svItem : string1Value5) {
            proFeeTotalGT += svItem.getValue1();
            hosFeeTotalGT += svItem.getValue2();
            outSideFeeTotoalGT += svItem.getValue3();
            reagentFeeTotalGT += svItem.getValue4();
        }

    }

    public void createSummaryTable(Bill bill) {

        double pro = 0.0;
        double hos = 0.0;
        double out = 0.0;
        double reg = 0.0;

        pro = calServiceTotNew(BillType.OpdBill, service, FeeType.Staff, department, paymentMethod, false, bill);
        hos = calServiceTotNew(BillType.OpdBill, service, FeeType.OwnInstitution, department, paymentMethod, false, bill);
        out = calServiceTotNew(BillType.OpdBill, service, FeeType.OtherInstitution, department, paymentMethod, false, bill);
        reg = calServiceTotNew(BillType.OpdBill, service, FeeType.Chemical, department, paymentMethod, false, bill);

        String1Value5 str = new String1Value5();

        str.setString(bill.getBillClassType().name());
        str.setValue1(pro);
        str.setValue2(hos);
        str.setValue3(out);
        str.setValue4(reg);

        string1Value5.add(str);
    }

    public void createBilList(Bill bill, List<BillItemWithFee> billItemWithFees) {
        for (BillItem i : getBillItemNew(BillType.OpdBill, service, department, paymentMethod, false, bill)) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setReagentFee(calFee(i, FeeType.Chemical));
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution));
            ////// // System.out.println("bi = " + bi);
            billItemWithFees.add(bi);
        }
    }

    public void createServiceSummeryInwardAdded() {
        Date startTime = new Date();

        serviceSummery = new ArrayList<>();
        for (BillItem i : getBillItem(BillType.InwardBill, service, false)) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution));
            serviceSummery.add(bi);
        }

        calCountTotalItem(BillType.InwardBill, false);
        proFeeTotal = calServiceTot(BillType.InwardBill, FeeType.Staff, false);
        hosFeeTotal = calServiceTot(BillType.InwardBill, FeeType.OwnInstitution, false);
        outSideFeeTotoal = calServiceTot(BillType.InwardBill, FeeType.OtherInstitution, false);

    }

    public void createServiceSummeryInwardAddedDate() {
        Date startTime = new Date();

        serviceSummery = new ArrayList<>();
        for (BillItem i : getBillItem(BillType.InwardBill, service)) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution));
            bi.setHospitalFeeMargin(calFeeMargin(i, FeeType.OwnInstitution));
            bi.setOutSideFee(calFee(i, FeeType.OtherInstitution));
            bi.setReagentFee(calFee(i, FeeType.Chemical));
            serviceSummery.add(bi);
        }

        calCountTotalItem(BillType.InwardBill);
        proFeeTotal = calServiceTot(BillType.InwardBill, FeeType.Staff);
        hosFeeTotal = calServiceTot(BillType.InwardBill, FeeType.OwnInstitution);
        outSideFeeTotoal = calServiceTot(BillType.InwardBill, FeeType.OtherInstitution);
        reagentFeeTotal = calServiceTot(BillType.InwardBill, FeeType.Chemical);
        hosFeeMarginTotal = calMarginTot(BillType.InwardBill, FeeType.OwnInstitution);

    }

    public void createServiceSummeryInwardDischarged() {
        Date startTime = new Date();

        serviceSummery = new ArrayList<>();
        for (BillItem i : getBillItem(BillType.InwardBill, service, true)) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution));
            serviceSummery.add(bi);
        }

        calCountTotalItem(BillType.InwardBill, true);
        proFeeTotal = calServiceTot(BillType.InwardBill, FeeType.Staff, true);
        hosFeeTotal = calServiceTot(BillType.InwardBill, FeeType.OwnInstitution, true);
        outSideFeeTotoal = calServiceTot(BillType.InwardBill, FeeType.OtherInstitution, true);

    }

    List<BillItemWithFee> serviceSummery;

    List<BillItemWithFee> serviceSummeryBill;
    List<BillItemWithFee> serviceSummeryCancelBill;
    List<BillItemWithFee> serviceSummeryRefundBill;

    public List<BillItemWithFee> getServiceSummery() {
        return serviceSummery;
    }

    public void setServiceSummery(List<BillItemWithFee> serviceSummery) {
        this.serviceSummery = serviceSummery;
    }

    private double calFee(BillItem bi, FeeType feeType) {
        HashMap hm = new HashMap();
        String sql = "Select sum(f.feeValue) from "
                + " BillFee f where "
                + " f.retired=false "
                + " and f.billItem=:b and "
                + " f.fee.feeType=:ftp";
        hm.put("b", bi);
        hm.put("ftp", feeType);

        return getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    private double calFee(BillItem bi) {
        HashMap hm = new HashMap();
        String sql = "Select sum(f.feeValue) from "
                + " BillFee f where "
                + " f.retired=false "
                + " and f.billItem=:b ";
        hm.put("b", bi);

        return getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    private double calFeeVat(BillItem bi) {
        HashMap hm = new HashMap();
        String sql = "Select sum(f.feeVat) from "
                + " BillFee f where "
                + " f.retired=false "
                + " and f.billItem=:b ";
        hm.put("b", bi);

        return getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    private String fetchStaffs(BillItem bi, FeeType feeType) {
        String name = "";
        HashMap hm = new HashMap();
        String sql = "Select f from "
                + " BillFee f where "
                + " f.retired=false "
                + " and f.billItem=:b and "
                + " f.fee.feeType=:ftp";
        hm.put("b", bi);
        hm.put("ftp", feeType);

        try {
            for (BillFee bf : (List<BillFee>) getBillFeeFacade().findByJpql(sql, hm)) {
                if ("".equalsIgnoreCase(name)) {

                } else {
                    name += " ," + bf.getStaff().getPerson().getName();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return name;

    }

    private double calFeeMargin(BillItem bi, FeeType feeType) {
        HashMap hm = new HashMap();
        String sql = "Select sum(f.feeMargin) from "
                + " BillFee f where "
                + " f.retired=false "
                + " and f.billItem=:b and "
                + " f.fee.feeType=:ftp";
        hm.put("b", bi);
        hm.put("ftp", feeType);

        return getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    private double calFeeFeeValue(BillItem bi, FeeType feeType) {
        HashMap hm = new HashMap();
        String sql = "Select sum(f.feeValue) from "
                + " BillFee f where "
                + " f.retired=false "
                + " and f.billItem=:b and "
                + " f.fee.feeType=:ftp ";
        hm.put("b", bi);
        hm.put("ftp", feeType);

        return getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    private double calFeeFeeGrossValue(BillItem bi, FeeType feeType) {
        HashMap hm = new HashMap();
        String sql = "Select sum(f.feeGrossValue) from "
                + " BillFee f where "
                + " f.retired=false "
                + " and f.billItem=:b and "
                + " f.fee.feeType=:ftp ";
        hm.put("b", bi);
        hm.put("ftp", feeType);

        return getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    private double calFeeFeeGrossValueFeeDiscount(BillItem bi, FeeType feeType) {
        HashMap hm = new HashMap();
        String sql = "Select sum(f.feeDiscount) from "
                + " BillFee f where "
                + " f.retired=false "
                + " and f.billItem=:b and "
                + " f.fee.feeType=:ftp ";
        hm.put("b", bi);
        hm.put("ftp", feeType);

        return getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    private double calFeeFeeGrossValueFeeMargin(BillItem bi, FeeType feeType) {
        HashMap hm = new HashMap();
        String sql = "Select sum(f.feeMargin) from "
                + " BillFee f where "
                + " f.retired=false "
                + " and f.billItem=:b and "
                + " f.fee.feeType=:ftp ";
        hm.put("b", bi);
        hm.put("ftp", feeType);

        return getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    private double calFeeOutSideFeeGrossValueFeeMargin(BillItem bi, FeeType feeType, FeeType feeType2) {
        HashMap hm = new HashMap();
        String sql = "Select sum(f.feeMargin) from "
                + " BillFee f where "
                + " f.retired=false "
                + " and f.billItem=:b and "
                + " (f.fee.feeType=:ftp "
                + "or f.fee.feeType=:ftp2 )";
        hm.put("b", bi);
        hm.put("ftp", feeType);
        hm.put("ftp2", feeType2);

        return getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    List<BillItemWithFee> billItemWithFees;

    public List<BillItemWithFee> getBillItemWithFees() {
        return billItemWithFees;
    }

    public void setBillItemWithFees(List<BillItemWithFee> billItemWithFees) {
        this.billItemWithFees = billItemWithFees;
    }

    public void createServiceCategorySummery() {
        Date startTime = new Date();

        if (getCategory() == null) {
            return;
        }
        if (getToDate() == null || getFromDate() == null) {
            return;
        }

        billItemWithFees = new ArrayList<>();

        List<BillItem> list = calBillItems(BillType.OpdBill, false);

        for (BillItem i : list) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution));
            bi.setStaffsNames(fetchStaffs(i, FeeType.Staff));
            bi.setOutSideFee(calFee(i, FeeType.OtherInstitution));
            bi.setVatFee(calFeeVat(i));
            bi.setStaffFee(calFee(i, FeeType.Staff));
            billItemWithFees.add(bi);
        }
        outSideFeeTotoal = calServiceTot(BillType.OpdBill, FeeType.OtherInstitution, false);
        calCountTotalCategory(BillType.OpdBill, false);
        calServiceTot1(BillType.OpdBill, false);

    }

    public void createInvestigationCategorySummery() {
        Date startTime = new Date();

        if (getCategory() == null) {
            return;
        }
        if (getToDate() == null || getFromDate() == null) {
            return;
        }
        Map temMap = new HashMap();
        String jpql = "select bf "
                + " from BillFee bf join bf.billItem bi join bi.item i join i.category c where "
                + " type(c)=:cat "
                + " and bi.bill.billType= :bTp  "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " order by bf.department.name ";

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("cat", InvestigationCategory.class);

        List<BillFee> bfs = getBillFeeFacade().findByJpql(jpql, temMap, TemporalType.TIMESTAMP);
        for (BillFee bf : bfs) {
            if (!bf.getDepartment().equals(bf.getBill().getToDepartment())) {
            }
        }

        billItemWithFees = new ArrayList<>();

        List<BillItem> list = calBillItems(BillType.OpdBill, false);

        for (BillItem i : list) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution) + calFee(i, FeeType.CollectingCentre));
            bi.setStaffsNames(fetchStaffs(i, FeeType.Staff));
            bi.setVatFee(calFeeVat(i));
            billItemWithFees.add(bi);
        }

        calCountTotalCategory(BillType.OpdBill, false);
        calServiceTot1(BillType.OpdBill, false);

    }

    private void calServiceTot1(BillType billType, boolean discharged) {

        if (getCategory() instanceof ServiceSubCategory || getCategory() instanceof InvestigationCategory) {
            value = getServiceValue(getCategory(), billType, discharged);
        }

        if (getCategory() instanceof ServiceCategory) {
            getServiceSubCategoryController().setParentCategory(getCategory());
            List<ServiceSubCategory> subCategorys = getServiceSubCategoryController().getItems();
            if (subCategorys.isEmpty()) {
                value = getServiceValue(getCategory(), billType, discharged);
            } else {
                value = 0;
                for (ServiceSubCategory ssc : subCategorys) {
                    value += getServiceValue(ssc, billType, discharged);
                }
            }
        }

    }

    public void createServiceCategorySummeryInwardAdded() {
        Date startTime = new Date();

        if (getCategory() == null) {
            return;
        }
        if (getToDate() == null || getFromDate() == null) {
            return;
        }

        billItemWithFees = new ArrayList<>();

        List<BillItem> list = calBillItems(BillType.InwardBill, false);

        for (BillItem i : list) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution));
            billItemWithFees.add(bi);
        }

        calCountTotalCategory(BillType.InwardBill, false);
        calServiceTot(BillType.InwardBill, false);

    }

    public void createServiceCategorySummeryInwardDischarged() {
        if (getCategory() == null) {
            return;
        }
        if (getToDate() == null || getFromDate() == null) {
            return;
        }

        billItemWithFees = new ArrayList<>();

        List<BillItem> list = calBillItems(BillType.InwardBill, true);

        for (BillItem i : list) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution));
            bi.setOutSideFee(calFee(i, FeeType.OtherInstitution));
            billItemWithFees.add(bi);
        }

        calCountTotalCategory(BillType.InwardBill, true);
        calServiceTot(BillType.InwardBill, true);

    }

    public void createServiceCategorySummeryInwardDischargedDetail() {
        Date startTime = new Date();

        if (getCategory() == null) {
            return;
        }
        if (getToDate() == null || getFromDate() == null) {
            return;
        }

        billItemWithFees = new ArrayList<>();
        hosFeeDisTotal = 0;
        hosFeeGrossValueTotal = 0;
        hosFeeMarginTotal = 0;
        hosFeeTotal = 0;
        proFeeTotal = 0;
        outSideFeeDiscountTotal = 0;
        outSideFeeGrossTotal = 0;
        outSideFeeMarginTotal = 0;
        outSideFeeTotoal = 0;

        List<BillItem> list = calBillItems(BillType.InwardBill, true);

        for (BillItem i : list) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFeeFeeValue(i, FeeType.OwnInstitution));
            bi.setHospitalFeeGross(calFeeFeeGrossValue(i, FeeType.OwnInstitution));
            bi.setHospitalFeeDiscount(calFeeFeeGrossValueFeeDiscount(i, FeeType.OwnInstitution));
            bi.setHospitalFeeMargin(calFeeFeeGrossValueFeeMargin(i, FeeType.OwnInstitution));
            bi.setOutSideFee(calFeeFeeValue(i, FeeType.OtherInstitution));
            bi.setOutSideFeeGross(calFeeFeeGrossValue(i, FeeType.OtherInstitution));
            bi.setOutSideFeeDiscount(calFeeFeeGrossValueFeeDiscount(i, FeeType.OtherInstitution));
            bi.setOutSideFeeMargin(calFeeFeeGrossValueFeeMargin(i, FeeType.OtherInstitution));
            hosFeeDisTotal += bi.getHospitalFeeDiscount();
            hosFeeGrossValueTotal += bi.getHospitalFeeGross();
            hosFeeMarginTotal += bi.getHospitalFeeMargin();
            hosFeeTotal += bi.getHospitalFee();
            proFeeTotal += bi.getProFee();

            outSideFeeGrossTotal += bi.getOutSideFeeGross();
            outSideFeeDiscountTotal += bi.getOutSideFeeDiscount();
            outSideFeeMarginTotal += bi.getOutSideFeeMargin();
            outSideFeeTotoal += bi.getOutSideFee();

            //bi.setOutSideFee(calFee(i, FeeType.OtherInstitution));
            billItemWithFees.add(bi);
        }

        calCountTotalCategory(BillType.InwardBill, true);
        //calServiceTot(BillType.InwardBill, true);

    }

    public void createServiceInwardCategorySummery() {
        Map m = new HashMap();
        String sql = "SELECT bi FROM BillItem bi WHERE bi.retired=false and bi.createdAt between :fd and :td and bi.bill.billType=:bt";
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.InwardBill);

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.DATE);

    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    @Inject
    ServiceSubCategoryController serviceSubCategoryController;

    public ServiceSubCategoryController getServiceSubCategoryController() {
        return serviceSubCategoryController;
    }

    public void setServiceSubCategoryController(ServiceSubCategoryController serviceSubCategoryController) {
        this.serviceSubCategoryController = serviceSubCategoryController;
    }

    private List<BillItem> calBillItems(BillType billType, boolean discharged) {
        if (getCategory() instanceof ServiceSubCategory || getCategory() instanceof InvestigationCategory) {
            return getBillItemByCategory(category, billType, discharged);
        }

        if (getCategory() instanceof ServiceCategory) {
            getServiceSubCategoryController().setParentCategory(getCategory());
            List<ServiceSubCategory> subCategorys = getServiceSubCategoryController().getItems();
            if (subCategorys.isEmpty()) {
                return getBillItemByCategory(getCategory(), billType, discharged);
            } else {
                Set<BillItem> setBillItem = new HashSet<>();
                for (ServiceSubCategory ssc : subCategorys) {
                    setBillItem.addAll(getBillItemByCategory(ssc, billType, discharged));
                }
                List<BillItem> tmpBillItems = new ArrayList<>();
                tmpBillItems.addAll(setBillItem);
                return tmpBillItems;
            }
        }

        return null;
    }

    private List<BillItem> getBillItemByCategory(Category cat, BillType billType, boolean discharged) {
        String sql;
        Map temMap = new HashMap();

        sql = "select bi FROM BillItem bi "
                + " where  bi.bill.institution=:ins "
                + " and  bi.bill.billType= :bTp  "
                + " and bi.item.category=:cat "
                + " and bi.retired=false ";

        if (billType != BillType.InwardBill) {
            if (credit) {
                sql += " and bi.bill.paymentMethod = :pm ";

                temMap.put("pm", PaymentMethod.Credit);
            } else {
                sql += " and ( bi.bill.paymentMethod = :pm1 "
                        + " or  bi.bill.paymentMethod = :pm2 "
                        + " or  bi.bill.paymentMethod = :pm3"
                        + " or  bi.bill.paymentMethod = :pm4) ";

                temMap.put("pm1", PaymentMethod.Cash);
                temMap.put("pm2", PaymentMethod.Card);
                temMap.put("pm3", PaymentMethod.Cheque);
                temMap.put("pm4", PaymentMethod.Slip);
            }

        }

        if (discharged) {
            sql += " and  bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        } else {
            sql += " and  bi.bill.createdAt between :fromDate and :toDate ";
        }

        sql += " order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", billType);

        temMap.put("cat", cat);
        List<BillItem> tmp = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return tmp;

    }

    public void calCountTotalCategory(BillType billType, boolean discharged) {
        long countTotal = 0;
        long billed = 0l;
        long cancelled = 0l;
        long refunded = 0l;

        if (getCategory() instanceof ServiceSubCategory || getCategory() instanceof InvestigationCategory) {
            billed = getCount(new BilledBill(), billType, getCategory(), discharged);
            cancelled = getCount(new CancelledBill(), billType, getCategory(), discharged);
            refunded = getCount(new RefundBill(), billType, getCategory(), discharged);
        }

        if (getCategory() instanceof ServiceCategory) {
            getServiceSubCategoryController().setParentCategory(getCategory());
            List<ServiceSubCategory> subCategorys = getServiceSubCategoryController().getItems();
            if (subCategorys.isEmpty()) {
                billed = getCount(new BilledBill(), billType, getCategory(), discharged);
                cancelled = getCount(new CancelledBill(), billType, getCategory(), discharged);
                refunded = getCount(new RefundBill(), billType, getCategory(), discharged);
            } else {
                billed = 0l;
                cancelled = 0l;
                refunded = 0l;
                for (ServiceSubCategory ssc : subCategorys) {
                    billed += getCount(new BilledBill(), billType, ssc, discharged);
                    cancelled += getCount(new CancelledBill(), billType, ssc, discharged);
                    refunded += getCount(new RefundBill(), billType, ssc, discharged);
                }
            }
        }

        countTotal = billed - (refunded + cancelled);

        count = countTotal;
    }

    private long getCount(Bill bill, BillType billType, Category cat, boolean discharged) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi "
                + " where bi.bill.billType=:bType "
                + " and bi.item.category=:cat "
                + " and type(bi.bill)=:billClass "
                + " and bi.bill.toInstitution=:ins "
                + " and bi.retired=false ";

        if (billType != BillType.InwardBill) {
            if (credit) {
                sql += " and bi.bill.paymentMethod = :pm ";

                temMap.put("pm", PaymentMethod.Credit);
            } else {
                sql += " and ( bi.bill.paymentMethod = :pm1 "
                        + " or  bi.bill.paymentMethod = :pm2 "
                        + " or  bi.bill.paymentMethod = :pm3"
                        + " or  bi.bill.paymentMethod = :pm4) ";

                temMap.put("pm1", PaymentMethod.Cash);
                temMap.put("pm2", PaymentMethod.Card);
                temMap.put("pm3", PaymentMethod.Cheque);
                temMap.put("pm4", PaymentMethod.Slip);
            }

        }

        if (discharged) {
            sql += " and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate";
        } else {
            sql += " and bi.bill.createdAt between :fromDate and :toDate";
        }

        sql += " order by bi.item.name";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("cat", cat);
        temMap.put("billClass", bill.getClass());
        temMap.put("bType", billType);
        temMap.put("ins", getSessionController().getInstitution());
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double getServiceValue(Category cat, BillType billType, FeeType feeType, boolean discharged) {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(bi.feeValue) FROM BillFee bi "
                + " where  bi.bill.institution=:ins "
                + " and  bi.bill.billType= :bTp "
                + " and bi.fee.feeType=:ftp "
                + " and bi.billItem.item.category=:cat "
                + " and bi.retired=false ";

        if (billType != BillType.InwardBill) {
            if (credit) {
                sql += " and bi.bill.paymentMethod = :pm ";

                temMap.put("pm", PaymentMethod.Credit);
            } else {
                sql += " and ( bi.bill.paymentMethod = :pm1 "
                        + " or  bi.bill.paymentMethod = :pm2 "
                        + " or  bi.bill.paymentMethod = :pm3"
                        + " or  bi.bill.paymentMethod = :pm4) ";

                temMap.put("pm1", PaymentMethod.Cash);
                temMap.put("pm2", PaymentMethod.Card);
                temMap.put("pm3", PaymentMethod.Cheque);
                temMap.put("pm4", PaymentMethod.Slip);
            }

        }

        if (discharged) {
            sql += " and  bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate";
        } else {
            sql += " and  bi.bill.createdAt between :fromDate and :toDate";
        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", billType);
        temMap.put("ftp", feeType);
        temMap.put("cat", cat);
        //     List<BillItem> tmp = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double getServiceValue(Category cat, BillType billType, boolean discharged) {
        double value = getServiceValue(cat, billType, FeeType.OwnInstitution, discharged);
        value += getServiceValue(cat, billType, FeeType.OtherInstitution, discharged);
        value += getServiceValue(cat, billType, FeeType.CollectingCentre, discharged);
        return value;
    }

    private void calServiceTot(BillType billType, boolean discharged) {

        if (getCategory() instanceof ServiceSubCategory) {
            proFeeTotal = getServiceValue(getCategory(), billType, FeeType.Staff, discharged);
            hosFeeTotal = getServiceValue(getCategory(), billType, FeeType.OwnInstitution, discharged);
            outSideFeeTotoal = getServiceValue(getCategory(), billType, FeeType.OtherInstitution, discharged);
        }

        if (getCategory() instanceof ServiceCategory) {
            getServiceSubCategoryController().setParentCategory(getCategory());
            List<ServiceSubCategory> subCategorys = getServiceSubCategoryController().getItems();
            if (subCategorys.isEmpty()) {
                proFeeTotal = getServiceValue(getCategory(), billType, FeeType.Staff, discharged);
                hosFeeTotal = getServiceValue(getCategory(), billType, FeeType.OwnInstitution, discharged);
                outSideFeeTotoal = getServiceValue(getCategory(), billType, FeeType.OtherInstitution, discharged);
            } else {
                proFeeTotal = 0;
                hosFeeTotal = 0;
                outSideFeeTotoal = 0;
                for (ServiceSubCategory ssc : subCategorys) {
                    proFeeTotal += getServiceValue(ssc, billType, FeeType.Staff, discharged);
                    hosFeeTotal += getServiceValue(ssc, billType, FeeType.OwnInstitution, discharged);
                    outSideFeeTotoal += getServiceValue(ssc, billType, FeeType.OtherInstitution, discharged);
                }
            }
        }

    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Item getService() {
        return service;
    }

    public void setService(Item service) {
        this.service = service;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public double getProFeeTotal() {
        return proFeeTotal;
    }

    public void setProFeeTotal(double proFeeTotal) {
        this.proFeeTotal = proFeeTotal;
    }

    public double getHosFeeGrossValueTotal() {
        return hosFeeGrossValueTotal;
    }

    public void setHosFeeGrossValueTotal(double hosFeeGrossValueTotal) {
        this.hosFeeGrossValueTotal = hosFeeGrossValueTotal;
    }

    public double getHosFeeDisTotal() {
        return hosFeeDisTotal;
    }

    public void setHosFeeDisTotal(double hosFeeDisTotal) {
        this.hosFeeDisTotal = hosFeeDisTotal;
    }

    public double getHosFeeMarginTotal() {
        return hosFeeMarginTotal;
    }

    public void setHosFeeMarginTotal(double hosFeeMarginTotal) {
        this.hosFeeMarginTotal = hosFeeMarginTotal;
    }

    public double getHosFeeTotal() {
        return hosFeeTotal;
    }

    public void setHosFeeTotal(double hosFeeTotal) {
        this.hosFeeTotal = hosFeeTotal;
    }

    public double getOutSideFeeTotoal() {
        return outSideFeeTotoal;
    }

    public void setOutSideFeeTotoal(double outSideFeeTotoal) {
        this.outSideFeeTotoal = outSideFeeTotoal;
    }

    public double getOutSideFeeGrossTotal() {
        return outSideFeeGrossTotal;
    }

    public void setOutSideFeeGrossTotal(double outSideFeeGrossTotal) {
        this.outSideFeeGrossTotal = outSideFeeGrossTotal;
    }

    public double getOutSideFeeDiscountTotal() {
        return outSideFeeDiscountTotal;
    }

    public void setOutSideFeeDiscountTotal(double outSideFeeDiscountTotal) {
        this.outSideFeeDiscountTotal = outSideFeeDiscountTotal;
    }

    public double getOutSideFeeMarginTotal() {
        return outSideFeeMarginTotal;
    }

    public void setOutSideFeeMarginTotal(double outSideFeeMarginTotal) {
        this.outSideFeeMarginTotal = outSideFeeMarginTotal;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getReagentFeeTotal() {
        return reagentFeeTotal;
    }

    public void setReagentFeeTotal(double reagentFeeTotal) {
        this.reagentFeeTotal = reagentFeeTotal;
    }

    public List<BillItemWithFee> getServiceSummeryBill() {
        return serviceSummeryBill;
    }

    public void setServiceSummeryBill(List<BillItemWithFee> serviceSummeryBill) {
        this.serviceSummeryBill = serviceSummeryBill;
    }

    public List<BillItemWithFee> getServiceSummeryCancelBill() {
        return serviceSummeryCancelBill;
    }

    public void setServiceSummeryCancelBill(List<BillItemWithFee> serviceSummeryCancelBill) {
        this.serviceSummeryCancelBill = serviceSummeryCancelBill;
    }

    public List<BillItemWithFee> getServiceSummeryRefundBill() {
        return serviceSummeryRefundBill;
    }

    public void setServiceSummeryRefundBill(List<BillItemWithFee> serviceSummeryRefundBill) {
        this.serviceSummeryRefundBill = serviceSummeryRefundBill;
    }

    public double getProFeeTotalC() {
        return proFeeTotalC;
    }

    public void setProFeeTotalC(double proFeeTotalC) {
        this.proFeeTotalC = proFeeTotalC;
    }

    public double getHosFeeTotalC() {
        return hosFeeTotalC;
    }

    public void setHosFeeTotalC(double hosFeeTotalC) {
        this.hosFeeTotalC = hosFeeTotalC;
    }

    public double getReagentFeeTotalC() {
        return reagentFeeTotalC;
    }

    public void setReagentFeeTotalC(double reagentFeeTotalC) {
        this.reagentFeeTotalC = reagentFeeTotalC;
    }

    public double getOutSideFeeTotoalC() {
        return outSideFeeTotoalC;
    }

    public void setOutSideFeeTotoalC(double outSideFeeTotoalC) {
        this.outSideFeeTotoalC = outSideFeeTotoalC;
    }

    public double getProFeeTotalR() {
        return proFeeTotalR;
    }

    public void setProFeeTotalR(double proFeeTotalR) {
        this.proFeeTotalR = proFeeTotalR;
    }

    public double getHosFeeTotalR() {
        return hosFeeTotalR;
    }

    public void setHosFeeTotalR(double hosFeeTotalR) {
        this.hosFeeTotalR = hosFeeTotalR;
    }

    public double getReagentFeeTotalR() {
        return reagentFeeTotalR;
    }

    public void setReagentFeeTotalR(double reagentFeeTotalR) {
        this.reagentFeeTotalR = reagentFeeTotalR;
    }

    public double getOutSideFeeTotoalR() {
        return outSideFeeTotoalR;
    }

    public void setOutSideFeeTotoalR(double outSideFeeTotoalR) {
        this.outSideFeeTotoalR = outSideFeeTotoalR;
    }

    public List<String1Value5> getString1Value5() {
        return string1Value5;
    }

    public void setString1Value5(List<String1Value5> string1Value5) {
        this.string1Value5 = string1Value5;
    }

    public double getProFeeTotalGT() {
        return proFeeTotalGT;
    }

    public void setProFeeTotalGT(double proFeeTotalGT) {
        this.proFeeTotalGT = proFeeTotalGT;
    }

    public double getHosFeeTotalGT() {
        return hosFeeTotalGT;
    }

    public void setHosFeeTotalGT(double hosFeeTotalGT) {
        this.hosFeeTotalGT = hosFeeTotalGT;
    }

    public double getReagentFeeTotalGT() {
        return reagentFeeTotalGT;
    }

    public void setReagentFeeTotalGT(double reagentFeeTotalGT) {
        this.reagentFeeTotalGT = reagentFeeTotalGT;
    }

    public double getOutSideFeeTotoalGT() {
        return outSideFeeTotoalGT;
    }

    public void setOutSideFeeTotoalGT(double outSideFeeTotoalGT) {
        this.outSideFeeTotoalGT = outSideFeeTotoalGT;
    }

    public List<Staff> getStaffs() {
        return staffs;
    }

    public void setStaffs(List<Staff> staffs) {
        this.staffs = staffs;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public boolean isOnlyInwardBills() {
        return onlyInwardBills;
    }

    public void setOnlyInwardBills(boolean onlyInwardBills) {
        this.onlyInwardBills = onlyInwardBills;
    }

    public double getVatFeeTotal() {
        return vatFeeTotal;
    }

    public void setVatFeeTotal(double vatFeeTotal) {
        this.vatFeeTotal = vatFeeTotal;
    }

    public boolean isCredit() {
        return credit;
    }

    public void setCredit(boolean credit) {
        this.credit = credit;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

}
