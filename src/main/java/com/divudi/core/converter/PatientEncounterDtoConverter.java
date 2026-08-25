package com.divudi.core.converter;

import com.divudi.core.data.dto.PatientEncounterDto;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.facade.PatientEncounterFacade;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@ApplicationScoped
public class PatientEncounterDtoConverter implements Converter<PatientEncounterDto> {

    @Inject
    private PatientEncounterFacade patientEncounterFacade;

    @Override
    public String getAsString(FacesContext context, UIComponent component, PatientEncounterDto dto) {
        if (dto == null || dto.getId() == null) {
            return "";
        }
        return String.valueOf(dto.getId());
    }

    @Override
    public PatientEncounterDto getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            Long id = Long.parseLong(value.trim());
            PatientEncounter pe = patientEncounterFacade.find(id);
            return pe != null ? new PatientEncounterDto(pe) : null;
        } catch (Exception e) {
            return null;
        }
    }
}