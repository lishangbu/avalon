import {defineConfig} from '@playwright/test'

/** 日常模拟独立于浏览器项目；用例不申请page、context或browser。 */
export default defineConfig({
    testDir: './simulations',
    timeout: 300_000,
    workers: 1,
    use: {trace: 'off'},
    outputDir: './simulation-results',
    reporter: [['list'], ['json', {outputFile: '../avalon-app/target/natural-simulation-results.json'}]],
})
