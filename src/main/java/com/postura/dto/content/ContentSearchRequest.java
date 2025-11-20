package com.postura.dto.content;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentSearchRequest {

    private String keyword;         // 제목 검색
    private String category;        // VARCHAR(50) 기반
    private String relatedPosture;  // 선택 사항: 연관된 자세 검색
}
