package com.postura.dto.content;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentDetailResponse {

    private Long id;               // guideId
    private String title;
    private String category;       // 카테고리
    private String contentText;    // 상세 텍스트
    private String imageUrl;     // S3 URL
    private String relatedPart;    // 연관된 자세
}
