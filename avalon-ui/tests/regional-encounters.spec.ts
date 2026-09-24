import {clickWorldAction, closeNpc} from './support/npc'
// 区域入口、双怪回合与精英捕捉资格走实际页面；夹具只提供任务进度和封印球。
import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {execFileSync} from 'node:child_process'

test('区域双怪、重连与精英捕获属性预览', async ({page}, testInfo) => {
    test.skip(process.env.AVALON_LOCAL_FIXTURES !== '1', '需要显式启用本地数据库夹具')
    const suffix = randomUUID().replaceAll('-', '').slice(0, 10)
    const name = `探索${suffix}`
    const fixture = BigInt(`0x${suffix}`)
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    const sql = (input: string) =>
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
            {input, stdio: ['pipe', 'pipe', 'pipe']},
        )
    await page.goto('/')
    await page.getByRole('button', {name: '创建账号', exact: true}).click()
    await page.getByLabel('账号', {exact: true}).fill(`wild_${suffix}`)
    await page.getByLabel('密码', {exact: true}).fill('Regional-browser-123')
    await page.getByRole('button', {name: '注册并进入'}).click()
    await page.getByLabel('角色名', {exact: true}).fill(name)
    await page.getByRole('button', {name: '踏上旅途'}).click()
    await expect(page.getByRole('button', {name: '晨露村家门，当前位置', exact: true})).toBeVisible()
    const steps = ['BQ007', 'BQ024', 'BQ025', 'BQ026', 'BQ029']
    const rows = steps.map((step, i) => `(${fixture + BigInt(i)}, '${step}')`).join(',')
    sql(`BEGIN;
        UPDATE player_character SET experience = 130060, location = 'CITY_OUTSKIRTS' WHERE name = '${name}';
        INSERT INTO quest_receipt(id, character_id, step, quest_code, completed, response, created_at)
            SELECT v.id, c.id, v.step, split_part(v.step, '_', 1), false, '{}', now()
            FROM player_character c CROSS JOIN (VALUES ${rows}) AS v(id, step) WHERE c.name = '${name}';
        INSERT INTO character_fact(id, character_id, code, created_at)
            SELECT v.id, c.id, 'QUEST:' || v.step, now()
            FROM player_character c CROSS JOIN (VALUES ${rows}) AS v(id, step) WHERE c.name = '${name}';
        INSERT INTO owned_item(id, character_id, source, code, name, quantity, bound, equipped, required_level, stats)
            SELECT v.id, c.id, v.code, v.code, v.name, 5, false, false, 1, '{}'
            FROM player_character c CROSS JOIN (VALUES
                (${fixture + 10n}, 'SEAL_I', '封印球I'),
                (${fixture + 11n}, 'STRONG_SEAL_I', '强化封印球I'),
                (${fixture + 12n}, 'STRONG_SEAL_II', '强化封印球II')) AS v(id, code, name)
            WHERE c.name = '${name}';
        COMMIT;`)
    await page.reload()
    await expect(page.locator('.encounter-entry').filter({hasText: '诺姆城外围双怪遭遇'})).toContainText('2只普通幻兽')
    await expect(page.locator('.encounter-entry').filter({hasText: '挑战精英影尾狐'})).toContainText('影尾狐 Lv.13')
    await page.getByRole('button', {name: '诺姆城外围双怪遭遇', exact: true}).click()
    const enemies = page.getByRole('region', {name: '敌方'}).locator('article')
    await expect(enemies).toHaveCount(2)
    const generated = await enemies.allTextContents()
    await page.reload()
    await expect(enemies).toHaveCount(2)
    expect(await enemies.allTextContents()).toEqual(generated)
    await page.getByLabel(`${name}行动`, {exact: true}).selectOption('DEFEND')
    await page.getByRole('button', {name: '提交本回合', exact: true}).click()
    await expect(page.getByRole('heading', {name: '第 2 回合', exact: true})).toBeVisible()
    await page.screenshot({path: testInfo.outputPath('regional-duo.png'), fullPage: true, animations: 'disabled'})
    await page.getByRole('button', {name: '认输', exact: true}).click()
    await page.getByRole('button', {name: '确认认输', exact: true}).click()
    await page.getByRole('button', {name: '返回地图', exact: true}).click()
    await expect(page.getByRole('button', {name: '诺姆城，当前位置', exact: true})).toBeVisible()
    // 认输保留受伤状态；先通过正式恢复操作补满生命，避免精英先手击倒导致投球未执行。
    sql(`UPDATE player_character SET location = 'CITY_PETS' WHERE name = '${name}';`)
    await page.reload()
    await clickWorldAction(page, '全队免费恢复')
    await expect(page.getByRole('button', {name: '全队免费恢复', exact: true})).toBeEnabled()
    await closeNpc(page)
    sql(`UPDATE player_character SET location = 'CITY_OUTSKIRTS' WHERE name = '${name}';`)
    await page.reload()
    await page.getByRole('button', {name: '挑战精英影尾狐', exact: true}).click()
    await expect(enemies).toHaveCount(1)
    await enemies.locator('summary').filter({hasText: '捕捉信息'}).click()
    await expect(enemies).toContainText('捕获后：生命')
    await expect(enemies).toContainText('强化封印球II ·')
    await expect(enemies).not.toContainText('强化封印球I ·')
    const action = page.getByLabel(`${name}行动`, {exact: true})
    await expect(action.locator('option[value="SEAL_I"]')).toHaveJSProperty('disabled', true)
    await expect(action.locator('option[value="STRONG_SEAL_I"]')).toHaveJSProperty('disabled', true)
    await expect(action.locator('option[value="STRONG_SEAL_II"]')).toHaveJSProperty('disabled', false)
    const supplies = page.getByRole('region', {name: '封印球库存'})
    const stock = (name: string) => supplies.locator('.row').filter({hasText: name})
    await expect(stock('强化封印球I 适用')).toContainText('×5')
    await expect(stock('强化封印球II 适用')).toContainText('×5')
    await action.selectOption('STRONG_SEAL_II')
    await page.getByRole('button', {name: '提交本回合', exact: true}).click()
    // 无论捕捉成功与否，已经执行的投球消耗都必须更新并可重连恢复。
    await expect(stock('强化封印球II 适用')).toContainText('×4')
    await page.reload()
    await expect(stock('强化封印球II 适用')).toContainText('×4')
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    await page.screenshot({path: testInfo.outputPath('elite-preview.png'), fullPage: true, animations: 'disabled'})
    await supplies.scrollIntoViewIfNeeded()
    await page.screenshot({path: testInfo.outputPath('capture-inventory.png'), fullPage: true, animations: 'disabled'})
    expect(errors).toEqual([])
})
