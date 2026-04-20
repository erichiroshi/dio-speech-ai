# dio-speech-ai

API REST de transcrição de áudio com Whisper via Speaches.

Versão: 5.3.0 | Java 25 | Spring Boot 4.x

---

## Requisitos

- Docker e Docker Compose v2+
- GPU NVIDIA (para Speaches CUDA) ou usar tag `-cpu`
- JDK 25 (modo dev local)
- uv (CLI do Speaches para baixar modelos)

---

## Quick Start

### Modo desenvolvimento (app local, infra no Docker)

```bash
docker compose -f docker-compose.dev.yml up -d
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Modo produção (stack completa no Docker)

```bash
./gradlew clean build -x test
docker compose up -d
```

### Baixar modelo Whisper

```bash
export SPEACHES_BASE_URL="http://localhost:8000"
uvx speaches-cli model download Systran/faster-whisper-small
```

O download acontece uma vez e fica no volume `hf-hub-cache`.

---

## Endpoints

### POST /api/transcriptions

Transcreve um arquivo de áudio.

**Request:** `multipart/form-data`

| Campo | Tipo   | Obrigatório | Descrição                     |
|-------|--------|-------------|-------------------------------|
| file  | File   | sim         | Arquivo de áudio (max 5MB)    |

**Formatos aceitos:** `audio/wav`, `audio/wave`, `audio/x-wav`, `audio/mpeg`

**Response 200 — cache miss:**

```json
{
  "text": "conteudo transcrito pelo whisper",
  "fileSizeBytes": 461842
}
```

**Response 200 — cache hit:**

```json
{
  "text": "conteudo transcrito pelo whisper",
  "fileSizeBytes": 461842,
  "cached": true
}
```

**Response 400 — arquivo inválido:**

```json
{
  "type": "https://api.diospeechai/errors/transcription-exception",
  "title": "Transcription Exception",
  "status": 400,
  "detail": "Tipo de arquivo inválido: 'application/pdf'.",
  "requestId": "uuid"
}
```

**Response 503 — CircuitBreaker aberto:**

```json
{
  "type": "https://api.diospeechai/errors/service-unavailable",
  "title": "Service Unavailable",
  "status": 503,
  "detail": "Servico de transcricao temporariamente indisponivel.",
  "requestId": "uuid"
}
```

### GET /actuator/health

Retorna status da aplicacao e do CircuitBreaker.

### GET /actuator/prometheus

Metricas no formato Prometheus.

---

## Variáveis de ambiente

| Variável             | Padrão                                | Descrição                         |
|----------------------|---------------------------------------|-----------------------------------|
| WHISPER_BASE_URL     | http://localhost:8000                 | URL do Speaches                   |
| WHISPER_MODEL        | Systran/faster-whisper-small          | Modelo Whisper                    |
| REDIS_HOST           | localhost                             | Host do Redis                     |
| REDIS_PORT           | 6379                                  | Porta do Redis                    |
| CACHE_TTL_HOURS      | 24                                    | TTL do cache em horas             |
| ZIPKIN_BASE_URL      | http://localhost:9411/api/v2/spans    | Endpoint Zipkin                   |
| TRACING_SAMPLING     | 1.0                                   | Taxa de amostragem (0.0 a 1.0)    |

---

## Serviços (Docker Compose)

| Servico         | Porta  | Descrição                     |
|-----------------|--------|-------------------------------|
| dio-speech-ai   | 8080   | API Spring Boot                |
| speaches        | 8000   | Servidor Whisper               |
| redis           | 6379   | Cache de transcricoes          |
| prometheus      | 9090   | Coleta de metricas             |
| grafana         | 3000   | Dashboards (admin/admin)       |
| zipkin          | 9411   | Tracing distribuido            |

---

## Cache

O cache usa SHA-256 do conteudo binario do arquivo como chave.
Dois arquivos com o mesmo conteudo (independente do nome) retornam a mesma transcricao.

Chave Redis: `transcription:{sha256hex}`
TTL padrao: 24 horas (configuravel via CACHE_TTL_HOURS)

---

## Resiliencia

CircuitBreaker (Resilience4j) protege a chamada ao Whisper:

- Abre apos 50% de falhas em 10 chamadas (minimo 5)
- Permanece aberto 30 segundos
- Retry: 3 tentativas com backoff exponencial (500ms, 1s, 2s)

Ver estado: `GET /actuator/health`

---

## Observabilidade

- Metricas: `GET /actuator/prometheus` → Prometheus → Grafana (porta 3000)
- Logs: JSON estruturado com requestId, fileName, fileSizeBytes, traceId
- Traces: Zipkin (porta 9411) — correlacionar pelo traceId nos logs

---

## CI/CD

- ci.yml: testes + Jacoco (70%) + SonarCloud + Codecov (todo push)
- docker.yml: build multistage + push Docker Hub + Trivy (todo push)
- release.yml: re-tag imagem + GitHub Release (em tags v*)
- docs.yml: gera este PDF e anexa ao release (em tags v*)

Imagem Docker: `erichiroshi/dio-speech-ai`

---

## Links

- GitHub: https://github.com/erichiroshi/dio-speech-ai
- Documentacao: https://erichiroshi.github.io/dio-speech-ai/
- Docker Hub: https://hub.docker.com/r/erichiroshi/dio-speech-ai
