package com.example.cabbagemarket10.domain.inquiry.repository;

import com.example.cabbagemarket10.domain.inquiry.entity.InquiryLog;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryLogRepository extends JpaRepository<InquiryLog, Long> {

    @Query(
            value = """
                    select inquiryLog
                    from InquiryLog inquiryLog
                    join fetch inquiryLog.author
                    where inquiryLog.item.id = :itemId
                      and inquiryLog.targetInquiry is null
                    order by inquiryLog.createdAt desc
                    """,
            countQuery = """
                    select count(inquiryLog)
                    from InquiryLog inquiryLog
                    where inquiryLog.item.id = :itemId
                      and inquiryLog.targetInquiry is null
                    """
    )
    Page<InquiryLog> findRootInquiriesByItemIdWithAuthor(
            @Param("itemId") Long itemId,
            Pageable pageable
    );

    @Query("""
            select inquiryLog
            from InquiryLog inquiryLog
            join fetch inquiryLog.item item
            join fetch item.seller
            where inquiryLog.id = :inquiryId
              and inquiryLog.targetInquiry is null
            """)
    Optional<InquiryLog> findQuestionByIdWithItemSeller(@Param("inquiryId") Long inquiryId);

    boolean existsByTargetInquiryId(Long inquiryId);
}
