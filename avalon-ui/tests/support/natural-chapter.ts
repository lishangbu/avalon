import {expect, type Page, type TestInfo} from '@playwright/test'
import {completeFormalOpening} from './formal-opening'

/** 复用唯一正式开篇，通过野外战斗继续到15级，不注入经验或任务事实。 */
export async function completeNaturalChapter(page: Page, info: TestInfo) {
    const journey = await completeFormalOpening(page, info)
    const current = () => page.locator('div.ion-page').filter({visible: true})
    const level = async () =>
        Number((await current().locator('.row-title').first().innerText()).match(/Lv\.(\d+)/)?.[1] ?? 0)
    const route = async (names: string[]) => {
        for (const name of names) await journey.move(name)
    }
    async function equip(names: string[]) {
        await page.getByRole('button', {name: '背包', exact: true}).click()
        await page.getByRole('button', {name: '装备', exact: true}).click()
        for (const name of names) {
            // 主线奖励与商店补购可同时留下同名散件；穿戴其中一件，不要求库存名称唯一。
            await current().locator('.equipment-row').filter({hasText: name}).first().click()
            const detail = page.locator('ion-modal').filter({visible: true})
            await detail.getByRole('button', {name: '穿戴', exact: true}).click()
            await expect(detail).toHaveCount(0)
        }
        await expect(current().locator('.item-link')).toHaveCount(7)
        await page.getByRole('button', {name: '冒险', exact: true}).click()
    }
    async function buy(names: string[]) {
        await page.getByRole('button', {name: '商店', exact: true}).click()
        for (const name of names) {
            const response = page.waitForResponse(
                (response) =>
                    response.url().endsWith('/api/inventory/equipment/commerce') &&
                    response.request().method() === 'POST',
            )
            await current()
                .locator('article')
                .filter({has: page.getByText(name, {exact: true})})
                .getByRole('button', {name: /购买/})
                .click()
            expect((await response).ok()).toBe(true)
        }
        await current().getByRole('button', {name: '返回', exact: true}).click()
        await expect(page).toHaveURL(/\/world$/)
    }
    // 开篇结束已达到7级散件门槛，穿上实际任务奖励再训练；穿戴本身不补生命魔力。
    await equip(['精铁剑', '旅人兜帽', '旅人短衣', '软皮手套', '旅行长裤', '软皮短靴', '平安吊坠'])
    await route(['村中广场', '石板路', '河边小路', '东侧林口', '平原路标', '小站歇脚处', '诺姆学院', '学院疗养处'])
    // 先在平原训练，再进入高等级城郊；每次回城恢复均经相邻道路。
    let upgraded = false
    for (let fights = 0; (await level()) < 11 && fights < 4000; fights++) {
        if (!upgraded && (await level()) >= 10) {
            const gear = ['平原剑', '旅行外衣', '轻便长靴', '加固护腿', '旅人护符']
            await route(['诺姆学院', '学院补给处'])
            await buy(gear)
            await equip(gear)
            await route(['诺姆学院', '学院疗养处'])
            upgraded = true
        }
        await page.getByRole('button', {name: '全队免费恢复', exact: true}).click()
        await route(['诺姆学院', '小站歇脚处', '平原路标'])
        await journey.fight('探索诺姆平原')
        await route(['小站歇脚处', '诺姆学院', '学院疗养处'])
    }
    expect(await level()).toBeGreaterThanOrEqual(11)
    await route(['诺姆学院', '诺姆城', '诺姆城幻兽中心'])
    for (let fights = 0; (await level()) < 15 && fights < 4000; fights++) {
        await page.getByRole('button', {name: '全队免费恢复', exact: true}).click()
        await route(['诺姆城', '诺姆城外围'])
        await journey.fight('探索诺姆城外围')
        await route(['诺姆城', '诺姆城幻兽中心'])
    }
    expect(await level()).toBe(15)
    await page.getByRole('button', {name: '全队免费恢复', exact: true}).click()
    await journey.move('诺姆城')
    await journey.move('诺姆城商店')
    const gear = ['卫兵剑', '卫兵头盔', '卫兵制服', '卫兵护手', '卫兵护腿', '卫兵长靴', '卫兵徽记']
    // 当前任务不赠卫兵装备，必须用实际练级金币购买，再逐件手动穿戴。
    await buy(gear)
    await equip(gear)
    await journey.move('诺姆城')
    await page.screenshot({path: info.outputPath('natural-level15.png'), fullPage: true})
    return {name: journey.name}
}
