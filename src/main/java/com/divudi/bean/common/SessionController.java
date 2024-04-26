/*
 * Author : Dr. M H B Ariyaratne
 *
 * Consultant (Health Informatics)
 * (94) 71 5812399
 * Email : buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.bean.pharmacy.PharmacySaleController;
import com.divudi.data.InstitutionType;
import com.divudi.data.Privileges;
import com.divudi.ejb.ApplicationEjb;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.entity.Bill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Logins;
import com.divudi.entity.Person;
import com.divudi.entity.UserIcon;
import com.divudi.entity.UserPreference;
import com.divudi.entity.WebUser;
import com.divudi.entity.WebUserDashboard;
import com.divudi.entity.WebUserDepartment;
import com.divudi.entity.WebUserPrivilege;
import com.divudi.entity.WebUserRole;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.LoginsFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.UserPreferenceFacade;
import com.divudi.facade.WebUserDashboardFacade;
import com.divudi.facade.WebUserDepartmentFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.facade.WebUserPrivilegeFacade;
import com.divudi.facade.WebUserRoleFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.IOException;
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
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.primefaces.model.DashboardColumn;
import org.primefaces.model.DashboardModel;
import org.primefaces.model.DefaultDashboardColumn;
import org.primefaces.model.DefaultDashboardModel;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletContext;

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
    @EJB
    InstitutionFacade institutionFacade;
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
    @Inject
    UserIconController userIconController;
    /**
     * Properties
     */
    private static final long serialVersionUID = 1L;
    WebUser loggedUser = null;
    @Deprecated
    private UserPreference loggedPreference;
    private UserPreference applicationPreference;
    @Deprecated
    private UserPreference institutionPreference;
    private UserPreference departmentPreference;
    private UserPreference userPreference;
    boolean logged = false;
    boolean activated = false;
    private String primeTheme;
    String defLocale;
    private List<Privileges> privilegeses;
    Department department;
    List<Department> departments;

    private List<Department> loggableDepartments;
    private List<Department> loggableSubDepartments;
    private List<Institution> loggableInstitutions;
    private List<UserIcon> userIcons;

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

    // A field to store the landing page
    private String landingPage;

    public String navigateToLoginPage() {

        return "/index1.xhtml";
    }

    public void redirectToIndex1(ComponentSystemEvent event) {
        if (!FacesContext.getCurrentInstance().isPostback()) {
            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect("/index1.xhtml");
            } catch (IOException e) {
                // Handle the exception (e.g., logging)
            }
        }
    }

    public String getLandingPageOld() {
        if (landingPage == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
            ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

            String url = request.getRequestURL().toString();

            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            landingPage = url;

            // Get the correct FacesServletMapping from the web.xml context-param
            String facesServletMapping = servletContext.getInitParameter("FacesServletMapping");
            if (facesServletMapping == null) {
                facesServletMapping = "/faces/"; // Default value
            }

            landingPage += facesServletMapping;

            if (getApplicationPreference().getThemeName() == null || getApplicationPreference().getThemeName().trim().equals("")) {
                landingPage += "index.xhtml";
            } else {
                landingPage += "themes/";
                landingPage += getApplicationPreference().getThemeName() + "/index.xhtml";
            }

        }
        return landingPage;
    }

    public String getLandingPage() {
        if (landingPage == null) {
            if (getApplicationPreference().getThemeName() == null || getApplicationPreference().getThemeName().trim().equals("")) {
                landingPage = "index";
            } else {
                landingPage = "themes/";
                landingPage += getApplicationPreference().getThemeName() + "/index";
            }
        }
        return landingPage;
    }

//    public void redirectToLandingPage() {
//        FacesContext context = FacesContext.getCurrentInstance();
//
//        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();
//
//        String applicationName = servletContext.getServletContextName();
//
//        String facesServletMapping = servletContext.getInitParameter("FacesServletMapping");
//        if (facesServletMapping == null) {
//            facesServletMapping = "/faces/"; // Default value
//        }
//        String redirectPath;
//        redirectPath = applicationName + facesServletMapping;
//        if (getApplicationPreference().getThemeName() == null || getApplicationPreference().getThemeName().trim().equals("")) {
//            redirectPath += "index1.xhtml";
//        } else {
//            redirectPath += "themes/";
//            redirectPath += getApplicationPreference().getThemeName() + "/index.xhtml";
//        }
//        try {
//            context.getExternalContext().redirect(redirectPath);
//
//        } catch (IOException e) {
//            System.out.println("e = " + e);
//        }
//    }
//
//    public void redirectToLandingPage1() {
//        FacesContext context = FacesContext.getCurrentInstance();
//        String redirectPath;
//        if (getApplicationPreference().getThemeName() == null || getApplicationPreference().getThemeName().trim().equals("")) {
//            redirectPath = "/index1.xhtml";
//        } else {
//            redirectPath = "/themes/" + getApplicationPreference().getThemeName() + "/index.xhtml";
//        }
//        try {
//            context.getExternalContext().redirect(context.getExternalContext().getRequestContextPath() + redirectPath);
//        } catch (IOException e) {
//            System.out.println("e = " + e);
//        }
//    }
    public void redirectToLandingPage() {
        FacesContext context = FacesContext.getCurrentInstance();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();
        String facesServletMapping = servletContext.getInitParameter("FacesServletMapping");
        if (facesServletMapping == null) {
            facesServletMapping = "/faces/"; // Default value
        }
        String redirectPath;
        if (getApplicationPreference().getThemeName() == null || getApplicationPreference().getThemeName().trim().equals("")) {
            redirectPath = facesServletMapping + "index1.xhtml";
        } else {
            redirectPath = facesServletMapping + "themes/" + getApplicationPreference().getThemeName() + "/index.xhtml";
        }
        try {
            context.getExternalContext().redirect(context.getExternalContext().getRequestContextPath() + redirectPath);
        } catch (IOException e) {
        }
    }

//    public void redirectToLandingPage3() {
//        FacesContext context = FacesContext.getCurrentInstance();
//        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();
//        String facesServletMapping = servletContext.getInitParameter("FacesServletMapping");
//        System.out.println("facesServletMapping = " + facesServletMapping);
//        if (facesServletMapping == null) {
//            facesServletMapping = "/faces/"; // Default value
//        }
//        System.out.println("facesServletMapping = " + facesServletMapping);
//        String redirectPath;
//        if (getApplicationPreference().getThemeName() == null || !getApplicationPreference().getThemeName().trim().equals("")) {
//            redirectPath = facesServletMapping + "index1.xhtml";
//        } else {
//            redirectPath = facesServletMapping + "themes/" + getApplicationPreference().getThemeName() + "/index.xhtml";
//        }
//        try {
//            context.getExternalContext().redirect(context.getExternalContext().getRequestContextPath() + redirectPath);
//        } catch (IOException e) {
//            System.out.println("e = " + e);
//        }
//    }
    public Date currentTime() {
        return new Date();
    }

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
        wu.setActivatedAt(new Date());
        wu.setName(userName);
        wu.setWebUserPassword(getSecurityController().hashAndCheck(password));
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
        return "/admin/institutions/admin_mange_application_preferences?faces-redirect=true";
    }

    public String toManageIntitutionPreferences() {
        String jpql;
        Map m = new HashMap();
        jpql = "select p from UserPreference p where p.institution=:ins order by p.id desc";
        m.put("ins", loggedUser.getInstitution());
        currentPreference = getUserPreferenceFacade().findFirstByJpql(jpql, m);
        if (currentPreference == null) {
            currentPreference = new UserPreference();
            currentPreference.setInstitution(loggedUser.getInstitution());

        }
        currentPreference.setWebUser(null);
        currentPreference.setDepartment(null);

        return "/admin/institutions/admin_mange_institutions_preferences";
    }

    public String toManageDepartmentPreferences() {
        String jpql;
        Map m = new HashMap();
        jpql = "select p from UserPreference p where p.department=:dep order by p.id desc";
        m.put("dep", loggedUser.getDepartment());
        currentPreference = getUserPreferenceFacade().findFirstByJpql(jpql, m);
        if (currentPreference == null) {
            currentPreference = new UserPreference();
            currentPreference.setDepartment(loggedUser.getDepartment());
        }
        currentPreference.setWebUser(null);
        currentPreference.setInstitution(null);
        return "/admin/institutions/admin_mange_department_preferences?faces-redirect=true";
    }

    public String navigateToManageWebUserPreferences(WebUser preferenceUser) {
        String jpql;
        Map m = new HashMap();
        jpql = "select p "
                + " from UserPreference p "
                + " where p.webUser=:wu order by p.id desc";
        m.put("wu", preferenceUser);
        currentPreference = getUserPreferenceFacade().findFirstByJpql(jpql, m);
        if (currentPreference == null) {
            currentPreference = new UserPreference();
            currentPreference.setWebUser(preferenceUser);
        }
        currentPreference.setDepartment(null);
        currentPreference.setInstitution(null);
        return "/admin/users/user_preferences?faces-redirect=true";
    }

    public String navigateToManageOwnWebUserPreferences() {
        if (loggedUser == null) {
            JsfUtil.addErrorMessage("User?");
            return "";
        }
        String jpql;
        Map m = new HashMap();
        jpql = "select p "
                + " from UserPreference p "
                + " where p.webUser=:wu order by p.id desc";
        m.put("wu", loggedUser);
        currentPreference = getUserPreferenceFacade().findFirstByJpql(jpql, m);
        if (currentPreference == null) {
            currentPreference = new UserPreference();
            currentPreference.setWebUser(loggedUser);
        }
        currentPreference.setDepartment(null);
        currentPreference.setInstitution(null);
        return "/user_own_preferances?faces-redirect=true";
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

    public void generateOpdBillTemplate() {
        if (currentPreference == null) {
            return;
        }
        String opdBillTemplate;
        opdBillTemplate = " <div class=\"container text-center mb-4\">\n"
                + "        <h1>{institution_name}</h1>\n"
                + "        <p>{institution_address}</p>\n"
                + "        <p>{institution_phone}</p>\n"
                + "        <p>{institution_email}</p>\n"
                + "        <p>{institution_website}</p>\n"
                + "    </div>\n"
                + "    <hr />\n"
                + "    <div class=\"container\">\n"
                + "        <div class=\"row\">\n"
                + "            <div class=\"col-6\">\n"
                + "                <p>Name: {name}</p>\n"
                + "                <p>Age: {age}</p>\n"
                + "                <p>Gender: {gender}</p>\n"
                + "            </div>\n"
                + "\n"
                + "            <div class=\"col-6\">\n"
                + "                <p>Bill Number: {id}</p>\n"
                + "                <p>Bill Date: {bill_date}</p>\n"
                + "                <p>Bill Time: {bill_time}</p>\n"
                + "            </div>\n"
                + "        </div>\n"
                + "    </div>\n"
                + "    <hr />\n"
                + "    <div class=\"container mt-4\">\n"
                + "            {item_value_table}\n"
                + "    </div>\n"
                + "    <hr />\n"
                + "    <div class=\"container mt-4\">\n"
                + "        <p>Gross Total: {gross_total}</p>\n"
                + "        <p>Discount: {discount}</p>\n"
                + "        <p>Net Total: {net_total}</p>\n"
                + "        <p>Items Count: {count_of_items}</p>\n"
                + "        <p>Cashier Name: {cashier_name}</p>\n"
                + "    </div>";
        currentPreference.setOpdBillTemplate(opdBillTemplate);
        saveUserPreferences();
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

    public void savePreferences(UserPreference uf) {
        if (uf != null) {
            if (uf.getId() == null || uf.getId() == 0) {
                userPreferenceFacade.create(uf);
            } else {
                userPreferenceFacade.edit(uf);
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
        if (department == null) {
            if (loggedUser == null) {
                if (loggedUser.getDepartment() != null) {
                    department = loggedUser.getDepartment();
                }
            }
        }
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
    String password;
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
            return "/index1.xhtml";
        } else {
            JsfUtil.addErrorMessage("Login Failure. Please try again");
            return "";
        }
    }

    public String loginActionWithoutDepartment() {
        department = null;
        institution = null;
        boolean l = checkUsersWithoutDepartment();
        if (l) {
            return "/index1.xhtml?faces-redirect=true";
        } else {
            JsfUtil.addErrorMessage("Invalid User! Login Failure. Please try again");
            return "";
        }
    }

    private boolean login() {
        getApplicationEjb().recordAppStart();
        if (userName.trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a username");
            return false;
        }
        if (isFirstVisit()) {
            prepareFirstVisit();
            return true;
        } else {
            if (department == null) {
                JsfUtil.addErrorMessage("Please select a department");
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
        user.setWebUserPassword(getSecurityController().hashAndCheck(password));
        user.setWebUserPerson(person);
        user.setActivated(true);
        user.setRole(myRole);
        uFacade.create(user);
    }

    public String registeUser() {
        if (!userNameAvailable(newUserName)) {
            JsfUtil.addErrorMessage("User name already exists. Plese enter another user name");
            return "";
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            JsfUtil.addErrorMessage("Password and Re-entered password are not matching");
            return "";
        }

        WebUser user = new WebUser();
        Person person = new Person();
        user.setWebUserPerson(person);
        user.setRole(role);

        person.setName(newPersonName);

        pFacade.create(person);
        user.setName(newUserName);
        user.setWebUserPassword(getSecurityController().hashAndCheck(newPassword));
        user.setWebUserPerson(person);
        user.setTelNo(telNo);
        user.setEmail(email);
        user.setActivated(Boolean.TRUE);

        uFacade.create(user);
        JsfUtil.addSuccessMessage("New User Registered.");
        return "";
    }

    public void changePassword() {
        WebUser user = getLoggedUser();
        if (!getSecurityController().matchPassword(password, user.getWebUserPassword())) {
            JsfUtil.addErrorMessage("The old password you entered is incorrect");
            return;
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            JsfUtil.addErrorMessage("Password and Re-entered password are not maching");
            return;
        }

        user.setWebUserPassword(getSecurityController().hashAndCheck(newPassword));
        uFacade.edit(user);
        //
        JsfUtil.addSuccessMessage("Password changed");

    }

    public void changeCurrentUserPassword() {
        if (getCurrent() == null) {
            JsfUtil.addErrorMessage("Select a User");
            return;
        }
        WebUser user = getCurrent();

        if (!newPassword.equals(newPasswordConfirm)) {
            JsfUtil.addErrorMessage("Password and Re-entered password are not maching");
            return;
        }

        user.setWebUserPassword(getSecurityController().hashAndCheck(newPassword));
        uFacade.edit(user);
        JsfUtil.addSuccessMessage("Password changed");
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
            JsfUtil.addSuccessMessage("First Visit");
            return true;
        } else {
            return false;
        }
    }

    private boolean checkUsers() {
        String temSQL;
        temSQL = "SELECT u FROM WebUser u WHERE u.retired = false";
        List<WebUser> allUsers = getFacede().findByJpql(temSQL);
        for (WebUser u : allUsers) {
            if ((u.getName()).equalsIgnoreCase(userName)) {
                if (getSecurityController().matchPassword(password, u.getWebUserPassword())) {
                    if (!canLogToDept(u, department)) {
                        JsfUtil.addErrorMessage("No privilage to Login This Department");
                        return false;
                    }
                    if (getApplicationController().isLogged(u) != null) {
                        JsfUtil.addErrorMessage("This user already logged. Other instances will be logged out now.");
                    }

                    u.setDepartment(department);
                    u.setInstitution(institution);

                    getFacede().edit(u);

                    setLoggedUser(u);
                    loggableDepartments = fillLoggableDepts();
//                    loggableSubDepartments = fillLoggableSubDepts(loggableDepartments);
                    loggableInstitutions = fillLoggableInstitutions();
                    userIcons = userIconController.fillUserIcons(u, department);
                    setLogged(Boolean.TRUE);
                    setActivated(u.isActivated());
                    setRole(u.getRole());

                    String sql;

                    UserPreference uf;
                    sql = "select p from UserPreference p where p.webUser=:u order by p.id desc";
                    Map m = new HashMap();
                    m.put("u", u);
                    uf = getUserPreferenceFacade().findFirstByJpql(sql, m);
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

                    insPre = getUserPreferenceFacade().findFirstByJpql(sql, m);

                    if (insPre == null) {

                        sql = "select p from UserPreference p where p.institution =:ins order by p.id desc";
                        m = new HashMap();
                        m.put("ins", institution);
                        insPre = getUserPreferenceFacade().findFirstByJpql(sql, m);

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

                    JsfUtil.addSuccessMessage("Logged successfully");
                    return true;
                }
            }
        }
        return false;
    }

    public void decryptAllUsers() {
        String temSQL;
        temSQL = "SELECT u FROM WebUser u";
        List<WebUser> allUsers = getFacede().findByJpql(temSQL);
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
        return loginForRequests(userName, password);
    }

    public boolean loginForRequestsForDoctors() {
        return loginForRequestsForDoctors(userName, password);
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
        temSQL = "SELECT u FROM WebUser u WHERE u.retired = false and (u.name)=:n order by u.id desc";
        Map m = new HashMap();

        m.put("n", temUserName.trim().toLowerCase());
        WebUser u = getFacede().findFirstByJpql(temSQL, m);

        //// // System.out.println("temSQL = " + temSQL);
        if (u == null) {
            return false;
        }

        if (getSecurityController().matchPassword(temPassword, u.getWebUserPassword())) {

            setLoggedUser(u);
            loggableDepartments = fillLoggableDepts();
//            loggableSubDepartments = fillLoggableSubDepts(loggableDepartments);
            loggableInstitutions = fillLoggableInstitutions();
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
        temSQL = "SELECT u FROM WebUser u WHERE u.retired = false and (u.name)=:n order by u.id desc";
        Map m = new HashMap();

        m.put("n", temUserName.trim().toLowerCase());
        WebUser u = getFacede().findFirstByJpql(temSQL, m);

        if (u == null) {
            return false;
        }

        if (getSecurityController().matchPassword(temPassword, u.getWebUserPassword())) {
            setLoggedUser(u);
            loggableDepartments = fillLoggableDepts();
//            loggableSubDepartments = fillLoggableSubDepts(loggableDepartments);
            loggableInstitutions = fillLoggableInstitutions();
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
        String jpql;
        jpql = "SELECT u FROM WebUser u WHERE u.retired = false and (u.name)=:un";
        Map m = new HashMap();
        m.put("un", userName.toLowerCase());
        List<WebUser> allUsers = getFacede().findByJpql(jpql, m);
        for (WebUser u : allUsers) {
            if ((u.getName()).equalsIgnoreCase(userName)) {
                boolean passwordIsOk = SecurityController.matchPassword(password, u.getWebUserPassword());
                if (passwordIsOk) {

                    departments = listLoggableDepts(u);

                    if (webUserController.testRun) {
                        departments = departmentController.fillAllItems();
                    }

                    if (departments.isEmpty()) {
                        JsfUtil.addErrorMessage("This user has no privilage to login to any Department. Please conact system administrator.");
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
                    loggableDepartments = fillLoggableDepts();
                    if (webUserController.testRun) {
                        loggableDepartments = departmentController.fillAllItems();
                    }
//                    loggableSubDepartments = fillLoggableSubDepts(loggableDepartments);
                    loggableInstitutions = fillLoggableInstitutions();

                    if (webUserController.testRun) {
                        loggableInstitutions = institutionController.fillAllItems();
                    }
                    loadDashboards();
                    setLogged(true);
                    setActivated(u.isActivated());
                    setRole(u.getRole());

                    String sql;
                    UserPreference uf;
                    sql = "select p from UserPreference p where p.webUser=:u order by p.id desc";
                    m = new HashMap();
                    m.put("u", u);
                    uf = getUserPreferenceFacade().findFirstByJpql(sql, m);
                    if (uf == null) {
                        uf = new UserPreference();
                        uf.setWebUser(u);
                        getUserPreferenceFacade().create(uf);
                    }
                    setUserPreference(uf);

                    JsfUtil.addSuccessMessage("Logged successfully!!!." + "\n Please select a department.");
                    JsfUtil.addSuccessMessage(setGreetingMsg());
                    if (getApplicationController().isLogged(u) != null) {
                        JsfUtil.addErrorMessage("This user is already logged.");
                    }
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

        for (WebUserDashboard d : getDashboards()) {
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
            return "/login?faces-redirect=true";
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

        userIcons = userIconController.fillUserIcons(loggedUser, department);
        dashboards = webUserController.listWebUserDashboards(loggedUser);

        userPrivilages = fillUserPrivileges(loggedUser, department, false);
        loggableSubDepartments = fillLoggableSubDepts(department);
//        if (userPrivilages == null || userPrivilages.isEmpty()) {
//            userPrivilages = fillUserPrivileges(loggedUser, null, true);
//            createUserPrivilegesForAllDepartments(loggedUser, department, loggableDepartments);
//            logout();
//        }

        String sql;
        Map m;

//        UserPreference preferances;
        sql = "select p from UserPreference p where p.department =:dep order by p.id desc";
        m = new HashMap();
        m.put("dep", department);
        departmentPreference = getUserPreferenceFacade().findFirstByJpql(sql, m);

//        if (getDepartment().getDepartmentType() == DepartmentType.Pharmacy) {
//            long i = searchController.createInwardBHTForIssueBillCount();
//            if (i > 0) {
//                JsfUtil.addSuccessMessage("This Phrmacy Has " + i + " BHT Request Today.");
//            }
//        }
        sql = "select p from UserPreference p where p.institution =:ins order by p.id desc";
        m = new HashMap();
        m.put("ins", institution);
        institutionPreference = getUserPreferenceFacade().findFirstByJpql(sql, m);

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
        return "/home?faces-redirect=true";
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
            msg = "Good Morning !, ";
        } else if (getCurrentHour() > 12 && getCurrentHour() < 15) {
            msg = "Good Afternoon !, ";
        } else if (getCurrentHour() > 14 && getCurrentHour() < 19) {
            msg = "Good Evening !, ";
        } else {
            msg = "Good Day !, ";
        }

        if (getLoggedUser() != null) {
            if (getLoggedUser().getWebUserPerson() != null) {
                msg += getLoggedUser().getWebUserPerson().getName();
            }
        }
        return msg;
    }

    private boolean canLogToDept(WebUser e, Department d) {
        String sql;
        sql = "select wd from WebUserDepartment wd where wd.retired=false and wd.webUser.id=" + e.getId() + " and wd.department.id = " + d.getId();
        return !getWebUserDepartmentFacade().findByJpql(sql).isEmpty();
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
        return departmentFacade.findByJpql(sql, m);
    }

    private List<Department> fillLoggableDepts() {
        WebUser e = getLoggedUser();
        if (e == null) {
            return new ArrayList<>();
        }
        String sql;
        Map m = new HashMap();
        m.put("wu", e);
        sql = "select distinct wd.department "
                + " from WebUserDepartment wd "
                + " where wd.retired=false "
                + " and wd.department.retired=false "
                + " and wd.webUser=:wu "
                + " order by wd.department.name";
        return departmentFacade.findByJpql(sql, m);
    }

    private List<Department> fillLoggableSubDepts(Department loggableDept) {
        List<Department> ds = new ArrayList<>();
        ds.add(loggableDept);
        return fillLoggableSubDepts(ds);
    }

    private List<Department> fillLoggableSubDepts(List<Department> loggableDepts) {
        if (loggableDepts == null || loggableDepts.isEmpty()) {
            return new ArrayList<>();
        }

        String sql;
        Map m = new HashMap();
        m.put("loggableDepts", loggableDepts);
        sql = "select distinct d "
                + " from Department d "
                + " where d.retired=false "
                + " and d.superDepartment in :loggableDepts "
                + " order by d.name";
        return departmentFacade.findByJpql(sql, m);
    }

    public List<Institution> fillLoggableInstitutions() {
        WebUser e = getLoggedUser();
        if (e == null) {
            return new ArrayList<>();
        }
        String jpql;
        Map m = new HashMap();
        m.put("wu", e);
        jpql = "select DISTINCT wd.department.institution "
                + " from WebUserDepartment wd "
                + " where wd.retired=false "
                + " and wd.department.retired=false "
                + " and wd.webUser=:wu "
                + " order by wd.department.institution.name";
        return institutionFacade.findByJpql(jpql, m);
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
        loggableDepartments = null;
        loggableSubDepartments = null;
        loggableInstitutions = null;
        userIcons = null;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getPrimeTheme() {
        if (primeTheme == null || primeTheme.equals("")) {
            primeTheme = "material-light-outlined";
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

    public List<WebUserPrivilege> fillUserPrivileges(WebUser twu, Department tdept, boolean deptIsNull) {
        String sql;
        Map m = new HashMap();
        sql = "select w "
                + " from WebUserPrivilege w "
                + " where w.retired=:ret "
                + " and w.webUser=:wu ";
        if (tdept != null) {
            sql += " and w.department=:dep ";
            m.put("dep", tdept);
        }
        if (deptIsNull) {
            sql += " and w.department is null ";
        }
        m.put("ret", false);
        m.put("wu", twu);
        List<WebUserPrivilege> twups = getWebUserPrivilegeFacade().findByJpql(sql, m);
        return twups;
    }

    public List<WebUserPrivilege> getUserPrivileges() {
        if (userPrivilages == null) {
            userPrivilages = fillUserPrivileges(getLoggedUser(), getLoggedUser().getDepartment(), false);
        }
        if (userPrivilages == null) {
            userPrivilages = new ArrayList<>();
        }
        return userPrivilages;
    }

    public void fillCurrentPreferences() {
        String jpql;
        Map m = new HashMap();
        jpql = "select p from UserPreference p where p.department=:dep order by p.id desc";
        m.put("dep", getDepartment());
        currentPreference = getUserPreferenceFacade().findFirstByJpql(jpql, m);
        if (currentPreference == null) {
            currentPreference = new UserPreference();
            currentPreference.setDepartment(loggedUser.getDepartment());
        }
        currentPreference.setWebUser(null);
        currentPreference.setInstitution(null);

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
        if (thisLogin.getId() == null) {
            getLoginsFacade().create(thisLogin);
        }else{
            getLoginsFacade().edit(thisLogin);
        }
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

    @Deprecated
    public UserPreference getLoggedPreference() {
        return loggedPreference;
    }

    @Deprecated
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
        if (dashboards == null) {
            dashboards = new ArrayList<>();
        }
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

    @Deprecated
    public UserPreference getInstitutionPreference() {
        institutionPreference = getApplicationPreference();
        return institutionPreference;
    }

    @Deprecated
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

    public List<Department> getLoggableDepartments() {
        if (loggableDepartments == null) {
            loggableDepartments = fillLoggableDepts();
        }
        return loggableDepartments;
    }

    public List<Institution> getLoggableInstitutions() {
        if (loggableInstitutions == null) {
            loggableInstitutions = fillLoggableInstitutions();
        }
        return loggableInstitutions;
    }

    public List<UserIcon> getUserIcons() {
        return userIcons;
    }

    public void setUserIcons(List<UserIcon> userIcons) {
        this.userIcons = userIcons;
    }

    private void createUserPrivilegesForAllDepartments(WebUser tmpLoggedUser, Department loggedDept, List<Department> tmpLoggableDeps) {
        List<WebUserPrivilege> twups = fillUserPrivileges(tmpLoggedUser, null, true);
        if (tmpLoggedUser == null) {
            return;
        }
        if (loggedDept == null) {
            return;
        }
        if (tmpLoggableDeps == null) {
            return;
        }
        if (tmpLoggableDeps.isEmpty()) {
            return;
        }
        List<Department> tds = tmpLoggableDeps;
        tds.remove(loggedDept);
        for (WebUserPrivilege twup : twups) {
            twup.setDepartment(loggedDept);
            getWebUserPrivilegeFacade().edit(twup);
            for (Department d : tds) {
                WebUserPrivilege nwup = new WebUserPrivilege();
                nwup.setWebUser(tmpLoggedUser);
                nwup.setDepartment(d);
                nwup.setPrivilege(twup.getPrivilege());
                getWebUserPrivilegeFacade().create(nwup);
            }
        }

    }

    public List<Department> getLoggableSubDepartments() {
        return loggableSubDepartments;
    }

    public void setLoggableSubDepartments(List<Department> loggableSubDepartments) {
        this.loggableSubDepartments = loggableSubDepartments;
    }

}
