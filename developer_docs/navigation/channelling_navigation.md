# Channelling Navigation Guide

This document lists all pages reachable from the **Channelling** menu. Each entry shows how to navigate from the main screen, the target page path, required user privilege and any relevant configuration keys.

| Menu steps | Page path | Privilege | Configuration notes |
|------------|-----------|-----------|---------------------|
| **Channelling → Channel Booking View** | `/channel/channel_booking.xhtml` | `ChannellingChannelBooking` | Requires shift start when `applicationPreference.opdBillingAftershiftStart` is enabled |
| **Channelling → Channel Booking - By Dates** | `/channel/channel_booking_by_date.xhtml` | `ChannellingChannelBooking` | Same conditions as Channel Booking View |
| **Channelling → Channel Booking - By Month** | `/channel/channel_booking_by_month.xhtml` | `ChannelBookingByMonth` | |
| **Channelling → Schedule Calendar** | `/channel/schedule_calendar.xhtml` | `Channelling` | |
| **Channelling → Channel Queue** | `/channel/channel_queue.xhtml` | `ChannellingChannelBooking` | |
| **Channelling → Channel Display** | `/channel/channel_display.xhtml` | `ChannellingChannelBooking` | |
| **Channelling → Past Bookings** | `/channel/past_channel_booking.xhtml` | `ChannellingPastBooking` | |
| **Channelling → Channel Bill Search** | `/channel/channel_bill_search.xhtml` | - | |
| **Channelling → Doctor Leave → By Date** | `/channel/channel_doctor_leave_by_date.xhtml` | `ChannellingDoctorLeaveByDate` | |
| **Channelling → Doctor Leave → By Service Session** | `/channel/channel_doctor_leave_by_service_session.xhtml` | `ChannellingDoctorLeaveByServiceSession` | |
| **Channelling → Channel Scheduling** | `/channel/channel_scheduling/index.xhtml` | `ChannelCreateSessions` | |
| **Channelling → Payment → Pay Doctor** | `/channel/channel_payment_staff_bill.xhtml` | `ChannellingPaymentPayDoctor` | |
| **Channelling → Payment → Payment Due Search** | `/channel/channel_payments_due_search.xhtml` | `ChannellingPaymentDueSearch` | |
| **Channelling → Payment → Payment Done Search** | `/channel/channel_payment_bill_search.xhtml` | `ChannellingPaymentDoneSearch` | |
| **Channelling → Payment → Pay Agent** | `/channel/channel_payment_agent_bill.xhtml` | `ChannellingPaymentPayDoctor` | |
| **Channelling → Cashier Transaction → Income** | `/channel/channel_income_bill.xhtml` | `ChannelCashierTransactionIncome` | |
| **Channelling → Cashier Transaction → Income Bill Search** | `/channel/channel_income_bill_search_own.xhtml` | `ChannelCashierTransactionIncomeSearch` | |
| **Channelling → Cashier Transaction → Expenses** | `/channel/channel_expenses_bill.xhtml` | `ChannelCashierTransactionExpencess` | |
| **Channelling → Cashier Transaction → Expenses Bill Search** | `/channel/channel_expenses_bill_search_own.xhtml` | `ChannelCashierTransactionExpencessSearch` | |
| **Channelling → Credit/Debit Note → Credit Note** | `/channel/channel_credit_note_bill.xhtml` | `ChannelCrdeitNote` | |
| **Channelling → Credit/Debit Note → Credit Note Search** | `/channel/channel_credit_note_bill_search_own.xhtml` | `ChannelCrdeitNoteSearch` | |
| **Channelling → Credit/Debit Note → Debit Note** | `/channel/channel_debit_note_bill.xhtml` | `ChannelDebitNote` | |
| **Channelling → Credit/Debit Note → Debit Note Search** | `/channel/channel_debit_note_bill_search_own.xhtml` | `ChannelDebitNoteSearch` | |
| **Channelling → Reports** | `/channel/channel_reports.xhtml` | `ChannelReports` | |
| **Channelling → Channel Analytics** | `/analytics/opd/index.xhtml` | `ChannelSummery` | |
| **Channelling → Management** | `/channel/management_index.xhtml` | `ChannelManagement` | |
| **Channelling → Patient Portal** | `/channel/patient_portal.xhtml` | `ChannelPatientPortal` | |
| **Channelling → Doctor Card** | `/channel/doctor_card.xhtml` | `ChannelDoctorCard` | |
| **Channelling → Financial Transaction Manager** | `/faces/cashier/index.xhtml` | `Opd` | Shown only when `applicationPreference.opdBillingAftershiftStart` is enabled |

