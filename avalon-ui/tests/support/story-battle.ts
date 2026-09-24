import {expect, type Page} from '@playwright/test'
import type {Battle} from '../../src/features/battle/types'

/** 只读公开战局；所有指令由页面提交，刷新验证服务端保存的回合。 */
export async function winStoryBattle(page: Page, name: string, sustain = false) {
    const [started] = await Promise.all([
        page.waitForResponse(
            (response) => response.url().endsWith('/api/battle') && response.request().method() === 'POST',
        ),
        page.getByRole('button', {name, exact: true}).click(),
    ])
    expect(started.ok(), `开战失败: ${started.status()}`).toBe(true)
    let battle: Battle = await started.json()
    await page.reload()
    for (let turn = 0; battle.status === 'ACTIVE' && turn < 60; turn++) {
        await expect(page.getByRole('heading', {name: `第 ${battle.round} 回合`, exact: true})).toBeVisible()
        for (const unit of battle.units.filter(
            (unit) => !unit.enemy && unit.hp > 0 && (unit.hero || unit.position < 2),
        )) {
            await page
                .locator('.actor-strip button')
                .filter({has: page.locator('strong', {hasText: unit.name})})
                .click()
            const skill = unit.actions.find(
                (action) => action.available && action.mana > 0 && action.target === 'ENEMY',
            )
            const wounded = battle.units
                .filter((target) => !target.enemy && target.hp > 0 && (target.hero || target.position < 2))
                .sort((a, b) => a.hp / a.stats.health - b.hp / b.stats.health)[0]!
            const heal = unit.actions.find(
                (action) => action.available && action.id === (unit.hero ? 'HEALTH_POTION' : '微疗'),
            )
            const useHeal = sustain && heal && wounded.hp < wounded.stats.health * 0.65
            await page
                .getByRole('combobox', {name: `${unit.name}行动`, exact: true})
                .selectOption(useHeal ? heal.id : (skill?.id ?? 'ATTACK'))
            if (useHeal) await page.getByLabel(`${unit.name}目标`, {exact: true}).selectOption(wounded.id)
        }
        const [submitted] = await Promise.all([
            page.waitForResponse(
                (response) =>
                    response.url().endsWith(`/api/battle/${battle.id}/turn`) && response.request().method() === 'POST',
            ),
            page.getByRole('button', {name: '提交本回合', exact: true}).click(),
        ])
        expect(submitted.ok(), `提交失败: ${submitted.status()}`).toBe(true)
        battle = await submitted.json()
    }
    expect(battle.status, battle.log.join('\n')).toBe('WON')
    await page.getByRole('button', {name: '返回地图', exact: true}).click()
    await expect(page).toHaveURL(/\/world$/)
    return battle
}
