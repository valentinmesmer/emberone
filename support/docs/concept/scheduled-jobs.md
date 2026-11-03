# Geplante Jobs

Design-Notizen: Index aller `@Scheduled`-Jobs im Backend. Scheduling ist in `platform.config.SchedulingConfig` aktiviert (`@EnableScheduling`, ShedLock über JDBC-Tabelle `shedlock`). Spring-Cron ist **6-stellig** (`Sek Min Stunde Tag Monat Wochentag`). Alle Cron-Jobs setzen `zone = "UTC"`. Cluster-weite Jobs tragen `@SchedulerLock`, damit pro Intervall nur eine Instanz läuft; Ausnahme: `RealtimeTokenExpiryScheduler` (JVM-lokale Sessions, jede Instanz).

## Mental Model

Inventar der Spring-`@Scheduled`-Methoden: die meisten Jobs laufen Mandanten über `TenantBatchRunner` (setzt `TenantContextHolder` pro Mandant für FORCE RLS; bei Fehlern pro Mandant weiter). Cron-Defaults und Env-Overrides stehen in `application.yml` unter `emberone.*`. Prozesslokale Ausnahme: Realtime-Token-Ablauf (kein Tenant-Batch, kein Cron-Zone).

## Inventar

Alle Cron-Zeiten sind **UTC** (`@Scheduled(..., zone = "UTC")`).

| Job | Cron / Delay (UTC) | Property / Env | Wirkung | Pro Mandant |
|-----|--------------------|----------------|---------|-------------|
| `AppointmentAutoArchiveScheduler.archiveEndedAppointments` | `0 0 2 * * *` (02:00 UTC) | `emberone.appointments.auto-archive-cron` / `EMBERONE_APPOINTMENTS_AUTO_ARCHIVE_CRON` | Archiviert Termine, deren Ende ≥ 4 Stunden zurückliegt | Ja |
| `RetentionScheduler.runRetention` | `0 30 3 * * *` (03:30 UTC) | `emberone.retention.cron` / `EMBERONE_RETENTION_CRON` | Führt alle Retention-Handler je Mandant aus (eigene Transaktion pro Handler), inkl. `NOTIFICATION` und `AUDIT` | Ja |
| `AppointmentSeriesExtensionScheduler.extendActiveSeries` | `0 0 4 * * *` (04:00 UTC) | `emberone.appointments.series-extension-cron` / `EMBERONE_APPOINTMENTS_SERIES_EXTENSION_CRON` | Verlängert aktive Serien im Materialisierungshorizont | Ja |
| `EntityAttachmentDraftCleanupScheduler.purgeStaleDraftAttachments` | `0 0 4 * * *` (04:00 UTC) | `emberone.attachments.draft-cleanup-cron` / `EMBERONE_ATTACHMENTS_DRAFT_CLEANUP_CRON` | Löscht veraltete Workflow-/Kommentar-Draft-Anhänge (`draft-retention-hours`, Default 12h) | Ja |
| `CertificationReminderScheduler.raiseCertificationReminders` | `0 0 6 * * *` (06:00 UTC) | `emberone.certifications.reminder-cron` / `EMBERONE_CERTIFICATIONS_REMINDER_CRON` | Öffnet deduplizierte Todos für fehlende / abgelaufene / auslaufende Zertifizierungen | Ja |
| `CourseOfferingReminderScheduler.sendCourseOfferingReminders` | `0 0 7 * * *` (07:00 UTC) | `emberone.course-offerings.reminder-cron` / `EMBERONE_COURSE_OFFERINGS_REMINDER_CRON` | Erinnert vor Lehrgangsbeginn Teilnehmer und Warteliste; je Angebot einmalig (`start_reminder_sent_at`), siehe [Lehrgangsangebot](courseoffering.md#benachrichtigungen) | Ja |
| `MembershipEndCleanupScheduler.cleanupEndedMemberships` | `0 * * * * *` (jede Minute, UTC) | `emberone.memberships.end-cleanup-cron` / `EMBERONE_MEMBERSHIPS_END_CLEANUP_CRON` | Wendet End-Seiteneffekte für Org- und Gruppenmitgliedschaften an, deren `endDate` verstrichen und `endProcessedAt` noch leer ist (downtime-sicher) | Ja |
| `NotificationOutboxScheduler.processOutbox` | `0 * * * * *` (jede Minute, UTC) | `emberone.notifications.outbox-cron` / `EMBERONE_NOTIFICATIONS_OUTBOX_CRON` | Verarbeitet PENDING E-Mail-Deliveries (`outbox-batch-size`, Default 100) | Ja |
| `ObjectStorageDeletionOutboxScheduler.processDeletionOutbox` | `0 * * * * *` (jede Minute, UTC) | `emberone.object-storage.deletion-outbox-cron` / `EMBERONE_OBJECT_STORAGE_DELETION_OUTBOX_CRON` | Löscht S3-Objekte aus `obj_pending_deletions`, je Mandant und danach Zeilen ohne `tenant_id` nach einer Tenant-Löschung (`deletion-outbox-batch-size`, Retry über `max-deletion-attempts`) | Ja |
| `IdentityDeprovisionScheduler.processPendingDeprovisions` | `0 * * * * *` (jede Minute, UTC) | `emberone.identity.deprovision-cron` / `EMBERONE_IDENTITY_DEPROVISION_CRON` | Löscht Authentik-Accounts für `idm_users.deprovision_pending_at` und leert das Feld | Ja |
| `RealtimeTokenExpiryScheduler.closeExpiredSessions` | fixed delay 30s | `emberone.realtime.token-expiry-check-interval-ms` | Schließt WebSocket-Sessions mit abgelaufenem JWT (Close-Code 4001) | Nein (prozesslokal) |

`RealtimeTokenExpiryScheduler` läuft nur bei `emberone.realtime.enabled=true` (gated auch `RealtimeWebSocketConfiguration`, das redundant `@EnableScheduling` deklariert).

## Verwandte Konfiguration (keine Schedules)

Begleitparameter neben den Cron-Properties aus dem Inventar (Batch-Größen, Cutoffs, Feature-Flags - **keine** zusätzlichen Cron-Ausdrücke; `emberone.retention.cron` steht im Inventar).

| Key | Default | Verwendet von |
|-----|---------|---------------|
| `emberone.notifications.outbox-batch-size` | `100` | Outbox-Batch |
| `emberone.course-offerings.start-reminder-lead` | `72h` | Vorlauf der Beginn-Erinnerung |
| `emberone.notifications.max-delivery-attempts` | `5` | Outbox-Retry |
| `emberone.attachments.draft-retention-hours` | `12` | Draft-Cleanup-Cutoff |
| `emberone.object-storage.deletion-outbox-batch-size` | `100` | S3-Lösch-Outbox-Batch |
| `emberone.object-storage.max-deletion-attempts` | `5` | S3-Lösch-Outbox-Retry |
| `emberone.identity.deprovision-batch-size` | `100` | IdP-Deprovision-Batch |
| `emberone.realtime.enabled` | `true` | Realtime-Scheduler + WS |

## Tagesablauf (Defaults, UTC)

```
02:00 UTC  Termin-Auto-Archiv
03:30 UTC  Retention (Löschklassen, inkl. Benachrichtigungen)
04:00 UTC  Terminserien-Verlängerung + Attachment-Draft-Cleanup
06:00 UTC  Zertifizierungs-Erinnerungen
*:* UTC    Mitgliedschafts-End-Cleanup (jede Minute, `endProcessedAt`)
*:* UTC    Benachrichtigungs-Outbox (jede Minute)
*:* UTC    S3-Lösch-Outbox (jede Minute)
*:* UTC    IdP-Deprovision (jede Minute)
~30s       Realtime-Token-Ablauf (wenn aktiv; kein Cron/UTC-Zone)
```

## Neuen Job hinzufügen

1. Bevorzugt eigene `*Scheduler`-Klasse im `schedule`-Package des Moduls.
2. Mandantenspezifische Daten: `TenantBatchRunner.forEachTenant(...)` nutzen.
3. Cron über `emberone.<module>.*-cron` in `application.yml` mit Env-Override.
4. In dieser Datei dokumentieren.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Aktivierung | `platform/config/SchedulingConfig.java` |
| Mandanten-Schleife | `platform/tenant/TenantBatchRunner.java` |
| Termine | `appointment/schedule/` |
| Attachments | `attachment/schedule/` |
| Mitgliedschaften | `userorganizationmembership/schedule/` |
| Zertifizierung | `certification/schedule/` |
| Benachrichtigung | `notification/schedule/` |
| Object Storage | `platform/storage/schedule/` |
| Identity / IdP | `identity/schedule/` |
| Retention | `retention/schedule/` |
| Realtime | `platform/realtime/auth/RealtimeTokenExpiryScheduler.java` |

Siehe auch: [Aufbewahrung und Löschung](retention-and-erasure.md), [Benachrichtigungssystem](notification-system.md), [Attachment & Dokument-Promotion](attachment-and-document-promotion.md), [Multi-Tenancy](multi-tenancy.md), [Realtime](realtime.md).
