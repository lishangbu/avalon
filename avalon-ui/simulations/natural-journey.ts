import assert from 'node:assert/strict'
import {randomUUID} from 'node:crypto'
import {readFileSync} from 'node:fs'
import type {Battle, Order} from '../src/features/battle/types'
import type {Character} from '../src/features/character/types'
import type {OwnedCreature} from '../src/features/creature/types'
import type {BagView, OwnedItem} from '../src/features/inventory/types'
import type {QuestState} from '../src/features/quest/types'
import type {WorkshopView} from '../src/features/academy/workshop'
import type {World} from '../src/features/world/types'
import {naturalOrders} from '../tests/support/battle-orders'

type Step = {id: string; location: string; objectives: {code: string; amount: number}[]}
type Scene = {id: string; location: string; requires?: string[]; answers?: string[]}
type Encounter = {id: string; location: string}
type BattleRecord = {
    id: string
    encounter: string
    status: string
    rounds: number
    before: number
    after: number
    captured: boolean
}
// 内容只用于选择路线、动作和答案；所有事实和资产仍由正式HTTP命令取得。
const content = (name: string) =>
    JSON.parse(
        readFileSync(new URL(`../../avalon-content/src/main/resources/content/${name}.json`, import.meta.url), 'utf8'),
    )
const steps: Step[] = content('quests').steps
const scenes: Scene[] = content('scene-actions').actions
const encounters: Encounter[] = content('battle').encounters

/** 只操作自行注册的新账号，不写数据库，不补经验，不固定随机数。 */
export class NaturalJourney {
    private tokens = {accessToken: '', refreshToken: ''}
    private readonly started = Date.now()
    private readonly acquired = new Set<string>()
    private readonly battles: BattleRecord[] = []
    private readonly training: {
        targetLevel: number
        encounter: string
        elapsedMs: number
        fights: number
        before: number
        after: number
    }[] = []
    private readonly checkpoints: {step: string; level: number; experience: number; elapsedMs: number}[] = []
    private character?: Character
    private requests = 0
    private openingMs = 0
    private route = ''
    private succeeded = false

    constructor(private readonly base: string) {}

    private async request<T>(path: string, data?: unknown, method = data === undefined ? 'GET' : 'POST'): Promise<T> {
        const send = () => {
            this.requests++
            return fetch(new URL(path, this.base), {
                method,
                headers: {
                    ...(this.tokens.accessToken ? {Authorization: `Bearer ${this.tokens.accessToken}`} : {}),
                    ...(data === undefined ? {} : {'Content-Type': 'application/json'}),
                },
                body: data === undefined ? undefined : JSON.stringify(data),
                signal: AbortSignal.timeout(15_000),
            })
        }
        let response = await send()
        if (response.status === 401 && path !== '/api/auth/refresh' && this.tokens.refreshToken) {
            await response.text()
            this.tokens = await this.request('/api/auth/refresh', {refreshToken: this.tokens.refreshToken})
            response = await send()
        }
        const body = await response.text()
        // 认证响应不写入错误或验收日志，避免泄露凭证。
        assert(response.ok, `${method} ${path}: ${response.status} ${path.startsWith('/api/auth/') ? '' : body}`)
        return JSON.parse(body || 'null')
    }

    private async world() {
        const state = await this.request<World>('/api/world')
        assert(state.character, '角色必须通过正式创角接口创建')
        this.character = state.character
        return {...state, character: state.character}
    }

    private path(state: World & {character: Character}, target: string): string[] {
        const queue = [[state.character.location]]
        const visited = new Set(queue[0])
        for (let i = 0; i < queue.length; i++) {
            const path = queue[i]!
            const last = path.at(-1)!
            if (last === target) return path.slice(1)
            for (const next of state.content.places.find((place) => place.id === last)?.exits ?? []) {
                if (visited.has(next)) continue
                visited.add(next)
                queue.push([...path, next])
            }
        }
        throw new Error(`没有已开放道路：${state.character.location} -> ${target}`)
    }

    private async walk(target: string) {
        const state = await this.world()
        let character = state.character
        for (const destination of this.path(state, target)) {
            character = await this.request<Character>('/api/world/move', {
                destination,
                expectedRevision: character.revision,
            })
            assert.equal(character.location, destination)
            this.acquired.add(`VISIT:${destination}`)
        }
        this.character = character
    }

    private async accept(step: string) {
        const state = await this.world()
        await this.request(`/api/quests/${step}/accept`, {
            requestId: randomUUID(),
            expectedRevision: state.character.revision,
        })
    }

    private async quest(step: string) {
        const state = await this.request<QuestState>('/api/quests')
        // 战斗奖励会在同一事务交付对应步骤，已完成时直接核对正式日志。
        if (state.journal.some((entry) => entry.steps.some((value) => value.id === step && value.completed))) return
        const current = state.available.find((value) => value.id === step)
        assert(current?.instance && current.round != null, `${step}必须有当前任务实例`)
        await this.request(
            '/api/quests/instances/commands',
            {
                requestId: randomUUID(),
                instance: current.instance,
                round: current.round,
                operation: 'DELIVER',
                step,
            },
            'POST',
        )
        const journal = await this.request<QuestState>('/api/quests')
        assert(
            journal.journal.some((entry) => entry.steps.some((entry) => entry.id === step && entry.completed)),
            `${step}必须已由服务端完成`,
        )
        this.acquired.add(`QUEST:${step}`)
        const {character} = await this.world()
        this.checkpoints.push({
            step,
            level: character.level,
            experience: character.experience,
            elapsedMs: Date.now() - this.started,
        })
    }

    private async scene(id: string, choiceId?: string) {
        const {character} = await this.world()
        const result = await this.request<{completed: boolean}>(`/api/scene-actions/${id}`, {
            requestId: randomUUID(),
            expectedRevision: character.revision,
            choiceId,
        })
        assert(result.completed, `${id}必须正式完成`)
        this.acquired.add(`SCENE:${id}`)
    }

    private async recover() {
        await this.request('/api/battle/recover', {}, 'POST')
        const lobby = await this.request<{vitals: {hp: number; mp: number; maxHp: number; maxMp: number}[]}>(
            '/api/battle/lobby',
        )
        assert(lobby.vitals.length > 0)
        assert(
            lobby.vitals.every((unit) => unit.hp === unit.maxHp && unit.mp === unit.maxMp),
            '恢复须由正式设施结算',
        )
    }

    private async battle(encounter: string, course = false, select = naturalOrders, captureInfo = false) {
        const before = (await this.world()).character.experience
        let battle = await this.request<Battle>(course ? '/api/battle/academy' : '/api/battle', {
            encounter,
            course: encounter,
            partners: encounter === 'BQ019' ? ['T_CAT', 'T_STONE', 'T_SPROUT', 'T_GALE'] : [],
            requestId: randomUUID(),
        })
        if (captureInfo)
            battle = await this.request<Battle>(`/api/battle/academy/${battle.id}/capture-info`, {ball: 'SEAL_I'})
        let rounds = 0
        while (battle.status === 'ACTIVE' && rounds < 60) {
            battle = await this.request<Battle>(`/api/battle/${battle.id}/turn`, {
                round: battle.round,
                orders: select(battle),
                surrender: false,
                replacements: {},
            })
            rounds++
        }
        // 未完成的捕捉练习必须正式退出后才能重试，不能遗留活动战局。
        if (battle.status === 'ACTIVE' && (captureInfo || encounter === 'TUTORIAL')) {
            battle = await this.request<Battle>(`/api/battle/${battle.id}/turn`, {
                round: battle.round,
                orders: [],
                surrender: true,
                replacements: {},
            })
        }
        const after = (await this.world()).character.experience
        this.battles.push({
            id: battle.id,
            encounter,
            status: battle.status,
            rounds,
            before,
            after,
            captured: battle.units.some((unit) => unit.enemy && unit.captured),
        })
        if (!captureInfo && encounter !== 'TUTORIAL') assert.equal(battle.status, 'WON', battle.log.join('\n'))
        if (battle.status === 'WON') this.acquired.add(`VICTORY:${encounter}`)
        return battle
    }

    private captureOrders(battle: Battle, threshold = 0.5): Order[] {
        const enemy = battle.units.find((unit) => unit.enemy && unit.hp > 0 && !unit.captured)!
        assert(enemy)
        const weakened = enemy.hp <= enemy.stats.health * threshold
        const hasBall = (battle.supplies.SEAL_I ?? 0) > 0
        return battle.units
            .filter((unit) => !unit.enemy && unit.hp > 0 && (unit.hero || unit.position < 2))
            .map((unit) => {
                const action = !hasBall
                    ? 'ATTACK'
                    : unit.hero
                      ? weakened
                          ? 'SEAL_I'
                          : 'DEFEND'
                      : weakened
                        ? 'DEFEND'
                        : unit.actions.some((action) => action.id === '水弹' && action.available)
                          ? '水弹'
                          : 'ATTACK'
                return {actor: unit.id, action, target: action === 'DEFEND' ? unit.id : enemy.id}
            })
    }

    private async capture() {
        const before = await this.request<OwnedCreature[]>('/api/creatures')
        for (let attempt = 0; attempt < 12; attempt++) {
            await this.walk('PET_CENTER')
            await this.recover()
            if (attempt) {
                await this.walk('SUPPLY_SHOP')
                await this.request('/api/battle/purchase', {code: 'SEAL_I', requestId: randomUUID()})
            }
            await this.walk('RIVERSIDE')
            const result = await this.battle('TUTORIAL', false, (battle) => this.captureOrders(battle))
            const pet = (await this.request<OwnedCreature[]>('/api/creatures')).find(
                (pet) => !before.some((old) => old.id === pet.id),
            )
            if (pet) {
                assert.equal(result.status, 'WON')
                await this.walk('PET_CENTER')
                await this.request(`/api/creatures/${pet.id}/position`, {position: 1})
                await this.request('/api/creatures/capture-registration', {}, 'POST')
                return
            }
        }
        throw new Error('正式概率捕捉未在操作预算内成功')
    }

    private async equip(codes?: string[]) {
        const {character} = await this.world()
        const items = await this.request<OwnedItem[]>('/api/inventory')
        const chosen = new Map<string, OwnedItem>()
        for (const item of items
            .filter(
                (item) =>
                    item.slot &&
                    item.quantity > 0 &&
                    item.requiredLevel <= character.level &&
                    (!item.requiredProfession || item.requiredProfession === character.profession) &&
                    (!codes || codes.includes(item.code)),
            )
            .sort((a, b) => a.requiredLevel - b.requiredLevel))
            chosen.set(item.slot!, item)
        if (codes)
            for (const code of codes)
                assert(
                    [...chosen.values()].some((item) => item.code === code),
                    `${code}必须真实持有且满足穿戴要求`,
                )
        for (const item of chosen.values())
            if (!item.equipped) await this.request(`/api/inventory/equipment/${item.id}/equip`, {}, 'POST')
    }

    private async train(target: number, encounter: string, recovery: string, max = 4000) {
        const started = Date.now()
        const before = (await this.world()).character.experience
        const countBefore = this.battles.length
        const location = encounters.find((entry) => entry.id === encounter)!.location
        await this.walk(recovery)
        for (let count = 0; (await this.world()).character.level < target && count < max; count++) {
            await this.recover()
            await this.walk(location)
            const battle = await this.battle(encounter)
            assert.equal(battle.status, 'WON')
            const record = this.battles.at(-1)!
            assert(record.after > record.before, `训练${record.id}必须获得真实经验`)
            await this.walk(recovery)
        }
        const final = (await this.world()).character
        assert(final.level >= target, `实际野战未达到${target}级`)
        this.training.push({
            targetLevel: target,
            encounter,
            elapsedMs: Date.now() - started,
            fights: this.battles.length - countBefore,
            before,
            after: final.experience,
        })
    }

    private async workshop(course: string, actions: [string, string?, number?][]) {
        let view = await this.request<WorkshopView>('/api/academy/workshop', {course, requestId: randomUUID()})
        for (const [type, target, position] of actions)
            view = await this.request<WorkshopView>(`/api/academy/workshop/${view.id}/actions`, {
                requestId: randomUUID(),
                revision: view.revision,
                type,
                target,
                position,
            })
        assert.equal(view.status, 'PASSED')
    }

    private async opening() {
        for (const [step, place] of [
            ['BQ001', 'SQUARE'],
            ['BQ002', 'ELDER_HOUSE'],
            ['BQ003', 'WEAPON_SHOP'],
            ['BQ004', 'PET_CENTER'],
        ]) {
            await this.walk(place!)
            await this.accept(step!)
            await this.quest(step!)
        }
        await this.accept('BQ005')
        const starter = (await this.request<OwnedCreature[]>('/api/creatures'))[0]!
        await this.request(`/api/creatures/${starter.id}/position`, {position: 0})
        await this.request(`/api/creatures/${starter.id}/skills/viewed`, {}, 'POST')
        await this.equip()
        await this.quest('BQ005')
        await this.walk('RIVERSIDE')
        await this.accept('BQ006')
        await this.quest('BQ006')
        await this.walk('PET_CENTER')
        await this.accept('BQ007')
        await this.capture()
        await this.quest('BQ007')
        await this.walk('SQUARE')
        await this.scene('BQ008_COLLECT_STONES')
        await this.quest('BQ008')
        await this.walk('PET_CENTER')
        await this.accept('BQ009')
        await this.recover()
        await this.quest('BQ009')
        await this.train(5, 'VILLAGE', 'PET_CENTER', 250)
        await this.recover()
        await this.walk('FOREST_EDGE')
        await this.quest('BQ010')
        await this.walk('CAVE_ENTRANCE')
        await this.battle('CAVE_BOSS')
        await this.quest('BQ011')
        await this.quest('BQ012')
        await this.walk('SQUARE')
        await this.quest('BQ013')
        await this.walk('ELDER_HOUSE')
        await this.quest('BQ014')
        await this.walk('PLAIN_SIGN')
        await this.accept('BQ015')
        await this.quest('BQ015')
        await this.walk('STATION_OUTSKIRTS')
        await this.scene('BQ016_DELIVER_CARE')
        await this.quest('BQ016')
        await this.walk('ACADEMY')
        await this.scene('BQ017_PRESENT_INTRO')
        await this.quest('BQ017')
        for (const course of ['BQ018', 'BQ019']) {
            await this.battle(course, true, (battle) =>
                battle.units
                    .filter((unit) => !unit.enemy && unit.hp > 0 && (unit.hero || unit.position < 2))
                    .map((unit) => {
                        if (course === 'BQ019')
                            return {
                                actor: unit.id,
                                action: unit.id === 'T_CAT' ? 'SWAP' : 'DEFEND',
                                target: unit.id === 'T_CAT' ? 'T_SPROUT' : unit.id,
                            }
                        const defend = unit.hero && battle.round === 2
                        return {
                            actor: unit.id,
                            action: defend ? 'DEFEND' : 'ATTACK',
                            target: defend ? unit.id : 'academy-target',
                        }
                    }),
            )
            await this.quest(course)
        }
        let passed = false
        for (let attempt = 0; attempt < 8 && !passed; attempt++)
            passed =
                (await this.battle('BQ020', true, (battle) => this.captureOrders(battle, 0.6), true)).status === 'WON'
        assert(passed, '学院捕捉课程须在正式概率下完成')
        await this.quest('BQ020')
        await this.workshop('BQ021', [['VIEW_POOL'], ['LEARN', 'BOOK_LIGHT_SCREEN'], ['FINISH']])
        await this.quest('BQ021')
        await this.walk('ACADEMY_PETS')
        await this.workshop('BQ022', [
            ['INSPECT_EQUIPMENT'],
            ['EQUIP', 'B'],
            ['RESET_VITALS'],
            ['UNEQUIP'],
            ['START_BAG'],
            ['USE_HEALTH'],
            ['CLAIM_MANA'],
            ['DEPOSIT', 'T_SPROUT'],
            ['WITHDRAW', 'T_SPROUT', 2],
            ['LOCK', 'T_SPROUT'],
            ['FINISH'],
        ])
        await this.quest('BQ022')
        await this.walk('ACADEMY')
        await this.battle('BQ023', true, (battle) =>
            battle.units
                .filter((unit) => !unit.enemy && unit.hp > 0 && (unit.hero || unit.position < 2))
                .map((unit) => ({
                    actor: unit.id,
                    action: unit.hero && battle.round === 1 ? '盾墙' : 'DEFEND',
                    target: unit.id,
                })),
        )
        await this.quest('BQ023')
        await this.accept('BQ024')
        await this.request(`/api/creatures/${starter.id}/position`, {position: 0})
        await this.walk('ACADEMY_PETS')
        await this.recover()
        await this.walk('ACADEMY')
        await this.scene('BQ024_PLAN', 'HABITAT_FIRST')
        await this.quest('BQ024')
        await this.walk('GNOME_CITY')
        await this.quest('BQ025')
        await this.walk('CITY_OUTSKIRTS')
        await this.quest('BQ026')
        await this.scene('BQ027_CLEAR_PATH')
        await this.battle('PATH_GUARD')
        await this.quest('BQ027')
        await this.walk('ACADEMY')
        await this.scene('BQ028_REPORT', 'KEEP_HABITAT')
        await this.quest('BQ028')
        await this.quest('BQ029')
        await this.walk('GNOME_CITY')
        await this.scene('BQ030_MEAL')
        await this.quest('BQ030')
        await this.walk('PET_CENTER')
        await this.recover()
        this.openingMs = Date.now() - this.started
    }

    private async buy(codes: string[]) {
        let bag = await this.request<BagView>('/api/inventory/bag')
        while (bag.capacity - bag.usedSlots < codes.length) {
            const item =
                bag.equipment.find((item) => !item.equipped && item.sellPrice > 0) ??
                bag.equipment.find((item) => !item.equipped && item.requiredLevel < 10)
            assert(item, '新测试账号需要闲置旧装备以腾出购买空间')
            await this.request('/api/inventory/equipment/commerce', {
                requestId: randomUUID(),
                operation: item.sellPrice > 0 ? 'SELL' : 'DESTROY',
                code: null,
                expected: item,
            })
            const updated = await this.request<BagView>('/api/inventory/bag')
            assert(updated.usedSlots < bag.usedSlots, '正式回收必须腾出背包格')
            bag = updated
        }
        const shop = await this.request<{products: {code: string}[]}>('/api/inventory/equipment/shop')
        for (const code of codes) {
            assert(
                shop.products.some((product) => product.code === code),
                `${code}必须由当前商店开放`,
            )
            await this.request('/api/inventory/equipment/commerce', {
                requestId: randomUUID(),
                operation: 'BUY',
                code,
                expected: null,
            })
        }
        await this.equip(codes)
    }

    private async level15() {
        await this.equip([
            'CAVE_WEAPON',
            'JOURNEY_HAT',
            'JOURNEY_BODY',
            'JOURNEY_HANDS',
            'JOURNEY_LEGS',
            'JOURNEY_FEET',
            'JOURNEY_ACCESSORY',
        ])
        await this.train(10, 'PLAINS', 'ACADEMY_PETS')
        await this.walk('ACADEMY_SHOP')
        await this.buy(['PLAIN_WEAPON', 'PLAIN_BODY', 'PLAIN_FEET', 'PLAIN_LEGS', 'PLAIN_ACCESSORY'])
        await this.train(11, 'PLAINS', 'ACADEMY_PETS')
        await this.train(15, 'CITY_WILDS', 'CITY_PETS')
        await this.recover()
        await this.walk('CITY_SHOP')
        const codes = [
            'GUARD_WEAPON',
            'GUARD_HEAD',
            'GUARD_BODY',
            'GUARD_HANDS',
            'GUARD_LEGS',
            'GUARD_FEET',
            'GUARD_ACCESSORY',
        ]
        await this.buy(codes)
        const items = await this.request<OwnedItem[]>('/api/inventory')
        assert(
            codes.every((code) => items.some((item) => item.code === code && item.equipped)),
            '卫兵装备七部位必须真实购买穿戴',
        )
        await this.walk('GNOME_CITY')
    }

    private async satisfy(fact: string, visiting = new Set<string>()) {
        if (fact.startsWith('VISIT:')) {
            const target = fact.slice(6)
            const state = await this.world()
            if (state.character.location === target)
                await this.walk(state.content.places.find((place) => place.id === target)!.exits[0]!)
            await this.walk(target)
            return
        }
        if (this.acquired.has(fact)) return
        assert(!visiting.has(fact), `任务前置循环：${fact}`)
        visiting.add(fact)
        if (fact.startsWith('SCENE:')) {
            const scene = scenes.find((scene) => scene.id === fact.slice(6))
            assert(scene, `未知动作${fact}`)
            for (const required of scene.requires ?? []) await this.satisfy(required, visiting)
            await this.walk(scene.location)
            await this.scene(scene.id, scene.answers?.[0])
        } else if (fact.startsWith('VICTORY:')) {
            const encounter = encounters.find((entry) => entry.id === fact.slice(8))
            assert(encounter, `未知遭遇${fact}`)
            const state = await this.world()
            const recovery = state.content.places
                .filter((place) => place.services.includes('RECOVERY'))
                .sort((a, b) => this.path(state, a.id).length - this.path(state, b.id).length)[0]!
            await this.walk(recovery.id)
            await this.recover()
            await this.walk(encounter.location)
            await this.battle(encounter.id)
        } else throw new Error(`未由真实操作取得的任务前置：${fact}`)
        visiting.delete(fact)
    }

    private async story16() {
        for (const step of steps.slice(30)) {
            await this.equip()
            const available = (await this.request<QuestState>('/api/quests')).available.find(
                (entry) => entry.id === step.id,
            )
            assert(available, `${step.id}必须由正式任务列表开放`)
            if (!available.accepted) {
                assert(available.acceptAvailable, `${step.id}必须允许接取`)
                await this.walk(available.location)
                await this.accept(step.id)
            }
            for (const goal of step.objectives)
                for (let count = 0; count < goal.amount; count++) await this.satisfy(goal.code)
            await this.walk(step.location)
            await this.quest(step.id)
        }
        await this.train(16, 'SHALLOWS_WILD', 'UNDINE_CARE_STATION', 100)
        await this.recover()
    }

    async run(route: 'story16' | 'level15') {
        this.route = route
        const name = `api_${randomUUID().replaceAll('-', '').slice(0, 10)}`
        this.tokens = await this.request('/api/auth/register', {
            username: name,
            password: 'Natural-http-1234',
            device: '自然模拟',
        })
        await this.request('/api/world/character', {name, profession: 'WARRIOR'})
        await this.opening()
        if (route === 'story16') await this.story16()
        else await this.level15()
        const state = await this.world()
        assert.equal(state.character.level, route === 'story16' ? 16 : 15)
        assert.equal(state.character.activeBattleId, null)
        assert(!state.character.activeCourseId)
        const journal = await this.request<QuestState>('/api/quests')
        for (const step of steps.slice(0, route === 'story16' ? 74 : 30))
            assert(
                journal.journal.some((entry) =>
                    entry.steps.some((progress) => progress.id === step.id && progress.completed),
                ),
                `${step.id}最终进度必须持久化`,
            )
        await this.request('/api/auth/logout', {}, 'POST')
        this.succeeded = true
    }

    report() {
        return {
            route: this.route,
            succeeded: this.succeeded,
            elapsedMs: Date.now() - this.started,
            openingMs: this.openingMs,
            requests: this.requests,
            browserOperations: 0,
            character: this.character,
            checkpoints: this.checkpoints,
            training: this.training,
            battles: this.battles,
        }
    }
}
