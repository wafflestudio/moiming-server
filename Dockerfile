FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY src ./src
RUN gradle build --no-daemon -x test
#RUN --mount=type=secret,id=github_token GITHUB_TOKEN=$(cat /run/secrets/github_token) ./gradlew :api:bootJar
#github_token을 받아서 waffle-spring에서 oci vault 라이브러리를 받아옴.


FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
