# Task: Switch Database from Firebase to Supabase & Install Supabase CLI

## Project Reference
- **Supabase Project ID**: `wolngyvenfyuaigjxajs`
- **Supabase Project Name**: `sehatak-home-health`
- **Supabase Project URL**: `https://wolngyvenfyuaigjxajs.supabase.co`

## Progress Checklist

### 1. Supabase CLI Installation & Setup
- [x] Download and install Supabase CLI binary on Windows (`v2.115.0`) <!-- id: 0 -->
- [x] Verify CLI installation (`supabase --version`) <!-- id: 1 -->
- [x] Initialize local Supabase project config (`supabase init`) <!-- id: 2 -->
- [x] Authenticate CLI with Personal Access Token (`supabase login`) <!-- id: 3 -->
- [x] Link local workspace to remote project `wolngyvenfyuaigjxajs` (`supabase link`) <!-- id: 4 -->

### 2. Database Schema & Row-Level Security (PostgreSQL / Supabase)
- [x] Create complete PostgreSQL migration SQL script with tables: <!-- id: 5 -->
  - `users` (profiles, roles: PATIENT, PROVIDER, ADMIN, FCM token)
  - `providers` (categories, verification status, rating aggregates, pricing)
  - `orders` (lifecycle, immutable terms, price breakdown SDG, cancellation/refund fields)
  - `payouts` (earnings ledger, ACCRUED -> PAID transitions)
  - `ratings` (1-5 stars, single rating per order constraint)
  - `notifications` & `admin_notifications`
  - `images` (base64 documents / storage attachments)
- [x] Implement Row Level Security (RLS) policies matching all security rules from `firestore.rules` <!-- id: 6 -->
- [x] Create atomic RPC function `submit_provider_rating` for atomic aggregate recalculation <!-- id: 7 -->
- [x] Enable Realtime publication for `orders`, `providers`, `notifications`, `admin_notifications`, `payouts` <!-- id: 8 -->
- [x] Push migration to live remote database via Supabase CLI (`supabase db push`) $\rightarrow$ **Success!** <!-- id: 9 -->

### 3. Android / Kotlin Supabase Integration Layer
- [x] Add Supabase configuration & networking client (URL + Anon key configured) <!-- id: 10 -->
- [x] Implement Supabase data models and JSON serialization mapping (`SupabaseModels.kt`) <!-- id: 11 -->
- [x] Implement repository layer for Supabase: <!-- id: 12 -->
  - `SupabaseSehatiRepository`
  - `SupabaseNotificationRepository`
  - `SupabasePayoutRepository`
  - `SupabaseProviderRegistrationRepository`
  - `SupabaseRatingRepository`
  - `SupabaseStorageRepository`
  - `SupabaseUserRepository`
- [x] Wire `RepositoryFactory` for unified data layer instantiation <!-- id: 13 -->

### 4. Verification & Testing
- [x] Create and run unit test suite including Supabase DTO mappings (`testDebugUnitTest` - 100% pass) <!-- id: 14 -->
- [x] Build Debug APK (`assembleDebug`) $\rightarrow$ `app-debug.apk` (28.4 MB) <!-- id: 15 -->
- [x] Verify live remote PostgREST endpoints with curl (HTTP 200 OK) <!-- id: 16 -->
- [x] Create step-by-step migration & linking guide in [`docs/supabase_migration_guide.md`](file:///C:/Users/HP/Desktop/health/docs/supabase_migration_guide.md) <!-- id: 17 -->

## 2026-08-23: Firebase-authenticated Supabase wiring (backend switch)

### 5. Identity bridge & data-plane switch
- [x] `SupabaseAuthTokenProvider`: resolves a fresh Firebase ID token per PostgREST request
- [x] `SupabaseClient` auth chain: manual override -> Firebase ID token -> anon key fallback
- [x] `USE_SUPABASE` BuildConfig flag (`assembleDebug -PuseSupabase=true`), default false (Firestore)
- [x] `RepositoryFactory` selects Firestore/Supabase repositories from the flag
- [x] All ViewModels + `RemoteImageModel` default to `RepositoryFactory` instead of hardcoded Firestore repos
- [x] Sign-in mirrors the Firebase user into Supabase `users` (`ensureUserDocument`, best-effort)
- [x] FCM token writes mirrored into Supabase `users.fcm_token`
- [x] Role/providerId reads follow the backend flag; sign-out clears token override
- [x] `testDebugUnitTest` + `assembleDebug` pass

### 6. Remaining before Supabase cutover
- [x] Add local Firebase third-party auth configuration and document hosted Supabase setup; set claims with `tools/fb_admin.py set-authenticated-claim`
- [x] Add strict non-destructive RLS migration removing `OR auth.role() = 'anon'` escape hatches (`supabase/migrations/20260823150000_supabase_cutover_security.sql`)
- [x] Add `address` to the Supabase model/schema and switch profile reads/writes off Firestore when `USE_SUPABASE=true`
- [x] Add dry-run/apply Firestore -> Supabase migration tooling with collection counts (`tools/migrate_firestore_to_supabase.py`)
- [x] Apply the third-party auth configuration and strict RLS migration to the hosted Supabase project
- [x] Run the migration tool against a reviewed Firestore backup, then verify counts/integrity
- [ ] End-to-end device test of patient/provider/admin flows with `-PuseSupabase=true`

### 7. Security and reliability hardening
- [x] Push hosted security hardening migration (`20260825120000_security_hardening.sql`)
- [x] Remove hardcoded Firebase CLI OAuth secrets from maintenance scripts
- [x] Reject authenticated Supabase requests when no usable Firebase ID token is available
- [x] Fix notification `markAllRead()` filtering to use `recipient_uid`
- [x] Add bounded polling backoff for Supabase orders, providers, notifications, ratings, and payouts
- [x] Gate the shared admin notification inbox by the resolved app role
- [x] Standardize login primary actions on `SehatiPrimaryButton`
- [x] Verify `testDebugUnitTest`, `assembleDebug -PuseSupabase=true`, and `git diff --check`

### 8. Remaining hardening execution
- [x] Save detailed execution plan in `docs/remaining-hardening-execution-plan.md`
- [x] Add and push server-side `transition_order` lifecycle RPC
- [x] Remove direct Supabase order update policy so lifecycle writes use the RPC
- [x] Remove the remaining hardcoded Firebase CLI secret from `tools/test_rules.py`
- [x] Make release signing fail closed when external keystore credentials are absent
- [x] Validate a signed release APK with a temporary keystore outside the repository
- [x] Replace actionable deprecated directional Material icons
- [ ] Run emulator smoke test; blocked because `D:\AndroidSdkLite\emulator\emulator.exe` is not installed

### 9. Final release readiness
- [x] Save final release-readiness plan in `docs/final-release-readiness-plan.md`
- [x] Add Dependabot checks for Gradle and Python dependencies
- [x] Add scheduled OSV vulnerability scanning for pushes and pull requests
- [x] Replace safe deprecated directional Material icons
- [x] Document FCM token API migration as a contract change, not a cosmetic warning
- [ ] Rotate the historical Firebase OAuth client secret outside the repository
- [ ] Configure production release keystore credentials
- [ ] Install/connect an Android device and complete Supabase smoke tests

### 10. Design-system sweep (continued)
- [x] Move `SehatiPrimaryButton` onto semantic `primary`/`onPrimary` tokens per the approved spec
- [x] Replace raw status hex in PaymentReview, ProviderDashboard, and UploadReceipt with `StatusColors`
- [x] Route BookingConfirm and UploadReceipt CTAs through `SehatiPrimaryButton`
- [x] Move BookingConfirm and UploadReceipt inputs onto `sehatiTextFieldColors()`
- [x] Add `bodySmallReadable` / `labelMediumReadable` type tokens and remove raw `lineHeight` from screens
- [x] Replace remaining raw `fontSize` in AdminDashboard refund actions with `labelMedium`
- [x] Give BottomNavBar items `Role.Tab` selection semantics and token-based badge colors
- [x] Migrate remaining `Color.White` content colors on branded headers to `onPrimary`/`onSecondary`/`onTertiary`
- [x] Add `RoleColors` tokens and remove the raw emerald/violet role badges from `SehatiTopBar`
- [x] Give the login auth-mode tabs `Role.Tab` + selected semantics and theme-driven colors
- [x] Keep `ImageViewerDialog` on literal white content: it sits on a fixed black lightbox scrim, not a themed surface
- [ ] Confirm the swept screens visually on a device (blocked with the emulator)
