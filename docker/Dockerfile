# ── Stage 1: build ────────────────────────────────────────────────────────────
# Usa JDK completo apenas para compilar e empacotar o JAR
FROM eclipse-temurin:25-jdk-jammy AS builder

WORKDIR /app

# Copia wrapper e configs de build primeiro (camadas de cache separadas)
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle/ gradle/

# Baixa dependências em camada separada — só invalida quando build.gradle muda
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --no-daemon --quiet

# Copia o código-fonte
COPY src/ src/

# Compila e empacota sem rodar testes (testes rodaram no CI antes do build)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew clean bootJar -x test --no-daemon --quiet

# ── Stage 2: runtime ──────────────────────────────────────────────────────────
# Imagem final enxuta — apenas JRE, sem ferramentas de build
FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

# curl para o HEALTHCHECK
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Usuário não-root para segurança
RUN addgroup --system spring \
    && adduser --system --ingroup spring spring

# Copia apenas o JAR do stage de build
COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown -R spring:spring /app

USER spring

EXPOSE 8080

ENTRYPOINT ["java", \
            "--enable-preview", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "/app/app.jar"]

HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1