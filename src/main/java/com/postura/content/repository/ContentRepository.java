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

    // related_posture 검색 (선택 사항)
    List<Content> findByRelatedPosture(String posture);
}
