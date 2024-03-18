/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.ejb.EmailManagerEjb;
import com.divudi.entity.AppEmail;
import com.divudi.entity.Institution;
import com.divudi.entity.Logins;
import com.divudi.entity.Patient;
import com.divudi.entity.Sms;
import com.divudi.entity.UserPreference;
import com.divudi.entity.WebUser;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.UserPreferenceFacade;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private UserPreferenceFacade userPreferenceFacade;

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
    private String themeName;

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
        if (u == null) {
            return null;
        }
        Logins tl = null;
        for (Logins l : getLoggins()) {
            if (l.getWebUser() != null) {
                if (l.getWebUser().equals(u)) {
                    tl = l;
                }
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
        try {
            Logins login = sc.getThisLogin();
            if(login==null){
                System.err.println("No Login has create for this session. Need to debug the Login session recording");
                return;
            }
            loggins.remove(login);
        } catch (Exception e) {
            System.err.println("e = " + e);
        }
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

    public String createNewPersonalHealthNumber(Institution ins, boolean old) {
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
            iln.patientCount = patientFacade.countByJpql(j, m);
        }
        iln.patientCount++;
        String poi = ins.getPointOfIssueNo();
        String num = String.format("%05d", iln.patientCount);
        String checkDigit = calculateCheckDigit(poi + num);
        String phn = poi + num + checkDigit;
        return phn;
    }

    public String createNewPersonalHealthNumber(Institution ins) {
        if (ins == null) {
            return null;
        }
        String alpha = "BCDFGHJKMPQRTVWXY";
        String numeric = "23456789";
        String alphanum = alpha + numeric;
        Random random = new Random();
        int length = 6;
        String poi = ins.getPointOfIssueNo();

        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                int index = random.nextInt(alphanum.length());
                char randomChar = alphanum.charAt(index);
                sb.append(randomChar);
            }
            String randomString = sb.toString();
            String checkDigit = calculateCheckDigit(poi + randomString);
            String phn = poi + randomString + checkDigit;

            if (!thePhnNumberAlreadyExsists(phn)) {
                return phn;
            }
        }
        return null;
    }

    public boolean thePhnNumberAlreadyExsists(String phn) {
        String jpql = "select p "
                + " from Patient p "
                + " where p.retired=:ret "
                + " and p.phn=:phn";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("phn", phn);
        Patient p = patientFacade.findFirstByJpql(jpql, m);
        if (p != null) {
            return true;
        }
        return false;
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
        hasAwebsiteAsFrontEnd = w;
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

    public UserPreferenceFacade getUserPreferenceFacade() {
        return userPreferenceFacade;
    }

    public void setUserPreferenceFacade(UserPreferenceFacade userPreferenceFacade) {
        this.userPreferenceFacade = userPreferenceFacade;
    }

    public String getThemeName() {
        String w = getApplicationPreference().getThemeName();
        themeName = w;
        return themeName;
    }

    public void setThemeName(String themeName) {
        getApplicationPreference().setThemeName(themeName);
        this.themeName = themeName;
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
