package com.postura.dto.content;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentSearchRequest {

    private String keyword;      // 제목 검색(부분일치)
    private String category;     // 카테고리 (예: "자세", "스트레칭", "전체" or null)
    private String relatedPart;  // 관련부위(예: "목", "어깨") - 부분일치
}
