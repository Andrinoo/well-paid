import { clearTokens, getAccessToken, getRefreshToken, setTokens } from "../session";
import { ApiError, apiErrorDetails } from "./errors";

export const API_BASE = import.meta.env.DEV
  ? "/__api"
  : (import.meta.env.VITE_API_BASE_URL || "https://well-paid-psi.vercel.app").replace(/\/$/, "");

type TokenPair = { access_token: string; refresh_token: string };

async function parseBody(res: Response): Promise<unknown> {
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return text;
  }
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

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
  retryUnauthorized = true,
): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
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
      throw new ApiError("A API não respondeu a tempo. Tente de novo.", 0, { code: "request_timeout" });
    }
    throw new ApiError("Não foi possível contactar a API.", 0, { code: "network_error" });
  }

  const body = await parseBody(res);
  if (res.status === 401 && retryUnauthorized && getRefreshToken()) {
    if (await refreshTokens()) return apiRequest<T>(path, init, false);
  }
  if (!res.ok) {
    const details = apiErrorDetails(body, `Pedido falhou (${res.status})`);
    throw new ApiError(details.message, res.status, details);
  }
  return body as T;
}
