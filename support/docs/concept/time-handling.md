# Zeitbehandlung (Time Handling)

Design-Notizen für einheitliche Datum/Uhrzeit-Regeln in API, Backend und Frontend. Verwandt: [Appointment](appointment.md), [Event](event.md), [Zeiteintrag](timeentry.md), [Workflow-Engine](workflow-engine.md).

## Mental Model: Zwei Semantiken

EmberOne unterscheidet **zwei Arten** von Zeitwerten. Jede hat ein festes Format und klare Regeln.

| Semantik | Bedeutung | API-Format | Java-Typ (DTO) | DB |
|----------|-----------|------------|----------------|-----|
| **Organisations-Wanduhr** | User gibt Uhrzeit ein oder liest sie als „14:30“ | `yyyy-MM-dd'T'HH:mm:ss` (ohne Offset) | `Instant` + `@OrganizationWallClock` | `TIMESTAMPTZ` (echtes UTC) |
| **System-Instant** | Nur Backend setzt den Wert (Audit, Read-Tracking) | ISO-8601 mit `Z` (Jackson-Default) | `Instant` (ohne Annotation) | `TIMESTAMPTZ` |

**Regel für neue Felder:** Kann der User die Uhrzeit eingeben oder als Wanduhr lesen? → `@OrganizationWallClock`. Setzt nur das System den Wert? → Default-`Instant`.

## Organisations-Wanduhr

### Konfiguration

```yaml
emberone:
  time:
    organization-zone: ${EMBERONE_ORGANIZATION_ZONE:Europe/Berlin}
```

Default: `Europe/Berlin`. Später pro Tenant überschreibbar.

### Backend

| Komponente | Ort |
|------------|-----|
| Converter | `platform/time/OrganizationWallClockTime.java` |
| Annotation | `platform/time/OrganizationWallClock.java` |
| Jackson (de)serialisierung | `OrganizationWallClockInstantSerializer/Deserializer` |
| Request-Parameter | `OrganizationWallClockInstantFormatter` (Web MVC) |
| Config | `OrganizationTimeConfiguration.java` |

**Ein- und Ausgang:**

```
API "2026-06-12T14:30:00"
  → parse in Europe/Berlin
  → Instant (UTC) in Domain/DB
  → format in Europe/Berlin
  → API "2026-06-12T14:30:00"
```

Scheduler, Deadlines und Validierungen arbeiten auf **echten UTC-Instants**.

### Felder mit `@OrganizationWallClock`

- **Termine:** `start`, `end`, `seriesEnd`, `occurrenceStart`, `lastRespondedAt`
- **Events:** `start`, `end`
- **Serientermine:** `seriesEnd` (+ separates `timezone` für RRULE)
- **Zeiterfassung:** `startTime`, `endTime`
- **Mitgliedschaften:** `startDate`, `endDate` (Org- und Gruppen-Membership-DTOs / Update-Requests)
- **Onboarding:** `invitedAt`
- **Workflows (UI):** `decidedAt`, `requestedAt`, `fulfilledAt`, `claimedAt`
- **User-Dokumente:** `viewedAt`

Qualifikations-Org-Decisions speichern `decidedAt` / `revokedAt` als Domain-`Instant` (System-Zeitstempel), nicht als `@OrganizationWallClock`-API-Felder.

### Frontend

| Aufgabe | Hilfsfunktion / Komponente |
|---------|----------------------------|
| Anzeige | `formatDateTime`, `formatTime`, `formatDateShort` in `lib/utils/date.ts` |
| Formular-Eingabe | `DateTimePicker` → `yyyy-MM-ddTHH:mm:ss` |
| API senden | Wanduhr-String direkt (kein `toISOString()`) |
| „Jetzt“ für API/Cache | `getOrganizationWallClockNow()` |
| Default-Start Event-Formular | `getCurrentDateTime()` (`CreateEventModal`) |
| Default-Start Termin-Formular | `getNextMinuteDateTime()` (`appointmentFormUtils`) |

Locale: `de-DE` (Browser-Zeitzone für Anzeige; API-Wanduhr entspricht Organisations-TZ).

**Achtung:** `getCurrentDateTime()` / `getOrganizationWallClockNow()` / `getNextMinuteDateTime()` lesen `new Date()`, also die **Browser-lokale** Zeit - keine Konvertierung gegen die Organisations-TZ. Sie unterstellen Browser-TZ = Organisations-TZ. Bei abweichender Client-Zeitzone weicht die gesendete Wanduhr-Zeit vom tatsächlichen „jetzt" der Organisation ab.

## System-Instant

Felder, die **nicht** vom User als Wanduhr editiert werden:

- `Notification.readAt`
- `Todo.archivedAt`, `completedAt` (sofern nur System)
- interne Domain-Felder ohne DTO-Exposure

**Frontend:** `new Date(isoString)` + `formatDateTime`. Optimistic Updates dürfen `toISOString()` nutzen.

## Zeiterfassung

Zeiterfassung nutzt dieselbe **Organisations-Wanduhr**-Semantik wie Termine (seit Vereinheitlichung). Früher `OffsetDateTime` + ISO-Offset, jetzt `Instant` + `@OrganizationWallClock` in DTOs, `Instant` in der Entity.

Stunden-Summen (`UserTimeEntryService.sumHoursForUserInYear`, `EventAttendanceService.getEventHoursForUser`) filtern nach **Kalenderjahr in Organisations-TZ**, nicht UTC-Mitternacht.

## Serientermine

Zusätzlich zur Wanduhr:

- `RecurrenceRequest.timezone`: IANA-ID (Frontend: Browser-TZ, Fallback `Europe/Berlin`)
- RRULE-`UNTIL` in der Rule bleibt UTC (`…Z`): iCal-Spezifikation
- Expansion im Backend über `AppointmentRecurrenceService` + Org-/Series-TZ

## Was nicht tun

- Kein Wanduhr-Passthrough (String = UTC ohne Umrechnung)
- Kein `toISOString()` für user-facing API-Felder
- Keine `@OrganizationWallClock` auf reinen System-Timestamps
- Keine User-Timezone-Preference (aktuell), Org-TZ ist Source of Truth

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Kern-Converter | `backend/.../platform/time/` |
| Termine | `backend/.../appointment/dto/*` |
| Events | `backend/.../event/dto/*` |
| Zeiterfassung | `backend/.../timeentry/`, `event/service/EventAttendanceService.java`, `user/service/ServiceHoursTotalService.java` |
| Frontend Utils | `frontend/lib/utils/date.ts` |
| DatePicker | `frontend/components/ui/date-picker.tsx` |
| DateTimePicker | `frontend/components/ui/date-time-picker.tsx` |
| Date-Field-Segment | `frontend/components/ui/date-field-segment.tsx` |
