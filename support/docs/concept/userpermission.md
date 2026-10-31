# Effektive User-Permissions

Design-Notizen für Read-Model und Cache der Org-Permission-Map. Package root: `com.emberone.userpermission`. Verwandt: [Sicherheit](security-and-permissions.md), [Permission Groups](permission-groups.md), [Caching](caching.md), [Mitgliedschaften](user-memberships.md).

## Mental Model

| Konzept | Rolle |
|---------|-------|
| **CachedUserPermissionLoader** | `@Cacheable(USER_PERMISSIONS)`: aktive Group- ∩ Org-Memberships → Mappings → `PermissionType`s |
| **UserPermissionDto** | `Map<organizationId, List<PermissionType>>` + `tenantLocale` |
| **RequestScopedUserPermissions** | Request-Cache vor `OrganizationAccessService` (liegt in `platform.security`) |

```
Membership-Änderung          Mapping-Änderung
        │                            │
        ▼                            ▼
UserPermissionsChangedEvent   OrganizationGroupPermissionsChangedEvent
                              PermissionGroupPermissionsChangedEvent
        │                            │
        └────────────┬───────────────┘
                     ▼
        PermissionCacheEvictionListener
                     │
                     ▼
CachedUserPermissionLoader → Self API → Frontend usePermissions()
```

| Änderung | Event | Eviction |
|----------|-------|----------|
| Org-/Gruppen-Mitgliedschaft | `UserPermissionsChangedEvent` | `PermissionCacheEvictionListener` → dieser User |
| Org-Gruppe ↔ Permission-Group-Mapping | `OrganizationGroupPermissionsChangedEvent` | aktive Mitglieder der Org-Gruppe |
| Permission-Group-Inhalt | `PermissionGroupPermissionsChangedEvent` | aktive Mitglieder gemappter Org-Gruppen |

## Was es besitzt

- Loader + `UserPermissionService` (Locale via `TenantLocaleService`)
- Self: `GET /api/v1/self/permissions`

## Was es nicht besitzt

| Thema | Woanders |
|-------|----------|
| `PermissionType`-Enum | `permission` |
| PermissionGroups / Mappings | `permissiongroup` |
| Granting / Schreib-APIs | Group-Mappings, Admin-Standards |
| Cache-Eviction-Listener | `permission/cache/PermissionCacheEvictionListener` |
| AuthZ-Entscheidungen | Access-Services hinter `SecurityService` |

## Invarianten & Fallstricke

1. Locale ist **nicht** im Permission-Cache - separate Resolution.
2. Leere Map, wenn keine aktiven Gruppenmitgliedschaften (Schnittmenge mit aktiver Org-Mitgliedschaft); der Loader kennt keinen Sonderfall für `ALL_MEMBERS` - Permissions kommen nur über Mappings.
3. Parent-Org kaskadiert keine Permissions.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Loader | `userpermission/cache/CachedUserPermissionLoader.java` |
| Service | `userpermission/service/UserPermissionService.java` |
| Self | `userpermission/controller/SelfPermissionController.java` |
| DTO | `userpermission/dto/UserPermissionDto.java` |
| Eviction | `permission/cache/PermissionCacheEvictionListener.java` |
