import {expect, type Page} from '@playwright/test'

/** 浏览器测试也沿人物入口操作，不能为旧用例保留玩家看不到的快捷按钮。 */
export async function closeNpc(page: Page) {
    const sheet = page
        .locator('ion-modal')
        .filter({visible: true})
        .filter({has: page.locator('[data-npc-panel]')})
    if (await sheet.count()) {
        await sheet.getByRole('button', {name: '关闭', exact: true}).click()
        await expect(sheet).toHaveCount(0)
    }
}

export async function revealWorldAction(page: Page, name: string) {
    let action = page.getByRole('button', {name, exact: true}).first()
    if (await action.isVisible()) return action
    await closeNpc(page)
    action = page.getByRole('button', {name, exact: true}).first()
    if (await action.isVisible()) return action
    const people = page.getByRole('region', {name: '在场人物'}).getByRole('button')
    if (new URL(page.url()).pathname !== '/world') return action
    await expect.poll(async () => (await action.isVisible()) || (await people.count()) > 0).toBe(true)
    if (await action.isVisible()) return action
    for (let i = 0; i < (await people.count()); i++) {
        await people.nth(i).click()
        const panel = page.locator('[data-npc-panel]')
        await expect(panel).toBeVisible()
        if (await action.isVisible()) return action
        await closeNpc(page)
    }
    return action
}

export async function clickWorldAction(page: Page, name: string) {
    await (await revealWorldAction(page, name)).click()
    if (name === '冒险' || name === '返回地图') await expect(page).toHaveURL(/\/world$/)
}
