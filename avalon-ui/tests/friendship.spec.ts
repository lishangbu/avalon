import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {auditLayout} from './support/layout-audit'

/** 两个独立浏览器身份通过真实接口建立和解除好友，不用本地数组替代服务端关系。 */
test('好友申请接受、删除确认、拒绝与撤回可在刷新后恢复', async ({page, browser}, info) => {
    test.setTimeout(90_000)
    const base = info.project.use.baseURL ?? 'http://localhost:8080'
    const suffix = randomUUID().replaceAll('-', '').slice(0, 8)
    const peers: {name: string; refreshToken: string}[] = []
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    for (let index = 0; index < 2; index++) {
        const name = `好友${suffix}${index}`
        const registration = await page.request.post(`${base}/api/auth/register`, {
            // 数字隔开测试前缀和十六进制随机串，避免拼接处随机命中严格英文短词。
            data: {username: `friends_0${suffix}_${index}`, password: 'Friends-browser-123', device: '测试'},
        })
        expect(registration.ok()).toBe(true)
        const tokens = await registration.json()
        const character = await page.request.post(`${base}/api/world/character`, {
            headers: {Authorization: `Bearer ${tokens.accessToken}`},
            data: {name, profession: 'WARRIOR'},
        })
        expect(character.ok()).toBe(true)
        peers.push({name, refreshToken: tokens.refreshToken})
    }
    await page.context().addInitScript((token) => {
        if (!localStorage.getItem('avalon.refresh-token')) localStorage.setItem('avalon.refresh-token', token)
    }, peers[0]!.refreshToken)
    const context = await browser.newContext({viewport: info.project.use.viewport})
    try {
        await context.addInitScript((token) => {
            if (!localStorage.getItem('avalon.refresh-token')) localStorage.setItem('avalon.refresh-token', token)
        }, peers[1]!.refreshToken)
        const other = await context.newPage()
        other.on('pageerror', (error) => errors.push(error.message))
        await page.goto(`${base}/more`)
        await page.getByText('好友', {exact: true}).click()
        await expect(page).toHaveURL(`${base}/friends`)
        await page.getByLabel('完整角色名', {exact: true}).fill(peers[1]!.name)
        await page.getByRole('button', {name: '添加好友', exact: true}).click()
        await expect(page.getByRole('region', {name: '发出的申请'}).getByText(peers[1]!.name)).toBeVisible()
        await other.goto(`${base}/friends`)
        const incoming = other.getByRole('region', {name: '收到的申请'})
        await expect(incoming.getByText(peers[0]!.name)).toBeVisible()
        await incoming.getByRole('button', {name: '接受', exact: true}).click()
        await expect(other.getByRole('region', {name: '好友', exact: true}).getByText(peers[0]!.name)).toBeVisible()
        await page.reload()
        const friends = page.getByRole('region', {name: '好友', exact: true})
        await expect(friends.getByText(peers[1]!.name)).toBeVisible()
        await auditLayout(page, info, 'friends')
        await page.screenshot({path: info.outputPath('friends.png'), fullPage: true})
        await friends.getByRole('button', {name: '删除好友', exact: true}).click()
        await expect(page.getByRole('dialog', {name: '删除好友', exact: true})).toBeVisible()
        // dialog 位于 Ionic 的 shadow DOM，正文通过 slot 投影；内容定位从宿主进入。
        const dialog = page
            .locator('ion-modal')
            .filter({has: page.locator('ion-title').getByText('删除好友', {exact: true})})
        await expect(dialog.getByText(`确认与 ${peers[1]!.name} 解除好友关系？`)).toBeVisible()
        await dialog.getByRole('button', {name: '取消', exact: true}).click()
        await expect(dialog).toBeHidden()
        await expect(friends.getByText(peers[1]!.name)).toBeVisible()
        await friends.getByRole('button', {name: '删除好友', exact: true}).click()
        await dialog.getByRole('button', {name: '确认删除', exact: true}).click()
        await expect(dialog).toBeHidden()
        await expect(friends.getByText(peers[1]!.name)).toHaveCount(0)
        await other.reload()
        await expect(other.getByRole('region', {name: '好友', exact: true}).getByText(peers[0]!.name)).toHaveCount(0)
        await page.getByLabel('完整角色名', {exact: true}).fill(peers[1]!.name)
        await page.getByRole('button', {name: '添加好友', exact: true}).click()
        await expect(page.getByRole('button', {name: '撤回', exact: true})).toBeVisible()
        await other.reload()
        await incoming.getByRole('button', {name: '拒绝', exact: true}).click()
        await expect(incoming.getByText(peers[0]!.name)).toHaveCount(0)
        await page.reload()
        await expect(page.getByRole('button', {name: '撤回', exact: true})).toHaveCount(0)
        await page.getByLabel('完整角色名', {exact: true}).fill(peers[1]!.name)
        await page.getByRole('button', {name: '添加好友', exact: true}).click()
        await page.getByRole('button', {name: '撤回', exact: true}).click()
        await page.reload()
        await expect(page.getByRole('button', {name: '撤回', exact: true})).toHaveCount(0)
        expect(errors).toEqual([])
    } finally {
        await context.close()
    }
})
