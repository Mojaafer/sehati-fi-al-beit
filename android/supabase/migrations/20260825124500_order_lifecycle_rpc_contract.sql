-- Align the hosted lifecycle RPC with the client contract and close direct order updates.

DROP POLICY IF EXISTS orders_update_participant_or_admin ON public.orders;

CREATE OR REPLACE FUNCTION public.transition_order(
    p_order_id TEXT,
    p_target_status TEXT,
    p_cancelled_by TEXT DEFAULT '',
    p_reason TEXT DEFAULT '',
    p_receipt_image_uri TEXT DEFAULT ''
) RETURNS public.orders
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_order public.orders;
    v_is_provider BOOLEAN;
    v_is_admin BOOLEAN;
    v_allowed BOOLEAN := FALSE;
BEGIN
    IF NOT public.is_authenticated_request() THEN
        RAISE EXCEPTION 'authenticated request required';
    END IF;

    SELECT * INTO v_order FROM public.orders WHERE id = p_order_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'order not found';
    END IF;

    v_is_admin := public.is_admin();
    v_is_provider := EXISTS (
        SELECT 1 FROM public.providers
        WHERE id = v_order.provider_id AND owner_uid = auth.uid()::TEXT
    );

    IF v_is_admin THEN
        v_allowed :=
            (v_order.status = 'PAYMENT_UNDER_REVIEW' AND p_target_status IN ('PAYMENT_CONFIRMED', 'REJECTED'))
            OR (v_order.status = 'REFUND_REQUESTED' AND p_target_status IN ('CANCELLED', 'PAYMENT_CONFIRMED'));
    ELSIF v_is_provider THEN
        v_allowed :=
            (v_order.status = 'ORDER_SENT' AND p_target_status IN ('ACCEPTED_BY_PROVIDER', 'CANCELLED'))
            OR (v_order.status = 'PAYMENT_CONFIRMED' AND p_target_status = 'COMPLETED');
    ELSIF v_order.patient_uid = auth.uid()::TEXT THEN
        v_allowed :=
            (p_target_status = 'PAYMENT_UNDER_REVIEW' AND v_order.status IN ('ACCEPTED_BY_PROVIDER', 'PAYMENT_PENDING', 'REJECTED'))
            OR (p_target_status = 'REFUND_REQUESTED' AND v_order.status = 'PAYMENT_CONFIRMED')
            OR (p_target_status = 'CANCELLED' AND v_order.status IN ('ORDER_SENT', 'ACCEPTED_BY_PROVIDER', 'PAYMENT_PENDING', 'PAYMENT_UNDER_REVIEW', 'REJECTED'));
    END IF;

    IF NOT v_allowed THEN
        RAISE EXCEPTION 'order transition is not allowed for this caller or current status';
    END IF;

    IF p_target_status = 'CANCELLED' AND p_cancelled_by NOT IN ('PATIENT', 'PROVIDER', 'ADMIN') THEN
        RAISE EXCEPTION 'invalid cancellation actor';
    END IF;
    IF p_target_status = 'CANCELLED' AND NOT (
        (p_cancelled_by = 'PATIENT' AND v_order.patient_uid = auth.uid()::TEXT)
        OR (p_cancelled_by = 'PROVIDER' AND v_is_provider)
        OR (p_cancelled_by = 'ADMIN' AND v_is_admin)
    ) THEN
        RAISE EXCEPTION 'cancellation actor does not match caller';
    END IF;

    UPDATE public.orders
    SET status = p_target_status,
        cancelled_by = CASE WHEN p_target_status = 'CANCELLED' THEN p_cancelled_by ELSE cancelled_by END,
        cancellation_reason = CASE WHEN p_target_status IN ('CANCELLED', 'REFUND_REQUESTED') THEN p_reason ELSE cancellation_reason END,
        receipt_image_uri = CASE WHEN p_receipt_image_uri <> '' THEN p_receipt_image_uri ELSE receipt_image_uri END,
        receipt_url = CASE WHEN p_receipt_image_uri <> '' THEN p_receipt_image_uri ELSE receipt_url END,
        updated_at_timestamp = (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT
    WHERE id = p_order_id
    RETURNING * INTO v_order;

    RETURN v_order;
END;
$$;

REVOKE ALL ON FUNCTION public.transition_order(TEXT, TEXT, TEXT, TEXT, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.transition_order(TEXT, TEXT, TEXT, TEXT, TEXT) TO authenticated;
