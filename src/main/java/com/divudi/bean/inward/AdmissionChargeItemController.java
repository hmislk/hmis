/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.inward.AdmissionChargeItem;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.AdmissionChargeItemFacade;
import com.divudi.core.facade.ItemFeeFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.common.SessionController;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Manages {@link AdmissionChargeItem} rows - the routine admission charges
 * that {@code AdmissionChargeApplicationBean} bills automatically on every
 * matching admission (issue #23594).
 *
 * <p>Resolution is a two-step filter, matching
 * {@code AdmissionChargeApplicationBean.resolveForItem}: for a given
 * {@link Item}, admission type is the outer filter (a {@code null}
 * admission type row applies to every admission type that has no
 * type-specific row of its own) and payment method is the inner filter
 * (a {@code null} payment method row applies to both Cash and Credit within
 * whichever admission-type group matched). Because step one is a filter and
 * not a preference, configuring even one admission-type-specific row for an
 * item completely replaces the "any type" rows for that admission type - so
 * both Cash and Credit must be covered for that admission type, or one of
 * them silently gets no charge at all. This controller validates against
 * that trap on save and also warns about the unrelated, but easily confused,
 * trap of double-charging an admission type that already carries its own
 * built-in {@code AdmissionType.admissionFee}.</p>
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class AdmissionChargeItemController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionController sessionController;

    @EJB
    private AdmissionChargeItemFacade ejbFacade;

    @EJB
    private ItemFeeFacade itemFeeFacade;

    @EJB
    private com.divudi.core.facade.AdmissionTypeFacade admissionTypeFacade;

    private AdmissionChargeItem current;
    private List<AdmissionChargeItem> items;

    // Filters
    private Item filterItem;
    private AdmissionType filterAdmissionType;
    private PaymentMethod filterPaymentMethod;

    public String navigateToManageAdmissionCharges() {
        fillItems();
        prepareAdd();
        return "/inward/inward_admission_charge_item?faces-redirect=true";
    }

    public void prepareAdd() {
        current = new AdmissionChargeItem();
        current.setQty(1.0);
    }

    public void search() {
        fillItems();
    }

    public void clearFilters() {
        filterItem = null;
        filterAdmissionType = null;
        filterPaymentMethod = null;
        fillItems();
    }

    public void fillItems() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder j = new StringBuilder("select a from AdmissionChargeItem a where a.retired=false ");
        if (filterItem != null) {
            j.append(" and a.item=:itm ");
            params.put("itm", filterItem);
        }
        if (filterAdmissionType != null) {
            j.append(" and a.admissionType=:at ");
            params.put("at", filterAdmissionType);
        }
        if (filterPaymentMethod != null) {
            j.append(" and a.paymentMethod=:pm ");
            params.put("pm", filterPaymentMethod);
        }
        j.append(" order by a.item.name, a.orderNo, a.id");
        items = ejbFacade.findByJpql(j.toString(), params);
    }

    public List<AdmissionChargeItem> getItems() {
        if (items == null) {
            fillItems();
        }
        return items;
    }

    /**
     * Validates, then creates/edits the current row.
     *
     * <p>Validation mirrors what
     * {@code AdmissionChargeApplicationBean.buildEntry} and
     * {@code resolveFee} need at runtime, so a row that passes here can
     * never be silently skipped (or worse, billed at zero) when an admission
     * is saved.</p>
     */
    public void saveSelected() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to save.");
            return;
        }

        if (current.getItem() == null) {
            JsfUtil.addErrorMessage("Please select the service item.");
            return;
        }
        Item item = current.getItem();

        if (item.getDepartment() == null) {
            JsfUtil.addErrorMessage("This item has no department. The department decides how the generated bills are bundled - set it under Item administration first.");
            return;
        }
        if (item.getInstitution() == null) {
            JsfUtil.addErrorMessage("This item has no institution set. Set it under Item administration first.");
            return;
        }
        if (item.getInwardChargeType() == null) {
            JsfUtil.addErrorMessage("This item has no inward charge type. Without it the charge cannot appear under the right heading on the interim and final bill.");
            return;
        }

        Map<String, Object> feeCountParams = new HashMap<>();
        feeCountParams.put("itm", item);
        long liveFeeCount = itemFeeFacade.findLongByJpql(
                "select count(f) from ItemFee f where f.retired=false and f.item=:itm",
                feeCountParams);
        if (liveFeeCount <= 0) {
            JsfUtil.addErrorMessage("This item has no fee configured, so the charge would cancel and refund as zero.");
            return;
        }

        if (current.getPaymentMethod() != null
                && current.getPaymentMethod() != PaymentMethod.Cash
                && current.getPaymentMethod() != PaymentMethod.Credit) {
            JsfUtil.addErrorMessage("An admission is only ever Cash or Credit.");
            return;
        }

        if (current.getPrice() < 0) {
            JsfUtil.addErrorMessage("Price cannot be negative.");
            return;
        }

        if (isDuplicate(current)) {
            JsfUtil.addErrorMessage("A charge for this item, admission type and payment method already exists.");
            return;
        }

        boolean editing = current.getId() != null;
        if (editing) {
            ejbFacade.edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(sessionController.getLoggedUser());
            ejbFacade.create(current);
            JsfUtil.addSuccessMessage("Saved Successfully.");
        }

        for (String warning : coverageWarningsForItem(item)) {
            JsfUtil.addWarningMessage(warning);
        }
        for (String warning : admissionFeeDoubleChargeWarnings(current.getAdmissionType())) {
            JsfUtil.addWarningMessage(warning);
        }

        fillItems();
        prepareAdd();
    }

    /**
     * Warns when the admission types this row will apply to already carry their
     * own built-in {@code AdmissionType.admissionFee}.
     *
     * <p>That fee is a separate, older mechanism: it is added to the interim and
     * final bill under the <i>Admission Fee</i> charge type without ever being
     * persisted as a BillItem. Configuring an admission charge here for the same
     * thing charges the patient twice.</p>
     *
     * <p>A row with no admission type applies to <b>every</b> admission type, so
     * that case has to check them all rather than just the one selected.</p>
     */
    private List<String> admissionFeeDoubleChargeWarnings(AdmissionType selectedAdmissionType) {
        List<String> warnings = new ArrayList<>();
        List<AdmissionType> typesToCheck = new ArrayList<>();

        if (selectedAdmissionType != null) {
            typesToCheck.add(selectedAdmissionType);
        } else {
            List<AdmissionType> all = admissionTypeFacade.findByJpql(
                    "select a from AdmissionType a where a.retired=false and a.admissionFee <> 0 order by a.name");
            if (all != null) {
                typesToCheck.addAll(all);
            }
        }

        for (AdmissionType at : typesToCheck) {
            if (at.getAdmissionFee() == 0.0) {
                continue;
            }
            warnings.add("Warning: admission type '" + at.getName()
                    + "' already carries a built-in admission fee of " + at.getAdmissionFee()
                    + ", which is added to the bill separately. Charging this item on that admission type as well will charge the patient twice.");
        }
        return warnings;
    }

    /**
     * True when a live row already exists for the same
     * (item, admissionType, paymentMethod) triple, excluding {@code row}
     * itself when it is being edited. {@code null} admission type / payment
     * method are matched with {@code is null} rather than a bound parameter,
     * since JPQL's {@code = :param} never matches a null value.
     */
    private boolean isDuplicate(AdmissionChargeItem row) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder j = new StringBuilder("select count(a) from AdmissionChargeItem a where a.retired=false and a.item=:itm ");
        params.put("itm", row.getItem());

        if (row.getAdmissionType() != null) {
            j.append(" and a.admissionType=:at ");
            params.put("at", row.getAdmissionType());
        } else {
            j.append(" and a.admissionType is null ");
        }

        if (row.getPaymentMethod() != null) {
            j.append(" and a.paymentMethod=:pm ");
            params.put("pm", row.getPaymentMethod());
        } else {
            j.append(" and a.paymentMethod is null ");
        }

        if (row.getId() != null) {
            j.append(" and a.id <> :selfId ");
            params.put("selfId", row.getId());
        }

        return ejbFacade.findLongByJpql(j.toString(), params) > 0;
    }

    /**
     * Warns when, for {@code item}, an admission type has been given a
     * Cash-only or Credit-only row set with no "any" row - meaning the other
     * payment method gets no charge at all for that admission type, because
     * admission-type-specific rows completely replace the "any type" rows.
     */
    public List<String> coverageWarningsForItem(Item item) {
        List<String> warnings = new ArrayList<>();
        if (item == null) {
            return warnings;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("itm", item);
        List<AdmissionChargeItem> rows = ejbFacade.findByJpql(
                "select a from AdmissionChargeItem a where a.retired=false and a.item=:itm",
                params);
        if (rows == null || rows.isEmpty()) {
            return warnings;
        }

        Set<AdmissionType> admissionTypesUsed = new LinkedHashSet<>();
        for (AdmissionChargeItem row : rows) {
            if (row.getAdmissionType() != null) {
                admissionTypesUsed.add(row.getAdmissionType());
            }
        }
        if (admissionTypesUsed.isEmpty()) {
            return warnings;
        }

        for (AdmissionType at : admissionTypesUsed) {
            boolean hasAny = false;
            boolean hasCash = false;
            boolean hasCredit = false;
            for (AdmissionChargeItem row : rows) {
                if (!at.equals(row.getAdmissionType())) {
                    continue;
                }
                if (row.getPaymentMethod() == null) {
                    hasAny = true;
                } else if (row.getPaymentMethod() == PaymentMethod.Cash) {
                    hasCash = true;
                } else if (row.getPaymentMethod() == PaymentMethod.Credit) {
                    hasCredit = true;
                }
            }
            if (hasAny) {
                continue;
            }
            if (hasCash && !hasCredit) {
                warnings.add("Warning: '" + item.getName() + "' has a " + at.getName() + " row for Cash only. "
                        + "A Credit " + at.getName() + " admission will get NO charge for this item at all, "
                        + "because admission-type-specific rows completely replace the 'any type' rows.");
            } else if (hasCredit && !hasCash) {
                warnings.add("Warning: '" + item.getName() + "' has a " + at.getName() + " row for Credit only. "
                        + "A Cash " + at.getName() + " admission will get NO charge for this item at all, "
                        + "because admission-type-specific rows completely replace the 'any type' rows.");
            }
        }
        return warnings;
    }

    public void delete() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Nothing to delete.");
            return;
        }
        current.setRetired(true);
        current.setRetirer(sessionController.getLoggedUser());
        current.setRetiredAt(new Date());
        ejbFacade.edit(current);
        JsfUtil.addSuccessMessage("Deleted Successfully.");
        fillItems();
        prepareAdd();
    }

    public AdmissionChargeItem getCurrent() {
        if (current == null) {
            current = new AdmissionChargeItem();
        }
        return current;
    }

    public void setCurrent(AdmissionChargeItem current) {
        this.current = current;
    }

    public Item getFilterItem() {
        return filterItem;
    }

    public void setFilterItem(Item filterItem) {
        this.filterItem = filterItem;
    }

    public AdmissionType getFilterAdmissionType() {
        return filterAdmissionType;
    }

    public void setFilterAdmissionType(AdmissionType filterAdmissionType) {
        this.filterAdmissionType = filterAdmissionType;
    }

    public PaymentMethod getFilterPaymentMethod() {
        return filterPaymentMethod;
    }

    public void setFilterPaymentMethod(PaymentMethod filterPaymentMethod) {
        this.filterPaymentMethod = filterPaymentMethod;
    }

}
