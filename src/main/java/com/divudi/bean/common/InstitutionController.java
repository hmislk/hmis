/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;

import com.divudi.data.BillType;
import com.divudi.data.InstitutionType;
import com.divudi.entity.Department;
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
import javax.inject.Inject;
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
    List<Institution> institution;

    public List<Institution> getSelectedItems() {
        if (selectText.trim().equals("")) {
            selectedItems = getFacade().findBySQL("select c from Institution c where c.retired=false order by c.name");
        } else {
            selectedItems = getFacade().findBySQL("select c from Institution c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        }

        return selectedItems;
    }

//    public List<Institution> getBranch(){
//        String sql="Select i From Institution i where i.retired=false and"
//                + " i.institution=:ins";
//    }
    public List<Institution> completeIns(String qry) {
        String sql;
        HashMap hm=new HashMap();
        sql = "select c "
                + " from Institution c "
                + " where c.retired=false "
                + " and (upper(c.name) like :q "
                + " or upper(c.institutionCode) like :q ) "
                + " order by c.name";
        hm.put("q", "%"+ qry.toUpperCase()+"%");
        return getFacade().findBySQL(sql,hm);
    }

    public List<Institution> completeCompany(String qry) {
        String sql;
        HashMap hm = new HashMap();
        hm.put("type", InstitutionType.Company);
        sql = "select c from Institution c"
                + "  where c.retired=false"
                + "  and c.institutionType=:type"
                + " and upper(c.name) like '%" + qry.toUpperCase() + "%' "
                + " order by c.name";

        return getFacade().findBySQL(sql, hm);
    }
    
    public List<Institution> completeAgency(String qry) {
        String sql;
        HashMap hm = new HashMap();
        hm.put("type", InstitutionType.Agency);
        sql = "select c from Institution c"
                + "  where c.retired=false"
                + "  and c.institutionType=:type"
                + " and upper(c.name) like '%" + qry.toUpperCase() + "%' "
                + " order by c.name";

        return getFacade().findBySQL(sql, hm);
    }

    
     public List<Institution> completeBank(String qry) {
        String sql;
        HashMap hm = new HashMap();
        hm.put("type", InstitutionType.Bank);
        sql = "select c from Institution c"
                + "  where c.retired=false"
                + "  and c.institutionType=:type"
                + " and upper(c.name) like '%" + qry.toUpperCase() + "%' "
                + " order by c.name";

        return getFacade().findBySQL(sql, hm);
    }
       public List<Institution> completeBankBranch(String qry) {
        String sql;
        HashMap hm = new HashMap();
        hm.put("type", InstitutionType.branch);
        sql = "select c from Institution c"
                + "  where c.retired=false"
                + "  and c.institutionType=:type"
                + " and upper(c.name) like '%" + qry.toUpperCase() + "%' "
                + " order by c.name";

        return getFacade().findBySQL(sql, hm);
    }
     
    
    
    public List<Institution> CompleteCompanyBydepartment(String qry) {
        String sql;
        HashMap hm = new HashMap();
        hm.put("type", InstitutionType.Company);
        sql = "select c from Institution c where c.retired=false and c.institutionType=:type and upper(c.name) like '%" + qry.toUpperCase() + "%' order by c.name";

        return getFacade().findBySQL(sql, hm);
    }

    public List<Institution> completeCreditCompany(String qry) {
        String sql;
        HashMap hm = new HashMap();
        hm.put("type", InstitutionType.CreditCompany);
        sql = "select c from Institution c where c.retired=false and "
                + "c.institutionType=:type and upper(c.name) "
                + "like '%" + qry.toUpperCase() + "%' order by c.name";

        institution = getFacade().findBySQL(sql, hm);
        return institution;
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
        return getFacade().findBySQL(sql, hm, 10);
    }

//     public List<Institution> completeSupplier(String qry) {
//        String sql;
//        HashMap hm=new HashMap();
//        hm.put("type", InstitutionType.Dealer);
//        sql = "select c from Institution c where c.retired=false and c.institutionType=:type and upper(c.name) like '%" + qry.toUpperCase() + "%' order by c.name";
//        
//        return getFacade().findBySQL(sql,hm);
//    }
    public List<Institution> completeCredit(String query) {
        List<Institution> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select p from Institution p where p.retired=false and p.institutionType=com.divudi.data.InstitutionType.CreditCompany and upper(p.name) like '%" + query.toUpperCase() + "%' order by p.name";
            //System.out.println(sql);
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
            UtilityController.addSuccessMessage("savedOldSuccessfully");
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
            UtilityController.addSuccessMessage("savedNewSuccessfully");
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
            UtilityController.addSuccessMessage("DeleteSuccessfull");
        } else {
            UtilityController.addSuccessMessage("NothingToDelete");
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

//    public List<Institution> getCompanies() {
//        String sql = "select c from Institution c where c.retired=false and c.institutionType=:tp  order by c.name";
//        HashMap hm = new HashMap();
//        hm.put("tp", InstitutionType.Company);
//        items = getFacade().findBySQL(sql, hm);
//        if (items == null) {
//            items = new ArrayList<>();
//        }
//
//        return items;
//    }
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

    public void setInstitutionTypes(InstitutionType[] institutionTypes) {
        this.institutionTypes = institutionTypes;
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
