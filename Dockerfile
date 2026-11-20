FROM node:22-bookworm-slim AS frontend
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /app
COPY pom.xml ./
COPY src/ ./src/
COPY --from=frontend /app/dist/ ./src/main/resources/static/
RUN mvn -B -ntp package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend /app/target/SistemaEspecialista-0.0.1-SNAPSHOT.war ./autolab.war
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "autolab.war"]
