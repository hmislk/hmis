package com.divudi.bean.common;

import com.divudi.core.entity.Institution;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers issue #24347 - when no PHN can be generated because the Point of Issue
 * is missing or not 4 characters, staff get a message naming the setting
 * instead of a silent save without a PHN.
 *
 * <p>No config controller is injected here, so the defaults apply: version V2,
 * scope Global, empty 'PHN POI Number', which falls back to the institution's
 * point of issue. Generating a valid PHN needs the database and is not covered.</p>
 */
class ApplicationControllerPhnPoiProblemTest {

    private static Institution institutionWithPoi(String poi) {
        Institution ins = new Institution();
        ins.setPointOfIssueNo(poi);
        return ins;
    }

    @Test
    void missingPoiNamesTheSetting() {
        String problem = new ApplicationController().phnPoiProblem(institutionWithPoi(null));
        assertNotNull(problem);
        assertTrue(problem.contains("'PHN POI Number'"));
        assertTrue(problem.contains("no Point of Issue"));
    }

    @Test
    void poiOfWrongLengthIsReported() {
        String problem = new ApplicationController().phnPoiProblem(institutionWithPoi("AB"));
        assertNotNull(problem);
        assertTrue(problem.contains("exactly 4 characters"));
    }

    @Test
    void fourCharacterPoiIsFine() {
        assertNull(new ApplicationController().phnPoiProblem(institutionWithPoi("RH01")));
    }

    @Test
    void noInstitutionMeansNoMessage() {
        assertNull(new ApplicationController().phnPoiProblem(null));
    }

    @Test
    void quietGenerationWithoutPoiReturnsNull() {
        assertNull(new ApplicationController().createNewPersonalHealthNumberQuietly(institutionWithPoi("")));
    }
}
