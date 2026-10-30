# Bundesland (Federal state)

Design-Notizen zur mandantenweiten Bundesland-Konfiguration und dem Extension Point `FederalStateSettings`. Package-Root: `com.emberone.federalstate`.

## Mental Model

Jeder Mandant gehört genau einem deutschen **Bundesland** (`FederalState`-Enum, z. B. `BAYERN`). Es wird bei der Mandanten-Erstellung gesetzt und ist unveränderlich (wie `locale`).

Das Bundesland bestimmt die gebündelte **System-Baseline**: Zertifizierungskatalog, Lehrgänge, Führerscheinklassen, Qualifikationen, Org-Setup-Standard **und** das `RetentionProfile`. Derzeit ist nur **Bayern** konfiguriert.

```
Tenant.federalState
        │
        ▼
FederalStateService.settings(tenantId)
        │
        ▼
FederalStateSettingsRegistry → BayernFederalStateSettings (derzeit einzige Bean)
        │
        └── FederalStateBaselines
                ├── TenantBaseline (Katalog)
                ├── OrganizationSetupBaseline (Org-Gruppen, Berechtigungen, Workflows)
                └── RetentionProfile (Fristen; Bayern: BayernRetentionProfile / BY)
```

Baselines sind der Einstiegspunkt: Domänencode ruft `FederalStateService.settings(tenantId)` bzw. `baselines().retentionProfile()` auf - **nicht** verstreute `if (federalState == BAYERN)` in Services. Spätere bundeslandspezifische Regeln (Altersgrenzen, Führungsbezeichnungen, …) gehören ebenfalls an `FederalStateSettings`, nicht an die Baselines-Record-Komponenten.

**Aufbewahrung:** `RetentionPolicyService` löst die Frist je `RetentionCategory` über `FederalStateBaselines.retentionProfile()` für das Bundesland des Mandanten auf. Details: [Aufbewahrung und Löschung](retention-and-erasure.md).

## Mandantenfeld

| Feld | Typ | Hinweise |
|------|-----|----------|
| `federalState` | `FederalState` | Pflicht, Default `BAYERN`, nach Create unveränderlich |

Gespeichert als `tenants.federal_state VARCHAR(32) NOT NULL`.

## Konfigurierte vs. bekannte Bundesländer

Das `FederalState`-Enum listet alle 16 Bundesländer. **Nur Bundesländer mit einer `FederalStateSettings`-Spring-Bean** können bei der Mandanten-Erstellung gewählt werden. Nicht konfigurierte Bundesländer → `400 UnsupportedFederalStateException`.

Admin-API: `GET /api/v1/auth/admin/tenants/federal-states` liefert die konfigurierten Werte.

## Extension Point: `FederalStateSettings`

```java
public interface FederalStateSettings {
    FederalState federalState();
    FederalStateBaselines baselines(); // tenant + organizationSetup + retentionProfile
}
```

Neues Bundesland hinzufügen:

1. `NordrheinWestfalenTenantBaseline` + `NordrheinWestfalenOrganizationSetupBaseline` + `*RetentionProfile` anlegen.
2. `NordrheinWestfalenFederalStateSettings` als `@Component` implementieren und `FederalStateBaselines(..., retentionProfile)` bauen.
3. Das Bundesland erscheint automatisch in der Create-UI.

Neuen Regeltyp hinzufügen:

1. Methode an `FederalStateSettings` ergänzen (ggf. mit Default).
2. In den Settings-Klassen der jeweiligen Bundesländer implementieren.
3. Über `federalStateService.settings(tenantId)` aufrufen.

## Frontend

- Admin-Create-Formular lädt Optionen über `listConfiguredFederalStates()`.
- Labels: `federalState.BAYERN` usw. in `locales/de.json`.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Enum | `federalstate/domain/FederalState.java` |
| SPI + Registry | `federalstate/FederalStateSettings.java`, `FederalStateSettingsRegistry.java` |
| Lookup | `federalstate/FederalStateService.java` |
| Cache | `admin/tenant/cache/TenantFederalStateCache.java` |
| Bayern-Baselines | `federalstate/bayern/BayernTenantBaseline.java`, `BayernOrganizationSetupBaseline.java`, `BayernFederalStateSettings.java` |
| Bayern-Retention | `federalstate/bayern/BayernRetentionProfile.java` |
| Baseline-Typen | `federalstate/baseline/FederalStateBaselines.java`, `TenantBaseline.java`, `OrganizationSetupBaseline.java`, `BaselineValidator.java` |
| Sync-Verdrahtung | `TenantBaselineSyncService`, `TenantStandardsBootstrapService`, `StandardService.ensureTenantBaselineSynced` |
| Tenant-Admin | `admin/tenant/service/AdminTenantService.java` |

Siehe auch: [Admin](admin.md), [Standard](standard.md), [Multi-Tenancy](multi-tenancy.md), [Aufbewahrung und Löschung](retention-and-erasure.md), [Dienstgrad und Funktion](rank-and-function.md) (Bayern-Seeds für Ränge/Funktionen).
