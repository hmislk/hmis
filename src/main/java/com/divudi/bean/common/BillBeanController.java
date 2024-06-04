
/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.collectingCentre.CollectingCentreBillController;
import com.divudi.bean.inward.InwardBeanController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.OpdBillingStrategy;
import com.divudi.data.OpdTokenNumberGenerationStrategy;
import com.divudi.data.PaymentMethod;
import static com.divudi.data.PaymentMethod.Card;
import static com.divudi.data.PaymentMethod.Cheque;
import static com.divudi.data.PaymentMethod.Credit;
import static com.divudi.data.PaymentMethod.Slip;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.data.inward.SurgeryBillType;
import com.divudi.ejb.ServiceSessionBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import com.divudi.entity.BillEntry;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillFeePayment;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Fee;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.PackageFee;
import com.divudi.entity.Packege;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Payment;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.PreBill;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.entity.inward.EncounterComponent;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.membership.AllowedPaymentMethod;
import com.divudi.entity.membership.MembershipScheme;
import com.divudi.facade.AllowedPaymentMethodFacade;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillFeePaymentFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.CategoryFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.EncounterComponentFacade;
import com.divudi.facade.FeeFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.PackageFeeFacade;
import com.divudi.facade.PackegeFacade;
import com.divudi.facade.PatientInvestigationFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
import javax.persistence.TemporalType;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class BillBeanController implements Serializable {

    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private PackegeFacade packegeFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillComponentFacade billComponentFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillSessionFacade billSessionFacade;
    @EJB
    private ItemFeeFacade itemFeeFacade;
    @EJB
    DepartmentFacade departmentFacade;
    @EJB
    private FeeFacade feeFacade;
    @EJB
    PackageFeeFacade packageFeeFacade;
    @EJB
    ServiceSessionBean serviceSessionBean;
    @EJB
    BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    CategoryFacade categoryFacade;
    @EJB
    AllowedPaymentMethodFacade allowedPaymentMethodFacade;
    @Inject
    DepartmentController departmentController;
    @Inject
    CollectingCentreBillController collectingCentreBillController;

    public boolean checkAllowedPaymentMethod(PaymentScheme paymentScheme, PaymentMethod paymentMethod) {
        String sql = "Select s From AllowedPaymentMethod s"
                + " where s.retired=false "
                + " and  s.paymentScheme=:ps "
                + " and s.paymentMethod=:pm ";
        HashMap hm = new HashMap();
        hm.put("ps", paymentScheme);
        hm.put("pm", paymentMethod);

        AllowedPaymentMethod allowedPaymentMethod = getAllowedPaymentMethodFacade().findFirstByJpql(sql, hm);

        if (allowedPaymentMethod != null) {
            return true;
        } else {
            return false;
        }
    }

//    public boolean checkAllowedPaymentMethod(MembershipScheme membershipScheme, PaymentMethod paymentMethod) {
//        String sql = "Select s From AllowedPaymentMethod s"
//                + " where s.retired=false "
//                + " and  s.membershipScheme=:ms "
//                + " and s.paymentMethod=:pm ";
//        HashMap hm = new HashMap();
//        hm.put("ms", membershipScheme);
//        hm.put("pm", paymentMethod);
//
//        AllowedPaymentMethod allowedPaymentMethod = getAllowedPaymentMethodFacade().findFirstByJpql(sql, hm);
//
//        if (allowedPaymentMethod != null) {
//            return true;
//        } else {
//            return false;
//        }
//    }
    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public BillItem fetchBillItem(PatientEncounter patientEncounter, BillType billType, InwardChargeType inwardChargeType) {
        String sql = "select b from BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:btp"
                + " and b.bill.patientEncounter=:pe "
                + " and b.inwardChargeType=:inw"
                + " order by b.id desc ";
        HashMap hm = new HashMap();
        hm.put("pe", patientEncounter);
        hm.put("btp", billType);
        hm.put("inw", inwardChargeType);

        BillItem b = getBillItemFacade().findFirstByJpql(sql, hm);
//        System.err.println("BillItem " + b);
        return b;
    }

    public BillItem fetchBillItem(Bill bill, InwardChargeType inwardChargeType) {
        String sql = "select b from BillItem b "
                + " where b.retired=false "
                + " and b.inwardChargeType=:inw"
                + " and b.bill=:b"
                + " order by b.id desc ";
        HashMap hm = new HashMap();
        hm.put("b", bill);
        hm.put("inw", inwardChargeType);

        BillItem b = getBillItemFacade().findFirstByJpql(sql, hm);
//        System.err.println("BillItem " + b);
        return b;
    }

    public List<Bill> fetchInwardPaymentBills(AdmissionType admissionType, PaymentMethod paymentMethod, Date fromDate, Date toDate, Institution institution) {
        String sql;
        sql = "SELECT b FROM Bill b"
                + " WHERE b.retired=false "
                + " and b.billType = :bTp "
                + " and b.patientEncounter.admissionType=:adm "
                + " and b.patientEncounter.paymentMethod=:pm  "
                + " and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate "
                + " order by b.id";
        Map temMap = new HashMap();
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        temMap.put("bTp", BillType.InwardPaymentBill);
        temMap.put("pm", paymentMethod);
        temMap.put("adm", admissionType);
        temMap.put("ins", institution);
        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public double calFeeValue(FeeType feeType, Date fromDate,
            Date toDate, Institution institution) {
        String sql = "SELECT sum(bf.feeValue)"
                + " FROM BillFee bf "
                + " WHERE bf.bill.billType=:bTp"
                + " and bf.fee.feeType=:ftp "
                + " and bf.bill.institution=:ins"
                + " and bf.bill.toInstitution=:ins"
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + " and ( bf.bill.paymentMethod = :pm1 "
                + " or  bf.bill.paymentMethod = :pm2"
                + " or  bf.bill.paymentMethod = :pm3 "
                + " or  bf.bill.paymentMethod = :pm4)";

        HashMap temMap = new HashMap();
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ftp", feeType);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calFeeValue(List<FeeType> feeTypes, Date fromDate,
            Date toDate, Institution institution) {
        String sql = "SELECT sum(bf.feeValue)"
                + " FROM BillFee bf "
                + " WHERE bf.bill.billType=:bTp"
                + " and bf.fee.feeType in :fts "
                + " and bf.bill.institution=:ins "
                + " and bf.bill.toInstitution=:ins "
                + " and bf.department.institution=:ins "
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + " and ( bf.bill.paymentMethod = :pm1 "
                + " or  bf.bill.paymentMethod = :pm2"
                + " or  bf.bill.paymentMethod = :pm3 "
                + " or  bf.bill.paymentMethod = :pm4)";

        HashMap temMap = new HashMap();
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("fts", feeTypes);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calFeeValue(FeeType feeType, BillItem billItem) {
        String sql = "SELECT sum(bf.feeValue)"
                + " FROM BillFee bf "
                + " WHERE bf.fee.feeType=:ftp "
                + " and bf.billItem=:bt ";

        HashMap temMap = new HashMap();
        temMap.put("ftp", feeType);
        temMap.put("bt", billItem);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calFeeValue(FeeType feeType, Bill bill) {
        String sql = "SELECT sum(bf.feeValue)"
                + " FROM BillFee bf "
                + " WHERE bf.fee.feeType=:ftp "
                + " and bf.bill=:b ";

        HashMap temMap = new HashMap();
        temMap.put("ftp", feeType);
        temMap.put("b", bill);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calFeeValueCredit(FeeType feeType, Date fromDate,
            Date toDate, Institution institution) {
        String sql = "SELECT sum(bf.feeValue)"
                + " FROM BillFee bf "
                + " WHERE bf.bill.billType=:bTp"
                + " and bf.fee.feeType=:ftp "
                + " and bf.bill.institution=:ins"
                + " and bf.bill.creditCompany is not null "
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + " and bf.bill.paymentMethod = :pm ";

        HashMap temMap = new HashMap();
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ftp", feeType);
        temMap.put("pm", PaymentMethod.Credit);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calFeeValue(Date fromDate, Date toDate, Institution institution, Institution creditCompany, List<PaymentMethod> paymentMethods) {
        String sql = "SELECT sum(bf.feeValue)"
                + " FROM BillFee bf "
                + " WHERE bf.bill.billType=:bTp"
                + " and ( bf.fee.feeType=:ftp1 "
                + " or bf.fee.feeType=:ftp2 ) "
                + " and bf.bill.institution=:ins"
                + " and bf.bill.toInstitution=:ins"
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + " and bf.bill.paymentMethod in :pms";

        HashMap temMap = new HashMap();
        if (creditCompany != null) {
            sql += " and bf.bill.creditCompany=:cd ";
            temMap.put("cd", creditCompany);

        }

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ftp1", FeeType.OwnInstitution);
        temMap.put("ftp2", FeeType.Staff);
        temMap.put("pms", paymentMethods);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calFeeValue(Date fromDate, Date toDate, FeeType feetype, Institution institution, Institution creditCompany, List<PaymentMethod> paymentMethods) {
        String sql = "SELECT sum(bf.feeValue)"
                + " FROM BillFee bf "
                + " WHERE bf.bill.billType=:bTp"
                + " and bf.fee.feeType=:ftp1 "
                + " and bf.bill.institution=:ins"
                + " and bf.bill.toInstitution=:ins"
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + " and bf.bill.paymentMethod in :pms";

        HashMap temMap = new HashMap();
        if (creditCompany != null) {
            sql += " and bf.bill.creditCompany=:cd ";
            temMap.put("cd", creditCompany);

        }

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ftp1", feetype);
        //temMap.put("ftp2", FeeType.Staff);
        temMap.put("pms", paymentMethods);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calFeeValue(Date fromDate, Date toDate, List<FeeType> feetypes, Institution institution, Institution creditCompany, List<PaymentMethod> paymentMethods) {
        String sql = "SELECT sum(bf.feeValue)"
                + " FROM BillFee bf "
                + " WHERE bf.bill.billType=:bTp"
                + " and bf.fee.feeType in :ftps "
                + " and bf.bill.institution=:ins"
                + " and bf.bill.toInstitution=:ins"
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + " and bf.bill.paymentMethod in :pms "
                + " and bf.billItem.retired=false "
                + " and bf.bill.retired=false "
                + " and bf.retired=false ";

        HashMap temMap = new HashMap();
        if (creditCompany != null) {
            sql += " and bf.bill.creditCompany=:cd ";
            temMap.put("cd", creditCompany);

        }

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ftps", feetypes);
        //temMap.put("ftp2", FeeType.Staff);
        temMap.put("pms", paymentMethods);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calFeeValue(Date fromDate, Date toDate, FeeType feetype, Institution institution, List<PaymentMethod> paymentMethods) {
        String sql = "SELECT sum(bf.feeValue)"
                + " FROM BillFee bf "
                + " WHERE bf.bill.billType=:bTp"
                + " and bf.fee.feeType=:ftp1 "
                //                + " and bf.bill.institution=:ins"
                + " and bf.billItem.item.department.institution=:ins"
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + " and bf.bill.paymentMethod in :pms";

        HashMap temMap = new HashMap();

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ftp1", feetype);
        //temMap.put("ftp2", FeeType.Staff);
        temMap.put("pms", paymentMethods);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

//    public double calTotalCount(Date fromDate, Date toDate, FeeType feetype, Institution institution, List<PaymentMethod> paymentMethods) {
//        String sql = " SELECT sum(count(bi)) "
//                + " FROM BillItem bi "
//                + " WHERE bi.bill.billType=:bTp"
//                + " and bf.fee.feeType=:ftp1 "
//                //                + " and bf.bill.institution=:ins"
//                + " and bi.item.department.institution=:ins"
//                + " and bi.bill.createdAt between :fromDate and :toDate "
//                + " and bi.bill.paymentMethod in :pms";
//        
//        
//        HashMap temMap = new HashMap();
//
//        temMap.put("toDate", toDate);
//        temMap.put("fromDate", fromDate);
//        temMap.put("ins", institution);
//        temMap.put("bTp", BillType.OpdBill);
//        temMap.put("ftp1", feetype);
//        //temMap.put("ftp2", FeeType.Staff);
//        temMap.put("pms", paymentMethods);
//        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//    }
    public double calFeeValueChannel(Date fromDate, Date toDate, FeeType feetype, Institution institution, List<PaymentMethod> paymentMethods) {

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "SELECT sum(bf.feeValue)"
                + " FROM BillFee bf "
                + " WHERE bf.bill.billType in :bTp"
                + " and bf.fee.feeType=:ftp1 "
                //                + " and bf.bill.institution=:ins"
                + " and bf.billItem.item.department.institution=:ins"
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + " and bf.bill.paymentMethod in :pms";

        HashMap temMap = new HashMap();

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("bTp", bts);
        temMap.put("ftp1", feetype);
        //temMap.put("ftp2", FeeType.Staff);
        temMap.put("pms", paymentMethods);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calFeeValueInward(Date fromDate, Date toDate, FeeType feetype, Institution institution, Department de) {
        String sql = "SELECT sum(bf.feeGrossValue)"
                + " FROM BillFee bf "
                + " WHERE bf.bill.billType=:bTp"
                + " and bf.fee.feeType=:ftp1 "
                + " and bf.bill.department=:dep"
                + " and bf.bill.institution=:ins"
                //+ " and bf.billItem.item.department.institution=:ins"
                + " and bf.bill.createdAt between :fromDate and :toDate ";
        //+ " and bf.bill.paymentMethod in :pms";

        HashMap temMap = new HashMap();

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("dep", de);
        temMap.put("bTp", BillType.InwardBill);
        temMap.put("ftp1", feetype);
        //temMap.put("ftp2", FeeType.Staff);
        //temMap.put("pms", paymentMethods);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calFeeValue(Date fromDate, Date toDate, FeeType feetype, Institution institution, PaymentMethod paymentMethod) {
        String sql = "SELECT sum(bf.feeValue)"
                + " FROM BillFee bf "
                + " WHERE bf.bill.billType=:bTp"
                + " and bf.fee.feeType=:ftp1 "
                //                + " and bf.bill.institution=:ins"
                + " and bf.billItem.item.department.institution=:ins"
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + " and bf.bill.paymentMethod=:pms";

        HashMap temMap = new HashMap();

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ftp1", feetype);
        //temMap.put("ftp2", FeeType.Staff);
        temMap.put("pms", paymentMethod);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calFeeValue(Date fromDate, Date toDate, Institution institution, Institution creditCompany) {
        HashMap temMap = new HashMap();
        String sql = "SELECT sum(bf.feeValue)"
                + " FROM BillFee bf "
                + " WHERE bf.bill.billType=:bTp"
                + " and ( bf.fee.feeType=:ftp1 "
                + " or bf.fee.feeType=:ftp2 ) "
                + " and bf.bill.institution=:ins"
                + " and bf.bill.toInstitution=:ins "
                + " and bf.bill.id in"
                + " (select paidBillItem.referenceBill.id "
                + " from BillItem paidBillItem"
                + "  where paidBillItem.retired=false"
                + "  and  paidBillItem.createdAt between :fromDate and :toDate "
                + " and paidBillItem.bill.billType=:paidBtp)";

        if (creditCompany != null) {
            sql += " and bf.bill.creditCompany=:cd ";
            temMap.put("cd", creditCompany);

        }

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("paidBtp", BillType.CashRecieveBill);
        temMap.put("ftp1", FeeType.OwnInstitution);
        temMap.put("ftp2", FeeType.Staff);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calOutSideInstitutionFees(Date fromDate, Date toDate, Institution institution) {
        String sql = "SELECT sum(bf.performInstitutionFee) "
                + " FROM Bill bf"
                + " WHERE bf.institution=:ins "
                + " and bf.toInstitution!=:ins "
                + " and bf.billType=:bt "
                + " and bf.createdAt between :fromDate and :toDate "
                + " and (bf.paymentMethod = :pm1 "
                + " or bf.paymentMethod = :pm2 "
                + " or bf.paymentMethod = :pm3 "
                + " or bf.paymentMethod = :pm4)";

        HashMap temMap = new HashMap();
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("bt", BillType.OpdBill);
        //   temMap.put("ftp", feeType);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        double val = getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return val;
    }

    public double calOutSideInstitutionFeesWithPro(Date fromDate, Date toDate, Institution institution) {
        String sql = "SELECT sum(bf.performInstitutionFee+bf.staffFee) "
                + " FROM Bill bf"
                + " WHERE bf.institution=:ins "
                + " and bf.toDepartment.institution!=:ins "
                //     + " and ( bf.fee.feeType=:ftp1 "
                //    + " or bf.fee.feeType=:ftp2 )"
                + " and bf.createdAt between :fromDate and :toDate "
                + " and (bf.paymentMethod = :pm1 "
                + " or bf.paymentMethod = :pm2 "
                + " or bf.paymentMethod = :pm3 "
                + " or bf.paymentMethod = :pm4)";

        HashMap temMap = new HashMap();
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        //  temMap.put("ftp1", FeeType.OwnInstitution);
        //   temMap.put("ftp2", FeeType.Staff);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        double val = getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return val;
    }

    public List<Object[]> fetchDoctorPayment(Date fromDate, Date toDate, List<BillType> refBillTypes) {
        String sql = "Select b.referenceBill.toDepartment,sum(b.netValue) "
                + " FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.referenceBill.billType in :refTypes "
                + " and b.createdAt between :fromDate and :toDate"
                + " group by b.referenceBill.toDepartment "
                + " order by b.referenceBill.toDepartment.name ";
        HashMap hm = new HashMap();
        hm.put("bType", BillType.PaymentBill);
        hm.put("refTypes", refBillTypes);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        ////System.out.println("hm = " + hm);
        ////System.out.println("sql = " + sql);
        return getBillItemFacade().findAggregates(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchDoctorPayment(Date fromDate, Date toDate, BillType refBillType) {
        String sql = "Select b.referenceBill.toDepartment,sum(b.netValue) "
                + " FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.referenceBill.billType=:refType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.bill.institution=:ins "
                + " group by b.referenceBill.toDepartment "
                + " order by b.referenceBill.toDepartment.name ";
        HashMap hm = new HashMap();
        hm.put("bType", BillType.PaymentBill);
        hm.put("refType", refBillType);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        hm.put("ins", sessionController.getInstitution());

        return getBillItemFacade().findAggregates(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchDoctorPayment(Date fromDate, Date toDate, BillType refBillType, Institution i) {
        String sql = "Select b.referanceBillItem.bill.toDepartment,sum(b.netValue) "
                + " FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.referanceBillItem.bill.billType=:refType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.bill.institution=:ins "
                + " group by b.referanceBillItem.bill.toDepartment "
                + " order by b.referanceBillItem.bill.toDepartment.name ";

        HashMap hm = new HashMap();
        hm.put("bType", BillType.PaymentBill);
        hm.put("refType", refBillType);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        hm.put("ins", i);

        return getBillItemFacade().findAggregates(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchDoctorPayment(Date fromDate, Date toDate, BillType refBillType, Institution i,
            List<PaymentMethod> paymentMethods, List<PaymentMethod> notPaymentMethods) {
        HashMap hm = new HashMap();
        String sql;

        sql = "Select b.referanceBillItem.bill.toDepartment,sum(b.netValue) "
                + " FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.referanceBillItem.bill.billType=:refType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.bill.institution=:ins ";

        if (paymentMethods != null) {
            sql += " and b.referanceBillItem.bill.paymentMethod in :pms ";
            hm.put("pms", paymentMethods);
        }

        if (notPaymentMethods != null) {
            sql += " and b.referanceBillItem.bill.paymentMethod not in :npms ";
            hm.put("npms", notPaymentMethods);
        }

        sql += " group by b.referanceBillItem.bill.toDepartment "
                + " order by b.referanceBillItem.bill.toDepartment.name ";

        hm.put("bType", BillType.PaymentBill);
        hm.put("refType", refBillType);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        hm.put("ins", i);

        return getBillItemFacade().findAggregates(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchDoctorPaymentBySpecility(Date fromDate, Date toDate, BillType refBillType, Institution i,
            List<PaymentMethod> paymentMethods, List<PaymentMethod> notPaymentMethods) {
        HashMap hm = new HashMap();
        String sql;

        sql = "Select b.paidForBillFee.staff.speciality.name,sum(b.netValue) "
                + " FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.referanceBillItem.bill.billType=:refType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.bill.institution=:ins ";

        if (paymentMethods != null) {
            sql += " and b.referanceBillItem.bill.paymentMethod in :pms ";
            hm.put("pms", paymentMethods);
        }

        if (notPaymentMethods != null) {
            sql += " and b.referanceBillItem.bill.paymentMethod not in :npms ";
            hm.put("npms", notPaymentMethods);
        }

        sql += " group by b.paidForBillFee.staff.speciality.name "
                + " order by b.paidForBillFee.staff.speciality.name ";

        hm.put("bType", BillType.PaymentBill);
        hm.put("refType", refBillType);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        hm.put("ins", i);

        return getBillItemFacade().findAggregates(sql, hm, TemporalType.TIMESTAMP);

    }

//    sql = "select bf.paidForBillFee.staff.speciality,"
//                + " sum(bf.paidForBillFee.feeValue) "
//                + " from BillItem bf"
//                + " where bf.retired=false "
//                + " and bf.bill.cancelled=false "
//                + " and type(bf.bill)=:bclass"
//                + " and bf.bill.billType=:btp"
//                + " and (bf.paidForBillFee.bill.billType=:refBtp1"
//                + " or bf.paidForBillFee.bill.billType=:refBtp2)";
//
//        if (byDischargDate) {
//            sql += " and bf.paidForBillFee.bill.patientEncounter.dateOfDischarge between :fd and :td ";
//        } else {
//            sql += " and bf.createdAt between :fd and :td ";
//        }
//
//        if (speciality != null) {
//            sql += " and bf.paidForBillFee.staff.speciality=:s ";
//            m.put("s", speciality);
//        }
//
//        if (admissionType != null) {
//            sql += " and bf.paidForBillFee.bill.patientEncounter.admissionType=:admTp ";
//            m.put("admTp", admissionType);
//        }
//        if (paymentMethod != null) {
//            sql += " and bf.paidForBillFee.bill.patientEncounter.paymentMethod=:pm";
//            m.put("pm", paymentMethod);
//        }
//        if (institution != null) {
//            sql += " and bf.paidForBillFee.bill.patientEncounter.creditCompany=:cd";
//            m.put("cd", institution);
//        }
//daily return
    public List<Object[]> fetchDoctorPaymentInward(Date fromDate, Date toDate) {
        String sql = "Select b.paidForBillFee.bill.patientEncounter.admissionType,"
                + " sum(b.netValue) "
                + " FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and(b.paidForBillFee.bill.billType=:refType1 "
                + " or b.paidForBillFee.bill.billType=:refType2 )"
                + " and b.bill.createdAt between :fromDate and :toDate"
                + " group by b.paidForBillFee.bill.patientEncounter.admissionType "
                + " order by b.paidForBillFee.bill.patientEncounter.admissionType.name ";
        HashMap hm = new HashMap();
        hm.put("bType", BillType.PaymentBill);
        hm.put("refType1", BillType.InwardBill);
        hm.put("refType2", BillType.InwardProfessional);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        Bill b;
        ////System.out.println("hm = " + hm);
        ////System.out.println("sql = " + sql);
        return getBillItemFacade().findAggregates(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<Object[]> fetchDoctorPaymentInwardTemporaryTesting(Date fromDate, Date toDate) {
        String sql = "Select b.paidForBillFee.bill.patientEncounter.admissionType,"
                + " sum(b.netValue) "
                + " FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and(b.paidForBillFee.bill.billType=:refType1 "
                + " or b.paidForBillFee.bill.billType=:refType2 )"
                + " and b.bill.createdAt between :fromDate and :toDate"
                + " group by b.paidForBillFee.bill.patientEncounter.admissionType "
                + " order by b.paidForBillFee.bill.patientEncounter.admissionType.name ";
        HashMap hm = new HashMap();
        hm.put("bType", BillType.PaymentBill);
        hm.put("refType1", BillType.InwardBill);
        hm.put("refType2", BillType.InwardProfessional);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        ////System.out.println("hm = " + hm);
        ////System.out.println("sql = " + sql);
        return getBillItemFacade().findAggregates(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchDoctorPaymentInward(AdmissionType admissionType, Date fromDate, Date toDate) {
        String sql = "Select b.paidForBillFee.staff.speciality,"
                + " sum(b.paidForBillFee.feeValue) "
                + " FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.cancelled=false " //inward report not tally
                + " and b.bill.billType=:bType "
                + " and b.paidForBillFee.bill.patientEncounter.admissionType=:adt"
                + " and( b.paidForBillFee.bill.billType=:refType1 "
                + " or b.paidForBillFee.bill.billType=:refType2 )"
                + " and b.bill.createdAt between :fromDate and :toDate"
                + " group by b.paidForBillFee.staff.speciality "
                + " order by b.paidForBillFee.staff.speciality.name ";
        HashMap hm = new HashMap();
        hm.put("bType", BillType.PaymentBill);
        hm.put("refType1", BillType.InwardBill);
        hm.put("refType2", BillType.InwardProfessional);
        hm.put("adt", admissionType);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);

        return getBillItemFacade().findAggregates(sql, hm, TemporalType.TIMESTAMP);

    }

//    public List<BillItem> fetchDoctorPaymentInward(AdmissionType admissionType, Date fromDate, Date toDate) {
//        String sql = "Select b "
//                + " FROM BillItem b "
//                + " where b.retired=false "
//                + " and b.bill.billType=:bType "
//                + " and( b.paidForBillFee.bill.billType=:refType1 "
//                + " or b.paidForBillFee.bill.billType=:refType2 )"
//                + " and b.createdAt between :fromDate and :toDate"
//                + " order by b.paidForBillFee.bill.patientEncounter.id ";
//        HashMap hm = new HashMap();
//        hm.put("bType", BillType.PaymentBill);
//        hm.put("refType1", BillType.InwardBill);
//        hm.put("refType2", BillType.InwardProfessional);
//        hm.put("fromDate", fromDate);
//        hm.put("toDate", toDate);
//
//        return getBillItemFacade().findAggregates(sql, hm, TemporalType.TIMESTAMP);
//
//    }
//    
    public double calDoctorPaymentInward(Date fromDate, Date toDate, Institution institution) {
        String sql = "Select sum(b.netValue) "
                + " FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType"
                + " and b.bill.institution=:ins "
                + " and (b.paidForBillFee.bill.billType=:refType1 "
                + " or b.paidForBillFee.bill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate ";

        HashMap hm = new HashMap();
        hm.put("bType", BillType.PaymentBill);
        hm.put("refType1", BillType.InwardBill);
        hm.put("refType2", BillType.InwardProfessional);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        hm.put("ins", institution);

        return getBillItemFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double calDoctorPayment(Date fromDate, Date toDate, BillType refBillType) {
        String sql = "Select sum(b.netValue) "
                + " FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.referanceBillItem.bill.billType=:refType "
                + " and b.createdAt between :fromDate and :toDate ";
        HashMap hm = new HashMap();
        hm.put("bType", BillType.PaymentBill);
        hm.put("refType", refBillType);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);

        return getBillItemFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double calDoctorPayment(Date fromDate, Date toDate, BillType refBillType, Institution i) {
        String sql = "Select sum(b.netValue) "
                + " FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.referanceBillItem.bill.billType=:refType "
                + " and b.bill.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate ";

        HashMap hm = new HashMap();
        hm.put("bType", BillType.PaymentBill);
        hm.put("refType", refBillType);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        hm.put("ins", i);

        return getBillItemFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double calDoctorPayment(Date fromDate, Date toDate, BillType refBillType, Institution i, List<PaymentMethod> paymentMethods, List<PaymentMethod> notPaymentMethods) {
        String sql;
        HashMap hm = new HashMap();
        sql = "Select sum(b.netValue) "
                + " FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.referanceBillItem.bill.billType=:refType "
                + " and b.bill.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate ";

        if (paymentMethods != null) {
            sql += " and b.referanceBillItem.bill.paymentMethod in :pms ";
            hm.put("pms", paymentMethods);
        }

        if (notPaymentMethods != null) {
            sql += " and b.referanceBillItem.bill.paymentMethod not in :npms ";
            hm.put("npms", notPaymentMethods);
        }

        hm.put("bType", BillType.PaymentBill);
        hm.put("refType", refBillType);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        hm.put("ins", i);

        return getBillItemFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double calDoctorPayment(Date fromDate, Date toDate, List<BillType> refBillTypes) {
        String sql = "Select sum(b.netValue) "
                + " FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.referenceBill.billType in :refTypes "
                + " and b.createdAt between :fromDate and :toDate ";
        HashMap hm = new HashMap();
        hm.put("bType", BillType.PaymentBill);
        hm.put("refTypes", refBillTypes);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);

        return getBillItemFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double calInwardPaymentTotal1(AdmissionType admissionType, Date fromDate, Date toDate, Institution institution) {
        String sql;
        sql = "SELECT sum(b.netTotal)"
                + " FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType = :bTp "
                + " and b.patientEncounter.admissionType=:adm "
                + " and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate"
                + "  order by b.id";
        Map temMap = new HashMap();
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        temMap.put("bTp", BillType.InwardPaymentBill);
        temMap.put("adm", admissionType);
        temMap.put("ins", institution);
        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public List<Object[]> calInwardPaymentTotalOld(AdmissionType admissionType, Date fromDate, Date toDate, Institution institution) {
        String sql;
        sql = "SELECT b.patientEncounter.admissionType,"
                + " sum(b.netTotal) "
                + " FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType = :bTp "
                + " and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate"
                + " group by b.patientEncounter.admissionType"
                + " order by b.patientEncounter.admissionType.name ";
        Map temMap = new HashMap();
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        temMap.put("bTp", BillType.InwardPaymentBill);
        temMap.put("ins", institution);
        return getBillFacade().findAggregates(sql, temMap, TemporalType.TIMESTAMP);
    }

    public List<Object[]> calInwardPaymentTotal(Date fromDate, Date toDate, Institution institution) {
        String sql;
        sql = "SELECT b.patientEncounter.admissionType,"
                + " b.patientEncounter.paymentMethod,"
                + " sum(b.netTotal) "
                + " FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType = :bTp "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate"
                + " group by b.patientEncounter.admissionType,"
                + " b.patientEncounter.paymentMethod "
                + " order by b.patientEncounter.admissionType.name,"
                + " b.patientEncounter.paymentMethod ";
        Map temMap = new HashMap();
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        temMap.put("bTp", BillType.InwardPaymentBill);
        temMap.put("ins", institution);
        return getBillFacade().findAggregates(sql, temMap, TemporalType.TIMESTAMP);
    }

    public double calInwardPaymentTotalValue(Date fromDate, Date toDate, Institution institution) {
        String sql;
        sql = "SELECT sum(b.netTotal) "
                + " FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType = :bTp "
                + " and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate"
                + "  order by b.id";
        Map temMap = new HashMap();
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        temMap.put("bTp", BillType.InwardPaymentBill);
        temMap.put("ins", institution);
        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public List<Bill> fetchBills(BillType billType, Date fromDate, Date toDate, Institution institution) {
        String sql;
        sql = "SELECT b FROM Bill b"
                + " WHERE b.retired=false "
                + " and b.billType = :bTp"
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate"
                + " order by b.id";

        Map temMap = new HashMap();
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        temMap.put("bTp", billType);
        temMap.put("ins", institution);

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public List<BillItem> fetchBillItems(BillType billType, boolean isOpd, Date fromDate, Date toDate, Institution institution) {
        String sql;
        HashMap temMap = new HashMap();

        sql = "SELECT b FROM BillItem b"
                + " WHERE b.bill.institution=:ins"
                + " and b.bill.billType=:btp"
                + " and b.retired=false "
                + " and b.createdAt between :fromDate and :toDate ";

        if (isOpd) {
            sql += " and b.referenceBill.billType=:refTp";
            temMap.put("refTp", BillType.OpdBill);
        } else {
            sql += " and b.patientEncounter is not null ";
        }

        sql += " order by b.id";

        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        temMap.put("btp", billType);
        temMap.put("ins", institution);

        return getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public List<Bill> fetchBills(BillType billType, boolean isOpd, Date fromDate, Date toDate, Institution institution) {
        String sql;
        HashMap temMap = new HashMap();

        sql = "SELECT distinct(b.bill) FROM BillItem b"
                + " WHERE b.bill.institution=:ins"
                + " and b.bill.billType=:btp"
                + " and b.retired=false "
                + " and b.createdAt between :fromDate and :toDate ";

        if (isOpd) {
            sql += " and b.referenceBill.billType=:refTp";
            temMap.put("refTp", BillType.OpdBill);
        } else {
            sql += " and b.patientEncounter is not null ";
        }

        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        temMap.put("btp", billType);
        temMap.put("ins", institution);

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public List<BillItem> fetchBillItemsPharmacy(BillType billType, Date fromDate, Date toDate, Institution institution) {
        String sql;
        HashMap temMap = new HashMap();

        sql = "SELECT b FROM BillItem b"
                + " WHERE b.bill.institution=:ins"
                + " and b.bill.billType=:btp"
                + " and b.retired=false "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.referenceBill.billType in :refTp "
                + " order by b.id ";

        temMap.put("refTp", Arrays.asList(new BillType[]{BillType.PharmacySale, BillType.PharmacyWholeSale}));
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        temMap.put("btp", billType);
        temMap.put("ins", institution);

        return getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public List<Bill> fetchBillItemsPharmacyOld(BillType billType, Date fromDate, Date toDate, Institution institution) {
        String sql;
        HashMap temMap = new HashMap();

        sql = "SELECT b FROM Bill b"
                + " WHERE b.institution=:ins"
                + " and b.billType=:btp"
                + " and b.retired=false "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.referenceBill.billType in :refTp "
                + " order by b.id ";

        temMap.put("refTp", Arrays.asList(new BillType[]{BillType.PharmacySale, BillType.PharmacyWholeSale}));
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        temMap.put("btp", billType);
        temMap.put("ins", institution);

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public List<BillItem> fetchBillItems(Bill b, Date fromDate, Date toDate, Institution institution) {
        String sql;
        HashMap temMap = new HashMap();

        sql = "SELECT b FROM BillItem b WHERE "
                + "b.bill.institution=:ins "
                + " and b.retired=false "
                + " and b.bill=:bl "
                + " and b.createdAt between :fromDate and :toDate "
                + " order by b.id";

        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        temMap.put("bl", b);
        temMap.put("ins", institution);
        return getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public List<Category> fetchBilledOpdCategory(Date fromDate, Date toDate, Institution institution) {
        String sql;
        Map temMap = new HashMap();

        sql = "select distinct(bi.item.category) "
                + " FROM BillItem bi "
                + " where bi.bill.institution=:ins "
                + " and bi.item.department.institution=:ins "
                + " and  bi.bill.billType= :bTp  "
                + " and  bi.bill.createdAt between :fromDate and :toDate "
                + " and (bi.bill.paymentMethod = :pm1 "
                + " or  bi.bill.paymentMethod = :pm2 "
                + " or  bi.bill.paymentMethod = :pm3 "
                + " or  bi.bill.paymentMethod = :pm4)"
                + " order by bi.item.category.name";
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        List<Category> tmp = getCategoryFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return tmp;

    }

    public List<Category> fetchBilledOpdCategoryCredit(Date fromDate, Date toDate, Institution institution, Institution creditCompany) {
        String sql;
        Map temMap = new HashMap();

        sql = "select distinct(bi.item.category) "
                + " FROM BillItem bi "
                + " where bi.bill.institution=:ins "
                + "and bi.bill.creditCompany=:cred "
                + " and bi.item.department.institution=:ins "
                + " and  bi.bill.billType= :bTp  "
                + " and  bi.bill.createdAt between :fromDate and :toDate "
                + " and (bi.bill.paymentMethod = :pm)"
                + " order by bi.item.category.name";
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("cred", creditCompany);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm", PaymentMethod.Credit);
        List<Category> tmp = getCategoryFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return tmp;

    }

    public List<Object[]> fetchOutSideInstitutionFees(Date fromDate, Date toDate, Institution institution) {
        String sql = "SELECT bf.toDepartment,sum(bf.performInstitutionFee)"
                + " FROM Bill bf"
                + " WHERE bf.institution=:ins"
                + " and bf.toInstitution!=:ins "
                + " and bf.billType=:bt "
                + " and bf.createdAt between :fromDate and :toDate "
                + " and (bf.paymentMethod = :pm1 "
                + " or bf.paymentMethod = :pm2 "
                + " or bf.paymentMethod = :pm3 "
                + " or bf.paymentMethod = :pm4)"
                + " group by bf.toDepartment "
                + " order by bf.toDepartment.name ";

        HashMap temMap = new HashMap();
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        // temMap.put("ftp", feeType);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        temMap.put("bt", BillType.OpdBill);

        List<Object[]> list = getBillFeeFacade().findAggregates(sql, temMap, TemporalType.TIMESTAMP);

        return list;
    }

    public double calOutSideDepartmentFees(Department department, FeeType feeType, Date fromDate, Date toDate, Institution institution) {
        String sql = "SELECT sum(bf.feeValue)"
                + " FROM BillFee bf"
                + " WHERE bf.billItem.bill.institution=:ins "
                + " and bf.billItem.item.institution!=:ins "
                + " and bf.fee.feeType=:ftp "
                + " and bf.billItem.bill.createdAt between :fromDate and :toDate "
                + " and (bf.billItem.bill.paymentMethod = :pm1 "
                + " or bf.billItem.bill.paymentMethod = :pm2 "
                + " or bf.billItem.bill.paymentMethod = :pm3 "
                + " or bf.billItem.bill.paymentMethod = :pm4)"
                + " and bf.billItem.item.department=:dep ";

        HashMap temMap = new HashMap();
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("ftp", feeType);
        temMap.put("dep", department);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);

        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    List<Bill> bills;

    public void createOutSideDepartment(Date fromDate, Date toDate, Institution institution) {
        String sql = "SELECT bf "
                + " FROM Bill bf "
                + " WHERE bf.institution=:ins "
                + " and bf.toDepartment.institution!=:ins "
                + " and bf.billType=:bt "
                + " and bf.createdAt between :fromDate and :toDate "
                + " and (bf.paymentMethod = :pm1 "
                + " or bf.paymentMethod = :pm2 "
                + " or bf.paymentMethod = :pm3 "
                + " or bf.paymentMethod = :pm4)"
                + " group by bf.toDepartment "
                + " order by bf.toDepartment.name ";

        HashMap temMap = new HashMap();
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        // temMap.put("ftp1", FeeType.OwnInstitution);
        //   temMap.put("ftp2", FeeType.Staff);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        temMap.put("bt", BillType.OpdBill);

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchOutSideDepartment(Date fromDate, Date toDate, Institution institution) {
        String sql = "SELECT bf.toDepartment,sum(bf.performInstitutionFee),sum(bf.staffFee)"
                + " FROM Bill bf"
                + " WHERE bf.institution=:ins"
                + " and bf.toDepartment.institution!=:ins "
                + " and bf.billType=:bt "
                + " and bf.createdAt between :fromDate and :toDate "
                + " and (bf.paymentMethod = :pm1 "
                + " or bf.paymentMethod = :pm2 "
                + " or bf.paymentMethod = :pm3 "
                + " or bf.paymentMethod = :pm4)"
                + " group by bf.toDepartment "
                + " order by bf.toDepartment.name ";

        HashMap temMap = new HashMap();
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        // temMap.put("ftp1", FeeType.OwnInstitution);
        //   temMap.put("ftp2", FeeType.Staff);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        temMap.put("bt", BillType.OpdBill);

        return getDepartmentFacade().findAggregates(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calDepartmentSale(Department d, Date fromDate, Date toDate) {
        String sql = "Select sum(b.netTotal) from Bill b "
                + " where b.retired=false"
                + " and  b.billType=:bType"
                + " and b.referenceBill.department=:dep "
                + " and b.createdAt between :fromDate and :toDate "
                + " and (b.paymentMethod = :pm1 "
                + " or  b.paymentMethod = :pm2 "
                + " or  b.paymentMethod = :pm3 "
                + " or  b.paymentMethod = :pm4)";
        HashMap hm = new HashMap();
        hm.put("bType", BillType.PharmacySale);
        hm.put("dep", d);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        hm.put("pm1", PaymentMethod.Cash);
        hm.put("pm2", PaymentMethod.Card);
        hm.put("pm3", PaymentMethod.Cheque);
        hm.put("pm4", PaymentMethod.Slip);
        double netTotal = getBillFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

        return netTotal;
    }

    public double calInstitutionSale(Date fromDate, Date toDate, Institution institution, BillType billType) {
        String sql = "Select sum(b.netTotal)"
                + " from Bill b "
                + " where b.retired=false"
                + " and b.billType=:bType"
                + " and b.referenceBill.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and (b.paymentMethod = :pm1 "
                + " or  b.paymentMethod = :pm2 "
                + " or  b.paymentMethod = :pm3 "
                + " or  b.paymentMethod = :pm4)";
        HashMap hm = new HashMap();
        hm.put("bType", billType);
        hm.put("ins", institution);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        hm.put("pm1", PaymentMethod.Cash);
        hm.put("pm2", PaymentMethod.Card);
        hm.put("pm3", PaymentMethod.Cheque);
        hm.put("pm4", PaymentMethod.Slip);
        double netTotal = getBillFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

        return netTotal;
    }

    public double calInstitutionSaleCredit(Date fromDate, Date toDate, Institution institution, BillType billType) {
        String sql = "Select sum(b.netTotal)"
                + " from Bill b "
                + " where b.retired=false"
                + " and b.billType=:bType"
                + " and b.referenceBill.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.paymentMethod=:pm ";
        HashMap hm = new HashMap();
        hm.put("bType", billType);
        hm.put("ins", institution);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        hm.put("pm", PaymentMethod.Credit);
        double netTotal = getBillFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

        return netTotal;
    }

    public double calChannelTotal(Date fromDate, Date toDate, Institution institution) {
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "Select sum(b.netTotal)"
                + " from Bill b "
                + " where b.retired=false "
                //                + " and b.cancelled=false "
                //                + " and b.refunded=false "
                + " and b.billType in :bType "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate ";

        HashMap hm = new HashMap();
        hm.put("bType", bts);
        hm.put("ins", institution);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        double netTotal = getBillFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

        return netTotal;
    }

    public double calBillTotal(BillType billType, Date fromDate, Date toDate, Institution institution) {
        String sql;
        sql = " SELECT sum(b.netTotal) "
                + " FROM Bill b"
                + " WHERE b.retired=false "
                + " and b.billType = :bTp "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " order by b.id";

        Map temMap = new HashMap();
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        temMap.put("bTp", billType);
        temMap.put("ins", institution);

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public double calBillTotal(BillType billType, boolean isOpd, Date fromDate, Date toDate, Institution institution) {
        String sql;
        Map temMap = new HashMap();
        sql = " SELECT sum(b.netValue) "
                + " FROM BillItem b"
                + " WHERE b.bill.retired=false "
                + " and b.bill.billType = :bTp "
                + " and b.bill.institution=:ins "
                + " and b.bill.createdAt between :fromDate and :toDate ";

        if (isOpd) {
            sql += " and b.referenceBill.billType=:refTp";
            temMap.put("refTp", BillType.OpdBill);
        } else {
            sql += " and b.patientEncounter is not null ";
        }

        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        temMap.put("bTp", billType);
        temMap.put("ins", institution);

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public double calBillTotalPharmacy(BillType billType, Date fromDate, Date toDate, Institution institution) {
        String sql;
        Map temMap = new HashMap();
        sql = " SELECT sum(b.netValue) "
                + " FROM BillItem b"
                + " WHERE b.bill.retired=false "
                + " and b.bill.billType = :bTp "
                + " and b.bill.institution=:ins "
                + " and b.bill.createdAt between :fromDate and :toDate "
                + " and b.referenceBill.billType in :refTp ";

        temMap.put("refTp", Arrays.asList(new BillType[]{BillType.PharmacySale, BillType.PharmacyWholeSale}));
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        temMap.put("bTp", billType);
        temMap.put("ins", institution);

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public double calBillTotalPharmacyold(BillType billType, Date fromDate, Date toDate, Institution institution) {
        String sql;
        Map temMap = new HashMap();
        sql = " SELECT sum(b.netTotal) "
                + " FROM Bill b"
                + " WHERE b.retired=false "
                + " and b.billType = :bTp "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.referenceBill.billType in :refTp ";

        temMap.put("refTp", Arrays.asList(new BillType[]{BillType.PharmacySale, BillType.PharmacyWholeSale}));
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        temMap.put("bTp", billType);
        temMap.put("ins", institution);

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public List<Bill> fetchBills(PaymentMethod paymentMethod, Date fromDate, Date toDate, Institution institution) {
        List<Bill> lstBills;
        String sql;
        Map temMap = new HashMap();
        sql = "select b from Bill b "
                + " where type(b)!=:type "
                + " and b.institution=:ins "
                + " and b.paymentMethod = :bTp "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false"
                + " order by b.id desc  ";

        temMap.put("bTp", paymentMethod);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("type", PreBill.class);
        temMap.put("ins", institution);
        lstBills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return lstBills;

    }

    public double calBillTotal(PaymentMethod paymentMethod, Date fromDate, Date toDate, Institution institution) {

        String sql;
        Map temMap = new HashMap();
        sql = "select sum(b.netTotal) "
                + " from Bill b "
                + " where type(b)!=:type "
                + " and b.institution=:ins "
                + " and b.paymentMethod = :bTp "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false"
                + " order by b.id desc  ";

        temMap.put("bTp", paymentMethod);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("type", PreBill.class);
        temMap.put("ins", institution);

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calBillTotalWithVat(PaymentMethod paymentMethod, Date fromDate, Date toDate, Institution institution) {

        String sql;
        Map temMap = new HashMap();
        sql = "select sum(b.netTotal+b.vat) "
                + " from Bill b "
                + " where type(b)!=:type "
                + " and b.institution=:ins "
                + " and b.paymentMethod = :bTp "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false"
                + " order by b.id desc  ";

        temMap.put("bTp", paymentMethod);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("type", PreBill.class);
        temMap.put("ins", institution);

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchDepartmentSale(Date fromDate, Date toDate, Institution institution, BillType billType) {
        PaymentMethod[] pms = new PaymentMethod[]{PaymentMethod.Cash, PaymentMethod.Card, PaymentMethod.Cheque, PaymentMethod.Slip};

        String sql = "Select b.referenceBill.department,"
                + " sum(b.netTotal) "
                + " from Bill b "
                + " where b.retired=false"
                + " and  b.billType=:bType"
                + " and b.referenceBill.department.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.paymentMethod in :pm"
                + " and type(b)!=:cl "
                + " group by b.referenceBill.department"
                + " order by b.referenceBill.department.name";
        HashMap hm = new HashMap();
        hm.put("bType", billType);
        hm.put("cl", PreBill.class);
        hm.put("ins", institution);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        hm.put("pm", Arrays.asList(pms));
        return getBillFacade().findAggregates(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchDepartmentSaleCredit(Date fromDate, Date toDate, Institution institution, BillType billType) {
        PaymentMethod[] pms = new PaymentMethod[]{PaymentMethod.Credit};

        String sql = "Select b.referenceBill.department,"
                + " sum(b.netTotal) "
                + " from Bill b "
                + " where b.retired=false"
                + " and  b.billType=:bType"
                + " and b.referenceBill.department.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.paymentMethod in :pm"
                + " and type(b)!=:cl "
                + " group by b.referenceBill.department"
                + " order by b.referenceBill.department.name";
        HashMap hm = new HashMap();
        hm.put("bType", billType);
        hm.put("cl", PreBill.class);
        hm.put("ins", institution);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        hm.put("pm", Arrays.asList(pms));
        return getBillFacade().findAggregates(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchChannelBills(Date fromDate, Date toDate, Institution institution) {
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "Select b.toDepartment,"
                + " sum(b.netTotal) "
                + " from Bill b "
                + " where b.retired=false"
                //                + " and b.cancelled=false "
                //                + " and b.refunded=false "
                + " and  b.billType in :bType"
                + " and b.toDepartment.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " group by b.toDepartment"
                + " order by b.toDepartment.name";

        HashMap hm = new HashMap();
        hm.put("bType", bts);
        hm.put("ins", institution);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        return getBillFacade().findAggregates(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Item> fetchBilledOpdItem(Category d, Date fromDate, Date toDate, Institution institution) {
        String sql;
        Map temMap = new HashMap();
        if (d == null) {
            return new ArrayList<>();
        }
        sql = "select distinct(bi.item) "
                + " FROM BillItem bi where "
                + "  bi.bill.institution=:ins "
                + " and bi.item.department.institution=:ins "
                + " and  bi.bill.billType= :bTp  "
                + " and bi.item.category=:cat"
                + " and  bi.bill.createdAt between :fromDate and :toDate "
                + " and ( bi.bill.paymentMethod = :pm1 "
                + " or  bi.bill.paymentMethod = :pm2 "
                + " or  bi.bill.paymentMethod = :pm3 "
                + " or  bi.bill.paymentMethod = :pm4)"
                + " order by bi.item.name";
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("cat", d);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        List<Item> tmp = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return tmp;

    }

    public List<Object[]> fetchBilledDepartmentItemCalTotal(Date fromDate, Date toDate, Department department) {
        String sql;
        Map temMap = new HashMap();

        sql = "select bf.toDepartment,sum(bf.netTotal) "
                + " FROM Bill bf "
                + " where bf.department=:ins "
                + " and  bf.billType= :bTp  "
                + " and  bf.createdAt between :fromDate and :toDate ";

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", department);
        temMap.put("bTp", BillType.PharmacyIssue);

        return getBillFeeFacade().findAggregates(sql, temMap, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchBilledDepartmentItem(Date fromDate, Date toDate, Department department) {
        String sql;
        Map temMap = new HashMap();

        sql = "select bf.toDepartment,sum(bf.netTotal) "
                + " FROM Bill bf "
                + " where bf.department=:ins "
                + " and  bf.billType= :bTp  "
                + " and  bf.createdAt between :fromDate and :toDate "
                + " group by bf.toDepartment"
                + " order by bf.toDepartment.name  ";
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", department);
        temMap.put("bTp", BillType.PharmacyIssue);

        return getBillFeeFacade().findAggregates(sql, temMap, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchBilledDepartmentItem(Date fromDate, Date toDate, Department department, BillType bt) {
        String sql;
        Map temMap = new HashMap();

        sql = "select b.toDepartment,sum(b.netTotal) "
                + " FROM Bill b "
                + " where b.department=:dept "
                + " and  b.billType= :bTp  "
                + " and  b.createdAt between :fromDate and :toDate "
                + " group by b.toDepartment"
                + " order by b.toDepartment.name  ";
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("dept", department);
        temMap.put("bTp", bt);

        return getBillFeeFacade().findAggregates(sql, temMap, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchBilledDepartmentItem(Date fromDate, Date toDate, Department department, BillType bt, boolean toDep) {
        String sql;
        Map temMap = new HashMap();

        sql = "select ";
        if (toDep) {
            sql += " b.toDepartment";
        } else {
            sql += " b.fromDepartment";
        }
        sql += ",sum(b.netTotal) "
                + " FROM Bill b "
                + " where b.department=:dept "
                + " and  b.billType= :bTp  "
                + " and  b.createdAt between :fromDate and :toDate ";
        if (toDep) {
            sql += " group by b.toDepartment "
                    + " order by b.toDepartment.name ";
        } else {
            sql += " group by b.fromDepartment"
                    + " order by b.fromDepartment.name";
        }
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("dept", department);
        temMap.put("bTp", bt);

        return getBillFeeFacade().findAggregates(sql, temMap, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchBilledDepartmentItemStore(Date fromDate, Date toDate, Department department) {
        String sql;
        Map temMap = new HashMap();

        sql = "select bf.toDepartment,sum(bf.netTotal) "
                + " FROM Bill bf "
                + " where bf.department=:ins "
                + " and  bf.billType= :bTp  "
                + " and  bf.createdAt between :fromDate and :toDate "
                + " group by bf.toDepartment"
                + " order by bf.toDepartment.name  ";
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", department);
        temMap.put("bTp", BillType.StoreIssue);

        return getBillFeeFacade().findAggregates(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calNetTotalBilledDepartmentItem(Date fromDate, Date toDate, Department department) {
        String sql;
        Map temMap = new HashMap();

        sql = "select bf.toDepartment,sum(bf.netTotal) "
                + " FROM Bill bf "
                + " where bf.department=:ins "
                + " and  bf.billType= :bTp  "
                + " and  bf.createdAt between :fromDate and :toDate "
                + " group by bf.toDepartment"
                + " order by bf.toDepartment.name  ";
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", department);
        temMap.put("bTp", BillType.PharmacyIssue);

        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calNetTotalBilledDepartmentItemStore(Date fromDate, Date toDate, Department department) {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(bf.netTotal) "
                + " FROM Bill bf "
                + " where bf.department=:ins "
                + " and  bf.billType= :bTp  "
                + " and  bf.createdAt between :fromDate and :toDate ";
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", department);
        temMap.put("bTp", BillType.StoreIssue);

        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchBilledOpdItem(Category d, FeeType feeType, Date fromDate, Date toDate, Institution institution) {
        String sql;
        Map temMap = new HashMap();

        sql = "select bf.billItem.item,sum(bf.feeValue) "
                + " FROM BillFee bf "
                + " where bf.billItem.bill.institution=:ins "
                + " and bf.billItem.item.department.institution=:ins "
                + " and  bf.billItem.bill.billType= :bTp  "
                + " and bf.billItem.item.category=:cat"
                + " and bf.fee.feeType=:ftp "
                + " and  bf.billItem.bill.createdAt between :fromDate and :toDate "
                + " and ( bf.billItem.bill.paymentMethod = :pm1 "
                + " or  bf.billItem.bill.paymentMethod = :pm2 "
                + " or  bf.billItem.bill.paymentMethod = :pm3 "
                + " or  bf.billItem.bill.paymentMethod = :pm4)"
                + " group by bf.billItem.item"
                + " order by bf.billItem.item.name  ";
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("cat", d);
        temMap.put("ftp", feeType);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        return getBillFeeFacade().findAggregates(sql, temMap, TemporalType.TIMESTAMP);

    }

    public long calBilledItemCount(Item i, Date fromDate, Date toDate, Institution institution) {
        long billed, cancelled, refunded;
        billed = cancelled = refunded = 0l;

        billed = billItemCount(new BilledBill(), i, fromDate, toDate, institution);
        cancelled = billItemCount(new CancelledBill(), i, fromDate, toDate, institution);
        refunded = billItemCount(new RefundBill(), i, fromDate, toDate, institution);

        return billed - (cancelled + refunded);

    }

    public double calFeeValue(Item i, FeeType feeType, Date fromDate, Date toDate, Institution institution) {
        String sql = "SELECT sum(bf.feeValue) FROM BillFee bf "
                + " WHERE bf.bill.billType=:bTp "
                + " and bf.fee.feeType=:ftp "
                + " and bf.bill.institution=:ins "
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + " and bf.billItem.item=:itm"
                + " and ( bf.bill.paymentMethod = :pm1 "
                + " or  bf.bill.paymentMethod = :pm2"
                + " or  bf.bill.paymentMethod = :pm3 "
                + " or  bf.bill.paymentMethod = :pm4)";

        HashMap temMap = new HashMap();
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("itm", i);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ftp", feeType);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calFeeValue(Category cat, FeeType feeType, Date fromDate, Date toDate, Institution institution) {
        String sql = "SELECT sum(bf.feeValue) "
                + " FROM BillFee bf "
                + " WHERE bf.bill.billType=:bTp "
                + " and bf.fee.feeType=:ftp "
                + " and bf.bill.institution=:ins "
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + " and bf.billItem.item.category=:cat "
                + " and ( bf.bill.paymentMethod = :pm1 "
                + " or  bf.bill.paymentMethod = :pm2"
                + " or  bf.bill.paymentMethod = :pm3"
                + " or  bf.bill.paymentMethod = :pm4)";

        HashMap temMap = new HashMap();
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("cat", cat);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ftp", feeType);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calFeeValue(Category cat, Date fromDate, Date toDate, Institution institution) {
        String sql = "SELECT sum(bf.feeValue) "
                + " FROM BillFee bf "
                + " WHERE bf.bill.billType=:bTp "
                + " and bf.bill.institution=:ins "
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + " and bf.billItem.item.category=:cat "
                + " and ( bf.bill.paymentMethod = :pm1 "
                + " or  bf.bill.paymentMethod = :pm2"
                + " or  bf.bill.paymentMethod = :pm3"
                + " or  bf.bill.paymentMethod = :pm4)";

        HashMap temMap = new HashMap();
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("cat", cat);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchFeeValue(FeeType feeType, Date fromDate, Date toDate, Institution institution) {
        String sql = "SELECT bf.billItem.item.category,sum(bf.feeValue) "
                + " FROM BillFee bf "
                + " WHERE bf.bill.billType=:bTp "
                + " and bf.fee.feeType=:ftp "
                + " and bf.bill.institution=:ins "
                + " and bf.billItem.item.department.institution=:ins "
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + " and ( bf.bill.paymentMethod = :pm1 "
                + " or  bf.bill.paymentMethod = :pm2"
                + " or  bf.bill.paymentMethod = :pm3"
                + " or  bf.bill.paymentMethod = :pm4)"
                + " group by bf.billItem.item.category "
                + " order by bf.billItem.item.category.name";

        HashMap temMap = new HashMap();
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ftp", feeType);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        return getBillFeeFacade().findAggregates(sql, temMap, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchFeeValueCredit(FeeType feeType, Date fromDate, Date toDate, Institution institution) {
        String sql = "SELECT bf.billItem.item.category,sum(bf.feeValue) "
                + " FROM BillFee bf "
                + " WHERE bf.bill.billType=:bTp "
                + " and bf.fee.feeType=:ftp "
                + " and bf.bill.institution=:ins "
                + " and bf.billItem.item.department.institution=:ins "
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + " and ( bf.bill.paymentMethod = :pm1 "
                + " or  bf.bill.paymentMethod = :pm2"
                + " or  bf.bill.paymentMethod = :pm3"
                + " or  bf.bill.paymentMethod = :pm4)"
                + " group by bf.billItem.item.category "
                + " order by bf.billItem.item.category.name";

        HashMap temMap = new HashMap();
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ftp", feeType);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        return getBillFeeFacade().findAggregates(sql, temMap, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchFeeValue(Date fromDate, Date toDate, Institution institution) {
        String sql = "SELECT bf.billItem.item.category,sum(bf.feeValue) "
                + " FROM BillFee bf "
                + " WHERE bf.bill.billType=:bTp "
                + " and (bf.fee.feeType=:ftp1"
                + "  or bf.fee.feeType=:ftp2) "
                + " and bf.bill.institution=:ins "
                + " and bf.billItem.item.department.institution=:ins "
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + " and ( bf.bill.paymentMethod = :pm1 "
                + " or  bf.bill.paymentMethod = :pm2"
                + " or  bf.bill.paymentMethod = :pm3"
                + " or  bf.bill.paymentMethod = :pm4)"
                + " group by bf.billItem.item.category"
                + " order by bf.billItem.item.category.name";

        HashMap temMap = new HashMap();
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ftp1", FeeType.OwnInstitution);
        temMap.put("ftp2", FeeType.Staff);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        return getBillFeeFacade().findAggregates(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double calBillValue(PaymentMethod paymentMethod, Date fromDate, Date toDate, Institution institution) {
        String sql = "SELECT sum(b.netTotal) FROM Bill b"
                + " WHERE b.institution=:ins "
                + " and  b.createdAt between :fromDate and :toDate"
                + " and  b.paymentMethod = :pm "
                + " and b.billType=:btp1 "
                + " and b.billType=:btp2"
                + " and b.billType=:btp3"
                + " and b.billType=:btp4"
                + " and b.billType=:btp5";

        HashMap temMap = new HashMap();
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("pm", paymentMethod);
        temMap.put("btp1", BillType.OpdBill);
        temMap.put("btp2", BillType.PaymentBill);
        temMap.put("btp3", BillType.AgentPaymentReceiveBill);
        temMap.put("btp4", BillType.CashRecieveBill);
        temMap.put("btp5", BillType.PettyCash);
        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public long billItemCount(Bill bill, Item i, Date fromDate, Date toDate, Institution institution) {

        Map temMap = new HashMap();
        String sql;

        sql = "select count(bi) FROM BillItem bi "
                + " where bi.bill.institution=:ins "
                + " and bi.item=:itm"
                + " and (bi.bill.paymentMethod = :pm1 "
                + " or bi.bill.paymentMethod = :pm2"
                + " or bi.bill.paymentMethod = :pm3"
                + " or bi.bill.paymentMethod = :pm4) "
                + " and bi.bill.billType=:btp "
                + " and type(bi.bill)=:billClass "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " order by bi.item.name";

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", institution);
        temMap.put("itm", i);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        temMap.put("billClass", bill.getClass());
        temMap.put("btp", BillType.OpdBill);

        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void updateInwardDipositList(PatientEncounter patientEncounter, Bill bill) {
        if (patientEncounter != null && bill != null) {
            if (patientEncounter.isPaymentFinalized() && patientEncounter.getFinalBill() != null) {
                bill.setForwardReferenceBill(patientEncounter.getFinalBill());
                getBillFacade().edit(bill);

                patientEncounter.getFinalBill().getBackwardReferenceBills().add(bill);
                getBillFacade().edit(patientEncounter.getFinalBill());
            }
        }

    }

    public Bill fetchByForwardBill(Bill forwardBill, SurgeryBillType surgeryBillType) {
        String sql = "Select bf from Bill bf "
                + " where bf.cancelled=false "
                + " and bf.retired=false"
                + " and bf.surgeryBillType=:srgBtp "
                + " and bf.forwardReferenceBill=:bill ";
        HashMap hm = new HashMap();
        hm.put("bill", forwardBill);
        hm.put("srgBtp", surgeryBillType);
        Bill bill = getBillFacade().findFirstByJpql(sql, hm);

        return bill;
    }

    public double getTotalByBillFee(BillItem billItem) {
        String sql = "Select sum(bf.feeValue) from BillFee bf where "
                + " bf.retired=false and bf.billItem=:bItm";
        HashMap hm = new HashMap();
        hm.put("bItm", billItem);
        return getBillFeeFacade().findDoubleByJpql(sql, hm);
    }

    public List<EncounterComponent> getEncounterComponents(Bill bi) {
        String sql = "Select enc from EncounterComponent enc "
                + " where enc.billItem.bill=:bill";
        HashMap hm = new HashMap();
        hm.put("bill", bi);

        return getEncounterComponentFacade().findByJpql(sql, hm);

    }

    public List<EncounterComponent> getEncounterComponentsWithRetired(Bill bi) {
        String sql = "Select enc from EncounterComponent enc "
                + " where enc.billItem.bill=:bill"
                + " and enc.retired=false ";
        HashMap hm = new HashMap();
        hm.put("bill", bi);

        return getEncounterComponentFacade().findByJpql(sql, hm);

    }

    public BillItem fetchFirstBillItem(Bill b) {
        String sql = "Select b From BillItem b where "
                + " b.retired=false "
                + " and b.bill=:bill";
        HashMap hm = new HashMap();
        hm.put("bill", b);

        return getBillItemFacade().findFirstByJpql(sql, hm);
    }

    public double getTotalByBillItem(Bill bill) {
        String sql = "Select sum(bf.netValue) from BillItem bf where "
                + " bf.retired=false and bf.bill=:bill";
        HashMap hm = new HashMap();
        hm.put("bill", bill);
        return getBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public void setBillFees(BillFee bf, boolean foreign, PaymentMethod paymentMethod, PaymentScheme paymentScheme, Institution institution, PriceMatrix priceMatrix) {

        boolean discountAllowed = false;

        if (bf == null) {
            return;
        }

        if (bf.getBillItem() != null && bf.getBillItem().getItem() != null) {
            discountAllowed = bf.getBillItem().getItem().isDiscountAllowed();
        } else {
        }

        double discount = 0;

        if (priceMatrix != null) {
            discount = priceMatrix.getDiscountPercent();
        } else {
        }

        if (!discountAllowed) {
            bf.setFeeValueBoolean(foreign);
        } else if (discountAllowed
                && institution != null
                && institution.getLabBillDiscount() > 0.0) {
            bf.setFeeValueForCreditCompany(foreign, institution.getLabBillDiscount());
        } else {
            bf.setFeeValueForeignAndDiscount(foreign, discount);
            bf.setPriceMatrix(priceMatrix);
        }

    }

    public void setBillFees(BillFee bf, boolean foreign, PaymentMethod paymentMethod, MembershipScheme membershipScheme, Item item, PriceMatrix priceMatrix) {

        boolean discountAllowed = item.isDiscountAllowed();

        if (discountAllowed == false) {
            bf.setFeeValueBoolean(foreign);
        } else if (priceMatrix != null) {
            bf.setFeeValueForeignAndDiscount(foreign, priceMatrix.getDiscountPercent());
            bf.setPriceMatrix(priceMatrix);
        } else {
            bf.setFeeValueForeignAndDiscount(foreign, 0);
        }
    }

    public List<ItemFee> getItemFee(BillItem billItem) {

        String sql;
        sql = "Select f from ItemFee f"
                + " where f.retired=false "
                + " and f.item=:itm";
        HashMap hm = new HashMap();
        hm.put("itm", billItem.getItem());
        return getItemFeeFacade().findByJpql(sql, hm);
    }

    public ItemFee getItemFee(BillItem billItem, FeeType feeType) {

        String sql;
        sql = "Select f from ItemFee f"
                + " where f.retired=false "
                + " and f.item=:itm"
                + " and f.feeType=:ftp";
        HashMap hm = new HashMap();
        hm.put("itm", billItem.getItem());
        hm.put("ftp", feeType);
        return getItemFeeFacade().findFirstByJpql(sql, hm);
    }

//    public Fee getFee(FeeType feeType) {
//        HashMap hm = new HashMap();
//        String sql = "Select f from Fee f where f.retired=false and f.FeeType=:nm";
//        hm.put("nm", FeeType.Matrix);
//        return getFeeFacade().findFirstByJpql(sql, hm, TemporalType.TIMESTAMP);
//    }
    public BillFee createBillFee(BillItem billItem, Fee i) {
        BillFee f;
        f = new BillFee();
        f.setFee(i);
        f.setFeeValue(i.getFee());
        f.setFeeGrossValue(i.getFee());
        f.setDepartment(billItem.getItem().getDepartment());
        f.setBillItem(billItem);

        f.setCreatedAt(new Date());

        if (billItem.getItem().getDepartment() != null) {
            f.setDepartment(billItem.getItem().getDepartment());
        } else {
            f.setDepartment(billItem.getBill().getDepartment());
        }
        if (billItem.getItem().getInstitution() != null) {
            f.setInstitution(billItem.getItem().getInstitution());
        } else {
            f.setInstitution(billItem.getBill().getDepartment().getInstitution());
        }
        if (i.getStaff() != null) {
            f.setStaff(i.getStaff());
        } else {
            f.setStaff(null);
        }
        f.setSpeciality(i.getSpeciality());

        return f;

    }

    public void saveEncounterComponents(List<Bill> bills, Bill batchBill, WebUser user) {
        for (BillFee bf : getBillFeeFromBills(bills)) {
            saveEncounterComponent(bf, batchBill, user);
        }

    }

    public void saveEncounterComponents(Bill bill, Bill batchBill, WebUser user) {
        for (BillFee bf : getBillFee(bill)) {
            saveEncounterComponent(bf, batchBill, user);
        }

    }

    public void setSurgeryData(Bill bill, Bill batchBill, SurgeryBillType surgeryBillType) {
        if (batchBill == null) {
            return;
        }

        bill.setForwardReferenceBill(batchBill);
        bill.setSurgeryBillType(surgeryBillType);

    }

    public void saveEncounterComponent(BillFee bf, Bill batchBill, WebUser user) {
        EncounterComponent ec = new EncounterComponent();
        ec.setPatientEncounter(batchBill.getPatientEncounter());
        ec.setChildEncounter(batchBill.getProcedure());
        ec.setBillFee(bf);
        ec.setBillItem(bf.getBillItem());
        ec.setCreatedAt(Calendar.getInstance().getTime());
        ec.setCreater(user);
        ec.setPatientEncounter(batchBill.getProcedure());
        if (ec.getBillFee() != null) {
            ec.setStaff(ec.getBillFee().getStaff());
        }

        if (ec.getId() == null) {
            getEncounterComponentFacade().create(ec);
        }

    }

    public void updateBatchBill(Bill b) {

        if (b == null) {
            return;
        }

        double value = getTotalByBill(b);
        b.setTotal(value);

        getBillFacade().edit(b);
    }

    private double getTotalByBill(Bill b) {
        String sql = "Select sum(bf.netTotal) from Bill bf where "
                + " bf.retired=false and bf.forwardReferenceBill=:bill";
        HashMap hm = new HashMap();
        hm.put("bill", b);
        return getBillFacade().findDoubleByJpql(sql, hm);
    }

    public Double[] fetchBillItemValues(Bill b) {
        String sql = "Select sum(bf.grossValue),sum(bf.discount),sum(bf.netValue),sum(bf.vat) "
                + " from BillItem bf where "
                + " bf.retired=false "
                + " and bf.bill=:bill ";
        HashMap hm = new HashMap();
        BillFee bf;
        hm.put("bill", b);
        Object[] obj = getBillFacade().findAggregateModified(sql, hm, TemporalType.TIMESTAMP);

        if (obj == null) {
            Double[] dbl = new Double[4];
            dbl[0] = 0.0;
            dbl[1] = 0.0;
            dbl[2] = 0.0;
            dbl[3] = 0.0;
            return dbl;
        }

        Double[] dbl = Arrays.copyOf(obj, obj.length, Double[].class);

        return dbl;
    }

    public Double[] fetchBillFeeValues(Bill b) {
        String sql = "Select sum(bf.feeGrossValue),sum(bf.feeDiscount),sum(bf.feeValue),sum(bf.feeVat) "
                + " from BillFee bf where "
                + " bf.retired=false "
                + " and bf.bill=:bill ";
        HashMap hm = new HashMap();
        hm.put("bill", b);
        Object[] obj = getBillFacade().findAggregateModified(sql, hm, TemporalType.TIMESTAMP);

        if (obj == null) {
            Double[] dbl = new Double[3];
            dbl[0] = 0.0;
            dbl[1] = 0.0;
            dbl[2] = 0.0;
//            dbl[3] = 0.0;
            return dbl;
        }

        Double[] dbl = Arrays.copyOf(obj, obj.length, Double[].class);

        return dbl;
    }

    public void setPaymentMethodData(Bill b, PaymentMethod paymentMethod, PaymentMethodData paymentMethodData) {

        if (paymentMethod.equals(PaymentMethod.Cheque)) {
            b.setBank(paymentMethodData.getCheque().getInstitution());
            b.setChequeRefNo(paymentMethodData.getCheque().getNo());
            b.setChequeDate(paymentMethodData.getCheque().getDate());
        }
        if (paymentMethod.equals(PaymentMethod.Slip)) {
            b.setBank(paymentMethodData.getSlip().getInstitution());
            b.setChequeDate(paymentMethodData.getSlip().getDate());
            //   b.setComments(paymentMethodData.getSlip().getComment());
        }

        if (paymentMethod.equals(PaymentMethod.Card)) {
            b.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
            b.setBank(paymentMethodData.getCreditCard().getInstitution());
        }

        if (paymentMethod.equals(PaymentMethod.OnlineSettlement)) {
            b.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
            b.setBank(paymentMethodData.getCreditCard().getInstitution());
        }

    }

    public ServiceSessionBean getServiceSessionBean() {
        return serviceSessionBean;
    }

    public void setServiceSessionBean(ServiceSessionBean serviceSessionBean) {
        this.serviceSessionBean = serviceSessionBean;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public PackageFeeFacade getPackageFeeFacade() {
        return packageFeeFacade;
    }

    public void setPackageFeeFacade(PackageFeeFacade packageFeeFacade) {
        this.packageFeeFacade = packageFeeFacade;
    }

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    public List<BillComponent> billComponentsFromBillEntries(List<BillEntry> billEntries) {
        List<BillComponent> bcs = new ArrayList<>();
        for (BillEntry be : billEntries) {
            for (BillComponent bc : be.getLstBillComponents()) {
                if (bc != null) {
                    bcs.add(bc);
                }
            }
        }
        return bcs;
    }

    public List<Bill> billsForTheDay(Date fromDate, Date toDate, BillType type) {
        List<Bill> lstBills;
        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where b.createdAt is not null and b.billType = :billType and b.createdAt between :fromDate and :toDate and b.retired=false order by b.insId desc  ";

        temMap.put("billType", type);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        lstBills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 100);

        if (lstBills == null) {
            lstBills = new ArrayList<>();
        }
        return lstBills;
    }

    public List<Bill> billsForTheDay2(Date fromDate, Date toDate, BillType type) {
        List<Bill> lstBills;
        String sql;
        Map temMap = new HashMap();
        sql = "select b from PreBill b where b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate and b.retired=false order by b.id desc ";

        temMap.put("billType", type);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        lstBills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        if (lstBills == null) {
            lstBills = new ArrayList<>();
        }
        return lstBills;
    }

    public List<Bill> billsForTheDayNotPaid(Date fromDate, Date toDate, BillType type) {
        List<Bill> lstBills;
        String sql;
        Map temMap = new HashMap();
        sql = "select b from PreBill b where b.billType = :billType and b.referenceBill is null "
                + " and b.createdAt between :fromDate and :toDate and b.retired=false order by b.id desc ";

        temMap.put("billType", type);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        lstBills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        if (lstBills == null) {
            lstBills = new ArrayList<>();
        }
        return lstBills;
    }

    public List<Bill> billsForTheDayNotPaid(BillType type, Department department) {
        List<Bill> lstBills;
        String sql;
        Map temMap = new HashMap();
        sql = "select b from PreBill b "
                + " where b.billType = :billType "
                + " and b.department=:dep "
                + " and b.referenceBill is null "
                + " and b.backwardReferenceBill is null "
                + " and b.forwardReferenceBill is null "
                + " and b.billedBill is null "
                + " and b.retired=false "
                + " and b.netTotal!=0 "
                + " order by b.id desc ";

        temMap.put("billType", type);
        temMap.put("dep", department);

        lstBills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        if (lstBills == null) {
            lstBills = new ArrayList<>();
        }
        return lstBills;
    }

    public List<Bill> billsRefundForTheDay(Date fromDate, Date toDate, BillType type) {
        List<Bill> lstBills;
        String sql;
        Map temMap = new HashMap();
        sql = "select b from RefundBill b where b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate and b.retired=false order by b.id desc ";

        temMap.put("billType", type);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        lstBills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        if (lstBills == null) {
            lstBills = new ArrayList<>();
        }
        return lstBills;
    }

    public List<Bill> billsForTheDay(Date fromDate, Date toDate, Institution ins, BillType type) {
        List<Bill> lstBills;
        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where b.billType = :billType and b.institution.id=" + ins.getId() + " and b.createdAt between :fromDate and :toDate and b.retired=false order by b.id desc  ";

        temMap.put("billType", type);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        lstBills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 100);

        if (lstBills == null) {
            lstBills = new ArrayList<>();
        }
        return lstBills;
    }

    public List<Bill> billsForTheDayForLazy(Date fromDate, Date toDate, Institution ins, BillType type) {
        List<Bill> lstBills;
        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where b.billType = :billType and b.institution.id=" + ins.getId() + " and b.createdAt between :fromDate and :toDate and b.retired=false order by b.id desc  ";

        temMap.put("billType", type);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        lstBills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        if (lstBills == null) {
            lstBills = new ArrayList<>();
        }
        return lstBills;
    }

    public List<Bill> billsFromSearch(String searchStr, Date fromDate, Date toDate, BillType type) {
        List<Bill> lstBills;
        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where"
                + " b.billType = :billType and b.retired=false"
                + " and  b.createdAt between :fromDate and :toDate"
                + " and ((b.patient.person.name) like '%" + searchStr.toUpperCase() + "%' "
                + " or (b.patient.person.phone) like '%" + searchStr.toUpperCase() + "%' "
                + " or (b.insId) like '%" + searchStr.toUpperCase() + "%') order by b.insId desc  ";
        temMap.put("billType", type);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        lstBills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        if (lstBills == null) {
            lstBills = new ArrayList<>();
        }
        return lstBills;
    }

    public List<Bill> billsFromSearch2(String searchStr, Date fromDate, Date toDate, BillType type) {
        List<Bill> lstBills;
        String sql;
        Map temMap = new HashMap();
        sql = "select b from PreBill b where"
                + " b.billType = :billType and b.retired=false"
                + " and  b.createdAt between :fromDate and :toDate"
                + " and ((b.patient.person.name) like '%" + searchStr.toUpperCase() + "%' "
                + " or (b.patient.person.phone) like '%" + searchStr.toUpperCase() + "%' "
                + " or (b.insId) like '%" + searchStr.toUpperCase() + "%') order by b.insId desc  ";
        temMap.put("billType", type);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        lstBills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        //System.err.println("Search : " + sql);
        if (lstBills == null) {
            lstBills = new ArrayList<>();
        }
        return lstBills;
    }

    public List<Bill> billsFromSearch(String searchStr, Date fromDate, Date toDate, Institution ins, BillType type) {
        List<Bill> lstBills;
        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where b.billType = :billType and b.institution.id=" + ins.getId() + " and b.retired=false and  b.createdAt between :fromDate "
                + " and :toDate and ((b.patient.person.name) like '%" + searchStr.toUpperCase() + "%'  or (b.patient.person.phone) like '%" + searchStr.toUpperCase() + "%'  or (b.insId) like '%" + searchStr.toUpperCase() + "%') order by b.id desc  ";
        temMap.put("billType", type);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        lstBills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        if (lstBills == null) {
            lstBills = new ArrayList<>();
        }
        return lstBills;
    }

    public List<Bill> billsFromSearchForUser(String searchStr, Date fromDate, Date toDate, WebUser user, BillType type) {
        List<Bill> lstBills;
        String sql;
        Map temMap = new HashMap();
        if (searchStr == null || searchStr.trim().equals("")) {
            sql = "select b from BilledBill b where b.billType = :billType and b.retired=false and  b.createdAt between :fromDate and :toDate and b.creater.id = " + user.getId() + " order by b.id desc  ";
        } else {
            sql = "select b from BilledBill b where b.billType = :billType and b.retired=false and  b.createdAt between :fromDate and :toDate and  b.creater.id = " + user.getId() + " and ((b.patient.person.name) like '%" + searchStr.toUpperCase() + "%'  or (b.patient.person.phone) like '%" + searchStr.toUpperCase() + "%'  or (b.insId) like '%" + searchStr.toUpperCase() + "%') order by b.id desc  ";
        }

        temMap.put("billType", type);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        //////System.out.println("sql ");
        lstBills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        if (lstBills == null) {
            lstBills = new ArrayList<>();
        }
        return lstBills;
    }

    public List<Bill> billsFromSearchForUser(String searchStr, Date fromDate, Date toDate, WebUser user, Institution ins, BillType type) {
        List<Bill> lstBills;
        String sql;
        Map temMap = new HashMap();
        if (searchStr == null || searchStr.trim().equals("")) {
            sql = "select b from BilledBill b where b.billType = :billType and b.retired=false and b.institution.id=" + ins.getId() + " and b.createdAt between :fromDate and :toDate and b.creater.id = " + user.getId() + " order by b.id desc  ";
        } else {
            sql = "select b from BilledBill b where b.billType = :billType and b.retired=false and b.institution.id=" + ins.getId() + " and b.createdAt between :fromDate and :toDate and  b.creater.id = " + user.getId() + " and ((b.patient.person.name) like '%" + searchStr.toUpperCase() + "%'  or (b.patient.person.phone) like '%" + searchStr.toUpperCase() + "%'  or (b.insId) like '%" + searchStr.toUpperCase() + "%') order by b.id desc  ";
        }
        temMap.put("billType", type);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        //////System.out.println("sql ");
        lstBills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        if (lstBills == null) {
            lstBills = new ArrayList<>();
        }
        return lstBills;
    }

    public List<BillFee> billFeesFromBillEntries(List<BillEntry> billEntries) {
        List<BillFee> bcs = new ArrayList<>();
        for (BillEntry be : billEntries) {
            for (BillFee bc : be.getLstBillFees()) {
                bcs.add(bc);
            }
        }
        return bcs;
    }

    public Double billTotalFromBillEntries(List<BillEntry> billEntries) {
        Double bcs = 0.0;
        for (BillEntry be : billEntries) {
            for (BillFee bc : be.getLstBillFees()) {
                bcs = bcs + bc.getFeeValue();
            }
        }
        return bcs;
    }

    public List<BillSession> billSessionsFromBillEntries(List<BillEntry> billEntries) {
        List<BillSession> bcs = new ArrayList<>();
        for (BillEntry be : billEntries) {
            for (BillSession bc : be.getLstBillSessions()) {
                bcs.add(bc);
            }
        }
        return bcs;
    }

    public List<BillItem> billItemsFromBillEntries(List<BillEntry> billEntries) {
        List<BillItem> bcs = new ArrayList<>();
        for (BillEntry be : billEntries) {
            BillItem bi = be.getBillItem();
            double ft = 0;
            for (BillFee bf : be.getLstBillFees()) {
                ft = +bf.getFeeValue();
            }
            bi.setRate(ft);
            bi.setGrossValue(ft);
            bi.setNetValue(ft);
            bcs.add(be.getBillItem());
        }
        return bcs;
    }

    public List<Item> itemFromPackage(Item packege) {
        String sql = "Select i from PackageItem p join p.item i where p.retired=false and p.packege.id = " + packege.getId();
        List<Item> packageItems = getItemFacade().findByJpql(sql);

        return packageItems;
    }

    public List<Item> itemFromMedicalPackage(Item packege) {

        String sql = "Select i from MedicalPackageItem p join p.item i where p.retired=false and p.packege.id = " + packege.getId();
        List<Item> packageItems = getItemFacade().findByJpql(sql);
        if (packageItems == null) {
            packageItems = new ArrayList<>();
            JsfUtil.addErrorMessage("No Items inside Package");
        }

        return packageItems;
    }

    public int calculateNumberOfBillsPerOrder(List<BillEntry> billEntrys) {
        Set<Department> deptSet = new HashSet();
        for (BillEntry be : billEntrys) {
            deptSet.add(be.getBillItem().getItem().getDepartment());
        }
        return deptSet.size();
    }

    public int checkDepartment(List<BillEntry> billEntries) {
        OpdBillingStrategy strategy = sessionController.getDepartmentPreference().getOpdBillingStrategy();

        if (strategy == OpdBillingStrategy.SINGLE_BILL_FOR_ALL_ORDERS) {
            return 1;
        }

        Set<Department> deptSet = new HashSet<>();
        Map<Department, Set<Category>> deptCategoryMap = new HashMap<>();

        for (BillEntry be : billEntries) {
            Department dept = be.getBillItem().getItem().getDepartment();
            Category cat = be.getBillItem().getItem().getCategory();

            deptSet.add(dept);

            if (strategy == OpdBillingStrategy.ONE_BILL_PER_DEPARTMENT_AND_CATEGORY) {
                deptCategoryMap.computeIfAbsent(dept, k -> new HashSet<>()).add(cat);
            }
        }

        if (strategy == OpdBillingStrategy.ONE_BILL_PER_DEPARTMENT) {
            return deptSet.size();
        } else if (strategy == OpdBillingStrategy.ONE_BILL_PER_DEPARTMENT_AND_CATEGORY) {
            int count = 0;
            for (Set<Category> categories : deptCategoryMap.values()) {
                count += categories.size();
            }
            return count;
        }

        return 0; // Fallback case, should not reach here.
    }

    public void checkBillItemFeesInitiated(Bill b) {
        if (b == null) {
            return;
        }
        if (b.getBillItems() == null) {
            b.setBillItems(fillBillItems(b));
        }
        if (b.getBillItems() == null || b.getBillItems().isEmpty()) {
            return;
        }
        for (BillItem bi : b.getBillItems()) {
            if (bi.getBillFees() == null || bi.getBillFees().isEmpty()) {
                bi.setBillFees(fillBillItemFees(bi));
            }
        }
    }

    public List<BillItem> fillBillItems(Bill b) {
        String j = "Select bi "
                + " from BillItem bi "
                + " where bi.bill=:b "
                + " and bi.retired=:ret ";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("b", b);
        return billItemFacade.findByJpql(j, m);
    }

    public List<BillFee> fillBillItemFees(BillItem bi) {
        String j = "Select bf "
                + " from BillFee bf "
                + " where bf.billItem=:bi "
                + " and bf.retired=:ret ";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("bi", bi);
        return billFeeFacade.findByJpql(j, m);
    }

    public BillItem saveBillItem(Bill b, BillEntry e, WebUser wu) {
        e.getBillItem().setCreatedAt(new Date());
        e.getBillItem().setCreater(wu);
        e.getBillItem().setBill(b);

        if (e.getBillItem().getId() == null) {
            getBillItemFacade().create(e.getBillItem());
        }

        saveBillComponent(e, b, wu);
        saveBillFee(e, b, wu);

        return e.getBillItem();
    }

    public BillItem saveBillItem(Bill b, BillEntry e, WebUser wu, Payment p) {
        e.getBillItem().setCreatedAt(new Date());
        e.getBillItem().setCreater(wu);
        e.getBillItem().setBill(b);

        if (e.getBillItem().getId() == null) {
            getBillItemFacade().create(e.getBillItem());
        }

        saveBillComponent(e, b, wu);
        saveBillFee(e, b, wu, p);

        return e.getBillItem();
    }

    @Inject
    BillController billController;
    @Inject
    SessionController sessionController;

    public void calculateBillItems(Bill bill, List<BillEntry> billEntrys) {
        double staff = 0.0;
        double ins = 0.0;
        double tot = 0.0;
        double dis = 0;
        double net = 0;
        double vat = 0.0;

        for (BillEntry e : billEntrys) {
            for (BillFee bf : e.getLstBillFees()) {
                tot += bf.getFeeGrossValue();
                net += bf.getFeeValue();
                dis += bf.getFeeDiscount();
                vat += bf.getFeeVat();

                if (bf.getFee().getFeeType() != FeeType.Staff) {
                    ins += bf.getFeeValue();
                } else {
                    staff += bf.getFeeValue();
                }
                if (bf.getId() == null || bf.getId() == 0) {
                    getBillFeeFacade().create(bf);
                } else {
                    getBillFeeFacade().edit(bf);
                }
            }
        }

        bill.setStaffFee(staff);
        bill.setPerformInstitutionFee(ins);

//        bill.setTotal(tot);
//        bill.setNetTotal(net);
//        bill.setDiscount(dis);
        if (sessionController.getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {
            ////System.out.println("cashRemain" + billController.getCashRemain());
            if (billController.getCashRemain() != 0) {
                if (tot > billController.getCashRemain()) {
                    bill.setBalance(tot - billController.getCashRemain());
                    bill.setTotal(tot);
                    bill.setNetTotal(billController.getCashRemain());
                    bill.setDiscount(dis);
                    bill.setCashPaid(billController.getCashRemain());
                    billController.setCashRemain(0.0);
                } else {
                    bill.setBalance(0.0);
                    bill.setTotal(tot);
                    bill.setNetTotal(net);
                    bill.setDiscount(dis);
                    bill.setCashPaid(tot);
                    billController.setCashRemain(billController.getCashRemain() - tot);
                }

            } else {
                bill.setBalance(tot);
                bill.setTotal(tot);
                bill.setNetTotal(0.0);
                bill.setCashPaid(0.0);
                bill.setDiscount(dis);
            }
            ////System.out.println(".................");

        } else {
            bill.setGrantTotal(tot);
            bill.setTotal(tot);
            bill.setNetTotal(net);
            bill.setBillTotal(net);
            bill.setDiscount(dis);
        }

        bill.setVat(vat);
        bill.setVatPlusNetTotal(vat + bill.getNetTotal());

        getBillFacade().edit(bill);
    }

    public void calculateBillItems(Bill bill, BillEntry e) {
        double s = 0.0;
        double i = 0.0;
        double tot = 0.0;
        double net = 0.0;

        for (BillFee bf : e.getLstBillFees()) {
            tot += bf.getFee().getFee();
            net += bf.getFeeValue();
            if (bf.getFee().getStaff() == null) {
                i = i + bf.getFeeValue();
            } else {
                s = s + bf.getFeeValue();
            }
            if (bf.getId() == null || bf.getId() == 0) {
                getBillFeeFacade().create(bf);
            } else {
                getBillFeeFacade().edit(bf);
            }
        }

        bill.setStaffFee(s);
        bill.setPerformInstitutionFee(i);

        if (tot > net) {
            bill.setTotal(tot);
            bill.setDiscount(tot - net);
            bill.setDiscountPercent(((tot - net) / tot) * 100);
            bill.setNetTotal(net);
        } else {
            bill.setTotal(net);
            bill.setDiscount(0.0);
            bill.setNetTotal(net);
        }

        getBillFacade().edit(bill);
    }

    public List<BillItem> saveBillItems(Bill b, List<BillEntry> billEntries, WebUser wu) {
        List<BillItem> list = new ArrayList<>();
        for (BillEntry e : billEntries) {
            e.getBillItem().setCreatedAt(new Date());
            e.getBillItem().setCreater(wu);
            e.getBillItem().setBill(b);
            if (e.getBillItem().getId() == null) {
                getBillItemFacade().create(e.getBillItem());
            }

            saveBillComponent(e, b, wu);
            saveBillFee(e, b, wu);

            updateBillItemByBillFee(e.getBillItem());

            list.add(e.getBillItem());
        }

        updateBillByBillFee(b);

        return list;
    }

    public void updateBillByBillFee(Bill b) {
        String sql = "SELECT sum(b.feeGrossValue)"
                + " FROM BillFee b "
                + " WHERE b.retired=false"
                + " and b.bill=:bill ";
        HashMap hm = new HashMap();
        hm.put("bill", b);
        double val = getBillFeeFacade().findDoubleByJpql(sql, hm);

        b.setTotal(val);

        sql = "SELECT sum(b.feeValue)"
                + " FROM BillFee b "
                + " WHERE b.retired=false"
                + " and b.bill=:bill ";
        hm = new HashMap();
        hm.put("bill", b);
        val = getBillFeeFacade().findDoubleByJpql(sql, hm);

        b.setNetTotal(val);

        getBillFacade().edit(b);
    }

    public void saveBillItems(Bill b, BillEntry e, WebUser wu) {

        // BillItem temBi = e.getBillItem();
        e.getBillItem().setCreatedAt(new Date());
        e.getBillItem().setCreater(wu);
        e.getBillItem().setBill(b);

        if (e.getBillItem().getId() == null) {
            getBillItemFacade().create(e.getBillItem());
        }
        ////////System.out.println("Saving Bill Item : " + e.getBillItem().getItem().getName());

        saveBillComponent(e, b, wu);
        saveBillFee(e, b, wu);
//            if (b.getBillType() != BillType.InwardBill && e.getBillItem() != null) {
//                
//                e.getBillItem().setBillSession(getServiceSessionBean().saveBillSession(e.getBillItem()));
//            }
        getBillItemFacade().edit(e.getBillItem());

    }

    public List<BillFee> saveBillFee(BillEntry e, Bill b, WebUser wu) {
        List<BillFee> list = new ArrayList<>();
        double ccfee = 0.0;
        double woccfee = 0.0;
        for (BillFee bf : e.getLstBillFees()) {
            bf.setCreatedAt(Calendar.getInstance().getTime());
            bf.setCreater(wu);
            bf.setBillItem(e.getBillItem());
            bf.setPatienEncounter(b.getPatientEncounter());
            bf.setPatient(b.getPatient());

            bf.setBill(b);

            if (bf.getId() == null) {
                getBillFeeFacade().create(bf);
            }
            if (bf.getFee().getFeeType() == FeeType.CollectingCentre) {
                ccfee += bf.getFeeValue();
            } else {
                woccfee += bf.getFeeValue();
            }
            list.add(bf);
        }
        e.getBillItem().setTransCCFee(ccfee);
        e.getBillItem().setTransWithOutCCFee(woccfee);

        return list;
    }

    public List<BillFee> saveBillFee(BillEntry e, Bill b, WebUser wu, Payment p) {
        List<BillFee> list = new ArrayList<>();
        for (BillFee bf : e.getLstBillFees()) {
            bf.setCreatedAt(Calendar.getInstance().getTime());
            bf.setCreater(wu);
            bf.setBillItem(e.getBillItem());
            bf.setPatienEncounter(b.getPatientEncounter());
            bf.setPatient(b.getPatient());

            bf.setBill(b);

            if (bf.getId() == null) {
                getBillFeeFacade().create(bf);
            }
            createBillFeePaymentAndPayment(bf, p);
            list.add(bf);
        }

        return list;
    }

    @Inject
    InwardBeanController inwardBean;

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    public void updateBillItemByBillFee(BillItem billItem) {
        String sql = "SELECT sum(b.feeGrossValue) "
                + " FROM BillFee b "
                + " WHERE b.retired=false "
                + " and b.billItem=:bItm ";
        HashMap hm = new HashMap();
        hm.put("bItm", billItem);
        double val = getBillFeeFacade().findDoubleByJpql(sql, hm);

        billItem.setGrossValue(val);

        sql = "SELECT sum(b.feeValue) "
                + " FROM BillFee b "
                + " WHERE b.retired=false "
                + " and b.billItem=:bItm ";
        hm = new HashMap();
        hm.put("bItm", billItem);
        val = getBillFeeFacade().findDoubleByJpql(sql, hm);

        billItem.setNetValue(val);

        sql = "SELECT sum(b.feeMargin) "
                + " FROM BillFee b "
                + " WHERE b.retired=false "
                + " and b.billItem=:bItm ";
        hm = new HashMap();
        hm.put("bItm", billItem);
        val = getBillFeeFacade().findDoubleByJpql(sql, hm);

        billItem.setMarginValue(val);

        sql = "SELECT sum(b.feeDiscount) "
                + " FROM BillFee b "
                + " WHERE b.retired=false "
                + " and b.billItem=:bItm ";
        hm = new HashMap();
        hm.put("bItm", billItem);
        val = getBillFeeFacade().findDoubleByJpql(sql, hm);

        billItem.setDiscount(val);
//
//        billItem.setEditedAt(new Date());
//        billItem.setEditor(webUser);

        getBillItemFacade().edit(billItem);

    }

    private void savePatientInvestigation(BillEntry e, BillComponent bc, WebUser wu) {
        PatientInvestigation ptIx = new PatientInvestigation();

        ptIx.setCreatedAt(Calendar.getInstance().getTime());
        ptIx.setCreater(wu);

        ptIx.setBillItem(e.getBillItem());
        ptIx.setBillComponent(bc);
        ptIx.setPackege(bc.getPackege());
        ptIx.setApproved(Boolean.FALSE);
        ptIx.setCancelled(Boolean.FALSE);
        ptIx.setCollected(Boolean.FALSE);
        ptIx.setDataEntered(Boolean.FALSE);
        ptIx.setInvestigation((Investigation) bc.getItem());
        ptIx.setOutsourced(Boolean.FALSE);
        ptIx.setPatient(e.getBillItem().getBill().getPatient());

        if (e.getBillItem().getBill().getPatientEncounter() != null) {
            ptIx.setEncounter(e.getBillItem().getBill().getPatientEncounter());
        }

        ptIx.setPerformed(Boolean.FALSE);
        ptIx.setPrinted(Boolean.FALSE);
        ptIx.setPrinted(Boolean.FALSE);
        ptIx.setReceived(Boolean.FALSE);

        ptIx.setReceiveDepartment(e.getBillItem().getItem().getDepartment());
        ptIx.setApproveDepartment(e.getBillItem().getItem().getDepartment());
        ptIx.setDataEntryDepartment(e.getBillItem().getItem().getDepartment());
        ptIx.setPrintingDepartment(e.getBillItem().getItem().getDepartment());
        ptIx.setPerformDepartment(e.getBillItem().getItem().getDepartment());

        if (e.getBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("No Bill Item Selected");
        } else if (e.getBillItem().getItem().getDepartment() == null) {
            JsfUtil.addErrorMessage("Under administration, add a Department for this investigation " + e.getBillItem().getItem().getName());
        } else if (e.getBillItem().getItem().getDepartment().getInstitution() == null) {
            JsfUtil.addErrorMessage("Under administration, add an Institution for the department " + e.getBillItem().getItem().getDepartment());
        } else if (e.getBillItem().getItem().getDepartment().getInstitution() != wu.getInstitution()) {
            ptIx.setOutsourcedInstitution(e.getBillItem().getItem().getInstitution());
        }

        ptIx.setRetired(false);

        if (ptIx.getId() == null) {
            getPatientInvestigationFacade().create(ptIx);
        }

    }

    public void saveBillComponent(BillEntry e, Bill b, WebUser wu) {
        for (BillComponent bc : e.getLstBillComponents()) {

            bc.setCreatedAt(Calendar.getInstance().getTime());
            bc.setCreater(wu);

            bc.setDepartment(b.getDepartment());
            bc.setInstitution(b.getDepartment().getInstitution());

            bc.setBill(b);

            if (bc.getId() == null) {
                getBillComponentFacade().create(bc);
            }

            if (bc.getItem() instanceof Investigation) {
                savePatientInvestigation(e, bc, wu);
            }

        }
    }

    public List<BillComponent> billComponentsFromBillItem(BillItem billItem) {
        String sql;
        List<BillComponent> t = new ArrayList<>();
        BillComponent b;
        if (billItem.getItem() instanceof Packege) {
            sql = "Select i from PackageItem p join p.item i "
                    + " where p.packege.id = " + billItem.getItem().getId();
            List<Item> packageItems = getItemFacade().findByJpql(sql);
            for (Item i : packageItems) {
                b = new BillComponent();
                BillItem bit = new BillItem();
                b.setBillItem(bit);
                b.setCreatedAt(new Date());
                b.setItem(i);
                b.setName(i.getName());
                b.setPackege((Packege) billItem.getItem());
                b.setStaff(i.getStaff());
                b.setSpeciality(i.getSpeciality());
                t.add(b);
            }

        } else {
            b = new BillComponent();
            b.setBillItem(billItem);
            b.setCreatedAt(new Date());
            b.setItem(billItem.getItem());
            //////System.out.println("Bill Item is " + billItem.getItem());
            b.setName(billItem.getItem().getName());
            b.setPackege(null);
            b.setStaff(billItem.getItem().getStaff());
            b.setSpeciality(billItem.getItem().getSpeciality());
            t.add(b);
        }

        return t;
    }

    public Double billItemRate(BillEntry billEntry) {
        Double temTot = 0.0;
        for (BillFee f : billEntry.getLstBillFees()) {
            temTot += f.getFeeValue();
        }
        return temTot;
    }

    public List<BillSession> billSessionsfromBillItem(BillItem billItem) {
        //TODO: Create Logic
        return null;
    }

    public List<BillFee> billFeefromBillItemPackage(BillItem billItem, Item packege) {
        List<BillFee> t = new ArrayList<>();
        BillFee f;
        String sql;
        sql = "Select f from PackageFee f where f.retired=false and f.packege.id=" + packege.getId()
                + " and f.item.id = " + billItem.getItem().getId();
        List<PackageFee> packFee = getPackageFeeFacade().findByJpql(sql);
        for (Fee i : packFee) {
            f = new BillFee();
            f.setFee(i);
            f.setFeeValue(i.getFee());
            f.setFeeGrossValue(i.getFee());
            //      //////System.out.println("Fee Value is " + f.getFeeValue());
            // f.setBill(billItem.getBill());
            f.setBillItem(billItem);
            f.setCreatedAt(new Date());
            if (billItem.getItem().getDepartment() != null) {
                f.setDepartment(billItem.getItem().getDepartment());
            } else {
                //  f.setDepartment(billItem.getBill().getDepartment());
            }
            if (billItem.getItem().getInstitution() != null) {
                f.setInstitution(billItem.getItem().getInstitution());
            } else {
                //   f.setInstitution(billItem.getBill().getDepartment().getInstitution());
            }
            if (i.getStaff() != null) {
                f.setStaff(i.getStaff());
            } else {
                f.setStaff(null);
            }
            f.setSpeciality(i.getSpeciality());
            t.add(f);

        }
        return t;
    }

    public List<Fee> getMedicalPackageFee(Item packege, Item item) {
        String sql;
        sql = "Select f from MedicalPackageFee f where f.retired=false and f.packege.id=" + packege.getId() + " and f.item.id = " + item.getId();
        return getFeeFacade().findByJpql(sql);
    }

    public List<Fee> getPackageFee(Item packege, Item item) {
        String sql;
        sql = "Select f from PackageFee f where f.retired=false and f.packege.id=" + packege.getId() + " and f.item.id = " + item.getId();
        return getFeeFacade().findByJpql(sql);
    }

    public List<BillFee> billFeefromBillItemMedicalPackage(BillItem billItem, Item packege) {
        List<BillFee> t = new ArrayList<>();
        BillFee f;
        String sql;
        sql = "Select f from MedicalPackageFee f where f.retired=false and f.packege.id=" + packege.getId() + " and f.item.id = " + billItem.getItem().getId();
        List<PackageFee> packFee = getPackageFeeFacade().findByJpql(sql);
        for (Fee i : packFee) {
            f = new BillFee();
            f.setFee(i);
            f.setFeeValue(i.getFee());
            f.setFeeGrossValue(i.getFee());
            f.setBillItem(billItem);
            f.setCreatedAt(new Date());
            if (billItem.getItem().getDepartment() != null) {
                f.setDepartment(billItem.getItem().getDepartment());
            } else {
                //  f.setDepartment(billItem.getBill().getDepartment());
            }
            if (billItem.getItem().getInstitution() != null) {
                f.setInstitution(billItem.getItem().getInstitution());
            } else {
                //   f.setInstitution(billItem.getBill().getDepartment().getInstitution());
            }
            if (i.getStaff() != null) {
                f.setStaff(i.getStaff());
            } else {
                f.setStaff(null);
            }
            f.setSpeciality(i.getSpeciality());
            t.add(f);

        }
        return t;
    }

    public List<BillFee> billFeefromBillItem(BillItem billItem) {
        List<BillFee> t = new ArrayList<>();
        BillFee f;
        String sql;
        if (billItem.getItem() instanceof Packege) {
            sql = "Select i from PackageItem p join p.item i where p.retired=false and p.packege.id = " + billItem.getItem().getId();
            List<Item> packageItems = getItemFacade().findByJpql(sql);
            for (Item pi : packageItems) {
                sql = "Select f from PackageFee f where f.retired=false and f.packege.id = " + billItem.getItem().getId() + " and f.item.id = " + pi.getId();
                List<PackageFee> packFee = getPackageFeeFacade().findByJpql(sql);
                for (Fee i : packFee) {
                    f = new BillFee();
                    f.setFee(i);
                    f.setFeeValue(i.getFee());
                    f.setFeeGrossValue(i.getFee());
                    //  f.setBill(billItem.getBill());
                    f.setBillItem(billItem);
                    f.setCreatedAt(new Date());
                    if (pi.getDepartment() != null) {
                        if (i.getFeeType() == FeeType.CollectingCentre && collectingCentreBillController.getCollectingCentre() != null) {
                            f.setDepartment(departmentController.getDefaultDepatrment(collectingCentreBillController.getCollectingCentre()));
                        } else {
                            f.setDepartment(pi.getDepartment());
                        }

                    } else {
                        // f.setDepartment(billItem.getBill().getDepartment());
                    }
                    if (pi.getInstitution() != null) {
                        if (i.getFeeType() == FeeType.CollectingCentre && collectingCentreBillController.getCollectingCentre() != null) {
                            f.setInstitution(collectingCentreBillController.getCollectingCentre());
                        } else {
                            f.setInstitution(pi.getInstitution());
                        }

                    } else {
                        // f.setInstitution(billItem.getBill().getDepartment().getInstitution());
                    }
                    if (i.getStaff() != null) {
                        f.setStaff(i.getStaff());
                    } else {
                        f.setStaff(null);
                    }
                    f.setSpeciality(i.getSpeciality());
                    f.setStaff(i.getStaff());

                    if (f.getBillItem().getItem().isVatable()) {
                        if (!(f.getFee().getFeeType() == FeeType.CollectingCentre && collectingCentreBillController.getCollectingCentre() != null)) {
                            f.setFeeVat(f.getFeeValue() * f.getBillItem().getItem().getVatPercentage() / 100);
                        }
                    }

                    f.setFeeVatPlusValue(f.getFeeValue() + f.getFeeVat());

                    t.add(f);

                }
            }
        } else {
            sql = "Select f from ItemFee f where f.retired=false and f.item.id = " + billItem.getItem().getId();
            List<ItemFee> itemFee = getItemFeeFacade().findByJpql(sql);
            for (Fee i : itemFee) {
                f = new BillFee();
                f.setFee(i);
                f.setFeeValue(i.getFee() * billItem.getQty());
                f.setFeeGrossValue(i.getFee() * billItem.getQty());
                //////System.out.println("Fee Value is " + f.getFeeValue());
                // f.setBill(billItem.getBill());
                f.setBillItem(billItem);
                f.setCreatedAt(new Date());
                if (billItem.getItem().getDepartment() != null) {
                    if (i.getFeeType() == FeeType.CollectingCentre && collectingCentreBillController.getCollectingCentre() != null) {
                        f.setDepartment(departmentController.getDefaultDepatrment(collectingCentreBillController.getCollectingCentre()));
                    } else {
                        f.setDepartment(billItem.getItem().getDepartment());
                    }
                } else {
                    //  f.setDepartment(billItem.getBill().getDepartment());
                }
                if (billItem.getItem().getInstitution() != null) {
                    if (i.getFeeType() == FeeType.CollectingCentre && collectingCentreBillController.getCollectingCentre() != null) {
                        f.setInstitution(collectingCentreBillController.getCollectingCentre());
                    } else {
                        f.setInstitution(billItem.getItem().getInstitution());
                    }
                } else {
                    //   f.setInstitution(billItem.getBill().getDepartment().getInstitution());
                }
                if (i.getStaff() != null) {
                    f.setStaff(i.getStaff());
                } else {
                    f.setStaff(null);
                }
                f.setSpeciality(i.getSpeciality());

                if (f.getBillItem().getItem().isVatable()) {
                    if (!(f.getFee().getFeeType() == FeeType.CollectingCentre && collectingCentreBillController.getCollectingCentre() != null)) {
                        f.setFeeVat(f.getFeeValue() * f.getBillItem().getItem().getVatPercentage() / 100);
                    }
                }

                f.setFeeVatPlusValue(f.getFeeValue() + f.getFeeVat());

                t.add(f);
            }
        }
        return t;
    }

    public double totalFeeforItem(Item item) {
        List<BillFee> t = new ArrayList<>();
        Double bf = 0.0;
        BillFee f;
        String sql;
        if (item instanceof Packege) {
            sql = "Select i from PackageItem p join p.item i where i.retired=false and p.packege.id = " + item.getId();
            List<Item> packageItems = getItemFacade().findByJpql(sql);
            for (Item pi : packageItems) {
                sql = "Select f from PackageFee f where f.retired=false and f.packege.id = " + item.getId() + " and f.item.id = " + pi.getId();
                List<PackageFee> packFee = getPackageFeeFacade().findByJpql(sql);
                for (Fee i : packFee) {
                    bf = +i.getFee();
                }
            }
        } else {
            sql = "Select f from ItemFee f where f.retired=false and f.item.id = " + item.getId();
            List<ItemFee> itemFee = getItemFeeFacade().findByJpql(sql);
            for (Fee i : itemFee) {
                bf = +i.getFee();
            }
        }
        return bf;
    }

    public void createBillFeePaymentAndPayment(BillFee bf, Payment p) {
        BillFeePayment bfp = new BillFeePayment();
        bfp.setBillFee(bf);
        bfp.setAmount(bf.getSettleValue());
        bfp.setInstitution(bf.getBillItem().getItem().getInstitution());
        bfp.setDepartment(bf.getBillItem().getItem().getDepartment());
        bfp.setCreater(sessionController.getLoggedUser());
        bfp.setCreatedAt(new Date());
        bfp.setPayment(p);
        getBillFeePaymentFacade().create(bfp);
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade aItemFacade) {
        itemFacade = aItemFacade;
    }

    public PackegeFacade getPackegeFacade() {
        return packegeFacade;
    }

    public void setPackegeFacade(PackegeFacade aPackegeFacade) {
        packegeFacade = aPackegeFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade aBillItemFacade) {
        billItemFacade = aBillItemFacade;
    }

    public BillComponentFacade getBillComponentFacade() {
        return billComponentFacade;
    }

    public void setBillComponentFacade(BillComponentFacade aBillComponentFacade) {
        billComponentFacade = aBillComponentFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade aBillFeeFacade) {
        billFeeFacade = aBillFeeFacade;
    }

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade aBillSessionFacade) {
        billSessionFacade = aBillSessionFacade;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade aItemFeeFacade) {
        itemFeeFacade = aItemFeeFacade;
    }

    public FeeFacade getFeeFacade() {
        return feeFacade;
    }

    public void setFeeFacade(FeeFacade feeFacade) {
        this.feeFacade = feeFacade;
    }

    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }

    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
    }

    public String checkPaymentMethod(PaymentMethod paymentMethod, Institution institution, String string, Date date) {
        switch (paymentMethod) {
            case Cheque:
                if (institution == null || string == null || date == null) {
                    return "Please select Cheque Number,Bank and Cheque Date";
                }

            case Slip:
                if (institution == null || string == null || date == null) {
                    return "Please Fill Memo,Bank and Slip Date ";
                }

            case Card:
                if (institution == null || string == null) {
                    return "Please Fill Credit Card Number and Bank";
                }
                if (string.trim().length() < 16) {
                    return "Enter 16 Digit";
                }
            case Credit:
                if (institution == null) {
                    return "Please Select Credit Company";
                }

        }

        if (institution != null && paymentMethod != PaymentMethod.Credit) {
            return "Please Select Payment Scheme with Credit";
        }

        return "";

    }

    public List<BillFee> getBillFee(Bill b) {
        String sql = "Select bf From BillFee bf "
                + " where bf.retired=false"
                + " and bf.bill=:b ";

        HashMap hm = new HashMap();
        hm.put("b", b);
        return getBillFeeFacade().findByJpql(sql, hm);
    }

    public List<BillFee> getBillFee(BillItem b) {
        HashMap hm = new HashMap();
        String sql = "Select bf From BillFee bf "
                + " where bf.retired=false"
                + " and bf.billItem.id=" + b.getId();

        //   hm.put("b", b);
        List<BillFee> list = getBillFeeFacade().findByJpql(sql);
        return list;
    }

    public List<BillFee> getBillFeeFromBills(List<Bill> list) {
        List<BillFee> billFees = new ArrayList<>();
        for (Bill b : list) {
            billFees.addAll(getBillFee(b));
        }

        return billFees;
    }
    @EJB
    private EncounterComponentFacade encounterComponentFacade;

    public List<EncounterComponent> getEncounterBillComponents(BillItem billItem) {

        String sql = "SELECT b FROM EncounterComponent b "
                + " WHERE b.retired=false "
                + " and b.billItem=:b ";
        HashMap hs = new HashMap();
        hs.put("b", billItem);
        List<EncounterComponent> list = getEncounterComponentFacade().findByJpql(sql, hs);

        return list;
    }

    public EncounterComponentFacade getEncounterComponentFacade() {
        return encounterComponentFacade;
    }

    public void setEncounterComponentFacade(EncounterComponentFacade encounterComponentFacade) {
        this.encounterComponentFacade = encounterComponentFacade;
    }

    public CategoryFacade getCategoryFacade() {
        return categoryFacade;
    }

    public void setCategoryFacade(CategoryFacade categoryFacade) {
        this.categoryFacade = categoryFacade;
    }

    public AllowedPaymentMethodFacade getAllowedPaymentMethodFacade() {
        return allowedPaymentMethodFacade;
    }

    public void setAllowedPaymentMethodFacade(AllowedPaymentMethodFacade allowedPaymentMethodFacade) {
        this.allowedPaymentMethodFacade = allowedPaymentMethodFacade;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public BillFeePaymentFacade getBillFeePaymentFacade() {
        return billFeePaymentFacade;
    }

    public void setBillFeePaymentFacade(BillFeePaymentFacade billFeePaymentFacade) {
        this.billFeePaymentFacade = billFeePaymentFacade;
    }

}
