package com.kaarelkaasla.klaustestassignment.service;

import java.util.Map;

/**
 * Service for calculating weighted scores.
 */
public interface ScoreService {
    /**
     * Calculates the weighted score based on the provided ratings.
     *
     * @param ratings
     *            A map of category names to their respective ratings.
     *
     * @return The calculated weighted score as a percentage.
     */
    double calculateScore(Map<String, Integer> ratings);
}
