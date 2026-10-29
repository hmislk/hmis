package com.divudi.bean.common;

import com.divudi.bean.hr.StaffController;
import com.divudi.core.data.StaffMappingType;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.StaffMapping;
import com.divudi.core.facade.StaffFacade;
import com.divudi.core.facade.StaffMappingFacade;
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

/**
 * Manages {@link StaffMapping} rows and serves the staff autocompletes that can
 * be limited to staff mapped for a purpose.
 */
@Named(value = "staffMappingController")
@SessionScoped
public class StaffMappingController implements Serializable {

    private static final long serialVersionUID = 1L;

    static final String SAMPLE_TRANSPORTER_LIMITED_KEY
            = "Sample Transporter List Is Limited To Staff Mapped As Sample Transporters";

    @EJB
    private StaffMappingFacade ejbFacade;
    @EJB
    private StaffFacade staffFacade;
    @Inject
    private SessionController sessionController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private StaffController staffController;

    private StaffMappingType mappingType = StaffMappingType.SAMPLE_TRANSPORTER;
    private Staff staff;
    private List<StaffMapping> mappings;

    public String navigateToManageStaffMappings() {
        staff = null;
        fillMappings();
        return "/admin/staff/manage_staff_mappings?faces-redirect=true";
    }

    public void fillMappings() {
        String jpql = "select m from StaffMapping m "
                + " where m.retired=false "
                + " and m.mappingType=:t "
                + " order by m.staff.person.name";
        Map<String, Object> params = new HashMap<>();
        params.put("t", mappingType);
        mappings = ejbFacade.findByJpql(jpql, params);
    }

    public void addMapping() {
        if (staff == null) {
            JsfUtil.addErrorMessage("Select a staff member to map");
            return;
        }
        if (mappingType == null) {
            JsfUtil.addErrorMessage("Select a mapping type");
            return;
        }
        String jpql = "select m from StaffMapping m "
                + " where m.retired=false "
                + " and m.mappingType=:t "
                + " and m.staff=:s";
        Map<String, Object> params = new HashMap<>();
        params.put("t", mappingType);
        params.put("s", staff);
        if (ejbFacade.findFirstByJpql(jpql, params) != null) {
            JsfUtil.addErrorMessage("This staff member is already mapped as " + mappingType.getLabel());
            return;
        }
        StaffMapping m = new StaffMapping();
        m.setStaff(staff);
        m.setMappingType(mappingType);
        m.setCreatedAt(new Date());
        m.setCreater(sessionController.getLoggedUser());
        ejbFacade.create(m);
        staff = null;
        fillMappings();
        JsfUtil.addSuccessMessage("Mapped");
    }

    public void removeMapping(StaffMapping m) {
        if (m == null) {
            return;
        }
        m.setRetired(true);
        m.setRetiredAt(new Date());
        m.setRetirer(sessionController.getLoggedUser());
        ejbFacade.edit(m);
        fillMappings();
        JsfUtil.addSuccessMessage("Mapping removed");
    }

    /**
     * Autocomplete for the sample transporter boxes. Offers all staff unless the
     * hospital has switched on the limit, in which case only staff mapped as
     * sample transporters are offered.
     */
    public List<Staff> completeSampleTransporter(String query) {
        if (!configOptionApplicationController.getBooleanValueByKey(SAMPLE_TRANSPORTER_LIMITED_KEY, false)) {
            return staffController.completeStaff(query);
        }
        if (query == null) {
            return new ArrayList<>();
        }
        String jpql = "select m.staff from StaffMapping m "
                + " where m.retired=false "
                + " and m.mappingType=:t "
                + " and m.staff.retired=false "
                + " and ((m.staff.person.name) like :q "
                + " or (m.staff.staffCode) like :q "
                + " or (m.staff.code) like :q "
                + " or (m.staff.epfNo) like :q) "
                + " order by m.staff.person.name";
        Map<String, Object> params = new HashMap<>();
        params.put("t", StaffMappingType.SAMPLE_TRANSPORTER);
        params.put("q", "%" + query.toUpperCase() + "%");
        return staffFacade.findByJpql(jpql, params, 20);
    }

    public StaffMappingType[] getMappingTypes() {
        return StaffMappingType.values();
    }

    public StaffMappingType getMappingType() {
        return mappingType;
    }

    public void setMappingType(StaffMappingType mappingType) {
        this.mappingType = mappingType;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public List<StaffMapping> getMappings() {
        return mappings;
    }
}
