# dio-speech-ai

API REST de transcricao de audio com Whisper via Speaches, com mensageria assincrona e notificacoes multicanal.

Versao: 9.3.0 | Java 25 | Spring Boot 4.x

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

### Modo producao (stack completa no Docker)

```bash
./gradlew clean build -x test
docker compose up -d
```

### Baixar modelo Whisper

```bash
export SPEACHES_BASE_URL="http://localhost:8000"
uvx speaches-cli model download Systran/faster-whisper-small
```

---

## Endpoints

### POST /api/transcriptions

Transcreve um arquivo de audio.

**Request:** `multipart/form-data`

| Campo | Tipo | Obrigatorio | Descricao |
|-------|------|-------------|-----------|
| file  | File | sim         | Arquivo de audio (max 5MB) |

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

**Response 400 — arquivo invalido ou ausente:**

```json
{
  "type": "https://api.diospeechai/errors/transcription-exception",
  "title": "Transcription Exception",
  "status": 400,
  "detail": "Tipo de arquivo invalido: 'application/pdf'.",
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

Retorna status da aplicacao, CircuitBreaker e RabbitMQ.

### GET /actuator/prometheus

Metricas no formato Prometheus.

---

## Mensageria — RabbitMQ

### Publicar pedido de transcricao (consumer)

Para transcrever via fila em vez de HTTP, publicar em:

- **Exchange:** `transcription.requests.exchange`
- **Routing key:** `transcription.requests`

**Payload:**

```json
{
  "audioBase64": "base64encodedaudio==",
  "filename": "audio.wav",
  "contentType": "audio/wav",
  "fileSizeBytes": 461842,
  "requestId": "uuid-opcional"
}
```

### Assinar resultados de transcricao

Para receber o evento apos cada transcricao:

- **Exchange:** `transcription.events` (topic)
- **Routing key:** `transcription.completed`

**Payload do evento:**

```json
{
  "transcriptionId": "uuid",
  "audioHash": "sha256hex",
  "text": "conteudo transcrito",
  "fileSizeBytes": 461842,
  "completedAt": "2026-04-27T10:00:00Z"
}
```

**Dead-Letter Queue:** mensagens que falham apos 3 tentativas vao para `transcription.requests.dlq`.

---

## Variaveis de ambiente

| Variavel | Padrao | Descricao |
|----------|--------|-----------|
| `WHISPER_BASE_URL` | `http://speaches:8000` | URL do Speaches |
| `WHISPER_MODEL` | `Systran/faster-whisper-small` | Modelo Whisper |
| `REDIS_HOST` | `localhost` | Host do Redis |
| `REDIS_PORT` | `6379` | Porta do Redis |
| `CACHE_TTL_HOURS` | `24` | TTL das transcricoes em cache |
| `RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Porta AMQP |
| `RABBITMQ_USER` | `admin` | Usuario RabbitMQ |
| `RABBITMQ_PASS` | `admin` | Senha RabbitMQ |
| `NOTIFICATION_CHANNEL` | `EMAIL` | Canal ativo: EMAIL, SMS ou WHATSAPP |
| `NOTIFICATION_RECIPIENT` | `noreply@example.com` | Destinatario padrao |
| `NOTIFICATION_EMAIL_FROM` | `noreply@diospeechai.com` | Remetente e-mail |
| `MAIL_HOST` | `smtp.gmail.com` | Servidor SMTP |
| `MAIL_PORT` | `587` | Porta SMTP |
| `MAIL_USERNAME` | — | Usuario SMTP |
| `MAIL_PASSWORD` | — | Senha SMTP / App Password |
| `NOTIFICATION_SMS_BASE_URL` | `https://api.vonage.com` | URL base provedor SMS |
| `NOTIFICATION_SMS_SEND_PATH` | `/v1/sms` | Path de envio SMS |
| `NOTIFICATION_SMS_API_KEY` | — | API key SMS (Twilio, Vonage...) |
| `NOTIFICATION_SMS_SENDER_ID` | `DioSpeech` | Identificador remetente SMS |
| `NOTIFICATION_WHATSAPP_BASE_URL` | `https://graph.facebook.com` | Meta Cloud API |
| `NOTIFICATION_WHATSAPP_SEND_PATH` | `/v19.0/.../messages` | Path de envio WhatsApp |
| `NOTIFICATION_WHATSAPP_ACCESS_TOKEN` | — | Token de acesso WhatsApp |
| `ZIPKIN_BASE_URL` | `http://localhost:9411` | URL do Zipkin |
| `TRACING_SAMPLING` | `1.0` | Taxa de sampling (0.0 a 1.0) |

---

## Servicos (Docker Compose)

| Servico | Porta | Descricao |
|---------|-------|-----------|
| dio-speech-ai | 8080 | API Spring Boot |
| speaches | 8000 | Servidor Whisper |
| redis | 6379 | Cache de transcricoes |
| rabbitmq | 5672 | Broker AMQP |
| rabbitmq (mgmt) | 15672 | Interface de gerenciamento (admin/admin) |
| prometheus | 9090 | Coleta de metricas |
| grafana | 3000 | Dashboards (admin/admin) |
| zipkin | 9411 | Tracing distribuido |

---

## Cache

O cache usa SHA-256 do conteudo binario do arquivo como chave.
Dois arquivos com o mesmo conteudo (independente do nome) retornam a mesma transcricao.

Chave Redis: `transcription:{sha256hex}`
TTL padrao: 24 horas (configuravel via CACHE_TTL_HOURS)

Cache hit retorna em ~15ms. Cache miss depende do Whisper (~800ms).

---

## Resiliencia

CircuitBreaker (Resilience4j) protege a chamada ao Whisper:

- Abre apos 50% de falhas em 10 chamadas (minimo 5)
- Permanece aberto 30 segundos
- Retry: 3 tentativas com backoff exponencial (500ms, 1s, 2s)

Ver estado: `GET /actuator/health`

RabbitMQ consumer com retry automatico:

- 3 tentativas com backoff (1s, 2s, 4s)
- Apos 3 falhas: mensagem encaminhada para DLQ sem perda

---

## Observabilidade

- Metricas: `GET /actuator/prometheus` → Prometheus → Grafana (porta 3000)
- Logs: JSON estruturado com requestId, fileName, fileSizeBytes, traceId
- Traces: Zipkin (porta 9411) — correlacionar pelo traceId nos logs

---

## Notificacoes

Apos cada transcricao bem-sucedida, um evento e publicado no RabbitMQ.
O servico de notificacao consome o evento e envia pelo canal configurado.

| Canal | Implementacao | Configuracao |
|-------|--------------|--------------|
| E-mail | Spring Mail (SMTP) | MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD |
| SMS | WebClient REST | NOTIFICATION_SMS_BASE_URL, NOTIFICATION_SMS_API_KEY |
| WhatsApp | WebClient REST | NOTIFICATION_WHATSAPP_BASE_URL, NOTIFICATION_WHATSAPP_ACCESS_TOKEN |

Para trocar de provedor SMS ou WhatsApp: alterar as variaveis de ambiente.
Para trocar o canal ativo: `NOTIFICATION_CHANNEL=EMAIL|SMS|WHATSAPP`.

---

## CI/CD

- `ci.yml`: testes + Jacoco (70%) + SonarCloud + Codecov (todo push)
- `docker.yml`: build multistage + push Docker Hub + Trivy (todo push)
- `release.yml`: re-tag imagem + GitHub Release (em tags v*)
- `docs.yml`: gera este PDF e anexa ao release (em tags v*)

Imagem Docker: `erichiroshi/dio-speech-ai`

---

## Links

- GitHub: https://github.com/erichiroshi/dio-speech-ai
- Documentacao: https://erichiroshi.github.io/dio-speech-ai/
- Roadmap: https://erichiroshi.github.io/dio-speech-ai/roadmap_dio_speech_ai.html
- Docker Hub: https://hub.docker.com/r/erichiroshi/dio-speech-ai
- RabbitMQ Management: http://localhost:15672 (admin/admin)
- Swagger UI: http://localhost:8080/swagger-ui.html