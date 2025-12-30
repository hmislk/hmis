package com.divudi.ws.report;

import com.divudi.core.entity.lab.InvestigationItemValue;
import com.divudi.core.entity.lab.PatientReportItemValue;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ViewReportItemValueResponseDTO {
    private String investigationItemName;
    private String strValue;
    private String lobValue;
    private String ixItemType;
    private String ixItemValueType;
    private List<String> itemValueList;

    public static ViewReportItemValueResponseDTO fromPatientReportItemValue(
            final PatientReportItemValue patientReportItemValue) {

        ViewReportItemValueResponseDTO dto = new ViewReportItemValueResponseDTO();

        if (patientReportItemValue == null) {
            return dto;
        }

        if (patientReportItemValue.getInvestigationItem() != null) {
            dto.investigationItemName = patientReportItemValue.getInvestigationItem().getName();
        } else {
            dto.investigationItemName = null;
        }

        dto.strValue = patientReportItemValue.getStrValue();
        dto.lobValue = patientReportItemValue.getLobValue();
        dto.ixItemType = patientReportItemValue.getInvestigationItem() != null
                ? patientReportItemValue.getInvestigationItem().getIxItemType() != null
                    ? patientReportItemValue.getInvestigationItem().getIxItemType().name()
                    : null
                : null;
        dto.ixItemValueType = patientReportItemValue.getInvestigationItem() != null
                ? patientReportItemValue.getInvestigationItem().getIxItemValueType() != null
                    ? patientReportItemValue.getInvestigationItem().getIxItemValueType().name()
                    : null
                : null;

        if (patientReportItemValue.getInvestigationItem() != null &&
                patientReportItemValue.getInvestigationItem().getInvestigationItemValues() != null) {

            dto.itemValueList = patientReportItemValue.getInvestigationItem()
                    .getInvestigationItemValues()
                    .stream()
                    .map(InvestigationItemValue::getName)
                    .collect(Collectors.toList());
        } else {
            dto.itemValueList = Collections.emptyList();
        }

        return dto;
    }

    public static List<ViewReportItemValueResponseDTO> fromPatientReportItemValues(final List<PatientReportItemValue> patientReportItemValues) {
        return patientReportItemValues.stream()
                .map(ViewReportItemValueResponseDTO::fromPatientReportItemValue)
                .collect(Collectors.toList());
    }

    public String getInvestigationItemName() {
        return investigationItemName;
    }

    public void setInvestigationItemName(String investigationItemName) {
        this.investigationItemName = investigationItemName;
    }

    public String getStrValue() {
        return strValue;
    }

    public void setStrValue(String strValue) {
        this.strValue = strValue;
    }

    public List<String> getItemValueList() {
        return itemValueList;
    }

    public void setItemValueList(List<String> itemValueList) {
        this.itemValueList = itemValueList;
    }

    public String getLobValue() {
        return lobValue;
    }

    public void setLobValue(String lobValue) {
        this.lobValue = lobValue;
    }

    public String getIxItemType() {
        return ixItemType;
    }

    public void setIxItemType(String ixItemType) {
        this.ixItemType = ixItemType;
    }

    public String getIxItemValueType() {
        return ixItemValueType;
    }

    public void setIxItemValueType(String ixItemValueType) {
        this.ixItemValueType = ixItemValueType;
    }
}
