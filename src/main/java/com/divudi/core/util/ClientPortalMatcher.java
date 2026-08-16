package com.divudi.core.util;

import com.divudi.core.entity.Patient;
import java.util.List;

public class ClientPortalMatcher {

    public enum MatchResult {
        NO_MATCH,
        SINGLE_MATCH,
        MULTIPLE_MATCH
    }

    private ClientPortalMatcher() {
    }

    public static MatchResult classify(List<Patient> matches) {
        if (matches == null || matches.isEmpty()) {
            return MatchResult.NO_MATCH;
        }
        if (matches.size() == 1) {
            return MatchResult.SINGLE_MATCH;
        }
        return MatchResult.MULTIPLE_MATCH;
    }
}
