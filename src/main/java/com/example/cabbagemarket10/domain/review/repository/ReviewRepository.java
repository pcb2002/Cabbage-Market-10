package com.example.cabbagemarket10.domain.review.repository;

import com.example.cabbagemarket10.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    long countByReviewee_Id(Long revieweeId);

    boolean existsByItem_IdAndReviewer_Id(Long itemId, Long reviewerId);

    @Query("""
            select coalesce(avg(review.rating), 0)
            from Review review
            where review.reviewee.id = :revieweeId
            """)
    double findAverageRatingByRevieweeId(@Param("revieweeId") Long revieweeId);
}
