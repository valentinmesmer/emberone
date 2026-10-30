# Organisation & Mitgliedschaft

Design-Notizen für Org-Hierarchie und Mitgliedschaftsmodell - zentrale Facade `Organization` und Hub hinter vielen Membership-/Permission-Pfaden. Packages: `com.emberone.organization`, `organizationgroup`, `userorganizationmembership`, `userorganizationgroupmembership`.

## Mental Model

```
Tenant
 └── Organisationsbaum (parentOrganization → children)
      ├── type: OrganizationTypeDefinition  →  StandardAssignment (Vererbung)
      ├── standardId?: per-Org-Standard-Override
      │
      └── OrganizationGroups (aus Standard + ad-hoc CUSTOM)
           ├── LEADERSHIP   ──maps──► PermissionGroup ADMIN
           ├── ALL_MEMBERS  ──maps──► PermissionGroup STANDARD  (UI verborgen)
           └── CUSTOM
                │
                └── UserOrganizationGroupMembership (LEADER | DEPUTY_LEADER | MEMBER)
                         │
                         └── PermissionTypes via PermissionGroupMapping
                              (gegated durch aktive UserOrganizationMembership)
```

**Zwei Mitgliedschaftsebenen:**

1. **Org-Mitgliedschaft** (`UserOrganizationMembership`) - „User gehört zu dieser Org“ (Datumsbereich + optional `main`). Voraussetzung für Permissions und ALL_MEMBERS-Auto-Sync.
2. **Org-Gruppenmitgliedschaft** (`UserOrganizationGroupMembership`) - „User ist in dieser Gruppe.“ **Das** gewährt Permissions via Mappings.

`GroupMembershipType` (`LEADER` / `DEPUTY_LEADER` / `MEMBER`) ist **organisatorische Rolle für UX / Führungs-Checks**, kein ACL-Gate. Alle aktiven Gruppenmitglieder bekommen dieselben Permissions aus den gemappten Permission Groups.

**Eltern-Hierarchie kaskadiert keine Permissions.** Mitgliedschaft in Parent-Org gewährt keine Child-Org-Permissions.

## Kernentitäten

### `Organization` (`organizations`)

| Feld | Rolle |
|------|-------|
| `tenantId` | Mandanten-Scope |
| `organizationCode` | Pro Mandant eindeutig |
| `name` | Anzeige |
| `type` → `OrganizationTypeDefinition` | Pflicht, mandantendefinierte Art |
| `parentOrganization` | Optional; `null` = Root |
| `standardId` | Optionales **per-Org-Standard-Override**; `null` = `StandardAssignment` des Typs erben |
| `deactivatedAt` | Soft-Deaktivierung; `null` = aktiv |

Hierarchie ist Baum. Roots: `findByTenantIdAndParentOrganizationIsNull`. Create/Update via `OrganizationService` - Create publiziert `OrganizationCreatedEvent`; Sync-Listener materialisiert den effektiven Standard in derselben Transaktion. **Hard-Delete von Organisationen ist nicht angeboten**; Tenant-Hard-Delete nur als bestätigter Mandanten-Purge.

### Deaktivierung und Verschieben (Admin)

- **Verschieben:** `POST /api/v1/auth/admin/tenants/{tenantId}/organizations/{organizationId}/move` mit `newParentOrganizationId` (`null` = Wurzel). Nur der Parent-Pointer der gewählten Org ändert sich; Unterbaum bleibt erhalten. Deaktivierte Orgs sind kein gültiges Ziel.
- **Deaktivieren:** `POST …/deactivate` mit Bestätigung des **Organisationsnamens** (zweistufige UI). Tenant-Admin (`canAccessTenant`). Deaktiviert die gewählte Org und **alle Unterorganisationen**. Aktive Mitgliedschaften in diesem Set: **Hauptorg** → `UserDeactivationService.deactivateUser` (Retention ab `users.deactivated_at`); sonst Membership beenden (Leadership-Guard umgangen). Org-Zeilen bleiben mit `deactivated_at` stehen; Personendaten folgen dem Löschkonzept.
- **Hinweis Betrieb:** Unterorgs, die erhalten bleiben sollen, müssen vorher verschoben werden; Hauptorgen betroffener Mitglieder müssen vorher geändert werden - die App prüft das nicht automatisch.

### `OrganizationTypeDefinition`

Mandantendefinierte Art (`name`, optional `configKey`, `active`). Standards werden Typen via `StandardAssignment` zugewiesen, nicht auf der Type-Entity gespeichert.

### `OrganizationGroup` (`organization_groups`)

| Feld | Rolle |
|------|-------|
| `organization` | Besitzende Org |
| `type` | `OrganizationGroupType` (Default `CUSTOM`) |
| `configKey` | Link zum Standard-Template; `null` ad-hoc |
| `standardOriginId` | Template-ID; bei Archiv clear |
| `archived` / `archivedAt` | Soft-Retire wenn Template entfernt, aber Mitgliedschaftshistorie ([Lifecycle & Statusfelder](lifecycle-and-status-fields.md)) |

### `OrganizationGroupType`

| Wert | Hinweise |
|------|----------|
| `LEADERSHIP` | System, Pflicht; kann Workflows genehmigen |
| `ALL_MEMBERS` | System, Pflicht; in UI **verborgen**; auto-sync aus Org-Mitgliedschaft |
| `CUSTOM` | Template oder ad-hoc |

Helfer: `isSystem()` <=> nicht `CUSTOM`; `canApproveWorkflows()` <=> nicht `ALL_MEMBERS` (also **CUSTOM** und `LEADERSHIP` dürfen Workflows genehmigen); `isVisibleInUserInterface()` verbirgt `ALL_MEMBERS`. Jeder Standard definiert genau ein `LEADERSHIP` und ein `ALL_MEMBERS`.

## Mitgliedschaft

Details, Events und Fallstricke: [Org- und Gruppen-Mitgliedschaften](user-memberships.md). Hier nur das für die Permission-Kaskade Nötige.

### Org-Mitgliedschaft (`user_organization_memberships`)

| Feld | Rolle |
|------|-------|
| `user`, `organization` | Paar |
| `main` | Hauptorg des Users |
| `startDate` / `endDate` | Aktiv wenn `startDate ≤ heute AND (endDate IS NULL OR endDate ≥ heute)` |
| `endProcessedAt` | Seiteneffekte angewandt; null = ausstehend (Cron oder sofortiges Ende) |

**Kein** `ACTIVE`/`DEACTIVATED`-Enum - „beendet“ heißt `endDate` gesetzt. `createMembership` nimmt kein `endDate` (`CreateUserOrganizationMembershipRequest` hat das Feld nicht; Zeile startet immer offen). Org-REST `updateMembership` ändert nur `startDate` (kein `endDate`); Beenden im Produkt nur per Workflow Typ 8/13 (Approve → `OrganizationMembershipExitService.applyApprovedExit`: Hauptorg → `UserDeactivationService.deactivateUser`; Nebenorg → `endMembership(now)`, siehe [Mitgliedschaften](user-memberships.md)) - **kein geplanter Org-Austritt** über Workflow oder Org-API. Der Service `endMembership` kann intern ein `endDate` setzen: liegt es in der Vergangenheit, `OrganizationMembershipEndedEvent` inkl. Gruppen-Cascade in derselben Transaktion; liegt es in der Zukunft, nur Datum + `OrganizationMembershipUpdatedEvent`, Ended-Event holt der minütliche `MembershipEndCleanupScheduler` nach (`endDate` verstrichen, `endProcessedAt` leer). Gruppen-Mitgliedschaften können `endDate` zusätzlich per `updateMembership` setzen (siehe [user-memberships](user-memberships.md#gruppen-membership-userorganizationgroupmembership)).

Events: `OrganizationMembershipCreatedEvent` / `Updated` / `Ended` → auch `UserPermissionsChangedEvent`.

### Org-Gruppenmitgliedschaft (`user_organization_group_memberships`)

Gleiche Datums-Aktiv-Semantik, plus `membershipType`: `LEADER` | `DEPUTY_LEADER` | `MEMBER`.

**System-Gruppen-Regeln:**

- Manuelles Create/Update von `ALL_MEMBERS`- oder `LEADERSHIP`-Mitgliedschaften auf normalen APIs blockiert.
- Admin/Leadership-Pfade nutzen `createSystemManagedMembership` / `updateSystemManagedMembership` (nicht der ALL_MEMBERS-Auto-Sync).
- **ALL_MEMBERS-Auto-Sync:** sync `@EventListener` `AllMembersGroupMembershipListener` - `onOrganizationMembershipCreated` / `onUserCreated` → `AllMembersGroupService.ensureUserInAllMembersGroup` (User als `MEMBER` in `ALL_MEMBERS`). Kein After-Commit-Listener.
- `UserService.ensureOrganizationMembershipAndAllMembersGroup` (Onboarding-Approve, Join-/Invite-Direktaufnahme, Admin-Leadership) ruft nur `UserOrganizationMembershipService.createMembership` auf; ALL_MEMBERS folgt über den Listener auf `OrganizationMembershipCreatedEvent` (sync in derselben Transaktion).
- Org-Mitgliedschaft end → `OrganizationMembershipEndedGroupListener` → `UserOrganizationGroupMembershipService.endAllActiveMembershipsForUserInOrganization` (alle aktiven Gruppenmitgliedschaften des Users in der Org beenden).
- **Leadership-Mitglieder können die Org nicht verlassen** und nicht per Leave-Request aus der Leitungsgruppe austreten. Org-Membership-Update und -Ende sind serverseitig blockiert, bis Tenant-Admin die Leitungsmitgliedschaft beendet. Details: [Org- und Gruppen-Mitgliedschaften](user-memberships.md#leadership-blockiert-org-ende-und-austritt).

## Permission-Kaskade

```
UserOrganizationGroupMembership (heute aktiv)
  ∩ UserOrganizationMembership (in dieser Org aktiv)
  → PermissionGroupMapping → PermissionGroup.permissionTypes
  → Map<organizationId, List<PermissionType>>
```

Gebaut von `CachedUserPermissionLoader` (Redis `USER_PERMISSIONS`), request-scoped via `RequestScopedUserPermissions` / `UserPermissionService`.

`OrganizationAccessService.hasOrganizationPermission`:

1. `GLOBAL_ADMIN` → Org muss existieren
2. sonst Org muss zum JWT-Mandanten gehören
3. `TENANT_ADMIN` → true für jede Org im Mandanten
4. sonst Map-Lookup für `PermissionType` unter `organizationId`

`SecurityService.isActiveOrganizationMember` ist **Mitgliedschafts**-Gate (Cache `ACTIVE_ORG_MEMBERSHIP`), keine Permission.

System-Baseline-Mappings: `LEADERSHIP` → `ADMIN`, `ALL_MEMBERS` → `STANDARD`. Details in [Sicherheit & Berechtigungen](security-and-permissions.md).

## Controller: Self / Organization / Admin

| Zielgruppe | Beispiel | Base-Pfad | Gate |
|------------|----------|-----------|------|
| **Self** | `OrganizationSelfController` | `/api/v1/self/organizations` | `isSelfServiceUser()`; Detail braucht aktive Mitgliedschaft; Picker `GET …/joinable`, `GET …/main-change-targets`, `GET …/leave-targets` (ohne Leadership-Orgs), `GET …/group-leave-targets` (ohne Leadership/ALL_MEMBERS), `GET …/group-join-targets` (ohne Leadership/ALL_MEMBERS und ohne bestehende Mitgliedschaft) |
| **Org-scoped** | Gruppen- / Membership-Controller, `UserMembershipOverviewController` | `/api/v1/auth/organizations/...` | `@PreAuthorize` auf Org-Permissions; Overview `GET …/users/{userId}/membership-overview` (`USER_READ`) |
| **Admin** | `AdminOrganizationController` | `/api/v1/auth/admin/tenants/{tenantId}/organizations` | `canAccessTenant` - Baum, CRUD, Führung |

Kein allgemeines Org-CRUD unter `/auth/organizations` - Struktur-Admin unter Admin. Self (`/api/v1/self/organizations`) listet die **eigenen** Mitgliedschaften und Picker; Mitglieder-Listen der Org liegen unter Admin (`…/admin/tenants/{tenantId}/organizations/{organizationId}/users`) bzw. Org-scoped Membership-/User-APIs.

`OrganizationUserMembershipInterceptor` erzwingt, dass Pfad-`userId` zu Pfad-`organizationId` bei verschachtelten User-Routen gehört.

## Bezug zum Standard

Bei Org-Create / Reassign läuft `StandardMaterializationService` `StandardDomain`-Plugins in Reihenfolge (Org-Create: Sync-Listener auf `OrganizationCreatedEvent`):

1. `OrganizationGroupStandardDomain` → `organization_groups` (`configKey` + `standardOriginId`)
2. `PermissionGroupStandardDomain` → Permission Groups + Mappings
3. (+ Workflows, Katalog-Selektionen, …)

Reassign upserted Templates und entfernt obsolete standard-getriebene Gruppen (Hard-Delete ohne Mitgliedschaftshistorie; sonst Archiv + Memberships beenden). Vor Hard-Delete/Archiv publiziert die Org-Group-Domain `OrganizationGroupDeletingEvent` (sync), damit Permission-Mappings ohne Rückabhängigkeit aufgeräumt werden. Org-Gruppen sind die **Runtime-Oberfläche** der Org-Group-Templates des Standards. Siehe [Standard](standard.md).

## Invarianten & Fallstricke

1. Permissions **nur** aus Org-Gruppenmitgliedschaften - Org-Mitgliedschaft allein gewährt nichts außer ALL_MEMBERS-Auto-Sync.
2. `LEADER` / `DEPUTY_LEADER` / `MEMBER` ändern **nicht** Permission-Auflösung.
3. Parent → Child Org-Mitgliedschaft **erbt keine** Permissions.
4. `ALL_MEMBERS` in UI unsichtbar, trägt aber meist `STANDARD`-Permissions.
5. Org-Mitgliedschaft beenden muss Gruppenmitgliedschaften in der Org beenden (Listener).
6. Aktive Leitungsgruppenmitgliedschaft blockiert Org-Update, Org-Ende und Leave-Requests; nur Tenant-Admin kann Leadership-Memberships beenden.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Org-Entity / Service | `organization/domain/Organization.java`, `organization/service/OrganizationService.java` |
| Org-Create → Materialize | `organization/service/OrganizationCreatedMaterializationListener.java` |
| Org-Group Deleting → Mappings | `organizationgroup/event/OrganizationGroupDeletingEvent.java`, `permissiongroupmapping/…/OrganizationGroupDeletingMappingCleanupListener.java` |
| Org-Gruppen | `organizationgroup/domain/*`, `organizationgroup/service/OrganizationGroupService.java` |
| Org-Mitgliedschaft | `userorganizationmembership/domain/*`, `.../service/UserOrganizationMembershipService.java` |
| Gruppenmitgliedschaft | `userorganizationgroupmembership/domain/*`, `.../service/UserOrganizationGroupMembershipService.java` |
| ALL_MEMBERS-Sync | `organizationgroup/service/AllMembersGroupService.java` (+ `AllMembersGroupMembershipListener`) |
| Membership-End → Groups | `userorganizationgroupmembership/.../OrganizationMembershipEndedGroupListener.java` |
| Standard-Domain | `organizationgroup/standard/OrganizationGroupStandardDomain.java` |
| Access-Checks | `platform/security/OrganizationAccessService.java`, `SecurityService.java` |
| Admin | `admin/organization/service/AdminOrganizationService.java` |

Siehe auch: [Org- und Gruppen-Mitgliedschaften](user-memberships.md), [Sicherheit & Berechtigungen](security-and-permissions.md), [Standard](standard.md), [Dienstgrad und Funktion](rank-and-function.md) (Funktionen aus Gruppenmitgliedschaft).

## Verwaltung über Audience pro Organisation

Eine Organisation mit verwaltender Fähigkeit kann eine Audience pflegen (`org_management_audiences`). Der Abschnitt „Verwaltung“ im Admin-Org-Fenster erscheint nur dann. Der Eintrag gewährt keinen Zugriff; das Modell und die Regeln stehen in [management.md](management.md).

Die Fähigkeit (`managementCapable` im Admin-Org-Detail) wird für die ganze Liste mit einer Abfrage vorgefiltert (Organisationen mit direkter `MANAGEMENT_ACCESS`-Zuordnung), danach erst pro Kandidat geprüft. Damit entstehen keine Abfragen je Organisation im Baum.

Ob eine Organisation **Ziel** einer Verwaltung ist, hängt nicht an dieser Organisation selbst, sondern an ihrer Eigenschaft als aktive Organisation des Mandanten (Auswahl und Aktivierung lehnen inaktive Ziele ab). Wird eine Organisation deaktiviert, verliert sie damit als Ziel sofort ihre Abdeckung, ohne dass Einträge gelöscht werden.

Zugriff auf Ziele läuft ausschließlich über den Choke-Point `OrganizationAccessService.hasOrganizationPermission`, siehe [management.md](management.md#zugriffsauswertung).
