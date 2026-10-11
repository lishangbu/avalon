import {expect, type Page, type TestInfo} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import assert from 'node:assert/strict'
import type {Battle, Lobby} from '../../src/features/battle/types'
import type {Character} from '../../src/features/character/types'
import type {World} from '../../src/features/world/types'
import {clickWorldAction} from './npc'
import {naturalOrders} from './battle-orders'

/** 只省去重复训练的页面点击；每次移动、恢复、回合和经验均由正式服务结算。 */
export function naturalTraining(page: Page, info: TestInfo) {
    let authorization = ''
    page.on('request', (request) => {
        const header = request.headers().authorization
        if (header?.startsWith('Bearer ')) authorization = header
    })

    async function request(path: string, method: 'GET' | 'POST' = 'GET', data?: unknown) {
        assert.match(authorization, /^Bearer /, '训练须复用页面登录身份')
        // Node直接发HTTP，避免Playwright为数万次接口调用生成重复trace；页面仍保留失败轨迹。
        const send = () =>
            fetch(new URL(path, page.url()), {
                method,
                headers: {
                    Authorization: authorization,
                    ...(data === undefined ? {} : {'Content-Type': 'application/json'}),
                },
                body: data === undefined ? undefined : JSON.stringify(data),
                signal: AbortSignal.timeout(15_000),
            })
        const previous = authorization
        let response = await send()
        if (response.status === 401) {
            await response.text()
            // 由页面恢复会话，避免与页面竞争刷新令牌；同一命令只重试一次。
            if (authorization === previous) {
                await page.reload()
                await expect.poll(() => authorization).not.toBe(previous)
            }
            response = await send()
        }
        const body = await response.text()
        assert(response.ok, `${method} ${path}: ${response.status} ${response.ok ? '' : body}`)
        return body ? JSON.parse(body) : undefined
    }
    async function world(): Promise<World & {character: Character}> {
        const state: World = await request('/api/world')
        assert(state.character, '训练角色必须已由页面创建')
        return {...state, character: state.character!}
    }

    return async function train(options: {
        targetLevel: number
        encounter: string
        outward: string[]
        homeward: string[]
        maxFights: number
        move: (name: string) => Promise<void>
        fight: (name: string) => Promise<void>
    }) {
        const started = Date.now()
        let state = await world()
        const initial = state.character
        const recovery = state.content.places.find((place) => place.id === initial.location)!
        assert(recovery, '从实际恢复设施出发')
        const records: {
            mode: string
            battleId?: string
            rounds?: number
            before: number
            after: number
            level: number
        }[] = []
        const fullUi = process.env.AVALON_FULL_UI_TRAINING === '1'
        async function moveApi(names: string[]) {
            for (const name of names) {
                const destination = state.content.places.find((place) => place.name === name)
                assert(destination, `正式地图必须包含${name}`)
                const character: Character = await request('/api/world/move', 'POST', {
                    destination: destination!.id,
                    expectedRevision: state.character.revision,
                })
                assert.equal(character.location, destination!.id)
                state = {...state, character}
            }
        }
        try {
            for (let count = 0; state.character.level < options.targetLevel && count < options.maxFights; count++) {
                const before = state.character.experience
                if (fullUi || count < 2) {
                    await clickWorldAction(page, '全队免费恢复')
                    for (const name of options.outward) await options.move(name)
                    await options.fight(options.encounter)
                    for (const name of options.homeward) await options.move(name)
                    state = await world()
                    records.push({mode: 'UI', before, after: state.character.experience, level: state.character.level})
                } else {
                    await request('/api/battle/recover', 'POST')
                    state = await world()
                    await moveApi(options.outward)
                    const lobby: Lobby = await request('/api/battle/lobby')
                    const encounter = lobby.encounters.find((entry) => entry.name === options.encounter)
                    assert(encounter, `当前位置须开放${options.encounter}`)
                    let battle: Battle = await request('/api/battle', 'POST', {
                        encounter: encounter!.id,
                        requestId: randomUUID(),
                    })
                    let rounds = 0
                    while (battle.status === 'ACTIVE' && rounds < 60) {
                        battle = await request(`/api/battle/${battle.id}/turn`, 'POST', {
                            round: battle.round,
                            orders: naturalOrders(battle),
                            surrender: false,
                            replacements: {},
                        })
                        rounds++
                    }
                    assert.equal(battle.status, 'WON', battle.log.join('\n'))
                    state = await world()
                    assert.equal(state.character.activeBattleId, null, '胜利必须结束活动战局')
                    await moveApi(options.homeward)
                    records.push({
                        mode: 'HTTP',
                        battleId: battle.id,
                        rounds,
                        before,
                        after: state.character.experience,
                        level: state.character.level,
                    })
                }
                assert(state.character.experience > before, `第${count + 1}场应真实获得经验`)
                assert.equal(state.character.location, initial.location, '每场须沿道路回到恢复设施')
            }
            expect(state.character.level, '真实战斗须在操作预算内达到目标等级').toBeGreaterThanOrEqual(
                options.targetLevel,
            )
            // HTTP操作之后清除页面旧查询，验证服务端持久化状态后再继续剧情。
            if (new URL(page.url()).pathname !== '/world') await page.goto('/world')
            else await page.reload()
            await expect(page.getByRole('button', {name: `${recovery.name}，当前位置`, exact: true})).toBeVisible()
            await expect(
                page.locator('div.ion-page').filter({visible: true}).locator('.row-title').first(),
            ).toContainText(`Lv.${state.character.level}`)
        } finally {
            await info.attach(`natural-training-level${options.targetLevel}`, {
                body: JSON.stringify(
                    {
                        targetLevel: options.targetLevel,
                        elapsedMs: Date.now() - started,
                        initial,
                        final: state.character,
                        uiFights: records.filter((entry) => entry.mode === 'UI').length,
                        httpFights: records.filter((entry) => entry.mode === 'HTTP').length,
                        records,
                    },
                    null,
                    2,
                ),
                contentType: 'application/json',
            })
        }
    }
}
