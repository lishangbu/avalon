import {clickWorldAction} from './support/npc'
// 夹具只解锁商店、准备金币；购买、穿戴、出售和销毁全部通过实际页面执行。
import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {execFileSync} from 'node:child_process'
import {auditLayout} from './support/layout-audit'

test('装备补购、绑定后出售与销毁确认', async ({page}, testInfo) => {
    test.skip(process.env.AVALON_LOCAL_FIXTURES !== '1', '需要显式启用本地数据库夹具')
    const suffix = randomUUID().replaceAll('-', '').slice(0, 12)
    const name = `装备${suffix}`
    const fixtureId = BigInt(`0x${suffix}`)
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    await page.goto('/')
    await page.getByRole('button', {name: '创建账号', exact: true}).click()
    await page.getByLabel('账号', {exact: true}).fill(`gear_${suffix}`)
    await page.getByLabel('密码', {exact: true}).fill('Commerce-browser-123')
    await page.getByRole('button', {name: '注册并进入'}).click()
    await page.getByLabel('角色名', {exact: true}).fill(name)
    await page.getByRole('button', {name: '踏上旅途'}).click()
    await expect(page.getByRole('button', {name: '晨露村家门，当前位置'})).toBeVisible()
    await page.getByRole('button', {name: '角色', exact: true}).click()
    const health = page
        .getByRole('region', {name: '常驻属性'})
        .locator('div')
        .filter({has: page.locator('dt').getByText('生命', {exact: true})})
        .locator('dd')
    await expect(health).toHaveText('176')
    await expect(page.getByRole('region', {name: '职业技能'})).toContainText('英勇打击')
    await expect(page.getByRole('region', {name: '职业技能'})).not.toContainText('盾墙')
    await page.getByRole('button', {name: '冒险', exact: true}).click()
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
            input: `BEGIN;
            UPDATE player_character SET gold = 100, location = 'WEAPON_SHOP' WHERE name = '${name}';
            INSERT INTO character_fact (id, character_id, code, created_at)
                SELECT ${fixtureId}, id, 'QUEST:O003', now() FROM player_character WHERE name = '${name}';
            COMMIT;`,
            stdio: ['pipe', 'pipe', 'pipe'],
        },
    )
    await page.reload()
    const current = page.locator('.ion-page').filter({visible: true})
    await clickWorldAction(page, '商店')
    await current
        .locator('article')
        .filter({hasText: '初始上衣'})
        .getByRole('button', {name: '购买 · 20 金币', exact: true})
        .click()
    await expect(current.getByText('持有 80 金币', {exact: true})).toBeVisible()
    await page.getByRole('button', {name: '返回', exact: true}).click()
    await page.getByRole('button', {name: '背包', exact: true}).click()
    await page.getByRole('button', {name: '装备', exact: true}).click()
    const sheet = (title: string) =>
        page.locator('ion-modal').filter({has: page.locator('ion-title').getByText(title, {exact: true})})
    const body = sheet('初始上衣')
    await current.getByRole('button', {name: /初始上衣/}).click()
    await expect(page.getByRole('dialog', {name: '初始上衣', exact: true})).toBeVisible()
    await body.getByRole('button', {name: '穿戴', exact: true}).click()
    await expect(body).not.toBeVisible()
    await page.getByRole('button', {name: '角色', exact: true}).click()
    await expect(health).toHaveText('206')
    await page.screenshot({path: testInfo.outputPath('character-profile.png'), fullPage: true, animations: 'disabled'})
    await page.getByRole('button', {name: '背包', exact: true}).click()
    await current.getByRole('button', {name: /初始上衣/}).click()
    await expect(body.getByText(/普通 · 已绑定/)).toBeVisible()
    await expect(body.getByRole('button', {name: '出售', exact: true})).toHaveCount(0)
    await body.getByRole('button', {name: '卸下', exact: true}).click()
    await expect(body).not.toBeVisible()
    await page.getByRole('button', {name: '角色', exact: true}).click()
    await expect(health).toHaveText('176')
    await page.getByRole('button', {name: '背包', exact: true}).click()
    await current.getByRole('button', {name: /初始上衣/}).click()
    await body.getByRole('button', {name: '出售', exact: true}).click()
    const sell = sheet('确认出售')
    await expect(sell.getByText('出售获得 4 金币。', {exact: true})).toBeVisible()
    // 浏览器返回关闭最上层确认，保留装备详情；第二次返回才关闭详情。
    await page.goBack()
    await expect(sell).not.toBeVisible()
    await expect(body).toBeVisible()
    await expect(page).toHaveURL(/\/inventory$/)
    await page.goBack()
    await expect(body).not.toBeVisible()
    await expect(page).toHaveURL(/\/inventory$/)
    await current.getByRole('button', {name: /初始上衣/}).click()
    await body.getByRole('button', {name: '出售', exact: true}).click()
    // 延迟真实请求以检查 busy 返回保护，响应仍来自生产接口。
    let releaseRequest!: () => void
    const gate = new Promise<void>((resolve) => {
        releaseRequest = resolve
    })
    await page.route('**/api/inventory/equipment/commerce', async (route) => {
        if (route.request().method() === 'POST') await gate
        await route.continue()
    })
    await sell.getByRole('button', {name: '确认出售', exact: true}).click()
    await expect(sell.getByRole('button', {name: '确认出售', exact: true})).toBeDisabled()
    await page.goBack()
    await expect(sell).toBeVisible()
    await expect(page).toHaveURL(/\/inventory$/)
    releaseRequest()
    await expect(sell).not.toBeVisible()
    await expect(body).not.toBeVisible()
    await expect(current.getByRole('button', {name: /初始上衣/})).toHaveCount(0)
    await page.unroute('**/api/inventory/equipment/commerce')
    await page.getByRole('button', {name: '冒险', exact: true}).click()
    await expect(current.getByText('84 金币', {exact: true})).toBeVisible()
    await clickWorldAction(page, '商店')
    await current
        .locator('article')
        .filter({hasText: '初始鞋'})
        .getByRole('button', {name: '购买 · 10 金币', exact: true})
        .click()
    await expect(current.getByText('持有 74 金币', {exact: true})).toBeVisible()
    await page.getByRole('button', {name: '返回', exact: true}).click()
    await page.getByRole('button', {name: '背包', exact: true}).click()
    await page.getByRole('button', {name: '装备', exact: true}).click()
    await current.getByRole('button', {name: /初始鞋/}).click()
    await expect(page.getByRole('dialog', {name: '初始鞋', exact: true})).toBeVisible()
    await sheet('初始鞋').getByRole('button', {name: '销毁', exact: true}).click()
    const destroy = sheet('确认销毁')
    await expect(destroy.getByText('补购价 10 金币', {exact: true})).toBeVisible()
    await page.screenshot({
        path: testInfo.outputPath('destroy-confirmation.png'),
        fullPage: true,
        animations: 'disabled',
    })
    await auditLayout(page, testInfo, 'destroy-confirmation')
    await destroy.getByRole('button', {name: '确认销毁', exact: true}).click()
    await expect(destroy).not.toBeVisible()
    await expect(current.getByRole('button', {name: /初始鞋/})).toHaveCount(0)
    await page.reload()
    await page.getByRole('button', {name: '装备', exact: true}).click()
    await expect(current.getByText('暂无库存装备', {exact: true})).toBeVisible()
    await page.getByRole('button', {name: '冒险', exact: true}).click()
    await expect(current.getByText('74 金币', {exact: true})).toBeVisible()
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    expect(errors).toEqual([])
})
