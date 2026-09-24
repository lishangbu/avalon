/**
 * 同一组弹层只占一个浏览器历史位置。嵌套弹层逐层关闭；Ionic 路由只看到原来的页面历史。
 * 使用捕获阶段消费这个位置的 popstate，避免 Router 把弹层返回误认为页面返回。
 */
interface Sheet {
    close: () => void
    canClose: () => boolean
}

const sheets: Sheet[] = []
let armed = false
let pagePosition = 0
let restoring = false
let removing: Promise<void> | undefined
let finishRemoval: (() => void) | undefined
const marker = 'avalonSheet'

function arm() {
    if (armed || removing || !sheets.length) return
    // 不改变 Router 的 position：弹层位置对页面路由不可见。
    pagePosition = window.history.state.position
    window.history.pushState({...window.history.state, [marker]: true}, '', window.location.href)
    armed = true
}

function removePosition() {
    if (!armed || removing) return
    removing = new Promise<void>((resolve) => {
        finishRemoval = resolve
    })
    window.history.back()
}

window.addEventListener(
    'popstate',
    (event) => {
        if (!armed) return
        event.stopImmediatePropagation()
        // 历史菜单可能一次越过弹层和页面。先回到弹层所属页面的原始位置，
        // 再关闭顶层；否则消费事件后，地址栏与 Router 内部位置会永久分离。
        if (!restoring && typeof event.state?.position === 'number' && event.state.position !== pagePosition) {
            restoring = true
            window.history.go(pagePosition - event.state.position)
            return
        }
        restoring = false
        armed = false
        if (removing) {
            const finish = finishRemoval
            removing = undefined
            finishRemoval = undefined
            finish?.()
            arm()
            return
        }
        const top = sheets.at(-1)
        if (top?.canClose()) {
            // 父组件 v-model 的回传要等 Vue 更新；先出栈，避免最后一层又被重新压入历史。
            sheets.pop()
            top.close()
        }
        // busy 时重新占住历史位置；嵌套时为剩余弹层保留一次返回。
        arm()
    },
    {capture: true},
)

/** 模型变更同步注销，关闭按钮、手势、Escape 和浏览器返回使用同一个栈。 */
export function registerSheet(sheet: Sheet) {
    sheets.push(sheet)
    arm()
    return () => {
        const index = sheets.indexOf(sheet)
        if (index >= 0) sheets.splice(index, 1)
        if (!sheets.length) removePosition()
    }
}

/** 路由必须等待显式关闭所引起的异步 history.back，防止新页面被它带走。 */
export async function beforeSheetNavigation(authenticated: boolean) {
    if (removing) await removing
    if (!sheets.length) return true
    if (!authenticated) {
        for (const sheet of [...sheets].reverse()) sheet.close()
        if (removing) await removing
        return true
    }
    const top = sheets.at(-1)!
    if (top.canClose()) top.close()
    if (removing) await removing
    return false
}
