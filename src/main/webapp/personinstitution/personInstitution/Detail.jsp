<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<f:view>
    <html>
        <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
            <title>PersonInstitution Detail</title>
            <link rel="stylesheet" type="text/css" href="/ruhunu/faces/jsfcrud.css" />
        </head>
        <body>
            <h:panelGroup id="messagePanel" layout="block">
                <h:messages errorStyle="color: red" infoStyle="color: green" layout="table"/>
            </h:panelGroup>
            <h1>PersonInstitution Detail</h1>
            <h:form>
                <h:panelGrid columns="2">
                    <h:outputText value="Department:"/>
                    <h:panelGroup>
                        <h:outputText value="#{personInstitution.personInstitution.department}"/>
                        <h:panelGroup rendered="#{personInstitution.personInstitution.department != null}">
                            <h:outputText value=" ("/>
                            <h:commandLink value="Show" action="#{department.detailSetup}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.currentDepartment" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution.department][department.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.relatedController" value="personInstitution"/>
                                <f:param name="jsfcrud.relatedControllerType" value="com.divudi.bean.common.PersonInstitutionController"/>
                            </h:commandLink>
                            <h:outputText value=" "/>
                            <h:commandLink value="Edit" action="#{department.editSetup}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.currentDepartment" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution.department][department.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.relatedController" value="personInstitution"/>
                                <f:param name="jsfcrud.relatedControllerType" value="com.divudi.bean.common.PersonInstitutionController"/>
                            </h:commandLink>
                            <h:outputText value=" "/>
                            <h:commandLink value="Destroy" action="#{department.destroy}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.currentDepartment" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution.department][department.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.relatedController" value="personInstitution"/>
                                <f:param name="jsfcrud.relatedControllerType" value="com.divudi.bean.common.PersonInstitutionController"/>
                            </h:commandLink>
                            <h:outputText value=" )"/>
                        </h:panelGroup>
                    </h:panelGroup>
                    <h:outputText value="Institution:"/>
                    <h:panelGroup>
                        <h:outputText value="#{personInstitution.personInstitution.institution}"/>
                        <h:panelGroup rendered="#{personInstitution.personInstitution.institution != null}">
                            <h:outputText value=" ("/>
                            <h:commandLink value="Show" action="#{institution.detailSetup}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.currentInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution.institution][institution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.relatedController" value="personInstitution"/>
                                <f:param name="jsfcrud.relatedControllerType" value="com.divudi.bean.common.PersonInstitutionController"/>
                            </h:commandLink>
                            <h:outputText value=" "/>
                            <h:commandLink value="Edit" action="#{institution.editSetup}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.currentInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution.institution][institution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.relatedController" value="personInstitution"/>
                                <f:param name="jsfcrud.relatedControllerType" value="com.divudi.bean.common.PersonInstitutionController"/>
                            </h:commandLink>
                            <h:outputText value=" "/>
                            <h:commandLink value="Destroy" action="#{institution.destroy}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.currentInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution.institution][institution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.relatedController" value="personInstitution"/>
                                <f:param name="jsfcrud.relatedControllerType" value="com.divudi.bean.common.PersonInstitutionController"/>
                            </h:commandLink>
                            <h:outputText value=" )"/>
                        </h:panelGroup>
                    </h:panelGroup>
                    <h:outputText value="Person:"/>
                    <h:panelGroup>
                        <h:outputText value="#{personInstitution.personInstitution.person}"/>
                        <h:panelGroup rendered="#{personInstitution.personInstitution.person != null}">
                            <h:outputText value=" ("/>
                            <h:commandLink value="Show" action="#{person.detailSetup}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.currentPerson" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution.person][person.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.relatedController" value="personInstitution"/>
                                <f:param name="jsfcrud.relatedControllerType" value="com.divudi.bean.common.PersonInstitutionController"/>
                            </h:commandLink>
                            <h:outputText value=" "/>
                            <h:commandLink value="Edit" action="#{person.editSetup}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.currentPerson" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution.person][person.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.relatedController" value="personInstitution"/>
                                <f:param name="jsfcrud.relatedControllerType" value="com.divudi.bean.common.PersonInstitutionController"/>
                            </h:commandLink>
                            <h:outputText value=" "/>
                            <h:commandLink value="Destroy" action="#{person.destroy}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.currentPerson" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution.person][person.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.relatedController" value="personInstitution"/>
                                <f:param name="jsfcrud.relatedControllerType" value="com.divudi.bean.common.PersonInstitutionController"/>
                            </h:commandLink>
                            <h:outputText value=" )"/>
                        </h:panelGroup>
                    </h:panelGroup>
                    <h:outputText value="Staff:"/>
                    <h:panelGroup>
                        <h:outputText value="#{personInstitution.personInstitution.staff}"/>
                        <h:panelGroup rendered="#{personInstitution.personInstitution.staff != null}">
                            <h:outputText value=" ("/>
                            <h:commandLink value="Show" action="#{staff.detailSetup}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.currentStaff" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution.staff][staff.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.relatedController" value="personInstitution"/>
                                <f:param name="jsfcrud.relatedControllerType" value="com.divudi.bean.common.PersonInstitutionController"/>
                            </h:commandLink>
                            <h:outputText value=" "/>
                            <h:commandLink value="Edit" action="#{staff.editSetup}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.currentStaff" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution.staff][staff.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.relatedController" value="personInstitution"/>
                                <f:param name="jsfcrud.relatedControllerType" value="com.divudi.bean.common.PersonInstitutionController"/>
                            </h:commandLink>
                            <h:outputText value=" "/>
                            <h:commandLink value="Destroy" action="#{staff.destroy}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.currentStaff" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution.staff][staff.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.relatedController" value="personInstitution"/>
                                <f:param name="jsfcrud.relatedControllerType" value="com.divudi.bean.common.PersonInstitutionController"/>
                            </h:commandLink>
                            <h:outputText value=" )"/>
                        </h:panelGroup>
                    </h:panelGroup>
                    <h:outputText value="Type:"/>
                    <h:outputText value="#{personInstitution.personInstitution.type}" title="Type" />
                    <h:outputText value="Creater:"/>
                    <h:panelGroup>
                        <h:outputText value="#{personInstitution.personInstitution.creater}"/>
                        <h:panelGroup rendered="#{personInstitution.personInstitution.creater != null}">
                            <h:outputText value=" ("/>
                            <h:commandLink value="Show" action="#{webUser.detailSetup}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.currentWebUser" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution.creater][webUser.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.relatedController" value="personInstitution"/>
                                <f:param name="jsfcrud.relatedControllerType" value="com.divudi.bean.common.PersonInstitutionController"/>
                            </h:commandLink>
                            <h:outputText value=" "/>
                            <h:commandLink value="Edit" action="#{webUser.editSetup}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.currentWebUser" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution.creater][webUser.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.relatedController" value="personInstitution"/>
                                <f:param name="jsfcrud.relatedControllerType" value="com.divudi.bean.common.PersonInstitutionController"/>
                            </h:commandLink>
                            <h:outputText value=" "/>
                            <h:commandLink value="Destroy" action="#{webUser.destroy}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.currentWebUser" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution.creater][webUser.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.relatedController" value="personInstitution"/>
                                <f:param name="jsfcrud.relatedControllerType" value="com.divudi.bean.common.PersonInstitutionController"/>
                            </h:commandLink>
                            <h:outputText value=" )"/>
                        </h:panelGroup>
                    </h:panelGroup>
                    <h:outputText value="CreatedAt:"/>
                    <h:outputText value="#{personInstitution.personInstitution.createdAt}" title="CreatedAt" >
                        <f:convertDateTime pattern="MM/dd/yyyy HH:mm:ss" />
                    </h:outputText>
                    <h:outputText value="Retirer:"/>
                    <h:panelGroup>
                        <h:outputText value="#{personInstitution.personInstitution.retirer}"/>
                        <h:panelGroup rendered="#{personInstitution.personInstitution.retirer != null}">
                            <h:outputText value=" ("/>
                            <h:commandLink value="Show" action="#{webUser.detailSetup}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.currentWebUser" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution.retirer][webUser.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.relatedController" value="personInstitution"/>
                                <f:param name="jsfcrud.relatedControllerType" value="com.divudi.bean.common.PersonInstitutionController"/>
                            </h:commandLink>
                            <h:outputText value=" "/>
                            <h:commandLink value="Edit" action="#{webUser.editSetup}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.currentWebUser" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution.retirer][webUser.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.relatedController" value="personInstitution"/>
                                <f:param name="jsfcrud.relatedControllerType" value="com.divudi.bean.common.PersonInstitutionController"/>
                            </h:commandLink>
                            <h:outputText value=" "/>
                            <h:commandLink value="Destroy" action="#{webUser.destroy}">
                                <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.currentWebUser" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution.retirer][webUser.converter].jsfcrud_invoke}"/>
                                <f:param name="jsfcrud.relatedController" value="personInstitution"/>
                                <f:param name="jsfcrud.relatedControllerType" value="com.divudi.bean.common.PersonInstitutionController"/>
                            </h:commandLink>
                            <h:outputText value=" )"/>
                        </h:panelGroup>
                    </h:panelGroup>
                    <h:outputText value="RetiredAt:"/>
                    <h:outputText value="#{personInstitution.personInstitution.retiredAt}" title="RetiredAt" >
                        <f:convertDateTime pattern="MM/dd/yyyy HH:mm:ss" />
                    </h:outputText>
                    <h:outputText value="RetireComments:"/>
                    <h:outputText value="#{personInstitution.personInstitution.retireComments}" title="RetireComments" />
                    <h:outputText value="Id:"/>
                    <h:outputText value="#{personInstitution.personInstitution.id}" title="Id" />


                </h:panelGrid>
                <br />
                <h:commandLink action="#{personInstitution.remove}" value="Destroy">
                    <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}" />
                </h:commandLink>
                <br />
                <br />
                <h:commandLink action="#{personInstitution.editSetup}" value="Edit">
                    <f:param name="jsfcrud.currentPersonInstitution" value="#{jsfcrud_class['com.divudi.bean.common.util.JsfUtil'].jsfcrud_method['getAsConvertedString'][personInstitution.personInstitution][personInstitution.converter].jsfcrud_invoke}" />
                </h:commandLink>
                <br />
                <h:commandLink action="#{personInstitution.createSetup}" value="New PersonInstitution" />
                <br />
                <h:commandLink action="#{personInstitution.listSetup}" value="Show All PersonInstitution Items"/>
                <br />
                <br />
                <h:commandLink value="Index" action="welcome" immediate="true" />

            </h:form>
        </body>
    </html>
</f:view>
