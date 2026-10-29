# syntax=docker/dockerfile:1
FROM node:24-bookworm-slim AS frontend
WORKDIR /build
COPY package.json package-lock.json .npmrc ./
RUN npm ci --ignore-scripts
COPY frontend ./frontend
RUN npm run build

FROM maven:3.9.16-eclipse-temurin-21 AS backend
WORKDIR /build
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn --batch-mode -q dependency:go-offline
COPY backend ./backend
COPY scripts/HealthCheck.java ./scripts/HealthCheck.java
COPY --from=frontend /build/frontend/dist ./frontend/dist
RUN --mount=type=cache,target=/root/.m2 mvn --batch-mode -q -DskipTests package && javac --release 21 -d healthcheck scripts/HealthCheck.java

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app
COPY --from=backend /build/target/fieldkit.jar /app/fieldkit.jar
COPY --from=backend /build/healthcheck /app/healthcheck
HEALTHCHECK --interval=20s --timeout=5s --start-period=50s --retries=4 CMD ["java","-Xmx24m","-cp","/app/healthcheck","HealthCheck"]
COPY LICENSES /app/LICENSES
COPY THIRD_PARTY_NOTICES.md /app/THIRD_PARTY_NOTICES.md
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:+ExitOnOutOfMemoryError -Duser.timezone=UTC"
USER app
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/fieldkit.jar","--debug=false"]
