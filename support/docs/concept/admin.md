# Admin-Modul

Design-Notizen für Tenant-/Plattform-Administration als API-Fassade. Package root: `com.emberone.admin`. Kein eigenes Domänenmodell außer `Tenant` - delegiert an `organization`, `standard`, `user`/`identity`, Workflow-, Zertifizierungs-, Qualifikations- und Training-Module. Verwandt: [Standard](standard.md), [Organisation](organization.md), [Sicherheit](security-and-permissions.md), [Identity Provider](identity-provider-authentik.md).

## Mental Model

| Konzept | Rolle |
|---------|-------|
| **Tenant** | Mandanten-Stammdaten (Locale, Bundesland, …); Isolationsgrenze |
| **Admin-API** | `/api/v1/auth/admin/...` - GLOBAL- oder TENANT_ADMIN-gated |
| **Bootstrap** | Tenant-Create → System-Standard-Baseline sync in neuer TX mit RLS-Bind |

```
Admin Controllers
        │
        ▼
Admin*Service ──► Organization / Standard / User / Catalog / Workflow
        │
        ▼
Tenant + Domänen-Packages (Besitzer der Kernlogik)
```

## Was es besitzt

- `Tenant`-Entity, Locale-Cache (`TenantLocaleCache`), Federal-State-Cache (`TenantFederalStateCache`)
- Admin-Controller/Services/DTOs für: Tenants, Org-Tree/Leadership, Org-Typen, Org-User-Liste, Standards (clone/content/modules/mappings/assignments/overrides + Impact), Users (App-Rollen via Authentik), tenant-scoped Kataloge (Certification-/Qualification-/Training-Course-/Driving-License-/Onboarding-Document-Definitionen), Workflow-Type-Katalog & -Definitionen, Retention-Profil-Ansicht, Tenant-Purge

**Controller-Prefixes (Auswahl):**

| Controller | Prefix |
|------------|--------|
| `AdminTenantController` | `/api/v1/auth/admin/tenants` (`GET /` paginiert mit Suche über Name und Kürzel, Sortierung per `sort` (`name`, je `,asc|,desc`; Standard `name,asc`); optionaler Filter `federalState` wiederholbar, OR; ungültig → 400; UI `/admin/tenants`: Menubar-Filter Bundesland wie `/admin/users`; `POST /` Create, `GET /federal-states`, `POST …/{tenantId}/purge`: nur `GLOBAL_ADMIN`) |
| `AdminUserController` | `GET /api/v1/auth/admin/users/list-filter-options` - dieselbe `@PreAuthorize` wie Benutzerliste (`isAdminUser()`); `AdminUserListFilterOptions` (`ListFilterOptionsSource`) |
| `AdminTenantController` | `GET /api/v1/auth/admin/tenants/list-filter-options` - dieselbe `@PreAuthorize` wie Mandantenliste (`canListTenants()`); `AdminTenantListFilterOptions` |
| `OrganizationListFilterOptionsController` | (Übergang) `/api/v1/auth/organizations/{organizationId}/list-filter-options` - wird pro Org-Liste ersetzt |
| `AdminRetentionController` | `/api/v1/auth/admin/tenants/{tenantId}/retention` (nur Lesen: aktives Profil + Fristen) |
| `AdminOrganizationController` | `.../tenants/{tenantId}/organizations` (u. a. `GET /{organizationId}/users` → aktive Org-Mitglieder via `UserService.getUsersByOrganizationId`; `GET /{organizationId}/children` → `@BoundedList`, einfache Liste der direkten Kind-Organisationen, Sortierung Name; Initial-Member nur über `POST …/leader-user`, kein Admin-Create weiterer Mitglieder) |
| `AdminOnboardingInvitationController` | `.../tenants/{tenantId}/organizations/{organizationId}/…`: optionale Eintritts-Erfassung für bestehende Members (nach Leiter-Anlage; in der Leitungsgruppen-Mitgliedschaft als Kachel → eigene Wizard-Seite), Abschluss ohne Workflow |
| `AdminTenantOnboardingDocumentController` | `.../tenants/{tenantId}/onboarding-documents` |
| `AdminOrganizationOnboardingDocumentController` | `.../tenants/{tenantId}/organizations/{organizationId}/onboarding-documents` |
| `AdminStandardController` | `.../tenants/{tenantId}/standards` (`GET /assignments` und `GET /organization-overrides` sind `@BoundedList`-Konfigurationslisten und liefern eine einfache Liste ohne `PageDto`) |
| `AdminTenantUserController` | `.../tenants/{tenantId}/users/{userId}`: `PATCH /app-role`, `POST /deactivate` (Schreibzugriffe mandantengebunden per Pfad; Nutzer eines anderen Mandanten → 404) |
| `AdminManagementActivationController` | `/api/v1/auth/admin/management-activations` (`GET` paginiert, Cross-Tenant-Lesen für `GLOBAL_ADMIN`, `GET /list-filter-options`); Beenden: `AdminTenantManagementActivationController` unter `.../tenants/{tenantId}/management-activations/{id}/end`. Siehe [Management](management.md) |
| `AdminUserController` | `/api/v1/auth/admin/users` (`GET`: paginierte Benutzerliste per `ListQuery` → `PageDto<TenantUserListItemDto>`, Suche über Vor-/Nachname und Benutzername, Sortierung per `sort` (`lastName` = Nachname dann Vorname, `firstName`, je `,asc|,desc`; Standard `lastName,asc`); optionale Filter `tenantId` und `organizationId` (jeweils wiederholbar, OR innerhalb der Dimension; nur `GLOBAL_ADMIN` für fremde Mandanten, `TENANT_ADMIN` mit fremdem Mandanten → 403), `organizationId` (aktive Mitgliedschaft in mindestens einer der gewählten Organisationen) und `appRole` (wiederholbar, OR; `USER`/`TENANT_ADMIN`/`GLOBAL_ADMIN`, ungültig → 400); UI `/admin/users`: `sheetLayout: 'menubar'` - Filter-Icon rechts neben der Suche blendet Mandant, Organisation und Rolle als kompakte Buttons mit Popover-Mehrfachauswahl ein (Referenz: [Listen-Filtermuster](frontend-component-library.md#listen-steuerung-listcontrols)); `GLOBAL_ADMIN` sieht alle Mandanten, `TENANT_ADMIN` nur den eigenen; App-Rolle; `GET /{userId}` → einzelner aktiver Benutzer als `TenantUserListItemDto` (Detailansicht, kein Paging-Scan; pro `userId` genau eine Mandanten-Zugehörigkeit; unbekannt/deaktiviert → 404, `TENANT_ADMIN` mit fremdem Mandanten → 403, Nicht-Admin → 403) - kein Org-Kontext) |
| Katalog-/Workflow-Admin | `.../tenants/{tenantId}/…`: `certification-definitions`, `qualification-definitions`, `training-course-types`, `driving-license-definitions`, `onboarding-documents`, `workflow-definitions`, `workflow-types` |

## Was es nicht besitzt

| Thema | Woanders |
|-------|----------|
| Org-/Gruppen-Kernlogik, Materialisierung | `organization`, `standard` |
| User-Entity / Provisioning-Details | `user`, `identity` |
| Workflow-Engine, Permission-Berechnung | `workflow`, `userpermission` / Access-Services |
| Self-/Org-Feature-APIs | jeweilige Domänen-Packages |

## Invarianten & Fallstricke

1. Tenant-Create / Federal-States-Liste / Tenant-Purge: nur `GLOBAL_ADMIN` (`isGlobalAdmin()`). Übrige Tenant-Admin-APIs: `canAccessTenant` / `isAdminUser`.
2. Tenant-Create: Row commit → dann `TenantStandardsBootstrapService` in `REQUIRES_NEW` mit Tenant-Bind.
3. `AdminStandardController` liegt unter `admin.standard.controller`, nicht unter `standard/`.
4. Leadership-Memberships nutzen `createSystemManagedMembership` (umgeht ALL_MEMBERS-Manual-Guard). Beenden = `updateSystemManagedMembership` mit `endDate` (kein Hard-Delete). Admin-Leadership-Ende ist der vorgesehene Weg, jemanden aus der Leitungsgruppe zu nehmen, bevor Org-Bearbeitung, Org-Ende oder Leave-Requests möglich sind (Backend blockiert Leadership weiterhin; User-Deaktivierung / Org-Deaktivierung können Leadership mitbeenden). Org-Hard-Delete ist nicht angeboten; Mandanten-Hard-Delete nur als bestätigter Purge (siehe [Aufbewahrung und Löschung](retention-and-erasure.md#tenant-löschung)).
5. App-Rollen-Zuweisung prüft Rank via `EmberOneAppRole.actorMayAssignRole`.

**Retention / Purge:** Löschklassen und Fristen unter `/admin/tenants/{id}/retention`, Entitäten je Klasse unter `/admin/tenants/{id}/retention/{category}` (UI) bzw. `GET …/retention` (API). Tenant-Purge: `POST …/purge` mit Bestätigung des **Mandantennamens** (zweistufige UI, nur `GLOBAL_ADMIN`); entfernt Mandantendaten, Dateien (Outbox) und IdP-Anmeldekonten des Mandanten. Org-Deaktivierung und Verschieben auf der Org-Detailseite (`canAccessTenant`). Details: [Aufbewahrung und Löschung](retention-and-erasure.md), [Organisation](organization.md).

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Tenant | `admin/tenant/{domain,controller,service,…}/` |
| Tenant-Purge | `admin/tenant/service/AdminTenantPurgeService.java` |
| Retention-Ansicht | `admin/retention/` |
| Org-Admin | `admin/organization/{controller,service,dto}/` |
| Standards-Admin | `admin/standard/{controller,service,dto}/` |
| User-Admin | `admin/user/{controller,service,dto}/` |
| Kataloge / Workflow-Admin | `admin/{certification,qualification,trainingcourse,drivinglicense,onboardingdocument,workflowdefinition,workflowtypecatalog}/controller/` |
| Onboarding-Erfassung | `admin/onboarding/` |
