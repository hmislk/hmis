# Migration v2.1.5 - PRICEMATRIX Index Optimization

## Purpose
Adds composite indexes to PRICEMATRIX/pricematrix table to optimize PaymentSchemeDiscount queries.

**Performance Impact**: Reduces discount calculation queries from 552ms to 2-5ms (100x faster)

---

## What's Changing?

This migration improves pharmacy discount calculation performance by adding database indexes. You will notice **significantly faster checkout times** when processing retail sales with discounts.

### Expected Impact
- **Downtime Required**: None - this update runs while the system is active
- **Duration**: Approximately 3 minutes
- **User Experience**: Pharmacy cashier sales will be noticeably faster (especially with payment schemes/discounts)
- **Modules Affected**: Pharmacy, Retail Sale, Cashier Sale, Discount Management, Billing

### What You'll Notice
- Faster checkout when applying payment schemes or discounts
- Reduced waiting time during retail pharmacy sales
- Improved system responsiveness at the cashier terminal

### Support Contact
If you experience any issues after this migration:
- **Email**: [Project Support Email]
- **GitHub Issue**: [#16990](https://github.com/hmislk/hmis/issues/16990)
- **Contact**: Dr M H B Ariyaratne

---

## For System Administrators &amp; DBAs

**Full technical runbook with execution instructions**: See [developer_docs/db/migrations/v2.1.5.md](../../../../developer_docs/db/migrations/v2.1.5.md)

The runbook includes:
- Step-by-step migration execution for Production/Development environments
- Verification procedures
- Rollback instructions
- Troubleshooting guide

---

## Metadata

- **Version**: v2.1.5
- **Author**: Dr M H B Ariyaratne
- **Date**: 2025-12-13
- **GitHub Issue**: [#16990](https://github.com/hmislk/hmis/issues/16990)
- **Branch**: `16990-speed-up-the-pharmacy-retail-sale`
- **Requires Downtime**: No
- **Estimated Duration**: 3 minutes
- **Affects**: Pharmacy, Retail Sale, Cashier Sale, Discount Management, Billing

---

## Related Files

- `migration-info.json` - Detailed migration metadata
- `../../../../developer_docs/db/migrations/v2.1.5.md` - Technical runbook for DBAs and system administrators
- `../../../../developer_docs/performance/mysql-pharmacy-retail-sale-optimization.md` - Complete optimization documentation
- `../../java/com/divudi/bean/pharmacy/PharmacySaleForCashierController.java` - Uses optimized discount queries
- `../../java/com/divudi/bean/common/PriceMatrixController.java` - DTO-based discount methods
