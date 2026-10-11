import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'

/** 真实页面验证拒绝后的输入保留与修正重试；不复制词表，也不模拟服务端响应。 */
test('注册与创角拒绝违禁文字后保留输入并允许修正', async ({page}) => {
    // 十六进制后缀避免随机账号本身意外命中严格英文短词；密码包含违禁词证明它不参与检测。
    const suffix = randomUUID().replaceAll('-', '').slice(0, 8)
    const password = 'admin-Fuck-password-123'
    const username = `text_${suffix}`
    await page.goto('/')
    await page.getByRole('button', {name: '创建账号', exact: true}).click()
    const accountInput = page.getByLabel('账号', {exact: true})
    const passwordInput = page.getByLabel('密码', {exact: true})
    await accountInput.fill(`admin_${suffix}`)
    await passwordInput.fill(password)
    await page.getByRole('button', {name: '注册并进入', exact: true}).click()
    await expect(page.getByText('账号包含不允许使用的内容，请修改后重试', {exact: true})).toBeVisible()
    await expect(accountInput).toHaveValue(`admin_${suffix}`)
    await expect(passwordInput).toHaveValue(password)
    await accountInput.fill(username)
    await page.getByRole('button', {name: '注册并进入', exact: true}).click()

    // 角色名格式合法、包含被禁止的官方称呼；拒绝后名称与职业选择都应保留。
    const roleInput = page.getByLabel('角色名', {exact: true})
    // 原生select的包裹标签含选项文字；按可访问的组合框名称定位，避免标签全文匹配失效。
    const profession = page.getByRole('combobox', {name: '职业', exact: true})
    await roleInput.fill('官_方_旅人')
    await profession.selectOption('MAGE')
    await page.getByRole('button', {name: '踏上旅途', exact: true}).click()
    await expect(page.getByText('角色名包含不允许使用的内容，请修改后重试', {exact: true})).toBeVisible()
    await expect(roleInput).toHaveValue('官_方_旅人')
    await expect(profession).toHaveValue('MAGE')
    await roleInput.fill(`旅人${suffix}`)
    await page.getByRole('button', {name: '踏上旅途', exact: true}).click()
    await expect(page.getByRole('button', {name: '伙伴', exact: true})).toBeVisible()
    await page.reload()
    await expect(page.getByRole('button', {name: '伙伴', exact: true})).toBeVisible()
})
