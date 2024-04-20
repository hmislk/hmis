package com.divudi.bean.common;

import com.divudi.data.ItemLight;
import com.divudi.entity.Packege;
import com.divudi.entity.Service;
import com.divudi.entity.lab.Investigation;
import com.divudi.facade.ItemFacade;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.ApplicationScoped;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named
@ApplicationScoped
public class ItemApplicationController {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    ItemFacade itemFacade;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private List<ItemLight> items;
    private List<ItemLight> investigationsAndServices;
    private List<ItemLight> investigations;
    private List<ItemLight> services;
    private List<ItemLight> packages;
    // </editor-fold>

    /**
     * Creates a new instance of ItemApplicationController
     */
    public ItemApplicationController() {
    }

    public List<ItemLight> fillAllItems() {
        String jpql = "SELECT new com.divudi.data.ItemLight("
                + "i.id, "
                + "CASE WHEN d.name IS NULL THEN 'No Department' ELSE d.name END, "
                + "i.name, i.code, i.total) "
                + "FROM Item i "
                + "LEFT JOIN i.department d "
                + "WHERE i.retired = :ret AND (TYPE(i) = Investigation OR TYPE(i) = Service OR TYPE(i) = MedicalPackage ) "
                + "ORDER BY i.name";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ret", false);

        List<ItemLight> lst = (List<ItemLight>) itemFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);
        return lst;
    }

    public void reloadItems() {
        List<ItemLight> reloaded = fillAllItems();
        items = reloaded;
        packages = null;
        services = null;
        investigations = null;
        investigationsAndServices = null;
        reloaded = null;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Other">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public List<ItemLight> getItems() {
        if (items == null) {
            items = fillAllItems();
        }
        return items;
    }

    public void setItems(List<ItemLight> items) {
        this.items = items;
    }

    public List<ItemLight> getInvestigations() {
        if (investigations == null) {
            investigations = fillInvestigations();
        }
        return investigations;
    }

    public List<ItemLight> getServices() {
        if (services == null) {
            services = fillServices();
        }
        return services;
    }

    public List<ItemLight> getPackages() {
        if (packages == null) {
            packages = fillPackages();
        }
        return packages;
    }

    // </editor-fold>
    public List<ItemLight> getInvestigationsAndServices() {
        if (investigationsAndServices == null) {
            investigationsAndServices = fillAllItems();
        }
        return investigationsAndServices;
    }

    private List<ItemLight> fillPackages() {
        System.out.println("fillPackages");
        String jpql = "SELECT new com.divudi.data.ItemLight("
                + "i.id, i.orderNo, i.isMasterItem, i.hasReportFormat, "
                + "c.name, c.id, ins.name, ins.id, "
                + "d.name, d.id, s.name, s.id, "
                + "p.name, stf.id, i.name, i.code, i.barcode, "
                + "i.printName, i.shortName, i.fullName, i.total) "
                + "FROM Item i "
                + "LEFT JOIN i.category c "
                + "LEFT JOIN i.institution ins "
                + "LEFT JOIN i.department d "
                + "LEFT JOIN i.speciality s "
                + "LEFT JOIN i.staff stf "
                + "LEFT JOIN stf.person p "
                + "WHERE i.retired = :ret AND TYPE(i) = Packege "
                + "ORDER BY i.name";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ret", false);
        List<ItemLight> lst = (List<ItemLight>) itemFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);
        return lst;
    }

    private List<ItemLight> fillInvestigations() {
        String jpql = "SELECT new com.divudi.data.ItemLight("
                + "i.id, i.orderNo, i.isMasterItem, i.hasReportFormat, "
                + "c.name, c.id, ins.name, ins.id, "
                + "d.name, d.id, s.name, s.id, "
                + "p.name, stf.id, i.name, i.code, i.barcode, "
                + "i.printName, i.shortName, i.fullName, i.total) "
                + "FROM Item i "
                + "LEFT JOIN i.category c "
                + "LEFT JOIN i.institution ins "
                + "LEFT JOIN i.department d "
                + "LEFT JOIN i.speciality s "
                + "LEFT JOIN i.staff stf "
                + "LEFT JOIN stf.person p "
                + "WHERE i.retired = :ret AND TYPE(i) = Investigation "
                + "ORDER BY i.name";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ret", false);

        List<ItemLight> lst = (List<ItemLight>) itemFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);
        return lst;
    }

    private List<ItemLight> fillServices() {
        String jpql = "SELECT new com.divudi.data.ItemLight("
                + "i.id, i.orderNo, i.isMasterItem, i.hasReportFormat, "
                + "c.name, c.id, ins.name, ins.id, "
                + "d.name, d.id, s.name, s.id, "
                + "p.name, stf.id, i.name, i.code, i.barcode, "
                + "i.printName, i.shortName, i.fullName, i.total) "
                + "FROM Item i "
                + "LEFT JOIN i.category c "
                + "LEFT JOIN i.institution ins "
                + "LEFT JOIN i.department d "
                + "LEFT JOIN i.speciality s "
                + "LEFT JOIN i.staff stf "
                + "LEFT JOIN stf.person p "
                + "WHERE i.retired = :ret AND TYPE(i) = Service "
                + "ORDER BY i.name";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ret", false);

        List<ItemLight> lst = (List<ItemLight>) itemFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);
        return lst;
    }

}
