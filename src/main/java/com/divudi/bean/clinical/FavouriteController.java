package com.divudi.bean.clinical;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.pharmacy.MeasurementUnitController;
import com.divudi.bean.pharmacy.VmpController;
import com.divudi.data.clinical.PrescriptionTemplateType;
import com.divudi.entity.Item;
import com.divudi.entity.clinical.PrescriptionTemplate;
import com.divudi.entity.pharmacy.MeasurementUnit;
import com.divudi.facade.PrescriptionTemplateFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author buddhika
 */
@Named
@SessionScoped
public class FavouriteController implements Serializable {

    /**
     * EJBs
     */
    @EJB
    PrescriptionTemplateFacade favouriteItemFacade;
    /**
     * Controllers
     */
    @Inject
    SessionController sessionController;
    @Inject
    MeasurementUnitController measurementUnitController;
    @Inject
    VmpController vmpController;
    /**
     * Properties
     */
    Item item;
    PrescriptionTemplate current;
    List<PrescriptionTemplate> items;
    private List<MeasurementUnit> availableDoseUnits;
    private List<Item> availableItems;
    private boolean itemadd = false;

    /**
     * Methods
     */
    public void fillFavouriteMedicines() {
        fillFavouriteItems(item, PrescriptionTemplateType.FavouriteMedicine);
    }

    public String navigateToFavoriteMedicineByAge(){
       return "/clinical/clinical_favourite_item_by_age?faces-redirect=true"; 
    }
    
    public String navigateToFavoriteMedicineByWeight(){
        return "/clinical/clinical_favourite_item_by_weight?faces-redirect=true";
    }
    
    public String navigateToEmrIndex(){
        return "/emr/admin/index?faces-redirect=true";
    }
    
    public void fillFavouriteDisgnosis() {
        fillFavouriteItems(item, PrescriptionTemplateType.FavouriteDiagnosis);
    }

    public String toAddFavDig() {
        item = null;
        items = null;
        current = null;
        return "/clinical/clinical_favourite_diagnosis";
    }

    public String toAddFavItem() {
        item = null;
        items = null;
        current = null;
        return "/clinical/clinical_favourite_item";
    }

    public void fillFavouriteItems(Item forItem, PrescriptionTemplateType type) {
        items = listFavouriteItems(forItem, type);
    }

    public List<PrescriptionTemplate> listFavouriteItems(Item forItem, PrescriptionTemplateType type) {
        return listFavouriteItems(forItem, type, null);
    }

    public List<PrescriptionTemplate> listFavouriteItems(Item forItem, PrescriptionTemplateType type, Double weight) {
        return listFavouriteItems(forItem, type, weight, null);
    }
    
    public List<PrescriptionTemplate> listFavouriteItems( PrescriptionTemplateType type, Double weight, Long ageInDays) {
        return listFavouriteItems(null, type, weight, ageInDays);
    }
    
    public List<PrescriptionTemplate> listFavouriteItems(Item forItem, PrescriptionTemplateType type, Double weight, Long ageInDays) {
        String j;
        Map m = new HashMap();
        j = "select i "
                + " from PrescriptionTemplate i "
                + " where i.retired=false "
                + " and i.forWebUser=:wu ";

        if (type != null) {
            m.put("t", type);
            j += " and i.type=:t ";
        }
        
        if(forItem!=null){
            m.put("fi", forItem);
            j += " and i.forItem=:fi ";
        }
        if (weight != null) {
            j += " and ( i.fromKg < :wt and i.toKg > :wt ) ";
            m.put("wt", weight);
        }
        if (ageInDays != null) {
            j += " and ( i.fromDays < :ad and i.toDays > :ad ) ";
            m.put("ad", (double)ageInDays);
        }
        j += " order by i.orderNo";

        m.put("wu", sessionController.getLoggedUser());
        List<PrescriptionTemplate> its = favouriteItemFacade.findByJpql(j, m);
        if(its==null){
            its = new ArrayList<>();
        }
        return its;
    }

    public void prepareAddingFavouriteItem() {
        if (item == null) {
            JsfUtil.addErrorMessage("No Item Selected");
            return;
        }
        itemadd = true;
        current = new PrescriptionTemplate();
        current.setForItem(item);
        current.setItem(item);
        current.setType(PrescriptionTemplateType.FavouriteMedicine);

        availableDoseUnits = new ArrayList<>();
        availableItems = new ArrayList<>();
        switch (item.getMedicineType()) {
            case Vmp:
                availableDoseUnits.add(item.getMeasurementUnit());
                availableDoseUnits.add(item.getIssueUnit());
                availableDoseUnits.add(item.getPackUnit());
                availableItems.add(item);
                availableItems.addAll(vmpController.ampsOfVmp(item));
                break;
            case Amp:
                availableDoseUnits.add(item.getVmp().getBaseUnit());
                availableDoseUnits.add(item.getVmp().getIssueUnit());
                availableItems.add(item);
                break;
            case Vtm:
                availableDoseUnits = measurementUnitController.getDoseUnits();
                availableItems.addAll(vmpController.ampsAndVmpsContainingVtm(item));
                availableItems.add(item);
                break;
            case Atm:
                availableDoseUnits = measurementUnitController.getDoseUnits();
                availableItems.addAll(vmpController.ampsAndVmpsContainingVtm(item));
                availableItems.add(item);
                break;
            case Ampp:
                break;
            case Vmpp:
                break;
            case Medicine:
                JsfUtil.addErrorMessage("Selected needs a subtype");
                break;
            case AnalyzerTest:
            case Appointment:
            case Investigation:
            case Other:
            case SampleComponent:
            case Service:
                JsfUtil.addErrorMessage("Selected is NOT a medicine");
                break;
            default:
        }
    }

    public void prepareAddingFavouriteDiagnosis() {
        if (item == null) {
            JsfUtil.addErrorMessage("No Diagnosis Selected");
            return;
        }
        current = new PrescriptionTemplate();
        current.setForItem(item);
        current.setType(PrescriptionTemplateType.FavouriteDiagnosis);
    }

    public String toViewFavouriteMedicines() {
        listMyFavouriteMedicines();
        return "/clinical/clinical_favourite_item";
    }

    public void listMyFavouriteMedicines() {

    }
    
    public void saveFavMedicine(){
        current.setType(PrescriptionTemplateType.FavouriteMedicine);
        current.setForItem(item);
        current.setForWebUser(sessionController.getLoggedUser());
        current.setOrderNo(getItems().size() + 1.0);
        favouriteItemFacade.create(current);
        fillFavouriteItems(item, PrescriptionTemplateType.FavouriteMedicine);
        current = null;
        JsfUtil.addSuccessMessage("Saved");
    }

//    public void removeFavourite() {
//        if (current == null) {
//            JsfUtil.addErrorMessage("Nothing current");
//            return;
//        }
//        current.setRetired(true);
//        favouriteItemFacade.edit(current);
//        PrescriptionTemplateType tt = current.getType();
//        current = null;
//        fillFavouriteItems(item, tt);
//        JsfUtil.addSuccessMessage("Removed");
//    }

//    public void addToFavouriteMedicine() {
//        if (item == null) {
//            JsfUtil.addErrorMessage("Please select an item");
//            return;
//        }
//        if (current == null) {
//            JsfUtil.addErrorMessage("Favourite Item is not create by getter. Please contact vendor.");
//            return;
//        }
//        if (current.getType() == null) {
//            JsfUtil.addErrorMessage("Favourite Type NOT current.");
//            return;
//        }
//        if (current.getTemplateFrom() == null) {
//            JsfUtil.addErrorMessage("From NOT current.");
//            return;
//        }
//        if (current.getTemplateTo() == null) {
//            JsfUtil.addErrorMessage("To NOT current.");
//            return;
//        }
//        if (current.getTemplateFrom().equals(current.getTemplateTo())) {
//            JsfUtil.addErrorMessage("From is equal not To. So not added.");
//            return;
//        }
//        switch (current.getFavouriteType()) {
//            case kg:
//                current.setFromDays(null);
//                current.setToDays(null);
//                current.setFromKg(current.getTemplateFrom());
//                current.setToKg(current.getTemplateTo());
//                break;
//            case days:
//                current.setFromDays(current.getTemplateFrom());
//                current.setToDays(current.getTemplateTo());
//                current.setFromKg(null);
//                current.setToKg(null);
//                break;
//            case months:
//                current.setFromDays(current.getTemplateFrom() * 30.4167);
//                current.setToDays(current.getTemplateTo() * 30.4167);
//                current.setFromKg(null);
//                current.setToKg(null);
//                break;
//            case years:
//                current.setFromDays(current.getTemplateFrom() * 365);
//                current.setToDays(current.getTemplateTo() * 365);
//                current.setFromKg(null);
//                current.setToKg(null);
//                break;
//            default:
//                JsfUtil.addErrorMessage("Favourite Type NOT current.");
//                return;
//        }
//
//        current.setType(PrescriptionTemplateType.FavouriteMedicine);
//        current.setForItem(item);
//        current.setForWebUser(sessionController.getLoggedUser());
//        current.setOrderNo(getItems().size() + 1.0);
//        favouriteItemFacade.create(current);
//        current = null;
//        fillFavouriteItems(item, PrescriptionTemplateType.FavouriteMedicine);
//        JsfUtil.addSuccessMessage("Saved");
//
//    }

//    public void addToFavouriteDiagnosis() {
//        if (item == null) {
//            JsfUtil.addErrorMessage("Please select a Diagnosis");
//            return;
//        }
//        if (current == null) {
//            JsfUtil.addErrorMessage("Favourite Item is not create by getter. Please contact vendor.");
//            return;
//        }
//        if (current.getFavouriteType() == null) {
//            JsfUtil.addErrorMessage("Favourite Type NOT current.");
//            return;
//        }
//        if (current.getTemplateFrom() == null) {
//            JsfUtil.addErrorMessage("From NOT current.");
//            return;
//        }
//        if (current.getTemplateTo() == null) {
//            JsfUtil.addErrorMessage("To NOT current.");
//            return;
//        }
//        if (current.getTemplateFrom().equals(current.getTemplateTo())) {
//            JsfUtil.addErrorMessage("From is equal not To. So not added.");
//            return;
//        }
//        switch (current.getFavouriteType()) {
//            case kg:
//                current.setFromDays(null);
//                current.setToDays(null);
//                current.setFromKg(current.getTemplateFrom());
//                current.setToKg(current.getTemplateTo());
//                break;
//            case days:
//                current.setFromDays(current.getTemplateFrom());
//                current.setToDays(current.getTemplateTo());
//                current.setFromKg(null);
//                current.setToKg(null);
//                break;
//            case months:
//                current.setFromDays(current.getTemplateFrom() * 30.4167);
//                current.setToDays(current.getTemplateTo() * 30.4167);
//                current.setFromKg(null);
//                current.setToKg(null);
//                break;
//            case years:
//                current.setFromDays(current.getTemplateFrom() * 365);
//                current.setToDays(current.getTemplateTo() * 365);
//                current.setFromKg(null);
//                current.setToKg(null);
//                break;
//            default:
//                JsfUtil.addErrorMessage("Favourite Type NOT current.");
//                return;
//        }
//
//        current.setType(PrescriptionTemplateType.FavouriteDiagnosis);
//        current.setForItem(item);
//        current.setForWebUser(sessionController.getLoggedUser());
//        current.setOrderNo(getItems().size() + 1.0);
//        favouriteItemFacade.create(current);
//        current = null;
//        fillFavouriteItems(item, PrescriptionTemplateType.FavouriteDiagnosis);
//        JsfUtil.addSuccessMessage("Saved");
//
//    }

    public void updateSelected() {
        updateSelected(current);
    }

    public void updateSelected(PrescriptionTemplate updatingItem) {
        if (updatingItem != null) {
            favouriteItemFacade.edit(updatingItem);
            JsfUtil.addSuccessMessage("Updated");
        }
    }

    /**
     * Creates a new instance of FavouriteController
     */
    public FavouriteController() {
    }

    /**
     * Getters And Setters
     */
    
    
    
    /**
     * @return 
     */
    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public List<PrescriptionTemplate> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public void setItems(List<PrescriptionTemplate> items) {
        this.items = items;
    }

    public PrescriptionTemplate getCurrent() {
        if (current == null) {
            current = new PrescriptionTemplate();
            current.setItem(item);
        }
        return current;
    }

    public void setCurrent(PrescriptionTemplate current) {
        this.current = current;
    }

    public List<MeasurementUnit> getAvailableDoseUnits() {
        return availableDoseUnits;
    }

    public void setAvailableDoseUnits(List<MeasurementUnit> availableDoseUnits) {
        this.availableDoseUnits = availableDoseUnits;
    }

    public List<Item> getAvailableItems() {
        return availableItems;
    }

    public void setAvailableItems(List<Item> availableItems) {
        this.availableItems = availableItems;
    }

    public boolean isItemadd() {
        return itemadd;
    }

    public void setItemadd(boolean itemadd) {
        this.itemadd = itemadd;
    }
    
    

}
