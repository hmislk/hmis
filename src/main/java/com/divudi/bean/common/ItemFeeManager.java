/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.data.FeeType;
import com.divudi.entity.Department;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Staff;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author pasan
 */
@Named
@SessionScoped
public class ItemFeeManager implements Serializable {

    /**
     * Creates a new instance of ItemFeeManager
     */
    public ItemFeeManager() {
    }

    Item item;
    ItemFee itemFee;
    ItemFee removingFee;

    List<ItemFee> itemFees;

    @EJB
    ItemFeeFacade itemFeeFacade;
    @EJB
    ItemFacade itemFacade;
    @EJB
    DepartmentFacade departmentFacade;
    @EJB
    StaffFacade staffFacade;

    @Inject
    SessionController sessionController;
    @Inject
    ItemController itemController;

    List<Department> departments;
    List<Staff> staffs;
    private List<Item> selectedList;

    public String navigateItemFeeList() {
        return "/admin/pricing/item_fee_list?faces-redirect=true";
    }

    public String navigateToCollectingCentreItemFeeList() {
        return "/admin/pricing/item_fee_list_collecting_centre?faces-redirect=true";
    }

    public String navigateToCorrectItemFees() {
        return "/dataAdmin/bulk_update_itemsFees?faces-redirect=true";
    }

    public String navigateToUploadItemFees() {
        return "/admin/pricing/item_fee_upload?faces-redirect=true";
    }

    public String navigateToUploadCollectingCentreItemFees() {
        return "/admin/pricing/item_fee_upload_collecting_centre?faces-redirect=true";
    }

    public String navigateItemViseFeeList() {
        return "/admin/pricing/manage_item_fees_bulk?faces-redirect=true";
    }

    public void createItemFessForSelectedItems() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        if (itemFee == null) {
            JsfUtil.addErrorMessage("No Item Fee");
            return;
        }
        ItemFee baseItemFee = itemFee;
        for (Item i : selectedList) {
            ItemFee itf = new ItemFee();
            itf.setName(baseItemFee.getName());
            itf.setItem(i);
            itf.setInstitution(baseItemFee.getInstitution());
            itf.setDepartment(baseItemFee.getDepartment());
            itf.setFeeType(baseItemFee.getFeeType());
            itf.setFee(baseItemFee.getFee());
            itf.setFfee(baseItemFee.getFfee());
            addNewFeeForItem(i, itf);
        }
        itemFee = baseItemFee;
    }

    public void fixIssueToReferralFees() {
        List<ItemFee> ifs = itemFeeFacade.findAll();
        for (ItemFee f : ifs) {
            if (f.getFeeType() == FeeType.Issue) {
                f.setFeeType(FeeType.Referral);
                f.setInstitution(f.getItem().getInstitution());
                f.setDepartment(f.getItem().getDepartment());
                itemFeeFacade.edit(f);
            }
        }
    }

    public ItemFee getRemovingFee() {
        return removingFee;
    }

    public void setRemovingFee(ItemFee removingFee) {
        this.removingFee = removingFee;
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public List<Staff> getStaffs() {
        return staffs;
    }

    public void setStaffs(List<Staff> staffs) {
        this.staffs = staffs;
    }

    public void removeFee() {
        if (removingFee == null) {
            JsfUtil.addErrorMessage("Select a fee");
            return;
        }
        removingFee.setRetired(true);
        removingFee.setRetiredAt(new Date());
        removingFee.setRetirer(sessionController.getLoggedUser());
        itemFeeFacade.edit(removingFee);
        itemFees = null;
        fillFees();
        updateTotal();
        JsfUtil.addSuccessMessage("Removed");
    }

    public void fillDepartments() {
        ////// // System.out.println("fill dept");
        String jpql;
        Map m = new HashMap();
        m.put("ins", getItemFee().getInstitution());
        jpql = "select d from Department d where d.retired=false and d.institution=:ins order by d.name";
        ////// // System.out.println("m = " + m);
        ////// // System.out.println("jpql = " + jpql);
        departments = departmentFacade.findByJpql(jpql, m);
    }

    public void fillStaff() {
        ////// // System.out.println("fill staff");
        String jpql;
        Map m = new HashMap();
        m.put("ins", getItemFee().getSpeciality());
        jpql = "select d from Staff d where d.retired=false and d.speciality=:ins order by d.person.name";
        ////// // System.out.println("m = " + m);
        ////// // System.out.println("jpql = " + jpql);
        staffs = staffFacade.findByJpql(jpql, m);
    }

    public List<Department> compelteDepartments(String qry) {
        String jpql;
        if (qry == null) {
            return new ArrayList<>();
        }
        Map m = new HashMap();
        m.put("ins", getItemFee().getInstitution());
        m.put("name", "%" + qry.toUpperCase() + "%");
        jpql = "select d from Department d where d.retired=false and d.institution=:ins and d.name like :name order by d.name";
        return departmentFacade.findByJpql(jpql, m);
    }

    public String navigateToItemFees() {
//        itemController.fillItemsForInward();
        return "/admin/pricing/manage_item_fees?faces-redirect=true";
    }

    public String navigateToItemFeesMultiple() {
        return "/admin/pricing/manage_item_fees_multiple?faces-redirect=true";
    }

    public void clearItemFees() {
        item = null;
        itemFees = null;
        removingFee = null;
    }

    public void clearSelectedItems() {
        selectedList = new ArrayList<>();
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ItemFee getItemFee() {
        if (itemFee == null) {
            itemFee = new ItemFee();
        }
        return itemFee;
    }

    public void setItemFee(ItemFee itemFee) {
        this.itemFee = itemFee;
    }

    public List<ItemFee> getItemFees() {
        return itemFees;
    }

    public void setItemFees(List<ItemFee> itemFees) {
        this.itemFees = itemFees;
    }

    public void fillFees() {
        itemFees = fillFees(item);
    }

    public String toManageItemFees() {
        if (item == null) {
            JsfUtil.addErrorMessage("Nothing Selected to Edit");
            return "";
        }
        fillFees();
        return "/common/manage_item_fees";
    }

    public List<ItemFee> fillFees(Item i) {
        String jpql;
        Map m = new HashMap();
        jpql = "select f from ItemFee f where f.retired=false and f.item=:i";
        m.put("i", i);
        return itemFeeFacade.findByJpql(jpql, m);
    }

    public void addNewFee() {
        if (item == null) {
            JsfUtil.addErrorMessage("Select Item ?");
            return;
        }
        if (itemFee == null) {
            JsfUtil.addErrorMessage("Select Item Fee");
            return;
        }
        if (itemFee.getName() == null || itemFee.getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please Fill Fee Name");
            return;
        }

        if (itemFee.getFeeType() == null) {
            JsfUtil.addErrorMessage("Please Fill Fee Type");
            return;
        }

        if (itemFee.getFeeType() == FeeType.OtherInstitution || itemFee.getFeeType() == FeeType.OwnInstitution || itemFee.getFeeType() == FeeType.Referral) {
            if (itemFee.getDepartment() == null) {
                JsfUtil.addErrorMessage("Please Select Department");
                return;
            }
        }

//        if (itemFee.getFeeType() == FeeType.Staff) {
//            if (itemFee.getStaff() == null || itemFee.getStaff().getPerson().getName().trim().equals("")) {
//                JsfUtil.addErrorMessage("Please Select Staff");
//                return;
//            }
//        }
        if (itemFee.getFee() == 0.00) {
            JsfUtil.addErrorMessage("Please Enter Local Fee Value");
            return;
        }

        if (itemFee.getFfee() == 0.00) {
            JsfUtil.addErrorMessage("Please Enter Foreign Fee Value");
            return;
        }
        getItemFee().setCreatedAt(new Date());
        getItemFee().setCreater(sessionController.getLoggedUser());
        itemFeeFacade.create(itemFee);

        getItemFee().setItem(item);
        itemFeeFacade.edit(itemFee);

        itemFee = new ItemFee();
        itemFees = null;
        fillFees();
        updateTotal();
        JsfUtil.addSuccessMessage("New Fee Added");
    }

    public void addNewFeeForItem(Item inputItem, ItemFee inputFee) {
        if (inputItem == null) {
            JsfUtil.addErrorMessage("Select Item ?");
            return;
        }
        inputFee.setCreatedAt(new Date());
        inputFee.setCreater(sessionController.getLoggedUser());
        itemFeeFacade.create(inputFee);

        inputFee.setItem(inputItem);
        itemFeeFacade.edit(inputFee);

        List<ItemFee> inputFees = fillFees(inputItem);
        updateTotal(inputItem, inputFees);

    }

    public void saveItemFee(ItemFee inputFee) {
        if (inputFee == null) {
            return;
        }
        if (inputFee.getId() == null) {
            inputFee.setCreatedAt(new Date());
            inputFee.setCreater(sessionController.getLoggedUser());
            itemFeeFacade.create(inputFee);
        } else {
            inputFee.setEditedAt(new Date());
            inputFee.setEditer(sessionController.getLoggedUser());
            itemFeeFacade.edit(inputFee);
        }

        List<ItemFee> inputFees = fillFees(inputFee.getItem());
        updateTotal(inputFee.getItem(), inputFees);

    }

    public void updateFee(ItemFee f) {
        itemFeeFacade.edit(f);
        updateTotal();
    }

    public void updateFee() {
        if (item == null) {
            return;
        }
        double t = 0.0;
        double tf = 0.0;
        for (ItemFee f : itemFees) {
            t += f.getFee();
            tf += f.getFfee();
            itemFeeFacade.edit(f);
        }
        getItem().setTotal(t);
        getItem().setTotalForForeigner(tf);
        itemFacade.edit(getItem());
    }

    public void updateTotal(Item inputItem, List<ItemFee> inputItemFees) {
        if (inputItem == null) {
            return;
        }
        double t = 0.0;
        double tf = 0.0;
        for (ItemFee f : inputItemFees) {
            t += f.getFee();
            tf += f.getFfee();
        }
        inputItem.setTotal(t);
        inputItem.setTotalForForeigner(tf);
        itemFacade.edit(inputItem);
    }

    public void updateTotal() {
        if (item == null) {
            return;
        }
        double t = 0.0;
        double tf = 0.0;
        for (ItemFee f : itemFees) {
            t += f.getFee();
            tf += f.getFfee();
        }
        getItem().setTotal(t);
        getItem().setTotalForForeigner(tf);
        itemFacade.edit(item);
    }

    public List<Item> getSelectedList() {
        if (selectedList == null) {
            selectedList = new ArrayList<>();
        }
        return selectedList;
    }

    public void setSelectedList(List<Item> selectedList) {
        this.selectedList = selectedList;
    }
}
