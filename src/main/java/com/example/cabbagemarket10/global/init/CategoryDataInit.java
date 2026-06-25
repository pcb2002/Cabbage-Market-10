package com.example.cabbagemarket10.global.init;

import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryDataInit implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String @NonNull ... args) throws Exception {
        // DB에 카테고리 데이터가 하나도 없을 때만 초기화 실행
        if (categoryRepository.count() == 0) {
            log.info("초기 카테고리 데이터 세팅을 시작합니다...");

            // 단일 구조에 맞춘 카테고리 목록 생성
            List<Category> initCategories = List.of(
                    Category.builder().name("패션/의류").sortOrder(1).isActive(true).build(),
                    Category.builder().name("디지털/가전").sortOrder(2).isActive(true).build(),
                    Category.builder().name("도서/음반").sortOrder(3).isActive(true).build(),
                    Category.builder().name("스포츠/레저").sortOrder(4).isActive(true).build(),
                    Category.builder().name("생활/주방").sortOrder(5).isActive(true).build(),
                    Category.builder().name("취미/게임").sortOrder(6).isActive(true).build(),
                    Category.builder().name("뷰티/미용").sortOrder(7).isActive(true).build(),
                    Category.builder().name("기타").sortOrder(8).isActive(true).build()
            );

            // saveAll을 사용하여 한 번에 Insert (성능 최적화)
            categoryRepository.saveAll(initCategories);

            log.info("초기 카테고리 데이터 세팅 완료! (총 {}개)", initCategories.size());
        } else {
            log.info("카테고리 데이터가 이미 존재하여 초기화를 건너뜁니다.");
        }
    }
}
