# Lokale Entwicklung

Alle Befehle vom **Repository-Root** aus ausführen.

## Voraussetzungen

| Werkzeug | Version | Hinweis |
|----------|---------|---------|
| Git, Bash | aktuell | macOS-Standard-Bash 3.2 reicht |
| Docker mit Compose v2 | aktuell | ca. 6 GB RAM für die Container |
| Java (JDK) | 25 | `JAVA_HOME` gesetzt; Gradle kommt per Wrapper |
| Bun | siehe `packageManager` in `frontend/package.json` | |
| Python | 3.x | Git-Hooks und Repo-Checks |

Freie Ports: `3000` (Frontend), `8080` (Backend), `5432` (Postgres), `6379` (Redis), `8333`/`9333` (S3), `9000`/`9443` (Authentik), `1025`/`8025` (Mailpit), `8081` (Redis Insight). Abweichende Ports in `support/dev/config/.env` setzen.

## Schnellstart

```bash
./support/dev/setup.sh                       # Env-Dateien + Git-Hooks, keine Eingaben nötig
./support/dev/scripts/start-docker.sh        # Postgres, Redis, S3, Mailpit, Authentik
./support/dev/scripts/run-backend.sh         # http://localhost:8080
cd frontend && bun install && bun dev        # http://localhost:3000
```

Beim ersten Start braucht Authentik 1-2 Minuten, bis die Blueprints angewendet sind. Danach unter <http://localhost:3000> anmelden (siehe [Anmelden](#anmelden)).

## Setup

`./support/dev/setup.sh` erzeugt `support/dev/config/.env` (Docker), `backend/.env` und `frontend/.env` aus den `*.example`-Dateien und aktiviert die Git-Hooks.

- Leere Werte in den Beispielen sind Secrets. Das Setup **generiert** sie (Hex, damit `source .env` und Compose sie ohne Quoting lesen). Gemeinsame Secrets (Postgres, Redis, S3, Authentik-API-Token) bekommen in Docker- und Backend-Env denselben Wert.
- Erneutes Ausführen behält vorhandene Werte und eigene Zusatz-Keys und ergänzt nur neue Keys aus den Beispielen. Für frische Werte die Datei löschen und neu erzeugen; bei geänderten DB-Passwörtern danach `reset-docker.sh`.
- `--prompt`: Secrets selbst eingeben statt generieren.
- `--intellij`: gibt `backend/.env` als eine Zeile für *Run Configuration → Environment variables* aus.

Git-Hooks ohne Setup: `./support/scripts/install-git-hooks.sh`.

## Docker-Umgebung

```bash
./support/dev/scripts/start-docker.sh   # starten (legt DB-Rollen an)
./support/dev/scripts/stop-docker.sh    # stoppen, Volumes bleiben
./support/dev/scripts/reset-docker.sh   # Volumes löschen und neu starten
```

| Dienst | URL | Zweck |
|--------|-----|-------|
| Authentik | <http://localhost:9000> | OIDC-IdP, Admin-Oberfläche |
| Mailpit | <http://localhost:8025> | fängt alle Mails des Backends ab (SMTP `1025`, ohne Auth/TLS) |
| Redis Insight | <http://localhost:8081> | Redis-UI, kein Login |
| SeaweedFS S3 | `http://localhost:8333` | Object Storage |

Redis Insight: Verbindung einmalig anlegen, mit Hostnamen aus dem Docker-Netzwerk (nicht `localhost`):

| Label | Host | Port | Passwort |
|-------|------|------|----------|
| EmberOne cache | `redis` | 6379 | `EMBERONE_REDIS_PASSWORD` aus `support/dev/config/.env` |
| Authentik | `authentik-redis` | 6379 | *(leer)* |

### Datenbank-Konten

- `emberone_migrator` besitzt Datenbank und Schema und führt Flyway aus (`FLYWAY_USERNAME`/`FLYWAY_PASSWORD`). Kein Superuser, kein `BYPASSRLS`.
- `emberone_app` ist das Laufzeitkonto (kein Superuser, RLS wirkt). Seine Rechte setzt der Flyway-Callback `afterMigrate.sql` bei jedem Backend-Start.

`start-docker.sh` und `reset-docker.sh` legen beide Rollen idempotent an (`bootstrap-db-roles.sh`). Von Hand ist nichts nötig. Hintergrund: [Multi-Tenancy](../docs/concept/multi-tenancy.md).

## Backend

```bash
./support/dev/scripts/run-backend.sh
```

Lädt `backend/.env` und startet `./gradlew bootRun`. API: <http://localhost:8080>, OpenAPI unter `/swagger-ui.html` (`SPRINGDOC_ENABLED=true`). In der IDE: `backend/.env` als Env-Datei der Run-Konfiguration nutzen, oder `./support/dev/setup.sh --intellij`.

## Frontend

```bash
cd frontend
bun install
bun dev
```

App: <http://localhost:3000>.

## Anmelden

Die Authentik-Blueprints (`support/dev/authentik-blueprints/`) legen beim Start Gruppen, Claims und den OIDC-Client `emberone` an (öffentlicher Client, Redirects auf `localhost:3000`).

- Benutzer: `akadmin`
- Passwort: `AUTHENTIK_BOOTSTRAP_PASSWORD` aus `support/dev/config/.env`
- Rolle: Global-Admin in EmberOne und Superuser in Authentik

Auf einer leeren Datenbank legt der Global-Admin unter **Admin** den ersten Mandanten, Organisationen und Personen an. Mails (Einladungen, Bestätigungen) landen in Mailpit.

## Tests und Checks

Die Tests brauchen keine laufenden Container.

```bash
cd backend && ./gradlew test --tests '*FooServiceTest'   # gezielt
cd backend && ./gradlew spotlessApply                    # formatieren
cd backend && ./gradlew build                            # wie CI: Format-Check + alle Tests
cd frontend && bun run lint                              # Biome
cd frontend && bun run format                            # formatieren
cd frontend && bun run build                             # wie CI
python3 support/scripts/add-copyright-headers.py --check # AGPL-Header neuer Dateien
```

Weitere Repo-Checks unter `support/scripts/`: `check-unused-i18n-keys.py` (ungenutzte Übersetzungen), `check-list-legacy-params.sh` (alte Listen-Parameter an API-Oberflächen).

## Layout

```
support/dev/
  DEV.md                   # diese Datei
  setup.sh                 # Einstieg
  compose/                 # docker-compose.dev.yml
  config/                  # .env.dev.example, erzeugte .env, SeaweedFS-Template (erzeugtes JSON gitignored)
  authentik-blueprints/    # Gruppen, Claims, OIDC-Client
  scripts/                 # start/stop/reset-docker, run-backend, setup-*, bootstrap-db-roles
    lib/                   # env-from-example, dev-paths
```
