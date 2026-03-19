FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN addgroup --system spring \
    && adduser --system --ingroup spring spring

COPY build/libs/*.jar app.jar

RUN chown -R spring:spring /app

USER spring

EXPOSE 8761

ENTRYPOINT ["java", \
            "--enable-preview", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "/app/app.jar"]

HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=5 \
  CMD curl -f http://localhost:8761/actuator/health || exit 1