/** 读取公开战局决定测试选招，所有行动仍通过页面提交；不写数据库战局或调用行动 API。 */
import {writeFileSync} from 'node:fs'
import {expect, type APIRequestContext, type Page, type TestInfo} from '@playwright/test'
import type {DungeonCombat, DungeonRun} from '../../src/features/dungeon/types'

export async function finishDungeon(
    pages: Page[],
    players: {token: string; request: APIRequestContext}[],
    base: string,
    run: string,
    info: TestInfo,
    started = Date.now(),
) {
    async function read<T>(index: number, path: string): Promise<T> {
        const player = players[index]!
        const response = await player.request.get(`${base}/api/dungeon/runs/${run}${path}`, {
            headers: {Authorization: `Bearer ${player.token}`},
        })
        expect(response.ok(), `读取副本${path}: ${response.status()}`).toBe(true)
        return response.json()
    }
    let checks = 0
    let rounds = 0
    while (++checks <= 100) {
        const progress = await read<DungeonRun>(0, '')
        if (!progress.battle) {
            await Promise.all(
                pages.map(async (playerPage) => {
                    await playerPage.reload()
                    await expect(playerPage.getByLabel('副本路线', {exact: true})).toBeVisible()
                    await playerPage.getByText('上一场战报', {exact: true}).click()
                    await expect(playerPage.getByText(/获得经验.*金币/).last()).toBeVisible()
                }),
            )
            if (progress.completed) {
                await Promise.all(
                    pages.map(async (playerPage) => {
                        await expect(playerPage.getByText('副本已通关', {exact: true})).toBeVisible()
                        await expect(playerPage.getByText(/领取今日首次参与礼包/).last()).toBeVisible()
                    }),
                )
                await pages[0]!
                    .locator('ion-content')
                    .filter({has: pages[0]!.getByLabel('副本路线', {exact: true})})
                    .evaluate(async (element) => {
                        await (element as HTMLIonContentElement).scrollToTop(0)
                    })
                await pages[0]!.screenshot({
                    path: info.outputPath('dungeon-completed.png'),
                    fullPage: true,
                    animations: 'disabled',
                })
                expect(await pages[0]!.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
                await Promise.all(
                    pages.map(async (playerPage) => {
                        await playerPage.getByRole('button', {name: '返回地图', exact: true}).click()
                        await expect(playerPage).toHaveURL(/\/world$/)
                    }),
                )
                writeFileSync(
                    info.outputPath('dungeon-timing.json'),
                    JSON.stringify(
                        {durationMs: Date.now() - started, rounds, scope: '页面四场、战间步行休整与领奖；不含组队准备'},
                        null,
                        2,
                    ),
                )
                return Date.now() - started
            }
            // 第一场后沿已清理路线回入口休整，再回前线；维克和裂耳之后各有休整点。
            await Promise.all(
                pages.map(async (playerPage) => {
                    if (progress.cleared === 1)
                        await playerPage.getByRole('button', {name: '前往副本入口', exact: true}).click()
                    await playerPage.getByRole('button', {name: '休整', exact: true}).click()
                    if (progress.cleared === 1)
                        await playerPage.getByRole('button', {name: '前往兽栏外径', exact: true}).click()
                    await playerPage.getByRole('button', {name: '准备就绪', exact: true}).click()
                }),
            )
            await pages[0]!.getByRole('button', {name: '开始遭遇', exact: true}).click()
            await expect.poll(async () => (await read<DungeonRun>(0, '')).battle).not.toBeNull()
            continue
        }
        const first = await read<DungeonCombat>(0, `/battles/${progress.battle}`)
        expect(first.status).toBe('ACTIVE')
        await Promise.all(
            pages.map(async (playerPage, i) => {
                const combat = await read<DungeonCombat>(i, `/battles/${progress.battle}`)
                await expect(
                    playerPage.getByRole('heading', {name: `${combat.name} · 第 ${combat.round} 回合`, exact: true}),
                ).toBeVisible()
                const own = combat.units.filter((unit) => unit.owner === combat.self).map((unit) => unit.detail)
                const enemies = combat.units.map((unit) => unit.detail).filter((unit) => unit.enemy && unit.hp > 0)
                const enemy = enemies.find((unit) => unit.id === 'dungeon-guard') ?? enemies[0]!
                for (const unit of own.filter((unit) => unit.hp > 0 && (unit.hero || unit.position < 2))) {
                    await playerPage.locator('.actor-strip button').filter({hasText: unit.name}).click()
                    const available = (id: string) =>
                        unit.actions.some((action) => action.id === id && action.available)
                    const wounded = own
                        .filter((target) => target.hp > 0 && (target.hero || target.position < 2))
                        .sort((a, b) => a.hp / a.stats.health - b.hp / b.stats.health)[0]!
                    let action = available('水弹') ? '水弹' : 'ATTACK',
                        target = enemy.id
                    if (wounded.hp < wounded.stats.health * 0.65 && available(unit.hero ? 'HEALTH_POTION' : '微疗')) {
                        action = unit.hero ? 'HEALTH_POTION' : '微疗'
                        target = wounded.id
                    } else if (unit.hero) {
                        const depleted = own.find(
                            (target) => !target.hero && target.position < 2 && target.hp > 0 && target.mp < 12,
                        )
                        if (depleted && available('MANA_POTION')) {
                            action = 'MANA_POTION'
                            target = depleted.id
                        }
                    }
                    await playerPage.getByLabel(`${unit.name}行动`, {exact: true}).selectOption(action)
                    await playerPage.getByLabel(`${unit.name}目标`, {exact: true}).selectOption(target)
                }
                await playerPage.getByRole('button', {name: '提交本回合', exact: true}).click()
            }),
        )
        rounds++
        await expect
            .poll(async () => {
                const next = await read<DungeonCombat>(0, `/battles/${progress.battle}`)
                return next.round !== first.round || next.status !== 'ACTIVE'
            })
            .toBe(true)
    }
    throw new Error('副本未在 100 轮检查内完成')
}
