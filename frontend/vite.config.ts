import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react({
      babel: {
        plugins: [['babel-plugin-react-compiler']],
      },
    }),
    tailwindcss(),
    VitePWA({
      strategies: 'injectManifest',
      srcDir: 'src',
      filename: 'sw-custom.js',
      registerType: 'autoUpdate',
      injectRegister: 'auto',
      manifest: {
        name: 'Rooma',
        short_name: 'Rooma',
        description: 'A room booking application',
        theme_color: '#ffffff',
        icons: [
          {
            src: '/icons/rooma_192.png',
            sizes: '192x192',
            type: 'image/png',
          },
          {
            src: '/icons/rooma_512.png',
            sizes: '512x512',
            type: 'image/png',
          },
        ],
      },
    }),
  ],
  test: {
    globals: true,
    environment: 'node',
    pool: 'threads',
    exclude: ['**/node_modules/**', '**/dist/**', '**/tests/**'],
  },
  define: {
    global: 'window',
  },
  server: {
    host: true,
    port: 5173,
  },
})
