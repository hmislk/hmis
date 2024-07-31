/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.hr.StaffController;
import com.divudi.data.Dashboard;
import com.divudi.data.Privileges;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Person;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.entity.WebUserDashboard;
import com.divudi.entity.WebUserPrivilege;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.WebUserDashboardFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.facade.WebUserPrivilegeFacade;
import com.divudi.facade.WebUserRoleFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.LoginPage;
import com.divudi.entity.UserNotification;
import com.divudi.entity.WebUserRole;
import com.divudi.facade.UserNotificationFacade;
import com.divudi.light.common.WebUserLight;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.event.FlowEvent;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class WebUserController implements Serializable {

    /**
     * EJBs
     */
    @EJB
    private WebUserFacade ejbFacade;
    @EJB
    WebUserRoleFacade roleFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private WebUserPrivilegeFacade webUserPrevilageFacade;
    @EJB
    private StaffFacade staffFacade;
    @EJB
    private WebUserDashboardFacade webUserDashboardFacade;
    @EJB
    UserNotificationFacade userNotificationFacade;
    /**
     * Controllers
     */
    @Inject
    SessionController sessionController;
    @Inject
    SecurityController securityController;
    @Inject
    private UserPaymentSchemeController userPaymentSchemeController;
    @Inject
    private UserDepartmentController userDepartmentController;
    @Inject
    private StaffController staffController;
    @Inject
    private UserPrivilageController userPrivilageController;
    @Inject
    UserIconController userIconController;
    @Inject
    TriggerSubscriptionController triggerSubscriptionController;
    @Inject
    UserNotificationController userNotificationController;
    @Inject
    WebUserRoleUserController webUserRoleUserController;
    /**
     * Class Variables
     */
    List<WebUser> items;
    List<WebUser> searchItems;
    private WebUser current;
    private WebUser selected;
    private WebUserLight selectedLight;
    String selectText = "";
    List<Department> departments;
    List<Institution> institutions;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    private Institution institution;
    private Department department;
    private Privileges[] currentPrivilegeses;
    Speciality speciality;
    List<WebUserPrivilege> userPrivileges;
    private List<WebUser> webUsers;
    List<WebUserLight> itemsToRemove;
    private List<WebUserLight> webUseLights;

    Staff staff;
    boolean createOnlyUser = false;
    boolean createOnlyUserForExsistingUser = false;
    private String newPassword;
    private String newPasswordConfirm;

    private Dashboard dashboard;
    private WebUserDashboard webUserDashboard;
    private List<WebUserDashboard> webUserDashboards;
    private int manageDiscountIndex;
    private int manageUsersIndex;
    private List<Department> departmentsOfSelectedUsersInstitution;

    private WebUserRole webUserRole;

    private LoginPage loginPage;

    boolean grantAllPrivilegesToAllUsersForTesting = false;

    private List<UserNotification> userNotifications;
    private int userNotificationCount;

    public String navigateToRemoveMultipleUsers() {
        return "/admin/users/user_remove_multiple";
    }

    public String navigateToManageUsersWithoutStaff() {
        fillLightUsersWithoutStaff();
        return "/admin/users/user_list_without_staff?faces-redirect=true";
    }

    public void removeMultipleUsers() {
        if (itemsToRemove == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        if (itemsToRemove.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        for (WebUserLight s : itemsToRemove) {
            if (s.getId() == null) {
                continue;
            }
            WebUser twu = getFacade().find(s.getId());
            if (twu == null) {
                continue;
            }
            twu.setRetired(true);
            twu.setRetireComments("Bulk Remove");
            twu.setRetirer(getSessionController().getLoggedUser());
            try {
                getFacade().edit(twu);
            } catch (Exception e) {
            }
        }
        itemsToRemove = null;
        fillLightUsers();
    }

    public void updateWebUser(WebUser webUser) {
        personFacade.edit(webUser.getWebUserPerson());
        staffFacade.edit(webUser.getStaff());
    }

    public void createWebUserDrawers() {
        String sql = "select c from WebUser c "
                + " where c.retired=false "
                + " c.drawer is not null "
                + " order by c.drawer.name,c.webUserPerson.name";
        webUsers = getFacade().findByJpql(sql);
    }

    public List<Department> getInstitutionDepatrments() {
        List<Department> d;
        if (getInstitution() == null) {
            return new ArrayList<>();
        } else {
            String sql = "Select d From Department d where d.retired=false and d.institution.id=" + getInstitution().getId();
            d = getDepartmentFacade().findByJpql(sql);
        }
        return d;
    }

    public void save(WebUser wu) {
        if (wu.getId() == null) {
            getFacade().create(wu);
        } else {
            getFacade().edit(wu);
        }
    }

    public void saveUser() {
        if (current == null) {
            return;
        }
        if (current.getId() == null || current.getId() == 0) {
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved");
        } else {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated");
        }
    }

    public void removeUser() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Select a user to remove");
            return;
        }
        selected.getWebUserPerson().setRetired(true);
        selected.getWebUserPerson().setRetirer(getSessionController().getLoggedUser());
        selected.getWebUserPerson().setRetiredAt(Calendar.getInstance().getTime());
        getPersonFacade().edit(selected.getWebUserPerson());
        selected.setName(selected.getId().toString());
        selected.setRetired(true);
        selected.setRetirer(getSessionController().getLoggedUser());
        selected.setRetiredAt(Calendar.getInstance().getTime());
        getFacade().edit(selected);
        selected = null;
        fillLightUsers();
        JsfUtil.addErrorMessage("User Removed");
    }

    public List<WebUser> completeUser(String qry) {
        List<WebUser> a = null;
        if (qry != null) {
            a = getFacade().findByJpql("select c from WebUser c"
                    + " where c.retired=false"
                    + " and  ("
                    + " c.webUserPerson.name like '%" + qry.toUpperCase() + "%'"
                    + " or "
                    + " c.code like '%" + qry.toUpperCase() + "%'"
                    + " or"
                    + " c.name like '%" + qry.toUpperCase() + "%'"
                    + " )"
                    + " order by c.webUserPerson.name");
        }
        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }

    public void setUserPrivileges(List<WebUserPrivilege> userPrivileges) {
        this.userPrivileges = userPrivileges;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public boolean hasPrivilege(String privilege) {
        boolean hasPri = false;
        if (grantAllPrivilegesToAllUsersForTesting) {
            return true;
        }
        if (getSessionController().getLoggedUser() == null) {
            return hasPri;
        }

        if (getSessionController().getLoggedUser().getId() == null) {
            return hasPri;
        }

        for (WebUserPrivilege w : getSessionController().getUserPrivileges()) {
            Privileges p = null;
            try {
                p = Privileges.valueOf(privilege);
            } catch (Exception e) {
                hasPri = false;
                return hasPri;
            }
            if (w.getPrivilege() != null && w.getPrivilege().equals(p)) {

                hasPri = true;
                return hasPri;
            }
        }
        return hasPri;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public List<Department> getDepartments() {
        if (departments == null) {
            String sql;
            if (getInstitution() != null && getInstitution().getId() != null) {
                sql = "select d from Department d where d.retired=false and d.institution.id = " + getInstitution().getId();
                departments = getDepartmentFacade().findByJpql(sql);
            }
        }
        if (departments == null) {
            departments = new ArrayList<>();
        }
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public List<Institution> getInstitutions() {
        if (institutions == null) {
            String sql;
            sql = "select i from Institution i where i.retired=false order by i.name";
            institutions = getInstitutionFacade().findByJpql(sql);
        }
        return institutions;
    }

    public void setInstitutions(List<Institution> institutions) {
        this.institutions = institutions;
        departments = null; // This line is essential. Othervice departments will not be refreshed when institution is changed

    }
    boolean skip;

    public String onFlowProcess(FlowEvent event) {
        if (skip) {
            skip = false;   //reset in case user goes back
            return "confirm";
        } else {
            return event.getNewStep();
        }
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public WebUserController() {
    }

    public List<WebUser> getItems() {
        if (items == null) {
            items = getFacade().findByJpql("Select d From WebUser d where d.retired = false order by d.webUserPerson.name");
            dycryptName();
        }

        return items;
    }

    private void dycryptName() {
        List<WebUser> temp = items;

        for (int i = 0; i < temp.size(); i++) {
            WebUser w = temp.get(i);
            w.setName((w.getName()).toLowerCase());
            temp.set(i, w);
        }

        items = temp;
    }

    public void setItems(List<WebUser> items) {
        this.items = items;
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

    private WebUserFacade getFacade() {
        return ejbFacade;
    }

    private void recreateModel() {
        items = null;
    }

    public void prepareAdd() {
        current = new WebUser();
    }

    public String navigateToAddNewUser() {
        setCurrent(new WebUser());
        getCurrent().setActivated(true);
        Person p = new Person();
        getCurrent().setWebUserPerson(p);
        setSpeciality(null);
        currentPrivilegeses = null;
        createOnlyUser = false;
        createOnlyUserForExsistingUser = false;
        staff = null;
        department = null;
        institution = null;
        loginPage = null;
        return "/admin/users/user_add_new";
    }

    public SecurityController getSecurityController() {
        return securityController;
    }

    public void setSecurityController(SecurityController securityController) {
        this.securityController = securityController;
    }

    public Boolean userNameAvailable(String userName) {
        boolean available = false;
        String j;
        j = "select w from WebUser w where w.retired=false";
        List<WebUser> allUsers = getFacade().findByJpql(j);
        if (allUsers == null) {
            return false;
        }
        for (WebUser w : allUsers) {

            if (userName != null && w != null && w.getName() != null) {
                if (userName.toLowerCase().equals((w.getName()).toLowerCase())) {
                    //////// // System.out.println("Ift");
                    available = true;
                    return available;// ok. that is may be the issue. we will try with it ok
                }
            }
        }
        return available;
    }

    public String saveNewUser() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return "";
        }
        if (createOnlyUserForExsistingUser && getStaff() == null) {
            JsfUtil.addErrorMessage("Select Staff");
            return "";
        }
        if (userNameAvailable(getCurrent().getName())) {
            JsfUtil.addErrorMessage("User name already exists. Plese enter another user name");
            return "";
        }
        getCurrent().setActivated(true);
        getCurrent().setActivatedAt(new Date());
        getCurrent().setActivator(getSessionController().getLoggedUser());
        getCurrent().getWebUserPerson().setCreatedAt(new Date());
        getCurrent().getWebUserPerson().setCreater(getSessionController().getLoggedUser());

        getCurrent().setLoginPage(loginPage);

        getPersonFacade().create(getCurrent().getWebUserPerson());
        if (createOnlyUserForExsistingUser) {
            getCurrent().getWebUserPerson().setName(getStaff().getPerson().getName());
            getCurrent().getWebUserPerson().setAddress(getStaff().getPerson().getAddress());
            getCurrent().getWebUserPerson().setMobile(getStaff().getPerson().getMobile());
            getPersonFacade().edit(getCurrent().getWebUserPerson());
            getCurrent().setCode(getStaff().getCode());
            getCurrent().setStaff(getStaff());
            if (getStaff().getWorkingDepartment() != null) {
                getCurrent().setInstitution(getStaff().getWorkingDepartment().getInstitution());
                getCurrent().setDepartment(getStaff().getWorkingDepartment());
            }

        } else {
            getCurrent().setInstitution(getInstitution());
            getCurrent().setDepartment(getDepartment());
            if (!createOnlyUser) {
                Staff staff = new Staff();
                //Save Staff
                staff.setPerson(getCurrent().getWebUserPerson());
                staff.setCreatedAt(Calendar.getInstance().getTime());
                staff.setDepartment(department);
                staff.setWorkingDepartment(department);
                staff.setInstitution(institution);
                staff.setSpeciality(speciality);
                staff.setCode(getCurrent().getCode());
                getStaffFacade().create(staff);
                getCurrent().setStaff(staff);
            }
        }

        //Save Web User
        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(sessionController.loggedUser);
        getCurrent().setName((getCurrent().getName()));
        getCurrent().setWebUserPassword(getSecurityController().hashAndCheck(getCurrent().getWebUserPassword()));
        getFacade().create(getCurrent());
        if (createOnlyUser) {
            JsfUtil.addSuccessMessage("Add New User Only");
        } else if (createOnlyUserForExsistingUser) {
            JsfUtil.addSuccessMessage("Add New User To Exsisting Staff");
        } else {
            JsfUtil.addSuccessMessage("Add New User & Staff");
        }

        if (webUserRole != null) {
            //Added the Current Department for Permission
            userDepartmentController.setSelectedUser(current);
            userDepartmentController.setCurrentDepartment(department);
            userDepartmentController.addDepartmentForUser();

            //Add Permission to Web User From User Role
            webUserRoleUserController.getCurrent().setWebUser(current);
            webUserRoleUserController.getCurrent().setWebUserRole(webUserRole);
            webUserRoleUserController.getCurrent().setWebUser(current);
            webUserRoleUserController.setDepartment(department);
            webUserRoleUserController.fillUsers();
            webUserRoleUserController.addUsers();
        }
        recreateModel();
        selectText = "";
        return navigateToListUsers();
    }

    public void onlyAddStaffListner() {
        createOnlyUserForExsistingUser = false;
    }

    public void onlyAddStaffForExsistingUserListner() {
        createOnlyUser = false;
    }

    public List<WebUser> getToApproveUsers() {
        String temSQL;
        temSQL = "SELECT u FROM WebUser u WHERE u.retired=false AND u.activated=false";
        return getEjbFacade().findByJpql(temSQL);
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public WebUserFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(WebUserFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public WebUserRoleFacade getRoleFacade() {
        return roleFacade;
    }

    public void setRoleFacade(WebUserRoleFacade roleFacade) {
        this.roleFacade = roleFacade;
    }

    public List<WebUser> getSearchItems() {
        if (searchItems == null) {
            if (selectText.equals("")) {
                searchItems = getFacade().findAll("name", true);
            } else {
                searchItems = getFacade().findAll("name", "%" + selectText + "%",
                        true);
                if (searchItems.size() > 0) {
                    current = searchItems.get(0);
                } else {
                    current = null;
                }
            }
        }
        return searchItems;
    }

    public void setSearchItems(List<WebUser> searchItems) {
        this.searchItems = searchItems;
    }

    public void delete() {
        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(sessionController.loggedUser);
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successful");
        } else {
            JsfUtil.addErrorMessage("Nothing To Delete");
        }
        recreateModel();
        getItems();
        current = null;
    }

    public String navigateToListUsers() {
        fillLightUsers();
        return "/admin/users/user_list?faces-redirect=true";
    }
    

    private void fillLightUsers() {
        HashMap<String, Object> m = new HashMap<>();
        String jpql;
        jpql = "Select new com.divudi.light.common.WebUserLight(wu.name, wu.webUserPerson.name, wu.id)"
                + " from WebUser wu "
                + " where wu.retired=:ret "
                + " and wu.staff is not null "
                + " order by wu.name";
        m.put("ret", false);
        webUseLights = (List<WebUserLight>) getPersonFacade().findLightsByJpql(jpql, m);
    }
    
    private void fillLightUsersWithoutStaff() {
        HashMap<String, Object> m = new HashMap<>();
        String jpql;
        jpql = "Select new com.divudi.light.common.WebUserLight(wu.name, wu.webUserPerson.name, wu.id)"
                + " from WebUser wu "
                + " where wu.retired=:ret "
                + " and wu.staff is null "
                + " order by wu.name";
        m.put("ret", false);
        webUseLights = (List<WebUserLight>) getPersonFacade().findLightsByJpql(jpql, m);
    }

    public List<WebUser> getSelectedItems() {

        return items;
    }

    public String getSelectText() {
        return selectText;
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        departments = null;
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Privileges[] getPrivilegeses() {
        return Privileges.values();
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

//    public List<Privileges> getCurrentPrivilegeses() {
//        return currentPrivilegeses;
//    }
//
//    public void setCurrentPrivilegeses(List<Privileges> currentPrivilegeses) {
//        this.currentPrivilegeses = currentPrivilegeses;
//    }
    public WebUserPrivilegeFacade getWebUserPrevilageFacade() {
        return webUserPrevilageFacade;
    }

    public void setWebUserPrevilageFacade(WebUserPrivilegeFacade webUserPrevilageFacade) {
        this.webUserPrevilageFacade = webUserPrevilageFacade;
    }

    public Privileges[] getCurrentPrivilegeses() {
        return currentPrivilegeses;
    }

    public void setCurrentPrivilegeses(Privileges[] currentPrivilegeses) {
        this.currentPrivilegeses = currentPrivilegeses;
    }

    public List<WebUser> getWebUsers() {
        return webUsers;
    }

    public void setWebUsers(List<WebUser> webUsers) {
        this.webUsers = webUsers;
    }

    public List<WebUserLight> getItemsToRemove() {
        return itemsToRemove;
    }

    public void setItemsToRemove(List<WebUserLight> itemsToRemove) {
        this.itemsToRemove = itemsToRemove;
    }

    public boolean isCreateOnlyUser() {
        return createOnlyUser;
    }

    public void setCreateOnlyUser(boolean createOnlyUser) {
        this.createOnlyUser = createOnlyUser;
    }

    public boolean isCreateOnlyUserForExsistingUser() {
        return createOnlyUserForExsistingUser;
    }

    public void setCreateOnlyUserForExsistingUser(boolean createOnlyUserForExsistingUser) {
        this.createOnlyUserForExsistingUser = createOnlyUserForExsistingUser;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public WebUser getSelected() {
        return selected;
    }

    public void setSelected(WebUser selected) {
        this.selected = selected;
    }

    public String navigateToEditUser() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select a user");
            return "";
        }
        current = selected;
        return "/admin/users/user?faces-redirect=true";
    }

    public String navigateToManageStaff() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select a user");
            return "";
        }
        if(selected.getStaff()==null){
            staff = new Staff();
            staff.setPerson(selected.getWebUserPerson());
            staffFacade.create(staff);
            selected.setStaff(staff);
            getFacade().edit(selected);
        }
        getStaffController().setCurrent(selected.getStaff());
        return "/hr/hr_staff_admin?faces-redirect=true";
    }

    public String navigateToEditPasswordByAdmin() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select a user");
            return "";
        }
        current = selected;
        return "/admin/users/change_password?faces-redirect=true";
    }

    public String navigateToManagePrivileges() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select a user");
            return "";
        }
        getUserPrivilageController().setCurrentWebUser(selected);
        getUserPrivilageController().init();
        getUserPrivilageController().setDepartments(getUserPrivilageController().fillWebUserDepartments(selected));
        return "/admin/users/user_privileges?faces-redirect=true";
    }

    public String navigateToManagePaymentSchemes() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select a user");
            return "";
        }
        getUserPaymentSchemeController().setSelectedUser(selected);
        return "/admin/users/user_payment_schemes?faces-redirect=true";
    }

    public String navigateToManageUserIcons() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select a user");
            return "";
        }
        userIconController.setUser(selected);
        userIconController.setDepartments(getUserPrivilageController().fillWebUserDepartments(selected));
        return "/admin/users/user_icons?faces-redirect=true";
    }

    public String navigateToManageUserSubscriptions() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select a user");
            return "";
        }
        triggerSubscriptionController.setUser(selected);
        triggerSubscriptionController.setDepartments(getUserPrivilageController().fillWebUserDepartments(selected));
        return "/admin/users/user_subscription?faces-redirect=true";
    }

    public String toManageSignature() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select a user");
            return "";
        }
        getStaffController().setCurrent(selected.getStaff());
        return "/admin/institutions/admin_staff_signature?faces-redirect=true";
    }

    public String navigateToManageDepartments() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select a user");
            return "";
        }
        getUserDepartmentController().setSelectedUser(selected);
        getUserDepartmentController().setItems(getUserDepartmentController().fillWebUserDepartments(selected));
        return "/admin/users/user_department?faces-redirect=true";
    }
    
    public String navigateToManageRoutes() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select a user");
            return "";
        }
        getUserDepartmentController().setSelectedUser(selected);
        getUserDepartmentController().setItems(getUserDepartmentController().fillWebUserDepartments(selected));
        return "/admin/users/user_department?faces-redirect=true";
    }

    public String toManageDashboards() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select a user");
            return "";
        }
        current = selected;
        listWebUserDashboards();
        return "/admin_manage_dashboards?faces-redirect=true";
    }

    public String toManageUserPreferences() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select a user");
            return "";
        }
        current = selected;
        return sessionController.navigateToManageWebUserPreferences(current);
    }

    public void addWebUserDashboard() {
        if (current == null) {
            JsfUtil.addErrorMessage("User ?");
            return;
        }
        if (current == null) {
            JsfUtil.addErrorMessage("Dashboard ?");
            return;
        }
        WebUserDashboard d = new WebUserDashboard();
        d.setWebUser(selected);
        d.setDashboard(dashboard);
        d.setCreatedAt(new Date());
        d.setCreater(sessionController.getLoggedUser());
        getWebUserDashboardFacade().create(d);
        JsfUtil.addSuccessMessage("Added");
        listWebUserDashboards();
    }

    public void removeWebUserDashboard() {
        if (webUserDashboard == null) {
            JsfUtil.addErrorMessage("Dashboard ?");
            return;
        }
        WebUserDashboard d = webUserDashboard;
        d.setRetired(true);
        d.setRetiredAt(new Date());
        d.setRetirer(sessionController.getLoggedUser());
        getWebUserDashboardFacade().edit(d);
        JsfUtil.addSuccessMessage("Removed");
        listWebUserDashboards();
    }

    public List<WebUserDashboard> listWebUserDashboards(WebUser wu) {
        List<WebUserDashboard> wuds = new ArrayList<>();
        if (wu == null) {
            return wuds;
        }
        String j = "Select d from WebUserDashboard d "
                + " where d.retired=false "
                + " and d.webUser=:u "
                + " order by d.id";
        Map m = new HashMap();
        m.put("u", wu);
        wuds = getWebUserDashboardFacade().findByJpql(j, m);
        return wuds;
    }

    public void listWebUserDashboards() {
        webUserDashboards = listWebUserDashboards(current);
    }

    public String backToViewUsers() {
        return "/admin/users/user_list";
    }

    public String changeCurrentUserPassword() {
        if (getCurrent() == null) {
            JsfUtil.addErrorMessage("Select a User");
            return "";
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            JsfUtil.addErrorMessage("Password and Re-entered password are not maching");
            return "";
        }
        String hashedPassword;
        hashedPassword = getSecurityController().hashAndCheck(newPassword);
        current.setWebUserPassword(hashedPassword);
        getFacade().edit(current);
        JsfUtil.addSuccessMessage("Password changed");
        return navigateToListUsers();
    }

    public UserPaymentSchemeController getUserPaymentSchemeController() {
        return userPaymentSchemeController;
    }

    public UserDepartmentController getUserDepartmentController() {
        return userDepartmentController;
    }

    public StaffController getStaffController() {
        return staffController;
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

    public UserPrivilageController getUserPrivilageController() {
        return userPrivilageController;
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    public WebUserDashboard getWebUserDashboard() {
        return webUserDashboard;
    }

    public void setWebUserDashboard(WebUserDashboard webUserDashboard) {
        this.webUserDashboard = webUserDashboard;
    }

    public List<WebUserDashboard> getWebUserDashboards() {
        return webUserDashboards;
    }

    public void setWebUserDashboards(List<WebUserDashboard> webUserDashboards) {
        this.webUserDashboards = webUserDashboards;
    }

    public WebUserDashboardFacade getWebUserDashboardFacade() {
        return webUserDashboardFacade;
    }

    public List<Department> getDepartmentsOfSelectedUsersInstitution() {
        departmentsOfSelectedUsersInstitution = new ArrayList<>();
        if (getCurrent() == null) {
            return departmentsOfSelectedUsersInstitution;
        }
        if (getCurrent().getInstitution() == null) {
            return departmentsOfSelectedUsersInstitution;
        }
        String jpql = "select d "
                + " from Department d "
                + " where d.retired=false "
                + " and d.institution=:ins "
                + " order by d.name";
        Map m = new HashMap();
        m.put("ins", getCurrent().getInstitution());
        departmentsOfSelectedUsersInstitution = getDepartmentFacade().findByJpql(jpql, m);
        return departmentsOfSelectedUsersInstitution;
    }

    public void setDepartmentsOfSelectedUsersInstitution(List<Department> departmentsOfSelectedUsersInstitution) {
        this.departmentsOfSelectedUsersInstitution = departmentsOfSelectedUsersInstitution;
    }

    public int getManageUsersIndex() {
        return manageUsersIndex;
    }

    public void setManageUsersIndex(int manageUsersIndex) {
        this.manageUsersIndex = manageUsersIndex;
    }

    public int getManageDiscountIndex() {
        return manageDiscountIndex;
    }

    public void setManageDiscountIndex(int manageDiscountIndex) {
        this.manageDiscountIndex = manageDiscountIndex;
    }

    public String navigateToManageUsers() {
        return "/admin/users/index?faces-redirect=true;";
    }

    public List<WebUserLight> getWebUseLights() {
        return webUseLights;
    }

    public void setWebUseLights(List<WebUserLight> webUseLights) {
        this.webUseLights = webUseLights;
    }

    public WebUserLight getSelectedLight() {
        return selectedLight;
    }

    public void setSelectedLight(WebUserLight selectedLight) {
        if (selectedLight != null) {
            selected = getFacade().find(selectedLight.getId());
        } else {
            selected = null;
        }
        this.selectedLight = selectedLight;
    }

    public WebUserRole getWebUserRole() {
        return webUserRole;
    }

    public void setWebUserRole(WebUserRole webUserRole) {
        this.webUserRole = webUserRole;
    }

    public LoginPage getLoginPage() {
        return loginPage;
    }

    public void setLoginPage(LoginPage loginPage) {
        this.loginPage = loginPage;
    }

    @FacesConverter(forClass = WebUser.class)
    public static class WebUserControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            WebUserController controller = (WebUserController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "webUserController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof WebUser) {
                WebUser o = (WebUser) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + WebUserController.class.getName());
            }
        }
    }

    public List<UserNotification> getUserNotifications() {
        return userNotifications;
    }

    public void setUserNotifications(List<UserNotification> userNotifications) {
        this.userNotifications = userNotifications;
    }

    public int getUserNotificationCount() {
        if (userNotificationController.fillLoggedUserNotifications() != null) {
            userNotificationCount = userNotificationController.fillLoggedUserNotifications().size();
        }
        return userNotificationCount;
    }

    public void setUserNotificationCount(int userNotificationCount) {
        this.userNotificationCount = userNotificationCount;
    }

}
