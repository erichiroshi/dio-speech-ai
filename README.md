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
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="MIT License">
</p>



---

## 📋 Sobre o projeto

Solução desenvolvida para o desafio **DIO × Globant — Java & Spring Boot AI Developer**.

O objetivo é construir uma API REST capaz de receber arquivos de áudio e retornar a transcrição em texto, integrando o modelo **Whisper** (via [Speaches](https://github.com/speaches-ai/speaches)) como motor de reconhecimento de fala.

O projeto demonstra integração com IA generativa a partir de uma stack Java moderna, separação de responsabilidades, tratamento de erros com `ProblemDetail` (RFC 9457) e infraestrutura containerizada com healthcheck e observabilidade via Micrometer + Prometheus.

---

## 📑 Índice

- [📋 Sobre o projeto](#-sobre-o-projeto)
- [📑 Índice](#-índice)
- [🗺️ Roadmap](#️-roadmap)
- [🌐 Documentação](#-documentação)
- [🛠️ Stack](#️-stack)
- [🏗️ Arquitetura](#️-arquitetura)
- [⚙️ Pré-requisitos](#️-pré-requisitos)
- [🚀 Quick Start](#-quick-start)
  - [Clone o projeto](#clone-o-projeto)
  - [🟢 Modo Desenvolvimento (recomendado para avaliação)](#-modo-desenvolvimento-recomendado-para-avaliação)
    - [1️⃣ Subir infraestrutura (Speaches + Prometheus)](#1️⃣-subir-infraestrutura-speaches--prometheus)
    - [2️⃣ Subir a aplicação](#2️⃣-subir-a-aplicação)
  - [🏭 Modo Produção (simulado)](#-modo-produção-simulado)
  - [🤖 Baixando o modelo Whisper](#-baixando-o-modelo-whisper)
  - [🧯 Encerrar ambiente](#-encerrar-ambiente)
- [📡 Endpoint de transcrição](#-endpoint-de-transcrição)
  - [`POST /api/transcriptions`](#post-apitranscriptions)
- [📊 Observabilidade](#-observabilidade)
- [🧪 Testando a API](#-testando-a-api)
- [🔧 Variáveis de ambiente](#-variáveis-de-ambiente)
- [📁 Estrutura do projeto](#-estrutura-do-projeto)
- [⚠️ Troubleshooting](#️-troubleshooting)
- [🔄 Atualizações de dependências](#-atualizações-de-dependências)
- [Autor](#autor)
- [📄 Licença](#-licença)

---

## 🗺️ Roadmap

**[Ver Roadmap completo — Fases 2 e 3](https://erichiroshi.github.io/dio-speech-ai/roadmap_dio_speech_ai.html)**

---

## 🌐 Documentação

Acesse a documentação completa do projeto:

👉 https://erichiroshi.github.io/dio-speech-ai/

---

## 🛠️ Stack

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 | Linguagem principal |
| Spring Boot | 4.x | Framework web |
| Lombok | — | Redução de boilerplate (`@RequiredArgsConstructor`) |
| Spring WebFlux (WebClient) | — | Integração HTTP com Speaches |
| Speaches | latest-cuda | Servidor Whisper (transcrição) |
| Docker / Docker Compose | — | Containerização e orquestração |
| Actuator + Micrometer | — | Métricas e healthcheck |
| Prometheus | Latest | Coleta de métricas via `/actuator/prometheus` |

---

## 🏗️ Arquitetura

```
Cliente (Postman / curl / Frontend)
         │
         │  POST /api/transcriptions
         │  multipart/form-data { file }
         ▼
┌─────────────────────────────────┐
│   Spring Boot API          :8080│
│                                 │
│  TranscriptionController        │
│         │                       │
│  TranscriptionService   ←───────┼── TranscriptionMetrics (Micrometer)
│         │                       │     • transcription.requests.total
│  SpeechToTextClient (WebClient) │     • transcription.whisper.duration
└────────┬────────────────────────┘     • transcription.file.size.bytes
         │
         │  POST /v1/audio/transcriptions
         ▼
┌──────────────────────────┐
│   Speaches (Whisper):8000│
│   faster-whisper-small   │
└──────────────────────────┘
         │
         ▼
┌─────────────────────────┐
│   TranscriptionResponse │
│   { text,               │
│     fileSizeBytes }     │
└─────────────────────────┘

         ┌─────────────────────────┐
         │   Prometheus       :9090│  ← scrape /actuator/prometheus cada 10s
         └─────────────────────────┘
```

---

## ⚙️ Pré-requisitos

- [Docker](https://docs.docker.com/get-docker/) + [Docker Compose](https://docs.docker.com/compose/) v2+
- GPU NVIDIA com drivers instalados (para a imagem CUDA padrão)
- `uv` — CLI do Speaches (para baixar modelos)
- JDK 25 (modo dev — execução via IDE ou `./gradlew bootRun`)

> Sem GPU? Use a imagem CPU alterando a tag no `compose.yaml`:
> ```yaml
> image: ghcr.io/speaches-ai/speaches:latest-cpu
> ```

---

## 🚀 Quick Start

O projeto possui dois modos de execução:

| Modo | Infraestrutura | Aplicação |
|---|---|---|
| **dev** | Docker Compose (`docker-compose.dev.yml`) | IDE ou `./gradlew bootRun` |
| **prod** | Docker Compose (`docker-compose.yml`) | Container (build automático) |

---

### Clone o projeto

```bash
git clone https://github.com/erichiroshi/dio-speech-ai.git
cd dio-speech-ai
```

---

### 🟢 Modo Desenvolvimento (recomendado para avaliação)

Infraestrutura via Docker, aplicação rodando local — ciclo de feedback rápido.

#### 1️⃣ Subir infraestrutura (Speaches + Prometheus)

```bash
docker compose -f docker-compose.dev.yml up -d
```

Serviços iniciados:
- Speaches (Whisper): http://localhost:8000
- Prometheus: http://localhost:9090

#### 2️⃣ Subir a aplicação

**Via Gradle:**
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

**Via IDE:**
```bash
./gradlew clean build   # gera o build
# Via IDE: Run Configuration → Environment Variables
SPRING_PROFILES_ACTIVE=dev
```
Refresh Gradle → executar `DioSpeechAiApplication`

**Acesse:**
- API: http://localhost:8080/api/transcriptions
- Health: http://localhost:8080/actuator/health
- Métricas: http://localhost:8080/actuator/prometheus

---

### 🏭 Modo Produção (simulado)

Toda a stack containerizada.

```bash
./gradlew clean build
docker compose up -d
```

O Compose irá:
1. Build da imagem da API (`docker/Dockerfile`)
2. Pull da imagem Speaches CUDA (pesada — aguarde na primeira vez)
3. Subir Speaches na porta `8000`
4. Subir a API na porta `8080` — somente após Speaches estar saudável
5. Subir Prometheus na porta `9090` — somente após a API estar saudável

```bash
docker compose logs -f     # acompanhar logs
docker compose ps          # verificar status dos 3 containers
```

---

### 🤖 Baixando o modelo Whisper

O modelo é baixado uma vez e cacheado no volume `hf-hub-cache` e não será re-baixado nas próximas subidas.

**Instale o `uv`:**

```bash
# Windows (PowerShell)
powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"

# Windows (Chocolatey)
choco install uv

# macOS / Linux
curl -LsSf https://astral.sh/uv/install.sh | sh
```

**Baixe o modelo** (com Speaches já em execução):

```bash
export SPEACHES_BASE_URL="http://localhost:8000"
uvx speaches-cli model download Systran/faster-whisper-small
```

> ⚠️ O download do modelo pode levar alguns minutos (~460 MB). Execute com o Speaches já em execução.

---

### 🧯 Encerrar ambiente

```bash
# Modo dev
docker compose -f docker-compose.dev.yml down

# Modo prod
docker compose down          # para containers
docker compose down -v       # para e remove volumes (apaga modelo)
```

---

## 📡 Endpoint de transcrição

### `POST /api/transcriptions`

Recebe um arquivo de áudio e retorna a transcrição.

**Request** — `multipart/form-data`

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `file` | File | ✅ | Arquivo de áudio |

**Formatos aceitos**

`audio/wav` · `audio/wave` · `audio/x-wav` · `audio/mpeg`

**Response — 200 OK**

```json
{
  "text": "testando o áudio para a gravação e teste da API",
  "fileSizeBytes": 461842
}
```

> ℹ️ O campo `processingTimeMs` foi removido na v2.1.0. O tempo de processamento agora é
> rastreado com precisão via `transcription.whisper.duration` (Timer Micrometer com p50/p95/p99).
> Acesse em: `GET /actuator/metrics/transcription.whisper.duration`

**Response — 400 Bad Request**

```json
{
  "type": "https://api.diospeechai/errors/transcription-exception",
  "title": "Transcription Exception",
  "status": 400,
  "detail": "Erro Whisper: modelo não encontrado",
  "instance": "/api/transcriptions",
  "timestamp": "2026-04-07T10:00:00Z"
}
```

---

## 📊 Observabilidade

Disponível a partir da **v2.1.0**.

| Endpoint | Descrição |
|---|---|
| `GET /actuator/health` | Status da aplicação |
| `GET /actuator/metrics` | Lista todas as métricas disponíveis |
| `GET /actuator/prometheus` | Métricas no formato Prometheus |

**Métricas customizadas de negócio:**

| Métrica | Tipo | Tags | Descrição |
|---|---|---|---|
| `transcription.requests.total` | Counter | `status=success\|error` | Total de transcrições |
| `transcription.whisper.duration` | Timer | — | Latência das chamadas ao Whisper (p50/p95/p99) |
| `transcription.file.size.bytes` | Distribution | — | Tamanhos dos arquivos processados |

**Prometheus UI:** http://localhost:9090

Queries úteis:
```promql
# Taxa de erros por minuto
rate(transcription_requests_total{status="error"}[1m])

# p99 de latência do Whisper
histogram_quantile(0.99, rate(transcription_whisper_duration_seconds_bucket[5m]))

# Tamanho médio dos arquivos
transcription_file_size_bytes_sum / transcription_file_size_bytes_count
```

---

## 🧪 Testando a API

Na raiz do projeto há um arquivo `audio.wav` para testes.

**Via curl:**

```bash
curl -s -X POST http://localhost:8080/api/transcriptions \
  -F "file=@audio.wav;type=audio/wav" | jq .
```

**Via curl — chamando o Speaches diretamente (sem a API):**

```bash
export SPEACHES_BASE_URL="http://localhost:8000"
export TRANSCRIPTION_MODEL_ID="Systran/faster-whisper-small"

curl -s "$SPEACHES_BASE_URL/v1/audio/transcriptions" \
  -F "file=@audio.wav" \
  -F "model=$TRANSCRIPTION_MODEL_ID" | jq .
```

**Via Postman:**

```
POST http://localhost:8080/api/transcriptions
Body → form-data
  Key: file | Type: File | Value: <selecione o arquivo>
```

**Via Speaches UI:**

```
http://localhost:8000/
1. Speech-to-Text
2. Grave ou selecione um áudio
3. Escolha o modelo: Systran/faster-whisper-small
4. Generate
```
---

## 🔧 Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `WHISPER_BASE_URL` | `http://speaches:8000` | URL do Speaches (usar nome do serviço no Docker) |
| `WHISPER_MODEL` | `Systran/faster-whisper-small` | ID do modelo no Hugging Face |

> No modo dev (`./gradlew bootRun`), o padrão aponta para `http://localhost:8000` automaticamente.

---

## 📁 Estrutura do projeto

```
dio-speech-ai/
├── src/
│   └── main/
│       ├── java/com/example/diospeechai/
│       │   └── transcription/
│       │       ├── config/
│       │       │   └── WebClientConfig.java
│       │       ├── controller/
│       │       │   └── TranscriptionController.java
│       │       ├── dto/
│       │       │   ├── TranscriptionResponse.java
│       │       │   └── WhisperResponse.java
│       │       ├── exception/
│       │       │   ├── GlobalExceptionHandler.java
│       │       │   └── TranscriptionException.java
│       │       ├── metrics/
│       │       │   └── TranscriptionMetrics.java       ← novo v2.1.0
│       │       └── service/
│       │           ├── SpeechToTextClient.java
│       │           └── TranscriptionService.java
│       └── resources/
│           └── application.yml
├── docs/
│   ├── index.html
│   ├── architecture.html
│   ├── observability.html
│   ├── resilience.html
│   ├── roadmap_dio_speech_ai.html
│   └── styles.css
├── monitoring/
│   └── prometheus/
│       ├── prometheus.yml                              ← prod (scrape container)
│       └── prometheus-dev.yml                         ← dev (scrape host.docker.internal)
├── docker/Dockerfile
├── docker-compose.yml                                  ← prod (app + speaches + prometheus)
├── docker-compose.dev.yml                             ← dev (speaches + prometheus)
├── audio.wav
└── build.gradle
```

---

## ⚠️ Troubleshooting

**`"Falha na comunicação com Whisper"` na resposta**

O Speaches ainda não está pronto ou o modelo não foi baixado. Verifique:
```bash
docker compose logs speaches
docker compose ps
```

**`Connection refused` ao chamar a API**

A aplicação não subiu. Verifique os logs:
```bash
docker compose logs app
```

**`"Tipo de arquivo inválido"`**

O cliente HTTP enviou o arquivo sem o `Content-Type` correto. Force-o explicitamente:
```bash
curl -F "file=@audio.wav;type=audio/wav" ...
```

**Modelo não encontrado no Speaches**

Verifique instalação do modelo
```bash
uvx speaches-cli model ls --task automatic-speech-recognition
```

Execute novamente o download do modelo com o Speaches em execução:
```bash
uvx speaches-cli model download Systran/faster-whisper-small
```

**`localhost` não funciona dentro do container**

Dentro do Docker, serviços se comunicam pelo nome definido no Compose:
```
✅ http://speaches:8000
❌ http://localhost:8000
```
**Prometheus não coleta métricas (modo dev)**

O `prometheus-dev.yml` aponta para `host.docker.internal:8080`. Certifique-se de que:
1. A aplicação está rodando na porta `8080`
2. O Docker tem permissão de acessar o host (padrão no Docker Desktop)

---

## 🔄 Atualizações de dependências

Este projeto utiliza Dependabot para manter dependências atualizadas automaticamente.
Atualizações são revisadas antes do merge para garantir compatibilidade.

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

<p align="center">
  <strong>Desenvolvido com ☕ e 💻</strong>
</p>

---

**Dúvidas?** Abra uma [issue](https://github.com/erichiroshi/dio-speech-ai/issues/new) ou me chame no [LinkedIn](https://www.linkedin.com/in/eric-hiroshi/)!