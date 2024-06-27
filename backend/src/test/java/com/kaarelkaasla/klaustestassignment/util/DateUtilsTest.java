package com.kaarelkaasla.klaustestassignment.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class DateUtilsTest {

    private DateUtils dateUtils;
    private SimpleDateFormat dateFormat;

    @BeforeEach
    public void setUp() {
        dateUtils = new DateUtils();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    }

    /**
     * Tests if a valid date string is correctly validated.
     */
    @Test
    public void testIsValidDate_Valid() {
        assertTrue(DateUtils.isValidDate("2023-12-31T23:59:59"));
    }

    /**
     * Tests if an invalid date string is correctly validated.
     */
    @Test
    public void testIsValidDate_Invalid() {
        assertFalse(DateUtils.isValidDate("invalid-date"));
    }

    /**
     * Tests if a valid date string is correctly parsed into a Date object.
     */
    @Test
    public void testParseDate_Valid() throws ParseException {
        Date expectedDate = dateFormat.parse("2023-12-31T23:59:59");
        Date parsedDate = DateUtils.parseDate("2023-12-31T23:59:59");
        assertEquals(expectedDate, parsedDate);
    }

    /**
     * Tests if an invalid date string throws a ParseException.
     */
    @Test
    public void testParseDate_Invalid() {
        assertThrows(ParseException.class, () -> {
            DateUtils.parseDate("invalid-date");
        });
    }

    /**
     * Tests if a Date object is correctly formatted into a date string.
     */
    @Test
    public void testFormatDate() throws ParseException {
        Date date = dateFormat.parse("2023-12-31T23:59:59");
        String formattedDate = DateUtils.formatDate(date);
        assertEquals("2023-12-31T23:59:59", formattedDate);
    }

    /**
     * Tests if a date-time string is correctly parsed into a LocalDateTime object.
     */
    @Test
    public void testParseDateTime() {
        LocalDateTime dateTime = DateUtils.parseDateTime("2023-12-31T23:59:59");
        assertEquals(LocalDateTime.of(2023, 12, 31, 23, 59, 59), dateTime);
    }

    /**
     * Tests if the correct number of days between two dates is calculated.
     */
    @Test
    public void testGetDaysBetween() {
        long days = DateUtils.getDaysBetween("2023-01-01T00:00:00", "2023-01-10T00:00:00");
        assertEquals(9, days);
    }

    /**
     * Tests if the method correctly identifies if two dates are in different months or years.
     */
    @Test
    public void testIsDifferentMonthOrYear() {
        assertTrue(DateUtils.isDifferentMonthOrYear("2023-01-01T00:00:00", "2023-02-01T00:00:00"));
        assertFalse(DateUtils.isDifferentMonthOrYear("2023-01-01T00:00:00", "2023-01-31T00:00:00"));
    }
}
