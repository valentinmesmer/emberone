# 2. Randbedingungen

## 2.1 Technische Randbedingungen

| Randbedingung | Ausprägung |
|---------------|------------|
| **Backend-Runtime** | Java 25, Spring Boot 4.x, Gradle |
| **Frontend-Runtime** | Bun, Next.js 16 (App Router), React 19 |
| **Persistenz** | PostgreSQL mit Row Level Security (FORCE RLS) |
| **Cache** | Redis als Spring-Cache (App); Authentik hat eigenes Redis - nicht für App-Messaging |
| **Identity Provider** | Derzeit Authentik (OIDC/JWT); Backend als OAuth2 Resource Server. Provisioning über `IdentityProviderUserProvisioner` abstrahiert; aktuell nur Authentik verdrahtet |
| **Object Storage** | S3-kompatibel (Dev: SeaweedFS) |
| **Mail** | SMTP (konfigurierbar über Env) |
| **Realtime** | STOMP über WebSocket; lokaler In-Memory-Broker je Instanz, Fan-out über Redis Pub/Sub |
| **Schema-Migration** | Flyway |
| **Container** | Docker; Images mit Docker Buildx |
| **CI** | GitHub Actions: Tests auf PR/Push; Image-Build nach GHCR nur, wenn Semantic Release ein Release veröffentlicht |

Weitere festgelegte Stack-Details: siehe [04-solution-strategy](04-solution-strategy.md) und [07-deployment-view](07-deployment-view.md).

## 2.2 Organisatorische Randbedingungen

- Monorepo mit `backend/`, `frontend/`, `support/dev/` (lokale Compose-Umgebung).
- Dokumentation folgt arc42; Domänenwissen liegt unter [`support/docs/concept/`](../concept/00-index.md).
- Commits folgen [Conventional Commits](https://www.conventionalcommits.org/) (englische Subjects); Prüfung per `commit-msg`-Hook und CI (Pull Requests sowie Push auf `main` und `development`, inkl. PR-Titel).
- Formatierung: Biome (Frontend) und google-java-format AOSP (Backend, 4 Spaces); `pre-commit` formatiert gestagte Dateien.
- Code, Identifier und Commit-Messages: Englisch. Dokumentation (arc42, concept): Deutsch.
- Default-Branch: `development` (Prereleases). Stabile Releases nur durch Merge nach `main`.
- Änderungen an Security-/Tenant-Pfaden erfordern besondere Sorgfalt (Tests unter `*SecurityTest`, Konzept-Docs).

## 2.3 Konventionen

| Bereich | Konvention |
|---------|------------|
| **Backend-Packages** | Domäne = Top-Level-Package unter `com.emberone.*`; Querschnitt unter `com.emberone.platform.*` |
| **API-Flächen** | Self- / Organization- / Admin-Controller-Muster (siehe [security-and-permissions](../concept/security-and-permissions.md)) |
| **Permissions** | `PermissionType` mit stabiler numerischer ID; Autorisierung über `@PreAuthorize` + `SecurityService` |
| **Multi-Tenancy** | Effektiver Tenant über `TenantContextHolder` → Postgres-GUCs (`app.tenant_id` / `app.user_id`); nicht nur JWT-Claim filtern |
| **JWT-Claims** | Nur `userId`, `tenantId`, `app_role` für Auth; Org-Permissions nie aus dem Token |
| **Frontend-Routen** | App Router: `/my-area/*` (Self), `/organizations/[id]/*` (Org), `/admin/*` (Plattform/Tenant-Admin) |
| **i18n** | Frontend: `useTranslation` + Locale-Dateien; Backend: Message-Bundles / Templates |
| **Tests** | Backend: JUnit + Spring Security WebMvc-Tests; Frontend: Lint + Build in CI |
| **Formatierung** | Frontend: Biome (`bun format`); Backend: Spotless / google-java-format AOSP (`./gradlew spotlessApply`) |
| **Commits** | Conventional Commits; Git-Hooks unter `.githooks/` (`./support/scripts/install-git-hooks.sh`) |
