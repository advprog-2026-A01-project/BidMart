import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

/*
Tanggung jawab: proxy /api ke backend.
 */
// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})