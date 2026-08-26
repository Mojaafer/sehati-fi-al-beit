-- Supabase cutover hardening for Firebase-authenticated clients.

ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS address TEXT NOT NULL DEFAULT '';

CREATE OR REPLACE FUNCTION public.is_authenticated_request()
RETURNS BOOLEAN
LANGUAGE SQL
STABLE
AS $$
    SELECT auth.role() = 'authenticated'
        AND auth.uid() IS NOT NULL;
$$;

DROP POLICY IF EXISTS "Users can read own profile or admin reads all" ON public.users;
DROP POLICY IF EXISTS "Users can insert own profile as PATIENT" ON public.users;
DROP POLICY IF EXISTS "Users can update own profile except role; admin updates all" ON public.users;
DROP POLICY IF EXISTS "Active providers are readable by all authenticated / public" ON public.providers;
DROP POLICY IF EXISTS "Provider creates self-registration in PENDING_REVIEW" ON public.providers;
DROP POLICY IF EXISTS "Provider updates own record, admin updates all" ON public.providers;
DROP POLICY IF EXISTS "Orders read by owner patient, assigned provider, or admin" ON public.orders;
DROP POLICY IF EXISTS "Patient creates orders" ON public.orders;
DROP POLICY IF EXISTS "Order status updates by parties or admin" ON public.orders;
DROP POLICY IF EXISTS "Payouts read by provider or admin" ON public.payouts;
DROP POLICY IF EXISTS "Payouts created by provider on visit completion" ON public.payouts;
DROP POLICY IF EXISTS "Admin updates payout status" ON public.payouts;
DROP POLICY IF EXISTS "Ratings are readable by all" ON public.ratings;
DROP POLICY IF EXISTS "Ratings inserted by order patient" ON public.ratings;
DROP POLICY IF EXISTS "Notifications read by recipient or admin" ON public.notifications;
DROP POLICY IF EXISTS "Notifications inserted by any user" ON public.notifications;
DROP POLICY IF EXISTS "Notifications updated/deleted by recipient or admin" ON public.notifications;
DROP POLICY IF EXISTS "Admin notifications read/managed by admin" ON public.admin_notifications;
DROP POLICY IF EXISTS "Admin notifications created by any user" ON public.admin_notifications;
DROP POLICY IF EXISTS "Images read by owner or admin" ON public.images;
DROP POLICY IF EXISTS "Images inserted by owner" ON public.images;

CREATE POLICY users_select_authenticated ON public.users FOR SELECT
USING (public.is_authenticated_request() AND (uid = auth.uid()::TEXT OR public.is_admin()));

CREATE POLICY users_insert_self ON public.users FOR INSERT
WITH CHECK (public.is_authenticated_request() AND uid = auth.uid()::TEXT AND role = 'PATIENT');

CREATE POLICY users_update_self_or_admin ON public.users FOR UPDATE
USING (public.is_authenticated_request() AND (uid = auth.uid()::TEXT OR public.is_admin()))
WITH CHECK (
    public.is_authenticated_request()
    AND (
        public.is_admin()
        OR (uid = auth.uid()::TEXT AND role = (SELECT role FROM public.users WHERE uid = auth.uid()::TEXT))
    )
);

CREATE POLICY providers_select_authenticated ON public.providers FOR SELECT
USING (public.is_authenticated_request());

CREATE POLICY providers_insert_owner_or_admin ON public.providers FOR INSERT
WITH CHECK (public.is_authenticated_request() AND (owner_uid = auth.uid()::TEXT OR public.is_admin()));

CREATE POLICY providers_update_owner_or_admin ON public.providers FOR UPDATE
USING (public.is_authenticated_request() AND (owner_uid = auth.uid()::TEXT OR public.is_admin()));

CREATE POLICY orders_select_participant_or_admin ON public.orders FOR SELECT
USING (
    public.is_authenticated_request()
    AND (
        patient_uid = auth.uid()::TEXT
        OR provider_id IN (SELECT id FROM public.providers WHERE owner_uid = auth.uid()::TEXT)
        OR public.is_admin()
    )
);

CREATE POLICY orders_insert_patient_or_admin ON public.orders FOR INSERT
WITH CHECK (public.is_authenticated_request() AND (patient_uid = auth.uid()::TEXT OR public.is_admin()));

CREATE POLICY orders_update_participant_or_admin ON public.orders FOR UPDATE
USING (
    public.is_authenticated_request()
    AND (
        patient_uid = auth.uid()::TEXT
        OR provider_id IN (SELECT id FROM public.providers WHERE owner_uid = auth.uid()::TEXT)
        OR public.is_admin()
    )
);

CREATE POLICY payouts_select_provider_or_admin ON public.payouts FOR SELECT
USING (public.is_authenticated_request() AND (provider_id IN (SELECT id FROM public.providers WHERE owner_uid = auth.uid()::TEXT) OR public.is_admin()));

CREATE POLICY payouts_insert_provider_or_admin ON public.payouts FOR INSERT
WITH CHECK (public.is_authenticated_request() AND (provider_id IN (SELECT id FROM public.providers WHERE owner_uid = auth.uid()::TEXT) OR public.is_admin()));

CREATE POLICY payouts_update_admin ON public.payouts FOR UPDATE
USING (public.is_authenticated_request() AND public.is_admin());

CREATE POLICY ratings_select_authenticated ON public.ratings FOR SELECT
USING (public.is_authenticated_request());

CREATE POLICY ratings_insert_patient_or_admin ON public.ratings FOR INSERT
WITH CHECK (public.is_authenticated_request() AND (patient_uid = auth.uid()::TEXT OR public.is_admin()));

CREATE POLICY notifications_select_recipient_or_admin ON public.notifications FOR SELECT
USING (public.is_authenticated_request() AND (recipient_uid = auth.uid()::TEXT OR public.is_admin()));

CREATE POLICY notifications_insert_authenticated ON public.notifications FOR INSERT
WITH CHECK (public.is_authenticated_request());

CREATE POLICY notifications_manage_recipient_or_admin ON public.notifications FOR ALL
USING (public.is_authenticated_request() AND (recipient_uid = auth.uid()::TEXT OR public.is_admin()))
WITH CHECK (public.is_authenticated_request() AND (recipient_uid = auth.uid()::TEXT OR public.is_admin()));

CREATE POLICY admin_notifications_manage_admin ON public.admin_notifications FOR ALL
USING (public.is_authenticated_request() AND public.is_admin())
WITH CHECK (public.is_authenticated_request() AND public.is_admin());

CREATE POLICY admin_notifications_insert_authenticated ON public.admin_notifications FOR INSERT
WITH CHECK (public.is_authenticated_request());

CREATE POLICY images_select_owner_or_admin ON public.images FOR SELECT
USING (public.is_authenticated_request() AND (owner_uid = auth.uid()::TEXT OR public.is_admin()));

CREATE POLICY images_insert_owner_or_admin ON public.images FOR INSERT
WITH CHECK (public.is_authenticated_request() AND (owner_uid = auth.uid()::TEXT OR public.is_admin()));
