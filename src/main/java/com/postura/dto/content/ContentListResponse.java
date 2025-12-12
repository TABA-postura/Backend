package com.postura.dto.content;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentListResponse {

    private Long id;               // guideId
    private String title;
    private String category;       // 카테고리
    private String relatedPart;    // 관련된 자세
    private String s3ImageUrl;     // S3 이미지 URL
}
