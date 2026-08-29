import path from 'node:path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
  server: {
    // The backend (backend/src/main/java/.../auth/config/SecurityConfig.java)
    // has no CORS config at all — it isn't meant to serve a browser origin
    // directly. Proxying same-origin here sidesteps that entirely rather
    // than adding CORS just for local dev; a real deploy serves the built
    // frontend from behind the same origin/gateway as the API anyway.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/actuator': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
