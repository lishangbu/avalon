import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {execFileSync} from 'node:child_process'

/** 夹具仅准备已结算宝箱；领取、装备详情、刷新防重经过实际后端。 */
test('背包领取固定副本宝箱并显示职业限制', async ({page}, info) => {
    test.skip(process.env.AVALON_LOCAL_FIXTURES !== '1', '需要显式启用本地数据库夹具')
    const suffix = randomUUID().replaceAll('-', '').slice(0, 12)
    const name = `箱${suffix}`
    const id = BigInt(`0x${suffix}`)
    await page.goto('/')
    await page.getByRole('button', {name: '创建账号', exact: true}).click()
    await page.getByLabel('账号', {exact: true}).fill(`chest_${suffix}`)
    await page.getByLabel('密码', {exact: true}).fill('Chest-browser-123')
    await page.getByRole('button', {name: '注册并进入'}).click()
    await page.getByLabel('角色名', {exact: true}).fill(name)
    await page.getByRole('button', {name: '踏上旅途'}).click()
    await expect(page).toHaveURL(/\/world$/)
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
            INSERT INTO dungeon_run(id, leader_id, request_id, active, state)
                SELECT ${id}, id, 'fixture-${suffix}', false, '{}' FROM player_character WHERE name='${name}';
            INSERT INTO dungeon_chest(id, character_id, run_id, name, loot, claimed, created_at)
                SELECT ${id}, id, ${id}, '洞穴调查发起者宝箱',
                '{"equipment":"CRYSTAL_STAFF","sellPrice":24,"gold":30,"extras":[{"code":"BOOK_LIGHT_SCREEN","amount":2,"bound":false}]}',
                false, now() FROM player_character WHERE name='${name}';
            COMMIT;`,
            stdio: ['pipe', 'pipe', 'pipe'],
        },
    )
    await page.getByRole('button', {name: '背包', exact: true}).click()
    await expect(page.getByText('洞穴调查发起者宝箱', {exact: true})).toBeVisible()
    await page.getByRole('button', {name: '打开宝箱', exact: true}).click()
    await expect(page.getByRole('button', {name: '打开宝箱', exact: true})).toHaveCount(0)
    await expect(page.getByRole('button', {name: /光幕技能书/})).toContainText('×2')
    await page.getByRole('button', {name: '装备', exact: true}).click()
    await page.getByRole('button', {name: /驯兽法杖/}).click()
    const sheet = page.locator('ion-modal').filter({has: page.locator('ion-title', {hasText: '驯兽法杖'})})
    await expect(sheet.getByText('限法师使用', {exact: true})).toBeVisible()
    await expect(sheet.getByText('不可补购 · 回收价 24 金币', {exact: true})).toBeVisible()
    await page.screenshot({path: info.outputPath('chest-equipment.png'), fullPage: true})
    await page.reload()
    await page.getByRole('button', {name: '物品', exact: true}).click()
    await expect(page.getByRole('button', {name: '打开宝箱', exact: true})).toHaveCount(0)
    await expect(page.getByRole('button', {name: /光幕技能书/})).toContainText('×2')
    await page.getByRole('button', {name: '冒险', exact: true}).click()
    await expect(page.getByText('30 金币', {exact: true})).toBeVisible()
})
