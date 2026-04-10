# Política de Versionamento — dio-speech-ai

## Convenção de versão

O projeto segue [Semantic Versioning](https://semver.org/) adaptado ao roadmap de fases:

```
MAJOR.MINOR.PATCH
  │     │     └── correção de bug dentro de um item (2.1.1, 2.1.2 ...)
  │     └──────── número do item dentro da fase   (2.1, 2.2, 2.3 ...)
  └────────────── número da fase                  (1, 2, 3)
```

| Versão | Significado |
|--------|-------------|
| `1.0.0` | Fase 1 completa — API base funcional |
| `2.1.0` | Fase 2, item 1 concluído — Micrometer + Prometheus |
| `2.2.0` | Fase 2, item 2 concluído — Logs estruturados |
| `2.3.0` | Fase 2, item 3 concluído — Redis no Compose |
| `2.4.0` | Fase 2, item 4 concluído — Cache SHA-256 |
| `2.5.0` | Fase 2, item 5 concluído — Métricas de negócio |
| `2.6.0` | Fase 2, item 6 concluído — Grafana dashboards |
| `2.7.0` | Fase 2, item 7 concluído — Tracing Zipkin/OTel |
| `3.1.0` | Fase 3, item 1 concluído — CircuitBreaker |
| `3.2.0` | Fase 3, item 2 concluído — Retry + backoff |
| `3.3.0` | Fase 3, item 3 concluído — Métricas de resiliência |
| `3.4.0` | Fase 3, item 4 concluído — Spring Security + JWT |
| `3.5.0` | Fase 3, item 5 concluído — Testes de integração |
| `X.Y.Z` onde Z>0 | Hotfix ou ajuste dentro do mesmo item |

---

## Regras obrigatórias por release

Antes de criar qualquer tag de release, os seguintes passos são obrigatórios:

1. **Atualizar `version` no `build.gradle`** — o valor deve espelhar a tag da release.
2. **Fazer commit de tudo** — nenhum arquivo modificado pode estar fora do stage.
3. **Criar tag anotada** (`-a`) — sempre com mensagem descritiva, nunca tag leve.
4. **Push da branch e da tag separadamente** — `git push` não envia tags por padrão.
5. **Criar GitHub Release** via `gh` CLI ou interface web, colando o changelog.

---

## Formato da mensagem de commit (Conventional Commits)

```
<tipo>(<escopo>): <descrição curta em português>

[corpo opcional — o que foi feito e por quê]

[rodapé opcional — breaking changes, issues fechadas]
```

Tipos aceitos:

| Tipo | Quando usar |
|------|-------------|
| `feat` | nova funcionalidade |
| `fix` | correção de bug |
| `chore` | configuração, dependências, infra |
| `docs` | README, comentários, CHANGELOG |
| `refactor` | refatoração sem mudança de comportamento |
| `test` | adição ou correção de testes |
| `perf` | melhoria de performance |

Exemplos:

```
feat(metrics): adicionar Micrometer + endpoint /actuator/prometheus
chore(docker): adicionar serviço Redis no compose.yaml
feat(cache): implementar cache de transcrição por hash SHA-256
feat(resilience): configurar CircuitBreaker Resilience4j para chamadas ao Whisper
```

---

## Formato da mensagem de tag anotada

```
git tag -a vX.Y.Z -m "vX.Y.Z — <título curto>

O que foi adicionado:
- item 1
- item 2

Arquivos principais alterados:
- caminho/Arquivo.java
- docker-compose.yml

Breaking changes: nenhum | <descrição se houver>
"
```

---

## Workflow completo de release (passo a passo)

### Pré-requisitos

```bash
# Verificar se gh CLI está instalado (para criação de GitHub Release)
gh --version

# Autenticar se necessário
gh auth login
```

### Fluxo padrão (após concluir um item do roadmap)

```bash
# 1. Verificar status — não deve haver nada pendente fora do commit
git status

# 2. Atualizar versão no build.gradle (editar manualmente)
#    version = '2.1.0'

# 3. Adicionar todos os arquivos modificados
git add .

# 4. Commit seguindo Conventional Commits
git commit -m "feat(metrics): adicionar Micrometer + Prometheus (v2.1.0)"

# 5. Push da branch
git push origin main

# 6. Criar tag anotada
git tag -a v2.1.0 -m "v2.1.0 — Micrometer + Prometheus

O que foi adicionado:
- Dependência micrometer-registry-prometheus
- Endpoint /actuator/prometheus exposto
- Métricas HTTP automáticas (latência, throughput, erros)
- Container Prometheus no docker-compose.yml

Arquivos principais alterados:
- build.gradle
- src/main/resources/application.yml
- docker-compose.yml

Breaking changes: nenhum
"

# 7. Push da tag (obrigatório — git push não envia tags)
git push origin v2.1.0

# 8. Criar GitHub Release com changelog
gh release create v2.1.0 \
  --title "v2.1.0 — Micrometer + Prometheus" \
  --notes "## O que foi adicionado

- Dependência \`micrometer-registry-prometheus\`
- Endpoint \`/actuator/prometheus\` exposto
- Métricas HTTP automáticas (latência, throughput, erros)
- Container Prometheus no \`docker-compose.yml\`

## Como testar

\`\`\`bash
docker compose up -d
curl http://localhost:8080/actuator/prometheus | grep http_server
\`\`\`"
```

---

## Comandos de consulta úteis

```bash
# Listar todas as tags em ordem
git tag -l --sort=-version:refname

# Ver detalhes de uma tag anotada
git show v2.1.0

# Ver log desde a última tag
git log $(git describe --tags --abbrev=0)..HEAD --oneline

# Ver diff entre duas releases
git diff v1.0.0 v2.1.0 --stat

# Listar releases no GitHub
gh release list
```

---

## Hotfix dentro de um item

Se um bug for encontrado após a release `2.1.0`:

```bash
git checkout -b hotfix/2.1.1-fix-prometheus-endpoint

# ... correção ...

git add .
git commit -m "fix(metrics): corrigir path do endpoint prometheus no application.yml"
git push origin hotfix/2.1.1-fix-prometheus-endpoint

# Merge para main
git checkout main
git merge --no-ff hotfix/2.1.1-fix-prometheus-endpoint
git push origin main

# Atualizar versão para 2.1.1 no build.gradle, depois:
git tag -a v2.1.1 -m "v2.1.1 — hotfix endpoint prometheus"
git push origin v2.1.1
gh release create v2.1.1 --title "v2.1.1 — hotfix endpoint prometheus" --notes "Correção: ..."

# Deletar branch de hotfix
git branch -d hotfix/2.1.1-fix-prometheus-endpoint
git push origin --delete hotfix/2.1.1-fix-prometheus-endpoint
```

---

## Release v1.0.0 — Fase 1 (API Base)

Comandos para criar a release inicial do estado atual do projeto:

```bash
# Dentro do diretório do projeto
cd dio-speech-ai

# 1. Atualizar versão no build.gradle
#    Editar: version = '0.0.1-SNAPSHOT'  →  version = '1.0.0'

# 2. Commit de bump de versão
git add build.gradle
git commit -m "chore(release): bump version para 1.0.0 — Fase 1 completa"

# 3. Push
git push origin main

# 4. Tag anotada
git tag -a v1.0.0 -m "v1.0.0 — Fase 1: API de transcrição de áudio (base)

O que está incluso:
- API REST POST /api/transcriptions (multipart/form-data)
- Integração com Whisper via Speaches (WebClient)
- Validação de tipo de arquivo (wav, mpeg) e tamanho (5 MB)
- Tratamento de erros com ProblemDetail (RFC 9457)
- GlobalExceptionHandler centralizado
- Docker Compose com speaches + API, healthcheck e dependência ordenada
- Spring Actuator com /actuator/health

Stack: Java 25 · Spring Boot 4.0.5 · WebFlux WebClient · Docker

Breaking changes: nenhum (versão inicial)
"

# 5. Push da tag
git push origin v1.0.0

# 6. GitHub Release
gh release create v1.0.0 \
  --title "v1.0.0 — Fase 1: API de transcrição de áudio" \
  --notes "## Fase 1 — Base concluída

Primeira release estável do projeto **dio-speech-ai**, desenvolvido para o desafio DIO × Globant.

## Funcionalidades

- \`POST /api/transcriptions\` — recebe arquivo de áudio e retorna transcrição
- Integração com **Whisper** via [Speaches](https://github.com/speaches-ai/speaches)
- Validação de Content-Type (\`audio/wav\`, \`audio/mpeg\`, \`audio/wave\`)
- Limite de 5 MB por arquivo
- Erros estruturados com **ProblemDetail (RFC 9457)**
- Infraestrutura Docker Compose com healthcheck e dependência ordenada

## Stack

| Tecnologia | Versão |
|---|---|
| Java | 25 |
| Spring Boot | 4.0.5 |
| Speaches (Whisper) | latest-cuda |
| Docker Compose | v2+ |

## Como rodar

\`\`\`bash
docker compose up -d
uvx speaches-cli model download Systran/faster-whisper-small
curl -X POST http://localhost:8080/api/transcriptions \
  -F 'file=@audio.wav;type=audio/wav' | jq .
\`\`\`

## Próxima release

\`v2.1.0\` — Observabilidade com Micrometer + Prometheus"
```
