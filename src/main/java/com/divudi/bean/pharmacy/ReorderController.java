package com.divudi.bean.pharmacy;

import com.divudi.bean.common.UtilityController;
import com.divudi.data.BillType;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Person;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.PharmaceuticalItem;
import com.divudi.entity.pharmacy.Reorder;
import com.divudi.facade.ReorderFacade;
import com.divudi.facade.util.JsfUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.primefaces.event.RowEditEvent;

@Named
@SessionScoped
public class ReorderController implements Serializable {

    private Reorder current;
    private List<Reorder> items = null;
    @EJB
    ReorderFacade ejbFacade;

    Department department;
    Institution institution;
    Person person;

    @Inject
    AmpController ampController;

    List<Reorder> departmentReorders;
     List<Item> selectedItems;

    public void onEdit(RowEditEvent event) {

        Reorder tmp = (Reorder) event.getObject();
        getEjbFacade().edit(tmp);
        UtilityController.addSuccessMessage("Reorder Level Updted");
    }

    public void createDepartmentReorders() {
        System.out.println("create department reorders");
        items = new ArrayList<>();
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        if (selectedItems == null || selectedItems.isEmpty()) {
            JsfUtil.addErrorMessage("Please select one or more items");
            return;
        }
        System.out.println("selectedItems = " + selectedItems);
        for (Item a : selectedItems) {
            Reorder r;
            Map m = new HashMap();
            m.put("d", department);
            m.put("i", a);
            System.out.println("m = " + m);
            String sql = "Select r from Reorder r where r.item=:i and r.department=:d";
            System.out.println("sql = " + sql);
            r = getEjbFacade().findFirstBySQL(sql, m);
            System.out.println("r = " + r);
            if (r == null) {

                r = new Reorder();
               
                r.setDepartment(department);
                

                r.setInstitution(institution);
                

                r.setItem(a);
                

                r.setMonthsConsideredForShortTermAnalysis(12);
                

                r.setYearsConsideredForLognTermAnalysis(5);
                

                r.setPerson(null);
                

                r.setServiceLevel(0.0);
                

                r.setSupplier(null);
                

                int cd = calculateOrderingCycleDurationInDays(r);
                System.out.println("cd = " + cd);
                r.setPurchaseCycleDurationInDays(cd);
                

                double dpdiu = calculateDailyDemandInUnits(r);
                System.out.println("dpdiu = " + dpdiu);
                r.setDemandInUnitsPerDay(dpdiu);
                

                int lt = calculateLeadTime(r);
                System.out.println("lt = " + lt);
                r.setLeadTimeInDays(lt);
                

                double roq = calculateRoq(r);
                System.out.println("roq = " + roq);
                r.setRoq(roq);
                

                double bufferStocks = dpdiu * 7;
                System.out.println("bufferStocks = " + bufferStocks);
                r.setBufferStocks(bufferStocks);
                
                
                
                 getEjbFacade().create(r);


            }
            items.add(r);
        }
    }
    
    public int calculateOrderingCycleDurationInDays(Reorder reorder) {
        System.out.println("calculating ordering cycle duration");
        String jpql;
        Map m = new HashMap();

        DateTime dt = new DateTime();
        DateTime tfd = dt.minusMonths(reorder.getMonthsConsideredForShortTermAnalysis());

        Date fd = tfd.toDate();
        Date td = new Date();

        jpql = "Select max(bi.bill.createdAt),min(bi.bill.createdAt),count(bi) "
                + " from BillItem bi "
                + " where bi.bill.billType in :bts"
                + " and bi.item=:amp "
                + " and bi.bill.createdAt between :fd and :td "
                + " group by bi.bill";

        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.PharmacyPurchaseBill);
        bts.add(BillType.PharmacyGrnBill);
        
        m.put("bts", bts);
        m.put("amp", reorder.getItem());
        m.put("fd", fd);
        m.put("td", td);

        System.out.println("jpql = " + jpql);
        System.out.println("m = " + m);

        Object[] obj = ejbFacade.findSingleAggregate(jpql, m);

        System.out.println("obj = " + obj);

        if (obj == null) {
            return 14;
        }
        Date minDate;
        Date maxDate;
        int count;

        try {
            System.out.println(" obj[0] = " + obj[0]);
            minDate = (Date) obj[0];
        } catch (Exception e) {
            minDate = new Date();
        }
        try {
            System.out.println(" obj[1] = " + obj[1]);
            maxDate = (Date) obj[1];
        } catch (Exception e) {
            maxDate = new Date();
        }
        try {
            System.out.println(" obj[2] = " + obj[2]);
            count = (int) obj[2];
        } catch (Exception e) {
            count = 1;
        }

        if (count == 0) {
            count = 1;
        }
        DateTime mind = new DateTime(minDate);
        DateTime maxd = new DateTime(maxDate);

        Days daysDiff = Days.daysBetween(maxd, mind);

        int ds = daysDiff.getDays();
        System.out.println("ds = " + ds);
        System.out.println("count = " + count);
        return (int) (ds / count);

    }
    
    public double calculateDailyDemandInUnits(Reorder reorder) {
        System.out.println("Calculate daily demand in Units - reorder = " + reorder);
        String jpql;
        Map m = new HashMap();
        DateTime dt = new DateTime();
        DateTime tfd = dt.minusMonths(reorder.getMonthsConsideredForShortTermAnalysis());
        Date fd = tfd.toDate();
        Date td = new Date();

     
        List<BillType> bts = new ArrayList<>();

        jpql = "Select max(bi.bill.createdAt), min(bi.bill.createdAt), sum(bi.qty) "
                + " from BillItem bi "
                + " where bi.bill.billType in :bts "
                + " and bi.item=:amp "
                + " and bi.bill.createdAt between :fd and :td ";

        bts.add(BillType.PharmacyAdjustment);
        bts.add(BillType.PharmacyPre);
        bts.add(BillType.PharmacyBhtPre);
        bts.add(BillType.PharmacyIssue);
        bts.add(BillType.PharmacyTransferIssue);
        m.put("bts", bts);
        m.put("amp", reorder.getItem());
        m.put("fd", fd);
        m.put("td", td);
        System.out.println("m = " + m);
        System.out.println("jpql = " + jpql);
        Object[] obj = ejbFacade.findSingleAggregate(jpql, m);
        System.out.println("obj = " + obj);
        if (obj == null) {
            return 14;
        }
        Date minDate;
        Date maxDate;
        Double totalQty;

        try {
            System.out.println(" obj[0] = " + obj[0]);
            minDate = (Date) obj[0];
        } catch (Exception e) {
            minDate = new Date();
        }
        try {
            System.out.println(" obj[1] = " + obj[1]);
            maxDate = (Date) obj[1];
        } catch (Exception e) {
            maxDate = new Date();
        }
        try {
            System.out.println(" obj[2] = " + obj[2]);
            totalQty = Math.abs((Double) obj[2]);
        } catch (Exception e) {
            totalQty = 0.0;
        }

        DateTime mind = new DateTime(minDate);
        DateTime maxd = new DateTime(maxDate);
        Days daysDiff = Days.daysBetween(mind, maxd);

        int ds = daysDiff.getDays();
        System.out.println("ds = " + ds);
        System.out.println("totalQty = " + totalQty);
       
        double dailyDemand = 0;
        if(ds==0){
            ds=1;
        }
        try {
            dailyDemand = totalQty / ds;
        } catch (Exception e) {
            dailyDemand = 1.0;
        }
        if (dailyDemand == 0.0) {
            dailyDemand = 1.0;
        }
        System.out.println("dailyDemand = " + dailyDemand);
        return dailyDemand;
    }
    
    public int calculateLeadTime(Reorder reorder) {
        String jpql;
        Map m = new HashMap();
        DateTime dt = new DateTime();
        DateTime tfd = dt.minusMonths(reorder.getMonthsConsideredForShortTermAnalysis());
        Date fd = tfd.toDate();
        Date td = new Date();

        BillItem bi = new BillItem();
        bi.getReferanceBillItem();

        jpql = "Select b, rb "
                + " from BillItem bi "
                + " join bi.bill b "
                + " join bi.referanceBillItem rbi "
                + " join rbi.bill rb "
                + " where b.billType in :bts "
                + " and rb.billType in :rbts "
                + " and bi.item=:amp "
                + " and b.createdAt between :fd and :td "
                + " ";

        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.PharmacyOrderApprove);

        List<BillType> rbts = new ArrayList<>();
        bts.add(BillType.PharmacyGrnBill);

        m.put("bts", bts);
        m.put("rbts", rbts);
        m.put("amp", reorder.getItem());
        m.put("fd", fd);
        m.put("td", td);
        List<Object[]> obj = ejbFacade.findAggregates(jpql, m);

        if (obj == null) {
            return 7;
        }

        int count = 0;
        long differenceInMs = 0l;
        for (Object[] objc : obj) {
            Bill b = (Bill) objc[0];
            System.out.println("b = " + b);
            Bill rf = (Bill) objc[1];
            System.out.println("rf = " + rf);
            count++;
            System.out.println("count = " + count);
            differenceInMs = differenceInMs + (rf.getCreatedAt().getTime() - b.getCreatedAt().getTime());
            System.out.println("differenceInMs = " + differenceInMs);
        }

        int avgLeadTimeInDays;

        try {
            Long avgLeadTimeInMs = differenceInMs / count;
            avgLeadTimeInDays = (int) (avgLeadTimeInMs / (1000 * 60 * 60 * 24));
        } catch (Exception e) {
            avgLeadTimeInDays = 7;
        }
        return avgLeadTimeInDays;
    }
    
    public double calculateRoq(Reorder reorder) {
        int numberOfDaysToOrder;
        if (reorder.getPurchaseCycleDurationInDays() < reorder.getLeadTimeInDays()) {
            numberOfDaysToOrder = reorder.getLeadTimeInDays();
        } else {
            numberOfDaysToOrder = reorder.getPurchaseCycleDurationInDays();
        }
        return numberOfDaysToOrder * reorder.getDemandInUnitsPerDay();
    }
    
    public void fillDepartmentReorders() {
        if (department == null) {
            return;
        }
        items = new ArrayList<>();
        List<Amp> amps = getAmpController().getItems();

        for (Amp a : amps) {
            Reorder r;
            Map m = new HashMap();
            m.put("d", department);
            m.put("i", a);
            String sql = "Select r from Reorder r where r.item=:i and r.department=:d";
            r = getEjbFacade().findFirstBySQL(sql, m);
            if (r == null) {
                r = new Reorder();
                r.setDepartment(department);
                r.setItem(a);
                getEjbFacade().create(r);

            }
            items.add(r);
        }
    }

    public AmpController getAmpController() {
        return ampController;
    }

    public void setAmpController(AmpController ampController) {
        this.ampController = ampController;
    }

    public List<Reorder> getDepartmentReorders() {
        return departmentReorders;
    }

    public void setDepartmentReorders(List<Reorder> departmentReorders) {
        this.departmentReorders = departmentReorders;
    }

    public Reorder getCurrent() {
        return current;
    }

    public void setCurrent(Reorder current) {
        this.current = current;
    }

    public List<Reorder> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public void setItems(List<Reorder> items) {
        this.items = items;
    }

    public ReorderFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ReorderFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public ReorderController() {
    }

    public Reorder getReorder(java.lang.Long id) {
        return ejbFacade.find(id);
    }

    public List<Item> getSelectedItems() {
        return selectedItems;
    }

    public void setSelectedItems(List<Item> selectedItems) {
        this.selectedItems = selectedItems;
    }
    
    

    @FacesConverter(forClass = Reorder.class)
    public static class ReorderControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ReorderController controller = (ReorderController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "reorderController");
            return controller.getReorder(getKey(value));
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
            if (object instanceof Reorder) {
                Reorder o = (Reorder) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + Reorder.class.getName());
            }
        }

    }

}
