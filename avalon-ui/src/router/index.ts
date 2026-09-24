/**
 * 创建唯一 Router 并安装守卫，不创建业务缓存或会话存储。HTML5 history 与 Spring Boot 的 SPA 回退配套，直接打开子路径由服务器返回应用入口。
 */
import {createRouter, createWebHistory} from '@ionic/vue-router'
import {routes} from './routes'
import {installGuards} from './guards'
import {beforeSheetNavigation} from './sheets'
import {authenticated} from '@/features/auth/session'

/** 此处只装配 Router；路由表、准入规则与导航状态各有明确归属。 */
export const router = createRouter({
    history: createWebHistory(),
    routes,
})
router.beforeEach(() => beforeSheetNavigation(authenticated.value))
installGuards(router)
