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
 * enum value, stored as application-scoped ConfigOption entries.
 */
@Named
@ViewScoped
public class InwardChargeTypeLabelController implements Serializable {

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    private List<InwardChargeType> chargeTypes;
    private Map<String, String> labelMap;

    @PostConstruct
    public void init() {
        chargeTypes = Arrays.asList(InwardChargeType.values());
        labelMap = new HashMap<>();
        for (InwardChargeType type : chargeTypes) {
            String custom = configOptionApplicationController.getShortTextValueByKey(
                    "Inward Charge Type Label - " + type.name(), "");
            labelMap.put(type.name(), custom == null ? "" : custom);
        }
    }

    public void saveAll() {
        for (InwardChargeType type : chargeTypes) {
            String custom = labelMap.get(type.name());
            configOptionApplicationController.saveInwardChargeTypeLabel(type, custom);
        }
    }

    public void saveOne(InwardChargeType type) {
        String custom = labelMap.get(type.name());
        configOptionApplicationController.saveInwardChargeTypeLabel(type, custom);
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
}
