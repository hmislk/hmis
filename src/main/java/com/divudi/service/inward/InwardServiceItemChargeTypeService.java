package com.divudi.service.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.inward.InwardChargeType;
import java.io.Serializable;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * The inward charge type of an inpatient service or investigation line whose
 * item has no inward charge type of its own (issue #26272).
 *
 * <p>OPD services and investigations can be billed on an inpatient service bill
 * (for example from the theatre item list) without an inward charge type in the
 * item master. The interim and final bill group these lines by charge type, so
 * such a line must count under one charge type. The rule is: the item's own
 * type, else the type stored on the bill item, else the configured default.
 * Items that already have a type are never moved.</p>
 */
@Stateless
public class InwardServiceItemChargeTypeService implements Serializable {

    private static final long serialVersionUID = 1L;

    /** New key; the default is {@link #DEFAULT_CHARGE_TYPE}. Holds an InwardChargeType name. */
    public static final String CONFIG_KEY = "Inward Charge Type for Service Items Without One";
    public static final InwardChargeType DEFAULT_CHARGE_TYPE = InwardChargeType.OperationTheatreCharges;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    /** The configured default charge type; a missing or unknown name gives the built-in default. */
    public InwardChargeType getDefaultChargeType() {
        String name = configOptionApplicationController.getShortTextValueByKey(CONFIG_KEY, DEFAULT_CHARGE_TYPE.name());
        return parse(name);
    }

    /** Pure parse of a configured name; blank or unknown gives {@link #DEFAULT_CHARGE_TYPE}. */
    public static InwardChargeType parse(String name) {
        if (name == null || name.trim().isEmpty()) {
            return DEFAULT_CHARGE_TYPE;
        }
        try {
            return InwardChargeType.valueOf(name.trim());
        } catch (IllegalArgumentException e) {
            return DEFAULT_CHARGE_TYPE;
        }
    }

    /** The charge type a line counts under; itemType wins, then the bill item's own, then the default. */
    public static InwardChargeType resolve(InwardChargeType itemType, InwardChargeType billItemType, InwardChargeType defaultType) {
        if (itemType != null) {
            return itemType;
        }
        return billItemType != null ? billItemType : defaultType;
    }

    public InwardChargeType resolve(InwardChargeType itemType, InwardChargeType billItemType) {
        if (itemType != null) {
            return itemType;
        }
        return resolve(itemType, billItemType, getDefaultChargeType());
    }

    /**
     * JPQL condition: BillItem alias {@code bi} counts under charge type
     * {@code :paramName}. Written without coalesce so it binds no extra
     * parameter.
     */
    public static String lineCountsUnderJpql(String bi, String paramName, boolean paramIsDefault) {
        String viaBillItem = bi + ".inwardChargeType = :" + paramName;
        if (paramIsDefault) {
            viaBillItem = "(" + bi + ".inwardChargeType is null or " + viaBillItem + ")";
        }
        return "(" + bi + ".item.inwardChargeType = :" + paramName
                + " or (" + bi + ".item.inwardChargeType is null and " + viaBillItem + "))";
    }

    public String lineCountsUnderJpql(String bi, String paramName, InwardChargeType type) {
        return lineCountsUnderJpql(bi, paramName, type == getDefaultChargeType());
    }
}
