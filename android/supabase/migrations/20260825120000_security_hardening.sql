-- Security hardening for client-facing Supabase operations.

ALTER FUNCTION public.is_admin() SET search_path = public;
ALTER FUNCTION public.submit_provider_rating(TEXT, TEXT, TEXT, TEXT, INT, TEXT) SET search_path = public;

REVOKE EXECUTE ON FUNCTION public.is_admin() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.submit_provider_rating(TEXT, TEXT, TEXT, TEXT, INT, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.is_admin() TO authenticated;
GRANT EXECUTE ON FUNCTION public.submit_provider_rating(TEXT, TEXT, TEXT, TEXT, INT, TEXT) TO authenticated;

CREATE OR REPLACE FUNCTION public.submit_provider_rating(
    p_order_id TEXT,
    p_provider_id TEXT,
    p_patient_uid TEXT,
    p_patient_name TEXT,
    p_stars INT,
    p_comment TEXT
) RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order public.orders;
    v_new_count INT;
    v_new_sum DOUBLE PRECISION;
    v_new_avg DOUBLE PRECISION;
BEGIN
    IF auth.uid() IS NULL OR auth.uid()::TEXT <> p_patient_uid THEN
        RAISE EXCEPTION 'rating caller does not own patient identity';
    END IF;
    SELECT * INTO v_order FROM public.orders WHERE id = p_order_id FOR UPDATE;
    IF NOT FOUND OR v_order.patient_uid <> auth.uid()::TEXT OR v_order.provider_id <> p_provider_id THEN
        RAISE EXCEPTION 'rating caller does not own order';
    END IF;
    IF v_order.status <> 'COMPLETED' OR v_order.is_rated THEN
        RAISE EXCEPTION 'order is not eligible for rating';
    END IF;
    IF p_stars < 1 OR p_stars > 5 THEN
        RAISE EXCEPTION 'rating must be between 1 and 5';
    END IF;
    INSERT INTO public.ratings (id, order_id, provider_id, patient_uid, patient_name, stars, rating, comment)
    VALUES (p_order_id, p_order_id, p_provider_id, auth.uid()::TEXT, p_patient_name, p_stars, p_stars, p_comment);
    UPDATE public.orders SET is_rated = TRUE, updated_at_timestamp = (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT WHERE id = p_order_id;
    SELECT COUNT(*), COALESCE(SUM(stars), 0) INTO v_new_count, v_new_sum FROM public.ratings WHERE provider_id = p_provider_id;
    v_new_avg := CASE WHEN v_new_count > 0 THEN ROUND((v_new_sum / v_new_count)::NUMERIC, 2) ELSE 0.0 END;
    UPDATE public.providers SET rating_sum = v_new_sum, rating_count = v_new_count, rating = v_new_avg, reviews_count = v_new_count, updated_at_timestamp = (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT WHERE id = p_provider_id;
    RETURN jsonb_build_object('success', TRUE, 'rating', v_new_avg, 'reviewsCount', v_new_count);
END;
$$;

DROP POLICY IF EXISTS admin_notifications_insert_authenticated ON public.admin_notifications;
CREATE POLICY admin_notifications_insert_admin ON public.admin_notifications FOR INSERT
WITH CHECK (public.is_authenticated_request() AND public.is_admin());

DROP POLICY IF EXISTS notifications_insert_authenticated ON public.notifications;
CREATE POLICY notifications_insert_participant_or_admin ON public.notifications FOR INSERT
WITH CHECK (
    public.is_authenticated_request()
    AND (
        recipient_uid = auth.uid()::TEXT
        OR public.is_admin()
        OR EXISTS (
            SELECT 1 FROM public.orders o
            WHERE o.id = order_id
              AND (o.patient_uid = auth.uid()::TEXT OR o.provider_id IN (SELECT p.id FROM public.providers p WHERE p.owner_uid = auth.uid()::TEXT))
        )
    )
);
