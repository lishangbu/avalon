import {expect, test, type BrowserContext, type Page} from '@playwright/test'
import type {BagView} from '../src/features/inventory/types'

test('同种采集材料合并上架且退货后恢复总量', async ({page}, info) => {
    test.skip(!process.env.AVALON_LIFE_USER, '需要隔离数据库的市场存档')
    await page.goto('/')
    await page.getByLabel('账号', {exact: true}).fill(`${process.env.AVALON_LIFE_USER}_market_${info.project.name}`)
    await page.getByLabel('密码', {exact: true}).fill('Life-browser-1234')
    await page.getByRole('button', {name: '进入冒险', exact: true}).click()
    await expect(page).toHaveURL(/\/world$/)
    await page.goto('/market')
    await page.getByLabel('我已了解交易规则').check()
    await page.getByRole('button', {name: '开始使用交易所'}).click()
    const tab = page.getByRole('combobox', {name: '交易功能', exact: true})
    await tab.selectOption('sell')
    const materials = page.getByRole('combobox', {name: '物品', exact: true})
    await expect(materials.locator('option', {hasText: '铜矿'})).toHaveCount(1)
    await materials.selectOption({label: '铜矿 ×9'})
    await page.getByLabel('数量', {exact: true}).fill('8')
    await page.getByRole('button', {name: '查看上架确认'}).click()
    const sale = page.locator('ion-modal').filter({has: page.locator('ion-title').getByText('确认上架', {exact: true})})
    const response = page.waitForResponse(
        (r) => r.url().endsWith('/api/market/items/batch') && r.request().method() === 'POST',
    )
    await sale.getByRole('button', {name: '确认上架', exact: true}).click()
    expect((await response).ok()).toBeTruthy()
    await expect(sale).not.toBeVisible()
    await tab.selectOption('mine')
    await expect(page.locator('.row')).toHaveCount(1)
    await expect(page.locator('.row')).toContainText('铜矿 ×8')
    await page.reload()
    await tab.selectOption('mine')
    await page.getByRole('button', {name: '查看详情', exact: true}).click()
    const detail = page
        .locator('ion-modal')
        .filter({has: page.locator('ion-title').getByText('商品详情', {exact: true})})
    await detail.getByRole('button', {name: '取消挂单并转待领'}).click()
    await expect(detail).not.toBeVisible()
    await page.getByRole('button', {name: '查看详情', exact: true}).click()
    await detail.getByLabel('数量', {exact: true}).fill('8')
    await detail.getByRole('button', {name: '领取退货'}).click()
    await expect(detail).not.toBeVisible()
    await tab.selectOption('sell')
    await expect(materials.locator('option', {hasText: '铜矿'})).toHaveCount(1)
    await expect(materials.locator('option', {hasText: '铜矿'})).toHaveText('铜矿 ×9')
})

test('学习名额与遗忘确认在刷新后保持一致', async ({page}, info) => {
    test.skip(!process.env.AVALON_LIFE_USER, '需要隔离数据库的6级存档')
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    await page.goto('/')
    await page.getByLabel('账号', {exact: true}).fill(`${process.env.AVALON_LIFE_USER}_${info.project.name}`)
    await page.getByLabel('密码', {exact: true}).fill('Life-browser-1234')
    await page.getByRole('button', {name: '进入冒险', exact: true}).click()
    await expect(page).toHaveURL(/\/world$/)
    await page.goto('/more')
    await page.getByText('生活技能', {exact: true}).click()
    for (const name of ['采矿', '伐木', '采药']) {
        await page.getByRole('button', {name: `学习${name}`, exact: true}).click()
        await expect(page.getByRole('button', {name: `学习${name}`, exact: true})).toHaveCount(0)
    }
    await expect(page.getByRole('button', {name: '学习狩猎', exact: true})).toBeDisabled()
    for (const name of ['种植', '钓鱼']) {
        await page.getByRole('button', {name: `学习${name}`, exact: true}).click()
        await expect(page.getByRole('button', {name: `学习${name}`, exact: true})).toHaveCount(0)
    }
    await page.reload()
    await expect(page.getByText('专业名额 3 / 3', {exact: false})).toBeVisible()
    const mining = page.locator('section.life-skill').filter({has: page.getByRole('heading', {name: '采矿 专业'})})
    await mining.getByRole('button', {name: '遗忘', exact: true}).click()
    await page.getByRole('button', {name: '取消', exact: true}).click()
    await expect(mining.getByRole('button', {name: '遗忘', exact: true})).toBeVisible()
    await mining.getByRole('button', {name: '遗忘', exact: true}).click()
    await page.getByRole('button', {name: '确认遗忘', exact: true}).click()
    await expect(page.getByRole('alertdialog')).toHaveCount(0)
    await page.getByRole('button', {name: '学习狩猎', exact: true}).click()
    await expect(page.getByRole('button', {name: '学习狩猎', exact: true})).toHaveCount(0)
    await page.reload()
    await expect(page.getByRole('button', {name: '学习采矿', exact: true})).toBeDisabled()
    expect(errors).toEqual([])
})

test('指定材料与工具制造后刷新保留产出和熟练度', async ({page}, info) => {
    test.skip(!process.env.AVALON_LIFE_USER, '需要隔离数据库的制造存档')
    await page.goto('/')
    await page.getByLabel('账号', {exact: true}).fill(`${process.env.AVALON_LIFE_USER}_craft_${info.project.name}`)
    await page.getByLabel('密码', {exact: true}).fill('Life-browser-1234')
    await page.getByRole('button', {name: '进入冒险', exact: true}).click()
    await expect(page).toHaveURL(/\/world$/)
    await page.goto('/life')
    await page.getByRole('link', {name: '生活制造', exact: true}).click()
    await page.getByLabel('制造配方').selectOption('LIFE_PART_0')
    await expect(page.getByText('下一件成功制造获得3熟练度。', {exact: false})).toBeVisible()
    await expect(page.getByText('需要木锤或更高档工具。', {exact: false})).toBeVisible()
    await page.getByLabel('制造工具', {exact: true}).selectOption({label: '木锤 × 1 · 绑定'})
    await page.getByLabel('碎石 · 未绑定 · 持有6').fill('3')
    let releaseResponse = () => {}
    const released = new Promise<void>((resolve) => {
        releaseResponse = resolve
    })
    let responseReady = () => {}
    const responseHeld = new Promise<void>((resolve) => {
        responseReady = resolve
    })
    let intercepted = false
    let craftPosts = 0
    page.on('request', (request) => {
        if (request.url().endsWith('/api/life/production') && request.method() === 'POST') craftPosts++
    })
    await page.route('**/api/life/production', async (route) => {
        if (route.request().method() !== 'POST' || intercepted) return route.continue()
        intercepted = true
        const response = await route.fetch()
        responseReady()
        await released
        await route.fulfill({response})
    })
    await page.getByRole('button', {name: '确认制造', exact: true}).click()
    await responseHeld
    try {
        const confirm = page.getByRole('button', {name: '确认制造', exact: true})
        await expect(confirm).toBeDisabled()
        await confirm.dispatchEvent('click')
        await confirm.dispatchEvent('click')
        expect(craftPosts).toBe(1)
        for (const label of [
            '制造配方',
            '制造次数',
            '制造工具',
            '允许使用的工具总数（含备用）',
            '碎石 · 未绑定 · 持有6',
            '出售物品',
            '出售数量',
        ])
            await expect(page.getByLabel(label)).toBeDisabled()
    } finally {
        releaseResponse()
    }
    await expect(page.getByText('本批制造完成。完成1次，获得1件', {exact: false})).toBeVisible()
    for (const label of ['制造配方', '制造次数', '制造工具', '允许使用的工具总数（含备用）', '出售物品', '出售数量'])
        await expect(page.getByLabel(label)).toBeEnabled()
    await expect(page.getByText('当前累计熟练度3。', {exact: false})).toBeVisible()
    expect(craftPosts).toBe(1)
    await expect(page.getByLabel('碎石 · 未绑定 · 持有3')).toBeVisible()
    await page.goto('/life')
    const artisan = page.locator('section.life-skill').filter({has: page.getByRole('heading', {name: '工匠 专业'})})
    await expect(artisan.getByText('累计熟练度 3', {exact: false})).toBeVisible()
    await page.reload()
    await expect(artisan.getByText('累计熟练度 3', {exact: false})).toBeVisible()
    await page.goto('/inventory')
    await expect(page.getByText('木级部件', {exact: false}).first()).toBeVisible()
})

test('装备制造跨堆叠选择足量材料并提示绑定', async ({page}, info) => {
    test.skip(!process.env.AVALON_LIFE_USER, '需要隔离数据库的装备制造存档')
    await page.goto('/')
    await page.getByLabel('账号', {exact: true}).fill(`${process.env.AVALON_LIFE_USER}_gear_${info.project.name}`)
    await page.getByLabel('密码', {exact: true}).fill('Life-browser-1234')
    await page.getByRole('button', {name: '进入冒险', exact: true}).click()
    await expect(page).toHaveURL(/\/world$/)
    await page.goto('/life/production')
    await page.getByLabel('制造配方').selectOption('CRAFT_3_SWORD')
    const summary = page.getByRole('list', {name: '本批材料汇总'})
    await expect(summary).toContainText('铜矿：本批需要192，已选0')
    await expect(summary).toContainText('库存192（未绑定99）')
    await page.getByLabel('制造次数').fill('2')
    await expect(summary).toContainText('铜矿：本批需要384，已选0')
    await page.getByLabel('制造次数').fill('1')
    await page.getByLabel('制造工具', {exact: true}).selectOption({label: '铜锤 × 1 · 未绑定'})
    await page.getByRole('button', {name: '填入未绑定材料', exact: true}).click()
    const unboundOre = page.getByLabel('铜矿 · 未绑定 · 持有1', {exact: true})
    const boundOre = page.getByLabel('铜矿 · 绑定 · 持有1', {exact: true})
    const wood = page.getByLabel('松木 · 未绑定 · 持有1', {exact: true})
    await expect(unboundOre).toHaveCount(99)
    await expect(boundOre).toHaveCount(93)
    await expect(wood).toHaveCount(96)
    const materialDetails = page.locator('details').filter({has: unboundOre.first()})
    await expect(materialDetails).not.toHaveAttribute('open')
    await materialDetails.locator('summary').click()
    await expect(unboundOre.first()).toBeVisible()
    await materialDetails.locator('summary').click()
    await expect(unboundOre.first()).not.toBeVisible()
    await expect
        .poll(() =>
            unboundOre.evaluateAll((elements) =>
                elements.every((element) => element instanceof HTMLInputElement && element.value === '1'),
            ),
        )
        .toBe(true)
    await expect(summary).toContainText('还差93')
    await expect
        .poll(() =>
            boundOre.evaluateAll((elements) =>
                elements.every((element) => element instanceof HTMLInputElement && element.value === ''),
            ),
        )
        .toBe(true)
    await page.getByRole('button', {name: '填入材料（允许绑定）', exact: true}).click()
    for (const inputs of [unboundOre, boundOre, wood])
        await expect
            .poll(() =>
                inputs.evaluateAll((elements) =>
                    elements.every((element) => element instanceof HTMLInputElement && element.value === '1'),
                ),
            )
            .toBe(true)
    await expect(summary).toContainText('铜矿：本批需要192，已选192')
    await expect(page.getByText('已选择绑定材料，使用这些材料制造的成品将绑定。')).toBeVisible()
    await page.getByRole('button', {name: '确认制造', exact: true}).click()
    await expect(page.getByText('本批制造完成。完成1次，获得1件', {exact: false})).toBeVisible()
    for (const inputs of [unboundOre, boundOre, wood]) await expect(inputs).toHaveCount(0)
    await page.goto('/inventory')
    await page.getByRole('button', {name: '装备', exact: true}).click()
    await expect(page.getByText('赤铜长剑', {exact: false}).first()).toBeVisible()
    await page.reload()
    await page.getByRole('button', {name: '装备', exact: true}).click()
    await expect(page.getByText('赤铜长剑', {exact: false}).first()).toBeVisible()
})

test('在线派遣实际获得材料且刷新后可以停止', async ({page}, info) => {
    test.skip(!process.env.AVALON_LIFE_USER, '需要隔离数据库的采集存档')
    test.setTimeout(45000)
    await page.goto('/')
    await page.getByLabel('账号', {exact: true}).fill(`${process.env.AVALON_LIFE_USER}_gather_${info.project.name}`)
    await page.getByLabel('密码', {exact: true}).fill('Life-browser-1234')
    await page.getByRole('button', {name: '进入冒险', exact: true}).click()
    await expect(page).toHaveURL(/\/world$/)
    await page.goto('/life/gathering')
    await page.getByLabel('资源点', {exact: true}).selectOption('MINING_1')
    await page.getByLabel('派遣伙伴', {exact: true}).selectOption({index: 1})
    await page.getByLabel('采集工具', {exact: true}).selectOption({index: 1})
    await page.locator('label').filter({hasText: '未绑定 · 持有2'}).first().locator('input').fill('1')
    await page.locator('label').filter({hasText: '未绑定 · 持有2'}).last().locator('input').fill('1')
    await page.getByRole('button', {name: '开始采集', exact: true}).click()
    await expect(page.getByText('完成 1 轮', {exact: false})).toBeVisible({timeout: 20000})
    await expect(page.getByText('剩余额度 0', {exact: false})).toHaveCount(2)
    let resumeHeartbeat = () => {}
    const resumed = new Promise<void>((resolve) => {
        resumeHeartbeat = resolve
    })
    let reportHeartbeat: (progress: number) => void = () => {}
    const heldHeartbeat = new Promise<number>((resolve) => {
        reportHeartbeat = resolve
    })
    let interceptedHeartbeat = false
    await page.route('**/api/life/gathering/heartbeat', async (route) => {
        if (interceptedHeartbeat) {
            await route.continue()
            return
        }
        interceptedHeartbeat = true
        const response = await route.fetch()
        const state: {progressMillis: number} = await response.json()
        reportHeartbeat(state.progressMillis)
        await resumed
        await route.fulfill({response})
    })
    const progressBeforePause = await heldHeartbeat
    const pausedResponse = page.waitForResponse(
        (response) =>
            response.url().endsWith('/api/life/gathering/heartbeat') &&
            response.request().postDataJSON()?.foreground === false &&
            response.ok(),
    )
    try {
        await page.evaluate(() => {
            Object.defineProperty(document, 'visibilityState', {configurable: true, value: 'hidden'})
            document.dispatchEvent(new Event('visibilitychange'))
            Object.defineProperty(document, 'visibilityState', {configurable: true, value: 'visible'})
            document.dispatchEvent(new Event('visibilitychange'))
        })
    } finally {
        resumeHeartbeat()
    }
    const pausedState: {status: string; progressMillis: number} = await (await pausedResponse).json()
    expect(pausedState.status).toBe('PAUSED')
    expect(pausedState.progressMillis).toBe(progressBeforePause)
    await page.reload()
    await expect(page.getByRole('button', {name: '结束派遣', exact: true})).toBeVisible()
    await page.getByRole('button', {name: '结束派遣', exact: true}).click()
    await expect(page.getByLabel('资源点', {exact: true})).toBeVisible()
    await page.goto('/inventory')
    await expect(page.getByText('碎石', {exact: true}).first()).toBeVisible()
})

test('农田播种浇水收获与扩建在手机和桌面持久化', async ({page}, info) => {
    test.skip(!process.env.AVALON_LIFE_USER, '需要隔离数据库的农田存档')
    await page.goto('/')
    await page.getByLabel('账号', {exact: true}).fill(`${process.env.AVALON_LIFE_USER}_farm_${info.project.name}`)
    await page.getByLabel('密码', {exact: true}).fill('Life-browser-1234')
    await page.getByRole('button', {name: '进入冒险', exact: true}).click()
    await expect(page).toHaveURL(/\/world$/)
    const bagResponse = page.waitForResponse((r) => r.url().endsWith('/api/inventory/bag') && r.ok())
    await page.goto('/inventory')
    const bag = await (await bagResponse).json()
    expect(bag.stacks.find((stack: {code: string}) => stack.code === 'LIFE_HEALTH_POTION_1').category).toBe('药品')
    await expect(page.getByText('药品 · 未绑定', {exact: true})).toBeVisible()
    await page.goto('/life')
    await page.getByRole('link', {name: '我的农田', exact: true}).click()
    await page.getByLabel('选择地块1', {exact: true}).check()
    await page.getByRole('combobox', {name: '作物', exact: true}).selectOption('CROP_0_0')
    await page.getByRole('combobox', {name: '种子', exact: true}).selectOption({index: 1})
    await page.getByRole('combobox', {name: '锄头', exact: true}).selectOption({index: 1})
    await page.getByRole('button', {name: '播种所选地块', exact: true}).click()
    await expect(page.getByText('等待浇水', {exact: true})).toHaveCount(1)
    await page.getByRole('button', {name: '选待浇水', exact: true}).click()
    await page.getByRole('button', {name: '浇水所选地块', exact: true}).click()
    await expect(page.getByText('等待浇水', {exact: true})).toHaveCount(0)
    await page.reload()
    await expect(page.getByText('约15分钟后成熟', {exact: true})).toBeVisible()
    await page.getByRole('button', {name: '选成熟作物', exact: true}).click()
    await page.getByRole('button', {name: '收获所选地块', exact: true}).click()
    await expect(page.getByText('完成1格，收获2件', {exact: false})).toBeVisible()
    await page.getByRole('button', {name: '扩建一格 · 100金币', exact: true}).click()
    await expect(page.getByLabel('选择地块7', {exact: true})).toBeVisible()
    await page.reload()
    await expect(page.getByLabel('选择地块7', {exact: true})).toBeVisible()
    await expect(page.getByText('可收获', {exact: true})).toHaveCount(0)
    await page.goto('/life')
    await page.getByRole('link', {name: '食用料理', exact: true}).click()
    await page.getByRole('combobox', {name: '料理', exact: true}).selectOption({index: 1})
    await page.getByRole('combobox', {name: '食用目标', exact: true}).selectOption({index: 1})
    await page.getByRole('button', {name: '食用一份', exact: true}).click()
    await expect(page.getByText('恢复生命120点、魔法0点。', {exact: true})).toBeVisible()
    await page.reload()
    await expect(page.getByRole('option', {name: '萝卜炖肉 × 1 · 未绑定', exact: true})).toHaveCount(1)
    await expect(page.getByRole('option', {name: /生命121\//})).toHaveCount(1)
})

test('按住松开跟随鱼区能够钓获并持久化鱼获', async ({page}, info) => {
    test.skip(!process.env.AVALON_LIFE_USER, '需要隔离数据库的钓鱼存档')
    test.setTimeout(40000)
    await page.goto('/')
    await page.getByLabel('账号', {exact: true}).fill(`${process.env.AVALON_LIFE_USER}_fish_${info.project.name}`)
    await page.getByLabel('密码', {exact: true}).fill('Life-browser-1234')
    await page.getByRole('button', {name: '进入冒险', exact: true}).click()
    await expect(page).toHaveURL(/\/world$/)
    await page.goto('/life/fishing')
    await page.getByRole('combobox', {name: '目标鱼种', exact: true}).selectOption('FISH_0_0')
    await page.getByRole('combobox', {name: '鱼竿', exact: true}).selectOption({index: 1})
    await page.getByRole('combobox', {name: '鱼饵', exact: true}).selectOption({index: 1})
    await page.getByRole('button', {name: '抛竿 · 消耗1份鱼饵', exact: true}).click()
    const control = page.getByRole('button', {name: '按住上升 · 松开下降', exact: true})
    await expect(control).toBeVisible()
    await control.scrollIntoViewIfNeeded()
    const bounds = await control.boundingBox()
    if (!bounds) throw new Error('未显示钓鱼操作按钮')
    const touch = info.project.use.hasTouch ? await page.context().newCDPSession(page) : null
    const x = bounds.x + bounds.width / 2,
        y = bounds.y + bounds.height / 2
    await control.evaluate((button) => {
        button.addEventListener('pointerdown', (event) => {
            if (event instanceof PointerEvent) button.setAttribute('data-tested-pointer', event.pointerType)
        })
    })
    if (!touch) await page.mouse.move(x, y)
    async function input(pressed: boolean) {
        if (touch)
            await touch.send('Input.dispatchTouchEvent', {
                type: pressed ? 'touchStart' : 'touchEnd',
                touchPoints: pressed ? [{x, y, id: 1}] : [],
            })
        else if (pressed) await page.mouse.down()
        else await page.mouse.up()
    }
    let held = false
    const deadline = Date.now() + 22000
    while (Date.now() < deadline && (await control.isVisible())) {
        const direction = await page
            .locator('.fishing-track')
            .evaluate((track) => {
                const zone = track.querySelector<HTMLElement>('.fish-zone'),
                    bobber = track.querySelector<HTMLElement>('.bobber')
                if (!zone || !bobber) return false
                return (
                    parseFloat(bobber.style.bottom) < parseFloat(zone.style.bottom) + parseFloat(zone.style.height) / 2
                )
            })
            .catch(() => false)
        if (direction !== held) {
            await input(direction)
            if (direction) await expect(control).toHaveAttribute('data-tested-pointer', touch ? 'touch' : 'mouse')
            held = direction
        }
        await page.waitForTimeout(100)
    }
    if (held) await input(false)
    await touch?.detach()
    await expect(page.getByText('钓获成功', {exact: false})).toBeVisible()
    await page.reload()
    await expect(page.getByText('钓获成功', {exact: false})).toBeVisible()
    await expect(page.getByRole('option', {name: '通用鱼饵 × 2 · 未绑定', exact: true})).toHaveCount(1)
    await page.goto('/inventory')
    await expect(page.getByText('溪鲫', {exact: true}).first()).toBeVisible()
    // 留住真实输入响应，模拟请求在途时快速切后台再返回，不能继续当前局。
    await page.goto('/life/fishing')
    await page.getByRole('combobox', {name: '目标鱼种', exact: true}).selectOption('FISH_0_0')
    await page.getByRole('combobox', {name: '鱼竿', exact: true}).selectOption({index: 1})
    await page.getByRole('combobox', {name: '鱼饵', exact: true}).selectOption({index: 1})
    let releaseResponse = () => {}
    const released = new Promise<void>((resolve) => {
        releaseResponse = resolve
    })
    let notifyResponse = () => {}
    const responseHeld = new Promise<void>((resolve) => {
        notifyResponse = resolve
    })
    let intercepted = false
    await page.route('**/api/life/fishing/input', async (route) => {
        if (intercepted) return route.continue()
        intercepted = true
        const response = await route.fetch()
        notifyResponse()
        await released
        await route.fulfill({response})
    })
    await page.getByRole('button', {name: '抛竿 · 消耗1份鱼饵', exact: true}).click()
    await responseHeld
    try {
        await page.evaluate(() => {
            Object.defineProperty(document, 'visibilityState', {configurable: true, value: 'hidden'})
            document.dispatchEvent(new Event('visibilitychange'))
            Object.defineProperty(document, 'visibilityState', {configurable: true, value: 'visible'})
            document.dispatchEvent(new Event('visibilitychange'))
        })
    } finally {
        releaseResponse()
    }
    await expect(page.getByText('鱼逃走了，本局结束', {exact: false})).toBeVisible({timeout: 2000})
    await page.reload()
    await expect(page.getByRole('option', {name: '通用鱼饵 × 1 · 未绑定', exact: true})).toHaveCount(1)
    await page.unroute('**/api/life/fishing/input')
    await page.getByRole('combobox', {name: '目标鱼种', exact: true}).selectOption('FISH_0_0')
    await page.getByRole('combobox', {name: '鱼竿', exact: true}).selectOption({index: 1})
    await page.getByRole('combobox', {name: '鱼饵', exact: true}).selectOption({index: 1})
    const firstInput = page.waitForRequest((r) => r.url().endsWith('/api/life/fishing/input'))
    await page.getByRole('button', {name: '抛竿 · 消耗1份鱼饵', exact: true}).click()
    expect((await firstInput).postDataJSON().held).toBe(false)
    await expect(control).toBeVisible()
    try {
        await page.context().setOffline(true)
        await expect(control).toBeDisabled()
        await expect(page.getByText('网络已断开，本局停止', {exact: false})).toBeVisible()
        await page.waitForTimeout(2800)
    } finally {
        await page.context().setOffline(false)
    }
    await expect(page.getByText('鱼逃走了，本局结束', {exact: false})).toBeVisible()
    await page.reload()
    await expect(page.getByRole('combobox', {name: '鱼饵', exact: true}).locator('option')).toHaveCount(1)
    await page.goto('/inventory')
    await expect(page.getByText('溪鲫', {exact: true}).first()).toBeVisible()
})

// 初始成长、剧情与工具由 JUnit 夹具准备；本链不注入材料、不模拟业务响应。
test('双账号实际采集交易制造，并处理金币不足、满背包和过期详情', async ({page, browser}, info) => {
    test.skip(!process.env.AVALON_LIFE_USER, '需要隔离数据库的合作存档')
    test.setTimeout(90000)
    const contexts: BrowserContext[] = []
    async function login(target: Page, role: string) {
        await target.goto('/')
        await target
            .getByLabel('账号', {exact: true})
            .fill(`${process.env.AVALON_LIFE_USER}_c_${role}_${info.project.name}`)
        await target.getByLabel('密码', {exact: true}).fill('Life-browser-1234')
        await target.getByRole('button', {name: '进入冒险', exact: true}).click()
        await expect(target).toHaveURL(/\/world$/)
    }
    async function participant(role: string) {
        const context = await browser.newContext({
            baseURL: info.project.use.baseURL,
            viewport: info.project.use.viewport,
            isMobile: info.project.use.isMobile,
            hasTouch: info.project.use.hasTouch,
            deviceScaleFactor: info.project.use.deviceScaleFactor,
            userAgent: info.project.use.userAgent,
        })
        contexts.push(context)
        const target = await context.newPage()
        await login(target, role)
        return target
    }
    async function market(target: Page) {
        await target.goto('/market')
        await target.getByLabel('我已了解交易规则').check()
        await target.getByRole('button', {name: '开始使用交易所'}).click()
        await expect(target.getByRole('combobox', {name: '交易功能', exact: true})).toBeVisible()
    }
    async function move(target: Page, names: string[]) {
        await target.goto('/world')
        for (const name of names) {
            await target.getByRole('button', {name, exact: true}).click()
            await expect(target.getByRole('button', {name: `${name}，当前位置`, exact: true})).toBeVisible()
        }
    }
    const detail = (target: Page) =>
        target.locator('ion-modal').filter({
            has: target.locator('ion-title').getByText('商品详情', {exact: true}),
        })
    async function snapshot(target: Page) {
        const world = target.waitForResponse((r) => r.url().endsWith('/api/world') && r.ok())
        await target.goto('/world')
        const {character} = await (await world).json()
        const inventory = target.waitForResponse((r) => r.url().endsWith('/api/inventory/bag') && r.ok())
        await target.goto('/inventory')
        const bag: BagView = await (await inventory).json()
        const quantity = (code: string) =>
            bag.stacks.filter((item) => item.code === code).reduce((sum, item) => sum + item.quantity, 0)
        return {gold: BigInt(character.gold), bag, quantity}
    }
    try {
        await login(page, 'miner')
        await page.goto('/life/gathering')
        await page.getByLabel('资源点', {exact: true}).selectOption('MINING_2')
        await page.getByLabel('派遣伙伴', {exact: true}).selectOption({index: 1})
        await page.getByLabel('采集工具', {exact: true}).selectOption({label: '木镐 × 10'})
        await page.getByLabel('使用工具总数（含备用）').fill('10')
        await page.getByRole('button', {name: '开始采集', exact: true}).click()
        await expect(page.getByText('完成 2 轮', {exact: false})).toBeVisible({timeout: 30000})
        await page.getByRole('button', {name: '结束派遣', exact: true}).click()
        const mined = await snapshot(page)
        expect(mined.quantity('LIFE_ORE_0')).toBe(4)
        await move(page, ['诺姆旧径', '诺姆城外围', '诺姆城', '诺姆城商店'])
        await market(page)
        await page.getByRole('combobox', {name: '交易功能', exact: true}).selectOption('sell')
        await page.getByRole('combobox', {name: '物品', exact: true}).selectOption({label: '碎石 ×4'})
        await page.getByLabel('数量', {exact: true}).fill('3')
        await page.getByLabel('单价（金币）').fill('2')
        await page.getByRole('button', {name: '查看上架确认'}).click()
        const sale = page
            .locator('ion-modal')
            .filter({has: page.locator('ion-title').getByText('确认上架', {exact: true})})
        await sale.getByRole('button', {name: '确认上架', exact: true}).click()
        await expect(sale).not.toBeVisible()

        const buyer = await participant('buyer'),
            poor = await participant('poor'),
            full = await participant('full')
        const beforeBuyer = await snapshot(buyer),
            beforePoor = await snapshot(poor),
            beforeFull = await snapshot(full)
        expect(beforeFull.bag.usedSlots).toBe(beforeFull.bag.capacity)
        for (const target of [buyer, poor, full]) {
            await market(target)
            await target.locator('.row').filter({hasText: '碎石 ×3'}).getByRole('button', {name: '查看详情'}).click()
            await detail(target).getByLabel('数量', {exact: true}).fill('3')
        }
        await detail(poor).getByRole('button', {name: '确认购买', exact: true}).click()
        await expect(detail(poor)).toContainText('金币不足')
        await detail(full).getByRole('button', {name: '确认购买', exact: true}).click()
        await expect(detail(full).getByRole('alert')).toContainText('背包')
        await expect(detail(full).getByRole('alert')).toContainText(/不足|已满/)
        // 失败购买不改变挂单；另一个账号仍可买走全部三份。
        await detail(buyer).getByRole('button', {name: '确认购买', exact: true}).click()
        await expect(detail(buyer)).not.toBeVisible()
        // 保留旧详情的玩家再购买，应看到结束提示，且不能继续向旧挂单提交。
        await detail(poor).getByRole('button', {name: '确认购买', exact: true}).click()
        await expect(detail(poor)).toContainText('挂单已结束')
        await expect(detail(poor).getByRole('button', {name: '确认购买', exact: true})).toHaveCount(0)
        const afterPoor = await snapshot(poor),
            afterFull = await snapshot(full)
        expect(afterPoor.gold).toBe(beforePoor.gold)
        expect(afterPoor.bag).toEqual(beforePoor.bag)
        expect(afterFull.gold).toBe(beforeFull.gold)
        expect(afterFull.bag).toEqual(beforeFull.bag)
        const purchased = await snapshot(buyer),
            sold = await snapshot(page)
        expect(purchased.gold).toBe(beforeBuyer.gold - 6n)
        expect(purchased.quantity('LIFE_ORE_0')).toBe(3)
        expect(sold.gold).toBe(mined.gold + 5n)
        expect(sold.quantity('LIFE_ORE_0')).toBe(1)
        await move(buyer, ['诺姆城', '诺姆锻造坊'])
        await buyer.goto('/life/production')
        await buyer.getByLabel('制造配方').selectOption('LIFE_PART_0')
        await buyer.getByLabel('制造工具', {exact: true}).selectOption({label: '木锤 × 1 · 绑定'})
        await buyer.getByRole('button', {name: '填入未绑定材料', exact: true}).click()
        await buyer.getByRole('button', {name: '确认制造', exact: true}).click()
        await expect(buyer.getByText('本批制造完成。完成1次，获得1件', {exact: false})).toBeVisible()
        const made = await snapshot(buyer)
        expect(made.quantity('LIFE_ORE_0')).toBe(0)
        expect(made.quantity('LIFE_PART_0')).toBe(1)
        expect(made.gold).toBe(purchased.gold)
        await buyer.reload()
        await expect(buyer.getByText('木级部件', {exact: true})).toBeVisible()
        await info.attach('cooperation-accounting', {
            body: JSON.stringify({
                gathered: 4,
                sold: 3,
                sellerRemaining: 1,
                purchaseCost: 6,
                sellerNet: 5,
                tax: 1,
                consumed: 3,
                produced: 1,
                recipe: 'LIFE_PART_0',
            }),
            contentType: 'application/json',
        })
    } finally {
        for (const context of contexts) await context.close()
    }
})
