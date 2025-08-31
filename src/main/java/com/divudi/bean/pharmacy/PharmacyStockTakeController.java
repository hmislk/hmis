package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.Privileges;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.service.pharmacy.StockTakeApprovalService;
import com.divudi.service.pharmacy.ApprovalProgressTracker;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

/**
 * Controller for capturing pharmacy stock snapshots and exporting them to
 * Excel.
 */
@Named
@SessionScoped
public class PharmacyStockTakeController implements Serializable {

    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;

    @EJB
    private StockFacade stockFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private StockTakeApprovalService stockTakeApprovalService;
    @EJB
    private ApprovalProgressTracker approvalProgressTracker;

    private Bill snapshotBill;
    private Bill physicalCountBill;
    private UploadedFile file;
    private Institution institution;
    private Department department;
    private Date fromDate;
    private Date toDate;
    // Removed legacy snapshotBills; using DTO rows instead
    // Use Light DTOs via constructor-based JPQL to avoid heavy entity graphs
    private List<com.divudi.core.light.common.PharmacySnapshotBillLight> snapshotBillRows;
    private List<VarianceRow> varianceRows; // aggregated variance report rows
    private String approvalJobId; // background approval job id

    /**
     * Generate stock count bill preview without persisting.
     */
    public String generateStockCountBill() {
        if (institution == null) {
            JsfUtil.addErrorMessage("Please select an institution");
            return null;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return null;
        }
        // Optional cross-check to prevent mismatch selections
        if (department.getInstitution() != null && !department.getInstitution().equals(institution)) {
            JsfUtil.addErrorMessage("Selected department does not belong to chosen institution");
            return null;
        }
        // Site selection is not used on this page
        Department dept = department;
        String jpql = "select s from Stock s where s.department=:d and s.stock>0";
        HashMap<String, Object> params = new HashMap<>();
        params.put("d", dept);
        List<Stock> stocks = stockFacade.findByJpql(jpql, params);
        if (stocks == null || stocks.isEmpty()) {
            JsfUtil.addErrorMessage("No stock available");
            return null;
        }
        snapshotBill = new Bill();
        snapshotBill.setBillType(BillType.PharmacySnapshotBill);
        snapshotBill.setBillClassType(BillClassType.BilledBill);
        snapshotBill.setDepartment(dept);
        snapshotBill.setInstitution(dept.getInstitution());
        snapshotBill.setCreatedAt(new Date());
        snapshotBill.setCreater(sessionController.getLoggedUser());
        double total = 0.0;
        for (Stock s : stocks) {
            BillItem bi = new BillItem();
            bi.setBill(snapshotBill);
            bi.setItem(s.getItemBatch().getItem());
            // Cache display fields to avoid lazy loading issues in preview pages
            if (s.getItemBatch() != null && s.getItemBatch().getItem() != null) {
                bi.setDescreption(s.getItemBatch().getItem().getName());
            }
            bi.setQty(s.getStock());
            bi.setCreatedAt(new Date());
            bi.setCreater(sessionController.getLoggedUser());
            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
            pbi.setBillItem(bi);
            pbi.setItemBatch(s.getItemBatch());
            pbi.setQty(s.getStock());
            pbi.setStock(s);
            if (s.getItemBatch() != null) {
                pbi.setStringValue(s.getItemBatch().getBatchNo());
            }
            Double costRate = s.getItemBatch() != null ? s.getItemBatch().getCostRate() : 0.0;
            pbi.setCostRate(costRate != null ? costRate : 0.0);
            double lineValue = (pbi.getCostRate()) * (bi.getQty() != null ? bi.getQty() : 0.0);
            bi.setNetValue(lineValue);
            total += lineValue;
            bi.setPharmaceuticalBillItem(pbi);
            if (snapshotBill.getBillItems() == null) {
                snapshotBill.setBillItems(new java.util.ArrayList<>());
            }
            snapshotBill.getBillItems().add(bi);
        }
        snapshotBill.setNetTotal(total);
        JsfUtil.addSuccessMessage("Preview generated. Please review and settle.");
        return "/pharmacy/pharmacy_stock_take_settle?faces-redirect=true";
    }

    /**
     * Persist the generated stock count bill and navigate to print view.
     */
    public String settleStockCount() {
        if (snapshotBill == null || snapshotBill.getBillItems() == null || snapshotBill.getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Nothing to settle");
            return null;
        }
        // Ensure fresh persistence for a new bill: null out IDs if bill is new
        if (snapshotBill.getId() == null) {
            if (snapshotBill.getBillItems() != null) {
                for (BillItem bi : snapshotBill.getBillItems()) {
                    bi.setId(null);
                    if (bi.getPharmaceuticalBillItem() != null) {
                        bi.getPharmaceuticalBillItem().setId(null);
                    }
                }
            }
        }
        Department dept = snapshotBill.getDepartment();
        if (snapshotBill.getId() == null) {
            String deptId = billNumberBean.departmentBillNumberGenerator(dept, BillType.PharmacySnapshotBill, BillClassType.BilledBill, BillNumberSuffix.NONE);
            snapshotBill.setInsId(deptId);
            snapshotBill.setDeptId(deptId);
            // Cascade will persist bill items and pharmaceutical bill items
            billFacade.create(snapshotBill);
        } else {
            // Existing bill: update only
            billFacade.edit(snapshotBill);
        }
        JsfUtil.addSuccessMessage("Stock count bill saved");
        return "/pharmacy/pharmacy_stock_take_print?faces-redirect=true";
    }

    // Convenience getters for EL to access downloads as properties
    public StreamedContent getDownloadGuidedSheet() {
        return downloadGuidedSheet();
    }

    public StreamedContent getDownloadBlindSheet() {
        return downloadBlindSheet();
    }

    /**
     * Download guided sheet with current system quantities.
     */
    public StreamedContent downloadGuidedSheet() {
        return generateSheet(true, "pharmacy_stock_guided.xlsx");
    }

    /**
     * Download blind sheet without system quantities.
     */
    public StreamedContent downloadBlindSheet() {
        return generateSheet(false, "pharmacy_stock_blind.xlsx");
    }

    private StreamedContent generateSheet(boolean includeSystemQty, String fileName) {
        if (snapshotBill == null) {
            return null;
        }
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Stock");

            // Helpers and formats
            CreationHelper creationHelper = wb.getCreationHelper();
            DataFormat dataFormat = wb.createDataFormat();

            // Styles
            Font headerFont = wb.createFont();
            headerFont.setBold(true);

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setLocked(true);

            CellStyle textLocked = wb.createCellStyle();
            textLocked.setLocked(true);

            CellStyle dateLocked = wb.createCellStyle();
            dateLocked.setLocked(true);
            dateLocked.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd"));

            CellStyle numberLocked = wb.createCellStyle();
            numberLocked.setLocked(true);
            numberLocked.setDataFormat(dataFormat.getFormat("#,##0.00"));

            CellStyle integerLocked = wb.createCellStyle();
            integerLocked.setLocked(true);
            integerLocked.setDataFormat(dataFormat.getFormat("#,##0"));

            CellStyle inputUnlocked = wb.createCellStyle();
            inputUnlocked.setLocked(false);
            inputUnlocked.setDataFormat(dataFormat.getFormat("#,##0.######"));
            inputUnlocked.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            inputUnlocked.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header
            Row header = sheet.createRow(0);
            int col = 0;
            // Always include BillItem ID first for reliable mapping
            Cell hId = header.createCell(col++);
            hId.setCellValue("BillItem ID");
            hId.setCellStyle(headerStyle);
            Cell hCode = header.createCell(col++);
            hCode.setCellValue("Code");
            hCode.setCellStyle(headerStyle);
            Cell hName = header.createCell(col++);
            hName.setCellValue("Name");
            hName.setCellStyle(headerStyle);
            Cell hCat = header.createCell(col++);
            hCat.setCellValue("Category");
            hCat.setCellStyle(headerStyle);
            Cell hBatch = header.createCell(col++);
            hBatch.setCellValue("Batch");
            hBatch.setCellStyle(headerStyle);
            Cell hExp = header.createCell(col++);
            hExp.setCellValue("Expiry Date");
            hExp.setCellStyle(headerStyle);
            Cell hPR = header.createCell(col++);
            hPR.setCellValue("Purchase Rate");
            hPR.setCellStyle(headerStyle);
            Cell hRR = header.createCell(col++);
            hRR.setCellValue("Retail Rate");
            hRR.setCellStyle(headerStyle);
            Cell hCR = header.createCell(col++);
            hCR.setCellValue("Cost Rate");
            hCR.setCellStyle(headerStyle);
            Integer systemQtyColIndex = null;
            if (includeSystemQty) {
                Cell hSys = header.createCell(col++);
                hSys.setCellValue("System Qty");
                hSys.setCellStyle(headerStyle);
                systemQtyColIndex = col - 1;
            }
            int realQtyColIndex = col; // this will be unlocked for input
            Cell hReal = header.createCell(col++);
            hReal.setCellValue("Real Stock Qty");
            hReal.setCellStyle(headerStyle);
            Cell hLV = header.createCell(col++);
            hLV.setCellValue("Line Value");
            hLV.setCellStyle(headerStyle);

            // Resolve items (avoid lazy issues on detached entity)
            List<BillItem> items = snapshotBill.getBillItems();
            if (items == null || items.isEmpty()) {
                if (snapshotBill.getId() != null) {
                    HashMap<String, Object> p = new HashMap<>();
                    p.put("b", snapshotBill);
                    items = billItemFacade.findByJpql("select bi from BillItem bi where bi.bill=:b order by bi.id", p);
                } else {
                    items = java.util.Collections.emptyList();
                }
            }

            // Rows
            int rowNum = 1;
            for (BillItem bi : items) {
                PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
                ItemBatch ib = pbi != null ? pbi.getItemBatch() : null;
                Row row = sheet.createRow(rowNum++);
                int c = 0;

                // BillItem ID
                Cell cId = row.createCell(c++);
                if (bi.getId() != null) {
                    cId.setCellValue(bi.getId());
                    cId.setCellStyle(integerLocked);
                } else {
                    cId.setCellValue(0);
                    cId.setCellStyle(integerLocked);
                }

                // Code
                Cell cCode = row.createCell(c++);
                cCode.setCellValue(ib != null && ib.getItem() != null && ib.getItem().getCode() != null ? ib.getItem().getCode() : "");
                cCode.setCellStyle(textLocked);

                // Name
                Cell cName = row.createCell(c++);
                cName.setCellValue(ib != null && ib.getItem() != null && ib.getItem().getName() != null ? ib.getItem().getName() : (bi.getDescreption() != null ? bi.getDescreption() : ""));
                cName.setCellStyle(textLocked);

                // Category
                Cell cCat = row.createCell(c++);
                cCat.setCellValue(bi.getItem() != null && bi.getItem().getCategory() != null && bi.getItem().getCategory().getName() != null ? bi.getItem().getCategory().getName() : "");
                cCat.setCellStyle(textLocked);

                // Batch
                Cell cBatch = row.createCell(c++);
                cBatch.setCellValue(ib != null && ib.getBatchNo() != null ? ib.getBatchNo() : "");
                cBatch.setCellStyle(textLocked);

                // Expiry
                Cell cExp = row.createCell(c++);
                if (ib != null && ib.getDateOfExpire() != null) {
                    cExp.setCellValue(ib.getDateOfExpire());
                } else {
                    cExp.setCellValue("");
                }
                cExp.setCellStyle(dateLocked);

                // Rates
                double pr = (ib != null) ? ib.getPurcahseRate() : 0.0;
                double rr = (ib != null) ? ib.getRetailsaleRate() : 0.0;
                double cr = (ib != null) ? ib.getCostRate() : 0.0;

                Cell cPR = row.createCell(c++);
                cPR.setCellValue(pr);
                cPR.setCellStyle(numberLocked);
                Cell cRR = row.createCell(c++);
                cRR.setCellValue(rr);
                cRR.setCellStyle(numberLocked);
                Cell cCR = row.createCell(c++);
                cCR.setCellValue(cr);
                cCR.setCellStyle(numberLocked);

                // System Qty (optional)
                if (includeSystemQty) {
                    double sys = pbi != null ? pbi.getQty() : 0.0;
                    Cell cSys = row.createCell(c++);
                    cSys.setCellValue(sys);
                    cSys.setCellStyle(integerLocked);
                }

                // Real Stock Qty (input - unlocked)
                Cell cReal = row.createCell(c++);
                cReal.setCellStyle(inputUnlocked);

                // Line Value (system = cost rate * system qty)
                double sysQtyForLV = includeSystemQty ? (pbi != null ? pbi.getQty() : 0.0) : 0.0;
                double lineValue = cr * sysQtyForLV;
                Cell cLV = row.createCell(c++);
                cLV.setCellValue(lineValue);
                cLV.setCellStyle(numberLocked);
            }

            // Protect sheet so that only the input column is editable
            sheet.protectSheet("protect");

            // Autosize columns
            int totalCols = header.getLastCellNum();
            for (int i = 0; i < totalCols; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            InputStream in = new ByteArrayInputStream(out.toByteArray());
            return DefaultStreamedContent.builder()
                    .name(fileName)
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .stream(() -> in)
                    .build();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Parse uploaded sheet and prepare physical count bill with variances.
     */
    public void parseUploadedSheet() {
        if (snapshotBill == null) {
            JsfUtil.addErrorMessage("No snapshot available");
            return;
        }
        if (file == null) {
            JsfUtil.addErrorMessage("No file uploaded");
            return;
        }
        try (InputStream in = file.getInputStream(); XSSFWorkbook wb = new XSSFWorkbook(in)) {
            XSSFSheet sheet = wb.getSheetAt(0);
            Department dept = snapshotBill.getDepartment();
            physicalCountBill = new Bill();
            physicalCountBill.setBillType(BillType.PharmacyPhysicalCountBill);
            physicalCountBill.setBillClassType(BillClassType.BilledBill);
            physicalCountBill.setDepartment(dept);
            physicalCountBill.setInstitution(dept.getInstitution());
            physicalCountBill.setCreatedAt(new Date());
            physicalCountBill.setCreater(sessionController.getLoggedUser());
            physicalCountBill.setReferenceBill(snapshotBill);
            // Identify relevant columns by header names to be resilient to layout changes
            Row header = sheet.getRow(0);
            int colBillItemId = findColumnIndex(header, "BillItem ID");
            int colCode = findColumnIndex(header, "Code");
            int colBatch = findColumnIndex(header, "Batch");
            int colRealStock = findColumnIndex(header, "Real Stock Qty");

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String code = colCode >= 0 ? getString(row, colCode) : null;
                String batch = colBatch >= 0 ? getString(row, colBatch) : null;
                // If Real Stock is blank, skip the row (do not treat as zero)
                Double physicalObj = colRealStock >= 0 ? getDoubleNullable(row, colRealStock) : null;
                if (physicalObj == null) {
                    continue;
                }
                double physical = physicalObj;

                BillItem snapItem = null;
                if (colBillItemId >= 0) {
                    Long bid = getLongNullable(row, colBillItemId);
                    if (bid != null && bid > 0) {
                        snapItem = findSnapshotBillItemById(bid);
                    }
                }
                if (snapItem == null && code != null && batch != null) {
                    snapItem = findSnapshotBillItem(code, batch);
                }
                if (snapItem == null) {
                    continue;
                }
                BillItem bi = new BillItem();
                bi.setBill(physicalCountBill);
                bi.setItem(snapItem.getItem());
                bi.setQty(physical);
                bi.setCreatedAt(new Date());
                bi.setCreater(sessionController.getLoggedUser());
                bi.setReferanceBillItem(snapItem);
                bi.setAdjustedValue(physical - snapItem.getQty());
                PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
                pbi.setBillItem(bi);
                pbi.setItemBatch(snapItem.getPharmaceuticalBillItem().getItemBatch());
                pbi.setQty(physical);
                bi.setPharmaceuticalBillItem(pbi);
                physicalCountBill.getBillItems().add(bi);
            }
        } catch (IOException e) {
            JsfUtil.addErrorMessage(e, "Error processing file");
            physicalCountBill = null;
        }
    }

    private String getString(Row row, int col) {
        if (row.getCell(col) == null) {
            return null;
        }
        try {
            return row.getCell(col).getStringCellValue().trim();
        } catch (Exception e) {
            try {
                return String.valueOf((long) row.getCell(col).getNumericCellValue());
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private double getDouble(Row row, int col) {
        if (row.getCell(col) == null) {
            return 0.0;
        }
        try {
            return row.getCell(col).getNumericCellValue();
        } catch (Exception e) {
            try {
                return Double.parseDouble(row.getCell(col).getStringCellValue());
            } catch (Exception ex) {
                return 0.0;
            }
        }
    }

    private Double getDoubleNullable(Row row, int col) {
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    String s = cell.getStringCellValue();
                    if (s == null || s.trim().isEmpty()) {
                        return null;
                    }
                    try {
                        return Double.parseDouble(s.trim());
                    } catch (Exception ignored) {
                        return null;
                    }
                case BLANK:
                    return null;
                default:
                    return null;
            }
        } catch (Exception ex) {
            return null;
        }
    }

    private Long getLongNullable(Row row, int col) {
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (long) cell.getNumericCellValue();
                case STRING:
                    String s = cell.getStringCellValue();
                    if (s == null || s.trim().isEmpty()) {
                        return null;
                    }
                    try {
                        return Long.parseLong(s.trim());
                    } catch (Exception ignored) {
                        return null;
                    }
                case BLANK:
                    return null;
                default:
                    return null;
            }
        } catch (Exception ex) {
            return null;
        }
    }

    // Removed legacy listSnapshotBills(); replaced by listSnapshotBillRows()
    // New: List snapshot bill rows using constructor-based DTO and findLightsByJpql
    public void listSnapshotBillRows() {
        HashMap<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();
        jpql.append("select new com.divudi.core.light.common.PharmacySnapshotBillLight( ");
        jpql.append(" b.id, b.deptId, b.createdAt, b.institution.name, b.department.name, ");
        jpql.append(" (select count(bi) from BillItem bi where bi.bill = b), b.netTotal ) ");
        jpql.append(" from Bill b where b.billType=:bt");
        params.put("bt", BillType.PharmacySnapshotBill);
        if (fromDate != null) {
            jpql.append(" and b.createdAt>=:fd");
            params.put("fd", fromDate);
        }
        if (toDate != null) {
            jpql.append(" and b.createdAt<=:td");
            params.put("td", toDate);
        }
        if (institution != null) {
            jpql.append(" and b.institution=:ins");
            params.put("ins", institution);
        }
        if (department != null) {
            jpql.append(" and b.department=:dep");
            params.put("dep", department);
        }
        jpql.append(" order by b.createdAt desc");

        @SuppressWarnings("unchecked")
        List<com.divudi.core.light.common.PharmacySnapshotBillLight> rows
                = (List<com.divudi.core.light.common.PharmacySnapshotBillLight>) billFacade.findLightsByJpql(jpql.toString(), params, javax.persistence.TemporalType.TIMESTAMP);
        snapshotBillRows = rows == null ? new java.util.ArrayList<>() : rows;
    }

    // Navigate to view the snapshot creation page while keeping context
    public String viewSnapshot(Bill b) {
        if (b == null) {
            return null;
        }
        this.snapshotBill = b;
        this.institution = b.getInstitution();
        this.department = b.getDepartment();
        this.snapshotBill = b;
        return "/pharmacy/pharmacy_stock_take_print?faces-redirect=true";
    }

    // Overload: navigate using id (for DTO rows)
    public String viewSnapshotById(Long billId) {
        if (billId == null) {
            return null;
        }
        Bill b = billFacade.find(billId);
        return viewSnapshot(b);
    }

    // Navigate to upload adjustments page with the selected snapshot
    public String gotoUploadAdjustments(Bill b) {
        if (b == null) {
            return null;
        }
        this.snapshotBill = b;
        this.institution = b.getInstitution();
        this.department = b.getDepartment();
        this.file = null;
        this.physicalCountBill = null;
        return "/pharmacy/pharmacy_stock_take_upload?faces-redirect=true";
    }

    // Overload: navigate using id (for DTO rows)
    public String gotoUploadAdjustmentsById(Long billId) {
        if (billId == null) {
            return null;
        }
        Bill b = billFacade.find(billId);
        return gotoUploadAdjustments(b);
    }

    // Navigate to view variance report for a snapshot
    public String gotoViewVariance(Bill b) {
        if (b == null) {
            return null;
        }
        this.snapshotBill = b;
        this.institution = b.getInstitution();
        this.department = b.getDepartment();
        prepareVarianceRows();
        return "/pharmacy/pharmacy_stock_take_variance?faces-redirect=true";
    }

    // Overload: navigate using id (for DTO rows)
    public String gotoViewVarianceById(Long billId) {
        if (billId == null) {
            return null;
        }
        Bill b = billFacade.find(billId);
        return gotoViewVariance(b);
    }

    // Build aggregated variance rows for the selected snapshot
    private void prepareVarianceRows() {
        varianceRows = new java.util.ArrayList<>();
        if (snapshotBill == null || snapshotBill.getBillItems() == null) {
            return;
        }

        // Map snapshot BillItem id -> row
        java.util.Map<Long, VarianceRow> map = new java.util.HashMap<>();
        for (BillItem snapItem : snapshotBill.getBillItems()) {
            VarianceRow vr = new VarianceRow();
            vr.setSnapshotItem(snapItem);
            vr.setInitialQty(snapItem.getQty() == null ? 0.0 : snapItem.getQty());
            vr.setSumVariance(0.0);
            vr.setLastPhysicalQty(null);
            if (snapItem.getId() != null) {
                map.put(snapItem.getId(), vr);
            }
            varianceRows.add(vr);
        }

        // Fetch all physical count bills referencing this snapshot
        String jpqlBills = "select b from Bill b where b.billType=:bt and b.referenceBill=:rb order by b.createdAt asc, b.id asc";
        HashMap<String, Object> p = new HashMap<>();
        p.put("bt", BillType.PharmacyPhysicalCountBill);
        p.put("rb", snapshotBill);
        List<Bill> physBills = billFacade.findByJpql(jpqlBills, p);
        if (physBills == null || physBills.isEmpty()) {
            return;
        }

        // Load all bill items of those physical bills in one go
        String jpqlItems = "select bi from BillItem bi where bi.bill in :pbs and bi.referanceBillItem is not null";
        HashMap<String, Object> pp = new HashMap<>();
        pp.put("pbs", physBills);
        List<BillItem> physItems = billItemFacade.findByJpql(jpqlItems, pp);
        if (physItems == null) {
            return;
        }
        // Sort by bill createdAt then id to determine the latest per snapshot item
        physItems.sort((a, b2) -> {
            Date da = a.getBill() != null ? a.getBill().getCreatedAt() : null;
            Date db = b2.getBill() != null ? b2.getBill().getCreatedAt() : null;
            int cmp;
            if (da == null && db == null) {
                cmp = 0;
            } else if (da == null) {
                cmp = -1;
            } else if (db == null) {
                cmp = 1;
            } else {
                cmp = da.compareTo(db);
            }
            if (cmp != 0) {
                return cmp;
            }
            Long ia = a.getId();
            Long ib = b2.getId();
            if (ia == null && ib == null) {
                return 0;
            }
            if (ia == null) {
                return -1;
            }
            if (ib == null) {
                return 1;
            }
            return ia.compareTo(ib);
        });

        // Aggregate
        for (BillItem pbi : physItems) {
            BillItem ref = pbi.getReferanceBillItem();
            if (ref == null || ref.getId() == null) {
                continue;
            }
            VarianceRow vr = map.get(ref.getId());
            if (vr == null) {
                continue;
            }
            double var = pbi.getAdjustedValue() == 0 ? 0.0 : pbi.getAdjustedValue();
            vr.setSumVariance(vr.getSumVariance() + var);
            // keep updating; after sorted ascending, last iteration holds latest
            vr.setLastPhysicalQty(pbi.getQty());
        }
    }

    private int findColumnIndex(Row header, String title) {
        if (header == null) {
            return -1;
        }
        short last = header.getLastCellNum();
        for (int i = 0; i < last; i++) {
            if (header.getCell(i) == null) {
                continue;
            }
            try {
                String v = header.getCell(i).getStringCellValue();
                if (v != null && v.trim().equalsIgnoreCase(title)) {
                    return i;
                }
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    private BillItem findSnapshotBillItem(String code, String batch) {
        if (snapshotBill == null) {
            return null;
        }
        for (BillItem bi : snapshotBill.getBillItems()) {
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            if (pbi == null || pbi.getItemBatch() == null) {
                continue;
            }
            if (code.equalsIgnoreCase(pbi.getItemBatch().getItem().getCode())
                    && batch.equalsIgnoreCase(pbi.getItemBatch().getBatchNo())) {
                return bi;
            }
        }
        return null;
    }

    private BillItem findSnapshotBillItemById(long id) {
        if (snapshotBill == null || snapshotBill.getBillItems() == null) {
            return null;
        }
        for (BillItem bi : snapshotBill.getBillItems()) {
            if (bi.getId() != null && bi.getId() == id) {
                return bi;
            }
        }
        return null;
    }

    /**
     * Persist prepared physical count bill.
     */
    public void savePhysicalCount() {
        if (physicalCountBill == null || physicalCountBill.getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No physical counts to save");
            return;
        }

        // Link physical count to snapshot via referenceBill only (no forward link on snapshot)
        if (snapshotBill != null && physicalCountBill.getReferenceBill() == null) {
            physicalCountBill.setReferenceBill(snapshotBill);
        }

        // Save Physical Count Bill (create or update)
        if (physicalCountBill.getId() == null) {
            Department dept = physicalCountBill.getDepartment();
            String deptId = billNumberBean.departmentBillNumberGenerator(dept, BillType.PharmacyPhysicalCountBill, BillClassType.BilledBill, BillNumberSuffix.NONE);
            physicalCountBill.setInsId(deptId);
            physicalCountBill.setDeptId(deptId);
            billFacade.create(physicalCountBill);
        } else {
            billFacade.edit(physicalCountBill);
        }

        // Save Bill Items (create or update) - PharmaceuticalBillItem is cascaded from BillItem
        if (physicalCountBill.getBillItems() != null) {
            Department dept = physicalCountBill.getDepartment();
            for (BillItem bi : physicalCountBill.getBillItems()) {
                if (bi == null) {
                    continue;
                }
                bi.setBill(physicalCountBill);
                PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
                if (pbi != null) {
                    pbi.setBillItem(bi);
                }
                if (bi.getId() == null) {
                    billItemFacade.create(bi);
                } else {
                    billItemFacade.edit(bi);
                }
                // Immediately reset stock to the physical quantity and write StockHistory
                try {
                    BillItem ref = bi.getReferanceBillItem();
                    PharmaceuticalBillItem refPbi = ref != null ? ref.getPharmaceuticalBillItem() : null;
                    Stock stock = refPbi != null ? refPbi.getStock() : null;
                    if (stock != null && pbi != null && dept != null) {
                        double before = stock.getStock();
                        double target = bi.getQty() == null ? before : bi.getQty();
                        pbi.setBeforeAdjustmentValue(before);
                        pbi.setAfterAdjustmentValue(target);
                        pharmacyBean.resetStock(pbi, stock, target, dept);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        JsfUtil.addSuccessMessage("Physical count saved and stock updated");
    }

    public void approvePhysicalCount() {
        if (physicalCountBill == null) {
            JsfUtil.addErrorMessage("No physical count available");
            return;
        }
        if (!webUserController.hasPrivilege(Privileges.PharmacyStockTakeApprove.toString())) {
            JsfUtil.addErrorMessage("Not authorized");
            return;
        }
        Department dept = physicalCountBill.getDepartment();
        Bill adjustmentBill = new Bill();
        adjustmentBill.setBillType(BillType.PharmacyStockAdjustmentBill);
        adjustmentBill.setBillClassType(BillClassType.BilledBill);
        adjustmentBill.setDepartment(dept);
        adjustmentBill.setInstitution(dept.getInstitution());
        Date now = new Date();
        adjustmentBill.setBillDate(now);
        adjustmentBill.setBillTime(now);
        adjustmentBill.setCreatedAt(now);
        adjustmentBill.setCreater(sessionController.getLoggedUser());
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(dept, BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT_BILL);
        adjustmentBill.setDeptId(deptId);
        adjustmentBill.setInsId(deptId);
        adjustmentBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT_BILL);
        adjustmentBill.setFromDepartment(dept);
        adjustmentBill.setFromInstitution(dept.getInstitution());
        adjustmentBill.setToDepartment(null);
        adjustmentBill.setToInstitution(null);
        adjustmentBill.setBackwardReferenceBill(physicalCountBill);
        physicalCountBill.setForwardReferenceBill(adjustmentBill);
        billFacade.create(adjustmentBill);
        for (BillItem bi : physicalCountBill.getBillItems()) {
            double variance = bi.getAdjustedValue();
            if (variance == 0) {
                continue;
            }
            BillItem abi = new BillItem();
            abi.setBill(adjustmentBill);
            abi.setItem(bi.getItem());
            abi.setQty(variance);
            abi.setCreatedAt(now);
            abi.setCreater(sessionController.getLoggedUser());
            abi.setInwardChargeType(InwardChargeType.Medicine);
            // Link adjustment line to its physical count line
            abi.setReferanceBillItem(bi);
            PharmaceuticalBillItem apbi = new PharmaceuticalBillItem();
            apbi.setBillItem(abi);
            apbi.setItemBatch(bi.getPharmaceuticalBillItem().getItemBatch());
            Stock stock = bi.getReferanceBillItem().getPharmaceuticalBillItem().getStock();
            apbi.setStock(stock);
            apbi.setQty(variance);
            // Use before/after captured at save time to avoid double updates
            PharmaceuticalBillItem savedPbi = bi.getPharmaceuticalBillItem();
            if (savedPbi != null) {
                apbi.setBeforeAdjustmentValue(savedPbi.getBeforeAdjustmentValue());
                apbi.setAfterAdjustmentValue(savedPbi.getAfterAdjustmentValue());
            }
            abi.setPharmaceuticalBillItem(apbi);
            // Persist only BillItem; PharmaceuticalBillItem is cascaded
            billItemFacade.create(abi);
            adjustmentBill.getBillItems().add(abi);
            // Stock was updated on savePhysicalCount(); no further stock change here
        }
        physicalCountBill.setApproveUser(sessionController.getLoggedUser());
        physicalCountBill.setApproveAt(new Date());
        billFacade.edit(physicalCountBill);
        billFacade.edit(adjustmentBill);
        JsfUtil.addSuccessMessage("Physical count approved");
    }

    public void rejectPhysicalCount() {
        if (physicalCountBill == null) {
            JsfUtil.addErrorMessage("No physical count available");
            return;
        }
        if (!webUserController.hasPrivilege(Privileges.PharmacyStockTakeApprove.toString())) {
            JsfUtil.addErrorMessage("Not authorized");
            return;
        }
        physicalCountBill.setApproveUser(sessionController.getLoggedUser());
        physicalCountBill.setApproveAt(new Date());
        physicalCountBill.setCancelled(true);
        billFacade.edit(physicalCountBill);
        JsfUtil.addSuccessMessage("Physical count rejected");
    }

    public Bill getPhysicalCountBill() {
        return physicalCountBill;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public Bill getSnapshotBill() {
        return snapshotBill;
    }

    public void setSnapshotBill(Bill snapshotBill) {
        this.snapshotBill = snapshotBill;
    }

    /**
     * Reset the stock taking session to start a new one
     */
    public void resetStockTakingSession() {
        this.snapshotBill = null;
        this.physicalCountBill = null;
        this.file = null;
        JsfUtil.addSuccessMessage("Stock taking session reset");
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

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    // Removed legacy getters/setters for snapshotBills
    public List<VarianceRow> getVarianceRows() {
        return varianceRows;
    }

    // Start asynchronous approval so it completes even if browser closes
    public void startApprovePhysicalCountAsync() {
        if (physicalCountBill == null || physicalCountBill.getBillItems() == null || physicalCountBill.getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No physical count available");
            return;
        }
        if (!webUserController.hasPrivilege(Privileges.PharmacyStockTakeApprove.toString())) {
            JsfUtil.addErrorMessage("Not authorized");
            return;
        }
        String jobId = java.util.UUID.randomUUID().toString();
        this.approvalJobId = jobId;
        approvalProgressTracker.start(jobId, physicalCountBill.getBillItems().size(), "Queued");
        Long approverId = sessionController.getLoggedUser() != null ? sessionController.getLoggedUser().getId() : null;
        stockTakeApprovalService.approvePhysicalCountAsync(physicalCountBill.getId(), approverId, jobId);
        JsfUtil.addSuccessMessage("Approval started in background. You may continue working.");
    }

    public int getApprovalProgressPercent() {
        if (approvalJobId == null) {
            return 0;
        }
        ApprovalProgressTracker.Progress p = approvalProgressTracker.get(approvalJobId);
        if (p == null || p.total <= 0) {
            return 0;
        }
        int percent = (int) Math.round((p.processed * 100.0) / p.total);
        return Math.max(0, Math.min(100, percent));
    }

    public String getApprovalStatusText() {
        if (approvalJobId == null) {
            return "";
        }
        ApprovalProgressTracker.Progress p = approvalProgressTracker.get(approvalJobId);
        if (p == null) {
            return "";
        }
        if (p.failed) {
            return "Failed: " + (p.errorMessage != null ? p.errorMessage : "");
        }
        if (p.completed) {
            return "Completed";
        }
        return p.status != null ? p.status : "Running";
    }

    public boolean isApprovalRunning() {
        if (approvalJobId == null) {
            return false;
        }
        ApprovalProgressTracker.Progress p = approvalProgressTracker.get(approvalJobId);
        return p != null && !p.completed && !p.failed;
    }

    public String getApprovalJobId() {
        return approvalJobId;
    }

    public List<com.divudi.core.light.common.PharmacySnapshotBillLight> getSnapshotBillRows() {
        return snapshotBillRows;
    }

    // DTO for variance report
    public static class VarianceRow implements Serializable {

        private BillItem snapshotItem;
        private Double initialQty;
        private Double sumVariance;
        private Double lastPhysicalQty;

        public BillItem getSnapshotItem() {
            return snapshotItem;
        }

        public void setSnapshotItem(BillItem snapshotItem) {
            this.snapshotItem = snapshotItem;
        }

        public Double getInitialQty() {
            return initialQty;
        }

        public void setInitialQty(Double initialQty) {
            this.initialQty = initialQty;
        }

        public Double getSumVariance() {
            return sumVariance;
        }

        public void setSumVariance(Double sumVariance) {
            this.sumVariance = sumVariance;
        }

        public Double getLastPhysicalQty() {
            return lastPhysicalQty;
        }

        public void setLastPhysicalQty(Double lastPhysicalQty) {
            this.lastPhysicalQty = lastPhysicalQty;
        }

        // Convenience getters for table columns
        public Long getBillItemId() {
            return snapshotItem != null ? snapshotItem.getId() : null;
        }

        public String getCode() {
            try {
                return snapshotItem.getPharmaceuticalBillItem().getItemBatch().getItem().getCode();
            } catch (Exception e) {
                return null;
            }
        }

        public String getItemName() {
            try {
                return snapshotItem.getItem().getName();
            } catch (Exception e) {
                return null;
            }
        }

        public String getBatch() {
            try {
                return snapshotItem.getPharmaceuticalBillItem().getItemBatch().getBatchNo();
            } catch (Exception e) {
                return null;
            }
        }
    }
}
