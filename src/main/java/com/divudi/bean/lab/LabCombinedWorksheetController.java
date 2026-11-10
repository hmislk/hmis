package com.divudi.bean.lab;

import com.divudi.core.data.dto.PatientInvestigationDTO;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.lab.InvestigationTube;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.lab.PatientSampleComponant;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.PatientSampleComponantFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Builds a single combined worksheet from the investigations selected on the
 * Print WorkSheet page. Only investigations of the same patient whose samples
 * are in the same tube can be combined.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Named
@ViewScoped
public class LabCombinedWorksheetController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    private PatientSampleComponantFacade patientSampleComponantFacade;

    @Inject
    private LaboratoryManagementController laboratoryManagementController;

    private List<PatientInvestigation> combinedItems;
    private InvestigationTube combinedTube;
    private List<Long> combinedSampleIds;
    private List<String> combinedBillNumbers;
    private final Map<Long, String> tubeNameCache = new HashMap<>();

    public void processCombinedWorksheet() {
        clear();

        List<PatientInvestigationDTO> selected = laboratoryManagementController.getTempSelectedDTOItems();
        if (selected == null || selected.size() < 2) {
            JsfUtil.addErrorMessage("Please select at least two investigations to combine.");
            return;
        }

        List<PatientInvestigation> items = new ArrayList<>();
        Long patientId = null;
        Long tubeId = null;
        InvestigationTube tube = null;
        Set<Long> sampleIds = new LinkedHashSet<>();
        Set<String> billNumbers = new LinkedHashSet<>();

        for (PatientInvestigationDTO dto : selected) {
            PatientInvestigation pi = patientInvestigationFacade.findWithoutCache(dto.getInvestigationId());
            if (pi == null) {
                JsfUtil.addErrorMessage("Error in PatientInvestigation");
                return;
            }
            String ixName = pi.getBillItem() != null && pi.getBillItem().getItem() != null
                    ? pi.getBillItem().getItem().getName()
                    : dto.getItemName();

            Bill bill = pi.getBillItem() == null ? null : pi.getBillItem().getBill();
            Patient patient = bill == null ? null : bill.getPatient();
            if (patient == null || patient.getId() == null) {
                JsfUtil.addErrorMessage("Patient not found for " + ixName + ".");
                return;
            }
            if (patientId == null) {
                patientId = patient.getId();
            } else if (!patientId.equals(patient.getId())) {
                JsfUtil.addErrorMessage("Only investigations of the same patient can be combined.");
                return;
            }

            Map<Long, InvestigationTube> tubesOfIx = new LinkedHashMap<>();
            for (PatientSampleComponant psc : findSampleComponents(pi)) {
                if (psc.getPatientSample() == null) {
                    continue;
                }
                sampleIds.add(psc.getPatientSample().getId());
                InvestigationTube t = psc.getPatientSample().getTube();
                if (t != null) {
                    tubesOfIx.put(t.getId(), t);
                }
            }
            if (tubesOfIx.isEmpty()) {
                JsfUtil.addErrorMessage("Sample tube not found for " + ixName + ".");
                return;
            }
            for (Map.Entry<Long, InvestigationTube> e : tubesOfIx.entrySet()) {
                if (tubeId == null) {
                    tubeId = e.getKey();
                    tube = e.getValue();
                } else if (!tubeId.equals(e.getKey())) {
                    JsfUtil.addErrorMessage("Only investigations with the same sample tube can be combined.");
                    return;
                }
            }

            if (bill.getDeptId() != null) {
                billNumbers.add(bill.getDeptId());
            }
            items.add(pi);
        }

        combinedItems = items;
        combinedTube = tube;
        combinedSampleIds = new ArrayList<>(sampleIds);
        combinedBillNumbers = new ArrayList<>(billNumbers);
        laboratoryManagementController.setSelectedItems(new ArrayList<>());
    }

    private List<PatientSampleComponant> findSampleComponents(PatientInvestigation pi) {
        String jpql = "SELECT psc "
                + " FROM PatientSampleComponant psc "
                + " WHERE psc.retired=:retired "
                + " AND psc.patientInvestigation.id=:piId";
        Map<String, Object> params = new HashMap<>();
        params.put("retired", false);
        params.put("piId", pi.getId());
        List<PatientSampleComponant> list = patientSampleComponantFacade.findByJpqlWithoutCache(jpql, params);
        return list == null ? new ArrayList<>() : list;
    }

    public String getTubeNameBySample(Long patientSampleId) {
        if (patientSampleId == null) {
            return "";
        }
        String cached = tubeNameCache.get(patientSampleId);
        if (cached != null) {
            return cached;
        }
        String jpql = "SELECT ps.tube.name "
                + " FROM PatientSample ps "
                + " WHERE ps.id=:psId";
        Map<String, Object> params = new HashMap<>();
        params.put("psId", patientSampleId);
        List<?> results = patientSampleComponantFacade.findLightsByJpql(jpql, params);
        String name = (results == null || results.isEmpty() || results.get(0) == null)
                ? ""
                : results.get(0).toString();
        tubeNameCache.put(patientSampleId, name);
        return name;
    }

    public void clear() {
        combinedItems = null;
        combinedTube = null;
        combinedSampleIds = null;
        combinedBillNumbers = null;
    }

    public boolean isCombined() {
        return combinedItems != null && !combinedItems.isEmpty();
    }

    public PatientInvestigation getFirstItem() {
        return isCombined() ? combinedItems.get(0) : null;
    }

    public List<PatientInvestigation> getCombinedItems() {
        return combinedItems;
    }

    public InvestigationTube getCombinedTube() {
        return combinedTube;
    }

    public List<Long> getCombinedSampleIds() {
        return combinedSampleIds;
    }

    public List<String> getCombinedBillNumbers() {
        return combinedBillNumbers;
    }

    public String getCombinedBillNumbersAsString() {
        return combinedBillNumbers == null ? "" : String.join(", ", combinedBillNumbers);
    }

}
