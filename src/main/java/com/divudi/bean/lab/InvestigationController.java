/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.ItemFeeManager;
import com.divudi.bean.common.ItemForItemController;
import com.divudi.bean.common.SessionController;

import com.divudi.data.InvestigationItemType;
import com.divudi.data.ItemType;
import com.divudi.data.SymanticType;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.data.lab.InvestigationWithCount;
import com.divudi.data.lab.JsonInvestigation;
import com.divudi.data.lab.JsonInvestigationList;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.clinical.ClinicalEntity;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.InvestigationCategory;
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.entity.lab.InvestigationItemValueFlag;
import com.divudi.entity.lab.PatientReport;
import com.divudi.entity.lab.ReportItem;
import com.divudi.entity.lab.WorksheetItem;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.InvestigationItemFacade;
import com.divudi.facade.InvestigationItemValueFlagFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.SpecialityFacade;
import com.divudi.facade.WorksheetItemFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class InvestigationController implements Serializable {

    /**
     * Managed Beans
     */
    @Inject
    SessionController sessionController;
    @Inject
    CommonController commonController;
    @Inject
    private BillBeanController billBean;
    @Inject
    InvestigationItemController investigationItemController;
    @Inject
    IxCalController ixCalController;
    @Inject
    ItemFeeManager itemFeeManager;
    @Inject
    PatientReportController patientReportController;
    @Inject
    ItemForItemController itemForItemController;
    /**
     * EJBs
     */
    @EJB
    private InvestigationFacade ejbFacade;
    @EJB
    private SpecialityFacade specialityFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    InvestigationItemFacade investigationItemFacade;
    @EJB
    InvestigationItemValueFlagFacade investigationItemValueFlagFacade;
    /**
     * Properties
     */
    List<Investigation> selectedItems;
    List<Investigation> selectedInvestigations;
    private Investigation current;
    private List<Investigation> items = null;
    String selectText = "";
    String bulkText = "";
    boolean billedAs;
    boolean reportedAs;
    Boolean listMasterItemsOnly = false;//if boolean is true list only institution null
    InvestigationCategory category;
    List<Investigation> catIxs;
    List<Investigation> allIxs;
    List<Investigation> itemsToRemove;
    Institution institution;
    Department department;
    List<Investigation> deletedIxs;
    List<Investigation> selectedIxs;
    List<PatientReport> selectedPatientReports;
    List<Investigation> ixWithoutSamples;
    List<InvestigationWithInvestigationItems> investigationWithInvestigationItemses;
    List<ItemWithFee> itemWithFees;
    private List<Investigation> investigationWithSelectedFormat;
    private Category categoryForFormat;
    private UploadedFile file;
    private StreamedContent downloadingFile;
    private int adminTabIndex;

    @Deprecated
    public String navigateToManageInvestigationForEmr() {
        return "/emr/admin/investigations";
    }

    @Deprecated
    public String navigateToAddInvestigationForLab() {
        current = new Investigation();
        return "/admin/lims/index";
    }

    public String navigateToLimsAdminIndex() {
        return "/admin/lims/index";
    }

    @Deprecated
    public String navigateToAddInvestigationForAdmin() {
        current = new Investigation();
        return "/admin/lims/investigation";
    }

    public String navigateToAddInvestigation() {
        current = new Investigation();
        return "/admin/lims/investigation";
    }

    public String navigateToAddInvestigationForLabForExport() {
        current = new Investigation();
        return "/lab/investigation_list_for_export";
    }

    public String navigateToManageInvestigationForLab() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return "";
        }
        return "/lab/manage_investigation";
    }

    // Method to generate the Excel file and initiate the download
    public void downloadAsExcelForEmr() {
        getItems();
        try {
            // Create a new Excel workbook
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Diagnoses");

            // Create a header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("No");
            headerRow.createCell(1).setCellValue("Name");
            // Add more columns as needed

            // Populate the data rows
            int rowNum = 1;
            for (Investigation diag : items) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum);
                row.createCell(1).setCellValue(diag.getName());
            }

            // Set the response headers to initiate the download
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"diagnoses.xlsx\"");

            // Write the workbook to the response output stream
            workbook.write(response.getOutputStream());
            workbook.close();
            context.responseComplete();
        } catch (Exception e) {
            // Handle any exceptions
            e.printStackTrace();
        }
    }

    public String navigateToManageValueSetsForAdmin() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return "";
        }
        return "/lab/value_sets";
    }

    public String navigateToManageFlagsForLab() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return "";
        }
        return "/lab/flags";
    }

    @Deprecated
    public String navigateToViewInvestigationForAdmin() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to delete");
            return "";
        }
        return "/admin/lims/investigation_single";
    }

    public String navigateToManageInvestigation() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to delete");
            return "";
        }
        return "/admin/lims/investigation";
    }

    public String navigateToListInvestigationsForAdmin() {
        fillItems();
        return "/admin/items/investigation_list";
    }

    public String toAddManyIx() {
        current = new Investigation();
        current.setInwardChargeType(InwardChargeType.Laboratory);
        return "/lab/add_many_ix";
    }

    public String saveManyIx() {
        if (current == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        String names[] = current.getComments().split("\\r?\\n");
        for (String n : names) {
            Investigation i = new Investigation();
            i.setName(n);
            i.setPrintName(n);
            i.setFullName(n);
            if (n.length() > 3) {
                i.setCode(n.substring(0, 3));
            } else {
                i.setCode(n);
            }
            i.setReportType(current.getReportType());
            i.setInvestigationCategory(current.getInvestigationCategory());
            i.setInvestigationTube(current.getInvestigationTube());
            i.setMachine(current.getMachine());
            i.setSessionNumberType(current.getSessionNumberType());
            i.setWorksheet(current.getWorksheet());
            i.setReportFormat(current.getReportFormat());
            i.setSample(current.getSample());
            i.setSampleVolume(current.getSampleVolume());
            i.setInstitution(current.getInstitution());
            i.setDepartment(current.getDepartment());
            i.setInwardChargeType(current.getInwardChargeType());
            i.setChargesVisibleForInward(current.isChargesVisibleForInward());
            i.setUserChangable(current.isUserChangable());
            i.setMarginNotAllowed(current.isMarginNotAllowed());
            i.setRequestForQuentity(current.isRequestForQuentity());
            i.setDiscountAllowed(current.isDiscountAllowed());
            i.setVatable(current.isVatable());
            i.setVatPercentage(current.getVatPercentage());
            i.setCreatedAt(new Date());
            i.setCreater(getSessionController().getLoggedUser());
            getFacade().create(i);

            i.setReportedAs(i);
            i.setBilledAs(i);
            i.setCategory(i.getInvestigationCategory());
            i.setSymanticType(SymanticType.Laboratory_Procedure);
            i.setInwardChargeType(InwardChargeType.Laboratory);

            getFacade().edit(i);

        }
        JsfUtil.addSuccessMessage("All Added");
        return toAddManyIx();
    }

    public StreamedContent getIxToJsonFile() {
        createJsonForListOfInvestigations(selectedInvestigations);
        return downloadingFile;
    }

    public void createJsonForListOfInvestigations(List<Investigation> ixs) {
        if (ixs == null || ixs.isEmpty()) {
            downloadingFile = null;
            return;
        }
        JsonInvestigationList jixlist = new JsonInvestigationList();
        for (Investigation i : ixs) {
            JsonInvestigation j = new JsonInvestigation();
            j.setName(i.getName());
            j.setCode(i.getCode());
            j.setFullName(i.getFullName());
            j.setPrintingName(i.getPrintName());
            if (i.getInvestigationCategory() != null) {
                j.setCategory(i.getInvestigationCategory().getName());
            }
            if (i.getSample() != null) {
                j.setSample(i.getSample().getName());
            }
            if (i.getInvestigationTube() != null) {
                j.setTube(i.getInvestigationTube().getName());
            }
            if (i.getMachine() != null) {
                j.setAnalyzer(i.getMachine().getName());
            }
            if (i.getPriority() != null) {
                j.setPrintingName(i.getPriority().toString());
            }

            jixlist.getInvestigations().add(j);
        }
        String j = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            j = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jixlist);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(InvestigationItemController.class.getName()).log(Level.SEVERE, null, ex);
        }
        InputStream stream = new ByteArrayInputStream(j.getBytes(Charset.defaultCharset()));
        downloadingFile = DefaultStreamedContent.builder().contentType("image/jpeg").name("selected_investigations.json").stream(() -> stream).build();
    }

    public String toUuploadJsonToInvestigations() {
        return "/admin/lab/upload_investigations";
    }

    public String uploadJsonToCreateAnInvestigations() {
        if (file == null) {
            JsfUtil.addErrorMessage("No file");
            return "";
        }
        try {
            InputStream inputStream = file.getInputStream();
            String text = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

        } catch (IOException ex) {
        }
        return "/lab/investigation_format";
    }

    public String uploadExcelToCreateAnInvestigations() {
        //file means private UploadedFile file;
        if (file == null) {
            JsfUtil.addErrorMessage("No file");
            return "";
        }
        try {
            InputStream inputStream = file.getInputStream();

        } catch (IOException ex) {
        }
        return "";
    }

    public void changeIxInstitutionAccordingToDept() {
        List<Investigation> ixs = getFacade().findAll(true);
        for (Investigation ix : ixs) {
            if (ix.getInstitution() != null && !ix.getDepartment().getInstitution().equals(ix.getInstitution())) {
                ix.setInstitution(ix.getDepartment().getInstitution());
                getFacade().edit(ix);
            }
        }
    }

    public void changeIxReportedAsToMasterItem() {
        List<Investigation> ixs = getFacade().findAll(true);
        for (Investigation ix : ixs) {
            if (ix.getReportedAs() != null && ix.getInstitution() != null && ix.getReportedAs().getInstitution() != null) {
                String j;
                Map m = new HashMap();
                j = "select ix from Investigation ix "
                        + " where ix.retired=false "
                        + " and ix.institution is null "
                        + " and ix.name=:ixn";
                m.put("ixn", ix.getName());

                getFacade().edit(ix);
            }
        }
    }

    public String navigateToEditFormatSingle() {
        if (current == null) {
            JsfUtil.addErrorMessage("Please select investigation");
            return "";
        }
        if (current.getId() == null) {
            JsfUtil.addErrorMessage("Please save investigation first.");
            return "";
        }
        if (current.getReportedAs() == null) {
            current.setReportedAs(current);
        }
        investigationItemController.setCurrentInvestigation((Investigation) current.getReportedAs());
        return investigationItemController.toEditInvestigationFormat();
    }

    public String navigateToManageReportComponents() {
        if (current == null) {
            JsfUtil.addErrorMessage("Please select investigation");
            return "";
        }
        if (current.getId() == null) {
            JsfUtil.addErrorMessage("Please save investigation first.");
            return "";
        }
        if (current.getReportedAs() == null) {
            current.setReportedAs(current);
        }
        investigationItemController.setCurrentInvestigation((Investigation) current.getReportedAs());

        return "/admin/lims/investigation_values";
    }

    public String toLoadParentInvestigation() {
        if (current == null) {
            JsfUtil.addErrorMessage("Please select investigation");
            return "";
        }
        if (current.getId() == null) {
            JsfUtil.addErrorMessage("Please save investigation first.");
            return "";
        }
        if (current.getReportedAs() != null) {
            current = (Investigation) current.getReportedAs();
        }
        return toManageInvestigationDetails();
    }

    public String navigateToEditFormatMultiple() {
        if (current == null) {
            JsfUtil.addErrorMessage("Please select investigation");
            return "";
        }
        if (current.getId() == null) {
            JsfUtil.addErrorMessage("Please save investigation first.");
            return "";
        }
        if (current.getReportedAs() == null) {
            current.setReportedAs(current);
        }
        investigationItemController.setCurrentInvestigation((Investigation) current.getReportedAs());

        return investigationItemController.toEditInvestigationFormatMultiple();
    }

    public String navigateToEditPathologyFormat() {
        if (current == null) {
            JsfUtil.addErrorMessage("Please select investigation");
            return "";
        }
        return "/admin/lims/pathology_format";
    }

    public String navigateToManageCalculations() {
        if (current == null) {
            JsfUtil.addErrorMessage("Please select investigation");
            return "";
        }
        if (current.getId() == null) {
            JsfUtil.addErrorMessage("Please save investigation first.");
            return "";
        }
        if (current.getReportedAs() == null) {
            current.setReportedAs(current);
        }
        ixCalController.setIx((Investigation) current.getReportedAs());
        return "/admin/lims/calculation";
    }

    public String navigateToReplaceableInvestigations() {
        if (current == null) {
            JsfUtil.addErrorMessage("Please select investigation");
            return "";
        }
        if (current.getId() == null) {
            JsfUtil.addErrorMessage("Please save investigation first.");
            return "";
        }
        if (current.getReportedAs() == null) {
            current.setReportedAs(current);
        }
        itemForItemController.setParentItem(current);
        return "/admin/lims/replaceable_ix";
    }

    public String navigateToManageFees() {
        if (current == null) {
            JsfUtil.addErrorMessage("Please select investigation");
            return "";
        }
        if (current.getId() == null) {
            JsfUtil.addErrorMessage("Please save investigation first.");
            return "";
        }
        itemFeeManager.setItem(current);
        itemFeeManager.fillFees();
        return "/admin/lims/manage_fees";
    }

    public void listDeletedIxs() {
        String sql = "select c from Investigation c where c.retired=true ";
        deletedIxs = getFacade().findByJpql(sql);
    }

    public void undeleteSelectedIxs() {
        for (Investigation s : selectedIxs) {
            s.setRetired(false);
            s.setRetiredAt(null);
            s.setRetirer(null);
            getFacade().edit(s);
            ////// // System.out.println("undeleted = " + s);
        }
        selectedIxs = null;
        listDeletedIxs();
    }

    public void deleteSelectedInvestigations() {
        for (Investigation s : selectedIxs) {
            s.setRetired(true);
            s.setRetiredAt(new Date());
            s.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(s);
        }
    }

    public List<Investigation> getDeletedIxs() {
        return deletedIxs;
    }

    public void setDeletedIxs(List<Investigation> deletedIxs) {
        this.deletedIxs = deletedIxs;
    }

    public List<Investigation> getSelectedIxs() {
        return selectedIxs;
    }

    public void setSelectedIxs(List<Investigation> selectedIxs) {
        this.selectedIxs = selectedIxs;
    }

    public List<Investigation> getItemsToRemove() {
        return itemsToRemove;
    }

    public void setItemsToRemove(List<Investigation> itemsToRemove) {
        this.itemsToRemove = itemsToRemove;
    }

    public void removeSelectedItems() {
        for (Investigation s : itemsToRemove) {
            s.setRetired(true);
            s.setRetireComments("Bulk Remove");
            s.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(s);
        }
        itemsToRemove = null;
        items = null;
    }

    public void listAllIxs() {
        String sql;
        sql = "Select i from Investigation i where i.retired=false ";
        sql += " order by i.name";
        allIxs = getFacade().findByJpql(sql);
    }

    public String listFilteredIxs() {
        String sql;
        Map m = new HashMap();
        sql = "Select i from Investigation i where i.retired=false ";
        if (institution != null) {
            sql += " and i.institution=:ins";
            m.put("ins", institution);
        }
        if (department != null) {
            sql += " and i.department=:dep";
            m.put("dep", department);
        }
        if (category != null) {
            sql += " and i.category=:cat";
            m.put("cat", category);
        }
        sql += " order by i.name";
        allIxs = getFacade().findByJpql(sql, m);
        return "/lab/investigation_list";
    }

    public void clearFields() {
        institution = null;
        department = null;
        category = null;
    }

    public void prepareSelectedReportSamples() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        selectedPatientReports = new ArrayList<>();
        ixWithoutSamples = new ArrayList<>();
        for (Investigation ix : selectedIxs) {
            PatientReport pr = patientReportController.getLastPatientReport(ix);
            if (pr != null) {
                selectedPatientReports.add(pr);
            } else {
                ixWithoutSamples.add(ix);
            }
        }

        
    }

    public List<PatientReport> getSelectedPatientReports() {
        return selectedPatientReports;
    }

    public void setSelectedPatientReports(List<PatientReport> selectedPatientReports) {
        this.selectedPatientReports = selectedPatientReports;
    }

    public List<Investigation> getIxWithoutSamples() {
        return ixWithoutSamples;
    }

    public void setIxWithoutSamples(List<Investigation> ixWithoutSamples) {
        this.ixWithoutSamples = ixWithoutSamples;
    }

    public List<Investigation> getInvestigationItems() {
        String sql;
        sql = "Select i from Investigation i "
                + " where i.retired=false ";

        return getFacade().findByJpql(sql);
    }

    public List<Department> getInstitutionDepatrments() {
        List<Department> d;
        if (getCurrent().getInstitution() == null) {
            return new ArrayList<>();
        } else {
            String sql = "Select d From Department d where d.retired=false and d.institution=:ins order by d.name";
            Map m = new HashMap();
            m.put("ins", getCurrent().getInstitution());
            d = getDepartmentFacade().findByJpql(sql, m);
        }

        return d;
    }

    public List<Department> getDepatrmentsOfSelectedInstitution() {
        List<Department> d;
        if (getInstitution() == null) {
            return new ArrayList<>();
        } else {
            String sql = "Select d From Department d where d.retired=false and d.institution=:ins order by d.name";
            Map m = new HashMap();
            m.put("ins", institution);
            d = getDepartmentFacade().findByJpql(sql, m);
        }
        return d;
    }

    public InvestigationCategory getCategory() {
        return category;
    }

    public void setCategory(InvestigationCategory category) {
        catIxs = null;
        this.category = category;
    }

    public void catToIxCat() {
        for (Investigation i : getItems()) {
            i.setCategory(i.getInvestigationCategory());
            getFacade().edit(i);
        }
        JsfUtil.addSuccessMessage("Saved");
    }

    @EJB
    WorksheetItemFacade worksheetItemFacade;

    public WorksheetItemFacade getWorksheetItemFacade() {
        return worksheetItemFacade;
    }

    public void setWorksheetItemFacade(WorksheetItemFacade worksheetItemFacade) {
        this.worksheetItemFacade = worksheetItemFacade;
    }

    public void reportItemsToWorksheetItems() {
        for (WorksheetItem wi : getWorksheetItemFacade().findAll()) {
            //////// // System.out.println("item removing is " + wi);
            getWorksheetItemFacade().remove(wi);
        }
        for (Investigation i : getItems()) {
            for (ReportItem ri : i.getReportItems()) {
                if (ri.getIxItemType() == InvestigationItemType.Value && ri.isRetired() == false) {
                    WorksheetItem wi = new WorksheetItem();
                    wi.setItem(i);
                    wi.setName(ri.getName());
                    i.getWorksheetItems().add(wi);
                    //////// // System.out.println("Worksheet added " + wi);
                }
            }
            getItemFacade().edit(i);
        }
    }

    @EJB
    ItemFacade itemFacade;

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public List<Investigation> getCatIxs() {
        if (catIxs == null) {
            if (category == null) {
                catIxs = getItems();
            } else {
                Map m = new HashMap();
                String sql = "select i from Investigation i where i.retired=false and i.investigationCategory = :cat order by i.department.name, i.name";
                m.put("cat", getCategory());
                catIxs = getFacade().findByJpql(sql, m);
            }
        }
        return catIxs;
    }

    public void setCatIxs(List<Investigation> catIxs) {
        this.catIxs = catIxs;
    }

    public List<Investigation> completeInvestigationsOfCurrentInstitution(String query) {
        if (query == null || query.trim().equals("")) {
            return new ArrayList<>();
        }
        List<Investigation> suggestions;
        String sql;
        Map m = new HashMap();
        sql = "select c from Investigation c "
                + " where c.retired=false  "
                + " and ((c.name) like :n or "
                + " (c.fullName) like :n or "
                + " (c.code) like :n or (c.printName) like :n ) ";
        sql += " and c.institution = :ins ";
        sql += " order by c.name";
        m.put("n", "%" + query.toUpperCase() + "%");
        Institution ins;
        if (institution != null) {
            ins = institution;
        } else {
            ins = getSessionController().getLoggedUser().getInstitution();
        }
        m.put("ins", ins);
        suggestions = getFacade().findByJpql(sql, m);
        return suggestions;
    }

    public List<Investigation> completeInvestigationsOfSelectedInstitution(String query) {
        if (query == null || query.trim().equals("")) {
            return new ArrayList<>();
        }
        List<Investigation> suggestions;
        String sql;
        Map m = new HashMap();

        Institution ins;
        if (institution != null) {
            sql = "select c from Investigation c "
                    + " where c.retired=false  "
                    + " and ((c.name) like :n or "
                    + " (c.fullName) like :n or "
                    + " (c.code) like :n or (c.printName) like :n ) ";
            sql += " and c.institution = :ins ";
            sql += " order by c.name";
            m.put("n", "%" + query.toUpperCase() + "%");
            m.put("ins", institution);
        } else {
            sql = "select c from Investigation c "
                    + " where c.retired=false  "
                    + " and ((c.name) like :n or "
                    + " (c.fullName) like :n or "
                    + " (c.code) like :n or (c.printName) like :n ) ";
            sql += " and c.institution is null ";
            sql += " order by c.name";
            m.put("n", "%" + query.toUpperCase() + "%");
        }

        suggestions = getFacade().findByJpql(sql, m);
        return suggestions;
    }

    public List<Investigation> completeInvest(String query) {
        if (query == null || query.trim().equals("")) {
            return new ArrayList<>();
        }
        List<Investigation> suggestions;
        String sql;
        Map m = new HashMap();

        //m.put(m, m);
        sql = "select c from Investigation c "
                + " where c.retired=false "
                + " and ((c.name) like :n or "
                + " (c.fullName) like :n or "
                + " (c.code) like :n or (c.printName) like :n ) ";
        //////// // System.out.println(sql);

        m.put("n", "%" + query.toUpperCase() + "%");

        if (listMasterItemsOnly == true) {
            sql += " and c.institution is null ";
        }

//        if (sessionController.getApplicationPreference().isInstitutionSpecificItems()) {
//            sql += " and (c.institution is null "
//                    + " or c.institution=:ins) ";
//            m.put("ins", sessionController.getInstitution());
//        }
        sql += " order by c.name";

        suggestions = getFacade().findByJpql(sql, m);

        return suggestions;
    }

    public List<Investigation> completeInvestigation(String query) {
        if (query == null || query.trim().equals("")) {
            return new ArrayList<>();
        }
        List<Investigation> suggestions;
        String jpql;
        Map m = new HashMap();
        jpql = "select c from Investigation c "
                + " where c.retired=false "
                + " and "
                + " ( "
                + " c.name like :n or "
                + " c.fullName like :n or "
                + " c.code like :n or c.printName like :n"
                + " ) ";

        m.put("n", "%" + query.toUpperCase() + "%");
        jpql += " order by c.name";
        suggestions = getFacade().findByJpql(jpql, m);
        return suggestions;
    }

    public List<InvestigationWithCount> completeInvestWithIiCount(String query) {
        if (query == null || query.trim().equals("")) {
            return new ArrayList<>();
        }
        List<Investigation> suggestions;
        String sql;
        Map m = new HashMap();

        //m.put(m, m);
        sql = "select c from Investigation c "
                + " where c.retired=false "
                + " and ((c.name) like :n or "
                + " (c.fullName) like :n or "
                + " (c.code) like :n or (c.printName) like :n ) ";
        //////// // System.out.println(sql);

        m.put("n", "%" + query.toUpperCase() + "%");

        if (listMasterItemsOnly == true) {
            sql += " and c.institution is null ";
        }

//        if (sessionController.getApplicationPreference().isInstitutionSpecificItems()) {
//            sql += " and (c.institution is null "
//                    + " or c.institution=:ins) ";
//            m.put("ins", sessionController.getInstitution());
//        }
        sql += " order by c.name";

        suggestions = getFacade().findByJpql(sql, m);

        List<InvestigationWithCount> ics = new ArrayList<>();
        for (Investigation ix : suggestions) {
            InvestigationWithCount ic = new InvestigationWithCount(ix, investigationItemController.findItemCount(ix));
            ics.add(ic);
        }

        return ics;

    }

    public List<Investigation> completeInvestWithout(String query) {
        List<Investigation> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            // sql = "select c from Investigation c where c.retired=false and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            sql = "select c from Investigation c where c.retired=false and type(c)!=Packege and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            //////// // System.out.println(sql);
            suggestions = getFacade().findByJpql(sql);
        }
        return suggestions;
    }

    public boolean isBilledAs() {
        return billedAs;
    }

    public void setBilledAs(boolean billedAs) {
        this.billedAs = billedAs;
    }

    public boolean isReportedAs() {
        return reportedAs;
    }

    public void setReportedAs(boolean reportedAs) {
        this.reportedAs = reportedAs;
    }

    public Boolean isListMasterItemsOnly() {
        if (listMasterItemsOnly == null) {
            if (getSessionController().getApplicationPreference().isInstitutionSpecificItems()) {
                listMasterItemsOnly = true;
            } else {
                listMasterItemsOnly = false;
            }
        }
        return listMasterItemsOnly;
    }

    public Boolean getListMasterItemsOnly() {
        if (listMasterItemsOnly == null) {
            if (getSessionController().getApplicationPreference().isInstitutionSpecificItems()) {
                listMasterItemsOnly = true;
            } else {
                listMasterItemsOnly = false;
            }
        }
        return listMasterItemsOnly;
    }

    public void setListMasterItemsOnly(Boolean listMasterItemsOnly) {
        this.listMasterItemsOnly = listMasterItemsOnly;
    }

    public String getBulkText() {

        return bulkText;
    }

    public void setBulkText(String bulkText) {
        this.bulkText = bulkText;
    }

    public void deleteIxWithoutIxAndFixReportedAs() {
        String j = "select i from Investigation i";
        List<Investigation> ixs = getFacade().findByJpql(j);
        for (Investigation ix : ixs) {
            if (ix.getInstitution() == null) {
                ix.setRetired(true);
                ix.setRetiredAt(new Date());
                ix.setRetirer(sessionController.getLoggedUser());
            } else {
                ix.setReportedAs(ix);
                ix.setBilledAs(ix);
            }
            getFacade().edit(ix);
        }
    }

    public List<Investigation> getInstitutionSelectedItems() {
        Map m = new HashMap();
        String sql;
        sql = "select c "
                + " from Investigation c "
                + " where c.retired=:r ";
        m.put("r", false);
        if (selectText != null && !selectText.trim().equals("")) {
            sql += " and (c.name) like :st ";
            m.put("st", "%" + getSelectText().toUpperCase() + "%");
        }
        if (sessionController.getApplicationPreference().isInstitutionSpecificItems()) {
            if (institution != null) {
                sql += " and c.institution=:ins ";
                m.put("ins", institution);
            } else {
                sql += " and c.institution is null ";
            }
        }
        sql += " order by c.name";
        selectedItems = getFacade().findByJpql(sql, m);
        return selectedItems;
    }

    public List<Investigation> getSelectedInvestigations() {
        return selectedInvestigations;
    }

    public void setSelectedInvestigations(List<Investigation> selectedInvestigations) {
        this.selectedInvestigations = selectedInvestigations;
    }

    public void deleteSelectedItems() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (selectedInvestigations.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing to Delete");
            return;
        }

        for (Investigation i : selectedInvestigations) {
            i.setRetired(true);
            i.setRetiredAt(new Date());
            i.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(i);
        }
        JsfUtil.addSuccessMessage("Successfully Deleted");
        selectedInvestigations = null;

        
    }

    public void unDeleteSelectedItems() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (selectedInvestigations.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing to Un-Delete");
            return;
        }

        for (Investigation i : selectedInvestigations) {
            i.setRetired(false);
            i.setRetiredAt(new Date());
            i.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(i);
        }
        JsfUtil.addSuccessMessage("Successfully Deleted");
        selectedInvestigations = null;

        
    }

    public void markSelectedActive() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (selectedInvestigations.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing to Active");
            return;
        }

        for (Investigation i : selectedInvestigations) {
            i.setInactive(false);
            getFacade().edit(i);
        }

        JsfUtil.addSuccessMessage("Successfully Actived");
        selectedInvestigations = null;

        

    }

    public void markSelectedInactive() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (selectedInvestigations.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing to Inactive");
            return;
        }

        for (Investigation i : selectedInvestigations) {
            i.setInactive(true);
            getFacade().edit(i);
        }

        JsfUtil.addSuccessMessage("Successfully Inactived");
        selectedInvestigations = null;

        
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<Investigation> getSelectedItems() {
        if (selectText.trim().equals("")) {
            selectedItems = getFacade().findByJpql("select c from Investigation c where c.retired=false order by c.name");
        } else {
            selectedItems = getFacade().findByJpql("select c from Investigation c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        }
        return selectedItems;
    }

    public List<Investigation> completeItem(String qry) {
        List<Investigation> completeItems = getFacade().findByJpql("select c from Item c where ( type(c) = Investigation or type(c) = Packege ) and c.retired=false and (c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
        return completeItems;
    }

//    public List<Investigation> completeDepartmentItem(String qry) {
//        if (getSessionController().getApplicationPreference().isInstitutionSpecificItems()) {
//            String sql;
//            Map m = new HashMap();
//            m.put("qry", "'%" + qry.toUpperCase() + "%'");
//            m.put("inv", Investigation.class);
//            m.put("ser", Investigation.class);
//            m.put("pak", Investigation.class);
//            m.put("ins", getSessionController().getInstitution());
//            sql = "select c "
//                    + " from Item c "
//                    + " where (type(c) =:inv or type(c) = :ser or type(c) = :pak) "
//                    + " and c.retired=false "
//                    + " and (c.name) like :qry "
//                    + " and c.institution=:ins ";
//            sql += "order by c.name";
//            List<Investigation> completeItems = getFacade().findByJpql(sql, m);
//            return completeItems;
//        } else {
//            return completeItem(qry);
//        }
//    }
    public List<Investigation> completeDepartmentItem(String qry) {
        if (getSessionController().getApplicationPreference().isInstitutionSpecificItems()) {
            String sql;
            Map m = new HashMap();
//            m.put("qry", "'%" + qry.toUpperCase() + "%'");
//            m.put("inv", Investigation.class);
//            m.put("ser", Investigation.class);
//            m.put("pak", Investigation.class);
            m.put("ins", getSessionController().getInstitution());
            sql = "select c from Item c where ( type(c) = Investigation or type(c) = Packege ) "
                    + "and c.retired=false and c.institution=:ins and (c.name) like '%" + qry.toUpperCase() + "%' order by c.name";
            List<Investigation> completeItems = getFacade().findByJpql(sql, m);
            return completeItems;
        } else {
            return completeItem(qry);
        }
    }

    public String toManageInvestigationDetails() {
        if (institution == null) {
            institution = getSessionController().getLoggedUser().getInstitution();
        }
        return "/admin/lims/investigation";
    }

    public String navigateToListInvestigation() {
        listAllIxs();
        return "/admin/lims/investigation_list";
    }

    public String navigateToManageReportTemplateNames() {
        listAllIxs();
        return "/admin/lims/investigation_list";
    }

    public void prepareAdd() {
        current = new Investigation();
        current.setInwardChargeType(InwardChargeType.Laboratory);
    }

    public void bulkUpload() {
        List<String> lstLines = Arrays.asList(getBulkText().split("\\r?\\n"));
        for (String s : lstLines) {
            List<String> w = Arrays.asList(s.split(","));
            try {
                String code = w.get(0);
                String ix = w.get(1);
                String ic = w.get(2);
                String f = w.get(4);
                //////// // System.out.println(code + " " + ix + " " + ic + " " + f);

                Investigation tix = new Investigation();
                tix.setCode(code);
                tix.setName(ix);
                tix.setDepartment(null);

            } catch (Exception e) {
            }

        }
    }

    public void setSelectedItems(List<Investigation> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    private boolean errorCheck() {
        if (getCurrent().isUserChangable() && getCurrent().isDiscountAllowed() == true) {
            JsfUtil.addErrorMessage("Cant tick both User can Change & Discount Allowed");
            return true;
        }
        return false;
    }

    public void makeSymanticTypeForAllIx() {
        for (Investigation i : getItems()) {
            i.setSymanticType(SymanticType.Laboratory_Procedure);
            getFacade().edit(i);
        }
        JsfUtil.addSuccessMessage("Updated");
    }

    public void saveSelectedForEhr() {
        getCurrent().setSymanticType(SymanticType.Laboratory_Procedure);
        if (getCurrent().getPrintName() == null || getCurrent().getPrintName().trim().equals("")) {
            getCurrent().setPrintName(getCurrent().getName());
        }
        if (getCurrent().getFullName() == null || getCurrent().getFullName().trim().equals("")) {
            getCurrent().setFullName(getCurrent().getName());
        }
        if (getCurrent().getCode() == null || getCurrent().getCode().trim().equals("")) {
            getCurrent().setCode(getCurrent().getName());
        }

        if (getCurrent().getInwardChargeType() == null) {
            getCurrent().setInwardChargeType(InwardChargeType.Laboratory);
        }
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            if (billedAs == false) {
                getCurrent().setBilledAs(getCurrent());
            }
            if (reportedAs == false) {
                getCurrent().setReportedAs(getCurrent());
            }
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());

            getFacade().create(getCurrent());
            if (billedAs == false) {
                getCurrent().setBilledAs(getCurrent());
            }
            if (reportedAs == false) {
                getCurrent().setReportedAs(getCurrent());
            }
            getFacade().edit(getCurrent());
            Item sc = new Item();
            sc.setCreatedAt(new Date());
            sc.setCreater(sessionController.getLoggedUser());
            sc.setItemType(ItemType.SampleComponent);
            sc.setName(getCurrent().getName());
            sc.setParentItem(getCurrent());
            getItemFacade().create(sc);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void saveSelected() {

        if (getCurrent() == null) {
            return;
        }
        

        getCurrent().setSymanticType(SymanticType.Laboratory_Procedure);
        if (getCurrent().getInwardChargeType() == null) {
            getCurrent().setInwardChargeType(InwardChargeType.Laboratory);
        }
        if (getCurrent().getId() != null) {
            if (billedAs == false) {
                getCurrent().setBilledAs(getCurrent());
            }
            if (reportedAs == false) {
                getCurrent().setReportedAs(getCurrent());
            }
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            if (billedAs == false) {
                getCurrent().setBilledAs(getCurrent());
            }
            if (reportedAs == false) {
                getCurrent().setReportedAs(getCurrent());
            }
            getFacade().edit(getCurrent());
            Item sc = new Item();
            sc.setCreatedAt(new Date());
            sc.setCreater(sessionController.getLoggedUser());
            sc.setItemType(ItemType.SampleComponent);
            sc.setName(getCurrent().getName());
            sc.setParentItem(getCurrent());
            getItemFacade().create(sc);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void save(Investigation i) {
        i.setSymanticType(SymanticType.Laboratory_Procedure);
        if (i.getInwardChargeType() == null) {
            i.setInwardChargeType(InwardChargeType.Laboratory);
        }
        if (i.getId() != null && i.getId() > 0) {
            if (billedAs == false) {
                i.setBilledAs(i);
            }
            if (reportedAs == false) {
                i.setReportedAs(i);
            }
            getFacade().edit(i);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            i.setCreatedAt(new Date());
            i.setCreater(getSessionController().getLoggedUser());
            getFacade().create(i);
            if (billedAs == false) {
                i.setBilledAs(i);
            }
            if (reportedAs == false) {
                i.setReportedAs(i);
            }
            getFacade().edit(i);
            Item sc = new Item();
            sc.setCreatedAt(new Date());
            sc.setCreater(sessionController.getLoggedUser());
            sc.setItemType(ItemType.SampleComponent);
            sc.setName(i.getName());
            sc.setParentItem(i);
            getItemFacade().create(sc);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void saveSelected(Investigation tix) {
        if (tix.getId() == null) {
            getFacade().create(tix);
        } else {
            getFacade().edit(tix);
        }
    }

    public void createInvestigationWithDynamicLables() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        investigationWithInvestigationItemses = new ArrayList<>();
        for (Investigation in : fetchInvestigations(current)) {
            InvestigationWithInvestigationItems items = new InvestigationWithInvestigationItems();
            items.setI(in);
            items.setFlags(fetchFlags(in));
            if (items.getFlags().isEmpty()) {
                continue;
            }
            investigationWithInvestigationItemses.add(items);
        }

        
    }

    public List<InvestigationItemWithInvestigationItemValueFlags> fetchFlags(Investigation i) {
        List<InvestigationItemWithInvestigationItemValueFlags> lisFlags = new ArrayList<>();
        for (InvestigationItem ii : fetchInvestigationItemsOfDynamicLabelType(i)) {
            InvestigationItemWithInvestigationItemValueFlags flags = new InvestigationItemWithInvestigationItemValueFlags();
            flags.setInvestigationItem(ii);
            flags.setFlags(fetchDynamicLabels(ii));
            if (flags.getFlags().isEmpty()) {
                continue;
            }
            lisFlags.add(flags);
        }
        return lisFlags;
    }

    public List<Investigation> fetchInvestigations(Investigation i) {
        List<Investigation> investigations;
        String sql;
        Map m = new HashMap();

        sql = "select c from Investigation c "
                + " where c.retired=false ";

        if (listMasterItemsOnly == true) {
            sql += " and c.institution is null ";
        }
        if (i != null) {
            sql += " and c=:i ";
            m.put("i", i);
        }
        sql += " order by c.name";

        investigations = getFacade().findByJpql(sql, m);
        return investigations;
    }

    public List<InvestigationItem> fetchInvestigationItemsOfDynamicLabelType(Investigation i) {
        List<InvestigationItem> investigationItemsOfDynamicLabelType;
        String sql;
        Map m = new HashMap();
        sql = " select i from InvestigationItem i where i.retired=false "
                + " and i.item=:i "
                + " and i.ixItemType=:ixType ";

        m.put("i", i);
        m.put("ixType", InvestigationItemType.DynamicLabel);

        investigationItemsOfDynamicLabelType = getInvestigationItemFacade().findByJpql(sql, m);
        return investigationItemsOfDynamicLabelType;
    }

    public List<InvestigationItemValueFlag> fetchDynamicLabels(InvestigationItem ii) {
        List<InvestigationItemValueFlag> dynamicLabels;
        String sql;
        Map m = new HashMap();

        sql = "select i from InvestigationItemValueFlag i where i.retired=false and  "
                + " i.investigationItemOfLabelType=:ii ";

        m.put("ii", ii);
        dynamicLabels = getInvestigationItemValueFlagFacade().findByJpql(sql, m);
        return dynamicLabels;
    }

    public List<Investigation> getInvestigationWithSelectedFormat() {
        if (investigationWithSelectedFormat == null) {
            String j = "select i from Investigation i where i.reportFormat=:rf order by i.name";
            Map m = new HashMap();
            m.put("rf", categoryForFormat);
            investigationWithSelectedFormat = getFacade().findByJpql(j, m);
        }
        return investigationWithSelectedFormat;
    }

    public void setInvestigationWithSelectedFormat(List<Investigation> investigationWithSelectedFormat) {
        this.investigationWithSelectedFormat = investigationWithSelectedFormat;
    }

    public Category getCategoryForFormat() {
        return categoryForFormat;
    }

    public void setCategoryForFormat(Category categoryForFormat) {
        this.categoryForFormat = categoryForFormat;
        investigationWithSelectedFormat = null;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public StreamedContent getDownloadingFile() {
        return downloadingFile;
    }

    public void setDownloadingFile(StreamedContent downloadingFile) {
        this.downloadingFile = downloadingFile;
    }

    public int getAdminTabIndex() {
        return adminTabIndex;
    }

    public void setAdminTabIndex(int adminTabIndex) {
        this.adminTabIndex = adminTabIndex;
    }

    public class InvestigationWithInvestigationItems {

        Investigation i;
        List<InvestigationItemWithInvestigationItemValueFlags> flags;

        public Investigation getI() {
            return i;
        }

        public void setI(Investigation i) {
            this.i = i;
        }

        public List<InvestigationItemWithInvestigationItemValueFlags> getFlags() {
            return flags;
        }

        public void setFlags(List<InvestigationItemWithInvestigationItemValueFlags> flags) {
            this.flags = flags;
        }

    }

    public class InvestigationItemWithInvestigationItemValueFlags {

        InvestigationItem investigationItem;
        List<InvestigationItemValueFlag> flags;

        public InvestigationItem getInvestigationItem() {
            return investigationItem;
        }

        public void setInvestigationItem(InvestigationItem investigationItem) {
            this.investigationItem = investigationItem;
        }

        public List<InvestigationItemValueFlag> getFlags() {
            return flags;
        }

        public void setFlags(List<InvestigationItemValueFlag> flags) {
            this.flags = flags;
        }

    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public InvestigationFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(InvestigationFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public InvestigationController() {
    }

    public Investigation getInvestigationByIdAndSetAsCurrent(Long id) {
        setCurrent(getFacade().find(id));
        return current;
    }

    public Investigation getCurrent() {
        if (current == null) {
            current = new Investigation();
        }
        return current;
    }

    public void setCurrent(Investigation current) {
        this.current = current;
        if (current != null) {
            if (current.getBilledAs() == current) {
                billedAs = false;
            } else {
                billedAs = true;
            }
            if (current.getReportedAs() == current) {
                reportedAs = false;
            } else {
                reportedAs = true;
            }
        }
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

    private InvestigationFacade getFacade() {
        return ejbFacade;
    }
    @EJB
    private ItemFeeFacade itemFeeFacade;

    public List<ItemFee> getItemFee() {
        List<ItemFee> temp;
        temp = getItemFeeFacade().findByJpql("select c from ItemFee c where c.retired = false and type(c.item) =Investigation order by c.item.name");

        if (temp == null) {
            return new ArrayList<ItemFee>();
        }

        return temp;
    }

    public List<Investigation> getItems() {
        if (items == null) {
            fillItems();
        }
        return items;
    }

    public void fillItems() {
        String sql = "select i from Investigation i where i.retired=false order by i.name";
        items = getFacade().findByJpql(sql);
    }

    public List<Investigation> fillAllItems() {
        String sql = "select i from Investigation i where i.retired=false order by i.name";
        return getFacade().findByJpql(sql);
    }

    public void createInvestigationWithFees() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        itemWithFees = new ArrayList<>();
        List<Item> temp;
        String sql = "select distinct(c.item) from ItemFee c where c.retired = false "
                + " and type(c.item) =Investigation "
                + " order by c.item.name";
        temp = getItemFacade().findByJpql(sql);
        for (Item item : temp) {
            ItemWithFee iwf = new ItemWithFee();
            iwf.setItem(item);
            sql = "select c from ItemFee c where c.retired = false "
                    + " and type(c.item) =Investigation "
                    + " and c.item.id=" + item.getId() + " order by c.item.name";
            iwf.setItemFees(getItemFeeFacade().findByJpql(sql));
            itemWithFees.add(iwf);
        }

        
    }

    public class ItemWithFee {

        Item item;
        List<ItemFee> itemFees;

        public Item getItem() {
            return item;
        }

        public void setItem(Item item) {
            this.item = item;
        }

        public List<ItemFee> getItemFees() {
            return itemFees;
        }

        public void setItemFees(List<ItemFee> itemFees) {
            this.itemFees = itemFees;
        }
    }

    public List<ItemWithFee> getItemWithFees() {
        return itemWithFees;
    }

    public void setItemWithFees(List<ItemWithFee> itemWithFees) {
        this.itemWithFees = itemWithFees;
    }

    public SpecialityFacade getSpecialityFacade() {
        return specialityFacade;
    }

    public void setSpecialityFacade(SpecialityFacade specialityFacade) {
        this.specialityFacade = specialityFacade;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade itemFeeFacade) {
        this.itemFeeFacade = itemFeeFacade;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public List<Investigation> getAllIxs() {
        if (allIxs == null) {
            allIxs = fillAllItems();
        }
        return allIxs;
    }

    public void setAllIxs(List<Investigation> allIxs) {
        this.allIxs = allIxs;
    }

    public InvestigationItemFacade getInvestigationItemFacade() {
        return investigationItemFacade;
    }

    public void setInvestigationItemFacade(InvestigationItemFacade investigationItemFacade) {
        this.investigationItemFacade = investigationItemFacade;
    }

    public InvestigationItemValueFlagFacade getInvestigationItemValueFlagFacade() {
        return investigationItemValueFlagFacade;
    }

    public void setInvestigationItemValueFlagFacade(InvestigationItemValueFlagFacade investigationItemValueFlagFacade) {
        this.investigationItemValueFlagFacade = investigationItemValueFlagFacade;
    }

    public List<InvestigationWithInvestigationItems> getInvestigationWithInvestigationItemses() {
        return investigationWithInvestigationItemses;
    }

    public void setInvestigationWithInvestigationItemses(List<InvestigationWithInvestigationItems> investigationWithInvestigationItemses) {
        this.investigationWithInvestigationItemses = investigationWithInvestigationItemses;
    }

    /**
     *
     */
    @FacesConverter("ixcon")
    public static class InvestigationConverter implements Converter {

        public InvestigationConverter() {
        }

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            InvestigationController controller = (InvestigationController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "investigationController");
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
            if (object instanceof Investigation) {
                Investigation o = (Investigation) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + InvestigationController.class.getName());
            }
        }
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public InvestigationItemController getInvestigationItemController() {
        return investigationItemController;
    }

    public IxCalController getIxCalController() {
        return ixCalController;
    }

    public ItemFeeManager getItemFeeManager() {
        return itemFeeManager;
    }

    public PatientReportController getPatientReportController() {
        return patientReportController;
    }

    public ItemForItemController getItemForItemController() {
        return itemForItemController;
    }

}
