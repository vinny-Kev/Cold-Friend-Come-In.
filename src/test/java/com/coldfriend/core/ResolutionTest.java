package com.coldfriend.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ResolutionTest {

    @Test
    void parsesWidthByHeight() {
        assertEquals(new Resolution(1280, 720), Resolution.parse("1280x720"));
    }

    @Test
    void rejectsExtraPartsAndEmptyParts() {
        assertThrows(IllegalArgumentException.class, () -> Resolution.parse("1280x720x"));
        assertThrows(IllegalArgumentException.class, () -> Resolution.parse("1280x"));
        assertThrows(IllegalArgumentException.class, () -> Resolution.parse("x720"));
    }

    @Test
    void rejectsNonNumbersAndNonPositiveSizes() {
        assertThrows(IllegalArgumentException.class, () -> Resolution.parse("abc"));
        assertThrows(IllegalArgumentException.class, () -> Resolution.parse("0x5"));
    }
}
