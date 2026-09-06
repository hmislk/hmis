package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.inward.InpatientPackageComponentType;
import com.divudi.core.entity.inward.InpatientPackage;
import com.divudi.core.entity.inward.InpatientPackageItem;
import com.divudi.core.facade.InpatientPackageFacade;
import com.divudi.core.facade.InpatientPackageItemFacade;
import com.divudi.core.util.InpatientPackagePricing;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@SessionScoped
public class InpatientPackageItemController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionController sessionController;

    @Inject
    private WebUserController webUserController;

    @EJB
    private InpatientPackageItemFacade ejbFacade;

    @EJB
    private InpatientPackageFacade inpatientPackageFacade;

    private InpatientPackage currentPackage;
    private InpatientPackageItem current;
    private List<InpatientPackageItem> items;

    public String navigateToManageComponents(InpatientPackage pkg) {
        currentPackage = pkg;
        current = null;
        items = null;
        return "/inward/inward_inpatient_package_item?faces-redirect=true";
    }

    public void prepareAdd() {
        current = new InpatientPackageItem();
        current.setInpatientPackage(currentPackage);
        current.setQty(1.0);
    }

    public List<InpatientPackageItem> getItems() {
        if (currentPackage == null || currentPackage.getId() == null) {
            items = new ArrayList<>();
            return items;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("pkg", currentPackage);
        items = ejbFacade.findByJpql(
                "SELECT i FROM InpatientPackageItem i WHERE i.retired = false AND i.inpatientPackage = :pkg ORDER BY i.componentType, i.id",
                m);
        return items;
    }

    public void saveSelected() {
        if (!webUserController.hasPrivilege("InwardPackageAdministration")) {
            JsfUtil.addErrorMessage("You are not authorized to manage Inpatient Packages.");
            return;
        }
        if (current == null || current.getComponentType() == null) {
            JsfUtil.addErrorMessage("Please select a component type");
            return;
        }
        if (current.getComponentType() == InpatientPackageComponentType.PROFESSIONAL_FEE_ROLE) {
            if (current.getSpeciality() == null && (current.getRoleLabel() == null || current.getRoleLabel().trim().isEmpty())) {
                JsfUtil.addErrorMessage("Please select a Speciality or enter a Role Label");
                return;
            }
        } else if (current.getItem() == null) {
            JsfUtil.addErrorMessage("Please select an Item");
            return;
        }
        if (current.getQty() == null || current.getQty() <= 0) {
            JsfUtil.addErrorMessage("Please enter a quantity greater than zero");
            return;
        }
        if (current.getFixedPrice() == null || current.getFixedPrice() < 0) {
            JsfUtil.addErrorMessage("Please enter a fixed price");
            return;
        }
        current.setInpatientPackage(currentPackage);
        if (current.getId() != null) {
            ejbFacade.edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(sessionController.getLoggedUser());
            ejbFacade.create(current);
            JsfUtil.addSuccessMessage("Component Added");
        }
        current = null;
        items = null;
        recalculateTotal();
    }

    public void delete() {
        if (!webUserController.hasPrivilege("InwardPackageAdministration")) {
            JsfUtil.addErrorMessage("You are not authorized to manage Inpatient Packages.");
            return;
        }
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Nothing to delete");
            return;
        }
        current.setRetired(true);
        current.setRetirer(sessionController.getLoggedUser());
        current.setRetiredAt(new Date());
        current.setRetireComments("Removed from package");
        ejbFacade.edit(current);
        current = null;
        items = null;
        recalculateTotal();
    }

    private void recalculateTotal() {
        if (currentPackage == null || currentPackage.getId() == null) {
            return;
        }
        double fixedRoomCharge = currentPackage.getFixedRoomCharge() != null ? currentPackage.getFixedRoomCharge() : 0.0;
        double total = InpatientPackagePricing.calculateTotalPrice(fixedRoomCharge, getItems());
        currentPackage.setTotalPrice(total);
        inpatientPackageFacade.edit(currentPackage);
    }

    public InpatientPackage getCurrentPackage() {
        return currentPackage;
    }

    public void setCurrentPackage(InpatientPackage currentPackage) {
        this.currentPackage = currentPackage;
    }

    public InpatientPackageItem getCurrent() {
        if (current == null) {
            current = new InpatientPackageItem();
        }
        return current;
    }

    public void setCurrent(InpatientPackageItem current) {
        this.current = current;
    }

    public InpatientPackageComponentType[] getComponentTypes() {
        return InpatientPackageComponentType.values();
    }
}
