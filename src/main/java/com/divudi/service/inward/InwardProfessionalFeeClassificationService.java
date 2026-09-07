package com.divudi.service.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Consultant;
import com.divudi.core.entity.Staff;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * The single definition of how an inpatient professional fee is classified as a
 * professional charge or an assisting charge.
 *
 * <p>By default the classification follows the staff record: a {@link Consultant}
 * is a {@link InwardChargeType#ProfessionalCharge}, anyone else (MO, nurse,
 * assistant) is a {@link InwardChargeType#DoctorAndNurses} — "Assisting Charge".
 * Hospitals that bill both as one professional charge enable
 * {@link ConfigOptionApplicationController#PROFESSIONAL_AND_ASSISTING_FEES_MERGED},
 * and then {@code DoctorAndNurses} must not appear anywhere: not as a final-bill
 * row, not as a report column, not as a selectable charge type.
 *
 * <p>This rule used to be re-derived inline as {@code type(staff) = Consultant} in
 * about a dozen queries, with the ConfigOption honoured in only one of them, so a
 * merged hospital saw a bundled final bill and a split view everywhere else
 * (issue #23543). Every consumer now goes through this service instead.
 *
 * <p>Nothing is persisted: the classification is a rule, not data. It cannot be
 * stored on the BillFee's BillItem either — one professional BillItem legitimately
 * carries several fees, and in production those routinely mix a consultant with an
 * assistant, so a per-item charge type could not represent them.
 */
@Stateless
public class InwardProfessionalFeeClassificationService implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Query parameter name used by {@link #staffCondition}. */
    private static final String STAFF_CLASS_PARAM = "professionalFeeStaffClass";

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    /**
     * True when this hospital shows professional and assisting fees as one
     * professional charge.
     */
    public boolean isMerged() {
        return configOptionApplicationController.isProfessionalAndAssistingFeesMerged();
    }

    /**
     * The charge type a professional fee belongs to, given the staff member it
     * was raised for. Returns {@code ProfessionalCharge} for everyone when the
     * merge option is on.
     */
    public InwardChargeType chargeTypeOf(Staff staff) {
        if (isMerged()) {
            return InwardChargeType.ProfessionalCharge;
        }
        return (staff instanceof Consultant)
                ? InwardChargeType.ProfessionalCharge
                : InwardChargeType.DoctorAndNurses;
    }

    /**
     * True when this charge type does not exist for this hospital and must be
     * left out of bill rows, report columns and charge-type selectors.
     */
    public boolean isSuppressed(InwardChargeType type) {
        return type == InwardChargeType.DoctorAndNurses && isMerged();
    }

    /**
     * A JPQL fragment restricting a BillFee alias to the fees of one charge type,
     * registering its own query parameter in {@code params} when it needs one.
     *
     * <p>When merged, every professional fee is a professional charge, so the only
     * restriction left is {@code staff is not null} — which keeps the result
     * identical to the sum of the two unmerged buckets. Without it, staff-less fee
     * rows (11 of them on one production database) would be pulled into the
     * professional total for the first time, because they are excluded from both
     * buckets today: {@code type(bf.staff)} forces an implicit inner join that
     * drops them before the WHERE clause is evaluated.
     *
     * <p>Callers must not ask for {@code DoctorAndNurses} while merged — check
     * {@link #isSuppressed} and skip the query, which is cheaper than running one
     * that cannot match.
     *
     * @param feeAlias the BillFee alias used in the query, e.g. {@code "bf"}
     * @param target   the charge type being queried
     * @param params   the query's parameter map, added to when required
     */
    public String staffCondition(String feeAlias, InwardChargeType target, Map<String, Object> params) {
        if (isMerged()) {
            return " and " + feeAlias + ".staff is not null ";
        }
        params.put(STAFF_CLASS_PARAM, Consultant.class);
        return (target == InwardChargeType.ProfessionalCharge)
                ? " and type(" + feeAlias + ".staff) = :" + STAFF_CLASS_PARAM + " "
                : " and type(" + feeAlias + ".staff) != :" + STAFF_CLASS_PARAM + " ";
    }

    /**
     * The given charge types with any suppressed one removed — use wherever a
     * charge-type universe is built (bill rows, report columns, dropdowns).
     */
    public List<InwardChargeType> visible(Collection<InwardChargeType> types) {
        List<InwardChargeType> visible = new ArrayList<>();
        if (types == null) {
            return visible;
        }
        for (InwardChargeType type : types) {
            if (!isSuppressed(type)) {
                visible.add(type);
            }
        }
        return visible;
    }

    /** Array overload of {@link #visible(Collection)}. */
    public InwardChargeType[] visible(InwardChargeType[] types) {
        if (types == null) {
            return new InwardChargeType[0];
        }
        List<InwardChargeType> visible = visible(Arrays.asList(types));
        return visible.toArray(new InwardChargeType[0]);
    }
}
