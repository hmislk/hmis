/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Consultant
 * (Health Informatics)
 *
 */
@Named
@SessionScoped
public class BillItemController implements Serializable {

    @EJB
    BillItemFacade billItemFacade;
    @EJB
    PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    PatientInvestigationFacade patientInvestigationFacade;

    @Inject
    SessionController sessionController;

    private List<BillItem> items = null;
    private BillItem selected;

    public List<BillItem> getItems() {
        return items;
    }

    public void save() {
        save(selected);
    }

    public void save(BillItem sbi) {
        if (sbi == null) {
            return;
        }
        if (sbi.getId() == null) {
            if (sbi.getCreatedAt() == null) {
                sbi.setCreatedAt(new Date());
            }
            if (sbi.getCreater() == null) {
                sbi.setCreater(sessionController.getLoggedUser());
            }
            getFacade().create(sbi);
        } else {
            getFacade().edit(sbi);
        }
    }

    public void save(PharmaceuticalBillItem sbi) {
        if (sbi == null) {
            return;
        }
        if (sbi.getId() == null) {
            if (sbi.getCreatedAt() == null) {
                sbi.setCreatedAt(new Date());
            }
            if (sbi.getCreater() == null) {
                sbi.setCreater(sessionController.getLoggedUser());
            }
            pharmaceuticalBillItemFacade.create(sbi);
        } else {
            pharmaceuticalBillItemFacade.edit(sbi);
        }
    }

    public void save(PatientInvestigation sbi) {
        if (sbi == null) {
            return;
        }
        if (sbi.getId() == null) {
            if (sbi.getCreatedAt() == null) {
                sbi.setCreatedAt(new Date());
            }
            if (sbi.getCreater() == null) {
                sbi.setCreater(sessionController.getLoggedUser());
            }
            patientInvestigationFacade.create(sbi);
        } else {
            patientInvestigationFacade.edit(sbi);
        }
    }

    
    public void save(List<BillItem> billItems) {
        if (billItems == null || billItems.isEmpty()) {
            return;
        }
        for (BillItem sbi : billItems) {
            save(sbi);
        }
    }

    public void savePharmaceuticalItems(List<PharmaceuticalBillItem> pharmaceuticalBillItems) {
        if (pharmaceuticalBillItems == null || pharmaceuticalBillItems.isEmpty()) {
            return;
        }
        for (PharmaceuticalBillItem sbi : pharmaceuticalBillItems) {
            save(sbi);
        }
    }

    public void setItems(List<BillItem> items) {
        this.items = items;
    }

    public BillItem getSelected() {
        return selected;
    }

    public void setSelected(BillItem selected) {
        this.selected = selected;
    }

    public BillItemController() {
    }

    public BillItem findBillItemInListBySerial(Integer id) {
        if (items == null) {
            return null;
        }
        for (BillItem bi : items) {
            if (id.equals(bi.getSearialNo())) {
                return bi;
            }
        }
        return null;
    }

    private BillItemFacade getFacade() {
        return billItemFacade;
    }

    void savePatientInvestigations(List<PatientInvestigation> viewingPatientInvestigations) {
        if (viewingPatientInvestigations == null || viewingPatientInvestigations.isEmpty()) {
            return;
        }
        for (PatientInvestigation sbi : viewingPatientInvestigations) {
            save(sbi);
        }

    }

//    @FacesConverter("temBillItemConverter")
//    public static class TemBillItemConverter implements Converter {
//
//        @Override
//        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
//            if (value == null || value.length() == 0) {
//                ////// // System.out.println("value = " + value);
////                ////// // System.out.println("value.length() = " + value.length());
//                return null;
//            }
//            BillItemController controller = (BillItemController) facesContext.getApplication().getELResolver().
//                    getValue(facesContext.getELContext(), null, "billItemController");
//            if (controller == null) {
//                ////// // System.out.println("null controller");
//                return null;
//            }
//            if (getKey(value) == null) {
//                ////// // System.out.println("value null");
//                return null;
//            }
//            return controller.findBillItemInListBySerial(getKey(value));
//        }
//
//        java.lang.Integer getKey(String value) {
//            java.lang.Integer key = null;
//            try {
//                key = Integer.valueOf(value);
//            } catch (NumberFormatException e) {
//            }
//
//            return key;
//        }
//
//        String getStringKey(java.lang.Long value) {
//            StringBuilder sb = new StringBuilder();
//            sb.append(value);
//            return sb.toString();
//        }
//
//        @Override
//        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
//            if (object == null) {
//                return null;
//            }
//            if (object instanceof BillItem) {
//                BillItem o = (BillItem) object;
//                return getStringKey(o.getId());
//            } else {
//                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "object {0} is of type {1}; expected type: {2}", new Object[]{object, object.getClass().getName(), BillItem.class.getName()});
//                return null;
//            }
//        }
//
//    }
    @FacesConverter(forClass = BillItem.class)
    public static class BillItemControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                ////// // System.out.println("value = " + value);
                ////// // System.out.println("value.length() = " + value.length());
                return null;
            }
            BillItemController controller = (BillItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "billItemController");
            if (controller == null) {
                ////// // System.out.println("null controller");
                return null;
            }
            if (controller.getFacade() == null) {
                ////// // System.out.println("facade null");
                return null;
            }
            if (getKey(value) == null) {
                ////// // System.out.println("value null");
                return null;
            }
            return controller.getFacade().find(getKey(value));
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
            if (object instanceof BillItem) {
                BillItem o = (BillItem) object;
                return getStringKey(o.getId());
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "object {0} is of type {1}; expected type: {2}", new Object[]{object, object.getClass().getName(), BillItem.class.getName()});
                return null;
            }
        }

    }

}
