package com.divudi.core.monitoring;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Singleton;

/**
 * Performance monitoring infrastructure for stock verification upload operations.
 * Provides detailed timing metrics and structured logging for performance analysis.
 *
 * Created as part of the stock verification upload performance optimization initiative
 * to provide visibility into bottlenecks and optimization effectiveness.
 */
@Singleton
public class StockVerificationMetrics implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(StockVerificationMetrics.class.getName());

    /**
     * Performance timer for measuring operation durations and throughput.
     * Provides structured logging with consistent format for performance analysis.
     */
    public static class PerformanceTimer {
        private long startTime;
        private String operation;
        private String jobId;

        /**
         * Start a new performance timer for the given operation.
         * Logs the operation start time for tracking.
         */
        public static PerformanceTimer start(String jobId, String operation) {
            PerformanceTimer timer = new PerformanceTimer();
            timer.jobId = jobId;
            timer.operation = operation;
            timer.startTime = System.currentTimeMillis();

            LOGGER.log(Level.INFO, "[StockVerification-{0}] {1} started",
                      new Object[]{jobId, operation});
            return timer;
        }

        /**
         * Log completion of the operation with performance metrics.
         * Calculates duration, throughput (records per second), and logs structured results.
         *
         * @param recordCount Number of records processed during this operation
         * @return Duration in milliseconds for further analysis
         */
        public long logCompletion(int recordCount) {
            long duration = System.currentTimeMillis() - startTime;
            double recordsPerSecond = recordCount > 0 ? recordCount / (duration / 1000.0) : 0;

            LOGGER.log(Level.INFO,
                "[StockVerification-{0}] {1} completed in {2}ms. " +
                "Records: {3}, Rate: {4} records/sec",
                new Object[]{jobId, operation, duration, recordCount,
                           String.format("%.2f", recordsPerSecond)});

            return duration;
        }

        /**
         * Log intermediate step progress for long-running operations.
         * Useful for tracking progress through multiple phases of processing.
         */
        public void logStep(String stepName, int processedSoFar) {
            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.log(Level.FINE, "[StockVerification-{0}] {1} - {2}: {3} processed in {4}ms",
                      new Object[]{jobId, operation, stepName, processedSoFar, elapsed});
        }

        /**
         * Get elapsed time without logging completion.
         * Useful for conditional logging or intermediate measurements.
         */
        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }

        /**
         * Log performance warning if operation takes longer than expected.
         * Helps identify operations that may need further optimization.
         */
        public void logSlowOperation(long thresholdMs, int recordCount) {
            long duration = getElapsedTime();
            if (duration > thresholdMs) {
                LOGGER.log(Level.WARNING,
                    "[StockVerification-{0}] SLOW OPERATION: {1} took {2}ms (threshold: {3}ms) " +
                    "for {4} records",
                    new Object[]{jobId, operation, duration, thresholdMs, recordCount});
            }
        }

        // Getters for programmatic access
        public String getJobId() { return jobId; }
        public String getOperation() { return operation; }
        public long getStartTime() { return startTime; }
    }

    /**
     * Metrics for algorithm optimization analysis.
     * Compares before/after performance to validate optimization effectiveness.
     */
    public static class OptimizationMetrics {
        private long originalDuration;
        private long optimizedDuration;
        private int recordCount;
        private String operation;

        public OptimizationMetrics(String operation) {
            this.operation = operation;
        }

        public void setOriginalDuration(long duration) {
            this.originalDuration = duration;
        }

        public void setOptimizedDuration(long duration) {
            this.optimizedDuration = duration;
        }

        public void setRecordCount(int count) {
            this.recordCount = count;
        }

        /**
         * Log optimization improvement analysis.
         * Calculates percentage improvement and throughput gains.
         */
        public void logOptimizationResults(String jobId) {
            if (originalDuration == 0) {
                LOGGER.log(Level.WARNING, "[StockVerification-{0}] Cannot calculate optimization: original duration is 0", jobId);
                return;
            }

            double improvementPercent = ((double)(originalDuration - optimizedDuration) / originalDuration) * 100;
            double originalRate = recordCount > 0 ? recordCount / (originalDuration / 1000.0) : 0;
            double optimizedRate = recordCount > 0 ? recordCount / (optimizedDuration / 1000.0) : 0;
            double throughputGain = optimizedRate - originalRate;

            LOGGER.log(Level.INFO,
                "[StockVerification-{0}] OPTIMIZATION RESULTS for {1}: " +
                "Original: {2}ms ({3} rec/sec), Optimized: {4}ms ({5} rec/sec), " +
                "Improvement: {6}% faster, Throughput gain: +{7} rec/sec",
                new Object[]{jobId, operation, originalDuration,
                           String.format("%.2f", originalRate), optimizedDuration,
                           String.format("%.2f", optimizedRate),
                           String.format("%.2f", improvementPercent),
                           String.format("%.2f", throughputGain)});
        }
    }

    /**
     * Memory usage tracking for large dataset processing.
     * Helps identify memory bottlenecks and validate memory optimizations.
     */
    public static class MemoryMetrics {
        private long initialMemory;
        private long peakMemory;
        private String operation;
        private String jobId;

        public static MemoryMetrics start(String jobId, String operation) {
            MemoryMetrics metrics = new MemoryMetrics();
            metrics.jobId = jobId;
            metrics.operation = operation;

            // Force garbage collection for accurate baseline
            System.gc();
            Runtime runtime = Runtime.getRuntime();
            metrics.initialMemory = runtime.totalMemory() - runtime.freeMemory();
            metrics.peakMemory = metrics.initialMemory;

            LOGGER.log(Level.FINE, "[StockVerification-{0}] Memory tracking started for {1}. " +
                      "Initial memory: {2} MB",
                      new Object[]{jobId, operation, metrics.initialMemory / (1024 * 1024)});

            return metrics;
        }

        /**
         * Sample current memory usage and update peak if necessary.
         * Should be called periodically during processing.
         */
        public void sampleMemoryUsage() {
            Runtime runtime = Runtime.getRuntime();
            long currentMemory = runtime.totalMemory() - runtime.freeMemory();
            if (currentMemory > peakMemory) {
                peakMemory = currentMemory;
            }
        }

        /**
         * Log final memory usage statistics.
         * Includes peak usage and memory efficiency metrics.
         */
        public void logMemoryResults(int recordCount) {
            sampleMemoryUsage(); // Final sample

            long memoryGrowth = peakMemory - initialMemory;
            double memoryPerRecord = recordCount > 0 ? (double)memoryGrowth / recordCount : 0;

            LOGGER.log(Level.INFO,
                "[StockVerification-{0}] MEMORY USAGE for {1}: " +
                "Initial: {2} MB, Peak: {3} MB, Growth: {4} MB, " +
                "Memory per record: {5} KB",
                new Object[]{jobId, operation,
                           initialMemory / (1024 * 1024),
                           peakMemory / (1024 * 1024),
                           memoryGrowth / (1024 * 1024),
                           String.format("%.2f", memoryPerRecord / 1024)});
        }
    }

    /**
     * Log system information for performance analysis context.
     * Helps correlate performance results with system capabilities.
     */
    public void logSystemInfo(String jobId) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        int processors = runtime.availableProcessors();

        LOGGER.log(Level.INFO,
            "[StockVerification-{0}] SYSTEM INFO: " +
            "Processors: {1}, Max memory: {2} MB, Total memory: {3} MB, " +
            "Free memory: {4} MB, Used memory: {5} MB",
            new Object[]{jobId, processors,
                       maxMemory / (1024 * 1024),
                       totalMemory / (1024 * 1024),
                       freeMemory / (1024 * 1024),
                       (totalMemory - freeMemory) / (1024 * 1024)});
    }

    /**
     * Log database connection pool information if available.
     * Helps identify database-related performance bottlenecks.
     */
    public void logDatabaseInfo(String jobId) {
        // This would typically integrate with the application's connection pool monitoring
        // For now, log that database monitoring is available
        LOGGER.log(Level.FINE,
                  "[StockVerification-{0}] Database connection monitoring placeholder", jobId);
    }
}