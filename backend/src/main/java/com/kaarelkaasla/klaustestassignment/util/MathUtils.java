package com.kaarelkaasla.klaustestassignment.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for various math operations.
 */
public class MathUtils {

    /**
     * Rounds the given value to two decimal places.
     *
     * @param value
     *            The value to round.
     *
     * @return The value rounded to two decimal places.
     */
    public static double roundToTwoDecimalPlaces(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
