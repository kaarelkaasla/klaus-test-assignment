package com.kaarelkaasla.klaustestassignment.util;

import com.kaarelkaasla.klaustestassignment.entity.RatingCategory;
import com.kaarelkaasla.klaustestassignment.repository.RatingCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RatingCategoryUtils class.
 */
public class RatingCategoryUtilsTest {

    @Mock
    private RatingCategoryRepository ratingCategoryRepository;

    private RatingCategoryUtils ratingCategoryUtils;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ratingCategoryUtils = new RatingCategoryUtils(ratingCategoryRepository);
    }

    /**
     * Tests if the method correctly retrieves a map of category IDs to category names.
     */
    @Test
    public void testGetCategoryIdToNameMap() {
        List<RatingCategory> ratingCategories = Arrays.asList(new RatingCategory(1L, "Category 1", 1.0),
                new RatingCategory(2L, "Category 2", 1.0));

        when(ratingCategoryRepository.findAll()).thenReturn(ratingCategories);

        Map<Long, String> categoryIdToNameMap = ratingCategoryUtils.getCategoryIdToNameMap();

        assertEquals(2, categoryIdToNameMap.size());
        assertEquals("Category 1", categoryIdToNameMap.get(1L));
        assertEquals("Category 2", categoryIdToNameMap.get(2L));
    }
}
