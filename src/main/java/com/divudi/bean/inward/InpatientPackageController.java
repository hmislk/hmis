package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.inward.InwardChargeType;
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
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@SessionScoped
public class InpatientPackageController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionController sessionController;

    @Inject
    private WebUserController webUserController;

    @EJB
    private InpatientPackageFacade ejbFacade;

    @EJB
    private InpatientPackageItemFacade inpatientPackageItemFacade;

    private InpatientPackage current;
    private List<InpatientPackage> items;

    private Map<String, String> amountInputMap;
    private InpatientPackage amountInputMapOwner;

    public void prepareAdd() {
        current = new InpatientPackage();
        items = null;
        amountInputMap = null;
        amountInputMapOwner = null;
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
        current.setRetireComments("Deleted from Manage Inpatient Packages");
        ejbFacade.edit(current);
        items = null;
        current = null;
        JsfUtil.addSuccessMessage("Deleted Successfully");
    }

    public void saveSelected() {
        if (!webUserController.hasPrivilege("InwardPackageAdministration")) {
            JsfUtil.addErrorMessage("You are not authorized to manage Inpatient Packages.");
            return;
        }
        if (current == null) {
            JsfUtil.addErrorMessage("Please click Add to create a new package, or select a package to edit");
            return;
        }
        boolean missingRequired = false;
        if (current.getName() == null || current.getName().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a package name");
            missingRequired = true;
        }
        if (current.getAdmissionType() == null) {
            JsfUtil.addErrorMessage("Please select an Admission Type");
            missingRequired = true;
        }
        if (current.getRoomCategory() == null) {
            JsfUtil.addErrorMessage("Please select a Room Category");
            missingRequired = true;
        }
        if (missingRequired) {
            return;
        }
        List<InpatientPackageItem> components = new ArrayList<>();
        if (current.getId() != null) {
            Map<String, Object> params = new HashMap<>();
            params.put("pkg", current);
            components = inpatientPackageItemFacade.findByJpql(
                    "SELECT i FROM InpatientPackageItem i WHERE i.retired = false AND i.inpatientPackage = :pkg",
                    params);
        }
        Map<String, Double> parsedAmounts = new HashMap<>();
        if (amountInputMap != null) {
            for (Map.Entry<String, String> e : amountInputMap.entrySet()) {
                Double amount = parseAmount(e.getValue());
                if (amount != null) {
                    parsedAmounts.put(e.getKey(), amount);
                }
            }
        }
        current.setChargeTypeAmounts(parsedAmounts);
        current.setFixedRoomCharge(parsedAmounts.getOrDefault(InwardChargeType.RoomCharges.name(), 0.0));
        current.setTotalPrice(InpatientPackagePricing.calculateTotalPrice(parsedAmounts, components));
        if (current.getId() != null) {
            ejbFacade.edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(sessionController.getLoggedUser());
            ejbFacade.create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        items = null;
        amountInputMap = null;
        amountInputMapOwner = null;
    }

    public Map<String, String> getAmountInputMap() {
        if (amountInputMap == null || amountInputMapOwner != current) {
            amountInputMap = new HashMap<>();
            amountInputMapOwner = current;
            if (current != null && current.getChargeTypeAmounts() != null) {
                for (Map.Entry<String, Double> e : current.getChargeTypeAmounts().entrySet()) {
                    amountInputMap.put(e.getKey(), formatAmount(e.getValue()));
                }
            }
        }
        return amountInputMap;
    }

    public void setAmountInputMap(Map<String, String> amountInputMap) {
        this.amountInputMap = amountInputMap;
    }

    private String formatAmount(Double v) {
        if (v == null) {
            return "";
        }
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.valueOf(v.longValue());
        }
        return String.valueOf(v);
    }

    private Double parseAmount(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<InpatientPackage> getItems() {
        if (items == null) {
            items = ejbFacade.findByJpql("SELECT p FROM InpatientPackage p WHERE p.retired = false ORDER BY p.name");
        }
        return items;
    }

    public InpatientPackage getCurrent() {
        if (current == null) {
            current = new InpatientPackage();
        }
        return current;
    }

    public void setCurrent(InpatientPackage current) {
        this.current = current;
    }

    public String navigateToManageInpatientPackagesFromMenu() {
        current = null;
        items = null;
        return "/inward/inward_inpatient_package?faces-redirect=true";
    }

    public InpatientPackageFacade getEjbFacade() {
        return ejbFacade;
    }

    @FacesConverter(forClass = InpatientPackage.class)
    public static class InpatientPackageControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            InpatientPackageController controller = (InpatientPackageController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "inpatientPackageController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof InpatientPackage) {
                InpatientPackage o = (InpatientPackage) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + InpatientPackageController.class.getName());
            }
        }
    }
}
