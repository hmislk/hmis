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
            String jpql = "SELECT new com.divudi.core.data.dto.ChannelBookingUserPerformanceDTO("
                    + "b.creater.id, "
                    + "b.creater.name, "
                    + "b.creater.webUserPerson.name, "
                    + "COUNT(b.id), "
                    + "SUM(CASE "
                    + "    WHEN (b.paymentMethod <> com.divudi.core.data.PaymentMethod.OnCall "
                    + "          OR b.settledAmountByPatient > 0 "
                    + "          OR b.paid = true) "
                    + "    THEN 1 ELSE 0 "
                    + "END)"
                    + ") "
                    + "FROM Bill b "
                    + "WHERE b.billTypeAtomic IN :billTypes "
                    + "AND b.createdAt BETWEEN :fromDate AND :toDate "
                    + "AND b.retired = false "
                    + "GROUP BY b.creater.id, b.creater.name, b.creater.webUserPerson.name "
                    + "ORDER BY b.creater.name";

            Map<String, Object> params = new HashMap<>();
            params.put("billTypes", Arrays.asList(
                    BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT,
                    BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT,
                    BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT
            ));
            params.put("fromDate", fromDate, TemporalType.TIMESTAMP);
            params.put("toDate", toDate, TemporalType.TIMESTAMP);

            userPerformanceList = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);

            if (userPerformanceList == null || userPerformanceList.isEmpty()) {
                JsfUtil.addErrorMessage("No data found for the selected date range");
                userPerformanceList = new ArrayList<>();
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
