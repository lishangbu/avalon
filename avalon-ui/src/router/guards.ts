/**
 * 页面准入以服务端角色状态为准。首次导航恢复会话，未登录进入登录页，无角色进入创角页，已有角色进入世界页。请求失败保留可重试错误，不伪造角色存在；路由地址绝不能决定地图位置。
 */
import type {Router} from 'vue-router'
import {queryClient} from '@/app/query-client'
import {authenticated, restoreSession} from '@/features/auth/session'
import {worldOptions} from '@/features/world/queries'
import {battleOptions} from '@/features/battle/queries'
import {dungeonCurrentOptions, dungeonRunOptions} from '@/features/dungeon/queries'
import {HttpError} from '@/shared/http'
import {beginNavigation, failNavigation} from './state'

/** 为路由实例安装准入守卫；会话恢复标记归属于该实例，不承担缓存初始化。 */
export function installGuards(router: Router) {
    let initialized = false
    // 页面地址只表达界面，不携带地图位置。刷新、返回、直接输入地址都经过同一准入判断。
    router.beforeEach(async (to) => {
        beginNavigation()
        try {
            if (!initialized) {
                await restoreSession()
                initialized = true
            }
            if (!authenticated.value) return to.path === '/login' ? true : '/login'
            const world = await queryClient.fetchQuery(worldOptions())
            // 隔离实践课期间回到同一会话；地址切换不能把临时资产带入正式页面。
            if (world.character?.activeCourseId) return to.path === '/academy/workshop' ? true : '/academy/workshop'
            if (world.character) {
                const dungeon = await queryClient.fetchQuery(dungeonCurrentOptions())
                if (dungeon.run) {
                    const path = `/dungeon/${dungeon.run.id}`
                    return to.path === path ? true : path
                }
                // 通关后允许查看结果；战斗中则始终由当前进度决定入口。
                if (to.path.startsWith('/dungeon/') && !world.character.activeBattleId) {
                    try {
                        const completed = await queryClient.fetchQuery(dungeonRunOptions(String(to.params.id)))
                        if (completed.completed) return true
                    } catch (cause) {
                        if (!(cause instanceof HttpError) || cause.status !== 404) throw cause
                    }
                    return '/world'
                }
            }
            let target = world.character ? (world.character.activeBattleId ? '/battle' : '/world') : '/character/create'
            // 非战斗页面仅改变显示内容。地点服务的权限仍由各自后端接口重新判断。
            if (
                world.character &&
                !world.character.activeBattleId &&
                ([
                    '/world',
                    '/character',
                    '/shop',
                    '/companions',
                    '/inventory',
                    '/crafting',
                    '/life',
                    '/life/gathering',
                    '/life/farm',
                    '/life/food',
                    '/life/fishing',
                    '/life/production',
                    '/dungeon-lobby',
                    '/more',
                    '/friends',
                    '/more/navigation',
                    '/quests',
                    '/encyclopedia',
                    '/evolution',
                    '/market',
                    '/enhancement',
                ].includes(to.path) ||
                    to.path.startsWith('/companions/'))
            )
                return true
            // 战斗结束后允许停留结算页；未结束时地图与个人页面一律回到战场。
            if (world.character && !world.character.activeBattleId && to.path === '/battle') {
                const battle = await queryClient.fetchQuery(battleOptions())
                if (battle) target = '/battle'
            }
            return to.path === target ? true : target
        } catch (cause) {
            // 读取地图时会话失效属于准入变化，不在登录页留下旧请求的错误提示。
            if (initialized && !authenticated.value) return to.path === '/login' ? true : '/login'
            failNavigation(cause)
            return false
        }
    })
}
