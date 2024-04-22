/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.InstitutionType;
import com.divudi.entity.Institution;
import com.divudi.facade.InstitutionFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class InstitutionBranchController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private InstitutionFacade ejbFacade;
    List<Institution> selectedItems;
    private Institution current;
    private List<Institution> items = null;
    //   private String insCode;
    String selectText = "";
    private Boolean codeDisabled = false;
    private InstitutionType[] institutionTypes;

    public List<Institution> getSelectedItems() {
        String sql;
        HashMap hm = new HashMap();
        if (selectText.trim().equals("")) {
            sql = "select c from Institution c where c.institutionType=:tp and c.retired=false order by c.name";
        } else {
            sql = "select c from Institution c where c.institutionType=:tp and c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name";
        }

        hm.put("tp", InstitutionType.branch);

        selectedItems = getFacade().findByJpql(sql, hm, TemporalType.DATE);

        return selectedItems;
    }

//    public List<Institution> completeIns(String qry) {
//        String sql;
//        sql = "select c from Institution c where c.retired=false and (c.name) like '%" + qry.toUpperCase() + "%' order by c.name";
//        return getFacade().findByJpql(sql);
//    }

//    public List<Institution> completeCompany(String qry) {
//        String sql;
//        sql = "select c from Institution c where c.retired=false and c.institutionType=com.divudi.data.InstitutionType.Company and (c.name) like '%" + qry.toUpperCase() + "%' order by c.name";
//        return getFacade().findByJpql(sql);
//    }

    public List<Institution> completeCredit(String query) {
        List<Institution> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select p from Institution p where p.retired=false and p.institutionType=com.divudi.data.InstitutionType.CreditCompany and (p.name) like '%" + query.toUpperCase() + "%' order by p.name";
            //////// // System.out.println(sql);
            suggestions = getFacade().findByJpql(sql);
        }
        return suggestions;
    }

    public void prepareAdd() {
        codeDisabled = false;
        current = new Institution();
    }

    public void setSelectedItems(List<Institution> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        if (getCurrent().getInstitutionType() == null) {
            JsfUtil.addErrorMessage("Select Instituion Type");
            return;
        }
        if (getCurrent().getInstitution() == null) {
            JsfUtil.addErrorMessage("Select Main Institution");
            return;
        }
        getCurrent().getInstitution().getBranch().add(getCurrent());
        getEjbFacade().edit(getCurrent().getInstitution());
        recreateModel();
        getItems();
        current=null;
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public InstitutionFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(InstitutionFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public InstitutionBranchController() {
    }

    public Institution getCurrent() {
        if (current == null) {
            current = new Institution();
            current.setInstitutionType(InstitutionType.branch);
        }
        return current;
    }

    public void setCurrent(Institution current) {
        codeDisabled = true;
        this.current = current;
    }

    public void delete() {
        if (getCurrent() != null) {
            getCurrent().setRetired(true);
            getCurrent().setRetiredAt(new Date());
            getCurrent().setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        current = null;
    }

    private InstitutionFacade getFacade() {
        return ejbFacade;
    }

    public List<Institution> getItems() {
        return items;
    }


    public Boolean getCodeDisabled() {
        return codeDisabled;
    }

    public void setCodeDisabled(Boolean codeDisabled) {
        this.codeDisabled = codeDisabled;
    }

    public InstitutionType[] getInstitutionTypes() {
        return InstitutionType.values();
    }

    public void setInstitutionTypes(InstitutionType[] institutionTypes) {
        this.institutionTypes = institutionTypes;
    }

}
