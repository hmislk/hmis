/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.CollectingCentrePaymentMethod;
import com.divudi.data.InstitutionType;
import com.divudi.entity.Institution;
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
 * @author www.divudi.com
 */
@Named
@SessionScoped
public class CollectingCentreController implements Serializable {

    /**
     * Creates a new instance of CollectingCentreController
     */
    public CollectingCentreController() {
        //////// // System.out.println("");
    }
    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    InstitutionController institutionController;
    @EJB
    private InstitutionFacade ejbFacade;
    List<Institution> selectedItems;
    private Institution current;
    private List<Institution> items = null;
    String selectText = "";

    public List<Institution> completeCollecting(String query) {
        List<Institution> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<Institution>();
        } else {
            sql = "select p from Institution p where p.retired=false and p.institutionType=com.divudi.data.InstitutionType.CollectingCentre and (p.name) like '%" + query.toUpperCase() + "%' order by p.name";
            //////// // System.out.println(sql);
            suggestions = getFacade().findByJpql(sql);
        }

        return suggestions;
    }

    public List<Institution> getSelectedItems() {
        if (selectText.trim().equals("")) {
            selectedItems = institutionController.completeInstitution(null, InstitutionType.CollectingCentre);
        } else {
            selectedItems = institutionController.completeInstitution(selectText, InstitutionType.CollectingCentre);
        }

//        selectedItems = getFacade().findByJpql("select c from Institution c where c.retired=false "
//                + "and i.institutionType = com.divudi.data.InstitutionType.CollectingCentre  "
//                + "and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new Institution();
        current.setInstitutionType(InstitutionType.CollectingCentre);
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

    public void generateAndAssignCode() {
        String jpql = "select c "
                + " from Institution c "
                + " where c.retired=:ret"
                + " and c.institutionType=:t "
                + " and c.code is not null "
                + " order by c.id desc";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("t", InstitutionType.CollectingCentre);
        Institution cc = getFacade().findFirstByJpql(jpql, m);
        if (cc == null) {
            return;
        }
        String previousCode = cc.getCode();
        getCurrent().setCode(newCode(previousCode));
    }

    public String newCode(String previousCode) {
        if (previousCode == null || previousCode.isEmpty()) {
            return "CC1"; // Default code if there's no previous code, adjust as needed
        }

        // Splitting the previous code into the string part and numeric part
        int i = previousCode.length() - 1;
        while (i >= 0 && Character.isDigit(previousCode.charAt(i))) {
            i--;
        }

        String prefix = previousCode.substring(0, i + 1);
        String numberPart = previousCode.substring(i + 1);

        // Parsing the numeric part to an integer and incrementing it
        int number;
        try {
            number = Integer.parseInt(numberPart);
        } catch (NumberFormatException e) {
            return prefix + "1"; // Default to 1 if parsing fails
        }
        number++;

        // Combining the incremented number with the prefix
        return prefix + number;
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

    public Institution getCurrent() {
        if (current == null) {
            current = new Institution();
            current.setInstitutionType(InstitutionType.CollectingCentre);
        }
        return current;
    }

    public Institution findCollectingCentreByName(String name) {
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
        m.put("t", InstitutionType.CollectingCentre);
        m.put("n", name);
        return getFacade().findFirstByJpql(jpql, m);
    }

    public Institution findCollectingCentreByCode(String code) {
        if (code == null) {
            return null;
        }
        if (code.trim().equals("")) {
            return null;
        }
        String jpql = "select c "
                + " from Institution c "
                + " where c.retired=:ret "
                + " and c.institutionType=:t "
                + " and c.code=:code";
        Map m = new HashMap<>();
        m.put("ret", false);
        m.put("t", InstitutionType.CollectingCentre);
        m.put("code", code);
        return getFacade().findFirstByJpql(jpql, m);
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
            String sql = "SELECT i FROM Institution i where i.retired=false and i.institutionType = com.divudi.data.InstitutionType.CollectingCentre order by i.name";
            items = getEjbFacade().findByJpql(sql);
        }
        return items;
    }

    public InstitutionController getInstitutionController() {
        return institutionController;
    }

    public void setInstitutionController(InstitutionController institutionController) {
        this.institutionController = institutionController;
    }

    public CollectingCentrePaymentMethod[] getCollectingCentrePaymentMethod() {
        return CollectingCentrePaymentMethod.values();
    }

}
