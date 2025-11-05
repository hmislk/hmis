package com.divudi.bean.common;

import com.divudi.core.data.DepartmentItemCount;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.FeeType;
import com.divudi.core.data.InstitutionItemCount;
import com.divudi.core.data.ItemLight;
import com.divudi.core.data.ItemType;
import com.divudi.core.data.dataStructure.ItemFeeRow;
import com.divudi.core.data.hr.ReportKeyWord;
import com.divudi.core.entity.BillExpense;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.ItemFee;
import com.divudi.core.entity.Packege;
import com.divudi.core.entity.Service;
import com.divudi.core.entity.ServiceCategory;
import com.divudi.core.entity.ServiceSubCategory;
import com.divudi.core.entity.inward.InwardService;
import com.divudi.core.entity.inward.TheatreService;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.lab.ItemForItem;
import com.divudi.core.entity.lab.Machine;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.Atm;
import com.divudi.core.entity.pharmacy.PharmaceuticalItem;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.entity.pharmacy.Vmpp;
import com.divudi.core.entity.pharmacy.Vtm;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.ItemFeeFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.lab.InvestigationController;
import com.divudi.bean.common.SpecialityController;
import com.divudi.bean.hr.StaffController;
import com.divudi.core.data.SessionNumberType;
import com.divudi.core.data.Sex;
import com.divudi.core.entity.UserPreference;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InvestigationFacade;
import com.divudi.core.facade.ItemMappingFacade;
import com.divudi.core.facade.ServiceFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.ItemFeeService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFTableStyleInfo;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.file.UploadedFile;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class ItemController implements Serializable {

    /**
     * EJBs
     */
    private static final long serialVersionUID = 1L;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private InvestigationFacade investigationFacade;
    @EJB
    private ItemFeeFacade itemFeeFacade;
    @EJB
    private ServiceFacade serviceFacade;
    @EJB
    ItemMappingFacade itemMappingFacade;
    @EJB
    DepartmentFacade departmentFacade;
    @EJB
    ItemFeeService itemFeeService;
    /**
     * Managed Beans
     */
    @Inject
    SessionController sessionController;
    @Inject
    ItemMappingController itemMappingController;
    @Inject
    ItemFeeManager itemFeeManager;
    @Inject
    DepartmentController departmentController;
    @Inject
    InstitutionController institutionController;
    @Inject
    ItemForItemController itemForItemController;
    @Inject
    ItemFeeController itemFeeController;
    @Inject
    ServiceController serviceController;
    @Inject
    ItemApplicationController itemApplicationController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    InvestigationController investigationController;
    @Inject
    SpecialityController specialityController;
    @Inject
    StaffController staffController;

    /**
     * Properties
     */
    private Institution site;
    private Institution collectionCentre;
    private Item current;
    private Item sampleComponent;
    private List<Item> items = null;
    private List<Item> investigationsAndServices = null;
    private List<Item> itemlist;
    private List<ItemLight> allItems;
    private List<ItemLight> filteredItems;
    private ItemLight selectedItemLight;
    private List<ItemLight> departmentItems;
    private List<ItemLight> institutionItems;
    private List<ItemLight> ccDeptItems;
    private List<ItemLight> ccInstitutionItems;
    private List<ItemFee> allItemFees;
    private List<Item> selectedList;
    private List<ItemFee> selectedItemFeeList;
    private Institution institution;
    private Department department;
    private Institution filterInstitution;
    private Department filterDepartment;
    private FeeType feeType;
    private List<Department> departments;
    private Machine machine;
    private List<Item> machineTests;
    private List<Item> investigationSampleComponents;
    private List<ItemFee> ItemFeesList;
    private List<ItemFeeRow> itemFeeRows;
    private List<ItemFee> importedFees;
    private Department selectedDepartment;
    private List<DepartmentItemCount> departmentItemCounts;
    private DepartmentItemCount departmentItemCount;
    private List<InstitutionItemCount> institutionItemCounts;
    private InstitutionItemCount institutionItemCount;
    private List<Item> suggestItems;
    boolean masterItem;
    private Sex patientGender;
    private UploadedFile file;
    private Category selectedCategory;

    private ReportKeyWord reportKeyWord;

    private List<Item> packaes;

    private String output;

    private List<String> importFailures;

    public void uploadAddReplaceItemsFromId() {
        items = new ArrayList<>();
        if (file != null) {
            try (InputStream inputStream = file.getInputStream()) {
                items = replaceItemsFromExcel(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void uploadToReplaceSiteFeesByItemCode() {
        if (site == null) {
            JsfUtil.addErrorMessage("Please select a site");
            return;
        }
        allItemFees = new ArrayList<>();
        if (file != null) {
            try (InputStream inputStream = file.getInputStream()) {
                allItemFees = replaceSiteFeesFromItemCodeFromExcel(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void uploadToAddSiteFeesByItemCode() {
        if (site == null) {
            JsfUtil.addErrorMessage("Please select a site");
            return;
        }
        allItemFees = new ArrayList<>();
        if (file != null) {
            try (InputStream inputStream = file.getInputStream()) {
                allItemFees = addForInstitutionItemFeesFromItemCodeFromExcel(inputStream, site);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void uploadToAddCcFeesByItemCode() {
        if (collectionCentre == null) {
            JsfUtil.addErrorMessage("Please select a Collection Centre");
            return;
        }
        allItemFees = new ArrayList<>();
        if (file != null) {
            try (InputStream inputStream = file.getInputStream()) {
                allItemFees = addForInstitutionItemFeesFromItemCodeFromExcel(inputStream, collectionCentre);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void uploadToAddDepartmentFeesByItemCode() {
        importedFees = new ArrayList<>();
        importFailures = new ArrayList<>();
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a Department");
            return;
        }

        if (file != null) {
            try (InputStream inputStream = file.getInputStream()) {
                importedFees = addForDepartmentItemFeesFromItemCodeFromExcel(inputStream, department);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (output != null) {
            String[] lines = output.split("<br/>");
            for (String l : lines) {
                if (l == null || l.trim().isEmpty()) {
                    continue;
                }
                if (!l.contains("Fee ready")) {
                    importFailures.add(l);
                }
            }
        }
    }

    public void saveImportedDepartmentFees() {
        if (importedFees == null || importedFees.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }
        for (ItemFee itf : importedFees) {
            if (itf.getId() == null) {
                itf.setCreatedAt(new Date());
                itf.setCreater(sessionController.getLoggedUser());
                itemFeeFacade.create(itf);
            } else {
                itemFeeFacade.edit(itf);
            }
            itemFeeService.updateFeeValue(itf.getItem(), itf.getForDepartment(), itf.getFee(), itf.getFfee());
        }
        JsfUtil.addSuccessMessage("Imported fees saved");
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    private List<ItemFee> replaceSiteFeesFromItemCodeFromExcel(InputStream inputStream) throws IOException {
        output = "";
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        List<ItemFee> itemFeesSaved = new ArrayList<>();

        // Assuming the first row contains headers, skip it
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }

        int rowNumber = 0;

        while (rowIterator.hasNext()) {
            rowNumber++;
            Row row = rowIterator.next();
            // Ensure item is initialized correctly
            String itemCode = null;
            Long itemCodeLong;

            // Column 0: Item ID
            Cell codeCell = row.getCell(0);
            if (codeCell != null) {
                if (codeCell.getCellType() == CellType.STRING) {
                    itemCode = codeCell.getStringCellValue();
                } else if (codeCell.getCellType() == CellType.NUMERIC) {
                    itemCodeLong = (long) codeCell.getNumericCellValue();
                    itemCode = itemCodeLong.toString();
                }
            }

            List<ItemFee> itemFeesMatchingTheCodeAndSite = itemFeeService.fetchSiteFeesByItem(itemCode, site);

            if (itemFeesMatchingTheCodeAndSite == null) {
                output += rowNumber + " - No Mathing Item Fee for Code " + itemCode + "/n<br/>";
                continue;
            }
            if (itemFeesMatchingTheCodeAndSite.isEmpty()) {
                output += rowNumber + " - No Mathing Item Fee for Code " + itemCode + "/n<br/>";
                continue;
            }
            if (itemFeesMatchingTheCodeAndSite.size() > 1) {
                output += rowNumber + " - More than one Mathing Item Fees for Code " + itemCode + "/n<br/>";
                continue;
            }

            ItemFee itemFee = itemFeesMatchingTheCodeAndSite.get(0);
            Double feeValue = 0.0;

            // Column 0: Item ID
            Cell feeCell = row.getCell(1);
            if (feeCell != null) {
                if (feeCell.getCellType() == CellType.STRING) {
                    feeValue = CommonFunctions.stringToDouble(feeCell.getStringCellValue());
                } else if (feeCell.getCellType() == CellType.NUMERIC) {
                    feeValue = (double) feeCell.getNumericCellValue();
                }
            }
            if (feeValue < 1) {
                output = rowNumber + " - Fee Value is wrong for Item with Code " + itemCode + "/n<br/>";
                continue;
            }

            itemFee.setFee(feeValue);
            itemFee.setFfee(feeValue);
            itemFeeFacade.edit(itemFee);

            itemFeeService.updateFeeValue(itemFee.getItem(), site, feeValue, feeValue);

            output += rowNumber + " - Successfully added Fee for Item with Code " + itemCode + "/n<br/>";

        }

        workbook.close(); // Always close the workbook to prevent memory leaks
        return itemFeesSaved;
    }

    private List<ItemFee> addForInstitutionItemFeesFromItemCodeFromExcel(InputStream inputStream, Institution fromInstitution) throws IOException {
        output = "";

        if (fromInstitution == null) {
            output = "❌ From Institution is not selected. Please select before proceeding.<br/>";
            return Collections.emptyList();
        }

        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        List<ItemFee> itemFeesSaved = new ArrayList<>();

        if (rowIterator.hasNext()) {
            rowIterator.next(); // Skip header
        }

        int rowNumber = 0;

        while (rowIterator.hasNext()) {
            rowNumber++;
            Row row = rowIterator.next();

            String itemCode = null;

            Cell codeCell = row.getCell(0);
            if (codeCell != null) {
                if (codeCell.getCellType() == CellType.STRING) {
                    itemCode = codeCell.getStringCellValue();
                } else if (codeCell.getCellType() == CellType.NUMERIC) {
                    itemCode = String.valueOf((long) codeCell.getNumericCellValue());
                }
            }

            if (itemCode == null || itemCode.trim().isEmpty()) {
                output += rowNumber + " - Missing Item Code.<br/>";
                continue;
            }

            Item item = findItemByCode(itemCode);
            if (item == null) {
                output += rowNumber + " - No matching item found for Code " + itemCode + "<br/>";
                continue;
            }

            Double feeValue = 0.0;
            Cell feeCell = row.getCell(1);
            if (feeCell != null) {
                if (feeCell.getCellType() == CellType.STRING) {
                    feeValue = CommonFunctions.stringToDouble(feeCell.getStringCellValue());
                } else if (feeCell.getCellType() == CellType.NUMERIC) {
                    feeValue = feeCell.getNumericCellValue();
                }
            }

            if (feeValue < 1) {
                output += rowNumber + " - Invalid Fee Value for Code " + itemCode + "<br/>";
                continue;
            }

            ItemFee itf = new ItemFee();
            itf.setName("Hospital Fee");
            itf.setItem(item);
            itf.setInstitution(sessionController.getInstitution());
            itf.setDepartment(sessionController.getDepartment());
            itf.setFeeType(FeeType.OwnInstitution);
            itf.setFee(feeValue);
            itf.setFfee(feeValue);
            itf.setCreatedAt(new Date());
            itf.setCreater(sessionController.getLoggedUser());
            itf.setForInstitution(fromInstitution);

            itemFeeFacade.create(itf);
            itemFeeService.updateFeeValue(item, site, feeValue, feeValue);

            itemFeesSaved.add(itf);
            output += rowNumber + " - Fee added successfully for Code " + itemCode + "<br/>";
        }

        workbook.close();
        return itemFeesSaved;
    }

    private List<ItemFee> addForDepartmentItemFeesFromItemCodeFromExcel(InputStream inputStream, Department fromDepartment) throws IOException {
        output = "";

        if (fromDepartment == null) {
            output = "❌ From Department is not selected. Please select before proceeding.<br/>";
            return Collections.emptyList();
        }

        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        List<ItemFee> itemFeesSaved = new ArrayList<>();

        if (rowIterator.hasNext()) {
            rowIterator.next();
        }

        int rowNumber = 0;

        while (rowIterator.hasNext()) {
            rowNumber++;
            Row row = rowIterator.next();

            String itemCode = null;

            Cell codeCell = row.getCell(0);
            if (codeCell != null) {
                if (codeCell.getCellType() == CellType.STRING) {
                    itemCode = codeCell.getStringCellValue();
                } else if (codeCell.getCellType() == CellType.NUMERIC) {
                    itemCode = String.valueOf((long) codeCell.getNumericCellValue());
                }
            }

            if (itemCode == null || itemCode.trim().isEmpty()) {
                output += rowNumber + " - Missing Item Code.<br/>";
                continue;
            }

            Item item = findItemByCode(itemCode);
            if (item == null) {
                output += rowNumber + " - No matching item found for Code " + itemCode + "<br/>";
                continue;
            }

            Double feeValue = 0.0;
            Cell feeCell = row.getCell(1);
            if (feeCell != null) {
                if (feeCell.getCellType() == CellType.STRING) {
                    feeValue = CommonFunctions.stringToDouble(feeCell.getStringCellValue());
                } else if (feeCell.getCellType() == CellType.NUMERIC) {
                    feeValue = feeCell.getNumericCellValue();
                }
            }

            if (feeValue < 1) {
                output += rowNumber + " - Invalid Fee Value for Code " + itemCode + "<br/>";
                continue;
            }

            ItemFee itf = new ItemFee();
            itf.setName("Hospital Fee");
            itf.setItem(item);
            itf.setInstitution(sessionController.getInstitution());
            itf.setDepartment(sessionController.getDepartment());
            itf.setFeeType(FeeType.OwnInstitution);
            itf.setFee(feeValue);
            itf.setFfee(feeValue);
            itf.setCreatedAt(new Date());
            itf.setCreater(sessionController.getLoggedUser());
            itf.setForDepartment(fromDepartment);

            itemFeesSaved.add(itf);
            output += rowNumber + " - Fee ready to save for Code " + itemCode + "<br/>";
        }

        workbook.close();
        return itemFeesSaved;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case FORMULA:
                Workbook wb = cell.getSheet().getWorkbook();
                FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
                CellValue cellValue = evaluator.evaluate(cell);
                return cellValue.getCellType() == CellType.NUMERIC ? String.valueOf(cellValue.getNumberValue()) : "";
            default:
                return "";
        }
    }

    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                return CommonFunctions.stringToDouble(cell.getStringCellValue());
            case FORMULA:
                Workbook wb = cell.getSheet().getWorkbook();
                FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
                CellValue cellValue = evaluator.evaluate(cell);
                return cellValue.getCellType() == CellType.NUMERIC ? cellValue.getNumberValue() : null;
            default:
                return null;
        }
    }

    private List<Item> replaceItemsFromExcel(InputStream inputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        List<Item> itemsToSave = new ArrayList<>();

        // Assuming the first row contains headers, skip it
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Item item = null;  // Ensure item is initialized correctly
            String itemIdString = null;
            Long itemIdLong = null;
            String itemName = null;
            boolean retired = false;

            // Column 0: Item ID
            Cell idCell = row.getCell(0);
            if (idCell != null) {
                if (idCell.getCellType() == CellType.STRING) {
                    itemIdString = idCell.getStringCellValue();
                    item = findItemById(itemIdString);
                } else if (idCell.getCellType() == CellType.NUMERIC) {
                    itemIdLong = (long) idCell.getNumericCellValue();
                    item = findItemById(itemIdLong);
                }
            }

            if (item == null) {
                continue; // Skip if item not found
            }

            // Column 3: Item Name
            Cell itemNameCell = row.getCell(3);
            if (itemNameCell != null && itemNameCell.getCellType() == CellType.STRING) {
                itemName = itemNameCell.getStringCellValue();
            }

            // Column 6: Retired (Yes/No)
            Cell retiredCell = row.getCell(6);
            if (retiredCell != null && retiredCell.getCellType() == CellType.STRING) {
                String retiredString = retiredCell.getStringCellValue();
                retired = retiredString.equalsIgnoreCase("Yes");
            }

            // Update item details
            item.setName(itemName);
            item.setRetired(retired);
            if (retired) {
                item.setRetiredAt(new Date());
                item.setRetirer(sessionController.getLoggedUser());
            }

            // Save the item
            itemsToSave.add(item);
            itemFacade.edit(item);
        }

        workbook.close(); // Always close the workbook to prevent memory leaks
        return itemsToSave;
    }

    public void downloadItems() throws IOException {
        // Check if items is null or empty
        if (items == null || items.isEmpty()) {
            JsfUtil.addErrorMessage("Please fill item fees first to download them.");
            return;
        }

        // Create a workbook and a sheet
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Base Item Fees");

        // Create the header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Item Code", "Item Type", "Item Name", "Category", "Financial Category", "Retired", "Institution", "Department"};

        // Apply header formatting
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < headers.length; i++) {
            Cell headerCell = headerRow.createCell(i);
            headerCell.setCellValue(headers[i]);
            headerCell.setCellStyle(headerStyle);
        }

        // Fill the data into the sheet
        int rowNum = 1;
        for (Item tmpItem : items) {
            if (tmpItem == null) {
                continue; // Skip null entries
            }

            Row row = sheet.createRow(rowNum++);

            // Create locked cells for ID and Item Code with null checks
            createLockedCell(row, 0, tmpItem.getId() != null ? tmpItem.getId().toString() : "N/A", false, workbook); // Locked ID
            createLockedCell(row, 1, tmpItem.getCode() != null ? tmpItem.getCode() : "N/A", false, workbook); // Locked Item Code

            // Create unlocked cells with null checks for other fields
            createUnlockedCell(row, 2, tmpItem.getItemType() != null ? tmpItem.getItemType().toString() : "N/A", workbook); // Unlocked Item Type
            createUnlockedCell(row, 3, tmpItem.getName() != null ? tmpItem.getName() : "N/A", workbook); // Unlocked Item Name

            //New Fields to add
            tmpItem.getPrintName();
            tmpItem.getFullName();
            tmpItem.getShortName();

            createUnlockedCell(row, 4, tmpItem.getCategory() != null && tmpItem.getCategory().getName() != null ? tmpItem.getCategory().getName() : "N/A", workbook); // Unlocked Category
            createUnlockedCell(row, 5, tmpItem.getFinancialCategory() != null && tmpItem.getFinancialCategory().getName() != null ? tmpItem.getFinancialCategory().getName() : "N/A", workbook); // Unlocked Financial Category
            createUnlockedCell(row, 6, tmpItem.isRetired() ? "Yes" : "No", workbook); // Unlocked Retired

            // Adding Institution and Department fields with null checks
            createUnlockedCell(row, 7, tmpItem.getInstitution() != null && tmpItem.getInstitution().getName() != null ? tmpItem.getInstitution().getName() : "N/A", workbook); // Unlocked Institution
            createUnlockedCell(row, 8, tmpItem.getDepartment() != null && tmpItem.getDepartment().getName() != null ? tmpItem.getDepartment().getName() : "N/A", workbook); // Unlocked Department
        }

        // Apply a table format, ensuring there are enough rows and columns for the table
        if (rowNum > 1) { // Ensure there are rows beyond the header
            AreaReference area = new AreaReference("A1:I" + rowNum, SpreadsheetVersion.EXCEL2007); // Adjusted to 9 columns
            XSSFTable table = sheet.createTable(area);
            table.setName("ItemTable");
            table.setDisplayName("Item Table");

            // Set the table style
            XSSFTableStyleInfo style = (XSSFTableStyleInfo) table.getStyle();
            if (style != null) {
                style.setName("TableStyleMedium9");
                style.setShowColumnStripes(true);
                style.setShowRowStripes(true);
            } else {
            }
        } else {
        }

        // Lock the sheet except for the unlocked cells
        sheet.protectSheet("password"); // Replace with your desired password

        // Write the output to the response
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"item_list.xlsx\"");
        OutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        workbook.close();
        outputStream.flush();
        outputStream.close();

        facesContext.responseComplete();
    }

    // Method to create locked cells
    private void createLockedCell(Row row, int column, Object value, boolean unlock, XSSFWorkbook workbook) {
        Cell cell = row.createCell(column);
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        }
        CellStyle style = workbook.createCellStyle();
        style.setLocked(!unlock); // Lock unless specified to unlock
        cell.setCellStyle(style);
    }

// Method to create unlocked cells
    private void createUnlockedCell(Row row, int column, Object value, XSSFWorkbook workbook) {
        createLockedCell(row, column, value, true, workbook); // Call with unlock true
    }

    public void processDepartmentItemCount() {
        // Query for count of items without a department
        String jpqlWithoutDept = "select count(i) "
                + "from Item i "
                + "where i.retired=:ret "
                + "and i.department is null "
                + "and (TYPE(i)=:ix or TYPE(i)=:sv)";

        // Query for items with departments
        String jpqlWithDept = "select new com.divudi.core.data.DepartmentItemCount("
                + "i.department.id, i.department.name, count(i)) "
                + "from Item i "
                + "where i.retired=:ret "
                + "and i.department is not null "
                + "and (TYPE(i)=:ix or TYPE(i)=:sv) "
                + "group by i.department.id, i.department.name "
                + "order by i.department.name";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("ix", Investigation.class);
        m.put("sv", Service.class);

        // Get count of items without a department
        Long countWithoutDepartment = itemFacade.countByJpql(jpqlWithoutDept, m);
        DepartmentItemCount icWithoutDept = new DepartmentItemCount(-1L, "No Department", countWithoutDepartment);

        // Get list of items with a department
        List<DepartmentItemCount> withDeptList = (List<DepartmentItemCount>) itemFacade.findLightsByJpql(jpqlWithDept, m);

        // Create final list and add count for items without a department first
        departmentItemCounts = new ArrayList<>();
        departmentItemCounts.add(icWithoutDept);
        departmentItemCounts.addAll(withDeptList);
    }

    public void processInstitutionItemCount() {
        // Query for count of items without an institution
        String jpqlWithoutIns = "select count(i) "
                + "from Item i "
                + "where i.retired=:ret "
                + "and i.institution is null "
                + "and (TYPE(i)=:ix or TYPE(i)=:sv)";

        // Query for items with institutions
        String jpqlWithIns = "select new com.divudi.core.data.InstitutionItemCount("
                + "i.institution.id, i.institution.name, i.institution.code, count(i)) "
                + "from Item i "
                + "where i.retired=:ret "
                + "and i.institution is not null "
                + "and (TYPE(i)=:ix or TYPE(i)=:sv) "
                + "group by i.institution.id, i.institution.name, i.institution.code "
                + "order by i.institution.name";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("ix", Investigation.class);
        m.put("sv", Service.class);

        // Get count of items without an institution
        Long countWithoutInstitution = itemFacade.countByJpql(jpqlWithoutIns, m);
        InstitutionItemCount icWithout = new InstitutionItemCount(-1L, "No Institution", "No Code", countWithoutInstitution);

        // Get list of items with an institution
        List<InstitutionItemCount> withInsList = (List<InstitutionItemCount>) itemFacade.findLightsByJpql(jpqlWithIns, m);

        // Create final list and add count for items without an institution first
        institutionItemCounts = new ArrayList<>();
        institutionItemCounts.add(icWithout);
        institutionItemCounts.addAll(withInsList);
    }

    public List<ItemFee> fetchItemFeeList() {
        List<ItemFee> itemFees = new ArrayList<>();
        String sql;
        sql = "select c from ItemFee c "
                + " where c.retired=false order by c.name ";
        ItemFeesList = getItemFeeFacade().findByJpql(sql);
        return ItemFeesList;
    }

    private List<ItemFee> fetchItemFeesForItem(Item item) {
        String sql = "select c from ItemFee c where c.item.id = :itemId and c.retired=false";
        Map<String, Object> params = new HashMap<>();
        params.put("itemId", item.getId());
        return getItemFeeFacade().findByJpql(sql, params);
    }

    public void fillFilteredItems() {
        filteredItems = fillItems();
    }

    public String fillInstitutionItems() {
        if (filterInstitution == null) {
            JsfUtil.addErrorMessage("Select institution");
            return null;
        }
        return fillInstitutionAndDepartmentItems(filterInstitution, null);
    }

    public String fillDepartmentItems() {
        if (filterInstitution == null) {
            JsfUtil.addErrorMessage("Select institution");
            return null;
        }
        if (filterDepartment == null) {
            JsfUtil.addErrorMessage("Select Department");
            return null;
        }
        return fillInstitutionAndDepartmentItems(filterInstitution, filterDepartment);
    }

    public String fillInstitutionAndDepartmentItems(Institution institution, Department department) {
        if (configOptionApplicationController.getBooleanValueByKey("List OPD Items with Item Fees in Manage Items", false)) {
            Map<String, Object> parameters = new HashMap<>();
            String jpql = "SELECT new com.divudi.core.data.ItemLight("
                    + "f.item.id, "
                    + "f.item.name, "
                    + "f.item.code, "
                    + "f.item.total, "
                    + "f.name, "
                    + "f.fee, "
                    + "f.ffee) "
                    + "FROM Fee f "
                    + "WHERE f.retired = :ret "
                    + "AND (TYPE(f.item)=:ixc OR TYPE(f.item)=:svc) ";

            parameters.put("ret", false);
            parameters.put("ixc", Investigation.class);
            parameters.put("svc", Service.class);
            if (filterInstitution != null) {
                jpql += "AND f.item.institution=:ins ";
                parameters.put("ins", institution);
            }
            if (filterDepartment != null) {
                jpql += "AND f.item.department=:dept ";
                parameters.put("dept", department);
            }

            jpql += "ORDER BY f.item.name";
            filteredItems = (List<ItemLight>) itemFacade.findLightsByJpql(jpql, parameters);
        } else {
            Map<String, Object> parameters = new HashMap<>();
            String jpql = "SELECT new com.divudi.core.data.ItemLight("
                    + "i.id, "
                    + "i.name, "
                    + "i.code, "
                    + "i.total) "
                    + "FROM Item i "
                    + "WHERE i.retired = :ret "
                    + "AND (TYPE(i)=:ixc OR TYPE(i)=:svc) ";

            parameters.put("ret", false);
            parameters.put("ixc", Investigation.class);
            parameters.put("svc", Service.class);
            if (filterInstitution != null) {
                jpql += "AND i.institution=:ins ";
                parameters.put("ins", institution);
            }
            if (filterDepartment != null) {
                jpql += "AND i.department=:dept ";
                parameters.put("dept", department);
            }
            jpql += "ORDER BY i.name";
            filteredItems = (List<ItemLight>) itemFacade.findLightsByJpql(jpql, parameters);
        }

        return "/admin/items/list?faces-redirect=true";
    }

    public String fillItemsWithoutInstitution() {
        if (configOptionApplicationController.getBooleanValueByKey("List OPD Items with Item Fees in Manage Items", false)) {
            Map<String, Object> parameters = new HashMap<>();
            String jpql = "SELECT new com.divudi.core.data.ItemLight("
                    + "f.item.id, "
                    + "f.item.name, "
                    + "f.item.code, "
                    + "f.item.total, "
                    + "f.name, "
                    + "f.fee, "
                    + "f.ffee) "
                    + "FROM Fee f "
                    + "WHERE f.retired = :ret "
                    + "AND (f.item.institution IS NULL) "
                    + "AND (TYPE(f.item)=:ixc OR TYPE(f.item)=:svc) ";

            parameters.put("ret", false);
            parameters.put("ixc", Investigation.class);
            parameters.put("svc", Service.class);

            jpql += "ORDER BY f.item.name";
            filteredItems = (List<ItemLight>) itemFacade.findLightsByJpql(jpql, parameters);
        } else {
            Map<String, Object> parameters = new HashMap<>();
            String jpql = "SELECT new com.divudi.core.data.ItemLight("
                    + "i.id, "
                    + "i.name, "
                    + "i.code, "
                    + "i.total) "
                    + "FROM Item i "
                    + "WHERE i.retired = :ret "
                    + "AND i.institution IS NULL "
                    + "AND (TYPE(i)=:ixc OR TYPE(i)=:svc) "
                    + "ORDER BY i.name";

            parameters.put("ret", false);
            parameters.put("ixc", Investigation.class);
            parameters.put("svc", Service.class);
            filteredItems = (List<ItemLight>) itemFacade.findLightsByJpql(jpql, parameters);
        }

        return "/admin/items/list?faces-redirect=true";
    }

    public String fillItemsWithoutDepartment() {
        if (configOptionApplicationController.getBooleanValueByKey("List OPD Items with Item Fees in Manage Items", false)) {
            Map<String, Object> parameters = new HashMap<>();
            String jpql = "SELECT new com.divudi.core.data.ItemLight("
                    + "f.item.id, "
                    + "f.item.name, "
                    + "f.item.code, "
                    + "f.item.total, "
                    + "f.name, "
                    + "f.fee, "
                    + "f.ffee) "
                    + "FROM Fee f "
                    + "WHERE f.retired = :ret "
                    + "AND (f.item.department IS NULL) "
                    + "AND (TYPE(f.item) = :ixc OR TYPE(f.item) = :svc) "
                    + "ORDER BY f.item.name";

            parameters.put("ret", false);
            parameters.put("ixc", Investigation.class);
            parameters.put("svc", Service.class);

            filteredItems = (List<ItemLight>) itemFacade.findLightsByJpql(jpql, parameters);

        } else {
            Map<String, Object> parameters = new HashMap<>();
            String jpql = "SELECT new com.divudi.core.data.ItemLight("
                    + "i.id, "
                    + "i.name, "
                    + "i.code, "
                    + "i.total) "
                    + "FROM Item i "
                    + "WHERE i.retired = :ret "
                    + "AND i.department IS NULL "
                    + "AND (TYPE(i)=:ixc OR TYPE(i)=:svc) "
                    + "ORDER BY i.name";

            parameters.put("ret", false);
            parameters.put("ixc", Investigation.class);
            parameters.put("svc", Service.class);
            filteredItems = (List<ItemLight>) itemFacade.findLightsByJpql(jpql, parameters);
        }

        return "/admin/items/list?faces-redirect=true";
    }

    public String navigateToListItemsOfSelectedDepartment() {
        if (departmentItemCount == null) {
            JsfUtil.addErrorMessage("Select dept");
            return "";
        }
        filterInstitution = null;
        if (departmentItemCount.getDepartmentId() == null) {
            filterDepartment = null;
            return fillItemsWithoutDepartment();
        } else {
            filterDepartment = departmentController.findDepartment(departmentItemCount.getDepartmentId());
            return fillDepartmentItems();
        }
    }

    public String navigateToListItemsOfSelectedInstitution() {
        if (institutionItemCount == null) {
            JsfUtil.addErrorMessage("Select dept");
            return "";
        }
        filterDepartment = null;
        if (institutionItemCount.getInstitutionId() == null || institutionItemCount.getInstitutionId() < 1l) {
            filterInstitution = null;
            return fillItemsWithoutInstitution();
        } else {
            filterInstitution = institutionController.findInstitution(institutionItemCount.getInstitutionId());
            return fillInstitutionItems();
        }
    }

    public List<ItemLight> fillItems() {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.core.data.ItemLight("
                + "i.id, i.orderNo, i.isMasterItem, i.hasReportFormat, "
                + "c.name, c.id, ins.name, ins.id, "
                + "d.name, d.id, s.name, s.id, "
                + "p.name, stf.id, i.name, i.code, i.barcode, "
                + "i.printName, i.shortName, i.fullName, i.total) "
                + "FROM Item i "
                + "LEFT JOIN i.category c "
                + "LEFT JOIN i.institution ins "
                + "LEFT JOIN i.department d "
                + "LEFT JOIN i.speciality s "
                + "LEFT JOIN i.staff stf "
                + "LEFT JOIN stf.person p "
                + "WHERE i.retired = :ret "
                + "AND (TYPE(i)=:ixc OR TYPE(i)=:svc) ";

        if (filterInstitution != null) {
            jpql += "AND i.institution=:ins ";
            parameters.put("ins", filterInstitution);
        } else {
            jpql += "AND i.institution IS NULL ";
        }

        if (filterDepartment != null) {
            jpql += "AND i.department=:dep ";
            parameters.put("dep", filterDepartment);
        } else {
            jpql += "AND i.department IS NULL ";
        }

        parameters.put("ret", false);
        parameters.put("ixc", Investigation.class);
        parameters.put("svc", Service.class);

        jpql += "ORDER BY i.name";

        List<ItemLight> lst = (List<ItemLight>) itemFacade.findLightsByJpql(jpql, parameters);
        return lst;
    }

    public String navigateToListAllItems() {
        allItems = null;
        return "/item/reports/item_list?faces-redirect=true";
    }

    public String navigateToEditFeaturesOfMultipleItems() {
        return "/admin/items/multiple_item_edit?faces-redirect=true";
    }

    public String navigateToListFilteredItems() {
        filteredItems = null;
        return "/admin/items/list?faces-redirect=true";
    }

    public String navigateToListAllItemsForAdmin() {
        allItems = null;
        filterDepartment = null;
        filterInstitution = null;
        return "/admin/items/list?faces-redirect=true";
    }

    public void fillInvestigations() {
        fillItemsByType(Investigation.class);
    }

    public void fillServices() {
        fillItemsByType(Service.class);
    }

    public void fillMedicines() {
        fillItemsByType(PharmaceuticalItem.class);
    }

    public void fillItemsByType(Class it) {
        String jpql = "select i "
                + " from Item i"
                + " where type(i)=:scs "
                + " order by i.name";
        Map m = new HashMap();
        m.put("scs", it);
        allItems = getFacade().findByJpql(jpql, m);
    }

    public String toManageItemIndex() {
        return "/admin/items/index?faces-redirect=true";
    }

    public String toListInvestigations() {
        fillInvestigations();
        return "/admin/investigations?faces-redirect=true";
    }

    public String toAddNewInvestigation() {
        current = new Investigation();
        return "/admin/investigation?faces-redirect=true";
    }

    public String toEditInvestigation() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        return "/admin/institution?faces-redirect=true";
    }

    public String deleteInvestigation() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        current.setRetired(true);
        getFacade().edit(current);
        return toListInvestigations();
    }

    public String saveSelectedInvestigation() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        if (current.getId() == null) {
            getFacade().create(current);
        } else {
            getFacade().edit(current);
        }
        return toListInvestigations();
    }

    public Item findItemByCode(String code) {
        String jpql;
        Map m = new HashMap();
        jpql = "select i "
                + " from Item i "
                + " where i.retired=:ret "
                + " and i.code=:code";
        m.put("ret", false);
        m.put("code", code);
        Item item = getFacade().findFirstByJpql(jpql, m);
        return item;
    }

    public List<Item> findItemsByCode(String code) {
        String jpql;
        Map m = new HashMap();
        jpql = "select i "
                + " from Item i "
                + " where i.retired=:ret "
                + " and i.code=:code";
        m.put("ret", false);
        m.put("code", code);
        List<Item> items = getFacade().findByJpql(jpql, m);
        return items;
    }

    public Item findItem(Long id) {
        return getFacade().find(id);
    }

    public Item findItemByCode(String code, String parentCode) {
        Item parentItem = findItemByCode(parentCode);
        String jpql;
        Map m = new HashMap();
        jpql = "select i "
                + " from Item i "
                + " where i.retired=:ret "
                + " and i.parentItem=:pi "
                + " and i.code=:code";
        m.put("ret", false);
        m.put("code", code);
        m.put("pi", parentItem);
        Item item = getFacade().findFirstByJpql(jpql, m);
        if (item == null) {
            jpql = "select i "
                    + " from Item i "
                    + " and i.parentItem=:pi "
                    + " where i.code=:code";
            m = new HashMap();
            m.put("code", code);
            m.put("pi", parentItem);
            item = getFacade().findFirstByJpql(jpql, m);
            if (item != null) {
                item.setRetired(false);
                getFacade().edit(item);
            } else {
                item = new Item();
                item.setName(code);
                item.setCode(code);
                item.setParentItem(parentItem);
                getFacade().create(item);
            }
        }
        return item;
    }

    public Item findItemByName(String name, String parentCode) {
        Item parentItem = findItemByCode(parentCode);
        String jpql;
        Map m = new HashMap();
        jpql = "select i "
                + " from Item i "
                + " where i.retired=:ret "
                + " and i.parentItem=:pi "
                + " and i.name=:name";
        m.put("ret", false);
        m.put("name", name);
        m.put("pi", parentItem);
        Item item = getFacade().findFirstByJpql(jpql, m);
        if (item == null) {
            jpql = "select i "
                    + " from Item i "
                    + " where i.parentItem=:pi "
                    + " and i.name=:name";
            m = new HashMap();
            m.put("name", name);
            m.put("pi", parentItem);
            item = getFacade().findFirstByJpql(jpql, m);
            if (item != null) {
                item.setRetired(false);
                getFacade().edit(item);
            } else {
                item = new Item();
                item.setName(name);
                item.setCode(CommonFunctions.nameToCode(name));
                item.setParentItem(parentItem);
                getFacade().create(item);
            }
        }
        return item;
    }

    public Item findItemByName(String name, Department dept) {
        try {
            String jpql;
            Map m = new HashMap();
            jpql = "select i "
                    + " from Item i "
                    + " where i.retired=:ret "
                    + " and i.department=:dept "
                    + " and i.name=:name";
            m.put("ret", false);
            m.put("name", name);
            m.put("dept", dept);
            Item item = getFacade().findFirstByJpql(jpql, m);
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    public Item findAndCreateItemByName(String name, Department dept) {
        try {
            String jpql;
            Map m = new HashMap();
            jpql = "select i "
                    + " from Item i "
                    + " where i.retired=:ret "
                    + " and i.department=:dept "
                    + " and i.name=:name";
            m.put("ret", false);
            m.put("name", name);
            m.put("dept", dept);
            Item item = getFacade().findFirstByJpql(jpql, m);
            if (item == null) {
                item = new Item();
                item.setName(name);
                getFacade().create(item);
            }
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    public Investigation findAndCreateInvestigationByNameAndCode(String name, String code) {
        try {
            String jpql;
            Map m = new HashMap();
            jpql = "select i "
                    + " from Investigation i "
                    + " where i.retired=:ret "
                    + " and i.code=:code "
                    + " and i.name=:name";
            m.put("ret", false);
            m.put("name", name);
            m.put("code", code);
            Investigation item = investigationFacade.findFirstByJpql(jpql, m);
            if (item == null) {
                item = new Investigation();
                item.setName(name);
                item.setCode(code);
                getFacade().create(item);
            }
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    public Service findAndCreateServiceByNameAndCode(String name, String code) {
        try {
            String jpql;
            Map m = new HashMap();
            jpql = "select i "
                    + " from Service i "
                    + " where i.retired=:ret "
                    + " and i.code=:code "
                    + " and i.name=:name";
            m.put("ret", false);
            m.put("name", name);
            m.put("code", code);
            Service item = serviceFacade.findFirstByJpql(jpql, m);
            if (item == null) {
                item = new Service();
                item.setName(name);
                item.setCode(code);
                getFacade().create(item);
            }
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    public Item findItemByName(String name, String code, Department dept) {
        try {
            String jpql;
            Map m = new HashMap();
            jpql = "select i "
                    + " from Item i "
                    + " where i.retired=:ret "
                    + " and i.department=:dept "
                    + " and i.code=:code "
                    + " and i.name=:name ";
            m.put("ret", false);
            m.put("name", name);
            m.put("code", code);
            m.put("dept", dept);
            Item item = getFacade().findFirstByJpql(jpql, m);
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    public Item findItemByNameAndCode(String name, String code) {
        try {
            String jpql;
            Map m = new HashMap();
            jpql = "select i "
                    + " from Item i "
                    + " where i.retired=:ret "
                    + " and i.code=:code "
                    + " and i.name=:name ";
            m.put("ret", false);
            m.put("name", name);
            m.put("code", code);
            Item item = getFacade().findFirstByJpql(jpql, m);
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    public Item findMasterItemByName(String name) {
        try {
            String jpql;
            Map m = new HashMap();
            jpql = "select i "
                    + " from Item i "
                    + " where i.retired=:ret "
                    + " and i.isMasterItem=:mi "
                    + " and i.name=:name";
            m.put("ret", false);
            m.put("name", name);
            m.put("mi", true);
            return getFacade().findFirstByJpql(jpql, m);
        } catch (Exception e) {
            return null;
        }
    }

    public void fillInvestigationSampleComponents() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select an investigation");
            return;
        }
        if (current instanceof Investigation) {
            investigationSampleComponents = findInvestigationSampleComponents((Investigation) current);
            if (investigationSampleComponents != null && investigationSampleComponents.size() > 1) {
                current.setHasMoreThanOneComponant(true);
                getFacade().edit(current);
            }
        } else {
            investigationSampleComponents = null;
        }
    }

    public List<Item> getInvestigationSampleComponents(Item ix) {
        if (ix == null) {
            JsfUtil.addErrorMessage("Select an investigation");
            return null;
        }
        if (ix instanceof Investigation) {
            return findInvestigationSampleComponents((Investigation) ix);
        }
        return null;
    }

    public Item getFirstInvestigationSampleComponents(Item ix) {
        if (ix == null) {
            JsfUtil.addErrorMessage("Select an investigation");
            return null;
        }
        if (ix instanceof Investigation) {
            List<Item> is = findInvestigationSampleComponents((Investigation) ix);
            if (is != null && !is.isEmpty()) {
                return is.get(0);
            } else {
                Item sc = new Item();
                sc.setParentItem(ix);
                sc.setItemType(ItemType.SampleComponent);
                sc.setCreatedAt(new Date());
                sc.setCreater(sessionController.getLoggedUser());
                sc.setName(ix.getName());
                getFacade().create(sc);
                return sc;
            }
        }
        return null;
    }

    public List<Item> findInvestigationSampleComponents(Investigation ix) {
        if (ix == null) {
            JsfUtil.addErrorMessage("Select an investigation");
            return null;
        }
        String j = "select i from Item i where i.itemType=:t and i.parentItem=:m and i.retired=:r order by i.name";
        Map m = new HashMap();
        m.put("t", ItemType.SampleComponent);
        m.put("r", false);
        m.put("m", ix);
        return getFacade().findByJpql(j, m);
    }

    public void removeSampleComponent() {
        if (sampleComponent == null) {
            JsfUtil.addErrorMessage("Select one to delete");
            return;
        }
        sampleComponent.setRetired(true);
        sampleComponent.setRetirer(sessionController.getLoggedUser());
        sampleComponent.setRetiredAt(new Date());
        getFacade().edit(sampleComponent);
        fillInvestigationSampleComponents();
        JsfUtil.addSuccessMessage("Removed");
    }

    public void toCreateSampleComponent() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select an investigation");
            return;
        }
        sampleComponent = new Item();
        sampleComponent.setParentItem(current);
        sampleComponent.setItemType(ItemType.SampleComponent);
        sampleComponent.setCreatedAt(new Date());
        sampleComponent.setCreater(sessionController.getLoggedUser());
    }

    public void createOrUpdateSampleComponent() {
        if (sampleComponent == null) {
            JsfUtil.addErrorMessage("Select one to delete");
            return;
        }
        sampleComponent.setParentItem(current);
        sampleComponent.setItemType(ItemType.SampleComponent);

        if (sampleComponent.getId() == null) {
            getFacade().create(sampleComponent);
            JsfUtil.addSuccessMessage("Added");
        } else {
            getFacade().edit(sampleComponent);
            JsfUtil.addSuccessMessage("Updated");
        }
        fillInvestigationSampleComponents();
    }

    public void addSampleComponentsForAllInvestigationsWithoutSampleComponents() {
        String j = "select ix from Investigation ix ";
        List<Item> ixs = getFacade().findByJpql(j);
        for (Item ix : ixs) {
            if (ix instanceof Investigation) {
                Investigation tix = (Investigation) ix;
                List<Item> scs = findInvestigationSampleComponents(tix);
                if (scs == null || scs.isEmpty()) {
                    sampleComponent = new Item();
                    sampleComponent.setName(tix.getName());
                    sampleComponent.setParentItem(tix);
                    sampleComponent.setItemType(ItemType.SampleComponent);
                    sampleComponent.setCreatedAt(new Date());
                    sampleComponent.setCreater(sessionController.getLoggedUser());
                    getFacade().create(sampleComponent);
                } else {
                    if (scs.size() > 1) {
                        tix.setHasMoreThanOneComponant(true);
                        getFacade().edit(tix);
                    } else {
                        tix.setHasMoreThanOneComponant(false);
                        getFacade().edit(tix);
                    }
                }
            }
        }
        JsfUtil.addSuccessMessage("Added");
    }

    public Item addSampleComponent(Investigation tix) {
        Item tmpSampleComponent = null;
        List<Item> scs = findInvestigationSampleComponents(tix);
        if (scs == null || scs.isEmpty()) {
            tmpSampleComponent = new Item();
            tmpSampleComponent.setName(tix.getName());
            tmpSampleComponent.setParentItem(tix);
            tmpSampleComponent.setItemType(ItemType.SampleComponent);
            tmpSampleComponent.setCreatedAt(new Date());
            tmpSampleComponent.setCreater(sessionController.getLoggedUser());
            getFacade().create(tmpSampleComponent);
        } else {
            if (scs.size() > 1) {

                tix.setHasMoreThanOneComponant(true);
                getFacade().edit(tix);
            } else {
                tix.setHasMoreThanOneComponant(false);
                getFacade().edit(tix);
            }
            tmpSampleComponent = tmpSampleComponent = scs.get(0);
        }
        return tmpSampleComponent;
    }

    public void fillMachineTests() {
        if (machine == null) {
            JsfUtil.addErrorMessage("Select a machine");
            return;
        }
        String j = "select i "
                + " from Item i "
                + " where i.itemType=:t "
                + " and i.machine=:m "
                + " and i.retired=:r "
                + " order by i.code";
        Map m = new HashMap();
        m.put("t", ItemType.AnalyzerTest);
        m.put("m", machine);
        m.put("r", false);
        machineTests = getFacade().findByJpql(j, m);
    }

    public List<Item> completeMachineTests(String qry) {
        List<Item> ts;
        String j = "select i from Item i where i.itemType=:t and ((i.name) like :m or (i.name) like :m ) and i.retired=:r order by i.code";
        Map m = new HashMap();
        m.put("t", ItemType.AnalyzerTest);
        m.put("m", "%" + qry.toLowerCase() + "%");
        m.put("r", false);
        ts = getFacade().findByJpql(j, m);
        return ts;
    }

    public void removeTest() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select one to delete");
            return;
        }
        current.setRetired(true);
        current.setRetirer(sessionController.getLoggedUser());
        current.setRetiredAt(new Date());
        getFacade().edit(current);
        fillMachineTests();
        JsfUtil.addSuccessMessage("Removed");
    }

    public void toCreateNewTest() {
        if (machine == null) {
            JsfUtil.addErrorMessage("Select a machine");
            return;
        }
        current = new Item();
        current.setItemType(ItemType.AnalyzerTest);
        current.setCreatedAt(new Date());
        current.setCreater(sessionController.getLoggedUser());
    }

    public void createOrUpdateTest() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select one to delete");
            return;
        }
        current.setMachine(machine);
//        current.setInstitution(machine.getInstitution());
        current.setItemType(ItemType.AnalyzerTest);

        if (current.getId() == null) {
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Added");
        } else {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated");
        }
        fillMachineTests();
    }

    public void refreshInvestigationsAndServices() {
        investigationsAndServices = null;
        getInvestigationsAndServices();
        for (Item i : getInvestigationsAndServices()) {
            i.getItemFeesAuto();
        }
    }

    public void createItemFessForItemsWithoutFee() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            if (i.getItemFeesAuto() == null || i.getItemFeesAuto().isEmpty()) {
                ItemFee itf = new ItemFee();
                itf.setName("Fee");
                itf.setItem(i);
                itf.setInstitution(i.getInstitution());
                itf.setDepartment(i.getDepartment());
                itf.setFeeType(FeeType.OwnInstitution);
                itf.setFee(0.0);
                itf.setFfee(0.0);
                itemFeeManager.setItemFee(itf);
                itemFeeManager.setItem(i);
                itemFeeManager.addNewFee();
            }
        }
    }

    public void markSelectedItemsFeesChangableAtBilling() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            i.setUserChangable(true);
            itemFacade.edit(i);
        }
        JsfUtil.addSuccessMessage("All Marked as Fees Changable at Billing");
    }

    public void updateSelectedItemCategory() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            i.setCategory(selectedCategory);
            itemFacade.edit(i);
        }
        JsfUtil.addSuccessMessage("Category Updated Successfully");
    }

    public void markSelectedItemsAsDiscountableAtBilling() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            i.setDiscountAllowed(true);
            itemFacade.edit(i);
        }
        JsfUtil.addSuccessMessage("All Marked as Fees Changable at Billing");
    }

    public void unmarkSelectedItemsFeesChangableAtBilling() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            i.setUserChangable(false);
            itemFacade.edit(i);
        }
        JsfUtil.addSuccessMessage("All Unmarked as Fees Changable at Billing");
    }

    public void markSelectedItemsAsPrintSessionNumber() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            i.setPrintSessionNumber(true);
            itemFacade.edit(i);
        }
        JsfUtil.addSuccessMessage("All Marked as Print Session Numbers");
    }

    public void unmarkSelectedItemsAsNotToPrintSessionNumber() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            i.setPrintSessionNumber(false);
            itemFacade.edit(i);
        }
        JsfUtil.addSuccessMessage("All Marked as Not to Print Session Numbers");
    }

    public void unmarkSelectedItemsAsDiscountableAtBilling() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            i.setDiscountAllowed(false);
            itemFacade.edit(i);
        }
        JsfUtil.addSuccessMessage("All Unmarked as Discountable at Billing");
    }

    public void markSelectedItemsForPrintSeparateFees() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            i.setPrintFeesForBills(true);
            itemFacade.edit(i);
        }
        JsfUtil.addSuccessMessage("All Marked for Print Separate Fees");
    }

    public void unMarkSelectedItemsForPrintSeparateFees() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            i.setPrintFeesForBills(false);
            itemFacade.edit(i);
        }
        JsfUtil.addSuccessMessage("All Unmarked for Print Separate Fees");
    }

    public void markSelectedItemsForRequestForQuentity() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            i.setRequestForQuentity(true);
            itemFacade.edit(i);
        }
        JsfUtil.addSuccessMessage("All Marked for Request For Quentity");
    }

    public void unMarkSelectedItemsForRequestForQuentity() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            i.setRequestForQuentity(false);
            itemFacade.edit(i);
        }
        JsfUtil.addSuccessMessage("All Unmarked for Request For Quentity");
    }

    public void markSelectedItemsForRatesvisibleduringInwardBilling() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            i.setChargesVisibleForInward(true);
            itemFacade.edit(i);
        }
        JsfUtil.addSuccessMessage("All Marked for Rates visible during Inward Billing");
    }

    public void unMarkSelectedItemsForRatesvisibleduringInwardBilling() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            i.setChargesVisibleForInward(false);
            itemFacade.edit(i);
        }
        JsfUtil.addSuccessMessage("All Unmarked for Rates visible during Inward Billing");
    }

    public void addSessionNumberType() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            i.setSessionNumberType(SessionNumberType.ByBill);
            itemFacade.edit(i);
        }
        JsfUtil.addSuccessMessage("All Unmarked for Print Separate Fees");
    }

    public void updateSelectedItemFees() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            if (!(i.getItemFeesAuto() == null) && !(i.getItemFeesAuto().isEmpty())) {
                double t = 0.0;
                double tf = 0.0;
                for (ItemFee itf : i.getItemFeesAuto()) {
                    getItemFeeFacade().edit(itf);
                    t += itf.getFee();
                    tf += itf.getFfee();
                }
                i.setTotal(t);
                i.setTotalForForeigner(tf);
                getFacade().edit(i);
            }
        }
        investigationsAndServices = null;
        getInvestigationsAndServices();
    }

    public void updateSelectedFeesForDiscountAllow() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            if (!(i.getItemFeesAuto() == null) && !(i.getItemFeesAuto().isEmpty())) {
                for (ItemFee itf : i.getItemFeesAuto()) {
                    if (itf.getFeeType() == FeeType.OwnInstitution) {
                        itf.setDiscountAllowed(true);
                    } else {
                        itf.setDiscountAllowed(false);
                    }
                    getItemFeeFacade().edit(itf);
                }
                getFacade().edit(i);
            }
        }
        investigationsAndServices = null;
        getInvestigationsAndServices();
    }

    public List<Department> fillInstitutionDepatrments() {
        Map m = new HashMap();
        m.put("ins", current.getInstitution());
        String sql = "Select d From Department d "
                + " where d.retired=false "
                + " and d.institution=:ins "
                + " order by d.name";
        departments = departmentFacade.findByJpql(sql, m);
        return departments;
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public void createNewItemsFromMasterItems() {
        if (institution == null) {
            JsfUtil.addErrorMessage("Select institution");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("Select department");
            return;
        }
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Select Items");
            return;
        }
        for (Item i : selectedList) {
            Item ni = null;
            if (i instanceof Investigation) {
                try {
                    ni = new Investigation();
                    BeanUtils.copyProperties(ni, i);

                } catch (IllegalAccessException | InvocationTargetException ex) {
                    Logger.getLogger(ItemController.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (i instanceof Service) {
                try {
                    ni = new Service();
                    BeanUtils.copyProperties(ni, i);
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    Logger.getLogger(ItemController.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                continue;
            }
            if (ni == null) {
                continue;
            }
            ni.setId(null);
            ni.setInstitution(institution);
            ni.setDepartment(department);
            ni.setItemFee(null);
            getFacade().create(ni);
            i.setItemFees(itemFeeManager.fillFees(i));

            for (ItemFee f : i.getItemFees()) {
                ItemFee nf = new ItemFee();
                try {
                    BeanUtils.copyProperties(nf, f);
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    Logger.getLogger(ItemController.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (nf.getInstitution() != null) {
                    nf.setInstitution(institution);
                }
                if (nf.getDepartment() != null) {
                    nf.setDepartment(department);
                }
                nf.setId(null);
                nf.setItem(ni);
                ni.getItemFees().add(nf);
                getItemFeeFacade().create(nf);
            }
            getFacade().edit(ni);
            List<Item> ifis = itemForItemController.getItemsForParentItem(i);
            if (ifis != null) {
                for (Item ifi : ifis) {
                    ItemForItem ifin = new ItemForItem();
                    ifin.setParentItem(ni);
                    ifin.setChildItem(ifi);
                    ifin.setCreatedAt(new Date());
                    ifin.setCreater(getSessionController().getLoggedUser());
                }
            }
        }
    }

    public void updateItemsFromMasterItems() {
        if (institution == null) {
            JsfUtil.addErrorMessage("Select institution");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("Select department");
            return;
        }
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Select Items");
            return;
        }

        for (Item i : selectedList) {
            if (i.getDepartment() != null) {
                i.setDepartment(department);
            }

            if (i.getInstitution() != null) {
                i.setInstitution(institution);
            }
            getFacade().edit(i);
        }

        selectedList = null;

    }

    public void updateItemsAndFees() {
        if (institution == null) {
            JsfUtil.addErrorMessage("Select institution");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("Select department");
            return;
        }
        if (selectedItemFeeList == null || selectedItemFeeList.isEmpty()) {
            JsfUtil.addErrorMessage("Select Items");
            return;
        }

        for (ItemFee fee : selectedItemFeeList) {
            if (fee.getDepartment() != null) {
                fee.setDepartment(department);
            }

            if (fee.getInstitution() != null) {
                fee.setInstitution(institution);
            }
            getItemFeeFacade().edit(fee);
        }

        selectedItemFeeList = null;
    }

    public List<Item> completeDealorItem(String query) {
        List<Item> suggestions;
        String sql;
        HashMap hm = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c.item from ItemsDistributors c"
                    + " where c.retired=false "
                    + " and c.item.retired=false "
                    + " and c.institution=:ins and ((c.item.name) like :q or "
                    + " (c.item.barcode) like :q or (c.item.code) like :q )order by c.item.name";
            hm.put("ins", getInstitution());
            hm.put("q", "%" + query + "%");
            suggestions = getFacade().findByJpql(sql, hm, 20);
        }
        return suggestions;

    }

    public List<Item> getDealorItem() {
        List<Item> suggestions;
        String jpql;
        Map params = new HashMap();
        jpql = "select c.item from ItemsDistributors c where c.retired=false "
                + " and c.institution=:ins "
                + " order by c.item.name";
        params.put("ins", getInstitution());
        suggestions = getFacade().findByJpql(jpql, params);
        return suggestions;
    }

    public List<Item> completeItem(String query, Class[] itemClasses, DepartmentType[] departmentTypes, int count) {
        String sql;
        List<Item> lst;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            lst = new ArrayList<>();
        } else {
            sql = "select c "
                    + " from Item c "
                    + " where c.retired=false ";

            if (departmentTypes != null) {
                sql += " and c.departmentType in :deps ";
                tmpMap.put("deps", Arrays.asList(departmentTypes));
            }

            if (itemClasses != null) {
                sql += " and type(c) in :types ";
                tmpMap.put("types", Arrays.asList(itemClasses));
            }

            sql += " and ((c.name) like :q or (c.code) like :q or (c.barcode) like :q  ) ";
            tmpMap.put("q", "%" + query.toUpperCase() + "%");

            sql += " order by c.name";

            if (count != 0) {
                lst = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, count);
            } else {
                lst = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP);
            }
        }
        return lst;
    }

    /**
     * Overloaded method that returns items sorted by type priority: VTM, ATM, VMP, AMP
     * and then by name within each type
     */
    public List<Item> completeItem(String query, Class[] itemClasses, DepartmentType[] departmentTypes, int count, boolean sortByTypePriority) {
        if (!sortByTypePriority) {
            return completeItem(query, itemClasses, departmentTypes, count);
        }

        // First, get the unsorted results
        List<Item> lst = completeItem(query, itemClasses, departmentTypes, count);

        // Sort by type priority: VTM=1, ATM=2, VMP=3, AMP=4, then by name
        Collections.sort(lst, (Item i1, Item i2) -> {
            int priority1 = getTypePriority(i1);
            int priority2 = getTypePriority(i2);

            if (priority1 != priority2) {
                return Integer.compare(priority1, priority2);
            }

            // Same priority, sort by name
            if (i1.getName() != null && i2.getName() != null) {
                return i1.getName().compareToIgnoreCase(i2.getName());
            }
            return 0;
        });

        return lst;
    }

    /**
     * Helper method to determine type priority for sorting
     * VTM=1, ATM=2, VMP=3, AMP=4, Others=5
     */
    private int getTypePriority(Item item) {
        if (item instanceof Vtm) {
            return 1;
        } else if (item instanceof Atm) {
            return 2;
        } else if (item instanceof Vmp) {
            return 3;
        } else if (item instanceof Amp) {
            return 4;
        }
        return 5;
    }

    public List<Item> completeItemWithRetired(String query, Class[] itemClasses, DepartmentType[] departmentTypes, int count) {
        String sql;
        List<Item> lst;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            lst = new ArrayList<>();
        } else {
            sql = "select c "
                    + " from Item c "
                    + " where ((c.name) like :q or (c.code) like :q or (c.barcode) like :q  ) ";
            tmpMap.put("q", "%" + query.toUpperCase() + "%");

            if (departmentTypes != null) {
                sql += " and c.departmentType in :deps ";
                tmpMap.put("deps", Arrays.asList(departmentTypes));
            }

            if (itemClasses != null) {
                sql += " and type(c) in :types ";
                tmpMap.put("types", Arrays.asList(itemClasses));
            }

            sql += " order by c.name";

            if (count != 0) {
                lst = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, count);
            } else {
                lst = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP);
            }
        }
        return lst;
    }

    public List<Item> findItemsFromBarcode(String barcode) {
        String sql;
        List<Item> lst;
        HashMap tmpMap = new HashMap();
        if (barcode == null) {
            lst = new ArrayList<>();
        } else {
            sql = "select c "
                    + " from Item c "
                    + " where c.retired=false ";

            sql += " and c.barcode=:q ";
            tmpMap.put("q", barcode);

            sql += " order by c.name";

            lst = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP);

        }
        return lst;
    }

    public List<Item> completeMasterItems(String query) {
        String jpql;
        List<Item> lst;
        if (query == null) {
            lst = new ArrayList<>();
        } else {
            HashMap tmpMap = new HashMap();
            jpql = "select damith "
                    + " from Item damith "
                    + " where damith.retired=:ret ";
            jpql += " and (damith.name like :q or damith.code like :q or damith.barcode like :q ) ";
            jpql += " and damith.isMasterItem=:mi ";
            tmpMap.put("q", "%" + query + "%");
            tmpMap.put("mi", true);
            tmpMap.put("ret", false);
            jpql += " order by damith.name";

            lst = getFacade().findByJpql(jpql, tmpMap);
        }

        return lst;
    }

    public List<Item> completeItem(String query) {
        return completeItem(query, null, null, 20);
    }

    List<Item> itemList;

    List<Item> suggestions;

    public List<Item> completeMedicine(String query) {
        DepartmentType[] dts = new DepartmentType[]{DepartmentType.Pharmacy, null};
        Class[] classes = new Class[]{Vmp.class, Amp.class, Vmp.class, Amp.class, Vmpp.class, Ampp.class};
        return completeItem(query, classes, dts, 0);
    }

    /**
     * Returns medicine items sorted by type priority: VTM, ATM, VMP, AMP
     * and then by name within each type
     */
    public List<Item> completeMedicineByTypePriority(String query) {
        DepartmentType[] dts = new DepartmentType[]{DepartmentType.Pharmacy, null};
        Class[] classes = new Class[]{Vtm.class, Atm.class, Vmp.class, Amp.class, Vmpp.class, Ampp.class};
        return completeItem(query, classes, dts, 0, true);
    }

    public List<Item> completeLabItemOnly(String query) {
        DepartmentType[] dts = new DepartmentType[]{DepartmentType.Lab};
        Class[] classes = new Class[]{Amp.class};
        return completeItem(query, classes, dts, 0);
    }

    public List<Item> completeItem(String query, Class[] itemClasses, DepartmentType[] departmentTypes) {
        return completeItem(query, itemClasses, departmentTypes, 0);
    }

    public List<Item> completeAmpItem(String query) {
        List<Item> suggestions = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return suggestions;
        } else {
            String[] words = query.split("\\s+");
            String sql = "SELECT c FROM Item c WHERE c.retired = false AND type(c) = :amp AND "
                    + "(c.departmentType IS NULL OR c.departmentType != :dep) AND (";

            // Dynamic part of the query for the name field using each word
            StringBuilder nameConditions = new StringBuilder();
            for (int i = 0; i < words.length; i++) {
                if (i > 0) {
                    nameConditions.append(" AND ");
                }
                nameConditions.append("LOWER(c.name) LIKE :nameStr").append(i);
            }

            // Adding name conditions and the static conditions for code and barcode
            sql += "(" + nameConditions + ") OR LOWER(c.code) LIKE :codeStr "
                    + "OR LOWER(c.barcode) LIKE :barcodeStr OR c.barcode = :exactBarcodeStr) "
                    + "ORDER BY c.name";

            // Setting parameters
            HashMap<String, Object> tmpMap = new HashMap<>();
            tmpMap.put("dep", DepartmentType.Store);
            tmpMap.put("amp", Amp.class);
            tmpMap.put("codeStr", "%" + query.toLowerCase() + "%");
            tmpMap.put("barcodeStr", "%" + query.toLowerCase() + "%");
            tmpMap.put("exactBarcodeStr", query);

            for (int i = 0; i < words.length; i++) {
                tmpMap.put("nameStr" + i, "%" + words[i].toLowerCase() + "%");
            }
            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
        }
        return suggestions;
    }

    public List<Item> completeAmpItemForLoggedDepartment(String query) {
        List<Item> suggestions = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return suggestions;
        } else {
            String[] words = query.split("\\s+");
           String sql = "SELECT c FROM Item c WHERE c.retired = false AND type(c) = :amp AND c.departmentType in :dts AND (";

            StringBuilder nameConditions = new StringBuilder();
            for (int i = 0; i < words.length; i++) {
                if (i > 0) {
                    nameConditions.append(" AND ");
                }
                nameConditions.append("LOWER(c.name) LIKE :nameStr").append(i);
            }

            sql += "(" + nameConditions + ") OR LOWER(c.code) LIKE :codeStr "
                    + "OR LOWER(c.barcode) LIKE :barcodeStr OR c.barcode = :exactBarcodeStr) "
                    + "ORDER BY c.name";

            HashMap<String, Object> tmpMap = new HashMap<>();
            tmpMap.put("amp", Amp.class);
            tmpMap.put("dts", sessionController.getAvailableDepartmentTypesForPharmacyTransactions());
            tmpMap.put("codeStr", "%" + query.toLowerCase() + "%");
            tmpMap.put("barcodeStr", "%" + query.toLowerCase() + "%");
            tmpMap.put("exactBarcodeStr", query);

            for (int i = 0; i < words.length; i++) {
                tmpMap.put("nameStr" + i, "%" + words[i].toLowerCase() + "%");
            }

            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
        }
        return suggestions;
    }

//    @Deprecated(forRemoval = true)
//    public List<Item> completeAmps(String query) {
//        // Please use Amp Controller completeAmps
//        //#{ampController.completeAmp}
//        String jpql;
//        HashMap params = new HashMap();
//        if (query == null) {
//            suggestions = new ArrayList<>();
//        } else {
//            jpql = "select c "
//                    + " from Amp c "
//                    + " where c.retired=false "
//                    + " and "
//                    + " (c.departmentType is null or c.departmentType!=:dep ) "
//                    + " and "
//                    + " ("
//                    + " (c.name) like :str "
//                    + " or "
//                    + " (c.code) like :str "
//                    + " or "
//                    + " (c.barcode) like :str ) "
//                    + "order by c.name";
//            params.put("dep", DepartmentType.Pharmacy);
//            params.put("str", "%" + query.toUpperCase() + "%");
//            System.out.println("jpql = " + jpql);
//            System.out.println("params = " + params);
//            suggestions = getFacade().findByJpql(jpql, params, 30);
//        }
//        return suggestions;
//
//    }
    public List<Item> completeAmpItemAll(String query) {
        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {

            sql = "select c from Item c where "
                    + " c.retired=:ret "
                    + " and (type(c)= :amp) "
                    + " and "
                    + " ( c.departmentType is null or c.departmentType!=:dep ) "
                    + " and "
                    + " ((c.name) like :str or (c.code) like :str or (c.barcode) like :str ) "
                    + " order by c.name";
            tmpMap.put("dep", DepartmentType.Store);
            tmpMap.put("amp", Amp.class);
            tmpMap.put("ret", false);
            tmpMap.put("str", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
        }
        return suggestions;

    }

    public List<Item> completeStoreItem(String query) {
        DepartmentType[] dts = new DepartmentType[]{DepartmentType.Store, DepartmentType.Inventry};
        Class[] classes = new Class[]{Amp.class};
        return completeItem(query, classes, dts, 0);
//        String sql;
//        HashMap tmpMap = new HashMap();
//        if (query == null) {
//            suggestions = new ArrayList<>();
//        } else {
//
//            sql = "select c from Item c "
//                    + "where c.retired=false and "
//                    + "(type(c)= :amp) "
//                    + "and (c.departmentType=:dep or c.departmentType=:inven )"
//                    + "and ((c.name) like :str or "
//                    + "(c.code) like :str or "
//                    + "(c.barcode) like :str) "
//                    + "order by c.name";
//            //////// // System.out.println(sql);
//            tmpMap.put("amp", Amp.class);
//            tmpMap.put("dep", DepartmentType.Store);
//            tmpMap.put("inven", DepartmentType.Inventry);
//            tmpMap.put("str", "%" + query.toUpperCase() + "%");
//            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
//        }
//        return suggestions;
//
    }

    public List<Item> completeStoreInventryItem(String query) {
        DepartmentType[] dts = new DepartmentType[]{DepartmentType.Inventry};
        Class[] classes = new Class[]{Amp.class};
        return completeItem(query, classes, dts, 0);
//        String sql;
//        HashMap tmpMap = new HashMap();
//        if (query == null) {
//            suggestions = new ArrayList<>();
//        } else {
//
//            sql = "select c from Item c "
//                    + "where c.retired=false and "
//                    + "(type(c)= :amp) "
//                    + "and c.departmentType=:dep "
//                    + "and ((c.name) like :str or "
//                    + "(c.code) like :str or "
//                    + "(c.barcode) like :str) "
//                    + "order by c.name";
//            //////// // System.out.println(sql);
//            tmpMap.put("amp", Amp.class);
//            tmpMap.put("dep", DepartmentType.Inventry);
//            tmpMap.put("str", "%" + query.toUpperCase() + "%");
//            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
//        }
//        return suggestions;

    }

    public List<Item> completeStoreItemOnlyWithRetired(String query) {
        DepartmentType[] dts = new DepartmentType[]{DepartmentType.Store};
        Class[] classes = new Class[]{Amp.class};
        return completeItemWithRetired(query, classes, dts, 0);
    }

    public List<Item> completeStoreItemOnly(String query) {
        DepartmentType[] dts = new DepartmentType[]{DepartmentType.Store};
        Class[] classes = new Class[]{Amp.class};
        return completeItem(query, classes, dts, 0);
//        String sql;
//        HashMap tmpMap = new HashMap();
//        if (query == null) {
//            suggestions = new ArrayList<>();
//        } else {
//
//            sql = "select c from Item c "
//                    + "where c.retired=false and "
//                    + "(type(c)= :amp) "
//                    + "and c.departmentType=:dep "
//                    + "and ((c.name) like :str or "
//                    + "(c.code) like :str or "
//                    + "(c.barcode) like :str) "
//                    + "order by c.name";
//            //////// // System.out.println(sql);
//            tmpMap.put("amp", Amp.class);
//            tmpMap.put("dep", DepartmentType.Store);
//            tmpMap.put("str", "%" + query.toUpperCase() + "%");
//            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
//        }
//        return suggestions;
//
    }

    public List<Item> completeExpenseItem(String query) {
        Class[] classes = new Class[]{BillExpense.class};
        return completeItem(query, classes, null, 0);
//        String sql;
//        HashMap tmpMap = new HashMap();
//        if (query == null) {
//            suggestions = new ArrayList<>();
//        } else {
//            sql = "select c from Item c "
//                    + "where c.retired=false and "
//                    + "(type(c)= :amp) "
//                    + "and ((c.name) like :str or "
//                    + "(c.code) like :str or "
//                    + "(c.barcode) like :str) "
//                    + "order by c.name";
//            //////// // System.out.println(sql);
//            tmpMap.put("amp", BillExpense.class);
//            tmpMap.put("str", "%" + query.toUpperCase() + "%");
//            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
//        }
//        return suggestions;
    }

    public List<Item> fetchStoreItem() {
        List<Item> suggestions;
        String sql;
        HashMap tmpMap = new HashMap();

        sql = "select c from Item c"
                + "  where c.retired=false and "
                + " (type(c)= :amp) "
                + " and c.departmentType=:dep "
                + " order by c.name";
        tmpMap.put("amp", Amp.class);
        tmpMap.put("dep", DepartmentType.Store);

        suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP);

        return suggestions;

    }

    public List<Item> completeAmpAndAmppItem(String query) {
        List<Item> suggestions;
        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            if (query.length() > 4) {
                sql = "select c from Item c where c.retired=false and (type(c)= :amp or type(c)=:ampp ) and ((c.name) like '%" + query.toUpperCase() + "%' or (c.code) like '%" + query.toUpperCase() + "%' or (c.barcode) like '%" + query.toUpperCase() + "%') order by c.name";
            } else {
                sql = "select c from Item c where c.retired=false and (type(c)= :amp or type(c)=:ampp ) and ((c.name) like '%" + query.toUpperCase() + "%' or (c.code) like '%" + query.toUpperCase() + "%') order by c.name";
            }

            tmpMap.put("amp", Amp.class);
            tmpMap.put("ampp", Ampp.class);
            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
        }
        return suggestions;

    }

    public List<Item> completeAmpAndAmppItemForLoggedDepartment(String query) {
        List<Item> suggestions;
        String sql;
        HashMap tmpMap = new HashMap();

        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            if (query.length() > 4) {
                // Criteria:
                // - Not retired
                // - Type is Amp or Ampp
                // - Query matches name, code, or barcode (case-insensitive)
                // - Department type is in allowed list
                sql = "select c from Item c where c.retired=false and (type(c)= :amp or type(c)=:ampp ) and ((c.name) like '%" + query.toUpperCase() + "%' or (c.code) like '%" + query.toUpperCase() + "%' or (c.barcode) like '%" + query.toUpperCase() + "%') and c.departmentType in :dts order by c.name";
            } else {
                // Criteria:
                // - Not retired
                // - Type is Amp or Ampp
                // - Query matches name or code only
                // - Department type is in allowed list
                sql = "select c from Item c where c.retired=false and (type(c)= :amp or type(c)=:ampp ) and ((c.name) like '%" + query.toUpperCase() + "%' or (c.code) like '%" + query.toUpperCase() + "%') and c.departmentType in :dts order by c.name";
            }

            tmpMap.put("amp", Amp.class);
            tmpMap.put("ampp", Ampp.class);
            tmpMap.put("dts", sessionController.getAvailableDepartmentTypesForPharmacyTransactions());
            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
        }

        return suggestions;
    }

    public List<Item> completeAmpAmppVmpVmppItems(String query) {
        List<Item> results;
        String jpql;
        Map<String, Object> parameters = new HashMap<>();

        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String q = query.trim().toUpperCase();

        int barcodeMinLength = 8; // fallback default
        int maxResults = 30; // fallback default

        try {
            barcodeMinLength = configOptionApplicationController.getIntegerValueByKey("BarcodeMinLength", 8);
        } catch (Exception e) {
            // Use default
        }

        try {
            maxResults = configOptionApplicationController.getIntegerValueByKey("PharmaceuticalAutocompleteMaxResults", 30);
        } catch (Exception e) {
            // Use default
        }

        boolean includeBarcode = q.length() >= barcodeMinLength;

        jpql = "SELECT i FROM Item i "
                + "WHERE i.retired = false "
                + "AND TYPE(i) IN (:amp, :ampp, :vmp, :vmpp) "
                + "AND (UPPER(i.name) LIKE :q "
                + "OR UPPER(i.code) LIKE :q "
                + (includeBarcode ? "OR UPPER(i.barcode) LIKE :q " : "")
                + ") "
                + "ORDER BY i.name";

        parameters.put("amp", Amp.class);
        parameters.put("ampp", Ampp.class);
        parameters.put("vmp", Vmp.class);
        parameters.put("vmpp", Vmpp.class);
        parameters.put("q", "%" + q + "%");

        results = getFacade().findByJpql(jpql, parameters, TemporalType.TIMESTAMP, maxResults);

        return results;
    }

    public List<Item> completeAmpAmppVmpVmppItemsForLoggedDepartment(String query) {
        List<Item> results;
        String jpql;
        Map<String, Object> parameters = new HashMap<>();

        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String q = query.trim().toUpperCase();

        int barcodeMinLength = 8; // fallback default
        int maxResults = 30; // fallback default

        try {
            barcodeMinLength = configOptionApplicationController.getIntegerValueByKey("BarcodeMinLength", 8);
        } catch (Exception e) {
            // Use default
        }

        try {
            maxResults = configOptionApplicationController.getIntegerValueByKey("PharmaceuticalAutocompleteMaxResults", 30);
        } catch (Exception e) {
            // Use default
        }

        boolean includeBarcode = q.length() >= barcodeMinLength;

        jpql = "SELECT i FROM Item i "
                + "WHERE i.retired = false "
                + "AND TYPE(i) IN (:amp, :ampp, :vmp, :vmpp) "
                + "AND i.departmentType in :dts "
                + "AND (UPPER(i.name) LIKE :q "
                + "OR UPPER(i.code) LIKE :q "
                + (includeBarcode ? "OR UPPER(i.barcode) LIKE :q " : "")
                + ") "
                + "ORDER BY i.name";

        parameters.put("amp", Amp.class);
        parameters.put("ampp", Ampp.class);
        parameters.put("vmp", Vmp.class);
        parameters.put("vmpp", Vmpp.class);
        parameters.put("dts", sessionController.getAvailableDepartmentTypesForPharmacyTransactions());
        parameters.put("q", "%" + q + "%");

        results = getFacade().findByJpql(jpql, parameters, TemporalType.TIMESTAMP, maxResults);

        return results;
    }

    public List<Item> completeAmpAndVmpItem(String query) {
        List<Item> suggestions;
        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            if (query.length() > 4) {
                sql = "select c from Item c where c.retired=false and (type(c)= :amp or type(c)=:vmp ) and ((c.name) like '%" + query.toUpperCase() + "%' or (c.code) like '%" + query.toUpperCase() + "%' or (c.barcode) like '%" + query.toUpperCase() + "%') order by c.name";
            } else {
                sql = "select c from Item c where c.retired=false and (type(c)= :amp or type(c)=:vmp ) and ((c.name) like '%" + query.toUpperCase() + "%' or (c.code) like '%" + query.toUpperCase() + "%') order by c.name";
            }

//////// // System.out.println(sql);
            tmpMap.put("amp", Amp.class);
            tmpMap.put("vmp", Vmp.class);
            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
        }
        return suggestions;

    }

    public List<Item> fillpackages() {
        String sql;
        sql = "select c from Item c where c.retired=false"
                + " and (c.inactive=false or c.inactive is null) "
                + " and type(c)=Packege "
                + " order by c.name";
        //System.out.println("sql = " + sql);
        packaes = getFacade().findByJpql(sql);
        //System.out.println("packaes = " + packaes);
        if (packaes == null) {
            return new ArrayList<>();
        }
        return packaes;
    }

    public List<Item> completePackage(String query) {
        List<Item> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Item c where c.retired=false"
                    + " and (c.inactive=false or c.inactive is null) "
                    + "and type(c)=Packege "
                    + "and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            suggestions = getFacade().findByJpql(sql);
        }
        return suggestions;

    }

    public List<Item> completeService(String query) {
        List<Item> suggestions;
        String sql;
        HashMap hm = new HashMap();

        sql = "select c from Item c where c.retired=false and type(c)=:cls"
                + " and (c.name) like :q order by c.name";

        hm.put("cls", Service.class);
        hm.put("q", "%" + query.toUpperCase() + "%");
        suggestions = getFacade().findByJpql(sql, hm, 20);

        return suggestions;

    }

    public List<Item> completeInvestigation(String query) {
        List<Item> suggestions;
        String sql;
        HashMap hm = new HashMap();

        sql = "select c from Item c where c.retired=false and type(c)=:cls"
                + " and (c.name) like :q or (c.code) like :q2 order by c.name";

        hm.put("cls", Investigation.class);
        hm.put("q", "%" + query.toUpperCase() + "%");
        hm.put("q2", "%" + query + "%");
        suggestions = getFacade().findByJpql(sql, hm, 20);

        return suggestions;

    }

    public List<Item> completeLoggedInstitutionInvestigation(String query) {
        List<Item> lst;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Item c "
                + " where c.retired=false "
                + " and type(c)=:cls "
                + " and (c.name) like :q "
                + " or (c.code) like :q2"
                + " and c.institution=:ins "
                + " order by c.name";
        hm.put("cls", Investigation.class);
        hm.put("q", "%" + query.toUpperCase() + "%");
        hm.put("q2", "%" + query + "%");
        hm.put("ins", sessionController.getLoggedUser().getInstitution());
        lst = getFacade().findByJpql(sql, hm, 20);
        return lst;
    }

    public List<Item> completeServiceWithoutProfessional(String query) {
        List<Item> lst;
        String sql;
        HashMap hm = new HashMap();

        sql = "select c from Item c where c.retired=false and type(c)=:cls"
                + " and (c.name) like :q "
                + " c.id in (Select f.item.id From Itemfee f where f.retired=false "
                + " and f.feeType!=:ftp ) order by c.name ";

        hm.put("ftp", FeeType.Staff);
        hm.put("cls", Service.class);
        hm.put("q", "%" + query.toUpperCase() + "%");
        lst = getFacade().findByJpql(sql, hm, 20);
        return lst;
    }

    public List<Item> completeMedicalPackage(String query) {
        List<Item> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Item c where c.retired=false "
                    + " and (c.inactive=false or c.inactive is null) "
                    + "and type(c)=MedicalPackage "
                    + "and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            suggestions = getFacade().findByJpql(sql);
        }
        return suggestions;

    }

    public List<Item> completeAllServicesAndInvestigations(String query) {
        List<Item> qryResults;
        HashMap<String, Object> m = new HashMap<>();
        String sql;

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("select c from Item c ")
                .append("where c.retired = false ")
                .append("and (c.name like :query or c.code like :query ) ")
                .append("and type(c) != :pac ")
                .append("and (type(c) = :ser ")
                .append("or type(c) = :inv ")
                .append("or type(c) = :ward ")
                .append("or type(c) = :the) ")
                .append("order by c.name");

        m.put("query", "%" + query + "%");

        sql = sqlBuilder.toString();

        m.put("pac", Packege.class);
        m.put("ser", Service.class);
        m.put("inv", Investigation.class);
        m.put("ward", InwardService.class);
        m.put("the", TheatreService.class);

        qryResults = getFacade().findByJpql(sql, m);

        return qryResults;
    }

    public List<Item> getCategoryServicesAndInvestigations(Category cat) {
        List<Item> qryResults;
        Map<String, Object> params = new HashMap<>();
        String sql;

        sql = "SELECT c FROM Item c "
                + "WHERE c.retired = false "
                + "AND c.category = :cat "
                + "AND (TYPE(c) = :type1 OR "
                + "     TYPE(c) = :type2 OR "
                + "     TYPE(c) = :type3 OR "
                + "     TYPE(c) = :type4) "
                + "ORDER BY c.name";

        params.put("cat", cat);
        params.put("type1", Service.class);
        params.put("type2", Investigation.class);
        params.put("type3", InwardService.class);
        params.put("type4", TheatreService.class);

        qryResults = getFacade().findByJpql(sql, params);

        return qryResults;
    }

    public List<Item> completeInwardItems(String query) {
        List<Item> suggestions;
        HashMap m = new HashMap();
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Item c "
                    + " where c.retired=false "
                    + " and type(c)!=:pac "
                    + " and (type(c)=:ser "
                    + " or type(c)=:inv"
                    + " or type(c)=:ward "
                    + " or type(c)=:the)  "
                    + " and ((c.name) like :q or (c.code) like :q ) "
                    + " order by c.name";
            m.put("pac", Packege.class);
            m.put("ser", Service.class);
            m.put("inv", Investigation.class);
            m.put("ward", InwardService.class);
            m.put("the", TheatreService.class);
            m.put("q", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findByJpql(sql, m, 20);
        }
        return suggestions;
    }

    public void fillItemsForInward() {
        HashMap m = new HashMap();
        String sql;
        suggestItems = new ArrayList<>();
        sql = "select c from Item c "
                + " where c.retired=false "
                + " and type(c)!=:pac "
                + " and (type(c)=:ser "
                + " or type(c)=:inv"
                + " or type(c)=:ward "
                + " or type(c)=:the)  "
                + " order by c.name";
        m.put("pac", Packege.class);
        m.put("ser", Service.class);
        m.put("inv", Investigation.class);
        m.put("ward", InwardService.class);
        m.put("the", TheatreService.class);
        suggestItems = getFacade().findByJpql(sql, m);
    }

    public void makeAllItemsToAllowDiscounts() {
        for (Item pi : getItems()) {
            pi.setDiscountAllowed(true);
            itemFacade.edit(pi);
        }
        JsfUtil.addSuccessMessage("All Servies and Investigations were made to allow discounts.");
    }

    public List<Item> completeTheatreItems(String query) {
        List<Item> suggestions;
        HashMap m = new HashMap();
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Item c "
                    + " where c.retired=false "
                    + " and type(c)=:the "
                    //                    + " and type(c)!=:pac "
                    //                    + " and (type(c)=:ser "
                    //                    + " or type(c)=:inv "
                    //                    + " or type(c)=:the)  "
                    + " and (c.name) like :q"
                    + " order by c.name";
//            m.put("pac", Packege.class);
//            m.put("ser", Service.class);
//            m.put("inv", Investigation.class);
            m.put("the", TheatreService.class);
            m.put("q", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findByJpql(sql, m, 20);
        }
        return suggestions;
    }

    Category category;

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    private List<Item> fetchInwardItems(String query, Department department) {
        HashMap m = new HashMap();
        String sql;
        sql = "select c from Item c "
                + " where c.retired=false "
                + " and type(c)!=:pac "
                + " and (type(c)=:ser "
                + " or type(c)=:inward "
                + " or type(c)=:inv) "
                + " and (c.inactive=false or c.inactive is null) "
                + " and (c.name) like :q";
        if (department != null) {
            sql += " and c.department=:dep ";
            m.put("dep", getReportKeyWord().getDepartment());
        }
        sql += " order by c.name";
        m.put("pac", Packege.class);
        m.put("ser", Service.class);
        m.put("inward", InwardService.class);
        m.put("inv", Investigation.class);
        m.put("q", "%" + query.toUpperCase() + "%");

        return getFacade().findByJpql(sql, m, 20);

    }

    private List<Item> fetchInwardItems(String query, Category cat, Department department) {
        HashMap m = new HashMap();
        String sql;
        sql = "select c from Item c "
                + " where c.retired=false "
                + " and c.category=:ct"
                + " and type(c)!=:pac "
                + " and (type(c)=:ser "
                + " or type(c)=:inward "
                + " or type(c)=:inv) "
                + " and (c.inactive=false or c.inactive is null) "
                + " and (c.name) like :q";
        if (department != null) {
            sql += " and c.department=:dep ";
            m.put("dep", getReportKeyWord().getDepartment());
        }
        sql += " order by c.name";
        m.put("ct", cat);
        m.put("pac", Packege.class);
        m.put("ser", Service.class);
        m.put("inv", Investigation.class);
        m.put("inward", InwardService.class);
        m.put("q", "%" + query.toUpperCase() + "%");

        return getFacade().findByJpql(sql, m, 20);

    }

    @Inject
    ServiceSubCategoryController serviceSubCategoryController;

    public ServiceSubCategoryController getServiceSubCategoryController() {
        return serviceSubCategoryController;
    }

    public void setServiceSubCategoryController(ServiceSubCategoryController serviceSubCategoryController) {
        this.serviceSubCategoryController = serviceSubCategoryController;
    }

    public List<Item> completeInwardItemsCategory(String query) {
        List<Item> suggestions = new ArrayList<>();

        if (category == null) {
            suggestions = fetchInwardItems(query, null);
        } else if (category instanceof ServiceCategory) {
            suggestions = fetchInwardItems(query, category, null);
            getServiceSubCategoryController().setParentCategory(category);
            for (ServiceSubCategory ssc : getServiceSubCategoryController().getItems()) {
                suggestions.addAll(fetchInwardItems(query, ssc, null));
            }
        } else {
            suggestions = fetchInwardItems(query, category, null);
        }

        return suggestions;
    }

    public List<Item> completeInwardItemsCategoryNew(String query) {
        List<Item> suggestions = new ArrayList<>();

        if (category == null) {
            suggestions = fetchInwardItems(query, getReportKeyWord().getDepartment());
        } else if (category instanceof ServiceCategory) {
            suggestions = fetchInwardItems(query, category, getReportKeyWord().getDepartment());
            getServiceSubCategoryController().setParentCategory(category);
            for (ServiceSubCategory ssc : getServiceSubCategoryController().getItems()) {
                suggestions.addAll(fetchInwardItems(query, ssc, getReportKeyWord().getDepartment()));
            }
        } else {
            suggestions = fetchInwardItems(query, category, getReportKeyWord().getDepartment());
        }

        return suggestions;
    }

    public List<Item> completeOpdItemsByNamesAndCode(String query) {
        if (sessionController.getApplicationPreference().isInstitutionRestrictedBilling()) {
            return completeOpdItemsByNamesAndCodeInstitutionSpecificOrNotSpecific(query, true);
        } else {
            return completeOpdItemsByNamesAndCodeInstitutionSpecificOrNotSpecific(query, false);
        }
    }

    public void makeItemsAsActiveOrInactiveByRetiredStatus() {
        String j = "select i from Item i";
        List<Item> tis = getFacade().findByJpql(j);
        for (Item i : tis) {
            if (i.isRetired()) {
                i.setInactive(true);
            } else {
                i.setInactive(false);
            }
            getFacade().edit(i);
        }
    }

    public void toggleItemIctiveInactiveState() {
        String j = "select i from Item i";
        List<Item> tis = getFacade().findByJpql(j);
        for (Item i : tis) {
            if (i.isInactive()) {
                i.setInactive(false);
            } else {
                i.setInactive(true);
            }
            getFacade().edit(i);
        }
    }

    public List<Item> completeOpdItemsByNamesAndCodeInstitutionSpecificOrNotSpecific(String query, boolean spcific) {
        if (query == null || query.trim().equals("")) {
            return new ArrayList<>();
        }
        List<Item> mySuggestions;
        HashMap m = new HashMap();
        String sql;

        sql = "select c from Item c "
                + " where c.retired<>true "
                + " and (c.inactive<>true) "
                + " and type(c)!=:pac "
                + " and type(c)!=:inw "
                + " and (type(c)=:ser "
                + " or type(c)=:inv)  "
                + " and ((c.name) like :q or (c.fullName) like :q or "
                + " (c.code) like :q or (c.printName) like :q ) ";
        if (spcific) {
            sql += " and c.institution=:ins";
            m.put("ins", getSessionController().getInstitution());
        }
        if (getReportKeyWord().getDepartment() != null) {
            sql += " and c.department=:dep";
            m.put("dep", getReportKeyWord().getDepartment());
        }
        sql += " order by c.name";
        m.put("pac", Packege.class);
        m.put("inw", InwardService.class);
        m.put("ser", Service.class);
        m.put("inv", Investigation.class);
        m.put("q", "%" + query.toUpperCase() + "%");

        mySuggestions = getFacade().findByJpql(sql, m, 20);

        return mySuggestions;
    }

    public List<Item> completeItemsByDepartment(String query, Department department) {
        List<Item> suggestions;
        HashMap<String, Object> parameters = new HashMap<>();
        String jpql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            jpql = "SELECT c FROM Item c "
                    + "WHERE c.retired = false "
                    + "AND (type(c)=:ser OR type(c)=:inv) "
                    + "AND (c.name LIKE :qry OR c.fullName LIKE :qry OR c.code LIKE :qry) "
                    + "AND c.department=:department "
                    + "ORDER BY c.name";
            parameters.put("ser", Service.class);
            parameters.put("inv", Investigation.class);
            parameters.put("q", "%" + query.toLowerCase() + "%");
            parameters.put("department", department);
            suggestions = getFacade().findByJpql(jpql, parameters, 20);
        }
        return suggestions;
    }

    public List<Item> completeItemsByDepartment(String query, Institution institution) {
        List<Item> suggestions;
        HashMap<String, Object> parameters = new HashMap<>();
        String jpql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            jpql = "SELECT c FROM Item c "
                    + "WHERE c.retired = false "
                    + "AND (type(c)=:ser OR type(c)=:inv) "
                    + "AND (c.name LIKE :qry OR c.fullName LIKE :qry OR c.code LIKE :qry) "
                    + "AND c.department.institution=:ins "
                    + "ORDER BY c.name";
            parameters.put("ser", Service.class);
            parameters.put("inv", Investigation.class);
            parameters.put("q", "%" + query.toLowerCase() + "%");
            parameters.put("ins", institution);
            suggestions = getFacade().findByJpql(jpql, parameters, 20);
        }
        return suggestions;
    }

    public List<Item> completeServicesPlusInvestigationsAll(String query) {
        List<Item> mySuggestions;
        HashMap m = new HashMap();
        String sql;
        if (query == null) {
            mySuggestions = new ArrayList<>();
        } else {
            sql = "select c from Item c "
                    + " where c.retired=false "
                    + " and (type(c)=:ser or type(c)=:inv)  "
                    + " and (c.name) like :q"
                    + " order by c.name";
            m.put("ser", Service.class);
            m.put("inv", Investigation.class);
            m.put("q", "%" + query.toUpperCase() + "%");

            mySuggestions = getFacade().findByJpql(sql, m, 20);
        }
        return mySuggestions;
    }

    public List<Item> getServicesPlusInvestigationsOfSelectedCategory(Category cat) {
        List<Item> mySuggestions;
        HashMap m = new HashMap();
        String sql;
        if (cat == null) {
            mySuggestions = new ArrayList<>();
        } else {
            sql = "select c from Item c "
                    + " where c.retired=false "
                    + " and (type(c)=:ser or type(c)=:inv)  "
                    + " and c.category=:cat "
                    + " order by c.name";
            m.put("ser", Service.class);
            m.put("inv", Investigation.class);
            m.put("cat", cat);

            mySuggestions = getFacade().findByJpql(sql, m, 20);
        }
        return mySuggestions;
    }

    public List<Item> completeItemsByInstitution(String query, Institution institution) {
        List<Item> suggestions;
        HashMap<String, Object> parameters = new HashMap<>();
        String jpql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            jpql = "SELECT c FROM Item c "
                    + "WHERE c.retired = false "
                    + "AND (type(c)=:ser OR type(c)=:inv) "
                    + "AND (LOWER(c.name) LIKE :q) "
                    + "AND c.institution = :institution "
                    + "ORDER BY c.name";
            parameters.put("ser", Service.class);
            parameters.put("inv", Investigation.class);
            parameters.put("q", "%" + query.toLowerCase() + "%");
            parameters.put("institution", institution);
            suggestions = getFacade().findByJpql(jpql, parameters, 20);
        }
        return suggestions;
    }

    @Deprecated
    public List<Item> completeOpdItemsForItemListringStrategyLoggedInstitution(String query) {
        List<Item> mySuggestions;
        HashMap m = new HashMap();
        String sql;
        if (query == null) {
            mySuggestions = new ArrayList<>();
        } else {
            sql = "select c "
                    + " from Item c "
                    + " where c.retired=false "
                    + " and type(c)!=:pac "
                    + " and type(c)!=:inw "
                    + " and (type(c)=:ser "
                    + " or type(c)=:inv)  "
                    + " and (c.name) like :q "
                    + " and c.institution=:ins "
                    + " order by c.name";
            m.put("pac", Packege.class);
            m.put("inw", InwardService.class);
            m.put("ser", Service.class);
            m.put("inv", Investigation.class);
            m.put("ins", sessionController.getInstitution());

            m.put("q", "%" + query.toUpperCase() + "%");

            mySuggestions = getFacade().findByJpql(sql, m, 20);
        }
        return mySuggestions;
    }

    public List<Item> completeItemWithoutPackOwn(String query) {
        List<Item> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<Item>();
        } else {
            sql = "select c from Item c where c.institution.id = " + getSessionController().getInstitution().getId() + " and c.retired=false and type(c)!=Packege and type(c)!=TimedItem and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";

            suggestions = getFacade().findByJpql(sql);
        }
        return suggestions;
    }

    public void makeSelectedAsMasterItems() {
        for (Item i : selectedList) {
            if (i.getInstitution() != null) {
                i.setInstitution(null);
                getFacade().edit(i);
            }
        }
    }

    public List<Item> fetchOPDItemList(boolean ins) {
        List<Item> items = new ArrayList<>();
        HashMap m = new HashMap();
        String sql;

        sql = "select c from Item c "
                + " where c.retired=false "
                + " and type(c)!=:pac "
                + " and type(c)!=:inw "
                + " and (type(c)=:ser "
                + " or type(c)=:inv)  ";

        if (ins) {
            sql += " and c.institution is null ";
        }

        sql += " order by c.name";

        m.put("pac", Packege.class);
        m.put("inw", InwardService.class);
        m.put("ser", Service.class);
        m.put("inv", Investigation.class);
        items = getFacade().findByJpql(sql, m);
        return items;
    }

    public List<ItemFee> fetchOPDItemFeeList(boolean ins, FeeType ftype) {
        List<ItemFee> itemFees = new ArrayList<>();
        HashMap m = new HashMap();
        String sql;

        sql = "select c from ItemFee c "
                + " where c.retired=false "
                + " and type(c.item)!=:pac "
                + " and type(c.item)!=:inw "
                + " and (type(c.item)=:ser "
                + " or type(c.item)=:inv)  ";

        if (ftype != null) {
            sql += " and c.feeType=:fee ";
            m.put("fee", ftype);
        }

        if (ins) {
            sql += " and c.institution is null ";
        }

        sql += " order by c.name";

        m.put("pac", Packege.class);
        m.put("inw", InwardService.class);
        m.put("ser", Service.class);
        m.put("inv", Investigation.class);

        itemFees = getItemFeeFacade().findByJpql(sql, m);
        return itemFees;
    }

    public void createAllItemsFeeList() {
        allItemFees = new ArrayList<>();
        allItemFees = fetchOPDItemFeeList(false, feeType);
    }

    public void updateSelectedOPDItemList() {

    }

    public void createOpdSeviceInvestgationList() {
        itemlist = getItems();
        for (Item i : itemlist) {
            List<ItemFee> tmp = serviceController.getFees(i);
            for (ItemFee itf : tmp) {
                i.setItemFee(itf);
                if (itf.getFeeType() == FeeType.OwnInstitution) {
                    i.setHospitalFee(i.getHospitalFee() + itf.getFee());
                    i.setHospitalFfee(i.getHospitalFfee() + itf.getFfee());
                } else if (itf.getFeeType() == FeeType.Staff) {
                    i.setProfessionalFee(i.getProfessionalFee() + itf.getFee());
                    i.setProfessionalFfee(i.getProfessionalFfee() + itf.getFfee());
                }
            }
        }
    }

    public void createInwardList() {
        itemlist = getInwardItems();
        for (Item i : itemlist) {
            List<ItemFee> tmp = serviceController.getFees(i);
            for (ItemFee itf : tmp) {
                i.setItemFee(itf);
                if (itf.getFeeType() == FeeType.OwnInstitution) {
                    i.setHospitalFee(i.getHospitalFee() + itf.getFee());
                    i.setHospitalFfee(i.getHospitalFfee() + itf.getFfee());
                } else if (itf.getFeeType() == FeeType.Staff) {
                    i.setProfessionalFee(i.getProfessionalFee() + itf.getFee());
                    i.setProfessionalFfee(i.getProfessionalFfee() + itf.getFfee());
                }
            }
        }
    }

    /**
     *
     */
    public ItemController() {
    }

    /**
     * Prepare to add new Collecting Centre
     *
     * @return
     */
    public void prepareAdd() {
        current = new Item();
    }

    public void prepareAddingInvestigation() {
        current = new Investigation();
    }

    public void prepareAddingService() {
        current = new Service();
    }

    /**
     *
     * @return
     */
    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    /**
     *
     * @param itemFacade
     */
    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    /**
     *
     * @return
     */
    public SessionController getSessionController() {
        return sessionController;
    }

    /**
     *
     * @param sessionController
     */
    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    /**
     * Return the current item
     *
     * @return
     */
    public Item getCurrent() {
        if (current == null) {
            current = new Item();
        }
        return current;
    }

    /**
     * Set the current item
     *
     * @param current
     */
    public void setCurrent(Item current) {
        this.current = current;
    }

    private ItemFacade getFacade() {
        return itemFacade;
    }

    /**
     *
     * @return
     */
    public List<Item> getItems() {
        if (items == null) {
            fillItemsWithInvestigationsAndServices();
        }
        return items;
    }

    public void fillItemsWithInvestigationsAndServices() {
        String temSql;
        HashMap h = new HashMap();
        temSql = "SELECT i FROM Item i where (type(i)=:t1 or type(i)=:t2 ) and i.retired=false order by i.department.name";
        h.put("t1", Investigation.class);
        h.put("t2", Service.class);
        items = getFacade().findByJpql(temSql, h, TemporalType.TIME);
    }

    public List<Item> getInwardItems() {
        String temSql;
        HashMap h = new HashMap();
        temSql = "SELECT i FROM Item i where type(i)=:t1 and i.retired=false order by i.department.name";
        h.put("t1", InwardService.class);
        items = getFacade().findByJpql(temSql, h, TemporalType.TIME);
        return items;
    }

    public List<Item> getItems(Category category) {
        String jpql;
        HashMap params = new HashMap();
        jpql = "SELECT i "
                + " FROM Item i "
                + " where i.category=:cat "
                + " and i.retired=:ret "
                + " order by i.name";
        params.put("cat", category);
        params.put("ret", false);
        return getFacade().findByJpql(jpql, params);
    }

    /**
     *
     * Set all Items to null
     *
     */
    private void recreateModel() {
        items = null;
        allItems = null;
        itemApplicationController.setItems(null);
    }

    /**
     *
     */
    public void saveSelected() {
        saveSelected(getCurrent());
        JsfUtil.addSuccessMessage("Saved");
        recreateModel();
        allItems = null;
        getAllItems();
        getItems();
        current = null;
        getCurrent();
    }

    public void saveSelectedWithItemLight() {
        saveSelected(getCurrent());
        JsfUtil.addSuccessMessage("Saved");
        recreateModel();
        getAllItems();
    }

    public void saveSelected(Item item) {
        if (item.getId() != null && item.getId() > 0) {
            getFacade().edit(item);
        } else {
            item.setCreatedAt(new Date());
            item.setCreater(getSessionController().getLoggedUser());
            getFacade().create(item);
        }
    }

    /**
     *
     * Delete the current Item
     *
     */
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
        getAllItems();
        getItems();
        current = null;
    }

    public void deleteWithItemLight() {
        if (getCurrent() == null) {
            JsfUtil.addSuccessMessage("No such item");
            return;
        }
        getCurrent().setRetired(true);
        getCurrent().setRetiredAt(new Date());
        getCurrent().setRetirer(getSessionController().getLoggedUser());
        getFacade().edit(getCurrent());
        JsfUtil.addSuccessMessage("Deleted Successfully");
        recreateModel();
        getAllItems();
        selectedItemLight = null;
    }

    public Institution getInstitution() {
        if (institution == null) {
            institution = getSessionController().getInstitution();
        }
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public FeeType getFeeType() {
        return feeType;
    }

    public void setFeeType(FeeType feeType) {
        this.feeType = feeType;
    }

    public List<Item> getSelectedList() {
        return selectedList;
    }

    public void setSelectedList(List<Item> selectedList) {
        this.selectedList = selectedList;
    }

    public List<ItemLight> getAllItems() {
        if (allItems == null) {
            allItems = itemApplicationController.getItems();
        }
        return allItems;
    }

    public void setAllItems(List<ItemLight> allItems) {
        this.allItems = allItems;
    }

    public List<ItemFee> getAllItemFees() {
        return allItemFees;
    }

    public void setAllItemFees(List<ItemFee> allItemFees) {
        this.allItemFees = allItemFees;
    }

    public List<ItemFee> getSelectedItemFeeList() {
        return selectedItemFeeList;
    }

    public void setSelectedItemFeeList(List<ItemFee> selectedItemFeeList) {
        this.selectedItemFeeList = selectedItemFeeList;
    }

    public Department getDepartment() {
        if (department == null) {
            department = getSessionController().getDepartment();
        }
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<Item> getItemList() {
        return itemList;
    }

    public void setItemList(List<Item> itemList) {
        this.itemList = itemList;
    }

    public List<Item> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<Item> suggestions) {
        this.suggestions = suggestions;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade itemFeeFacade) {
        this.itemFeeFacade = itemFeeFacade;
    }

    public ItemFeeManager getItemFeeManager() {
        return itemFeeManager;
    }

    public void setItemFeeManager(ItemFeeManager itemFeeManager) {
        this.itemFeeManager = itemFeeManager;
    }

    public List<Item> getItemlist() {
        return itemlist;
    }

    public void setItemlist(List<Item> itemlist) {
        this.itemlist = itemlist;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public List<Item> getInvestigationsAndServices() {
        if (investigationsAndServices == null) {
            investigationsAndServices = fillInvestigationsAndServices();
        }
        return investigationsAndServices;
    }

    public List<Item> fillInvestigationsAndServices() {
        UserPreference up = sessionController.getDepartmentPreference();
        switch (up.getInwardItemListingStrategy()) {
            case ALL_ITEMS:
                return listAllInvestigationsAndServices();
            case ITEMS_OF_LOGGED_DEPARTMENT:
                return listInvestigationsAndServicesOfLoggedDepartment();
            case ITEMS_OF_LOGGED_INSTITUTION:
                return listInvestigationsAndServicesOfLoggedInstitution();
            case ITEMS_MAPPED_TO_LOGGED_DEPARTMENT:
                return fillInvestigationsAndServicesMappingToDepartment(sessionController.getDepartment());
            case ITEMS_MAPPED_TO_LOGGED_INSTITUTION:
                return fillInvestigationsAndServicesMappingToInstitution(sessionController.getInstitution());
            default:
                return listAllInvestigationsAndServices();
        }
    }

    public List<Item> listAllInvestigationsAndServices() {
        if (investigationsAndServices == null) {
            investigationsAndServices = listInvestigationsAndServices(null, null);
        }
        return investigationsAndServices;
    }

    public List<Item> listInvestigationsAndServicesOfLoggedInstitution() {
        if (investigationsAndServices == null) {
            investigationsAndServices = listInvestigationsAndServices(sessionController.getLoggedUser().getInstitution(), null);
        }
        return investigationsAndServices;
    }

    public List<Item> listInvestigationsAndServicesOfLoggedDepartment() {
        if (investigationsAndServices == null) {
            investigationsAndServices = listInvestigationsAndServices(sessionController.getLoggedUser().getInstitution(), sessionController.getLoggedUser().getDepartment());
        }
        return investigationsAndServices;
    }

    public List<Item> fillInvestigationsAndServicesMappingToInstitution(Institution institution) {
        if (investigationsAndServices == null) {
            investigationsAndServices = fillInvestigationsAndServicesMapping(institution, null);
        }
        return investigationsAndServices;
    }

    public List<Item> fillInvestigationsAndServicesMappingToDepartment(Department department) {
        if (investigationsAndServices == null) {
            investigationsAndServices = fillInvestigationsAndServicesMapping(null, department);
        }
        return investigationsAndServices;
    }

    public List<Item> fillInvestigationsAndServicesMapping(Institution institution, Department department) {
        if (investigationsAndServices == null) {
            String jpql = "SELECT im.item"
                    + " FROM ItemMapping im "
                    + " WHERE im.retired = false ";
            HashMap<String, Object> parameters = new HashMap<>();
            if (institution != null) {
                jpql += " and im.institution=:ins ";
                parameters.put("ins", institution);
            }

            if (department != null) {
                jpql += " and im.department=:dep ";
                parameters.put("dep", department);
            }

            jpql += " ORDER BY im.item.name ";
            investigationsAndServices = itemFacade.findByJpql(jpql, parameters);

        }
        return investigationsAndServices;
    }

    public List<Item> fillDepartmentItems(Department department) {
        String jpql = "SELECT item"
                + " FROM Item item "
                + " WHERE item.retired=:ret ";
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("ret", false);
        if (department != null) {
            jpql += " and item.department=:dep ";
            parameters.put("dep", department);
        }
        jpql += " ORDER BY item.name ";
        return itemFacade.findByJpql(jpql, parameters);
    }

    public List<Item> listInvestigationsAndServices(Institution institution, Department department) {
        if (investigationsAndServices == null) {
            String temSql;
            HashMap h = new HashMap();
            temSql = "SELECT i FROM Item i where (type(i)=:t1 or type(i)=:t2 ) and i.retired=false ";
            h
                    .put("t1", Investigation.class
                    );
            h
                    .put("t2", Service.class
                    );

            if (institution != null) {
                temSql += " and i.institution=:ins ";
                h.put("ins", institution);
            }

            if (department != null) {
                temSql += " and i.department=:dep ";
                h.put("dep", department);
            }
            temSql += " order by i.department.name";
            investigationsAndServices = getFacade().findByJpql(temSql, h, TemporalType.TIME);
        }
        return investigationsAndServices;
    }

    public void setInvestigationsAndServices(List<Item> investigationsAndServices) {
        this.investigationsAndServices = investigationsAndServices;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public List<Item> getMachineTests() {
        return machineTests;
    }

    public void setMachineTests(List<Item> machineTests) {
        this.machineTests = machineTests;
    }

    public List<Item> getInvestigationSampleComponents() {
        return investigationSampleComponents;
    }

    public void setInvestigationSampleComponents(List<Item> investigationSampleComponents) {
        this.investigationSampleComponents = investigationSampleComponents;
    }

    public Item getSampleComponent() {
        return sampleComponent;
    }

    public void setSampleComponent(Item sampleComponent) {
        this.sampleComponent = sampleComponent;
    }

    public boolean isMasterItem() {
        return masterItem;
    }

    public void setMasterItem(boolean masterItem) {
        this.masterItem = masterItem;
    }

    public List<ItemFee> getItemFeesList() {
        return ItemFeesList;
    }

    public void setItemFeesList(List<ItemFee> ItemFeesList) {
        this.ItemFeesList = ItemFeesList;
    }

    public List<ItemLight> fillItemsByDepartment(Department dept) {
        List<ItemLight> deptItems = new ArrayList<>();
        for (ItemLight i : itemApplicationController.getItems()) {
            if (i.getDepartmentId() != null && i.getDepartmentId().equals(dept.getId())) {
                deptItems.add(i);
            }
        }
        return deptItems;
    }

    public void reloadItemsFromDatabase() {
        itemApplicationController.fillAllItemsBypassingCache();
        // Clear this controller’s cached copies to avoid stale data
        recreateModel();
        reloadItems();
        JsfUtil.addSuccessMessage("Items reloaded from database.");
    }

    public List<ItemLight> fillItemsByInstitution(Institution institution) {
        List<ItemLight> insItems = new ArrayList<>();
        if (institution == null) {
            return insItems;
        }
        if (institution.getId() == null) {
            return insItems;
        }
        for (ItemLight i : itemApplicationController.getItems()) {
            if (i.getInstitutionId() == null) {
                continue;
            }
            if (Objects.equals(i.getInstitutionId(), institution.getId())) {
                insItems.add(i);
            }
        }

        return insItems;
    }

    public void reloadItems() {
        departmentItems = null;
        institutionItems = null;
        packaes = null;
    }

    public List<ItemLight> getDepartmentItems() {
        if (departmentItems == null) {
            departmentItems = fillItemsByDepartment(getSessionController().getDepartment());
        }
        return departmentItems;
    }

    public List<ItemLight> getInstitutionItems() {
        if (institutionItems == null) {
            institutionItems = fillItemsByInstitution(getSessionController().getInstitution());
        }
        return institutionItems;
    }

    public List<ItemLight> getCcDeptItems() {
        return ccDeptItems;
    }

    public void setCcDeptItems(List<ItemLight> ccDeptItems) {
        this.ccDeptItems = ccDeptItems;
    }

    public List<ItemLight> getCcInstitutionItems() {
        return ccInstitutionItems;
    }

    public void setCcInstitutionItems(List<ItemLight> ccInstitutionItems) {
        this.ccInstitutionItems = ccInstitutionItems;
    }

    public List<ItemFeeRow> getItemFeeRows() {
        return itemFeeRows;
    }

    public void setItemFeeRows(List<ItemFeeRow> itemFeeRows) {
        this.itemFeeRows = itemFeeRows;
    }

    public ItemLight findItemLightById(Long id) {
        Optional<ItemLight> itemLightOptional = findItemLightByIdStreaming(id);
        ItemLight il = itemLightOptional.orElse(null);
        return il;
    }

    public Optional<ItemLight> findItemLightByIdStreaming(Long id) {
        if (id == null) {
            return Optional.empty(); // Clearly indicate absence of value
        }
        return itemApplicationController.getItems().stream()
                .filter(itemLight -> id.equals(itemLight.getId()))
                .findFirst(); // Returns an Optional describing the first matching element, or an empty Optional if no match is found
    }

    public ItemLight getSelectedItemLight() {
        if (getCurrent() == null) {
            selectedItemLight = null;
        } else {
            selectedItemLight = new ItemLight(getCurrent());
        }
        return selectedItemLight;
    }

    public void setSelectedItemLight(ItemLight selectedItemLight) {
        this.selectedItemLight = selectedItemLight;
        if (selectedItemLight == null) {
            setCurrent(null);
        } else {
            setCurrent(findItem(selectedItemLight.getId()));
        }
    }

    public List<DepartmentItemCount> getDepartmentItemCounts() {
        return departmentItemCounts;
    }

    public void setDepartmentItemCounts(List<DepartmentItemCount> departmentItemCounts) {
        this.departmentItemCounts = departmentItemCounts;
    }

    public DepartmentItemCount getDepartmentItemCount() {
        return departmentItemCount;
    }

    public void setDepartmentItemCount(DepartmentItemCount departmentItemCount) {
        this.departmentItemCount = departmentItemCount;
    }

    public List<InstitutionItemCount> getInstitutionItemCounts() {
        return institutionItemCounts;
    }

    public void setInstitutionItemCounts(List<InstitutionItemCount> institutionItemCounts) {
        this.institutionItemCounts = institutionItemCounts;
    }

    public InstitutionItemCount getInstitutionItemCount() {
        return institutionItemCount;
    }

    public void setInstitutionItemCount(InstitutionItemCount institutionItemCount) {
        this.institutionItemCount = institutionItemCount;
    }

    public List<ItemLight> getFilteredItems() {
        return filteredItems;
    }

    public void setFilteredItems(List<ItemLight> filteredItems) {
        this.filteredItems = filteredItems;
    }

    public Institution getFilterInstitution() {
        return filterInstitution;
    }

    public void setFilterInstitution(Institution filterInstitution) {
        this.filterInstitution = filterInstitution;
    }

    public Department getFilterDepartment() {
        return filterDepartment;
    }

    public void setFilterDepartment(Department filterDepartment) {
        this.filterDepartment = filterDepartment;
    }

    public List<Item> getPackaes() {
        if (packaes == null) {
            packaes = fillpackages();
        }
        return packaes;
    }

    public void setPackaes(List<Item> packaes) {
        this.packaes = packaes;
    }

    public List<Item> getSuggestItems() {
        return suggestItems;
    }

    public void setSuggestItems(List<Item> suggestItems) {
        this.suggestItems = suggestItems;
    }

    public Sex getPatientGender() {
        return patientGender;
    }

    public void setPatientGender(Sex patientGender) {
        this.patientGender = patientGender;
    }

    private Item findItemById(Long id) {
        String jpql = "select i "
                + " from Item i "
                + " where i.retired=:ret "
                + " and i.id=:iid ";
        Map params = new HashMap<>();
        params.put("ret", false);
        params.put("iid", id);
        return getFacade().findFirstByJpql(jpql, params);
    }

    private Item findItemById(String id) {
        try {
            Long longId = Long.parseLong(id); // Convert String to Long
            return findItemById(longId); // Reuse the existing method
        } catch (NumberFormatException e) {
// Log the error if the string is not a valid Long
            return null;
        }
    }

    public Category getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(Category selectedCategory) {
        this.selectedCategory = selectedCategory;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public List<ItemFee> getImportedFees() {
        return importedFees;
    }

    public void setImportedFees(List<ItemFee> importedFees) {
        this.importedFees = importedFees;
    }

    public List<String> getImportFailures() {
        return importFailures;
    }

    public void setImportFailures(List<String> importFailures) {
        this.importFailures = importFailures;
    }

    public Item findItemByNameAndInstitution(String itemName, String institutionName) {
        String jpql;
        Map m = new HashMap();
        jpql = "select i "
                + " from Item i "
                + " where i.retired=:ret "
                + " and i.name=:itemName"
                + " and i.institution.name=:institutionName";
        m.put("ret", false);
        m.put("itemName", itemName);
        m.put("institutionName", institutionName);
        Item item = getFacade().findFirstByJpql(jpql, m);
        return item;
    }

    public Institution getCollectionCentre() {
        return collectionCentre;
    }

    public void setCollectionCentre(Institution collectionCentre) {
        this.collectionCentre = collectionCentre;
    }

    public Department getSelectedDepartment() {
        return selectedDepartment;
    }

    public void setSelectedDepartment(Department selectedDepartment) {
        this.selectedDepartment = selectedDepartment;
    }

    @FacesConverter("itemLightConverter")
    public static class ItemLightConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext context, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                Long id = Long.valueOf(value);
                ItemController controller = (ItemController) context.getApplication().getELResolver()
                        .getValue(context.getELContext(), null, "itemController");
                ItemLight il = controller.findItemLightById(id);
                return il;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public String getAsString(FacesContext context, UIComponent component, Object value) {
            if (value instanceof ItemLight) {
                return ((ItemLight) value).getId().toString(); // Assuming getId() returns the ID
            }
            return null; // Or handle the error condition
        }
    }

    @FacesConverter(forClass = Item.class)
    public static class ItemControllerConverter implements Converter {

        /**
         *
         * @param facesContext
         * @param component
         * @param value
         * @return
         */
        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ItemController controller = (ItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "itemController");
            return controller.getItemFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key = 0l;
            try {
                key = Long.valueOf(value);
            } catch (Exception e) {

            }

            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        /**
         *
         * @param facesContext
         * @param component
         * @param object
         * @return
         */
        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Item) {
                Item o = (Item) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ItemController.class.getName());
            }
        }
    }

    /**
     *
     */
}
