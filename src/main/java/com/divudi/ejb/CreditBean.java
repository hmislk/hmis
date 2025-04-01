/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.CountedServiceType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.*;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.PatientEncounterFacade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 * @author safrin
 */
@Stateless
public class CreditBean {

    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    PatientEncounterFacade patientEncounterFacade;

    public List<Bill> getCreditBills(Institution ins, BillType billType, Date fromDate, Date toDate, boolean lessThan) {
        String sql = "Select b From BilledBill b"
                + " where b.retired=false "
                + " and b.createdAt  between :frm and :to ";

        if (lessThan) {
            sql += " and (abs(b.netTotal)-abs(b.paidAmount))>:val ";
        } else {
            sql += " and (abs(b.netTotal)-abs(b.paidAmount))<:val ";
        }

        sql += " and b.cancelledBill is null  "
                + " and b.refundedBill is null"
                + " and b.creditCompany=:cc "
                + " and b.paymentMethod= :pm "
                + " and b.billType=:tp";

        HashMap hm = new HashMap();
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("cc", ins);
        hm.put("pm", PaymentMethod.Credit);
        hm.put("tp", billType);
        hm.put("val", 0.1);

        return (List<Bill>) getBillFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<Bill> getCreditBillsPharmacy(Institution ins, List<BillType> billTypes, Date fromDate, Date toDate, boolean lessThan) {
        String sql = "Select b From BilledBill b"
                + " where b.retired=false "
                + " and b.createdAt  between :frm and :to ";

        if (lessThan) {
            sql += " and (abs(b.netTotal)-abs(b.paidAmount))>:val ";
        } else {
            sql += " and (abs(b.netTotal)-abs(b.paidAmount))<:val ";
        }

        sql += " and b.cancelledBill is null  "
                + " and b.refundedBill is null "
                + " and b.toInstitution=:cc "
                + " and b.paymentMethod=:pm "
                + " and b.billType in :tps "
                + " and b.toStaff is null ";

        HashMap hm = new HashMap();
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("cc", ins);
        hm.put("pm", PaymentMethod.Credit);
        hm.put("tps", billTypes);
        hm.put("val", 0.1);

        return (List<Bill>) getBillFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<BillItem> getCreditBillItems(Institution ins, BillType billType, Date fromDate, Date toDate, boolean lessThan) {
        String sql = "Select bi From BillItem bi join  bi.bill b"
                + " where b.retired=false "
                + " and b.createdAt  between :frm and :to ";

        if (lessThan) {
            sql += " and (abs(b.netTotal)-abs(b.paidAmount))>:val ";
        } else {
            sql += " and (abs(b.netTotal)-abs(b.paidAmount))<:val ";
        }

        sql += " and b.cancelledBill is null  "
                + " and b.refundedBill is null"
                + " and b.creditCompany=:cc "
                + " and b.paymentMethod= :pm "
                + " and b.billType=:tp "
                + " and type(b)=:cla ";

        HashMap hm = new HashMap();
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("cc", ins);
        hm.put("pm", PaymentMethod.Credit);
        hm.put("tp", billType);
        hm.put("val", 0.1);
        hm.put("cla", BilledBill.class);

        return (List<BillItem>) getBillItemFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<Institution> getCreditInstitution(BillType billType, Date fromDate, Date toDate, boolean lessThan) {
        String sql;
        HashMap hm;
        sql = "Select distinct(b.creditCompany) From BilledBill b "
                + " where b.retired=false ";

        if (lessThan) {
            sql += " and (abs(b.netTotal)-abs(b.paidAmount))>:val ";
        } else {
            sql += " and (abs(b.netTotal)-abs(b.paidAmount))<:val ";
        }

        sql += " and b.cancelled=false "
                + " and b.refundedBill is null"
                + " and b.createdAt between :frm and :to "
                + " and b.paymentMethod= :pm "
                + " and b.billType=:tp"
                + " order by b.creditCompany.name  ";

        hm = new HashMap();
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("pm", PaymentMethod.Credit);
        hm.put("tp", billType);
        hm.put("val", 0.1);

        return (List<Institution>) getInstitutionFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<Institution> getCreditInstitutionPharmacy(List<BillType> billTypes, Date fromDate, Date toDate, boolean lessThan) {
        String sql;
        HashMap hm;
        sql = "Select distinct(b.toInstitution) From BilledBill b "
                + " where b.retired=false ";

        if (lessThan) {
            sql += " and (abs(b.netTotal)-abs(b.paidAmount))>:val ";
        } else {
            sql += " and (abs(b.netTotal)-abs(b.paidAmount))<:val ";
        }

        sql += " and b.cancelled=false "
                + " and b.refunded=false "
                + " and b.createdAt between :frm and :to "
                + " and b.paymentMethod=:pm "
                + " and b.billType in :tps "
                + " and b.toStaff is null "
                + " order by b.toInstitution.name ";

        hm = new HashMap();
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("pm", PaymentMethod.Credit);
        hm.put("tps", billTypes);
        hm.put("val", 0.1);

        return (List<Institution>) getInstitutionFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<PatientEncounter> getCreditPatientEncounter(Institution institution, Date fromDate, Date toDate, PaymentMethod paymentMethod, boolean lessThan) {
        String sql;
        HashMap hm;
        sql = "Select b From PatientEncounter b "
                + " where b.retired=false ";
//        if (lessThan) {
//            sql += " and (abs(b.creditUsedAmount)-abs(b.creditPaidAmount)) >:val ";
//        } else {
//            sql += " and (abs(b.creditUsedAmount)-abs(b.creditPaidAmount)) <:val ";
//        }
        if (lessThan) {
            sql += " and (abs(b.finalBill.netTotal)-(abs(b.creditPaidAmount)+abs(b.finalBill.paidAmount))) >=:val ";
        } else {
            sql += " and (abs(b.finalBill.netTotal)-(abs(b.creditPaidAmount)+abs(b.finalBill.paidAmount))) <=:val ";
        }
        sql += " and b.dateOfDischarge between :frm and :to "
                + " and b.discharged = true "
                + " and b.paymentMethod= :pm "
                + " and b.creditCompany=:ins  ";

        hm = new HashMap();
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("pm", paymentMethod);
        hm.put("ins", institution);
        hm.put("val", 0.01);

        return (List<PatientEncounter>) getPatientEncounterFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<PatientEncounter> getCreditPatientEncounter(Institution institution, Date fromDate, Date toDate, PaymentMethod paymentMethod, boolean lessThan,
            Institution institutionOfDepartment, Department department, Institution site) {
        String sql;
        HashMap hm = new HashMap();

        sql = "Select b From PatientEncounter b "
                + " JOIN b.finalBill fb"
                + " where b.retired=false ";

        if (institutionOfDepartment != null) {
            sql += " and fb.institution = :insd ";
            hm.put("insd", institutionOfDepartment);
        }

        if (department != null) {
            sql += " and fb.department = :dep ";
            hm.put("dep", department);
        }

        if (site != null) {
            sql += " and fb.department.site = :site ";
            hm.put("site", site);
        }

        if (lessThan) {
            sql += " and (abs(b.finalBill.netTotal)-(abs(b.creditPaidAmount)+abs(b.finalBill.paidAmount))) >=:val ";
        } else {
            sql += " and (abs(b.finalBill.netTotal)-(abs(b.creditPaidAmount)+abs(b.finalBill.paidAmount))) <=:val ";
        }
        sql += " and b.dateOfDischarge between :frm and :to "
                + " and b.discharged = true "
                + " and b.paymentMethod= :pm "
                + " and b.creditCompany=:ins  ";

        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("pm", paymentMethod);
        hm.put("ins", institution);
        hm.put("val", 0.01);

        return (List<PatientEncounter>) getPatientEncounterFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<PatientEncounter> getCreditPatientEncounterWithFinalizedPayments(Institution institution, Date fromDate, Date toDate, PaymentMethod paymentMethod,
                                                                                 Institution institutionOfDepartment, Department department, Institution site) {
        String sql;
        HashMap hm = new HashMap();

        sql = "Select b From PatientEncounter b "
                + " JOIN b.finalBill fb "
                + " where b.retired=false "
                + " and b.paymentFinalized=true ";

        if (institutionOfDepartment != null) {
            sql += " and fb.institution = :insd ";
            hm.put("insd", institutionOfDepartment);
        }

        if (department != null) {
            sql += " and fb.department = :dep ";
            hm.put("dep", department);
        }

        if (site != null) {
            sql += " and fb.department.site = :site ";
            hm.put("site", site);
        }

        sql += " and b.dateOfDischarge between :frm and :to "
                + " and b.discharged = true "
                + " and b.paymentMethod= :pm "
                + " and b.creditCompany=:ins  ";

        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("pm", paymentMethod);
        hm.put("ins", institution);

        return (List<PatientEncounter>) getPatientEncounterFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<Institution> getCreditCompanyFromBht(boolean lessThan, PaymentMethod paymentMethod) {
        String sql;
        HashMap hm;
        sql = "Select distinct(b.creditCompany) "
                + " From PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                + " and b.paymentMethod=:pm ";

//        if (lessThan) {
//            sql += " and abs(b.creditUsedAmount)-abs(b.creditPaidAmount)> :val ";
//        } else {
//            sql += " and abs(b.creditUsedAmount)-abs(b.creditPaidAmount)< :val ";
//        }
        if (lessThan) {
            sql += " and (abs(b.finalBill.netTotal)-(abs(b.creditPaidAmount)+abs(b.finalBill.paidAmount))) >:val ";
        } else {
            sql += " and (abs(b.finalBill.netTotal)-(abs(b.creditPaidAmount)+abs(b.finalBill.paidAmount))) <:val ";
        }

        hm = new HashMap();
        hm.put("val", 0.1);
        hm.put("pm", paymentMethod);
        return getInstitutionFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<Institution> getCreditCompanyFromBht(boolean lessThan, PaymentMethod paymentMethod,
            Institution institutionOfDepartment, Department department, Institution site) {
        String sql;
        HashMap hm;
        sql = "Select distinct(b.creditCompany) "
                + " From PatientEncounter b "
                + " JOIN b.finalBill fb "
                + " where b.retired=false "
                + " and b.discharged=true "
                + " and b.paymentMethod=:pm ";

//        if (lessThan) {
//            sql += " and abs(b.creditUsedAmount)-abs(b.creditPaidAmount)> :val ";
//        } else {
//            sql += " and abs(b.creditUsedAmount)-abs(b.creditPaidAmount)< :val ";
//        }
        if (lessThan) {
            sql += " and (abs(b.finalBill.netTotal)-(abs(b.creditPaidAmount)+abs(b.finalBill.paidAmount))) >:val ";
        } else {
            sql += " and (abs(b.finalBill.netTotal)-(abs(b.creditPaidAmount)+abs(b.finalBill.paidAmount))) <:val ";
        }

        hm = new HashMap();

        if (institutionOfDepartment != null) {
            sql += " and fb.institution = :insd ";
            hm.put("insd", institutionOfDepartment);
        }

        if (department != null) {
            sql += " and fb.department = :dep ";
            hm.put("dep", department);
        }

        if (site != null) {
            sql += " and fb.department.site = :site ";
            hm.put("site", site);
        }

        hm.put("val", 0.1);
        hm.put("pm", paymentMethod);
        return getInstitutionFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<Institution> getCreditInstitutionByPatientEncounter(Date fromDate, Date toDate, PaymentMethod paymentMethod, boolean lessThan) {
        String sql;
        HashMap hm;
        sql = "Select distinct(b.creditCompany)"
                + " From PatientEncounter b "
                + " where b.retired=false ";
//        if (lessThan) {
//            sql += " and abs(b.creditUsedAmount)-abs(b.creditPaidAmount) >:val ";
//        } else {
//            sql += " and abs(b.creditUsedAmount)-abs(b.creditPaidAmount) <:val ";
//        }
        if (lessThan) {
            sql += " and (abs(b.finalBill.netTotal)-(abs(b.creditPaidAmount)+abs(b.finalBill.paidAmount))) >=:val ";
        } else {
            sql += " and (abs(b.finalBill.netTotal)-(abs(b.creditPaidAmount)+abs(b.finalBill.paidAmount))) <=:val ";
        }
        sql += " and b.dateOfDischarge between :frm and :to "
                + " and b.discharged = true "
                + " and b.paymentMethod = :pm "
                + " order by b.creditCompany.name  ";

        hm = new HashMap();
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("pm", paymentMethod);
        hm.put("val", 0.01);

        return (List<Institution>) getInstitutionFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<Institution> getCreditInstitutionByPatientEncounter(Date fromDate, Date toDate, PaymentMethod paymentMethod, boolean lessThan,
            Institution institution, Department department, Institution site) {
        String sql;
        HashMap<String, Object> hm = new HashMap<>();
        sql = "Select distinct(b.creditCompany)"
                + " From PatientEncounter b "
                + " JOIN b.finalBill fb "
                + " where b.retired=false ";

        if (lessThan) {
            sql += " and (abs(b.finalBill.netTotal)-(abs(b.creditPaidAmount)+abs(b.finalBill.paidAmount))) >=:val ";
        } else {
            sql += " and (abs(b.finalBill.netTotal)-(abs(b.creditPaidAmount)+abs(b.finalBill.paidAmount))) <=:val ";
        }
        sql += " and b.dateOfDischarge between :frm and :to "
                + " and b.discharged = true "
                + " and b.paymentMethod = :pm ";

        if (institution != null) {
            sql += " and fb.institution = :insd ";
            hm.put("insd", institution);
        }

        if (department != null) {
            sql += " and fb.department = :dep ";
            hm.put("dep", department);
        }

        if (site != null) {
            sql += " and fb.department.site = :site ";
            hm.put("site", site);
        }

        sql += " order by b.creditCompany.name";

        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("pm", paymentMethod);
        hm.put("val", 0.01);

        return getInstitutionFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<Institution> getCreditInstitutionByPatientEncounterWithFinalizedPayments(Date fromDate, Date toDate, PaymentMethod paymentMethod,
                                                                                         Institution institution, Department department, Institution site) {
        String sql;
        HashMap<String, Object> hm = new HashMap<>();
        sql = "Select distinct(b.creditCompany)"
                + " From PatientEncounter b "
                + " JOIN b.finalBill fb "
                + " where b.retired=false "
                + " and b.paymentFinalized=true ";

        sql += " and b.dateOfDischarge between :frm and :to "
                + " and b.discharged = true "
                + " and b.paymentMethod = :pm ";

        if (institution != null) {
            sql += " and fb.institution = :insd ";
            hm.put("insd", institution);
        }

        if (department != null) {
            sql += " and fb.department = :dep ";
            hm.put("dep", department);
        }

        if (site != null) {
            sql += " and fb.department.site = :site ";
            hm.put("site", site);
        }

        sql += " order by b.creditCompany.name";

        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("pm", paymentMethod);

        return getInstitutionFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public double getPaidAmount(Bill b, BillType billType) {
        return getPaidAmount(b, Collections.singletonList(billType)); // Calls the overloaded method
    }

    public double getPaidAmount(Bill b, List<BillType> billTypes) {
        if (b == null || billTypes == null || billTypes.isEmpty()) {
            return 0.0; // Return zero if no valid data
        }

        String sql = "SELECT SUM(b.netValue) FROM BillItem b "
                + "WHERE b.retired=false "
                + "AND b.referenceBill=:rB "
                + "AND b.bill.billType IN :btps";

        HashMap<String, Object> params = new HashMap<>();
        params.put("rB", b);
        params.put("btps", billTypes);

        return getBillItemFacade().findDoubleByJpql(sql, params);
    }

    public double getPaidAmountByBillTypeAtomic(Bill b, BillTypeAtomic billType) {
        return getPaidAmountByBillTypeAtomic(b, Collections.singletonList(billType)); // Calls the overloaded method
    }

    public double getPaidAmountByBillTypeAtomic(Bill b, List<BillTypeAtomic> billTypes) {
        if (b == null || billTypes == null || billTypes.isEmpty()) {
            return 0.0; // Return zero if no valid data
        }
        String sql = "SELECT SUM(b.netValue) FROM BillItem b "
                + "WHERE b.retired=false "
                + "AND b.referenceBill=:rB "
                + "AND b.bill.billTypeAtomic IN :btps";
        HashMap<String, Object> params = new HashMap<>();
        params.put("rB", b);
        params.put("btps", billTypes);
        return getBillItemFacade().findDoubleByJpql(sql, params);
    }

    public double getTotalCreditSettledAmount(Bill b) {
        String sql = "Select sum(b.netValue) "
                + " From BillItem b "
                + " where b.retired=false "
                + " and b.referenceBill=:rB "
                + " and b.bill.billTypeAtomic in :btas ";
        HashMap hm = new HashMap();
        hm.put("rB", b);
        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.addAll(BillTypeAtomic.findByCountedServiceType(CountedServiceType.CREDIT_SETTLE_BY_COMPANY));
        btas.addAll(BillTypeAtomic.findByCountedServiceType(CountedServiceType.CREDIT_SETTLE_BY_PATIENT));
        hm.put("btas", btas);
        return getBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getPaidAmount(Bill b, BillTypeAtomic billTypeAtomic) {
        String sql = "Select sum(b.netValue) From BillItem b "
                + " where b.retired=false "
                + " and b.referenceBill=:rB "
                + " and b.bill.billTypeAtomic=:bta ";
        HashMap hm = new HashMap();
        hm.put("rB", b);
        hm.put("bta", billTypeAtomic);
        return getBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getSettledAmountByCompany(Bill b) {
        System.out.println("Starting getSettledAmountByCompany");
        System.out.println("Input Bill: " + b);

        String sql = "Select sum(b.netValue)"
                + " from BillItem b "
                + " where b.retired=false "
                + " and b.referenceBill=:rB "
                + " and b.bill.billTypeAtomic in :btas ";
        System.out.println("JPQL Query: " + sql);

        HashMap<String, Object> hm = new HashMap<>();
        List<BillTypeAtomic> btas = BillTypeAtomic.findByCountedServiceType(CountedServiceType.CREDIT_SETTLE_BY_COMPANY);
        System.out.println("BillTypeAtomics for CREDIT_SETTLE_BY_COMPANY: " + btas);

        hm.put("rB", b);
        hm.put("btas", btas);
        System.out.println("Query Parameters: " + hm);

        double result = getBillItemFacade().findDoubleByJpql(sql, hm);
        System.out.println("Query Result (Settled Amount by Company): " + result);

        System.out.println("Completed getSettledAmountByCompany");
        return result;
    }

    public double getSettledAmountByPatient(Bill b) {
        String sql = "Select sum(b.netValue)"
                + " from BillItem b "
                + " where b.retired=false "
                + " and b.referenceBill=:rB "
                + " and b.bill.billTypeAtomic in :btas ";
        HashMap hm = new HashMap();
        List<BillTypeAtomic> btas = BillTypeAtomic.findByCountedServiceType(CountedServiceType.CREDIT_SETTLE_BY_PATIENT);
        hm.put("rB", b);
        hm.put("btas", btas);
        return getBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getRefundAmount(Bill b) {
        String sql = "Select sum(b.netTotal+b.vat) "
                + " From Bill b "
                + " where b.retired=false "
                + " and b.billedBill=:b ";

        HashMap hm = new HashMap();
        hm.put("b", b);

        return getBillItemFacade().findDoubleByJpql(sql, hm);

    }

    public Object[] getRefundAmounts(Bill b) {
        String sql = "Select sum(b.netTotal),sum(b.vat) "
                + " From Bill b "
                + " where b.retired=false "
                + " and b.billedBill=:b ";

        HashMap hm = new HashMap();
        hm.put("b", b);

        return getBillItemFacade().findSingleAggregate(sql, hm);

    }

    public List<Bill> getPaidBills(Bill b, BillType billType) {
        String sql = "Select b.bill From BillItem b "
                + " where b.retired=false "
                + " and b.referenceBill=:rB "
                + " and b.bill.billType=:btp ";

        HashMap hm = new HashMap();
        hm.put("rB", b);
        hm.put("btp", billType);

        return billFacade.findByJpql(sql, hm);

    }

    public double getPaidAmount(PatientEncounter p, BillType billType) {
        String sql = "Select sum(b.netValue) From BillItem b "
                + " where b.retired=false "
                + " and b.patientEncounter=:pe "
                + " and b.bill.billType=:btp ";

        HashMap hm = new HashMap();
        hm.put("pe", p);
        hm.put("btp", billType);

        return getBillItemFacade().findDoubleByJpql(sql, hm);

    }

    public double getPaidAmount(PatientEncounter p, BillType billType, Date date) {
        String sql = "Select sum(b.netValue)"
                + "  From BillItem b "
                + " where b.retired=false "
                + " and b.patientEncounter=:pe "
                + " and b.bill.billType=:btp "
                + " and b.bill.createdAt < :date";

        HashMap hm = new HashMap();
        hm.put("pe", p);
        hm.put("btp", billType);
        hm.put("date", date);

        return getBillItemFacade().findDoubleByJpql(sql, hm);

    }

    public List<Institution> getDealorFromReturnBills(Date frmDate, Date toDate, List<BillType> billTypes) {
        String sql;
        HashMap hm;
        sql = "Select distinct(b.toInstitution) From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.paymentMethod=:pm "
                + " and b.createdAt between :frm and :to "
                + " and b.billType in :bts"
                + " order by b.toInstitution.name  ";
        hm = new HashMap();
        hm.put("frm", frmDate);
        hm.put("to", toDate);
        hm.put("pm", PaymentMethod.Credit);
        hm.put("bts", billTypes);
        return getInstitutionFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Bill> getBills(Institution institution, Date frmDate, Date toDate, List<BillType> billTypes) {
        String sql;
        HashMap hm = new HashMap();
        sql = "Select b From BilledBill b"
                + " where  b.retired=false "
                + " and b.cancelled=false "
                + " and b.paymentMethod=:pm "
                + " and b.createdAt  between :frm and :to"
                + " and ((abs(b.netTotal)-abs(b.paidAmount))> :val) "
                + " and (b.fromInstitution=:ins ) "
                + " and b.billType in :bts";
        hm.put("frm", frmDate);
        hm.put("to", toDate);
        hm.put("val", 0.1);
        hm.put("ins", institution);
        hm.put("pm", PaymentMethod.Credit);
        hm.put("bts", billTypes);
        return getBillFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double getGrnReturnValue(Bill refBill, List<BillType> billTypes) {
        String sql = "select sum(b.netTotal) from"
                + " Bill b where "
                + " b.retired=false "
                //                + " and b.paymentMethod=:pm "
                + " and b.referenceBill=:refBill "
                + " and b.billType in :bts";

        HashMap hm = new HashMap();
        hm.put("refBill", refBill);
//        hm.put("pm", PaymentMethod.Credit);
        hm.put("bts", billTypes);
        return getBillFacade().findDoubleByJpql(sql, hm, TemporalType.DATE);
    }

    public List<Bill> getGrnReturnBills(Bill refBill, List<BillType> billTypes) {
        String sql = "select b from"
                + " Bill b where "
                + " b.retired=false "
                + " and b.paymentMethod=:pm "
                + " and b.referenceBill=:refBill "
                + " and b.billType in :bts";

        HashMap hm = new HashMap();
        hm.put("refBill", refBill);
        hm.put("pm", PaymentMethod.Credit);
        hm.put("bts", billTypes);
        return getBillFacade().findByJpql(sql, hm, TemporalType.DATE);
    }

    public List<Institution> getDealorFromBills(List<BillType> billTypes) {
        String sql;
        HashMap hm;
        sql = "Select distinct(b.fromInstitution)"
                + " From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.paymentMethod=:pm "
                + " and ((abs(b.netTotal)-abs(b.paidAmount))> :val) "
                + " and b.billType in :bts ";
        hm = new HashMap();
        hm.put("val", 0.1);
        hm.put("pm", PaymentMethod.Credit);
        hm.put("bts", billTypes);
        return getInstitutionFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Institution> getCreditCompanyFromBills(boolean lessThan) {
        String sql;
        HashMap hm;
        sql = "Select distinct(b.creditCompany) "
                + " From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.paymentMethod=:pm "
                + " and b.billType=:tp1 ";

        if (lessThan) {
            sql += " and ((abs(b.netTotal)-abs(b.paidAmount))> :val) ";
        } else {
            sql += " and ((abs(b.netTotal)-abs(b.paidAmount))< :val) ";
        }

        hm = new HashMap();
        hm.put("val", 0.1);
        hm.put("pm", PaymentMethod.Credit);
        hm.put("tp1", BillType.OpdBill);
        return getInstitutionFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<Institution> getCreditCompanyFromBillsPharmacy(boolean lessThan) {
        String sql;
        HashMap hm;
        sql = "Select distinct(b.toInstitution) "
                + " From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.paymentMethod=:pm "
                + " and b.billType in :tp1 "
                + " and b.toStaff is null ";

        if (lessThan) {
            sql += " and ((abs(b.netTotal)-abs(b.paidAmount))> :val) ";
        } else {
            sql += " and ((abs(b.netTotal)-abs(b.paidAmount))< :val) ";
        }

        hm = new HashMap();
        hm.put("val", 0.1);
        hm.put("pm", PaymentMethod.Credit);
        hm.put("tp1", Arrays.asList(BillType.PharmacyWholeSale, BillType.PharmacySale));
        return getInstitutionFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<Institution> getDealorFromReturnBills() {
        String sql;
        HashMap hm;
        sql = "Select distinct(b.toInstitution) "
                + " From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.paymentMethod=:pm "
                + " and (b.billType=:tp1 or b.billType=:tp2)"
                + " order by b.toInstitution.name  ";
        hm = new HashMap();
        hm.put("pm", PaymentMethod.Credit);
        hm.put("tp1", BillType.PharmacyGrnReturn);
        hm.put("tp2", BillType.PurchaseReturn);
        return getInstitutionFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Institution> getDealorFromReturnBills(List<BillType> billTypes) {
        String sql;
        HashMap hm;
        sql = "Select distinct(b.toInstitution) "
                + " From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.paymentMethod=:pm "
                + " and b.billType in :bts "
                + " order by b.toInstitution.name  ";
        hm = new HashMap();
        hm.put("pm", PaymentMethod.Credit);
        hm.put("bts", billTypes);
        return getInstitutionFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Bill> getBills(Institution institution, List<BillType> billTypes) {
        String sql;
        HashMap hm = new HashMap();
        sql = "Select b From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false  "
                + " and b.paymentMethod=:pm "
                + " and b.createdAt is not null "
                + " and ((abs(b.netTotal)-abs(b.paidAmount))> :val) "
                + " and (b.fromInstitution=:ins ) "
                + " and b.billType in :bts";

        hm.put("val", 0.1);
        hm.put("ins", institution);
        hm.put("pm", PaymentMethod.Credit);
        hm.put("bts", billTypes);
        return getBillFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Bill> getCreditBills(Institution institution, boolean lessThan) {
        String sql;
        HashMap hm = new HashMap();
        sql = "Select b From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.paymentMethod=:pm "
                + " and b.createdAt is not null "
                + " and(b.creditCompany=:ins ) "
                + " and b.billType=:tp1";

        if (lessThan) {
            sql += " and ((abs(b.netTotal)-abs(b.paidAmount))> :val) ";
        } else {
            sql += " and ((abs(b.netTotal)-abs(b.paidAmount))< :val) ";
        }

        hm.put("val", 0.1);
        hm.put("ins", institution);
        hm.put("pm", PaymentMethod.Credit);
        hm.put("tp1", BillType.OpdBill);
        return getBillFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Bill> getCreditBillsPharmacy(Institution institution, boolean lessThan) {
        String sql;
        HashMap hm = new HashMap();
        sql = "Select b From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.paymentMethod=:pm "
                + " and b.createdAt is not null "
                + " and b.toInstitution=:ins "
                + " and b.billType in :tps "
                + " and b.toStaff is null ";

        if (lessThan) {
            sql += " and ((abs(b.netTotal)-abs(b.paidAmount))> :val) ";
        } else {
            sql += " and ((abs(b.netTotal)-abs(b.paidAmount))< :val) ";
        }

        hm.put("val", 0.1);
        hm.put("ins", institution);
        hm.put("pm", PaymentMethod.Credit);
        hm.put("tps", Arrays.asList(BillType.PharmacyWholeSale, BillType.PharmacySale));
        return getBillFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<PatientEncounter> getCreditPatientEncounters(Institution institution, boolean lessThan, PaymentMethod paymentMethod) {
        String sql;
        HashMap hm = new HashMap();
        sql = "Select b From PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                + " and b.paymentMethod=:pm "
                + " and (b.creditCompany=:ins ) ";

//        if (lessThan) {
//            sql += " and abs(b.creditUsedAmount)-abs(b.creditPaidAmount)> :val ";
//        } else {
//            sql += " and abs(b.creditUsedAmount)-abs(b.creditPaidAmount)< :val ";
//        }
        if (lessThan) {
            sql += " and (abs(b.finalBill.netTotal)-(abs(b.creditPaidAmount)+abs(b.finalBill.paidAmount))) >:val ";
        } else {
            sql += " and (abs(b.finalBill.netTotal)-(abs(b.creditPaidAmount)+abs(b.finalBill.paidAmount))) <:val ";
        }

        hm.put("val", 0.1);
        hm.put("ins", institution);
        hm.put("pm", paymentMethod);
        return getPatientEncounterFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<PatientEncounter> getCreditPatientEncounters(Institution institution, boolean lessThan, PaymentMethod paymentMethod,
            Institution institutionOfDepartment, Department department, Institution site) {
        String sql;
        HashMap hm = new HashMap();
        sql = "Select b From PatientEncounter b "
                + " JOIN b.finalBill fb"
                + " where b.retired=false "
                + " and b.discharged=true "
                + " and b.paymentMethod=:pm "
                + " and (b.creditCompany=:ins ) ";

//        if (lessThan) {
//            sql += " and abs(b.creditUsedAmount)-abs(b.creditPaidAmount)> :val ";
//        } else {
//            sql += " and abs(b.creditUsedAmount)-abs(b.creditPaidAmount)< :val ";
//        }
        if (lessThan) {
            sql += " and (abs(b.finalBill.netTotal)-(abs(b.creditPaidAmount)+abs(b.finalBill.paidAmount))) >:val ";
        } else {
            sql += " and (abs(b.finalBill.netTotal)-(abs(b.creditPaidAmount)+abs(b.finalBill.paidAmount))) <:val ";
        }

        if (institutionOfDepartment != null) {
            sql += " and fb.institution = :insd ";
            hm.put("insd", institutionOfDepartment);
        }

        if (department != null) {
            sql += " and fb.department = :dep ";
            hm.put("dep", department);
        }

        if (site != null) {
            sql += " and fb.department.site = :site ";
            hm.put("site", site);
        }

        hm.put("val", 0.1);
        hm.put("ins", institution);
        hm.put("pm", paymentMethod);
        return getPatientEncounterFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<Institution> getDealorFromBills(Date frmDate, Date toDate, List<BillType> billTypes) {
        String sql;
        HashMap hm;
        sql = "Select distinct(b.fromInstitution) "
                + " From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.paymentMethod=:pm "
                + " and ((abs(b.netTotal)-abs(b.paidAmount))> :val) "
                + " and b.createdAt between :frm and :to "
                + " and b.billType in :bts";
        hm = new HashMap();
        hm.put("frm", frmDate);
        hm.put("to", toDate);
        hm.put("val", 0.1);
        hm.put("pm", PaymentMethod.Credit);
        hm.put("bts", billTypes);
        return getInstitutionFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
    }

}
