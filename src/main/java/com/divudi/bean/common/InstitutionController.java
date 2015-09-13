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
    private List<Institution> companies = null;
    private List<Institution> creditCompanies = null;
    private List<Institution> banks = null;
    private List<Institution> suppliers = null;
    private List<Institution> agencies = null;
    List<Institution> institution;
    String selectText = "";
    private Boolean codeDisabled = false;

    public List<Institution> getSelectedItems() {
        if (selectText.trim().equals("")) {
            selectedItems = completeInstitution(null, InstitutionType.values());
        } else {
            selectedItems = completeInstitution(selectText, InstitutionType.values());
        }
        return selectedItems;
    }

    public List<Institution> completeIns(String qry) {
        return completeInstitution(qry, InstitutionType.values());
    }

    public List<Institution> completeInstitution(String qry, InstitutionType[] types) {
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Institution c "
                + " where c.retired=false ";
        if (qry != null) {
            sql += " and upper(c.name) like :qry ";
            hm.put("qry", "%" + qry.toUpperCase() + "%");
        }
        if (types != null) {
            List<InstitutionType> lstTypes = Arrays.asList(types);
            hm.put("types", lstTypes);
            sql += "  and c.institutionType in :types";
        }
        sql += " order by c.name";
        return getFacade().findBySQL(sql, hm);
    }

    public List<Institution> getSuppliers() {
        if (suppliers == null) {
            suppliers = completeInstitution(null, InstitutionType.Dealer);
        }
        return suppliers;
    }

    public void setSuppliers(List<Institution> suppliers) {
        this.suppliers = suppliers;
    }

    public List<Institution> getAgencies() {
        if (agencies == null) {
            agencies = completeInstitution(null, InstitutionType.Agency);
        }
        return agencies;
    }

    public void setAgencies(List<Institution> agencies) {
        this.agencies = agencies;
    }

    public List<Institution> completeInstitution(String qry, InstitutionType type) {
        InstitutionType[] types = new InstitutionType[]{type};
        return completeInstitution(qry, types);
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

    public List<Institution> completeSuppliers(String qry) {
        return completeInstitution(qry, new InstitutionType[]{InstitutionType.Dealer, InstitutionType.StoreDealor});
    }

    public List<Institution> getCreditCompanies() {
        if (creditCompanies == null) {
            creditCompanies = completeInstitution(null, InstitutionType.CreditCompany);
        }
        return creditCompanies;
    }

    public List<Institution> getCompanies() {
        if (companies == null) {
            companies = completeInstitution(null, InstitutionType.Company);
        }
        return companies;
    }

    public List<Institution> getBanks() {
        if (banks == null) {
            banks = completeInstitution(null, InstitutionType.Bank);
        }
        return banks;
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

    public void prepareAdd() {
        codeDisabled = false;
        current = new Institution();
    }

    public void prepareAddAgency() {
        codeDisabled = false;
        current = new Institution();
        current.setInstitutionType(InstitutionType.Agency);
    }
    
    public void setSelectedItems(List<Institution> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
        agencies = null;
        suppliers = null;
        companies = null;
        creditCompanies = null;
        banks = null;
        suppliers = null;
        agencies = null;

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

    public SessionController getSessionController() {
        return sessionController;
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
        if (items == null) {
            items = getFacade().findAll("name", true);
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
