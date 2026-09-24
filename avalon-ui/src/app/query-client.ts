/**
 * 整个应用共享一个 Vue Query 客户端，路由预读、页面查询和退出清理必须引用同一实例。不得在页面创建第二份客户端，否则会形成跨账号残留或互不刷新的缓存。
 */
import {QueryClient} from '@tanstack/vue-query'

/** 应用共享一个服务端缓存实例；路由准入、业务查询和退出清理使用同一份数据。 */
export const queryClient = new QueryClient()
