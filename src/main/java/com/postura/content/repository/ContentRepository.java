package com.postura.content.repository;

import com.postura.content.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContentRepository extends JpaRepository<Content, Long> {

    /**
     * 통합 검색:
     * - category, keyword, relatedPart 중 어떤 값이 오든(혹은 안 오든) 모두 처리
     * - keyword: title 부분일치 (ignore case)
     * - relatedPart: relatedPart 부분일치 (ignore case)
     *
     * 주의:
     * - category가 null/빈값이면 전체 카테고리
     */
    @Query("""
        select c
        from Content c
        where (:category is null or :category = '' or c.category = :category)
          and (:keyword is null or :keyword = '' or lower(c.title) like lower(concat('%', :keyword, '%')))
          and (:relatedPart is null or :relatedPart = '' or lower(c.relatedPart) like lower(concat('%', :relatedPart, '%')))
        """)
    List<Content> search(@Param("category") String category,
                         @Param("keyword") String keyword,
                         @Param("relatedPart") String relatedPart);

    /**
     * Report 모듈 연동용: category가 "스트레칭"이고 posture가 문제 유형과 일치하는 Content 목록
     */
    List<Content> findByCategoryAndPosture(String category, String posture);
}
