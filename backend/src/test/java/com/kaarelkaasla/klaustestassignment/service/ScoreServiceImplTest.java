package com.kaarelkaasla.klaustestassignment.service;

import com.kaarelkaasla.klaustestassignment.entity.RatingCategory;
import com.kaarelkaasla.klaustestassignment.repository.RatingCategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ScoreServiceImpl class.
 */
public class ScoreServiceImplTest {

    @Mock
    private RatingCategoryRepository ratingCategoryRepository;

    @InjectMocks
    private ScoreServiceImpl scoreService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void tearDown() {
        Mockito.reset(ratingCategoryRepository);
    }

    /**
     * Tests that calculateScore throws an exception when ratings map is null.
     */
    @Test
    public void testCalculateScore_NullRatings() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            scoreService.calculateScore(null);
        });

        assertEquals("Ratings map must not be null or empty", thrown.getMessage());
    }

    /**
     * Tests that calculateScore throws an exception when ratings map is empty.
     */
    @Test
    public void testCalculateScore_EmptyRatings() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            scoreService.calculateScore(Collections.emptyMap());
        });

        assertEquals("Ratings map must not be null or empty", thrown.getMessage());
    }

    /**
     * Tests that calculateScore throws an exception when database retrieval fails.
     */
    @Test
    public void testCalculateScore_DatabaseError() {
        when(ratingCategoryRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            scoreService.calculateScore(Collections.singletonMap("Category 1", 3));
        });

        assertEquals("Failed to retrieve rating categories from the database", thrown.getMessage());
    }

    /**
     * Tests that calculateScore throws an exception for invalid rating values.
     */
    @Test
    public void testCalculateScore_InvalidRatingValue() {
        List<RatingCategory> ratingCategories = Arrays.asList(new RatingCategory(1L, "Category 1", 1.0));

        when(ratingCategoryRepository.findAll()).thenReturn(ratingCategories);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            scoreService.calculateScore(Collections.singletonMap("Category 1", 6));
        });

        assertEquals("Invalid rating value for category: Category 1", thrown.getMessage());
    }

    /**
     * Tests that calculateScore returns zero when total weight is zero.
     */
    @Test
    public void testCalculateScore_ZeroTotalWeight() {
        List<RatingCategory> ratingCategories = Arrays.asList(new RatingCategory(1L, "Category 1", 0.0));

        when(ratingCategoryRepository.findAll()).thenReturn(ratingCategories);

        double score = scoreService.calculateScore(Collections.singletonMap("Category 1", 3));
        assertEquals(0, score);
    }

    /**
     * Tests that calculateScore calculates the correct score.
     */
    @Test
    public void testCalculateScore_Success() {
        List<RatingCategory> ratingCategories = Arrays.asList(new RatingCategory(1L, "Category 1", 1.0),
                new RatingCategory(2L, "Category 2", 2.0));

        when(ratingCategoryRepository.findAll()).thenReturn(ratingCategories);

        Map<String, Integer> ratings = Map.of("Category 1", 4, "Category 2", 3);

        double score = scoreService.calculateScore(ratings);
        assertEquals(66.67, score);
    }
}
