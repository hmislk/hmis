package com.divudi.service;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.FeeType;
import com.divudi.data.HistoryType;
import com.divudi.data.PaymentMethod;
import static com.divudi.data.PaymentMethod.Agent;
import static com.divudi.data.PaymentMethod.Card;
import static com.divudi.data.PaymentMethod.Cash;
import static com.divudi.data.PaymentMethod.Cheque;
import static com.divudi.data.PaymentMethod.Credit;
import static com.divudi.data.PaymentMethod.MultiplePaymentMethods;
import static com.divudi.data.PaymentMethod.OnCall;
import static com.divudi.data.PaymentMethod.OnlineSettlement;
import static com.divudi.data.PaymentMethod.Slip;
import static com.divudi.data.PaymentMethod.Staff;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.ServiceSessionBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Patient;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.RefundBill;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.WebUser;
import com.divudi.entity.channel.SessionInstance;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.java.CommonFunctions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 *
 * @author Chinthaka Prasad
 */
@Stateless
public class ChannelService {

    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillSessionFacade billSessionFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private ServiceSessionBean serviceSessionBean;
    @EJB
    private ItemFeeFacade itemFeeFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillNumberGenerator billNumberBean;

    @Inject
    BillBeanController billBeanController;

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public Bill saveBilledBill(boolean forReservedNumbers, Patient patient, SessionInstance session, String refNo, WebUser user) {
        Bill savingBill = createBill(patient, session);
        BillItem savingBillItemForSession = createSessionItem(savingBill, refNo, session);

//        PriceMatrix priceMatrix;
//        if (itemsAddedToBooking != null || itemsAddedToBooking.isEmpty()) {
//            for (Item ai : itemsAddedToBooking) {
//                BillItem aBillItem = createAdditionalItem(savingBill, ai);
//                additionalBillItems.add(aBillItem);
//            }
//        }
        BillSession savingBillSession;

        savingBillSession = createBillSession(savingBill, savingBillItemForSession, forReservedNumbers, session);
        System.out.println(savingBillSession);

        List<BillFee> savingBillFees = new ArrayList<>();

//        priceMatrix = priceMatrixController.fetchChannellingMemberShipDiscount(paymentMethod, paymentScheme, selectedSessionInstance.getOriginatingSession().getCategory());
////        System.out.println("priceMatrix = " + priceMatrix);
        List<BillFee> savingBillFeesFromSession = createBillFeeForSessions(savingBill, savingBillItemForSession, false, null);

        List<BillFee> savingBillFeesFromAdditionalItems = new ArrayList<>();

//        if (!additionalBillItems.isEmpty()) {
//            for (BillItem abi : additionalBillItems) {
//                List<BillFee> blf = createBillFeeForSessions(savingBill, abi, addedItemFees, priceMatrix);
//                for (BillFee bf : blf) {
//                    savingBillFeesFromAdditionalItems.add(bf);
//                }
//            }
//        }
        if (savingBillFeesFromSession != null) {
            savingBillFees.addAll(savingBillFeesFromSession);
        }
        if (!savingBillFeesFromAdditionalItems.isEmpty()) {
            savingBillFees.addAll(savingBillFeesFromAdditionalItems);
        }

        List<BillItem> savingBillItems = new ArrayList<>();
        savingBillItems.add(savingBillItemForSession);
        getBillItemFacade().edit(savingBillItemForSession);
        savingBillItemForSession.setHospitalFee(billBeanController.calFeeValue(FeeType.OwnInstitution, savingBillItemForSession));
        savingBillItemForSession.setStaffFee(billBeanController.calFeeValue(FeeType.Staff, savingBillItemForSession));
        savingBillItemForSession.setBillSession(savingBillSession);

        getBillSessionFacade().edit(savingBillSession);
        savingBill.setHospitalFee(billBeanController.calFeeValue(FeeType.OwnInstitution, savingBill));
        savingBill.setStaffFee(billBeanController.calFeeValue(FeeType.Staff, savingBill));
        savingBill.setSingleBillItem(savingBillItemForSession);
        savingBill.setSingleBillSession(savingBillSession);
        savingBill.setBillItems(savingBillItems);
        savingBill.setBillFees(savingBillFees);
        // savingBill.setCashPaid(cashPaid);
        // savingBill.setCashBalance(cashBalance);
        savingBill.setBalance(savingBill.getNetTotal());

//        if (savingBill.getBillType() == BillType.ChannelAgent) {
//            updateBallance(savingBill.getCreditCompany(), 0 - savingBill.getNetTotal(), HistoryType.ChannelBooking, savingBill, savingBillItemForSession, savingBillSession, savingBillItemForSession.getAgentRefNo());
//            savingBill.setBalance(0.0);
//            savingBillSession.setPaidBillSession(savingBillSession);
//        } else if (savingBill.getBillType() == BillType.ChannelCash) {
//            savingBill.setBalance(0.0);
//            savingBillSession.setPaidBillSession(savingBillSession);
//        } else if (savingBill.getBillType() == BillType.ChannelOnCall) {
//            savingBill.setBalance(savingBill.getNetTotal());
//        } else if (savingBill.getBillType() == BillType.ChannelStaff) {
//            savingBill.setBalance(0.0);
//            savingBillSession.setPaidBillSession(savingBillSession);
//        }
        savingBill.setSingleBillItem(savingBillItemForSession);
        savingBill.setSingleBillSession(savingBillSession);
//        if (referredBy != null) {
//            savingBill.setReferredBy(referredBy);
//        }
//        if (referredByInstitution != null) {
//            savingBill.setReferenceInstitution(referredByInstitution);
//        }
//        if (collectingCentre != null) {
//            savingBill.setCollectingCentre(collectingCentre);
//        }
//        if (savingBill.getPaidAmount() == 0.0) {
//            if (!(savingBill.getPaymentMethod() == PaymentMethod.OnCall)) {
//                savingBill.setPaidAmount(feeNetTotalForSelectedBill);
//            } else {
//                if (feeNetTotalForSelectedBill != null) {
//                    savingBill.setNetTotal(feeNetTotalForSelectedBill);
//                }
//            }
//        }

        calculateBillTotalsFromBillFees(savingBill, savingBillFees);

        getBillFacade().edit(savingBill);
        getBillSessionFacade().edit(savingBillSession);
        return savingBill;
    }

    public List<ItemFee> findServiceSessionFees(ServiceSession ss) {
        String sql;
        Map m = new HashMap();
        sql = "Select f from ItemFee f "
                + " where f.retired=false "
                + " and (f.serviceSession=:ses "
                + " or f.item=:ses )"
                + " order by f.id";
        m.put("ses", ss);
        List<ItemFee> tfs = itemFeeFacade.findByJpql(sql, m);
        return tfs;
    }

    private void calculateBillTotalsFromBillFees(Bill billToCaclculate, List<BillFee> billfeesAvailable) {
//        System.out.println("calculateBillTotalsFromBillFees");
//        System.out.println("billToCaclculate = " + billToCaclculate);
//        System.out.println("billfeesAvailable = " + billfeesAvailable);
        double calculatingGrossBillTotal = 0.0;
        double calculatingNetBillTotal = 0.0;

        for (BillFee iteratingBillFee : billfeesAvailable) {
//            System.out.println("iteratingBillFee = " + iteratingBillFee);
            if (iteratingBillFee.getFee() == null) {
                continue;
            }

            calculatingGrossBillTotal += iteratingBillFee.getFeeGrossValue();
//            System.out.println("calculatingGrossBillTotal = " + calculatingGrossBillTotal);
            calculatingNetBillTotal += iteratingBillFee.getFeeValue();
//            System.out.println("calculatingNetBillTotal = " + calculatingNetBillTotal);

        }
        billToCaclculate.setDiscount(calculatingGrossBillTotal - calculatingNetBillTotal);
        billToCaclculate.setNetTotal(calculatingNetBillTotal);
        System.out.println(calculatingNetBillTotal + " g " + calculatingGrossBillTotal);
        billToCaclculate.setTotal(calculatingGrossBillTotal);
        getBillFacade().edit(billToCaclculate);
    }

    @Deprecated
    private String generateBillNumberDeptId(Bill bill) {
        String suffix = bill.getDepartment().getDepartmentCode();
        BillClassType billClassType = null;
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff, BillType.ChannelCredit};
        List<BillType> bts = Arrays.asList(billTypes);
        BillType billType = null;
        String deptId = null;
        if (bill instanceof BilledBill) {

            billClassType = BillClassType.BilledBill;
            if (bill.getBillType() == BillType.ChannelOnCall || bill.getBillType() == BillType.ChannelStaff) {
                billType = bill.getBillType();
                if (billType == BillType.ChannelOnCall) {
                    suffix += "BKONCALL";
                } else {
                    suffix += "BKSTAFF";
                }
                deptId = billNumberBean.departmentBillNumberGenerator(bill.getInstitution(), bill.getDepartment(), billType, billClassType, suffix);
            } else {
                suffix += "CHANN";
                deptId = billNumberBean.departmentBillNumberGenerator(bill.getInstitution(), bill.getDepartment(), bts, billClassType, suffix);
            }
        }

        if (bill instanceof CancelledBill) {
            suffix += "CHANNCAN";
            billClassType = BillClassType.CancelledBill;
            deptId = billNumberBean.departmentBillNumberGenerator(bill.getInstitution(), bill.getDepartment(), bts, billClassType, suffix);
        }

        if (bill instanceof RefundBill) {
            suffix += "CHANNREF";
            billClassType = BillClassType.RefundBill;
            deptId = billNumberBean.departmentBillNumberGenerator(bill.getInstitution(), bill.getDepartment(), bts, billClassType, suffix);
        }

        return deptId;
    }

    private Bill createBill(Patient patient, SessionInstance session) {
        Bill bill = new BilledBill();
        bill.setStaff(session.getOriginatingSession().getStaff());
        //bill.setToStaff(toStaff);
        bill.setAppointmentAt(session.getSessionDate());
        bill.setTotal(session.getOriginatingSession().getTotal());
        bill.setNetTotal(session.getOriginatingSession().getTotal());
        bill.setPaymentMethod(PaymentMethod.OnCall);
        bill.setPatient(patient);

        bill.setBillType(BillType.ChannelOnCall);
        bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT);
        System.out.println(bill);
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(session.getDepartment(), BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT);

        if (deptId.equals("")) {
            return null;
        }
        bill.setDeptId(deptId);
        bill.setInsId(deptId);

        if (bill.getBillType().getParent() == BillType.ChannelCashFlow) {
            bill.setBookingId(billNumberBean.bookingIdGenerator(session.getInstitution(), new BilledBill()));
            bill.setPaidAmount(session.getOriginatingSession().getTotal());
            bill.setPaidAt(new Date());
        }

        bill.setBillDate(new Date());
        bill.setBillTime(new Date());
        bill.setCreatedAt(new Date());
        // bill.setCreater(getSessionController().getLoggedUser());
        // bill.setDepartment(getSessionController().getDepartment());
        //bill.setInstitution(session.getInstitution());

        bill.setToDepartment(session.getDepartment());
        bill.setToInstitution(session.getInstitution());

        getBillFacade().create(bill);

        if (bill.getBillType() == BillType.ChannelCash || bill.getBillType() == BillType.ChannelAgent || bill.getBillType() == BillType.ChannelStaff) {
            bill.setPaidBill(bill);
            getBillFacade().edit(bill);
        }
        return bill;
    }

    private BillItem createSessionItem(Bill bill, String refNo, SessionInstance session) {
        BillItem bi = new BillItem();
        bi.setAdjustedValue(0.0);
        bi.setAgentRefNo(refNo);
        bi.setBill(bill);
        bi.setBillTime(new Date());
        bi.setCreatedAt(new Date());
        // bi.setCreater(getSessionController().getLoggedUser());
        bi.setGrossValue(session.getOriginatingSession().getTotal());
        bi.setItem(session.getOriginatingSession());
//        bi.setItem(getSelectedSessionInstance().getOriginatingSession());
        bi.setNetRate(session.getOriginatingSession().getTotal());
        bi.setNetValue(session.getOriginatingSession().getTotal());
        bi.setQty(1.0);
        bi.setRate(session.getOriginatingSession().getTotal());
        bi.setSessionDate(session.getSessionAt());

        billItemFacade.create(bi);
        return bi;
    }

    private BillSession createBillSession(Bill bill, BillItem billItem, boolean forReservedNumbers, SessionInstance session) {
        BillSession bs = new BillSession();
        bs.setAbsent(false);
        bs.setBill(bill);
        bs.setReservedBooking(forReservedNumbers);
        bs.setBillItem(billItem);
        bs.setCreatedAt(new Date());
        // bs.setCreater(getSessionController().getLoggedUser());
        bs.setDepartment(session.getOriginatingSession().getDepartment());
        bs.setInstitution(session.getOriginatingSession().getInstitution());
        bs.setItem(session.getOriginatingSession());
//        bs.setPresent(true);
//        bs.setPresent(true);
//        bs.setItem(getSelectedSessionInstance().getOriginatingSession());

        bs.setServiceSession(session.getOriginatingSession());
        bs.setSessionInstance(session);
//        bs.setServiceSession(getSelectedSessionInstance().getOriginatingSession());
        bs.setSessionDate(session.getSessionDate());
        bs.setSessionTime(session.getSessionTime());
        bs.setStaff(session.getStaff());

        List<Integer> reservedNumbers = CommonFunctions.convertStringToIntegerList(session.getOriginatingSession().getReserveNumbers());
        Integer count = null;

        if (forReservedNumbers) {
//            // Pass the selectedReservedBookingNumber to the service method
//            count = serviceSessionBean.getNextAvailableReservedNumber(session, reservedNumbers, selectedReserverdBookingNumber);
//            if (count == null) {
//                count = serviceSessionBean.getNextNonReservedSerialNumber(session, reservedNumbers);
//                JsfUtil.addErrorMessage("No reserved numbers available. Normal number is given");
//            }
        } else {
            count = serviceSessionBean.getNextNonReservedSerialNumber(session, reservedNumbers);
        }

        if (count != null) {
            bs.setSerialNo(count);
        } else {
            bs.setSerialNo(1);
        }
        System.out.println(bs);
        getBillSessionFacade().create(bs);

        return bs;
    }

    public List<ItemFee> findItemFees(Item i) {
        String sql;
        Map m = new HashMap();
        sql = "Select f from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:i "
                + " order by f.id";
        m.put("i", i);
        return itemFeeFacade.findByJpql(sql, m);
    }

    private List<BillFee> createBillFeeForSessions(Bill bill, BillItem billItem, boolean thisIsAnAdditionalFee, PriceMatrix priceMatrix) {
        List<BillFee> billFeeList = new ArrayList<>();
        double tmpTotal = 0;
        double tmpDiscount = 0;
        double tmpGrossTotal = 0.0;

        List<ItemFee> sessionsFees = null;

        if (thisIsAnAdditionalFee) {
            sessionsFees = findItemFees(billItem.getItem());
        } else {
            if (billItem.getItem() instanceof ServiceSession) {
                sessionsFees = findServiceSessionFees((ServiceSession) billItem.getItem());
            }
        }

        if (sessionsFees == null) {
            return billFeeList;
        }
        for (ItemFee f : sessionsFees) {
            if (bill.getPaymentMethod() != PaymentMethod.Agent) {
                if (f.getFeeType() == FeeType.OtherInstitution) {
                    continue;
                }
            }
            if (bill.getPaymentMethod() != PaymentMethod.OnCall) {
                if (f.getFeeType() == FeeType.OwnInstitution && f.getName().equalsIgnoreCase("On-Call Fee")) {
                    continue;
                }
            }
            BillFee bf = new BillFee();
            bf.setBill(bill);
            bf.setBillItem(billItem);
            bf.setCreatedAt(new Date());
            // bf.setCreater(getSessionController().getLoggedUser());
            if (f.getFeeType() == FeeType.OwnInstitution) {
                bf.setInstitution(f.getInstitution());
                bf.setDepartment(f.getDepartment());
            } else if (f.getFeeType() == FeeType.OtherInstitution) {
                bf.setInstitution(bill.getInstitution());
            } else if (f.getFeeType() == FeeType.Staff) {
                bf.setSpeciality(f.getSpeciality());
                bf.setStaff(f.getStaff());
            }

            bf.setFee(f);
            bf.setFeeAt(new Date());
            bf.setOrderNo(0);
            bf.setPatient(bill.getPatient());

            if (bf.getPatienEncounter() != null) {
                bf.setPatienEncounter(bill.getPatientEncounter());
            }

            bf.setPatient(bill.getPatient());

            if (f.getFeeType() == FeeType.Staff) {
                bf.setStaff(f.getStaff());
            }

            if (f.getFeeType() == FeeType.OwnInstitution) {
                bf.setInstitution(bill.getInstitution());
            }

            double d = 0;
            if (bill.getPatient().getPerson().isForeigner()) {
                bf.setFeeValue(f.getFfee());
                bf.setFeeGrossValue(f.getFfee());
            } else {
                bf.setFeeValue(f.getFee());
                bf.setFeeGrossValue(f.getFee());
            }

//            priceMatrix = priceMatrixController.fetchChannellingMemberShipDiscount(paymentMethod, paymentScheme, selectedSessionInstance.getOriginatingSession().getCategory());
            if (priceMatrix != null) {
                if (f.getFeeType() == FeeType.OwnInstitution) {
                    d = bf.getFeeValue() * (priceMatrix.getDiscountPercent() / 100);
                    bf.setFeeDiscount(d);
                } else if (f.getFeeType() == FeeType.Staff) {
                    bf.setFeeDiscount(0.0);
                } else {
                    bf.setFeeDiscount(0.0);
                }

                bf.setFeeGrossValue(bf.getFeeGrossValue());
                bf.setFeeValue(bf.getFeeGrossValue() - bf.getFeeDiscount());
                tmpDiscount += d;
            }

            tmpGrossTotal += bf.getFeeGrossValue();
            tmpTotal += bf.getFeeValue();
            billFeeFacade.create(bf);
            billFeeList.add(bf);
        }

        billItem.setDiscount(tmpDiscount);
        billItem.setNetValue(tmpTotal);
        getBillItemFacade().edit(billItem);

        return billFeeList;

    }

}
