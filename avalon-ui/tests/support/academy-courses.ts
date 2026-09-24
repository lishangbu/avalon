import {clickWorldAction, revealWorldAction, closeNpc} from './npc'
import {expect, type Page} from '@playwright/test'
import type {Battle} from '../../src/features/battle/types'
import type {WorkshopView} from '../../src/features/academy/workshop'

/** 开篇页面用例选择战士；其余职业技能路径由四职业连续服务回归覆盖。 */
export async function completeWarriorCourse(page: Page) {
    const started = page.waitForResponse(
        (response) => response.url().endsWith('/api/battle/academy') && response.request().method() === 'POST',
    )
    await clickWorldAction(page, '开始我的职业办法')
    let battle: Battle = await (await started).json()
    for (let turn = 0; battle.status === 'ACTIVE' && turn < 12; turn++) {
        await expect(page.getByRole('heading', {name: `第 ${battle.round} 回合`, exact: true})).toBeVisible()
        for (const unit of battle.units.filter(
            (unit) => !unit.enemy && unit.hp > 0 && (unit.hero || unit.position < 2),
        )) {
            await page
                .locator('.actor-strip button')
                .filter({has: page.locator('strong', {hasText: unit.name})})
                .click()
            await page
                .getByRole('combobox', {name: `${unit.name}行动`, exact: true})
                .selectOption(unit.hero && battle.round === 1 ? '盾墙' : 'DEFEND')
        }
        const response = page.waitForResponse(
            (response) =>
                response.url().endsWith(`/api/battle/${battle.id}/turn`) && response.request().method() === 'POST',
        )
        await clickWorldAction(page, '提交本回合')
        battle = await (await response).json()
    }
    expect(battle.status, battle.log.join('\n')).toBe('WON')
    await clickWorldAction(page, '返回地图')
    await clickWorldAction(page, '核验职业练习并领取课程奖励')
    const dialogue = page
        .locator('ion-modal')
        .filter({visible: true})
        .filter({has: page.locator('ion-title').filter({hasText: /^对话$/})})
    await dialogue.getByRole('button', {name: '继续', exact: true}).click()
    await closeNpc(page)
    await expect(dialogue).toHaveCount(0)
}

/** 使用借用伙伴完成学院基础课程；只观察公开响应，全部命令通过页面提交。 */
export async function completeBasicAcademyCourses(page: Page) {
    const region = page.getByRole('region', {name: '学院实践课', exact: true})
    async function claim(label: string) {
        await clickWorldAction(page, label)
        const dialogue = page
            .locator('ion-modal')
            .filter({visible: true})
            .filter({has: page.locator('ion-title').filter({hasText: /^对话$/})})
        await dialogue.getByRole('button', {name: '继续', exact: true}).click()
        await closeNpc(page)
        await expect(dialogue).toHaveCount(0)
    }
    for (const course of [
        {name: '全队一起行动', claim: '提交全队指令课程成果', swap: false},
        {name: '留给替补的位置', claim: '提交替补课程成果', swap: true},
    ]) {
        await revealWorldAction(page, `开始${course.name}`)
        if (course.swap) {
            for (const name of ['借用利爪猫', '借用小岩怪', '借用芽灵', '借用青岚雀'])
                await region.getByRole('button', {name, exact: true}).click()
            await expect(region.getByText('已选 4/4，再次点击可取消，后续位置随之顺移。', {exact: true})).toBeVisible()
        }
        const started = page.waitForResponse(
            (response) => response.url().endsWith('/api/battle/academy') && response.request().method() === 'POST',
        )
        await clickWorldAction(page, `开始${course.name}`)
        let battle: Battle = await (await started).json()
        await expect(page).toHaveURL(/\/battle$/)
        for (let turn = 0; battle.status === 'ACTIVE' && turn < 24; turn++) {
            await expect(page.getByRole('heading', {name: `第 ${battle.round} 回合`, exact: true})).toBeVisible()
            for (const unit of battle.units.filter(
                (unit) => !unit.enemy && unit.hp > 0 && (unit.hero || unit.position < 2),
            )) {
                const action = course.swap
                    ? unit.id === 'T_CAT'
                        ? 'SWAP'
                        : 'DEFEND'
                    : unit.hero && battle.round === 2
                      ? 'DEFEND'
                      : 'ATTACK'
                await page
                    .locator('.actor-strip button')
                    .filter({has: page.locator('strong', {hasText: unit.name})})
                    .click()
                await page.getByRole('combobox', {name: `${unit.name}行动`, exact: true}).selectOption(action)
                if (action === 'SWAP')
                    await page.getByRole('combobox', {name: `${unit.name}目标`, exact: true}).selectOption('T_SPROUT')
            }
            const response = page.waitForResponse(
                (response) =>
                    response.url().endsWith(`/api/battle/${battle.id}/turn`) && response.request().method() === 'POST',
            )
            await clickWorldAction(page, '提交本回合')
            battle = await (await response).json()
        }
        expect(battle.status, battle.log.join('\n')).toBe('WON')
        await clickWorldAction(page, '返回地图')
        await expect(page).toHaveURL(/\/world$/)
        await claim(course.claim)
    }
}

/** 学习与仓库实践逐步确认服务端结果，并刷新检查会话恢复，不跳过训练操作。 */
export async function completeAcademyWorkshops(page: Page) {
    async function act(label: string, pet = false) {
        const response = page.waitForResponse(
            (response) =>
                response.url().includes('/api/academy/workshop/') &&
                response.url().endsWith('/actions') &&
                response.request().method() === 'POST',
        )
        const scope = pet ? page.locator('.row').filter({has: page.locator('strong', {hasText: /^练习芽灵$/})}) : page
        await scope.getByRole('button', {name: label, exact: true}).click()
        const result = await response
        expect(result.ok()).toBe(true)
        return (await result.json()) as WorkshopView
    }
    async function finish() {
        const result = await act('完成课程')
        expect(result.status).toBe('PASSED')
        await clickWorldAction(page, '返回地图')
        await expect(page).toHaveURL(/\/world$/)
    }
    await clickWorldAction(page, '开始技能书里的选择')
    await expect(page).toHaveURL(/\/academy\/workshop$/)
    await act('查看技能池')
    const learned = await act('学习光幕')
    expect(learned.learningResult?.learned).toBe('光幕')
    await page.reload()
    await expect(page.getByText('学会：光幕', {exact: true})).toBeVisible()
    await finish()
    await clickWorldAction(page, '提交技能书课程成果')
    const dialogue = page
        .locator('ion-modal')
        .filter({visible: true})
        .filter({has: page.locator('ion-title').filter({hasText: /^对话$/})})
    await dialogue.getByRole('button', {name: '继续', exact: true}).click()
    await closeNpc(page)
    await expect(dialogue).toHaveCount(0)
    await clickWorldAction(page, '学院疗养处')
    await expect(page.getByRole('button', {name: '学院疗养处，当前位置', exact: true})).toBeVisible()
    await clickWorldAction(page, '开始行囊里的分寸')
    await expect(page).toHaveURL(/\/academy\/workshop$/)
    const before = await act('比较训练装备')
    const equipped = await act('穿戴练习外衣B')
    expect(equipped.hp).toBe(before.hp)
    expect(equipped.mp).toBe(before.mp)
    await act('请教员重置训练生命')
    const unequipped = await act('卸下训练装备')
    expect(unequipped.hp).toBe(unequipped.maxHp)
    await act('进入背包与仓库练习')
    await act('使用训练生命药')
    await act('领取待领魔力药')
    await act('存入仓库', true)
    await act('取回第1替补位', true)
    await act('锁定保护', true)
    await finish()
    await clickWorldAction(page, '核验训练库存与仓库操作')
    await dialogue.getByRole('button', {name: '继续', exact: true}).click()
    await closeNpc(page)
    await expect(dialogue).toHaveCount(0)
    await clickWorldAction(page, '诺姆学院')
    await expect(page.getByRole('button', {name: '诺姆学院，当前位置', exact: true})).toBeVisible()
}

/** 教学捕捉也遵循正式概率；失败后重开隔离课程，不改投影生命或成功结果。 */
export async function completeAcademyCapture(page: Page) {
    let completed = false
    for (let attempt = 0; attempt < 8 && !completed; attempt++) {
        const started = page.waitForResponse(
            (response) => response.url().endsWith('/api/battle/academy') && response.request().method() === 'POST',
        )
        await clickWorldAction(page, '开始看懂属性与封印球')
        let battle: Battle = await (await started).json()
        const info = page.getByRole('region', {name: '捕捉实践资料', exact: true})
        await info
            .locator('.row')
            .filter({hasText: /封印球I ·/})
            .getByRole('button', {name: '查看并选择此球', exact: true})
            .click()
        await expect(info.getByRole('button', {name: '已查看并选择', exact: true})).toBeVisible()
        for (let turn = 0; battle.status === 'ACTIVE' && turn < 20; turn++) {
            const target = battle.units.find((unit) => unit.enemy)!
            await expect(page.getByRole('heading', {name: `第 ${battle.round} 回合`, exact: true})).toBeVisible()
            for (const unit of battle.units.filter(
                (unit) => !unit.enemy && unit.hp > 0 && (unit.hero || unit.position < 2),
            )) {
                const action =
                    unit.hero && target.hp <= target.stats.health * 0.6
                        ? 'SEAL_I'
                        : !unit.hero &&
                            target.hp > target.stats.health * 0.6 &&
                            unit.actions.some((action) => action.id === '水弹' && action.available)
                          ? '水弹'
                          : 'DEFEND'
                await page
                    .locator('.actor-strip button')
                    .filter({has: page.locator('strong', {hasText: unit.name})})
                    .click()
                await page.getByRole('combobox', {name: `${unit.name}行动`, exact: true}).selectOption(action)
            }
            const response = page.waitForResponse(
                (response) =>
                    response.url().endsWith(`/api/battle/${battle.id}/turn`) && response.request().method() === 'POST',
            )
            await clickWorldAction(page, '提交本回合')
            battle = await (await response).json()
        }
        completed = battle.status === 'WON'
        if (battle.status === 'ACTIVE') {
            await clickWorldAction(page, '退出训练')
            await page
                .locator('ion-modal')
                .filter({visible: true})
                .getByRole('button', {name: '确认退出', exact: true})
                .click()
        }
        await clickWorldAction(page, '返回地图')
        await expect(page).toHaveURL(/\/world$/)
    }
    expect(completed, '捕捉实践未在正常概率和操作预算内完成').toBe(true)
    await clickWorldAction(page, '提交投影捕捉课程成果')
    const dialogue = page
        .locator('ion-modal')
        .filter({visible: true})
        .filter({has: page.locator('ion-title').filter({hasText: /^对话$/})})
    await dialogue.getByRole('button', {name: '继续', exact: true}).click()
    await closeNpc(page)
    await expect(dialogue).toHaveCount(0)
}
