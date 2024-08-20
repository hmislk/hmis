/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

/**
 *
 * @author safrin
 */
public enum BillNumberSuffix {

    RF,//Refund
    CAN,//Cancel
    //Professional Payment
    PROPAY,//Professional Payment
    PROCAN,//Professional Cancel
    CHNPROPAY,//Channel Pro Pay
    //Pharmacy
    GRN,//Pharmacy GRN
    GRNCAN,//GRN Cancel
    GRNRET,//GRN Return
    PHDIRRET,//Pharmacy Return to dealer Without traising
    GRNRETCAN,//GRN Return Cancel
    PO,//Purchase Order
    POCAN,//PO Cancel
    POR,//Purchase Order Request
    PORCAN,//Purchase Order Request Cancell
    PHCAN,//Pharmacy Cancel    
    PHPUR,//Purchase  
    PURRET,//Purchase Return
    PURCAN,//Purchase Cancel
    PHTI,//Transfer Issue
    PHTICAN,//Trnsfer Issue Cancel
    PHTR,//Transfer Recieve
    PHTRCAN,//Transfer Receive Cancel
    PHTRQ,//Transfer Request
    //PHPRE,//Pre Bill
    PHSAL,//Before Error Sale
    SALE,//After Error
    SALCAN,//Pharmacy Sale Cancel
    PRECAN,//Pre Cancel
    @Deprecated
    BHTISSUE,//Bht Issue
    @Deprecated
    BHTISSUECAN,//Bht Issue Cancel
    PHISSUE,//Pharmacy Issue
    PHISSUEREQ,//Pharmacy Issue Request
    PHISSCAN,//Pharmacy Issue Cancel
    PHISSRET,//Pharmacy Issue Return
    STISSUE,//Store Issue
    STTISSUECAN,//Store Issue can
    PHRET,
    RETCAN,//Sale Return Cancel
    //PHPRERET,//Pre BIll Return
    // PHSAL,//Sale Bill
    //  PRERET,//Pre Return
    // SALRET,//Sal Return
    MJADJ,//Major Adjustment
    ADJ,//Adjustment
    //Inward
    INWPAY,//Payment Bill
    INWFINAL,//Inward Final
    INWINTRIM,//Inward Interim    
    INWPRO,//Professional
    INWSER,//Service
    INWREF,//Refund
    INWREFCAN,//Refund Cancel
    INWCAN,//Cancell
    //Agent
    AGNPAY,//Payment 
    AGNCAN,//Payment Cancel
    AGNCN,//Credit Note 
    AGNCNCAN,//Credit Note Cancel
    AGNDN,//Debit Note 
    AGNDNCAN,//Debit Note Cancel
    //Agent
    PD,//Patient Deposit 
    PDC,//Patient Deposit Cancel
    PDR,//Patient Deposit Return
    PDU,//Patient Deposit Utilization
    //Collecting Centre
    CCPAY,//Payment 
    CCCAN,//Payment Cancel
    CCCN,//Credit Note 
    CCCNCAN,//Credit Note Cancel
    CCDN,//Debit Note 
    CCDNCAN,//Debit Note Cancel
    //Credit Company
    CRDPAY,//Payment
    CRDCAN,//Cancel
    //Petty Cash
    PTYPAY,//Payment
    PTYCAN,//Cancell
    NONE,//NO Prefix
    DRADJ,//Drawer Adjustment
    PACK,//Package
    SURG,//Surger Bill
    TIME,//Timed Service 
    CSIN,//Cashier Cash Out
    CSINCAN,//Cash In Cancel
    CSOUT,//Cashier CAsh IN
    CSOUTCAN,//Cash Out Cancel
    DI,//Department Issue
    DIC,//Department Issue Cancell
    ISS,//Department Issue
    ISSCAN,//Department Issue Cancel
    STTRQ,//transper request
    STTI,//transfer issue
    STTR,//transfer Recive
    I,//Channel Income
    E,//Channel Expenses
    ICAN,//Channel Income Cancel
    ECAN,//Channel Expenses Cancel
    ;

    public String getSuffix() {
        if (this == BillNumberSuffix.NONE) {
            return "";
        } else {
            String suffix = this.toString();
            switch (this) {
                case ADJ:break;
                case AGNCAN:break;
                case AGNCN:break;
                case AGNCNCAN:break;
                case AGNDN:break;
                case AGNDNCAN:break;
                case AGNPAY:break;
                case CAN:
                    suffix = "C";
                    break;
                case CCCAN:
                    suffix = "CC";
                    break;
                case CCCN:break;
                case CCCNCAN:break;
                case CCDN:break;
                case CCDNCAN:break;
                case CCPAY:
                    suffix = "CP";
                    break;
                case CHNPROPAY:break;
                case CRDCAN:break;
                case CRDPAY:break;
                case CSIN:break;
                case CSINCAN:break;
                case CSOUT:break;
                case CSOUTCAN:break;
                case DI:break;
                case DIC:break;
                case DRADJ:break;
                case E:break;
                case ECAN:break;
                case GRN:suffix = "G";
                    break;
                case GRNCAN:suffix = "GC";
                    break;
                case GRNRET:
                    suffix = "GR";
                    break;
                case GRNRETCAN:
                    suffix = "CRC";
                    break;
                case I:
                    suffix = "W";
                    break;
                case ICAN:
                    suffix = "WC";
                    break;
                case INWCAN:
                    suffix = "WC";
                    break;
                case INWFINAL:
                    suffix = "WF";
                    break;
                case INWINTRIM:
                    suffix = "WI";
                    break;
                case INWPAY:
                    suffix = "WA";
                    break;
                case INWPRO:
                    suffix = "WP";
                    break;
                case INWREF:break;
                case INWREFCAN:break;
                case INWSER:break;
                case ISS:break;
                case ISSCAN:break;
                case MJADJ:break;
                case NONE:break;
                case PACK:break;
                case PHCAN:break;
                case PHDIRRET:break;
                case PHISSCAN:break;
                case PHISSRET:break;
                case PHISSUE:break;
                case PHISSUEREQ:break;
                case PHPUR:break;
                case PHRET:break;
                case PHSAL:break;
                case PHTI:break;
                case PHTICAN:break;
                case PHTR:break;
                case PHTRCAN:break;
                case PHTRQ:break;
                case PO:break;
                case POCAN:break;
                case POR:break;
                case SALE: suffix = "S"; break;
                case SALCAN: suffix = "SC"; break;
                default:
                    suffix = this.toString();
            }

            return suffix;
        }
    }
}
