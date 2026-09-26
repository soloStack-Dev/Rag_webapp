FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/target/rag-app-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001

EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]