import {clickWorldAction, closeNpc} from './support/npc'
import {expect, test} from '@playwright/test'
import type {Battle} from '../src/features/battle/types'

test('港城修船、试航战斗与返航交接可在手机和桌面完成', async ({page}, info) => {
    test.skip(!process.env.AVALON_PORT_USER, '需要隔离数据库的港城邀请前置存档')
    test.setTimeout(180_000)
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    await page.goto('/')
    await page.getByLabel('账号', {exact: true}).fill(`${process.env.AVALON_PORT_USER}_${info.project.name}`)
    await page.getByLabel('密码', {exact: true}).fill('Port-browser-1234')
    await page.getByRole('button', {name: '进入冒险', exact: true}).click()
    const click = async (name: string) => clickWorldAction(page, name)
    async function move(name: string) {
        await closeNpc(page)
        await click(name)
        await expect(page.getByRole('button', {name: `${name}，当前位置`, exact: true})).toBeVisible()
    }
    async function claim(name: string) {
        await click(name)
        const sheet = page
            .locator('ion-modal')
            .filter({visible: true})
            .filter({has: page.locator('ion-title').filter({hasText: /^对话$/})})
        await sheet.getByRole('button', {name: '继续', exact: true}).click()
        await expect(sheet).toHaveCount(0)
        await closeNpc(page)
    }
    await click('听罗温说起港城的邀请')
    await expect(page.getByText('你已完成学院实践，可以继续训练伙伴与探索野外。', {exact: true})).toHaveCount(0)
    await expect(page.getByRole('button', {name: '罗温杂货铺，当前位置', exact: true})).toBeVisible()
    for (const location of ['诺姆城', '诺姆城外围', '河口商道', '温蒂妮城']) await move(location)
    await click('登记从诺姆来访')
    await claim('确认抵达温蒂妮')
    await move('白帆码头')
    for (const action of ['检查旧船外板', '检查舱底与接缝', '确认安全试航计划']) await click(action)
    await claim('确认白帆码头的旧船记录')
    await move('白帆船坞')
    for (const action of ['比较板材裂纹', '比较板材受潮情况']) await click(action)
    await click('甲批：边缘开裂的板材')
    await expect(page.getByText(/修船板材需要同时满足结构完整和干燥/)).toBeVisible()
    await expect(page.getByRole('button', {name: '确认好木料也有用途记录', exact: true})).toBeDisabled()
    await click('乙批：完整干燥的板材')
    await claim('确认好木料也有用途记录')
    for (const action of ['试装修船索具', '记录索具松紧结果', '调整并复核索具']) await click(action)
    await claim('确认绳结不是装饰记录')
    await move('白帆码头')
    await click('查看试载箱与重心提示')
    await click('所有重箱集中在同一侧')
    await expect(page.getByText(/再看重心提示：重箱放低/)).toBeVisible()
    await expect(page.getByRole('button', {name: '逐箱确认试载固定', exact: true})).toHaveCount(0)
    await click('重箱放低并靠中线，左右均衡留通道')
    await click('逐箱确认试载固定')
    await claim('确认试载与固定记录')
    await move('近岸望潮台')
    for (const action of ['记录近岸礁石位置', '核对潮位标尺', '核查回港航标']) await click(action)
    await claim('确认近岸航段观察')
    await move('白帆码头')
    await click('与莎琳确认试航队伍')
    await expect(page.getByRole('button', {name: '白帆码头，当前位置', exact: true})).toBeVisible()
    await move('近岸第一航标')
    await click('核验第一航标')
    await page.reload()
    await expect(page.getByRole('button', {name: '近岸第一航标，当前位置', exact: true})).toBeVisible()
    await move('近岸第二航标')
    await click('核验第二航标')
    await move('白帆近岸湾')
    await click('核验第三航标与停靠位')
    await claim('确认首次近岸试航去程')
    await expect(page.getByRole('button', {name: '扶正倾斜的近岸航标', exact: true})).toHaveCount(0)
    await page.screenshot({path: info.outputPath('port-first-voyage.png'), fullPage: true})
    const started = page.waitForResponse(
        (response) => response.url().endsWith('/api/battle') && response.request().method() === 'POST',
    )
    await page.getByRole('button', {name: /航标旁的麻烦/}).click()
    let battle: Battle = await (await started).json()
    for (let turn = 0; battle.status === 'ACTIVE' && turn < 40; turn++) {
        await expect(page.getByRole('heading', {name: `第 ${battle.round} 回合`, exact: true})).toBeVisible()
        const party = battle.units.filter((unit) => !unit.enemy && unit.hp > 0 && (unit.hero || unit.position < 2))
        for (const unit of party) {
            await page
                .locator('.actor-strip button')
                .filter({has: page.locator('strong', {hasText: unit.name})})
                .click()
            const skill = unit.actions.find(
                (action) => action.available && action.mana > 0 && action.target === 'ENEMY',
            )
            await page
                .getByRole('combobox', {name: `${unit.name}行动`, exact: true})
                .selectOption(skill?.id ?? 'ATTACK')
        }
        const submitted = page.waitForResponse(
            (response) =>
                response.url().endsWith(`/api/battle/${battle.id}/turn`) && response.request().method() === 'POST',
        )
        await click('提交本回合')
        battle = await (await submitted).json()
    }
    expect(battle.status, battle.log.join('\n')).toBe('WON')
    await click('返回地图')
    await expect(page).toHaveURL(/\/world$/)
    await click('扶正倾斜的近岸航标')
    await claim('确认航标恢复可用')
    // 提前回港仍不能跳过返程航标，刷新后保留已完成的航标维修。
    await move('白帆码头')
    await expect(page.getByRole('button', {name: '确认完整往返试航', exact: true})).toBeDisabled()
    await page.reload()
    await move('近岸第二航标')
    await click('复核回程第二航标')
    await move('近岸第一航标')
    await click('复核回程第一航标')
    await move('白帆码头')
    for (const action of ['回港检查船底与接缝', '回港检查索具', '回港检查试载箱']) await click(action)
    await claim('确认完整往返试航')
    await click('向莎琳提交本船试航记录')
    await click('确认获准的近岸航段')
    await claim('领取旧船的近岸出航资格')
    await page.reload()
    await expect(page.getByRole('button', {name: '白帆码头，当前位置', exact: true})).toBeVisible()
    await expect(page.getByRole('button', {name: '领取旧船的近岸出航资格', exact: true})).toHaveCount(0)
    await page.screenshot({path: info.outputPath('port-complete.png'), fullPage: true})
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    expect(errors).toEqual([])
})
