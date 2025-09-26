
            sh.setItemStockValueAtCostRate(itemStock * costRate);
            sh.setInstitutionItemStockValueAtCostRate(institutionStock * costRate);
            sh.setTotalItemStockValueAtCostRate(totalStock * costRate);
        }

        if (sh.getId() == null) {
            getStockHistoryFacade().createAndFlush(sh);
        } else {
            getStockHistoryFacade().editAndCommit(sh);
        }

        phItem.setStockHistory(sh);
        getPharmaceuticalBillItemFacade().editAndCommit(phItem);
    }

    public void addToStockHistory(PharmaceuticalBillItem phItem, Stock stock, Staff staff) {
        if (phItem == null) {
            return;
        }

        if (phItem.getBillItem() == null) {
            return;
        }

        if (phItem.getBillItem().getItem() == null) {
            return;
        }

        // Extract AMP (Actual Medicinal Product) even if input is AMPP (Pack) for stock calculations
        Item originalItem = phItem.getBillItem().getItem();
        Item amp = originalItem;
        if (amp instanceof Ampp) {
            amp = ((Ampp) amp).getAmp();
        }

        StockHistory sh = new StockHistory();
        sh.setFromDate(Calendar.getInstance().getTime());
        sh.setPbItem(phItem);
        sh.setHxDate(Calendar.getInstance().get(Calendar.DATE));
        sh.setHxMonth(Calendar.getInstance().get(Calendar.MONTH));
        sh.setHxWeek(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
        sh.setHxYear(Calendar.getInstance().get(Calendar.YEAR));

        sh.setStockAt(Calendar.getInstance().getTime());

        sh.setStaff(staff);
        Stock fetchedStock = getStockFacade().findWithoutCache(stock.getId());

        sh.setStockQty(fetchedStock.getStock());

        // Use AMP for stock calculations since stocks are stored for AMPs only
        double itemStock = getStockQty(amp, phItem.getBillItem().getBill().getDepartment());
        double institutionStock = getStockQty(amp, phItem.getBillItem().getBill().getFromDepartment().getInstitution());
        double totalStock = getStockQty(amp);

        sh.setItemStock(itemStock);
        sh.setItem(originalItem); // Keep original item reference for history
        sh.setItemBatch(fetchedStock.getItemBatch());
        sh.setCreatedAt(new Date());
        sh.setInstitutionItemStock(institutionStock);
        sh.setTotalItemStock(totalStock);

        // Calculate stock values at different rates
        if (fetchedStock.getItemBatch() != null) {
            double purchaseRate = fetchedStock.getItemBatch().getPurcahseRate();
            Double costRateObj = fetchedStock.getItemBatch().getCostRate();
            double costRate = costRateObj != null ? costRateObj : 0.0;

            sh.setItemStockValueAtPurchaseRate(itemStock * purchaseRate);
            sh.setInstitutionItemStockValueAtPurchaseRate(institutionStock * purchaseRate);
            sh.setTotalItemStockValueAtPurchaseRate(totalStock * purchaseRate);

            sh.setItemStockValueAtCostRate(itemStock * costRate);
            sh.setInstitutionItemStockValueAtCostRate(institutionStock * costRate);
            sh.setTotalItemStockValueAtCostRate(totalStock * costRate);
        }
        if (sh.getId() == null) {
            getStockHistoryFacade().createAndFlush(sh);
        } else {
            getStockHistoryFacade().editAndFlush(sh);
        }

        phItem.setStockHistory(sh);
        getPharmaceuticalBillItemFacade().editAndCommit(phItem);
    }

    //
    /**
     * ⚠️⚠️⚠️ CRITICAL INVENTORY MANAGEMENT METHOD - DO NOT MODIFY ⚠️⚠️⚠️
     * 
     * 🚨 WARNING TO ALL DEVELOPERS AND AI AGENTS: 🚨
     * This method handles CRITICAL stock addition operations that directly affect:
     * - Real money and financial reports
     * - Regulatory compliance and audit trails  
     * - Inventory accuracy across the entire system
     * - Purchase order and GRN processing
     * 
     * 🛑 NEVER MODIFY THIS METHOD WITHOUT:
     * 1. Senior developer + Financial controller approval
     * 2. Full backup and rollback plan
     * 3. Extensive testing with audit verification
     * 4. Regulatory compliance review
     * 
     * 📋 This method correctly handles:
     * - Stock level addition with proper validation
     * - Database consistency with editAndFlush
     * - Audit trail creation via addToStockHistory  
     * - Proper error handling with boolean return
     * 
     * 🚨 CRITICAL RULE FOR AMP/AMPP HANDLING: 🚨
     * ❌ DO NOT modify input parameters (pbi.getBillItem().setItem()) in this method
     * ❌ DO NOT add AMP/AMPP conversion logic here
     * ✅ Handle AMP/AMPP conversions in CONTROLLERS before calling this method
     * ✅ Ensure Stock objects passed to this method are already associated with correct AMP items
     * 
     * This method is FULLY TESTED and PRODUCTION STABLE. Any AMP/AMPP issues 
     * should be resolved at the controller level, NOT here.
     */
    public boolean addToStock(Stock stock, double qty, PharmaceuticalBillItem pbi, Department d) {
