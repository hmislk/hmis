/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;

import com.divudi.bean.hr.HrReportController;
import com.divudi.data.InvestigationItemType;
import com.divudi.entity.Category;
import java.util.TimeZone;
import com.divudi.facade.FormFormatFacade;
import com.divudi.entity.FormFormat;
import com.divudi.entity.Staff;
import com.divudi.entity.lab.CommonReportItem;
import com.divudi.facade.CommonReportItemFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class FormFormatController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private FormFormatFacade ejbFacade;
    List<FormFormat> selectedItems;
    private FormFormat current;
    private List<FormFormat> items = null;
    String selectText = "";

    List<Staff> staffes;
    @EJB
    StaffFacade staffFacade;
    @EJB
    private CommonReportItemFacade criFacade;
    Category formCategory;
    @Inject
    HrReportController hrReportController;
    Staff staff;
    private List<CommonReportItem> formItems = null;

    public void fillStaffDetailReport() {
        if (formCategory == null) {
            JsfUtil.addErrorMessage("Form ?");
            return;
        }
        String j;
        Map m = new HashMap();
        
        j = "select s from Staff s where s.retired=false ";
        
//        if(hrReportController.getReportKeyWord().getDepartment()!=null){
//            j+= " and s.workingDepartment =:dep ";
//            m.put("dep", hrReportController.getReportKeyWord().getDepartment());
//        }
//        
//        if(hrReportController.getReportKeyWord().getInstitution()!=null){
//            j+= " and s.workingDepartment.institution =:ins ";
//            m.put("ins", hrReportController.getReportKeyWord().getInstitution());
//        }
        
        j+= " order by s.person.name ";
        
        staffes = staffFacade.findBySQL(j);

        j = "SELECT i FROM CommonReportItem i where i.retired=false and i.category=:cat order by i.cssTop, i.cssLeft, i.id";
        m.put("cat", formCategory);
        formItems = criFacade.findBySQL(j, m);

    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }
    
    

    public Category getFormCategory() {
        return formCategory;
    }

    public void setFormCategory(Category formCategory) {
        this.formCategory = formCategory;
    }

    public List<Staff> getStaffes() {
        return staffes;
    }

    public void setStaffes(List<Staff> staffes) {
        this.staffes = staffes;
    }

    public List<CommonReportItem> getFormItems() {
        return formItems;
    }

    public void setFormItems(List<CommonReportItem> formItems) {
        this.formItems = formItems;
    }

    
    
    public List<FormFormat> getSelectedItems() {
        selectedItems = getFacade().findBySQL("select c from FormFormat c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new FormFormat();
    }

    public void setSelectedItems(List<FormFormat> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public FormFormatFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(FormFormatFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public FormFormatController() {
    }

    public FormFormat getCurrent() {
        if (current == null) {
            current = new FormFormat();
        }
        return current;
    }

    public void setCurrent(FormFormat current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private FormFormatFacade getFacade() {
        return ejbFacade;
    }

    public List<FormFormat> getItems() {
        // items = getFacade().findAll("name", true);
        String sql = "SELECT i FROM FormFormat i where i.retired=false order by i.name";
        items = getEjbFacade().findBySQL(sql);
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }
    /**
     *
     */
}
