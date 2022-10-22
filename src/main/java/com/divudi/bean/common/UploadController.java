package com.divudi.bean.common;



import com.divudi.entity.Upload;
import com.divudi.entity.WebContent;
import com.divudi.facade.UploadFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.IOException;
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
import org.primefaces.model.file.UploadedFile;

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
    private Upload selected;
  
    private UploadedFile file;

   public String toAddNewUpload(){
       selected = new Upload();
       selected.setWebContent(new WebContent());
       return "/webcontent/upload";
   }
   
    public String toListUploads(){
       listUploads();
       return "/webcontent/uploads";
   }
    
    public String toViewUpload(){
        if(selected==null){
            JsfUtil.addErrorMessage("Nothing");
            return "";
        }
        if(selected.getWebContent()==null){
            selected.setWebContent(new WebContent());
        }
        return "/webcontent/upload";
    }
    
    public void listUploads(){
        String j ="select u "
                + " from Upload u "
                + " where u.retired=:ret "
                + " order by u.webContent.name";
        Map m = new HashMap();
        m.put("ret", false);
        items = getFacade().findBySQL(j, m, TemporalType.DATE);
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
            saveUpload(selected);
        } catch (IOException ex) {
            System.out.println("ex = " + ex);
        }
        return toListUploads();
    }
    
    public UploadController() {
    }
    
    public String save(){
        saveUpload(selected);
        listUploads();
        return toListUploads();
    }
    
    public void saveUpload(Upload u){
        if(u==null){
            return;
        }
        if(u.getWebContent()!=null){
            webContentController.saveWebContent(u.getWebContent());
        }
        if(u.getId()==null){
            getFacade().create(u);
        }else{
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
