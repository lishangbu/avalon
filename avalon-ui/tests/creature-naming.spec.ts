import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {execFileSync} from 'node:child_process'

/** 夹具仅准备幻兽和金币；报价、取消、免费与付费改名均通过正式页面。 */
test('伙伴首次免费改名、付费确认及刷新保留', async ({page}, info) => {
    test.skip(process.env.AVALON_LOCAL_FIXTURES !== '1', '需要显式启用本地数据库夹具')
    const suffix = randomUUID().replaceAll('-', '').slice(0, 10)
    const name = `name_${suffix}`
    const fixture = BigInt(`0x${suffix}`) * 4n
    await page.goto('/')
    await page.getByRole('button', {name: '创建账号', exact: true}).click()
    await page.getByLabel('账号', {exact: true}).fill(name)
    await page.getByLabel('密码', {exact: true}).fill('Naming-browser-123')
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
            input: `UPDATE player_character SET gold=30 WHERE name='${name}';
        INSERT INTO owned_creature(id,character_id,source,species,name,element,level,experience,ability_slot,ability,skills,party_position)
        SELECT ${fixture},id,'naming-browser','SPROUT','嫩芽精','生',1,0,'A','回春','["藤鞭"]',0 FROM player_character WHERE name='${name}';`,
        },
    )
    await page.reload()
    await page.getByRole('button', {name: '伙伴', exact: true}).click()
    await page.locator('.pet-link').filter({hasText: '嫩芽精'}).click()
    await page.getByRole('button', {name: '改名', exact: true}).click()
    await expect(page.getByRole('dialog', {name: '伙伴改名', exact: true})).toBeVisible()
    // 内容投影在 ion-modal 的 light DOM，dialog 位于 shadow DOM，不能用后者限定内容查找。
    const sheet = page
        .locator('ion-modal')
        .filter({has: page.locator('ion-title').getByText('伙伴改名', {exact: true})})
    await expect(sheet.getByText('费用：首次免费', {exact: true})).toBeVisible()
    await sheet.getByLabel('新名称', {exact: true}).fill('青芽')
    await sheet.getByRole('button', {name: '确认改名', exact: true}).click()
    await expect(sheet).toBeHidden()
    await expect(page.getByRole('heading', {name: '青芽', exact: true})).toBeVisible()
    await page.getByRole('button', {name: '改名', exact: true}).click()
    await expect(sheet.getByText('费用：10 金币', {exact: true})).toBeVisible()
    await sheet.getByLabel('新名称', {exact: true}).fill('青叶')
    await expect(sheet.getByRole('button', {name: '确认改名', exact: true})).toBeInViewport()
    await page.screenshot({path: info.outputPath('rename-confirmation.png'), fullPage: true})
    await sheet.getByRole('button', {name: '取消', exact: true}).click()
    await expect(sheet).toBeHidden()
    await expect(page.getByRole('heading', {name: '青芽', exact: true})).toBeVisible()
    await page.getByRole('button', {name: '改名', exact: true}).click()
    await sheet.getByLabel('新名称', {exact: true}).fill('青叶')
    await sheet.getByRole('button', {name: '确认改名', exact: true}).click()
    await expect(sheet).toBeHidden()
    await page.reload()
    await expect(page.getByRole('heading', {name: '青叶', exact: true})).toBeVisible()
    const result = execFileSync(
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
            `SELECT gold || ':' || rename_count FROM player_character c JOIN owned_creature p ON p.character_id=c.id WHERE c.name='${name}'`,
        ],
        {encoding: 'utf8'},
    )
    expect(result.trim()).toBe('20:2')
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
})
