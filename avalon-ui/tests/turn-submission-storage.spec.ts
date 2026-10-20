import {expect, test} from '@playwright/test'
import {
    clearTurnSubmission,
    readTurnSubmission,
    saveTurnSubmission,
} from '../src/features/battle/turn-submission-storage'

/** 独立存储模拟关闭后重新读取，验证协议边界；不冒充真实浏览器进程恢复证据。 */
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
const body = {
    requestId: '11111111-1111-4111-8111-111111111111',
    revision: 3,
    round: 2,
    orders: [{actor: 'hero', action: 'DEFEND', target: 'hero', targets: ['hero']}],
    surrender: false,
    replacements: {pet: 1},
}

test('恢复完整原命令且隔离不同战局', () => {
    const storage = new MemoryStorage()
    saveTurnSubmission(storage, '100', body)
    expect(readTurnSubmission(storage, '100')).toEqual(body)
    expect(readTurnSubmission(storage, '200')).toBeUndefined()
})

test('不同请求独立保存，旧回执不得清理新请求', () => {
    const storage = new MemoryStorage()
    const next = {...body, revision: 4, requestId: '22222222-2222-4222-8222-222222222222'}
    saveTurnSubmission(storage, '100', next)
    saveTurnSubmission(storage, '100', body)
    expect(readTurnSubmission(storage, '100')).toEqual(body)
    clearTurnSubmission(storage, '100', body.requestId)
    expect(readTurnSubmission(storage, '100')).toEqual(next)
    clearTurnSubmission(storage, '100', body.requestId)
    expect(readTurnSubmission(storage, '100')).toEqual(next)
})

test('损坏命令与槽位错配不得恢复；额外字段不得进入命令', () => {
    const storage = new MemoryStorage()
    const key = `avalon.battle-turn.v1:100:${body.requestId}`
    for (const invalid of [
        '{',
        'null',
        JSON.stringify({...body, revision: -1}),
        JSON.stringify({...body, round: 0.5}),
        JSON.stringify({...body, orders: [{actor: 'hero'}]}),
        JSON.stringify({...body, replacements: {pet: -1}}),
        JSON.stringify({...body, requestId: 'wrong'}),
    ]) {
        storage.setItem(key, invalid)
        expect(readTurnSubmission(storage, '100')).toBeUndefined()
        expect(storage.length).toBe(0)
    }
    storage.setItem(
        key,
        JSON.stringify({...body, gold: 999, orders: body.orders.map((order) => ({...order, damage: 999}))}),
    )
    expect(readTurnSubmission(storage, '100')).toEqual(body)
})

test('存储不可写时抛出失败，不伪装命令已保存', () => {
    const storage = new MemoryStorage()
    storage.setItem = () => {
        throw new Error('quota')
    }
    expect(() => saveTurnSubmission(storage, '100', body)).toThrow('quota')
})
