package com.divudi.bean.common;

import com.divudi.data.InstitutionType;
import java.util.TimeZone;
import com.divudi.facade.InstitutionFacade;
import com.divudi.entity.Institution;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class InstitutionController implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Managed Beans
     */
    @Inject
    SessionController sessionController;
    /**
     * EJBs
     */
    @EJB
    private InstitutionFacade ejbFacade;
    /**
     * Properties
     */
    List<Institution> selectedItems;
    private Institution current;
    private List<Institution> items = null;
    private List<Institution> itemsToRemove = null;
    List<Institution> institution;
    String selectText = "";
    private Boolean codeDisabled = false;

    public List<Institution> getSelectedItems() {
        if (selectText.trim().equals("")) {
            selectedItems = completeInstitution(null, null);
        } else {
            selectedItems = completeInstitution(selectText, null);
        }
        return selectedItems;
    }

    public List<Institution> completeIns(String qry) {
        return completeInstitution(qry, null);
    }

    public List<Institution> completeInstitution(String qry, InstitutionType type) {
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Institution c "
                + " where c.retired=false ";
        if (qry != null) {
            sql += " and upper(c.name) like :qry ";
            hm.put("qry", "%" + qry.toUpperCase() + "%");
        }
        if (type != null) {
            hm.put("type", type);
            sql += "  and c.institutionType=:type";
        }
        sql += " order by c.name";
        return getFacade().findBySQL(sql, hm);
    }

    public List<Institution> completeCompany(String qry) {
        return completeInstitution(qry, InstitutionType.Company);
    }

    public List<Institution> completeAgency(String qry) {
        return completeInstitution(qry, InstitutionType.Agency);
    }

    public List<Institution> completeBank(String qry) {
        return completeInstitution(qry, InstitutionType.Bank);
    }

    public List<Institution> completeBankBranch(String qry) {
        return completeInstitution(qry, InstitutionType.branch);
    }

    public List<Institution> completeCreditCompany(String qry) {
        return completeInstitution(qry, InstitutionType.CreditCompany);
    }

    public List<Institution> getCreditCompany() {
        String sql;
        HashMap hm = new HashMap();
        hm.put("type", InstitutionType.CreditCompany);
        sql = "select c from Institution c "
                + " where c.retired=false "
                + " and c.institutionType=:type"
                + "  order by c.name";
        return getFacade().findBySQL(sql, hm);
    }

    public List<Institution> getCompanies() {
        String sql;
        HashMap hm = new HashMap();
        hm.put("type", InstitutionType.Company);
        sql = "select c from Institution c where c.retired=false and"
                + " c.institutionType=:type order by c.name";

        return getFacade().findBySQL(sql, hm);
    }

    public List<Institution> completeSupplier(String qry) {
        String sql;
        HashMap hm = new HashMap();
        InstitutionType[] insType = {InstitutionType.Dealer, InstitutionType.StoreDealor};
        List<InstitutionType> institutionTypesList = Arrays.asList(insType);

        hm.put("ins", institutionTypesList);
        hm.put("q", "%" + qry.toUpperCase() + "%");
        sql = "select c from Institution c "
                + " where c.retired=false "
                + " and c.institutionType in :ins"
                + " and (upper(c.name) like :q "
                + " or upper(c.institutionCode) like :q )"
                + " order by c.name";
        return getFacade().findBySQL(sql, hm, 20);
    }

    public List<Institution> completeCredit(String query) {
        List<Institution> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select p from Institution p where p.retired=false and p.institutionType=com.divudi.data.InstitutionType.CreditCompany and upper(p.name) like '%" + query.toUpperCase() + "%' order by p.name";
            ////System.out.println(sql);
            suggestions = getFacade().findBySQL(sql);
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

    private Boolean checkCodeExist() {
        String sql = "SELECT i FROM Institution i where i.retired=false ";
        List<Institution> ins = getEjbFacade().findBySQL(sql);
        if (ins != null) {
            for (Institution i : ins) {
                if (i.getInstitutionCode() == null || i.getInstitutionCode().trim().equals("")) {
                    continue;
                }
                if (i.getInstitutionCode() != null && i.getInstitutionCode().equals(getCurrent().getInstitutionCode())) {
                    UtilityController.addErrorMessage("Insituion Code Already Exist Try another Code");
                    return true;
                }
            }
        }
        return false;
    }

    public void saveSelected() {
        if (getCurrent().getInstitutionType() == null) {
            UtilityController.addErrorMessage("Select Instituion Type");
            return;
        }

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {

            if (getCurrent().getInstitutionCode() != null) {
                getCurrent().setInstitutionCode(getCurrent().getInstitutionCode());
            }
            getFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            if (getCurrent().getInstitutionCode() != null) {
                if (!checkCodeExist()) {
                    getCurrent().setInstitutionCode(getCurrent().getInstitutionCode());

                } else {
                    return;
                }
            }
            getCurrent().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            UtilityController.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
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

    public InstitutionController() {
    }

    public Institution getCurrent() {
        if (current == null) {
            current = new Institution();
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
            getCurrent().setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getCurrent().setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
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
        items = getFacade().findAll("name", true);
        return items;
    }

    public List<Institution> getInsuranceCompany() {
        String sql = "select c from Institution c where c.retired=false and c.institutionType=:tp  order by c.name";
        HashMap hm = new HashMap();
        hm.put("tp", InstitutionType.CreditCompany);
        items = getFacade().findBySQL(sql, hm);
        if (items == null) {
            items = new ArrayList<>();
        }

        return items;
    }

    public List<Institution> getBanks() {
        String sql = "select c from Institution c where c.retired=false and c.institutionType=:tp  order by c.name";
        HashMap hm = new HashMap();
        hm.put("tp", InstitutionType.Bank);
        items = getFacade().findBySQL(sql, hm);
        if (items == null) {
            items = new ArrayList<>();
        }

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

    public List<Institution> getItemsToRemove() {
        return itemsToRemove;
    }

    public void setItemsToRemove(List<Institution> itemsToRemove) {
        this.itemsToRemove = itemsToRemove;
    }

    public List<Institution> getInstitution() {
        return institution;
    }

    public void setInstitution(List<Institution> institution) {
        this.institution = institution;
    }

    public void removeSelectedItems() {
        for (Institution s : itemsToRemove) {
            s.setRetired(true);
            s.setRetireComments("Bulk Remove");
            s.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(s);
        }
        itemsToRemove = null;
        items = null;
    }

    public Institution getInstitutionByName(String name, InstitutionType type) {
        String sql;
        Map m = new HashMap();
        m.put("n", name.toUpperCase());
        m.put("t", type);
        sql = "select i from Institution i where upper(i.name) =:n and i.institutionType=:t";
        Institution i = getFacade().findFirstBySQL(sql, m);
        if (i == null) {
            i = new Institution();
            i.setName(name);
            i.setInstitutionType(type);
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            getFacade().create(i);
        } else {
            i.setRetired(false);
            getFacade().edit(i);
        }
        return i;
    }

    @FacesConverter("institutionConverter")
    public static class InstitutionConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            InstitutionController controller = (InstitutionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "institutionController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
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
            if (object instanceof Institution) {
                Institution o = (Institution) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + InstitutionController.class.getName());
            }
        }
    }

    /**
     *
     */
    @FacesConverter(forClass = Institution.class)
    public static class InstitutionControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            InstitutionController controller = (InstitutionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "institutionController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
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
            if (object instanceof Institution) {
                Institution o = (Institution) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + InstitutionController.class.getName());
            }
        }
    }
}
