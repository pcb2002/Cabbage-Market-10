package com.example.cabbagemarket10.domain.review.repository;

import com.example.cabbagemarket10.domain.review.dto.response.ReceivedReviewListItemResponse;
import com.example.cabbagemarket10.domain.review.dto.response.WrittenReviewListItemResponse;
import com.example.cabbagemarket10.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query(value = """
            select new com.example.cabbagemarket10.domain.review.dto.response.ReceivedReviewListItemResponse(
                r.id,
                r.item.id,
                reviewer.id,
                reviewer.nickname,
                reviewer.profileImageUrl,
                r.rating,
                r.content,
                r.createdAt
            )
            from Review r
            left join r.reviewer reviewer
            where r.reviewee.id = :revieweeId
            order by r.createdAt desc, r.id desc
            """,
            countQuery = """
            select count(r)
            from Review r
            where r.reviewee.id = :revieweeId
            """)
    Page<ReceivedReviewListItemResponse> findReceivedReviews(@Param("revieweeId") Long revieweeId, Pageable pageable);

    @Query(value = """
            select new com.example.cabbagemarket10.domain.review.dto.response.WrittenReviewListItemResponse(
                r.id,
                item.id,
                item.title,
                (select min(ii.imageUrl) from ItemImage ii where ii.item = item and ii.isThumbnail = true),
                reviewee.id,
                reviewee.nickname,
                r.rating,
                r.content,
                r.createdAt
            )
            from Review r
            left join r.item item
            left join r.reviewee reviewee
            where r.reviewer.id = :reviewerId
            order by r.createdAt desc, r.id desc
            """,
            countQuery = """
            select count(r)
            from Review r
            where r.reviewer.id = :reviewerId
            """)
    Page<WrittenReviewListItemResponse> findWrittenReviews(@Param("reviewerId") Long reviewerId, Pageable pageable);
}
