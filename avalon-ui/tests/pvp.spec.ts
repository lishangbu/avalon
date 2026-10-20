import {expect, test, type Page} from '@playwright/test'

test.use({actionTimeout: 15_000})
test('免战、共享回合与采集受袭跳转', async ({browser, baseURL, viewport, isMobile, hasTouch, userAgent}) => {
    test.skip(!process.env.AVALON_PVP_ATTACKER, '由PvpBrowserIT准备隔离角色')
    test.setTimeout(120_000)
    const options = {baseURL, viewport, isMobile, hasTouch, userAgent}
    const first = await browser.newContext(options),
        second = await browser.newContext(options)
    const attacker = await first.newPage(),
        defender = await second.newPage()
    const errors: string[] = []
    for (const page of [attacker, defender]) page.on('pageerror', (error) => errors.push(error.message))
    async function login(page: Page, name: string) {
        await page.goto('/')
        await page.getByLabel('账号', {exact: true}).fill(name)
        await page.getByLabel('密码', {exact: true}).fill('Pvp-browser-1234')
        await page.getByRole('button', {name: '进入冒险', exact: true}).click()
        await expect(page).toHaveURL(/\/world$/)
    }
    try {
        await login(attacker, process.env.AVALON_PVP_ATTACKER!)
        await login(defender, process.env.AVALON_PVP_DEFENDER!)
        await defender.goto('/life/gathering')
        await expect(defender.getByRole('heading', {name: '正在采集', exact: true})).toBeVisible()
        await attacker.bringToFront()
        const protection = attacker.getByRole('region', {name: '免战与附近训练师'})
        await protection.getByRole('button', {name: '使用免战牌（30分钟）', exact: true}).click()
        await expect(protection.getByText(/免战保护至/)).toBeVisible()
        await protection
            .locator('.row')
            .filter({hasText: process.env.AVALON_PVP_DEFENDER!})
            .getByRole('button', {name: '攻击', exact: true})
            .click()
        await attacker.getByRole('button', {name: '确认攻击', exact: true}).click()
        await expect(attacker).toHaveURL(/\/battle$/)
        await expect(defender).toHaveURL(/\/battle$/, {timeout: 15_000})
        for (const page of [attacker, defender]) {
            await expect(page.getByRole('heading', {name: '第 1 回合', exact: true})).toBeVisible()
            await expect(page.getByText('捕捉信息', {exact: true})).toHaveCount(0)
        }
        await attacker.getByRole('button', {name: '提交本回合', exact: true}).click()
        await expect(attacker.getByText('行动已锁定，等待对方提交或本轮结束。', {exact: false})).toBeVisible()
        await expect(attacker.getByRole('button', {name: '提交本回合', exact: true})).toBeDisabled()
        await defender.getByRole('button', {name: '提交本回合', exact: true}).click()
        for (const page of [attacker, defender])
            await expect(page.getByRole('heading', {name: '第 2 回合', exact: true})).toBeVisible({timeout: 15_000})
        await defender.getByRole('button', {name: '认输', exact: true}).click()
        await defender.getByRole('button', {name: '确认认输', exact: true}).click()
        await expect(attacker.getByRole('heading', {name: '战斗胜利', exact: true})).toBeVisible({timeout: 15_000})
        await expect(defender.getByRole('heading', {name: '队伍倒下', exact: true})).toBeVisible()
        expect(errors).toEqual([])
    } finally {
        await first.close()
        await second.close()
    }
})
