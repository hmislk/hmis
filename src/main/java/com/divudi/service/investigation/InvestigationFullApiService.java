package com.divudi.service.investigation;

import com.divudi.core.data.dto.investigation.InvestigationFullDTO;
import com.divudi.core.data.dto.investigation.InvestigationItemDTO;
import com.divudi.core.data.dto.investigation.InvestigationItemValueDTO;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only aggregation of an investigation's complete definition tree —
 * metadata, category/sample/container/analyzer linking, components, report
 * format (items, item values, calculations, flags, dynamic labels),
 * validators, and fees — into a single nested JSON document.
 *
 * Pure composition over the existing per-domain services built in
 * #22360/#22361/#22362/#22364; no new entity mutation logic. See issue
 * #22365 (supersedes #458).
 *
 * @author Buddhika
 */
@Stateless
public class InvestigationFullApiService implements Serializable {

    @EJB
    private InvestigationApiService investigationApiService;
    @EJB
    private InvestigationComponentApiService investigationComponentApiService;
    @EJB
    private InvestigationFormatApiService investigationFormatApiService;
    @EJB
    private InvestigationValidatorApiService investigationValidatorApiService;
    @EJB
    private InvestigationFeeApiService investigationFeeApiService;

    public InvestigationFullDTO getFullDefinition(Long investigationId) throws Exception {
        InvestigationFullDTO dto = new InvestigationFullDTO();
        dto.setInvestigation(investigationApiService.findById(investigationId));
        dto.setComponents(investigationComponentApiService.listComponents(investigationId));
        dto.setValidators(investigationValidatorApiService.listValidators(investigationId));
        dto.setFees(investigationFeeApiService.listFees(investigationId));

        List<InvestigationItemDTO> items = investigationFormatApiService.listItems(investigationId);
        dto.setFormatItems(items);
        List<InvestigationItemValueDTO> allValues = new ArrayList<>();
        for (InvestigationItemDTO item : items) {
            allValues.addAll(investigationFormatApiService.listValues(investigationId, item.getId()));
        }
        dto.setItemValues(allValues);
        dto.setCalculations(investigationFormatApiService.listCalculations(investigationId));
        dto.setFlags(investigationFormatApiService.listFlags(investigationId));
        dto.setDynamicLabels(investigationFormatApiService.listDynamicLabels(investigationId));

        dto.setMessage("Full investigation definition retrieved successfully");
        return dto;
    }
}
