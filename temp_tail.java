        StockHistory sh = new StockHistory();
        Date now = new Date();
        Calendar cal = Calendar.getInstance();

        sh.setFromDate(now);
        sh.setPbItem(phItem);
        sh.setHxDate(cal.get(Calendar.DATE));
        sh.setHxMonth(cal.get(Calendar.MONTH));
        sh.setHxWeek(cal.get(Calendar.WEEK_OF_YEAR));
        sh.setHxYear(cal.get(Calendar.YEAR));

        sh.setStockAt(now);
        sh.setCreatedAt(now);
        sh.setDepartment(d);
        sh.setInstitution(d.getInstitution());

        Stock fetchedStock = getStockFacade().findWithoutCache(stock.getId());
        sh.setStockQty(fetchedStock.getStock());

        // Ensure AMP is used for item tracking
        sh.setItem(amp);
        sh.setItemBatch(fetchedStock.getItemBatch());

        double itemStock = getStockQty(amp, d);
        double institutionStock = getStockQty(amp, d.getInstitution());
        double totalStock = getStockQty(amp);

        sh.setItemStock(itemStock);
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
