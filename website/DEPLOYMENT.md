# Deployment

The website is deployed on Vercel and connected to a Supabase Postgres database and private Storage bucket.

## Production

- Website: <https://sehatak-home-health.vercel.app>
- Admin login: <https://sehatak-home-health.vercel.app/admin/login>
- Vercel dashboard: <https://vercel.com/aemj/sehatak-home-health>
- Supabase dashboard: <https://supabase.com/dashboard/project/wolngyvenfyuaigjxajs>
- Supabase project reference: `wolngyvenfyuaigjxajs`

## Admin Access

- Username: `admin`
- Password: stored in the Windows user environment variable `SEHATAK_ADMIN_PASSWORD`

Retrieve the password from PowerShell without storing it in the repository:

```powershell
[Environment]::GetEnvironmentVariable('SEHATAK_ADMIN_PASSWORD', 'User')
```

## Deployment Changes

- Migrated Cloudflare D1 database access to Supabase Postgres.
- Migrated Cloudflare R2 uploads to a private Supabase Storage bucket named `private-files`.
- Added the complete Supabase database migration at `supabase/migrations/20260823000000_initial_schema.sql`.
- Configured sensitive Vercel environment variables for production and preview.
- Added `vercel.json` with the Next.js framework configuration.
- Updated website metadata to use the production Vercel domain.
- Updated the admin login form to use the deployed username.

## Vercel Environment Variables

The deployment uses these variables:

- `DATABASE_URL`
- `SUPABASE_URL`
- `SUPABASE_SERVICE_ROLE_KEY`
- `SUPABASE_STORAGE_BUCKET`
- `ADMIN_USERNAME`
- `ADMIN_PASSWORD_SHA256`
- `ADMIN_SESSION_SECRET`

Sensitive values are stored as Vercel secrets and are not included in this file.

## Verification

- Production homepage returned HTTP `200`.
- Public doctors API returned HTTP `200`.
- Authenticated admin login returned HTTP `200` and issued a secure session cookie.
- Authenticated notifications API returned HTTP `200`.
- Strict TypeScript checking passed with `npx tsc --noEmit`.
- Local and Vercel production builds completed successfully.
- Local and remote Supabase migration histories are synchronized.

## Current Data

The Supabase database was empty at deployment time. Doctors, bookings, provider applications, and service requests will populate as records are created through the website and admin area.
