# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests -Dspotless.check.skip=true \
    && cp target/event-tickets-*.jar app.jar

FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="event-tickets" \
      org.opencontainers.image.description="Ticketing for small venues and meetups" \
      org.opencontainers.image.licenses="MIT"
RUN useradd --system --uid 1001 --home /app tickets
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER tickets
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=10 \
  CMD curl -fs http://localhost:8080/actuator/health/readiness || exit 1
# Sized for a small VPS; override with JAVA_OPTS.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
