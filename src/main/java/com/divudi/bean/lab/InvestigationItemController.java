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
import java.util.List;
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

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private InvestigationItemFacade ejbFacade;
    @EJB
    InvestigationItemValueFacade iivFacade;
    @EJB
    ReportItemFacade riFacade;
    List<InvestigationItem> selectedItems;
    private InvestigationItem current;
    private Investigation currentInvestigation;
    private List<InvestigationItem> items = null;
    String selectText = "";
    InvestigationItemValue removingItem;
    InvestigationItemValue addingItem;
    String addingString;
    EditMode editMode = EditMode.View_Mode;

    private String input;
    private int keyCode;
    private int previousKeyCode;
    private int specialCode;

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

                ri.setRiTop(Integer.valueOf(ri.getCssTop()));
                ri.setRiLeft(Integer.valueOf(ri.getCssLeft()));
                ri.setRiHeight(Integer.valueOf(ri.getCssHeight()));
                ri.setRiWidth(Integer.valueOf(ri.getCssWidth()));

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
        currentInvestigation.getReportItems().add(current);
        getIxFacade().edit(currentInvestigation);
        listInvestigationItem();
    }

    public void removeItem() {
        current.setRetired(true);
        current.setRetirer(getSessionController().getLoggedUser());
        current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        getEjbFacade().edit(getCurrent());
        getItems().remove(getCurrent());

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
    @EJB
    InvestigationFacade ixFacade;

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
        if (getCurrentInvestigation().getId() != null) {
            String temSql;
            temSql = "SELECT i FROM InvestigationItem i where i.retired=false and i.item.id = " + getCurrentInvestigation().getId() + " order by i.ixItemType, i.cssTop , i.cssLeft";
            items = getFacade().findBySQL(temSql);
        } else {
            items = new ArrayList<InvestigationItem>();
        }
        return items;
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

    public enum EditMode {

        View_Mode,
        Edit_mode,
        Move_Mode,
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
