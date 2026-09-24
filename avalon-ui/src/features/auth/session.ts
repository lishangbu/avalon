/**
 * 会话状态仅存在于当前浏览器标签页；短期访问令牌在内存，长期刷新令牌按已确认方案放 localStorage。刷新共用 Promise，支持 Web Locks 时进一步串行化跨标签页刷新。代际只隔离身份，不是游戏内容版本。
 */
import type {Tokens} from './types'
import {readonly, ref} from 'vue'

/** 访问 Token 只保留在内存；刷新 Token 按已确认的浏览器方案持久化。 */
const refreshKey = 'avalon.refresh-token'
const signedIn = ref(false)
export const authenticated = readonly(signedIn)
/** 每次身份失效递增，拒绝迟到的刷新响应重新登录旧账号。 */
let generation = 0

export function sessionGeneration() {
    return generation
}

export function currentAccessToken() {
    return accessToken
}

let accessToken: string | undefined
let refreshing: Promise<boolean> | undefined

export function acceptTokens(tokens: Tokens) {
    localStorage.setItem(refreshKey, tokens.refreshToken)
    accessToken = tokens.accessToken
    signedIn.value = true
}

export function clearSession() {
    generation++
    accessToken = undefined
    signedIn.value = false
    localStorage.removeItem(refreshKey)
}

/** 单页共享刷新 Promise；支持 Web Locks 的浏览器进一步串行化多标签页刷新。 */
export function restoreSession(): Promise<boolean> {
    if (refreshing) return refreshing
    const startedAt = generation
    const rotate = async () => {
        const token = localStorage.getItem(refreshKey)
        if (!token) {
            clearSession()
            return false
        }
        const response = await fetch('/api/auth/refresh', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({refreshToken: token}),
        })
        if (startedAt !== generation) return false
        if (response.status === 401) {
            clearSession()
            return false
        }
        if (!response.ok) throw new Error('暂时无法恢复会话，请稍后重试')
        const tokens = (await response.json()) as Tokens
        if (startedAt !== generation) return false
        acceptTokens(tokens)
        return true
    }
    const result = (async () =>
        navigator.locks ? await navigator.locks.request('avalon-session-refresh', rotate) : await rotate())().finally(
        () => {
            refreshing = undefined
        },
    )
    refreshing = result
    return result
}
