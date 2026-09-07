
package com.divudi.ws.channel;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author Chinthaka Prasad
 */
@Provider
public class CorsResponseFilter implements ContainerResponseFilter{
    
     @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {

        responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
        responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        responseContext.getHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization, Finance");
        responseContext.getHeaders().add("Access-Control-Max-Age", "3600");
        // Access-Control-Allow-Credentials is intentionally omitted: the REST API
        // authenticates via the custom Finance header (API key), not cookies. Sending
        // Allow-Credentials: true alongside Allow-Origin: * is invalid per the CORS
        // spec and was rejected by browsers anyway (issue #19867).
    }
}
