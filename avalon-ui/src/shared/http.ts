/**
 * 协议层不认识角色或任务。认证适配在启动时注入；受保护接口401最多恢复一次会话并重发，其他业务错误不自动重试写操作。请求发送前后的会话代际用于拒绝旧身份响应，避免新账号重放旧命令。
 */
/** HTTP 层仅处理协议，不认识角色、任务或 Vue 状态。认证策略在应用启动时注入。 */
interface Authentication {
    token(): string | undefined

    restore(): Promise<boolean>

    clear(): void

    generation(): number
}

let authentication: Authentication

/** 保留响应状态供导航区分无权限与临时故障，不能把网络失败当作角色已离场。 */
export class HttpError extends Error {
    constructor(
        public readonly status: number,
        message: string,
    ) {
        super(message)
    }
}

export function configureAuthentication(value: Authentication) {
    authentication = value
}

/** 仅受保护接口尝试刷新一次；业务失败不自动重试写操作，避免重复提交。 */
export async function api<T>(path: string, method = 'GET', body?: unknown, protectedRequest = true): Promise<T> {
    const generation = authentication.generation()
    const assertSession = () => {
        if (protectedRequest && generation !== authentication.generation()) throw new Error('会话已改变，请重新操作')
    }
    const send = () =>
        fetch(path, {
            method,
            headers: {
                'Content-Type': 'application/json',
                ...(protectedRequest && authentication.token()
                    ? {Authorization: `Bearer ${authentication.token()}`}
                    : {}),
            },
            body: body === undefined ? undefined : JSON.stringify(body),
        })
    let response = await send()
    // 旧请求的 401 不能触发新账号刷新，更不能以新账号身份重放旧写操作。
    assertSession()
    if (response.status === 401 && protectedRequest && (await authentication.restore())) {
        assertSession()
        response = await send()
    }
    assertSession()
    if (!response.ok) {
        if (response.status === 401 && protectedRequest) authentication.clear()
        const error = (await response.json().catch(() => ({}))) as {
            message?: string
        }
        throw new HttpError(
            response.status,
            error.message ?? (response.status === 401 ? '请重新登录' : '操作未完成，请重试'),
        )
    }
    if (response.status === 204) return undefined as T
    const result = (await response.json()) as T
    assertSession()
    return result
}
