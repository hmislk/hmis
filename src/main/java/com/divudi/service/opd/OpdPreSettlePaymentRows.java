package com.divudi.service.opd;

import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.Staff;

/**
 * Fills one Payment from one Multiple Payment Methods row of an OPD pre-bill
 * settle (#26261). Each method sets its own values and stops. No method uses
 * the values of another method. Only the Patient Deposit row changes the
 * patient balance, and only the Staff row adds a staff credit. Both go through
 * the request, so the writer saves them in its transaction and the undo steps
 * can put them back.
 */
public final class OpdPreSettlePaymentRows {

    private OpdPreSettlePaymentRows() {
    }

    public static void fillMultipleRow(Payment p, ComponentDetail cd, Patient patient, OpdPreSettleRequest request) {
        PaymentMethodData data = cd.getPaymentMethodData();
        switch (cd.getPaymentMethod()) {
            case Card:
                p.setBank(data.getCreditCard().getInstitution());
                p.setCreditCardRefNo(data.getCreditCard().getNo());
                p.setPaidValue(data.getCreditCard().getTotalValue());
                p.setComments(data.getCreditCard().getComment());
                break;
            case Cheque:
                p.setChequeDate(data.getCheque().getDate());
                p.setChequeRefNo(data.getCheque().getNo());
                p.setPaidValue(data.getCheque().getTotalValue());
                p.setComments(data.getCheque().getComment());
                break;
            case Cash:
                p.setPaidValue(data.getCash().getTotalValue());
                break;
            case ewallet:
                p.setPolicyNo(data.getEwallet().getReferralNo());
                p.setReferenceNo(data.getEwallet().getReferenceNo());
                p.setCreditCompany(data.getEwallet().getInstitution());
                p.setPaidValue(data.getEwallet().getTotalValue());
                p.setComments(data.getEwallet().getComment());
                break;
            case Agent:
            case Credit:
                p.setReferenceNo(data.getCredit().getReferralNo());
                p.setComments(data.getCredit().getComment());
                p.setPaidValue(data.getCredit().getTotalValue());
                break;
            case PatientDeposit:
                final double deposit = data.getPatient_deposit().getTotalValue();
                p.setPaidValue(deposit);
                p.setComments(data.getPatient_deposit().getComment());
                final Double balanceBefore = patient.getRunningBalance();
                request.addUndo(() -> patient.setRunningBalance(balanceBefore));
                if (balanceBefore != null) {
                    patient.setRunningBalance(balanceBefore - deposit);
                } else {
                    patient.setRunningBalance(0.0 - deposit);
                }
                request.getPatientsToSave().add(patient);
                break;
            case Slip:
                p.setPaidValue(data.getSlip().getTotalValue());
                p.setBank(data.getSlip().getInstitution());
                p.setRealizedAt(data.getSlip().getDate());
                p.setComments(data.getSlip().getComment());
                break;
            case OnlineSettlement:
                p.setBank(data.getOnlineSettlement().getInstitution());
                p.setPaidValue(data.getOnlineSettlement().getTotalValue());
                p.setPaymentDate(data.getOnlineSettlement().getDate());
                p.setReferenceNo(data.getOnlineSettlement().getReferenceNo());
                p.setComments(data.getOnlineSettlement().getComment());
                break;
            case OnCall:
            case Staff:
                p.setPaidValue(data.getStaffCredit().getTotalValue());
                if (data.getStaffCredit().getToStaff() != null) {
                    final Staff creditStaff = data.getStaffCredit().getToStaff();
                    request.addStaffCredit(creditStaff, data.getStaffCredit().getTotalValue());
                }
                break;
            default:
                break;
        }
    }
}
