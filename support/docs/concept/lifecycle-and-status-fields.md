# Lifecycle- und Statusfelder

Design-Notizen für wiederkehrende Spalten und API-Felder mit einheitlicher Semantik. Package root: `com.emberone.platform.domain` (`LifecycleFields`). Eng verwandt: [Organisation & Mitgliedschaft](organization.md), [User-Mitgliedschaften](user-memberships.md), [Workflow-Engine](workflow-engine.md), [Todo](todo.md).

## Mental Model

Wiederkehrende Lifecycle-Spalten bedeuten **über Domänen hinweg dasselbe** - nicht denselben Mechanismus mit anderem Namen:

| Feld-Familie | Eine Frage |
|--------------|------------|
| `active` / `archived` (+ `archived_at`) | Ist dieser **Katalog-/Stammdateneintrag** wählbar bzw. soft-retired? |
| `enabled` | Ist dieser **Prozess-/Feature-Schalter** an? |
| `end_date` (+ optional `end_processed_at`) | Ist diese **Beziehung/Zuweisung** zeitlich noch gültig? |
| `deactivated_at` | Ist **User-Konto** oder **Organisation** soft-deaktiviert? |
| `archived_at` allein / `completed_at` / `status` | Prozesszustand der Zeile selbst (Inbox, Workflow) |

**Nicht vermischen:** Katalog-Lifecycle ≠ Feature-Schalter ≠ Gültigkeitsintervall.

## Übersicht

| Feld | Bedeutung | Typische Nutzung |
|------|-----------|------------------|
| **`active`** | Mandanten-Admin schaltet einen **Katalogeintrag** vorübergehend ab, ohne ihn zu archivieren. | Zertifizierung, Lehrgang, Qualifikation, Führerschein-Klasse, Onboarding-Dokument |
| **`archived`** + **`archived_at`** | **Soft-Retirement**: Zeile bleibt für Historie, ist aber nicht mehr wählbar; Zeitpunkt der Archivierung. | Kataloge (wenn Löschen wegen Historie blockiert), Org-Gruppen, Termine |
| **`enabled`** | **Feature-/Prozess-Schalter** (kein Katalog-Lifecycle). | `WorkflowDefinition`, Workflow-Type-Mapping-Templates, Notification-Overrides, Realtime-Config |
| **`end_date`** (+ optional **`end_processed_at`**) | **Gültigkeitsintervall** einer Beziehung oder Zuweisung (nicht „archiviert“). | Org-/Gruppen-Mitgliedschaft, Zert-Zuweisung, Qualifikations-Org-Entscheidung, Abwesenheit |
| **`deactivated_at`** | **Benutzerkonto** dauerhaft deaktiviert (Login/Provisioning). | `users` |
| **`deactivated_at`** | **Organisation** soft-deaktiviert (kein Hard-Delete); Unterbaum bei Admin-Deaktivierung mitgesetzt. | `organizations` |
| **`archived_at` allein** | Inbox-Archiv ohne separates `archived`-Flag: gesetzt = aus aktiver Inbox. | `todo_items` |
| **`completed_at`** / **`status`** | Prozesszustand der Zeile selbst (nicht Katalog). | Todos, Workflows |

## Mandanten-Katalog (`active` + `archived` + `archived_at`)

Tabellen: `tenant_cert_definitions`, `tenant_training_course_types`, `tenant_qualification_definitions`, `tenant_driving_license_definitions`, `onb_document_definitions`, `onb_org_document_definitions`.

| Zustand | `active` | `archived` | `archived_at` | Bedeutung |
|---------|----------|------------|---------------|-----------|
| Normal nutzbar | `true` | `false` | `NULL` | Auswahl in Standards, neue Zuweisungen/Workflows erlaubt (sofern Domain das prüft) |
| Admin pausiert | `false` | `false` | `NULL` | Historie bleibt; keine neuen Nutzungen (`isCatalogSelectable` = false) |
| Archiviert | `false` | `true` | gesetzt | Wie pausiert, zusätzlich aus Admin-Listen „aktiv“; Katalog-`UNIQUE (tenant_id, config_key)` gilt auch für archivierte Zeilen (kein Partial Unique) - Ausnahme: `organization_groups` (Partial Unique `WHERE … archived = false`) |
| Nach Unarchive | abhängig | `false` | `NULL` | Standard: `active = true`; Onboarding-Dokumente: `active = hasTemplate()` |

**Invarianten (DB-Check `chk_*_tenant_catalog_lifecycle`):**

- `active = true` ⇒ `archived = false`
- `archived = true` ⇒ `archived_at IS NOT NULL` und `active = false`
- `archived = false` ⇒ `archived_at IS NULL`

**Code:** Archivieren/Unarchivieren nur über `LifecycleFields.archiveCatalogEntry` / `unarchiveCatalogEntry`. Verfügbarkeit: `LifecycleFields.isCatalogSelectable(active, archived)`.

`OrganizationTypeDefinition` nutzt nur **`active`** (kein Archiv): Typen ohne Historie-Problematik werden deaktiviert statt archiviert.

## Soft-Retirement ohne `active` (`archived` + `archived_at`)

| Entität | Grund |
|---------|--------|
| `organization_groups` | Template entfernt, aber Gruppen-Mitgliedschaftshistorie bleibt |
| `appointments` | Nach Promote oder Auto-Archive (≥ 4h nach Ende); kein Admin-`active`-Toggle |

**Invariante:** `archived` und `archived_at` sind konsistent (`chk_*_retirement`).

**Code:** `LifecycleFields.archive(target, now)` setzt beide Felder.

Termine: `archived` bleibt das Filter-Flag in Queries; `archived_at` ist Audit.

## Todos (`archived_at` ohne `archived`)

- `archived_at IS NULL` ⇒ offene Inbox (Status `OPEN`)
- `archived_at` gesetzt bei Complete/Cancel ⇒ aus aktiver Inbox; nur Reopen setzt `archived_at` wieder auf `NULL`
- Kein separates `archived`-Boolean, um Doppelzustände zu vermeiden

Details: [Todo](todo.md).

## Mitgliedschaft & Zuweisung (`start_date` / `end_date`)

- **Aktiv** = `start_date <= now` und (`end_date IS NULL` oder `end_date > now`): Instant-Intervalle sind halboffen `[start, end)`, aneinandergrenzende Zeilen überlappen nicht (Org-/Gruppen-Mitgliedschaft; Termine sind ebenso halboffen)
- **Kalendertag-Intervalle** (`LocalDate`: Abwesenheit, Dienstgrad, Qualifikations-/Zertifikatsentscheidungen, Lehrgänge): `end_date` ist der letzte gültige Tag (inklusiv)
- **`end_processed_at`**: asynchrone Nacharbeit nach abgelaufenem `end_date` (Scheduler)

Details: [Org- und Gruppen-Mitgliedschaften](user-memberships.md).

## `enabled` (Feature-Schalter)

- Workflow-Typ für eine Org erlaubt (`wf_workflow_definitions.enabled`, Templates `std_workflow_type_mapping_templates.enabled`)
- Kein Ersatz für Katalog-`active`; Workflow-Services prüfen `enabled`, Katalog-Services prüfen `isCatalogSelectable`

Details: [Workflow-Engine](workflow-engine.md), [Module & Standard Module Config](module-and-standard-module-config.md).

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Hilfs-API | `platform/domain/LifecycleFields.java` |
| Katalog-Services | `Tenant*DefinitionService`, `TenantTrainingCourseTypeService`, `*OnboardingDocumentDefinitionService` |
| Org-Gruppe Archiv | `OrganizationGroupStandardDomain` |
| Termin Archiv | `AppointmentService`, `AppointmentAutoArchiveService` |
| Todo Archiv | `TodoService` (`archivedAt`) |

`user_ranks.end_date` markiert abgeschlossene Dienstgrad-Intervalle; siehe [Dienstgrad und Funktion](rank-and-function.md).
