package com.divudi.service.investigation;

import com.divudi.core.data.InvestigationReportType;
import com.divudi.core.data.dto.investigation.*;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.facade.InvestigationFacade;
import com.divudi.core.util.CommonFunctions;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.*;

@Stateless
public class InvestigationApiService implements Serializable {
    @EJB private InvestigationFacade investigationFacade;

    public List<InvestigationSearchResultDTO> search(String query, Boolean inactive, int limit) {
        Map<String, Object> m = new HashMap<>();
        StringBuilder j = new StringBuilder("select i from Investigation i where i.retired=false ");
        if (query != null && !query.trim().isEmpty()) {
            j.append("and (lower(i.name) like :q or lower(i.code) like :q or lower(i.printName) like :q) ");
            m.put("q", "%" + query.trim().toLowerCase() + "%");
        }
        if (inactive != null) { j.append("and i.inactive=:inactive "); m.put("inactive", inactive); }
        j.append("order by i.name");
        List<Investigation> rows = investigationFacade.findByJpql(j.toString(), m, TemporalType.TIMESTAMP, limit);
        List<InvestigationSearchResultDTO> out = new ArrayList<>();
        for (Investigation i : rows) out.add(toSearch(i));
        return out;
    }

    public InvestigationResponseDTO findById(Long id) throws Exception { return toResponse(load(id), "Investigation found successfully"); }

    public InvestigationResponseDTO create(InvestigationCreateRequestDTO req, WebUser user) throws Exception {
        if (req == null || !req.isValid()) throw new Exception("Valid request is required");
        Map<String, Object> m = new HashMap<>(); m.put("name", req.getName().trim().toLowerCase());
        List<Investigation> ex = investigationFacade.findByJpql("select i from Investigation i where i.retired=false and lower(i.name)=:name", m, TemporalType.TIMESTAMP, 1);
        if (!ex.isEmpty()) throw new IllegalStateException(String.valueOf(ex.get(0).getId()));
        Investigation i = new Investigation();
        i.setName(req.getName().trim());
        i.setCode(req.getCode() != null && !req.getCode().trim().isEmpty() ? req.getCode().trim() : CommonFunctions.nameToCode(req.getName()));
        i.setPrintName(req.getPrintName());
        i.setInactive(Boolean.TRUE.equals(req.getInactive()));
        i.setBypassSampleWorkflow(Boolean.TRUE.equals(req.getBypassSampleWorkflow()));
        if (req.getReportType() != null && !req.getReportType().trim().isEmpty()) i.setReportType(InvestigationReportType.valueOf(req.getReportType().trim()));
        i.setCreater(user); i.setCreatedAt(new Date()); i.setRetired(false);
        investigationFacade.create(i); i.setBilledAs(i); i.setReportedAs(i); investigationFacade.edit(i);
        return toResponse(i, "Investigation created successfully");
    }

    public InvestigationResponseDTO update(Long id, InvestigationUpdateRequestDTO req, WebUser user) throws Exception {
        Investigation i = load(id);
        if (req == null || !req.isValid()) throw new Exception("Valid update request is required");
        if (req.getName() != null && !req.getName().trim().isEmpty()) i.setName(req.getName().trim());
        if (req.getCode() != null) i.setCode(req.getCode().trim());
        if (req.getPrintName() != null) i.setPrintName(req.getPrintName());
        if (req.getInactive() != null) i.setInactive(req.getInactive());
        if (req.getBypassSampleWorkflow() != null) i.setBypassSampleWorkflow(req.getBypassSampleWorkflow());
        if (req.getReportType() != null && !req.getReportType().trim().isEmpty()) i.setReportType(InvestigationReportType.valueOf(req.getReportType().trim()));
        i.setEditer(user); i.setEditedAt(new Date()); investigationFacade.edit(i);
        return toResponse(i, "Investigation updated successfully");
    }

    public InvestigationResponseDTO setActive(Long id, boolean inactive, WebUser user) throws Exception {
        Investigation i = load(id); i.setInactive(inactive); i.setEditer(user); i.setEditedAt(new Date()); investigationFacade.edit(i);
        return toResponse(i, inactive ? "Investigation deactivated successfully" : "Investigation activated successfully");
    }

    private Investigation load(Long id) throws Exception { Investigation i = investigationFacade.find(id); if (i == null || i.isRetired()) throw new Exception("Investigation not found with ID: " + id); return i; }
    private InvestigationSearchResultDTO toSearch(Investigation i) { return new InvestigationSearchResultDTO(i.getId(), i.getName(), i.getCode(), i.getPrintName(), i.isInactive(), i.getReportType() != null ? i.getReportType().name() : null, i.isBypassSampleWorkflow()); }
    private InvestigationResponseDTO toResponse(Investigation i, String m) { return new InvestigationResponseDTO(i.getId(), i.getName(), i.getCode(), i.getPrintName(), i.isInactive(), i.getReportType() != null ? i.getReportType().name() : null, i.isBypassSampleWorkflow(), m); }
}
