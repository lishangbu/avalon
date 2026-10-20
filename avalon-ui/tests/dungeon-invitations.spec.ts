import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {execFileSync} from 'node:child_process'
import {auditLayout} from './support/layout-audit'

/** 三个真实账号建立好友关系；等级夹具只用于抵达副本门槛，不替代自然成长验收。 */
test('好友邀请在战斗中接受后保留战局并单独确认入队', async ({page, browser}, info) => {
    test.skip(process.env.AVALON_LOCAL_FIXTURES !== '1', '需要显式启用本地数据库夹具')
    test.setTimeout(90_000)
    const base = info.project.use.baseURL ?? 'http://localhost:8080'
    const suffix = randomUUID().replaceAll('-', '').slice(0, 10)
    const peers: {name: string; accessToken: string; refreshToken: string}[] = []
    const context = await browser.newContext({viewport: info.project.use.viewport})
    const recipient = await context.newPage()
    const errors: string[] = []
    for (const [index, view] of [page, recipient].entries()) {
        view.on('pageerror', (error) => errors.push(error.message))
        // 浏览器恢复会话会轮换令牌，夹具后续请求必须使用该身份当前的令牌。
        view.on('request', (request) => {
            const authorization = request.headers().authorization
            if (authorization?.startsWith('Bearer ') && peers[index]) peers[index]!.accessToken = authorization.slice(7)
        })
    }
    let lobby: string | undefined
    async function post(index: number, path: string, data?: unknown) {
        return page.request.post(`${base}${path}`, {
            headers: {Authorization: `Bearer ${peers[index]!.accessToken}`},
            data,
        })
    }
    try {
        for (let index = 0; index < 3; index++) {
            const registration = await page.request.post(`${base}/api/auth/register`, {
                data: {username: `invite_${suffix}_${index}`, password: 'Invitation-browser-123', device: '测试'},
            })
            expect(registration.ok()).toBe(true)
            const name = `邀请${suffix}${index}`
            peers.push({name, ...(await registration.json())})
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
            if (index) {
                const friendship = await post(0, '/api/friends', {name})
                expect(friendship.ok()).toBe(true)
                expect((await post(index, `/api/friends/${(await friendship.json()).id}/accept`)).ok()).toBe(true)
            }
        }
        for (const [index, view] of [page, recipient].entries()) {
            await view.context().addInitScript((token) => {
                if (!localStorage.getItem('avalon.refresh-token')) localStorage.setItem('avalon.refresh-token', token)
            }, peers[index]!.refreshToken)
        }
        await page.goto(`${base}/dungeon-lobby`)
        await page.getByRole('button', {name: '发起招募', exact: true}).click()
        await expect(page.getByText('1 / 3 人', {exact: true})).toBeVisible()
        const current = await page.request.get(`${base}/api/dungeon/lobbies/current`, {
            headers: {Authorization: `Bearer ${peers[0]!.accessToken}`},
        })
        expect(current.ok(), await current.text()).toBe(true)
        lobby = (await current.json()).lobby.id
        await page.getByRole('button', {name: `邀请${peers[1]!.name}`, exact: true}).click()
        await expect(page.getByText('已发送邀请', {exact: true})).toBeVisible()
        const battle = await post(1, '/api/battle', {encounter: 'CITY_WILDS', requestId: randomUUID()})
        expect(battle.ok(), await battle.text()).toBe(true)
        await recipient.goto(`${base}/battle`)
        const invitation = recipient.getByRole('region', {name: '副本邀请', exact: true})
        await expect(invitation.getByText(new RegExp(peers[0]!.name))).toBeVisible()
        await auditLayout(recipient, info, 'battle-invitation')
        await recipient.screenshot({path: info.outputPath('battle-invitation.png'), fullPage: true})
        await invitation.getByRole('button', {name: '接受邀请', exact: true}).click()
        await expect(recipient.getByText('等待队友 · 2/3 人', {exact: true})).toBeVisible()
        await expect(recipient).toHaveURL(`${base}/battle`)
        await recipient.reload()
        await expect(recipient.getByText('等待队友 · 2/3 人', {exact: true})).toBeVisible()
        await expect(page.getByText('2 / 3 人', {exact: true})).toBeVisible()
        await page.getByRole('button', {name: `邀请${peers[2]!.name}`, exact: true}).click()
        await expect(page.getByText('已发送邀请', {exact: true})).toHaveCount(2)
        const inbox = await page.request.get(`${base}/api/dungeon/invitations`, {
            headers: {Authorization: `Bearer ${peers[2]!.accessToken}`},
        })
        const third = (await inbox.json()).invitations[0].id
        expect((await post(2, `/api/dungeon/invitations/${third}/accept`)).ok()).toBe(true)
        await recipient.getByRole('button', {name: '确认入队', exact: true}).click()
        await expect(recipient.getByText('结束当前战斗后可前往准备。', {exact: true})).toBeVisible()
        await expect(recipient.getByRole('link', {name: '组队准备', exact: true})).toHaveCount(0)
        expect((await post(1, `/api/dungeon/lobbies/${lobby}/ready`)).status()).toBe(409)
        await recipient.getByRole('button', {name: '认输', exact: true}).click()
        await recipient.getByRole('button', {name: '确认认输', exact: true}).click()
        await recipient.getByRole('button', {name: '返回地图', exact: true}).click()
        await recipient.getByRole('link', {name: '组队准备', exact: true}).click()
        await recipient.getByRole('button', {name: '确认出战准备', exact: true}).click()
        await expect(recipient.getByText(/已准备/).first()).toBeVisible()
        expect(errors).toEqual([])
    } finally {
        if (lobby)
            for (let index = 0; index < peers.length; index++) await post(index, `/api/dungeon/lobbies/${lobby}/leave`)
        await context.close()
    }
})
