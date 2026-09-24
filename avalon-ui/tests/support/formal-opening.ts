import {clickWorldAction, revealWorldAction, closeNpc} from './npc'
// 正式剧情从注册开始走页面；不写数据库、不调用接口代替移动或领奖。
import {expect, type Page, type TestInfo} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import type {Battle, Lobby} from '../../src/features/battle/types'
import type {BagView} from '../../src/features/inventory/types'
import {
    completeBasicAcademyCourses,
    completeAcademyCapture,
    completeAcademyWorkshops,
    completeWarriorCourse,
} from './academy-courses'

export async function completeFormalOpening(page: Page, testInfo: TestInfo) {
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    const suffix = randomUUID().replaceAll('-', '').slice(0, 10)
    await page.goto('/')
    await clickWorldAction(page, '创建账号')
    await page.getByLabel('账号', {exact: true}).fill(`formal_${suffix}`)
    await page.getByLabel('密码', {exact: true}).fill('Formal-opening-1234')
    await page.getByRole('button', {name: '注册并进入'}).click()
    await page.getByLabel('角色名', {exact: true}).fill(`庆典${suffix}`)
    await page.getByRole('button', {name: '踏上旅途'}).click()
    async function move(name: string) {
        await closeNpc(page)
        await page.getByRole('button', {name, exact: true}).click()
        await expect(page.getByRole('button', {name: `${name}，当前位置`, exact: true})).toBeVisible()
    }
    async function talk(label: string) {
        await clickWorldAction(page, label)
        const sheet = page
            .locator('ion-modal')
            .filter({visible: true})
            .filter({has: page.locator('ion-title').filter({hasText: /^对话$/})})
        await expect(sheet).toContainText('对话')
        await sheet.getByRole('button', {name: '继续', exact: true}).click()
        await expect(sheet).toHaveCount(0)
        await closeNpc(page)
    }
    await expect(page.getByRole('button', {name: '晨露村家门，当前位置', exact: true})).toBeVisible()
    await move('石板路')
    await move('村中广场')
    await talk('接取任务')
    await talk('接受庆典准备')
    await page.reload()
    await expect(page.getByText('村长的介绍', {exact: true})).toBeVisible()
    await move('村长家')
    await talk('接取任务')
    await talk('听取远行建议')
    await move('村中广场')
    await move('武器店')
    await talk('接取任务')
    await talk('领取木剑信物')
    await move('村中广场')
    await move('幻兽中心')
    await talk('接取任务')
    await talk('与利爪猫结契')
    await page.reload()
    await expect(page.getByRole('button', {name: '与利爪猫结契', exact: true})).toHaveCount(0)
    await talk('接取任务')
    const current = () => page.locator('div.ion-page').filter({visible: true})
    await clickWorldAction(page, '伙伴')
    await current().locator('.pet-link').filter({hasText: '利爪猫'}).click()
    await current().getByRole('button', {name: '查看已学技能', exact: true}).click()
    await current().getByRole('combobox', {name: '目标位置', exact: true}).selectOption('0')
    await current().getByRole('button', {name: '调整位置', exact: true}).click()
    await current().getByRole('button', {name: '返回', exact: true}).click()
    await expect(page).toHaveURL(/\/companions$/)
    await clickWorldAction(page, '背包')
    await clickWorldAction(page, '装备')
    for (let i = 0; i < 3; i++) {
        await current().locator('.equipment-row').first().click()
        const detail = page.locator('ion-modal').filter({visible: true})
        await detail.getByRole('button', {name: '穿戴', exact: true}).click()
        await expect(detail).toHaveCount(0)
    }
    await clickWorldAction(page, '冒险')
    await talk('保存出战安排')
    await move('村中广场')
    await move('石板路')
    await move('河边小路')
    await talk('接取任务')
    const scene = page.getByRole('region', {name: '现场行动', exact: true})
    // 两处现场观察分别产生事实；刷新后仍可回看，不重复产生奖励。
    const observations = ['查看河岸外侧的石痕', '核对巢边的石痕']
    for (let i = 0; i < observations.length; i++) {
        await clickWorldAction(page, observations[i])
        await expect(scene.getByRole('button', {name: '回看记录', exact: true})).toHaveCount(i + 1)
    }
    await talk('记录石头移动的原因')
    await page.reload()
    // 等待服务端事实重新加载，避免在空白加载态误判“任务按钮已消失”。
    await expect(scene.getByRole('button', {name: '回看记录', exact: true})).toHaveCount(2)
    await expect(page.getByRole('button', {name: '记录石头移动的原因', exact: true})).toHaveCount(0)
    await scene.scrollIntoViewIfNeeded()
    await move('石板路')
    await move('村中广场')
    await move('幻兽中心')
    // 请求已在真实服务器提交，只丢弃第一次响应，验证结果未知时不会生成第二份赠球请求。
    async function sealInventory() {
        await clickWorldAction(page, '背包')
        await expect(page).toHaveURL(/\/inventory$/)
        const loaded = page.waitForResponse(
            (response) => response.url().endsWith('/api/inventory/bag') && response.request().method() === 'GET',
        )
        await page.reload()
        const bag: BagView = await (await loaded).json()
        await clickWorldAction(page, '冒险')
        await expect(page).toHaveURL(/\/world$/)
        return bag.stacks.filter((stack) => stack.code === 'SEAL_I').reduce((sum, stack) => sum + stack.quantity, 0)
    }
    const ballsBeforeAcceptance = await sealInventory()
    let acceptedPayload = ''
    let acceptanceCalls = 0
    let acceptedResult: unknown
    await page.route('**/api/quests/BQ007/accept', async (route) => {
        const payload = route.request().postData() ?? ''
        if (++acceptanceCalls === 1) {
            acceptedPayload = payload
            const committed = await route.fetch()
            expect(committed.ok()).toBe(true)
            acceptedResult = await committed.json()
            await route.abort('connectionfailed')
        } else {
            expect(payload).toBe(acceptedPayload)
            await route.continue()
        }
    })
    await clickWorldAction(page, '接取任务')
    await expect(page.getByRole('button', {name: '重试接取', exact: true})).toBeVisible()
    const retried = page.waitForResponse(
        (response) => response.url().endsWith('/api/quests/BQ007/accept') && response.request().method() === 'POST',
    )
    await clickWorldAction(page, '重试接取')
    const receipt = await retried
    expect(receipt.ok()).toBe(true)
    expect(await receipt.json()).toEqual(acceptedResult)
    expect(acceptanceCalls).toBe(2)
    await page.unroute('**/api/quests/BQ007/accept')
    const acceptanceDialog = page
        .locator('ion-modal')
        .filter({visible: true})
        .filter({has: page.locator('ion-title').filter({hasText: /^对话$/})})
    await acceptanceDialog.getByRole('button', {name: '继续', exact: true}).click()
    await expect(acceptanceDialog).toHaveCount(0)
    await expect(page.getByText('已接取，请完成现场要求后回报。', {exact: true})).toBeVisible()
    await page.reload()
    await revealWorldAction(page, '登记捕获结果')
    await expect(page.getByText('已接取，请完成现场要求后回报。', {exact: true})).toBeVisible()
    await expect(page.getByRole('button', {name: '接取任务', exact: true})).toHaveCount(0)
    expect(await sealInventory()).toBe(ballsBeforeAcceptance + 5)

    // 只读取页面本来收到的公开战局决定下一步；所有战斗命令都从可见按钮提交。
    // 普通概率失败后保留真实消耗，回城恢复、从商店补球后再尝试。
    let captured = false
    for (let attempt = 0; attempt < 8 && !captured; attempt++) {
        await clickWorldAction(page, '全队免费恢复')
        if (attempt > 0) {
            await move('村中广场')
            await move('杂货铺')
            await clickWorldAction(page, '商店')
            await current()
                .locator('article')
                .filter({has: page.locator('strong', {hasText: /^封印球I$/})})
                .getByRole('button', {name: '购买 · 8 金币', exact: true})
                .click()
            await current().getByRole('button', {name: '返回', exact: true}).click()
            await expect(page).toHaveURL(/\/world$/)
            await move('村中广场')
        } else await move('村中广场')
        await move('石板路')
        await move('河边小路')
        const started = page.waitForResponse(
            (response) => response.url().endsWith('/api/battle') && response.request().method() === 'POST',
        )
        await clickWorldAction(page, '寻找教学小岩怪')
        let battle: Battle = await (await started).json()
        await expect(page).toHaveURL(/\/battle$/)
        await page.goto('/world')
        await expect(page).toHaveURL(/\/battle$/)
        for (let turn = 0; battle.status === 'ACTIVE' && turn < 40; turn++) {
            const enemy = battle.units.find((unit) => unit.enemy && unit.hp > 0)!
            const weakened = enemy.hp * 2 <= enemy.stats.health
            const hasBall = (battle.supplies.SEAL_I ?? 0) > 0
            await expect(page.getByRole('heading', {name: `第 ${battle.round} 回合`, exact: true})).toBeVisible()
            for (const unit of battle.units.filter(
                (unit) => !unit.enemy && unit.hp > 0 && (unit.hero || unit.position < 2),
            )) {
                const action = !hasBall
                    ? 'ATTACK'
                    : unit.hero
                      ? weakened
                          ? 'SEAL_I'
                          : 'DEFEND'
                      : weakened
                        ? 'DEFEND'
                        : unit.actions.some((action) => action.id === '水弹' && action.available)
                          ? '水弹'
                          : 'ATTACK'
                await page
                    .locator('.actor-strip button')
                    .filter({has: page.locator('strong', {hasText: unit.name})})
                    .click()
                await page.getByRole('combobox', {name: `${unit.name}行动`, exact: true}).selectOption(action)
            }
            const submitted = page.waitForResponse(
                (response) =>
                    response.url().endsWith(`/api/battle/${battle.id}/turn`) && response.request().method() === 'POST',
            )
            await clickWorldAction(page, '提交本回合')
            battle = await (await submitted).json()
        }
        expect(battle.status, battle.log.join('\n')).not.toBe('ACTIVE')
        captured = battle.units.some((unit) => unit.enemy && unit.captured)
        await clickWorldAction(page, '返回地图')
        await expect(page).toHaveURL(/\/world$/)
        if (battle.status === 'WON') {
            await move('石板路')
            await move('村中广场')
        }
        await move('幻兽中心')
    }
    expect(captured, '正常概率捕获须在操作预算内成功，不能直接生成伙伴替代').toBe(true)
    await clickWorldAction(page, '伙伴')
    await current().locator('.pet-link').filter({hasText: '小岩怪'}).click()
    await current().getByRole('combobox', {name: '目标位置', exact: true}).selectOption('1')
    await current().getByRole('button', {name: '调整位置', exact: true}).click()
    await expect(current().getByText('当前位置：第二出战位', {exact: true})).toBeVisible()
    await current().getByRole('button', {name: '返回', exact: true}).click()
    await expect(page).toHaveURL(/\/companions$/)
    await clickWorldAction(page, '冒险')
    await clickWorldAction(page, '登记捕获结果')
    await talk('登记自己捕获的新伙伴')
    await move('村中广场')
    await clickWorldAction(page, '领取庆典散石')
    await talk('交付巢外采集的散石')
    await move('幻兽中心')
    await talk('接取任务')
    await clickWorldAction(page, '全队免费恢复')
    await clickWorldAction(page, '阅读出行补给说明')
    await talk('核对恢复与准备')
    await page.reload()
    await expect(page.getByText('林口的车辙', {exact: true})).toBeVisible()
    /** 战斗判断使用公开血量和技能可用性；不调经验接口，也不固定随机结果。 */
    async function fight(label: string) {
        const started = page.waitForResponse(
            (response) => response.url().endsWith('/api/battle') && response.request().method() === 'POST',
        )
        await clickWorldAction(page, label)
        let battle: Battle = await (await started).json()
        for (let turn = 0; battle.status === 'ACTIVE' && turn < 60; turn++) {
            await expect(page.getByRole('heading', {name: `第 ${battle.round} 回合`, exact: true})).toBeVisible()
            const party = battle.units.filter((unit) => !unit.enemy && unit.hp > 0 && (unit.hero || unit.position < 2))
            const injured = [...party].sort((a, b) => a.hp / a.stats.health - b.hp / b.stats.health)[0]!
            for (const unit of party) {
                const available = unit.actions.filter((action) => action.available)
                const danger = battle.telegraph?.includes('即将')
                let action = danger
                    ? 'DEFEND'
                    : (available.find((action) => action.mana > 0 && action.target === 'ENEMY')?.id ?? 'ATTACK')
                let target: string | undefined
                if (
                    unit.hero &&
                    injured.hp / injured.stats.health < 0.45 &&
                    available.some((action) => action.id === 'HEALTH_POTION')
                ) {
                    action = 'HEALTH_POTION'
                    target = injured.id
                } else if (
                    !unit.hero &&
                    injured.hp / injured.stats.health < 0.55 &&
                    available.some((action) => action.id === '微疗')
                ) {
                    action = '微疗'
                    target = injured.id
                }
                await page
                    .locator('.actor-strip button')
                    .filter({has: page.locator('strong', {hasText: unit.name})})
                    .click()
                await page.getByRole('combobox', {name: `${unit.name}行动`, exact: true}).selectOption(action)
                if (target)
                    await page.getByRole('combobox', {name: `${unit.name}目标`, exact: true}).selectOption(target)
            }
            const submitted = page.waitForResponse(
                (response) =>
                    response.url().endsWith(`/api/battle/${battle.id}/turn`) && response.request().method() === 'POST',
            )
            await clickWorldAction(page, '提交本回合')
            battle = await (await submitted).json()
        }
        expect(battle.status, battle.log.join('\n')).toBe('WON')
        await clickWorldAction(page, '返回地图')
        await expect(page).toHaveURL(/\/world$/)
        await expect(current().locator('.row-title').first()).toContainText(/Lv\.\d+/)
    }
    // 推荐5级挑战洞穴：缺口通过普通野战取得，每次沿相邻道路回城恢复。
    const heroSummary = () => current().locator('.row-title').first()
    for (let count = 0; !/Lv\.[5-9]/.test(await heroSummary().innerText()) && count < 250; count++) {
        await clickWorldAction(page, '全队免费恢复')
        await move('村中广场')
        await move('石板路')
        await move('河边小路')
        await fight('探索晨露村外')
        await move('石板路')
        await move('村中广场')
        await move('幻兽中心')
    }
    await expect(heroSummary()).toContainText(/Lv\.[5-9]/)
    await clickWorldAction(page, '全队免费恢复')
    await move('村中广场')
    await move('石板路')
    await move('河边小路')
    await move('东侧林口')
    await clickWorldAction(page, '查看林口车辙')
    await clickWorldAction(page, '核对断开的车辙')
    await talk('说明车辙调查')
    await move('山洞入口')
    await clickWorldAction(page, '查看洞口脚印')
    await clickWorldAction(page, '查看攻击痕迹')
    await fight('挑战獠牙兽王')
    // 洞穴胜利在同一结算事务中完成BQ012，地图直接引导返村赴宴，不重复领奖。
    await expect(page.getByText('赶上开席', {exact: true})).toBeVisible()
    await move('东侧林口')
    await move('河边小路')
    await move('石板路')
    await move('村中广场')
    await talk('回报开路并与伙伴赴宴')
    await move('村长家')
    await talk('领取学院介绍并确认远行')
    await move('村中广场')
    await move('石板路')
    await move('河边小路')
    await move('东侧林口')
    await move('平原路标')
    await talk('接取任务')
    await clickWorldAction(page, '阅读平原路标')
    await clickWorldAction(page, '确认饮水路线')
    await talk('确认驮兽饮水路线')
    await move('小站歇脚处')
    for (const label of ['检查驮兽状态', '确认小站水源', '交接照护用品']) await clickWorldAction(page, label)
    await talk('完成驮兽照护交接')
    await move('诺姆学院')
    await clickWorldAction(page, '查看学院门牌')
    await clickWorldAction(page, '出示学院介绍信')
    await talk('交介绍信并领取课程清单')
    await page.reload()
    await revealWorldAction(page, '开始全队一起行动')
    await expect(
        page.getByRole('region', {name: '学院实践课', exact: true}).getByText('全队一起行动', {exact: true}),
    ).toBeVisible()
    await completeBasicAcademyCourses(page)
    await completeAcademyCapture(page)
    await completeAcademyWorkshops(page)
    await completeWarriorCourse(page)
    // 调查任务只认可接取后的准备；此前课程或编队不能代替本次出发安排。
    await talk('接取任务')
    await clickWorldAction(page, '伙伴')
    await current().locator('.pet-link').filter({hasText: '利爪猫'}).click()
    await current().getByRole('combobox', {name: '目标位置', exact: true}).selectOption('0')
    await current().getByRole('button', {name: '调整位置', exact: true}).click()
    await current().getByRole('button', {name: '返回', exact: true}).click()
    await expect(page).toHaveURL(/\/companions$/)
    await clickWorldAction(page, '冒险')
    await move('学院疗养处')
    await clickWorldAction(page, '全队免费恢复')
    await move('诺姆学院')
    await clickWorldAction(page, testInfo.project.name === 'mobile' ? '先观察食物来源' : '先观察幻兽活动范围')
    await talk('确认自己的野外调查计划')
    await move('诺姆城')
    await clickWorldAction(page, '确认诺姆城街口')
    await talk('抵达诺姆城并确认城郊调查方向')
    await move('诺姆城外围')
    const observationOrder =
        testInfo.project.name === 'mobile'
            ? ['查看花期变化', '观察食物来源', '观察幻兽活动范围']
            : ['观察幻兽活动范围', '观察食物来源', '查看花期变化']
    for (const label of observationOrder) await clickWorldAction(page, label)
    await talk('归纳三处现场观察')
    await clickWorldAction(page, '清理城郊小路')
    await fight('疏通城郊小路')
    // 清路胜利与BQ027奖励在同一事务中结算，页面应直接进入报告任务。
    await expect(page.getByText('我看见的诺姆郊野', {exact: true})).toBeVisible()
    await expect(page.getByRole('button', {name: '确认调查通道已经开放', exact: true})).toHaveCount(0)
    await move('诺姆城')
    await move('诺姆学院')
    await clickWorldAction(page, '保留巢域并调整小路')
    await talk('提交与现场证据吻合的报告')
    await talk('领取独立冒险资格')
    await move('诺姆城')
    await clickWorldAction(page, '参加出发前的晚餐')
    await talk('与熟人共餐并听听航海节')
    await page.reload()
    await expect(page.getByText('橱窗里还缺什么', {exact: true})).toBeVisible()
    await expect(page.getByText('提早开的花', {exact: true})).toHaveCount(0)
    // 未追踪的另一条主线仍在日志中，地图不同时突出两条远程主线。
    await page.goto('/quests')
    await expect(page.getByRole('heading', {name: '罗温重新开门', exact: true})).toBeVisible()
    await expect(page.getByRole('heading', {name: '园圃的花期表', exact: true})).toBeVisible()
    await page.goto('/world')
    // 故事完成后家乡仍开放，通过真实相邻道路回到最初的恢复设施。
    for (const place of [
        '诺姆学院',
        '小站歇脚处',
        '平原路标',
        '东侧林口',
        '河边小路',
        '石板路',
        '村中广场',
        '幻兽中心',
    ])
        await move(place)
    const recovered = page.waitForResponse(
        (response) => response.url().endsWith('/api/battle/recover') && response.request().method() === 'POST',
    )
    await clickWorldAction(page, '全队免费恢复')
    expect((await recovered).ok()).toBe(true)
    await expect(page.getByRole('button', {name: '全队免费恢复', exact: true})).toBeEnabled()
    // 恢复后刷新页面，确认持久化资源已恢复，而不是只捕获到点击后的忙碌状态。
    const refreshed = page.waitForResponse(
        (response) => response.url().endsWith('/api/battle/lobby') && response.request().method() === 'GET',
    )
    await page.reload()
    const lobby: Lobby = await (await refreshed).json()
    expect(lobby.vitals.length).toBeGreaterThanOrEqual(3)
    for (const unit of lobby.vitals) {
        expect(unit.hp, unit.name).toBe(unit.maxHp)
        expect(unit.mp, unit.name).toBe(unit.maxMp)
    }
    await expect(page.getByRole('button', {name: '幻兽中心，当前位置', exact: true})).toBeVisible()
    await current()
        .locator('ion-content')
        .evaluate(async (element) => {
            await (element as HTMLIonContentElement).scrollToTop(0)
        })
    await page.screenshot({path: testInfo.outputPath('formal-opening.png'), fullPage: true, animations: 'disabled'})
    expect(errors).toEqual([])
    return {name: `庆典${suffix}`, move, fight}
}
