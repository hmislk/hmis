package com.divudi.service.investigation;

import com.divudi.core.data.CalculationType;
import com.divudi.core.data.CssFontStyle;
import com.divudi.core.data.CssTextAlign;
import com.divudi.core.data.InvestigationItemType;
import com.divudi.core.data.InvestigationItemValueType;
import com.divudi.core.data.Sex;
import com.divudi.core.data.dto.investigation.*;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.lab.*;
import com.divudi.core.facade.*;
import com.divudi.core.util.CommonFunctions;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.*;

@Stateless
public class InvestigationFormatApiService implements Serializable {

    @EJB
    private InvestigationFacade investigationFacade;
    @EJB
    private InvestigationItemFacade investigationItemFacade;
    @EJB
    private InvestigationItemValueFacade investigationItemValueFacade;
    @EJB
    private IxCalFacade ixCalFacade;
    @EJB
    private TestFlagFacade testFlagFacade;
    @EJB
    private InvestigationItemValueFlagFacade investigationItemValueFlagFacade;
    @EJB
    private MachineFacade machineFacade;
    @EJB
    private ItemFacade itemFacade;

    // =========================================================================
    // Investigation Items
    // =========================================================================

    public List<InvestigationItemDTO> listItems(Long investigationId) throws Exception {
        loadInvestigation(investigationId);
        Map<String, Object> m = new HashMap<>();
        m.put("ixId", investigationId);
        String j = "select i from InvestigationItem i where i.retired=false and i.item.id=:ixId order by i.orderNo";
        List<InvestigationItem> rows = investigationItemFacade.findByJpql(j, m, TemporalType.TIMESTAMP);
        List<InvestigationItemDTO> out = new ArrayList<>();
        for (InvestigationItem row : rows) {
            out.add(toItemDTO(row, null));
        }
        return out;
    }

    public InvestigationItemDTO getItem(Long investigationId, Long itemId) throws Exception {
        InvestigationItem item = loadItem(itemId, investigationId);
        return toItemDTO(item, "Item found successfully");
    }

    public InvestigationItemDTO createItem(Long investigationId, InvestigationItemCreateDTO req, WebUser user) throws Exception {
        if (req == null || !req.isValid()) {
            throw new Exception("Valid request is required (name and ixItemType are mandatory)");
        }
        Investigation ix = loadInvestigation(investigationId);
        InvestigationItem item = new InvestigationItem();
        item.setItem(ix);
        item.setName(req.getName().trim());
        item.setCode(req.getCode() != null && !req.getCode().trim().isEmpty()
                ? req.getCode().trim() : CommonFunctions.nameToCode(req.getName()));
        if (req.getDescription() != null) item.setDescription(req.getDescription());
        if (req.getOrderNo() != null) item.setOrderNo(req.getOrderNo());
        item.setIxItemType(parseEnum(InvestigationItemType.class, req.getIxItemType(), "ixItemType"));
        if (req.getIxItemValueType() != null && !req.getIxItemValueType().trim().isEmpty()) {
            item.setIxItemValueType(parseEnum(InvestigationItemValueType.class, req.getIxItemValueType(), "ixItemValueType"));
        }
        if (req.getAutomated() != null) item.setAutomated(req.getAutomated());
        if (req.getResultCode() != null) item.setResultCode(req.getResultCode());
        if (req.getFormatPrefix() != null) item.setFormatPrefix(req.getFormatPrefix());
        if (req.getFormatSuffix() != null) item.setFormatSuffix(req.getFormatSuffix());
        if (req.getHtmltext() != null) item.setHtmltext(req.getHtmltext());
        if (req.getCanNotApproveIfValueIsEmpty() != null) item.setCanNotApproveIfValueIsEmpty(req.getCanNotApproveIfValueIsEmpty());
        if (req.getAbsoluteLowValue() != null) item.setAbsoluteLowValue(req.getAbsoluteLowValue());
        if (req.getAbsoluteHighValue() != null) item.setAbsoluteHighValue(req.getAbsoluteHighValue());
        if (req.getRiTop() != null) item.setRiTop(req.getRiTop());
        if (req.getRiLeft() != null) item.setRiLeft(req.getRiLeft());
        if (req.getRiWidth() != null) item.setRiWidth(req.getRiWidth());
        if (req.getRiHeight() != null) item.setRiHeight(req.getRiHeight());
        if (req.getRiFontSize() != null) item.setRiFontSize(req.getRiFontSize());
        if (req.getCssTextAlign() != null && !req.getCssTextAlign().trim().isEmpty()) {
            item.setCssTextAlign(parseEnum(CssTextAlign.class, req.getCssTextAlign(), "cssTextAlign"));
        }
        if (req.getCssFontStyle() != null && !req.getCssFontStyle().trim().isEmpty()) {
            item.setCssFontStyle(parseEnum(CssFontStyle.class, req.getCssFontStyle(), "cssFontStyle"));
        }
        if (req.getMachineId() != null) {
            Machine machine = machineFacade.find(req.getMachineId());
            if (machine == null) throw new Exception("Machine not found with ID: " + req.getMachineId());
            item.setMachine(machine);
        }
        if (req.getTestId() != null) {
            Item test = itemFacade.find(req.getTestId());
            if (test == null) throw new Exception("Analyzer test item not found with ID: " + req.getTestId());
            item.setTest(test);
        }
        item.setCreater(user);
        item.setCreatedAt(new Date());
        item.setRetired(false);
        investigationItemFacade.createAndFlush(item);
        return toItemDTO(item, "Item created successfully");
    }

    public InvestigationItemDTO updateItem(Long investigationId, Long itemId, InvestigationItemUpdateDTO req, WebUser user) throws Exception {
        if (req == null || !req.isValid()) {
            throw new Exception("Valid update request is required");
        }
        InvestigationItem item = loadItem(itemId, investigationId);
        if (req.getName() != null && !req.getName().trim().isEmpty()) item.setName(req.getName().trim());
        if (req.getCode() != null) item.setCode(req.getCode().trim());
        if (req.getDescription() != null) item.setDescription(req.getDescription());
        if (req.getOrderNo() != null) item.setOrderNo(req.getOrderNo());
        if (req.getIxItemType() != null && !req.getIxItemType().trim().isEmpty()) {
            item.setIxItemType(parseEnum(InvestigationItemType.class, req.getIxItemType(), "ixItemType"));
        }
        if (req.getIxItemValueType() != null && !req.getIxItemValueType().trim().isEmpty()) {
            item.setIxItemValueType(parseEnum(InvestigationItemValueType.class, req.getIxItemValueType(), "ixItemValueType"));
        }
        if (req.getAutomated() != null) item.setAutomated(req.getAutomated());
        if (req.getResultCode() != null) item.setResultCode(req.getResultCode());
        if (req.getFormatPrefix() != null) item.setFormatPrefix(req.getFormatPrefix());
        if (req.getFormatSuffix() != null) item.setFormatSuffix(req.getFormatSuffix());
        if (req.getHtmltext() != null) item.setHtmltext(req.getHtmltext());
        if (req.getCanNotApproveIfValueIsEmpty() != null) item.setCanNotApproveIfValueIsEmpty(req.getCanNotApproveIfValueIsEmpty());
        if (req.getAbsoluteLowValue() != null) item.setAbsoluteLowValue(req.getAbsoluteLowValue());
        if (req.getAbsoluteHighValue() != null) item.setAbsoluteHighValue(req.getAbsoluteHighValue());
        if (req.getRiTop() != null) item.setRiTop(req.getRiTop());
        if (req.getRiLeft() != null) item.setRiLeft(req.getRiLeft());
        if (req.getRiWidth() != null) item.setRiWidth(req.getRiWidth());
        if (req.getRiHeight() != null) item.setRiHeight(req.getRiHeight());
        if (req.getRiFontSize() != null) item.setRiFontSize(req.getRiFontSize());
        if (req.getCssTextAlign() != null && !req.getCssTextAlign().trim().isEmpty()) {
            item.setCssTextAlign(parseEnum(CssTextAlign.class, req.getCssTextAlign(), "cssTextAlign"));
        }
        if (req.getCssFontStyle() != null && !req.getCssFontStyle().trim().isEmpty()) {
            item.setCssFontStyle(parseEnum(CssFontStyle.class, req.getCssFontStyle(), "cssFontStyle"));
        }
        if (req.getMachineId() != null) {
            Machine machine = machineFacade.find(req.getMachineId());
            if (machine == null) throw new Exception("Machine not found with ID: " + req.getMachineId());
            item.setMachine(machine);
        }
        if (req.getTestId() != null) {
            Item test = itemFacade.find(req.getTestId());
            if (test == null) throw new Exception("Analyzer test item not found with ID: " + req.getTestId());
            item.setTest(test);
        }
        investigationItemFacade.edit(item);
        return toItemDTO(item, "Item updated successfully");
    }

    public InvestigationItemDTO deleteItem(Long investigationId, Long itemId, WebUser user) throws Exception {
        InvestigationItem item = loadItem(itemId, investigationId);
        item.setRetired(true);
        item.setRetirer(user);
        item.setRetiredAt(new Date());
        investigationItemFacade.edit(item);
        return toItemDTO(item, "Item retired successfully");
    }

    // =========================================================================
    // Investigation Item Values
    // =========================================================================

    public List<InvestigationItemValueDTO> listValues(Long investigationId, Long itemId) throws Exception {
        loadItem(itemId, investigationId);
        Map<String, Object> m = new HashMap<>();
        m.put("itemId", itemId);
        String j = "select v from InvestigationItemValue v where v.retired=false "
                + "and TYPE(v) = InvestigationItemValue and v.investigationItem.id=:itemId order by v.orderNo";
        List<InvestigationItemValue> rows = investigationItemValueFacade.findByJpql(j, m, TemporalType.TIMESTAMP);
        List<InvestigationItemValueDTO> out = new ArrayList<>();
        for (InvestigationItemValue row : rows) {
            out.add(toValueDTO(row, null));
        }
        return out;
    }

    public InvestigationItemValueDTO createValue(Long investigationId, Long itemId, InvestigationItemValueDTO req, WebUser user) throws Exception {
        if (req == null || req.getName() == null || req.getName().trim().isEmpty()) {
            throw new Exception("Value name is required");
        }
        InvestigationItem item = loadItem(itemId, investigationId);
        InvestigationItemValue val = new InvestigationItemValue();
        val.setInvestigationItem(item);
        val.setName(req.getName().trim());
        val.setCode(req.getCode() != null && !req.getCode().trim().isEmpty()
                ? req.getCode().trim() : CommonFunctions.nameToCode(req.getName()));
        if (req.getOrderNo() != null) val.setOrderNo(req.getOrderNo());
        val.setCreater(user);
        val.setCreatedAt(new Date());
        val.setRetired(false);
        investigationItemValueFacade.createAndFlush(val);
        return toValueDTO(val, "Value created successfully");
    }

    public InvestigationItemValueDTO updateValue(Long investigationId, Long valueId, InvestigationItemValueDTO req, WebUser user) throws Exception {
        if (req == null) throw new Exception("Valid update request is required");
        InvestigationItemValue val = loadValue(valueId, investigationId);
        if (req.getName() != null && !req.getName().trim().isEmpty()) val.setName(req.getName().trim());
        if (req.getCode() != null) val.setCode(req.getCode().trim());
        if (req.getOrderNo() != null) val.setOrderNo(req.getOrderNo());
        investigationItemValueFacade.edit(val);
        return toValueDTO(val, "Value updated successfully");
    }

    public InvestigationItemValueDTO deleteValue(Long investigationId, Long valueId, WebUser user) throws Exception {
        InvestigationItemValue val = loadValue(valueId, investigationId);
        val.setRetired(true);
        val.setRetirer(user);
        val.setRetiredAt(new Date());
        investigationItemValueFacade.edit(val);
        return toValueDTO(val, "Value retired successfully");
    }

    // =========================================================================
    // Calculations
    // =========================================================================

    public List<IxCalDTO> listCalculations(Long investigationId) throws Exception {
        loadInvestigation(investigationId);
        Map<String, Object> m = new HashMap<>();
        m.put("ixId", investigationId);
        String j = "select c from IxCal c where (c.retired=false or c.retired is null) "
                + "and c.calIxItem.item.id=:ixId order by c.calIxItem.id, c.orderNo";
        List<IxCal> rows = ixCalFacade.findByJpql(j, m, TemporalType.TIMESTAMP);
        List<IxCalDTO> out = new ArrayList<>();
        for (IxCal row : rows) {
            out.add(toCalDTO(row, null));
        }
        return out;
    }

    public IxCalDTO createCalculation(Long investigationId, IxCalDTO req, WebUser user) throws Exception {
        if (req == null || req.getCalIxItemId() == null || req.getCalculationType() == null) {
            throw new Exception("calIxItemId and calculationType are required");
        }
        loadInvestigation(investigationId);
        InvestigationItem calItem = loadItem(req.getCalIxItemId(), investigationId);
        IxCal cal = new IxCal();
        cal.setCalIxItem(calItem);
        cal.setCalculationType(parseEnum(CalculationType.class, req.getCalculationType(), "calculationType"));
        if (req.getValIxItemId() != null) {
            cal.setValIxItem(loadItem(req.getValIxItemId(), investigationId));
        }
        if (req.getConstantValue() != null) cal.setConstantValue(req.getConstantValue());
        if (req.getMaleConstantValue() != null) cal.setMaleConstantValue(req.getMaleConstantValue());
        if (req.getFemaleConstantValue() != null) cal.setFemaleConstantValue(req.getFemaleConstantValue());
        if (req.getJavascript() != null) cal.setJavascript(req.getJavascript());
        if (req.getOrderNo() != null) cal.setOrderNo(req.getOrderNo());
        cal.setRetired(false);
        ixCalFacade.createAndFlush(cal);
        return toCalDTO(cal, "Calculation created successfully");
    }

    public IxCalDTO updateCalculation(Long investigationId, Long calcId, IxCalDTO req, WebUser user) throws Exception {
        if (req == null) throw new Exception("Valid update request is required");
        IxCal cal = loadCalculation(calcId, investigationId);
        if (req.getCalIxItemId() != null) {
            cal.setCalIxItem(loadItem(req.getCalIxItemId(), investigationId));
        }
        if (req.getValIxItemId() != null) {
            cal.setValIxItem(loadItem(req.getValIxItemId(), investigationId));
        }
        if (req.getCalculationType() != null && !req.getCalculationType().trim().isEmpty()) {
            cal.setCalculationType(parseEnum(CalculationType.class, req.getCalculationType(), "calculationType"));
        }
        if (req.getConstantValue() != null) cal.setConstantValue(req.getConstantValue());
        if (req.getMaleConstantValue() != null) cal.setMaleConstantValue(req.getMaleConstantValue());
        if (req.getFemaleConstantValue() != null) cal.setFemaleConstantValue(req.getFemaleConstantValue());
        if (req.getJavascript() != null) cal.setJavascript(req.getJavascript());
        if (req.getOrderNo() != null) cal.setOrderNo(req.getOrderNo());
        ixCalFacade.edit(cal);
        return toCalDTO(cal, "Calculation updated successfully");
    }

    public IxCalDTO deleteCalculation(Long investigationId, Long calcId, WebUser user) throws Exception {
        IxCal cal = loadCalculation(calcId, investigationId);
        cal.setRetired(true);
        cal.setRetirer(user);
        cal.setRetiredAt(new Date());
        ixCalFacade.edit(cal);
        return toCalDTO(cal, "Calculation retired successfully");
    }

    // =========================================================================
    // Flags
    // =========================================================================

    public List<TestFlagDTO> listFlags(Long investigationId) throws Exception {
        loadInvestigation(investigationId);
        Map<String, Object> m = new HashMap<>();
        m.put("ixId", investigationId);
        String j = "select f from TestFlag f where f.retired=false "
                + "and f.item.id=:ixId order by f.orderNo";
        List<TestFlag> rows = testFlagFacade.findByJpql(j, m, TemporalType.TIMESTAMP);
        List<TestFlagDTO> out = new ArrayList<>();
        for (TestFlag row : rows) {
            out.add(toFlagDTO(row, null));
        }
        return out;
    }

    public TestFlagDTO createFlag(Long investigationId, TestFlagDTO req, WebUser user) throws Exception {
        if (req == null || req.getInvestigationItemOfValueTypeId() == null
                || req.getInvestigationItemOfFlagTypeId() == null) {
            throw new Exception("investigationItemOfValueTypeId and investigationItemOfFlagTypeId are required");
        }
        Investigation ix = loadInvestigation(investigationId);
        InvestigationItem valueItem = loadItem(req.getInvestigationItemOfValueTypeId(), investigationId);
        InvestigationItem flagItem = loadItem(req.getInvestigationItemOfFlagTypeId(), investigationId);
        TestFlag flag = new TestFlag();
        flag.setItem(ix);
        flag.setInvestigationItemOfValueType(valueItem);
        flag.setInvestigationItemOfFlagType(flagItem);
        if (req.getSex() != null && !req.getSex().trim().isEmpty()) {
            flag.setSex(parseEnum(Sex.class, req.getSex(), "sex"));
        }
        if (req.getFromAge() != null) flag.setFromAge(req.getFromAge());
        if (req.getToAge() != null) flag.setToAge(req.getToAge());
        if (req.getFromVal() != null) flag.setFromVal(req.getFromVal());
        if (req.getToVal() != null) flag.setToVal(req.getToVal());
        if (req.getFlagMessage() != null) flag.setFlagMessage(req.getFlagMessage());
        if (req.getHighMessage() != null) flag.setHighMessage(req.getHighMessage());
        if (req.getLowMessage() != null) flag.setLowMessage(req.getLowMessage());
        if (req.getNormalMessage() != null) flag.setNormalMessage(req.getNormalMessage());
        if (req.getDisplayFlagMessage() != null) flag.setDisplayFlagMessage(req.getDisplayFlagMessage());
        if (req.getDisplayHighMessage() != null) flag.setDisplayHighMessage(req.getDisplayHighMessage());
        if (req.getDisplayLowMessage() != null) flag.setDisplayLowMessage(req.getDisplayLowMessage());
        if (req.getDisplayNormalMessage() != null) flag.setDisplayNormalMessage(req.getDisplayNormalMessage());
        flag.setCreater(user);
        flag.setCreatedAt(new Date());
        flag.setRetired(false);
        testFlagFacade.createAndFlush(flag);
        return toFlagDTO(flag, "Flag created successfully");
    }

    public TestFlagDTO updateFlag(Long investigationId, Long flagId, TestFlagDTO req, WebUser user) throws Exception {
        if (req == null) throw new Exception("Valid update request is required");
        TestFlag flag = loadFlag(flagId, investigationId);
        if (req.getInvestigationItemOfValueTypeId() != null) {
            InvestigationItem valueItem = loadItem(req.getInvestigationItemOfValueTypeId(), investigationId);
            flag.setInvestigationItemOfValueType(valueItem);
        }
        if (req.getInvestigationItemOfFlagTypeId() != null) {
            InvestigationItem flagItem = loadItem(req.getInvestigationItemOfFlagTypeId(), investigationId);
            flag.setInvestigationItemOfFlagType(flagItem);
        }
        if (req.getSex() != null && !req.getSex().trim().isEmpty()) {
            flag.setSex(parseEnum(Sex.class, req.getSex(), "sex"));
        }
        if (req.getFromAge() != null) flag.setFromAge(req.getFromAge());
        if (req.getToAge() != null) flag.setToAge(req.getToAge());
        if (req.getFromVal() != null) flag.setFromVal(req.getFromVal());
        if (req.getToVal() != null) flag.setToVal(req.getToVal());
        if (req.getFlagMessage() != null) flag.setFlagMessage(req.getFlagMessage());
        if (req.getHighMessage() != null) flag.setHighMessage(req.getHighMessage());
        if (req.getLowMessage() != null) flag.setLowMessage(req.getLowMessage());
        if (req.getNormalMessage() != null) flag.setNormalMessage(req.getNormalMessage());
        if (req.getDisplayFlagMessage() != null) flag.setDisplayFlagMessage(req.getDisplayFlagMessage());
        if (req.getDisplayHighMessage() != null) flag.setDisplayHighMessage(req.getDisplayHighMessage());
        if (req.getDisplayLowMessage() != null) flag.setDisplayLowMessage(req.getDisplayLowMessage());
        if (req.getDisplayNormalMessage() != null) flag.setDisplayNormalMessage(req.getDisplayNormalMessage());
        testFlagFacade.edit(flag);
        return toFlagDTO(flag, "Flag updated successfully");
    }

    public TestFlagDTO deleteFlag(Long investigationId, Long flagId, WebUser user) throws Exception {
        TestFlag flag = loadFlag(flagId, investigationId);
        flag.setRetired(true);
        flag.setRetirer(user);
        flag.setRetiredAt(new Date());
        testFlagFacade.edit(flag);
        return toFlagDTO(flag, "Flag retired successfully");
    }

    // =========================================================================
    // Dynamic Labels
    // =========================================================================

    public List<DynamicLabelDTO> listDynamicLabels(Long investigationId) throws Exception {
        loadInvestigation(investigationId);
        Map<String, Object> m = new HashMap<>();
        m.put("ixId", investigationId);
        String j = "select f from InvestigationItemValueFlag f where f.retired=false "
                + "and f.investigationItemOfLabelType.item.id=:ixId order by f.orderNo";
        List<InvestigationItemValueFlag> rows = investigationItemValueFlagFacade.findByJpql(j, m, TemporalType.TIMESTAMP);
        List<DynamicLabelDTO> out = new ArrayList<>();
        for (InvestigationItemValueFlag row : rows) {
            out.add(toDynamicLabelDTO(row, null));
        }
        return out;
    }

    public DynamicLabelDTO createDynamicLabel(Long investigationId, DynamicLabelDTO req, WebUser user) throws Exception {
        if (req == null || req.getInvestigationItemOfLabelTypeId() == null) {
            throw new Exception("investigationItemOfLabelTypeId is required");
        }
        loadInvestigation(investigationId);
        InvestigationItem labelItem = loadItem(req.getInvestigationItemOfLabelTypeId(), investigationId);
        InvestigationItemValueFlag dl = new InvestigationItemValueFlag();
        dl.setInvestigationItemOfLabelType(labelItem);
        if (req.getSex() != null && !req.getSex().trim().isEmpty()) {
            dl.setSex(parseEnum(Sex.class, req.getSex(), "sex"));
        }
        if (req.getFromAge() != null) dl.setFromAge(req.getFromAge());
        if (req.getToAge() != null) dl.setToAge(req.getToAge());
        if (req.getFlagMessage() != null) dl.setFlagMessage(req.getFlagMessage());
        dl.setCreater(user);
        dl.setCreatedAt(new Date());
        dl.setRetired(false);
        investigationItemValueFlagFacade.createAndFlush(dl);
        return toDynamicLabelDTO(dl, "Dynamic label created successfully");
    }

    public DynamicLabelDTO updateDynamicLabel(Long investigationId, Long labelId, DynamicLabelDTO req, WebUser user) throws Exception {
        if (req == null) throw new Exception("Valid update request is required");
        InvestigationItemValueFlag dl = loadDynamicLabel(labelId, investigationId);
        if (req.getInvestigationItemOfLabelTypeId() != null) {
            InvestigationItem labelItem = loadItem(req.getInvestigationItemOfLabelTypeId(), investigationId);
            dl.setInvestigationItemOfLabelType(labelItem);
        }
        if (req.getSex() != null && !req.getSex().trim().isEmpty()) {
            dl.setSex(parseEnum(Sex.class, req.getSex(), "sex"));
        }
        if (req.getFromAge() != null) dl.setFromAge(req.getFromAge());
        if (req.getToAge() != null) dl.setToAge(req.getToAge());
        if (req.getFlagMessage() != null) dl.setFlagMessage(req.getFlagMessage());
        investigationItemValueFlagFacade.edit(dl);
        return toDynamicLabelDTO(dl, "Dynamic label updated successfully");
    }

    public DynamicLabelDTO deleteDynamicLabel(Long investigationId, Long labelId, WebUser user) throws Exception {
        InvestigationItemValueFlag dl = loadDynamicLabel(labelId, investigationId);
        dl.setRetired(true);
        dl.setRetirer(user);
        dl.setRetiredAt(new Date());
        investigationItemValueFlagFacade.edit(dl);
        return toDynamicLabelDTO(dl, "Dynamic label retired successfully");
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String raw, String field) throws Exception {
        try {
            return Enum.valueOf(enumClass, raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new Exception("Invalid " + field + ": " + raw
                    + ". Valid values: " + Arrays.toString(enumClass.getEnumConstants()));
        }
    }

    private Investigation loadInvestigation(Long id) throws Exception {
        Investigation ix = investigationFacade.find(id);
        if (ix == null || ix.isRetired()) {
            throw new Exception("Investigation not found with ID: " + id);
        }
        return ix;
    }

    private InvestigationItem loadItem(Long itemId, Long investigationId) throws Exception {
        InvestigationItem item = investigationItemFacade.find(itemId);
        if (item == null || item.isRetired()) {
            throw new Exception("Investigation item not found with ID: " + itemId);
        }
        if (item.getItem() == null || !item.getItem().getId().equals(investigationId)) {
            throw new Exception("Item " + itemId + " does not belong to investigation " + investigationId);
        }
        return item;
    }

    private InvestigationItemValue loadValue(Long valueId, Long investigationId) throws Exception {
        InvestigationItemValue val = investigationItemValueFacade.find(valueId);
        if (val == null || val.isRetired()) {
            throw new Exception("Investigation item value not found with ID: " + valueId);
        }
        if (val.getInvestigationItem() == null || val.getInvestigationItem().getItem() == null
                || !val.getInvestigationItem().getItem().getId().equals(investigationId)) {
            throw new Exception("Value " + valueId + " does not belong to investigation " + investigationId);
        }
        return val;
    }

    private IxCal loadCalculation(Long calcId, Long investigationId) throws Exception {
        IxCal cal = ixCalFacade.find(calcId);
        if (cal == null || Boolean.TRUE.equals(cal.isRetired())) {
            throw new Exception("Calculation not found with ID: " + calcId);
        }
        if (cal.getCalIxItem() == null || cal.getCalIxItem().getItem() == null
                || !cal.getCalIxItem().getItem().getId().equals(investigationId)) {
            throw new Exception("Calculation " + calcId + " does not belong to investigation " + investigationId);
        }
        return cal;
    }

    private TestFlag loadFlag(Long flagId, Long investigationId) throws Exception {
        TestFlag flag = testFlagFacade.find(flagId);
        if (flag == null || flag.isRetired()) {
            throw new Exception("Flag not found with ID: " + flagId);
        }
        if (flag.getItem() == null || !flag.getItem().getId().equals(investigationId)) {
            throw new Exception("Flag " + flagId + " does not belong to investigation " + investigationId);
        }
        return flag;
    }

    private InvestigationItemValueFlag loadDynamicLabel(Long labelId, Long investigationId) throws Exception {
        InvestigationItemValueFlag dl = investigationItemValueFlagFacade.find(labelId);
        if (dl == null || dl.isRetired()) {
            throw new Exception("Dynamic label not found with ID: " + labelId);
        }
        if (dl.getInvestigationItemOfLabelType() == null
                || dl.getInvestigationItemOfLabelType().getItem() == null
                || !dl.getInvestigationItemOfLabelType().getItem().getId().equals(investigationId)) {
            throw new Exception("Dynamic label " + labelId + " does not belong to investigation " + investigationId);
        }
        return dl;
    }

    private InvestigationItemDTO toItemDTO(InvestigationItem item, String msg) {
        InvestigationItemDTO dto = new InvestigationItemDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setCode(item.getCode());
        dto.setDescription(item.getDescription());
        dto.setOrderNo(item.getOrderNo());
        dto.setIxItemType(item.getIxItemType() != null ? item.getIxItemType().name() : null);
        dto.setIxItemValueType(item.getIxItemValueType() != null ? item.getIxItemValueType().name() : null);
        dto.setAutomated(item.isAutomated());
        dto.setResultCode(item.getResultCode());
        dto.setFormatPrefix(item.getFormatPrefix());
        dto.setFormatSuffix(item.getFormatSuffix());
        dto.setHtmltext(item.getHtmltext());
        dto.setCanNotApproveIfValueIsEmpty(item.isCanNotApproveIfValueIsEmpty());
        dto.setAbsoluteLowValue(item.getAbsoluteLowValue());
        dto.setAbsoluteHighValue(item.getAbsoluteHighValue());
        dto.setRiTop(item.getRiTop());
        dto.setRiLeft(item.getRiLeft());
        dto.setRiWidth(item.getRiWidth());
        dto.setRiHeight(item.getRiHeight());
        dto.setRiFontSize(item.getRiFontSize());
        dto.setCssTextAlign(item.getCssTextAlign() != null ? item.getCssTextAlign().name() : null);
        dto.setCssFontStyle(item.getCssFontStyle() != null ? item.getCssFontStyle().name() : null);
        if (item.getMachine() != null) {
            dto.setMachineId(item.getMachine().getId());
            dto.setMachineName(item.getMachine().getName());
        }
        if (item.getTest() != null) {
            dto.setTestId(item.getTest().getId());
            dto.setTestName(item.getTest().getName());
            dto.setTestCode(item.getTest().getCode());
        }
        dto.setMessage(msg);
        return dto;
    }

    private InvestigationItemValueDTO toValueDTO(InvestigationItemValue val, String msg) {
        InvestigationItemValueDTO dto = new InvestigationItemValueDTO();
        dto.setId(val.getId());
        dto.setName(val.getName());
        dto.setCode(val.getCode());
        dto.setOrderNo(val.getOrderNo());
        if (val.getInvestigationItem() != null) {
            dto.setInvestigationItemId(val.getInvestigationItem().getId());
            dto.setInvestigationItemName(val.getInvestigationItem().getName());
        }
        dto.setMessage(msg);
        return dto;
    }

    private IxCalDTO toCalDTO(IxCal cal, String msg) {
        IxCalDTO dto = new IxCalDTO();
        dto.setId(cal.getId());
        if (cal.getCalIxItem() != null) {
            dto.setCalIxItemId(cal.getCalIxItem().getId());
            dto.setCalIxItemName(cal.getCalIxItem().getName());
        }
        if (cal.getValIxItem() != null) {
            dto.setValIxItemId(cal.getValIxItem().getId());
            dto.setValIxItemName(cal.getValIxItem().getName());
        }
        dto.setCalculationType(cal.getCalculationType() != null ? cal.getCalculationType().name() : null);
        dto.setConstantValue(cal.getConstantValue());
        dto.setMaleConstantValue(cal.getMaleConstantValue());
        dto.setFemaleConstantValue(cal.getFemaleConstantValue());
        dto.setJavascript(cal.getJavascript());
        dto.setOrderNo(cal.getOrderNo());
        dto.setMessage(msg);
        return dto;
    }

    private TestFlagDTO toFlagDTO(TestFlag flag, String msg) {
        TestFlagDTO dto = new TestFlagDTO();
        dto.setId(flag.getId());
        if (flag.getInvestigationItemOfValueType() != null) {
            dto.setInvestigationItemOfValueTypeId(flag.getInvestigationItemOfValueType().getId());
            dto.setValueItemName(flag.getInvestigationItemOfValueType().getName());
        }
        if (flag.getInvestigationItemOfFlagType() != null) {
            dto.setInvestigationItemOfFlagTypeId(flag.getInvestigationItemOfFlagType().getId());
            dto.setFlagItemName(flag.getInvestigationItemOfFlagType().getName());
        }
        dto.setSex(flag.getSex() != null ? flag.getSex().name() : null);
        dto.setFromAge(flag.getFromAge());
        dto.setToAge(flag.getToAge());
        dto.setFromVal(flag.getFromVal());
        dto.setToVal(flag.getToVal());
        dto.setFlagMessage(flag.getFlagMessage());
        dto.setHighMessage(flag.getHighMessage());
        dto.setLowMessage(flag.getLowMessage());
        dto.setNormalMessage(flag.getNormalMessage());
        dto.setDisplayFlagMessage(flag.isDisplayFlagMessage());
        dto.setDisplayHighMessage(flag.isDisplayHighMessage());
        dto.setDisplayLowMessage(flag.isDisplayLowMessage());
        dto.setDisplayNormalMessage(flag.isDisplayNormalMessage());
        dto.setMessage(msg);
        return dto;
    }

    private DynamicLabelDTO toDynamicLabelDTO(InvestigationItemValueFlag dl, String msg) {
        DynamicLabelDTO dto = new DynamicLabelDTO();
        dto.setId(dl.getId());
        if (dl.getInvestigationItemOfLabelType() != null) {
            dto.setInvestigationItemOfLabelTypeId(dl.getInvestigationItemOfLabelType().getId());
            dto.setLabelItemName(dl.getInvestigationItemOfLabelType().getName());
        }
        dto.setSex(dl.getSex() != null ? dl.getSex().name() : null);
        dto.setFromAge(dl.getFromAge());
        dto.setToAge(dl.getToAge());
        dto.setFlagMessage(dl.getFlagMessage());
        dto.setMessage(msg);
        return dto;
    }
}
