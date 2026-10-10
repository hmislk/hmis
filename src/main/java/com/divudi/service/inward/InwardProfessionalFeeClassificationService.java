package com.divudi.service.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.Consultant;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.Speciality;
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
 * consultant charge, an assistant charge, or a technician/paramedical charge.
 *
 * <p>The category is persisted per fee on {@link BillFee#getProfessionalFeeCategory()}
 * rather than derived purely from the staff record, so a user can override it on the
 * entry form. When a fee has no category saved (legacy data, or a fee created before
 * this field existed), it falls back to {@link InwardChargeType#ProfessionalCharge}.
 *
 * <p>{@link #defaultCategoryFor(Staff, Speciality)} gives the entry form the category
 * to preselect when a staff member or speciality is picked. A speciality's
 * {@link Speciality#getDefaultProfessionalFeeCategory() Default Professional Fee
 * Category} wins when an admin has set one (e.g. TECHNICIAN → Technician Fee), because
 * technicians are usually registered as {@link Consultant} records so they can be
 * picked on these forms (issue #23983). Otherwise it falls back to the staff record's
 * subtype: a {@link Consultant} defaults to {@link InwardChargeType#ProfessionalCharge}
 * ("Consultant Fee"), a {@link Doctor} (non-consultant) defaults to
 * {@link InwardChargeType#DoctorAndNurses} ("Assistant Fee"), and anyone else (nurse,
 * technician, other paramedical staff) defaults to
 * {@link InwardChargeType#TechnicianAndParamedicalCharge} ("Technician Fee"). The user
 * may still change the preselected category before saving.
 *
 * <p>Hospitals that bill consultant and assistant fees as one professional charge
 * enable {@link ConfigOptionApplicationController#PROFESSIONAL_AND_ASSISTING_FEES_MERGED},
 * and then {@code DoctorAndNurses} must not appear anywhere: not as a final-bill row,
 * not as a report column, not as a selectable charge type. The technician/paramedical
 * category is unaffected by this toggle — it is always its own bucket.
 *
 * <p>This rule used to be re-derived inline as {@code type(staff) = Consultant} in
 * about a dozen queries, with the ConfigOption honoured in only one of them, so a
 * merged hospital saw a bundled final bill and a split view everywhere else
 * (issue #23543). Every consumer now goes through this service instead.
 */
@Stateless
public class InwardProfessionalFeeClassificationService implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    /**
     * True when this hospital shows professional and assisting fees as one
     * professional charge.
     */
    public boolean isMerged() {
        return configOptionApplicationController.isProfessionalAndAssistingFeesMerged();
    }

    /** The charge type an already-saved fee belongs to. */
    public InwardChargeType chargeTypeOf(BillFee fee) {
        InwardChargeType category = fee.getProfessionalFeeCategory();
        return category != null ? category : InwardChargeType.ProfessionalCharge;
    }

    /** The default category to preselect when this staff member is picked on the entry form. */
    public InwardChargeType defaultCategoryFor(Staff staff) {
        return defaultCategoryFor(staff, null);
    }

    /**
     * The default category for a fee entered against {@code staff} under the
     * speciality selected on the form. The first configured default wins, in
     * this order: the selected speciality, the staff member's own speciality,
     * then the staff subtype rule — so nothing changes until an admin sets a
     * default on a speciality.
     *
     * @param staff              the staff member picked on the form; may be null
     *                           when only a configured speciality is known
     * @param selectedSpeciality the speciality picked on the form; may be null
     */
    public InwardChargeType defaultCategoryFor(Staff staff, Speciality selectedSpeciality) {
        InwardChargeType fromSpeciality = configuredCategory(selectedSpeciality);
        if (fromSpeciality == null && staff != null) {
            fromSpeciality = configuredCategory(staff.getSpeciality());
        }
        if (fromSpeciality != null) {
            return fromSpeciality;
        }
        if (staff instanceof Consultant) {
            return InwardChargeType.ProfessionalCharge;
        }
        if (staff instanceof Doctor) {
            return InwardChargeType.DoctorAndNurses;
        }
        return InwardChargeType.TechnicianAndParamedicalCharge;
    }

    /**
     * The speciality's configured default, or null when it has none or it is
     * not one of the three fee categories. A merged hospital's Assistant Fee
     * default is still returned: the entry form offers all three categories and
     * the bill folds Assistant into Consultant itself.
     */
    private InwardChargeType configuredCategory(Speciality speciality) {
        if (speciality == null) {
            return null;
        }
        InwardChargeType category = speciality.getDefaultProfessionalFeeCategory();
        if (category == InwardChargeType.ProfessionalCharge
                || category == InwardChargeType.DoctorAndNurses
                || category == InwardChargeType.TechnicianAndParamedicalCharge) {
            return category;
        }
        return null;
    }

    /**
     * True when this charge type does not exist for this hospital and must be
     * left out of bill rows, report columns and charge-type selectors.
     */
    public boolean isSuppressed(InwardChargeType type) {
        return type == InwardChargeType.DoctorAndNurses && isMerged();
    }

    /**
     * A JPQL fragment restricting a BillFee alias to the fees of one charge type.
     *
     * <p>The category is read from the persisted {@code professionalFeeCategory}
     * field, falling back to {@link InwardChargeType#ProfessionalCharge} for fees
     * saved before this field existed (or otherwise left null), via a
     * {@code coalesce(...)} in the JPQL fragment itself — matching how
     * {@link #chargeTypeOf(BillFee)} resolves the same fee in Java.
     *
     * <p>{@link InwardChargeType#TechnicianAndParamedicalCharge} is never affected
     * by the merge toggle — it is always its own bucket, matched directly. For the
     * other two categories: when merged, every non-technician professional fee is
     * a professional charge, so the condition matches anything that is not the
     * technician/paramedical category (this is only reachable via
     * {@code ProfessionalCharge} — {@code DoctorAndNurses} is suppressed above when
     * merged). When not merged, the condition matches the target category exactly.
     *
     * <p>Every branch also requires {@code feeAlias.staff is not null}. Under the
     * old {@code type(bf.staff)} rule this exclusion was implicit — {@code type()}
     * forces an inner join that drops staff-less rows before the WHERE clause runs
     * — and merged mode restated it explicitly for the same reason (issue #23543:
     * staff-less fee rows, 11 of them on one production database, must not be
     * pulled into a total for the first time just because the query changed). The
     * persisted-field query has no such implicit join, so this stays explicit in
     * every branch here to preserve that guarantee.
     *
     * <p>Callers must not ask for {@code DoctorAndNurses} while merged — check
     * {@link #isSuppressed} and skip the query, which is cheaper than running one
     * that cannot match. Both that and an unrelated charge type throw rather than
     * returning a predicate: silently answering with the assisting bucket would
     * put real money under the wrong heading.
     *
     * @param feeAlias the BillFee alias used in the query, e.g. {@code "bf"}
     * @param target   the charge type being queried
     * @param params   the query's parameter map; unused by this implementation but
     *                 kept in the signature for compatibility with existing callers
     * @throws IllegalArgumentException if {@code target} is not a professional fee
     *         charge type, or is one this hospital does not use
     */
    public String staffCondition(String feeAlias, InwardChargeType target, Map<String, Object> params) {
        if (target != InwardChargeType.ProfessionalCharge
                && target != InwardChargeType.DoctorAndNurses
                && target != InwardChargeType.TechnicianAndParamedicalCharge) {
            throw new IllegalArgumentException(
                    "Not a professional fee charge type: " + target);
        }
        if (isSuppressed(target)) {
            throw new IllegalArgumentException(
                    target + " does not exist for this hospital - check isSuppressed() and skip the query");
        }

        String staffNotNull = " and " + feeAlias + ".staff is not null ";
        String categoryField = feeAlias + ".professionalFeeCategory";
        String technician = "com.divudi.core.data.inward.InwardChargeType.TechnicianAndParamedicalCharge";

        if (target == InwardChargeType.TechnicianAndParamedicalCharge) {
            // Never affected by the merge toggle — always its own bucket. A null
            // category defaults to ProfessionalCharge, never Technician, so no
            // null-check is needed here.
            return staffNotNull + " and " + categoryField + " = " + technician + " ";
        }
        if (isMerged()) {
            // Consultant + Assistant collapse into one bucket (reachable only via
            // ProfessionalCharge — DoctorAndNurses is suppressed above when merged).
            // A null category defaults to ProfessionalCharge, i.e. not Technician,
            // so it must match here too — spelled out explicitly rather than via
            // coalesce(), which this EclipseLink version does not evaluate
            // correctly combined with a fully-qualified enum literal comparison
            // (confirmed by hand: the equivalent raw SQL filters correctly, the
            // JPQL coalesce(...) != <literal> form does not).
            return staffNotNull + " and (" + categoryField + " is null or " + categoryField + " != " + technician + ") ";
        }
        if (target == InwardChargeType.ProfessionalCharge) {
            // A null category defaults to ProfessionalCharge.
            return staffNotNull + " and (" + categoryField + " is null or " + categoryField
                    + " = com.divudi.core.data.inward.InwardChargeType.ProfessionalCharge) ";
        }
        // DoctorAndNurses: null never defaults here, so no null-check needed.
        return staffNotNull + " and " + categoryField + " = com.divudi.core.data.inward.InwardChargeType." + target.name() + " ";
    }

    /**
     * The category half of {@link #staffCondition} for
     * {@link InwardChargeType#ProfessionalCharge}, negated: true for a fee whose
     * saved category puts it outside the consultant bucket (technician fees, and
     * assistant fees when not merged). It has no leading {@code and} and no
     * staff test, so a caller can OR it with its own null-staff/null-fee tests
     * to build the exact complement of the professional bucket — e.g. the
     * "other" side of a professional/other split (issue #23982).
     *
     * <p>A null category counts as Consultant Fee, so it never matches here.
     */
    public String notProfessionalChargeCategory(String feeAlias) {
        String categoryField = feeAlias + ".professionalFeeCategory";
        if (isMerged()) {
            return " " + categoryField
                    + " = com.divudi.core.data.inward.InwardChargeType.TechnicianAndParamedicalCharge ";
        }
        return " (" + categoryField + " is not null and " + categoryField
                + " != com.divudi.core.data.inward.InwardChargeType.ProfessionalCharge) ";
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
