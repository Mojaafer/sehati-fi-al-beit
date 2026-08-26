# Supabase Database Migration & Setup Guide (دليل إعداد وقاعدة بيانات Supabase)

## Project Details
- **Project ID**: `wolngyvenfyuaigjxajs`
- **Dashboard URL**: [https://supabase.com/dashboard/project/wolngyvenfyuaigjxajs](https://supabase.com/dashboard/project/wolngyvenfyuaigjxajs)
- **API URL**: `https://wolngyvenfyuaigjxajs.supabase.co`

---

## 1. Supabase CLI Installation & Commands

Supabase CLI is installed on this system (`supabase --version` returns `2.115.0`).

### Linking to Your Remote Supabase Project:
```powershell
supabase link --project-ref wolngyvenfyuaigjxajs
```
*(If prompted for your database password or access token, enter the credentials from your Supabase project settings).*

### Pushing Migrations to Remote PostgreSQL:
```powershell
supabase db push
```

---

## 2. Direct SQL Execution (Via Dashboard SQL Editor)

Alternatively, you can apply the entire database schema, security policies (RLS), and atomic stored procedures in one step:
1. Open the [Supabase SQL Editor](https://supabase.com/dashboard/project/wolngyvenfyuaigjxajs/sql/new)
2. Copy and paste the contents of [`supabase/migrations/20260823000000_sehati_schema.sql`](file:///C:/Users/HP/Desktop/health/supabase/migrations/20260823000000_sehati_schema.sql)
3. Click **Run**.

---

## 3. Database Schema Overview

The database contains 8 core tables with Row Level Security (RLS) policies matching 100% of Firestore security rules:

| Table | Description | Realtime Enabled |
|---|---|---|
| `users` | User profiles, roles (`PATIENT`, `PROVIDER`, `ADMIN`), FCM push tokens | No |
| `providers` | Healthcare providers catalogue, status (`PENDING_REVIEW`, `ACTIVE`, `REJECTED`), ratings, pricing | **Yes** |
| `orders` | Home-care visit bookings, status transitions, immutable fee breakdown (`SDG`) | **Yes** |
| `payouts` | Provider earnings ledger (`ACCRUED` -> `PAID`) | **Yes** |
| `ratings` | Patient reviews and 1-5 star ratings with atomic provider rating aggregate recalculation | No |
| `notifications` | User inbox notifications | **Yes** |
| `admin_notifications` | Shared admin inbox notifications | **Yes** |
| `images` | Base64-encoded receipt & document uploads | No |

---

## 4. Android App Configuration

The Android application is now configured with the Supabase networking layer:
- **`SupabaseConfig`**: Configures project URL and anon key (`BuildConfig.SUPABASE_URL`, `BuildConfig.SUPABASE_ANON_KEY`).
- **`SupabaseApiService`**: Retrofit PostgREST service for native Android requests.
- **`SupabaseSehatiRepository`**: Complete repository implementation for providers and orders.
- **`SupabaseNotificationRepository`**: Implements user & admin notification streams.
- **`SupabasePayoutRepository`**: Implements provider earnings & admin payout management.
- **`SupabaseProviderRegistrationRepository`**: Handles provider onboarding & admin approval.
- **`SupabaseRatingRepository`**: Handles reviews and atomic RPC rating updates.
- **`SupabaseStorageRepository`**: Handles receipt and document uploads with image compression.
