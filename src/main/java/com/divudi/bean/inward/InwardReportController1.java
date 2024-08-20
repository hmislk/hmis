/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.data.table.String1Value2;
import com.divudi.data.table.String2Value4;

import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.inward.Admission;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.entity.inward.PatientRoom;
import com.divudi.entity.inward.RoomCategory;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PatientRoomFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author pdhs
 */
@Named
@SessionScoped
public class InwardReportController1 implements Serializable {

    @Temporal(TemporalType.TIMESTAMP)
    private Date fromDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date toDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date fromDatePaid;
    @Temporal(TemporalType.TIMESTAMP)
    private Date toDatePaid;
    Category category;
    Item service;

    private AdmissionType admissionType;
    private PaymentMethod paymentMethod;
    private Institution institution;
    PatientEncounter patientEncounter;
    private List<OpdService> opdServices;
    List<String1Value2> timedServices;
    List<RoomChargeInward> roomChargeInwards;
    List<String1Value2> professionals;
    List<String1Value2> professionalsPaid;
    List<String2Value4> inwardCharges;
    List<String1Value2> finalValues;
    List<BillFee> billFees;
    List<BillItem> billItems;
    List<BillItem> billItemMediciene;
    List<BillItem> billItemOutSide;
    List<BillItem> billItemAdimissionFee;
    List<BillItem> billItemGeneralIssuing;
    List<Bill> paidbyPatient;

    List<ItemRateRow> itemRateRows;
    List<Item> items;


    private CommonFunctions commonFunctions;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    BillItemFacade BillItemFacade;
    @EJB
    PatientRoomFacade patientRoomFacade;
    @EJB
    BillFacade billFacade;

    @Inject
    CommonController commonController;

    double billFreeGross;
    double billFeeMargin;
    double billFeeDiscount;
    double billFeeNet;
    double billfeePaidValue;
    double billItemNetValue;

    double paidbyPatientTotalValue;
    double creditPaymentTotalValue;

    double opdSrviceGross;
    double opdServiceMargin;
    double opdServiceDiscount;
    double opdServiceNetValue;
    double roomGross;
    double roomDiscount;
    double timedGross;
    double timedDiscount;
    double professionalGross;
    double professionalGrossPaid;
    double inwardGross;
    double inwardMargin;
    double inwardDiscount;
    double inwardNetValue;
    double inwardAdmissionFeeNetValue;
    double inwardMedicieneNetValue;
    double inwardGeneralIssuingNetValue;
    double inwardOutSideNetValue;

    public InwardReportController1() {
    }

    @Inject
    PriceMatrixController priceMatrixController;

    public void processForItemsWithInwardMatrix() {
        items = new ArrayList<>();
        itemRateRows = new ArrayList<>();
    }

//    public void calculateItemForInwardMatrix() {
//        if (items == null) {
//            return;
//        }
//        for (Item i : items) {
//            ItemRateRow irr = new ItemRateRow(i, priceMatrixController.getItemWithInwardMargin(i));
//            itemRateRows.add(irr);
//        }
////        items = new ArrayList<>();
//    }
    public double fetchItemForInwardMatrix(Item item) {
        if (items == null || department == null) {
            return 0;
        }
        PriceMatrix pm = priceMatrixController.fetchInwardMargin(item, item.getTotal(), department);

        if (pm == null) {
            return 0;
        }

        return (item.getTotal() * pm.getMargin()) / 100;

    }

    public void listItems() {
        items = new ArrayList<>();
        itemRateRows = new ArrayList<>();
    }

    Department department;

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<ItemRateRow> getItemRateRows() {
        return itemRateRows;
    }

    public void setItemRateRows(List<ItemRateRow> itemRateRows) {
        this.itemRateRows = itemRateRows;
    }

    public List<Item> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public Double[] fetchRoomValues() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.calculatedRoomCharge),"
                + " sum(pr.discountRoomCharge),"
                + " sum(pr.calculatedMaintainCharge), "
                + " sum(pr.discountMaintainCharge), "
                + " sum(pr.calculatedMoCharge), "
                + " sum(pr.discountMoCharge), "
                + " sum(pr.calculatedNursingCharge), "
                + " sum(pr.discountNursingCharge), "
                + " sum(pr.calculatedLinenCharge), "
                + " sum(pr.discountLinenCharge), "
                + " sum(pr.calculatedAdministrationCharge), "
                + " sum(pr.discountAdministrationCharge), "
                + " sum(pr.calculatedMedicalCareCharge), "
                + " sum(pr.discountMedicalCareCharge) "
                + " FROM PatientRoom pr "
                + " where pr.retired=false"
                //                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter.dateOfDischarge between :fromDate and :toDate ";

        if (admissionType != null) {
            sql = sql + " and pr.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and pr.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and pr.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);

        Object obj[] = patientRoomFacade.findAggregateModified(sql, hm, TemporalType.TIMESTAMP);
        if (obj == null) {
            Double[] dbl = new Double[14];
            dbl[0] = 0.0;
            dbl[1] = 0.0;
            dbl[2] = 0.0;
            dbl[3] = 0.0;
            dbl[4] = 0.0;
            dbl[5] = 0.0;
            dbl[6] = 0.0;
            dbl[7] = 0.0;
            dbl[8] = 0.0;
            dbl[9] = 0.0;
            dbl[10] = 0.0;
            dbl[11] = 0.0;
            dbl[12] = 0.0;
            dbl[13] = 0.0;

            return dbl;
        } else {
            return Arrays.copyOf(obj, obj.length, Double[].class);
        }

    }

    public List<PatientRoom> fetchPatientRoomTime(Category roomCategory) {
        HashMap hm = new HashMap();
        String sql = "SELECT pr "
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.admittedAt is not null "
                + " and pr.dischargedAt is not null "
                + " and pr.roomFacilityCharge.roomCategory=:cat "
                + " and pr.patientEncounter.dateOfDischarge between :fromDate and :toDate ";

        if (admissionType != null) {
            sql = sql + " and pr.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and pr.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and pr.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        hm.put("cat", roomCategory);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);
        return patientRoomFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchDoctorPaymentInward() {
        HashMap hm = new HashMap();
        String sql = "Select b.paidForBillFee.staff.speciality,"
                + " sum(b.paidForBillFee.feeValue) "
                + " FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.paidForBillFee.bill.patientEncounter.discharged=true "
                + " and(b.paidForBillFee.bill.billType=:refType1 "
                + " or b.paidForBillFee.bill.billType=:refType2 )"
                + " and b.paidForBillFee.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate";

        if (admissionType != null) {
            sql = sql + " and b.bill.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and b.bill.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.bill.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        sql += " group by b.paidForBillFee.staff.speciality "
                + " order by b.paidForBillFee.staff.speciality.name ";

        hm.put("bType", BillType.PaymentBill);
        hm.put("refType1", BillType.InwardBill);
        hm.put("refType2", BillType.InwardProfessional);
        hm.put("fromDate", fromDate);
        hm.put("toDate", toDate);

        return billFeeFacade.findAggregates(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchDoctorPaymentInwardModified(Date frmDate, Date tDate, boolean byDischargedDate) {
        ////// // System.out.println("fetchDoctorPaymentInwardModified");
        HashMap hm = new HashMap();
        String sql = "Select b.staff.speciality,"
                + " sum(b.feeValue) "
                + " FROM BillFee b "
                + " where b.retired=false "
                + " and(b.bill.billType=:refType1 "
                + " or b.bill.billType=:refType2 )";

        if (byDischargedDate) {
            sql += " and b.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate";
        } else {
            sql += " and b.createdAt between :fromDate and :toDate";
        }

        if (admissionType != null) {
            sql = sql + " and b.bill.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and b.bill.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.bill.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        sql += " group by b.staff.speciality "
                + " order by b.staff.speciality.name ";

        hm.put("refType1", BillType.InwardBill);
        hm.put("refType2", BillType.InwardProfessional);
        hm.put("fromDate", frmDate);
        hm.put("toDate", tDate);


        return billFeeFacade.findAggregates(sql, hm, TemporalType.TIMESTAMP);

    }

    private List<Object[]> fetchDoctorPaymentInwardPaid(Date frmDate, Date tDate, boolean byDischargDate) {
        ////// // System.out.println("fetchDoctorPaymentInwardPaid");
        String sql;
        Map m = new HashMap();
        m.put("bclass", BilledBill.class);
        m.put("fd", frmDate);
        m.put("td", tDate);
        m.put("btp", BillType.PaymentBill);
        m.put("refBtp1", BillType.InwardBill);
        m.put("refBtp2", BillType.InwardProfessional);

        sql = "select bf.paidForBillFee.staff.speciality,"
                + " sum(bf.paidForBillFee.feeValue) "
                + " from BillItem bf"
                + " where bf.retired=false "
                + " and bf.bill.cancelled=false "
                + " and type(bf.bill)=:bclass"
                + " and bf.bill.billType=:btp"
                + " and (bf.paidForBillFee.bill.billType=:refBtp1"
                + " or bf.paidForBillFee.bill.billType=:refBtp2)";

//        Remove Cancelled
        sql = "select bf.paidForBillFee.staff.speciality,"
                + " sum(bf.paidForBillFee.feeValue) "
                + " from BillItem bf"
                + " where bf.retired=false "
                + " and type(bf.bill)=:bclass"
                + " and bf.bill.billType=:btp"
                + " and (bf.paidForBillFee.bill.billType=:refBtp1"
                + " or bf.paidForBillFee.bill.billType=:refBtp2)";

        if (byDischargDate) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.dateOfDischarge between :fd and :td ";
        } else {
            sql += " and bf.createdAt between :fd and :td ";
        }

        if (speciality != null) {
            sql += " and bf.paidForBillFee.staff.speciality=:s ";
            m.put("s", speciality);
        }

        if (admissionType != null) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.admissionType=:admTp ";
            m.put("admTp", admissionType);
        }
        if (paymentMethod != null) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.paymentMethod=:pm";
            m.put("pm", paymentMethod);
        }
        if (institution != null) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.creditCompany=:cd";
            m.put("cd", institution);
        }

        sql += " group by bf.paidForBillFee.staff.speciality "
                + " order by bf.paidForBillFee.staff.speciality.name ";

        ////// // System.out.println("sql = " + sql);
        ////// // System.out.println("m = " + m);
        return getBillFeeFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    public void createDoctorPaymentInward(Date frmDate, Date tDate, boolean byDischargedDate) {
        ////// // System.out.println("createDoctorPaymentInward");
        professionals = new ArrayList<>();
        professionalGross = 0;
        List<Object[]> list = fetchDoctorPaymentInwardModified(frmDate, tDate, byDischargedDate);
        for (Object[] obj : list) {
            Speciality sp = (Speciality) obj[0];
            double dbl = (Double) obj[1];

            String1Value2 string1Value2 = new String1Value2();
            string1Value2.setSpeciality(sp);
            string1Value2.setString(sp.getName());
            string1Value2.setValue1(dbl);

            professionalGross += string1Value2.getValue1();

            professionals.add(string1Value2);

        }

    }

    public void createDoctorPaymentInwardPaid(Date frmDate, Date tDate, boolean byDischargedDate) {
        professionalsPaid = new ArrayList<>();
        professionalGrossPaid = 0;
        ////// // System.out.println("frmDate = " + frmDate);
        ////// // System.out.println("tDate = " + tDate);
        List<Object[]> list = fetchDoctorPaymentInwardPaid(frmDate, tDate, byDischargedDate);
        for (Object[] obj : list) {
            Speciality sp = (Speciality) obj[0];
            double dbl = (Double) obj[1];

            String1Value2 string1Value2 = new String1Value2();
            string1Value2.setSpeciality(sp);
            string1Value2.setString(sp.getName());
            string1Value2.setValue1(dbl);

            professionalGrossPaid += string1Value2.getValue1();

            professionalsPaid.add(string1Value2);

        }

    }

    public List<String1Value2> getProfessionalsPaid() {
        return professionalsPaid;
    }

    public void setProfessionalsPaid(List<String1Value2> professionalsPaid) {
        this.professionalsPaid = professionalsPaid;
    }

    public double getProfessionalGrossPaid() {
        return professionalGrossPaid;
    }

    public void setProfessionalGrossPaid(double professionalGrossPaid) {
        this.professionalGrossPaid = professionalGrossPaid;
    }

    public void createTimedService() {
        HashMap hm = new HashMap();
        String sql = "SELECT i.item,"
                + " sum(i.serviceValue),"
                + " sum(i.discount) "
                + " FROM PatientItem i "
                + " where i.retired=false "
                + " and i.patientEncounter.discharged=true "
                + " and i.patientEncounter.dateOfDischarge between :fd and :td  ";
        hm.put("fd", fromDate);
        hm.put("td", toDate);

        if (admissionType != null) {
            sql = sql + " and i.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and i.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and i.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        sql += " group by i.item "
                + " order by i.item.name";

        List<Object[]> results = billFeeFacade.findAggregates(sql, hm, TemporalType.TIMESTAMP);

        if (results == null) {
            return;
        }

        timedServices = new ArrayList<>();
        for (Object[] obj : results) {
            String1Value2 row = new String1Value2();
            Item item = (Item) obj[0];
            row.setString(item.getName());
            row.setValue1((double) obj[1]);
            row.setValue2((double) obj[2]);

            timedGross += row.getValue1();
            timedDiscount += row.getValue2();
            timedServices.add(row);
        }

    }

    public double fetchAdmissionFeeDiscount() {
        HashMap hm = new HashMap();
        hm.put("inwTp", InwardChargeType.AdmissionFee);
        hm.put("btp", BillType.InwardFinalBill);
        String sql = "SELECT  sum(i.discount)"
                + " FROM BillItem i join PatientEncounter pe on i.bill.id=pe.finalBill.id"
                + " where i.retired=false"
                + " and i.inwardChargeType=:inwTp"
                + " and i.bill.billType=:btp "
                + " and pe.discharged=true "
                + " and pe.dateOfDischarge between :fd and :td  ";

        if (admissionType != null) {
            sql = sql + " and pe.admissionType=:at ";
            hm.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and pe.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and pe.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        hm.put("fd", fromDate);
        hm.put("td", toDate);

        return billFeeFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

//     public double fetchAdmissionFeeGross() {
//        HashMap hm = new HashMap();
//        hm.put("inwTp", InwardChargeType.AdmissionFee);
//        hm.put("btp", BillType.InwardFinalBill);
//        String sql;
//
//        //Old JPQL
////        sql = "SELECT  sum(i.grossValue)"
////                + " FROM BillItem i join PatientEncounter pe on i.bill.id=pe.finalBill.id"
////                + " where i.retired=false"
////                + " and i.inwardChargeType=:inwTp"
////                + " and i.bill.billType=:btp "
////                + " and pe.paymentFinalized=true "
////                + " and pe.dateOfDischarge between :fd and :td  ";
//
//        //Old JPQL
//        sql = "SELECT sum(i.grossValue) "
//                + " FROM BillItem i join i.bill.patientEncounter.finalBill b on i.bill.id=b.id "
//                + " where i.retired=false"
//                + " and i.inwardChargeType=:inwTp"
//                + " and i.bill.billType=:btp "
//                + " and pe.paymentFinalized=true "
//                + " and pe.dateOfDischarge between :fd and :td  ";
//
//        Admission a = new Admission();
//        a.isPaymentFinalized();
//
//        if (admissionType != null) {
//            sql = sql + " and pe.admissionType=:at ";
//            hm.put("at", admissionType);
//
//        }
//
//        if (paymentMethod != null) {
//            sql = sql + " and pe.paymentMethod=:bt ";
//            hm.put("bt", paymentMethod);
//        }
//
//        if (institution != null) {
//            sql = sql + " and pe.creditCompany=:cc ";
//            hm.put("cc", institution);
//        }
//        hm.put("fd", fromDate);
//        hm.put("td", toDate);
//        System.err.println("a = " + a);
//        return billFeeFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);
//    }
    @EJB
    PatientEncounterFacade patientEncounterFacade;
    @Inject
    InwardBeanController inwardBeanController;
    double admissionGross;
    double admissionDiscount;
    double admissionNetValue;

    public void calAdmissionFee() {
        admissionGross = 0;
        admissionDiscount = 0;
        admissionNetValue = 0;
        HashMap hm = new HashMap();
        String sql;

        sql = "SELECT pe "
                + " FROM PatientEncounter pe"
                + " where pe.retired=false "
                + " and pe.discharged=true "
                //                + " and pe.paymentFinalized=true "
                + " and pe.dateOfDischarge between :fd and :td  ";

        Admission a = new Admission();
        a.isPaymentFinalized();

        if (admissionType != null) {
            sql = sql + " and pe.admissionType=:at ";
            hm.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and pe.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and pe.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        hm.put("fd", fromDate);
        hm.put("td", toDate);

        List<PatientEncounter> list = patientEncounterFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
        for (PatientEncounter patientEncounter : list) {
            Bill finalBill = inwardBeanController.fetchFinalBill(patientEncounter);
            if (finalBill == null) {
                continue;
            }
            hm.clear();
            hm.put("inwTp", InwardChargeType.AdmissionFee);
            hm.put("b", finalBill);
            sql = "SELECT  i "
                    + " FROM BillItem i "
                    + " where i.retired=false"
                    + " and i.inwardChargeType=:inwTp"
                    + " and i.bill=:b ";
            BillItem billItem = BillItemFacade.findFirstByJpql(sql, hm);
            admissionGross += billItem.getGrossValue();
            admissionDiscount += billItem.getDiscount();
            admissionNetValue += billItem.getNetValue();
        }

    }

    public double fetchOutSideFee() {
        HashMap hm = new HashMap();

        hm.put("btp", BillType.InwardOutSideBill);
        String sql = "SELECT  sum(i.netTotal)"
                + " FROM Bill i "
                + " where i.retired=false"
                + " and i.billType=:btp "
                + " and i.patientEncounter.discharged=true "
                + " and i.patientEncounter.dateOfDischarge between :fd and :td  ";

        if (admissionType != null) {
            sql = sql + " and i.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and i.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and i.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        hm.put("fd", fromDate);
        hm.put("td", toDate);
        return billFeeFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchAdmissionFee() {
        HashMap hm = new HashMap();
        String sql = "SELECT  sum(i.admissionType.admissionFee)"
                + " FROM PatientEncounter i "
                + " where i.retired=false "
                + " and i.discharged=true "
                + " and i.dateOfDischarge between :fd and :td  ";

        if (admissionType != null) {
            sql = sql + " and i.admissionType=:at ";
            hm.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and i.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and i.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        return billFeeFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public Double[] fetchIssue(BillType billType) {
        String sql;
        HashMap hm = new HashMap();
        sql = "SELECT  sum(b.grossValue),"
                + " sum(b.marginValue),"
                + " sum(b.discount),"
                + " sum(b.netValue) "
                + " FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.bill.patientEncounter.discharged=true "
                + " and b.bill.patientEncounter.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql = sql + " and b.bill.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and b.bill.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.bill.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("btp", billType);

        Object obj[] = billFeeFacade.findAggregateModified(sql, hm, TemporalType.TIMESTAMP);
//        System.err.println("OBJ " + obj);
        if (obj == null) {
            Double[] dbl = new Double[4];
            dbl[0] = 0.0;
            dbl[1] = 0.0;
            dbl[2] = 0.0;
            dbl[3] = 0.0;
            return dbl;
        } else {
            return Arrays.copyOf(obj, obj.length, Double[].class);
        }

    }

    public void createInwardService() {
        inwardCharges = new ArrayList<>();

        String2Value4 string1Value3 = new String2Value4();
        string1Value3.setString(InwardChargeType.AdmissionFee.getLabel());
        calAdmissionFee();
        string1Value3.setValue1(admissionGross);
        string1Value3.setValue3(admissionDiscount);
        string1Value3.setValue4(admissionNetValue);
        inwardGross += string1Value3.getValue1();
        inwardDiscount += string1Value3.getValue3();
        inwardNetValue += string1Value3.getValue4();
        inwardCharges.add(string1Value3);

        string1Value3 = new String2Value4();
        Double[] dbl = fetchIssue(BillType.PharmacyBhtPre);
        string1Value3.setString(InwardChargeType.Medicine.getLabel());
        string1Value3.setValue1(dbl[0]);
        string1Value3.setValue2(dbl[1]);
        string1Value3.setValue3(dbl[2]);
        string1Value3.setValue4(dbl[3]);
        inwardGross += string1Value3.getValue1();
        inwardMargin += string1Value3.getValue2();
        inwardDiscount += string1Value3.getValue3();
        inwardNetValue += string1Value3.getValue4();
        inwardCharges.add(string1Value3);

        string1Value3 = new String2Value4();
        dbl = fetchIssue(BillType.StoreBhtPre);
        string1Value3.setString(InwardChargeType.GeneralIssuing.getLabel());
        string1Value3.setValue1(dbl[0]);
        string1Value3.setValue2(dbl[1]);
        string1Value3.setValue3(dbl[2]);
        string1Value3.setValue4(dbl[3]);
        inwardGross += string1Value3.getValue1();
        inwardMargin += string1Value3.getValue2();
        inwardDiscount += string1Value3.getValue3();
        inwardNetValue += string1Value3.getValue4();
        inwardCharges.add(string1Value3);

        string1Value3 = new String2Value4();
        string1Value3.setString("Out Side Charges : ");
        string1Value3.setValue1(fetchOutSideFee());
        string1Value3.setValue4(string1Value3.getValue1());
        inwardGross += string1Value3.getValue1();
        inwardNetValue += string1Value3.getValue1();
        inwardCharges.add(string1Value3);

    }

    private List<Object[]> calFee() {
        String sql;
        Map m = new HashMap();
        sql = "select bf.billItem.item.category, "
                + " sum(bf.feeDiscount),"
                + " sum(bf.feeMargin),"
                + " sum(bf.feeGrossValue),"
                + " sum(bf.feeValue),"
                + " sum(bf.billItem.qty)"
                + " from BillFee bf "
                + " where bf.retired=false "
                + " and bf.bill.patientEncounter.discharged=true "
                + " and bf.billItem.retired=false "
                + " and bf.fee.feeType!=:ftp ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("ftp", FeeType.Staff);
        m.put("billType", BillType.InwardBill);
        sql = sql + " and bf.bill.billType=:billType "
                + " and bf.bill.patientEncounter.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql = sql + " and bf.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and bf.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bf.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        sql = sql + " group by bf.billItem.item.category"
                //                + " bf.billItem.bill.billClassType"
                + "  order by bf.billItem.item.category.name";
        return billFeeFacade.findAggregates(sql, m, TemporalType.TIMESTAMP);
    }

    private long calFee(Category category1, Bill bill) {
        String sql;
        Map m = new HashMap();
        sql = "select count(bf.billItem)"
                + " from BillFee bf "
                + " where "
                + " bf.retired=false "
                + " and type(bf.bill)=:class "
                + " and bf.billItem.item.category=:cat"
                + " and bf.billItem.retired=false "
                + " and bf.bill.patientEncounter.discharged=true "
                + " and bf.fee.feeType!=:ftp ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("class", bill.getClass());
        m.put("cat", category1);
        m.put("ftp", FeeType.Staff);
        m.put("billType", BillType.InwardBill);
        sql = sql + " and bf.bill.billType=:billType and"
                + " bf.bill.patientEncounter.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql = sql + " and bf.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and bf.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bf.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        return billFeeFacade.findLongByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public void createOpdServiceWithoutPro() {

        List<Object[]> results = calFee();
//        PatientEncounter pe = new PatientEncounter();
//        pe.getAdmissionType();
        if (results == null) {
            return;
        }

        opdServices = new ArrayList<>();

        for (Object[] objs : results) {

            OpdService row = new OpdService();
            row.setCategory((Category) objs[0]);
            row.setDiscount((double) objs[1]);
            row.setMargin((double) objs[2]);
            row.setGrossValue((double) objs[3]);
            row.setNetValue((double) objs[4]);
            Double dbl = (Double) objs[5];

            if (dbl != null) {
                row.setCount(dbl.longValue());
            }
//            System.err.println("objs 1 = ");
//            try {
//                long billed = calFee(row.getCategory(), new BilledBill());
////                System.err.println("objs 2 = ");
//                long cancel = calFee(row.getCategory(), new CancelledBill());
////                System.err.println("objs 3 = ");
//                long ret = calFee(row.getCategory(), new RefundBill());
//                row.setCount(billed - (cancel + ret));
//            } catch (Exception e) {
//                row.setCount(0l);
//            }
            opdSrviceGross += row.getGrossValue();
            opdServiceMargin += row.getMargin();
            opdServiceDiscount += row.getDiscount();
            opdServiceNetValue += row.getNetValue();
            opdServices.add(row);

        }

    }

    List<String1Value2> roomTimes;
    List<CategoryTime> categoryTimes;
    List<PatientRoom> patientRooms;
    @Inject
    RoomCategoryController roomCategoryController;

    public void createRoomTable() {
        roomChargeInwards = new ArrayList<>();
        Double[] dbl = fetchRoomValues();

        RoomChargeInward row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.RoomCharges);
        row.setCalculated(dbl[0]);
        row.setDiscount(dbl[1]);
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomChargeInwards.add(row);
        ///////////////
        row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.MaintainCharges);
        row.setCalculated(dbl[2]);
        row.setDiscount(dbl[3]);
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomChargeInwards.add(row);
        ///////////////
        row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.MOCharges);
        row.setCalculated(dbl[4]);
        row.setDiscount(dbl[5]);
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomChargeInwards.add(row);
        ///////////////
        row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.NursingCharges);
        row.setCalculated(dbl[6]);
        row.setDiscount(dbl[7]);
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomChargeInwards.add(row);
        ///////////////
        row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.LinenCharges);
        row.setCalculated(dbl[8]);
        row.setDiscount(dbl[9]);
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomChargeInwards.add(row);
        ///////////////
        row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.AdministrationCharge);

        row.setCalculated(dbl[10]);
        row.setDiscount(dbl[11]);
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomChargeInwards.add(row);
        ///////////////
        row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.MedicalCareICU);
        row.setCalculated(dbl[12]);
        row.setDiscount(dbl[13]);
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomChargeInwards.add(row);
        ///////////////

    }

    public void bhtCreditPayments() {
        Date startTime = new Date();
        
        HashMap hm = new HashMap();
        String sql = "Select b from BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:btp"
                + " and b.bill.cancelled=false "
                + " and type(b.bill)=:class "
                + " and b.patientEncounter is not null"
                + " and b.createdAt between :frm and :to ";

        if (admissionType != null) {
            sql = sql + " and b.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and b.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        sql += " order by b.patientEncounter.bhtNo ";

        hm.put("btp", BillType.CashRecieveBill);
        hm.put("class", BilledBill.class);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());

        billItems = BillItemFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
        totalBhtCreditPayments();
        
        

    }

    public Double totalBhtCreditPayments() {
        HashMap hm = new HashMap();
        String sql = "Select sum(b.netValue) from BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:btp"
                + " and b.bill.cancelled=false "
                + " and type(b.bill)=:class "
                + " and b.patientEncounter is not null"
                + " and b.createdAt between :frm and :to ";

        if (admissionType != null) {
            sql = sql + " and b.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and b.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        sql += " order by b.patientEncounter.bhtNo ";

        hm.put("btp", BillType.CashRecieveBill);
        hm.put("class", BilledBill.class);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());

        billItemNetValue = BillItemFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);
        return billItemNetValue;

    }

    public void opdCreditPayments() {
        Date startTime = new Date();
        
        HashMap hm = new HashMap();
        String sql = "Select b.referenceBill.billItems"
                + " from BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:btp"
                + " and b.bill.cancelled=false "
                + " and type(b.bill)=:class "
                + " and b.referenceBill is not null"
                + " and b.createdAt between :frm and :to ";

        if (category != null) {
            sql += " and b.bill.item.category=:cat";
            hm.put("cat", category);
        }

        if (institution != null) {
            sql += " and b.referenceBill.creditCompany=:ins";
            hm.put("ins", institution);
        }

        if (service != null) {
            sql += " and b.bill.item=:itm";
            hm.put("itm", service);
        }

        sql += " order by b.referenceBill.insId ";

        hm.put("btp", BillType.CashRecieveBill);
        hm.put("class", BilledBill.class);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());

        billItems = BillItemFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
        totalOfOpdCreditPayments();

        
    }

    public Double totalOfOpdCreditPayments() {
        HashMap hm = new HashMap();
        String sql = "Select sum(b.referenceBill.netTotal)"
                + " from BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:btp"
                + " and b.bill.cancelled=false "
                + " and type(b.bill)=:class "
                + " and b.referenceBill is not null"
                + " and b.createdAt between :frm and :to ";

        sql += " order by b.referenceBill.insId ";

        hm.put("btp", BillType.CashRecieveBill);
        hm.put("class", BilledBill.class);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());

        billItemNetValue = BillItemFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

        return billItemNetValue;

    }

    public void createRoomTime() {
        Date startTime = new Date();
        
        categoryTimes = new ArrayList<>();

        for (RoomCategory rm : roomCategoryController.getItems()) {
            long time = 0;
            double calculated = 0;
            double added = 0;
            Calendar frm = Calendar.getInstance();
            Calendar to = Calendar.getInstance();
            Calendar ans = Calendar.getInstance();
            List<PatientRoom> list = fetchPatientRoomTime(rm);
            for (PatientRoom pt : list) {
                frm.setTime(pt.getAdmittedAt());
                to.setTime(pt.getDischargedAt());
                time += (to.getTimeInMillis() - frm.getTimeInMillis());

                added += pt.getAddedRoomCharge();
                calculated += (pt.getCalculatedRoomCharge() - pt.getAddedRoomCharge());

            }

            CategoryTime row = new CategoryTime();
            row.setRoomCategory((RoomCategory) rm);
            row.setTime(time / (1000 * 60 * 60));
            row.setCalculated(calculated);
            row.setAdded(added);
            categoryTimes.add(row);
        }
        
        
    }

    public void createRoomTime(Category cat) {

        long dbl = 0;
        Calendar frm = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        Calendar ans = Calendar.getInstance();
        patientRooms = fetchPatientRoomTime(cat);
        for (PatientRoom pt : patientRooms) {
            if (pt.getAdmittedAt() != null && pt.getDischargedAt() != null) {
                frm.setTime(pt.getAdmittedAt());
                to.setTime(pt.getDischargedAt());
                long val = (to.getTimeInMillis() - frm.getTimeInMillis());
                pt.setTmpStayedTime(val / (1000 * 60 * 60));
            }

        }

    }

    public void createBHTDiscountTable() {
        Date statTime = new Date();
        
        String sql;
        Map m = new HashMap();
        billItems = new ArrayList<>();
//        BillItem bi = new BillItem();
//        bi.getBill().getPatientEncounter().getBhtNo();
        sql = "select bi"
                + " from BillItem bi where "
                + " bi.bill.patientEncounter.dateOfDischarge between :fd and :td "
                + " and bi.retired=false "
                + " and type(bi.bill)=:class "
                + " and bi.bill.billType=:bt "
                + " and bi.bill.cancelled=false ";

        m.put("bt", BillType.InwardFinalBill);
        m.put("class", BilledBill.class);
        m.put("fd", fromDate);
        m.put("td", toDate);

        if (admissionType != null) {
            sql += " and bi.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql += " and bi.bill.patientEncounter.paymentMethod=:pm ";
            m.put("pm", paymentMethod);
        }

        if (institution != null) {
            sql += " and bi.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        if (patientEncounter != null) {
            sql += " and bi.bill.patientEncounter=:pe ";
            m.put("pe", patientEncounter);
        }

        sql += " order by bi.bill.patientEncounter.bhtNo ,bi.inwardChargeType";

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        Double[] dbl = fetchFinalBillTotals();

        inwardGross = dbl[0];
        inwardMargin = dbl[1];
        inwardDiscount = dbl[2];
        inwardNetValue = dbl[3];
        
        

    }

    public Double[] fetchFinalBillTotals() {
        String sql;
        Map m = new HashMap();
        sql = "select sum(bi.grossValue),"
                + " sum(bi.marginValue),"
                + " sum(bi.discount),"
                + " sum(bi.netValue)"
                + " from BillItem bi where "
                + " bi.bill.patientEncounter.dateOfDischarge between :fd and :td "
                + " and bi.retired=false "
                + " and type(bi.bill)=:class "
                + " and bi.bill.billType=:bt "
                + " and bi.bill.cancelled=false ";

        m.put("bt", BillType.InwardFinalBill);
        m.put("class", BilledBill.class);
        m.put("fd", fromDate);
        m.put("td", toDate);

        if (admissionType != null) {
            sql += " and bi.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql += " and bi.bill.patientEncounter.paymentMethod=:pm ";
            m.put("pm", paymentMethod);
        }

        if (institution != null) {
            sql += " and bi.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        if (patientEncounter != null) {
            sql += " and bi.bill.patientEncounter=:pe ";
            m.put("pe", patientEncounter);
        }

        Object obj[] = patientRoomFacade.findAggregateModified(sql, m, TemporalType.TIMESTAMP);
        if (obj == null) {
            Double[] dbl = new Double[4];
            dbl[0] = 0.0;
            dbl[1] = 0.0;
            dbl[2] = 0.0;
            dbl[3] = 0.0;

            return dbl;
        } else {
            return Arrays.copyOf(obj, obj.length, Double[].class);
        }

    }

    public List<CategoryTime> getCategoryTimes() {
        return categoryTimes;
    }

    public void setCategoryTimes(List<CategoryTime> categoryTimes) {
        this.categoryTimes = categoryTimes;
    }

    public List<PatientRoom> getPatientRooms() {
        return patientRooms;
    }

    public void setPatientRooms(List<PatientRoom> patientRooms) {
        this.patientRooms = patientRooms;
    }

    public RoomCategoryController getRoomCategoryController() {
        return roomCategoryController;
    }

    public void setRoomCategoryController(RoomCategoryController roomCategoryController) {
        this.roomCategoryController = roomCategoryController;
    }

    public double getBillItemNetValue() {
        return billItemNetValue;
    }

    public void setBillItemNetValue(double billItemNetValue) {
        this.billItemNetValue = billItemNetValue;
    }

    public List<BillItem> getBillItemMediciene() {
        return billItemMediciene;
    }

    public void setBillItemMediciene(List<BillItem> billItemMediciene) {
        this.billItemMediciene = billItemMediciene;
    }

    public List<BillItem> getBillItemOutSide() {
        return billItemOutSide;
    }

    public void setBillItemOutSide(List<BillItem> billItemOutSide) {
        this.billItemOutSide = billItemOutSide;
    }

    public List<BillItem> getBillItemAdimissionFee() {
        return billItemAdimissionFee;
    }

    public void setBillItemAdimissionFee(List<BillItem> billItemAdimissionFee) {
        this.billItemAdimissionFee = billItemAdimissionFee;
    }

    public List<BillItem> getBillItemGeneralIssuing() {
        return billItemGeneralIssuing;
    }

    public void setBillItemGeneralIssuing(List<BillItem> billItemGeneralIssuing) {
        this.billItemGeneralIssuing = billItemGeneralIssuing;
    }

    public double getInwardAdmissionFeeNetValue() {
        return inwardAdmissionFeeNetValue;
    }

    public void setInwardAdmissionFeeNetValue(double inwardAdmissionFeeNetValue) {
        this.inwardAdmissionFeeNetValue = inwardAdmissionFeeNetValue;
    }

    public double getInwardMedicieneNetValue() {
        return inwardMedicieneNetValue;
    }

    public void setInwardMedicieneNetValue(double inwardMedicieneNetValue) {
        this.inwardMedicieneNetValue = inwardMedicieneNetValue;
    }

    public double getInwardGeneralIssuingNetValue() {
        return inwardGeneralIssuingNetValue;
    }

    public void setInwardGeneralIssuingNetValue(double inwardGeneralIssuingNetValue) {
        this.inwardGeneralIssuingNetValue = inwardGeneralIssuingNetValue;
    }

    public double getInwardOutSideNetValue() {
        return inwardOutSideNetValue;
    }

    public void setInwardOutSideNetValue(double inwardOutSideNetValue) {
        this.inwardOutSideNetValue = inwardOutSideNetValue;
    }

    public Item getService() {
        return service;
    }

    public void setService(Item service) {
        this.service = service;
    }

    public void makeNull() {
        opdSrviceGross = 0;
        opdServiceMargin = 0;
        opdServiceDiscount = 0;
        opdServiceNetValue = 0;
        roomGross = 0;
        roomDiscount = 0;
        timedGross = 0;
        timedDiscount = 0;
        professionalGross = 0;
        inwardGross = 0;
        inwardMargin = 0;
        inwardDiscount = 0;
        inwardNetValue = 0;
    }

    public void process() {
        Date startTime = new Date();

        makeNull();

        createOpdServiceWithoutPro();
        createRoomTable();
        createDoctorPaymentInward(getFromDate(), getToDate(), true);
        createTimedService();
        createInwardService();
        createFinalSummeryMonth();
        createPaidByPatient();
        createCreditPayment();
        
        

    }

    public void processProfessionalPayment() {
        ////// // System.out.println("professinal payment bill processing");
        makeNull();

        createDoctorPaymentInward(getFromDate(), getToDate(), false);
        createDoctorPaymentInwardPaid(getFromDatePaid(), getToDatePaid(), false);

        for (String1Value2 added : professionals) {

            for (String1Value2 paid : professionalsPaid) {
                if (paid.getSpeciality().equals(added.getSpeciality())) {
                    added.setValue2(paid.getValue1());
//                    break;
                }
            }
        }

    }

    public void processProfessionalPaymentByDischargedDate() {
        makeNull();

        createDoctorPaymentInward(getFromDate(), getToDate(), true);
        createDoctorPaymentInwardPaid(getFromDatePaid(), getToDatePaid(), true);

        for (String1Value2 added : professionals) {

            for (String1Value2 paid : professionalsPaid) {
                if (paid.getSpeciality().equals(added.getSpeciality())) {
                    added.setValue2(paid.getValue1());
                    break;
                }
            }
        }

    }

    public void processRoomTime() {
        makeNull();
        createRoomTime();
    }

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
    }

    public InwardBeanController getInwardBeanController() {
        return inwardBeanController;
    }

    public void setInwardBeanController(InwardBeanController inwardBeanController) {
        this.inwardBeanController = inwardBeanController;
    }

    public double getAdmissionGross() {
        return admissionGross;
    }

    public void setAdmissionGross(double admissionGross) {
        this.admissionGross = admissionGross;
    }

    public double getAdmissionDiscount() {
        return admissionDiscount;
    }

    public void setAdmissionDiscount(double admissionDiscount) {
        this.admissionDiscount = admissionDiscount;
    }

    public double getAdmissionNetValue() {
        return admissionNetValue;
    }

    public void setAdmissionNetValue(double admissionNetValue) {
        this.admissionNetValue = admissionNetValue;
    }

    public List<String1Value2> getRoomTimes() {
        return roomTimes;
    }

    public void setRoomTimes(List<String1Value2> roomTimes) {
        this.roomTimes = roomTimes;
    }

    private void createFinalSummeryMonth() {
        finalValues = new ArrayList<>();
        String1Value2 dd;
        ////////       
        dd = new String1Value2();
        dd.setString("Total Gross ");
        dd.setValue1(inwardGross + opdSrviceGross + roomGross + professionalGross + timedGross);
        finalValues.add(dd);
        ///////////
        dd = new String1Value2();
        dd.setString("Total Margin ");
        dd.setValue1(inwardMargin + opdServiceMargin);
        finalValues.add(dd);
        ///////////
        dd = new String1Value2();
        dd.setString("Total Discount ");
        dd.setValue1(inwardDiscount + timedDiscount + roomDiscount + opdServiceDiscount);
        finalValues.add(dd);
        ///////////

        dd = new String1Value2();
        dd.setString("Total Net ");
        Double tmp = inwardNetValue + opdServiceNetValue + (roomGross - roomDiscount) + professionalGross + (timedGross - timedDiscount);
        dd.setValue1(tmp);
        finalValues.add(dd);

    }

    public void createPaidByPatient() {

        String sql;
        Map m = new HashMap();
        sql = "SELECT b FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType=:bty"
                + " and b.patientEncounter.dateOfDischarge between :frmd and :td";

        m.put("bty", BillType.InwardPaymentBill);
        m.put("frmd", fromDate);
        m.put("td", toDate);

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:adty";
            m.put("adty", admissionType);
        }

        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod=:paymth";
            m.put("paymth", paymentMethod);
        }

        if (institution != null) {
            sql += " and b.patientEncounter.creditCompany=:crdcom";
            m.put("crdcom", institution);
        }

        paidbyPatient = billFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        totalofCreatePaidByPatient();

    }

    public double totalofCreatePaidByPatient() {

        String sql;
        Map m = new HashMap();
        sql = "SELECT sum(b.netTotal) FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType=:bty"
                + " and b.patientEncounter.dateOfDischarge between :frmd and :td";

        m.put("bty", BillType.InwardPaymentBill);
        m.put("frmd", fromDate);
        m.put("td", toDate);

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:adty";
            m.put("adty", admissionType);
        }

        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod=:paymth";
            m.put("paymth", paymentMethod);
        }

        if (institution != null) {
            sql += " and b.patientEncounter.creditCompany=:crdcom";
            m.put("crdcom", institution);
        }

        paidbyPatientTotalValue = billFacade.findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
        return paidbyPatientTotalValue;

    }

    public void createCreditPayment() {

        String sql;
        Map m = new HashMap();
        sql = "SELECT bi FROM BillItem bi "
                + " WHERE bi.retired=false "
                + " and bi.bill.billType=:bty"
                + " and bi.patientEncounter.dateOfDischarge between :frmd and :td";

        m.put("bty", BillType.CashRecieveBill);
        m.put("frmd", fromDate);
        m.put("td", toDate);

        if (admissionType != null) {
            sql += " and bi.patientEncounter.admissionType =:adty";
            m.put("adty", admissionType);
        }

        if (paymentMethod != null) {
            sql += " and bi.patientEncounter.paymentMethod=:paymth";
            m.put("paymth", paymentMethod);
        }

        if (institution != null) {
            sql += " and bi.patientEncounter.creditCompany=:crdcom";
            m.put("crdcom", institution);
        }

        billItems = BillItemFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        totalofCreateCreditPayment();

    }

    public double totalofCreateCreditPayment() {

        String sql;
        Map m = new HashMap();
        sql = "SELECT sum(bi.netValue) FROM BillItem bi "
                + " WHERE bi.retired=false "
                + " and bi.bill.billType=:bty"
                + " and bi.patientEncounter.dateOfDischarge between :frmd and :td";

        m.put("bty", BillType.CashRecieveBill);
        m.put("frmd", fromDate);
        m.put("td", toDate);

        if (admissionType != null) {
            sql += " and bi.patientEncounter.admissionType =:adty";
            m.put("adty", admissionType);
        }

        if (paymentMethod != null) {
            sql += " and bi.patientEncounter.paymentMethod=:paymth";
            m.put("paymth", paymentMethod);
        }

        if (institution != null) {
            sql += " and bi.patientEncounter.creditCompany=:crdcom";
            m.put("crdcom", institution);
        }

        creditPaymentTotalValue = BillItemFacade.findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

        return creditPaymentTotalValue;

    }

    public String processCategoryBillItems() {
        String sql;
        Map m = new HashMap();
        sql = "select bf "
                + " from BillFee bf "
                + " where bf.bill.patientEncounter.discharged=true "
                + " and bf.retired=false "
                + " and bf.billItem.retired=false "
                + " and bf.fee.feeType!=:ftp ";
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("ftp", FeeType.Staff);
        m.put("billType", BillType.InwardBill);
        sql = sql + " and bf.bill.billType=:billType and"
                + " bf.bill.patientEncounter.dateOfDischarge between :fd and :td ";
        if (admissionType != null) {
            sql = sql + " and bf.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (category != null) {
            sql = sql + " and bf.billItem.item.category=:category";
            m.put("category", category);
        }

        if (paymentMethod != null) {
            sql = sql + " and bf.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bf.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        sql = sql + " order by bf.bill.patientEncounter.bhtNo";
        billFees = billFeeFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

//        PatientEncounter pe = new PatientEncounter();
//        pe.getBhtNo();
        if (billFees == null) {
            billFees = new ArrayList<>();
        }

        billFreeGross = 0.0;
        billFeeDiscount = 0.0;
        billFeeMargin = 0.0;
        billFeeNet = 0.0;

        for (BillFee f : billFees) {
            if (f.getFeeGrossValue() != null) {
                billFreeGross += f.getFeeGrossValue();
            }
            billFeeDiscount += f.getFeeDiscount();
            billFeeMargin += f.getFeeMargin();
            billFeeNet += f.getFeeValue();
        }

        return "report_income_by_caregories_and_bht";
    }

    public String processProfessionalFees() {

        String sql;
        Map m = new HashMap();
        sql = "select bf "
                + " from BillFee bf "
                + " where bf.bill.patientEncounter.discharged=true "
                + " and bf.retired=false "
                + " and bf.billItem.retired=false "
                + " and bf.fee.feeType=:ftp ";
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("ftp", FeeType.Staff);
        m.put("billType1", BillType.InwardBill);
        m.put("billType2", BillType.InwardProfessional);
        sql = sql + " and (bf.bill.billType=:billType1"
                + " or bf.bill.billType=:billType2)"
                + " and bf.bill.patientEncounter.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql = sql + " and bf.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (speciality != null) {
            sql = sql + " and bf.staff.speciality=:sp";
            m.put("sp", speciality);
        }

        if (staff != null) {
            sql = sql + " and bf.staff=:stf";
            m.put("sp", staff);
        }

        if (paymentMethod != null) {
            sql = sql + " and bf.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bf.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        sql = sql + " order by bf.bill.patientEncounter.bhtNo";
        billFees = billFeeFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

//        PatientEncounter pe = new PatientEncounter();
//        pe.getBhtNo();
//        if (billFees == null) {
//            billFees = new ArrayList<>();
//        }
//
//        billFreeGross = 0.0;
//        billFeeDiscount = 0.0;
//        billFeeMargin = 0.0;
//        billFeeNet = 0.0;
//
//        for (BillFee f : billFees) {
//            if (f.getFeeGrossValue() != null) {
//                billFreeGross += f.getFeeGrossValue();
//            }
//            billFeeDiscount += f.getFeeDiscount();
//            billFeeMargin += f.getFeeMargin();
//            billFeeNet += f.getFeeValue();
//            
//        }
        Double[] dbl = totalOfProcessProfessionalFees();

        billFeeNet = dbl[0];
        billfeePaidValue = dbl[1];

        return "report_income_by_professional_fees_and_bht";
    }

    public Double[] totalOfProcessProfessionalFees() {
        String sql;
        Map m = new HashMap();
        sql = "select sum(bf.feeValue),sum(bf.paidValue) "
                + " from BillFee bf "
                + " where bf.bill.patientEncounter.discharged=true "
                + " and bf.retired=false "
                + " and bf.billItem.retired=false "
                + " and bf.fee.feeType=:ftp ";
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("ftp", FeeType.Staff);
        m.put("billType1", BillType.InwardBill);
        m.put("billType2", BillType.InwardProfessional);
        sql = sql + " and (bf.bill.billType=:billType1"
                + " or bf.bill.billType=:billType2)"
                + " and bf.bill.patientEncounter.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql = sql + " and bf.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (speciality != null) {
            sql = sql + " and bf.staff.speciality=:sp";
            m.put("sp", speciality);
        }

        if (staff != null) {
            sql = sql + " and bf.staff=:stf";
            m.put("sp", staff);
        }

        if (paymentMethod != null) {
            sql = sql + " and bf.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bf.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        sql = sql + " order by bf.bill.patientEncounter.bhtNo";

        Object totalObj[] = billFeeFacade.findAggregateModified(sql, m, TemporalType.TIMESTAMP);
        if (totalObj == null) {
            Double dbl[] = new Double[2];
            dbl[0] = 0.0;
            dbl[1] = 0.0;
            return dbl;

        } else {
            return Arrays.copyOf(totalObj, totalObj.length, Double[].class);
        }
    }

    Speciality speciality;
    Staff staff;

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public double getBillfeePaidValue() {
        return billfeePaidValue;
    }

    public void setBillfeePaidValue(double billfeePaidValue) {
        this.billfeePaidValue = billfeePaidValue;
    }

    public void processPatientRooms() {
        String sql;
        Map m = new HashMap();
        sql = "select bf "
                + " from PatientRoom bf "
                + " where bf.patientEncounter.discharged=true "
                + " and bf.retired=false"
                + " and bf.patientEncounter.dateOfDischarge between :fd and :td ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        if (admissionType != null) {
            sql = sql + " and bf.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and bf.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bf.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        sql += " order by bf.patientEncounter.bhtNo";
        patientRooms = patientRoomFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

//        PatientEncounter pe = new PatientEncounter();
//        pe.getBhtNo();
        if (billFees == null) {
            billFees = new ArrayList<>();
        }

    }

    public void processInwardCharges() {
        processInwardChargesAdmissionFee();
        processInwardChargesMediceine();
        processInwardChargesGeneralIssuing();
        processInwardChargesOutSideCharges();

    }

    public void processInwardChargesAdmissionFee() {
        String sql;
        Map m = new HashMap();
        sql = "select bi "
                + " from BillItem bi "
                + " where bi.bill.patientEncounter.discharged=true "
                + " and bi.retired=false"
                + " and bi.bill.patientEncounter.dateOfDischarge between :fd and :td "
                + " and bi.bill.billType=:bty"
                + " and bi.inwardChargeType=:inwty";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bty", BillType.InwardFinalBill);
        m.put("inwty", InwardChargeType.AdmissionFee);
        if (admissionType != null) {
            sql = sql + " and bi.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and bi.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bi.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        sql += " order by bi.bill.patientEncounter.bhtNo";
        billItemAdimissionFee = BillItemFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        totalOfProcessInwardChargesAdmissionFee();

//        PatientEncounter pe = new PatientEncounter();
//        pe.getBhtNo();
        if (billFees == null) {
            billFees = new ArrayList<>();
        }

    }

    public double totalOfProcessInwardChargesAdmissionFee() {
        String sql;
        Map m = new HashMap();
        sql = "select sum(bi.netValue) "
                + " from BillItem bi "
                + " where bi.bill.patientEncounter.discharged=true "
                + " and bi.retired=false"
                + " and bi.bill.patientEncounter.dateOfDischarge between :fd and :td "
                + " and bi.bill.billType=:bty"
                + " and bi.inwardChargeType=:inwty";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bty", BillType.InwardFinalBill);
        m.put("inwty", InwardChargeType.AdmissionFee);
        if (admissionType != null) {
            sql = sql + " and bi.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and bi.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bi.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        sql += " order by bi.bill.patientEncounter.bhtNo";

        inwardAdmissionFeeNetValue = BillItemFacade.findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

        return inwardAdmissionFeeNetValue;

    }

    public void processInwardChargesMediceine() {
        String sql;
        Map m = new HashMap();
        sql = "select bi "
                + " from BillItem bi "
                + " where bi.bill.patientEncounter.discharged=true "
                + " and bi.retired=false"
                + " and bi.bill.patientEncounter.dateOfDischarge between :fd and :td "
                + " and bi.bill.billType=:bty"
                + " and bi.inwardChargeType=:inwty";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bty", BillType.PharmacyBhtPre);
        m.put("inwty", InwardChargeType.Medicine);
        if (admissionType != null) {
            sql = sql + " and bi.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and bi.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bi.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        sql += " order by bi.bill.patientEncounter.bhtNo";
        billItemMediciene = BillItemFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        totalOfProcessInwardChargesMediceine();

//        PatientEncounter pe = new PatientEncounter();
//        pe.getBhtNo();
        if (billFees == null) {
            billFees = new ArrayList<>();
        }

    }

    public double totalOfProcessInwardChargesMediceine() {
        String sql;
        Map m = new HashMap();
        sql = "select sum(bi.netValue) "
                + " from BillItem bi "
                + " where bi.bill.patientEncounter.discharged=true "
                + " and bi.retired=false"
                + " and bi.bill.patientEncounter.dateOfDischarge between :fd and :td "
                + " and bi.bill.billType=:bty"
                + " and bi.inwardChargeType=:inwty";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bty", BillType.PharmacyBhtPre);
        m.put("inwty", InwardChargeType.Medicine);
        if (admissionType != null) {
            sql = sql + " and bi.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and bi.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bi.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        sql += " order by bi.bill.patientEncounter.bhtNo";

        inwardMedicieneNetValue = BillItemFacade.findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

        return inwardMedicieneNetValue;

    }

    public void processInwardChargesGeneralIssuing() {
        String sql;
        Map m = new HashMap();
        sql = "select bi "
                + " from BillItem bi "
                + " where bi.bill.patientEncounter.discharged=true "
                + " and bi.retired=false"
                + " and bi.bill.patientEncounter.dateOfDischarge between :fd and :td "
                + " and bi.bill.billType=:bty"
                + " and bi.inwardChargeType=:inwty";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bty", BillType.StoreBhtPre);
        m.put("inwty", InwardChargeType.GeneralIssuing);
        if (admissionType != null) {
            sql = sql + " and bi.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and bi.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bi.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        sql += " order by bi.bill.patientEncounter.bhtNo";
        billItemGeneralIssuing = BillItemFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        totalOfProcessInwardChargesIssuing();

//        PatientEncounter pe = new PatientEncounter();
//        pe.getBhtNo();
        if (billFees == null) {
            billFees = new ArrayList<>();
        }

    }

    public double totalOfProcessInwardChargesIssuing() {
        String sql;
        Map m = new HashMap();
        sql = "select sum(bi.netValue) "
                + " from BillItem bi "
                + " where bi.bill.patientEncounter.discharged=true "
                + " and bi.retired=false"
                + " and bi.bill.patientEncounter.dateOfDischarge between :fd and :td "
                + " and bi.bill.billType=:bty"
                + " and bi.inwardChargeType=:inwty";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bty", BillType.StoreBhtPre);
        m.put("inwty", InwardChargeType.GeneralIssuing);
        if (admissionType != null) {
            sql = sql + " and bi.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and bi.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bi.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        sql += " order by bi.bill.patientEncounter.bhtNo";

        inwardGeneralIssuingNetValue = BillItemFacade.findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

        return inwardGeneralIssuingNetValue;

    }

    public void processInwardChargesOutSideCharges() {
        String sql;
        Map m = new HashMap();
        sql = "select bi "
                + " from BillItem bi "
                + " where bi.bill.patientEncounter.discharged=true "
                + " and bi.retired=false"
                + " and bi.bill.patientEncounter.dateOfDischarge between :fd and :td "
                + " and bi.bill.billType=:bty";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bty", BillType.InwardOutSideBill);
        if (admissionType != null) {
            sql = sql + " and bi.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and bi.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bi.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        sql += " order by bi.bill.patientEncounter.bhtNo";
        ////// // System.out.println("m = " + m);
        ////// // System.out.println("sql = " + sql);
        billItemOutSide = BillItemFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        ////// // System.out.println("billItemOutSide = " + billItemOutSide);
        totalOfProcessInwardChargesOutSideCharges();

//        PatientEncounter pe = new PatientEncounter();
//        pe.getBhtNo();
        if (billFees == null) {
            billFees = new ArrayList<>();
        }

    }

    public double totalOfProcessInwardChargesOutSideCharges() {
        String sql;
        Map m = new HashMap();
        sql = "select sum(bi.netValue) "
                + " from BillItem bi "
                + " where bi.bill.patientEncounter.discharged=true "
                + " and bi.retired=false"
                + " and bi.bill.patientEncounter.dateOfDischarge between :fd and :td "
                + " and bi.bill.billType=:bty";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bty", BillType.InwardOutSideBill);

        if (admissionType != null) {
            sql = sql + " and bi.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and bi.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bi.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        sql += " order by bi.bill.patientEncounter.bhtNo";

        inwardOutSideNetValue = BillItemFacade.findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

        return inwardOutSideNetValue;

    }

    ////////////GETTERS AND SETTERS
    public List<String1Value2> getTimedServices() {
        return timedServices;
    }

    public void setTimedServices(List<String1Value2> timedServices) {
        this.timedServices = timedServices;
    }

    public List<RoomChargeInward> getRoomChargeInwards() {
        return roomChargeInwards;
    }

    public void setRoomChargeInwards(List<RoomChargeInward> roomChargeInwards) {
        this.roomChargeInwards = roomChargeInwards;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<OpdService> getOpdServices() {
        return opdServices;
    }

    public void setOpdServices(List<OpdService> opdServices) {
        this.opdServices = opdServices;
    }

    public List<String1Value2> getProfessionals() {
        return professionals;
    }

    public void setProfessionals(List<String1Value2> professionals) {
        this.professionals = professionals;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public List<String2Value4> getInwardCharges() {
        return inwardCharges;
    }

    public void setInwardCharges(List<String2Value4> inwardCharges) {
        this.inwardCharges = inwardCharges;
    }

    public PatientRoomFacade getPatientRoomFacade() {
        return patientRoomFacade;
    }

    public void setPatientRoomFacade(PatientRoomFacade patientRoomFacade) {
        this.patientRoomFacade = patientRoomFacade;
    }

    public double getOpdSrviceGross() {
        return opdSrviceGross;
    }

    public void setOpdSrviceGross(double opdSrviceGross) {
        this.opdSrviceGross = opdSrviceGross;
    }

    public double getOpdServiceMargin() {
        return opdServiceMargin;
    }

    public void setOpdServiceMargin(double opdServiceMargin) {
        this.opdServiceMargin = opdServiceMargin;
    }

    public double getOpdServiceDiscount() {
        return opdServiceDiscount;
    }

    public void setOpdServiceDiscount(double opdServiceDiscount) {
        this.opdServiceDiscount = opdServiceDiscount;
    }

    public double getRoomGross() {
        return roomGross;
    }

    public void setRoomGross(double roomGross) {
        this.roomGross = roomGross;
    }

    public double getRoomDiscount() {
        return roomDiscount;
    }

    public void setRoomDiscount(double roomDiscount) {
        this.roomDiscount = roomDiscount;
    }

    public double getTimedGross() {
        return timedGross;
    }

    public void setTimedGross(double timedGross) {
        this.timedGross = timedGross;
    }

    public double getTimedDiscount() {
        return timedDiscount;
    }

    public void setTimedDiscount(double timedDiscount) {
        this.timedDiscount = timedDiscount;
    }

    public double getProfessionalGross() {
        return professionalGross;
    }

    public void setProfessionalGross(double professionalGross) {
        this.professionalGross = professionalGross;
    }

    public double getInwardGross() {
        return inwardGross;
    }

    public void setInwardGross(double inwardGross) {
        this.inwardGross = inwardGross;
    }

    public double getInwardMargin() {
        return inwardMargin;
    }

    public void setInwardMargin(double inwardMargin) {
        this.inwardMargin = inwardMargin;
    }

    public double getInwardDiscount() {
        return inwardDiscount;
    }

    public void setInwardDiscount(double inwardDiscount) {
        this.inwardDiscount = inwardDiscount;
    }

    public List<Bill> getPaidbyPatient() {
        return paidbyPatient;
    }

    public void setPaidbyPatient(List<Bill> paidbyPatient) {
        this.paidbyPatient = paidbyPatient;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public double getPaidbyPatientTotalValue() {
        return paidbyPatientTotalValue;
    }

    public void setPaidbyPatientTotalValue(double paidbyPatientTotalValue) {
        this.paidbyPatientTotalValue = paidbyPatientTotalValue;
    }

    public double getCreditPaymentTotalValue() {
        return creditPaymentTotalValue;
    }

    public void setCreditPaymentTotalValue(double creditPaymentTotalValue) {
        this.creditPaymentTotalValue = creditPaymentTotalValue;
    }

    //DATA STRUCTURE
    public class OpdService {

        PatientEncounter bht;
        Category category;
        double grossValue;
        double discount;
        double margin;
        double netValue;
        Long count;

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }

        public PatientEncounter getBht() {
            return bht;
        }

        public void setBht(PatientEncounter bht) {
            this.bht = bht;
        }

        public Category getCategory() {
            return category;
        }

        public void setCategory(Category category) {
            this.category = category;
        }

        public double getGrossValue() {
            return grossValue;
        }

        public void setGrossValue(double grossValue) {
            this.grossValue = grossValue;
        }

        public double getDiscount() {
            return discount;
        }

        public void setDiscount(double discount) {
            this.discount = discount;
        }

        public double getMargin() {
            return margin;
        }

        public void setMargin(double margin) {
            this.margin = margin;
        }

        public double getNetValue() {
            return netValue;
        }

        public void setNetValue(double netValue) {
            this.netValue = netValue;
        }

    }

    public class RoomChargeInward {

        InwardChargeType inwardChargeType;
        double calculated;
        double discount;
        double addition;

        public InwardChargeType getInwardChargeType() {
            return inwardChargeType;
        }

        public void setInwardChargeType(InwardChargeType inwardChargeType) {
            this.inwardChargeType = inwardChargeType;
        }

        public double getCalculated() {
            return calculated;
        }

        public void setCalculated(double calculated) {
            this.calculated = calculated;
        }

        public double getDiscount() {
            return discount;
        }

        public void setDiscount(double discount) {
            this.discount = discount;
        }

        public double getAddition() {
            return addition;
        }

        public void setAddition(double addition) {
            this.addition = addition;
        }

    }

    public double getOpdServiceNetValue() {
        return opdServiceNetValue;
    }

    public void setOpdServiceNetValue(double opdServiceNetValue) {
        this.opdServiceNetValue = opdServiceNetValue;
    }

    public double getInwardNetValue() {
        return inwardNetValue;
    }

    public void setInwardNetValue(double inwardNetValue) {
        this.inwardNetValue = inwardNetValue;
    }

    public BillItemFacade getBillItemFacade() {
        return BillItemFacade;
    }

    public void setBillItemFacade(BillItemFacade BillItemFacade) {
        this.BillItemFacade = BillItemFacade;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<BillFee> getBillFees() {
        return billFees;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public double getBillFreeGross() {
        return billFreeGross;
    }

    public void setBillFreeGross(double billFreeGross) {
        this.billFreeGross = billFreeGross;
    }

    public double getBillFeeMargin() {
        return billFeeMargin;
    }

    public void setBillFeeMargin(double billFeeMargin) {
        this.billFeeMargin = billFeeMargin;
    }

    public double getBillFeeDiscount() {
        return billFeeDiscount;
    }

    public void setBillFeeDiscount(double billFeeDiscount) {
        this.billFeeDiscount = billFeeDiscount;
    }

    public double getBillFeeNet() {
        return billFeeNet;
    }

    public void setBillFeeNet(double billFeeNet) {
        this.billFeeNet = billFeeNet;
    }

    public List<String1Value2> getFinalValues() {
        return finalValues;
    }

    public void setFinalValues(List<String1Value2> finalValues) {
        this.finalValues = finalValues;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public Date getFromDatePaid() {
        if (fromDatePaid == null) {
            fromDatePaid = getCommonFunctions().getStartOfDay(new Date());
        }

        return fromDatePaid;
    }

    public void setFromDatePaid(Date fromDatePaid) {
        this.fromDatePaid = fromDatePaid;
    }

    public Date getToDatePaid() {
        if (toDatePaid == null) {
            toDatePaid = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDatePaid;
    }

    public void setToDatePaid(Date toDatePaid) {
        this.toDatePaid = toDatePaid;
    }

    public class CategoryTime {

        Category roomCategory;
        private double time;
        private double calculated;
        private double added;

        public Category getRoomCategory() {
            return roomCategory;
        }

        public void setRoomCategory(Category roomCategory) {
            this.roomCategory = roomCategory;
        }

        public double getTime() {
            return time;
        }

        public void setTime(double time) {
            this.time = time;
        }

        public double getCalculated() {
            return calculated;
        }

        public void setCalculated(double calculated) {
            this.calculated = calculated;
        }

        public double getAdded() {
            return added;
        }

        public void setAdded(double added) {
            this.added = added;
        }

    }

    public class ItemRateRow {

        Item item;
        double rate;

        public Item getItem() {
            return item;
        }

        public void setItem(Item item) {
            this.item = item;
        }

        public double getRate() {
            return rate;
        }

        public void setRate(double rate) {
            this.rate = rate;
        }

        public ItemRateRow() {
        }

        public ItemRateRow(Item item, double rate) {
            this.item = item;
            this.rate = rate;
        }

    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }
    
}
