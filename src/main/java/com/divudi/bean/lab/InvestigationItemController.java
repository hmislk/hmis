/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.CssFontStyle;
import com.divudi.data.CssTextAlign;
import com.divudi.data.CssTextDecoration;
import com.divudi.data.CssVerticalAlign;
import com.divudi.data.InvestigationItemType;
import com.divudi.data.InvestigationItemValueType;
import com.divudi.data.ReportItemType;
import com.divudi.entity.Item;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.entity.lab.InvestigationItemValue;
import com.divudi.entity.lab.ReportItem;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.InvestigationItemFacade;
import com.divudi.facade.InvestigationItemValueFacade;
import com.divudi.facade.ReportItemFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
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
    /**
     * Controllers
     */
    @Inject
    SessionController sessionController;
    @Inject
    InvestigationController investigationController;
    /**
     * Properties
     */
    List<InvestigationItem> selectedItems;
    private InvestigationItem current;
    private Investigation currentInvestigation;
    private List<InvestigationItem> items = null;

    String selectText = "";
    InvestigationItemValue removingItem;
    InvestigationItemValue addingItem;
    String addingString;

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

    public Double getMovePercent() {
        if (movePercent == null) {
            movePercent = 5.0;
        }
        return movePercent;
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

        //System.out.println("copyingFromInvestigation = " + copyingFromInvestigation);
        //System.out.println("copyingToInvestigation = " + copyingToInvestigation);
        for (InvestigationItem ii : copyingFromInvestigation.getReportItems()) {

            //System.out.println("ii = " + ii);
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

                    //System.out.println("iiv = " + iiv);
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

        return "/lab/investigation_format";

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
        System.out.println("ii = " + ii);
        if (ii == null) {
            return;
        }
        System.out.println("keyCode = " + keyCode);
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
        System.out.println("saving Ii on Ajax");
        System.out.println("ii = " + ii);
        if (ii != null) {
            System.out.println("ii name = " + ii.getName());
            getFacade().edit(ii);
        }
        setCurrent(ii);
    }

    public void makeThisCurrent(InvestigationItem ii) {
        System.out.println("saving Ii on Ajax");
        System.out.println("ii = " + ii);
        setCurrent(ii);
        System.out.println("current = " + current);
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
        String sql = "select ri from ReportItem ri ";

        return riFacade.findBySQL(sql);
    }

    public void moveUpAllReportItems() {
        if (getAllReportItemList().isEmpty()) {
            UtilityController.addErrorMessage("There is No items to move");
            return;
        }

        for (ReportItem ri : getAllReportItemList()) {
            ri.setRiTop(ri.getRiTop() + 1);
            riFacade.edit(ri);
        }

        UtilityController.addSuccessMessage("Moved Successfully");
    }

    public void moveLeftAllReportItems() {
        if (getAllReportItemList().isEmpty()) {
            UtilityController.addErrorMessage("There is No items to move");
            return;
        }

        for (ReportItem ri : getAllReportItemList()) {
            ri.setRiLeft(ri.getRiLeft() + 1);
            riFacade.edit(ri);
        }

        UtilityController.addSuccessMessage("Moved Successfully");
    }

    public void moveDownAllReportItems() {
        if (getAllReportItemList().isEmpty()) {
            UtilityController.addErrorMessage("There is No items to move");
            return;
        }

        for (ReportItem ri : getAllReportItemList()) {
            ri.setRiHeight(ri.getRiHeight() + 1);
            riFacade.edit(ri);
        }

        UtilityController.addSuccessMessage("Moved Successfully");
    }

    public void fixWidthAllReportItems() {
        if (getItems().isEmpty()) {
            UtilityController.addErrorMessage("There is No items to move");
            return;
        }

        for (ReportItem ri : getItems()) {
            ri.setRiWidth(fixWidth);
            riFacade.edit(ri);
        }

        UtilityController.addSuccessMessage("Fixed the width");
    }

    public void fixHeightAllReportItems() {
        if (getItems().isEmpty()) {
            UtilityController.addErrorMessage("There is No items to move");
            return;
        }

        for (ReportItem ri : getItems()) {
            ri.setRiHeight(fixHeight);
            riFacade.edit(ri);
        }

        UtilityController.addSuccessMessage("Fixed the width");
    }

    public void moveRightAllReportItems() {
        if (getAllReportItemList().isEmpty()) {
            UtilityController.addErrorMessage("There is No items to move");
            return;
        }

        for (ReportItem ri : getAllReportItemList()) {
            ri.setRiWidth(ri.getRiWidth() + 1);
            riFacade.edit(ri);
        }

        UtilityController.addSuccessMessage("Moved Successfully");
    }

    public void updateAllFontValues() {

        for (ReportItem ri : getAllReportItemList()) {
            if (fontFamily != null) {
                System.out.println("update Font Family");
                ri.setCssFontFamily(fontFamily);
                riFacade.edit(ri);
            }

            if (fontSize != 0) {
                System.out.println("update Font Size");
                ri.setRiFontSize(fontSize);
                riFacade.edit(ri);
            }
        }

        UtilityController.addSuccessMessage("Update Success");

    }

    public List<InvestigationItem> completeIxItem(String qry) {
        List<InvestigationItem> iivs;
        if (qry.trim().equals("") || currentInvestigation == null || currentInvestigation.getId() == null) {
            return new ArrayList<>();
        } else {
            String sql;
            sql = "select i from InvestigationItem i where i.retired=false and i.ixItemType = com.divudi.data.InvestigationItemType.Value and upper(i.name) like '%" + qry.toUpperCase() + "%' and i.item.id = " + currentInvestigation.getId();
            iivs = getEjbFacade().findBySQL(sql);
        }
        if (iivs == null) {
            iivs = new ArrayList<>();
        }
        return iivs;
    }

    public List<InvestigationItem> getCurrentIxItems() {
        List<InvestigationItem> iivs;
        if (currentInvestigation == null || currentInvestigation.getId() == null) {
            return new ArrayList<>();
        } else {
            String sql;
            sql = "select i from InvestigationItem i where i.retired=false and i.ixItemType = com.divudi.data.InvestigationItemType.Value and i.item.id = " + currentInvestigation.getId();
            iivs = getEjbFacade().findBySQL(sql);
        }
        if (iivs == null) {
            iivs = new ArrayList<>();
        }
        return iivs;
    }

    public void addValueToIxItem() {
        if (current == null) {
            UtilityController.addErrorMessage("Please select an Ix");
            return;
        }
        if (addingString.trim().equals("")) {
            UtilityController.addErrorMessage("Enter a value");
            return;
        }
        InvestigationItemValue i = new InvestigationItemValue();
        i.setName(addingString);
        i.setInvestigationItem(current);
        current.getInvestigationItemValues().add(i);
        getEjbFacade().edit(current);
        UtilityController.addSuccessMessage("Added");
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
        selectedItems = getFacade().findBySQL("select c from InvestigationItem c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        if (selectedItems == null) {
            selectedItems = new ArrayList<>();
        }

        return selectedItems;
    }

    public void listInvestigationItem() {
        if (getCurrentInvestigation() == null || getCurrentInvestigation().getId() == null) {
            items = new ArrayList<>();
        } else {
            items = getEjbFacade().findBySQL("select ii from InvestigationItem ii where ii.retired=false and ii.item.id=" + getCurrentInvestigation().getId());
        }
    }

    String xml;

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    private UploadedFile file;

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public void convertCssValuesToRiValues() {
        String j = "select ri from ReportItem ri";
        List<ReportItem> ris = riFacade.findBySQL(j);
        for (ReportItem ri : ris) {

            try {

                System.out.println("ri = " + ri);

                ri.setCssTop(ri.getCssTop().replace("%", ""));
                ri.setCssLeft(ri.getCssLeft().replace("%", ""));
                ri.setCssHeight(ri.getCssHeight().replace("%", ""));
                ri.setCssWidth(ri.getCssWidth().replace("%", ""));

                try {
                    ri.setRiTop(Double.parseDouble(ri.getCssTop()));
                } catch (Exception e) {
                    ri.setRiTop(11.11);
                    System.out.println("ri.getCssTop() = " + ri.getCssTop());
                }

                try {
                    ri.setRiLeft(Double.parseDouble(ri.getCssLeft()));
                } catch (Exception e) {
                    ri.setRiTop(22.22);
                    System.out.println("ri.getCssLeft() = " + ri.getCssLeft());
                }

                try {
                    ri.setRiHeight(Double.parseDouble(ri.getCssHeight()));
                } catch (Exception e) {
                    ri.setRiHeight(2);
                    System.out.println("ri.getCssHeight() = " + ri.getCssHeight());
                }

                try {
                    ri.setRiWidth(Double.parseDouble(ri.getCssWidth()));
                    if (ri.getRiWidth() < 20) {
                        ri.setRiWidth(20);
                    }
                } catch (Exception e) {
                    ri.setRiWidth(40);
                    System.out.println("ri.getCssWidth() = " + ri.getCssWidth());
                }

                System.out.println("ri.getName() = " + ri.getName());
                System.out.println("ri.getHtmltext() = " + ri.getHtmltext());

                if (ri.getHtmltext() == null || ri.getHtmltext().trim().equals("")) {
                    ri.setHtmltext(ri.getName());
                }

                riFacade.edit(ri);

            } catch (Exception e) {
                System.out.println("e = " + e);
            }
        }

    }

    public void upload() {
        if (getCurrentInvestigation() == null) {
            JsfUtil.addErrorMessage("No Investigation");
            return;
        }
        for (InvestigationItem ii : getCurrentInvestigation().getReportItems()) {
            ii.setRetired(true);
            ii.setRetiredAt(new Date());
            ii.setRetireComments("Retired before importing the format");
            ii.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(ii);
        }
        InputStream in;

        StringWriter writer = new StringWriter();
        System.out.println("file = " + file);
        if (file != null) {
            try {
                File uploadedFile = new File("/tmp/" + file.getFileName());
                InputStream inputStream = file.getInputstream();
                OutputStream out = new FileOutputStream(uploadedFile);
                int read = 0;
                byte[] bytes = new byte[1024];
                while ((read = inputStream.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
                inputStream.close();
                out.flush();
                out.close();
                SAXBuilder builder = new SAXBuilder();
                File xmlfile = uploadedFile;
                Document document = (Document) builder.build(xmlfile);
                Element rootNode = document.getRootElement();
                List list = rootNode.getChildren("investigation_item");
                for (int i = 0; i < list.size(); i++) {
                    Element nii = (Element) list.get(i);
                    InvestigationItem ii = new InvestigationItem();
                    ii.setItem(currentInvestigation);
                    ii.setCreatedAt(new Date());
                    ii.setCreater(getSessionController().getLoggedUser());
                    ii.setCssFontFamily(nii.getAttributeValue("font_family"));
                    ii.setCssFontVariant(nii.getAttributeValue("font_variant"));
                    ii.setCssFontWeight(nii.getAttributeValue("font_weight"));
                    ii.setCssMargin(nii.getAttributeValue("margin"));
                    ii.setCssPadding(nii.getAttributeValue("padding"));
                    ii.setDescription(nii.getAttributeValue("description"));
                    ii.setFormatPrefix(nii.getAttributeValue("prefix"));
                    ii.setFormatString(nii.getAttributeValue("format_string"));
                    ii.setFormatSuffix(nii.getAttributeValue("suffix"));
                    ii.setName(nii.getAttributeValue("name"));
                    ii.setHtmltext(nii.getAttributeValue("html_text"));
                    ii.setCssFontFamily(nii.getAttributeValue("font_family"));
                    ii.setCssFontStyle(CssFontStyle.valueOf(nii.getAttributeValue("font_style")));
                    ii.setCssTextAlign(CssTextAlign.valueOf(nii.getAttributeValue("text_align")));
                    ii.setCssVerticalAlign(CssVerticalAlign.valueOf(nii.getAttributeValue("vertical_align")));
                    ii.setIxItemType(InvestigationItemType.valueOf(nii.getAttributeValue("item_type")));
                    ii.setIxItemValueType(InvestigationItemValueType.valueOf(nii.getAttributeValue("value_type")));
                    if (nii.getAttributeValue("report_item_type") != null && ReportItemType.valueOf(nii.getAttributeValue("report_item_type")) != null) {
                        ii.setReportItemType(ReportItemType.valueOf(nii.getAttributeValue("report_item_type")));
                    }
                    ii.setRiFontSize(nii.getAttribute("font_size").getIntValue());
                    ii.setRiHeight(nii.getAttribute("height").getIntValue());
                    ii.setRiLeft(nii.getAttribute("left").getIntValue());
                    ii.setRiTop(nii.getAttribute("top").getIntValue());
                    ii.setRiWidth(nii.getAttribute("width").getIntValue());

                    List listiivs = nii.getChildren("investigation_item_value");
                    for (Object listiiv : listiivs) {
                        Element niiv = (Element) listiiv;
                        InvestigationItemValue iiv = new InvestigationItemValue();
                        iiv.setInvestigationItem(ii);
                        iiv.setCreatedAt(new Date());
                        iiv.setCreater(getSessionController().getLoggedUser());
                        iiv.setName(niiv.getAttributeValue("name"));
                        ii.getInvestigationItemValues().add(iiv);
                        getIivFacade().edit(iiv);
                    }
                    getFacade().edit(ii);
                }
                getIxFacade().edit(currentInvestigation);
            } catch (IOException io) {
                System.out.println("IOException");
                System.out.println(io.getMessage());
            } catch (Exception jdomex) {
                System.out.println("JDOM Excepton");
                System.out.println(jdomex.getMessage());
            }

        }
    }

    private StreamedContent downloadingFile;

    public void createXml() {
        if (currentInvestigation == null) {
            return;
        }
        InputStream stream = new ByteArrayInputStream(ixToXml(currentInvestigation).getBytes(Charset.defaultCharset()));
        downloadingFile = new DefaultStreamedContent(stream, "image/jpg", currentInvestigation.getName() + ".xml");
    }

    public StreamedContent getDownloadingFile() {
        createXml();
        return downloadingFile;
    }

//    public void get
    public String ixToXml(Item item) {
        if (item == null) {
            return "";
        }
        if (!(item instanceof Investigation)) {
            return "";
        }

        Investigation ix = (Investigation) item;

        String xml = "";

        Element eix = new Element("investigation");

        if (ix.getBarcode() != null) {
            eix.setAttribute("bar_code", ix.getBarcode());
        }

        if (ix.getCode() != null) {
            eix.setAttribute("code", ix.getCode());
        }

        if (ix.getDescreption() != null) {
            eix.setAttribute("descreption", ix.getDescreption());
        }

        if (ix.getName() != null) {
            eix.setAttribute("name", ix.getName());
        }

        if (ix.getFullName() != null) {
            eix.setAttribute("full_name", ix.getFullName());
        }

        if (ix.getPrintName() != null) {
            eix.setAttribute("print_name", ix.getPrintName());
        }

        if (ix.getShortName() != null) {
            eix.setAttribute("short_name", ix.getShortName());
        }

        if (ix.getCategory() != null && ix.getCategory().getName() != null) {
            eix.setAttribute("category_name", ix.getCategory().getName());
        }

        if (ix.getInvestigationCategory() != null && ix.getCategory().getName() != null) {
            eix.setAttribute("investigation_category_name", ix.getInvestigationCategory().getName());
        }

        if (ix.getInvestigationTube() != null && ix.getInvestigationTube().getName() != null) {
            eix.setAttribute("investigation_tube_name", ix.getInvestigationTube().getName());
        }

        if (ix.getReportType() != null) {
            eix.setAttribute("report_type", ix.getReportType().name());
        }

        if (ix.getSample() != null && ix.getSample().getName() != null) {
            eix.setAttribute("sample_name", ix.getSample().getName());
        }

        Document doc = new Document(eix);
        doc.setRootElement(eix);

        for (InvestigationItem ii : ix.getReportItems()) {

            if (!ii.isRetired()) {

                Element eii = new Element("investigation_item");

                if (ii.getCssFontFamily() != null) {
                    eii.setAttribute("font_family", ii.getCssFontFamily());
                }

                if (ii.getCssFontVariant() != null) {
                    eii.setAttribute("font_variant", ii.getCssFontVariant());
                }

                if (ii.getCssFontWeight() != null) {
                    eii.setAttribute("font_weight", ii.getCssFontWeight());
                }

                if (ii.getCssMargin() != null) {
                    eii.setAttribute("margin", ii.getCssMargin());
                }

                if (ii.getCssPadding() != null) {
                    eii.setAttribute("padding", ii.getCssPadding());
                }

                if (ii.getDescription() != null) {
                    eii.setAttribute("description", ii.getDescription());
                }

                if (ii.getFormatPrefix() != null) {
                    eii.setAttribute("prefix", ii.getFormatPrefix());
                }

                if (ii.getFormatString() != null) {
                    eii.setAttribute("format_string", ii.getFormatString());
                }

                if (ii.getFormatSuffix() != null) {
                    eii.setAttribute("suffix", ii.getFormatSuffix());
                }

                if (ii.getName() != null) {
                    eii.setAttribute("name", ii.getName());
                }

                if (ii.getHtmltext() != null) {
                    eii.setAttribute("html_text", ii.getHtmltext());
                }

                if (ii.getCssFontStyle() != null) {
                    eii.setAttribute("font_style", ii.getCssFontStyle().name());
                }

                if (ii.getCssTextAlign() != null) {
                    eii.setAttribute("text_align", ii.getCssTextAlign().name());
                }

                if (ii.getCssVerticalAlign() != null) {
                    eii.setAttribute("vertical_align", ii.getCssVerticalAlign().name());
                }

                if (ii.getIxItemType() != null) {
                    eii.setAttribute("item_type", ii.getIxItemType().name());
                }

                if (ii.getIxItemValueType() != null) {
                    eii.setAttribute("value_type", ii.getIxItemValueType().name());
                }

                if (ii.getReportItemType() != null) {
                    eii.setAttribute("report_item_type", ii.getReportItemType().name());
                }

                eii.setAttribute("font_size", String.valueOf(ii.getRiFontSize()));
                eii.setAttribute("height", String.valueOf(ii.getRiHeight()));
                eii.setAttribute("left", String.valueOf(ii.getRiLeft()));
                eii.setAttribute("top", String.valueOf(ii.getRiTop()));
                eii.setAttribute("width", String.valueOf(ii.getRiWidth()));

                for (InvestigationItemValue iiv : ii.getInvestigationItemValues()) {
                    Element eiiv = new Element("investigation_item_value");
                    eiiv.setAttribute("name", iiv.getName());
                    eii.addContent(eiiv);
                }

                eix.addContent(eii);
            }
        }

        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat());
        xml = xmlOutput.outputString(doc);
        System.out.println("File Saved!");

        return xml;
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

        return getEjbFacade().findFirstBySQL(j, m);
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
    private double riBlockTop;
    private double riBlockLeft;
    private double riValueLeft;
    private double riUnitLeft;
    private double riRefLeft;
    private double riRowGap;
    private double riColGap;
    private double riHeight;
    private String testHeaderName;
    private String valueHeaderName;
    private String unitHeaderName;
    private String refHeaderName;

    public void toAddNewTest() {
        System.out.print("toAddNewTest");
        addingNewTest = true;
        InvestigationItem lastItem = getLastReportItem(InvestigationItemType.Investigation);
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
                riRowGap = lastItem.getValueHeader().getRiTop() - lastItem.getTestHeader().getRiTop();
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

        }

    }

    public void addNewTest() {
        addingNewTest = false;
        if (currentInvestigation == null) {
            UtilityController.addErrorMessage("Please select an investigation");
            return;
        }

        InvestigationItem testHeader = new InvestigationItem();
        testHeader.setName(testHeaderName);
        testHeader.setHtmltext(testHeaderName);
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

        InvestigationItem valueHeader = new InvestigationItem();
        valueHeader.setName(valueHeaderName);
        valueHeader.setHtmltext(valueHeaderName);
        valueHeader.setIxItemType(InvestigationItemType.Label);
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

        InvestigationItem unitHeader = new InvestigationItem();
        unitHeader.setName(unitHeaderName);
        unitHeader.setHtmltext(unitHeaderName);
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

        InvestigationItem referenceHeader = new InvestigationItem();
        referenceHeader.setName(refHeaderName);
        referenceHeader.setHtmltext(refHeaderName);
        referenceHeader.setIxItemType(InvestigationItemType.Label);
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

        InvestigationItem testLabel = new InvestigationItem();
        testLabel.setName(testName);
        testLabel.setHtmltext(testName);
        testLabel.setIxItemType(InvestigationItemType.Label);
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

        InvestigationItem valueValue = new InvestigationItem();
        valueValue.setName(testName + " value");
        valueValue.setHtmltext(testName + " value");
        valueValue.setIxItemType(InvestigationItemType.Value);
        valueValue.setRiTop(valueHeader.getRiTop() + riRowGap);
        valueValue.setRiLeft(valueHeader.getRiLeft());
        valueValue.setRiWidth(valueHeader.getRiWidth());
        valueValue.setRiHeight(riHeight);
        valueValue.setCssTextAlign(CssTextAlign.Left);
        valueValue.setCssVerticalAlign(CssVerticalAlign.Top);
        valueValue.setRiFontSize(riFontSize);
        valueValue.setCssFontStyle(cssFontStyle);
        valueValue.setCssFontFamily(cssFontFamily);
        valueValue.setCssFontWeight(cssValueFontWeight);
        valueValue.setCssTextDecoration(cssValueDecoration);
        valueValue.setItem(currentInvestigation);
        valueValue.setCreatedAt(new Date());
        valueValue.setCreater(getSessionController().getLoggedUser());

        InvestigationItem unitLabel = new InvestigationItem();
        unitLabel.setName(testUnit);
        unitLabel.setHtmltext(testUnit);
        unitLabel.setIxItemType(InvestigationItemType.Label);
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
        unitLabel.setCreatedAt(new Date());
        unitLabel.setCreater(getSessionController().getLoggedUser());

        InvestigationItem referenceLabel = new InvestigationItem();
        referenceLabel.setName(testReferenceRange);
        referenceLabel.setHtmltext(testReferenceRange);
        referenceLabel.setIxItemType(InvestigationItemType.Label);
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

        InvestigationItem commentLabel = new InvestigationItem();
        commentLabel.setName("Test Comments");
        commentLabel.setHtmltext(testComments);
        commentLabel.setIxItemType(InvestigationItemType.Label);
        commentLabel.setRiTop(riBlockTop + 2 * riRowGap);
        commentLabel.setRiLeft(testHeader.getRiLeft());
        commentLabel.setRiWidth(100 - (testHeader.getRiLeft() * 2));
        commentLabel.setRiHeight(riHeight);
        commentLabel.setCssTextAlign(CssTextAlign.Left);
        commentLabel.setCssVerticalAlign(CssVerticalAlign.Top);
        commentLabel.setRiFontSize(riFontSize);
        commentLabel.setCssFontStyle(cssFontStyle);
        commentLabel.setCssFontFamily(cssFontFamily);
        commentLabel.setCssFontWeight(cssValueFontWeight);
        commentLabel.setCssTextDecoration(cssValueDecoration);
        commentLabel.setItem(currentInvestigation);
        commentLabel.setCreatedAt(new Date());
        commentLabel.setCreater(getSessionController().getLoggedUser());

        current = new InvestigationItem();
        current.setName(testName);
        current.setItem(currentInvestigation);
        current.setCreatedAt(new Date());
        current.setCreater(getSessionController().getLoggedUser());

        currentInvestigation.getReportItems().add(testHeader);
        currentInvestigation.getReportItems().add(valueHeader);
        currentInvestigation.getReportItems().add(unitHeader);
        currentInvestigation.getReportItems().add(referenceHeader);
        currentInvestigation.getReportItems().add(testLabel);
        currentInvestigation.getReportItems().add(valueValue);
        currentInvestigation.getReportItems().add(unitLabel);
        currentInvestigation.getReportItems().add(referenceLabel);
        currentInvestigation.getReportItems().add(commentLabel);
        currentInvestigation.getReportItems().add(current);

        current.setTestHeader(testHeader);
        current.setValueHeader(valueHeader);
        current.setUnitHeader(unitHeader);
        current.setReferenceHeader(referenceHeader);
        current.setTestLabel(testLabel);
        current.setValueValue(valueValue);
        current.setUnitLabel(unitLabel);
        current.setReferenceLabel(referenceLabel);
        current.setCommentLabel(commentLabel);
        current.setIxItemType(InvestigationItemType.Investigation);

        getIxFacade().edit(currentInvestigation);

        listInvestigationItem();
    }

    public void addNewLabel() {
        if (currentInvestigation == null) {
            UtilityController.addErrorMessage("Please select an investigation");
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
        //System.out.println("1");
        if (current == null) {
            UtilityController.addErrorMessage("Nothing to Remove");
            return;
        }
        //System.out.println("1");
        if (removingItem == null) {
            UtilityController.addErrorMessage("Nothing to Remove");
            return;
        }
        //System.out.println("3");
        getIivFacade().remove(removingItem);
        //System.out.println("4");
        current.getInvestigationItemValues().remove(removingItem);
        //System.out.println("5");
        getEjbFacade().edit(current);
        //System.out.println("6");

        UtilityController.addSuccessMessage("Removed");
    }

    public void addNewValue() {
        if (currentInvestigation == null) {
            UtilityController.addErrorMessage("Please select an investigation");
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
            UtilityController.addErrorMessage("Please select an investigation");
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

    public void addNewFlag() {
        if (currentInvestigation == null) {
            UtilityController.addErrorMessage("Please select an investigation");
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

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            UtilityController.addSuccessMessage("Saved Successfully");
            getCurrentInvestigation().getReportItems().add(current);
            getIxFacade().edit(currentInvestigation);
        }

//        recreateModel();
//        getItems();
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
        System.out.println("current = " + current);
        System.out.println("this.current = " + this.current);
        this.current = current;
        System.out.println("this.current = " + this.current);
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
            temSql = "SELECT i FROM InvestigationItem i where i.retired=false and i.item.id = " + ix.getId() + " order by i.ixItemType, i.cssTop , i.cssLeft";
            iis = getFacade().findBySQL(temSql);
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
            iis = getFacade().countBySql(temSql);
        } else {
            iis = null;
        }
        return iis;
    }

    public Investigation getCurrentInvestigation() {
        if (currentInvestigation == null) {
            currentInvestigation = new Investigation();
            //current = null;
        }
        current = null;
        return currentInvestigation;
    }

    public void setCurrentInvestigation(Investigation currentInvestigation) {
        this.currentInvestigation = currentInvestigation;
        listInvestigationItem();

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
