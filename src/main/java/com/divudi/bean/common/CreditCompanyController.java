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
import com.divudi.entity.Category;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.facade.InstitutionFacade;
import java.io.Serializable;
import java.util.ArrayList;
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
public class CreditCompanyController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    CommonController commonController;
    @EJB
    private InstitutionFacade ejbFacade;
    List<Institution> selectedItems;
    private Institution current;
    Item service;
    Category category;
    InstitutionType institutionType;

    private List<Institution> items = null;
    List<Institution> institutions;
    String selectText = "";

    public Institution findCreditCompanyByName(String name) {
        if (name == null) {
            return null;
        }
        if (name.trim().equals("")) {
            return null;
        }
        String jpql = "select c "
                + " from Institution c "
                + " where c.retired=:ret "
                + " and c.institutionType=:t "
                + " and c.name=:n";
        Map m = new HashMap<>();
        m.put("ret", false);
        m.put("t", InstitutionType.CreditCompany);
        m.put("n", name);
        return getFacade().findFirstByJpql(jpql, m);
    }
    
    
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

    public List<Institution> getCreditCompanies() {
        String sql;
        sql = "select p from Institution p where p.retired=false and p.institutionType=com.divudi.data.InstitutionType.CreditCompany order by p.name";
        return getFacade().findByJpql(sql);
    }

    public List<Institution> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from Institution c where c.retired=false and i.institutionType = com.divudi.data.InstitutionType.CreditCompany  and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new Institution();
        current.setInstitutionType(InstitutionType.CreditCompany);
    }

    public void setSelectedItems(List<Institution> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    public List<Institution> getInstitutions() {
        return institutions;
    }

    public void setInstitutions(List<Institution> institutions) {
        this.institutions = institutions;
    }

    public Item getService() {
        return service;
    }

    public void setService(Item service) {
        this.service = service;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
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
    
    public void save(Institution cc) {
        if (cc == null) {
            return;
        }
        if (cc.getId() != null) {
            getFacade().edit(cc);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            cc.setCreatedAt(new Date());
            cc.setCreater(getSessionController().getLoggedUser());
            getFacade().create(cc);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
    }

    public void fillCreditCompany() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        String sql = "select i from Institution i "
                + "where i.retired=false ";
        institutions = getEjbFacade().findByJpql(sql);

        
    }

    public void fillInstitutions() {

        String sql;
        Map m=new HashMap();
        
        sql= "select i from Institution i "
                + " where i.retired=false ";
        
        if (institutionType != null) {
            sql += " and i.institutionType=:insT";
            m.put("insT", institutionType);
        }
        
        institutions = getEjbFacade().findByJpql(sql,m);

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

    public CreditCompanyController() {
    }

    public Institution getCurrent() {
        if (current == null) {
            current = new Institution();
            current.setInstitutionType(InstitutionType.CreditCompany);
        }
        return current;
    }

    public void setCurrent(Institution current) {
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

    private InstitutionFacade getFacade() {
        return ejbFacade;
    }

    public List<Institution> getItems() {
        if (items == null) {
            String sql = "SELECT i FROM Institution i where i.retired=false and i.institutionType = com.divudi.data.InstitutionType.CreditCompany order by i.name";
            items = getEjbFacade().findByJpql(sql);
        }
        return items;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public InstitutionType getInstitutionType() {
        return institutionType;
    }

    public void setInstitutionType(InstitutionType institutionType) {
        this.institutionType = institutionType;
    }

    /**
     *
     */
}
