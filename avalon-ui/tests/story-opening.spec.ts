import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {clickWorldAction, closeNpc} from './support/npc'

test.use({actionTimeout: 15_000})

test('新故事从登记到领取伙伴与人物行装', async ({page}) => {
    test.setTimeout(180_000)
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    const suffix = randomUUID().replaceAll('-', '').slice(0, 10)
    await page.goto('/')
    await clickWorldAction(page, '创建账号')
    await page.getByLabel('账号', {exact: true}).fill(`rewrite_${suffix}`)
    await page.getByLabel('密码', {exact: true}).fill('Rewrite-opening-1234')
    await page.getByRole('button', {name: '注册并进入'}).click()
    await page.getByLabel('角色名', {exact: true}).fill(`远行${suffix}`)
    await page.getByRole('button', {name: '踏上旅途'}).click()
    const current = () => page.locator('div.ion-page').filter({visible: true})
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
        await expect(sheet).toBeVisible()
        await sheet.getByRole('button', {name: '继续', exact: true}).click()
        await expect(sheet).toHaveCount(0)
        await closeNpc(page)
    }
    await expect(page.getByRole('button', {name: '晨露村家门，当前位置', exact: true})).toBeVisible()
    await move('石板路')
    await move('村中广场')
    await talk('接取任务')
    await move('幻兽中心')
    await talk('登记远行准备')
    await talk('接取任务')
    await talk('领取利爪猫')
    await clickWorldAction(page, '伙伴')
    await current().locator('.pet-link').filter({hasText: '利爪猫'}).click()
    await current().getByRole('combobox', {name: '目标位置', exact: true}).selectOption('0')
    await current().getByRole('button', {name: '调整位置', exact: true}).click()
    await current().getByRole('button', {name: '返回', exact: true}).click()
    await expect(page).toHaveURL(/\/companions$/)
    await clickWorldAction(page, '冒险')
    await talk('确认伙伴出战安排')
    await move('村中广场')
    await move('武器店')
    await talk('接取任务')
    await talk('领取职业基础装备与补给')
    await clickWorldAction(page, '背包')
    await clickWorldAction(page, '装备')
    for (let i = 0; i < 3; i++) {
        await current().locator('.equipment-row').first().click()
        const detail = page.locator('ion-modal').filter({visible: true})
        await detail.getByRole('button', {name: '穿戴', exact: true}).click()
        await expect(detail).toHaveCount(0)
    }
    await clickWorldAction(page, '冒险')
    await talk('确认实际穿戴行装')
    await page.reload()
    await expect(page.getByRole('button', {name: '武器店，当前位置', exact: true})).toBeVisible()
    expect(errors).toEqual([])
})
