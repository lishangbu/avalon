/** 只保存待确认命令，不保存令牌、服务端成果或奖励；完成与权限仍由正式接口判断。 */
export type SceneSubmission = {actionId: string; requestId: string; expectedRevision: number; choiceId?: string}

/** 角色与人物面板分别隔离，防止换号或打开另一人物后重放前一角色的命令。 */
export function sceneSubmissionKey(characterId: string, npcId?: string) {
    return `avalon.scene-pending.v1:${characterId}:${npcId ?? 'field'}`
}

/** 每个请求使用独立槽位，两个标签页同时保存也不会覆盖彼此的未确认命令。 */
export function saveSceneSubmission(storage: Storage, key: string, request: SceneSubmission) {
    storage.setItem(`${key}:${request.requestId}`, JSON.stringify(request))
}

/** 只清理该请求的独立槽位，不能删除另一标签页的命令。 */
export function clearSceneSubmission(storage: Storage, key: string, requestId: string) {
    const ownKey = `${key}:${requestId}`
    if (readStoredSubmission(storage, ownKey)?.requestId === requestId) storage.removeItem(ownKey)
}

/** 依版本和请求号稳定选择一个未确认请求；确认后由页面继续读取下一项。 */
export function readSceneSubmission(storage: Storage, key: string): SceneSubmission | undefined {
    // 先复制键名，再校验内容，避免移除损坏项时改变Storage下标而漏读后续记录。
    const keys = Array.from({length: storage.length}, (_, index) => storage.key(index)).filter(
        (entry): entry is string => entry !== null && entry.startsWith(`${key}:`),
    )
    return keys
        .map((entry) => {
            const request = readStoredSubmission(storage, entry)
            // 槽位与负载请求号必须一致，否则确认时找不到该槽位，会无限恢复同一条损坏记录。
            if (request && entry !== `${key}:${request.requestId}`) {
                storage.removeItem(entry)
                return undefined
            }
            return request
        })
        .filter((entry): entry is SceneSubmission => entry !== undefined)
        .sort((a, b) => a.expectedRevision - b.expectedRevision || a.requestId.localeCompare(b.requestId))[0]
}

/** 本地存储是非可信输入。损坏或过期结构只删除，不自动生成新请求或调用写接口。 */
function readStoredSubmission(storage: Storage, key: string): SceneSubmission | undefined {
    const raw = storage.getItem(key)
    if (!raw) return undefined
    let value: unknown
    try {
        value = JSON.parse(raw)
    } catch {
        storage.removeItem(key)
        return undefined
    }
    const object = value as Partial<SceneSubmission> | null
    const identifier = (input: unknown): input is string =>
        typeof input === 'string' && /^[A-Z0-9_]{1,128}$/.test(input)
    if (
        !object ||
        typeof object !== 'object' ||
        !identifier(object.actionId) ||
        typeof object.requestId !== 'string' ||
        !/^[0-9a-f-]{36}$/i.test(object.requestId) ||
        !Number.isSafeInteger(object.expectedRevision) ||
        object.expectedRevision! < 0 ||
        (object.choiceId !== undefined && !identifier(object.choiceId))
    ) {
        storage.removeItem(key)
        return undefined
    }
    // 明确投影字段，不能把本地JSON中的额外属性直接扩散进服务端命令。
    return {
        actionId: object.actionId,
        requestId: object.requestId,
        expectedRevision: object.expectedRevision!,
        ...(object.choiceId === undefined ? {} : {choiceId: object.choiceId}),
    }
}
