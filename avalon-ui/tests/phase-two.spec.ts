import {clickWorldAction} from './support/npc'
import {devices, expect, test, type Page} from '@playwright/test'
import {readFileSync} from 'node:fs'
import {auditLayout} from './support/layout-audit'

/** 教学材料必须显式允许绑定；进化和兑换均调用真实接口，并验证刷新后的持久状态。 */
test('研究所教学、绑定进化与技能书兑换', async ({page}, info) => {
    test.skip(!process.env.AVALON_PHASE_TWO_FIXTURE, '由PhaseTwoBrowserTest显式启动')
    const fixture = JSON.parse(readFileSync(process.env.AVALON_PHASE_TWO_FIXTURE!, 'utf8'))[info.project.name].research
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    await page.goto('/')
    await page.getByLabel('账号', {exact: true}).fill(fixture.player.username)
    await page.getByLabel('密码', {exact: true}).fill(fixture.player.password)
    await page.getByRole('button', {name: '进入冒险', exact: true}).click()
    await expect(page).toHaveURL(/\/world$/)
    await page.goto('/evolution')
    await page.getByRole('combobox', {name: '教学路线', exact: true}).selectOption('GALE_SPARROW')
    await expect(page.getByRole('button', {name: '确认领取教学材料'})).toBeDisabled()
    await page.getByLabel('我已了解：使用这些材料进化会绑定伙伴，不能交易。').check()
    await page.getByRole('button', {name: '确认领取教学材料'}).click()
    await page.getByRole('combobox', {name: '选择伙伴', exact: true}).selectOption(fixture.petId)
    // 未绑定野生个体默认不动用教学赠送的绑定材料，不能静默替玩家绑定。
    const rejected = page.waitForResponse((response) => response.url().includes(`/${fixture.petId}/evolution`))
    await page.getByRole('button', {name: '查看进化方案'}).click()
    expect((await rejected).ok()).toBe(false)
    await expect(page.getByRole('button', {name: '确认进化（不可撤销）'})).toHaveCount(0)
    await page.getByRole('combobox', {name: '材料使用方式', exact: true}).selectOption('BOUND_FIRST')
    await page.getByRole('button', {name: '查看进化方案'}).click()
    await expect(page.getByText('进化后已绑定，不能交易。', {exact: false})).toBeVisible()
    const bindingLabel = page.locator('label').filter({hasText: '确认兑换绑定物品；使用绑定进化材料会绑定伙伴。'})
    const exchangeButton = page.getByRole('button', {name: '确认兑换', exact: true})
    await expect
        .poll(async () => {
            const label = await bindingLabel.boundingBox(),
                button = await exchangeButton.boundingBox()
            return !!label && !!button && label.y + label.height <= button.y
        })
        .toBe(true)
    await auditLayout(page, info, 'evolution-confirmation')
    await page.screenshot({path: info.outputPath('evolution.png')})
    await page.getByRole('button', {name: '确认进化（不可撤销）'}).click()
    await expect(page.getByText('青岚隼进化完成。生命与魔力不会因进化恢复。')).toBeVisible()
    await page.getByRole('combobox', {name: '兑换项目', exact: true}).selectOption('BOOK_GALE_BLADE')
    await page.getByLabel('确认兑换绑定物品；使用绑定进化材料会绑定伙伴。').check()
    await page.getByRole('button', {name: '确认兑换', exact: true}).click()
    await expect(page.getByText('可用研究凭证：0 枚。兑换物品均绑定，不能交易。')).toBeVisible()
    await page.reload()
    await expect(page.getByRole('combobox', {name: '教学路线', exact: true})).toHaveCount(0)
    await expect(
        page.getByRole('combobox', {name: '选择伙伴', exact: true}).locator(`option[value="${fixture.petId}"]`),
    ).toHaveCount(0)
    await expect(page.getByText('可用研究凭证：0 枚。兑换物品均绑定，不能交易。')).toBeVisible()
    await page.goto(`/companions/${fixture.petId}`)
    await expect(page.getByText('已绑定，不能交易', {exact: true})).toBeVisible()
    await expect(page.getByText(`原捕获者：${fixture.player.username}`, {exact: true})).toBeVisible()
    expect(errors).toEqual([])
})

/** 高等级与初始库存由隔离Java测试提供，所有培养、上架、成交都由浏览器实际操作。 */
test('强化装备通过市场交付，手机确认与刷新状态正确', async ({page, browser}, info) => {
    test.skip(!process.env.AVALON_PHASE_TWO_FIXTURE, '由PhaseTwoBrowserTest显式启动')
    test.setTimeout(90_000)
    page.setDefaultTimeout(15_000)
    const fixtures = JSON.parse(readFileSync(process.env.AVALON_PHASE_TWO_FIXTURE!, 'utf8'))
    const fixture = fixtures[info.project.name]
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    page.on('console', (message) => {
        if (message.type() === 'error') errors.push(message.text())
    })
    async function login(target: Page, player: {username: string; password: string}) {
        await target.goto('/')
        await target.getByLabel('账号', {exact: true}).fill(player.username)
        await target.getByLabel('密码', {exact: true}).fill(player.password)
        await target.getByRole('button', {name: '进入冒险', exact: true}).click()
        await expect(target).toHaveURL(/\/world$/)
    }
    await login(page, fixture.seller)
    await clickWorldAction(page, '装备强化')
    await page.getByRole('combobox', {name: '装备', exact: true}).selectOption(fixture.seller.itemId)
    await page.getByRole('button', {name: '查看强化预览'}).click()
    const confirmation = page
        .locator('ion-modal')
        .filter({has: page.locator('ion-title').getByText('确认单次强化', {exact: true})})
    await expect(confirmation).toContainText('成功率 90%')
    await expect(confirmation).toContainText('10 金币')
    await auditLayout(page, info, 'enhancement-confirmation')
    await page.screenshot({path: info.outputPath('enhancement.png')})
    await confirmation.getByRole('button', {name: '确认强化一次'}).click()
    await expect(confirmation).not.toBeVisible()
    await expect(page.getByText(/强化成功：/)).toBeVisible()
    await page.reload()
    await expect(
        page.getByRole('combobox', {name: '装备', exact: true}).locator('option', {hasText: '+1'}),
    ).toHaveCount(1)
    await page.goto('/market')
    await page.getByLabel('我已了解交易规则').check()
    await page.getByRole('button', {name: '开始使用交易所'}).click()
    await page.getByRole('combobox', {name: '交易功能', exact: true}).selectOption('sell')
    await page.getByRole('combobox', {name: '物品', exact: true}).selectOption(fixture.seller.itemId)
    await page.getByLabel('单价（金币）').fill('100')
    await page.getByRole('button', {name: '查看上架确认'}).click()
    const sale = page.locator('ion-modal').filter({has: page.locator('ion-title').getByText('确认上架', {exact: true})})
    await expect(sale).toContainText('税后收入 95')
    await sale.getByRole('button', {name: '确认上架', exact: true}).click()
    await expect(sale).not.toBeVisible()
    const buyerContext = await browser.newContext({
        ...devices[info.project.name === 'mobile' ? 'Pixel 7' : 'Desktop Chrome'],
        baseURL: process.env.AVALON_BASE_URL,
    })
    try {
        const buyer = await buyerContext.newPage()
        buyer.setDefaultTimeout(15_000)
        buyer.on('pageerror', (error) => errors.push(error.message))
        buyer.on('console', (message) => {
            if (message.type() === 'error') errors.push(message.text())
        })
        await login(buyer, fixture.buyer)
        await buyer.goto('/market')
        await buyer.getByLabel('我已了解交易规则').check()
        await buyer.getByRole('button', {name: '开始使用交易所'}).click()
        await buyer.getByRole('combobox', {name: '类别', exact: true}).selectOption('EQUIPMENT')
        await buyer.getByText('装备筛选', {exact: true}).click()
        await buyer.getByRole('combobox', {name: '强化等级', exact: true}).selectOption('1')
        await buyer.getByRole('button', {name: '查看详情', exact: true}).click()
        const detail = buyer
            .locator('ion-modal')
            .filter({has: buyer.locator('ion-title').getByText('商品详情', {exact: true})})
        await expect(detail).toContainText('强化 +1')
        await auditLayout(buyer, info, 'enhanced-market-detail')
        await buyer.screenshot({path: info.outputPath('market.png')})
        await detail.getByRole('button', {name: '确认购买'}).click()
        await expect(detail).not.toBeVisible()
        await buyer.goto('/enhancement')
        await expect(
            buyer
                .getByRole('combobox', {name: '装备', exact: true})
                .locator(`option[value="${fixture.seller.itemId}"]`),
        ).toContainText('+1')
        await buyer.reload()
        await expect(
            buyer
                .getByRole('combobox', {name: '装备', exact: true})
                .locator(`option[value="${fixture.seller.itemId}"]`),
        ).toContainText('+1')
    } finally {
        await buyerContext.close()
    }
    expect(errors).toEqual([])
})

test('隐藏伙伴取消退货后重新成交，原捕获者与仓库归属保留', async ({page, browser}, info) => {
    test.skip(!process.env.AVALON_PHASE_TWO_FIXTURE, '由PhaseTwoBrowserTest显式启动')
    test.setTimeout(90_000)
    const fixture = JSON.parse(readFileSync(process.env.AVALON_PHASE_TWO_FIXTURE!, 'utf8'))[info.project.name].petMarket
    const errors: string[] = []
    async function enter(target: Page, player: {username: string; password: string}) {
        target.on('pageerror', (error) => errors.push(error.message))
        target.on('console', (message) => {
            if (message.type() === 'error') errors.push(message.text())
        })
        await target.goto('/')
        await target.getByLabel('账号', {exact: true}).fill(player.username)
        await target.getByLabel('密码', {exact: true}).fill(player.password)
        await target.getByRole('button', {name: '进入冒险', exact: true}).click()
        await expect(target).toHaveURL(/\/world$/)
        await target.goto('/market')
        await target.getByLabel('我已了解交易规则').check()
        await target.getByRole('button', {name: '开始使用交易所'}).click()
    }
    const modal = (target: Page, title: string) =>
        target.locator('ion-modal').filter({has: target.locator('ion-title').getByText(title, {exact: true})})
    async function sell() {
        await page.getByRole('combobox', {name: '交易功能', exact: true}).selectOption('sell')
        await page.getByRole('combobox', {name: '商品种类', exact: true}).selectOption('CREATURE')
        await page.getByRole('combobox', {name: '伙伴', exact: true}).selectOption(fixture.petId)
        await page.getByLabel('单价（金币）').fill('100')
        await page.getByRole('button', {name: '查看上架确认'}).click()
        const confirmation = modal(page, '确认上架')
        await expect(confirmation).toContainText('税后收入 95')
        await confirmation.getByRole('button', {name: '确认上架', exact: true}).click()
        await expect(confirmation).not.toBeVisible()
    }
    await enter(page, fixture.seller)
    await sell()
    await page.getByRole('combobox', {name: '交易功能', exact: true}).selectOption('mine')
    await page.getByRole('button', {name: '查看详情', exact: true}).click()
    await modal(page, '商品详情').getByRole('button', {name: '取消挂单并转待领'}).click()
    await expect(modal(page, '商品详情')).not.toBeVisible()
    await expect(page.getByText('有退货尚未领完，暂不能新上架；仍可浏览和购买。')).toBeVisible()
    await page.getByRole('button', {name: '查看详情', exact: true}).click()
    await modal(page, '商品详情').getByRole('button', {name: '领取退货'}).click()
    await expect(modal(page, '商品详情')).not.toBeVisible()
    await sell()
    const context = await browser.newContext({
        ...devices[info.project.name === 'mobile' ? 'Pixel 7' : 'Desktop Chrome'],
        baseURL: process.env.AVALON_BASE_URL,
    })
    try {
        const buyer = await context.newPage()
        await enter(buyer, fixture.buyer)
        await buyer.getByRole('combobox', {name: '类别', exact: true}).selectOption('CREATURE')
        await buyer.getByRole('combobox', {name: '特性类别', exact: true}).selectOption('true')
        await buyer.getByLabel('已学技能（完整名称）').fill('碎岩击')
        await buyer.getByRole('button', {name: '查看详情', exact: true}).click()
        const detail = modal(buyer, '商品详情')
        await expect(detail).toContainText('隐藏特性：守护')
        await expect(detail).toContainText(`原捕获者：${fixture.seller.username}`)
        await expect(detail).toContainText('已学技能：碎岩击')
        await auditLayout(buyer, info, 'creature-market-detail')
        await buyer.screenshot({path: info.outputPath('creature-market.png')})
        await detail.getByRole('button', {name: '确认购买'}).click()
        await expect(detail).not.toBeVisible()
        await buyer.goto(`/companions/${fixture.petId}`)
        await expect(buyer.getByRole('heading', {name: '小岩怪', exact: true})).toBeVisible()
        await expect(buyer.getByText('当前位置：仓库')).toBeVisible()
        await expect(buyer.getByText('守护 · 隐藏')).toBeVisible()
        await buyer.reload()
        await expect(buyer.getByText('当前位置：仓库')).toBeVisible()
        await expect(buyer.getByText(`原捕获者：${fixture.seller.username}`, {exact: true})).toBeVisible()
        await expect(buyer.getByText('未绑定；交易仍需满足编队与锁定等条件')).toBeVisible()
    } finally {
        await context.close()
    }
    await page.goto(`/companions/${fixture.petId}`)
    await expect(page.getByRole('heading', {name: '小岩怪', exact: true})).toHaveCount(0)
    expect(errors).toEqual([])
})
