/*
 * The MIT License
 *
 * Copyright 2020 hiu_pdhs_sp.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.divudi.bean.common;


import com.divudi.entity.Upload;
import com.divudi.facade.UploadFacade;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author hiu_pdhs_sp
 */
@Named
@RequestScoped
public class StreamedContentController {


    @EJB
    private UploadFacade uploadFacade;

    private String strId;

    /**
     * Creates a new instance of StreamedContentController
     */
    public StreamedContentController() {
    }

    public StreamedContent getImageByUploadId() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getRenderResponse()) {
            return new DefaultStreamedContent();
        } else {
            String id = context.getExternalContext().getRequestParameterMap().get("id");
            Long l;
            try {
                l = Long.valueOf(id);
            } catch (NumberFormatException e) {
                l = 0l;
            }
            String j = "select s from Upload s where s.id=:id";
            Map m = new HashMap();
            m.put("id", l);
            Upload temImg = getUploadFacade().findFirstByJpql(j, m);
            if (temImg != null) {
                byte[] imgArr = null;
                try {
                    imgArr = temImg.getBaImage();
                } catch (Exception e) {
                    return new DefaultStreamedContent();
                }
                if (imgArr == null) {
                    return new DefaultStreamedContent();
                }
                
                InputStream targetStream = new ByteArrayInputStream(temImg.getBaImage());
                
                StreamedContent str = DefaultStreamedContent.builder().contentType(temImg.getFileType()).name(temImg.getFileName()).stream(() -> targetStream).build();

                
                return str;
            } else {
                return new DefaultStreamedContent();
            }
        }
    }
    
    public StreamedContent getImageByUpWebContentId() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getRenderResponse()) {
            return new DefaultStreamedContent();
        } else {
            String id = context.getExternalContext().getRequestParameterMap().get("id");
            Long l;
            try {
                l = Long.valueOf(id);
            } catch (NumberFormatException e) {
                l = 0l;
            }
            String j = "select s from Upload s where s.webContent.id=:id";
            Map m = new HashMap();
            m.put("id", l);
            Upload temImg = getUploadFacade().findFirstByJpql(j, m);
            if (temImg != null) {
                byte[] imgArr = null;
                try {
                    imgArr = temImg.getBaImage();
                } catch (Exception e) {
                    return new DefaultStreamedContent();
                }
                if (imgArr == null) {
                    return new DefaultStreamedContent();
                }
                
                InputStream targetStream = new ByteArrayInputStream(temImg.getBaImage());
                
                StreamedContent str = DefaultStreamedContent.builder().contentType(temImg.getFileType()).name(temImg.getFileName()).stream(() -> targetStream).build();

                
                return str;
            } else {
                return new DefaultStreamedContent();
            }
        }
    }
    
    
    
//
//    public StreamedContent imageByCodeInlineWithoutGet(String code) {
//        FacesContext context = FacesContext.getCurrentInstance();
//        if (context.getRenderResponse()) {
//            return new DefaultStreamedContent();
//        } else {
//
//            String id = code;
//            if (id == null) {
//                return new DefaultStreamedContent();
//            }
//            String j = "select s from Upload s where (s.code)=:id";
//            Map m = new HashMap();
//            m.put("id", id.trim().toLowerCase());
//            Upload temImg = getUploadFacade().findFirstByJpql(j, m);
//            if (temImg != null) {
//                byte[] imgArr = null;
//                try {
//                    imgArr = temImg.getBaImage();
//                } catch (Exception e) {
//                    return new DefaultStreamedContent();
//                }
//                if (imgArr == null) {
//                    return new DefaultStreamedContent();
//                }
//                StreamedContent str = new DefaultStreamedContent(new ByteArrayInputStream(imgArr), temImg.getFileName());
//                return str;
//            } else {
//                return new DefaultStreamedContent();
//            }
//        }
//    }

    public StreamedContent imageByCodeById(String strId ){
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getRenderResponse()) {
            return new DefaultStreamedContent();
        } else {

            String id = strId;
            if (id == null) {
                return new DefaultStreamedContent();
            }
            String j = "select s from Upload s where (s.code)=:id";
            Map m = new HashMap();
            m.put("id", id.trim().toLowerCase());
            Upload temImg = getUploadFacade().findFirstByJpql(j, m);
            if (temImg != null) {
                byte[] imgArr = null;
                try {
                    imgArr = temImg.getBaImage();
                } catch (Exception e) {
                    return new DefaultStreamedContent();
                }
                if (imgArr == null) {
                    return new DefaultStreamedContent();
                }
                
                InputStream targetStream = new ByteArrayInputStream(temImg.getBaImage());
                
                StreamedContent str = DefaultStreamedContent.builder().contentType(temImg.getFileType()).name(temImg.getFileName()).stream(() -> targetStream).build();

                return str;
            } else {
                return new DefaultStreamedContent();
            }
        }
    }

//    public String toViewImageByStrId() {
//        FacesContext context = FacesContext.getCurrentInstance();
//        strId = context.getExternalContext().getRequestParameterMap().get("id");
//        return "image";
//    }
//
//    public StreamedContent getImageByPagesStrId() {
//        System.out.println("imageByPagesStrId");
//        System.out.println("strId = " + strId);
//        FacesContext context = FacesContext.getCurrentInstance();
//        if (context.getRenderResponse()) {
//            System.out.println("RenderResponse = " + context.getRenderResponse());
//            return new DefaultStreamedContent();
//        } else {
//            System.out.println("RenderResponse = " + context.getRenderResponse());
//            String id = strId;
//            if (id == null) {
//                return new DefaultStreamedContent();
//            }
//            String j = "select s from Upload s where s.strId=:id";
//            Map m = new HashMap();
//            m.put("id", id);
//            System.out.println("m = " + m);
//            Upload temImg = getUploadFacade().findFirstByJpql(j, m);
//            if (temImg != null) {
//                byte[] imgArr = null;
//                try {
//                    imgArr = temImg.getBaImage();
//                } catch (Exception e) {
//                    return new DefaultStreamedContent();
//                }
//                if (imgArr == null) {
//                    return new DefaultStreamedContent();
//                }
//                StreamedContent str = new DefaultStreamedContent(new ByteArrayInputStream(imgArr), temImg.getFileName());
//                return str;
//            } else {
//                return new DefaultStreamedContent();
//            }
//        }
//    }
//
//    public StreamedContent imageByStrId(String code) {
//        System.out.println("imageByStrId");
//        FacesContext context = FacesContext.getCurrentInstance();
//        if (context.getRenderResponse()) {
//            return new DefaultStreamedContent();
//        } else {
//
//            String id = code;
//            if (id == null) {
//                return new DefaultStreamedContent();
//            }
//            String j = "select s from Upload s where s.strId=:id";
//            Map m = new HashMap();
//            m.put("id", id);
//            System.out.println("m = " + m);
//            Upload temImg = getUploadFacade().findFirstByJpql(j, m);
//            if (temImg != null) {
//                byte[] imgArr = null;
//                try {
//                    imgArr = temImg.getBaImage();
//                } catch (Exception e) {
//                    return new DefaultStreamedContent();
//                }
//                if (imgArr == null) {
//                    return new DefaultStreamedContent();
//                }
//                StreamedContent str = new DefaultStreamedContent(new ByteArrayInputStream(imgArr), temImg.getFileName());
//                return str;
//            } else {
//                return new DefaultStreamedContent();
//            }
//        }
//    }
//
//    public StreamedContent getImageByCodeInline(String code) {
//        FacesContext context = FacesContext.getCurrentInstance();
//        if (context.getRenderResponse()) {
//            return new DefaultStreamedContent();
//        } else {
//
//            String id = code;
//            if (id == null) {
//                return new DefaultStreamedContent();
//            }
//            String j = "select s from Upload s where (s.code)=:id";
//            Map m = new HashMap();
//            m.put("id", id.trim().toLowerCase());
//            Upload temImg = getUploadFacade().findFirstByJpql(j, m);
//            if (temImg != null) {
//                byte[] imgArr = null;
//                try {
//                    imgArr = temImg.getBaImage();
//                } catch (Exception e) {
//                    return new DefaultStreamedContent();
//                }
//                if (imgArr == null) {
//                    return new DefaultStreamedContent();
//                }
//                StreamedContent str = new DefaultStreamedContent(new ByteArrayInputStream(imgArr), temImg.getFileName());
//                return str;
//            } else {
//                return new DefaultStreamedContent();
//            }
//        }
//    }
//
//    public StreamedContent getImageBySterId() {
//        FacesContext context = FacesContext.getCurrentInstance();
//        if (context.getRenderResponse()) {
//            return new DefaultStreamedContent();
//        } else {
//
//            String id = context.getExternalContext().getRequestParameterMap().get("id");
//            System.out.println("id = " + id);
//            if (id == null) {
//                return new DefaultStreamedContent();
//            }
//            String j = "select s from Upload s where s.strId=:id";
//            Map m = new HashMap();
//            m.put("id", id);
//            Upload temImg = getUploadFacade().findFirstByJpql(j, m);
//
//            if (temImg != null) {
//                byte[] imgArr = null;
//                try {
//                    imgArr = temImg.getBaImage();
//                } catch (Exception e) {
//                    return new DefaultStreamedContent();
//                }
//                if (imgArr == null) {
//                    return new DefaultStreamedContent();
//                }
//                StreamedContent str = new DefaultStreamedContent(new ByteArrayInputStream(imgArr), temImg.getFileName());
//                return str;
//            } else {
//                return new DefaultStreamedContent();
//            }
//        }
//    }
//
//    public StreamedContent getImageByCode() {
//        FacesContext context = FacesContext.getCurrentInstance();
//        if (context.getRenderResponse()) {
//            return new DefaultStreamedContent();
//        } else {
//
//            String id = context.getExternalContext().getRequestParameterMap().get("id");
//            if (id == null) {
//                return new DefaultStreamedContent();
//            }
//            String j = "select s from Upload s where (s.code)=:id";
//            Map m = new HashMap();
//            m.put("id", id.trim().toLowerCase());
//            Upload temImg = getUploadFacade().findFirstByJpql(j, m);
//            if (temImg != null) {
//                byte[] imgArr = null;
//                try {
//                    imgArr = temImg.getBaImage();
//                } catch (Exception e) {
//                    return new DefaultStreamedContent();
//                }
//                if (imgArr == null) {
//                    return new DefaultStreamedContent();
//                }
//                StreamedContent str = new DefaultStreamedContent(new ByteArrayInputStream(imgArr), temImg.getFileName());
//                return str;
//            } else {
//                return new DefaultStreamedContent();
//            }
//        }
//    }

   

    public UploadFacade getUploadFacade() {
        return uploadFacade;
    }

    public String getStrId() {
        return strId;
    }

    public void setStrId(String strId) {
        this.strId = strId;
    }

}
