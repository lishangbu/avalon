import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {execFileSync} from 'node:child_process'

/** 使用真实图鉴长列表验证 Ionic 内部滚动容器，不以 window 滚动替代手机页面行为。 */
test('伙伴分类与滚动位置在主入口往返后恢复', async ({page}) => {
    await page.setViewportSize({width: 390, height: 640})
    await page.goto('/')
    await page.getByRole('button', {name: '创建账号', exact: true}).click()
    const suffix = randomUUID().replaceAll('-', '').slice(0, 10)
    await page.getByLabel('账号', {exact: true}).fill(`scroll_${suffix}`)
    await page.getByLabel('密码', {exact: true}).fill('Scroll-browser-123')
    await page.getByRole('button', {name: '注册并进入'}).click()
    await page.getByLabel('角色名', {exact: true}).fill(`旅人${suffix}`)
    await page.getByRole('button', {name: '踏上旅途'}).click()
    await page.getByRole('button', {name: '伙伴', exact: true}).click()
    await page.getByRole('button', {name: '图鉴', exact: true}).click()
    const content = () => page.locator('div.ion-page').filter({visible: true}).locator('ion-content')
    await expect(page.getByText('尚未收集', {exact: true}).first()).toBeVisible()
    await content().evaluate(async (element) => {
        const scroll = await (element as HTMLIonContentElement).getScrollElement()
        scroll.scrollTop = 280
    })
    const top = () =>
        content().evaluate(async (element) => (await (element as HTMLIonContentElement).getScrollElement()).scrollTop)
    await expect.poll(top).toBeGreaterThan(250)
    await page.getByRole('button', {name: '背包', exact: true}).click()
    await page.getByRole('button', {name: '伙伴', exact: true}).click()
    await expect.poll(top).toBeGreaterThan(250)
    // 新的随身分类从顶部开始，再回到图鉴时保留该分类自己的位置。
    await page.getByRole('button', {name: '随身', exact: true}).click()
    await expect.poll(top).toBe(0)
    await page.getByRole('button', {name: '图鉴', exact: true}).click()
    await expect.poll(top).toBeGreaterThan(200)
})

test('满仓库详情返回及延迟刷新保留列表位置', async ({page}, testInfo) => {
    test.skip(process.env.AVALON_LOCAL_FIXTURES !== '1', '需要显式启用本地数据库夹具')
    await page.setViewportSize({width: 320, height: 640})
    const suffix = randomUUID().replaceAll('-', '').slice(0, 10)
    const name = `仓库${suffix}`,
        id = BigInt(`0x${suffix}`) * 100n
    await page.goto('/')
    await page.getByRole('button', {name: '创建账号', exact: true}).click()
    await page.getByLabel('账号', {exact: true}).fill(`store_${suffix}`)
    await page.getByLabel('密码', {exact: true}).fill('Storage-browser-123')
    await page.getByRole('button', {name: '注册并进入'}).click()
    await page.getByLabel('角色名', {exact: true}).fill(name)
    await page.getByRole('button', {name: '踏上旅途'}).click()
    await expect(page.getByRole('button', {name: '晨露村家门，当前位置', exact: true})).toBeVisible()
    // 只预置仓库个体；详情导航、锁定、读取与返回均使用正式页面和接口。
    execFileSync(
        'docker',
        [
            'exec',
            '-i',
            process.env.AVALON_TEST_DB_CONTAINER ?? 'avalon-postgres-1',
            'psql',
            '-U',
            process.env.AVALON_TEST_DB_USER ?? 'avalon',
            '-d',
            process.env.AVALON_TEST_DB_NAME ?? 'avalon',
            '-v',
            'ON_ERROR_STOP=1',
        ],
        {
            input: `INSERT INTO owned_creature
            (id, character_id, source, species, name, element, level, experience, ability_slot, ability, skills, party_position)
            SELECT ${id} + n, c.id, 'scroll-' || n, 'STONE_GOLEM', '小岩怪', '地', 1, 0,
                'H', '守护', '["碎岩击","掩护"]', 100 + n
            FROM player_character c CROSS JOIN generate_series(0, 29) n WHERE c.name = '${name}';`,
            stdio: ['pipe', 'pipe', 'pipe'],
        },
    )
    await page.reload()
    await page.getByRole('button', {name: '伙伴', exact: true}).click()
    await page.getByRole('button', {name: '仓库', exact: true}).click()
    const current = () => page.locator('div.ion-page').filter({visible: true})
    const top = () =>
        current()
            .filter({has: page.locator('[aria-label="伙伴分类"]')})
            .locator('ion-content')
            .evaluate(async (element) => (await (element as HTMLIonContentElement).getScrollElement()).scrollTop)
    await expect(current().locator('.pet-link')).toHaveCount(30)
    const target = current().locator('.pet-link').nth(15)
    await target.scrollIntoViewIfNeeded()
    const position = await top()
    expect(position).toBeGreaterThan(1000)
    await target.click()
    await expect(current().getByText('当前位置：仓库', {exact: true})).toBeVisible()
    await current().getByRole('button', {name: '锁定', exact: true}).click()
    await expect(current().getByRole('button', {name: '解锁', exact: true})).toBeVisible()
    await page.getByRole('button', {name: '返回', exact: true}).click()
    await expect(current().locator('.pet-link')).toHaveCount(30)
    await expect.poll(async () => Math.abs((await top()) - position)).toBeLessThan(5)
    await page.getByRole('button', {name: '背包', exact: true}).click()
    // 延迟真实响应而非伪造列表，确认缓存显示与后续刷新不会重置滚动位置。
    let release!: () => void
    const gate = new Promise<void>((resolve) => {
        release = resolve
    })
    await page.route(
        '**/api/creatures',
        async (route) => {
            const response = await route.fetch()
            await gate
            await route.fulfill({response})
        },
        {times: 1},
    )
    const request = page.waitForRequest((request) => new URL(request.url()).pathname === '/api/creatures')
    const refreshed = page.waitForResponse((response) => new URL(response.url()).pathname === '/api/creatures')
    await page.getByRole('button', {name: '伙伴', exact: true}).click()
    await request
    try {
        await expect.poll(async () => Math.abs((await top()) - position)).toBeLessThan(5)
    } finally {
        release()
    }
    expect((await refreshed).ok()).toBe(true)
    await expect(current().locator('.pet-link')).toHaveCount(30)
    await expect.poll(async () => Math.abs((await top()) - position)).toBeLessThan(5)
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    await page.screenshot({path: testInfo.outputPath('warehouse-restored.png'), animations: 'disabled'})
    // 推进浏览器计时器触发查询默认的五分钟回收，保留本次登录的滚动记忆。
    // 再次进入时必须先看到空列表，防止把有缓存的后台刷新误当成首次加载。
    await page.clock.install()
    await page.getByRole('button', {name: '背包', exact: true}).click()
    await expect(page.locator('.pet-link')).toHaveCount(0)
    await page.clock.fastForward(310_000)
    let releaseUncached!: () => void
    const uncachedGate = new Promise<void>((resolve) => {
        releaseUncached = resolve
    })
    await page.route(
        '**/api/creatures',
        async (route) => {
            const response = await route.fetch()
            await uncachedGate
            await route.fulfill({response})
        },
        {times: 1},
    )
    const loading = page.waitForRequest((request) => new URL(request.url()).pathname === '/api/creatures')
    await page.getByRole('button', {name: '伙伴', exact: true}).click()
    await loading
    try {
        await expect(current().locator('.pet-link')).toHaveCount(0)
    } finally {
        releaseUncached()
    }
    await expect(current().locator('.pet-link')).toHaveCount(30)
    await expect.poll(async () => Math.abs((await top()) - position)).toBeLessThan(5)
})
