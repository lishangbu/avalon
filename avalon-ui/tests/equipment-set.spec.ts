import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {execFileSync} from 'node:child_process'
import {auditLayout} from './support/layout-audit'

/** 夹具提供等级和套装，实际穿脱、套装查询及刷新走正式接口。 */
test('巡林者套装随穿脱显示激活门槛并在刷新后保留', async ({page}, info) => {
    test.skip(process.env.AVALON_LOCAL_FIXTURES !== '1', '需要显式启用本地数据库夹具')
    const suffix = randomUUID().replaceAll('-', '').slice(0, 12)
    const name = `套${suffix}`
    const id = BigInt(`0x${suffix}`)
    await page.goto('/')
    await page.getByRole('button', {name: '创建账号', exact: true}).click()
    await page.getByLabel('账号', {exact: true}).fill(`set_${suffix}`)
    await page.getByLabel('密码', {exact: true}).fill('Equipment-set-123')
    await page.getByRole('button', {name: '注册并进入'}).click()
    await page.getByLabel('角色名', {exact: true}).fill(name)
    await page.getByRole('button', {name: '踏上旅途'}).click()
    await expect(page).toHaveURL(/\/world$/)
    const pieces = [
        ['CRYSTAL_HEAD', '巡林者头盔', 'HEAD', 30],
        ['CRYSTAL_BODY', '巡林者外衣', 'BODY', 60],
        ['CRYSTAL_HANDS', '巡林者护手', 'HANDS', 15],
        ['CRYSTAL_FEET', '巡林者长靴', 'FEET', 20],
    ] as const
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
        UPDATE player_character SET experience=264820 WHERE name='${name}';
        ${pieces
            .map(
                ([code, label, slot, health], index) => `INSERT INTO owned_item
            (id, character_id, source, code, name, quantity, slot, required_level, stats, equipped, bound, quality)
            SELECT ${id + BigInt(index)}, id, 'set-fixture-${index}', '${code}', '${label}', 1, '${slot}', 15,
            '{"health":${health}}', false, true, '精良' FROM player_character WHERE name='${name}';`,
            )
            .join('\n')}
        COMMIT;`,
            stdio: ['pipe', 'pipe', 'pipe'],
        },
    )
    await page.getByRole('button', {name: '背包', exact: true}).click()
    await page.getByRole('button', {name: '装备', exact: true}).click()
    const current = page.locator('.ion-page').filter({visible: true})
    for (const [, label] of pieces) {
        await current.getByRole('button', {name: new RegExp(label)}).click()
        const sheet = page.locator('ion-modal').filter({has: page.locator('ion-title').getByText(label, {exact: true})})
        await sheet.getByRole('button', {name: '穿戴', exact: true}).click()
        await expect(sheet).not.toBeVisible()
    }
    await expect(current.getByRole('heading', {name: '巡林者套装 · 4/5'})).toBeVisible()
    await expect(current.getByText('2 件 · 生命上限 +30 · 已激活')).toBeVisible()
    await expect(current.getByText('4 件 · 已激活', {exact: true})).toBeVisible()
    await expect(current.getByText(/提供 30 点护盾/)).toBeVisible()
    await page.screenshot({path: info.outputPath('crystal-set.png'), fullPage: true})
    await auditLayout(page, info, 'crystal-set')
    await current.getByRole('button', {name: /巡林者长靴/}).click()
    const sheet = page
        .locator('ion-modal')
        .filter({has: page.locator('ion-title').getByText('巡林者长靴', {exact: true})})
    await sheet.getByRole('button', {name: '卸下', exact: true}).click()
    await expect(sheet).not.toBeVisible()
    await expect(current.getByText('4 件 · 未激活', {exact: true})).toBeVisible()
    await page.reload()
    await page.getByRole('button', {name: '装备', exact: true}).click()
    await expect(current.getByRole('heading', {name: '巡林者套装 · 3/5'})).toBeVisible()
})
