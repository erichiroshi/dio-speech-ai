<p align="center">
  <img width="30%" src="./images/logo eric hiroshi.png" alt="Backend Brasil Logo">
</p>

![Java](https://img.shields.io/badge/Java-25-red)
![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue)
![MIT](https://img.shields.io/badge/license-MIT-blue)

<p style="font-size: 2em"> 🚀 Speech-to-Text API (Whisper + Spring Boot)<p>
<p style="font-size: 1.5em">Desafio DIO - Globant - Java & Spring Boot AI Developer
<p style="font-size: 1.5em">API para transcrição de áudio utilizando Whisper (via Speaches) com backend em Spring Boot.

Este repositório foi desenvolvido como solução para um desafio proposto pela DIO, com o objetivo de construir uma API inteligente de processamento de áudio utilizando Spring Boot. O projeto integra recursos de reconhecimento de fala para converter áudio em dados estruturados, aplicando conceitos modernos de APIs REST, boas práticas de arquitetura e comunicação com serviços de IA.

Além da implementação funcional, o projeto busca demonstrar organização de código, separação de responsabilidades e uma base sólida para evolução, sendo adequado tanto para fins de aprendizado quanto para demonstrações técnicas alinhadas a cenários reais de mercado.

---

## 📑 Índice

- [📑 Índice](#-índice)
- [📦 Stack](#-stack)
- [⚙️ Pré-requisitos](#️-pré-requisitos)
- [🐳 Subindo o ambiente](#-subindo-o-ambiente)
- [📥 Instalando o CLI (uv)](#-instalando-o-cli-uv)
- [🤖 Baixando modelo Whisper (pt-BR)](#-baixando-modelo-whisper-pt-br)
- [📡 Endpoint de transcrição](#-endpoint-de-transcrição)
- [Teste](#teste)
- [⚠️ Troubleshooting](#️-troubleshooting)
- [📁 Estrutura do projeto](#-estrutura-do-projeto)
- [🧠 Observações técnicas](#-observações-técnicas)
- [🚀 Melhorias futuras](#-melhorias-futuras)

## 📦 Stack
- Java 25 / Spring Boot
- Docker & Docker Compose
- Whisper (via speaches)
- WebClient (integração HTTP)

## ⚙️ Pré-requisitos
- Docker + Docker Compose
- (Opcional) GPU NVIDIA configurada
- uv (para gerenciar CLI do speaches)

## 🐳 Subindo o ambiente

O Docker Compose irá:

- baixar a imagem ghcr.io/speaches-ai/speaches:latest-cuda (demora, imagem pesada!)
- criar o volume de cache do Hugging Face
- subir o serviço de transcrição (speaches)
- subir a API (dio-speaches-ai)
  
```bash
docker compose up 
```

## 📥 Instalando o CLI (uv)
- Windows (Chocolatey)
```bash
choco install uv
```
- PowerShell
```bash
powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"
```

## 🤖 Baixando modelo Whisper (pt-BR)
Necessário para transcrição de áudio.

Defina a URL do serviço:
```bash
export SPEACHES_BASE_URL="http://localhost:8000"
```
Baixe o modelo:  (demora, modelo "pesado")
```bash
uvx speaches-cli model download Systran/faster-whisper-small
```
💡 O modelo será armazenado no volume Docker (hf-hub-cache), evitando downloads futuros.

## 📡 Endpoint de transcrição
🔹 Request
POST http://localhost:8080/api/transcriptions  
🔹 Body (form-data)  
Key	Type	Description  
file	File	Arquivo .wav ou .wave  
🔹 Response  
```json
{
  "text": "texto transcrito",
  "processingTimeMs": 856,
  "fileSizeBytes": 461842
}
```

## Teste
na raiz tem uma arquivo de teste: 'audio.wav'  
no postman fazer como na imagem:  
<p align="center">
  <img width="100%" src="./images/postman exemplo.png" alt="Backend Brasil Logo">
</p>

ou via cli:
```bash
export SPEACHES_BASE_URL="http://localhost:8000"
export TRANSCRIPTION_MODEL_ID="Systran/faster-whisper-small"

curl -s "$SPEACHES_BASE_URL/v1/audio/transcriptions" -F "file=@audio.wav" -F "model=$TRANSCRIPTION_MODEL_ID"
```
response:
```json
{"text":"testando o áudio para a gravação e teste da API"}
```

## ⚠️ Troubleshooting  
❌ Erro 500 na API

Verifique:

variável:

WHISPER_BASE_URL=http://speaches:8000
comunicação entre containers (docker network)
❌ Connection refused
speaches não está saudável

verifique:

docker ps
docker logs speaches
❌ Modelo não encontrado

Execute novamente:

uvx speaches-cli model download Systran/faster-whisper-small
❌ localhost não funciona no container

Dentro do Docker:

localhost != outro serviço

Use:
http://speaches:8000
## 📁 Estrutura do projeto
```
.
├── docker-compose.yml
├── docker/
│   └── Dockerfile
├── src/
├── build/
└── README.md
```
## 🧠 Observações técnicas
O serviço speaches expõe API compatível com Whisper  
O backend usa WebClient para integração  
O volume hf-hub-cache evita re-download de modelos  
Healthchecks garantem ordem correta de inicialização  

## 🚀 Melhorias futuras
Suporte a múltiplos idiomas  
Upload assíncrono (fila / mensageria)  
Cache de transcrições  
Observabilidade (Prometheus + Grafana)  