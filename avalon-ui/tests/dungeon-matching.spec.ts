import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {execFileSync} from 'node:child_process'
import {auditLayout} from './support/layout-audit'

/** 使用全局匹配池的测试以单 worker 执行，避免不同视口的测试账号互相匹配。 */
test('自动匹配可在个人战斗中确认且必须结算后准备', async ({page}, testInfo) => {
    test.skip(process.env.AVALON_LOCAL_FIXTURES !== '1', '需要显式启用本地数据库夹具')
    test.setTimeout(90_000)
    const base = testInfo.project.use.baseURL ?? 'http://localhost:8080'
    const suffix = randomUUID().replaceAll('-', '').slice(0, 10)
    const tokens: {accessToken: string; refreshToken: string}[] = []
    let lobby: string | undefined
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    page.on('request', (request) => {
        const authorization = request.headers().authorization
        if (authorization?.startsWith('Bearer ') && tokens[0]) tokens[0].accessToken = authorization.slice(7)
    })
    async function post(index: number, path: string, data?: unknown) {
        return page.request.post(`${base}${path}`, {
            headers: {Authorization: `Bearer ${tokens[index]!.accessToken}`},
            data,
        })
    }
    try {
        for (let index = 0; index < 3; index++) {
            const response = await page.request.post(`${base}/api/auth/register`, {
                data: {username: `match_${suffix}_${index}`, password: 'Dungeon-browser-123', device: '测试'},
            })
            expect(response.ok()).toBe(true)
            tokens.push(await response.json())
            const name = `匹配${suffix}${index}`
            expect((await post(index, '/api/world/character', {name, profession: 'WARRIOR'})).ok()).toBe(true)
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
                UPDATE player_character SET experience=264820,location='CITY_OUTSKIRTS' WHERE name='${name}';
                INSERT INTO quest_receipt(id,character_id,step,quest_code,completed,response,created_at)
                SELECT ${BigInt(`0x${suffix}`) * 10n + BigInt(index)},id,'O014','O014',false,'{}',now() FROM player_character WHERE name='${name}';
                INSERT INTO character_fact(id,character_id,code,created_at)
                SELECT ${BigInt(`0x${suffix}`) * 10n + BigInt(index)},id,'QUEST:O014',now() FROM player_character WHERE name='${name}';
                COMMIT;`,
                    stdio: ['pipe', 'pipe', 'pipe'],
                },
            )
        }
        await page.context().addInitScript((token) => {
            if (!localStorage.getItem('avalon.refresh-token')) localStorage.setItem('avalon.refresh-token', token)
        }, tokens[0]!.refreshToken)
        await page.goto(`${base}/dungeon-lobby`)
        await page.getByRole('button', {name: '自动匹配', exact: true}).click()
        await expect(page.getByText('1 / 3 人', {exact: true})).toBeVisible()
        const current = await page.request.get(`${base}/api/dungeon/lobbies/current`, {
            headers: {Authorization: `Bearer ${tokens[0]!.accessToken}`},
        })
        lobby = (await current.json()).lobby.id
        const battle = await post(0, '/api/battle', {encounter: 'CITY_WILDS', requestId: randomUUID()})
        expect(battle.ok(), await battle.text()).toBe(true)
        await page.goto(`${base}/battle`)
        for (const index of [1, 2]) {
            const result = await post(index, '/api/dungeon/lobbies/match')
            expect(result.ok()).toBe(true)
            expect((await result.json()).id).toBe(lobby)
        }
        await page.getByRole('button', {name: '确认入队', exact: true}).click()
        await expect(page.getByText('结束当前战斗后可前往准备。', {exact: true})).toBeVisible()
        await expect(page.getByRole('link', {name: '组队准备', exact: true})).toHaveCount(0)
        expect((await post(0, `/api/dungeon/lobbies/${lobby}/ready`)).status()).toBe(409)
        await auditLayout(page, testInfo, 'battle-matching')
        await page.screenshot({path: testInfo.outputPath('battle-matching.png'), fullPage: true})
        await page.goto(`${base}/dungeon-lobby`)
        await expect(page).toHaveURL(`${base}/battle`)
        await page.getByRole('button', {name: '认输', exact: true}).click()
        await page.getByRole('button', {name: '确认认输', exact: true}).click()
        await page.getByRole('button', {name: '返回地图', exact: true}).click()
        await page.getByRole('link', {name: '组队准备', exact: true}).click()
        await page.getByRole('button', {name: '确认出战准备', exact: true}).click()
        await expect(page.getByText(/已准备/).first()).toBeVisible()
        expect(errors).toEqual([])
    } finally {
        if (lobby)
            for (let index = 0; index < tokens.length; index++) await post(index, `/api/dungeon/lobbies/${lobby}/leave`)
    }
})
