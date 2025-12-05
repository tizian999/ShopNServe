import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [react(), vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
    fs: {
      // Erlaube Zugriff auf das Frontend Root, damit alias außerhalb des Shell-Root funktioniert
      allow: [
        '..',
        path.resolve(__dirname, '..'),
        path.resolve(__dirname, '../microclient-vue/src')
      ]
    }
  },
  resolve: {
    alias: {
      // Alias um direkt Shop.vue zu importieren
      '@vue-micro': path.resolve(__dirname, '../microclient-vue/src')
    }
  },
  optimizeDeps: {
    include: ['vue']
  }
})
