package com.divudi.bean.clinical;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Template placeholder values on the OPD visit documents: typed text stays text,
 * line breaks built by the generator stay line breaks.
 */
public class PatientEncounterCardTextTest {

    private static String cardText(Class<?> c, String s) throws Exception {
        Method m = c.getDeclaredMethod("cardText", String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, s);
    }

    private static String br(Class<?> c) throws Exception {
        Field f = c.getDeclaredField("CARD_BR");
        f.setAccessible(true);
        return (String) f.get(null);
    }

    @Test
    void typedTagsStayText() throws Exception {
        for (Class<?> c : new Class<?>[]{PatientEncounterController.class, PastPatientEncounterController.class}) {
            assertEquals("&lt;b&gt;x&lt;/b&gt; &lt;img src=x&gt; O&#39;Neil &amp; co",
                    cardText(c, "<b>x</b> <img src=x> O'Neil & co"));
        }
    }

    @Test
    void lineBreakMarkersBecomeBr() throws Exception {
        for (Class<?> c : new Class<?>[]{PatientEncounterController.class, PastPatientEncounterController.class}) {
            assertEquals("Rx<br/>A &lt;1&gt;<br/>", cardText(c, "Rx" + br(c) + "A <1>" + br(c)));
            assertEquals("BP - 120&nbsp;", cardText(c, "BP - 120 "));
            assertEquals("", cardText(c, null));
        }
    }
}
