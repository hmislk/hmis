package com.divudi.service;

import com.divudi.core.data.dto.PaymentSchemeResponseDto;
import com.divudi.core.data.dto.PaymentSchemeUpdateDto;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.facade.PaymentSchemeFacade;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
public class PaymentSchemeApiService {

    @EJB
    private PaymentSchemeFacade paymentSchemeFacade;

    public List<PaymentSchemeResponseDto> list(String query, int limit) {
        StringBuilder jpql = new StringBuilder(
                "select s from PaymentScheme s where s.retired = false");
        Map<String, Object> params = new HashMap<>();

        if (query != null && !query.trim().isEmpty()) {
            jpql.append(" and upper(s.name) like :q");
            params.put("q", "%" + query.trim().toUpperCase() + "%");
        }

        jpql.append(" order by s.orderNo, s.name");

        List<PaymentScheme> rows = paymentSchemeFacade.findByJpql(jpql.toString(), params, limit);
        List<PaymentSchemeResponseDto> result = new ArrayList<>();
        if (rows != null) {
            for (PaymentScheme s : rows) {
                result.add(toDto(s));
            }
        }
        return result;
    }

    public PaymentSchemeResponseDto update(Long id, PaymentSchemeUpdateDto req) throws Exception {
        PaymentScheme s = paymentSchemeFacade.find(id);
        if (s == null || s.isRetired()) {
            throw new Exception("PaymentScheme not found or retired: " + id);
        }

        if (req.getName() != null) {
            if (req.getName().trim().isEmpty()) throw new Exception("name must not be blank");
            s.setName(req.getName());
        }
        if (req.getPrintingName() != null) s.setPrintingName(req.getPrintingName());
        if (req.getValidForPharmacy() != null) s.setValidForPharmacy(req.getValidForPharmacy());
        if (req.getValidForBilledBills() != null) s.setValidForBilledBills(req.getValidForBilledBills());
        if (req.getValidForInpatientBills() != null) s.setValidForInpatientBills(req.getValidForInpatientBills());
        if (req.getValidForChanneling() != null) s.setValidForChanneling(req.getValidForChanneling());
        if (req.getStaffMemberRequired() != null) s.setStaffMemberRequired(req.getStaffMemberRequired());
        if (req.getMembershipRequired() != null) s.setMembershipRequired(req.getMembershipRequired());
        if (req.getStaffRequired() != null) s.setStaffRequired(req.getStaffRequired());
        if (req.getStaffOrFamilyRequired() != null) s.setStaffOrFamilyRequired(req.getStaffOrFamilyRequired());
        if (req.getMemberRequired() != null) s.setMemberRequired(req.getMemberRequired());
        if (req.getMemberOrFamilyRequired() != null) s.setMemberOrFamilyRequired(req.getMemberOrFamilyRequired());
        if (req.getSeniorCitizenRequired() != null) s.setSeniorCitizenRequired(req.getSeniorCitizenRequired());
        if (req.getPregnantMotherRequired() != null) s.setPregnantMotherRequired(req.getPregnantMotherRequired());
        if (req.getOrderNo() != null) s.setOrderNo(req.getOrderNo());

        paymentSchemeFacade.edit(s);
        return toDto(s);
    }

    private PaymentSchemeResponseDto toDto(PaymentScheme s) {
        PaymentSchemeResponseDto dto = new PaymentSchemeResponseDto();
        dto.setId(s.getId());
        dto.setName(s.getName());
        dto.setPrintingName(s.getPrintingName());
        dto.setValidForPharmacy(s.isValidForPharmacy());
        dto.setValidForBilledBills(s.isValidForBilledBills());
        dto.setValidForInpatientBills(s.isValidForInpatientBills());
        dto.setValidForChanneling(s.isValidForChanneling());
        dto.setStaffMemberRequired(s.isStaffMemberRequired());
        dto.setMembershipRequired(s.isMembershipRequired());
        dto.setStaffRequired(s.isStaffRequired());
        dto.setStaffOrFamilyRequired(s.isStaffOrFamilyRequired());
        dto.setMemberRequired(s.isMemberRequired());
        dto.setMemberOrFamilyRequired(s.isMemberOrFamilyRequired());
        dto.setSeniorCitizenRequired(s.isSeniorCitizenRequired());
        dto.setPregnantMotherRequired(s.isPregnantMotherRequired());
        dto.setRetired(s.isRetired());
        dto.setOrderNo(s.getOrderNo());
        return dto;
    }
}
