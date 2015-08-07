<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<f:view>
    <html>
        <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
            <title>Listing PersonInstitution Items</title>
            <link rel="stylesheet" type="text/css" href="/ruhunu/faces/jsfcrud.css" />
        </head>
        <body>
            <h:panelGroup id="messagePanel" layout="block">
                <h:messages errorStyle="color: red" infoStyle="color: green" layout="table"/>
            </h:panelGroup>
            <h1>Listing PersonInstitution Items</h1>
            <h:form styleClass="jsfcrud_list_form">
                <h:outputText escape="false" value="(No PersonInstitution Items Found)<br />" rendered="#{personInstitution.pagingInfo.itemCount == 0}" />
                <h:panelGroup rendered="#{personInstitution.pagingInfo.itemCount > 0}">
                    <h:outputText value="Item #{personInstitution.pagingInfo.firstItem + 1}..#{personInstitution.pagingInfo.lastItem} of #{personInstitution.pagingInfo.itemCount}"/>&nbsp;
                    <h:commandLink action="#{personInstitution.prev}" value="Previous #{personInstitution.pagingInfo.batchSize}" rendered="#{personInstitution.pagingInfo.firstItem >= personInstitution.pagingInfo.batchSize}"/>&nbsp;
                    <h:commandLink action="#{personInstitution.next}" value="Next #{personInstitution.pagingInfo.batchSize}" rendered="#{personInstitution.pagingInfo.lastItem + personInstitution.pagingInfo.batchSize <= personInstitution.pagingInfo.itemCount}"/>&nbsp;
                    <h:commandLink action="#{personInstitution.next}" value="Remaining #{personInstitution.pagingInfo.itemCount - personInstitution.pagingInfo.lastItem}"
                                   rendered="#{personInstitution.pagingInfo.lastItem < personInstitution.pagingInfo.itemCount && personInstitution.pagingInfo.lastItem + personInstitution.pagingInfo.batchSize > personInstitution.pagingInfo.itemCount}"/>
                    <h:dataTable value="#{personInstitution.personInstitutionItems}" var="item" border="0" cellpadding="2" cellspacing="0" rowClasses="jsfcrud_odd_row,jsfcrud_even_row" rules="all" style="border:solid 1px">
                        <h:column>
                            <f:facet name="header">
                                <h:outputText value="Department"/>
                            </f:facet>
                            <h:outputText value="#{item.department}"/>
                        </h:column>
                        <h:column>
                            <f:facet name="header">
                                <h:outputText value="Institution"/>
                            </f:facet>
                            <h:outputText value="#{item.institution}"/>
                        </h:column>
                        <h:column>
                            <f:facet name="header">
                                <h:outputText value="Person"/>
                            </f:facet>
                            <h:outputText value="#{item.person}"/>
                        </h:column>
                        <h:column>
                            <f:facet name="header">
                                <h:outputText value="Staff"/>
                            </f:facet>
                            <h:outputText value="#{item.staff}"/>
                        </h:column>
                        <h:column>
                            <f:facet name="header">
                                <h:outputText value="Type"/>
                            </f:facet>
                            <h:outputText value="#{item.type}"/>
                        </h:column>
                        <h:column>
                            <f:facet name="header">
                                <h:outputText value="Creater"/>
                            </f:facet>
                            <h:outputText value="#{item.creater}"/>
                        </h:column>
                        <h:column>
                            <f:facet name="header">
                                <h:outputText value="CreatedAt"/>
                            </f:facet>
                            <h:outputText value="#{item.createdAt}">
                                <f:convertDateTime pattern="MM/dd/yyyy HH:mm:ss" />
                            </h:outputText>
                        </h:column>
                        <h:column>
                            <f:facet name="header">
                                <h:outputText value="Retirer"/>
                            </f:facet>
                            <h:outputText value="#{item.retirer}"/>
                        </h:column>
                        <h:column>
                            <f:facet name="header">
                                <h:outputText value="RetiredAt"/>
                            </f:facet>
                            <h:outputText value="#{item.retiredAt}">
                                <f:convertDateTime pattern="MM/dd/yyyy HH:mm:ss" />
                            </h:outputText>
                        </h:column>
                        <h:column>
                            <f:facet name="header">
                                <h:outputText value="RetireComments"/>
                            </f:facet>
                            <h:outputText value="#{item.retireComments}"/>
                        </h:column>
                        <h:column>
                            <f:facet name="header">
                                <h:outputText value="Id"/>
                            </f:facet>
                            <h:outputText value="#{item.id}"/>
                        </h:column>
                        <h:column>
                            <f:facet name="header">
                                <h:outputText escape="false" value="&nbsp;"/>
                            </f:facet>
                            <h:commandLink value="Show" action="#{personInstitution.detailSetup}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][item][personInstitution.converter].jsfcrud_invoke}"/>
                            </h:commandLink>
                            <h:outputText value=" "/>
                            <h:commandLink value="Edit" action="#{personInstitution.editSetup}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][item][personInstitution.converter].jsfcrud_invoke}"/>
                            </h:commandLink>
                            <h:outputText value=" "/>
                            <h:commandLink value="Destroy" action="#{personInstitution.remove}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][item][personInstitution.converter].jsfcrud_invoke}"/>
                            </h:commandLink>
                        </h:column>

                    </h:dataTable>
                </h:panelGroup>
                <br />
                <h:commandLink action="#{personInstitution.createSetup}" value="New PersonInstitution"/>
                <br />
                <br />
                <h:commandLink value="Index" action="welcome" immediate="true" />


            </h:form>
        </body>
    </html>
</f:view>
