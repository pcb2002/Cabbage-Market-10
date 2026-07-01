package com.example.cabbagemarket10.domain.chat.repository;

import com.example.cabbagemarket10.domain.chat.dto.restful.ChatRoomDetail;
import com.example.cabbagemarket10.domain.chat.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {

    @Query("""
            SELECT new com.example.cabbagemarket10.domain.chat.dto.restful.ChatRoomDetail
                        (cr.id, i.title, cr.lastMessageAt)
            FROM ChatRoom cr
            join fetch cr.item i
            join fetch cr.createdBy cbci
            WHERE cbci.id = :clientId OR i.seller.id = :clientId""")
    Page<ChatRoomDetail> findByClientId(Long clientId, Pageable pageable);
}
