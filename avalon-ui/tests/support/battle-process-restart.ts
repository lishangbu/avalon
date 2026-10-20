import {chromium, devices, expect, type BrowserContext, type Page, type TestInfo} from '@playwright/test'
import {rmSync} from 'node:fs'
import {resolve} from 'node:path'
import type {Battle} from '../../src/features/battle/types'
import type {TurnSubmission} from '../../src/features/battle/turn-submission-storage'

/** 在真实活动战局中结束浏览器进程，验证自动恢复同一战局并继续正常提交，不注入会话或战局数据。 */
export async function verifyBattleProcessRestart(account: string, testInfo: TestInfo): Promise<Battle> {
    const profile = testInfo.outputPath('active-battle-profile')
    let context: BrowserContext | undefined
    const launch = () =>
        chromium.launchPersistentContext(profile, {
            ...devices[testInfo.project.name === 'mobile' ? 'Pixel 7' : 'Desktop Chrome'],
            baseURL: process.env.AVALON_BASE_URL ?? 'http://localhost:8080',
            executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE,
        })
    let lostBody: TurnSubmission | undefined
    const defend = async (
        page: Page,
        battle: Battle,
        loseResponse: boolean | 'before-server' = false,
    ): Promise<Battle> => {
        await expect(page.getByRole('heading', {name: `第 ${battle.round} 回合`, exact: true})).toBeVisible()
        for (const unit of battle.units.filter(
            (unit) => !unit.enemy && unit.hp > 0 && (unit.hero || unit.position < 2),
        )) {
            await page
                .locator('.actor-strip button')
                .filter({has: page.locator('strong', {hasText: unit.name})})
                .click()
            await page.getByRole('combobox', {name: `${unit.name}行动`, exact: true}).selectOption('DEFEND')
            await page.getByRole('combobox', {name: `${unit.name}目标`, exact: true}).selectOption(unit.id)
        }
        // 可选分支真实结算后只丢弃响应，保存测试观测值，不向页面注入任何草稿或战局。
        let settleLost: (battle: Battle) => void = () => {}
        let rejectLost: (cause: unknown) => void = () => {}
        const lost = loseResponse
            ? new Promise<Battle>((resolve, reject) => {
                  settleLost = resolve
                  rejectLost = reject
              })
            : undefined
        if (loseResponse)
            await page.route(
                `**/api/battle/${battle.id}/turn`,
                async (route) => {
                    try {
                        lostBody = route.request().postDataJSON()
                        // 未送达分支不调用fetch或continue，返回值仅用于后续与正式GET比较，不注入页面。
                        if (loseResponse === 'before-server') {
                            await route.abort('connectionreset')
                            settleLost(battle)
                            return
                        }
                        const response = await route.fetch()
                        expect(response.ok()).toBeTruthy()
                        const result: Battle = await response.json()
                        await route.abort('connectionreset')
                        settleLost(result)
                    } catch (cause) {
                        rejectLost(cause)
                    }
                },
                {times: 1},
            )
        const submitted =
            lost ??
            page
                .waitForResponse(
                    (response) =>
                        response.url().endsWith(`/api/battle/${battle.id}/turn`) &&
                        response.request().method() === 'POST',
                )
                .then((response) => response.json())
        await page.getByRole('button', {name: '提交本回合', exact: true}).click()
        return submitted
    }
    try {
        context = await launch()
        const before = context.pages()[0]!
        before.setDefaultTimeout(15_000)
        await before.goto('/login')
        await before.getByLabel('账号', {exact: true}).fill(account)
        await before.getByLabel('密码', {exact: true}).fill('Natural-http-1234')
        await before.getByRole('button', {name: '进入冒险', exact: true}).click()
        await before.waitForURL('**/world')
        const started = before
            .waitForResponse(
                (response) => response.url().endsWith('/api/battle') && response.request().method() === 'POST',
            )
            .then((response) => response.json())
        await before.getByRole('button', {name: '让受惊个体退离作业口', exact: true}).click()
        const saved = await defend(before, await started)
        expect(saved.status).toBe('ACTIVE')
        // 等待前一轮页面操作完成再关闭，确保本项验证的是已确认的活动战局，而非结果未知命令。
        await expect(before.getByRole('heading', {name: `第 ${saved.round} 回合`, exact: true})).toBeVisible()
        await context.close()
        context = undefined
        context = await launch()
        const after = context.pages()[0]!
        after.setDefaultTimeout(15_000)
        let writes = 0
        after.on('request', (request) => {
            if (request.method() === 'POST' && new URL(request.url()).pathname.startsWith('/api/battle')) writes++
        })
        const restored = after
            .waitForResponse(
                (response) =>
                    response.url().endsWith('/api/battle') && response.request().method() === 'GET' && response.ok(),
            )
            .then((response) => response.json())
        // 直接访问地图仍须由正式路由守卫返回战场；恢复不得新开战局或自动执行回合。
        await after.goto('/world')
        await after.waitForURL('**/battle')
        const state: Battle = (await restored).battle
        expect(state).toEqual(saved)
        expect(writes).toBe(0)
        const continued = await defend(after, state)
        expect(continued.id).toBe(saved.id)
        expect(continued.round).toBeGreaterThan(saved.round)
        expect(continued.status).toBe('ACTIVE')
        // 先截断尚未送达的命令，再通过重启后的正式GET证明整份战局完全未变。
        await defend(after, continued, 'before-server')
        await expect(after.getByRole('button', {name: '重试原回合', exact: true})).toBeEnabled()
        const unsentBody = lostBody
        await context.close()
        context = undefined
        context = await launch()
        const unsentPage = context.pages()[0]!
        unsentPage.setDefaultTimeout(15_000)
        let unsentWrites = 0
        unsentPage.on('request', (request) => {
            if (request.method() === 'POST' && new URL(request.url()).pathname.startsWith('/api/battle')) unsentWrites++
        })
        const unchanged = unsentPage
            .waitForResponse(
                (response) =>
                    response.url().endsWith('/api/battle') && response.request().method() === 'GET' && response.ok(),
            )
            .then((response) => response.json())
        await unsentPage.goto('/world')
        await unsentPage.waitForURL('**/battle')
        expect((await unchanged).battle).toEqual(continued)
        await expect(unsentPage.getByRole('button', {name: '重试原回合', exact: true})).toBeEnabled()
        expect(unsentWrites).toBe(0)
        // 玩家第一次手动重试才真正结算；再丢弃回执，保留原有“已结算但未确认”的第二次重启检查。
        const committed = new Promise<Battle>((resolve, reject) => {
            void unsentPage
                .route(
                    `**/api/battle/${saved.id}/turn`,
                    async (route) => {
                        try {
                            expect(route.request().postDataJSON()).toEqual(unsentBody)
                            const response = await route.fetch()
                            expect(response.ok()).toBeTruthy()
                            const result: Battle = await response.json()
                            await route.abort('connectionreset')
                            resolve(result)
                        } catch (cause) {
                            reject(cause)
                        }
                    },
                    {times: 1},
                )
                .catch(reject)
        })
        await unsentPage.getByRole('button', {name: '重试原回合', exact: true}).click()
        const unknown = await committed
        expect(unknown.status).toBe('ACTIVE')
        await expect(unsentPage.getByRole('button', {name: '重试原回合', exact: true})).toBeEnabled()
        await context.close()
        context = undefined
        context = await launch()
        const retryPage = context.pages()[0]!
        retryPage.setDefaultTimeout(15_000)
        let replayWrites = 0
        retryPage.on('request', (request) => {
            if (request.method() === 'POST' && new URL(request.url()).pathname.startsWith('/api/battle')) replayWrites++
        })
        await retryPage.goto('/world')
        await retryPage.waitForURL('**/battle')
        await expect(retryPage.getByRole('button', {name: '重试原回合', exact: true})).toBeEnabled()
        expect(replayWrites).toBe(0)
        const replay = retryPage
            .waitForResponse(
                (response) =>
                    response.url().endsWith(`/api/battle/${saved.id}/turn`) && response.request().method() === 'POST',
            )
            .then(async (response) => {
                expect(response.request().postDataJSON()).toEqual(lostBody)
                expect(response.ok()).toBeTruthy()
                expect(await response.json()).toEqual(unknown)
            })
        await retryPage.getByRole('button', {name: '重试原回合', exact: true}).click()
        await replay
        await expect(retryPage.getByRole('button', {name: '重试原回合', exact: true})).toHaveCount(0)
        console.log('战斗命令未送达及已结算响应丢失，两次跨进程恢复原负载、原回执通过')
        // 正常认输以释放战局，后续自然路线继续验证任务未完成、恢复及重新挑战。
        await expect(retryPage.getByRole('heading', {name: `第 ${unknown.round} 回合`, exact: true})).toBeVisible()
        await retryPage.getByRole('button', {name: '认输', exact: true}).click()
        // 认输也是正式终局回合：真实执行后丢弃响应，重启到战斗页时必须仍能确认原请求。
        let terminalBody: unknown
        const surrendered = new Promise<Battle>((resolve, reject) => {
            void retryPage
                .route(
                    `**/api/battle/${saved.id}/turn`,
                    async (route) => {
                        try {
                            terminalBody = route.request().postDataJSON()
                            const response = await route.fetch()
                            expect(response.ok()).toBeTruthy()
                            const result: Battle = await response.json()
                            await route.abort('connectionreset')
                            resolve(result)
                        } catch (cause) {
                            reject(cause)
                        }
                    },
                    {times: 1},
                )
                .catch(reject)
        })
        await retryPage.getByRole('button', {name: '确认认输', exact: true}).click()
        const result: Battle = await surrendered
        expect(result.status).toBe('SURRENDERED')
        // 结果未知时不能让认输弹窗继续遮挡重试；这条断言锁定曾实际复现的交互缺陷。
        await expect(retryPage.getByRole('dialog', {name: '确认认输', exact: true})).toHaveCount(0)
        await expect(retryPage.getByRole('button', {name: '重试原回合', exact: true})).toBeEnabled()
        await context.close()
        context = undefined
        context = await launch()
        const terminal = context.pages()[0]!
        terminal.setDefaultTimeout(15_000)
        let terminalWrites = 0
        terminal.on('request', (request) => {
            if (request.method() === 'POST' && new URL(request.url()).pathname.startsWith('/api/battle'))
                terminalWrites++
        })
        // 终局不再锁定地图，因此显式进入正常战斗页核对终局回执，不把活动战局的路由断言套用于此处。
        await terminal.goto('/battle')
        await expect(terminal.getByRole('heading', {name: '已认输', exact: true})).toBeVisible()
        await expect(terminal.getByRole('button', {name: '重试原回合', exact: true})).toBeEnabled()
        expect(terminalWrites).toBe(0)
        const terminalReplay = terminal
            .waitForResponse(
                (response) =>
                    response.url().endsWith(`/api/battle/${saved.id}/turn`) && response.request().method() === 'POST',
            )
            .then(async (response) => {
                expect(response.ok()).toBeTruthy()
                expect(response.request().postDataJSON()).toEqual(terminalBody)
                expect(await response.json()).toEqual(result)
            })
        await terminal.getByRole('button', {name: '重试原回合', exact: true}).click()
        await terminalReplay
        await expect(terminal.getByRole('button', {name: '重试原回合', exact: true})).toHaveCount(0)
        await terminal.reload()
        await expect(terminal.getByRole('heading', {name: '已认输', exact: true})).toBeVisible()
        console.log('认输终局回合响应丢失跨进程恢复、手动原请求确认及刷新通过')
        console.log('活动战局跨进程恢复同一完整状态、路由锁定及继续回合通过')
        return result
    } finally {
        try {
            await context?.close()
        } finally {
            // 配置中含临时会话凭据，严格限制删除目标为本用例固定子目录，结束后不保留。
            expect(resolve(profile)).toBe(resolve(testInfo.outputDir, 'active-battle-profile'))
            rmSync(profile, {recursive: true, force: true})
        }
    }
}
