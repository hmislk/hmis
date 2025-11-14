package com.divudi.service;

import com.divudi.core.data.BillCategory;

import static com.divudi.core.data.BillCategory.BILL;
import static com.divudi.core.data.BillCategory.CANCELLATION;
import static com.divudi.core.data.BillCategory.PAYMENTS;
import static com.divudi.core.data.BillCategory.PREBILL;
import static com.divudi.core.data.BillCategory.REFUND;
import com.divudi.core.data.BillClassType;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BillType;

import static com.divudi.core.data.BillTypeAtomic.CC_PAYMENT_CANCELLATION_BILL;
import static com.divudi.core.data.BillTypeAtomic.CC_PAYMENT_MADE_BILL;
import static com.divudi.core.data.BillTypeAtomic.CC_PAYMENT_MADE_CANCELLATION_BILL;
import static com.divudi.core.data.BillTypeAtomic.CC_PAYMENT_RECEIVED_BILL;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.FeeType;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.PaymentMethod;

import static com.divudi.core.data.PaymentMethod.Cash;
import static com.divudi.core.data.PaymentMethod.MultiplePaymentMethods;

import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.data.ReportTemplateRowBundle;
import com.divudi.core.data.ServiceType;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dto.ItemMovementSummaryDTO;
import com.divudi.core.data.dto.LabDailySummaryDTO;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.core.data.dto.LabIncomeReportDTO;
import com.divudi.core.data.dto.OpdSaleSummaryDTO;
import com.divudi.core.data.dto.PharmacyIncomeBillDTO;
import com.divudi.core.data.dto.PharmacyIncomeBillItemDTO;
import com.divudi.core.data.dto.OpdIncomeReportDTO;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.cashTransaction.DenominationTransaction;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;

import com.divudi.core.data.dto.PharmacyIncomeCostBillDTO;

import com.divudi.core.entity.Category;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.DenominationTransactionFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.light.common.BillLight;

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

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Dr M H B Ariyaratne
 */
@Stateless
public class BillService {

    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private DenominationTransactionFacade denominationTransactionFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private DrawerService drawerService;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;

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

        bi.setCollectingCentreFee(collectingCentreFeesCalculateByBillFees);
        bi.setStaffFee(staffFeesCalculatedByBillFees);
        bi.setHospitalFee(hospitalFeeCalculatedByBillFess);
        bi.setReagentFee(reagentFeeCalculatedByBillFees);
        bi.setOtherFee(additionalFeeCalculatedByBillFees);
        billItemFacade.edit(bi);
    }

    public void createBillItemFeesAndAssignToNewBillItem(BillItem originalBillItem, BillItem duplicateBillItem) {
        double staffFeesCalculatedByBillFees = 0.0;
        double collectingCentreFeesCalculateByBillFees = 0.0;
        double hospitalFeeCalculatedByBillFess = 0.0;
        double reagentFeeCalculatedByBillFees = 0.0;
        double additionalFeeCalculatedByBillFees = 0.0;

        List<BillFee> originalBillfess = fetchBillFees(originalBillItem);

        List<BillFee> duplicateBillfess = new ArrayList<>();

        for (BillFee bf : originalBillfess) {
            BillFee duplicateBillFee = new BillFee();
            duplicateBillFee.copy(bf);
            duplicateBillFee.setBillItem(duplicateBillItem);
            duplicateBillFee.setBill(duplicateBillItem.getBill());
            duplicateBillFee.setCreatedAt(new Date());
            billFeeFacade.create(duplicateBillFee);
            duplicateBillfess.add(duplicateBillFee);
        }

        for (BillFee bf : duplicateBillfess) {
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

        duplicateBillItem.setCollectingCentreFee(collectingCentreFeesCalculateByBillFees);
        duplicateBillItem.setStaffFee(staffFeesCalculatedByBillFees);
        duplicateBillItem.setHospitalFee(hospitalFeeCalculatedByBillFess);
        duplicateBillItem.setReagentFee(reagentFeeCalculatedByBillFees);
        duplicateBillItem.setOtherFee(additionalFeeCalculatedByBillFees);
        billItemFacade.edit(duplicateBillItem);
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

    public boolean hasMultipleIndividualBillsForThisBatchBill(Bill batchBill) {
        if (batchBill == null) {
            return false;
        }
        String j = "SELECT COUNT(b) FROM Bill b WHERE b.backwardReferenceBill = :batch";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("batch", batchBill);
        Long count = billFacade.findLongByJpql(j, parameters);
        return count != null && count > 1;
    }

    public boolean hasMultipleIndividualBillsForBatchBillOfThisIndividualBill(Bill individualBill) {
        if (individualBill == null) {
            return false;
        }
        Bill batchBill = fetchBatchBillOfIndividualBill(individualBill);
        if (batchBill == null) {
            return false;
        }
        String j = "SELECT COUNT(b) FROM Bill b WHERE b.backwardReferenceBill = :batch";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("batch", batchBill);
        Long count = billFacade.findLongByJpql(j, parameters);
        return count != null && count > 1;
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

    public Bill fetchBillById(Long id) {
        String jpql = "SELECT b "
                + " FROM Bill b "
                + " where b.id=:bid "
                + " order by b.id";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("bid", id);
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
    
    public Bill reloadBill(Long billId) {
        if (billId == null) {
            return null;
        }
        Bill bill = fetchBillById(billId);
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
        Map<String, Object> params = new HashMap<>();
        params.put("bi", billItem);
        params.put("ret", false);
        return billFeeFacade.findByJpql(jpql, params);
    }

    public List<BillTypeAtomic> fetchAllUtilizedBillTypeAtomics() {
        String jpql = "SELECT DISTINCT b.billTypeAtomic "
                + "FROM Bill b "
                + "WHERE b.retired = :ret";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        return (List<BillTypeAtomic>) billFacade.findLightsByJpql(jpql, params);
    }

    public List<BillFee> fetchBillFees(Bill bill) {
        List<BillFee> fetchingBillFees;
        String jpql;
        Map<String, Object> params = new HashMap<>();
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
        HashMap<String, Object> params = new HashMap<>();
        jpql = "SELECT bi "
                + " FROM BillItem bi "
                + " WHERE bi.bill=:bl "
                + " and (bi.retired is null or bi.retired=false) "
                + " order by bi.id";
        params.put("bl", b);
        return billItemFacade.findByJpql(jpql, params);
    }

    /**
     * Fetches bill type atomics for OPD finance operations, now including all pharmacy credit bills
     * as part of the credit consolidation initiative where pharmacy credit bills are managed
     * alongside OPD credit bills under the unified OPD Credit Settle bill type.
     * This includes pharmacy retail sales, wholesale sales, and sales settled at cashier.
     */
    public List<BillTypeAtomic> fetchBillTypeAtomicsForOpdFinance() {
        List<BillTypeAtomic> btas = new ArrayList<>();
        // OPD Bill Types
        btas.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
        btas.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        btas.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
        btas.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
        btas.add(BillTypeAtomic.OPD_BILL_REFUND);
        // Package OPD Bill Types
        btas.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        btas.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
        btas.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
        btas.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
        btas.add(BillTypeAtomic.PACKAGE_OPD_BILL_REFUND);
        // Pharmacy Bill Types (consolidated with OPD for credit management)
        // Pharmacy Retail Sales
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE);
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER);
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED);
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_REFUND);
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS);
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);
        // Pharmacy Wholesale
        btas.add(BillTypeAtomic.PHARMACY_WHOLESALE);
        btas.add(BillTypeAtomic.PHARMACY_WHOLESALE_PRE);
        btas.add(BillTypeAtomic.PHARMACY_WHOLESALE_CANCELLED);
        btas.add(BillTypeAtomic.PHARMACY_WHOLESALE_REFUND);
        // Pharmacy Wholesale GRN
        btas.add(BillTypeAtomic.PHARMACY_GRN_WHOLESALE);
        return btas;
    }

    public List<BillTypeAtomic> fetchBillTypeAtomicsForOnlyOpdBills() {
        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
        btas.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        btas.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
        btas.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
        btas.add(BillTypeAtomic.OPD_BILL_REFUND);
        return btas;
    }

    public List<BillTypeAtomic> fetchBillTypeAtomicsForPharmacyRetailSaleAndOpdSaleBills() {
        List<BillTypeAtomic> btas = new ArrayList<>();

        // OPD-related
        btas.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
        btas.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        btas.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
        btas.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
        btas.add(BillTypeAtomic.OPD_BILL_REFUND);

        // Pharmacy Retail Sale
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE);
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_WITHOUT_STOCKS);
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_WITHOUT_STOCKS);
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER);
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED);
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED_PRE);
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_REFUND);
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY);
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS);
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS_PREBILL);
        btas.add(BillTypeAtomic.PHARMACY_SALE_WITHOUT_STOCK);
        btas.add(BillTypeAtomic.PHARMACY_SALE_WITHOUT_STOCK_PRE);
        btas.add(BillTypeAtomic.PHARMACY_SALE_WITHOUT_STOCK_CANCELLED);
        btas.add(BillTypeAtomic.PHARMACY_SALE_WITHOUT_STOCK_REFUND);
        btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_ADD_TO_STOCK);

        return btas;
    }

    public List<BillTypeAtomic> fetchBillTypeAtomicsForOnlyPackageBills() {
        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        btas.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
        btas.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
        btas.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
        btas.add(BillTypeAtomic.PACKAGE_OPD_BILL_REFUND);
        return btas;
    }

    public List<PharmaceuticalBillItem> fetchPharmaceuticalBillItems(Bill b) {
        String jpql;
        HashMap<String, Object> params = new HashMap<>();
        jpql = "SELECT pbi "
                + " FROM PharmaceuticalBillItem pbi "
                + " WHERE pbi.billItem.bill=:bl "
                + " order by pbi.id";
        params.put("bl", b);
        return pharmaceuticalBillItemFacade.findByJpql(jpql, params);
    }

    public Long fetchBillItemCount(Bill b) {
        String jpql;
        HashMap<String, Object> params = new HashMap<>();
        jpql = "SELECT count(bi) "
                + " FROM BillItem bi "
                + " WHERE bi.bill=:bl "
                + " and (bi.retired is null or bi.retired=false) "
                + " order by bi.id";
        params.put("bl", b);
        return billItemFacade.findLongByJpql(jpql, params);
    }

    public Long calulateRevenueBillItemCount(Date fd, Date td, PaymentMethod pm,
            Institution institution, Department department, BillType[] billTypes, Class bc) {
        String sql;
        Map m = new HashMap();
        sql = "select count(bi) "
                + " from BillItem bi "
                + " where bi.bill.retired=false "
                + " and bi.bill.billType in :billTypes "
                + " and bi.bill.createdAt between :fd and :td "
                + " and type(bi.bill) =:bc ";
        if (pm != null) {
            sql += " and bi.bill.paymentMethod=:pm ";
            m.put("pm", pm);
        }
        if (institution != null) {
            sql += " and bi.bill.toInstitution=:ins ";
            m.put("ins", institution);
        }

        if (department != null) {
            sql += " and bi.bill.toDepartment=:dep ";
            m.put("dep", department);
        }

        List<BillType> bts = Arrays.asList(billTypes);

        m.put("billTypes", bts);
        m.put("bc", bc);
        m.put("fd", fd);
        m.put("td", td);

        return billFacade.findLongByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public Long calulateRevenueBillItemCount(Date fd, Date td, PaymentMethod pm,
            Institution institution, Department department, BillType[] billTypes) {
        return calulateRevenueBillItemCount(fd, td, pm, institution, department, billTypes, BilledBill.class)
                - calulateRevenueBillItemCount(fd, td, pm, institution, department, billTypes, CancelledBill.class)
                - calulateRevenueBillItemCount(fd, td, pm, institution, department, billTypes, RefundBill.class);
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
    public List<Payment> fetchBillPaymentsFromBillId(Long billId) {
        List<Payment> fetchingBillComponents;
        String jpql;
        Map<String, Object> params = new HashMap<>();
        jpql = "Select p "
                + " from Payment p "
                + "where p.bill.id=:billId "
                + "order by p.id";
        params.put("billId", billId);
        fetchingBillComponents = paymentFacade.findByJpql(jpql, params);
        return fetchingBillComponents;
    }
    

    public List<Payment> fetchBillPayments(Bill bill) {
        List<Payment> fetchingBillComponents;
        String jpql;
        Map<String, Object> params = new HashMap<>();
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
        Map<String, Object> params = new HashMap<>();
        jpql = "Select p "
                + " from Payment p "
                + "where p.bill=:bill "
                + "order by p.id";
        if (batchBill != null) {
            params.put("bill", batchBill);
        } else {
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
        HashMap<String, Object> params = new HashMap<>();
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
        Map<String, Object> params = new HashMap<>();

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
        Map<String, Object> params = new HashMap<>();

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
        System.out.println("jpql = " + jpql);
        System.out.println("params = " + params);
        List<Bill> fetchedBills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        return fetchedBills;
    }
    
    public List<BillLight> fetchBillDtos(Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            List<BillTypeAtomic> billTypeAtomics,
            AdmissionType admissionType,
            PaymentScheme paymentScheme) {
        return fetchBillDtos(fromDate, toDate, institution, site, department, null, billTypeAtomics, admissionType, paymentScheme);
    }
    
    
    
    public List<BillLight> fetchBillDtos(
            Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            WebUser webUser,
            List<BillTypeAtomic> billTypeAtomics,
            AdmissionType admissionType,
            PaymentScheme paymentScheme) {
        String jpql;
        Map<String, Object> params = new HashMap<>();

        jpql = "select new com.divudi.core.light.common.BillLight( b.id, b.billTypeAtomic, b.netTotal ) "
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
        }else{
            jpql += " and b.paymentScheme is null";
        }
        

        jpql += " order by b.createdAt desc  ";
        List<BillLight> fetchedBills = (List<BillLight>) billFacade.findLightsByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);
        return fetchedBills;
    }

    /**
     * Fetches BillLight objects with comprehensive financial details including stock values.
     * Used for pharmacy reports that require cost, purchase, and retail sale values.
     */
    public List<BillLight> fetchBillLightsWithFinanceDetails(
            Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            WebUser webUser,
            List<BillTypeAtomic> billTypeAtomics,
            AdmissionType admissionType,
            PaymentScheme paymentScheme) {
        String jpql;
        Map<String, Object> params = new HashMap<>();

        jpql = "select new com.divudi.core.light.common.BillLight("
                + " b.id, "
                + " b.billTypeAtomic, "
                + " b.total, "
                + " b.netTotal, "
                + " b.discount, "
                + " b.margin, "
                + " b.serviceCharge, "
                + " coalesce(bfd.totalCostValue, 0.0), "
                + " coalesce(bfd.totalPurchaseValue, 0.0), "
                + " coalesce(bfd.totalRetailSaleValue, 0.0), "
                + " b.paymentMethod, "
                + " b.patientEncounter "
                + ") "
                + " from Bill b "
                + " left join b.billFinanceDetails bfd "
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

        jpql += " order by b.createdAt desc ";

        List<BillLight> fetchedBills = (List<BillLight>) billFacade.findLightsByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);
        return fetchedBills;
    }

    public List<LabIncomeReportDTO> fetchBillsAsLabIncomeReportDTOs(Date fromDate,
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
            String visitType) {

        String jpql = "select new com.divudi.core.data.dto.LabIncomeReportDTO("
                + " b.id, "
                + " b.deptId, "
                + " b.createdAt, "
                + " coalesce(b.netTotal,0.0), "
                + " coalesce(b.total,0.0), "
                + " b.billTypeAtomic, "
                + " b.paymentMethod, "
                + " coalesce(b.discount,0.0), "
                + " coalesce(b.serviceCharge,0.0), "
                + " b.paymentScheme) "
                + " from Bill b "
                + " where b.retired=:ret "
                + " and b.billTypeAtomic in :billTypesAtomics "
                + " and b.createdAt between :fromDate and :toDate ";

        Map<String, Object> params = new HashMap<>();
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

        if (toInstitution != null) {
            jpql += " and b.toInstitution=:toIns ";
            params.put("toIns", toInstitution);
        }

        if (toDepartment != null) {
            jpql += " and b.toDepartment=:toDep ";
            params.put("toDep", toDepartment);
        }

        if (visitType != null && !visitType.trim().isEmpty()) {
            jpql += " and b.ipOpOrCc=:type ";
            params.put("type", visitType.trim());
        }

        jpql += " order by b.createdAt desc";

        List<LabIncomeReportDTO> results = (List<LabIncomeReportDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        return results;
    }

    public List<PharmacyIncomeCostBillDTO> fetchBillIncomeCostDtos(Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            WebUser webUser,
            List<BillTypeAtomic> billTypeAtomics,
            AdmissionType admissionType,
            PaymentScheme paymentScheme) {

        String jpql = "select new com.divudi.core.data.dto.PharmacyIncomeCostBillDTO("
                + " b.id, b.deptId, b.billTypeAtomic, "
                + " coalesce(pers.name,'N/A'), coalesce(pe.bhtNo,''), b.createdAt, "
                + " coalesce(bfd.totalRetailSaleValue,0), coalesce(bfd.totalPurchaseValue,0)) "
                + " from Bill b "
                + " left join b.billFinanceDetails bfd "
                + " left join b.patient pat "
                + " left join pat.person pers "
                + " left join b.patientEncounter pe "
                + " where b.retired=:ret "
                + " and b.billTypeAtomic in :billTypesAtomics "
                + " and b.createdAt between :fromDate and :toDate ";

        Map<String, Object> params = new HashMap<>();
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

        jpql += " order by b.createdAt desc";

        return (List<PharmacyIncomeCostBillDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public List<PharmacyIncomeBillDTO> fetchBillsAsPharmacyIncomeBillDTOs(Date fromDate,
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

        jpql = "select new com.divudi.core.data.dto.PharmacyIncomeBillDTO("
                + " b.id, b.deptId, coalesce(pers.name,'N/A'), b.billTypeAtomic, b.createdAt, coalesce(b.netTotal, 0.0), b.paymentMethod, coalesce(b.total, 0.0), "
                + " b.patientEncounter, coalesce(b.discount, 0.0), coalesce(b.margin, 0.0), coalesce(b.serviceCharge, 0.0), b.paymentScheme, "
                + " coalesce(bfd.totalRetailSaleValue, 0.0), coalesce(bfd.totalPurchaseValue, 0.0), coalesce(bfd.totalCostValue, 0.0) ) "
                + " from Bill b "
                + " left join b.billFinanceDetails bfd "
                + " left join b.patient pat "
                + " left join pat.person pers "
                + " left join b.patientEncounter pe "
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


        List<PharmacyIncomeBillDTO> results = (List<PharmacyIncomeBillDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);


        if (results != null && !results.isEmpty()) {
            for (int i = 0; i < Math.min(5, results.size()); i++) {
                PharmacyIncomeBillDTO dto = results.get(i);
            }
        }

        return results;
    }

    public List<OpdIncomeReportDTO> fetchOpdIncomeReportDTOs(Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            WebUser webUser,
            List<BillTypeAtomic> billTypeAtomics,
            AdmissionType admissionType,
            PaymentScheme paymentScheme) {

        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate and toDate cannot be null");
        }
        if (fromDate.after(toDate)) {
            throw new IllegalArgumentException("fromDate cannot be after toDate");
        }

        String jpql = "select new com.divudi.core.data.dto.OpdIncomeReportDTO("
                + " b.id, b.deptId, coalesce(pers.name,'N/A'), b.billTypeAtomic, b.createdAt, "
                + " coalesce(b.netTotal,0.0), b.paymentMethod, coalesce(b.total,0.0), "
                + " pe, coalesce(b.discount,0.0), coalesce(b.margin,0.0), "
                + " coalesce(b.serviceCharge,0.0), b.paymentScheme ) "
                + " from Bill b "
                + " left join b.patient pat "
                + " left join pat.person pers "
                + " left join b.patientEncounter pe "
                + " where b.retired=:ret "
                + " and b.billTypeAtomic in :billTypesAtomics "
                + " and b.createdAt between :fromDate and :toDate";

        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("billTypesAtomics", billTypeAtomics);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        if (institution != null) {
            jpql += " and b.institution=:ins";
            params.put("ins", institution);
        }
        if (webUser != null) {
            jpql += " and b.creater=:user";
            params.put("user", webUser);
        }
        if (department != null) {
            jpql += " and b.department=:dep";
            params.put("dep", department);
        }
        if (site != null) {
            jpql += " and b.department.site=:site";
            params.put("site", site);
        }
        if (admissionType != null) {
            jpql += " and b.patientEncounter.admissionType=:admissionType";
            params.put("admissionType", admissionType);
        }
        if (paymentScheme != null) {
            jpql += " and b.paymentScheme=:paymentScheme";
            params.put("paymentScheme", paymentScheme);
        }

        jpql += " order by b.createdAt desc";

        // Debug logging
        System.out.println("=== BillService.fetchOpdIncomeReportDTOs Query Debug ===");
        System.out.println("JPQL: " + jpql);
        System.out.println("Parameters: " + params);
        System.out.println("========================================================");

        return (List<OpdIncomeReportDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
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
        Map<String, Object> params = new HashMap<>();

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
        Map<String, Object> params = new HashMap<>();

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
// Method from 13937-all-item-movement-summary-hotfix

    public List<ItemMovementSummaryDTO> fetchItemMovementSummaryDTOs(
            Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            List<BillTypeAtomic> billTypeAtomics) {

        String jpql = "select new com.divudi.core.data.dto.ItemMovementSummaryDTO("
                + " b.billTypeAtomic, bi.item.id, bi.item.name, sum(pbi.qty + pbi.freeQty), sum(bi.netValue))"
                + " from PharmaceuticalBillItem pbi"
                + " join pbi.billItem bi"
                + " join bi.bill b"
                + " where (b.retired=false or b.retired is null)"
                + " and (bi.retired=false or bi.retired is null)"
                + " and b.billTypeAtomic in :billTypesAtomics"
                + " and b.createdAt between :fromDate and :toDate";

        Map<String, Object> params = new HashMap<>();
        params.put("billTypesAtomics", billTypeAtomics);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        if (institution != null) {
            jpql += " and b.institution=:ins";
            params.put("ins", institution);
        }
        if (department != null) {
            jpql += " and b.department=:dep";
            params.put("dep", department);
        }
        if (site != null) {
            jpql += " and b.department.site=:site";
            params.put("site", site);
        }

        jpql += " group by b.billTypeAtomic, bi.item.id, bi.item.name"
                + " order by b.billTypeAtomic, bi.item.name";

        return pharmaceuticalBillItemFacade.findItemMovementSummaryDTOs(jpql, params, TemporalType.TIMESTAMP);
    }

// Method from development branch
    public List<PharmacyIncomeBillItemDTO> fetchPharmacyIncomeBillItemDTOs(Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            WebUser webUser,
            List<BillTypeAtomic> billTypeAtomics,
            AdmissionType admissionType,
            PaymentScheme paymentScheme) {

        String jpql;
        Map<String, Object> params = new HashMap<>();

        jpql = "select new com.divudi.core.data.dto.PharmacyIncomeBillItemDTO( "
                + " b.id, "
                + " bi.id, "
                + " b.deptId, "
                + " coalesce(pers.name, 'N/A'), "
                + " b.billTypeAtomic, "
                + " b.createdAt, "
                + " coalesce(b.netTotal, 0.0), "
                + " b.paymentMethod, "
                + " coalesce(b.total, 0.0), "
                + " pe, "
                + " coalesce(pbi.qty, 0.0), "
                + " coalesce(pbi.retailRate, 0.0), "
                + " coalesce(pbi.purchaseRate, 0.0), "
                + " coalesce(bi.netRate, 0.0), "
                + " coalesce(it.name, 'N/A') ) "
                + " from PharmaceuticalBillItem pbi "
                + " join pbi.billItem bi "
                + " join bi.bill b "
                + " left join bi.item it "
                + " left join b.patientEncounter pe "
                + " left join b.patient pat "
                + " left join pat.person pers "
                + " where (b.retired = false or b.retired is null) "
                + " and (bi.retired = false or bi.retired is null) "
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

        jpql += " order by b.createdAt desc";

        return (List<PharmacyIncomeBillItemDTO>) billItemFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public List<PharmacyIncomeBillItemDTO> fetchPharmacyIncomeBillItemWithCostRateDTOs(Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            WebUser webUser,
            List<BillTypeAtomic> billTypeAtomics,
            AdmissionType admissionType,
            PaymentScheme paymentScheme) {

        String jpql;
        Map<String, Object> params = new HashMap<>();

        jpql = "select new com.divudi.core.data.dto.PharmacyIncomeBillItemDTO( "
                + " b.id, "
                + " bi.id, "
                + " b.deptId, "
                + " coalesce(pers.name, 'N/A'), "
                + " b.billTypeAtomic, "
                + " b.createdAt, "
                + " coalesce(b.netTotal, 0.0), "
                + " b.paymentMethod, "
                + " coalesce(b.total, 0.0), "
                + " pe, "
                + " coalesce(pbi.qty, 0.0), "
                + " coalesce(pbi.retailRate, 0.0), "
                + " coalesce(pbi.purchaseRate, 0.0), "
                + " coalesce(pbi.itemBatch.costRate, 0.0), "
                + " coalesce(bi.netRate, 0.0), "
                + " coalesce(it.name, 'N/A') ) "
                + " from PharmaceuticalBillItem pbi "
                + " join pbi.billItem bi "
                + " join bi.bill b "
                + " left join bi.item it "
                + " left join b.patientEncounter pe "
                + " left join b.patient pat "
                + " left join pat.person pers "
                + " where (b.retired = false or b.retired is null) "
                + " and (bi.retired = false or bi.retired is null) "
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

        jpql += " order by b.createdAt desc";

        return (List<PharmacyIncomeBillItemDTO>) billItemFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public List<OpdSaleSummaryDTO> fetchOpdSaleSummaryDTOs(Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department,
            Category category,
            Item item) {

        List<BillTypeAtomic> billTypeAtomics = BillTypeAtomic.findByServiceType(ServiceType.OPD);

        // Updated to use new constructor with IDs for navigation support
        String jpql = "select new com.divudi.core.data.dto.OpdSaleSummaryDTO("
                + " bi.item.category.id,"  // Category ID for navigation
                + " coalesce(bi.item.category.name, 'No Category'),"  // Category name for display
                + " bi.item.id,"  // Item ID for navigation
                + " coalesce(bi.item.name, 'No Item'),"  // Item name for display
                + " sum(case when b.billClassType in (:cancel, :refund) then -1 else 1 end),"  // Count
                + " sum(case when b.billClassType in (:cancel, :refund) then -bi.hospitalFee else bi.hospitalFee end),"
                + " sum(case when b.billClassType in (:cancel, :refund) then -bi.staffFee else bi.staffFee end),"
                + " sum(case when b.billClassType in (:cancel, :refund) then -bi.grossValue else bi.grossValue end),"
                + " sum(case when b.billClassType in (:cancel, :refund) then -bi.discount else bi.discount end),"
                + " sum(case when b.billClassType in (:cancel, :refund) then -bi.netValue else bi.netValue end)"
                + ") "
                + " from BillItem bi join bi.bill b "
                + " where b.retired=:ret "
                + " and b.billTypeAtomic in :bts "
                + " and b.createdAt between :fd and :td ";

        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("bts", billTypeAtomics);
        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("cancel", BillClassType.CancelledBill);
        params.put("refund", BillClassType.RefundBill);

        if (institution != null) {
            jpql += " and b.department.institution=:ins";
            params.put("ins", institution);
        }
        if (department != null) {
            jpql += " and b.department=:dep";
            params.put("dep", department);
        }
        if (site != null) {
            jpql += " and b.department.site=:site";
            params.put("site", site);
        }
        if (category != null) {
            jpql += " and bi.item.category=:cat";
            params.put("cat", category);
        }
        if (item != null) {
            jpql += " and bi.item=:itm";
            params.put("itm", item);
        }

        // Group by IDs and names for proper aggregation with navigation support
        jpql += " group by bi.item.category.id, bi.item.category.name, bi.item.id, bi.item.name"
                + " order by bi.item.category.name, bi.item.name";

        return (List<OpdSaleSummaryDTO>) billItemFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
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
            PaymentScheme paymentScheme,
            PaymentMethod paymentMethod) {
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

        // Payment Method
        if (paymentMethod != null) {
            jpql += " and b.paymentMethod=:paymentMethod ";
            params.put("paymentMethod", paymentMethod);
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
        Map<String, Object> params = new HashMap<>();

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
            DepartmentType departmentType,
            String visitType
    ) {
        String jpql;
        Map<String, Object> params = new HashMap<>();

        jpql = "select b "
                + " from Bill b "
                + " where b.retired=:ret "
                + " and b.billTypeAtomic in :billTypesAtomics "
                + " and b.createdAt between :fromDate and :toDate ";

        jpql += " and b.toDepartment.departmentType=:deptType ";
        params.put("deptType", DepartmentType.Lab);

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
        Map<String, Object> params = new HashMap<>();

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
        List<BillItem> fetchedBillItems = billItemFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
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
        Map<String, Object> params = new HashMap<>();

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
        List<BillItem> fetchedBillItems = billItemFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        return fetchedBillItems;
    }

    public List<LabDailySummaryDTO> fetchLabDailySummaryDtos(Date fromDate,
            Date toDate,
            Institution institution,
            Institution site,
            Department department) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate and toDate cannot be null");
        }
        if (fromDate.after(toDate)) {
            throw new IllegalArgumentException("fromDate cannot be after toDate");
        }
        String jpql = "select new com.divudi.core.data.dto.LabDailySummaryDTO("
                + " concat('', b.paymentMethod),"
                + " sum(case when b.paymentMethod = :cashMethod then bi.netValue else 0 end),"
                + " sum(case when b.paymentMethod = :cardMethod then bi.netValue else 0 end),"
                + " sum(case when b.paymentMethod = :onlineMethod then bi.netValue else 0 end),"
                + " sum(case when b.paymentMethod = :creditMethod then bi.netValue else 0 end),"
                + " sum(case when b.patientEncounter is not null and b.paymentMethod = :creditMethod then bi.netValue else 0 end),"
                + " sum(case when b.paymentMethod not in (:cashMethod, :cardMethod, :onlineMethod, :creditMethod) then bi.netValue else 0 end),"
                + " sum(bi.netValue),"
                + " sum(bi.discount),"
                + " sum(bi.marginValue))"
                + " from BillItem bi"
                + " join bi.bill b"
                + " join bi.item i"
                + " where (b.retired=false or b.retired is null)"
                + " and (bi.retired=false or bi.retired is null)"
                + " and type(i) = com.divudi.core.entity.lab.Investigation"
                + " and b.createdAt between :fromDate and :toDate";

        Map<String, Object> params = new HashMap<>();
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);
        params.put("cashMethod", PaymentMethod.Cash);
        params.put("cardMethod", PaymentMethod.Card);
        params.put("onlineMethod", PaymentMethod.OnlineSettlement);
        params.put("creditMethod", PaymentMethod.Credit);

        if (institution != null) {
            jpql += " and b.institution=:ins";
            params.put("ins", institution);
        }
        if (department != null) {
            jpql += " and b.department=:dep";
            params.put("dep", department);
        }
        if (site != null) {
            jpql += " and b.department.site=:site";
            params.put("site", site);
        }

        jpql += " group by b.paymentMethod";

        return (List<LabDailySummaryDTO>) billItemFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
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
        //TODO: Use a List of Bill Type Atomics instead of calling the findBy methods
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
        Map<String, Object> params = new HashMap<>();
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
        Map<String, Object> params = new HashMap<>();
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
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("message", "No bill selected");
            return new GsonBuilder().setPrettyPrinting().create().toJson(res);
        }
        if (bill.getBillTypeAtomic() == null) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("message", "Bill type not specified");
            return new GsonBuilder().setPrettyPrinting().create().toJson(res);
        }

        return convertGenericBillToJson(bill);
    }

    private String convertGenericBillToJson(Bill bill) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Map<String, Object> root = new LinkedHashMap<>();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("billTypeAtomic", bill.getBillTypeAtomic().toString());
        summary.put("billType", bill.getBillType());
        summary.put("deptId", bill.getDeptId());
        summary.put("invoiceNo", bill.getInvoiceNumber());
        summary.put("createdAt", bill.getCreatedAt() != null ? dateFormat.format(bill.getCreatedAt()) : null);
        root.put("summary", summary);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("department_id", bill.getDepartment() != null ? bill.getDepartment().getId() : null);
        details.put("from_institution_id", bill.getFromInstitution() != null ? bill.getFromInstitution().getId() : null);
        details.put("to_institution_id", bill.getToInstitution() != null ? bill.getToInstitution().getId() : null);
        details.put("paymentMethod", bill.getPaymentMethod());
        details.put("saleValue", bill.getSaleValue());
        details.put("total", bill.getTotal());
        details.put("tax", bill.getTax());
        details.put("discount", bill.getDiscount());
        details.put("netTotal", bill.getNetTotal());
        root.put("billDetails", details);

        List<Map<String, Object>> billItemsList = new ArrayList<>();
        if (bill.getBillItems() != null) {
            for (BillItem bi : bill.getBillItems()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("item_id", bi.getItem() != null ? bi.getItem().getId() : null);
                item.put("qty", bi.getQty());
                if (bi.getPharmaceuticalBillItem() != null) {
                    item.put("expiry", bi.getPharmaceuticalBillItem().getDoe() != null ? dateFormat.format(bi.getPharmaceuticalBillItem().getDoe()) : null);
                    item.put("batchNo", bi.getPharmaceuticalBillItem().getStringValue());
                    item.put("receivedQty", bi.getPharmaceuticalBillItem().getQty());
                    item.put("receivedFreeQty", bi.getPharmaceuticalBillItem().getFreeQty());
                }

                Map<String, Object> fin = new LinkedHashMap<>();
                if (bi.getBillItemFinanceDetails() != null) {
                    BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
                    fin.put("netTotal", fd.getNetTotal());
                    fin.put("grossTotal", fd.getGrossTotal());
                    fin.put("discount", fd.getTotalDiscount());
                    fin.put("tax", fd.getTotalTax());
                } else {
                    fin.put("netTotal", bi.getNetValue());
                    fin.put("grossTotal", bi.getGrossValue());
                    fin.put("discount", bi.getDiscount());
                    fin.put("tax", bi.getVat());
                }
                item.put("finance", fin);
                billItemsList.add(item);
            }
        }
        root.put("billItems", billItemsList);

        List<Map<String, Object>> paymentsList = new ArrayList<>();
        if (bill.getPayments() != null) {
            for (Payment p : bill.getPayments()) {
                Map<String, Object> pm = new LinkedHashMap<>();
                pm.put("paymentMethod", p.getPaymentMethod());
                pm.put("paidValue", p.getPaidValue());
                pm.put("createdAt", p.getCreatedAt() != null ? dateFormat.format(p.getCreatedAt()) : null);
                pm.put("bank_id", p.getBank() != null ? p.getBank().getId() : null);
                pm.put("chequeRefNo", p.getChequeRefNo());
                pm.put("creditCardRefNo", p.getCreditCardRefNo());
                paymentsList.add(pm);
            }
        }
        root.put("payments", paymentsList);

        List<Map<String, Object>> cancellationsList = new ArrayList<>();
        if (bill.getCancelledBill() != null) {
            Map<String, Object> c = new LinkedHashMap<>();
            Bill cb = bill.getCancelledBill();
            c.put("billTypeAtomic", cb.getBillTypeAtomic() != null ? cb.getBillTypeAtomic().toString() : null);
            c.put("deptId", cb.getDeptId());
            c.put("createdAt", cb.getCreatedAt() != null ? dateFormat.format(cb.getCreatedAt()) : null);
            c.put("invoiceNo", cb.getInvoiceNumber());
            cancellationsList.add(c);
        }
        root.put("cancellations", cancellationsList);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(root);
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
                if (billNetTotalIsNotEqualToBillItemNetTotalError) {
                    hasAtLeatOneError = true;
                }
                break;
            case CC_BILL:
            case CC_BILL_CANCELLATION:
            case CC_BILL_REFUND:
            case CC_PAYMENT_CANCELLATION_BILL:
            case CC_PAYMENT_RECEIVED_BILL:
            case CC_PAYMENT_MADE_BILL:
            case CC_PAYMENT_MADE_CANCELLATION_BILL:
                boolean noItemsError = billHasNoBillItems(bill);
                boolean netTotalError = billNetTotalIsNotEqualToBillItemNetTotal(bill);
                boolean hospitalFeeError = billHospitalFeeTotalIsNotEqualToBillItemHospitalFeeTotal(bill);
                boolean centreFeeError = billCollectingCentreFeeTotalIsNotEqualToBillItemCollectingCentreFeeTotal(bill);

                if (noItemsError || netTotalError || hospitalFeeError || centreFeeError) {
                    hasAtLeatOneError = true;
                }
                break;
            case CC_CREDIT_NOTE:
            case CC_CREDIT_NOTE_CANCELLATION:
            case CC_DEBIT_NOTE:
            case CC_DEBIT_NOTE_CANCELLATION:

            default:
                hasAtLeatOneError = false;

        }
        if (hasAtLeatOneError) {
        }
        return hasAtLeatOneError;
    }

    // ChatGPT contributed method to validate bill item net total consistency
    public boolean billNetTotalIsNotEqualToBillItemNetTotal(Bill bill) {
        if (bill == null || bill.getBillItems() == null) {
            return true;
        }

        double billNetTotal = Math.abs(bill.getNetTotal());
        double billItemNetTotal = bill.getBillItems().stream()
                .filter(bi -> bi != null && !bi.isRetired())
                .mapToDouble(bi -> Math.abs(bi.getNetValue()))
                .sum();

        boolean mismatch = Math.abs(billNetTotal - billItemNetTotal) >= 0.01;

        if (mismatch) {
            bill.setTmpComments((bill.getTmpComments() == null ? "" : bill.getTmpComments())
                    + "Bill net total (" + billNetTotal + ") does not match sum of bill item net totals (" + billItemNetTotal + "). ");
        }

        return mismatch;
    }

    // ChatGPT contributed method to validate collecting centre fee totals
    public boolean billCollectingCentreFeeTotalIsNotEqualToBillItemCollectingCentreFeeTotal(Bill bill) {
        if (bill == null || bill.getBillItems() == null) {
            return true;
        }

        double billCcTotal = Math.abs(bill.getTotalCenterFee());
        double billItemCcTotal = bill.getBillItems().stream()
                .filter(bi -> bi != null && !bi.isRetired())
                .mapToDouble(bi -> Math.abs(bi.getCollectingCentreFee()))
                .sum();

        boolean mismatch = Math.abs(billCcTotal - billItemCcTotal) >= 0.01;

        if (mismatch) {
            bill.setTmpComments((bill.getTmpComments() == null ? "" : bill.getTmpComments())
                    + "Bill collecting centre fee total (" + billCcTotal + ") does not match sum of bill item collecting centre fees (" + billItemCcTotal + "). ");
        }

        return mismatch;
    }

    // ChatGPT contributed method to validate hospital fee totals
    public boolean billHospitalFeeTotalIsNotEqualToBillItemHospitalFeeTotal(Bill bill) {
        if (bill == null || bill.getBillItems() == null) {
            return true;
        }

        double billHospitalTotal = Math.abs(bill.getTotalHospitalFee());
        double billItemHospitalTotal = bill.getBillItems().stream()
                .filter(bi -> bi != null && !bi.isRetired())
                .mapToDouble(bi -> Math.abs(bi.getHospitalFee()))
                .sum();

        boolean mismatch = Math.abs(billHospitalTotal - billItemHospitalTotal) >= 0.01;

        if (mismatch) {
            bill.setTmpComments((bill.getTmpComments() == null ? "" : bill.getTmpComments())
                    + "Bill hospital fee total (" + billHospitalTotal + ") does not match sum of bill item hospital fees (" + billItemHospitalTotal + "). ");
        }

        return mismatch;
    }

    // ChatGPT contributed method to check if bill has no bill items
    public boolean billHasNoBillItems(Bill bill) {
        if (bill == null || bill.getBillItems() == null || bill.getBillItems().isEmpty()) {
            bill.setTmpComments((bill.getTmpComments() == null ? "" : bill.getTmpComments())
                    + "This bill has no bill items. ");
            return true;
        }
        return false;
    }

    public List<PatientInvestigation> fetchPatientInvestigations(Bill bill) {
        String jpql;
        HashMap<String, Object> params = new HashMap<>();
        jpql = "SELECT pbi "
                + " FROM PatientInvestigation pbi "
                + " WHERE pbi.billItem.bill=:bl "
                + " order by pbi.id";
        params.put("bl", bill);
        List<PatientInvestigation> ptix = patientInvestigationFacade.findByJpql(jpql, params);
        return ptix;
    }

    public List<PatientInvestigation> fetchPatientInvestigationsOfBatchBill(Bill batchBill) {
        if (batchBill == null) {
            return new ArrayList<>();
        }
        String jpql = "SELECT pbi "
                + "FROM PatientInvestigation pbi "
                + "WHERE pbi.billItem.bill IN ("
                + "  SELECT b FROM Bill b WHERE b.backwardReferenceBill = :bb"
                + ") "
                + "ORDER BY pbi.id";
        Map<String, Object> params = new HashMap<>();
        params.put("bb", batchBill);
        return patientInvestigationFacade.findByJpql(jpql, params);
    }

    public List<BillItem> checkCreditBillPaymentReciveFromCreditCompany(Bill bill) {
        List<BillItem> billItems = new ArrayList<>();

        if (bill == null) {
            return billItems;
        }

        Map<String, Object> params = new HashMap<>();
        String jpql = "select bi "
                + " from BillItem bi"
                + " where bi.retired=:ret "
                + " and bi.referenceBill =:b "
                + " and bi.bill.cancelled =:can "
                + " and bi.bill.billTypeAtomic =:bta ";
        params.put("ret", false);
        params.put("can", false);
        params.put("bta", BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_RECEIVED);
        params.put("b", bill);

        billItems = billItemFacade.findByJpql(jpql, params);
        return billItems;

    }

    public List<DenominationTransaction> fetchDenominationTransactionFromBill(Bill b) {
        String jpql = "select dt "
                + " from DenominationTransaction dt "
                + " where dt.retired=:ret "
                + " and dt.bill=:b";
        Map m = new HashMap();
        m.put("b", b);
        m.put("ret", false);
        return denominationTransactionFacade.findByJpql(jpql, m);
    }

    public void createBillFinancialDetailsForPharmacyBill(Bill b) {

        if (b == null) {
            return;
        }

        BillTypeAtomic bta = Optional
                .ofNullable(b)
                .map(Bill::getBillTypeAtomic)
                .orElse(null);
        if (bta == null || bta.getBillCategory() == null) {
            return; // unable to categorise safely
        }
        BillCategory bc = bta.getBillCategory();

        Double saleValue = 0.0;
        Double purchaseValue = 0.0;

        List<BillItem> billItems = new ArrayList<>();
        billItems = fetchBillItems(b);

        for (BillItem bi : billItems) {

            if (bi == null || bi.getPharmaceuticalBillItem() == null) {
                continue;
            }

            Double q = bi.getPharmaceuticalBillItem().getQty();
            Double rRate = bi.getPharmaceuticalBillItem().getRetailRate();
            if (bta == BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS) {
                rRate = bi.getNetRate();
            }

            Double pRate = bi.getPharmaceuticalBillItem().getPurchaseRate();

            // Fix for transfer receive bills: Use preserved rates from pack values
            if (bta == BillTypeAtomic.PHARMACY_RECEIVE) {
                // Use pack rates which preserve original transfer issue rates, fall back to unit rates if not available
                if (bi.getPharmaceuticalBillItem().getPurchaseRatePack() > 0) {
                    pRate = bi.getPharmaceuticalBillItem().getPurchaseRatePack();
                }
                if (bi.getPharmaceuticalBillItem().getRetailRatePack() > 0) {
                    rRate = bi.getPharmaceuticalBillItem().getRetailRatePack();
                }
            }

            if (q == null || rRate == null || pRate == null) {
                continue;
            }

            double qty = Math.abs(q);
            double retail = Math.abs(rRate);
            double purchase = Math.abs(pRate);

            double retailTotal = 0;
            double purchaseTotal = 0;

            switch (bc) {
                case BILL:
                case PAYMENTS:
                case PREBILL:
                    retailTotal = retail * qty;
                    purchaseTotal = purchase * qty;
                    break;

                case CANCELLATION:
                case REFUND:
                    retailTotal = -retail * qty;
                    purchaseTotal = -purchase * qty;
                    break;

                default:
                    break;
            }
            saleValue += retailTotal;
            purchaseValue += purchaseTotal;
        }
        if (b.getBillFinanceDetails() == null) {
            b.setBillFinanceDetails(new BillFinanceDetails());
        }

        b.getBillFinanceDetails().setTotalRetailSaleValue(BigDecimal.valueOf(saleValue));
        b.getBillFinanceDetails().setTotalPurchaseValue(BigDecimal.valueOf(purchaseValue));
        billFacade.editAndCommit(b);
    }

    public void createBillFinancialDetailsForPharmacyDirectIssueBill(Bill b, List<BillItem> billItems) {
        if (b == null) {
            return;
        }
        Double billValueAtRetailRate = 0.0;
        Double billValueAtPurchaseRate = 0.0;
        Double billValueAtCostRate = 0.0;
        for (BillItem bi : billItems) {
            if (bi == null || bi.getPharmaceuticalBillItem() == null) {
                continue;
            }
            Double quentityInUnits = bi.getPharmaceuticalBillItem().getQty();
            Double retailRatePerUnit = bi.getPharmaceuticalBillItem().getItemBatch().getRetailsaleRate();
            Double purchaseRatePerUnit = bi.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate();
            Double costRatePerUnit = bi.getPharmaceuticalBillItem().getItemBatch().getCostRate();


            double billItemRetailValue = 0;
            double billItemPurchaseValue = 0;
            double billItemCostValue = 0;

            billItemRetailValue = retailRatePerUnit * quentityInUnits;
            billItemPurchaseValue = purchaseRatePerUnit * quentityInUnits;
            billItemCostValue = costRatePerUnit * quentityInUnits;

            billValueAtRetailRate += billItemRetailValue;
            billValueAtPurchaseRate += billItemPurchaseValue;
            billValueAtCostRate += billItemCostValue;
        }
        b.getBillFinanceDetails().setTotalRetailSaleValue(BigDecimal.valueOf(billValueAtRetailRate));
        b.getBillFinanceDetails().setTotalPurchaseValue(BigDecimal.valueOf(billValueAtPurchaseRate));
        b.getBillFinanceDetails().setTotalCostValue(BigDecimal.valueOf(billValueAtCostRate));
        billFacade.editAndCommit(b);
    }

}
