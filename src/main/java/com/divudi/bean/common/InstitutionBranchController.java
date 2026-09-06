/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.dto.BankBranchDto;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.util.JsfUtil;
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
public class InstitutionBranchController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    SessionController sessionController;
    @EJB
    private InstitutionFacade ejbFacade;

    private Institution current;
    private Long selectedBranchId;
    private List<Institution> items = null;
    String selectText = "";
    private Boolean codeDisabled = false;

    /**
     * Returns a DTO list of all active bank branches for safe display in the
     * listbox (avoids lazy-loading issues with entity collections).
     */
    public List<BankBranchDto> getBranchDtos() {
        String jpql = "select c.id, c.name, c.institutionCode, i.name "
                + "from Institution c left join c.institution i "
                + "where c.institutionType = :tp and c.retired = false "
                + "order by c.name";
        Map<String, Object> params = new HashMap<>();
        params.put("tp", InstitutionType.branch);
        List<Object[]> rows = getFacade().findObjectArrayByJpql(jpql, params, null);
        List<BankBranchDto> dtos = new ArrayList<>();
        for (Object[] row : rows) {
            Long id = (Long) row[0];
            String name = (String) row[1];
            String code = (String) row[2];
            String bankName = (String) row[3];
            dtos.add(new BankBranchDto(id, name, code, bankName));
        }
        return dtos;
    }

    /**
     * Called by AJAX when user selects a branch in the listbox. Loads the full
     * entity into {@code current} for editing in the detail panel.
     */
    public void onBranchSelect() {
        if (selectedBranchId != null) {
            Institution found = getFacade().find(selectedBranchId);
            if (found != null && found.getInstitutionType() == InstitutionType.branch) {
                current = found;
            }
        } else {
            current = null;
        }
    }

    public List<Institution> completeCredit(String query) {
        List<Institution> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select p from Institution p where p.retired=false and p.institutionType=com.divudi.core.data.InstitutionType.CreditCompany and (p.name) like '%" + query.toUpperCase() + "%' order by p.name";
            suggestions = getFacade().findByJpql(sql);
        }
        return suggestions;
    }

    public void prepareAdd() {
        codeDisabled = false;
        current = new Institution();
        current.setInstitutionType(InstitutionType.branch);
        selectedBranchId = null;
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        getCurrent().setInstitutionType(InstitutionType.branch);
        if (getCurrent().getInstitution() == null) {
            JsfUtil.addErrorMessage("Select the Bank");
            return;
        }
        if (getCurrent().getName() == null || getCurrent().getName().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Enter Branch Name");
            return;
        }
        if (getCurrent().getId() == null) {
            getFacade().create(getCurrent());
        } else {
            getFacade().edit(getCurrent());
        }
        JsfUtil.addSuccessMessage("Saved Successfully");
        recreateModel();
        current = null;
        selectedBranchId = null;
    }

    public String getSelectText() {
        return selectText;
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

    public Long getSelectedBranchId() {
        return selectedBranchId;
    }

    public void setSelectedBranchId(Long selectedBranchId) {
        this.selectedBranchId = selectedBranchId;
    }

    public void delete() {
        if (current != null && current.getId() != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addErrorMessage("Nothing to Delete");
        }
        recreateModel();
        current = null;
        selectedBranchId = null;
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

}
