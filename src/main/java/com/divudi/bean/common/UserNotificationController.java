/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.pharmacy.PharmacyBillSearch;
import com.divudi.bean.pharmacy.PharmacySaleBhtController;
import com.divudi.bean.pharmacy.PurchaseOrderController;
import com.divudi.bean.pharmacy.TransferIssueController;
import com.divudi.data.BillTypeAtomic;
import static com.divudi.data.BillTypeAtomic.PHARMACY_ORDER;
import static com.divudi.data.BillTypeAtomic.PHARMACY_TRANSFER_REQUEST;
import com.divudi.data.MessageType;
import com.divudi.data.OptionScope;
import com.divudi.data.SmsSentResponse;
import com.divudi.data.TriggerType;
import com.divudi.ejb.SmsManagerEjb;
import com.divudi.entity.Bill;
import com.divudi.entity.Department;
import com.divudi.entity.UserNotification;
import com.divudi.entity.Institution;
import com.divudi.entity.Notification;
import com.divudi.entity.Sms;
import com.divudi.entity.TriggerSubscription;
import com.divudi.entity.WebUser;
import com.divudi.facade.NotificationFacade;
import com.divudi.facade.SmsFacade;
import com.divudi.facade.UserNotificationFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class UserNotificationController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    TriggerSubscriptionController triggerSubscriptionController;
    @Inject
    SmsController smsController;
    @Inject
    ConfigOptionController configOptionController;
    @Inject
    TransferIssueController transferIssueController;
    @Inject
    PurchaseOrderController purchaseOrderController;
    @Inject
    PharmacyBillSearch pharmacyBillSearch;
    @EJB
    private UserNotificationFacade ejbFacade;
    @EJB
    NotificationFacade notificationFacade;
    @EJB
    SmsFacade smsFacade;
    private UserNotification current;
    private List<UserNotification> items = null;

    @Inject
    PharmacySaleBhtController pharmacySaleBhtController;
    @Inject
    SmsManagerEjb smsManager;
    private Date date;
    private boolean todayNotification;
    private boolean seenedNotifiaction;
    private boolean completedNotification;
    private boolean notCompeletedNotifiaction;
    private boolean canceldRequests;

    public String navigateToRecivedNotification() {
        return "/Notification/user_notifications";
    }

    public String navigateToSentNotification() {
        return "/Notification/sent_notifications";
    }

    public void clearNotificationsByCriteria() {
        if (seenedNotifiaction) {
            if (items == null) {
                return;
            }
            for (UserNotification un : items) {
                if (un.isSeen()) {
                    un.setRetired(true);
                    un.setRetiredAt(new Date());
                    getFacade().edit(un);
                }
            }
            fillLoggedUserNotifications();
        }

        if (completedNotification) {
            if (items == null) {
                return;
            }
            for (UserNotification un : items) {
                if (un.getNotification().isCompleted()) {
                    un.setRetired(true);
                    un.setRetiredAt(new Date());
                    getFacade().edit(un);
                }
            }
            fillLoggedUserNotifications();
        }

        if (canceldRequests) {
            if (items == null) {
                return;
            }
            for (UserNotification un : items) {
                if (un.getNotification().getBill().isCancelled()) {
                    un.setRetired(true);
                    un.setRetiredAt(new Date());
                    getFacade().edit(un);
                }
            }
            fillLoggedUserNotifications();
        }

        if (notCompeletedNotifiaction) {
            if (items == null) {
                return;
            }
            for (UserNotification un : items) {
                if (!un.getNotification().isCompleted()) {
                    un.setRetired(true);
                    un.setRetiredAt(new Date());
                    getFacade().edit(un);
                }
            }
            fillLoggedUserNotifications();

        }

    }

    public void filterNotificationsByCriteria() {
        if (seenedNotifiaction) {
            if (items == null) {
                return;
            }
            Iterator<UserNotification> iterator = items.iterator();
            while (iterator.hasNext()) {
                UserNotification notification = iterator.next();
                if (notification.getNotification() == null) {
                    continue;
                }

                if (!notification.isSeen()) {
                    iterator.remove();
                }
            }
        }

        if (completedNotification) {
            if (items == null) {
                return;
            }
            System.out.println("iterator 1 seenedNotifiaction = " + items.size());
            Iterator<UserNotification> iterator = items.iterator();
            while (iterator.hasNext()) {
                UserNotification notification = iterator.next();
                if (notification.getNotification() == null) {
                    continue;
                }

                if (!notification.getNotification().isCompleted()) {
                    iterator.remove();
                }
            }
            System.out.println("iterator 2 seenedNotifiaction = " + items.size());
        }

        if (todayNotification) {
            if (items == null) {
                return;
            }
            Iterator<UserNotification> iterator = items.iterator();
            while (iterator.hasNext()) {
                UserNotification notification = iterator.next();
                if (notification.getNotification() == null) {
                    continue;
                }

                if (!notification.getNotification().getCreatedAt().equals(getDate())) {
                    iterator.remove();
                }
            }
        }

        if (notCompeletedNotifiaction) {
            if (items == null) {
                return;
            }
            Iterator<UserNotification> iterator = items.iterator();
            while (iterator.hasNext()) {
                UserNotification notification = iterator.next();
                if (notification.getNotification() == null) {
                    continue;
                }

                if (notification.getNotification().isCompleted()) {
                    iterator.remove();
                }
            }
        }

        if (canceldRequests) {
            if (items == null) {
                return;
            }
            Iterator<UserNotification> iterator = items.iterator();
            while (iterator.hasNext()) {
                UserNotification notification = iterator.next();
                if (notification.getNotification() == null) {
                    continue;
                }

                if (!notification.getNotification().getBill().isCancelled()) {
                    iterator.remove();
                }
            }
        }
    }

    public void save(UserNotification userNotification) {
        if (userNotification == null) {
            return;
        }
        if (userNotification.getId() != null) {
            getFacade().edit(userNotification);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            userNotification.setCreatedAt(new Date());
            userNotification.setCreater(getSessionController().getLoggedUser());
            getFacade().create(userNotification);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
    }

    public void prepareAdd() {
        current = new UserNotification();
    }

    public void recreateModel() {
        items = null;
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
        recreateModel();
        getItems();
    }

    public void userNotificationRequestComplete() {
        if (current == null) {
            JsfUtil.addErrorMessage("User Notification Error !");
            return;
        }
        current.getNotification().setCompleted(true);
        notificationFacade.edit(current.getNotification());
        current.setSeen(true);
        getFacade().edit(current);
        fillLoggedUserNotifications();
    }

    public void removeUserNotification(UserNotification un) {
        Department todept = null;
        Notification n = un.getNotification();
        switch (n.getBill().getBillType()) {
            case PharmacyOrder:
                todept = n.getBill().getFromDepartment();
                break;
            case PharmacyTransferIssue:
                todept = n.getBill().getToDepartment();
                break;
            case PharmacyTransferRequest:
                todept = n.getBill().getToDepartment();
                break;
            case InwardPharmacyRequest:
                todept = n.getBill().getToDepartment();
                break;
            case PharmacyOrderApprove:
                todept = n.getBill().getFromDepartment();
                break;
        }

        if (!todept.equals(sessionController.getLoggedUser().getDepartment())) {
            JsfUtil.addErrorMessage("You can't Access On Current Department !");
            return;
        }
        if (un.getNotification().getBill() == null) {
            return;
        }
        un.setRetired(true);
        getFacade().edit(un);
        fillLoggedUserNotifications();
    }

    private UserNotificationFacade getEjbFacade() {
        return ejbFacade;
    }

    public List<UserNotification> fillLoggedUserNotifications() {
        String jpql = "select un "
                + " from UserNotification un "
                + " where un.webUser=:wu "
                + " and un.retired=:ret";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("wu", sessionController.getLoggedUser());
        items = getFacade().findByJpql(jpql, m);
        return items;
    }

    public String navigateToCurrentNotificationRequest(UserNotification un) {
        un.setSeen(true);
        getFacade().edit(un);

        Department todept = null;
        Notification n = un.getNotification();
        switch (n.getBill().getBillType()) {
            case PharmacyOrder:
                todept = n.getBill().getFromDepartment();
                break;
            case PharmacyTransferIssue:
                todept = n.getBill().getToDepartment();
                break;
            case PharmacyTransferRequest:
                todept = n.getBill().getToDepartment();
                break;
            case InwardPharmacyRequest:
                todept = n.getBill().getToDepartment();
                break;
            case PharmacyOrderApprove:
                todept = n.getBill().getFromDepartment();
                break;
        }

        if (!todept.equals(sessionController.getLoggedUser().getDepartment())) {
            JsfUtil.addErrorMessage("You can't Access On Current Department !");
            return "";
        }
        if (un.getNotification().getBill() == null) {
            return "";
        }
        Bill bill = un.getNotification().getBill();
        BillTypeAtomic type = bill.getBillTypeAtomic();
        switch (type) {
            case INWARD_PHARMACY_REQUEST:
                pharmacySaleBhtController.setBhtRequestBill(bill);
                return pharmacySaleBhtController.navigateToIssueMedicinesDirectlyForBhtRequest();

            case PHARMACY_TRANSFER_REQUEST:
                transferIssueController.setRequestedBill(bill);
                return transferIssueController.navigateToPharmacyIssueForRequests();

            case PHARMACY_ORDER:
                purchaseOrderController.setRequestedBill(bill);
                return purchaseOrderController.navigateToPurchaseOrderApproval();

            case PHARMACY_ORDER_APPROVAL:
                pharmacyBillSearch.setBill(bill);
                return "/pharmacy/pharmacy_reprint_po";
            case PHARMACY_DIRECT_ISSUE:
                pharmacyBillSearch.setBill(bill);
                return "/pharmacy/pharmacy_reprint_po";

            default:
                return "";
        }

    }

    public void createUserNotifications(Notification notification) {
        if (notification == null) {
            return;
        }
        if (notification.getBill() == null) {
            return;
        }
        if (notification.getBill().getBillTypeAtomic() == null) {
            return;
        }
        createUserNotificationsForMedium(notification);
    }

    private void createUserNotificationsForMedium(Notification n) {
        Department todept = null;
        if (n == null) {
            return;
        }

        switch (n.getBill().getBillType()) {
            case PharmacyOrder:
                todept = n.getBill().getFromDepartment();
                break;
            case PharmacyTransferIssue:
                todept = n.getBill().getToDepartment();
                System.out.println("todept = " + todept);
                break;
            case PharmacyTransferRequest:
                todept = n.getBill().getToDepartment();
                break;
            case InwardPharmacyRequest:
                todept = n.getBill().getToDepartment();
                break;
            case PharmacyOrderApprove:
                todept = n.getBill().getFromDepartment();
                break;
            default:
                todept = sessionController.getDepartment();
        }

        if (n.getBill() == null) {
            return;
        }
        List<WebUser> notificationUsers = triggerSubscriptionController.fillSubscribedUsersByDepartment(n.getTriggerType(), todept);
        System.out.println("notificationUsers = " + notificationUsers.size());
        switch (n.getTriggerType().getMedium()) {
            case EMAIL:
                for (WebUser u : notificationUsers) {
                    String number = u.getWebUserPerson().getMobile();
                    //TODo
                }
                break;
            case SMS:
                for (WebUser u : notificationUsers) {
                    String number = u.getWebUserPerson().getMobile();
                    sendSmsForUserSubscriptions(number);
                }
                break;
            case SYSTEM_NOTIFICATION:
                for (WebUser u : notificationUsers) {
                    UserNotification nun = new UserNotification();
                    nun.setNotification(n);
                    nun.setWebUser(u);
                    getFacade().create(nun);
                }
                break;
        }

    }

    public void sendSmsForUserSubscriptions(String userMobNumber) {
        Sms e = new Sms();
        e.setCreatedAt(new Date());
        e.setCreater(sessionController.getLoggedUser());
        e.setReceipientNumber(userMobNumber);
        e.setSendingMessage(createSmsForUserNotification());
        e.setDepartment(getSessionController().getLoggedUser().getDepartment());
        e.setInstitution(getSessionController().getLoggedUser().getInstitution());
        e.setPending(false);
        //e.setSmsType(MessageType.ChannelDoctorArrival);
        smsFacade.create(e);
        SmsSentResponse sent = smsManager.sendSmsByApplicationPreference(e.getReceipientNumber(), e.getSendingMessage(), sessionController.getApplicationPreference());
        e.setSentSuccessfully(sent.isSentSuccefully());
        e.setReceivedMessage(sent.getReceivedMessage());
        smsFacade.edit(e);
    }

    public String createSmsForUserNotification() {
        String template = configOptionController.getLongTextValueByKey("SMS Template for User Notification", OptionScope.APPLICATION, null, null, null);
        if (template == null || template.isEmpty()) {
            template = "{patient_name} {appointment_time}";
        }
        //TODO: Replace placeholders with actual values
        template = template.replace("{patient_name}", "")
                .replace("{doctor}", "")
                .replace("{appointment_time}", "")
                .replace("{appointment_date}", "")
                .replace("{serial_no}", "")
                .replace("{doc}", "")
                .replace("{time}", "")
                .replace("{date}", "")
                .replace("{No}", "");
        return "";
    }

    public void createAllertMessage(Notification n) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage("Successful", "Your message: " + n.getMessage()));
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public UserNotificationController() {
    }

    public UserNotification getCurrent() {
        if (current == null) {
            current = new UserNotification();
        }
        return current;
    }

    public void setCurrent(UserNotification current) {
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

    private UserNotificationFacade getFacade() {
        return ejbFacade;
    }

    public List<UserNotification> getItems() {
        return items;
    }

    public boolean isTodayNotification() {
        return todayNotification;
    }

    public void setTodayNotification(boolean todayNotification) {
        this.todayNotification = todayNotification;
    }

    public boolean isSeenedNotifiaction() {
        return seenedNotifiaction;
    }

    public void setSeenedNotifiaction(boolean seenedNotifiaction) {
        this.seenedNotifiaction = seenedNotifiaction;
    }

    public boolean isCompletedNotification() {
        return completedNotification;
    }

    public void setCompletedNotification(boolean completedNotification) {
        this.completedNotification = completedNotification;
    }

    public boolean isNotCompeletedNotifiaction() {
        return notCompeletedNotifiaction;
    }

    public void setNotCompeletedNotifiaction(boolean notCompeletedNotifiaction) {
        this.notCompeletedNotifiaction = notCompeletedNotifiaction;
    }

    public Date getDate() {
        date = new Date();
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public boolean isCanceldRequests() {
        return canceldRequests;
    }

    public void setCanceldRequests(boolean canceldRequests) {
        this.canceldRequests = canceldRequests;
    }

    /**
     *
     */
    @FacesConverter(forClass = UserNotification.class)
    public static class UserNotificationConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            UserNotificationController controller = (UserNotificationController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "userNotificationController");
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
            if (object instanceof UserNotification) {
                UserNotification o = (UserNotification) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + UserNotification.class.getName());
            }
        }
    }

}
