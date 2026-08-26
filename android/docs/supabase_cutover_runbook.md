# Supabase Cutover Runbook

## 1. Configure Firebase third-party auth

In the hosted Supabase project settings, enable Firebase third-party authentication with:

- Firebase project ID: `sehati-home-care`
- Firebase issuer: `https://securetoken.google.com/sehati-home-care`
- Firebase audience: `sehati-home-care`

Firebase ID tokens must contain `role: authenticated`. Set this claim with a trusted Firebase Admin
operation. For an individual test account, the repository helper uses the Firebase Identity Toolkit
admin API:

```powershell
python tools/fb_admin.py set-authenticated-claim FIREBASE_UID
```

For production, set the same custom claim for every account from a controlled admin job. Do not put
a service-account key or Supabase service-role key in the Android app.

The local equivalent is recorded in `supabase/config.toml`:

```toml
[auth.third_party.firebase]
enabled = true
project_id = "sehati-home-care"
```

After changing claims, users must sign out and sign in again so Firebase issues a new ID token.

## 2. Apply the security migration

Review and apply:

```powershell
supabase db push
```

`20260823150000_supabase_cutover_security.sql` adds `users.address`, requires authenticated
Firebase-backed requests, and removes the anonymous RLS bypasses. Apply it only after the
third-party auth configuration is active, otherwise the Android app will receive 401/403 errors.

## 3. Migrate legacy data

Run the dry-run first:

```powershell
python tools/migrate_firestore_to_supabase.py
```

Review the collection counts and mapping. For the actual write, use a temporary environment
variable containing the Supabase service-role key:

```powershell
$env:SUPABASE_SERVICE_ROLE_KEY = "..."
python tools/migrate_firestore_to_supabase.py --apply
Remove-Item Env:SUPABASE_SERVICE_ROLE_KEY
```

The service-role key bypasses RLS and must never be committed or passed to the Android build.
The script is intentionally not automatic: take a Firestore export/backup, review the dry-run,
and run the write once during a maintenance window.

## 4. Verify before enabling the Android switch

Check row counts and representative records in Supabase for users, providers, orders, payouts,
ratings, and images. Confirm every migrated user has the same Firebase UID. Confirm provider
owners, order patient/provider IDs, rating order IDs, and image references are unchanged.

Build the Supabase variant:

```powershell
gradle.bat assembleDebug -PuseSupabase=true
```

Run patient, provider, and admin flows on a device. Test login, profile update, provider approval,
booking, receipt upload, status transitions, rating, payout review, and notifications.

Keep the Firestore build available until this acceptance pass is complete. Afterward, remove the
legacy Firestore writes in a separate change and retain Firestore only as an archived backup.
