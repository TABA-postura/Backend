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
        // 전체 조회
        else {
            result = contentRepository.findAll();
        }

        return result.stream()
                .map(content -> ContentListResponse.builder()
                        .id(content.getGuideId())
                        .title(content.getTitle())
                        .category(content.getCategory())
                        .s3ImageUrl(content.getS3ImageUrl())
                        .relatedPosture(content.getRelatedPosture())
                        .build()
                ).toList();
    }

    /**
     * 콘텐츠 상세 조회
     */
    public ContentDetailResponse getContentDetail(Long id) {

        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("콘텐츠를 찾을 수 없습니다."));

        return ContentDetailResponse.builder()
                .id(content.getGuideId())
                .title(content.getTitle())
                .category(content.getCategory())
                .contentText(content.getContentText())
                .s3ImageUrl(content.getS3ImageUrl())
                .relatedPosture(content.getRelatedPosture())
                .build();
    }

    // *************************************************************
    // REPORT 모듈 연동을 위한 메서드 추가 (핵심)
    // *************************************************************

    /**
     * Report 모듈에서 가장 빈번한 문제 유형을 기반으로 스트레칭 가이드를 조회합니다.
     * Content 엔티티의 relatedPosture 필드와 연결합니다.
     * @param problemType AI 감지 신호 (예: "FORWARD_HEAD", "UNE_SHOULDER")
     * @return Content 엔티티 리스트
     */
    public List<Content> getGuidesByProblemType(String problemType) {

        // 1. AI 신호를 Content 엔티티의 'relatedPosture' 필드 값에 맞게 변환해야 합니다.
        //    (예: "FORWARD_HEAD" -> "목")
        String relatedPostureKeyword = mapProblemTypeToPostureKeyword(problemType);

        // 2. ContentRepository를 사용하여 해당 키워드와 관련된 가이드 조회
        //    (findByRelatedPosture 메서드가 ContentRepository에 정의되어야 함을 가정합니다.)
        return contentRepository.findByRelatedPosture(relatedPostureKeyword);
    }

    /**
     * AI 문제 유형 코드를 Content 엔티티의 relatedPosture 키워드로 변환합니다.
     */
    private String mapProblemTypeToPostureKeyword(String problemType) {
        // 문제 유형에 따라 Content.relatedPosture에 정의된 키워드 (목, 어깨, 상체)를 반환합니다.
        switch (problemType) {
            case "FORWARD_HEAD":
            case "HEAD_TILT":
                return "목";
            case "UNEQUAL_SHOULDERS":
            case "LEANING_ON_ARM":
                return "어깨";
            case "UPPER_BODY_TILT":
            case "ASYMMETRIC_POSTURE":
            case "TOO_CLOSE":
                return "상체"; // 또는 복합적인 문제를 커버할 수 있는 키워드
            default:
                return ""; // 해당 없음 (Good이거나 정의되지 않은 경우)
        }
    }
}
