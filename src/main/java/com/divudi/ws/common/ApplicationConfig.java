/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.common;

import java.util.Set;
import javax.ws.rs.core.Application;

/**
 *
 * @author Archmage-Dushan - Modified 
 */
@javax.ws.rs.ApplicationPath("api")
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();
        // Use Jackson 2.x JAXB-aware provider and fail fast if not found
        try {
            Class<?> jacksonProvider = Class.forName("com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider");
            resources.add(jacksonProvider);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Jackson JSON provider not found", ex);
        }
        addRestResourceClasses(resources);
        return resources;
    }

    /**
     * Do not modify addRestResourceClasses() method.
     * It is automatically populated with
     * all resources defined in the project.
     * If required, comment out calling this method in getClasses().
     */
    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(com.divudi.ws.channel.ChannelApi.class);
        resources.add(com.divudi.ws.channel.CorsResponseFilter.class);
        resources.add(com.divudi.ws.common.ApiMembership.class);
        resources.add(com.divudi.ws.common.ConfigResource.class);
        resources.add(com.divudi.ws.fhir.Fhir.class);
        resources.add(com.divudi.ws.finance.CostingData.class);
        resources.add(com.divudi.ws.finance.Finance.class);
        resources.add(com.divudi.ws.finance.Qb.class);
        resources.add(com.divudi.ws.finance.clinical.Fhir.class);
        resources.add(com.divudi.ws.inward.ApiInward.class);
        resources.add(com.divudi.ws.lims.Lims.class);
        resources.add(com.divudi.ws.lims.LimsMiddlewareController.class);
        resources.add(com.divudi.ws.lims.MiddlewareController.class);
    }
    
}