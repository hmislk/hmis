package com.divudi.bean.common;

import com.divudi.core.data.RequestType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Request;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.RequestFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.RequestService;
import java.io.Serializable;
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

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private RequestFacade requestFacade;
    @EJB
    BillFacade billFacade;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    SessionController sessionController;
    @Inject
    BillController billController;
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Variables">
    private static final long serialVersionUID = 1L;
    
    private Patient patient;
    private Bill batchBill;
    private List<Bill> bills;
    private String comment;
    private boolean printPreview;
    
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Navigation Method">
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Function">
    public void makeNull(){
        patient = null;
        batchBill = null;
        bills = null;
        comment = null;
        printPreview = false;
    }
    
    
    public String navigateToCreateRequest(Bill bill){
        if( bill== null || bill.getId() == null){
            JsfUtil.addErrorMessage("Bill not found for request Cancel");
            return "";
        }
        String navigation = "";
        Bill originalBill = billFacade.find(bill.getId());
        printPreview = false;
        System.out.println("originalBill = " + originalBill);
        System.out.println("BillTypeAtomic = " + originalBill.getBillTypeAtomic());
        switch (originalBill.getBillTypeAtomic()) {
            case OPD_BATCH_BILL_WITH_PAYMENT:
                System.out.println("Case = OPD_BATCH_BILL_WITH_PAYMENT");
                setBatchBill(originalBill);
                bills = billController.billsOfBatchBill(batchBill);
                patient = batchBill.getPatient();
                comment = null;
                
                System.out.println("batchBill = " + batchBill.getDeptId());
                System.out.println("bills = " + bills.size());
                System.out.println("patient = " + patient.getPerson().getNameWithTitle());
                
                navigation = "/opd/opd_bill_cancel_request?faces-redirect=true";
                break;
            case OPD_BILL_WITH_PAYMENT:
                navigation = "OPD_BATCH_BILL_WITH_PAYMENT";
                break;
            default:
                
                
        }
        return navigation;
    }
    
    @Inject
    RequestService requestService;
    @EJB
    BillFacade billFacade1;
    
    public void createRequestforOPDBatchBill(){
        if(batchBill== null){
            JsfUtil.addErrorMessage("Bill not found for Create Request ");
            return ;
        }
        if(comment == null || comment.trim().contains("")){
            JsfUtil.addErrorMessage("Comment is mandatory.");
            return ;
        }
        Request currentRequest = requestService.findRequest(batchBill);
        
        if(currentRequest != null){
            JsfUtil.addErrorMessage("There is already a "+currentRequest.getRequestType().getDisplayName() +" for this bill.");
            return ;
        }else{
            Request newlyRequest = new Request();
            
            newlyRequest.setBill(batchBill);
            newlyRequest.setRequester(sessionController.getLoggedUser());
            newlyRequest.setRequestAt(new Date());
            newlyRequest.setRejectionReason(comment);
            newlyRequest.setRequestType(RequestType.BILL_CANCELLATION);
            
            requestService.save(newlyRequest, sessionController.getLoggedUser());
            
            //Update Batch Bill
            batchBill.setCurrentRequest(currentRequest);
            billFacade1.edit(batchBill);
            
            //Update Induvidual Bills of Batch Bil
            for(Bill b : billController.billsOfBatchBill(batchBill)){
                b.setCurrentRequest(currentRequest);
                billFacade1.edit(b);
            }
            
            
            
        }
        
        
        printPreview = true;
    }
    
    
    public RequestController() {
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
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
    
    // </editor-fold>
    
}
