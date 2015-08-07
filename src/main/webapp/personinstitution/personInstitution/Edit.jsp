<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<f:view>
    <html>
        <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
            <title>Editing PersonInstitution</title>
            <link rel="stylesheet" type="text/css" href="/ruhunu/faces/jsfcrud.css" />
        </head>
        <body>
            <h:panelGroup id="messagePanel" layout="block">
                <h:messages errorStyle="color: red" infoStyle="color: green" layout="table"/>
            </h:panelGroup>
            <h1>Editing PersonInstitution</h1>
            <h:form>
                <h:panelGrid columns="2">
                    <h:outputText value="Department:"/>
                    <h:selectOneMenu id="department" value="#{personInstitution.personInstitution.department}" title="Department" >
                        <f:selectItems value="#{department.departmentItemsAvailableSelectOne}"/>
                    </h:selectOneMenu>
                    <h:outputText value="Institution:"/>
                    <h:selectOneMenu id="institution" value="#{personInstitution.personInstitution.institution}" title="Institution" >
                        <f:selectItems value="#{institution.institutionItemsAvailableSelectOne}"/>
                    </h:selectOneMenu>
                    <h:outputText value="Person:"/>
                    <h:selectOneMenu id="person" value="#{personInstitution.personInstitution.person}" title="Person" >
                        <f:selectItems value="#{person.personItemsAvailableSelectOne}"/>
                    </h:selectOneMenu>
                    <h:outputText value="Staff:"/>
                    <h:selectOneMenu id="staff" value="#{personInstitution.personInstitution.staff}" title="Staff" >
                        <f:selectItems value="#{staff.staffItemsAvailableSelectOne}"/>
                    </h:selectOneMenu>
                    <h:outputText value="Type:"/>
                    <h:inputText id="type" value="#{personInstitution.personInstitution.type}" title="Type" />
                    <h:outputText value="Creater:"/>
                    <h:selectOneMenu id="creater" value="#{personInstitution.personInstitution.creater}" title="Creater" >
                        <f:selectItems value="#{webUser.webUserItemsAvailableSelectOne}"/>
                    </h:selectOneMenu>
                    <h:outputText value="CreatedAt (MM/dd/yyyy HH:mm:ss):"/>
                    <h:inputText id="createdAt" value="#{personInstitution.personInstitution.createdAt}" title="CreatedAt" >
                        <f:convertDateTime pattern="MM/dd/yyyy HH:mm:ss" />
                    </h:inputText>
                    <h:outputText value="Retirer:"/>
                    <h:selectOneMenu id="retirer" value="#{personInstitution.personInstitution.retirer}" title="Retirer" >
                        <f:selectItems value="#{webUser.webUserItemsAvailableSelectOne}"/>
                    </h:selectOneMenu>
                    <h:outputText value="RetiredAt (MM/dd/yyyy HH:mm:ss):"/>
                    <h:inputText id="retiredAt" value="#{personInstitution.personInstitution.retiredAt}" title="RetiredAt" >
                        <f:convertDateTime pattern="MM/dd/yyyy HH:mm:ss" />
                    </h:inputText>
                    <h:outputText value="RetireComments:"/>
                    <h:inputText id="retireComments" value="#{personInstitution.personInstitution.retireComments}" title="RetireComments" />
                    <h:outputText value="Id:"/>
                    <h:outputText value="#{personInstitution.personInstitution.id}" title="Id" />

                </h:panelGrid>
                <br />
                <h:commandLink action="#{personInstitution.edit}" value="Save">
                    <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                </h:commandLink>
                <br />
                <br />
                <h:commandLink action="#{personInstitution.detailSetup}" value="Show" immediate="true">
                    <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                </h:commandLink>
                <br />
                <h:commandLink action="#{personInstitution.listSetup}" value="Show All PersonInstitution Items" immediate="true"/>
                <br />
                <br />
                <h:commandLink value="Index" action="welcome" immediate="true" />

            </h:form>
        </body>
    </html>
</f:view>
