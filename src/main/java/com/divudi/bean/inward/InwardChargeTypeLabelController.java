package com.divudi.bean.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.inward.InwardChargeType;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.faces.view.ViewScoped;

/**
 * Backing bean for the inward charge type label configuration admin page.
 * Allows hospital admins to set custom display names for each InwardChargeType
 * enum value, stored as application-scoped ConfigOption entries. Also allows
 * setting per-type ordering numbers used to order charge type columns in
 * reports (Report Order) and, in future, on the Final Bill (Final Bill Order)
 * (issue #23340).
 */
@Named
@ViewScoped
public class InwardChargeTypeLabelController implements Serializable {

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    private List<InwardChargeType> chargeTypes;
    private Map<String, String> labelMap;
    private Map<String, String> reportOrderMap;
    private Map<String, String> finalBillOrderMap;

    @PostConstruct
    public void init() {
        chargeTypes = Arrays.asList(InwardChargeType.values());
        labelMap = new HashMap<>();
        reportOrderMap = new HashMap<>();
        finalBillOrderMap = new HashMap<>();
        for (InwardChargeType type : chargeTypes) {
            String custom = configOptionApplicationController.getShortTextValueByKey(
                    "Inward Charge Type Label - " + type.name(), "");
            labelMap.put(type.name(), custom == null ? "" : custom);
            // Kept as String (not Integer) the same way labelMap is: a p:inputText
            // bound to a Map<String, Integer> entry submits a raw String, since
            // MapELResolver.setValue() puts the value as-is without consulting the
            // map's erased generic type — an Integer-typed map caused a
            // ClassCastException on save (issue #23340 QA). Parsed back to int only
            // where actually needed, in orderOrDefault().
            reportOrderMap.put(type.name(), String.valueOf(configOptionApplicationController.getInwardChargeTypeReportOrder(type)));
            finalBillOrderMap.put(type.name(), String.valueOf(configOptionApplicationController.getInwardChargeTypeFinalBillOrder(type)));
        }
    }

    public void saveAll() {
        for (InwardChargeType type : chargeTypes) {
            String custom = labelMap.get(type.name());
            configOptionApplicationController.saveInwardChargeTypeLabel(type, custom);
            configOptionApplicationController.saveInwardChargeTypeReportOrder(type, orderOrDefault(reportOrderMap, type));
            configOptionApplicationController.saveInwardChargeTypeFinalBillOrder(type, orderOrDefault(finalBillOrderMap, type));
        }
    }

    public void saveOne(InwardChargeType type) {
        String custom = labelMap.get(type.name());
        configOptionApplicationController.saveInwardChargeTypeLabel(type, custom);
        configOptionApplicationController.saveInwardChargeTypeReportOrder(type, orderOrDefault(reportOrderMap, type));
        configOptionApplicationController.saveInwardChargeTypeFinalBillOrder(type, orderOrDefault(finalBillOrderMap, type));
    }

    /**
     * Parses the submitted order string for a type, falling back to the same
     * ordinal-based default the ConfigOptionApplicationController getters use
     * when the field was left blank or holds something unparsable (never lets
     * a bad row block the save).
     */
    private int orderOrDefault(Map<String, String> orderMap, InwardChargeType type) {
        String order = orderMap.get(type.name());
        if (order == null || order.trim().isEmpty()) {
            return (type.ordinal() + 1) * 10;
        }
        try {
            return Integer.parseInt(order.trim());
        } catch (NumberFormatException e) {
            return (type.ordinal() + 1) * 10;
        }
    }

    public List<InwardChargeType> getChargeTypes() {
        return chargeTypes;
    }

    public Map<String, String> getLabelMap() {
        return labelMap;
    }

    public void setLabelMap(Map<String, String> labelMap) {
        this.labelMap = labelMap;
    }

    public Map<String, String> getReportOrderMap() {
        return reportOrderMap;
    }

    public void setReportOrderMap(Map<String, String> reportOrderMap) {
        this.reportOrderMap = reportOrderMap;
    }

    public Map<String, String> getFinalBillOrderMap() {
        return finalBillOrderMap;
    }

    public void setFinalBillOrderMap(Map<String, String> finalBillOrderMap) {
        this.finalBillOrderMap = finalBillOrderMap;
    }
}
