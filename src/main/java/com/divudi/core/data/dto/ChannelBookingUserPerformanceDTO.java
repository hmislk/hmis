
package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * DTO for Channel Booking User Performance Report
 * Tracks user performance metrics for channel bookings
 *
 * @author Claude Code
 */
public class ChannelBookingUserPerformanceDTO implements Serializable {

    private Long userId;
    private String userName;
    private String userPersonName;
    private Long totalBookings;
    private Long paidBookings;

    /**
     * Default constructor required for DTO projection
     */
    public ChannelBookingUserPerformanceDTO() {
        this.totalBookings = 0L;
        this.paidBookings = 0L;
    }

    /**
     * Constructor for JPQL DTO projection
     * @param userId User ID
     * @param userName User's login name
     * @param userPersonName User's person name (from WebUserPerson)
     * @param totalBookings Total number of bookings created
     * @param paidBookings Number of paid/settled bookings
     */
    public ChannelBookingUserPerformanceDTO(Long userId, String userName, String userPersonName, Long totalBookings, Long paidBookings) {
        this.userId = userId;
        this.userName = userName;
        this.userPersonName = userPersonName;
        this.totalBookings = totalBookings;
        this.paidBookings = paidBookings;
    }

    /**
     * Get the display name combining user name and person name
     * @return Formatted display name
     */
    public String getDisplayName() {
        if (userPersonName != null && !userPersonName.trim().isEmpty()) {
            if (userName != null && !userName.equals(userPersonName)) {
                return userPersonName + " (" + userName + ")";
            }
            return userPersonName;
        }
        return userName != null ? userName : "";
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPersonName() {
        return userPersonName;
    }

    public void setUserPersonName(String userPersonName) {
        this.userPersonName = userPersonName;
    }

    public Long getTotalBookings() {
        return totalBookings != null ? totalBookings : 0L;
    }

    public void setTotalBookings(Long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public Long getPaidBookings() {
        return paidBookings != null ? paidBookings : 0L;
    }

    public void setPaidBookings(Long paidBookings) {
        this.paidBookings = paidBookings;
    }
}
