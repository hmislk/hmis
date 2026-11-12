package com.divudi.ejb;

import com.divudi.core.data.InvestigationItemType;
import com.divudi.core.data.InvestigationItemValueType;
import com.divudi.core.entity.lab.Antibiotic;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.lab.InvestigationItem;
import com.divudi.core.facade.InvestigationFacade;
import com.divudi.core.facade.InvestigationItemFacade;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying that PatientReportBean builds antibiotic report values
 * without mutating the shared investigation master format (#25331).
 */
public class PatientReportBeanTest {

    private PatientReportBean patientReportBean;
    private StubInvestigationItemFacade stubIiFacade;
    private StubInvestigationFacade stubIxFacade;

    static class StubInvestigationItemFacade extends InvestigationItemFacade {
        InvestigationItem existingItem;
        InvestigationItem createdItem;
        int createCount = 0;

        @Override
        public InvestigationItem findFirstByJpql(String jpql, Map<String, Object> parameters) {
            return existingItem;
        }

        @Override
        public void create(InvestigationItem entity) {
            this.createdItem = entity;
            this.createCount++;
        }
    }

    static class StubInvestigationFacade extends InvestigationFacade {
        int editCount = 0;

        @Override
        public void edit(Investigation entity) {
            this.editCount++;
        }
    }

    @BeforeEach
    void setUp() {
        patientReportBean = new PatientReportBean();
        stubIiFacade = new StubInvestigationItemFacade();
        stubIxFacade = new StubInvestigationFacade();
        patientReportBean.iiFacade = stubIiFacade;
        patientReportBean.setIxFacade(stubIxFacade);
    }

    @Test
    void investigationItemForAntibiotic_whenNotFound_createsWithoutMutatingInvestigationMaster() {
        Antibiotic a = new Antibiotic();
        a.setName("Amoxicillin");

        Investigation ix = new Investigation();
        ix.setName("Culture and Sensitivity");

        InvestigationItem result = patientReportBean.investigationItemForAntibiotic(a, ix);

        assertNotNull(result);
        assertEquals("Amoxicillin", result.getName());
        assertEquals(ix, result.getItem());
        assertEquals(InvestigationItemType.Antibiotic, result.getIxItemType());
        assertEquals(InvestigationItemValueType.Varchar, result.getIxItemValueType());
        assertEquals("90%", result.getCssTop());
        assertEquals(1, stubIiFacade.createCount);
        assertSame(result, stubIiFacade.createdItem);

        // Crucial for #25331: must NOT mutate shared investigation master reportItems
        assertTrue(ix.getReportItems().isEmpty(), "Investigation reportItems must not be modified during report generation");
        assertEquals(0, stubIxFacade.editCount, "Investigation master must not be edited during report generation");
    }

    @Test
    void investigationItemForAntibiotic_whenFound_returnsExistingAndDoesNotCreateOrMutate() {
        Antibiotic a = new Antibiotic();
        a.setName("Ciprofloxacin");

        Investigation ix = new Investigation();
        ix.setName("Blood Culture");

        InvestigationItem existing = new InvestigationItem();
        existing.setName("Ciprofloxacin");
        existing.setItem(ix);
        stubIiFacade.existingItem = existing;

        InvestigationItem result = patientReportBean.investigationItemForAntibiotic(a, ix);

        assertSame(existing, result);
        assertEquals(0, stubIiFacade.createCount);
        assertEquals(0, stubIxFacade.editCount);
        assertTrue(ix.getReportItems().isEmpty());
    }
}
