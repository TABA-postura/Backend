package com.postura.content.service;

import com.postura.content.entity.Content;
import com.postura.content.repository.ContentRepository;
import com.postura.dto.content.ContentDetailResponse;
import com.postura.dto.content.ContentListResponse;
import com.postura.dto.content.ContentSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepository contentRepository;

    /**
     * 콘텐츠 검색 / 목록 조회
     * - category + keyword + relatedPart 조합 검색 지원
     * - category가 "전체"면 필터 해제
     */
    public List<ContentListResponse> searchContents(ContentSearchRequest request) {

        String category = normalizeCategory(request.getCategory());
        String keyword = normalize(request.getKeyword());
        String relatedPart = normalize(request.getRelatedPart());

        List<Content> result = contentRepository.search(category, keyword, relatedPart);

        return result.stream()
                .map(content -> ContentListResponse.builder()
                        .id(content.getGuideId())
                        .title(content.getTitle())
                        .category(content.getCategory())
                        .imageUrl(content.getImageUrl())
                        .relatedPart(content.getRelatedPart())
                        .build()
                )
                .toList();
    }

    /**
     * 콘텐츠 상세 조회
     */
    public ContentDetailResponse getContentDetail(Long id) {

        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("콘텐츠를 찾을 수 없습니다. id=" + id));

        return ContentDetailResponse.builder()
                .id(content.getGuideId())
                .title(content.getTitle())
                .category(content.getCategory())
                .contentText(content.getContentText())
                .imageUrl(content.getImageUrl())
                .relatedPart(content.getRelatedPart())
                .build();
    }

    // *************************************************************
    // REPORT 모듈 연동을 위한 메서드 (기존 유지)
    // *************************************************************
    public List<Content> getGuidesByProblemType(String problemType) {
        return contentRepository.findByCategoryAndPosture("스트레칭", problemType);
    }

    // -------------------------
    // 내부 유틸
    // -------------------------
    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeCategory(String category) {
        String normalized = normalize(category);
        if (normalized == null) return null;
        // "전체" 입력 시 필터 해제
        if ("전체".equalsIgnoreCase(normalized)) return null;
        return normalized;
    }
}
