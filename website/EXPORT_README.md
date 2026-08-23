# منصة صحتك — Source Export

This archive contains the complete source code for the current Sehatak website,
including the public pages, doctors directory, admin panel, API routes, D1
migrations, and public brand assets.

## Requirements

- Node.js 22.13 or newer
- npm
- Cloudflare-compatible D1 database binding named `DB`
- Cloudflare-compatible R2 bucket binding named `BUCKET`

## Local setup

1. Run `npm ci`.
2. Copy `.dev.vars.example` to `.dev.vars`.
3. Set a new admin username, password hash, and session secret.
4. Run `npm run dev`.

Create the password hash on Linux with:

```bash
printf '%s' 'your-password' | sha256sum
```

Create a session secret with:

```bash
openssl rand -hex 32
```

## Database and uploads

- Database schema and migrations are in `db/` and `drizzle/`.
- Uploaded provider documents and payment receipts require the R2 `BUCKET` binding.
- Production database records, uploaded files, and hosted environment values are
  intentionally not included in this source archive.

## Sites deployment

The exported `.openai/hosting.json` keeps only the logical `DB` and `BUCKET`
binding names. A new Sites project will assign its own project identity. Do not
copy credentials or production secrets into source files.
