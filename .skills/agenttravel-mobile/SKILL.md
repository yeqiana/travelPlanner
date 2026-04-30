---
name: agenttravel-mobile
description: AgentTravel / TravelPlanner mobile frontend engineering rules for React, Vite, TypeScript, and Tailwind CSS v4. Use when Codex works on AgentTravel mobile WebApp UI, Tailwind cleanup, component extraction, page splitting, frontend folder structure, design tokens, responsive mobile review, or API boundary cleanup.
---

# AgentTravel Mobile

## When to use

Use this skill for AgentTravel / TravelPlanner frontend tasks involving:

- Mobile WebApp pages, components, dialogs, drawers, bottom inputs, itinerary cards, timelines, or share posters.
- React + Vite + TypeScript + Tailwind CSS v4 implementation or refactoring.
- Tailwind `className` cleanup, duplicated style extraction, or design token work.
- Frontend folder structure, page responsibility, feature boundaries, or API boundary cleanup.
- Mobile UI validation for 320 / 375 / 430 width screens.

Do not use this skill for backend-only work, login, payment, order flows, database changes, or unrelated documentation.

## Frontend architecture rules

- Keep the frontend structure aligned with `app / pages / features / shared / styles / assets`.
- Put application shell, app-level providers, route entry, and root composition in `app`.
- Put page-level composition in `pages`; pages may connect feature modules but must not become UI style dumps.
- Put business capabilities in `features`, such as `travel-planning`, `chat-session`, and `user-preferences`.
- Put reusable visual primitives in `components/ui`.
- Put shared utilities, types, API helpers, storage helpers, and non-visual helpers in `shared`.
- Put global CSS, design tokens, theme variables, and density rules in `styles`.
- Keep API calls out of page JSX. Travel planning APIs belong in `features/travel-planning/api`; generic HTTP helpers belong in `shared/api`.
- Do not introduce React Router, a new state library, a UI framework, or a new styling system unless the user explicitly asks and a plan explains why.

## Tailwind className rules

- Do not allow oversized Tailwind strings in page JSX.
- If one `className` has more than 12 Tailwind classes, extract it into a component, variant, helper constant, or `cn()` composition.
- If conditional styling appears in JSX, prefer `cn()` or a local variant map over nested template strings.
- Do not copy long Tailwind strings between files.
- Do not use arbitrary values only to make something look nicer. Avoid casual use of `text-[...]`, `rounded-[...]`, `shadow-[...]`, `bg-[#...]`, `h-[...]`, and `w-[...]`.
- Arbitrary values are allowed only when preserving an existing measured design detail, fixing a concrete mobile layout bug, or representing safe-area / viewport constraints.
- Keep hover, active, disabled, selected, loading, and responsive styles inside reusable component variants when they repeat.
- Prefer readable Tailwind order: layout, size, spacing, typography, color, border, shadow, state, responsive.

## Component extraction rules

- If the same button, icon button, card, input, textarea, chip, badge, modal, drawer, bottom bar, or timeline style appears more than twice, extract a reusable component or variant.
- Put generic UI components in `components/ui`, for example `Button`, `IconButton`, `Card`, `TextInput`, `Textarea`, `Chip`, `Badge`, `Modal`, `Sheet`, and `Timeline`.
- Keep AgentTravel-specific components in `features/*/components`, for example itinerary cards, session sidebar, travel preference form, and route/share modals.
- A generic UI component must not import feature types such as `Itinerary`, `ChatSession`, or `TravelPreferences`.
- A feature component may compose `components/ui` primitives and own business-specific wording, icons, and data mapping.
- When extracting, preserve behavior first. Do not change copy, API flow, state shape, localStorage keys, or visual hierarchy unless the user asked.

## Design token rules

- Promote repeated colors, typography, radius, shadow, spacing, z-index, density, and safe-area values into tokens or component variants.
- Use tokens for AgentTravel brand basics: primary blue, neutral text, muted text, surface white, soft gray background, warning/orange tips, success, danger, and overlay.
- Use tokens or variants for common radius sizes such as chip, button, card, modal, and full pill.
- Use tokens or variants for common shadows such as low card shadow, floating button shadow, modal shadow, and drawer shadow.
- Do not hardcode the same color, radius, or shadow in multiple page files.
- Keep mobile density explicit: compact controls for small screens, default density for normal mobile, and no oversized hero typography inside cards or tool surfaces.

## Page responsibility rules

- Page files are responsible for layout, feature composition, and data-flow connection.
- Page files must not contain large visual primitives, repeated Tailwind style recipes, API adapters, storage adapters, or model SDK calls.
- Page files may hold temporary local UI state only when it is page-specific and small.
- Move cross-page state, session behavior, user preferences, and storage behavior into the owning feature or shared helper when they grow.
- Keep page JSX shallow. If a page needs deeply nested UI sections, extract named components before adding more behavior.

## Styling responsibility rules

- `components/ui` owns reusable visual styling, size variants, intent variants, disabled states, and interaction states.
- `features/*/components` owns business layout and composes UI primitives.
- `pages/*` owns screen-level spacing and module ordering only.
- `styles/*` owns global Tailwind imports, CSS variables, theme tokens, density rules, scrollbar behavior, and safe-area helpers.
- Do not put one-off global CSS into feature files.
- Do not use inline `style` attributes unless a runtime value cannot be represented with Tailwind or CSS variables.

## Testing and validation rules

- After any frontend code refactor, run these commands from `frontend`:

```powershell
npm run lint
npm run build
```

- If the task changes UI, also validate mobile behavior at 320, 375, and 430 pixel widths.
- Mobile validation must check buttons, cards, dialogs, drawers, bottom input, timeline/table content, long text wrapping, scroll areas, and safe-area padding.
- If a command cannot run because dependencies or tools are missing, state that clearly in the final response.
- For documentation-only skill changes, verify file existence, required headings, and diff scope instead of running frontend build commands.
- Final responses must state what changed, what was validated, and what was not validated.

## Refactor workflow

1. Read the current code, related docs, and similar existing components before changing anything.
2. Produce a short plan before implementation for any non-trivial frontend refactor.
3. Lock scope: preserve business behavior unless the user explicitly requests behavior changes.
4. Identify repeated UI patterns and decide whether to extract `components/ui`, feature components, tokens, or variants.
5. Refactor in small steps: first move structure, then extract components, then reduce Tailwind duplication, then validate.
6. Keep API boundaries intact: pages call feature APIs, feature APIs adapt providers or backend contracts.
7. Run the required checks and report any unverified areas.

Preferred long-term order:

```text
规范抽取 -> 轻量拆分 -> UI 基础组件 -> token 收敛 -> API 边界检查 -> 移动端走查
```

## Forbidden actions

- Do not change business behavior unless the user explicitly asks.
- Do not introduce new frameworks, UI libraries, routing libraries, state libraries, or styling systems without approval.
- Do not put Gemini SDK calls, backend endpoint calls, or complex `fetch` logic inside page components.
- Do not grow page JSX with long repeated Tailwind strings.
- Do not use arbitrary Tailwind values as decoration or taste-based tweaks.
- Do not create generic components that import AgentTravel business types.
- Do not silently rename public types, API functions, routes, DTOs, storage keys, or config keys.
- Do not fix unrelated encoding, login, payment, order, backend, or database issues as a drive-by change.
- Do not delete existing tests or docs to make checks pass.
- Do not claim validation passed without running the relevant command or explaining why it was skipped.
