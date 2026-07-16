package com.divudi.service.pharmacy;

import com.divudi.core.data.Title;
import com.divudi.core.data.dto.pharmacy.BhtIssueRequestItemPrintDto;
import com.divudi.core.data.dto.pharmacy.BhtIssueRequestPrintDto;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class BhtIssueRequestNativeSqlService {

    private static final Logger LOGGER = Logger.getLogger(BhtIssueRequestNativeSqlService.class.getName());

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    public BhtIssueRequestPrintDto loadPrintDtoByBillId(long billId) {
        try {
            String sql = "SELECT "
                    + "fd.PRINTINGNAME, fd.NAME, fd.ADDRESS, fd.TELEPHONE1, fd.TELEPHONE2, fd.FAX, "
                    + "td.NAME, "
                    + "b.DEPTID, b.CREATEDAT, "
                    + "pe.BHTNO, pp.NAME, pp.TITLE, pt.PHN, rfc.NAME, "
                    + "rp.NAME, rp.TITLE, wu.NAME, "
                    + "b.COMMENTS, b.COMPLETED, b.CANCELLED "
                    + "FROM bill b "
                    + "LEFT JOIN department fd ON fd.ID = b.FROMDEPARTMENT_ID "
                    + "LEFT JOIN department td ON td.ID = b.TODEPARTMENT_ID "
                    + "LEFT JOIN patientencounter pe ON pe.ID = b.PATIENTENCOUNTER_ID "
                    + "LEFT JOIN patient pt ON pt.ID = pe.PATIENT_ID "
                    + "LEFT JOIN person pp ON pp.ID = pt.PERSON_ID "
                    + "LEFT JOIN patientroom pr ON pr.ID = pe.CURRENTPATIENTROOM_ID "
                    + "LEFT JOIN roomfacilitycharge rfc ON rfc.ID = pr.ROOMFACILITYCHARGE_ID "
                    + "LEFT JOIN webuser wu ON wu.ID = b.CREATER_ID "
                    + "LEFT JOIN person rp ON rp.ID = wu.WEBUSERPERSON_ID "
                    + "WHERE b.ID = ?1 AND b.RETIRED = 0";

            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(sql).setParameter(1, billId).getResultList();
            if (rows.isEmpty()) {
                return null;
            }
            Object[] r = rows.get(0);
            int col = 0;

            BhtIssueRequestPrintDto dto = new BhtIssueRequestPrintDto();
            dto.setFromDepartmentPrintingName(str(r[col++]));
            dto.setFromDepartmentName(str(r[col++]));
            dto.setFromDepartmentAddress(str(r[col++]));
            dto.setFromDepartmentTelephone1(str(r[col++]));
            dto.setFromDepartmentTelephone2(str(r[col++]));
            dto.setFromDepartmentFax(str(r[col++]));
            dto.setToDepartmentName(str(r[col++]));
            dto.setRequestNo(str(r[col++]));
            dto.setCreatedAt(toDate(r[col++]));
            dto.setBhtNo(str(r[col++]));

            String patientPlainName = str(r[col++]);
            String patientTitle = str(r[col++]);
            dto.setPatientName(titleLabel(patientTitle) + " " + patientPlainName);

            dto.setPatientPhn(str(r[col++]));
            dto.setRoomName(str(r[col++]));

            String requesterPlainName = str(r[col++]);
            String requesterTitle = str(r[col++]);
            dto.setRequestedByName(titleLabel(requesterTitle) + " " + requesterPlainName);

            dto.setSystemUserName(str(r[col++]));
            dto.setComments(str(r[col++]));
            dto.setCompleted(toBool(r[col++]));
            dto.setCancelled(toBool(r[col++]));

            dto.setItems(loadItems(billId));
            return dto;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load BHT issue request print DTO for bill " + billId, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<BhtIssueRequestItemPrintDto> loadItems(long billId) {
        List<BhtIssueRequestItemPrintDto> items = new ArrayList<>();
        String sql = "SELECT i.NAME, bi.QTY, bi.DESCREPTION "
                + "FROM billitem bi "
                + "LEFT JOIN item i ON i.ID = bi.ITEM_ID "
                + "WHERE bi.BILL_ID = ?1 AND bi.RETIRED = 0 "
                + "ORDER BY bi.ID";
        List<Object[]> rows = em.createNativeQuery(sql).setParameter(1, billId).getResultList();
        for (Object[] r : rows) {
            int col = 0;
            BhtIssueRequestItemPrintDto item = new BhtIssueRequestItemPrintDto();
            item.setItemName(str(r[col++]));
            item.setQty(r[col] != null ? ((Number) r[col]).doubleValue() : 0.0);
            col++;
            item.setDirections(str(r[col++]));
            items.add(item);
        }
        return items;
    }

    String str(Object o) {
        return o != null ? o.toString().trim() : "";
    }

    boolean toBool(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue() != 0;
        }
        return false;
    }

    Date toDate(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Timestamp) {
            return new Date(((Timestamp) o).getTime());
        }
        if (o instanceof Date) {
            return (Date) o;
        }
        return null;
    }

    String titleLabel(String titleName) {
        if (titleName == null || titleName.trim().isEmpty()) {
            return "";
        }
        try {
            return Title.valueOf(titleName.trim()).getLabel();
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
}
