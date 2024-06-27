package com.kaarelkaasla.klaustestassignment.util;

import com.kaarelkaasla.klaustestassignment.entity.RatingCategory;
import com.kaarelkaasla.klaustestassignment.repository.RatingCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for operations related to rating categories.
 */
@Component
public class RatingCategoryUtils {

    private final RatingCategoryRepository ratingCategoryRepository;

    /**
     * Constructs a RatingCategoryUtils with the given RatingCategoryRepository.
     *
     * @param ratingCategoryRepository
     *            The repository for accessing rating category data.
     */
    @Autowired
    public RatingCategoryUtils(RatingCategoryRepository ratingCategoryRepository) {
        this.ratingCategoryRepository = ratingCategoryRepository;
    }

    /**
     * Retrieves a map of category IDs to category names.
     *
     * @return A map where the key is the category ID and the value is the category name.
     */
    public Map<Long, String> getCategoryIdToNameMap() {
        return ratingCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(RatingCategory::getId, RatingCategory::getName));
    }
}
