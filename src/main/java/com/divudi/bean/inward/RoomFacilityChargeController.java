/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.inward.RoomFacility;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.entity.inward.RoomFacilityCharge;
import com.divudi.entity.inward.TimedItemFee;
import com.divudi.facade.RoomFacilityChargeFacade;
import com.divudi.facade.TimedItemFeeFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class RoomFacilityChargeController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private RoomFacilityChargeFacade ejbFacade;
    List<RoomFacilityCharge> selectedItems;
    private RoomFacilityCharge current;
    private List<RoomFacilityCharge> items = null;
    List<RoomFacilityCharge> roomFacilityCharges;
    String selectText = "";

    double adminstrationCharge = 0.0;
    double medicalCareCharge = 0.0;
    double roomCharge = 0.0;
    double maintananceCharge = 0.0;
    double nursingCharge = 0.0;
    double moCharge = 0.0;
    double linenCharge = 0.0;

    //            sql = "SELECT rm FROM "
//                    + " RoomFacilityCharge rm "
//                    + " WHERE rm.retired=false "
//                    + " AND rm.id NOT IN( "
//                    + " SELECT pr.roomFacilityCharge.id"
//                    + " FROM PatientRoom pr"
//                    + " WHERE pr.retired=false "
//                    + " AND pr.discharged=true )"
//                    + " AND (rm.name) LIKE :q"
//                    + " ORDER BY rm.name";
    public List<RoomFacilityCharge> completeRoom(String query) {
        
        List<RoomFacilityCharge> suggestions;
        String sql;
        HashMap hm = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {

            sql = "SELECT rm FROM "
                    + " RoomFacilityCharge rm "
                    + " WHERE rm.retired=false "
                    + " AND rm.room.filled!=true "
                    + " AND rm.room.id NOT IN( "
                    + " SELECT pr.roomFacilityCharge.room.id"
                    + " FROM PatientRoom pr"
                    + " WHERE pr.retired=false "
                    + " AND pr.discharged=false)"
                    + " AND (rm.name) LIKE :q"
                    + " ORDER BY rm.name";
            hm.put("q", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findByJpql(sql, hm);
        }
        return suggestions;
    }

    public List<RoomFacilityCharge> completeRoomChargeAll(String query) {
        List<RoomFacilityCharge> suggestions;
        String sql;
        HashMap hm = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {

            sql = "SELECT rm FROM "
                    + " RoomFacilityCharge rm "
                    + " WHERE rm.retired=false "
                    + " AND (rm.name) LIKE :q"
                    + " ORDER BY rm.name";
            hm.put("q", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findByJpql(sql, hm);
        }
        return suggestions;
    }

    AdmissionType admissionType;

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

//    public List<RoomFacilityCharge> completeRoomChange(String query) {
//        List<RoomFacilityCharge> suggestions;
//        String sql;
//        HashMap hm = new HashMap();
//
//        if (getCurrent() != null) {
//            sql = "select p from"
//                    + " RoomFacilityCharge p "
//                    + " where p.retired=false"
//                    + " and (p.room.filled=false or p.room=:rm) "
//                    + " and (p.name) like :q"
//                    + " order by p.name";
//            
//            hm.put("rm", getCurrent().getRoom());
//            hm.put("q", "%" + query.toUpperCase() + "%");
//            suggestions = getFacade().findByJpql(sql, hm);
//        } else {
//            suggestions = completeRoom(query);
//        }
//
//        return suggestions;
//    }
    public List<RoomFacilityCharge> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from RoomFacilityCharge c "
                + "where c.retired=false  and (c.name)"
                + " like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new RoomFacilityCharge();
        TimedItemFee tmp = new TimedItemFee();
        current.setTimedItemFee(tmp);
    }

    public void setSelectedItems(List<RoomFacilityCharge> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }
    @EJB
    private TimedItemFeeFacade timedItemFeeFacade;

    private TimedItemFee saveTimedItemFee() {
        TimedItemFee temp = getCurrent().getTimedItemFee();

        if (temp.getId() == null) {
            getTimedItemFeeFacade().create(temp);
        } else {
            getTimedItemFeeFacade().edit(temp);
        }

        return temp;
    }

    public void saveSelected() {
        saveTimedItemFee();

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void fillRoomFacilityCharge() {
        String sql = "SELECT i FROM RoomFacilityCharge i where i.retired=false ";
        roomFacilityCharges = getEjbFacade().findByJpql(sql);
    }

    public void updateRoomFacilityCharge(RoomFacilityCharge r) {

        ////// // System.out.println("r = " + r);
        ////// // System.out.println("r1 = " + r.getRoomCharge());
        ////// // System.out.println("r1 = " + r.getAdminstrationCharge());
        ////// // System.out.println("r1 = " + r.getMedicalCareCharge());
        ////// // System.out.println("r1 = " + r.getLinenCharge());
        getTimedItemFeeFacade().edit(r.getTimedItemFee());
        getFacade().edit(r);
        JsfUtil.addSuccessMessage("Updated");
//        fillRoomFacilityCharge();
    }

    public void updateAllCharges() {
        String sql = "SELECT i FROM RoomFacilityCharge i where i.retired=false ";
        roomFacilityCharges = getEjbFacade().findByJpql(sql);
        for (RoomFacilityCharge r : roomFacilityCharges) {
            r.setAdminstrationCharge(adminstrationCharge);
            r.setMedicalCareCharge(medicalCareCharge);
            r.setRoomCharge(roomCharge);
            r.setMaintananceCharge(maintananceCharge);
            r.setNursingCharge(nursingCharge);
            r.setMoCharge(moCharge);
            r.setLinenCharge(linenCharge);
            getFacade().edit(r);
        }
        JsfUtil.addSuccessMessage("Sucessfully Uptate All Room Charges");
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public RoomFacilityChargeFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(RoomFacilityChargeFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public RoomFacilityChargeController() {
    }

    public RoomFacilityCharge getCurrent() {
        if (current == null) {
            current = new RoomFacilityCharge();

            TimedItemFee tmp = new TimedItemFee();
            current.setTimedItemFee(tmp);
        }
        return current;
    }

    public void setCurrent(RoomFacilityCharge current) {
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

    private RoomFacilityChargeFacade getFacade() {
        return ejbFacade;
    }

    public List<RoomFacilityCharge> getItems() {
        if (items == null) {
            String sql = "SELECT i FROM RoomFacilityCharge i where i.retired=false ";
            items = getEjbFacade().findByJpql(sql);
        }
        return items;
    }

    public RoomFacility[] getRoomFacilitys() {
        return RoomFacility.values();
    }

    public TimedItemFeeFacade getTimedItemFeeFacade() {
        return timedItemFeeFacade;
    }

    public void setTimedItemFeeFacade(TimedItemFeeFacade timedItemFeeFacade) {
        this.timedItemFeeFacade = timedItemFeeFacade;
    }

    public List<RoomFacilityCharge> getRoomFacilityCharges() {
        return roomFacilityCharges;
    }

    public void setRoomFacilityCharges(List<RoomFacilityCharge> roomFacilityCharges) {
        this.roomFacilityCharges = roomFacilityCharges;
    }

    public double getAdminstrationCharge() {
        return adminstrationCharge;
    }

    public void setAdminstrationCharge(double adminstrationCharge) {
        this.adminstrationCharge = adminstrationCharge;
    }

    public double getMedicalCareCharge() {
        return medicalCareCharge;
    }

    public void setMedicalCareCharge(double medicalCareCharge) {
        this.medicalCareCharge = medicalCareCharge;
    }

    public double getRoomCharge() {
        return roomCharge;
    }

    public void setRoomCharge(double roomCharge) {
        this.roomCharge = roomCharge;
    }

    public double getMaintananceCharge() {
        return maintananceCharge;
    }

    public void setMaintananceCharge(double maintananceCharge) {
        this.maintananceCharge = maintananceCharge;
    }

    public double getNursingCharge() {
        return nursingCharge;
    }

    public void setNursingCharge(double nursingCharge) {
        this.nursingCharge = nursingCharge;
    }

    public double getMoCharge() {
        return moCharge;
    }

    public void setMoCharge(double moCharge) {
        this.moCharge = moCharge;
    }

    public double getLinenCharge() {
        return linenCharge;
    }

    public void setLinenCharge(double linenCharge) {
        this.linenCharge = linenCharge;
    }

    /**
     *
     */
    @FacesConverter(forClass = RoomFacilityCharge.class)
    public static class RoomFacilityChargeControlleConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            RoomFacilityChargeController controller = (RoomFacilityChargeController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "roomFacilityChargeController");
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
            if (object instanceof RoomFacilityCharge) {
                RoomFacilityCharge o = (RoomFacilityCharge) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + RoomFacilityChargeController.class.getName());
            }
        }
    }

    @FacesConverter("fac")
    public static class RoomFacilityChargeControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            RoomFacilityChargeController controller = (RoomFacilityChargeController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "roomFacilityChargeController");
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
            if (object instanceof RoomFacilityCharge) {
                RoomFacilityCharge o = (RoomFacilityCharge) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + RoomFacilityChargeController.class.getName());
            }
        }
    }
}
