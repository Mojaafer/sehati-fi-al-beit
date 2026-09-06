/**
 * Bounds admin list endpoints so a growing table cannot turn into an ever-larger response.
 *
 * Every one of these lists previously did an unbounded `select` and serialised the whole table
 * into JSON. That is fine at today's row counts and steadily worse forever after, and a Cloudflare
 * Worker has a hard memory ceiling to hit on the way.
 *
 * The default is deliberately far above current volumes, so this is not a visible change for the
 * existing admin screens. When a page *is* truncated the response carries `hasMore: true` and the
 * endpoint logs it, so the day it starts mattering is not a silent one.
 */
export const DEFAULT_PAGE_SIZE = 500;
export const MAX_PAGE_SIZE = 1000;

export type PageParams = {
  /** Rows to return. */
  limit: number;
  /** Rows to skip. */
  offset: number;
  /** Pass this to the query: one extra row is how `hasMore` is detected without a COUNT. */
  fetchLimit: number;
};

export function pageParams(request: Request, defaultLimit: number = DEFAULT_PAGE_SIZE): PageParams {
  const params = new URL(request.url).searchParams;

  const requestedLimit = Number(params.get("limit"));
  const limit = Number.isSafeInteger(requestedLimit) && requestedLimit > 0
    ? Math.min(requestedLimit, MAX_PAGE_SIZE)
    : defaultLimit;

  const requestedOffset = Number(params.get("offset"));
  const offset = Number.isSafeInteger(requestedOffset) && requestedOffset > 0 ? requestedOffset : 0;

  return { limit, offset, fetchLimit: limit + 1 };
}

/**
 * Trims the sentinel row off a `fetchLimit`-sized result and reports whether it was there.
 */
export function splitPage<T>(rows: T[], page: PageParams, label: string): { rows: T[]; hasMore: boolean } {
  const hasMore = rows.length > page.limit;
  if (hasMore) {
    console.warn(`${label}: response truncated at ${page.limit} rows (offset ${page.offset}); paginate the admin UI`);
  }
  return { rows: hasMore ? rows.slice(0, page.limit) : rows, hasMore };
}
