# Permission Groups & Mapping

Design-Notizen für die Org-Berechtigungs-Datenseite (Katalog, Gruppen, Mappings). Package roots: `com.emberone.permission`, `permissiongroup`, `permissiongroupmapping` (+ Auflösung in `userpermission`). Autorisierungs-Checks: [Sicherheit & Berechtigungen](security-and-permissions.md). Materialisierung: [Standard](standard.md).

## Mental Model

```
PermissionType (Enum, numerische id + Module)
        ↑ grants
PermissionGroup (org-scoped: ADMIN | STANDARD | CUSTOM)
        ↑ PermissionGroupMapping
OrganizationGroup
        ↑ Mitgliedschaft
User → Map<organizationId, List<PermissionType>>
```

Dieses Cluster besitzt das **Datenmodell und die Ableitung** der Org-Feature-Berechtigungen - nicht die SpEL-Checks.

## Was es besitzt

| Package | Besitz |
|---------|--------|
| **`permission`** | Katalog (`PermissionType`, `PermissionGroupType`), Change-Events, Cache-Eviction, Realtime (`permissions.changed`) |
| **`permissiongroup`** | Org-Instanzen (`perm_groups` + Grant-Zeilen), Standard-Templates, SPI `PermissionGroupStandardDomain` |
| **`permissiongroupmapping`** | N:M OrgGroup → PermissionGroup (`perm_group_mappings`, Flag `standardManaged`) |
| **`userpermission`** | Effektive Map laden/cachen; Read-API nur Self `GET /api/v1/self/permissions` |

Baseline: `LEADERSHIP` → `ADMIN`, `ALL_MEMBERS` → `STANDARD`. `ADMIN.effectivePermissionTypes` expandiert zu allen Types **außer `MANAGEMENT_ACCESS`** (bei Materialisierung zusätzlich nach enabled Modules gefiltert). Details: [Sicherheit](security-and-permissions.md).

## Was es nicht besitzt

| Thema | Woanders |
|-------|----------|
| `@PreAuthorize` / Admin-Bypass | `platform.security.SecurityService` |
| JWT-App-Rolle | Token-Kontext |
| Org-/Gruppenmitgliedschaften | Organization-Domain |
| Org-Level PermissionGroup-CRUD-Controller | **fehlt** - Instanzen entstehen durch Standard-Materialisierung |
| Direct-User-Grants | existieren nicht |
| Admin-User-Permissions-Read-API | existiert nicht (nur Self) |

## Wichtige Invarianten

1. Effektive Permissions = aktive Org-Mitgliedschaft ∩ aktive Gruppenmitgliedschaft ∩ Mappings → Types (`CachedUserPermissionLoader`).
2. `GroupMembershipType` (Leader/Member) wird **nicht** gelesen - Mitgliedschaft reicht.
3. Kein separates Permissions-Table - Groups granten Types direkt (`PermissionGroupPermissionType`).
4. JSON-Vertrag: numerische `id` (`@JsonValue`) - Backend/Frontend-Enums müssen matchen.
5. Manuelle Mappings (`standardManaged=false`) überleben Template-Remove; Standard-Mappings nicht.
6. Änderungen → Events → Cache-Evict + Realtime.
7. Vor Hard-Delete/Archiv einer Org-Gruppe: `OrganizationGroupDeletingEvent` (sync) → Mapping-Cleanup (auch bei Archiv nötig; bei Row-Delete zusätzlich FK-CASCADE in V1).

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Types | `permission/domain/PermissionType.java` |
| Gruppen + Grants | `permissiongroup/domain/` |
| Mappings | `permissiongroupmapping/` |
| Org-Group-Delete → Mappings | `OrganizationGroupDeletingMappingCleanupListener` |
| Standard-SPI | `permissiongroup/standard/PermissionGroupStandardDomain.java` |
| Auflösung | `userpermission/cache/CachedUserPermissionLoader.java` |
