package com.divudi.service;

import com.divudi.core.entity.Item;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.data.StockValueRow;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.AmpFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.util.CommonFunctions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.TemporalType;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author M H B Ariyaratne, buddhika.ari@gmail.com
 *
 */
@Stateless
public class StockService {

    @EJB
    StockFacade stockFacade;
    @EJB
    AmpFacade ampFacade;

    // ChatGPT contributed - 2025-05
    @Asynchronous
    @Deprecated // Now these details are NOT recorded in the Stock. Instead have to take from Item Batch as it used to be
    public void addItemNamesToAllStocksAsync() {
        int batchSize = 500;
        int start = 0;
        List<Stock> batch;
        do {
            batch = stockFacade.findByJpqlWithRange("SELECT s FROM Stock s order by s.stock desc", start, batchSize);
            for (Stock s : batch) {
                if (s.getItemBatch() != null && s.getItemBatch().getItem() != null && s.getItemBatch().getItem().getName() != null) {
                    s.setItemName(s.getItemBatch().getItem().getName());
                    s.setBarcode(s.getItemBatch().getItem().getBarcode());
                    String code = s.getItemBatch().getItem().getCode();
                    Long longCode = CommonFunctions.stringToLong(code);
                    s.setLongCode(longCode);
                    s.setDateOfExpire(s.getItemBatch().getDateOfExpire());
                    s.setRetailsaleRate(s.getItemBatch().getRetailsaleRate());
                } else {
                    s.setItemName("UNKNOWN");
                }
            }
            stockFacade.batchEdit(batch, batchSize);
            start += batchSize;
        } while (!batch.isEmpty());
    }

// ChatGPT contributed - 2025-05
    @Deprecated // Now these details are NOT recorded in the Stock. Instead have to take from Item Batch as it used to be
    public void addItemNamesToAllStocks() {
        List<Stock> allStocks = stockFacade.findByJpql("SELECT s FROM Stock s");
        int count = 0;
        for (Stock s : allStocks) {
            if (s.getItemBatch() != null && s.getItemBatch().getItem() != null && s.getItemBatch().getItem().getName() != null) {
                s.setItemName(s.getItemBatch().getItem().getName());
                s.setBarcode(s.getItemBatch().getItem().getBarcode());
                String code = s.getItemBatch().getItem().getCode();
                Long longCode = CommonFunctions.stringToLong(code);
                s.setLongCode(longCode);
                s.setDateOfExpire(s.getItemBatch().getDateOfExpire());
                s.setRetailsaleRate(s.getItemBatch().getRetailsaleRate());
            } else {
                s.setItemName("UNKNOWN");
            }
            stockFacade.edit(s);
            count++;
        }
    }

    // ChatGPT contributed - 2025-06
    public StockValueRow calculateStockValues(Institution institution, Institution site, Department department) {
        System.out.println("calculateStockValues");
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();
        jpql.append("select sum(s.stock * s.itemBatch.purcahseRate), "
                + "sum(s.stock * s.itemBatch.retailsaleRate), "
                + "sum(s.stock * coalesce(s.itemBatch.costRate,0)) "
                + "from Stock s where s.stock>0");

        if (department != null) {
            jpql.append(" and s.department=:dep");
            params.put("dep", department);
        } else if (site != null) {
            jpql.append(" and s.department.site=:site");
            params.put("site", site);
        } else if (institution != null) {
            jpql.append(" and s.department.institution=:ins");
            params.put("ins", institution);
        }

        Object[] obj = stockFacade.findAggregateModified(jpql.toString(), params, TemporalType.TIMESTAMP);
        
        
        StockValueRow row = new StockValueRow();
        row.setInstitution(institution);
        row.setSite(site);
        row.setDepartment(department);
        if (obj != null) {
            if (obj[0] != null) {
                row.setPurchaseValue((Double) obj[0]);
            }
            if (obj[1] != null) {
                row.setRetailValue((Double) obj[1]);
            }
            if (obj[2] != null) {
                row.setCostValue((Double) obj[2]);
            }
        }
        return row;
    }

    private List<Amp> ampsOfVmp(Item vmp) {
        List<Amp> suggestions = new ArrayList<>();
        if (!(vmp instanceof Vmp)) {
            return suggestions;
        }
        String jpql = "select a from Amp a "
                + " where a.retired=:ret" 
                + " and a.vmp=:vmp "
                + " order by a.name";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("vmp", vmp);
        suggestions = ampFacade.findByJpql(jpql, m);
        return suggestions;
    }

    public double findStock(Item item) {
        return findStock((Institution) null, item);
    }

    public double findStock(Institution institution, Item item) {
        if (item instanceof Amp) {
            Amp amp = (Amp) item;
            return findStock(institution, amp);
        } else if (item instanceof Vmp) {
            List<Amp> amps = ampsOfVmp(item);
            return findStock(institution, amps);
        } else {
            return 0.0;
        }
    }

    public double findInstitutionStock(Institution institution, Item item) {
        return findStock(institution, item);
    }

    public double findDepartmentStock(Department department, Item item) {
        if (item instanceof Amp) {
            Amp amp = (Amp) item;
            List<Amp> amps = new ArrayList<>();
            amps.add(amp);
            return findStock(department, amps);
        } else if (item instanceof Vmp) {
            List<Amp> amps = ampsOfVmp(item);
            return findStock(department, amps);
        } else {
            return 0.0;
        }
    }

    public double findSiteStock(Institution site, Item item) {
        if (item instanceof Amp) {
            Amp amp = (Amp) item;
            List<Amp> amps = new ArrayList<>();
            amps.add(amp);
            return findSiteStock(site, amps);
        } else if (item instanceof Vmp) {
            List<Amp> amps = ampsOfVmp(item);
            return findSiteStock(site, amps);
        } else {
            return 0.0;
        }
    }

    public double findStock(Institution institution, Institution site, Department department, Item item) {
        if (item instanceof Amp) {
            return findStock(institution, site, department, (Amp) item);
        } else {
            return 0.0;
        }
    }

    public double findStock(Institution institution, Institution site, Department department, Amp amp) {
        String jpql = "select sum(i.stock) from Stock i where i.retired=false and i.itemBatch.item=:amp";
        Map<String, Object> m = new HashMap<>();
        m.put("amp", amp);
        if (department != null) {
            jpql += " and i.department=:dep";
            m.put("dep", department);
        } else if (institution != null && site != null) {
            jpql += " and i.department.site=:site and i.department.institution=:ins";
            m.put("site", site);
            m.put("ins", institution);
        } else if (institution != null) {
            jpql += " and i.department.institution=:ins";
            m.put("ins", institution);
        } else if (site != null) {
            jpql += " and i.department.site=:site";
            m.put("site", site);
        }
        return stockFacade.findDoubleByJpql(jpql, m);
    }

    public double findStock(Institution institution, Amp item) {
        List<Amp> amps = new ArrayList<>();
        amps.add(item);
        return findStock(institution, amps);
    }

    public double findStock(Institution institution, List<Amp> amps) {
        Double stock;
        String jpql;
        Map<String, Object> m = new HashMap<>();
        m.put("amps", amps);
        jpql = "select sum(i.stock) "
                + " from Stock i ";
        if (institution == null) {
            jpql += " where i.itemBatch.item in :amps ";
        } else {
            m.put("ins", institution);
            jpql += " where i.department.institution=:ins "
                    + " and i.itemBatch.item in :amps ";
        }
        stock = stockFacade.findDoubleByJpql(jpql, m);
        if (stock != null) {
            return stock;
        }
        return 0.0;
    }

    public double findSiteStock(Institution site, List<Amp> amps) {
        Double stock;
        String jpql;
        Map<String, Object> m = new HashMap<>();
        m.put("amps", amps);
        jpql = "select sum(i.stock) "
                + " from Stock i ";
        if (site == null) {
            jpql += " where i.itemBatch.item in :amps ";
        } else {
            m.put("ins", site);
            jpql += " where i.department.site=:ins "
                    + " and i.itemBatch.item in :amps ";
        }
        stock = stockFacade.findDoubleByJpql(jpql, m);
        if (stock != null) {
            return stock;
        }
        return 0.0;
    }

    public double findStock(Department department, List<Amp> amps) {
        Double stock;
        String jpql;
        Map<String, Object> m = new HashMap<>();
        m.put("amps", amps);
        jpql = "select sum(i.stock) "
                + " from Stock i ";
        if (department == null) {
            jpql += " where i.itemBatch.item in :amps ";
        } else {
            m.put("dep", department);
            jpql += " where i.department=:dep "
                    + " and i.itemBatch.item in :amps ";
        }
        stock = stockFacade.findDoubleByJpql(jpql, m);
        if (stock != null) {
            return stock;
        }
        return 0.0;
    }

}

