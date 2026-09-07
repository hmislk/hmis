/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.investigation;

import com.divudi.core.data.dto.service.ItemFeeCreateRequestDTO;
import com.divudi.core.data.dto.service.ItemFeeDTO;
import com.divudi.core.data.dto.service.ItemFeeUpdateRequestDTO;
import com.divudi.core.data.dto.service.ServiceResponseDTO;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.facade.InvestigationFacade;
import com.divudi.service.service.ServiceApiService;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.Serializable;
import java.util.List;

/**
 * Service for Investigation Pricing/Fees API operations.
 * Delegates the actual fee CRUD to {@link ServiceApiService}, which already
 * operates generically on any {@code Item} subtype (Service, InwardService,
 * Investigation, etc.) via {@code ItemFee}. This class only adds the
 * investigation-specific existence/retired guard, matching the pattern used
 * by {@link InvestigationComponentApiService}.
 *
 * @author Buddhika
 */
@Stateless
public class InvestigationFeeApiService implements Serializable {

    @EJB
    private InvestigationFacade investigationFacade;

    @EJB
    private ServiceApiService serviceApiService;

    public List<ItemFeeDTO> listFees(Long investigationId) throws Exception {
        loadInvestigation(investigationId);
        return serviceApiService.listFees(investigationId);
    }

    public ServiceResponseDTO addFee(Long investigationId, ItemFeeCreateRequestDTO request, WebUser user) throws Exception {
        loadInvestigation(investigationId);
        return serviceApiService.addFee(investigationId, request, user);
    }

    public ServiceResponseDTO updateFee(Long investigationId, Long feeId, ItemFeeUpdateRequestDTO request, WebUser user) throws Exception {
        loadInvestigation(investigationId);
        return serviceApiService.updateFee(investigationId, feeId, request, user);
    }

    public ServiceResponseDTO removeFee(Long investigationId, Long feeId, WebUser user) throws Exception {
        loadInvestigation(investigationId);
        return serviceApiService.removeFee(investigationId, feeId, user);
    }

    private Investigation loadInvestigation(Long id) throws Exception {
        Investigation ix = investigationFacade.find(id);
        if (ix == null || ix.isRetired()) {
            throw new Exception("Investigation not found with ID: " + id);
        }
        return ix;
    }
}
