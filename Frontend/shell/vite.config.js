import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [react(), vue()],
  server: {
    port: 5173, // Explicitly setting port
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
    fs: {
      // Allow access to parent directories for microfrontend imports
      allow: [
        '..',
      ]
    }
  },
  resolve: {
    alias: {
      // Alias for Vue microfrontend import
      '@vue-micro': path.resolve(__dirname, '../microclient-vue/src'),
      // Force a single copy of React
      'react': path.resolve(__dirname, 'node_modules/react'),
      'react-dom': path.resolve(__dirname, 'node_modules/react-dom'),
    }
  },
  optimizeDeps: {
    include: ['vue']
  }
})
