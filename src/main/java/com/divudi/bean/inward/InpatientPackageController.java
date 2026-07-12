package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.entity.inward.InpatientPackage;
import com.divudi.core.facade.InpatientPackageFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@SessionScoped
public class InpatientPackageController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionController sessionController;

    @EJB
    private InpatientPackageFacade ejbFacade;

    private InpatientPackage current;
    private List<InpatientPackage> items;

    public void prepareAdd() {
        current = new InpatientPackage();
        items = null;
    }

    public void delete() {
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
        if (current == null || current.getName() == null || current.getName().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a package name");
            return;
        }
        if (current.getAdmissionType() == null) {
            JsfUtil.addErrorMessage("Please select an Admission Type");
            return;
        }
        if (current.getRoomCategory() == null) {
            JsfUtil.addErrorMessage("Please select a Room Category");
            return;
        }
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
}
