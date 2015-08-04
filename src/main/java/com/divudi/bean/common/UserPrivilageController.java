/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;

import com.divudi.data.dataStructure.PrivilageNode;
import com.divudi.data.Privileges;
import com.divudi.entity.WebUser;
import java.util.TimeZone;
import com.divudi.facade.WebUserPrivilegeFacade;
import com.divudi.entity.WebUserPrivilege;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.faces.component.UIComponent;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
//package org.primefaces.examples.view;  

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.persistence.TemporalType;

import org.primefaces.model.TreeNode;

//import org.primefaces.examples.domain.Document;  
//import org.primefaces.model;
/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class UserPrivilageController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private WebUserPrivilegeFacade ejbFacade;
    private List<WebUserPrivilege> selectedItems;
    private WebUserPrivilege current;
    private WebUser currentWebUser;
    private List<WebUserPrivilege> items = null;
    //private Privileges currentPrivileges;
    private TreeNode root;
    private TreeNode[] selectedNodes;
    private List<Privileges> privilegeList;

    private TreeNode createTreeNode() {
        TreeNode tmproot = new PrivilageNode("Root", null);

        TreeNode node0 = new PrivilageNode("OPD", tmproot);
        TreeNode node00 = new PrivilageNode("Billing Menu", node0, Privileges.Opd);
        TreeNode node01 = new PrivilageNode("Bill", node0, Privileges.OpdBilling);
        TreeNode node01a = new PrivilageNode("Pre Billing", node0, Privileges.OpdPreBilling);
        TreeNode node02 = new PrivilageNode("Bill Search", node0, Privileges.OpdBillSearch);
        TreeNode node03 = new PrivilageNode("Bill Item Search", node0, Privileges.OpdBillItemSearch);
        TreeNode node04 = new PrivilageNode("Reprint", node0, Privileges.OpdReprint);
        TreeNode node05 = new PrivilageNode("Cancel", node0, Privileges.OpdCancel);
        TreeNode node06 = new PrivilageNode("Return", node0, Privileges.OpdReturn);
        TreeNode node07 = new PrivilageNode("Reactivate", node0, Privileges.OpdReactivate);
        TreeNode node08 = new PrivilageNode("OPD Bill Search Edit", node0, Privileges.OpdBillSearchEdit);

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
        TreeNode node1a10 = new PrivilageNode("Theatre BHT Issue", node1a, Privileges.TheaterIssue);
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
        TreeNode node201 = new PrivilageNode("Billing", node2, Privileges.LabBilling);
        TreeNode node217 = new PrivilageNode("Lab Cashier", node2, Privileges.LabCashier);
        TreeNode node219 = new PrivilageNode("Lab Cashier Bill Search", node2, Privileges.LabCasheirBillSearch);
        TreeNode node220 = new PrivilageNode("Lab Cashier Bill Item Search", node2, Privileges.LabCasheirBillSearch);
        TreeNode node218 = new PrivilageNode("Lab Bill Search", node2, Privileges.LabBillSearchCashier);
        TreeNode node202 = new PrivilageNode("Search Bills", node2, Privileges.LabBillSearch);
        TreeNode node203 = new PrivilageNode("Search Bills Items", node2, Privileges.LabBillItemSearch);
        TreeNode node204 = new PrivilageNode("Patient Edit", node2, Privileges.LabEditPatient);
        TreeNode node205 = new PrivilageNode("Lab Bill Reprint", node2, Privileges.LabBillReprint);
        TreeNode node206 = new PrivilageNode("Lab Bill Return", node2, Privileges.LabBillReturning);
        TreeNode node207 = new PrivilageNode("Lab Bill Cancel", node2, Privileges.LabBillCancelling);
        TreeNode node208 = new PrivilageNode("Reactivate", node2, Privileges.LabBillReactivating);
        TreeNode node209 = new PrivilageNode("Sample Collection", node2, Privileges.LabSampleCollecting);
        TreeNode node210 = new PrivilageNode("Sample Receive", node2, Privileges.LabSampleReceiving);
        TreeNode node211 = new PrivilageNode("DataEntry", node2, Privileges.LabDataentry);
        TreeNode node212 = new PrivilageNode("Autherize", node2, Privileges.LabAutherizing);
        TreeNode node213 = new PrivilageNode("De-Autherize", node2, Privileges.LabDeAutherizing);
        TreeNode node214 = new PrivilageNode("Report Print", node2, Privileges.LabPrinting);
        TreeNode node214a = new PrivilageNode("Report Reprint", node2, Privileges.LabReprinting);
        TreeNode node215 = new PrivilageNode("Lab Report Formats Editing", node2, Privileges.LabReportFormatEditing);
        TreeNode node216 = new PrivilageNode("Lab Summeries", node2, Privileges.LabSummeriesLevel1);
        TreeNode node221 = new PrivilageNode("Lab Investigation Fees", node2, Privileges.LabInvestigationFee);
        TreeNode node222 = new PrivilageNode("Lab Bill Cancell Special(after collecting sample can cancell)", node2, Privileges.LabBillCancelSpecial);
        TreeNode node223 = new PrivilageNode("Lab Bill Refund Special(after collecting sample can Refund)", node2, Privileges.LabBillRefundSpecial);
        TreeNode node224 = new PrivilageNode("Add Inward Services", node2, Privileges.LabAddInwardServices);
        TreeNode node225 = new PrivilageNode("Search By Logged Institution", node2, Privileges.LabSearchBillLoggedInstitution);

        TreeNode node3 = new PrivilageNode("Pharmacy", tmproot);
        TreeNode node300 = new PrivilageNode("Pharmacy Menu", node3, Privileges.Pharmacy);
        TreeNode node301 = new PrivilageNode("Pharmacy Administration", node3, Privileges.PharmacyAdministration);
        TreeNode node306 = new PrivilageNode("Pharmacy Stock Adjustment", node3, Privileges.PharmacyStockAdjustment);
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
        TreeNode node66 = new PrivilageNode("Membership", node6);
        TreeNode node660 = new PrivilageNode("Membership Menu", node66, Privileges.MemberShip);
        TreeNode node661 = new PrivilageNode("Membership Schemes", node66, Privileges.MembershipSchemes);
        TreeNode node662 = new PrivilageNode("Inward Membership", node66);
        TreeNode node6620 = new PrivilageNode("Inward Membership Menu", node662, Privileges.MemberShipInwardMemberShip);
        TreeNode node6621 = new PrivilageNode("Schemes Dicounts", node662, Privileges.MemberShipInwardMemberShipSchemesDicounts);
        TreeNode node6622 = new PrivilageNode("Inward Membership Report", node662, Privileges.MemberShipInwardMemberShipInwardMemberShipReport);
        TreeNode node663 = new PrivilageNode("Opd MemberShip Dis", node66);
        TreeNode node6630 = new PrivilageNode("Opd MemberShip Dis Menu", node663, Privileges.MemberShipOpdMemberShipDis);
        TreeNode node6631 = new PrivilageNode("By Department", node663, Privileges.MemberShipOpdMemberShipDisByDepartment);
        TreeNode node6632 = new PrivilageNode("By Category", node663, Privileges.MemberShipOpdMemberShipDisByCategory);
        TreeNode node6633 = new PrivilageNode("Opd MemberShip Report", node663, Privileges.MemberShipOpdMemberShipDisOpdMemberShipReport);
        TreeNode node67 = new PrivilageNode("Only For Developers(Don't Add That)", node6, Privileges.Developers);
        TreeNode node68 = new PrivilageNode("Search All", node6, Privileges.SearchAll);

        TreeNode node9 = new PrivilageNode("Human Resource", tmproot);
        TreeNode node91 = new PrivilageNode("HR Menu", node9, Privileges.Hr);
        TreeNode node910 = new PrivilageNode("HR Anistration", node91, Privileges.HrAdmin);
        TreeNode node911 = new PrivilageNode("HR Employee History Reports", node91, Privileges.EmployeeHistoryReport);
        TreeNode node912 = new PrivilageNode("HR Delete Late Leave", node91, Privileges.hrDeleteLateLeave);

        TreeNode node8 = new PrivilageNode("Higheist Accountability", tmproot);
        TreeNode node81 = new PrivilageNode("Change Professional Fee", node8, Privileges.ChangeProfessionalFee);
        TreeNode node82 = new PrivilageNode("Change Professional Fee", node8, Privileges.ChangeCollectingCentre);

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
        TreeNode node2304 = new PrivilageNode("Doctor Leave", node23, Privileges.ChannellingDoctorLeave);
        TreeNode node2305 = new PrivilageNode("Channel Sheduling", node23, Privileges.ChannellingChannelSheduling);
        TreeNode node2306 = new PrivilageNode("Channel Agent Fee", node23, Privileges.ChannellingChannelAgentFee);
        TreeNode node2309 = new PrivilageNode("Channel Booking Change", node23, Privileges.ChannelBookingChange);
        TreeNode node2307 = new PrivilageNode("Payment", node23);
        TreeNode node23070 = new PrivilageNode("Payment Menu", node2307, Privileges.ChannellingPayment);
        TreeNode node23071 = new PrivilageNode("Pay Doctor", node2307, Privileges.ChannellingPaymentPayDoctor);
        TreeNode node23072 = new PrivilageNode("Payment Due Search", node2307, Privileges.ChannellingPaymentDueSearch);
        TreeNode node23073 = new PrivilageNode("Payment Done Search", node2307, Privileges.ChannellingPaymentDoneSearch);
        

        return tmproot;
    }

    public UserPrivilageController() {
        root = createTreeNode();
    }

    public TreeNode getRoot2() {
        return root;
    }

    public void setRoot2(TreeNode root2) {
        this.root = root2;
    }

    public TreeNode[] getSelectedNodes() {
        return selectedNodes;
    }

    public void setSelectedNodes(TreeNode[] selectedNodes) {
        this.selectedNodes = selectedNodes;
    }

    private void removeAllPrivilages() {
        String sql = "SELECT i FROM WebUserPrivilege i where i.webUser.id= " + getCurrentWebUser().getId();
        List<WebUserPrivilege> tmp = getEjbFacade().findBySQL(sql);

        for (WebUserPrivilege wb : tmp) {
            wb.setRetired(true);
            getEjbFacade().edit(wb);
        }

    }

    public void savePrivileges() {
        if (currentWebUser == null) {
            UtilityController.addErrorMessage("Please select a user");
            return;
        }
        if (selectedNodes != null && selectedNodes.length > 0) {
            removeAllPrivilages();
            for (TreeNode node : selectedNodes) {
                Privileges p;
                p = ((PrivilageNode) node).getP();
                addSinglePrivilege(p);
            }
        }
        getItems();
    }

    public void addSinglePrivilege(Privileges p) {
        if (p == null) {
            return;
        }
        WebUserPrivilege wup;
        Map m = new HashMap();
        m.put("wup", p);
        String sql = "SELECT i FROM WebUserPrivilege i where i.retired=false and i.webUser.id= " + getCurrentWebUser().getId() + " and i.privilege=:wup ";
        List<WebUserPrivilege> tmp = getEjbFacade().findBySQL(sql, m, TemporalType.DATE);

        if (tmp == null || tmp.isEmpty()) {
            wup = new WebUserPrivilege();
            wup.setCreater(getSessionController().getLoggedUser());
            wup.setCreatedAt(Calendar.getInstance().getTime());
            wup.setPrivilege(p);
            wup.setWebUser(getCurrentWebUser());
            getFacade().create(wup);
        }

//        for (WebUserPrivilege wu : tmpNode) {
//            wu.setRetired(false);
//        }
    }

    public void remove() {
        if (getCurrent() == null) {
            UtilityController.addErrorMessage("Select Privilage");
            return;
        }

        getCurrent().setRetired(true);

        getFacade().edit(getCurrent());
    }

    private void recreateModel() {
        items = null;
    }

    public WebUserPrivilegeFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(WebUserPrivilegeFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public WebUserPrivilege getCurrent() {
        if (current == null) {
            current = new WebUserPrivilege();

        }
        return current;
    }

    public void setCurrent(WebUserPrivilege current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private WebUserPrivilegeFacade getFacade() {
        return ejbFacade;
    }
    private TreeNode tmpNode;

//    public List<WebUserPrivilege> getItems2() {
//        // items = getFacade().findAll("name", true);
//        if (getCurrentWebUser() == null) {
//            return new ArrayList<WebUserPrivilege>();
//        }
//
//        String sql = "SELECT i FROM WebUserPrivilege i where i.retired=false and i.webUser.id= " + getCurrentWebUser().getId() + " order by i.webUser.webUserPerson.name";
//        items = getEjbFacade().findBySQL(sql);
//        if (items == null) {
//            items = new ArrayList<WebUserPrivilege>();
//        }
//        for (TreeNode n : root.getChildren()) {
//            n.setSelected(false);
//        }
//        for (TreeNode n : root.getChildren()) {
//            ////System.out.println("n is " + n);
//            for (TreeNode n1 : n.getChildren()) {
//                Privileges p;
//                ////System.out.println("n1 is " + n1);
//                //
//                try {
//                    if (n1 instanceof PrivilageNode) {
//                        p = ((PrivilageNode) n1).getP();
//                        markTreeNode(p, n1);
//                    } else {
//                        ////System.out.println("type of p is ");
//                    }
//                } catch (Exception e) {
//                    ////System.out.println("exception e is " + e.getMessage());
//                }
//            }
//        }
//        return items;
//    }
    private void unselectNode() {
        for (TreeNode n : root.getChildren()) {
            n.setSelected(false);
            for (TreeNode n1 : n.getChildren()) {
                n1.setSelected(false);
                for (TreeNode n2 : n1.getChildren()) {
                    n2.setSelected(false);
                }
            }
        }

        tmpNode = root;
    }

    public List<WebUserPrivilege> getItems() {
        if (getCurrentWebUser() == null) {
            root = createTreeNode();
            tmpNode = root;
            return new ArrayList<>();

        }

        String sql = "SELECT i FROM WebUserPrivilege i where i.retired=false and i.webUser.id= " + getCurrentWebUser().getId() + " order by i.webUser.webUserPerson.name";
        items = getEjbFacade().findBySQL(sql);
        if (items == null) {
            items = new ArrayList<>();
            root = createTreeNode();
            tmpNode = root;
            return items;
        }

        root = createTreeNode();
        for (WebUserPrivilege wup : items) {
            for (TreeNode n : root.getChildren()) {
                if (wup.getPrivilege() == ((PrivilageNode) n).getP()) {
                    n.setSelected(true);
                }
                for (TreeNode n1 : n.getChildren()) {
                    if (wup.getPrivilege() == ((PrivilageNode) n1).getP()) {
                        n1.setSelected(true);
                    }
                    for (TreeNode n2 : n1.getChildren()) {
                        if (wup.getPrivilege() == ((PrivilageNode) n2).getP()) {
                            n2.setSelected(true);
                        }
                    }
                }
            }
        }
        tmpNode = root;
        return items;
    }

//    public void markTreeNode(Privileges p, TreeNode n) {
//        if (p == null) {
//            return;
//        }
//        n.setSelected(false);
//        Map m = new HashMap();
//        m.put("wup", p);
//        String sql = "SELECT i FROM WebUserPrivilege i where i.webUser.id= " + getCurrentWebUser().getId() + " and i.privilege=:wup ";
//        List<WebUserPrivilege> tmp = getEjbFacade().findBySQL(sql, m, TemporalType.DATE);
//        if (tmp == null || tmp.isEmpty()) {
//            for (WebUserPrivilege wu : tmp) {
//                if (!wu.isRetired()) {
//                    n.setSelected(true);
//                }
//            }
//        }
//    }
    public WebUser getCurrentWebUser() {
        return currentWebUser;
    }

    public void setCurrentWebUser(WebUser currentWebUser) {
        this.currentWebUser = currentWebUser;
        tmpNode = null;
    }

    public List<Privileges> getPrivilegeList() {
        return privilegeList;
    }

    public void setPrivilegeList(List<Privileges> privilegeList) {
        this.privilegeList = privilegeList;

    }

    public List<WebUserPrivilege> getSelectedItems() {
        return selectedItems;
    }

    public void setSelectedItems(List<WebUserPrivilege> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public TreeNode getTmp() {
        getItems();
        return tmpNode;
    }

    public void setTmp(TreeNode tmp) {
        this.tmpNode = tmp;
    }

    public TreeNode getRoot() {
        return root;
    }

    public void setRoot(TreeNode root) {
        this.root = root;
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
