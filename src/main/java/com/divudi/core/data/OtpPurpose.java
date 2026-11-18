package com.divudi.core.data;

/**
 * What a one-time code was issued for. A code issued for one purpose never
 * validates for another.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
public enum OtpPurpose {
    /** Second step of a staff login (alternative to the authenticator app) */
    STAFF_LOGIN_2FA,
    /** Confirms that a staff user owns the mobile number used for SMS login codes */
    STAFF_MOBILE_VERIFICATION
}
