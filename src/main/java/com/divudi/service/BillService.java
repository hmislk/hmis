package com.divudi.service;

import com.divudi.data.BillTypeAtomic;
import com.divudi.data.InstitutionType;
import com.divudi.data.PaymentMethod;
import static com.divudi.data.PaymentMethod.None;
import com.divudi.data.ReportTemplateRow;
import com.divudi.data.ReportTemplateRowBundle;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Payment;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PaymentFacade;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

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
    @EJB
    PaymentFacade paymentFacade;

    public void createBillItemFeeBreakdownAsHospitalFeeItemDiscount(List<BillItem> billItems) {
        for (BillItem bi : billItems) {
            createBillItemFeeBreakdownAsHospitalFeeItemDiscount(bi);
        }
    }

    public List<PaymentMethod> availablePaymentMethodsForCancellation(Bill bill) {
        List<PaymentMethod> pms = new ArrayList<>();
        if (bill.getPaymentMethod() == null) {
            pms.add(PaymentMethod.Cash);
            return pms;
        } else if (bill.getPaymentMethod() == PaymentMethod.Cash) {
            pms.add(PaymentMethod.Cash);
            return pms;
        } else if (bill.getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            pms.add(PaymentMethod.Cash);
            pms.add(PaymentMethod.Slip);
            pms.add(PaymentMethod.MultiplePaymentMethods);
            return pms;
        }
        for (Payment p : fetchBillPayments(bill)) {
            if (p.getPaymentDate() == null) {
                continue;
            }
            switch (p.getPaymentMethod()) {
                case Agent:
                case Card:
                case Cash:
                case Cheque:
                case Credit:
                case IOU:

                case OnCall:
                case OnlineSettlement:
                case PatientDeposit:
                case PatientPoints:
                case Slip:
                case Staff:
                case Staff_Welfare:
                case Voucher:
                case YouOweMe:
                case ewallet:
                    pms.add(PaymentMethod.IOU);
                    break;
                case None:
                case MultiplePaymentMethods:

            }
        }
        return pms;
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

    public List<Bill> fetchIndividualBillsOfBatchBill(Bill batchBill) {
        System.out.println("batchBill = " + batchBill);
        String j = "Select b "
                + " from Bill b "
                + " where b.backwardReferenceBill=:bb ";
        Map m = new HashMap();
        m.put("bb", batchBill);
        System.out.println("m = " + m);
        System.out.println("j = " + j);
        List<Bill> tbs = billFacade.findByJpql(j, m);
        return tbs;
    }

    public void saveBill(Bill bill) {
        saveBill(bill, null);
    }

    public void saveBill(Bill bill, WebUser creator) {
        if (bill == null) {
            return;
        }
        if (bill.getId() == null) {
            if (bill.getCreatedAt() == null) {
                bill.setCreatedAt(new Date());
            }
            if (bill.getCreater() == null && creator != null) {
                bill.setCreater(creator);
            }
            billFacade.create(bill);
        } else {
            billFacade.edit(bill);
        }
    }

    public Bill reloadBill(Bill bill) {
        if (bill == null) {
            return null;
        }
        if (bill.getId() == null) {
            return bill;
        }
        return billFacade.findWithoutCache(bill.getId());
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

    public List<Payment> fetchBillPayments(Bill bill) {
        System.out.println("bill = " + bill);
        List<Payment> fetchingBillComponents;
        String jpql;
        Map params = new HashMap();
        jpql = "Select p "
                + " from Payment p "
                + "where p.bill=:bill "
                + "order by p.id";
        params.put("bill", bill);
        fetchingBillComponents = paymentFacade.findByJpql(jpql, params);
        return fetchingBillComponents;
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

    public ReportTemplateRowBundle createBundleByKeywordForBills(List<BillTypeAtomic> billTypesAtomics,
            Institution ins, Department dep,
            Institution fromIns,
            Department fromDep,
            Institution toIns,
            Department toDep,
            WebUser user,
            Date paramFromDate,
            Date paramToDate,
            SearchKeyword filter) {
        ReportTemplateRowBundle outputBundle = new ReportTemplateRowBundle();
        List<ReportTemplateRow> outputRows;
        String jpql;
        Map params = new HashMap();

        jpql = "select new com.divudi.data.ReportTemplateRow(b) "
                + " from Bill b "
                + " where b.retired=:ret "
                + " and b.billTypeAtomic in :billTypesAtomics "
                + " and b.createdAt between :fromDate and :toDate ";

        params.put("ret", false);
        params.put("billTypesAtomics", billTypesAtomics);
        params.put("fromDate", paramFromDate);
        params.put("toDate", paramToDate);

        if (ins != null) {
            jpql += " and b.institution=:ins ";
            params.put("ins", ins);
        }

        if (user != null) {
            jpql += " and b.creater=:user ";
            params.put("user", user);
        }

        if (dep != null) {
            jpql += " and b.department=:dep ";
            params.put("dep", dep);
        }

        if (toDep != null) {
            jpql += " and b.toDepartment=:todep ";
            params.put("todep", toDep);
        }

        if (fromDep != null) {
            jpql += " and b.fromDepartment=:fromdep ";
            params.put("fromdep", fromDep);
        }

        if (fromIns != null) {
            jpql += " and b.fromInstitution=:fromins ";
            params.put("fromins", fromIns);
        }

        if (toIns != null) {
            jpql += " and b.toInstitution=:toins ";
            params.put("toins", toIns);
        }

        if (filter != null) {
            if (filter.getPatientName() != null && !filter.getPatientName().trim().equals("")) {
                jpql += " and  ((b.patient.person.name) like :patientName )";
                params.put("patientName", "%" + filter.getPatientName().trim().toUpperCase() + "%");
            }

            if (filter.getPatientPhone() != null && !filter.getPatientPhone().trim().equals("")) {
                jpql += " and  ((b.patient.person.phone) like :patientPhone )";
                params.put("patientPhone", "%" + filter.getPatientPhone().trim().toUpperCase() + "%");
            }

            if (filter.getBillNo() != null && !filter.getBillNo().trim().equals("")) {
                jpql += " and  b.deptId like :billNo";
                params.put("billNo", "%" + filter.getBillNo().trim().toUpperCase() + "%");
            }

            if (filter.getNetTotal() != null && !filter.getNetTotal().trim().equals("")) {
                jpql += " and  ((b.netTotal) like :netTotal )";
                params.put("netTotal", "%" + filter.getNetTotal().trim().toUpperCase() + "%");
            }

            if (filter.getTotal() != null && !filter.getTotal().trim().equals("")) {
                jpql += " and  ((b.total) like :total )";
                params.put("total", "%" + filter.getTotal().trim().toUpperCase() + "%");
            }
        }

        jpql += " order by b.createdAt desc  ";

        outputRows = (List<ReportTemplateRow>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        outputBundle.setReportTemplateRows(outputRows);
        return outputBundle;
    }
}
