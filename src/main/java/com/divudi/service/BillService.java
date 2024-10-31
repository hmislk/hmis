package com.divudi.service;

import com.divudi.data.InstitutionType;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.RefundBill;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author Dr M H B Ariyaratne
 *
 */
@Stateless
public class BillService {

    @EJB
    BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;

    public void createBillItemFeeBreakdownAsHospitalFeeItemDiscount(List<BillItem> billItems) {
        for (BillItem bi : billItems) {
            createBillItemFeeBreakdownAsHospitalFeeItemDiscount(bi);
        }
    }

    public void createBillItemFeeBreakdownAsHospitalFeeItemDiscount(BillItem bi) {
        double staffFeesCalculatedByBillFees = 0.0;
        double collectingCentreFeesCalculateByBillFees = 0.0;
        double hospitalFeeCalculatedByBillFess = 0.0;
        List<BillFee> bfs = fetchBillFees(bi);
        for (BillFee bf : bfs) {
            if (bf.getInstitution() != null && bf.getInstitution().getInstitutionType() == InstitutionType.CollectingCentre) {
                collectingCentreFeesCalculateByBillFees += bf.getFeeGrossValue();
            } else if (bf.getStaff() != null || bf.getSpeciality() != null) {
                staffFeesCalculatedByBillFees += bf.getFeeGrossValue();
            } else {
                hospitalFeeCalculatedByBillFess = bf.getFeeGrossValue();
            }
        }
        bi.setCollectingCentreFee(collectingCentreFeesCalculateByBillFees);
        bi.setStaffFee(staffFeesCalculatedByBillFees);
        bi.setHospitalFee(hospitalFeeCalculatedByBillFess);
        billItemFacade.edit(bi);
    }

    public void createBillItemFeeBreakdownFromBill(Bill bill) {
        List<BillItem> billItems = fetchBillItems(bill);
        createBillItemFeeBreakdownAsHospitalFeeItemDiscount(billItems);
    }

    public void createBillItemFeeBreakdownFromBills(List<Bill> bills) {
        List<BillItem> allBillItems = fetchBillItems(bills);
        createBillItemFeeBreakdownAsHospitalFeeItemDiscount(allBillItems);
    }

    public List<BillFee> fetchBillFees(BillItem billItem) {
        String jpql = "select bf "
                + "from BillFee bf "
                + " where bf.retired=:ret "
                + " and bf.billItem=:bi";
        Map params = new HashMap();
        params.put("bi", billItem);
        params.put("ret", false);
        return billFeeFacade.findByJpql(jpql, params);
    }

    public List<BillItem> fetchBillItems(Bill b) {
        String jpql;
        HashMap params = new HashMap();
        jpql = "SELECT bi "
                + " FROM BillItem bi "
                + " WHERE bi.bill=:bl "
                + " order by bi.id";
        params.put("bl", b);
        return billItemFacade.findByJpql(jpql, params);
    }

    public List<BillItem> fetchBillItems(List<Bill> bills) {
        List<BillItem> allBillItems = new ArrayList<>();
        for (Bill b : bills) {
            allBillItems.addAll(fetchBillItems(b));
        }
        return allBillItems;
    }

    public void calculateBillBreakdownAsHospitalCcAndStaffTotalsByBillFees(Bill bill) {
        System.out.println("calculateBillBreakdownAsHospitalCcAndStaffTotalsByBillFees");
        double billStaffFee = 0.0;
        double billCollectingCentreFee = 0.0;
        double billHospitalFee = 0.0;

        List<BillItem> billItems = fetchBillItems(bill);
        System.out.println("Processing Bill: " + bill.getId());

        for (BillItem bi : billItems) {
            // Initialize fee accumulators for the current BillItem
            double staffFeesCalculatedByBillFees = 0.0;
            double collectingCentreFeesCalculatedByBillFees = 0.0;
            double hospitalFeeCalculatedByBillFees = 0.0;

            System.out.println("Processing BillItem: " + bi.getId());

            // Fetch BillFees for the current BillItem
            List<BillFee> billFees = fetchBillFees(bi);
            for (BillFee bf : billFees) {
                System.out.println("Processing BillFee: " + bf.getId()
                        + ", FeeGrossValue: " + bf.getFeeGrossValue());

                // Calculate fees based on InstitutionType, Staff, or Speciality
                if (bf.getInstitution() != null
                        && bf.getInstitution().getInstitutionType() == InstitutionType.CollectingCentre) {
                    collectingCentreFeesCalculatedByBillFees += bf.getFeeGrossValue();
                    System.out.println("CollectingCentre Fee Updated: "
                            + collectingCentreFeesCalculatedByBillFees);
                } else if (bf.getStaff() != null || bf.getSpeciality() != null) {
                    staffFeesCalculatedByBillFees += bf.getFeeGrossValue();
                    System.out.println("Staff Fee Updated: " + staffFeesCalculatedByBillFees);
                } else {
                    hospitalFeeCalculatedByBillFees += bf.getFeeGrossValue();
                    System.out.println("Hospital Fee Updated: " + hospitalFeeCalculatedByBillFees);
                }
            }

            // Set calculated fees to the BillItem
            bi.setCollectingCentreFee(collectingCentreFeesCalculatedByBillFees);
            bi.setStaffFee(staffFeesCalculatedByBillFees);
            bi.setHospitalFee(hospitalFeeCalculatedByBillFees);
            billItemFacade.editAndCommit(bi);

            // Log the values set to the BillItem
            System.out.println("BillItem " + bi.getId()
                    + " - Hospital Fee: " + hospitalFeeCalculatedByBillFees
                    + ", Staff Fee: " + staffFeesCalculatedByBillFees
                    + ", Collecting Centre Fee: " + collectingCentreFeesCalculatedByBillFees);

            // Accumulate the fees to the Bill totals
            billCollectingCentreFee += collectingCentreFeesCalculatedByBillFees;
            billStaffFee += staffFeesCalculatedByBillFees;
            billHospitalFee += hospitalFeeCalculatedByBillFees;

            System.out.println("bi.getHospitalFee() = " + bi.getHospitalFee());
            billItemFacade.create(bi);

        }

        // Set the accumulated totals to the Bill
        bill.setPerformInstitutionFee(billHospitalFee);
        bill.setTotalCenterFee(billCollectingCentreFee);
        bill.setTotalHospitalFee(billHospitalFee);
        bill.setTotalStaffFee(billStaffFee);

        // Additional fee assignments as per existing structure
        bill.setStaffFee(billStaffFee);
        bill.setCollctingCentreFee(billCollectingCentreFee);
        bill.setHospitalFee(billHospitalFee);
        bill.setProfessionalFee(billStaffFee);

        // Log the final bill totals
        System.out.println("Final Bill Totals - Hospital Fee: " + billHospitalFee
                + ", Staff Fee: " + billStaffFee
                + ", Collecting Centre Fee: " + billCollectingCentreFee);

        // Persist the updated Bill
        billFacade.editAndCommit(bill);
    }

    public void calculateBillBreakdownAsHospitalCcAndStaffTotalsByBillFees(List<Bill> bills) {
        for (Bill bill : bills) {
            calculateBillBreakdownAsHospitalCcAndStaffTotalsByBillFees(bill);
        }
    }

}
