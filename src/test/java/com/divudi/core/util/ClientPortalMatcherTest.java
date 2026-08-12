package com.divudi.core.util;

import com.divudi.core.entity.Patient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ClientPortalMatcherTest {

    @Test
    public void testNullListIsNoMatch() {
        assertEquals(ClientPortalMatcher.MatchResult.NO_MATCH, ClientPortalMatcher.classify(null));
    }

    @Test
    public void testEmptyListIsNoMatch() {
        List<Patient> matches = new ArrayList<>();
        assertEquals(ClientPortalMatcher.MatchResult.NO_MATCH, ClientPortalMatcher.classify(matches));
    }

    @Test
    public void testSingleEntryIsSingleMatch() {
        List<Patient> matches = Arrays.asList(new Patient());
        assertEquals(ClientPortalMatcher.MatchResult.SINGLE_MATCH, ClientPortalMatcher.classify(matches));
    }

    @Test
    public void testMultipleEntriesIsMultipleMatch() {
        List<Patient> matches = Arrays.asList(new Patient(), new Patient(), new Patient());
        assertEquals(ClientPortalMatcher.MatchResult.MULTIPLE_MATCH, ClientPortalMatcher.classify(matches));
    }
}
