package com.divudi.service.otp;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.MessageType;
import com.divudi.core.data.OtpChannel;
import com.divudi.core.data.OtpPurpose;
import com.divudi.core.entity.OtpCode;
import com.divudi.core.entity.Sms;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.OtpCodeFacade;
import com.divudi.core.facade.SmsFacade;
import com.divudi.ejb.SmsManagerEjb;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * Issues and verifies one-time codes sent by SMS.
 *
 * Security notes: the code is generated with SecureRandom, only its HMAC is
 * stored, the stored SMS text is masked after sending, comparison is constant
 * time, and verification locks the row so a code can be used once only.
 * Account-level lockout after repeated failures is handled by the caller
 * (TwoFactorAuthController), which shares one failure counter between the
 * authenticator app and SMS codes.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Stateless
public class OtpService {

    private static final Logger LOG = Logger.getLogger(OtpService.class.getName());

    public static final int CODE_LENGTH = 6;
    /** Wrong entries allowed on one code before it is closed */
    public static final int MAX_ATTEMPTS_PER_CODE = 5;
    private static final long ONE_HOUR_MS = 60L * 60L * 1000L;
    private static final int KEEP_DAYS = 30;
    /**
     * Local development only: when the JVM is started with
     * -Dhmis.otp.devLogCode=true the code is written to the server log
     * because no SMS gateway is configured. Never set this on a server.
     */
    private static final String DEV_LOG_PROPERTY = "hmis.otp.devLogCode";

    @EJB
    private OtpCodeFacade otpCodeFacade;
    @EJB
    private SmsFacade smsFacade;
    @EJB
    private SmsManagerEjb smsManagerEjb;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    /**
     * Sends a new code to the given mobile number. Earlier unused codes of the
     * same user and purpose stop working.
     *
     * @param mobile the number to send to (as stored on the user)
     * @param signingKey HMAC key (application encryption key)
     */
    public OtpIssueResult issueSmsCode(WebUser user, OtpPurpose purpose, String mobile,
            String signingKey, String requestIp, String userAgent) {
        if (user == null || user.getId() == null || purpose == null) {
            throw new IllegalArgumentException("User and purpose are required");
        }
        String e164 = OtpCodeUtil.normalizeMobile(mobile, allowedCountryCodes());
        if (e164 == null) {
            return OtpIssueResult.invalidMobile();
        }

        Date now = now();
        long cooldownSeconds = configOptionApplicationController.getTwoFactorSmsResendCooldownSeconds();
        List<Date> recent = otpCodeFacade.findCreatedTimesSince(user.getId(), new Date(now.getTime() - ONE_HOUR_MS));
        if (!recent.isEmpty()) {
            long sinceLastMs = now.getTime() - recent.get(recent.size() - 1).getTime();
            if (sinceLastMs < cooldownSeconds * 1000L) {
                return OtpIssueResult.rateLimited(ceilSeconds(cooldownSeconds * 1000L - sinceLastMs));
            }
            int maxPerHour = configOptionApplicationController.getTwoFactorSmsMaxCodesPerUserPerHour();
            if (recent.size() >= maxPerHour) {
                long freeAtMs = recent.get(recent.size() - maxPerHour).getTime() + ONE_HOUR_MS;
                return OtpIssueResult.rateLimited(ceilSeconds(freeAtMs - now.getTime()));
            }
        }
        long sentLastHour = otpCodeFacade.countByChannelSince(OtpChannel.SMS, new Date(now.getTime() - ONE_HOUR_MS));
        if (sentLastHour >= configOptionApplicationController.getTwoFactorSmsMaxPerHourForAllUsers()) {
            LOG.log(Level.WARNING, "OTP SMS hourly limit for all users reached ({0}); request refused", sentLastHour);
            return OtpIssueResult.rateLimited(cooldownSeconds);
        }

        for (OtpCode old : otpCodeFacade.findActive(user.getId(), purpose)) {
            old.setInvalidatedAt(now);
            otpCodeFacade.edit(old);
        }

        String code = OtpCodeUtil.generateNumericCode(CODE_LENGTH);
        int validitySeconds = configOptionApplicationController.getTwoFactorSmsValiditySeconds();
        Date expiresAt = new Date(now.getTime() + validitySeconds * 1000L);

        String sendTo = mobile.trim();
        Sms sms = new Sms();
        sms.setCreatedAt(now);
        sms.setCreater(user);
        sms.setReceipientNumber(sendTo);
        sms.setSmsType(MessageType.StaffTwoFactorOTP);
        sms.setPending(false);
        sms.setSendingMessage(buildMessage(code, validitySeconds));
        boolean sent;
        try {
            sent = smsManagerEjb.sendSms(sms);
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "OTP SMS gateway call failed for {0}", OtpCodeUtil.maskMobile(e164));
            sent = false;
        }
        // The stored copy never contains the code, so SMS logs and reports cannot reveal it
        sms.setSendingMessage(buildMessage(maskedCode(), validitySeconds));
        sms.setSentSuccessfully(sent);
        sms.setSendingFailed(!sent);
        if (sent) {
            sms.setSentAt(now);
        }
        smsFacade.create(sms);

        // Codes are logged only on a developer machine started with -Dhmis.otp.devLogCode=true
        boolean devLog = Boolean.getBoolean(DEV_LOG_PROPERTY);
        if (devLog) {
            LOG.log(Level.WARNING, "[DEV ONLY - hmis.otp.devLogCode] OTP for user {0} ({1}): {2}",
                    new Object[]{user.getName(), purpose, code});
        }

        OtpCode otp = new OtpCode();
        otp.setWebUser(user);
        otp.setPurpose(purpose);
        otp.setChannel(OtpChannel.SMS);
        otp.setIdentifier(sendTo);
        otp.setCodeHash(OtpCodeUtil.hash(signingKey, user.getId(), purpose.name(), code));
        otp.setCreatedAt(now);
        otp.setExpiresAt(expiresAt);
        otp.setRequestIp(truncate(requestIp, 255));
        otp.setUserAgent(truncate(userAgent, 500));
        otp.setSentSuccessfully(sent);
        otp.setSms(sms);
        if (!sent && !devLog) {
            // An undelivered code cannot be used; it still counts toward the limits
            otp.setInvalidatedAt(now);
        }
        otpCodeFacade.create(otp);

        if (!sent && !devLog) {
            return OtpIssueResult.sendFailed();
        }
        return OtpIssueResult.sent(expiresAt, cooldownSeconds);
    }

    /**
     * Checks a code against the user's latest unused code for the purpose.
     * The row is locked for the check, and a correct code is marked used in
     * the same transaction.
     */
    public OtpVerifyResult verifyCode(WebUser user, OtpPurpose purpose, String enteredCode, String signingKey) {
        if (user == null || user.getId() == null || purpose == null) {
            return OtpVerifyResult.of(OtpVerifyResult.Status.NO_ACTIVE_CODE);
        }
        Long id = otpCodeFacade.findLatestActiveId(user.getId(), purpose);
        OtpCode otp = otpCodeFacade.findForUpdate(id);
        if (otp == null || !otp.isActive()) {
            return OtpVerifyResult.of(OtpVerifyResult.Status.NO_ACTIVE_CODE);
        }
        Date now = now();
        if (otp.getExpiresAt() == null || !now.before(otp.getExpiresAt())) {
            otp.setInvalidatedAt(now);
            otpCodeFacade.edit(otp);
            return OtpVerifyResult.of(OtpVerifyResult.Status.EXPIRED);
        }
        if (otp.getAttempts() >= MAX_ATTEMPTS_PER_CODE) {
            otp.setInvalidatedAt(now);
            otpCodeFacade.edit(otp);
            return OtpVerifyResult.of(OtpVerifyResult.Status.TOO_MANY_ATTEMPTS);
        }
        otp.setAttempts(otp.getAttempts() + 1);
        String code = OtpCodeUtil.cleanCode(enteredCode);
        boolean match = code != null
                && OtpCodeUtil.hashesMatch(otp.getCodeHash(), OtpCodeUtil.hash(signingKey, user.getId(), purpose.name(), code));
        if (match) {
            otp.setConsumedAt(now);
            otpCodeFacade.edit(otp);
            return OtpVerifyResult.verified(otp.getIdentifier());
        }
        if (otp.getAttempts() >= MAX_ATTEMPTS_PER_CODE) {
            otp.setInvalidatedAt(now);
        }
        otpCodeFacade.edit(otp);
        return OtpVerifyResult.of(OtpVerifyResult.Status.INVALID);
    }

    /**
     * Closes the user's unused codes for the purpose (e.g. login cancelled).
     */
    public void invalidateActiveCodes(WebUser user, OtpPurpose purpose) {
        if (user == null || user.getId() == null || purpose == null) {
            return;
        }
        Date now = now();
        for (OtpCode old : otpCodeFacade.findActive(user.getId(), purpose)) {
            old.setInvalidatedAt(now);
            otpCodeFacade.edit(old);
        }
    }

    /**
     * True when the number is valid and in an allowed country.
     */
    public boolean isMobileAcceptable(String mobile) {
        return OtpCodeUtil.normalizeMobile(mobile, allowedCountryCodes()) != null;
    }

    /**
     * The number in one format (+94771234567), or null when it is not valid
     * or not from an allowed country; used to compare two numbers.
     */
    public String normalizeMobile(String mobile) {
        return OtpCodeUtil.normalizeMobile(mobile, allowedCountryCodes());
    }

    @Schedule(hour = "3", minute = "20", second = "0", persistent = false)
    public void deleteOldCodes() {
        try {
            Date before = new Date(now().getTime() - KEEP_DAYS * 24L * ONE_HOUR_MS);
            otpCodeFacade.deleteCreatedBefore(before);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Deleting old one-time codes failed; will retry tomorrow", e);
        }
    }

    private String buildMessage(String code, int validitySeconds) {
        String template = configOptionApplicationController.getTwoFactorSmsMessageTemplate();
        long minutes = Math.max(1, Math.round(validitySeconds / 60.0));
        return template.replace("\\n", "\n")
                .replace("{code}", code)
                .replace("{app_name}", configOptionApplicationController.getAuthenticatorAppIssuerName())
                .replace("{minutes}", String.valueOf(minutes));
    }

    private static String maskedCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append('*');
        }
        return sb.toString();
    }

    private List<String> allowedCountryCodes() {
        return OtpCodeUtil.parseCountryCodes(configOptionApplicationController.getTwoFactorSmsAllowedCountryCodes());
    }

    private static long ceilSeconds(long ms) {
        return (ms + 999) / 1000;
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    protected Date now() {
        return new Date();
    }
}
