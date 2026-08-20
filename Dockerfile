# 1단계: 빌드 (의존성 → 소스, 레이어 캐싱)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 먼저 받기: gradle 설정·래퍼만 복사 (소스 안 바뀌면 이 레이어 캐시 재사용 → 빌드 빨라짐)
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon

# 소스 복사 후 jar 빌드
COPY src ./src
RUN ./gradlew bootJar --no-daemon

# 2단계: 실행 (JRE + 비-root 유저)
FROM eclipse-temurin:21-jre
WORKDIR /app

# 보안: root 대신 전용 유저로 실행
RUN useradd -u 1001 appuser
COPY --from=build /app/build/libs/*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
