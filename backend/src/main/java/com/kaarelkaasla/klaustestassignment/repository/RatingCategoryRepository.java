package com.kaarelkaasla.klaustestassignment.repository;

import com.kaarelkaasla.klaustestassignment.entity.RatingCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for the RatingCategory entity. Extends JpaRepository to provide basic CRUD operations.
 */
@Repository
public interface RatingCategoryRepository extends JpaRepository<RatingCategory, Long> {
}
