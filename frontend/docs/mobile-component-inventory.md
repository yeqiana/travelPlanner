# AgentTravel 移动端组件盘点

本文档盘点当前 `frontend/src` 中的页面和组件职责，用于后续目录重构、组件复用和 UI 走查。

## 当前结构概览

当前前端源码结构较扁平：

```text
src/
  App.tsx
  main.tsx
  index.css
  types.ts
  components/
  lib/
```

当前没有显式路由层，也没有独立页面目录。`App.tsx` 通过状态切换首页表单和聊天视图。

## 组件职责盘点

| 文件 | 当前职责 | 主要问题 | 后续建议归属 |
| --- | --- | --- | --- |
| `App.tsx` | 应用壳、顶部导航、侧边抽屉、会话状态、设置弹窗、搜索弹窗、首页/聊天切换 | 职责过多，是最大的大组件；会话状态和 UI 交织 | `app/App.tsx` + `pages` + `features/chat-session` |
| `components/HomeForm.tsx` | 首页旅行需求表单 | 表单状态和样式集中；chip、stepper 可复用 | `pages/home` 或 `features/travel-planning/components` |
| `components/ChatView.tsx` | 聊天消息列表、底部输入、推荐追问、行程操作按钮 | 聊天 UI、输入框、行程操作混在一起 | `pages/chat` + `features/chat-session/components` |
| `components/InteractiveItinerary.tsx` | 行程卡片、按天时间线、估价、交通提示、分享入口 | 行程展示逻辑较重，卡片和 timeline 可拆 | `features/travel-planning/components` |
| `components/SettingsModal.tsx` | 用户旅行偏好设置 | 表单选项写死在组件内 | `features/user-preferences/components` |
| `components/SessionSearchModal.tsx` | 历史会话搜索 | 搜索逻辑和展示耦合 | `features/chat-session/components` |
| `components/RouteMapModal.tsx` | 一图流路线弹窗 | 分享图 UI 较独立，但样式复杂 | `features/itinerary-share/components` |
| `components/LocationShareModal.tsx` | 单地点分享弹窗 | 分享生成相关 | `features/itinerary-share/components` |
| `components/ItineraryShareModal.tsx` | 整体行程分享弹窗 | 分享生成相关 | `features/itinerary-share/components` |
| `lib/gemini.ts` | 组装 Prompt、调用 Gemini、解析行程 JSON | 前端直连模型；API Key 暴露风险；与后端契约未隔离 | `features/travel-planning/api` 临时适配，后续替换为后端 API |
| `lib/export.ts` | 导出 ICS 日历 | 可保留为独立工具，但需要归类 | `features/travel-planning/lib` 或 `shared/lib` |
| `types.ts` | 行程、消息、会话、偏好类型 | 类型全部集中，后续会变大 | 按 feature 拆到 `model`，公共类型放 `shared/types` |

## 大组件与大文件风险

当前文件行数靠前：

- `App.tsx`：约 457 行。
- `RouteMapModal.tsx`：约 153 行。
- `InteractiveItinerary.tsx`：约 150 行。
- `ChatView.tsx`：约 140 行。
- `HomeForm.tsx`：约 140 行。

风险：

- 业务状态、UI 状态和布局混杂，修改一个功能容易影响多个区域。
- 弹窗、按钮、chip、卡片样式重复，后续统一视觉成本高。
- 页面级组件没有和业务 feature 分离，难以接入真实后端 API。
- 类型、API 和状态没有边界，后续多人协作容易互相踩踏。

## 复用不足的组件模式

建议后续优先沉淀这些基础组件：

- `Button`：主按钮、次按钮、图标按钮、危险按钮。
- `Chip`：偏好选择、推荐追问、状态标签。
- `Modal`：居中弹窗基础结构。
- `Sheet`：侧边抽屉或移动端底部弹层。
- `Textarea` / `TextInput`：统一输入背景、聚焦态和字号。
- `Stepper`：天数选择。
- `Timeline`：行程时间线。
- `EmptyState`：空历史、无搜索结果。
- `IconBadge`：蓝色图标块、头像、日期标记。

基础组件必须放在 `shared/ui`，不能带具体旅行业务语义。带业务语义的组合组件放在对应 `features`。

## API 与状态问题

当前 API 调用集中在 `lib/gemini.ts`，虽然没有散落在多个页面中，但调用入口由 `App.tsx` 直接触发，仍然缺少清晰边界。

后续建议：

- 页面组件不直接调用模型 SDK 或后端 endpoint。
- 行程生成 API 统一从 `features/travel-planning/api` 导出。
- 会话持久化从 `App.tsx` 移到 `features/chat-session/model` 或 `shared/storage`。
- 用户偏好持久化从 `App.tsx` 移到 `features/user-preferences/model`。

## 样式问题

当前样式基本都写在 JSX 的 Tailwind class 中，且包含不少任意值：

- `rounded-[20px]`
- `shadow-[0_4px_0...]`
- `bg-[#f4f4f5]`
- `z-[9999]`

后续建议：

- 先沉淀 token，再迁移重复 class。
- 对高频组件使用 `shared/ui` 包装样式。
- 保留 Tailwind，但避免每个页面重新拼一套按钮、输入框和卡片。

## 后续拆分顺序建议

1. 先抽 `App.tsx` 的应用壳、侧边抽屉、会话状态。
2. 再拆 `ChatView.tsx` 的消息列表、底部输入、推荐追问。
3. 再拆 `InteractiveItinerary.tsx` 的行程头部、Day 分组、Activity 卡片、Tips。
4. 最后整理分享弹窗和导出工具。

每一步都应保持行为不变，并在改动后做移动端 UI 走查。
