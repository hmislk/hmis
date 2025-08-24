package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 * Controller for capturing pharmacy stock snapshots and exporting them to Excel.
 */
@Named
@SessionScoped
public class PharmacyStockTakeController implements Serializable {

    @Inject
    private SessionController sessionController;

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

    /**
     * Capture current department stock into a snapshot bill.
     */
    public void captureSnapshot() {
        Department dept = sessionController.getLoggedUser().getDepartment();
        if (dept == null) {
            JsfUtil.addErrorMessage("No department found");
            return;
        }
        String jpql = "select s from Stock s where s.department=:d and s.stock>0";
        HashMap<String, Object> params = new HashMap<>();
        params.put("d", dept);
        List<Stock> stocks = stockFacade.findByJpql(jpql, params);
        if (stocks == null || stocks.isEmpty()) {
            JsfUtil.addErrorMessage("No stock available");
            return;
        }
        snapshotBill = new Bill();
        snapshotBill.setBillType(BillType.PharmacySnapshotBill);
        snapshotBill.setBillClassType(BillClassType.BilledBill);
        snapshotBill.setDepartment(dept);
        snapshotBill.setInstitution(dept.getInstitution());
        snapshotBill.setCreatedAt(new Date());
        snapshotBill.setCreater(sessionController.getLoggedUser());
        String deptId = billNumberBean.departmentBillNumberGenerator(dept, BillType.PharmacySnapshotBill, BillClassType.BilledBill, BillNumberSuffix.NONE);
        snapshotBill.setInsId(deptId);
        snapshotBill.setDeptId(deptId);
        billFacade.create(snapshotBill);

        for (Stock s : stocks) {
            BillItem bi = new BillItem();
            bi.setBill(snapshotBill);
            bi.setItem(s.getItemBatch().getItem());
            bi.setQty(s.getStock());
            bi.setCreatedAt(new Date());
            bi.setCreater(sessionController.getLoggedUser());
            billItemFacade.create(bi);
            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
            pbi.setBillItem(bi);
            pbi.setItemBatch(s.getItemBatch());
            pbi.setQty(s.getStock());
            pbi.setStock(s);
            pharmaceuticalBillItemFacade.create(pbi);
            bi.setPharmaceuticalBillItem(pbi);
            snapshotBill.getBillItems().add(bi);
        }
        billFacade.edit(snapshotBill);
        JsfUtil.addSuccessMessage("Snapshot created");
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
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Code");
            header.createCell(1).setCellValue("Name");
            header.createCell(2).setCellValue("Batch");
            int col = 3;
            if (includeSystemQty) {
                header.createCell(col++).setCellValue("System Qty");
            }
            header.createCell(col).setCellValue("Counted Qty");
            int rowNum = 1;
            for (BillItem bi : snapshotBill.getBillItems()) {
                PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
                ItemBatch ib = pbi.getItemBatch();
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(ib.getItem().getCode());
                row.createCell(1).setCellValue(ib.getItem().getName());
                row.createCell(2).setCellValue(ib.getBatchNo());
                int c = 3;
                if (includeSystemQty) {
                    row.createCell(c++).setCellValue(pbi.getQty());
                }
                // Counted quantity cell left blank for user input
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

    public Bill getSnapshotBill() {
        return snapshotBill;
    }

    public void setSnapshotBill(Bill snapshotBill) {
        this.snapshotBill = snapshotBill;
    }
}
