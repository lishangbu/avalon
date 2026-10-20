import {chromium, devices, expect, type BrowserContext, type Page, type TestInfo} from '@playwright/test'
import {rmSync} from 'node:fs'
import {resolve} from 'node:path'

/** 使用真实浏览器进程与正式命令验证结果未知时的恢复，不注入令牌、请求草稿或服务端事实。 */
export async function verifySceneProcessRestart(
    account: string,
    scene: {id: string; label: string; success: string},
    testInfo: TestInfo,
) {
    const profile = testInfo.outputPath('pending-scene-profile')
    const endpoint = `**/api/scene-actions/${scene.id}`
    const bodies: unknown[] = []
    let committed: unknown
    let context: BrowserContext | undefined
    const launch = () =>
        chromium.launchPersistentContext(profile, {
            ...devices[testInfo.project.name === 'mobile' ? 'Pixel 7' : 'Desktop Chrome'],
            baseURL: process.env.AVALON_BASE_URL ?? 'http://localhost:8080',
            executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE,
        })
    const row = (page: Page) =>
        page.locator('section[aria-label="现场行动"] > div.row').filter({
            has: page.getByText(scene.label, {exact: true}),
        })
    try {
        context = await launch()
        let before = context.pages()[0]!
        before.setDefaultTimeout(15_000)
        await before.goto('/login')
        await before.getByLabel('账号', {exact: true}).fill(account)
        await before.getByLabel('密码', {exact: true}).fill('Natural-http-1234')
        await before.getByRole('button', {name: '进入冒险', exact: true}).click()
        await before.waitForURL('**/world')
        // 第二个页面使用同一真实会话与localStorage，专门验证storage事件同步，而不是复制页面内存。
        const peer = await context.newPage()
        peer.setDefaultTimeout(15_000)
        await peer.goto('/world')
        await expect(row(peer).getByRole('button', {name: scene.label, exact: true})).toBeVisible()
        // 第一阶段在浏览器请求边界直接中止，不调用fetch或continue，确保写命令未交给服务端。
        // 页面失败后会重新正式读取现场状态；该响应必须仍未完成，不能仅凭错误提示推定未执行。
        await before.route(
            endpoint,
            async (route) => {
                bodies.push(route.request().postDataJSON())
                await route.abort('connectionreset')
            },
            {times: 1},
        )
        const unchanged = before
            .waitForResponse(
                (response) =>
                    response.url().endsWith('/api/scene-actions') &&
                    response.request().method() === 'GET' &&
                    response.ok(),
            )
            .then((response) => response.json())
        await row(before).getByRole('button', {name: scene.label, exact: true}).click()
        await expect(before.getByRole('button', {name: '重试原操作', exact: true})).toBeEnabled()
        // 第一页落盘后第二页只应显示待确认命令；同步事件不能越权自动发起第二次POST。
        await expect(peer.getByRole('button', {name: '重试原操作', exact: true})).toBeEnabled()
        // 原行动标题仍可展示，方便玩家理解上下文；真正的写按钮已因pending状态禁用。
        await expect(peer.getByRole('button', {name: scene.label, exact: true})).toBeDisabled()
        const uncommitted = (await unchanged).actions.find((action: {id: string}) => action.id === scene.id)
        expect(uncommitted).toMatchObject({id: scene.id, completed: false})
        expect(uncommitted.recordedAt ?? null).toBeNull()
        await expect(row(before).locator('time')).toHaveCount(0)
        await context.close()
        context = undefined
        context = await launch()
        before = context.pages()[0]!
        before.setDefaultTimeout(15_000)
        await before.route(endpoint, async (route) => {
            if (route.request().method() !== 'POST') return route.continue()
            bodies.push(route.request().postDataJSON())
            // 服务端先真实执行，再只丢弃浏览器响应；此时请求仍须由页面自身保存，测试不写localStorage。
            const response = await route.fetch()
            expect(response.ok()).toBe(true)
            committed = await response.json()
            await route.abort('connectionreset')
        })
        // 同一配置重启后仍未提交；这次手动重试才首次到达服务端，再丢弃回执以覆盖第二种断线窗口。
        await before.goto('/world')
        await expect(before.getByRole('button', {name: '重试原操作', exact: true})).toBeEnabled()
        await expect(row(before).locator('time')).toHaveCount(0)
        expect(bodies).toHaveLength(1)
        await before.getByRole('button', {name: '重试原操作', exact: true}).click()
        const retry = before.getByRole('button', {name: '重试原操作', exact: true})
        await expect(retry).toBeVisible()
        await expect(retry).toBeEnabled()
        await expect(row(before).getByText('已完成', {exact: true})).toBeVisible()
        const recordedAt = await row(before).locator('time').getAttribute('datetime')
        expect(recordedAt).toBeTruthy()
        expect(bodies).toHaveLength(2)
        expect(bodies[1]).toEqual(bodies[0])
        // 关闭独立持久化context会结束其浏览器进程；重新启动时不复制内存、storageState或令牌。
        await context.close()
        context = undefined
        context = await launch()
        const after = context.pages()[0]!
        after.setDefaultTimeout(15_000)
        await after.route(endpoint, async (route) => {
            if (route.request().method() === 'POST') bodies.push(route.request().postDataJSON())
            await route.continue()
        })
        await after.goto('/world')
        await expect(after.getByRole('button', {name: '重试原操作', exact: true})).toBeEnabled()
        await expect(row(after).locator('time')).toHaveAttribute('datetime', recordedAt!)
        expect(bodies).toHaveLength(2)
        const replay = after
            .waitForResponse(
                (response) =>
                    response.url().endsWith(`/api/scene-actions/${scene.id}`) && response.request().method() === 'POST',
            )
            .then((response) => response.json())
        await after.getByRole('button', {name: '重试原操作', exact: true}).click()
        expect(await replay).toEqual(committed)
        expect(bodies).toHaveLength(3)
        expect(bodies[2]).toEqual(bodies[0])
        await expect(after.getByRole('button', {name: '重试原操作', exact: true})).toHaveCount(0)
        await after.unroute(endpoint)
        await after.reload()
        await expect(row(after).locator('time')).toHaveAttribute('datetime', recordedAt!)
        await row(after).getByRole('button', {name: '回看记录', exact: true}).click()
        await expect(after.getByRole('status').filter({hasText: scene.success})).toBeVisible()
        console.log(`${scene.id}请求未到服务端及已结算响应丢失，两次跨进程恢复原负载重试通过`)
    } finally {
        try {
            await context?.close()
        } finally {
            // 临时配置包含测试账号会话，清理前验证固定目录归属，不能留下凭据或删除其他输出路径。
            expect(resolve(profile)).toBe(resolve(testInfo.outputDir, 'pending-scene-profile'))
            rmSync(profile, {recursive: true, force: true})
        }
    }
}
