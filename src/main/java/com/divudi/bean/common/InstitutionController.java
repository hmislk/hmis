package com.divudi.bean.common;

import com.divudi.core.data.HistoryType;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.entity.AgentHistory;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.AgentHistoryFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
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
    @EJB
    AgentHistoryFacade agentHistoryFacade;
    /**
     * Properties
     */
    List<Institution> selectedItems;
    List<Institution> selectedAgencies;
    private Institution current;
    Institution agency;
    private List<Institution> items = null;
    private List<Institution> itemsToRemove = null;
    private List<Institution> companies = null;
    private List<Institution> creditCompanies = null;
    private List<Institution> banks = null;
    private List<Institution> suppliers = null;
    private List<Institution> agencies = null;
    private List<Institution> collectingCentre = null;
    private List<Institution> collectingCentresAndManagedInstitutions = null;
    private List<Institution> institution;
    private List<Institution> searchItems = null;
    private String selectText = "";
    private Boolean codeDisabled = false;
    private int managaeInstitutionIndex = -1;
    private List<Institution> sites;
    private List<InstitutionDuplicateGroup> duplicateGroups;

    public void fillAllSites() {
        sites = new ArrayList<>();
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Institution c "
                + " where c.retired=false "
                + " and c.institutionType =:type";

        sql += " order by c.name";
        hm.put("type", InstitutionType.Site);
        sites = getFacade().findByJpql(sql, hm);
    }

    private List<Institution> institutions;
    private InstitutionType institutionType;

    public void fillRetiredInstitution() {
        institutions = new ArrayList<>();
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Institution c "
                + " where c.retired=true ";

        if(institutionType != null){
            sql += " and c.institutionType =:type ";
            hm.put("type", institutionType);
        }

        sql += " order by c.name";
        institutions = getFacade().findByJpql(sql, hm);
    }

    public void reactivateRetiredInstitution(Institution institution) {
        Institution currentRetiredInstitution = getFacade().find(institution.getId());

        if(currentRetiredInstitution == null){
            JsfUtil.addErrorMessage("No Found Institution");
            return;
        }
        if(!currentRetiredInstitution.isRetired()){
            JsfUtil.addErrorMessage("Already Active");
            return;
        }

        currentRetiredInstitution.setRetired(false);
        getFacade().edit(currentRetiredInstitution);

        JsfUtil.addSuccessMessage("Successfully Reactiveed");
    }

    public String toAdminManageInstitutions() {
        return "/admin/institutions/admin_institutions_index?faces-redirect=true";
    }

    public String toListInstitutions() {
        fillItems();
        return "/admin/institutions/institutions?faces-redirect=true";
    }

    public String navigatetoActivateInstitutions() {
        return "/admin/institutions/activate_institutions?faces-redirect=true";
    }

    public String toAddNewInstitution() {
        current = new Institution();
        return "/admin/institutions/institution?faces-redirect=true";
    }

    public String toEditInstitution() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        return "/admin/institutions/institution?faces-redirect=true";
    }

    public void makeNull() {
        current = null;

    }

    public String deleteInstitution() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        current.setRetired(true);
        getFacade().edit(current);
        return toListInstitutions();
    }

    public String saveSelectedInstitution() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        if (isDuplicateName(current)) {
            JsfUtil.addErrorMessage("Another institution with same name exists");
            return "";
        }
        if (current.getId() == null) {
            getFacade().create(current);
        } else {
            getFacade().edit(current);
        }
        return toListInstitutions();
    }

    public List<Institution> getSelectedItems() {
        if (selectText.trim().equals("")) {
            selectedItems = completeInstitution(null, InstitutionType.values());
        } else {
            selectedItems = completeInstitution(selectText, InstitutionType.values());
        }
        return selectedItems;
    }

    public void fetchSelectedAgencys() {
        InstitutionType[] types = {InstitutionType.Agency};
        if (selectText.trim().equals("")) {
            selectedAgencies = completeInstitution(null, types);
        } else {
            selectedAgencies = completeInstitution(selectText, types);
        }
    }

    public void fetchSelectedCollectingCentre() {
        InstitutionType[] types = {InstitutionType.CollectingCentre};
        if (selectText.trim().equals("")) {
            collectingCentre = completeInstitution(null, types);
        } else {
            collectingCentre = completeInstitution(selectText, types);
        }
    }

    public List<Institution> completeIns(String qry) {
        return completeInstitution(qry, InstitutionType.values());
    }

    public List<Institution> getSearchItems() {
        return searchItems;
    }

    public void fillSearchItems() {
        if (selectText == null || selectText.trim().equals("")) {
            String jpql = "select i "
                    + "from Institution i "
                    + "where i.retired=:ret "
                    + "order by i.name";

            Map m = new HashMap();
            m.put("ret", false);
            searchItems = getFacade().findByJpql(jpql, m);

            if (searchItems != null && !searchItems.isEmpty()) {
                current = searchItems.get(0);
            } else {
                current = null;
            }
        } else {
            String sql = "Select i from Institution i where i.retired=false and (i.name) like :in order by i.name";
            Map m = new HashMap();
            m.put("in", "%" + selectText.toUpperCase() + "%");
            searchItems = getFacade().findByJpql(sql, m);
            if (searchItems != null && !searchItems.isEmpty()) {
                current = searchItems.get(0);
            } else {
                current = null;
            }
        }
    }

    public List<Institution> fillAllItems() {
        List<Institution> ins;
        String sql = "Select i from Institution i where i.retired=:ret order by i.name";
        Map m = new HashMap();
        m.put("ret", false);
        ins = getFacade().findByJpql(sql, m);
        return ins;
    }

    public List<Institution> completeInstitution(String qry, InstitutionType[] types) {
        return completeInstitution(qry, types, null);
    }

    public List<Institution> completeInstitution(String qry, InstitutionType type, Institution parent) {
        return completeInstitution(qry, new InstitutionType[]{type}, parent);
    }

    public List<Institution> completeInstitution(String qry, InstitutionType[] types, Institution parent) {
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Institution c "
                + " where c.retired=false ";
        if (qry != null) {
            sql += " and ((c.name) like :qry or (c.code) like :qry) ";
            hm.put("qry", "%" + qry.toUpperCase() + "%");
        }
        if (types != null) {
            List<InstitutionType> lstTypes = Arrays.asList(types);
            hm.put("types", lstTypes);
            sql += "  and c.institutionType in :types";
        }
        if (parent != null) {
            hm.put("parent", parent);
            sql += "  and c.parentInstitution=:parent";
        }
        sql += " order by c.name";
        return getFacade().findByJpql(sql, hm);
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

    public List<Institution> getCollectingCentre() {
        if (collectingCentre == null) {
            collectingCentre = completeInstitution(null, InstitutionType.CollectingCentre);
        }

        return collectingCentre;
    }

    public void setCollectingCentre(List<Institution> collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    public List<Institution> getAgencies() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (selectText.trim().equals("")) {
            agencies = completeInstitution(null, InstitutionType.Agency);
        } else {
            agencies = completeInstitution(selectText, InstitutionType.Agency);
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

    public List<Institution> completeSite(String qry) {
        //Sites
        return completeInstitution(qry, InstitutionType.Site);
    }

    public List<Institution> completeCollectingCenter(String qry) {
        return completeInstitution(qry, InstitutionType.CollectingCentre);
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

    public List<Institution> getCollectingCenter() {
        if (collectingCentre == null) {
            collectingCentre = completeInstitution(null, InstitutionType.CollectingCentre);
        }
        return collectingCentre;
    }

    public List<Institution> getBanks() {
        if (banks == null) {
            banks = completeInstitution(null, InstitutionType.Bank);
        }
        return banks;
    }

    public List<Institution> getBranches(Institution bank) {
        return completeInstitution(null, InstitutionType.branch, bank);
    }

    public Institution getInstitutionByName(String name, InstitutionType type) {
        if (name == null || type == null) {
            return null;
        }

        String cleanedName = name.trim();
        if (cleanedName.isEmpty()) {
            return null;
        }

        String sql = "select i from Institution i where upper(i.name)=:n and i.institutionType=:t";
        Map<String, Object> m = new HashMap<>();
        m.put("n", cleanedName.toUpperCase());
        m.put("t", type);
        Institution i = getFacade().findFirstByJpql(sql, m);

        if (i == null) {
            i = new Institution();
            i.setName(cleanedName);
            i.setInstitutionType(type);
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            getFacade().create(i);
        } else if (i.isRetired()) {
            i.setRetired(false);
            getFacade().edit(i);
        }

        return i;
    }

    public Institution findAndSaveInstitutionByName(String name) {
        if (name == null) {
            return null;
        }

        String cleanedName = name.trim();
        if (cleanedName.isEmpty()) {
            return null;
        }

        String sql = "select i from Institution i where upper(i.name)=:name and i.retired=:ret";
        Map<String, Object> m = new HashMap<>();
        m.put("name", cleanedName.toUpperCase());
        m.put("ret", false);
        Institution i = getFacade().findFirstByJpql(sql, m);

        if (i == null) {
            i = new Institution();
            i.setName(cleanedName);
            getFacade().create(i);
        } else if (i.isRetired()) {
            i.setRetired(false);
            getFacade().edit(i);
        }

        return i;
    }

    public Institution findAndSaveInstitutionByCode(String code) {
        if (code == null || code.trim().equals("")) {
            return null;
        }
        String sql;
        Map m = new HashMap();
        m.put("code", code);
        m.put("ret", false);
        sql = "select i "
                + " from Institution i "
                + " where i.code=:code"
                + " and i.retired=:ret";
        Institution i = getFacade().findFirstByJpql(sql, m);
        return i;
    }

    public Institution findExistingInstitutionByName(String name) {
        if (name == null || name.trim().equals("")) {
            return null;
        }
        String sql;
        Map m = new HashMap();
        m.put("name", name);
        m.put("ret", false);
        sql = "select i "
                + " from Institution i "
                + " where i.name=:name"
                + " and i.retired=:ret";
        Institution i = getFacade().findFirstByJpql(sql, m);
        return i;
    }

    private Boolean checkCodeExist() {
        String sql = "SELECT i FROM Institution i where i.retired=false and i.institutionCode is not null ";
        List<Institution> ins = getEjbFacade().findByJpql(sql);
        if (ins != null) {
            for (Institution i : ins) {
                if (i.getCode() == null || i.getCode().trim().equals("")) {
                    continue;
                }
                if (i.getCode() != null && i.getCode().equals(getCurrent().getCode())) {
                    JsfUtil.addErrorMessage("Insituion Code Already Exist Try another Code");
                    return true;
                }
            }
        }
        return false;
    }

    private Boolean checkCodeExistAgency() {
        String sql = "SELECT i FROM Institution i where i.retired=false and i.institutionCode is not null ";
        List<Institution> ins = getEjbFacade().findByJpql(sql);
        if (ins != null) {
            for (Institution i : ins) {
                if (i.getCode() == null || i.getCode().trim().equals("")) {
                    continue;
                }
                if (i.getCode() != null && i.getCode().equals(getAgency().getCode())) {
                    JsfUtil.addErrorMessage("Insituion Code Already Exist Try another Code");
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

    public void prepareAddSite() {
        codeDisabled = false;
        current = new Institution();
        current.setInstitutionType(InstitutionType.Site);
    }

    public void prepareAddAgency() {
        codeDisabled = false;
        agency = new Institution();
        agency.setInstitutionType(InstitutionType.Agency);
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
        collectingCentresAndManagedInstitutions = null;
        creditCompanies = null;
        banks = null;
        suppliers = null;
        agencies = null;

    }

    public void save(Institution ins) {
        if (!ins.isRetired() && isDuplicateName(ins)) {
            JsfUtil.addErrorMessage("Another institution with same name exists");
            return;
        }
        if (ins.getId() == null) {
            getFacade().create(ins);
        } else {
            getFacade().edit(ins);
        }
    }

    public void saveSelectedSite() {
        if (getCurrent().getInstitutionType() != InstitutionType.Site) {
            JsfUtil.addErrorMessage("Invalid Institution Type");
            return;
        }

        if (isDuplicateName(getCurrent())) {
            JsfUtil.addErrorMessage("Another institution with same name exists");
            return;
        }

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
//
//            if (getCurrent().getCode() != null) {
//                getCurrent().setInstitutionCode(getCurrent().getCode());
//            }
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
//            if (getCurrent().getCode() != null) {
//                if (!checkCodeExist()) {
//                    getCurrent().setInstitutionCode(getCurrent().getCode());
//
//                } else {
//                    return;
//                }
//            }
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        fillAllSites();
    }

    public void saveSelected() {
        if (getCurrent().getInstitutionType() == null) {
            JsfUtil.addErrorMessage("Select Institution Type");
            return;
        }

        if (isDuplicateName(getCurrent())) {
            JsfUtil.addErrorMessage("Another institution with same name exists");
            return;
        }

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
//
//            if (getCurrent().getCode() != null) {
//                getCurrent().setInstitutionCode(getCurrent().getCode());
//            }
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
//            if (getCurrent().getCode() != null) {
//                if (!checkCodeExist()) {
//                    getCurrent().setInstitutionCode(getCurrent().getCode());
//
//                } else {
//                    return;
//                }
//            }
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        fillItems();
    }

    public void saveSelectedAgency() {
        if (getAgency().getInstitutionType() == null) {
            JsfUtil.addErrorMessage("Select Institution Type");
            return;
        }

        if (isDuplicateName(getAgency())) {
            JsfUtil.addErrorMessage("Another institution with same name exists");
            return;
        }

        if (getAgency().getId() != null && getAgency().getId() > 0) {

//            if (getAgency().getCode() != null) {
//                getAgency().setInstitutionCode(getAgency().getCode());
//            }
            getFacade().edit(getAgency());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
//            if (getAgency().getCode() != null) {
//                if (!checkCodeExistAgency()) {
//                    getAgency().setInstitutionCode(getAgency().getCode());
//
//                } else {
//                    return;
//                }
//            }
            getAgency().setCreatedAt(new Date());
            getAgency().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getAgency());
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        fetchSelectedAgencys();
    }

    public void updateAgentCreditLimit() {
        updateCreditLimit(HistoryType.AgentBalanceUpdateBill);
    }

    public void updateCollectingCentreCreditLimit() {
        updateCreditLimit(HistoryType.CollectingCentreBalanceUpdateBill);
    }

    public void updateCreditLimit(HistoryType historyType) {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please Select an Agency");
            return;
        }

        if (current.getMaxCreditLimit() == 0.0) {
            JsfUtil.addErrorMessage("Please Enter Maximum Credit Limit.");
            return;
        }

        Institution i = getFacade().find(current.getId());
        double mcl = i.getMaxCreditLimit();
        //// // System.out.println("mcl = " + mcl);
        double acl = i.getAllowedCredit();
        double scl = i.getStandardCreditLimit();

        if (current.getStandardCreditLimit() > current.getAllowedCredit()) {
            JsfUtil.addErrorMessage("Allowed Credit Limit must Grater Than or Equal To Standard Credit Limit");
            return;
        }

        if (current.getMaxCreditLimit() < current.getAllowedCredit()) {
            JsfUtil.addErrorMessage("Allowed Credit Limit must Less Than Maximum Credit Limit");
            return;
        }

        if ((current.getStandardCreditLimit() == scl) && (current.getAllowedCredit() == acl) && (current.getMaxCreditLimit() == mcl)) {
            JsfUtil.addErrorMessage("Nothing To Update");
            return;
        }

        if (current.getStandardCreditLimit() != scl) {
            createAgentCreditLimitUpdateHistory(current, scl, current.getStandardCreditLimit(), historyType, "Standard Credit Limit");
            JsfUtil.addSuccessMessage("Standard Credit Limit Updated");
        }

        if (current.getAllowedCredit() != acl) {
            createAgentCreditLimitUpdateHistory(current, acl, current.getAllowedCredit(), historyType, "Allowed Credit Limit");
            JsfUtil.addSuccessMessage("Allowed Credit Limit Updated");
        }

        if (current.getMaxCreditLimit() != mcl) {
            createAgentCreditLimitUpdateHistory(current, mcl, current.getMaxCreditLimit(), historyType, "Max Credit Limit");
            JsfUtil.addSuccessMessage("Max Credit Limit Updated");
        }
        getFacade().edit(current);

        switch (historyType) {
            case AgentBalanceUpdateBill:
                getAgencies();
                break;
            case CollectingCentreBalanceUpdateBill:
                getCollectingCentre();
                break;
        }
    }

    public void createAgentCreditLimitUpdateHistory(Institution ins, double historyValue, double transactionValue, HistoryType historyType, String comment) {
        AgentHistory agentHistory = new AgentHistory();
        agentHistory.setCreatedAt(new Date());
        agentHistory.setCreater(getSessionController().getLoggedUser());
        agentHistory.setBalanceBeforeTransaction(historyValue);
        agentHistory.setTransactionValue(transactionValue);
        agentHistory.setHistoryType(historyType);
        agentHistory.setComment(comment);
        agentHistory.setAgency(ins);
        agentHistory.setInstitution(ins);
        agentHistoryFacade.create(agentHistory);
        JsfUtil.addSuccessMessage("History Saved");
    }

    public Institution findInstitution(Long id) {
        if (id == null) {
            return null;
        }
        return getFacade().find(id);
    }

    // Overloaded method to handle Integer input
    public Institution findInstitution(Integer id) {
        if (id == null) {
            return null;
        }
        return findInstitution(Long.valueOf(id));
    }

    // Overloaded method to handle String input
    public Institution findInstitution(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        try {
            Long longId = Long.parseLong(id);
            return findInstitution(longId);
        } catch (NumberFormatException e) {
// Handle the case where the string is not a valid long number
                        return null;
        }
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
            getCurrent().setRetiredAt(new Date());
            getCurrent().setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        fetchSelectedAgencys();
        current = null;
        getCurrent();
    }

    public void deleteSite() {

        if (getCurrent() != null) {
            getCurrent().setRetired(true);
            getCurrent().setRetiredAt(new Date());
            getCurrent().setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }

        current = null;
        getCurrent();
        fillAllSites();
        getSites();
    }

    public void deleteAgency() {

        if (getAgency() != null) {
            getAgency().setRetired(true);
            getAgency().setRetiredAt(new Date());
            getAgency().setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(getAgency());
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        fetchSelectedAgencys();
        prepareAddAgency();
    }

    private InstitutionFacade getFacade() {
        return ejbFacade;
    }

    private boolean isDuplicateName(Institution ins) {
        if (ins == null || ins.getName() == null) {
            return false;
        }
        String name = ins.getName().trim();
        if (name.isEmpty()) {
            return false;
        }
        String jpql = "select i from Institution i where i.retired=false and upper(trim(i.name))=:n";
        Map<String, Object> m = new HashMap<>();
        m.put("n", name.toUpperCase());
        Institution other = getFacade().findFirstByJpql(jpql, m);
        return other != null && (ins.getId() == null || !other.getId().equals(ins.getId()));
    }

    public List<Institution> getItems() {
        if (items == null) {
            fillItems();
        }
        return items;
    }

    public void fillItems() {
        String j;
        j = "select i "
                + " from Institution i "
                + " where i.retired=:ret"
                + " order by i.name";
        Map m = new HashMap();
        m.put("ret", false);
        items = getFacade().findByJpql(j, m);
    }

    public void formatAgentSerial() {
        InstitutionType[] types = {InstitutionType.Agency};
        selectedAgencies = completeInstitution(null, types);
        for (Institution a : selectedAgencies) {
//            //// // System.out.println("a.getCode() = " + a.getCode());
            DecimalFormat df = new DecimalFormat("000");
            double d = Double.parseDouble(a.getCode());
//            //// // System.out.println("d = " + d);
            a.setInstitutionCode(df.format(d));
//            //// // System.out.println("a.getCode() = " + a.getCode());
            getFacade().edit(a);
        }
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

    public List<Institution> getSelectedAgencies() {
        if (selectedAgencies == null) {
            fetchSelectedAgencys();
        }
        return selectedAgencies;
    }

    public void setSelectedAgencies(List<Institution> selectedAgencies) {
        this.selectedAgencies = selectedAgencies;
    }

    public Institution getAgency() {
        if (agency == null) {
            agency = new Institution();
            agency.setInstitutionType(InstitutionType.Agency);
        }
        return agency;
    }

    public void setAgency(Institution agency) {
        this.agency = agency;
    }

    public int getManagaeInstitutionIndex() {
        return managaeInstitutionIndex;
    }

    public void setManagaeInstitutionIndex(int managaeInstitutionIndex) {
        this.managaeInstitutionIndex = managaeInstitutionIndex;
    }

    // Newly created by Dr M H B Ariyaratne with assistance from ChatGPT from OpenAI.
    public List<Institution> getCollectingCentresAndManagedInstitutions() {
        if (collectingCentresAndManagedInstitutions == null) {
            collectingCentresAndManagedInstitutions = new ArrayList<>();
            collectingCentresAndManagedInstitutions.addAll(getCompanies());
            collectingCentresAndManagedInstitutions.addAll(getCollectingCenter());
        }
        return collectingCentresAndManagedInstitutions;
    }

    public void setCollectingCentresAndManagedInstitutions(List<Institution> collectingCentresAndManagedInstitutions) {
        this.collectingCentresAndManagedInstitutions = collectingCentresAndManagedInstitutions;
    }

    public List<Institution> getSites() {
        if (sites == null) {
            fillAllSites();
        }

        return sites;
    }

    public void setSites(List<Institution> sites) {
        this.sites = sites;
    }

    public InstitutionType getInstitutionType() {
        return institutionType;
    }

    public void setInstitutionType(InstitutionType institutionType) {
        this.institutionType = institutionType;
    }

    public List<Institution> getInstitutions() {
        return institutions;
    }

    public void setInstitutions(List<Institution> institutions) {
        this.institutions = institutions;
    }

    public String navigateToDuplicateInstitutions() {
        detectDuplicateInstitutions();
        return "/admin/institutions/institution_duplicates?faces-redirect=true";
    }

    public List<InstitutionDuplicateGroup> getDuplicateGroups() {
        return duplicateGroups;
    }

    public void detectDuplicateInstitutions() {
        String jpql = "SELECT i FROM Institution i WHERE i.retired=false ORDER BY upper(trim(i.name)), i.id";
        List<Institution> all = getFacade().findByJpql(jpql);
        Map<String, List<Institution>> grouped = all.stream()
                .filter(ins -> ins.getName() != null)
                .collect(Collectors.groupingBy(ins -> ins.getName().trim().toUpperCase()));
        duplicateGroups = grouped.values().stream()
                .filter(l -> l.size() > 1)
                .map(l -> new InstitutionDuplicateGroup(l))
                .collect(Collectors.toList());
    }

    public void retireDuplicateGroup(InstitutionDuplicateGroup g) {
        if (g == null || g.getInstitutions() == null || g.getInstitutions().size() < 2) {
            return;
        }
        g.getInstitutions().sort((a, b) -> a.getId().compareTo(b.getId()));
        for (int i = 1; i < g.getInstitutions().size(); i++) {
            Institution ins = g.getInstitutions().get(i);
            ins.setRetired(true);
            ins.setRetiredAt(new Date());
            ins.setRetirer(sessionController.getLoggedUser());
            save(ins);
        }
        detectDuplicateInstitutions();
        JsfUtil.addSuccessMessage("Duplicates retired for " + g.getName());
    }

    public static class InstitutionDuplicateGroup {
        private List<Institution> institutions;

        public InstitutionDuplicateGroup(List<Institution> institutions) {
            this.institutions = institutions;
        }

        public List<Institution> getInstitutions() {
            return institutions;
        }

        public String getName() {
            return institutions.get(0).getName();
        }
    }

    /**
     *
     */
    @FacesConverter(forClass = Institution.class)
    public static class InstitutionControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
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
