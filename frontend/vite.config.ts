import path from 'node:path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': path.resolve(import.meta.dirname, './src') },
  },
  server: {
    // Pinned: the Google and GitHub OAuth apps have their redirect URIs
    // registered against http://localhost:5173, so a fallback port would
    // silently break sign-in. Fail loudly instead.
    port: 5173,
    strictPort: true,
    // The backend has no CORS config — it is not meant to serve a browser
    // origin directly. Proxying same-origin here sidesteps that for local
    // dev; a real deploy serves the built frontend behind the same gateway.
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/actuator': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
