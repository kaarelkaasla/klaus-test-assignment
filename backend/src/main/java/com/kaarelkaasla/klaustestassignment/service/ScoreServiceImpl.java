package com.kaarelkaasla.klaustestassignment.service;

import com.kaarelkaasla.klaustestassignment.entity.RatingCategory;
import com.kaarelkaasla.klaustestassignment.repository.RatingCategoryRepository;
import com.kaarelkaasla.klaustestassignment.util.DateUtils;
import com.kaarelkaasla.klaustestassignment.util.MathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Implementation of the ScoreService for calculating weighted scores.
 */
@Service
@Slf4j
public class ScoreServiceImpl implements ScoreService {

    private final RatingCategoryRepository ratingCategoryRepository;

    @Autowired
    public ScoreServiceImpl(RatingCategoryRepository ratingCategoryRepository) {
        this.ratingCategoryRepository = ratingCategoryRepository;
    }

    @Override
    public double calculateScore(Map<String, Integer> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            log.warn("Ratings map is null or empty");
            throw new IllegalArgumentException("Ratings map must not be null or empty");
        }

        List<RatingCategory> ratingCategories;
        try {
            ratingCategories = ratingCategoryRepository.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve rating categories from the database", e);
            throw new RuntimeException("Failed to retrieve rating categories from the database", e);
        }

        double[] results = ratingCategories.stream()
                .filter(ratingCategory -> ratings.containsKey(ratingCategory.getName())).map(ratingCategory -> {
                    Integer ratingValue = ratings.get(ratingCategory.getName());
                    if (ratingValue == null || ratingValue < 0 || ratingValue > 5) {
                        log.warn("Invalid rating value for category: {}", ratingCategory.getName());
                        throw new IllegalArgumentException(
                                "Invalid rating value for category: " + ratingCategory.getName());
                    }
                    return new double[] { ratingCategory.getWeight(), ratingValue * ratingCategory.getWeight() };
                }).reduce(new double[] { 0, 0 }, (a, b) -> new double[] { a[0] + b[0], a[1] + b[1] });

        double totalWeight = results[0];
        double weightedSum = results[1];

        if (totalWeight == 0) {
            log.warn("Total weight is zero, unable to calculate score");
            return 0;
        }

        double score = (weightedSum / (totalWeight * 5)) * 100;
        return MathUtils.roundToTwoDecimalPlaces(score);
    }
}
