/*
 * Author : Dr. M H B Ariyaratne
 *
 * MO(Health Information), Department of Health Services, Southern Province
 * and
 * Email : buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.bean.pharmacy.PharmacySaleController;
import com.divudi.data.Privileges;
import com.divudi.ejb.ApplicationEjb;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Logins;
import com.divudi.entity.Person;
import com.divudi.entity.UserPreference;
import com.divudi.entity.WebUser;
import com.divudi.entity.WebUserPrivilege;
import com.divudi.entity.WebUserRole;
import com.divudi.facade.LoginsFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.UserPreferenceFacade;
import com.divudi.facade.WebUserDepartmentFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.facade.WebUserPrivilegeFacade;
import com.divudi.facade.WebUserRoleFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class SessionController implements Serializable, HttpSessionListener {

    @EJB
    private WebUserDepartmentFacade webUserDepartmentFacade;
    @EJB
    UserPreferenceFacade userPreferenceFacade;
    private static final long serialVersionUID = 1L;
    WebUser loggedUser = null;
    private UserPreference institutionPreference;
    UserPreference userPreference;
    boolean logged = false;
    boolean activated = false;
    String primeTheme;
    String defLocale;
    private List<Privileges> privilegeses;
    @Inject
    SecurityController securityController;
    @Inject
    SessionController sessionController;
    Department department;
    Institution institution;
    @EJB
    private CashTransactionBean cashTransactionBean;
    boolean paginator;
    WebUser webUser;

    String billNo;
    String phoneNo;
    UserPreference currentPreference;

    public UserPreference getCurrentPreference() {
        return currentPreference;
    }

    public void setCurrentPreference(UserPreference currentPreference) {
        this.currentPreference = currentPreference;
    }

    public String toManageApplicationPreferences() {
        String jpql;
        Map m = new HashMap();
        jpql = "select p from UserPreference p where p.institution is null and p.department is null and p.webUser is null order by p.id";
        currentPreference = getUserPreferenceFacade().findFirstBySQL(jpql);
        if (currentPreference == null) {
            currentPreference = new UserPreference();

            getUserPreferenceFacade().create(currentPreference);
        }
        currentPreference.setWebUser(null);
        currentPreference.setDepartment(null);
        currentPreference.setInstitution(null);
        return "/admin_mange_application_preferences";
    }

    public String toManageIntitutionPreferences() {
        String jpql;
        Map m = new HashMap();
        jpql = "select p from UserPreference p where p.institution=:ins order by p.id";
        m.put("ins", institution);
        currentPreference = getUserPreferenceFacade().findFirstBySQL(jpql, m);
        if (currentPreference == null) {
            currentPreference = new UserPreference();
            currentPreference.setInstitution(institution);

        }
        currentPreference.setWebUser(null);
        currentPreference.setDepartment(null);

        return "/admin_mange_institutions_preferences";
    }

    public String toManageDepartmentPreferences() {
        String jpql;
        Map m = new HashMap();
        jpql = "select p from UserPreference p where p.department=:dep order by p.id";
        m.put("dep", department);
        currentPreference = getUserPreferenceFacade().findFirstBySQL(jpql,m);
        if (currentPreference == null) {
            currentPreference = new UserPreference();
            currentPreference.setDepartment(department);
        }
        currentPreference.setWebUser(null);
        currentPreference.setInstitution(null);
        return "/admin_mange_department_preferences";
    }

    public void updateUserPreferences() {
        if (institutionPreference != null) {
            if (institutionPreference.getId() == null || institutionPreference.getId() == 0) {
                userPreference.setInstitution(sessionController.getInstitution());
                userPreferenceFacade.create(institutionPreference);
                JsfUtil.addSuccessMessage("Preferences Saved");
            } else {
                userPreferenceFacade.edit(institutionPreference);
                JsfUtil.addSuccessMessage("Preferences Updated");
            }

        }
    }

    public void saveUserPreferences() {
        if (currentPreference != null) {
            if (currentPreference.getId() == null || currentPreference.getId() == 0) {
                userPreferenceFacade.create(currentPreference);
                JsfUtil.addSuccessMessage("Preferences Saved");
            } else {
                userPreferenceFacade.edit(currentPreference);
                JsfUtil.addSuccessMessage("Preferences Updated");
            }

        }
    }

    public void removeInstitutionUserPreferences() {
        if (currentPreference != null) {
            currentPreference.setInstitution(null);
            if (currentPreference.getId() == null || currentPreference.getId() == 0) {
                userPreferenceFacade.create(currentPreference);
                JsfUtil.addSuccessMessage("Preferences Saved");
            } else {
                userPreferenceFacade.edit(currentPreference);
                JsfUtil.addSuccessMessage("Preferences Updated");
            }

        }
    }

    public void removeDepartmentUserPreferences() {
        if (currentPreference != null) {
            currentPreference.setDepartment(null);
            if (currentPreference.getId() == null || currentPreference.getId() == 0) {
                userPreferenceFacade.create(institutionPreference);
                JsfUtil.addSuccessMessage("Preferences Saved");
            } else {
                userPreferenceFacade.edit(currentPreference);
                JsfUtil.addSuccessMessage("Preferences Updated");
            }

        }
    }

    public void makePaginatorTrue() {
        paginator = true;
    }

    public void makePaginatorFalse() {
        paginator = false;

    }

    public Date getCurrentDate() {
        return new Date();
    }

    public void update() {
        getFacede().edit(getLoggedUser());
        getCashTransactionBean().updateDrawers();
    }

    public void updateOtherUser() {
        if (webUser == null) {
            return;
        }
        getFacede().edit(webUser);
        getCashTransactionBean().updateDrawers();
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        if (department != null) {
            institution = department.getInstitution();
        }
        this.department = department;
    }

    public Institution getInstitution() {
        if (institution == null) {
            if (department != null) {
                institution = department.getInstitution();
            } else {
                if (loggedUser != null) {
                    if (loggedUser.getInstitution() != null) {
                        institution = loggedUser.getInstitution();
                    } else if (loggedUser.getDepartment() != null) {
                        institution = loggedUser.getDepartment().getInstitution();
                    }
                }
            }
        }
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public SecurityController getSecurityController() {
        return securityController;
    }

    public void setSecurityController(SecurityController securityController) {
        this.securityController = securityController;
    }

    @EJB
    WebUserFacade uFacade;
    @EJB
    PersonFacade pFacade;
    @EJB
    WebUserRoleFacade rFacade;
    //
    WebUser current;
    String userName;
    String passord;
    String newPassword;
    String newPasswordConfirm;
    String newPersonName;
    String newUserName;
    String newDesignation;
    String newInstitution;
    String newPasswordHint;
    String telNo;
    String email;
    private String displayName;
    WebUserRole role;

    public WebUserRole getRole() {
        return role;
    }

    public void setRole(WebUserRole role) {
        this.role = role;
    }

    public String getTelNo() {
        return telNo;
    }

    public void setTelNo(String telNo) {
        this.telNo = telNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    private WebUserFacade getFacede() {
        return uFacade;
    }

    public String loginAction() {
        if (login()) {
            return "/index.xhtml";
        } else {
            UtilityController.addErrorMessage("Login Failure. Please try again");
            return "";
        }
    }

    private boolean login() {

        getApplicationEjb().recordAppStart();

        if (userName.trim().equals("")) {
            UtilityController.addErrorMessage("Please enter a username");
            return false;
        }

        if (false) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(2015, 05, 17, 23, 59, 59);//2015/june/17/23:00:00
            calendar.set(Calendar.MILLISECOND, 999);

            Date expired = calendar.getTime();
            System.out.println("expired = " + expired);
            Date nowDate = new Date();
            System.out.println("nowDate = " + nowDate);

            if (nowDate.after(expired)) {
                UtilityController.addErrorMessage("Your Application has Expired");
                return false;
            }
        }
        // password
        if (isFirstVisit()) {
            prepareFirstVisit();
            return true;
        } else {
            if (department == null) {
                UtilityController.addErrorMessage("Please select a department");
                return false;
            }
            return checkUsers();
        }
    }

    private void prepareFirstVisit() {
        WebUser user = new WebUser();
        Person person = new Person();
        person.setName(userName);
        pFacade.create(person);

        WebUserRole myRole;
        myRole = new WebUserRole();
        myRole.setName("circular_editor");
        rFacade.create(myRole);

        myRole = new WebUserRole();
        myRole.setName("circular_adder");
        rFacade.create(myRole);

        myRole = new WebUserRole();
        myRole.setName("circular_viewer");
        rFacade.create(myRole);

        myRole = new WebUserRole();
        myRole.setName("admin");
        rFacade.create(myRole);

        user.setName(getSecurityController().encrypt(userName));
        user.setWebUserPassword(getSecurityController().hash(passord));
        user.setWebUserPerson(person);
        user.setActivated(true);
        user.setRole(myRole);
        uFacade.create(user);
    }

    public String registeUser() {
        if (!userNameAvailable(newUserName)) {
            UtilityController.addErrorMessage("User name already exists. Plese enter another user name");
            return "";
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            UtilityController.addErrorMessage("Password and Re-entered password are not matching");
            return "";
        }

        WebUser user = new WebUser();
        Person person = new Person();
        user.setWebUserPerson(person);
        user.setRole(role);

        person.setName(newPersonName);

        pFacade.create(person);
        user.setName(getSecurityController().encrypt(newUserName));
        user.setWebUserPassword(getSecurityController().hash(newPassword));
        user.setWebUserPerson(person);
        user.setTelNo(telNo);
        user.setEmail(email);
        user.setActivated(Boolean.TRUE);

        uFacade.create(user);
        UtilityController.addSuccessMessage("New User Registered.");
        return "";
    }

    public void changePassword() {
        WebUser user = getLoggedUser();
        if (!getSecurityController().matchPassword(passord, user.getWebUserPassword())) {
            UtilityController.addErrorMessage("The old password you entered is incorrect");
            return;
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            UtilityController.addErrorMessage("Password and Re-entered password are not maching");
            return;
        }

        user.setWebUserPassword(getSecurityController().hash(newPassword));
        uFacade.edit(user);
        //
        UtilityController.addSuccessMessage("Password changed");

    }

    public void changeCurrentUserPassword() {
        if (getCurrent() == null) {
            UtilityController.addErrorMessage("Select a User");
            return;
        }
        WebUser user = getCurrent();

        if (!newPassword.equals(newPasswordConfirm)) {
            UtilityController.addErrorMessage("Password and Re-entered password are not maching");
            return;
        }

        user.setWebUserPassword(getSecurityController().hash(newPassword));
        uFacade.edit(user);
        UtilityController.addSuccessMessage("Password changed");
    }

    public Boolean userNameAvailable(String userName) {
        Boolean available = true;
        List<WebUser> allUsers = getFacede().findAll();
        for (WebUser w : allUsers) {
            if (userName.toLowerCase().equals(getSecurityController().decrypt(w.getName()).toLowerCase())) {
                available = false;
            }
        }
        return available;
    }

    private boolean isFirstVisit() {
        if (getFacede().count() <= 0) {
            UtilityController.addSuccessMessage("First Visit");
            return true;
        } else {
//            UtilityController.addSuccessMessage("Welcome back");
            return false;
        }

    }

    public boolean isFirstLogin() {
        if (getFacede().count() <= 1) {
            return true;
        } else {
            return false;
        }

    }

    private boolean checkUsers() {
        String temSQL;
        temSQL = "SELECT u FROM WebUser u WHERE u.retired = false";
        List<WebUser> allUsers = getFacede().findBySQL(temSQL);
        for (WebUser u : allUsers) {
            if (getSecurityController().decrypt(u.getName()).equalsIgnoreCase(userName)) {
                if (getSecurityController().matchPassword(passord, u.getWebUserPassword())) {
                    if (!canLogToDept(u, department)) {
                        UtilityController.addErrorMessage("No privilage to Login This Department");
                        return false;
                    }
                    if (getApplicationController().isLogged(u) != null) {
                        UtilityController.addErrorMessage("This user already logged. Other instances will be logged out now.");
                    }

                    u.setDepartment(department);
                    u.setInstitution(institution);

                    getFacede().edit(u);

                    setLoggedUser(u);
                    setLogged(Boolean.TRUE);
                    setActivated(u.isActivated());
                    setRole(u.getRole());

                    String sql;

                    UserPreference uf;
                    sql = "select p from UserPreference p where p.webUser=:u ";
                    Map m = new HashMap();
                    m.put("u", u);
                    uf = getUserPreferenceFacade().findFirstBySQL(sql, m);
                    if (uf == null) {
                        uf = new UserPreference();
                        uf.setWebUser(u);
                        getUserPreferenceFacade().create(uf);
                    }
                    setUserPreference(uf);

                    UserPreference insPre;

                    sql = "select p from UserPreference p where p.department =:dep order by p.id";
                    m = new HashMap();
                    m.put("dep", department);

                    insPre = getUserPreferenceFacade().findFirstBySQL(sql, m);
                    System.out.println("1");
                    System.out.println("sql = " + sql);
                    System.out.println("m = " + m);
                    System.out.println("insPre = " + insPre);

                    if (insPre == null) {

                        sql = "select p from UserPreference p where p.institution =:ins order by p.id ";
                        m = new HashMap();
                        m.put("ins", institution);
                        insPre = getUserPreferenceFacade().findFirstBySQL(sql, m);
                        System.out.println("2");
                        System.out.println("sql = " + sql);
                        System.out.println("m = " + m);
                        System.out.println("insPre = " + insPre);

                        if (insPre == null) {
                            sql = "select p from UserPreference p where p.institution is null and p.department is null and p.webUser is null order by p.id";
                            insPre = getUserPreferenceFacade().findFirstBySQL(sql);
                            System.out.println("3");
                            System.out.println("sql = " + sql);
                            System.out.println("m = " + m);
                            System.out.println("insPre = " + insPre);

                        }

                        if (insPre == null) {
                            insPre = new UserPreference();
                            insPre.setWebUser(null);
                            insPre.setDepartment(null);
                            insPre.setInstitution(null);
                            getUserPreferenceFacade().create(insPre);
                        }
                    }

                    setInstitutionPreference(insPre);

                    recordLogin();

                    UtilityController.addSuccessMessage("Logged successfully");
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canLogToDept(WebUser e, Department d) {
        String sql;
        sql = "select wd from WebUserDepartment wd where wd.retired=false and wd.webUser.id=" + e.getId() + " and wd.department.id = " + d.getId();
        return !getWebUserDepartmentFacade().findBySQL(sql).isEmpty();
    }

    @Inject
    ApplicationController applicationController;

    @EJB
    ApplicationEjb applicationEjb;

    public ApplicationEjb getApplicationEjb() {
        return applicationEjb;
    }

    public void setApplicationEjb(ApplicationEjb applicationEjb) {
        this.applicationEjb = applicationEjb;
    }

    public ApplicationController getApplicationController() {
        return applicationController;
    }

    public void setApplicationController(ApplicationController applicationController) {
        this.applicationController = applicationController;
    }

    @Inject
    private PharmacySaleController pharmacySaleController;

    public void logout() {
        userPrivilages = null;
        recordLogout();
        setLoggedUser(null);
        setLogged(false);
        setActivated(false);
        getPharmacySaleController().makeNull();

    }

    public WebUser getCurrent() {
        if (current == null) {
            current = new WebUser();
            Person p = new Person();
            current.setWebUserPerson(p);
        }
        return current;
    }

    public void setCurrent(WebUser current) {
        this.current = current;
    }

    public WebUserFacade getEjbFacade() {
        return uFacade;
    }

    public void setEjbFacade(WebUserFacade ejbFacade) {
        this.uFacade = ejbFacade;
    }

    public String getPassord() {
        return passord;
    }

    public void setPassord(String passord) {
        this.passord = passord;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNewDesignation() {
        return newDesignation;
    }

    public void setNewDesignation(String newDesignation) {
        this.newDesignation = newDesignation;
    }

    public String getNewInstitution() {
        return newInstitution;
    }

    public void setNewInstitution(String newInstitution) {
        this.newInstitution = newInstitution;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPasswordConfirm() {
        return newPasswordConfirm;
    }

    public void setNewPasswordConfirm(String newPasswordConfirm) {
        this.newPasswordConfirm = newPasswordConfirm;
    }

    public String getNewPasswordHint() {
        return newPasswordHint;
    }

    public void setNewPasswordHint(String newPasswordHint) {
        this.newPasswordHint = newPasswordHint;
    }

    public String getNewPersonName() {
        return newPersonName;
    }

    public void setNewPersonName(String newPersonName) {
        this.newPersonName = newPersonName;
    }

    public PersonFacade getpFacade() {
        return pFacade;
    }

    public void setpFacade(PersonFacade pFacade) {
        this.pFacade = pFacade;
    }

    public WebUserFacade getuFacade() {
        return uFacade;
    }

    public void setuFacade(WebUserFacade uFacade) {
        this.uFacade = uFacade;
    }

    public String getNewUserName() {
        return newUserName;
    }

    public void setNewUserName(String newUserName) {
        this.newUserName = newUserName;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
        setLogged(activated);
    }

    public boolean isLogged() {
        return logged;
    }

    public void setLogged(boolean logged) {
        this.logged = logged;
    }

    public WebUserRoleFacade getrFacade() {
        return rFacade;
    }

    public void setrFacade(WebUserRoleFacade rFacade) {
        this.rFacade = rFacade;
    }

    public String getDisplayName() {
        return getSecurityController().decrypt(getLoggedUser().getName());
    }

    /**
     * Creates a new instance of SessionController
     */
    public SessionController() {
        ////System.out.println("session started");
    }

    public String getDefLocale() {
        defLocale = "en";
        if (getLoggedUser() != null) {
            if (getLoggedUser().getDefLocale() != null) {
                if (!getLoggedUser().getDefLocale().equals("")) {
                    return getLoggedUser().getDefLocale();
                }
            }
        }
        return defLocale;
    }

    public void setDefLocale(String defLocale) {
        this.defLocale = defLocale;
    }

    public String getPrimeTheme() {
        if (primeTheme == null || primeTheme.equals("")) {
            primeTheme = "hot-sneaks";
        }
        if (getLoggedUser() != null) {
            if (getLoggedUser().getPrimeTheme() != null) {
                if (!getLoggedUser().getPrimeTheme().equals("")) {
                    return getLoggedUser().getPrimeTheme();
                }
            }
        }
        return primeTheme;
    }

    public void setPrimeTheme(String primeTheme) {
        this.primeTheme = primeTheme;
    }

    /**
     *
     * @return
     */
    public WebUser getLoggedUser() {
        return loggedUser;
    }

    /**
     *
     * @param loggedUser
     */
    public void setLoggedUser(WebUser loggedUser) {
        this.loggedUser = loggedUser;
    }

    public List<Privileges> getPrivilegeses() {
        if (privilegeses == null || privilegeses.isEmpty()) {
            privilegeses.addAll(Arrays.asList(Privileges.values()));
        }
        return privilegeses;
    }
    private List<WebUserPrivilege> userPrivilages;
    @EJB
    private WebUserPrivilegeFacade webUserPrivilegeFacade;

    public List<WebUserPrivilege> getUserPrivileges() {
        if (userPrivilages == null) {
            String sql;
            sql = "select w from WebUserPrivilege w where w.retired=false and w.webUser.id = " + getLoggedUser().getId();
            ////System.out.println("5");
            userPrivilages = getWebUserPrivilegeFacade().findBySQL(sql);
        }
        if (userPrivilages == null) {
            userPrivilages = new ArrayList<>();
        }
        return userPrivilages;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public void setPrivilegeses(List<Privileges> privilegeses) {
        this.privilegeses = privilegeses;
    }

    public WebUserDepartmentFacade getWebUserDepartmentFacade() {
        return webUserDepartmentFacade;
    }

    public void setWebUserDepartmentFacade(WebUserDepartmentFacade webUserDepartmentFacade) {
        this.webUserDepartmentFacade = webUserDepartmentFacade;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public WebUserPrivilegeFacade getWebUserPrivilegeFacade() {
        return webUserPrivilegeFacade;
    }

    public void setWebUserPrivilegeFacade(WebUserPrivilegeFacade webUserPrivilegeFacade) {
        this.webUserPrivilegeFacade = webUserPrivilegeFacade;
    }

    public void setUserPrivilages(List<WebUserPrivilege> userPrivilages) {
        this.userPrivilages = userPrivilages;
    }
    Logins thisLogin;

    public Logins getThisLogin() {
        return thisLogin;
    }

    public void setThisLogin(Logins thisLogin) {
        this.thisLogin = thisLogin;
    }
    @EJB
    LoginsFacade loginsFacade;

    public LoginsFacade getLoginsFacade() {
        return loginsFacade;
    }

    public void setLoginsFacade(LoginsFacade loginsFacade) {
        this.loginsFacade = loginsFacade;
    }

    private void recordLogin() {
        if (thisLogin != null) {
            thisLogin.setLogoutAt(Calendar.getInstance().getTime());
            if (thisLogin.getId() != null && thisLogin.getId() != 0) {
                getLoginsFacade().edit(thisLogin);
            }
        }

        thisLogin = new Logins();
        thisLogin.setLogedAt(Calendar.getInstance().getTime());
        thisLogin.setInstitution(institution);
        thisLogin.setDepartment(department);
        thisLogin.setWebUser(loggedUser);

        HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();

        String ip = httpServletRequest.getRemoteAddr();
        String host = httpServletRequest.getRemoteHost();

        thisLogin.setIpaddress(ip);
        thisLogin.setComputerName(host);

        getLoginsFacade().create(thisLogin);
        getApplicationController().addToLoggins(this);
    }

    @PreDestroy
    private void recordLogout() {
        ////System.out.println("session distroyed " + thisLogin);
        if (thisLogin == null) {
            return;
        }
        applicationController.removeLoggins(this);
        thisLogin.setLogoutAt(Calendar.getInstance().getTime());
        getLoginsFacade().edit(thisLogin);
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        ////System.out.println("starting session");
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        ////System.out.println("recording logout as session is distroid");
        recordLogout();
    }

    public PharmacySaleController getPharmacySaleController() {
        return pharmacySaleController;
    }

    public void setPharmacySaleController(PharmacySaleController pharmacySaleController) {
        this.pharmacySaleController = pharmacySaleController;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public UserPreference getInstitutionPreference() {
        return institutionPreference;
    }

    public void setInstitutionPreference(UserPreference institutionPreference) {
        this.institutionPreference = institutionPreference;
    }

    public UserPreferenceFacade getUserPreferenceFacade() {
        return userPreferenceFacade;
    }

    public void setUserPreferenceFacade(UserPreferenceFacade userPreferenceFacade) {
        this.userPreferenceFacade = userPreferenceFacade;
    }

    public boolean getPaginator() {
        return paginator;
    }

    public void setPaginator(boolean paginator) {
        this.paginator = paginator;
    }

    public UserPreference getUserPreference() {
        return userPreference;
    }

    public void setUserPreference(UserPreference userPreference) {
        this.userPreference = userPreference;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

}
