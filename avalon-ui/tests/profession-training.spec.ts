// 夹具准备学院基础课通过事实及真实低资源；职业课程全程由页面操作，不伪造课程结果。
import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {execFileSync} from 'node:child_process'

for (const course of [
    {profession: '战士', skill: '盾墙', rounds: 2},
    {profession: '法师', skill: '魔爆术', rounds: 1},
    {profession: '牧师', skill: '恢复', rounds: 9},
    {profession: '游侠', skill: '猎人印记', rounds: 2},
]) {
    test(`${course.profession}学院职业课程、重连与资源隔离`, async ({page}, testInfo) => {
        test.setTimeout(90_000)
        test.skip(process.env.AVALON_LOCAL_FIXTURES !== '1', '需要显式启用本地数据库夹具')
        const suffix = randomUUID().replaceAll('-', '').slice(0, 10)
        const name = `训练${suffix}`
        const errors: string[] = []
        page.on('pageerror', (error) => errors.push(error.message))
        await page.goto('/')
        await page.getByRole('button', {name: '创建账号', exact: true}).click()
        await page.getByLabel('账号', {exact: true}).fill(`train_${suffix}`)
        await page.getByLabel('密码', {exact: true}).fill('Training-browser-123')
        await page.getByRole('button', {name: '注册并进入'}).click()
        await page.getByLabel('角色名', {exact: true}).fill(name)
        await page.getByRole('combobox', {name: '职业', exact: true}).selectOption({label: course.profession})
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
                UPDATE player_character SET gold = 40, experience = 12120, location = 'ACADEMY' WHERE name = '${name}';
                INSERT INTO character_fact(id, character_id, code, created_at)
                    SELECT ${BigInt(`0x${suffix}`) + 1n}, id, 'QUEST:O012', now() FROM player_character WHERE name = '${name}';
                INSERT INTO character_fact(id, character_id, code, created_at)
                    SELECT ${BigInt(`0x${suffix}`)}, id, 'TRAINING:ACADEMY_DEFENSE', now() FROM player_character WHERE name = '${name}';
                INSERT INTO combat_vitals(id, character_id, hp, mp)
                    SELECT id, id, 10, 2 FROM player_character WHERE name = '${name}';
                COMMIT;`,
                stdio: ['pipe', 'pipe', 'pipe'],
            },
        )
        await page.reload()
        await page.getByRole('button', {name: '角色', exact: true}).click()
        const skills = page.getByRole('region', {name: '职业技能'})
        await expect(skills).toBeVisible()
        await expect(skills).not.toContainText(course.skill)
        await page.getByRole('button', {name: '冒险', exact: true}).click()
        await page.getByRole('button', {name: '开始我的职业办法', exact: true}).click()
        await expect(page.getByRole('heading', {name: '第 1 回合', exact: true})).toBeVisible()
        // 主动离开战斗地址仍由路由守卫恢复进行中的训练，刷新不重建试题。
        await page.goto('/world')
        await expect(page).toHaveURL(/\/battle$/)
        for (let round = 1; round <= course.rounds; round++) {
            await expect(page.getByRole('heading', {name: `第 ${round} 回合`, exact: true})).toBeVisible()
            if (course.profession === '法师') {
                await page.getByLabel('训练人物行动', {exact: true}).selectOption('魔爆术')
                for (let target = 1; target <= 3; target++)
                    await page.getByRole('checkbox', {name: `训练靶${target}`, exact: true}).check()
            } else if (course.profession === '战士') {
                await page.getByLabel('训练人物行动', {exact: true}).selectOption(round === 1 ? '盾墙' : 'DEFEND')
            } else if (course.profession === '牧师') {
                for (const actor of ['训练人物', '受伤学员1', '受伤学员2']) {
                    await page.locator('.actor-strip button').filter({hasText: actor}).click()
                    await page.getByLabel(`${actor}行动`, {exact: true}).selectOption('DEFEND')
                }
                if ([1, 4, 7].includes(round)) {
                    await page.locator('.actor-strip button').filter({hasText: '训练人物'}).click()
                    await page.getByLabel('训练人物行动', {exact: true}).selectOption('恢复')
                    await page
                        .getByLabel('训练人物目标', {exact: true})
                        .selectOption(round === 1 ? 'trainee' : `patient-${round === 4 ? 0 : 1}`)
                }
            } else {
                await page.getByLabel('训练人物行动', {exact: true}).selectOption(round === 1 ? '猎人印记' : 'ATTACK')
                await page.locator('.actor-strip button').filter({hasText: '训练伙伴'}).click()
                await page.getByLabel('训练伙伴行动', {exact: true}).selectOption(round === 1 ? 'DEFEND' : 'ATTACK')
            }
            if (round === 1) {
                expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
                await page.screenshot({
                    path: testInfo.outputPath('training-orders.png'),
                    fullPage: true,
                    animations: 'disabled',
                })
            }
            await page.getByRole('button', {name: '提交本回合', exact: true}).click()
        }
        await expect(page.getByRole('heading', {name: '战斗胜利', exact: true})).toBeVisible()
        await page.reload()
        await page.getByRole('button', {name: '返回地图', exact: true}).click()
        await expect(page.getByText('40 金币', {exact: true})).toBeVisible()
        await expect(page.getByRole('button', {name: '诺姆学院，当前位置'})).toBeVisible()
        await expect(page.getByText(/生命 10\/.*魔力 2\//)).toBeVisible()
        await page.getByRole('button', {name: '角色', exact: true}).click()
        await expect(skills).toContainText(course.skill)
        await expect(skills.locator('article')).toHaveCount(3)
        await page.getByRole('button', {name: '冒险', exact: true}).click()
        await page.getByRole('button', {name: '开始我的职业办法', exact: true}).click()
        await page.getByRole('button', {name: '退出训练', exact: true}).click()
        await page.getByRole('button', {name: '确认退出', exact: true}).click()
        await page.getByRole('button', {name: '返回地图', exact: true}).click()
        await expect(page.getByText('40 金币', {exact: true})).toBeVisible()
        expect(errors).toEqual([])
    })
}
