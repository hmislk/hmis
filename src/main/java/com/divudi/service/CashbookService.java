package com.divudi.service;

import com.divudi.core.data.CashBookEntryData;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.cashTransaction.CashBook;
import com.divudi.core.entity.cashTransaction.CashBookEntry;
import com.divudi.core.facade.CashBookEntryFacade;
import com.divudi.core.facade.CashBookFacade;
import java.util.Date;
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
public class CashbookService {

    @EJB
    CashBookEntryFacade cashbookEntryFacade;
    @EJB
    CashBookFacade cashbookFacade;


    /**
     * Records funds leaving (negative value) or entering (positive value) a cash
     * book outside a handover: bank/safe deposits, withdrawals and non-cash
     * settlements. Fills the per-method value, the from/to department,
     * institution and site and the running balances the same way the handover
     * entry does, so the cash book summaries include it. Collections themselves
     * reach the cash book only at handover acceptance, never at billing.
     */
    public CashBookEntry writeFundMovementEntry(Payment p, double signedValue, CashBook cashbook, Bill bill, WebUser user) {
        if (p == null || p.getPaymentMethod() == null || cashbook == null || cashbook.getDepartment() == null) {
            return null;
        }
        // Lock the cash book so concurrent entries read each other's running balances in order
        cashbook = cashbookFacade.findWithLock(cashbook.getId());
        Department dept = cashbook.getDepartment();

        CashBookEntryData entryData = new CashBookEntryData(dept, dept);
        addValueForPaymentMethod(entryData, p.getPaymentMethod(), signedValue);
        entryData.setTotal(signedValue);

        Date now = new Date();
        CashBookEntry cbe = new CashBookEntry();
        cbe.setInstitution(dept.getInstitution());
        cbe.setDepartment(dept);
        cbe.setSite(dept.getSite());
        cbe.setFromDepartment(dept);
        cbe.setToDepartment(dept);
        cbe.setFromInstitution(dept.getInstitution());
        cbe.setToInstitution(dept.getInstitution());
        cbe.setFromSite(dept.getSite());
        cbe.setToSite(dept.getSite());
        cbe.setCashBook(cashbook);
        cbe.setBill(bill);
        cbe.setPayment(p);
        cbe.setPaymentMethod(p.getPaymentMethod());
        cbe.setEntryValue(signedValue);
        cbe.setCashbookDate(now);
        cbe.setWebUser(user);
        cbe.setCreater(user);
        cbe.setCreatedAt(now);

        cbe.setCashValue(entryData.getCashValue());
        cbe.setCardValue(entryData.getCardValue());
        cbe.setAgentValue(entryData.getAgentValue());
        cbe.setChequeValue(entryData.getChequeValue());
        cbe.setCreditValue(entryData.getCreditValue());
        cbe.setIouValue(entryData.getIouValue());
        cbe.setMultiplePaymentMethodsValue(entryData.getMultiplePaymentMethodsValue());
        cbe.setOnCallValue(entryData.getOnCallValue());
        cbe.setOnlineSettlementValue(entryData.getOnlineSettlementValue());
        cbe.setPatientDepositValue(entryData.getPatientDepositValue());
        cbe.setPatientPointsValue(entryData.getPatientPointsValue());
        cbe.setSlipValue(entryData.getSlipValue());
        cbe.setStaffValue(entryData.getStaffValue());
        cbe.setStaffWelfareValue(entryData.getStaffWelfareValue());
        cbe.setVoucherValue(entryData.getVoucherValue());
        cbe.setEwalletValue(entryData.getEwalletValue());

        updateCashbookEntryBalances(entryData, cbe);
        cashbookEntryFacade.create(cbe);
        return cbe;
    }

    /**
     * Reverses every fund-movement entry written for the given bill, e.g. when a
     * non-cash settlement is cancelled. Bills that never wrote an entry (settled
     * before the cash book was updated at settlement) reverse nothing.
     */
    public void reverseFundMovementEntries(Bill originalBill, Bill reversalBill, WebUser user) {
        if (originalBill == null) {
            return;
        }
        String jpql = "select e from CashBookEntry e "
                + " where e.retired = :ret "
                + " and e.bill = :bill "
                + " order by e.id";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("bill", originalBill);
        List<CashBookEntry> entries = cashbookEntryFacade.findByJpql(jpql, m);
        for (CashBookEntry e : entries) {
            writeFundMovementEntry(e.getPayment(), -e.getEntryValue(), e.getCashBook(), reversalBill, user);
        }
    }

    public CashBook findAndSaveCashBookForDepartment(Department dept) {
        if (dept == null) {
            return null;
        }
        return findAndSaveCashBookBySite(dept.getSite(), dept.getInstitution(), dept);
    }

    private void addValueForPaymentMethod(CashBookEntryData entryData, PaymentMethod pm, double value) {
        switch (pm) {
            case Cash:
                entryData.setCashValue(entryData.getCashValue() + value);
                break;
            case Card:
                entryData.setCardValue(entryData.getCardValue() + value);
                break;
            case Agent:
                entryData.setAgentValue(entryData.getAgentValue() + value);
                break;
            case Cheque:
                entryData.setChequeValue(entryData.getChequeValue() + value);
                break;
            case Credit:
                entryData.setCreditValue(entryData.getCreditValue() + value);
                break;
            case IOU:
                entryData.setIouValue(entryData.getIouValue() + value);
                break;
            case MultiplePaymentMethods:
                entryData.setMultiplePaymentMethodsValue(entryData.getMultiplePaymentMethodsValue() + value);
                break;
            case OnCall:
                entryData.setOnCallValue(entryData.getOnCallValue() + value);
                break;
            case OnlineSettlement:
                entryData.setOnlineSettlementValue(entryData.getOnlineSettlementValue() + value);
                break;
            case PatientDeposit:
                entryData.setPatientDepositValue(entryData.getPatientDepositValue() + value);
                break;
            case PatientPoints:
                entryData.setPatientPointsValue(entryData.getPatientPointsValue() + value);
                break;
            case Slip:
                entryData.setSlipValue(entryData.getSlipValue() + value);
                break;
            case Staff:
                entryData.setStaffValue(entryData.getStaffValue() + value);
                break;
            case Staff_Welfare:
                entryData.setStaffWelfareValue(entryData.getStaffWelfareValue() + value);
                break;
            case Voucher:
                entryData.setVoucherValue(entryData.getVoucherValue() + value);
                break;
            case ewallet:
                entryData.setEwalletValue(entryData.getEwalletValue() + value);
                break;
            default:
                break;
        }
    }

    public CashBook findAndSaveCashBookBySite(Institution site, Institution ins, Department dept) {
        if (site==null) {
            return null;
        }

        if (ins==null) {
            return null;
        }

        if (dept==null) {
            return null;
        }

        String sql;
        Map m = new HashMap();
        m.put("ins", ins);
        m.put("dept", dept);
        m.put("site", site);
        m.put("ret", false);
        sql = "select cb "
                + " from CashBook cb "
                + " where cb.institution=:ins"
                + " and cb.department=:dept"
                + " and cb.site=:site"
                + " and cb.retired=:ret";
        CashBook cb = cashbookFacade.findFirstByJpql(sql, m);

        if (cb == null) {
            cb = new CashBook();
            cb.setInstitution(ins);
            cb.setDepartment(dept);
            cb.setSite(site);
            cashbookFacade.create(cb);
        }
        return cb;
    }

    public void updateCashbookEntryBalances(CashBookEntryData entryData, CashBookEntry cbe) {
        CashBookEntry lastFromDepartmentEntry = fetchLastCashbookEntryForFromDepartment(entryData.getFromDepartment());
        CashBookEntry lastToDepartmentEntry = fetchLastCashbookEntryForToDepartment(entryData.getToDepartment());
        CashBookEntry lastFromInstitutionEntry = fetchLastCashbookEntryForFromInstitution(entryData.getFromDepartment().getInstitution());
        CashBookEntry lastToInstitutionEntry = fetchLastCashbookEntryForToInstitution(entryData.getToDepartment().getInstitution());
        CashBookEntry lastFromSiteEntry = fetchLastCashbookEntryForFromSite(entryData.getFromDepartment().getSite());
        CashBookEntry lastToSiteEntry = fetchLastCashbookEntryForToSite(entryData.getToDepartment().getSite());

        //ALL
        cbe.setFromDepartmentBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentBalanceAfter() : 0.0);
        cbe.setToDepartmentBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentBalanceAfter() : 0.0);
        cbe.setFromInstitutionBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionBalanceAfter() : 0.0);
        cbe.setToInstitutionBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionBalanceAfter() : 0.0);
        cbe.setFromSiteBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteBalanceAfter() : 0.0);
        cbe.setToSiteBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteBalanceAfter() : 0.0);

        cbe.setFromDepartmentBalanceAfter(cbe.getFromDepartmentBalanceBefore() + entryData.getTotal());
        cbe.setToDepartmentBalanceAfter(cbe.getToDepartmentBalanceBefore() + entryData.getTotal());
        cbe.setFromInstitutionBalanceAfter(cbe.getFromInstitutionBalanceBefore() + entryData.getTotal());
        cbe.setToInstitutionBalanceAfter(cbe.getToInstitutionBalanceBefore() + entryData.getTotal());
        cbe.setFromSiteBalanceAfter(cbe.getFromSiteBalanceBefore() + entryData.getTotal());
        cbe.setToSiteBalanceAfter(cbe.getToSiteBalanceBefore() + entryData.getTotal());

        //CASH
        cbe.setFromDepartmentCashBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentCashBalanceAfter() : 0.0);
        cbe.setToDepartmentCashBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentCashBalanceAfter() : 0.0);
        cbe.setFromInstitutionCashBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionCashBalanceAfter() : 0.0);
        cbe.setToInstitutionCashBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionCashBalanceAfter() : 0.0);
        cbe.setFromSiteCashBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteCashBalanceAfter() : 0.0);
        cbe.setToSiteCashBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteCashBalanceAfter() : 0.0);

        cbe.setFromDepartmentCashBalanceAfter(cbe.getFromDepartmentCashBalanceBefore() + entryData.getCashValue());
        cbe.setToDepartmentCashBalanceAfter(cbe.getToDepartmentCashBalanceBefore() + entryData.getCashValue());
        cbe.setFromInstitutionCashBalanceAfter(cbe.getFromInstitutionCashBalanceBefore() + entryData.getCashValue());
        cbe.setToInstitutionCashBalanceAfter(cbe.getToInstitutionCashBalanceBefore() + entryData.getCashValue());
        cbe.setFromSiteCashBalanceAfter(cbe.getFromSiteCashBalanceBefore() + entryData.getCashValue());
        cbe.setToSiteCashBalanceAfter(cbe.getToSiteCashBalanceBefore() + entryData.getCashValue());

        //CARD
        cbe.setFromDepartmentCardBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentCardBalanceAfter() : 0.0);
        cbe.setToDepartmentCardBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentCardBalanceAfter() : 0.0);
        cbe.setFromInstitutionCardBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionCardBalanceAfter() : 0.0);
        cbe.setToInstitutionCardBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionCardBalanceAfter() : 0.0);
        cbe.setFromSiteCardBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteCardBalanceAfter() : 0.0);
        cbe.setToSiteCardBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteCardBalanceAfter() : 0.0);

        cbe.setFromDepartmentCardBalanceAfter(cbe.getFromDepartmentCardBalanceBefore() + entryData.getCardValue());
        cbe.setToDepartmentCardBalanceAfter(cbe.getToDepartmentCardBalanceBefore() + entryData.getCardValue());
        cbe.setFromInstitutionCardBalanceAfter(cbe.getFromInstitutionCardBalanceBefore() + entryData.getCardValue());
        cbe.setToInstitutionCardBalanceAfter(cbe.getToInstitutionCardBalanceBefore() + entryData.getCardValue());
        cbe.setFromSiteCardBalanceAfter(cbe.getFromSiteCardBalanceBefore() + entryData.getCardValue());
        cbe.setToSiteCardBalanceAfter(cbe.getToSiteCardBalanceBefore() + entryData.getCardValue());

        //AGENT
        cbe.setFromDepartmentAgentBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentAgentBalanceAfter() : 0.0);
        cbe.setToDepartmentAgentBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentAgentBalanceAfter() : 0.0);
        cbe.setFromInstitutionAgentBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionAgentBalanceAfter() : 0.0);
        cbe.setToInstitutionAgentBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionAgentBalanceAfter() : 0.0);
        cbe.setFromSiteAgentBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteAgentBalanceAfter() : 0.0);
        cbe.setToSiteAgentBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteAgentBalanceAfter() : 0.0);

        cbe.setFromDepartmentAgentBalanceAfter(cbe.getFromDepartmentAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setToDepartmentAgentBalanceAfter(cbe.getToDepartmentAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setFromInstitutionAgentBalanceAfter(cbe.getFromInstitutionAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setToInstitutionAgentBalanceAfter(cbe.getToInstitutionAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setFromSiteAgentBalanceAfter(cbe.getFromSiteAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setToSiteAgentBalanceAfter(cbe.getToSiteAgentBalanceBefore() + entryData.getAgentValue());

        //CHEQUE
        cbe.setFromDepartmentChequeBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentChequeBalanceAfter() : 0.0);
        cbe.setToDepartmentChequeBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentChequeBalanceAfter() : 0.0);
        cbe.setFromInstitutionChequeBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionChequeBalanceAfter() : 0.0);
        cbe.setToInstitutionChequeBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionChequeBalanceAfter() : 0.0);
        cbe.setFromSiteChequeBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteChequeBalanceAfter() : 0.0);
        cbe.setToSiteChequeBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteChequeBalanceAfter() : 0.0);

        cbe.setFromDepartmentChequeBalanceAfter(cbe.getFromDepartmentChequeBalanceBefore() + entryData.getChequeValue());
        cbe.setToDepartmentChequeBalanceAfter(cbe.getToDepartmentChequeBalanceBefore() + entryData.getChequeValue());
        cbe.setFromInstitutionChequeBalanceAfter(cbe.getFromInstitutionChequeBalanceBefore() + entryData.getChequeValue());
        cbe.setToInstitutionChequeBalanceAfter(cbe.getToInstitutionChequeBalanceBefore() + entryData.getChequeValue());
        cbe.setFromSiteChequeBalanceAfter(cbe.getFromSiteChequeBalanceBefore() + entryData.getChequeValue());
        cbe.setToSiteChequeBalanceAfter(cbe.getToSiteChequeBalanceBefore() + entryData.getChequeValue());

// Staff
        cbe.setFromDepartmentStaffBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentStaffBalanceAfter() : 0.0);
        cbe.setToDepartmentStaffBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentStaffBalanceAfter() : 0.0);
        cbe.setFromInstitutionStaffBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionStaffBalanceAfter() : 0.0);
        cbe.setToInstitutionStaffBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionStaffBalanceAfter() : 0.0);
        cbe.setFromSiteStaffBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteStaffBalanceAfter() : 0.0);
        cbe.setToSiteStaffBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteStaffBalanceAfter() : 0.0);

        cbe.setFromDepartmentStaffBalanceAfter(cbe.getFromDepartmentStaffBalanceBefore() + entryData.getStaffValue());
        cbe.setToDepartmentStaffBalanceAfter(cbe.getToDepartmentStaffBalanceBefore() + entryData.getStaffValue());
        cbe.setFromInstitutionStaffBalanceAfter(cbe.getFromInstitutionStaffBalanceBefore() + entryData.getStaffValue());
        cbe.setToInstitutionStaffBalanceAfter(cbe.getToInstitutionStaffBalanceBefore() + entryData.getStaffValue());
        cbe.setFromSiteStaffBalanceAfter(cbe.getFromSiteStaffBalanceBefore() + entryData.getStaffValue());
        cbe.setToSiteStaffBalanceAfter(cbe.getToSiteStaffBalanceBefore() + entryData.getStaffValue());

// Credit
        cbe.setFromDepartmentCreditBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentCreditBalanceAfter() : 0.0);
        cbe.setToDepartmentCreditBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentCreditBalanceAfter() : 0.0);
        cbe.setFromInstitutionCreditBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionCreditBalanceAfter() : 0.0);
        cbe.setToInstitutionCreditBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionCreditBalanceAfter() : 0.0);
        cbe.setFromSiteCreditBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteCreditBalanceAfter() : 0.0);
        cbe.setToSiteCreditBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteCreditBalanceAfter() : 0.0);

        cbe.setFromDepartmentCreditBalanceAfter(cbe.getFromDepartmentCreditBalanceBefore() + entryData.getCreditValue());
        cbe.setToDepartmentCreditBalanceAfter(cbe.getToDepartmentCreditBalanceBefore() + entryData.getCreditValue());
        cbe.setFromInstitutionCreditBalanceAfter(cbe.getFromInstitutionCreditBalanceBefore() + entryData.getCreditValue());
        cbe.setToInstitutionCreditBalanceAfter(cbe.getToInstitutionCreditBalanceBefore() + entryData.getCreditValue());
        cbe.setFromSiteCreditBalanceAfter(cbe.getFromSiteCreditBalanceBefore() + entryData.getCreditValue());
        cbe.setToSiteCreditBalanceAfter(cbe.getToSiteCreditBalanceBefore() + entryData.getCreditValue());

// Staff_Welfare
        cbe.setFromDepartmentStaffWelfareBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentStaffWelfareBalanceAfter() : 0.0);
        cbe.setToDepartmentStaffWelfareBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentStaffWelfareBalanceAfter() : 0.0);
        cbe.setFromInstitutionStaffWelfareBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionStaffWelfareBalanceAfter() : 0.0);
        cbe.setToInstitutionStaffWelfareBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionStaffWelfareBalanceAfter() : 0.0);
        cbe.setFromSiteStaffWelfareBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteStaffWelfareBalanceAfter() : 0.0);
        cbe.setToSiteStaffWelfareBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteStaffWelfareBalanceAfter() : 0.0);

        cbe.setFromDepartmentStaffWelfareBalanceAfter(cbe.getFromDepartmentStaffWelfareBalanceBefore() + entryData.getStaffWelfareValue());
        cbe.setToDepartmentStaffWelfareBalanceAfter(cbe.getToDepartmentStaffWelfareBalanceBefore() + entryData.getStaffWelfareValue());
        cbe.setFromInstitutionStaffWelfareBalanceAfter(cbe.getFromInstitutionStaffWelfareBalanceBefore() + entryData.getStaffWelfareValue());
        cbe.setToInstitutionStaffWelfareBalanceAfter(cbe.getToInstitutionStaffWelfareBalanceBefore() + entryData.getStaffWelfareValue());
        cbe.setFromSiteStaffWelfareBalanceAfter(cbe.getFromSiteStaffWelfareBalanceBefore() + entryData.getStaffWelfareValue());
        cbe.setToSiteStaffWelfareBalanceAfter(cbe.getToSiteStaffWelfareBalanceBefore() + entryData.getStaffWelfareValue());

// Voucher
        cbe.setFromDepartmentVoucherBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentVoucherBalanceAfter() : 0.0);
        cbe.setToDepartmentVoucherBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentVoucherBalanceAfter() : 0.0);
        cbe.setFromInstitutionVoucherBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionVoucherBalanceAfter() : 0.0);
        cbe.setToInstitutionVoucherBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionVoucherBalanceAfter() : 0.0);
        cbe.setFromSiteVoucherBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteVoucherBalanceAfter() : 0.0);
        cbe.setToSiteVoucherBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteVoucherBalanceAfter() : 0.0);

        cbe.setFromDepartmentVoucherBalanceAfter(cbe.getFromDepartmentVoucherBalanceBefore() + entryData.getVoucherValue());
        cbe.setToDepartmentVoucherBalanceAfter(cbe.getToDepartmentVoucherBalanceBefore() + entryData.getVoucherValue());
        cbe.setFromInstitutionVoucherBalanceAfter(cbe.getFromInstitutionVoucherBalanceBefore() + entryData.getVoucherValue());
        cbe.setToInstitutionVoucherBalanceAfter(cbe.getToInstitutionVoucherBalanceBefore() + entryData.getVoucherValue());
        cbe.setFromSiteVoucherBalanceAfter(cbe.getFromSiteVoucherBalanceBefore() + entryData.getVoucherValue());
        cbe.setToSiteVoucherBalanceAfter(cbe.getToSiteVoucherBalanceBefore() + entryData.getVoucherValue());

// IOU
        cbe.setFromDepartmentIouBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentIouBalanceAfter() : 0.0);
        cbe.setToDepartmentIouBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentIouBalanceAfter() : 0.0);
        cbe.setFromInstitutionIouBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionIouBalanceAfter() : 0.0);
        cbe.setToInstitutionIouBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionIouBalanceAfter() : 0.0);
        cbe.setFromSiteIouBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteIouBalanceAfter() : 0.0);
        cbe.setToSiteIouBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteIouBalanceAfter() : 0.0);

        cbe.setFromDepartmentIouBalanceAfter(cbe.getFromDepartmentIouBalanceBefore() + entryData.getIouValue());
        cbe.setToDepartmentIouBalanceAfter(cbe.getToDepartmentIouBalanceBefore() + entryData.getIouValue());
        cbe.setFromInstitutionIouBalanceAfter(cbe.getFromInstitutionIouBalanceBefore() + entryData.getIouValue());
        cbe.setToInstitutionIouBalanceAfter(cbe.getToInstitutionIouBalanceBefore() + entryData.getIouValue());
        cbe.setFromSiteIouBalanceAfter(cbe.getFromSiteIouBalanceBefore() + entryData.getIouValue());
        cbe.setToSiteIouBalanceAfter(cbe.getToSiteIouBalanceBefore() + entryData.getIouValue());

// Agent
        cbe.setFromDepartmentAgentBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentAgentBalanceAfter() : 0.0);
        cbe.setToDepartmentAgentBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentAgentBalanceAfter() : 0.0);
        cbe.setFromInstitutionAgentBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionAgentBalanceAfter() : 0.0);
        cbe.setToInstitutionAgentBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionAgentBalanceAfter() : 0.0);
        cbe.setFromSiteAgentBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteAgentBalanceAfter() : 0.0);
        cbe.setToSiteAgentBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteAgentBalanceAfter() : 0.0);

        cbe.setFromDepartmentAgentBalanceAfter(cbe.getFromDepartmentAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setToDepartmentAgentBalanceAfter(cbe.getToDepartmentAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setFromInstitutionAgentBalanceAfter(cbe.getFromInstitutionAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setToInstitutionAgentBalanceAfter(cbe.getToInstitutionAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setFromSiteAgentBalanceAfter(cbe.getFromSiteAgentBalanceBefore() + entryData.getAgentValue());
        cbe.setToSiteAgentBalanceAfter(cbe.getToSiteAgentBalanceBefore() + entryData.getAgentValue());

// Slip
        cbe.setFromDepartmentSlipBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentSlipBalanceAfter() : 0.0);
        cbe.setToDepartmentSlipBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentSlipBalanceAfter() : 0.0);
        cbe.setFromInstitutionSlipBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionSlipBalanceAfter() : 0.0);
        cbe.setToInstitutionSlipBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionSlipBalanceAfter() : 0.0);
        cbe.setFromSiteSlipBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteSlipBalanceAfter() : 0.0);
        cbe.setToSiteSlipBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteSlipBalanceAfter() : 0.0);

        cbe.setFromDepartmentSlipBalanceAfter(cbe.getFromDepartmentSlipBalanceBefore() + entryData.getSlipValue());
        cbe.setToDepartmentSlipBalanceAfter(cbe.getToDepartmentSlipBalanceBefore() + entryData.getSlipValue());
        cbe.setFromInstitutionSlipBalanceAfter(cbe.getFromInstitutionSlipBalanceBefore() + entryData.getSlipValue());
        cbe.setToInstitutionSlipBalanceAfter(cbe.getToInstitutionSlipBalanceBefore() + entryData.getSlipValue());
        cbe.setFromSiteSlipBalanceAfter(cbe.getFromSiteSlipBalanceBefore() + entryData.getSlipValue());
        cbe.setToSiteSlipBalanceAfter(cbe.getToSiteSlipBalanceBefore() + entryData.getSlipValue());

// eWallet
        cbe.setFromDepartmentEwalletBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentEwalletBalanceAfter() : 0.0);
        cbe.setToDepartmentEwalletBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentEwalletBalanceAfter() : 0.0);
        cbe.setFromInstitutionEwalletBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionEwalletBalanceAfter() : 0.0);
        cbe.setToInstitutionEwalletBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionEwalletBalanceAfter() : 0.0);
        cbe.setFromSiteEwalletBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteEwalletBalanceAfter() : 0.0);
        cbe.setToSiteEwalletBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteEwalletBalanceAfter() : 0.0);

        cbe.setFromDepartmentEwalletBalanceAfter(cbe.getFromDepartmentEwalletBalanceBefore() + entryData.getEwalletValue());
        cbe.setToDepartmentEwalletBalanceAfter(cbe.getToDepartmentEwalletBalanceBefore() + entryData.getEwalletValue());
        cbe.setFromInstitutionEwalletBalanceAfter(cbe.getFromInstitutionEwalletBalanceBefore() + entryData.getEwalletValue());
        cbe.setToInstitutionEwalletBalanceAfter(cbe.getToInstitutionEwalletBalanceBefore() + entryData.getEwalletValue());
        cbe.setFromSiteEwalletBalanceAfter(cbe.getFromSiteEwalletBalanceBefore() + entryData.getEwalletValue());
        cbe.setToSiteEwalletBalanceAfter(cbe.getToSiteEwalletBalanceBefore() + entryData.getEwalletValue());

// Patient Deposit
        cbe.setFromDepartmentPatientDepositBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentPatientDepositBalanceAfter() : 0.0);
        cbe.setToDepartmentPatientDepositBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentPatientDepositBalanceAfter() : 0.0);
        cbe.setFromInstitutionPatientDepositBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionPatientDepositBalanceAfter() : 0.0);
        cbe.setToInstitutionPatientDepositBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionPatientDepositBalanceAfter() : 0.0);
        cbe.setFromSitePatientDepositBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSitePatientDepositBalanceAfter() : 0.0);
        cbe.setToSitePatientDepositBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSitePatientDepositBalanceAfter() : 0.0);

        cbe.setFromDepartmentPatientDepositBalanceAfter(cbe.getFromDepartmentPatientDepositBalanceBefore() + entryData.getPatientDepositValue());
        cbe.setToDepartmentPatientDepositBalanceAfter(cbe.getToDepartmentPatientDepositBalanceBefore() + entryData.getPatientDepositValue());
        cbe.setFromInstitutionPatientDepositBalanceAfter(cbe.getFromInstitutionPatientDepositBalanceBefore() + entryData.getPatientDepositValue());
        cbe.setToInstitutionPatientDepositBalanceAfter(cbe.getToInstitutionPatientDepositBalanceBefore() + entryData.getPatientDepositValue());
        cbe.setFromSitePatientDepositBalanceAfter(cbe.getFromSitePatientDepositBalanceBefore() + entryData.getPatientDepositValue());
        cbe.setToSitePatientDepositBalanceAfter(cbe.getToSitePatientDepositBalanceBefore() + entryData.getPatientDepositValue());

// Patient Points
        cbe.setFromDepartmentPatientPointsBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentPatientPointsBalanceAfter() : 0.0);
        cbe.setToDepartmentPatientPointsBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentPatientPointsBalanceAfter() : 0.0);
        cbe.setFromInstitutionPatientPointsBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionPatientPointsBalanceAfter() : 0.0);
        cbe.setToInstitutionPatientPointsBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionPatientPointsBalanceAfter() : 0.0);
        cbe.setFromSitePatientPointsBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSitePatientPointsBalanceAfter() : 0.0);
        cbe.setToSitePatientPointsBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSitePatientPointsBalanceAfter() : 0.0);

        cbe.setFromDepartmentPatientPointsBalanceAfter(cbe.getFromDepartmentPatientPointsBalanceBefore() + entryData.getPatientPointsValue());
        cbe.setToDepartmentPatientPointsBalanceAfter(cbe.getToDepartmentPatientPointsBalanceBefore() + entryData.getPatientPointsValue());
        cbe.setFromInstitutionPatientPointsBalanceAfter(cbe.getFromInstitutionPatientPointsBalanceBefore() + entryData.getPatientPointsValue());
        cbe.setToInstitutionPatientPointsBalanceAfter(cbe.getToInstitutionPatientPointsBalanceBefore() + entryData.getPatientPointsValue());
        cbe.setFromSitePatientPointsBalanceAfter(cbe.getFromSitePatientPointsBalanceBefore() + entryData.getPatientPointsValue());
        cbe.setToSitePatientPointsBalanceAfter(cbe.getToSitePatientPointsBalanceBefore() + entryData.getPatientPointsValue());

// Online Settlement
        cbe.setFromDepartmentOnlineSettlementBalanceBefore(lastFromDepartmentEntry != null ? lastFromDepartmentEntry.getFromDepartmentOnlineSettlementBalanceAfter() : 0.0);
        cbe.setToDepartmentOnlineSettlementBalanceBefore(lastToDepartmentEntry != null ? lastToDepartmentEntry.getToDepartmentOnlineSettlementBalanceAfter() : 0.0);
        cbe.setFromInstitutionOnlineSettlementBalanceBefore(lastFromInstitutionEntry != null ? lastFromInstitutionEntry.getFromInstitutionOnlineSettlementBalanceAfter() : 0.0);
        cbe.setToInstitutionOnlineSettlementBalanceBefore(lastToInstitutionEntry != null ? lastToInstitutionEntry.getToInstitutionOnlineSettlementBalanceAfter() : 0.0);
        cbe.setFromSiteOnlineSettlementBalanceBefore(lastFromSiteEntry != null ? lastFromSiteEntry.getFromSiteOnlineSettlementBalanceAfter() : 0.0);
        cbe.setToSiteOnlineSettlementBalanceBefore(lastToSiteEntry != null ? lastToSiteEntry.getToSiteOnlineSettlementBalanceAfter() : 0.0);

        cbe.setFromDepartmentOnlineSettlementBalanceAfter(cbe.getFromDepartmentOnlineSettlementBalanceBefore() + entryData.getOnlineSettlementValue());
        cbe.setToDepartmentOnlineSettlementBalanceAfter(cbe.getToDepartmentOnlineSettlementBalanceBefore() + entryData.getOnlineSettlementValue());
        cbe.setFromInstitutionOnlineSettlementBalanceAfter(cbe.getFromInstitutionOnlineSettlementBalanceBefore() + entryData.getOnlineSettlementValue());
        cbe.setToInstitutionOnlineSettlementBalanceAfter(cbe.getToInstitutionOnlineSettlementBalanceBefore() + entryData.getOnlineSettlementValue());
        cbe.setFromSiteOnlineSettlementBalanceAfter(cbe.getFromSiteOnlineSettlementBalanceBefore() + entryData.getOnlineSettlementValue());
        cbe.setToSiteOnlineSettlementBalanceAfter(cbe.getToSiteOnlineSettlementBalanceBefore() + entryData.getOnlineSettlementValue());

    }

    public CashBookEntry fetchLastCashbookEntryForFromDepartment(Department dept) {
        String jpql = "select e "
                + " from CashBookEntry e "
                + " where e.retired=:ret "
                + " and e.fromDepartment=:dep"
                + " order by e.id desc";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("dep", dept);
        return cashbookEntryFacade.findFirstByJpql(jpql, m);
    }

    public CashBookEntry fetchLastCashbookEntryForToDepartment(Department dept) {
        String jpql = "select e "
                + " from CashBookEntry e "
                + " where e.retired=:ret "
                + " and e.toDepartment=:dep"
                + " order by e.id desc";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("dep", dept);
        return cashbookEntryFacade.findFirstByJpql(jpql, m);
    }

    public CashBookEntry fetchLastCashbookEntryForFromInstitution(Institution institution) {
        String jpql = "select e "
                + " from CashBookEntry e "
                + " where e.retired = :ret "
                + " and e.fromInstitution = :inst "
                + " order by e.id desc";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("inst", institution);
        return cashbookEntryFacade.findFirstByJpql(jpql, m);
    }

    public CashBookEntry fetchLastCashbookEntryForToInstitution(Institution institution) {
        String jpql = "select e "
                + " from CashBookEntry e "
                + " where e.retired = :ret "
                + " and e.toInstitution = :inst "
                + " order by e.id desc";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("inst", institution);
        return cashbookEntryFacade.findFirstByJpql(jpql, m);
    }

    public CashBookEntry fetchLastCashbookEntryForFromSite(Institution site) {
        String jpql = "select e "
                + " from CashBookEntry e "
                + " where e.retired = :ret "
                + " and e.fromSite = :site "
                + " order by e.id desc";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("site", site);
        return cashbookEntryFacade.findFirstByJpql(jpql, m);
    }

    public CashBookEntry fetchLastCashbookEntryForToSite(Institution site) {
        String jpql = "select e "
                + " from CashBookEntry e "
                + " where e.retired = :ret "
                + " and e.toSite = :site "
                + " order by e.id desc";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("site", site);
        return cashbookEntryFacade.findFirstByJpql(jpql, m);
    }

}
