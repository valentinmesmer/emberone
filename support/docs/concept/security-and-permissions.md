# Sicherheit & Berechtigungen

Design-Notizen für `SecurityService` und das Berechtigungsmodell. Package roots: `com.emberone.platform.security`, `permission`, `permissiongroup`, `permissiongroupmapping`, `userpermission`. Eng verwandt: [Token & Mandantenkontext](token-and-tenant-context.md), [Organisation & Mitgliedschaft](organization.md), [Permission Groups](permission-groups.md), [User Permissions](userpermission.md).

## Mental Model

Autorisierung hat **zwei Achsen**:

| Achse | Quelle | Verwendet für |
|-------|--------|---------------|
| **App-Rolle** (`app_role` JWT-Claim) | Authentik → JWT | Plattform-/Mandanten-Admin-UI & Admin-APIs (`GLOBAL_ADMIN` / `TENANT_ADMIN` / `USER`) |
| **Org-Berechtigung** (`PermissionType`) | DB via Org-Gruppe → Permission-Group-Mappings | Feature-APIs unter `/api/v1/auth/organizations/{id}/...` |

Org-Berechtigungen werden **nicht** im JWT getragen. Sie werden aus der DB als `Map<organizationId, List<PermissionType>>` geladen. JWT-Claims für Auth: nur `userId`, `tenantId`, `app_role` (siehe [Token & Mandantenkontext](token-and-tenant-context.md)).

`SecurityService` ist die **dünne SpEL-Fassade** (`@PreAuthorize("@securityService....")`). Fachlogik liegt in fokussierten Access-Services:

```
@PreAuthorize("@securityService.hasX(#organizationId)")
        │
        ▼
SecurityService  (Fassade, 8 Kollaboratoren)
   ├── TokenService                currentUserId für Instanz-Gates
   ├── AdminAccessService          App-Rolle, Tenant-Scope, Self-Service-Eligibility
   ├── OrganizationAccessService   Org-Permission / Membership
   ├── WorkflowOpenAccess          Self-Service-Workflow starten?
   ├── WorkflowInstanceAccess      Instanz: view/claim/decide/…
   ├── OnboardingSubmissionWorkflowOpenAccess
   │                               Einladung → ONBOARDING_SUBMISSION öffnen?
   ├── OrganizationMembershipEndRequestWorkflowOpenAccess
   │                               Org USER_WRITE → Austritts-Workflow öffnen?
   └── OrganizationUserMainOrganizationChangeWorkflowOpenAccess
                                   Org USER_WRITE → Hauptorg-Wechsel öffnen?
```

## Was eine Berechtigung ist

`PermissionType` - Enum mit stabiler numerischer `id` und `Module`. JSON nutzt `@JsonValue` auf `getId()` (numerisch, nicht Name).

| Modul | Enum-Konstanten (id) |
|-------|----------------------|
| **CORE** | `ORGANIZATION_READ`(1), `ORGANIZATION_GROUP_READ`(2), `USER_READ`(3), `USER_WRITE`(4), `EMERGENCY_CONTACT_READ/WRITE`(13/14), `ABSENCE_READ/WRITE`(15/16), `DOCUMENT_READ/WRITE`(17/18), `TIME_ENTRY_READ/WRITE`(19/20), `WORKFLOW_READ`(21), `CERTIFICATION_READ/WRITE`(22/23), `TRAINING_COURSE_READ/WRITE`(24/25) (Lehrgangsnachweise der Mitglieder, nicht Lehrgangsangebote), `QUALIFICATION_READ/WRITE`(26/27), `DRIVING_LICENSE_READ/WRITE`(28/29), `PROFILE_IMAGE_READ`(30), `RANK_READ`(31), `RANK_WRITE`(32) |
| **SERVICE** | `SERVICE_EVENT_READ/WRITE`(5/6) |
| **OPERATIONS** | `OPERATION_EVENT_READ/WRITE`(7/8) |
| **APPOINTMENTS** | `APPOINTMENT_READ/WRITE`(9/10), `EVENT_ATTENDANCE_READ/WRITE`(11/12) |
| **COURSE_OFFERINGS** | `COURSE_OFFERING_READ/WRITE`(33/34); die Anmeldungen eines Mitglieds in der Mitgliederansicht dagegen über `TRAINING_COURSE_READ` |
| **MANAGEMENT** | `MANAGEMENT_ACCESS`(35); Fähigkeit, andere Organisationen des Mandanten zu verwalten. Nie im ADMIN-Wildcard, siehe unten |

Die IDs sind lückenlos (1-35); Backend-Enum und Frontend-`PermissionTypeEnum` führen dieselben Werte.

**Ausnahme ADMIN-Wildcard:** `PermissionGroupType.ADMIN.effectivePermissionTypes` liefert alle `PermissionType`-Werte **außer** `MANAGEMENT_ACCESS`. Die Verwaltungsfähigkeit muss immer ausdrücklich über eine PermissionGroup vergeben werden. Die Ausnahme liegt an dieser einen Stelle; Materialisieren, Kopieren, Instanz-Sync und Baselines laufen alle darüber. Die Baseline vergibt `MANAGEMENT_ACCESS` nicht.

### Permission Groups

`PermissionGroupType`: `ADMIN` | `STANDARD` | `CUSTOM`.

- `ADMIN.effectivePermissionTypes` → **alle** `PermissionType`-Werte.
- `STANDARD` / `CUSTOM` mit expliziten Listen.
- `PermissionGroupMapping` verknüpft `OrganizationGroup` → `PermissionGroup` (oft `standardManaged`).

System-Baseline: `LEADERSHIP` → `ADMIN`, `ALL_MEMBERS` → `STANDARD`.

## Wie `SecurityService` prüft

Bean-Name: `"securityService"` (SpEL: `@securityService....`). Alle öffentlichen Methoden delegieren.

### Kernpfade

| Methodenfamilie | Delegiert an | Verhalten |
|-----------------|--------------|-----------|
| `isGlobalAdmin` / `isTenantAdmin` / `isAdminUser` / `canAccessTenant` / `canListTenants` / `isSelfServiceUser` | `AdminAccessService` | Exakter Match auf JWT `app_role`; Tenant-Scope; Self = JWT-`userId` vorhanden und nicht `PLATFORM_ADMIN_USER_ID` (`0L`) |
| `hasOrganizationPermission(orgId, type)` und Domain-Wrapper | `OrganizationAccessService` | GLOBAL: Org existiert; TENANT_ADMIN: Org im JWT-Mandanten; USER: direkte Rechte aus `RequestScopedUserPermissions`, danach Management-Rechte aus `ManagementAccessService.rightsIn(orgId)` (Choke-Point, siehe [Management](management.md)) |
| `isActiveOrganizationMember(orgId)` | `OrganizationAccessService` | Mitgliedschafts-Gate (keine Permission) |
| `canView` / `canDecide` / `canClaim` / … | `WorkflowInstanceAccess` + `tokenService.getUserId()` | Instanzregeln, fail-closed |
| `canBeginSelfServiceWorkflow(typeId)` | `WorkflowOpenAccess` | Handler + enabled Definition + optionale Required Permission (`GLOBAL_ADMIN` early-return; sonst `hasOrganizationPermission`, das auch `TENANT_ADMIN` durchlässt) |
| `canSubmitOnboardingInvitation(invitationId)` | `OnboardingSubmissionWorkflowOpenAccess` | eigene offene Einladung (Tenant-Match) + aktivierte `ONBOARDING_SUBMISSION`-Definition der Ziel-Org |
| `canOpenOrganizationMembershipEndWorkflow(orgId, targetUserId)` | `OrganizationMembershipEndRequestWorkflowOpenAccess` | Org `USER_WRITE` + Zielmitglied: Austritts-Workflow (Typ 13) öffnen |
| `canOpenOrganizationUserMainOrganizationChangeWorkflow(orgId, targetUserId)` | `OrganizationUserMainOrganizationChangeWorkflowOpenAccess` | Org `USER_WRITE` + Zielmitglied: Hauptorg-Wechsel (Typ 14) öffnen |


### Auflösung für normale User

1. `CachedUserPermissionLoader.load(tenantId, userId)` - aktive Org-Gruppenmitgliedschaften ∩ aktive Org-Mitgliedschaften → gemappte Permission-Typen keyed by Org.
2. Liest **nicht** `GroupMembershipType` (Leader vs. Member).
3. **Keine** User-Direct-Permission-Zeilen - Grants nur via Group-Mappings.
4. Gecacht in Redis (`userPermissions`, Key `{tenantId}:{userId}`); request-scoped via `RequestScopedUserPermissions`.


Mitgliedschafts-/Mapping-Änderungen publizieren `UserPermissionsChangedEvent` / `OrganizationGroupPermissionsChangedEvent` → Cache-Eviction (+ Realtime an Clients).

## Controller-Muster: Self / Organization / Admin

Dieses Naming wiederholt sich über die Security-/Controller-Fläche und mappt auf URL-Prefixes + Gate-Stil:

| Muster | Pfad-Prefix | Gate | Wirkt auf |
|--------|-------------|------|-----------|
| **Self** | `/api/v1/self/**` | Klassen-Level `isSelfServiceUser()` | Aktuellen User aus Token |
| **Organization** | `/api/v1/auth/organizations/{organizationId}/...` | Methoden-Level `hasXPermission(#organizationId)` | Org-scoped Ressourcen |
| **Admin** | `/api/v1/auth/admin/...` | `isAdminUser()` / `canAccessTenant(#tenantId)` / `isGlobalAdmin()` | Mandanten-/Plattform-Admin |

Self- und Auth-Pfade brauchen gültiges User-JWT (`EmberOneUserTokenClaims`: bekannter `app_role` und (`GLOBAL_ADMIN` oder vorhandene `tenantId`)).

Verschachtelte Org-User-Routen (`.../organizations/{org}/users/{user}/...`) treffen auch `OrganizationUserMembershipInterceptor` - Pfad-User muss zur Pfad-Org gehören.

Workflow-Instanz-Ops unter `/api/v1/auth/workflows` nutzen Instanzmethoden (`canView(#id)`, …), nicht allein Org-Permission-Wrapper.

### Wie `@PreAuthorize` die Bean erreicht

`@EnableMethodSecurity` + SpEL `@securityService.<method>(...)`. Pfad- / `#request`-Args binden an Methodenparameter.

## Frontend-Hälfte

Die UI spiegelt dieses Modell:

- JWT `app_role` → Admin-Chrome (`canAccessAdministration`)
- `GET /api/v1/self/permissions` → Org-Permission-Map für `usePermissions()`

Siehe [Frontend Auth & Berechtigungen](frontend-auth.md).

## Invarianten & Fallstricke

1. JWT autorisiert **keine** Org-Features - nur App-Rolle + DB-abgeleitete Permissions.
2. Org-Mitgliedschaft ohne Gruppenmitgliedschaft ⇒ keine Permissions (außer ALL_MEMBERS-Auto-Sync).
3. `GLOBAL_ADMIN` / `TENANT_ADMIN` bypassen die Permission-Map für Org-Checks vollständig.
4. Neues `PermissionType` braucht Backend-Enum **und** Frontend `PermissionTypeEnum` mit gleicher numerischer ID (manueller Vertrag).
5. Workflow-Instanz-Zugriff ist separates Regelwerk (`WorkflowInstanceAccess`) - `WORKFLOW_READ` allein reicht nicht zum Entscheiden. Instanz-Gates respektieren Handler-Flags (`allowsInputChangeRequests`, `allowsOpenerFreeInputEdit`, …); Details: [Workflow-Engine](workflow-engine.md).
6. Neue AuthZ-Logik gehört in den passenden Access-Service, nicht in die Fassade `SecurityService`.
7. Filter-Kette authentifizierter Requests: `RequestIdFilter` → Bearer → `TenantContextWebFilter` → `DeactivatedUserWebFilter` (siehe [Token & Mandantenkontext](token-and-tenant-context.md)).

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| SpEL-Fassade | `platform/security/SecurityService.java` |
| App-Rolle / Tenant / Self | `platform/security/AdminAccessService.java` |
| Org-Permission-Check | `platform/security/OrganizationAccessService.java` |
| Self-Service-Workflow starten | `platform/security/WorkflowOpenAccess.java` |
| Workflow-Instanz-Regeln | `platform/security/WorkflowInstanceAccess.java` |
| Onboarding-Einladung öffnen | `workflow/onboardingsubmission/OnboardingSubmissionWorkflowOpenAccess.java` |
| Austritts-Workflow öffnen | `workflow/organizationmembershipend/OrganizationMembershipEndRequestWorkflowOpenAccess.java` |
| Hauptorg-Wechsel öffnen | `workflow/organizationusermainorganizationchange/OrganizationUserMainOrganizationChangeWorkflowOpenAccess.java` |
| Request-Id / Deaktivierung | `RequestIdFilter`, `DeactivatedUserWebFilter` |
| Permission-Enum | `permission/domain/PermissionType.java` |
| Permission Groups | `permissiongroup/domain/*` (`PermissionGroupType` in `permission.domain`) |
| Permission-Group-Mappings | `permissiongroupmapping/domain/*` |
| Loader / Cache | `userpermission/cache/CachedUserPermissionLoader.java` |
| Request-Scope | `platform/security/RequestScopedUserPermissions.java` |

Siehe auch: [Token & Mandantenkontext](token-and-tenant-context.md), [Organisation & Mitgliedschaft](organization.md), [Dienstgrad und Funktion](rank-and-function.md) (`RANK_READ`/`RANK_WRITE`).
