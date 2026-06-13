/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.core.util.JsfUtil;
import com.divudi.bean.pharmacy.PharmacyBillSearch;
import com.divudi.bean.pharmacy.PharmacySaleBhtController;
import com.divudi.bean.pharmacy.PurchaseOrderController;
import com.divudi.bean.pharmacy.TransferIssueController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.OptionScope;
import com.divudi.ejb.SmsManagerEjb;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.UserNotification;
import com.divudi.core.entity.Notification;
import com.divudi.bean.inward.BhtSummeryController;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Sms;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.NotificationFacade;
import com.divudi.core.facade.SmsFacade;
import com.divudi.core.facade.UserNotificationFacade;
import com.divudi.service.NotificationPushService;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
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
    @Inject
    NotificationPushService notificationPushService;
    @Inject
    BhtSummeryController bhtSummeryController;
    private Date date;
    // Notification list filters
    private boolean todayNotification;
    private String seenFilter = "ALL"; // ALL | SEEN | UNSEEN
    private String completionFilter = "ALL"; // ALL | COMPLETED | PENDING
    private boolean canceldRequests;
    private boolean showCleared; // list previously cleared (retired) notifications for restoring

    public String navigateToRecivedNotification() {
        resetFilters();
        fillLoggedUserNotifications();
        return "/Notification/user_notifications?faces-redirect=true";
    }

    public void resetFilters() {
        todayNotification = false;
        canceldRequests = false;
        showCleared = false;
        seenFilter = "ALL";
        completionFilter = "ALL";
    }

    public int getUnseenCount() {
        if (sessionController == null || sessionController.getLoggedUser() == null) {
            return 0;
        }
        try {
            String jpql = "select count(un) "
                    + " from UserNotification un "
                    + " where un.webUser=:wu "
                    + " and un.retired=:ret "
                    + " and un.seen=:seen";
            Map m = new HashMap();
            m.put("ret", false);
            m.put("seen", false);
            m.put("wu", sessionController.getLoggedUser());
            long count = getFacade().findLongByJpql(jpql, m);
            return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
        } catch (Exception e) {
            return 0;
        }
    }

    public String navigateToSentNotification() {
        return "/Notification/sent_notifications";
    }

    /**
     * Retires (clears) every notification currently listed. The user filters
     * first, then clears, so what is removed is exactly what is on screen.
     * Cleared notifications remain in the database and can be viewed and
     * restored through the "Show Cleared" filter.
     */
    public void clearNotificationsByCriteria() {
        if (showCleared) {
            JsfUtil.addErrorMessage("These notifications are already cleared. Use Restore to bring one back.");
            return;
        }
        if (items == null || items.isEmpty()) {
            JsfUtil.addErrorMessage("No notifications listed to clear");
            return;
        }
        int clearedCount = 0;
        for (UserNotification un : items) {
            un.setRetired(true);
            un.setRetiredAt(new Date());
            un.setRetirer(sessionController.getLoggedUser());
            getFacade().edit(un);
            clearedCount++;
        }
        JsfUtil.addSuccessMessage(clearedCount + " notification(s) cleared");
        filterNotificationsByCriteria();
    }

    /**
     * Brings back a previously cleared (retired) notification so it appears
     * in the normal list again.
     */
    public void restoreUserNotification(UserNotification un) {
        if (un == null || un.getId() == null) {
            JsfUtil.addErrorMessage("Nothing to restore !");
            return;
        }
        un.setRetired(false);
        un.setRetiredAt(null);
        un.setRetirer(null);
        un.setRetireComments(null);
        getFacade().edit(un);
        JsfUtil.addSuccessMessage("Notification restored");
        filterNotificationsByCriteria();
    }

    /**
     * Reloads the notification list from the database applying the selected
     * filter criteria. Always queries fresh so changing or removing criteria
     * works as expected.
     */
    public void filterNotificationsByCriteria() {
        if (sessionController == null || sessionController.getLoggedUser() == null) {
            items = null;
            return;
        }
        StringBuilder jpql = new StringBuilder("select un "
                + " from UserNotification un "
                + " where un.webUser=:wu "
                + " and un.retired=:ret ");
        Map<String, Object> m = new HashMap<>();
        m.put("wu", sessionController.getLoggedUser());
        m.put("ret", showCleared);

        if (todayNotification) {
            java.util.Calendar todayCal = java.util.Calendar.getInstance();
            todayCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            todayCal.set(java.util.Calendar.MINUTE, 0);
            todayCal.set(java.util.Calendar.SECOND, 0);
            todayCal.set(java.util.Calendar.MILLISECOND, 0);
            jpql.append(" and un.notification.createdAt >= :fromDate ");
            m.put("fromDate", todayCal.getTime());
        }

        if ("SEEN".equals(seenFilter)) {
            jpql.append(" and un.seen=:seen ");
            m.put("seen", true);
        } else if ("UNSEEN".equals(seenFilter)) {
            jpql.append(" and un.seen=:seen ");
            m.put("seen", false);
        }

        if ("COMPLETED".equals(completionFilter)) {
            jpql.append(" and un.notification.completed=:com ");
            m.put("com", true);
        } else if ("PENDING".equals(completionFilter)) {
            jpql.append(" and un.notification.completed=:com ");
            m.put("com", false);
        }

        if (canceldRequests) {
            jpql.append(" and un.notification.bill is not null "
                    + " and un.notification.bill.cancelled=:can ");
            m.put("can", true);
        }

        jpql.append(" order by un.id desc");
        items = getFacade().findByJpql(jpql.toString(), m, 100);
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
        if (un == null || un.getNotification() == null) {
            JsfUtil.addErrorMessage("Nothing to remove !");
            return;
        }
        Department todept = null;
        Notification n = un.getNotification();
        if (n.getBill() != null) {
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
        } else if (n.getPatientRoom() != null) {
            if (n.getPatientRoom().getRoomFacilityCharge() != null) {
                todept = n.getPatientRoom().getRoomFacilityCharge().getDepartment();
            }
        } else if (n.getPatientEncounter() == null) {
            // Not a bill, room or encounter based notification - nothing we know how to remove
            return;
        }
        // PatientEncounter-based (clinical/final discharge) notifications have no
        // owning department; the notification already belongs to the logged user.
        if (todept != null && !todept.equals(sessionController.getLoggedUser().getDepartment())) {
            JsfUtil.addErrorMessage("You can't Access On Current Department !");
            return;
        }
        un.setRetired(true);
        un.setRetiredAt(new Date());
        un.setRetirer(sessionController.getLoggedUser());
        getFacade().edit(un);
        filterNotificationsByCriteria();
    }

    private UserNotificationFacade getEjbFacade() {
        return ejbFacade;
    }

    public List<UserNotification> fillLoggedUserNotifications() {
        try {
            if (sessionController == null || sessionController.getLoggedUser() == null) {
                items = null;
                return items;
            }
            String jpql = "select un "
                    + " from UserNotification un "
                    + " where un.webUser=:wu "
                    + " and un.retired=:ret "
                    + " order by un.id desc";
            Map m = new HashMap();
            m.put("ret", false);
            m.put("wu", sessionController.getLoggedUser());
            items = getFacade().findByJpql(jpql, m, 20);
            return items;
        } catch (Exception e) {
            // Avoid breaking page rendering when notifications query fails
            items = null;
            return items;
        }
    }

    /**
     * Called by the menu WebSocket push (p:remoteCommand). Refreshes the badge
     * list and surfaces the newest unread notification as a transient, non-modal
     * toast. A growl/toast is used deliberately so it never steals focus or blocks
     * a user who is mid-way through entering a bill.
     */
    public void onPushRefreshNotifications() {
        fillLoggedUserNotifications();
        if (items == null || items.isEmpty()) {
            return;
        }
        UserNotification latest = null;
        for (UserNotification un : items) {
            if (un.getNotification() == null || un.isSeen()) {
                continue;
            }
            if (latest == null
                    || (un.getId() != null && latest.getId() != null && un.getId() > latest.getId())) {
                latest = un;
            }
        }
        if (latest != null && latest.getNotification().getMessage() != null
                && !latest.getNotification().getMessage().isEmpty()) {
            FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "New Notification", latest.getNotification().getMessage());
            FacesContext.getCurrentInstance().addMessage(null, fm);
        }
    }

    public String navigateToCurrentNotificationRequest(UserNotification un) {
        if (un == null || un.getNotification() == null) {
            JsfUtil.addErrorMessage("Invalid notification");
            return "";
        }
        un.setSeen(true);
        un.setRetired(true);
        un.setRetiredAt(new Date());
        un.setRetirer(sessionController.getLoggedUser());
        un.setRetireComments("Viewed");
        getFacade().edit(un);

        // Handle PatientRoom-based (discharge) notifications
        if (un.getNotification().getPatientRoom() != null) {
            PatientRoom pr = un.getNotification().getPatientRoom();
            if (pr.getPatientEncounter() != null) {
                bhtSummeryController.setPatientEncounter(pr.getPatientEncounter());
                return bhtSummeryController.navigateToInpatientProfile();
            }
            return "";
        }

        // Handle PatientEncounter-based notifications
        if (un.getNotification().getPatientEncounter() != null) {
            bhtSummeryController.setPatientEncounter(un.getNotification().getPatientEncounter());
            return bhtSummeryController.navigateToInpatientProfile();
        }

        if (un.getNotification().getBill() == null) {
            return "";
        }

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
        Bill bill = un.getNotification().getBill();
        BillTypeAtomic type = bill.getBillTypeAtomic();
        switch (type) {
            case REQUEST_MEDICINE_INWARD:
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
        if (notification.getPatientRoom() == null && notification.getPatientEncounter() == null) {
            if (notification.getBill() == null) {
                return;
            }
            if (notification.getBill().getBillTypeAtomic() == null) {
                return;
            }
        }
        createUserNotificationsForMedium(notification);
    }

    private void createUserNotificationsForMedium(Notification n) {
        Department todept = null;
        if (n == null) {
            return;
        }
        if (n.getBill() == null && n.getPatientRoom() == null && n.getPatientEncounter() == null) {
            return;
        }

        if (n.getBill() != null) {
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
                default:
                    todept = sessionController.getDepartment();
            }
        } else if (n.getPatientRoom() != null) {
            todept = n.getPatientRoom().getRoomFacilityCharge().getDepartment();
        } else if (n.getPatientEncounter() != null) {
            PatientEncounter pe = n.getPatientEncounter();
            if (pe.getCurrentPatientRoom() != null
                    && pe.getCurrentPatientRoom().getRoomFacilityCharge() != null) {
                todept = pe.getCurrentPatientRoom().getRoomFacilityCharge().getDepartment();
            }
            if (todept == null) {
                todept = pe.getDepartment();
            }
        }

        List<WebUser> notificationUsers = triggerSubscriptionController.fillSubscribedUsersByDepartment(n.getTriggerType(), todept);
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
                    notificationPushService.pushToUser(u.getId());
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
        Boolean sent = smsManager.sendSms(e);
        if (sent) {
            JsfUtil.addSuccessMessage("SMS Sent");
        } else {
            JsfUtil.addSuccessMessage("SMS Failed");
        }

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
            JsfUtil.addErrorMessage("Nothing to Delete");
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
        if (items == null) {
            fillLoggedUserNotifications();
        }
        return items;
    }

    public boolean isTodayNotification() {
        return todayNotification;
    }

    public void setTodayNotification(boolean todayNotification) {
        this.todayNotification = todayNotification;
    }

    public String getSeenFilter() {
        return seenFilter;
    }

    public void setSeenFilter(String seenFilter) {
        this.seenFilter = seenFilter;
    }

    public String getCompletionFilter() {
        return completionFilter;
    }

    public void setCompletionFilter(String completionFilter) {
        this.completionFilter = completionFilter;
    }

    public boolean isShowCleared() {
        return showCleared;
    }

    public void setShowCleared(boolean showCleared) {
        this.showCleared = showCleared;
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
