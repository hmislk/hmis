package com.divudi.service;

import com.divudi.ejb.PharmacyBean;
import com.divudi.service.pharmacy.DirectIssueBatchService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Coordinates the deploy-time warmup chain that pre-pays the first-click tax
 * on the BHT pharmacy issue page (Issue #20138).
 *
 * Runs on a {@link ManagedExecutorService} thread spawned from
 * {@code @PostConstruct}. EJB {@code @Asynchronous} cannot be used here
 * because the bean is still in its activation chain when {@code @PostConstruct}
 * fires, so the asynchronous proxy is not yet installed and the call would
 * run synchronously on the deployment thread, blocking deploy completion.
 *
 * The chain runs three steps in order, each cheap on its own:
 *
 *   1. service-bean activation — touches DirectIssueBatchService, PharmacyBean,
 *      and BillService so their transitive @EJB dependency graphs (30+ beans)
 *      are wired before the first user transaction. Empirically removed ~1s
 *      of first-Settle tax per service touched.
 *
 *   2. write-path warmup ({@link WritePathWarmupService}) — persist+flush+rollback
 *      a dummy Bill / BillItem / etc., compiling the InsertObjectQuery
 *      descriptors EclipseLink would otherwise compile inline on first save.
 *
 *   3. working-set warmup ({@link WorkingSetWarmupService}) — bulk-load every
 *      pharmacy Stock + ItemBatch + Item row with stock>0 into the L2 cache,
 *      so the first Settle on a never-before-touched item is cache-hot.
 *      This is the dominant fix; the others mop up secondary effects.
 *
 * Earlier revisions also did per-class {@code em.find()} read-touches and
 * dry-runs of 16 settle-path JPQL aggregates and 4 facade reads. Logs proved
 * those did not affect first-Settle timings (descriptors and JPQL plans are
 * already global once any user query runs); they have been removed to keep
 * deploy time short. The working-set load implicitly warms ItemBatch / Item /
 * Stock descriptors as a side-effect of materialising real rows.
 */
@Singleton
@Startup
public class EntityWarmupService {

    private static final Logger LOGGER = Logger.getLogger(EntityWarmupService.class.getName());

    @EJB
    private DirectIssueBatchService directIssueBatchService;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillService billService;

    @EJB
    private WritePathWarmupService writePathWarmupService;
    @EJB
    private WorkingSetWarmupService workingSetWarmupService;

    @PostConstruct
    public void init() {
        // Spawn a plain daemon thread so deploy completes immediately. Cannot
        // use @Asynchronous here because @PostConstruct runs before the
        // container installs the async proxy on this same bean — the call
        // would resolve synchronously and block the deploy thread (the very
        // bug this method is structured to avoid).
        Thread t = new Thread(this::runWarmupChain, "EntityWarmupService-startup");
        t.setDaemon(true);
        t.start();
    }

    private void runWarmupChain() {
        try {
            long total = System.currentTimeMillis();

            long serviceStart = System.currentTimeMillis();
            warmupServices();
            LOGGER.log(Level.INFO, "[EntityWarmup] service bean activation TOTAL: {0}ms",
                    System.currentTimeMillis() - serviceStart);

            if (writePathWarmupService != null) {
                try {
                    writePathWarmupService.warmWritePath();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "[EntityWarmup] write-path warmup failed: " + e.getMessage());
                }
            }

            // Working-set warmup — bulk-load active pharmacy Stock rows into
            // L2 via FetchGroup (id + stock only, no @ManyToOne cascade).
            // This is the dominant fix for the cold first-Settle tax. Issue
            // #20138.
            if (workingSetWarmupService != null) {
                try {
                    workingSetWarmupService.warmActiveStockWorkingSet();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "[EntityWarmup] working-set warmup failed: " + e.getMessage());
                }
            }

            LOGGER.log(Level.INFO, "[EntityWarmup] FULL CHAIN TOTAL: {0}ms",
                    System.currentTimeMillis() - total);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[EntityWarmup] chain failed: " + e.getMessage(), e);
        }
    }

    private void warmupServices() {
        touchService("DirectIssueBatchService", () -> directIssueBatchService.validateBillForSettlement(null));
        touchService("PharmacyBean", () -> {
            if (pharmacyBean != null) {
                pharmacyBean.getBillNumberBean();
            }
            return null;
        });
        touchService("BillService", () -> billService.fetchFirstBill());
    }

    private void touchService(String name, java.util.concurrent.Callable<Object> call) {
        try {
            long t = System.currentTimeMillis();
            call.call();
            LOGGER.log(Level.INFO, "[EntityWarmup] service {0}: {1}ms",
                    new Object[]{name, System.currentTimeMillis() - t});
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[EntityWarmup] service " + name + " skipped: " + e.getMessage());
        }
    }
}
