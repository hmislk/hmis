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
import com.divudi.data.BillTypeAtomic;
import static com.divudi.data.BillTypeAtomic.PHARMACY_TRANSFER_REQUEST;
import com.divudi.data.OptionScope;
import com.divudi.data.TriggerType;
import com.divudi.data.TriggerTypeParent;
import com.divudi.entity.AppEmail;
import com.divudi.entity.Bill;
import com.divudi.entity.Notification;
import com.divudi.facade.NotificationFacade;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
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

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class NotificationController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    UserNotificationController userNotificationController;
    @Inject
    SecurityController securityController;
    @Inject
    CommonController commonController;
    @Inject
    ConfigOptionController configOptionController;
    @EJB
    private NotificationFacade ejbFacade;
    private Notification current;
    private List<Notification> items = null;

    public void save(Notification notification) {
        if (notification == null) {
            return;
        }
        if (notification.getId() != null) {
            getFacade().edit(notification);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            notification.setCreatedAt(new Date());
            notification.setCreater(getSessionController().getLoggedUser());
            getFacade().create(notification);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
    }

    public void prepareAdd() {
        current = new Notification();
    }

    public void recreateModel() {
        items = null;
    }

    public void createNotification(Bill bill) {
        if (bill == null) {
            return;
        }
        BillTypeAtomic type = bill.getBillTypeAtomic();
        switch (type) {
            case PHARMACY_TRANSFER_REQUEST:
                createPharmacyTransferRequestNotifications(bill);
                break;
            case PHARMACY_ORDER:
                createPharmacyReuestForInpatients(bill);
                break;
            case OPD_BILL_CANCELLATION:
                createOpdBillCancellationNotification(bill);
                break;
            case OPD_BATCH_BILL_CANCELLATION:
                createOpdBatchBillCancellationNotification(bill);
                break;
            case OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION:
                //No Notification is necessary
                break;
            case PHARMACY_DIRECT_ISSUE:
                createPharmacyDirectIssueNotifications(bill);
                break;
            default:
                throw new AssertionError();
        }
    }

    private void createPharmacyTransferRequestNotifications(Bill bill) {
        Date date = new Date();
        for (TriggerType tt : TriggerType.getTriggersByParent(TriggerTypeParent.TRANSFER_REQUEST)) {
            Notification nn = new Notification();
            nn.setCreatedAt(date);
            nn.setBill(bill);
            nn.setTriggerType(tt);
            nn.setCreater(sessionController.getLoggedUser());
            nn.setMessage(createTemplateForNotificationMessage(bill.getBillTypeAtomic()));
            getFacade().create(nn);
            userNotificationController.createUserNotifications(nn);
        }
    }
    
    private void createPharmacyDirectIssueNotifications(Bill bill) {
        Date date = new Date();
        for (TriggerType tt : TriggerType.getTriggersByParent(TriggerTypeParent.TRANSFER_ISSUE)) {
            Notification nn = new Notification();
            nn.setCreatedAt(date);
            nn.setBill(bill);
            nn.setTriggerType(tt);
            nn.setCreater(sessionController.getLoggedUser());
            nn.setMessage(createTemplateForNotificationMessage(bill.getBillTypeAtomic()));
            getFacade().create(nn);
            userNotificationController.createUserNotifications(nn);
        }
    }

    private String createTemplateForNotificationMessage(BillTypeAtomic bt) {
        String message = null;
        if (bt == null) {
            return null;
        }

        if (bt == BillTypeAtomic.PHARMACY_TRANSFER_REQUEST) {
            message = configOptionController.getLongTextValueByKey("Message Template for Pharmacy Transfer Request Notification", OptionScope.APPLICATION, null, null, null);
        }

        if (bt == BillTypeAtomic.PHARMACY_ORDER) {
            message = configOptionController.getLongTextValueByKey("Message Template for Pharmacy Order Request Notification", OptionScope.APPLICATION, null, null, null);
        }

        if (bt == BillTypeAtomic.OPD_BILL_CANCELLATION) {
            message = configOptionController.getLongTextValueByKey("Message Template for OPD Bill Cancellation Notification", OptionScope.APPLICATION, null, null, null);
        }

        if (bt == BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION) {
            message = configOptionController.getLongTextValueByKey("Message Template for OPD Batch Bill Notification", OptionScope.APPLICATION, null, null, null);
        }

        if (bt == BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION) {
            message = configOptionController.getLongTextValueByKey("Message Template for OPD Bill Cancellation During Batch Bill Cancellation Notification", OptionScope.APPLICATION, null, null, null);
        }

        if (message == null || message == "" || message.isEmpty()) {
            message = "New Request from ";
        }

        return message;
    }

    private void createOpdBillCancellationNotification(Bill bill) {

        String messageBody = "";
        messageBody = "<!DOCTYPE html>"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">"
                + "<head>"
                + "<title>"
                + "Cancellation of Bill Number "
                + bill.getInsId()
                + "</title>"
                + "</head>"
                + "<h:body>";
        messageBody += "<p>";
        messageBody += "Bill No : " + bill.getInsId() + "<br/>";
        messageBody += "Bill Date : " + bill.getBillDate() + "<br/>";
        messageBody += "Bill Value : " + bill.getNetTotal() + "<br/>";
        messageBody += "Billed By : " + bill.getCreater().getWebUserPerson().getNameWithTitle() + "<br/>";
        messageBody += "Cancelled Date : " + new Date() + "<br/>";
        messageBody += "Cancelled By : " + getSessionController().getLoggedUser().getWebUserPerson().getNameWithTitle() + "<br/>";
        messageBody += "</p>";
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, 1);

        String temId = bill.getId() + "";
        temId = securityController.encrypt(temId);
        try {
            temId = URLEncoder.encode(temId, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
        }

        String ed = CommonController.getDateFormat(c.getTime(), sessionController.getApplicationPreference().getLongDateFormat());
        ed = securityController.encrypt(ed);
        try {
            ed = URLEncoder.encode(ed, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
        }
        String url = commonController.getBaseUrl() + "faces/requests/bill.xhtml?id=" + temId + "&user=" + ed;
        messageBody += "<p>"
                + "Your Report is attached"
                + "<br/>"
                + "Please visit "
                + "<a href=\""
                + url
                + "\">this link</a>"
                + " to view or print the bill.The link will expire in one month for privacy and confidentially issues."
                + "<br/>"
                + "</p>";

        messageBody += "</h:body></html>";

        Date date = new Date();
        for (TriggerType tt : TriggerType.getTriggersByParent(TriggerTypeParent.OPD_BILL_CANCELLATION)) {
            Notification nn = new Notification();
            nn.setCreatedAt(date);
            nn.setBill(bill);
            nn.setTriggerType(tt);
            nn.setCreater(sessionController.getLoggedUser());
            switch (tt.getMedium()) {
                case EMAIL:
                    nn.setMessage(messageBody);
                    break;
                case SMS:
                    nn.setMessage("OPD Bill Cancelled.");
                    break;
                case SYSTEM_NOTIFICATION:
                    nn.setMessage("OPD Bill Cancelled.");
                    break;
                default:
                    throw new AssertionError();
            }
            getFacade().create(nn);
            userNotificationController.createUserNotifications(nn);
        }

    }

    private void createOpdBatchBillCancellationNotification(Bill bill) {

        String messageBody = "";
        messageBody = "<!DOCTYPE html>"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">"
                + "<head>"
                + "<title>"
                + "Cancellation of Bill Number "
                + bill.getInsId()
                + "</title>"
                + "</head>"
                + "<h:body>";
        messageBody += "<p>";
        messageBody += "Bill No : " + bill.getInsId() + "<br/>";
        messageBody += "Bill Date : " + bill.getBillDate() + "<br/>";
        messageBody += "Bill Value : " + bill.getNetTotal() + "<br/>";
        messageBody += "Billed By : " + bill.getCreater().getWebUserPerson().getNameWithTitle() + "<br/>";
        messageBody += "Cancelled Date : " + new Date() + "<br/>";
        messageBody += "Cancelled By : " + getSessionController().getLoggedUser().getWebUserPerson().getNameWithTitle() + "<br/>";
        messageBody += "</p>";
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, 1);

        String temId = bill.getId() + "";
        temId = securityController.encrypt(temId);
        try {
            temId = URLEncoder.encode(temId, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
        }

        String ed = CommonController.getDateFormat(c.getTime(), sessionController.getApplicationPreference().getLongDateFormat());
        ed = securityController.encrypt(ed);
        try {
            ed = URLEncoder.encode(ed, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
        }
        String url = commonController.getBaseUrl() + "faces/requests/bill.xhtml?id=" + temId + "&user=" + ed;
        messageBody += "<p>"
                + "Your Report is attached"
                + "<br/>"
                + "Please visit "
                + "<a href=\""
                + url
                + "\">this link</a>"
                + " to view or print the bill.The link will expire in one month for privacy and confidentially issues."
                + "<br/>"
                + "</p>";

        messageBody += "</h:body></html>";

        Date date = new Date();
        for (TriggerType tt : TriggerType.getTriggersByParent(TriggerTypeParent.OPD_BILL_CANCELLATION)) {
            Notification nn = new Notification();
            nn.setCreatedAt(date);
            nn.setBill(bill);
            nn.setTriggerType(tt);
            nn.setCreater(sessionController.getLoggedUser());
            switch (tt.getMedium()) {
                case EMAIL:
                    nn.setMessage(messageBody);
                    break;
                case SMS:
                    nn.setMessage("OPD Batch Bill Cancelled.");
                    break;
                case SYSTEM_NOTIFICATION:
                    nn.setMessage("OPD Batch Bill Cancelled.");
                    break;
                default:
                    throw new AssertionError();
            }
            getFacade().create(nn);
            userNotificationController.createUserNotifications(nn);
        }
    }

    private void createPharmacyReuestForInpatients(Bill bill) {
        Date date = new Date();
        for (TriggerType tt : TriggerType.getTriggersByParent(TriggerTypeParent.INPATIENT_ORDER_REQUEST)) {
            Notification nn = new Notification();
            nn.setCreatedAt(date);
            nn.setBill(bill);
            nn.setTriggerType(tt);
            nn.setCreater(sessionController.getLoggedUser());
            nn.setMessage(createTemplateForNotificationMessage(bill.getBillTypeAtomic()));
            getFacade().create(nn);
            userNotificationController.createUserNotifications(nn);
        }
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

    public NotificationFacade getEjbFacade() {
        return ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public NotificationController() {
    }

    public Notification getCurrent() {
        if (current == null) {
            current = new Notification();
        }
        return current;
    }

    public void setCurrent(Notification current) {
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

    private NotificationFacade getFacade() {
        return ejbFacade;
    }

    public List<Notification> getItems() {
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = Notification.class)
    public static class NotificationConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            NotificationController controller = (NotificationController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "notificationController");
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
            if (object instanceof Notification) {
                Notification o = (Notification) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + Notification.class.getName());
            }
        }
    }

}
