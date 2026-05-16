import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/gw': {
        target: 'https://oc.p07.icu',
        ws: true,
        rewrite: () => '/',
      },
    },
  },
})
