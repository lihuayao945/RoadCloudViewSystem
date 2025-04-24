import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    cors: true, // 启用CORS
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 修改为云端后端地址
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
        configure: (proxy, options) => {
          // 代理请求事件
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log('发送代理请求:', req.method, req.url, '->',
                        options.target + (req.url.replace(/^\/api/, '') || '/'))
          })
          // 代理响应事件
          proxy.on('proxyRes', (proxyRes, req, res) => {
            console.log('收到代理响应:', proxyRes.statusCode, req.url)
          })
          // 代理错误事件
          proxy.on('error', (err, req, res) => {
            console.log('代理错误:', err)
          })
        }
      },
      '/download': {
        target: 'http://localhost:5147', // 修改为模型下载后端地址
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/download/, ''),
        configure: (proxy, options) => {
          // 代理请求事件
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log('发送代理请求:', req.method, req.url, '->',
                        options.target + (req.url.replace(/^\/download/, '') || '/'))
          })
          // 代理响应事件
          proxy.on('proxyRes', (proxyRes, req, res) => {
            console.log('收到代理响应:', proxyRes.statusCode, req.url)
          })
          // 代理错误事件
          proxy.on('error', (err, req, res) => {
            console.log('代理错误:', err)
          })
        }
      },
      '/system': {
        target: 'http://localhost:8080', // 修改为云端后端地址
        changeOrigin: true
      },
      '/menu': {
        target: 'http://localhost:8080', // 修改为云端后端地址
        changeOrigin: true,
        secure: false,
        ws: true, // 支持websocket
        xfwd: true, // 添加x-forwarded请求头
        timeout: 30000, // 设置超时时间
        configure: (proxy, options) => {
          // 代理请求事件
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log('发送menu代理请求:', req.method, req.url, '->',
                        options.target + req.url)
            
            // 添加必要的头信息
            proxyReq.setHeader('Accept', 'application/json, text/plain, */*')
            proxyReq.setHeader('Content-Type', 'application/json; charset=utf-8')
            // 添加跨域头
            proxyReq.setHeader('Origin', 'http://localhost:5173')
            proxyReq.setHeader('Referer', 'http://localhost:5173')
          })
          // 代理响应事件
          proxy.on('proxyRes', (proxyRes, req, res) => {
            console.log('收到menu代理响应:', proxyRes.statusCode, req.url)
            
            // 调试: 记录响应头和内容
            let responseBody = '';
            proxyRes.on('data', function(chunk) {
              responseBody += chunk;
            });
            proxyRes.on('end', function() {
              console.log('响应头:', proxyRes.headers);
              try {
                const data = JSON.parse(responseBody);
                console.log('响应内容:', JSON.stringify(data).substring(0, 200) + '...');
              } catch (e) {
                console.log('响应内容不是JSON格式');
              }
            });
          })
          // 代理错误事件
          proxy.on('error', (err, req, res) => {
            console.log('menu代理错误:', err)
          })
        }
      }
    }
  }
})
