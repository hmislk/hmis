/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.Bill;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author buddhika
 */
@Stateless
public class BillFacade extends AbstractFacade<Bill> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public BillFacade() {
        super(Bill.class);
    }

    /**
     * Corrects historical bill-level fee aggregation data for bills affected by
     * the OPD billing fee aggregation bug (April 18, 2025 - January 22, 2026).
     *
     * This method uses native SQL to efficiently update bill-level fee totals
     * based on aggregating BillItem-level fees.
     *
     * @param fromDate Start date for correction (inclusive)
     * @param toDate End date for correction (inclusive)
     * @param dryRun If true, only counts affected records without updating
     * @return Number of bills corrected
     * @throws Exception If correction fails
     */
    public int correctHistoricalBillFeeAggregation(Date fromDate, Date toDate, boolean dryRun) throws Exception {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("From date and to date cannot be null");
        }

        if (fromDate.after(toDate)) {
            throw new IllegalArgumentException("From date cannot be after to date");
        }

        // Detect correct table name case (BILL vs bill)
        String[] tableNamePairs = detectTableNameCase();
        String billTableName = tableNamePairs[0];
        String billItemTableName = tableNamePairs[1];

        try {
            // First, get count of bills that need correction
            String countSql =
                "SELECT COUNT(*) as bill_count " +
                "FROM " + billTableName + " b " +
                "WHERE b.CREATEDAT >= ? AND b.CREATEDAT <= ? " +
                "AND b.RETIRED = false " +
                "AND b.BILLTYPE = 'OpdBill' " +
                "AND (b.TOTALHOSPITALFEE IS NULL OR b.TOTALHOSPITALFEE = 0 " +
                "     OR b.TOTALSTAFFFEE IS NULL OR b.TOTALSTAFFFEE = 0 " +
                "     OR b.TOTALCENTERFEE IS NULL OR b.TOTALCENTERFEE = 0)";

            javax.persistence.Query countQuery = getEntityManager().createNativeQuery(countSql);
            countQuery.setParameter(1, fromDate);
            countQuery.setParameter(2, toDate);

            Object countResult = countQuery.getSingleResult();
            int billsToCorrect = ((Number) countResult).intValue();

            if (billsToCorrect == 0) {
                System.out.println("No bills found that need fee aggregation correction in the specified date range.");
                return 0;
            }

            System.out.println("Found " + billsToCorrect + " bills that need fee aggregation correction.");

            if (dryRun) {
                System.out.println("DRY RUN: Would correct " + billsToCorrect + " bills.");
                return billsToCorrect;
            }

            // Update bill-level fees based on BillItem aggregation
            String updateSql =
                "UPDATE " + billTableName + " b " +
                "SET " +
                "    b.TOTALHOSPITALFEE = (" +
                "        SELECT COALESCE(SUM(bi.HOSPITALFEE), 0) " +
                "        FROM " + billItemTableName + " bi " +
                "        WHERE bi.BILL_ID = b.ID AND bi.RETIRED = false" +
                "    ), " +
                "    b.TOTALSTAFFFEE = (" +
                "        SELECT COALESCE(SUM(bi.STAFFFEE), 0) " +
                "        FROM " + billItemTableName + " bi " +
                "        WHERE bi.BILL_ID = b.ID AND bi.RETIRED = false" +
                "    ), " +
                "    b.TOTALCENTERFEE = (" +
                "        SELECT COALESCE(SUM(bi.COLLECTINGCENTREFEE), 0) " +
                "        FROM " + billItemTableName + " bi " +
                "        WHERE bi.BILL_ID = b.ID AND bi.RETIRED = false" +
                "    ), " +
                "    b.PERFORMINSTITUTIONFEE = (" +
                "        SELECT COALESCE(SUM(bi.HOSPITALFEE), 0) " +
                "        FROM " + billItemTableName + " bi " +
                "        WHERE bi.BILL_ID = b.ID AND bi.RETIRED = false" +
                "    ) " +
                "WHERE b.CREATEDAT >= ? AND b.CREATEDAT <= ? " +
                "AND b.RETIRED = false " +
                "AND b.BILLTYPE = 'OpdBill' " +
                "AND (b.TOTALHOSPITALFEE IS NULL OR b.TOTALHOSPITALFEE = 0 " +
                "     OR b.TOTALSTAFFFEE IS NULL OR b.TOTALSTAFFFEE = 0 " +
                "     OR b.TOTALCENTERFEE IS NULL OR b.TOTALCENTERFEE = 0)";

            javax.persistence.Query updateQuery = getEntityManager().createNativeQuery(updateSql);
            updateQuery.setParameter(1, fromDate);
            updateQuery.setParameter(2, toDate);

            int updatedCount = updateQuery.executeUpdate();

            System.out.println("Successfully corrected fee aggregation for " + updatedCount + " bills.");

            return updatedCount;

        } catch (Exception e) {
            System.err.println("Error correcting historical bill fee aggregation: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Convenience method to correct all bills affected by the April 2025 OPD billing bug.
     * Covers the period from April 18, 2025 to January 22, 2026.
     *
     * @param dryRun If true, only counts affected records without updating
     * @return Number of bills corrected
     * @throws Exception If correction fails
     */
    public int correctOpdBillingBugData(boolean dryRun) throws Exception {
        // April 18, 2025 - when Lawan commented out the fee aggregation
        Date fromDate = java.sql.Date.valueOf("2025-04-18");
        // January 22, 2026 - when the bug was fixed
        Date toDate = java.sql.Date.valueOf("2026-01-22");

        System.out.println("Correcting OPD billing fee aggregation data for period: " +
                         fromDate + " to " + toDate);

        return correctHistoricalBillFeeAggregation(fromDate, toDate, dryRun);
    }

    /**
     * Detects the correct table name case (uppercase vs lowercase) for the current database.
     * Some database instances use BILL/BILLITEM while others use bill/billitem.
     *
     * @return Array of [billTableName, billItemTableName] with correct case
     * @throws Exception if neither case works
     */
    private String[] detectTableNameCase() throws Exception {
        String[] uppercaseNames = {"BILL", "BILLITEM"};
        String[] lowercaseNames = {"bill", "billitem"};

        // Try uppercase first
        try {
            String testSql = "SELECT COUNT(*) FROM " + uppercaseNames[0] + " WHERE 1=0";
            javax.persistence.Query testQuery = getEntityManager().createNativeQuery(testSql);
            testQuery.getSingleResult();
            // If we reach here, uppercase works
            System.out.println("Detected uppercase table names: BILL, BILLITEM");
            return uppercaseNames;
        } catch (Exception e1) {
            // Try lowercase
            try {
                String testSql = "SELECT COUNT(*) FROM " + lowercaseNames[0] + " WHERE 1=0";
                javax.persistence.Query testQuery = getEntityManager().createNativeQuery(testSql);
                testQuery.getSingleResult();
                // If we reach here, lowercase works
                System.out.println("Detected lowercase table names: bill, billitem");
                return lowercaseNames;
            } catch (Exception e2) {
                // Neither worked - this is a serious problem
                String errorMsg = "Could not detect table name case. Tried both uppercase (BILL/BILLITEM) and lowercase (bill/billitem). " +
                                 "Uppercase error: " + e1.getMessage() + ". Lowercase error: " + e2.getMessage();
                System.err.println(errorMsg);
                throw new Exception(errorMsg);
            }
        }
    }

}
