/**
 * 操作辅助函数将错误保留在所属组件，返回成功标记控制是否关闭弹窗。游戏命令共享浏览器内的互斥标记，登录和设备读取使用局部标记。这个互斥仅改善交互，服务端仍必须验证事务、锁、幂等与归属。
 */
import {readonly, ref} from 'vue'

/** 同一浏览器内的游戏写操作互斥；它不替代服务端事务与 revision 校验。 */
const writing = ref(false)

export function useAction(gameWrite = false) {
    const pending = gameWrite ? writing : ref(false)
    const error = ref('')

    /** 返回成功标记，避免失败后仍关闭弹窗或切换页面。 */
    async function run(action: () => Promise<unknown>): Promise<boolean> {
        if (pending.value) return false
        pending.value = true
        error.value = ''
        try {
            await action()
            return true
        } catch (cause) {
            error.value = cause instanceof Error ? cause.message : '操作未完成，请重试'
            return false
        } finally {
            pending.value = false
        }
    }

    return {busy: readonly(pending), error, run}
}
