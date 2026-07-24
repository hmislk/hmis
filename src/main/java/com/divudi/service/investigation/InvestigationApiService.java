package com.divudi.service.investigation;

import com.divudi.core.data.InvestigationReportType;
import com.divudi.core.data.dto.investigation.*;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.lab.InvestigationCategory;
import com.divudi.core.entity.lab.InvestigationTube;
import com.divudi.core.entity.lab.Machine;
import com.divudi.core.entity.lab.Sample;
import com.divudi.core.facade.InvestigationCategoryFacade;
import com.divudi.core.facade.InvestigationFacade;
import com.divudi.core.facade.InvestigationTubeFacade;
import com.divudi.core.facade.MachineFacade;
import com.divudi.core.facade.SampleFacade;
import com.divudi.core.util.CommonFunctions;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.*;

@Stateless
public class InvestigationApiService implements Serializable {
    @EJB private InvestigationFacade investigationFacade;
    @EJB private InvestigationCategoryFacade investigationCategoryFacade;
    @EJB private SampleFacade sampleFacade;
    @EJB private InvestigationTubeFacade investigationTubeFacade;
    @EJB private MachineFacade machineFacade;

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
        i.setVatable(Boolean.TRUE.equals(req.getVatable()));
        validateVatPercentage(req.getVatPercentage());
        if (req.getVatPercentage() != null) i.setVatPercentage(req.getVatPercentage());
        if (req.getReportType() != null && !req.getReportType().trim().isEmpty()) i.setReportType(InvestigationReportType.valueOf(req.getReportType().trim()));
        InvestigationCategory category = resolveCategory(req.getCategoryId(), req.getCategoryName(), user);
        if (category != null) i.setCategory(category);
        Sample sample = resolveSample(req.getSampleId(), req.getSampleName(), user);
        if (sample != null) i.setSample(sample);
        InvestigationTube container = resolveContainer(req.getContainerId(), req.getContainerName(), user);
        if (container != null) i.setInvestigationTube(container);
        Machine analyzer = resolveAnalyzer(req.getAnalyzerId(), req.getAnalyzerName(), user);
        if (analyzer != null) i.setMachine(analyzer);
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
        if (req.getVatable() != null) i.setVatable(req.getVatable());
        validateVatPercentage(req.getVatPercentage());
        if (req.getVatPercentage() != null) i.setVatPercentage(req.getVatPercentage());
        if (req.getReportType() != null && !req.getReportType().trim().isEmpty()) i.setReportType(InvestigationReportType.valueOf(req.getReportType().trim()));
        if (req.getCategoryId() != null || req.getCategoryName() != null) {
            i.setCategory(resolveCategory(req.getCategoryId(), req.getCategoryName(), user));
        }
        if (req.getSampleId() != null || req.getSampleName() != null) {
            i.setSample(resolveSample(req.getSampleId(), req.getSampleName(), user));
        }
        if (req.getContainerId() != null || req.getContainerName() != null) {
            i.setInvestigationTube(resolveContainer(req.getContainerId(), req.getContainerName(), user));
        }
        if (req.getAnalyzerId() != null || req.getAnalyzerName() != null) {
            i.setMachine(resolveAnalyzer(req.getAnalyzerId(), req.getAnalyzerName(), user));
        }
        i.setEditer(user); i.setEditedAt(new Date()); investigationFacade.edit(i);
        return toResponse(i, "Investigation updated successfully");
    }

    public InvestigationResponseDTO setActive(Long id, boolean inactive, WebUser user) throws Exception {
        Investigation i = load(id); i.setInactive(inactive); i.setEditer(user); i.setEditedAt(new Date()); investigationFacade.edit(i);
        return toResponse(i, inactive ? "Investigation deactivated successfully" : "Investigation activated successfully");
    }

    private Investigation load(Long id) throws Exception { Investigation i = investigationFacade.find(id); if (i == null || i.isRetired()) throw new Exception("Investigation not found with ID: " + id); return i; }

    private InvestigationCategory resolveCategory(Long id, String name, WebUser user) throws Exception {
        if (id != null) {
            InvestigationCategory c = investigationCategoryFacade.find(id);
            if (c == null || c.isRetired()) throw new Exception("Category not found with ID: " + id);
            return c;
        }
        if (name != null && !name.trim().isEmpty()) {
            Map<String, Object> m = new HashMap<>();
            m.put("ret", false);
            m.put("name", name.trim());
            InvestigationCategory c = investigationCategoryFacade.findFirstByJpql(
                    "select c from InvestigationCategory c where c.retired=:ret and c.name=:name order by c.name", m);
            if (c == null) {
                c = new InvestigationCategory();
                c.setName(name.trim());
                c.setCreatedAt(new Date());
                c.setCreater(user);
                investigationCategoryFacade.createAndFlush(c);
            }
            return c;
        }
        return null;
    }

    private Sample resolveSample(Long id, String name, WebUser user) throws Exception {
        if (id != null) {
            Sample s = sampleFacade.find(id);
            if (s == null || s.isRetired()) throw new Exception("Sample not found with ID: " + id);
            return s;
        }
        if (name != null && !name.trim().isEmpty()) {
            Map<String, Object> m = new HashMap<>();
            m.put("ret", false);
            m.put("name", name.trim());
            Sample s = sampleFacade.findFirstByJpql(
                    "select s from Sample s where s.retired=:ret and s.name=:name order by s.name", m);
            if (s == null) {
                s = new Sample();
                s.setName(name.trim());
                s.setCreatedAt(new Date());
                s.setCreater(user);
                sampleFacade.createAndFlush(s);
            }
            return s;
        }
        return null;
    }

    private InvestigationTube resolveContainer(Long id, String name, WebUser user) throws Exception {
        if (id != null) {
            InvestigationTube t = investigationTubeFacade.find(id);
            if (t == null || t.isRetired()) throw new Exception("Container not found with ID: " + id);
            return t;
        }
        if (name != null && !name.trim().isEmpty()) {
            Map<String, Object> m = new HashMap<>();
            m.put("ret", false);
            m.put("name", name.trim());
            InvestigationTube t = investigationTubeFacade.findFirstByJpql(
                    "select t from InvestigationTube t where t.retired=:ret and t.name=:name order by t.name", m);
            if (t == null) {
                t = new InvestigationTube();
                t.setName(name.trim());
                t.setCreatedAt(new Date());
                t.setCreater(user);
                investigationTubeFacade.createAndFlush(t);
            }
            return t;
        }
        return null;
    }

    private Machine resolveAnalyzer(Long id, String name, WebUser user) throws Exception {
        if (id != null) {
            Machine ma = machineFacade.find(id);
            if (ma == null || ma.isRetired()) throw new Exception("Analyzer not found with ID: " + id);
            return ma;
        }
        if (name != null && !name.trim().isEmpty()) {
            Map<String, Object> m = new HashMap<>();
            m.put("ret", false);
            m.put("name", name.trim());
            Machine ma = machineFacade.findFirstByJpql(
                    "select ma from Machine ma where ma.retired=:ret and ma.name=:name order by ma.name", m);
            if (ma == null) {
                ma = new Machine();
                ma.setName(name.trim());
                ma.setCreatedAt(new Date());
                ma.setCreater(user);
                machineFacade.createAndFlush(ma);
            }
            return ma;
        }
        return null;
    }

    private void validateVatPercentage(Double vatPercentage) throws Exception {
        if (vatPercentage != null && (vatPercentage < 0 || vatPercentage > 100)) {
            throw new Exception("vatPercentage must be between 0 and 100");
        }
    }
    private InvestigationSearchResultDTO toSearch(Investigation i) {
        InvestigationSearchResultDTO dto = new InvestigationSearchResultDTO(i.getId(), i.getName(), i.getCode(), i.getPrintName(), i.isInactive(), i.getReportType() != null ? i.getReportType().name() : null, i.isBypassSampleWorkflow());
        dto.setVatable(i.isVatable());
        dto.setVatPercentage(i.getVatPercentage());
        populateLinks(dto, i);
        return dto;
    }
    private InvestigationResponseDTO toResponse(Investigation i, String m) {
        InvestigationResponseDTO dto = new InvestigationResponseDTO(i.getId(), i.getName(), i.getCode(), i.getPrintName(), i.isInactive(), i.getReportType() != null ? i.getReportType().name() : null, i.isBypassSampleWorkflow(), m);
        dto.setVatable(i.isVatable());
        dto.setVatPercentage(i.getVatPercentage());
        populateLinks(dto, i);
        return dto;
    }
    private void populateLinks(InvestigationSearchResultDTO dto, Investigation i) {
        if (i.getCategory() != null) {
            dto.setCategoryId(i.getCategory().getId());
            dto.setCategoryName(i.getCategory().getName());
        }
        if (i.getSample() != null) {
            dto.setSampleId(i.getSample().getId());
            dto.setSampleName(i.getSample().getName());
        }
        if (i.getInvestigationTube() != null) {
            dto.setContainerId(i.getInvestigationTube().getId());
            dto.setContainerName(i.getInvestigationTube().getName());
        }
        if (i.getMachine() != null) {
            dto.setAnalyzerId(i.getMachine().getId());
            dto.setAnalyzerName(i.getMachine().getName());
        }
    }
}
