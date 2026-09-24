// 浏览器开发构建配置。开发代理保持API同源语义，生产资源由Spring Boot提供；此配置不存储账号凭据。
import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import {fileURLToPath} from 'node:url'

// 独立启动 Vite 时也通过开发代理访问后端，浏览器无需持有跨域凭据。
export default defineConfig({
    resolve: {alias: {'@': fileURLToPath(new URL('./src', import.meta.url))}},
    plugins: [vue()],
    build: {
        rolldownOptions: {
            treeshake: {
                // Ionic 9 的组件入口通过导出的 defineCustomElement 按需注册。
                // 仅允许删除未引用的组件入口，保留运行时、样式及其他依赖的原始副作用判断。
                // 升级 Ionic 后必须重新验证生产包的组件注册、弹层与路由行为。
                moduleSideEffects: (id) => (/\/@ionic\/core\/components\/ion-[^/]+\.js$/.test(id) ? false : undefined),
            },
        },
    },
    server: {port: 5173, strictPort: true, proxy: {'/api': 'http://localhost:8080'}},
})
