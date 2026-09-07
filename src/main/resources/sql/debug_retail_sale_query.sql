-- Debug query for retail sale calculations
-- Replace the date values below with your actual date range

SELECT
    SUM(pbi.qty * ib.PURCAHSERATE) as total_purchase_value,
    SUM(pbi.qty * ib.COSTRATE) as total_cost_value,
    SUM(pbi.qty * ib.RETAILSALERATE) as total_retail_value
FROM
    BILLITEM bi
    INNER JOIN BILL b ON bi.bill_id = b.id
    INNER JOIN PHARMACEUTICALBILLITEM pbi ON pbi.billitem_id = bi.id
    INNER JOIN ITEMBATCH ib ON pbi.itembatch_id = ib.id
WHERE
    bi.retired = 0
    AND b.BILLTYPEATOMIC IN (
        'PHARMACY_RETAIL_SALE',
        'PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER',
        'PHARMACY_RETAIL_SALE_CANCELLED',
        'PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS',
        'PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS'
    )
    AND b.CREATEDAT BETWEEN '2025-01-01' AND '2025-12-31';  -- Change these dates

-- Detailed breakdown query to see individual records
SELECT
    bi.id as billitem_id,
    pbi.id as pbi_id,
    b.id as bill_id,
    b.BILLTYPEATOMIC,
    b.PAYMENTMETHOD,
    pbi.qty,
    ib.PURCAHSERATE,
    ib.COSTRATE,
    ib.RETAILSALERATE,
    (pbi.qty * ib.PURCAHSERATE) as purchase_value,
    (pbi.qty * ib.COSTRATE) as cost_value,
    (pbi.qty * ib.RETAILSALERATE) as retail_value,
    b.CREATEDAT
FROM
    BILLITEM bi
    INNER JOIN BILL b ON bi.bill_id = b.id
    INNER JOIN PHARMACEUTICALBILLITEM pbi ON pbi.billitem_id = bi.id
    INNER JOIN ITEMBATCH ib ON pbi.itembatch_id = ib.id
WHERE
    bi.retired = 0
    AND b.BILLTYPEATOMIC IN (
        'PHARMACY_RETAIL_SALE',
        'PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER',
        'PHARMACY_RETAIL_SALE_CANCELLED',
        'PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS',
        'PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS'
    )
    AND b.CREATEDAT BETWEEN '2025-01-01' AND '2025-12-31'  -- Change these dates
ORDER BY b.BILLTYPEATOMIC, b.CREATEDAT;

-- Summary by BILL type to understand the distribution
SELECT
    b.BILLTYPEATOMIC,
    COUNT(*) as record_count,
    SUM(pbi.qty) as total_qty,
    SUM(pbi.qty * ib.PURCAHSERATE) as total_purchase_value,
    SUM(pbi.qty * ib.COSTRATE) as total_cost_value,
    SUM(pbi.qty * ib.RETAILSALERATE) as total_retail_value
FROM
    BILLITEM bi
    INNER JOIN BILL b ON bi.bill_id = b.id
    INNER JOIN PHARMACEUTICALBILLITEM pbi ON pbi.billitem_id = bi.id
    INNER JOIN ITEMBATCH ib ON pbi.itembatch_id = ib.id
WHERE
    bi.retired = 0
    AND b.BILLTYPEATOMIC IN (
        'PHARMACY_RETAIL_SALE',
        'PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER',
        'PHARMACY_RETAIL_SALE_CANCELLED',
        'PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS',
        'PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS'
    )
    AND b.CREATEDAT BETWEEN '2025-01-01' AND '2025-12-31'  -- Change these dates
GROUP BY b.BILLTYPEATOMIC
ORDER BY b.BILLTYPEATOMIC;
