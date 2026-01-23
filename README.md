# 🚀 Postura Backend (Spring Boot)

웹캠 기반 실시간 자세 교정 서비스 'Postura'의 코어 API 서버입니다.</br>
Spring Boot를 기반으로 고성능 데이터 파이프라인을 구축하여 사용자의 자세 데이터를 실시간으로 처리하며,</br>
분석된 데이터를 바탕으로 개인 맞춤형 통계 리포트를 제공합니다. </br>
**[수집(FastAPI) → 필터링 및 비동기 처리(Spring Boot) → 고속 캐싱(Redis) → 영구 저장(MySQL) → 배치 집계(Batch)]** </br>
주요 기능은 다음과 같다. </br>
- 회원가입 및 로그인
- 모니터링 제어
- 자세 판별 데이터 수집
- 실시간 피드백 및 통계
- 정보 제공
- 통계 데이터 및 리포트 제
- 맞춤형 스트레칭 추천

---
## 🛠️ 기술 스택
- **언어:** ![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
  
- **웹 프레임워크:** ![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)

- **데이터베이스:** ![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white) ![AWS RDS](https://img.shields.io/badge/AWS_RDS-527FFF?style=flat-square&logo=amazon-rds&logoColor=white)

- **캐시/메시징:** ![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)

- **Infra/DevOps:** ![AWS](https://img.shields.io/badge/AWS-EC2%20%2F%20ALB-FF9900?style=flat-square&logo=amazonaws&logoColor=white) ![Docker](https://img.shields.io/badge/Docker-Container-2496ED?style=flat-square&logo=docker&logoColor=white) ![Actions](https://img.shields.io/badge/GitHub_Actions-CI/CD-2088FF?style=flat-square&logo=githubactions&logoColor=white)
---

## 📂 프로젝트 구조

<details>
  <summary><b>폴더/파일 트리 펼쳐보기</b></summary>

```text
src/main/java/com/postura/
├── ai/                                 # [AI 데이터 파이프라인]
│   ├── controller/
│   │   └── AiController.java           # FastAPI 분석 결과 수신
│   ├── entity/
│   │   └── PostureLog.java             # 자세 분석 원본 로그
│   ├── repository/
│   │   └── PostureLogRepository.java   # 대용량 로그 조회 및 삭제
│   └── service/
│       ├── PostureLogService.java      # @Async 기반 비동기 로그 저장
│       └── LogCleanupService.java      # 30일 경과 로그 자동 삭제 배치```
├── auth/                               # [사용자 인증 및 권한 관리]
│   ├── controller/
│   │   └── AuthController.java         # 로그인, 로그아웃 및 토큰 재발급 API 엔드포인트
│   ├── domain/
│   │   └── RefreshToken.java           # Access Token 만료 시 재발급을 위한 토큰 엔티티
│   ├── filter/
│   │   └── JwtAuthenticationFilter.java # 모든 요청에서 JWT 유효성을 검사하는 보안 필터
│   ├── handler/
│   │   ├── OAuth2AuthenticationFailureHandler.java # 소셜 로그인 실패 시 후속 처리 로직
│   │   └── OAuth2AuthenticationSuccessHandler.java # 소셜 로그인 성공 시 JWT 발급 및 리다이렉트
│   ├── repository/
│   │   └── RefreshTokenRepository.java  # DB/Redis를 통한 리프레시 토큰 저장 및 조회
│   └── service/
│       ├── AuthService.java            # 일반 로그인 처리 및 인증 관련 핵심 비즈니스 로직
│       ├── JwtTokenProvider.java       # JWT 생성, 토큰 파싱, 유효성 검증 유틸리티
│       └── OAuthService.java           # 소셜 제공자(Google 등)로부터 받은 사용자 정보 처리
├── common/                             # [전역 공통 컴포넌트]
│   ├── exception/                      # [예외 처리 시스템]
│   │   ├── CustomException.java        # 비즈니스 로직 예외의 최상위 클래스
│   │   ├── DuplicateEmailException.java # 이메일 중복 시 발생하는 구체적 예외
│   │   ├── ErrorCode.java              # 에러 메시지와 상태 코드를 정의한 Enum
│   │   ├── ErrorResponse.java          # 클라이언트에게 반환할 공통 에러 응답 객체
│   │   └── GlobalExceptionHandler.java  # 어플리케이션 전역의 예외를 캐치하여 처리
│   ├── util/                           # [유틸리티 및 컨버터]
│   │   └── StringListConverter.java    # DB와 Entity 간의 리스트 데이터 변환 (JPA)
│   ├── BaseTimeEntity.java             # 생성/수정 시간을 자동 관리하는 Base Entity
│   └── config/                         # [시스템 환경 설정]
│       ├── properties/                 # 외부 설정 파일(.yml) 매핑 클래스
│       │   └── AppProperties.java      # 앱 전역 설정값 관리
│       ├── JwtProperties.java          # JWT 관련 설정값 (Secret Key, 만료시간 등)
│       ├── SecurityConfig.java         # Spring Security 필터 및 권한 설정
│       └── WebConfig.java              # CORS, 인터셉터 등 웹 관련 설정
├── content/                            # [콘텐츠 및 메인 도메인 관리]
│   ├── controller/
│   │   └── ContentController.java      # 콘텐츠 관련 API 엔드포인트 (CRUD 및 비즈니스 요청 접수)
│   ├── entity/
│   │   └── Content.java                # 자세 교정 콘텐츠 정보를 담는 핵심 데이터 모델
│   ├── repository/
│   │   └── ContentRepository.java      # 데이터베이스 접근을 위한 JPA 리포지토리 인터페이스
│   └── service/
│       └── ContentService.java         # 콘텐츠 등록, 수정, 조회 및 상세 로직 수행
├── dto/                                # [계층 간 데이터 전송 객체 모음]
│   ├── ai/                             # [AI 서버 통신용 데이터]
│   │   ├── PostureLogRequest.java      # AI 서버로 전송할 자세 분석 요청 데이터
│   │   └── RealtimeFeedbackResponse.java # 분석 후 실시간 피드백 응답 데이터
│   ├── auth/                           # [인증 및 회원가입 관련 데이터]
│   │   ├── LoginRequest.java           # 일반 로그인 요청 (ID/PW)
│   │   ├── OAuthLoginRequest.java      # 소셜 로그인 인증 데이터
│   │   ├── RefreshTokenRequest.java    # 토큰 재발급 요청 데이터
│   │   ├── SignUpRequest.java          # 신규 회원가입 정보
│   │   ├── TokenResponse.java          # JWT 발급 결과 응답 (Access/Refresh)
│   │   └── UserInfo.java               # 인증된 사용자 정보 DTO
│   ├── content/                        # [콘텐츠 CRUD 및 검색 데이터]
│   │   ├── ContentDetailResponse.java  # 콘텐츠 상세 정보 응답
│   │   ├── ContentListResponse.java    # 콘텐츠 목록 조회 응답
│   │   └── ContentSearchRequest.java   # 검색 조건 필터 데이터
│   ├── monitor/                        # [실시간 세션 모니터링 데이터]
│   │   ├── SessionControlRequest.java  # 측정 세션 제어(시작/중지) 요청
│   │   └── SessionStartResponse.java   # 세션 시작 결과 및 초기 설정 응답
│   └── report/                         # [분석 리포트 및 통계 데이터]
│       ├── RecommendationDto.java      # 사용자 맞춤형 교정 추천 데이터
│       └── StatReportDto.java          # 기간별 자세 통계 리포트 데이터
├── health/                             # [시스템 상태 모니터링]
│   └── controller/
│       └── HealthCheckController.java  # AWS ALB 및 인프라 상태 확인용 엔드포인트
├── monitor/                            # [실시간 자세 분석 세션 관리]
│   ├── controller/
│   │   ├── FeedbackController.java     # 사용자 맞춤형 실시간 피드백 전달 API
│   │   └── SessionController.java      # 분석 세션 시작 및 종료 제어
│   ├── entity/
│   │   ├── MonitoringSession.java      # 개별 측정 세션 정보(시간, 상태 등) 엔티티
│   │   └── SessionStatus.java          # 세션의 상태(START, PROGRESS, END 등) 정의 Enum
│   ├── repository/
│   │   └── MonitoringSessionRepository.java # 세션 데이터 저장 및 관리
│   └── service/
│       ├── MonitoringService.java      # 측정 세션의 생명주기 관리 비즈니스 로직
│       └── RealtimeFeedbackService.java # AI 분석 결과를 바탕으로 즉각적인 교정 가이드 생성
├── report/                             # [사용자 분석 리포트 생성 및 관리]
│   ├── controller/
│   │   └── ReportController.java       # 일간/주간/월간 통계 및 맞춤형 리포트 조회 API
│   ├── entity/
│   │   └── AggregateStat.java          # 기간별로 요약된 자세 통계 데이터 엔티티
│   ├── repository/
│   │   └── AggregateStatRepository.java # 통계 데이터 조회 및 기간별 집계 쿼리 수행
│   └── service/
│       ├── StatAggregationService.java # 원본 로그(PostureLog)를 분석하여 통계 데이터로 가공
│       └── SelfManagementService.java  # 통계 기반의 사용자 맞춤형 개선 가이드 및 관리 로직
├── user/                               # [사용자 계정 및 보안 정보 관리]
│   ├── domain/                         # 소셜 인증 결과 처리를 위한 데이터 모델
│   │   ├── CustomOAuth2User.java       # 소셜 로그인 사용자 정보의 공통 인터페이스
│   │   ├── CustomOidUser.java          # OpenID Connect(Google 등) 인증 사용자 모델
│   │   └── OAuth2Attributes.java       # 각 SNS 제공자별 속성 값을 매핑하는 유틸리티
│   ├── entity/
│   │   └── User.java                   # 사용자 정보(이메일, 권한 등)를 담는 DB 엔티티
│   ├── repository/
│   │   └── UserRepository.java         # 이메일이나 소셜 식별값으로 사용자 정보 조회
│   └── service/                        # [인증 및 비즈니스 로직 서비스]
│       ├── CustomOAuth2UserService.java # 소셜 로그인 시 사용자 프로필 로드 및 저장
│       ├── CustomOidUserService.java   # OIDC 기반 로그인 사용자 처리 서비스
│       ├── CustomUserDetails.java      # Spring Security용 사용자 인증 정보 객체
│       ├── CustomUserDetailsService.java # DB 기반 일반 사용자 인증 정보를 불러오는 서비스
│       └── UserService.java            # 프로필 수정 및 사용자 정보 관리 비즈니스 로직
├── util/                               # [전역 유틸리티]
│   └── StringListConverter.java        # DB 컬럼과 자바 List 객체 간 데이터 변환 처리
└── PosturaApplication.java             # [프로젝트 실행 클래스] Spring Boot 어플리케이션 메인
```
</details>

---
