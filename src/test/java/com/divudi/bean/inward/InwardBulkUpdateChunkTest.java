package com.divudi.bean.inward;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The id chunking used by the Create Final Bill bulk updates (#17562). */
class InwardBulkUpdateChunkTest {

    @Test
    void emptyAndNullGiveNoChunks() {
        assertTrue(InwardBeanController.chunkIds(null, 500).isEmpty());
        assertTrue(InwardBeanController.chunkIds(new ArrayList<Long>(), 500).isEmpty());
    }

    @Test
    void smallListIsOneChunk() {
        List<List<Long>> chunks = InwardBeanController.chunkIds(Arrays.asList(1L, 2L, 3L), 500);
        assertEquals(1, chunks.size());
        assertEquals(Arrays.asList(1L, 2L, 3L), chunks.get(0));
    }

    @Test
    void largeListSplitsWithoutLosingOrDuplicatingIds() {
        List<Long> ids = new ArrayList<>();
        for (long i = 1; i <= 1203; i++) {
            ids.add(i);
        }
        List<List<Long>> chunks = InwardBeanController.chunkIds(ids, 500);
        assertEquals(3, chunks.size());
        assertEquals(500, chunks.get(0).size());
        assertEquals(500, chunks.get(1).size());
        assertEquals(203, chunks.get(2).size());
        List<Long> joined = new ArrayList<>();
        chunks.forEach(joined::addAll);
        assertEquals(ids, joined);
    }

    @Test
    void exactMultipleHasNoEmptyTail() {
        List<Long> ids = new ArrayList<>();
        for (long i = 0; i < 1000; i++) {
            ids.add(i);
        }
        assertEquals(2, InwardBeanController.chunkIds(ids, 500).size());
    }

    @Test
    void sizeBelowOneIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> InwardBeanController.chunkIds(Arrays.asList(1L), 0));
    }
}
