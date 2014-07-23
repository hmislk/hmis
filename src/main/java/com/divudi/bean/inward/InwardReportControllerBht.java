/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.inward;

import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.data.table.String1Value2;
import com.divudi.data.table.String2Value4;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.Category;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.PatientItem;
import com.divudi.entity.Speciality;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PatientItemFacade;
import com.divudi.facade.PatientRoomFacade;
import java.io.Serializable;
import java.util.ArrayList;
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
 * @author pdhs
 */
@Named
@SessionScoped
public class InwardReportControllerBht implements Serializable {

    PatientEncounter patientEncounter;
    Bill bill;
    private AdmissionType admissionType;
    private PaymentMethod paymentMethod;
    private Institution institution;
    private List<OpdService> opdServices;
    List<String1Value2> timedServices;
    List<RoomChargeInward> roomChargeInwards;
    List<String1Value2> professionals;
    List<String2Value4> inwardCharges;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    PatientItemFacade patientItemFacade;
    @EJB
    PatientRoomFacade patientRoomFacade;
    double opdSrviceGross;
    double opdServiceMargin;
    double opdServiceDiscount;
    private double opdNetTotal;
    double roomGross;
    double roomDiscount;
    double timedGross;
    double timedDiscount;
    double professionalGross;
    double inwardGross;
    double inwardMargin;
    double inwardDiscount;
    double inwardNetValue;
    double total;
    double discount;
    double netTotal;

    Bill finalBill;

    public double fetchPatientRoom_AdminCalculated() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.calculatedAdministrationCharge-pr.addedAdministrationCharge) "
                + " FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";

        hm.put("bhtno", patientEncounter);

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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_AdminDiscount() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.discountAdministrationCharge) "
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);
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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_AdminAddition() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.addedAdministrationCharge) "
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);
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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_MedicalCalculated() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.calculatedMedicalCareCharge-pr.addedMedicalCareCharge) "
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";

        hm.put("bhtno", patientEncounter);
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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_MedicalDiscount() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.discountMedicalCareCharge) "
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";

        hm.put("bhtno", patientEncounter);
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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_MedicalAddition() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.addedMedicalCareCharge) "
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";

        hm.put("bhtno", patientEncounter);
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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_LinenCalculated() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.calculatedLinenCharge-pr.addedLinenCharge) "
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";

        hm.put("bhtno", patientEncounter);
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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_LinenDiscount() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.discountLinenCharge) "
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";

        hm.put("bhtno", patientEncounter);
        if (admissionType != null) {
            sql = sql + " and pr.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);
        }

//        if (paymentMethod != null) {
//            sql = sql + " and pr.patientEncounter.paymentMethod=:bt ";
//            hm.put("bt", paymentMethod);
//        }
        if (institution != null) {
            sql = sql + " and pr.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_LinenAddition() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.addedLinenCharge) "
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";

        hm.put("bhtno", patientEncounter);
        if (admissionType != null) {
            sql = sql + " and pr.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);
        }

//        if (paymentMethod != null) {
//            sql = sql + " and pr.patientEncounter.paymentMethod=:bt ";
//            hm.put("bt", paymentMethod);
//        }
        if (institution != null) {
            sql = sql + " and pr.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_NursingCalculated() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                ////////////////
                + " sum(pr.calculatedNursingCharge-pr.addedNursingCharge) "
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";

        hm.put("bhtno", patientEncounter);
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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_NursingDiscount() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.discountNursingCharge) "
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";

        hm.put("bhtno", patientEncounter);
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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_NursingAddition() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.addedNursingCharge) "
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";

        hm.put("bhtno", patientEncounter);
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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_MoDiscount() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.discountMoCharge) "
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";

        hm.put("bhtno", patientEncounter);
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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_MoAddition() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.addedMoCharge) "
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";

        hm.put("bhtno", patientEncounter);
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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_MoCalculated() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                ////////////////////
                + " sum(pr.calculatedMoCharge-pr.addedMoCharge)"
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";

        hm.put("bhtno", patientEncounter);
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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_MaintainDiscount() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.discountMaintainCharge) "
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);

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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_MaintainAddition() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.addedMaintainCharge) "
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);

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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_MaintainCalculated() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.calculatedMaintainCharge-pr.addedMaintainCharge)"
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";

        hm.put("bhtno", patientEncounter);

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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_RoomDiscount() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.discountRoomCharge)"
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);

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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_RoomAddition() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.addedRoomCharge)"
                ////////////////
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);

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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double fetchPatientRoom_RoomCalculated() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.calculatedRoomCharge-pr.addedRoomCharge)"
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);

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

        return patientRoomFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchDoctorPaymentInward() {
        HashMap hm = new HashMap();
        String sql = "Select b.paidForBillFee.staff.speciality,"
                + " sum(b.paidForBillFee.feeValue) "
                + " FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.paidForBillFee.bill.patientEncounter.paymentFinalized=true "
                + " and(b.paidForBillFee.bill.billType=:refType1 "
                + " or b.paidForBillFee.bill.billType=:refType2 )"
                + " and b.paidForBillFee.bill.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);

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

        return billFeeFacade.findAggregates(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchDoctorPaymentInwardModified() {
        HashMap hm = new HashMap();
        String sql = "Select b.staff.speciality,"
                + " sum(b.feeValue) "
                + " FROM BillFee b "
                + " where b.retired=false "
                + " and b.bill.patientEncounter.paymentFinalized=true "
                + " and(b.bill.billType=:refType1 "
                + " or b.bill.billType=:refType2 )"
                + " and b.bill.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);

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

        return billFeeFacade.findAggregates(sql, hm, TemporalType.TIMESTAMP);

    }

    public void createDoctorPaymentInward() {
        professionals = new ArrayList<>();
        List<Object[]> list = fetchDoctorPaymentInwardModified();
        System.err.println("Professional " + list);
        for (Object[] obj : list) {
            Speciality speciality = (Speciality) obj[0];
            double dbl = (Double) obj[1];

            String1Value2 string1Value2 = new String1Value2();
            string1Value2.setString(speciality.getName());
            string1Value2.setValue1(dbl);

            professionalGross += string1Value2.getValue1();

            professionals.add(string1Value2);

        }

    }
    
    public void createTimedService() {
        HashMap hm = new HashMap();
        String sql = "SELECT i.item,"
                + " sum(i.serviceValue),"
                + " sum(i.discount) "
                + " FROM PatientItem i "
                + " where i.retired=false "
                + " and i.patientEncounter.paymentFinalized=true "
                + " and i.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);

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

        List<Object[]> results = billFeeFacade.findAggregates(sql, hm, TemporalType.DATE);

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

    @EJB
    BillItemFacade billItemFacade;
    
   
    public void updateBillItem(BillItem billItem) {
        billItem.setNetValue((billItem.getGrossValue() + billItem.getMarginValue()) - billItem.getDiscount());

        billItemFacade.edit(billItem);
    }
    
    public void updatePatientBillItem(PatientItem patientItem){
    
        patientItemFacade.edit(patientItem);
    }

    public void updateBillFee(BillFee billFee) {
        billFee.setFeeValue((billFee.getFeeGrossValue() + billFee.getFeeMargin()) - billFee.getFeeDiscount());

        billFeeFacade.edit(billFee);
    }

    public double fetchAdmissionFeeDiscount() {
        Bill b = inwardBeanController.fetchFinalBill(patientEncounter);
        HashMap hm = new HashMap();
        hm.put("inwTp", InwardChargeType.AdmissionFee);
        hm.put("btp", BillType.InwardFinalBill);
        String sql = "SELECT  sum(i.discount)"
                + " FROM BillItem i "
                + " where i.retired=false"
                + " and i.inwardChargeType=:inwTp"
                + " and i.bill.billType=:btp "
                + " and i.bill.patientEncounter.paymentFinalized=true "
                + " and i.bill.patientEncounter=:bhtno "
                + " and i.bill=:b ";
        hm.put("bhtno", patientEncounter);
        hm.put("b", b);

        if (admissionType != null) {
            sql = sql + " and i.bill.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and i.bill.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and i.bill.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        return billFeeFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    public double fetchAdmissionFeeNetValue() {
        Bill b = inwardBeanController.fetchFinalBill(patientEncounter);
        HashMap hm = new HashMap();
        hm.put("inwTp", InwardChargeType.AdmissionFee);
        hm.put("btp", BillType.InwardFinalBill);
        String sql = "SELECT  sum(i.netValue)"
                + " FROM BillItem i "
                + " where i.retired=false"
                + " and i.inwardChargeType=:inwTp"
                + " and i.bill.billType=:btp "
                + " and i.bill.patientEncounter.paymentFinalized=true "
                + " and i.bill.patientEncounter=:bhtno "
                + " and i.bill=:b ";
        hm.put("bhtno", patientEncounter);
        hm.put("b", b);

        if (admissionType != null) {
            sql = sql + " and i.bill.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and i.bill.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and i.bill.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        return billFeeFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    public double fetchAdmissionFeeGross() {
        Bill b = inwardBeanController.fetchFinalBill(patientEncounter);
        HashMap hm = new HashMap();
        hm.put("inwTp", InwardChargeType.AdmissionFee);
        hm.put("btp", BillType.InwardFinalBill);
        String sql = "SELECT  sum(i.grossValue)"
                + " FROM BillItem i "
                + " where i.retired=false"
                + " and i.inwardChargeType=:inwTp"
                + " and i.bill.billType=:btp "
                + " and i.bill.patientEncounter.paymentFinalized=true "
                + " and i.bill.patientEncounter=:bhtno "
                + " and i.bill=:b ";
        hm.put("bhtno", patientEncounter);
        hm.put("b", b);
        if (admissionType != null) {
            sql = sql + " and i.bill.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and i.bill.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and i.bill.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        return billFeeFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    public double fetchOutSideFee() {
        HashMap hm = new HashMap();

        hm.put("btp", BillType.InwardOutSideBill);
        String sql = "SELECT  sum(i.netTotal)"
                + " FROM Bill i "
                + " where i.retired=false"
                + " and i.billType=:btp "
                + " and i.patientEncounter.paymentFinalized=true "
                + " and i.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);

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

        return billFeeFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    public double fetchAdmissionFee() {
        HashMap hm = new HashMap();
        String sql = "SELECT  sum(i.admissionType.admissionFee)"
                + " FROM PatientEncounter i "
                + " where i.retired=false "
                + " and i.paymentFinalized=true "
                + " and i..bhtno=:bhtno ";
        hm.put("bhtno", patientEncounter);

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

        return billFeeFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    public double fetchMadicineGross() {
        String sql;
        HashMap hm = new HashMap();
        sql = "SELECT  sum(b.grossValue)"
                + " FROM BillItem b "
                + " WHERE b.retired=false "
                + " and (b.bill.billType=:btp1 "
                + " or  b.bill.billType=:btp2)"
                + " and b.bill.patientEncounter.paymentFinalized=true "
                + " and b.bill.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);

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

        hm.put("btp1", BillType.PharmacyBhtPre);
        hm.put("btp2", BillType.StoreBhtPre);

        return billFeeFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    public double fetchMadicineMargin() {
        String sql;
        HashMap hm = new HashMap();
        sql = "SELECT  sum(b.marginValue)"
                + " FROM BillItem b "
                + " WHERE b.retired=false "
                + " and (b.bill.billType=:btp1 "
                + " or  b.bill.billType=:btp2)"
                + " and b.bill.patientEncounter.paymentFinalized=true "
                + " and b.bill.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);

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

        hm.put("btp1", BillType.PharmacyBhtPre);
        hm.put("btp2", BillType.StoreBhtPre);

        return billFeeFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    public double fetchMadicineDiscount() {
        String sql;
        HashMap hm = new HashMap();
        sql = "SELECT  sum(b.discount)"
                + " FROM BillItem b "
                + " WHERE b.retired=false "
                + " and (b.bill.billType=:btp1 "
                + " or  b.bill.billType=:btp2)"
                + " and b.bill.patientEncounter.paymentFinalized=true "
                + " and b.bill.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);

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

        hm.put("btp1", BillType.PharmacyBhtPre);
        hm.put("btp2", BillType.StoreBhtPre);

        return billFeeFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    public double fetchMadicineNetValue() {
        String sql;
        HashMap hm = new HashMap();
        sql = "SELECT  sum(b.netValue)"
                + " FROM BillItem b "
                + " WHERE b.retired=false "
                + " and (b.bill.billType=:btp1 "
                + " or  b.bill.billType=:btp2)"
                + " and b.bill.patientEncounter.paymentFinalized=true "
                + " and b.bill.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);

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

        hm.put("btp1", BillType.PharmacyBhtPre);
        hm.put("btp2", BillType.StoreBhtPre);

        return billFeeFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    public void createInwardService() {
        inwardCharges = new ArrayList<>();

        String2Value4 string1Value3 = new String2Value4();
        string1Value3.setString(InwardChargeType.AdmissionFee.getLabel());
        string1Value3.setValue1(fetchAdmissionFeeGross());
        string1Value3.setValue3(fetchAdmissionFeeDiscount());
        string1Value3.setValue4(fetchAdmissionFeeNetValue());
        inwardGross += string1Value3.getValue1();
        inwardDiscount += string1Value3.getValue3();
        inwardNetValue += string1Value3.getValue4();
        inwardCharges.add(string1Value3);

        string1Value3 = new String2Value4();
        string1Value3.setString(InwardChargeType.Medicine.getLabel());
        string1Value3.setValue1(fetchMadicineGross());
        string1Value3.setValue2(fetchMadicineMargin());
        string1Value3.setValue3(fetchMadicineDiscount());
        string1Value3.setValue4(fetchMadicineNetValue());
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

    public void createOpdServiceWithoutPro() {
        String sql;
        Map m = new HashMap();
        sql = "select bf.billItem.item.category, "
                + " sum(bf.feeDiscount),"
                + " sum(bf.feeMargin),"
                + " sum(bf.feeGrossValue),"
                + " sum(bf.feeValue)"
                + " from BillFee bf "
                + " where bf.bill.patientEncounter.paymentFinalized=true "
                + " and bf.retired=false"
                + " and bf.billItem.retired=false "
                + " and bf.fee.feeType!=:ftp "
                + " and bf.bill.patientEncounter=:bhtno ";

        m.put("ftp", FeeType.Staff);
        m.put("billType", BillType.InwardBill);
        m.put("bhtno", patientEncounter);
        sql = sql + " and bf.bill.billType=:billType ";

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

        sql = sql + " group by bf.billItem.item.category order by bf.billItem.item.category.name";
        List<Object[]> results = billFeeFacade.findAggregates(sql, m);

        System.out.println("results = " + results);

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

            opdSrviceGross += row.getGrossValue();
            opdServiceMargin += row.getMargin();
            opdServiceDiscount += row.getDiscount();
            opdNetTotal += row.getNetValue();

            opdServices.add(row);

        }

    }

    public void createOpdServiceWithoutPro2() {
        String sql;
        Map m = new HashMap();
        sql = "select bi.item.category, "
                + " sum(bi.discount),"
                + " sum(bi.marginValue),"
                + " sum(bi.grossValue),"
                + " sum(bi.netValue)"
                + " from BillItem bi join BillFee bf on bi.id=bf.billItem.id "
                + " where bi.bill.patientEncounter.paymentFinalized=true "
                + " and bi.retired=false "
                + " and bi.bill.retired=false "
                + " and bi.bill.patientEncounter=:bhtno "
                + " and bf.retired=false "
                + " and bf.fee.feeType!=:ftp ";

        m.put("ftp", FeeType.Staff);
        m.put("billType", BillType.InwardBill);
        m.put("bhtno", patientEncounter);
        sql = sql + " and bi.bill.billType=:billType ";

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

        sql = sql + " group by bi.item.category "
                + " order by bi.item.category.name";
        List<Object[]> results = billFeeFacade.findAggregates(sql, m, TemporalType.DATE);

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

            opdSrviceGross += row.getGrossValue();
            opdServiceMargin += row.getMargin();
            opdServiceDiscount += row.getDiscount();

            opdServices.add(row);

        }

    }

    double roomAddition;

    public void createRoomTable() {
        roomChargeInwards = new ArrayList<>();

        RoomChargeInward row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.RoomCharges);

        row.setCalculated(fetchPatientRoom_RoomCalculated());
        row.setDiscount(fetchPatientRoom_RoomDiscount());
        row.setAddition(fetchPatientRoom_RoomAddition());
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomAddition += row.getAddition();
        roomChargeInwards.add(row);
        ///////////////
        row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.MaintainCharges);
        row.setCalculated(fetchPatientRoom_MaintainCalculated());
        row.setDiscount(fetchPatientRoom_MaintainDiscount());
        row.setAddition(fetchPatientRoom_MaintainAddition());
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomAddition += row.getAddition();
        roomChargeInwards.add(row);
        ///////////////
        row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.MOCharges);
        row.setCalculated(fetchPatientRoom_MoCalculated());
        row.setDiscount(fetchPatientRoom_MoDiscount());
        row.setAddition(fetchPatientRoom_MoAddition());
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomAddition += row.getAddition();
        roomChargeInwards.add(row);
        ///////////////
        row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.NursingCharges);
        row.setCalculated(fetchPatientRoom_NursingCalculated());
        row.setDiscount(fetchPatientRoom_NursingDiscount());
        row.setAddition(fetchPatientRoom_NursingAddition());
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomAddition += row.getAddition();
        roomChargeInwards.add(row);
        ///////////////
        row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.LinenCharges);
        row.setCalculated(fetchPatientRoom_LinenCalculated());
        row.setDiscount(fetchPatientRoom_LinenDiscount());
        row.setAddition(fetchPatientRoom_LinenAddition());
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomAddition += row.getAddition();
        roomChargeInwards.add(row);
        ///////////////
        row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.AdministrationCharge);
        row.setCalculated(fetchPatientRoom_AdminCalculated());
        row.setDiscount(fetchPatientRoom_AdminDiscount());
        row.setAddition(fetchPatientRoom_AdminAddition());
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomAddition += row.getAddition();
        roomChargeInwards.add(row);
        ///////////////
        row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.MedicalCareICU);
        row.setCalculated(fetchPatientRoom_MedicalCalculated());
        row.setDiscount(fetchPatientRoom_MedicalDiscount());
        row.setAddition(fetchPatientRoom_MedicalAddition());
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomAddition += row.getAddition();
        roomChargeInwards.add(row);
        ///////////////

    }

    public void makeNull() {
        opdSrviceGross = 0;
        opdServiceMargin = 0;
        opdServiceDiscount = 0;
        opdNetTotal = 0;
        roomGross = 0;
        roomDiscount = 0;
        roomAddition = 0;
        finalBill = null;
        timedGross = 0;
        timedDiscount = 0;
        professionalGross = 0;
        inwardGross = 0;
        inwardMargin = 0;
        inwardDiscount = 0;
        inwardNetValue = 0;
    }

    public void selectLitener() {
        if (patientEncounter == null) {
            return;
        }

        bill = patientEncounter.getFinalBill();
    }

    @Inject
    InwardBeanController inwardBeanController;

    public void process() {
        makeNull();

        createOpdServiceWithoutPro();
        createRoomTable();
        createDoctorPaymentInward();
        createTimedService();
        createInwardService();
        finalBill = inwardBeanController.fetchFinalBill(patientEncounter);
        calTotal();
    }

    public void calTotal() {
        total = 0;
        discount = 0;
        netTotal = 0;

        total = opdSrviceGross + opdServiceMargin
                + roomGross + roomAddition
                + timedGross
                + professionalGross
                + inwardGross + inwardMargin;

        discount = opdServiceDiscount
                + roomDiscount
                + timedDiscount
                + inwardDiscount;

        netTotal = opdNetTotal
                + roomGross + roomAddition - roomDiscount
                + timedGross - timedDiscount
                + professionalGross
                + inwardNetValue;
    }

    public void process2() {
        makeNull();

        createOpdServiceWithoutPro2();
        createRoomTable();
        createDoctorPaymentInward();
        createTimedService();
        createInwardService();
        finalBill = inwardBeanController.fetchFinalBill(patientEncounter);

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

    public double getOpdNetTotal() {
        return opdNetTotal;
    }

    public void setOpdNetTotal(double opdNetTotal) {
        this.opdNetTotal = opdNetTotal;
    }

    //DATA STRUCTURE
    public class OpdService {

        PatientEncounter bht;
        Category category;
        double grossValue;
        double discount;
        double margin;
        double netValue;

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

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Bill getFinalBill() {
        return finalBill;
    }

    public void setFinalBill(Bill finalBill) {
        this.finalBill = finalBill;
    }

    public double getRoomAddition() {
        return roomAddition;
    }

    public void setRoomAddition(double roomAddition) {
        this.roomAddition = roomAddition;
    }

    public InwardBeanController getInwardBeanController() {
        return inwardBeanController;
    }

    public void setInwardBeanController(InwardBeanController inwardBeanController) {
        this.inwardBeanController = inwardBeanController;
    }

    public double getInwardNetValue() {
        return inwardNetValue;
    }

    public void setInwardNetValue(double inwardNetValue) {
        this.inwardNetValue = inwardNetValue;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public PatientItemFacade getPatientItemFacade() {
        return patientItemFacade;
    }

    public void setPatientItemFacade(PatientItemFacade patientItemFacade) {
        this.patientItemFacade = patientItemFacade;
    }
    
    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
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

}
