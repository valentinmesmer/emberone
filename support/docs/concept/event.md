# Event-Modul (Dienst & Einsatz)

Design-Notizen für Ist-Betrieb: Dienst- und Einsatzereignisse plus Anwesenheit. Package root: `com.emberone.event`. Orthogonal zu [Appointment](appointment.md) (Planung) und [Todo](todo.md) (Inbox) - Events erzeugen keine Todos. Verwandt: [Zeiteintrag](timeentry.md). Event-basierte Zertifizierungs-Grants: [Training & Zertifizierung](trainingcourse-and-certification.md#event-basierte-zertifizierung-certificationeventgrant).

## Mental Model

| Konzept | Rolle |
|---------|-------|
| **ServiceEvent** | Dienst (topic) |
| **OperationEvent** | Einsatz (Nummer/Keyword/Tag/`specialSignal` Boolean) |
| **EventAttendance** | Anwesenheit an einem Event (polymorph: `eventType` + `eventId`, kein FK); kann Empfänger für `CertificationEventGrant` sein |
| **Appointment** | Geplanter Termin; kann zu Service-Event **promotet** und archiviert werden |

```
Appointment (optional promote)
        │
        ▼
EventService ──► ServiceEvent / OperationEvent
        │
        ▼
EventAttendanceService ──► Attendances (+ Time-Entries für Stunden-Totals)
```

## Was es besitzt

- `BaseEvent` (@MappedSuperclass), `ServiceEvent`, `OperationEvent`, `EventAttendance`, `EventType`
- CRUD Org-API mit `SERVICE_EVENT_*` / `OPERATION_EVENT_*` / `EVENT_ATTENDANCE_*` Permissions
- Org-Listen: `GET .../events/service` und `.../operation` mit `ListQuery` + `EventListFilter` (`from`/`to` auf `start`, Default-Fenster via `EventListDateRange`); Client-`sort` (je `asc`/`desc`, Standard `startAt,desc`): Dienste und Einsätze nur `startAt`; Anwesenheit pro Event paginiert (ohne `sort`)
- Self-API: nur eigene Attendances, als Batch pro Kategorie (`GET .../events/{eventCategory}/attendances/me`, `@BoundedList`)
- Zertifizierungs-Grants pro Event: `GET/POST .../certification-grants`, `GET/PUT/DELETE .../certification-grants/{grantId}` (Liste `@BoundedList`; Details: [Training Course & Certification](trainingcourse-and-certification.md#event-basierte-zertifizierung-certificationeventgrant))
- Bei Service-Create optional `promoteFromAppointmentId` → Appointment archivieren

| Controller | Prefix |
|------------|--------|
| `EventSelfController` | `/api/v1/self/organizations/{organizationId}/events` |
| `EventController` | `/api/v1/auth/organizations/{organizationId}/events` |

## Was es nicht besitzt

| Thema | Woanders |
|-------|----------|
| Geplante Termine / RSVP / Serien | `appointment` |
| Freie Zeiteinträge | `timeentry` (werden für Stunden-Totals mitgelesen) |
| Todos / Workflows | `todo` / `workflow` |

## Invarianten & Fallstricke

1. Attendance ohne FK - Löschen eines Events publiziert sync `EventDeletingEvent` und muss Attendances und Cert-Grants (`CertificationEventGrant`, siehe [Training & Zertifizierung](trainingcourse-and-certification.md#event-basierte-zertifizierung-certificationeventgrant)) explizit mitlöschen.
2. Es gibt keine Self-Event-Liste; Listen laufen über Org-API mit Permissions.
3. `OperationEvent.specialSignal` ist ein Boolean-Feld (Sondersignal), kein Textfeld.
4. Category-Pfade: `"service"` / `"operation"`.

## Realtime

Nach Commit publiziert `EventRealtimeListener` thin STOMP-Events:

| Domänen-Event | Realtime-Typ |
|---------------|--------------|
| `EventChangedEvent` | `event.updated` |
| `EventDeletingEvent` | `event.deleted` |
| `EventAttendanceAddedEvent` / `EventAttendanceRemovedEvent` | `event.attendance.changed` |

Destinations: Entity-Topic `/topic/tenant.{t}.service-event.{id}` bzw. `operation-event.{id}` plus Mandanten-List-Topic `events`. Subscribe auf Entity-Topics: `EventTopicAuthorizer` (Kategorie-Read oder Attendance-Read). List-Topic nur Mandanten-Match.

Frontend: `EventRealtimeSync` in `AuthLayout` (Listen), `EventDetailView` subscribt Entity-Topic. Dispatcher invalidiert Event-Listen bzw. gezielt Attendance-Caches. Details: [Realtime](realtime.md).

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Domain | `event/domain/{BaseEvent,ServiceEvent,OperationEvent,EventAttendance}.java` |
| Services | `event/service/EventService.java`, `EventAttendanceService.java` |
| Delete-Cleanup (sync) | `EventDeletionAttendanceCleanupListener`, `certification/…/EventDeletionCertificationGrantCleanupListener` |
| Domänen-Event | `event/event/EventDeletingEvent.java` |
| Realtime | `event/realtime/{EventRealtimeListener,EventRealtimeNotifier,EventTopicAuthorizer}.java` |
| Controller | `event/controller/EventController.java`, `EventSelfController.java` |
