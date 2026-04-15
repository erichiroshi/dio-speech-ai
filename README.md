<p align="center">
  <img width="25%" src="./images/logo eric hiroshi.png" alt="Eric Hiroshi Logo">
</p>

<h1 align="center">🎙️ Speech-to-Text API</h1>

<p align="center">
  API de transcrição de áudio com Whisper e Spring Boot
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-25-red?style=flat-square&logo=openjdk" alt="Java 25">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot 4">
  <img src="https://img.shields.io/badge/Speaches-Whisper-4A90D9?style=flat-square" alt="Speaches Whisper">
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white" alt="Docker Compose">
  <img src="https://img.shields.io/badge/Prometheus-E6522C?style=flat-square&logo=prometheus&logoColor=white" alt="Prometheus">
  <img src="https://img.shields.io/badge/Grafana-F46800?style=flat-square&logo=grafana&logoColor=white" alt="Grafana">
  <img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white" alt="Redis">
  <img src="https://img.shields.io/badge/Resilience4j-informational?style=flat-square" alt="Resilience4j">
  <img src="https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white" alt="JWT">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="MIT License">
</p>

---

## 📋 Sobre o projeto

Solução desenvolvida para o desafio **DIO × Globant — Java & Spring Boot AI Developer**.

API REST para transcrição de áudio com **Whisper** via [Speaches](https://github.com/speaches-ai/speaches), construída em três fases evolutivas: API base → observabilidade + cache → resiliência + segurança.

---

## 📑 Índice

- [🗺️ Roadmap](#️-roadmap)
- [🌐 Documentação](#-documentação)
- [🛠️ Stack](#️-stack)
- [🏗️ Arquitetura](#️-arquitetura)
- [⚙️ Pré-requisitos](#️-pré-requisitos)
- [🚀 Quick Start](#-quick-start)
- [📡 Endpoint de transcrição](#-endpoint-de-transcrição)
- [🔐 Autenticação JWT](#-autenticação-jwt)
- [📊 Observabilidade](#-observabilidade)
- [🧪 Testando a API](#-testando-a-api)
- [🔧 Variáveis de ambiente](#-variáveis-de-ambiente)
- [📁 Estrutura do projeto](#-estrutura-do-projeto)
- [⚠️ Troubleshooting](#️-troubleshooting)
- [Autor](#autor)

---

## 🗺️ Roadmap

**[Ver Roadmap completo — Fases 1, 2 e 3](https://erichiroshi.github.io/dio-speech-ai/roadmap_dio_speech_ai.html)**

| Fase | Versões | Status |
|---|---|---|
| Fase 1 — API base | v1.0.0 | ✅ Concluída |
| Fase 2 — Observabilidade + Cache | v2.1.0 → v2.7.0 | ✅ Concluída |
| Fase 3 — Resiliência + Segurança | v3.1.0 → v3.5.0 | ✅ Concluída |

---

## 🌐 Documentação

👉 https://erichiroshi.github.io/dio-speech-ai/

---

## 🛠️ Stack

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 | Linguagem principal |
| Spring Boot | 4.x | Framework web |
| Lombok | — | `@RequiredArgsConstructor`, `@Slf4j` |
| Spring WebFlux (WebClient) | — | Integração HTTP com Speaches |
| Speaches | latest-cuda | Servidor Whisper (transcrição) |
| Docker / Docker Compose | — | Containerização e orquestração |
| Actuator + Micrometer | — | Métricas e healthcheck |
| Prometheus | Latest | Scraping via `/actuator/prometheus` |
| Grafana | Latest | Dashboards com provisioning automático |
| Logback + logstash-encoder | 9.0 | Logs estruturados JSON |
| Zipkin + OpenTelemetry | — | Tracing distribuído |
| Redis | 8-alpine | Cache de transcrições por SHA-256 |
| Resilience4j | 2.4.0 | CircuitBreaker + Retry com backoff |
| Spring Security + Nimbus JWT | 9.37.3 | Autenticação stateless HS256 |
| Testcontainers | 1.19.8 | Testes de integração com Redis real |

---

## 🏗️ Arquitetura

```
Cliente
  │  POST /api/transcriptions
  │  Authorization: Bearer <token>
  ▼
Spring Boot API  :8080
  │
  ├── SecurityFilterChain → JwtAuthFilter → valida token
  ├── MdcLoggingFilter    → requestId, httpMethod, requestUri
  │
  ├── TranscriptionService
  │     ├── CacheService (SHA-256 → Redis)
  │     │     └── HIT  → retorna cached=true (sem Whisper)
  │     │     └── MISS → chama Whisper
  │     │
  │     └── SpeechToTextClient
  │           ├── @Retry (3x, backoff 500ms→2s)
  │           └── @CircuitBreaker (CLOSED/OPEN/HALF_OPEN)
  │                 └── POST speaches:8000/v1/audio/transcriptions
  │
  └── TranscriptionMetrics (Micrometer)
        requests.total · whisper.duration · file.size · cache.hit/miss

Infraestrutura (Docker network: backend)
  ├── speaches    :8000  — Whisper GPU
  ├── redis       :6379  — Cache (TTL 24h)
  ├── prometheus  :9090  — Scraping métricas
  ├── grafana     :3000  — Dashboards
  └── zipkin      :9411  — Tracing
```

---

## ⚙️ Pré-requisitos

- Docker + Docker Compose v2+
- GPU NVIDIA (para Speaches CUDA)
- JDK 25 (modo dev)
- `uv` — CLI do Speaches (para baixar modelos)

> Sem GPU? Altere no compose: `image: ghcr.io/speaches-ai/speaches:latest-cpu`

---

## 🚀 Quick Start

| Modo | Infraestrutura | Aplicação |
|---|---|---|
| **dev** | `docker compose -f docker-compose.dev.yml up -d` | `./gradlew bootRun --args='--spring.profiles.active=dev'` |
| **prod** | `./gradlew clean build && docker compose up -d` | Container (build automático) |

### Clone

```bash
git clone https://github.com/erichiroshi/dio-speech-ai.git
cd dio-speech-ai
```

### Modo Desenvolvimento

```bash
# 1. Infraestrutura (Speaches + Redis + Prometheus + Grafana + Zipkin)
docker compose -f docker-compose.dev.yml up -d

# 2. Aplicação
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Serviços disponíveis:
- API: http://localhost:8080
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- Zipkin: http://localhost:9411
- Redis: http://localhost:6379
- Speaches: http://localhost:8000

### Modo Produção

```bash
./gradlew clean build
docker compose up -d
docker compose ps   # verificar os 6 containers
```

### Baixar modelo Whisper

O modelo é baixado uma vez e cacheado no volume `hf-hub-cache` e não será re-baixado nas próximas subidas.

```bash
export SPEACHES_BASE_URL="http://localhost:8000"
uvx speaches-cli model download Systran/faster-whisper-small
```

> ⚠️ O download do modelo pode levar alguns minutos (~460 MB). Execute com o Speaches já em execução.

### Encerrar

```bash
docker compose -f docker-compose.dev.yml down   # dev
docker compose down                             # prod
docker compose down -v                          # remove volumes (apaga modelo)
```

---

## 📡 Endpoint de transcrição

### `POST /api/transcriptions`

> **Requer autenticação JWT** — ver [seção abaixo](#-autenticação-jwt).

**Request** — `multipart/form-data`

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `file` | File | ✅ | Arquivo de áudio |

**Formatos aceitos:** `audio/wav` · `audio/wave` · `audio/x-wav` · `audio/mpeg`

**Response — 200 OK (cache miss)**
```json
{ "text": "testando o áudio para a gravação", "fileSizeBytes": 461842 }
```

**Response — 200 OK (cache hit)**
```json
{ "text": "testando o áudio para a gravação", "fileSizeBytes": 461842, "cached": true }
```

**Response — 503 Service Unavailable (CircuitBreaker aberto)**
```json
{
  "type": "https://api.diospeechai/errors/service-unavailable",
  "title": "Service Unavailable",
  "status": 503,
  "detail": "Serviço de transcrição temporariamente indisponível.",
  "requestId": "a3f2c1d0-..."
}
```

---

## 🔐 Autenticação JWT

### Obter token

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"any"}' | jq -r .token)
```

### Usar token

```bash
curl -s -X POST http://localhost:8080/api/transcriptions \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@audio.wav;type=audio/wav" | jq .
```

### Endpoints públicos

| Endpoint | Motivo |
|---|---|
| `GET /actuator/health` | Healthcheck Docker |
| `GET /actuator/prometheus` | Scraping Prometheus |
| `POST /auth/token` | Obter token JWT |

### Variáveis JWT

| Variável | Padrão | Descrição |
|---|---|---|
| `JWT_SECRET` | (inseguro) | Segredo HMAC-SHA256 — **obrigatório em prod** |
| `JWT_EXPIRATION_HOURS` | `8` | Expiração em horas |

```bash
# Gerar secret seguro para produção
openssl rand -hex 32
```

---

## 📊 Observabilidade

### Endpoints Actuator

| Endpoint | Descrição |
|---|---|
| `/actuator/health` | Status + CircuitBreaker state |
| `/actuator/prometheus` | Todas as métricas |
| `/actuator/metrics` | Lista de métricas disponíveis |

### Métricas customizadas

| Métrica | Tipo | Tags |
|---|---|---|
| `transcription.requests.total` | Counter | `status=success\|error` |
| `transcription.whisper.duration` | Timer | — (p50/p95/p99) |
| `transcription.file.size.bytes` | Distribution | — |
| `transcription.cache.total` | Counter | `result=hit\|miss` |

### Métricas de resiliência (automáticas)

| Métrica | Descrição |
|---|---|
| `resilience4j_circuitbreaker_state` | Estado do CB: 0=CLOSED · 1=OPEN · 2=HALF_OPEN |
| `resilience4j_circuitbreaker_calls_*` | Chamadas por tipo (successful/failed/not_permitted) |
| `resilience4j_retry_calls_total` | Tentativas de retry por resultado |

### Grafana

Acesse http://localhost:3000 — dashboard **dio-speech-ai** abre automaticamente com 9 painéis incluindo a seção de resiliência.

### Traces

Acesse http://localhost:9411 — todos os spans de cada requisição, com `traceId` correlacionado nos logs JSON.

---

## 🧪 Testando a API

```bash
# Obter token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"any"}' | jq -r .token)

# Transcrever (cache miss)
curl -s -X POST http://localhost:8080/api/transcriptions \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@audio.wav;type=audio/wav" | jq .

# Transcrever novamente (cache hit — mesmo arquivo)
curl -s -X POST http://localhost:8080/api/transcriptions \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@audio.wav;type=audio/wav" | jq .cached
# → true

# Executar testes de integração
./gradlew test
```

---

## 🔧 Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `WHISPER_BASE_URL` | `http://speaches:8000` | URL do Speaches |
| `WHISPER_MODEL` | `Systran/faster-whisper-small` | Modelo Whisper |
| `REDIS_HOST` | `localhost` | Host do Redis |
| `REDIS_PORT` | `6379` | Porta do Redis |
| `CACHE_TTL_HOURS` | `24` | TTL das transcrições em cache |
| `ZIPKIN_BASE_URL` | `http://localhost:9411` | URL do Zipkin |
| `TRACING_SAMPLING` | `1.0` | Taxa de sampling (0.0–1.0) |
| `JWT_SECRET` | (inseguro) | Secret HMAC-SHA256 — **obrigatório em prod** |
| `JWT_EXPIRATION_HOURS` | `8` | Expiração do token |

---

## 📁 Estrutura do projeto

```
dio-speech-ai/
├── src/
│   ├── main/
│   │   ├── java/com/example/diospeechai/
│   │   │   ├── config/
│   │   │   │   ├── WebClientConfig.java
│   │   │   │   ├── RedisConfig.java            ← v2.3.0
│   │   │   │   └── MdcLoggingFilter.java       ← v2.2.0
│   │   │   ├── security/                       ← v3.4.0
│   │   │   │   ├── JwtProperties.java
│   │   │   │   ├── JwtService.java
│   │   │   │   ├── JwtValidationException.java
│   │   │   │   ├── JwtAuthFilter.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── AuthController.java
│   │   │   └── transcription/
│   │   │       ├── cache/
│   │   │       │   └── CacheService.java       ← v2.4.0
│   │   │       ├── controller/
│   │   │       │   └── TranscriptionController.java
│   │   │       ├── dto/
│   │   │       │   ├── TranscriptionResponse.java
│   │   │       │   └── WhisperResponse.java
│   │   │       ├── exception/
│   │   │       │   ├── GlobalExceptionHandler.java
│   │   │       │   ├── TranscriptionException.java
│   │   │       │   └── ServiceUnavailableException.java  ← v3.1.0
│   │   │       ├── metrics/
│   │   │       │   └── TranscriptionMetrics.java         ← v2.1.0
│   │   │       └── service/
│   │   │           ├── SpeechToTextClient.java
│   │   │           └── TranscriptionService.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── logback-spring.xml              ← v2.2.0
│   └── test/
│       └── java/com/example/diospeechai/
│           └── transcription/
│               └── TranscriptionIntegrationTest.java  ← v3.5.0
├── docs/
│   ├── index.html
│   ├── architecture.html
│   ├── observability.html
│   ├── resilience.html
│   ├── roadmap_dio_speech_ai.html
│   └── styles.css
├── monitoring/
│   ├── prometheus/
│   │   ├── prometheus.yml          ← prod
│   │   └── prometheus-dev.yml      ← dev
│   └── grafana/
│       ├── provisioning/
│       │   ├── datasources/prometheus.yml
│       │   └── dashboards/dashboards.yml
│       └── dashboards/
│           └── dio-speech-ai.json
├── docker/Dockerfile
├── docker-compose.yml              ← prod (6 serviços)
├── docker-compose.dev.yml          ← dev  (5 serviços)
├── audio.wav
├── build.gradle
└── VERSIONING.md
```

---

## ⚠️ Troubleshooting

**`"Falha na comunicação com Whisper"`**
```bash
docker compose logs speaches && docker compose ps
```

**`503 Service Unavailable` — CircuitBreaker aberto**
```bash
# Ver estado do CB
curl http://localhost:8080/actuator/health | jq .components.circuitBreakers
# Aguardar waitDurationInOpenState (30s) para voltar ao estado HALF_OPEN
```

**`401 Unauthorized`**
```bash
# Obter novo token
curl -s -X POST http://localhost:8080/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"any"}' | jq .token
```

**Prometheus não coleta métricas (modo dev)**

O `prometheus-dev.yml` aponta para `host.docker.internal:8080`. Certifique-se de que a aplicação está rodando com `./gradlew bootRun`.

**Modelo não encontrado no Speaches**
```bash
uvx speaches-cli model ls --task automatic-speech-recognition
uvx speaches-cli model download Systran/faster-whisper-small
```

---

## Autor

**Eric Hiroshi** — Backend Engineer · Java / Spring Boot

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Eric%20Hiroshi-blue)](https://www.linkedin.com/in/eric-hiroshi/)
[![GitHub](https://img.shields.io/badge/GitHub-erichiroshi-black)](https://github.com/erichiroshi)

---

## 📄 Licença

Este projeto está sob a licença [MIT](LICENSE).

---

<p align="center">
  <em>"Código limpo é aquele que expressa a intenção com simplicidade e precisão."</em>
</p>

<p align="center"><strong>Desenvolvido com ☕ e 💻</strong></p>

---

**Dúvidas?** Abra uma [issue](https://github.com/erichiroshi/dio-speech-ai/issues/new) ou me chame no [LinkedIn](https://www.linkedin.com/in/eric-hiroshi/)!
