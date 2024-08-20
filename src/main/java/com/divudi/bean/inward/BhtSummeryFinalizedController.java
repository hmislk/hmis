/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.pharmacy.BhtIssueReturnController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.inward.InwardChargeType;

import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.ItemFee;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.PatientItem;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.entity.inward.PatientRoom;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PatientItemFacade;
import com.divudi.facade.PatientRoomFacade;
import com.divudi.facade.SpecialityFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
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
 * @author safrin
 */
@Named
@SessionScoped
public class BhtSummeryFinalizedController implements Serializable {

    @EJB
    BillFacade billfacade;
    @EJB
    BillFeeFacade billFeeFacade;

    PatientEncounter patientEncounter;
    List<PatientRoom> patientRooms;
    List<PatientItem> patientItems;
    List<BillItem> billItems;
    List<BillFee> billFees;
    List<BillItem> pharmacyItems;
    List<BillItem> storeItems;
    List<BillFee> proBillFee;
    List<BillFee> assistBillFee;
    List<Bill> outSideBills;
    List<Bill> paymentBills;
    List<Bill> paidbyPatientBillList;
    List<BillItem> creditPayment;
    List<BillItem> billItemsInward;
    List<bhtWithVat> bhtWithVats;

    public BhtSummeryFinalizedController(BillFacade billfacade, BillFeeFacade billFeeFacade, PatientEncounter patientEncounter, List<PatientRoom> patientRooms, List<PatientItem> patientItems, List<BillItem> billItems, List<BillFee> billFees, List<BillItem> pharmacyItems, List<BillItem> storeItems, List<BillFee> proBillFee, List<BillFee> assistBillFee, List<Bill> outSideBills, List<Bill> paymentBills, List<Bill> paidbyPatientBillList, List<BillItem> creditPayment, List<BillItem> billItemsInward, Date fromDate, Date toDate, CommonFunctions commonFunctions, AdmissionType admissionType, InwardChargeType inwardChargeType, Bill bill, InwardBeanController inwardBean, InwardReportControllerBht inwardReportControllerBht, CommonController commonController, double billItemGross, double billItemMargin, double billItemDiscount, double billItemNetValue, double billItemGrossPharmacy, double billItemMarginPharmacy, double billItemDiscountPharmacy, double billFeeGross, double billFeeMargin, double billFeeDiscount, double billFeeNetValue, double paidbyPatientTotalValue, double creditCompanyPaymentTotal, PatientRoomFacade patientRoomFacade, double totalGross, double totalDiscount, double totalNet, double totalVat, double totalMed, PatientEncounterFacade patientEncounterFacade, List<BillFee> fillteredFees, List<BillItem> filterItems, BillItemFacade billItemFacade, SpecialityFacade specialityFacade, StaffFacade staffFacade, BillBeanController billBeanController, ItemFeeFacade itemFeeFacade, BhtIssueReturnController bhtIssueReturnController, SessionController sessionController, PatientItemFacade patientItemFacade) {
        this.billfacade = billfacade;
        this.billFeeFacade = billFeeFacade;
        this.patientEncounter = patientEncounter;
        this.patientRooms = patientRooms;
        this.patientItems = patientItems;
        this.billItems = billItems;
        this.billFees = billFees;
        this.pharmacyItems = pharmacyItems;
        this.storeItems = storeItems;
        this.proBillFee = proBillFee;
        this.assistBillFee = assistBillFee;
        this.outSideBills = outSideBills;
        this.paymentBills = paymentBills;
        this.paidbyPatientBillList = paidbyPatientBillList;
        this.creditPayment = creditPayment;
        this.billItemsInward = billItemsInward;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.commonFunctions = commonFunctions;
        this.admissionType = admissionType;
        this.inwardChargeType = inwardChargeType;
        this.bill = bill;
        this.inwardBean = inwardBean;
        this.inwardReportControllerBht = inwardReportControllerBht;
        this.commonController = commonController;
        this.billItemGross = billItemGross;
        this.billItemMargin = billItemMargin;
        this.billItemDiscount = billItemDiscount;
        this.billItemNetValue = billItemNetValue;
        this.billItemGrossPharmacy = billItemGrossPharmacy;
        this.billItemMarginPharmacy = billItemMarginPharmacy;
        this.billItemDiscountPharmacy = billItemDiscountPharmacy;
        this.billFeeGross = billFeeGross;
        this.billFeeMargin = billFeeMargin;
        this.billFeeDiscount = billFeeDiscount;
        this.billFeeNetValue = billFeeNetValue;
        this.paidbyPatientTotalValue = paidbyPatientTotalValue;
        this.creditCompanyPaymentTotal = creditCompanyPaymentTotal;
        this.patientRoomFacade = patientRoomFacade;
        this.totalGross = totalGross;
        this.totalDiscount = totalDiscount;
        this.totalNet = totalNet;
        this.totalVat = totalVat;
        this.totalMed = totalMed;
        this.patientEncounterFacade = patientEncounterFacade;
        this.fillteredFees = fillteredFees;
        this.filterItems = filterItems;
        this.billItemFacade = billItemFacade;
        this.specialityFacade = specialityFacade;
        this.staffFacade = staffFacade;
        this.billBeanController = billBeanController;
        this.itemFeeFacade = itemFeeFacade;
        this.bhtIssueReturnController = bhtIssueReturnController;
        this.sessionController = sessionController;
        this.patientItemFacade = patientItemFacade;
    }

    @Temporal(TemporalType.TIMESTAMP)
    private Date fromDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date toDate;

    private CommonFunctions commonFunctions;

    AdmissionType admissionType;
    InwardChargeType inwardChargeType;
    Bill bill;
    @Inject
    InwardBeanController inwardBean;
    @Inject
    InwardReportControllerBht inwardReportControllerBht;
    @Inject
    CommonController commonController;
    double billItemGross;
    double billItemMargin;
    double billItemDiscount;
    double billItemNetValue;
    double billItemGrossPharmacy;
    double billItemMarginPharmacy;
    double billItemDiscountPharmacy;
    double billFeeGross;
    double billFeeMargin;
    double billFeeDiscount;
    double billFeeNetValue;
    double paidbyPatientTotalValue;
    double creditCompanyPaymentTotal;

    boolean activeBackButton = false;
    PaymentMethod paymentMethod;

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

    public SpecialityFacade getSpecialityFacade() {
        return specialityFacade;
    }

    public void setSpecialityFacade(SpecialityFacade specialityFacade) {
        this.specialityFacade = specialityFacade;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public InwardReportControllerBht getInwardReportControllerBht() {
        return inwardReportControllerBht;
    }

    public void setInwardReportControllerBht(InwardReportControllerBht inwardReportControllerBht) {
        this.inwardReportControllerBht = inwardReportControllerBht;
    }

    public BillBeanController getBillBeanController() {
        return billBeanController;
    }

    public void setBillBeanController(BillBeanController billBeanController) {
        this.billBeanController = billBeanController;
    }

    public double getBillItemGross() {
        return billItemGross;
    }

    public void setBillItemGross(double billItemGross) {
        this.billItemGross = billItemGross;
    }

    public double getBillItemMargin() {
        return billItemMargin;
    }

    public void setBillItemMargin(double billItemMargin) {
        this.billItemMargin = billItemMargin;
    }

    public double getBillFeeGross() {
        return billFeeGross;
    }

    public void setBillFeeGross(double billFeeGross) {
        this.billFeeGross = billFeeGross;
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

    public double getBillItemDiscount() {
        return billItemDiscount;
    }

    public void setBillItemDiscount(double billItemDiscount) {
        this.billItemDiscount = billItemDiscount;
    }

    public BillFacade getBillfacade() {
        return billfacade;
    }

    public void setBillfacade(BillFacade billfacade) {
        this.billfacade = billfacade;
    }

    public List<Bill> getPaidbyPatientBillList() {
        return paidbyPatientBillList;
    }

    public void setPaidbyPatientBillList(List<Bill> paidbyPatientBillList) {
        this.paidbyPatientBillList = paidbyPatientBillList;
    }

    public double getPaidbyPatientTotalValue() {
        return paidbyPatientTotalValue;
    }

    public void setPaidbyPatientTotalValue(double paidbyPatientTotalValue) {
        this.paidbyPatientTotalValue = paidbyPatientTotalValue;
    }

    public List<BillItem> getCreditPayment() {
        return creditPayment;
    }

    public void setCreditPayment(List<BillItem> creditPayment) {
        this.creditPayment = creditPayment;
    }

    public double getCreditCompanyPaymentTotal() {
        return creditCompanyPaymentTotal;
    }

    public void setCreditCompanyPaymentTotal(double creditCompanyPaymentTotal) {
        this.creditCompanyPaymentTotal = creditCompanyPaymentTotal;
    }

    @EJB
    PatientRoomFacade patientRoomFacade;

    public void updateRoom(PatientRoom rm) {

        patientRoomFacade.edit(rm);

    }

    public void updateAllBillItems() {
        if (filterItems == null) {
            return;
        }

        for (BillItem b : filterItems) {
            inwardReportControllerBht.updateBillItem(b);
        }
    }

    public void updateAllBillFees() {
        if (filterItems == null) {
            return;
        }

        for (BillItem b : filterItems) {
            for (BillFee bf : b.getBillFees()) {
                inwardReportControllerBht.updateBillFee(bf);
            }
        }
    }

    public void calculateService() {
        if (filterItems == null) {
            return;
        }

        billItemGross = 0;
        billItemNetValue = 0;
        billFeeNetValue = 0;
        billItemMargin = 0;
        billItemDiscount = 0;
        billFeeGross = 0;
        billFeeMargin = 0;
        billFeeDiscount = 0;

        for (BillItem b : filterItems) {
            billItemGross += b.getGrossValue();
            billItemMargin += b.getMarginValue();
            billItemDiscount += b.getDiscount();
            billItemNetValue += b.getNetValue();

            for (BillFee bf : b.getBillFees()) {
                if (!bf.isRetired()) {
                    billFeeGross += bf.getFeeGrossValue();
                    billFeeMargin += bf.getFeeMargin();
                    billFeeDiscount += bf.getFeeDiscount();
                    billFeeNetValue += bf.getFeeValue();
                }

            }
        }
    }

    public void calculatePharmacy() {
        billItemGrossPharmacy = 0;
        billItemMarginPharmacy = 0;
        billItemDiscountPharmacy = 0;

        for (BillItem b : pharmacyItems) {
            billItemGrossPharmacy += b.getGrossValue();
            billItemMarginPharmacy += b.getMarginValue();
            billItemDiscountPharmacy += b.getDiscount();

        }
    }

    public void calculateStore() {
        billItemGrossPharmacy = 0;
        billItemMarginPharmacy = 0;
        billItemDiscountPharmacy = 0;

        for (BillItem b : storeItems) {
            billItemGrossPharmacy += b.getGrossValue();
            billItemMarginPharmacy += b.getMarginValue();
            billItemDiscountPharmacy += b.getDiscount();

        }
    }

    public void delete(BillFee bf) {
        bf.setRetired(true);
        billFeeFacade.edit(bf);
    }

    public void updateBillFee(BillFee bf) {
        billFeeFacade.edit(bf);
    }

    public void updateBillItem(BillItem b) {
        billItemFacade.edit(b);
    }

    public InwardChargeType getInwardChargeType() {
        return inwardChargeType;
    }

    public void setInwardChargeType(InwardChargeType inwardChargeType) {
        this.inwardChargeType = inwardChargeType;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public void selectLitener() {
        if (patientEncounter == null) {
            return;
        }

        bill = patientEncounter.getFinalBill();
        createPaidByPatient();
        createCreditPayment();
    }

    double totalGross;
    double totalDiscount;
    double totalNet;
    double totalVat;
    double totalMed;
    double totalDoc;
    double totalHos;
    double totalRoom;

    public void createBhtInwardChargeTypeTable() {
        Date startTime = new Date();

        billItemsInward = new ArrayList<>();
        for (PatientEncounter pe : patientEncounters()) {
            bill = pe.getFinalBill();
            for (BillItem bi : bill.getBillItems()) {
                if (bi.getInwardChargeType() == inwardChargeType) {
                    billItemsInward.add(bi);
                }
            }
        }
        totalGross = 0.0;
        totalDiscount = 0.0;
        totalNet = 0.0;
        for (BillItem bi : billItemsInward) {
            totalGross += bi.getGrossValue();
            totalDiscount += bi.getDiscount();
            totalNet += bi.getNetValue();
        }

        

    }

    public void createBhtInwardVatTable() {
        Date startTime = new Date();

        bhtWithVats = new ArrayList<>();
        totalGross = 0.0;
        totalDiscount = 0.0;
        totalNet = 0.0;
        totalVat = 0.0;
        totalMed = 0.0;
        totalDoc = 0.0;
        totalHos = 0.0;
        totalRoom = 0.0;
        for (PatientEncounter pe : patientEncounters(admissionType,paymentMethod)) {
            bhtWithVat bwv = new bhtWithVat();
            double vat = 0.0;
            double med = 0.0;
            double doc = 0.0;
            double hos = 0.0;
            double room = 0.0;
            if (pe.getFinalBill()==null) {
                continue;
            }
            for (BillItem bi : pe.getFinalBill().getBillItems()) {
                if (bi.getInwardChargeType() == InwardChargeType.VAT || bi.getInwardChargeType() == InwardChargeType.HospitalSupportService) {
                    vat += bi.getNetValue();
                }
                if (bi.getInwardChargeType() == InwardChargeType.Medicine) {
                    med += bi.getNetValue();
                }
                if (bi.getInwardChargeType() == InwardChargeType.ProfessionalCharge || bi.getInwardChargeType() == InwardChargeType.DoctorAndNurses) {
                    doc += bi.getNetValue();
                }
                if (bi.getInwardChargeType() != InwardChargeType.ProfessionalCharge && bi.getInwardChargeType() != InwardChargeType.DoctorAndNurses
                        && bi.getInwardChargeType() != InwardChargeType.Medicine && bi.getInwardChargeType() != InwardChargeType.VAT
                        && bi.getInwardChargeType() != InwardChargeType.RoomCharges
                        && bi.getInwardChargeType() != InwardChargeType.HospitalSupportService) {
                    hos += bi.getNetValue();
                }
                if (bi.getInwardChargeType() == InwardChargeType.RoomCharges) {
                    room += bi.getNetValue();
                }
            }

            bwv.setPe(pe);
            bwv.setVat(vat);
            bwv.setMedicine(med);
            bwv.setDoc(doc);
            bwv.setHos(hos);
            bwv.setRoom(room);

            bhtWithVats.add(bwv);

            totalGross += pe.getFinalBill().getTotal();
            totalDiscount += pe.getFinalBill().getDiscount();
            totalNet += pe.getFinalBill().getNetTotal();
            totalVat += vat;
            totalMed += med;
            totalDoc += doc;
            totalHos += hos;
            totalRoom += room;
        }

        

    }

    @EJB
    PatientEncounterFacade patientEncounterFacade;

    public List<PatientEncounter> patientEncounters() {
        String sql;
        Map m = new HashMap();

        sql = "select c from Admission c where "
                + " c.retired=false "
                + " and c.paymentFinalized=true "
                + " and c.dateOfDischarge between :fd and :td "
                + " order by c.bhtNo ";

        m.put("fd", fromDate);
        m.put("td", toDate);

        return patientEncounterFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public List<PatientEncounter> patientEncounters(AdmissionType admissionType,PaymentMethod paymentMethod) {
        String sql;
        Map m = new HashMap();

        sql = "select c from Admission c where "
                + " c.retired=false "
                + " and c.paymentFinalized=true "
                + " and c.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql += " and c.admissionType=:at ";
            m.put("at", admissionType);
        }
        
        if (paymentMethod != null) {
            sql = sql + " and c.paymentMethod=:pm ";
            m.put("pm", paymentMethod);
        }

        sql += " order by c.bhtNo ";

        m.put("fd", fromDate);
        m.put("td", toDate);

        return patientEncounterFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public void createCreditPayment() {

        String sql;
        Map m = new HashMap();
        sql = "SELECT bi FROM BillItem bi "
                + " WHERE bi.retired=false "
                + " and bi.bill.billType=:bty"
                + " and bi.patientEncounter=:bhtno";

        m.put("bty", BillType.CashRecieveBill);
        m.put("bhtno", patientEncounter);

        billItems = billItemFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        billItemNetValue = 0;
        for (BillItem bi : billItems) {
            if (billItems == null) {
                return;
            }
            billItemNetValue += bi.getNetValue();
        }

    }

    public void createPaidByPatient() {

        String sql;
        Map m = new HashMap();
        sql = "SELECT b FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType=:bty"
                + " and b.patientEncounter=:bhtno";

        m.put("bty", BillType.InwardPaymentBill);
        m.put("bhtno", patientEncounter);

        paidbyPatientBillList = billfacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        paidbyPatientTotalValue = 0.0;
        for (Bill b : paidbyPatientBillList) {
            if (paidbyPatientBillList == null) {
                return;
            }
            paidbyPatientTotalValue += b.getNetTotal();
        }

    }

    public List<BillItem> getPharmacyItems() {
        return pharmacyItems;
    }

    public void setPharmacyItems(List<BillItem> pharmacyItems) {
        this.pharmacyItems = pharmacyItems;
    }

    public List<Bill> getOutSideBills() {
        return outSideBills;
    }

    public void setOutSideBills(List<Bill> outSideBills) {
        this.outSideBills = outSideBills;
    }

    List<BillFee> fillteredFees;

    public List<BillFee> getFillteredFees() {
        return fillteredFees;
    }

    public void setFillteredFees(List<BillFee> fillteredFees) {
        this.fillteredFees = fillteredFees;
    }

    public List<BillFee> getProBillFee() {
        return proBillFee;
    }

    public void setProBillFee(List<BillFee> proBillFee) {
        this.proBillFee = proBillFee;
    }

    public List<BillFee> getAssistBillFee() {
        return assistBillFee;
    }

    public void setAssistBillFee(List<BillFee> assistBillFee) {
        this.assistBillFee = assistBillFee;
    }

    public List<Bill> getPaymentBills() {
        return paymentBills;
    }

    public void setPaymentBills(List<Bill> paymentBills) {
        this.paymentBills = paymentBills;
    }

    List<BillItem> filterItems;

    public List<BillItem> getFilterItems() {
        return filterItems;
    }

    public void setFilterItems(List<BillItem> filterItems) {
        this.filterItems = filterItems;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public List<PatientItem> getPatientItems() {
        return patientItems;
    }

    public void setPatientItems(List<PatientItem> patientItems) {
        this.patientItems = patientItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public List<PatientRoom> getPatientRooms() {
        return patientRooms;
    }

    public void setPatientRooms(List<PatientRoom> patientRooms) {
        this.patientRooms = patientRooms;
    }

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public List<BillFee> getBillFees() {
        return billFees;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public void makeNullList() {
        patientRooms = null;
        billItems = null;
        proBillFee = null;
        assistBillFee = null;
        outSideBills = null;
        paymentBills = null;
        pharmacyItems = null;
    }

    public void makeNull() {
        patientEncounter = null;
        activeBackButton = false;
        makeNullList();
    }

    public void createTablesFinalizedByBillType() {
        if (inwardChargeType == null) {
            billFees = getInwardBean().fetchBillFees(getPatientEncounter());
            billItems = getInwardBean().fetchBillItems(getPatientEncounter());

        } else {
            billFees = getInwardBean().fetchBillFees(patientEncounter, inwardChargeType);
            billItems = getInwardBean().fetchBillItems(patientEncounter, inwardChargeType);
        }
    }

    public void createTablesFinalized() {
        makeNullList();
        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter());
        billItems = getInwardBean().fetchEagerBillItems(getPatientEncounter());
        billFees = getInwardBean().fetchBillFees(getPatientEncounter());
        patientItems = getInwardBean().fetchPatientItem(getPatientEncounter());
        outSideBills = getInwardBean().fetchOutSideBill2(getPatientEncounter());
        proBillFee = getInwardBean().createProfesionallFee(getPatientEncounter());
        assistBillFee = getInwardBean().createDoctorAndNurseFee(getPatientEncounter());
        paymentBills = getInwardBean().fetchPaymentBill(getPatientEncounter());
        paidbyPatientTotalValue = getInwardReportControllerBht().calPaidbyPatient(paymentBills);
        pharmacyItems = getInwardBean().fetchPharmacyIssueBillItem(getPatientEncounter(), BillType.PharmacyBhtPre);
        storeItems = getInwardBean().fetchPharmacyIssueBillItem(getPatientEncounter(), BillType.StoreBhtPre);
        creditPayment = getInwardReportControllerBht().createCreditPayment(getPatientEncounter());
        creditCompanyPaymentTotal = getInwardReportControllerBht().calTotalCreditCompany(creditPayment);

    }

    /**
     * Creates a new instance of BhtSummeryFinalizedController
     */
    public BhtSummeryFinalizedController() {
    }

    @EJB
    BillItemFacade billItemFacade;

    public void errorCheck() {
        String sql = "Select b from BillItem b"
                + " where b.retired=false "
                + " and b.bill.billType=:btp";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        List<BillItem> list = billItemFacade.findByJpql(sql, hm);

        int i = 0;
        for (BillItem b : list) {
            if (b == null) {
                continue;
            }

            sql = "Select sum(bf.feeGrossValue),"
                    + " sum(bf.feeDiscount),"
                    + " sum(bf.feeMargin),"
                    + " sum(bf.feeValue) "
                    + " from BillFee bf"
                    + " where bf.retired=false "
                    + " and bf.billItem=:bt";
            hm = new HashMap();
            hm.put("bt", b);
            Object[] obj = billFeeFacade.findAggregate(sql, hm, TemporalType.DATE);

            if (obj == null) {
                continue;
            }

            Double feeGross = (Double) obj[0];
            Double feeDiscount = (Double) obj[1];
            Double feeMargin = (Double) obj[2];
            Double feeValue = (Double) obj[3];

            if (feeValue == null) {
                continue;
            }

            if (feeValue != b.getNetValue()) {
                i++;

                sql = "Select bf from BillFee bf where bf.retired=false and  bf.billItem=:bt";
                hm = new HashMap();
                hm.put("bt", b);
                List<BillFee> bfList = billFeeFacade.findByJpql(sql, hm);
                if (bfList == null || bfList.isEmpty()) {
                    continue;
                }

                if (bfList.size() == 1) {
                    for (BillFee billFee : bfList) {
                        billFee.setFeeGrossValue(b.getGrossValue());
                        billFee.setFeeDiscount(b.getDiscount());
                        billFee.setFeeMargin(b.getMarginValue());
                        billFee.setFeeValue(b.getNetValue());
                        billFeeFacade.edit(billFee);
                    }
                } else {
                    for (BillFee billFee : bfList) {
                        if (b.getDiscount() == 0 && b.getMarginValue() == 0) {
                            if (billFee.getFeeDiscount() != 0) {
                                billFee.setFeeValue(billFee.getFeeGrossValue() + billFee.getFeeDiscount());
                                billFee.setFeeDiscount(0);
                            } else {

                                billFee.setFeeValue(billFee.getFeeGrossValue());
                            }

                            billFeeFacade.edit(billFee);

                        }
                    }
                }

            }
        }
    }

    @EJB
    SpecialityFacade specialityFacade;
    @EJB
    StaffFacade staffFacade;

    public List<Staff> fetchStaff() {
        HashMap hm = new HashMap();
        String sql = "Select distinct(b.staff) "
                + " FROM BillFee b "
                + " where b.retired=false"
                + " and b.staff.speciality is null "
                + " and(b.bill.billType=:refType1 "
                + " or b.bill.billType=:refType2 )";

        hm.put("refType1", BillType.InwardBill);
        hm.put("refType2", BillType.InwardProfessional);

        return staffFacade.findByJpql(sql, hm);

    }

    public void errorCheck2() {
        String sql = "Select s from Speciality s where s.retired=false "
                + " and s.name like '%Nurse%' ";
        Speciality speciality = specialityFacade.findFirstByJpql(sql);

        for (Staff staff : fetchStaff()) {
            staff.setSpeciality(speciality);
            staffFacade.edit(staff);
        }
    }

    public void errorCorrectionPharmacy() {
        for (BillItem bi : inwardBean.fetchBillItem1(BillType.PharmacyBhtPre)) {
            bi.setGrossValue(bi.getNetValue());
            billItemFacade.edit(bi);
        }

//        for (BillItem bi : inwardBean.fetchBillItem2(BillType.PharmacyBhtPre)) {
//            System.err.println("Id " + bi.getId());
//            System.err.println("Gross " + bi.getGrossValue());
//            System.err.println("Net " + bi.getNetValue());
//            bi.setMarginValue(0);
//            bi.setGrossValue(bi.getNetValue());
//            billItemFacade.edit(bi);
//        }
//        for (BillItem bi : inwardBean.fetchBillItem3(BillType.PharmacyBhtPre)) {
//            System.err.println("Id " + bi.getId());
//            System.err.println("Gross " + bi.getGrossValue());
//            System.err.println("Net " + bi.getNetValue());
////            billItemFacade.edit(bi);
//        }
    }

    @Inject
    BillBeanController billBeanController;

    public void changeDiscountListener(double discount, double total, PatientEncounter patientEncounter, BillType billType) {
        double discountPercent = (discount * 100) / total;
        double disValue = 0;

        disValue = updateIssueBillFees(discountPercent, patientEncounter, billType);

    }

    @EJB
    ItemFeeFacade itemFeeFacade;

    public void restFeeType() {
        String sql = "Select s from ItemFee s "
                + " where s.retired=false "
                + " and s.feeType is null "
                + " and s.speciality is null "
                + " and s.staff is null";
        List<ItemFee> itemFee = itemFeeFacade.findByJpql(sql);
//        System.err.println(itemFee.size());
        for (ItemFee i : itemFee) {
            i.setFeeType(FeeType.OwnInstitution);
            itemFeeFacade.edit(i);
        }

        sql = "Select s from ItemFee s "
                + " where s.retired=false "
                + " and s.feeType is null "
                + " and ( s.speciality is not null "
                + " or s.staff is not null)";
        itemFee = itemFeeFacade.findByJpql(sql);
//        System.err.println(itemFee.size());
        for (ItemFee i : itemFee) {
            i.setFeeType(FeeType.Staff);
            itemFeeFacade.edit(i);
        }
    }

    public void errorCorrectionService() {
        for (BillItem bi : inwardBean.fetchBillItem1(BillType.InwardBill)) {
            bi.setGrossValue(bi.getNetValue());
            billItemFacade.edit(bi);
        }

        for (BillFee bi : inwardBean.fetchBillFee1(BillType.InwardBill)) {
            bi.setFeeGrossValue(bi.getFeeValue());
            billFeeFacade.edit(bi);
        }

//        for (BillItem bi : inwardBean.fetchBillItem2(BillType.InwardBill)) {
//            System.err.println("Id " + bi.getId());
//            System.err.println("Gross " + bi.getGrossValue());
//            System.err.println("Net " + bi.getNetValue());
//            bi.setMarginValue(0);
//            bi.setGrossValue(bi.getNetValue());
//            billItemFacade.edit(bi);
//        }
//        
//         for (BillFee bi : inwardBean.fetchBillFee2(BillType.InwardBill)) {
//            System.err.println("Id " + bi.getId());
//            System.err.println("Gross " + bi.getFeeGrossValue());
//            System.err.println("Net " + bi.getFeeValue());
//            bi.setFeeMargin(0);
//            bi.setFeeGrossValue(bi.getFeeValue());
//            billFeeFacade.edit(bi);
//        }
//        for (BillItem bi : inwardBean.fetchBillItem3(BillType.InwardBill)) {
//            System.err.println("Id " + bi.getId());
//            System.err.println("Gross " + bi.getGrossValue());
//            System.err.println("Net " + bi.getNetValue());
////            billItemFacade.edit(bi);
//        }
//        
//        for (BillFee bi : inwardBean.fetchBillFee3(BillType.InwardBill)) {
//            System.err.println("Id " + bi.getId());
//            System.err.println("Gross " + bi.getFeeGrossValue());
//            System.err.println("Net " + bi.getFeeValue());
////            billItemFacade.edit(bi);
//        }
    }

    private double updateIssueBillFees(double discountPercent, PatientEncounter patientEncounter, BillType billType) {
        List<BillItem> listBillItems = getInwardBean().getIssueBillItemByInwardChargeType(patientEncounter, billType);
        double disTot = 0;
        if (listBillItems == null || listBillItems.isEmpty()) {
            return disTot;
        }

        for (BillItem bf : listBillItems) {
            double value = bf.getGrossValue() + bf.getMarginValue();
            double dis = (value * discountPercent) / 100;
            disTot += dis;
            bf.setDiscount(dis);
            bf.setNetValue(value - dis);
            billItemFacade.edit(bf);
        }

        return disTot;
    }

    public void errorCorrection2() {
//        List<Bill> bills = inwardBean.fetchFinalBills();
//        for (Bill b : bills) {
//            inwardReportControllerBht.setPatientEncounter(b.getPatientEncounter());
//            double gross = inwardReportControllerBht.fetchMadicineGross() + inwardReportControllerBht.fetchMadicineMargin();
//            double discount = inwardReportControllerBht.fetchMadicineDiscount();
//            double netValue = inwardReportControllerBht.fetchMadicineNetValue();
//
//            BillItem billItem = billBeanController.fetchBillItem(b, InwardChargeType.Medicine);
//            if (billItem == null) {
//                continue;
//            }
//
//            if (Math.abs((discount - billItem.getDiscount())) > 0.01) {
//                System.err.println("BHT Discount **** " + b.getPatientEncounter().getBhtNo());
//                System.err.println("Pharmacy Discount " + discount);
//                changeDiscountListener(billItem.getDiscount(), billItem.getGrossValue(), b.getPatientEncounter());
//            }
//
//        }
    }
    @Inject
    BhtIssueReturnController bhtIssueReturnController;
    @Inject
    SessionController sessionController;
    @EJB
    PatientItemFacade patientItemFacade;

    public void errorCorrecion() {
        PatientItem patientItem = new PatientItem();
        patientItem.setCreatedAt(new Date());
        patientItem.setCreater(sessionController.getLoggedUser());
        patientItemFacade.create(patientItem);
    }

//    public void errorCorrection3() {
//        List<BillItem> lst = inwardBean.fetchBillItems(BillType.PharmacyBhtPre, new RefundBill());
//        Set<BillItem> billItemSets = new HashSet<>();
//        for (BillItem b : lst) {
//            if (billItemSets.contains(b.getReferanceBillItem())) {
//                System.err.println("Already Exist : " + b.getReferanceBillItem());
//                b.setRetired(true);
//                billItemFacade.edit(b);
//
//            }
//
//            bhtIssueReturnController.updateMargin(b, b.getBill().getFromDepartment());
//            if (Math.abs(b.getGrossValue()) == Math.abs(b.getReferanceBillItem().getGrossValue())) {
//                billItemSets.add(b.getReferanceBillItem());
//            }
//        }
//    }
    public void processBillItems() {
        billItems = new ArrayList<>();
        String sql = "Select b "
                + " from BillItem b"
                + " where b.retired=false "
                + " and b.bill.billType=:btp";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        List<BillItem> list = billItemFacade.findByJpql(sql, hm);

        int i = 0;
        boolean flag = false;
        for (BillItem b : list) {
            flag = false;
            sql = "Select sum(bf.feeValue) "
                    + " from BillFee bf"
                    + " where bf.retired=false "
                    + " and bf.billItem=:bt";

            hm = new HashMap();
            hm.put("bt", b);
            double feeValue = billFeeFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);

            if (feeValue != b.getNetValue()) {
                i++;

                sql = "Select bf "
                        + " from BillFee bf"
                        + " where bf.retired=false "
                        + " and  bf.billItem=:bt";
                hm = new HashMap();
                hm.put("bt", b);
                List<BillFee> bfList = billFeeFacade.findByJpql(sql, hm);

                if (bfList == null || bfList.isEmpty()) {
                    continue;
                }

                if (bfList.size() == 1) {
                    for (BillFee billFee : bfList) {
                        billFee.setFeeGrossValue(b.getGrossValue());
                        billFee.setFeeDiscount(b.getDiscount());
                        billFee.setFeeMargin(b.getMarginValue());
                        billFee.setFeeValue(b.getNetValue());
                        billFeeFacade.edit(billFee);
                    }
                } else {
                    billItems.add(b);
                }

            }

        }

    }

    public void processBillItems2() {
        billItems = new ArrayList<>();
        String sql = "Select b "
                + " from BillItem b"
                + " where b.retired=false "
                + " and b.bill.billType=:btp";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        List<BillItem> list = billItemFacade.findByJpql(sql, hm);

        int i = 0;
        for (BillItem b : list) {
            sql = "Select sum(bf.feeValue) "
                    + " from BillFee bf"
                    + " where bf.retired=false "
                    + " and bf.billItem=:bt";

            hm = new HashMap();
            hm.put("bt", b);
            double feeValue = billFeeFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);

            if (feeValue != b.getNetValue()) {
                i++;

                sql = "Select bf "
                        + " from BillFee bf"
                        + " where bf.retired=false "
                        + " and  bf.billItem=:bt";
                hm = new HashMap();
                hm.put("bt", b);
                List<BillFee> bfList = billFeeFacade.findByJpql(sql, hm);

                if (bfList == null || bfList.isEmpty()) {
                    continue;
                }

                if (bfList.size() == 1) {
                    for (BillFee billFee : bfList) {
                        billFee.setFeeGrossValue(b.getGrossValue());
                        billFee.setFeeDiscount(b.getDiscount());
                        billFee.setFeeMargin(b.getMarginValue());
                        billFee.setFeeValue(b.getNetValue());
                        billFeeFacade.edit(billFee);
                    }
                } else {
                    boolean flag = false;
                    for (BillFee billFee : bfList) {
                        double value = billFee.getFeeGrossValue() + billFee.getFeeMargin() + billFee.getFeeDiscount();

                        if (flag == false && value == b.getNetValue()) {
                            billFee.setFeeValue(value);
                            billFeeFacade.edit(billFee);
                            flag = true;
                        }

                        if (flag && billFee.getFee().getFeeType() != FeeType.Staff) {
                            billFee.setRetired(true);
                            billFeeFacade.edit(billFee);
                        }
                    }

                }

            }

        }

    }

    public String createBhtBillFinalized() {
        activeBackButton = true;
        createTablesFinalized();

        return "/inward/inward_bill_intrim_finalized";
    }

    public class bhtWithVat {

        PatientEncounter pe;
        double vat;
        double medicine;
        double room;
        double doc;
        double hos;

        public double getRoom() {
            return room;
        }

        public void setRoom(double room) {
            this.room = room;
        }

        public PatientEncounter getPe() {
            return pe;
        }

        public void setPe(PatientEncounter pe) {
            this.pe = pe;
        }

        public double getVat() {
            return vat;
        }

        public void setVat(double vat) {
            this.vat = vat;
        }

        public double getMedicine() {
            return medicine;
        }

        public void setMedicine(double medicine) {
            this.medicine = medicine;
        }

        public double getDoc() {
            return doc;
        }

        public void setDoc(double doc) {
            this.doc = doc;
        }

        public double getHos() {
            return hos;
        }

        public void setHos(double hos) {
            this.hos = hos;
        }
    }

    public double getBillItemGrossPharmacy() {
        return billItemGrossPharmacy;
    }

    public void setBillItemGrossPharmacy(double billItemGrossPharmacy) {
        this.billItemGrossPharmacy = billItemGrossPharmacy;
    }

    public double getBillItemMarginPharmacy() {
        return billItemMarginPharmacy;
    }

    public void setBillItemMarginPharmacy(double billItemMarginPharmacy) {
        this.billItemMarginPharmacy = billItemMarginPharmacy;
    }

    public double getBillItemDiscountPharmacy() {
        return billItemDiscountPharmacy;
    }

    public void setBillItemDiscountPharmacy(double billItemDiscountPharmacy) {
        this.billItemDiscountPharmacy = billItemDiscountPharmacy;
    }

    public double getBillItemNetValue() {
        return billItemNetValue;
    }

    public void setBillItemNetValue(double billItemNetValue) {
        this.billItemNetValue = billItemNetValue;
    }

    public double getBillFeeNetValue() {
        return billFeeNetValue;
    }

    public void setBillFeeNetValue(double billFeeNetValue) {
        this.billFeeNetValue = billFeeNetValue;
    }

    public List<BillItem> getStoreItems() {
        return storeItems;
    }

    public void setStoreItems(List<BillItem> storeItems) {
        this.storeItems = storeItems;
    }

    public PatientRoomFacade getPatientRoomFacade() {
        return patientRoomFacade;
    }

    public void setPatientRoomFacade(PatientRoomFacade patientRoomFacade) {
        this.patientRoomFacade = patientRoomFacade;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade itemFeeFacade) {
        this.itemFeeFacade = itemFeeFacade;
    }

    public BhtIssueReturnController getBhtIssueReturnController() {
        return bhtIssueReturnController;
    }

    public void setBhtIssueReturnController(BhtIssueReturnController bhtIssueReturnController) {
        this.bhtIssueReturnController = bhtIssueReturnController;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PatientItemFacade getPatientItemFacade() {
        return patientItemFacade;
    }

    public void setPatientItemFacade(PatientItemFacade patientItemFacade) {
        this.patientItemFacade = patientItemFacade;
    }

    public List<BillItem> getBillItemsInward() {
        return billItemsInward;
    }

    public void setBillItemsInward(List<BillItem> billItemsInward) {
        this.billItemsInward = billItemsInward;
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

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public boolean isActiveBackButton() {
        return activeBackButton;
    }

    public void setActiveBackButton(boolean activeBackButton) {
        this.activeBackButton = activeBackButton;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public List<bhtWithVat> getBhtWithVats() {
        return bhtWithVats;
    }

    public void setBhtWithVats(List<bhtWithVat> bhtWithVats) {
        this.bhtWithVats = bhtWithVats;
    }

    public double getTotalGross() {
        return totalGross;
    }

    public void setTotalGross(double totalGross) {
        this.totalGross = totalGross;
    }

    public double getTotalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(double totalDiscount) {
        this.totalDiscount = totalDiscount;
    }

    public double getTotalNet() {
        return totalNet;
    }

    public void setTotalNet(double totalNet) {
        this.totalNet = totalNet;
    }

    public double getTotalVat() {
        return totalVat;
    }

    public void setTotalVat(double totalVat) {
        this.totalVat = totalVat;
    }

    public double getTotalMed() {
        return totalMed;
    }

    public void setTotalMed(double totalMed) {
        this.totalMed = totalMed;
    }

    public double getTotalDoc() {
        return totalDoc;
    }

    public void setTotalDoc(double totalDoc) {
        this.totalDoc = totalDoc;
    }

    public double getTotalHos() {
        return totalHos;
    }

    public void setTotalHos(double totalHos) {
        this.totalHos = totalHos;
    }

    public double getTotalRoom() {
        return totalRoom;
    }

    public void setTotalRoom(double totalRoom) {
        this.totalRoom = totalRoom;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

}
