# jobtrail-frontend

Vite + React + TypeScript + Tailwind v4 + shadcn/ui. Talks to the Spring
Boot backend (`../backend`) only through `src/lib/api-client.ts`, and only
over relative paths — see "Reaching the backend" below.

## Run locally

```bash
npm install
npm run dev
```

Opens on `http://localhost:5173`. Needs the backend running on `:8080`
(`../backend`, `./mvnw spring-boot:run`) for anything beyond the static
shell to work — the header's backend-status badge is the visible check for
that.

## Reaching the backend

The backend (`SecurityConfig`) has no CORS configuration — it isn't meant
to serve a browser origin directly. Rather than add CORS just for local
dev, `vite.config.ts` proxies `/api/**` and `/actuator/**` to
`http://localhost:8080`, same-origin from the browser's perspective. A real
deploy serves the built frontend from behind the same origin/gateway as the
API for the same reason.

`src/lib/api-client.ts` is the only thing that calls `fetch` — every
request is a relative path (`/api/...`, `/actuator/...`) so it works
against the dev proxy unmodified. `VITE_API_BASE_URL` (see `.env.example`)
only needs setting for a split-origin setup, e.g. a locally-run frontend
against a deployed backend.

## Stack

- **Routing**: `react-router-dom`, one layout route (`AppLayout`) wrapping
  page routes — see `src/routes/router.tsx`.
- **Server state**: TanStack Query, one `QueryClient` (`src/lib/query-client.ts`)
  provided at the app root. `src/hooks/use-health.ts` is the reference
  example: a query hook built on `apiFetch`.
- **UI**: shadcn/ui components in `src/components/ui` (added via
  `npx shadcn@latest add <component>`, see `components.json`) on Tailwind
  v4's CSS-first config (`src/index.css`, no `tailwind.config.*`).
- **Path alias**: `@/*` → `src/*` (see `tsconfig.app.json` / `vite.config.ts`).

## Build

```bash
npm run build   # tsc -b && vite build
npm run lint    # oxlint
```
