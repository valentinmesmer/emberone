# 11. Risiken und technische Schulden

| ID | Risiko / technische Schuld | Auswirkung | Gegenmaßnahme |
|----|----------------------------|------------|---------------|
| R-1 | **Zentrale Facades** (`Workflow`, `TokenService`, `Standard`, `Organization`, `SecurityService`, …) mit hoher Kopplung | Änderungen schwer abzuschätzen; Regressionen | Concept-Docs; gezielte Extraktion hinter Facades; Tests an Hotspots - [ADR-005](09-architecture-decisions.md) |
| R-2 | **Security-/Tenant-Fehler** (falscher Holder, fehlende RLS-Policy, JWT vs. DB-Permissions verwechselt) | Cross-Tenant-Leak oder Privilege-Escalation | FORCE RLS; Security-WebMvc-Tests; Docs multi-tenancy / security |
| R-3 | **Frontend↔Backend Permission-Drift** | UI zeigt Aktionen, API liefert 403 - oder umgekehrt Lücken im Gating | `usePermissions`-Vertrag pflegen; gemeinsame Permission-IDs; Stichproben in Reviews |
| R-4 | **Standard-/Config-Komplexität** (Baseline, Tenant-Default, Org-Override, effektive Auflösung) | Falsche Workflow-/Permission-Config in Produktion | [standard.md](../concept/standard.md); Admin-UI-Tests; klare Export/Materialisierungsregeln |
| R-5 | **Cache-Stale nach Membership-Änderungen** | Veraltete Rechte bis TTL | After-Commit-Eviction-Listener; Cache-Inventar in [caching.md](../concept/caching.md) |
| R-6 | **Abhängigkeit Authentik / S3 / SMTP** | Ausfall blockiert Login, Dateien oder Mail | Health/Retries; Betriebshinweise in [Retention & Löschung](../concept/retention-and-erasure.md); Dev-Compose entkoppelt App-Prozesse; IdP-Wechsel ist Aufwand (SPI + OIDC/Claims), kein Drop-in |
| R-7 | **Doku-Drift nach Refactors** (Package-Umzüge, Access-Service-Split) | Concept-/arc42-Texte widersprechen dem Code | Bei API-/AuthZ-/Lifecycle-Änderungen passende Docs mitziehen ([CONTRIBUTING.md](../../../CONTRIBUTING.md)) |
| R-8 | **Instanzgebundener Zustand** (neuer JVM-lokaler Zustand, Job ohne `@SchedulerLock`, Outbox ohne Claim) | Mehrere Instanzen: doppelte Jobs, verpasste Events, inkonsistente Caches | Regeln in [Skalierbarkeit](../concept/scalability.md) |
| R-9 | **Daten- und Listenwachstum** (unbegrenzte Listen, fehlende Indizes, N+1) | Latenz und Speicher unter Last; Retention-/Audit-Kosten | [Skalierbarkeit](../concept/scalability.md); [pagination.md](../concept/pagination.md) bei API-Änderungen |

Priorisiertes Kopplungsrisiko bleibt konzentrierte Facade-Nutzung (R-1 / ADR-005). Kernmodule sind dokumentiert - offenes Restrisiko ist Drift (R-7). Skalierbarkeit: [Skalierbarkeit](../concept/scalability.md).
