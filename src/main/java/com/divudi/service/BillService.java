package com.divudi.service;

import com.divudi.core.data.BillCategory;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.FeeType;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.PaymentMethod;

import static com.divudi.core.data.PaymentMethod.Cash;
import static com.divudi.core.data.PaymentMethod.MultiplePaymentMethods;

import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.data.ReportTemplateRowBundle;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import java.util.ArrayList;
import java.util.HashMap;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;
import com.google.gson.GsonBuilder;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Dr M H B Ariyaratne
 *
 */
@Stateless
public class BillService {

    @EJB
    DepartmentFacade departmentFacade;
    @EJB
    InstitutionFacade institutionFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    DrawerService drawerService;
    @EJB
    ItemFacade itemFacade;

    private static final Gson gson = new Gson();

    @Deprecated //Please use payment service > createPaymentMethod
    public List<Payment> createPayment(Bill bill, PaymentMethod pm, PaymentMethodData paymentMethodData) {
        List<Payment> ps = new ArrayList<>();

        if (paymentMethodData == null) {
            PaymentMethod npm;
            if (bill.getPaymentMethod() == null) {
                npm = Cash;
            }
            if (bill.getPaymentMethod() == MultiplePaymentMethods) {
                npm = Cash;
            } else {
                npm = bill.getPaymentMethod();
            }
            Payment p = new Payment();
            p.setBill(bill);
            p.setInstitution(bill.getInstitution());
            p.setDepartment(bill.getDepartment());
            p.setCreatedAt(bill.getCreatedAt());
            p.setCreater(bill.getCreater());
            p.setComments("Created Payments to correct Erros of Not creating a payment");
            p.setPaymentMethod(bill.getPaymentMethod());
            p.setPaidValue(bill.getNetTotal());
            paymentFacade.create(p);
            ps.add(p);
            return ps;
        }

        if (bill.getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                Payment p = new Payment();
                p.setBill(bill);
                p.setInstitution(bill.getInstitution());
                p.setDepartment(bill.getDepartment());
                p.setCreatedAt(new Date());
                p.setCreater(bill.getCreater());
                p.setPaymentMethod(cd.getPaymentMethod());

                switch (cd.getPaymentMethod()) {
                    case Card:
                        p.setBank(cd.getPaymentMethodData().getCreditCard().getInstitution());
                        p.setCreditCardRefNo(cd.getPaymentMethodData().getCreditCard().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCreditCard().getTotalValue());
                        break;
                    case Cheque:
                        p.setBank(cd.getPaymentMethodData().getCheque().getInstitution());
                        p.setChequeDate(cd.getPaymentMethodData().getCheque().getDate());
                        p.setChequeRefNo(cd.getPaymentMethodData().getCheque().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCheque().getTotalValue());
                        break;
                    case Cash:
                        p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
                        break;
                    case ewallet:
                        break;
                    case Agent:
                        break;
                    case Credit:
                        break;
                    case PatientDeposit:
                        break;
                    case Slip:
                        p.setPaidValue(cd.getPaymentMethodData().getSlip().getTotalValue());
                        p.setBank(cd.getPaymentMethodData().getSlip().getInstitution());
                        p.setRealizedAt(cd.getPaymentMethodData().getSlip().getDate());
                        p.setReferenceNo(cd.getPaymentMethodData().getSlip().getReferenceNo());
                        break;
                    case OnCall:
                        break;
                    case OnlineSettlement:
                        break;
                    case Staff:
                        p.setPaidValue(cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                        break;
                    case YouOweMe:
                        break;
                    case MultiplePaymentMethods:
                        break;
                }

                paymentFacade.create(p);
                ps.add(p);
            }
        } else {
            Payment p = new Payment();
            p.setBill(bill);
            p.setInstitution(bill.getInstitution());
            p.setDepartment(bill.getDepartment());
            p.setCreatedAt(new Date());
            p.setCreater(bill.getCreater());
            p.setPaymentMethod(pm);

            switch (pm) {
                case Card:
                    p.setBank(paymentMethodData.getCreditCard().getInstitution());
                    p.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
                    break;
                case Cheque:
                    p.setChequeDate(paymentMethodData.getCheque().getDate());
                    p.setChequeRefNo(paymentMethodData.getCheque().getNo());
                    break;
                case Cash:
                    break;
                case ewallet:
                    break;
                case Agent:
                    break;
                case Credit:
                    break;
                case PatientDeposit:
                    break;
                case Slip:
                    p.setBank(paymentMethodData.getSlip().getInstitution());
                    p.setRealizedAt(paymentMethodData.getSlip().getDate());
                case OnCall:
                    break;
                case OnlineSettlement:
                    break;
                case Staff:
                    break;
                case YouOweMe:
                    break;
                case MultiplePaymentMethods:
                    break;
            }

            p.setPaidValue(p.getBill().getNetTotal());
            paymentFacade.create(p);
            ps.add(p);
        }
        drawerService.updateDrawerForIns(ps);
        return ps;
    }

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
        double reagentFeeCalculatedByBillFees = 0.0;
        double additionalFeeCalculatedByBillFees = 0.0;

        List<BillFee> bfs = fetchBillFees(bi);
        System.out.println("bfs = " + bfs);

        for (BillFee bf : bfs) {
            System.out.println(bf.getFee().getFeeType() + " - " + bf.getFeeValue());
        }

        for (BillFee bf : bfs) {
            if (bf.getInstitution() != null && bf.getInstitution().getInstitutionType() == InstitutionType.CollectingCentre) {
                collectingCentreFeesCalculateByBillFees += bf.getFeeGrossValue();
            } else if (bf.getStaff() != null || bf.getSpeciality() != null) {
                staffFeesCalculatedByBillFees += bf.getFeeGrossValue();
            } else {
                hospitalFeeCalculatedByBillFess += bf.getFeeGrossValue();
            }
            if (bf.getFee().getFeeType() == FeeType.Chemical) {
                reagentFeeCalculatedByBillFees += bf.getFeeGrossValue();
            } else if (bf.getFee().getFeeType() == FeeType.Additional) {
                additionalFeeCalculatedByBillFees += bf.getFeeGrossValue();
            }

        }

        System.out.println("Hospital Fee  = " + hospitalFeeCalculatedByBillFess);
        System.out.println("Reagent Fee = " + reagentFeeCalculatedByBillFees);
        System.out.println("Staff Fees = " + staffFeesCalculatedByBillFees);
        System.out.println("Additional Fee = " + additionalFeeCalculatedByBillFees);

        bi.setCollectingCentreFee(collectingCentreFeesCalculateByBillFees);
        bi.setStaffFee(staffFeesCalculatedByBillFees);
        bi.setHospitalFee(hospitalFeeCalculatedByBillFess);
        bi.setReagentFee(reagentFeeCalculatedByBillFees);
        bi.setOtherFee(additionalFeeCalculatedByBillFees);
        billItemFacade.edit(bi);
    }

    public void createBillItemFeeBreakdownFromBill(Bill bill) {
        List<BillItem> billItems = fetchBillItems(bill);
        createBillItemFeeBreakdownAsHospitalFeeItemDiscount(billItems);
    }

    public void createBillItemFeeBreakdownFromBills(List<Bill> bills) {
        List<BillItem> billItems = fetchBillItems(bills);

        createBillItemFeeBreakdownAsHospitalFeeItemDiscount(billItems);

    }

    public List<Bill> fetchIndividualBillsOfBatchBill(Bill batchBill) {
        String j = "Select b "
                + " from Bill b "
                + " where b.backwardReferenceBill=:bb ";
        Map m = new HashMap();
        m.put("bb", batchBill);
        List<Bill> tbs = billFacade.findByJpql(j, m);
        return tbs;
    }

    public Bill fetchBatchBillOfIndividualBill(Bill individualBill) {
        String j = "SELECT b.backwardReferenceBill "
                + "FROM Bill b "
                + "WHERE b = :b";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("b", individualBill);
        Bill batchBill = (Bill) billFacade.findFirstByJpql(j, parameters);
        return batchBill;
    }

    public Bill fetchBillReferredAsReferenceBill(Bill referredBill, BillTypeAtomic billTypeAtomic) {
        return fetchBillByReferenceType("referenceBill", referredBill, billTypeAtomic);
    }

    public Bill fetchBillReferredAsBackwardReferenceBill(Bill referredBill, BillTypeAtomic billTypeAtomic) {
        return fetchBillByReferenceType("backwardReferenceBill", referredBill, billTypeAtomic);
    }

    public Bill fetchBillReferredAsForwardReferenceBill(Bill referredBill, BillTypeAtomic billTypeAtomic) {
        return fetchBillByReferenceType("forwardReferenceBill", referredBill, billTypeAtomic);
    }

    public Bill fetchBillReferredAsBilledBill(Bill referredBill, BillTypeAtomic billTypeAtomic) {
        return fetchBillByReferenceType("billedBill", referredBill, billTypeAtomic);
    }

    public Bill fetchBillReferredAsCancelledBill(Bill referredBill, BillTypeAtomic billTypeAtomic) {
        return fetchBillByReferenceType("cancelledBill", referredBill, billTypeAtomic);
    }

    public Bill fetchBillReferredAsRefundedBill(Bill referredBill, BillTypeAtomic billTypeAtomic) {
        return fetchBillByReferenceType("refundedBill", referredBill, billTypeAtomic);
    }

    public Bill fetchBillReferredAsReactivatedBill(Bill referredBill, BillTypeAtomic billTypeAtomic) {
        return fetchBillByReferenceType("reactivatedBill", referredBill, billTypeAtomic);
    }

    public Bill fetchBillReferredAsPaidBill(Bill referredBill, BillTypeAtomic billTypeAtomic) {
        return fetchBillByReferenceType("paidBill", referredBill, billTypeAtomic);
    }

    private Bill fetchBillByReferenceType(String referenceField, Bill referredBill, BillTypeAtomic billTypeAtomic) {
        String jpql = "SELECT b FROM Bill b WHERE b." + referenceField + " = :b AND b.billTypeAtomic = :billType";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("b", referredBill);
        parameters.put("billType", billTypeAtomic);
        return billFacade.findFirstByJpql(jpql, parameters);
    }

    public Bill fetchFirstBill() {
        String jpql = "SELECT b "
                + " FROM Bill b "
                + " where b.retired=:ret "
                + " order by b.id";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ret", false);
        return billFacade.findFirstByJpql(jpql, parameters);
    }

    public Bill fetchLastBill() {
        String jpql = "SELECT b "
                + " FROM Bill b "
                + " where b.retired=:ret "
                + " order by b.id desc";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ret", false);
        return billFacade.findFirstByJpql(jpql, parameters);
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

    public List<BillFee> fetchBillFees(Bill bill) {
        List<BillFee> fetchingBillFees;
        String jpql;
        Map params = new HashMap();
        jpql = "Select bf "
                + " from BillFee bf "
                + "where bf.bill=:bill "
                + "order by bf.billItem.id";
        params.put("bill", bill);
        fetchingBillFees = billFeeFacade.findByJpql(jpql, params);
        return fetchingBillFees;
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

    public Long fetchBillItemCount(Bill b) {
        String jpql;
        HashMap params = new HashMap();
        jpql = "SELECT count(bi) "
                + " FROM BillItem bi "
                + " WHERE bi.bill=:bl "
                + " order by bi.id";
        params.put("bl", b);
        return billItemFacade.findLongByJpql(jpql, params);
    }

    public List<BillItem> fetchBillItems(List<Bill> bills) {
        List<BillItem> allBillItems = new ArrayList<>();
        for (Bill b : bills) {
            allBillItems.addAll(fetchBillItems(b));
        }
        return allBillItems;
    }

    public void initiateBillItemsAndBillFees(Bill b) {
        if (b == null) {
            return;
        }
        if (b.getBillItems() == null) {
            b.setBillItems(fetchBillItems(b));
        }
        if (b.getBillItems() == null || b.getBillItems().isEmpty()) {
            return;
        }
        for (BillItem bi : b.getBillItems()) {
            if (bi.getBillFees() == null || bi.getBillFees().isEmpty()) {
                bi.setBillFees(fetchBillFees(bi));
            }
        }
    }

    public List<Payment> fetchBillPayments(Bill bill) {
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

    public List<Payment> fetchBillPayments(Bill bill, Bill batchBill) {
        List<Payment> fetchingBillComponents;
        String jpql;
        Map params = new HashMap();
        jpql = "Select p "
                + " from Payment p "
                + "where p.bill=:bill "
                + "order by p.id";
        if (batchBill != null) {
            params.put("bill", batchBill);
        }else{
            params.put("bill", bill);
        }
        fetchingBillComponents = paymentFacade.findByJpql(jpql, params);
        return fetchingBillComponents;
    }

    public Payment fetchBillPayment(Bill b) {
        if (b == null) {
            return null;
        }
        String jpql;
        HashMap params = new HashMap();
        jpql = "SELECT p "
                + " FROM Payment p "
                + " WHERE p.bill=:bl "
                + " order by p.id";
        params.put("bl", b);
        return paymentFacade.findFirstByJpql(jpql, params);
    }

    public void calculateBillBreakdownAsHospitalCcAndStaffTotalsByBillFees(Bill bill) {
        double billStaffFee = 0.0;
        double billCollectingCentreFee = 0.0;
        double billHospitalFee = 0.0;

        List<BillItem> billItems = fetchBillItems(bill);

        for (BillItem bi : billItems) {
            // Initialize fee accumulators for the current BillItem
            double staffFeesCalculatedByBillFees = 0.0;
            double collectingCentreFeesCalculatedByBillFees = 0.0;
            double hospitalFeeCalculatedByBillFees = 0.0;

            // Fetch BillFees for the current BillItem
            List<BillFee> billFees = fetchBillFees(bi);
            for (BillFee bf : billFees) {

                // Calculate fees based on InstitutionType, Staff, or Speciality
                if (bf.getInstitution() != null
                        && bf.getInstitution().getInstitutionType() == InstitutionType.CollectingCentre) {
                    collectingCentreFeesCalculatedByBillFees += bf.getFeeGrossValue();
                } else if (bf.getStaff() != null || bf.getSpeciality() != null) {
                    staffFeesCalculatedByBillFees += bf.getFeeGrossValue();
                } else {
                    hospitalFeeCalculatedByBillFees += bf.getFeeGrossValue();
                }
            }

            // Set calculated fees to the BillItem
            bi.setCollectingCentreFee(collectingCentreFeesCalculatedByBillFees);
            bi.setStaffFee(staffFeesCalculatedByBillFees);
            bi.setHospitalFee(hospitalFeeCalculatedByBillFees);
            billItemFacade.editAndCommit(bi);
            // Log the values set to the BillItem
            // Log the values set to the BillItem

            // Accumulate the fees to the Bill totals
            billCollectingCentreFee += collectingCentreFeesCalculatedByBillFees;
            billStaffFee += staffFeesCalculatedByBillFees;
            billHospitalFee += hospitalFeeCalculatedByBillFees;

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

        jpql = "select new com.divudi.core.data.ReportTemplateRow(b) "
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

    public List<Bill> fetchBills(Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            WebUser webUser,
            List<BillTypeAtomic> billTypeAtomics) {
        return fetchBills(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, null, null);
    }

    public List<Bill> fetchBills(Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            WebUser webUser,
            BillTypeAtomic billTypeAtomic) {
        return fetchBills(fromDate, toDate, institution, site, department, webUser,
                billTypeAtomic != null ? Collections.singletonList(billTypeAtomic) : null);
    }

    public List<Bill> fetchBills(Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            WebUser webUser,
            List<BillTypeAtomic> billTypeAtomics,
            AdmissionType admissionType,
            PaymentScheme paymentScheme) {
        String jpql;
        Map params = new HashMap();

        jpql = "select b "
                + " from Bill b "
                + " where b.retired=:ret "
                + " and b.billTypeAtomic in :billTypesAtomics "
                + " and b.createdAt between :fromDate and :toDate ";

        params.put("ret", false);
        params.put("billTypesAtomics", billTypeAtomics);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        if (institution != null) {
            jpql += " and b.institution=:ins ";
            params.put("ins", institution);
        }

        if (webUser != null) {
            jpql += " and b.creater=:user ";
            params.put("user", webUser);
        }

        if (department != null) {
            jpql += " and b.department=:dep ";
            params.put("dep", department);
        }

        if (site != null) {
            jpql += " and b.department.site=:site ";
            params.put("site", site);
        }

        if (admissionType != null) {
            jpql += " and b.patientEncounter.admissionType=:admissionType ";
            params.put("admissionType", admissionType);
        }

        if (paymentScheme != null) {
            jpql += " and b.paymentScheme=:paymentScheme ";
            params.put("paymentScheme", paymentScheme);
        }

        jpql += " order by b.createdAt desc  ";
        List<Bill> fetchedBills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        return fetchedBills;
    }
    
    public List<BillItem> fetchBillItems(Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            WebUser webUser,
            List<BillTypeAtomic> billTypeAtomics,
            AdmissionType admissionType,
            PaymentScheme paymentScheme) {
        String jpql;
        Map params = new HashMap();

        jpql = "select bi "
                + " from BillItem bi "
                + " join bi.bill b "
                + " where (b.retired=false or b.retired is null) "
                 + " and (bi.retired=false or bi.retired is null) "
                + " and b.billTypeAtomic in :billTypesAtomics "
                + " and b.createdAt between :fromDate and :toDate ";

        params.put("billTypesAtomics", billTypeAtomics);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        if (institution != null) {
            jpql += " and b.institution=:ins ";
            params.put("ins", institution);
        }

        if (webUser != null) {
            jpql += " and b.creater=:user ";
            params.put("user", webUser);
        }

        if (department != null) {
            jpql += " and b.department=:dep ";
            params.put("dep", department);
        }

        if (site != null) {
            jpql += " and b.department.site=:site ";
            params.put("site", site);
        }

        if (admissionType != null) {
            jpql += " and b.patientEncounter.admissionType=:admissionType ";
            params.put("admissionType", admissionType);
        }

        if (paymentScheme != null) {
            jpql += " and b.paymentScheme=:paymentScheme ";
            params.put("paymentScheme", paymentScheme);
        }

        jpql += " order by b.createdAt desc  ";
        List<BillItem> fetchedBillItems = billItemFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        return fetchedBillItems;
    }
    
    public List<PharmaceuticalBillItem> fetchPharmaceuticalBillItems(Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            WebUser webUser,
            List<BillTypeAtomic> billTypeAtomics,
            AdmissionType admissionType,
            PaymentScheme paymentScheme) {
        String jpql;
        Map params = new HashMap();

        jpql = "select pbi "
                + " from PharmaceuticalBillItem pbi "
                + " join pbi.billItem bi "
                + " join bi.bill b "
                + " where (b.retired=false or b.retired is null) "
                 + " and (bi.retired=false or bi.retired is null) "
                + " and b.billTypeAtomic in :billTypesAtomics "
                + " and b.createdAt between :fromDate and :toDate ";

        params.put("billTypesAtomics", billTypeAtomics);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        if (institution != null) {
            jpql += " and b.institution=:ins ";
            params.put("ins", institution);
        }

        if (webUser != null) {
            jpql += " and b.creater=:user ";
            params.put("user", webUser);
        }

        if (department != null) {
            jpql += " and b.department=:dep ";
            params.put("dep", department);
        }

        if (site != null) {
            jpql += " and b.department.site=:site ";
            params.put("site", site);
        }

        if (admissionType != null) {
            jpql += " and b.patientEncounter.admissionType=:admissionType ";
            params.put("admissionType", admissionType);
        }

        if (paymentScheme != null) {
            jpql += " and b.paymentScheme=:paymentScheme ";
            params.put("paymentScheme", paymentScheme);
        }

        jpql += " order by b.createdAt desc  ";
        List<PharmaceuticalBillItem> fetchedPharmaceuticalBillItems = pharmaceuticalBillItemFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        return fetchedPharmaceuticalBillItems;
    }

    public List<Bill> fetchBillsWithToInstitution(Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            Institution toInstitution,
            Department toDepartment,
            Institution toSite,
            WebUser webUser,
            List<BillTypeAtomic> billTypeAtomics,
            AdmissionType admissionType,
            PaymentScheme paymentScheme) {
        String jpql = "select b "
                + " from Bill b "
                + " where b.retired=:ret "
                + " and b.billTypeAtomic in :billTypesAtomics "
                + " and b.createdAt between :fromDate and :toDate ";
        Map<String, Object> params = new HashMap<>();

        params.put("ret", false);
        params.put("billTypesAtomics", billTypeAtomics);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        // From Institution
        if (institution != null) {
            jpql += " and b.institution=:ins ";
            params.put("ins", institution);
        }

        // From Department
        if (department != null) {
            jpql += " and b.department=:dep ";
            params.put("dep", department);
        }

        // From Site (via Department.site)
        if (site != null) {
            jpql += " and b.department.site=:site ";
            params.put("site", site);
        }

        // To Institution
        if (toInstitution != null) {
            jpql += " and b.toInstitution=:toIns ";
            params.put("toIns", toInstitution);
        }

        // To Department
        if (toDepartment != null) {
            jpql += " and b.toDepartment=:toDep ";
            params.put("toDep", toDepartment);
        }

        // To Site (via toDepartment.site)
        if (toSite != null) {
            jpql += " and b.toDepartment.site=:toSite ";
            params.put("toSite", toSite);
        }

        // WebUser
        if (webUser != null) {
            jpql += " and b.creater=:user ";
            params.put("user", webUser);
        }

        // Admission Type
        if (admissionType != null) {
            jpql += " and b.patientEncounter.admissionType=:admissionType ";
            params.put("admissionType", admissionType);
        }

        // Payment Scheme
        if (paymentScheme != null) {
            jpql += " and b.paymentScheme=:paymentScheme ";
            params.put("paymentScheme", paymentScheme);
        }

        jpql += " order by b.createdAt desc";

        return billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public List<Bill> fetchBills(Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            WebUser webUser,
            List<BillTypeAtomic> billTypeAtomics,
            AdmissionType admissionType,
            PaymentScheme paymentScheme,
            Institution toInstitution,
            Department toDepartment,
            String visitType
    ) {
        String jpql;
        Map params = new HashMap();

        jpql = "select b "
                + " from Bill b "
                + " where b.retired=:ret "
                + " and b.billTypeAtomic in :billTypesAtomics "
                + " and b.createdAt between :fromDate and :toDate ";

        params.put("ret", false);
        params.put("billTypesAtomics", billTypeAtomics);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        if (institution != null) {
            jpql += " and b.institution=:ins ";
            params.put("ins", institution);
        }

        if (webUser != null) {
            jpql += " and b.creater=:user ";
            params.put("user", webUser);
        }

        if (department != null) {
            jpql += " and b.department=:dep ";
            params.put("dep", department);
        }

        if (admissionType != null) {
            jpql += " and b.patientEncounter.admissionType=:admissionType ";
            params.put("admissionType", admissionType);
        }

        if (paymentScheme != null) {
            jpql += " and b.paymentScheme=:paymentScheme ";
            params.put("paymentScheme", paymentScheme);
        }

        if (toInstitution != null) {
            jpql += " and b.toInstitution=:toIns ";
            params.put("toIns", toInstitution);
        }

        if (toDepartment != null) {
            jpql += " and b.toDepartment=:toDep ";
            params.put("toDep", toDepartment);
        }

        if (visitType != null && !visitType.trim().isEmpty()) {
            jpql += " AND b.ipOpOrCc = :type";
            params.put("type", visitType.trim());
        }

        jpql += " order by b.createdAt desc  ";
        List<Bill> fetchedBills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        return fetchedBills;
    }

    public List<BillItem> fetchBillItems(Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            WebUser webUser,
            List<BillTypeAtomic> billTypeAtomics) {
        return fetchBillItems(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, null);
    }

    public List<BillItem> fetchBillItems(Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            WebUser webUser,
            List<BillTypeAtomic> billTypeAtomics,
            PatientEncounter patientEncounter) {
        String jpql;
        Map params = new HashMap();

        jpql = "select bi "
                + " from BillItem bi "
                + " join bi.bill b"
                + " where b.retired=:ret "
                + " and bi.retired=:ret "
                + " and b.billTypeAtomic in :billTypesAtomics ";

        params.put("ret", false);
        params.put("billTypesAtomics", billTypeAtomics);

        if (fromDate != null) {
            jpql += " and b.createdAt >= :fromDate ";
            params.put("fromDate", fromDate);
        }

        if (toDate != null) {
            jpql += " and b.createdAt <= :toDate ";
            params.put("toDate", toDate);
        }

        if (institution != null) {
            jpql += " and b.institution=:ins ";
            params.put("ins", institution);
        }

        if (webUser != null) {
            jpql += " and b.creater=:user ";
            params.put("user", webUser);
        }

        if (department != null) {
            jpql += " and b.department=:dep ";
            params.put("dep", department);
        }

        if (patientEncounter != null) {
            jpql += " and b.patientEncounter=:patientEncounter ";
            params.put("patientEncounter", patientEncounter);
        }

        jpql += " order by b.createdAt, bi.id ";
        List<BillItem> fetchedBillItems = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        return fetchedBillItems;
    }

    public List<BillItem> fetchBillItemsWithoutCancellationsAndReturns(Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            WebUser webUser,
            List<BillTypeAtomic> billTypeAtomics,
            PatientEncounter patientEncounter) {
        String jpql;
        Map params = new HashMap();

        jpql = "select bi "
                + " from BillItem bi "
                + " join bi.bill b"
                + " where b.retired=:ret "
                + " and b.cancelled=:bc "
                + " and bi.refunded=:bir "
                + " and bi.retired=:ret "
                + " and b.billTypeAtomic in :billTypesAtomics ";

        params.put("ret", false);
        params.put("bc", false);
        params.put("bir", false);
        params.put("billTypesAtomics", billTypeAtomics);

        if (fromDate != null) {
            jpql += " and b.createdAt >= :fromDate ";
            params.put("fromDate", fromDate);
        }

        if (toDate != null) {
            jpql += " and b.createdAt <= :toDate ";
            params.put("toDate", toDate);
        }

        if (institution != null) {
            jpql += " and b.institution=:ins ";
            params.put("ins", institution);
        }

        if (webUser != null) {
            jpql += " and b.creater=:user ";
            params.put("user", webUser);
        }

        if (department != null) {
            jpql += " and b.department=:dep ";
            params.put("dep", department);
        }

        if (patientEncounter != null) {
            jpql += " and b.patientEncounter=:patientEncounter ";
            params.put("patientEncounter", patientEncounter);
        }

        jpql += " order by b.createdAt, bi.id ";
        List<BillItem> fetchedBillItems = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        return fetchedBillItems;
    }

    public List<Bill> fetchReturnBills(Bill inputBill) {
        String jpql;
        if (inputBill == null) {
            return null;
        }
        if (inputBill.getBillTypeAtomic() == null) {
            return null;
        }
        List<BillTypeAtomic> btas = new ArrayList<>();
        switch (inputBill.getBillTypeAtomic()) {
            case PHARMACY_GRN:
                btas.add(BillTypeAtomic.PHARMACY_GRN_RETURN);
                break;
            case PHARMACY_DIRECT_PURCHASE:
                btas.add(BillTypeAtomic.PHARMACY_GRN_RETURN);
                break;
            default:
                btas.addAll(BillTypeAtomic.findByCategory(BillCategory.REFUND));
        }
        //
        Map params = new HashMap();
        jpql = "select b "
                + " from Bill b "
                + " where b.retired=:ret "
                + " and (b.billedBill=:bill or b.referenceBill=:bill) "
                + " and b.billTypeAtomic in :btas ";
        jpql += " order by b.createdAt";
        params.put("ret", false);
        params.put("btas", btas);
        params.put("bill", inputBill);
        List<Bill> fetchedBills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        return fetchedBills;
    }

    public List<Bill> fetchAllReferanceBills(Bill inputBill) {
        if (inputBill == null || inputBill.getBillTypeAtomic() == null) {
            return null;
        }

        Set<Bill> allRefBills = new LinkedHashSet<>();

        if (inputBill.getReferenceBill() != null) {
            allRefBills.add(inputBill.getReferenceBill());
        }
        if (inputBill.getBilledBill() != null) {
            allRefBills.add(inputBill.getBilledBill());
        }
        if (inputBill.getBackwardReferenceBill() != null) {
            allRefBills.add(inputBill.getBackwardReferenceBill());
        }
        if (inputBill.getForwardReferenceBill() != null) {
            allRefBills.add(inputBill.getForwardReferenceBill());
        }
        if (inputBill.getPaidBill() != null) {
            allRefBills.add(inputBill.getPaidBill());
        }

        String jpql = "select b from Bill b "
                + "where b.billedBill = :bill "
                + "or b.referenceBill = :bill "
                + "or b.backwardReferenceBill = :bill "
                + "or b.forwardReferenceBill = :bill "
                + "or b.paidBill = :bill "
                + "order by b.createdAt";

        Map<String, Object> params = new HashMap<>();
        params.put("bill", inputBill);

        List<Bill> fetchedBills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (fetchedBills != null) {
            allRefBills.addAll(fetchedBills);
        }

        return new ArrayList<>(allRefBills);
    }

    public List<BillItem> fetchPaymentBillItems(Bill inputBill) {
        String jpql;
        if (inputBill == null) {
            return null;
        }
        if (inputBill.getBillTypeAtomic() == null) {
            return null;
        }
        List<BillTypeAtomic> btas = new ArrayList<>();
        switch (inputBill.getBillTypeAtomic()) {
            case PHARMACY_GRN:
            case PHARMACY_DIRECT_PURCHASE:
                btas.add(BillTypeAtomic.SUPPLIER_PAYMENT);
                btas.add(BillTypeAtomic.SUPPLIER_PAYMENT_CANCELLED);
                btas.add(BillTypeAtomic.SUPPLIER_PAYMENT_RETURNED);
                break;
            default:
                btas.addAll(BillTypeAtomic.findByCategory(BillCategory.PAYMENTS));
        }
        //
        Map params = new HashMap();
        jpql = "select bi "
                + " from BillItem bi"
                + " join bi.bill b "
                + " where b.retired=:ret "
                + " and bi.referenceBill = :bill "
                + " and b.billTypeAtomic in :btas ";
        jpql += " order by b.createdAt";
        params.put("ret", false);
        params.put("btas", btas);
        params.put("bill", inputBill);
        List<BillItem> fetchedBillItems = billItemFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        return fetchedBillItems;
    }

    public String convertBillToJson(Bill bill) {
        if (bill == null) {
            return "{}";
        }
        if (bill.getBillTypeAtomic() == null) {
            return "{}";
        }
        switch (bill.getBillTypeAtomic()) {
            case PHARMACY_GRN:
                return convertPharmacyGrnBillToJson(bill);
        }
        return "{}";
    }

    public String convertPharmacyGrnBillToJson(Bill bill) {
        if (bill == null) {
            return "{}";
        }

        Map<String, Object> billMap = new LinkedHashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Extract relevant Bill fields
        billMap.put("billTypeAtomic", bill.getBillTypeAtomic() != null ? bill.getBillTypeAtomic() : null);
        billMap.put("referenceBill_DeptId", bill.getReferenceBill() != null ? bill.getReferenceBill().getDeptId() : null);
        billMap.put("deptId", bill.getDeptId());
        billMap.put("createdAt", dateFormat.format(bill.getCreatedAt()));
        billMap.put("invoiceNo", bill.getInvoiceNumber());
        billMap.put("department_id", bill.getDepartment() != null ? bill.getDepartment().getId() : null);
        billMap.put("supplier_id", bill.getFromInstitution() != null ? bill.getFromInstitution().getId() : null);
        billMap.put("paymentMethod", bill.getPaymentMethod());

        List<Map<String, Object>> billItemsList = new ArrayList<>();
        if (bill.getBillItems() != null) {
            for (BillItem bip : bill.getBillItems()) {
                Map<String, Object> itemMap = new LinkedHashMap<>();
                itemMap.put("item_id", bip.getItem().getId());
                itemMap.put("expiry", bip.getPharmaceuticalBillItem() != null ? dateFormat.format(bip.getPharmaceuticalBillItem().getDoe()) : null);
                itemMap.put("batchNo", bip.getPharmaceuticalBillItem() != null ? bip.getPharmaceuticalBillItem().getStringValue() : null);
                itemMap.put("receivedQty", bip.getPharmaceuticalBillItem() != null ? bip.getPharmaceuticalBillItem().getQty() : null);
                itemMap.put("receivedFreeQty", bip.getPharmaceuticalBillItem() != null ? bip.getPharmaceuticalBillItem().getFreeQty() : null);
                itemMap.put("purchasePrice", bip.getPharmaceuticalBillItem() != null ? bip.getPharmaceuticalBillItem().getPurchaseRate() : null);
                itemMap.put("salePrice", bip.getPharmaceuticalBillItem() != null ? bip.getPharmaceuticalBillItem().getRetailRate() : null);
                itemMap.put("purchaseValue", bip.getPharmaceuticalBillItem() != null ? bip.getPharmaceuticalBillItem().getPurchaseRate() * bip.getPharmaceuticalBillItem().getQty() : null);
                itemMap.put("saleValue", bip.getPharmaceuticalBillItem() != null ? bip.getPharmaceuticalBillItem().getRetailRate() * bip.getPharmaceuticalBillItem().getQty() : null);
                billItemsList.add(itemMap);
            }
        }
        billMap.put("billItems", billItemsList);

        // Extract financial details
        billMap.put("saleValue", bill.getSaleValue());
        billMap.put("total", bill.getTotal());
        billMap.put("tax", bill.getTax());
        billMap.put("discount", bill.getDiscount());
        billMap.put("netTotal", bill.getNetTotal());

        // Convert to JSON
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(billMap);
    }

    public Bill convertJsonToBill(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        if (!jsonObject.has("billTypeAtomic")) {
            return null;
        }

        String billTypeAtomic = jsonObject.get("billTypeAtomic").getAsString();
        switch (billTypeAtomic) {
            case "PHARMACY_GRN":
                return importPharmacyGrnBillFromJson(jsonObject);
            default:
                return null;
        }
    }

    public Bill importPharmacyGrnBillFromJson(JsonObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Bill bill = new Bill();
        bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN);
        bill.setDeptId(jsonObject.has("deptId") ? jsonObject.get("deptId").getAsString() : null);
        bill.setInvoiceNumber(jsonObject.has("invoiceNo") ? jsonObject.get("invoiceNo").getAsString() : null);

        String paymentMethodString = jsonObject.has("paymentMethod") ? jsonObject.get("paymentMethod").getAsString() : null;
        PaymentMethod pm = PaymentMethod.valueOf(paymentMethodString);
        bill.setPaymentMethod(pm);

        if (jsonObject.has("createdAt")) {
            try {
                Date createdAt = dateFormat.parse(jsonObject.get("createdAt").getAsString());
                bill.setCreatedAt(createdAt);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (jsonObject.has("department_id")) {
            Department department = departmentFacade.find(jsonObject.get("department_id").getAsLong());
            bill.setDepartment(department);
        }

        if (jsonObject.has("supplier_id")) {
            Institution supplier = institutionFacade.find(jsonObject.get("supplier_id").getAsLong());
            bill.setFromInstitution(supplier);
        }

        if (jsonObject.has("billItems")) {
            List<Map<String, Object>> billItemsList = gson.fromJson(jsonObject.get("billItems"), List.class);
            for (Map<String, Object> itemMap : billItemsList) {
                BillItem billItem = new BillItem();
                Item item = itemFacade.find(((Number) itemMap.get("item_id")).longValue());

                billItem.setItem(item);

                PharmaceuticalBillItem pharmaceuticalBillItem = billItem.getPharmaceuticalBillItem();

                if (itemMap.get("expiry") != null) {
                    try {
                        pharmaceuticalBillItem.setDoe(dateFormat.parse(itemMap.get("expiry").toString()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                pharmaceuticalBillItem.setStringValue((String) itemMap.get("batchNo"));
                pharmaceuticalBillItem.setQty(Double.parseDouble(itemMap.get("receivedQty").toString()));
                pharmaceuticalBillItem.setFreeQty(Double.parseDouble(itemMap.get("receivedFreeQty").toString()));
                pharmaceuticalBillItem.setPurchaseRate(Double.parseDouble(itemMap.get("purchasePrice").toString()));
                pharmaceuticalBillItem.setRetailRate(Double.parseDouble(itemMap.get("salePrice").toString()));

                bill.getBillItems().add(billItem);
            }
        }

        if (jsonObject.has("saleValue")) {
            bill.setSaleValue(jsonObject.get("saleValue").getAsDouble());
        }
        if (jsonObject.has("total")) {
            bill.setTotal(jsonObject.get("total").getAsDouble());
        }
        if (jsonObject.has("tax")) {
            bill.setTax(jsonObject.get("tax").getAsDouble());
        }
        if (jsonObject.has("discount")) {
            bill.setDiscount(jsonObject.get("discount").getAsDouble());
        }
        if (jsonObject.has("netTotal")) {
            bill.setNetTotal(jsonObject.get("netTotal").getAsDouble());
        }

        return bill;
    }

    public boolean checkBillForErrors(Bill bill) {
        if (bill == null) {
            return true;
        }
        if (bill.getBillTypeAtomic() == null) {
            bill.setTmpComments("No Bill Type Atomic");
            return true;
        }
        boolean hasAtLeatOneError = false;
        switch (bill.getBillTypeAtomic()) {
            case PHARMACY_GRN:
            case PHARMACY_ORDER:
            case PHARMACY_ORDER_APPROVAL:
                boolean billNetTotalIsNotEqualToBillItemNetTotalError = billNetTotalIsNotEqualToBillItemNetTotal(bill);
                System.out.println("billNetTotalIsNotEqualToBillItemNetTotalError = " + billNetTotalIsNotEqualToBillItemNetTotalError);
                if (billNetTotalIsNotEqualToBillItemNetTotalError) {
                    hasAtLeatOneError = true;
                }
                break;
            default:
                hasAtLeatOneError = false;

        }
        System.out.println("hasAtLeatOneError = " + hasAtLeatOneError);
        return hasAtLeatOneError;
    }

    public boolean billNetTotalIsNotEqualToBillItemNetTotal(Bill bill) {
        if (bill == null || bill.getBillItems() == null) {
            return true;
        }

        double billNetTotal = Math.abs(bill.getNetTotal());
        double billItemNetTotal = 0.0;

        for (BillItem bi : bill.getBillItems()) {
            if (bi != null) {
                billItemNetTotal += Math.abs(bi.getNetValue());
            }
        }
        boolean billNetTotalIsNotEqualToBillItemNetTotalError = Math.abs(billNetTotal - billItemNetTotal) >= 0.01;
        System.out.println("billNetTotalIsNotEqualToBillItemNetTotalError = " + billNetTotalIsNotEqualToBillItemNetTotalError);
        return billNetTotalIsNotEqualToBillItemNetTotalError;
    }

}
