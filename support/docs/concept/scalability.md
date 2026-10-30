# Skalierbarkeit

Design-Notizen dazu, wie das Backend für mehrere Instanzen, Last und Datenwachstum ausgelegt ist. Verwandt: [Realtime / WebSocket](realtime.md), [Geplante Jobs](scheduled-jobs.md), [Caching](caching.md), [Benachrichtigungssystem](notification-system.md), [Paginierung](pagination.md), [Aufbewahrung und Löschung](retention-and-erasure.md), [Multi-Tenancy](multi-tenancy.md).

## Mental Model

Jede Backend-Instanz ist austauschbar und hält keinen fachlichen Zustand. Geteilter Zustand liegt in Postgres (Daten, Locks, Outboxen), Redis (Cache, Realtime-Fan-out) und S3 (Dateien).

```
Client ──▶ Proxy ──▶ Backend-Instanz 1..N ──┬─▶ Postgres (RLS, Outbox-Tabellen, ShedLock)
                                             ├─▶ Redis    (Cache, Pub/Sub für Realtime)
                                             └─▶ S3       (Dateien)
```

Daraus folgen Regeln für neuen Code:

- Kein JVM-lokaler Zustand, der Korrektheit beeinflusst (keine lokalen Caches, `synchronized`-Sperren oder Maps mit fachlichem Inhalt). Ausnahme ist verbindungsgebundener Zustand wie die WebSocket-Sessions (`RealtimeSessionRegistry`).
- Jeder `@Scheduled`-Job trägt `@SchedulerLock`.
- Kein Daten-Seed oder Backfill beim Start und keiner im Request-Pfad. Die Mandanten-Baseline wird beim Anlegen des Mandanten geseedet; Buckets legt der Start einmalig an.
- Zustellung (Mail, S3-Löschung, IdP-Deprovisionierung) läuft über Outbox-Tabellen.
- Schemaänderungen (Indizes, Partitionierung) stehen direkt in `V1__Initial_setup.sql`.

## Instanzübergreifende Bausteine

| Thema | Umsetzung | Doku |
|-------|-----------|------|
| Realtime | `RealtimePublisher` schreibt in den Redis-Channel `emberone:realtime`; jede Instanz liefert über ihren lokalen STOMP-Broker an ihre Clients aus | [realtime.md](realtime.md) |
| Geplante Jobs | ShedLock (JDBC-Tabelle `shedlock`), `@SchedulerLock` pro Job, `defaultLockAtMostFor = PT30M` | [scheduled-jobs.md](scheduled-jobs.md) |
| Outboxen | Mail (`ntf_deliveries`), S3-Löschung (`obj_pending_deletions`), IdP-Deprovisionierung (`idm_users`): Zeilen werden mit `FOR UPDATE SKIP LOCKED` gesichert abgeholt, I/O läuft außerhalb der Transaktion | [notification-system.md](notification-system.md), [identity-provider-authentik.md](identity-provider-authentik.md) |
| Cache | Redis ist der einzige Cache (`CacheConfig`, TTL je Cache). Evictions laufen nach Commit; ein Redis-Fehler wird als Cache-Miss behandelt (`ResilientCacheErrorHandler`) | [caching.md](caching.md) |
| Mandanten-Baseline | `TenantBaselineSyncService` läuft beim Anlegen des Mandanten, serialisiert über einen Postgres-Advisory-Lock pro Mandant | [standard.md](standard.md) |
| Deaktivierte User | Prüfung pro Request über den Redis-Cache `userActive`, Eviction bei `UserDeactivatedEvent` | [caching.md](caching.md) |
| Dateien | Downloads streamen aus S3 (`ResponseInputStream`) und werden nach Ende der Transaktion ausgeliefert; Buckets werden einmal beim Start angelegt (Fehler werden geloggt, der Start läuft weiter) | [attachment-and-document-promotion.md](attachment-and-document-promotion.md) |

## Request-Pfad und Connections

- Virtual Threads sind aktiv (`spring.threads.virtual.enabled`). Der Engpass ist daher der Hikari-Pool: `maximum-pool-size = minimum-idle = 25` pro Instanz. Die Summe über alle Instanzen muss unter Postgres `max_connections` bleiben.
- Pro Transaktion setzt `PostgresSessionContext` die GUCs `app.tenant_id` und `app.user_id` (transaktionslokal, damit PgBouncer im Transaction-Mode funktioniert). Hinter PgBouncer < 1.21 braucht die JDBC-URL `prepareThreshold=0`.
- Jeder Repository-Aufruf außerhalb einer Transaktion kostet eine eigene Transaktion: Connection-Checkout und `set_config` für die GUCs. Die Repository-Interfaces sind dafür auf Typebene `readOnly`, schreibende Methoden deklarieren ihre Transaktion selbst (siehe [Multi-Tenancy](multi-tenancy.md#jpa--transaktionsscoping)).
- `@AfterCommitEventListener` öffnet eine zweite Transaktion (`REQUIRES_NEW`) und braucht damit eine zweite Connection. Listener ohne DB-Zugriff (Cache-Eviction, Realtime-Push) nutzen `@AfterCommitCallback`.
- Hibernate batcht: `default_batch_fetch_size = 64`, `jdbc.batch_size = 50`, `order_inserts` und `order_updates`. Entities nutzen `IDENTITY`; Insert-Batching greift damit nicht.
- Externe Aufrufe haben Timeouts: Authentik/Feign 2 s Connect und 5 s Read, S3 `apiCallTimeout` 60 s, Redis 1 s, SMTP 10 s. Die Deprovisionierung läuft über die Outbox (`deprovision_pending_at`); das Anlegen und Aktualisieren von IdP-Konten ruft Authentik synchron auf.
- Scheduler: Pool mit 8 Threads, Mandantenschleife über `TenantBatchRunner`.
- Betrieb: Graceful Shutdown (`server.shutdown: graceful`, 30 s), Health-Probes (`db`, `redis` in der Readiness-Gruppe) und Prometheus-Endpunkt auf dem Management-Port 8081, ECS-JSON-Logs.
- JVM-Flags im Backend-Image: `-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError -XX:+UseG1GC -XX:+UseCompactObjectHeaders`. Das Jar liegt als Layered Jar im Image (`dependencies` bleibt zwischen Releases byte-identisch, nur `application` ändert sich).

## Datenwachstum

- Wachsende Listen sind paginiert (`PageDto`, Default 50, Maximum 200), siehe [pagination.md](pagination.md). Listen mit unbegrenztem Wachstum haben keine unbegrenzte Antwort.
- Indizes für Hot Paths (Mitgliedschaften, Termine, Zeiterfassung, Event-Anwesenheit, FK-Spalten) stehen in `V1__Initial_setup.sql`. Jede neue FK-Spalte mit Cascade oder Löschpfad bekommt einen Index mit führendem `tenant_id`.
- `action_log` ist nach `changed_at` partitioniert (monatlich, Partitionen sind bis 2032 vorangelegt, danach greift die Default-Partition). Löschläufe der Retention und der Tenant-Purge setzen `app.retention` bzw. `app.purging`, damit der Audit-Trigger sie nicht protokolliert. Der Tenant-Purge löscht in mehreren Batch-Transaktionen.
- Listen mit Zusatzdaten laden zuerst die ID-Seite und danach die Collections für diese Seite, damit Paginierung nicht im Speicher passiert.

## Frontend

Das Frontend ist eine zustandslose Client-SPA (`output: 'standalone'`, Runtime-Config als `runtime-config.json`, Health unter `/healthz`). Last auf das Backend wird begrenzt durch:

- Debounce (1 s) und Jitter bei Realtime-Invalidierungen (`lib/store/cache-invalidation.ts`) und beim WebSocket-Reconnect (`lib/realtime/jitter.ts`).
- Paginierte Listen über `usePagedList` und `lib/api/paged.ts`.

## Erweiterungen

- Read-Replica: `readOnly = true` ist durchgängig gesetzt; Routing nach `isCurrentTransactionReadOnly()` wäre ein Einhängepunkt. Eigenständige Repository-Aufrufe sind dadurch bereits read-only und könnten ohne Änderung an den Aufrufern auf eine Replica geroutet werden. GUCs sind auf Hot Standby erlaubt. Replikationslag ist bei Reads nach Writes zu beachten.
- Optimistic Locking (`@Version`) ist nicht eingesetzt.
- Suche über `LOWER(x) LIKE`; eine bessere Suchstrategie (`pg_trgm`) ist an einer Stelle austauschbar, sobald eine einheitliche Listen-Abfrage existiert (siehe [pagination.md](pagination.md)).

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Scheduler, ShedLock | `platform/config/SchedulingConfig.java`, `platform/tenant/TenantBatchRunner.java` |
| Realtime Fan-out | `platform/realtime/publish/RealtimeRedisFanOut.java`, `RealtimePublisher.java` |
| Cache | `platform/config/CacheConfig.java`, `platform/cache/` |
| Outbox-Claim | `NotificationDeliveryRepository`, `ObjectPendingDeletionRepository`, `IdentityUserRepository` |
| Baseline-Sync | `standard/service/TenantBaselineSyncService.java`, `admin/tenant/service/TenantStandardsBootstrapService.java` |
| Object Storage | `platform/storage/service/ObjectStorageService.java`, `ObjectStorageBucketInitializer.java` |
| Konfiguration | `backend/src/main/resources/application.yml`, `backend/docker/Dockerfile` |
