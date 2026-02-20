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
├── community.html       # Discord/Reddit/GitHub links, guestbook, credits
├── style.css            # Single shared stylesheet
└── gifs/                # 19 animated GIFs (dividers, decorations, 88x31 buttons)
```

## Architecture & Conventions

**Layout**: Table-based (`<table class="layout-table">`) with left sidebar navigation (`<td class="nav-sidebar">`) and main content area (`<td class="main-content">`). Every page replicates this same table structure — there is no templating.

**DOCTYPE**: HTML 4.01 Transitional. Uses a mix of old-school HTML attributes (`<FONT>`, `<CENTER>`, `<MARQUEE>`, `bgcolor`, `border`) alongside CSS classes.

**Navigation**: Each page has identical sidebar nav with `.nav-link` buttons. The current page gets `.nav-link.active`. A separate `.mobile-nav` div is hidden on desktop and shown via `@media (max-width: 700px)`.

**Styling**: Single `style.css` shared across all pages. CSS animations (`@keyframes blinker`, `glow`, `rainbow`) provide effects. Gradient backgrounds simulate a starfield.

**Color palette**:
- Background: navy `#000033` / `#0a001a`
- Text: white `#FFFFFF`, headings: yellow `#FFFF00`
- Links: cyan `#00CCFF`, visited: purple `#CC99FF`, hover: yellow
- Accents: green `#00FF00`, orange `#FF6600`
- Borders/chrome: purple `#663399` / `#7744AA`

**Fonts**: Times New Roman (body), Comic Sans MS (marquee, nav title), Courier New (counter digits, step numbers).

## Design Philosophy

The site is intentionally designed to look like an authentic mid-to-late 1990s personal homepage. When making changes:

- **Maintain the retro aesthetic** — no modern UI patterns (flexbox, grid, smooth transitions, rounded corners)
- **Use period-appropriate techniques**: `<TABLE>` for layout, `<FONT>` for inline styling, `<CENTER>` for alignment, animated GIFs for decoration
- **Fire bar GIFs** (`gifs/fire_bar.gif`) serve as section dividers throughout the site
- **88x31 pixel buttons** in the footer are classic 90s web badges
- The fake visitor counter is decorative (hardcoded digits, not functional)

## When Adding New Pages

1. Copy the full table layout structure from an existing page (sidebar + main content)
2. Update the `.nav-link.active` class to the correct page
3. Update both the sidebar nav and the `.mobile-nav` div
4. Link `style.css` in the `<head>`
5. Match the HTML 4.01 Transitional DOCTYPE and meta tags
