package com.divudi.service.inward;

import com.divudi.core.data.dto.inward.InpatientPackageDto;
import com.divudi.core.data.dto.inward.InpatientPackageItemDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class InpatientPackageApiServiceTest {

    private static class DummyInpatientPackageFacade extends InpatientPackageFacade {
        private final Map<Long, InpatientPackage> store = new HashMap<>();
        private long nextId = 1;
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public InpatientPackage find(Object id) { return store.get(id); }
        @Override public void create(InpatientPackage entity) {
            entity.setId(nextId++);
            store.put(entity.getId(), entity);
        }
        @Override public void edit(InpatientPackage entity) { store.put(entity.getId(), entity); }
        // create() already assigns the id synchronously, so a real flush is unnecessary here;
        // this override just satisfies InpatientPackageApiService's flush() call, which would
        // otherwise NPE against this dummy's null EntityManager.
        @Override public void flush() { }
    }

    private static class DummyInpatientPackageItemFacade extends InpatientPackageItemFacade {
        private final Map<Long, InpatientPackageItem> store = new HashMap<>();
        private long nextId = 1;
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public InpatientPackageItem find(Object id) { return store.get(id); }
        @Override public void create(InpatientPackageItem entity) {
            entity.setId(nextId++);
            store.put(entity.getId(), entity);
        }
        @Override public void edit(InpatientPackageItem entity) { store.put(entity.getId(), entity); }
        @Override
        public List<InpatientPackageItem> findByJpql(String jpql, Map<String, Object> parameters) {
            InpatientPackage pkg = (InpatientPackage) parameters.get("pkg");
            List<InpatientPackageItem> result = new ArrayList<>();
            for (InpatientPackageItem item : store.values()) {
                if (!item.isRetired() && item.getInpatientPackage() != null
                        && item.getInpatientPackage().getId().equals(pkg.getId())) {
                    result.add(item);
                }
            }
            return result;
        }
    }

    private static class DummyAdmissionTypeFacade extends AdmissionTypeFacade {
        AdmissionType entity;
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public AdmissionType find(Object id) {
            return (entity != null && entity.getId().equals(id)) ? entity : null;
        }
    }

    private static class DummyRoomCategoryFacade extends RoomCategoryFacade {
        RoomCategory entity;
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public RoomCategory find(Object id) {
            return (entity != null && entity.getId().equals(id)) ? entity : null;
        }
    }

    private static class DummyItemFacade extends ItemFacade {
        Map<Long, Item> items = new HashMap<>();
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public Item find(Object id) { return items.get(id); }
    }

    private static class DummySpecialityFacade extends SpecialityFacade {
        Map<Long, Speciality> specialities = new HashMap<>();
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public Speciality find(Object id) { return specialities.get(id); }
    }

    private InpatientPackageApiService service;
    private DummyInpatientPackageFacade packageFacade;
    private DummyInpatientPackageItemFacade packageItemFacade;
    private DummyAdmissionTypeFacade admissionTypeFacade;
    private DummyRoomCategoryFacade roomCategoryFacade;
    private DummyItemFacade itemFacade;
    private DummySpecialityFacade specialityFacade;
    private WebUser user;

    @BeforeEach
    public void setUp() throws Exception {
        service = new InpatientPackageApiService();

        packageFacade = new DummyInpatientPackageFacade();
        packageItemFacade = new DummyInpatientPackageItemFacade();
        admissionTypeFacade = new DummyAdmissionTypeFacade();
        roomCategoryFacade = new DummyRoomCategoryFacade();
        itemFacade = new DummyItemFacade();
        specialityFacade = new DummySpecialityFacade();

        AdmissionType admissionType = new AdmissionType();
        admissionType.setId(1L);
        admissionTypeFacade.entity = admissionType;

        RoomCategory roomCategory = new RoomCategory();
        roomCategory.setId(1L);
        roomCategoryFacade.entity = roomCategory;

        Item serviceItem = new Item();
        serviceItem.setId(10L);
        itemFacade.items.put(10L, serviceItem);

        Speciality speciality = new Speciality();
        speciality.setId(20L);
        specialityFacade.specialities.put(20L, speciality);

        user = new WebUser();

        setField("packageFacade", packageFacade);
        setField("packageItemFacade", packageItemFacade);
        setField("admissionTypeFacade", admissionTypeFacade);
        setField("roomCategoryFacade", roomCategoryFacade);
        setField("itemFacade", itemFacade);
        setField("specialityFacade", specialityFacade);
    }

    private void setField(String name, Object value) throws Exception {
        Field f = InpatientPackageApiService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    private InpatientPackageDto baseDto() {
        InpatientPackageDto dto = new InpatientPackageDto();
        dto.setName("Normal Delivery Package");
        dto.setAdmissionTypeId(1L);
        dto.setRoomCategoryId(1L);
        dto.setIncludedRoomDurationHours(48.0);
        Map<String, Double> chargeTypeAmounts = new LinkedHashMap<>();
        chargeTypeAmounts.put(InwardChargeType.RoomCharges.name(), 5000.0);
        chargeTypeAmounts.put(InwardChargeType.NursingCharges.name(), 1000.0);
        dto.setChargeTypeAmounts(chargeTypeAmounts);
        return dto;
    }

    private InpatientPackageItemDto serviceItemDto(Long id, double qty, double fixedPrice) {
        InpatientPackageItemDto item = new InpatientPackageItemDto();
        item.setId(id);
        item.setComponentType("SERVICE");
        item.setItemId(10L);
        item.setQty(qty);
        item.setFixedPrice(fixedPrice);
        return item;
    }

    @Test
    @DisplayName("Create sets header fields, derives fixedRoomCharge, and computes totalPrice from chargeTypeAmounts + items")
    public void testCreatePackage_computesTotalPrice() throws Exception {
        InpatientPackageDto dto = baseDto();
        List<InpatientPackageItemDto> items = new ArrayList<>();
        items.add(serviceItemDto(null, 1.0, 2000.0));
        dto.setItems(items);

        InpatientPackageDto result = service.createPackage(dto, user);

        assertNotNull(result.getId());
        assertEquals(5000.0, result.getFixedRoomCharge());
        assertEquals(8000.0, result.getTotalPrice());
        assertEquals(1, result.getItems().size());
        assertNotNull(result.getItems().get(0).getId());
    }

    @Test
    @DisplayName("Create rejects a blank name")
    public void testCreatePackage_missingName_throwsValidationException() {
        InpatientPackageDto dto = baseDto();
        dto.setName("  ");
        assertThrows(InpatientPackageValidationException.class, () -> service.createPackage(dto, user));
    }

    @Test
    @DisplayName("Create rejects an unresolvable admissionTypeId")
    public void testCreatePackage_unknownAdmissionType_throwsValidationException() {
        InpatientPackageDto dto = baseDto();
        dto.setAdmissionTypeId(999L);
        assertThrows(InpatientPackageValidationException.class, () -> service.createPackage(dto, user));
    }

    @Test
    @DisplayName("PROFESSIONAL_FEE_ROLE item without speciality or roleLabel is rejected")
    public void testCreatePackage_professionalFeeRoleWithoutSpecialityOrRoleLabel_throws() {
        InpatientPackageDto dto = baseDto();
        InpatientPackageItemDto item = new InpatientPackageItemDto();
        item.setComponentType("PROFESSIONAL_FEE_ROLE");
        item.setQty(1.0);
        item.setFixedPrice(500.0);
        dto.setItems(Collections.singletonList(item));
        assertThrows(InpatientPackageValidationException.class, () -> service.createPackage(dto, user));
    }

    @Test
    @DisplayName("PROFESSIONAL_FEE_ROLE item accepts a roleLabel with no speciality")
    public void testCreatePackage_professionalFeeRoleWithRoleLabel_succeeds() throws Exception {
        InpatientPackageDto dto = baseDto();
        InpatientPackageItemDto item = new InpatientPackageItemDto();
        item.setComponentType("PROFESSIONAL_FEE_ROLE");
        item.setRoleLabel("Visiting Consultant");
        item.setQty(1.0);
        item.setFixedPrice(3000.0);
        dto.setItems(Collections.singletonList(item));

        InpatientPackageDto result = service.createPackage(dto, user);

        assertEquals(1, result.getItems().size());
        assertEquals("Visiting Consultant", result.getItems().get(0).getRoleLabel());
        assertNull(result.getItems().get(0).getItemId());
    }

    @Test
    @DisplayName("A non-professional-fee item without itemId is rejected")
    public void testCreatePackage_serviceItemWithoutItemId_throws() {
        InpatientPackageDto dto = baseDto();
        InpatientPackageItemDto item = new InpatientPackageItemDto();
        item.setComponentType("SERVICE");
        item.setQty(1.0);
        item.setFixedPrice(500.0);
        dto.setItems(Collections.singletonList(item));
        assertThrows(InpatientPackageValidationException.class, () -> service.createPackage(dto, user));
    }

    @Test
    @DisplayName("Update full-replaces items when an array is sent: existing id updates, no id creates, missing id retires")
    public void testUpdatePackage_fullReplaceDiff() throws Exception {
        InpatientPackageDto createDto = baseDto();
        List<InpatientPackageItemDto> initialItems = new ArrayList<>();
        initialItems.add(serviceItemDto(null, 1.0, 1000.0));
        initialItems.add(serviceItemDto(null, 2.0, 500.0));
        createDto.setItems(initialItems);
        InpatientPackageDto created = service.createPackage(createDto, user);

        Long keepId = created.getItems().get(0).getId();
        Long dropId = created.getItems().get(1).getId();

        InpatientPackageDto updateDto = baseDto();
        List<InpatientPackageItemDto> updatedItems = new ArrayList<>();
        updatedItems.add(serviceItemDto(keepId, 1.0, 1500.0));
        updatedItems.add(serviceItemDto(null, 1.0, 750.0));
        updateDto.setItems(updatedItems);

        InpatientPackageDto updated = service.updatePackage(created.getId(), updateDto, user);

        assertEquals(2, updated.getItems().size());
        assertTrue(updated.getItems().stream()
                .anyMatch(i -> keepId.equals(i.getId()) && i.getFixedPrice() == 1500.0));
        assertTrue(updated.getItems().stream()
                .anyMatch(i -> i.getId() != null && !i.getId().equals(keepId)));
        InpatientPackageItem droppedEntity = packageItemFacade.find(dropId);
        assertTrue(droppedEntity.isRetired());
    }

    @Test
    @DisplayName("Update with items omitted (null) leaves the existing components untouched")
    public void testUpdatePackage_omittedItems_leavesExistingUnchanged() throws Exception {
        InpatientPackageDto createDto = baseDto();
        createDto.setItems(Collections.singletonList(serviceItemDto(null, 1.0, 1000.0)));
        InpatientPackageDto created = service.createPackage(createDto, user);
        Long existingItemId = created.getItems().get(0).getId();

        InpatientPackageDto updateDto = baseDto();
        updateDto.setName("Renamed Package");
        // updateDto.items left null: header-only update

        InpatientPackageDto updated = service.updatePackage(created.getId(), updateDto, user);

        assertEquals("Renamed Package", updated.getName());
        assertEquals(1, updated.getItems().size());
        assertEquals(existingItemId, updated.getItems().get(0).getId());
        assertFalse(packageItemFacade.find(existingItemId).isRetired());
    }

    @Test
    @DisplayName("Update rejects a payload with the same item id listed twice")
    public void testUpdatePackage_duplicateItemIdInPayload_throws() throws Exception {
        InpatientPackageDto createDto = baseDto();
        createDto.setItems(Collections.singletonList(serviceItemDto(null, 1.0, 1000.0)));
        InpatientPackageDto created = service.createPackage(createDto, user);
        Long existingItemId = created.getItems().get(0).getId();

        InpatientPackageDto updateDto = baseDto();
        List<InpatientPackageItemDto> duplicated = new ArrayList<>();
        duplicated.add(serviceItemDto(existingItemId, 1.0, 1500.0));
        duplicated.add(serviceItemDto(existingItemId, 1.0, 1500.0));
        updateDto.setItems(duplicated);

        assertThrows(InpatientPackageValidationException.class,
                () -> service.updatePackage(created.getId(), updateDto, user));
    }

    @Test
    @DisplayName("Retire sets retired flag, retirer, retiredAt and comments")
    public void testRetirePackage_setsRetiredFields() throws Exception {
        InpatientPackageDto created = service.createPackage(baseDto(), user);

        InpatientPackageDto retired = service.retirePackage(created.getId(), "No longer offered", user);

        InpatientPackage entity = packageFacade.find(retired.getId());
        assertTrue(entity.isRetired());
        assertEquals("No longer offered", entity.getRetireComments());
        assertNotNull(entity.getRetiredAt());
    }

    @Test
    @DisplayName("Get on a retired package reports not found")
    public void testGetPackage_retired_notFound() throws Exception {
        InpatientPackageDto created = service.createPackage(baseDto(), user);
        service.retirePackage(created.getId(), "gone", user);

        Exception ex = assertThrows(Exception.class, () -> service.getPackage(created.getId()));
        assertTrue(ex.getMessage().contains("not found"));
    }
}
