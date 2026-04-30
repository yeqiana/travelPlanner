# AgentTravel 移动端前端目录结构规范

本文档定义 TravelPlanner / AgentTravel 前端后续推荐目录结构。当前阶段只沉淀规范，不实际搬迁文件。

## 推荐结构

```text
frontend/src/
  app/
    App.tsx
    main.tsx
    providers/
    routes/
  pages/
    home/
      HomePage.tsx
    chat/
      ChatPage.tsx
  features/
    travel-planning/
      api/
      model/
      components/
      hooks/
      lib/
    chat-session/
      model/
      components/
      hooks/
    user-preferences/
      model/
      components/
      hooks/
    itinerary-share/
      components/
      lib/
  shared/
    api/
    config/
    hooks/
    lib/
    storage/
    types/
    ui/
  styles/
    index.css
    tokens.css
    tailwind.css
  assets/
    images/
    icons/
```

## 分层职责

### app

放应用级入口和全局配置。

适合放：

- React 根组件。
- 全局 Provider。
- 路由注册。
- 应用外壳布局。

不适合放：

- 具体业务表单。
- 行程生成逻辑。
- 会话 CRUD 细节。
- 大段页面 JSX。

### pages

放页面级编排组件。页面组件只负责组织 feature 和 shared 组件，不直接承载复杂业务逻辑。

建议页面：

- `home/HomePage.tsx`：旅行需求入口页。
- `chat/ChatPage.tsx`：聊天和行程结果页。

规则：

- 页面可以读取 route 参数和组合布局。
- 页面不直接调用模型 SDK。
- 页面不直接写复杂 localStorage 逻辑。

### features

按业务能力组织。一个 feature 应包含自己的 API、类型、hook 和业务组件。

建议 feature：

- `travel-planning`：行程生成、行程展示、行程导出。
- `chat-session`：历史会话、当前会话、搜索、删除、重命名。
- `user-preferences`：用户长期旅行偏好。
- `itinerary-share`：一图流、地点分享、行程海报。

规则：

- 业务组件放在对应 feature 内。
- feature 可以依赖 `shared`，但不同 feature 之间不要随意互相引用内部文件。
- 如果确实需要跨 feature 复用，优先提升到 `shared`。

### shared

放无业务语义的通用能力。

适合放：

- 基础 UI：按钮、输入框、弹窗、抽屉、chip、时间线基础件。
- 通用 hook。
- 通用 storage 工具。
- 通用 API client。
- 通用类型和工具函数。

不适合放：

- 带 AgentTravel 业务文案的组件。
- 行程规划专用逻辑。
- 会话专用状态。

### styles

放全局样式和 token。

建议文件：

- `index.css`：全局样式入口。
- `tokens.css`：颜色、字号、圆角、阴影、间距变量。
- `tailwind.css`：Tailwind 导入和插件配置相关样式。

规则：

- 不在全局样式里写页面局部样式。
- 重复出现 3 次以上的颜色、圆角、阴影，应评估沉淀为 token 或 shared/ui。

### assets

放静态资源。

适合放：

- 图片。
- 图标。
- 品牌素材。
- 分享海报背景素材。

规则：

- 业务图片应按模块分目录。
- 不把运行时生成图片放进源码资源目录。

## 命名规范

- React 组件文件使用 PascalCase，如 `ChatPage.tsx`。
- hook 使用 `useXxx.ts`。
- API 文件使用语义化命名，如 `travelPlanningApi.ts`。
- 类型文件可使用 `types.ts` 或 `model/types.ts`。
- 业务目录使用小写短横线，如 `travel-planning`。

## 导入边界

推荐依赖方向：

```text
app -> pages -> features -> shared
```

规则：

- `shared` 不依赖 `features`。
- `features` 不依赖 `pages`。
- `pages` 不写底层工具和 API 细节。
- `app` 不写业务流程。

## API 组织规则

当前 `lib/gemini.ts` 是 AI Studio 原型直连方式。后续接入 TravelPlanner 后端时，目标结构应为：

```text
features/travel-planning/api/
  travelPlanningApi.ts
  travelPlanningTypes.ts
shared/api/
  httpClient.ts
  apiError.ts
```

规则：

- 页面组件不能直接调用 `fetch`、`axios`、模型 SDK 或后端 endpoint。
- API request / response 类型必须和后端接口文档保持一致。
- 新增或修改接口时，必须评估是否更新 `docs/yeqian/api/` 和前端文档。

## 状态组织规则

当前状态主要在 `App.tsx` 内。后续建议拆分：

- 会话状态：`features/chat-session/model`。
- 用户偏好状态：`features/user-preferences/model`。
- 行程生成状态：`features/travel-planning/model`。
- localStorage 封装：`shared/storage`。

规则：

- 简单状态优先使用 React hook。
- 不为了小规模状态提前引入全局状态库。
- 如果未来引入状态库，必须先有明确痛点和迁移计划。

## 迁移原则

- 每次只迁移一个清晰边界。
- 保持行为不变。
- 不顺手重写 UI。
- 不顺手接后端。
- 不顺手新增依赖。
- 每次迁移后确认 `frontend/src` 能正常构建或至少通过类型检查。
