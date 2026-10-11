import {expect, type Page, type TestInfo} from '@playwright/test'
import {completeFormalOpening} from './formal-opening'
import {clickWorldAction} from './npc'
import type {BagView} from '../../src/features/inventory/types'

/** 复用唯一正式开篇，通过野外战斗继续到15级，不注入经验或任务事实。 */
export async function completeNaturalChapter(page: Page, info: TestInfo) {
    page.setDefaultTimeout(15_000)
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
        await clickWorldAction(page, '冒险')
    }
    async function makeRoom(slots: number) {
        const response = page.waitForResponse(
            (response) => response.url().endsWith('/api/inventory/bag') && response.request().method() === 'GET',
        )
        await page.getByRole('button', {name: '背包', exact: true}).click()
        let bag: BagView = await (await response).json()
        await page.getByRole('button', {name: '装备', exact: true}).click()
        // 测试账号优先出售闲置装备；不可出售时，只清理已换下的低等级旧装备。
        while (bag.capacity - bag.usedSlots < slots) {
            const item =
                bag.equipment.find((item) => !item.equipped && item.sellPrice > 0) ??
                bag.equipment.find((item) => !item.equipped && item.requiredLevel < 10)
            expect(item, '应有已换下的旧装备腾出购买空间').toBeTruthy()
            const operation = item!.sellPrice > 0 ? '出售' : '销毁'
            await current().locator('.equipment-row').filter({hasText: item!.name}).first().click()
            const detail = page.locator('ion-modal').filter({visible: true})
            await detail.getByRole('button', {name: operation, exact: true}).click()
            const refreshed = page.waitForResponse(
                (response) => response.url().endsWith('/api/inventory/bag') && response.request().method() === 'GET',
            )
            await page
                .locator('ion-modal')
                .filter({visible: true})
                .getByRole('button', {name: `确认${operation}`, exact: true})
                .click()
            bag = await (await refreshed).json()
            await expect(page.locator('ion-modal').filter({visible: true})).toHaveCount(0)
        }
        await clickWorldAction(page, '冒险')
    }
    async function buy(names: string[]) {
        await makeRoom(names.length)
        await clickWorldAction(page, '商店')
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
            const purchased = await response
            expect(
                purchased.ok(),
                `购买${name}: ${purchased.status()} ${purchased.ok() ? '' : await purchased.text()}`,
            ).toBe(true)
        }
        await current().getByRole('button', {name: '返回', exact: true}).click()
        await expect(page).toHaveURL(/\/world$/)
    }
    // 开篇结束已达到7级散件门槛，穿上实际任务奖励再训练；穿戴本身不补生命魔力。
    await equip(['精铁剑', '旅人兜帽', '旅人短衣', '软皮手套', '旅行长裤', '软皮短靴', '平安吊坠'])
    await route(['村中广场', '石板路', '河边小路', '东侧林口', '平原路标', '小站歇脚处', '诺姆学院', '学院疗养处'])
    // 先在平原训练，再进入高等级城郊；每次回城恢复均经相邻道路。
    const plainsTraining = (targetLevel: number) =>
        journey.train({
            targetLevel,
            encounter: '探索诺姆平原',
            outward: ['诺姆学院', '小站歇脚处', '平原路标'],
            homeward: ['小站歇脚处', '诺姆学院', '学院疗养处'],
            maxFights: 4000,
            move: journey.move,
            fight: journey.fight,
        })
    await plainsTraining(10)
    // 到10级后照常在页面购买并穿戴装备，再继续训练；金币来自实际奖励。
    const upgradedGear = ['平原剑', '旅行外衣', '轻便长靴', '加固护腿', '旅人护符']
    await route(['诺姆学院', '学院补给处'])
    await buy(upgradedGear)
    await equip(upgradedGear)
    await route(['诺姆学院', '学院疗养处'])
    await plainsTraining(11)
    expect(await level()).toBeGreaterThanOrEqual(11)
    await route(['诺姆学院', '诺姆城', '诺姆城幻兽中心'])
    await journey.train({
        targetLevel: 15,
        encounter: '探索诺姆城外围',
        outward: ['诺姆城', '诺姆城外围'],
        homeward: ['诺姆城', '诺姆城幻兽中心'],
        maxFights: 4000,
        move: journey.move,
        fight: journey.fight,
    })
    expect(await level()).toBe(15)
    await clickWorldAction(page, '全队免费恢复')
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
