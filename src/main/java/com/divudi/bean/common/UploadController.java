package com.divudi.bean.common;

import com.divudi.data.UploadType;
import com.divudi.entity.Category;
import com.divudi.entity.Upload;
import com.divudi.entity.WebContent;
import com.divudi.facade.UploadFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.ByteArrayInputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.xwpf.converter.xhtml.XHTMLConverter;
import org.apache.poi.xwpf.converter.xhtml.XHTMLOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.w3c.dom.Document;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import org.apache.commons.io.IOUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.primefaces.model.file.UploadedFile;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.HWPFDocumentCore;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xwpf.converter.xhtml.XHTMLConverter;
import org.apache.poi.xwpf.converter.xhtml.XHTMLOptions;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.DefaultStreamedContent;
import java.io.ByteArrayInputStream;

@Named
@SessionScoped
public class UploadController implements Serializable {

    @EJB
    private UploadFacade ejbFacade;
    @Inject
    private WebUserController webUserController;
    @Inject
    WebContentController webContentController;

    private List<Upload> items = null;
    private List<Upload> diagnosisCardTemplates;

    private Upload selected;
    private UploadedFile file;

    public String toAddNewWebImageUpload() {
        selected = new Upload();
        selected.setUploadType(UploadType.Web_Image);
        selected.setWebContent(new WebContent());
        return "/webcontent/upload";
    }

    public String toAddNewDiagnosisCardTemplateUpload() {
        selected = new Upload();
        selected.setUploadType(UploadType.Diagnosis_Card_Template);
        selected.setWebContent(new WebContent());
        return "/inward/upload";
    }

    public String getWordDocumentAsHtml() {
        System.out.println("getWordDocumentAsHtml: Start");

        if (selected == null) {
            System.out.println("getWordDocumentAsHtml: No document is selected");
            return "No document selected.";
        }

        if (selected.getBaImage() == null) {
            System.out.println("getWordDocumentAsHtml: Selected document has no content");
            return "Selected document has no content.";
        }

        try {
            System.out.println("getWordDocumentAsHtml: Creating ByteArrayInputStream");
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(selected.getBaImage());

            System.out.println("getWordDocumentAsHtml: Creating XWPFDocument");
            XWPFDocument document = new XWPFDocument(byteArrayInputStream);

            System.out.println("getWordDocumentAsHtml: Creating XHTMLOptions");
            XHTMLOptions options = XHTMLOptions.create();

            System.out.println("getWordDocumentAsHtml: Creating ByteArrayOutputStream");
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            System.out.println("getWordDocumentAsHtml: Converting to HTML");
            XHTMLConverter.getInstance().convert(document, out, options);

            String htmlOutput = out.toString("UTF-8");
            System.out.println("getWordDocumentAsHtml: Conversion complete");
            return htmlOutput;
        } catch (IOException e) {
            System.out.println("getWordDocumentAsHtml: Error - " + e.getMessage());
            e.printStackTrace();
            return "Error converting document to HTML.";
        }
    }

    public String toAddNewUpload() {
        selected = new Upload();
        selected.setWebContent(new WebContent());
        return "/webcontent/upload";
    }

    public StreamedContent downloadModifiedWordFile() {
        if (selected == null || selected.getBaImage() == null) {
            return null;
        }

        return DefaultStreamedContent.builder()
                .name(selected.getFileName())
                .contentType(selected.getFileType())
                .stream(() -> new ByteArrayInputStream(selected.getBaImage()))
                .build();
    }

    public String toListWebImageUploads() {
        listUploads(UploadType.Web_Image);
        return "/webcontent/uploads";
    }

    public String toListUploads() {
        listUploads(UploadType.Web_Image);
        return "/webcontent/uploads";
    }

    public String toListDiagnosisCardUploads() {
        listUploads(UploadType.Diagnosis_Card_Template);
        return "/inward/uploads";
    }

    public String toViewUpload() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Nothing");
            return "";
        }
        if (selected.getWebContent() == null) {
            selected.setWebContent(new WebContent());
        }
        return "/webcontent/upload";
    }

    public String toViewDiagnosisCardTemplateUpload() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Nothing");
            return "";
        }
        if (selected.getWebContent() == null) {
            selected.setWebContent(new WebContent());
        }
        return "/inward/upload_view";
    }

    public String viewCurrentDiagnosisCardTemplate() {
        if (selected == null) {
            return "";
        }
        if (selected.getFileName() == null || selected.getFileName().trim() == null) {
            return "";
        }
        return convertWordToHtml(selected);
    }

    public String getCurrentDiagnosisCardTemplateAsHtml() {
        if (selected == null) {
            return "";
        }
        if (selected.getFileName() == null || selected.getFileName().trim() == null) {
            return "";
        }
        return convertWordToHtml(selected);
    }

    public String convertWordToHtml(Upload upload) {
        if (upload == null) {
            return "Invalid upload object.";
        }

        try {
            Object wordDocument = uploadToWordDocument(upload);

            try ( ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                if (wordDocument instanceof XWPFDocument) {
                    XWPFDocument docx = (XWPFDocument) wordDocument;
                    XHTMLOptions options = XHTMLOptions.create();
                    XHTMLConverter.getInstance().convert(docx, out, options);
                } else if (wordDocument instanceof HWPFDocument) {
                    HWPFDocument doc = (HWPFDocument) wordDocument;
                    WordToHtmlConverter wordToHtmlConverter = new WordToHtmlConverter(
                            DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
                    wordToHtmlConverter.processDocument(doc);
                    Document htmlDocument = wordToHtmlConverter.getDocument();
                    DOMSource domSource = new DOMSource(htmlDocument);
                    StreamResult streamResult = new StreamResult(out);

                    TransformerFactory tf = TransformerFactory.newInstance();
                    Transformer serializer = tf.newTransformer();
                    serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                    serializer.setOutputProperty(OutputKeys.INDENT, "yes");
                    serializer.setOutputProperty(OutputKeys.METHOD, "html");
                    serializer.transform(domSource, streamResult);
                } else {
                    throw new IllegalArgumentException("Unsupported document type.");
                }

                return out.toString("UTF-8");
            }
        } catch (Exception e) {
            // Handle exceptions
            return "Error converting document: " + e.getMessage();
        }
    }

    public XWPFDocument uploadToXWPFDocument(Upload upload) throws IOException {
        if (upload == null || upload.getBaImage() == null || upload.getBaImage().length == 0) {
            throw new IllegalArgumentException("Invalid upload object or empty file content.");
        }

        try ( ByteArrayInputStream input = new ByteArrayInputStream(upload.getBaImage())) {
            return new XWPFDocument(input);
        }
    }

    public Object uploadToWordDocument(Upload upload) throws IOException {
        if (upload == null || upload.getBaImage() == null || upload.getBaImage().length == 0) {
            throw new IllegalArgumentException("Invalid upload object or empty file content.");
        }

        try ( ByteArrayInputStream input = new ByteArrayInputStream(upload.getBaImage())) {
            if (upload.getFileName().endsWith(".docx")) {
                return new XWPFDocument(input);
            } else if (upload.getFileName().endsWith(".doc")) {
                return new HWPFDocument(new POIFSFileSystem(input));
            } else {
                throw new IllegalArgumentException("Unsupported file type: " + upload.getFileName());
            }
        }
    }

    public void listUploads() {
        listUploads(null);
    }

    public void listUploads(UploadType type) {
        items = fillUploads(type);
    }

    public List<Upload> fillUploads(UploadType type) {
        String j = "select u "
                + " from Upload u "
                + " where u.retired=:ret ";
        Map m = new HashMap();
        m.put("ret", false);
        if (type != null) {
            j += " and u.uploadType=:type ";
            m.put("type", type);
        }
        j += " order by u.webContent.name";
        return getFacade().findByJpql(j, m, TemporalType.DATE);
    }
    
    public Upload findUpload(Category category) {
        String j = "select u "
                + " from Upload u "
                + " where u.retired=:ret"
                + " and u.category=:cat";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("cat", category);
       
        
        return getFacade().findFirstByJpql(j, m, TemporalType.DATE);
    }

    public String upload() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        if (file == null) {
            JsfUtil.addErrorMessage("No file");
            return "";
        }
        try {
            InputStream input = file.getInputStream();
            byte[] bytes = IOUtils.toByteArray(input);
            selected.setBaImage(bytes);
            selected.setFileName(file.getFileName());
            selected.setFileType(file.getContentType());
            selected.setUploadType(UploadType.Web_Image);
            saveUpload(selected);
        } catch (IOException ex) {
        }
        return toListWebImageUploads();
    }

    public String uploadDiagnosisCardTemplate() {
        if (selected == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        if (file == null) {
            JsfUtil.addErrorMessage("No file");
            return "";
        }
        try {
            InputStream input = file.getInputStream();
            byte[] bytes = IOUtils.toByteArray(input);
            selected.setBaImage(bytes);
            selected.setFileName(file.getFileName());
            selected.setFileType(file.getContentType());
            selected.setUploadType(UploadType.Diagnosis_Card_Template);
            saveUpload(selected);
        } catch (IOException ex) {
        }
        return toListWebImageUploads();
    }

    public Upload findAndReplaceText(Upload upload, Map<String, String> replacements) {
        if (upload == null || upload.getBaImage() == null || upload.getFileName() == null) {
            throw new IllegalArgumentException("Invalid upload object or empty file content.");
        }

        if (!upload.getFileName().endsWith(".docx")) {
            throw new IllegalArgumentException("Unsupported file type: " + upload.getFileName());
        }

        try ( XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(upload.getBaImage()))) {
            for (XWPFParagraph p : document.getParagraphs()) {
                for (XWPFRun r : p.getRuns()) {
                    String text = r.getText(0);
                    for (Map.Entry<String, String> entry : replacements.entrySet()) {
                        text = text.replace(entry.getKey(), entry.getValue());
                    }
                    r.setText(text, 0);
                }
            }

            try ( ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                document.write(out);
                upload.setBaImage(out.toByteArray());
            }
        } catch (IOException e) {
            // Handle exceptions
            throw new RuntimeException("Error processing Word document", e);
        }

        return upload;
    }

    public UploadController() {
    }

    public String save() {
        saveUpload(selected);
        listUploads();
        return toListWebImageUploads();
    }

    public String saveDiagnosisCardTemplateUpload() {
        saveUpload(selected);
        return toListDiagnosisCardUploads();
    }

    public void saveUpload(Upload u) {
        if (u == null) {
            return;
        }
        if (u.getWebContent() != null) {
            webContentController.saveWebContent(u.getWebContent());
        }
        if (u.getId() == null) {
            getFacade().create(u);
        } else {
            getFacade().edit(u);
        }
    }

    public Upload getSelected() {
        return selected;
    }

    public void setSelected(Upload selected) {
        this.selected = selected;
    }

    protected void setEmbeddableKeys() {
    }

    protected void initializeEmbeddableKey() {
    }

    private UploadFacade getFacade() {
        return ejbFacade;
    }

    public Upload prepareCreate() {
        selected = new Upload();
        initializeEmbeddableKey();
        return selected;
    }

    public List<Upload> getItems() {
        return items;
    }

    public Upload getUpload(java.lang.Long id) {
        return getFacade().find(id);
    }

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public UploadFacade getEjbFacade() {
        return ejbFacade;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public List<Upload> getDiagnosisCardTemplates() {
        if (diagnosisCardTemplates == null) {
            diagnosisCardTemplates = fillUploads(UploadType.Diagnosis_Card_Template);
        }
        return diagnosisCardTemplates;
    }

    public void setDiagnosisCardTemplates(List<Upload> diagnosisCardTemplates) {
        this.diagnosisCardTemplates = diagnosisCardTemplates;
    }

    @FacesConverter(forClass = Upload.class)
    public static class UploadControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            UploadController controller = (UploadController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "uploadController");
            return controller.getUpload(getKey(value));
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
            if (object instanceof Upload) {
                Upload o = (Upload) object;
                return getStringKey(o.getId());
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "object {0} is of type {1}; expected type: {2}", new Object[]{object, object.getClass().getName(), Upload.class.getName()});
                return null;
            }
        }

    }

}
