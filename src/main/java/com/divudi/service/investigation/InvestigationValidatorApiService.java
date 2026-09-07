package com.divudi.service.investigation;

import com.divudi.core.data.dto.investigation.InvestigationValidatorDTO;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.lab.InvestigationValidator;
import com.divudi.core.facade.InvestigationFacade;
import com.divudi.core.facade.InvestigationValidatorFacade;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API service for Investigation result-validator (min/max range) CRUD.
 * See issue #22364.
 *
 * The {@code InvestigationValidaterComponent} relation (and both directions
 * of the bidirectional link between it and {@code InvestigationValidator})
 * is dead code — no bean or xhtml in the app ever reads or writes it, and
 * its facade is never injected anywhere. Confirmed by repo-wide search
 * before implementing this API; deliberately not exposed here. The live UI
 * (`InvestigationValidatorComponentController.addNewValidator()`) always
 * sets {@code InvestigationValidator.item} to the investigation itself, so
 * this service does the same rather than accepting a separate item
 * reference from the client.
 *
 * @author Buddhika
 */
@Stateless
public class InvestigationValidatorApiService implements Serializable {

    @EJB
    private InvestigationFacade investigationFacade;
    @EJB
    private InvestigationValidatorFacade investigationValidatorFacade;

    public List<InvestigationValidatorDTO> listValidators(Long investigationId) throws Exception {
        loadInvestigation(investigationId);
        Map<String, Object> m = new HashMap<>();
        m.put("ixId", investigationId);
        String j = "select v from InvestigationValidator v where v.item.id=:ixId and v.retired=false order by v.name";
        List<InvestigationValidator> rows = investigationValidatorFacade.findByJpql(j, m, TemporalType.TIMESTAMP);
        List<InvestigationValidatorDTO> out = new ArrayList<>();
        for (InvestigationValidator row : rows) {
            out.add(toDTO(row, null));
        }
        return out;
    }

    public InvestigationValidatorDTO createValidator(Long investigationId, InvestigationValidatorDTO req, WebUser user) throws Exception {
        if (req == null || req.getName() == null || req.getName().trim().isEmpty()) {
            throw new Exception("name is required");
        }
        Investigation ix = loadInvestigation(investigationId);
        InvestigationValidator v = new InvestigationValidator();
        v.setItem(ix);
        v.setName(req.getName().trim());
        v.setMaximumValue(req.getMaximumValue());
        v.setMinimumValue(req.getMinimumValue());
        validateRange(v.getMinimumValue(), v.getMaximumValue());
        v.setCreater(user);
        v.setCreatedAt(new Date());
        investigationValidatorFacade.createAndFlush(v);
        return toDTO(v, "Validator created successfully");
    }

    public InvestigationValidatorDTO updateValidator(Long investigationId, Long validatorId, InvestigationValidatorDTO req, WebUser user) throws Exception {
        InvestigationValidator v = loadValidator(validatorId, investigationId);
        if (req == null) {
            throw new Exception("Request body is required");
        }
        // Validate the prospective merged range before mutating the managed
        // entity — `v` is JPA-managed here, so any setter call is dirty-tracked
        // and will be flushed at transaction commit even if this method later
        // throws, since a checked Exception does not trigger EJB CMT rollback.
        Double effectiveMax = req.getMaximumValue() != null ? req.getMaximumValue() : v.getMaximumValue();
        Double effectiveMin = req.getMinimumValue() != null ? req.getMinimumValue() : v.getMinimumValue();
        validateRange(effectiveMin, effectiveMax);

        if (req.getName() != null && !req.getName().trim().isEmpty()) {
            v.setName(req.getName().trim());
        }
        if (req.getMaximumValue() != null) {
            v.setMaximumValue(req.getMaximumValue());
        }
        if (req.getMinimumValue() != null) {
            v.setMinimumValue(req.getMinimumValue());
        }
        v.setEditor(user);
        v.setEditedAt(new Date());
        investigationValidatorFacade.edit(v);
        return toDTO(v, "Validator updated successfully");
    }

    public InvestigationValidatorDTO deleteValidator(Long investigationId, Long validatorId, WebUser user) throws Exception {
        InvestigationValidator v = loadValidator(validatorId, investigationId);
        v.setRetired(true);
        v.setRetirer(user);
        v.setRetiredAt(new Date());
        investigationValidatorFacade.edit(v);
        return toDTO(v, "Validator deleted successfully");
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void validateRange(Double minimumValue, Double maximumValue) throws Exception {
        if (minimumValue != null && maximumValue != null && minimumValue > maximumValue) {
            throw new Exception("minimumValue cannot exceed maximumValue");
        }
    }

    private Investigation loadInvestigation(Long id) throws Exception {
        Investigation ix = investigationFacade.find(id);
        if (ix == null || ix.isRetired()) {
            throw new Exception("Investigation not found with ID: " + id);
        }
        return ix;
    }

    private InvestigationValidator loadValidator(Long validatorId, Long investigationId) throws Exception {
        Investigation investigation = loadInvestigation(investigationId);
        InvestigationValidator v = investigationValidatorFacade.find(validatorId);
        if (v == null || v.isRetired()) {
            throw new Exception("Validator not found with ID: " + validatorId);
        }
        if (v.getItem() == null || !investigation.getId().equals(v.getItem().getId())) {
            throw new Exception("Validator " + validatorId + " does not belong to investigation " + investigationId);
        }
        return v;
    }

    private InvestigationValidatorDTO toDTO(InvestigationValidator v, String msg) {
        InvestigationValidatorDTO dto = new InvestigationValidatorDTO();
        dto.setId(v.getId());
        if (v.getItem() != null) {
            dto.setInvestigationId(v.getItem().getId());
        }
        dto.setName(v.getName());
        dto.setMaximumValue(v.getMaximumValue());
        dto.setMinimumValue(v.getMinimumValue());
        dto.setMessage(msg);
        return dto;
    }
}
