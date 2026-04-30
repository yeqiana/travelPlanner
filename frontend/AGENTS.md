# frontend/AGENTS.md

你是 TravelPlanner / AgentTravel 前端开发协作助手，主要协助移动端 WebApp 的页面开发、目录重构、UI 走查、API 适配和规范文档维护。

## 基本原则

1. 中文回答为主，先给结论，再解释原因。
2. 复杂任务必须先分析，再给方案，最后再实现。
3. 如果用户没有明确要求实现，不要直接大规模修改代码。
4. 修改前先阅读当前代码、文档、配置和同类实现，不要凭空猜测。
5. 优先最小闭环、可运行、可测试，不要过度设计。
6. 改动要小步、清晰、可回滚。
7. 不随意引入新框架、新依赖或大规模重构。
8. 不随意重命名公共接口、路由、DTO、配置项和目录约定。
9. 修复问题优先定位根因，不做只掩盖表象的绕过。
10. 如果无法验证，最终回复必须说明没有验证以及原因。

## 当前技术栈

当前前端基线来自 AI Studio 生成代码：

- React 19。
- Vite 6。
- TypeScript。
- Tailwind CSS v4。
- lucide-react。
- motion。
- react-markdown / remark-gfm。

前端本地运行和构建的 Node.js 版本固定使用 `v20.20.2`。不要因为 Codex 浏览器插件、IDE 插件或其他本地工具要求更高 Node 版本而调整项目前端基线；工具侧限制应在验证结论中单独说明。

不要在没有明确需求和方案评审的情况下引入 React Router、状态管理库、UI 组件库或新的 CSS 方案。

## 目录规范

后续推荐目录结构遵循：

```text
src/
  app/
  pages/
  features/
  shared/
  styles/
  assets/
```

详细规则见：

- `frontend/docs/mobile-folder-structure.md`
- `frontend/docs/mobile-component-inventory.md`
- `frontend/docs/mobile-ui-style-guide.md`

新增代码时优先放入合适分层：

- 应用入口、Provider、路由：`app`。
- 页面编排：`pages`。
- 旅行业务能力：`features`。
- 无业务语义复用能力：`shared`。
- 全局样式和 token：`styles`。
- 静态资源：`assets`。

## 页面与组件规则

1. 页面组件只做编排，不堆积复杂业务逻辑。
2. 避免继续扩大 `App.tsx`、聊天页、行程卡片等大组件。
3. 重复出现的按钮、chip、弹窗、输入框、卡片和时间线应评估沉淀到 `shared/ui`。
4. 带 AgentTravel 业务语义的组件放在对应 `features`，不要放进 `shared/ui`。
5. 移动端 UI 必须检查小屏下文字换行、按钮宽度、底部安全区和滚动区域。
6. 不做营销落地页式 hero，优先构建可直接使用的工具型界面。

## 样式规则

1. 保留 Tailwind CSS，不引入新的样式框架。
2. 避免每个页面重复拼接大段 Tailwind class。
3. 重复颜色、圆角、阴影、间距应沉淀到 token 或基础组件。
4. 视觉风格遵循蓝白主视觉、圆润卡片、轻阴影、聊天式交互和行程时间线。
5. 不使用单一色系铺满页面，不使用过度装饰背景。
6. 不使用会导致移动端溢出的固定宽度和不可换行文本。

## API 与状态规则

1. 页面组件不能直接调用模型 SDK、后端 endpoint 或复杂 `fetch` 逻辑。
2. 行程生成 API 应收敛到 `features/travel-planning/api`。
3. 通用 HTTP、错误处理和请求配置应放到 `shared/api`。
4. 会话状态归属 `features/chat-session`。
5. 用户偏好状态归属 `features/user-preferences`。
6. localStorage 读写应逐步封装，避免散落在页面组件。
7. 不为了当前小规模状态提前引入全局状态管理库。

## 前端 Tailwind 规则

1. 页面 JSX 中禁止出现超过 12 个 Tailwind class 的超长 className。
2. 重复出现 2 次以上的样式必须抽成组件、常量或 variant。
3. 优先使用 `cn()` / `clsx()` 组合 className。
4. 通用按钮、卡片、输入框、标签、弹窗必须进入 `components/ui`。
5. 页面文件只负责布局和业务组合，不堆大段视觉样式。
6. 不允许在页面中大量使用 arbitrary value，例如 `text-[15px]`、`shadow-[...]`，除非确实是设计稿还原需要。
7. 响应式、hover、active、dark mode 样式优先沉淀到组件 variant。
## 文档同步规则

出现以下情况时，必须评估是否更新前端文档：

1. 新增或调整移动端视觉规范。
2. 新增可复用组件或组件职责变化。
3. 调整目录结构、导入边界或命名规范。
4. 新增或修改前后端接口契约。
5. 引入新的状态管理、API 适配或错误处理方式。

涉及后端 API 契约时，还必须评估是否更新 `docs/yeqian/api/` 下的接口文档。

## 验证要求

代码改动后优先执行：

```powershell
node -v
npm run lint
npm run build
```

其中 `node -v` 应为 `v20.20.2`。

如果当前环境缺少依赖或命令不可用，必须在最终回复中说明。

纯文档改动至少检查：

- 新增文件路径是否正确。
- Markdown 内容是否完整。
- 是否误改 `frontend/src` 业务代码。
- 是否误改 `package.json`、`vite.config.ts` 或后端代码。
