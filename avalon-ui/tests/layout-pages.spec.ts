import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {execFileSync} from 'node:child_process'
import {auditLayout} from './support/layout-audit'

/** 夹具仅建立页面所需的资源和权限；页面内容仍来自真实接口。 */
test('各业务页面窄屏横屏及系统双主题布局', async ({page}, info) => {
    test.skip(
        process.env.AVALON_LAYOUT_AUDIT !== '1' ||
            process.env.AVALON_LOCAL_FIXTURES !== '1' ||
            info.project.name !== 'mobile',
        '显式开启手机布局验收',
    )
    test.setTimeout(120_000)
    const suffix = randomUUID().replaceAll('-', '').slice(0, 10)
    const name = `布局${suffix}`,
        id = BigInt(`0x${suffix}`)
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    await page.goto('/')
    await expect(page.getByRole('button', {name: '进入冒险', exact: true})).toBeVisible()
    await auditLayout(page, info, 'login')
    await page.getByRole('button', {name: '创建账号', exact: true}).click()
    await page.getByLabel('账号', {exact: true}).fill(`layout_${suffix}`)
    await page.getByLabel('密码', {exact: true}).fill('Layout-browser-123')
    await page.getByRole('button', {name: '注册并进入'}).click()
    await expect(page.getByLabel('角色名', {exact: true})).toBeVisible()
    await auditLayout(page, info, 'create')
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
            UPDATE player_character SET location = 'WEAPON_SHOP', gold = 100 WHERE name = '${name}';
            INSERT INTO character_fact(id, character_id, code, created_at)
                SELECT ${id}, id, 'QUEST:O003', now() FROM player_character WHERE name = '${name}';
            INSERT INTO owned_creature(id, character_id, source, species, name, element, level, experience,
                ability_slot, ability, skills, party_position)
                SELECT ${id}, id, 'layout-pet', 'STONE_GOLEM', '小岩怪', '地', 3, 300, 'A', '坚岩', '["碎岩击","掩护"]', 0
                FROM player_character WHERE name = '${name}';
            INSERT INTO owned_item(id, character_id, source, code, name, quantity, slot, required_level, stats, equipped, bound, pending_quantity)
                SELECT ${id}, id, 'layout-fragments', 'FRAGMENT_THUNDER', '雷牙技能书碎片', 5, NULL, 1, '{}', false, true, 0
                FROM player_character WHERE name = '${name}';
            COMMIT;`,
            stdio: ['pipe', 'pipe', 'pipe'],
        },
    )
    for (const route of [
        {path: '/world', ready: '武器店，当前位置', name: 'world', role: 'button'},
        {path: '/character', ready: 'Web 浏览器', name: 'character'},
        {path: '/shop', ready: '装备', name: 'shop'},
        {path: '/inventory', ready: '雷牙技能书碎片', name: 'inventory'},
        {path: '/crafting', ready: '配方', name: 'crafting', role: 'combobox'},
        {path: '/companions', ready: '出战编队', name: 'companions'},
        {path: `/companions/${id}`, ready: '学习技能', name: 'companion', role: 'button'},
        {path: `/companions/${id}/learning`, ready: '当前技能', name: 'learning'},
    ]) {
        await page.goto(route.path)
        const ready =
            route.role === 'button'
                ? page.getByRole('button', {name: route.ready, exact: true})
                : route.role === 'combobox'
                  ? page.getByRole('combobox', {name: route.ready, exact: true})
                  : page.getByText(route.ready, {exact: true})
        await expect(ready.first()).toBeVisible()
        await auditLayout(page, info, route.name)
    }
    expect(errors).toEqual([])
})
