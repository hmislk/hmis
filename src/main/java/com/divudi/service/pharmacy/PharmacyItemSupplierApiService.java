package com.divudi.service.pharmacy;

import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.dto.pharmacy.BulkItemSupplierResultDto;
import com.divudi.core.data.dto.pharmacy.ItemSupplierDto;
import com.divudi.core.data.dto.pharmacy.ItemSupplierRequestDto;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.ItemsDistributors;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.ItemsDistributorsFacade;
import com.divudi.service.AuditService;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Pharmacy Item Supplier (ItemsDistributors) API operations.
 * Provides business logic for creating, bulk linking, retrieving, and retiring
 * distributor/supplier links for pharmaceutical items.
 */
@Stateless
public class PharmacyItemSupplierApiService implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ItemsDistributorsFacade itemsDistributorsFacade;

    @EJB
    private InstitutionFacade institutionFacade;

    @EJB
    private ItemFacade itemFacade;

    @EJB
    private AuditService auditService;

    public static class CreateItemSupplierResult {
        private int status;
        private String message;
        private ItemSupplierDto data;
        private ItemsDistributors entity;

        public CreateItemSupplierResult(int status, String message, ItemSupplierDto data, ItemsDistributors entity) {
            this.status = status;
            this.message = message;
            this.data = data;
            this.entity = entity;
        }

        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public ItemSupplierDto getData() { return data; }
        public ItemsDistributors getEntity() { return entity; }
    }

    public static class RetireItemSupplierResult {
        private int status;
        private String message;
        private ItemSupplierDto data;

        public RetireItemSupplierResult(int status, String message, ItemSupplierDto data) {
            this.status = status;
            this.message = message;
            this.data = data;
        }

        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public ItemSupplierDto getData() { return data; }
    }

    /**
     * Search active item-supplier links with optional filtering and pagination.
     */
    public List<ItemSupplierDto> searchLinks(Long supplierId, Long itemId, Integer limit, Integer offset) {
        StringBuilder jpql = new StringBuilder("SELECT id FROM ItemsDistributors id WHERE id.retired = false AND id.item.retired = false");
        Map<String, Object> params = new HashMap<>();

        if (supplierId != null) {
            jpql.append(" AND id.institution.id = :supplierId");
            params.put("supplierId", supplierId);
        }
        if (itemId != null) {
            jpql.append(" AND id.item.id = :itemId");
            params.put("itemId", itemId);
        }
        jpql.append(" ORDER BY id.id DESC");

        int safeLimit = (limit != null && limit > 0) ? Math.min(limit, 500) : 50;
        int safeOffset = (offset != null && offset > 0) ? offset : 0;

        List<ItemsDistributors> list = itemsDistributorsFacade.findByJpqlWithRange(jpql.toString(), params, safeOffset, safeLimit);
        List<ItemSupplierDto> dtos = new ArrayList<>();
        if (list != null) {
            for (ItemsDistributors entity : list) {
                dtos.add(ItemSupplierDto.fromEntity(entity));
            }
        }
        return dtos;
    }

    /**
     * Get an item-supplier link by ID.
     */
    public ItemSupplierDto getLinkById(Long id) {
        if (id == null) {
            return null;
        }
        ItemsDistributors link = itemsDistributorsFacade.find(id);
        if (link == null) {
            return null;
        }
        return ItemSupplierDto.fromEntity(link);
    }

    /**
     * Find an active link for a given supplier and item.
     */
    public ItemsDistributors findActiveLink(Institution supplier, Item item) {
        if (supplier == null || item == null) {
            return null;
        }
        String jpql = "SELECT id FROM ItemsDistributors id WHERE id.retired = false AND id.institution = :ins AND id.item = :item";
        Map<String, Object> params = new HashMap<>();
        params.put("ins", supplier);
        params.put("item", item);
        return itemsDistributorsFacade.findFirstByJpql(jpql, params);
    }

    /**
     * Create a link between a Dealer supplier and an active item.
     * Rejects non-Dealer institutions and retired items.
     * Returns 409 if an active link already exists.
     */
    public CreateItemSupplierResult createLink(ItemSupplierRequestDto request, WebUser user) {
        if (request == null) {
            return new CreateItemSupplierResult(400, "Request payload is required", null, null);
        }
        if (request.getSupplierId() == null) {
            return new CreateItemSupplierResult(400, "supplierId is required", null, null);
        }
        if (request.getItemId() == null) {
            return new CreateItemSupplierResult(400, "itemId is required", null, null);
        }

        Institution supplier = institutionFacade.find(request.getSupplierId());
        if (supplier == null) {
            return new CreateItemSupplierResult(404, "Supplier not found with ID: " + request.getSupplierId(), null, null);
        }
        if (supplier.isRetired()) {
            return new CreateItemSupplierResult(400, "Supplier is retired: " + request.getSupplierId(), null, null);
        }
        if (supplier.getInstitutionType() != InstitutionType.Dealer) {
            return new CreateItemSupplierResult(400, "Supplier must be a Dealer institution (got: " + supplier.getInstitutionType() + ")", null, null);
        }

        Item item = itemFacade.find(request.getItemId());
        if (item == null) {
            return new CreateItemSupplierResult(404, "Item not found with ID: " + request.getItemId(), null, null);
        }
        if (item.isRetired()) {
            return new CreateItemSupplierResult(400, "Item is retired: " + request.getItemId(), null, null);
        }
        if (!(item instanceof Amp || item instanceof Ampp)) {
            return new CreateItemSupplierResult(400, "Item must be a pharmaceutical item (Amp or Ampp): " + request.getItemId(), null, null);
        }

        ItemsDistributors existing = findActiveLink(supplier, item);
        if (existing != null) {
            return new CreateItemSupplierResult(409, "Active link already exists for supplier " + supplier.getId() + " and item " + item.getId(),
                    ItemSupplierDto.fromEntity(existing), existing);
        }

        ItemsDistributors link = new ItemsDistributors();
        link.setInstitution(supplier);
        link.setItem(item);
        link.setOrderNo(request.getOrderNo() != null ? request.getOrderNo() : 0);
        link.setCreater(user);
        link.setCreatedAt(new Date());
        link.setRetired(false);

        itemsDistributorsFacade.createAndFlush(link);

        ItemSupplierDto dto = ItemSupplierDto.fromEntity(link);
        auditService.logAudit(null, dto, user, "ItemsDistributors", "CREATE_ITEM_SUPPLIER_LINK", link.getId());

        return new CreateItemSupplierResult(201, "Item supplier link created successfully", dto, link);
    }

    /**
     * Create multiple item-supplier links in bulk, returning per-row results.
     */
    public List<BulkItemSupplierResultDto> createBulk(List<ItemSupplierRequestDto> requests, WebUser user) {
        List<BulkItemSupplierResultDto> results = new ArrayList<>();
        if (requests == null || requests.isEmpty()) {
            return results;
        }

        for (ItemSupplierRequestDto req : requests) {
            BulkItemSupplierResultDto row = new BulkItemSupplierResultDto();
            if (req != null) {
                row.setSupplierId(req.getSupplierId());
                row.setItemId(req.getItemId());
            }

            CreateItemSupplierResult res = createLink(req, user);
            row.setCode(res.getStatus());
            row.setMessage(res.getMessage());

            if (res.getStatus() == 201) {
                row.setOutcome("created");
                row.setId(res.getData().getId());
                row.setData(res.getData());
            } else if (res.getStatus() == 409) {
                row.setOutcome("already_exists");
                if (res.getData() != null) {
                    row.setId(res.getData().getId());
                    row.setData(res.getData());
                }
            } else {
                row.setOutcome("error");
            }

            results.add(row);
        }

        return results;
    }

    /**
     * Soft-retire an item-supplier link.
     */
    public RetireItemSupplierResult retireLink(Long id, String retireComments, WebUser user) {
        if (id == null) {
            return new RetireItemSupplierResult(400, "Link ID is required", null);
        }

        ItemsDistributors link = itemsDistributorsFacade.find(id);
        if (link == null) {
            return new RetireItemSupplierResult(404, "Item supplier link not found with ID: " + id, null);
        }

        if (link.isRetired()) {
            return new RetireItemSupplierResult(200, "Item supplier link is already retired", ItemSupplierDto.fromEntity(link));
        }

        ItemSupplierDto before = ItemSupplierDto.fromEntity(link);

        link.setRetired(true);
        link.setRetirer(user);
        link.setRetiredAt(new Date());
        link.setRetireComments(retireComments != null && !retireComments.trim().isEmpty() ? retireComments.trim() : "Retired via API");

        itemsDistributorsFacade.edit(link);

        ItemSupplierDto after = ItemSupplierDto.fromEntity(link);
        auditService.logAudit(before, after, user, "ItemsDistributors", "RETIRE_ITEM_SUPPLIER_LINK", link.getId());

        return new RetireItemSupplierResult(200, "Item supplier link retired successfully", after);
    }
}
