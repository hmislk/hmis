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
import com.divudi.data.Privileges;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.WebUser;
import com.divudi.entity.WebUserPrivilege;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.WebUserPrivilegeFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.WebUserRole;
import com.divudi.entity.WebUserRolePrivilege;
import com.divudi.facade.WebUserRolePrivilegeFacade;
import java.io.Serializable;
import java.util.ArrayList;
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
import javax.inject.Named;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
// </editor-fold>

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
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

        TreeNode opdNode = new DefaultTreeNode(new PrivilegeHolder(null, "OPD"), allNode);

        TreeNode billingMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Opd, "Billing Menu"), opdNode);

        TreeNode billNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdBilling, "Bill"), opdNode);
        TreeNode preBillingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdPreBilling, "Pre Billing"), opdNode);
        TreeNode collectingCentreBillingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdCollectingCentreBilling, "Collecting Centre Billing"), opdNode);
        TreeNode billSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdBillSearch, "Bill Search"), opdNode);
        TreeNode billItemSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdBillItemSearch, "Bill Item Search"), opdNode);
        TreeNode reprintNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdReprint, "Reprint"), opdNode);
        TreeNode cancelNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdCancel, "Cancel"), opdNode);
        TreeNode returnNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdReturn, "Return"), opdNode);
        TreeNode reactivateNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdReactivate, "Reactivate"), opdNode);
        TreeNode OpdLabReportSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdLabReportSearch, "Lab Report Search"), opdNode);
        TreeNode opdBillSearchEditNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdBillSearchEdit, "OPD Bill Search Edit (Patient Details)"), opdNode);
        TreeNode OpdReprintOriginalBillNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdReprintOriginalBill, "Reprint the Original Bill"), opdNode);

        TreeNode inwardNode = new DefaultTreeNode(new PrivilegeHolder(null, "Inward"), allNode);
        TreeNode inwardMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Inward, "Inward Menu"), inwardNode);
        TreeNode admissionsNode = new DefaultTreeNode("Admissions", inwardNode);
        TreeNode admissionMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardAdmissions, "Admission Menu"), admissionsNode);
        TreeNode admissionNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardAdmissionsAdmission, "Admission"), admissionsNode);
        TreeNode editAdmissionDetailsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardAdmissionsEditAdmission, "Edit Admission Details"), admissionsNode);
        TreeNode inwardAppointmentNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardAdmissionsInwardAppoinment, "Inward Appointment"), admissionsNode);

        TreeNode roomNode = new DefaultTreeNode("Room", inwardNode);
        TreeNode roomMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardRoom, "Room Menu"), roomNode);
        TreeNode roomOccupancyNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardRoomRoomOccupency, "Room Occupancy"), roomNode);
        TreeNode roomChangeNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardRoomRoomChange, "Room Change"), roomNode);
        TreeNode guardianRoomChangeNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardRoomGurdianRoomChange, "Guardian Room Change"), roomNode);
        TreeNode dischargeRoomOccupancyNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardRoomDischarge, "Discharge Room in Room Occupancy"), roomNode);

        TreeNode servicesItemsNode = new DefaultTreeNode("Services & Items", inwardNode);
        TreeNode servicesItemsMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardServicesAndItems, "Services & Items Menu"), servicesItemsNode);
        TreeNode addServicesNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardServicesAndItemsAddServices, "Add Services"), servicesItemsNode);
        TreeNode addOutsideChargesNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardServicesAndItemsAddOutSideCharges, "Add Outside Charges"), servicesItemsNode);
        TreeNode addProfessionalFeeNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardServicesAndItemsAddProfessionalFee, "Add Professional Fee"), servicesItemsNode);
        TreeNode addTimedServicesNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardServicesAndItemsAddTimedServices, "Add Timed Services"), servicesItemsNode);

        TreeNode inwardBillingNode = new DefaultTreeNode("Billing", inwardNode);
        TreeNode inwardBillingMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardBilling, "Billing Menu"), inwardBillingNode);
        TreeNode interimBillNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardBillingInterimBill, "Interim Bill"), inwardBillingNode);
        TreeNode interimBillSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardBillingInterimBillSearch, "Interim Bill Search"), inwardBillingNode);
        TreeNode editPatientNameAfterPaymentNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardFinalBillReportEdit, "Edit Patient Name After Payment Finalized"), inwardBillingNode);

        TreeNode inwardPharmacyNode = new DefaultTreeNode("Pharmacy", inwardNode);
        TreeNode inwardPharmacyMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardPharmacyMenu, "Pharmacy Menu"), inwardPharmacyNode);
        TreeNode inwardPharmacyIssueRequestNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardPharmacyIssueRequest, "Pharmacy Issue Request"), inwardPharmacyNode);
        TreeNode inwardPharmacyIssueRequestSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardPharmacyIssueRequestSearch, "Pharmacy Issue Request Search"), inwardPharmacyNode);

        TreeNode searchNode = new DefaultTreeNode("Search", inwardNode);
        TreeNode searchMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSearch, "Search Menu"), searchNode);
        TreeNode searchServiceBillNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSearchServiceBill, "Search Service Bill"), searchNode);
        TreeNode searchProfessionalBillNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSearchProfessionalBill, "Search Professional Bill"), searchNode);
        TreeNode searchFinalBillNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSearchFinalBill, "Search Final Bill"), searchNode);

        TreeNode inwardReportsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardReport, "Inward Reports"), inwardNode);
        TreeNode administrationNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardAdministration, "Administration"), inwardNode);

        TreeNode additionalPrivilegesNode = new DefaultTreeNode("Additional Privileges", inwardNode);
        TreeNode additionalPrivilegeMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardAdditionalPrivilages, "Additional Privilege Menu"), additionalPrivilegesNode);
        TreeNode searchBillsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardBillSearch, "Search Bills"), additionalPrivilegesNode);
        TreeNode searchBillItemsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardBillItemSearch, "Search Bill Items"), additionalPrivilegesNode);
        TreeNode reprintInwardNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardBillReprint, "Reprint"), additionalPrivilegesNode);
        TreeNode cancelInwardNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardCancel, "Cancel"), additionalPrivilegesNode);
        TreeNode returnInwardNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardReturn, "Return"), additionalPrivilegesNode);
        TreeNode reactivateInwardNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardReactivate, "Reactivate"), additionalPrivilegesNode);
        TreeNode showInwardFeeNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ShowInwardFee, "Show Inward Fee"), additionalPrivilegesNode);
        TreeNode inwardCheckNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardCheck, "Inward Check"), additionalPrivilegesNode);
        TreeNode inwardUncheckNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardUnCheck, "Inward Uncheck"), additionalPrivilegesNode);
        TreeNode inwardFinalBillCancelNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardFinalBillCancel, "Inward Final Bill Cancel Without Check Date Range"), additionalPrivilegesNode);
        TreeNode inwardOutsideMarkAsUnpaidNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardOutSideMarkAsUnPaid, "Inward Outside Bill Mark as Un-Paid"), additionalPrivilegesNode);
        TreeNode inwardBillSettleWithoutCheckNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardBillSettleWithoutCheck, "Inward Bill Settle Without Check"), additionalPrivilegesNode);

        TreeNode theatreNode = new DefaultTreeNode(new PrivilegeHolder(null, "Theatre"), allNode);
        TreeNode theatreMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Theatre, "Theatre Menu"), theatreNode);
        TreeNode addSurgeryNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheatreAddSurgery, "Add Surgery"), theatreNode);
        TreeNode theatreBillingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheatreBilling, "Theatre Billing"), theatreNode);
        TreeNode theatreTransferMenuItemNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterTransfer, "Theatre Transfer Menu Item"), theatreNode);
        TreeNode theatreTransferRequestNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterTransferRequest, "Theatre Transfer Request"), theatreNode);
        TreeNode theatreTransferIssueNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterTransferIssue, "Theatre Transfer Issue"), theatreNode);
        TreeNode theatreTransferReceiveNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterTransferRecieve, "Theatre Transfer Receive"), theatreNode);
        TreeNode theatreTransferReportNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterTransferReport, "Theatre Transfer Report"), theatreNode);
        TreeNode theatreShowReportsMenuItemNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterReports, "Theatre Show Reports Menu Item"), theatreNode);
        TreeNode theatreShowSummaryMenuItemNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterSummeries, "Theatre Show Summary Menu Item"), theatreNode);

        TreeNode theatreBHTIssueNode = new DefaultTreeNode("Theatre BHT Issue", theatreNode);
        TreeNode theatreBHTIssueNodeChild = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssue, "Theatre BHT Issue"), theatreBHTIssueNode);
        TreeNode pharmacyBHTIssueNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssuePharmacy, "Pharmacy BHT Issue"), theatreBHTIssueNode);
        TreeNode generalBHTIssueNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueStore, "General BHT Issue"), theatreBHTIssueNode);
        TreeNode inwardBHTBillingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueStoreBhtBilling, "Inward BHT Billing"), generalBHTIssueNode);
        TreeNode searchBHTIssueBillNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueStoreBhtSearchBill, "Search BHT Issue Bill"), generalBHTIssueNode);
        TreeNode searchBHTIssueBillItemsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueStoreBhtSearchBillItem, "Search BHT Issue Bill Items"), generalBHTIssueNode);

        TreeNode opdIssueNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueOpd, "Opd Issue"), theatreNode);
        TreeNode opdIssueForCashierNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueOpdForCasheir, "Opd Issue For Cashier"), opdIssueNode);
        TreeNode opdIssueSearchPreBillNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueOpdSearchPreBill, "Opd Issue Search Pre Bill"), opdIssueNode);
        TreeNode opdIssueReturnItemOnlyNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueOpdSearchPreBillForReturnItemOnly, "Opd Issue Return Item Only"), opdIssueNode);
        TreeNode opdIssueSearchPreBillReturnNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueOpdSearchPreBillReturn, "Opd Issue Search Pre Bill Return"), opdIssueNode);
        TreeNode opdIssuePreBillAddToStockNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.TheaterIssueOpdSearchPreBillAddToStock, "Opd Issue Pre Bill Add To Stock"), opdIssueNode);

        TreeNode labNode = new DefaultTreeNode(new PrivilegeHolder(null, "Lab"), allNode);
        TreeNode labMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Lab, "Lab Menu"), labNode);
        TreeNode labBillingMenuNode = new DefaultTreeNode("Billing Menu", labNode);
        TreeNode labBillNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBilling, "Lab Bill"), labBillingMenuNode);
        TreeNode labBillSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillSearch, "Lab Bill Search"), labBillingMenuNode);
        TreeNode labBillItemSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillItemSearch, "Lab Bill Item Search"), labBillingMenuNode);
        TreeNode labBillSearchCashierNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillSearchCashier, "Lab Bill Search"), labNode);
        TreeNode labSearchBillsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillSearch, "Search Bills"), labNode);
        TreeNode labReportSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReportSearch, "Lab Report Search"), labNode);
        TreeNode patientEditNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabEditPatient, "Patient Edit"), labNode);
        TreeNode labBillReprintNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillReprint, "Lab Bill Reprint"), labNode);
        TreeNode labBillReturnNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillReturning, "Lab Bill Return"), labNode);
        TreeNode labBillCancelNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillCancelling, "Lab Bill Cancel"), labNode);
        TreeNode ccBillCancelNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.CollectingCentreCancelling, "CC Bill Cancel"), labNode);
        TreeNode labBillReactivateNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillReactivating, "Reactivate"), labNode);
        TreeNode sampleCollectionNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSampleCollecting, "Sample Collection"), labNode);
        TreeNode sampleReceiveNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSampleReceiving, "Sample Receive"), labNode);
        TreeNode dataEntryNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabDataentry, "DataEntry"), labNode);
        TreeNode authorizeNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAutherizing, "Authorize"), labNode);
        TreeNode deAuthorizeNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabDeAutherizing, "De-Authorize"), labNode);
        TreeNode reportPrintNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabPrinting, "Report Print"), labNode);
        TreeNode reportReprintNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReprinting, "Report Reprint"), reportPrintNode);
        TreeNode labReportFormatsEditingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReportFormatEditing, "Lab Report Formats Editing"), labNode);
        TreeNode reportEditAfterAuthorizedNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReportEdit, "Report Edit After Authorized"), labNode);
        TreeNode labSummariesNode = new DefaultTreeNode("Lab Summaries", labNode);
        TreeNode labSummariesMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSummeries, "Lab Summaries Menu"), labSummariesNode);
        TreeNode labSummariesLevel1Node = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSummeriesLevel1, "Lab Summaries Level1"), labSummariesNode);
        TreeNode labSummariesLevel2Node = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSummeriesLevel2, "Lab Summaries Level2"), labSummariesNode);
        TreeNode labSummariesLevel3Node = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSummeriesLevel3, "Lab Summaries Level3"), labSummariesNode);
        TreeNode labInvestigationFeeNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabInvestigationFee, "Lab Investigation Fees"), labNode);
        TreeNode labBillCancelSpecialNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillCancelSpecial, "Lab Bill Cancel Special(after collecting sample can cancel)"), labNode);
        TreeNode labBillRefundSpecialNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabBillRefundSpecial, "Lab Bill Refund Special(after collecting sample can Refund)"), labNode);
        TreeNode addInwardServicesNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAddInwardServices, "Add Inward Services"), labNode);
        TreeNode searchByLoggedInstitutionNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReportSearchByLoggedInstitution, "Search By Logged Institution"), labNode);
        TreeNode labAdministrationNode = new DefaultTreeNode(new PrivilegeHolder(null, "Lab Administration"), labNode);
        TreeNode labAdministrationMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabAdiministrator, "Lab Administration Menu"), labAdministrationNode);
        TreeNode manageItemsMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabItems, "Manage Items Menu"), labAdministrationNode);
        TreeNode manageItemFeeUpdateNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabItemFeeUpadate, "Manage Item Fee Update"), manageItemsMenuNode);
        TreeNode manageItemFeeDeleteNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabItemFeeDelete, "Manage Item Fee Delete"), manageItemsMenuNode);
        TreeNode manageReportsMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReports, "Manage Reports Menu"), labAdministrationNode);
        TreeNode listsMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabLists, "Lists Menu"), labAdministrationNode);
        TreeNode setupMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabSetUp, "Setup Menu"), labAdministrationNode);
        TreeNode labInwardBillingMenuNode = new DefaultTreeNode("Lab Inward Billing Menu", labNode);
        TreeNode labInwardBillNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabInwardBilling, "Lab Inward Bill"), labInwardBillingMenuNode);
        TreeNode labInwardBillSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabInwardSearchServiceBill, "Lab Inward Bill Search"), labInwardBillingMenuNode);
        TreeNode labCollectingCenterBillingNode = new DefaultTreeNode(new PrivilegeHolder(null, "Lab Collecting Center Billing"), labNode);
        TreeNode labCollectingCenterMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabCollectingCentreBilling, "Lab Collecting Center Menu"), labCollectingCenterBillingNode);
        TreeNode labCCBillingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabCCBilling, "Lab Collecting center Billing"), labCollectingCenterBillingNode);
        TreeNode labCCBillingSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabCCBillingSearch, "Lab Collecting Center Bill search"), labCollectingCenterBillingNode);
        TreeNode labReportingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.LabReporting, "Lab Reporting"), labNode);

        TreeNode pharmacyNode = new DefaultTreeNode(new PrivilegeHolder(null, "Pharmacy"), allNode);
        TreeNode pharmacyMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Pharmacy, "Pharmacy Menu"), pharmacyNode);
        TreeNode pharmacyAdministrationNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdministration, "Pharmacy Administration"), pharmacyNode);

        TreeNode channellingNode = new DefaultTreeNode(new PrivilegeHolder(null, "Channelling"), allNode);
        TreeNode channellingMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Channelling, "Channelling Menu"), channellingNode);
        TreeNode channelBookingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingChannelBooking, "Channel Booking"), channellingNode);
        TreeNode channelfutureBookingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingFutureChannelBooking, "Channel Future Booking"), channellingNode);
        TreeNode channelpastBookingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPastBooking, "Past Booking"), channellingNode);
        TreeNode channelbookedListNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingBookedList, "Booked List"), channellingNode);
        TreeNode channeldoctorLeaveMenuNode = new DefaultTreeNode("Doctor Leave Menu", channellingNode);
        TreeNode channeldoctorLeaveByDateNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingDoctorLeaveByDate, "Doctor Leave By Date"), channeldoctorLeaveMenuNode);
        TreeNode channeldoctorLeaveBySessionNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingDoctorLeaveByServiceSession, "Doctor Leave By Service Session"), channeldoctorLeaveMenuNode);
        TreeNode channelchannelSchedulingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingChannelSheduling, "Channel Scheduling"), channellingNode);
        TreeNode channelSpecialchannelSchedulingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingSpecialChannelSheduling, "Special Channel Scheduling"), channellingNode);
        TreeNode channelAgentFeeNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingChannelAgentFee, "Channel Agent Fee"), channellingNode);
        TreeNode channelBookingInterfaceNode = new DefaultTreeNode("Channel Booking Interface", channellingNode);
        TreeNode channelbookingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingBokking, "Booking"), channelBookingInterfaceNode);
        TreeNode channelreprintNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingReprint, "Reprint"), channelBookingInterfaceNode);
        TreeNode channelcancelNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingCancel, "Cancel"), channelBookingInterfaceNode);
        TreeNode channelrefundNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingRefund, "Refund"), channelBookingInterfaceNode);
        TreeNode channelsettleNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingSettle, "Settle"), channelBookingInterfaceNode);
        TreeNode channelchangeNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingChange, "Change"), channelBookingInterfaceNode);
        TreeNode channelsearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingSearch, "Search"), channelBookingInterfaceNode);
        TreeNode channelviewsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingViews, "Views"), channelBookingInterfaceNode);
        TreeNode channeldoctorPaymentNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingDocPay, "Doctor Payment"), channelBookingInterfaceNode);
        TreeNode channelrestrictBookingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelBookingRestric, "Restrict Channel Booking"), channelBookingInterfaceNode);
        TreeNode channelprintPastBookingReceiptNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPrintInPastBooking, "Print Past Booking Receipt"), channellingNode);
        TreeNode channelpaymentNode = new DefaultTreeNode("Payment", channellingNode);
        TreeNode channelpaymentMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPayment, "Payment Menu"), channelpaymentNode);
        TreeNode channelpayDoctorNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPaymentPayDoctor, "Pay Doctor"), channelpaymentNode);
        TreeNode channelpaymentDueSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPaymentDueSearch, "Payment Due Search"), channelpaymentNode);
        TreeNode channelpaymentDoneSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPaymentDoneSearch, "Payment Done Search"), channelpaymentNode);
        TreeNode channelcashierTransactionNode = new DefaultTreeNode("Cashier Transaction", channellingNode);
        TreeNode channelcashierTransactionMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCashierTransaction, "Cashier Transaction Menu"), channelcashierTransactionNode);
        TreeNode channelincomeNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCashierTransactionIncome, "Income"), channelcashierTransactionNode);
        TreeNode channelincomeSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCashierTransactionIncomeSearch, "Income Search"), channelcashierTransactionNode);
        TreeNode channelexpensesNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCashierTransactionExpencess, "Expenses"), channelcashierTransactionNode);
        TreeNode channelexpensesSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCashierTransactionExpencessSearch, "Expenses Search"), channelcashierTransactionNode);
        TreeNode channeladministratorNode = new DefaultTreeNode("Administrator", channellingNode);
        TreeNode channeleditAppointmentCountNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingApoinmentNumberCountEdit, "Edit Appointment Count"), channeladministratorNode);
        TreeNode channeleditAppointmentNumberNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingEditSerialNo, "Edit Appointment Number"), channeladministratorNode);
        TreeNode channeleditPatientDetailsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingEditPatientDetails, "Edit Patient Details"), channeladministratorNode);
        TreeNode channeldeleteScheduleNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingChannelShedulRemove, "Delete Schedule"), channeladministratorNode);
        TreeNode channeleditSessionNameNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingChannelShedulName, "Edit Session Name"), channeladministratorNode);
        TreeNode channeleditSessionStartingNoNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingChannelShedulStartingNo, "Edit Session Starting No"), channeladministratorNode);
        TreeNode channeleditSessionRoomNoNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingChannelShedulRoomNo, "Edit Session Room No"), channeladministratorNode);
        TreeNode channeleditSessionMaxRowNoNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingChannelShedulMaxRowNo, "Edit Session Max Row No"), channeladministratorNode);
        TreeNode channeleditCreditLimitUserLevelNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingEditCreditLimitUserLevel, "Edit Credit Limit User Level"), channeladministratorNode);
        TreeNode channeleditCreditLimitAdminLevelNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingEditCreditLimitAdminLevel, "Edit Credit Limit Administrator Level"), channeladministratorNode);
        TreeNode ChannellingReprintOriginalBill = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingReprintOriginalBill, "Channelling Reprint Original Bill"), channeladministratorNode);
        TreeNode ChannellingPastBookingPatientAttend = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPastBookingPatientAttend, "Channelling Attend Patients To Past Booking"), channeladministratorNode);
        TreeNode channelReportsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelReports, "Channel Reports"), channellingNode);
        TreeNode channelSummaryNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelSummery, "Channel Summary"), channellingNode);
        TreeNode channelManagementNode = new DefaultTreeNode("Channel Management", channellingNode);
        TreeNode channelManagementMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelManagement, "Channel Management Menu"), channelManagementNode);
        TreeNode channelAgenciesNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelAgencyAgencies, "Channel Agencies"), channelManagementNode);
        TreeNode channelAgencyCreditLimitUpdateNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelAgencyCreditLimitUpdate, "Channel Agency Credit Limit Update"), channelManagementNode);
        TreeNode channelAgencyCreditLimitUpdateBulkNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelAgencyCreditLimitUpdateBulk, "Channel Agency Credit Limit Update (Bulk)"), channelManagementNode);
        TreeNode addChannelBookToAgencyNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelAddChannelBookToAgency, "Add Channel Book To Agency"), channelManagementNode);
        TreeNode manageSpecialitiesNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelManageSpecialities, "Channel Management Specialities"), channelManagementNode);
        TreeNode manageConsultantsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelManageConsultants, "Channel Management Consultants"), channelManagementNode);
        TreeNode editingAppointmentCountNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelEditingAppoinmentCount, "Channel Editing Appointment Count"), channelManagementNode);
        TreeNode addChannelingConsultantsToInstitutionNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelAddChannelingConsultantToInstitutions, "Add Channelling Consultants To Institution"), channelManagementNode);
        TreeNode channelFeeUpdateNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelFeeUpdate, "Channel Fee Update"), channelManagementNode);
        TreeNode channelCreditNoteNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCrdeitNote, "Channel Credit Note"), channelManagementNode);
        TreeNode channelCreditNoteSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCrdeitNoteSearch, "Channel Credit Note Search"), channelManagementNode);
        TreeNode channelDebitNoteNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelDebitNote, "Channel Debit Note"), channelManagementNode);
        TreeNode channelDebitNoteSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelDebitNoteSearch, "Channel Debit Note Search"), channelManagementNode);
        TreeNode channelCashCancelRestrictionNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelCashCancelRestriction, "Channel Cash Cancel Restriction"), channelManagementNode);
        TreeNode channelActiveVatNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannelActiveVat, "Channel Active Vat"), channelManagementNode);

        ///////////////////
        TreeNode paymentNode = new DefaultTreeNode(new PrivilegeHolder(null, "Payment"), allNode);
        TreeNode paymentMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Payment, "Payment Menu"), paymentNode);
        TreeNode staffPaymentNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PaymentBilling, "Staff Payment Billing"), paymentNode);
        TreeNode paymentSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PaymentBillSearch, "Payment Search"), paymentNode);
        TreeNode paymentReprintsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PaymentBillReprint, "Payment Reprints"), paymentNode);
        TreeNode paymentCancelNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PaymentBillCancel, "Payment Cancel"), paymentNode);
        TreeNode paymentRefundNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PaymentBillRefund, "Payment Refund"), paymentNode);
        TreeNode paymentReactivationNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PaymentBillReactivation, "Payment Reactivation"), paymentNode);

        TreeNode reportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Reports"), allNode);
        TreeNode reportsMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Reports, "Reports Menu"), reportsNode);
        TreeNode forOwnInstitutionNode = new DefaultTreeNode("For Own Institution", reportsNode);
        TreeNode forAllInstitutionNode = new DefaultTreeNode("For All Institution", reportsNode);

        TreeNode cashCardBillReportsOwnNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSearchCashCardOwn, "Cash/Card Bill Reports"), forOwnInstitutionNode);
        TreeNode creditBillReportsOwnNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSearchCreditOwn, "Credit Bill Reports"), forOwnInstitutionNode);
        TreeNode itemReportsOwnNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsItemOwn, "Item Reports"), forOwnInstitutionNode);

        TreeNode cashCardBillReportsOtherNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSearchCashCardOther, "Cash/Card Bill Reports"), forAllInstitutionNode);
        TreeNode creditBillReportsOtherNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportSearchCreditOther, "Credit Bill Reports"), forAllInstitutionNode);
        TreeNode itemReportsOtherNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsItemOwn, "Item Reports"), forAllInstitutionNode);

        TreeNode clinicalsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Clinicals"), allNode);
        TreeNode clinicalDataNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Clinical, "Clinical Data"), clinicalsNode);
        TreeNode patientSummeryNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientSummery, "Patient Summary"), clinicalsNode);
        TreeNode patientDetailsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientDetails, "Patient Details"), clinicalsNode);
        TreeNode patientPhotoNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientPhoto, "Patient Photo"), clinicalsNode);
        TreeNode visitDetailsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalVisitDetail, "Visit Details"), clinicalsNode);
        TreeNode visitSummeryNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalVisitSummery, "Visit Summary"), clinicalsNode);
        TreeNode historyNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalHistory, "History"), clinicalsNode);
        TreeNode clinicaladministrationNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalAdministration, "Administration"), clinicalsNode);
        TreeNode clinicalPatientDeleteNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientDelete, "Clinical Patient Delete"), clinicalsNode);

        TreeNode adminNode = new DefaultTreeNode(new PrivilegeHolder(null, "Administration"), allNode);
        TreeNode adminMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Admin, "Admin Menu"), adminNode);
        TreeNode manageUsersNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.AdminManagingUsers, "Manage Users"), adminNode);
        TreeNode manageInstitutionsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.AdminInstitutions, "Manage Institutions"), adminNode);
        TreeNode manageStaffNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.AdminStaff, "Manage Staff"), adminNode);
        TreeNode manageItemsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.AdminItems, "Manage Items/Services"), adminNode);
        TreeNode manageFeesNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.AdminPrices, "Manage Fees/Prices/Packages"), adminNode);
        TreeNode filterWithoutDepartmentNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.AdminFilterWithoutDepartment, "Filter Without Department"), adminNode);
        TreeNode searchAllNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.SearchAll, "Search All"), adminNode);
        TreeNode changeProfessionalFeeNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChangeProfessionalFee, "Change Professional Fee"), adminNode);
        TreeNode changeCollectingCentreNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChangeCollectingCentre, "Change Collecting Centre"), adminNode);
        TreeNode sendBulkSMSNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.SendBulkSMS, "Send Bulk SMS"), adminNode);
        TreeNode onlyForDevelopersNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Developers, "Only For Developers(Don't Add That)"), adminNode);

        TreeNode membershipNode = new DefaultTreeNode(new PrivilegeHolder(null, "Membership"), allNode);
        TreeNode membershipMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShip, "Membership Menu"), membershipNode);
        TreeNode addMembersNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipAdd, "Add Members"), membershipNode);
        TreeNode editMembersNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipEdit, "Edit Members"), membershipNode);
        TreeNode membershipReportsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.MembershipReports, "Membership Reports"), membershipNode);
        TreeNode membershipDiscountManagementNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.MembershipDiscountManagement, "Membership Discount Management"), membershipNode);
        TreeNode membershipAdministrationNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.MembershipAdministration, "Membership Administration"), membershipNode);
        TreeNode otherNode = new DefaultTreeNode("Other", membershipNode);

        TreeNode membershipSchemesNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.MembershipSchemes, "Membership Schemes"), otherNode);
        TreeNode inwardMembershipMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipInwardMemberShip, "Inward Membership Menu"), otherNode);
        TreeNode schemesDiscountsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipInwardMemberShipSchemesDicounts, "Schemes Discounts"), otherNode);
        TreeNode inwardMembershipReportNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipInwardMemberShipInwardMemberShipReport, "Inward Membership Report"), otherNode);
        TreeNode opdMembershipDisMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipOpdMemberShipDis, "Opd Membership Dis Menu"), otherNode);
        TreeNode discountByDepartmentNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipOpdMemberShipDisByDepartment, "Discount By Department"), otherNode);
        TreeNode discountByCategoryNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipOpdMemberShipDisByCategory, "Discount By Category"), otherNode);
        TreeNode opdMembershipReportNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipOpdMemberShipDisOpdMemberShipReport, "Opd Membership Report"), otherNode);
        TreeNode reActivateRegisteredPatientNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipMemberReActive, "Re-Activate Registered Patient"), otherNode);
        TreeNode deActivateRegisteredPatientNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.MemberShipMemberDeActive, "De-Activate Registered Patient"), otherNode);

        TreeNode humanResourceNode = new DefaultTreeNode(new PrivilegeHolder(null, "Human Resource"), allNode);
        TreeNode hrMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Hr, "HR Menu"), humanResourceNode);
        TreeNode workingTimeNode = new DefaultTreeNode("Working Time", humanResourceNode);
        TreeNode workingTimeMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrWorkingTime, "Working Time Menu"), workingTimeNode);
        TreeNode rosterTableNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrRosterTable, "Roster Table"), workingTimeNode);
        TreeNode uploadAttendanceNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrUploadAttendance, "Upload Attendance"), workingTimeNode);
        TreeNode analyseAttendanceByRosterNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrAnalyseAttendenceByRoster, "Analyse Attendance By Roster"), workingTimeNode);
        TreeNode analyseAttendanceByStaffNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrAnalyseAttendenceByStaff, "Analyse Attendance By Staff"), workingTimeNode);
        TreeNode formNode = new DefaultTreeNode("Form", humanResourceNode);
        TreeNode formMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrForms, "Form Menu"), formNode);
        TreeNode leaveFormNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrLeaveForms, "Leave Form"), formNode);
        TreeNode additionalFormNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrAdditionalForms, "Additional Form"), formNode);
        TreeNode hrSalaryAdvanceNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrAdvanceSalary, "HR Salary Advance"), humanResourceNode);
        TreeNode hrSalaryNode = new DefaultTreeNode("HR Salary", humanResourceNode);
        TreeNode hrSalaryGenerateNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrGenerateSalary, "HR Salary Generate"), hrSalaryNode);
        TreeNode hrSalaryGenerateSpecialNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrGenerateSalarySpecial, "HR Salary Generate Special"), hrSalaryNode);
        TreeNode hrSalaryPrintNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrPrintSalary, "HR Salary Print"), humanResourceNode);
        TreeNode hrReportsNode = new DefaultTreeNode("HR Reports", humanResourceNode);
        TreeNode hrReportsMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrReports, "HR Reports Menu"), hrReportsNode);
        TreeNode hrReportsLevel1Node = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrReportsLevel1, "HR Reports Level 1"), hrReportsNode);
        TreeNode hrReportsLevel2Node = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrReportsLevel2, "HR Reports Level 2"), hrReportsNode);
        TreeNode hrReportsLevel3Node = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrReportsLevel3, "HR Reports Level 3"), hrReportsNode);
// TreeNode employeeHistoryReportNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.EmployeeHistoryReport, "HR Employee History Reports"), hrReportsNode);
        TreeNode hrAdministrationNode = new DefaultTreeNode("HR Administration", humanResourceNode);
        TreeNode hrAdministrationMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrAdmin, "HR Administration Menu"), hrAdministrationNode);
        TreeNode hrDeleteLateLeaveNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.hrDeleteLateLeave, "HR Delete Late Leave"), hrAdministrationNode);
        TreeNode hrEditRetiedDateNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrEditRetiedDate, "HR Edit Retied Date"), hrAdministrationNode);
        TreeNode hrRemoveResignDateNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.HrRemoveResignDate, "HR Remove Resign Date"), hrAdministrationNode);

        TreeNode storeNode = new DefaultTreeNode(new PrivilegeHolder(null, "Store"), allNode);
        TreeNode storeMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Store, "Store Menu"), storeNode);
        TreeNode storeissueNode = new DefaultTreeNode("Issue", storeNode);
        TreeNode storeissueMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreIssue, "Issue Menu"), storeissueNode);
        TreeNode storeinwardBillingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreIssueInwardBilling, "Inward Billing"), storeissueNode);
        TreeNode storesearchIssueBillNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreIssueSearchBill, "Search Issue Bill"), storeissueNode);
        TreeNode storesearchIssueBillItemsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreIssueBillItems, "Search Issue Bill Items"), storeissueNode);
        TreeNode storepurchaseNode = new DefaultTreeNode("Purchase", storeNode);
        TreeNode storepurchaseMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchase, "Purchase Menu"), storepurchaseNode);
        TreeNode storepurchaseOrderNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchaseOrder, "Purchase Order"), storepurchaseNode);
        TreeNode storepoApproveNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchaseOrderApprove, "PO Approve"), storepurchaseNode);
        TreeNode storestoregrnReceiveNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchaseGRNRecive, "GRN Receive"), storepurchaseNode);
        TreeNode storestoregrnReturnNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchaseGRNReturn, "GRN Return"), storepurchaseNode);
        TreeNode storepurchasePurchaseNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchasePurchase, "Purchase"), storepurchaseNode);
        TreeNode storepoApproveSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchaseOrderApproveSearch, "PO Approve Search"), storepurchaseNode);
        TreeNode storetransferNode = new DefaultTreeNode("Transfer", storeNode);
        TreeNode storetransferMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreTransfer, "Transfer Menu"), storetransferNode);
        TreeNode storetransferRequestNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreTransferRequest, "Request"), storetransferNode);
        TreeNode storetransferIssueNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreTransferIssue, "Issue"), storetransferNode);
        TreeNode storetransferReceiveNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreTransferRecive, "Receive"), storetransferNode);
        TreeNode storetransferReportNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreTransferReport, "Report"), storetransferNode);
        TreeNode storeadjustmentNode = new DefaultTreeNode("Adjustment", storeNode);
        TreeNode storeadjustmentMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreAdjustment, "Adjustment Menu"), storeadjustmentNode);
        TreeNode storedepartmentStockNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreAdjustmentDepartmentStock, "Department Stock(Qty)"), storeadjustmentNode);
        TreeNode storestaffStockAdjustmentNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreAdjustmentStaffStock, "Staff Stock Adjustment"), storeadjustmentNode);
        TreeNode storepurchaseRateNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreAdjustmentPurchaseRate, "Purchase Rate"), storeadjustmentNode);
        TreeNode storesaleRateNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreAdjustmentSaleRate, "Sale Rate"), storeadjustmentNode);
        TreeNode storedelorPaymentNode = new DefaultTreeNode("Delor Payment", storeNode);
        TreeNode storedelorPaymentMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreDealorPayment, "Delor Payment Menu"), storedelorPaymentNode);
        TreeNode storedelorDueSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreDealorPaymentDueSearch, "Delor Due Search"), storedelorPaymentNode);
        TreeNode storedelorDueByAgeNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreDealorPaymentDueByAge, "Delor Due By Age"), storedelorPaymentNode);
        TreeNode storepaymentNode = new DefaultTreeNode("Payment", storedelorPaymentNode);
        TreeNode storepaymentMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreDealorPaymentPayment, "Payment Menu"), storepaymentNode);
        TreeNode storegrnPaymentNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreDealorPaymentPaymentGRN, "GRN Payment"), storepaymentNode);
        TreeNode storegrnPaymentSelectNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreDealorPaymentPaymentGRNSelect, "GRN Payment(Select)"), storepaymentNode);
        TreeNode storegrnPaymentDueSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreDealorPaymentGRNDoneSearch, "GRN Payment Due Search"), storedelorPaymentNode);
        TreeNode storesearchNode = new DefaultTreeNode("Search", storeNode);
        TreeNode storesearchMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreSearch, "Search Menu"), storesearchNode);
        TreeNode storereportNode = new DefaultTreeNode("Report", storeNode);
        TreeNode storereportMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreReports, "Report Menu"), storereportNode);
        TreeNode storesummaryNode = new DefaultTreeNode("Summary", storeNode);
        TreeNode storesummaryMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreSummery, "Summary Menu"), storesummaryNode);
        TreeNode storeadministrationNode = new DefaultTreeNode("Administration", storeNode);
        TreeNode storeadministrationMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreAdministration, "Administration Menu"), storeadministrationNode);

        TreeNode searchRootNode = new DefaultTreeNode(new PrivilegeHolder(null, "Search"), allNode);
        TreeNode searchMenuRootNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Search, "Search Menu"), searchRootNode);
        TreeNode grandSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.SearchGrand, "Grand Search"), searchRootNode);

        TreeNode userNode = new DefaultTreeNode(new PrivilegeHolder(null, "User"), allNode);
        TreeNode changeThemeNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChangePreferece, "Change Theme"), userNode);

        TreeNode cashTransactionNode = new DefaultTreeNode(new PrivilegeHolder(null, "Cash Transaction"), allNode);
        TreeNode cashTransactionMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.CashTransaction, "Cash Transaction Menu"), cashTransactionNode);
        TreeNode cashInNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.CashTransactionCashIn, "Cash In"), cashTransactionNode);
        TreeNode cashOutNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.CashTransactionCashOut, "Cash Out"), cashTransactionNode);
        TreeNode listToCashReceiveNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.CashTransactionListToCashRecieve, "List To Cash Receive"), cashTransactionNode);

        //Pharmacy
        TreeNode retailTransaction = new DefaultTreeNode("Pharmacy Retail Transaction", pharmacyNode);
        TreeNode retailTransactionMenu = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyRetailTransactionMenue, "Pharmacy Retail Transaction Menu"), retailTransaction);
        TreeNode PharmacySale = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySale, "Pharmacy Sale"), retailTransaction);
        TreeNode PharmacySaleForCashier = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleForCashier, "Pharmacy Sale For Cashier"), retailTransaction);
        TreeNode PharmacySaleWithOutStock = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleWithOutStock, "Pharmacy Sale With Out Stock"), retailTransaction);
        TreeNode PharmacySearchSaleBill = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySearchSaleBill, "Pharmacy Search Sale Bill"), retailTransaction);
        TreeNode PharmacySearchSalePreBill = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySearchSalePreBill, "Pharmacy Search Sale PreBill"), retailTransaction);
        TreeNode PharmacySearchSaleBillItems = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySearchSaleBillItems, "Pharmacy Search Sale BillItems"), retailTransaction);
        TreeNode PharmacyReturnItemsOnly = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyReturnItemsOnly, "Pharmacy Return Items Only"), retailTransaction);
        TreeNode PharmacyReturnItemsAndPayments = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyReturnItemsAndPayments, "Pharmacy Return Items And Payments"), retailTransaction);
        TreeNode PharmacySearchReturnBill = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySearchReturnBill, "Pharmacy Search ReturnBill"), retailTransaction);
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
        TreeNode disbursementIssueForRequest = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisbursementIssurForRequest, "Issue for Request"), disbursementNode);
        TreeNode disbursementDirectIssue = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisbursementDirectIssue, "Direct Issue"), disbursementNode);
        TreeNode disbursementRecieve = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisbursementRecieve, "Recieve"), disbursementNode);
        TreeNode PharmacyDisbursementReports = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisbursementReports, "Pharmacy Disbursement Reports"), disbursementNode);

        TreeNode InpatientMedicationManagementNode = new DefaultTreeNode("Inpatient medication Management", pharmacyNode);
        TreeNode InpatientMedicationManagementMenue = new DefaultTreeNode(new PrivilegeHolder(Privileges.InpatientMedicationManagementMenue, "Procurement Menu"), InpatientMedicationManagementNode);
        TreeNode PharmacyDirectIssueToBht = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDirectIssueToBht, "Pharmacy Direct Issue To Bht"), InpatientMedicationManagementNode);
        TreeNode PharmacyDirectIssueToTheaterCases = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDirectIssueToTheaterCases, "Pharmacy Direct Issue To Theater Cases"), InpatientMedicationManagementNode);
        TreeNode PharmacyBhtIssueRequest = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyBhtIssueRequest, "Pharmacy Bht Issue Request"), InpatientMedicationManagementNode);
        TreeNode PharmacySearchInpatientDirectIssuesbyBill = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySearchInpatientDirectIssuesbyBill, "Pharmacy Search Inpatient Direct Issues by Bill"), InpatientMedicationManagementNode);
        TreeNode PharmacySearchInpatientDirectIssuesbyItem = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySearchInpatientDirectIssuesbyItem, "Pharmacy Search Inpatient Direct Issues by Item"), InpatientMedicationManagementNode);
        TreeNode PharmacySearchInpatientDirectIssueReturnsbyBill = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySearchInpatientDirectIssueReturnsbyBill, "Pharmacy Search Inpatient Direct Issue Returns by Bill"), InpatientMedicationManagementNode);
        TreeNode PharmacysSearchInpatientDirectIssueReturnsbyItem = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacysSearchInpatientDirectIssueReturnsbyItem, "Pharmacy Search Inpatient Direct Issue Returns by Item"), InpatientMedicationManagementNode);

        TreeNode ProcumentNode = new DefaultTreeNode("Pharmacy Procument", pharmacyNode);
        TreeNode pharmacyProcurementMenu = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyProcurementMenu, "Procurement Menu"), ProcumentNode);
        TreeNode pharmacyCreatePurchaseOrder = new DefaultTreeNode(new PrivilegeHolder(Privileges.CreatePurchaseOrder, "Create Purchase Order"), ProcumentNode);
        TreeNode pharmacyAutoOrderPModel = new DefaultTreeNode(new PrivilegeHolder(Privileges.AutoOrderPModel, "Auto Order (P Model)"), ProcumentNode);
        TreeNode pharmacyAutoOrderQModel = new DefaultTreeNode(new PrivilegeHolder(Privileges.AutoOrderQModal, "Auto Order (Q Model)"), ProcumentNode);
        TreeNode pharmacyDirectPurchase = new DefaultTreeNode(new PrivilegeHolder(Privileges.DirectPurchase, "Direct Purchase"), ProcumentNode);
        TreeNode pharmacyPurchaseOrderApprovel = new DefaultTreeNode(new PrivilegeHolder(Privileges.PurchaseOrdersApprovel, "Purchase Orders Approvel"), ProcumentNode);
        TreeNode pharmacyGoodRecipt = new DefaultTreeNode(new PrivilegeHolder(Privileges.GoodsRecipt, "Pharmacy Good Recipt"), ProcumentNode);
        TreeNode pharmacyReturnReceviedGoods = new DefaultTreeNode(new PrivilegeHolder(Privileges.ReturnReceviedGoods, "Pharmacy Return Recevied Goods"), ProcumentNode);
        TreeNode pharmacyReturnWithoutRecipt = new DefaultTreeNode(new PrivilegeHolder(Privileges.ReturnWithoutRecipt, "Pharmacy Return WIthout Recipt"), ProcumentNode);
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
        TreeNode PharmacyAdjustmentStaffStockAdjustment = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdjustmentStaffStockAdjustment, "Pharmacy Adjustment Staff Stock Adjustment"), PharmacyAdjustment);
        TreeNode PharmacyAdjustmentPurchaseRate = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdjustmentPurchaseRate, "Pharmacy Adjustment Purchase Rate"), PharmacyAdjustment);
        TreeNode PharmacyAdjustmentSaleRate = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdjustmentSaleRate, "Pharmacy Adjustment Sale Rate"), PharmacyAdjustment);
        TreeNode PharmacyAdjustmentWholeSaleRate = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdjustmentWholeSaleRate, "Pharmacy Adjustment Wholesale Rate"), PharmacyAdjustment);
        TreeNode PharmacyAdjustmentExpiaryDate = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdjustmentExpiryDate, "Pharmacy Adjustment Expiary Date"), PharmacyAdjustment);
        TreeNode PharmacyAdjustmentReports = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdjustmentSearchAdjustmentBills, "Pharmacy Adjustment Search Adjustment Bills"), PharmacyAdjustment);

        TreeNode PharmacyDisposal = new DefaultTreeNode("Pharmacy Disposal", pharmacyNode);
        TreeNode pharmacyDisposalMenu = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisposalMenue, "Pharmacy Disposal Menue"), PharmacyDisposal);
        TreeNode PharmacyDisposalIssue = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisposalIssue, "Pharmacy Disposal Issue"), PharmacyDisposal);
        TreeNode PharmacyDisposalSearchIssueBill = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisposalSearchIssueBill, "Pharmacy Disposal Search Issue Bill"), PharmacyDisposal);
        TreeNode PharmacyDisposalSearchIssueBillItems = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisposalSearchIssueBillItems, "Pharmacy Disposal Search Issue Return Bill"), PharmacyDisposal);
        TreeNode PharmacyDisposalSearchIssueReturnBill = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisposalSearchIssueReturnBill, "Pharmacy Adjustment Purchase Rate"), PharmacyDisposal);
        TreeNode PharmacyDisposalUnitIssueMargin = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDisposalUnitIssueMargin, "Pharmacy Disposal Unit Issue Margin"), PharmacyDisposal);

        // Adding Optician node and subnodes
        TreeNode opticianNode = new DefaultTreeNode(new PrivilegeHolder(null, "Optician"), allNode);

        TreeNode opticianPatientManagementNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpticianPatientManagement, "Patient Management"), opticianNode);
        TreeNode opticianAppointmentManagementNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpticianAppointmentManagement, "Appointment Management"), opticianNode);
        TreeNode opticianEmrNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpticianEmr, "EMR"), opticianNode);
        TreeNode opticianStockManagementNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpticianStockManagement, "Stock Management"), opticianNode);
        TreeNode opticianProductCatalogNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpticianProductCatalog, "Product Catalog"), opticianNode);
        TreeNode opticianRepairManagementNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpticianRepairManagement, "Repair Management"), opticianNode);

        TreeNode PharmacyItemSearch = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyItemSearch, "Item Search"), pharmacyNode);
        TreeNode PharmacyGenarateReports = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGenarateReports, "Genarate Reports"), pharmacyNode);
        TreeNode PharmacySummaryViews = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySummaryViews, "Summary Views"), pharmacyNode);
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
            JsfUtil.addErrorMessage("Please select a user");
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

    public static List<PrivilegeHolder> extractPrivileges(TreeNode[] selectedNodes) {
        List<PrivilegeHolder> privileges = new ArrayList<>();
        if (selectedNodes != null) {
            for (TreeNode node : selectedNodes) {
                Object data = node.getData();
                if (data instanceof PrivilegeHolder) {
                    privileges.add((PrivilegeHolder) data);
                }
            }
        }
        return privileges;
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

    public void fillUserPrivileges() {
        List<WebUserPrivilege> wups;
        if (currentWebUser == null) {
            JsfUtil.addErrorMessage("User?");
        }
        String j = "SELECT i "
                + " FROM WebUserPrivilege i "
                + " where i.webUser=:wu "
                + " and i.retired=:ret "
                + " and i.department=:dep";
        Map m = new HashMap();
        m.put("wu", currentWebUser);
        m.put("ret", false);
        m.put("dep", department);
        currentWebUserPrivileges = getEjbFacade().findByJpql(j, m);
        currentUserPrivilegeHolders = createPrivilegeHolders(currentWebUserPrivileges);
        unselectTreeNodes(rootTreeNode);
        checkNodes(rootTreeNode, currentUserPrivilegeHolders);
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
                    getValue(facesContext.getELContext(), null, "userPrivilegeController");
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
