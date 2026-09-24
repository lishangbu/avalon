/** 主题是设备偏好。存储被禁用时只保存内存值，不影响登录或游戏。 */
import {ref, watch} from 'vue'

export type ThemePreference = 'system' | 'light' | 'dark'
export const theme = ref<ThemePreference>('system')
const key = 'avalon.theme'

export function installTheme() {
    try {
        const saved = localStorage.getItem(key)
        if (saved === 'light' || saved === 'dark') theme.value = saved
    } catch {
        /* 存储不可用时使用系统偏好。 */
    }
    const media = matchMedia('(prefers-color-scheme: dark)')
    const apply = () => {
        const dark = theme.value === 'dark' || (theme.value === 'system' && media.matches)
        document.documentElement.classList.toggle('ion-palette-dark', dark)
        document.documentElement.dataset.theme = dark ? 'dark' : 'light'
        document.querySelector('meta[name="theme-color"]')?.setAttribute('content', dark ? '#111216' : '#f2f3f7')
    }
    media.addEventListener('change', apply)
    watch(theme, (value) => {
        try {
            localStorage.setItem(key, value)
        } catch {
            /* 当前页面仍应用偏好。 */
        }
        apply()
    })
    apply()
}
