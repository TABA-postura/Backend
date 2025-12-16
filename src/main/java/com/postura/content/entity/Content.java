package com.postura.content.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "content")
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guide_id")
    private Long guideId; // id

    @Column(nullable = false, length = 100)
    private String title; // ex. 거북목 증후군, 목 스트레칭

    @Column(nullable = false, length = 50)
    private String category; // 자세 or 스트레칭

    @Column(name = "content_text", columnDefinition = "TEXT", nullable = false)
    private String contentText; // 상세 텍스트

    @Column(name = "image_url", length = 255)
    private String imageUrl; // 이미지 url

    @Column(name = "related_part", length = 100)
    private String relatedPart; // ex. 목, 어깨

    @Column(nullable = true)
    private String posture; // 스트레칭에만 값을 넣고 자세에는 NULL ex. ASYMMETRIC_POSTURE

    public void updateContent(String title,
                              String category,
                              String contentText,
                              String imageUrl,
                              String relatedPosture) {

        this.title = title;
        this.category = category;
        this.contentText = contentText;
        this.imageUrl = imageUrl;
        this.relatedPart = relatedPosture;
    }
}
