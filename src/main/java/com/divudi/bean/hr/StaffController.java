/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.FormItemValue;
import com.divudi.bean.common.SessionController;

import com.divudi.data.InvestigationItemType;
import com.divudi.data.Sex;
import com.divudi.data.hr.EmployeeStatus;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.data.hr.SalaryPaymentFrequency;
import com.divudi.data.hr.SalaryPaymentMethod;
import com.divudi.entity.Category;
import com.divudi.entity.Consultant;
import com.divudi.entity.Department;
import com.divudi.entity.Doctor;
import com.divudi.entity.Person;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.Roster;
import com.divudi.entity.hr.StaffDesignation;
import com.divudi.entity.hr.StaffEmployeeStatus;
import com.divudi.entity.hr.StaffEmployment;
import com.divudi.entity.hr.StaffGrade;
import com.divudi.entity.hr.StaffSalary;
import com.divudi.entity.hr.StaffStaffCategory;
import com.divudi.entity.hr.StaffWorkingDepartment;
import com.divudi.entity.lab.CommonReportItem;
import com.divudi.entity.lab.ReportItem;
import com.divudi.facade.CommonReportItemFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.FormItemValueFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.StaffEmploymentFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.StaffSalaryFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
import org.apache.commons.io.IOUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
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
    @Inject
    HrReportController hrReportController;
    @Inject
    StaffSalaryController staffSalaryController;
    @Inject
    CommonController commonController;
    ////
    @EJB
    private StaffEmploymentFacade staffEmploymentFacade;
    @EJB
    private StaffFacade ejbFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    StaffSalaryFacade staffSalaryFacade;
    List<Staff> selectedItems;
    List<Staff> selectedList;
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
    List<Staff> itemsToRemove;
    Date tempRetireDate = null;
    boolean removeResign = false;

    public void removeSelectedItems() {
        for (Staff s : itemsToRemove) {
            s.setRetired(true);
            s.setRetireComments("Bulk Remove");
            s.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(s);
        }
        itemsToRemove = null;
        items = null;
    }

    public void deleteStaff() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        current.setRetired(true);
        getFacade().edit(current);
        fillItems();
    }

    public FormItemValue formItemValue(ReportItem ri, Person p) {
        if (ri == null || p == null) {
            ////System.out.println("ri = " + ri);
            ////System.out.println("p = " + p);
            return null;
        }
        String jpql;
        jpql = "select v from FormItemValue v where v.person=:p and v.reportItem=:ri";
        Map m = new HashMap();
        m.put("p", p);
        m.put("ri", ri);
        FormItemValue v = fivFacade.findFirstByJpql(jpql, m);
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
        listFormItems();
    }

    public void listFormItems() {
        ////System.out.println("getting form items");
        String temSql;
        ////System.out.println("formCategory = " + formCategory);
        if (formCategory != null) {
            temSql = "SELECT i FROM CommonReportItem i where i.retired=false and i.category=:cat order by i.name";
            Map m = new HashMap();
            m.put("cat", formCategory);
            formItems = criFacade.findByJpql(temSql, m);
        } else {
            formItems = new ArrayList<>();
        }

        fivs = new ArrayList<>();
        for (CommonReportItem crf : formItems) {
            if (crf.getIxItemType() == InvestigationItemType.ItemsCatetgory || crf.getIxItemType() == InvestigationItemType.Value) {
                FormItemValue fiv = formItemValue(crf, getCurrent().getPerson());
                fivs.add(fiv);
            }
        }
    }

    List<FormItemValue> fivs = new ArrayList<>();

    public List<FormItemValue> getFivs() {
        return fivs;
    }

    public void setFivs(List<FormItemValue> fivs) {
        this.fivs = fivs;
    }

    public List<CommonReportItem> getFormItems() {
        return formItems;
    }

    public void setFormItems(List<CommonReportItem> formItems) {
        this.formItems = formItems;
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

        staffWithCode = getEjbFacade().findByJpql(sql);
    }

    public void createStaffList() {

        String sql = "select s from Staff s where "
                + " s.retired=false "
                + " order by s.person.name ";

        staffWithCode = getEjbFacade().findByJpql(sql);
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
        staffWithCode = getEjbFacade().findByJpql(sql, hm);
    }

    public void createDoctorsOnly() {

        String sql = "select s from Staff s where "
                + " s.retired=false "
                + " and (type(s)=:class1"
                + " or type(s)=:class2)"
                + " order by s.code ";
        HashMap hm = new HashMap();
        hm.put("class1", Doctor.class);
        hm.put("class2", Consultant.class);
        staffWithCode = getEjbFacade().findByJpql(sql, hm);
    }

    public void createStaffWithCode() {
        HashMap hm = new HashMap();
        hm.put("class", Consultant.class);
        String sql = "select p from Staff p "
                + " where p.retired=false "
                + " and type(p)!=:class"
                + " and LENGTH(p.code) > 0 "
                + " and LENGTH(p.person.name) > 0 "
                + " order by p.codeInterger ";

        //////System.out.println(sql);
        staffWithCode = getEjbFacade().findByJpql(sql, hm);

    }

    ReportKeyWord reportKeyWord;

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public CommonReportItemFacade getCriFacade() {
        return criFacade;
    }

    public void setCriFacade(CommonReportItemFacade criFacade) {
        this.criFacade = criFacade;
    }

    Date fromDate;
    Date toDate;

    public FormItemValueFacade getFivFacade() {
        return fivFacade;
    }

    public void setFivFacade(FormItemValueFacade fivFacade) {
        this.fivFacade = fivFacade;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public void createActiveStaffTable(Date ssDate) {
        Date startTime = new Date();
        Date toDate = null;

        HashMap hm = new HashMap();
        hm.put("class", Consultant.class);
        String sql = "select ss from Staff ss "
                + " where ss.retired=false "
                + " and type(ss)!=:class "
                + " and LENGTH(ss.code) > 0 "
                + " and LENGTH(ss.person.name) > 0 "
                + " and ss.employeeStatus!=:sts";

        sql += " and (ss.dateLeft is null or ss.dateLeft > :to ) ";
        hm.put("to", ssDate);

        hm.put("sts", EmployeeStatus.Temporary);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.workingDepartment=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getInstitution() != null) {
            sql += " and ss.workingDepartment.institution=:ins ";
            hm.put("ins", getReportKeyWord().getInstitution());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " order by ss.codeInterger ";
        staffWithCode = getEjbFacade().findByJpql(sql, hm, TemporalType.DATE);
        selectedStaffes = staffWithCode;
        fetchWorkDays(staffWithCode);

        
    }

    public void createResignedStaffTable() {
        HashMap hm = new HashMap();
        hm.put("class", Consultant.class);
        String sql = "select ss from Staff ss "
                + " where ss.retired=false "
                + " and type(ss)!=:class "
                + " and LENGTH(ss.code) > 0 "
                + " and LENGTH(ss.person.name) > 0 "
                + " and ss.employeeStatus!=:sts"
                + " and ss.dateLeft >:fd "
                + " and ss.dateLeft < :to ";
        hm.put("to", staffSalaryController.getSalaryCycle().getSalaryToDate());
        hm.put("fd", staffSalaryController.getSalaryCycle().getSalaryFromDate());

        hm.put("sts", EmployeeStatus.Temporary);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.workingDepartment=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getInstitution() != null) {
            sql += " and ss.workingDepartment.institution=:ins ";
            hm.put("ins", getReportKeyWord().getInstitution());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " order by ss.codeInterger ";
        staffWithCode = getEjbFacade().findByJpql(sql, hm, TemporalType.DATE);
        selectedStaffes = staffWithCode;
        fetchWorkDays(staffWithCode);
    }

    public void createActiveStaffOnylSalaryGeneratedTable() {
        staffWithCode = new ArrayList<>();
        hrReportController.setReportKeyWord(reportKeyWord);
        hrReportController.getReportKeyWord().setSalaryCycle(staffSalaryController.getSalaryCycle());
        staffWithCode = hrReportController.fetchOnlySalaryGeneratedStaff();
        fetchWorkDays(staffWithCode);
    }

    public void createActiveStaffOnylSalaryNotGeneratedTable(Date ssDate) {
        List<Staff> salaryGeneratedStaff = new ArrayList<>();
        hrReportController.getReportKeyWord().setSalaryCycle(staffSalaryController.getSalaryCycle());
        salaryGeneratedStaff = hrReportController.fetchOnlySalaryGeneratedStaff();
        createActiveStaffTable(ssDate);
        staffWithCode.removeAll(salaryGeneratedStaff);
        fetchWorkDays(staffWithCode);
    }

    public void createActiveStaffTable() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        HashMap hm = new HashMap();
        hm.put("class", Consultant.class);
        String sql = "select ss from Staff ss "
                + " where ss.retired=false "
                + " and type(ss)!=:class "
                + " and LENGTH(ss.code) > 0 "
                + " and LENGTH(ss.person.name) > 0 "
                + " and ss.employeeStatus!=:sts";

//        sql += " and (ss.dateLeft is null or ss.dateLeft > :to ) ";
//        hm.put("to", ssDate );
        hm.put("sts", EmployeeStatus.Temporary);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.workingDepartment=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getInstitution() != null) {
            sql += " and ss.workingDepartment.institution=:ins ";
            hm.put("ins", getReportKeyWord().getInstitution());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " order by ss.codeInterger ";
        ////System.out.println(sql);
        ////System.out.println("hm = " + hm);
        staffWithCode = getEjbFacade().findByJpql(sql, hm, TemporalType.DATE);

        

    }

    public void fetchWorkDays(List<Staff> staffs) {
        for (Staff s : staffs) {
            if (staffSalaryController.getSalaryCycle() != null) {
                s.setTransWorkedDays(hrReportController.fetchWorkedDays(s, staffSalaryController.getSalaryCycle().getDayOffPhFromDate(), staffSalaryController.getSalaryCycle().getDayOffPhToDate()));
                s.setTransWorkedDaysSalaryFromToDate(hrReportController.fetchWorkedDays(s, staffSalaryController.getSalaryCycle().getSalaryFromDate(), staffSalaryController.getSalaryCycle().getSalaryToDate()));

            }
        }
    }

    public void createTable() {
        HashMap hm = new HashMap();
        hm.put("class", Consultant.class);
        String sql = "select ss from Staff ss "
                + " where ss.retired=false "
                + " and type(ss)!=:class "
                + " and LENGTH(ss.code) > 0 "
                + " and LENGTH(ss.person.name) > 0 ";

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.workingDepartment=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " order by ss.codeInterger ";
        //////System.out.println(sql);
        staffWithCode = getEjbFacade().findByJpql(sql, hm);

    }

    public List<Staff> completeStaffCode(String query) {
        List<Staff> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select p from Staff p "
                    + " where p.retired=false "
                    + " and LENGTH(p.code) > 0 "
                    + " and LENGTH(p.person.name) > 0 "
                    + " and ((p.person.name) like '%" + query.toUpperCase() + "%' "
                    + " or (p.code) like '%" + query.toUpperCase() + "%' )"
                    + " order by p.person.name";

            //////System.out.println(sql);
            suggestions = getEjbFacade().findByJpql(sql, 20);
        }
        return suggestions;
    }

    public List<Staff> completeConsultant(String query) {
        List<Staff> suggestions;
        String sql;
        HashMap hm = new HashMap();
        hm.put("class", Consultant.class);
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select s from Staff s "
                    + " where s.retired=false "
                    + " and type(s)=:class "
                    + " and LENGTH(s.person.name) > 0 "
                    + " and (s.person.name) like '%" + query.toUpperCase() + "%' "
                    + " order by s.person.name";

            //////System.out.println(sql);
            suggestions = getEjbFacade().findByJpql(sql, hm, 20);
        }
        return suggestions;
    }

    public List<Staff> completeStaffCodeChannel(String query) {
        List<Staff> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select p from Staff p "
                    + " where p.retired=false "
                    + " and LENGTH(p.code) > 0 "
                    + " and LENGTH(p.person.name) > 0 "
                    + " and ((p.person.name) like '%" + query.toUpperCase() + "%' "
                    + " or (p.code)='" + query.toUpperCase() + "' )"
                    + " order by p.person.name";

            //////System.out.println(sql);
            suggestions = getEjbFacade().findByJpql(sql, 20);
        }
        return suggestions;
    }

    public List<Staff> completeStaffCodeChannelWithOutResignOrRetierd(String query) {
        List<Staff> suggestions;
        String sql;
        Map m = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select p from Staff p "
                    + " where p.retired=false "
                    + " and (p.dateLeft is null or p.dateLeft>:cd)"
                    + " and LENGTH(p.code) > 0 "
                    + " and LENGTH(p.person.name) > 0 "
                    + " and ((p.person.name) like '%" + query.toUpperCase() + "%' "
                    + " or (p.code)='" + query.toUpperCase() + "' )"
                    + " order by p.person.name";

            m.put("cd", new Date());

            //////System.out.println(sql);
            suggestions = getEjbFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 20);
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
        //////System.out.println("gettin ins dep ");
        if (getCurrent().getInstitution() == null) {
            return new ArrayList<>();
        } else {
            String sql = "Select d From Department d where d.retired=false and"
                    + " d.institution=:ins";
            HashMap hm = new HashMap();
            hm.put("ins", getCurrent().getInstitution());
            d = getDepartmentFacade().findByJpql(sql, hm);
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
                    + " ((p.person.name) like :q or  "
                    + " (p.code) like :q )"
                    + " order by p.person.name";
            //////System.out.println(sql);
            HashMap hm = new HashMap();
            hm.put("q", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findByJpql(sql, hm, 20);
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
                + " and ((p.person.name) like :q "
                + " or  (p.code) like :q )"
                + " order by p.person.name";
        //////System.out.println(sql);
        HashMap hm = new HashMap();
        hm.put("rs", roster);
        hm.put("q", "%" + query.toUpperCase() + "%");
        suggestions = getFacade().findByJpql(sql, hm, 20);

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
//            //////System.out.println(sql);
        hm.put("sp", speciality);
        ss = getFacade().findByJpql(sql, hm);

        return ss;
    }

    public List<Staff> completeStaffWithoutDoctors(String query) {
        List<Staff> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select p from Staff p where p.retired=false and "
                    + "((p.person.name) like '%" + query.toUpperCase() + "%' or "
                    + " (p.code) like '%" + query.toUpperCase() + "%' ) and type(p) != Doctor"
                    + " order by p.person.name";
            //////System.out.println(sql);
            suggestions = getFacade().findByJpql(sql, 20);
        }
        return suggestions;
    }

    public String saveSignature() {
        InputStream in;
        if (file == null || "".equals(file.getFileName())) {
            return "";
        }
        if (file == null) {
            JsfUtil.addErrorMessage("Please select an image");
            return "";
        }
        if (getCurrent().getId() == null || getCurrent().getId() == 0) {
            JsfUtil.addErrorMessage("Please select staff member");
            return "";
        }
        //////System.out.println("file name is not null");
        //////System.out.println(file.getFileName());
        try {
            in = getFile().getInputStream();
            getCurrent().setFileName(file.getFileName());
            getCurrent().setFileType(file.getContentType());
            getCurrent().setBaImage(IOUtils.toByteArray(in));
            getFacade().edit(getCurrent());
            return "";
        } catch (Exception e) {
            //////System.out.println("Error " + e.getMessage());
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

                InputStream targetStream = new ByteArrayInputStream(temImg.getBaImage());
                StreamedContent str = DefaultStreamedContent.builder().contentType(temImg.getFileType()).name(temImg.getFileName()).stream(() -> targetStream).build();

//                return new DefaultStreamedContent(new ByteArrayInputStream(temImg.getBaImage()), temImg.getFileType());
                return str;
            } else {
                return new DefaultStreamedContent();
            }
        }
    }

    public StreamedContent getSignature() {
//        FacesContext context = FacesContext.getCurrentInstance();
//        if (context.getRenderResponse()) {
//            //////System.out.println("render response");
//            return new DefaultStreamedContent();
//        } else {
        //////System.out.println("image resuest");

        if (current == null) {
            //////System.out.println("staff null");
            return new DefaultStreamedContent();
        }
        //////System.out.println("staf is " + current);
        if (current.getId() != null && current.getBaImage() != null) {
            //////System.out.println(current.getFileType());
            //////System.out.println(current.getFileName());
//            return new DefaultStreamedContent(new ByteArrayInputStream(current.getBaImage()), current.getFileType(), current.getFileName());

            InputStream targetStream = new ByteArrayInputStream(current.getBaImage());
            StreamedContent str = DefaultStreamedContent.builder().contentType(current.getFileType()).name(current.getFileName()).stream(() -> targetStream).build();

            return str;
        } else {
            //////System.out.println("nulls");
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

        Staff temStaff = getFacade().findFirstByJpql("select s from Staff s where s.baImage != null and s.id = " + stfId);

        //////System.out.println("Printing");
        if (temStaff == null) {
            return new DefaultStreamedContent();
        } else if (temStaff.getId() != null && temStaff.getBaImage() != null) {
            //////System.out.println(temStaff.getFileType());
            //////System.out.println(temStaff.getFileName());

            InputStream targetStream = new ByteArrayInputStream(temStaff.getBaImage());
            StreamedContent str = DefaultStreamedContent.builder().contentType(temStaff.getFileType()).name(temStaff.getFileName()).stream(() -> targetStream).build();
            return str;
//            return new DefaultStreamedContent(new ByteArrayInputStream(temStaff.getBaImage()), temStaff.getFileType(), temStaff.getFileName());
        } else {
            return new DefaultStreamedContent();
        }
    }

    public List<Staff> getSelectedItems() {

        /**
         *
         *
         *
         *
         * sql = "select ss from Staff ss " + " where ss.retired=false " + " and
         * type(ss)!=:class " + " and ss.codeInterger!=0 ";
         *
         *
         *
         */
        String sql = "";
        HashMap hm = new HashMap();
        if (selectText.trim().equals("")) {
            sql = "select c from Staff c "
                    + " where c.retired=false "
                    //                    + " and type(c)!=:class"
                    + " order by c.person.name";
        } else {
            sql = "select c from Staff c"
                    + " where c.retired=false "
                    //                    + " and type(c)!=:class"
                    + " and ((c.person.name) like :q or (c.code) like :p) "
                    + " order by c.person.name";
            hm.put("q", "%" + getSelectText().toUpperCase() + "%");
            hm.put("p", "%" + getSelectText().toUpperCase() + "%");
        }

//        hm.put("class", Consultant.class);
        selectedItems = getFacade().findByJpql(sql, hm);

        return selectedItems;
    }

    public void resetWorkingHour() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = "select c from Staff c "
                + " where c.retired=false "
                + " and type(c)!=:class"
                + " order by c.person.name";

        hm.put("class", Consultant.class);
        selectedItems = getFacade().findByJpql(sql, hm);

        for (Staff stf : selectedItems) {
            stf.setWorkingTimeForOverTimePerWeek(45);
            stf.setWorkingTimeForNoPayPerWeek(28);
            getFacade().edit(stf);
        }

    }

    public void resetLateInEarlyOutLeaveAllowed() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = "select c from Staff c "
                + " where c.retired=false "
                + " and type(c)!=:class"
                + " order by c.person.name";

        hm.put("class", Consultant.class);
        selectedItems = getFacade().findByJpql(sql, hm);

        for (Staff stf : selectedItems) {
            stf.setAllowedEarlyOutLeave(true);
            stf.setAllowedLateInLeave(true);
            getFacade().edit(stf);
        }

    }

    public List<Staff> completeItems(String qry) {
        HashMap hm = new HashMap();
        String sql = "select c from Staff c "
                + " where c.retired=false "
                + " and (c.person.name) like :q "
                + " or (c.code) like :q "
                + " order by c.person.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        List<Staff> s = getFacade().findByJpql(sql, hm, 20);
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
        tempRetireDate = null;
        removeResign = false;
    }

    public void delete() {
        if (current != null) {
            if (current.getId() == null) {
                JsfUtil.addSuccessMessage("Nothing To Delete");
            } else {

                current.setRetired(true);
                current.setRetiredAt(new Date());
                current.setRetirer(getSessionController().getLoggedUser());
                getFacade().edit(current);
                JsfUtil.addSuccessMessage("Deleted Successfully");
            }
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
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
        tempRetireDate = null;
    }

    public void saveSelected() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }
        if (current.getPerson() == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }
        if (current.getSpeciality() == null) {
            JsfUtil.addErrorMessage("Plaese Select Speciality.");
            return;
        }

        if (current.getPerson().getLastName() == null || current.getPerson().getLastName().isEmpty()) {
            JsfUtil.addErrorMessage("Last Name Requied To Save");
            return;
        }

        if (current.getPerson().getInitials() == null || current.getPerson().getInitials().isEmpty()) {
            JsfUtil.addErrorMessage("Initials Requied To Save");
            return;
        }

        if (current.getPerson().getFullName() == null || current.getPerson().getFullName().isEmpty()) {
            JsfUtil.addErrorMessage("Full Name Requied To Save");
            return;
        }

        if (current.getPerson().getNameWithInitials() == null) {
            JsfUtil.addErrorMessage("Name With Initials Requied To Save");
            return;
        }

        if (removeResign) {
            current.setDateLeft(null);
            removeResign = false;
        } else {
            if (tempRetireDate != null && checkDateBetwenSalaryCycle(tempRetireDate)) {
                JsfUtil.addErrorMessage("This Retire Date Inside in Salary Cycle. Please Check and add Retire date");
                tempRetireDate = null;
                return;
            }
        }

        if (tempRetireDate != null) {
            current.setDateLeft(tempRetireDate);
        }

        ////System.out.println("current.getId() = " + current.getId());
        ////System.out.println("current.getPerson().getId() = " + current.getPerson().getId());
//        if (current.getPerson().getId() == null || current.getPerson().getId() == 0) {
//            getPersonFacade().create(current.getPerson());
//        } else {
//            getPersonFacade().edit(current.getPerson());
//        }
        getCurrent().chageCodeToInteger();

        if (getCurrent().getPerson().getDob() != null && getCurrent().getPerson().getSex() != null) {
            Calendar dob = Calendar.getInstance();
            dob.setTime(getCurrent().getPerson().getDob());
            Calendar dor = Calendar.getInstance();
            dor.setTime(getCurrent().getPerson().getDob());
            if (getCurrent().getPerson().getSex() == Sex.Female) {
                dor.set(Calendar.YEAR, (dob.get(Calendar.YEAR) + 50));
            }
            if (getCurrent().getPerson().getSex() == Sex.Male) {
                dor.set(Calendar.YEAR, (dob.get(Calendar.YEAR) + 55));
            }
            if (getCurrent().getPerson().getSex() == Sex.Male || getCurrent().getPerson().getSex() == Sex.Female) {
                if (getCurrent().getDateRetired() != null) {
//                    if (dor.getTime().after(getCurrent().getDateRetired())) {
//                        getCurrent().setDateRetired(dor.getTime());
//                    }
                    getCurrent().setDateRetired(dor.getTime());
                } else {
                    getCurrent().setDateRetired(dor.getTime());
                }

            }
        }
        if (getCurrent().isWithOutNotice()) {
            getCurrent().setDateWithOutNotice(null);
        }

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
//            current.getPerson().setEditer(getSessionController().getLoggedUser());
//            current.getPerson().setEditedAt(new Date());
            getPersonFacade().edit(current.getPerson());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Staff Details Updated");
        } else {
            current.getPerson().setCreatedAt(new Date());
            current.getPerson().setCreater(getSessionController().getLoggedUser());
            getPersonFacade().create(current.getPerson());

            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("New Staff Created");
        }

        updateStaffEmployment();

        recreateModel();
        getItems();
    }

    public void updateFormItem(FormItemValue fi) {
        if (fi == null) {
            ////System.out.println("fi = " + fi);
            return;
        }
        fivFacade.edit(fi);
        ////System.out.println("fi updates " + fi);
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

    public void listenerWithNotice() {
        if (getCurrent().isWithOutNotice()) {
            getCurrent().setDateWithOutNotice(null);
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
        tempRetireDate = null;
        removeResign = false;
        listFormItems();
    }

    private StaffFacade getFacade() {
        return ejbFacade;
    }

    List<Staff> staffes;
    List<Staff> selectedStaffes;
    double resetStaffBalance;

    public double getResetStaffBalance() {
        return resetStaffBalance;
    }

    public void setResetStaffBalance(double resetStaffBalance) {
        this.resetStaffBalance = resetStaffBalance;
    }

    public List<Staff> getSelectedStaffes() {
        return selectedStaffes;
    }

    public void setSelectedStaffes(List<Staff> selectedStaffes) {
        this.selectedStaffes = selectedStaffes;
    }

    public List<Staff> getStaffes() {
        return staffes;
    }

    public void setStaffes(List<Staff> staffes) {
        this.staffes = staffes;
    }

    public String navigateToAdminStaffViewSignature() {
        fillStaffes();
        return "/admin/staff/admin_staff_view_signature?faces-redirect=true";
    }

    public String admin_edit_staff_balance() {
        fillStaffes();
        return "/admin_edit_staff_balance";
    }

    public void resetStaffBalance() {
        for (Staff s : selectedStaffes) {
            s.setAnnualWelfareUtilized(resetStaffBalance);
            getFacade().edit(s);
//            getFacade().flush();
        }
        JsfUtil.addSuccessMessage("Balances Updated");
    }

    public void fillStaffes() {
        String temSql;
        temSql = "SELECT i FROM Staff i where i.retired=false and i.person is not null and i.person.name is not null order by i.person.name";
        staffes = getFacade().findByJpql(temSql);
    }

    public List<Staff> getItemsToRemove() {
        return itemsToRemove;
    }

    public void setItemsToRemove(List<Staff> itemsToRemove) {
        this.itemsToRemove = itemsToRemove;
    }

    public List<Staff> getItems() {
        if (items == null) {
            String temSql;
            temSql = "SELECT i FROM Staff i where i.retired=false and i.person is not null and i.person.name is not null order by i.person.name";
            items = getFacade().findByJpql(temSql);
        }
        return items;
    }

    public void fillItems() {
        String temSql;
        temSql = "SELECT i FROM Staff i where i.retired=false and i.person is not null and i.person.name is not null order by i.person.name";
        items = getFacade().findByJpql(temSql);
    }

    public Staff findStaffByName(String name) {
        String jpql = "select c "
                + " from Staff c "
                + " where c.retired=:ret "
                + " and c.person.name=:name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("name", name);
        return getFacade().findFirstByJpql(jpql, m);
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

    public Date getTempRetireDate() {
        return tempRetireDate;
    }

    public void setTempRetireDate(Date tempRetireDate) {
        this.tempRetireDate = tempRetireDate;
    }

    private boolean checkDateBetwenSalaryCycle(Date tempReDate) {
        String sql;
        Map m = new HashMap();

        sql = "select c from StaffSalary c "
                + " where c.retired=false "
                + " and c.salaryCycle.retired=false "
                + " and c.staff=:s "
                + " and c.salaryCycle.salaryFromDate<=:d "
                + " and c.salaryCycle.salaryToDate>=:d ";

        m.put("d", tempReDate);
        m.put("s", getCurrent());

        List<StaffSalary> cycles = staffSalaryFacade.findByJpql(sql, m, TemporalType.DATE);

        if (cycles.size() > 0) {
            return true;
        } else {
            return false;
        }

    }

    public boolean isRemoveResign() {
        return removeResign;
    }

    public void setRemoveResign(boolean removeResign) {
        this.removeResign = removeResign;
    }

    /**
     * Converters
     */
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

    public String navigateToManageStaff() {
        return "/admin/staff/admin_manage_staff_index.xhtml";
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

}
