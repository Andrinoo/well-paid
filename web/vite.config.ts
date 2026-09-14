import path from "node:path";
import { fileURLToPath } from "node:url";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv, type Plugin, type PreviewServer, type ViteDevServer } from "vite";

const rootDir = path.dirname(fileURLToPath(import.meta.url));

function normalizePath(raw: string): string {
  const p = raw.trim();
  if (!p) return "";
  const withSlash = p.startsWith("/") ? p : `/${p}`;
  return withSlash.replace(/\/$/, "") || "/";
}

function saStealthPlugin(secretPath: string, apiPrefix: string): Plugin {
  const secret = normalizePath(secretPath);
  const prefix = normalizePath(apiPrefix) || "/sa-api";
  const boot = `window.__WP_SA__=${JSON.stringify({ path: secret, apiPrefix: prefix })};`;

  function intercept(server: ViteDevServer | PreviewServer) {
    server.middlewares.use((req, res, next) => {
      const raw = req.url || "";
      const url = raw.split("?")[0] || "";
      if (url === "/sa.html") {
        res.statusCode = 404;
        res.setHeader("X-Robots-Tag", "noindex, nofollow, noarchive");
        res.setHeader("Referrer-Policy", "no-referrer");
        res.end("Not found");
        return;
      }
      if (secret && (url === secret || url.startsWith(`${secret}/`))) {
        const qs = raw.includes("?") ? raw.slice(raw.indexOf("?")) : "";
        req.url = `/sa.html${qs}`;
        res.setHeader("X-Robots-Tag", "noindex, nofollow, noarchive");
        res.setHeader("Referrer-Policy", "no-referrer");
      }
      next();
    });
  }

  return {
    name: "wp-sa-stealth",
    configureServer: intercept,
    configurePreviewServer: intercept,
    transformIndexHtml(html, ctx) {
      const file = (ctx.filename || "").replaceAll("\\", "/");
      if (!file.endsWith("/sa.html")) return html;
      return html.replace("/*__WP_SA__*/", boot);
    },
  };
}

export default defineConfig(({ mode }) => {
  const env = {
    ...loadEnv(mode, path.resolve(rootDir, ".."), ""),
    ...loadEnv(mode, rootDir, ""),
  };
  const saPath = env.SUPERADMIN_PATH || process.env.SUPERADMIN_PATH || "";
  const saApi =
    env.SUPERADMIN_API_PREFIX || process.env.SUPERADMIN_API_PREFIX || "/sa-api";

  return {
    plugins: [react(), tailwindcss(), saStealthPlugin(saPath, saApi)],
    server: {
      host: true,
      port: 5174,
      strictPort: true,
      proxy: {
        "/__api": {
          target: "https://well-paid-psi.vercel.app",
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/__api/, ""),
        },
      },
    },
    build: {
      rollupOptions: {
        input: {
          main: path.resolve(rootDir, "index.html"),
          sa: path.resolve(rootDir, "sa.html"),
        },
      },
    },
  };
});
