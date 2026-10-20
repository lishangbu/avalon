// 夹具仅准备等级和前置；招募、三人确认、战间准备、回合和退出均由真实页面操作。
import {expect, test, type APIRequestContext} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {mkdirSync, writeFileSync} from 'node:fs'
import {resolve} from 'node:path'
import {winStoryBattle} from './support/story-battle'
import {execFileSync} from 'node:child_process'
import {clickWorldAction} from './support/npc'
import {auditLayout} from './support/layout-audit'
import {finishDungeon} from './support/dungeon-completion'

for (const complete of [false, true])
    test(
        complete ? '三人副本四场通关、休整与奖励' : '三人副本准备、手动回合、重连与路由限制',
        async ({browser, page}, testInfo) => {
            test.skip(process.env.AVALON_LOCAL_FIXTURES !== '1', '需要显式启用本地数据库夹具')
            test.setTimeout(complete ? 300_000 : 90_000)
            const contexts = [
                page.context(),
                await browser.newContext(testInfo.project.use),
                await browser.newContext(testInfo.project.use),
            ]
            const pages = [page, await contexts[1]!.newPage(), await contexts[2]!.newPage()]
            for (const playerPage of pages) playerPage.setDefaultTimeout(10_000)
            const players: {token: string; name: string; request: APIRequestContext}[] = []
            const suffix = randomUUID().replaceAll('-', '').slice(0, 10)
            const errors: string[] = []
            const base = testInfo.project.use.baseURL ?? 'http://localhost:8080'
            async function post(index: number, path: string, data?: unknown) {
                const player = players[index]!
                const response = await player.request.post(`${base}${path}`, {
                    headers: {Authorization: `Bearer ${player.token}`},
                    data,
                })
                expect(response.ok(), `${path} status=${response.status()}`).toBe(true)
                return response
            }
            try {
                for (let i = 0; i < 3; i++) {
                    const context = contexts[i]!
                    const response = await context.request.post(`${base}/api/auth/register`, {
                        data: {username: `dung_${suffix}_${i}`, password: 'Dungeon-browser-123', device: '测试'},
                    })
                    expect(response.ok()).toBe(true)
                    const tokens = await response.json()
                    const name = `队员${suffix}${i}`
                    players.push({token: tokens.accessToken, name, request: context.request})
                    // 浏览器刷新会轮换凭证；只读验收请求跟随页面实际使用的当前访问令牌。
                    pages[i]!.on('request', (request) => {
                        const authorization = request.headers().authorization
                        if (authorization?.startsWith('Bearer ')) players[i]!.token = authorization.slice(7)
                    })
                    await post(i, '/api/world/character', {name, profession: 'WARRIOR'})
                    await context.addInitScript((token) => {
                        if (!localStorage.getItem('avalon.refresh-token'))
                            localStorage.setItem('avalon.refresh-token', token)
                    }, tokens.refreshToken)
                    pages[i]!.on('pageerror', (error) => errors.push(error.message))
                    execFileSync(
                        'docker',
                        [
                            'exec',
                            '-i',
                            process.env.AVALON_TEST_DB_CONTAINER ?? 'avalon-postgres-1',
                            'psql',
                            '-U',
                            process.env.AVALON_TEST_DB_USER ?? 'avalon',
                            '-d',
                            process.env.AVALON_TEST_DB_NAME ?? 'avalon',
                            '-v',
                            'ON_ERROR_STOP=1',
                        ],
                        {
                            input: `BEGIN;
                    UPDATE player_character SET experience = 86440, location = 'CITY_OUTSKIRTS' WHERE name = '${name}';
                    INSERT INTO character_fact (id, character_id, code, created_at)
                        SELECT ${BigInt(`0x${suffix}`) * 10n + BigInt(i + 3)}, id, 'QUEST:O013', now() FROM player_character WHERE name = '${name}';
                    INSERT INTO character_fact (id, character_id, code, created_at)
                        SELECT ${BigInt(`0x${suffix}`) * 10n + BigInt(i)}, id, 'QUEST:O014', now() FROM player_character WHERE name = '${name}';
                    INSERT INTO character_fact (id, character_id, code, created_at)
                        SELECT ${BigInt(`0x${suffix}`) * 10n + BigInt(i + 6)}, id, 'QUEST:O012', now() FROM player_character WHERE name = '${name}';
                    INSERT INTO quest_receipt(id, character_id, step, quest_code, completed, response, created_at)
                        SELECT ${BigInt(`0x${suffix}`) * 10n + BigInt(i)}, id, 'O014', 'N026', false, '{}', now()
                        FROM player_character WHERE name = '${name}';
                    INSERT INTO owned_creature(id, character_id, source, species, name, element, level, experience,
                        ability_slot, ability, skills, party_position)
                        SELECT ${BigInt(`0x${suffix}`) * 100n + BigInt(i * 10)} + n, id, 'dungeon-pet-' || n,
                            'CLAW_CAT', '伙伴' || (n + 1), '水', 12, 86440, 'A', '完好防护', '["水弹","微疗"]', n
                        FROM player_character CROSS JOIN generate_series(0, 3) AS n WHERE name = '${name}';
                    ${
                        complete
                            ? `INSERT INTO owned_item
                        (id, character_id, source, code, name, quantity, slot, required_level, stats, equipped, bound, pending_quantity)
                        SELECT ${BigInt(`0x${suffix}`) * 100n + BigInt(i * 10 + 5)} + n, id, 'dungeon-supply-' || n,
                            CASE WHEN n = 0 THEN 'HEALTH_POTION' ELSE 'MANA_POTION' END,
                            CASE WHEN n = 0 THEN '生命药' ELSE '魔力药' END, 99, NULL, 1, '{}', false, false, 0
                        FROM player_character CROSS JOIN generate_series(0, 1) AS n WHERE name = '${name}';`
                            : ''
                    }
                    COMMIT;`,
                            stdio: ['pipe', 'pipe', 'pipe'],
                        },
                    )
                }
                for (const playerPage of pages) {
                    await playerPage.goto(`${base}/world`)
                    await clickWorldAction(playerPage, '林间兽栏入口')
                    await clickWorldAction(playerPage, '副本招募')
                }
                await page.getByRole('button', {name: '发起招募', exact: true}).click()
                for (const i of [1, 2])
                    await pages[i]!.getByRole('button', {name: `加入${players[0]!.name}的队伍`, exact: true}).click()
                for (const playerPage of pages) {
                    await playerPage.getByRole('button', {name: '确认加入', exact: true}).click()
                    await playerPage.getByRole('button', {name: '确认出战准备', exact: true}).click()
                }
                await expect(page.getByRole('button', {name: '进入副本', exact: true})).toBeEnabled()
                await page.screenshot({
                    path: testInfo.outputPath('dungeon-lobby.png'),
                    fullPage: true,
                    animations: 'disabled',
                })
                await auditLayout(page, testInfo, 'dungeon-lobby')
                expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
                const dungeonStarted = Date.now()
                await page.getByRole('button', {name: '进入副本', exact: true}).click()
                await expect(page).toHaveURL(/\/dungeon\/\d+$/)
                const run = page.url().split('/').at(-1)!
                for (const playerPage of pages) {
                    await expect(playerPage).toHaveURL(`${base}/dungeon/${run}`)
                    await playerPage.getByRole('button', {name: '准备就绪', exact: true}).click()
                }
                if (!complete) {
                    await expect(page.getByRole('button', {name: '开始遭遇', exact: true})).toBeVisible()
                    // 真正断开一名成员的浏览器网络，等待服务端租约到期，不能只改页面状态。
                    await contexts[2]!.setOffline(true)
                    await expect
                        .poll(
                            async () => {
                                const response = await players[0]!.request.get(`${base}/api/dungeon/runs/${run}`, {
                                    headers: {Authorization: `Bearer ${players[0]!.token}`},
                                })
                                return (await response.json()).canBegin
                            },
                            {timeout: 30_000},
                        )
                        .toBe(false)
                    const refused = await players[0]!.request.post(`${base}/api/dungeon/runs/${run}/begin`, {
                        headers: {Authorization: `Bearer ${players[0]!.token}`},
                    })
                    expect(refused.status()).toBe(409)
                    await contexts[2]!.setOffline(false)
                    await pages[2]!.reload()
                    await expect(pages[2]!.getByRole('button', {name: '准备就绪', exact: true})).toBeVisible()
                    await pages[2]!.getByRole('button', {name: '准备就绪', exact: true}).click()
                }
                await Promise.all([
                    page.waitForResponse(
                        (response) =>
                            response.url().endsWith(`/api/dungeon/runs/${run}/begin`) &&
                            response.request().method() === 'POST',
                    ),
                    page.getByRole('button', {name: '开始遭遇', exact: true}).click(),
                ])
                if (complete) {
                    const dungeonMs = await finishDungeon(pages, players, base, run, testInfo, dungeonStarted)
                    // 同一人物、同一两只在场伙伴。计入往返恢复，使用真实页面战斗和服务端经验。
                    // 三名玩家并行操作；自动化基线不代替真人计时。
                    await clickWorldAction(page, '诺姆城外围')
                    const recover = async () => {
                        for (const name of ['诺姆城', '诺姆城幻兽中心', '全队免费恢复', '诺姆城', '诺姆城外围'])
                            await clickWorldAction(page, name)
                    }
                    await recover()
                    const experience = async () => {
                        const response = await players[0]!.request.get(`${base}/api/world`, {
                            headers: {Authorization: `Bearer ${players[0]!.token}`},
                        })
                        expect(response.ok()).toBe(true)
                        return (await response.json()).character.experience as number
                    }
                    const before = await experience()
                    const wildStarted = Date.now()
                    for (let battle = 0; battle < 20; battle++) {
                        const result = await winStoryBattle(page, '探索诺姆城外围', true)
                        const low = result.units.some(
                            (unit) =>
                                !unit.enemy &&
                                (unit.hero || unit.position < 2) &&
                                (unit.hp < unit.stats.health * 0.7 || unit.mp < unit.stats.mana * 0.25),
                        )
                        if (low || (battle + 1) % 5 === 0) await recover()
                    }
                    const wildMs = Date.now() - wildStarted
                    const wildExperience = (await experience()) - before
                    expect(wildExperience).toBeGreaterThan(0)
                    const comparison = JSON.stringify(
                        {
                            level: 12,
                            battles: 20,
                            dungeonMs,
                            wildMs,
                            wildExperience,
                            wildExperiencePerMinute: (wildExperience * 60000) / wildMs,
                            scope: '自动页面基线；三账号并行操作，入场准备至结算与退出；野外20战、低资源或每五战往返恢复；非真人效率',
                        },
                        null,
                        2,
                    )
                    writeFileSync(testInfo.outputPath('reward-comparison.json'), comparison)
                    const evidence = resolve('..', 'outputs', 'level15')
                    mkdirSync(evidence, {recursive: true})
                    writeFileSync(
                        resolve(evidence, `dungeon-reward-baseline-${testInfo.project.name}.json`),
                        comparison,
                    )
                    expect(errors).toEqual([])
                    return
                }
                for (let i = 0; i < 3; i++) {
                    const playerPage = pages[i]!
                    await expect(
                        playerPage.getByRole('heading', {name: '兽栏外径 · 第 1 回合', exact: true}),
                    ).toBeVisible()
                    await playerPage.getByLabel(`${players[i]!.name}行动`, {exact: true}).selectOption('DEFEND')
                    await playerPage.locator('.actor-strip button').filter({hasText: '伙伴1'}).click()
                    await playerPage.getByLabel('伙伴1行动', {exact: true}).selectOption('SWAP')
                    await playerPage.locator('.actor-strip button').filter({hasText: '伙伴2'}).click()
                    await playerPage.getByLabel('伙伴2行动', {exact: true}).selectOption('微疗')
                    const targets = playerPage.getByLabel('伙伴2目标', {exact: true})
                    await expect(
                        targets.locator('option').filter({hasText: `${players[(i + 1) % 3]!.name} · 伙伴1`}),
                    ).toHaveCount(1)
                    await expect(targets.locator('option').filter({hasText: '你 · 伙伴1'})).toHaveCount(1)
                    await playerPage.getByLabel('伙伴2行动', {exact: true}).selectOption('DEFEND')
                    await expect(playerPage.getByLabel('伙伴3行动', {exact: true})).toHaveCount(0)
                    await expect(playerPage.getByLabel('伙伴4行动', {exact: true})).toHaveCount(0)
                    await expect(playerPage.getByLabel(`${players[(i + 1) % 3]!.name}行动`, {exact: true})).toHaveCount(
                        0,
                    )
                }
                await page.waitForResponse(
                    (response) => response.url().includes('/battles/') && response.request().method() === 'GET',
                )
                await page.locator('.actor-strip button').filter({hasText: players[0]!.name}).click()
                await expect(page.getByLabel(`${players[0]!.name}行动`, {exact: true})).toHaveValue('DEFEND')
                await page.screenshot({
                    path: testInfo.outputPath('dungeon-round.png'),
                    fullPage: true,
                    animations: 'disabled',
                })
                await auditLayout(page, testInfo, 'dungeon-round')
                expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
                await page.getByRole('button', {name: '提交本回合', exact: true}).click()
                await expect(page.getByText('等待本轮结算', {exact: true})).toBeVisible()
                await page.reload()
                await expect(page.getByText('等待本轮结算', {exact: true})).toBeVisible()
                for (const i of [1, 2]) await pages[i]!.getByRole('button', {name: '提交本回合', exact: true}).click()
                await expect(page.getByRole('heading', {name: '兽栏外径 · 第 2 回合', exact: true})).toBeVisible()
                await page.locator('.actor-strip button').filter({hasText: '伙伴3'}).click()
                await expect(page.getByLabel('伙伴3行动', {exact: true})).toBeVisible()
                await expect(page.getByLabel('伙伴1行动', {exact: true})).toHaveCount(0)
                await page.goto(`${base}/world`)
                await expect(page).toHaveURL(`${base}/dungeon/${run}`)
                for (const playerPage of pages) {
                    await playerPage.getByRole('button', {name: '退出副本', exact: true}).click()
                    await playerPage.getByRole('button', {name: '确认退出', exact: true}).click()
                }
                for (const playerPage of pages) await expect(playerPage).toHaveURL(`${base}/world`)
                expect(errors).toEqual([])
            } finally {
                await contexts[1]!.close()
                await contexts[2]!.close()
            }
        },
    )
