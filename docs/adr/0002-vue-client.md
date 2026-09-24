# Vue 客户端与单仓库集成

状态：已确认。客户端采用 Vue 3 与 Ionic Vue，面向手机和桌面浏览器。

前端位于 avalon 仓库的 avalon-ui，采用 Vue 3、TypeScript、Vite、Ionic Vue，统一 iOS 视觉模式，支持浅色、深色与跟随系统。地图、战斗等游戏组件自定义。TanStack Query 管理服务端数据，Ionic Vue Router 管理页面及缓存生命周期。布局以手机为先，桌面主要内容限制在 480px。

当前前端结构见 [前端说明](../frontend-structure.md)，实际验证范围见 [当前验收](../engineering-acceptance.md)。

frontend-maven-plugin 在后端启动模块构建前端，Spring Boot 提供静态产物，Web 页面与 API 同源发布。开发时独立运行 Vite 5173，代理后端 8080。前端保留独立 package.json 和锁文件，不注册为 Java 子模块。当前仅交付手机与桌面浏览器版本。Android 原生封装暂缓，不引入 Capacitor 或原生插件。

长期刷新 Token 使用 localStorage；短期访问 Token 仅保留在内存。不在 URL 中传递 Token。不渲染未经消毒的用户 HTML。

页面设计覆盖手机／桌面布局、返回导航、刷新与断线恢复；当前实际验收范围以验收记录为准，模拟视口不等于真机软键盘验证。Android 安装包不属于本阶段交付；未来增加原生目标时另行选型与验收。
