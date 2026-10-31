# Benachrichtigungssystem

Design-Notizen zur transaktionalen Outbox um `ntf_notifications` (logische Nachricht) und `ntf_deliveries` (Sendestatus pro Kanal). Producer senden transaktionale Benachrichtigungen nie direkt als E-Mail, sie rufen `NotificationService.notify(...)` auf. Willkommen-Mail und E-Mail-Bestätigung sind **keine** Notification-Typen; sie gehen direkt über `MailTemplateService`. Package root: `com.emberone.notification` (Retention-Handler im Retention-Modul). Inbox-Listen-GET: [Paginierung von Listen](pagination.md).

## Mental Model

```
Domänen-Event / Adapter
  → NotificationService.notify(NotificationRequest)
      → Kanäle auflösen (Defaults + User-Overrides)
      → Notification + NotificationDelivery(s) einfügen
      → NotificationCreatedEvent publizieren
          → IN_APP sofort SENT → STOMP (siehe Realtime)
          → EMAIL bleibt PENDING

NotificationOutboxScheduler (jede Minute, pro Mandant)
  → EmailChannelSender → type.renderEmail → Thymeleaf-Mail
```

Termine nutzen **dieses** Modul heute nicht (nur Realtime).

## Kanäle

| Kanal | Create-Status | Send-Pfad |
|-------|---------------|-----------|
| `IN_APP` | `SENT` | Inbox + WebSocket |
| `EMAIL` | `PENDING` | Outbox → `EmailChannelSender` |

Nur eine `ChannelSender`-Implementierung: E-Mail.

## Typ-Registry

`NotificationType`-Enum + ein Spring-`NotificationTypeDefinition`-Bean pro Wert. Startup **schlägt fehl** bei fehlendem oder doppeltem Enum-Wert (`NotificationTypeDefinitionRegistry`).

| Typ | Default-Kanäle | E-Mail-Template |
|-----|----------------|-----------------|
| `WORKFLOW_OPENED` | IN_APP + EMAIL | generisch `notification` |
| `WORKFLOW_APPROVED` | IN_APP + EMAIL | generisch |
| `WORKFLOW_REJECTED` | IN_APP + EMAIL | generisch |
| `WORKFLOW_CANCELLED` | IN_APP + EMAIL | generisch |
| `WORKFLOW_INPUT_CHANGE_REQUESTED` | IN_APP + EMAIL | generisch |
| `TODO_ASSIGNED` | IN_APP + EMAIL | generisch (i18n nach Todo-Typ) |
| `USERNAME_CHANGED` | IN_APP + EMAIL | `username-changed` |
| `COURSE_REGISTRATION_REMOVED` | IN_APP + EMAIL | generisch (Org hat Teilnahme oder Wartelistenplatz mit Begründung beendet) |
| `COURSE_OFFERING_CANCELLED` | IN_APP + EMAIL | generisch (Org hat das Angebot abgesagt; Thema und Begründung; ein Event, eine Anfrage an alle Betroffenen) |
| `COURSE_OFFERING_SESSIONS_CHANGED` | IN_APP + EMAIL | generisch (Termine oder Orte eines veröffentlichten Angebots geändert; Teilnehmer und Warteliste) |
| `COURSE_OFFERING_START_REMINDER` | IN_APP + EMAIL | generisch (Erinnerung vor Lehrgangsbeginn an Teilnehmer und Warteliste; Scheduler) |

SPI (`NotificationTypeDefinition`): `type()`, `defaultChannels()`, `derivedSourceKeys()`, `enrichDisplayParams`, `messageKeyPrefix`, `renderEmail`. `AbstractNotificationTypeDefinition` für generischen E-Mail-Pfad.

User-Präferenzen (`ntf_user_channel_overrides`) überschreiben Definition-Defaults pro Kanal×Typ. `GET /api/v1/self/notification-preferences` liefert alle Typen in Deklarationsreihenfolge von `NotificationType` (= Anzeigereihenfolge, kein Client-Sort).

**Hinweis:** `WORKFLOW_CANCELLED` geht an den Antragsteller bei Abbruch (inkl. `WORKFLOW_STALE`), nicht aber, wenn er selbst abbricht oder deaktiviert ist. `WORKFLOW_OPENED` bleibt ungenutzt - Genehmiger werden über Todos informiert.

## Outbox / Retry / Retention

| Property | Default | Bedeutung |
|----------|---------|-----------|
| `emberone.notifications.outbox-cron` | `0 * * * * *` | PENDING Deliveries abarbeiten |
| `max-delivery-attempts` | `5` | Retries vor FAILED |
| `outbox-batch-size` | `100` | Pro Mandant-Tick |

Outbox via `TenantBatchRunner`. Erfolg → `SENT`; unreachable recipient → `FAILED` (kein Retry); andere Fehler erhöhen `attempts`.

**Retention:** Löschklasse `NOTIFICATION` im Retention-Modul (`RetentionScheduler`, Frist aus dem Aufbewahrungsprofil des Bundeslands). **Ungelesen** (`read_at IS NULL`): Hard Delete, wenn `created_at` vor dem Stichtag liegt (Bayern-Default: 1 Monat). **Gelesen** (`read_at IS NOT NULL`): beim nächsten Retention-Lauf, unabhängig vom Alter. `ntf_deliveries` folgt per `ON DELETE CASCADE`. Siehe [Aufbewahrung und Löschung](retention-and-erasure.md) und [Geplante Jobs](scheduled-jobs.md).

## Domänen-Anbindung

| Domäne | Pfad |
|--------|------|
| **Workflow** | `WorkflowNotificationAdapter` → Ersteller bei APPROVED / REJECTED / INPUT_CHANGE_REQUESTED; Genehmiger via Todos |
| **Todo** | `TodoNotificationListener` → `TODO_ASSIGNED` bei Create (`notifiesOnCreate`; Claim unterdrückt) und bei VisibilityGained (Membership-Join / Gruppen-Remap) |
| **Zertifizierung** | Reminder-Cron öffnet `CERTIFICATION_EXPIRING`-Todos → `TODO_ASSIGNED` (kein eigener Notification-Typ) |
| **Identity** | Username via `IdentityNotificationListener`; Welcome-Mail via `MailTemplateService` |
| **User** | E-Mail-Bestätigung via `MailTemplateService` (`UserNotificationListener`) |
| **Termin** | Keine |

Workflow-Ersteller-Alerts ≠ Genehmiger-Alerts: Ersteller Lifecycle-Notifications; Genehmiger Todos (nur manche Todo-Typen notify).

## Self-API / Inbox (My Area)

| Methode | Pfad | Zweck |
|---------|------|--------|
| `GET` | `/api/v1/self/notifications` | Paginierte Inbox (`ListQuery` + `status=UNREAD\|READ`, Default `UNREAD` ohne Param) |
| `GET` | `/api/v1/self/notifications/{id}` | Einzelne In-App-Benachrichtigung für Detail (Empfänger + IN_APP-Kanal) |
| `GET` | `/api/v1/self/notifications/unread-count` | Badge-Zähler (unabhängig von Listen-Filter) |
| `POST` | `/api/v1/self/notifications/{id}/read` | Einzeln gelesen |
| `POST` | `/api/v1/self/notifications/read-all` | Alle gelesen |

Kein Client-`sort`; Standard `createdAt,desc`. Kein `unreadOnly`-Parameter mehr - Lesestatus nur über `status`.

Frontend: `notificationListDefinition` (`frontend/lib/list/notificationList.ts`), `ListControls` + `usePagedList` in `lib/store/queries/notifications.ts`, Views unter `components/notification/`.

## Neuen Benachrichtigungstyp hinzufügen

1. Enum-Wert in `NotificationType`.
2. `@Component` `NotificationTypeDefinition` (meist `AbstractNotificationTypeDefinition`) mit Defaults, optional `enrichDisplayParams` / `messageKeyPrefix` / custom `renderEmail`.
3. i18n in `messages_de.properties`: `{prefix}.title` / `{prefix}.body` (Default-Prefix `notification.{TYPE}`; Todos `todo.{TODO_TYPE}`).
4. Optional: Custom Thymeleaf unter `templates/mail/` + `mail.*`-Keys.
5. Producer, der `notificationService.notify(NotificationRequest.builder()...)` aus Adapter/Listener aufruft - Domänenservices nicht aufblähen.
6. Über Todos: `notifiesOnCreate` am Todo-Typ, `TODO_ASSIGNED` mit Todo-Typ-Text (Zertifizierungs-Muster). Neuer `NotificationType` nur wenn Event nicht „Todo zugewiesen“.
7. Param-Key-Konstanten auf `NotificationParamKeys` (oder Domain `*ParamKeys`); abgeleitete Display-Keys via `derivedSourceKeys()`.
8. Tests; Registry-Vollständigkeit beim Startup erzwungen.

## E-Mail-Rendering

1. Empfänger auflösen (`UserContactInformation.privateEmail`).
2. `typeDefinitions.require(type).renderEmail(context)`.
3. `MailService.sendTemplateMail` → `templates/mail/`.

Shared Fragment: `_brand-header.html`. Display-Anreicherung: `NotificationDisplaySupport` + pro Typ `enrichDisplayParams`.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Entry / Outbox | `notification/service/` |
| Retention | `retention/handler/NotificationRetentionHandler.java` |
| Kanäle | `notification/channel/` |
| Typen | `notification/domain/NotificationType.java`, `notification/type/` |
| Scheduler | `notification/schedule/` |
| Adapter | `notification/adapter/workflow/WorkflowNotificationAdapter`, `todo/.../TodoNotification*`, `identity/.../IdentityNotificationListener` |
| Realtime | `notification/realtime/` |
| Listen-Query | `notification/list/*`, `notification/controller/NotificationSelfController.java` |

Siehe auch: [Realtime](realtime.md), [Workflow-Engine](workflow-engine.md), [Geplante Jobs](scheduled-jobs.md), [Aufbewahrung und Löschung](retention-and-erasure.md), [Paginierung von Listen](pagination.md).
