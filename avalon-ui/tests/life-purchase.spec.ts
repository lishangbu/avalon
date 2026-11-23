import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {execFileSync} from 'node:child_process'

test('生活用品批量采购与按随身库存补齐，目标满足后不再扣款', async ({page, baseURL}, info) => {
    test.skip(process.env.AVALON_LOCAL_FIXTURES !== '1', '需要独立数据库及显式夹具开关')
    const suffix = randomUUID().replaceAll('-', '').slice(0, 12)
    const registered = await page.request.post(`${baseURL}/api/auth/register`, {
        data: {username: `buy_${suffix}`, password: 'Purchase-browser-1234', device: '采购验收'},
    })
    expect(registered.ok()).toBe(true)
    const tokens = await registered.json()
    const created = await page.request.post(`${baseURL}/api/world/character`, {
        headers: {Authorization: `Bearer ${tokens.accessToken}`},
        data: {name: `采购${suffix}`, profession: 'WARRIOR'},
    })
    expect(created.ok()).toBe(true)
    const character = await created.json()
    expect(String(character.id)).toMatch(/^\d+$/)
    // 只准备商店位置和资金；实际库存、采购、扣款及幂等全部使用正式服务。
    execFileSync(
        'docker',
        [
            'exec',
            '-i',
            process.env.AVALON_TEST_DB_CONTAINER!,
            'psql',
            '-U',
            process.env.AVALON_TEST_DB_USER!,
            '-d',
            process.env.AVALON_TEST_DB_NAME!,
            '-v',
            'ON_ERROR_STOP=1',
        ],
        {
            input: `UPDATE player_character SET location='SUPPLY_SHOP',gold=1000 WHERE id=${character.id};`,
        },
    )
    await page.context().addInitScript((refresh) => {
        if (!localStorage.getItem('avalon.refresh-token')) localStorage.setItem('avalon.refresh-token', refresh)
    }, tokens.refreshToken)
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    const purchases: {item: string; quantity: number}[] = []
    let authorization = ''
    page.on('request', (request) => {
        if (request.headers().authorization?.startsWith('Bearer ')) authorization = request.headers().authorization!
        if (request.method() === 'POST' && new URL(request.url()).pathname === '/api/life/production/buy')
            purchases.push(request.postDataJSON())
    })
    await page.goto('/life/production')
    const shopFilter = page.getByLabel('仅显示当前地点出售的商品')
    await expect(shopFilter).toBeChecked()
    await expect(page.getByRole('region', {name: '金针采购', exact: true})).toHaveCount(0)
    await shopFilter.uncheck()
    const remoteTool = page.getByRole('region', {name: '金针采购', exact: true})
    await expect(remoteTool).toContainText('出售地点：')
    await expect(remoteTool).toContainText('需前往出售地点')
    await expect(remoteTool.getByRole('button')).toBeDisabled()
    await shopFilter.check()
    const offer = page.getByRole('region', {name: '木针采购'})
    await expect(offer).toBeVisible()
    const quantity = offer.getByLabel('购买数量', {exact: true})
    await quantity.fill('2')
    await page.getByLabel('商品搜索', {exact: true}).fill('不存在的商品')
    await expect(offer).toHaveCount(0)
    await expect(page.getByText('当前筛选没有商品。可查看全部商品的出售地点，到达对应地点后采购。')).toBeVisible()
    await page.getByLabel('商品搜索', {exact: true}).fill('木针')
    await expect(offer).toBeVisible()
    await expect(quantity).toHaveValue('2')
    await page.getByLabel('商品搜索', {exact: true}).fill('')
    await quantity.fill('1.5')
    await expect(offer.getByRole('button')).toBeDisabled()
    await quantity.fill('100')
    await expect(offer.getByRole('button')).toBeDisabled()
    expect(purchases).toHaveLength(0)
    await quantity.fill('2')
    await expect(offer).toContainText('本次购买2件，合计40金币')
    await offer.getByRole('button', {name: '购买木针 · 40金币'}).click()
    await expect(offer).toContainText('随身库存2件')
    await page.getByLabel('采购方式').selectOption('target')
    await offer.getByLabel('目标库存', {exact: true}).fill('5')
    await expect(offer).toContainText('本次购买3件，合计60金币')
    await offer.getByRole('button', {name: '补齐木针 · 60金币'}).click()
    await expect(offer).toContainText('随身库存5件')
    await expect(offer).toContainText('库存已达到目标，无需购买')
    await expect(offer.getByRole('button')).toBeDisabled()
    expect(purchases.map(({item, quantity}) => ({item, quantity}))).toEqual([
        {item: 'TOOL_WOOD_NEEDLE', quantity: 2},
        {item: 'TOOL_WOOD_NEEDLE', quantity: 3},
    ])
    // 通过页面的新会话身份读取真实结果，避免拿已轮换的注册访问令牌断言。
    expect(authorization).toMatch(/^Bearer /)
    const world = await page.request.get(`${baseURL}/api/world`, {headers: {Authorization: authorization}})
    expect(world.ok()).toBe(true)
    expect((await world.json()).character.gold).toBe(900)
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
    expect(errors).toEqual([])
    await page.screenshot({path: info.outputPath('life-purchase.png')})
})
