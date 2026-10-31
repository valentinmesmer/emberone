# Frontend PWA / Offline (Serwist)

Design-Notizen zur installierbaren PWA mit Precache und Offline-Fallback - **kein** Offline-First für API-Daten. Packages: `serwist`, `@serwist/turbopack`.

## Mental Model

```
Build / Serwist precache ──► Shell + Assets (+ /~offline)
        │
        ▼
Runtime (app/sw.ts)
  /api/*                 → NetworkOnly
  /runtime-config.json   → NetworkOnly   (Container schreibt pro Deploy)
  defaultCache           → Assets / Navigation
  Offline-Navigation     → /~offline
        │
Logout ──► clearClientCachesBeforeLogout()  (siehe Auth)
```

## Wichtige Dateien

| Rolle | Pfad |
|-------|------|
| Next-Wrapper | `next.config.ts` → `withSerwist(...)` |
| Service Worker | `app/sw.ts` |
| SW-Route | `app/serwist/[path]/route.ts` |
| Provider | `app/layout.tsx` → `SerwistProvider` |
| Offline-Page | `app/~offline/page.tsx` |
| Manifest | `public/site.webmanifest` |
| Install-Prompt | `components/pwa/PwaInstallPrompt.tsx` |
| Logout-Cleanup | `lib/auth/logoutClientCleanup.ts` |

## Konfiguration

- SW-URL: `/serwist/sw.js`
- **Dev deaktiviert:** `disable={process.env.NODE_ENV === "development"}`
- Precache: `self.__SW_MANIFEST` + Extra-Entry `/~offline` (Revision: Commit-Hash / UUID)
- `globIgnores: ['public/runtime-config.json']` in `app/serwist/[path]/route.ts` - Datei wird erst im Container geschrieben; ein Precache-Hash würde einen veralteten Inhalt festnageln
- `skipWaiting`, `clientsClaim`, `navigationPreload`, `cleanupOutdatedCaches`
- `reloadOnOnline={false}`

## Caching-Strategie (`app/sw.ts`)

```
1. same-origin /api/*              → NetworkOnly   (nie cachen; Auth-scoped)
2. same-origin /runtime-config.json → NetworkOnly   (pro Deploy neu; vor defaultCache)
3. …defaultCache                   → Serwist Turbopack Defaults (Assets/Navigation)
4. Document-Fallback               → /~offline bei Offline-Navigation
```

## Was offline funktioniert / nicht

| Funktioniert | Funktioniert nicht |
|--------------|-------------------|
| Installierbare PWA (Manifest, Icons, `standalone`) | API-Calls / authentifizierte Daten |
| Precachte Build-Assets | Frische App-Seiten mit Live-Daten |
| Navigation-Fallback → `/~offline` | Offline-Editing / Sync-Queue |
| Soft Install-Prompt (nach ≥2 Besuchstagen) | Service Worker in Development |
| Frisches `/runtime-config.json` (NetworkOnly) | Gecachte Runtime-Config über Deploys hinweg |

Beim Logout werden alle Cache-Storage-Einträge geleert (`lib/auth/logoutClientCleanup.ts`), damit keine precachte Shell oder Assets der vorherigen Session überdauern. PWA-Install-Präferenzen (`emberone.pwa.install`) bleiben unberührt. Details: [Frontend Auth](frontend-auth.md).

Produktaussage der Offline-Page: App ist lokal vorbereitet - Retry wenn wieder online. Keine Offline-Datenschicht.

Siehe auch: [Frontend API-Client & Runtime Config](frontend-api-client.md) (`runtime-config.json` / Docker-Entrypoint).
