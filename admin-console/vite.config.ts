import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Em dev: pedidos a /wellpaid-api/* são enviados para o FastAPI — mesma origem que o Vite, sem CORS.
    proxy: {
      '/wellpaid-api': {
        target: 'http://127.0.0.1:8000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/wellpaid-api/, ''),
      },
    },
  },
})
