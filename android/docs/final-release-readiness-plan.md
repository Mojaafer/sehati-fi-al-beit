# Final Release Readiness Plan

## Completed in this pass

- Add hosted order lifecycle enforcement through `transition_order`.
- Remove direct Supabase order-update access.
- Require external release signing credentials.
- Remove the remaining hardcoded Firebase CLI OAuth secret.
- Validate debug and release builds.
- Keep migration files immutable after they are pushed.

## Remaining external actions

1. Configure the production release keystore through `KEYSTORE_PATH`, `STORE_PASSWORD`,
   `KEY_ALIAS`, and `KEY_PASSWORD`.
2. Rotate the historical Firebase OAuth client secret in Google Cloud/Firebase. Current
   source files now read it from `FIREBASE_CLI_CLIENT_SECRET`.
3. Install the configured `SehatiLite` emulator or connect an Android device, then run
   patient/provider/admin Supabase smoke tests.
4. Confirm the migration files in `supabase/migrations/` are never edited after being
   applied. Add a new timestamped migration for every hosted change.

## Warning policy

- Keep `onNewToken()` and `FirebaseMessaging.getToken()` until the app’s push contract is
  migrated from FCM registration tokens to Firebase Installation IDs. The current warning
  is a planned API migration, not a safe cosmetic replacement.
- Resolve Moshi annotation-target warnings only after confirming generated adapter output;
  use explicit `@param:` targets if the compiler upgrade requires it.

## Acceptance criteria

- Debug Supabase tests/build pass.
- Release build succeeds only with explicit signing credentials.
- Dependency advisories are checked automatically on pull requests and weekly.
- Hosted and local migration versions match.
- Device results are recorded, or the missing emulator/device is explicitly documented.
