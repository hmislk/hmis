
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data;

/**
 *
 * @author Buddhika
 */
public enum HistoryType {
    Sale,
    Issue,
    Order,
    GoodReceive,
    Stock,
    ChannelBooking,
    ChannelBalanceReset,
    @Deprecated
    ChannelBookingCancel,
    ChannelDeposit,
    ChannelDepositCancel,
    ChannelCreditNote,
    ChannelCreditNoteCancel,
    ChannelDebitNote,
    ChannelDebitNoteCancel,
    AgentBalanceUpdateBill,
    CollectingCentreBalanceUpdateBill,
    CollectingCentreDeposit,
    CollectingCentreDepositCancel,
    CollectingCentreCreditNote,
    CollectingCentreCreditNoteCancel,
    CollectingCentreDebitNote,
    CollectingCentreDebitNoteCancel,
    CollectingCentreBilling,
    MonthlyRecord,   
}
