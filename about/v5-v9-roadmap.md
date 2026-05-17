# Fases 5 a 9 — Roadmap técnico

Documento de referência sobre as decisões de arquitetura, tecnologias e
trade-offs das próximas fases do projeto dio-speech-ai.

---

## Fase 5 — CI/CD (v5.1.0 a v5.3.0)

### Por que CI/CD antes da refatoração?

A Fase 7 vai mover arquivos, renomear pacotes e remover classes.
Sem um pipeline de testes automatizado, não há como saber se a refatoração
quebrou algo sem rodar os testes manualmente antes de cada commit.

O CI/CD é a rede de segurança que torna a refatoração hexagonal segura
para executar em incrementos pequenos.

### Por que remover JWT nesta fase?

A implementação atual de JWT não tem autenticação real — aceita qualquer
username sem validar credenciais. Manter uma falsa sensação de segurança
é pior do que não ter segurança declarada.

A remoção nesta fase simplifica o código, os testes e o CI antes da
refatoração hexagonal. Segurança de verdade volta em fase futura com
RBAC, refresh token e integração com IdP.

### Jacoco — por que 70%?

O projeto tem Testcontainers que cobrem o fluxo completo, mas classes
como `GlobalExceptionHandler` e `TranscriptionMetrics` exigem testes
específicos ainda não escritos. Começar com 80% travaria o CI antes
mesmo de entregar valor.

A estratégia é: 70% agora → subir gradualmente conforme os testes
unitários das fases seguintes aumentam a cobertura naturalmente.

**Exclusões do threshold:**
- `**/config/**` — beans de configuração Spring
- `**/dto/**` — records sem lógica
- `**/exception/**` — classes de exceção simples
- `**/*Application*` — entry point
- `**/properties/**` — @ConfigurationProperties

### SonarCloud vs SonarQube local

SonarQube local exige servidor rodando. SonarCloud é gratuito para
projetos públicos, integra diretamente com GitHub Actions e adiciona
comentários automáticos nos PRs. Para um portfólio, o badge verde
do SonarCloud no README é um sinal visível de qualidade.

### Dois CIs separados — por quê?

`ci.yml` roda em todo push de qualquer branch — rápido (só JDK + Gradle,
~3 min). `docker.yml` faz build de imagem — mais lento (~8 min).
Separar garante que feedback de testes chega rápido sem esperar o
Docker build.

### Trivy com exit-code 0

Vulnerabilidades CRITICAL e HIGH são reportadas mas não bloqueiam o CI.
Em produção real seria `exit-code: 1`. Para um projeto de portfólio,
bloquear o CI por vulnerabilidade numa dependência transitiva do
Spring Boot seria ruído sem valor — o foco é visibilidade.

### docs.yml — por que readme_pdf.md separado?

O `README.md` usa emojis, badges Shields.io e HTML inline. Pandoc +
xelatex não renderiza isso adequadamente — gera caracteres inválidos,
badges quebrados e layout desestruturado.

O `readme_pdf.md` é um documento técnico limpo: sem emojis, sem badges,
sem HTML. Markdown puro → PDF profissional. Conteúdo: o que é o
projeto, como rodar, endpoints, variáveis de ambiente, configuração.

---

## Fase 6 — Bean Validation (v6.1.0 a v6.2.0)

⚠️ **NÃO IMPLEMENTADA**

### O que seria validado

Os DTOs de entrada do projeto não tinham validação declarativa além
do `@NotBlank` no `username` do `TokenRequest`. A Fase 6 estenderia
validação para todos os pontos de entrada.

**`TranscribeCommand`** (Fase 7 em diante): `@NotNull` nos bytes,
`@NotBlank` no filename, `@Positive` no tamanho.

**Endpoint HTTP atual**: o `MultipartFile` já tem validação no
`TranscriptionService.validate()` via lógica imperativa. A Fase 6
moveria parte dessa validação para constraints declarativas onde faz sentido,
mantendo validação de Content-Type no service (lógica de negócio).

### Padronização do GlobalExceptionHandler

O handler tinha mensagens em dois idiomas — `"Validation Error"` e
`"You do not have permission"` em inglês, todo o resto em português.
Esta fase padronizaria para português.

**Trade-off:** padronizar em inglês seria mais "enterprise" e
internacionalizável. Mas o projeto é ptBR, o time é ptBR, e
consistência interna vale mais do que preparação prematura para i18n.

### Motivo da não implementação

A Fase 6 não foi implementada porque, até a versão atual (v10.2.0), não há
DTOs com múltiplos campos que se beneficiariam da validação bean validation.
Os objetos de entrada são simples (apenas um arquivo de upload) e a validação
de conteúdo é feita por lógica de negócio no service.

### Condição para implementação futura

A Fase 6 será implementada quando o sistema introduzir DTOs complexos com
múltiplos campos que requerem validação declarativa (como endpoints com
corpo JSON contendo vários campos que precisam de validação como tamanho,
formato, padrão, etc.).

---

## Fase 7 — Arquitetura Hexagonal (v7.1.0 a v7.6.0)

### O princípio central

O domínio não conhece nenhuma tecnologia. O domínio define interfaces
(ports). Os adapters implementam essas interfaces.

Fluxo de dependência: **adapters → domínio** (nunca o contrário).

### Por que hexagonal para este projeto?

O problema concreto: `TranscriptionService` conhece `SpeechToTextClient`
(Whisper), `CacheService` (Redis) e `TranscriptionMetrics` ao mesmo tempo.
Trocar o Whisper por OpenAI exige mexer no service de negócio.

Com ports & adapters: `WhisperAdapter implements SpeechToTextPort`.
Para OpenAI: `OpenAiAdapter implements SpeechToTextPort`. O domínio
não muda.

### Refatoração incremental em 6 versões

Cada versão é compilável e os testes passam. Nenhuma é uma big-bang.

| Versão | O que move |
|---|---|
| 7.1 | Entidade Transcription + ports de saída |
| 7.2 | Port de entrada + command/result |
| 7.3 | TranscribeAudioUseCase + teste unitário puro |
| 7.4 | WhisperAdapter + RedisAdapter |
| 7.5 | Controller refatorado |
| 7.6 | Auth hexagonal + JwtProperties no lugar certo |

### Teste unitário puro — o maior ganho

`TranscribeAudioUseCaseTest` com Mockito:
- Zero Spring context
- Zero Docker / Testcontainers
- Executa em ~20ms
- Testa a lógica de negócio isolada

Antes, para testar o fluxo de cache era necessário o
`TranscriptionIntegrationTest` com Redis real — ~10s de startup.

---

## Fase 8 — RabbitMQ (v8.1.0 a v8.3.0)

### O modelo de eventos

Após cada transcrição bem-sucedida, o sistema publica:

```
Exchange: transcription.events (topic)
Routing key: transcription.completed
Payload: TranscriptionCompletedEvent
  { transcriptionId, audioHash, text, fileSizeBytes, completedAt }
```

Qualquer serviço assina essa exchange. O serviço de transcrição
não sabe — nem quer saber — quem vai consumir.

### Consumer de pedidos (adapter de entrada)

`TranscriptionRequestConsumer` recebe pedidos via fila e usa o mesmo
`TranscribeAudioPort` que o controller HTTP usa. Mesma lógica de cache,
mesma resiliência, mesmo evento publicado. O caso de uso não sabe
de onde veio o pedido.

### DLQ — dead-letter queue

Após 3 tentativas com falha, a mensagem vai para
`transcription.requests.dlq` sem ser perdida. Em produção, uma
equipe de suporte pode inspecionar e re-processar mensagens da DLQ.

### Trade-off: RabbitMQ vs Kafka

RabbitMQ: orientado a mensagens, confirmação por mensagem (ACK),
DLQ nativa, configuração simples. Ideal para roteamento por tipo
de evento e para um número moderado de consumidores.

Kafka: orientado a log, retenção configurável, replay de eventos,
escala horizontal melhor para volumes altos. Mais complexo de operar.

Para notificações (SMS, e-mail, WhatsApp) com volume de transcrições
moderado, RabbitMQ é a escolha adequada.

---

## Fase 9 — Notificações (v9.1.0 a v9.3.0)

### O padrão

`NotificationPort` define o contrato. Cada canal é um adapter.
O consumer RabbitMQ chama `notificationPort.send()` — não sabe
se vai pelo Twilio, Spring Mail ou Meta API.

### E-mail — Spring Mail

`spring-boot-starter-mail`. Template HTML da transcrição. SMTP
configurável via variáveis de ambiente. Testes com GreenMail
(servidor SMTP embarcado).

**Trade-off:** usar SendGrid ou Mailgun seria mais robusto para
produção (gerenciamento de bounces, templates avançados). Spring Mail
via SMTP é adequado para demonstração e desenvolvimento.

### SMS e WhatsApp — via WebClient

Adapters genéricos via WebClient REST. A interface é a mesma.
Trocar Twilio por Vonage = novo arquivo, zero impacto no consumer.

Testes com MockWebServer (já usado nos testes do Whisper).

### Por que separar em 3 versões?

9.1 cria o `NotificationPort` e a estrutura base.
9.2 implementa e-mail (mais simples, sem conta externa obrigatória para testes).
9.3 implementa SMS/WhatsApp (exige conta em provedor externo).

Permite entregar valor incremental e demonstrar cada canal separadamente.
