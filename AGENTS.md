# AGENTS.md

EmberOne: multi-tenant management platform for fire departments. Spring Boot monolith (`backend/`) + Next.js (`frontend/`). OIDC via Authentik. Postgres with FORCE RLS.

Before working on a domain, read its concept doc: [`support/docs/concept/00-index.md`](support/docs/concept/00-index.md).

## Commands

Run only what the change touches. Tests need no running containers.

```bash
cd backend && ./gradlew test --tests '*FooServiceTest'    # targeted test
cd backend && ./gradlew spotlessApply                     # format Java
cd frontend && bun run lint                               # lint (Biome)
cd frontend && bun run format                             # format TS/CSS/JSON
python3 support/scripts/add-copyright-headers.py --check  # headers on new files
```

Full check, same as CI: `cd backend && ./gradlew build` and `cd frontend && bun run build`.

## Conventions

- Code, identifiers, commits: English, Conventional Commits. Docs: German.
- Reuse the existing pattern (same package, API surface, concept doc). No second way, no workarounds. If blocked, ask.
- Do not guess Authentik or deploy behavior: check the concept doc or ask.
- New source files get the Elastic-2.0 header (`add-copyright-headers.py`).

### Backend

- New domain tables use FORCE RLS. The tenant flows from `TenantContextHolder` into the GUC `app.tenant_id`, so never add `WHERE tenant_id = :tenantId`.
- Org authorization goes through `SecurityService` in `@PreAuthorize`, never through JWT claims or query filters:

  ```java
  @PreAuthorize("@securityService.hasUserWritePermission(#organizationId)")
  ```

- Keep the API surfaces separate: `/api/v1/self`, `/api/v1/auth/organizations/{id}`, `/api/v1/auth/admin`.
- List GETs are paginated with `ListQuery` + `ListDefinition` + `Specification`; the only exception is `@BoundedList`. No legacy params such as `openOnly` or `archived` (`support/scripts/check-list-legacy-params.sh`).
- Workflows: add a handler per type; do not fork `WorkflowService`.
- A new `PermissionType` needs the same numeric id in `PermissionType.java` and `frontend/types/permission.ts`.

### Frontend

- Routes mirror the API surfaces: `/my-area`, `/organizations/[id]`, `/admin`. Keep `page.tsx` thin.
- Text via `useTranslation` + `locales/de.json`; UI from `components/ui/*`; data via the store hooks in `lib/`.
- Lists use `defineList` + `useListState` + `ListControls`. Never `.filter` or `.sort` paginated data on the client.
- Before adding a button or action, read [`frontend-actions.md`](support/docs/concept/frontend-actions.md). Entity-wide actions (delete, archive, status, …) go into `ActionsSection` at the page bottom, never into a header icon.

## Done means

- The checks for the touched area pass.
- Security, tenant or RLS change → covered by a `*SecurityTest`.
- API, permission or lifecycle change → concept doc updated in the same change.

## Boundaries

- Never run `git commit` or `git push`; the maintainer does. Base branch is `development`.
- Ask first before adding dependencies or changing Flyway migrations, CI, or Authentik blueprints.
- No secrets, no copyrighted or incompatibly licensed code. Vulnerabilities: [`SECURITY.md`](SECURITY.md).
