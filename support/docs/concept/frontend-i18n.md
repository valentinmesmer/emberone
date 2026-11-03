# Frontend i18n (`useTranslation`)

Design-Notizen zu Locale- und Key-Konventionen für `useTranslation()`. Die Nutzung ist ein Standard-i18n-Hook; dieses Dokument beschreibt **wo Dateien liegen** und wie Keys strukturiert sind.

## Mental Model

```
PermissionProvider ──tenantLocale──► LocaleProvider
                                              │
                                              ├─ document.documentElement.lang
                                              ├─ I18nProvider (React Aria, BCP-47)
                                              └─ useTranslation() → locales/de.json
```

Die aktive Locale kommt aus **`permissions.tenantLocale`** (Self-Permissions-API), Fallback `DEFAULT_LOCALE` (`'de'`). Heute **keine** Benutzereinstellung. Nur `de` wird unterstützt.

## Wichtige Dateien

| Rolle | Pfad |
|-------|------|
| Hook | `hooks/useTranslation.ts` |
| Lookup + Interpolation | `lib/i18n/translations.ts` |
| Locale-Typen | `lib/i18n/locale.ts` |
| Messages | `locales/de.json` |
| Provider | `components/locale/LocaleProvider.tsx` (wickelt React-Aria `I18nProvider` ein) |

## Locale-Konventionen

```ts
type Locale = 'de';
DEFAULT_LOCALE = 'de';
toBcp47Locale('de') → 'de-DE'  // React Aria / Intl
```

`LocaleProvider` setzt `document.documentElement.lang`. Server-Metadaten nutzen dieselben `t(...)`-Helfer mit Default `de`.

## Key-Konventionen

1. **Form:** verschachteltes JSON → **punktgetrennter Pfad** (`common.save`, `myArea.workflowDetail.actorUser`).
2. **Segmente:** **camelCase** (`userManagement`, `emergencyContacts`, `noPermission`).
3. **Top-Level-Domänen** in `de.json` (nicht vollständig): `admin`, `app`, `appointments`, `auth`, `certifications`, `common`, `events`, `home`, `locale`, `myArea`, `notifications`, `organization`, `organizationManagement`, `permissions`, `pwa`, `qualifications`, `todos`, `trainingCourse`, `userManagement`, `workflows`, ...
4. **Interpolation:** doppelte geschweifte Klammern `{{paramName}}`  
   Beispiel: `t('myArea.workflowDetail.actorUser', { id: decision.userId })`
5. **Fehlender Key:** gibt den Key unverändert zurück (Signal für „fehlende Übersetzung“).
6. **Permission-Labels:** `permissions.<ENUM_NAME>` passend zu `PermissionTypeEnum`-Membernamen (nicht numerische IDs).

## Geteilte Vokabeln (KISS)

Wiederkehrende UI-Chrome nicht unter Feature-Namespaces kopieren:

| Zweck | Keys | Nutzung |
|-------|------|---------|
| Aktionen, Menü, Validierung | `common.*` | `save`, `cancel`, `edit`, `delete`, `menu`, … |
| Datei-Anhänge | `common.attachments.*` | `chooseFile`, `removeFile`, `download` |
| Antragsformulare (identischer Text) | `common.request.*` | `submit`, `errorSubmit`, `successSubmitted`, `reasonLabel`, `attachmentsLabel`, … |
| Nachweis-/Record-Detail (identisch) | `common.record.*` | `documentsTitle`, `noDocuments`, `notesLabel` |
| Workflow-Typ-Labels | `admin.standards.workflowTypes.<ENUM>` | Eine Map; `workflowTypeTranslationKey()` und Standards-UI |

Form-spezifische Texte (`modalIntro`, `errorAlreadyOpen`, `attachmentsHint` mit anderem Wortlaut) bleiben unter `myArea.*` o. ä. Einige `myArea.*`-Keys werden bewusst aus Org-/Admin-UI referenziert (z. B. Abwesenheit, Zeiterfassung) - das ist kein Eigentumsfehler, sondern shared Copy.

**Anti-Pattern:** gleichen String unter `admin.*` / `myArea.*` erneut anlegen, wenn er schon unter `common.*` existiert.

**Prüfung:** `python3 support/scripts/check-unused-i18n-keys.py` (Exact-Strings + dynamische Enum-Prefixes; vor PR bei i18n-Änderungen).

## Hook-Oberfläche

```ts
const t = useTranslation();
t('common.save');
t('some.key', { count: 3 });
```

Implementierung: `useLocale()` → memoized `(key, params?) => t(key, locale, params)`.

## Strings hinzufügen

1. Verschachtelten Pfad unter der passenden Top-Level-Domäne in `locales/de.json` anlegen.
2. `t('domain.path.to.key')` in der Komponente aufrufen.
3. Bestehende Domänen bevorzugen statt neuer Top-Level-Wurzeln.
4. Permission-Anzeigenamen mit `PermissionTypeEnum`-Namen synchron halten.

## Fallstricke

1. `useTranslation` importiert `@/components/locale/LocaleProvider` direkt (kein Zyklus über `components/locale/index.ts`). Locale-Barrel weiterhin nicht in die Hook-Abhängigkeitskette ziehen.
2. Locale ist mandantengetrieben - UI-Sprache ist keine Client-Einstellung, bis Multi-Locale aktiv ist.
3. Fehlende Keys geben still den Key zurück - kann in Produktion wie „kaputter Text“ wirken.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Hook | `hooks/useTranslation.ts` |
| Auflösen / interpolieren | `lib/i18n/translations.ts` |
| Locale-Typen | `lib/i18n/locale.ts` |
| Katalog | `locales/de.json` |
| Provider | `components/locale/LocaleProvider.tsx` → `@react-aria/i18n` `I18nProvider` |

Siehe auch: [Frontend Auth & Berechtigungen](frontend-auth.md) (`tenantLocale` im Permissions-DTO).
