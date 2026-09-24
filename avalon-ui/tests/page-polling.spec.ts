// 使用真实招募接口验证请求生命周期；显式模拟 visibilitychange，不将其作为手机系统挂起的真机证明。
import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {execFileSync} from 'node:child_process'

test('招募页面离开及标签页隐藏停止轮询，恢复后重新读取', async ({page}, testInfo) => {
    test.skip(process.env.AVALON_LOCAL_FIXTURES !== '1', '需要显式启用本地数据库夹具')
    test.setTimeout(45_000)
    const suffix = randomUUID().replaceAll('-', '').slice(0, 10)
    const base = testInfo.project.use.baseURL ?? 'http://localhost:8080'
    const response = await page.request.post(`${base}/api/auth/register`, {
        data: {username: `poll_${suffix}`, password: 'Polling-browser-123', device: '测试'},
    })
    expect(response.ok()).toBe(true)
    const tokens = await response.json()
    const name = `轮询${suffix}`
    const character = await page.request.post(`${base}/api/world/character`, {
        headers: {Authorization: `Bearer ${tokens.accessToken}`},
        data: {name, profession: 'WARRIOR'},
    })
    expect(character.ok()).toBe(true)
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
            UPDATE player_character SET experience = 264820, location = 'GNOME_CITY' WHERE name = '${name}';
            INSERT INTO character_fact(id, character_id, code, created_at)
                SELECT ${BigInt(`0x${suffix}`) + 1n}, id, 'QUEST:BQ024', now() FROM player_character WHERE name = '${name}';
            INSERT INTO character_fact(id, character_id, code, created_at)
                SELECT ${BigInt(`0x${suffix}`)}, id, 'QUEST:BQ029', now() FROM player_character WHERE name = '${name}';
            COMMIT;`,
            stdio: ['pipe', 'pipe', 'pipe'],
        },
    )
    await page.addInitScript((token) => localStorage.setItem('avalon.refresh-token', token), tokens.refreshToken)
    let reads = 0
    page.on('request', (request) => {
        if (request.method() === 'GET' && new URL(request.url()).pathname === '/api/dungeon/lobbies/current') reads++
    })
    await page.goto('/world')
    await page.getByRole('button', {name: '副本招募', exact: true}).click()
    await expect(page.getByRole('button', {name: '发起招募', exact: true})).toBeVisible()
    const initial = reads
    await expect.poll(() => reads).toBeGreaterThan(initial)

    // 浏览器无头模式不保证后台标签可见性，模拟平台事件后检查实际网络请求而非内部状态。
    await page.evaluate(() => {
        Object.defineProperty(document, 'visibilityState', {configurable: true, get: () => 'hidden'})
        document.dispatchEvent(new Event('visibilitychange'))
    })
    await page.waitForTimeout(250)
    const hidden = reads
    await page.waitForTimeout(4500)
    expect(reads).toBe(hidden)
    await page.evaluate(() => {
        Reflect.deleteProperty(document, 'visibilityState')
        document.dispatchEvent(new Event('visibilitychange'))
    })
    await expect.poll(() => reads).toBeGreaterThan(hidden)

    await page.getByRole('button', {name: '返回', exact: true}).click()
    await expect(page).toHaveURL(/\/world$/)
    await page.waitForTimeout(250)
    const away = reads
    await page.waitForTimeout(4500)
    expect(reads).toBe(away)
    await page.getByRole('button', {name: '副本招募', exact: true}).click()
    await expect.poll(() => reads).toBeGreaterThan(away)
    await expect(page.getByRole('button', {name: '发起招募', exact: true})).toBeEnabled()
})
