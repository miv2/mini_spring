# 실행 스테이지: 이미 빌드된 JAR 파일만 가져와서 가볍게 실행합니다.
FROM eclipse-temurin:17-jdk-focal
WORKDIR /app

# GitHub Actions에서 빌드한 JAR 파일을 복사합니다.
# plain.jar가 아닌 실행 가능한 jar만 복사하도록 패턴을 명확히 합니다.
COPY build/libs/*[!-plain].jar app.jar

EXPOSE 8081
# 배포 환경임을 명시하는 프로필 설정
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]