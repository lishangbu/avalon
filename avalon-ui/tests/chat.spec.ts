import {expect, test} from '@playwright/test'
import {randomUUID} from 'node:crypto'
import {execFileSync} from 'node:child_process'

/**
 * 两个独立浏览器身份连接真实SSE和PostgreSQL，验证私聊、重连、已读、设置与账号隔离。
 * 夹具只修改战斗门禁/频率时间，聊天消息全部经过正式认证API或页面，不插入假消息。
 * 桌面/手机复用本用例，直接检查视口溢出；不把手机视口当成真实软键盘验收。
 */
test('聊天实时私聊、重连补齐、世界纯文本、战斗隐藏与换号隔离', async ({page, browser, baseURL}, info) => {
    test.skip(process.env.AVALON_LOCAL_FIXTURES !== '1', '需要显式启用隔离数据库夹具')
    test.setTimeout(90_000)
    if (info.project.name === 'desktop') await page.setViewportSize({width: 1440, height: 1000})
    const base = baseURL!
    const suffix = randomUUID().replaceAll('-', '').slice(0, 8)
    const players: {name: string; id: string; refresh: string; access: string}[] = []
    for (let index = 0; index < 3; index++) {
        const name = `聊友0${suffix}${index}`
        const registered = await page.request.post(`${base}/api/auth/register`, {
            data: {username: `chat_0${suffix}_${index}`, password: 'Chat-browser-1234', device: '聊天验收'},
        })
        expect(registered.ok()).toBe(true)
        const tokens = await registered.json()
        const created = await page.request.post(`${base}/api/world/character`, {
            headers: {Authorization: `Bearer ${tokens.accessToken}`},
            data: {name, profession: 'WARRIOR'},
        })
        expect(created.ok()).toBe(true)
        const character = await created.json()
        // 浏览器启动会轮换注册会话的访问令牌；API探针使用独立登录会话，不能继续拿旧注册令牌发请求。
        const probe = await page.request.post(`${base}/api/auth/login`, {
            data: {username: `chat_0${suffix}_${index}`, password: 'Chat-browser-1234', device: '聊天API验收'},
        })
        expect(probe.ok()).toBe(true)
        const probeTokens = await probe.json()
        players.push({name, id: character.id, refresh: tokens.refreshToken, access: probeTokens.accessToken})
    }
    const a = players[0]!,
        b = players[1]!,
        c = players[2]!
    // 快捷入口使用真实已接受关系；申请和接受仍由双方各自认证，不能用数据库伪造好友。
    const friendship = await page.request.post(`${base}/api/friends`, {
        headers: {Authorization: `Bearer ${a.access}`},
        data: {name: b.name},
    })
    expect(friendship.ok()).toBe(true)
    const friendId = (await friendship.json()).id
    const acceptedFriend = await page.request.post(`${base}/api/friends/${friendId}/accept`, {
        headers: {Authorization: `Bearer ${b.access}`},
    })
    expect(acceptedFriend.ok()).toBe(true)
    // 第三人通过正式移动、接取和交付完成首个任务，生成真实个人通知；没有直接插入系统消息。
    let cWorld = await (
        await page.request.get(`${base}/api/world`, {headers: {Authorization: `Bearer ${c.access}`}})
    ).json()
    for (const destination of ['STONE_PATH', 'SQUARE']) {
        const moved = await page.request.post(`${base}/api/world/move`, {
            headers: {Authorization: `Bearer ${c.access}`},
            data: {destination, expectedRevision: cWorld.character.revision},
        })
        expect(moved.ok()).toBe(true)
        cWorld = {character: await moved.json()}
    }
    const accepted = await page.request.post(`${base}/api/quests/O001/accept`, {
        headers: {Authorization: `Bearer ${c.access}`},
        data: {requestId: randomUUID(), expectedRevision: cWorld.character.revision},
    })
    expect(accepted.ok()).toBe(true)
    const questState = await (
        await page.request.get(`${base}/api/quests`, {headers: {Authorization: `Bearer ${c.access}`}})
    ).json()
    const current = questState.available.find((step: {id: string}) => step.id === 'O001')
    expect(current?.instance).toBeTruthy()
    const completed = await page.request.post(`${base}/api/quests/instances/commands`, {
        headers: {Authorization: `Bearer ${c.access}`},
        data: {
            requestId: randomUUID(),
            instance: current.instance,
            round: current.round,
            operation: 'DELIVER',
            step: 'O001',
        },
    })
    expect(completed.ok()).toBe(true)
    // SQL只使用测试生成的十六进制后缀和服务端数字ID，参数列表执行，不经过shell拼接。
    const fixture = (sql: string) =>
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
            {input: sql},
        )
    const resetRate = (id: string) =>
        fixture(
            `UPDATE chat_settings SET last_sent_at=clock_timestamp()-interval '3 seconds' WHERE character_id=${id};`,
        )
    await page.context().addInitScript((token) => {
        if (!localStorage.getItem('avalon.refresh-token')) localStorage.setItem('avalon.refresh-token', token)
    }, a.refresh)
    const otherContext = await browser.newContext({viewport: info.project.use.viewport})
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    try {
        await otherContext.addInitScript((token) => {
            if (!localStorage.getItem('avalon.refresh-token')) localStorage.setItem('avalon.refresh-token', token)
        }, b.refresh)
        const other = await otherContext.newPage()
        other.on('pageerror', (error) => errors.push(error.message))
        await page.goto(`${base}/friends`)
        await other.goto(`${base}/world`)
        const panel = page.getByRole('complementary', {name: '游戏聊天'})
        const recipient = other.getByRole('complementary', {name: '游戏聊天'})
        const friendList = page.getByRole('region', {name: '好友', exact: true})
        await friendList.getByRole('button', {name: '私聊', exact: true}).click()
        await expect(panel.getByText(`私聊 · ${b.name}`, {exact: true})).toBeVisible()
        await expect(panel.getByLabel('聊天内容')).toBeFocused()
        await expect(panel.getByRole('log')).toContainText('暂无消息')
        // 重开同一好友不会清掉草稿；即使原来停在设置，也应回到可输入的私聊正文。
        await panel.getByLabel('聊天内容').fill('快捷入口保留的草稿')
        await panel.getByRole('button', {name: '设置', exact: true}).click()
        await panel.getByRole('button', {name: '关闭聊天', exact: true}).click()
        await friendList.getByRole('button', {name: '私聊', exact: true}).click()
        await expect(panel.getByRole('region', {name: '聊天设置'})).toHaveCount(0)
        await expect(panel.getByLabel('聊天内容')).toHaveValue('快捷入口保留的草稿')
        await expect(panel.getByLabel('聊天内容')).toBeFocused()
        await page.goto(`${base}/world`)
        // 完整回归可能已经产生全服公告；用实际系统未读数作为基线，不假定新号的总未读必为0。
        const initial = await (
            await other.request.get(`${base}/api/chat`, {headers: {Authorization: `Bearer ${b.access}`}})
        ).json()
        const expectedUnread =
            initial.systemUnread +
            initial.conversations.reduce((sum: number, item: {unread: number}) => sum + item.unread, 0) +
            1
        const first = `离线也保留的留言${suffix}`
        await panel.getByLabel('聊天内容').fill(first)
        await panel.getByRole('button', {name: '发送', exact: true}).click()
        await expect(panel.getByRole('log')).toContainText(first)
        await expect(recipient.locator('.chat-toggle span')).toHaveText(String(expectedUnread))
        await recipient.getByRole('button', {name: /^聊天/}).click()
        await recipient.getByRole('button', {name: '私聊', exact: true}).click()
        await recipient
            .getByRole('navigation', {name: '私聊会话'})
            .getByRole('button', {name: new RegExp(a.name)})
            .click()
        await expect(recipient.getByRole('log')).toContainText(first)
        await expect(panel.getByRole('log').getByText('已读', {exact: true})).toBeVisible()
        // 宽屏停靠游戏右侧，窄屏覆盖；两种形态都必须完整落在可视视口，不能继续套用旧容器边界。
        const panelBounds = await panel.locator('.chat-content').boundingBox()
        const viewport = page.viewportSize()!
        expect(panelBounds!.x).toBeGreaterThanOrEqual(0)
        expect(panelBounds!.x + panelBounds!.width).toBeLessThanOrEqual(viewport.width + 1)
        if (viewport.width >= 1380) {
            const appBounds = await page.locator('ion-app').boundingBox()
            expect(panelBounds!.x).toBeGreaterThanOrEqual(appBounds!.x + appBounds!.width)
            await page.setViewportSize({width: 1280, height: 1000})
            const overlay = await panel.locator('.chat-content').boundingBox()
            const fixedGame = await page.locator('ion-app').boundingBox()
            expect(fixedGame!.width).toBe(480)
            expect(overlay!.x).toBeLessThan(fixedGame!.x + fixedGame!.width)
            expect(overlay!.x + overlay!.width).toBeLessThanOrEqual(1280)
            await page.setViewportSize(viewport)
        }
        await expect(panel.getByRole('button', {name: '发送', exact: true})).toBeInViewport({ratio: 1})
        // 保存实际桌面/手机面板供布局人工复核，不依赖DOM类型检查推断可读性。
        await page.screenshot({path: info.outputPath('chat-private.png')})
        await expect(recipient.getByRole('button', {name: '关闭聊天', exact: true})).toBeVisible()
        // 真实断网期间B仍发送；A恢复连接后无需手动刷新即补齐，保留未发送的草稿。
        await panel.getByLabel('聊天内容').fill('重连后仍在的草稿')
        await page.context().setOffline(true)
        // 真正的断网发送必须保留草稿，并且不能提前插入一条“已发送”气泡。
        await panel.getByRole('button', {name: '发送', exact: true}).click()
        await expect(panel.getByRole('alert')).toBeVisible()
        await expect(panel.getByLabel('聊天内容')).toHaveValue('重连后仍在的草稿')
        await expect(panel.getByRole('log')).not.toContainText('重连后仍在的草稿')
        const missed = `断网期间消息${suffix}`
        const offline = await other.request.post(`${base}/api/chat/messages`, {
            headers: {Authorization: `Bearer ${b.access}`},
            data: {requestId: randomUUID(), channel: 'PRIVATE', peer: a.name, text: missed},
        })
        expect(offline.ok()).toBe(true)
        // 累计55条模拟长断线，覆盖跨越单页50条的补齐；只有频率时间由夹具推进，消息仍走正式API。
        for (let index = 0; index < 54; index++) {
            fixture(
                `UPDATE chat_settings SET last_sent_at=clock_timestamp()-interval '3 seconds' WHERE character_id=${b.id}; UPDATE chat_message SET created_at=clock_timestamp()-interval '2 minutes' WHERE sender=${b.id};`,
            )
            const queued = await other.request.post(`${base}/api/chat/messages`, {
                headers: {Authorization: `Bearer ${b.access}`},
                data: {requestId: randomUUID(), channel: 'PRIVATE', peer: a.name, text: `补齐${suffix}第${index}条`},
            })
            expect(queued.ok()).toBe(true)
        }
        await page.context().setOffline(false)
        await expect(panel.getByRole('log')).toContainText(`补齐${suffix}第53条`, {timeout: 20_000})
        for (let index = 0; index < 54; index++)
            await expect(panel.getByRole('log')).toContainText(`补齐${suffix}第${index}条`)
        await expect(panel.getByRole('log')).toContainText(missed, {timeout: 20_000})
        await expect(panel.getByLabel('聊天内容')).toHaveValue('重连后仍在的草稿')
        await panel.getByRole('button', {name: '重试连接', exact: true}).click()
        await expect(panel.getByRole('alert')).toHaveCount(0)
        // 正在阅读历史时，SSE 到达只能提示新增消息，不能抢走滚动位置或提前标记最新消息已读。
        const history = panel.getByRole('log')
        await history.evaluate((element) => {
            element.scrollTop = 80
            element.dispatchEvent(new Event('scroll'))
        })
        const readingTop = await history.evaluate((element) => element.scrollTop)
        await panel.getByRole('button', {name: '设置', exact: true}).click()
        await panel.getByRole('button', {name: '返回聊天', exact: true}).click()
        await expect.poll(() => history.evaluate((element) => element.scrollTop)).toBeCloseTo(readingTop, 0)
        resetRate(b.id)
        const arriving = `阅读历史时的新消息${suffix}`
        const arrival = await other.request.post(`${base}/api/chat/messages`, {
            headers: {Authorization: `Bearer ${b.access}`},
            data: {requestId: randomUUID(), channel: 'PRIVATE', peer: a.name, text: arriving},
        })
        expect(arrival.ok()).toBe(true)
        await expect(panel.getByRole('button', {name: /条新消息/})).toBeVisible()
        await expect.poll(() => history.evaluate((element) => element.scrollTop)).toBeCloseTo(readingTop, 0)
        await panel.getByRole('button', {name: /条新消息/}).click()
        await expect(history).toContainText(arriving)
        await expect
            .poll(() => history.evaluate((element) => element.scrollHeight - element.scrollTop - element.clientHeight))
            .toBeLessThan(2)
        // 拉黑动作清空旧缓存，解除后从服务器恢复；设置与频道偏好跨刷新保留。
        await recipient.getByRole('log').getByRole('button', {name: a.name, exact: true}).first().click()
        await recipient.getByRole('button', {name: `拉黑${a.name}`, exact: true}).click()
        await recipient.getByRole('button', {name: '确认拉黑', exact: true}).click()
        await recipient.getByRole('button', {name: '设置', exact: true}).click()
        await recipient.getByLabel('拒收陌生人私聊').check()
        await expect(recipient.getByLabel('拒收陌生人私聊')).toBeChecked()
        await expect(recipient.getByLabel('私聊提示音')).not.toBeChecked()
        await recipient.getByRole('button', {name: '解除拉黑', exact: true}).click()
        await other.reload()
        await recipient.getByRole('button', {name: '设置', exact: true}).click()
        await expect(recipient.getByLabel('拒收陌生人私聊')).toBeChecked()
        await recipient.getByLabel('拒收陌生人私聊').uncheck()
        await recipient.getByRole('button', {name: '返回聊天', exact: true}).click()
        // 世界频道显示玩家原文，HTML标签不被解析；移动视口也没有横向溢出。
        resetRate(a.id)
        await panel.getByRole('button', {name: '世界', exact: true}).click()
        const plain = `<b>纯文字${suffix}</b>`
        await panel.getByLabel('聊天内容').fill('世界频道草稿')
        await panel.getByRole('button', {name: '私聊', exact: true}).click()
        await panel
            .getByRole('navigation', {name: '私聊会话'})
            .getByRole('button', {name: new RegExp(b.name)})
            .click()
        await expect(panel.getByLabel('聊天内容')).toHaveValue('重连后仍在的草稿')
        await panel.getByRole('button', {name: '世界', exact: true}).click()
        await expect(panel.getByLabel('聊天内容')).toHaveValue('世界频道草稿')
        await panel.getByLabel('聊天内容').dispatchEvent('keydown', {key: 'Enter', isComposing: true})
        await expect(panel.getByLabel('聊天内容')).toHaveValue('世界频道草稿')
        await panel.getByLabel('聊天内容').fill(plain)
        if (info.project.name === 'desktop') {
            // Shift+Enter 只换行，普通 Enter 才发送；前面已单独验证输入法组合确认。
            await panel.getByLabel('聊天内容').press('Shift+Enter')
            await expect(panel.getByLabel('聊天内容')).toHaveValue(`${plain}\n`)
            await panel.getByLabel('聊天内容').press('Enter')
        } else {
            await panel.getByLabel('聊天内容').press('Enter')
            await expect(panel.getByLabel('聊天内容')).toHaveValue(`${plain}\n`)
            await panel.getByRole('button', {name: '发送', exact: true}).click()
        }
        await expect(panel.getByRole('log')).toContainText(plain)
        expect(await panel.getByRole('log').locator('b').count()).toBe(0)
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
        const lightSurface = await panel
            .locator('.chat-content')
            .evaluate((element) => getComputedStyle(element).backgroundColor)
        await page.emulateMedia({colorScheme: 'dark'})
        await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
        expect(
            await panel.locator('.chat-content').evaluate((element) => getComputedStyle(element).backgroundColor),
        ).not.toBe(lightSurface)
        await page.screenshot({path: info.outputPath('chat-world-dark.png')})
        await page.emulateMedia({colorScheme: 'light'})
        // 重新进入会话只加载最新50条；加载更早一页后，原第一条消息在视口中的位置应保持。
        await panel.getByRole('button', {name: '私聊', exact: true}).click()
        await page.screenshot({path: info.outputPath('chat-conversations.png')})
        // 不同对端的草稿独立，即使新会话尚无服务器历史也能往返保留。
        await panel.getByLabel('私聊角色名').fill(c.name)
        await panel.getByRole('button', {name: '联系', exact: true}).click()
        await panel.getByLabel('聊天内容').fill('第三位好友的独立草稿')
        await panel.getByRole('button', {name: '返回会话', exact: true}).click()
        await panel
            .getByRole('navigation', {name: '私聊会话'})
            .getByRole('button', {name: new RegExp(b.name)})
            .click()
        const older = panel.getByRole('button', {name: '加载更早消息', exact: true})
        await expect(panel.getByLabel('聊天内容')).toHaveValue('重连后仍在的草稿')
        await expect(older).toBeVisible()
        await history.evaluate((element) => {
            element.scrollTop = 80
        })
        const anchor = history.locator('article').first()
        const anchorText = await anchor.locator('.message-text').innerText()
        const anchorTop = (await anchor.boundingBox())!.y
        // DOM click 不先滚动按钮，隔离验证分页算法自身的高度补偿。
        await older.evaluate((element: HTMLButtonElement) => element.click())
        await expect(history).toContainText(first)
        // scrollHeight/scrollTop 使用整数像素，而字体布局含小数；容许不足两个 CSS 像素的取整差。
        await expect
            .poll(async () =>
                Math.abs((await history.locator('article').filter({hasText: anchorText}).boundingBox())!.y - anchorTop),
            )
            .toBeLessThan(2)
        // 夹具只设置战斗占用，页面和服务端实际门禁仍由生产代码执行；消息仍通过第三人正式API发送。
        fixture(`UPDATE player_character SET active_battle_id=123 WHERE id=${a.id};`)
        await page.reload()
        await expect(panel).toBeHidden()
        const during = `战后查看的消息${suffix}`
        const received = await other.request.post(`${base}/api/chat/messages`, {
            headers: {Authorization: `Bearer ${c.access}`},
            data: {requestId: randomUUID(), channel: 'PRIVATE', peer: a.name, text: during},
        })
        expect(received.ok()).toBe(true)
        await expect(panel).toBeHidden()
        const blocked = await other.request.get(`${base}/api/chat/messages?channel=PRIVATE&peer=${c.name}`, {
            headers: {Authorization: `Bearer ${a.access}`},
        })
        expect(blocked.status()).toBe(409)
        fixture(`UPDATE player_character SET active_battle_id=NULL WHERE id=${a.id};`)
        await page.goto(`${base}/world`)
        await expect(panel).toBeVisible()
        await panel.getByRole('button', {name: '私聊', exact: true}).click()
        await panel
            .getByRole('navigation', {name: '私聊会话'})
            .getByRole('button', {name: new RegExp(c.name)})
            .click()
        await expect(panel.getByRole('log')).toContainText(during)
        // 换号在同一浏览器完成：旧角色正文、选中对端与草稿不得进入第三人的新面板。
        await page.evaluate((token) => {
            localStorage.setItem('avalon.refresh-token', token)
        }, c.refresh)
        await page.reload()
        await panel.getByRole('button', {name: /聊天/}).click()
        await expect(panel.getByRole('button', {name: '世界', exact: true})).toHaveAttribute('aria-pressed', 'true')
        await expect(panel.getByLabel('聊天内容')).toHaveValue('')
        await expect(panel.getByRole('log')).not.toContainText(first)
        await panel.getByRole('button', {name: /^系统/}).click()
        await expect(panel.getByRole('log')).toContainText('任务交付完成：今天要做的事')
        await expect(panel.getByLabel('聊天内容')).toHaveCount(0)
        await page.screenshot({path: info.outputPath('chat-system.png')})
        expect(errors).toEqual([])
    } finally {
        await page.context().setOffline(false)
        await otherContext.close()
    }
})
