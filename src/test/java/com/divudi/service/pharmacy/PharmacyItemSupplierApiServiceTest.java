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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PharmacyItemSupplierApiServiceTest {

    private PharmacyItemSupplierApiService service;
    private DummyItemsDistributorsFacade dummyItemsDistributorsFacade;
    private DummyInstitutionFacade dummyInstitutionFacade;
    private DummyItemFacade dummyItemFacade;
    private DummyAuditService dummyAuditService;

    private Institution validDealer;
    private Institution nonDealer;
    private Institution retiredDealer;
    private Item validItem;
    private Item retiredItem;
    private WebUser user;

    private static class DummyItemsDistributorsFacade extends ItemsDistributorsFacade {
        List<ItemsDistributors> saved = new ArrayList<>();
        Map<Long, ItemsDistributors> db = new HashMap<>();
        long idSeq = 1;

        @Override protected EntityManager getEntityManager() { return null; }

        @Override
        public void create(ItemsDistributors entity) {
            if (entity.getId() == null) {
                entity.setId(idSeq++);
            }
            saved.add(entity);
            db.put(entity.getId(), entity);
        }

        @Override
        public void createAndFlush(ItemsDistributors entity) {
            create(entity);
        }

        @Override
        public void edit(ItemsDistributors entity) {
            db.put(entity.getId(), entity);
        }

        @Override
        public ItemsDistributors find(Object id) {
            return db.get(id);
        }

        @Override
        public ItemsDistributors findFirstByJpql(String jpql, Map<String, Object> params) {
            Institution ins = (Institution) params.get("ins");
            Item itm = (Item) params.get("item");
            for (ItemsDistributors id : db.values()) {
                if (!id.isRetired()
                        && id.getInstitution() != null && id.getInstitution().getId().equals(ins.getId())
                        && id.getItem() != null && id.getItem().getId().equals(itm.getId())) {
                    return id;
                }
            }
            return null;
        }

        @Override
        public List<ItemsDistributors> findByJpqlWithRange(String jpql, Map<String, Object> params, int startPosition, int maxResults) {
            List<ItemsDistributors> res = new ArrayList<>();
            Long supId = params != null ? (Long) params.get("supplierId") : null;
            Long itmId = params != null ? (Long) params.get("itemId") : null;

            for (ItemsDistributors id : db.values()) {
                if (id.isRetired()) continue;
                if (id.getItem() != null && id.getItem().isRetired()) continue;
                if (supId != null && (id.getInstitution() == null || !id.getInstitution().getId().equals(supId))) continue;
                if (itmId != null && (id.getItem() == null || !id.getItem().getId().equals(itmId))) continue;
                res.add(id);
            }
            return res;
        }
    }

    private static class DummyInstitutionFacade extends InstitutionFacade {
        Map<Long, Institution> db = new HashMap<>();
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public Institution find(Object id) { return db.get(id); }
    }

    private static class DummyItemFacade extends ItemFacade {
        Map<Long, Item> db = new HashMap<>();
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public Item find(Object id) { return db.get(id); }
    }

    private static class DummyAuditService extends AuditService {
        static class AuditCall {
            Object before;
            Object after;
            WebUser user;
            String entityType;
            String eventTrigger;
            Long objectId;

            AuditCall(Object before, Object after, WebUser user, String entityType, String eventTrigger, Long objectId) {
                this.before = before;
                this.after = after;
                this.user = user;
                this.entityType = entityType;
                this.eventTrigger = eventTrigger;
                this.objectId = objectId;
            }
        }

        List<AuditCall> calls = new ArrayList<>();

        @Override
        public void logAudit(Object before, Object after, WebUser user, String entityType, String eventTrigger, Long objectId) {
            calls.add(new AuditCall(before, after, user, entityType, eventTrigger, objectId));
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        service = new PharmacyItemSupplierApiService();

        dummyItemsDistributorsFacade = new DummyItemsDistributorsFacade();
        dummyInstitutionFacade = new DummyInstitutionFacade();
        dummyItemFacade = new DummyItemFacade();

        injectField("itemsDistributorsFacade", dummyItemsDistributorsFacade);
        injectField("institutionFacade", dummyInstitutionFacade);
        injectField("itemFacade", dummyItemFacade);

        dummyAuditService = new DummyAuditService();
        injectField("auditService", dummyAuditService);

        user = new WebUser();
        user.setId(10L);
        user.setName("testuser");

        validDealer = new Institution();
        validDealer.setId(100L);
        validDealer.setName("ABC Pharmaceuticals");
        validDealer.setInstitutionType(InstitutionType.Dealer);
        validDealer.setRetired(false);
        dummyInstitutionFacade.db.put(validDealer.getId(), validDealer);

        nonDealer = new Institution();
        nonDealer.setId(101L);
        nonDealer.setName("City Hospital Agency");
        nonDealer.setInstitutionType(InstitutionType.Agency);
        nonDealer.setRetired(false);
        dummyInstitutionFacade.db.put(nonDealer.getId(), nonDealer);

        retiredDealer = new Institution();
        retiredDealer.setId(102L);
        retiredDealer.setName("Defunct Supplier");
        retiredDealer.setInstitutionType(InstitutionType.Dealer);
        retiredDealer.setRetired(true);
        dummyInstitutionFacade.db.put(retiredDealer.getId(), retiredDealer);

        validItem = new Amp();
        validItem.setId(200L);
        validItem.setName("Paracetamol 500mg Tablet");
        validItem.setCode("PARA500");
        validItem.setRetired(false);
        dummyItemFacade.db.put(validItem.getId(), validItem);

        retiredItem = new Amp();
        retiredItem.setId(201L);
        retiredItem.setName("Discontinued Syrup");
        retiredItem.setCode("DISC01");
        retiredItem.setRetired(true);
        dummyItemFacade.db.put(retiredItem.getId(), retiredItem);
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = PharmacyItemSupplierApiService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
    }

    @Test
    @DisplayName("Successfully creates item-supplier link with valid dealer and item")
    void testCreateLink_Success() {
        ItemSupplierRequestDto req = new ItemSupplierRequestDto(100L, 200L, 1);
        PharmacyItemSupplierApiService.CreateItemSupplierResult result = service.createLink(req, user);

        assertEquals(201, result.getStatus());
        assertNotNull(result.getData());
        assertEquals(100L, result.getData().getSupplierId());
        assertEquals("ABC Pharmaceuticals", result.getData().getSupplierName());
        assertEquals(200L, result.getData().getItemId());
        assertEquals("Paracetamol 500mg Tablet", result.getData().getItemName());
        assertFalse(result.getData().isRetired());

        assertEquals(1, dummyItemsDistributorsFacade.saved.size());
        ItemsDistributors saved = dummyItemsDistributorsFacade.saved.get(0);
        assertEquals(user, saved.getCreater());
        assertNotNull(saved.getCreatedAt());
        assertFalse(saved.isRetired());

        assertEquals(1, dummyAuditService.calls.size());
        DummyAuditService.AuditCall audit = dummyAuditService.calls.get(0);
        assertNull(audit.before);
        assertNotNull(audit.after);
        assertEquals(user, audit.user);
        assertEquals("ItemsDistributors", audit.entityType);
        assertEquals("CREATE_ITEM_SUPPLIER_LINK", audit.eventTrigger);
        assertEquals(saved.getId(), audit.objectId);
    }

    @Test
    @DisplayName("Rejects creation with null request or missing supplierId/itemId")
    void testCreateLink_MissingFields() {
        assertEquals(400, service.createLink(null, user).getStatus());
        assertEquals(400, service.createLink(new ItemSupplierRequestDto(null, 200L), user).getStatus());
        assertEquals(400, service.createLink(new ItemSupplierRequestDto(100L, null), user).getStatus());
    }

    @Test
    @DisplayName("Rejects creation when supplier does not exist")
    void testCreateLink_SupplierNotFound() {
        ItemSupplierRequestDto req = new ItemSupplierRequestDto(999L, 200L);
        PharmacyItemSupplierApiService.CreateItemSupplierResult result = service.createLink(req, user);
        assertEquals(404, result.getStatus());
    }

    @Test
    @DisplayName("Rejects creation when supplier is retired")
    void testCreateLink_SupplierRetired() {
        ItemSupplierRequestDto req = new ItemSupplierRequestDto(102L, 200L);
        PharmacyItemSupplierApiService.CreateItemSupplierResult result = service.createLink(req, user);
        assertEquals(400, result.getStatus());
        assertTrue(result.getMessage().contains("retired"));
    }

    @Test
    @DisplayName("Rejects creation when supplier is not a Dealer")
    void testCreateLink_SupplierNotDealer() {
        ItemSupplierRequestDto req = new ItemSupplierRequestDto(101L, 200L);
        PharmacyItemSupplierApiService.CreateItemSupplierResult result = service.createLink(req, user);
        assertEquals(400, result.getStatus());
        assertTrue(result.getMessage().contains("Dealer"));
    }

    @Test
    @DisplayName("Rejects creation when supplier is StoreDealor (only Dealer allowed for pharmacy)")
    void testCreateLink_SupplierStoreDealor() {
        Institution storeDealer = new Institution();
        storeDealer.setId(103L);
        storeDealer.setName("Hospital Store Dealer");
        storeDealer.setInstitutionType(InstitutionType.StoreDealor);
        storeDealer.setRetired(false);
        dummyInstitutionFacade.db.put(storeDealer.getId(), storeDealer);

        ItemSupplierRequestDto req = new ItemSupplierRequestDto(103L, 200L);
        PharmacyItemSupplierApiService.CreateItemSupplierResult result = service.createLink(req, user);
        assertEquals(400, result.getStatus());
        assertTrue(result.getMessage().contains("Dealer"));
    }

    @Test
    @DisplayName("Rejects creation when item does not exist")
    void testCreateLink_ItemNotFound() {
        ItemSupplierRequestDto req = new ItemSupplierRequestDto(100L, 999L);
        PharmacyItemSupplierApiService.CreateItemSupplierResult result = service.createLink(req, user);
        assertEquals(404, result.getStatus());
    }

    @Test
    @DisplayName("Rejects creation when item is retired")
    void testCreateLink_ItemRetired() {
        ItemSupplierRequestDto req = new ItemSupplierRequestDto(100L, 201L);
        PharmacyItemSupplierApiService.CreateItemSupplierResult result = service.createLink(req, user);
        assertEquals(400, result.getStatus());
        assertTrue(result.getMessage().contains("retired"));
    }

    @Test
    @DisplayName("Returns 409 Conflict when an active link already exists")
    void testCreateLink_DuplicateConflict() {
        // First create
        ItemSupplierRequestDto req = new ItemSupplierRequestDto(100L, 200L);
        PharmacyItemSupplierApiService.CreateItemSupplierResult res1 = service.createLink(req, user);
        assertEquals(201, res1.getStatus());

        // Duplicate create
        PharmacyItemSupplierApiService.CreateItemSupplierResult res2 = service.createLink(req, user);
        assertEquals(409, res2.getStatus());
        assertNotNull(res2.getData());
        assertEquals(res1.getData().getId(), res2.getData().getId());
        assertTrue(res2.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Bulk creation processes valid, duplicate, and invalid entries correctly")
    void testCreateBulk_MixedOutcomes() {
        // Pre-create link for validItem
        service.createLink(new ItemSupplierRequestDto(100L, 200L), user);

        // Add a second valid item
        Amp item2 = new Amp();
        item2.setId(300L);
        item2.setName("Amoxicillin 250mg Capsule");
        item2.setRetired(false);
        dummyItemFacade.db.put(item2.getId(), item2);

        List<ItemSupplierRequestDto> batch = Arrays.asList(
                new ItemSupplierRequestDto(100L, 300L), // new -> created (201)
                new ItemSupplierRequestDto(100L, 200L), // existing -> already_exists (409)
                new ItemSupplierRequestDto(100L, 999L), // non-existent item -> error (404)
                new ItemSupplierRequestDto(101L, 300L)  // non-dealer -> error (400)
        );

        List<BulkItemSupplierResultDto> results = service.createBulk(batch, user);
        assertEquals(4, results.size());

        assertEquals("created", results.get(0).getOutcome());
        assertEquals(201, results.get(0).getCode());
        assertNotNull(results.get(0).getId());

        assertEquals("already_exists", results.get(1).getOutcome());
        assertEquals(409, results.get(1).getCode());
        assertNotNull(results.get(1).getId());

        assertEquals("error", results.get(2).getOutcome());
        assertEquals(404, results.get(2).getCode());

        assertEquals("error", results.get(3).getOutcome());
        assertEquals(400, results.get(3).getCode());
    }

    @Test
    @DisplayName("Soft-retires link and sets audit fields")
    void testRetireLink_Success() {
        PharmacyItemSupplierApiService.CreateItemSupplierResult created =
                service.createLink(new ItemSupplierRequestDto(100L, 200L), user);
        Long linkId = created.getData().getId();

        WebUser retirer = new WebUser();
        retirer.setId(20L);
        retirer.setName("adminuser");

        PharmacyItemSupplierApiService.RetireItemSupplierResult retired =
                service.retireLink(linkId, "Supplier discontinued this item", retirer);

        assertEquals(200, retired.getStatus());
        assertTrue(retired.getData().isRetired());
        assertEquals("Supplier discontinued this item", retired.getData().getRetireComments());

        ItemsDistributors entity = dummyItemsDistributorsFacade.find(linkId);
        assertTrue(entity.isRetired());
        assertEquals(retirer, entity.getRetirer());
        assertNotNull(entity.getRetiredAt());
        assertEquals("Supplier discontinued this item", entity.getRetireComments());

        assertEquals(2, dummyAuditService.calls.size());
        DummyAuditService.AuditCall retireAudit = dummyAuditService.calls.get(1);
        assertNotNull(retireAudit.before);
        assertNotNull(retireAudit.after);
        assertEquals(retirer, retireAudit.user);
        assertEquals("ItemsDistributors", retireAudit.entityType);
        assertEquals("RETIRE_ITEM_SUPPLIER_LINK", retireAudit.eventTrigger);
        assertEquals(linkId, retireAudit.objectId);
    }

    @Test
    @DisplayName("Retire link is idempotent on already retired link")
    void testRetireLink_AlreadyRetired() {
        PharmacyItemSupplierApiService.CreateItemSupplierResult created =
                service.createLink(new ItemSupplierRequestDto(100L, 200L), user);
        Long linkId = created.getData().getId();

        service.retireLink(linkId, "First retirement", user);
        PharmacyItemSupplierApiService.RetireItemSupplierResult second =
                service.retireLink(linkId, "Second retirement", user);

        assertEquals(200, second.getStatus());
        assertTrue(second.getMessage().contains("already retired"));
    }

    @Test
    @DisplayName("Retire link returns 404 when link does not exist")
    void testRetireLink_NotFound() {
        PharmacyItemSupplierApiService.RetireItemSupplierResult res = service.retireLink(999L, "comment", user);
        assertEquals(404, res.getStatus());
    }

    @Test
    @DisplayName("Search links filters by supplier and item, ignoring retired links")
    void testSearchLinks_Filtering() {
        service.createLink(new ItemSupplierRequestDto(100L, 200L), user);

        Amp item2 = new Amp();
        item2.setId(300L);
        item2.setName("Item 2");
        item2.setRetired(false);
        dummyItemFacade.db.put(item2.getId(), item2);
        PharmacyItemSupplierApiService.CreateItemSupplierResult link2 =
                service.createLink(new ItemSupplierRequestDto(100L, 300L), user);

        // Retire link2
        service.retireLink(link2.getData().getId(), "retire", user);

        // Search for supplier 100
        List<ItemSupplierDto> list = service.searchLinks(100L, null, 10, 0);
        assertEquals(1, list.size());
        assertEquals(200L, list.get(0).getItemId());

        // Search for item 200
        List<ItemSupplierDto> itemSearch = service.searchLinks(null, 200L, 10, 0);
        assertEquals(1, itemSearch.size());

        // Search for non-existent item
        List<ItemSupplierDto> empty = service.searchLinks(null, 999L, 10, 0);
        assertEquals(0, empty.size());
    }

    @Test
    @DisplayName("Rejects creation when item is not an Amp or Ampp (e.g. generic Item or Investigation)")
    void testCreateLink_RejectsNonAmpOrAmpp() {
        Item genericItem = new Item();
        genericItem.setId(400L);
        genericItem.setName("Lab Investigation Blood Test");
        genericItem.setRetired(false);
        dummyItemFacade.db.put(genericItem.getId(), genericItem);

        ItemSupplierRequestDto req = new ItemSupplierRequestDto(100L, 400L);
        PharmacyItemSupplierApiService.CreateItemSupplierResult result = service.createLink(req, user);
        assertEquals(400, result.getStatus());
        assertTrue(result.getMessage().contains("Amp or Ampp"));
    }

    @Test
    @DisplayName("Successfully creates link when item is an Ampp")
    void testCreateLink_AmppSuccess() {
        Ampp ampp = new Ampp();
        ampp.setId(500L);
        ampp.setName("Paracetamol 500mg 100s Pack");
        ampp.setRetired(false);
        dummyItemFacade.db.put(ampp.getId(), ampp);

        ItemSupplierRequestDto req = new ItemSupplierRequestDto(100L, 500L);
        PharmacyItemSupplierApiService.CreateItemSupplierResult result = service.createLink(req, user);
        assertEquals(201, result.getStatus());
        assertEquals(500L, result.getData().getItemId());
    }

    @Test
    @DisplayName("Search links excludes active links whose item is retired")
    void testSearchLinks_ExcludesRetiredItems() {
        Amp activeAmp = new Amp();
        activeAmp.setId(601L);
        activeAmp.setName("Active Amp");
        activeAmp.setRetired(false);
        dummyItemFacade.db.put(activeAmp.getId(), activeAmp);
        service.createLink(new ItemSupplierRequestDto(100L, 601L), user);

        Amp soonToBeRetiredAmp = new Amp();
        soonToBeRetiredAmp.setId(602L);
        soonToBeRetiredAmp.setName("Retiring Amp");
        soonToBeRetiredAmp.setRetired(false);
        dummyItemFacade.db.put(soonToBeRetiredAmp.getId(), soonToBeRetiredAmp);
        service.createLink(new ItemSupplierRequestDto(100L, 602L), user);

        // Mark second item as retired in DB
        soonToBeRetiredAmp.setRetired(true);

        List<ItemSupplierDto> list = service.searchLinks(100L, null, 10, 0);
        boolean has602 = list.stream().anyMatch(dto -> dto.getItemId().equals(602L));
        assertFalse(has602);
        boolean has601 = list.stream().anyMatch(dto -> dto.getItemId().equals(601L));
        assertTrue(has601);
    }
}
