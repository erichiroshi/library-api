# =========================================================
# =============== BUILD STAGE =============================
# =========================================================
FROM gradle:9.3.1-jdk25 AS build
WORKDIR /app

# Copia apenas arquivos necessários primeiro (melhora cache)
COPY gradle gradle
COPY build.gradle settings.gradle gradlew ./

# Baixa dependências (camada cacheável)
RUN gradle --no-daemon dependencies || true

# Agora copia o restante
COPY src src

# Build da aplicação
RUN gradle clean bootJar --no-daemon

# =========================================================
# =============== RUNTIME STAGE ===========================
# =========================================================
FROM eclipse-temurin:25-jre-jammy

# Metadados da imagem
LABEL org.opencontainers.image.title="Library API"
LABEL org.opencontainers.image.description="Backend production-ready para gerenciamento de biblioteca"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.authors="erichiroshi"
LABEL org.opencontainers.image.url="https://github.com/erichiroshi/library-api"
LABEL org.opencontainers.image.source="https://github.com/erichiroshi/library-api"
LABEL org.opencontainers.image.licenses="MIT"

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
	
# Copia o JAR da build stage
COPY --from=build /app/build/libs/*.jar app.jar	

# Ajusta permissões
RUN chown -R spring:spring /app

# Muda para usuário não-root
USER spring

# Documenta porta exposta
EXPOSE 8080

# JVM tuning profissional para container
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:InitialRAMPercentage=50.0", \
            "-XX:MaxRAMPercentage=75.0", \
            "-XX:+ExitOnOutOfMemoryError", \
            "-XX:+AlwaysPreTouch", \
            "-XX:+UseStringDeduplication", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "/app/app.jar"]

# Healthcheck robusto
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
