# Comprehensive UI Guidelines for HMIS ERP

## Template Structure

### ✅ Preferred: HTML Template Structure
```xhtml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
      xmlns:p="http://primefaces.org/ui"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:ph="http://xmlns.jcp.org/jsf/composite/pharmacy">
    <h:body>
        <ui:composition template="/resources/template/template.xhtml">
            <ui:define name="content">
                <!-- Page content here -->
            </ui:define>
        </ui:composition>
    </h:body>
</html>
```

### ❌ Avoid: Composition Template Structure
```xhtml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE composition PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<ui:composition xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
                template="/resources/template/template.xhtml"
                xmlns:h="http://xmlns.jcp.org/jsf/html"
                xmlns:p="http://primefaces.org/ui"
                xmlns:f="http://xmlns.jcp.org/jsf/core">
```

**Reason**: HTML template structure provides better browser compatibility and clearer document structure.

## Text and Label Components

### ✅ Use PrimeFaces Components
```xhtml
<!-- For labels -->
<p:outputLabel value="Supplier Name" for="supplierInput" class="form-label fw-bold text-secondary"/>

<!-- For text display -->
<h:outputText value="#{bean.property}" />

<!-- For styled text with formatting -->
<h:outputText value="#{bean.amount}">
    <f:convertNumber pattern="#,##0.00" />
</h:outputText>
```

### ❌ Avoid HTML Elements
```xhtml
<!-- Don't use these -->
<label>Supplier Name</label>
<p>Some text content</p>
<span>Display text</span>
```

**Reason**: PrimeFaces components integrate better with JSF lifecycle and theming.

## Headers and Typography

### ✅ ERP-Appropriate Headers
```xhtml
<!-- Use panel headers for sections -->
<p:panel header="Purchase Order Approval">
    <!-- Content -->
</p:panel>

<!-- Use facet headers for emphasis -->
<p:panel>
    <f:facet name="header">
        <i class="fas fa-check-circle text-success me-2"></i>
        <h:outputText value="Approve Purchase Orders" class="text-success"/>
    </f:facet>
</p:panel>

<!-- Use panelGrid facet for table-like headers -->
<p:panelGrid columns="2">
    <f:facet name="header">
        <h:outputText value="Search Criteria"/>
    </f:facet>
    <!-- Grid content -->
</p:panelGrid>
```

### ❌ Avoid Web-Style Headers
```xhtml
<!-- Don't use these in ERP -->
<h1>Page Title</h1>
<h2>Section Header</h2>
<h3>Subsection</h3>
```

**Reason**: ERP systems use panels and components for structure, not web-style headers.

## Panel Optimization

### ✅ Direct PanelGrid Usage
```xhtml
<!-- When you only need a grid with header -->
<p:panelGrid columns="2" styleClass="mt-3">
    <f:facet name="header">
        <h:outputText value="Order Details"/>
    </f:facet>
    
    <p:outputLabel value="Order Date:" class="fw-bold"/>
    <h:outputText value="#{order.orderDate}"/>
    
    <p:outputLabel value="Supplier:" class="fw-bold"/>
    <h:outputText value="#{order.supplier.name}"/>
</p:panelGrid>
```

### ❌ Avoid Unnecessary Panel Wrapper
```xhtml
<!-- Don't do this -->
<p:panel>
    <p:panelGrid columns="2">
        <f:facet name="header">
            <h:outputText value="Order Details"/>
        </f:facet>
        <!-- Content -->
    </p:panelGrid>
</p:panel>
```

**Reason**: Eliminates unnecessary nesting and uses panelGrid's built-in header capability.

## DataTable Guidelines

### ✅ Clean DataTable Structure
```xhtml
<p:dataTable 
    value="#{controller.items}" 
    var="item"
    styleClass="table-striped"
    sortMode="multiple">
    
    <!-- Regular columns - no icons -->
    <p:column headerText="Item Name" sortBy="#{item.name}">
        <h:outputText value="#{item.name}"/>
    </p:column>
    
    <!-- Numeric columns - right aligned -->
    <p:column headerText="Amount" sortBy="#{item.amount}" styleClass="text-end">
        <h:outputText value="#{item.amount}">
            <f:convertNumber pattern="#,##0.00"/>
        </h:outputText>
    </p:column>
    
    <!-- Status column -->
    <p:column headerText="Status">
        <span class="badge bg-success">#{item.status}</span>
    </p:column>
    
    <!-- Single action column -->
    <p:column headerText="Actions" styleClass="text-center" exportable="false">
        <p:commandButton 
            class="ui-button-success" 
            icon="fas fa-edit" 
            value="Edit"
            action="#{controller.edit(item)}"/>
    </p:column>
</p:dataTable>
```

### ❌ Avoid These Patterns
```xhtml
<!-- Don't add icons in regular columns -->
<p:column headerText="Supplier">
    <i class="fas fa-building"></i>
    <h:outputText value="#{item.supplier}"/>
</p:column>

<!-- Don't create multiple action buttons for same page -->
<p:column headerText="Actions">
    <p:commandButton value="View" action="#{controller.view}"/>
    <p:commandButton value="Edit" action="#{controller.view}"/>
    <p:commandButton value="Details" action="#{controller.view}"/>
</p:column>

<!-- Don't left-align numeric values -->
<p:column headerText="Amount">
    <h:outputText value="#{item.amount}"/>
</p:column>
```

## CSS and Bootstrap Usage

### ✅ Bootstrap for Layout and Spacing
```xhtml
<!-- Grid system -->
<div class="row">
    <div class="col-lg-3 col-md-4">
        <!-- Sidebar -->
    </div>
    <div class="col-lg-9 col-md-8">
        <!-- Main content -->
    </div>
</div>

<!-- Spacing utilities -->
<div class="mb-3">
    <div class="d-flex justify-content-between align-items-center">
        <!-- Content -->
    </div>
</div>

<!-- Text utilities -->
<h:outputText value="#{item.amount}" class="text-end fw-bold"/>
```

### ✅ PrimeFaces for Components
```xhtml
<!-- Button styling -->
<p:commandButton class="ui-button-success"/>
<p:commandButton class="ui-button-info"/>
<p:commandButton class="ui-button-warning"/>
<p:commandButton class="ui-button-danger"/>

<!-- Table styling -->
<p:dataTable styleClass="table-striped"/>
```

### ❌ Avoid Custom CSS
```xhtml
<!-- Don't write custom CSS -->
<style>
.my-custom-button {
    background-color: #007bff;
    padding: 10px;
}
</style>

<!-- Don't use Bootstrap button classes on PrimeFaces -->
<p:commandButton class="btn btn-primary"/>
```

## Icons Usage

### ✅ Strategic Icon Placement
```xhtml
<!-- In form labels (preserve space) -->
<div class="mb-3">
    <i class="fas fa-calendar-alt me-1"></i>
    <p:outputLabel value="Date" class="form-label fw-bold text-secondary"/>
</div>

<!-- In action buttons -->
<p:commandButton 
    icon="fas fa-check" 
    class="ui-button-success" 
    value="Approve"/>

<!-- In panel headers -->
<f:facet name="header">
    <i class="fas fa-shopping-cart text-primary me-2"></i>
    <h:outputText value="Purchase Orders"/>
</f:facet>
```

### ❌ Avoid Icon Overuse
```xhtml
<!-- Don't add icons in every datatable cell -->
<p:column headerText="Name">
    <i class="fas fa-user"></i>
    <h:outputText value="#{item.name}"/>
</p:column>
```

## Complete Page Example

```xhtml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
      xmlns:p="http://primefaces.org/ui"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:f="http://xmlns.jcp.org/jsf/core">
    <h:body>
        <ui:composition template="/resources/template/template.xhtml">
            <ui:define name="content">
                <h:form>
                    <!-- Page Header -->
                    <p:panel styleClass="border-0">
                        <f:facet name="header">
                            <i class="fas fa-check-circle text-success me-2"></i>
                            <h:outputText value="Purchase Order Approval" class="text-success"/>
                        </f:facet>
                        
                        <div class="row">
                            <!-- Search Sidebar -->
                            <div class="col-lg-3 col-md-4">
                                <p:panel header="Search Filters">
                                    <div class="mb-3">
                                        <i class="fas fa-calendar-alt me-1"></i>
                                        <p:outputLabel value="From Date" class="form-label fw-bold text-secondary" for="fromDate"/>
                                        <p:calendar 
                                            id="fromDate" 
                                            value="#{controller.fromDate}" 
                                            styleClass="w-100"/>
                                    </div>
                                    
                                    <p:commandButton 
                                        class="ui-button-info w-100" 
                                        icon="fas fa-search" 
                                        value="Search" 
                                        action="#{controller.search}"/>
                                </p:panel>
                            </div>
                            
                            <!-- Main Content -->
                            <div class="col-lg-9 col-md-8">
                                <p:dataTable 
                                    value="#{controller.orders}" 
                                    var="order"
                                    styleClass="table-striped"
                                    sortMode="multiple">
                                    
                                    <p:column headerText="Order Date" sortBy="#{order.date}">
                                        <h:outputText value="#{order.date}">
                                            <f:convertDateTime pattern="dd/MM/yyyy"/>
                                        </h:outputText>
                                    </p:column>
                                    
                                    <p:column headerText="Supplier">
                                        <h:outputText value="#{order.supplier.name}"/>
                                    </p:column>
                                    
                                    <p:column headerText="Amount" styleClass="text-end" sortBy="#{order.amount}">
                                        <h:outputText value="#{order.amount}">
                                            <f:convertNumber pattern="#,##0.00"/>
                                        </h:outputText>
                                    </p:column>
                                    
                                    <p:column headerText="Actions" styleClass="text-center" exportable="false">
                                        <p:commandButton 
                                            class="ui-button-success" 
                                            icon="fas fa-check" 
                                            value="Approve"
                                            action="#{controller.approve(order)}"/>
                                    </p:column>
                                </p:dataTable>
                            </div>
                        </div>
                    </p:panel>
                </h:form>
            </ui:define>
        </ui:composition>
    </h:body>
</html>
```

## Key Principles

1. **ERP-Focused**: Use panel-based structure, not web-style headers
2. **Component Consistency**: PrimeFaces for interactive elements, Bootstrap for layout
3. **Minimal CSS**: Rely on framework classes, avoid custom styling
4. **Clean DataTables**: No unnecessary icons, single action column, right-aligned numbers
5. **Template Structure**: Use HTML template format for better compatibility
6. **Label Standards**: Use p:outputLabel and h:outputText consistently
7. **Panel Optimization**: Avoid unnecessary nesting, use panelGrid headers
8. **Strategic Icons**: Use sparingly in labels and headers, not in every cell

---
This behavior should persist across all Claude Code sessions for this project.