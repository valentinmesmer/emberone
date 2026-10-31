# Zeiteintrag

Design-Notizen für freie Zeiteinträge (Start/Ende) an Organisation und optional Org-Gruppe. Package root: `com.emberone.timeentry`. Verwandt: [Event](event.md) (Stunden-Totals mischen Attendances + Time-Entries), [Zeitbehandlung](time-handling.md), [Sicherheit](security-and-permissions.md).

## Mental Model

| Konzept | Rolle |
|---------|-------|
| **UserTimeEntry** | Freier Zeitraum (`startTime` / optional `endTime`) an Org (+ optional Group) |
| **EventAttendance** | Anwesenheit an Dienst/Einsatz - orthogonal, fließt mit in Stats |

API-Zeiten: Organisations-Wanduhr (`yyyy-MM-dd'T'HH:mm:ss`), siehe [Zeitbehandlung](time-handling.md).

```
UserTimeEntryService
  (validateOrganizationWriteAccess:
   Subject-Membership immer nötig;
   Self: Membership reicht ohne WRITE)
        │
        ▼
user_time_entries  ──► Self-/Org-API
        │
        └──► ServiceHoursTotalService (Stunden-Totals)
```

## Was es besitzt

- Self-CRUD: Controller nur `isSelfServiceUser()`; Service: aktive Org-Membership des Subjects reicht (**ohne** `TIME_ENTRY_WRITE`)
- Org-User-APIs: `TIME_ENTRY_WRITE` allein reicht **nicht** - Subject muss Mitglied der Org sein; Group muss zur Org gehören
- Self-, User-Org- und Org-Listen-Controller (paginiert: `ListQuery`, `TimeEntryListFilter` mit `from`/`to` auf `startTime`, kein `userId`-Filter (Personensicht über den User-Org-Endpoint); Standardsortierung `startTime` absteigend; Client-`sort` (je `asc`/`desc`): `startAt` (`startTime`); zusätzlich `lastName` (Nachname, dann Vorname) nur in der Org-Liste; Suche `search` über `user.firstName`/`user.lastName`, nicht über die Beschreibung)
- Offenes Ende erlaubt (`endTime` null)

| Controller | Prefix |
|------------|--------|
| `UserTimeEntrySelfController` | `/api/v1/self/time-entries` |
| `UserTimeEntryUserOrganizationController` | `.../users/{userId}/time-entries` |
| `UserTimeEntryOrganizationController` | `.../user-time-entries` |

## Was es nicht besitzt

| Thema | Woanders |
|-------|----------|
| Event-Dauer / Attendances | `event` |
| Appointments | `appointment` |

## Fallstricke

`validateOrganizationWriteAccess` verlangt **immer** Subject-Membership (`assertUserBelongsToOrganization`); `TIME_ENTRY_WRITE` ohne Membership reicht nicht. Self-CUD: Membership genügt ohne Write-Permission. Absence verlangt `ABSENCE_WRITE` nur für mutierende Ops.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Entity | `timeentry/domain/UserTimeEntry.java` |
| Service | `timeentry/service/UserTimeEntryService.java` |
| Listen | `timeentry/list/*` |
| Controller | `timeentry/controller/*` |
