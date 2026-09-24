// 验证地址直接访问、刷新、返回和会话失效边界。主动在服务端撤销会话，保证清理测试经过真实401和刷新失败，而不是模拟成功响应。
import {expect, type Page, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'

/** 仍通过真实账号和 API 验证准入；不伪造服务端角色或会话响应。 */
async function register(page: Page) {
    const suffix = randomUUID().replaceAll('-', '').slice(0, 12)
    await page.getByRole('button', {name: '创建账号', exact: true}).click()
    await page.getByLabel('账号', {exact: true}).fill(`route_${suffix}`)
    await page.getByLabel('密码', {exact: true}).fill('Routing-browser-123')
    await page.getByRole('button', {name: '注册并进入'}).click()
    await expect(page).toHaveURL(/\/character\/create$/)
    return suffix
}

async function createCharacter(page: Page, suffix: string) {
    await page.getByLabel('角色名', {exact: true}).fill(`旅人${suffix}`)
    await page.getByRole('button', {name: '踏上旅途'}).click()
    await expect(page).toHaveURL(/\/world$/)
    await expect(page.getByRole('button', {name: '晨露村家门，当前位置', exact: true})).toBeVisible()
}

test('直接访问与刷新均按服务端角色状态进入页面', async ({page}) => {
    await page.goto('/world')
    await expect(page).toHaveURL(/\/login$/)
    const suffix = await register(page)
    await page.goto('/world')
    await expect(page).toHaveURL(/\/character\/create$/)
    await page.reload()
    await expect(page.getByLabel('角色名', {exact: true})).toBeVisible()
    await createCharacter(page, suffix)
    await page.goto('/character/create')
    await expect(page).toHaveURL(/\/world$/)
    await page.goto('/login')
    await expect(page).toHaveURL(/\/world$/)
    await page.goBack()
    await expect(page).toHaveURL(/\/world$/)
    await expect(page.getByRole('button', {name: '晨露村家门，当前位置', exact: true})).toBeVisible()
})

test('移动弹层打开时会话失效应清理页面与旧账号数据', async ({page}) => {
    let authorization = ''
    page.on('request', (request) => {
        const header = request.headers()['authorization']
        if (header) authorization = header
    })
    await page.goto('/login')
    const suffix = await register(page)
    await createCharacter(page, suffix)
    await page.getByRole('button', {name: '背包', exact: true}).click()
    await page.getByRole('button', {name: '任务', exact: true}).click()
    await page.getByRole('button', {name: '伙伴', exact: true}).click()
    await page.getByRole('button', {name: '图鉴', exact: true}).click()
    await page.getByRole('button', {name: '背包', exact: true}).click()
    await expect(page.getByRole('button', {name: '任务', exact: true})).toHaveAttribute('aria-pressed', 'true')
    await page.getByRole('button', {name: '伙伴', exact: true}).click()
    await expect(page.getByRole('button', {name: '图鉴', exact: true})).toHaveAttribute('aria-pressed', 'true')
    await page.getByRole('button', {name: '冒险', exact: true}).click()
    await page.getByRole('button', {name: '展开全图', exact: true}).click()
    await page.getByRole('button', {name: '村中广场', exact: true}).click()
    const dialog = page.locator('ion-modal').filter({has: page.locator('ion-title', {hasText: '村中广场'})})
    await expect(dialog).toBeVisible()
    // 服务端撤销会话，浏览器内存仍持有旧 Token，下一次操作触发真实 401/刷新失败。
    expect(authorization).not.toBe('')
    const revoked = await page.request.post('/api/auth/logout', {
        headers: {Authorization: authorization},
    })
    expect(revoked.ok()).toBe(true)
    await dialog.getByRole('button', {name: '关闭', exact: true}).click()
    await page.getByRole('button', {name: '石板路', exact: true}).click()
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('dialog')).toHaveCount(0)
    expect(await page.evaluate(() => localStorage.getItem('avalon.refresh-token'))).toBeNull()
    await expect(page.getByText(`旅人${suffix}`, {exact: true})).toHaveCount(0)
    const next = await register(page)
    await expect(page.getByLabel('角色名', {exact: true})).toHaveValue('')
    await expect(page.getByRole('dialog')).toHaveCount(0)
    await createCharacter(page, next)
    await page.getByRole('button', {name: '背包', exact: true}).click()
    await expect(page.getByRole('button', {name: '物品', exact: true})).toHaveAttribute('aria-pressed', 'true')
    await page.getByRole('button', {name: '伙伴', exact: true}).click()
    await expect(page.getByRole('button', {name: '随身', exact: true})).toHaveAttribute('aria-pressed', 'true')
})

test('弹层刷新和历史前进后仍可正常关闭与导航', async ({page}) => {
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    await page.goto('/login')
    const suffix = await register(page)
    await createCharacter(page, suffix)
    const open = async () => {
        // 主入口会重建页面，先等地图就绪，再判断本次页面的全图状态。
        await expect(page.getByRole('button', {name: '晨露村家门，当前位置', exact: true})).toBeVisible()
        const expand = page.getByRole('button', {name: '展开全图', exact: true})
        if (await expand.count()) await expand.click()
        await page.getByRole('button', {name: '村中广场', exact: true}).click()
    }
    const dialog = page.locator('ion-modal').filter({has: page.locator('ion-title', {hasText: '村中广场'})})
    await open()
    await expect(dialog).toBeVisible()
    await page.reload()
    // 临时查看状态不持久化；刷新后不能恢复过期移动确认。
    await expect(page.getByRole('button', {name: '晨露村家门，当前位置', exact: true})).toBeVisible()
    await expect(page.locator('ion-modal:visible')).toHaveCount(0)
    await open()
    await expect(dialog).toBeVisible()
    await page.goBack()
    await expect(dialog).toBeHidden()
    await page.goForward()
    await expect(page).toHaveURL(/\/world$/)
    await expect(page.locator('ion-modal:visible')).toHaveCount(0)
    await open()
    await expect(dialog).toBeVisible()
    await dialog.getByRole('button', {name: '关闭', exact: true}).click()
    await expect(dialog).toBeHidden()
    await page.getByRole('button', {name: '背包', exact: true}).click()
    await expect(page).toHaveURL(/\/inventory$/)
    await page.getByRole('button', {name: '冒险', exact: true}).click()
    await open()
    await dialog.getByRole('button', {name: '关闭', exact: true}).click()
    await page.getByRole('button', {name: '石板路', exact: true}).click()
    await expect(dialog).toBeHidden()
    await expect(page.getByRole('button', {name: '石板路，当前位置', exact: true})).toBeVisible()
    expect(errors).toEqual([])
})
