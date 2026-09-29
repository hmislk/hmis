package com.divudi.service.inward;

import com.divudi.core.data.dto.inward.InpatientPackageDto;
import com.divudi.core.data.dto.inward.InpatientPackageItemDto;
import com.divudi.core.data.inward.InpatientPackageComponentType;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.InpatientPackage;
import com.divudi.core.entity.inward.InpatientPackageItem;
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.facade.AdmissionTypeFacade;
import com.divudi.core.facade.InpatientPackageFacade;
import com.divudi.core.facade.InpatientPackageItemFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.RoomCategoryFacade;
import com.divudi.core.facade.SpecialityFacade;
import com.divudi.core.util.InpatientPackagePricing;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Stateless
public class InpatientPackageApiService implements Serializable {

    @EJB
    private InpatientPackageFacade packageFacade;

    @EJB
    private InpatientPackageItemFacade packageItemFacade;

    @EJB
    private AdmissionTypeFacade admissionTypeFacade;

    @EJB
    private RoomCategoryFacade roomCategoryFacade;

    @EJB
    private ItemFacade itemFacade;

    @EJB
    private SpecialityFacade specialityFacade;

    public InpatientPackageDto createPackage(InpatientPackageDto dto, WebUser user) throws Exception {
        if (dto == null) {
            throw new InpatientPackageValidationException("Request body is required");
        }
        InpatientPackage pkg = new InpatientPackage();
        applyHeaderFields(pkg, dto);
        pkg.setCreatedAt(new Date());
        pkg.setCreater(user);
        packageFacade.create(pkg);

        List<InpatientPackageItem> items = applyItems(pkg, dto.getItems(), user, new ArrayList<>());
        recomputeTotal(pkg, items);
        // IDENTITY-strategy ids (InpatientPackage.id, InpatientPackageItem.id) are not
        // populated on persist() alone under EclipseLink's container-managed persistence
        // context -- only on flush. Flush here so the response DTO carries real ids.
        packageFacade.flush();

        return toDto(pkg, items);
    }

    public List<InpatientPackageDto> listPackages(Long admissionTypeId, Long roomCategoryId) throws Exception {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder("SELECT p FROM InpatientPackage p WHERE p.retired = false ");
        if (admissionTypeId != null) {
            jpql.append("AND p.admissionType.id = :atId ");
            params.put("atId", admissionTypeId);
        }
        if (roomCategoryId != null) {
            jpql.append("AND p.roomCategory.id = :rcId ");
            params.put("rcId", roomCategoryId);
        }
        jpql.append("ORDER BY p.name");

        List<InpatientPackage> packages = packageFacade.findByJpql(jpql.toString(), params);
        List<InpatientPackageDto> results = new ArrayList<>();
        for (InpatientPackage pkg : packages) {
            results.add(toDto(pkg, loadItems(pkg)));
        }
        return results;
    }

    public InpatientPackageDto getPackage(Long id) throws Exception {
        InpatientPackage pkg = findActivePackage(id);
        return toDto(pkg, loadItems(pkg));
    }

    public InpatientPackageDto updatePackage(Long id, InpatientPackageDto dto, WebUser user) throws Exception {
        if (dto == null) {
            throw new InpatientPackageValidationException("Request body is required");
        }
        InpatientPackage pkg = findActivePackage(id);
        applyHeaderFields(pkg, dto);
        packageFacade.edit(pkg);

        List<InpatientPackageItem> existing = loadItems(pkg);
        List<InpatientPackageItem> items = applyItems(pkg, dto.getItems(), user, existing);
        recomputeTotal(pkg, items);
        // See createPackage(): flush so any newly-created item's IDENTITY id is
        // populated before it is serialized into the response DTO.
        packageFacade.flush();

        return toDto(pkg, items);
    }

    public InpatientPackageDto retirePackage(Long id, String retireComments, WebUser user) throws Exception {
        InpatientPackage pkg = findActivePackage(id);
        pkg.setRetired(true);
        pkg.setRetirer(user);
        pkg.setRetiredAt(new Date());
        pkg.setRetireComments(retireComments);
        packageFacade.edit(pkg);
        return toDto(pkg, loadItems(pkg));
    }

    // -------------------------------------------------------------------
    // Header
    // -------------------------------------------------------------------

    private void applyHeaderFields(InpatientPackage pkg, InpatientPackageDto dto) throws Exception {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new InpatientPackageValidationException("name is required");
        }
        if (dto.getAdmissionTypeId() == null) {
            throw new InpatientPackageValidationException("admissionTypeId is required");
        }
        if (dto.getRoomCategoryId() == null) {
            throw new InpatientPackageValidationException("roomCategoryId is required");
        }
        AdmissionType admissionType = admissionTypeFacade.find(dto.getAdmissionTypeId());
        if (admissionType == null || admissionType.isRetired()) {
            throw new InpatientPackageValidationException("AdmissionType not found: " + dto.getAdmissionTypeId());
        }
        RoomCategory roomCategory = roomCategoryFacade.find(dto.getRoomCategoryId());
        if (roomCategory == null || roomCategory.isRetired()) {
            throw new InpatientPackageValidationException("RoomCategory not found: " + dto.getRoomCategoryId());
        }

        pkg.setName(dto.getName().trim());
        pkg.setAdmissionType(admissionType);
        pkg.setRoomCategory(roomCategory);
        pkg.setIncludedRoomDurationHours(
                dto.getIncludedRoomDurationHours() != null ? dto.getIncludedRoomDurationHours() : 0.0);

        Map<String, Double> chargeTypeAmounts = dto.getChargeTypeAmounts() != null
                ? new LinkedHashMap<>(dto.getChargeTypeAmounts()) : new LinkedHashMap<>();
        pkg.setChargeTypeAmounts(chargeTypeAmounts);
        pkg.setFixedRoomCharge(chargeTypeAmounts.getOrDefault(InwardChargeType.RoomCharges.name(), 0.0));
    }

    // -------------------------------------------------------------------
    // Items — full-replace diff. itemDtos == null means "leave untouched".
    // -------------------------------------------------------------------

    private List<InpatientPackageItem> applyItems(InpatientPackage pkg, List<InpatientPackageItemDto> itemDtos,
            WebUser user, List<InpatientPackageItem> existing) throws Exception {
        if (itemDtos == null) {
            return existing;
        }

        Map<Long, InpatientPackageItem> existingById = new HashMap<>();
        for (InpatientPackageItem item : existing) {
            existingById.put(item.getId(), item);
        }

        Set<Long> keptIds = new HashSet<>();
        List<InpatientPackageItem> result = new ArrayList<>();

        for (InpatientPackageItemDto itemDto : itemDtos) {
            InpatientPackageItem item;
            if (itemDto.getId() != null) {
                item = existingById.get(itemDto.getId());
                if (item == null) {
                    throw new InpatientPackageValidationException(
                            "InpatientPackageItem not found on this package: " + itemDto.getId());
                }
                if (!keptIds.add(item.getId())) {
                    throw new InpatientPackageValidationException(
                            "Duplicate item id in payload: " + itemDto.getId());
                }
            } else {
                item = new InpatientPackageItem();
                item.setInpatientPackage(pkg);
                item.setCreatedAt(new Date());
                item.setCreater(user);
            }
            applyItemFields(item, itemDto);
            if (item.getId() != null) {
                packageItemFacade.edit(item);
            } else {
                packageItemFacade.create(item);
            }
            result.add(item);
        }

        for (InpatientPackageItem item : existing) {
            if (!keptIds.contains(item.getId())) {
                item.setRetired(true);
                item.setRetirer(user);
                item.setRetiredAt(new Date());
                item.setRetireComments("Removed from package via API full-replace update");
                packageItemFacade.edit(item);
            }
        }

        return result;
    }

    private void applyItemFields(InpatientPackageItem item, InpatientPackageItemDto dto) throws Exception {
        if (dto.getComponentType() == null || dto.getComponentType().trim().isEmpty()) {
            throw new InpatientPackageValidationException("componentType is required for each item");
        }
        InpatientPackageComponentType componentType;
        try {
            componentType = InpatientPackageComponentType.valueOf(dto.getComponentType().trim());
        } catch (IllegalArgumentException e) {
            throw new InpatientPackageValidationException("Invalid componentType: " + dto.getComponentType());
        }

        if (componentType == InpatientPackageComponentType.PROFESSIONAL_FEE_ROLE) {
            Speciality speciality = null;
            if (dto.getSpecialityId() != null) {
                speciality = specialityFacade.find(dto.getSpecialityId());
                if (speciality == null || speciality.isRetired()) {
                    throw new InpatientPackageValidationException("Speciality not found: " + dto.getSpecialityId());
                }
            }
            boolean hasRoleLabel = dto.getRoleLabel() != null && !dto.getRoleLabel().trim().isEmpty();
            if (speciality == null && !hasRoleLabel) {
                throw new InpatientPackageValidationException(
                        "specialityId or roleLabel is required when componentType is PROFESSIONAL_FEE_ROLE");
            }
            item.setSpeciality(speciality);
            item.setRoleLabel(hasRoleLabel ? dto.getRoleLabel().trim() : null);
            item.setItem(null);
        } else {
            if (dto.getItemId() == null) {
                throw new InpatientPackageValidationException(
                        "itemId is required for componentType " + componentType.name());
            }
            Item masterItem = itemFacade.find(dto.getItemId());
            if (masterItem == null || masterItem.isRetired()) {
                throw new InpatientPackageValidationException("Item not found: " + dto.getItemId());
            }
            item.setItem(masterItem);
            item.setSpeciality(null);
            item.setRoleLabel(null);
        }

        if (dto.getQty() == null || dto.getQty() <= 0) {
            throw new InpatientPackageValidationException("qty must be greater than zero");
        }
        if (dto.getFixedPrice() == null || dto.getFixedPrice() < 0) {
            throw new InpatientPackageValidationException("fixedPrice must be zero or greater");
        }

        item.setComponentType(componentType);
        item.setQty(dto.getQty());
        item.setFixedPrice(dto.getFixedPrice());
    }

    // -------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------

    private InpatientPackage findActivePackage(Long id) throws Exception {
        if (id == null) {
            throw new InpatientPackageValidationException("id is required");
        }
        InpatientPackage pkg = packageFacade.find(id);
        if (pkg == null || pkg.isRetired()) {
            throw new Exception("InpatientPackage not found: " + id);
        }
        return pkg;
    }

    private List<InpatientPackageItem> loadItems(InpatientPackage pkg) {
        Map<String, Object> params = new HashMap<>();
        params.put("pkg", pkg);
        return packageItemFacade.findByJpql(
                "SELECT i FROM InpatientPackageItem i WHERE i.retired = false AND i.inpatientPackage = :pkg "
                + "ORDER BY i.componentType, i.id",
                params);
    }

    private void recomputeTotal(InpatientPackage pkg, List<InpatientPackageItem> items) {
        Map<String, Double> chargeTypeAmounts = pkg.getChargeTypeAmounts() != null
                ? pkg.getChargeTypeAmounts() : new LinkedHashMap<>();
        double total = InpatientPackagePricing.calculateTotalPrice(chargeTypeAmounts, items);
        pkg.setTotalPrice(total);
        packageFacade.edit(pkg);
    }

    // -------------------------------------------------------------------
    // DTO mapping
    // -------------------------------------------------------------------

    private InpatientPackageDto toDto(InpatientPackage pkg, List<InpatientPackageItem> items) {
        InpatientPackageDto dto = new InpatientPackageDto();
        dto.setId(pkg.getId());
        dto.setName(pkg.getName());
        dto.setAdmissionTypeId(pkg.getAdmissionType() != null ? pkg.getAdmissionType().getId() : null);
        dto.setRoomCategoryId(pkg.getRoomCategory() != null ? pkg.getRoomCategory().getId() : null);
        dto.setIncludedRoomDurationHours(pkg.getIncludedRoomDurationHours());
        dto.setFixedRoomCharge(pkg.getFixedRoomCharge());
        dto.setChargeTypeAmounts(pkg.getChargeTypeAmounts() != null
                ? new LinkedHashMap<>(pkg.getChargeTypeAmounts()) : new LinkedHashMap<>());
        dto.setTotalPrice(pkg.getTotalPrice());
        List<InpatientPackageItemDto> itemDtos = new ArrayList<>();
        for (InpatientPackageItem item : items) {
            itemDtos.add(toDto(item));
        }
        dto.setItems(itemDtos);
        return dto;
    }

    private InpatientPackageItemDto toDto(InpatientPackageItem item) {
        InpatientPackageItemDto dto = new InpatientPackageItemDto();
        dto.setId(item.getId());
        dto.setComponentType(item.getComponentType() != null ? item.getComponentType().name() : null);
        dto.setItemId(item.getItem() != null ? item.getItem().getId() : null);
        dto.setSpecialityId(item.getSpeciality() != null ? item.getSpeciality().getId() : null);
        dto.setRoleLabel(item.getRoleLabel());
        dto.setQty(item.getQty());
        dto.setFixedPrice(item.getFixedPrice());
        return dto;
    }
}
