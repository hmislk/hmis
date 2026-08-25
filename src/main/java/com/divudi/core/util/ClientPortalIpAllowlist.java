package com.divudi.core.util;

public class ClientPortalIpAllowlist {

    private ClientPortalIpAllowlist() {
    }

    public static boolean isAllowed(String requestIp, String allowedIpsCsv) {
        if (requestIp == null || requestIp.trim().isEmpty()
                || allowedIpsCsv == null || allowedIpsCsv.trim().isEmpty()) {
            return false;
        }
        for (String allowed : allowedIpsCsv.split(",")) {
            if (allowed.trim().equalsIgnoreCase(requestIp.trim())) {
                return true;
            }
        }
        return false;
    }
}
