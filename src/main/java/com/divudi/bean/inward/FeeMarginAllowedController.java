package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.ItemFee;
import com.divudi.core.entity.Service;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.facade.ItemFeeFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Admin tool to bulk-enable the fee-level "Service Charge Allowed"
 * (marginAllowed) flag on base item fees. Fees where marginAllowed is FALSE are
 * skipped during inward service-charge calculation, so this screen lets an admin
 * flip selected fees to allowed in one pass.
 */
@Named
@ViewScoped
public class FeeMarginAllowedController implements Serializable {

    @EJB
    private ItemFeeFacade itemFeeFacade;
    @Inject
    private SessionController sessionController;

    private Department department;
    private String itemType = "Service";
    private List<ItemFeeMarginGroup> groups;

    public String navigateToManageFeeMarginAllowed() {
        department = null;
        itemType = "Service";
        groups = null;
        return "/inward/inward_fee_margin_allowed?faces-redirect=true";
    }

    public void process() {
        groups = new ArrayList<>();
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department.");
            return;
        }
        Class<? extends Item> typeClass = "Investigation".equals(itemType) ? Investigation.class : Service.class;

        String jpql = "select f from ItemFee f "
                + " where f.retired = false "
                + " and f.forInstitution is null "
                + " and f.forCategory is null "
                + " and f.item.department = :dept "
                + " and type(f.item) = :t "
                + " order by f.item.name, f.name";
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dept", department);
        m.put("t", typeClass);

        List<ItemFee> fees = itemFeeFacade.findByJpql(jpql, m);

        Map<Long, ItemFeeMarginGroup> grouped = new LinkedHashMap<>();
        for (ItemFee f : fees) {
            if (f.getItem() == null || f.getItem().getId() == null) {
                continue;
            }
            ItemFeeMarginGroup group = grouped.get(f.getItem().getId());
            if (group == null) {
                group = new ItemFeeMarginGroup(f.getItem(), itemType);
                grouped.put(f.getItem().getId(), group);
            }
            group.getFeeRows().add(new FeeRow(f));
        }
        groups = new ArrayList<>(grouped.values());

        if (groups.isEmpty()) {
            JsfUtil.addErrorMessage("No fees found for the selected department and item type.");
        } else {
            JsfUtil.addSuccessMessage(groups.size() + " items loaded.");
        }
    }

    public void updateSelectedFees() {
        if (groups == null || groups.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing to update. Process first.");
            return;
        }
        int updated = 0;
        Date now = new Date();
        for (ItemFeeMarginGroup group : groups) {
            for (FeeRow row : group.getFeeRows()) {
                if (!row.isSelected()) {
                    continue;
                }
                ItemFee f = row.getItemFee();
                if (Boolean.TRUE.equals(f.getMarginAllowed())) {
                    continue;
                }
                f.setMarginAllowed(true);
                f.setEditedAt(now);
                f.setEditer(sessionController.getLoggedUser());
                itemFeeFacade.edit(f);
                updated++;
            }
        }
        if (updated == 0) {
            JsfUtil.addErrorMessage("No fees selected (or all selected fees are already allowed).");
        } else {
            JsfUtil.addSuccessMessage(updated + " fees updated to Service Charge Allowed.");
        }
    }

    public void selectAll() {
        setAllSelected(true);
    }

    public void deselectAll() {
        setAllSelected(false);
    }

    private void setAllSelected(boolean value) {
        if (groups == null) {
            return;
        }
        for (ItemFeeMarginGroup group : groups) {
            for (FeeRow row : group.getFeeRows()) {
                row.setSelected(value);
            }
        }
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public List<ItemFeeMarginGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<ItemFeeMarginGroup> groups) {
        this.groups = groups;
    }

    public static class ItemFeeMarginGroup implements Serializable {

        private final Item item;
        private final String type;
        private final List<FeeRow> feeRows = new ArrayList<>();

        public ItemFeeMarginGroup(Item item, String type) {
            this.item = item;
            this.type = type;
        }

        public Item getItem() {
            return item;
        }

        public String getType() {
            return type;
        }

        public List<FeeRow> getFeeRows() {
            return feeRows;
        }
    }

    public static class FeeRow implements Serializable {

        private final ItemFee itemFee;
        private boolean selected;

        public FeeRow(ItemFee itemFee) {
            this.itemFee = itemFee;
            this.selected = !Boolean.TRUE.equals(itemFee.getMarginAllowed());
        }

        public ItemFee getItemFee() {
            return itemFee;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }
}
