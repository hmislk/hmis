/*
 * Author : Dr. M H B Ariyaratne
 *
 * Acting Consultant (Health Informatics), Department of Health Services, Southern Province
 * (94) 71 5812399
 * Email : buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.bean.pharmacy.PharmacySaleController;
import com.divudi.data.DepartmentType;
import com.divudi.data.InstitutionType;
import com.divudi.data.Privileges;
import com.divudi.ejb.ApplicationEjb;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.entity.Bill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Logins;
import com.divudi.entity.Person;
import com.divudi.entity.UserPreference;
import com.divudi.entity.WebUser;
import com.divudi.entity.WebUserDashboard;
import com.divudi.entity.WebUserDepartment;
import com.divudi.entity.WebUserPrivilege;
import com.divudi.entity.WebUserRole;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.LoginsFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.UserPreferenceFacade;
import com.divudi.facade.WebUserDashboardFacade;
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

import org.primefaces.model.DashboardColumn;
import org.primefaces.model.DashboardModel;
import org.primefaces.model.DefaultDashboardColumn;
import org.primefaces.model.DefaultDashboardModel;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class SessionController implements Serializable, HttpSessionListener {

    /**
     * EJBs
     */
    @EJB
    private WebUserDepartmentFacade webUserDepartmentFacade;
    @EJB
    UserPreferenceFacade userPreferenceFacade;
    @EJB
    private CashTransactionBean cashTransactionBean;
    @EJB
    DepartmentFacade departmentFacade;
    @EJB
    ApplicationEjb applicationEjb;
    @EJB
    PersonFacade personFacade;
    @EJB
    WebUserFacade webUserFacade;
    @EJB
    WebUserDashboardFacade webUserDashboardFacade;
    /**
     * Controllers
     */
    @Inject
    SecurityController securityController;
    @Inject
    ApplicationController applicationController;
    @Inject
    SearchController searchController;
    @Inject
    InstitutionController institutionController;
    @Inject
    DepartmentController departmentController;
    @Inject
    WebUserController webUserController;
    @Inject
    PersonController personController;
    /**
     * Properties
     */
    private static final long serialVersionUID = 1L;
    WebUser loggedUser = null;
    private UserPreference loggedPreference;
    private UserPreference applicationPreference;
    private UserPreference institutionPreference;
    private UserPreference departmentPreference;
    UserPreference userPreference;
    boolean logged = false;
    boolean activated = false;
    private String primeTheme;
    String defLocale;
    private List<Privileges> privilegeses;
    Department department;
    List<Department> departments;
    Institution institution;
    private List<WebUserDashboard> dashboards;
    boolean paginator;
    WebUser webUser;
    String billNo;
    String phoneNo;
    UserPreference currentPreference;
    Bill bill;
    private DashboardModel dashboardModel;
    String loginRequestResponse;
    private Boolean firstLogin;

    private boolean websiteUserGoingToLog = false;

    private String institutionName;
    private String departmentName;
    private String adminName;

    public String createFirstLogin() {
        Institution ins = new Institution();
        ins.setName(institutionName);
        ins.setInstitutionCode(institutionName);
        ins.setCreatedAt(new Date());
        ins.setInstitutionType(InstitutionType.Company);
        institutionController.save(ins);
        Department dep = new Department();
        dep.setInstitution(ins);
        dep.setName(departmentName);
        dep.setPrintingName(departmentName);
        dep.setDepartmentCode(departmentName);
        departmentController.save(dep);

        Person p = new Person();
        p.setName(userName);
        p.setCreatedAt(new Date());
        personController.save(p);

        WebUser wu = new WebUser();
        wu.setWebUserPerson(p);
        wu.setInstitution(ins);
        wu.setDepartment(dep);
        wu.setCreatedAt(new Date());
        wu.setActivated(true);
        wu.setActivatedAt( new Date());
        wu.setName(userName);
        wu.setWebUserPassword(getSecurityController().hash(passord));
        webUserController.save(wu);

        for (Privileges pv : Privileges.values()) {
            WebUserPrivilege wup = new WebUserPrivilege();
            wup.setWebUser(wu);
            wup.setPrivilege(pv);
            wup.setCreatedAt(new Date());
            webUserPrivilegeFacade.create(wup);
        }

        WebUserDepartment wud = new WebUserDepartment();
        wud.setCreatedAt(new Date());
        wud.setDepartment(dep);
        wud.setWebUser(wu);
        webUserDepartmentFacade.create(wud);

        firstLogin = null;

        return "/index";

    }

    public String toLoginFromWeb() {
        websiteUserGoingToLog = true;
        return "";
    }

    public UserPreference getCurrentPreference() {
        return currentPreference;
    }

    public void setCurrentPreference(UserPreference currentPreference) {
        this.currentPreference = currentPreference;
    }

    public String toManageApplicationPreferences() {
        String jpql;
        Map m = new HashMap();
        jpql = "select p from UserPreference p where p.institution is null and p.department is null and p.webUser is null order by p.id desc";
        currentPreference = getUserPreferenceFacade().findFirstByJpql(jpql);
        if (currentPreference == null) {
            currentPreference = new UserPreference();

            getUserPreferenceFacade().create(currentPreference);
        }
        currentPreference.setWebUser(null);
        currentPreference.setDepartment(null);
        currentPreference.setInstitution(null);
        return "/admin_mange_application_preferences";
    }

    public String toPublicLogin() {
        return "/public_login";
    }

    public String toManageIntitutionPreferences() {
        String jpql;
        Map m = new HashMap();
        jpql = "select p from UserPreference p where p.institution=:ins order by p.id desc";
        m.put("ins", loggedUser.getInstitution());
        currentPreference = getUserPreferenceFacade().findFirstBySQL(jpql, m);
        if (currentPreference == null) {
            currentPreference = new UserPreference();
            currentPreference.setInstitution(loggedUser.getInstitution());

        }
        currentPreference.setWebUser(null);
        currentPreference.setDepartment(null);

        return "/admin_mange_institutions_preferences";
    }

    public String toManageDepartmentPreferences() {
        String jpql;
        Map m = new HashMap();
        jpql = "select p from UserPreference p where p.department=:dep order by p.id desc";
        m.put("dep", loggedUser.getDepartment());
        currentPreference = getUserPreferenceFacade().findFirstBySQL(jpql, m);
        if (currentPreference == null) {
            currentPreference = new UserPreference();
            currentPreference.setDepartment(loggedUser.getDepartment());
        }
        currentPreference.setWebUser(null);
        currentPreference.setInstitution(null);
        return "/admin_mange_department_preferences";
    }

    public void updateUserPreferences() {
        if (loggedPreference != null) {
            if (loggedPreference.getId() == null || loggedPreference.getId() == 0) {
                userPreference.setInstitution(institution);
                userPreferenceFacade.create(loggedPreference);
                JsfUtil.addSuccessMessage("Preferences Saved");
            } else {
                userPreferenceFacade.edit(loggedPreference);
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
                userPreferenceFacade.create(loggedPreference);
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

    public String loginActionWithoutDepartment() {
        department = null;
        institution = null;
        boolean l = checkUsersWithoutDepartment();
        System.out.println("l = " + l);
        if (l) {
            return "/index.xhtml";
        } else {
            UtilityController.addErrorMessage("Invalid User! Login Failure. Please try again");
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
            Date nowDate = new Date();

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

        user.setName(userName);
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
        user.setName(newUserName);
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
            if (userName.toLowerCase().equals(w.getName().toLowerCase())) {
                available = false;
            }
        }
        return available;
    }

    private boolean isFirstVisit() {
        String j = "Select w from WebUser w order by w.id";
        WebUser ws = getFacede().findFirstByJpql(j);
        if (ws == null) {
            UtilityController.addSuccessMessage("First Visit");
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
            if ((u.getName()).equalsIgnoreCase(userName)) {
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
                    sql = "select p from UserPreference p where p.webUser=:u order by p.id desc";
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

                    sql = "select p from UserPreference p where p.department =:dep order by p.id desc";
                    m = new HashMap();
                    m.put("dep", department);

                    insPre = getUserPreferenceFacade().findFirstBySQL(sql, m);

                    if (insPre == null) {

                        sql = "select p from UserPreference p where p.institution =:ins order by p.id desc";
                        m = new HashMap();
                        m.put("ins", institution);
                        insPre = getUserPreferenceFacade().findFirstBySQL(sql, m);

                        if (insPre == null) {
                            sql = "select p from UserPreference p where p.institution is null and p.department is null and p.webUser is null order by p.id desc";
                            insPre = getUserPreferenceFacade().findFirstByJpql(sql);

                        }

                        if (insPre == null) {
                            insPre = new UserPreference();
                            insPre.setWebUser(null);
                            insPre.setDepartment(null);
                            insPre.setInstitution(null);
                            getUserPreferenceFacade().create(insPre);
                        }
                    }

                    setLoggedPreference(insPre);

                    recordLogin();

                    UtilityController.addSuccessMessage("Logged successfully");
                    return true;
                }
            }
        }
        return false;
    }

    public void decryptAllUsers() {
        String temSQL;
        temSQL = "SELECT u FROM WebUser u";
        List<WebUser> allUsers = getFacede().findBySQL(temSQL);
        int i = 1;
        for (WebUser u : allUsers) {
            try {
                u.setName(getSecurityController().decrypt(u.getName()));
            } catch (Exception e) {
                if (u.getName().trim().equals("")) {
                    u.setName("" + i);
                }
            }
            if (u.getName() != null && !u.getName().trim().equals("")) {
                getFacede().edit(u);
            }
            i++;
        }
    }

    public boolean loginForRequests() {
        return loginForRequests(userName, passord);
    }

    public boolean loginForRequestsForDoctors() {
        return loginForRequestsForDoctors(userName, passord);
    }

    public boolean loginForRequests(String temUserName, String temPassword) {
        logged = false;
        loggedUser = null;
        if (temUserName == null) {
            return false;
        }
        if (temPassword == null) {
            return false;
        }
        String temSQL;
        loginRequestResponse = "#{";
        temSQL = "SELECT u FROM WebUser u WHERE u.retired = false and lower(u.name)=:n order by u.id desc";
        Map m = new HashMap();

        m.put("n", temUserName.trim().toLowerCase());
        WebUser u = getFacede().findFirstBySQL(temSQL, m);

        //// // System.out.println("temSQL = " + temSQL);
        if (u == null) {
            return false;
        }

        if (getSecurityController().matchPassword(temPassword, u.getWebUserPassword())) {

            setLoggedUser(u);
            setLogged(Boolean.TRUE);
            setActivated(u.isActivated());
            setRole(u.getRole());

            String sql;

            loginRequestResponse += "Login=1|";
            loginRequestResponse += "User=" + u.getName() + "|";
            loginRequestResponse += "UserId=" + u.getId() + "|";
            loginRequestResponse += "}";
            return true;
        }
        loginRequestResponse += "Login=0|}";
        return false;
    }

    public boolean loginForRequestsForDoctors(String temUserName, String temPassword) {
        logged = false;
        loggedUser = null;
        if (temUserName == null) {
            return false;
        }
        if (temPassword == null) {
            return false;
        }
        String temSQL;
        loginRequestResponse = "#{";
        temSQL = "SELECT u FROM WebUser u WHERE u.retired = false and lower(u.name)=:n order by u.id desc";
        Map m = new HashMap();

        m.put("n", temUserName.trim().toLowerCase());
        WebUser u = getFacede().findFirstBySQL(temSQL, m);

        if (u == null) {
            return false;
        }

        if (getSecurityController().matchPassword(temPassword, u.getWebUserPassword())) {
            setLoggedUser(u);
            setLogged(Boolean.TRUE);
            setActivated(u.isActivated());
            setRole(u.getRole());
            loginRequestResponse += "Login=1|";
            loginRequestResponse += "User=" + u.getName() + "|";
            loginRequestResponse += "UserId=" + u.getId() + "|";
            loginRequestResponse += "}";
            return true;
        }
        loginRequestResponse += "Login=0|}";
        return false;
    }

    private boolean checkUsersWithoutDepartment() {
        String temSQL;
        temSQL = "SELECT u FROM WebUser u WHERE u.retired = false and lower(u.name)=:un";
        Map m = new HashMap();
        m.put("un", userName.toLowerCase());
        List<WebUser> allUsers = getFacede().findBySQL(temSQL, m);
        for (WebUser u : allUsers) {
            if ((u.getName()).equalsIgnoreCase(userName)) {
                if (SecurityController.matchPassword(passord, u.getWebUserPassword())) {
                    departments = listLoggableDepts(u);
                    if (departments.isEmpty()) {
                        UtilityController.addErrorMessage("This user has no privilage to login to any Department. Please conact system administrator.");
                        return false;
                    }
                    boolean f = false;
                    for (Department d : departments) {
                        if (d.equals(u.getDepartment())) {
                            f = true;
                        }
                    }

                    if (f) {
                        List<Department> tds = new ArrayList<>();
                        tds.add(u.getDepartment());
                        for (Department d : departments) {
                            if (!d.equals(u.getDepartment())) {
                                tds.add(d);
                            }
                        }
                        departments = tds;
                    }

                    getFacede().edit(u);
                    setLoggedUser(u);
                    dashboards = webUserController.listWebUserDashboards(u);
                    loadDashboards();
                    setLogged(true);
                    setActivated(u.isActivated());
                    setRole(u.getRole());

                    String sql;

                    UserPreference uf;
                    sql = "select p from UserPreference p where p.webUser=:u order by p.id desc";
                    m = new HashMap();
                    m.put("u", u);
                    uf = getUserPreferenceFacade().findFirstBySQL(sql, m);
                    if (uf == null) {
                        uf = new UserPreference();
                        uf.setWebUser(u);
                        getUserPreferenceFacade().create(uf);
                    }
                    setUserPreference(uf);

                    if (departments.size() == 1) {
                        department = departments.get(0);
                        selectDepartment();
                        UtilityController.addSuccessMessage("Logged successfully. Department is " + department.getName());
                        System.out.println("logged = " + logged);
                    } else {
                        UtilityController.addSuccessMessage("Logged successfully!!!." + "\n Please select a department.");
                        UtilityController.addSuccessMessage(setGreetingMsg() + " " + loggedUser.getWebUserPerson().getName());
                    }
                    if (getApplicationController().isLogged(u) != null) {
                        UtilityController.addErrorMessage("This user is already logged.");
                    }
                    System.out.println("logged = " + logged);
                    return true;
                }
            }
        }
        return false;
    }

    public void loadDashboards() {
        dashboardModel = new DefaultDashboardModel();
        DashboardColumn column1 = new DefaultDashboardColumn();
        DashboardColumn column2 = new DefaultDashboardColumn();
        DashboardColumn column3 = new DefaultDashboardColumn();

        int i = 0;

        for (WebUserDashboard d : dashboards) {
            int n = i % 3;
            switch (n) {
                case 1:
                    column1.addWidget(d.getDashboard().toString());
                    break;
                case 2:
                    column2.addWidget(d.getDashboard().toString());
                    break;
                case 0:
                    column3.addWidget(d.getDashboard().toString());
                    break;
            }
            i++;
        }

        dashboardModel.addColumn(column1);
        dashboardModel.addColumn(column2);
        dashboardModel.addColumn(column3);
    }

    public String selectDepartment() {
        if (loggedUser == null) {
            return "/login";
        }
        if (loggedUser.getWebUserPerson() == null) {
            Person p = new Person();
            p.setName(loggedUser.getName());
            personFacade.create(p);
            loggedUser.setWebUserPerson(p);
            webUserFacade.edit(loggedUser);
        }

        loggedUser.setDepartment(department);
        loggedUser.setInstitution(department.getInstitution());
        getFacede().edit(loggedUser);
        String sql;
        Map m;

//        UserPreference preferances;
        sql = "select p from UserPreference p where p.department =:dep order by p.id desc";
        m = new HashMap();
        m.put("dep", department);
        departmentPreference = getUserPreferenceFacade().findFirstBySQL(sql, m);

        if (getDepartment().getDepartmentType() == DepartmentType.Pharmacy) {
            long i = searchController.createInwardBHTForIssueBillCount();
            if (i > 0) {
                UtilityController.addSuccessMessage("This Phrmacy Has " + i + " BHT Request Today.");
            }
        }

        sql = "select p from UserPreference p where p.institution =:ins order by p.id desc";
        m = new HashMap();
        m.put("ins", institution);
        institutionPreference = getUserPreferenceFacade().findFirstBySQL(sql, m);

        sql = "select p from UserPreference p where p.institution is null and p.department is null and p.webUser is null order by p.id desc";
        applicationPreference = getUserPreferenceFacade().findFirstByJpql(sql);

        if (applicationPreference == null) {
            applicationPreference = new UserPreference();
            applicationPreference.setWebUser(null);
            applicationPreference.setDepartment(null);
            applicationPreference.setInstitution(null);
            getUserPreferenceFacade().create(applicationPreference);
        }

        if (institutionPreference == null) {
            institutionPreference = applicationPreference;
        }
        if (departmentPreference == null) {
            departmentPreference = institutionPreference;
        }

        setLoggedPreference(departmentPreference);
        recordLogin();
        return "/home";
    }

    private void loadApplicationPreferances() {
        String sql = "select p from UserPreference p where p.institution is null and p.department is null and p.webUser is null order by p.id desc";
        applicationPreference = getUserPreferenceFacade().findFirstByJpql(sql);
        if (applicationPreference == null) {
            applicationPreference = new UserPreference();
            applicationPreference.setWebUser(null);
            applicationPreference.setDepartment(null);
            applicationPreference.setInstitution(null);
            getUserPreferenceFacade().create(applicationPreference);
        }
    }

    //get Current hour
    public int getCurrentHour() {
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int hours = cal.get(Calendar.HOUR_OF_DAY);
        return hours;

    }

    //set greeting message
    public String setGreetingMsg() {
        getCurrentHour();
        String msg = "";
        if (getCurrentHour() < 12 || getCurrentHour() == 12) {
            msg = "Good Morning !";
        } else if (getCurrentHour() > 12 && getCurrentHour() < 15) {

            msg = "Good Afternoon !";
        } else if (getCurrentHour() > 14 && getCurrentHour() < 19) {
            msg = "Good Evening !";
        }
        return msg;
    }

    private boolean canLogToDept(WebUser e, Department d) {
        String sql;
        sql = "select wd from WebUserDepartment wd where wd.retired=false and wd.webUser.id=" + e.getId() + " and wd.department.id = " + d.getId();
        return !getWebUserDepartmentFacade().findBySQL(sql).isEmpty();
    }

    private List<Department> listLoggableDepts(WebUser e) {
        if (e == null) {
            return new ArrayList<>();
        }
        String sql;
        Map m = new HashMap();
        m.put("wu", e);
        sql = "select wd.department "
                + " from WebUserDepartment wd "
                + " where wd.retired=false "
                + " and wd.department.retired=false "
                + " and wd.webUser=:wu "
                + " order by wd.department.name";
        return departmentFacade.findBySQL(sql, m);
    }

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
        websiteUserGoingToLog = false;
        recordLogout();
        setLoggedUser(null);
        setLogged(false);
        setActivated(false);
        getPharmacySaleController().clearForNewBill();

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
        return (getLoggedUser().getName());
    }

    /**
     * Creates a new instance of SessionController
     */
    public SessionController() {
        //////// // System.out.println("session started");
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

//    public String getPrimeTheme() {
//        if (primeTheme == null || primeTheme.equals("")) {
//            primeTheme = "hot-sneaks";
//        }
//        if (getLoggedUser() != null) {
//            if (getLoggedUser().getPrimeTheme() != null) {
//                if (!getLoggedUser().getPrimeTheme().equals("")) {
//                    return getLoggedUser().getPrimeTheme();
//                }
//            }
//        }
//        return primeTheme;
//    }
//    public void setPrimeTheme(String primeTheme) {
//        this.primeTheme = primeTheme;
//    }
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
            //////// // System.out.println("5");
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
        //////// // System.out.println("session distroyed " + thisLogin);
        if (thisLogin == null) {
            return;
        }
        applicationController.removeLoggins(this);
        thisLogin.setLogoutAt(Calendar.getInstance().getTime());
        getLoginsFacade().edit(thisLogin);
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        //////// // System.out.println("starting session");
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        //////// // System.out.println("recording logout as session is distroid");
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

    public UserPreference getLoggedPreference() {
        return loggedPreference;
    }

    public void setLoggedPreference(UserPreference loggedPreference) {
        this.loggedPreference = loggedPreference;
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

    public List<Department> getDepartments() {
        if (departments == null) {
            departments = listLoggableDepts(webUser);
        }
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public DashboardModel getDashboardModel() {
        return dashboardModel;
    }

    public void setDashboardModel(DashboardModel dashboardModel) {
        this.dashboardModel = dashboardModel;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public WebUserFacade getWebUserFacade() {
        return webUserFacade;
    }

    public void setWebUserFacade(WebUserFacade webUserFacade) {
        this.webUserFacade = webUserFacade;
    }

    public WebUserDashboardFacade getWebUserDashboardFacade() {
        return webUserDashboardFacade;
    }

    public void setWebUserDashboardFacade(WebUserDashboardFacade webUserDashboardFacade) {
        this.webUserDashboardFacade = webUserDashboardFacade;
    }

    public SearchController getSearchController() {
        return searchController;
    }

    public void setSearchController(SearchController searchController) {
        this.searchController = searchController;
    }

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
    }

    public List<WebUserDashboard> getDashboards() {
        return dashboards;
    }

    public void setDashboards(List<WebUserDashboard> dashboards) {
        this.dashboards = dashboards;
    }

    public String getLoginRequestResponse() {
        return loginRequestResponse;
    }

    public void setLoginRequestResponse(String loginRequestResponse) {
        this.loginRequestResponse = loginRequestResponse;
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

    public UserPreference getInstitutionPreference() {
        institutionPreference = getApplicationPreference();
        return institutionPreference;
    }

    public void setInstitutionPreference(UserPreference institutionPreference) {
        this.institutionPreference = institutionPreference;
        setApplicationPreference(institutionPreference);
    }

    public boolean isWebsiteUserGoingToLog() {
        return websiteUserGoingToLog;
    }

    public void setWebsiteUserGoingToLog(boolean websiteUserGoingToLog) {
        this.websiteUserGoingToLog = websiteUserGoingToLog;
    }

    public String getPrimeTheme() {
        return primeTheme;
    }

    public void setPrimeTheme(String primeTheme) {
        this.primeTheme = primeTheme;
    }

    public UserPreference getDepartmentPreference() {
        return departmentPreference;
    }

    public void setDepartmentPreference(UserPreference departmentPreference) {
        this.departmentPreference = departmentPreference;
    }

    public Boolean getFirstLogin() {
        if (firstLogin == null) {
            firstLogin = isFirstVisit();
        }
        return firstLogin;
    }

    public void setFirstLogin(Boolean firstLogin) {
        this.firstLogin = firstLogin;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

}
