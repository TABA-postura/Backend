package com.postura.dto.report;

import lombok.Builder;
import lombok.Getter;

/**
 * 맞춤 추천 스트레칭 정보 (Content 엔티티 필드에 맞춤)
 */
@Getter
@Builder
public class RecommendationDto {
    private final String problemType; // 문제 유형 (FORWARD_HEAD 등)
    private final String recommendedGuideTitle; // Content.title
    private final Long guideId; // Content.guideId (FK 대신 직접 ID 사용)
}
