/**
 * 这里只定义页面地址与按需加载组件。页面路由不改变地图位置；未知页面回到受保护的世界入口。
 */
import type {RouteRecordRaw} from 'vue-router'
import {lifeActivityEntries} from '@/features/life/activities'

/** 页面按需加载；路由地址不携带角色位置，地图移动仍由服务端校验。 */
export const routes: RouteRecordRaw[] = [
    {path: '/', redirect: '/world'},
    {path: '/login', component: () => import('@/pages/auth/LoginPage.vue')},
    {
        path: '/character/create',
        component: () => import('@/pages/character/CreateCharacterPage.vue'),
    },
    {path: '/world', component: () => import('@/pages/world/WorldPage.vue')},
    {path: '/more', component: () => import('@/pages/navigation/MorePage.vue')},
    {path: '/life', component: () => import('@/pages/life/LifeSkillsPage.vue')},
    ...lifeActivityEntries.map((activity) => ({path: activity.path, component: activity.page})),
    {path: '/friends', component: () => import('@/pages/social/FriendsPage.vue')},
    {path: '/more/navigation', component: () => import('@/pages/navigation/NavigationPage.vue')},
    {path: '/quests', component: () => import('@/pages/quest/QuestsPage.vue')},
    {path: '/encyclopedia', component: () => import('@/pages/creature/CompanionsPage.vue')},
    {path: '/character', component: () => import('@/pages/character/CharacterPage.vue')},
    {path: '/shop', component: () => import('@/pages/inventory/ShopPage.vue')},
    {path: '/inventory', component: () => import('@/pages/inventory/InventoryPage.vue')},
    {path: '/crafting', component: () => import('@/pages/inventory/CraftingPage.vue')},
    {path: '/companions', component: () => import('@/pages/creature/CompanionsPage.vue')},
    {path: '/evolution', component: () => import('@/pages/creature/EvolutionPage.vue')},
    {path: '/market', component: () => import('@/pages/market/MarketPage.vue')},
    {path: '/enhancement', component: () => import('@/pages/inventory/EnhancementPage.vue')},
    {path: '/companions/:id', component: () => import('@/pages/creature/CompanionPage.vue')},
    {path: '/companions/:id/learning', component: () => import('@/pages/creature/LearningPage.vue')},
    {path: '/battle', component: () => import('@/pages/battle/BattlePage.vue')},
    {path: '/academy/workshop', component: () => import('@/pages/academy/WorkshopPage.vue')},
    {path: '/dungeon/:id', component: () => import('@/pages/dungeon/DungeonPage.vue')},
    {path: '/dungeon-lobby', component: () => import('@/pages/dungeon/DungeonLobbyPage.vue')},
    {path: '/:pathMatch(.*)*', redirect: '/world'},
]
