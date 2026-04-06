FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY src ./src
RUN --mount=type=secret,id=github_token \
  GITHUB_TOKEN=$(cat /run/secrets/github_token) \
  gradle build --no-daemon -x test && \
  echo "=== Checking vault library in JAR ===" && \
  (jar tf build/libs/*.jar | grep -i oci-vault && echo "[DEBUG] VAULT_LIB_FOUND") || echo "[DEBUG] VAULT_LIB_NOT_FOUND"
#RUN --mount=type=secret,id=github_token GITHUB_TOKEN=$(cat /run/secrets/github_token) ./gradlew :api:bootJar
#github_token을 받아서 waffle-spring에서 oci vault 라이브러리를 받아옴.


FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
