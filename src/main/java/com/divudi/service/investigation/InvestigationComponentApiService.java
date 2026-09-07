package com.divudi.service.investigation;

import com.divudi.core.data.dto.investigation.InvestigationComponentDTO;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.lab.InvestigationComponent;
import com.divudi.core.facade.InvestigationComponentFacade;
import com.divudi.core.facade.InvestigationFacade;
import com.divudi.core.facade.InvestigationItemFacade;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
public class InvestigationComponentApiService implements Serializable {

    @EJB
    private InvestigationFacade investigationFacade;
    @EJB
    private InvestigationComponentFacade investigationComponentFacade;
    @EJB
    private InvestigationItemFacade investigationItemFacade;

    // =========================================================================
    // Components
    // =========================================================================

    public List<InvestigationComponentDTO> listComponents(Long investigationId) throws Exception {
        loadInvestigation(investigationId);
        Map<String, Object> m = new HashMap<>();
        m.put("ixId", investigationId);
        String j = "select c from InvestigationComponent c where c.investigation.id=:ixId order by c.componentName";
        List<InvestigationComponent> rows = investigationComponentFacade.findByJpql(j, m, TemporalType.TIMESTAMP);
        List<InvestigationComponentDTO> out = new ArrayList<>();
        for (InvestigationComponent row : rows) {
            out.add(toDTO(row, null));
        }
        return out;
    }

    public InvestigationComponentDTO createComponent(Long investigationId, InvestigationComponentDTO req, WebUser user) throws Exception {
        if (req == null || req.getComponentName() == null || req.getComponentName().trim().isEmpty()) {
            throw new Exception("componentName is required");
        }
        Investigation ix = loadInvestigation(investigationId);
        InvestigationComponent c = new InvestigationComponent();
        c.setInvestigation(ix);
        c.setComponentName(req.getComponentName().trim());
        investigationComponentFacade.createAndFlush(c);
        return toDTO(c, "Component created successfully");
    }

    public InvestigationComponentDTO updateComponent(Long investigationId, Long componentId, InvestigationComponentDTO req, WebUser user) throws Exception {
        if (req == null || req.getComponentName() == null || req.getComponentName().trim().isEmpty()) {
            throw new Exception("componentName is required");
        }
        InvestigationComponent c = loadComponent(componentId, investigationId);
        c.setComponentName(req.getComponentName().trim());
        investigationComponentFacade.edit(c);
        return toDTO(c, "Component updated successfully");
    }

    public InvestigationComponentDTO deleteComponent(Long investigationId, Long componentId, WebUser user) throws Exception {
        InvestigationComponent c = loadComponent(componentId, investigationId);
        Map<String, Object> m = new HashMap<>();
        m.put("compId", componentId);
        String j = "select count(i) from InvestigationItem i where i.investigationComponent.id=:compId";
        long usageCount = investigationItemFacade.findLongByJpql(j, m, TemporalType.TIMESTAMP);
        if (usageCount > 0) {
            throw new Exception("Cannot delete component: " + usageCount + " report item(s) still reference it");
        }
        InvestigationComponentDTO dto = toDTO(c, "Component deleted successfully");
        investigationComponentFacade.remove(c);
        return dto;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Investigation loadInvestigation(Long id) throws Exception {
        Investigation ix = investigationFacade.find(id);
        if (ix == null || ix.isRetired()) {
            throw new Exception("Investigation not found with ID: " + id);
        }
        return ix;
    }

    private InvestigationComponent loadComponent(Long componentId, Long investigationId) throws Exception {
        Investigation investigation = loadInvestigation(investigationId);
        InvestigationComponent c = investigationComponentFacade.find(componentId);
        if (c == null) {
            throw new Exception("Component not found with ID: " + componentId);
        }
        if (c.getInvestigation() == null || !investigation.getId().equals(c.getInvestigation().getId())) {
            throw new Exception("Component " + componentId + " does not belong to investigation " + investigationId);
        }
        return c;
    }

    private InvestigationComponentDTO toDTO(InvestigationComponent c, String msg) {
        InvestigationComponentDTO dto = new InvestigationComponentDTO();
        dto.setId(c.getId());
        if (c.getInvestigation() != null) {
            dto.setInvestigationId(c.getInvestigation().getId());
        }
        dto.setComponentName(c.getComponentName());
        dto.setMessage(msg);
        return dto;
    }
}
