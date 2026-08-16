/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Consultant in Health Informatics
 * buddhika.ari [at] gmail.com
 * and
 * (94) 71 5812399
 */
package com.divudi.bean.common;

// <editor-fold defaultstate="collapsed" desc="Imports">
import com.divudi.core.data.Privileges;
import static com.divudi.core.data.Privileges.PrintOriginalPoBillFromReprint;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.WebUserPrivilege;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.WebUserPrivilegeFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.WebUserRole;
import com.divudi.core.entity.WebUserRolePrivilege;
import com.divudi.core.facade.WebUserRolePrivilegeFacade;
import com.divudi.service.AuditEventService;
import com.divudi.core.entity.AuditEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
// </editor-fold>

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 *
 */
@Named
@SessionScoped
public class UserPrivilageController implements Serializable {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private WebUserPrivilegeFacade ejbFacade;
    @EJB
    private WebUserRolePrivilegeFacade facede;
    @EJB
    DepartmentFacade departmentFacade;
    @EJB
    WebUserPrivilegeFacade webUserPrivilegeFacade;
    @EJB
    AuditEventService auditEventService;
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private static final long serialVersionUID = 1L;
    private List<WebUserPrivilege> selectedItems;
    private List<WebUserPrivilege> currentWebUserPrivileges;
    private List<WebUserRolePrivilege> selectedRoleItems;
    private List<WebUserRolePrivilege> currentWebUserRolePrivileges;
    private WebUser currentWebUser;
    private WebUserRole webUserRole;
    private TreeNode[] selectedNodes;
    private TreeNode<PrivilegeHolder> rootTreeNode;
    private Institution institution;
    private Department department;
    private List<Department> departments;
    private List<PrivilegeHolder> currentUserPrivilegeHolders;
    private boolean privilegesLoaded;
    private String searchText;
    @Inject
    SessionController sessionController;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Constructors">
    public UserPrivilageController() {
    }

    @PostConstruct
    public void init() {
        rootTreeNode = createPrivilegeHolderTreeNodes();
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Functional Methods">
    public List<Department> fillWebUserDepartments(WebUser wu) {
        Set<Department> departmentSet = new HashSet<>();
        String sql = "SELECT i.department "
                + " FROM WebUserDepartment i "
                + " WHERE i.retired = :ret "
                + " AND i.webUser = :wu "
                + " ORDER BY i.department.name";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("wu", wu);
        List<Department> depts = departmentFacade.findByJpql(sql, m);
        departmentSet.addAll(depts);
        return new ArrayList<>(departmentSet);
    }

    private TreeNode<PrivilegeHolder> createPrivilegeHolderTreeNodes() {
        TreeNode root = new DefaultTreeNode(new PrivilegeHolder(null, "Root"), null);

        TreeNode allNode = new DefaultTreeNode(new PrivilegeHolder(null, "Privileges for All Sections"), root);

        // OPD Privileges
        TreeNode opdNode = new DefaultTreeNode(new PrivilegeHolder(null, "OPD"), allNode);

        TreeNode billingMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Opd, "Billing Menu"), opdNode);

        TreeNode billNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdBilling, "Bill"), opdNode);
        TreeNode billOrderingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdOrdering, "Bill without Financial Details"), opdNode);
        TreeNode preBillingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdPreBilling, "Pre Billing"), opdNode);
        TreeNode collectingCentreBillingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdCollectingCentreBilling, "Collecting Centre Billing"), opdNode);
        // OpdCollectingCentreBillingMenu and OpdCollectingCentreBillSearch are sub-items of collecting centre billing
        new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdCollectingCentreBillingMenu, "Collecting Centre Billing Menu"), collectingCentreBillingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdCollectingCentreBillSearch, "Collecting Centre Bill Search"), collectingCentreBillingNode);
        TreeNode billSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdBillSearch, "Bill Search"), opdNode);
        TreeNode billItemSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdBillItemSearch, "Bill Item Search"), opdNode);
        TreeNode reprintNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdReprint, "Reprint"), opdNode);
        TreeNode cancelNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdCancel, "Cancel"), opdNode);
        TreeNode individualCancelNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdIndividualCancel, "Individual Cancel"), opdNode);
        TreeNode packageBillCancelNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdPackageBillCancel, "Package Bill Cancel"), opdNode);
        TreeNode returnNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdReturn, "Return"), opdNode);
        TreeNode reactivateNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdReactivate, "Reactivate"), opdNode);
        TreeNode OpdLabReportSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdLabReportSearch, "Lab Report Search"), opdNode);
        TreeNode opdBillSearchEditNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdBillSearchEdit, "OPD Bill Search Edit (Patient Details)"), opdNode);
        TreeNode OpdReprintOriginalBillNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdReprintOriginalBill, "Reprint the Original Bill"), opdNode);
        TreeNode addCreditLimit = new DefaultTreeNode(new PrivilegeHolder(Privileges.AddCreditLimitInRegistration, "Add Credit Limit During Patient Registration"), opdNode);
        TreeNode addNewRefferalDoctor = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdAddNewRefferalDoctor, "Add New Referral Doctor"), opdNode);
        TreeNode addNewCollectingCentre = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdAddNewCollectingCentre, "Add New Referral Center"), opdNode);
        TreeNode opdEditPatientDetailsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdEditPatientDetails, "Edit Patient Details in OPD Billing"), opdNode);

        TreeNode cashierNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Cashier, "Cashier Menu"), opdNode);
        TreeNode acceptPaymentForCashierBills = new DefaultTreeNode(new PrivilegeHolder(Privileges.AcceptPaymentForPharmacyBills, "Accept payment for sale for cashier bills"), cashierNode);
        TreeNode scanBills = new DefaultTreeNode(new PrivilegeHolder(Privileges.ScanBillsFromCashier, "Scan Bills From Cashier Menu"), cashierNode);
        TreeNode acceptPaymentForOpdBatchBills = new DefaultTreeNode(new PrivilegeHolder(Privileges.AcceptPaymentForOpdBatchBills, "Accept payment for OPD Bactch Bills From Cashier Menu"), cashierNode);
        TreeNode refundBillsAtCashier = new DefaultTreeNode(new PrivilegeHolder(Privileges.RefundFromCashier, "Refunds From Cashier"), cashierNode);
        TreeNode refundOpdBills = new DefaultTreeNode(new PrivilegeHolder(Privileges.RefundOpdBillsFromCashier, "Refund Opd Bills From Cashier Menu"), cashierNode);
        TreeNode refundPharmacyBills = new DefaultTreeNode(new PrivilegeHolder(Privileges.RefundPharmacyBillsFromCashier, "Refund Pharmacy Bills From Cashier"), cashierNode);

        // TheaterIssueBHT is the inpatient theater BHT issue privilege, placed under OPD cashier section
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueBHT, "Theater Issue BHT"), cashierNode);

        // Inward Privileges
        TreeNode inwardNode = new DefaultTreeNode(new PrivilegeHolder(null, "Inward"), allNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.Inward, "Inward Menu"), inwardNode);
        TreeNode admissionsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Admissions"), inwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardAdmissions, "Admission Menu"), admissionsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardAdmissionsAdmission, "Admission"), admissionsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardAdmissionsEditAdmission, "Edit Admission Details"), admissionsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardAdmissionsInwardAppoinment, "Inward Appointment"), admissionsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardEditPatientDetailsFromAdmission, "Edit Patient Details From Admission"), admissionsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardEditPaymentDetails, "Edit Payment Details"), admissionsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardManageAllergies, "Manage Allergies"), admissionsNode);

        TreeNode appointmentNode = new DefaultTreeNode(new PrivilegeHolder(null, "Appointment"), inwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardAppointmentMenu, "Appointment Menu"), appointmentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.AddInwardAppointment, "Add IP Appointment"), appointmentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardAppointmentAdmission, "IP Appointment to Admit"), appointmentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardAppointmentUpdate, "IP Appointment Update"), appointmentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardAppointmentCancel, "IP Appointment Cancel"), appointmentNode);

        TreeNode roomNode = new DefaultTreeNode(new PrivilegeHolder(null, "Room"), inwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardRoom, "Room Menu"), roomNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.WatingRoomAdmitPatient, "Waiting Room Admit Patient"), roomNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardRoomRoomOccupency, "Room Occupancy"), roomNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardRoomRoomChange, "Room Change"), roomNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardRoomGurdianRoomChange, "Guardian Room Change"), roomNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardRoomDischarge, "Discharge Room"), roomNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardRoomTransferInitiate, "Initiate Room Transfer"), roomNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardRoomPatientAccept, "Accept Patient (Handover/Transfer)"), roomNode);

        TreeNode servicesItemsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Services & Items"), inwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardServicesAndItems, "Services & Items Menu"), servicesItemsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardServicesAndItemsAddServices, "Add Services"), servicesItemsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardServicesAndItemsAddOutSideCharges, "Add Outside Charges"), servicesItemsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardServicesAndItemsAddProfessionalFee, "Add Professional Fee"), servicesItemsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardServicesAndItemsAddTimedServices, "Add Timed Services"), servicesItemsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardAddChargesAfterNursingDischarge, "Add Charges After Nursing Discharge"), servicesItemsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardProcessReturnAfterNursingDischarge, "Process Return After Nursing Discharge"), servicesItemsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardHoldProfessionalPayments, "Hold Professional Payments"), servicesItemsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardPayProfessionalFeesWhileOnHold, "Pay Professional Fees While On Hold"), servicesItemsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardServiceItemRequestApproval, "Approve Service/Item Requests"), servicesItemsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardServiceItemRequestRejection, "Reject Service/Item Requests"), servicesItemsNode);

        TreeNode inwardBillingNode = new DefaultTreeNode(new PrivilegeHolder(null, "Billing"), inwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardBilling, "Billing Menu"), inwardBillingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardBillingInterimBill, "Interim Bill"), inwardBillingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardBillingInterimBillSearch, "Interim Bill Search"), inwardBillingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardFinalBillReportEdit, "Edit Patient Name After Payment Finalized"), inwardBillingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardDoctorPaymentAccess, "Doctor Payment Access"), inwardBillingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardPostFinalPaymentAccess, "Post Final Payment Access"), inwardBillingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardMakeDepositAccess, "Make Deposit Access"), inwardBillingNode);

        TreeNode dashboardPanelsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Inpatient Dashboard Panels"), inwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InpatientDashboardPanelAdmission, "Admission Panel"), dashboardPanelsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InpatientDashboardPanelBilling, "Billing Panel"), dashboardPanelsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InpatientDashboardPanelServices, "Services Panel"), dashboardPanelsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InpatientDashboardPanelRoomManagement, "Room Management Panel"), dashboardPanelsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InpatientDashboardPanelOperationTheatre, "Operation Theatre Panel"), dashboardPanelsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InpatientDashboardPanelClinicalData, "Clinical Data Panel"), dashboardPanelsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InpatientDashboardPanelPharmaceuticals, "Pharmaceuticals Panel"), dashboardPanelsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InpatientDashboardPanelDocuments, "Documents Panel"), dashboardPanelsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InpatientDashboardPanelReports, "Reports Panel"), dashboardPanelsNode);

        TreeNode inwardSurgeryNode = new DefaultTreeNode(new PrivilegeHolder(null, "Surgery"), inwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSurgeryAdd, "Add Surgery"), inwardSurgeryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSurgeryManage, "Manage Surgery"), inwardSurgeryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSurgeryValidate, "Validate Surgery"), inwardSurgeryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSurgeryValidationRevert, "Revert Surgery Validation"), inwardSurgeryNode);

        TreeNode clinicalDataViewNode = new DefaultTreeNode(new PrivilegeHolder(null, "Clinical Data Access"), inwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardPatientHistoryView, "Patient History View"), clinicalDataViewNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardClinicalNotesView, "Clinical Notes View"), clinicalDataViewNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardWardMedicationsView, "Ward Medications View"), clinicalDataViewNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardDischargeMedicationsView, "Discharge Medications View"), clinicalDataViewNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardInvestigationsView, "Investigations View"), clinicalDataViewNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardImagesView, "Images View"), clinicalDataViewNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardDiagnosisCardView, "Diagnosis Card View"), clinicalDataViewNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardEventHistoryView, "Event History View"), clinicalDataViewNode);

        TreeNode inwardPharmacyNode = new DefaultTreeNode(new PrivilegeHolder(null, "Pharmacy"), inwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardPharmacyMenu, "Pharmacy Menu"), inwardPharmacyNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardPharmacyIssueRequest, "Pharmacy Issue Request"), inwardPharmacyNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardPharmacyIssueRequestSearch, "Pharmacy Issue Request Search"), inwardPharmacyNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardPharmacyIssueRequestCancel, "Pharmacy Issue Request Cancel"), inwardPharmacyNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardPharmacyReturnCancel, "Pharmacy Return Cancel"), inwardPharmacyNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardPharmacyReturnSubmit, "Pharmacy Return Submit"), inwardPharmacyNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardPharmacyBhtReceive, "Pharmacy BHT Receive"), inwardPharmacyNode);

        TreeNode searchNode = new DefaultTreeNode(new PrivilegeHolder(null, "Search"), inwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSearch, "Search Menu"), searchNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSearchServiceBill, "Search Service Bill"), searchNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSearchAdmissionsGeneralSearch, "Search Admissions - General Search (Date Range)"), searchNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSearchProfessionalBill, "Search Professional Bill"), searchNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSearchFinalBill, "Search Final Bill"), searchNode);

        TreeNode admissionSearchScopeNode = new DefaultTreeNode(new PrivilegeHolder(null, "Admission Search Scope"), searchNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSearchAdmissionsByAdmittedDepartmentAnyInstitute, "By Admitted Department - Any Institute"), admissionSearchScopeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSearchAdmissionsByAdmittedDepartmentLoggedInstitute, "By Admitted Department - Logged Institute"), admissionSearchScopeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSearchAdmissionsByAdmittedDepartmentLoggedDepartment, "By Admitted Department - Logged Department"), admissionSearchScopeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSearchAdmissionsByCurrentDepartmentAnyInstitute, "By Current Department - Any Institute"), admissionSearchScopeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSearchAdmissionsByCurrentDepartmentLoggedInstitute, "By Current Department - Logged Institute"), admissionSearchScopeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSearchAdmissionsByCurrentDepartmentLoggedDepartment, "By Current Department - Logged Department"), admissionSearchScopeNode);

        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardReport, "Inward Reports"), inwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardPostDischargeReports, "Inward Post-Discharge Reports"), inwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardAdministration, "Administration"), inwardNode);

        TreeNode inwardLaboratoryNode = new DefaultTreeNode(new PrivilegeHolder(null, "Laboratory"), inwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardLaboratory, "Laboratory Dashboard Menu"), inwardLaboratoryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardLaboratoryBarcodeGeneration, "Barcode Generation"), inwardLaboratoryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardLaboratorySampleManagement, "Sample Management"), inwardLaboratoryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardLaboratoryReportSearch, "Report Search"), inwardLaboratoryNode);

        TreeNode inwardFormsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Forms"), inwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardFormTemplateAdmin, "Form Template Admin"), inwardFormsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardFormFill, "Fill / Edit Forms"), inwardFormsNode);

        TreeNode inwardClinicalNode = new DefaultTreeNode(new PrivilegeHolder(null, "Clinical"), inwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InpatientClinicalAssessment, "Clinical Notes / Assessments"), inwardClinicalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InpatientClinicalDischarge, "Clinical Discharge"), inwardClinicalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardNursingDischarge, "Nursing Discharge"), inwardClinicalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardPhysicalDischarge, "Physical Discharge"), inwardClinicalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardDocumentUpload, "Document Upload"), inwardClinicalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InpatientLetter, "Generate Inpatient Letters"), inwardClinicalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSendEmail, "Send Email"), inwardClinicalNode);

        TreeNode inwardPackageNode = new DefaultTreeNode(new PrivilegeHolder(null, "Packages"), inwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardPackageAdministration, "Manage Inpatient Packages"), inwardPackageNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardPackageAdmission, "Package-Based Admission"), inwardPackageNode);

        TreeNode additionalPrivilegesNode = new DefaultTreeNode(new PrivilegeHolder(null, "Additional Privileges"), inwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardAdditionalPrivilages, "Additional Privilege Menu"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardBillSearch, "Search Bills"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardBillItemSearch, "Search Bill Items"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardBillReprint, "Reprint"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardCancel, "Cancel"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardReturn, "Return"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardReactivate, "Reactivate"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ShowInwardFee, "Show Inward Fee"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardCheck, "Inward Check"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardUnCheck, "Inward Uncheck"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardFinalBillCancel, "Inward Final Bill Cancel"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardAdmissionCancel, "Inward Admission Cancel"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardOutSideMarkAsUnPaid, "Inward Outside Mark As Unpaid"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardBillSettleWithoutCheck, "Inward Bill Settle Without Check"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSearchServiceBillUnrestrictedAccess, "Inward Bill Search Without Restriction"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSettleFinalBillUnrestricted, "Inward Final Bill Settle Without Restriction"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSettleFinalBill, "Inward Settle Final Bill"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardFinalBillCreateVersion, "Inward Final Bill Create New Version"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardFinalBillSetConfirmed, "Inward Final Bill Set As Confirmed"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardFinalBillRetire, "Inward Final Bill Retire"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardFinalBillEmail, "Inward Final Bill Email"), additionalPrivilegesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSaveProvisionalFinalBill, "Inward Save Provisional Final Bill"), additionalPrivilegesNode);

        // Theatre Privileges
        TreeNode theatreNode = new DefaultTreeNode(new PrivilegeHolder(null, "Theatre"), allNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.Theatre, "Theatre Menu"), theatreNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheatreAddSurgery, "Add Surgery"), theatreNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheatreBilling, "Theatre Billing"), theatreNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheatreSendPatient, "Send Patient to Theatre"), theatreNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheatreAcceptPatient, "Accept Patient in Theatre"), theatreNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheatreReturnPatient, "Return Patient to Ward"), theatreNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.WardAcceptTheatreReturn, "Accept Patient Returning from Theatre"), theatreNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterTransfer, "Theatre Transfer Menu Item"), theatreNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterTransferRequest, "Theatre Transfer Request"), theatreNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterTransferIssue, "Theatre Transfer Issue"), theatreNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterTransferRecieve, "Theatre Transfer Receive"), theatreNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterTransferReport, "Theatre Transfer Report"), theatreNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterReports, "Theatre Show Reports Menu Item"), theatreNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterSummeries, "Theatre Show Summary Menu Item"), theatreNode);

        TreeNode theatreBHTIssueNode = new DefaultTreeNode(new PrivilegeHolder(null, "Theatre BHT Issue"), theatreNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssue, "Theatre BHT Issue"), theatreBHTIssueNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssuePharmacy, "Pharmacy BHT Issue"), theatreBHTIssueNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueStore, "General BHT Issue"), theatreBHTIssueNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueStoreBhtBilling, "Inward BHT Billing"), theatreBHTIssueNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueStoreBhtSearchBill, "Search BHT Issue Bill"), theatreBHTIssueNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueStoreBhtSearchBillItem, "Search BHT Issue Bill Items"), theatreBHTIssueNode);

        TreeNode opdIssueNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueOpd, "Opd Issue"), theatreNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueOpdForCasheir, "Opd Issue For Cashier"), opdIssueNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueOpdSearchPreBill, "Opd Issue Search Pre Bill"), opdIssueNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueOpdSearchPreBillForReturnItemOnly, "Opd Issue Return Item Only"), opdIssueNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueOpdSearchPreBillReturn, "Opd Issue Search Pre Bill Return"), opdIssueNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueOpdSearchPreBillAddToStock, "Opd Issue Pre Bill Add To Stock"), opdIssueNode);

        // Lab Privileges
        TreeNode labNode = new DefaultTreeNode(new PrivilegeHolder(null, "Lab"), allNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.Lab, "Lab Menu"), labNode);
        TreeNode labBillingMenuNode = new DefaultTreeNode(new PrivilegeHolder(null, "Billing Menu"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBilling, "Lab Bill"), labBillingMenuNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillSearch, "Lab Bill Search"), labBillingMenuNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillItemSearch, "Lab Bill Item Search"), labBillingMenuNode);

        TreeNode labDashBoardNode = new DefaultTreeNode(new PrivilegeHolder(null, "Laboratory DashBoard"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.DashBoardMenu, "DashBoard Menu"), labDashBoardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.DashBoardBillSearch, "Search Bill Bills"), labDashBoardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.DashBoardWorksheet, "Work Sheet"), labDashBoardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.DashBoardSampleSearch, "Search Sample"), labDashBoardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.DashBoardInvestigationSearch, "Search Investigation"), labDashBoardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.DashBoardReportSearch, "Report Search"), labDashBoardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.DashBoardPatientReportSearch, "Patient Report Search"), labDashBoardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.DoctorDashBoardMenu, "Doctor DashBoard Menu"), labDashBoardNode);

        TreeNode labSampleNode = new DefaultTreeNode(new PrivilegeHolder(null, "Samples"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSampleCollecting, "Sample Collection"), labSampleNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSampleSending, "Sample Send"), labSampleNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.OutLabSampleSending, "Out Lab Sample Send"), labSampleNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSampleReceiving, "Sample Receive"), labSampleNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSampleRejecting, "Sample Reject"), labSampleNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSampleSeparate, "Sample Separate"), labSampleNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSampleRetrieving, "Receiving the Sent Sample"), labSampleNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.AccessLabTestHistory, "Access Investigation History"), labSampleNode);

        TreeNode labReportingNode = new DefaultTreeNode(new PrivilegeHolder(null, "Reporting"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabDataentry, "Data Entry"), labReportingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAutherizing, "Authorize"), labReportingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabDeAutherizing, "De-Authorize"), labReportingNode);

        TreeNode labReportPrintNode = new DefaultTreeNode(new PrivilegeHolder(null, "Report Print"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabPrinting, "Report Print in Laboratory"), labReportPrintNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReportPrint, "Report Printing"), labReportPrintNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReportSearchByLoggedInstitution, "Search By Logged Institution"), labReportPrintNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReportSearchByLoggedDepartment, "Search By Logged Department"), labReportPrintNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReportSearchOwn, "Lab Report Search Own"), labReportPrintNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReportSearchAll, "Lab Report Search All"), labReportPrintNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReportSearchByDepartment, "Lab Report Search By Department"), labReportPrintNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReport, "Lab Report"), labReportPrintNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.AdminReportSearch, "Admin Report Search"), labReportPrintNode);

        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillSearchCashier, "Lab Bill Search"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillSearch, "Search Bills"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReportSearch, "Lab Report Search"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabEditPatient, "Patient Edit"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillReprint, "Lab Bill Reprint"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillReturning, "Lab Bill Return"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillCancelling, "Lab Bill Cancel"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CollectingCentreCancelling, "CC Bill Cancel"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillReactivating, "Reactivate"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReprinting, "Report Reprint"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReportFormatEditing, "Lab Report Formats Editing"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReportEdit, "Report Edit After Authorized"), labNode);

        TreeNode labSummariesNode = new DefaultTreeNode(new PrivilegeHolder(null, "Lab Summaries"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSummeries, "Lab Summaries Menu"), labSummariesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSummeriesLevel1, "Lab Summaries Level 1"), labSummariesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSummeriesLevel2, "Lab Summaries Level 2"), labSummariesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSummeriesLevel3, "Lab Summaries Level 3"), labSummariesNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabInvestigationFee, "Lab Investigation Fees"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillCancelSpecial, "Lab Bill Cancel Special"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillRefundSpecial, "Lab Bill Refund Special"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillRefunding, "Lab Bill Refunding"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabCasheirBillSearch, "Lab Cashier Bill Search"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabCashier, "Lab Cashier Menu"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabPatientDetailsEdit, "Lab Patient Details Edit"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReceive, "Lab Receive"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabRevertSample, "Lab Revert Sample"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSearchBillLoggedInstitution, "Lab Search Bill (Logged Institution)"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAddInwardServices, "Add Inward Services"), labNode);

        TreeNode labAdministrationNode = new DefaultTreeNode(new PrivilegeHolder(null, "Lab Administration"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAdiministrator, "Lab Administration Menu"), labAdministrationNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabItems, "Manage Items Menu"), labAdministrationNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabItemFeeUpadate, "Manage Item Fee Update"), labAdministrationNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabItemFeeDelete, "Manage Item Fee Delete"), labAdministrationNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReports, "Manage Reports Menu"), labAdministrationNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabLists, "Lists Menu"), labAdministrationNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSetUp, "Setup Menu"), labAdministrationNode);
        TreeNode labInwardBillingMenuNode = new DefaultTreeNode(new PrivilegeHolder(null, "Lab Inward Billing Menu"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabInwardBilling, "Lab Inward Bill"), labInwardBillingMenuNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabInwardSearchServiceBill, "Lab Inward Bill Search"), labInwardBillingMenuNode);
        TreeNode labCollectingCenterBillingNode = new DefaultTreeNode(new PrivilegeHolder(null, "Lab Collecting Center Billing"), labNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabCollectingCentreBilling, "Lab Collecting Center Menu"), labCollectingCenterBillingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabCCBilling, "Lab Collecting Center Billing"), labCollectingCenterBillingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabCCBillingSearch, "Lab Collecting Center Bill Search"), labCollectingCenterBillingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReporting, "Lab Reporting"), labNode);

        // Pharmacy Privileges
        TreeNode pharmacyNode = new DefaultTreeNode(new PrivilegeHolder(null, "Pharmacy"), allNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.Pharmacy, "Pharmacy Menu"), pharmacyNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdministration, "Pharmacy Administration"), pharmacyNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyItemNameEdit, "Pharmacy Item Name Edit"), pharmacyNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDonation, "Pharmacy Donation"), pharmacyNode);

        // Channelling Privileges
        TreeNode channellingNode = new DefaultTreeNode(new PrivilegeHolder(null, "Channelling"), allNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.Channelling, "Channelling Menu"), channellingNode);
        TreeNode channelBooking = new DefaultTreeNode(new PrivilegeHolder(null, "Channel Booking"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingChannelBooking, "Channel Booking"), channelBooking);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelSessionMultipleDeletion, "Channel Sessions Multiple Deletion"), channelBooking);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelSessionHolidayMark, "Channel Sessions Holiday Mark"), channelBooking);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelSessionManagement, "Channel Sessions Management"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelSheduleManagement, "Channel Shedule Management"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingByMonth, "Channel Booking by Month"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelPatientPortal, "Channel Patient portal"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelDoctorCard, "Channel Doctor card"), channellingNode);

        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingFutureChannelBooking, "Channel Future Booking"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPastBooking, "Past Booking"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingBookedList, "Booked List"), channellingNode);
        TreeNode channelDoctorLeaveMenuNode = new DefaultTreeNode(new PrivilegeHolder(null, "Doctor Leave Menu"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingDoctorLeaveByDate, "Doctor Leave By Date"), channelDoctorLeaveMenuNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingDoctorLeaveByServiceSession, "Doctor Leave By Service Session"), channelDoctorLeaveMenuNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingChannelSheduling, "Channel Scheduling"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingSpecialChannelSheduling, "Special Channel Scheduling"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingChannelAgentFee, "Channel Agent Fee"), channellingNode);
        TreeNode channelBookingInterfaceNode = new DefaultTreeNode(new PrivilegeHolder(null, "Channel Booking Interface"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingBokking, "Booking"), channelBookingInterfaceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingReprint, "Reprint"), channelBookingInterfaceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingCancel, "Cancel"), channelBookingInterfaceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingRefund, "Refund"), channelBookingInterfaceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingSettle, "Settle"), channelBookingInterfaceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingChange, "Change"), channelBookingInterfaceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingSearch, "Search"), channelBookingInterfaceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingViews, "Views"), channelBookingInterfaceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingDocPay, "Doctor Payment"), channelBookingInterfaceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingRestric, "Restrict Channel Booking"), channelBookingInterfaceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPrintInPastBooking, "Print Past Booking Receipt"), channellingNode);
        TreeNode channelPaymentNode = new DefaultTreeNode(new PrivilegeHolder(null, "Payment"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPayment, "Payment Menu"), channelPaymentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPaymentPayDoctor, "Pay Doctor"), channelPaymentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPaymentDueSearch, "Payment Due Search"), channelPaymentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPaymentDoneSearch, "Payment Done Search"), channelPaymentNode);
        TreeNode channelCashierTransactionNode = new DefaultTreeNode(new PrivilegeHolder(null, "Cashier Transaction"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCashierTransaction, "Cashier Transaction Menu"), channelCashierTransactionNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCashierTransactionIncome, "Income"), channelCashierTransactionNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCashierTransactionIncomeSearch, "Income Search"), channelCashierTransactionNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCashierTransactionExpencess, "Expenses"), channelCashierTransactionNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCashierTransactionExpencessSearch, "Expenses Search"), channelCashierTransactionNode);
        TreeNode channelAdministratorNode = new DefaultTreeNode(new PrivilegeHolder(null, "Administrator"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingApoinmentNumberCountEdit, "Edit Appointment Count"), channelAdministratorNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingEditSerialNo, "Edit Appointment Number"), channelAdministratorNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingEditPatientDetails, "Edit Patient Details"), channelAdministratorNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingChannelShedulRemove, "Delete Schedule"), channelAdministratorNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingChannelShedulName, "Edit Session Name"), channelAdministratorNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingChannelShedulStartingNo, "Edit Session Starting No"), channelAdministratorNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingChannelShedulRoomNo, "Edit Session Room No"), channelAdministratorNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingChannelShedulMaxRowNo, "Edit Session Max Row No"), channelAdministratorNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingEditCreditLimitUserLevel, "Edit Credit Limit User Level"), channelAdministratorNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingEditCreditLimitAdminLevel, "Edit Credit Limit Administrator Level"), channelAdministratorNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingReprintOriginalBill, "Channelling Reprint Original Bill"), channelAdministratorNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPastBookingPatientAttend, "Channelling Attend Patients To Past Booking"), channelAdministratorNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelAdd, "Channel Add"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCancel, "Channel Cancel"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelRefund, "Channel Refund"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelReturn, "Channel Return"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelView, "Channel View"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelViewHistory, "Channel View History"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelDoctorPayments, "Channel Doctor Payments"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelDoctorPaymentCancel, "Channel Doctor Payment Cancel"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCreateSessions, "Channel Create Sessions"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCreateSpecialSessions, "Channel Create Special Sessions"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelManageSessions, "Channel Manage Sessions"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelAdministration, "Channel Administration"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelAgencyReports, "Channel Agency Reports"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingDoctorLeave, "Channelling Doctor Leave"), channelDoctorLeaveMenuNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingDoctorSessionView, "Channelling Doctor Session View"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelReports, "Channel Reports"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelSummery, "Channel Summary"), channellingNode);
        TreeNode channelManagementNode = new DefaultTreeNode(new PrivilegeHolder(null, "Channel Management"), channellingNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelManagement, "Channel Management Menu"), channelManagementNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelAgencyAgencies, "Channel Agencies"), channelManagementNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelAgencyCreditLimitUpdate, "Channel Agency Credit Limit Update"), channelManagementNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelAgencyCreditLimitUpdateBulk, "Channel Agency Credit Limit Update (Bulk)"), channelManagementNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelAddChannelBookToAgency, "Add Channel Book To Agency"), channelManagementNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelManageSpecialities, "Channel Management Specialities"), channelManagementNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelManageConsultants, "Channel Management Consultants"), channelManagementNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelEditingAppoinmentCount, "Channel Editing Appointment Count"), channelManagementNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelAddChannelingConsultantToInstitutions, "Add Channelling Consultants To Institution"), channelManagementNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelFeeUpdate, "Channel Fee Update"), channelManagementNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCrdeitNote, "Channel Credit Note"), channelManagementNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCrdeitNoteSearch, "Channel Credit Note Search"), channelManagementNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelDebitNote, "Channel Debit Note"), channelManagementNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelDebitNoteSearch, "Channel Debit Note Search"), channelManagementNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCashCancelRestriction, "Channel Cash Cancel Restriction"), channelManagementNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelActiveVat, "Channel Active Vat"), channelManagementNode);

        TreeNode clinicsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Clinics"), allNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.Clinic, "Clinics"), clinicsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicSession, "Clinic Session"), clinicsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicCalendar, "Clinic Calendar"), clinicsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicQueue, "Clinic Queue"), clinicsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicDisplay, "Clinic Display"), clinicsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicSchedule, "Clinic Schedule"), clinicsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicReports, "Clinic Reports"), clinicsNode);

        // Payment Privileges
        TreeNode paymentNode = new DefaultTreeNode(new PrivilegeHolder(null, "Payment"), allNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.Payment, "Payment Menu"), paymentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PaymentBilling, "Staff Payment Billing"), paymentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PaymentBillSearch, "Payment Search"), paymentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PaymentBillReprint, "Payment Reprints"), paymentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PaymentBillCancel, "Payment Cancel"), paymentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PaymentBillRefund, "Payment Refund"), paymentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PaymentBillReactivation, "Payment Reactivation"), paymentNode);

        // Reports Privileges
        TreeNode reportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Reports"), allNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.Reports, "Reports Menu"), reportsNode);
        TreeNode forOwnInstitutionNode = new DefaultTreeNode(new PrivilegeHolder(null, "For Own Institution"), reportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSearchCashCardOwn, "Cash/Card Bill Reports"), forOwnInstitutionNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSearchCreditOwn, "Credit Bill Reports"), forOwnInstitutionNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsItemOwn, "Item Reports"), forOwnInstitutionNode);
        TreeNode forAllInstitutionNode = new DefaultTreeNode(new PrivilegeHolder(null, "For All Institution"), reportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSearchCashCardOther, "Cash/Card Bill Reports"), forAllInstitutionNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportSearchCreditOther, "Credit Bill Reports"), forAllInstitutionNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsItemOther, "Item Reports"), forAllInstitutionNode);

        // Clinical Privileges
        TreeNode clinicalsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Clinicals"), allNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.Clinical, "Clinical Data"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientSummery, "Patient Summary"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientDetails, "Patient Details"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientPhoto, "Patient Photo"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalVisitDetail, "Visit Details"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalVisitSummery, "Visit Summary"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalHistory, "History"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalAdministration, "Administration"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalAdministrationEditLetter, "Edit Letter Templates"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientAdd, "Add Patient"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientEdit, "Edit Patient"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientNameChange, "Change Patient Name"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientPhoneNumberEdit, "Edit Patient Phone Number"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientBlacklist, "Blacklist / Unblacklist Patient"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientPseudonymise, "Pseudonymise Patient"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientCommentsView, "View Patient Comments"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientCommentsEdit, "Edit Patient Comments"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalMembershipAdd, "Add Membership"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalMembershipEdit, "Edit Membership"), clinicalsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientDelete, "Clinical Patient Delete"), clinicalsNode);

        // Administration Privileges
        TreeNode adminNode = new DefaultTreeNode(new PrivilegeHolder(null, "Administration"), allNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.Admin, "Admin Menu"), adminNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.AdminManagingUsers, "Manage Users"), adminNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.AdminInstitutions, "Manage Institutions"), adminNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.AdminStaff, "Manage Staff"), adminNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.AdminItems, "Manage Items/Services"), adminNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.AdminPrices, "Manage Fees/Prices/Packages"), adminNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.AdminPatientRelationships, "Manage Patient Relationships"), adminNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.AdminInactivePatients, "Manage Inactive Patients"), adminNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MergePatients, "Merge Patients"), adminNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ClientPortalCreateAccount, "Create Client Portal Account"), adminNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ManageCreditCompany, "Manage Credit Companies"), adminNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.AdminFilterWithoutDepartment, "Filter Without Department"), adminNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.SearchAll, "Search All"), adminNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChangeProfessionalFee, "Change Professional Fee"), adminNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.SendBulkSMS, "Send Bulk SMS"), adminNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.Developers, "Only For Developers"), adminNode);

        // Membership Privileges
        TreeNode membershipNode = new DefaultTreeNode(new PrivilegeHolder(null, "Membership"), allNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShip, "Membership Menu"), membershipNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipAdd, "Add Members"), membershipNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipSearch, "Search Members"), membershipNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipEdit, "Edit Members"), membershipNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MembershipReports, "Membership Reports"), membershipNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MembershipDiscountManagement, "Membership Discount Management"), membershipNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MembershipAdministration, "Membership Administration"), membershipNode);
        TreeNode otherNode = new DefaultTreeNode(new PrivilegeHolder(null, "Other"), membershipNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MembershipSchemes, "Membership Schemes"), otherNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipInwardMemberShip, "Inward Membership Menu"), otherNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipInwardMemberShipSchemesDicounts, "Schemes Discounts"), otherNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipInwardMemberShipInwardMemberShipReport, "Inward Membership Report"), otherNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipOpdMemberShipDis, "Opd Membership Dis Menu"), otherNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipOpdMemberShipDisByDepartment, "Discount By Department"), otherNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipOpdMemberShipDisByCategory, "Discount By Category"), otherNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipOpdMemberShipDisOpdMemberShipReport, "Opd Membership Report"), otherNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipMemberReActive, "Re-Activate Registered Patient"), otherNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipMemberDeActive, "De-Activate Registered Patient"), otherNode);

        // Human Resource Privileges
        TreeNode humanResourceNode = new DefaultTreeNode(new PrivilegeHolder(null, "Human Resource"), allNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.Hr, "HR Menu"), humanResourceNode);
        TreeNode workingTimeNode = new DefaultTreeNode(new PrivilegeHolder(null, "Working Time"), humanResourceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrWorkingTime, "Working Time Menu"), workingTimeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrRosterTable, "Roster Table"), workingTimeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrUploadAttendance, "Upload Attendance"), workingTimeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrAnalyseAttendenceByRoster, "Analyse Attendance By Roster"), workingTimeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrAnalyseAttendenceByStaff, "Analyse Attendance By Staff"), workingTimeNode);
        TreeNode formNode = new DefaultTreeNode(new PrivilegeHolder(null, "Form"), humanResourceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrForms, "Form Menu"), formNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrLeaveForms, "Leave Form"), formNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrAdditionalForms, "Additional Form"), formNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrAdvanceSalary, "HR Salary Advance"), humanResourceNode);
        TreeNode hrSalaryNode = new DefaultTreeNode(new PrivilegeHolder(null, "HR Salary"), humanResourceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrGenerateSalary, "HR Salary Generate"), hrSalaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrGenerateSalarySpecial, "HR Salary Generate Special"), hrSalaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrPrintSalary, "HR Salary Print"), humanResourceNode);
        TreeNode hrReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "HR Reports"), humanResourceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrReports, "HR Reports Menu"), hrReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrReportsLevel1, "HR Reports Level 1"), hrReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrReportsLevel2, "HR Reports Level 2"), hrReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrReportsLevel3, "HR Reports Level 3"), hrReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.EmployeeHistoryReport, "Employee History Report"), hrReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrAdmin, "HR Administration Menu"), humanResourceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.hrDeleteLateLeave, "HR Delete Late Leave"), humanResourceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrEditRetiedDate, "HR Edit Retired Date"), humanResourceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.HrRemoveResignDate, "HR Remove Resign Date"), humanResourceNode);

        // Store Privileges
        TreeNode storeNode = new DefaultTreeNode(new PrivilegeHolder(null, "Store"), allNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.Store, "Store Menu"), storeNode);
        TreeNode storeIssueNode = new DefaultTreeNode(new PrivilegeHolder(null, "Issue"), storeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreIssue, "Issue Menu"), storeIssueNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreIssueInwardBilling, "Inward Billing"), storeIssueNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreIssueSearchBill, "Search Issue Bill"), storeIssueNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreIssueBillItems, "Search Issue Bill Items"), storeIssueNode);
        TreeNode storePurchaseNode = new DefaultTreeNode(new PrivilegeHolder(null, "Purchase"), storeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchase, "Purchase Menu"), storePurchaseNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchaseOrder, "Purchase Order"), storePurchaseNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchaseOrderApprove, "PO Approve"), storePurchaseNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchaseGRNRecive, "GRN Receive"), storePurchaseNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchaseGRNReturn, "GRN Return"), storePurchaseNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchasePurchase, "Purchase"), storePurchaseNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchaseOrderApproveSearch, "PO Approve Search"), storePurchaseNode);
        TreeNode storeTransferNode = new DefaultTreeNode(new PrivilegeHolder(null, "Transfer"), storeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreTransfer, "Transfer Menu"), storeTransferNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreTransferRequest, "Request"), storeTransferNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreTransferIssue, "Issue"), storeTransferNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreTransferRecive, "Receive"), storeTransferNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreTransferReport, "Report"), storeTransferNode);
        TreeNode storeAdjustmentNode = new DefaultTreeNode(new PrivilegeHolder(null, "Adjustment"), storeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreAdjustment, "Adjustment Menu"), storeAdjustmentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreAdjustmentDepartmentStock, "Department Stock (Qty)"), storeAdjustmentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreAdjustmentStaffStock, "Staff Stock Adjustment"), storeAdjustmentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreAdjustmentPurchaseRate, "Purchase Rate"), storeAdjustmentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreAdjustmentSaleRate, "Sale Rate"), storeAdjustmentNode);
        TreeNode storeDealorPaymentNode = new DefaultTreeNode(new PrivilegeHolder(null, "Delor Payment"), storeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreDealorPayment, "Delor Payment Menu"), storeDealorPaymentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreDealorPaymentDueSearch, "Delor Due Search"), storeDealorPaymentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreDealorPaymentDueByAge, "Delor Due By Age"), storeDealorPaymentNode);
        TreeNode storePaymentNode = new DefaultTreeNode(new PrivilegeHolder(null, "Payment"), storeDealorPaymentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreDealorPaymentPayment, "Payment Menu"), storePaymentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreDealorPaymentPaymentGRN, "GRN Payment"), storePaymentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreDealorPaymentPaymentGRNSelect, "GRN Payment (Select)"), storePaymentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreDealorPaymentGRNDoneSearch, "GRN Payment Due Search"), storeDealorPaymentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreSearch, "Search Menu"), storeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreReports, "Report Menu"), storeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreSummery, "Summary Menu"), storeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreAdministration, "Administration Menu"), storeNode);

        TreeNode courierNode = new DefaultTreeNode(new PrivilegeHolder(null, "Courier"), allNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.Courier, "Courier Menu"), courierNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CourierCollectSamples, "Courier Collect Samples"), courierNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CourierHandoverSamplesToLab, "Courier Handover Samples to Lab"), courierNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CourierViewReports, "Courier View Reports"), courierNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CourierPrintReports, "Courier Print Reports"), courierNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CourierViewStatistics, "Courier View Statistics"), courierNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CourierViewBillReports, "Courier View Bill Reports"), courierNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CourierViewPaymentReports, "Courier View Payment Reports"), courierNode);

        TreeNode collectingCentreNode = new DefaultTreeNode(new PrivilegeHolder(null, "Collecting Centre"), allNode);

        TreeNode collectingCentreManageNode = new DefaultTreeNode(new PrivilegeHolder(null, "Collecting Centre Manage"), collectingCentreNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CollectingCentreManageMenu, "Collecting Centre Manage Menu"), collectingCentreManageNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CollectingCentreBilling, "Collecting Centre Billing"), collectingCentreManageNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CCPaymentReceive, "Collecting Centre Payment Receive"), collectingCentreManageNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.SearchCCPaymentReceive, "Search Collecting Centre Payment Receive"), collectingCentreManageNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.IssueReferenceBook, "Collecting Centre Issue Reference Book"), collectingCentreManageNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.SearchIssuedReferenceBook, "Search Collecting Centre Reference Book"), collectingCentreManageNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChangeCreditLimitInCC, "Change Collecting Centre Credit Limit"), collectingCentreManageNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PayCollectingCentre, "Pay Collecting Centre"), collectingCentreManageNode);

        TreeNode creditDebitNoteNode = new DefaultTreeNode(new PrivilegeHolder(null, "Credit/Debit Note"), collectingCentreNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CollectingCentreCreditDebitNoteMenu, "Credit/Debit Note Menu"), creditDebitNoteNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CollectingCentreCreditNote, "Collecting Centre Credit Note"), creditDebitNoteNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CollectingCentreDebitNote, "Collecting Centre Debit Note"), creditDebitNoteNode);

        new DefaultTreeNode(new PrivilegeHolder(Privileges.CollectingCentreReports, "Collecting Centre Reports"), collectingCentreNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChangeCollectingCentre, "Change Collecting Centre"), collectingCentreNode);

        // User Menu
        TreeNode userNode = new DefaultTreeNode(new PrivilegeHolder(null, "User"), allNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.UserMenu, "User Menu"), userNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChangeMyPassword, "Change User Password"), userNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChangeMyTheme, "Change User Theme"), userNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChangePreferece, "Change User Preferances"), userNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChangeMyApiKeys, "Change API Keys"), userNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.AiChat, "AI Chat"), userNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ChangeReceiptPrintingPaperTypes, "Change Receipt Printing Paper Types"), userNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.MyFinanacialTransactionManager, "User Financial Transaction Manager"), userNode);

        // Search Privileges
        TreeNode searchRootNode = new DefaultTreeNode(new PrivilegeHolder(null, "Search"), allNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.Search, "Search Menu"), searchRootNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.SearchGrand, "Grand Search"), searchRootNode);

        // Cash Transaction Privileges
        TreeNode cashTransactionNode = new DefaultTreeNode(new PrivilegeHolder(null, "Cash Transaction"), allNode);
        TreeNode cashTransactionMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.CashTransaction, "Cash Transaction Menu"), cashTransactionNode);
        TreeNode cashInNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.CashTransactionCashIn, "Cash In"), cashTransactionNode);
        TreeNode cashOutNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.CashTransactionCashOut, "Cash Out"), cashTransactionNode);

        TreeNode handoverAcceptAsCashier = new DefaultTreeNode(new PrivilegeHolder(Privileges.ShiftHandoverAcceptAsCashier, "Shift Handover Accept As A Cashier"), cashTransactionNode);
        TreeNode handoverAcceptAsMainCashier = new DefaultTreeNode(new PrivilegeHolder(Privileges.ShiftHandoverAcceptAsMainCashier, "Shift Handover Accept As Main Cashier"), cashTransactionNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.SettleHandoverProofMissing, "Settle Handover Proof Missing"), cashTransactionNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.SettleNonCashPayments, "Settle Non-Cash Payments"), cashTransactionNode);

        TreeNode listToCashReceiveNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.CashTransactionListToCashRecieve, "List To Cash Receive"), cashTransactionNode);

        TreeNode PettyCashBillApprove = new DefaultTreeNode(new PrivilegeHolder(Privileges.PettyCashBillApprove, "Petty Cash Bill Approval"), cashTransactionNode);
        TreeNode PettyCashBillCancellationApprove = new DefaultTreeNode(new PrivilegeHolder(Privileges.PettyCashBillCancellationApprove, "Petty Cash Bill Cancellation Approval"), cashTransactionNode);
        TreeNode PettyCashEditFinancialYear = new DefaultTreeNode(new PrivilegeHolder(Privileges.PettyCashEditFinancialYear, "Petty Cash Edit Financial Year"), cashTransactionNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.AllCashierSummery, "All Cashier Summary"), cashTransactionNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CashierHandoverStatusReport, "Cashier Handover Status Report"), cashTransactionNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.IncomeReport, "Income Report"), cashTransactionNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.DuesAndAccess, "Dues and Access"), cashTransactionNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CheckEnteredData, "Check Entered Data"), cashTransactionNode);

        //Pharmacy
        TreeNode pharmacyTokenManagement = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyTokenManagement, "Pharmacy Token Management"), pharmacyNode);
        TreeNode retailTransaction = new DefaultTreeNode(new PrivilegeHolder(null, "Pharmacy Retail Transaction"), pharmacyNode);
        TreeNode retailTransactionMenu = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyRetailTransactionMenue, "Pharmacy Retail Transaction Menu"), retailTransaction);
        TreeNode PharmacySale = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySale, "Pharmacy Sale"), retailTransaction);
        TreeNode PharmacySaleForCashier = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleForCashier, "Pharmacy Sale For Cashier"), retailTransaction);
        TreeNode PharmacySaleQuick = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleQuick, "Pharmacy Sale - Quick"), retailTransaction);
        TreeNode PharmacySaleForCashierQuick = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleForCashierQuick, "Pharmacy Sale For Cashier - Quick"), retailTransaction);
        TreeNode PharmacySaleWithOutStock = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleWithOutStock, "Pharmacy Sale With Out Stock"), retailTransaction);
        TreeNode PharmacySearchSaleBill = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySearchSaleBill, "Pharmacy Search Sale Bill"), retailTransaction);
        TreeNode PharmacySearchSalePreBill = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySearchSalePreBill, "Pharmacy Search Sale PreBill"), retailTransaction);
        TreeNode PharmacySearchSaleBillItems = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySearchSaleBillItems, "Pharmacy Search Sale BillItems"), retailTransaction);
        TreeNode PharmacyReturnItemsOnly = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyReturnItemsOnly, "Pharmacy Return Items Only"), retailTransaction);
        TreeNode PharmacyReturnItemsAndPayments = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyReturnItemsAndPayments, "Pharmacy Return Items And Payments"), retailTransaction);
        TreeNode PharmacySearchReturnBill = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySearchReturnBill, "Pharmacy Search ReturnBill"), retailTransaction);
        TreeNode PharmacySearchReturnBillCancel = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySearchReturnBillCancel, "Pharmacy Search Return Bill Cancel"), retailTransaction);
        TreeNode PharmacySaleReprint = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleReprint, "Pharmacy Sale Reprint"), retailTransaction);
        TreeNode PrintOriginalPharmacyBillFromReprint = new DefaultTreeNode(new PrivilegeHolder(Privileges.PrintOriginalPharmacyBillFromReprint, "Print Original Pharmacy Bill From Reprint"), retailTransaction);
        TreeNode PharmacySaleCancel = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleCancel, "Pharmacy Sale Bill Cancel"), retailTransaction);
        TreeNode PharmacyAddToStock = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAddToStock, "Pharmacy Add To Stock"), retailTransaction);

        TreeNode PharmacyWholeSaleTransAction = new DefaultTreeNode("Pharmacy Wholesale Transaction", pharmacyNode);
        TreeNode PharmacyWholeSaleTransactionMenue = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyWholeSaleTransactionMenue, "Procurement Menu"), PharmacyWholeSaleTransAction);
        TreeNode PharmacyWholesaleSale = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyWholesaleSale, "Pharmacy Wholesale Sale"), PharmacyWholeSaleTransAction);
        TreeNode PharmacyWholesaleSaleForCashier = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyWholesaleSaleForCashier, "Pharmacy Wholesale Sale For Cashier"), PharmacyWholeSaleTransAction);
        TreeNode PharmacyWholesaleSearchSaleBill = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyWholesaleSearchSaleBill, "Pharmacy Wholesale Search Sale Bill"), PharmacyWholeSaleTransAction);
        TreeNode PharmacyWholesaleSearchSaleBillToPay = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyWholesaleSearchSaleBillToPay, "Pharmacy Wholesale Search Sale Bill To Pay"), PharmacyWholeSaleTransAction);
        TreeNode PharmacyWholesaleSearchSaleBillItems = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyWholesaleSearchSaleBillItems, "Pharmacy Wholesale Search Sale Bill Items"), PharmacyWholeSaleTransAction);
        TreeNode PharmacyWholesaleReturnItemsOnly = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyWholesaleReturnItemsOnly, "Pharmacy Wholesale Return Items Only"), PharmacyWholeSaleTransAction);
        TreeNode PharmacyWholesaleWholeSaleAddToStock = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyWholesaleWholeSaleAddToStock, "Pharmacy WholeSale Add To Stock"), PharmacyWholeSaleTransAction);
        TreeNode PharmacyWholeSalePurchase = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyWholeSalePurchase, "Pharmacy Whole Sale Purchase"), PharmacyWholeSaleTransAction);
        TreeNode PharmacySearchReturnBillItems = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySearchReturnBillItems, "Pharmacy Search Return Bill Items"), PharmacyWholeSaleTransAction);

        TreeNode disbursementNode = new DefaultTreeNode("Pharmacy Disbursement", pharmacyNode);
        TreeNode disbursementMenue = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisburesementMenu, "Pharmacy Disburesement Menu"), disbursementNode);
        TreeNode disbursementRequest = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisbursementRequest, "Request"), disbursementNode);
        TreeNode disbursementFinalizeRequest = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisbursementFinalizeRequest, "Finalize Transfer Request"), disbursementNode);
        TreeNode PharmacyDisbursementApproval = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisbursementRequestApproval, "Pharmacy Disbursement Request Approval"), disbursementNode);
        TreeNode disbursementIssueForRequest = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisbursementIssurForRequest, "Issue for Request"), disbursementNode);
        TreeNode issueForRequestSave = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyIssueForRequestSave, "Issue for Request Save"), disbursementNode);
        TreeNode issueForRequestFinalize = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyIssueForRequestFinalize, "Issue for Request Finalize"), disbursementNode);
        TreeNode issueForRequestApprove = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyIssueForRequestApprove, "Issue for Request Approve"), disbursementNode);
        TreeNode disbursementDirectIssue = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisbursementDirectIssue, "Direct Issue"), disbursementNode);
        TreeNode disbursementRecieve = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisbursementRecieve, "Recieve"), disbursementNode);
        TreeNode receiveSave = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyReceiveSave, "Receive Save"), disbursementNode);
        TreeNode receiveFinalize = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyReceiveFinalize, "Receive Finalize"), disbursementNode);
        TreeNode receiveApprove = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyReceiveApprove, "Receive Approve"), disbursementNode);
        TreeNode transferIssueCancel = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyTransferIssueCancel, "Transfer Issue Cancel"), disbursementNode);
        TreeNode transferReceiveCancel = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyTransferReceiveCancel, "Transfer Receive Cancel"), disbursementNode);
        TreeNode TransferReciveApproval = new DefaultTreeNode(new PrivilegeHolder(Privileges.TransferReciveApproval, "Recieve Approval"), disbursementNode);
        TreeNode PharmacyDisbursementReports = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisbursementReports, "Pharmacy Disbursement Reports"), disbursementNode);
        TreeNode PharmacyTransferViewRates = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyTransferViewRates, "Pharmacy Transfer View Rates"), disbursementNode);
        TreeNode StockRequestViewRates = new DefaultTreeNode(new PrivilegeHolder(Privileges.StockRequestViewRates, "Stock Request View Rates"), disbursementNode);
        TreeNode ConsumptionViewRates = new DefaultTreeNode(new PrivilegeHolder(Privileges.ConsumptionViewRates, "Consumption View Rates"), disbursementNode);
        TreeNode StockTransactionViewRates = new DefaultTreeNode(new PrivilegeHolder(Privileges.StockTransactionViewRates, "Stock Transaction View Rates"), disbursementNode);
        TreeNode DiscardViewRates = new DefaultTreeNode(new PrivilegeHolder(Privileges.DiscardViewRates, "Discard View Rates"), disbursementNode);

        TreeNode InpatientMedicationManagementNode = new DefaultTreeNode("Inpatient medication Management", pharmacyNode);
        TreeNode InpatientMedicationManagementMenue = new DefaultTreeNode(new PrivilegeHolder(Privileges.InpatientMedicationManagementMenue, "Procurement Menu"), InpatientMedicationManagementNode);
        TreeNode PharmacyDirectIssueToBht = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDirectIssueToBht, "Pharmacy Direct Issue To Bht"), InpatientMedicationManagementNode);
        TreeNode PharmacyDischargeMedicineIssue = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDischargeMedicineIssue, "Pharmacy Discharge Medicine Issue"), InpatientMedicationManagementNode);
        TreeNode PharmacyDirectIssueToTheaterCases = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDirectIssueToTheaterCases, "Pharmacy Direct Issue To Theater Cases"), InpatientMedicationManagementNode);
        TreeNode PharmacyBhtIssueRequest = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyBhtIssueRequest, "Pharmacy Bht Issue Request"), InpatientMedicationManagementNode);
        TreeNode PharmacyBhtRequestForceComplete = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyBhtRequestForceComplete, "Pharmacy Bht Request Force Complete"), InpatientMedicationManagementNode);
        TreeNode PharmacyReturnFromWardForceComplete = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyReturnFromWardForceComplete, "Pharmacy Return From Ward Force Complete"), InpatientMedicationManagementNode);
        TreeNode PharmacySearchInpatientDirectIssuesbyBill = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySearchInpatientDirectIssuesbyBill, "Pharmacy Search Inpatient Direct Issues by Bill"), InpatientMedicationManagementNode);
        TreeNode PharmacySearchInpatientDirectIssuesbyItem = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySearchInpatientDirectIssuesbyItem, "Pharmacy Search Inpatient Direct Issues by Item"), InpatientMedicationManagementNode);
        TreeNode PharmacySearchInpatientDirectIssueReturnsbyBill = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySearchInpatientDirectIssueReturnsbyBill, "Pharmacy Search Inpatient Direct Issue Returns by Bill"), InpatientMedicationManagementNode);
        TreeNode PharmacysSearchInpatientDirectIssueReturnsbyItem = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacysSearchInpatientDirectIssueReturnsbyItem, "Pharmacy Search Inpatient Direct Issue Returns by Item"), InpatientMedicationManagementNode);
        TreeNode NursingIPBillingViewRates = new DefaultTreeNode(new PrivilegeHolder(Privileges.NursingIPBillingViewRates, "Nursing IP Billing View Rates"), InpatientMedicationManagementNode);
        TreeNode IPRequestViewRates = new DefaultTreeNode(new PrivilegeHolder(Privileges.IPRequestViewRates, "IP Request View Rates"), InpatientMedicationManagementNode);

        TreeNode ProcumentNode = new DefaultTreeNode("Pharmacy Procument", pharmacyNode);
        TreeNode pharmacyProcurementMenu = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyProcurementMenu, "Procurement Menu"), ProcumentNode);
        TreeNode pharmacyCreatePurchaseOrder = new DefaultTreeNode(new PrivilegeHolder(Privileges.CreatePurchaseOrder, "Create Purchase Order"), ProcumentNode);
        TreeNode pharmacyAutoOrderPModel = new DefaultTreeNode(new PrivilegeHolder(Privileges.AutoOrderPModel, "Auto Order (P Model)"), ProcumentNode);
        TreeNode pharmacyAutoOrderQModel = new DefaultTreeNode(new PrivilegeHolder(Privileges.AutoOrderQModal, "Auto Order (Q Model)"), ProcumentNode);
        TreeNode pharmacyDirectPurchase = new DefaultTreeNode(new PrivilegeHolder(Privileges.DirectPurchase, "Direct Purchase"), ProcumentNode);
        TreeNode pharmacyDirectPurchaseSave = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDirectPurchaseSave, "Pharmacy Direct Purchase Save"), ProcumentNode);
        TreeNode pharmacyDirectPurchaseFinalize = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDirectPurchaseFinalize, "Pharmacy Direct Purchase Finalize"), ProcumentNode);
        TreeNode pharmacyDirectPurchaseApprove = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDirectPurchaseApprove, "Pharmacy Direct Purchase Approve"), ProcumentNode);
        TreeNode pharmacyPurchaseOrderApprovel = new DefaultTreeNode(new PrivilegeHolder(Privileges.PurchaseOrdersApprovel, "Purchase Orders Approvel"), ProcumentNode);
        TreeNode pharmacyPurchaseOrderSave = new DefaultTreeNode(new PrivilegeHolder(Privileges.PurchaseOrderSave, "Purchase Order Save"), ProcumentNode);
        TreeNode pharmacyPurchaseOrderFinalize = new DefaultTreeNode(new PrivilegeHolder(Privileges.PurchaseOrderFinalize, "Purchase Order Finalize"), ProcumentNode);
        TreeNode pharmacyGoodRecipt = new DefaultTreeNode(new PrivilegeHolder(Privileges.GoodsRecipt, "Pharmacy Good Recipt"), ProcumentNode);
        TreeNode pharmacyGrnSave = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGrnSave, "Pharmacy GRN Save"), ProcumentNode);
        TreeNode pharmacyGrnFinalize = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGrnFinalize, "Pharmacy GRN Finalize"), ProcumentNode);
        TreeNode pharmacyGrnApprove = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGrnApprove, "Pharmacy GRN Approve"), ProcumentNode);
        TreeNode pharmacyGrnCancel = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGrnCancel, "Pharmacy GRN Cancel"), ProcumentNode);
        TreeNode pharmacyGrnReturnCancel = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGrnReturnCancel, "Pharmacy GRN Return Cancel"), ProcumentNode);
        TreeNode pharmacyReturnReceviedGoods = new DefaultTreeNode(new PrivilegeHolder(Privileges.ReturnReceviedGoods, "Pharmacy Return Recevied Goods"), ProcumentNode);
        TreeNode pharmacyCreateGrnReturn = new DefaultTreeNode(new PrivilegeHolder(Privileges.CreateGrnReturn, "Create GRN Return"), ProcumentNode);
        TreeNode pharmacyFinalizeGrnReturn = new DefaultTreeNode(new PrivilegeHolder(Privileges.FinalizeGrnReturn, "Finalize GRN Return"), ProcumentNode);
        TreeNode pharmacyApproveGrnReturn = new DefaultTreeNode(new PrivilegeHolder(Privileges.ApproveGrnReturn, "Approve GRN Return"), ProcumentNode);
        TreeNode pharmacyPrintOriginalGrnBillFromReprint = new DefaultTreeNode(new PrivilegeHolder(Privileges.PrintOriginalGrnBillFromReprint, "Print Original GRN Bill From Reprint"), ProcumentNode);
        // Direct Purchase Return workflow
        TreeNode pharmacyCreateDirectPurchaseReturn = new DefaultTreeNode(new PrivilegeHolder(Privileges.CreateDirectPurchaseReturn, "Create Direct Purchase Return"), ProcumentNode);
        TreeNode pharmacyFinalizeDirectPurchaseReturn = new DefaultTreeNode(new PrivilegeHolder(Privileges.FinalizeDirectPurchaseReturn, "Finalize Direct Purchase Return"), ProcumentNode);
        TreeNode pharmacyApproveDirectPurchaseReturn = new DefaultTreeNode(new PrivilegeHolder(Privileges.ApproveDirectPurchaseReturn, "Approve Direct Purchase Return"), ProcumentNode);
        TreeNode pharmacyReturnWithoutRecipt = new DefaultTreeNode(new PrivilegeHolder(Privileges.ReturnWithoutRecipt, "Pharmacy Return WIthout Recipt"), ProcumentNode);
        TreeNode pharmacyReturnWithoutReceiptBill = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyReturnWithoutReceiptBill, "Pharmacy Return Without Receipt Bill"), ProcumentNode);
        TreeNode pharmacyOrderCancellation = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyOrderCancellation, "Pharmacy Order Cancellation"), ProcumentNode);

        TreeNode DealerPayment = new DefaultTreeNode("Pharmacy Dealer Payment", pharmacyNode);
        TreeNode PharmacyDealerPaymentMenue = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDealerPaymentMenue, "Pharmacy Dealer Payment Menue"), DealerPayment);
        TreeNode PharmacyDealerDueSearch = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDealerDueSearch, "Pharmacy Dealer Due Search"), DealerPayment);
        TreeNode PharmacyDealerDueByAge = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDealerDueByAge, "Pharmacy Dealer Due By Age"), DealerPayment);
        TreeNode PharmacyPayment = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyPayment, "Pharmacy Payment"), DealerPayment);
        TreeNode PharmacyGRNPaymentApprove = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGRNPaymentApprove, "Pharmacy GRN Payment Approve"), DealerPayment);
        TreeNode PharmacyGRNPaymentDoneSearch = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGRNPaymentDoneSearch, "Pharmacy GRN Payment Done Search"), DealerPayment);
        TreeNode PharmacyCreditDueAndAccess = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyCreditDueAndAccess, "Pharmacy Credit Du eAnd Access"), DealerPayment);

        TreeNode PharmacyAdjustment = new DefaultTreeNode("Pharmacy Adjustment", pharmacyNode);
        TreeNode pharmacyPharmacyAdjustmentMenue = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdjustmentMenue, "Pharmacy Adjustment Menu"), PharmacyAdjustment);
        TreeNode PharmacyAdjustmentDepartmentStockQTY = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdjustmentDepartmentStockQTY, "Pharmacy Adjustment Department Stock QTY"), PharmacyAdjustment);
        TreeNode PharmacyAdjustmentDepartmentStockBySingleItemQTY = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdjustmentDepartmentStockBySingleItemQTY, "Pharmacy Adjustment Department Stock By Single Item QTY"), PharmacyAdjustment);
        // Place Pharmacy stock adjustment privilege under individual stock adjustment
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyStockAdjustment, "Pharmacy Stock Adjustment"), PharmacyAdjustmentDepartmentStockBySingleItemQTY);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyStockAdjustmentSingleItem, "Pharmacy Stock Adjustment Single Item"), PharmacyAdjustmentDepartmentStockBySingleItemQTY);
        TreeNode PharmacyAdjustmentStaffStockAdjustment = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdjustmentStaffStockAdjustment, "Pharmacy Adjustment Staff Stock Adjustment"), PharmacyAdjustment);
        TreeNode PharmacyAdjustmentPurchaseRate = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdjustmentPurchaseRate, "Pharmacy Adjustment Purchase Rate"), PharmacyAdjustment);
        TreeNode PharmacyAdjustmentCostRate = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdjustmentCostRate, "Pharmacy Adjustment Cost Rate"), PharmacyAdjustment);
        TreeNode PharmacyAdjustmentSaleRate = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdjustmentSaleRate, "Pharmacy Adjustment Sale Rate"), PharmacyAdjustment);
        TreeNode PharmacyAdjustmentWholeSaleRate = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdjustmentWholeSaleRate, "Pharmacy Adjustment Wholesale Rate"), PharmacyAdjustment);
        TreeNode PharmacyAdjustmentExpiaryDate = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdjustmentExpiryDate, "Pharmacy Adjustment Expiary Date"), PharmacyAdjustment);
        TreeNode PharmacyAdjustmentReports = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdjustmentSearchAdjustmentBills, "Pharmacy Adjustment Search Adjustment Bills"), PharmacyAdjustment);
        TreeNode PharmacyPhysicalCountApprove = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyPhysicalCountApprove, "Pharmacy Physical Count Approve"), PharmacyAdjustment);
        // Stock Take approval privilege for new stock take workflow
        TreeNode PharmacyStockTakeApprove = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyStockTakeApprove, "Pharmacy Stock Take Approve"), PharmacyAdjustment);
        // Create New Batch privilege
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdjustmentCreateBatch, "Pharmacy Adjustment Create Batch"), PharmacyAdjustment);
        // Archive Old StockHistory Records (issue #20726)
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ArchiveOldStockHistory, "Archive Old StockHistory Records"), PharmacyAdjustment);
        // Archive Old ItemBatch Records (issue #20724)
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ArchiveOldItemBatch, "Archive Old ItemBatch Records"), PharmacyAdjustment);

        TreeNode pharmacyDisposalNode = new DefaultTreeNode(new PrivilegeHolder(null, "Pharmacy Disposal"), pharmacyNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisposalMenue, "Pharmacy Disposal Menu"), pharmacyDisposalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisposalIssue, "Pharmacy Disposal Issue"), pharmacyDisposalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisposalIssueFinalize, "Pharmacy Disposal Issue Finalize"), pharmacyDisposalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisposalIssueApprove, "Pharmacy Disposal Issue Approve"), pharmacyDisposalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisposalIssueCancel, "Pharmacy Disposal Issue Cancel"), pharmacyDisposalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDiscardCategoryManage, "Pharmacy Issue Category Manage"), pharmacyDisposalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisposalSearchIssueBill, "Pharmacy Disposal Search Issue Bill"), pharmacyDisposalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisposalSearchIssueBillItems, "Pharmacy Disposal Search Issue Bill Items"), pharmacyDisposalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisposalSearchIssueReturnBill, "Pharmacy Disposal Search Issue Return Bill"), pharmacyDisposalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisposalUnitIssueMargin, "Pharmacy Disposal Unit Issue Margin"), pharmacyDisposalNode);
        // Disposal returns
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CreateDisposalReturn, "Create Disposal Return"), pharmacyDisposalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.FinalizeDisposalReturn, "Finalize Disposal Return"), pharmacyDisposalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ApproveDisposalReturn, "Approve Disposal Return"), pharmacyDisposalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ViewDisposalReturn, "View Disposal Return"), pharmacyDisposalNode);

        // Adding Optician node and subnodes
        TreeNode opticianNode = new DefaultTreeNode(new PrivilegeHolder(null, "Optician"), allNode);
        // Optician is the menu-level marker; Ophthalmology* are @Deprecated and intentionally omitted
        new DefaultTreeNode(new PrivilegeHolder(Privileges.Optician, "Optician Menu"), opticianNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.OpticianPatientManagement, "Patient Management"), opticianNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.OpticianAppointmentManagement, "Appointment Management"), opticianNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.OpticianEmr, "EMR"), opticianNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.OpticianStockManagement, "Stock Management"), opticianNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.OpticianProductCatalog, "Product Catalog"), opticianNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.OpticianRepairManagement, "Repair Management"), opticianNode);

        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyItemSearch, "Item Search"), pharmacyNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGenarateReports, "Generate Reports"), pharmacyNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySummaryViews, "Summary Views"), pharmacyNode);
        // Retail transaction extras
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyRetailTransaction, "Pharmacy Retail Transaction"), retailTransaction);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleReturn, "Pharmacy Sale Return"), retailTransaction);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleWithoutStock, "Pharmacy Sale Without Stock"), retailTransaction);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyReturnWithoutTraising, "Pharmacy Return Without Traising"), retailTransaction);
        // Wholesale extras
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyWholeSaleTransaction, "Pharmacy Wholesale Transaction"), PharmacyWholeSaleTransAction);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyWholesaleMenue, "Pharmacy Wholesale Menu"), PharmacyWholeSaleTransAction);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleWh, "Pharmacy Sale Wholesale"), PharmacyWholeSaleTransAction);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleReprintWh, "Pharmacy Sale Reprint Wholesale"), PharmacyWholeSaleTransAction);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleCancelWh, "Pharmacy Sale Cancel Wholesale"), PharmacyWholeSaleTransAction);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleReturnWh, "Pharmacy Sale Return Wholesale"), PharmacyWholeSaleTransAction);
        // Procurement extras
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGoodReceive, "Pharmacy Good Receive"), ProcumentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGoodReceiveWh, "Pharmacy Good Receive Wholesale"), ProcumentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGoodReceiveCancel, "Pharmacy Good Receive Cancel"), ProcumentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGoodReceiveReturn, "Pharmacy Good Receive Return"), ProcumentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGoodReceiveEdit, "Pharmacy Good Receive Edit"), ProcumentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyPurchase, "Pharmacy Purchase"), ProcumentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyPurchaseWh, "Pharmacy Purchase Wholesale"), ProcumentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyPurchaseReprint, "Pharmacy Purchase Reprint"), ProcumentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyPurchaseCancellation, "Pharmacy Purchase Cancellation"), ProcumentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyPurchaseReturn, "Pharmacy Purchase Return"), ProcumentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PrintOriginalPoBillFromReprint, "Print Original PO Bill From Reprint"), ProcumentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyOrderCreation, "Pharmacy Order Creation"), ProcumentNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyOrderApproval, "Pharmacy Order Approval"), ProcumentNode);
        // Dealer payment extras
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDealorPayment, "Pharmacy Dealer Payment"), DealerPayment);
        // Adjustment extras
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdjustmentTransferAllStock, "Transfer All Stock"), PharmacyAdjustment);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyReAddToStock, "Pharmacy Re-Add to Stock"), PharmacyAdjustment);
        // Inpatient medication extras
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyBHTIssueAccept, "Pharmacy BHT Issue Accept"), InpatientMedicationManagementNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyInwardBilling, "Pharmacy Inward Billing"), InpatientMedicationManagementNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyInwardBillingCancel, "Pharmacy Inward Billing Cancel"), InpatientMedicationManagementNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyInwardBillingReturn, "Pharmacy Inward Billing Return"), InpatientMedicationManagementNode);
        // General pharmacy extras
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyStockIssue, "Pharmacy Stock Issue"), pharmacyNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySearch, "Pharmacy Search"), pharmacyNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyReports, "Pharmacy Reports"), pharmacyNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyTransfer, "Pharmacy Transfer"), pharmacyNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySummery, "Pharmacy Summary"), pharmacyNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySetReorderLevel, "Pharmacy Set Reorder Level"), pharmacyNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleWithoutStock, "Pharmacy Sale Without Stock (Legacy)"), pharmacyNode);

        // Request Privileges
        TreeNode requestNode = new DefaultTreeNode(new PrivilegeHolder(null, "Request Manage"), allNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.RequestManager, "Request Manager"), requestNode);
        TreeNode billCancelRequestApproval = new DefaultTreeNode(new PrivilegeHolder(Privileges.BillCancelRequestApproval, "Bill Cancel Approval"), requestNode);
        TreeNode itemRefundRequestApproval = new DefaultTreeNode(new PrivilegeHolder(Privileges.ItemRefundRequestApproval, "Item Refund Approval"), requestNode);
        TreeNode drawerAdjustmentRequestApproval = new DefaultTreeNode(new PrivilegeHolder(Privileges.DrawerAdjustmentRequestApproval, "Drawer Adjustment Approval"), requestNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.DrawerAdjustmentDirect, "Drawer Adjustment Direct (No Approval)"), requestNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PettyCashCancellationApproval, "Petty-Cash Cancellation Approval"), requestNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyRetailSaleReturnApproval, "Pharmacy Retail Sale Return Approval"), requestNode);

        // Float Transfer Privileges
        TreeNode floatTransferNode = new DefaultTreeNode(new PrivilegeHolder(null, "Float Transfer"), allNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.IssueFundTransfer, "Issue Float Transfer"), floatTransferNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReceiveFundTransfer, "Receive Float Transfer"), floatTransferNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.DeclineFundTransfer, "Decline Float Transfer"), floatTransferNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.RequestFundTransfer, "Request Float Transfer"), floatTransferNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ProcessFundTransferRequest, "Process Float Transfer Request"), floatTransferNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CancelOwnFundTransfer, "Cancel Own Float Transfer"), floatTransferNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.CancelOthersFundTransfer, "Cancel Others Float Transfer"), floatTransferNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ViewFundTransferReports, "View Float Transfer Reports"), floatTransferNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ViewAllShiftShortageBills, "View All Shift Shortage Bills"), floatTransferNode);

        // Request Privileges
        TreeNode nurseNode = new DefaultTreeNode(new PrivilegeHolder(null, "Nursing Work Bench"), allNode);
        TreeNode nursingWorkBench = new DefaultTreeNode(new PrivilegeHolder(Privileges.NursingWorkBench, "Nursing Work Bench"), nurseNode);
        TreeNode showDrugCharges = new DefaultTreeNode(new PrivilegeHolder(Privileges.ShowDrugCharges, "Show Drug Charges"), nurseNode);
        TreeNode ShowServiceCharges = new DefaultTreeNode(new PrivilegeHolder(Privileges.ShowServiceCharges, "Show Service Charges"), nurseNode);
        TreeNode ShowTimeServiceCharges = new DefaultTreeNode(new PrivilegeHolder(Privileges.ShowTimeServiceCharges, "Show Time Service Charges"), nurseNode);

        TreeNode nursingWorkBenchPanelsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Nursing Workbench Panels"), nurseNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.NursingWorkBenchPanelEdit, "Edit Panel"), nursingWorkBenchPanelsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.NursingWorkBenchPanelClinicalData, "Clinical Data Panel"), nursingWorkBenchPanelsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.NursingWorkBenchPanelRoomManagement, "Room Management Panel"), nursingWorkBenchPanelsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.NursingWorkBenchPanelService, "Service Panel"), nursingWorkBenchPanelsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.NursingWorkBenchPanelOperationTheatre, "Operation Theatre Panel"), nursingWorkBenchPanelsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.NursingWorkBenchPanelPharmaceuticals, "Pharmaceuticals Panel"), nursingWorkBenchPanelsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.NursingWorkBenchPanelReports, "Reports Panel"), nursingWorkBenchPanelsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.NursingWorkBenchPanelPayments, "Payments Panel"), nursingWorkBenchPanelsNode);

        // Admin Privileges
        TreeNode superAdminNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.SuperAdmin, "Super Admin"), allNode);
        TreeNode editData = new DefaultTreeNode(new PrivilegeHolder(Privileges.EditData, "Edit Data"), superAdminNode);
        TreeNode reActivate = new DefaultTreeNode(new PrivilegeHolder(Privileges.Reactivate, "Reactivate"), superAdminNode);
        TreeNode deleteData = new DefaultTreeNode(new PrivilegeHolder(Privileges.DeleteData, "Delete Data"), superAdminNode);
        TreeNode billCancel = new DefaultTreeNode(new PrivilegeHolder(Privileges.BillCancel, "Bill Cancel "), superAdminNode);
        TreeNode billRefund = new DefaultTreeNode(new PrivilegeHolder(Privileges.BillRefund, "Bill Refund"), superAdminNode);

        // Pharmacy Analytics Reports - individual report privileges
        TreeNode pharmacyAnalyticsReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Pharmacy Analytics Reports"), allNode);
        TreeNode pharmacyAnalyticsAdjustmentReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Adjustment Reports"), pharmacyAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsItemWiseAdjustments, "Item-wise adjustments"), pharmacyAnalyticsAdjustmentReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsExpiryAdjustments, "Expiry adjustments"), pharmacyAnalyticsAdjustmentReportsNode);
        TreeNode pharmacyAnalyticsConsumptionReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Consumption Reports"), pharmacyAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsUnitIssueByBill, "Unit Issue by bill"), pharmacyAnalyticsConsumptionReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsUnitIssueByDepartment, "Unit Issue by Department"), pharmacyAnalyticsConsumptionReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsUnitIssueByItemBatch, "Unit Issue by Item (Batch)"), pharmacyAnalyticsConsumptionReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsUnitIssueByItem, "Unit Issue by Item"), pharmacyAnalyticsConsumptionReportsNode);
        TreeNode pharmacyAnalyticsDisbursementReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Disbursement Reports"), pharmacyAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsTransferIssueByBillItem, "Transfer Issue By Bill Item"), pharmacyAnalyticsDisbursementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsTransferIssueByBill, "Transfer Issue by Bill"), pharmacyAnalyticsDisbursementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsTransferIssueSummary, "Transfer Issue Summary"), pharmacyAnalyticsDisbursementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsTransferReceiveByBillItem, "Transfer Receive By Bill Item"), pharmacyAnalyticsDisbursementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsTransferReceiveByBill, "Transfer Receive by Bill"), pharmacyAnalyticsDisbursementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsTransferReceiveSummary, "Transfer Receive Summary"), pharmacyAnalyticsDisbursementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsReportTransferIssuedNotRecieved, "Report Transfer Issued not Recieved"), pharmacyAnalyticsDisbursementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsStaffStockReport, "Staff Stock Report"), pharmacyAnalyticsDisbursementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsTransferReportSummary, "Transfer Report Summary"), pharmacyAnalyticsDisbursementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsTransferIssueSummaryReportByDate, "Transfer Issue Summary Report By Date"), pharmacyAnalyticsDisbursementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsTransferReceiveVsBHTIssueQuntityTotalsByItem, "Transfer Receive Vs BHT Issue Quntity Totals By Item"), pharmacyAnalyticsDisbursementReportsNode);
        TreeNode pharmacyAnalyticsFinancialReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Financial Reports"), pharmacyAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsCashInOutReport, "Cash In/Out Report"), pharmacyAnalyticsFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsCashierReport, "Cashier Report"), pharmacyAnalyticsFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsCashierSummary, "Cashier Summary"), pharmacyAnalyticsFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsAllCashierReport, "All Cashier Report"), pharmacyAnalyticsFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsAllCashierSummary, "All Cashier Summary"), pharmacyAnalyticsFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsCashierDetailedReportByDepartment, "Cashier Detailed Report by Department"), pharmacyAnalyticsFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPharmacySaleSummary, "Pharmacy Sale Summary"), pharmacyAnalyticsFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPharmacySaleSummaryDate, "Pharmacy Sale Summary Date"), pharmacyAnalyticsFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsAllDepartmentSaleSummary, "All Department Sale Summary"), pharmacyAnalyticsFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsSaleSummaryByBillType, "Sale Summary - By Bill Type"), pharmacyAnalyticsFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsSaleSummaryByPaymentMethod, "Sale Summary - By Payment Method"), pharmacyAnalyticsFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsSaleSummaryByPaymentMethodByBill, "Sale Summary - By Payment Method (By Bill)"), pharmacyAnalyticsFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsStockOverviewReport, "Stock Overview Report"), pharmacyAnalyticsFinancialReportsNode);
        TreeNode pharmacyAnalyticsInpatientReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Inpatient Reports"), pharmacyAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsBHTIssueByBill, "BHT Issue - By Bill"), pharmacyAnalyticsInpatientReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsBHTIssueByBillItem, "BHT Issue - By Bill Item"), pharmacyAnalyticsInpatientReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsBHTIssueByItem, "BHT Issue - By Item"), pharmacyAnalyticsInpatientReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsBHTIssueStaff, "BHT Issue - Staff"), pharmacyAnalyticsInpatientReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsBHTIssueWithMarginReport, "BHT Issue With Margin Report"), pharmacyAnalyticsInpatientReportsNode);
        TreeNode pharmacyAnalyticsItemReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Item Reports"), pharmacyAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsBinCard, "Bin Card"), pharmacyAnalyticsItemReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsItemBinCard, "Item Bin Card"), pharmacyAnalyticsItemReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsBatchBinCard, "Batch Bin Card"), pharmacyAnalyticsItemReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsItemsAMPList, "Items (AMP) List"), pharmacyAnalyticsItemReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsMedicineVTMATMVMPAMPVMPPAndAMPPList, "Medicine (VTM,ATM,VMP,AMP,VMPP and AMPP) List"), pharmacyAnalyticsItemReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsSingleItemSummary, "Single Item Summary"), pharmacyAnalyticsItemReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsAllItemsSummary, "All Items Summary"), pharmacyAnalyticsItemReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsItemsWithoutDistributor, "Items Without Distributor"), pharmacyAnalyticsItemReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsItemsWithSuppliersAndPrices, "Items With Suppliers and Prices"), pharmacyAnalyticsItemReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsItemsWithDistributor, "Items With Distributor"), pharmacyAnalyticsItemReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsItemsWithMultipleDistributorItemsOnly, "Items With Multiple Distributor(Items Only)"), pharmacyAnalyticsItemReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsItemWithMultipleDistributor, "Item With Multiple Distributor"), pharmacyAnalyticsItemReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsROLAndROQManagement, "ROL and ROQ Management"), pharmacyAnalyticsItemReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsReorderAnalysis, "Reorder Analysis"), pharmacyAnalyticsItemReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsMovementReportStockByDate, "Movement Report Stock By Date"), pharmacyAnalyticsItemReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsMovementReportStockByDateByBatch, "Movement Report Stock By Date - By Batch"), pharmacyAnalyticsItemReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPharmacyAllReport, "Pharmacy All Report"), pharmacyAnalyticsItemReportsNode);
        TreeNode pharmacyAnalyticsMovementReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Movement Reports"), pharmacyAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsMovementOutBySaleIssueAndConsumptionWithCurrentStockReport, "Movement Out by Sale, Issue, and Consumption with Current Stock Report"), pharmacyAnalyticsMovementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsStockMovementTimelineGraphical, "Stock Movement Timeline (Graphical)"), pharmacyAnalyticsMovementReportsNode);
        TreeNode pharmacyAnalyticsOrderingNode = new DefaultTreeNode(new PrivilegeHolder(null, "Ordering"), pharmacyAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsOrderingRequirementReport, "Ordering Requirement Report"), pharmacyAnalyticsOrderingNode);
        TreeNode pharmacyAnalyticsProcurementReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Procurement Reports"), pharmacyAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPharmacyProcurementReport, "Pharmacy Procurement Report"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPharmacyDirectPurchaseReport, "Pharmacy Direct purchase Report"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsGRNSummary, "GRN Summary"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsDepartmentStockByBatch, "Department Stock By Batch"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPurchaseOrdersNotApproved, "Purchase Orders Not Approved"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsDepartmentStockByBatchToUpload, "Department Stock By Batch to Upload"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsItemWiseProcurement, "Item-wise Procurement"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPurcharseBillWithSupplier, "Purcharse Bill with Supplier"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPharmacyGRNReport, "Pharmacy GRN Report"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPharmacyGRNAndPurchaseReport, "Pharmacy GRN and purchase Report"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsGRNPurchaseItemsBySupplier, "GRN Purchase Items by Supplier"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsGRNSummaryBySupplier, "GRN Summary By Supplier"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsGRNBillItemReport, "GRN Bill Item Report"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsGRNRegistry, "GRN Registry"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsGRNReturnList, "GRN Return List"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPurchaseOrderSummary, "Purchase Order Summary"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPurchaseBillsByDepartment, "Purchase Bills by Department"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPurchaseSummaryBySupplier, "Purchase Summary By Supplier"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPurchaseSummaryCreditCash, "Purchase Summary (Credit / Cash )"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPurchaseAndGRNSummaryCreditCash, "Purchase and GRN Summary (Credit / Cash )"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPurchaseSummaryBySupplierCreditCash, "Purchase Summary By Supplier (Credit / Cash)"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsGRNPaymentSummary, "GRN Payment Summary"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsGRNPaymentSummaryBySupplier, "GRN Payment Summary By Supplier"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPharmacyReturnWithoutTraising, "Pharmacy Return Without Traising"), pharmacyAnalyticsProcurementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsProcurementBillItemList, "Procurement Bill Item List"), pharmacyAnalyticsProcurementReportsNode);
        TreeNode pharmacyAnalyticsRetailSaleReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Retail Sale Reports"), pharmacyAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsSaleReport, "Sale Report"), pharmacyAnalyticsRetailSaleReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPrescriptionReport, "Prescription Report"), pharmacyAnalyticsRetailSaleReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsInstitutionItemMovement, "Institution Item Movement"), pharmacyAnalyticsRetailSaleReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsFastMoving, "Fast Moving"), pharmacyAnalyticsRetailSaleReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsSlowMoving, "Slow Moving"), pharmacyAnalyticsRetailSaleReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsNonMoving, "Non Moving"), pharmacyAnalyticsRetailSaleReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPrescriptionSummary, "Prescription Summary"), pharmacyAnalyticsRetailSaleReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPresciptionList, "Presciption List"), pharmacyAnalyticsRetailSaleReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsListOfPharmacyBills, "List of Pharmacy Bills"), pharmacyAnalyticsRetailSaleReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsRetailSaleBillList, "Retail Sale Bill List"), pharmacyAnalyticsRetailSaleReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsSaleDetailByBill, "Sale Detail - By Bill"), pharmacyAnalyticsRetailSaleReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsSaleDetailByBillItems, "Sale Detail - By Bill Items"), pharmacyAnalyticsRetailSaleReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsSaleDetailByDiscountScheme, "Sale Detail - By Discount Scheme"), pharmacyAnalyticsRetailSaleReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsSaleSummaryByDiscountSchemeSummary, "Sale Summary By Discount Scheme Summary"), pharmacyAnalyticsRetailSaleReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsSaleDetailByPaymentMethod, "Sale Detail - By Payment Method"), pharmacyAnalyticsRetailSaleReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPharmacySaleReport, "Pharmacy Sale Report"), pharmacyAnalyticsRetailSaleReportsNode);
        TreeNode pharmacyAnalyticsStockReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Stock Reports"), pharmacyAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsBatchStock, "Batch Stock"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsItemStock, "Item Stock"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsExpiringStock, "Expiring Stock"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsShortExpiryByAMPPeriod, "Short Expiry (by AMP Period)"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsStaffStock, "Staff Stock"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsZeroStockItemReport, "Zero Stock Item Report"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsSuppliersExpiringStocks, "Suppliers Expiring Stocks"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsStockReportByItem, "Stock Report by Item"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsStockReportByItemOrderByVMP, "Stock Report by Item - Order by VMP"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsStockReportByProduct, "Stock Report by Product"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsStockReportOfSingleProduct, "Stock Report of Single Product"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsSupplierStockReport, "Supplier Stock Report"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsSuppliersStockSummary, "Suppliers Stock Summary"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsCategoryStockReport, "Category Stock Report"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsCategoryStockSummary, "Category Stock Summary"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsStockHistory, "Stock History"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsBeforeStockTakingReport, "Before Stock Taking Report"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsAfterStockTakingReport, "After Stock Taking Report"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsStockTakingReportNew, "Stock Taking Report(New)"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsStockWithMovement, "Stock With Movement"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsDepartmentViceStock, "Department Vice Stock"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsStockSummaryWithSuppliers, "Stock Summary (with Suppliers)"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsStockReportWithSuppliers, "Stock Report (with Suppliers)"), pharmacyAnalyticsStockReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsStockReportByBatchForExport, "Stock Report by Batch for Export"), pharmacyAnalyticsStockReportsNode);
        TreeNode pharmacyAnalyticsSummaryReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Summary Reports"), pharmacyAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPharmacyIncomeReport, "Pharmacy Income Report"), pharmacyAnalyticsSummaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsIncomeSummaryCategory, "Income Summary Category"), pharmacyAnalyticsSummaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPharmacyIncomeAndCost, "Pharmacy Income and Cost"), pharmacyAnalyticsSummaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsDailyStockValuesF15, "Daily Stock Values (F-15)"), pharmacyAnalyticsSummaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsF15DrillDownLevel1, "F-15 Drill-Down (Level 1)"), pharmacyAnalyticsSummaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsF9B, "F 9B"), pharmacyAnalyticsSummaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsBillTypes, "Bill Types"), pharmacyAnalyticsSummaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsAllItemMovementSummary, "All Item Movement Summary"), pharmacyAnalyticsSummaryReportsNode);
        TreeNode pharmacyAnalyticsWholesaleReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Wholesale Reports"), pharmacyAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPharmacyWholesaleReport, "Pharmacy Wholesale Report"), pharmacyAnalyticsWholesaleReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAnalyticsPharmacyWholesaleCreditBills, "Pharmacy Wholesale Credit Bills"), pharmacyAnalyticsWholesaleReportsNode);

        // Lab Analytics Reports - individual report privileges
        TreeNode labAnalyticsReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Lab Analytics Reports"), allNode);
        TreeNode labAnalyticsAuditingNode = new DefaultTreeNode(new PrivilegeHolder(null, "Auditing"), labAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsCancelledBillSearch, "Bills Cancelled after Approving Reports"), labAnalyticsAuditingNode);
        TreeNode labAnalyticsClinicalNode = new DefaultTreeNode(new PrivilegeHolder(null, "Clinical"), labAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsTestResultsSingle, "Test Results - Single"), labAnalyticsClinicalNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsTestResults, "Test Results"), labAnalyticsClinicalNode);
        TreeNode labAnalyticsCollectingCentresNode = new DefaultTreeNode(new PrivilegeHolder(null, "Collecting Centres"), labAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsCollectionCentreDetail, "Report by Collection Centre(Detail)"), labAnalyticsCollectingCentresNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsCollectionCentreSummary, "Report by Collection Centre(Summary)"), labAnalyticsCollectingCentresNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsCollectionCentreCount, "Report by Collection Centre Count"), labAnalyticsCollectingCentresNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsCollectionCentreCountSummary, "Report by Collection Centre Count(Summary)"), labAnalyticsCollectingCentresNode);
        TreeNode labAnalyticsIncomeNode = new DefaultTreeNode(new PrivilegeHolder(null, "Income"), labAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsIncomeSummary, "Income Summary"), labAnalyticsIncomeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsReportSummaryDepartment, "Report Summary Department"), labAnalyticsIncomeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsReportSummaryByDay, "Report Summary by day"), labAnalyticsIncomeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsInvestigationSummaryFeeType, "Investigation Summary Fee Type"), labAnalyticsIncomeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsInvestigationSummaryRegentFee, "Investigation Summary Regent Fee"), labAnalyticsIncomeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsInvestigationSummaryFeeTypeWithCredit, "Investigation Summary Fee Type With Credit"), labAnalyticsIncomeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsInvestigationSummaryRegentFeeWithCredit, "Investigation Summary Regent Fee With Credit"), labAnalyticsIncomeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsInvestigationSummaryRegentFeeByPayMethod, "Investigation Summary Regent Fee By Payment Method"), labAnalyticsIncomeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsDailyLabSummaryByDepartment, "#{configOptionApplicationController.getLongTextValueByKey('Daily Lab Summmary By Department Report Menu Name','Daily Lab Summmary By Department')}"), labAnalyticsIncomeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsDailyLabSummaryByDepartmentDto, "#{configOptionApplicationController.getLongTextValueByKey('Daily Lab Summmary By Department Report Menu Name','Daily Lab Summmary By Department')} (DTO)"), labAnalyticsIncomeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsCardIncomeReport, "Laboratary Card Income Report"), labAnalyticsIncomeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsDailyOpdFeeSummary, "Daily OPD Fee Summary"), labAnalyticsIncomeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsDailyOpdFeeSummaryWithCounts, "Daily OPD Fee Summary with Counts"), labAnalyticsIncomeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsDailyInwardFeeSummary, "Daily Inward Fee Summary"), labAnalyticsIncomeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsDailyInwardFeeSummaryWithCounts, "Daily Inward Fee Summary with Counts"), labAnalyticsIncomeNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsReportSummaryByMonthCashCredit, "Report Summary by Month With Cash and Credit"), labAnalyticsIncomeNode);
        TreeNode labAnalyticsInstitutionsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Institutions"), labAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsByOrderingInstitution, "By Ordering Institution"), labAnalyticsInstitutionsNode);
        TreeNode labAnalyticsInwardNode = new DefaultTreeNode(new PrivilegeHolder(null, "Inward"), labAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsInwardSummaryByAddedDate, "Inward Lab Summary by Added Date"), labAnalyticsInwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsInwardSummaryByAddedDateWithMargin, "Inward Lab Summary by Added Date With Margin"), labAnalyticsInwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsInvestigationSummaryInward, "Investigation Summary Inward"), labAnalyticsInwardNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsInvestigationSummaryInwardByDate, "Investigation Summary Inward by Date"), labAnalyticsInwardNode);
        TreeNode labAnalyticsLabSummaryNode = new DefaultTreeNode(new PrivilegeHolder(null, "Lab Summary"), labAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsTestWiseCountReport, "Test Wise Count Report"), labAnalyticsLabSummaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsTestWiseCountReportDto, "Test Wise Count Report - DTO"), labAnalyticsLabSummaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsTestWiseReagentCostReport, "Test Wise Reagent Cost Report"), labAnalyticsLabSummaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsIncomeReport, "Laboratary Income Report"), labAnalyticsLabSummaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsOrderReport, "Laboratory Order Report"), labAnalyticsLabSummaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsLaboratorySummary, "Laboratory Summary"), labAnalyticsLabSummaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsDailySummaryByBillTypes, "Daily Summary By Bill Types"), labAnalyticsLabSummaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsDailySummary, "Daily Summary"), labAnalyticsLabSummaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsDailySummaryInwardOpd, "Daily Summary Inward and Opd"), labAnalyticsLabSummaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsDailySummaryInwardOpdByDate, "Daily Summary Inward and Opd by Date"), labAnalyticsLabSummaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsDailySummaryInwardOpdCount, "Daily Summary Inward and Opd Count"), labAnalyticsLabSummaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsAllIncomeSummary, "Laboratory All Income Summary"), labAnalyticsLabSummaryNode);
        TreeNode labAnalyticsPerformanceNode = new DefaultTreeNode(new PrivilegeHolder(null, "Performance"), labAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsInvestigationList, "Investigation List"), labAnalyticsPerformanceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsBillList, "Bill List"), labAnalyticsPerformanceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsBillItemList, "Bill Item List"), labAnalyticsPerformanceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsClientList, "Client List"), labAnalyticsPerformanceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsSampleList, "Sample List"), labAnalyticsPerformanceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsSampleListDto, "Sample List (DTO)"), labAnalyticsPerformanceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsAvgTurnAroundTime, "Average Turn Around Time"), labAnalyticsPerformanceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsBillWiseTurnAroundTime, "Bill-vice turn-around time"), labAnalyticsPerformanceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsByBilledInstitution, "By Billed Institution"), labAnalyticsPerformanceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsByBilledDepartment, "By Billed Department"), labAnalyticsPerformanceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsByReportedInstitution, "By Reported Institution"), labAnalyticsPerformanceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsByReportedDepartment, "By Reported Department"), labAnalyticsPerformanceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsOpdBillItemsForCreditCompanies, "OPD Bill Items For Credit Companies"), labAnalyticsPerformanceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsCancelledLabBillList, "List of Cancelled Lab Bills"), labAnalyticsPerformanceNode);
        TreeNode labAnalyticsReferenceNode = new DefaultTreeNode(new PrivilegeHolder(null, "Reference"), labAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsPriceList, "Price List"), labAnalyticsReferenceNode);
        TreeNode labAnalyticsReferringDoctorsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Referring Doctors"), labAnalyticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsReferringDoctorDetail, "Report by Referring Doctor(Details)"), labAnalyticsReferringDoctorsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAnalyticsReferringDoctorSummary, "Report by Referring Doctor(Summary)"), labAnalyticsReferringDoctorsNode);

        // Reports Page Reports - individual report privileges
        TreeNode reportsPageReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Reports Page Reports"), allNode);
        TreeNode reportsPageAdministrationNode = new DefaultTreeNode(new PrivilegeHolder(null, "Administration"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDepartmentReports, "1. Department Report"), reportsPageAdministrationNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsEmployeeDetails, "2. Employee Detail"), reportsPageAdministrationNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsEmployeeToRetired, "3. Employee To Retired Details"), reportsPageAdministrationNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsEmployeeEndofProbation, "4. Employee End of Probation"), reportsPageAdministrationNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffDetail, "5. Staff Detail"), reportsPageAdministrationNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsHolidayReport, "6. Holiday Report"), reportsPageAdministrationNode);
        TreeNode reportsPageAssetReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Asset Reports"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAssetRegister, "1. Asset Register"), reportsPageAssetReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsPoStatusReport, "2. PO Status Report"), reportsPageAssetReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsEmployeeAssetIssue, "3. Employee Asset Issue"), reportsPageAssetReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsFixedAssetIssue, "4. Fixed Asset Issue"), reportsPageAssetReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAssetWarentyExpireReport, "5. Asset Warranty Expire Report"), reportsPageAssetReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAssetGrnReport, "6. Asset GRN Report"), reportsPageAssetReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAssetTransferReport, "7. Asset Transfer Report"), reportsPageAssetReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsItemLoacationHistory, "8. Item Loacation History"), reportsPageAssetReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAssetAmcExpiryReport, "9. Asset AMC Expiry"), reportsPageAssetReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAssetWarrantyExpiry, "10. Asset Warranty Expiry"), reportsPageAssetReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAssetAmcReport, "11. AMC"), reportsPageAssetReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsWorkOrderReport, "12. Work Order"), reportsPageAssetReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsPretentiveMaintainanceReport, "13. Preventive Maintenance"), reportsPageAssetReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsModalityDowntime, "14. Modality Down Time"), reportsPageAssetReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAssetDisposalReportSaleDisposalWriteOff, "15. Asset Disposal Report (Sale Disposal, Write-off)"), reportsPageAssetReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsPurchaseRateMovement, "16. Purchase Rate movement"), reportsPageAssetReportsNode);
        TreeNode reportsPageAttendanceNode = new DefaultTreeNode(new PrivilegeHolder(null, "Attendance"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAttendanceReport, "1. Attandance Report"), reportsPageAttendanceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsLateInAndEarlyOut, "2. Late In and Early Out"), reportsPageAttendanceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffShiftDetailsByStaff, "3. Staff Shift Detail By Report"), reportsPageAttendanceNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsVerifiedReport, "4. Verified Report"), reportsPageAttendanceNode);
        TreeNode reportsPageCafeAndKitchenReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Cafe and Kitchen Reports"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCafeDiscount, "1. Café Discount"), reportsPageCafeAndKitchenReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCafeSale, "2. Café Sale"), reportsPageCafeAndKitchenReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCafeExpiry, "3. Café Expiry"), reportsPageCafeAndKitchenReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCafeConsumption, "4. Café Consumption"), reportsPageCafeAndKitchenReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCafeInwardPatientSale, "5. Cafe Inward Patient Sale"), reportsPageCafeAndKitchenReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsInwardService, "6. Inward Service"), reportsPageCafeAndKitchenReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsTheatreService, "7. Theatre Service"), reportsPageCafeAndKitchenReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsBillExpenses, "8. Bill Expenses"), reportsPageCafeAndKitchenReportsNode);
        TreeNode reportsPageCashierReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Cashier Reports"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsTotalCashierSummary, "Total Cashier Summary"), reportsPageCashierReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAllCashierSummary, "1. All Cashier Summary"), reportsPageCashierReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCashierSummary, "2. Cashier Summary"), reportsPageCashierReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCashierDetails, "3. Cashier Details"), reportsPageCashierReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsListAllDrawers, "4. All Drawers"), reportsPageCashierReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAllCashierHandovers, "5. Shifts"), reportsPageCashierReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsHandoverStatusReport, "6. Handovers"), reportsPageCashierReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsShiftEndCash, "7. Shift End Cash"), reportsPageCashierReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsActiveShiftsReport, "8. Active Shifts"), reportsPageCashierReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsIouConversionBillReport, "9. IOU Conversion Bills"), reportsPageCashierReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsIouConversionPaymentReport, "10. IOU Conversion Payments"), reportsPageCashierReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsShiftStartAndEnd, "12. Shift End Summary"), reportsPageCashierReportsNode);
        TreeNode reportsPageCollectionCenterReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Collection Center Reports"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCourierLabReportsPrint, "Courier Lab Reports Print"), reportsPageCollectionCenterReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCCReportsPrint, "1. Collection Center Reports Print"), reportsPageCollectionCenterReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCCCurrentBalanceReport, "2. Collection Center Current Balance"), reportsPageCollectionCenterReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCCBalanceReport, "2. Collection Center Balance"), reportsPageCollectionCenterReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCCReceiptReport, "3. Collection Center Receipt"), reportsPageCollectionCenterReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCCBillWiseDetailReport, "4. Collection Center Bill Wise Detail"), reportsPageCollectionCenterReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCCWiseInvoiceListReport, "5. Collection Center Wise Invoice List"), reportsPageCollectionCenterReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCCStatementReport, "6. Collection Center Statement"), reportsPageCollectionCenterReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCCWiseSummaryReport, "7. Collection Center Wise Summary"), reportsPageCollectionCenterReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsTestWiseCountReport, "8. Collection Center Test Wise Count"), reportsPageCollectionCenterReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCCRouteAnalysisReport, "9. Route Analysis"), reportsPageCollectionCenterReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCCBookReport, "10. Collction Centre Book"), reportsPageCollectionCenterReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCCBookWiseDetail, "11. Collction Centre Book Wise Detail"), reportsPageCollectionCenterReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCCInvestigationListReport, "12. Collction Centre Investigation List"), reportsPageCollectionCenterReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCCBillItemListReport, "12. Collction Centre Bill Item List"), reportsPageCollectionCenterReportsNode);
        TreeNode reportsPageDashboardNode = new DefaultTreeNode(new PrivilegeHolder(null, "Dashboard"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDashboard, "Dashboard"), reportsPageDashboardNode);
        TreeNode reportsPageFinancialReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Financial Reports"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDailyReturn, "Daily return"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDailyReturnDto, "Daily return – Fast"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsIncomeBreakdownByCategory, "Income Breakdown by Category"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsBillsByItemCategory, "Bills by Category"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsIpIncomeCategoryWiseReport, "3. IP Income Category Wise"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsServiceCategoryWiseBillDetail, "4. Service Category Wise Bill Details"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsServiceCategoryWiseBillDetailOpd, "4.1. Service Category Wise Bill Details OPD"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsProfessionalFeePayment, "5. Professional Fees Payment"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDiscount, "6. Discount"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsOutsidePayment, "7. Outside Payments"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCollectionCenterWiseIncome, "8. Collection Center Wise Income"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsInvoiceAndReciptReportSerialWise, "9. Invoice and Receipt Report (Serial Wise)"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsPharmacySaleReport, "10. Pharmacy Sale (OP/IP)"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDebtorSettlement, "11. Debtor Settlement"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDebtorBalanceReport, "12. Debtor Balance Report"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsOpdAndInwardDueReport, "13. OPD and Inward Due"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDebtorAgeAnlysis, "14. Debtor Age Anlysis"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCreditInvoiceDispatch, "15. Credit Invoice Dispatch"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsPettyCashPayment, "16. Petty Cash Payment"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsWhtReport, "17. WHT"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsBillWiseItemMovementReport, "18. Bill Wise Item Movement"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDebtorSettlementFinancial, "19. Debtor Settlement"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffWelfareBills, "21. Staff Welfare"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsProfitMatrixReport, "22. Profit Matrix"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsPackageReport, "23. Package Report"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDebtorAnalysis, "24. Debtor Analysis"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDrawerHistory, "25. Drawer History By User"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAllUsersDrawerHistory, "26. Drawer History"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDrawerAdjustments, "26 A. Drawer Adjustments"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDepartmentRevenueReport, "27. Department Revenue"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsPaymentSettlement, "28. Payment Settlement"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDueSearch, "29. Due Search"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDueSearchCreditCompany, "30. Due Search (Credit Company)"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDueAge, "31. Due Age"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDueAgeCreditCompany, "32. Due Age (Credit Company)"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDueAgeDetail, "33. Due Age Detail"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsExcessSearchCreditCompany, "34. Excess Search (Credit Company)"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsExcessAgeCreditCompany, "35. Excess Age (Credit Company)"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsExcessSearch, "36. Excess Search"), reportsPageFinancialReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsExcessAge, "37. Excess Age"), reportsPageFinancialReportsNode);
        TreeNode reportsPageFingerPrintReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Finger print Reports"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsFingerPrintRecordByLogged, "1. Fingerprint Record by Logged"), reportsPageFingerPrintReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsFingerPrintRecordByVerified, "2. Fingerprint Record by Verified"), reportsPageFingerPrintReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsFingerPrintRecordNoShiftSettled, "3. Fingerprint Record by No Shift Settled"), reportsPageFingerPrintReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsFingerPrintApprove, "4. Fingerprint Approve"), reportsPageFingerPrintReportsNode);
        TreeNode reportsPageFormNode = new DefaultTreeNode(new PrivilegeHolder(null, "Form"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsLeaveForm, "1. Leave Form"), reportsPageFormNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAdditionalFormReportVerification, "2. Additinal Form Report Verification"), reportsPageFormNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsOnlineFormStatus, "3. Online Form Status"), reportsPageFormNode);
        TreeNode reportsPageHistoryNode = new DefaultTreeNode(new PrivilegeHolder(null, "History"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffShiftHistory, "1. Staff Shift History"), reportsPageHistoryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsFingerprintHistory, "2. Fingerprint history"), reportsPageHistoryNode);
        TreeNode reportsPageInpatientReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Inpatient Reports"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAdmissionDischargeReport, "1. Admission and Discharge"), reportsPageInpatientReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsIpUnsettledInvoices, "2. IP Unsettled Invoices"), reportsPageInpatientReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsRoomChange, "3. Room Change"), reportsPageInpatientReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAdmissionCategoryWiseAdmission, "4. Admission Category Wise Admission"), reportsPageInpatientReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsIpServiceReport, "5. Service Reports"), reportsPageInpatientReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAdmissionReport, "6. Admission Reports"), reportsPageInpatientReportsNode);
        TreeNode reportsPageInventoryReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Inventory Reports"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsClosingStockReport, "1. Closing Stock"), reportsPageInventoryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsConsumption, "2. Consumption (Legacy)"), reportsPageInventoryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsConsumptionDto, "2. Consumption"), reportsPageInventoryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStockTransferReport, "3. Stock Transfers"), reportsPageInventoryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCostOfGoodsSold, "4. Cost Of Good Sold"), reportsPageInventoryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsGoodInTransit, "5. Good in Transit"), reportsPageInventoryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsGrnReport, "6. GRN Report"), reportsPageInventoryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsBatchWiseStockReport, "7. Batch Wise Stock"), reportsPageInventoryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSlowFastNoneMovement, "8. Slow/Fast/None Movement Report"), reportsPageInventoryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsGrn, "9. GRN"), reportsPageInventoryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsBeforeStockTaking, "10. Before Stock Taking"), reportsPageInventoryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAfterStockTaking, "11. After Stock Taking"), reportsPageInventoryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStockLedgerDto, "12. Stock Ledger"), reportsPageInventoryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStockLedger, "12. Stock Leger (Legacy)"), reportsPageInventoryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsExpiryItem, "13. Expiry Item Report"), reportsPageInventoryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsGrnReturnVarianceReport, "14. GRN Return Variance Report"), reportsPageInventoryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsGrnSummaryReport, "15. GRN Summary Report"), reportsPageInventoryReportsNode);
        TreeNode reportsPageLaborataryReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Laboratary Reports"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsLabReportsTestCount, "Test Count Report"), reportsPageLaborataryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsTestWiseCountReports, "Test Wise Count"), reportsPageLaborataryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsLabBillItemList, "Lab Bill Item List"), reportsPageLaborataryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsTurnAroundTimeDetails, "Turn Around Time Details"), reportsPageLaborataryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsTurnAroundTimeHourly, "Turn Around Time(Hourly)"), reportsPageLaborataryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsLabPeakHourStatistics, "Peak Hour Statistics"), reportsPageLaborataryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSampleCarrierReport, "Sample Carrier"), reportsPageLaborataryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsLabOrganismAntibioticSensitivityReport, "Organism Antibiotic Sensitivity"), reportsPageLaborataryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsLabInvetigationWiseReport, "Investigation Wise Research"), reportsPageLaborataryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAnnualTestStatistics, "Annual Test Statistics"), reportsPageLaborataryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsExternalLaboratoryWorkloadReport, "External Laboratory Workload"), reportsPageLaborataryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsLaboratoryWorkloadReport, "Laboratory Workload"), reportsPageLaborataryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsInvestigationMonthEndSummery, "Investigation Month End Summary"), reportsPageLaborataryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsInvestigationMonthEndDetails, "Investigation Month End Details"), reportsPageLaborataryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsLabRegisterReport, "Lab Register"), reportsPageLaborataryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsCollectionCenterStatement, "Collection center statement"), reportsPageLaborataryReportsNode);
        TreeNode reportsPageLeaveNode = new DefaultTreeNode(new PrivilegeHolder(null, "Leave"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsLeaveReport, "1. Leave Report"), reportsPageLeaveNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsLeaveReportSummery, "2. Leave Report Summary"), reportsPageLeaveNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsLateLeaveDetails, "3. Late Leave(Detail)"), reportsPageLeaveNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsLeaveSummeryReport, "4.Leave Summary Report"), reportsPageLeaveNode);
        TreeNode reportsPageManagementReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Management Reports"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsRoomOccupancyReport, "1. Room Occupancy"), reportsPageManagementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSurgerySurvey, "2. Surgery Survey"), reportsPageManagementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSugeryStatus, "3. Surgery Status"), reportsPageManagementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSurgeryCostEstimation, "4. Surgery Cost Estimation"), reportsPageManagementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDurationServiceReport, "5. Duration Service Report"), reportsPageManagementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsPharmacyDepartmentWiseSaleReport, "6. Pharmacy Department Wise Sale Report"), reportsPageManagementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsManagementAdmissionCountReport, "7. Referring Doctor Wise Revenue"), reportsPageManagementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsReferringDoctorWiseRevenueDto, "7a. Referring Doctor Wise Revenue (DTO - Fast)"), reportsPageManagementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsOtRoomWiseSergeryCount, "8. OT Room Wise Surgery Count"), reportsPageManagementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSurgeryWiseCount, "9. Surgery Count(Surgery Wise)"), reportsPageManagementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSurgeryCountDoctorWise, "10. Surgery Count(Doctor Wise)"), reportsPageManagementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSurgeryCountTypeWise, "11. Surgery Count(Type)"), reportsPageManagementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAdmissionCountConsultationWise, "12. Admission Count(Consultant Wise)"), reportsPageManagementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAdmissionCountPaymentTypeWise, "13. Admission Count(Payment Type Wise)"), reportsPageManagementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsManagementHospitalCensusReport, "14. Hospital Census"), reportsPageManagementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsROOMOCCUPANCY, "15. ROOM OCCUPANCY"), reportsPageManagementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsOpdWeeklyReport, "16. OPD Weekly Report"), reportsPageManagementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSpecialityDoctorWiseIncome, "17. Speciality/Doctor Wise Income Report"), reportsPageManagementReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAllDepartmentSaleReport, "18. All Department Sale Report"), reportsPageManagementReportsNode);
        TreeNode reportsPageProfessionalPaymentReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Professional Payment Reports"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsProfessionalFees, "Professional Fees"), reportsPageProfessionalPaymentReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsProfessionalFeePayments, "Professional Fee Payments"), reportsPageProfessionalPaymentReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsProfessionalPayments, "Professional Payments"), reportsPageProfessionalPaymentReportsNode);
        TreeNode reportsPageQBImportReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "QB Import Reports"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsDailyReturnImportForQbReport, "1. QB Import Reports"), reportsPageQBImportReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsReportQbItemList, "2. Item Import Report"), reportsPageQBImportReportsNode);
        TreeNode reportsPageSalaryReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Salary Reports"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsAllStaffSalarySummary, "1. All Staff Salary Summary"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffPayrollReport, "2. Staff Payroll"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffPayrollAccountant, "3. Staff Payroll(Accountant)"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffPayrollByDepartmentByRoster, "4. Staff Payroll(By Department, By Roster)"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffPayrollSelectedStaff, "5. Staff Payroll(Selected Staff)"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffOverTimeReport, "6. Staff Over Time"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsNopayandSalaryAllowanceReport, "7. No Pay and Salary Allowance Report"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffPaysheetComponentList, "8. Staff Paysheet Component List"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffSalaryBankWise, "9. Staff Salary Bank Wise"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffSalaryPaymentToBank, "10. Staff Salary Payment To Bank"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffSalaryPaymentToBankSlip, "11. Staff Salary Payment To Bank(Slip)"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffSalaryPaymentToBankPayPast, "12. Staff Salary Payment To Bank(Pay Fast)"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffSalaryComponentReport, "13. Staff Salary Component"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffSalaryComponentBankWiseReport, "14. Staff Salary Component(Bank Wise)"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffSalaryComponentBetweenToSalaryCycles, "15. Staff Salary Component(Between To Salary Cycles)"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsEPF, "16. EPF"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsETF, "17. ETF"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsEpfEtfUploadReport, "18. EPF/ETF Upload Report"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffSalaryGenerateOrNotReport, "19. Staff Salary Generate Or Not Report"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffSalaryGenerateOrDeleteDetailReport, "20. Staff Salary Generate Or Delete Detail Report"), reportsPageSalaryReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffGratuity, "21. Staff Garduity"), reportsPageSalaryReportsNode);
        TreeNode reportsPageShiftNode = new DefaultTreeNode(new PrivilegeHolder(null, "Shift"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsStaffShiftReport, "1. Staff Shift Report"), reportsPageShiftNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsEnteredShiftReport, "2. Entered Shift Report"), reportsPageShiftNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsRosterTimeAndVerifyTime, "3. Roaster Table and Verify Time Report"), reportsPageShiftNode);
        TreeNode reportsPageStatisticsReportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Statistics Reports"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsPatientJourney, "1. Patient Journey"), reportsPageStatisticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsPatientLedger, "2. Patient Ledger"), reportsPageStatisticsReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSpecialityWiseDemograhicData, "3. Speciality Wise Demographic Data"), reportsPageStatisticsReportsNode);
        TreeNode reportsPageSummaryNode = new DefaultTreeNode(new PrivilegeHolder(null, "Summary"), reportsPageReportsNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsHeadCountReport, "1. Head Count"), reportsPageSummaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsEmployeeWorkedDayReport, "2. Employee Worked Day Report"), reportsPageSummaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsEmployeeWorkedDayReportSalaryCycle, "3. Employee Worked Day Report(Salary Cycle)"), reportsPageSummaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsMonthendEmployeeWorkingTimeAndOvertime, "4. Month End Employee Working Time + Over Time Report"), reportsPageSummaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsMonthEndEmployeeNoPayReportByMinutes, "5. Month End Employee(No Pay) Report-By Minute"), reportsPageSummaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsMonthEndEmployeeSummery, "6. Month End Employee Summary"), reportsPageSummaryNode);
        new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsFingerAnalysisReportBySalaryCycle, "7. Finger Analysis Report by Salary Cycle"), reportsPageSummaryNode);

        return root;
    }

    public void savePrivileges() {
        if (currentWebUser == null) {
            JsfUtil.addErrorMessage("Please select a user");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        saveWebUserPrivileges();
    }

    public void saveUserRolePrivileges() {
        if (webUserRole == null) {
            JsfUtil.addErrorMessage("Please select a User Role");
            return;
        }
        saveWebUserRolePrivileges();
    }

    public List<PrivilegeHolder> createPrivilegeHolders(List<WebUserPrivilege> ps) {
        List<PrivilegeHolder> phs = new ArrayList<>();
        if (ps == null) {
            return phs;
        }

        for (WebUserPrivilege tmpWup : ps) {
            if (tmpWup.getPrivilege() == null) {
                // Log audit event for null privilege
                try {
                    AuditEvent auditEvent = new AuditEvent();
                    auditEvent.setEventDataTime(new Date());
                    auditEvent.setEventTrigger("NULL_PRIVILEGE_DETECTED");
                    auditEvent.setEntityType("WebUserPrivilege");
                    auditEvent.setObjectId(tmpWup.getId());
                    if (sessionController.getLoggedUser() != null) {
                        auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
                    }
                    if (tmpWup.getWebUser() != null) {
                        auditEvent.setBeforeJson("WebUserPrivilege ID: " + tmpWup.getId() +
                                               ", WebUser: " + tmpWup.getWebUser().getName() +
                                               ", Privilege: null (UNEXPECTED)");
                    } else {
                        auditEvent.setBeforeJson("WebUserPrivilege ID: " + tmpWup.getId() +
                                               ", Privilege: null (UNEXPECTED)");
                    }
                    auditEvent.setAfterJson("Record retired due to null privilege");
                    auditEvent.setEventStatus("WARNING");
                    auditEventService.saveAuditEvent(auditEvent);
                } catch (Exception e) {
                    // Log to console if audit logging fails
                    System.err.println("Failed to log audit event for null privilege: " + e.getMessage());
                }

                tmpWup.setRetired(true);
                tmpWup.setRetiredAt(new Date());
                tmpWup.setRetirer(sessionController.getLoggedUser());
                if (tmpWup.getId() == null) {
                    webUserPrivilegeFacade.create(tmpWup);
                } else {
                    webUserPrivilegeFacade.edit(tmpWup);
                }
                continue;
            }
            PrivilegeHolder ph = new PrivilegeHolder();
            ph.setPrivilege(tmpWup.getPrivilege());
            ph.setName(tmpWup.getPrivilege().getLabel());
            phs.add(ph);
        }
        return phs;
    }

    public List<PrivilegeHolder> createRolePrivilegeHolders(List<WebUserRolePrivilege> ps) {
        List<PrivilegeHolder> phs = new ArrayList<>();
        if (ps == null) {
            return phs;
        }

        for (WebUserRolePrivilege tmpWup : ps) {
            PrivilegeHolder ph = new PrivilegeHolder();
            ph.setPrivilege(tmpWup.getPrivilege());
            ph.setName(tmpWup.getPrivilege().getLabel());
            phs.add(ph);
        }
        return phs;
    }

    public void saveWebUserPrivileges(WebUser u, List<PrivilegeHolder> selected, Department dept) {
        currentWebUser = u;
        department = dept;
        List<PrivilegeHolder> selectedPrivileges = selected;
        for (WebUserPrivilege wup : getCurrentWebUserPrivileges()) {
            wup.setRetired(true);

        }
        getFacade().batchEdit(getCurrentWebUserPrivileges());
        if (selectedPrivileges == null) {
            return;
        }

        List<WebUserPrivilege> newWups = new ArrayList<>();
        List<WebUserPrivilege> oldWups = new ArrayList<>();

        for (PrivilegeHolder ph : selectedPrivileges) {
            if (ph.getPrivilege() == null) {
                continue;
            }
            String jpql = "select w"
                    + " from WebUserPrivilege w "
                    + " where w.department=:dep "
                    + " and w.webUser=:wu "
                    + " and w.privilege=:p";
            Map m = new HashMap();
            m.put("dep", department);
            m.put("wu", currentWebUser);
            m.put("p", ph.getPrivilege());
            WebUserPrivilege wup = getFacade().findFirstByJpql(jpql, m);
            if (wup == null) {
                wup = new WebUserPrivilege();
                wup.setDepartment(department);
                wup.setWebUser(currentWebUser);
                wup.setPrivilege(ph.getPrivilege());
                newWups.add(wup);
            } else {
                wup.setRetired(false);
                oldWups.add(wup);
            }
        }
        getFacade().batchCreate(newWups);
        getFacade().batchEdit(oldWups);

        fillUserPrivileges();
        JsfUtil.addSuccessMessage("Updated");
    }

    public void saveWebUserPrivileges() {
        List<PrivilegeHolder> selectedPrivileges = extractPrivileges(selectedNodes);

        // Retire all current web user privileges initially
        List<WebUserPrivilege> currentPrivileges = getCurrentWebUserPrivileges();
        for (WebUserPrivilege wup : currentPrivileges) {
            wup.setRetired(true);
        }

        if (selectedPrivileges == null) {
            getFacade().batchEdit(currentPrivileges);
            return;
        }

        List<WebUserPrivilege> newWups = new ArrayList<>();
        List<WebUserPrivilege> nonRetiredPrivileges = new ArrayList<>();

        for (PrivilegeHolder ph : selectedPrivileges) {
            if (ph.getPrivilege() == null) {
                continue;
            }

            boolean found = false;
            for (WebUserPrivilege wup : currentPrivileges) {
                if (wup.getPrivilege() == ph.getPrivilege()) {
                    wup.setRetired(false);
                    nonRetiredPrivileges.add(wup);
                    found = true;
                    break;
                }
            }

            if (!found) {
                WebUserPrivilege newWup = new WebUserPrivilege();
                newWup.setDepartment(department);
                newWup.setWebUser(currentWebUser);
                newWup.setPrivilege(ph.getPrivilege());
                newWups.add(newWup);
            }
        }

        getFacade().batchCreate(newWups);
        getFacade().batchEdit(currentPrivileges);

        // Combine non-retired current privileges and newly added privileges
        List<WebUserPrivilege> updatedPrivileges = new ArrayList<>(nonRetiredPrivileges);
        updatedPrivileges.addAll(newWups);

        // Set the combined list as current web user privileges
        setCurrentWebUserPrivileges(updatedPrivileges);
        // Log final state after saving

        for (WebUserPrivilege wup : updatedPrivileges) {
        }

        fillUserPrivileges();
        JsfUtil.addSuccessMessage("Updated");
    }

    private List<PrivilegeHolder> extractPrivileges(TreeNode[] selectedNodes) {
        List<PrivilegeHolder> privileges = new ArrayList<>();
        if (selectedNodes != null) {
            for (TreeNode node : selectedNodes) {
                Object data = node.getData();
                if (data instanceof PrivilegeHolder) {
                    PrivilegeHolder ph = (PrivilegeHolder) data;
                    privileges.add(ph);
                } else {
                    // Handle the case where the data is not of type PrivilegeHolder

                }
            }
        }
        return privileges;
    }

//    public static List<PrivilegeHolder> extractPrivileges(TreeNode[] selectedNodes) {
//        List<PrivilegeHolder> privileges = new ArrayList<>();
//        if (selectedNodes != null) {
//            for (TreeNode node : selectedNodes) {
//                Object data = node.getData();
//                if (data instanceof PrivilegeHolder) {
//                    privileges.add((PrivilegeHolder) data);
//                }
//            }
//        }
//        return privileges;
//    }
    public void saveWebUserRolePrivileges() {
        List<PrivilegeHolder> selectedPrivileges = extractPrivileges(selectedNodes);

        for (WebUserRolePrivilege wup : getCurrentWebUserRolePrivileges()) {
            wup.setRetired(true);
        }

        getRoleFacede().batchEdit(getCurrentWebUserRolePrivileges());
        if (selectedPrivileges == null) {
            return;
        }

        List<WebUserRolePrivilege> newWups = new ArrayList<>();
        List<WebUserRolePrivilege> oldWups = new ArrayList<>();

        for (PrivilegeHolder ph : selectedPrivileges) {
            if (ph.getPrivilege() == null) {
                continue;
            }
            String jpql = "select w"
                    + " from WebUserRolePrivilege w "
                    + " where w.webUserRole=:wu "
                    + " and w.privilege=:p";
            Map m = new HashMap();
            m.put("wu", webUserRole);
            m.put("p", ph.getPrivilege());
            WebUserRolePrivilege wup = getRoleFacede().findFirstByJpql(jpql, m);
            if (wup == null) {
                wup = new WebUserRolePrivilege();
                wup.setWebUserRole(webUserRole);
                wup.setPrivilege(ph.getPrivilege());
                newWups.add(wup);
            } else {
                wup.setRetired(false);
                oldWups.add(wup);
            }
        }
        getRoleFacede().batchCreate(newWups);
        getRoleFacede().batchEdit(oldWups);
        fillUserRolePrivileges();
        JsfUtil.addSuccessMessage("Updated");
    }
    
    private static void checkNodes(TreeNode root, List<PrivilegeHolder> privilegesToCheck) {
        if (root == null || privilegesToCheck == null || privilegesToCheck.isEmpty()) {
            return;
        }

        // Cast each child to TreeNode
        for (Object childObject : root.getChildren()) {
            if (childObject instanceof TreeNode) {
                TreeNode childNode = (TreeNode) childObject;
                checkNode(childNode, privilegesToCheck);
            }
        }
    }

    private static void checkNode(TreeNode node, List<PrivilegeHolder> privilegesToCheck) {
        if (node.getData() instanceof PrivilegeHolder) {
            PrivilegeHolder holder = (PrivilegeHolder) node.getData();
            if (privilegesToCheck.contains(holder)) {
                ((DefaultTreeNode) node).setSelected(true);
            }
        }

        // Cast each child to TreeNode
        for (Object childObject : node.getChildren()) {
            if (childObject instanceof TreeNode) {
                TreeNode childNode = (TreeNode) childObject;
                checkNode(childNode, privilegesToCheck);
            }
        }
    }

    private static void unselectTreeNodes(TreeNode root) {
        if (root == null) {
            return;
        }

        // Unselect the current node
        ((DefaultTreeNode) root).setSelected(false);

        // Recursively unselect child nodes
        for (Object childObject : root.getChildren()) {
            if (childObject instanceof TreeNode) {
                TreeNode childNode = (TreeNode) childObject;
                unselectTreeNodes(childNode);
            }
        }
    }

    public void filterPrivileges() {
        collapseAll(rootTreeNode);
        rootTreeNode.setExpanded(true);
        if (searchText == null || searchText.trim().isEmpty()) {
            return;
        }
        String st = searchText.trim().toLowerCase();
        expandMatches(rootTreeNode, st);
    }

    private void collapseAll(TreeNode node) {
        if (node == null) {
            return;
        }
        node.setExpanded(false);
        for (Object childObj : node.getChildren()) {
            if (childObj instanceof TreeNode) {
                collapseAll((TreeNode) childObj);
            }
        }
    }

    private boolean expandMatches(TreeNode node, String search) {
        boolean match = false;
        if (node.getData() instanceof PrivilegeHolder) {
            PrivilegeHolder ph = (PrivilegeHolder) node.getData();
            if (ph.getName() != null && ph.getName().toLowerCase().contains(search)) {
                match = true;
            }
        }
        for (Object childObj : node.getChildren()) {
            if (childObj instanceof TreeNode) {
                if (expandMatches((TreeNode) childObj, search)) {
                    match = true;
                }
            }
        }
        if (match) {
            node.setExpanded(true);
        }
        return match;
    }

    public void fillUserPrivileges() {
        List<WebUserPrivilege> wups;
        if (currentWebUser == null) {
            JsfUtil.addErrorMessage("User?");
        }
        String j = "SELECT i "
                + " FROM WebUserPrivilege i "
                + " where i.webUser=:wu "
                + " and i.retired<>:ret "
                + " and i.department=:dep";
        Map m = new HashMap();
        m.put("wu", currentWebUser);
        m.put("ret", true);
        m.put("dep", department);
        currentWebUserPrivileges = getEjbFacade().findByJpql(j, m);
        currentUserPrivilegeHolders = createPrivilegeHolders(currentWebUserPrivileges);
        unselectTreeNodes(rootTreeNode);
        checkNodes(rootTreeNode, currentUserPrivilegeHolders);
        privilegesLoaded = true;
    }
    
    public List<WebUserPrivilege> loadUserPrivileges(WebUser wu, Department dept){
        List<WebUserPrivilege> list = new ArrayList<>();
        
        if (wu == null) {
            return list;
        }
        if (dept == null) {
            return list;
        }
        String jpql = "SELECT i "
                + " FROM WebUserPrivilege i "
                + " where i.webUser=:wu "
                + " and i.department=:dep";
        Map m = new HashMap();
        m.put("wu", wu);
        m.put("dep", dept);
        
        list =  getEjbFacade().findByJpqlWithoutCache(jpql, m);
        
        return list;
    }
    
    public void clearUserAllDepartmentPrivileges(WebUser wu, Department dept){
        for(WebUserPrivilege wup : loadUserPrivileges(wu,dept)){
            wup.setRetired(true);
            getFacade().edit(wup);
        }
    }
    

    public WebUserPrivilege addUserPrivilege(Privileges prv, WebUser wu, Department dept) {
        if (prv == null) {
            return null;
        }
        if (wu == null) {
            return null;
        }
        if (dept == null) {
            return null;
        }
        String j = "SELECT i "
                + " FROM WebUserPrivilege i "
                + " where i.webUser=:wu "
                + " and i.privilege=:p "
                + " and i.department=:dep";
        Map m = new HashMap();
        m.put("wu", wu);
        m.put("p", prv);
        m.put("dep", dept);
        WebUserPrivilege wup = getEjbFacade().findFirstByJpql(j, m);
        if (wup == null) {
            wup = new WebUserPrivilege();
            wup.setDepartment(dept);
            wup.setWebUser(wu);
            wup.setPrivilege(prv);
            getFacade().create(wup);
        } else {
            wup.setRetired(false);
            getFacade().edit(wup);
        }
        return wup;
    }

    public void togglePrivilege(String privilegeName) {
        if (privilegeName == null || privilegeName.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Invalid privilege name");
            return;
        }

        if (currentWebUser == null) {
            JsfUtil.addErrorMessage("Please select a user");
            return;
        }

        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }

        try {
            Privileges privilege = Privileges.valueOf(privilegeName);

            // Check if user currently has this privilege
            String j = "SELECT i "
                    + " FROM WebUserPrivilege i "
                    + " where i.webUser=:wu "
                    + " and i.privilege=:p "
                    + " and i.department=:dep "
                    + " and i.retired=:ret";
            Map<String, Object> m = new HashMap<>();
            m.put("wu", currentWebUser);
            m.put("p", privilege);
            m.put("dep", department);
            m.put("ret", false);

            WebUserPrivilege existingPrivilege = getEjbFacade().findFirstByJpql(j, m);

            if (existingPrivilege != null) {
                // User has the privilege, so remove it (retire it)
                existingPrivilege.setRetired(true);
                existingPrivilege.setRetiredAt(new Date());
                existingPrivilege.setRetirer(sessionController.getLoggedUser());
                getFacade().edit(existingPrivilege);
                JsfUtil.addSuccessMessage("Privilege '" + privilege.toString() + "' removed from " + currentWebUser.getName());
            } else {
                // User doesn't have the privilege, so add it
                addUserPrivilege(privilege, currentWebUser, department);
                JsfUtil.addSuccessMessage("Privilege '" + privilege.toString() + "' assigned to " + currentWebUser.getName());
            }

            // Reload privileges to reflect changes
            fillUserPrivileges();

        } catch (IllegalArgumentException e) {
            JsfUtil.addErrorMessage("Invalid privilege: " + privilegeName);
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error toggling privilege: " + e.getMessage());
        }
    }

    public void makePrivilegesNeededToBeReloaded() {
        privilegesLoaded = false;
    }

    public void fillUserRolePrivileges(WebUserRole u) {
        webUserRole = u;
        fillUserRolePrivileges();
    }

    public void fillUserRolePrivileges() {
        List<WebUserRolePrivilege> wups;
        if (webUserRole == null) {
            JsfUtil.addErrorMessage("User Role?");
        }
        String j = "SELECT i "
                + " FROM WebUserRolePrivilege i "
                + " where i.webUserRole=:wu "
                + " and i.retired=:ret ";
        Map m = new HashMap();
        m.put("wu", webUserRole);
        m.put("ret", false);
        currentWebUserRolePrivileges = getRoleFacede().findByJpql(j, m);
        currentUserPrivilegeHolders = createRolePrivilegeHolders(currentWebUserRolePrivileges);
        unselectTreeNodes(rootTreeNode);
        checkNodes(rootTreeNode, currentUserPrivilegeHolders);
        privilegesLoaded = true;
    }

    public List<WebUserRolePrivilege> fetchUserPrivileges(WebUserRole role) {
        List<WebUserRolePrivilege> wups;
        if (role == null) {
            return null;
        }
        String j = "SELECT i "
                + " FROM WebUserRolePrivilege i "
                + " where i.webUserRole=:wu "
                + " and i.retired=:ret ";
        Map m = new HashMap();
        m.put("wu", role);
        m.put("ret", false);
        wups = getRoleFacede().findByJpql(j, m);
        return wups;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Navigational Methods">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public TreeNode[] getSelectedNodes() {
        return selectedNodes;
    }

    public void setSelectedNodes(TreeNode[] selectedNodes) {
        this.selectedNodes = selectedNodes;
    }

    private WebUserPrivilegeFacade getEjbFacade() {
        return ejbFacade;
    }

    public WebUser getCurrentWebUser() {
        return currentWebUser;
    }

    public void setCurrentWebUser(WebUser currentWebUser) {
        this.currentWebUser = currentWebUser;
    }

    public List<WebUserPrivilege> getSelectedItems() {
        return selectedItems;
    }

    public void setSelectedItems(List<WebUserPrivilege> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public List<WebUserRolePrivilege> getSelectedRoleItems() {
        return selectedRoleItems;
    }

    public void setSelectedRoleItems(List<WebUserRolePrivilege> selectedRoleItems) {
        this.selectedRoleItems = selectedRoleItems;
    }

    public TreeNode getRootTreeNode() {
        return rootTreeNode;
    }

    public void setRootTreeNode(TreeNode tmp) {
        this.rootTreeNode = tmp;
    }

    private WebUserPrivilegeFacade getFacade() {
        return ejbFacade;

    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<PrivilegeHolder> getCurrentUserPrivilegeHolders() {
        if (currentUserPrivilegeHolders == null) {
            currentUserPrivilegeHolders = new ArrayList<>();
        }
        return currentUserPrivilegeHolders;
    }

    public void setCurrentUserPrivilegeHolders(List<PrivilegeHolder> currentUserPrivilegeHolders) {
        this.currentUserPrivilegeHolders = currentUserPrivilegeHolders;
    }

    public List<WebUserPrivilege> getCurrentWebUserPrivileges() {
        if (currentWebUserPrivileges == null) {
            currentWebUserPrivileges = new ArrayList<>();
        }
        return currentWebUserPrivileges;
    }

    public void setCurrentWebUserPrivileges(List<WebUserPrivilege> currentWebUserPrivileges) {
        this.currentWebUserPrivileges = currentWebUserPrivileges;
    }

    public List<WebUserRolePrivilege> getCurrentWebUserRolePrivileges() {
        if (currentWebUserRolePrivileges == null) {
            currentWebUserRolePrivileges = new ArrayList<>();
        }
        return currentWebUserRolePrivileges;
    }

    public void setCurrentWebUserRolePrivileges(List<WebUserRolePrivilege> currentWebUserRolePrivileges) {
        this.currentWebUserRolePrivileges = currentWebUserRolePrivileges;
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public WebUserRole getWebUserRole() {
        return webUserRole;
    }

    public void setWebUserRole(WebUserRole webUserRole) {
        this.webUserRole = webUserRole;
    }

    public WebUserRolePrivilegeFacade getRoleFacede() {
        return facede;
    }

    public void setRoleFacede(WebUserRolePrivilegeFacade facede) {
        this.facede = facede;
    }

    public boolean isPrivilegesLoaded() {
        return privilegesLoaded;
    }

    public void setPrivilegesLoaded(boolean privilegesLoaded) {
        this.privilegesLoaded = privilegesLoaded;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Converters">
    /**
     *
     */
    @FacesConverter(forClass = WebUserPrivilege.class)
    public static class WebUserPrivilegeControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            UserPrivilageController controller = (UserPrivilageController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "userPrivilageController");
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
            if (object instanceof WebUserPrivilege) {
                WebUserPrivilege o = (WebUserPrivilege) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + UserPrivilageController.class.getName());
            }
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Innver Classes">
    public class PrivilegeHolder {

        private Privileges privilege;
        private String name;

        public PrivilegeHolder() {
        }

        public PrivilegeHolder(Privileges privilege, String name) {
            this.privilege = privilege;
            this.name = name;
        }

        public PrivilegeHolder(String name) {
            this.name = name;
        }

        public Privileges getPrivilege() {
            return privilege;
        }

        public void setPrivilege(Privileges privilege) {
            this.privilege = privilege;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 73 * hash + Objects.hashCode(this.privilege);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PrivilegeHolder other = (PrivilegeHolder) obj;
            return this.privilege == other.privilege;
        }

    }
    // </editor-fold>
}
