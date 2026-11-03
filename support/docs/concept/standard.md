# Standard (Organisations-Setup)

Design-Notizen für das Standards-Modul - zentrale Facade `Standard` mit hoher Kopplung. Package root: `com.emberone.standard`, Domain-Plugins in Geschwister-Packages (`organizationgroup/standard`, `permissiongroup/standard`, `workflow/standard`, …).

## Mental Model

Ein **Standard** ist ein benannter, wiederverwendbarer **Organisations-Setup-Blueprint** für einen Mandanten. Er beantwortet: Wenn wir eine Organisation anlegen (oder neu zuweisen), welche Org-Gruppen, Permission Groups, Workflow-Genehmigungen/-Definitionen und Katalog-Teilmengen sollen gelten?

Er ist **nicht**:

| Verwechselt mit | Tatsächliche Rolle |
|-----------------|-------------------|
| **`Module`** (`CORE`, `SERVICE`, …) | Feature-Bereich; Standard kann schaltbare Module via `StandardModule` *deaktivieren* |
| **`WorkflowType`** | Festes Enum von Antragsarten; Standard *mappt* jeden Typ auf Genehmiger-Org-Gruppe |
| **`Qualification` / Cert / Training-Def** | Mandanten-Katalogeinträge; Standard **selektiert** nur, welche eine Org sieht |
| **`Organization`** | Konsument eines Standards; erhält materialisierte Instanzzeilen |

```
SYSTEM-Baseline (Code) ──sync──► SYSTEM Standard (read-only Templates)
                                        │
                                     clone
                                        ▼
                                 CUSTOM Standard ──assign──► OrganizationType
                                        │                         │
                                   (Override?)                    │ inherit
                                        ▼                         ▼
                                   Organization.standardId     effektiver Standard
                                        │
                         materialize / reassign (StandardDomain SPI)
                                        ▼
                    Org-Instanzen: Gruppen, Perm Groups, wf mappings/defs
                    (+ Katalog-Filter zur Read-Time für Certs/Training/Quals)
```

## Kernentitäten

### `Standard` (`std_standards`)

| Feld | Bedeutung |
|------|-----------|
| `tenantId` | Mandantenspezifisch |
| `kind` | Heute immer `ORGANIZATION_SETUP` (Hook für künftige Arten) |
| `source` | `SYSTEM` (read-only Klon-Quelle) oder `CUSTOM` (editierbar) |
| `name` / `description` | Anzeige-Metadaten |
| `baseStandardId` | Klon-Linie (System-Baseline für Customs) |

`isSystem()` <=> `source == SYSTEM`. SYSTEM-Standard **darf nicht direkt zugewiesen** werden - Mandanten klonen → CUSTOM → zuweisen.

### Zuweisung & Override

| Entity / Feld | Rolle |
|---------------|-------|
| `StandardAssignment` (`std_assignments`) | Genau ein Standard pro `OrganizationTypeDefinition` |
| `Organization.standardId` | Optionales **per-Org-Override**; `null` = Typ-Zuweisung erben |
| `StandardModule` (`std_standard_modules`) | **Deaktivierte** schaltbare Module (fehlende Zeile = enabled; `CORE` nie gespeichert) |

### Effektive Auflösung

`EffectiveStandardResolver`:

1. Wenn `Organization.standardId` gesetzt → dieser Standard
2. Sonst → Standard zugewiesen zum Org-Typ (`std_assignments`)

Gecacht via `EffectiveStandardOrganizationCache` / `EffectiveStandardTypeCache`. Genutzt von Materialisierung, Katalog-Services, `EnabledModuleResolver`.

**Readiness:** Org-Create braucht **CUSTOM**-Standard am Org-Typ (`StandardReadinessService` → `StandardNotConfiguredException`).

## Template- vs. Instanz-Ebenen

| Ebene | Speicher | Hinweise |
|-------|----------|----------|
| Library-Templates | `std_*_templates`, Selection-Tabellen | Keyed by `standard_id` + `config_key` |
| Mandanten-Katalog | Cert/Training/Qual-Defs, `tenant_workflow_definitions`, Org-Typ-Defs | Mandantenweit geteilt; Standards selektieren Teilmengen |
| Org-Instanzen | `organization_groups`, `perm_groups`, `perm_group_mappings`, `wf_type_mappings`, `wf_definitions` | Tragen `configKey` + `standardOriginId` |

**Zwei Config-Key-Systeme (nicht vermischen):**

| System | Wo | Form |
|--------|----|------|
| **Standard-Templates** | Org-Gruppen, Permission Groups, Workflow-Mappings/Defs am Standard | System-Enum-Name (z. B. `LEADERSHIP`) oder von `StandardConfigKey` allokiertes `custom:slug` |
| **Mandanten-Kataloge** | Lehrgang, Nachweis, Führerschein, Qualifikation, Eintrittsdokument | `config_key` **ist** die Kennung/Kürzel der Katalogzeile; Rename zieht Selections, Qualifikations-Voraussetzungen und Onboarding-Step-Referenzen mit |

`StandardConfigKey` baut nur Template-Keys (`custom:…`); Katalog-Domains und Eintrittsdokumente nutzen dieses Hilfsmittel nicht.

## StandardDomain SPI

`StandardDomain` - ein Spring-Bean pro beteiligter Domäne. `StandardDomainRegistry` fächert Lifecycle aus; `@Order` erzwingt Abhängigkeitsreihenfolge (Org-Gruppen vor Permission/Workflow-Mappings).

| Hook | Zweck |
|------|-------|
| `domainKey()` | Stabile ID (Logging / Content-UI) |
| `copyTemplates(source, target)` | Klonen „Klonen & anpassen“ |
| `materializeForOrg(orgId, standard)` | Frische Org: fehlende Instanzzeilen |
| `reassignForOrg(orgId, standard)` | Standard gewechselt: Upsert per `configKey`, obsolete Standard-Origin-Zeilen entfernen |
| `describeContent(standard)` | Admin-Content-UI (`StandardContentItem`) |

### Alle Implementierungen

| `@Order` | Klasse | `domainKey` | Materialisiert? |
|----------|--------|-------------|-----------------|
| 10 | `OrganizationGroupStandardDomain` | `organization-groups` | → `organization_groups` |
| 20 | `PermissionGroupStandardDomain` | `permission-groups` (+ synthetisch `group-mappings` in Content) | → `perm_groups`, `perm_group_mappings` |
| 30 | `WorkflowTypeMappingStandardDomain` | `workflow-type-mappings` | → `wf_type_mappings` (+ Sync-Flags auf `wf_definitions`) |
| 40 | `WorkflowDefinitionStandardDomain` | `workflow-definitions` | → `wf_definitions` (mandantenweite statische Genehmiger via `TenantWorkflowDefinitionService`) |
| 50 | `CertificationStandardDomain` | `certifications` | **Keine** per-Org-Zeilen - Filter beim Katalog-Read |
| 51 | `TrainingCourseStandardDomain` | `training-courses` | Gleich |
| 52 | `QualificationStandardDomain` | `qualifications` | Gleich; publiziert `QualificationCatalogChangedEvent` |
| 53 | `DrivingLicenseStandardDomain` | `driving-licenses` | Gleich |
| 54 | `RankStandardDomain` | `ranks` | Gleich (Dienstgrade; keine Laufzeit-Filterung bei Vergabe) |
| 55 | `DutyFunctionStandardDomain` | `duty-functions` | Grants → `std_function_grants` (keine per-Org-Materialisierung) |
| 56 | `FunctionSelectionStandardDomain` | `functions` | Funktions-Selektionen für Grant-UI |

Katalog-Domains (50-53, 54, 56) speichern nur **Selektionen** am Standard; Änderung ändert Katalog-Rückgabe für Orgs, deren effektiver Standard diese Keys enthält.

**Kein `StandardDomain`:** Onboarding-Steps und Eintrittsdokumente (Tenant-/Org-Katalog) gehören zur Onboarding-Domäne (`StandardOnboardingStepService`, `std_onboarding_steps`) - siehe [Onboarding](onboarding.md). Sie erscheinen in der Admin-Standard-API (`…/onboarding-steps`), nicht in der Domain-Registry-Tabelle oben.

## Lifecycle-Abläufe

### Baseline-Sync

Beim **Anlegen** eines Mandanten: `TenantStandardsBootstrapService.syncBaseline` (nach Persist der Tenant-Zeile). Der Sync läuft nur dabei (synchron im Admin-Request zum Anlegen) und nie im normalen Request-Pfad. Neue Baseline-Einträge aus einem Release erreichen bestehende Mandanten nicht automatisch: `POST /api/v1/auth/admin/tenants/{tenantId}/baseline-sync` (nur `GLOBAL_ADMIN`, idempotent, 204) stößt den Sync pro Mandant erneut an; `TenantBaselineSyncService` hält keinen JVM-Zustand und serialisiert pro Mandant über einen Postgres-Advisory-Lock. `patchTenant` ändert heute nur den Namen; eine spätere Änderung des Bundeslands müsste separat `ensureTenantBaselineSynced()` anstoßen.

Ablauf:

1. Mandanten-Kataloge + Org-Typen + Mandanten-Workflow-Defs seeden/syncen (über `FederalStateSettings` des Tenants, heute `BayernFederalStateSettings`)
2. SYSTEM-`Standard` upserten + Templates syncen (`OrgSetupBaselineTemplateService`)
3. `ensureWorkflowApprovalsAcrossStandards` - fehlende Workflow-Templates auf *allen* Standards via `ensureMissingFromBaseline` (übernimmt `item.enabled()` aus der Baseline; Bayern: `enabled=true`), dann betroffene Orgs rematerialisieren

Zwei Code-Baselines formen `FederalStateBaselines`:

- `BayernOrganizationSetupBaseline` → `OrganizationSetupBaseline` (Gruppen, Perms, Workflow-Type-Mappings)
- `BayernTenantBaseline` → `TenantBaseline` (Mandanten-Katalog-Seeds, Workflow-Definitionen)

### Klonen → zuweisen → materialisieren

1. **Klonen** SYSTEM → CUSTOM (`StandardService.cloneStandard`): `registry.copyTemplates` → Module-Zeilen (`copyModuleConfiguration`) → `StandardOnboardingStepService.copySteps` → `selectAllCatalogItems` → `ensureRequiredSystemGroups`
2. **Zuweisen** CUSTOM an Org-Typ (`StandardService.assignStandard`) → Ziel-Standard **explizit** an `reassignStandardForType` (nur erbende Orgs, `standardId == null`; Override-Orgs unberührt)
3. **Org-Create** → `OrganizationCreatedEvent` → Sync-Listener `OrganizationCreatedMaterializationListener` → `StandardMaterializationService.materializeForOrg`
4. **Per-Org-Override** → `setOrganizationStandardOverride` → Ziel-Standard **explizit** an `reassignStandardForOrg(org, standard)` (nicht über den Org-Effective-Cache; Eviction erst after-commit)
5. **Template-Edits** am CUSTOM-Standard propagieren live zu getriebenen Orgs (`StandardOrganizationResolver` + Domain-Edit-APIs)

### Reassign-Regeln

- Instanzen per `configKey` upserten
- Instanzen löschen/archivieren, deren `standardOriginId` gesetzt, Key aber nicht mehr in Templates
- Mitgliedschafts-bewusste Org-Group-Behandlung: archivieren vs. Hard-Delete bei Mitgliedern
- Neue Baseline-Workflow-Genehmigungen landen auf bestehenden Standards mit dem Baseline-`enabled`-Flag (`ensureMissingFromBaseline` / Bayern: enabled)

## Caching

| Cache | Was | Evicted by |
|-------|-----|------------|
| `effectiveStandardByOrgType` | Typ → Standard-ID | `OrganizationTypeStandardAssignedEvent` |
| `effectiveStandardByOrg` | Org → Standard-ID | Org-Create, Override-Change, Typ-Reassignment |
| `organizationsForStandard` | Standard → Org-IDs | gleich (prev/new Standard-IDs) |
| `enabledModulesByStandard` | Standard → enabled modules | `StandardEnabledModulesChangedEvent` |

Listener: `StandardCacheEvictionListener` (`@AfterCommitEventListener`).

## Admin-API

`AdminStandardController` → `/api/v1/auth/admin/tenants/{tenantId}/standards`  
Fassade: `AdminStandardService` → `StandardService`

- List / clone / update meta / delete / content / modules
- Content-Editoren: org-groups, permission-groups, group-mappings, workflows, Katalog-Selektionen (`…/rank-selections`, `…/function-selections`, `…/function-grants`)
- Onboarding-Steps: `GET/PUT …/standards/{id}/onboarding-steps` (nicht über `StandardDomain`-SPI; eigene Tabelle `std_onboarding_steps`)
- Assignments: list, put pro Org-Typ, Impact-Preview
- Overrides: list, get/set Org-Standard, Impact-Preview

## Invarianten & Fallstricke

1. SYSTEM ist read-only und **nicht zuweisbar**; nur CUSTOM darf zugewiesen werden.
2. Jeder Standard muss required System-Org-Group-Templates behalten (z. B. `LEADERSHIP`, `ALL_MEMBERS`).
3. Org-Gruppe, die als Workflow-Genehmiger dient, nicht löschen; System-Org-/Permission-Groups nicht löschen.
4. Nur CUSTOM Permission Groups hinzufügbar; Grants gefiltert nach enabled modules.
5. Katalog-Domains schreiben keine per-Org-Zeilen - Selektionsänderungen nur Read-Time-Filterung.
6. Permission-Content zeigt auch `group-mappings` (kein separates SPI-Bean).

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Entity | `standard/domain/Standard.java` |
| SPI + Registry | `standard/spi/StandardDomain.java`, `StandardDomainRegistry.java` |
| Orchestrierung | `standard/service/StandardService.java` |
| Materialisierung | `standard/service/StandardMaterializationService.java` |
| Effektive Auflösung | `standard/service/EffectiveStandardResolver.java` |
| Baseline-Sync | `standard/service/TenantBaselineSyncService.java`, `admin/tenant/service/TenantStandardsBootstrapService.java`, `federalstate/baseline/*`, `federalstate/bayern/*` |
| Caches | `standard/cache/*` |
| Domain-Plugins | `*/standard/*StandardDomain.java` (siehe Tabelle) |
| Onboarding-Steps | `onboarding/service/StandardOnboardingStepService.java` → [Onboarding](onboarding.md) |
| Admin | `admin/standard/controller/AdminStandardController.java` |

Siehe auch: [Workflow-Engine](workflow-engine.md) für Workflow-Mapping/-Definition-Domains und Runtime; [Dienstgrad und Funktion](rank-and-function.md) für `RankStandardDomain`, `FunctionSelectionStandardDomain` (`functions`) und `DutyFunctionStandardDomain` (`duty-functions`).
