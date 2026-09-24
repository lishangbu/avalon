import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'

/** 注入平台安全区和缩小视口只验证网页布局，不冒充操作系统软键盘真机验收。 */
test('安全区和短视口下可以输入、提交并关闭地图弹层', async ({page}, info) => {
    const suffix = randomUUID().replaceAll('-', '').slice(0, 10)
    await page.setViewportSize({width: 390, height: 360})
    await page.goto('/')
    await page.addStyleTag({
        content:
            ':root {--safe-area-inset-top: 24px; --safe-area-inset-bottom: 34px; --safe-area-inset-left: 44px; --safe-area-inset-right: 44px;} ',
    })
    await page.getByRole('button', {name: '创建账号', exact: true}).click()
    const username = page.getByLabel('账号', {exact: true}),
        password = page.getByLabel('密码', {exact: true})
    await username.fill(`inset_${suffix}`)
    await password.fill('Safe-area-browser-123')
    await expect(password).toBeFocused()
    await password.scrollIntoViewIfNeeded()
    await expect(password).toBeInViewport({ratio: 1})
    await page.getByRole('button', {name: '注册并进入'}).click()
    await page.getByLabel('角色名', {exact: true}).fill(`安全${suffix}`)
    await page.getByRole('button', {name: '踏上旅途'}).click()
    await page.setViewportSize({width: 390, height: 740})
    await expect(page.getByRole('button', {name: '晨露村家门，当前位置'})).toBeVisible()
    const nav = page.getByRole('navigation', {name: '主导航'})
    await expect
        .poll(() => nav.evaluate((element) => Number.parseFloat(getComputedStyle(element).paddingBottom)))
        .toBe(34)
    const last = await nav.getByRole('button', {name: '角色', exact: true}).boundingBox()
    expect(last!.y + last!.height).toBeLessThanOrEqual(740 - 34)
    const body = page.locator('div.ion-page').filter({visible: true}).locator('.page-body')
    expect(await body.evaluate((element) => Number.parseFloat(getComputedStyle(element).paddingLeft))).toBe(44)
    expect(await body.evaluate((element) => Number.parseFloat(getComputedStyle(element).paddingRight))).toBe(44)
    await page.getByRole('button', {name: '展开全图', exact: true}).click()
    await page.getByRole('button', {name: '村中广场', exact: true}).click()
    const modal = page.locator('ion-modal').filter({visible: true})
    await expect(modal.getByText('道路尚未相连', {exact: true})).toBeVisible()
    await page.setViewportSize({width: 390, height: 360})
    await expect(modal.getByRole('button', {name: '关闭', exact: true})).toBeInViewport({ratio: 1})
    await page.screenshot({path: info.outputPath('safe-area-short-sheet.png')})
    await modal.getByRole('button', {name: '关闭', exact: true}).click()
    await expect(modal).toHaveCount(0)
    await page.getByRole('button', {name: '石板路', exact: true}).click()
    await expect(page.getByRole('button', {name: '石板路，当前位置'})).toBeVisible()
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
})
