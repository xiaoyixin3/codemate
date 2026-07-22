FROM maven:3.9.14-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn --batch-mode --no-transfer-progress -pl codemate-web -am package -DskipTests -Pprod

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd --system --create-home --uid 10001 codemate \
    && mkdir -p /var/lib/codemate/images /var/lib/codemate/logs \
    && chown -R codemate:codemate /app /var/lib/codemate
COPY --from=build /workspace/codemate-web/target/codemate-web-0.0.1-SNAPSHOT.jar /app/codemate.jar
USER codemate
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.devtools.restart.enabled=false", "-jar", "/app/codemate.jar"]
