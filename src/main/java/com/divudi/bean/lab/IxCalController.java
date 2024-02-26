/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.InvestigationItemType;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.entity.lab.IxCal;
import com.divudi.entity.lab.ReportItem;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.InvestigationItemFacade;
import com.divudi.facade.IxCalFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent; import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 Informatics)
 */
@Named
@SessionScoped
public  class IxCalController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private IxCalFacade ixCalFacade;
    @EJB
    InvestigationItemFacade iiFacade;
    @EJB
    InvestigationFacade ixFacade;
    @EJB
    IxCalFacade iivcFacade;
    private List<IxCal> items = null;
    List<InvestigationItem> vals;
    List<InvestigationItem> cals;
    Investigation ix;
    InvestigationItem val;
    InvestigationItem cal;
    IxCal reIxCal;
    IxCal addingIxCal;

    public IxCalFacade getIivcFacade() {
        return iivcFacade;
    }

    public void setIivcFacade(IxCalFacade iivcFacade) {
        this.iivcFacade = iivcFacade;
    }

    public IxCal getAddingIxCal() {
        if (addingIxCal == null) {
            addingIxCal = new IxCal();
        }
        return addingIxCal;
    }

    public void setAddingIxCal(IxCal addingIxCal) {
        this.addingIxCal = addingIxCal;
    }

    public IxCal getReIxCal() {
        return reIxCal;
    }

    public void setReIxCal(IxCal reIxCal) {
        this.reIxCal = reIxCal;
    }

    public void addCal() {
        if (ix == null) {
            JsfUtil.addErrorMessage("Investigation ?");
            return;
        }
        if (cal == null) {
            JsfUtil.addErrorMessage("Calculation ?");
            return;
        }
        if (addingIxCal == null) {
            JsfUtil.addErrorMessage("Cal?");
            return;
        }
        addingIxCal.setCalIxItem(cal);
        //////// // System.out.println("id is " + addingIxCal.getId());
        if (addingIxCal.getId() == null || addingIxCal.getId() == 0) {
            //////// // System.out.println("iivc creating");
            getIivcFacade().create(addingIxCal);
        } else {
            //////// // System.out.println("iivc editing");
            getIivcFacade().edit(addingIxCal);
        }
        items.add(addingIxCal);
        addingIxCal = new IxCal();
        JsfUtil.addSuccessMessage("Added");

    }

    public IxCal lastCal() {
        IxCal tcal = null;
        if (items != null || items.isEmpty()!=true) {
            //////// // System.out.println("items are null or empty");
            tcal = items.get(items.size() - 1);
        }
        return tcal;
    }

    public void removeLastCal() {
        if (items != null && items.isEmpty() != true) {
            IxCal tcal = lastCal();
            tcal.setRetired(true);
            tcal.setRetiredAt(new Date());
            tcal.setRetirer(sessionController.getLoggedUser());
            getIivcFacade().edit(tcal);
        }
    }

    public InvestigationItem getCal() {
        return cal;
    }

    public void setCal(InvestigationItem cal) {
        this.cal = cal;
    }

    public InvestigationItemFacade getIiFacade() {
        return iiFacade;
    }

    public void setIiFacade(InvestigationItemFacade iiFacade) {
        this.iiFacade = iiFacade;
    }

    public InvestigationFacade getIxFacade() {
        return ixFacade;
    }

    public void setIxFacade(InvestigationFacade ixFacade) {
        this.ixFacade = ixFacade;
    }

    public Investigation getIx() {
        return ix;
    }

    public void setIx(Investigation ix) {
        this.ix = ix;
    }

    public List<InvestigationItem> getVals() {
        if (ix != null) {
            Map m = new HashMap();
            m.put("iit", InvestigationItemType.Value);
            vals = getIiFacade().findByJpql("select i from InvestigationItem i where i.retired=false and i.item.id = " + ix.getId() + " and i.ixItemType =:iit", m, TemporalType.TIMESTAMP);
        }
        if (vals == null) {
            vals = new ArrayList<InvestigationItem>();
        }
//        vals = new ArrayList<InvestigationItem>();
//        if (ix != null) {
//            for (ReportItem ii : ix.getReportItems()) {
//                if (ii instanceof InvestigationItem && ii.getIxItemType() == InvestigationItemType.Value) {
//                    vals.add((InvestigationItem) ii);
//                }
//            }
//        }
        return vals;
    }

    public List<InvestigationItem> getCals() {
        if (ix != null) {
            String jpql;
            jpql = "select i from InvestigationItem i where i.retired=false and i.item.id = " + ix.getId() + " and i.ixItemType = :iit order by i.cssTop";
            Map m = new HashMap();
            m.put("iit", InvestigationItemType.Calculation);
            cals = getIiFacade().findByJpql(jpql, m, TemporalType.TIMESTAMP);
            if (cals == null) {
                cals = new ArrayList<InvestigationItem>();
            }
        }
//        cals = new ArrayList<InvestigationItem>();
        if (ix != null) {
            //////// // System.out.println("ii count is " + ix.getReportItems().size());
            for (ReportItem ii : ix.getReportItems()) {
                
                if (ii instanceof InvestigationItem && ii.getIxItemType() == InvestigationItemType.Calculation) {
                    //////// // System.out.println("ii is " + ii);
                }
            }
        }
        return cals;
    }

    public InvestigationItem getVal() {
        return val;
    }

    public void setVal(InvestigationItem val) {
        this.val = val;
    }

    public IxCalFacade getEjbFacade() {
        return ixCalFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public IxCalController() {
    }

    private IxCalFacade getFacade() {
        return ixCalFacade;
    }

    public List<IxCal> getItems() {
        String sql;
        if (ix != null && cal != null) {
            sql = "select i from IxCal i where (i.retired=false or i.retired is null) and i.calIxItem.id = " + cal.getId();
            ////// // System.out.println("sql = " + sql);
            items = getFacade().findByJpql(sql);
        }
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = IxCal.class)
    public static class IxCalControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            IxCalController controller = (IxCalController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "ixCalController");
            return controller.ixCalFacade.find(getKey(value));
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
            if (object instanceof IxCal) {
                IxCal o = (IxCal) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + IxCalController.class.getName());
            }
        }
    }
}
