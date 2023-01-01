/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.ejb.EmailManagerEjb;
import com.divudi.entity.AppEmail;
import com.divudi.entity.Institution;
import com.divudi.entity.Logins;
import com.divudi.entity.Sms;
import com.divudi.entity.UserPreference;
import com.divudi.entity.WebUser;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.UserPreferenceFacade;
import com.divudi.facade.util.JsfUtil;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 *
 * @author Buddhika
 */
@Named
@ApplicationScoped
public class ApplicationController {

    @EJB
    private PatientFacade patientFacade;
    @EJB
    private EmailManagerEjb eejb;
    @EJB
    UserPreferenceFacade userPreferenceFacade;

    private UserPreference applicationPreference;

    String personalHealthNumber;
    Long personalHealthNumberCount;

    List<InstitutionLastPhn> insPhns;

    Date startTime;
    Date storesExpiery;

    private List<AppEmail> mailsToSent;
    private List<Sms> smsToSent;

    private String subject;
    private String body;

    private boolean hasAwebsiteAsFrontEnd = false;

    private void loadApplicationPreferances() {
        String sql = "select p from UserPreference p where p.institution is null and p.department is null and p.webUser is null order by p.id desc";
        applicationPreference = userPreferenceFacade.findFirstByJpql(sql);
        if (applicationPreference == null) {
            applicationPreference = new UserPreference();
            applicationPreference.setWebUser(null);
            applicationPreference.setDepartment(null);
            applicationPreference.setInstitution(null);
            userPreferenceFacade.create(applicationPreference);
        }
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @PostConstruct
    public void recordStart() {
        startTime = Calendar.getInstance().getTime();
//        if (sessionControllers == null) {
//            sessionControllers = new ArrayList<>();
//        }
    }

    List<Logins> loggins;

    public List<Logins> getLoggins() {
        if (loggins == null) {
            loggins = new ArrayList<>();
        }
        return loggins;
    }

    public void setLoggins(List<Logins> loggins) {
        this.loggins = loggins;
    }

    public Logins isLogged(WebUser u) {
        Logins tl = null;
        for (Logins l : getLoggins()) {
            if (l.getWebUser().equals(u)) {
                tl = l;
            }
        }
        return tl;
    }

    public void addToLoggins(SessionController sc) {
        Logins login = sc.getThisLogin();
        if (loggins == null) {
            loggins = new ArrayList<>();
        }
        try {
            loggins.add(login);
//            for (SessionController s : getSessionControllers()) {
//                if (s.getLoggedUser().equals(login.getWebUser())) {
//                    //////// // System.out.println("making log out");
//                    s.logout();
//                }
//            }
//            getSessionControllers().add(sc);
        } catch (Exception e) {
        }
    }

    public void removeLoggins(SessionController sc) {
        Logins login = sc.getThisLogin();
        //////// // System.out.println("sessions logged before removing is " + getLoggins().size());
        loggins.remove(login);
//        sessionControllers.remove(sc);
    }

    /**
     * Creates a new instance of ApplicationController
     */
    public ApplicationController() {
    }

    public Date getStoresExpiery() {
        if (storesExpiery == null) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, 2020);
            c.set(Calendar.MONTH, 0);
            c.set(Calendar.DATE, 1);
            storesExpiery = c.getTime();
        }
        return storesExpiery;
    }

    public void setStoresExpiery(Date storesExpiery) {
        this.storesExpiery = storesExpiery;
    }

    public String createNewPersonalHealthNumber(Institution ins) {
        InstitutionLastPhn iln = null;
        for (InstitutionLastPhn p : getInsPhns()) {
            if (p.institution.equals(ins)) {
                iln = p;
            }
        }
        if (iln == null) {
            iln = new InstitutionLastPhn();
            String j = "select count(p) from Patient p "
                    + " where p.createdInstitution=:ins ";
            Map m = new HashMap();
            m.put("ins", ins);
            iln.institution = ins;
            iln.patientCount = patientFacade.countBySql(j, m);
        }
        iln.patientCount++;
        String poi = ins.getPointOfIssueNo();
        String num = String.format("%05d", iln.patientCount);
        String checkDigit = calculateCheckDigit(poi + num);
        String phn = poi + num + checkDigit;
        return phn;
    }

    /**
     * Checks if the card is valid
     *
     * @param card {@link String} card number
     * @return result {@link boolean} true of false
     */
    public static boolean luhnCheck(String card) {
        if (card == null) {
            return false;
        }
        char checkDigit = card.charAt(card.length() - 1);
        String digit = calculateCheckDigit(card.substring(0, card.length() - 1));
        return checkDigit == digit.charAt(0);
    }

    /**
     * Calculates the last digits for the card number received as parameter
     *
     * @param card {@link String} number
     * @return {@link String} the check digit
     */
    public static String calculateCheckDigit(String card) {
        if (card == null) {
            return null;
        }
        String digit;
        /* convert to array of int for simplicity */
        int[] digits = new int[card.length()];
        for (int i = 0; i < card.length(); i++) {
            digits[i] = Character.getNumericValue(card.charAt(i));
        }

        /* double every other starting from right - jumping from 2 in 2 */
        for (int i = digits.length - 1; i >= 0; i -= 2) {
            digits[i] += digits[i];

            /* taking the sum of digits grater than 10 - simple trick by substract 9 */
            if (digits[i] >= 10) {
                digits[i] = digits[i] - 9;
            }
        }
        int sum = 0;
        for (int i = 0; i < digits.length; i++) {
            sum += digits[i];
        }
        /* multiply by 9 step */
        sum = sum * 9;

        /* convert to string to be easier to take the last digit */
        digit = sum + "";
        return digit.substring(digit.length() - 1);
    }

    public List<AppEmail> getMailsToSent() {
        if (mailsToSent == null) {
            mailsToSent = new ArrayList<>();
        }
        return mailsToSent;
    }

    public void setMailsToSent(List<AppEmail> mailsToSent) {
        this.mailsToSent = mailsToSent;
    }

    public List<Sms> getSmsToSent() {
        if (smsToSent == null) {
            smsToSent = new ArrayList<>();
        }
        return smsToSent;
    }

    public void setSmsToSent(List<Sms> smsToSent) {
        this.smsToSent = smsToSent;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public EmailManagerEjb getEejb() {
        return eejb;
    }

    public void setEejb(EmailManagerEjb eejb) {
        this.eejb = eejb;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isHasAwebsiteAsFrontEnd() {
        boolean w = getApplicationPreference().isHasAwebsiteAsFrontEnd();
        hasAwebsiteAsFrontEnd=w;
        return hasAwebsiteAsFrontEnd;
    }

    public void setHasAwebsiteAsFrontEnd(boolean hasAwebsiteAsFrontEnd) {
        getApplicationPreference().setHasAwebsiteAsFrontEnd(hasAwebsiteAsFrontEnd);
        this.hasAwebsiteAsFrontEnd = hasAwebsiteAsFrontEnd;
    }

    public UserPreference getApplicationPreference() {
        if (applicationPreference == null) {
            loadApplicationPreferances();
        }
        return applicationPreference;
    }

    public void setApplicationPreference(UserPreference applicationPreference) {
        this.applicationPreference = applicationPreference;
    }

    class InstitutionLastPhn {

        Institution institution;
        Long patientCount;
    }

    public String getPersonalHealthNumber() {
        return personalHealthNumber;
    }

    public void setPersonalHealthNumber(String personalHealthNumber) {
        this.personalHealthNumber = personalHealthNumber;
    }

    public Long getPersonalHealthNumberCount() {
        return personalHealthNumberCount;
    }

    public void setPersonalHealthNumberCount(Long personalHealthNumberCount) {
        this.personalHealthNumberCount = personalHealthNumberCount;
    }

    public List<InstitutionLastPhn> getInsPhns() {
        if (insPhns == null) {
            insPhns = new ArrayList<>();
        }
        return insPhns;
    }

    public void setInsPhns(List<InstitutionLastPhn> insPhns) {
        this.insPhns = insPhns;
    }

}
