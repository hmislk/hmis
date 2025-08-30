package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.Privileges;
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
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
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
 * Controller for capturing pharmacy stock snapshots and exporting them to Excel.
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

    private Bill snapshotBill;
    private Bill physicalCountBill;
    private UploadedFile file;
    private Institution institution;
    private Department department;

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
        if (snapshotBill == null || snapshotBill.getBillItems().isEmpty()) {
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
            Cell hId = header.createCell(col++); hId.setCellValue("BillItem ID"); hId.setCellStyle(headerStyle);
            Cell hCode = header.createCell(col++); hCode.setCellValue("Code"); hCode.setCellStyle(headerStyle);
            Cell hName = header.createCell(col++); hName.setCellValue("Name"); hName.setCellStyle(headerStyle);
            Cell hCat  = header.createCell(col++); hCat.setCellValue("Category"); hCat.setCellStyle(headerStyle);
            Cell hBatch= header.createCell(col++); hBatch.setCellValue("Batch"); hBatch.setCellStyle(headerStyle);
            Cell hExp  = header.createCell(col++); hExp.setCellValue("Expiry Date"); hExp.setCellStyle(headerStyle);
            Cell hPR   = header.createCell(col++); hPR.setCellValue("Purchase Rate"); hPR.setCellStyle(headerStyle);
            Cell hRR   = header.createCell(col++); hRR.setCellValue("Retail Rate"); hRR.setCellStyle(headerStyle);
            Cell hCR   = header.createCell(col++); hCR.setCellValue("Cost Rate"); hCR.setCellStyle(headerStyle);
            Integer systemQtyColIndex = null;
            if (includeSystemQty) {
                Cell hSys = header.createCell(col++); hSys.setCellValue("System Qty"); hSys.setCellStyle(headerStyle);
                systemQtyColIndex = col - 1;
            }
            int realQtyColIndex = col; // this will be unlocked for input
            Cell hReal = header.createCell(col++); hReal.setCellValue("Real Stock Qty"); hReal.setCellStyle(headerStyle);
            Cell hLV   = header.createCell(col++); hLV.setCellValue("Line Value"); hLV.setCellStyle(headerStyle);

            // Rows
            int rowNum = 1;
            for (BillItem bi : snapshotBill.getBillItems()) {
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
                Double pr = (ib != null && ib.getPurcahseRate() != null) ? ib.getPurcahseRate() : 0.0;
                Double rr = (ib != null && ib.getRetailsaleRate() != null) ? ib.getRetailsaleRate() : 0.0;
                Double cr = (ib != null && ib.getCostRate() != null) ? ib.getCostRate() : 0.0;

                Cell cPR = row.createCell(c++); cPR.setCellValue(pr); cPR.setCellStyle(numberLocked);
                Cell cRR = row.createCell(c++); cRR.setCellValue(rr); cRR.setCellStyle(numberLocked);
                Cell cCR = row.createCell(c++); cCR.setCellValue(cr); cCR.setCellStyle(numberLocked);

                // System Qty (optional)
                if (includeSystemQty) {
                    Double sys = pbi != null && pbi.getQty() != null ? pbi.getQty() : 0.0;
                    Cell cSys = row.createCell(c++);
                    cSys.setCellValue(sys);
                    cSys.setCellStyle(integerLocked);
                }

                // Real Stock Qty (input - unlocked)
                Cell cReal = row.createCell(c++);
                cReal.setCellStyle(inputUnlocked);

                // Line Value (system = cost rate * system qty)
                Double sysQtyForLV = includeSystemQty ? (pbi != null && pbi.getQty() != null ? pbi.getQty() : 0.0) : 0.0;
                Double lineValue = (cr != null ? cr : 0.0) * sysQtyForLV;
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
                double physical = colRealStock >= 0 ? getDouble(row, colRealStock) : 0.0;

                BillItem snapItem = null;
                if (colBillItemId >= 0) {
                    long bid = (long) getDouble(row, colBillItemId);
                    if (bid > 0) {
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
        Department dept = physicalCountBill.getDepartment();
        String deptId = billNumberBean.departmentBillNumberGenerator(dept, BillType.PharmacyPhysicalCountBill, BillClassType.BilledBill, BillNumberSuffix.NONE);
        physicalCountBill.setInsId(deptId);
        physicalCountBill.setDeptId(deptId);
        physicalCountBill.setBackwardReferenceBill(snapshotBill);
        if (snapshotBill != null) {
            snapshotBill.setForwardReferenceBill(physicalCountBill);
            billFacade.edit(snapshotBill);
        }
        billFacade.create(physicalCountBill);
        for (BillItem bi : physicalCountBill.getBillItems()) {
            billItemFacade.create(bi);
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            if (pbi != null) {
                pharmaceuticalBillItemFacade.create(pbi);
            }
        }
        billFacade.edit(physicalCountBill);
        JsfUtil.addSuccessMessage("Physical count saved");
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
        adjustmentBill.setCreatedAt(new Date());
        adjustmentBill.setCreater(sessionController.getLoggedUser());
        String deptId = billNumberBean.departmentBillNumberGenerator(dept, BillType.PharmacyStockAdjustmentBill, BillClassType.BilledBill, BillNumberSuffix.NONE);
        adjustmentBill.setInsId(deptId);
        adjustmentBill.setDeptId(deptId);
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
            abi.setCreatedAt(new Date());
            abi.setCreater(sessionController.getLoggedUser());
            billItemFacade.create(abi);
            PharmaceuticalBillItem apbi = new PharmaceuticalBillItem();
            apbi.setBillItem(abi);
            apbi.setItemBatch(bi.getPharmaceuticalBillItem().getItemBatch());
            Stock stock = bi.getReferanceBillItem().getPharmaceuticalBillItem().getStock();
            apbi.setStock(stock);
            apbi.setQty(variance);
            pharmaceuticalBillItemFacade.create(apbi);
            abi.setPharmaceuticalBillItem(apbi);
            adjustmentBill.getBillItems().add(abi);
            if (stock != null) {
                stock.setStock(stock.getStock() + variance);
                stockFacade.edit(stock);
            }
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
}
