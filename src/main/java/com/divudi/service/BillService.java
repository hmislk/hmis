package com.divudi.service;

import com.divudi.data.BillCategory;
import com.divudi.data.BillTypeAtomic;
import static com.divudi.data.BillTypeAtomic.PHARMACY_DIRECT_PURCHASE;
import static com.divudi.data.BillTypeAtomic.PHARMACY_GRN;
import com.divudi.data.InstitutionType;
import com.divudi.data.PaymentMethod;
import static com.divudi.data.PaymentMethod.Agent;
import static com.divudi.data.PaymentMethod.Card;
import static com.divudi.data.PaymentMethod.Cash;
import static com.divudi.data.PaymentMethod.Cheque;
import static com.divudi.data.PaymentMethod.Credit;
import static com.divudi.data.PaymentMethod.MultiplePaymentMethods;
import static com.divudi.data.PaymentMethod.None;
import static com.divudi.data.PaymentMethod.OnCall;
import static com.divudi.data.PaymentMethod.OnlineSettlement;
import static com.divudi.data.PaymentMethod.PatientDeposit;
import static com.divudi.data.PaymentMethod.Slip;
import static com.divudi.data.PaymentMethod.Staff;
import static com.divudi.data.PaymentMethod.YouOweMe;
import static com.divudi.data.PaymentMethod.ewallet;
import com.divudi.data.ReportTemplateRow;
import com.divudi.data.ReportTemplateRowBundle;
import com.divudi.data.dataStructure.ComponentDetail;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Payment;
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
    @EJB
    DrawerService drawerService;

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

    public List<Bill> fetchBills(Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            WebUser webUser,
            List<BillTypeAtomic> billTypeAtomics) {
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
        System.out.println("jpql = " + jpql);
        System.out.println("params = " + params);
        List<BillItem> fetchedBillItems = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        System.out.println("fetchedBillItems = " + fetchedBillItems.size());
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
        System.out.println("jpql = " + jpql);
        System.out.println("params = " + params);
        List<BillItem> fetchedBillItems = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        System.out.println("fetchedBillItems = " + fetchedBillItems.size());
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
        System.out.println("jpql = " + jpql);
        System.out.println("params = " + params);
        List<Bill> fetchedBills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        return fetchedBills;
    }

    public List<BillItem> fetchPaymentBills(Bill inputBill) {
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
                + " and bi.referanceBillItem in :inputBillItems "
                + " and b.billTypeAtomic in :btas ";
        jpql += " order by b.createdAt";
        params.put("ret", false);
        params.put("btas", btas);
        params.put("inputBillItems", inputBill.getBillItems());
        System.out.println("jpql = " + jpql);
        System.out.println("params = " + params);
        List<BillItem> fetchedBillItems = billItemFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        return fetchedBillItems;
    }

}
