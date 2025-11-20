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
}
