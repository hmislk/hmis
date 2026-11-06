/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.data.dto.AmppDto;
import com.divudi.core.data.dto.pharmaceutical.AmppRequestDTO;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.MeasurementUnit;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.entity.pharmacy.Vmpp;
import com.divudi.core.facade.AmpFacade;
import com.divudi.core.facade.AmppFacade;
import com.divudi.core.facade.MeasurementUnitFacade;
import com.divudi.core.facade.VmppFacade;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PharmaceuticalItemApiServiceTest {

    private PharmaceuticalItemApiService service;
    private DummyAmppFacade dummyAmppFacade;
    private DummyVmppFacade dummyVmppFacade;
    private DummyAmpFacade dummyAmpFacade;
    private DummyMeasurementUnitFacade dummyMeasurementUnitFacade;

    private WebUser testUser;
    private Vmp testVmp;
    private Amp testAmp;
    private MeasurementUnit testUnit;
    private MeasurementUnit otherUnit;
    private Gson gson;

    private static class DummyAmppFacade extends AmppFacade {
        Map<Long, Ampp> db = new HashMap<>();
        long idSeq = 1;

        @Override protected EntityManager getEntityManager() { return null; }

        @Override
        public void create(Ampp entity) {
            if (entity.getId() == null) {
                entity.setId(idSeq++);
            }
            db.put(entity.getId(), entity);
        }

        @Override
        public void createAndFlush(Ampp entity) {
            create(entity);
        }

        @Override
        public void edit(Ampp entity) {
            db.put(entity.getId(), entity);
        }

        @Override
        public Ampp find(Object id) {
            return db.get(id);
        }

        @Override
        public Ampp findFirstByJpql(String jpql, Map<String, Object> params) {
            if (params != null && params.containsKey("n")) {
                String name = (String) params.get("n");
                for (Ampp a : db.values()) {
                    if (!a.isRetired() && a.getName() != null && a.getName().equalsIgnoreCase(name)) {
                        return a;
                    }
                }
            }
            return null;
        }
    }

    private static class DummyVmppFacade extends VmppFacade {
        Map<Long, Vmpp> db = new HashMap<>();
        List<Vmpp> flushed = new ArrayList<>();
        long idSeq = 100;

        @Override protected EntityManager getEntityManager() { return null; }

        @Override
        public void create(Vmpp entity) {
            if (entity.getId() == null) {
                entity.setId(idSeq++);
            }
            db.put(entity.getId(), entity);
        }

        @Override
        public void createAndFlush(Vmpp entity) {
            create(entity);
            flushed.add(entity);
        }

        @Override
        public Vmpp find(Object id) {
            return db.get(id);
        }


        @Override
        public void edit(Vmpp entity) {
            db.put(entity.getId(), entity);
        }

        @Override
        public Vmpp findFirstByJpql(String jpql, Map<String, Object> params) {
            if (params != null && params.containsKey("vmp") && params.containsKey("d") && params.containsKey("s")) {
                Vmp vmp = (Vmp) params.get("vmp");
                Double dbl = (Double) params.get("d");
                MeasurementUnit unit = (MeasurementUnit) params.get("s");

                for (Vmpp v : db.values()) {
                    if (!v.isRetired() && v.getVmp() != null && v.getVmp().getId().equals(vmp.getId())
                            && Double.compare(v.getDblValue(), dbl) == 0
                            && v.getPackUnit() != null && v.getPackUnit().getId().equals(unit.getId())) {
                        return v;
                    }
                }
            }
            return null;
        }
    }

    private static class DummyAmpFacade extends AmpFacade {
        Map<Long, Amp> db = new HashMap<>();
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public Amp find(Object id) { return db.get(id); }
    }

    private static class DummyMeasurementUnitFacade extends MeasurementUnitFacade {
        Map<Long, MeasurementUnit> db = new HashMap<>();
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public MeasurementUnit find(Object id) { return db.get(id); }
    }

    @BeforeEach
    void setUp() throws Exception {
        service = new PharmaceuticalItemApiService();
        gson = new Gson();

        dummyAmppFacade = new DummyAmppFacade();
        dummyVmppFacade = new DummyVmppFacade();
        dummyAmpFacade = new DummyAmpFacade();
        dummyMeasurementUnitFacade = new DummyMeasurementUnitFacade();

        injectField("amppFacade", dummyAmppFacade);
        injectField("vmppFacade", dummyVmppFacade);
        injectField("ampFacade", dummyAmpFacade);
        injectField("measurementUnitFacade", dummyMeasurementUnitFacade);

        testUser = new WebUser();
        testUser.setId(1L);
        testUser.setName("pharmacist");

        testVmp = new Vmp();
        testVmp.setId(10L);
        testVmp.setName("Paracetamol 500mg");

        testAmp = new Amp();
        testAmp.setId(20L);
        testAmp.setName("Panadol 500mg Tablet");
        testAmp.setVmp(testVmp);
        dummyAmpFacade.db.put(testAmp.getId(), testAmp);

        testUnit = new MeasurementUnit();
        testUnit.setId(30L);
        testUnit.setName("Tablets");
        dummyMeasurementUnitFacade.db.put(testUnit.getId(), testUnit);

        otherUnit = new MeasurementUnit();
        otherUnit.setId(31L);
        otherUnit.setName("Capsules");
        dummyMeasurementUnitFacade.db.put(otherUnit.getId(), otherUnit);
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = PharmaceuticalItemApiService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
    }

    @Test
    @DisplayName("createItem auto-resolves and flushes VMPP returning populated vmppId and vmppName")
    void testCreateAmpp_AutoResolvesVmpp() throws Exception {
        AmppRequestDTO req = new AmppRequestDTO();
        req.setName("Panadol 500mg 100s Pack");
        req.setAmpId(testAmp.getId());
        req.setDblValue(100.0);
        req.setPackUnitId(testUnit.getId());

        AmppDto dto = (AmppDto) service.createItem("ampp", gson.toJson(req), testUser, gson);

        assertNotNull(dto);
        assertNotNull(dto.getId());
        assertNotNull(dto.getVmppId());
        assertTrue(dto.getVmppName().contains("100"));
        assertEquals(1, dummyVmppFacade.flushed.size());
    }

    @Test
    @DisplayName("createItem with explicit vmppId links matching VMPP")
    void testCreateAmpp_ExplicitVmpp_Valid() throws Exception {
        Vmpp existingVmpp = new Vmpp();
        existingVmpp.setId(105L);
        existingVmpp.setVmp(testVmp);
        existingVmpp.setDblValue(100.0);
        existingVmpp.setPackUnit(testUnit);
        existingVmpp.setRetired(false);
        dummyVmppFacade.db.put(existingVmpp.getId(), existingVmpp);

        AmppRequestDTO req = new AmppRequestDTO();
        req.setName("Panadol 500mg 100s Pack Custom");
        req.setAmpId(testAmp.getId());
        req.setDblValue(100.0);
        req.setPackUnitId(testUnit.getId());
        req.setVmppId(existingVmpp.getId());

        AmppDto dto = (AmppDto) service.createItem("ampp", gson.toJson(req), testUser, gson);

        assertNotNull(dto);
        assertEquals(105L, dto.getVmppId());
    }

    @Test
    @DisplayName("createItem rejects retired explicit VMPP with 400")
    void testCreateAmpp_ExplicitVmpp_Retired() {
        Vmpp retiredVmpp = new Vmpp();
        retiredVmpp.setId(106L);
        retiredVmpp.setVmp(testVmp);
        retiredVmpp.setDblValue(100.0);
        retiredVmpp.setPackUnit(testUnit);
        retiredVmpp.setRetired(true);
        dummyVmppFacade.db.put(retiredVmpp.getId(), retiredVmpp);

        AmppRequestDTO req = new AmppRequestDTO();
        req.setName("Panadol 500mg Retired Pack");
        req.setAmpId(testAmp.getId());
        req.setDblValue(100.0);
        req.setPackUnitId(testUnit.getId());
        req.setVmppId(retiredVmpp.getId());

        PharmaceuticalItemApiException ex = assertThrows(PharmaceuticalItemApiException.class,
                () -> service.createItem("ampp", gson.toJson(req), testUser, gson));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("retired"));
    }

    @Test
    @DisplayName("createItem rejects explicit VMPP whose pack size differs with 400")
    void testCreateAmpp_ExplicitVmpp_PackSizeMismatch() {
        Vmpp diffSizeVmpp = new Vmpp();
        diffSizeVmpp.setId(107L);
        diffSizeVmpp.setVmp(testVmp);
        diffSizeVmpp.setDblValue(50.0); // 50 vs 100
        diffSizeVmpp.setPackUnit(testUnit);
        diffSizeVmpp.setRetired(false);
        dummyVmppFacade.db.put(diffSizeVmpp.getId(), diffSizeVmpp);

        AmppRequestDTO req = new AmppRequestDTO();
        req.setName("Panadol Pack Size Mismatch");
        req.setAmpId(testAmp.getId());
        req.setDblValue(100.0);
        req.setPackUnitId(testUnit.getId());
        req.setVmppId(diffSizeVmpp.getId());

        PharmaceuticalItemApiException ex = assertThrows(PharmaceuticalItemApiException.class,
                () -> service.createItem("ampp", gson.toJson(req), testUser, gson));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("pack size"));
    }

    @Test
    @DisplayName("createItem rejects explicit VMPP whose pack unit differs with 400")
    void testCreateAmpp_ExplicitVmpp_PackUnitMismatch() {
        Vmpp diffUnitVmpp = new Vmpp();
        diffUnitVmpp.setId(108L);
        diffUnitVmpp.setVmp(testVmp);
        diffUnitVmpp.setDblValue(100.0);
        diffUnitVmpp.setPackUnit(otherUnit); // Capsules vs Tablets
        diffUnitVmpp.setRetired(false);
        dummyVmppFacade.db.put(diffUnitVmpp.getId(), diffUnitVmpp);

        AmppRequestDTO req = new AmppRequestDTO();
        req.setName("Panadol Pack Unit Mismatch");
        req.setAmpId(testAmp.getId());
        req.setDblValue(100.0);
        req.setPackUnitId(testUnit.getId());
        req.setVmppId(diffUnitVmpp.getId());

        PharmaceuticalItemApiException ex = assertThrows(PharmaceuticalItemApiException.class,
                () -> service.createItem("ampp", gson.toJson(req), testUser, gson));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("pack unit"));
    }

    @Test
    @DisplayName("createItem rejects explicit VMPP when AMP has no VMP with 400")
    void testCreateAmpp_ExplicitVmpp_AmpWithoutVmp() {
        Amp ampNoVmp = new Amp();
        ampNoVmp.setId(21L);
        ampNoVmp.setName("Orphan Amp Without VMP");
        ampNoVmp.setVmp(null);
        dummyAmpFacade.db.put(ampNoVmp.getId(), ampNoVmp);

        Vmpp anyVmpp = new Vmpp();
        anyVmpp.setId(109L);
        anyVmpp.setVmp(testVmp);
        anyVmpp.setDblValue(100.0);
        anyVmpp.setPackUnit(testUnit);
        anyVmpp.setRetired(false);
        dummyVmppFacade.db.put(anyVmpp.getId(), anyVmpp);

        AmppRequestDTO req = new AmppRequestDTO();
        req.setName("Panadol Orphan Amp");
        req.setAmpId(ampNoVmp.getId());
        req.setDblValue(100.0);
        req.setPackUnitId(testUnit.getId());
        req.setVmppId(anyVmpp.getId());

        PharmaceuticalItemApiException ex = assertThrows(PharmaceuticalItemApiException.class,
                () -> service.createItem("ampp", gson.toJson(req), testUser, gson));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("no associated VMP"));
    }

    @Test
    @DisplayName("createItem rejects explicit VMPP from different VMP with 400")
    void testCreateAmpp_ExplicitVmpp_WrongVmp() {
        Vmp otherVmp = new Vmp();
        otherVmp.setId(11L);
        otherVmp.setName("Amoxicillin 250mg");

        Vmpp otherVmpVmpp = new Vmpp();
        otherVmpVmpp.setId(110L);
        otherVmpVmpp.setVmp(otherVmp);
        otherVmpVmpp.setDblValue(100.0);
        otherVmpVmpp.setPackUnit(testUnit);
        otherVmpVmpp.setRetired(false);
        dummyVmppFacade.db.put(otherVmpVmpp.getId(), otherVmpVmpp);

        AmppRequestDTO req = new AmppRequestDTO();
        req.setName("Panadol Wrong VMP Link");
        req.setAmpId(testAmp.getId());
        req.setDblValue(100.0);
        req.setPackUnitId(testUnit.getId());
        req.setVmppId(otherVmpVmpp.getId());

        PharmaceuticalItemApiException ex = assertThrows(PharmaceuticalItemApiException.class,
                () -> service.createItem("ampp", gson.toJson(req), testUser, gson));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("does not belong to the VMP"));
    }

    @Test
    @DisplayName("updateItem validates vmppId before mutating entity; fields are not partially saved on error")
    void testUpdateAmpp_ValidatesBeforeModifying() {
        Ampp existing = new Ampp();
        existing.setId(50L);
        existing.setName("Original Ampp Name");
        existing.setCode("ORIG001");
        existing.setAmp(testAmp);
        existing.setDblValue(100.0);
        existing.setPackUnit(testUnit);
        existing.setRetired(false);
        dummyAmppFacade.db.put(existing.getId(), existing);

        // Send a PUT trying to change name to "Mutated Name" and providing an invalid vmppId (999)
        AmppRequestDTO req = new AmppRequestDTO();
        req.setName("Mutated Name");
        req.setVmppId(999L); // non-existent VMPP

        PharmaceuticalItemApiException ex = assertThrows(PharmaceuticalItemApiException.class,
                () -> service.updateItem("ampp", existing.getId(), gson.toJson(req), testUser, gson));
        assertEquals(404, ex.getStatus());

        // Verify entity in DB was NOT modified!
        Ampp inDb = dummyAmppFacade.find(existing.getId());
        assertEquals("Original Ampp Name", inDb.getName(), "Entity name must not be mutated when validation fails");
    }

    @Test
    @DisplayName("updateItem setting dblValue to 0 clears the VMPP association")
    void testUpdateAmpp_ClearsVmppWhenDblValueZero() throws Exception {
        Vmpp vmpp = new Vmpp();
        vmpp.setId(111L);
        vmpp.setVmp(testVmp);
        vmpp.setDblValue(100.0);
        vmpp.setPackUnit(testUnit);
        vmpp.setRetired(false);
        dummyVmppFacade.db.put(vmpp.getId(), vmpp);

        Ampp existing = new Ampp();
        existing.setId(51L);
        existing.setName("Panadol Pack 100s");
        existing.setAmp(testAmp);
        existing.setDblValue(100.0);
        existing.setPackUnit(testUnit);
        existing.setVmpp(vmpp);
        existing.setRetired(false);
        dummyAmppFacade.db.put(existing.getId(), existing);

        // Update dblValue to 0.0
        AmppRequestDTO req = new AmppRequestDTO();
        req.setDblValue(0.0);

        AmppDto dto = (AmppDto) service.updateItem("ampp", existing.getId(), gson.toJson(req), testUser, gson);

        assertNotNull(dto);
        assertNull(dto.getVmppId(), "VMPP must be cleared when dblValue is set to 0");
        Ampp inDb = dummyAmppFacade.find(existing.getId());
        assertNull(inDb.getVmpp(), "Entity in DB must have vmpp cleared");
    }

    private Ampp existingAmppForRejection() {
        Ampp existing = new Ampp();
        existing.setId(61L);
        existing.setName("Rejection Pack");
        existing.setAmp(testAmp);
        existing.setDblValue(100.0);
        existing.setPackUnit(testUnit);
        dummyAmppFacade.db.put(existing.getId(), existing);
        return existing;
    }

    @Test
    @DisplayName("updateItem with a missing ampId or packUnitId changes nothing")
    void testUpdateAmpp_MissingAmpOrUnitChangesNothing() {
        Ampp existing = existingAmppForRejection();

        AmppRequestDTO badAmp = new AmppRequestDTO();
        badAmp.setName("Changed");
        badAmp.setDblValue(50.0);
        badAmp.setAmpId(99999L);
        assertThrows(PharmaceuticalItemApiException.class,
                () -> service.updateItem("ampp", existing.getId(), gson.toJson(badAmp), testUser, gson));

        AmppRequestDTO badUnit = new AmppRequestDTO();
        badUnit.setName("Changed");
        badUnit.setDblValue(50.0);
        badUnit.setPackUnitId(99999L);
        assertThrows(PharmaceuticalItemApiException.class,
                () -> service.updateItem("ampp", existing.getId(), gson.toJson(badUnit), testUser, gson));

        assertEquals("Rejection Pack", existing.getName());
        assertEquals(100.0, existing.getDblValue());
        assertTrue(dummyVmppFacade.flushed.isEmpty());
    }

    @Test
    @DisplayName("updateItem with a duplicate name creates no VMPP, because that exception does not roll back")
    void testUpdateAmpp_DuplicateNameCreatesNoVmpp() {
        Ampp existing = existingAmppForRejection();
        Ampp other = new Ampp();
        other.setId(62L);
        other.setName("OTHER PACK");
        dummyAmppFacade.db.put(other.getId(), other);

        AmppRequestDTO req = new AmppRequestDTO();
        req.setName("Other Pack");
        req.setDblValue(75.0);

        assertThrows(DuplicateItemException.class,
                () -> service.updateItem("ampp", existing.getId(), gson.toJson(req), testUser, gson));

        assertTrue(dummyVmppFacade.flushed.isEmpty(), "No VMPP may be left behind by a rejected update");
        assertEquals("Rejection Pack", existing.getName());
        assertEquals(100.0, existing.getDblValue());
    }
    // #25204: a rejected update must mark the transaction rollback-only, so the fields it
    // already changed on the managed entity are not committed with the error.
    private int[] useServiceRecordingRollback() throws Exception {
        final int[] rollbackCalls = {0};
        service = new PharmaceuticalItemApiService() {
            @Override
            void markRollbackOnly() {
                rollbackCalls[0]++;
            }
        };
        injectField("amppFacade", dummyAmppFacade);
        injectField("vmppFacade", dummyVmppFacade);
        injectField("ampFacade", dummyAmpFacade);
        injectField("measurementUnitFacade", dummyMeasurementUnitFacade);
        return rollbackCalls;
    }

    @Test
    @DisplayName("rejected VMPP update (unknown pack unit) marks the transaction rollback-only")
    void testUpdateVmpp_RejectedUpdateMarksRollback() throws Exception {
        int[] rollbackCalls = useServiceRecordingRollback();
        Vmpp existing = new Vmpp();
        existing.setId(500L);
        existing.setName("Paracetamol 500mg 10s");
        existing.setVmp(testVmp);
        existing.setDblValue(10.0);
        existing.setPackUnit(testUnit);
        dummyVmppFacade.db.put(existing.getId(), existing);

        com.divudi.core.data.dto.pharmaceutical.VmppRequestDTO req = new com.divudi.core.data.dto.pharmaceutical.VmppRequestDTO();
        req.setName("Renamed Pack");
        req.setDblValue(20.0);
        req.setPackUnitId(999L);

        Exception ex = assertThrows(Exception.class,
                () -> service.updateItem("vmpp", existing.getId(), gson.toJson(req), testUser, gson));
        assertTrue(ex.getMessage().contains("Pack unit not found"));
        assertEquals(1, rollbackCalls[0], "the transaction must be marked rollback-only");
    }

    @Test
    @DisplayName("accepted VMPP update does not mark the transaction rollback-only")
    void testUpdateVmpp_AcceptedUpdateDoesNotMarkRollback() throws Exception {
        int[] rollbackCalls = useServiceRecordingRollback();
        Vmpp existing = new Vmpp();
        existing.setId(501L);
        existing.setName("Paracetamol 500mg 20s");
        existing.setVmp(testVmp);
        existing.setDblValue(20.0);
        existing.setPackUnit(testUnit);
        dummyVmppFacade.db.put(existing.getId(), existing);

        com.divudi.core.data.dto.pharmaceutical.VmppRequestDTO req = new com.divudi.core.data.dto.pharmaceutical.VmppRequestDTO();
        req.setPackUnitId(otherUnit.getId());

        service.updateItem("vmpp", existing.getId(), gson.toJson(req), testUser, gson);
        assertEquals(otherUnit, existing.getPackUnit());
        assertEquals(0, rollbackCalls[0]);
    }

    @Test
    @DisplayName("rejected create (unknown AMP) marks the transaction rollback-only")
    void testCreateAmpp_RejectedCreateMarksRollback() throws Exception {
        int[] rollbackCalls = useServiceRecordingRollback();
        AmppRequestDTO req = new AmppRequestDTO();
        req.setName("Unknown AMP Pack");
        req.setAmpId(9999L);
        req.setDblValue(10.0);
        req.setPackUnitId(testUnit.getId());

        assertThrows(Exception.class, () -> service.createItem("ampp", gson.toJson(req), testUser, gson));
        assertEquals(1, rollbackCalls[0]);
    }
}
