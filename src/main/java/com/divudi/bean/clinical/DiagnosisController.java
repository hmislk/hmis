/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.clinical;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.SymanticType;
import com.divudi.entity.clinical.ClinicalEntity;
import com.divudi.facade.ClinicalEntityFacade;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class DiagnosisController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private ClinicalEntityFacade ejbFacade;
    List<ClinicalEntity> selectedItems;
    private ClinicalEntity current;
    private List<ClinicalEntity> items = null;
    
    String selectText = "";

    public List<ClinicalEntity> completeDiagnosis(String qry) {
        List<ClinicalEntity> c;
        Map m = new HashMap();
        m.put("t", SymanticType.Disease_or_Syndrome);
        m.put("n", "%" + qry.toUpperCase() + "%");
        String sql;
        sql = "select c from ClinicalEntity c where c.retired=false and upper(c.name) like :n and c.symanticType=:t order by c.name";
        c = getFacade().findBySQL(sql, m, 10);
        if (c == null) {
            c = new ArrayList<>();
        }
        return c;
    }

    public ClinicalEntity findAndSaveDiagnosis(String dxName) {
        ClinicalEntity c;
        Map m = new HashMap();
        m.put("t", SymanticType.Disease_or_Syndrome);
        m.put("n", dxName);
        m.put("ret", false);
        String sql;
        sql = "select c "
                + " from ClinicalEntity c "
                + " where c.retired=:ret "
                + " and c.name=:n "
                + " and c.symanticType=:t ";
        c = getFacade().findFirstByJpql(sql, m);
        if (c == null) {
            c = new ClinicalEntity();
            c.setSymanticType(SymanticType.Disease_or_Syndrome);
            c.setName(dxName);
            c.setCode(CommonController.nameToCode("Disease_or_Syndrome_" + dxName));
            getFacade().create(c);
        }
        return c;
    }

    public List<ClinicalEntity> getSelectedItems() {
        Map m = new HashMap();
        m.put("t", SymanticType.Disease_or_Syndrome);
        m.put("n", "%" + getSelectText().toUpperCase() + "%");
        String sql;
        sql = "select c from ClinicalEntity c where c.retired=false and upper(c.name) like :n and c.symanticType=:t order by c.name";
        selectedItems = getFacade().findByJpql(sql, m);
        return selectedItems;
    }

    public void prepareAdd() {
        current = new ClinicalEntity();
        current.setSymanticType(SymanticType.Disease_or_Syndrome);
        //TODO:
    }

    public void setSelectedItems(List<ClinicalEntity> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        current.setSymanticType(SymanticType.Disease_or_Syndrome);
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Saved");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("Updates");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public ClinicalEntityFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ClinicalEntityFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public DiagnosisController() {
    }

    public ClinicalEntity getCurrent() {
        if (current == null) {
            current = new ClinicalEntity();
        }
        return current;
    }

    public void setCurrent(ClinicalEntity current) {
        this.current = current;
    }

    public void delete() {
        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
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

    private ClinicalEntityFacade getFacade() {
        return ejbFacade;
    }

    public List<ClinicalEntity> getItems() {
        return items;
    }

    public String navigateToListDiagnoses() {
        System.out.println("navigateToListDiagnoses");
        Map m = new HashMap();
        m.put("t", SymanticType.Disease_or_Syndrome);
        m.put("ret", false);
        String jpql;
        jpql = "select c "
                + " from ClinicalEntity c "
                + " where c.retired=:ret "
                + " and c.symanticType=:t "
                + " order by c.name";
        System.out.println("jpql = " + jpql);
        System.out.println("m = " + m);
        items = getFacade().findByJpql(jpql, m);
        System.out.println("items = " + items.size());
        return "/emr/reports/diagnoses";
    }

    
}
