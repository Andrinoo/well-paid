import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  setTokens,
} from "./session";

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

export async function fetchCashflow(): Promise<DashboardCashflow> {
  return (await request(
    "/dashboard/cashflow?dynamic=true&forecast_months=3",
    { method: "GET" },
    true,
  )) as DashboardCashflow;
}
