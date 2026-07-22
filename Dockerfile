FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 friotrack && mkdir -p /app/logs && chown friotrack:friotrack /app/logs
COPY --from=build /workspace/target/friotrackapi-0.0.1-SNAPSHOT.jar /app/friotrackapi.jar
USER friotrack
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/friotrackapi.jar"]
