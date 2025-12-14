package com.postura.content.repository;

import com.postura.content.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentRepository extends JpaRepository<Content, Long> {

    // 제목 검색
    List<Content> findByTitleContainingIgnoreCase(String keyword);

    // 카테고리 검색 (String 기반)
    List<Content> findByCategory(String category);

    // 카테고리 + 제목 검색
    List<Content> findByCategoryAndTitleContainingIgnoreCase(String category, String keyword);

    // 관련된 자세 (relatedPosture) 검색
    List<Content> findByRelatedPart(String posture);

    // 카테고리 + 제목 + 관련 자세 검색
    List<Content> findByCategoryAndTitleContainingIgnoreCaseAndRelatedPart(String category, String keyword, String posture);

    /**
     * Report 모듈 연동용: category가 "스트레칭"이고 posture가 문제 유형과 일치하는 Content 목록을 조회합니다.
     */
    List<Content> findByCategoryAndPosture(String category, String posture);
}
