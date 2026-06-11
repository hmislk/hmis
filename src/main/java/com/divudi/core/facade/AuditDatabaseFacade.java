/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.Item;
import java.sql.Connection;
import java.util.List;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import org.eclipse.persistence.internal.jpa.EntityManagerImpl;
import org.eclipse.persistence.sessions.JNDIConnector;
import org.eclipse.persistence.sessions.server.ServerSession;

/**
 * Facade for audit database operations including schema management.
 * This facade connects to the audit database for database administration tasks.
 *
 * @author buddhika
 */
@Stateless
public class AuditDatabaseFacade extends AbstractFacade<Item> {

    @PersistenceContext(unitName = "hmisAuditPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}
        return em;
    }

    public AuditDatabaseFacade() {
        super(Item.class);
    }

    /**
     * Temporary measure while this GenerationType.AUTO build (sequence-table
     * IDs) and the newer GenerationType.IDENTITY build (AUTO_INCREMENT IDs)
     * share the audit database (REPORTLOG, AUDITEVENT, ...). Separates the two
     * allocators into disjoint ID ranges so they stop colliding on the same
     * primary keys. THIS application server must be restarted after running —
     * it holds a preallocated sequence block in memory. See {@link IdAllocatorSeparation}.
     *
     * @return human-readable report of what was changed
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<String> separateIdAllocatorsForDualVersionOperation() throws Exception {
        Connection conn = getRawJdbcConnection();
        try {
            conn.setAutoCommit(true);
            return IdAllocatorSeparation.separate(conn);
        } finally {
            // Restore autoCommit=false before returning connection to Payara's JTA pool.
            try { conn.setAutoCommit(false); } catch (Exception ignored) { }
            conn.close();
        }
    }

    /**
     * Obtain a raw JDBC connection from EclipseLink's JNDI datasource,
     * completely outside JTA. ALTER TABLE causes MySQL implicit commits that
     * desync the JTA transaction manager, so DDL must bypass it. Caller is
     * responsible for closing the connection.
     */
    private Connection getRawJdbcConnection() throws Exception {
        EntityManagerImpl emImpl = em.unwrap(EntityManagerImpl.class);
        ServerSession serverSession = emImpl.getServerSession();
        DataSource ds = ((JNDIConnector) serverSession.getLogin().getConnector()).getDataSource();
        return ds.getConnection();
    }
}