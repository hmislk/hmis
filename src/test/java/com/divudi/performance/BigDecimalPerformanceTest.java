package com.divudi.performance;

import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.util.BigDecimalUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for BigDecimal null-safe operations.
 * Ensures that the BigDecimalUtil methods and nullable field handling
 * do not introduce significant performance degradation.
 * 
 * @author Dr M H B Ariyaratne
 * @since BigDecimal Refactoring Phase 6
 */
@DisplayName("BigDecimal Performance Tests")
public class BigDecimalPerformanceTest {
    
    private static final int LARGE_DATASET_SIZE = 1000;
    private static final int PERFORMANCE_ITERATIONS = 10;
    private static final int WARMUP_ITERATIONS = 20;
    private static final long PERFORMANCE_THRESHOLD_MS = Long.parseLong(System.getProperty("perf.threshold.ms", "5000"));
    private List<BillItemFinanceDetails> testDataset;
    
    @BeforeEach
    public void setUp() {
        testDataset = createLargeTestDataset();
    }
    
    @Test
    @DisplayName("Null-Safe Addition Performance Test")
    @Timeout(value = 5) // Should complete within 5 seconds
    public void testNullSafeAdditionPerformance_ShouldCompleteQuickly() {
        long startTime = System.currentTimeMillis();
        
        BigDecimal runningTotal = BigDecimal.ZERO;
        
        // Perform many null-safe additions
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            for (BillItemFinanceDetails item : testDataset) {
                runningTotal = BigDecimalUtil.add(runningTotal, item.getQuantity());
                runningTotal = BigDecimalUtil.add(runningTotal, item.getFreeQuantity());
                runningTotal = BigDecimalUtil.add(runningTotal, item.getLineGrossRate());
            }
        }
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // Verify we got a result (not testing correctness, just performance)
        assertNotNull(runningTotal);
        
        // Log performance results
        System.out.printf("Null-safe addition performance: %d operations in %d ms%n", 
            PERFORMANCE_ITERATIONS * testDataset.size() * 3, executionTime);
        
        // Ensure reasonable performance (adjust threshold as needed)
        assertTrue(executionTime < 5000, "Null-safe additions taking too long: " + executionTime + "ms");
    }
    
    @Test
    @DisplayName("Traditional vs Null-Safe Operations Comparison")
    @Timeout(value = 10) // Should complete within 10 seconds  
    public void testTraditionalVsNullSafeComparison_ShouldHaveSimilarPerformance() {
        // Test traditional null checking vs BigDecimalUtil methods
        
        // Traditional approach timing
        long traditionalStartTime = System.currentTimeMillis();
        BigDecimal traditionalTotal = BigDecimal.ZERO;
        
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            for (BillItemFinanceDetails item : testDataset) {
                BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                BigDecimal freeQty = item.getFreeQuantity() != null ? item.getFreeQuantity() : BigDecimal.ZERO;
                traditionalTotal = traditionalTotal.add(qty).add(freeQty);
            }
        }
        
        long traditionalEndTime = System.currentTimeMillis();
        long traditionalTime = traditionalEndTime - traditionalStartTime;
        
        // BigDecimalUtil approach timing
        long utilStartTime = System.currentTimeMillis();
        BigDecimal utilTotal = BigDecimal.ZERO;
        
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            for (BillItemFinanceDetails item : testDataset) {
                utilTotal = BigDecimalUtil.add(utilTotal, item.getQuantity());
                utilTotal = BigDecimalUtil.add(utilTotal, item.getFreeQuantity());
            }
        }
        
        long utilEndTime = System.currentTimeMillis();
        long utilTime = utilEndTime - utilStartTime;
        
        // Results should be identical
        assertEquals(traditionalTotal, utilTotal);
        
        // Log performance comparison
        System.out.printf("Performance comparison:%n");
        System.out.printf("  Traditional approach: %d ms%n", traditionalTime);
        System.out.printf("  BigDecimalUtil approach: %d ms%n", utilTime);
        System.out.printf("  Difference: %d ms (%.2fx)%n", 
            Math.abs(utilTime - traditionalTime),
            Math.max(utilTime, traditionalTime) / (double) Math.min(utilTime, traditionalTime));
        
        // BigDecimalUtil should not be more than 10x slower than traditional approach
        // This accounts for the additional method call overhead for null safety and JVM warm-up variations
        // Handle case where traditionalTime is 0 (too fast to measure)
        if (traditionalTime == 0) {
            // If traditional approach is 0ms, allow BigDecimalUtil to take up to 50ms (reasonable overhead)
            assertTrue(utilTime <= 50, 
                String.format("BigDecimalUtil is too slow: %d ms when traditional approach is unmeasurable", utilTime));
        } else {
            double performanceRatio = (double) utilTime / traditionalTime;
            assertTrue(performanceRatio < 10.0, 
                String.format("BigDecimalUtil is too slow: %.2fx traditional time", performanceRatio));
        }
    }
    
    @Test
    @DisplayName("Large Dataset Financial Calculations Performance")
    @Timeout(value = 15) // Should complete within 15 seconds
    public void testLargeDatasetCalculationsPerformance_ShouldScaleWell() {
        // JVM warm-up iterations
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            performCalculations();
        }
        
        long startTime = System.currentTimeMillis();
        BigDecimal[] results = performCalculations();
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // Verify we got reasonable results
        BigDecimal totalGrossAmount = results[0];
        BigDecimal totalNetAmount = results[1];
        BigDecimal totalDiscounts = results[2];
        BigDecimal totalTaxes = results[3];
        
        assertTrue(BigDecimalUtil.isPositive(totalGrossAmount) || BigDecimalUtil.isNullOrZero(totalGrossAmount));
        assertNotNull(totalNetAmount);
        assertNotNull(totalDiscounts);
        assertNotNull(totalTaxes);
        
        System.out.printf("Large dataset calculations: %d items processed in %d ms%n", 
            testDataset.size(), executionTime);
        System.out.printf("  Average time per item: %.2f ms%n", 
            (double) executionTime / testDataset.size());
        
        // Should process large datasets efficiently (configurable threshold)
        assertTrue(executionTime < PERFORMANCE_THRESHOLD_MS, 
            String.format("Large dataset processing too slow: %dms (threshold: %dms)", executionTime, PERFORMANCE_THRESHOLD_MS));
        
        // Should process at least 100 items per second
        double itemsPerSecond = (testDataset.size() * 1000.0) / executionTime;
        assertTrue(itemsPerSecond > 100, 
            String.format("Processing rate too slow: %.1f items/second", itemsPerSecond));
    }
    
    @Test
    @DisplayName("Memory Usage Test for Nullable Fields")
    @Timeout(value = 10)
    public void testMemoryUsage_ShouldNotLeakMemory() {
        // Test that nullable fields don't cause memory leaks
        long startMemory = getUsedMemory();
        
        List<BillItemFinanceDetails> largeList = new ArrayList<>();
        
        // Create many entities with mixed null/non-null values
        for (int i = 0; i < LARGE_DATASET_SIZE * 2; i++) {
            BillItemFinanceDetails item = new BillItemFinanceDetails();
            
            // Mix of null and non-null values to test memory behavior
            if (i % 3 == 0) {
                item.setQuantity(null);
                item.setLineGrossRate(new BigDecimal("10.00"));
            } else if (i % 3 == 1) {
                item.setQuantity(new BigDecimal("5.00"));
                item.setLineGrossRate(null);
            } else {
                item.setQuantity(new BigDecimal("3.00"));
                item.setLineGrossRate(new BigDecimal("15.00"));
            }
            
            largeList.add(item);
        }
        
        // Perform calculations to ensure objects are used
        BigDecimal total = BigDecimal.ZERO;
        for (BillItemFinanceDetails item : largeList) {
            total = BigDecimalUtil.add(total, 
                BigDecimalUtil.multiply(item.getQuantity(), item.getLineGrossRate()));
        }
        
        // Clear references and suggest GC
        largeList.clear();
        largeList = null;
        System.gc();
        Thread.yield();
        
        long endMemory = getUsedMemory();
        long memoryUsed = endMemory - startMemory;
        
        System.out.printf("Memory usage test: %d KB used for %d entities%n", 
            memoryUsed / 1024, LARGE_DATASET_SIZE * 2);
        
        // Memory usage should be reasonable (adjust threshold as needed)
        long maxExpectedMemory = LARGE_DATASET_SIZE * 2 * 1024; // 1KB per entity max
        assertTrue(memoryUsed < maxExpectedMemory, 
            String.format("Memory usage too high: %d KB", memoryUsed / 1024));
    }
    
    @Test
    @DisplayName("Concurrent Access Performance Test")
    @Timeout(value = 15)
    public void testConcurrentAccessPerformance_ShouldBeThreadSafe() throws InterruptedException {
        final int threadCount = 2;
        final int operationsPerThread = Math.max(1, PERFORMANCE_ITERATIONS / threadCount);
        
        Thread[] threads = new Thread[threadCount];
        final BigDecimal[] results = new BigDecimal[threadCount];
        
        long startTime = System.currentTimeMillis();
        
        // Create multiple threads performing BigDecimalUtil operations
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                BigDecimal threadTotal = BigDecimal.ZERO;
                
                for (int j = 0; j < operationsPerThread; j++) {
                    for (BillItemFinanceDetails item : testDataset) {
                        threadTotal = BigDecimalUtil.add(threadTotal, item.getQuantity());
                        threadTotal = BigDecimalUtil.multiply(threadTotal, BigDecimal.valueOf(1.01));
                    }
                }
                
                results[threadIndex] = threadTotal;
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for completion
        for (Thread thread : threads) {
            thread.join();
        }
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // Verify all threads completed successfully
        for (BigDecimal result : results) {
            assertNotNull(result);
        }
        
        System.out.printf("Concurrent performance: %d threads completed in %d ms%n", 
            threadCount, executionTime);
        
        // Should complete within reasonable time
        assertTrue(executionTime < 15000, "Concurrent operations too slow: " + executionTime + "ms");
    }
    
    // Helper methods
    
    private List<BillItemFinanceDetails> createLargeTestDataset() {
        List<BillItemFinanceDetails> dataset = new ArrayList<>();
        
        for (int i = 0; i < LARGE_DATASET_SIZE; i++) {
            BillItemFinanceDetails item = new BillItemFinanceDetails();
            
            // Create mix of null and non-null values to simulate real-world data
            switch (i % 4) {
                case 0: // Complete data
                    item.setQuantity(new BigDecimal("" + (i + 1)));
                    item.setFreeQuantity(new BigDecimal("1.00"));
                    item.setLineGrossRate(new BigDecimal("10.50"));
                    item.setLineDiscountRate(new BigDecimal("5.00"));
                    item.setBillTaxRate(new BigDecimal("8.00"));
                    break;
                case 1: // Partial data
                    item.setQuantity(new BigDecimal("" + (i + 1)));
                    item.setFreeQuantity(null);
                    item.setLineGrossRate(new BigDecimal("15.75"));
                    item.setLineDiscountRate(null);
                    item.setBillTaxRate(new BigDecimal("10.00"));
                    break;
                case 2: // Minimal data
                    item.setQuantity(new BigDecimal("" + (i + 1)));
                    item.setFreeQuantity(null);
                    item.setLineGrossRate(new BigDecimal("20.00"));
                    item.setLineDiscountRate(null);
                    item.setBillTaxRate(null);
                    break;
                case 3: // Mixed nulls
                    item.setQuantity(null);
                    item.setFreeQuantity(new BigDecimal("2.00"));
                    item.setLineGrossRate(null);
                    item.setLineDiscountRate(new BigDecimal("3.00"));
                    item.setBillTaxRate(null);
                    break;
            }
            
            dataset.add(item);
        }
        
        return dataset;
    }
    
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * Performs financial calculations on the test dataset for performance measurement.
     * @return Array containing [totalGrossAmount, totalNetAmount, totalDiscounts, totalTaxes]
     */
    private BigDecimal[] performCalculations() {
        BigDecimal totalGrossAmount = BigDecimal.ZERO;
        BigDecimal totalNetAmount = BigDecimal.ZERO;
        BigDecimal totalDiscounts = BigDecimal.ZERO;
        BigDecimal totalTaxes = BigDecimal.ZERO;
        
        for (BillItemFinanceDetails item : testDataset) {
            // Simulate financial calculations
            BigDecimal quantity = BigDecimalUtil.valueOrZero(item.getQuantity());
            BigDecimal rate = BigDecimalUtil.valueOrZero(item.getLineGrossRate());
            BigDecimal grossAmount = BigDecimalUtil.multiply(quantity, rate);
            
            BigDecimal discountRate = BigDecimalUtil.valueOrZero(item.getLineDiscountRate());
            BigDecimal discountAmount = BigDecimalUtil.multiply(grossAmount, 
                discountRate.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN));
            
            BigDecimal netAmount = BigDecimalUtil.subtract(grossAmount, discountAmount);
            
            BigDecimal taxRate = BigDecimalUtil.valueOrZero(item.getBillTaxRate());
            BigDecimal taxAmount = BigDecimalUtil.multiply(netAmount,
                taxRate.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN));
            
            // Aggregate totals
            totalGrossAmount = BigDecimalUtil.add(totalGrossAmount, grossAmount);
            totalNetAmount = BigDecimalUtil.add(totalNetAmount, netAmount);
            totalDiscounts = BigDecimalUtil.add(totalDiscounts, discountAmount);
            totalTaxes = BigDecimalUtil.add(totalTaxes, taxAmount);
        }
        
        return new BigDecimal[]{totalGrossAmount, totalNetAmount, totalDiscounts, totalTaxes};
    }
}