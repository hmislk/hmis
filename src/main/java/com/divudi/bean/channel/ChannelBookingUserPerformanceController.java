/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dto.ChannelBookingUserPerformanceDTO;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Controller for Channel Booking User Performance Report
 * Shows user performance metrics for channel bookings
 *
 * @author Claude Code
 */
@Named
@SessionScoped
public class ChannelBookingUserPerformanceController implements Serializable {

    private static final long serialVersionUID = 1L;

    // Filters
    private Date fromDate;
    private Date toDate;

    // Report data
    private List<ChannelBookingUserPerformanceDTO> userPerformanceList;

    // Dependencies
    @EJB
    private BillFacade billFacade;

    @Inject
    private SessionController sessionController;

    /**
     * Constructor - initializes with last month's date range
     */
    public ChannelBookingUserPerformanceController() {
        resetDatesToLastMonth();
    }

    /**
     * Reset dates to last month (first day to last day)
     */
    private void resetDatesToLastMonth() {
        Calendar cal = Calendar.getInstance();

        // Set to first day of last month
        cal.add(Calendar.MONTH, -1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        fromDate = cal.getTime();

        // Set to last day of last month
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        toDate = cal.getTime();
    }

    /**
     * Process the report - fetch user performance data
     */
    public void processReport() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select both From Date and To Date");
            return;
        }

        if (fromDate.after(toDate)) {
            JsfUtil.addErrorMessage("From Date cannot be after To Date");
            return;
        }

        userPerformanceList = new ArrayList<>();

        try {
            // Step 1: Get booking counts (bills created by each user)
            String bookingJpql = "SELECT new com.divudi.core.data.dto.ChannelBookingUserPerformanceDTO("
                    + "b.creater.id, "
                    + "b.creater.name, "
                    + "b.creater.webUserPerson.name, "
                    + "COUNT(b.id), "
                    + "0L"
                    + ") "
                    + "FROM Bill b "
                    + "WHERE b.billTypeAtomic IN :billTypes "
                    + "AND b.createdAt BETWEEN :fromDate AND :toDate "
                    + "AND b.retired = false "
                    + "GROUP BY b.creater.id, b.creater.name, b.creater.webUserPerson.name";

            Map<String, Object> params = new HashMap<>();
            params.put("billTypes", Arrays.asList(
                    BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT,
                    BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT,
                    BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT
            ));
            params.put("fromDate", fromDate);
            params.put("toDate", toDate);

            List<ChannelBookingUserPerformanceDTO> bookingList =
                (List<ChannelBookingUserPerformanceDTO>) billFacade.findLightsByJpql(bookingJpql, params, TemporalType.TIMESTAMP);

            // Step 2: Get payment counts (payments accepted by each user)
            String paymentJpql = "SELECT new com.divudi.core.data.dto.ChannelBookingUserPerformanceDTO("
                    + "p.creater.id, "
                    + "p.creater.name, "
                    + "p.creater.webUserPerson.name, "
                    + "0L, "
                    + "COUNT(DISTINCT p.bill.id)"
                    + ") "
                    + "FROM Payment p "
                    + "WHERE p.bill.billTypeAtomic IN :billTypes "
                    + "AND p.createdAt BETWEEN :fromDate AND :toDate "
                    + "AND p.retired = false "
                    + "AND p.bill.retired = false "
                    + "GROUP BY p.creater.id, p.creater.name, p.creater.webUserPerson.name";

            List<ChannelBookingUserPerformanceDTO> paymentList =
                (List<ChannelBookingUserPerformanceDTO>) billFacade.findLightsByJpql(paymentJpql, params, TemporalType.TIMESTAMP);

            // Step 3: Merge the results
            Map<Long, ChannelBookingUserPerformanceDTO> userMap = new HashMap<>();

            // Add booking counts
            if (bookingList != null) {
                for (ChannelBookingUserPerformanceDTO dto : bookingList) {
                    userMap.put(dto.getUserId(), dto);
                }
            }

            // Add payment counts
            if (paymentList != null) {
                for (ChannelBookingUserPerformanceDTO dto : paymentList) {
                    if (userMap.containsKey(dto.getUserId())) {
                        // User exists, update payment count
                        userMap.get(dto.getUserId()).setPaidBookings(dto.getPaidBookings());
                    } else {
                        // User only has payments, add them
                        userMap.put(dto.getUserId(), dto);
                    }
                }
            }

            // Convert map to list and sort by user name
            userPerformanceList = new ArrayList<>(userMap.values());
            userPerformanceList.sort((a, b) -> {
                String nameA = a.getUserPersonName() != null ? a.getUserPersonName() : a.getUserName();
                String nameB = b.getUserPersonName() != null ? b.getUserPersonName() : b.getUserName();
                nameA = nameA != null ? nameA : "";
                nameB = nameB != null ? nameB : "";
                return nameA.compareToIgnoreCase(nameB);
            });

            if (userPerformanceList.isEmpty()) {
                JsfUtil.addErrorMessage("No data found for the selected date range");
            } else {
                JsfUtil.addSuccessMessage("Report generated successfully with "
                        + userPerformanceList.size() + " user(s)");
            }

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error generating report: " + e.getMessage());
            e.printStackTrace();
            userPerformanceList = new ArrayList<>();
        }
    }

    /**
     * Clear all filters and data
     */
    public void clear() {
        resetDatesToLastMonth();
        userPerformanceList = new ArrayList<>();
    }

    /**
     * Get total bookings across all users
     * @return Sum of all bookings
     */
    public Long getTotalBookingsSum() {
        if (userPerformanceList == null || userPerformanceList.isEmpty()) {
            return 0L;
        }
        return userPerformanceList.stream()
                .mapToLong(ChannelBookingUserPerformanceDTO::getTotalBookings)
                .sum();
    }

    /**
     * Get total paid bookings across all users
     * @return Sum of all paid bookings
     */
    public Long getTotalPaidBookingsSum() {
        if (userPerformanceList == null || userPerformanceList.isEmpty()) {
            return 0L;
        }
        return userPerformanceList.stream()
                .mapToLong(ChannelBookingUserPerformanceDTO::getPaidBookings)
                .sum();
    }

    // Getters and Setters

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public List<ChannelBookingUserPerformanceDTO> getUserPerformanceList() {
        if (userPerformanceList == null) {
            userPerformanceList = new ArrayList<>();
        }
        return userPerformanceList;
    }

    public void setUserPerformanceList(List<ChannelBookingUserPerformanceDTO> userPerformanceList) {
        this.userPerformanceList = userPerformanceList;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }
}
