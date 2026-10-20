import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {execFileSync} from 'node:child_process'
import {auditLayout} from './support/layout-audit'

/** 夹具只解锁等级和前置；补员发布、确认、准备和入本使用实际页面。 */
test('副本发布补员、候补确认与刷新恢复', async ({browser, page}, testInfo) => {
    test.skip(process.env.AVALON_LOCAL_FIXTURES !== '1', '需要显式启用本地数据库夹具')
    test.setTimeout(120_000)
    const base = testInfo.project.use.baseURL ?? 'http://localhost:8080'
    const suffix = randomUUID().replaceAll('-', '').slice(0, 10)
    const candidateContext = await browser.newContext(testInfo.project.use)
    const candidatePage = await candidateContext.newPage()
    const tokens: {accessToken: string; refreshToken: string}[] = []
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    candidatePage.on('pageerror', (error) => errors.push(error.message))
    async function post(index: number, path: string, data?: unknown) {
        const response = await page.request.post(`${base}${path}`, {
            headers: {Authorization: `Bearer ${tokens[index]!.accessToken}`},
            data,
        })
        expect(response.ok(), `${path}: ${response.status()}`).toBe(true)
        return response
    }
    try {
        for (let index = 0; index < 4; index++) {
            const response = await page.request.post(`${base}/api/auth/register`, {
                data: {username: `repl_${suffix}_${index}`, password: 'Dungeon-browser-123', device: '测试'},
            })
            expect(response.ok()).toBe(true)
            tokens.push(await response.json())
            const name = `补员${suffix}${index}`
            await post(index, '/api/world/character', {name, profession: 'WARRIOR'})
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
                UPDATE player_character SET experience=264820, location='FOREST_PEN' WHERE name='${name}';
                INSERT INTO character_fact(id,character_id,code,created_at)
                SELECT ${BigInt(`0x${suffix}`) * 10n + BigInt(index)},id,'QUEST:O014',now()
                FROM player_character WHERE name='${name}';
                COMMIT;`,
                    stdio: ['pipe', 'pipe', 'pipe'],
                },
            )
        }
        const lobby = (await (await post(0, '/api/dungeon/lobbies', {requestId: randomUUID()})).json()).id
        for (const index of [1, 2]) await post(index, `/api/dungeon/lobbies/${lobby}/join`)
        for (const index of [0, 1, 2]) {
            await post(index, `/api/dungeon/lobbies/${lobby}/confirm`)
            await post(index, `/api/dungeon/lobbies/${lobby}/ready`)
        }
        const run = (await (await post(0, `/api/dungeon/lobbies/${lobby}/launch`)).json()).run
        await post(2, `/api/dungeon/runs/${run}/leave`)
        for (const [playerPage, index] of [
            [page, 0],
            [candidatePage, 3],
        ] as const) {
            await playerPage.context().addInitScript((token) => {
                if (!localStorage.getItem('avalon.refresh-token')) localStorage.setItem('avalon.refresh-token', token)
            }, tokens[index]!.refreshToken)
        }
        await page.goto(`${base}/dungeon/${run}`)
        await page.getByRole('button', {name: '发布补员招募', exact: true}).click()
        await expect(page.getByText('补员招募中', {exact: true})).toBeVisible()
        await candidatePage.goto(`${base}/dungeon-lobby`)
        const offer = candidatePage.locator(`article[data-run-id="${run}"]`)
        await offer.getByRole('button', {name: '申请补员', exact: true}).click()
        await expect(candidatePage.getByText('补员确认', {exact: true})).toBeVisible()
        await expect(candidatePage.getByRole('button', {name: '进入副本', exact: true})).toBeDisabled()
        await auditLayout(candidatePage, testInfo, 'replacement-confirmation')
        await candidatePage.screenshot({path: testInfo.outputPath('replacement-confirmation.png'), fullPage: true})
        await candidatePage.getByRole('button', {name: '确认加入', exact: true}).click()
        await candidatePage.getByRole('button', {name: '确认出战准备', exact: true}).click()
        await candidatePage.getByRole('button', {name: '进入副本', exact: true}).click()
        await expect(candidatePage).toHaveURL(`${base}/dungeon/${run}`)
        await candidatePage.reload()
        await expect(candidatePage.getByRole('button', {name: '准备就绪', exact: true})).toBeVisible()
        await expect(page.getByRole('button', {name: '停止补员招募', exact: true})).toHaveCount(0)
        await expect(candidatePage.getByText('补员' + suffix + '3（你）', {exact: true})).toBeVisible()
        expect(errors).toEqual([])
    } finally {
        await candidateContext.close()
    }
})
