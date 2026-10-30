# Onboarding (Organisationsstandards)

Design-Notizen für konfigurierbare Onboarding-Schritte in Organisationsstandards. Package root: `com.emberone.onboarding`. Verwandt: [Standard](standard.md), [Workflow-Engine](workflow-engine.md) (Typ 11).

## Mental Model

Onboarding ist die **Aufnahme-Checkliste** eines Organisationsstandards: konfigurierte Steps → Einladung → Drafts → Freigabe → `promote` in User-Tabellen. Membership entsteht erst bei Direktaufnahme oder Approve (bzw. besteht schon bei Admin-Erfassung).

| Konzept | Rolle |
|---------|-------|
| **Standard-Steps** | Geordnete Step-Typen am CUSTOM-Standard (`std_onboarding_steps`); kein `StandardDomain`-SPI |
| **Einladung** | `onb_invitations` pro User+Ziel-Org; Status `OPEN` / `SUBMITTED` / `APPROVED` |
| **Aufnahme-Entscheidung** | `recordInvitationForAdmission` / `areAllStepsLiveFulfilled`: offene Steps → `OPEN` ohne Membership; sonst Direktaufnahme (`APPROVED` + Membership) |
| **Einreichung** | Wizard öffnet `ONBOARDING_SUBMISSION` (Typ 11); Approve → Membership + `promote` |
| **Step-Handler** | `OnboardingStepHandler`: `validateConfig`, `isFulfilled` (live), `isOnboardingFulfilled` (Draft), `promote` |

```
Admin-Steps am Standard
        │
Org-Invite / Join-Approve ──► recordInvitationForAdmission
        │                         ├─ Steps offen → OPEN (keine Membership)
        │                         └─ live erfüllt / keine Steps → APPROVED + Membership
        ▼
Drafts (Wizard) ──submit──► ONBOARDING_SUBMISSION ──approve──► Membership + promote
Admin-Erfassung (Leiter) ──complete──► promote + APPROVED (ohne Workflow)
```

Abgrenzung: Katalog-Selections (Nachweise, Lehrgänge, …) filtern, welche Mandanten-Katalogeinträge eine Org sieht. Onboarding-Steps definieren separat, was ein neues Mitglied durchlaufen soll.

## Scope

- **Org-Invite:** `POST /api/v1/auth/users/organizations/{organizationId}` nutzt dieselbe Aufnahme-Entscheidung wie der Join-Workflow
- **Eintritts-Workflow:** `ORGANIZATION_JOIN_REQUEST` Approve → `recordInvitationForAdmission` für die Ziel-Org; Open blockiert bei pending Eintritt zur Ziel-Org
- **Org-/Self-Listen:** Org `GET …/onboarding-invitations` (`OPEN`, `SUBMITTED`); Self `GET …/self/onboarding-invitations` (alle Status inkl. `APPROVED`); My-Area `/my-area/onboarding`
- **Kein Admin-Mitglieder-Create:** Weitere Mitglieder nur über Org-Invite bzw. Approve der Einreichung; Admin legt nur den Initial-Member an (`leader-user` → `createInitialTenantMember`)
- **Admin-Erfassung:** Nach `leader-user` optional Eintritt erfassen; Leitungsgruppen-Detail: Eintritt-Kachel („nachreichen“ / Status / Review). Abschluss = `recordCompletedOnboarding` → `promote` + `APPROVED` ohne Workflow und ohne erneute Membership. Einladungs-Hard-Delete nicht angeboten. Wizard: `…/onboarding/[invitationId]?membershipId=…`. `invited_by_user_id` über `getReferencedUserId()` (Plattform-Admin → `NULL`, siehe [User-Löschung: Fremdschlüssel](user-delete-foreign-keys.md)).

## Lifecycle (Einladung)

| Ereignis | Status | Daten |
|----------|--------|-------|
| Org-Invite / Join-Workflow (Aufnahme mit Onboarding) | `OPEN` | Drafts leer oder editierbar |
| Join-Workflow (Direktaufnahme) | `APPROVED` | leere Einladung; Membership + `ALL_MEMBERS` sofort |
| Wizard Complete → Workflow open | `SUBMITTED` | Drafts unverändert |
| Workflow Approve | `APPROVED` | Org-Mitgliedschaft + `ALL_MEMBERS` + `promote` aller Steps |
| Admin `recordCompletedOnboarding` | `APPROVED` | nur `promote` (Membership besteht bereits, z. B. Leiter); kein Workflow |
| Workflow Reject / Cancel | `OPEN` | Drafts bleiben; Cancel beendet den Workflow terminal → erneutes Einreichen = neuer Workflow. Reject mappt auf Input-Change (kein terminales `REJECTED`) |
| Workflow Input-Change (via Reject) | `OPEN` | Drafts wieder editierbar; Complete über denselben Submit-Endpunkt erfüllt den Change-Request |
| Input-Change zurückgezogen / erfüllt | `SUBMITTED` | wieder read-only |

`completionEnabled` = `status == OPEN && allStepsFulfilled`. `saveStep` nur bei `OPEN`. Self-Edit und Org-Pending-Liste gelten nicht für Einladungen, deren User in der Ziel-Org schon aktives Mitglied ist (Admin-Erfassung).

Workflow-Subject: `"ONBOARDING_INVITATION"` + `invitationId`. Concurrency-Key: `org:{targetOrganizationId}`. Start nur aus dem Wizard (`listedInBeginnableCatalog() = false`). Pre-Open-Security: `canSubmitOnboardingInvitation(invitationId)` prüft eigene offene Einladung und aktivierte Workflow-Definition der Ziel-Org. Tenant-Baseline (inkl. neue Workflow-Typen) wird zentral per `TenantBaselineSyncFilter` vor `/api/v1/self` und `/api/v1/auth` synchronisiert.

## Mandanten-Katalog Eintrittsdokumente

Mandanten-Eintrittsdokumente folgen demselben Muster wie Lehrgangstypen, Nachweise und Führerscheinklassen: `config_key` **ist** die Kennung. Rename zieht Onboarding-Step-Referenzen mit. Die UI zeigt die Kennung, nicht ein abgeleitetes `custom:…`.

Pro Definition: `fulfillment_mode` (`CONFIRMED` | `SIGNED_UPLOAD`), `mandatory` (Default `true`), Katalog-Lifecycle wie andere Mandanten-Kataloge.

### Vorlagen-Versionen (immutable)

Die Zeile in `onb_document_definitions` / `onb_org_document_definitions` trägt nur die **aktuelle** Versionsnummer (`version`, `0` = keine Vorlage). Jeder Upload legt eine unveränderliche Zeile an:

| Tabelle | Scope |
|---------|--------|
| `onb_document_definition_template_versions` | Mandant |
| `onb_org_document_definition_template_versions` | Organisation |

Spalten je Version: `version`, `storage_key` (`attachments`-Bucket), `sha256_hex`, Dateiname, Content-Type, Größe, `uploaded_by_user_id`, `uploaded_at`. Austausch = neue Version + höhere Nummer; alte Datei bleibt, solange ein Nachweis (`usr_entry_document_fulfillments` oder `usr_entry_document_fulfillment_events`) noch deren Hash referenziert.

### Nachweis (rechtssicher)

Ziel: später belegbar **was** (Version + SHA-256), **wann**, **wer**, **wie** (Kanal).

| Speicher | Rolle |
|----------|--------|
| `usr_entry_document_fulfillments` | Aktueller Stand pro User + `config_key` (+ optional `organization_id`); UNIQUE je Scope |
| `usr_entry_document_fulfillment_events` | Append-only Historie (FORCE RLS); jede Aktion eine Zeile |

Fulfillment-Spalten (Auszug): `template_version`, `template_sha256_hex`, `outcome` (`ACCEPTED` \| `DECLINED` \| `WITHDRAWN`), `mode`, optional `user_document_id`, `confirmed_at` (nur bei `ACCEPTED`).

Event-Spalten (Auszug): dieselbe Version/Hash, `confirmation_text` (Wortlaut bei Bestätigung), `outcome`, `channel` (`SELF` \| `ADMIN_RECORDED`), `confirmed_at` (Serverzeit der Aktion), `actor_user_id`, optional `admin_record_reason`, optional `workflow_reviewer_user_id` + `workflow_reviewed_at` (signierter Upload nach Workflow-Approve).

Aufbewahrung der Nachweise: Löschklasse `USER_MASTER` (persönliche Akte), **nicht** `ONBOARDING` - siehe [Aufbewahrung und Löschung](retention-and-erasure.md).

### Erfüllung und Pflicht

- **Live (`isFulfilled`):** nur **Pflicht**-Dokumente müssen für die **aktuelle** Katalog-Version `ACCEPTED` sein (Tenant: `organization_id IS NULL`; Org: passende `organization_id`). Freiwillige blockieren den Step nicht.
- **Wizard (`isOnboardingFulfilled`):** Pflicht-Docs müssen im Draft erfüllt sein (Version, Modus); freiwillige Docs blockieren nicht (Ablehnung im Draft optional über `declined`).
- **Wiederverwendung:** erfüllte Pflicht-Docs aus der User-Tabelle zählen live; abgelehnte freiwillige (`DECLINED`, aktuelle Version) zählen für den Step ebenfalls als erledigt.
- **Neue Vorlage:** akzeptierte/ abgelehnte Stände gelten nur für die damalige `template_version`; nach Upload muss erneut gehandelt werden.

Erfüllungsmodi: `CONFIRMED` (Bestätigung nach Öffnen der Vorlage) oder `SIGNED_UPLOAD` (Vorlage öffnen/herunterladen, signiert hochladen; Workflow-Approve setzt Prüfer-Zeit auf den Nachweis).

**Wizard / Draft (serverseitig):** Vor Bestätigung oder Upload muss die Vorlage geöffnet worden sein (`openedAt` im Step-Payload, gesetzt beim Template-Download). Bei `CONFIRMED` sind `confirmationText` (UI nennt Name, Version, Upload-Datum) und `confirmedAt` Pflicht. Beim `promote` werden Draft-Version und Template-Hash gegen die Katalog-Version geprüft; Nachweis-Events entstehen in `UserEntryDocumentFulfillmentService`.

**Admin-Erfassung:** `POST …/complete` verlangt `adminRecordReason`; Promotion schreibt Kanal `ADMIN_RECORDED` und Begründung auf die Events.

**Nach dem Eintritt:** Zustimmung oder Ablehnung freiwilliger Dokumente erfolgt nur im Eintritts-Wizard und ist danach unverändert (kein Widerruf, keine nachträgliche Self-Ablehnung). **Nachweis-Export:** `GET /api/v1/self/entry-documents/proof-export` (PDF mit aktuellem Stand und Event-Historie).

## Organisations-Eintrittsdokumente

Organisationsspezifische Definitionen in `onb_org_document_definitions` (FORCE RLS, pro `organization_id`). Verwaltung im **Admin** unter Mandant → Organisation (`canAccessTenant`), nur wenn der effektive Standard der Organisation den Step `ORGANIZATION_ENTRY_DOCUMENTS` enthält. Der Step hat leere Config `{}`; zur Laufzeit werden alle aktiven Org-Definitionen mit Vorlage der Ziel-Org aufgelöst. Hat die Org keine aktiven Docs, gilt der Step als live erfüllt und erscheint nicht im Wizard.

Admin-API Org-Docs: `…/organizations/{organizationId}/onboarding-documents` (CRUD, Template, archive/unarchive). Tenant-Katalog: `…/tenants/{tenantId}/onboarding-documents` (gleiche Fläche ohne `organizationId`).
Self/Org-Uploads Org-Docs: `…/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/{configKey}/…` (Tenant-Docs: flache `…/documents/{configKey}/…`).

## Step-Typen

| Typ | Config | `isFulfilled` (Skip) | `isOnboardingFulfilled` (Draft) |
|-----|--------|----------------------|---------------------------------|
| `ENTRY_DOCUMENTS` | `{ documentConfigKeys: string[] }` | alle **Pflicht**-Docs in **User-Tabelle** `ACCEPTED` mit **aktueller** Katalog-Version | alle **Pflicht**-Docs im Draft erfüllt; freiwillige blockieren nicht |
| `ORGANIZATION_ENTRY_DOCUMENTS` | `{}` | keine aktiven Org-Docs **oder** alle **Pflicht**-Org-Docs live `ACCEPTED` (aktuelle Version) | wie `ENTRY_DOCUMENTS`, Keys aus `onb_org_document_definitions` |
| `EMERGENCY_CONTACTS` | `{ minimumCount: number }` (≥ 1) | `count(user contacts) >= minimumCount` | `count(draft contacts) >= minimumCount` |
| `TRAINING_COURSES` | `{}` | andere aktive Haupt-Org-Mitgliedschaft (`main=true`, Org ≠ Ziel-Org) | mind. ein Draft-`record` **oder** `noneDeclared: true` |
| `CERTIFICATIONS` | `{}` | andere aktive Haupt-Org-Mitgliedschaft | mind. ein Draft-`record` **oder** `noneDeclared: true` |
| `DRIVING_LICENSE` | `{}` | andere aktive Haupt-Org-Mitgliedschaft | Draft-`license` gesetzt **oder** `noneDeclared: true` |
| `RANK` | `{}` | andere aktive Haupt-Org-Mitgliedschaft **oder** bereits offener `UserRank` | `tenantRankId` gesetzt **oder** `noneDeclared: true`; optional Anhang (`…/steps/RANK/attachment`) |

Pro Standard maximal **ein Step je Typ**.

## `promote` (bei Workflow-Approve oder Admin-Erfassung)

`OnboardingStepHandler.promote(ctx, config, payload)` schreibt Drafts in die bestehenden User-Domain-Services (`*FromOnboarding`, analog `*FromWorkflow`). Regeln:

- `isFulfilled` (live) → no-op
- `noneDeclared` → no-op
- Attachments: Kopie aus `attachments-bucket` (`…/onboarding-invitations/{id}/…`) → User-Dokument-Bucket (auch Gesundheitsnachweise und andere Zertifikatsnachweise). Titel und Tags kommen aus denselben `workflow.documentPromotion.*`-Funktionen wie bei Workflow-Approve (`apply*Metadata` / Dokumentname bei Eintrittsdokumenten), keine Roh-Keys wie `training-course`.
- Nach erfolgreichem `promoteAllSteps`: Draft-Dateien im `attachments`-Bucket für diese Einladung löschen (Prozess-Kopien). `onb_invitation_step_data` bleibt für Checkliste, Zeitstempel und Versionen (Audit) und wird während aktiver Mitgliedschaft für die Eintritt-UI gehalten. Löschklasse `ONBOARDING` entfernt die `APPROVED`-Einladung erst **6 Monate nach Austritt** (`users.deactivated_at`); offene Einladungen werden beim Austritt sofort gelöscht. Eintrittsdokumente und promotete Nachweise liegen in der persönlichen Akte und folgen deren Retention.

| Step | Ziel |
|------|------|
| `EMERGENCY_CONTACTS` | `EmergencyContactService` |
| `DRIVING_LICENSE` | `DrivingLicenseRecordService` + Attachment → `UserDocument` |
| `CERTIFICATIONS` | Assignment falls nötig, dann `CertificationRecordService` |
| `TRAINING_COURSES` | `TrainingCourseRecordService` |
| `ENTRY_DOCUMENTS` | `usr_entry_document_fulfillments` (`organization_id` null) + Uploads als `UserDocument` |
| `ORGANIZATION_ENTRY_DOCUMENTS` | `usr_entry_document_fulfillments` (mit `organization_id`) + Uploads als `UserDocument` |
| `RANK` | `UserRankService.assignFromOnboarding` (nur Erstvergabe); optional Anhang → User-Dokument |

## Architektur

```
OnboardingStepHandler (SPI)
  ├── EntryDocumentsStepHandler
  ├── OrganizationEntryDocumentsStepHandler
  ├── EmergencyContactsStepHandler
  ├── TrainingCoursesStepHandler
  ├── CertificationsStepHandler
  ├── DrivingLicenseStepHandler
  └── RankStepHandler

OnboardingStepHandlerRegistry  →  Map<OnboardingStepType, Handler>
OnboardingPromotionService     →  promote je konfiguriertem Step
```

- Config wird als JSONB am Standard-Step gespeichert; Handler parsen/validieren typisierte Records (`OnboardingStepConfig` sealed interface).
- Draft-Antworten liegen pro Einladung und Step in einer Payload-Zeile. Promotion in User-Tabellen bei Workflow-Approve oder Admin `recordCompletedOnboarding`.
- Readiness: `OnboardingInvitationReadinessService` für Draft-Erfüllung bei Submit; `areAllStepsLiveFulfilled` für Join-Direktaufnahme.
- Beim **Klonen** eines Standards werden Steps mitkopiert (`StandardOnboardingStepService.copySteps`).
- Beim **Baseline-Sync** werden ggf. Eintrittsdokument-Steps aus dem Tenant-Baseline befüllt (`replaceEntryDocumentsStep`). Zusätzliche System-Steps nur, wenn `TenantBaseline.systemOnboardingStepTypes` nicht leer ist (`ensureSystemOnboardingSteps`); Bayern-Baseline: leer (kein voreingestellter `RANK`-Step).
- Archivieren/Löschen eines Mandanten-Eintrittsdokuments entfernt den `config_key` aus allen `ENTRY_DOCUMENTS`-Steps.
- User- und Einladungs-Hard-Delete sind nicht angeboten.

## Auflösung Einladung → Steps

```
OnboardingInvitation.targetOrganizationId
  → EffectiveStandardResolver.resolveForOrganization(org)
  → StandardOnboardingSteps ORDER BY sort_order
```

Kataloge (Lehrgänge/Nachweise/Führerscheinklassen) kommen über die bestehenden Catalog-Services der Ziel-Org, nicht über Org-Mitgliedschaft (Invitees haben keine).

## Wizard-Verhalten

Detail-DTO: `editable`, `allStepsFulfilled`, `completionEnabled` (`OPEN` + alle Steps erfüllt), `alreadyFulfilledStepTypes` (konfigurierte Steps ohne Angaben in dieser Einladung, live bereits erfüllt).

**`APPROVED` ist eingefroren:** Detail und Review bauen Steps sowie Eintrittsdokument-Kataloge nur aus den gespeicherten Invitation-Drafts. Nachträgliche Änderungen am Organisationsstandard oder an Tenant-/Org-Eintrittsdokumenten erzeugen keine neuen offenen Items und kein `fulfilled: false`. `OPEN` und `SUBMITTED` bleiben live (Wizard, Readiness, Workflow-Approve).

| Situation | Verhalten |
|-----------|-----------|
| Step bereits live erfüllt (`isFulfilled`) und ohne Angaben in dieser Einladung | im Wizard, in der read-only Detailansicht und im Approve-Workflow aus der Step-Liste ausgeblendet; zählt als erledigt. Die betroffenen Step-Typen stehen in `alreadyFulfilledStepTypes` und werden dort als Liste angezeigt |
| Step in dieser Einladung erfasst (Draft-Payload), danach per Approve live erfüllt | bleibt in der Detailansicht und im Workflow sichtbar |
| Lehrgänge, Nachweise, Führerschein | entfallen bei anderer aktiver Haupt-Org-Mitgliedschaft |
| Notfallkontakte | entfallen, wenn die Live-Anzahl die Mindestanzahl erreicht |
| Eintrittsdokumente | entfallen, wenn alle konfigurierten Docs in der User-Tabelle mit aktueller Version erfüllt sind; teilweise Erfüllung zeigt nur die fehlenden Docs (`catalog.documents[].alreadyFulfilled`); in dieser Einladung hochgeladene/bestätigte Docs bleiben nach Freigabe in der Eintritt-Detailansicht sichtbar (Metadaten aus Draft-Payload). **Anschauen/Download bei `APPROVED`:** signierte Uploads über `catalog.*.promotedUserDocumentId` → persönliche User-Dokumente; Vorlagen weiter über Einladungs-Template-API; gelöschte Draft-Keys in S3 ohne Promote-Dokument → kein View-Button |
| Leere Drafts ohne `noneDeclared` | gelten nicht als erfüllt |
| `SUBMITTED` | read-only Detail, Katalog/Erfüllung weiter live |
| `APPROVED` | read-only Detail eingefroren auf Invitation-Drafts; Katalogänderungen greifen nicht nach. Checkliste/Metadaten aus gespeicherten Step-Payloads; Dateizugriff für Nachweise/Lehrgänge/Führerschein/Eintritts-Uploads über promote-Ziele (User-Dokumente, Cert-/Lehrgangs-Records), nicht über gelöschte Draft-S3-Keys. Detail-DTO: `subjectUserId`, bei Eintrittsdokumenten `promotedUserDocumentId` |
| `OPEN` + aktiver Change-Request | editierbar; Complete über denselben Submit wie die Ersteinreichung |

## Org-API (Einladungen)

Prefix: `/api/v1/auth/organizations/{organizationId}`

| Methode | Pfad | Zweck |
|---------|------|-------|
| GET | `/onboarding-available` | Ob der effektive Standard Onboarding-Steps hat (`USER_READ`) |
| GET | `/onboarding-invitations` | Offene und eingereichte Einladungen (`OPEN`, `SUBMITTED`) |
| GET | `/onboarding-invitations/{invitationId}` | Wizard-Detail read-only (`USER_READ`) |
| GET | `/onboarding-invitations/{invitationId}/documents/{configKey}/template` | Vorlage Tenant-Eintrittsdokument |
| GET | `/onboarding-invitations/{invitationId}/documents/{configKey}/file` | Hochgeladenes Tenant-Dokument |
| GET | `/onboarding-invitations/{invitationId}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/{configKey}/template` | Vorlage Org-Eintrittsdokument |
| GET | `/onboarding-invitations/{invitationId}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/{configKey}/file` | Hochgeladenes Org-Dokument |
| GET | `/onboarding-invitations/{invitationId}/steps/{stepType}/records/{recordKey}/attachment` | Nachweis- oder Lehrgangs-Dokument |
| GET | `/onboarding-invitations/{invitationId}/steps/DRIVING_LICENSE/attachment` | Führerschein-Dokument |
| GET | `/onboarding-invitations/{invitationId}/steps/RANK/attachment` | Dienstrang-Nachweis |
| GET | `/users/{userId}/onboarding-invitation` | Onboarding des Mitglieds read-only; 404 wenn keine Zeile |

## Self-API (eigene Onboardings)

Prefix: `/api/v1/self`

| Methode | Pfad | Zweck |
|---------|------|-------|
| GET | `/onboarding-invitations` | Alle eigenen Einladungen inkl. Ziel-Organisationsname und Status |
| GET | `/onboarding-invitations/{id}` | Wizard-Detail: Steps, Fulfillment, Draft-Payload, Katalog |
| PUT | `/onboarding-invitations/{id}/steps/{stepType}` | Draft des Steps speichern (`OPEN`) |
| GET | `/onboarding-invitations/{id}/documents/{configKey}/template` | Vorlage Tenant-Eintrittsdokument |
| GET | `/onboarding-invitations/{id}/documents/{configKey}/file` | Hochgeladenes Tenant-Dokument |
| POST | `/onboarding-invitations/{id}/documents/{configKey}` | Signiertes Tenant-Upload (`SIGNED_UPLOAD`) |
| GET | `/onboarding-invitations/{id}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/{configKey}/template` | Vorlage Org-Eintrittsdokument |
| GET | `/onboarding-invitations/{id}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/{configKey}/file` | Hochgeladenes Org-Dokument |
| POST | `/onboarding-invitations/{id}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/{configKey}` | Signiertes Org-Upload |
| GET | `/onboarding-invitations/{id}/steps/{stepType}/records/{recordKey}/attachment` | Nachweis- oder Lehrgangs-Dokument |
| POST | `/onboarding-invitations/{id}/steps/{stepType}/records/{recordKey}/attachment` | Nachweis- oder Lehrgangs-Dokument hochladen |
| DELETE | `/onboarding-invitations/{id}/steps/{stepType}/records/{recordKey}/attachment` | Nachweis- oder Lehrgangs-Dokument entfernen (`attachmentIndex`) |
| GET | `/onboarding-invitations/{id}/steps/DRIVING_LICENSE/attachment` | Führerschein-Dokument |
| POST | `/onboarding-invitations/{id}/steps/DRIVING_LICENSE/attachment` | Führerschein-Dokument hochladen |
| DELETE | `/onboarding-invitations/{id}/steps/DRIVING_LICENSE/attachment` | Führerschein-Dokument entfernen (`attachmentIndex`) |
| GET | `/onboarding-invitations/{id}/steps/RANK/attachment` | Dienstrang-Nachweis |
| POST | `/onboarding-invitations/{id}/steps/RANK/attachment` | Dienstrang-Nachweis hochladen |
| DELETE | `/onboarding-invitations/{id}/steps/RANK/attachment` | Dienstrang-Nachweis entfernen (`attachmentIndex`) |
| GET | `/entry-documents/proof-export` | PDF-Export aller Eintrittsdokument-Nachweise (aktueller Stand + Event-Historie) |

Workflow-Submit (nicht unter `/self`): `POST /api/v1/auth/workflows/onboarding-submission/{invitationId}` öffnet die Einreichung oder erfüllt einen offenen Change-Request.

## Admin-API

Prefix: `/api/v1/auth/admin/tenants/{tenantId}/standards`

| Methode | Pfad | Zweck |
|---------|------|-------|
| GET | `/onboarding-step-types` | Verfügbare Step-Typen |
| GET | `/{id}/onboarding-steps` | Konfigurierte Steps |
| PUT | `/{id}/onboarding-steps` | Gesamte Liste ersetzen |

Prefix: `/api/v1/auth/admin/tenants/{tenantId}/organizations/{organizationId}`

| Methode | Pfad | Zweck |
|---------|------|-------|
| GET | `/onboarding-available` | Ob der effektive Standard Onboarding-Steps hat |
| GET | `/users/{userId}/onboarding-recording-available` | Ob Admin-Erfassung für dieses Mitglied möglich ist (aktives Org-Mitglied, Steps, keine Einladung) |
| GET | `/users/{userId}/onboarding-invitation` | Eintritt des Mitglieds (beliebiger Status); 404 wenn keine Zeile |
| POST | `/users/{userId}/onboarding-invitations` | Admin-Erfassung starten (`OPEN`, User muss aktives Org-Mitglied sein) |
| GET | `/onboarding-invitations/{invitationId}` | Wizard-Detail |
| PUT | `/onboarding-invitations/{invitationId}/steps/{stepType}` | Draft speichern |
| POST | `/onboarding-invitations/{invitationId}/complete` | `recordCompletedOnboarding` (Body: `adminRecordReason` Pflicht; promote + `APPROVED`, Kanal `ADMIN_RECORDED`) |
| GET | `/onboarding-invitations/{invitationId}/documents/{configKey}/template` | Vorlage Tenant-Eintrittsdokument |
| GET | `/onboarding-invitations/{invitationId}/documents/{configKey}/file` | Hochgeladenes Tenant-Dokument |
| POST | `/onboarding-invitations/{invitationId}/documents/{configKey}` | Signiertes Tenant-Upload |
| GET | `/onboarding-invitations/{invitationId}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/{configKey}/template` | Vorlage Org-Eintrittsdokument |
| GET | `/onboarding-invitations/{invitationId}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/{configKey}/file` | Hochgeladenes Org-Dokument |
| POST | `/onboarding-invitations/{invitationId}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/{configKey}` | Signiertes Org-Upload (Admin-Erfassung) |
| GET/POST/DELETE | `/onboarding-invitations/{invitationId}/steps/{stepType}/records/{recordKey}/attachment` | Nachweis- oder Lehrgangs-Dokument (analog Self) |
| GET/POST/DELETE | `/onboarding-invitations/{invitationId}/steps/DRIVING_LICENSE/attachment` | Führerschein-Dokument (analog Self) |
| GET/POST/DELETE | `/onboarding-invitations/{invitationId}/steps/RANK/attachment` | Dienstrang-Nachweis (analog Self) |

Org-API (`USER_READ`): Org-Eintrittsdokumente in der Einladung nur GET (Vorlage/Datei); Upload nur Self bzw. Admin-Erfassung. Dienstrang-/Führerschein-/Record-Attachments in der Einladung nur GET.

Prefix: `/api/v1/auth/admin/tenants/{tenantId}/onboarding-documents` (Tenant-Katalog: CRUD, Template, archive/unarchive).

Prefix: `/api/v1/auth/admin/tenants/{tenantId}/organizations/{organizationId}/onboarding-documents` (Org-Katalog: CRUD, Template, archive/unarchive).

## Frontend

- Standard-Detail: `StandardOnboardingStepManagement`
- Mandanten-Katalog: `AdminTenantOnboardingDocumentManagement`; Org-Detail: Org-Eintrittsdokumente wenn Step `ORGANIZATION_ENTRY_DOCUMENTS`
- Mitgliederverwaltung: Einladungen und Mitglieder in einer Liste; Status-Badge (`OPEN`/`SUBMITTED`/…); Hub-Section **Eintritt** (`USER_READ`)
- My-Area: `/my-area/onboarding` (`MyAreaOnboardingList`); Wizard (`MyAreaOnboardingWizard`) unter `/my-area/onboarding/[invitationId]`; Session-Hinweis `OnboardingReminderPrompt`
- Wizard: „Nicht zustimmen“ bei freiwilligen Docs (nur während des Eintritts), Bestätigungstext mit Version/Datum
- App-Link: `AppLinkType.ONBOARDING_INVITATION` → `AppNavigationResolver` → `/my-area/onboarding/{invitationId}`
- Workflow-Review (`ONBOARDING_SUBMISSION`): Input-Display-Plugin; Opener Self-API, Approver Org-API (`USER_READ`)
- Kein Eintrag im Workflow-Begin-Modal (Typ nicht im beginnbaren Katalog)
- Admin: nach „Leiter anlegen“ optional erfassen; in der Leitungsgruppen-Mitgliedschafts-Detailansicht Eintritt als Kachel (starten / Status / abgeschlossen); Wizard-Seite unter `…/organizations/[orgId]/onboarding/[invitationId]` (mit `membershipId` zurück zur Mitgliedschaft). Complete ohne Workflow. UI-Texte: „Eintritt“ / „nachreichen“, nicht „Onboarding“.

## Datenbank

| Tabelle | Zweck |
|---------|-------|
| `onb_invitations` | Onboarding-Einladung (User, Ziel-Org, Status); UNIQUE pro User+Ziel-Org |
| `onb_invitation_step_data` | Draft-Payload pro Einladung und Step-Typ |
| `std_onboarding_steps` | Step-Config am Organisationsstandard |
| `onb_document_definitions` | Mandanten-Katalog Eintrittsdokumente (`mandatory`, `fulfillment_mode`, aktuelle `version`) |
| `onb_document_definition_template_versions` | Immutable Vorlagen-Versionen (Hash, Storage-Key, Metadaten) |
| `onb_org_document_definitions` | Org-Katalog Eintrittsdokumente |
| `onb_org_document_definition_template_versions` | Immutable Org-Vorlagen-Versionen |
| `usr_entry_document_fulfillments` | Aktueller Nachweis-Stand pro User/Key/Scope (`template_version`, `template_sha256_hex`, `outcome`, `mode`, optional `user_document_id`, `confirmed_at`) |
| `usr_entry_document_fulfillment_events` | Append-only Nachweis-Historie (Text, Kanal, Akteur, Workflow-Prüfung) |

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Step-Handler SPI | `onboarding/spi/OnboardingStepHandler.java` |
| Handler-Implementierungen | `onboarding/handler/*StepHandler.java` |
| Promotion | `onboarding/service/OnboardingPromotionService.java` |
| Attachment-Promotion | `onboarding/service/OnboardingAttachmentPromotionService.java` → `userdocument/…/UserDocumentPromotionService` |
| Readiness | `onboarding/service/OnboardingInvitationReadinessService.java` |
| Admin-Abschluss ohne Workflow | `OnboardingInvitationService.recordCompletedOnboarding` |
| Einladungs-Lifecycle | `onboarding/service/OnboardingInvitationService.java` |
| Admin-Erfassung API | `admin/onboarding/controller/AdminOnboardingInvitationController.java` |
| Tenant-/Org-Eintrittsdokumente Admin | `admin/onboardingdocument/controller/AdminTenantOnboardingDocumentController.java`, `AdminOrganizationOnboardingDocumentController.java` |
| Wizard / Draft-Speichern | `onboarding/service/OnboardingInvitationProgressService.java` |
| Eintrittsdokument User-Tabelle / Events | `onboardingdocument/service/UserEntryDocumentFulfillmentService.java` |
| Vorlagen-Versionen (Tenant/Org) | `onboardingdocument/service/OnboardingDocumentTemplateService.java`, `OrganizationOnboardingDocumentTemplateService.java` |
| Draft-Validierung (Öffnen, Bestätigungstext) | `onboarding/service/OnboardingEntryDocumentDraftSupport.java` |
| Self Nachweis-Export | `onboardingdocument/controller/SelfEntryDocumentFulfillmentController.java` |
| Workflow Handler | `workflow/onboardingsubmission/OnboardingSubmissionWorkflowHandler.java` |
| Self-API Einladung | `onboarding/controller/OnboardingInvitationSelfController.java` |
| Org-API | `onboarding/controller/OnboardingInvitationOrganizationController.java` |
| Org User-Detail | `onboarding/controller/OnboardingInvitationUserOrganizationController.java` |

Siehe auch: [Workflow-Engine](workflow-engine.md) (Typ 11), [User-Mitgliedschaften](user-memberships.md) (Membership erst bei Approve), [Standard](standard.md), [Attachment & Dokument-Promotion](attachment-and-document-promotion.md), [Dienstgrad und Funktion](rank-and-function.md) (Step `RANK`).
