import {authenticatedStream} from '@/shared/http'

/**
 * 单个角色组件拥有一条实时连接，卸载即停止。服务端发送的ready/changed仅表示数据库需要重新读取。
 * 使用流式TextDecoder处理UTF-8分块；SSE帧可能横跨多个网络块，必须保留不完整尾部。
 * 45秒服务端正常断开也会重连，ready随后补齐停机/断网期间的消息；失败指数退避避免请求风暴。
 * offline立即取消当前读取，online立即重连；不会等浏览器判定半断开的TCP超时才补消息。
 */
export function connectChat(changed: () => void, status: (connected: boolean) => void) {
    let stopped = false,
        attempt = 0
    let active: AbortController | undefined
    let timer: ReturnType<typeof setTimeout> | undefined
    let delay = 1000
    async function connect() {
        if (stopped || !navigator.onLine) return
        const current = ++attempt
        const controller = new AbortController()
        active = controller
        try {
            const response = await authenticatedStream('/api/chat/events', controller.signal)
            const reader = response.body?.getReader()
            if (!reader) throw new Error('聊天流不可用')
            if (stopped || current !== attempt) {
                await reader.cancel()
                return
            }
            status(true)
            delay = 1000
            const decoder = new TextDecoder()
            let pending = ''
            while (!controller.signal.aborted && current === attempt) {
                const chunk = await reader.read()
                if (chunk.done) break
                pending += decoder.decode(chunk.value, {stream: true}).replace(/\r/g, '')
                let boundary: number
                while ((boundary = pending.indexOf('\n\n')) >= 0) {
                    const frame = pending.slice(0, boundary)
                    pending = pending.slice(boundary + 2)
                    if (/^event:(?: ?)(ready|changed)$/m.test(frame)) changed()
                }
            }
        } catch {
            // 连接错误只改变在线提示，不清空数据库历史、草稿或未读；身份失效由共享HTTP适配处理。
        } finally {
            // 旧连接的迟到finally不能覆盖刚刚online建立的新连接，也不能留下第二个重连定时器。
            if (!stopped && current === attempt) {
                status(false)
                timer = setTimeout(() => void connect(), delay)
                delay = Math.min(delay * 2, 10000)
            }
        }
    }
    function offline() {
        attempt++
        active?.abort()
        if (timer) clearTimeout(timer)
        status(false)
    }
    function online() {
        if (stopped) return
        if (timer) clearTimeout(timer)
        active?.abort()
        void connect()
    }
    window.addEventListener('offline', offline)
    window.addEventListener('online', online)
    void connect()
    return () => {
        stopped = true
        attempt++
        active?.abort()
        if (timer) clearTimeout(timer)
        window.removeEventListener('offline', offline)
        window.removeEventListener('online', online)
    }
}
