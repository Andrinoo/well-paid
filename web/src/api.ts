import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  setTokens,
} from "./session";
import { shiftMonth } from "./format";

export const API_BASE = import.meta.env.DEV
  ? "/__api"
  : (import.meta.env.VITE_API_BASE_URL || "https://well-paid-psi.vercel.app").replace(
      /\/$/,
      "",
    );

export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

function detailMessage(body: unknown, fallback: string): string {
  if (!body || typeof body !== "object") return fallback;
  const detail = (body as { detail?: unknown }).detail;
  if (typeof detail === "string") return detail;
  if (Array.isArray(detail) && detail[0] && typeof detail[0] === "object") {
    const first = detail[0] as { msg?: string };
    if (first.msg) return first.msg;
  }
  return fallback;
}

async function parseBody(res: Response): Promise<unknown> {
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return text;
  }
}

async function request(
  path: string,
  init: RequestInit,
  retry: boolean,
): Promise<unknown> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const access = getAccessToken();
  if (access) headers.set("Authorization", `Bearer ${access}`);

  let res: Response;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers,
      signal: init.signal ?? AbortSignal.timeout(25_000),
    });
  } catch (err) {
    const name = err instanceof Error ? err.name : "";
    if (name === "AbortError" || name === "TimeoutError") {
      throw new ApiError("A API não respondeu a tempo. Tente de novo.", 0);
    }
    throw new ApiError("Não foi possível contactar a API.", 0);
  }
  const body = await parseBody(res);

  if (res.status === 401 && retry && getRefreshToken()) {
    const refreshed = await refreshTokens();
    if (refreshed) return request(path, init, false);
  }

  if (!res.ok) {
    throw new ApiError(
      detailMessage(body, `Pedido falhou (${res.status})`),
      res.status,
    );
  }
  return body;
}

async function refreshTokens(): Promise<boolean> {
  const refresh = getRefreshToken();
  if (!refresh) return false;
  try {
    const res = await fetch(`${API_BASE}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refresh_token: refresh }),
      signal: AbortSignal.timeout(25_000),
    });
    const body = (await parseBody(res)) as TokenPair | null;
    if (!res.ok || !body?.access_token || !body.refresh_token) {
      clearTokens();
      return false;
    }
    setTokens(body.access_token, body.refresh_token);
    return true;
  } catch {
    clearTokens();
    return false;
  }
}

export type TokenPair = {
  access_token: string;
  refresh_token: string;
};

export type UserMe = {
  email: string;
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

export async function registerAccount(
  email: string,
  password: string,
  fullName: string,
): Promise<RegisterResult> {
  return (await request(
    "/auth/register",
    {
      method: "POST",
      body: JSON.stringify({
        email,
        password,
        full_name: fullName.trim() || null,
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
  title: string;
  target_cents: number;
  current_cents: number;
  is_active: boolean;
};

export type ReservePlan = {
  id: string;
  title: string;
  monthly_target_cents: number;
  balance_cents: number;
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
};

export type InvestmentPosition = {
  id: string;
  instrument_type: string;
  name: string;
  principal_cents: number;
  annual_rate_bps: number;
};

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

export async function deleteExpense(id: string): Promise<void> {
  await request(
    `/expenses/${id}?delete_target=occurrence&delete_scope=all&confirm_delete_paid=true`,
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

export async function createGoal(body: {
  title: string;
  target_cents: number;
}): Promise<void> {
  await request("/goals", { method: "POST", body: JSON.stringify(body) }, true);
}

export async function contributeGoal(
  id: string,
  amount_cents: number,
): Promise<void> {
  await request(
    `/goals/${id}/contribute`,
    { method: "POST", body: JSON.stringify({ amount_cents }) },
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
}): Promise<void> {
  await request(
    "/investments/positions",
    { method: "POST", body: JSON.stringify({ ...body, is_liquid: true }) },
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
