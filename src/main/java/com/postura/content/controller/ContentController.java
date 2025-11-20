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
     */
    @PostMapping
    public List<ContentListResponse> searchContents(@RequestBody ContentSearchRequest request) {
        return contentService.searchContents(request);
    }

    /**
     * 콘텐츠 상세 조회
     */
    @GetMapping("/{id}")
    public ContentDetailResponse getContentDetail(@PathVariable Long id) {
        return contentService.getContentDetail(id);
    }
}
