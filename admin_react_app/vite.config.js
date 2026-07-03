import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  define: {
    'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV || 'development')
  },
  plugins: [
    react(),
    tailwindcss()
  ],

  // Build optimizations
  build: {
    // Enable source maps for production debugging
    sourcemap: true,
    
    // Optimize chunk splitting
    rollupOptions: {
      output: {
        // Manual chunk splitting for better caching
        manualChunks: {
          // Vendor chunks
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'chart-vendor': ['recharts'],
          'ui-vendor': ['@headlessui/react'],
          
          // Feature-based chunks
          'auth': [
            './src/context/AuthContext.jsx',
            './src/services/authService.js',
            './src/guards/ProtectedRoute.jsx',
            './src/guards/RoleBasedRoute.jsx'
          ],
          'dashboard': [
            './src/pages/Dashboard.jsx',
            './src/components/dashboard/RevenueChart.jsx',
            './src/utils/kpiCalculations.js'
          ],
          'admin': [
            './src/pages/AdminManagement.jsx',
            './src/services/adminService.js'
          ],
          'sessions': [
            './src/pages/LiveSessions.jsx',
            './src/services/sessionService.js'
          ],
          'payments': [
            './src/pages/PaymentCollection.jsx',
            './src/services/paymentService.js'
          ]
        },
        
        // Optimize chunk file names for better caching
        chunkFileNames: (chunkInfo) => {
          const facadeModuleId = chunkInfo.facadeModuleId
            ? chunkInfo.facadeModuleId.split('/').pop().replace('.jsx', '').replace('.js', '')
            : 'chunk';
          return `js/${facadeModuleId}-[hash].js`;
        },
        assetFileNames: (assetInfo) => {
          const info = assetInfo.name.split('.');
          const ext = info[info.length - 1];
          if (/\.(css)$/.test(assetInfo.name)) {
            return `css/[name]-[hash].${ext}`;
          }
          if (/\.(png|jpe?g|svg|gif|tiff|bmp|ico)$/i.test(assetInfo.name)) {
            return `images/[name]-[hash].${ext}`;
          }
          return `assets/[name]-[hash].${ext}`;
        }
      }
    },
    
    // Optimize build performance
    target: 'esnext',
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true, // Remove console.log in production
        drop_debugger: true
      }
    },
    
    // Set chunk size warning limit
    chunkSizeWarningLimit: 1000
  },

  // Development server configuration

  // proxy: {
  //   '/auth': {
  //     target: 'http://localhost:3001',
  //     changeOrigin: true,
  //   },
  //   '/parking': {
  //     target: 'http://localhost:3001',
  //     changeOrigin: true,
  //   },
  //   '/users': {
  //     target: 'http://localhost:3001',
  //     changeOrigin: true,
  //   },
  //   // Add additional proxies for other API prefixes as needed
  // }

  server: {
    port: 5173,
    host: true
  },

  // Optimize dependencies
  optimizeDeps: {
    include: [
      'react',
      'react-dom',
      'react-router-dom',
      'recharts'
    ]
  }
})
