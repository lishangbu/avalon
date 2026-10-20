import {expect, test} from '@playwright/test'
import {clickWorldAction, closeNpc} from './support/npc'

test.use({actionTimeout: 15_000})

/** 隔离夹具提供前置与材料；改选、刷新和现场交付均经过真实页面及服务器。 */
test('矿道方法可改选，材料交付后汇合并保留完成记录', async ({page}, info) => {
    test.skip(!process.env.AVALON_STORY_BRANCH_USER, '需要故事浏览器隔离入口')
    test.setTimeout(120_000)
    await page.goto('/')
    await page.getByLabel('账号', {exact: true}).fill(`${process.env.AVALON_STORY_BRANCH_USER}_${info.project.name}`)
    await page.getByLabel('密码', {exact: true}).fill('Story-branch-1234')
    await page.getByRole('button', {name: '进入冒险', exact: true}).click()
    await expect(page).toHaveURL(/\/world$/)
    await page.goto('/quests')
    const quest = () =>
        page.locator('article').filter({has: page.getByRole('heading', {name: '旧路与旁路', exact: true})})
    for (const title of ['清除旧路守卫', '修复矿道旁路']) {
        await quest()
            .getByRole('button', {name: `${title}（可切换）`, exact: true})
            .click()
        await page.getByRole('button', {name: '确认方法', exact: true}).click()
        await expect(quest().getByRole('button', {name: `${title}（当前路线）`, exact: true})).toBeDisabled()
    }
    await page.reload()
    await expect(quest().getByRole('button', {name: '修复矿道旁路（当前路线）', exact: true})).toBeDisabled()
    await page.goto('/world')
    await clickWorldAction(page, '确认所选通路方案')
    const dialog = page
        .locator('ion-modal')
        .filter({visible: true})
        .filter({has: page.locator('ion-title').filter({hasText: /^对话$/})})
    await dialog.getByRole('button', {name: '继续', exact: true}).click()
    await expect(dialog).toHaveCount(0)
    await closeNpc(page)
    await page.goto('/quests')
    await page.locator('ion-segment-button').filter({hasText: '已完成'}).click()
    await expect(quest()).toBeVisible()
    await page.reload()
    await page.locator('ion-segment-button').filter({hasText: '已完成'}).click()
    await expect(quest()).toBeVisible()
    await expect(quest().getByText('已完成', {exact: true}).first()).toBeVisible()
})
