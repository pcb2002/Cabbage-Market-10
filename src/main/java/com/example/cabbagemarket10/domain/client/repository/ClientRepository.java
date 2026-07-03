package com.example.cabbagemarket10.domain.client.repository;

import com.example.cabbagemarket10.domain.client.entity.Client;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByEmail(String email);

    // 이메일 중복 검사는 탈퇴(soft-delete) 회원까지 포함해야 한다.
    // @SQLRestriction("is_deleted = false")은 native 쿼리에 적용되지 않으므로 native로 고정한다.
    @Query(value = "SELECT COUNT(1) FROM client WHERE email = :email", nativeQuery = true)
    long countByEmailIncludingDeleted(@Param("email") String email);
}
