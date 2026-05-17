# dio-speech-ai — Visão geral do projeto

## O que é o projeto

API REST de transcrição de áudio desenvolvida para o desafio **DIO × Globant — Java & Spring Boot AI Developer**. O objetivo técnico era demonstrar integração com IA generativa a partir de uma stack Java moderna, evoluindo em dez fases da funcionalidade básica até uma API pronta para produção.

O serviço recebe um arquivo de áudio via `POST /api/transcriptions`, envia para o modelo Whisper (via Speaches), e retorna a transcrição em texto. A partir da Fase 2, respostas já transcritas são servidas do cache em vez de processar novamente.

---

## As dez fases

| Fase | Versões | Foco |
|---|---|---|
| Fase 1 | v1.0.0 | API funcional: transcrição síncrona, Docker, tratamento de erros |
| Fase 2 | v2.1.0 → v2.7.0 | Maturidade: observabilidade completa + cache inteligente |
| Fase 3 | v3.1.0 → v3.5.0 | Resiliência: CircuitBreaker, Retry (JWT removido na v5.1.0) |
| Fase 4 | v4.1.0 | Documentação: Swagger UI, README atualizado |
| Fase 5 | v5.1.0 → v5.3.0 | CI/CD: GitHub Actions, Docker automatizado |
| Fase 6 | v6.x | Pendente: Bean Validation (aguardando DTOs validáveis) |
| Fase 7 | v7.1.0 → v7.6.0 | Arquitetura Hexagonal: ports, adapters, domain pur |
| Fase 8 | v8.1.0 → v8.3.0 | Mensageria: RabbitMQ, DLQ, assíncrono |
| Fase 9 | v9.1.0 → v9.3.0 | Notificações: Email, SMS, WhatsApp |
| Fase 10 | v10.1.0 → v10.2.0 | IA: Spring AI + Ollama para resumos |

---

## Arquitetura

```
Cliente HTTP
  │  POST /api/transcriptions
  ▼
Spring Boot API  :8080
  │
  ├── MdcLoggingFilter (requestId + traceId nos logs)
  │
  ├── TranscriptionService
  │     ├── CacheService → SHA-256 → Redis
  │     │     ├── HIT  → retorna em ~15ms (cached: true)
  │     │     └── MISS → chama Whisper
  │     │
  │     └── SpeechToTextClient
  │           ├── @Retry (3x, backoff exponencial 500ms→2s)
  │           └── @CircuitBreaker (CLOSED/OPEN/HALF_OPEN)
  │
  └── TranscriptionMetrics (Micrometer)

Infraestrutura
  ├── speaches    :8000  — Whisper GPU (modelo Systran/faster-whisper-small)
  ├── redis       :6379  — Cache (TTL 24h, persistência AOF)
  ├── prometheus  :9090  — Scraping métricas a cada 10s
  ├── grafana     :3000  — Dashboards (provisioning automático)
  └── zipkin      :9411  — Tracing distribuído
```

---

## Stack e por que cada escolha

### Java 25 + Spring Boot 4.x

Java 25 é a versão LTS mais recente. Spring Boot 4.x exige Java 17+ e traz melhorias significativas no startup time, consumo de memória e suporte a Virtual Threads (Project Loom). A combinação é a stack mais moderna disponível no ecossistema Spring no momento do desenvolvimento.

### Spring WebFlux (WebClient)

A chamada HTTP ao Speaches usa `WebClient` (reativo) em vez de `RestTemplate` (bloqueante legado) ou `RestClient` (novo bloqueante). A escolha do `WebClient` foi pelo suporte nativo a timeout configurável, tratamento de erros por status HTTP (`.onStatus()`), e integração natural com o ecossistema reativo.

Uma nota importante: o `WebClient` é usado com `.block()` — tornando a chamada bloqueante. Isso é um ponto de evolução futuro (migrar para fluxo reativo completo com `Mono<WhisperResponse>`), mas para a fase atual simplifica a integração com o `@CircuitBreaker` e `@Retry` do Resilience4j, que funcionam melhor com código imperativo.

### Speaches + Whisper

Speaches é um servidor de inferência compatível com a API da OpenAI para speech-to-text. Ele expõe `POST /v1/audio/transcriptions` com o mesmo contrato da API da OpenAI — isso significa que trocar o backend de IA (para Whisper hospedado, para a OpenAI real, ou para outro modelo) requer apenas mudar a `base-url`, sem alterar código.

O modelo `Systran/faster-whisper-small` foi escolhido por ser leve (~244MB) e suficientemente rápido para demonstração.

### Redis 8

Redis é o padrão para cache em APIs Java por três razões: latência sub-milissegundo, TTL nativo por chave, e suporte a persistência opcional (AOF). A versão 8-alpine foi escolhida para imagem menor (~30MB).

A serialização usa `JacksonJsonRedisSerializer` (substituto do depreciado `Jackson2JsonRedisSerializer`) com `TranscriptionResponse` tipado. O valor fica como JSON legível no Redis, permitindo consumo por outros serviços.

### Micrometer + Prometheus + Grafana

O trio padrão para observabilidade de métricas em aplicações Java:

- **Micrometer**: camada de abstração de instrumentação. O código de negócio não sabe que o destino é Prometheus.
- **Prometheus**: coleta por pull a cada 10s. Detecta mortes da aplicação. Modelo de dados baseado em séries temporais com labels.
- **Grafana**: visualização. Provisioning automático via arquivos versionados no repositório — zero configuração manual.

O dashboard inclui 12 painéis cobrindo métricas de negócio (transcrições, cache, Whisper) e métricas de resiliência (CircuitBreaker, Retry).

### Logback + logstash-logback-encoder

Logs JSON estruturados com campos MDC de primeiro nível. A escolha pelo `logstash-logback-encoder` foi pela integração nativa com o Logback existente e pelo suporte a campos MDC como campos raiz (não aninhados sob `mdc`).

O perfil `dev` usa texto colorido para leitura no terminal. O perfil `prod` usa JSON para ingestão em agregadores.

### Zipkin + OpenTelemetry

Terceiro pilar de observabilidade. A ponte `micrometer-tracing-bridge-otel` conecta o Micrometer (que o Spring Boot usa) ao SDK do OpenTelemetry. O exportador Zipkin converte os spans para o formato Zipkin.

O benefício dessa arquitetura em camadas: o destino dos traces pode ser trocado (Jaeger, Grafana Tempo, etc.) sem alterar código de instrumentação.

### Resilience4j 2.4.0

Biblioteca de resiliência para Java. O módulo `resilience4j-spring-boot4` adiciona suporte ao Spring Boot 4 com autoconfigure para `@CircuitBreaker`, `@Retry`, `@Bulkhead`, etc.

A cadeia `@Retry → @CircuitBreaker` protege o `SpeechToTextClient`:
- Retry tenta até 3 vezes com backoff exponencial (500ms→1s→2s) em erros de I/O
- CircuitBreaker abre após 50% de falhas em 10 chamadas e bloqueia por 30s

As métricas do Resilience4j são expostas automaticamente via Micrometer — nenhum código adicional.

### Autenticação (descontinuada)

⚠️ JWT foi descontinuado na versão 5.1.0 por questões de simplicidade e segurança. Todas as rotas são agora públicas, sem necessidade de autenticação.

### Testcontainers + MockWebServer

Testes de integração com infraestrutura real (Redis) e Speaches simulado. Testcontainers garante paridade com o Redis real. MockWebServer (OkHttp) substitui o Speaches nos testes sem precisar subir GPU.

---

## Fluxo completo de uma requisição

### Cache miss (primeira vez com aquele áudio)

```
1. Cliente envia POST /api/transcriptions
2. MdcLoggingFilter gera requestId → MDC
3. TranscriptionController recebe o arquivo
4. TranscriptionService valida (tipo, tamanho)
5. TranscriptionService calcula SHA-256 do arquivo
6. CacheService busca no Redis → MISS
7. TranscriptionMetrics.recordCacheMiss()
8. SpeechToTextClient.transcribe() — dentro do Timer Micrometer
     @Retry → @CircuitBreaker → WebClient → Speaches → Whisper
9. TranscriptionMetrics.recordWhisperCall() registra duração
10. CacheService armazena no Redis com TTL 24h
11. TranscriptionMetrics.recordSuccess()
12. Retorna TranscriptionResponse { text, fileSizeBytes }
13. Log JSON emitido com requestId, fileName, fileSizeBytes, whisperModel, traceId
```

### Cache hit (mesmo áudio de novo)

```
1-6. igual ao cache miss
7. CacheService busca no Redis → HIT
8. TranscriptionMetrics.recordCacheHit()
9. Retorna TranscriptionResponse { text, fileSizeBytes, cached: true }
   (Speaches não é chamado)
```

### CircuitBreaker aberto (Speaches fora do ar)

```
1-8. igual ao cache miss
9. @CircuitBreaker detecta circuito OPEN → fallback imediato
10. ServiceUnavailableException lançada
11. GlobalExceptionHandler retorna 503 ProblemDetail com requestId
```

---

## Métricas disponíveis

| Métrica | Tipo | Descrição |
|---|---|---|
| `transcription.requests.total{status=success\|error}` | Counter | Total de transcrições |
| `transcription.whisper.duration` | Timer | Latência do Whisper (p50/p95/p99) |
| `transcription.file.size.bytes` | Distribution | Tamanhos dos arquivos |
| `transcription.cache.total{result=hit\|miss}` | Counter | Cache hit/miss |
| `resilience4j_circuitbreaker_state` | Gauge | 0=CLOSED, 1=OPEN, 2=HALF_OPEN |
| `resilience4j_circuitbreaker_calls_*` | Counter | Chamadas por tipo |
| `resilience4j_retry_calls_total` | Counter | Retries por resultado |
| `http_server_requests_seconds_*` | Timer | Métricas HTTP automáticas |

---

## Decisões que eu faria diferente

**WebClient com .block()**: o código mistura paradigma reativo (WebClient) com imperativo (block). A solução correta seria usar `Mono<WhisperResponse>` no `SpeechToTextClient` e propagar o reativo até o controller, ou migrar completamente para `RestClient` (bloqueante, mas moderno). O `.block()` funciona, mas desperdiça o potencial do WebClient.

**AuthController sem validação real**: o `POST /auth/token` aceita qualquer senha. Em produção, precisaria de integração com um IdP (Keycloak, Auth0) ou um repositório de usuários com senhas hashed (BCrypt).

**Cache sem invalidação**: transcrições ficam no Redis por 24h sem nenhum mecanismo de invalidação. Se o conteúdo de um áudio mudar (improvável, mas possível), o cache retornaria dados obsoletos. Uma estratégia de invalidação baseada em versão ou em eventos seria o próximo passo.

---

## Como rodar

```bash
# Modo dev: infraestrutura no Docker, app local
docker compose -f docker-compose.dev.yml up -d
./gradlew bootRun --args='--spring.profiles.active=dev'

# Modo prod: stack completa
./gradlew clean build
docker compose up -d

# Baixar modelo Whisper (primeira vez)
uvx speaches-cli model download Systran/faster-whisper-small

# Transcrever
curl -s -X POST http://localhost:8080/api/transcriptions \
  -F "file=@audio.wav;type=audio/wav" | jq .
```

---

## Repositório

- **Código**: https://github.com/erichiroshi/dio-speech-ai
- **Documentação**: https://erichiroshi.github.io/dio-speech-ai/
