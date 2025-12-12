package com.postura.content.controller;

import com.postura.content.service.ContentService;
import com.postura.dto.content.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    /**
     * 콘텐츠 목록 조회 / 검색
     * - 제목 (keyword), 카테고리 (category)로 필터링해서 콘텐츠를 조회합니다.
     */
    @PostMapping("/search")
    public List<ContentListResponse> searchContents(@RequestBody ContentSearchRequest request) {
        // 서비스에서 카테고리와 제목을 기반으로 검색 수행
        return contentService.searchContents(request);
    }

    /**
     * 콘텐츠 상세 조회
     * - id를 통해 콘텐츠의 상세 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    public ContentDetailResponse getContentDetail(@PathVariable Long id) {
        // 서비스에서 id를 기반으로 콘텐츠의 상세 정보를 반환
        return contentService.getContentDetail(id);
    }
}
