export type ApiErrorPayload = {
  code?: string;
  message?: string;
  detail?: unknown;
  fields?: Record<string, string>;
};

export class ApiError extends Error {
  readonly status: number;
  readonly code: string | null;
  readonly fields: Record<string, string>;

  constructor(
    message: string,
    status: number,
    options?: { code?: string | null; fields?: Record<string, string> },
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = options?.code ?? null;
    this.fields = options?.fields ?? {};
  }
}
export function apiErrorDetails(
  body: unknown,
  fallback: string,
): { message: string; code: string | null; fields: Record<string, string> } {
  if (!body || typeof body !== "object") {
    return { message: fallback, code: null, fields: {} };
  }

  const payload = body as ApiErrorPayload;
  const fields = payload.fields && typeof payload.fields === "object" ? payload.fields : {};
  const code = typeof payload.code === "string" ? payload.code : null;

  if (typeof payload.message === "string" && payload.message.trim()) {
    return { message: payload.message, code, fields };
  }
  if (typeof payload.detail === "string") {
    return { message: payload.detail, code: code ?? payload.detail, fields };
  }
  if (Array.isArray(payload.detail) && payload.detail[0] && typeof payload.detail[0] === "object") {
    const first = payload.detail[0] as { msg?: unknown };
    if (typeof first.msg === "string") return { message: first.msg, code, fields };
  }
  return { message: fallback, code, fields };
}
