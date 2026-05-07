<p align="center">
  <img width="25%" src="./images/logo eric hiroshi.png" alt="Eric Hiroshi Logo">
</p>

<h1 align="center">🎙️ Speech-to-Text API</h1>

<p align="center">
  API de transcrição de áudio com Whisper/Spring Boot e resumo via Spring AI
</p>

![CI](https://github.com/erichiroshi/dio-speech-ai/actions/workflows/ci.yml/badge.svg)
![PDF](https://github.com/erichiroshi/dio-speech-ai/actions/workflows/docker.yml/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=erichiroshi_dio-speech-ai&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=erichiroshi_dio-speech-ai)
[![codecov](https://codecov.io/gh/erichiroshi/dio-speech-ai/graph/badge.svg?token=HehyExb9gN)](https://codecov.io/gh/erichiroshi/dio-speech-ai)

<p align="center">
  <img src="https://img.shields.io/badge/Java-25-red?style=flat-square&logo=openjdk" alt="Java 25">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot 4">
  <img src="https://img.shields.io/badge/Speaches-Whisper-4A90D9?style=flat-square" alt="Speaches Whisper">
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white" alt="Docker Compose">
  <img src="https://img.shields.io/badge/Prometheus-E6522C?style=flat-square&logo=prometheus&logoColor=white" alt="Prometheus">
  <img src="https://img.shields.io/badge/Grafana-F46800?style=flat-square&logo=grafana&logoColor=white" alt="Grafana">
  <img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white" alt="Redis">
  <img src="https://img.shields.io/badge/Resilience4j-informational?style=flat-square" alt="Resilience4j">
  <img src="https://img.shields.io/badge/OpenAPI-3.1-6BA539?style=flat-square&logo=openapiinitiative&logoColor=white" alt="OpenAPI 3.1">
  <img src="https://img.shields.io/badge/Jacoco-70%25-brightgreen?style=flat-square" alt="Jacoco">
  <img src="https://img.shields.io/badge/SonarCloud-passing-4E9BCD?style=flat-square&logo=sonarcloud&logoColor=white" alt="SonarCloud">
  <img src="https://img.shields.io/badge/GitHub_Actions-CI%2FCD-2088FF?style=flat-square&logo=githubactions&logoColor=white" alt="GitHub Actions">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="MIT License">
</p>
---

## 📋 Sobre o projeto

Solução desenvolvida para o desafio **DIO × Globant — Java & Spring Boot AI Developer**.

API REST para transcrição de áudio com **Whisper** via [Speaches](https://github.com/speaches-ai/speaches), construída em quatro fases evolutivas: API base → observabilidade + cache → resiliência + segurança → documentação.

---

## 📑 Índice

- [🗺️ Roadmap](#️-roadmap)
- [🌐 Documentação](#-documentação)
- [🛠️ Stack](#️-stack)
- [🏗️ Arquitetura](#️-arquitetura)
- [⚙️ Pré-requisitos](#️-pré-requisitos)
- [🚀 Quick Start](#-quick-start)
- [📄 Swagger UI](#-swagger-ui)
- [📡 Endpoint de transcrição](#-endpoint-de-transcrição)
- [🔐 Autenticação JWT](#-autenticação-jwt)
- [📊 Observabilidade](#-observabilidade)
- [🧪 Testando a API](#-testando-a-api)
- [🔧 Variáveis de ambiente](#-variáveis-de-ambiente)
- [🔀 Fluxo real de execução](#-fluxo-real-de-execução)
- [📁 Estrutura do projeto](#-estrutura-do-projeto)
- [⚠️ Troubleshooting](#️-troubleshooting)
- [Autor](#autor)

---
 
## 🗺️ Roadmap
 
**[Ver Roadmap completo — Fases 1 a 9](https://erichiroshi.github.io/dio-speech-ai/roadmap_dio_speech_ai.html)**
 
| Fase | Versões | Status |
|---|---|---|
| Fase 1 — API base | v1.0.0 | ✅ Concluída |
| Fase 2 — Observabilidade + Cache | v2.1.0 → v2.7.0 | ✅ Concluída |
| Fase 3 — Resiliência + Segurança | v3.1.0 → v3.5.0 | ✅ Concluída |
| Fase 4 — Documentação | v4.1.0 | ✅ Concluída |
| Fase 5 — CI/CD | v5.1.0 → v5.3.0 | ✅ Concluída |
| Fase 6 — Bean Validation | v6.x | ⏸️ Pendente — sem DTOs validáveis no momento |
| Fase 7 — Arquitetura Hexagonal | v7.1.0 → v7.6.0 | ✅ Concluída |
| Fase 8 — RabbitMQ | v8.1.0 → v8.3.0 | ✅ Concluída |
| Fase 9 — Notificações | v9.1.0 → v9.3.0 | ✅ Concluída |
| Fase 10 — Spring AI + Ollama | v10.1.0 → v10.2.0 | ✅ Concluída |
 
---

## 🌐 Documentação

👉 https://erichiroshi.github.io/dio-speech-ai/
 
| Página | Descrição |
|---|---|
| [Home](https://erichiroshi.github.io/dio-speech-ai/) | Visão geral e quick start |
| [Arquitetura](https://erichiroshi.github.io/dio-speech-ai/architecture.html) | C4 Model — Contexto, Containers, Componentes |
| [Observabilidade](https://erichiroshi.github.io/dio-speech-ai/observability.html) | Métricas, Logs JSON, Tracing |
| [Resiliência](https://erichiroshi.github.io/dio-speech-ai/resilience.html) | CircuitBreaker, Retry, estados e configuração |
| [API Reference](https://erichiroshi.github.io/dio-speech-ai/swagger.html) | Endpoints, formatos, exemplos de request/response |
 
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
| Zipkin + OpenTelemetry | Latest | Tracing distribuído |
| Redis | 8.6.2-alpine | Cache de transcrições por SHA-256 |
| Resilience4j | 2.4.0 | CircuitBreaker + Retry com backoff |
| Testcontainers | 2.0.4 | Testes de integração com Redis real |
| RabbitMQ | 3-management-alpine | Mensageria assíncrona + DLQ |
| Spring AMQP | — | Producer + consumer + DLQ |
| Spring Mail | — | Notificações por e-mail (SMTP) |
| GreenMail | 2.1.3 | Servidor SMTP embarcado nos testes |
| MockWebServer | 5.3.2 | Simula Speaches, SMS e WhatsApp nos testes |
| Spring AI | 2.0.0-M5 | ChatClient + Ollama integration |
| Ollama | latest | LLM local offline — sem API key |
| SpringDoc OpenAPI | 3.0.3 | Spec OAS 3.1 + Swagger UI |
| Jacoco | 0.8.14 | Cobertura de testes (threshold 70%) |
| SonarCloud | 7.2.3.7755 | Análise estática + Quality Gate |
| GitHub Actions | — | CI/CD: testes, Docker, releases, PDF |
| SonarQube local | — | Análise estática no docker-compose.dev.yml |

---
 
### POST /api/transcriptions/{audioHash}/analysis
 
Gera um resumo da transcrição identificada pelo `audioHash` (retornado por `POST /api/transcriptions`).
 
**Response 200 — gerado agora:**
```json
{ "audioHash": "a1b2c3...", "summary": "O áudio discute...", "model": "llama3.2:3b" }
```
 
**Response 200 — cache hit (~15ms):**
```json
{ "audioHash": "a1b2c3...", "summary": "O áudio discute...", "model": "llama3.2:3b", "cached": true }
```
 
**Response 404 — transcrição não encontrada no cache**
 
**Response 503 — Ollama indisponível**
 
---
 
## 🏗️ Arquitetura
 
```
Cliente HTTP
  │  POST /api/transcriptions
  ▼
TranscriptionController  (adapter/in/http)
  │  MultipartFile → TranscribeCommand(bytes, filename, contentType, size)
  ▼
TranscribeAudioUseCase  (application — implements TranscribeAudioPort)
  │  1. validate(contentType) → 400 se tipo inválido
  │  2. sha256(bytes) → cache.get() → HIT retorna com cached=true
  │  3. MISS → speechPort.transcribe(bytes) → cache.put() → event.publish()
  │  └── TranscriptionMetrics
  │
  ├── WhisperAdapter → @Retry + @CircuitBreaker → speaches:8000
  ├── RedisTranscriptionCacheAdapter → Redis (TTL 24h)
  └── NoOpTranscriptionEventAdapter → log (RabbitMQ na Fase 8)
 
shared/config/    MdcLoggingFilter · OpenApiConfig · WebClientConfig · RedisConfig
shared/exception/ GlobalExceptionHandler (ProblemDetail RFC 9457)
 
Infraestrutura (Docker network: backend)
  ├── speaches    :8000  — Whisper
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
# 1. Infraestrutura (Speaches + Redis + Prometheus + Grafana + Zipkin + Sonarqube)
docker compose -f docker-compose.dev.yml up -d

# 2. Aplicação
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Serviços disponíveis:
- API: http://localhost:8080
- **Swagger UI: http://localhost:8080/swagger-ui.html**
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- Zipkin: http://localhost:9411
- Redis: http://localhost:6379
- Speaches: http://localhost:8000
- Sonarqube: http://localhost:9000

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

## 📄 Swagger UI
 
Com a aplicação em execução, acesse a documentação interativa:
 
```
http://localhost:8080/swagger-ui.html
```

### Endpoints da documentação
 
| URL | Descrição |
|---|---|
| `/swagger-ui.html` | Swagger UI interativo |
| `/v3/api-docs` | Spec OpenAPI 3.1 (JSON) |
| `/v3/api-docs.yaml` | Spec OpenAPI 3.1 (YAML) |
 
---

## 📡 Endpoint de transcrição

### `POST /api/transcriptions`

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
# Transcrever (cache miss)
curl -s -X POST http://localhost:8080/api/transcriptions \
  -F "file=@audio.wav;type=audio/wav" | jq .

# Transcrever novamente (cache hit — mesmo arquivo)
curl -s -X POST http://localhost:8080/api/transcriptions \
  -F "file=@audio.wav;type=audio/wav" | jq .cached
# → true

# Executar testes de integração
./gradlew test

# Inspecionar chaves no Redis
docker exec -it redis redis-cli keys 'transcription:*'
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
| `RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Porta AMQP |
| `RABBITMQ_USER` | `admin` | Usuário RabbitMQ |
| `RABBITMQ_PASS` | `admin` | Senha RabbitMQ |
| `NOTIFICATION_CHANNEL` | `EMAIL` | Canal ativo: EMAIL, SMS, WHATSAPP |
| `NOTIFICATION_RECIPIENT` | `noreply@example.com` | Destinatário padrão |
| `NOTIFICATION_EMAIL_FROM` | `noreply@diospeechai.com` | Remetente e-mail |
| `MAIL_HOST` | `smtp.gmail.com` | Servidor SMTP |
| `MAIL_PORT` | `587` | Porta SMTP |
| `MAIL_USERNAME` | — | Usuário SMTP |
| `MAIL_PASSWORD` | — | Senha / App Password |
| `NOTIFICATION_SMS_BASE_URL` | `https://api.vonage.com` | URL base SMS |
| `NOTIFICATION_SMS_API_KEY` | — | API key SMS (Twilio, Vonage...) |
| `NOTIFICATION_WHATSAPP_BASE_URL` | `https://graph.facebook.com` | Meta Cloud API |
| `NOTIFICATION_WHATSAPP_ACCESS_TOKEN` | — | Token de acesso WhatsApp |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | URL do Ollama |
| `OLLAMA_MODEL` | `llama3.2:3b` | Modelo LLM local |
| `CACHE_SUMMARY_TTL_HOURS` | `72` | TTL do cache de resumos |
 
---

## 🔀 Fluxo real de execução

  <img width="100%" src="images/mermaid-diagram.png" alt="mermaid-diagram">

---
 
## 📁 Estrutura do projeto
 
```
src/main/java/com/example/diospeechai/
├── DioSpeechAiApplication.java
├── shared/
│   ├── config/
│   │   ├── MdcLoggingFilter.java        ← requestId + MDC
│   │   ├── OpenApiConfig.java           ← SpringDoc sem JWT
│   │   ├── RedisConfig.java             ← RedisTemplate<String, TranscriptionResult>
│   │   └── WebClientConfig.java
│   └── exception/
│       └── GlobalExceptionHandler.java  ← ProblemDetail RFC 9457
├── transcription/
│   ├── domain/                          ← zero import de framework
│   │   ├── model/Transcription.java
│   │   └── port/
│   │       ├── in/TranscribeAudioPort.java
│   │       └── out/ SpeechToTextPort · TranscriptionCachePort · TranscriptionEventPort
│   ├── application/
│   │   ├── TranscribeAudioUseCase.java  ← orquestração pura
│   │   ├── command/TranscribeCommand.java
│   │   └── result/TranscriptionResult.java
│   ├── infrastructure/                  ← adapters de saída
│   │   ├── speechtotext/whisper/WhisperAdapter.java
│   │   ├── cache/redis/RedisTranscriptionCacheAdapter.java
│   │   └── messaging/rabbit/RabbitTranscriptionEventAdapter.java
│   ├── adapter/
│   │   ├── in/http/TranscriptionController.java
│   │   ├── in/messaging/TranscriptionRequestConsumer.java
│   │   └── out/messaging/event/TranscriptionCompletedEvent.java
│   ├── metrics/TranscriptionMetrics.java
│   └── exception/
├── notification/
│   └── ...
└── analysis/
    ├── domain/
    │   ├── model/TranscriptionSummary.java
    │   └── port/
    │       ├── in/SummarizeTranscriptionPort.java
    │       └── out/ LanguageModelPort · SummaryStorePort
    ├── application/SummarizeTranscriptionUseCase.java
    ├── infrastructure/
    │   ├── ollama/OllamaLanguageModelAdapter.java   ← Spring AI ChatClient
    │   └── cache/RedisSummaryStoreAdapter.java       ← TTL 72h
    └── adapter/in/http/TranscriptionAnalysisController.java
    ├── domain/
    │   ├── model/ NotificationChannel · NotificationRequest
    │   └── port/NotificationPort.java   ← interface de cada canal
    ├── application/NotifyUseCase.java   ← delega ao adapter pelo canal
    ├── adapter/in/messaging/TranscriptionCompletedConsumer.java
    └── infrastructure/
        ├── email/EmailNotificationAdapter.java  ← Spring Mail
        ├── sms/SmsNotificationAdapter.java      ← WebClient (Twilio, Vonage...)
        └── whatsapp/WhatsAppNotificationAdapter.java  ← Meta Cloud API
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
