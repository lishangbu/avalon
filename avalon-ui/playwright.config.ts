// 浏览器回归使用真实打包服务和数据库，手机与桌面项目复用相同业务用例；视口模拟不能代替所有真机测试。
import {defineConfig, devices} from '@playwright/test'

// 使用已打包的 Spring Boot 地址，验证同源静态资源和真实 API，不能用 Mock 替代。
export default defineConfig({
    testDir: './tests',
    timeout: 30_000,
    use: {
        baseURL: process.env.AVALON_BASE_URL ?? 'http://localhost:8080',
        trace: 'retain-on-failure',
        launchOptions: {executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE},
    },
    projects: [
        {name: 'desktop', use: {...devices['Desktop Chrome']}},
        {name: 'mobile', use: {...devices['Pixel 7']}},
    ],
})
