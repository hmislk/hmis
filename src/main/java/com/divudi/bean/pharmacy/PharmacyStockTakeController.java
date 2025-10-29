package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
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
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.Institution;
import com.divudi.core.data.dto.StockDTO;
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
import com.divudi.service.pharmacy.StockCountGenerationService;
import com.divudi.service.pharmacy.StockCountGenerationTracker;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private static final Logger LOGGER = Logger.getLogger(PharmacyStockTakeController.class.getName());

    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

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
    @EJB
    private StockCountGenerationService stockCountGenerationService;
    @EJB
    private StockCountGenerationTracker stockCountGenerationTracker;
    @EJB
    private com.divudi.core.facade.CategoryFacade categoryFacade;

    private Bill snapshotBill;
    private Bill physicalCountBill;
    private UploadedFile file;
    private Institution institution;
    private Institution site;
    private Department department;
    private Date fromDate;
    private Date toDate;
    // Removed legacy snapshotBills; using DTO rows instead
    // Use Light DTOs via constructor-based JPQL to avoid heavy entity graphs
    private List<com.divudi.core.light.common.PharmacySnapshotBillLight> snapshotBillRows;
    private List<VarianceRow> varianceRows; // aggregated variance report rows
    private String approvalJobId; // background approval job id
    private com.divudi.core.entity.Category selectedCategory; // for category-specific downloads
    // Pending physical count bills
    private List<com.divudi.core.light.common.PharmacyPhysicalCountLight> pendingPhysicalCounts;

    private String comments;
    private boolean printPreview;
    private Bill adjustmentBill; // stores the adjustment bill created during approval for printing

    // Configuration for including zero-stock batches in stock count
    private boolean includeZeroStockBatches;
    private int zeroStockBatchLimit = 5; // default limit of 5 zero-stock batches per item

    // Stock count generation job tracking
    private String generationJobId;

    /**
     * Generate stock count bill preview without persisting.
     */
    public String generateStockCountBill() {
        // Null check for injected dependencies
        if (webUserController == null) {
            JsfUtil.addErrorMessage("System error: Web user controller not available");
            LOGGER.log(Level.SEVERE, "webUserController is null in generateStockCountBill");
            return null;
        }
        if (sessionController == null) {
            JsfUtil.addErrorMessage("System error: Session controller not available");
            LOGGER.log(Level.SEVERE, "sessionController is null in generateStockCountBill");
            return null;
        }
        if (stockFacade == null) {
            JsfUtil.addErrorMessage("System error: Stock facade not available");
            LOGGER.log(Level.SEVERE, "stockFacade is null in generateStockCountBill");
            return null;
        }

        // Check privilege
        if (!webUserController.hasPrivilege(Privileges.PharmacyStockAdjustment.toString())) {
            JsfUtil.addErrorMessage("Not authorized to create stock take snapshots");
            return null;
        }

        // Check department
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return null;
        }

        Department dept = department;

        // Fetch stocks using DTO projection for performance (avoids N+1 queries)
        String jpql = "select new com.divudi.core.data.dto.StockDTO("
                + "s.id, ib.id, i.id, "
                + "c.name, i.name, ib.batchNo, "
                + "ib.dateOfExpire, s.stock, ib.costRate) "
                + "from Stock s "
                + "join s.itemBatch ib "
                + "join ib.item i "
                + "left join i.category c "
                + "where s.department=:d and s.stock>0 "
                + "order by coalesce(c.name, '') asc, "
                + "coalesce(i.name, '') asc, "
                + "coalesce(ib.dateOfExpire, current_date) asc";
        HashMap<String, Object> params = new HashMap<>();
        params.put("d", dept);
        @SuppressWarnings("unchecked")
        List<StockDTO> stockDTOs = (List<StockDTO>) (List<?>) stockFacade.findByJpql(jpql, params);
        if (stockDTOs == null || stockDTOs.isEmpty()) {
            JsfUtil.addErrorMessage("No stock available");
            return null;
        }

        // Fetch all Stock and ItemBatch entities in bulk with JOIN FETCH for bill item creation
        java.util.List<Long> stockIds = stockDTOs.stream()
                .map(StockDTO::getStockId)
                .collect(java.util.stream.Collectors.toList());
        String entityJpql = "select s from Stock s "
                + "join fetch s.itemBatch ib "
                + "join fetch ib.item "
                + "where s.id in :ids";
        HashMap<String, Object> entityParams = new HashMap<>();
        entityParams.put("ids", stockIds);
        List<Stock> stocks = stockFacade.findByJpql(entityJpql, entityParams);

        // Create map for quick lookup
        java.util.Map<Long, Stock> stockMap = new java.util.HashMap<>();
        for (Stock s : stocks) {
            if (s != null) {
                stockMap.put(s.getId(), s);
            }
        }

        // Initialize snapshot bill
        snapshotBill = new Bill();
        snapshotBill.setBillType(BillType.PharmacySnapshotBill);
        snapshotBill.setBillClassType(BillClassType.BilledBill);
        snapshotBill.setDepartment(dept);

        // Null check for institution
        if (dept.getInstitution() != null) {
            snapshotBill.setInstitution(dept.getInstitution());
        }

        snapshotBill.setCreatedAt(new Date());

        // Null check for logged user
        if (sessionController.getLoggedUser() != null) {
            snapshotBill.setCreater(sessionController.getLoggedUser());
        }

        double total = 0.0;
        for (StockDTO dto : stockDTOs) {
            // Null check for DTO
            if (dto == null || dto.getStockId() == null) {
                continue;
            }

            // Get stock entity from map
            Stock s = stockMap.get(dto.getStockId());
            if (s == null || s.getItemBatch() == null || s.getItemBatch().getItem() == null) {
                LOGGER.log(Level.WARNING, "Stock entity not found or incomplete. Stock ID: {0}", dto.getStockId());
                continue;
            }

            ItemBatch itemBatch = s.getItemBatch();

            // Create bill item
            BillItem bi = new BillItem();
            bi.setBill(snapshotBill);
            bi.setItem(itemBatch.getItem());

            // Use DTO data (already fetched, no lazy loading)
            String itemName = dto.getItemName();
            bi.setDescreption(itemName != null ? itemName : "");

            // Set quantity from DTO
            Double stockQty = dto.getStockQty();
            bi.setQty(stockQty != null ? stockQty : 0.0);

            bi.setCreatedAt(new Date());

            // Set creater safely
            if (sessionController.getLoggedUser() != null) {
                bi.setCreater(sessionController.getLoggedUser());
            }

            // Create pharmaceutical bill item
            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
            pbi.setBillItem(bi);
            pbi.setItemBatch(itemBatch);
            pbi.setQty(stockQty != null ? stockQty : 0.0);
            pbi.setStock(s);

            // Set batch number from DTO
            String batchNo = dto.getBatchNo();
            if (batchNo != null) {
                pbi.setStringValue(batchNo);
            }

            // Set cost rate from DTO
            Double costRate = dto.getCostRate();
            double safeCostRate = (costRate != null) ? costRate : 0.0;
            pbi.setCostRate(safeCostRate);

            // Calculate line value
            double safeQty = (bi.getQty() != null) ? bi.getQty() : 0.0;
            double lineValue = safeCostRate * safeQty;

            bi.setNetValue(lineValue);
            total += lineValue;
            bi.setPharmaceuticalBillItem(pbi);

            // Initialize bill items list if null
            if (snapshotBill.getBillItems() == null) {
                snapshotBill.setBillItems(new java.util.ArrayList<>());
            }
            snapshotBill.getBillItems().add(bi);
        }

        // Handle zero-stock batches if configured
        if (includeZeroStockBatches && zeroStockBatchLimit > 0) {
            // Fetch zero-stock batches using DTO projection, ordered by expiry date descending
            String zeroStockJpql = "select new com.divudi.core.data.dto.StockDTO("
                    + "s.id, ib.id, i.id, "
                    + "c.name, i.name, ib.batchNo, "
                    + "ib.dateOfExpire, s.stock, ib.costRate) "
                    + "from Stock s "
                    + "join s.itemBatch ib "
                    + "join ib.item i "
                    + "left join i.category c "
                    + "where s.department=:d and (s.stock is null or s.stock = 0) "
                    + "order by coalesce(c.name, '') asc, "
                    + "coalesce(i.name, '') asc, "
                    + "coalesce(ib.dateOfExpire, current_date) desc";
            @SuppressWarnings("unchecked")
            List<StockDTO> zeroStockDTOs = (List<StockDTO>) (List<?>) stockFacade.findByJpql(zeroStockJpql, params);

            if (zeroStockDTOs != null && !zeroStockDTOs.isEmpty()) {
                // Fetch zero-stock Stock entities in bulk
                java.util.List<Long> zeroStockIds = zeroStockDTOs.stream()
                        .map(StockDTO::getStockId)
                        .collect(java.util.stream.Collectors.toList());
                String zeroEntityJpql = "select s from Stock s "
                        + "join fetch s.itemBatch ib "
                        + "join fetch ib.item "
                        + "where s.id in :ids";
                HashMap<String, Object> zeroEntityParams = new HashMap<>();
                zeroEntityParams.put("ids", zeroStockIds);
                List<Stock> zeroStocks = stockFacade.findByJpql(zeroEntityJpql, zeroEntityParams);

                // Create map for quick lookup
                java.util.Map<Long, Stock> zeroStockMap = new java.util.HashMap<>();
                for (Stock zs : zeroStocks) {
                    if (zs != null) {
                        zeroStockMap.put(zs.getId(), zs);
                    }
                }

                // Group zero-stock batches by item ID and limit per item
                java.util.Map<Long, java.util.List<StockDTO>> zeroStocksByItemId = new java.util.LinkedHashMap<>();
                for (StockDTO dto : zeroStockDTOs) {
                    if (dto == null || dto.getId() == null) {
                        continue;
                    }
                    Long itemId = dto.getId(); // StockDTO.id holds itemId in our constructor
                    zeroStocksByItemId.computeIfAbsent(itemId, k -> new java.util.ArrayList<>()).add(dto);
                }

                // Process zero-stock batches with limit per item
                for (java.util.List<StockDTO> itemZeroStockDTOs : zeroStocksByItemId.values()) {
                    int limit = Math.min(zeroStockBatchLimit, itemZeroStockDTOs.size());

                    for (int i = 0; i < limit; i++) {
                        StockDTO dto = itemZeroStockDTOs.get(i);

                        // Get stock entity from map
                        Stock zs = zeroStockMap.get(dto.getStockId());
                        if (zs == null || zs.getItemBatch() == null || zs.getItemBatch().getItem() == null) {
                            LOGGER.log(Level.WARNING, "Zero-stock entity not found or incomplete. Stock ID: {0}", dto.getStockId());
                            continue;
                        }

                        ItemBatch itemBatch = zs.getItemBatch();

                        // Create bill item for zero-stock batch
                        BillItem bi = new BillItem();
                        bi.setBill(snapshotBill);
                        bi.setItem(itemBatch.getItem());

                        String itemName = dto.getItemName();
                        bi.setDescreption(itemName != null ? itemName : "");
                        bi.setQty(0.0);
                        bi.setCreatedAt(new Date());

                        if (sessionController.getLoggedUser() != null) {
                            bi.setCreater(sessionController.getLoggedUser());
                        }

                        // Create pharmaceutical bill item
                        PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
                        pbi.setBillItem(bi);
                        pbi.setItemBatch(itemBatch);
                        pbi.setQty(0.0);
                        pbi.setStock(zs);

                        String batchNo = dto.getBatchNo();
                        if (batchNo != null) {
                            pbi.setStringValue(batchNo);
                        }

                        Double costRate = dto.getCostRate();
                        double safeCostRate = (costRate != null) ? costRate : 0.0;
                        pbi.setCostRate(safeCostRate);

                        bi.setNetValue(0.0);
                        bi.setPharmaceuticalBillItem(pbi);

                        if (snapshotBill.getBillItems() == null) {
                            snapshotBill.setBillItems(new java.util.ArrayList<>());
                        }
                        snapshotBill.getBillItems().add(bi);
                    }
                }
            }
        }

        snapshotBill.setNetTotal(total);
        JsfUtil.addSuccessMessage("Preview generated. Please review and settle.");
        return "/pharmacy/pharmacy_stock_take_settle?faces-redirect=true";
    }

    /**
     * Start async stock count bill generation with progress tracking.
     * This is the recommended method for large departments to avoid timeouts.
     */
    public String generateStockCountBillAsync() {
        // Check privilege
        if (!webUserController.hasPrivilege(Privileges.PharmacyStockAdjustment.toString())) {
            JsfUtil.addErrorMessage("Not authorized to create stock take snapshots");
            return null;
        }

        // Check department
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return null;
        }

        // Generate unique job ID
        generationJobId = "stock-count-" + department.getId() + "-" + System.currentTimeMillis();

        // Initialize progress tracker
        stockCountGenerationTracker.start(generationJobId, 0, "Starting stock count generation...");

        // Start async generation
        stockCountGenerationService.generateStockCountBillAsync(
            generationJobId,
            department,
            includeZeroStockBatches,
            zeroStockBatchLimit,
            sessionController.getLoggedUser()
        );

        JsfUtil.addSuccessMessage("Stock count generation started. Please wait...");
        return "/pharmacy/pharmacy_stock_take_progress?faces-redirect=true";
    }

    /**
     * Check progress of async stock count generation.
     * Called by polling mechanism on progress page.
     */
    public void checkGenerationProgress() {
        // This method is called by p:poll, no action needed
        // Progress is retrieved via getGenerationProgress()
    }

    /**
     * Get current generation progress.
     * @return Progress object or null if no job in progress
     */
    public StockCountGenerationTracker.Progress getGenerationProgress() {
        if (generationJobId == null) {
            return null;
        }
        return stockCountGenerationTracker.get(generationJobId);
    }

    /**
     * Complete the generation process and load the generated bill.
     * Called when progress indicates completion.
     */
    public String completeGeneration() {
        if (generationJobId == null) {
            JsfUtil.addErrorMessage("No generation job found");
            return null;
        }

        StockCountGenerationTracker.Progress progress = stockCountGenerationTracker.get(generationJobId);

        if (progress == null) {
            JsfUtil.addErrorMessage("Generation progress not found");
            return null;
        }

        if (progress.failed) {
            JsfUtil.addErrorMessage("Generation failed: " + progress.errorMessage);
            stockCountGenerationTracker.remove(generationJobId);
            generationJobId = null;
            return null;
        }

        if (!progress.completed) {
            JsfUtil.addErrorMessage("Generation not yet completed");
            return null;
        }

        // Get the in-memory bill (NOT persisted yet - like sync method)
        snapshotBill = progress.getGeneratedBill();

        if (snapshotBill != null) {
            JsfUtil.addSuccessMessage("Stock count bill generated successfully with " +
                snapshotBill.getBillItems().size() + " items");
            stockCountGenerationTracker.remove(generationJobId);
            generationJobId = null;
            // User can now review and click "Record/Settle Stock Count" to persist
            return "/pharmacy/pharmacy_stock_take_settle?faces-redirect=true";
        }

        JsfUtil.addErrorMessage("Failed to retrieve generated bill");
        return null;
    }

    /**
     * Persist the generated stock count bill and navigate to print view.
     */
    public String settleStockCount() {
        if (!webUserController.hasPrivilege(Privileges.PharmacyStockAdjustment.toString())) {
            JsfUtil.addErrorMessage("Not authorized to settle stock take bills");
            return null;
        }
        if (snapshotBill == null || snapshotBill.getBillItems() == null || snapshotBill.getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Nothing to settle");
            return null;
        }

        // Check if there's an ongoing stock taking for this department
        // Use department from snapshotBill to prevent bypass via mutable controller field
        Department deptFromBill = snapshotBill.getDepartment();
        if (deptFromBill != null && hasOngoingStockTaking(deptFromBill)) {
            JsfUtil.addErrorMessage("Cannot start a new stock taking. There is already an ongoing stock taking session for this department. Please complete the existing session first.");
            LOGGER.log(Level.WARNING, "[StockTake] Attempted to start new stock taking while one is ongoing. Department: {0}", deptFromBill.getName());
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
        // Null check for required parameters
        if (snapshotBill == null) {
            LOGGER.log(Level.WARNING, "Cannot generate sheet. snapshotBill is null");
            return null;
        }

        // Null check for billItemFacade
        if (billItemFacade == null) {
            LOGGER.log(Level.SEVERE, "billItemFacade is null in generateSheet");
            return null;
        }

        // Null check for fileName
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "pharmacy_stock.xlsx";
        }

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Stock");
            if (sheet == null) {
                LOGGER.log(Level.SEVERE, "Failed to create Excel sheet");
                return null;
            }

            // Helpers and formats
            CreationHelper creationHelper = wb.getCreationHelper();
            if (creationHelper == null) {
                LOGGER.log(Level.SEVERE, "Failed to get CreationHelper");
                return null;
            }

            DataFormat dataFormat = wb.createDataFormat();
            if (dataFormat == null) {
                LOGGER.log(Level.SEVERE, "Failed to get DataFormat");
                return null;
            }

            // Styles
            Font headerFont = wb.createFont();
            if (headerFont != null) {
                headerFont.setBold(true);
            }

            CellStyle headerStyle = wb.createCellStyle();
            if (headerStyle != null) {
                if (headerFont != null) {
                    headerStyle.setFont(headerFont);
                }
                headerStyle.setLocked(true);
            }

            CellStyle textLocked = wb.createCellStyle();
            if (textLocked != null) {
                textLocked.setLocked(true);
            }

            CellStyle dateLocked = wb.createCellStyle();
            if (dateLocked != null) {
                dateLocked.setLocked(true);
                dateLocked.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd"));
            }

            CellStyle numberLocked = wb.createCellStyle();
            if (numberLocked != null) {
                numberLocked.setLocked(true);
                numberLocked.setDataFormat(dataFormat.getFormat("#,##0.00"));
            }

            CellStyle integerLocked = wb.createCellStyle();
            if (integerLocked != null) {
                integerLocked.setLocked(true);
                integerLocked.setDataFormat(dataFormat.getFormat("#,##0"));
            }

            CellStyle inputUnlocked = wb.createCellStyle();
            if (inputUnlocked != null) {
                inputUnlocked.setLocked(false);
                inputUnlocked.setDataFormat(dataFormat.getFormat("#,##0.######"));
                inputUnlocked.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
                inputUnlocked.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }

            // Header
            Row header = sheet.createRow(0);
            if (header == null) {
                LOGGER.log(Level.SEVERE, "Failed to create header row");
                return null;
            }

            int col = 0;
            // Always include BillItem ID first for reliable mapping
            Cell hId = header.createCell(col++);
            if (hId != null) {
                hId.setCellValue("BillItem ID");
                if (headerStyle != null) {
                    hId.setCellStyle(headerStyle);
                }
            }

            Cell hCode = header.createCell(col++);
            if (hCode != null) {
                hCode.setCellValue("Code");
                if (headerStyle != null) {
                    hCode.setCellStyle(headerStyle);
                }
            }

            Cell hName = header.createCell(col++);
            if (hName != null) {
                hName.setCellValue("Name");
                if (headerStyle != null) {
                    hName.setCellStyle(headerStyle);
                }
            }

            Cell hCat = header.createCell(col++);
            if (hCat != null) {
                hCat.setCellValue("Category");
                if (headerStyle != null) {
                    hCat.setCellStyle(headerStyle);
                }
            }

            Cell hBatch = header.createCell(col++);
            if (hBatch != null) {
                hBatch.setCellValue("Batch");
                if (headerStyle != null) {
                    hBatch.setCellStyle(headerStyle);
                }
            }

            Cell hExp = header.createCell(col++);
            if (hExp != null) {
                hExp.setCellValue("Expiry Date");
                if (headerStyle != null) {
                    hExp.setCellStyle(headerStyle);
                }
            }

            Cell hPR = header.createCell(col++);
            if (hPR != null) {
                hPR.setCellValue("Purchase Rate");
                if (headerStyle != null) {
                    hPR.setCellStyle(headerStyle);
                }
            }

            Cell hRR = header.createCell(col++);
            if (hRR != null) {
                hRR.setCellValue("Retail Rate");
                if (headerStyle != null) {
                    hRR.setCellStyle(headerStyle);
                }
            }

            Cell hCR = header.createCell(col++);
            if (hCR != null) {
                hCR.setCellValue("Cost Rate");
                if (headerStyle != null) {
                    hCR.setCellStyle(headerStyle);
                }
            }

            Integer systemQtyColIndex = null;
            if (includeSystemQty) {
                Cell hSys = header.createCell(col++);
                if (hSys != null) {
                    hSys.setCellValue("System Qty");
                    if (headerStyle != null) {
                        hSys.setCellStyle(headerStyle);
                    }
                }
                systemQtyColIndex = col - 1;
            }

            int realQtyColIndex = col; // this will be unlocked for input
            Cell hReal = header.createCell(col++);
            if (hReal != null) {
                hReal.setCellValue("Real Stock Qty");
                if (headerStyle != null) {
                    hReal.setCellStyle(headerStyle);
                }
            }

            Cell hLV = header.createCell(col++);
            if (hLV != null) {
                hLV.setCellValue("Line Value");
                if (headerStyle != null) {
                    hLV.setCellStyle(headerStyle);
                }
            }

            // Resolve items (avoid lazy issues on detached entity)
            List<BillItem> items = snapshotBill.getBillItems();
            if (items == null || items.isEmpty()) {
                if (snapshotBill.getId() != null) {
                    HashMap<String, Object> p = new HashMap<>();
                    p.put("b", snapshotBill);
                    items = billItemFacade.findByJpql("select bi from BillItem bi where bi.bill=:b order by bi.id", p);
                }
            }

            // Ensure items is not null
            if (items == null) {
                items = java.util.Collections.emptyList();
            }

            // Rows
            int rowNum = 1;
            for (BillItem bi : items) {
                // Null check for bill item
                if (bi == null) {
                    continue;
                }

                PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
                ItemBatch ib = pbi != null ? pbi.getItemBatch() : null;
                Row row = sheet.createRow(rowNum++);
                if (row == null) {
                    continue;
                }

                int c = 0;

                // BillItem ID
                Cell cId = row.createCell(c++);
                if (cId != null) {
                    if (bi.getId() != null) {
                        cId.setCellValue(bi.getId());
                    } else {
                        cId.setCellValue(0);
                    }
                    if (integerLocked != null) {
                        cId.setCellStyle(integerLocked);
                    }
                }

                // Code
                Cell cCode = row.createCell(c++);
                if (cCode != null) {
                    cCode.setCellValue(ib != null && ib.getItem() != null && ib.getItem().getCode() != null ? ib.getItem().getCode() : "");
                    if (textLocked != null) {
                        cCode.setCellStyle(textLocked);
                    }
                }

                // Name
                Cell cName = row.createCell(c++);
                if (cName != null) {
                    cName.setCellValue(ib != null && ib.getItem() != null && ib.getItem().getName() != null ? ib.getItem().getName() : (bi.getDescreption() != null ? bi.getDescreption() : ""));
                    if (textLocked != null) {
                        cName.setCellStyle(textLocked);
                    }
                }

                // Category
                Cell cCat = row.createCell(c++);
                if (cCat != null) {
                    cCat.setCellValue(bi.getItem() != null && bi.getItem().getCategory() != null && bi.getItem().getCategory().getName() != null ? bi.getItem().getCategory().getName() : "");
                    if (textLocked != null) {
                        cCat.setCellStyle(textLocked);
                    }
                }

                // Batch
                Cell cBatch = row.createCell(c++);
                if (cBatch != null) {
                    cBatch.setCellValue(ib != null && ib.getBatchNo() != null ? ib.getBatchNo() : "");
                    if (textLocked != null) {
                        cBatch.setCellStyle(textLocked);
                    }
                }

                // Expiry
                Cell cExp = row.createCell(c++);
                if (cExp != null) {
                    if (ib != null && ib.getDateOfExpire() != null) {
                        cExp.setCellValue(ib.getDateOfExpire());
                        if (dateLocked != null) {
                            cExp.setCellStyle(dateLocked);
                        }
                    } else {
                        cExp.setCellValue("");
                        if (textLocked != null) {
                            cExp.setCellStyle(textLocked);
                        }
                    }
                }

                // Rates - handle null returns from getter methods
                double pr = 0.0;
                double rr = 0.0;
                double cr = 0.0;

                if (ib != null) {
                    Double prObj = ib.getPurcahseRate();
                    pr = (prObj != null) ? prObj : 0.0;

                    Double rrObj = ib.getRetailsaleRate();
                    rr = (rrObj != null) ? rrObj : 0.0;

                    Double crObj = ib.getCostRate();
                    cr = (crObj != null) ? crObj : 0.0;
                }

                Cell cPR = row.createCell(c++);
                if (cPR != null) {
                    cPR.setCellValue(pr);
                    if (numberLocked != null) {
                        cPR.setCellStyle(numberLocked);
                    }
                }

                Cell cRR = row.createCell(c++);
                if (cRR != null) {
                    cRR.setCellValue(rr);
                    if (numberLocked != null) {
                        cRR.setCellStyle(numberLocked);
                    }
                }

                Cell cCR = row.createCell(c++);
                if (cCR != null) {
                    cCR.setCellValue(cr);
                    if (numberLocked != null) {
                        cCR.setCellStyle(numberLocked);
                    }
                }

                // System Qty (optional) - handle null return from pbi.getQty()
                if (includeSystemQty) {
                    double sys = 0.0;
                    if (pbi != null) {
                        Double sysObj = pbi.getQty();
                        sys = (sysObj != null) ? sysObj : 0.0;
                    }
                    Cell cSys = row.createCell(c++);
                    if (cSys != null) {
                        cSys.setCellValue(sys);
                        if (integerLocked != null) {
                            cSys.setCellStyle(integerLocked);
                        }
                    }
                }

                // Real Stock Qty (input - unlocked)
                Cell cReal = row.createCell(c++);
                if (cReal != null && inputUnlocked != null) {
                    cReal.setCellStyle(inputUnlocked);
                }

                // Line Value (system = cost rate * system qty) - handle null return from pbi.getQty()
                double sysQtyForLV = 0.0;
                if (includeSystemQty && pbi != null) {
                    Double qtyObj = pbi.getQty();
                    sysQtyForLV = (qtyObj != null) ? qtyObj : 0.0;
                }
                double lineValue = cr * sysQtyForLV;
                Cell cLV = row.createCell(c++);
                if (cLV != null) {
                    cLV.setCellValue(lineValue);
                    if (numberLocked != null) {
                        cLV.setCellStyle(numberLocked);
                    }
                }
            }

            // Autosize columns
            int totalCols = header.getLastCellNum();
            for (int i = 0; i < totalCols; i++) {
                try {
                    sheet.autoSizeColumn(i);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to autosize column " + i, e);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            byte[] bytes = out.toByteArray();
            if (bytes == null || bytes.length == 0) {
                LOGGER.log(Level.SEVERE, "Failed to generate Excel file bytes");
                return null;
            }

            InputStream in = new ByteArrayInputStream(bytes);
            return DefaultStreamedContent.builder()
                    .name(fileName)
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .stream(() -> in)
                    .build();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error generating sheet", e);
            JsfUtil.addErrorMessage("Error generating sheet: " + e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error generating sheet", e);
            JsfUtil.addErrorMessage("Unexpected error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse uploaded sheet and prepare physical count bill with variances.
     */
    public void parseUploadedSheet() {
        LOGGER.log(Level.INFO, "[StockTake] parseUploadedSheet() called. snapshotBillId={0}, fileName={1}",
                new Object[]{snapshotBill != null ? snapshotBill.getId() : null, file != null ? file.getFileName() : null});
        if (snapshotBill == null) {
            JsfUtil.addErrorMessage("No snapshot available");
            LOGGER.log(Level.WARNING, "[StockTake] Parse aborted. snapshotBill is null");
            return;
        }

        // Check if the stock taking is already completed
        if (snapshotBill.isCompleted()) {
            JsfUtil.addErrorMessage("Cannot upload to a completed stock taking session");
            LOGGER.log(Level.WARNING, "[StockTake] Parse aborted. Stock taking already completed. billId={0}", snapshotBill.getId());
            return;
        }
        if (file == null) {
            JsfUtil.addErrorMessage("No file uploaded");
            LOGGER.log(Level.WARNING, "[StockTake] Parse aborted. Uploaded file is null");
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
            LOGGER.log(Level.INFO, "[StockTake] Header columns detected. BillItemID={0}, Code={1}, Batch={2}, RealStock={3}",
                    new Object[]{colBillItemId, colCode, colBatch, colRealStock});
// There are systamatically validated below
//            if (colRealStock < 0) {
//                // User 
//                JsfUtil.addErrorMessage("Column 'Real Stock Qty' not found in the uploaded file. Please use the exported template and fill quantities.");
//                LOGGER.log(Level.WARNING, "[StockTake] Missing required column: Real Stock Qty");
//                return;
//            }
//            if (colBillItemId < 0 && (colCode < 0 || colBatch < 0)) {
//                JsfUtil.addErrorMessage("Unable to identify items. Ensure either 'BillItem ID' or both 'Code' and 'Batch' columns exist.");
//                LOGGER.log(Level.WARNING, "[StockTake] Unable to match items. Missing BillItem ID and/or Code+Batch");
//                return;
//            }

            int processed = 0;
            int matched = 0;
            int skippedNoQty = 0;
            int skippedNoMatch = 0;
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
                    skippedNoQty++;
                    continue;
                }
                double physical = physicalObj;
                processed++;

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
                    skippedNoMatch++;
                    continue;
                }
                matched++;
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
            LOGGER.log(Level.INFO, "[StockTake] Parse completed. processedRows={0}, matchedItems={1}, skippedNoQty={2}, skippedNoMatch={3}",
                    new Object[]{processed, matched, skippedNoQty, skippedNoMatch});
            if (matched == 0) {
                JsfUtil.addErrorMessage("No valid rows found. Ensure the file is from the system template and the 'Real Stock Qty' column has values.");
            }
        } catch (IOException e) {
            JsfUtil.addErrorMessage(e, "Error processing file");
            LOGGER.log(Level.SEVERE, "[StockTake] Exception while parsing uploaded file", e);
            physicalCountBill = null;
        }
    }

    /**
     * Parse, persist a Physical Count bill, and navigate to review page.
     */
    public String parseAndPersistNavigate() {
        LOGGER.log(Level.INFO, "[StockTake] parseAndPersistNavigate() invoked");
        if (!webUserController.hasPrivilege(Privileges.Pharmacy.toString())) {
            JsfUtil.addErrorMessage("Not authorized to upload/save physical count data");
            LOGGER.log(Level.WARNING, "[StockTake] User missing privilege 'Pharmacy' for upload/save");
            return null;
        }
        parseUploadedSheet();
        if (physicalCountBill == null || physicalCountBill.getBillItems() == null || physicalCountBill.getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Could not process the upload. Please verify the file and try again.");
            LOGGER.log(Level.WARNING, "[StockTake] Persist aborted. physicalCountBill is null or no items parsed");
            return null;
        }
        if (physicalCountBill.getId() == null) {
            Department dept = physicalCountBill.getDepartment();
            String deptId = billNumberBean.departmentBillNumberGenerator(dept, BillType.PharmacyPhysicalCountBill, BillClassType.BilledBill, BillNumberSuffix.NONE);
            physicalCountBill.setInsId(deptId);
            physicalCountBill.setDeptId(deptId);
            billFacade.create(physicalCountBill);
            LOGGER.log(Level.INFO, "[StockTake] Created PhysicalCount bill on parse. id={0}, items={1}",
                    new Object[]{physicalCountBill.getId(), physicalCountBill.getBillItems().size()});
        } else {
            billFacade.edit(physicalCountBill);
            LOGGER.log(Level.INFO, "[StockTake] Updated PhysicalCount bill on parse. id={0}, items={1}",
                    new Object[]{physicalCountBill.getId(), physicalCountBill.getBillItems().size()});
        }
        // Persist items
        for (BillItem bi : physicalCountBill.getBillItems()) {
            if (bi == null) {
                continue;
            }
            bi.setBill(physicalCountBill);
            if (bi.getPharmaceuticalBillItem() != null) {
                bi.getPharmaceuticalBillItem().setBillItem(bi);
            }
            if (bi.getId() == null) {
                billItemFacade.create(bi);
            } else {
                billItemFacade.edit(bi);
            }
        }
        JsfUtil.addSuccessMessage("Upload processed successfully. Items parsed: " + physicalCountBill.getBillItems().size());
        return "/pharmacy/pharmacy_stock_take_review?faces-redirect=true";
    }

    public void listPendingPhysicalCounts() {
        java.util.HashMap<String, Object> params = new java.util.HashMap<>();
        StringBuilder j = new StringBuilder();
        j.append("select new com.divudi.core.light.common.PharmacyPhysicalCountLight(");
        j.append(" b.id, b.deptId, b.createdAt, b.institution.name, b.department.name,");
        j.append(" (select count(bi) from BillItem bi where bi.bill=b) ) ");
        j.append(" from Bill b where b.billType=:bt and b.approveAt is null");
        params.put("bt", BillType.PharmacyPhysicalCountBill);
        j.append(" order by b.createdAt desc");
        @SuppressWarnings("unchecked")
        List<com.divudi.core.light.common.PharmacyPhysicalCountLight> rows
                = (List<com.divudi.core.light.common.PharmacyPhysicalCountLight>) billFacade.findLightsByJpql(j.toString(), params, javax.persistence.TemporalType.TIMESTAMP);
        pendingPhysicalCounts = rows == null ? new java.util.ArrayList<>() : rows;
    }

    public String viewPhysicalCountById(Long billId) {
        if (billId == null) {
            return null;
        }
        Bill b = billFacade.find(billId);
        if (b == null) {
            return null;
        }
        this.physicalCountBill = b;
        this.snapshotBill = b.getReferenceBill();
        this.institution = b.getInstitution();
        this.department = b.getDepartment();
        return "/pharmacy/pharmacy_stock_take_review?faces-redirect=true";
    }

    public List<com.divudi.core.light.common.PharmacyPhysicalCountLight> getPendingPhysicalCounts() {
        return pendingPhysicalCounts;
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
        jpql.append(" (select count(bi) from BillItem bi where bi.bill = b), b.netTotal, b.completed ) ");
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
        if (site != null) {
            jpql.append(" and b.department.site=:site");
            params.put("site", site);
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

        // Check if the stock taking is already completed
        if (b.isCompleted()) {
            JsfUtil.addErrorMessage("Cannot upload to a completed stock taking session. This stock taking has already been closed.");
            LOGGER.log(Level.WARNING, "[StockTake] Attempted to upload to completed stock taking. billId={0}", b.getId());
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
     * Check if there's an ongoing (incomplete) stock taking for the given
     * department. An ongoing stock taking is one where bill.completed = false.
     */
    private boolean hasOngoingStockTaking(Department dept) {
        if (dept == null) {
            return false;
        }
        String jpql = "select count(b) from Bill b where b.billType=:bt and b.department=:dept and (b.completed is null or b.completed=false)";
        HashMap<String, Object> params = new HashMap<>();
        params.put("bt", BillType.PharmacySnapshotBill);
        params.put("dept", dept);
        Long count = billFacade.countByJpql(jpql, params);
        return count != null && count > 0;
    }

    /**
     * Complete/close the current stock taking session. This marks the snapshot
     * bill as completed.
     */
    public void completeStockTaking() {
        LOGGER.log(Level.INFO, "[StockTake] completeStockTaking() called. snapshotBillId={0}",
                new Object[]{snapshotBill != null ? snapshotBill.getId() : null});

        if (snapshotBill == null) {
            JsfUtil.addErrorMessage("No stock taking session to complete");
            LOGGER.log(Level.WARNING, "[StockTake] Complete failed. snapshotBill is null");
            return;
        }

        if (!webUserController.hasPrivilege(Privileges.PharmacyStockAdjustment.toString())) {
            JsfUtil.addErrorMessage("Not authorized to complete stock taking");
            LOGGER.log(Level.WARNING, "[StockTake] Complete failed. User lacks privilege");
            return;
        }

        // Mark the bill as completed
        snapshotBill.setCompleted(true);
        snapshotBill.setCompletedAt(new Date());
        snapshotBill.setCompletedBy(sessionController.getLoggedUser());
        billFacade.edit(snapshotBill);

        LOGGER.log(Level.INFO, "[StockTake] Stock taking completed. billId={0}, department={1}",
                new Object[]{snapshotBill.getId(), snapshotBill.getDepartment().getName()});

        JsfUtil.addSuccessMessage("Stock taking session completed successfully");
    }

    /**
     * Persist prepared physical count bill.
     */
    public void savePhysicalCount() {
        LOGGER.log(Level.INFO, "[StockTake] savePhysicalCount() called. snapshotBillId={0}, physicalCountId={1}",
                new Object[]{snapshotBill != null ? snapshotBill.getId() : null, physicalCountBill != null ? physicalCountBill.getId() : null});
        if (physicalCountBill == null || physicalCountBill.getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No physical counts to save");
            LOGGER.log(Level.WARNING, "[StockTake] No physical counts to save. physicalCountBill is null or items empty");
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
            LOGGER.log(Level.INFO, "[StockTake] Created PhysicalCount bill. id={0}, deptId={1}, items={2}",
                    new Object[]{physicalCountBill.getId(), deptId, physicalCountBill.getBillItems() != null ? physicalCountBill.getBillItems().size() : 0});
        } else {
            billFacade.edit(physicalCountBill);
            LOGGER.log(Level.INFO, "[StockTake] Updated PhysicalCount bill. id={0}, items={1}",
                    new Object[]{physicalCountBill.getId(), physicalCountBill.getBillItems() != null ? physicalCountBill.getBillItems().size() : 0});
        }

        // Save Bill Items (create or update) - PharmaceuticalBillItem is cascaded from BillItem
        if (physicalCountBill.getBillItems() != null) {
            int saved = 0;
            for (BillItem bi : physicalCountBill.getBillItems()) {
                if (bi == null) {
                    continue;
                }
                bi.setBill(physicalCountBill);
                if (bi.getPharmaceuticalBillItem() != null) {
                    bi.getPharmaceuticalBillItem().setBillItem(bi);
                }
                if (bi.getId() == null) {
                    billItemFacade.create(bi);
                } else {
                    billItemFacade.edit(bi);
                }
                saved++;
                LOGGER.log(Level.FINE, "[StockTake] Saved PC BillItem. pcBillId={0}, pcItemId={1}, snapRefItemId={2}, qty={3}",
                        new Object[]{physicalCountBill.getId(), bi.getId(), bi.getReferanceBillItem() != null ? bi.getReferanceBillItem().getId() : null, bi.getQty()});
            }
            LOGGER.log(Level.INFO, "[StockTake] Saved PhysicalCount items. count={0}", saved);
        }

        JsfUtil.addSuccessMessage("Physical count saved");
    }

    public void approvePhysicalCount() {
        LOGGER.log(Level.INFO, "[StockTake] approvePhysicalCount() called. pcBillId={0}", new Object[]{physicalCountBill != null ? physicalCountBill.getId() : null});
        if (physicalCountBill == null) {
            JsfUtil.addErrorMessage("No physical count available");
            LOGGER.log(Level.WARNING, "[StockTake] Approve failed. physicalCountBill is null");
            return;
        }
        if (!webUserController.hasPrivilege(Privileges.PharmacyStockTakeApprove.toString())) {
            JsfUtil.addErrorMessage("Not authorized");
            LOGGER.log(Level.WARNING, "[StockTake] Approve failed. User lacks privilege PharmacyStockTakeApprove");
            return;
        }
        Department dept = physicalCountBill.getDepartment();
        this.adjustmentBill = new Bill();
        this.adjustmentBill.setBillType(BillType.PharmacyStockAdjustmentBill);
        adjustmentBill.setBillClassType(BillClassType.BilledBill);
        adjustmentBill.setComments(comments);
        adjustmentBill.setDepartment(dept);
        adjustmentBill.setInstitution(dept.getInstitution());
        Date now = new Date();
        adjustmentBill.setBillDate(now);
        adjustmentBill.setBillTime(now);
        adjustmentBill.setCreatedAt(now);
        adjustmentBill.setCreater(sessionController.getLoggedUser());
        adjustmentBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT_BILL);

        // Generate deptId and insId using configurable bill number generation strategy
        boolean billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Stock Adjustments - Prefix + Department Code + Institution Code + Year + Yearly Number and Yearly Number", false);
        boolean billNumberGenerationStrategyForDepartmentIdIsPrefixInsDeptYearCount = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Stock Adjustments - Prefix + Institution Code + Department Code + Year + Yearly Number and Yearly Number", false);
        boolean billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Stock Adjustments - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);
        boolean billNumberGenerationStrategyForInstitutionIdIsPrefixInsYearCount = configOptionApplicationController.getBooleanValueByKey("Institution Number Generation Strategy for Stock Adjustments - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);

        String billId = "";

        if (billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount) {
            if (adjustmentBill.getDeptId() == null || adjustmentBill.getDeptId().trim().equals("")) {
                billId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(dept, BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT_BILL);
                adjustmentBill.setDeptId(billId);
            }
        } else if (billNumberGenerationStrategyForDepartmentIdIsPrefixInsDeptYearCount) {
            if (adjustmentBill.getDeptId() == null || adjustmentBill.getDeptId().trim().equals("")) {
                billId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(dept, BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT_BILL);
                adjustmentBill.setDeptId(billId);
            }
        } else if (billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount) {
            if (adjustmentBill.getDeptId() == null || adjustmentBill.getDeptId().trim().equals("")) {
                billId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(dept, BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT_BILL);
                adjustmentBill.setDeptId(billId);
            }
        } else {
            //Keep Legacy Method intact without any changes
            if (adjustmentBill.getDeptId() == null || adjustmentBill.getDeptId().trim().equals("")) {
                billId = billNumberBean.departmentBillNumberGeneratorYearly(dept, BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT_BILL);
                adjustmentBill.setDeptId(billId);
            }
        }

        if (billNumberGenerationStrategyForInstitutionIdIsPrefixInsYearCount) {
            if (adjustmentBill.getInsId() == null || adjustmentBill.getInsId().trim().equals("")) {
                String insId = billNumberBean.institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(dept, BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT_BILL);
                adjustmentBill.setInsId(insId);
            }
        } else {
            //Keep Legacy Method intact without any changes
            if (adjustmentBill.getInsId() == null || adjustmentBill.getInsId().trim().equals("")) {
                if (billId != null && !billId.trim().isEmpty()) {
                    adjustmentBill.setInsId(billId);
                }
            }
        }
        adjustmentBill.setFromDepartment(dept);
        adjustmentBill.setFromInstitution(dept.getInstitution());
        adjustmentBill.setToDepartment(null);
        adjustmentBill.setToInstitution(null);
        adjustmentBill.setBackwardReferenceBill(physicalCountBill);
        physicalCountBill.setForwardReferenceBill(adjustmentBill);
        billFacade.create(adjustmentBill);
        LOGGER.log(Level.INFO, "[StockTake] Created Adjustment bill. id={0}, deptId={1}", new Object[]{adjustmentBill.getId(), adjustmentBill.getDeptId()});
        for (BillItem bi : physicalCountBill.getBillItems()) {
            double variance = bi.getAdjustedValue();
            if (variance == 0) {
                LOGGER.log(Level.FINE, "[StockTake] Skipping zero variance. refItemId={0}", new Object[]{bi.getReferanceBillItem() != null ? bi.getReferanceBillItem().getId() : null});
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
            if (stock != null) {
                double before = stock.getStock();
                double target = bi.getQty() == null ? before : bi.getQty();
                apbi.setBeforeAdjustmentValue(before);
                apbi.setAfterAdjustmentValue(target);
            }
            abi.setPharmaceuticalBillItem(apbi);
            // Persist only BillItem; PharmaceuticalBillItem is cascaded
            billItemFacade.create(abi);
            adjustmentBill.getBillItems().add(abi);
            // Update stock via PharmacyBean to ensure StockHistory is recorded at approval
            if (stock != null) {
                double targetQty = apbi.getAfterAdjustmentValue();
                boolean ok = pharmacyBean.resetStock(apbi, stock, targetQty, dept);
                LOGGER.log(Level.INFO, "[StockTake] Posted adjustment line. adjItemId={0}, refItemId={1}, stockId={2}, before={3}, after={4}, variance={5}, resetOk={6}",
                        new Object[]{abi.getId(), bi.getId(), stock.getId(), apbi.getBeforeAdjustmentValue(), apbi.getAfterAdjustmentValue(), variance, ok});
            } else {
                LOGGER.log(Level.WARNING, "[StockTake] No stock linked to snapshot item. refItemId={0}", new Object[]{bi.getReferanceBillItem() != null ? bi.getReferanceBillItem().getId() : null});
            }
        }
        physicalCountBill.setApproveUser(sessionController.getLoggedUser());
        physicalCountBill.setApproveAt(new Date());
        billFacade.edit(physicalCountBill);
        billFacade.edit(adjustmentBill);
        LOGGER.log(Level.INFO, "[StockTake] Approval completed. pcBillId={0}, adjBillId={1}, adjItems={2}",
                new Object[]{physicalCountBill.getId(), adjustmentBill.getId(), adjustmentBill.getBillItems() != null ? adjustmentBill.getBillItems().size() : 0});

        // Set printPreview to true to show the print section
        this.printPreview = true;

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
        this.printPreview = false;
        this.adjustmentBill = null;
        JsfUtil.addSuccessMessage("Stock taking session reset");
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
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

    public com.divudi.core.entity.Category getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(com.divudi.core.entity.Category selectedCategory) {
        this.selectedCategory = selectedCategory;
    }

    public List<com.divudi.core.entity.Category> completeCategory(String query) {
        HashMap<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder("select c from Category c where c.retired=false");

        if (query != null && !query.trim().isEmpty()) {
            jpql.append(" and upper(c.name) like :q");
            params.put("q", "%" + query.trim().toUpperCase() + "%");
        }

        jpql.append(" order by c.name");
        return categoryFacade.findByJpql(jpql.toString(), params, 20);
    }

    public StreamedContent downloadCategoryGuidedSheet() {
        if (selectedCategory == null) {
            JsfUtil.addErrorMessage("Please select a category first");
            return null;
        }
        return generateCategorySheet(true, "pharmacy_stock_guided_" + selectedCategory.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".xlsx");
    }

    public StreamedContent downloadCategoryBlindSheet() {
        if (selectedCategory == null) {
            JsfUtil.addErrorMessage("Please select a category first");
            return null;
        }
        return generateCategorySheet(false, "pharmacy_stock_blind_" + selectedCategory.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".xlsx");
    }

    public StreamedContent getDownloadCategoryGuidedSheet() {
        return downloadCategoryGuidedSheet();
    }

    public StreamedContent getDownloadCategoryBlindSheet() {
        return downloadCategoryBlindSheet();
    }

    private StreamedContent generateCategorySheet(boolean includeSystemQty, String fileName) {
        // Null check for required parameters
        if (snapshotBill == null || selectedCategory == null) {
            LOGGER.log(Level.WARNING, "Cannot generate category sheet. snapshotBill or selectedCategory is null");
            return null;
        }

        // Null check for billItemFacade
        if (billItemFacade == null) {
            LOGGER.log(Level.SEVERE, "billItemFacade is null in generateCategorySheet");
            return null;
        }

        // Null check for fileName
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "pharmacy_stock_category.xlsx";
        }

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Stock");
            if (sheet == null) {
                LOGGER.log(Level.SEVERE, "Failed to create Excel sheet");
                return null;
            }

            // Helpers and formats
            CreationHelper creationHelper = wb.getCreationHelper();
            if (creationHelper == null) {
                LOGGER.log(Level.SEVERE, "Failed to get CreationHelper");
                return null;
            }

            DataFormat dataFormat = wb.createDataFormat();
            if (dataFormat == null) {
                LOGGER.log(Level.SEVERE, "Failed to get DataFormat");
                return null;
            }

            // Styles
            Font headerFont = wb.createFont();
            if (headerFont != null) {
                headerFont.setBold(true);
            }

            CellStyle headerStyle = wb.createCellStyle();
            if (headerStyle != null) {
                if (headerFont != null) {
                    headerStyle.setFont(headerFont);
                }
                headerStyle.setLocked(true);
            }

            CellStyle textLocked = wb.createCellStyle();
            if (textLocked != null) {
                textLocked.setLocked(true);
            }

            CellStyle dateLocked = wb.createCellStyle();
            if (dateLocked != null) {
                dateLocked.setLocked(true);
                dateLocked.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd"));
            }

            CellStyle numberLocked = wb.createCellStyle();
            if (numberLocked != null) {
                numberLocked.setLocked(true);
                numberLocked.setDataFormat(dataFormat.getFormat("#,##0.00"));
            }

            CellStyle integerLocked = wb.createCellStyle();
            if (integerLocked != null) {
                integerLocked.setLocked(true);
                integerLocked.setDataFormat(dataFormat.getFormat("#,##0"));
            }

            CellStyle inputUnlocked = wb.createCellStyle();
            if (inputUnlocked != null) {
                inputUnlocked.setLocked(false);
                inputUnlocked.setDataFormat(dataFormat.getFormat("#,##0.######"));
                inputUnlocked.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
                inputUnlocked.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }

            // Header
            Row header = sheet.createRow(0);
            if (header == null) {
                LOGGER.log(Level.SEVERE, "Failed to create header row");
                return null;
            }

            int col = 0;
            // Always include BillItem ID first for reliable mapping
            Cell hId = header.createCell(col++);
            if (hId != null) {
                hId.setCellValue("BillItem ID");
                if (headerStyle != null) {
                    hId.setCellStyle(headerStyle);
                }
            }

            Cell hCode = header.createCell(col++);
            if (hCode != null) {
                hCode.setCellValue("Code");
                if (headerStyle != null) {
                    hCode.setCellStyle(headerStyle);
                }
            }

            Cell hName = header.createCell(col++);
            if (hName != null) {
                hName.setCellValue("Name");
                if (headerStyle != null) {
                    hName.setCellStyle(headerStyle);
                }
            }

            Cell hCat = header.createCell(col++);
            if (hCat != null) {
                hCat.setCellValue("Category");
                if (headerStyle != null) {
                    hCat.setCellStyle(headerStyle);
                }
            }

            Cell hBatch = header.createCell(col++);
            if (hBatch != null) {
                hBatch.setCellValue("Batch");
                if (headerStyle != null) {
                    hBatch.setCellStyle(headerStyle);
                }
            }

            Cell hExp = header.createCell(col++);
            if (hExp != null) {
                hExp.setCellValue("Expiry Date");
                if (headerStyle != null) {
                    hExp.setCellStyle(headerStyle);
                }
            }

            Cell hPR = header.createCell(col++);
            if (hPR != null) {
                hPR.setCellValue("Purchase Rate");
                if (headerStyle != null) {
                    hPR.setCellStyle(headerStyle);
                }
            }

            Cell hRR = header.createCell(col++);
            if (hRR != null) {
                hRR.setCellValue("Retail Rate");
                if (headerStyle != null) {
                    hRR.setCellStyle(headerStyle);
                }
            }

            Cell hCR = header.createCell(col++);
            if (hCR != null) {
                hCR.setCellValue("Cost Rate");
                if (headerStyle != null) {
                    hCR.setCellStyle(headerStyle);
                }
            }

            Integer systemQtyColIndex = null;
            if (includeSystemQty) {
                Cell hSys = header.createCell(col++);
                if (hSys != null) {
                    hSys.setCellValue("System Qty");
                    if (headerStyle != null) {
                        hSys.setCellStyle(headerStyle);
                    }
                }
                systemQtyColIndex = col - 1;
            }

            int realQtyColIndex = col; // this will be unlocked for input
            Cell hReal = header.createCell(col++);
            if (hReal != null) {
                hReal.setCellValue("Real Stock Qty");
                if (headerStyle != null) {
                    hReal.setCellStyle(headerStyle);
                }
            }

            Cell hLV = header.createCell(col++);
            if (hLV != null) {
                hLV.setCellValue("Line Value");
                if (headerStyle != null) {
                    hLV.setCellStyle(headerStyle);
                }
            }

            // Get items filtered by category
            List<BillItem> items = null;
            if (snapshotBill.getId() != null) {
                HashMap<String, Object> p = new HashMap<>();
                p.put("b", snapshotBill);
                p.put("cat", selectedCategory);
                items = billItemFacade.findByJpql("select bi from BillItem bi where bi.bill=:b and bi.item.category=:cat order by bi.id", p);
            }

            // Ensure items is not null
            if (items == null) {
                items = java.util.Collections.emptyList();
            }

            // Rows
            int rowNum = 1;
            for (BillItem bi : items) {
                // Null check for bill item
                if (bi == null) {
                    continue;
                }

                PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
                ItemBatch ib = pbi != null ? pbi.getItemBatch() : null;
                Row row = sheet.createRow(rowNum++);
                if (row == null) {
                    continue;
                }

                int c = 0;

                // BillItem ID
                Cell cId = row.createCell(c++);
                if (cId != null) {
                    if (bi.getId() != null) {
                        cId.setCellValue(bi.getId());
                        if (integerLocked != null) {
                            cId.setCellStyle(integerLocked);
                        }
                    } else {
                        cId.setCellValue("");
                        if (textLocked != null) {
                            cId.setCellStyle(textLocked);
                        }
                    }
                }

                // Code
                Cell cCode = row.createCell(c++);
                if (cCode != null) {
                    cCode.setCellValue(bi.getItem() != null && bi.getItem().getCode() != null ? bi.getItem().getCode() : "");
                    if (textLocked != null) {
                        cCode.setCellStyle(textLocked);
                    }
                }

                // Name
                Cell cName = row.createCell(c++);
                if (cName != null) {
                    cName.setCellValue(bi.getDescreption() != null ? bi.getDescreption() : "");
                    if (textLocked != null) {
                        cName.setCellStyle(textLocked);
                    }
                }

                // Category
                Cell cCat = row.createCell(c++);
                if (cCat != null) {
                    cCat.setCellValue(bi.getItem() != null && bi.getItem().getCategory() != null && bi.getItem().getCategory().getName() != null ? bi.getItem().getCategory().getName() : "");
                    if (textLocked != null) {
                        cCat.setCellStyle(textLocked);
                    }
                }

                // Batch
                Cell cBatch = row.createCell(c++);
                if (cBatch != null) {
                    if (ib != null && ib.getBatchNo() != null) {
                        cBatch.setCellValue(ib.getBatchNo());
                    } else {
                        cBatch.setCellValue("");
                    }
                    if (textLocked != null) {
                        cBatch.setCellStyle(textLocked);
                    }
                }

                // Expiry
                Cell cExp = row.createCell(c++);
                if (cExp != null) {
                    if (ib != null && ib.getDateOfExpire() != null) {
                        cExp.setCellValue(ib.getDateOfExpire());
                        if (dateLocked != null) {
                            cExp.setCellStyle(dateLocked);
                        }
                    } else {
                        cExp.setCellValue("");
                        if (textLocked != null) {
                            cExp.setCellStyle(textLocked);
                        }
                    }
                }

                // Rates - handle null returns from getter methods
                double pr = 0.0;
                double rr = 0.0;
                double cr = 0.0;

                if (ib != null) {
                    Double prObj = ib.getPurcahseRate();
                    pr = (prObj != null) ? prObj : 0.0;

                    Double rrObj = ib.getRetailsaleRate();
                    rr = (rrObj != null) ? rrObj : 0.0;

                    Double crObj = ib.getCostRate();
                    cr = (crObj != null) ? crObj : 0.0;
                }

                Cell cPR = row.createCell(c++);
                if (cPR != null) {
                    cPR.setCellValue(pr);
                    if (numberLocked != null) {
                        cPR.setCellStyle(numberLocked);
                    }
                }

                Cell cRR = row.createCell(c++);
                if (cRR != null) {
                    cRR.setCellValue(rr);
                    if (numberLocked != null) {
                        cRR.setCellStyle(numberLocked);
                    }
                }

                Cell cCR = row.createCell(c++);
                if (cCR != null) {
                    cCR.setCellValue(cr);
                    if (numberLocked != null) {
                        cCR.setCellStyle(numberLocked);
                    }
                }

                // System Qty (optional)
                if (systemQtyColIndex != null) {
                    Cell cSys = row.createCell(c++);
                    if (cSys != null) {
                        if (bi.getQty() != null) {
                            cSys.setCellValue(bi.getQty());
                        } else {
                            cSys.setCellValue(0.0);
                        }
                        if (numberLocked != null) {
                            cSys.setCellStyle(numberLocked);
                        }
                    }
                }

                // Real Qty (this is the editable column)
                Cell cReal = row.createCell(c++);
                if (cReal != null) {
                    // Do NOT prefill Data.
//                    if (includeSystemQty && bi.getQty() != null) {
//                        cReal.setCellValue(bi.getQty()); // pre-fill with system qty for guided sheet
//                    } else {
//                        cReal.setCellValue(0.0);
//                    }
                    if (inputUnlocked != null) {
                        cReal.setCellStyle(inputUnlocked);
                    }
                }

                // Line Value - handle null return
                Cell cLV = row.createCell(c++);
                if (cLV != null) {
                    Double netValueObj = bi.getNetValue();
                    double netValue = (netValueObj != null) ? netValueObj : 0.0;
                    cLV.setCellValue(netValue);
                    if (numberLocked != null) {
                        cLV.setCellStyle(numberLocked);
                    }
                }
            }

            // Auto-size columns
            for (int i = 0; i < col; i++) {
                try {
                    sheet.autoSizeColumn(i);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to autosize column " + i, e);
                }
            }

            // Convert to bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            byte[] bytes = baos.toByteArray();
            if (bytes == null || bytes.length == 0) {
                LOGGER.log(Level.SEVERE, "Failed to generate Excel file bytes");
                return null;
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            return DefaultStreamedContent.builder()
                    .name(fileName)
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .stream(() -> bais)
                    .build();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error generating category sheet", e);
            JsfUtil.addErrorMessage("Error generating sheet: " + e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error generating category sheet", e);
            JsfUtil.addErrorMessage("Unexpected error: " + e.getMessage());
            return null;
        }
    }

    // Removed legacy getters/setters for snapshotBills
    public List<VarianceRow> getVarianceRows() {
        return varianceRows;
    }

    // Start asynchronous approval so it completes even if browser closes
    public void startApprovePhysicalCountAsync() {
        LOGGER.log(Level.INFO, "[StockTake] startApprovePhysicalCountAsync() called. pcBillId={0}, items={1}",
                new Object[]{physicalCountBill != null ? physicalCountBill.getId() : null,
                    physicalCountBill != null && physicalCountBill.getBillItems() != null ? physicalCountBill.getBillItems().size() : 0});
        if (physicalCountBill == null || physicalCountBill.getBillItems() == null || physicalCountBill.getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No physical count available");
            LOGGER.log(Level.WARNING, "[StockTake] Async approval aborted. No physical count or items.");
            return;
        }
        if (!webUserController.hasPrivilege(Privileges.PharmacyStockTakeApprove.toString())) {
            JsfUtil.addErrorMessage("Not authorized");
            LOGGER.log(Level.WARNING, "[StockTake] Async approval aborted. Missing privilege PharmacyStockTakeApprove");
            return;
        }
        String jobId = java.util.UUID.randomUUID().toString();
        this.approvalJobId = jobId;
        approvalProgressTracker.start(jobId, physicalCountBill.getBillItems().size(), "Queued");
        Long approverId = sessionController.getLoggedUser() != null ? sessionController.getLoggedUser().getId() : null;
        LOGGER.log(Level.INFO, "[StockTake] Dispatching async approval. jobId={0}, approverId={1}, items={2}",
                new Object[]{jobId, approverId, physicalCountBill.getBillItems().size()});
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

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public Bill getAdjustmentBill() {
        return adjustmentBill;
    }

    public void setAdjustmentBill(Bill adjustmentBill) {
        this.adjustmentBill = adjustmentBill;
    }

    public boolean isIncludeZeroStockBatches() {
        return includeZeroStockBatches;
    }

    public void setIncludeZeroStockBatches(boolean includeZeroStockBatches) {
        this.includeZeroStockBatches = includeZeroStockBatches;
    }

    public int getZeroStockBatchLimit() {
        return zeroStockBatchLimit;
    }

    public void setZeroStockBatchLimit(int zeroStockBatchLimit) {
        this.zeroStockBatchLimit = zeroStockBatchLimit;
    }

    public String getGenerationJobId() {
        return generationJobId;
    }

    public void setGenerationJobId(String generationJobId) {
        this.generationJobId = generationJobId;
    }

    /**
     * Generate a sanitized filename for variance report Excel export. Includes
     * the snapshot bill number, sanitized to remove invalid filename
     * characters.
     *
     * @return sanitized filename with .xlsx extension
     */
    public String getVarianceExcelFilename() {
        if (snapshotBill == null || snapshotBill.getDeptId() == null || snapshotBill.getDeptId().trim().isEmpty()) {
            return "pharmacy_stock_take_variance.xlsx";
        }
        // Sanitize the bill number by replacing invalid filename characters with underscore
        String sanitized = snapshotBill.getDeptId()
                .replaceAll("[/\\\\:*?\"<>|,.-]", "_")
                .trim();
        return "pharmacy_stock_take_variance_" + sanitized;
    }

    // Navigation methods
    /**
     * Navigate to the Start New Stock Taking page
     *
     * @return navigation outcome
     */
    public String navigateToStartNewStockTaking() {
        institution = sessionController.getInstitution();
        site = sessionController.getLoggedSite();
        department = sessionController.getDepartment();
        return "/pharmacy/pharmacy_stock_take?faces-redirect=true";
    }

    /**
     * Navigate to the Manage Stock Takings page
     *
     * @return navigation outcome
     */
    public String navigateToManageStockTakings() {
        institution = sessionController.getInstitution();
        site = sessionController.getLoggedSite();
        department = sessionController.getDepartment();
        return "/pharmacy/pharmacy_stock_take_list?faces-redirect=true";
    }

    /**
     * Navigate to the Pending Physical Count Approvals page
     *
     * @return navigation outcome
     */
    public String navigateToPendingPhysicalCountApprovals() {
        return "/pharmacy/pharmacy_physical_count_pending?faces-redirect=true";
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
