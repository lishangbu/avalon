import {expect, test} from '@playwright/test'
import {
    clearSceneSubmission,
    readSceneSubmission,
    saveSceneSubmission,
    sceneSubmissionKey,
} from '../src/features/world/scene-submission-storage'

/** 用独立内存存储核对信任边界，不依赖浏览器、账号或数据库。 */
class MemoryStorage implements Storage {
    private entries = new Map<string, string>()
    get length() {
        return this.entries.size
    }
    clear() {
        this.entries.clear()
    }
    getItem(key: string) {
        return this.entries.get(key) ?? null
    }
    key(index: number) {
        return [...this.entries.keys()][index] ?? null
    }
    removeItem(key: string) {
        this.entries.delete(key)
    }
    setItem(key: string, value: string) {
        this.entries.set(key, value)
    }
}

const submission = {
    actionId: 'N015_SHUTDOWN',
    requestId: 'd78b5c37-2e8a-46e1-b601-964f87909aa0',
    expectedRevision: 52,
}

test('恢复原请求并隔离角色与人物面板', () => {
    const storage = new MemoryStorage()
    const key = sceneSubmissionKey('100')
    storage.setItem(`${key}:${submission.requestId}`, JSON.stringify(submission))
    expect(readSceneSubmission(storage, key)).toEqual(submission)
    expect(readSceneSubmission(storage, sceneSubmissionKey('200'))).toBeUndefined()
    expect(readSceneSubmission(storage, sceneSubmissionKey('100', 'SHALIN'))).toBeUndefined()
    expect(storage.getItem(`${key}:${submission.requestId}`)).not.toBeNull()
})

test('损坏JSON及非法版本不得恢复为可提交命令', () => {
    const storage = new MemoryStorage()
    const key = sceneSubmissionKey('100')
    for (const raw of [
        '{',
        'null',
        JSON.stringify({...submission, expectedRevision: -1}),
        JSON.stringify({...submission, expectedRevision: 0.5}),
        JSON.stringify({...submission, requestId: 'invalid'}),
    ]) {
        storage.setItem(`${key}:${submission.requestId}`, raw)
        expect(readSceneSubmission(storage, key)).toBeUndefined()
        expect(storage.getItem(`${key}:${submission.requestId}`)).toBeNull()
    }
})

test('只恢复协议字段并保留原答案及版本', () => {
    const storage = new MemoryStorage()
    const key = sceneSubmissionKey('100')
    storage.setItem(
        `${key}:${submission.requestId}`,
        JSON.stringify({...submission, choiceId: 'NEAR', gold: 999999, completed: true}),
    )
    expect(readSceneSubmission(storage, key)).toEqual({...submission, choiceId: 'NEAR'})
})

test('旧回执不能删除其他标签页的新请求', () => {
    const storage = new MemoryStorage()
    const key = sceneSubmissionKey('100')
    storage.setItem(`${key}:${submission.requestId}`, JSON.stringify(submission))
    clearSceneSubmission(storage, key, '11111111-1111-4111-8111-111111111111')
    expect(readSceneSubmission(storage, key)).toEqual(submission)
    clearSceneSubmission(storage, key, submission.requestId)
    expect(storage.getItem(`${key}:${submission.requestId}`)).toBeNull()
})

test('并发标签页的独立请求均可恢复，清理一项不会丢失另一项', () => {
    const storage = new MemoryStorage()
    const key = sceneSubmissionKey('100')
    const second = {...submission, requestId: '11111111-1111-4111-8111-111111111111', expectedRevision: 53}
    saveSceneSubmission(storage, key, submission)
    saveSceneSubmission(storage, key, second)
    expect(storage.length).toBe(2)
    expect(readSceneSubmission(storage, key)).toEqual(submission)
    clearSceneSubmission(storage, key, submission.requestId)
    expect(readSceneSubmission(storage, key)).toEqual(second)
    clearSceneSubmission(storage, key, second.requestId)
    expect(storage.length).toBe(0)
})

test('损坏独立记录不妨碍其他未确认请求恢复', () => {
    const storage = new MemoryStorage()
    const key = sceneSubmissionKey('100')
    storage.setItem(`${key}:broken`, '{')
    storage.setItem(`${key}:wrong-request`, JSON.stringify(submission))
    saveSceneSubmission(storage, key, submission)
    expect(readSceneSubmission(storage, key)).toEqual(submission)
    expect(storage.getItem(`${key}:broken`)).toBeNull()
    expect(storage.getItem(`${key}:wrong-request`)).toBeNull()
    storage.setItem(`${key}:${submission.requestId}`, JSON.stringify(submission))
    clearSceneSubmission(storage, key, submission.requestId)
    expect(storage.length).toBe(0)
})
