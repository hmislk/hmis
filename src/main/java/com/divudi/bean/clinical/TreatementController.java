/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.clinical;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class TreatementController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private ClinicalEntityFacade ejbFacade;
    List<ClinicalEntity > selectedItems;
    private ClinicalEntity current;
    private List<ClinicalEntity> items = null;
    List<ClinicalEntity> insItems =null;
    String selectText = "";

//    public List<ClinicalEntity> completeTreatments(String qry) {
//        List<ClinicalEntity> c;
//        Map m = new HashMap();
//        m.put("t", SymanticType.Pharmacologic_Substance);
//        m.put("n", "%" + qry.toUpperCase() + "%");
//        String sql;
//        sql="select c from ClinicalEntity c where c.retired=false and (c.name) like :n and c.symanticType=:t order by c.name";
//        c = getFacade().findByJpql(sql,m,10);
//        if (c == null) {
//            c = new ArrayList<>();
//        }
//        return c;
//    }

    public List<ClinicalEntity> getSelectedItems() {
        Map m = new HashMap();
        m.put("t", SymanticType.Pharmacologic_Substance);
        m.put("n", "%" + getSelectText().toUpperCase() + "%");
        String sql;
        sql="select c from ClinicalEntity c where c.retired=false and (c.name) like :n and c.symanticType=:t order by c.name";
        selectedItems = getFacade().findByJpql(sql,m);
        return selectedItems;
    }

    public void prepareAdd() {
        current = new ClinicalEntity();
        current.setInstitution(sessionController.getInstitution());
        current.setSymanticType(SymanticType.Pharmacologic_Substance);
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
        current.setSymanticType(SymanticType.Pharmacologic_Substance);
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Saved");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Updated");
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

    public TreatementController() {
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
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
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
        if (items == null) {
            Map m = new HashMap();
            m.put("t", SymanticType.Pharmacologic_Substance);
            String sql;
            sql = "select c from ClinicalEntity c where c.retired=false and c.symanticType=:t order by c.name";
            items = getFacade().findByJpql(sql, m);
        }
        return items;
    }

    public List<ClinicalEntity> getInsItems() {
        if (insItems == null) {
            Map m = new HashMap();
            m.put("t", SymanticType.Pharmacologic_Substance);
            m.put("ins", sessionController.getInstitution());
            String sql;
            sql = "select c "
                    + " from ClinicalEntity c "
                    + " where c.retired=false "
                    + " and c.symanticType=:t "
                    + " and c.institution=:ins "
                    + " order by c.name";
            insItems = getFacade().findByJpql(sql, m);
        }
        return insItems;
    }

    public void setInsItems(List<ClinicalEntity> insItems) {
        this.insItems = insItems;
    }
    
    
}
