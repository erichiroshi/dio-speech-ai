#!/usr/bin/env bash
# =============================================================================
# release.sh — Script de release automatizado para dio-speech-ai
#
# Uso:
#   ./release.sh <versão> "<título>" "<descrição>"
#
# Exemplos:
#   ./release.sh 1.0.0 "Fase 1: API de transcrição de áudio" "API base funcional"
#   ./release.sh 2.1.0 "Micrometer + Prometheus" "Observabilidade fase 2 item 1"
#
# O script:
#   1. Valida que não há arquivos não commitados
#   2. Atualiza o version no build.gradle
#   3. Faz commit, push, tag anotada e push da tag
#   4. Cria GitHub Release via gh CLI
# =============================================================================

set -euo pipefail

# ─── Cores para output ────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ─── Funções utilitárias ──────────────────────────────────────────────────────
info()    { echo -e "${BLUE}[INFO]${NC} $*"; }
success() { echo -e "${GREEN}[OK]${NC} $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }
error()   { echo -e "${RED}[ERRO]${NC} $*"; exit 1; }

# ─── Validação de argumentos ──────────────────────────────────────────────────
if [ $# -lt 2 ]; then
  echo "Uso: $0 <versão> \"<título>\" [\"<notas extras>\"]"
  echo ""
  echo "Exemplos:"
  echo "  $0 1.0.0 \"Fase 1: API base\""
  echo "  $0 2.1.0 \"Micrometer + Prometheus\" \"Adiciona /actuator/prometheus\""
  exit 1
fi

VERSION="$1"
TITLE="$2"
EXTRA_NOTES="${3:-}"

TAG="v${VERSION}"

# ─── Validar formato da versão (X.Y.Z) ───────────────────────────────────────
if ! echo "$VERSION" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+$'; then
  error "Versão inválida: '$VERSION'. Use o formato MAJOR.MINOR.PATCH (ex: 2.1.0)"
fi

info "Iniciando release ${TAG} — ${TITLE}"
echo ""

# ─── Verificar se estamos em um repositório git ───────────────────────────────
if ! git rev-parse --git-dir > /dev/null 2>&1; then
  error "Este diretório não é um repositório git."
fi

# ─── Verificar se a tag já existe ────────────────────────────────────────────
if git tag -l | grep -q "^${TAG}$"; then
  error "Tag ${TAG} já existe. Use uma versão diferente."
fi

# ─── Verificar arquivos não commitados ───────────────────────────────────────
if ! git diff --quiet || ! git diff --cached --quiet; then
  warn "Há arquivos modificados ou em stage. Deseja continuar mesmo assim? (s/N)"
  read -r confirm
  if [[ "$confirm" != "s" && "$confirm" != "S" ]]; then
    error "Release cancelada. Faça commit dos arquivos pendentes antes de continuar."
  fi
fi

# ─── Atualizar versão no build.gradle ────────────────────────────────────────
BUILD_FILE="build.gradle"

if [ ! -f "$BUILD_FILE" ]; then
  error "build.gradle não encontrado. Execute o script na raiz do projeto."
fi

CURRENT_VERSION=$(grep "^version = " "$BUILD_FILE" | sed "s/version = '//;s/'//")
info "Versão atual no build.gradle: ${CURRENT_VERSION}"
info "Nova versão: ${VERSION}"

# Substituir version no build.gradle
sed -i "s/^version = '.*'/version = '${VERSION}'/" "$BUILD_FILE"

# Verificar se a substituição funcionou
NEW_VERSION=$(grep "^version = " "$BUILD_FILE" | sed "s/version = '//;s/'//")
if [ "$NEW_VERSION" != "$VERSION" ]; then
  error "Falha ao atualizar versão no build.gradle"
fi

success "build.gradle atualizado: ${CURRENT_VERSION} → ${VERSION}"

# ─── Commit do bump de versão ─────────────────────────────────────────────────
git add build.gradle

# Se houver outros arquivos em stage, incluí-los no mesmo commit
if ! git diff --cached --quiet; then
  git commit -m "chore(release): bump version para ${VERSION} — ${TITLE}"
  success "Commit criado"
fi

# ─── Push da branch ───────────────────────────────────────────────────────────
BRANCH=$(git rev-parse --abbrev-ref HEAD)
info "Fazendo push da branch ${BRANCH}..."
git push origin "$BRANCH"
success "Push da branch concluído"

# ─── Determinar o changelog desde a última tag ───────────────────────────────
LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")

if [ -n "$LAST_TAG" ]; then
  CHANGELOG=$(git log "${LAST_TAG}..HEAD" --oneline --no-decorate 2>/dev/null || echo "Sem commits anteriores")
  info "Commits desde ${LAST_TAG}:"
else
  CHANGELOG=$(git log --oneline --no-decorate 2>/dev/null | head -20)
  info "Commits do projeto (sem tag anterior):"
fi
echo "$CHANGELOG" | sed 's/^/  /'
echo ""

# ─── Construir mensagem da tag ────────────────────────────────────────────────
TAG_MESSAGE="${TAG} — ${TITLE}

Commits inclusos nesta release:
$(echo "$CHANGELOG" | head -20)

${EXTRA_NOTES:+Notas adicionais:
${EXTRA_NOTES}
}Stack: Java 25 · Spring Boot 4.0.5 · Docker Compose
Breaking changes: verificar changelog acima
"

# ─── Criar tag anotada ────────────────────────────────────────────────────────
info "Criando tag anotada ${TAG}..."
git tag -a "$TAG" -m "$TAG_MESSAGE"
success "Tag ${TAG} criada"

# ─── Push da tag ──────────────────────────────────────────────────────────────
info "Fazendo push da tag ${TAG}..."
git push origin "$TAG"
success "Push da tag concluído"

# ─── GitHub Release via gh CLI ───────────────────────────────────────────────
if command -v gh &> /dev/null; then
  info "Criando GitHub Release via gh CLI..."

  RELEASE_NOTES="## ${TITLE}

### Commits desta release

\`\`\`
${CHANGELOG}
\`\`\`

${EXTRA_NOTES:+### Notas adicionais

${EXTRA_NOTES}
}
---
*Release gerada automaticamente por \`release.sh\`*"

  gh release create "$TAG" \
    --title "${TAG} — ${TITLE}" \
    --notes "$RELEASE_NOTES"

  success "GitHub Release criada: https://github.com/$(gh repo view --json nameWithOwner -q .nameWithOwner)/releases/tag/${TAG}"
else
  warn "gh CLI não encontrado. Crie a GitHub Release manualmente:"
  echo ""
  echo "  gh release create ${TAG} \\"
  echo "    --title \"${TAG} — ${TITLE}\" \\"
  echo "    --notes \"${EXTRA_NOTES:-Release ${TAG}}\""
  echo ""
  echo "  Ou acesse: https://github.com/<seu-repo>/releases/new?tag=${TAG}"
fi

# ─── Resumo final ─────────────────────────────────────────────────────────────
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
success "Release ${TAG} concluída com sucesso!"
echo ""
echo "  Tag:     ${TAG}"
echo "  Título:  ${TITLE}"
echo "  Branch:  ${BRANCH}"
echo "  Commits: $(echo "$CHANGELOG" | wc -l | tr -d ' ') commits inclusos"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
info "Próximos passos: iniciar as tarefas da próxima versão no roadmap."
