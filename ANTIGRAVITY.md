# ANTIGRAVITY.md — GameNest UI/UX Agent Instructions

## 1. Project Identity

**Project:** GameNest  
**Type:** Gaming Community Platform

GameNest is a community website for gamers.

Core ideas:

```text
GameNest
│
├── Games
├── Questions & Answers
├── Find Players (LFG)
├── Teams / Community
└── User Accounts
```

The project currently has two application roles:

```text
USER
ADMIN
```

There is currently **NO MODERATOR role**.

Do not introduce additional roles unless the project owner explicitly requests it.

---

## 2. Role of Antigravity

Antigravity is responsible for:

- UI/UX design
- Frontend implementation
- JSP presentation
- HTML
- CSS
- JavaScript
- Frontend assets
- Browser-based UI testing
- Responsive behavior
- Accessibility
- Visual consistency

Antigravity is NOT responsible for:

- Backend architecture
- Business logic
- Database design
- SQL
- DAO implementation
- Service implementation
- Authentication implementation
- Authorization implementation
- Database permissions
- Security policies outside the frontend
- Creating new backend APIs unless explicitly requested

Primary rule:

> **Antigravity improves how GameNest looks and behaves without changing how the backend works.**

---

## 3. Backend / UI Agent Separation

The project may use another coding agent, such as Claude Code, for backend/database/business implementation.

Keep responsibilities separated:

```text
                    GameNest
                       │
           ┌───────────┴───────────┐
           │                       │
      Backend Agent           Antigravity
           │                       │
           ▼                       ▼
   Java / Servlet             UI / UX
   Service / DAO              JSP / CSS / JS
   SQL Server                 Browser
   Business Rules             Visual Design
```

Do not modify backend files merely to make a visual feature easier.

If a UI requirement appears to require backend changes:

1. Inspect the existing contract.
2. Determine whether the UI can work with the existing route/data.
3. If not, report the backend requirement.
4. Do not silently alter backend behavior.

---

## 4. Read Before Editing

Before changing any UI:

1. Read this file.
2. Inspect the existing project structure.
3. Inspect the target JSP.
4. Inspect related CSS.
5. Inspect related JavaScript.
6. Inspect existing assets.
7. Inspect routes/hrefs already used by the page.
8. Open the current page in the browser when possible.
9. Understand what is already working.

Never immediately replace an existing page without first understanding it.

---

## 5. Do Not Break Existing Contracts

Existing backend routes, forms, links, and authentication flows are contracts.

Do not change:

- servlet mappings
- URLs
- form action URLs
- request parameters
- session attributes
- authentication behavior
- authorization behavior
- database fields

unless the project owner explicitly asks for it.

Examples:

```text
Login
Register
Logout
Games
Questions
Answers
Admin Dashboard
```

must continue to point to the routes already implemented.

---

## 6. Current Application Roles

Only these application roles exist:

```text
USER
ADMIN
```

### USER UI

Normal public/community experience.

### ADMIN UI

Administrative experience under:

```text
/admin/*
```

Admin pages should look like part of the same GameNest brand, but should have a distinct administration layout.

Do not create a Moderator UI.

---

## 7. Public GameNest Design Direction

The current visual direction is:

- Dark theme
- Gaming-oriented
- Modern
- Premium
- Community-focused
- Blue/cyan/purple accents
- Subtle neon glow
- High contrast typography
- Rounded cards
- Controlled shadows
- Clean spacing
- Minimal but expressive animation

GameNest should feel like:

> **A modern gaming community platform.**

It should NOT look like:

- an e-commerce store
- a game launcher
- a banking dashboard
- a generic corporate admin template
- an overly futuristic sci-fi interface

---

## 8. Brand Voice in UI

Use concise Vietnamese UI text unless the existing page is intentionally English.

Preferred examples:

```text
Khám phá Games
Hỏi đáp
Tìm đội (LFG)
Cộng đồng
Tham gia ngay
Đăng nhập
Đăng ký
Dashboard
Tài khoản
Đăng xuất
```

Do not randomly translate existing project labels.

Keep naming consistent across the website.

---

## 9. Public Home Page

The public homepage is currently:

```text
index.jsp
```

The homepage should communicate:

1. Game discovery
2. Community
3. Finding teammates
4. Questions / discussion
5. Joining GameNest

The current hero uses a gaming visual background with moving game covers.

---

## 10. Hero Design Pattern

A preferred GameNest hero pattern is:

### Multi-row Infinite Game Cover Marquee

Multiple rows of game covers can move horizontally.

Example:

```text
Row 1:
[Game][Game][Game][Game][Game]
← ← ← ← ← ← ←

Row 2:
[Game][Game][Game][Game][Game]
→ → → → → → →

Row 3:
[Game][Game][Game][Game][Game]
← ← ← ← ← ← ←
```

Requirements:

- seamless loop
- no visible gaps
- no sudden reset
- smooth movement
- different speeds can be used
- different rows should preferably move in opposite directions

The marquee is decorative and must not overpower the main hero content.

---

## 11. Hero Content

Current brand slogan:

> **Kết Nối. Chinh Phục. Tỏa Sáng.**

Keep this slogan unless the project owner explicitly requests a change.

Hero content should remain the focal point even when animated game covers are present.

Use:

- dark overlays
- gradients
- controlled blur
- edge fading
- opacity
- visual hierarchy

to preserve readability.

---

## 12. UI Hierarchy

Always establish a clear hierarchy:

```text
Primary heading
      ↓
Supporting text
      ↓
Primary CTA
      ↓
Secondary CTA
      ↓
Secondary information
```

Do not make every element glow, animate, or use large typography.

A visual hierarchy must exist.

---

## 13. Cards and Components

Reusable UI components should have consistent:

- border radius
- spacing
- typography
- hover states
- shadows
- focus states
- icon sizing

Common GameNest components:

```text
Game Card
Question Card
Answer Block
LFG Card
Team Card
Stat Card
Navigation Item
Button
Badge
Search Box
Filter
Modal
Toast
```

Do not create five visually different versions of the same component unless the use case requires it.

---

## 14. Games UI

Games are a major part of GameNest.

Typical user flow:

```text
Games
  ↓
Game List
  ↓
Game Detail
  ├── Questions
  ├── Find Players
  └── Teams
```

Game cards should prioritize:

- game cover
- game title
- concise information
- clear interaction
- recognizable visual identity

Avoid overcrowding cards with too much metadata.

---

## 15. Questions & Answers UI

Questions and Answers are community content.

Prioritize:

- readability
- author visibility
- timestamps
- answer count
- clear hierarchy
- accepted answer visibility
- action discoverability

Do not make long text difficult to read with tiny font sizes or excessive card decoration.

For user-generated content:

- preserve readable line length
- wrap long text
- prevent layout overflow
- visually distinguish content from metadata

---

## 16. LFG UI

LFG means:

**Looking For Group / Tìm người chơi**

An LFG card may show:

```text
Game
Mode
Rank
Region
Players
Required slots
Start time
Status
```

The UI should make it easy to answer:

> "Đây có phải nhóm mình muốn tham gia không?"

Do not bury important information under decorative effects.

---

## 17. Admin UI

Admin pages live under:

```text
/admin/*
```

The Admin interface should use a reusable layout:

```text
┌─────────────────────────────────────────┐
│ Header                                  │
├──────────────┬──────────────────────────┤
│ Sidebar      │ Main Content             │
│              │                          │
│ Dashboard    │                          │
│ Accounts     │                          │
│ Games        │                          │
│ Questions    │                          │
│ Answers      │                          │
│ ...          │                          │
└──────────────┴──────────────────────────┘
```

The Sidebar should be shared across Admin pages.

Do not create separate unrelated sidebar designs for each module.

---

## 18. Admin Dashboard

The Admin Dashboard is a management overview, not a public homepage.

It may contain:

- statistic cards
- system summaries
- status summaries
- quick navigation

Current known statistics include:

```text
Accounts
Games
Questions
Answers
Active / Inactive Games
Active / Deleted Questions
Active / Deleted Answers
```

Do not invent fake numbers.

If data is not supplied by the backend, do not hardcode business statistics.

---

## 19. Admin Navigation

Admin navigation should be clear and persistent.

Potential sections:

```text
Dashboard
Accounts
Games
Questions
Answers
```

Only link to routes that actually exist.

Do not invent backend pages to make a UI look complete.

If a future module is shown visually, mark it as disabled or planned instead of creating fake functionality.

---

## 20. Responsiveness

Every important page must work on:

### Desktop

- wide content
- comfortable spacing
- full navigation

### Tablet

- reduced spacing
- adaptable columns
- usable navigation

### Mobile

- no horizontal page overflow
- readable typography
- touch-friendly controls
- collapsible/sidebar drawer if appropriate
- buttons must remain usable
- forms must fit the viewport

Do not solve responsive issues by simply shrinking everything.

---

## 21. Accessibility

UI must account for:

- sufficient text/background contrast
- visible keyboard focus
- clear button labels
- meaningful link text
- form labels
- readable font sizes
- accessible interactive controls
- `prefers-reduced-motion`

Do not make critical information dependent only on color.

Example:

Bad:

```text
Green = Active
Red = Inactive
```

Prefer:

```text
● ACTIVE
● INACTIVE
```

with color as an additional visual cue.

---

## 22. Motion and Animation

Animation should support the interface, not distract from it.

Good uses:

- hover transitions
- subtle card movement
- marquee
- fade-in
- modal transitions
- menu transitions
- button feedback

Avoid:

- constant flashing
- aggressive scaling
- excessive particle effects
- rapid movement behind important text
- animation that causes layout jumps

Respect:

```css
@media (prefers-reduced-motion: reduce)
```

where appropriate.

---

## 23. Performance

Prefer simple browser-native technologies:

- HTML
- CSS
- Vanilla JavaScript
- JSP

Do not introduce a frontend framework unless explicitly requested.

Avoid unnecessary external UI libraries.

Prefer:

```text
transform
opacity
CSS animation
```

for animation.

Avoid repeated expensive DOM operations.

Do not continuously perform heavy JavaScript work for decorative animation if CSS can do it.

---

## 24. Assets

Use existing project assets when available.

Before introducing new assets:

1. Inspect the existing asset structure.
2. Reuse assets if suitable.
3. Keep asset naming consistent.
4. Avoid duplicate copies of the same image.

Do not expose secret keys or private storage credentials inside frontend files.

Do not upload assets to third-party services unless explicitly requested.

---

## 25. Browser Testing

After significant UI changes:

1. Start/use the existing local server.
2. Open the affected route.
3. Inspect the actual browser result.
4. Check browser console.
5. Test the main interaction.
6. Test responsive dimensions where possible.
7. Check for:
   - overflow
   - broken images
   - broken links
   - layout shifts
   - unreadable text
   - animation glitches

Do not assume the UI works merely because the JSP/CSS compiles.

---

## 26. Browser-First Visual Review

For UI tasks, browser output is the source of truth.

Do not stop at source-code inspection.

When possible:

```text
Edit
 ↓
Run
 ↓
Open Browser
 ↓
Inspect
 ↓
Adjust
 ↓
Retest
```

Take into account the actual viewport and the real content.

---

## 27. Scope Control

When asked to modify one page or component:

- modify the requested scope
- avoid unrelated redesign
- do not rewrite the whole website
- do not change backend architecture
- do not alter unrelated routes
- do not change database logic

Example:

If asked to improve the homepage hero:

```text
Allowed:
index.jsp
Hero CSS
Hero JS
Hero assets

Not automatically allowed:
Servlet
DAO
Service
Model
SQL
Authentication
Admin logic
```

---

## 28. Do Not Invent Business Logic

UI should represent existing business rules.

Do not assume:

- users can delete things
- admins can edit anything
- games can be deleted
- accounts can be permanently removed
- a feature exists because a menu item looks appropriate

If business behavior is unclear:

> Ask for clarification or report the dependency instead of inventing the rule.

---

## 29. Preserve Security Boundaries

Frontend conditions are not security.

Never treat:

```text
display:none
hidden button
disabled link
JavaScript check
```

as authorization.

If an Admin-only action needs to be hidden from USER, hiding it improves UX.

Actual permission must still be enforced server-side by the backend.

Do not attempt to implement backend authorization in JavaScript.

---

## 30. Error and Empty States

Design UI for:

- empty game list
- no search results
- no questions
- no answers
- deleted content
- unavailable content
- loading state where applicable
- server error
- access denied
- not found

Do not make an empty page look broken.

Example:

```text
Không tìm thấy game phù hợp.

[ Khám phá tất cả Games ]
```

---

## 31. Forms

Forms should provide:

- clear labels
- sensible grouping
- validation feedback
- focus states
- error messages
- disabled/loading states where appropriate

Do not rely only on HTML `required`.

Backend remains responsible for final validation.

Frontend validation is a UX feature, not a security boundary.

---

## 32. Search and Filters

Search/filter UI should make active filters obvious.

Prefer:

```text
[ Search game...          ] 🔍
```

or:

```text
Game: [ All ▼ ]
Status: [ Active ▼ ]
```

Avoid overly complicated filter panels unless the data requires them.

---

## 33. Typography

Typography should prioritize Vietnamese readability.

Use the project's existing font when appropriate.

Do not introduce many font families.

Recommended hierarchy:

```text
H1 — page title
H2 — section title
H3 — component title
Body — readable content
Caption — metadata
```

Do not overuse uppercase text.

---

## 34. Color System

GameNest's visual identity should generally use:

```text
Background:
very dark / near-black

Primary accent:
blue / cyan

Secondary accent:
purple / violet

Success:
green

Warning:
amber/yellow

Danger:
red
```

Do not make every component use all colors.

Accent colors should communicate hierarchy and interaction.

---

## 35. Responsive Navigation

Public navigation should remain simple.

Public navigation currently centers around:

```text
Games
Hỏi Đáp
Tìm Đội (LFG)
Cộng Đồng
```

Right side may include:

```text
Đăng nhập
Đăng ký
```

When authenticated, respect the application's existing account/login state.

Do not invent authentication behavior in the UI.

---

## 36. Admin vs Public Visual Identity

Public:

- community
- energetic
- gaming
- expressive

Admin:

- focused
- structured
- information-dense
- professional
- still recognizable as GameNest

Admin does not need to look identical to the public homepage.

---

## 37. Reusable Layout Strategy

Prefer reusable JSP fragments/includes for repeated UI when the project structure supports them.

Examples:

```text
Header
Navbar
Footer
Admin Sidebar
Admin Header
```

Do not copy/paste the same navigation into many JSP files if it can be shared cleanly.

When modifying a shared component, check which pages use it before changing behavior.

---

## 38. File Safety

Before editing a file:

- inspect current content
- preserve working behavior
- avoid accidental deletion
- avoid replacing unrelated content

Do not delete files merely because they look unused without checking references.

---

## 39. Final UI Review Checklist

Before reporting completion, check:

- [ ] Page renders correctly
- [ ] Layout is responsive
- [ ] No horizontal overflow
- [ ] No broken images
- [ ] No broken links
- [ ] Text is readable
- [ ] Buttons have hover/focus states
- [ ] Forms are usable
- [ ] Empty states are acceptable
- [ ] Animation is smooth
- [ ] Reduced-motion behavior is considered
- [ ] Existing routes remain unchanged
- [ ] Backend files were not changed unnecessarily
- [ ] Browser console has no new errors

---

## 40. Final Principle

GameNest UI should follow:

> **Simple where it needs to be simple, expressive where it helps the gaming experience, and always consistent with the real business system.**

The UI must make GameNest easier to understand and use.

Do not add visual complexity merely because it looks impressive.

Always prioritize:

```text
Usability
   ↓
Clarity
   ↓
Consistency
   ↓
Performance
   ↓
Visual polish
```
