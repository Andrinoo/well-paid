import { API_BASE, ApiError } from "../api";

export type SaBoot = {
  path: string;
  apiPrefix: string;
};

declare global {
  interface Window {
    __WP_SA__?: SaBoot;
  }
}

const ACCESS_KEY = "wp_sa_access";
const REFRESH_KEY = "wp_sa_refresh";

export function saBoot(): SaBoot {
  const raw = window.__WP_SA__;
  const path = (raw?.path || "").trim();
  const apiPrefix = (raw?.apiPrefix || "/sa-api").trim() || "/sa-api";
  return {
    path: path.startsWith("/") ? path.replace(/\/$/, "") : path ? `/${path}` : "",
    apiPrefix: apiPrefix.startsWith("/") ? apiPrefix.replace(/\/$/, "") : `/${apiPrefix}`,
  };
}

export function getSaAccess(): string | null {
  return localStorage.getItem(ACCESS_KEY);
}

export function setSaTokens(access: string, refresh: string): void {
  localStorage.setItem(ACCESS_KEY, access);
  localStorage.setItem(REFRESH_KEY, refresh);
}

export function clearSaTokens(): void {
  localStorage.removeItem(ACCESS_KEY);
  localStorage.removeItem(REFRESH_KEY);
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

function detailMessage(body: unknown, fallback: string): string {
  if (!body || typeof body !== "object") return fallback;
  const detail = (body as { detail?: unknown }).detail;
  if (typeof detail === "string") return detail;
  return fallback;
}

export async function saRequest(
  path: string,
  init: RequestInit = {},
  retry = true,
): Promise<unknown> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const access = getSaAccess();
  if (access) headers.set("Authorization", `Bearer ${access}`);

  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers,
    signal: init.signal ?? AbortSignal.timeout(25_000),
  });
  const body = await parseBody(res);

  if (res.status === 401 && retry && localStorage.getItem(REFRESH_KEY)) {
    const refreshed = await refreshSa();
    if (refreshed) return saRequest(path, init, false);
  }

  if (!res.ok) {
    throw new ApiError(detailMessage(body, "Pedido recusado."), res.status);
  }
  return body;
}

async function refreshSa(): Promise<boolean> {
  const refresh = localStorage.getItem(REFRESH_KEY);
  if (!refresh) return false;
  try {
    const res = await fetch(`${API_BASE}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refresh_token: refresh }),
    });
    if (!res.ok) {
      clearSaTokens();
      return false;
    }
    const body = (await res.json()) as { access_token: string; refresh_token: string };
    setSaTokens(body.access_token, body.refresh_token);
    return true;
  } catch {
    clearSaTokens();
    return false;
  }
}

export async function saLogin(email: string, password: string): Promise<void> {
  const body = (await saRequest(
    "/auth/login",
    { method: "POST", body: JSON.stringify({ email, password }) },
    false,
  )) as { access_token: string; refresh_token: string };
  setSaTokens(body.access_token, body.refresh_token);
}

export type AdminMe = {
  email: string;
  is_admin: boolean;
  is_superuser: boolean;
};

export type AdminUserRow = {
  id: string;
  email: string;
  full_name: string | null;
  display_name: string | null;
  is_active: boolean;
  is_admin: boolean;
  is_superuser: boolean;
  is_free_plan: boolean;
  email_verified_at: string | null;
  trial_ends_at: string | null;
  created_at: string;
};

export type PaymentRow = {
  id: string;
  amount_cents: number;
  currency: string;
  method: string;
  status: string;
  payer_name: string | null;
  pix_copy: string | null;
  pix_qr: string | null;
  paid_at: string | null;
  due_at: string | null;
  created_at: string;
};

export type BillingDetail = {
  id: string;
  email: string;
  full_name: string | null;
  display_name: string | null;
  is_active: boolean;
  is_free_plan: boolean;
  is_superuser: boolean;
  plan: string;
  trial_ends_at: string | null;
  paid_until: string | null;
  modules: string[];
  catalog: { id: string; included_in_basic: boolean; price_cents: number }[];
  payments: PaymentRow[];
};

export async function fetchSaMe(): Promise<AdminMe> {
  return (await saRequest(`${saBoot().apiPrefix}/me`)) as AdminMe;
}

export async function fetchUsers(params: {
  q?: string;
  plan?: string;
  is_active?: string;
}): Promise<{ items: AdminUserRow[]; total: number }> {
  const qs = new URLSearchParams();
  if (params.q) qs.set("q", params.q);
  if (params.plan) qs.set("plan", params.plan);
  if (params.is_active === "true" || params.is_active === "false") {
    qs.set("is_active", params.is_active);
  }
  qs.set("limit", "50");
  const suffix = qs.toString() ? `?${qs.toString()}` : "";
  return (await saRequest(`${saBoot().apiPrefix}/users${suffix}`)) as {
    items: AdminUserRow[];
    total: number;
  };
}

export async function fetchBilling(userId: string): Promise<BillingDetail> {
  return (await saRequest(
    `${saBoot().apiPrefix}/users/${userId}/billing`,
  )) as BillingDetail;
}

export async function patchFree(userId: string, isFree: boolean): Promise<BillingDetail> {
  return (await saRequest(`${saBoot().apiPrefix}/users/${userId}/free`, {
    method: "PATCH",
    body: JSON.stringify({ is_free_plan: isFree }),
  })) as BillingDetail;
}

export async function patchModules(
  userId: string,
  modules: { module_id: string; enabled: boolean }[],
): Promise<BillingDetail> {
  return (await saRequest(`${saBoot().apiPrefix}/users/${userId}/modules`, {
    method: "PATCH",
    body: JSON.stringify({ modules }),
  })) as BillingDetail;
}

export async function createPix(userId: string): Promise<BillingDetail> {
  return (await saRequest(`${saBoot().apiPrefix}/users/${userId}/pix`, {
    method: "POST",
  })) as BillingDetail;
}

export async function releasePix(
  userId: string,
  payerName: string,
  paymentId?: string,
): Promise<BillingDetail> {
  return (await saRequest(`${saBoot().apiPrefix}/users/${userId}/pix/release`, {
    method: "POST",
    body: JSON.stringify({
      payer_name: payerName,
      payment_id: paymentId || null,
    }),
  })) as BillingDetail;
}
