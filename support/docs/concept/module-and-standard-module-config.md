# Module & Standard Module Config

Design-Notizen für Feature-Module und deren Toggle **pro Standard** (nicht direkt pro Org). Package roots: `com.emberone.module` (Enum), Persistenz/Auflösung in `com.emberone.standard` (`StandardModule`, `EnabledModuleResolver`). Kontext: [Standard](standard.md), Permission-Filter: [Permission Groups](permission-groups.md).

## Mental Model

```
Standard ──(disabled rows)──► StandardModule(module)
Organization ──effective► Standard ──EnabledModuleResolver──► Set<Module>
PermissionType.module ──filter──► sichtbare Grants
```

Gespeicherte Zeilen in `std_standard_modules` = **deaktivierte** toggleable Module. Fehlende Zeile = enabled. `CORE` wird nie gespeichert.

## Kernentitäten

| Entity / Typ | Rolle |
|--------------|-------|
| `Module` | Feature-Bereich (`CORE`, `SERVICE`, `OPERATIONS`, `APPOINTMENTS`, `COURSE_OFFERINGS`, `MANAGEMENT`); trägt zugehörige `PermissionType`s |
| `StandardModule` (`std_standard_modules`) | Disable-Zeile pro Standard+Modul; steuert `EnabledModuleResolver` |

## Module-Werte

| Enum | Toggleable | Bedeutung |
|------|------------|-----------|
| `CORE` | nein (immer an) | Basis: User, Docs, Workflow, Cert/Training/Qual, … |
| `SERVICE` | ja | Dienst-Events (`SERVICE_EVENT_*`) |
| `OPERATIONS` | ja | Einsatz-Events (`OPERATION_EVENT_*`) |
| `APPOINTMENTS` | ja | Termine + Event-Attendance (`APPOINTMENT_*`, `EVENT_ATTENDANCE_*`) |
| `COURSE_OFFERINGS` | ja | Lehrgangsangebote (`COURSE_OFFERING_*`) |
| `MANAGEMENT` | ja | Verwaltung anderer Organisationen (`MANAGEMENT_ACCESS`); Default an |

## Was es besitzt

- Toggle-Zustand pro Standard (`StandardService.get/updateEnabledModules`)
- Auflösung Standard → enabled Modules (`EnabledModuleResolver.forStandard`)
- Filter von `PermissionType`s (`Module.filterPermissions`)
- Sync von Permission-Group-Instanzen bei Modul-Disable (`PermissionGroupStandardDomain`)
- Cache: `EnabledModulesCache`; Event: `StandardEnabledModulesChangedEvent`
- Admin-API: `GET/PUT /api/v1/auth/admin/tenants/{tenantId}/standards/{id}/modules`
- Admin-UI: `StandardModuleManagement` auf der Standard-Detailseite (`/admin/tenants/[id]/standards/[standardId]`)

## Was es nicht besitzt

| Thema | Hinweis |
|-------|---------|
| Feature-Flags in der Org-Tabelle | Org erbt über `EffectiveStandardResolver` |
| Domain-Logik Termine/Events | nur Permission-Sichtbarkeit |
| Catalog-Selections (Cert/Training/Qual) | **Inhalt** des Standards, getrennt von Modul-Toggles - siehe [Standard](standard.md) |
| UI-Feature-Gates im Frontend | spiegeln Permissions / Modul-Verfügbarkeit indirekt |

## Key Flow

1. Admin setzt enabled Modules → Disabled-Rows speichern → Event → Cache-Evict (after-commit)
2. Sofort `PermissionGroupStandardDomain.syncInstancesForEnabledModules` mit dem neuen Enabled-Set (nicht dem Cache): **alle** Permission-Group-Templates und Org-Instanzen werden auf die enabled Modules zugeschnitten
3. Runtime: `allEnabled − disabled`
4. Neue Grants dürfen nur Permissions enabled Modules tragen (`requireModulesEnabled`)

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Enum | `module/domain/Module.java` |
| Persistenz | `standard/domain/StandardModule.java` |
| Resolver | `standard/service/EnabledModuleResolver.java` |
| Service / Admin-API | `standard/service/StandardService.java`, `admin/standard/controller/AdminStandardController.java` (`…/standards/{id}/modules`) |
| Cache | `standard/cache/EnabledModulesCache.java` |
| Admin-UI | `frontend/components/admin/standards/StandardModuleManagement.tsx` |

## Modul MANAGEMENT

`MANAGEMENT` ist toggleable und standardmäßig an. Audience-Konfiguration und `MANAGEMENT_ACCESS` folgen dem Modul wie bei anderen Feature-Modulen. Details: [management.md](management.md).
