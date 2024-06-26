package com.kaarelkaasla.klaustestassignment.util;

import com.kaarelkaasla.klaustestassignment.entity.RatingCategory;
import com.kaarelkaasla.klaustestassignment.repository.RatingCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class RatingUtilsTest {

    @Mock
    private RatingCategoryRepository ratingCategoryRepository;

    private RatingUtils ratingUtils;
    private SimpleDateFormat dateFormat;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ratingUtils = new RatingUtils(ratingCategoryRepository);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    }

    /**
     * Tests if a valid date string is correctly validated.
     */
    @Test
    public void testIsValidDate_Valid() {
        assertTrue(ratingUtils.isValidDate("2023-12-31T23:59:59"));
    }

    /**
     * Tests if an invalid date string is correctly validated.
     */
    @Test
    public void testIsValidDate_Invalid() {
        assertFalse(ratingUtils.isValidDate("invalid-date"));
    }

    /**
     * Tests if a valid date string is correctly parsed into a Date object.
     */
    @Test
    public void testParseDate_Valid() throws ParseException {
        Date expectedDate = dateFormat.parse("2023-12-31T23:59:59");
        Date parsedDate = ratingUtils.parseDate("2023-12-31T23:59:59");
        assertEquals(expectedDate, parsedDate);
    }

    /**
     * Tests if an invalid date string throws a ParseException.
     */
    @Test
    public void testParseDate_Invalid() {
        assertThrows(ParseException.class, () -> {
            ratingUtils.parseDate("invalid-date");
        });
    }

    /**
     * Tests if a Date object is correctly formatted into a date string.
     */
    @Test
    public void testFormatDate() throws ParseException {
        Date date = dateFormat.parse("2023-12-31T23:59:59");
        String formattedDate = ratingUtils.formatDate(date);
        assertEquals("2023-12-31T23:59:59", formattedDate);
    }

    /**
     * Tests if the correct number of days between two dates is calculated.
     */
    @Test
    public void testGetDaysBetween() {
        long days = ratingUtils.getDaysBetween("2023-01-01T00:00:00", "2023-01-10T00:00:00");
        assertEquals(9, days);
    }

    /**
     * Tests if the method correctly identifies if two dates are in different months or years.
     */
    @Test
    public void testIsDifferentMonthOrYear() {
        assertTrue(ratingUtils.isDifferentMonthOrYear("2023-01-01T00:00:00", "2023-02-01T00:00:00"));
        assertFalse(ratingUtils.isDifferentMonthOrYear("2023-01-01T00:00:00", "2023-01-31T00:00:00"));
    }

    /**
     * Tests if the method correctly retrieves a map of category IDs to category names.
     */
    @Test
    public void testGetCategoryIdToNameMap() {
        List<RatingCategory> ratingCategories = Arrays.asList(new RatingCategory(1L, "Category 1", 1.0),
                new RatingCategory(2L, "Category 2", 1.0));

        when(ratingCategoryRepository.findAll()).thenReturn(ratingCategories);

        Map<Long, String> categoryIdToNameMap = ratingUtils.getCategoryIdToNameMap();

        assertEquals(2, categoryIdToNameMap.size());
        assertEquals("Category 1", categoryIdToNameMap.get(1L));
        assertEquals("Category 2", categoryIdToNameMap.get(2L));
    }
}
