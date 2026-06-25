package com.example.cabbagemarket10.domain.inquiry.repository;

import com.example.cabbagemarket10.domain.inquiry.entity.InquiryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryLogRepository extends JpaRepository<InquiryLog, Long> {
}
