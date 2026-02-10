/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dto.AmpDto;
import com.divudi.core.data.dto.AmppDto;
import com.divudi.core.data.dto.AtmDto;
import com.divudi.core.data.dto.VmppDto;
import com.divudi.core.data.dto.VmpDto;
import com.divudi.core.data.dto.VtmDto;
import com.divudi.core.data.dto.pharmaceutical.AmpRequestDTO;
import com.divudi.core.data.dto.pharmaceutical.AmppRequestDTO;
import com.divudi.core.data.dto.pharmaceutical.AtmRequestDTO;
import com.divudi.core.data.dto.pharmaceutical.PharmaceuticalItemBaseRequestDTO;
import com.divudi.core.data.dto.pharmaceutical.VmppRequestDTO;
import com.divudi.core.data.dto.pharmaceutical.VmpRequestDTO;
import com.divudi.core.data.dto.pharmaceutical.VtmRequestDTO;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.DosageForm;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.Atm;
import com.divudi.core.entity.pharmacy.MeasurementUnit;
import com.divudi.core.entity.pharmacy.PharmaceuticalItemCategory;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.entity.pharmacy.Vmpp;
import com.divudi.core.entity.pharmacy.Vtm;
import com.divudi.core.facade.AmpFacade;
import com.divudi.core.facade.AmppFacade;
import com.divudi.core.facade.AtmFacade;
import com.divudi.core.facade.CategoryFacade;
import com.divudi.core.facade.DosageFormFacade;
import com.divudi.core.facade.MeasurementUnitFacade;
import com.divudi.core.facade.PharmaceuticalItemCategoryFacade;
import com.divudi.core.facade.VmpFacade;
import com.divudi.core.facade.VmppFacade;
import com.divudi.core.facade.VtmFacade;
import com.divudi.core.util.CommonFunctions;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Pharmaceutical Item API operations.
 * Provides business logic for managing VTM, ATM, VMP, AMP, VMPP, and AMPP entities.
 *
 * @author Buddhika
 */
@Stateless
public class PharmaceuticalItemApiService implements Serializable {

    @EJB
    private VtmFacade vtmFacade;
    @EJB
    private AtmFacade atmFacade;
    @EJB
    private VmpFacade vmpFacade;
    @EJB
    private AmpFacade ampFacade;
    @EJB
    private VmppFacade vmppFacade;
    @EJB
    private AmppFacade amppFacade;
    @EJB
    private DosageFormFacade dosageFormFacade;
    @EJB
    private MeasurementUnitFacade measurementUnitFacade;
    @EJB
    private PharmaceuticalItemCategoryFacade pharmaceuticalItemCategoryFacade;

    // ==================== SEARCH ====================

    public List<?> searchItems(String type, String query, String departmentTypeStr, Integer limit) throws Exception {
        if (query == null || query.trim().isEmpty()) {
            throw new Exception("Query parameter is required");
        }

        DepartmentType deptType = null;
        if (departmentTypeStr != null && !departmentTypeStr.trim().isEmpty()) {
            try {
                deptType = DepartmentType.valueOf(departmentTypeStr.trim());
            } catch (IllegalArgumentException e) {
                throw new Exception("Invalid departmentType: " + departmentTypeStr);
            }
        }

        int resultLimit = (limit != null && limit > 0 && limit <= 100) ? limit : 20;

        switch (type) {
            case "vtm":
                return searchVtms(query.trim(), deptType, resultLimit);
            case "atm":
                return searchAtms(query.trim(), deptType, resultLimit);
            case "vmp":
                return searchVmps(query.trim(), deptType, resultLimit);
            case "amp":
                return searchAmps(query.trim(), deptType, resultLimit);
            case "vmpp":
                return searchVmpps(query.trim(), deptType, resultLimit);
            case "ampp":
                return searchAmpps(query.trim(), deptType, resultLimit);
            default:
                throw new Exception("Invalid item type: " + type);
        }
    }

    @SuppressWarnings("unchecked")
    private List<VtmDto> searchVtms(String query, DepartmentType deptType, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toUpperCase() + "%");

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT new com.divudi.core.data.dto.VtmDto(")
                .append("i.id, i.name, i.code, i.descreption, i.instructions, i.retired, i.inactive) ")
                .append("FROM Vtm i ")
                .append("WHERE i.retired = false ")
                .append("AND (upper(i.name) LIKE :query OR upper(i.code) LIKE :query) ");

        if (deptType != null) {
            jpql.append("AND i.departmentType = :deptType ");
            params.put("deptType", deptType);
        }

        jpql.append("ORDER BY i.name");

        return (List<VtmDto>) vtmFacade.findLightsByJpql(jpql.toString(), params, javax.persistence.TemporalType.TIMESTAMP, limit);
    }

    @SuppressWarnings("unchecked")
    private List<AtmDto> searchAtms(String query, DepartmentType deptType, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toUpperCase() + "%");

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT new com.divudi.core.data.dto.AtmDto(")
                .append("i.id, i.name, i.code, i.descreption, i.retired, i.inactive) ")
                .append("FROM Atm i ")
                .append("WHERE i.retired = false ")
                .append("AND (upper(i.name) LIKE :query OR upper(i.code) LIKE :query) ");

        if (deptType != null) {
            jpql.append("AND i.departmentType = :deptType ");
            params.put("deptType", deptType);
        }

        jpql.append("ORDER BY i.name");

        return (List<AtmDto>) atmFacade.findLightsByJpql(jpql.toString(), params, javax.persistence.TemporalType.TIMESTAMP, limit);
    }

    @SuppressWarnings("unchecked")
    private List<VmpDto> searchVmps(String query, DepartmentType deptType, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toUpperCase() + "%");

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT new com.divudi.core.data.dto.VmpDto(")
                .append("i.id, i.name, i.code, i.descreption, i.retired, i.inactive, ")
                .append("i.vtm.id, i.vtm.name, i.dosageForm.id, i.dosageForm.name) ")
                .append("FROM Vmp i ")
                .append("WHERE i.retired = false ")
                .append("AND (upper(i.name) LIKE :query OR upper(i.code) LIKE :query) ");

        if (deptType != null) {
            jpql.append("AND i.departmentType = :deptType ");
            params.put("deptType", deptType);
        }

        jpql.append("ORDER BY i.name");

        return (List<VmpDto>) vmpFacade.findLightsByJpql(jpql.toString(), params, javax.persistence.TemporalType.TIMESTAMP, limit);
    }

    @SuppressWarnings("unchecked")
    private List<AmpDto> searchAmps(String query, DepartmentType deptType, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toUpperCase() + "%");

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT new com.divudi.core.data.dto.AmpDto(")
                .append("i.id, i.name, i.code, i.barcode, i.inactive, ")
                .append("i.vmp.id, i.vmp.name, i.category.id, i.category.name) ")
                .append("FROM Amp i ")
                .append("WHERE i.retired = false ")
                .append("AND (upper(i.name) LIKE :query OR upper(i.code) LIKE :query) ");

        if (deptType != null) {
            jpql.append("AND i.departmentType = :deptType ");
            params.put("deptType", deptType);
        }

        jpql.append("ORDER BY i.name");

        return (List<AmpDto>) ampFacade.findLightsByJpql(jpql.toString(), params, javax.persistence.TemporalType.TIMESTAMP, limit);
    }

    @SuppressWarnings("unchecked")
    private List<VmppDto> searchVmpps(String query, DepartmentType deptType, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toUpperCase() + "%");

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT new com.divudi.core.data.dto.VmppDto(")
                .append("i.id, i.name, i.code, i.retired, i.inactive, ")
                .append("i.vmp.id, i.vmp.name) ")
                .append("FROM Vmpp i ")
                .append("WHERE i.retired = false ")
                .append("AND (upper(i.name) LIKE :query OR upper(i.code) LIKE :query) ");

        if (deptType != null) {
            jpql.append("AND i.departmentType = :deptType ");
            params.put("deptType", deptType);
        }

        jpql.append("ORDER BY i.name");

        return (List<VmppDto>) vmppFacade.findLightsByJpql(jpql.toString(), params, javax.persistence.TemporalType.TIMESTAMP, limit);
    }

    @SuppressWarnings("unchecked")
    private List<AmppDto> searchAmpps(String query, DepartmentType deptType, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toUpperCase() + "%");

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT new com.divudi.core.data.dto.AmppDto(")
                .append("i.id, i.name, i.code, i.retired, i.inactive, ")
                .append("i.dblValue, i.packUnit.name, i.amp.id, i.amp.name) ")
                .append("FROM Ampp i ")
                .append("WHERE i.retired = false ")
                .append("AND (upper(i.name) LIKE :query OR upper(i.code) LIKE :query) ");

        if (deptType != null) {
            jpql.append("AND i.departmentType = :deptType ");
            params.put("deptType", deptType);
        }

        jpql.append("ORDER BY i.name");

        return (List<AmppDto>) amppFacade.findLightsByJpql(jpql.toString(), params, javax.persistence.TemporalType.TIMESTAMP, limit);
    }

    // ==================== GET BY ID ====================

    public Object findItemById(String type, Long id) throws Exception {
        if (id == null) {
            throw new Exception("Item ID is required");
        }

        switch (type) {
            case "vtm":
                return findVtmById(id);
            case "atm":
                return findAtmById(id);
            case "vmp":
                return findVmpById(id);
            case "amp":
                return findAmpById(id);
            case "vmpp":
                return findVmppById(id);
            case "ampp":
                return findAmppById(id);
            default:
                throw new Exception("Invalid item type: " + type);
        }
    }

    private VtmDto findVtmById(Long id) throws Exception {
        Vtm item = vtmFacade.find(id);
        if (item == null) {
            throw new Exception("VTM not found with ID: " + id);
        }
        return new VtmDto(item.getId(), item.getName(), item.getCode(),
                item.getDescreption(), item.getInstructions(), item.isRetired(), item.isInactive());
    }

    private AtmDto findAtmById(Long id) throws Exception {
        Atm item = atmFacade.find(id);
        if (item == null) {
            throw new Exception("ATM not found with ID: " + id);
        }
        return new AtmDto(item.getId(), item.getName(), item.getCode(),
                item.getDescreption(), item.isRetired(), item.isInactive());
    }

    private VmpDto findVmpById(Long id) throws Exception {
        Vmp item = vmpFacade.find(id);
        if (item == null) {
            throw new Exception("VMP not found with ID: " + id);
        }
        Long vtmId = item.getVtm() != null ? item.getVtm().getId() : null;
        String vtmName = item.getVtm() != null ? item.getVtm().getName() : null;
        Long dosageFormId = item.getDosageForm() != null ? item.getDosageForm().getId() : null;
        String dosageFormName = item.getDosageForm() != null ? item.getDosageForm().getName() : null;
        return new VmpDto(item.getId(), item.getName(), item.getCode(),
                item.getDescreption(), item.isRetired(), item.isInactive(),
                vtmId, vtmName, dosageFormId, dosageFormName);
    }

    private AmpDto findAmpById(Long id) throws Exception {
        Amp item = ampFacade.find(id);
        if (item == null) {
            throw new Exception("AMP not found with ID: " + id);
        }
        Long vmpId = item.getVmp() != null ? item.getVmp().getId() : null;
        String vmpName = item.getVmp() != null ? item.getVmp().getName() : null;
        Long categoryId = item.getCategory() != null ? item.getCategory().getId() : null;
        String categoryName = item.getCategory() != null ? item.getCategory().getName() : null;
        return new AmpDto(item.getId(), item.getName(), item.getCode(),
                item.getBarcode(), item.isInactive(),
                vmpId, vmpName, categoryId, categoryName);
    }

    private VmppDto findVmppById(Long id) throws Exception {
        Vmpp item = vmppFacade.find(id);
        if (item == null) {
            throw new Exception("VMPP not found with ID: " + id);
        }
        Long vmpId = item.getVmp() != null ? item.getVmp().getId() : null;
        String vmpName = item.getVmp() != null ? item.getVmp().getName() : null;
        return new VmppDto(item.getId(), item.getName(), item.getCode(),
                item.isRetired(), item.isInactive(), vmpId, vmpName);
    }

    private AmppDto findAmppById(Long id) throws Exception {
        Ampp item = amppFacade.find(id);
        if (item == null) {
            throw new Exception("AMPP not found with ID: " + id);
        }
        Long ampId = item.getAmp() != null ? item.getAmp().getId() : null;
        String ampName = item.getAmp() != null ? item.getAmp().getName() : null;
        String packUnitName = item.getPackUnit() != null ? item.getPackUnit().getName() : null;
        return new AmppDto(item.getId(), item.getName(), item.getCode(),
                item.isRetired(), item.isInactive(),
                item.getDblValue(), packUnitName, ampId, ampName);
    }

    // ==================== CREATE ====================

    public Object createItem(String type, String requestJson, WebUser user,
                             com.google.gson.Gson gson) throws Exception {
        if (user == null) {
            throw new Exception("User is required for creating items");
        }

        switch (type) {
            case "vtm":
                return createVtm(gson.fromJson(requestJson, VtmRequestDTO.class), user);
            case "atm":
                return createAtm(gson.fromJson(requestJson, AtmRequestDTO.class), user);
            case "vmp":
                return createVmp(gson.fromJson(requestJson, VmpRequestDTO.class), user);
            case "amp":
                return createAmp(gson.fromJson(requestJson, AmpRequestDTO.class), user);
            case "vmpp":
                return createVmpp(gson.fromJson(requestJson, VmppRequestDTO.class), user);
            case "ampp":
                return createAmpp(gson.fromJson(requestJson, AmppRequestDTO.class), user);
            default:
                throw new Exception("Invalid item type: " + type);
        }
    }

    private VtmDto createVtm(VtmRequestDTO request, WebUser user) throws Exception {
        validateBaseRequest(request);
        Vtm item = new Vtm();
        applyBaseFields(item, request);
        item.setInstructions(request.getInstructions());
        setAuditFieldsForCreate(item, user);
        vtmFacade.createAndFlush(item);
        return new VtmDto(item.getId(), item.getName(), item.getCode(),
                item.getDescreption(), item.getInstructions(), item.isRetired(), item.isInactive());
    }

    private AtmDto createAtm(AtmRequestDTO request, WebUser user) throws Exception {
        validateBaseRequest(request);
        Atm item = new Atm();
        applyBaseFields(item, request);
        if (request.getVtmId() != null) {
            Vtm vtm = vtmFacade.find(request.getVtmId());
            if (vtm == null) {
                throw new Exception("VTM not found with ID: " + request.getVtmId());
            }
            item.setVtm(vtm);
        }
        setAuditFieldsForCreate(item, user);
        atmFacade.createAndFlush(item);
        return new AtmDto(item.getId(), item.getName(), item.getCode(),
                item.getDescreption(), item.isRetired(), item.isInactive());
    }

    private VmpDto createVmp(VmpRequestDTO request, WebUser user) throws Exception {
        validateBaseRequest(request);
        Vmp item = new Vmp();
        applyBaseFields(item, request);
        if (request.getVtmId() != null) {
            Vtm vtm = vtmFacade.find(request.getVtmId());
            if (vtm == null) {
                throw new Exception("VTM not found with ID: " + request.getVtmId());
            }
            item.setVtm(vtm);
        }
        if (request.getDosageFormId() != null) {
            DosageForm dosageForm = dosageFormFacade.find(request.getDosageFormId());
            if (dosageForm == null) {
                throw new Exception("Dosage form not found with ID: " + request.getDosageFormId());
            }
            item.setDosageForm(dosageForm);
        }
        setAuditFieldsForCreate(item, user);
        vmpFacade.createAndFlush(item);
        Long vtmId = item.getVtm() != null ? item.getVtm().getId() : null;
        String vtmName = item.getVtm() != null ? item.getVtm().getName() : null;
        Long dosageFormId = item.getDosageForm() != null ? item.getDosageForm().getId() : null;
        String dosageFormName = item.getDosageForm() != null ? item.getDosageForm().getName() : null;
        return new VmpDto(item.getId(), item.getName(), item.getCode(),
                item.getDescreption(), item.isRetired(), item.isInactive(),
                vtmId, vtmName, dosageFormId, dosageFormName);
    }

    private AmpDto createAmp(AmpRequestDTO request, WebUser user) throws Exception {
        validateBaseRequest(request);
        Amp item = new Amp();
        applyBaseFields(item, request);
        applyAmpSpecificFields(item, request);
        setAuditFieldsForCreate(item, user);
        ampFacade.createAndFlush(item);
        Long vmpId = item.getVmp() != null ? item.getVmp().getId() : null;
        String vmpName = item.getVmp() != null ? item.getVmp().getName() : null;
        Long categoryId = item.getCategory() != null ? item.getCategory().getId() : null;
        String categoryName = item.getCategory() != null ? item.getCategory().getName() : null;
        return new AmpDto(item.getId(), item.getName(), item.getCode(),
                item.getBarcode(), item.isInactive(),
                vmpId, vmpName, categoryId, categoryName);
    }

    private VmppDto createVmpp(VmppRequestDTO request, WebUser user) throws Exception {
        validateBaseRequest(request);
        Vmpp item = new Vmpp();
        applyBaseFields(item, request);
        applyPackFields(item, request.getVmpId(), request.getDblValue(), request.getPackUnitId(), "VMP");
        setAuditFieldsForCreate(item, user);
        vmppFacade.createAndFlush(item);
        Long vmpId = item.getVmp() != null ? item.getVmp().getId() : null;
        String vmpName = item.getVmp() != null ? item.getVmp().getName() : null;
        return new VmppDto(item.getId(), item.getName(), item.getCode(),
                item.isRetired(), item.isInactive(), vmpId, vmpName);
    }

    private AmppDto createAmpp(AmppRequestDTO request, WebUser user) throws Exception {
        validateBaseRequest(request);
        Ampp item = new Ampp();
        applyBaseFields(item, request);
        if (request.getAmpId() != null) {
            Amp amp = ampFacade.find(request.getAmpId());
            if (amp == null) {
                throw new Exception("AMP not found with ID: " + request.getAmpId());
            }
            item.setAmp(amp);
        }
        if (request.getDblValue() != null) {
            item.setDblValue(request.getDblValue());
        }
        if (request.getPackUnitId() != null) {
            MeasurementUnit packUnit = measurementUnitFacade.find(request.getPackUnitId());
            if (packUnit == null) {
                throw new Exception("Pack unit not found with ID: " + request.getPackUnitId());
            }
            item.setPackUnit(packUnit);
        }
        setAuditFieldsForCreate(item, user);
        amppFacade.createAndFlush(item);
        Long ampId = item.getAmp() != null ? item.getAmp().getId() : null;
        String ampName = item.getAmp() != null ? item.getAmp().getName() : null;
        String packUnitName = item.getPackUnit() != null ? item.getPackUnit().getName() : null;
        return new AmppDto(item.getId(), item.getName(), item.getCode(),
                item.isRetired(), item.isInactive(),
                item.getDblValue(), packUnitName, ampId, ampName);
    }

    // ==================== UPDATE ====================

    public Object updateItem(String type, Long id, String requestJson, WebUser user,
                             com.google.gson.Gson gson) throws Exception {
        if (id == null) {
            throw new Exception("Item ID is required");
        }
        if (user == null) {
            throw new Exception("User is required for updating items");
        }

        switch (type) {
            case "vtm":
                return updateVtm(id, gson.fromJson(requestJson, VtmRequestDTO.class), user);
            case "atm":
                return updateAtm(id, gson.fromJson(requestJson, AtmRequestDTO.class), user);
            case "vmp":
                return updateVmp(id, gson.fromJson(requestJson, VmpRequestDTO.class), user);
            case "amp":
                return updateAmp(id, gson.fromJson(requestJson, AmpRequestDTO.class), user);
            case "vmpp":
                return updateVmpp(id, gson.fromJson(requestJson, VmppRequestDTO.class), user);
            case "ampp":
                return updateAmpp(id, gson.fromJson(requestJson, AmppRequestDTO.class), user);
            default:
                throw new Exception("Invalid item type: " + type);
        }
    }

    private VtmDto updateVtm(Long id, VtmRequestDTO request, WebUser user) throws Exception {
        Vtm item = vtmFacade.find(id);
        if (item == null) {
            throw new Exception("VTM not found with ID: " + id);
        }
        applyBaseFieldsIfProvided(item, request);
        if (request.getInstructions() != null) {
            item.setInstructions(request.getInstructions());
        }
        setAuditFieldsForEdit(item, user);
        vtmFacade.edit(item);
        return new VtmDto(item.getId(), item.getName(), item.getCode(),
                item.getDescreption(), item.getInstructions(), item.isRetired(), item.isInactive());
    }

    private AtmDto updateAtm(Long id, AtmRequestDTO request, WebUser user) throws Exception {
        Atm item = atmFacade.find(id);
        if (item == null) {
            throw new Exception("ATM not found with ID: " + id);
        }
        applyBaseFieldsIfProvided(item, request);
        if (request.getVtmId() != null) {
            Vtm vtm = vtmFacade.find(request.getVtmId());
            if (vtm == null) {
                throw new Exception("VTM not found with ID: " + request.getVtmId());
            }
            item.setVtm(vtm);
        }
        setAuditFieldsForEdit(item, user);
        atmFacade.edit(item);
        return new AtmDto(item.getId(), item.getName(), item.getCode(),
                item.getDescreption(), item.isRetired(), item.isInactive());
    }

    private VmpDto updateVmp(Long id, VmpRequestDTO request, WebUser user) throws Exception {
        Vmp item = vmpFacade.find(id);
        if (item == null) {
            throw new Exception("VMP not found with ID: " + id);
        }
        applyBaseFieldsIfProvided(item, request);
        if (request.getVtmId() != null) {
            Vtm vtm = vtmFacade.find(request.getVtmId());
            if (vtm == null) {
                throw new Exception("VTM not found with ID: " + request.getVtmId());
            }
            item.setVtm(vtm);
        }
        if (request.getDosageFormId() != null) {
            DosageForm dosageForm = dosageFormFacade.find(request.getDosageFormId());
            if (dosageForm == null) {
                throw new Exception("Dosage form not found with ID: " + request.getDosageFormId());
            }
            item.setDosageForm(dosageForm);
        }
        setAuditFieldsForEdit(item, user);
        vmpFacade.edit(item);
        Long vtmId = item.getVtm() != null ? item.getVtm().getId() : null;
        String vtmName = item.getVtm() != null ? item.getVtm().getName() : null;
        Long dosageFormId = item.getDosageForm() != null ? item.getDosageForm().getId() : null;
        String dosageFormName = item.getDosageForm() != null ? item.getDosageForm().getName() : null;
        return new VmpDto(item.getId(), item.getName(), item.getCode(),
                item.getDescreption(), item.isRetired(), item.isInactive(),
                vtmId, vtmName, dosageFormId, dosageFormName);
    }

    private AmpDto updateAmp(Long id, AmpRequestDTO request, WebUser user) throws Exception {
        Amp item = ampFacade.find(id);
        if (item == null) {
            throw new Exception("AMP not found with ID: " + id);
        }
        applyBaseFieldsIfProvided(item, request);
        applyAmpSpecificFieldsIfProvided(item, request);
        setAuditFieldsForEdit(item, user);
        ampFacade.edit(item);
        Long vmpId = item.getVmp() != null ? item.getVmp().getId() : null;
        String vmpName = item.getVmp() != null ? item.getVmp().getName() : null;
        Long categoryId = item.getCategory() != null ? item.getCategory().getId() : null;
        String categoryName = item.getCategory() != null ? item.getCategory().getName() : null;
        return new AmpDto(item.getId(), item.getName(), item.getCode(),
                item.getBarcode(), item.isInactive(),
                vmpId, vmpName, categoryId, categoryName);
    }

    private VmppDto updateVmpp(Long id, VmppRequestDTO request, WebUser user) throws Exception {
        Vmpp item = vmppFacade.find(id);
        if (item == null) {
            throw new Exception("VMPP not found with ID: " + id);
        }
        applyBaseFieldsIfProvided(item, request);
        if (request.getVmpId() != null) {
            Vmp vmp = vmpFacade.find(request.getVmpId());
            if (vmp == null) {
                throw new Exception("VMP not found with ID: " + request.getVmpId());
            }
            item.setVmp(vmp);
        }
        if (request.getDblValue() != null) {
            item.setDblValue(request.getDblValue());
        }
        if (request.getPackUnitId() != null) {
            MeasurementUnit packUnit = measurementUnitFacade.find(request.getPackUnitId());
            if (packUnit == null) {
                throw new Exception("Pack unit not found with ID: " + request.getPackUnitId());
            }
            item.setPackUnit(packUnit);
        }
        setAuditFieldsForEdit(item, user);
        vmppFacade.edit(item);
        Long vmpId = item.getVmp() != null ? item.getVmp().getId() : null;
        String vmpName = item.getVmp() != null ? item.getVmp().getName() : null;
        return new VmppDto(item.getId(), item.getName(), item.getCode(),
                item.isRetired(), item.isInactive(), vmpId, vmpName);
    }

    private AmppDto updateAmpp(Long id, AmppRequestDTO request, WebUser user) throws Exception {
        Ampp item = amppFacade.find(id);
        if (item == null) {
            throw new Exception("AMPP not found with ID: " + id);
        }
        applyBaseFieldsIfProvided(item, request);
        if (request.getAmpId() != null) {
            Amp amp = ampFacade.find(request.getAmpId());
            if (amp == null) {
                throw new Exception("AMP not found with ID: " + request.getAmpId());
            }
            item.setAmp(amp);
        }
        if (request.getDblValue() != null) {
            item.setDblValue(request.getDblValue());
        }
        if (request.getPackUnitId() != null) {
            MeasurementUnit packUnit = measurementUnitFacade.find(request.getPackUnitId());
            if (packUnit == null) {
                throw new Exception("Pack unit not found with ID: " + request.getPackUnitId());
            }
            item.setPackUnit(packUnit);
        }
        setAuditFieldsForEdit(item, user);
        amppFacade.edit(item);
        Long ampId = item.getAmp() != null ? item.getAmp().getId() : null;
        String ampName = item.getAmp() != null ? item.getAmp().getName() : null;
        String packUnitName = item.getPackUnit() != null ? item.getPackUnit().getName() : null;
        return new AmppDto(item.getId(), item.getName(), item.getCode(),
                item.isRetired(), item.isInactive(),
                item.getDblValue(), packUnitName, ampId, ampName);
    }

    // ==================== RETIRE (SOFT DELETE) ====================

    public Object retireItem(String type, Long id, String retireComments, WebUser user) throws Exception {
        if (id == null) {
            throw new Exception("Item ID is required");
        }
        if (user == null) {
            throw new Exception("User is required for retiring items");
        }

        Item item;
        switch (type) {
            case "vtm":
                item = vtmFacade.find(id);
                break;
            case "atm":
                item = atmFacade.find(id);
                break;
            case "vmp":
                item = vmpFacade.find(id);
                break;
            case "amp":
                item = ampFacade.find(id);
                break;
            case "vmpp":
                item = vmppFacade.find(id);
                break;
            case "ampp":
                item = amppFacade.find(id);
                break;
            default:
                throw new Exception("Invalid item type: " + type);
        }

        if (item == null) {
            throw new Exception(type.toUpperCase() + " not found with ID: " + id);
        }

        if (item.isRetired()) {
            throw new Exception(type.toUpperCase() + " is already retired");
        }

        item.setRetired(true);
        item.setRetirer(user);
        item.setRetiredAt(Calendar.getInstance().getTime());
        item.setRetireComments(retireComments);

        editByType(type, item);

        return findItemById(type, id);
    }

    // ==================== UNRETIRE (RESTORE) ====================

    public Object unretireItem(String type, Long id, WebUser user) throws Exception {
        if (id == null) {
            throw new Exception("Item ID is required");
        }
        if (user == null) {
            throw new Exception("User is required for restoring items");
        }

        Item item;
        switch (type) {
            case "vtm":
                item = vtmFacade.find(id);
                break;
            case "atm":
                item = atmFacade.find(id);
                break;
            case "vmp":
                item = vmpFacade.find(id);
                break;
            case "amp":
                item = ampFacade.find(id);
                break;
            case "vmpp":
                item = vmppFacade.find(id);
                break;
            case "ampp":
                item = amppFacade.find(id);
                break;
            default:
                throw new Exception("Invalid item type: " + type);
        }

        if (item == null) {
            throw new Exception(type.toUpperCase() + " not found with ID: " + id);
        }

        if (!item.isRetired()) {
            throw new Exception(type.toUpperCase() + " is not retired");
        }

        item.setRetired(false);
        item.setRetirer(null);
        item.setRetiredAt(null);
        item.setRetireComments(null);

        editByType(type, item);

        return findItemById(type, id);
    }

    // ==================== ACTIVATE / DEACTIVATE ====================

    public Object setItemActiveStatus(String type, Long id, boolean active, WebUser user) throws Exception {
        if (id == null) {
            throw new Exception("Item ID is required");
        }
        if (user == null) {
            throw new Exception("User is required for changing item status");
        }

        Item item;
        switch (type) {
            case "vtm":
                item = vtmFacade.find(id);
                break;
            case "atm":
                item = atmFacade.find(id);
                break;
            case "vmp":
                item = vmpFacade.find(id);
                break;
            case "amp":
                item = ampFacade.find(id);
                break;
            case "vmpp":
                item = vmppFacade.find(id);
                break;
            case "ampp":
                item = amppFacade.find(id);
                break;
            default:
                throw new Exception("Invalid item type: " + type);
        }

        if (item == null) {
            throw new Exception(type.toUpperCase() + " not found with ID: " + id);
        }

        item.setInactive(!active);
        setAuditFieldsForEdit(item, user);

        editByType(type, item);

        return findItemById(type, id);
    }

    // ==================== HELPER METHODS ====================

    private void validateBaseRequest(PharmaceuticalItemBaseRequestDTO request) throws Exception {
        if (request == null) {
            throw new Exception("Request body is required");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new Exception("Name is required");
        }
    }

    private void applyBaseFields(Item item, PharmaceuticalItemBaseRequestDTO request) {
        item.setName(request.getName().trim());
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            item.setCode(request.getCode().trim());
        } else {
            item.setCode(CommonFunctions.nameToCode(request.getName()));
        }
        if (request.getDescreption() != null) {
            item.setDescreption(request.getDescreption());
        }
        if (request.getDepartmentType() != null && !request.getDepartmentType().trim().isEmpty()) {
            try {
                item.setDepartmentType(DepartmentType.valueOf(request.getDepartmentType().trim()));
            } catch (IllegalArgumentException e) {
                // Keep default if invalid
            }
        }
    }

    private void applyBaseFieldsIfProvided(Item item, PharmaceuticalItemBaseRequestDTO request) {
        if (request == null) {
            return;
        }
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            item.setName(request.getName().trim());
        }
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            item.setCode(request.getCode().trim());
        }
        if (request.getDescreption() != null) {
            item.setDescreption(request.getDescreption());
        }
        if (request.getDepartmentType() != null && !request.getDepartmentType().trim().isEmpty()) {
            try {
                item.setDepartmentType(DepartmentType.valueOf(request.getDepartmentType().trim()));
            } catch (IllegalArgumentException e) {
                // Keep current value if invalid
            }
        }
    }

    private void applyAmpSpecificFields(Amp item, AmpRequestDTO request) throws Exception {
        if (request.getVmpId() != null) {
            Vmp vmp = vmpFacade.find(request.getVmpId());
            if (vmp == null) {
                throw new Exception("VMP not found with ID: " + request.getVmpId());
            }
            item.setVmp(vmp);
        }
        if (request.getAtmId() != null) {
            Atm atm = atmFacade.find(request.getAtmId());
            if (atm == null) {
                throw new Exception("ATM not found with ID: " + request.getAtmId());
            }
            item.setAtm(atm);
        }
        if (request.getCategoryId() != null) {
            PharmaceuticalItemCategory category = pharmaceuticalItemCategoryFacade.find(request.getCategoryId());
            if (category == null) {
                throw new Exception("Category not found with ID: " + request.getCategoryId());
            }
            item.setCategory(category);
        }
        if (request.getDosageFormId() != null) {
            DosageForm dosageForm = dosageFormFacade.find(request.getDosageFormId());
            if (dosageForm == null) {
                throw new Exception("Dosage form not found with ID: " + request.getDosageFormId());
            }
            item.setDosageForm(dosageForm);
        }
        if (request.getBarcode() != null) {
            item.setBarcode(request.getBarcode());
        }
        if (request.getDiscountAllowed() != null) {
            item.setDiscountAllowed(request.getDiscountAllowed());
        }
        if (request.getAllowFractions() != null) {
            item.setAllowFractions(request.getAllowFractions());
        }
        if (request.getConsumptionAllowed() != null) {
            item.setConsumptionAllowed(request.getConsumptionAllowed());
        }
        if (request.getRefundsAllowed() != null) {
            item.setRefundsAllowed(request.getRefundsAllowed());
        }
    }

    private void applyAmpSpecificFieldsIfProvided(Amp item, AmpRequestDTO request) throws Exception {
        applyAmpSpecificFields(item, request);
    }

    private void applyPackFields(Item item, Long parentVmpId, Double dblValue, Long packUnitId, String parentType) throws Exception {
        if (parentVmpId != null) {
            Vmp vmp = vmpFacade.find(parentVmpId);
            if (vmp == null) {
                throw new Exception(parentType + " not found with ID: " + parentVmpId);
            }
            item.setVmp(vmp);
        }
        if (dblValue != null) {
            item.setDblValue(dblValue);
        }
        if (packUnitId != null) {
            MeasurementUnit packUnit = measurementUnitFacade.find(packUnitId);
            if (packUnit == null) {
                throw new Exception("Pack unit not found with ID: " + packUnitId);
            }
            item.setPackUnit(packUnit);
        }
    }

    private void setAuditFieldsForCreate(Item item, WebUser user) {
        item.setCreater(user);
        item.setCreatedAt(Calendar.getInstance().getTime());
        item.setRetired(false);
        item.setInactive(false);
    }

    private void setAuditFieldsForEdit(Item item, WebUser user) {
        item.setEditer(user);
        item.setEditedAt(Calendar.getInstance().getTime());
    }

    private void editByType(String type, Item item) {
        switch (type) {
            case "vtm":
                vtmFacade.edit((Vtm) item);
                break;
            case "atm":
                atmFacade.edit((Atm) item);
                break;
            case "vmp":
                vmpFacade.edit((Vmp) item);
                break;
            case "amp":
                ampFacade.edit((Amp) item);
                break;
            case "vmpp":
                vmppFacade.edit((Vmpp) item);
                break;
            case "ampp":
                amppFacade.edit((Ampp) item);
                break;
        }
    }
}
