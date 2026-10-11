// 夹具只准备独立账号的个体与地点；仓库存取、保护、放生及图鉴全部走页面和正式接口。
import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {execFileSync} from 'node:child_process'

test('仓库存取、锁定与隐藏个体放生后图鉴保留', async ({page}, testInfo) => {
    test.skip(process.env.AVALON_LOCAL_FIXTURES !== '1', '需要显式启用本地数据库夹具')
    const suffix = randomUUID().replaceAll('-', '').slice(0, 12)
    const fixtureId = BigInt(`0x${suffix}`) * 4n
    const name = `伙伴${suffix}`
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    await page.goto('/')
    await page.getByRole('button', {name: '创建账号', exact: true}).click()
    // 数字隔开测试前缀和十六进制随机串，避免拼接处随机命中严格英文短词。
    await page.getByLabel('账号', {exact: true}).fill(`pets_0${suffix}`)
    await page.getByLabel('密码', {exact: true}).fill('Creatures-browser-123')
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
            UPDATE player_character SET location = 'PET_CENTER' WHERE name = '${name}';
            INSERT INTO owned_creature
                (id, character_id, source, species, name, element, level, experience, ability_slot, ability, skills, party_position)
                SELECT ${fixtureId}, id, 'browser-management', 'STONE_GOLEM', '小岩怪', '地', 1, 0, 'H', '守护', '["碎岩击","掩护"]', 4
                FROM player_character WHERE name = '${name}';
            INSERT INTO owned_creature
                (id, character_id, source, species, name, element, level, experience, ability_slot, ability, skills, party_position, locked)
                SELECT ${fixtureId + 1n}, id, 'browser-retained', 'CLAW_CAT', '留守伙伴', '水', 1, 0, 'A', '完好防护', '["水弹","微疗"]', 0, true
                FROM player_character WHERE name = '${name}';
            COMMIT;`,
            stdio: ['pipe', 'pipe', 'pipe'],
        },
    )
    await page.reload()
    await page.getByRole('button', {name: '伙伴', exact: true}).click()
    const pets = page.locator('.ion-page').filter({visible: true})
    await pets.locator('.pet-link').filter({hasText: '小岩怪'}).click()
    await expect(pets.getByText('当前位置：携带位1', {exact: true})).toBeVisible()
    await pets.getByRole('button', {name: '锁定', exact: true}).click()
    await expect(pets.getByRole('button', {name: '解锁', exact: true})).toBeVisible()
    await expect(pets.getByRole('button', {name: '放生', exact: true})).toHaveCount(0)
    await pets.getByRole('button', {name: '存入仓库', exact: true}).click()
    await expect(pets.getByText('当前位置：仓库', {exact: true})).toBeVisible()
    await page.getByRole('button', {name: '返回', exact: true}).click()
    await expect(pets.locator('.pet-link').filter({hasText: '留守伙伴'})).toBeVisible()
    await pets.getByRole('button', {name: '仓库', exact: true}).click()
    await expect(pets.getByText('仓库 1 / 30', {exact: true})).toBeVisible()
    await expect(pets.getByRole('checkbox')).toHaveCount(0)
    await pets.locator('.pet-link').filter({hasText: '小岩怪'}).click()
    await pets.getByRole('combobox', {name: '目标位置', exact: true}).selectOption('4')
    await pets.getByRole('button', {name: '取出并编队', exact: true}).click()
    await expect(pets.getByText('当前位置：携带位1', {exact: true})).toBeVisible()
    await page.getByRole('button', {name: '返回', exact: true}).click()
    // 从详情返回仍停留仓库分类，不能无条件重置为随身。
    await expect(pets.getByText('仓库 0 / 30', {exact: true})).toBeVisible()
    await pets.getByRole('button', {name: '随身', exact: true}).click()
    await pets.locator('.pet-link').filter({hasText: '小岩怪'}).click()
    await pets.getByRole('button', {name: '解锁', exact: true}).click()
    await expect(pets.getByRole('button', {name: '锁定', exact: true})).toBeVisible()
    await page.getByRole('button', {name: '返回', exact: true}).click()
    await pets.getByRole('checkbox', {name: '选择放生小岩怪'}).check()
    await pets.getByRole('button', {name: '放生所选（1）', exact: true}).click()
    const confirmation = page
        .locator('ion-modal')
        .filter({has: page.locator('ion-title').getByText('确认放生', {exact: true})})
    await expect(confirmation.getByText('隐藏特性个体', {exact: true})).toBeVisible()
    await expect(confirmation.getByText('技能：碎岩击、掩护', {exact: true})).toBeVisible()
    await page.screenshot({
        path: testInfo.outputPath('release-confirmation.png'),
        fullPage: true,
        animations: 'disabled',
    })
    await confirmation.getByRole('button', {name: '取消', exact: true}).click()
    await expect(pets.locator('.pet-link').filter({hasText: '小岩怪'})).toBeVisible()
    await pets.getByRole('checkbox', {name: '选择放生小岩怪'}).check()
    await pets.getByRole('button', {name: '放生所选（1）', exact: true}).click()
    await confirmation.getByRole('button', {name: '确认放生', exact: true}).click()
    await expect(confirmation).toBeHidden()
    await expect(pets.locator('.pet-link').filter({hasText: '留守伙伴'})).toBeVisible()
    await expect(pets.locator('.pet-link').filter({hasText: '小岩怪'})).toHaveCount(0)
    await page.reload()
    await pets.getByRole('button', {name: '图鉴', exact: true}).click()
    const stone = pets.locator('article').filter({hasText: '小岩怪'})
    await expect(stone.getByText('已收集 · 持有 0', {exact: true})).toBeVisible()
    await expect(stone.getByText('隐藏：守护', {exact: true})).toBeVisible()
    await pets.getByRole('button', {name: '领取图鉴奖励', exact: true}).click()
    await expect(stone.getByText('首次收集奖励已领取', {exact: true})).toBeVisible()
    await expect(pets.getByRole('button', {name: '领取图鉴奖励', exact: true})).toHaveCount(0)
    await page.screenshot({path: testInfo.outputPath('encyclopedia.png'), fullPage: true, animations: 'disabled'})
    await page.reload()
    await page.getByRole('button', {name: '冒险', exact: true}).click()
    await expect(pets.getByText('20 金币', {exact: true})).toBeVisible()
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    expect(errors).toEqual([])
})
