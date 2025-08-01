package com.divudi.service;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.HistoricalRecord;
import com.divudi.core.entity.Upload;
import com.divudi.core.data.dto.ItemMovementSummaryDTO;
import com.divudi.core.facade.HistoricalRecordFacade;
import com.divudi.core.facade.UploadFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.bean.pharmacy.StockController;
import com.divudi.core.entity.Item;
import com.divudi.core.util.CommonFunctions;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
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
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private StockService stockService;

    @Asynchronous
    public void generateAllItemMovementReport(HistoricalRecord hr, String longDateFormat) {
        if (hr == null) {
            return;
        }
        hr.setStartedAt(new Date());
        historicalRecordFacade.edit(hr);
        try {

            // Pharmacy Retail Sale (not pre bill)
            List<BillTypeAtomic> types = Arrays.asList(
                    // Sale in one colour - starts here
                    // Pharmacy Retail Sale (not pre bill)
                    BillTypeAtomic.PHARMACY_RETAIL_SALE, //Quantities should be minus, values should be plus
                    BillTypeAtomic.PHARMACY_RETAIL_SALE_WITHOUT_STOCKS, //Quantities should be minus, values should be plus

                    // Pharmacy Retail Sale Cancellation
                    BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED, //Quantities should be plus, values should be minus
                    BillTypeAtomic.PHARMACY_SALE_WITHOUT_STOCK_CANCELLED, //Quantities should be plus, values should be minus

                    // Pharmacy Retail Sale Return
                    BillTypeAtomic.PHARMACY_RETAIL_SALE_REFUND, //Quantities should be plus, values should be minus
                    BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY, //Quantities should be plus, values should be minus
                    BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS, //Quantities should be plus, values should be minus
                    BillTypeAtomic.PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION, //Quantities should be minus, values should be plus

                    // Pharmacy Wholesale Sale
                    BillTypeAtomic.PHARMACY_WHOLESALE, //Quantities should be minus, values should be plus

                    // Pharmacy Wholesale Cancellation
                    BillTypeAtomic.PHARMACY_WHOLESALE_CANCELLED, //Quantities should be plus, values should be minus

                    // Pharmacy Wholesale Return
                    BillTypeAtomic.PHARMACY_WHOLESALE_REFUND, //Quantities should be plus, values should be minus

                // Sale in one background colour - ends here                    
                    // inpatient Sale in one background colour - starts here                    
                    // Inpatient Issue
                    BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, //Quantities should be minus, values neutral

                    // Inpatient Issue Return
                    BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN, //Quantities should be plus, values neutral

                    // Inpatient Issue Cancellation
                    BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION, //Quantities should be plus, values neutral

                    // inpatient Sale in one background colour - ends here                    
                    // Goods purchase in one background colour - starts here                    
                    // GRN
                    BillTypeAtomic.PHARMACY_GRN, //Quantities should be plus, values should be minus (money going out)

                    // GRN Cancellation
                    BillTypeAtomic.PHARMACY_GRN_CANCELLED, //Quantities should be minus, values should be plus

                    // GRN Return
                    BillTypeAtomic.PHARMACY_GRN_RETURN, //Quantities should be minus, values should be plus

                    // GRN Return Cancellation
                    BillTypeAtomic.PHARMACY_GRN_REFUND, //Quantities should be plus, values should be minus

                    // Direct Purchase
                    BillTypeAtomic.PHARMACY_DIRECT_PURCHASE, //Quantities should be plus, values should be minus

                    // Direct Purchase Cancellation
                    BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED, //Quantities should be minus, values should be plus

                    // Direct Purchase Return
                    BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND, //Quantities should be minus, values should be plus
                    
                    // Goods purchase in one background colour - starts here                    
                    // Goods transferred in one background colour - starts here                    

                    // Transfer Issue
                    BillTypeAtomic.PHARMACY_DIRECT_ISSUE, //Quantities should be minus, values neutral

                    // Transfer Receive
                    BillTypeAtomic.PHARMACY_RECEIVE //Quantities should be plus, values neutral
                    
                    // Goods transferred in one background colour - ends here                    
            );

            List<ItemMovementSummaryDTO> rows = billService.fetchItemMovementSummaryDTOs(
                    hr.getFromDateTime(),
                    hr.getToDateTime(),
                    hr.getInstitution(),
                    hr.getSite(),
                    hr.getDepartment(),
                    types);
            System.out.println("DTOs fetched: " + rows.size());

            XSSFWorkbook wb = new XSSFWorkbook();
            XSSFSheet sheet = wb.createSheet("Item Movement Summary");

            CellStyle borderStyle = wb.createCellStyle();
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);

            CellStyle salesStyle = wb.createCellStyle();
            salesStyle.cloneStyleFrom(borderStyle);
            salesStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            salesStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle inpatientStyle = wb.createCellStyle();
            inpatientStyle.cloneStyleFrom(borderStyle);
            inpatientStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            inpatientStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle purchaseStyle = wb.createCellStyle();
            purchaseStyle.cloneStyleFrom(borderStyle);
            purchaseStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            purchaseStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle transferStyle = wb.createCellStyle();
            transferStyle.cloneStyleFrom(borderStyle);
            transferStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            transferStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            DataFormat df = wb.createDataFormat();
            short qtyFormat = df.getFormat("#,##0.#");
            short valFormat = df.getFormat("#,##0.00");

            CellStyle qtyBorderStyle = wb.createCellStyle();
            qtyBorderStyle.cloneStyleFrom(borderStyle);
            qtyBorderStyle.setDataFormat(qtyFormat);

            CellStyle valBorderStyle = wb.createCellStyle();
            valBorderStyle.cloneStyleFrom(borderStyle);
            valBorderStyle.setDataFormat(valFormat);

            Map<BillTypeAtomic, CellStyle> styleMap = new EnumMap<>(BillTypeAtomic.class);
            Map<BillTypeAtomic, CellStyle> qtyStyleMap = new EnumMap<>(BillTypeAtomic.class);
            Map<BillTypeAtomic, CellStyle> valStyleMap = new EnumMap<>(BillTypeAtomic.class);
            for (BillTypeAtomic bt : types) {
                CellStyle style;
                if (bt == BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE
                        || bt == BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN
                        || bt == BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION) {
                    style = inpatientStyle;
                } else if (bt == BillTypeAtomic.PHARMACY_GRN
                        || bt == BillTypeAtomic.PHARMACY_GRN_CANCELLED
                        || bt == BillTypeAtomic.PHARMACY_GRN_RETURN
                        || bt == BillTypeAtomic.PHARMACY_GRN_REFUND
                        || bt == BillTypeAtomic.PHARMACY_DIRECT_PURCHASE
                        || bt == BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED
                        || bt == BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND) {
                    style = purchaseStyle;
                } else if (bt == BillTypeAtomic.PHARMACY_DIRECT_ISSUE
                        || bt == BillTypeAtomic.PHARMACY_RECEIVE) {
                    style = transferStyle;
                } else {
                    style = salesStyle;
                }
                styleMap.put(bt, style);

                CellStyle qs = wb.createCellStyle();
                qs.cloneStyleFrom(style);
                qs.setDataFormat(qtyFormat);
                qtyStyleMap.put(bt, qs);

                CellStyle vs = wb.createCellStyle();
                vs.cloneStyleFrom(style);
                vs.setDataFormat(valFormat);
                valStyleMap.put(bt, vs);
            }

            int r = 0;

            if (hr.getFromDateTime() != null) {
                Row fr = sheet.createRow(r++);
                fr.createCell(0).setCellValue("From");
                fr.createCell(1).setCellValue(CommonFunctions.getDateFormat(hr.getFromDateTime(), longDateFormat));
            }
            if (hr.getToDateTime() != null) {
                Row tr = sheet.createRow(r++);
                tr.createCell(0).setCellValue("To");
                tr.createCell(1).setCellValue(CommonFunctions.getDateFormat(hr.getToDateTime(), longDateFormat));
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

            Map<String, Map<BillTypeAtomic, ItemMovementSummaryDTO>> grouped = new TreeMap<>();
            List<BillTypeAtomic> billTypes = types;
            for (ItemMovementSummaryDTO dto : rows) {
                String itemName = dto.getItemName() != null ? dto.getItemName().trim() : "";
                BillTypeAtomic bt = dto.getBillTypeAtomic();
                if (!itemName.isEmpty() && bt != null) {
                    grouped.computeIfAbsent(itemName, k -> new EnumMap<>(BillTypeAtomic.class))
                            .put(bt, dto);
                }
            }

            Map<String, Double> stockMap = new TreeMap<>();
            Map<BillTypeAtomic, Double> billTypeTotals = new EnumMap<>(BillTypeAtomic.class);
            for (Map.Entry<String, Map<BillTypeAtomic, ItemMovementSummaryDTO>> inEntry : grouped.entrySet()) {
                Map<BillTypeAtomic, ItemMovementSummaryDTO> im = inEntry.getValue();
                ItemMovementSummaryDTO first = null;
                if (im != null && !im.isEmpty()) {
                    first = im.values().iterator().next();
                }
                Item itm = null;
                if (first != null && first.getItemId() != null) {
                    itm = itemFacade.find(first.getItemId());
                }
                double st = 0.0;
                if (itm != null) {
                    if (hr.getDepartment() != null) {
                        Double d = stockService.findDepartmentStock(hr.getDepartment(), itm);
                        st = d != null ? d : 0.0;
                    } else if (hr.getSite() != null) {
                        st = stockService.findSiteStock(hr.getSite(), itm);
                    } else if (hr.getInstitution() != null) {
                        st = stockService.findStock(hr.getInstitution(), itm);
                    } else {
                        st = stockService.findStock(itm);
                    }
                }
                stockMap.put(inEntry.getKey(), st);
            }

            Row header1 = sheet.createRow(r++);
            Cell hItem = header1.createCell(0);
            hItem.setCellValue("Item");
            hItem.setCellStyle(borderStyle);
            int col = 1;
            for (BillTypeAtomic bt : billTypes) {
                CellStyle st = styleMap.get(bt);
                Cell c = header1.createCell(col);
                c.setCellValue(bt.getLabel());
                c.setCellStyle(st);
                Cell dummy = header1.createCell(col + 1);
                dummy.setCellStyle(st);
                sheet.addMergedRegion(new CellRangeAddress(r - 1, r - 1, col, col + 1));
                col += 2;
            }
            Cell totalHeadQty = header1.createCell(col);
            totalHeadQty.setCellValue("Total Qty");
            totalHeadQty.setCellStyle(borderStyle);
            Cell totalHeadVal = header1.createCell(col + 1);
            totalHeadVal.setCellValue("Total Value");
            totalHeadVal.setCellStyle(borderStyle);
            Cell stockHead = header1.createCell(col + 2);
            stockHead.setCellValue("Current Stock");
            stockHead.setCellStyle(borderStyle);

            Row header2 = sheet.createRow(r++);
            Cell hb = header2.createCell(0);
            hb.setCellStyle(borderStyle);
            col = 1;
            for (BillTypeAtomic bt : billTypes) {
                CellStyle st = styleMap.get(bt);
                Cell q = header2.createCell(col++);
                q.setCellValue("Qty");
                q.setCellStyle(st);
                Cell v = header2.createCell(col++);
                v.setCellValue("Net Value");
                v.setCellStyle(st);
            }
            Cell tQty = header2.createCell(col++);
            tQty.setCellValue("Qty");
            tQty.setCellStyle(borderStyle);
            Cell tVal = header2.createCell(col++);
            tVal.setCellValue("Net Value");
            tVal.setCellStyle(borderStyle);
            Cell stkVal = header2.createCell(col++);
            stkVal.setCellValue("Qty");
            stkVal.setCellStyle(borderStyle);

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
                    CellStyle qst = qtyStyleMap.get(bt);
                    CellStyle vst = valStyleMap.get(bt);
                    Cell qCell = dataRow.createCell(col);
                    Cell vCell = dataRow.createCell(col + 1);
                    if (dto != null) {
                        qCell.setCellValue(dto.getQuantity());
                        vCell.setCellValue(dto.getNetValue());
                        rowQty += dto.getQuantity() != null ? dto.getQuantity() : 0;
                        rowVal += dto.getNetValue() != null ? dto.getNetValue() : 0;
                        billTypeTotals.merge(bt, dto.getNetValue() != null ? dto.getNetValue() : 0.0, Double::sum);
                    }
                    qCell.setCellStyle(qst);
                    vCell.setCellStyle(vst);
                    col += 2;
                }
                Cell tq = dataRow.createCell(col);
                tq.setCellValue(rowQty);
                tq.setCellStyle(qtyBorderStyle);
                Cell tv = dataRow.createCell(col + 1);
                tv.setCellValue(rowVal);
                tv.setCellStyle(valBorderStyle);
                Cell stockCell = dataRow.createCell(col + 2);
                Double sc = stockMap.get(entry.getKey());
                stockCell.setCellValue(sc != null ? sc : 0.0);
                stockCell.setCellStyle(borderStyle);

                grandTotal += rowVal;
            }

            CellStyle thickQtyStyle = wb.createCellStyle();
            thickQtyStyle.cloneStyleFrom(qtyBorderStyle);
            thickQtyStyle.setBorderBottom(BorderStyle.MEDIUM);
            thickQtyStyle.setBorderTop(BorderStyle.MEDIUM);
            thickQtyStyle.setBorderLeft(BorderStyle.MEDIUM);
            thickQtyStyle.setBorderRight(BorderStyle.MEDIUM);

            CellStyle thickValStyle = wb.createCellStyle();
            thickValStyle.cloneStyleFrom(valBorderStyle);
            thickValStyle.setBorderBottom(BorderStyle.MEDIUM);
            thickValStyle.setBorderTop(BorderStyle.MEDIUM);
            thickValStyle.setBorderLeft(BorderStyle.MEDIUM);
            thickValStyle.setBorderRight(BorderStyle.MEDIUM);

            Row totalRow = sheet.createRow(r++);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("Total Net Value");
            totalLabel.setCellStyle(thickValStyle);

            int tc = 1;
            for (BillTypeAtomic bt : billTypes) {
                Cell q = totalRow.createCell(tc++);
                q.setCellStyle(thickQtyStyle);
                Cell v = totalRow.createCell(tc++);
                Double vv = billTypeTotals.get(bt);
                if (vv != null) {
                    v.setCellValue(vv);
                } else {
                    v.setCellValue(0.0);
                }
                v.setCellStyle(thickValStyle);
            }
            Cell blankQty = totalRow.createCell(tc++);
            blankQty.setCellStyle(thickQtyStyle);
            Cell grandVal = totalRow.createCell(tc++);
            grandVal.setCellValue(grandTotal);
            grandVal.setCellStyle(thickValStyle);
            Cell blankStock = totalRow.createCell(tc++);
            blankStock.setCellStyle(thickQtyStyle);

            int lastCol = billTypes.size() * 2 + 2;
            int qtyTotCol = 1 + billTypes.size() * 2;
            int valTotCol = qtyTotCol + 1;
            for (int i = 0; i <= lastCol; i++) {
                sheet.autoSizeColumn(i);
                boolean qtyCol = (i >= 1 && i < qtyTotCol && ((i - 1) % 2 == 0)) || i == qtyTotCol;
                boolean valCol = (i >= 1 && i < qtyTotCol && ((i - 1) % 2 == 1)) || i == valTotCol;
                int minWidth = 0;
                if (qtyCol) {
                    minWidth = 10 * 256;
                } else if (valCol) {
                    minWidth = 14 * 256;
                }
                if (minWidth > 0 && sheet.getColumnWidth(i) < minWidth) {
                    sheet.setColumnWidth(i, minWidth);
                }
            }

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
