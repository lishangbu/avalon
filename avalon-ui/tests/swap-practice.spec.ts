// 夹具准备可用替补；进入、换怪、重连和结束均使用正式浏览器流程。
import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {execFileSync} from 'node:child_process'
import {auditLayout} from './support/layout-audit'

test('换怪练习结束后保留真实编队与资源', async ({page}, testInfo) => {
    test.skip(process.env.AVALON_LOCAL_FIXTURES !== '1', '需要显式启用本地数据库夹具')
    const suffix = randomUUID().replaceAll('-', '').slice(0, 10)
    const fixture = BigInt(`0x${suffix}`)
    const name = `换怪${suffix}`
    await page.goto('/')
    await page.getByRole('button', {name: '创建账号', exact: true}).click()
    await page.getByLabel('账号', {exact: true}).fill(`swap_${suffix}`)
    await page.getByLabel('密码', {exact: true}).fill('Swap-browser-123')
    await page.getByRole('button', {name: '注册并进入'}).click()
    await page.getByLabel('角色名', {exact: true}).fill(name)
    await page.getByRole('button', {name: '踏上旅途'}).click()
    await expect(page.getByRole('button', {name: '晨露村家门，当前位置'})).toBeVisible()
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
            UPDATE player_character SET experience = 12120, location = 'ACADEMY' WHERE name = '${name}';
            INSERT INTO character_fact(id, character_id, code, created_at)
                SELECT ${fixture + 1n}, id, 'QUEST:BQ016', now() FROM player_character WHERE name = '${name}';
            INSERT INTO character_fact(id, character_id, code, created_at)
                SELECT ${fixture}, id, 'TRAINING:BQ018', now() FROM player_character WHERE name = '${name}';
            INSERT INTO owned_creature(id, character_id, source, species, name, element, level, experience,
                ability_slot, ability, skills, party_position)
                SELECT v.id, c.id, v.source, v.species, v.name, v.element, 1, 0, 'A', v.ability, v.skills, v.position
                FROM player_character c CROSS JOIN (VALUES
                    (${fixture + 1n}, 'swap-cat', 'CLAW_CAT', '利爪猫', '水', '完好防护', '["水弹","微疗"]', 0),
                    (${fixture + 2n}, 'swap-stone', 'STONE_GOLEM', '小岩怪', '地', '坚岩', '["碎岩击","掩护"]', 2)
                ) AS v(id, source, species, name, element, ability, skills, position) WHERE c.name = '${name}';
            INSERT INTO combat_vitals(id, character_id, hp, mp)
                SELECT id, id, 10, 2 FROM player_character WHERE name = '${name}';
            COMMIT;`,
            stdio: ['pipe', 'pipe', 'pipe'],
        },
    )
    await page.reload()
    for (const partner of ['借用利爪猫', '借用小岩怪', '借用芽灵', '借用青岚雀'])
        await page.getByRole('button', {name: partner, exact: true}).click()
    await page.getByRole('button', {name: '开始留给替补的位置', exact: true}).click()
    await page.getByRole('button', {name: '完整战报', exact: true}).click()
    await page.evaluate(() => history.go(-2))
    await expect(page.locator('ion-modal:visible')).toHaveCount(0)
    await expect(page).toHaveURL(/\/battle$/)
    await page.reload()
    await expect(page.getByRole('heading', {name: '第 1 回合', exact: true})).toBeVisible()
    await page.goto('/inventory')
    await expect(page).toHaveURL(/\/battle$/)
    await expect(page.getByRole('heading', {name: '第 1 回合', exact: true})).toBeVisible()
    await expect(page.getByRole('navigation', {name: '主导航'})).toHaveCount(0)
    await page.getByRole('button', {name: '完整战报', exact: true}).click()
    const report = page
        .locator('ion-modal')
        .filter({has: page.locator('ion-title').getByText('完整战报', {exact: true})})
    await expect(report).toBeVisible()
    await page.goBack()
    await expect(report).not.toBeVisible()
    await expect(page).toHaveURL(/\/battle$/)
    await page.goForward()
    await expect(report).not.toBeVisible()
    await expect(page.getByRole('heading', {name: '第 1 回合', exact: true})).toBeVisible()
    await page.getByRole('button', {name: '完整战报', exact: true}).click()
    await expect(report).toBeVisible()
    // 浏览器历史菜单可以跳过多项；即使越过弹层占位，仍必须回到服务器允许的战场。
    await page.evaluate(() => history.go(-2))
    await expect(report).not.toBeVisible()
    await expect(page).toHaveURL(/\/battle$/)
    await expect(page.getByRole('heading', {name: '第 1 回合', exact: true})).toBeVisible()
    await page.reload()
    await expect(page).toHaveURL(/\/battle$/)
    await page.getByRole('combobox', {name: '借用人物行动', exact: true}).selectOption('DEFEND')
    await page.locator('.actor-strip button').filter({hasText: '借用小岩怪'}).click()
    await page.getByRole('combobox', {name: '借用小岩怪行动', exact: true}).selectOption('DEFEND')
    await page.locator('.actor-strip button').filter({hasText: '借用利爪猫'}).click()
    await page.getByRole('combobox', {name: '借用利爪猫行动', exact: true}).selectOption('SWAP')
    await page.getByRole('combobox', {name: '借用利爪猫目标', exact: true}).selectOption('T_SPROUT')
    await page.screenshot({path: testInfo.outputPath('swap-practice.png'), fullPage: true, animations: 'disabled'})
    await auditLayout(page, testInfo, 'battle-orders')
    await page.getByRole('button', {name: '提交本回合', exact: true}).click()
    await expect(page.getByRole('heading', {name: '战斗胜利', exact: true})).toBeVisible()
    await page.getByRole('button', {name: '返回地图', exact: true}).click()
    await expect(page.getByText(/生命 10\/.*魔力 2\//)).toBeVisible()
    await expect(page.getByText('0 金币', {exact: true})).toBeVisible()
    // 重新进入课程需要重新确认四只借用伙伴；上一场选择不作为本次自动授权。
    for (const partner of ['借用利爪猫', '借用小岩怪', '借用芽灵', '借用青岚雀'])
        await page.getByRole('button', {name: partner, exact: true}).click()
    await page.getByRole('button', {name: '开始留给替补的位置', exact: true}).click()
    await page.locator('.actor-strip button').filter({hasText: '借用利爪猫'}).click()
    await expect(page.getByRole('combobox', {name: '借用利爪猫行动', exact: true})).toBeVisible()
    await expect(page.getByRole('combobox', {name: '借用小岩怪行动', exact: true})).toHaveCount(0)
    await page.getByRole('button', {name: '退出训练', exact: true}).click()
    await page.getByRole('button', {name: '确认退出', exact: true}).click()
    await page.getByRole('button', {name: '返回地图', exact: true}).click()
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
})
