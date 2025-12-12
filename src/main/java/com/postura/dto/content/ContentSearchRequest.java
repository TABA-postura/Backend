package com.postura.dto.content;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentSearchRequest {

    private String keyword;    // 제목 검색
    private String category;   // 카테고리 (전체, 자세, 스트레칭)
}
