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
import com.divudi.data.Sex;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.entity.lab.TestFlag;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.InvestigationItemFacade;
import com.divudi.facade.TestFlagFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.event.RowEditEvent;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class TestFlagController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private TestFlagFacade ejbFacade;
    @EJB
    InvestigationItemFacade investigationItemFacade;
    @EJB
    InvestigationFacade investigationFacade;

    private TestFlag current;
    private List<TestFlag> items = null;
    Investigation investigation;
    List<InvestigationItem> investigationItemsOfValueType;
    List<InvestigationItem> investigationItemsOfFlagType;
    InvestigationItem investigationItemOfValueType;
    InvestigationItem investigationItemofFlagType;
    long fromAge;
    long toAge;
    String fromAgeUnit;
    String toAgeUnit;
    String flagMessage;
    String lowMessage;
    String highMessage;
    double fromValue;
    double toValue;
    Sex sex;
    TestFlag removingFlag;

    public void onEdit(RowEditEvent event) {
        TestFlag tmp = (TestFlag) event.getObject();

        getFacade().edit(tmp);
    }

    public void saveFlags() {
        for (TestFlag f : getItems()) {
            getFacade().edit(f);
            //System.err.println("sss " + f.getNormalMessage());
        }
    }

    public double getFromValue() {
        return fromValue;
    }

    public void removeFlag() {
        if (removingFlag == null) {
            JsfUtil.addErrorMessage("Nothing to remove");
            return;
        }
        getFacade().remove(removingFlag);
        items = null;
        JsfUtil.addSuccessMessage("Removed");
    }

    public void setFromValue(double fromValue) {
        this.fromValue = fromValue;
    }

    public double getToValue() {
        return toValue;
    }

    public void setToValue(double toValue) {
        this.toValue = toValue;
    }

    public String getLowMessage() {
        return lowMessage;
    }

    public void setLowMessage(String lowMessage) {
        this.lowMessage = lowMessage;
    }

    public String getHighMessage() {
        return highMessage;
    }

    public void setHighMessage(String highMessage) {
        this.highMessage = highMessage;
    }

    public TestFlag getRemovingFlag() {
        return removingFlag;
    }

    public void setRemovingFlag(TestFlag removingFlag) {
        this.removingFlag = removingFlag;
    }

    TestFlag removingCal;

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public String getFlagMessage() {
        return flagMessage;
    }

    public void setFlagMessage(String flagMessage) {
        this.flagMessage = flagMessage;
    }

    public void addFlag() {
        //////// // System.out.println("adding flag");
        if (investigation == null) {
            JsfUtil.addErrorMessage("Select an investigation");
            return;
        }
        if (investigationItemOfValueType == null) {
            JsfUtil.addErrorMessage("Select a field");
            return;
        }
        if (investigationItemofFlagType == null) {
            JsfUtil.addErrorMessage("Select a flag");
            return;
        }

        TestFlag f = new TestFlag();
        f.setCreatedAt(Calendar.getInstance().getTime());
        f.setCreater(getSessionController().getLoggedUser());
        f.setInvestigationItemOfValueType(investigationItemOfValueType);
        f.setFlagMessage(flagMessage);
        f.setItem(investigation);
        if (getFromAgeUnit().equalsIgnoreCase("Days")) {
            f.setFromAge(fromAge);
        } else if (getFromAgeUnit().equalsIgnoreCase("Months")) {
            f.setFromAge(fromAge * 30);
        } else {
            f.setFromAge(fromAge * 365);
        }

        if (getToAgeUnit().equalsIgnoreCase("Days")) {
            f.setToAge(toAge);
        } else if (getToAgeUnit().equalsIgnoreCase("Months")) {
            f.setToAge(toAge * 30);
        } else {
            f.setToAge(toAge * 365);
        }
        f.setInvestigationItemOfFlagType(investigationItemofFlagType);
        f.setInvestigationItemOfValueType(investigationItemOfValueType);
        f.setSex(sex);
        f.setHighMessage(highMessage);
        f.setLowMessage(lowMessage);
        f.setNormalMessage(flagMessage);
        f.setFromVal(fromValue);
        f.setToVal(toValue);
        getEjbFacade().create(f);
        clearForNew();
        JsfUtil.addErrorMessage("Added");
    }

    private void clearForNew() {
        investigationItemOfValueType = null;
        investigationItemofFlagType = null;
        fromAge = 0;
        toAge = 0;
        fromAgeUnit = "Years";
        toAgeUnit = "Years";
        sex = null;
        flagMessage = "";
        lowMessage = "";
        highMessage = "";
        fromValue = 0.0;
        toValue = 0.0;
        items = null;
    }

    public String getToAgeUnit() {
        return toAgeUnit;
    }

    public void setToAgeUnit(String toAgeUnit) {
        this.toAgeUnit = toAgeUnit;
    }

    public long getFromAge() {
        return fromAge;
    }

    public void setFromAge(long fromAge) {
        this.fromAge = fromAge;
    }

    public long getToAge() {
        return toAge;
    }

    public void setToAge(long toAge) {
        this.toAge = toAge;
    }

    public String getFromAgeUnit() {
        return fromAgeUnit;
    }

    public void setFromAgeUnit(String fromAgeUnit) {
        this.fromAgeUnit = fromAgeUnit;
    }

    public InvestigationItem getInvestigationItemofFlagType() {
        return investigationItemofFlagType;
    }

    public void setInvestigationItemofFlagType(InvestigationItem investigationItemofFlagType) {
        this.investigationItemofFlagType = investigationItemofFlagType;
    }

    public InvestigationItemFacade getInvestigationItemFacade() {
        return investigationItemFacade;
    }

    public void setInvestigationItemFacade(InvestigationItemFacade investigationItemFacade) {
        this.investigationItemFacade = investigationItemFacade;
    }

    public InvestigationFacade getInvestigationFacade() {
        return investigationFacade;
    }

    public void setInvestigationFacade(InvestigationFacade investigationFacade) {
        this.investigationFacade = investigationFacade;
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation(Investigation investigation) {
        this.investigation = investigation;
        recreateModel();
    }

    public List<InvestigationItem> getInvestigationItemsOfValueType() {
        if (investigation != null) {
            Map m = new HashMap();
            m.put("iit", InvestigationItemType.Value);
            investigationItemsOfValueType = getInvestigationItemFacade().findByJpql("select i from InvestigationItem i where i.retired=false and i.item.id = " + investigation.getId() + " and i.ixItemType =:iit", m, TemporalType.TIMESTAMP);
        }
        if (investigationItemsOfValueType == null) {
            investigationItemsOfValueType = new ArrayList<InvestigationItem>();
        }
        return investigationItemsOfValueType;
    }

    public List<InvestigationItem> getInvestigationItemsOfFlagType() {

        if (investigation != null) {
            Map m = new HashMap();
            m.put("iit", InvestigationItemType.Flag);
            investigationItemsOfFlagType = getInvestigationItemFacade().findByJpql("select i from InvestigationItem i where i.retired=false and i.item.id = " + investigation.getId() + " and i.ixItemType=:iit", m, TemporalType.DATE);
        }
        if (investigationItemsOfFlagType == null) {
            investigationItemsOfFlagType = new ArrayList<InvestigationItem>();
        }
        return investigationItemsOfFlagType;
    }

    public void setInvestigationItemsOfFlagType(List<InvestigationItem> investigationItemsOfFlagType) {
        this.investigationItemsOfFlagType = investigationItemsOfFlagType;
    }

    public void setInvestigationItemsOfValueType(List<InvestigationItem> investigationItemsOfValueType) {
        this.investigationItemsOfValueType = investigationItemsOfValueType;
    }

    public InvestigationItem getInvestigationItemOfValueType() {
        return investigationItemOfValueType;
    }

    public void setInvestigationItemOfValueType(InvestigationItem investigationItemOfValueType) {
        this.investigationItemOfValueType = investigationItemOfValueType;
    }

    private void recreateModel() {
        items = null;
    }

    public TestFlagFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(TestFlagFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public TestFlagController() {
    }

    public TestFlag getCurrent() {
        return current;
    }

    public void setCurrent(TestFlag current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private TestFlagFacade getFacade() {
        return ejbFacade;
    }

    public List<TestFlag> getItems() {
        if (items == null) {
            String sql;
            //////// // System.out.println("getting items");
            Map m = new HashMap();
            m.put("ix", getInvestigation());
            if (investigation != null) {
                sql = "select i from TestFlag i where i.retired=false and i.item =:ix ";
                items = getFacade().findByJpql(sql, m);
            }
            //////// // System.out.println("item are " + items);
            if (items == null) {
                items = new ArrayList<>();
            }
        }
        return items;
    }

    public List<TestFlag> getIxItemFlags(InvestigationItem ix) {
        List<TestFlag> f;
        String sql;
        Map m = new HashMap();
        m.put("ix", ix);
        sql = "select i from TestFlag i where i.retired=false and i.investigationItemOfFlagType =:ix ";
        f = getFacade().findByJpql(sql, m);
        if (f == null) {
            f = new ArrayList<>();
        }
        return f;
    }

    public List<String> getIxItemFlagsString(InvestigationItem ix) {
        List<TestFlag> fs = getIxItemFlags(ix);
        Set<String> tfss = new HashSet();
        for (TestFlag f : fs) {
            tfss.add(f.getNormalMessage());
            tfss.add(f.getHighMessage());
            tfss.add(f.getLowMessage());

        }
        List<String> sortedList = new ArrayList(tfss);
        Collections.sort(sortedList);
        return sortedList;
    }

    /**
     *
     */
    @FacesConverter(forClass = TestFlag.class)
    public static class TestFlagControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            TestFlagController controller = (TestFlagController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "testFlagController");
            return controller.getEjbFacade().find(getKey(value));
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
            if (object instanceof TestFlag) {
                TestFlag o = (TestFlag) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + TestFlagController.class.getName());
            }
        }
    }
}
