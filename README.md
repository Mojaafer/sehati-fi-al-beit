# صحتي في البيت — Sehati Fi Al-Beit

Home-healthcare booking platform for Sudan. Patients book nursing, lab-draw,
physiotherapy and doctor visits at home; providers accept and complete jobs; an
admin verifies bank-transfer receipts and approves provider applications.

This repository is a monorepo with two projects:

| Folder | Project | Stack |
|---|---|---|
| [`android/`](android/) | Arabic (RTL) mobile app — booking, order lifecycle, payments review, notifications | Kotlin · Jetpack Compose (Material 3) · Firebase Auth + FCM · Firestore **or** Supabase PostgREST |
| [`website/`](website/) | Bilingual website | Next.js · Node/npm |

Each folder has its own README and toolchain — do not mix the Android Gradle
build with the website's npm tooling.

## Android app

Full setup, build, and architecture notes: [`android/README.md`](android/README.md)
and [`AGENTS.md`](AGENTS.md). Quick start:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\jdk-17.0.13+11"   # JDK 17 required
.\android\gradlew.bat -p android testDebugUnitTest assembleDebug -PuseSupabase=true
```

- `-PuseSupabase=true` routes the data layer through Supabase PostgREST with
  server-side enforcement (`transition_order` RPC + RLS); without it the app uses
  Firestore. Always smoke-test both after data-layer changes.
- `google-services.json` and the shared debug keystore are committed on purpose so
  collaborators can clone and run immediately — **this repo must stay private**.
- Supabase migrations live in `android/supabase/migrations/` and are immutable once
  pushed; add a new timestamped file for every schema change.

## Website

See [`website/README.md`](website/README.md) and
[`website/DEPLOYMENT.md`](website/DEPLOYMENT.md).

## CI

- **Dependabot** — weekly dependency checks for Gradle (`android/`), Python tools,
  and npm (`website/`).
- **OSV vulnerability scan** — runs on every push/PR plus weekly
  (`.github/workflows/osv-scanner.yml`).
