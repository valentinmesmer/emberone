# Appointment (Termine)

Design-Notizen für geplante Termine inkl. Serien und RSVP - getrennt von Ist-Betrieb-Events. Package root: `com.emberone.appointment`. Events/Attendance: [event.md](event.md) (`com.emberone.event`). Zeitformate: [Zeitbehandlung](time-handling.md). Jobs: [Geplante Jobs](scheduled-jobs.md). Anhänge: [Attachment](attachment-and-document-promotion.md).

## Mental Model

| Konzept | Rolle |
|---------|-------|
| **Appointment** | Geplanter Termin (Einzel oder Serien-Occurrence); `responseMode` steuert ob/wie RSVP möglich ist |
| **AppointmentSeries** | RRULE-Serie; Occurrences erben Mode/Deadline vom Template und werden materialisiert |
| **AppointmentResponse** | RSVP-Wert (`YES` / `NO` / `UNCERTAIN`) + optionaler Kommentar; nur wenn Mode ≠ `NONE` |
| **Event** (`ServiceEvent` / `OperationEvent`) | Ist-Betrieb nach dem Termin; Attendance nachträglich |
| **Promote** | Appointment → ServiceEvent; Termin wird archiviert (kein dauerhafter FK) |

```
AppointmentSeries 1──* Appointment
Appointment 1──* Response / Read / AudienceGroups
Appointment --promote--> ServiceEvent → EventAttendance
```

## Kernentitäten (RSVP)

### `AppointmentResponseMode`

| Mode | Erlaubte Werte (Backend `validateResponse`) | Kommentar |
|------|---------------------------------------------|-----------|
| `NONE` | - | Keine Antworten; Submit wird abgelehnt |
| `YES_NO` | YES, NO | **einziger** Mode, der `UNCERTAIN` serverseitig ablehnt; kein Kommentar |
| `YES_NO_UNCERTAIN` | YES, NO, UNCERTAIN | kein Kommentar |
| `YES_NO_WITH_COMMENT` | YES, NO, UNCERTAIN | Kommentar erlaubt; Backend lässt `UNCERTAIN` zu (UI kann die Option ausblenden) |
| `YES_NO_UNCERTAIN_WITH_COMMENT` | YES, NO, UNCERTAIN | Kommentar erlaubt |

`NONE` = keine Responses (weder UI noch API). Mode `NONE` normalisiert `responseDeadlineMinutesBefore` auf `0`. Archivierte Termine: Responses abgelehnt.

### `responseDeadlineMinutesBefore`

Spalte `INT NOT NULL DEFAULT 0`. Minuten **vor** `start`. Deadline = `start - minutes` (`AppointmentResponseDeadlineUtil`). `null` wird wie `0` behandelt; `hasDeadline` nur bei `> 0`. Nach Ablauf der Deadline sind weitere Responses abgelehnt.

## Was es besitzt

- Termine (`Appointment`: `archived` + `archived_at`, `isException`, Audience - [Lifecycle & Statusfelder](lifecycle-and-status-fields.md))
- Serien (`AppointmentSeries`: `recurrenceRule`, Horizont, Template-Felder inkl. Mode/Deadline)
- RSVP (`AppointmentResponse`: YES/NO/UNCERTAIN) inkl. `AppointmentResponseMode` und `responseDeadlineMinutesBefore`
- Unread-Tracking (`AppointmentRead`)
- Audience: `ALL` | `SELECTED_GROUPS`
- Dokumente am Termin (ohne Promotion zu UserDocuments); Delete publiziert `AppointmentDeletingEvent` (sync Attachment-Cleanup) und `AppointmentChangedEvent(DELETED)` (auch bei Serien-Scopes)
- Auto-Archive und Series-Extension (Scheduler)
- Realtime (`AppointmentChangedEvent`)

`AppointmentType` heute nur `SERVICE`. Series-Edit: `THIS_OCCURRENCE` | `THIS_AND_FUTURE` | `ENTIRE_SERIES`.

## Was es nicht besitzt

| Thema | Woanders |
|-------|----------|
| Service-/Operation-Events | `com.emberone.event` |
| Event-Attendance | `EventAttendanceService` |
| Operations-Termine | nur als `EventType.OPERATION` - kein Appointment-Typ |
| Modul-Toggle | `Module.APPOINTMENTS` via [Module-Config](module-and-standard-module-config.md) |

**Appointment ≠ Event:** RSVP voraus, Attendance am Event danach.

## Key Flows

1. **CRUD / Serie** - `AppointmentService` + `AppointmentRecurrenceService` materialisieren Occurrences (Horizont 12 Monate, Cap 500); Extension-Scheduler verlängert aktive Serien
2. **Antworten** - `AppointmentResponseService.validateResponse`: `NONE` / archiviert / abgelaufene Deadline → Reject; `UNCERTAIN` nur bei Mode `YES_NO` blockiert
3. **Promote → Event** - Zeiten müssen matchen, Termin gestartet, Typ SERVICE↔SERVICE → Appointment `archived=true`
4. **Auto-Archive** - Termine mit Ende ≥ 4h zurück

Permissions: `APPOINTMENT_READ/WRITE` unter `Module.APPOINTMENTS`; Event-CRUD unter `SERVICE` / `OPERATIONS`.

## Listen-API (Org)

`GET /api/v1/auth/organizations/{organizationId}/appointments` - `ListQuery` + `AppointmentListFilter` mit `period=UPCOMING|PAST` (fehlend: `UPCOMING`; `UPCOMING`: `end >= now`, `start` aufsteigend; `PAST`: `end < now`, `start` absteigend; ohne `sort` folgt die Sortierung der Periode; Client-`sort` hat Vorrang vor der Periode, erlaubt: `start`, `name` je `asc`/`desc`; Name ohne Groß-/Kleinschreibung). Suche (`search`) nur über `name`. Zusätzlich bleibt ein expliziter Zeitraum `from` (inkl.) / `to` (exkl.) für Lookups wie den Anwesenheits-Import von Terminen: ist `from` oder `to` gesetzt, wird `period` ignoriert und es gilt die Überlappung mit dem Fenster (Default −1/+12 Monate, max. Spanne = Serien-Horizont, Standardsortierung `start` aufsteigend, ein Client-`sort` hat auch hier Vorrang). Antwort: `AppointmentPageDto`. Anhänge pro Termin: `GET …/attachments` mit `@BoundedList` (unpaginiert).

Frontend: `appointmentListDefinition`, `useAppointmentList` / `useAppointmentListState`, `ListControls` in `AppointmentListView`.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Entities | `appointment/domain/` |
| Response-Mode / Deadline | `AppointmentResponseMode`, `AppointmentResponseDeadlineUtil` |
| Service / Recurrence | `appointment/service/` |
| Auto-Archive / Extension | `appointment/schedule/` |
| Promote-Ziel | `event/…/EventService.createServiceEvent` |
| Dokumente | `appointment/…/AppointmentDocumentService` |
| Org-Listen-Query | `appointment/list/*`, `AppointmentController.listAppointments` |
