# 1) JDK 기반 이미지 선택 (런타임)
FROM eclipse-temurin:17-jdk AS build

# 2) 작업 디렉토리 생성
WORKDIR /app

# 3) Gradle 관련 파일만 복사 → dependency 캐시 활용
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 4) 프로젝트 전체 복사
COPY src src

# 5) jar 빌드
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# -------------- 실제 실행 이미지 --------------
FROM eclipse-temurin:17-jre

WORKDIR /app

# 빌드 단계에서 만들어진 jar 파일 가져오기
COPY --from=build /app/build/libs/*.jar app.jar

# 8) 컨테이너 포트 오픈
EXPOSE 8080

# 9) 실행 명령
ENTRYPOINT ["java", "-jar", "app.jar"]
