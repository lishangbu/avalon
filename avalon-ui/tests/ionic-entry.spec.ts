import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'

/** 新页面仍走真实数据库，覆盖注册、创角、相邻移动、主题、设备与重新登录。 */
test('Ionic 浏览器冒险入口闭环', async ({page}, testInfo) => {
    const suffix = randomUUID().replaceAll('-', '').slice(0, 12)
    const username = `ionic_${suffix}`,
        password = 'Browser-test-123456'
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    // 未认证的受保护 API 先经过认证过滤器；已认证未知路由的 404 另由 HTTP 契约测试覆盖。
    expect((await page.request.get('/api/not-a-route')).status()).toBe(401)
    await page.goto('/')
    await page.getByRole('button', {name: '创建账号', exact: true}).click()
    await page.getByLabel('账号', {exact: true}).fill(username)
    await page.getByLabel('密码', {exact: true}).fill(password)
    await page.getByRole('button', {name: '注册并进入'}).click()
    await expect(page.getByRole('heading', {name: '启程'})).toBeVisible()
    await page.getByLabel('角色名', {exact: true}).fill(`旅人${suffix}`)
    await page.getByRole('button', {name: '踏上旅途'}).click()
    await expect(page.getByRole('button', {name: '晨露村家门，当前位置'})).toBeVisible()
    await page.getByRole('button', {name: '展开全图'}).click()
    // 全图保留地点间距；即使存在后续区域，也不能靠压缩画布让节点相互遮挡。
    const overlaps = await page
        .getByRole('region', {name: '冒险地图'})
        .locator('button')
        .evaluateAll((nodes) => {
            const boxes = nodes.map((node) => node.getBoundingClientRect())
            return boxes.some((box, index) =>
                boxes
                    .slice(index + 1)
                    .some(
                        (other) =>
                            box.left < other.right &&
                            box.right > other.left &&
                            box.top < other.bottom &&
                            box.bottom > other.top,
                    ),
            )
        })
    expect(overlaps).toBe(false)
    if (testInfo.project.name === 'mobile') {
        const original = page.viewportSize()!
        for (const viewport of [
            {width: 320, height: 640},
            {width: 844, height: 390},
        ]) {
            await page.setViewportSize(viewport)
            expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
            await page.screenshot({path: testInfo.outputPath(`full-map-${viewport.width}.png`), fullPage: true})
        }
        await page.setViewportSize(original)
    }
    await page.getByRole('button', {name: '村中广场', exact: true}).click()
    await expect(page.getByText('道路尚未相连', {exact: true})).toBeVisible()
    await page.goBack()
    await expect(page.locator('ion-modal').filter({visible: true})).toHaveCount(0)
    await expect(page).toHaveURL(/\/world$/)
    await expect(page.getByRole('button', {name: '晨露村家门，当前位置'})).toBeVisible()
    // 再次打开并显式关闭，不能积累幽灵历史位置。
    await page.getByRole('button', {name: '村中广场', exact: true}).click()
    await expect(page.getByText('道路尚未相连', {exact: true})).toBeVisible()
    await page.keyboard.press('Escape')
    await expect(page.locator('ion-modal').filter({visible: true})).toHaveCount(0)
    await page.getByRole('button', {name: '石板路', exact: true}).click()
    await expect(page.getByRole('button', {name: '石板路，当前位置'})).toBeVisible()
    await expect(page.locator('ion-modal').filter({visible: true})).toHaveCount(0)
    await page.reload()
    await expect(page.getByRole('button', {name: '石板路，当前位置'})).toBeVisible()
    // 当前地点不重复打开详情；相邻地点直接提交，不自动弹出详情。
    await page.getByRole('button', {name: '石板路，当前位置'}).click()
    const location = page.locator('ion-modal').filter({visible: true})
    await expect(page.getByRole('button', {name: '查看地点', exact: true})).toHaveCount(0)
    await expect(location).toHaveCount(0)
    let releaseMove!: () => void
    const gate = new Promise<void>((resolve) => {
        releaseMove = resolve
    })
    let moves = 0
    await page.route('**/api/world/move', async (route) => {
        moves++
        await gate
        await route.continue()
    })
    await page.getByRole('button', {name: '晨露村家门', exact: true}).click()
    await expect(page.getByRole('button', {name: '晨露村家门', exact: true})).toBeDisabled()
    await expect(location).toHaveCount(0)
    expect(moves).toBe(1)
    releaseMove()
    await expect(page.getByRole('button', {name: '晨露村家门，当前位置'})).toBeVisible()
    await page.unroute('**/api/world/move')
    await page.getByRole('button', {name: '石板路', exact: true}).click()
    await expect(page.getByRole('button', {name: '石板路，当前位置'})).toBeVisible()
    await expect(location).toHaveCount(0)
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    await page.screenshot({path: testInfo.outputPath('map-light.png'), fullPage: true})
    await page.getByRole('button', {name: '角色', exact: true}).click()
    await expect(page.getByText('Web 浏览器', {exact: true})).toBeVisible()
    await page.getByRole('combobox', {name: '主题', exact: true}).selectOption('dark')
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
    await page.screenshot({path: testInfo.outputPath('character-dark.png'), fullPage: true})
    await page.getByRole('button', {name: '退出登录', exact: true}).click()
    await page.locator('ion-modal').getByRole('button', {name: '退出登录', exact: true}).click()
    await expect(page.getByRole('button', {name: '进入冒险', exact: true})).toBeVisible()
    expect(await page.evaluate(() => localStorage.getItem('avalon.refresh-token'))).toBeNull()
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
    await page.getByLabel('账号', {exact: true}).fill(username)
    await page.getByLabel('密码', {exact: true}).fill(password)
    await page.getByRole('button', {name: '进入冒险', exact: true}).click()
    await expect(page.getByRole('button', {name: '石板路，当前位置'})).toBeVisible()
    expect(errors).toEqual([])
})
