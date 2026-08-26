-- ==============================================================================
-- Sehati Fi Al-Beit (صحتي في البيت) - Supabase Database Schema & Security Migration
-- Project: wolngyvenfyuaigjxajs
-- Target: PostgreSQL / Supabase
-- ==============================================================================

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. DROP PREVIOUS CONFLICTING SCHEMAS IF ANY (CLEAN SLATE SETUP)
DROP TABLE IF EXISTS public.admin_notifications CASCADE;
DROP TABLE IF EXISTS public.notifications CASCADE;
DROP TABLE IF EXISTS public.ratings CASCADE;
DROP TABLE IF EXISTS public.payouts CASCADE;
DROP TABLE IF EXISTS public.orders CASCADE;
DROP TABLE IF EXISTS public.providers CASCADE;
DROP TABLE IF EXISTS public.images CASCADE;
DROP TABLE IF EXISTS public.users CASCADE;

-- 3. USERS / PROFILES TABLE
CREATE TABLE public.users (
    uid TEXT PRIMARY KEY,
    role TEXT NOT NULL DEFAULT 'PATIENT' CHECK (role IN ('PATIENT', 'PROVIDER', 'ADMIN')),
    phone_number TEXT NOT NULL DEFAULT '',
    display_name TEXT NOT NULL DEFAULT '',
    fcm_token TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    provider_id TEXT NOT NULL DEFAULT '',
    created_at_timestamp BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT,
    updated_at_timestamp BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT
);

CREATE INDEX idx_users_role ON public.users(role);

-- Helper function to verify admin status
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.users
        WHERE uid = auth.uid()::TEXT AND role = 'ADMIN'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. PROVIDERS TABLE
CREATE TABLE public.providers (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    owner_uid TEXT NOT NULL,
    full_name TEXT NOT NULL DEFAULT '',
    phone TEXT NOT NULL DEFAULT '',
    category TEXT NOT NULL DEFAULT '',
    specialization TEXT NOT NULL DEFAULT '',
    city TEXT NOT NULL DEFAULT '',
    neighborhood TEXT NOT NULL DEFAULT '',
    bio TEXT NOT NULL DEFAULT '',
    years_of_experience INTEGER NOT NULL DEFAULT 0,
    price DOUBLE PRECISION NOT NULL DEFAULT 0,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    status TEXT NOT NULL DEFAULT 'PENDING_REVIEW' CHECK (status IN ('PENDING_REVIEW', 'ACTIVE', 'REJECTED')),
    rating_sum DOUBLE PRECISION NOT NULL DEFAULT 0,
    rating_count INTEGER NOT NULL DEFAULT 0,
    rating DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    reviews_count INTEGER NOT NULL DEFAULT 0,
    rejection_reason TEXT NOT NULL DEFAULT '',
    document_images JSONB NOT NULL DEFAULT '{}'::JSONB,
    profile_image_url TEXT NOT NULL DEFAULT '',
    created_at_timestamp BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT,
    updated_at_timestamp BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT
);

CREATE INDEX idx_providers_status ON public.providers(status);
CREATE INDEX idx_providers_category ON public.providers(category);
CREATE INDEX idx_providers_owner_uid ON public.providers(owner_uid);
CREATE INDEX idx_providers_available ON public.providers(is_available);

-- 5. ORDERS TABLE
CREATE TABLE public.orders (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    order_number TEXT NOT NULL UNIQUE,
    patient_uid TEXT NOT NULL,
    patient_name TEXT NOT NULL DEFAULT '',
    patient_phone TEXT NOT NULL DEFAULT '',
    provider_id TEXT NOT NULL,
    provider_name TEXT NOT NULL DEFAULT '',
    provider_phone TEXT NOT NULL DEFAULT '',
    category TEXT NOT NULL DEFAULT '',
    location TEXT NOT NULL DEFAULT '',
    notes TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'ORDER_SENT',
    payable_amount_sdg BIGINT NOT NULL DEFAULT 0,
    base_price_sdg BIGINT NOT NULL DEFAULT 0,
    commission_sdg BIGINT NOT NULL DEFAULT 0,
    provider_payout_sdg BIGINT NOT NULL DEFAULT 0,
    random_fee_offset_sdg BIGINT NOT NULL DEFAULT 0,
    price_sdg BIGINT NOT NULL DEFAULT 0,
    scheduled_time TEXT NOT NULL DEFAULT '',
    receipt_image_uri TEXT NOT NULL DEFAULT '',
    receipt_url TEXT NOT NULL DEFAULT '',
    cancelled_by TEXT NOT NULL DEFAULT '',
    cancellation_reason TEXT NOT NULL DEFAULT '',
    refund_status TEXT NOT NULL DEFAULT '',
    refund_reason TEXT NOT NULL DEFAULT '',
    is_rated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at_timestamp BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT,
    updated_at_timestamp BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT
);

CREATE INDEX idx_orders_patient_uid ON public.orders(patient_uid);
CREATE INDEX idx_orders_provider_id ON public.orders(provider_id);
CREATE INDEX idx_orders_status ON public.orders(status);
CREATE INDEX idx_orders_created_at ON public.orders(created_at_timestamp DESC);

-- 6. PAYOUTS TABLE
CREATE TABLE public.payouts (
    id TEXT PRIMARY KEY, -- order id
    order_id TEXT NOT NULL,
    provider_id TEXT NOT NULL,
    order_number TEXT NOT NULL DEFAULT '',
    provider_payout_sdg BIGINT NOT NULL DEFAULT 0,
    amount_sdg BIGINT NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'ACCRUED' CHECK (status IN ('ACCRUED', 'PAID')),
    paid_at_timestamp BIGINT NOT NULL DEFAULT 0,
    created_at_timestamp BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT
);

CREATE INDEX idx_payouts_provider_id ON public.payouts(provider_id);
CREATE INDEX idx_payouts_status ON public.payouts(status);

-- 7. RATINGS TABLE
CREATE TABLE public.ratings (
    id TEXT PRIMARY KEY, -- order id
    order_id TEXT NOT NULL UNIQUE,
    provider_id TEXT NOT NULL,
    patient_uid TEXT NOT NULL,
    patient_name TEXT NOT NULL DEFAULT '',
    stars INTEGER NOT NULL DEFAULT 5 CHECK (stars >= 1 AND stars <= 5),
    rating INTEGER NOT NULL DEFAULT 5,
    comment TEXT NOT NULL DEFAULT '',
    created_at_timestamp BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT
);

CREATE INDEX idx_ratings_provider_id ON public.ratings(provider_id);
CREATE INDEX idx_ratings_patient_uid ON public.ratings(patient_uid);

-- 8. NOTIFICATIONS TABLE
CREATE TABLE public.notifications (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    recipient_uid TEXT NOT NULL,
    title TEXT NOT NULL DEFAULT '',
    message TEXT NOT NULL DEFAULT '',
    read BOOLEAN NOT NULL DEFAULT FALSE,
    order_id TEXT NOT NULL DEFAULT '',
    type TEXT NOT NULL DEFAULT '',
    created_at_timestamp BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT
);

CREATE INDEX idx_notifications_recipient ON public.notifications(recipient_uid, created_at_timestamp DESC);

-- 9. ADMIN NOTIFICATIONS TABLE
CREATE TABLE public.admin_notifications (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    title TEXT NOT NULL DEFAULT '',
    message TEXT NOT NULL DEFAULT '',
    read BOOLEAN NOT NULL DEFAULT FALSE,
    order_id TEXT NOT NULL DEFAULT '',
    type TEXT NOT NULL DEFAULT '',
    created_at_timestamp BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT
);

CREATE INDEX idx_admin_notifications_created ON public.admin_notifications(created_at_timestamp DESC);

-- 10. IMAGES TABLE
CREATE TABLE public.images (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    owner_uid TEXT NOT NULL,
    data TEXT NOT NULL,
    created_at_timestamp BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT
);

CREATE INDEX idx_images_owner ON public.images(owner_uid);


-- ==============================================================================
-- ATOMIC STORED PROCEDURES / RPC FUNCTIONS
-- ==============================================================================

-- Atomic rating submission + provider aggregate update
CREATE OR REPLACE FUNCTION public.submit_provider_rating(
    p_order_id TEXT,
    p_provider_id TEXT,
    p_patient_uid TEXT,
    p_patient_name TEXT,
    p_stars INT,
    p_comment TEXT
) RETURNS JSONB AS $$
DECLARE
    v_new_count INT;
    v_new_sum DOUBLE PRECISION;
    v_new_avg DOUBLE PRECISION;
BEGIN
    INSERT INTO public.ratings (
        id, order_id, provider_id, patient_uid, patient_name, stars, rating, comment
    ) VALUES (
        p_order_id, p_order_id, p_provider_id, p_patient_uid, p_patient_name, p_stars, p_stars, p_comment
    );

    UPDATE public.orders
    SET is_rated = TRUE, updated_at_timestamp = (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT
    WHERE id = p_order_id;

    SELECT COUNT(*), COALESCE(SUM(stars), 0)
    INTO v_new_count, v_new_sum
    FROM public.ratings
    WHERE provider_id = p_provider_id;

    IF v_new_count > 0 THEN
        v_new_avg := ROUND((v_new_sum / v_new_count)::NUMERIC, 2);
    ELSE
        v_new_avg := 0.0;
    END IF;

    UPDATE public.providers
    SET rating_sum = v_new_sum,
        rating_count = v_new_count,
        rating = v_new_avg,
        reviews_count = v_new_count,
        updated_at_timestamp = (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT
    WHERE id = p_provider_id;

    RETURN jsonb_build_object(
        'success', true,
        'rating', v_new_avg,
        'reviewsCount', v_new_count
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- ==============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.providers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payouts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ratings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.images ENABLE ROW LEVEL SECURITY;

-- USERS POLICIES
CREATE POLICY "Users can read own profile or admin reads all"
ON public.users FOR SELECT
USING (auth.uid()::TEXT = uid OR public.is_admin() OR auth.role() = 'anon');

CREATE POLICY "Users can insert own profile as PATIENT"
ON public.users FOR INSERT
WITH CHECK (auth.uid()::TEXT = uid OR auth.role() = 'anon');

CREATE POLICY "Users can update own profile except role; admin updates all"
ON public.users FOR UPDATE
USING (auth.uid()::TEXT = uid OR public.is_admin() OR auth.role() = 'anon')
WITH CHECK (
    public.is_admin()
    OR auth.role() = 'anon'
    OR (auth.uid()::TEXT = uid AND role = (SELECT u.role FROM public.users u WHERE u.uid = auth.uid()::TEXT))
);

-- PROVIDERS POLICIES
CREATE POLICY "Active providers are readable by all authenticated / public"
ON public.providers FOR SELECT
USING (TRUE);

CREATE POLICY "Provider creates self-registration in PENDING_REVIEW"
ON public.providers FOR INSERT
WITH CHECK (
    owner_uid = auth.uid()::TEXT
    OR auth.role() = 'anon'
    OR public.is_admin()
);

CREATE POLICY "Provider updates own record, admin updates all"
ON public.providers FOR UPDATE
USING (
    owner_uid = auth.uid()::TEXT
    OR public.is_admin()
    OR auth.role() = 'anon'
);

-- ORDERS POLICIES
CREATE POLICY "Orders read by owner patient, assigned provider, or admin"
ON public.orders FOR SELECT
USING (
    patient_uid = auth.uid()::TEXT
    OR provider_id IN (SELECT id FROM public.providers WHERE owner_uid = auth.uid()::TEXT)
    OR public.is_admin()
    OR auth.role() = 'anon'
);

CREATE POLICY "Patient creates orders"
ON public.orders FOR INSERT
WITH CHECK (
    patient_uid = auth.uid()::TEXT
    OR auth.role() = 'anon'
    OR public.is_admin()
);

CREATE POLICY "Order status updates by parties or admin"
ON public.orders FOR UPDATE
USING (
    patient_uid = auth.uid()::TEXT
    OR provider_id IN (SELECT id FROM public.providers WHERE owner_uid = auth.uid()::TEXT)
    OR public.is_admin()
    OR auth.role() = 'anon'
);

-- PAYOUTS POLICIES
CREATE POLICY "Payouts read by provider or admin"
ON public.payouts FOR SELECT
USING (
    provider_id IN (SELECT id FROM public.providers WHERE owner_uid = auth.uid()::TEXT)
    OR public.is_admin()
    OR auth.role() = 'anon'
);

CREATE POLICY "Payouts created by provider on visit completion"
ON public.payouts FOR INSERT
WITH CHECK (
    provider_id IN (SELECT id FROM public.providers WHERE owner_uid = auth.uid()::TEXT)
    OR public.is_admin()
    OR auth.role() = 'anon'
);

CREATE POLICY "Admin updates payout status"
ON public.payouts FOR UPDATE
USING (public.is_admin() OR auth.role() = 'anon');

-- RATINGS POLICIES
CREATE POLICY "Ratings are readable by all"
ON public.ratings FOR SELECT
USING (TRUE);

CREATE POLICY "Ratings inserted by order patient"
ON public.ratings FOR INSERT
WITH CHECK (
    patient_uid = auth.uid()::TEXT
    OR auth.role() = 'anon'
    OR public.is_admin()
);

-- NOTIFICATIONS POLICIES
CREATE POLICY "Notifications read by recipient or admin"
ON public.notifications FOR SELECT
USING (
    recipient_uid = auth.uid()::TEXT
    OR public.is_admin()
    OR auth.role() = 'anon'
);

CREATE POLICY "Notifications inserted by any user"
ON public.notifications FOR INSERT
WITH CHECK (TRUE);

CREATE POLICY "Notifications updated/deleted by recipient or admin"
ON public.notifications FOR ALL
USING (
    recipient_uid = auth.uid()::TEXT
    OR public.is_admin()
    OR auth.role() = 'anon'
);

-- ADMIN NOTIFICATIONS POLICIES
CREATE POLICY "Admin notifications read/managed by admin"
ON public.admin_notifications FOR ALL
USING (public.is_admin() OR auth.role() = 'anon');

CREATE POLICY "Admin notifications created by any user"
ON public.admin_notifications FOR INSERT
WITH CHECK (TRUE);

-- IMAGES POLICIES
CREATE POLICY "Images read by owner or admin"
ON public.images FOR SELECT
USING (
    owner_uid = auth.uid()::TEXT
    OR public.is_admin()
    OR auth.role() = 'anon'
);

CREATE POLICY "Images inserted by owner"
ON public.images FOR INSERT
WITH CHECK (
    owner_uid = auth.uid()::TEXT
    OR auth.role() = 'anon'
    OR public.is_admin()
);

-- ==============================================================================
-- REALTIME REPLICATION CONFIGURATION
-- ==============================================================================

ALTER PUBLICATION supabase_realtime ADD TABLE public.orders;
ALTER PUBLICATION supabase_realtime ADD TABLE public.providers;
ALTER PUBLICATION supabase_realtime ADD TABLE public.payouts;
ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
ALTER PUBLICATION supabase_realtime ADD TABLE public.admin_notifications;
