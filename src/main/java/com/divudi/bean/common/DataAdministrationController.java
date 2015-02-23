/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.data.BillType;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillNumber;
import com.divudi.entity.lab.PatientReport;
import com.divudi.entity.lab.PatientReportItemValue;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillEntryFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillNumberFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PatientInvestigationItemValueFacade;
import com.divudi.facade.PatientReportFacade;
import com.divudi.facade.PatientReportItemValueFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.util.JsfUtil;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Administrator
 */
@Named(value = "dataAdministrationController")
@ApplicationScoped
public class DataAdministrationController {

    @EJB
    PatientInvestigationItemValueFacade patientInvestigationItemValueFacade;
    @EJB
    PatientReportItemValueFacade patientReportItemValueFacade;
    @EJB
    PatientReportFacade patientReportFacade;
    @EJB
    PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    BillComponentFacade billComponentFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    BillEntryFacade billEntryFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    BillNumberFacade billNumberFacade;
    @EJB
    PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @Inject
    SessionController sessionController;

    public void restBillNumber() {
        String sql = "Select b from BillNumber b where b.retired=false";
        List<BillNumber> list = billNumberFacade.findBySQL(sql);
        for (BillNumber b : list) {
            b.setRetired(true);
            b.setRetiredAt(new Date());
            b.setRetirer(sessionController.getLoggedUser());
            billNumberFacade.edit(b);
        }
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public void makeRateFromPurchaseRateToSaleInTransferBills() {
        List<Bill> bills;
        String j;
        Map m = new HashMap();

        j = "select b from Bill b where b.billType in :bt";

        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.PharmacyTransferIssue);
        bts.add(BillType.PharmacyTransferReceive);
        bts.add(BillType.PharmacyTransferRequest);
        System.out.println("arr" + bts);
        m.put("bt", bts);

//        j="select b from Bill b where (b.billType=: bts and b.billType=: bts2 and b.billType=: bts3)";
//        m.put("bts", BillType.PharmacyTransferIssue);
//        m.put("bts", BillType.PharmacyTransferReceive);
//        m.put("bts", BillType.PharmacyTransferRequest);
//        
        bills = getBillFacade().findBySQL(j, m);

        for (Bill b : bills) {
            System.out.println("b = " + b);
            double gt = 0;
            double nt = 0;
            for (BillItem bi : b.getBillItems()) {
                System.out.println("billitem" + b.getBillItems());
                //System.out.println("bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate() = " + bi.getPharmaceuticalBillItem().getStock().getItemBatch());

                System.out.println("bi.getRate() = " + bi.getRate());
                System.out.println("bi.getNetValue() = " + bi.getNetValue());
                System.out.println("bi.getQty() = " + bi.getQty());
                System.out.println("bi.getNetValue() = " + bi.getNetValue());

                bi.setRate((double) fetchPharmacyuticalBillitem(bi));
                System.out.println("Rate"+ fetchPharmacyuticalBillitem(bi));
                System.out.println("getRate"+bi.getRate());
                bi.setNetRate((double) fetchPharmacyuticalBillitem(bi));

                //                bi.setRate(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate());
//                bi.setNetRate(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate());
                System.out.println("rate"+bi.getNetRate());
                System.out.println("Net rate"+bi.getNetValue());
                bi.setNetValue(bi.getNetRate() * bi.getQty());
                bi.setGrossValue(bi.getNetValue());

                billItemFacade.edit(bi);

                gt += bi.getNetValue();
                nt += bi.getGrossValue();

            }

            billFacade.edit(b);
        }

    }

    public double fetchPharmacyuticalBillitem(BillItem bi) {
        String sql;
        Map m = new HashMap();

        sql = "Select ph.stock.itemBatch.retailsaleRate from PharmaceuticalBillItem ph where ph.billItem =:bi ";
        m.put("bi", bi);

        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, m);

    }

    /**
     * Creates a new instance of DataAdministrationController
     */
    public DataAdministrationController() {
    }

    public void removeAllBillsAndBillItems() {
        for (PatientReportItemValue v : getPatientReportItemValueFacade().findAll()) {
            getPatientReportItemValueFacade().remove(v);
        }
        JsfUtil.addErrorMessage("Removed all patient report items values");
        for (PatientReport r : getPatientReportFacade().findAll()) {
            getPatientReportFacade().remove(r);
        }
        JsfUtil.addErrorMessage("Removed all patient reports");
        for (BillFee f : getBillFeeFacade().findAll()) {
            getBillFeeFacade().remove(f);
        }
        JsfUtil.addErrorMessage("Removed all bill fees");
    }

//    Getters & Setters
    public PatientReportItemValueFacade getPatientReportItemValueFacade() {
        return patientReportItemValueFacade;
    }

    public void setPatientReportItemValueFacade(PatientReportItemValueFacade patientReportItemValueFacade) {
        this.patientReportItemValueFacade = patientReportItemValueFacade;
    }

    public PatientInvestigationItemValueFacade getPatientInvestigationItemValueFacade() {
        return patientInvestigationItemValueFacade;
    }

    public void setPatientInvestigationItemValueFacade(PatientInvestigationItemValueFacade patientInvestigationItemValueFacade) {
        this.patientInvestigationItemValueFacade = patientInvestigationItemValueFacade;
    }

    public PatientReportFacade getPatientReportFacade() {
        return patientReportFacade;
    }

    public void setPatientReportFacade(PatientReportFacade patientReportFacade) {
        this.patientReportFacade = patientReportFacade;
    }

    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }

    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
    }

    public BillComponentFacade getBillComponentFacade() {
        return billComponentFacade;
    }

    public void setBillComponentFacade(BillComponentFacade billComponentFacade) {
        this.billComponentFacade = billComponentFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BillEntryFacade getBillEntryFacade() {
        return billEntryFacade;
    }

    public void setBillEntryFacade(BillEntryFacade billEntryFacade) {
        this.billEntryFacade = billEntryFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

}
