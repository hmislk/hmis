package com.divudi.bean.common;

import static com.divudi.core.data.BillTypeAtomic.OPD_BILL_WITH_PAYMENT;
import com.divudi.core.data.RequestStatus;
import com.divudi.core.data.RequestType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Request;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.RequestFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.service.RequestService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
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
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Named
@SessionScoped
public class RequestController implements Serializable {

    public RequestController() {
    }

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private RequestFacade requestFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    BillNumberGenerator billNumberGenerator;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    SessionController sessionController;
    @Inject
    BillController billController;
    @Inject
    RequestService requestService;
    @Inject
    WebUserController webUserController;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Variables">
    private static final long serialVersionUID = 1L;

    private Patient patient;
    private Bill batchBill;
    private List<Bill> bills;
    private String comment;
    private boolean printPreview;

    private Date fromDate;
    private Date toDate;
    private List<Request> requests;
    private Request currentRequest;

    private String billNo;
    private String bhtNo;
    private String requestNo;
    private RequestType requestType;
    private RequestStatus status;

    private PatientEncounter patientEncounter;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Navigation Method">
    public String navigateToSearchRequest() {
        requests = new ArrayList<>();
        return "/common/request/view_request?faces-redirect=true";
    }

    public String navigateToBackSearchBillList() {

        System.out.println("Request = " + currentRequest);
        System.out.println("Bill = " + currentRequest.getBill());
        System.out.println("Bill Type Atomic = " + currentRequest.getBill().getBillTypeAtomic());

        switch (currentRequest.getBill().getBillTypeAtomic()) {
            case OPD_BATCH_BILL_WITH_PAYMENT:
                return "/opd/opd_batch_bill_print?faces-redirect=true";
            case INWARD_SERVICE_BILL:
                return "/lab/inward_search_service?faces-redirect=true";
            default:
                return "";
        }

    }

    public String navigateToBackSearchRequest() {
        return "/common/request/view_request?faces-redirect=true";
    }

    public String navigateToCreateRequest(Bill bill) {
        if (bill == null || bill.getId() == null) {
            JsfUtil.addErrorMessage("Bill not found for request Cancel");
            return "";
        }

        if (bill.getDepartment() == null || bill.getDepartment().getId() == null || sessionController.getDepartment() == null || sessionController.getDepartment().getId() == null) {
            JsfUtil.addErrorMessage("Department information missing.");
            return "";
        }

        if (!bill.getDepartment().getId().equals(sessionController.getDepartment().getId())) {
            JsfUtil.addErrorMessage("You must log in to " + bill.getDepartment().getName() + " to cancel this bill.");
            return "";
        }

        String navigation = "";
        Bill originalBill = billFacade.find(bill.getId());

        Request req = requestService.findRequest(originalBill);

        if (req != null) {
            JsfUtil.addErrorMessage("There is already a " + req.getRequestType().getDisplayName() + " requesr for this bill.");
            return "";
        } else {
            printPreview = false;
            bills = new ArrayList<>();

            switch (originalBill.getBillTypeAtomic()) {
                case OPD_BATCH_BILL_WITH_PAYMENT:
                    setBatchBill(originalBill);
                    bills = billController.billsOfBatchBill(batchBill);
                    patient = batchBill.getPatient();
                    comment = null;

                    navigation = "/common/request/opd_bill_cancel_request?faces-redirect=true";
                    break;
                case OPD_BILL_WITH_PAYMENT:
                    navigation = "";
                    break;
                case INWARD_SERVICE_BILL:
                    setBatchBill(originalBill);
                    bills.add(originalBill);
                    patient = originalBill.getPatient();
                    patientEncounter = originalBill.getPatientEncounter();
                    comment = null;
                    navigation = "/common/request/inward_bill_cancel_request?faces-redirect=true";
                    break;
                default:
                    navigation = "";
            }
        }
        return navigation;
    }

    public String navigateToApproveRequest() {
        if (currentRequest == null) {
            JsfUtil.addErrorMessage("Not found for a request for Approvel");
            return "";
        }
        if (currentRequest.getBill() == null) {
            JsfUtil.addErrorMessage("Bill not found for request Cancel");
            return "";
        }

        //Update Review Status
        if (currentRequest.getStatus() == RequestStatus.PENDING) {
            currentRequest.setReviewedBy(sessionController.getLoggedUser());
            currentRequest.setReviewedAt(new Date());
            currentRequest.setStatus(RequestStatus.UNDER_REVIEW);
            requestService.save(currentRequest, sessionController.getLoggedUser());
        }

        bills = new ArrayList<>();
        String navigation = "";

        switch (currentRequest.getBill().getBillTypeAtomic()) {
            case OPD_BATCH_BILL_WITH_PAYMENT:
                bills = billController.billsOfBatchBill(currentRequest.getBill());
                patient = currentRequest.getBill().getPatient();
                patientEncounter = null;
                comment = null;

                navigation = "/common/request/bill_cancel_request_approvel?faces-redirect=true";
                break;

            case INWARD_SERVICE_BILL:
                patient = currentRequest.getBill().getPatient();
                patientEncounter = currentRequest.getBill().getPatientEncounter();
                bills.add(currentRequest.getBill());
                comment = null;

                navigation = "/common/request/bill_cancel_request_approvel?faces-redirect=true";
                break;
            case OPD_BILL_WITH_PAYMENT:
                navigation = "";
                break;
            default:
                navigation = "";
        }

        return navigation;
    }

    public String navigateToCancelRequest() {
        if (currentRequest == null) {
            JsfUtil.addErrorMessage("Not found for a request for Approvel");
            return "";
        }
        if (currentRequest.getBill() == null) {
            JsfUtil.addErrorMessage("Bill not found for request Cancel");
            return "";
        }

        String navigation = "";
        bills = new ArrayList<>();

        switch (currentRequest.getBill().getBillTypeAtomic()) {

            case OPD_BATCH_BILL_WITH_PAYMENT:
                bills = billController.billsOfBatchBill(currentRequest.getBill());
                patient = currentRequest.getBill().getPatient();
                comment = null;

                navigation = "/common/request/bill_cancel_request_cancel?faces-redirect=true";
                break;
            case INWARD_SERVICE_BILL:
                bills.add(currentRequest.getBill());
                patient = currentRequest.getBill().getPatient();
                patientEncounter = currentRequest.getBill().getPatientEncounter();
                comment = null;

                navigation = "/common/request/bill_cancel_request_cancel?faces-redirect=true";
                break;
            case OPD_BILL_WITH_PAYMENT:
                navigation = "";
                break;
            default:
                navigation = "";
        }
        return navigation;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Function">
    public void makeNull() {
        patient = null;
        batchBill = null;
        bills = null;
        comment = null;
        printPreview = false;

        requests = null;

        billNo = null;
        bhtNo = null;
        requestNo = null;
        requestType = null;
        status = null;
        patientEncounter = null;
    }

    public void createRequestforOPDBatchBill() {
        if (batchBill == null) {
            JsfUtil.addErrorMessage("Bill not found for Create Request ");
            return;
        }
        if (comment == null || comment.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Comment is mandatory.");
            return;
        }

        if (batchBill.getDepartment() == null || batchBill.getDepartment().getId() == null || sessionController.getDepartment() == null || sessionController.getDepartment().getId() == null) {
            JsfUtil.addErrorMessage("Department information missing.");
            return;
        }

        if (!batchBill.getDepartment().getId().equals(sessionController.getDepartment().getId())) {
            JsfUtil.addErrorMessage("You must log in to " + batchBill.getDepartment().getName() + " to cancel this bill.");
            return;
        }

        Request req = requestService.findRequest(batchBill);

        if (req != null) {
            JsfUtil.addErrorMessage("There is already a " + req.getRequestType().getDisplayName() + " requesr for this bill.");
            return;
        } else {
            for (Bill b : bills) {
                if (b.getCurrentRequest() != null) {
                    JsfUtil.addErrorMessage("There is already a " + b.getCurrentRequest().getRequestType().getDisplayName() + " requesr for this bill.");
                    return;
                }
            }

            Request newlyRequest = new Request();

            newlyRequest.setBill(batchBill);
            newlyRequest.setRequester(sessionController.getLoggedUser());
            newlyRequest.setRequestAt(new Date());
            newlyRequest.setRequestReason(comment);
            newlyRequest.setRequestType(RequestType.BILL_CANCELLATION);
            newlyRequest.setStatus(RequestStatus.PENDING);

            newlyRequest.setInstitution(sessionController.getInstitution());
            newlyRequest.setDepartment(sessionController.getDepartment());

            String reqNo = billNumberGenerator.departmentRequestNumberGeneratorYearly(sessionController.getDepartment(), RequestType.BILL_CANCELLATION);
            newlyRequest.setRequestNo(reqNo);

            requestService.save(newlyRequest, sessionController.getLoggedUser());

            //Update Batch Bill
            batchBill.setCurrentRequest(newlyRequest);
            billFacade.edit(batchBill);

            //Update Induvidual Bills of Batch Bil
            for (Bill b : bills) {
                b.setCurrentRequest(newlyRequest);
                billFacade.edit(b);
            }

            setCurrentRequest(newlyRequest);
        }

        printPreview = true;
    }

    public void createRequestforInpatientServiceBill() {
        if (batchBill == null) {
            JsfUtil.addErrorMessage("Bill not found for Create Request ");
            return;
        }
        if (comment == null || comment.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Comment is mandatory.");
            return;
        }

        if (batchBill.getDepartment() == null || batchBill.getDepartment().getId() == null || sessionController.getDepartment() == null || sessionController.getDepartment().getId() == null) {
            JsfUtil.addErrorMessage("Department information missing.");
            return;
        }

        if (!batchBill.getDepartment().getId().equals(sessionController.getDepartment().getId())) {
            JsfUtil.addErrorMessage("You must log in to " + batchBill.getDepartment().getName() + " to cancel this bill.");
            return;
        }

        Request req = requestService.findRequest(batchBill);

        if (req != null) {
            JsfUtil.addErrorMessage("There is already a " + req.getRequestType().getDisplayName() + " requesr for this bill.");
            return;
        } else {

            Request newlyRequest = new Request();

            newlyRequest.setBill(batchBill);
            newlyRequest.setRequester(sessionController.getLoggedUser());
            newlyRequest.setRequestAt(new Date());
            newlyRequest.setRequestReason(comment);
            newlyRequest.setRequestType(RequestType.BILL_CANCELLATION);
            newlyRequest.setStatus(RequestStatus.PENDING);

            newlyRequest.setInstitution(sessionController.getInstitution());
            newlyRequest.setDepartment(sessionController.getDepartment());

            String reqNo = billNumberGenerator.departmentRequestNumberGeneratorYearly(sessionController.getDepartment(), RequestType.BILL_CANCELLATION);
            newlyRequest.setRequestNo(reqNo);

            requestService.save(newlyRequest, sessionController.getLoggedUser());

            //Update Batch Bill
            batchBill.setCurrentRequest(newlyRequest);
            billFacade.edit(batchBill);

            setCurrentRequest(newlyRequest);
        }

        printPreview = true;
    }

    public void searchRequest() {
        requests = new ArrayList<>();
        requests = requestService.fillAllRequest(fromDate, toDate, billNo, bhtNo, requestNo, requestType, status, sessionController.getDepartment().getDepartmentType());
    }

    public void approveRequest() {
        if (currentRequest == null) {
            JsfUtil.addErrorMessage("Request not found for approval");
            return;
        }

        if (!webUserController.hasPrivilege("BillCancelRequestApproval")) {
            JsfUtil.addErrorMessage("You have not authorize to Approval this.");
            return;
        }

        if (currentRequest.getBill() == null) {
            JsfUtil.addErrorMessage("Bill not found for request Cancel");
            return;
        }

        currentRequest.setApprovedAt(new Date());
        currentRequest.setApprovedBy(sessionController.getLoggedUser());
        currentRequest.setStatus(RequestStatus.APPROVED);
        requestService.save(currentRequest, sessionController.getLoggedUser());

        JsfUtil.addSuccessMessage("Successfully Approve");

    }

    public void cancelApprovel() {
        if (currentRequest == null) {
            JsfUtil.addErrorMessage("Not found for a request for Approvel");
            return;
        }

        if (currentRequest.getBill() == null) {
            JsfUtil.addErrorMessage("Bill not found for request Cancel");
            return;
        }

        if (currentRequest.getStatus() != RequestStatus.APPROVED) {
            JsfUtil.addErrorMessage("Can't Cancel Approvel");
            return;
        }

        if (!webUserController.hasPrivilege("BillCancelRequestApproval")) {
            JsfUtil.addErrorMessage("You have not authorize to Approval this.");
            return;
        }

        currentRequest.setApprovedAt(null);
        currentRequest.setApprovedBy(null);
        currentRequest.setStatus(RequestStatus.UNDER_REVIEW);
        requestService.save(currentRequest, sessionController.getLoggedUser());

        System.out.println("Successfully Cancel Approvel");
        JsfUtil.addSuccessMessage("Approval cancelled");

    }

    public void rejectRequest() {
        if (currentRequest == null) {
            JsfUtil.addErrorMessage("Not found for a request for Approvel");
            return;
        }
        if (currentRequest.getBill() == null) {
            JsfUtil.addErrorMessage("Bill not found for request Cancel");
            return;
        }

        if (!webUserController.hasPrivilege("BillCancelRequestApproval")) {
            JsfUtil.addErrorMessage("You have not authorize to Cancel this.");
            return;
        }

        currentRequest.setRejectedAt(new Date());
        currentRequest.setRejectedBy(sessionController.getLoggedUser());
        currentRequest.setRejectionReason(comment);
        currentRequest.setStatus(RequestStatus.REJECTED);
        requestService.save(currentRequest, sessionController.getLoggedUser());

        //Update Batch Bill
        currentRequest.getBill().setCurrentRequest(null);
        billFacade.edit(currentRequest.getBill());

        //Update Induvidual Bills of Batch Bil
        if (bills != null) {
            for (Bill b : bills) {
                b.setCurrentRequest(null);
                billFacade.edit(b);
            }
        }

        System.out.println("Successfully Reject = ");
        JsfUtil.addSuccessMessage("Rejected successfully");

    }

    public void complteRequest(Request req) {

        req.setCompletedBy(sessionController.getLoggedUser());
        req.setCompletedAt(new Date());
        req.setStatus(RequestStatus.COMPLETED);

        requestService.save(req, sessionController.getLoggedUser());

        //Update Batch Bill
        req.getBill().setCurrentRequest(null);
        billFacade.edit(req.getBill());

        //Update Induvidual Bills of Batch Bil
        if (bills != null) {
            for (Bill b : bills) {
                b.setCurrentRequest(null);
                billFacade.edit(b);
            }
        }

        JsfUtil.addSuccessMessage("Successfully Reject");
    }

    public void cancelRequestbyUser() {
        if (currentRequest == null) {
            JsfUtil.addErrorMessage("Not found for a request for Approvel");
            return;
        }
        if (currentRequest.getBill() == null) {
            JsfUtil.addErrorMessage("Bill not found for request Cancel");
            return;
        }

        currentRequest.setCancelledAt(new Date());
        currentRequest.setCancelledBy(sessionController.getLoggedUser());
        currentRequest.setCancellationReason(comment);
        currentRequest.setStatus(RequestStatus.CANCELLED);
        requestService.save(currentRequest, sessionController.getLoggedUser());

        //Update Batch Bill
        currentRequest.getBill().setCurrentRequest(null);
        billFacade.edit(currentRequest.getBill());

        //Update Induvidual Bills of Batch Bil
        if (bills != null) {
            for (Bill b : bills) {
                b.setCurrentRequest(null);
                billFacade.edit(b);
            }
        }

        System.out.println("Successfully Reject = ");
        JsfUtil.addSuccessMessage("Cancelled successfully");
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public String getRequestNo() {
        return requestNo;
    }

    public void setRequestNo(String requestNo) {
        this.requestNo = requestNo;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    @FacesConverter(forClass = Request.class)
    public static class AreaConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            RequestController controller = (RequestController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "requestController");
            return controller.getRequestFacade().find(getKey(value));
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
            if (object instanceof Request) {
                Request o = (Request) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + Request.class.getName());
            }
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Getter & Setter">
    public boolean isPrintPreview() {
        return printPreview;
    }

    public Request getCurrentRequest() {
        return currentRequest;
    }

    public void setCurrentRequest(Request currentRequest) {
        this.currentRequest = currentRequest;
    }

    public RequestFacade getRequestFacade() {
        return requestFacade;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Bill getBatchBill() {
        return batchBill;
    }

    public void setBatchBill(Bill batchBill) {
        this.batchBill = batchBill;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public List<Request> getRequests() {
        if (requests == null) {
            requests = new ArrayList<>();
        }
        return requests;
    }

    public void setRequests(List<Request> requests) {
        this.requests = requests;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    // </editor-fold>
}
