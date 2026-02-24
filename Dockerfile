# =========================================================
# =============== BUILD STAGE =============================
# =========================================================
FROM gradle:9.2.1-jdk25 AS build
WORKDIR /app

# Copia apenas arquivos necessários primeiro (melhora cache)
COPY gradle gradle
COPY build.gradle settings.gradle gradlew ./

RUN gradle --no-daemon dependencies || true

# Agora copia o restante
COPY src src

RUN gradle clean bootJar --no-daemon

# =========================================================
# =============== RUNTIME STAGE ===========================
# =========================================================
FROM eclipse-temurin:25-jre

# Segurança: evitar locale/timezone inconsistentes
ENV LANG=C.UTF-8

WORKDIR /app

# Instala curl para healthcheck (leve)
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Cria usuário não-root
RUN addgroup --system spring \
    && adduser --system --ingroup spring spring
	
# Copia o JAR
COPY --from=build /app/build/libs/*.jar app.jar	

# Ajusta permissões
RUN chown -R spring:spring /app

USER spring

EXPOSE 8080

# JVM tuning profissional para container
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:InitialRAMPercentage=50.0", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-XX:+AlwaysPreTouch", "-jar", "/app/app.jar"]

# Healthcheck robusto
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

LABEL org.opencontainers.image.title="Library API"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.authors="erichiroshi"
