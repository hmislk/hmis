/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.data.FeeType;
import com.divudi.entity.Department;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Staff;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.Category;
import com.divudi.entity.Institution;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFTableStyleInfo;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author pasan
 */
@Named
@SessionScoped
public class ItemFeeManager implements Serializable {

    /**
     * Creates a new instance of ItemFeeManager
     */
    public ItemFeeManager() {
    }

    Item item;
    ItemFee itemFee;
    ItemFee removingFee;
    private Institution collectingCentre;
    private Institution forSite;
    private Category feeListType;

    List<ItemFee> itemFees;

    @Inject
    FeeValueController feeValueController;
    @EJB
    ItemFeeFacade itemFeeFacade;
    @EJB
    ItemFacade itemFacade;
    @EJB
    DepartmentFacade departmentFacade;
    @EJB
    StaffFacade staffFacade;

    @Inject
    SessionController sessionController;
    @Inject
    ItemController itemController;

    List<Department> departments;
    List<Staff> staffs;
    private List<Item> selectedList;

    private Double totalItemFee;
    private Double totalItemFeeForForeigners;

    public String navigateToInstitutionItemFeeUpload() {
        return "/admin/pricing/institution_item_fee_upload?faces-redirect=true";
    }

    public String navigateItemFeeList() {
        return "/admin/pricing/item_fee_list?faces-redirect=true";
    }

    public String navigateToCollectingCentreItemFeeList() {
        return "/admin/pricing/item_fee_list_collecting_centre?faces-redirect=true";
    }

    public String navigateToCorrectItemFees() {
        return "/dataAdmin/bulk_update_itemsFees?faces-redirect=true";
    }

    public String navigateToUploadItemFees() {
        return "/admin/pricing/item_fee_upload?faces-redirect=true";
    }

    public String navigateToDownloadBaseItemFees() {
        itemFees = new ArrayList<>();
        return "/admin/pricing/download_base_item_fees?faces-redirect=true";
    }

    public String navigateToDownloadItemFeesForSites() {
        itemFees = new ArrayList<>();
        return "/admin/pricing/download_item_fees_for_sites?faces-redirect=true";
    }

    public String navigateToDownloadItemFeesForCollectingCentres() {
        itemFees = new ArrayList<>();
        return "/admin/pricing/download_item_fees_for_collecting_centres?faces-redirect=true";
    }

    public String navigateToUploadFeeListItemFees() {
        return "";
    }

    public String navigateToDownloadItemFeesForLists() {
        itemFees = new ArrayList<>();
        return "/admin/pricing/download_item_fees_for_lists?faces-redirect=true";
    }

    public String navigateToUploadChangedItemFees() {
        itemFees = new ArrayList<>();
        return "/admin/pricing/upload_changed_item_fees?faces-redirect=true";
    }

    public String navigateToUploadFeeListType() {
        return "/admin/pricing/feelist_type_upload?faces-redirect=true";
    }

    public String navigateToUploadInstitutionItemFees() {
        return "/admin/pricing/feelist_item_fees_upload?faces-redirect=true";
    }

    public String navigateToUploadCollectingCentreFeeList() {
        return "/admin/pricing/collecting_centre_price_list_upload?faces-redirect=true";
    }

    public String navigateToUploadCollectingCentreItemFees() {
        return "/admin/pricing/item_fee_upload_collecting_centre?faces-redirect=true";
    }

    public String navigateItemViseFeeList() {
        return "/admin/pricing/manage_item_fees_bulk?faces-redirect=true";
    }

    public void fillBaseItemFees() {
        itemFees = fillFees(null, null, null);
    }

    public void fillFeeListItemFees() {
        itemFees = fillFees(null, null, feeListType);
    }

    public void fillSiteItemFees() {
        itemFees = fillFees(null, forSite, null);
    }

    public void downloadBaseItemFeesAsExcel() throws IOException {
        // Check if itemFees is null or empty
        if (itemFees == null || itemFees.isEmpty()) {
            JsfUtil.addErrorMessage("Please fill item fees first to download them.");
            return;
        }

        // Create a workbook and a sheet
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Base Item Fees");

        // Create the header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Item Code", "Item Name", "Fee Name", "Fee Type", "Discount Allowed", "Retired", "Institution", "Department", "Staff", "Value for Locals", "Value for Foreigners"};

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
        for (ItemFee fee : itemFees) {
            if (fee == null) {
                continue; // Skip null entries
            }

            Row row = sheet.createRow(rowNum++);

            // Lock all cells except fee, foreignFee, feeName, retired, and discountAllowed
            boolean unlock = (rowNum - 1) > 0; // Avoid unlocking headers

            // Null checks for each field to avoid NullPointerException
            createLockedCell(row, 0, fee.getId(), unlock, workbook);

            if (fee.getItem() != null) {
                createLockedCell(row, 1, fee.getItem().getCode(), unlock, workbook);
                createLockedCell(row, 2, fee.getItem().getName(), unlock, workbook);
            } else {
                createLockedCell(row, 1, "", unlock, workbook); // Fallback for null item code
                createLockedCell(row, 2, "", unlock, workbook); // Fallback for null item name
            }

            createUnlockedCell(row, 3, fee.getName(), workbook); // Fee Name - Unlocked

            if (fee.getFeeType() != null) {
                createLockedCell(row, 4, fee.getFeeType().getLabel(), unlock, workbook);
            } else {
                createLockedCell(row, 4, "", unlock, workbook); // Fallback for null fee type
            }

            createUnlockedCell(row, 5, fee.isDiscountAllowed() ? "Yes" : "No", workbook); // Discount Allowed - Unlocked
            createUnlockedCell(row, 6, fee.isRetired() ? "Yes" : "No", workbook); // Retired - Unlocked

            if (fee.getInstitution() != null) {
                createLockedCell(row, 7, fee.getInstitution().getName(), unlock, workbook);
            } else {
                createLockedCell(row, 7, "", unlock, workbook); // Fallback for null institution
            }

            if (fee.getDepartment() != null) {
                createLockedCell(row, 8, fee.getDepartment().getName(), unlock, workbook);
            } else {
                createLockedCell(row, 8, "", unlock, workbook); // Fallback for null department
            }

            if (fee.getStaff() != null && fee.getStaff().getPerson() != null) {
                createLockedCell(row, 9, fee.getStaff().getPerson().getNameWithTitle(), unlock, workbook);
            } else {
                createLockedCell(row, 9, "", unlock, workbook); // Fallback for null staff
            }

            createUnlockedCell(row, 10, fee.getFee(), workbook); // Value for Locals - Unlocked
            createUnlockedCell(row, 11, fee.getFfee(), workbook); // Value for Foreigners - Unlocked
        }

        // Apply a table format, ensuring there are enough rows and columns for the table
        if (rowNum > 1) { // Ensure there are rows beyond the header
            AreaReference area = new AreaReference("A1:L" + rowNum, SpreadsheetVersion.EXCEL2007);
            XSSFTable table = sheet.createTable(area);
            table.setName("BaseItemFeesTable");
            table.setDisplayName("BaseItemFeesTable");

            // Set the table style, with a null check to avoid the NullPointerException
            XSSFTableStyleInfo style = (XSSFTableStyleInfo) table.getStyle();
            if (style != null) {
                style.setName("TableStyleMedium9");
                style.setShowColumnStripes(true);
                style.setShowRowStripes(true);
            } else {
                // Optionally log or handle the case where the style is not applied
                System.out.println("Table style could not be applied.");
            }
        } else {
            // Log or handle the case where no data is present for the table
            System.out.println("No data available to create a table.");
        }

        // Lock the sheet except for the unlocked cells
        sheet.protectSheet("password"); // Replace with your desired password

        // Write the output to the response
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"base_item_fees.xlsx\"");
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

    public void uploadToReplaceItemFees() {

        itemFees = fillFees(null, null, null);
    }

    public void fillForCollectingCentreItemFees() {
        itemFees = fillFees(null, collectingCentre, null);
    }

    public void fillForSiteItemFees() {
        itemFees = fillFees(null, forSite, null);
    }

    public void createItemFessForSelectedItems() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        if (itemFee == null) {
            JsfUtil.addErrorMessage("No Item Fee");
            return;
        }
        ItemFee baseItemFee = itemFee;
        for (Item i : selectedList) {
            ItemFee itf = new ItemFee();
            itf.setName(baseItemFee.getName());
            itf.setItem(i);
            itf.setInstitution(baseItemFee.getInstitution());
            itf.setDepartment(baseItemFee.getDepartment());
            itf.setFeeType(baseItemFee.getFeeType());
            itf.setFee(baseItemFee.getFee());
            itf.setFfee(baseItemFee.getFfee());
            addNewFeeForItem(i, itf);
        }
        itemFee = baseItemFee;
    }

    public void fixIssueToReferralFees() {
        List<ItemFee> ifs = itemFeeFacade.findAll();
        for (ItemFee f : ifs) {
            if (f.getFeeType() == FeeType.Issue) {
                f.setFeeType(FeeType.Referral);
                f.setInstitution(f.getItem().getInstitution());
                f.setDepartment(f.getItem().getDepartment());
                itemFeeFacade.edit(f);
            }
        }
    }

    public ItemFee getRemovingFee() {
        return removingFee;
    }

    public void setRemovingFee(ItemFee removingFee) {
        this.removingFee = removingFee;
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public List<Staff> getStaffs() {
        return staffs;
    }

    public void setStaffs(List<Staff> staffs) {
        this.staffs = staffs;
    }

    public void removeFee() {
        if (removingFee == null) {
            JsfUtil.addErrorMessage("Select a fee");
            return;
        }
        removingFee.setRetired(true);
        removingFee.setRetiredAt(new Date());
        removingFee.setRetirer(sessionController.getLoggedUser());
        itemFeeFacade.edit(removingFee);
        itemFees = null;
        fillFees();
        updateTotal();
        JsfUtil.addSuccessMessage("Removed");
    }

    public void fillDepartments() {
        ////// // System.out.println("fill dept");
        String jpql;
        Map m = new HashMap();
        m.put("ins", getItemFee().getInstitution());
        jpql = "select d from Department d where d.retired=false and d.institution=:ins order by d.name";
        ////// // System.out.println("m = " + m);
        ////// // System.out.println("jpql = " + jpql);
        departments = departmentFacade.findByJpql(jpql, m);
    }

    public void fillStaff() {
        ////// // System.out.println("fill staff");
        String jpql;
        Map m = new HashMap();
        m.put("ins", getItemFee().getSpeciality());
        jpql = "select d from Staff d where d.retired=false and d.speciality=:ins order by d.person.name";
        ////// // System.out.println("m = " + m);
        ////// // System.out.println("jpql = " + jpql);
        staffs = staffFacade.findByJpql(jpql, m);
    }

    public List<Department> compelteDepartments(String qry) {
        String jpql;
        if (qry == null) {
            return new ArrayList<>();
        }
        Map m = new HashMap();
        m.put("ins", getItemFee().getInstitution());
        m.put("name", "%" + qry.toUpperCase() + "%");
        jpql = "select d from Department d where d.retired=false and d.institution=:ins and d.name like :name order by d.name";
        return departmentFacade.findByJpql(jpql, m);
    }

    public String navigateToItemFees() {
        item = null;
        itemFees = null;
        return "/admin/pricing/manage_item_fees?faces-redirect=true";
    }

    public String navigateToCollectingCentreItemFees() {
        collectingCentre = null;
        item = null;
        feeListType = null;
        fillForCollectingCentreFees();
        return "/admin/pricing/manage_collecting_centre_item_fees?faces-redirect=true";
    }

    public String navigateToForSiteItemFees() {
        collectingCentre = null;
        item = null;
        feeListType = null;
        fillForCollectingCentreFees();
        return "/admin/pricing/manage_for_site_item_fees?faces-redirect=true";
    }

    public String navigateToFeeListFees() {
        collectingCentre = null;
        item = null;
        feeListType = null;
        fillForCollectingCentreFees();
        return "/admin/pricing/manage_fee_list_item_fees?faces-redirect=true";
    }

    public String navigateToItemFeesMultiple() {
        return "/admin/pricing/manage_item_fees_multiple?faces-redirect=true";
    }

    public void clearItemFees() {
        item = null;
        itemFees = null;
        removingFee = null;
        collectingCentre = null;
    }

    public void clearSelectedItems() {
        selectedList = new ArrayList<>();
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ItemFee getItemFee() {
        if (itemFee == null) {
            itemFee = new ItemFee();
        }
        return itemFee;
    }

    public void setItemFee(ItemFee itemFee) {
        this.itemFee = itemFee;
    }

    public List<ItemFee> getItemFees() {
        return itemFees;
    }

    public void setItemFees(List<ItemFee> itemFees) {
        this.itemFees = itemFees;
    }

    public void fillFees() {
        itemFees = fillFees(item);
    }

    public void fillForCollectingCentreFees() {
        if (collectingCentre == null) {
            itemFees = null;
            totalItemFee = 0.0;
            totalItemFeeForForeigners = 0.0;
            return;
        }
        itemFees = fillFees(item, collectingCentre);
        totalItemFee = itemFees.stream()
                .filter(Objects::nonNull)
                .mapToDouble(ItemFee::getFee)
                .sum();
        totalItemFeeForForeigners = itemFees.stream()
                .filter(Objects::nonNull)
                .mapToDouble(ItemFee::getFfee)
                .sum();

        feeValueController.updateFeeValue(item, collectingCentre, totalItemFee, totalItemFeeForForeigners);

    }

    public void fillForSiteFees() {
        if (forSite == null) {
            itemFees = null;
            totalItemFee = 0.0;
            totalItemFeeForForeigners = 0.0;
            return;
        }
        itemFees = fillFees(item, forSite);
        totalItemFee = itemFees.stream()
                .filter(Objects::nonNull)
                .mapToDouble(ItemFee::getFee)
                .sum();
        totalItemFeeForForeigners = itemFees.stream()
                .filter(Objects::nonNull)
                .mapToDouble(ItemFee::getFfee)
                .sum();
        feeValueController.updateFeeValue(item, forSite, totalItemFee, totalItemFeeForForeigners);

    }

    public void fillForForCategoryFees() {
        if (feeListType == null) {
            itemFees = null;
            totalItemFee = 0.0;
            totalItemFeeForForeigners = 0.0;
            return;
        }
        itemFees = fillFees(item, feeListType);
        totalItemFee = itemFees.stream()
                .filter(Objects::nonNull)
                .mapToDouble(ItemFee::getFee)
                .sum();
        totalItemFeeForForeigners = itemFees.stream()
                .filter(Objects::nonNull)
                .mapToDouble(ItemFee::getFfee)
                .sum();
    }

    public String toManageItemFees() {
        if (item == null) {
            JsfUtil.addErrorMessage("Nothing Selected to Edit");
            return "";
        }
        fillFees();
        return "/common/manage_item_fees?faces-redirect=true";
    }

    public List<ItemFee> fillFees(Item i) {
        return fillFees(i, null, null);
    }

    public List<ItemFee> fillFees(Item i, Institution ins) {
        return fillFees(i, ins, null);
    }

    public List<ItemFee> fillFees(Item i, Category cat) {
        return fillFees(i, null, cat);
    }

    public List<ItemFee> fillFees(Item i, Institution forInstitution, Category forCategory) {
        String jpql = "select f "
                + " from ItemFee f "
                + " where f.retired=:ret ";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);

        if (i != null) {
            jpql += " and f.item=:i";
            m.put("i", i);
        }

        if (forInstitution != null) {
            jpql += " and f.forInstitution=:ins";
            m.put("ins", forInstitution);
        } else {
            jpql += " and f.forInstitution is null";
        }

        if (forCategory != null) {
            jpql += " and f.forCategory=:cat";
            m.put("cat", forCategory);
        } else {
            jpql += " and f.forCategory is null";
        }
        return itemFeeFacade.findByJpql(jpql, m);
    }

    public void addNewFee() {
        if (item == null) {
            JsfUtil.addErrorMessage("Select Item ?");
            return;
        }
        if (itemFee == null) {
            JsfUtil.addErrorMessage("Select Item Fee");
            return;
        }
        if (itemFee.getName() == null || itemFee.getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please Fill Fee Name");
            return;
        }

        if (itemFee.getFeeType() == null) {
            JsfUtil.addErrorMessage("Please Fill Fee Type");
            return;
        }

        if (itemFee.getFeeType() == FeeType.OtherInstitution || itemFee.getFeeType() == FeeType.OwnInstitution || itemFee.getFeeType() == FeeType.Referral) {
            if (itemFee.getDepartment() == null) {
                JsfUtil.addErrorMessage("Please Select Department");
                return;
            }
        }

//        if (itemFee.getFeeType() == FeeType.Staff) {
//            if (itemFee.getStaff() == null || itemFee.getStaff().getPerson().getName().trim().equals("")) {
//                JsfUtil.addErrorMessage("Please Select Staff");
//                return;
//            }
//        }
        if (itemFee.getFee() == 0.00) {
            JsfUtil.addErrorMessage("Please Enter Local Fee Value");
            return;
        }

        if (itemFee.getFfee() == 0.00) {
            JsfUtil.addErrorMessage("Please Enter Foreign Fee Value");
            return;
        }
        getItemFee().setCreatedAt(new Date());
        getItemFee().setCreater(sessionController.getLoggedUser());
        itemFeeFacade.create(itemFee);

        getItemFee().setItem(item);
        itemFeeFacade.edit(itemFee);

        itemFee = new ItemFee();
        itemFees = null;
        fillFees();
        updateTotal();
        JsfUtil.addSuccessMessage("New Fee Added");
    }

    public void addNewCollectingCentreFee() {
        if (collectingCentre == null) {
            JsfUtil.addErrorMessage("Select Collecting Centre ?");
            return;
        }
        if (item == null) {
            JsfUtil.addErrorMessage("Select Item ?");
            return;
        }
        if (itemFee == null) {
            JsfUtil.addErrorMessage("Select Item Fee");
            return;
        }
        if (itemFee.getName() == null || itemFee.getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please Fill Fee Name");
            return;
        }

        if (itemFee.getFeeType() == null) {
            JsfUtil.addErrorMessage("Please Fill Fee Type");
            return;
        }

        if (itemFee.getFeeType() == FeeType.OtherInstitution || itemFee.getFeeType() == FeeType.OwnInstitution || itemFee.getFeeType() == FeeType.Referral) {
            if (itemFee.getDepartment() == null) {
                JsfUtil.addErrorMessage("Please Select Department");
                return;
            }
        }
        if (itemFee.getFee() == 0.00) {
            JsfUtil.addErrorMessage("Please Enter Local Fee Value");
            return;
        }

        if (itemFee.getFfee() == 0.00) {
            JsfUtil.addErrorMessage("Please Enter Foreign Fee Value");
            return;
        }
        getItemFee().setCreatedAt(new Date());
        getItemFee().setCreater(sessionController.getLoggedUser());
        getItemFee().setForInstitution(collectingCentre);
        itemFeeFacade.create(itemFee);
        getItemFee().setItem(item);
        itemFeeFacade.edit(itemFee);
        itemFee = new ItemFee();
        itemFees = null;
        fillForCollectingCentreFees();
        JsfUtil.addSuccessMessage("New Fee Added for Collecting Centre");
    }

    public void addNewForSiteFee() {
        if (forSite == null) {
            JsfUtil.addErrorMessage("Select a Site ?");
            return;
        }
        if (item == null) {
            JsfUtil.addErrorMessage("Select an Item ?");
            return;
        }
        if (itemFee == null) {
            JsfUtil.addErrorMessage("Select Item Fee");
            return;
        }
        if (itemFee.getName() == null || itemFee.getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please Fill Fee Name");
            return;
        }

        if (itemFee.getFeeType() == null) {
            JsfUtil.addErrorMessage("Please Fill Fee Type");
            return;
        }

        if (itemFee.getFeeType() == FeeType.OtherInstitution || itemFee.getFeeType() == FeeType.OwnInstitution || itemFee.getFeeType() == FeeType.Referral) {
            if (itemFee.getDepartment() == null) {
                JsfUtil.addErrorMessage("Please Select Department");
                return;
            }
        }
        if (itemFee.getFee() == 0.00) {
            JsfUtil.addErrorMessage("Please Enter Local Fee Value");
            return;
        }

        if (itemFee.getFfee() == 0.00) {
            JsfUtil.addErrorMessage("Please Enter Foreign Fee Value");
            return;
        }
        getItemFee().setCreatedAt(new Date());
        getItemFee().setCreater(sessionController.getLoggedUser());
        getItemFee().setForInstitution(forSite);
        itemFeeFacade.create(itemFee);
        getItemFee().setItem(item);
        itemFeeFacade.edit(itemFee);
        itemFee = new ItemFee();
        itemFees = null;

        fillForSiteFees();
        JsfUtil.addSuccessMessage("New Fee Added for Collecting Centre");
    }

    public void addNewFeeForFeeListType() {
        if (feeListType == null) {
            JsfUtil.addErrorMessage("Select Collecting Centre ?");
            return;
        }
        if (item == null) {
            JsfUtil.addErrorMessage("Select Item ?");
            return;
        }
        if (itemFee == null) {
            JsfUtil.addErrorMessage("Select Item Fee");
            return;
        }
        if (itemFee.getName() == null || itemFee.getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please Fill Fee Name");
            return;
        }

        if (itemFee.getFeeType() == null) {
            JsfUtil.addErrorMessage("Please Fill Fee Type");
            return;
        }

        if (itemFee.getFeeType() == FeeType.OtherInstitution || itemFee.getFeeType() == FeeType.OwnInstitution || itemFee.getFeeType() == FeeType.Referral) {
            if (itemFee.getDepartment() == null) {
                JsfUtil.addErrorMessage("Please Select Department");
                return;
            }
        }
        if (itemFee.getFee() == 0.00) {
            JsfUtil.addErrorMessage("Please Enter Local Fee Value");
            return;
        }

        if (itemFee.getFfee() == 0.00) {
            JsfUtil.addErrorMessage("Please Enter Foreign Fee Value");
            return;
        }
        getItemFee().setCreatedAt(new Date());
        getItemFee().setCreater(sessionController.getLoggedUser());
        getItemFee().setForInstitution(null);
        getItemFee().setForCategory(feeListType);
        itemFeeFacade.create(itemFee);

        getItemFee().setItem(item);
        itemFeeFacade.edit(itemFee);

        itemFee = new ItemFee();
        itemFees = null;
        fillForForCategoryFees();
        JsfUtil.addSuccessMessage("New Fee Added for Fee List");
    }

    public void addNewFeeForItem(Item inputItem, ItemFee inputFee) {
        if (inputItem == null) {
            JsfUtil.addErrorMessage("Select Item ?");
            return;
        }
        inputFee.setCreatedAt(new Date());
        inputFee.setCreater(sessionController.getLoggedUser());
        itemFeeFacade.create(inputFee);

        inputFee.setItem(inputItem);
        itemFeeFacade.edit(inputFee);

        List<ItemFee> inputFees = fillFees(inputItem);
        updateTotal(inputItem, inputFees);

    }

    public void saveItemFee(ItemFee inputFee) {
        if (inputFee == null) {
            return;
        }
        if (inputFee.getId() == null) {
            inputFee.setCreatedAt(new Date());
            inputFee.setCreater(sessionController.getLoggedUser());
            itemFeeFacade.create(inputFee);
        } else {
            inputFee.setEditedAt(new Date());
            inputFee.setEditer(sessionController.getLoggedUser());
            itemFeeFacade.edit(inputFee);
        }

        List<ItemFee> inputFees = fillFees(inputFee.getItem());
        updateTotal(inputFee.getItem(), inputFees);

    }

    public void updateFee(ItemFee f) {
        itemFeeFacade.edit(f);
        updateTotal();
    }

    public void updateFee() {
        if (item == null) {
            return;
        }
        double t = 0.0;
        double tf = 0.0;
        for (ItemFee f : itemFees) {
            t += f.getFee();
            tf += f.getFfee();
            itemFeeFacade.edit(f);
        }
        getItem().setTotal(t);
        getItem().setTotalForForeigner(tf);
        itemFacade.edit(getItem());
    }

    public void updateTotal(Item inputItem, List<ItemFee> inputItemFees) {
        if (inputItem == null) {
            return;
        }
        double t = 0.0;
        double tf = 0.0;
        for (ItemFee f : inputItemFees) {
            t += f.getFee();
            tf += f.getFfee();
        }
        inputItem.setTotal(t);
        inputItem.setTotalForForeigner(tf);
        itemFacade.edit(inputItem);
    }

    public void updateTotal() {
        if (item == null) {
            return;
        }
        double t = 0.0;
        double tf = 0.0;
        for (ItemFee f : itemFees) {
            t += f.getFee();
            tf += f.getFfee();
        }
        getItem().setTotal(t);
        getItem().setTotalForForeigner(tf);
        itemFacade.edit(item);
    }

    public List<Item> getSelectedList() {
        if (selectedList == null) {
            selectedList = new ArrayList<>();
        }
        return selectedList;
    }

    public void setSelectedList(List<Item> selectedList) {
        this.selectedList = selectedList;
    }

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    public Double getTotalItemFee() {
        return totalItemFee;
    }

    public void setTotalItemFee(Double totalItemFee) {
        this.totalItemFee = totalItemFee;
    }

    public Double getTotalItemFeeForForeigners() {
        return totalItemFeeForForeigners;
    }

    public void setTotalItemFeeForForeigners(Double totalItemFeeForForeigners) {
        this.totalItemFeeForForeigners = totalItemFeeForForeigners;
    }

    public Category getFeeListType() {
        return feeListType;
    }

    public void setFeeListType(Category feeListType) {
        this.feeListType = feeListType;
    }

    public Institution getForSite() {
        return forSite;
    }

    public void setForSite(Institution forSite) {
        this.forSite = forSite;
    }

}
