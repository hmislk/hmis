/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.hr.PaysheetComponentType;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.PaysheetComponent;
import com.divudi.entity.hr.SalaryCycle;
import com.divudi.entity.hr.StaffSalary;
import com.divudi.entity.hr.StaffSalaryComponant;
import com.divudi.facade.PaysheetComponentFacade;
import com.divudi.facade.SalaryCycleFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.StaffPaysheetComponentFacade;
import com.divudi.facade.StaffSalaryComponantFacade;
import com.divudi.facade.StaffSalaryFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class SalaryCycleController implements Serializable {

    private SalaryCycle current;
    private List<SalaryCycle> salaryCycleList;
    @EJB
    private SalaryCycleFacade facade;
    @Inject
    private SessionController sessionController;
    List<SalaryCycle> salaryCycles;
    List<String> headersAdd;

    public List<String> getHeadersAdd() {
        if (headersAdd == null) {
            headersAdd = new ArrayList<>();
        }
        return headersAdd;
    }

    public void setHeadersAdd(List<String> headersAdd) {
        this.headersAdd = headersAdd;
    }

    public List<SalaryCycle> completeSalaryCycle(String qry) {

        String sql = "";
        HashMap hm = new HashMap();
        sql = "select c from SalaryCycle c "
                + " where c.retired=false "
                + " and upper(c.name) like :q ";
        sql += " order by c.id desc";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        salaryCycles = getFacade().findBySQL(sql, hm);

        return salaryCycles;
    }

    public void listAllSalaryCycles() {
        String sql;
        sql = "select c from SalaryCycle c "
                + " where c.retired=false "
                + " order by c.id desc";
        salaryCycles = getFacade().findBySQL(sql, 20);
    }

    @EJB
    StaffSalaryFacade staffSalaryFacade;

    @EJB
    PaysheetComponentFacade paysheetComponentFacade;

    @EJB
    StaffPaysheetComponentFacade staffPaysheetComponentFacade;

    public String hr_report_all_staff_salary() {
        listAllSalaryCycles();
        return "/hr/hr_report_all_staff_salary";
    }

    public List<SalaryCycle> getSalaryCycles() {
        if (salaryCycles == null) {
            listAllSalaryCycles();
        }
        return salaryCycles;
    }

    public void setSalaryCycles(List<SalaryCycle> salaryCycles) {
        this.salaryCycles = salaryCycles;
    }

    public void saveSelected() {
        if (current == null) {
            return;
        }

        current.processName();
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            UtilityController.addSuccessMessage("savedOldSuccessfully");
        } else {
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("savedNewSuccessfully");
        }

        //     recreateModel();
//        createSalaryCycleList();
        listAllSalaryCycles();
        current = null;
    }

    public SalaryCycleController() {
    }

    public void prepareAdd() {
        current = null;
    }

    public void delete() {

        if (current != null) {
            // removeAll();
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());

            getFacade().edit(current);

//            getFacade().remove(current);
//            getCurrentRoster().getSalaryCycleList().remove(getCurrent());
//            getRosterFacade().edit(getCurrentRoster());
            UtilityController.addSuccessMessage("DeleteSuccessfull");
        } else {
            UtilityController.addSuccessMessage("NothingToDelete");
        }
        //   recreateModel();

        current = null;

    }

    public SalaryCycle getCurrent() {
        if (current == null) {
            current = new SalaryCycle();
        }
        return current;
    }

    public void setCurrent(SalaryCycle current) {
        this.current = current;

    }

    public SalaryCycleFacade getFacade() {
        return facade;
    }

    public void setFacade(SalaryCycleFacade facade) {
        this.facade = facade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public List<SalaryCycle> getSalaryCycleList() {
        return salaryCycleList;
    }

    public void setSalaryCycleList(List<SalaryCycle> salaryCycleList) {
        this.salaryCycleList = salaryCycleList;
    }

    List<StaffAndSalarySalaryComponent> staffAnsAndSalarySalaryComponents;

    public List<StaffAndSalarySalaryComponent> getStaffAnsAndSalarySalaryComponents() {
        return staffAnsAndSalarySalaryComponents;
    }

    public void setStaffAnsAndSalarySalaryComponents(List<StaffAndSalarySalaryComponent> staffAnsAndSalarySalaryComponents) {
        this.staffAnsAndSalarySalaryComponents = staffAnsAndSalarySalaryComponents;
    }

    @EJB
    StaffFacade staffFacade;
    List<StaffSalary> staffSalary;

    public StaffSalaryFacade getStaffSalaryFacade() {
        return staffSalaryFacade;
    }

    public void setStaffSalaryFacade(StaffSalaryFacade staffSalaryFacade) {
        this.staffSalaryFacade = staffSalaryFacade;
    }

    public PaysheetComponentFacade getPaysheetComponentFacade() {
        return paysheetComponentFacade;
    }

    public void setPaysheetComponentFacade(PaysheetComponentFacade paysheetComponentFacade) {
        this.paysheetComponentFacade = paysheetComponentFacade;
    }

    public StaffPaysheetComponentFacade getStaffPaysheetComponentFacade() {
        return staffPaysheetComponentFacade;
    }

    public void setStaffPaysheetComponentFacade(StaffPaysheetComponentFacade staffPaysheetComponentFacade) {
        this.staffPaysheetComponentFacade = staffPaysheetComponentFacade;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public List<StaffSalary> getStaffSalary() {
        return staffSalary;
    }

    public void setStaffSalary(List<StaffSalary> staffSalary) {
        this.staffSalary = staffSalary;
    }

    public StaffSalaryComponantFacade getStaffSalaryComponantFacade() {
        return staffSalaryComponantFacade;
    }

    public void setStaffSalaryComponantFacade(StaffSalaryComponantFacade staffSalaryComponantFacade) {
        this.staffSalaryComponantFacade = staffSalaryComponantFacade;
    }

//    public void fillStaffAndSalaryComponents2() {
//
//        List<PaysheetComponent> paysheetComponents;
//        List<Staff> staffes;
//        String jpql;
//        Map m;
//
//        headersAdd = new ArrayList<>();
//
//        m = new HashMap();
//        jpql = "select distinct(spc.staffPaysheetComponent.paysheetComponent) "
//                + " from StaffSalaryComponant spc"
//                + " where spc.salaryCycle=:sc"
//                + " and spc.retired=false"
//                + " order by spc.staffPaysheetComponent.paysheetComponent.orderNo";
//        m.put("sc", current);
//        paysheetComponents = paysheetComponentFacade.findBySQL(jpql, m);
//
//        headersAdd.add("Staff Name");
//        for (PaysheetComponent paysheetComponent : paysheetComponents) {
//            headersAdd.add(paysheetComponent.getName());
//        }
//
//        m = new HashMap();
//        jpql = "select distinct(spc.staffSalary.staff)"
//                + " from StaffSalaryComponant spc "
//                + " where spc.salaryCycle=:sc "
//                + " and spc.retired=false";
//        m.put("sc", current);
//        staffes = staffFacade.findBySQL(jpql, m);
//
//        staffAnsAndSalarySalaryComponents = new ArrayList<>();
//
//        for (Staff s : staffes) {
//            StaffAndSalarySalaryComponent sc = new StaffAndSalarySalaryComponent();
//            sc.setStaff(s);          
//            for (PaysheetComponent psc : paysheetComponents) {
//                jpql = "select spc from StaffSalaryComponant spc "
//                        + " where spc.staffSalary.staff=:st"
//                        + " and spc.retired=false"
//                        + " and spc.staffSalary.retired=false"
//                        + " and spc.staffPaysheetComponent.paysheetComponent=:pc "
//                        + " and spc.salaryCycle=:sc ";
//                m = new HashMap();
//                m.put("st", s);
//                m.put("pc", psc);
//                m.put("sc", current);
//                List<StaffSalaryComponant> c = staffSalaryComponantFacade.findBySQL(jpql, m);
//                sc.getStaffSalaryComponant().addAll(c);
//            }
//            staffAnsAndSalarySalaryComponents.add(sc);
//        }
//    }
    private List<PaysheetComponent> fetchPaysheetComponents(PaysheetComponentType paysheetComponentType) {
        HashMap m = new HashMap();
        String jpql = "select distinct(spc.staffPaysheetComponent.paysheetComponent) "
                + " from StaffSalaryComponant spc"
                + " where spc.salaryCycle=:sc"
                + " and spc.retired=false "
                + " and spc.staffPaysheetComponent.paysheetComponent.componentType in :tp"
                + " order by spc.staffPaysheetComponent.paysheetComponent.orderNo";
        m.put("sc", current);
        m.put("tp", Arrays.asList(paysheetComponentType.children()));
        return paysheetComponentFacade.findBySQL(jpql, m);
    }

    List<String> headersSub;

    public List<String> getHeadersSub() {
        return headersSub;
    }

    public void setHeadersSub(List<String> headersSub) {
        this.headersSub = headersSub;
    }

    public void fillStaffAndSalaryComponents() {

        List<PaysheetComponent> paysheetComponentsAddition;
        List<PaysheetComponent> paysheetComponentsSubstraction;
        List<Staff> staffes;
        String jpql;
        Map m;

        headersAdd = new ArrayList<>();
        paysheetComponentsAddition = fetchPaysheetComponents(PaysheetComponentType.addition);
        for (PaysheetComponent paysheetComponent : paysheetComponentsAddition) {
            headersAdd.add(paysheetComponent.getName());
        }

        headersSub = new ArrayList<>();
        paysheetComponentsSubstraction = fetchPaysheetComponents(PaysheetComponentType.subtraction);
        for (PaysheetComponent paysheetComponent : paysheetComponentsSubstraction) {
            headersSub.add(paysheetComponent.getName());
        }

        m = new HashMap();
        jpql = "select distinct(spc.staffSalary.staff)"
                + " from StaffSalaryComponant spc "
                + " where spc.salaryCycle=:sc "
                + " and spc.retired=false";
        m.put("sc", current);
        staffes = staffFacade.findBySQL(jpql, m);

        staffAnsAndSalarySalaryComponents = new ArrayList<>();

        for (Staff s : staffes) {
            StaffAndSalarySalaryComponent sc = new StaffAndSalarySalaryComponent();
            sc.setStaff(s);
            for (PaysheetComponent psc : paysheetComponentsAddition) {
                List<StaffSalaryComponant> c = fetchSalaryComponents(s, psc);
                if (c != null) {
                    sc.getStaffSalaryComponantsAddition().addAll(c);
                }
            }
            for (PaysheetComponent psc : paysheetComponentsSubstraction) {
                List<StaffSalaryComponant> c = fetchSalaryComponents(s, psc);
                if (c != null) {
                    sc.getStaffSalaryComponantsSubstraction().addAll(c);
                }
            }
            sc.calValue();
            staffAnsAndSalarySalaryComponents.add(sc);
        }
    }

    private List<StaffSalaryComponant> fetchSalaryComponents(Staff s, PaysheetComponent psc) {
        String jpql = "select spc from StaffSalaryComponant spc "
                + " where spc.staffSalary.staff=:st"
                + " and spc.retired=false"
                + " and spc.staffSalary.retired=false"
                + " and spc.staffPaysheetComponent.paysheetComponent=:pc "
                + " and spc.salaryCycle=:sc ";
        HashMap m = new HashMap();
        m.put("st", s);
        m.put("pc", psc);
        m.put("sc", current);
        return staffSalaryComponantFacade.findBySQL(jpql, m);

    }

    public void fillStaffSalary() {

        String jpql = "select spc from StaffSalary spc "
                + " where spc.retired=false"
                + " and spc.salaryCycle=:sc"
                + " order by spc.staff.codeInterger ";
        HashMap m = new HashMap();
        m.put("sc", current);
        staffSalary = staffSalaryFacade.findBySQL(jpql, m);

    }

    @EJB
    StaffSalaryComponantFacade staffSalaryComponantFacade;

    public class StaffAndSalarySalaryComponent {

        Staff staff;
        double valueAdding;
        double epf;
        double etf;
        double valueSubstarction;
        List<StaffSalaryComponant> staffSalaryComponantsAddition;
        List<StaffSalaryComponant> staffSalaryComponantsSubstraction;

        public void calValue() {
            valueAdding = 0;
            valueSubstarction = 0;
            epf = 0;
            etf = 0;

            if (staffSalaryComponantsAddition != null) {
                for (StaffSalaryComponant componant : staffSalaryComponantsAddition) {
                    valueAdding += componant.getComponantValue();
                    epf += componant.getEpfValue();
                    etf += componant.getEtfValue();
                }
            }

            if (staffSalaryComponantsSubstraction != null) {
                for (StaffSalaryComponant componant : staffSalaryComponantsSubstraction) {
                    valueSubstarction += componant.getComponantValue();
                    epf += componant.getEpfValue();
                    etf += componant.getEtfValue();
                }
            }

        }

        public double getEpf() {
            return epf;
        }

        public void setEpf(double epf) {
            this.epf = epf;
        }

        public double getEtf() {
            return etf;
        }

        public void setEtf(double etf) {
            this.etf = etf;
        }

        public double getValueAdding() {
            return valueAdding;
        }

        public void setValueAdding(double valueAdding) {
            this.valueAdding = valueAdding;
        }

        public double getValueSubstarction() {
            return valueSubstarction;
        }

        public void setValueSubstarction(double valueSubstarction) {
            this.valueSubstarction = valueSubstarction;
        }

        public Staff getStaff() {
            return staff;
        }

        public void setStaff(Staff staff) {
            this.staff = staff;
        }

        public List<StaffSalaryComponant> getStaffSalaryComponantsAddition() {
            if (staffSalaryComponantsAddition == null) {
                staffSalaryComponantsAddition = new ArrayList<>();
            }
            return staffSalaryComponantsAddition;
        }

        public void setStaffSalaryComponantsAddition(List<StaffSalaryComponant> staffSalaryComponantsAddition) {
            this.staffSalaryComponantsAddition = staffSalaryComponantsAddition;
        }

        public List<StaffSalaryComponant> getStaffSalaryComponantsSubstraction() {
            if (staffSalaryComponantsSubstraction == null) {
                staffSalaryComponantsSubstraction = new ArrayList<>();
            }
            return staffSalaryComponantsSubstraction;
        }

        public void setStaffSalaryComponantsSubstraction(List<StaffSalaryComponant> staffSalaryComponantsSubstraction) {
            this.staffSalaryComponantsSubstraction = staffSalaryComponantsSubstraction;
        }

    }

    @FacesConverter(forClass = SalaryCycle.class)
    public static class SalaryCycleConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SalaryCycleController controller = (SalaryCycleController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "salaryCycleController");
            return controller.getFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            try {
                key = Long.valueOf(value);
            } catch (NumberFormatException exception) {
                key = 0l;
            }
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
            if (object instanceof SalaryCycle) {
                SalaryCycle o = (SalaryCycle) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + SalaryCycleController.class.getName());
            }
        }
    }

    @FacesConverter("salaryCycleConverter")
    public static class SalaryCycleControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SalaryCycleController controller = (SalaryCycleController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "salaryCycleController");
            return controller.getFacade().find(getKey(value));
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
            if (object instanceof SalaryCycle) {
                SalaryCycle o = (SalaryCycle) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + SalaryCycleController.class.getName());
            }
        }
    }

}
