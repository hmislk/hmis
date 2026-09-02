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
            String jpql = "SELECT "
                    + "fd.printingName, fd.name, fd.address, fd.telephone1, fd.telephone2, fd.fax, "
                    + "td.name, "
                    + "b.deptId, b.createdAt, "
                    + "pe.bhtNo, pp.name, pp.title, pt.phn, rfc.name, "
                    + "rp.name, rp.title, wu.name, "
                    + "b.comments, b.completed, b.cancelled "
                    + "FROM Bill b "
                    + "LEFT JOIN b.fromDepartment fd "
                    + "LEFT JOIN b.toDepartment td "
                    + "LEFT JOIN b.patientEncounter pe "
                    + "LEFT JOIN pe.patient pt "
                    + "LEFT JOIN pt.person pp "
                    + "LEFT JOIN pe.currentPatientRoom pr "
                    + "LEFT JOIN pr.roomFacilityCharge rfc "
                    + "LEFT JOIN b.creater wu "
                    + "LEFT JOIN wu.webUserPerson rp "
                    + "WHERE b.id = :billId AND b.retired = false";

            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createQuery(jpql).setParameter("billId", billId).getResultList();
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
        String jpql = "SELECT i.name, bi.qty, bi.descreption, bi.instructions, p.comment "
                + "FROM BillItem bi "
                + "LEFT JOIN bi.item i "
                + "LEFT JOIN bi.prescription p "
                + "WHERE bi.bill.id = :billId AND bi.retired = false "
                + "ORDER BY bi.id";
        List<Object[]> rows = em.createQuery(jpql).setParameter("billId", billId).getResultList();
        for (Object[] r : rows) {
            int col = 0;
            BhtIssueRequestItemPrintDto item = new BhtIssueRequestItemPrintDto();
            item.setItemName(str(r[col++]));
            item.setQty(r[col] != null ? ((Number) r[col]).doubleValue() : 0.0);
            col++;
            item.setDirections(str(r[col++]));
            item.setInstructions(str(r[col++]));
            item.setPrescriptionComment(str(r[col++]));
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
