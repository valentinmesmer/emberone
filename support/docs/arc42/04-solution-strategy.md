# 4. Lösungsstrategie

EmberOne setzt auf einen **modularen Monolithen** (Spring Boot) plus eine **Next.js-App** (App Router, clientseitige Auth-/Permission-Provider), verbunden über JWT und optionale Realtime-Kanäle. Domänengrenzen sind Package-Grenzen, keine Microservice-Netzwerkgrenzen.

## Technologieentscheidungen

| Entscheidung | Wahl | Warum |
|--------------|------|-------|
| Backend | Spring Boot Modular Monolith | Eine Deploy-Einheit, starke Konsistenz (Workflows + Notifications + RLS in einer Transaktion), Package-Modularität |
| Frontend | Next.js App Router + React | Routengruppen Self/Org/Admin, PWA (Serwist), React Query |
| AuthN | Authentik + OIDC/JWT (aktuell) | Externer IdP; Backend validiert JWT (`userId`, `tenantId`, `app_role`). Provisioning einseitig App→IdP; SPI vorhanden, derzeit nur Authentik |
| AuthZ | DB-Permissions + `SecurityService` | Org-Permissions dynamisch aus der DB; nicht im JWT (keine Gruppen-/Permission-Claims für AuthZ) |
| Isolation | Postgres FORCE RLS + Tenant-GUCs | Defense-in-depth gegen Cross-Tenant-Leaks |
| Cache | Redis + After-Commit-Eviction | Performance für Permission-/Org-Lookups ohne stale Writes |
| Realtime | STOMP/WebSocket | Inbox, Notifications, Org-Änderungen live |
| Storage | S3-API | Austauschbarer Object Storage |
| Migration | Flyway | Versionierte Schema-/RLS-Änderungen |

## Top-Level-Zerlegung

```
┌─────────────────────────────────────────────────────────┐
│ Frontend (Next.js)                                      │
│  /auth  /my-area  /organizations/[id]  /admin           │
└───────────────────────────┬─────────────────────────────┘
                            │ REST + WS
┌───────────────────────────▼─────────────────────────────┐
│ Backend (Spring Boot)                                   │
│  Domänen-Packages │ platform.* (security inkl. Tenant-Kontext, …) │
└───────┬───────────┬──────────┬──────────┬───────────────┘
        ▼           ▼          ▼          ▼
   PostgreSQL     Redis      S3/SMTP   Authentik
```

## Qualitätsziele → Strategie

| Qualitätsziel ([Kap. 1.2](01-introduction-and-goals.md)) | Strategie |
|----------------------------------------------------------|-----------|
| Isolation & Security | RLS + Holder/GUCs; `@PreAuthorize`; Self/Org/Admin-Controller-Trennung |
| Domänenkorrektheit | Generische Workflow-Engine + typisierte Handler; dokumentierte Zustandsautomaten |
| Wartbarkeit | Concept-Docs für zentrale Facades; Package-pro-Domäne; CI-Tests inkl. Security-WebMvc |
| UX / Realtime | Domain-Notifier + Topic-Auth; Frontend Permission-/Auth-Hooks |
| Betreibbarkeit | `support/dev/compose`, Dockerfiles, GitHub Actions (Tests; Image-Build nach GHCR nur nach Semantic-Release-Publish) |

## Bewusste Nicht-Ziele (aktuell)

- Keine Microservice-Zerlegung pro Domäne (Kosten/Konsistenz).
- Keine Gruppen-/Permission-Liste als autoritative AuthZ im JWT (nur `userId`, `tenantId`, `app_role`).
- Kein eigener IdP - Authentik ist Source of Truth für Login/JWT/Passwörter; EmberOne ist Master für Profil/App-Rolle und provisioniert einseitig zum IdP. Ein weiterer IdP braucht neue Provisioner-Implementierung plus OIDC-/Claim-Konfiguration.
