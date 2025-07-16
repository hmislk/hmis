package com.divudi.service;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.HistoricalRecord;
import com.divudi.core.entity.Upload;
import com.divudi.core.data.dto.ItemMovementSummaryDTO;
import com.divudi.core.facade.HistoricalRecordFacade;
import com.divudi.core.facade.UploadFacade;
import com.divudi.core.util.CommonFunctions;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Stateless
public class PharmacyAsyncReportService {

    @EJB
    private BillService billService;
    @EJB
    private UploadFacade uploadFacade;
    @EJB
    private HistoricalRecordFacade historicalRecordFacade;
    @Inject
    SessionController sessionController;

    @Asynchronous
    public void generateAllItemMovementReport(HistoricalRecord hr) {
        if (hr == null) {
            return;
        }
        hr.setStartedAt(new Date());
        historicalRecordFacade.edit(hr);
        try {
            List<BillTypeAtomic> types = Arrays.asList(BillTypeAtomic.values());
            List<ItemMovementSummaryDTO> rows = billService.fetchItemMovementSummaryDTOs(
                    hr.getFromDateTime(),
                    hr.getToDateTime(),
                    hr.getInstitution(),
                    hr.getSite(),
                    hr.getDepartment(),
                    types);
            XSSFWorkbook wb = new XSSFWorkbook();
            XSSFSheet sheet = wb.createSheet("Item Movement Summary");

            CellStyle borderStyle = wb.createCellStyle();
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);

            int r = 0;
            
            //TODO: If institution, Site, Department is not given, it should come as Department: All, Site : All, , Institution: All
            //TODO: Date time should be formatted with - sessionController.getApplicationPreference().getLongDateFormat()
            // TODO: THe rows should have a border
            // TODO: the Bill TYpe Atomic should be in a merged cell above the respective Quantity and Net value columns
            // TODO: For each item: the final Qty and final value should be added as the total columns
            // TODO: as the last column the current stock should be listed . the total stock can be taken from 
            // StockController
            // method depending on selection, public double findInstitutionStock(Institution institution, Item item) {, public double findDepartmentStock(Department department, Item item) { or  public double findSiteStock(Institution site, Item item) {
            // TODO: at the bottom, there should be a row to total all the net values ( quentities are not useful to total as they are from different items)
            
            if (hr.getFromDateTime() != null) {
                Row fr = sheet.createRow(r++);
                fr.createCell(0).setCellValue("From");
                fr.createCell(1).setCellValue(CommonFunctions.getDateFormat(hr.getFromDateTime(), sessionController.getApplicationPreference().getLongDateFormat()));
            }
            if (hr.getToDateTime() != null) {
                Row tr = sheet.createRow(r++);
                tr.createCell(0).setCellValue("To");
                tr.createCell(1).setCellValue(CommonFunctions.getDateFormat(hr.getToDateTime(), sessionController.getApplicationPreference().getLongDateFormat()));
            }
            {
                Row ir = sheet.createRow(r++);
                ir.createCell(0).setCellValue("Institution");
                ir.createCell(1).setCellValue(hr.getInstitution() != null ? hr.getInstitution().getName() : "All");
            }
            {
                Row sr = sheet.createRow(r++);
                sr.createCell(0).setCellValue("Site");
                sr.createCell(1).setCellValue(hr.getSite() != null ? hr.getSite().getName() : "All");
            }
            {
                Row dr = sheet.createRow(r++);
                dr.createCell(0).setCellValue("Department");
                dr.createCell(1).setCellValue(hr.getDepartment() != null ? hr.getDepartment().getName() : "All");
            }
            r++; // blank line
// ChatGPT contribution: One row per item, with Qty and Net Value as adjacent columns under each Bill Type
            Map<String, Map<BillTypeAtomic, ItemMovementSummaryDTO>> grouped = new TreeMap<>();
            Set<BillTypeAtomic> billTypes = new TreeSet<>(Comparator.comparing(BillTypeAtomic::getLabel));

// Group data
            for (ItemMovementSummaryDTO dto : rows) {
                grouped.computeIfAbsent(dto.getItemName(), k -> new EnumMap<>(BillTypeAtomic.class))
                        .put(dto.getBillTypeAtomic(), dto);
                billTypes.add(dto.getBillTypeAtomic());
            }

// First header row: Bill type names
            Row header1 = sheet.createRow(r++);
            Cell hItem = header1.createCell(0);
            hItem.setCellValue("Item");
            hItem.setCellStyle(borderStyle);
            int col = 1;
            for (BillTypeAtomic bt : billTypes) {
                Cell c = header1.createCell(col);
                c.setCellValue(bt.getLabel());
                c.setCellStyle(borderStyle);
                header1.createCell(col + 1).setCellStyle(borderStyle);
                sheet.addMergedRegion(new CellRangeAddress(r - 1, r - 1, col, col + 1));
                col += 2;
            }
            Cell totalHeadQty = header1.createCell(col);
            totalHeadQty.setCellValue("Total Qty");
            totalHeadQty.setCellStyle(borderStyle);
            Cell totalHeadVal = header1.createCell(col + 1);
            totalHeadVal.setCellValue("Total Value");
            totalHeadVal.setCellStyle(borderStyle);

// Second header row: Qty / Net Value
            Row header2 = sheet.createRow(r++);
            Cell hb = header2.createCell(0);
            hb.setCellStyle(borderStyle);
            col = 1;
            for (int i = 0; i < billTypes.size(); i++) {
                Cell q = header2.createCell(col++);
                q.setCellValue("Qty");
                q.setCellStyle(borderStyle);
                Cell v = header2.createCell(col++);
                v.setCellValue("Net Value");
                v.setCellStyle(borderStyle);
            }
            Cell tQty = header2.createCell(col++);
            tQty.setCellValue("Qty");
            tQty.setCellStyle(borderStyle);
            Cell tVal = header2.createCell(col++);
            tVal.setCellValue("Net Value");
            tVal.setCellStyle(borderStyle);

// Data rows: One row per item
            double grandTotal = 0.0;
            for (Map.Entry<String, Map<BillTypeAtomic, ItemMovementSummaryDTO>> entry : grouped.entrySet()) {
                Row dataRow = sheet.createRow(r++);
                Cell itemCell = dataRow.createCell(0);
                itemCell.setCellValue(entry.getKey());
                itemCell.setCellStyle(borderStyle);
                Map<BillTypeAtomic, ItemMovementSummaryDTO> itemMap = entry.getValue();

                double rowQty = 0.0;
                double rowVal = 0.0;

                col = 1;
                for (BillTypeAtomic bt : billTypes) {
                    ItemMovementSummaryDTO dto = itemMap.get(bt);
                    Cell qCell = dataRow.createCell(col);
                    Cell vCell = dataRow.createCell(col + 1);
                    if (dto != null) {
                        qCell.setCellValue(dto.getQuantity());
                        vCell.setCellValue(dto.getNetValue());
                        rowQty += dto.getQuantity() != null ? dto.getQuantity() : 0;
                        rowVal += dto.getNetValue() != null ? dto.getNetValue() : 0;
                    }
                    qCell.setCellStyle(borderStyle);
                    vCell.setCellStyle(borderStyle);
                    col += 2;
                }
                Cell tq = dataRow.createCell(col);
                tq.setCellValue(rowQty);
                tq.setCellStyle(borderStyle);
                Cell tv = dataRow.createCell(col + 1);
                tv.setCellValue(rowVal);
                tv.setCellStyle(borderStyle);

                grandTotal += rowVal;
            }

            Row totalRow = sheet.createRow(r++);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("Total Net Value");
            totalLabel.setCellStyle(borderStyle);
            sheet.addMergedRegion(new CellRangeAddress(r - 1, r - 1, 0, billTypes.size()*2));
            Cell totalValueCell = totalRow.createCell(billTypes.size()*2 + 1);
            totalValueCell.setCellValue(grandTotal);
            totalValueCell.setCellStyle(borderStyle);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            wb.close();
            Upload u = new Upload();
            u.setBaImage(baos.toByteArray());
            u.setFileName("All_Item_Movement_Summary.xlsx");
            u.setFileType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            u.setCreatedAt(new Date());
            u.setCreater(hr.getCreatedBy());
            u.setInstitution(hr.getInstitution());
            u.setHistoricalRecord(hr);
            uploadFacade.create(u);
            hr.setCompleted(true);
            hr.setCompletedAt(new Date());
        } catch (Exception e) {
            hr.setCompleted(false);
        } finally {
            historicalRecordFacade.edit(hr);
        }
    }
}
