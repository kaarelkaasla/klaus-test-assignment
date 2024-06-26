package com.kaarelkaasla.klaustestassignment.repository;

import com.kaarelkaasla.klaustestassignment.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for the Rating entity. Extends JpaRepository to provide basic CRUD operations. Contains custom
 * queries to aggregate ratings data.
 */
@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    /**
     * Finds aggregated ratings between the specified start and end dates.
     *
     * @param startDate
     *            the start date of the period in 'YYYY-MM-DD' format.
     * @param endDate
     *            the end date of the period in 'YYYY-MM-DD' format.
     *
     * @return a list of objects arrays containing date, rating category ID, frequency, and average rating.
     */
    @Query(value = "SELECT DATE(created_at) as date, rating_category_id, COUNT(*) as frequency, AVG(rating) as average_rating FROM ratings WHERE created_at BETWEEN :startDate AND :endDate GROUP BY DATE(created_at), rating_category_id ORDER BY DATE(created_at), rating_category_id", nativeQuery = true)
    List<Object[]> findAggregatedRatingsBetween(String startDate, String endDate);

    /**
     * Finds weekly aggregated ratings between the specified start and end dates.
     *
     * @param startDate
     *            the start date of the period in 'YYYY-MM-DD' format.
     * @param endDate
     *            the end date of the period in 'YYYY-MM-DD' format.
     *
     * @return a list of objects arrays containing week range, rating category ID, frequency, and average rating.
     */
    @Query(value = "SELECT MIN(DATE(created_at)) || ' to ' || CASE WHEN MAX(DATE(created_at)) > :endDate THEN :endDate ELSE MAX(DATE(created_at)) END as week_range, rating_category_id, COUNT(*) as frequency, AVG(rating) as average_rating FROM ratings WHERE created_at BETWEEN :startDate AND :endDate GROUP BY strftime('%Y-%W', created_at), rating_category_id ORDER BY MIN(DATE(created_at)), rating_category_id", nativeQuery = true)
    List<Object[]> findWeeklyAggregatedRatingsBetween(@Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * Finds all ratings within the specified period.
     *
     * @param startDate
     *            the start date of the period in 'YYYY-MM-DD' format.
     * @param endDate
     *            the end date of the period in 'YYYY-MM-DD' format.
     *
     * @return a list of objects arrays containing ticket ID, rating category ID, and rating.
     */
    @Query(value = "SELECT ticket_id, rating_category_id, rating FROM ratings WHERE created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    List<Object[]> findRatingsWithinPeriod(String startDate, String endDate);
}
