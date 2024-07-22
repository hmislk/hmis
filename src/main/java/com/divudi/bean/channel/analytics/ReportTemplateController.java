/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.channel.analytics;

import com.divudi.bean.common.*;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.ReportTemplateRow;
import com.divudi.data.ReportTemplateRowBundle;
import com.divudi.data.analytics.ReportTemplateColumn;
import com.divudi.data.analytics.ReportTemplateFilter;
import com.divudi.data.analytics.ReportTemplateType;
import com.divudi.entity.BillFee;
import com.divudi.entity.Department;
import com.divudi.entity.ReportTemplate;
import com.divudi.entity.Institution;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.facade.ReportTemplateFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Consultant
 * in Health Informatics
 */
@Named
@SessionScoped
public class ReportTemplateController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private ReportTemplateFacade ejbFacade;
    private ReportTemplate current;
    private List<ReportTemplate> items = null;
    
    
    private Date date;
    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Department department;
    private WebUser user;
    private Staff staff;
    
    private Institution creditCompany;
    private Institution fromInstitution;
    private Department fromDepartment;
    private Institution toInstitution;
    private Department toDepartment;

    private List<ReportTemplateRow> ReportTemplateRows;
    private ReportTemplateRowBundle reportTemplateRowBundle;

    public void save(ReportTemplate reportTemplate) {
        if (reportTemplate == null) {
            return;
        }
        if (reportTemplate.getId() != null) {
            getFacade().edit(reportTemplate);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            reportTemplate.setCreatedAt(new Date());
            reportTemplate.setCreater(getSessionController().getLoggedUser());
            getFacade().create(reportTemplate);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
    }

    
    
    

    public ReportTemplate findReportTemplateByName(String name) {
        if (name == null) {
            return null;
        }
        if (name.trim().equals("")) {
            return null;
        }
        String jpql = "select a "
                + " from ReportTemplate a "
                + " where a.retired=:ret "
                + " and a.name=:n";
        Map m = new HashMap<>();
        m.put("ret", false);
        m.put("n", name);
        return getFacade().findFirstByJpql(jpql, m);
    }

    public List<ReportTemplate> completeReportTemplate(String qry) {
        List<ReportTemplate> list;
        String jpql;
        HashMap params = new HashMap();
        jpql = "select c from ReportTemplate c "
                + " where c.retired=false "
                + " and (c.name) like :q "
                + " order by c.name";
        params.put("q", "%" + qry.toUpperCase() + "%");
        list = getFacade().findByJpql(jpql, params);

        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public void prepareAdd() {
        current = new ReportTemplate();
    }

    public void recreateModel() {
        items = null;
    }

    public String processReport() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return null;
        }
        if (current.getReportTemplateType() == null) {
            JsfUtil.addErrorMessage("No report Type");
            return "";
        }
        switch (current.getReportTemplateType()) {
            case BILLT_TYPE_AND_PAYMENT_METHOD_SUMMARY_PAYMENTS:
                handleBillTypeAndPaymentMethodSummaryPayments();
                break;
            case BILLT_TYPE_AND_PAYMENT_METHOD_SUMMARY_USING_BILLS:
                handleBillTypeAndPaymentMethodSummaryUsingBills();
                break;
            case BILL_TYPE_ATOMIC_SUMMARY_USING_FEES:
                handleBillFeeGroupedByBillTypeAtomic();
                break;
            case BILL_FEE_GROUPED_BY_TO_DEPARTMENT_AND_CATEGORY:
                handleBillFeeGroupedByToDepartmentAndCategory();
                break;
            case BILL_FEE_LIST:
                handleBillFeeList();
                break;
            case BILL_ITEM_LIST:
                handleBillItemList();
                break;
            case BILL_LIST:
                handleBillList();
                break;
            case BILL_TYPE_ATOMIC_SUMMARY_USING_BILLS:
                handleBillTypeAtomicSummaryUsingBills();
                break;
            case BILL_TYPE_ATOMIC_SUMMARY_USING_PAYMENTS:
                handleBillTypeAtomicSummaryUsingPayments();
                break;
            case ENCOUNTER_LIST:
                handleEncounterList();
                break;
            case PATIENT_LIST:
                handlePatientList();
                break;
            case PAYMENT_METHOD_SUMMARY_USING_BILLS:
                handlePaymentMethodSummaryUsingBills();
                break;
            case PAYMENT_METHOD_SUMMARY_USING_PAYMENTS:
                handlePaymentMethodSummaryUsingPayments();
                break;
            case PAYMENT_TYPE_SUMMARY_PAYMENTS:
                handlePaymentTypeSummaryPayments();
                break;
            case PAYMENT_TYPE_SUMMARY_USING_BILLS:
                handlePaymentTypeSummaryUsingBills();
                break;
            case ITEM_CATEGORY_SUMMARY_BY_BILL_FEE:
                handleItemCategorySummaryByBillFee();
                break;
            case ITEM_CATEGORY_SUMMARY_BY_BILL_ITEM:
                handleItemCategorySummaryByBillItem();
                break;
            case ITEM_CATEGORY_SUMMARY_BY_BILL:
                handleItemCategorySummaryByBill();
                break;
            case TO_DEPARTMENT_SUMMARY_BY_BILL_FEE:
                handleToDepartmentSummaryByBillFee();
                break;
            case TO_DEPARTMENT_SUMMARY_BY_BILL_ITEM:
                handleToDepartmentSummaryByBillItem();
                break;
            case TO_DEPARTMENT_SUMMARY_BY_BILL:
                handleToDepartmentSummaryByBill();
                break;
            default:
                JsfUtil.addErrorMessage("Unknown Report Type");
                return "";
        }
        return "";
    }

    private void handleBillTypeAndPaymentMethodSummaryPayments() {
        // Method implementation here
    }

    private void handleBillTypeAndPaymentMethodSummaryUsingBills() {
        // Method implementation here
    }

    private void handleBillFeeGroupedByBillTypeAtomic() {
        String jpql;
        Map<String, Object> parameters = new HashMap<>();
        reportTemplateRowBundle = new ReportTemplateRowBundle();

        jpql = "select new com.divudi.data.ReportTemplateRow("
                + " bill.billTypeAtomic, sum(bf.feeValue)) "
                + " from BillFee bf "
                + " join bf.bill bill "
                + " where bf.retired<>:bfr "
                + " and bf.billItem.retired<>:bir "
                + " and bill.retired<>:br ";
        parameters.put("bfr", true);
        parameters.put("bir", true);
        parameters.put("br", true);

        if (current.getBillTypeAtomics() != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", current.getBillTypeAtomics());
        }

        for (ReportTemplateFilter f : current.getReportFilters()) {
            switch (f) {
                case DATE:
                    jpql += " and bill.billDate=:bd ";
                    parameters.put("bd", date);
                    break;
                case TO_DATE:
                    jpql += " and bill.billDate < :td ";
                    parameters.put("td", toDate);
                    break;
                case FROM_DATE:
                    jpql += " and bill.billDate > :fd ";
                    parameters.put("fd", fromDate);
                    break;
                case INSTITUTION:
                    jpql += " and bill.institution=:ins ";
                    parameters.put("ins", institution);
                    break;
                case DEPARTMENT:
                    jpql += " and bill.department=:dep ";
                    parameters.put("dep", department);
                    break;
                case FROM_INSTITUTION:
                    jpql += " and bill.fromInstitution=:fins ";
                    parameters.put("fins", fromInstitution);
                    break;
                case FROM_DEPARTMENT:
                    jpql += " and bill.fromDepartment=:fdep ";
                    parameters.put("fdep", fromDepartment);
                    break;
                case TO_INSTITUTION:
                    jpql += " and bill.toInstitution=:tins ";
                    parameters.put("tins", toInstitution);
                    break;
                case TO_DEPARTMENT:
                    jpql += " and bill.toDepartment=:tdep ";
                    parameters.put("tdep", toDepartment);
                    break;
                case CREDIT_COMPANY:
                    jpql += " and bill.creditCompany=:creditCompany ";
                    parameters.put("creditCompany", creditCompany);
                    break;
                default:
                    throw new AssertionError();
            }
        }

        jpql += " group by bill.billTypeAtomic";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.DATE);

        if (rs == null || rs.isEmpty()) {
            System.out.println("No results found.");
        } else {
            System.out.println("Results found: " + rs.size());
        }

        long idCounter = 1;
        for (ReportTemplateRow row : rs) {
            row.setId(idCounter++);
        }
        reportTemplateRowBundle.setReportTemplateRows(rs);
    }

    private void handleBillFeeGroupedByToDepartmentAndCategory() {
        String jpql;
        Map<String, Object> parameters = new HashMap<>();
        reportTemplateRowBundle = new ReportTemplateRowBundle();

        jpql = "select new com.divudi.data.ReportTemplateRow("
                + " bill.billTypeAtomic, sum(bf.feeValue)) "
                + " from BillFee bf "
                + " join bf.bill bill "
                + " where bf.retired<>:bfr "
                + " and bf.billItem.retired<>:bir "
                + " and bill.retired<>:br ";
        parameters.put("bfr", true);
        parameters.put("bir", true);
        parameters.put("br", true);

        if (current.getBillTypeAtomics() != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", current.getBillTypeAtomics());
        }

        for (ReportTemplateFilter f : current.getReportFilters()) {
            switch (f) {
                case DATE:
                    jpql += " and bill.billDate=:bd ";
                    parameters.put("bd", date);
                    break;
                case TO_DATE:
                    jpql += " and bill.billDate < :td ";
                    parameters.put("td", toDate);
                    break;
                case FROM_DATE:
                    jpql += " and bill.billDate > :fd ";
                    parameters.put("fd", fromDate);
                    break;
                case INSTITUTION:
                    jpql += " and bill.institution=:ins ";
                    parameters.put("ins", institution);
                    break;
                case DEPARTMENT:
                    jpql += " and bill.department=:dep ";
                    parameters.put("dep", department);
                    break;
                case FROM_INSTITUTION:
                    jpql += " and bill.fromInstitution=:fins ";
                    parameters.put("fins", fromInstitution);
                    break;
                case FROM_DEPARTMENT:
                    jpql += " and bill.fromDepartment=:fdep ";
                    parameters.put("fdep", fromDepartment);
                    break;
                case TO_INSTITUTION:
                    jpql += " and bill.toInstitution=:tins ";
                    parameters.put("tins", toInstitution);
                    break;
                case TO_DEPARTMENT:
                    jpql += " and bill.toDepartment=:tdep ";
                    parameters.put("tdep", toDepartment);
                    break;
                default:
                    throw new AssertionError();
            }
        }

        jpql += " group by bill.billTypeAtomic";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.DATE);

        if (rs == null || rs.isEmpty()) {
            System.out.println("No results found.");
        } else {
            System.out.println("Results found: " + rs.size());
        }

        long idCounter = 1;
        for (ReportTemplateRow row : rs) {
            row.setId(idCounter++);
        }
        reportTemplateRowBundle.setReportTemplateRows(rs);
    }

    private void handleBillFeeList() {
        // Method implementation here
    }

    private void handleBillItemList() {
        // Method implementation here
    }

    private void handleBillList() {
        // Method implementation here
    }

    private void handleBillTypeAtomicSummaryUsingBills() {
        String jpql;
        Map<String, Object> parameters = new HashMap<>();
        reportTemplateRowBundle = new ReportTemplateRowBundle();

        jpql = "select new com.divudi.data.ReportTemplateRow("
                + " bill.billTypeAtomic, sum(bill.netTotal)) "
                + " from Bill bill "
                + " where bill.retired<>:br ";
        parameters.put("br", true);

        if (current.getBillTypeAtomics() != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", current.getBillTypeAtomics());
        }

        for (ReportTemplateFilter f : current.getReportFilters()) {
            switch (f) {
                case DATE:
                    jpql += " and bill.billDate=:bd ";
                    parameters.put("bd", date);
                    break;
                case TO_DATE:
                    jpql += " and bill.billDate < :td ";
                    parameters.put("td", toDate);
                    break;
                case FROM_DATE:
                    jpql += " and bill.billDate > :fd ";
                    parameters.put("fd", fromDate);
                    break;
                case INSTITUTION:
                    jpql += " and bill.institution=:ins ";
                    parameters.put("ins", institution);
                    break;
                case DEPARTMENT:
                    jpql += " and bill.department=:dep ";
                    parameters.put("dep", department);
                    break;
                case FROM_INSTITUTION:
                    jpql += " and bill.fromInstitution=:fins ";
                    parameters.put("fins", fromInstitution);
                    break;
                case FROM_DEPARTMENT:
                    jpql += " and bill.fromDepartment=:fdep ";
                    parameters.put("fdep", fromDepartment);
                    break;
                case TO_INSTITUTION:
                    jpql += " and bill.toInstitution=:tins ";
                    parameters.put("tins", toInstitution);
                    break;
                case TO_DEPARTMENT:
                    jpql += " and bill.toDepartment=:tdep ";
                    parameters.put("tdep", toDepartment);
                    break;
                case CREDIT_COMPANY:
                    jpql += " and bill.creditCompany=:creditCompany ";
                    parameters.put("creditCompany", creditCompany);
                    break;
                default:
                    throw new AssertionError();
            }
        }

        jpql += " group by bill.billTypeAtomic";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.DATE);

        if (rs == null || rs.isEmpty()) {
            System.out.println("No results found.");
        } else {
            System.out.println("Results found: " + rs.size());
        }

        long idCounter = 1;
        for (ReportTemplateRow row : rs) {
            row.setId(idCounter++);
        }
        reportTemplateRowBundle.setReportTemplateRows(rs);

    }

    private void handleBillTypeAtomicSummaryUsingPayments() {
        // Method implementation here
    }

    private void handleEncounterList() {
        // Method implementation here
    }

    private void handlePatientList() {
        // Method implementation here
    }

    private void handlePaymentMethodSummaryUsingBills() {
        // Method implementation here
    }

    private void handlePaymentMethodSummaryUsingPayments() {
        // Method implementation here
    }

    private void handlePaymentTypeSummaryPayments() {
        // Method implementation here
    }

    private void handlePaymentTypeSummaryUsingBills() {
        // Method implementation here

    }

    public void saveSelected() {
        if (getCurrent().getName().isEmpty() || getCurrent().getName() == null) {
            JsfUtil.addErrorMessage("Please enter Value");
            return;
        }
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

    public ReportTemplateFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ReportTemplateFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ReportTemplateController() {
    }

    public ReportTemplate getCurrent() {
        if (current == null) {
            current = new ReportTemplate();
        }
        return current;
    }

    public void setCurrent(ReportTemplate current) {
        this.current = current;
    }

    private List<ReportTemplateColumn> getReportTemplateColumns(String input) {
        List<ReportTemplateColumn> columns = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return columns;
        }
        String[] lines = input.split("\\r?\\n");
        for (String line : lines) {
            for (ReportTemplateColumn column : ReportTemplateColumn.values()) {
                if (column.getLabel().equalsIgnoreCase(line.trim())) {
                    columns.add(column);
                    break;
                }
            }
        }
        return columns;
    }

    private List<ReportTemplateFilter> getReportTemplateFilters(String input) {
        List<ReportTemplateFilter> columns = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return columns;
        }
        String[] lines = input.split("\\r?\\n");
        for (String line : lines) {
            for (ReportTemplateFilter column : ReportTemplateFilter.values()) {
                if (column.getLabel().equalsIgnoreCase(line.trim())) {
                    columns.add(column);
                    break;
                }
            }
        }
        return columns;
    }

    private List<BillTypeAtomic> getBillTypeAtomics(String input) {
        List<BillTypeAtomic> columns = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return columns;
        }
        String[] lines = input.split("\\r?\\n");
        for (String line : lines) {
            for (BillTypeAtomic column : BillTypeAtomic.values()) {
                if (column.getLabel().equalsIgnoreCase(line.trim())) {
                    columns.add(column);
                    break;
                }
            }
        }
        return columns;
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

    private ReportTemplateFacade getFacade() {
        return ejbFacade;
    }

    public String navigateToReportTemplateList() {
        items = getAllItems();
        return "/dataAdmin/report_template_list";
    }

    public String navigateToAddNewReportTemplate() {
        current = new ReportTemplate();
        return "/dataAdmin/report_template";
    }

    public void deleteReportTemplate() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        current.setRetired(true);
        current.setRetirer(sessionController.getLoggedUser());
        current.setRetiredAt(new Date());
        save(current);
        items = getAllItems();
        JsfUtil.addSuccessMessage("Removed");
    }

    public List<ReportTemplateType> getReportTemplateTypes() {
        return Arrays.asList(ReportTemplateType.values());
    }

    public String navigateToEditReportTemplate() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return null;
        }
        return "/dataAdmin/report_template?faces-redirect=true";
    }

    public String navigateToGenerateReport() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return null;
        }
        return "/dataAdmin/report?faces-redirect=true";
    }

    public String navigateToEditGenerateReport() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return null;
        }
        return "/dataAdmin/report";
    }

    public List<ReportTemplate> getAllItems() {
        List<ReportTemplate> allItems;
        String j;
        j = "select a "
                + " from ReportTemplate a "
                + " where a.retired=false "
                + " order by a.name";
        allItems = getFacade().findByJpql(j);
        return allItems;
    }

    public List<ReportTemplate> getItems() {
        return items;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public WebUser getUser() {
        return user;
    }

    public void setUser(WebUser user) {
        this.user = user;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public List<ReportTemplateRow> getReportTemplateRows() {
        return ReportTemplateRows;
    }

    public void setReportTemplateRows(List<ReportTemplateRow> ReportTemplateRows) {
        this.ReportTemplateRows = ReportTemplateRows;
    }

    public ReportTemplateRowBundle getReportTemplateRowBundle() {
        return reportTemplateRowBundle;
    }

    public void setReportTemplateRowBundle(ReportTemplateRowBundle reportTemplateRowBundle) {
        this.reportTemplateRowBundle = reportTemplateRowBundle;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }
    /**
     *
     */
    private void handleItemCategorySummaryByBillFee() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void handleItemCategorySummaryByBillItem() {
        String jpql;
        Map<String, Object> parameters = new HashMap<>();
        reportTemplateRowBundle = new ReportTemplateRowBundle();

        jpql = "select new com.divudi.data.ReportTemplateRow("
                + " bi.item.category.name, sum(bi.netValue)) "
                + " from BillItem bi"
                + " join bi.bill bill "
                + " where bill.retired<>:br "
                + " and bi.retired<>:br ";
        parameters.put("br", true);

        if (current.getBillTypeAtomics() != null) {
            jpql += " and bill.billTypeAtomic in :btas ";
            parameters.put("btas", current.getBillTypeAtomics());
        }

        for (ReportTemplateFilter f : current.getReportFilters()) {
            switch (f) {
                case DATE:
                    jpql += " and bill.billDate=:bd ";
                    parameters.put("bd", date);
                    break;
                case TO_DATE:
                    jpql += " and bill.billDate < :td ";
                    parameters.put("td", toDate);
                    break;
                case FROM_DATE:
                    jpql += " and bill.billDate > :fd ";
                    parameters.put("fd", fromDate);
                    break;
                case INSTITUTION:
                    jpql += " and bill.institution=:ins ";
                    parameters.put("ins", institution);
                    break;
                case DEPARTMENT:
                    jpql += " and bill.department=:dep ";
                    parameters.put("dep", department);
                    break;
                case FROM_INSTITUTION:
                    jpql += " and bill.fromInstitution=:fins ";
                    parameters.put("fins", fromInstitution);
                    break;
                case FROM_DEPARTMENT:
                    jpql += " and bill.fromDepartment=:fdep ";
                    parameters.put("fdep", fromDepartment);
                    break;
                case TO_INSTITUTION:
                    jpql += " and bill.toInstitution=:tins ";
                    parameters.put("tins", toInstitution);
                    break;
                case TO_DEPARTMENT:
                    jpql += " and bill.toDepartment=:tdep ";
                    parameters.put("tdep", toDepartment);
                    break;
                case CREDIT_COMPANY:
                    jpql += " and bill.creditCompany=:creditCompany ";
                    parameters.put("creditCompany", creditCompany);
                    break;
                default:
                    throw new AssertionError();
            }
        }

        jpql += " and bi.item is not null "
                + " and bi.item.category is not null ";
        
        jpql += " group by bi.item.category ";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) ejbFacade.findLightsByJpql(jpql, parameters, TemporalType.DATE);

        if (rs == null || rs.isEmpty()) {
            System.out.println("No results found.");
        } else {
            System.out.println("Results found: " + rs.size());
        }

        long idCounter = 1;
        for (ReportTemplateRow row : rs) {
            row.setId(idCounter++);
        }
        reportTemplateRowBundle.setReportTemplateRows(rs);
    }

    private void handleItemCategorySummaryByBill() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void handleToDepartmentSummaryByBillFee() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void handleToDepartmentSummaryByBillItem() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void handleToDepartmentSummaryByBill() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

 
    public static class ReportTemplateConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ReportTemplateController controller = (ReportTemplateController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "reportTemplateController");
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
            if (object instanceof ReportTemplate) {
                ReportTemplate o = (ReportTemplate) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ReportTemplate.class.getName());
            }
        }
    }

}
