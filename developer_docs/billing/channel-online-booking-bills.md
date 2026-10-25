# Channel online agent bookings: bill shape

- Pending: `CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT` (ChannelOnCall); holds `onlineBooking`.
- Completed: `CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT` (ChannelAgent, method Agent). `referenceBill` = the pending bill.
- Cancelled at the counter: `CHANNEL_CANCELLATION_WITH_PAYMENT_ONLINE_BOOKING` (has a creater). `billedBill` = the completed bill.
- The completed bill has **no creater and no patient**. Reports must `left join` `bill.creater` and `bill.patient`; an inner join (or selecting `bill.creater.name`) silently drops every online booking.
- Patient name/phone: `bill.referenceBill.onlineBooking.patientName/phoneNo`; for the cancellation, `bill.billedBill.referenceBill.onlineBooking`.
- `ChannelApi.createBill` (old API path) sets no `billTypeAtomic` at all, so atomic-type reports never see those bills.
