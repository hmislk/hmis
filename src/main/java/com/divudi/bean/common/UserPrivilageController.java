/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * and
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.data.Privileges;
import com.divudi.data.dataStructure.PrivilageNode;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.WebUser;
import com.divudi.entity.WebUserPrivilege;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.WebUserPrivilegeFacade;
import com.divudi.facade.util.JsfUtil;
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
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class UserPrivilageController implements Serializable {

    @Inject
    SessionController sessionController;

    @EJB
    private WebUserPrivilegeFacade ejbFacade;
    @EJB
    DepartmentFacade departmentFacade;

    private static final long serialVersionUID = 1L;

    private List<WebUserPrivilege> selectedItems;

    private List<WebUserPrivilege> currentWebUserPrivileges;

    private WebUser currentWebUser;
    private TreeNode[] selectedNodes;
    private TreeNode<PrivilegeHolder> rootTreeNode;
    private Institution institution;
    private Department department;
    private List<Department> departments;
    private List<PrivilegeHolder> currentUserPrivilegeHolders;

    @Deprecated
    public void onNodeSelect(AjaxBehaviorEvent event) {
        System.out.println("onNodeSelect");
        System.out.println("event = " + event);
        TreeNode selectedNode = (TreeNode) event.getComponent().getAttributes().get("node");
        if (selectedNode != null && selectedNode.getData() instanceof PrivilageNode) {
            PrivilageNode privilageNode = (PrivilageNode) selectedNode.getData();
            Privileges selectedPrivilege = privilageNode.getP();

            String j = "SELECT i "
                    + " FROM WebUserPrivilege i "
                    + " where i.webUser=:wu and"
                    + " i.privilege=:p ";
            Map m = new HashMap();
            m.put("wu", currentWebUser);
            m.put("p", selectedPrivilege);
            WebUserPrivilege wup = getEjbFacade().findFirstByJpql(j, m);
            System.out.println("correctWup = " + wup);
            if (wup == null) {
                wup = new WebUserPrivilege();
                wup.setCreatedAt(new Date());
                wup.setCreater(sessionController.getLoggedUser());
                wup.setPrivilege(selectedPrivilege);
                wup.setWebUser(currentWebUser);
                getFacade().create(wup);
            }
            System.out.println("ttn.isSelected() = " + privilageNode.isSelected());
            if (privilageNode.isSelected()) {
                wup.setRetired(false);
            } else {
                wup.setRetired(true);
            }
            System.out.println("wup.isRetired() = " + wup.isRetired());
            getFacade().edit(wup);

            System.out.println("Selected Privilege: " + selectedPrivilege);
        }
    }

    @Deprecated
    public void onNodeUnselect(AjaxBehaviorEvent event) {
        System.out.println("onNodeSelect");
        System.out.println("event = " + event);
        TreeNode selectedNode = (TreeNode) event.getComponent().getAttributes().get("node");
        if (selectedNode != null && selectedNode.getData() instanceof PrivilageNode) {
            PrivilageNode privilageNode = (PrivilageNode) selectedNode.getData();
            Privileges selectedPrivilege = privilageNode.getP();

            String j = "SELECT i "
                    + " FROM WebUserPrivilege i "
                    + " where i.webUser=:wu and"
                    + " i.privilege=:p ";
            Map m = new HashMap();
            m.put("wu", currentWebUser);
            m.put("p", selectedPrivilege);
            WebUserPrivilege wup = getEjbFacade().findFirstByJpql(j, m);
            System.out.println("correctWup = " + wup);
            if (wup == null) {
                wup = new WebUserPrivilege();
                wup.setCreatedAt(new Date());
                wup.setCreater(sessionController.getLoggedUser());
                wup.setPrivilege(selectedPrivilege);
                wup.setWebUser(currentWebUser);
                getFacade().create(wup);
            }
            System.out.println("ttn.isSelected() = " + privilageNode.isSelected());
            if (privilageNode.isSelected()) {
                wup.setRetired(false);
            } else {
                wup.setRetired(true);
            }
            System.out.println("wup.isRetired() = " + wup.isRetired());
            getFacade().edit(wup);

            System.out.println("Selected Privilege: " + selectedPrivilege);
        }
    }

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

        TreeNode opdNode = new DefaultTreeNode(new PrivilegeHolder(null, "OPD"), root);

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
        TreeNode opdBillSearchEditNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.OpdBillSearchEdit, "OPD Bill Search Edit (Patient Details)"), opdNode);

        TreeNode inwardNode = new DefaultTreeNode(new PrivilegeHolder(null, "Inward"), root);

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

        TreeNode theatreNode = new DefaultTreeNode(new PrivilegeHolder(null, "Theatre"), root);
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

        TreeNode labNode = new DefaultTreeNode(new PrivilegeHolder(null, "Lab"), root);
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

        TreeNode pharmacyNode = new DefaultTreeNode(new PrivilegeHolder(null, "Pharmacy"), root);
        TreeNode pharmacyMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Pharmacy, "Pharmacy Menu"), pharmacyNode);
        TreeNode pharmacyAdministrationNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyAdministration, "Pharmacy Administration"), pharmacyNode);
        TreeNode pharmacyStockAdjustmentNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyStockAdjustment, "Pharmacy Stock Adjustment"), pharmacyNode);
        TreeNode pharmacyStockAdjustmentSingleItemNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyStockAdjustmentSingleItem, "Pharmacy Stock Adjustment By Single Item"), pharmacyNode);
        TreeNode pharmacyReAddToStockNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyReAddToStock, "Pharmacy Re Add To Stock"), pharmacyNode);
        TreeNode pharmacyStockIssueNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyStockIssue, "Pharmacy Stock Issue"), pharmacyNode);

        TreeNode grnNode = new DefaultTreeNode("GRN", pharmacyNode);
        TreeNode grnNodeChild = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGoodReceive, "GRN"), grnNode);
        TreeNode grnForWholesaleNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGoodReceiveWh, "GRN For Wholesale"), grnNode);
        TreeNode grnCancellingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGoodReceiveCancel, "GRN Cancelling"), grnNode);
        TreeNode grnReturnNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGoodReceiveReturn, "GRN Return"), grnNode);
        TreeNode grnEditNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyGoodReceiveEdit, "GRN Edit"), grnNode);

        TreeNode orderNode = new DefaultTreeNode("Order", pharmacyNode);
        TreeNode orderCreationNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyOrderCreation, "Order Creation"), orderNode);
        TreeNode orderApprovalNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyOrderApproval, "Order Approval"), orderNode);
        TreeNode orderCancellationNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyOrderCancellation, "Order Cancellation"), orderNode);

        TreeNode saleNode = new DefaultTreeNode("Sale", pharmacyNode);
        TreeNode pharmacySaleNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySale, "Pharmacy Sale"), saleNode);
        TreeNode pharmacySaleWithoutStockNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleWithoutStock, "Pharmacy Sale without Stock"), saleNode);
        TreeNode pharmacyWholesaleNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleWh, "Pharmacy Wholesale"), saleNode);
        TreeNode pharmacySaleCancelNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleCancel, "Pharmacy Sale Cancel"), saleNode);
        TreeNode pharmacyWholesaleCancelNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleCancelWh, "Pharmacy Wholesale Cancel"), saleNode);
        TreeNode pharmacySaleReturnNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleReturn, "Pharmacy Sale Return"), saleNode);
        TreeNode pharmacyWholesaleReturnNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySaleReturnWh, "Pharmacy Wholesale Return"), saleNode);

        TreeNode purchaseNode = new DefaultTreeNode("Purchase", pharmacyNode);
        TreeNode pharmacyPurchaseNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyPurchase, "Purchase"), purchaseNode);
        TreeNode pharmacyPurchaseWholesaleNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyPurchaseWh, "Purchase Wholesale"), purchaseNode);
        TreeNode pharmacyPurchaseCancellationNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyPurchaseCancellation, "Purchase Cancel"), purchaseNode);
        TreeNode pharmacyPurchaseReturnNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyPurchaseReturn, "Purchase Return"), purchaseNode);
        TreeNode pharmacyReturnWithoutTraisingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyReturnWithoutTraising, "Pharmacy Return Without Traising"), purchaseNode);

        TreeNode pharmacyDealerPaymentNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyDealorPayment, "Pharmacy Dealer Payment"), pharmacyNode);
        TreeNode pharmacySearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySearch, "Pharmacy Search"), pharmacyNode);
        TreeNode pharmacyReportsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyReports, "Pharmacy Reports"), pharmacyNode);
        TreeNode pharmacySummaryNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySummery, "Pharmacy Summary"), pharmacyNode);
        TreeNode pharmacyTransferNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyTransfer, "Pharmacy Transfer"), pharmacyNode);
        TreeNode pharmacySetReorderLevelNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacySetReorderLevel, "Pharmacy Set Reorder Level"), pharmacyNode);
        TreeNode pharmacyBHTIssueAcceptNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PharmacyBHTIssueAccept, "Pharmacy Accept BHT Issue"), pharmacyNode);

        ///////////////////
        TreeNode paymentNode = new DefaultTreeNode(new PrivilegeHolder(null, "Payment"), root);
        TreeNode paymentMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Payment, "Payment Menu"), paymentNode);
        TreeNode staffPaymentNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PaymentBilling, "Staff Payment"), paymentNode);
        TreeNode paymentSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PaymentBillSearch, "Payment Search"), paymentNode);
        TreeNode paymentReprintsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PaymentBillReprint, "Payment Reprints"), paymentNode);
        TreeNode paymentCancelNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PaymentBillCancel, "Payment Cancel"), paymentNode);
        TreeNode paymentRefundNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PaymentBillRefund, "Payment Refund"), paymentNode);
        TreeNode paymentReactivationNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.PaymentBillReactivation, "Payment Reactivation"), paymentNode);

        TreeNode reportsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Reports"), root);
        TreeNode reportsMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Reports, "Reports Menu"), reportsNode);
        TreeNode forOwnInstitutionNode = new DefaultTreeNode("For Own Institution", reportsNode);
        TreeNode forAllInstitutionNode = new DefaultTreeNode("For All Institution", reportsNode);

        TreeNode cashCardBillReportsOwnNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSearchCashCardOwn, "Cash/Card Bill Reports"), forOwnInstitutionNode);
        TreeNode creditBillReportsOwnNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSearchCreditOwn, "Credit Bill Reports"), forOwnInstitutionNode);
        TreeNode itemReportsOwnNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsItemOwn, "Item Reports"), forOwnInstitutionNode);

        TreeNode cashCardBillReportsOtherNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsSearchCashCardOther, "Cash/Card Bill Reports"), forAllInstitutionNode);
        TreeNode creditBillReportsOtherNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportSearchCreditOther, "Credit Bill Reports"), forAllInstitutionNode);
        TreeNode itemReportsOtherNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ReportsItemOwn, "Item Reports"), forAllInstitutionNode);

        TreeNode clinicalsNode = new DefaultTreeNode(new PrivilegeHolder(null, "Clinicals"), root);
        TreeNode clinicalDataNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Clinical, "Clinical Data"), clinicalsNode);
        TreeNode patientSummeryNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientSummery, "Patient Summery"), clinicalsNode);
        TreeNode patientDetailsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientDetails, "Patient Details"), clinicalsNode);
        TreeNode patientPhotoNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientPhoto, "Patient Photo"), clinicalsNode);
        TreeNode visitDetailsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalVisitDetail, "Visit Details"), clinicalsNode);
        TreeNode visitSummeryNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalVisitSummery, "Visit Summery"), clinicalsNode);
        TreeNode historyNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalHistory, "History"), clinicalsNode);
        TreeNode clinicaladministrationNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalAdministration, "Administration"), clinicalsNode);
        TreeNode clinicalPatientDeleteNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ClinicalPatientDelete, "Clinical Patient Delete"), clinicalsNode);

        TreeNode adminNode = new DefaultTreeNode(new PrivilegeHolder(null, "Administration"), root);
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

        TreeNode membershipNode = new DefaultTreeNode(new PrivilegeHolder(null, "Membership"), root);
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

        TreeNode humanResourceNode = new DefaultTreeNode(new PrivilegeHolder(null, "Human Resource"), root);
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

        TreeNode storeNode = new DefaultTreeNode(new PrivilegeHolder(null, "Store"), root);
        TreeNode storeMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Store, "Store Menu"), storeNode);
        TreeNode storeissueNode = new DefaultTreeNode("Issue", storeNode);
        TreeNode storeissueMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreIssue, "Issue Menu"), storeissueNode);
        TreeNode storeinwardBillingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreIssueInwardBilling, "Inward Billing"), storeissueNode);
        TreeNode storesearchIssueBillNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreIssueSearchBill, "Search Issue Bill"), storeissueNode);
        TreeNode storesearchIssueBillItemsNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StoreIssueBillItems, "Search Issue Bill Items"), storeissueNode);
        TreeNode storepurchaseNode = new DefaultTreeNode("Purchase", storeNode);
        TreeNode storepurchaseMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchase, "Purchase Menu"), purchaseNode);
        TreeNode storepurchaseOrderNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchaseOrder, "Purchase Order"), purchaseNode);
        TreeNode storepoApproveNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchaseOrderApprove, "PO Approve"), purchaseNode);
        TreeNode storestoregrnReceiveNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchaseGRNRecive, "GRN Receive"), purchaseNode);
        TreeNode storestoregrnReturnNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchaseGRNReturn, "GRN Return"), purchaseNode);
        TreeNode storepurchasePurchaseNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchasePurchase, "Purchase"), purchaseNode);
        TreeNode storepoApproveSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.StorePurchaseOrderApproveSearch, "PO Approve Search"), purchaseNode);
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

        TreeNode searchRootNode = new DefaultTreeNode(new PrivilegeHolder(null, "Search"), root);
        TreeNode searchMenuRootNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Search, "Search Menu"), searchRootNode);
        TreeNode grandSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.SearchGrand, "Grand Search"), searchRootNode);

        TreeNode userNode = new DefaultTreeNode(new PrivilegeHolder(null, "User"), root);
        TreeNode changeThemeNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChangePreferece, "Change Theme"), userNode);

        TreeNode cashTransactionNode = new DefaultTreeNode(new PrivilegeHolder(null, "Cash Transaction"), root);
        TreeNode cashTransactionMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.CashTransaction, "Cash Transaction Menu"), cashTransactionNode);
        TreeNode cashInNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.CashTransactionCashIn, "Cash In"), cashTransactionNode);
        TreeNode cashOutNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.CashTransactionCashOut, "Cash Out"), cashTransactionNode);
        TreeNode listToCashReceiveNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.CashTransactionListToCashRecieve, "List To Cash Receive"), cashTransactionNode);

        TreeNode channellingNode = new DefaultTreeNode(new PrivilegeHolder(null, "Channelling"), root);
        TreeNode channellingMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.Channelling, "Channelling Menu"), channellingNode);
        TreeNode channelBookingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingChannelBooking, "Channel Booking"), channellingNode);
        TreeNode channelfutureBookingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingFutureChannelBooking, "Channel Future Booking"), channellingNode);
        TreeNode channelpastBookingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPastBooking, "Past Booking"), channellingNode);
        TreeNode channelbookedListNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingBookedList, "Booked List"), channellingNode);
        TreeNode channeldoctorLeaveMenuNode = new DefaultTreeNode("Doctor Leave Menu", channellingNode);
        TreeNode channeldoctorLeaveByDateNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingDoctorLeaveByDate, "Doctor Leave By Date"), channeldoctorLeaveMenuNode);
        TreeNode channeldoctorLeaveBySessionNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingDoctorLeaveByServiceSession, "Doctor Leave By Service Session"), channeldoctorLeaveMenuNode);
        TreeNode channelchannelSchedulingNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingChannelSheduling, "Channel Scheduling"), channellingNode);
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
        TreeNode channelpaymentMenuNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPayment, "Payment Menu"), paymentNode);
        TreeNode channelpayDoctorNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPaymentPayDoctor, "Pay Doctor"), paymentNode);
        TreeNode channelpaymentDueSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPaymentDueSearch, "Payment Due Search"), paymentNode);
        TreeNode channelpaymentDoneSearchNode = new DefaultTreeNode(new PrivilegeHolder(Privileges.ChannellingPaymentDoneSearch, "Payment Done Search"), paymentNode);
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

        return root;
    }

    @Deprecated
    private TreeNode createTreeNode() {
        TreeNode tmproot = new PrivilageNode("Root", null);
        TreeNode node0 = new PrivilageNode("OPD", tmproot);
        TreeNode node00 = new PrivilageNode("Billing Menu", node0, Privileges.Opd);
        TreeNode node01 = new PrivilageNode("Bill", node0, Privileges.OpdBilling);
        TreeNode node01a = new PrivilageNode("Pre Billing", node0, Privileges.OpdPreBilling);
        TreeNode node01aa = new PrivilageNode("Collecting Centre Billing", node0, Privileges.OpdCollectingCentreBilling);
        TreeNode node02 = new PrivilageNode("Bill Search", node0, Privileges.OpdBillSearch);
        TreeNode node03 = new PrivilageNode("Bill Item Search", node0, Privileges.OpdBillItemSearch);
        TreeNode node04 = new PrivilageNode("Reprint", node0, Privileges.OpdReprint);
        TreeNode node05 = new PrivilageNode("Cancel", node0, Privileges.OpdCancel);
        TreeNode node06 = new PrivilageNode("Return", node0, Privileges.OpdReturn);
        TreeNode node07 = new PrivilageNode("Reactivate", node0, Privileges.OpdReactivate);
        TreeNode node08 = new PrivilageNode("OPD Bill Search Edit(Patient Details)", node0, Privileges.OpdBillSearchEdit);

        TreeNode node1 = new PrivilageNode("Inward", tmproot);
        TreeNode node10 = new PrivilageNode("Inward Menu", node1, Privileges.Inward);
        TreeNode node11 = new PrivilageNode("Addmissions", node1);
        TreeNode node110 = new PrivilageNode("Admission Menu", node11, Privileges.InwardAdmissions);
        TreeNode node111 = new PrivilageNode("Admission", node11, Privileges.InwardAdmissionsAdmission);
        TreeNode node112 = new PrivilageNode("Edit Admission Details", node11, Privileges.InwardAdmissionsEditAdmission);
        TreeNode node113 = new PrivilageNode("Inward Appoinment", node11, Privileges.InwardAdmissionsInwardAppoinment);
        TreeNode node12 = new PrivilageNode("Room", node1);
        TreeNode node120 = new PrivilageNode("Room Menu", node12, Privileges.InwardRoom);
        TreeNode node121 = new PrivilageNode("Room Occupency", node12, Privileges.InwardRoomRoomOccupency);
        TreeNode node122 = new PrivilageNode("Room Change", node12, Privileges.InwardRoomRoomChange);
        TreeNode node123 = new PrivilageNode("Gurdian Room Change", node12, Privileges.InwardRoomGurdianRoomChange);
        TreeNode node124 = new PrivilageNode("Dischage Room in Room Ocupency", node12, Privileges.InwardRoomDischarge);
        TreeNode node13 = new PrivilageNode("Services & Items", node1);
        TreeNode node130 = new PrivilageNode("Services & Items", node13, Privileges.InwardServicesAndItems);
        TreeNode node131 = new PrivilageNode("Add Services", node13, Privileges.InwardServicesAndItemsAddServices);
        TreeNode node132 = new PrivilageNode("Add Out Side Charges", node13, Privileges.InwardServicesAndItemsAddOutSideCharges);
        TreeNode node133 = new PrivilageNode("Add Professional Fee", node13, Privileges.InwardServicesAndItemsAddProfessionalFee);
        TreeNode node134 = new PrivilageNode("Add Timed Services", node13, Privileges.InwardServicesAndItemsAddTimedServices);
        TreeNode node14 = new PrivilageNode("Billing", node1);
        TreeNode node140 = new PrivilageNode("Billing Menu", node14, Privileges.InwardBilling);
        TreeNode node141 = new PrivilageNode("Interim Bill", node14, Privileges.InwardBillingInterimBill);
        TreeNode node142 = new PrivilageNode("Interim Bill Search", node14, Privileges.InwardBillingInterimBillSearch);
        TreeNode node143 = new PrivilageNode("Edit Patient Name After Payment Finalized", node14, Privileges.InwardFinalBillReportEdit);
        TreeNode node19 = new PrivilageNode("Pharmacy", node1);
        TreeNode node190 = new PrivilageNode("Pharmacy Menu", node19, Privileges.InwardPharmacyMenu);
        TreeNode node191 = new PrivilageNode("Pharmacy Issue Request", node19, Privileges.InwardPharmacyIssueRequest);
        TreeNode node192 = new PrivilageNode("Pharmacy Issue Request Search", node19, Privileges.InwardPharmacyIssueRequestSearch);
        TreeNode node15 = new PrivilageNode("Search", node1);
        TreeNode node150 = new PrivilageNode("Serch Menu", node15, Privileges.InwardSearch);
        TreeNode node151 = new PrivilageNode("Serch Service Bill", node15, Privileges.InwardSearchServiceBill);
        TreeNode node152 = new PrivilageNode("Serch Professional Bill", node15, Privileges.InwardSearchProfessionalBill);
        TreeNode node153 = new PrivilageNode("Serch Final Bill", node15, Privileges.InwardSearchFinalBill);
        TreeNode node16 = new PrivilageNode("Reports", node1, Privileges.InwardReport);
        TreeNode node17 = new PrivilageNode("Administration", node1, Privileges.InwardAdministration);
        TreeNode node18 = new PrivilageNode("Aditional Privilages", node1);
        TreeNode node180 = new PrivilageNode("Additional Privilage Menu", node18, Privileges.InwardAdditionalPrivilages);
        TreeNode node181 = new PrivilageNode("Search Bills", node18, Privileges.InwardBillSearch);
        TreeNode node182 = new PrivilageNode("Search Bill Items", node18, Privileges.InwardBillItemSearch);
        TreeNode node183 = new PrivilageNode("Reprint", node18, Privileges.InwardBillReprint);
        TreeNode node184 = new PrivilageNode("Cancel", node18, Privileges.InwardCancel);
        TreeNode node185 = new PrivilageNode("Return", node18, Privileges.InwardReturn);
        TreeNode node186 = new PrivilageNode("Reactivate", node18, Privileges.InwardReactivate);
        TreeNode node187 = new PrivilageNode("Show Inward Fee", node18, Privileges.ShowInwardFee);
        TreeNode node188 = new PrivilageNode("Inward Check", node18, Privileges.InwardCheck);
        TreeNode node189 = new PrivilageNode("Inward Uncheck", node18, Privileges.InwardUnCheck);
        TreeNode node1810 = new PrivilageNode("Inward Final Bill Cancel With Out Check Date Range", node18, Privileges.InwardFinalBillCancel);
        TreeNode node1811 = new PrivilageNode("Inward Out Side Bill Mark as Un-Paid", node18, Privileges.InwardOutSideMarkAsUnPaid);
        TreeNode node1812 = new PrivilageNode("Inward Bill Settle With Out Check", node18, Privileges.InwardBillSettleWithoutCheck);

        TreeNode node1a = new PrivilageNode("Theatre", tmproot);
        TreeNode node1a0 = new PrivilageNode("Theatre Menu", node1a, Privileges.Theatre);
        TreeNode node1a2 = new PrivilageNode("Add Surgery", node1a, Privileges.TheatreAddSurgery);
        TreeNode node1a1 = new PrivilageNode("Theatre Billing", node1a, Privileges.TheatreBilling);
        TreeNode node1a3 = new PrivilageNode("Theatre Transfer Menu Item", node1a, Privileges.TheaterTransfer);
        TreeNode node1a4 = new PrivilageNode("Theatre Transfer Request", node1a, Privileges.TheaterTransferRequest);
        TreeNode node1a5 = new PrivilageNode("Theatre Transfer Issue", node1a, Privileges.TheaterTransferIssue);
        TreeNode node1a6 = new PrivilageNode("Theatre Transfer Recieve", node1a, Privileges.TheaterTransferRecieve);
        TreeNode node1a7 = new PrivilageNode("Theatre Transfer Report", node1a, Privileges.TheaterTransferReport);
        TreeNode node1a8 = new PrivilageNode("Theatre Show Reports Menu Item", node1a, Privileges.TheaterReports);
        TreeNode node1a9 = new PrivilageNode("Theatre Show Summery Menu Item", node1a, Privileges.TheaterSummeries);
        TreeNode node1a10 = new PrivilageNode("Theatre BHT Issue", node1a);
        TreeNode node1a100 = new PrivilageNode("Theatre BHT Issue", node1a10, Privileges.TheaterIssue);
        TreeNode node1a101 = new PrivilageNode("Pharmacy BHT Issue", node1a10, Privileges.TheaterIssuePharmacy);
        TreeNode node1a102 = new PrivilageNode("General BHT Issue", node1a10, Privileges.TheaterIssueStore);
        TreeNode node1a1021 = new PrivilageNode("Inward BHT Billing", node1a102, Privileges.TheaterIssueStoreBhtBilling);
        TreeNode node1a1022 = new PrivilageNode("Search BHT Issue Bill", node1a102, Privileges.TheaterIssueStoreBhtSearchBill);
        TreeNode node1a1023 = new PrivilageNode("Search BHT Issue Bill Items ", node1a102, Privileges.TheaterIssueStoreBhtSearchBillItem);
        TreeNode node1a11 = new PrivilageNode("Opd Issue", node1a, Privileges.TheaterIssueOpd);
        TreeNode node1a12 = new PrivilageNode("Opd Issue For Cashier", node1a11, Privileges.TheaterIssueOpdForCasheir);
        TreeNode node1a13 = new PrivilageNode("Opd Issue Search Pre Bill", node1a11, Privileges.TheaterIssueOpdSearchPreBill);
        TreeNode node1a14 = new PrivilageNode("Opd Issue Return Item Only", node1a11, Privileges.TheaterIssueOpdSearchPreBillForReturnItemOnly);
        TreeNode node1a15 = new PrivilageNode("Opd Issue Search Pre Bill Return", node1a11, Privileges.TheaterIssueOpdSearchPreBillReturn);
        TreeNode node1a16 = new PrivilageNode("Opd Issue Pre Bill Add To Stock", node1a11, Privileges.TheaterIssueOpdSearchPreBillAddToStock);

        TreeNode node2 = new PrivilageNode("Lab", tmproot);
        TreeNode node200 = new PrivilageNode("Lab Menu", node2, Privileges.Lab);
        TreeNode node201 = new PrivilageNode("Billing Menu", node2);
        TreeNode node217 = new PrivilageNode("Lab Bill", node201, Privileges.LabBilling);
        TreeNode node201a = new PrivilageNode("Lab Bill Search", node201, Privileges.LabBillSearch);
        TreeNode node201b = new PrivilageNode("Lab Bill Item Search", node201, Privileges.LabBillItemSearch);
        TreeNode node218 = new PrivilageNode("Lab Bill Search", node2, Privileges.LabBillSearchCashier);
        TreeNode node202 = new PrivilageNode("Search Bills", node2, Privileges.LabBillSearch);
        TreeNode node203 = new PrivilageNode("Lab Report Search", node2, Privileges.LabReportSearch);
        TreeNode node204 = new PrivilageNode("Patient Edit", node2, Privileges.LabEditPatient);
        TreeNode node205 = new PrivilageNode("Lab Bill Reprint", node2, Privileges.LabBillReprint);
        TreeNode node206 = new PrivilageNode("Lab Bill Return", node2, Privileges.LabBillReturning);
        TreeNode node207 = new PrivilageNode("Lab Bill Cancel", node2, Privileges.LabBillCancelling);
        TreeNode node226 = new PrivilageNode("CC Bill Cancel", node2, Privileges.CollectingCentreCancelling);
        TreeNode node208 = new PrivilageNode("Reactivate", node2, Privileges.LabBillReactivating);
        TreeNode node209 = new PrivilageNode("Sample Collection", node2, Privileges.LabSampleCollecting);
        TreeNode node210 = new PrivilageNode("Sample Receive", node2, Privileges.LabSampleReceiving);
        TreeNode node211 = new PrivilageNode("DataEntry", node2, Privileges.LabDataentry);
        TreeNode node212 = new PrivilageNode("Autherize", node2, Privileges.LabAutherizing);
        TreeNode node213 = new PrivilageNode("De-Autherize", node2, Privileges.LabDeAutherizing);
        TreeNode node214 = new PrivilageNode("Report Print", node2, Privileges.LabPrinting);
        TreeNode node214a = new PrivilageNode("Report Reprint", node214, Privileges.LabReprinting);
        TreeNode node215 = new PrivilageNode("Lab Report Formats Editing", node2, Privileges.LabReportFormatEditing);
        TreeNode node215a = new PrivilageNode("Report Edit After Authorized", node2, Privileges.LabReportEdit);
        TreeNode node216 = new PrivilageNode("Lab Summeries", node2);
        TreeNode node216a = new PrivilageNode("Lab Summeries Menu", node216, Privileges.LabSummeries);
        TreeNode node216b = new PrivilageNode("Lab Summeries Level1", node216, Privileges.LabSummeriesLevel1);
        TreeNode node216c = new PrivilageNode("Lab Summeries Level2", node216, Privileges.LabSummeriesLevel2);
        TreeNode node216d = new PrivilageNode("Lab Summeries Level3", node216, Privileges.LabSummeriesLevel3);
        TreeNode node221 = new PrivilageNode("Lab Investigation Fees", node2, Privileges.LabInvestigationFee);
        TreeNode node222 = new PrivilageNode("Lab Bill Cancell Special(after collecting sample can cancell)", node2, Privileges.LabBillCancelSpecial);
        TreeNode node223 = new PrivilageNode("Lab Bill Refund Special(after collecting sample can Refund)", node2, Privileges.LabBillRefundSpecial);
        TreeNode node224 = new PrivilageNode("Add Inward Services", node2, Privileges.LabAddInwardServices);
        TreeNode node225 = new PrivilageNode("Search By Logged Institution", node2, Privileges.LabReportSearchByLoggedInstitution);
        TreeNode node227 = new PrivilageNode("Lab Administration", node2);
        TreeNode node227a = new PrivilageNode("Lab Administration Menu", node227, Privileges.LabAdiministrator);
        TreeNode node227b = new PrivilageNode("Mannage Items Menu", node227, Privileges.LabItems);
        TreeNode node227ba = new PrivilageNode("Mannage Item Fee Update", node227, Privileges.LabItemFeeUpadate);
        TreeNode node227bb = new PrivilageNode("Mannage Item Fee Delete", node227, Privileges.LabItemFeeDelete);
        TreeNode node227c = new PrivilageNode("Mannage Reports Menu", node227, Privileges.LabReports);
        TreeNode node227d = new PrivilageNode("Lists Menu", node227, Privileges.LabLists);
        TreeNode node227e = new PrivilageNode("Setup Menu", node227, Privileges.LabSetUp);
        TreeNode node228 = new PrivilageNode("Lab Inward Billing Menu", node2);
        TreeNode node228a = new PrivilageNode("Lab Inward Bill", node228, Privileges.LabInwardBilling);
        TreeNode node228b = new PrivilageNode("Lab Inward Bill Search", node228, Privileges.LabInwardSearchServiceBill);
        TreeNode node229 = new PrivilageNode("Lab Collecting Center Billing", node2);
        TreeNode node229a = new PrivilageNode("Lab Collecting Center Menu", node229, Privileges.LabCollectingCentreBilling);
        TreeNode node229b = new PrivilageNode("Lab Collecting center Billing", node229, Privileges.LabCCBilling);
        TreeNode node229c = new PrivilageNode("Lab Collecting Center Bill search", node229, Privileges.LabCCBillingSearch);
        TreeNode node230 = new PrivilageNode("Lab Reporting", node2, Privileges.LabReporting);

        TreeNode node3 = new PrivilageNode("Pharmacy", tmproot);
        TreeNode node300 = new PrivilageNode("Pharmacy Menu", node3, Privileges.Pharmacy);
        TreeNode node301 = new PrivilageNode("Pharmacy Administration", node3, Privileges.PharmacyAdministration);
        TreeNode node306 = new PrivilageNode("Pharmacy Stock Adjustment", node3, Privileges.PharmacyStockAdjustment);
        TreeNode node306a = new PrivilageNode("Pharmacy Stock Adjustment By Single Item", node3, Privileges.PharmacyStockAdjustmentSingleItem);
        TreeNode node307 = new PrivilageNode("Pharmacy Re Add To Stock", node3, Privileges.PharmacyReAddToStock);
        TreeNode node314 = new PrivilageNode("Pharmacy Stock Issue", node3, Privileges.PharmacyStockIssue);

        ///////////////////////
        TreeNode node302 = new PrivilageNode("GRN", node3);
        TreeNode node3021 = new PrivilageNode("GRN", node302, Privileges.PharmacyGoodReceive);
        TreeNode node3021a = new PrivilageNode("GRN For Wholesale", node302, Privileges.PharmacyGoodReceiveWh);
        TreeNode node3022 = new PrivilageNode("GRN Cancelling", node302, Privileges.PharmacyGoodReceiveCancel);
        TreeNode node3023 = new PrivilageNode("GRN Return", node302, Privileges.PharmacyGoodReceiveReturn);
        TreeNode node3024 = new PrivilageNode("GRN Edit", node302, Privileges.PharmacyGoodReceiveEdit);
        ///////////////////////
        TreeNode node303 = new PrivilageNode("Order", node3);
        TreeNode node3031 = new PrivilageNode("Order Creation", node303, Privileges.PharmacyOrderCreation);
        TreeNode node3032 = new PrivilageNode("Order Aproval", node303, Privileges.PharmacyOrderApproval);
        TreeNode node3033 = new PrivilageNode("Order Cancellation", node303, Privileges.PharmacyOrderCancellation);
        //////////////////
        TreeNode node304 = new PrivilageNode("Sale", node3);
        TreeNode node3041 = new PrivilageNode("Pharmacy Sale", node304, Privileges.PharmacySale);
        TreeNode node3041aa = new PrivilageNode("Pharmacy Sale without Stock", node304, Privileges.PharmacySaleWithoutStock);
        TreeNode node3041a = new PrivilageNode("Pharmacy Wholesale", node304, Privileges.PharmacySaleWh);
        TreeNode node3042 = new PrivilageNode("Pharmacy Sale Cancel", node304, Privileges.PharmacySaleCancel);
        TreeNode node3042a = new PrivilageNode("Pharmacy Wholesale Cancel", node304, Privileges.PharmacySaleCancelWh);
        TreeNode node3043 = new PrivilageNode("Pharmacy Sale Return", node304, Privileges.PharmacySaleReturn);
        TreeNode node3043a = new PrivilageNode("Pharmacy Wholesale Return", node304, Privileges.PharmacySaleReturnWh);
//////////////////
        TreeNode node305 = new PrivilageNode("Purchase", node3);
        TreeNode node3051 = new PrivilageNode("Purchase", node305, Privileges.PharmacyPurchase);
        TreeNode node3051a = new PrivilageNode("Purchase Wholesale", node305, Privileges.PharmacyPurchaseWh);
        TreeNode node3052 = new PrivilageNode("Purchase Cancel", node305, Privileges.PharmacyPurchaseCancellation);
        TreeNode node3053 = new PrivilageNode("Purchase Return", node305, Privileges.PharmacyPurchaseReturn);
        TreeNode node3054 = new PrivilageNode("Pharmacy Return Without Traising", node305, Privileges.PharmacyReturnWithoutTraising);
        ///////////////////
        TreeNode node308 = new PrivilageNode("Pharmacy Dealor Payment", node3, Privileges.PharmacyDealorPayment);
        TreeNode node309 = new PrivilageNode("Pharmacy Search", node3, Privileges.PharmacySearch);
        TreeNode node310 = new PrivilageNode("Pharmacy Reports", node3, Privileges.PharmacyReports);
        TreeNode node311 = new PrivilageNode("Pharmacy Summery", node3, Privileges.PharmacySummery);
        TreeNode node312 = new PrivilageNode("Pharmacy Transfer", node3, Privileges.PharmacyTransfer);
        TreeNode node313 = new PrivilageNode("Pharmacy Set Reorder Level", node3, Privileges.PharmacySetReorderLevel);
        TreeNode node315 = new PrivilageNode("Pharmacy Accept BHT Issue", node3, Privileges.PharmacyBHTIssueAccept);

        ///////////////////
        TreeNode node4 = new PrivilageNode("Payment", tmproot);
        TreeNode node400 = new PrivilageNode("Payment Menu", node4, Privileges.Payment);
        TreeNode node401 = new PrivilageNode("Staff Payment", node4, Privileges.PaymentBilling);
        TreeNode node402 = new PrivilageNode("Payment Search", node4, Privileges.PaymentBillSearch);
        TreeNode node403 = new PrivilageNode("Payment Reprints", node4, Privileges.PaymentBillReprint);
        TreeNode node404 = new PrivilageNode("Payment Cancel", node4, Privileges.PaymentBillCancel);
        TreeNode node405 = new PrivilageNode("Payment Refund", node4, Privileges.PaymentBillRefund);
        TreeNode node406 = new PrivilageNode("Payment Reactivation", node4, Privileges.PaymentBillReactivation);

        TreeNode node5 = new PrivilageNode("Reports", tmproot);
        TreeNode node53 = new PrivilageNode("Reports Menu", node5, Privileges.Reports);
        TreeNode node51 = new PrivilageNode("For Own Institution", node5);
        TreeNode node52 = new PrivilageNode("For All Institution", node5);

        TreeNode node510 = new PrivilageNode("Cash/Card Bill Reports", node51, Privileges.ReportsSearchCashCardOwn);
        TreeNode node511 = new PrivilageNode("Credit Bill Reports", node51, Privileges.ReportsSearchCreditOwn);
        TreeNode node512 = new PrivilageNode("Item Reports", node51, Privileges.ReportsItemOwn);

        TreeNode node520 = new PrivilageNode("Cash/Card Bill Reports", node52, Privileges.ReportsSearchCashCardOther);
        TreeNode node521 = new PrivilageNode("Credit Bill Reports", node52, Privileges.ReportSearchCreditOther);
        TreeNode node522 = new PrivilageNode("Item Reports", node52, Privileges.ReportsItemOwn);

        TreeNode node7 = new PrivilageNode("Clinicals", tmproot);
        TreeNode node700 = new PrivilageNode("Clinical Data", node7, Privileges.Clinical);
        TreeNode node701 = new PrivilageNode("Patient Summery", node7, Privileges.ClinicalPatientSummery);
        TreeNode node702 = new PrivilageNode("Patient Details", node7, Privileges.ClinicalPatientDetails);
        TreeNode node703 = new PrivilageNode("Patient Photo", node7, Privileges.ClinicalPatientPhoto);
        TreeNode node704 = new PrivilageNode("Visit Details", node7, Privileges.ClinicalVisitDetail);
        TreeNode node705 = new PrivilageNode("Visit Summery", node7, Privileges.ClinicalVisitSummery);
        TreeNode node706 = new PrivilageNode("History", node7, Privileges.ClinicalHistory);
        TreeNode node707 = new PrivilageNode("Administration", node7, Privileges.ClinicalAdministration);
        TreeNode node708 = new PrivilageNode("Clinical Patient Delete", node7, Privileges.ClinicalPatientDelete);

        TreeNode node6 = new PrivilageNode("Administration", tmproot);
        TreeNode node60 = new PrivilageNode("Admin Menu", node6, Privileges.Admin);
        TreeNode node61 = new PrivilageNode("Manage Users", node6, Privileges.AdminManagingUsers);
        TreeNode node62 = new PrivilageNode("Manage Institutions", node6, Privileges.AdminInstitutions);
        TreeNode node63 = new PrivilageNode("Manage Staff", node6, Privileges.AdminStaff);
        TreeNode node64 = new PrivilageNode("Manage Items/Services", node6, Privileges.AdminItems);
        TreeNode node65 = new PrivilageNode("Manage Fees/Prices/Packages", node6, Privileges.AdminPrices);
        TreeNode node65a = new PrivilageNode("Filter Without Department", node6, Privileges.AdminFilterWithoutDepartment);
        TreeNode node68 = new PrivilageNode("Search All", node6, Privileges.SearchAll);
        TreeNode node81 = new PrivilageNode("Change Professional Fee", node6, Privileges.ChangeProfessionalFee);
        TreeNode node82 = new PrivilageNode("Change Professional Fee", node6, Privileges.ChangeCollectingCentre);
        TreeNode node69 = new PrivilageNode("Send Bulk SMS", node6, Privileges.SendBulkSMS);
        TreeNode node67 = new PrivilageNode("Only For Developers(Don't Add That)", node6, Privileges.Developers);

        TreeNode node66 = new PrivilageNode("Membership", tmproot);
        TreeNode node660 = new PrivilageNode("Membership Menu", node66, Privileges.MemberShip);
        TreeNode node6601 = new PrivilageNode("Add Members", node66, Privileges.MemberShipAdd);
        TreeNode node6602 = new PrivilageNode("Edit Members", node66, Privileges.MemberShipEdit);
        TreeNode node6603 = new PrivilageNode("Membership Reports", node66, Privileges.MembershipReports);
        TreeNode node6604 = new PrivilageNode("Membership Discount Management", node66, Privileges.MembershipDiscountManagement);
        TreeNode node6605 = new PrivilageNode("Membership Administration", node66, Privileges.MembershipAdministration);
        TreeNode node6606 = new PrivilageNode("Other", node66);

        TreeNode node661 = new PrivilageNode("Membership Schemes", node6606, Privileges.MembershipSchemes);
        TreeNode node6620 = new PrivilageNode("Inward Membership Menu", node6606, Privileges.MemberShipInwardMemberShip);
        TreeNode node6621 = new PrivilageNode("Schemes Dicounts", node6606, Privileges.MemberShipInwardMemberShipSchemesDicounts);
        TreeNode node6622 = new PrivilageNode("Inward Membership Report", node6606, Privileges.MemberShipInwardMemberShipInwardMemberShipReport);
        TreeNode node6630 = new PrivilageNode("Opd MemberShip Dis Menu", node6606, Privileges.MemberShipOpdMemberShipDis);
        TreeNode node6631 = new PrivilageNode("Discount By Department", node6606, Privileges.MemberShipOpdMemberShipDisByDepartment);
        TreeNode node6632 = new PrivilageNode("Discount By Category", node6606, Privileges.MemberShipOpdMemberShipDisByCategory);
        TreeNode node6633 = new PrivilageNode("Opd MemberShip Report", node6606, Privileges.MemberShipOpdMemberShipDisOpdMemberShipReport);
        TreeNode node664 = new PrivilageNode("Re-Activate Registed Patient", node6606, Privileges.MemberShipMemberReActive);
        TreeNode node665 = new PrivilageNode("De-Activate Registed Patient", node6606, Privileges.MemberShipMemberDeActive);

        TreeNode node9 = new PrivilageNode("Human Resource", tmproot);
        TreeNode node91 = new PrivilageNode("HR Menu", node9, Privileges.Hr);
        TreeNode node92 = new PrivilageNode("Working Time", node9);
        TreeNode node920 = new PrivilageNode("Working Time Menu", node92, Privileges.HrWorkingTime);
        TreeNode node921 = new PrivilageNode("Roster Table", node92, Privileges.HrRosterTable);
        TreeNode node922 = new PrivilageNode("Upload Attendence", node92, Privileges.HrUploadAttendance);
        TreeNode node923 = new PrivilageNode("Analyse Attendence By Roster", node92, Privileges.HrAnalyseAttendenceByRoster);
        TreeNode node924 = new PrivilageNode("Analyse Attendence By Staff", node92, Privileges.HrAnalyseAttendenceByStaff);
        TreeNode node93 = new PrivilageNode("Form", node9);
        TreeNode node930 = new PrivilageNode("Form Menu", node93, Privileges.HrForms);
        TreeNode node931 = new PrivilageNode("Leave Form", node93, Privileges.HrLeaveForms);
        TreeNode node932 = new PrivilageNode("Additional Form", node93, Privileges.HrAdditionalForms);
        TreeNode node94 = new PrivilageNode("HR Salary Advance", node9, Privileges.HrAdvanceSalary);
        TreeNode node95 = new PrivilageNode("HR Salary", node9);
        TreeNode node95a = new PrivilageNode("HR Salary Generate", node95, Privileges.HrGenerateSalary);
        TreeNode node95b = new PrivilageNode("HR Salary Generate Special", node95, Privileges.HrGenerateSalarySpecial);
        TreeNode node96 = new PrivilageNode("HR Salary Print", node9, Privileges.HrPrintSalary);
        TreeNode node97 = new PrivilageNode("HR Reports", node9);
        TreeNode node970 = new PrivilageNode("HR Reports Menu", node97, Privileges.HrReports);
        TreeNode node971 = new PrivilageNode("HR Reports Level 1", node97, Privileges.HrReportsLevel1);
        TreeNode node972 = new PrivilageNode("HR Reports Level 2", node97, Privileges.HrReportsLevel2);
        TreeNode node973 = new PrivilageNode("HR Reports Level 3", node97, Privileges.HrReportsLevel3);
//        TreeNode node974 = new PrivilageNode("HR Employee History Reports", node97, Privileges.EmployeeHistoryReport);
        TreeNode node98 = new PrivilageNode("HR Administration", node9);
        TreeNode node980 = new PrivilageNode("HR Administration Menu", node98, Privileges.HrAdmin);
        TreeNode node981 = new PrivilageNode("HR Delete Late Leave", node98, Privileges.hrDeleteLateLeave);
        TreeNode node982 = new PrivilageNode("HR Edit Retied Date", node98, Privileges.HrEditRetiedDate);
        TreeNode node983 = new PrivilageNode("HR Remove Resign Date", node98, Privileges.HrRemoveResignDate);

        TreeNode node20 = new PrivilageNode("Store", tmproot);
        TreeNode node2000 = new PrivilageNode("Store Menu", node20, Privileges.Store);
        TreeNode node2001 = new PrivilageNode("Issue", node20);
        TreeNode node20010 = new PrivilageNode("Issue Menu", node2001, Privileges.StoreIssue);
        TreeNode node20011 = new PrivilageNode("Inward Billing", node2001, Privileges.StoreIssueInwardBilling);
        TreeNode node20012 = new PrivilageNode("Search Issue Bill", node2001, Privileges.StoreIssueSearchBill);
        TreeNode node20013 = new PrivilageNode("Search Issue Bill Items", node2001, Privileges.StoreIssueBillItems);
        TreeNode node2002 = new PrivilageNode("Purchase", node20);
        TreeNode node20020 = new PrivilageNode("Purchase Menu", node2002, Privileges.StorePurchase);
        TreeNode node20021 = new PrivilageNode("Purchase Order", node2002, Privileges.StorePurchaseOrder);
        TreeNode node20022 = new PrivilageNode("PO Approve", node2002, Privileges.StorePurchaseOrderApprove);
        TreeNode node20023 = new PrivilageNode("GRN Recive", node2002, Privileges.StorePurchaseGRNRecive);
        TreeNode node20024 = new PrivilageNode("GRN Return", node2002, Privileges.StorePurchaseGRNReturn);
        TreeNode node20025 = new PrivilageNode("Purchase", node2002, Privileges.StorePurchasePurchase);
        TreeNode node20026 = new PrivilageNode("PO Approve Search", node2002, Privileges.StorePurchaseOrderApproveSearch);
        TreeNode node2003 = new PrivilageNode("Transfer", node20);
        TreeNode node20030 = new PrivilageNode("Transfer Menu", node2003, Privileges.StoreTransfer);
        TreeNode node20031 = new PrivilageNode("Request", node2003, Privileges.StoreTransferRequest);
        TreeNode node20032 = new PrivilageNode("Issue", node2003, Privileges.StoreTransferIssue);
        TreeNode node20033 = new PrivilageNode("Recive", node2003, Privileges.StoreTransferRecive);
        TreeNode node20034 = new PrivilageNode("Report", node2003, Privileges.StoreTransferReport);
        TreeNode node2004 = new PrivilageNode("Ajustment", node20);
        TreeNode node20040 = new PrivilageNode("Adjustment Menu", node2004, Privileges.StoreAdjustment);
        TreeNode node20041 = new PrivilageNode("Department Stock(Qty)", node2004, Privileges.StoreAdjustmentDepartmentStock);
        TreeNode node20042 = new PrivilageNode("Staff Stock Adjustment", node2004, Privileges.StoreAdjustmentStaffStock);
        TreeNode node20043 = new PrivilageNode("Purchase Rate", node2004, Privileges.StoreAdjustmentPurchaseRate);
        TreeNode node20044 = new PrivilageNode("Sale Rate", node2004, Privileges.StoreAdjustmentSaleRate);
        TreeNode node2005 = new PrivilageNode("Delor Payment", node20);
        TreeNode node20050 = new PrivilageNode("Delor Payment Menu", node2005, Privileges.StoreDealorPayment);
        TreeNode node20051 = new PrivilageNode("Delor Due Search", node2005, Privileges.StoreDealorPaymentDueSearch);
        TreeNode node20052 = new PrivilageNode("Delor Due By Age", node2005, Privileges.StoreDealorPaymentDueByAge);
        TreeNode node20053 = new PrivilageNode("Payment", node2005);
        TreeNode node200530 = new PrivilageNode("Payment Menu", node20053, Privileges.StoreDealorPaymentPayment);
        TreeNode node200531 = new PrivilageNode("GRN Payment", node20053, Privileges.StoreDealorPaymentPaymentGRN);
        TreeNode node200532 = new PrivilageNode("GRN Payment(Select)", node20053, Privileges.StoreDealorPaymentPaymentGRNSelect);
        TreeNode node20054 = new PrivilageNode("GRN Payment Due Search", node2005, Privileges.StoreDealorPaymentGRNDoneSearch);
        TreeNode node2006 = new PrivilageNode("Search", node20);
        TreeNode node20060 = new PrivilageNode("Search Menu", node2006, Privileges.StoreSearch);
        TreeNode node2007 = new PrivilageNode("Report", node20);
        TreeNode node20070 = new PrivilageNode("Report Menu", node2007, Privileges.StoreReports);
        TreeNode node2008 = new PrivilageNode("Summery", node20);
        TreeNode node20080 = new PrivilageNode("Summery Menu", node2008, Privileges.StoreSummery);
        TreeNode node2009 = new PrivilageNode("Administration", node20);
        TreeNode node20090 = new PrivilageNode("Administration Menu", node2009, Privileges.StoreAdministration);

        TreeNode node21 = new PrivilageNode("Search", tmproot);
        TreeNode node2100 = new PrivilageNode("Search Menu", node21, Privileges.Search);
        TreeNode node2101 = new PrivilageNode("Grand Search", node21, Privileges.SearchGrand);

        TreeNode node24 = new PrivilageNode("User", tmproot);
        TreeNode node240 = new PrivilageNode("Change Theme", node24, Privileges.ChangePreferece);

        TreeNode node22 = new PrivilageNode("Cash Transaction", tmproot);
        TreeNode node2200 = new PrivilageNode("Cash Transaction Menu", node22, Privileges.CashTransaction);
        TreeNode node2201 = new PrivilageNode("Cash In", node22, Privileges.CashTransactionCashIn);
        TreeNode node2202 = new PrivilageNode("Cash Out", node22, Privileges.CashTransactionCashOut);
        TreeNode node2203 = new PrivilageNode("List To Cash Recieve", node22, Privileges.CashTransactionListToCashRecieve);

        TreeNode node23 = new PrivilageNode("Channelling", tmproot);
        TreeNode node2300 = new PrivilageNode("Channelling Menu", node23, Privileges.Channelling);
        TreeNode node2301 = new PrivilageNode("Channel Booking", node23, Privileges.ChannellingChannelBooking);
        TreeNode node2308 = new PrivilageNode("Channel Future Booking", node23, Privileges.ChannellingFutureChannelBooking);
        TreeNode node2302 = new PrivilageNode("Past Booking", node23, Privileges.ChannellingPastBooking);
        TreeNode node2303 = new PrivilageNode("Booked List", node23, Privileges.ChannellingBookedList);
        TreeNode node2304 = new PrivilageNode("Doctor Leave Menu", node23, Privileges.ChannellingDoctorLeave);
        TreeNode node230400 = new PrivilageNode("Doctor Leave By Date", node2304, Privileges.ChannellingDoctorLeaveByDate);
        TreeNode node230401 = new PrivilageNode("Doctor Leave By Service Session", node2304, Privileges.ChannellingDoctorLeaveByServiceSession);
        TreeNode node2305 = new PrivilageNode("Channel Sheduling", node23, Privileges.ChannellingChannelSheduling);
        TreeNode node2306 = new PrivilageNode("Channel Agent Fee", node23, Privileges.ChannellingChannelAgentFee);
        TreeNode node2309 = new PrivilageNode("Channel Booking Interface", node23);
        TreeNode node2309a = new PrivilageNode("Booking", node2309, Privileges.ChannelBookingBokking);
        TreeNode node2309b = new PrivilageNode("Reprint", node2309, Privileges.ChannelBookingReprint);
        TreeNode node2309c = new PrivilageNode("Cancel", node2309, Privileges.ChannelBookingCancel);
        TreeNode node2309d = new PrivilageNode("Refunfd", node2309, Privileges.ChannelBookingRefund);
        TreeNode node2309e = new PrivilageNode("Settle", node2309, Privileges.ChannelBookingSettle);
        TreeNode node2309f = new PrivilageNode("Change", node2309, Privileges.ChannelBookingChange);
        TreeNode node2309g = new PrivilageNode("Serch", node2309, Privileges.ChannelBookingSearch);
        TreeNode node2309h = new PrivilageNode("Views", node2309, Privileges.ChannelBookingViews);
        TreeNode node2309i = new PrivilageNode("Doctor Payment", node2309, Privileges.ChannelBookingDocPay);
        TreeNode node2309j = new PrivilageNode("Restric Channel booking", node2309, Privileges.ChannelBookingRestric);
        TreeNode node2310 = new PrivilageNode("Print Past Booking Recipt", node23, Privileges.ChannellingPrintInPastBooking);
        TreeNode node2307 = new PrivilageNode("Payment", node23);
        TreeNode node23070 = new PrivilageNode("Payment Menu", node2307, Privileges.ChannellingPayment);
        TreeNode node23071 = new PrivilageNode("Pay Doctor", node2307, Privileges.ChannellingPaymentPayDoctor);
        TreeNode node23072 = new PrivilageNode("Payment Due Search", node2307, Privileges.ChannellingPaymentDueSearch);
        TreeNode node23073 = new PrivilageNode("Payment Done Search", node2307, Privileges.ChannellingPaymentDoneSearch);
        TreeNode node23011 = new PrivilageNode("Cashier Transaction", node23);
        TreeNode node23011a = new PrivilageNode("Cashier Transaction Menu", node23011, Privileges.ChannelCashierTransaction);
        TreeNode node23011b = new PrivilageNode("Income", node23011, Privileges.ChannelCashierTransactionIncome);
        TreeNode node23011c = new PrivilageNode("Income Search", node23011, Privileges.ChannelCashierTransactionIncomeSearch);
        TreeNode node23011d = new PrivilageNode("Expensses", node23011, Privileges.ChannelCashierTransactionExpencess);
        TreeNode node23011e = new PrivilageNode("Expensess Search", node23011, Privileges.ChannelCashierTransactionExpencessSearch);
        TreeNode node23010 = new PrivilageNode("Administrator", node23);
        TreeNode node23010a = new PrivilageNode("Edit Appoinment Count", node23010, Privileges.ChannellingApoinmentNumberCountEdit);
        TreeNode node23010b = new PrivilageNode("Edit Appoinment Number", node23010, Privileges.ChannellingEditSerialNo);
        TreeNode node23010c = new PrivilageNode("Edit Patient Details", node23010, Privileges.ChannellingEditPatientDetails);
        TreeNode node23010d = new PrivilageNode("Delete Shedule", node23010, Privileges.ChannellingChannelShedulRemove);
        TreeNode node23010e = new PrivilageNode("Edit Session Name", node23010, Privileges.ChannellingChannelShedulName);
        TreeNode node23010f = new PrivilageNode("Edit Session Starting No", node23010, Privileges.ChannellingChannelShedulStartingNo);
        TreeNode node23010g = new PrivilageNode("Edit Session Room No", node23010, Privileges.ChannellingChannelShedulRoomNo);
        TreeNode node23010h = new PrivilageNode("Edit Session Max Row No", node23010, Privileges.ChannellingChannelShedulMaxRowNo);
        TreeNode node23010i = new PrivilageNode("Edit Credit Limit User Level", node23010, Privileges.ChannellingEditCreditLimitUserLevel);
        TreeNode node23010j = new PrivilageNode("Edit Credit Limit Administrator Level", node23010, Privileges.ChannellingEditCreditLimitAdminLevel);
        TreeNode node23010k = new PrivilageNode("Channel Reports", node23, Privileges.ChannelReports);
        TreeNode node23010l = new PrivilageNode("Channel Summery", node23, Privileges.ChannelSummery);
        TreeNode node23012 = new PrivilageNode("Channel Mamagement", node23);
        TreeNode node23012a = new PrivilageNode("Channel Mamagement Menu", node23012, Privileges.ChannelManagement);
        TreeNode node23012b = new PrivilageNode("Channel Agencies", node23012, Privileges.ChannelAgencyAgencies);
        TreeNode node23012c = new PrivilageNode("Channel Agenciey Credit Limit Update", node23012, Privileges.ChannelAgencyCreditLimitUpdate);
        TreeNode node23012d = new PrivilageNode("Channel Agenciey Credit Limit Update (Bulk)", node23012, Privileges.ChannelAgencyCreditLimitUpdateBulk);
        TreeNode node23012e = new PrivilageNode("Add Channel Book To Agency", node23012, Privileges.ChannelAddChannelBookToAgency);
        TreeNode node23012f = new PrivilageNode("Channel Management Specialities", node23012, Privileges.ChannelManageSpecialities);
        TreeNode node23012g = new PrivilageNode("Channel Management Consultants", node23012, Privileges.ChannelManageConsultants);
        TreeNode node23012h = new PrivilageNode("Channel Editing Appoinment Count", node23012, Privileges.ChannelEditingAppoinmentCount);
        TreeNode node23012i = new PrivilageNode("Add Channelling Consultants To Institution ", node23012, Privileges.ChannelAddChannelingConsultantToInstitutions);
        TreeNode node23012j = new PrivilageNode("Channel Fee Update", node23012, Privileges.ChannelFeeUpdate);
        TreeNode node23012k = new PrivilageNode("Channel Credit Note", node23012, Privileges.ChannelCrdeitNote);
        TreeNode node23012l = new PrivilageNode("Channel Credit Note Search", node23012, Privileges.ChannelCrdeitNoteSearch);
        TreeNode node23012m = new PrivilageNode("Channel Debit Note", node23012, Privileges.ChannelDebitNote);
        TreeNode node23012n = new PrivilageNode("Channel Debit Note Search", node23012, Privileges.ChannelDebitNoteSearch);
        TreeNode node23012o = new PrivilageNode("Channel Cash Cancel Restriction", node23012, Privileges.ChannelCashCancelRestriction);
        TreeNode node23012p = new PrivilageNode("Channel Active Vat", node23012, Privileges.ChannelActiveVat);

        return tmproot;
    }

    public UserPrivilageController() {
    }

    @PostConstruct
    public void init() {
        rootTreeNode = createPrivilegeHolderTreeNodes();
    }

    public TreeNode[] getSelectedNodes() {
        return selectedNodes;
    }

    public void setSelectedNodes(TreeNode[] selectedNodes) {
        this.selectedNodes = selectedNodes;
    }

    public void savePrivileges() {
        if (currentWebUser == null) {
            UtilityController.addErrorMessage("Please select a user");
            return;
        }
        if (department == null) {
            UtilityController.addErrorMessage("Please select a department");
            return;
        }
        saveWebUserPrivileges();
    }

    public WebUserPrivilegeFacade getEjbFacade() {
        return ejbFacade;
    }

    public List<TreeNode> collectNodes(TreeNode node) {
        List<TreeNode> nodeList = new ArrayList<>();
        collectNodesHelper(node, nodeList);
        return nodeList;
    }

    private void collectNodesHelper(TreeNode node, List<TreeNode> nodeList) {
        if (node == null) {
            return;
        }
        nodeList.add(node);
        for (Object object : node.getChildren()) {
            if (object instanceof TreeNode) {
                TreeNode child = (TreeNode) object;
                collectNodesHelper(child, nodeList);
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
        checkNodes(rootTreeNode, currentUserPrivilegeHolders);
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

    @Deprecated
    public void markWebUserPrivileges() {
        List<WebUserPrivilege> wups = new ArrayList<>();
        if (currentWebUser != null) {
            String j = "SELECT i "
                    + " FROM WebUserPrivilege i "
                    + " where i.webUser=:wu ";
            Map m = new HashMap();
            m.put("wu", currentWebUser);
            wups = getEjbFacade().findByJpql(j, m);
        }
        List<TreeNode> tmpAllNodes = collectNodes(rootTreeNode);
        for (TreeNode ttn : tmpAllNodes) {
            if (!ttn.isLeaf()) {
                continue;
            }
            PrivilageNode tpn = null;
            if (ttn instanceof PrivilageNode) {
                tpn = (PrivilageNode) ttn;
            }
            if (tpn == null) {
                continue;
            }
            Privileges tp = null;
            if (tpn.getP() == null) {
                continue;
            }
            tp = tpn.getP();
            for (WebUserPrivilege twup : wups) {
                if (twup.getPrivilege() == null) {
                    continue;
                }
                if (twup.getPrivilege().equals(tp)) {
                    if (!twup.isRetired()) {
                        ttn.setSelected(true);
                    }
                }
            }
        }
        markParentNodes(rootTreeNode);
    }

    private boolean markParentNodes(TreeNode node) {
        if (node.isLeaf()) {
            return node.isSelected();
        }

        boolean allChildrenSelected = true;
        for (Object object : node.getChildren()) {
            if (object instanceof TreeNode) {
                TreeNode child = (TreeNode) object;
                boolean childSelected = markParentNodes(child);
                if (!childSelected) {
                    allChildrenSelected = false;
                }
            }
        }

        node.setSelected(allChildrenSelected);
        return allChildrenSelected;
    }

    public void saveWebUserPrivilegesOld() {
        List<WebUserPrivilege> wups = new ArrayList<>();
        List<WebUserPrivilege> nwups = new ArrayList<>();
        if (currentWebUser != null) {
            String j = "SELECT i "
                    + " FROM WebUserPrivilege i "
                    + " where i.webUser=:wu ";
            Map m = new HashMap();
            m.put("wu", currentWebUser);
            wups = getEjbFacade().findByJpql(j, m);
        }
        List<TreeNode> tmpAllNodes = collectNodes(rootTreeNode);
        for (TreeNode ttn : tmpAllNodes) {
            System.out.println("ttn = " + ttn);
            if (!ttn.isLeaf()) {
                System.out.println("ttn is a leaf");
                continue;
            }
            PrivilageNode tpn = null;
            if (ttn instanceof PrivilageNode) {
                tpn = (PrivilageNode) ttn;
                System.out.println("tpn = " + tpn);
            }
            if (tpn == null) {
                System.out.println("tpn is null");
                continue;
            }
            Privileges tp = null;
            if (tpn.getP() == null) {
                continue;
            }
            boolean needToCreateNew = true;
            tp = tpn.getP();
            System.out.println("tp = " + tp);
            WebUserPrivilege correctWup = null;
            for (WebUserPrivilege twup : wups) {
                if (twup.getPrivilege() == null) {
                    continue;
                }
                if (twup.getPrivilege().equals(tp)) {
                    correctWup = twup;
                }
            }
            if (ttn.isSelected()) {
                if (correctWup != null) {
                    correctWup.setRetired(false);
                    needToCreateNew = false;
                }
            } else {
                needToCreateNew = false;
                if (correctWup != null) {
                    correctWup.setRetired(true);
                    needToCreateNew = false;
                }
            }
            if (needToCreateNew) {
                WebUserPrivilege nwup = new WebUserPrivilege();
                nwup.setWebUser(currentWebUser);
                nwup.setPrivilege(tp);
                nwup.setCreatedAt(new Date());
                nwup.setCreater(sessionController.getLoggedUser());
                nwups.add(nwup);
            }
        }
        getFacade().batchCreate(nwups);
        getFacade().batchEdit(wups);
    }

    public void saveWebUserPrivileges() {
        List<PrivilegeHolder> selectedPrivileges = extractPrivileges(selectedNodes);
        System.out.println("selectedPrivileges = " + selectedPrivileges);

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
            System.out.println("ph = " + ph);
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
                getFacade().create(wup);
                getFacade().edit(wup);
                newWups.add(wup);
            } else {
                wup.setRetired(false);
                oldWups.add(wup);
            }
        }
        getFacade().batchCreate(newWups);
        getFacade().batchEdit(oldWups);
        JsfUtil.addSuccessMessage("Updated");

    }

    public static List<PrivilegeHolder> extractPrivileges(TreeNode[] selectedNodes) {
        System.out.println("extractPrivileges");
        System.out.println("selectedNodes = " + selectedNodes);
        List<PrivilegeHolder> privileges = new ArrayList<>();
        if (selectedNodes != null) {
            for (TreeNode node : selectedNodes) {
                Object data = node.getData();
                System.out.println("data = " + data);
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

    @Deprecated
    public void saveWebUserPrivilegesOld1() {
        List<TreeNode> tmpAllNodes = collectNodes(rootTreeNode);
        for (TreeNode ttn : tmpAllNodes) {
            System.out.println("ttn = " + ttn);
            if (!ttn.isLeaf()) {
                System.out.println("ttn is a leaf");
                continue;
            }
            PrivilageNode tpn = null;
            if (ttn instanceof PrivilageNode) {
                tpn = (PrivilageNode) ttn;
                System.out.println("tpn = " + tpn);
            }
            if (tpn == null) {
                System.out.println("tpn is null");
                continue;
            }
            Privileges tp = null;
            if (tpn.getP() == null) {
                continue;
            }

            tp = tpn.getP();
            System.out.println("tp = " + tp);

            String j = "SELECT i "
                    + " FROM WebUserPrivilege i "
                    + " where i.webUser=:wu and"
                    + " i.privilege=:p ";
            Map m = new HashMap();
            m.put("wu", currentWebUser);
            m.put("p", tp);
            WebUserPrivilege wup = getEjbFacade().findFirstByJpql(j, m);
            System.out.println("correctWup = " + wup);
            if (wup == null) {
                wup = new WebUserPrivilege();
                wup.setCreatedAt(new Date());
                wup.setCreater(sessionController.getLoggedUser());
                wup.setPrivilege(tp);
                wup.setWebUser(currentWebUser);
                getFacade().create(wup);
            }
            System.out.println("ttn.isSelected() = " + ttn.isSelected());
            if (ttn.isSelected()) {
                wup.setRetired(false);
            } else {
                wup.setRetired(true);
            }
            System.out.println("wup.isRetired() = " + wup.isRetired());
            getFacade().edit(wup);
        }
        createSelectedPrivilegesForUser();
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

    public TreeNode getRootTreeNode() {
        return rootTreeNode;
    }

    public void setRootTreeNode(TreeNode tmp) {
        this.rootTreeNode = tmp;
    }

    @Deprecated
    public void createSelectedPrivilegesForUser() {
        markWebUserPrivileges();
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

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

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
}
