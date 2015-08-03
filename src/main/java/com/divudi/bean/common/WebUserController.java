/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;

import java.util.TimeZone;
import com.divudi.data.Privileges;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Person;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.WebUserFacade;
import com.divudi.facade.WebUserRoleFacade;
import com.divudi.entity.WebUser;
import com.divudi.entity.WebUserPrivilege;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.WebUserPrivilegeFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import org.primefaces.event.FlowEvent;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class WebUserController implements Serializable {

    @Inject
    SessionController sessionController;
    @Inject
    SecurityController securityController;
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
    List<WebUser> items;
    List<WebUser> searchItems;
    private WebUser current;
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
    WebUser removingUser;
    private List<WebUser> webUsers;
    List<WebUser> itemsToRemove;

    public void removeSelectedItems() {
        for (WebUser s : itemsToRemove) {
            s.setRetired(true);
            s.setRetireComments("Bulk Remove");
            s.setRetirer(getSessionController().getLoggedUser());
            try {
                getFacade().edit(s);
            } catch (Exception e) {
                System.out.println("e = " + e);
            }
        }
        itemsToRemove = null;
        items = null;
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

        webUsers = getFacade().findBySQL(sql);
    }

    public List<Department> getInstitutionDepatrments() {
        List<Department> d;
        if (getInstitution() == null) {
            return new ArrayList<>();
        } else {
            String sql = "Select d From Department d where d.retired=false and d.institution.id=" + getInstitution().getId();
            d = getDepartmentFacade().findBySQL(sql);
        }

        return d;
    }

    public void saveUser() {
        if (current == null) {
            return;
        }
        if (current.getId() == null || current.getId() == 0) {
            getFacade().create(current);
            UtilityController.addSuccessMessage("Saved");
        } else {
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Updated");
        }
    }

    public WebUser getRemovingUser() {
        return removingUser;
    }

    public void setRemovingUser(WebUser removingUser) {
        this.removingUser = removingUser;
    }

    public void removeUser() {
        if (removingUser == null) {
            UtilityController.addErrorMessage("Select a user to remove");
            return;
        }
        removingUser.getWebUserPerson().setRetired(true);
        removingUser.getWebUserPerson().setRetirer(getSessionController().getLoggedUser());
        removingUser.getWebUserPerson().setRetiredAt(Calendar.getInstance().getTime());
        getPersonFacade().edit(removingUser.getWebUserPerson());

        removingUser.setName(removingUser.getId().toString());
        removingUser.setRetired(true);
        removingUser.setRetirer(getSessionController().getLoggedUser());
        removingUser.setRetiredAt(Calendar.getInstance().getTime());
        //getFacade().edit(removingUser);
        getFacade().edit(removingUser);
        UtilityController.addErrorMessage("User Removed");
    }

    public List<WebUser> completeUser(String qry) {
        List<WebUser> a = null;
        if (qry != null) {
            a = getFacade().findBySQL("select c from WebUser c where c.retired=false and  (upper(c.webUserPerson.name) like '%" + qry.toUpperCase() + "%' or upper(c.code) like '%" + qry.toUpperCase() + "%') order by c.webUserPerson.name");
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
        if (getSessionController().getLoggedUser() == null) {
            return hasPri;
        }

        if (getSessionController().getLoggedUser().getId() == null) {
            return hasPri;
        }

        for (WebUserPrivilege w : getSessionController().getUserPrivileges()) {
            if (w.getPrivilege() != null && w.getPrivilege().equals(Privileges.valueOf(privilege))) {

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
                departments = getDepartmentFacade().findBySQL(sql);
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
            institutions = getInstitutionFacade().findBySQL(sql);
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
            items = getFacade().findBySQL("Select d From WebUser d where d.retired = false order by d.webUserPerson.name");
            dycryptName();
        }

        return items;
    }

    private void dycryptName() {
        List<WebUser> temp = items;

        for (int i = 0; i < temp.size(); i++) {
            WebUser w = temp.get(i);
            w.setName(getSecurityController().decrypt(w.getName()).toLowerCase());
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

    public WebUser searchItem(String itemName, boolean createNewIfNotPresent) {
        WebUser searchedItem = null;
        List<WebUser> temItems;
        temItems = getFacade().findAll("name", itemName, true);
        if (temItems.size() > 0) {
            searchedItem = (WebUser) temItems.get(0);
        } else if (createNewIfNotPresent) {
            searchedItem = new WebUser();
            searchedItem.setName(itemName);
            searchedItem.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            searchedItem.setCreater(sessionController.loggedUser);
            getFacade().create(searchedItem);
        }
        return searchedItem;
    }

    private void recreateModel() {
        items = null;
    }

    public void prepareAdd() {
        current = new WebUser();
    }

    public void prepairAddNewUser() {
        setCurrent(new WebUser());
        Person p = new Person();
        getCurrent().setWebUserPerson(p);
        setSpeciality(null);
        currentPrivilegeses = null;

    }

    public SecurityController getSecurityController() {
        return securityController;
    }

    public void setSecurityController(SecurityController securityController) {
        this.securityController = securityController;
    }

    public Boolean userNameAvailable(String userName) {
        boolean available = false;
        List<WebUser> allUsers = getFacade().findAll("name", true);
        if (allUsers == null) {
            return false;
        }
        for (WebUser w : allUsers) {

            if (userName != null && w != null && w.getName() != null) {
                if (userName.toLowerCase().equals(getSecurityController().decrypt(w.getName()).toLowerCase())) {
                    ////System.out.println("Ift");
                    available = true;
                    return available;// ok. that is may be the issue. we will try with it ok
                }
            }
        }
        return available;
    }

    public void saveNewUser() {
        // We Deal with a new Web ser only here
        //

        if (current == null) {
            UtilityController.addErrorMessage("Nothing to save");
            return;
        }

        if (userNameAvailable(getCurrent().getName())) {
            UtilityController.addErrorMessage("User name already exists. Plese enter another user name");
            return;
        }
        Staff staff = new Staff();
        getCurrent().setActivated(true);
        getCurrent().setActivatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        getCurrent().setActivator(getSessionController().getLoggedUser());

        ////System.out.println("Start");
        //Save Person
        getCurrent().getWebUserPerson().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        getCurrent().getWebUserPerson().setCreater(getSessionController().getLoggedUser());
        getPersonFacade().create(getCurrent().getWebUserPerson());
        ////System.out.println("Person Saved");
        //Save Staff
        staff.setPerson(getCurrent().getWebUserPerson());
        staff.setCreatedAt(Calendar.getInstance().getTime());
        staff.setDepartment(department);
        staff.setInstitution(institution);
        staff.setCode(getCurrent().getCode());
        getStaffFacade().create(staff);
        //Save Web User
        getCurrent().setInstitution(getInstitution());
        getCurrent().setDepartment(getDepartment());
        getCurrent().setName(getSecurityController().encrypt(getCurrent().getName()));
        getCurrent().setWebUserPassword(getSecurityController().hash(getCurrent().getWebUserPassword()));
        getCurrent().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        getCurrent().setCreater(sessionController.loggedUser);
        getCurrent().setStaff(staff);
        getFacade().create(getCurrent());
        ////System.out.println("Web User Saved");
        //SetPrivilage
//        for (Privileges p : currentPrivilegeses) {
//            WebUserPrivilege pv = new WebUserPrivilege();
//            pv.setWebUser(current);
//            pv.setPrivilege(p);
//            pv.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
//            pv.setCreater(getSessionController().getLoggedUser());
//            getWebUserPrevilageFacade().create(pv);
//
//        }

        recreateModel();
        prepairAddNewUser();
        selectText = "";
        UtilityController.addSuccessMessage("New User Added");

    }

    public List<WebUser> getToApproveUsers() {
        String temSQL;
        temSQL = "SELECT u FROM WebUser u WHERE u.retired=false AND u.activated=false";
        return getEjbFacade().findBySQL(temSQL);
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
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(sessionController.loggedUser);
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successful");
        } else {
            UtilityController.addErrorMessage("Nothing To Delete");
        }
        recreateModel();
        getItems();
        current = null;
    }

    public void createTable() {

        if (selectText.trim().equals("")) {
            items = getFacade().findBySQL("select c from WebUser c where c.retired=false order by c.webUserPerson.name");
        } else {
            items = getFacade().findBySQL("select c from WebUser c where c.retired=false and upper(c.webUserPerson.name) like '%" + getSelectText().toUpperCase() + "%' order by c.webUserPerson.name");
        }
        dycryptName();
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

    public List<WebUser> getItemsToRemove() {
        return itemsToRemove;
    }

    public void setItemsToRemove(List<WebUser> itemsToRemove) {
        this.itemsToRemove = itemsToRemove;
    }

    @FacesConverter("webUs")
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
}
