/**
 * 认证 HTTP 用例封装。成功登录才接收令牌；退出和撤销当前设备清理会话，由启动监听统一移除缓存。设备名称仅用于显示，不构成可信设备授权。
 */
import {api} from '@/shared/http'
import {acceptTokens, clearSession} from './session'
import type {Device, Tokens} from './types'

export async function signIn(username: string, password: string, register: boolean) {
    const tokens = await api<Tokens>(
        register ? '/api/auth/register' : '/api/auth/login',
        'POST',
        {username, password, device: 'Web 浏览器'},
        false,
    )
    acceptTokens(tokens)
}

export async function signOut() {
    await api('/api/auth/logout', 'POST')
    clearSession()
}

export const listDevices = () => api<Device[]>('/api/auth/sessions')

export async function revokeDevice(device: Device) {
    await api(`/api/auth/sessions/${device.id}`, 'DELETE')
    if (device.current) clearSession()
}
