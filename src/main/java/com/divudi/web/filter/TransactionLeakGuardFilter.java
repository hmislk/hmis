/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.web.filter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * Detects and clears JTA transactions that are still associated with the HTTP
 * worker thread when a request finishes.
 *
 * <p>
 * Background: on 2026-09-01 Ruhunu production ran for about ten hours with an
 * already-rolled-back JTA transaction bound to the Grizzly worker threads. Every
 * later request that happened to land on such a thread had its first EJB call
 * rejected by the container with
 * {@code TransactionRolledbackLocalException: Client&#39;s transaction aborted},
 * across every module - pharmacy, OPD, collecting centre, lab, even login. At
 * peak roughly one request in three failed. The only cure was a full domain
 * restart, because nothing in the request path ever disassociated the stale
 * transaction from the thread.
 * </p>
 *
 * <p>
 * A servlet container reuses worker threads, so a single leaked transaction is
 * not a single failed request - it silently poisons every subsequent request
 * routed to that thread. This filter closes that window: it runs at the outermost
 * position of the chain, and on the way out of every request it verifies that no
 * transaction remains bound to the thread. If one does, the transaction is
 * suspended (which is what actually disassociates it), rolled back when it is
 * still rollable, and reported at SEVERE with enough context to identify the
 * request that leaked it.
 * </p>
 *
 * <p>
 * Note that setting {@code transaction-service timeout-in-seconds} is <em>not</em>
 * an alternative to this filter. The GlassFish timeout reaper only calls
 * {@code setRollbackOnly()} on an expired transaction; the transaction stays
 * associated with the thread, which is precisely the failure state described
 * above.
 * </p>
 *
 * <p>
 * The guard must never affect the response, so every failure inside it is
 * swallowed and logged. The per-request cost is one {@code ThreadLocal} read.
 * </p>
 */
public class TransactionLeakGuardFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(TransactionLeakGuardFilter.class.getName());

    /** Standard Java EE name for the synchronization registry. */
    private static final String TSR_JNDI = "java:comp/TransactionSynchronizationRegistry";

    /** GlassFish/Payara name for the transaction manager. */
    private static final String TM_JNDI = "java:appserver/TransactionManager";

    /** Total leaks seen since deployment, so the scale is visible in a single log line. */
    private static final AtomicLong LEAK_COUNT = new AtomicLong();

    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private TransactionManager transactionManager;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Looked up once rather than injected: this filter has to work even if
        // CDI/EJB injection into filters is unavailable, and a failed lookup must
        // degrade to "guard disabled", never to "application will not start".
        try {
            InitialContext ctx = new InitialContext();
            transactionSynchronizationRegistry = (TransactionSynchronizationRegistry) ctx.lookup(TSR_JNDI);
            transactionManager = (TransactionManager) ctx.lookup(TM_JNDI);
            LOGGER.info("TransactionLeakGuardFilter active: leaked transactions will be cleared at request end.");
        } catch (NamingException | ClassCastException e) {
            LOGGER.log(Level.WARNING, "TransactionLeakGuardFilter could not resolve the JTA services; "
                    + "leaked-transaction detection is DISABLED for this deployment.", e);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            // On an async dispatch the real work continues on another thread after
            // this returns, so a transaction bound here is not yet a leak.
            if (!request.isAsyncStarted()) {
                clearLeakedTransaction(request);
            }
        }
    }

    /**
     * Verifies that the calling thread carries no transaction, and clears it if it
     * does. Never throws.
     *
     * @param request the request that just finished, used only for reporting
     */
    private void clearLeakedTransaction(ServletRequest request) {
        if (transactionSynchronizationRegistry == null) {
            return;
        }
        try {
            int status = transactionSynchronizationRegistry.getTransactionStatus();
            if (status == Status.STATUS_NO_TRANSACTION) {
                return;
            }

            long count = LEAK_COUNT.incrementAndGet();
            LOGGER.log(Level.SEVERE, "Leaked JTA transaction detected at end of request"
                    + " (status={0}, thread={1}, request={2}, user={3}, totalLeaksSinceDeployment={4})."
                    + " Clearing it so it cannot poison the next request served by this thread.",
                    new Object[]{statusName(status), Thread.currentThread().getName(),
                        describe(request), remoteUser(request), count});

            if (transactionManager == null) {
                LOGGER.severe("No TransactionManager available - the leaked transaction could NOT be cleared."
                        + " Subsequent requests on this thread are likely to fail with"
                        + " a rolled-back client transaction until the domain is restarted.");
                return;
            }

            // suspend() is what actually disassociates the transaction from the
            // thread. rollback() alone would not be enough once it has already
            // completed, and would throw in that state.
            Transaction leaked = transactionManager.suspend();
            if (leaked == null) {
                return;
            }
            int leakedStatus = leaked.getStatus();
            if (leakedStatus == Status.STATUS_ACTIVE || leakedStatus == Status.STATUS_MARKED_ROLLBACK) {
                leaked.rollback();
                LOGGER.log(Level.INFO, "Leaked transaction rolled back (was {0}).", statusName(leakedStatus));
            } else {
                LOGGER.log(Level.INFO, "Leaked transaction was already {0}; detached without rollback.",
                        statusName(leakedStatus));
            }
        } catch (Throwable t) {
            // A guard that breaks responses is worse than the leak it guards against.
            LOGGER.log(Level.SEVERE, "TransactionLeakGuardFilter failed while clearing a leaked transaction", t);
        }
    }

    private String describe(ServletRequest request) {
        if (!(request instanceof HttpServletRequest)) {
            return "non-HTTP request";
        }
        HttpServletRequest http = (HttpServletRequest) request;
        String uri = http.getMethod() + " " + http.getRequestURI();
        String query = http.getQueryString();
        return query == null ? uri : uri + "?" + query;
    }

    private String remoteUser(ServletRequest request) {
        if (!(request instanceof HttpServletRequest)) {
            return "unknown";
        }
        String user = ((HttpServletRequest) request).getRemoteUser();
        return user == null ? "anonymous" : user;
    }

    private String statusName(int status) {
        switch (status) {
            case Status.STATUS_ACTIVE:
                return "ACTIVE";
            case Status.STATUS_MARKED_ROLLBACK:
                return "MARKED_ROLLBACK";
            case Status.STATUS_PREPARED:
                return "PREPARED";
            case Status.STATUS_COMMITTED:
                return "COMMITTED";
            case Status.STATUS_ROLLEDBACK:
                return "ROLLEDBACK";
            case Status.STATUS_UNKNOWN:
                return "UNKNOWN";
            case Status.STATUS_NO_TRANSACTION:
                return "NO_TRANSACTION";
            case Status.STATUS_PREPARING:
                return "PREPARING";
            case Status.STATUS_COMMITTING:
                return "COMMITTING";
            case Status.STATUS_ROLLING_BACK:
                return "ROLLING_BACK";
            default:
                return "status-" + status;
        }
    }

    @Override
    public void destroy() {
        // Nothing to release.
    }
}
