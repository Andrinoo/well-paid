function normalizePath(raw: string | undefined): string {
  const p = (raw || "").trim();
  if (!p) return "";
  const withSlash = p.startsWith("/") ? p : `/${p}`;
  return withSlash.replace(/\/$/, "") || "/";
}

const STEALTH_HEADERS: Record<string, string> = {
  "X-Robots-Tag": "noindex, nofollow, noarchive",
  "Referrer-Policy": "no-referrer",
};

export default function middleware(request: Request): Response | undefined {
  const secret = normalizePath(process.env.SUPERADMIN_PATH);
  const url = new URL(request.url);
  const { pathname } = url;

  if (pathname === "/sa.html") {
    return new Response("Not found", { status: 404, headers: STEALTH_HEADERS });
  }

  if (secret && (pathname === secret || pathname.startsWith(`${secret}/`))) {
    const target = new URL(request.url);
    target.pathname = "/sa.html";
    const headers = new Headers(STEALTH_HEADERS);
    headers.set("x-middleware-rewrite", target.toString());
    return new Response(null, { status: 200, headers });
  }

  return undefined;
}
