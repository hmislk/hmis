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
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.ItemFee;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Service;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.bean.common.BillBeanController;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.service.pharmacy.DirectIssueBatchService;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
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
 * stock movement). A department user later approves (charges the BHT and
 * deducts stock atomically) or rejects (records a reason) the request via a
 * JSF approval queue built separately. External systems poll
 * {@code GET /api/itemrequests/{id}} for status.
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
    private BillFeeFacade billFeeFacade;

    @EJB
    private ItemFacade itemFacade;

    @EJB
    private PatientEncounterFacade patientEncounterFacade;

    @EJB
    private DepartmentFacade departmentFacade;

    @EJB
    private StockFacade stockFacade;

    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;

    @EJB
    private BillNumberGenerator billNumberBean;

    @Inject
    private BillBeanController billBeanController;

    @Inject
    private DirectIssueBatchService directIssueBatchService;

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

        // See approveRequest()'s note on Bill.billItems (@OneToMany cascade=ALL,
        // orphanRemoval=true) — keep it in sync with every BillItem persisted
        // against this bill to avoid a spurious delete-orphan on a later edit().
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

        return toResponseDTO(requestBill, savedLines, "PENDING", null, null, null);
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
    // Approve — the core transactional operation
    // =========================================================================

    /**
     * Approves a pending item/service request: charges the BHT for every line
     * and, for inventory lines, deducts stock atomically. If any line cannot be
     * fulfilled (insufficient stock) the whole operation rolls back — this
     * method is expected to run inside the EJB container-managed transaction of
     * this {@code @Stateless} bean, so a thrown exception undoes everything
     * already persisted in this call.
     * <p>
     * Re-fetches the request bill by id (rather than accepting a pre-fetched
     * entity) to guard against a stale in-memory bill racing a concurrent
     * approval/rejection/cancellation of the same request.
     */
    public Bill approveRequest(Long requestBillId, WebUser approvingUser, Department approvingDepartment) {
        Bill requestBill = fetchRequestBillOrThrow(requestBillId);
        String status = deriveStatus(requestBill);
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("Request is already " + status + ", cannot approve");
        }

        List<BillItem> requestLines = billItemFacade.findByJpql(
                "select bi from BillItem bi where bi.retired=false and bi.bill=:b",
                singleParam("b", requestBill));

        Bill approvalBill = new Bill();
        approvalBill.setBillTypeAtomic(BillTypeAtomic.INWARD_SERVICE_ITEM_APPROVAL);
        approvalBill.setBillType(BillType.InwardServiceItemApproval);
        approvalBill.setReferenceBill(requestBill);
        approvalBill.setFromDepartment(requestBill.getFromDepartment());
        approvalBill.setFromInstitution(requestBill.getFromInstitution());
        approvalBill.setToDepartment(requestBill.getToDepartment());
        approvalBill.setToInstitution(requestBill.getToInstitution());
        approvalBill.setDepartment(approvingDepartment);
        approvalBill.setInstitution(approvingDepartment != null ? approvingDepartment.getInstitution() : null);
        approvalBill.setPatientEncounter(requestBill.getPatientEncounter());
        approvalBill.setCreater(approvingUser);
        approvalBill.setCreatedAt(new Date());

        String deptId = approvingDepartment != null
                ? billNumberBean.departmentBillNumberGeneratorYearly(approvingDepartment, BillTypeAtomic.INWARD_SERVICE_ITEM_APPROVAL)
                : null;
        approvalBill.setDeptId(deptId);
        approvalBill.setInsId(deptId);

        // Bill.billItems is a @OneToMany(cascade=ALL, orphanRemoval=true) collection.
        // Every BillItem persisted against this bill MUST also be appended here,
        // otherwise the next time this Bill is managed/edited, EclipseLink treats
        // the DB-only rows as orphans removed from the collection and issues a
        // DELETE for them (causing a spurious FK-violation rollback).
        approvalBill.setBillItems(new ArrayList<BillItem>());

        billFacade.create(approvalBill);

        List<BillItem> inventoryBillItems = new ArrayList<>();
        double approvalTotal = 0.0;
        double approvalNetTotal = 0.0;

        for (BillItem requestLine : requestLines) {
            Item item = requestLine.getItem();
            double qty = Math.abs(requestLine.getQty());

            if (isServiceItem(item)) {
                BillItem approvalLine = new BillItem();
                approvalLine.setBill(approvalBill);
                approvalLine.setItem(item);
                approvalLine.setQty(qty);
                approvalLine.setReferanceBillItem(requestLine);
                approvalLine.setCreatedAt(new Date());
                approvalLine.setCreater(approvingUser);
                billItemFacade.create(approvalLine);
                approvalBill.getBillItems().add(approvalLine);

                double lineTotal = saveServiceLineFees(approvalBill, approvalLine, item, approvingUser);
                approvalTotal += lineTotal;
                approvalNetTotal += lineTotal;
            } else {
                Stock stock = findAvailableStockForItem(item, approvalBill.getToDepartment(), qty);
                if (stock == null) {
                    throw new IllegalStateException("Insufficient stock for item: " + item.getName());
                }

                // Charge the BHT at the batch retail rate, mirroring
                // PharmacySaleBhtController's BHT-issue pricing.
                double retailRate = stock.getItemBatch() != null ? stock.getItemBatch().getRetailsaleRate() : 0.0;
                double lineValue = retailRate * qty;

                BillItem approvalLine = new BillItem();
                approvalLine.setBill(approvalBill);
                approvalLine.setItem(item);
                approvalLine.setQty(qty);
                approvalLine.setRate(retailRate);
                approvalLine.setGrossValue(lineValue);
                approvalLine.setNetValue(lineValue);
                approvalLine.setReferanceBillItem(requestLine);
                approvalLine.setCreatedAt(new Date());
                approvalLine.setCreater(approvingUser);
                approvalTotal += lineValue;
                approvalNetTotal += lineValue;
                // BillItem.pharmaceuticalBillItem is the inverse side of a
                // @OneToOne(cascade=ALL) owned by PharmaceuticalBillItem.billItem —
                // must persist BillItem first with the relation still null, then
                // create+link PharmaceuticalBillItem, then edit BOTH sides, mirroring
                // PharmacySaleBhtController.savePreBillItemsFinallyRequest (lines
                // ~1396-1407). Skipping the final PharmaceuticalBillItem edit causes
                // EclipseLink to lose track of the relationship and issue a spurious
                // DELETE on the BillItem at commit time (FK violation).
                billItemFacade.create(approvalLine);
                approvalBill.getBillItems().add(approvalLine);

                PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
                pbi.setBillItem(approvalLine);
                pbi.setStock(stock);
                pbi.setQty(qty);
                pbi.setQtyInUnit(qty);
                pharmaceuticalBillItemFacade.create(pbi);

                approvalLine.setPharmaceuticalBillItem(pbi);
                billItemFacade.edit(approvalLine);
                pbi.setBillItem(approvalLine);
                pharmaceuticalBillItemFacade.edit(pbi);

                inventoryBillItems.add(approvalLine);
            }
        }

        if (!inventoryBillItems.isEmpty()) {
            // Throws (RuntimeException) on insufficient stock at commit time —
            // propagated, not swallowed, so the container rolls back everything
            // persisted above in this same transaction.
            directIssueBatchService.batchStockDeduction(inventoryBillItems);
        }

        if (approvalTotal > 0 || approvalNetTotal > 0) {
            approvalBill.setTotal(approvalTotal);
            approvalBill.setNetTotal(approvalNetTotal);
            billFacade.edit(approvalBill);
        }

        requestBill.setFullyIssued(true);
        billFacade.edit(requestBill);

        return approvalBill;
    }

    // =========================================================================
    // Reject
    // =========================================================================

    public Bill rejectRequest(Long requestBillId, String reason, WebUser rejectingUser) {
        Bill requestBill = fetchRequestBillOrThrow(requestBillId);
        String status = deriveStatus(requestBill);
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("Request is already " + status + ", cannot reject");
        }

        requestBill.setCancelled(true);
        requestBill.setBillTypeAtomic(BillTypeAtomic.INWARD_SERVICE_ITEM_REJECTION);
        String existingComments = requestBill.getComments();
        requestBill.setComments((existingComments == null || existingComments.trim().isEmpty() ? "" : existingComments + " | ")
                + "Rejected: " + reason);
        billFacade.edit(requestBill);

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

    private String deriveStatus(Bill requestBill) {
        if (requestBill.isCancelled()) {
            if (requestBill.getBillTypeAtomic() == BillTypeAtomic.INWARD_SERVICE_ITEM_REJECTION) {
                return "REJECTED";
            }
            return "CANCELLED";
        }
        Bill approvalBill = findApprovalBill(requestBill);
        if (approvalBill != null) {
            return "APPROVED";
        }
        return "PENDING";
    }

    private Bill findApprovalBill(Bill requestBill) {
        String jpql = "select b from Bill b where b.referenceBill=:reqBill "
                + "and b.billTypeAtomic=:approvalType and b.cancelled=false";
        Map<String, Object> params = new HashMap<>();
        params.put("reqBill", requestBill);
        params.put("approvalType", BillTypeAtomic.INWARD_SERVICE_ITEM_APPROVAL);
        return billFacade.findFirstByJpql(jpql, params);
    }

    private ItemRequestResponseDTO buildResponseForRequestBill(Bill requestBill) {
        List<BillItem> lines = billItemFacade.findByJpql(
                "select bi from BillItem bi where bi.retired=false and bi.bill=:b",
                singleParam("b", requestBill));

        String status = deriveStatus(requestBill);
        Bill approvalBill = null;
        String rejectionReason = null;

        if ("APPROVED".equals(status)) {
            approvalBill = findApprovalBill(requestBill);
        } else if ("REJECTED".equals(status)) {
            // Cancellation reasons stay in comments only; rejectionReason is
            // reserved for department rejections.
            rejectionReason = requestBill.getComments();
        }

        Long approvalBillId = approvalBill != null ? approvalBill.getId() : null;
        Date decidedAt = approvalBill != null ? approvalBill.getCreatedAt() : null;
        String decidedBy = approvalBill != null && approvalBill.getCreater() != null
                ? approvalBill.getCreater().getName() : null;

        return toResponseDTO(requestBill, lines, status, approvalBillId, decidedAt, decidedBy, rejectionReason);
    }

    private ItemRequestResponseDTO toResponseDTO(Bill requestBill, List<BillItem> lines, String status,
            Long approvalBillId, Date decidedAt, String decidedBy) {
        return toResponseDTO(requestBill, lines, status, approvalBillId, decidedAt, decidedBy, null);
    }

    private ItemRequestResponseDTO toResponseDTO(Bill requestBill, List<BillItem> lines, String status,
            Long approvalBillId, Date decidedAt, String decidedBy, String rejectionReason) {

        ItemRequestResponseDTO dto = new ItemRequestResponseDTO();
        dto.setId(requestBill.getId());
        dto.setRequestNo(requestBill.getDeptId());
        dto.setBhtNo(requestBill.getPatientEncounter() != null ? requestBill.getPatientEncounter().getBhtNo() : null);
        dto.setTargetDepartmentId(requestBill.getToDepartment() != null ? requestBill.getToDepartment().getId() : null);
        dto.setTargetDepartmentName(requestBill.getToDepartment() != null ? requestBill.getToDepartment().getName() : null);
        dto.setStatus(status);
        dto.setComments(requestBill.getComments());
        dto.setRejectionReason(rejectionReason);
        dto.setCreatedAt(requestBill.getCreatedAt());
        dto.setCreatedBy(requestBill.getCreater() != null ? requestBill.getCreater().getName() : null);
        dto.setApprovalBillId(approvalBillId);
        dto.setDecidedAt(decidedAt);
        dto.setDecidedBy(decidedBy);

        List<ItemRequestLineResponseDTO> lineDtos = new ArrayList<>();
        for (BillItem bi : lines) {
            ItemRequestLineResponseDTO lineDto = new ItemRequestLineResponseDTO();
            lineDto.setBillItemId(bi.getId());
            lineDto.setItemId(bi.getItem() != null ? bi.getItem().getId() : null);
            lineDto.setItemName(bi.getItem() != null ? bi.getItem().getName() : null);
            lineDto.setItemType(isServiceItem(bi.getItem()) ? "SERVICE" : "INVENTORY");
            lineDto.setQty(bi.getQty() != null ? bi.getQty() : 0.0);
            lineDto.setNetValue(bi.getNetValue());
            lineDtos.add(lineDto);
        }
        dto.setLines(lineDtos);

        return dto;
    }

    /**
     * SERVICE covers both OPD {@link Service} and {@link
     * com.divudi.core.entity.inward.InwardService} (which extends Service).
     * Anything else passed to this request/approval flow is treated as an
     * INVENTORY (stock) item, e.g. Water Bottle/Tea/Milk/Sugar.
     */
    private boolean isServiceItem(Item item) {
        return item instanceof Service;
    }

    /**
     * Mirrors {@code InwardAdditionalChargeController.saveBillFee(BillItem)}:
     * creates one BillFee per configured ItemFee for the item, falling back to
     * a single generic fee entry when the item has no ItemFee records.
     *
     * @return the total fee value charged for this line (sum of feeValue)
     */
    private double saveServiceLineFees(Bill approvalBill, BillItem approvalLine, Item item, WebUser approvingUser) {
        List<ItemFee> itemFees = billBeanController.fillFees(item);
        double lineTotal = 0.0;

        if (itemFees != null && !itemFees.isEmpty()) {
            List<BillFee> created = new ArrayList<>();
            for (ItemFee f : itemFees) {
                BillFee bf = new BillFee();
                bf.setBill(approvalBill);
                bf.setBillItem(approvalLine);
                bf.setFee(f);
                bf.setFeeAt(new Date());
                bf.setCreatedAt(new Date());
                bf.setCreater(approvingUser);
                bf.setPatienEncounter(approvalBill.getPatientEncounter());
                bf.setPatient(approvalBill.getPatientEncounter() != null ? approvalBill.getPatientEncounter().getPatient() : null);
                bf.setFeeValue(f.getFee());
                bf.setFeeGrossValue(f.getFee());
                bf.setFeeDiscount(0.0);
                billFeeFacade.create(bf);
                created.add(bf);
                lineTotal += f.getFee();
            }
            approvalLine.setBillFees(created);
            approvalLine.setGrossValue(lineTotal);
            approvalLine.setNetValue(lineTotal);
            billItemFacade.edit(approvalLine);
        }

        return lineTotal;
    }

    /**
     * Finds a Stock row for the given item+department with enough quantity,
     * ordered by earliest expiry (mirrors {@code PharmacyBean.getStockByQty}
     * FIFO-by-expiry pattern, but queried directly against Stock.itemBatch.item
     * rather than requiring an Amp/Vmp cast, since stock items such as Water
     * Bottle/Tea/Milk/Sugar are not necessarily pharmaceutical Amps).
     */
    private Stock findAvailableStockForItem(Item item, Department department, double qty) {
        if (item == null || department == null) {
            return null;
        }
        String jpql = "select s from Stock s where s.retired=false "
                + "and s.itemBatch.item=:item and s.department=:d and s.stock>=:q "
                + "order by s.itemBatch.dateOfExpire";
        Map<String, Object> params = new HashMap<>();
        params.put("item", item);
        params.put("d", department);
        params.put("q", qty);
        return stockFacade.findFirstByJpql(jpql, params);
    }

    private Map<String, Object> singleParam(String key, Object value) {
        Map<String, Object> params = new HashMap<>();
        params.put(key, value);
        return params;
    }
}
