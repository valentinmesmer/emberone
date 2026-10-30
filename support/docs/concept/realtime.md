# Realtime / WebSocket

Design-Notizen zu STOMP über natives WebSocket unter `/ws`. Feature-Flag (`emberone.realtime.enabled`, Default `true`). HTTP erlaubt `/ws/**`; echte Auth bei STOMP CONNECT / SUBSCRIBE.

## Mental Model

```
Client ──WS──▶ /ws ──STOMP──▶ CONNECT (JWT → EmberOnePrincipal)
                              SUBSCRIBE → TopicAuthorizationService
                                           ├ /user/queue/*        → erlauben (principal-scoped)
                                           ├ Mandanten-List-Topic → Mandanten-Match
                                           └ Entity-Topic         → TopicAuthorizer

Domänen-Event ──AfterCommit──▶ *RealtimeNotifier
                              └ RealtimePublisher
                                   └ Redis Pub/Sub (`emberone:realtime`)
                                        └ jede Instanz → RealtimeLocalDelivery → SimpMessagingTemplate
                                             ├ publishToTopic  → thin /topic/tenant.{t}.{domain}[.{id}]
                                             └ publishToUser   → /user/queue/{notifications|todos|workflows|permissions}
```

**Design-Split:** Fan-out-Topics für gemeinsame/List-Invalidierung (thin + Subscribe-ACL wo nötig); User-Queues für zielgerichtete Updates (Routing ist die ACL; rich Payloads bei Notifications/Todos, thin bei Workflows/Permissions).

## Konfiguration

| Einstellung | Wert |
|-------------|------|
| Endpoint | `/ws` (native WS, kein SockJS) |
| Broker | simple in-memory lokal: `/topic`, `/queue`; Fan-out über Redis Pub/Sub (`emberone:realtime`) |
| App / User Prefixes | `/app` (Publish heute ungenutzt), `/user` |
| Auth-Interceptor | `RealtimeAuthChannelInterceptor` client inbound |
| Token-Ablauf-Check | alle `emberone.realtime.token-expiry-check-interval-ms` (Default 30s) |
| Redis-Fan-out | `emberone.realtime.redis-channel` (Default `emberone:realtime`); STOMP-Heartbeats 10s/10s |

Frontend: `@stomp/stompjs` → `wss?://{api}/ws`, CONNECT-Header `Authorization: Bearer ...`.

## Topic-Namensgebung

Geparst von `RealtimeTopicPath`:

```
^/topic/tenant\.(\d+)\.([^.]+)(?:\.(\d+))?$
```

### Broadcast-Topics

| Pattern | Bedeutung | Auth-Tiefe |
|---------|-----------|------------|
| `/topic/tenant.{tenantId}.{domain}` | Mandantenweite Liste/Broadcast | Nur Mandanten-Match |
| `/topic/tenant.{tenantId}.{domain}.{entityId}` | Entity-scoped | Mandant + `TopicAuthorizer` |

In Nutzung: `appointments` (Liste), `appointment.{id}`, `events` (Liste), `service-event.{id}`, `operation-event.{id}`, `organization-memberships` (Liste), `workflow.{id}`.

### User-Queues

Principal-Name: `{tenantId}:{userId}`. Clients subscriben `/user/queue/...`; Spring routet pro User.

| Queue | Destination | Payload |
|-------|-------------|---------|
| notifications | `/user/queue/notifications` | Rich (`NotificationDto`) |
| todos | `/user/queue/todos` | Rich (`TodoDto`) |
| workflows | `/user/queue/workflows` | Thin |
| permissions | `/user/queue/permissions` | Thin |

### Event-Envelope (`RealtimeEvent`)

```
{ type, entityIds: Map<String,Long>, occurredAt, payload? }
```

- **Thin** (`payload == null`): Pflicht für alle `/topic`-Publishes.
- **Rich:** erlaubt auf User-Queues.

## Auth-Modell

### CONNECT

1. `Authorization: Bearer <token>` erforderlich
2. JWT decodieren; `EmberOneUserTokenClaims.isValidForUserFacingApi`
3. `tenantId` + `userId` + `exp` erforderlich
4. `EmberOnePrincipal` setzen (`name = "{tenantId}:{userId}"`)
5. Session + Ablauf in `RealtimeSessionRegistry` registrieren

### SUBSCRIBE

1. Authentifizierter Principal erforderlich
2. JWT in `SecurityContextHolder` binden (Broker-Threads ohne HTTP-Kontext)
3. `TopicAuthorizationService.authorizeSubscribe`

| Destination | Regel |
|-------------|-------|
| `/user/queue/*` | Erlaubt wenn CONNECT ok |
| Mandanten-Topic, passender Mandant | Erlaubt (keine Domain-ACL) |
| Mandanten-Topic, fremder Mandant | Verweigert |
| Entity-Topic | Passender Mandant + erster unterstützender `TopicAuthorizer` |
| Unbekanntes Pattern | Verweigert |

**List-Topic-Subtilität:** `/topic/tenant.{t}.appointments`, `/topic/tenant.{t}.events` und `/topic/tenant.{t}.organization-memberships` nur mandantenweit. Jeder authentifizierte Mandanten-User kann subscriben; Events sind thin Invalidation - REST-Re-Fetch wendet Audience- bzw. Permission-Regeln an.

### Token-Ablauf

`RealtimeTokenExpiryScheduler` schließt Sessions mit Close-Code **4001** (`access_token_expired`). Client reconnectet mit frischem Token.

## Topic-Authorizer

`AbstractTenantScopedTopicAuthorizer`: ablehnen wenn Domain unsupported oder kein `entityId`; `bindFromPrincipal` → `authorizeEntity` → Holder clear.

| Authorizer | Domain | Check |
|------------|--------|-------|
| `AppointmentTopicAuthorizer` | `appointment` | `APPOINTMENT_READ` + Audience-Scope |
| `EventTopicAuthorizer` | `service-event`, `operation-event` | `SERVICE_EVENT_READ` / `OPERATION_EVENT_READ` oder `EVENT_ATTENDANCE_READ` |
| `WorkflowTopicAuthorizer` | `workflow` | `WorkflowInstanceAccess.canView` |

Kein Authorizer für List-Domains `appointments`, `events` und `organization-memberships`. Nur User-Queues (ohne Entity-Topic): Todos, Notifications, Permissions. Workflows nutzen zusätzlich Entity-Topic `workflow.{id}` für die Detailansicht.

## Notifier pro Domäne

| Domäne | Trigger | Destination | Typen | Payload |
|--------|---------|-------------|-------|---------|
| Appointment | `AppointmentChangedEvent` | Entity + List Topics | `appointment.updated` | thin |
| Event | `EventChangedEvent`, `EventDeletingEvent`, Attendance add/remove | Entity-Topic (`service-event` / `operation-event`) + List `events` | `event.updated`, `event.deleted`, `event.attendance.changed` | thin |
| Org-Membership | Created / Updated / Ended | List `organization-memberships` | `organization-membership.changed` | thin (`organizationId`, `userId`) |
| Workflow | `WorkflowEventListener`-Adapter | Entity-Topic + `/user/queue/workflows` (Opener, Participants, Decisions, aktive Gruppenmitglieder) | `workflow.updated`, `workflow.comment.created` | thin |
| Todo | created/updated | `/user/queue/todos` (Assignees + Gruppenmitglieder) | `todo.created`, `todo.updated` | rich |
| Notification | created, IN_APP SENT | `/user/queue/notifications` | `notification.created` | rich |
| Permission | Permission-Change-Events | `/user/queue/permissions` | `permissions.changed` | thin |

Publisher nutzen `ObjectProvider<RealtimePublisher>` → No-op wenn Realtime aus. Sends verzögert bis nach DB-Commit bei aktiver Transaktion.

## Frontend (kurz)

| Baustein | Rolle |
|----------|-------|
| `lib/realtime/RealtimeContext.tsx` | STOMP-Client, CONNECT-Auth, auto User-Queue-Subs (notifications, todos, workflows, permissions) |
| `lib/realtime/topics.ts` | Topic-Builder (`buildEventTopic`, `buildTenantEventsTopic`, `buildTenantOrganizationMembershipsTopic`, …) |
| `lib/realtime/eventDispatcher.ts` | Event-Typ → react-query Invalidierung / Toast / Permissions-Refresh |
| `lib/realtime/useRealtimePollingFallback.ts` | Polling-Interval nur wenn Realtime aus oder disconnected |
| `AppointmentRealtimeSync` | Mandanten-List-Topic bzw. Entity-Topic für Termine |
| `EventRealtimeSync` / `EventDetailView` | Mandanten-List-Topic bzw. Entity-Topic für Dienst/Einsatz |
| `OrganizationMembershipRealtimeSync` | Mandanten-List-Topic für Org-Mitgliedschaften (Mitgliederlisten / my-organizations) |

Thin Events → invalidieren und re-fetch (`event.updated`/`event.deleted` → Event-Listen; `event.attendance.changed` → Attendance + Stunden-Totals; `organization-membership.changed` → Org-User-Listen + `myOrganizations`). Rich Notification/Todo können Cache direkt patchen (`applyNotificationCreated`, `applyTodoChanged`). Unread-/Count-Queries (Notifications, Todos, Workflows, Appointments) nutzen `useRealtimePollingFallback` statt festem Polling.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| WS-Config | `platform/realtime/config/RealtimeWebSocketConfiguration.java` |
| Auth | `platform/realtime/auth/RealtimeAuthChannelInterceptor.java` |
| Topics / Authorizer | `platform/realtime/topic/` |
| Publish / Redis-Fan-out | `platform/realtime/publish/RealtimePublisher.java`, `RealtimeRedisFanOut.java`, `RealtimeLocalDelivery.java`, `config/RealtimeRedisFanOutConfiguration.java` |
| Domain-Notifier | `{appointment,event,workflow,todo,notification,permission,userorganizationmembership}/realtime/` |

## Anhang: Optionaler manueller QA-Testplan (2 Browser)

Nur bei Bedarf (manuelles Smoke/QA). Voraussetzungen: `emberone.realtime.enabled=true`, `NEXT_PUBLIC_REALTIME_ENABLED` nicht `false`, Backend + Frontend, zwei User im selben Mandanten (A = Antragsteller, B = Approver in derselben Org-Gruppe).

**WS-Check:** DevTools → Network → `ws` → nach Login `/ws` Status 101; Badge-Counts ohne 60s-Polling.

| Szenario | Kurz |
|----------|------|
| Workflow-Claim (User-Queue) | A öffnet Workflow → B sieht Inbox/Badge ohne Reload; B claimt/entscheidet → A sieht Status ohne Reload |
| Event-Attendance (Entity-Topic) | B ändert Attendance → A sieht Liste ohne Reload; List-Topic `events` bei Edit/Delete |
| Org-Membership (List-Topic) | Eintritt approve → B sieht neues Member; A Cache/`myOrganizations` + ggf. `permissions.changed` |
| Fallback | Realtime aus / WS blockieren → Badges spätestens nach Polling (60s / Appointments 120s) |

Siehe auch: [Multi-Tenancy](multi-tenancy.md), [Benachrichtigungssystem](notification-system.md), [Token & Mandantenkontext](token-and-tenant-context.md), [Org- und Gruppen-Mitgliedschaften](user-memberships.md).
