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
    private String category;       // VARCHAR(50)
    private String contentText;    // TEXT
    private String s3ImageUrl;     // S3 URL
    private String relatedPosture; // 연관된 자세
}
