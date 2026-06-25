package com.example.cabbagemarket10.domain.item.repository;

import com.example.cabbagemarket10.domain.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    // 기본적인 CRUD 및 페이징 기능 제공
    // 추후 QueryDSL 확장을 위해 ItemRepositoryCustom 인터페이스를 만들고
    // 여기에서 ItemRepositoryCustom을 상속받는 구조로 변경하시면 됩니다.
}