package com.postura.content.service;

import com.postura.content.entity.Content;
import com.postura.content.repository.ContentRepository;
import com.postura.dto.content.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepository contentRepository;

    /**
     * 콘텐츠 검색 / 목록 조회
     * - 카테고리와 키워드를 기준으로 콘텐츠를 검색합니다.
     */
    public List<ContentListResponse> searchContents(ContentSearchRequest request) {

        List<Content> result;

        boolean hasCategory = request.getCategory() != null && !request.getCategory().isEmpty();
        boolean hasKeyword = request.getKeyword() != null && !request.getKeyword().isEmpty();

        // 카테고리 + 키워드 둘 다 있는 경우
        if (hasCategory && hasKeyword) {
            result = contentRepository.findByCategoryAndTitleContainingIgnoreCase(
                    request.getCategory(), request.getKeyword());
        }
        // 카테고리만 검색
        else if (hasCategory) {
            result = contentRepository.findByCategory(request.getCategory());
        }
        // 키워드만 검색
        else if (hasKeyword) {
            result = contentRepository.findByTitleContainingIgnoreCase(request.getKeyword());
        }
        // 전체 조회 (카테고리와 키워드 없이 모든 콘텐츠 조회)
        else {
            result = contentRepository.findAll();
        }

        // 검색된 결과를 ContentListResponse로 변환하여 반환
        return result.stream()
                .map(content -> ContentListResponse.builder()
                        .id(content.getGuideId())
                        .title(content.getTitle())
                        .category(content.getCategory())
                        .imageUrl(content.getImageUrl())
                        .relatedPart(content.getRelatedPart())
                        .build()
                ).toList();
    }

    /**
     * 콘텐츠 상세 조회
     * - id를 기반으로 콘텐츠의 상세 정보를 조회합니다.
     */
    public ContentDetailResponse getContentDetail(Long id) {

        // id로 콘텐츠를 찾고, 없으면 예외를 발생
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("콘텐츠를 찾을 수 없습니다."));

        // 상세 정보를 반환
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
    // REPORT 모듈 연동을 위한 메서드 추가 (핵심)
    // *************************************************************

    /**
     * Report 모듈에서 가장 빈번한 문제 유형을 기반으로 스트레칭 가이드를 조회합니다.
     * Content 엔티티의 category="스트레칭"이고 posture 필드가 problemType과 일치하는 항목을 찾습니다.
     * @param problemType AI 감지 신호 (예: "FORWARD_HEAD", "UNEQUAL_SHOULDERS")
     * @return Content 엔티티 리스트
     */
    public List<Content> getGuidesByProblemType(String problemType) {
        // ContentRepository를 사용하여 'category'가 "스트레칭"이고 'posture'가 problemType과 일치하는 가이드 조회
        return contentRepository.findByCategoryAndPosture("스트레칭", problemType);
    }
}
