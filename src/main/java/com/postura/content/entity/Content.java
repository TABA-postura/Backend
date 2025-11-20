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
    private String title; // 거북목 증후군, 목 스트레칭

    @Column(nullable = false, length = 50)
    private String category; // 질환, 운동

    @Column(name = "content_text", columnDefinition = "TEXT", nullable = false)
    private String contentText; // 상세 텍스트

    @Column(name = "s3_image_url", length = 255)
    private String s3ImageUrl; // 이미지 url

    @Column(name = "related_posture", length = 100)
    private String relatedPosture; // 목, 어깨

    public void updateContent(String title,
                              String category,
                              String contentText,
                              String s3ImageUrl,
                              String relatedPosture) {

        this.title = title;
        this.category = category;
        this.contentText = contentText;
        this.s3ImageUrl = s3ImageUrl;
        this.relatedPosture = relatedPosture;
    }
}
