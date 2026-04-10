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
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="MIT License">
</p>

---

## 📋 Sobre o projeto

Solução desenvolvida para o desafio **DIO × Globant — Java & Spring Boot AI Developer**.

O objetivo é construir uma API REST capaz de receber arquivos de áudio e retornar a transcrição em texto, integrando o modelo **Whisper** (via [Speaches](https://github.com/speaches-ai/speaches)) como motor de reconhecimento de fala.

O projeto demonstra integração com IA generativa a partir de uma stack Java moderna, separação de responsabilidades, tratamento de erros com `ProblemDetail` (RFC 9457) e infraestrutura containerizada com healthcheck e dependências ordenadas.

---

## 📑 Índice

- [🛠️ Stack](#️-stack)
- [🏗️ Arquitetura](#️-arquitetura)
- [⚙️ Pré-requisitos](#️-pré-requisitos)
- [🐳 Subindo o ambiente](#-subindo-o-ambiente)
- [🤖 Baixando o modelo Whisper](#-baixando-o-modelo-whisper)
- [📡 Endpoint de transcrição](#-endpoint-de-transcrição)
  - [`POST /api/transcriptions`](#post-apitranscriptions)
- [🧪 Testando a API](#-testando-a-api)
- [🔧 Variáveis de ambiente](#-variáveis-de-ambiente)
- [📁 Estrutura do projeto](#-estrutura-do-projeto)
- [⚠️ Troubleshooting](#️-troubleshooting)
- [🚀 Melhorias futuras](#-melhorias-futuras)
- [Autor](#autor)
- [📄 Licença](#-licença)

---

## Roadmap para fase 2 - observabilidade + cache e fase 3 - resiliência + segurança
**[ROADMAP - Fase 2 + 3](docs/roadmap_dio_speech_ai.html)**

---

## 🛠️ Stack

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 25 | Linguagem principal |
| Spring Boot | 4.x | Framework web |
| Spring WebFlux (WebClient) | — | Integração HTTP com Speaches |
| Speaches | latest-cuda | Servidor Whisper (transcrição) |
| Docker / Docker Compose | — | Containerização e orquestração |
| Actuator | — | Healthcheck da aplicação |

---

## 🏗️ Arquitetura

```
Cliente (Postman / curl / Frontend)
         │
         │  POST /api/transcriptions
         │  multipart/form-data { file }
         ▼
┌─────────────────────────┐
│   Spring Boot API       │  :8080
│                         │
│  TranscriptionController│
│         │               │
│  TranscriptionService   │  ← validação de tipo e tamanho
│         │               │
│  SpeechToTextClient     │  ← WebClient
└────────┬────────────────┘
         │
         │  POST /v1/audio/transcriptions
         │  { file, model }
         ▼
┌─────────────────────────┐
│   Speaches (Whisper)    │  :8000
│   Systran/faster-       │
│   whisper-small         │
└─────────────────────────┘
         │
         │  { "text": "..." }
         ▼
┌─────────────────────────┐
│   TranscriptionResponse │
│   { text,               │
│     processingTimeMs,   │
│     fileSizeBytes }     │
└─────────────────────────┘
```

Os dois serviços sobem via Docker Compose. A aplicação só inicia após o Speaches passar no healthcheck (`/health`), garantindo que o modelo já está disponível.

---

## ⚙️ Pré-requisitos

- [Docker](https://docs.docker.com/get-docker/) + [Docker Compose](https://docs.docker.com/compose/) v2+
- GPU NVIDIA com drivers instalados (para a imagem CUDA padrão)
- `uv` — CLI do Speaches (para baixar modelos)

> Sem GPU? Use a imagem CPU alterando a tag no `compose.yaml`:
> ```yaml
> image: ghcr.io/speaches-ai/speaches:latest-cpu
> ```

---

## 🐳 Subindo o ambiente

```bash
docker compose up -d
```

O Compose irá:
1. Fazer build da imagem da API (`Dockerfile`)
2. Pull da imagem `ghcr.io/speaches-ai/speaches:latest-cuda` (pesada — aguarde na primeira vez)
3. Subir o Speaches na porta `8000`
4. Subir a API na porta `8080` — somente após o Speaches estar saudável

Acompanhe os logs em tempo real:
```bash
docker compose logs -f
```

Verifique se os dois containers estão de pé:
```bash
docker compose ps
```

---

## 🤖 Baixando o modelo Whisper

O modelo precisa ser baixado uma vez. Ele ficará cacheado no volume Docker `./models` e não será re-baixado nas próximas subidas.

**Instale o `uv`:**

```bash
# Windows (PowerShell)
powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"

# Windows (Chocolatey)
choco install uv

# macOS / Linux
curl -LsSf https://astral.sh/uv/install.sh | sh
```

**Baixe o modelo:**

```bash
export SPEACHES_BASE_URL="http://localhost:8000"

uvx speaches-cli model download Systran/faster-whisper-small
```

> ⚠️ O download do modelo pode levar alguns minutos (~460 MB). Execute com o Speaches já em execução.

---

## 📡 Endpoint de transcrição

### `POST /api/transcriptions`

Recebe um arquivo de áudio e retorna a transcrição.

**Request**

```
Content-Type: multipart/form-data
```

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `file` | File | ✅ | Arquivo de áudio (`.wav`, `.wave`) |

**Formatos aceitos**

`audio/wav` · `audio/wave` · `audio/x-wav` · `audio/mpeg`

**Response — 200 OK**

```json
{
  "text": "testando o áudio para a gravação e teste da API",
  "processingTimeMs": 856,
  "fileSizeBytes": 461842
}
```

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
| `WHISPER_BASE_URL` | `http://speaches:8000` | URL interna do serviço Speaches |
| `WHISPER_MODEL` | `Systran/faster-whisper-small` | ID do modelo no Hugging Face |

> Dentro do Docker, use sempre o nome do serviço (`speaches`) como host — nunca `localhost`.

---

## 📁 Estrutura do projeto

```
dio-speech-ai/
├── src/
│   └── main/
│       ├── java/com/example/diospeechai/
│       │   ├── config/
│       │   │   └── WebClientConfig.java          # Bean do WebClient
│       │   └── transcription/
│       │       ├── controller/
│       │       │   └── TranscriptionController.java
│       │       ├── dto/
│       │       │   ├── TranscriptionResponse.java
│       │       │   └── WhisperResponse.java
│       │       ├── exception/
│       │       │   ├── GlobalExceptionHandler.java
│       │       │   └── TranscriptionException.java
│       │       └── service/
│       │           ├── SpeechToTextClient.java    # Integração com Speaches
│       │           └── TranscriptionService.java  # Validação e orquestração
│       └── resources/
│           └── application.yml
├── models/                                        # Cache local dos modelos Whisper
├── audio.wav                                      # Arquivo de teste
├── docker-compose.yaml                            # Docker Compose (app + speaches GPU NVIDIA)
├── docker/Dockerfile
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

---

## 🚀 Melhorias futuras

**[ROADMAP](docs/roadmap_dio_speech_ai.html)**

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

---

**Dúvidas?** Abra uma [issue](https://github.com/erichiroshi/dio-speech-ai/issues/new) ou me chame no [LinkedIn](https://www.linkedin.com/in/eric-hiroshi/)!