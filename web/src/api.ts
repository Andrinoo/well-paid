import { clearTokens, getRefreshToken, setTokens } from "./session";
import { shiftMonth } from "./format";
import { API_BASE, apiRequest as request } from "./api/http";

export { ApiError } from "./api/errors";
export { API_BASE } from "./api/http";

export type TokenPair = {
  access_token: string;
  refresh_token: string;
};

export type UserMe = {
  email: string;
  public_id?: string | null;
  full_name: string | null;
  display_name: string | null;
  family_mode_enabled?: boolean;
};

export type RegisterResult = {
  message: string;
  email: string;
  dev_verification_token?: string | null;
  dev_verification_code?: string | null;
};

export type CategorySpend = {
  category_key: string;
  name: string;
  amount_cents: number;
  share_bps: number | null;
};

export type PendingExpenseItem = {
  id: string;
  description: string;
  amount_cents: number;
  due_date: string | null;
  is_mine: boolean;
};

export type GoalSummaryItem = {
  id: string;
  title: string;
  current_cents: number;
  target_cents: number;
  is_mine: boolean;
  reference_thumbnail_url?: string | null;
};

export type DashboardOverview = {
  period: { year: number; month: number };
  month_income_cents: number;
  month_expense_total_cents: number;
  month_balance_cents: number;
  spending_by_category: CategorySpend[];
  pending_total_cents: number;
  pending_preview: PendingExpenseItem[];
  upcoming_due: PendingExpenseItem[];
  goals_preview: GoalSummaryItem[];
  emergency_reserve_balance_cents?: number;
  emergency_reserve_monthly_target_cents?: number;
};

export type DashboardCashflow = {
  months: { year: number; month: number }[];
  income_cents: number[];
  expense_paid_cents: number[];
  expense_forecast_cents: number[];
};

export async function login(
  email: string,
  password: string,
): Promise<TokenPair> {
  const body = (await request(
    "/auth/login",
    { method: "POST", body: JSON.stringify({ email, password }) },
    false,
  )) as TokenPair;
  setTokens(body.access_token, body.refresh_token);
  return body;
}

export type CaptchaConfig = {
  enabled: boolean;
  site_key: string | null;
};

export async function fetchCaptchaConfig(): Promise<CaptchaConfig> {
  return (await request("/auth/captcha", { method: "GET" }, false)) as CaptchaConfig;
}

export async function registerAccount(
  email: string,
  password: string,
  fullName: string,
  turnstileToken?: string | null,
): Promise<RegisterResult> {
  return (await request(
    "/auth/register",
    {
      method: "POST",
      body: JSON.stringify({
        email,
        password,
        full_name: fullName.trim() || null,
        ...(turnstileToken ? { turnstile_token: turnstileToken } : {}),
      }),
    },
    false,
  )) as RegisterResult;
}

export async function verifyEmail(payload: {
  token?: string;
  email?: string;
  code?: string;
}): Promise<TokenPair> {
  const body = (await request(
    "/auth/verify-email",
    { method: "POST", body: JSON.stringify(payload) },
    false,
  )) as TokenPair;
  setTokens(body.access_token, body.refresh_token);
  return body;
}

export async function resendVerification(email: string): Promise<string> {
  const body = (await request(
    "/auth/resend-verification",
    { method: "POST", body: JSON.stringify({ email }) },
    false,
  )) as { message: string };
  return body.message;
}

export async function forgotPassword(email: string): Promise<string> {
  const body = (await request(
    "/auth/forgot-password",
    { method: "POST", body: JSON.stringify({ email }) },
    false,
  )) as { message: string };
  return body.message;
}

export async function resetPassword(
  token: string,
  newPassword: string,
): Promise<void> {
  await request(
    "/auth/reset-password",
    { method: "POST", body: JSON.stringify({ token, new_password: newPassword }) },
    false,
  );
}

export async function fetchMe(): Promise<UserMe> {
  return (await request("/auth/me", { method: "GET" }, true)) as UserMe;
}

export async function logout(): Promise<void> {
  const refresh = getRefreshToken();
  try {
    if (refresh) {
      await request(
        "/auth/logout",
        { method: "POST", body: JSON.stringify({ refresh_token: refresh }) },
        false,
      );
    }
  } finally {
    clearTokens();
  }
}

export async function fetchOverview(
  year: number,
  month: number,
): Promise<DashboardOverview> {
  const q = new URLSearchParams({ year: String(year), month: String(month) });
  return (await request(
    `/dashboard/overview?${q}`,
    { method: "GET" },
    true,
  )) as DashboardOverview;
}

export async function fetchCashflow(opts?: {
  dynamic?: boolean;
  forecastMonths?: number;
  year?: number;
  month?: number;
}): Promise<DashboardCashflow> {
  const dynamic = opts?.dynamic ?? true;
  const forecastMonths = opts?.forecastMonths ?? 3;
  const q = new URLSearchParams({
    dynamic: String(dynamic),
    forecast_months: String(forecastMonths),
  });
  if (!dynamic && opts?.year && opts?.month) {
    const start = shiftMonth(opts.year, opts.month, -5);
    q.set("start_year", String(start.year));
    q.set("start_month", String(start.month));
    q.set("end_year", String(opts.year));
    q.set("end_month", String(opts.month));
  }
  return (await request(
    `/dashboard/cashflow?${q}`,
    { method: "GET" },
    true,
  )) as DashboardCashflow;
}

export type HomeBanner = {
  id: string;
  title: string;
  body: string;
  kind: string;
  cta_label: string | null;
  cta_url: string | null;
};

export async function fetchHomeBanner(): Promise<HomeBanner | null> {
  try {
    const body = (await request(
      "/announcements/active?placement=home_banner&limit=1",
      { method: "GET" },
      true,
    )) as { items?: HomeBanner[] };
    return body.items?.[0] ?? null;
  } catch {
    return null;
  }
}

export type Category = { id: string; key: string; name: string };

export type Expense = {
  id: string;
  description: string;
  amount_cents: number;
  expense_date: string;
  due_date: string | null;
  status: string;
  category_id: string;
  category_name: string;
  installment_total?: number;
  installment_number?: number;
  recurring_frequency?: string | null;
  is_shared?: boolean;
  is_family?: boolean;
  is_mine?: boolean;
  is_projected?: boolean;
  monthly_interest_bps?: number | null;
  installment_group_id?: string | null;
  recurring_series_id?: string | null;
  shared_with_label?: string | null;
  counterparty_label?: string | null;
};

export type Income = {
  id: string;
  description: string;
  amount_cents: number;
  income_date: string;
  income_category_id: string;
  category_name: string;
  notes?: string | null;
};

export type Goal = {
  id: string;
  owner_user_id?: string;
  is_mine?: boolean;
  title: string;
  target_cents: number;
  current_cents: number;
  is_active: boolean;
  is_family?: boolean;
  target_url?: string | null;
  reference_product_name?: string | null;
  reference_price_cents?: number | null;
  reference_currency?: string;
  price_checked_at?: string | null;
  price_source?: string | null;
  reference_thumbnail_url?: string | null;
  description?: string | null;
  due_at?: string | null;
  price_check_interval_hours?: number;
  last_price_track_at?: string | null;
  tracking_enabled?: boolean;
  price_alternatives?: { label: string; price_cents: number; url?: string | null }[];
};

export type GoalContribution = {
  id: string;
  goal_id: string;
  amount_cents: number;
  note: string | null;
  recorded_at: string;
};

export type GoalPriceHistoryItem = {
  id: string;
  goal_id: string;
  price_cents: number;
  currency: string;
  source: string | null;
  observed_url: string | null;
  observed_title: string | null;
  capture_type: string;
  recorded_at: string;
};

export type GoalProductHit = {
  title: string;
  price_cents: number;
  currency_id: string;
  url: string;
  thumbnail: string | null;
  source: string;
};

export type ReservePlan = {
  id: string;
  title: string;
  details?: string | null;
  monthly_target_cents: number;
  target_cents?: number | null;
  balance_cents: number;
  opening_balance_cents?: number;
  tracking_start?: string;
  target_end_date?: string | null;
  status: string;
};

export type ShoppingList = {
  id: string;
  title: string | null;
  store_name: string | null;
  status: string;
  items_count: number;
  total_cents: number | null;
};

export type ShoppingItem = {
  id: string;
  label: string;
  quantity: number;
  line_amount_cents: number | null;
  is_picked: boolean;
};

export type InvestmentOverview = {
  total_allocated_cents: number;
  total_yield_cents: number;
  estimated_monthly_yield_cents: number;
  rates_source?: string;
  rates_fallback_used?: boolean;
  buckets?: { key: string; label: string; allocated_cents: number; yield_cents: number; yield_pct_month: number }[];
};

export type InvestmentPosition = {
  id: string;
  instrument_type: string;
  name: string;
  principal_cents: number;
  annual_rate_bps: number;
  description?: string | null;
  maturity_date?: string | null;
  is_liquid?: boolean;
};

export type InvestmentTicker = { symbol: string; name: string; instrument_type: string; last_price?: number | null; currency?: string | null; change_24h_percent?: number | null };
export type InvestmentRates = { cdi_annual_percent: number; cdb_annual_percent: number; fixed_income_annual_percent: number; source: string; rates_fallback_used: boolean };
export type InvestmentQuote = { symbol: string; last_price: number; currency: string; as_of?: string | null; source: string; confidence?: number | null; fallback_used?: boolean; stale?: boolean; change_24h?: number | null; change_24h_percent?: number | null; day_high?: number | null; day_low?: number | null; volume_24h?: number | null; error?: string | null };
export type InvestmentFundamentals = { symbol: string; company_name?: string | null; pl?: string | null; pvp?: string | null; daily_liquidity?: string | null; dividend_yield?: string | null; dividend_yield_12m?: string | null; roe?: string | null; ev_ebitda?: string | null; net_margin?: string | null; net_debt_ebitda?: string | null; eps?: string | null; source: string; confidence?: number | null };

export type FamilyMe = {
  family: {
    id: string;
    name: string;
    members: {
      user_id: string;
      email: string;
      full_name?: string | null;
      role: string;
      is_self: boolean;
    }[];
  } | null;
};

function monthQuery(year: number, month: number): string {
  return new URLSearchParams({
    year: String(year),
    month: String(month),
  }).toString();
}

export async function fetchCategories(): Promise<Category[]> {
  return (await request("/categories", { method: "GET" }, true)) as Category[];
}

export async function fetchIncomeCategories(): Promise<Category[]> {
  return (await request("/income-categories", { method: "GET" }, true)) as Category[];
}

export async function fetchExpenses(
  year: number,
  month: number,
): Promise<Expense[]> {
  return (await request(
    `/expenses?${monthQuery(year, month)}`,
    { method: "GET" },
    true,
  )) as Expense[];
}

export async function createExpense(body: {
  description: string;
  amount_cents: number;
  expense_date: string;
  due_date: string | null;
  category_id: string;
  status?: "pending" | "paid";
  monthly_interest_bps?: number | null;
  start_date?: string | null;
  installment_total?: number;
  recurring_frequency?: string | null;
  is_shared?: boolean;
  is_family?: boolean;
  shared_with_user_id?: string | null;
  split_mode?: "amount" | "percent" | null;
  owner_share_cents?: number | null;
  peer_share_cents?: number | null;
  owner_percent_bps?: number | null;
  peer_percent_bps?: number | null;
}): Promise<void> {
  await request("/expenses", { method: "POST", body: JSON.stringify(body) }, true);
}

export async function payExpense(id: string): Promise<void> {
  await request(`/expenses/${id}/pay`, { method: "POST", body: "{}" }, true);
}

export async function updateExpense(
  id: string,
  body: Partial<Pick<Expense, "description" | "amount_cents" | "expense_date" | "due_date" | "category_id">>,
): Promise<Expense> {
  return (await request(
    `/expenses/${id}`,
    { method: "PUT", body: JSON.stringify(body) },
    true,
  )) as Expense;
}

export async function deleteExpense(
  id: string,
  options?: { target?: "occurrence" | "series"; scope?: "all" | "future_unpaid" },
): Promise<void> {
  const query = new URLSearchParams({
    delete_target: options?.target ?? "occurrence",
    delete_scope: options?.scope ?? "all",
    confirm_delete_paid: "true",
  });
  await request(
    `/expenses/${id}?${query.toString()}`,
    { method: "DELETE" },
    true,
  );
}

export async function deleteIncome(id: string): Promise<void> {
  await request(`/incomes/${id}`, { method: "DELETE" }, true);
}

export async function fetchIncomes(
  year: number,
  month: number,
): Promise<Income[]> {
  return (await request(
    `/incomes?${monthQuery(year, month)}`,
    { method: "GET" },
    true,
  )) as Income[];
}

export async function createIncome(body: {
  description: string;
  amount_cents: number;
  income_date: string;
  income_category_id: string;
  notes?: string | null;
}): Promise<void> {
  await request("/incomes", { method: "POST", body: JSON.stringify(body) }, true);
}

export async function fetchGoals(): Promise<Goal[]> {
  return (await request("/goals", { method: "GET" }, true)) as Goal[];
}

export async function searchGoalProducts(query: string): Promise<GoalProductHit[]> {
  const body = (await request(
    "/goals/product-search",
    { method: "POST", body: JSON.stringify({ query }) },
    true,
  )) as { results?: GoalProductHit[] };
  return body.results ?? [];
}

export async function createGoal(body: {
  title: string;
  target_cents: number;
  current_cents?: number;
  is_active?: boolean;
  is_family?: boolean;
  target_url?: string | null;
  reference_product_name?: string | null;
  reference_price_cents?: number | null;
  reference_thumbnail_url?: string | null;
  reference_currency?: string;
  description?: string | null;
  due_at?: string | null;
  price_check_interval_hours?: number;
  tracking_enabled?: boolean;
  price_source?: string | null;
}): Promise<void> {
  await request("/goals", { method: "POST", body: JSON.stringify(body) }, true);
}

export async function updateGoal(id: string, body: Partial<{
  title: string;
  target_cents: number;
  is_active: boolean;
  is_family: boolean;
  target_url: string | null;
  reference_product_name: string | null;
  reference_price_cents: number | null;
  reference_currency: string;
  price_source: string | null;
  reference_thumbnail_url: string | null;
  description: string | null;
  due_at: string | null;
  price_check_interval_hours: number;
  tracking_enabled: boolean;
}>): Promise<Goal> {
  return await request(`/goals/${id}`, { method: "PUT", body: JSON.stringify(body) }, true) as Goal;
}

export async function deleteGoal(id: string, confirmDeleteBalance = false): Promise<void> {
  const query = confirmDeleteBalance ? "?confirm_delete_balance=true" : "";
  await request(`/goals/${id}${query}`, { method: "DELETE" }, true);
}

export async function fetchGoalContributions(id: string): Promise<GoalContribution[]> {
  return await request(`/goals/${id}/contributions`, { method: "GET" }, true) as GoalContribution[];
}

export async function fetchGoalPriceHistory(id: string): Promise<GoalPriceHistoryItem[]> {
  const body = await request(`/goals/${id}/price-history`, { method: "GET" }, true) as {
    items?: GoalPriceHistoryItem[];
  };
  return body.items ?? [];
}

export async function refreshGoalPrice(id: string): Promise<Goal> {
  return await request(`/goals/${id}/refresh-reference-price`, { method: "POST", body: "{}" }, true) as Goal;
}

export function thumbnailSrc(raw: string | null | undefined): string | null {
  const t = (raw ?? "").trim();
  if (!t) return null;
  let url = t;
  if (url.startsWith("//")) url = `https:${url}`;
  else if (url.startsWith("www.")) url = `https://${url}`;
  if (!/^https?:\/\//i.test(url)) return null;
  return `${API_BASE}/media/thumbnail?u=${encodeURIComponent(url)}`;
}

export async function contributeGoal(
  id: string,
  amount_cents: number,
  note?: string | null,
): Promise<void> {
  await request(
    `/goals/${id}/contribute`,
    { method: "POST", body: JSON.stringify({ amount_cents, note: note?.trim() || null }) },
    true,
  );
}

export async function fetchReservePlans(): Promise<ReservePlan[]> {
  return (await request(
    "/emergency-reserve/plans",
    { method: "GET" },
    true,
  )) as ReservePlan[];
}

export async function createReservePlan(body: {
  title: string;
  monthly_target_cents: number;
  target_cents?: number | null;
  opening_balance_cents?: number | null;
  details?: string | null;
}): Promise<void> {
  await request(
    "/emergency-reserve/plans",
    { method: "POST", body: JSON.stringify(body) },
    true,
  );
}

export async function contributeReserve(
  planId: string,
  amount_cents: number,
  contribution_date: string,
): Promise<void> {
  await request(
    "/emergency-reserve/contributions",
    {
      method: "POST",
      body: JSON.stringify({
        contribution_date,
        total_amount_cents: amount_cents,
        allocations: [{ plan_id: planId, amount_cents }],
      }),
    },
    true,
  );
}

export async function fetchShoppingLists(): Promise<ShoppingList[]> {
  return (await request("/shopping-lists", { method: "GET" }, true)) as ShoppingList[];
}

export async function createShoppingList(title: string): Promise<void> {
  await request(
    "/shopping-lists",
    { method: "POST", body: JSON.stringify({ title }) },
    true,
  );
}

export async function fetchShoppingDetail(id: string): Promise<{
  items: ShoppingItem[];
}> {
  return (await request(
    `/shopping-lists/${id}`,
    { method: "GET" },
    true,
  )) as { items: ShoppingItem[] };
}

export async function addShoppingItem(
  listId: string,
  label: string,
): Promise<void> {
  await request(
    `/shopping-lists/${listId}/items`,
    { method: "POST", body: JSON.stringify({ label, quantity: 1 }) },
    true,
  );
}

export async function patchShoppingItem(
  listId: string,
  itemId: string,
  body: { is_picked?: boolean },
): Promise<void> {
  await request(
    `/shopping-lists/${listId}/items/${itemId}`,
    { method: "PATCH", body: JSON.stringify(body) },
    true,
  );
}

export async function deleteShoppingList(id: string): Promise<void> {
  await request(`/shopping-lists/${id}`, { method: "DELETE" }, true);
}

export async function fetchInvestmentOverview(): Promise<InvestmentOverview> {
  return (await request(
    "/investments/overview",
    { method: "GET" },
    true,
  )) as InvestmentOverview;
}

export async function fetchPositions(): Promise<InvestmentPosition[]> {
  return (await request(
    "/investments/positions",
    { method: "GET" },
    true,
  )) as InvestmentPosition[];
}

export async function createPosition(body: {
  instrument_type: string;
  name: string;
  principal_cents: number;
  annual_rate_bps: number;
  description?: string | null;
  maturity_date?: string | null;
  is_liquid?: boolean;
}): Promise<void> {
  await request(
    "/investments/positions",
    { method: "POST", body: JSON.stringify({ is_liquid: true, ...body }) },
    true,
  );
}

export async function fetchFamilyMe(): Promise<FamilyMe> {
  return (await request("/families/me", { method: "GET" }, true)) as FamilyMe;
}

export async function createFamily(name: string): Promise<void> {
  await request(
    "/families/me",
    { method: "POST", body: JSON.stringify({ name }) },
    true,
  );
}

export async function joinFamily(token: string): Promise<void> {
  await request(
    "/families/join",
    { method: "POST", body: JSON.stringify({ token }) },
    true,
  );
}

export async function createFamilyInvite(): Promise<{ token: string }> {
  return (await request(
    "/families/me/invites",
    { method: "POST", body: "{}" },
    true,
  )) as { token: string };
}

export async function patchDisplayName(display_name: string): Promise<UserMe> {
  return (await request(
    "/auth/me",
    { method: "PATCH", body: JSON.stringify({ display_name }) },
    true,
  )) as UserMe;
}

export async function searchInvestmentTickers(query: string): Promise<InvestmentTicker[]> {
  return await request(`/investments/tickers/search?${new URLSearchParams({ q: query, limit: "8" })}`, { method: "GET" }, true) as InvestmentTicker[];
}

export async function fetchInvestmentQuote(symbol: string): Promise<InvestmentQuote> {
  return await request(`/investments/quote?${new URLSearchParams({ symbol })}`, { method: "GET" }, true) as InvestmentQuote;
}

export async function fetchInvestmentFundamentals(symbol: string): Promise<InvestmentFundamentals> {
  return await request(`/investments/fundamentals?${new URLSearchParams({ symbol })}`, { method: "GET" }, true) as InvestmentFundamentals;
}

export async function fetchInvestmentRates(): Promise<InvestmentRates> {
  return await request("/investments/suggested-rates", { method: "GET" }, true) as InvestmentRates;
}

export async function addInvestmentPrincipal(id: string, add_principal_cents: number): Promise<void> {
  await request(`/investments/positions/${id}`, { method: "PATCH", body: JSON.stringify({ add_principal_cents }) }, true);
}

export async function deleteInvestmentPosition(id: string): Promise<void> {
  await request(`/investments/positions/${id}`, { method: "DELETE" }, true);
}

export async function fetchInvestmentPreferences(): Promise<{ view_mode: "cards" | "list" }> {
  return await request("/investments/preferences", { method: "GET" }, true) as { view_mode: "cards" | "list" };
}

export async function saveInvestmentPreferences(view_mode: "cards" | "list"): Promise<void> {
  await request("/investments/preferences", { method: "PUT", body: JSON.stringify({ view_mode }) }, true);
}

export async function moveReserveBalance(
  planId: string,
  amountCents: number,
  movementDate: string,
  note?: string | null,
): Promise<void> {
  await request(
    "/emergency-reserve/contributions",
    {
      method: "POST",
      body: JSON.stringify({
        contribution_date: movementDate,
        total_amount_cents: amountCents,
        allocations: [{ plan_id: planId, amount_cents: amountCents }],
        note: note?.trim() || null,
      }),
    },
    true,
  );
}

export async function updateReservePlan(
  planId: string,
  body: { title: string; monthly_target_cents: number; target_cents?: number | null; details?: string | null },
): Promise<ReservePlan> {
  return (await request(`/emergency-reserve/plans/${planId}`, { method: "PUT", body: JSON.stringify(body) }, true)) as ReservePlan;
}

export async function deleteReservePlan(planId: string): Promise<void> {
  await request(`/emergency-reserve/plans/${planId}`, { method: "DELETE" }, true);
}

export async function patchFamilyMode(family_mode_enabled: boolean): Promise<UserMe> {
  return (await request(
    "/auth/me",
    { method: "PATCH", body: JSON.stringify({ family_mode_enabled }) },
    true,
  )) as UserMe;
}
