/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.sap;

/**
 * One item line from the SAP MM material document OData response.
 * Maps to A_MaterialDocItem entity from API_MATERIAL_DOCUMENT_SRV.
 */
public class SapMaterialDocumentDTO {

    private String MaterialDocument;
    private String MaterialDocumentItem;
    private String Material;
    private String Plant;
    private String StorageLocation;
    private String Quantity;
    private String BaseUnit;
    private String PostingDate;
    private String MaterialDocumentYear;

    public SapMaterialDocumentDTO() {
    }

    public String getMaterialDocument() { return MaterialDocument; }
    public void setMaterialDocument(String materialDocument) { MaterialDocument = materialDocument; }
    public String getMaterialDocumentItem() { return MaterialDocumentItem; }
    public void setMaterialDocumentItem(String materialDocumentItem) { MaterialDocumentItem = materialDocumentItem; }
    public String getMaterial() { return Material; }
    public void setMaterial(String material) { Material = material; }
    public String getPlant() { return Plant; }
    public void setPlant(String plant) { Plant = plant; }
    public String getStorageLocation() { return StorageLocation; }
    public void setStorageLocation(String storageLocation) { StorageLocation = storageLocation; }
    public String getQuantity() { return Quantity; }
    public void setQuantity(String quantity) { Quantity = quantity; }
    public String getBaseUnit() { return BaseUnit; }
    public void setBaseUnit(String baseUnit) { BaseUnit = baseUnit; }
    public String getPostingDate() { return PostingDate; }
    public void setPostingDate(String postingDate) { PostingDate = postingDate; }
    public String getMaterialDocumentYear() { return MaterialDocumentYear; }
    public void setMaterialDocumentYear(String materialDocumentYear) { MaterialDocumentYear = materialDocumentYear; }
}
