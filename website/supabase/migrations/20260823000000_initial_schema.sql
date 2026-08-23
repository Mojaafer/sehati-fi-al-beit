create table public.provider_applications (
  id serial primary key,
  profession text not null check (profession in ('doctor', 'lab', 'physio', 'nurse')),
  full_name text not null,
  phone text not null,
  specialty text,
  experience text,
  city text not null,
  workplace text,
  availability text,
  status text not null default 'new' check (status in ('new', 'under_review', 'needs_documents', 'verified', 'rejected')),
  admin_note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.provider_documents (
  id serial primary key,
  application_id integer not null references public.provider_applications(id),
  document_type text not null check (document_type in ('cv', 'qualification', 'license', 'identity')),
  file_name text not null,
  mime_type text not null,
  object_key text not null,
  review_status text not null default 'pending' check (review_status in ('pending', 'accepted', 'rejected')),
  created_at timestamptz not null default now()
);

create table public.doctors (
  id serial primary key,
  full_name text not null,
  specialty text not null,
  clinic_location text not null,
  city text not null,
  booking_cost integer not null,
  currency text not null default 'SDG',
  availability text not null default '',
  clinic_days text not null default '[]',
  clinic_start_time text not null default '10:00',
  clinic_end_time text not null default '14:00',
  slot_duration_minutes integer not null default 30,
  phone text not null,
  supervisor_name text,
  supervisor_phone text,
  payment_instructions text not null,
  payment_instructions_visible integer not null default 1,
  status text not null default 'active' check (status in ('active', 'inactive')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.doctor_bookings (
  id serial primary key,
  booking_code text not null unique,
  doctor_id integer not null references public.doctors(id),
  patient_name text not null,
  patient_phone text not null,
  requested_date text not null,
  requested_time text not null default 'first_come',
  patient_note text,
  whatsapp_opt_in integer not null default 0,
  status text not null default 'pending_payment' check (status in ('pending_payment', 'receipt_submitted', 'payment_confirmed', 'payment_rejected', 'confirmed', 'cancelled')),
  receipt_file_name text,
  receipt_mime_type text,
  receipt_object_key text,
  admin_note text,
  reviewed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.service_requests (
  id serial primary key,
  request_code text not null unique,
  service_type text not null check (service_type in ('lab', 'nurse', 'physio')),
  patient_name text not null,
  patient_phone text not null,
  requested_date text not null,
  requested_time text not null,
  city text not null,
  neighborhood text,
  location_note text,
  service_details text,
  status text not null default 'pending' check (status in ('pending', 'accepted', 'rejected', 'cancelled')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.admin_notifications (
  id serial primary key,
  kind text not null check (kind = 'home_care_request'),
  service_request_id integer references public.service_requests(id),
  title text not null,
  body text not null,
  is_read integer not null default 0,
  created_at timestamptz not null default now(),
  read_at timestamptz
);

create table public.admin_security (
  key text primary key,
  failed_login_attempts integer not null default 0,
  locked_until timestamptz,
  updated_at timestamptz not null default now()
);

alter table public.provider_applications enable row level security;
alter table public.provider_documents enable row level security;
alter table public.doctors enable row level security;
alter table public.doctor_bookings enable row level security;
alter table public.service_requests enable row level security;
alter table public.admin_notifications enable row level security;
alter table public.admin_security enable row level security;

insert into storage.buckets (id, name, public, file_size_limit)
values ('private-files', 'private-files', false, 5242880)
on conflict (id) do update set public = false, file_size_limit = excluded.file_size_limit;
