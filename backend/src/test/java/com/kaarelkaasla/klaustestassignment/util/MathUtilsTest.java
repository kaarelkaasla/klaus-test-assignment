package com.kaarelkaasla.klaustestassignment.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MathUtils class.
 */
public class MathUtilsTest {

    /**
     * Tests if the value is correctly rounded to two decimal places.
     */
    @Test
    public void testRoundToTwoDecimalPlaces() {
        assertEquals(123.46, MathUtils.roundToTwoDecimalPlaces(123.456));
        assertEquals(123.45, MathUtils.roundToTwoDecimalPlaces(123.454));
        assertEquals(0.00, MathUtils.roundToTwoDecimalPlaces(0));
        assertEquals(-123.46, MathUtils.roundToTwoDecimalPlaces(-123.456));
    }
}
