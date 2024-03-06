/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.hr.HrReportController;
import com.divudi.entity.Category;
import com.divudi.entity.FormFormat;
import com.divudi.entity.Staff;
import com.divudi.entity.lab.CommonReportItem;
import com.divudi.facade.CommonReportItemFacade;
import com.divudi.facade.FormFormatFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
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
        j += " order by s.person.name ";

        staffes = staffFacade.findByJpql(j);

        j = "SELECT i FROM CommonReportItem i where i.retired=false and i.category=:cat order by i.cssTop, i.cssLeft, i.id";
        m.put("cat", formCategory);
        formItems = criFacade.findByJpql(j, m);

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
        selectedItems = getFacade().findByJpql("select c from FormFormat c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
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
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
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
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
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
        if (items == null) {
            String sql = "SELECT i FROM FormFormat i where i.retired=false order by i.name";
            items = getEjbFacade().findByJpql(sql);
        }
        return items;
    }
    
    public String navigateToManageForms(){
        return "/forms/index";
    }
}
