/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.memberShip;

import com.divudi.bean.common.EnumController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.inward.RoomCategoryController;
import com.divudi.data.PaymentMethod;
import com.divudi.data.inward.InwardChargeType;

import com.divudi.entity.Institution;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.entity.inward.RoomCategory;
import com.divudi.entity.memberShip.MembershipScheme;
import com.divudi.facade.PriceMatrixFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.inject.Inject;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class InwardMemberShipDiscount implements Serializable {

    private MembershipScheme currentMembershipScheme;
    private PaymentMethod currentPaymentMethod;
    private Institution institution;
    private List<PriceMatrix> items;
    @Inject
    private EnumController enumController;
    @Inject
    private SessionController sessionController;
    @EJB
    private PriceMatrixFacade priceMatrixFacade;
    @Inject
    PriceMatrixController priceMatrixController;

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    
   
    public void edit(PriceMatrix inwardPriceAdjustment) {
        getPriceMatrixFacade().edit(inwardPriceAdjustment);
    }

    public void makeNull() {
        currentMembershipScheme = null;
        currentPaymentMethod = null;
        items = null;
    }

    AdmissionType admissionType;

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    @Inject
    RoomCategoryController roomCategoryController;

    public RoomCategoryController getRoomCategoryController() {
        return roomCategoryController;
    }

    public void setRoomCategoryController(RoomCategoryController roomCategoryController) {
        this.roomCategoryController = roomCategoryController;
    }

    public void createItems() {
        if (getCurrentPaymentMethod() == null || admissionType == null) {
            return;
        }
        items = new ArrayList<>();
        for (InwardChargeType ict : getEnumController().getInwardChargeTypes()) {
            if (ict == InwardChargeType.RoomCharges
                    || ict == InwardChargeType.AdministrationCharge
                    || ict == InwardChargeType.LinenCharges
                    || ict == InwardChargeType.MOCharges
                    || ict == InwardChargeType.MaintainCharges
                    || ict == InwardChargeType.MedicalCareICU
                    || ict == InwardChargeType.NursingCharges) {
                for (RoomCategory rm : getRoomCategoryController().getItems()) {
                    items.add(getPriceMatrixController().getInwardMemberShipDiscount(getCurrentMembershipScheme(), getInstitution(), getCurrentPaymentMethod(), ict, admissionType, rm, getSessionController().getLoggedUser()));
                }
            }
           
            items.add(getPriceMatrixController().getInwardMemberShipDiscount(getCurrentMembershipScheme(), getInstitution(), getCurrentPaymentMethod(), ict, admissionType, getSessionController().getLoggedUser()));

        }

    }

    /**
     * Creates a new instance of InwardMemberShipDiscount
     */
    public InwardMemberShipDiscount() {
    }

    public MembershipScheme getCurrentMembershipScheme() {
        return currentMembershipScheme;
    }

    public void setCurrentMembershipScheme(MembershipScheme currentMembershipScheme) {
        this.currentMembershipScheme = currentMembershipScheme;
    }

    public PaymentMethod getCurrentPaymentMethod() {
        return currentPaymentMethod;
    }

    public void setCurrentPaymentMethod(PaymentMethod currentPaymentMethod) {
        this.currentPaymentMethod = currentPaymentMethod;
    }

    public List<PriceMatrix> getItems() {
        return items;
    }

    public void setItems(List<PriceMatrix> items) {
        this.items = items;
    }

    public EnumController getEnumController() {
        return enumController;
    }

    public void setEnumController(EnumController enumController) {
        this.enumController = enumController;
    }

    public PriceMatrixFacade getPriceMatrixFacade() {
        return priceMatrixFacade;
    }

    public void setPriceMatrixFacade(PriceMatrixFacade priceMatrixFacade) {
        this.priceMatrixFacade = priceMatrixFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

}
