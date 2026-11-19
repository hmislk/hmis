package com.divudi.web.filter;

import com.divudi.bean.common.TwoFactorAuthController;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Server-side hold for a user who must set up two-factor login before using
 * anything else (see TwoFactorAuthController#isSetupRequiredNow). While the
 * hold lasts, every page request (any /faces/ path or .xhtml) except the
 * set-up page, the login page, the logout page and page resources is turned
 * away, whatever template the page uses and whether it is a normal or an
 * AJAX request. If the check itself fails, the request is turned away too
 * (the hold fails closed). The templates also show only the set-up screen.
 */
public class TwoFactorSetupGateFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(TwoFactorSetupGateFilter.class.getName());
    private static final String SETUP_PAGE = "/user_authenticator.xhtml";

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        if (request.getSession(false) == null || !isPageRequest(request) || isAllowed(pathOf(request)) || !isHeld()) {
            chain.doFilter(req, res);
            return;
        }
        String facesRequest = request.getHeader("Faces-Request");
        if (facesRequest != null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/faces" + SETUP_PAGE);
    }

    private static String pathOf(HttpServletRequest request) {
        String path = request.getServletPath() + (request.getPathInfo() == null ? "" : request.getPathInfo());
        return path.startsWith("/faces/") ? path.substring("/faces".length()) : path;
    }

    private static boolean isPageRequest(HttpServletRequest request) {
        String path = request.getServletPath() + (request.getPathInfo() == null ? "" : request.getPathInfo());
        return path.startsWith("/faces/") || path.endsWith(".xhtml");
    }

    private static boolean isAllowed(String path) {
        return SETUP_PAGE.equals(path) || "/index.xhtml".equals(path) || "/logout.xhtml".equals(path)
                || path.startsWith("/javax.faces.resource/") || path.startsWith("/resources/");
    }

    private static boolean isHeld() {
        try {
            return CDI.current().select(TwoFactorAuthController.class).get().isSetupRequiredNow();
        } catch (RuntimeException e) {
            // Fail closed: a check that cannot be made is treated as "held"
            LOGGER.log(Level.SEVERE, "Two-factor set-up gate check failed; request turned away", e);
            return true;
        }
    }

    @Override
    public void destroy() {
    }
}
