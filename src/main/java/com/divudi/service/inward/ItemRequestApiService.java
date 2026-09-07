/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.inward;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BillType;
import com.divudi.core.data.dto.itemrequest.ItemRequestCreateRequestDTO;
import com.divudi.core.data.dto.itemrequest.ItemRequestLineDTO;
import com.divudi.core.data.dto.itemrequest.ItemRequestLineResponseDTO;
import com.divudi.core.data.dto.itemrequest.ItemRequestResponseDTO;
import com.divudi.core.data.dto.itemrequest.ItemRequestSearchResultDTO;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Service;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.ejb.BillNumberGenerator;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service layer for the external Item/Service Request API (issue #21793).
 * <p>
 * External systems submit item/service requests (meals, stock items such as
 * water/tea/milk/sugar) against a patient's active BHT. Requests are saved as
 * a Pending {@link BillType#InwardServiceItemRequest} bill (no charge, no
 * stock movement). A department user later fulfills each line by navigating
 * into the real Add Services / Direct Issue pages (which link the resulting
 * BillItem back to the request line via {@code referanceBillItem}), or
 * rejects still-pending lines (records a reason) via a JSF approval queue
 * built separately. External systems poll {@code GET /api/itemrequests/{id}}
 * for status.
 *
 * @author Claude AI Assistant
 */
@Stateless
public class ItemRequestApiService implements Serializable {

    @EJB
    private BillFacade billFacade;

    @EJB
    private BillItemFacade billItemFacade;

    @EJB
    private ItemFacade itemFacade;

    @EJB
    private PatientEncounterFacade patientEncounterFacade;

    @EJB
    private DepartmentFacade departmentFacade;

    @EJB
    private BillNumberGenerator billNumberBean;

    // =========================================================================
    // Submit
    // =========================================================================

    public ItemRequestResponseDTO submitRequest(ItemRequestCreateRequestDTO dto, WebUser apiUser) {
        if (dto == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (dto.getBhtNo() == null || dto.getBhtNo().trim().isEmpty()) {
            throw new IllegalArgumentException("bhtNo is required");
        }

        PatientEncounter pe = findPatientEncounterByBhtNo(dto.getBhtNo().trim());
        if (pe == null) {
            throw new IllegalArgumentException("No active BHT found for bhtNo: " + dto.getBhtNo());
        }
        if (Boolean.TRUE.equals(pe.isDischarged())) {
            throw new IllegalArgumentException("Patient is discharged for BHT: " + dto.getBhtNo());
        }
        if (pe.isPaymentFinalized()) {
            throw new IllegalArgumentException("BHT has already been settled: " + dto.getBhtNo());
        }

        if (dto.getTargetDepartmentId() == null) {
            throw new IllegalArgumentException("targetDepartmentId is required");
        }
        Department targetDepartment = departmentFacade.find(dto.getTargetDepartmentId());
        if (targetDepartment == null || targetDepartment.isRetired()) {
            throw new IllegalArgumentException("Target department not found: " + dto.getTargetDepartmentId());
        }

        if (dto.getLines() == null || dto.getLines().isEmpty()) {
            throw new IllegalArgumentException("At least one request line is required");
        }

        // Resolve and validate every line up-front before persisting anything.
        List<Item> resolvedItems = new ArrayList<>();
        for (ItemRequestLineDTO line : dto.getLines()) {
            if (line.getQty() <= 0) {
                throw new IllegalArgumentException("qty must be greater than zero for itemId: " + line.getItemId());
            }
            if (line.getItemId() == null) {
                throw new IllegalArgumentException("itemId is required for each line");
            }
            Item item = itemFacade.find(line.getItemId());
            if (item == null || item.isRetired()) {
                throw new IllegalArgumentException("Item not found: " + line.getItemId());
            }
            resolvedItems.add(item);
        }

        Bill requestBill = new Bill();
        requestBill.setPatientEncounter(pe);
        // The API has no ward-session context to resolve the requesting ward
        // department reliably (PharmacyRequestForBhtController derives this from
        // the logged-in user's current department session, which does not exist
        // for an external API caller). Leaving fromDepartment/fromInstitution
        // null rather than guessing — see final report.
        requestBill.setFromDepartment(null);
        requestBill.setFromInstitution(null);
        requestBill.setToDepartment(targetDepartment);
        requestBill.setToInstitution(targetDepartment.getInstitution());
        requestBill.setDepartment(targetDepartment);
        requestBill.setInstitution(targetDepartment.getInstitution());
        requestBill.setBillTypeAtomic(BillTypeAtomic.INWARD_SERVICE_ITEM_REQUEST);
        requestBill.setBillType(BillType.InwardServiceItemRequest);
        requestBill.setComments(dto.getComments());
        requestBill.setCreatedAt(new Date());
        requestBill.setCreater(apiUser);

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(targetDepartment, BillTypeAtomic.INWARD_SERVICE_ITEM_REQUEST);
        requestBill.setDeptId(deptId);
        requestBill.setInsId(deptId);

        // Bill.billItems is a @OneToMany(cascade=ALL, orphanRemoval=true) collection.
        // Keep it in sync with every BillItem persisted against this bill to avoid
        // a spurious delete-orphan on a later edit().
        requestBill.setBillItems(new ArrayList<BillItem>());

        billFacade.create(requestBill);

        List<BillItem> savedLines = new ArrayList<>();
        for (int i = 0; i < dto.getLines().size(); i++) {
            ItemRequestLineDTO lineDto = dto.getLines().get(i);
            Item item = resolvedItems.get(i);

            BillItem billItem = new BillItem();
            billItem.setBill(requestBill);
            billItem.setItem(item);
            billItem.setQty(lineDto.getQty());
            billItem.setGrossValue(0.0);
            billItem.setNetValue(0.0);
            billItem.setCreatedAt(new Date());
            billItem.setCreater(apiUser);

            billItemFacade.create(billItem);
            requestBill.getBillItems().add(billItem);
            savedLines.add(billItem);
        }

        // Bill uses GenerationType.IDENTITY; EclipseLink defers the INSERT until
        // flush/commit, so without an explicit flush the generated id would still
        // be null when the response DTO is built.
        billFacade.flush();

        return toResponseDTO(requestBill, savedLines, "PENDING");
    }

    // =========================================================================
    // Get by id
    // =========================================================================

    public ItemRequestResponseDTO getRequestById(Long id) {
        Bill bill = fetchRequestBillOrThrow(id);
        return buildResponseForRequestBill(bill);
    }

    // =========================================================================
    // List / search
    // =========================================================================

    public List<ItemRequestSearchResultDTO> listRequests(Long targetDepartmentId, String status,
            Date fromDate, Date toDate, Integer limit) {

        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder(
                "select b from Bill b where b.retired=false and b.billType=:bTp");
        params.put("bTp", BillType.InwardServiceItemRequest);

        if (targetDepartmentId != null) {
            jpql.append(" and b.toDepartment.id=:toDepId");
            params.put("toDepId", targetDepartmentId);
        }
        if (fromDate != null && toDate != null) {
            jpql.append(" and b.createdAt between :fromDate and :toDate");
            params.put("fromDate", fromDate);
            params.put("toDate", toDate);
        }
        jpql.append(" order by b.createdAt desc");

        List<Bill> bills = billFacade.findByJpql(jpql.toString(), params);

        List<ItemRequestSearchResultDTO> results = new ArrayList<>();
        for (Bill b : bills) {
            String derivedStatus = deriveStatus(b);
            if (status != null && !status.trim().isEmpty() && !status.trim().equalsIgnoreCase(derivedStatus)) {
                continue;
            }
            ItemRequestSearchResultDTO row = new ItemRequestSearchResultDTO();
            row.setId(b.getId());
            row.setRequestNo(b.getDeptId());
            row.setBhtNo(b.getPatientEncounter() != null ? b.getPatientEncounter().getBhtNo() : null);
            row.setTargetDepartmentName(b.getToDepartment() != null ? b.getToDepartment().getName() : null);
            row.setStatus(derivedStatus);
            row.setCreatedAt(b.getCreatedAt());
            results.add(row);

            if (limit != null && results.size() >= limit) {
                break;
            }
        }
        return results;
    }

    // =========================================================================
    // Cancel (by requester, only while PENDING)
    // =========================================================================

    public ItemRequestResponseDTO cancelRequest(Long id, String reason, WebUser apiUser) {
        Bill bill = fetchRequestBillOrThrow(id);
        String status = deriveStatus(bill);
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("Request is already " + status + ", cannot cancel");
        }

        bill.setCancelled(true);
        bill.setBillTypeAtomic(BillTypeAtomic.INWARD_SERVICE_ITEM_REQUEST_CANCELLATION);
        String existingComments = bill.getComments();
        bill.setComments((existingComments == null || existingComments.trim().isEmpty() ? "" : existingComments + " | ")
                + "Cancelled: " + reason);
        billFacade.edit(bill);

        return buildResponseForRequestBill(bill);
    }

    // =========================================================================
    // Reject
    // =========================================================================

    /**
     * Rejects only the given still-pending request lines (retires their
     * BillItem rows with a reason), leaving any already-fulfilled lines and
     * their real bills untouched. Issue #21793 redesign — rejection is no
     * longer whole-request once partial fulfillment is possible.
     */
    public Bill rejectRemainingLines(Long requestBillId, List<BillItem> linesToReject, String reason, WebUser rejectingUser, Department rejectingDepartment) {
        Bill requestBill = fetchRequestBillOrThrow(requestBillId);
        assertRequestBelongsToDepartment(requestBill, rejectingDepartment, "reject");
        if (requestBill.isCancelled()) {
            throw new IllegalStateException("Request is cancelled, cannot reject lines");
        }

        for (BillItem line : linesToReject) {
            if (line.getBill() == null || !line.getBill().equals(requestBill)) {
                continue;
            }
            if (isLineFulfilled(line)) {
                continue;
            }
            line.setRetired(true);
            line.setRetirer(rejectingUser);
            line.setRetiredAt(new Date());
            line.setRetireComments(reason);
            billItemFacade.edit(line);
        }

        return requestBill;
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private PatientEncounter findPatientEncounterByBhtNo(String bhtNo) {
        String jpql = "select pe from PatientEncounter pe where pe.retired=false and pe.bhtNo=:bht";
        Map<String, Object> params = new HashMap<>();
        params.put("bht", bhtNo);
        return patientEncounterFacade.findFirstByJpql(jpql, params);
    }

    /**
     * Server-side authorization scope check: approve/reject may only act on
     * requests routed to the acting user's own department. The JSF queue is
     * already department-filtered, but that is UI-level only — a crafted
     * postback (or a stale session list after a department switch) could
     * otherwise act on another department's request.
     */
    private void assertRequestBelongsToDepartment(Bill requestBill, Department actingDepartment, String action) {
        if (actingDepartment == null || actingDepartment.getId() == null
                || requestBill.getToDepartment() == null
                || !actingDepartment.getId().equals(requestBill.getToDepartment().getId())) {
            throw new IllegalStateException("Request belongs to another department's queue, cannot " + action);
        }
    }

    private Bill fetchRequestBillOrThrow(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Item request id is required");
        }
        Bill bill = billFacade.find(id);
        if (bill == null || bill.getBillType() != BillType.InwardServiceItemRequest) {
            throw new IllegalArgumentException("Item request not found: " + id);
        }
        return bill;
    }

    /**
     * A request line is FULFILLED once some BillItem elsewhere references it via
     * referanceBillItem (set by BillBhtController/InpatientDirectIssueNativeSqlController's
     * save paths — issue #21793 redesign). Overall status is derived from the mix
     * of fulfilled / rejected / pending lines.
     */
    public boolean isLineFulfilled(BillItem requestLine) {
        Map<String, Object> params = new HashMap<>();
        params.put("line", requestLine);
        Long count = billItemFacade.findLongByJpql(
                "select count(bi) from BillItem bi where bi.retired=false and bi.referanceBillItem=:line",
                params);
        return count != null && count > 0;
    }

    private BillItem findFulfillingBillItem(BillItem requestLine) {
        Map<String, Object> params = new HashMap<>();
        params.put("line", requestLine);
        return billItemFacade.findFirstByJpql(
                "select bi from BillItem bi where bi.retired=false and bi.referanceBillItem=:line",
                params);
    }

    private String deriveStatus(Bill requestBill) {
        if (requestBill.isCancelled()) {
            return "CANCELLED";
        }
        List<BillItem> lines = billItemFacade.findByJpql(
                "select bi from BillItem bi where bi.bill=:b",
                singleParam("b", requestBill));
        boolean anyFulfilled = false;
        boolean anyPending = false;
        boolean anyRejected = false;
        for (BillItem line : lines) {
            if (isLineFulfilled(line)) {
                anyFulfilled = true;
            } else if (Boolean.TRUE.equals(line.isRetired())) {
                anyRejected = true;
            } else {
                anyPending = true;
            }
        }
        if (anyFulfilled && anyRejected && !anyPending) {
            return "PARTIALLY_FULFILLED_AND_REJECTED";
        }
        if (anyFulfilled && anyPending) {
            return "PARTIALLY_FULFILLED";
        }
        if (anyFulfilled) {
            return "FULFILLED";
        }
        if (anyRejected && !anyPending) {
            return "REJECTED";
        }
        return "PENDING";
    }

    private ItemRequestResponseDTO buildResponseForRequestBill(Bill requestBill) {
        List<BillItem> lines = billItemFacade.findByJpql(
                "select bi from BillItem bi where bi.retired=false and bi.bill=:b",
                singleParam("b", requestBill));
        // Include retired (rejected) lines too, so the API can report their status.
        List<BillItem> retiredLines = billItemFacade.findByJpql(
                "select bi from BillItem bi where bi.retired=true and bi.bill=:b",
                singleParam("b", requestBill));

        List<BillItem> allLines = new ArrayList<>();
        allLines.addAll(lines);
        allLines.addAll(retiredLines);

        String status = deriveStatus(requestBill);
        return toResponseDTO(requestBill, allLines, status);
    }

    private ItemRequestResponseDTO toResponseDTO(Bill requestBill, List<BillItem> lines, String status) {
        ItemRequestResponseDTO dto = new ItemRequestResponseDTO();
        dto.setId(requestBill.getId());
        dto.setRequestNo(requestBill.getDeptId());
        dto.setBhtNo(requestBill.getPatientEncounter() != null ? requestBill.getPatientEncounter().getBhtNo() : null);
        dto.setTargetDepartmentId(requestBill.getToDepartment() != null ? requestBill.getToDepartment().getId() : null);
        dto.setTargetDepartmentName(requestBill.getToDepartment() != null ? requestBill.getToDepartment().getName() : null);
        dto.setStatus(status);
        dto.setComments(requestBill.getComments());
        dto.setCreatedAt(requestBill.getCreatedAt());
        dto.setCreatedBy(requestBill.getCreater() != null ? requestBill.getCreater().getName() : null);

        List<ItemRequestLineResponseDTO> lineDtos = new ArrayList<>();
        List<Long> fulfillingBillIds = new ArrayList<>();
        for (BillItem bi : lines) {
            ItemRequestLineResponseDTO lineDto = new ItemRequestLineResponseDTO();
            lineDto.setBillItemId(bi.getId());
            lineDto.setItemId(bi.getItem() != null ? bi.getItem().getId() : null);
            lineDto.setItemName(bi.getItem() != null ? bi.getItem().getName() : null);
            lineDto.setItemType(isServiceItem(bi.getItem()) ? "SERVICE" : "INVENTORY");
            lineDto.setQty(bi.getQty() != null ? bi.getQty() : 0.0);

            if (Boolean.TRUE.equals(bi.isRetired())) {
                lineDto.setStatus("REJECTED");
                lineDto.setRejectionReason(bi.getRetireComments());
            } else {
                BillItem fulfillingLine = findFulfillingBillItem(bi);
                if (fulfillingLine != null) {
                    lineDto.setStatus("FULFILLED");
                    lineDto.setFulfillingBillId(fulfillingLine.getBill().getId());
                    lineDto.setFulfillingBillType(fulfillingLine.getBill().getBillType().name());
                    if (!fulfillingBillIds.contains(fulfillingLine.getBill().getId())) {
                        fulfillingBillIds.add(fulfillingLine.getBill().getId());
                    }
                } else {
                    lineDto.setStatus("PENDING");
                }
            }
            lineDtos.add(lineDto);
        }
        dto.setLines(lineDtos);
        dto.setFulfillingBillIds(fulfillingBillIds);

        return dto;
    }

    /**
     * SERVICE covers both OPD {@link Service} and {@link
     * com.divudi.core.entity.inward.InwardService} (which extends Service), as
     * well as {@link Investigation} lines (which extend {@link Item} directly,
     * not {@link Service}) — these are routed through the same Add
     * Services / Investigations approval path as real services. Anything else
     * passed to this request/approval flow is treated as an INVENTORY (stock)
     * item, e.g. Water Bottle/Tea/Milk/Sugar.
     *
     * <p>Shared with {@link com.divudi.bean.inward.ItemRequestApprovalController}
     * so the pending-queue "remaining lines" split and this API's per-line
     * {@code itemType} reporting cannot drift apart.
     */
    public static boolean isServiceItem(Item item) {
        return item instanceof Service || item instanceof Investigation;
    }

    private Map<String, Object> singleParam(String key, Object value) {
        Map<String, Object> params = new HashMap<>();
        params.put(key, value);
        return params;
    }
}
