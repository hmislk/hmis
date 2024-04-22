/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.SessionController;

import com.divudi.data.CssFontStyle;
import com.divudi.data.CssTextAlign;
import com.divudi.data.CssTextDecoration;
import com.divudi.data.CssVerticalAlign;
import com.divudi.data.InvestigationItemType;
import com.divudi.data.InvestigationItemValueType;
import com.divudi.data.ItemType;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.entity.lab.InvestigationItemValue;
import com.divudi.entity.lab.InvestigationTube;
import com.divudi.entity.lab.Machine;
import com.divudi.entity.lab.ReportItem;
import com.divudi.entity.lab.Sample;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.InvestigationItemFacade;
import com.divudi.facade.InvestigationItemValueFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ReportItemFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class InvestigationItemController implements Serializable {

    /**
     * EJBs
     */
    @EJB
    private InvestigationItemFacade ejbFacade;
    @EJB
    InvestigationItemValueFacade iivFacade;
    @EJB
    private ReportItemFacade riFacade;
    @EJB
    InvestigationFacade ixFacade;
    @EJB
    private ItemFacade ItemFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    /**
     * Controllers
     */
    @Inject
    SessionController sessionController;
    @Inject
    InvestigationController investigationController;
    @Inject
    private ItemController itemController;
    @Inject
    InvestigationTubeController investigationTubeController;
    @Inject
    SampleController sampleController;
    @Inject
    MachineController machineController;

    /**
     * Properties
     */
    List<InvestigationItem> selectedItems;
    List<InvestigationItem> selectedItemsToChange;
    private InvestigationItem current;
    private Investigation currentInvestigation;
    private List<InvestigationItem> items = null;
    private List<InvestigationItem> importantItems = null;

    String selectText = "";
    InvestigationItemValue removingItem;
    InvestigationItemValue addingItem;
    String addingString;

    private String jsonString;

    Investigation copyingFromInvestigation;
    Investigation copyingToInvestigation;
    String ixXml;

    private static final long serialVersionUID = 1L;

    EditMode editMode = EditMode.View_Mode;

    private String input;
    private int keyCode;
    private int previousKeyCode;
    private int specialCode;
    String fontFamily;
    double fontSize;

    Double movePercent;
    Double fixHeight;
    Double fixWidth;

    private Institution institution;
    private Department department;

    private UploadedFile file;

    public Double getMovePercent() {
        if (movePercent == null) {
            movePercent = 5.0;
        }
        return movePercent;
    }

    public void nextInvestigation() {
        Investigation thisOne = getCurrentInvestigation();
        for (int i = 0; i < investigationController.getItems().size(); i++) {
            Investigation ix = investigationController.getItems().get(i);
            if (thisOne.equals(ix)) {
                if ((i + 1) < investigationController.getItems().size()) {
                    setCurrentInvestigation(investigationController.getItems().get(i + 1));
                }
            }
        }
    }

    public List<InvestigationItem> completeIxValues(String qry) {
        List<InvestigationItem> iivs;
        String sql;
        Map m = new HashMap();
        sql = "select i from InvestigationItem i "
                + " where i.retired=false "
                + " and i.ixItemType =:vt "
                + " and (i.name) like :qry";
        m.put("vt", InvestigationItemType.Value);
        m.put("qry", "%" + qry.toUpperCase() + "%");
        iivs = getFacade().findByJpql(sql, m);
        if (iivs == null) {
            iivs = new ArrayList<>();
        }
        return iivs;
    }

    public void previousInvestigation() {
        Investigation thisOne = getCurrentInvestigation();
        for (int i = 0; i < investigationController.getItems().size(); i++) {
            Investigation ix = investigationController.getItems().get(i);
            if (thisOne.equals(ix)) {
                if (i > 0) {
                    setCurrentInvestigation(investigationController.getItems().get(i - 1));
                }
            }
        }
    }

    public void makeAllIxItemsToMachIxDetails() {
        if (currentInvestigation == null) {
            JsfUtil.addErrorMessage("Select Ix");
            return;
        }
        for (InvestigationItem tixi : getImportantItems()) {
            if (tixi.getItem() instanceof Investigation) {

                Investigation tix = investigationController.getInvestigationByIdAndSetAsCurrent(tixi.getItem().getId());
                if (tix.equals(currentInvestigation)) {
                    //System.out.println("Is current ix");
                    tixi.setTube(tix.getInvestigationTube());
                    //System.out.println("tix.getInvestigationTube() = " + tix.getInvestigationTube());
                    //System.out.println("tixi.getTube() = " + tixi.getTube());
                    tixi.setSample(tix.getSample());
                    //System.out.println("tix.getSample() = " + tix.getSample());
                    tixi.setMachine(tix.getMachine());
                    Item sc = itemController.getFirstInvestigationSampleComponents(tix);
                    tixi.setSampleComponent(sc);
                    getFacade().edit(tixi);
                }
            }
        }
    }

    public void makeAllInvestigationsAndItemsToMachIxDetails() {
        InvestigationTube tixt = investigationTubeController.getAnyTube();
        Sample ts = sampleController.getAnySample();
        Machine tm = machineController.getAnyMachine();
        for (Investigation tix : investigationController.getAllIxs()) {
            boolean needToSaveIx = false;
            if (tix.getMachine() == null) {
                needToSaveIx = true;
                tix.setMachine(tm);
            }
            if (tix.getInvestigationTube() == null) {
                needToSaveIx = true;
                tix.setInvestigationTube(tixt);
            }
            if (tix.getSample() == null) {
                needToSaveIx = true;
                tix.setSample(ts);
            }
            if (needToSaveIx) {
                investigationController.saveSelected(tix);
            }
            for (InvestigationItem tixi : getImportantItems(tix)) {
                tixi.setTube(tix.getInvestigationTube());
                tixi.setSample(tix.getSample());
                tixi.setMachine(tix.getMachine());
//                Item sc = itemController.getFirstInvestigationSampleComponents(tix);
//                tixi.setSampleComponent(sc);
                getFacade().edit(tixi);
            }
        }

    }

    public List<InvestigationItem> listInvestigationItemsFilteredByItemTypes(Investigation ix, List<InvestigationItemType> types) {
        List<InvestigationItem> tis = new ArrayList<>();
        if (ix != null) {
            String temSql;
            temSql = "SELECT i FROM InvestigationItem i "
                    + " where i.retired=false "
                    + " and i.item=:item "
                    + " and i.ixItemType in :types "
                    + " order by i.riTop, i.riLeft";
            Map m = new HashMap();
            m.put("item", ix);
            m.put("types", types);
            tis = getFacade().findByJpql(temSql, m);
        }
        return tis;
    }

    public List<Item> getCurrentReportComponants() {
        if (currentInvestigation == null) {
            JsfUtil.addErrorMessage("Select an investigation");
            return null;
        }
        String j = "select i from Item i where i.itemType=:t and i.parentItem=:m and i.retired=:r order by i.name";
        Map m = new HashMap();
        m.put("t", ItemType.SampleComponent);
        m.put("r", false);
        m.put("m", currentInvestigation);
        return getItemFacade().findByJpql(j, m);
    }

    public void setCurrentReportComponants(List<Item> crc) {

    }

    public void setMovePercent(Double movePercent) {
        this.movePercent = movePercent;
    }

    public Double getFixHeight() {
        if (fixHeight == null) {
            fixHeight = 2.0;
        }
        return fixHeight;
    }

    public void setFixHeight(Double fixHeight) {
        this.fixHeight = fixHeight;
    }

    public Double getFixWidth() {
        if (fixWidth == null) {
            fixWidth = 10.0;
        }
        return fixWidth;
    }

    public void setFixWidth(Double fixWidth) {
        this.fixWidth = fixWidth;
    }

    public void toInvestigationMaster() {
        investigationController.setCurrent(currentInvestigation);
    }

    public String copyInvestigation() {
        if (copyingFromInvestigation == null) {
            JsfUtil.addErrorMessage("Please select an iinvestigation to copy from");
            return "";
        }
        if (copyingToInvestigation == null) {
            JsfUtil.addErrorMessage("Please select an iinvestigation to copy from");
            return "";
        }

        ////System.out.println("copyingFromInvestigation = " + copyingFromInvestigation);
        ////System.out.println("copyingToInvestigation = " + copyingToInvestigation);
        for (InvestigationItem ii : copyingFromInvestigation.getReportItems()) {

            ////System.out.println("ii = " + ii);
            if (!ii.isRetired()) {

                InvestigationItem nii = new InvestigationItem();
                nii.setCategory(ii.getCategory());
                nii.setCreatedAt(new Date());
                nii.setCreater(getSessionController().getLoggedUser());
                nii.setCssBackColor(ii.getCssBackColor());
                nii.setCssBorder(ii.getCssBorder());
                nii.setCssBorderRadius(ii.getCssBorderRadius());
                nii.setCssClip(ii.getCssClip());
                nii.setCssColor(ii.getCssColor());
                nii.setCssFontFamily(ii.getCssFontFamily());
                nii.setCssFontSize(ii.getCssFontSize());
                nii.setCssFontStyle(ii.getCssFontStyle());
                nii.setCssFontVariant(ii.getCssFontVariant());
                nii.setCssFontWeight(ii.getCssFontWeight());
                nii.setCssHeight(ii.getCssHeight());
                nii.setCssLeft(ii.getCssLeft());
                nii.setCssLineHeight(ii.getCssLineHeight());
                nii.setCssMargin(ii.getCssMargin());
                nii.setCssOverflow(ii.getCssOverflow());
                nii.setCssPadding(ii.getCssPadding());
                nii.setCssPosition(ii.getCssPosition());
                nii.setCssStyle(ii.getCssStyle());
                nii.setCssTextAlign(ii.getCssTextAlign());
                nii.setCssTop(ii.getCssTop());
                nii.setCssVerticalAlign(ii.getCssVerticalAlign());
                nii.setCssWidth(ii.getCssWidth());
                nii.setCssZorder(ii.getCssZorder());

                nii.setIxItemType(ii.getIxItemType());
                nii.setIxItemValueType(ii.getIxItemValueType());
                nii.setItem(copyingToInvestigation);

                nii.setName(ii.getName());
                nii.setReportItemType(ii.getReportItemType());

                List<InvestigationItemValue> niivs = new ArrayList<>();
                for (InvestigationItemValue iiv : ii.getInvestigationItemValues()) {

                    ////System.out.println("iiv = " + iiv);
                    InvestigationItemValue niiv = new InvestigationItemValue();
                    niiv.setCode(iiv.getCode());
                    niiv.setCreatedAt(new Date());
                    niiv.setCreater(getSessionController().getLoggedUser());
                    niiv.setInvestigationItem(nii);
                    niiv.setName(iiv.getName());
                    niiv.setOrderNo(iiv.getOrderNo());
                    niivs.add(niiv);
                }

                nii.setInvestigationItemValues(niivs);

                getEjbFacade().create(nii);

            }

        }

        setCurrentInvestigation(copyingToInvestigation);

        return toEditInvestigationFormat();

    }

    public EditMode getEditMode() {
        return editMode;
    }

    public void setEditMode(EditMode editMode) {
        this.editMode = editMode;
    }

    public int getPreviousKeyCode() {
        return previousKeyCode;
    }

    public void setPreviousKeyCode(int previousKeyCode) {
        this.previousKeyCode = previousKeyCode;
    }

    public int getSpecialCode() {
        return specialCode;
    }

    public void setSpecialCode(int specialCode) {
        this.specialCode = specialCode;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
    }

    public void ajaxIiKeydownListner(InvestigationItem ii) {
        if (ii == null) {
            return;
        }
        if (specialCode == 17) {
            specialCode = 17;
        } else {
            if (keyCode != previousKeyCode) {
                specialCode = 0;
            }
            switch (keyCode) {
                case 38:
                    //Up
                    ii.setRiTop(ii.getRiTop() - 1);
                    break;
                case 40:
                    // Down
                    ii.setRiTop(ii.getRiTop() + 1);
                    break;
                case 37:
                    // Left
                    ii.setRiLeft(ii.getRiLeft() - 1);
                    break;
                case 39:
                    // Right
                    ii.setRiLeft(ii.getRiLeft() + 1);
                    break;
                case 46:
                    ii.setRetired(true);
                    ii.setRetiredAt(new Date());
                    ii.setRetirer(getSessionController().getLoggedUser());
                    break;
                case 66://b
                    ii.setCssFontWeight("bold");
                    break;
                case 73://i
                    ii.setCssFontStyle(CssFontStyle.Italic);
                    break;
                case 78://n
                    ii.setCssFontWeight("bold");
                    ii.setCssFontStyle(CssFontStyle.Normal);
                    break;
                case 82://r
                    ii.setCssTextAlign(CssTextAlign.Right);
                    break;
                case 76://l
                    ii.setCssTextAlign(CssTextAlign.Left);
                    break;
                case 67://c
                    ii.setCssTextAlign(CssTextAlign.Center);
                    break;
                case 74://j
                    ii.setCssTextAlign(CssTextAlign.Justify);
                    break;
                case 71://g
                    ii.setRiFontSize(ii.getRiFontSize() + 1);
                    break;
                case 83://s
                    ii.setRiFontSize(ii.getRiFontSize() - 1);
                    break;
                case 87://w
                    ii.setRiWidth(ii.getRiWidth() + 1);
                    break;
                case 79://o
                    ii.setRiWidth(ii.getRiWidth() - 1);
                    break;
                case 84://t
                    ii.setRiHeight(ii.getRiHeight() + 1);
                    break;
                case 72://h
                    ii.setRiHeight(ii.getRiHeight() - 1);
                    break;
                default:
                    // Other key was pressed.
                    break;
            }
        }
        previousKeyCode = keyCode;
        getFacade().edit(ii);
        setCurrent(ii);
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public void saveIiOnAjax(InvestigationItem ii) {
        if (ii != null) {
            getFacade().edit(ii);
        }
        setCurrent(ii);
    }

    public void makeThisCurrent(InvestigationItem ii) {
        setCurrent(ii);
    }

    public InvestigationItemValueFacade getIivFacade() {
        return iivFacade;
    }

    public void setIivFacade(InvestigationItemValueFacade iivFacade) {
        this.iivFacade = iivFacade;
    }

    public String getAddingString() {
        return addingString;
    }

    public void setAddingString(String addingString) {
        this.addingString = addingString;
    }

    public List<ReportItem> getAllReportItemList() {
        String sql = "select ri from ReportItem ri where ri.item = :item ";
        Map m = new HashMap();
        m.put("item", currentInvestigation);
        return riFacade.findByJpql(sql, m);
    }

    public void moveUpAllReportItems() {
        if (getSelectedItemsToChange().isEmpty()) {
            JsfUtil.addErrorMessage("There is No items to move");
            return;
        }

        for (ReportItem ri : getSelectedItemsToChange()) {
            ri.setRiTop(ri.getRiTop() - movePercent);
            riFacade.edit(ri);
        }

        JsfUtil.addSuccessMessage("Moved Successfully");
    }

    public void moveLeftAllReportItems() {
        if (getSelectedItemsToChange().isEmpty()) {
            JsfUtil.addErrorMessage("There is No items to move");
            return;
        }

        for (ReportItem ri : getSelectedItemsToChange()) {
            ri.setRiLeft(ri.getRiLeft() - movePercent);
            riFacade.edit(ri);
        }

        JsfUtil.addSuccessMessage("Moved Successfully");
    }

    public void moveDownAllReportItems() {
        if (getSelectedItemsToChange().isEmpty()) {
            JsfUtil.addErrorMessage("There is No items to move");
            return;
        }

        for (ReportItem ri : getSelectedItemsToChange()) {
            ri.setRiTop(ri.getRiTop() + movePercent);
            riFacade.edit(ri);
        }

        JsfUtil.addSuccessMessage("Moved Successfully");
    }

    public void fixWidthAllReportItems() {
        if (getSelectedItemsToChange().isEmpty()) {
            JsfUtil.addErrorMessage("There is No items to move");
            return;
        }

        for (ReportItem ri : getSelectedItemsToChange()) {
            ri.setRiWidth(fixWidth);
            riFacade.edit(ri);
        }

        JsfUtil.addSuccessMessage("Fixed the width");
    }

    public void fixHeightAllReportItems() {
        if (getSelectedItemsToChange().isEmpty()) {
            JsfUtil.addErrorMessage("There is No items to move");
            return;
        }

        for (ReportItem ri : getSelectedItemsToChange()) {
            ri.setRiHeight(fixHeight);
            riFacade.edit(ri);
        }

        JsfUtil.addSuccessMessage("Fixed the width");
    }

    public void moveRightAllReportItems() {
        if (getSelectedItemsToChange().isEmpty()) {
            JsfUtil.addErrorMessage("There is No items to move");
            return;
        }

        for (ReportItem ri : getSelectedItemsToChange()) {
            ri.setRiLeft(ri.getRiLeft() + movePercent);
            riFacade.edit(ri);
        }

        JsfUtil.addSuccessMessage("Moved Successfully");
    }

    public void updateAllFontValues() {
        if (getSelectedItemsToChange().isEmpty()) {
            JsfUtil.addErrorMessage("There is No items to update font");
            return;
        }
        for (ReportItem ri : getSelectedItemsToChange()) {
            if (fontFamily != null) {
                ri.setCssFontFamily(fontFamily);
                riFacade.edit(ri);
            }

            if (fontSize != 0) {
                ri.setRiFontSize(fontSize);
                riFacade.edit(ri);
            }
        }

        JsfUtil.addSuccessMessage("Update Success");

    }

//    public List<InvestigationItem> completeIxItemForAnyIx(String qry) {
//        List<InvestigationItem> iivs;
//        if (qry.trim().equals("")) {
//            return new ArrayList<>();
//        } else {
//            String sql;
//            Map m = new HashMap();
//            sql = "select i from InvestigationItem i where i.retired<>true "
//                    + "and i.ixItemType = :t "
//                    + "and (i.name) like :n "
//                    + "order by i.name";
//
//            sql = "select i from InvestigationItem i where "
//                    + " (i.name) like :n "
//                    + "order by i.name";
//
////            m.put("t", InvestigationItemType.Value);
//            m.put("n", "'%" + qry.toUpperCase() + "%'");
//            //System.out.println("m = " + m);
//            iivs = getEjbFacade().findByJpql(sql, m);
//        }
//        if (iivs == null) {
//            iivs = new ArrayList<>();
//        }
//        return iivs;
//    }
//    public List<InvestigationItem> completeIxItem(String qry) {
//        List<InvestigationItem> iivs;
//        if (qry.trim().equals("") || currentInvestigation == null || currentInvestigation.getId() == null) {
//            return new ArrayList<>();
//        } else {
//            String sql;
//            sql = "select i from InvestigationItem i where i.retired=false and i.ixItemType = com.divudi.data.InvestigationItemType.Value and (i.name) like '%" + qry.toUpperCase() + "%' and i.item.id = " + currentInvestigation.getId();
//            iivs = getEjbFacade().findByJpql(sql);
//        }
//        if (iivs == null) {
//            iivs = new ArrayList<>();
//        }
//        return iivs;
//    }
    public List<InvestigationItem> completeTemplate(String qry) {
        List<InvestigationItem> iivs;
        if (qry.trim().equals("")) {
            return new ArrayList<>();
        } else {
            String sql;
            Map m = new HashMap();
            sql = "select i from InvestigationItem i "
                    + " where i.retired=false "
                    + " and i.ixItemType = :t and (i.name) like :q "
                    + " order by i.name";
            m.put("t", InvestigationItemType.Template);
            m.put("q", "%" + qry.toUpperCase() + "%");
            iivs = getEjbFacade().findByJpql(sql, m);
        }
        if (iivs == null) {
            iivs = new ArrayList<>();
        }
        return iivs;
    }

    public void saveTemplate() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }
        if (current.getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please give a name");
            return;
        }
        if (current.getHtmltext().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter template");
            return;
        }
        current.setIxItemType(InvestigationItemType.Template);
        current.setIxItemValueType(InvestigationItemValueType.Memo);
        if (current.getId() == null) {
            getFacade().create(current);
            JsfUtil.addSuccessMessage("New Tempalte Added");
        } else {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Template Updated");
        }

    }

    public void newTemplate() {
        current = new InvestigationItem();
    }

    public List<InvestigationItem> getCurrentIxItems() {
        List<InvestigationItem> iivs;
        if (currentInvestigation == null || currentInvestigation.getId() == null) {
            return new ArrayList<>();
        } else {
            String sql;
            sql = "select i from InvestigationItem i where i.retired=false and i.ixItemType = com.divudi.data.InvestigationItemType.Value and i.item.id = " + currentInvestigation.getId();
            iivs = getEjbFacade().findByJpql(sql);
        }
        if (iivs == null) {
            iivs = new ArrayList<>();
        }
        return iivs;
    }

    public void addValueToIxItem() {
        if (current == null) {
            JsfUtil.addErrorMessage("Please select an Ix");
            return;
        }
        if (addingString.trim().equals("")) {
            JsfUtil.addErrorMessage("Enter a value");
            return;
        }
        InvestigationItemValue i = new InvestigationItemValue();
        i.setName(addingString);
        i.setInvestigationItem(current);
        current.getInvestigationItemValues().add(i);
        getEjbFacade().edit(current);
        JsfUtil.addSuccessMessage("Added");
        addingString = "";
    }

    public InvestigationItemValue getRemovingItem() {
        return removingItem;
    }

    public void setRemovingItem(InvestigationItemValue removingItem) {
        this.removingItem = removingItem;
    }

    public InvestigationItemValue getAddingItem() {
        return addingItem;
    }

    public void setAddingItem(InvestigationItemValue addingItem) {
        this.addingItem = addingItem;
    }

    public List<InvestigationItem> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from InvestigationItem c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        if (selectedItems == null) {
            selectedItems = new ArrayList<>();
        }

        return selectedItems;
    }

    public void retireSelectedItems() {
        if (selectedItems == null || selectedItems.isEmpty()) {
            return;
        }
        for (InvestigationItem tii : selectedItems) {
            tii.setRetired(true);
            tii.setRetireComments("Bulk");
            tii.setRetiredAt(new Date());
            tii.setRetirer(sessionController.getLoggedUser());
            getFacade().edit(tii);
        }
    }

    public void listInvestigationItem() {
        if (getCurrentInvestigation() == null || getCurrentInvestigation().getId() == null) {
            items = new ArrayList<>();
        } else {
            items = getEjbFacade().findByJpql("select ii from InvestigationItem ii where ii.retired=false and ii.item.id=" + getCurrentInvestigation().getId());
            userChangableItems = null;
            getUserChangableItems();
        }
    }

    String xml;

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public void convertCssValuesToRiValues() {
        String j = "select ri from ReportItem ri";
        List<ReportItem> ris = riFacade.findByJpql(j);
        for (ReportItem ri : ris) {

            try {

                ri.setCssTop(ri.getCssTop().replace("%", ""));
                ri.setCssLeft(ri.getCssLeft().replace("%", ""));
                ri.setCssHeight(ri.getCssHeight().replace("%", ""));
                ri.setCssWidth(ri.getCssWidth().replace("%", ""));

                try {
                    ri.setRiTop(Double.parseDouble(ri.getCssTop()));
                } catch (Exception e) {
                    ri.setRiTop(11.11);
                }

                try {
                    ri.setRiLeft(Double.parseDouble(ri.getCssLeft()));
                } catch (Exception e) {
                    ri.setRiTop(22.22);
                }

                try {
                    ri.setRiHeight(Double.parseDouble(ri.getCssHeight()));
                } catch (Exception e) {
                    ri.setRiHeight(2);
                }

                try {
                    ri.setRiWidth(Double.parseDouble(ri.getCssWidth()));
                    if (ri.getRiWidth() < 20) {
                        ri.setRiWidth(20);
                    }
                } catch (Exception e) {
                    ri.setRiWidth(40);
                }

                if (ri.getHtmltext() == null || ri.getHtmltext().trim().equals("")) {
                    ri.setHtmltext(ri.getName());
                }

                riFacade.edit(ri);

            } catch (Exception e) {
            }
        }

    }

    public String uploadJsonToCreateAnInvestigation() {
        if (file == null) {
            JsfUtil.addErrorMessage("No file");
            return "";
        }
        try {
            InputStream inputStream = file.getInputStream();
            String text = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            convertJsonToIx(text);

        } catch (IOException ex) {
        }
        return "/lab/investigation_format";
    }

//    public String uploadExcelToCreateInvestigations() {
//        if (file == null) {
//            JsfUtil.addErrorMessage("No file");
//            return "";
//        }
//        try {
//            InputStream inputStream = file.getInputStream();
//            String text = new BufferedReader(
//                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))
//                    .lines()
//                    .collect(Collectors.joining("\n"));
//
//            convertJsonToIx(text);
//
//        } catch (IOException ex) {
//        }
//        return "/lab/uploaded_investigations";
//    }
    private void convertJsonToIx(String jsonString) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode actualObj = mapper.readTree(jsonString);
            String color = actualObj.get("reportFormat").asText();

        } catch (JsonProcessingException ex) {
            Logger.getLogger(InvestigationItemController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private StreamedContent downloadingFile;

    public void createXml() {
//        if (currentInvestigation == null) {
//            return;
//        }
//        InputStream stream = new ByteArrayInputStream(ixToXml(currentInvestigation).getBytes(Charset.defaultCharset()));
//        downloadingFile = new DefaultStreamedContent(stream, "image/jpg", currentInvestigation.getName() + ".xml");
    }

    public StreamedContent getDownloadingFile() {
        createJson();
        return downloadingFile;
    }

    public StreamedContent getIxToJsonFile() {
        createJson();
        return downloadingFile;
    }

    public String toUploadJsonToCreateAnInvestigation() {
        if (institution == null) {
            institution = getSessionController().getInstitution();
        }
        if (department == null) {
            department = getSessionController().getDepartment();
        }
        return "/lab/investigation_upload";
    }

    public void createJson() {
        if (currentInvestigation == null) {
            return;
        }
        InputStream stream = new ByteArrayInputStream(convertIxToJson(currentInvestigation).getBytes(Charset.defaultCharset()));
        downloadingFile = DefaultStreamedContent.builder().contentType("image/jpeg").name(currentInvestigation.getName() + ".json").stream(() -> stream).build();
    }

    public InvestigationItem getLastReportItem() {
        return getLastReportItem(null);
    }

    public InvestigationItem getLastReportItem(InvestigationItemType type) {
        String j = "select i from InvestigationItem i ";
        Map m = new HashMap();
        if (type != null) {
            j += " where i.ixItemType=:t ";
            m.put("t", type);
        }
        j += "order by i.id desc";

        return getEjbFacade().findFirstByJpql(j, m);
    }

    public InvestigationItem getLastReportItemComplete(InvestigationItemType type) {
        List<InvestigationItem> its = getLastReportItems(type);
        for (InvestigationItem ii : its) {
            if (ii.getTestHeader() != null) {
                return ii;
            }
        }
        return null;
    }

    public List<Department> getInstitutionDepatrments() {
        List<Department> d;
        if (getInstitution() == null) {
            return new ArrayList<>();
        } else {
            String sql = "Select d From Department d where d.retired=false and d.institution=:ins order by d.name";
            Map m = new HashMap();
            m.put("ins", getInstitution());
            d = departmentFacade.findByJpql(sql, m);
        }

        return d;
    }

    public List<InvestigationItem> getLastReportItems(InvestigationItemType type) {
        String j = "select i from InvestigationItem i ";
        Map m = new HashMap();
        if (type != null) {
            j += " where i.ixItemType=:t ";
            m.put("t", type);
        }
        j += "order by i.id desc";

        return getEjbFacade().findByJpql(j, m, 50);
    }

    private String testName;
    private String testUnit;
    private String testReferenceRange;
    private String testComments;
    private String cssFontFamily;
    private double riFontSize;
    private CssFontStyle cssFontStyle;
    private String cssHeaderFontWeight;
    private String cssValueFontWeight;
    private CssTextDecoration cssHeaderDecoration;
    private CssTextDecoration cssValueDecoration;
    private boolean addingNewTest = false;
    private boolean withoutHeadings = false;
    private boolean withoutComments = false;
    private boolean withoutReportHeader;
    private boolean withoutReportEnd;
    private double riBlockTop;
    private double riBlockLeft;
    private double riValueLeft;
    private double riUnitLeft;
    private double riFlagLeft;
    private double riRefLeft;
    private double riRowGap;
    private double riColGap;
    private double riHeight;
    private String testHeaderName;
    private String valueHeaderName;
    private String unitHeaderName;
    private String refHeaderName;
    private Machine machine;
    private Item test;
    private boolean automated;
    private Sample sample;
    private Item investigationComponent;
    private InvestigationTube tube;

    public void toAddNewTestFirst() {
        InvestigationItem lastItem = getLastReportItemComplete(InvestigationItemType.Investigation);
        toAddNewTest(lastItem);
    }

    public void toAddNewTestOthers() {
        InvestigationItem lastItem = getLastReportItem(InvestigationItemType.Investigation);
        toAddNewTest(lastItem);
    }

    public String convertIxToJson(Investigation i) {
        String j = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            j = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(i);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(InvestigationItemController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return j;
    }

    public void convertJsonToIx() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            currentInvestigation = mapper.readValue(jsonString, Investigation.class);
            toEditInvestigationFormat();
        } catch (JsonProcessingException ex) {
            Logger.getLogger(InvestigationItemController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void toAddNewTest(InvestigationItem lastItem) {

        addingNewTest = true;

        if (lastItem != null) {

            if (lastItem.getTestHeader() != null) {
                riBlockTop = lastItem.getTestHeader().getRiTop();
                riBlockLeft = lastItem.getTestHeader().getRiLeft();
                testHeaderName = lastItem.getTestHeader().getName();
                riHeight = lastItem.getTestHeader().getRiHeight();
                cssHeaderFontWeight = lastItem.getTestHeader().getCssFontWeight();
                cssHeaderDecoration = lastItem.getTestHeader().getCssTextDecoration();
            }
            if (lastItem.getTestHeader() != null && lastItem.getValueHeader() != null) {
                riRowGap = lastItem.getValueValue().getRiTop() - lastItem.getTestHeader().getRiTop();
                riColGap = lastItem.getValueHeader().getRiLeft() - lastItem.getTestHeader().getRiLeft() - lastItem.getTestHeader().getRiWidth();
            }
            if (lastItem.getValueHeader() != null) {
                riValueLeft = lastItem.getValueHeader().getRiLeft();
                valueHeaderName = lastItem.getValueHeader().getName();
            }
            if (lastItem.getUnitHeader() != null) {
                riUnitLeft = lastItem.getUnitHeader().getRiLeft();
                unitHeaderName = lastItem.getUnitHeader().getName();
            }
            if (lastItem.getReferenceHeader() != null) {
                riRefLeft = lastItem.getReferenceHeader().getRiLeft();
                refHeaderName = lastItem.getReferenceHeader().getName();
            }
            if (lastItem.getValueValue() != null) {
                cssFontFamily = lastItem.getValueValue().getCssFontFamily();
                riFontSize = lastItem.getValueValue().getRiFontSize();
                cssFontStyle = lastItem.getValueValue().getCssFontStyle();
                cssValueFontWeight = lastItem.getValueValue().getCssFontWeight();
                cssValueDecoration = lastItem.getValueValue().getCssTextDecoration();

            }
            if (withoutHeadings) {
                riBlockTop -= riRowGap;
            }

            testName = currentInvestigation.getName();
            tube = currentInvestigation.getInvestigationTube();
            sample = currentInvestigation.getSample();

        }

    }

    public void addNewTest() {
        addingNewTest = false;
        if (currentInvestigation == null) {
            JsfUtil.addErrorMessage("Please select an investigation");
            return;
        }

        InvestigationItem testHeader = new InvestigationItem();
        InvestigationItem valueHeader = new InvestigationItem();
        InvestigationItem unitHeader = new InvestigationItem();
        InvestigationItem referenceHeader = new InvestigationItem();
        InvestigationItem testLabel = new InvestigationItem();
        InvestigationItem valueValue = new InvestigationItem();
        InvestigationItem flagValue = new InvestigationItem();
        InvestigationItem unitLabel = new InvestigationItem();
        InvestigationItem referenceLabel = new InvestigationItem();
        InvestigationItem commentValue = new InvestigationItem();
        current = new InvestigationItem();

        testHeader.setName(testHeaderName);
        testHeader.setHtmltext(testHeaderName);
        testHeader.setIxItemValueType(InvestigationItemValueType.Varchar);
        testHeader.setIxItemType(InvestigationItemType.Label);
        testHeader.setRiTop(riBlockTop);
        testHeader.setRiLeft(riBlockLeft);
        testHeader.setRiWidth(riValueLeft - riBlockLeft - riColGap);
        testHeader.setRiHeight(riHeight);
        testHeader.setCssTextAlign(CssTextAlign.Left);
        testHeader.setCssVerticalAlign(CssVerticalAlign.Top);
        testHeader.setRiFontSize(riFontSize);
        testHeader.setCssFontStyle(cssFontStyle);
        testHeader.setCssFontFamily(cssFontFamily);
        testHeader.setCssFontWeight(cssHeaderFontWeight);
        testHeader.setCssTextDecoration(cssHeaderDecoration);
        testHeader.setItem(currentInvestigation);
        testHeader.setCreatedAt(new Date());
        testHeader.setCreater(getSessionController().getLoggedUser());

        valueHeader.setName(valueHeaderName);
        valueHeader.setHtmltext(valueHeaderName);
        valueHeader.setIxItemType(InvestigationItemType.Label);
        valueHeader.setIxItemValueType(InvestigationItemValueType.Varchar);
        valueHeader.setRiTop(riBlockTop);
        valueHeader.setRiLeft(riValueLeft);
        valueHeader.setRiWidth(riUnitLeft - riValueLeft - riColGap);
        valueHeader.setRiHeight(riHeight);
        valueHeader.setCssTextAlign(CssTextAlign.Left);
        valueHeader.setCssVerticalAlign(CssVerticalAlign.Top);
        valueHeader.setRiFontSize(riFontSize);
        valueHeader.setCssFontStyle(cssFontStyle);
        valueHeader.setCssFontFamily(cssFontFamily);
        valueHeader.setCssFontWeight(cssHeaderFontWeight);
        valueHeader.setCssTextDecoration(cssHeaderDecoration);
        valueHeader.setItem(currentInvestigation);
        valueHeader.setCreatedAt(new Date());
        valueHeader.setCreater(getSessionController().getLoggedUser());

        unitHeader.setName(unitHeaderName);
        unitHeader.setHtmltext(unitHeaderName);
        unitHeader.setIxItemValueType(InvestigationItemValueType.Varchar);
        unitHeader.setIxItemType(InvestigationItemType.Label);
        unitHeader.setRiTop(riBlockTop);
        unitHeader.setRiLeft(riUnitLeft);
        unitHeader.setRiWidth(riRefLeft - riUnitLeft - riColGap);
        unitHeader.setRiHeight(riHeight);
        unitHeader.setCssTextAlign(CssTextAlign.Left);
        unitHeader.setCssVerticalAlign(CssVerticalAlign.Top);
        unitHeader.setRiFontSize(riFontSize);
        unitHeader.setCssFontStyle(cssFontStyle);
        unitHeader.setCssFontFamily(cssFontFamily);
        unitHeader.setCssFontWeight(cssHeaderFontWeight);
        unitHeader.setCssTextDecoration(cssHeaderDecoration);
        unitHeader.setItem(currentInvestigation);
        unitHeader.setCreatedAt(new Date());
        unitHeader.setCreater(getSessionController().getLoggedUser());

        referenceHeader.setName(refHeaderName);
        referenceHeader.setHtmltext(refHeaderName);
        referenceHeader.setIxItemType(InvestigationItemType.Label);
        referenceHeader.setIxItemValueType(InvestigationItemValueType.Varchar);
        referenceHeader.setRiTop(riBlockTop);
        referenceHeader.setRiLeft(riRefLeft);
        referenceHeader.setRiWidth(100 - riRefLeft - riColGap);
        referenceHeader.setRiHeight(riHeight);
        referenceHeader.setCssTextAlign(CssTextAlign.Left);
        referenceHeader.setCssVerticalAlign(CssVerticalAlign.Top);
        referenceHeader.setRiFontSize(riFontSize);
        referenceHeader.setCssFontStyle(cssFontStyle);
        referenceHeader.setCssFontFamily(cssFontFamily);
        referenceHeader.setCssFontWeight(cssHeaderFontWeight);
        referenceHeader.setCssTextDecoration(cssHeaderDecoration);
        referenceHeader.setItem(currentInvestigation);
        referenceHeader.setCreatedAt(new Date());
        referenceHeader.setCreater(getSessionController().getLoggedUser());

        testLabel.setName(testName);
        testLabel.setHtmltext(testName);
        testLabel.setIxItemType(InvestigationItemType.Label);
        testLabel.setIxItemValueType(InvestigationItemValueType.Varchar);
        testLabel.setRiTop(riBlockTop + riRowGap);
        testLabel.setRiLeft(testHeader.getRiLeft());
        testLabel.setRiWidth(testHeader.getRiWidth());
        testLabel.setRiHeight(riHeight);
        testLabel.setCssTextAlign(CssTextAlign.Left);
        testLabel.setCssVerticalAlign(CssVerticalAlign.Top);
        testLabel.setRiFontSize(riFontSize);
        testLabel.setCssFontStyle(cssFontStyle);
        testLabel.setCssFontFamily(cssFontFamily);
        testLabel.setCssFontWeight(cssValueFontWeight);
        testLabel.setCssTextDecoration(cssValueDecoration);
        testLabel.setItem(currentInvestigation);
        testLabel.setCreatedAt(new Date());
        testLabel.setCreater(getSessionController().getLoggedUser());

        valueValue.setName(testName + " value");
        valueValue.setHtmltext(testName + " value");
        valueValue.setIxItemType(InvestigationItemType.Value);
        valueValue.setIxItemValueType(InvestigationItemValueType.Varchar);
        valueValue.setRiTop(valueHeader.getRiTop() + riRowGap);
        valueValue.setRiLeft(valueHeader.getRiLeft());
        valueValue.setRiWidth(valueHeader.getRiWidth() - 5);
        valueValue.setRiHeight(riHeight);
        valueValue.setCssTextAlign(CssTextAlign.Left);
        valueValue.setCssVerticalAlign(CssVerticalAlign.Top);
        valueValue.setRiFontSize(riFontSize);
        valueValue.setCssFontStyle(cssFontStyle);
        valueValue.setCssFontFamily(cssFontFamily);
        valueValue.setCssFontWeight(cssValueFontWeight);
        valueValue.setCssTextDecoration(cssValueDecoration);
        valueValue.setItem(currentInvestigation);
        valueValue.setAutomated(automated);
        valueValue.setMachine(machine);
        valueValue.setTest(test);
        valueValue.setSample(sample);
        valueValue.setCreatedAt(new Date());
        valueValue.setCreater(getSessionController().getLoggedUser());
        valueValue.setSampleComponent(investigationComponent);

        flagValue.setName(testName + " Flag");
        flagValue.setHtmltext(testName + " Flag");
        flagValue.setIxItemType(InvestigationItemType.Flag);
        flagValue.setIxItemValueType(InvestigationItemValueType.Varchar);
        flagValue.setRiTop(valueHeader.getRiTop() + riRowGap);
        flagValue.setRiLeft(valueValue.getRiLeft() + valueValue.getRiWidth() + 1);
        flagValue.setRiWidth(4);
        flagValue.setRiHeight(riHeight);
        flagValue.setCssTextAlign(CssTextAlign.Left);
        flagValue.setCssVerticalAlign(CssVerticalAlign.Top);
        flagValue.setRiFontSize(riFontSize);
        flagValue.setCssFontStyle(cssFontStyle);
        flagValue.setCssFontFamily(cssFontFamily);
        flagValue.setCssFontWeight(cssValueFontWeight);
        flagValue.setCssTextDecoration(cssValueDecoration);
        flagValue.setItem(currentInvestigation);
        flagValue.setAutomated(automated);
        flagValue.setMachine(machine);
        flagValue.setTest(test);
        flagValue.setSample(sample);
        flagValue.setCreatedAt(new Date());
        flagValue.setSampleComponent(investigationComponent);
        flagValue.setCreater(getSessionController().getLoggedUser());

        unitLabel.setName(testUnit);
        unitLabel.setHtmltext(testUnit);
        unitLabel.setIxItemType(InvestigationItemType.Label);
        unitLabel.setIxItemValueType(InvestigationItemValueType.Varchar);
        unitLabel.setRiTop(riBlockTop + riRowGap);
        unitLabel.setRiLeft(unitHeader.getRiLeft());
        unitLabel.setRiWidth(unitHeader.getRiWidth());
        unitLabel.setRiHeight(riHeight);
        unitLabel.setCssTextAlign(CssTextAlign.Left);
        unitLabel.setCssVerticalAlign(CssVerticalAlign.Top);
        unitLabel.setRiFontSize(riFontSize);
        unitLabel.setCssFontStyle(cssFontStyle);
        unitLabel.setCssFontFamily(cssFontFamily);
        unitLabel.setCssFontWeight(cssValueFontWeight);
        unitLabel.setCssTextDecoration(cssValueDecoration);
        unitLabel.setItem(currentInvestigation);
        unitLabel.setAutomated(automated);
        unitLabel.setMachine(machine);
        unitLabel.setTest(test);
        unitLabel.setCreatedAt(new Date());
        unitLabel.setCreater(getSessionController().getLoggedUser());

        referenceLabel.setName(testName + " Ref");
        referenceLabel.setHtmltext(testReferenceRange);
        referenceLabel.setIxItemType(InvestigationItemType.Label);
        referenceLabel.setIxItemValueType(InvestigationItemValueType.Memo);
        referenceLabel.setRiTop(riBlockTop + riRowGap);
        referenceLabel.setRiLeft(referenceHeader.getRiLeft());
        referenceLabel.setRiWidth(referenceHeader.getRiWidth());
        referenceLabel.setRiHeight(riHeight);
        referenceLabel.setCssTextAlign(CssTextAlign.Left);
        referenceLabel.setCssVerticalAlign(CssVerticalAlign.Top);
        referenceLabel.setRiFontSize(riFontSize);
        referenceLabel.setCssFontStyle(cssFontStyle);
        referenceLabel.setCssFontFamily(cssFontFamily);
        referenceLabel.setCssFontWeight(cssValueFontWeight);
        referenceLabel.setCssTextDecoration(cssValueDecoration);
        referenceLabel.setItem(currentInvestigation);
        referenceLabel.setCreatedAt(new Date());
        referenceLabel.setCreater(getSessionController().getLoggedUser());

        commentValue.setName("Test Comments");
        commentValue.setHtmltext(testComments);
        commentValue.setIxItemType(InvestigationItemType.Value);
        commentValue.setIxItemValueType(InvestigationItemValueType.Memo);
        commentValue.setRiTop(riBlockTop + 2 * riRowGap);
        commentValue.setRiLeft(testHeader.getRiLeft());
        commentValue.setRiWidth(100 - (testHeader.getRiLeft() * 2));
        commentValue.setRiHeight(riHeight);
        commentValue.setCssTextAlign(CssTextAlign.Left);
        commentValue.setCssVerticalAlign(CssVerticalAlign.Top);
        commentValue.setRiFontSize(riFontSize);
        commentValue.setCssFontStyle(cssFontStyle);
        commentValue.setCssFontFamily(cssFontFamily);
        commentValue.setCssFontWeight(cssValueFontWeight);
        commentValue.setCssTextDecoration(cssValueDecoration);
        commentValue.setItem(currentInvestigation);
        commentValue.setCreatedAt(new Date());
        commentValue.setCreater(getSessionController().getLoggedUser());

        current.setName(testName);
        current.setItem(currentInvestigation);
        current.setCreatedAt(new Date());
        current.setCreater(getSessionController().getLoggedUser());

        if (!withoutHeadings) {
            currentInvestigation.getReportItems().add(testHeader);
            currentInvestigation.getReportItems().add(valueHeader);
            currentInvestigation.getReportItems().add(unitHeader);
            currentInvestigation.getReportItems().add(referenceHeader);
        }

        currentInvestigation.getReportItems().add(testLabel);
        currentInvestigation.getReportItems().add(valueValue);
        currentInvestigation.getReportItems().add(flagValue);
        currentInvestigation.getReportItems().add(unitLabel);
        currentInvestigation.getReportItems().add(referenceLabel);

        if (!withoutComments) {
            currentInvestigation.getReportItems().add(commentValue);
        }

        currentInvestigation.getReportItems().add(current);

        if (!withoutHeadings) {
            current.setTestHeader(testHeader);
            current.setValueHeader(valueHeader);
            current.setUnitHeader(unitHeader);
            current.setReferenceHeader(referenceHeader);
        }
        current.setTestLabel(testLabel);
        current.setValueValue(valueValue);
        current.setUnitLabel(unitLabel);
        current.setReferenceLabel(referenceLabel);
        if (!withoutComments) {
            current.setCommentLabel(commentValue);
        }
        current.setIxItemType(InvestigationItemType.Investigation);
        current.setAutomated(automated);
        current.setFlagValue(flagValue);
        current.setMachine(machine);
        current.setTest(test);
        current.setSample(sample);
        current.setSampleComponent(investigationComponent);

        if (!withoutReportHeader) {
            InvestigationItem reportHeader = new InvestigationItem();
            reportHeader.setName(currentInvestigation.getFullName());
            reportHeader.setHtmltext(currentInvestigation.getFullName());
            reportHeader.setIxItemValueType(InvestigationItemValueType.Varchar);
            reportHeader.setIxItemType(InvestigationItemType.Label);
            reportHeader.setRiTop(riBlockTop - 2 * riRowGap);
            reportHeader.setRiLeft(10);
            reportHeader.setRiWidth(80);
            reportHeader.setRiHeight(riHeight);
            reportHeader.setCssTextAlign(CssTextAlign.Center);
            reportHeader.setCssVerticalAlign(CssVerticalAlign.Top);
            reportHeader.setRiFontSize(riFontSize);
            reportHeader.setCssFontStyle(cssFontStyle);
            reportHeader.setCssFontFamily(cssFontFamily);
            reportHeader.setCssFontWeight(cssHeaderFontWeight);
            reportHeader.setCssTextDecoration(cssHeaderDecoration);
            reportHeader.setItem(currentInvestigation);
            reportHeader.setCreatedAt(new Date());
            reportHeader.setCreater(getSessionController().getLoggedUser());
            currentInvestigation.getReportItems().add(reportHeader);
        }

        if (!withoutReportEnd) {
            InvestigationItem reportHeader = new InvestigationItem();
            reportHeader.setName("End of Report");
            reportHeader.setHtmltext("-------------- End of Report -------------");
            reportHeader.setIxItemValueType(InvestigationItemValueType.Varchar);
            reportHeader.setIxItemType(InvestigationItemType.Label);
            reportHeader.setRiTop(riBlockTop + 3 * riRowGap);
            reportHeader.setRiLeft(10);
            reportHeader.setRiWidth(80);
            reportHeader.setRiHeight(riHeight);
            reportHeader.setCssTextAlign(CssTextAlign.Center);
            reportHeader.setCssVerticalAlign(CssVerticalAlign.Top);
            reportHeader.setRiFontSize(riFontSize);
            reportHeader.setCssFontStyle(cssFontStyle);
            reportHeader.setCssFontFamily(cssFontFamily);
            reportHeader.setCssFontWeight(cssHeaderFontWeight);
            reportHeader.setCssTextDecoration(cssHeaderDecoration);
            reportHeader.setItem(currentInvestigation);
            reportHeader.setCreatedAt(new Date());
            reportHeader.setCreater(getSessionController().getLoggedUser());
            currentInvestigation.getReportItems().add(reportHeader);
        }

        getIxFacade().edit(currentInvestigation);

        listInvestigationItem();
    }

    public void addNewLabel() {
        if (currentInvestigation == null) {
            JsfUtil.addErrorMessage("Please select an investigation");
            return;
        }
        current = new InvestigationItem();
        current.setName("Label");
        current.setItem(currentInvestigation);
        current.setIxItemType(InvestigationItemType.Label);
        current.setCreatedAt(new Date());
        current.setCreater(getSessionController().getLoggedUser());

        InvestigationItem lastItem = getLastReportItem();
        if (lastItem != null) {
            current.setCssFontFamily(lastItem.getCssFontFamily());
            current.setCssFontSize(lastItem.getCssFontSize());
            current.setCssFontStyle(lastItem.getCssFontStyle());
            current.setCssFontWeight(lastItem.getCssFontWeight());
        }

        currentInvestigation.getReportItems().add(current);
        getIxFacade().edit(currentInvestigation);
        listInvestigationItem();
    }

    public void removeSingleItem(InvestigationItem i) {
        i.setRetired(true);
        i.setRetirer(getSessionController().getLoggedUser());
        i.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        getRiFacade().edit(i);
        getItems().remove(i);
    }

    public void removeItem() {

        if (current.getIxItemType() == InvestigationItemType.Investigation) {
            if (current.getTestHeader() != null) {
                removeSingleItem((InvestigationItem) current.getTestHeader());
            }
            if (current.getValueHeader() != null) {
                removeSingleItem((InvestigationItem) current.getValueHeader());
            }
            if (current.getUnitHeader() != null) {
                removeSingleItem((InvestigationItem) current.getUnitHeader());
            }
            if (current.getReferenceHeader() != null) {
                removeSingleItem((InvestigationItem) current.getReferenceHeader());
            }
            if (current.getTestLabel() != null) {
                removeSingleItem((InvestigationItem) current.getTestLabel());
            }
            if (current.getValueValue() != null) {
                removeSingleItem((InvestigationItem) current.getValueValue());
            }
            if (current.getFlagValue() != null) {
                removeSingleItem((InvestigationItem) current.getFlagValue());
            }
            if (current.getUnitLabel() != null) {
                removeSingleItem((InvestigationItem) current.getUnitLabel());
            }
            if (current.getReferenceLabel() != null) {
                removeSingleItem((InvestigationItem) current.getReferenceLabel());
            }
            if (current.getCommentLabel() != null) {
                removeSingleItem((InvestigationItem) current.getCommentLabel());
            }
        }

        removeSingleItem(current);

    }

    public void removeInvestigationItemValue() {
        ////System.out.println("1");
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to Remove");
            return;
        }
        ////System.out.println("1");
        if (removingItem == null) {
            JsfUtil.addErrorMessage("Nothing to Remove");
            return;
        }
        ////System.out.println("3");
        getIivFacade().remove(removingItem);
        ////System.out.println("4");
        current.getInvestigationItemValues().remove(removingItem);
        ////System.out.println("5");
        getEjbFacade().edit(current);
        ////System.out.println("6");

        JsfUtil.addSuccessMessage("Removed");
    }

    public void addNewValue() {
        if (currentInvestigation == null) {
            JsfUtil.addErrorMessage("Please select an investigation");
            return;
        }
        current = new InvestigationItem();
        current.setName("New Value");
        current.setItem(currentInvestigation);
        current.setIxItemType(InvestigationItemType.Value);

        InvestigationItem lastItem = getLastReportItem();
        if (lastItem != null) {
            current.setCssFontFamily(lastItem.getCssFontFamily());
            current.setCssFontSize(lastItem.getCssFontSize());
            current.setCssFontStyle(lastItem.getCssFontStyle());
            current.setCssFontWeight(lastItem.getCssFontWeight());
        }

        currentInvestigation.getReportItems().add(current);
        getIxFacade().edit(currentInvestigation);
        listInvestigationItem();
    }

    public void addNewCalculation() {
        if (currentInvestigation == null) {
            JsfUtil.addErrorMessage("Please select an investigation");
            return;
        }
        current = new InvestigationItem();
        current.setName("New Calculation");
        current.setItem(currentInvestigation);
        current.setIxItemType(InvestigationItemType.Calculation);
        InvestigationItem lastItem = getLastReportItem();
        if (lastItem != null) {
            current.setCssFontFamily(lastItem.getCssFontFamily());
            current.setCssFontSize(lastItem.getCssFontSize());
            current.setCssFontStyle(lastItem.getCssFontStyle());
            current.setCssFontWeight(lastItem.getCssFontWeight());
        }
//        getEjbFacade().create(current);
        currentInvestigation.getReportItems().add(current);
        getIxFacade().edit(currentInvestigation);
        listInvestigationItem();
        listInvestigationItem();
    }

    public String navigateBackToManageInvestigation() {
        if (currentInvestigation == null) {
            return "";
        }
        investigationController.setCurrent(currentInvestigation);
        return investigationController.navigateToManageInvestigationForLab();
    }

    public void addNewFlag() {
        if (currentInvestigation == null) {
            JsfUtil.addErrorMessage("Please select an investigation");
            return;
        }
        current = new InvestigationItem();
        current.setName("New Flag");
        current.setItem(currentInvestigation);
        current.setIxItemType(InvestigationItemType.Flag);
        InvestigationItem lastItem = getLastReportItem();
        if (lastItem != null) {
            current.setCssFontFamily(lastItem.getCssFontFamily());
            current.setCssFontSize(lastItem.getCssFontSize());
            current.setCssFontStyle(lastItem.getCssFontStyle());
            current.setCssFontWeight(lastItem.getCssFontWeight());
        }
//        getEjbFacade().create(current);
        currentInvestigation.getReportItems().add(current);
        getIxFacade().edit(currentInvestigation);
        listInvestigationItem();
        listInvestigationItem();
    }

    public void prepareAdd() {
        current = new InvestigationItem();
    }

    public void setSelectedItems(List<InvestigationItem> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
        current = null;
        currentInvestigation = null;

    }

    public void saveSelected() {
        if (getCurrent().getId() != null) {
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            JsfUtil.addSuccessMessage("Saved Successfully");
            getCurrentInvestigation().getReportItems().add(current);
            getIxFacade().edit(currentInvestigation);
        }
    }

    public InvestigationFacade getIxFacade() {
        return ixFacade;
    }

    public void setIxFacade(InvestigationFacade ixFacade) {
        this.ixFacade = ixFacade;
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public InvestigationItemFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(InvestigationItemFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public InvestigationItemController() {
    }

    public InvestigationItem getCurrent() {
        if (current == null) {
            current = new InvestigationItem();
        }
        return current;
    }

    public void setCurrent(InvestigationItem current) {
        this.current = current;
    }

    private InvestigationItemFacade getFacade() {
        return ejbFacade;
    }

    public List<InvestigationItem> getItems() {
        items = getItems(currentInvestigation);
        return items;
    }

    public List<InvestigationItem> getItems(Investigation ix) {
        List<InvestigationItem> iis;
        if (ix != null && ix.getId() != null) {
            String temSql;
            temSql = "SELECT i FROM InvestigationItem i where i.retired=false and i.item=:item order by i.riTop, i.riLeft";
            Map m = new HashMap();
            m.put("item", ix);
            iis = ejbFacade.findByJpql(temSql, m);
        } else {
            iis = new ArrayList<>();
        }
        return iis;
    }

    public Long findItemCount(Investigation ix) {
        Long iis;
        if (ix != null && ix.getId() != null) {
            String temSql;
            temSql = "SELECT i FROM InvestigationItem i where i.retired=false and i.item.id = " + ix.getId();
            iis = getFacade().countByJpql(temSql);
        } else {
            iis = null;
        }
        return iis;
    }

    public Investigation getCurrentInvestigation() {
        if (currentInvestigation == null) {
            currentInvestigation = new Investigation();
        }
        return currentInvestigation;
    }

    public void setCurrentInvestigation(Investigation currentInvestigation) {
        this.currentInvestigation = currentInvestigation;
        listInvestigationItem();
    }

    public String toEditInvestigationFormat() {
        if (currentInvestigation == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return "";
        }
        listInvestigationItem();
        return "/admin/lims/investigation_format";
    }

    public String toEditInvestigationFormatMultiple() {
        if (currentInvestigation == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return "";
        }
        listInvestigationItem();
        return "/admin/lims/investigation_format_multiple";
    }

    public ReportItemFacade getRiFacade() {
        return riFacade;
    }

    public void setRiFacade(ReportItemFacade riFacade) {
        this.riFacade = riFacade;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getTestUnit() {
        return testUnit;
    }

    public void setTestUnit(String testUnit) {
        this.testUnit = testUnit;
    }

    public String getTestReferenceRange() {
        return testReferenceRange;
    }

    public void setTestReferenceRange(String testReferenceRange) {
        this.testReferenceRange = testReferenceRange;
    }

    public String getTestComments() {
        return testComments;
    }

    public void setTestComments(String testComments) {
        this.testComments = testComments;
    }

    public String getCssFontFamily() {
        return cssFontFamily;
    }

    public void setCssFontFamily(String cssFontFamily) {
        this.cssFontFamily = cssFontFamily;
    }

    public double getRiFontSize() {
        return riFontSize;
    }

    public void setRiFontSize(double riFontSize) {
        this.riFontSize = riFontSize;
    }

    public CssFontStyle getCssFontStyle() {
        return cssFontStyle;
    }

    public void setCssFontStyle(CssFontStyle cssFontStyle) {
        this.cssFontStyle = cssFontStyle;
    }

    public String getCssHeaderFontWeight() {
        return cssHeaderFontWeight;
    }

    public void setCssHeaderFontWeight(String cssHeaderFontWeight) {
        this.cssHeaderFontWeight = cssHeaderFontWeight;
    }

    public String getCssValueFontWeight() {
        return cssValueFontWeight;
    }

    public void setCssValueFontWeight(String cssValueFontWeight) {
        this.cssValueFontWeight = cssValueFontWeight;
    }

    public CssTextDecoration getCssHeaderDecoration() {
        return cssHeaderDecoration;
    }

    public void setCssHeaderDecoration(CssTextDecoration cssHeaderDecoration) {
        this.cssHeaderDecoration = cssHeaderDecoration;
    }

    public CssTextDecoration getCssValueDecoration() {
        return cssValueDecoration;
    }

    public void setCssValueDecoration(CssTextDecoration cssValueDecoration) {
        this.cssValueDecoration = cssValueDecoration;
    }

    public boolean isAddingNewTest() {
        return addingNewTest;
    }

    public void setAddingNewTest(boolean addingNewTest) {
        this.addingNewTest = addingNewTest;
    }

    public double getRiBlockTop() {
        return riBlockTop;
    }

    public void setRiBlockTop(double riBlockTop) {
        this.riBlockTop = riBlockTop;
    }

    public double getRiBlockLeft() {
        return riBlockLeft;
    }

    public void setRiBlockLeft(double riBlockLeft) {
        this.riBlockLeft = riBlockLeft;
    }

    public double getRiValueLeft() {
        return riValueLeft;
    }

    public void setRiValueLeft(double riValueLeft) {
        this.riValueLeft = riValueLeft;
    }

    public double getRiUnitLeft() {
        return riUnitLeft;
    }

    public void setRiUnitLeft(double riUnitLeft) {
        this.riUnitLeft = riUnitLeft;
    }

    public double getRiRefLeft() {
        return riRefLeft;
    }

    public void setRiRefLeft(double riRefLeft) {
        this.riRefLeft = riRefLeft;
    }

    public double getRiRowGap() {
        return riRowGap;
    }

    public void setRiRowGap(double riRowGap) {
        this.riRowGap = riRowGap;
    }

    public double getRiColGap() {
        return riColGap;
    }

    public void setRiColGap(double riColGap) {
        this.riColGap = riColGap;
    }

    public double getRiHeight() {
        return riHeight;
    }

    public void setRiHeight(double riHeight) {
        this.riHeight = riHeight;
    }

    public String getTestHeaderName() {
        return testHeaderName;
    }

    public void setTestHeaderName(String testHeaderName) {
        this.testHeaderName = testHeaderName;
    }

    public String getValueHeaderName() {
        return valueHeaderName;
    }

    public void setValueHeaderName(String valueHeaderName) {
        this.valueHeaderName = valueHeaderName;
    }

    public String getUnitHeaderName() {
        return unitHeaderName;
    }

    public void setUnitHeaderName(String unitHeaderName) {
        this.unitHeaderName = unitHeaderName;
    }

    public String getRefHeaderName() {
        return refHeaderName;
    }

    public void setRefHeaderName(String refHeaderName) {
        this.refHeaderName = refHeaderName;
    }

    public boolean isWithoutHeadings() {
        return withoutHeadings;
    }

    public void setWithoutHeadings(boolean withoutHeadings) {
        this.withoutHeadings = withoutHeadings;
    }

    public boolean isWithoutComments() {
        return withoutComments;
    }

    public void setWithoutComments(boolean withoutComments) {
        this.withoutComments = withoutComments;
    }

    public double getRiFlagLeft() {
        return riFlagLeft;
    }

    public void setRiFlagLeft(double riFlagLeft) {
        this.riFlagLeft = riFlagLeft;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public Item getTest() {
        return test;
    }

    public void setTest(Item test) {
        this.test = test;
    }

    public boolean isAutomated() {
        return automated;
    }

    public void setAutomated(boolean automated) {
        this.automated = automated;
    }

    public boolean isWithoutReportHeader() {
        return withoutReportHeader;
    }

    public void setWithoutReportHeader(boolean withoutReportHeader) {
        this.withoutReportHeader = withoutReportHeader;
    }

    public boolean isWithoutReportEnd() {
        return withoutReportEnd;
    }

    public void setWithoutReportEnd(boolean withoutReportEnd) {
        this.withoutReportEnd = withoutReportEnd;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public Item getInvestigationComponent() {
        return investigationComponent;
    }

    public void setInvestigationComponent(Item investigationComponent) {
        this.investigationComponent = investigationComponent;
    }

    public InvestigationTube getTube() {
        return tube;
    }

    public void setTube(InvestigationTube tube) {
        this.tube = tube;
    }

    public List<InvestigationItem> getUserChangableItems() {
        if (userChangableItems == null || userChangableItems.isEmpty()) {

        } else {
            InvestigationItem tii = userChangableItems.get(0);
            Investigation tix = (Investigation) tii.getItem();
            if (tix.equals(currentInvestigation)) {
                return userChangableItems;
            }
        }
        List<InvestigationItemType> l = new ArrayList<>();
        l.add(InvestigationItemType.Label);
        l.add(InvestigationItemType.Value);
        l.add(InvestigationItemType.Flag);
        l.add(InvestigationItemType.Calculation);
        l.add(InvestigationItemType.Css);
        l.add(InvestigationItemType.DynamicLabel);
        l.add(InvestigationItemType.Investigation);
        l.add(InvestigationItemType.AntibioticList);
        l.add(InvestigationItemType.Template);
        l.add(InvestigationItemType.Barcode);
        l.add(InvestigationItemType.BarcodeVertical);
//            Label,
//    Value,
//    Calculation,
//    Flag,
//    DynamicLabel,
//    Css,
//    Barcode,
//    BarcodeVertical,
//    Investigation,
//    Template,
//    AntibioticList,
        userChangableItems = listInvestigationItemsFilteredByItemTypes(currentInvestigation, l);
        return userChangableItems;
    }

    public void setUserChangableItems(List<InvestigationItem> userChangableItems) {
        this.userChangableItems = userChangableItems;
    }

    public ItemFacade getItemFacade() {
        return ItemFacade;
    }

    public ItemController getItemController() {
        return itemController;
    }

    public String getJsonString() {
        return jsonString;
    }

    public void setJsonString(String jsonString) {
        this.jsonString = jsonString;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public enum EditMode {

        View_Mode,
        Edit_mode,
        Move_Mode,
    }

    public InvestigationController getInvestigationController() {
        return investigationController;
    }

    public void setInvestigationController(InvestigationController investigationController) {
        this.investigationController = investigationController;
    }

    public Investigation getCopyingFromInvestigation() {
        return copyingFromInvestigation;
    }

    public void setCopyingFromInvestigation(Investigation copyingFromInvestigation) {
        this.copyingFromInvestigation = copyingFromInvestigation;
    }

    public Investigation getCopyingToInvestigation() {
        return copyingToInvestigation;
    }

    public void setCopyingToInvestigation(Investigation copyingToInvestigation) {
        this.copyingToInvestigation = copyingToInvestigation;
    }

    public String getIxXml() {
        return ixXml;
    }

    public void setIxXml(String ixXml) {
        this.ixXml = ixXml;
    }

    private List<InvestigationItem> userChangableItems;

    public List<InvestigationItem> getImportantItems() {
        if (importantItems == null || importantItems.isEmpty()) {

        } else {
            InvestigationItem tii = importantItems.get(0);
            Investigation tix = (Investigation) tii.getItem();
            if (tix.equals(currentInvestigation)) {
                return importantItems;
            }
        }
        List<InvestigationItemType> l = new ArrayList<>();
        l.add(InvestigationItemType.Value);
        l.add(InvestigationItemType.Flag);
        l.add(InvestigationItemType.Calculation);
        l.add(InvestigationItemType.DynamicLabel);
        l.add(InvestigationItemType.Template);
        importantItems = listInvestigationItemsFilteredByItemTypes(currentInvestigation, l);
        return importantItems;
    }

    public List<InvestigationItem> getImportantItems(Investigation tix) {
        List<InvestigationItemType> l = new ArrayList<>();
        l.add(InvestigationItemType.Value);
        l.add(InvestigationItemType.Flag);
        l.add(InvestigationItemType.Calculation);
        l.add(InvestigationItemType.DynamicLabel);
        l.add(InvestigationItemType.Template);
        importantItems = listInvestigationItemsFilteredByItemTypes(tix, l);
        return importantItems;
    }

    public void setImportantItems(List<InvestigationItem> importantItems) {
        this.importantItems = importantItems;
    }

    public List<InvestigationItem> getSelectedItemsToChange() {
        return selectedItemsToChange;
    }

    public void setSelectedItemsToChange(List<InvestigationItem> selectedItemsToChange) {
        this.selectedItemsToChange = selectedItemsToChange;
    }

    /**
     *
     */
    @FacesConverter("iiCon")
//    @FacesConverter("ixConverter")
    public static class InvestigationItemConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            InvestigationItemController controller = (InvestigationItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "investigationItemController");
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
            if (object instanceof InvestigationItem) {
                InvestigationItem o = (InvestigationItem) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + InvestigationItemController.class.getName());
            }
        }
    }

    /**
     *
     */
    @FacesConverter(forClass = InvestigationItem.class)
//    @FacesConverter("ixConverter")
    public static class InvestigationItemControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            InvestigationItemController controller = (InvestigationItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "investigationItemController");
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
            if (object instanceof InvestigationItem) {
                InvestigationItem o = (InvestigationItem) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + InvestigationItemController.class.getName());
            }
        }
    }

}
