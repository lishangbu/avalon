/** 页面入口和读取契约；命令、实时会话及权限判断由各活动实现。导航不会移动角色。 */
export const lifeActivities = {
    gathering: {
        title: '野外采集',
        path: '/life/gathering',
        endpoint: '/api/life/gathering',
        queryKey: ['gathering'],
        loading: '正在读取采集资源…',
        description: '资源不限次数。保持页面在线前台才推进，离页或断线暂停；结束派遣后才能移动或战斗。',
        page: () => import('@/pages/life/GatheringPage.vue'),
    },
    farm: {
        title: '我的农田',
        path: '/life/farm',
        endpoint: '/api/life/farm',
        queryKey: ['life-farm'],
        loading: '正在查看农田…',
        description: '村庄广场与诺姆城共用一块农田。播种后浇水一次开始生长，离线也会成长，成熟作物不会枯萎。',
        page: () => import('@/pages/life/FarmPage.vue'),
    },
    food: {
        title: '食用料理',
        path: '/life/food',
        endpoint: '/api/life/food',
        queryKey: ['life-food'],
        loading: '正在读取料理…',
        description: '战斗外为人物或随身伙伴恢复生命、魔法，无额外增益。无需学习烹饪即可食用。',
        page: () => import('@/pages/life/FoodPage.vue'),
    },
    fishing: {
        title: '钓鱼',
        path: '/life/fishing',
        endpoint: '/api/life/fishing',
        queryKey: ['fishing'],
        loading: '正在读取钓鱼水域…',
        description:
            '按住让浮标上升，松开下降。让浮标留在鱼区内积累进度，每局10～20秒。切后台或断线会结束本局，鱼饵不返还。',
        page: () => import('@/pages/life/FishingPage.vue'),
    },
    production: {
        title: '生活制造',
        path: '/life/production',
        endpoint: '/api/life/production',
        queryKey: ['life-production'],
        loading: '正在读取制造配方…',
        description: '到指定设施即时制造。每件完成后才检查工具报废，未完成部分不扣材料。',
        page: () => import('@/pages/life/LifeProductionPage.vue'),
    },
} as const

export type LifeActivityId = keyof typeof lifeActivities
export const lifeActivityEntries = Object.values(lifeActivities)
