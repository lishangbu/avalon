import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {execFileSync} from 'node:child_process'

/** 本地夹具提供重复书；选择数量、取消、确认、余额与刷新均验证真实页面。 */
test('补给商店回收重复书且保留绑定书', async ({page}, info) => {
    test.skip(process.env.AVALON_LOCAL_FIXTURES !== '1', '需要显式启用本地数据库夹具')
    const suffix = randomUUID().replaceAll('-', '').slice(0, 10)
    const name = `book_${suffix}`
    const fixture = BigInt(`0x${suffix}`) * 8n
    await page.goto('/')
    await page.getByRole('button', {name: '创建账号', exact: true}).click()
    await page.getByLabel('账号', {exact: true}).fill(name)
    await page.getByLabel('密码', {exact: true}).fill('Recycling-browser-123')
    await page.getByRole('button', {name: '注册并进入'}).click()
    await page.getByLabel('角色名', {exact: true}).fill(name)
    await page.getByRole('button', {name: '踏上旅途'}).click()
    await expect(page.getByRole('button', {name: '伙伴', exact: true})).toBeVisible()
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
        UPDATE player_character SET gold=30,location='SUPPLY_SHOP' WHERE name='${name}';
        INSERT INTO owned_item(id,character_id,source,code,name,quantity,required_level,stats,equipped,bound)
        SELECT ${fixture},id,'recycle-bound','BOOK_WATER','水弹技能书',1,1,'{}',false,true FROM player_character WHERE name='${name}';
        INSERT INTO owned_item(id,character_id,source,code,name,quantity,required_level,stats,equipped,bound)
        SELECT ${fixture + 1n},id,'recycle-drop','BOOK_WATER','水弹技能书',3,1,'{}',false,false FROM player_character WHERE name='${name}';
        COMMIT;`,
        },
    )
    await page.goto('/shop')
    await expect(page.getByText('每本 6 金币 · 可出售 3 本', {exact: true})).toBeVisible()
    const quantity = page.getByLabel('水弹技能书回收数量', {exact: true})
    const sell = page.getByRole('button', {name: '出售 水弹技能书', exact: true})
    await quantity.fill('4')
    await expect(sell).toBeDisabled()
    await quantity.fill('2')
    await sell.click()
    await expect(page.getByRole('dialog', {name: '确认回收技能书', exact: true})).toBeVisible()
    const sheet = page
        .locator('ion-modal')
        .filter({has: page.locator('ion-title').getByText('确认回收技能书', {exact: true})})
    await expect(sheet.getByText('获得 12 金币', {exact: true})).toBeVisible()
    await sheet.getByRole('button', {name: '取消', exact: true}).click()
    await expect(sheet).toBeHidden()
    await expect(page.getByText('持有 30 金币', {exact: true})).toBeVisible()
    await sell.click()
    await expect(sheet.getByRole('button', {name: '确认出售', exact: true})).toBeInViewport({ratio: 1})
    await page.screenshot({path: info.outputPath('book-recycling.png'), fullPage: true})
    await sheet.getByRole('button', {name: '确认出售', exact: true}).click()
    await expect(sheet).toBeHidden()
    await expect(page.getByText('持有 42 金币', {exact: true})).toBeVisible()
    await quantity.fill('1')
    await sell.click()
    await sheet.getByRole('button', {name: '确认出售', exact: true}).click()
    await expect(sheet).toBeHidden()
    await page.reload()
    await expect(page.getByText('持有 48 金币', {exact: true})).toBeVisible()
    await expect(page.getByText('暂无可回收的重复技能书', {exact: true})).toBeVisible()
    const remaining = execFileSync(
        'docker',
        [
            'exec',
            process.env.AVALON_TEST_DB_CONTAINER ?? 'avalon-postgres-1',
            'psql',
            '-U',
            process.env.AVALON_TEST_DB_USER ?? 'avalon',
            '-d',
            process.env.AVALON_TEST_DB_NAME ?? 'avalon',
            '-At',
            '-c',
            `SELECT bound || ':' || quantity FROM owned_item WHERE id IN (${fixture},${fixture + 1n}) ORDER BY id`,
        ],
        {encoding: 'utf8'},
    )
    expect(remaining.trim()).toBe('true:1\nfalse:0')
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
})
