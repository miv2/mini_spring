# 1. 빌드 스테이지: 소스 코드를 빌드하여 JAR 파일을 만듭니다.
FROM gradle:8.10-jdk17 AS build
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

# 2. 실행 스테이지: 빌드된 JAR 파일만 가져와서 가볍게 실행합니다.
FROM eclipse-temurin:17-jdk-focal
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
