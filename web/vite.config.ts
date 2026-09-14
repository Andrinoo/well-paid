import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5174,
    strictPort: true,
    proxy: {
      "/__api": {
        target: "https://well-paid-psi.vercel.app",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/__api/, ""),
      },
    },
  },
});
