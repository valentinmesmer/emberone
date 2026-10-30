# Training Course & Certification

Design-Notizen für Mandanten-Kataloge und User-Nachweise für Lehrgänge, Zertifizierungen und Führerscheine. Packages: `com.emberone.trainingcourse`, `certification`, `drivinglicense`. Katalog-Lifecycle (`active` / `archived`): [Lifecycle & Statusfelder](lifecycle-and-status-fields.md). Qualifikation konsumiert alle drei - siehe [Qualifikation](qualification.md). Führerschein-Details: [Führerschein](driving-license.md). Standard-Filter: [Standard](standard.md). Reminder/Todos: [Todo](todo.md), [Geplante Jobs](scheduled-jobs.md).

## Mental Model

```
TenantTrainingCourseType (config_key)     TenantCertDefinition (config_key)
        ↑ grantedTypes                              ↑ assignment + record
TrainingCourseUserSubmission          UserCertificationAssignment
                                      UserCertificationRecord
        │                                       │
        └──────────► Qualification (Prerequisites via config_key) ◄────────┘
                              ▲
        UserDrivingLicense (document + classes)
```

Keine gemeinsame Entity zwischen Lehrgang, Zertifizierung und Führerschein - Kopplung nur über Qualification und Domain-Events.

Katalog-Identität: `config_key` ist das Kürzel. Ändert sich das Kürzel, werden Standard-Selections und Qualifikations-Voraussetzungen mitgezogen.

## Was es besitzt

### Training (`trainingcourse`)

- Mandanten-Katalog Kursarten (`TenantTrainingCourseType`)
- User-Nachweise als **Submission** (ein Nachweis → mehrere granted types)
- Dokumente am Submission; Workflow `TRAINING_COURSE_RECORD`

### Certification (`certification`)

- Mandanten-Katalog Definitionen (`TenantCertDefinition`: `intervalDays`, `reminderDaysBeforeExpiry`, `evidenceTypes[]`)
- **Zuweisungen** (Pflicht, `UserCertificationAssignment`) vs. **Records** (Nachweis, `UserCertificationRecord`)
- Status **berechnet**, nicht persistiert: `MISSING` | `IN_REVIEW` | `EXPIRED` | `EXPIRING_SOON` | `VALID` | `ARCHIVED`
- Reminder → deduplizierte Todos (`CERTIFICATION_EXPIRING`)
- Zuweisungs-/Nachweis-Quellen (`CertificationAssignmentSource` / `CertificationSubmissionMethod`): `MANUAL` (Org-CRUD) | `WORKFLOW` (`CERTIFICATION_RECORD`, ein Workflow pro Definition) | `EVENT` (Event-Grant, siehe unten) | `ONBOARDING` (Approve einer Onboarding-Einladung, App-Link `ONBOARDING_INVITATION`)
- `TenantCertDefinitionEvidenceType` (`CertEvidenceType`: `DOCUMENT`, `SERVICE_EVENT`, `OPERATION_EVENT`) steuert, welche Nachweiswege pro Definition erlaubt sind; nur Event-Typen konfiguriert ⇒ Self-Service/manuell gesperrt (`isEventOnlyEvidence`)
- Führerschein-Kontrolle (`FSK` in Bayern-Baseline): eine Zertifizierung, nicht pro Klasse, Qualifikation bindet FSK über `certDefinitions[]`, Klassen über `drivingLicenses[]` (siehe [Führerschein](driving-license.md))

### Event-basierte Zertifizierung (`CertificationEventGrant`)

Ein Org-User konfiguriert pro Event (Dienst/Einsatz) **einen oder mehrere** `CertificationEventGrant`s - je Cert-Definition höchstens einen (Unique: Tenant + Event-Typ + Event-ID + Definition). Felder: Cert-Definition (nur beim Anlegen) + `recipientMode` (`ALL_ATTENDEES` | `SPECIFIC`) + `completedDate` (= Event-Startdatum). Erlaubt nur, wenn die Definition `SERVICE_EVENT`/`OPERATION_EVENT` als Evidence Type führt. Definition ist nach dem Anlegen unveränderlich; Update ändert nur Empfänger/Notizen; Entfernen über `DELETE …/{grantId}` (inkl. verknüpfter Records/EVENT-Assignments).

| Trigger | Effekt (`CertificationEventGrantService.reconcile`) |
|---------|------|
| Grant angelegt/geändert | Für jeden effektiven Empfänger: `Assignment` (Source `EVENT`) + `Record` (Method `EVENT`, App-Link `CERTIFICATION_EVENT_GRANT`→Grant-ID) anlegen/syncen |
| Grant gelöscht | Records + EVENT-Assignments für diesen Grant entfernen |
| Teilnahme hinzugefügt (`EventAttendanceAddedEvent`) | `ALL_ATTENDEES`-Grants reconcilen (neue Person aufnehmen) |
| Teilnahme entfernt (`EventAttendanceRemovedEvent`) | Bei `SPECIFIC` Empfänger-Zeile löschen; `reconcile` entfernt Nachweise für Personen außerhalb der effektiven Empfänger (auch `ALL_ATTENDEES`) |
| Event gelöscht | Alle Grants inkl. Records/Assignments entfernen |

Nachweis gilt als „bearbeitet“ (`edited`), wenn `completedDate`/`notes` vom Record abweichen, reines UI-Warnsignal; `reconcile` überschreibt diese Felder beim nächsten Sync trotzdem aus dem Grant.

Endpunkte (Präfix `/api/v1/auth/organizations/{orgId}`): `GET …/certification-grants`, `GET/PUT/DELETE …/certification-grants/{grantId}`, `POST …/certification-grants`.

UI: Event-Detail listet Grants; Zeile führt auf Grant-Detail (`…/department/{service|operation}/{eventId}/certification-grants/{grantId}`). App-Link `CERTIFICATION_EVENT_GRANT` auf Zertifizierungs-Records navigiert auf das **Event** (`…/department/{service|operation}/{eventId}`), nicht auf das Grant-Admin-Detail - Frontend zeigt den Link nur bei Event-Read-Permission (`SERVICE_EVENT_READ` / `OPERATION_EVENT_READ`). Todos/Notifications nutzen diesen Typ derzeit nicht (`AppNavigationResolver` liefert dafür `null`).

## Was es nicht besitzt

| Thema | Hinweis |
|-------|---------|
| Qualification-Zusammensetzung / Org-Decision | Qualification-Domain |
| Training-Assignments / Training-Reminder | **gibt es nicht** |
| Org-Materialisierung der Kataloge | nur Standard-**Selections** zur Read-Time |
| Document-Upload selbst | Attachment-/Promotion-Layer |

## Key Flows

1. **Katalog:** Admin pflegt Tenant-Typen → Standard selektiert `config_key`s → Org-Katalog via `*CatalogService` + `EffectiveStandardResolver`. Hard-Delete publiziert `TenantCertDefinitionDeletedEvent` / `TenantTrainingCourseTypeDeletedEvent` / `TenantDrivingLicenseDefinitionDeletedEvent` → `TenantCatalogDefinitionDeletedQualificationCleanupListener` räumt Prerequisite-Links. Cert-Archive: `TenantCertDefinitionArchivedEvent` → Reminder-Todos canceln.
2. **Lehrgang-Nachweis:** manuell, Workflow oder Onboarding-Promote (`createSubmissionFromOnboarding`, App-Link `ONBOARDING_INVITATION`) → Document-Promotion → Submission + granted types → `TrainingCourseRecordChangedEvent`
3. **Zertifizierung:** Assignment → Record (manuell, Workflow, Event-Grant oder Onboarding-Promote) → Status-Berechnung; Reminder via `CertificationAssignmentChangedEvent` / Scheduler (kein direkter `TodoService` im Assignment-Service)
4. **Qualification:** liest granted Kursarten + Cert-Status + Führerschein-Klassen (+ optional Org-Decision); Voraussetzungen und Sync: [Qualifikation](qualification.md)

Permissions unter `Module.CORE`: `TRAINING_COURSE_*`, `CERTIFICATION_*` (siehe [Sicherheit](security-and-permissions.md)).

Org-Listen `GET …/certifications/assignments` und `…/records`: paginiert (`page`/`size`, max. 200), Antwort `PageDto`. Die Lehrgangs-Records (`…/records`, Self und Org) unterstützen `sort` mit `endDate` (Standard `endDate,desc`), `name`.

## API-Endpunkte (Training)

| Bereich | Pfad |
|---------|------|
| Self (Records) | `GET /api/v1/self/training-courses/records` (`ListQuery`, `TrainingCourseUserRecordPageDto`) |
| Self (Katalog) | `GET /api/v1/self/training-courses/available-types` (`@BoundedList`, Membership-Org-Katalog) |
| Org (CRUD / Katalog) | `/api/v1/auth/organizations/{orgId}/users/{userId}/training-courses/…`; Records paginiert; `available-types` / `pending-submissions` `@BoundedList` |
| Workflow (Self) | `POST /api/v1/auth/workflows/training-course-record` |

Self-Katalog entspricht Cert `requestable-definitions`: My-Area und Self-Workflows dürfen **nicht** den Org-Endpoint mit `TRAINING_COURSE_READ` nutzen.

## Zertifikats-Statusübersicht (Org)

| Pfad | Antwort |
|------|---------|
| `GET /api/v1/auth/organizations/{orgId}/certifications/status-overview` | `PageDto<CertificationOverviewRowDto>`; Parameter `page`, `size`, `search` (Vor-/Nachname, Token-Suche), `sort` (`name`, `expiryDate` je `asc`/`desc`; ohne `sort` Nachname, Vorname, Nachweistyp; ungültiger Key → 400; Sortierung in-memory), `status` = `ALL` \| `MISSING` \| `EXPIRED` \| `EXPIRING_SOON` \| `VALID` (fehlend oder `ALL` = alle; ungültiger Wert → 400) |
| `GET /api/v1/auth/organizations/{orgId}/certifications/status-overview/counts` | `{ "all", "missing", "expired", "expiringSoon", "valid" }` (Zahlen; ignoriert `status` und `search`) |

Beide Endpunkte erfordern `hasCertificationReadPermission(orgId)`. Eine Zeile (`userId`, `userDisplayName`, `firstName`, `lastName`, `tenantCertDefinitionId`, `certDefinitionName`, `status`, `expiryDate`) entspricht einer aktiven Pflicht (Assignment) eines aktiven Org-Mitglieds; Status `IN_REVIEW`, Archiviertes und nicht zugewiesene Historie erscheinen nicht. Sortierung fest: Nachname, Vorname, Zertifikatsname.

**Bewusste Ausnahme von der DB-Paginierung:** Der Status wird zur Laufzeit berechnet (`CertificationStatusService.computeStatus`: wirksamer Record, Ablaufdatum, `reminderDaysBeforeExpiry`, Organisationsdatum „heute“, offene Zertifikats-Workflows). Eine SQL-Variante würde diese Logik duplizieren. Deshalb lädt der Service Mitglieder, Assignments, Records, Definitionen und offene `CERTIFICATION_RECORD`-Workflows der Organisation gebündelt (konstante Zahl Abfragen, kein N+1), berechnet den Status mit derselben Logik wie `getStatusForUser` und filtert, sucht und paginiert anschließend im Speicher. Das gilt nur für diese Liste; die Organisationsgröße begrenzt den Aufwand.

## Org-Nachweise löschen (absichtlich unterschiedlich)

| | Lehrgang | Zertifizierung |
|---|----------|----------------|
| Nachweis entfernen | `DELETE …/training-courses/records/{recordId}` | kein Record-Delete |
| Pflicht entfernen | - (keine Assignments) | `DELETE …/certifications/assignments/{assignmentId}` |

Submission = ganzer Lehrgang-Nachweis; granted types sind per PUT nicht änderbar. Cert-Records sind Verlauf (Renewal, Events) - Pflicht und Status über Assignment, nicht durch Löschen einzelner Records.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Training-Katalog / Submissions | `trainingcourse/` |
| Cert-Definitionen / Assignments / Records | `certification/` |
| Status | `certification/…/CertificationStatusService` |
| Reminder | `certification/…/CertificationReminderService` + Scheduler |
| Event-Grants | `certification/event/` (`CertificationEventGrant`, `CertificationEventGrantService`, `EventAttendanceCertificationGrantListener`) |
| Event-Delete → Grants | `certification/…/EventDeletionCertificationGrantCleanupListener` |
| Katalog-Hard-Delete → Qual | `qualification/…/TenantCatalogDefinitionDeletedQualificationCleanupListener` |
| Standard-Selections | `*/standard/` in jeweiligem Package |
| Workflow-Handler | `workflow.trainingcourse`, `workflow.certificationrecord` |
