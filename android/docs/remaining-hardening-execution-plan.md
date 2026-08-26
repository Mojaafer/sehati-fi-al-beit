# Remaining Hardening Execution Plan

## Goal

Complete the Supabase cutover hardening for the Sehati Android app without weakening
the existing Firebase-authenticated client flow or the current RLS model.

## Workstreams

### 1. Server-side order lifecycle enforcement

- Inventory every order status write in `SehatiViewModel` and both repository implementations.
- Add a Supabase RPC that validates the caller, current status, participant role, and
  allowed target status before applying the update.
- Route Supabase status changes and cancellations through the RPC instead of direct
  PostgREST updates.
- Keep Firestore behavior unchanged until the Supabase path is proven on device.
- Add unit coverage for request construction and invalid transition handling where the
  existing test architecture permits it.
- Push the migration only after local SQL inspection and the repository build pass.

### 2. Dependency and secret review

- Inspect the version catalog and resolved dependency graph.
- Run an available dependency vulnerability scan without re-enabling Robolectric or
  other downloads prohibited by the repository guidance.
- Search tracked source and history-adjacent files for private keys, OAuth secrets,
  service-account material, and accidental local artifacts.
- Fix confirmed findings only; do not rotate public Supabase anon configuration as if it
  were a private key.

### 3. Release signing validation

- Remove the invalid release-signing fallback that points at a Gradle Kotlin script.
- Require `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` for release
  builds, failing early when any value is missing or invalid.
- Build the release variant with a test keystore supplied outside the repository.
- Confirm the release APK is signed with the expected certificate and that no keystore
  or passwords enter Git.

### 4. Emulator smoke test

- Build and install `assembleDebug -PuseSupabase=true` using the documented emulator
  scripts.
- Exercise patient, provider, and admin sign-in paths using test accounts.
- Verify profile mirroring, provider review, booking status transitions, receipt review,
  notifications, rating submission, and payout visibility.
- Capture failures with `tools/uidump.py` and logcat; do not treat a successful install
  as proof of RLS correctness.

### 5. Warning cleanup and final verification

- Replace actionable deprecated Material icons and FCM APIs where behavior is unchanged.
- Resolve or explicitly suppress the Kotlin annotation-target warnings in the Supabase
  DTOs after confirming the intended Moshi serialization target.
- Run `testDebugUnitTest`, `assembleDebug -PuseSupabase=true`, release build validation,
  Python syntax checks, and `git diff --check`.
- Update `task.md` with actual results and leave device-only checks explicitly marked if
  credentials or emulator availability prevent completion.

## Acceptance Criteria

- No client-side Supabase order write can bypass the validated lifecycle transition path.
- Release builds fail closed when signing credentials are absent or invalid.
- No private credential is present in tracked files or newly added artifacts.
- Debug Supabase build and JVM tests pass.
- Device smoke-test results are recorded with any blocked scenarios named explicitly.
