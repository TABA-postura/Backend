package com.postura.common;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Spring Data JPA Auditing 기능을 사용하여 엔티티가 데이터베이스에
 * 생성(createAt)되거나 수정(updateAt)될 때 그 시각을 자동으로 기록하고,
 * 모든 JPA 엔티티가 상속받아 시간 관리 기능을 공통으로 사용할 수 있도록 하는 추상 공통 클래스입니다.
 */
@Getter // 엔티티 시간 조회
@MappedSuperclass // JPA 엔티티들이 이 클래스를 상속받을 경우, 이 클래스의 필드들을 DB 테이블의 컬럼으로 인식
@EntityListeners(AuditingEntityListener.class)
// JPA에게 해당 클래스에 Auditing 기능을 적용하도록 지시(생성/수정 시간 자동화)
public abstract class BaseTimeEntity {

    @CreatedDate // 엔티티 생성될때 시간 저장
    private LocalDateTime createAt;

    @LastModifiedBy // 엔티티 값 변경할 때 시간이 저장
    private LocalDateTime updateAt;

}
