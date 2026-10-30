# 12. Glossar

Begriffsklärungen für leicht verwechselbare Domänenkonzepte. Ausführliche Modelle: [`support/docs/concept/`](../concept/00-index.md).

## Häufige Verwechslungen

### Standard vs. Module vs. WorkflowType vs. Qualification

| Begriff | Was es ist | Was es nicht ist |
|---------|------------|------------------|
| **Standard** | Benannter Organisations-Setup-Blueprint (Gruppen, Permission Groups, Workflow-Mappings, Katalog-Teilmengen). Wird einer Org (über Org-Typ bzw. Override) zugewiesen und materialisiert. | Kein Feature-Flag und keine Antragsart |
| **Module** | Feature-Bereich (`CORE`, `SERVICE`, `OPERATIONS`, `APPOINTMENTS`, `COURSE_OFFERINGS`, `MANAGEMENT`). Toggle **pro Standard** (Disabled-Rows); steuert u. a. sichtbare `PermissionType`s. | Kein Setup-Blueprint; kein Katalogeintrag |
| **WorkflowType** | Festes Enum der Antragsarten (IDs und Handler: [Workflow-Engine](../concept/workflow-engine.md)): u. a. `NAME_CHANGE` (1) … `ORGANIZATION_LEAVE_REQUEST` (8), `ORGANIZATION_GROUP_LEAVE_REQUEST` (9), `ORGANIZATION_GROUP_JOIN_REQUEST` (10), `ONBOARDING_SUBMISSION` (11), `ORGANIZATION_MEMBERSHIP_END_REQUEST` (13), `ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE` (14), `USER_DOCUMENT_SUBMISSION` (15). ID 12 ist entfernt. Standard mappt Typ → Genehmiger-Org-Gruppe. Absences sind direktes CRUD, kein Workflow. | Kein Laufzeit-Antrag (`Workflow`-Instanz) |
| **Qualification** | Zusammengesetzte Definition: Voraussetzungen aus Lehrgangs- und Zertifizierungs-`config_key`s (+ optional Org-Entscheidung). Status wird berechnet. | Kein einzelner Cert-/Training-Record; kein Standard |

Siehe: [Standard](../concept/standard.md), [Module-Config](../concept/module-and-standard-module-config.md), [Workflow-Engine](../concept/workflow-engine.md), [Qualifikation](../concept/qualification.md).

### Organization vs. OrganizationGroup

| Begriff | Was es ist |
|---------|------------|
| **Organization** | Organisationseinheit im Mandanten-Baum (Hierarchie, Typ, optionaler Standard-Override). Träger von Mitgliedschaften und Org-APIs. |
| **OrganizationGroup** | Gruppe *innerhalb* einer Organisation (z. B. `LEADERSHIP`, `ALL_MEMBERS`, Custom). Träger von Permission-Mappings und Workflow-Genehmiger-Rollen; Mitgliedschaftsebenen `LEADER` / `DEPUTY_LEADER` / `MEMBER`. |

Siehe: [Organisation](../concept/organization.md).

### Todo vs. Workflow vs. Event

| Begriff | Was es ist | Was es nicht ist |
|---------|------------|------------------|
| **Todo** | Persistierte Aufgaben-Inbox-Zeile (wer, Link zur Quelle; kein Due-/Remind-Feld). Gespiegelt aus Workflow-Slots oder Zertifizierungs-Remindern (externer Scheduler). | Kein eigener Business-Prozess; kein Kalender-Termin |
| **Workflow** | Laufzeit-Instanz eines Genehmigungsprozesses (`WorkflowType`). Besitzt Teilnehmer, Slots, Entscheidungen, Dokument-Promotion. | Keine Inbox-Zeile; kein Dienst/Einsatz |
| **Event** | Ist-Betrieb: Dienst (`ServiceEvent`) oder Einsatz (`OperationEvent`), inkl. Attendance. Promotion aus **Appointment** gibt es nur für `ServiceEvent`. | Kein geplanter Termin (das ist Appointment); kein Todo |

Verwandt: **Appointment** = geplanter Termin inkl. RSVP/Serie; orthogonal zu Todo/Workflow.

Siehe: [Todo](../concept/todo.md), [Workflow-Engine](../concept/workflow-engine.md), [Appointment](../concept/appointment.md), [Event](../concept/event.md).

## Weitere Begriffe

| Begriff | Definition |
|---------|------------|
| **Appointment** | Geplanter Termin (Einzel oder Serien-Occurrence) mit Audience und RSVP. Kann zu einem Service-Event promotet werden und wird dann archiviert. |
| **AppointmentSeries** | RRULE-Serie; Occurrences werden materialisiert und periodisch verlängert. |
| **PermissionType** | Enum einer Org-Feature-Berechtigung (numerische `id` + zugehöriges `Module`). Wirksam über OrgGroup → PermissionGroup-Mappings, nicht über JWT-Claims. |
| **PermissionGroup** | Org-scoped Bündelung von `PermissionType`s (`ADMIN` / `STANDARD` / `CUSTOM`). Oft aus Standard-Templates materialisiert. |
| **Tenant / Mandant** | Isolationsgrenze für Daten (Postgres RLS / Session-Kontext). JWT trägt `tenantId` (`EmberOneJwtClaims.TENANT_ID`); Postgres-GUCs sind `app.tenant_id` / `app.user_id`. Admin-Pfade können den Holder-Mandanten überschreiben. |
| **App-Rolle** | JWT-Claim `app_role`: `GLOBAL_ADMIN` / `TENANT_ADMIN` / `USER`. Plattform-/Mandanten-Admin-Achse, getrennt von Org-Permissions. |
| **Effective Standard** | Der für eine Org wirksame Standard: Org-Override (`Organization.standardId`) oder Zuweisung am Org-Typ. |
| **config_key** | Stabiler Schlüssel. Standard-Templates: System = Enum-Name, Custom = `custom:slug`. Mandanten-Kataloge (Lehrgang, Nachweis, Führerschein, Qualifikation, Eintrittsdokument): `config_key` ist das Kürzel/die Kennung. |
| **EntityAttachment** | Temporärer/entity-gebundener Dateianhang (Workflow-Draft, Workflow, Comment, Appointment). Nicht das permanente User-Dokumentenarchiv. |
| **UserDocument** | Permanentes persönliches Dokument (eigener Bucket); Ziel der Workflow-Dokument-Promotion. |
| **TrainingCourse (Submission)** | User-Nachweis einer Lehrgangsart; ein Submission kann mehrere granted types tragen. Keine Assignments/Reminder. |
| **Certification** | Katalog-Definition plus **Assignment** (Pflicht) und **Record** (Nachweis); Status berechnet (`MISSING`, `IN_REVIEW`, `EXPIRED`, `EXPIRING_SOON`, `VALID`, `ARCHIVED`). Reminder erzeugen Todos. |
| **Authentik** | Derzeit genutzter externer Identity Provider (OIDC); User-Provisioning und JWT-Ausstellung. Provisioning-SPI vorhanden; derzeit nur Authentik verdrahtet. |
| **Access-Service** | Fokussierte AuthZ-Klasse hinter der SpEL-Fassade `SecurityService`. Muster: Logik in Access-Services, Fassade nur Delegation. |
| **AdminAccessService** | App-Rollen (`GLOBAL_ADMIN` / `TENANT_ADMIN`), Tenant-Scoping und Self-Service-Eligibility (`isSelfServiceUser`). |
| **OrganizationAccessService** | Org-Permission-Checks und aktive Mitgliedschaft; Admin-Rollen bypassen die Permission-Map. |
| **WorkflowOpenAccess** | Darf ein Self-Service-Workflow-Typ gestartet werden? (Handler, enabled Definition, optional Required Permission). Typ-spezifische Collaborators: `OnboardingSubmissionWorkflowOpenAccess`, `OrganizationMembershipEndRequestWorkflowOpenAccess`, `OrganizationUserMainOrganizationChangeWorkflowOpenAccess`. |
| **WorkflowInstanceAccess** | Per-Workflow-Instanz: view / claim / decide / cancel / comment / payload / promotions; fail-closed. |
