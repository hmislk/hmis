/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.bean.lab.InvestigationController;
import com.divudi.data.BillType;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillNumber;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Category;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.PatientReport;
import com.divudi.entity.lab.PatientReportItemValue;
import com.divudi.entity.pharmacy.ItemBatch;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillEntryFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillNumberFacade;
import com.divudi.facade.CategoryFacade;
import com.divudi.facade.ItemBatchFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PatientInvestigationItemValueFacade;
import com.divudi.facade.PatientReportFacade;
import com.divudi.facade.PatientReportItemValueFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.util.JsfUtil;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

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
    @Inject
    BillSearch billSearch;
    @EJB
    ItemFacade itemFacade;

    @EJB
    CategoryFacade categoryFacade;
    @EJB
    ItemBatchFacade itemBatchFacade;

    List<Bill> bills;
    List<Bill> selectedBills;

    public void addWholesalePrices() {
        List<ItemBatch> ibs = itemBatchFacade.findAll();
        for (ItemBatch ib : ibs) {
            if (ib.getItem() != null) {
                //System.out.println("ib.getItem().getName() = " + ib.getItem().getName());
            }
            if (ib.getWholesaleRate() == 0) {
                //System.out.println("ib.getPurcahseRate = " + ib.getPurcahseRate());
                //System.out.println("ib.getWholesaleRate() = " + ib.getWholesaleRate());
                ib.setWholesaleRate((ib.getPurcahseRate() / 115) * 108);
                itemBatchFacade.edit(ib);
                //System.out.println("ib.getWholesaleRate() = " + ib.getWholesaleRate());
            } else {
                //System.out.println("no change");
            }
        }
    }

    public void removeUnsedPharmaceuticalCategories() {
        Map m = new HashMap();
        String sql;

        sql = "SELECT c FROM PharmaceuticalItemCategory c ";

        Set<Category> allCats = new HashSet<>(categoryFacade.findBySQL(sql, m));

        sql = "SELECT i.category "
                + " FROM Item i "
                + " GROUP BY i.category";

        //System.out.println("sql = " + sql);
        m = new HashMap();

        Set<Category> usedCats = new HashSet<>(categoryFacade.findBySQL(sql, m));

        //System.out.println("Used Cats " + usedCats.size());
        //System.out.println("All Cats after removing " + allCats.size());
        allCats.removeAll(usedCats);
        //System.out.println("All Cats after removing " + allCats.size());

        for (Category c : allCats) {
            //System.out.println("c = " + c);
            //System.out.println("c.getName() = " + c.getName());
            c.setRetired(true);
            c.setRetiredAt(new Date());
            c.setRetireComments("Bulk1");
            categoryFacade.edit(c);
        }
    }

    @Inject
    InvestigationController investigationController;

    public void addInstitutionToInvestigationsWithoutInstitution() {
        List<Investigation> lst = investigationController.getItems();
        for (Investigation ix : lst) {
            if (ix.getInstitution() == null) {
                ix.setInstitution(ix.getDepartment().getInstitution());
                itemFacade.edit(ix);
            }
        }
    }

    public void detectWholeSaleBills() {
        String sql;
        Map m = new HashMap();
        sql = "select b from Bill b where (b.billType=:bt1 or b.billType=:bt2) order by b.id desc";
        m.put("bt1", BillType.PharmacySale);
        m.put("bt2", BillType.PharmacyPre);
        List<Bill> bs = getBillFacade().findBySQL(sql, m, 20);
        for (Bill b : bs) {
            //System.out.println("b = " + b);
            //System.out.println("b.getBillType() = " + b.getBillType());
            if (b.getBillItems().get(0).getRate() == b.getBillItems().get(0).getPharmaceuticalBillItem().getStock().getItemBatch().getWholesaleRate()) {
                //System.out.println("whole sale bill");
                if (b.getBillType() == BillType.PharmacySale) {
                    b.setBillType(BillType.PharmacyWholeSale);
                }
                if (b.getBillType() == BillType.PharmacyPre) {
                    b.setBillType(BillType.PharmacyWholesalePre);
                }
                getBillFacade().edit(b);
            }

        }

    }

    public void addBillFeesToProfessionalCancelBills() {
        List<Bill> bs;
        String s;
        Map m = new HashMap();
        String newLine = System.getProperty("line.separator");

        s = "select b from Bill b where type(b) =:bct and b.billType=:bt and b.cancelledBill is not null order by b.id desc";

        m.put("bct", BilledBill.class);
        m.put("bt", BillType.PaymentBill);

        bs = billFacade.findBySQL(s, m);
        int i = 1;
        for (Bill b : bs) {
            Bill cb = b.getCancelledBill();
            int n = 0;
            for (BillItem bi : cb.getBillItems()) {
                bi.setPaidForBillFee(b.getBillItems().get(n).getPaidForBillFee());
                n++;
                billItemFacade.edit(bi);
            }
            //System.out.println(newLine);
            //System.out.println("Error number " + i + newLine);

            //System.out.println("Bill Details " + newLine);
            //System.out.println("\tIns Number = " + b.getInsId() + newLine);
            //System.out.println("\tDep Number = " + b.getDeptId() + newLine);
            //System.out.println("\tBill Date = " + b.getCreatedAt() + newLine);
            //System.out.println("\tValue = " + b.getNetTotal() + newLine);
            //System.out.println("Cancelled Bill Details " + newLine);
            //System.out.println("\tIns Number = " + cb.getInsId() + newLine);
            //System.out.println("\tDep Number = " + cb.getDeptId() + newLine);
            //System.out.println("\tBill Date = " + cb.getCreatedAt() + newLine);
            //System.out.println("\tValue = " + cb.getNetTotal() + newLine);
            i++;
        }
    }

    public void addOPDBillFeesToProfessionalCancelBills() {
        List<Bill> bills;
        String s;
        Map m = new HashMap();

        s = "select distinct(b.cancelledBill) from BillItem bi join bi.bill b where "
                + " type(b) =:bct "
                + " and b.billType=:bt "
                + " and bi.referenceBill.billType=:rbt "
                + " and b.cancelledBill is not null "
                + " order by b.id ";

        m.put("bct", BilledBill.class);
        m.put("bt", BillType.PaymentBill);
        m.put("rbt", BillType.OpdBill);

//        bills = billFacade.findBySQL(s, m);
        bills = billFacade.findBySQL(s, m, 10);
        for (Bill cb : bills) {
            System.out.println("cb = " + cb);
            System.out.println("cb.insId() = " + cb.getInsId());
            System.out.println("cb.deptId() = " + cb.getDeptId());
            for (BillItem bi : cb.getBillItems()) {
                System.err.println("**************");
                System.out.println("bi = " + bi);
                System.out.println("bi.getRetiredAt() = " + bi.getRetiredAt());
                System.out.println("bi.isRetired() = " + bi.isRetired());
                System.out.println("bi.getBill().getBillType() = " + bi.getBill().getBillType());
                System.out.println("bi.getReferenceBill() = " + bi.getReferenceBill());
                if (bi.getReferanceBillItem()!=null) {
                    if (bi.getReferanceBillItem().getBill()!=null) {
                    }else{
                    }
                }else{
                }
                if (bi.getReferenceBill() != null) {
                }
                System.out.println("bi.getReferenceBill().getDepartment().getName() = " + bi.getReferenceBill().getDepartment().getName());
                System.out.println("bi.getBill().getInstitution().getName() = " + bi.getBill().getInstitution().getName());
                System.err.println("**************");
                String sql;
                sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();
                List<BillFee> tmp = getBillFeeFacade().findBySQL(sql);
                if (tmp.size() > 0) {
                    System.err.println("Bill Fee Alreday Created");
                } else {
                    sql = "Select bi From BillItem bi where bi.retired=false and bi.referanceBillItem.id=" + bi.getReferanceBillItem().getId();
                    BillItem billItem = getBillItemFacade().findFirstBySQL(sql);
                    System.out.println("billItem = " + billItem);
                    sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + billItem.getId();
                    tmp = getBillFeeFacade().findBySQL(sql);
                    if (tmp.size() > 0) {
                        billSearch.cancelBillFee(cb, bi, tmp);
                    } else {
                        saveBillFee(billItem);
                        sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + billItem.getId();
                        tmp = getBillFeeFacade().findBySQL(sql);
                        billSearch.cancelBillFee(cb, bi, tmp);
                    }
                }
            }
        }
    }

    public void saveBillFee(BillItem bi) {
        BillFee bf = new BillFee();
        bf.setCreatedAt(Calendar.getInstance().getTime());
        bf.setCreater(bi.getCreater());
        bf.setBillItem(bi);
        bf.setPatienEncounter(bi.getBill().getPatientEncounter());
        bf.setPatient(bi.getBill().getPatient());
        bf.setFeeValue(0 - bi.getNetValue());
        bf.setFeeGrossValue(0 - bi.getGrossValue());
        bf.setSettleValue(0 - bi.getNetValue());
        bf.setCreatedAt(new Date());
        bf.setBill(bi.getBill());

        if (bf.getId() == null) {
            getBillFeeFacade().create(bf);
        }
    }

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
        //System.out.println("arr" + bts);
        m.put("bt", bts);

//        j="select b from Bill b where (b.billType=: bts and b.billType=: bts2 and b.billType=: bts3)";
//        m.put("bts", BillType.PharmacyTransferIssue);
//        m.put("bts", BillType.PharmacyTransferReceive);
//        m.put("bts", BillType.PharmacyTransferRequest);
//        
        bills = getBillFacade().findBySQL(j, m);

        for (Bill b : bills) {
            //System.out.println("b = " + b);
            double gt = 0;
            double nt = 0;
            for (BillItem bi : b.getBillItems()) {
                //System.out.println("billitem" + b.getBillItems());
                ////System.out.println("bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate() = " + bi.getPharmaceuticalBillItem().getStock().getItemBatch());

                //System.out.println("bi.getRate() = " + bi.getRate());
                //System.out.println("bi.getNetValue() = " + bi.getNetValue());
                //System.out.println("bi.getQty() = " + bi.getQty());
                //System.out.println("bi.getNetValue() = " + bi.getNetValue());
                bi.setRate((double) fetchPharmacyuticalBillitem(bi));
                //System.out.println("Rate" + fetchPharmacyuticalBillitem(bi));
                //System.out.println("getRate" + bi.getRate());
                bi.setNetRate((double) fetchPharmacyuticalBillitem(bi));

                //                bi.setRate(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate());
//                bi.setNetRate(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate());
                //System.out.println("rate" + bi.getNetRate());
                //System.out.println("Net rate" + bi.getNetValue());
                bi.setNetValue(bi.getNetRate() * bi.getQty());
                bi.setGrossValue(bi.getNetValue());

                billItemFacade.edit(bi);

                gt += bi.getNetValue();
                nt += bi.getGrossValue();

            }
            b.setNetTotal(gt);

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

    public void createInwardServiceBillWithPaymentmethord() {
        bills = new ArrayList<>();
        String sql;
        Map temMap = new HashMap();
        sql = "select b from Bill b where "
                + " b.billType = :billType "
                + " and b.retired=false "
                + " and b.paymentMethod is not null "
                + " order by b.createdAt ";
        temMap.put("billType", BillType.InwardBill);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
    }

    public void updateInwardServiceBillWithPaymentmethord() {
        if (selectedBills.isEmpty() || selectedBills == null) {
            JsfUtil.addErrorMessage("Nothing To Update");
            return;
        }
        for (Bill b : selectedBills) {
            b.setPaymentMethod(null);
            getBillFacade().edit(b);
            System.err.println("canged");
        }
        createInwardServiceBillWithPaymentmethord();
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

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public List<Bill> getSelectedBills() {
        return selectedBills;
    }

    public void setSelectedBills(List<Bill> selectedBills) {
        this.selectedBills = selectedBills;
    }

}
