#!/usr/bin/env bash
# Pornește întregul stack demonstrativ: PostgreSQL (dacă e oprit), API-ul Spring Boot
# și interfața Vite. Ctrl+C oprește API-ul și frontend-ul; baza de date rămâne pornită.
set -euo pipefail

PROIECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$PROIECT_DIR/backend"
FRONTEND_DIR="$PROIECT_DIR/frontend"
LOG_DIR="${TMPDIR:-/tmp}/pcgarage-logs"

DB_CONTAINER="pcgarage-postgres"
BACKEND_URL="http://localhost:8080"
FRONTEND_PORT="5173"
FRONTEND_URL="http://localhost:$FRONTEND_PORT"

say() { printf '\033[1;36m[pcgarage]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[pcgarage]\033[0m %s\n' "$*" >&2; exit 1; }

mkdir -p "$LOG_DIR"

# 1. Baza de date
if ! podman container exists "$DB_CONTAINER" >/dev/null 2>&1; then
    fail "Containerul '$DB_CONTAINER' nu există. Vezi backend/README.md pentru recreare."
fi
if [[ "$(podman inspect -f '{{.State.Running}}' "$DB_CONTAINER")" != "true" ]]; then
    say "Pornesc PostgreSQL..."
    podman start "$DB_CONTAINER" >/dev/null
fi
for _ in $(seq 1 30); do
    podman exec "$DB_CONTAINER" pg_isready -U pcgarage -d pcgarage >/dev/null 2>&1 && break
    sleep 1
done
podman exec "$DB_CONTAINER" pg_isready -U pcgarage -d pcgarage >/dev/null 2>&1 \
    || fail "PostgreSQL nu răspunde. Vezi backend/README.md."
say "PostgreSQL rulează pe localhost:5432 (baza pcgarage)."

# 2. Verificări pentru porturi ocupate
curl -fsS "$BACKEND_URL/api/products" >/dev/null 2>&1 \
    && fail "Backend-ul pare deja pornit pe $BACKEND_URL. Oprește-l înainte."
curl -fsS "$FRONTEND_URL" >/dev/null 2>&1 \
    && fail "Portul $FRONTEND_PORT este deja ocupat. Oprește procesul și reîncearcă."

# 3. Node (Vite 8 cere cel puțin 20.19)
MIN_NODE="20.19.0"
node_satisfies() {
    local binary="$1" version
    version="$("$binary" -p 'process.versions.node' 2>/dev/null || true)"
    [[ -n "$version" ]] || return 1
    [[ "$(printf '%s\n%s\n' "$MIN_NODE" "$version" | sort -V | head -1)" == "$MIN_NODE" ]]
}

NODE_BIN=""
NODE_FOUND_OLD=""
for candidate in "$(command -v node 2>/dev/null || true)" \
                 "$HOME/.local/bin/node" \
                 /home/linuxbrew/.linuxbrew/bin/node \
                 /usr/local/bin/node; do
    [[ -n "$candidate" && -x "$candidate" ]] || continue
    if node_satisfies "$candidate"; then
        NODE_BIN="$candidate"
        break
    fi
    NODE_FOUND_OLD="$candidate ($("$candidate" -v 2>/dev/null || echo '?'))"
done

if [[ -z "$NODE_BIN" ]]; then
    HINT="Rulează scriptul într-un terminal pe host (nu în distrobox/container), unde ai Node 26,
sau instalează Node 22+ în mediul curent."
    if [[ -n "${container:-}" ]]; then
        HINT="Se pare că rulezi într-un container ($container). Deschide un terminal pe host
și rulează din nou scriptul; acolo există Node 26. Alternativ, instalează Node 22+ în container."
    fi
    fail "Vite 8 cere Node >= $MIN_NODE, dar am găsit: ${NODE_FOUND_OLD:-niciun Node}.
$HINT"
fi

NODE_BIN="$(readlink -f "$NODE_BIN")"
export PATH="$(dirname "$NODE_BIN"):$PATH"
say "Node $("$NODE_BIN" -v) ($NODE_BIN)"

# 4. Java
if [[ -z "${JAVA_HOME:-}" ]]; then
    if command -v java >/dev/null 2>&1; then
        JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
    elif [[ -x "$HOME/.local/share/JetBrains/Toolbox/apps/intellij-idea/jbr/bin/java" ]]; then
        JAVA_HOME="$HOME/.local/share/JetBrains/Toolbox/apps/intellij-idea/jbr"
    else
        fail "Nu găsesc Java 21+. Setează JAVA_HOME înainte de a rula scriptul."
    fi
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

# 5. Backend: compilează doar dacă lipsește jar-ul sau sursele sunt mai noi decât ultima compilare
JAR="$(ls "$BACKEND_DIR"/target/pcgarage-demo-api-*.jar 2>/dev/null | head -1 || true)"
BUILD_STAMP="$BACKEND_DIR/target/.porneste-build"
NEEDS_BUILD=0
if [[ -z "$JAR" || ! -f "$BUILD_STAMP" ]]; then
    NEEDS_BUILD=1
elif [[ -n "$(find "$BACKEND_DIR/src" "$BACKEND_DIR/pom.xml" -newer "$BUILD_STAMP" -print -quit 2>/dev/null)" ]]; then
    NEEDS_BUILD=1
fi

if [[ "$NEEDS_BUILD" == "1" ]]; then
    say "Compilez backend-ul..."
    (cd "$BACKEND_DIR" && ./mvnw -q -DskipTests package) \
        || fail "Compilarea a eșuat."
    touch "$BUILD_STAMP"
    JAR="$(ls "$BACKEND_DIR"/target/pcgarage-demo-api-*.jar | head -1)"
fi

export JWT_SECRET="${JWT_SECRET:-local-demo-secret-change-before-sharing-123456}"
export ADMIN_EMAIL="${ADMIN_EMAIL:-admin@nucleu.local}"
export ADMIN_PASSWORD="${ADMIN_PASSWORD:-AdminDemo12345!}"

say "Pornesc backend-ul..."
java -jar "$JAR" > "$LOG_DIR/backend.log" 2>&1 &
BACKEND_PID=$!

cleanup() {
    trap - EXIT INT TERM
    say "Oprire..."
    [[ -n "${FRONTEND_PID:-}" ]] && kill "$FRONTEND_PID" 2>/dev/null || true
    [[ -n "${BACKEND_PID:-}" ]] && kill "$BACKEND_PID" 2>/dev/null || true
    wait 2>/dev/null || true
    say "Backend și frontend oprite. PostgreSQL rămâne pornit."
}
trap cleanup EXIT INT TERM

for _ in $(seq 1 60); do
    curl -fsS "$BACKEND_URL/api/products" >/dev/null 2>&1 && break
    kill -0 "$BACKEND_PID" 2>/dev/null || { tail -20 "$LOG_DIR/backend.log"; fail "Backend-ul s-a oprit. Vezi $LOG_DIR/backend.log"; }
    sleep 1
done
curl -fsS "$BACKEND_URL/api/products" >/dev/null 2>&1 \
    || { tail -20 "$LOG_DIR/backend.log"; fail "Backend-ul nu răspunde. Vezi $LOG_DIR/backend.log"; }
say "Backend: $BACKEND_URL (Swagger: $BACKEND_URL/swagger-ui/index.html)"

# 6. Frontend
if [[ ! -x "$FRONTEND_DIR/node_modules/.bin/vite" ]]; then
    say "Instalez dependențele frontend-ului..."
    (cd "$FRONTEND_DIR" && npm install) || fail "npm install a eșuat."
fi

say "Pornesc frontend-ul..."
(cd "$FRONTEND_DIR" && exec "$NODE_BIN" ./node_modules/vite/bin/vite.js --port "$FRONTEND_PORT" --strictPort) \
    > "$LOG_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!

for _ in $(seq 1 60); do
    curl -fsS "$FRONTEND_URL" >/dev/null 2>&1 && break
    kill -0 "$FRONTEND_PID" 2>/dev/null || { tail -20 "$LOG_DIR/frontend.log"; fail "Frontend-ul s-a oprit. Vezi $LOG_DIR/frontend.log"; }
    sleep 1
done
curl -fsS "$FRONTEND_URL" >/dev/null 2>&1 \
    || { tail -20 "$LOG_DIR/frontend.log"; fail "Frontend-ul nu răspunde. Vezi $LOG_DIR/frontend.log"; }

say "Frontend: $FRONTEND_URL"
say "Admin: $ADMIN_EMAIL / $ADMIN_PASSWORD"
say "Loguri: $LOG_DIR"
say "Apasă Ctrl+C pentru a opri backend-ul și frontend-ul."

if [[ "${NO_OPEN:-0}" != "1" ]] && command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$FRONTEND_URL" >/dev/null 2>&1 || true
fi

wait
