/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.FormItemValue;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.hr.EmployeeStatus;
import com.divudi.data.hr.SalaryPaymentFrequency;
import com.divudi.data.hr.SalaryPaymentMethod;
import com.divudi.entity.Category;
import com.divudi.entity.Consultant;
import com.divudi.entity.Department;
import com.divudi.entity.Doctor;
import java.util.TimeZone;
import com.divudi.entity.Person;
import com.divudi.entity.Speciality;
import com.divudi.facade.StaffFacade;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.Roster;
import com.divudi.entity.hr.StaffDesignation;
import com.divudi.entity.hr.StaffEmployeeStatus;
import com.divudi.entity.hr.StaffEmployment;
import com.divudi.entity.hr.StaffGrade;
import com.divudi.entity.hr.StaffStaffCategory;
import com.divudi.entity.hr.StaffWorkingDepartment;
import com.divudi.entity.lab.CommonReportItem;
import com.divudi.entity.lab.PatientReportItemValue;
import com.divudi.entity.lab.ReportItem;
import com.divudi.facade.CommonReportItemFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.FormFacade;
import com.divudi.facade.FormItemValueFacade;
import com.divudi.facade.PatientReportItemValueFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.StaffEmploymentFacade;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import org.apache.commons.io.IOUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class StaffController implements Serializable {

    StreamedContent scCircular;
    StreamedContent scCircularById;
    private UploadedFile file;
    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    ////
    @EJB
    private StaffEmploymentFacade staffEmploymentFacade;
    @EJB
    private StaffFacade ejbFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    List<Staff> selectedItems;
    private List<Staff> selectedList;
    private List<Staff> filteredStaff;
    private Staff selectedStaff;
    private Staff current;
    List<Staff> staffWithCode;
    private List<Staff> items = null;
    String selectText = "";

    @EJB
    private CommonReportItemFacade criFacade;
    @EJB
    FormItemValueFacade fivFacade;
    Category formCategory;
    private List<CommonReportItem> formItems = null;

    public FormItemValue formItemValue(ReportItem ri, Person p) {
        if (ri == null || p == null) {
            System.out.println("ri = " + ri);
            System.out.println("p = " + p);
            return null;
        }
        String jpql;
        jpql = "select v from FormItemValue v where v.person=:p and v.reportItem=:ri";
        Map m = new HashMap();
        m.put("p", p);
        m.put("ri", ri);
        FormItemValue v = fivFacade.findFirstBySQL(jpql, m);
        if (v == null) {
            v = new FormItemValue();
            v.setPerson(p);
            v.setReportItem(ri);
            fivFacade.create(v);
        }

        return v;
    }

    public Category getFormCategory() {
        return formCategory;
    }

    public void setFormCategory(Category formCategory) {
        this.formCategory = formCategory;
    }

    public void listFormItems() {
        System.out.println("getting form items");
        String temSql;
        System.out.println("formCategory = " + formCategory);
        if (formCategory != null) {
            temSql = "SELECT i FROM CommonReportItem i where i.retired=false and i.category=:cat order by i.name";
            Map m = new HashMap();
            m.put("cat", formCategory);
            formItems = criFacade.findBySQL(temSql, m);
        } else {
            formItems = new ArrayList<>();
        }
    }

    public List<CommonReportItem> getFormItems() {
        return formItems;
    }

    public void setFormItems(List<CommonReportItem> formItems) {
        this.formItems=formItems;
    }

    
    public List<Staff> getStaffWithCode() {
        return staffWithCode;
    }

    public void setStaffWithCode(List<Staff> staffWithCode) {
        this.staffWithCode = staffWithCode;
    }

    public void createStaffListWithOutSpecility() {

        String sql = "select s from Staff s where "
                + " s.retired=false "
                + " and s.speciality is null "
                + " order by s.person.name ";

        staffWithCode = getEjbFacade().findBySQL(sql);
    }

    public void createStaffList() {

        String sql = "select s from Staff s where "
                + " s.retired=false "
                + " order by s.person.name ";

        staffWithCode = getEjbFacade().findBySQL(sql);
    }

    public void createStaffOnly() {

        String sql = "select s from Staff s where "
                + " s.retired=false "
                + " and (type(s)!=:class1"
                + " and type(s)!=:class2)"
                + " order by s.code ";
        HashMap hm = new HashMap();
        hm.put("class1", Doctor.class);
        hm.put("class2", Consultant.class);
        staffWithCode = getEjbFacade().findBySQL(sql, hm);
    }

    public void createStaffWithCode() {
        HashMap hm = new HashMap();
        hm.put("class", Consultant.class);
        String sql = "select p from Staff p "
                + " where p.retired=false "
                + " and type(p)!=:class"
                + " and LENGTH(p.code) > 0 "
                + " and LENGTH(p.person.name) > 0 "
                + " order by p.person.name ";

        //System.out.println(sql);
        staffWithCode = getEjbFacade().findBySQL(sql, hm);

    }

    public List<Staff> completeStaffCode(String query) {
        List<Staff> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select p from Staff p where p.retired=false and"
                    + " (upper(p.person.name) like '%" + query.toUpperCase() + "%'or"
                    + "  upper(p.code) like '%" + query.toUpperCase() + "%' )"
                    + " order by p.person.name";

            //System.out.println(sql);
            suggestions = getEjbFacade().findBySQL(sql, 20);
        }
        return suggestions;
    }

    public void makeNull() {
        items = null;
        selectedStaff = null;
        filteredStaff = null;
        current = null;
        selectedItems = null;
        selectedList = null;
        selectedStaff = null;
        staffWithCode = null;

    }

    public EmployeeStatus[] getEmployeeStatuses() {
        return EmployeeStatus.values();
    }

    public SalaryPaymentFrequency[] getPayingMethod() {
        return SalaryPaymentFrequency.values();
    }

    public SalaryPaymentMethod[] getSalaryPaymentMethods() {
        return SalaryPaymentMethod.values();
    }

    public List<Department> getInstitutionDepatrments() {
        List<Department> d;
        //System.out.println("gettin ins dep ");
        if (getCurrent().getInstitution() == null) {
            return new ArrayList<>();
        } else {
            String sql = "Select d From Department d where d.retired=false and"
                    + " d.institution=:ins";
            HashMap hm = new HashMap();
            hm.put("ins", getCurrent().getInstitution());
            d = getDepartmentFacade().findBySQL(sql, hm, 20);
        }

        return d;
    }

    List<Staff> suggestions;

    public List<Staff> completeStaff(String query) {

        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select p from Staff p where p.retired=false  and"
                    + " (upper(p.person.name) like :q or  "
                    + " upper(p.code) like :q )"
                    + " order by p.person.name";
            //System.out.println(sql);
            HashMap hm = new HashMap();
            hm.put("q", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findBySQL(sql, hm, 20);
        }
        return suggestions;
    }
    Roster roster;

    public List<Staff> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<Staff> suggestions) {
        this.suggestions = suggestions;
    }

    public Roster getRoster() {
        return roster;
    }

    public void setRoster(Roster roster) {
        this.roster = roster;
    }

    public List<Staff> completeRosterStaff(String query) {

        String sql;

        sql = "select p from Staff p "
                + " where p.retired=false "
                + " and p.roster=:rs "
                + " and (upper(p.person.name) like :q "
                + " or  upper(p.code) like :q )"
                + " order by p.person.name";
        //System.out.println(sql);
        HashMap hm = new HashMap();
        hm.put("rs", roster);
        hm.put("q", "%" + query.toUpperCase() + "%");
        suggestions = getFacade().findBySQL(sql, hm, 20);

        return suggestions;
    }

    public List<Staff> getSpecialityStaff(Speciality speciality) {
        List<Staff> ss;
        String sql;
        HashMap hm = new HashMap();
        sql = "select p from Staff p where  "
                + " p.speciality=:sp and "
                + " p.retired=false "
                + "order by p.person.name";
//            //System.out.println(sql);
        hm.put("sp", speciality);
        ss = getFacade().findBySQL(sql, hm);

        System.err.println("Staff List Size " + ss.size());

        return ss;
    }

    public List<Staff> completeStaffWithoutDoctors(String query) {
        List<Staff> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select p from Staff p where p.retired=false and "
                    + "(upper(p.person.name) like '%" + query.toUpperCase() + "%' or "
                    + " upper(p.code) like '%" + query.toUpperCase() + "%' ) and type(p) != Doctor"
                    + " order by p.person.name";
            //System.out.println(sql);
            suggestions = getFacade().findBySQL(sql, 20);
        }
        return suggestions;
    }

    public String saveSignature() {
        InputStream in;
        if (file == null || "".equals(file.getFileName())) {
            return "";
        }
        if (file == null) {
            UtilityController.addErrorMessage("Please select an image");
            return "";
        }
        if (getCurrent().getId() == null || getCurrent().getId() == 0) {
            UtilityController.addErrorMessage("Please select staff member");
            return "";
        }
        //System.out.println("file name is not null");
        //System.out.println(file.getFileName());
        try {
            in = getFile().getInputstream();
            getCurrent().setFileName(file.getFileName());
            getCurrent().setFileType(file.getContentType());
            getCurrent().setBaImage(IOUtils.toByteArray(in));
            getFacade().edit(getCurrent());
            return "";
        } catch (Exception e) {
            //System.out.println("Error " + e.getMessage());
            return "";
        }

    }

    public StreamedContent getSignatureById() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getRenderResponse()) {
            // So, we're rendering the view. Return a stub StreamedContent so that it will generate right URL.
            return new DefaultStreamedContent();
        } else {
            // So, browser is requesting the image. Get ID value from actual request param.
            String id = context.getExternalContext().getRequestParameterMap().get("id");
            Long l;
            try {
                l = Long.valueOf(id);
            } catch (NumberFormatException e) {
                l = 0l;
            }
            Staff temImg = getFacade().find(Long.valueOf(id));
            if (temImg != null) {
                return new DefaultStreamedContent(new ByteArrayInputStream(temImg.getBaImage()), temImg.getFileType());
            } else {
                return new DefaultStreamedContent();
            }
        }
    }

    public StreamedContent getSignature() {
//        FacesContext context = FacesContext.getCurrentInstance();
//        if (context.getRenderResponse()) {
//            //System.out.println("render response");
//            return new DefaultStreamedContent();
//        } else {
        //System.out.println("image resuest");

        if (current == null) {
            //System.out.println("staff null");
            return new DefaultStreamedContent();
        }
        //System.out.println("staf is " + current);
        if (current.getId() != null && current.getBaImage() != null) {
            //System.out.println(current.getFileType());
            //System.out.println(current.getFileName());
            return new DefaultStreamedContent(new ByteArrayInputStream(current.getBaImage()), current.getFileType(), current.getFileName());
        } else {
            //System.out.println("nulls");
            return new DefaultStreamedContent();
        }
//        }

    }

    public StreamedContent displaySignature(Long stfId) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getRenderResponse()) {
            return new DefaultStreamedContent();
        }
        if (stfId == null) {
            return new DefaultStreamedContent();
        }

        Staff temStaff = getFacade().findFirstBySQL("select s from Staff s where s.baImage != null and s.id = " + stfId);

        //System.out.println("Printing");
        if (temStaff == null) {
            return new DefaultStreamedContent();
        } else {
            if (temStaff.getId() != null && temStaff.getBaImage() != null) {
                //System.out.println(temStaff.getFileType());
                //System.out.println(temStaff.getFileName());
                return new DefaultStreamedContent(new ByteArrayInputStream(temStaff.getBaImage()), temStaff.getFileType(), temStaff.getFileName());
            } else {
                return new DefaultStreamedContent();
            }
        }
    }

    public List<Staff> getSelectedItems() {
        String sql = "";
        HashMap hm = new HashMap();
        if (selectText.trim().equals("")) {
            sql = "select c from Staff c "
                    + " where c.retired=false "
                    + " and type(c)!=:class"
                    + " order by c.person.name";
        } else {
            sql = "select c from Staff c"
                    + " where c.retired=false "
                    + " and type(c)!=:class"
                    + " and upper(c.person.name) like :q "
                    + " order by c.person.name";
            hm.put("q", "%" + getSelectText().toUpperCase() + "%");
        }

        hm.put("class", Consultant.class);
        selectedItems = getFacade().findBySQL(sql, hm);

        return selectedItems;
    }

    public List<Staff> completeItems(String qry) {
        HashMap hm = new HashMap();
        String sql = "select c from Staff c "
                + " where c.retired=false "
                + " and upper(c.person.name) like :q "
                + " or upper(c.code) like :q "
                + " order by c.person.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        List<Staff> s = getFacade().findBySQL(sql, hm, 20);
        return s;
    }

    public StreamedContent getScCircular() {
        return scCircular;
    }

    public void setScCircular(StreamedContent scCircular) {
        this.scCircular = scCircular;
    }

    public StreamedContent getScCircularById() {
        return scCircularById;
    }

    public void setScCircularById(StreamedContent scCircularById) {
        this.scCircularById = scCircularById;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public void prepareAdd() {
        current = new Staff();
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("DeleteSuccessfull");
        } else {
            UtilityController.addSuccessMessage("NothingToDelete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    public void setSelectedItems(List<Staff> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
        formItems = null;
    }

    public void saveSelected() {
        if (current == null) {
            UtilityController.addErrorMessage("Nothing to save");
            return;
        }
        if (current.getPerson() == null) {
            UtilityController.addErrorMessage("Nothing to save");
            return;
        }
        if (current.getSpeciality() == null) {
            UtilityController.addErrorMessage("Plaese Select Speciality.");
            return;
        }
        if (current.getPerson().getId() == null || current.getPerson().getId() == 0) {
            getPersonFacade().create(current.getPerson());
        } else {
            getPersonFacade().edit(current.getPerson());
        }

        getCurrent().chageCodeToInteger();

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            UtilityController.addSuccessMessage("savedOldSuccessfully");
        } else {
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("savedNewSuccessfully");
        }

        updateStaffEmployment();

        recreateModel();
        getItems();
    }

    public void updateFormItem(FormItemValue fi) {
        if (fi == null) {
            System.out.println("fi = " + fi);
            return;
        }
        fivFacade.edit(fi);
        System.out.println("fi updates " + fi);
    }

    public void updateCodeToIntege() {
        for (Staff staff : ejbFacade.findAll()) {
            staff.chageCodeToInteger();
            ejbFacade.edit(staff);

        }

    }

    private void updateStaffEmployment() {

        if (getCurrent().getStaffEmployment() == null) {
            StaffEmployment se = new StaffEmployment();
            se.setCreatedAt(new Date());
            se.setCreater(getSessionController().getLoggedUser());
            se.setFromDate(new Date());
            se.setStaff(getCurrent());
            getStaffEmploymentFacade().create(se);
            getCurrent().setStaffEmployment(se);

            getEjbFacade().edit(getCurrent());
        }

        createComponent();

        getStaffEmploymentFacade().edit(getCurrent().getStaffEmployment());

    }

    private void createComponent() {
        if (getCurrent().getWorkingDepartment() != null && getCurrent().getStaffEmployment().getStaffWorkingDepartments().isEmpty()) {
            StaffWorkingDepartment tmp = new StaffWorkingDepartment();
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setFromDate(new Date());
            tmp.setDepartment(getCurrent().getWorkingDepartment());
            tmp.setStaffEmployment(getCurrent().getStaffEmployment());
            getCurrent().getStaffEmployment().getStaffWorkingDepartments().add(tmp);
        }

        if (getCurrent().getEmployeeStatus() != null && getCurrent().getStaffEmployment().getStaffEmployeeStatuss().isEmpty()) {
            StaffEmployeeStatus tmp = new StaffEmployeeStatus();
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setFromDate(new Date());
            tmp.setEmployeeStatus(getCurrent().getEmployeeStatus());
            tmp.setStaffEmployment(getCurrent().getStaffEmployment());
            getCurrent().getStaffEmployment().getStaffEmployeeStatuss().add(tmp);
        }

        if (getCurrent().getStaffCategory() != null && getCurrent().getStaffEmployment().getStaffStaffCategorys().isEmpty()) {
            StaffStaffCategory tmp = new StaffStaffCategory();
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setFromDate(new Date());
            tmp.setStaffCategory(getCurrent().getStaffCategory());
            tmp.setStaffEmployment(getCurrent().getStaffEmployment());
            getCurrent().getStaffEmployment().getStaffStaffCategorys().add(tmp);
        }

        if (getCurrent().getGrade() != null && getCurrent().getStaffEmployment().getStaffGrades().isEmpty()) {
            StaffGrade tmp = new StaffGrade();
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setFromDate(new Date());
            tmp.setGrade(getCurrent().getGrade());
            tmp.setStaffEmployment(getCurrent().getStaffEmployment());
            getCurrent().getStaffEmployment().getStaffGrades().add(tmp);
        }

        if (getCurrent().getDesignation() != null && getCurrent().getStaffEmployment().getStaffDesignations().isEmpty()) {
            StaffDesignation tmp = new StaffDesignation();
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setFromDate(new Date());
            tmp.setDesignation(getCurrent().getDesignation());
            tmp.setStaffEmployment(getCurrent().getStaffEmployment());
            getCurrent().getStaffEmployment().getStaffDesignations().add(tmp);
        }

    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public StaffFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(StaffFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public StaffController() {
    }

    public Staff getCurrent() {
        if (current == null) {
            Person p = new Person();
            current = new Staff();
            current.setPerson(p);
        }
        return current;
    }

    public void setCurrent(Staff current) {
        this.current = current;
        getSignature();
    }

    public void changeStaff() {
        formItems = null;
    }

    private StaffFacade getFacade() {
        return ejbFacade;
    }

    public List<Staff> getItems() {
        String temSql;
        temSql = "SELECT i FROM Staff i where i.retired=false and i.person is not null and i.person.name is not null order by i.person.name";
        items = getFacade().findBySQL(temSql);
        return items;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public List<Staff> getFilteredStaff() {
        return filteredStaff;
    }

    public void setFilteredStaff(List<Staff> filteredStaff) {
        this.filteredStaff = filteredStaff;
    }

    public Staff getSelectedStaff() {
        return selectedStaff;
    }

    public void setSelectedStaff(Staff selectedStaff) {
        this.selectedStaff = selectedStaff;
    }

    public StaffEmploymentFacade getStaffEmploymentFacade() {
        return staffEmploymentFacade;
    }

    public void setStaffEmploymentFacade(StaffEmploymentFacade staffEmploymentFacade) {
        this.staffEmploymentFacade = staffEmploymentFacade;
    }

    public List<Staff> getSelectedList() {
        return selectedList;
    }

    public void setSelectedList(List<Staff> selectedList) {
        this.selectedList = selectedList;
    }

    /**
     *
     */
    @FacesConverter(forClass = Staff.class)
    public static class StaffControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StaffController controller = (StaffController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "staffController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            if (value == null || value.trim().equals("null")) {
                value = "";
            }
            try {
                key = Long.valueOf(value);
            } catch (Exception e) {
                key = 0l;
            }
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
            if (object instanceof Staff) {
                Staff o = (Staff) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + StaffController.class.getName());
            }
        }
    }

    @FacesConverter("stfcon")
    public static class StaffConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StaffController controller = (StaffController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "staffController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            if (value == null || value.trim().equals("null")) {
                value = "";
            }
            try {
                key = Long.valueOf(value);
            } catch (Exception e) {
                key = 0l;
            }
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
            if (object instanceof Staff) {
                Staff o = (Staff) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + StaffController.class.getName());
            }
        }
    }
}
