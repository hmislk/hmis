package com.divudi.bean.collectingCentre;

import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.bean.report.ReportController;
import com.divudi.core.entity.Bill;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import javax.inject.Inject;

/**
 *
 * @author Buddhika Ariyaratne
 */
@Named
@SessionScoped
public class CourierController implements Serializable {

    private Bill currentCCBill;

    @Inject
    PatientInvestigationController patientInvestigationController;
    @Inject
    ReportController reportController;

    private int activeIndex = 0;


    /**
     * Creates a new instance of CourierController
     */
    public CourierController() {
    }

    public String navigateToCourierCollectSamples() {
        activeIndex = 1;
        return "/collecting_centre/courier/collectSamples.xhtml?faces-redirect=true";
    }

    public String navigateToCourierHandoverSamplesToLab() {
        activeIndex = 2;
        return "/collecting_centre/courier/handoverSamplesToLab.xhtml?faces-redirect=true";
    }

    public String navigateToCourierViewReports() {
        patientInvestigationController.makeNull();
        activeIndex = 3;
        return "/collecting_centre/courier/viewReports.xhtml?faces-redirect=true";
    }

    public String navigateToCourierIndex() {
        patientInvestigationController.makeNull();
        activeIndex = 0;
        return "/collecting_centre/courier/index.xhtml?faces-redirect=true";
    }

    public String navigateToCourierPrintReports() {
        activeIndex = 4;
        return patientInvestigationController.navigateToGenerateBarcodesFromCourier();
    }

    public String navigateToCourierViewStatistics() {
        activeIndex = 5;
        return "/collecting_centre/courier/viewStatistics.xhtml?faces-redirect=true";
    }

    public String navigateToCourierViewCCReports() {
        activeIndex = 6;
        return "/collecting_centre/courier/courier_report_index.xhtml?faces-redirect=true";
    }

    public String navigateToCourierLabReportsPrint() {
        reportController.setReportTemplateFileIndexName("/collecting_centre/courier/courier_report_index.xhtml");
        return "/reports/collectionCenterReports/courier_lab_report_print?faces-redirect=true";
    }

    public String navigateToCCReportsPrint() {
        reportController.setReportTemplateFileIndexName("/collecting_centre/courier/courier_report_index.xhtml");
        return "/reports/collectionCenterReports/collection_center_report_print?faces-redirect=true";
    }

    public String navigateToCCCurrentBalanceReport() {
        reportController.setReportTemplateFileIndexName("/collecting_centre/courier/courier_report_index.xhtml");
        return "/reports/collectionCenterReports/collection_center_current_balance_report?faces-redirect=true";
    }

    public String navigateToCCBalanceReport() {
        reportController.setReportTemplateFileIndexName("/collecting_centre/courier/courier_report_index.xhtml");
        return "/reports/collectionCenterReports/collection_center_balance_report?faces-redirect=true";
    }

    public String navigateToCCReceiptReport() {
        reportController.setReportTemplateFileIndexName("/collecting_centre/courier/courier_report_index.xhtml");
        return "/reports/collectionCenterReports/collection_center_recipt_reports?faces-redirect=true";
    }

    public String navigateToCCBillWiseDetailReport() {
        reportController.setReportTemplateFileIndexName("/collecting_centre/courier/courier_report_index.xhtml");
        return "/reports/collectionCenterReports/collection_center_bill_wise_detail_report?faces-redirect=true";
    }

    public String navigateToCCWiseInvoiceListReport() {
        reportController.setReportTemplateFileIndexName("/collecting_centre/courier/courier_report_index.xhtml");
        return "/reports/collectionCenterReports/collection_center_wise_invoice_list_report?faces-redirect=true";
    }

    public String navigateToCCStatementReport() {
        reportController.setReportTemplateFileIndexName("/collecting_centre/courier/courier_report_index.xhtml");
        return "/reports/collectionCenterReports/collection_center_statement_report?faces-redirect=true";
    }

    public String navigateToCCWiseSummaryReport() {
        reportController.setReportTemplateFileIndexName("/collecting_centre/courier/courier_report_index.xhtml");
        return "/reports/collectionCenterReports/collection_center_wise_summary_report?faces-redirect=true";
    }

    public String navigateToTestWiseCountReport() {
        reportController.setReportTemplateFileIndexName("/collecting_centre/courier/courier_report_index.xhtml");
        return "/reports/collectionCenterReports/collection_center_test_wise_count_report?faces-redirect=true";
    }

    public String navigateToCCRouteAnalysisReport() {
        reportController.setReportTemplateFileIndexName("/collecting_centre/courier/courier_report_index.xhtml");
        return "/reports/collectionCenterReports/route_analysis_report?faces-redirect=true";
    }

    public String navigateToCCBookReport() {
        reportController.setReportTemplateFileIndexName("/collecting_centre/courier/courier_report_index.xhtml");
        return "/reports/collectionCenterReports/collection_center_book_report?faces-redirect=true";
    }

    public String navigateToCCBookWiseDetail() {
        reportController.setReportTemplateFileIndexName("/collecting_centre/courier/courier_report_index.xhtml");
        return "/reports/collectionCenterReports/collection_centre_book_wise_detail?faces-redirect=true";
    }

    public String navigateToCCInvestigationListReport() {
        reportController.setReportTemplateFileIndexName("/collecting_centre/courier/courier_report_index.xhtml");
        return "/reports/collectionCenterReports/cc_investigation_list?faces-redirect=true";
    }

    public String navigateToCCBillItemListReport() {
        reportController.setReportTemplateFileIndexName("/collecting_centre/courier/courier_report_index.xhtml");
        return "/reports/collectionCenterReports/cc_bill_item_list?faces-redirect=true";
    }

    public String navigateToCourierViewReciptReports() {
        activeIndex = 7;
        return "/reports/collectionCenterReports/collection_center_recipt_reports_c?faces-redirect=true";
    }

    public String navigateToCourierViewPaymentReports() {
        activeIndex = 8;
        return "/collecting_centre/courier/viewPaymentReports.xhtml?faces-redirect=true";
    }

    public String navigateToCCBillViewFormPaymentReports() {
        return "/collecting_centre/courier/cc_bill_reprint_view?faces-redirect=true";
    }

    public String navigateToViewReciptFromCCBillView() {
        activeIndex = 7;
        return "/reports/collectionCenterReports/collection_center_recipt_reports_c?faces-redirect=true";
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }

    public Bill getCurrentCCBill() {
        return currentCCBill;
    }

    public void setCurrentCCBill(Bill currentCCBill) {
        this.currentCCBill = currentCCBill;
    }
}
