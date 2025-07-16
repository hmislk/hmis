package com.divudi.service;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.HistoricalRecord;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Upload;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.HistoricalRecordFacade;
import com.divudi.core.facade.UploadFacade;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.apache.poi.ss.usermodel.Row;
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

    private static class Summary {
        double qty;
        double value;
    }

    @Asynchronous
    public void generateAllItemMovementReport(HistoricalRecord hr) {
        if (hr == null) {
            return;
        }
        hr.setStartedAt(new Date());
        historicalRecordFacade.edit(hr);
        try {
            List<BillTypeAtomic> types = Arrays.asList(BillTypeAtomic.values());
            List<PharmaceuticalBillItem> pbis = billService.fetchPharmaceuticalBillItems(
                    hr.getFromDateTime(),
                    hr.getToDateTime(),
                    hr.getInstitution(),
                    hr.getSite(),
                    hr.getDepartment(),
                    null,
                    types,
                    null,
                    null);
            Map<BillTypeAtomic, Map<Item, Summary>> grouped = new ConcurrentHashMap<>();
            for (PharmaceuticalBillItem pbi : pbis) {
                if (pbi == null || pbi.getBillItem() == null || pbi.getBillItem().getBill() == null) {
                    continue;
                }
                BillTypeAtomic bt = pbi.getBillItem().getBill().getBillTypeAtomic();
                Item item = pbi.getBillItem().getItem();
                if (bt == null || item == null) {
                    continue;
                }
                Map<Item, Summary> itemMap = grouped.computeIfAbsent(bt, k -> new ConcurrentHashMap<>());
                Summary s = itemMap.computeIfAbsent(item, k -> new Summary());
                s.qty += pbi.getQty() + pbi.getFreeQty();
                s.value += pbi.getBillItem().getNetValue();
            }
            XSSFWorkbook wb = new XSSFWorkbook();
            XSSFSheet sheet = wb.createSheet("Item Movement Summary");
            int r = 0;
            if (hr.getFromDateTime() != null) {
                Row fr = sheet.createRow(r++);
                fr.createCell(0).setCellValue("From");
                fr.createCell(1).setCellValue(hr.getFromDateTime());
            }
            if (hr.getToDateTime() != null) {
                Row tr = sheet.createRow(r++);
                tr.createCell(0).setCellValue("To");
                tr.createCell(1).setCellValue(hr.getToDateTime());
            }
            if (hr.getInstitution() != null) {
                Row ir = sheet.createRow(r++);
                ir.createCell(0).setCellValue("Institution");
                ir.createCell(1).setCellValue(hr.getInstitution().getName());
            }
            if (hr.getSite() != null) {
                Row sr = sheet.createRow(r++);
                sr.createCell(0).setCellValue("Site");
                sr.createCell(1).setCellValue(hr.getSite().getName());
            }
            if (hr.getDepartment() != null) {
                Row dr = sheet.createRow(r++);
                dr.createCell(0).setCellValue("Department");
                dr.createCell(1).setCellValue(hr.getDepartment().getName());
            }
            r++; // blank line
            Row header = sheet.createRow(r++);
            header.createCell(0).setCellValue("Bill Type");
            header.createCell(1).setCellValue("Item");
            header.createCell(2).setCellValue("Total Qty");
            header.createCell(3).setCellValue("Net Value");
            for (Map.Entry<BillTypeAtomic, Map<Item, Summary>> e1 : grouped.entrySet()) {
                for (Map.Entry<Item, Summary> e2 : e1.getValue().entrySet()) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(e1.getKey().getLabel());
                    row.createCell(1).setCellValue(e2.getKey().getName());
                    row.createCell(2).setCellValue(e2.getValue().qty);
                    row.createCell(3).setCellValue(e2.getValue().value);
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
