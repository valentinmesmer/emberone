# Caching-Strategie

Design-Notizen für Spring Cache über Redis: neun benannte Caches, JSON-Value-Wrapper und **After-Commit**-Eviction-Listener. Package roots: `com.emberone.platform.cache` / `platform.config` (`CacheConfig`), Domänen-`*cache`-Packages. Eng verwandt: [Multi-Tenancy](multi-tenancy.md), [Standard](standard.md), [Sicherheit & Berechtigungen](security-and-permissions.md).

Ohne dieses Dokument lernt man „was invalidiert was“ nur durch jeden `*CacheEvictionListener` einzeln.

## Mental Model

```
Domänenservices publizieren Events
        │
        ▼
@AfterCommitEventListener (AFTER_COMMIT + REQUIRES_NEW)
        │
        ▼
*CacheEvictionListener → *Evictor / cache.evict(...)
        │
        ▼
Redis: {cacheName}::{key} gelöscht
        │
        ▼
Nächster @Cacheable-Miss → DB-Reload
```

- Produktion: `spring.cache.type: redis` (`CacheConfig` setzt TTLs pro Cache).
- Tests: `spring.cache.type: simple` (In-Memory).
- Null-Werte werden nie gecacht (`disableCachingNullValues`).
- TTL ist Sicherheitsnetz bei verpasstem Event.

## Cache-Inventar

| Cache-Name | Key | Inhalt | TTL | Loader |
|------------|-----|--------|-----|--------|
| `userPermissions` | `{tenantId}:{userId}` | `UserPermissionDto` (Org → Permissions; **ohne** Locale) | 5 min | `CachedUserPermissionLoader` |
| `orgInTenant` | `{organizationId}:{tenantId}` | exists boolean | 60 min | `OrganizationTenantCache` |
| `effectiveStandardByOrgType` | `{tenantId}:{organizationTypeId}` | Standard-ID (null uncached) | 30 min | `EffectiveStandardTypeCache` |
| `effectiveStandardByOrg` | `{tenantId}:{organizationId}` | effektive Standard-ID | 30 min | `EffectiveStandardOrganizationCache` |
| `organizationsForStandard` | `{tenantId}:{standardId}` | Org-ID-Liste | 30 min | `StandardOrganizationIdCache` |
| `enabledModulesByStandard` | `{tenantId}:{standardId}` | `Set<Module>` | 30 min | `EnabledModulesCache` |
| `tenantLocale` | `{tenantId}` | `SupportedLocale` (Default `DE`) | 24 h | `TenantLocaleCache` |
| `tenantFederalState` | `{tenantId}` | `FederalState` | 24 h | `TenantFederalStateCache` |
| `activeOrgMembership` | `{userId}:{organizationId}` | active boolean | 5 min | `ActiveOrganizationMembershipCache` |
| `userActive` | `{userId}` | active boolean (Deaktivierungs-Check pro Request) | 60 s | `DeactivatedUserAccessService` |

Konstanten: `platform.cache.CacheNames`. Redis-Physische Keys: `{cacheName}::{key}`.

Auth-sensitive Caches (`userPermissions`, `activeOrgMembership`) kurze TTLs. Locale getrennt von Permissions, damit Locale-Änderungen Permission-Maps nicht flushen.

## Invalidierungsregeln

Alle Listener nutzen `@AfterCommitEventListener(readOnly = true)`. Evictors sind eigene Beans, damit Spring AOP `@CacheEvict` anwendet (Listener annotieren sich nicht selbst).

### `userPermissions` - `PermissionCacheEvictionListener`

| Event | Effekt |
|-------|--------|
| `UserPermissionsChangedEvent` | Evict dieser `userId` |
| `OrganizationGroupPermissionsChangedEvent` | Evict aller **aktiven** Mitglieder dieser Org-Gruppe |
| `PermissionGroupPermissionsChangedEvent` | Evict aktiver Mitglieder aller Org-Gruppen, die auf diese Permission Group gemappt sind |

Publisher u. a. Membership-Services, `PermissionGroupMappingService`, `PermissionGroupStandardDomain`.

Extra: `RequestScopedUserPermissions` memoized einmal pro HTTP-Request nach Redis/DB-Load.

### `orgInTenant` - `OrganizationCacheEvictionListener`

| Event | Effekt |
|-------|--------|
| `OrganizationCreatedEvent` | Key evicten (löscht mögliches negatives `exists=false`) |

Org-Updates ändern Existenz nicht → kein Evict. Kein Org-Delete-Pfad, der diesen Cache heute braucht.

### Standards - `StandardCacheEvictionListener`

| Event | Caches |
|-------|--------|
| `OrganizationCreatedEvent` | `effectiveStandardByOrg`; `organizationsForStandard` (Override und/oder Typ-Zuweisung) |
| `OrganizationTypeStandardAssignedEvent` | `effectiveStandardByOrgType`; erbende Orgs `effectiveStandardByOrg`; beide Standards `organizationsForStandard` |
| `OrganizationStandardOverrideChangedEvent` | Org `effectiveStandardByOrg`; vorheriger + neuer `organizationsForStandard` |
| `StandardEnabledModulesChangedEvent` | `enabledModulesByStandard` |

Org-effektiver Standard kann Type-Cache verschachtelt laden (zwei Redis-Einträge).

### `tenantLocale` / `tenantFederalState` - `TenantLocaleCacheEvictionListener`

| Event | Effekt |
|-------|--------|
| `TenantUpdatedEvent` | Evict `tenantLocale` und `tenantFederalState` je `{tenantId}` (via `TenantFederalStateCacheEvictor`) |

### `activeOrgMembership` - `OrganizationMembershipCacheEvictionListener`

| Event | Effekt |
|-------|--------|
| Membership Created / Updated / Ended | Evict `{userId}:{organizationId}` |

### `userActive` - `UserActiveCacheEvictionListener`

| Event | Effekt |
|-------|--------|
| `UserDeactivatedEvent` | Evict `{userId}` |

## Konfiguration

| Baustein | Rolle |
|----------|-------|
| `CachingEnableConfiguration` | `@EnableCaching` |
| `CacheConfig` | Redis-TTLs + Serializer (`@ConditionalOnProperty` redis) |
| `RedisCacheSerializerFactory` | Jackson JSON + Default-Typing für `com.emberone.*` |
| Value-Records | `platform.cache.value.*` (Skalare/Enums wrapped; Permissions direkt `UserPermissionDto`) |

Kein Redis-App-Cache: Thymeleaf `resolver.setCacheable(true)` in Mail-Config.

## Invarianten & Fallstricke

1. **Event + After-Commit-Eviction** bevorzugen statt `@CacheEvict` aus dem mutierenden Service.
2. Mandantenspezifische Werte brauchen `tenantId` im Key, wo Kollision möglich wäre (z. B. `userPermissions` = `{tenantId}:{userId}`). Ausnahme: `activeOrgMembership` keyed `{userId}:{organizationId}` - Org-IDs sind global eindeutig; kein `tenantId` im Key.
3. Locale nicht in `userPermissions` cachen.

4. Negatives Caching (`orgInTenant` false) braucht explizites Create-Evict oder 60-min-sticky Miss.
5. Fehlender Eviction-Listener ist Korrektheits-Bug; TTL begrenzt nur Veraltung.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Namen / Config | `platform/cache/CacheNames.java`, `platform/config/CacheConfig.java` |
| Permission | `permission/cache/`, `userpermission/cache/` |
| Organization | `organization/cache/` |
| Standard | `standard/cache/` |
| Membership | `userorganizationmembership/cache/` |
| Locale | `admin/tenant/cache/` |

Siehe auch: [Multi-Tenancy](multi-tenancy.md), [Standard](standard.md), [Sicherheit & Berechtigungen](security-and-permissions.md).
