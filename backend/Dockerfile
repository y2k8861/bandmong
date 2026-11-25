# ---- 빌드 ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
# 의존성만 먼저 받아 두면, 코드만 바뀔 때 이 층을 캐시로 다시 쓴다
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# ---- 실행 ----
FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --uid 1001 app
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
USER app
EXPOSE 8080
# 컨테이너 메모리의 70%까지 힙으로 (EC2 1~2GB 인스턴스 기준)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError" \
    SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Seoul
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
