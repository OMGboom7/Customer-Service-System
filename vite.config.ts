import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import basicSsl from '@vitejs/plugin-basic-ssl'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  // 代理目标: 优先取环境变量，默认公网地址
  const proxyTarget = env.VITE_GATEWAY_URL || 'https://oc.p07.icu'

  return {
    plugins: [
      react(),
      basicSsl(),
    ],
    server: {
      port: 5173,
      host: true,
      https: true,
      proxy: {
        '/gw': {
          target: proxyTarget,
          ws: true,
          changeOrigin: true,
          secure: false,
          rewrite: () => '/',
        },
      },
    },
  }
})
