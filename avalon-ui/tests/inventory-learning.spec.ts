// 独立夹具验收：只预置资源，合成、学习及领取均走真实页面和生产接口。
// 显式开启本地夹具后才写入 Docker 开发数据库，不作为自然通关或掉落概率的证据。
import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {execFileSync} from 'node:child_process'

test('碎片合成、学习与部分待领持久化', async ({page}, testInfo) => {
    test.skip(process.env.AVALON_LOCAL_FIXTURES !== '1', '需要显式启用本地数据库夹具')
    test.setTimeout(60_000)
    const suffix = randomUUID().replaceAll('-', '').slice(0, 12)
    const fixtureId = BigInt(`0x${suffix}`) * 4n
    const name = `资源${suffix}`
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    await page.goto('/')
    await page.getByRole('button', {name: '创建账号', exact: true}).click()
    // 数字隔开测试前缀和十六进制随机串，避免拼接处随机命中严格英文短词。
    await page.getByLabel('账号', {exact: true}).fill(`items_0${suffix}`)
    await page.getByLabel('密码', {exact: true}).fill('Inventory-browser-123')
    await page.getByRole('button', {name: '注册并进入'}).click()
    await page.getByLabel('角色名', {exact: true}).fill(name)
    await page.getByRole('button', {name: '踏上旅途'}).click()
    await expect(page.getByRole('button', {name: '晨露村家门，当前位置'})).toBeVisible()

    // 名称只含固定中文前缀和十六进制随机串；使用标准输入传 SQL，不经过 shell 插值。
    // 夹具使用随机正编号，每组预留四个编号，各测试只修改自己刚创建的角色。
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
            UPDATE player_character SET gold = 20 WHERE name = '${name}';
            INSERT INTO owned_creature
                (id, character_id, source, species, name, element, level, experience, ability_slot, ability, skills, party_position)
                SELECT ${fixtureId}, id, 'browser-fixture', 'STONE_GOLEM', '小岩怪', '地', 3, 300, 'A', '坚岩', '["碎岩击","掩护"]', 0
                FROM player_character WHERE name = '${name}';
            INSERT INTO owned_item
                (id, character_id, source, code, name, quantity, slot, required_level, stats, equipped, bound, pending_quantity)
                SELECT ${fixtureId + 1n}, id, 'browser-fragments', 'FRAGMENT_THUNDER', '雷牙技能书碎片', 5, NULL, 1, '{}', false, true, 0
                FROM player_character WHERE name = '${name}';
            INSERT INTO owned_item
                (id, character_id, source, code, name, quantity, slot, required_level, stats, equipped, bound, pending_quantity)
                SELECT ${fixtureId + 2n}, id, 'browser-pending', 'HEALTH_POTION', '生命药', 98, NULL, 1, '{}', false, false, 5
                FROM player_character WHERE name = '${name}';
            COMMIT;`,
            stdio: ['pipe', 'pipe', 'pipe'],
        },
    )
    await page.reload()
    await page.getByRole('button', {name: '背包', exact: true}).click()
    // Ionic 缓存路由页面：断言限定当前可见页，避免读到离屏旧投影。
    const current = page.locator('.ion-page').filter({visible: true})
    const stack = (name: string, quantity: number) =>
        current
            .locator('article')
            .filter({has: page.getByText(name, {exact: true})})
            .filter({has: page.getByText(`×${quantity}`, {exact: true})})
    await expect(stack('生命药', 98)).toBeVisible()
    await stack('生命药', 98).getByRole('button').click()
    const itemDetail = page.locator('ion-modal').filter({has: page.locator('ion-title', {hasText: '生命药'})})
    await expect(itemDetail).toBeVisible()
    await expect(itemDetail.getByText('98', {exact: true})).toBeVisible()
    await expect(itemDetail.getByText('未绑定', {exact: true})).toBeVisible()
    await page.goBack()
    await expect(itemDetail).not.toBeVisible()
    await expect(page).toHaveURL(/\/inventory$/)
    await page.getByRole('button', {name: '碎片合成', exact: true}).click()
    await page.getByRole('combobox', {name: '配方', exact: true}).selectOption('THUNDER_BOOK')
    await page.getByRole('spinbutton').fill('4.5')
    await expect(page.getByRole('button', {name: '合成', exact: true})).toBeDisabled()
    await page.getByRole('spinbutton').fill('5')
    await expect(current.getByText('成品：已绑定。', {exact: true})).toBeVisible()
    await page.getByRole('button', {name: '合成', exact: true}).click()
    await expect(current.getByText('获得 雷牙技能书 · 已绑定', {exact: true})).toBeVisible()
    await page.getByRole('button', {name: '返回', exact: true}).click()
    await expect(stack('雷牙技能书', 1)).toBeVisible()
    await expect(current.getByText('雷牙技能书碎片', {exact: true})).toHaveCount(0)

    await page.getByRole('button', {name: '待领', exact: true}).click()
    const pending = current
    await pending.getByLabel('领取数量').fill('2')
    await pending.getByRole('button', {name: '领取', exact: true}).click()
    await expect(pending.getByText('生命药 ×3', {exact: true})).toBeVisible()
    await page.screenshot({path: testInfo.outputPath('stack-and-pending.png'), fullPage: true, animations: 'disabled'})
    await page.getByRole('button', {name: '物品', exact: true}).click()
    await expect(stack('生命药', 99)).toBeVisible()
    await expect(stack('生命药', 1)).toBeVisible()

    await page.getByRole('button', {name: '伙伴', exact: true}).click()
    await current.locator('.pet-link').filter({hasText: '小岩怪'}).click()
    await page.getByRole('button', {name: '学习技能', exact: true}).click()
    const learning = current
    const book = learning.locator('option').filter({hasText: '雷牙技能书'})
    await expect(book).toHaveCount(1)
    await learning
        .getByRole('combobox', {name: '技能书', exact: true})
        .selectOption((await book.getAttribute('value')) ?? '')
    await learning.getByRole('button', {name: '确认学习', exact: true}).click()
    await expect(learning.getByText('碎岩击、掩护、雷牙', {exact: true})).toBeVisible()
    await expect(learning.getByText(/学会：雷牙。遗忘：无。/)).toBeVisible()
    await learning.getByText(/学会：雷牙。遗忘：无。/).scrollIntoViewIfNeeded()
    await page.screenshot({path: testInfo.outputPath('learning-success.png'), fullPage: true, animations: 'disabled'})
    await page.reload()
    await expect(current.getByText('碎岩击、掩护、雷牙', {exact: true})).toBeVisible()
    // 刷新后的真实投影仍保留技能与消耗结果，验证持久化而非仅成功提示。
    await page.goto('/inventory')
    await expect(stack('生命药', 99)).toBeVisible()
    await expect(current.getByText('雷牙技能书', {exact: true})).toHaveCount(0)
    await page.getByRole('button', {name: '待领', exact: true}).click()
    await expect(pending.getByText('生命药 ×3', {exact: true})).toBeVisible()
    await page.goto('/character')
    await expect(current.getByText('10', {exact: true})).toBeVisible()
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    expect(errors).toEqual([])
})
