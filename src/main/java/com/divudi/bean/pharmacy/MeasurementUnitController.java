/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;

import com.divudi.entity.Item;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.MeasurementUnit;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.facade.MeasurementUnitFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Informatics)
 */
@Named
@SessionScoped
public class MeasurementUnitController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private MeasurementUnitFacade ejbFacade;
    List<MeasurementUnit> selectedItems;
    private MeasurementUnit current;
    private List<MeasurementUnit> items = null;
    String selectText = "";
    List<MeasurementUnit> doseUnits;
    List<MeasurementUnit> durationUnits;
    List<MeasurementUnit> frequencyUnits;
    List<MeasurementUnit> issueUnits;
    List<MeasurementUnit> packUnits;
    List<MeasurementUnit> strengthUnits;
    List<MeasurementUnit> allUnits;

    public String navigateToAddMeasurementUnit() {
        current = new MeasurementUnit();
        return "/pharmacy/admin/unit";
    }
    
    public String navigateToManageMeasurementUnit() {
        current = new MeasurementUnit();
        return "/pharmacy/admin/manage_unit";
    }

    public String navigateToListAllMeasurementUnit() {

        return "/pharmacy/admin/units";
    }

    public String navigateToEditMeasurementUnit() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing");
            return "";
        }
        return "/pharmacy/admin/unit";
    }

    public void fillAllUnits() {
        String jpql;
        Map m = new HashMap();
        jpql = "select m "
                + " from MeasurementUnit m "
                + " where m.retired=:ret "
                + " order by m.name";
        m.put("ret", false);
        allUnits = getFacade().findByJpql(jpql, m);
        doseUnits = new ArrayList<>();
        durationUnits = new ArrayList<>();
        frequencyUnits = new ArrayList<>();
        issueUnits = new ArrayList<>();
        packUnits = new ArrayList<>();
        strengthUnits = new ArrayList<>();
        if (allUnits == null) {
            return;
        }
        for (MeasurementUnit mu : allUnits) {
            if (mu.isIssueUnit()) {
                issueUnits.add(mu);
                doseUnits.add(mu);
                durationUnits.add(mu);
            } else if (mu.isPackUnit()) {
                doseUnits.add(mu);
                packUnits.add(mu);
                durationUnits.add(mu);
            } else if (mu.isStrengthUnit()) {
                strengthUnits.add(mu);
                doseUnits.add(mu);
            } else if (mu.isDurationUnit()) {
                durationUnits.add(mu);
            } else if (mu.isFrequencyUnit()) {
                frequencyUnits.add(mu);
            }
        }
    }

    public MeasurementUnit findAndSaveMeasurementUnitByName(String name) {
        String jpql;
        Map m = new HashMap();
        jpql = "select m "
                + " from MeasurementUnit m "
                + " where m.retired=:ret "
                + " and m.name=:name";
        m.put("ret", false);
        m.put("name", name);
        MeasurementUnit mu = getFacade().findFirstByJpql(jpql, m);
        if (mu == null) {
            mu = new MeasurementUnit();
            mu.setName(name);
            mu.setCode("measurement_unit_" + CommonController.nameToCode(name));
            getFacade().create(mu);
        }
        return mu;
    }

    public void save(MeasurementUnit mu) {
        if (mu == null) {
            return;
        }
        if (mu.getId() == null) {
            getFacade().create(mu);
        } else {
            getFacade().edit(mu);
        }
    }

    public void prepareAdd() {
        current = new MeasurementUnit();
    }

    public void setSelectedItems(List<MeasurementUnit> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
        fillAllUnits();
    }

    public void saveSelected() {
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        allUnits = null;
        getAllUnits();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public MeasurementUnitFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(MeasurementUnitFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public MeasurementUnitController() {
    }

    public MeasurementUnit getCurrent() {
        return current;
    }

    public void setCurrent(MeasurementUnit current) {
        this.current = current;
    }

    public void delete() {
        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private MeasurementUnitFacade getFacade() {
        return ejbFacade;
    }

    public List<MeasurementUnit> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    public List<MeasurementUnit> getDoseUnits() {
        if (doseUnits == null) {
            fillAllUnits();
        }
        return doseUnits;
    }

    public List<MeasurementUnit> getDurationUnits() {
        if (durationUnits == null) {
            fillAllUnits();
        }
        return durationUnits;
    }

    public List<MeasurementUnit> getFrequencyUnits() {
        if (frequencyUnits == null) {
            fillAllUnits();
        }
        return frequencyUnits;
    }

    public List<MeasurementUnit> getDoseUnitsForMedicine(Item medicine) {
        fillAllUnits(); //TODO - Optimize
        if (medicine == null) {

            return getDoseUnits();
        }
        if (medicine instanceof Amp) {

            List<MeasurementUnit> us = new ArrayList<>();
            Set<MeasurementUnit> uniqueUnits = new HashSet<>();
            Amp amp = (Amp) medicine;
            if (amp.getIssueUnit() != null) {
                uniqueUnits.add(amp.getIssueUnit());
            }
            if (amp.getStrengthUnit() != null) {
                uniqueUnits.add(amp.getStrengthUnit());
            }
            if (amp.getVmp() != null) {
                if (amp.getVmp().getMeasurementUnit() != null) {
                    uniqueUnits.add(amp.getVmp().getMeasurementUnit());
                }
                if (amp.getVmp().getStrengthUnit() != null) {
                    uniqueUnits.add(amp.getVmp().getStrengthUnit());
                }
            }
            us.addAll(uniqueUnits);
            return us;
        } else if (medicine instanceof Vmp) {

            List<MeasurementUnit> us = new ArrayList<>();
            Set<MeasurementUnit> uniqueUnits = new HashSet<>();
            Vmp vmp = (Vmp) medicine;
            if (vmp.getIssueUnit() != null) {
                uniqueUnits.add(vmp.getIssueUnit());
            }
            if (vmp.getStrengthUnit() != null) {
                uniqueUnits.add(vmp.getStrengthUnit());
            }
            us.addAll(uniqueUnits);
            return us;
        } else {
            return getDoseUnits();
        }
    }

    public List<MeasurementUnit> getIssueUnits() {
        if (issueUnits == null) {
            fillAllUnits();
        }
        return issueUnits;
    }

    public List<MeasurementUnit> getPackUnits() {
        if (packUnits == null) {
            fillAllUnits();
        }
        return packUnits;
    }

    public List<MeasurementUnit> getStrengthUnits() {
        if (strengthUnits == null) {
            fillAllUnits();
        }
        return strengthUnits;
    }

    public List<MeasurementUnit> getAllUnits() {
        if (allUnits == null) {
            fillAllUnits();
        }
        return allUnits;
    }

    /**
     *
     */
    @FacesConverter(forClass = MeasurementUnit.class)
    public static class MeasurementUnitControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            MeasurementUnitController controller = (MeasurementUnitController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "measurementUnitController");
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
            if (object instanceof MeasurementUnit) {
                MeasurementUnit o = (MeasurementUnit) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + MeasurementUnitController.class.getName());
            }
        }
    }
}
