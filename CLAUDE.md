# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A retro 1990s GeoCities-style website for the "Old Border Shandalar" mod — a Magic: The Gathering community project that converts the open-source Forge engine's Adventure Mode to use only old-border cards (1993–2003). The actual mod development lives at [github.com/vanja-ivancevic/forge](https://github.com/vanja-ivancevic/forge); this repo is the promotional fan site.

## Development

This is a static HTML/CSS site with no build system, no bundler, no JavaScript frameworks.

- **Serve locally**: Open any `.html` file in `docs/` directly in a browser, or use `python3 -m http.server -d docs`
- **Deploy**: Push to `master` — GitHub Pages auto-publishes from the `docs/` folder
- **No tests, linting, or CI** exist

## Site Structure

```
docs/                    # GitHub Pages root — all site content lives here
├── index.html           # Home page (hero, marquee, downloads)
├── features.html        # 10-feature grid (2-column table)
├── howtoplay.html       # 4-step setup guide
├── guide.html           # External walkthrough link + testimonials
├── changelog.html       # Monthly changelog sourced from Forge repo commits
├── community.html       # Discord/Reddit/GitHub links, guestbook, credits
├── style.css            # Single shared stylesheet
├── shared.js            # Injected sidebar, mobile nav, ribbon, live visitor counter
├── clippy.js            # MS Agent "Genie" assistant (offers modern site link)
├── sparkles.js          # Cursor sparkle effect
├── favicon.svg          # Site favicon
├── gifs/                # Animated GIFs (dividers, decorations, 88x31 buttons)
└── modern/              # Modern/clean redesign (same pages, different aesthetic)
    ├── index.html
    ├── features.html
    ├── howtoplay.html
    ├── guide.html
    ├── changelog.html
    ├── community.html
    └── modern.css        # Modern-only stylesheet
```

## Two-Site Architecture

The retro (`docs/`) and modern (`docs/modern/`) sites mirror each other page-for-page. Each version links to the corresponding page on the other version. When adding or renaming a page, update both versions and their cross-links.

- **Retro pages**: HTML 4.01 Transitional, table layout, `style.css`, include `shared.js` + `clippy.js`
- **Modern pages**: HTML5, semantic layout, `modern.css`, no shared.js (standalone nav per page)
- **Cross-linking**: `shared.js` generates `modern/<page>` links; modern pages link back to `../<page>`

## Architecture & Conventions

**Layout (retro)**: Table-based (`<table class="layout-table">`) with left sidebar navigation (`<td class="nav-sidebar">`) and main content area (`<td class="main-content">`). Sidebar, mobile nav, ribbon, and visitor counter are injected by `shared.js` synchronously — pages just include the script and provide `<td class="main-content">`.

**Layout (modern)**: Semantic HTML5 with flexbox/grid, `<nav>` with hamburger toggle, `modern.css` only. Uses `data-theme="modern"` on `<body>` (Genie checks this to skip modern pages).

**DOCTYPE**: HTML 4.01 Transitional (retro). HTML5 (modern).

**Navigation (retro)**: `shared.js` auto-generates sidebar nav and mobile nav. Active page is detected from `location.pathname`. Each page also gets a "Modern Site" link pointing to `modern/<same-page>`.

**Navigation (modern)**: Each modern page has its own `<nav>` with a hamburger toggle and a "Retro Site" link back to the retro version.

**Styling**: Single `style.css` shared across retro pages. CSS animations (`@keyframes blinker`, `glow`, `rainbow`) provide effects. Gradient backgrounds simulate a starfield.

**Color palette**:
- Background: navy `#000033` / `#0a001a`
- Text: white `#FFFFFF`, headings: yellow `#FFFF00`
- Links: cyan `#00CCFF`, visited: purple `#CC99FF`, hover: yellow
- Accents: green `#00FF00`, orange `#FF6600`
- Borders/chrome: purple `#663399` / `#7744AA`

**Fonts**: Times New Roman (body), Comic Sans MS (marquee, nav title), Courier New (counter digits, step numbers).

## Design Philosophy

The retro site is intentionally designed to look like an authentic mid-to-late 1990s personal homepage. When making changes:

- **Maintain the retro aesthetic** — no modern UI patterns (flexbox, grid, smooth transitions, rounded corners)
- **Use period-appropriate techniques**: `<TABLE>` for layout, `<FONT>` for inline styling, `<CENTER>` for alignment, animated GIFs for decoration
- **Fire bar GIFs** (`gifs/fire_bar.gif`) serve as section dividers throughout the site
- **88x31 pixel buttons** in the footer are classic 90s web badges
- The visitor counter is **live** — uses CounterAPI.dev (`api.counterapi.dev/v1/old-border-shandalar/visits`). Increments once per session via `sessionStorage`.
- **Genie assistant** (`clippy.js`): MS Agent loaded from clippyjs CDN. Offers to redirect users to the modern site. Dismissed state stored in `sessionStorage`. Skips modern pages via `data-theme` check.

## When Adding New Pages

**Retro page:**
1. Copy an existing retro page — keep only the `<td class="main-content">` area; the sidebar/nav/ribbon come from `shared.js`
2. Include `<script src="shared.js"></script>` before body content and `<script type="module" src="clippy.js"></script>` before `</body>`
3. Add nav link entry in `shared.js` (both sidebar and mobile nav)
4. Link `style.css` in the `<head>`
5. Match the HTML 4.01 Transitional DOCTYPE

**Modern page:**
1. Copy an existing modern page's full structure
2. Add the new page link to the `<nav>` in **every** modern page
3. Add corresponding entry in retro `shared.js` nav
4. Link `modern.css`, use `<body data-theme="modern">`
