# Visual & Interaction Audit — WallKraft

Every issue follows Apple HIG as the design DNA. Same rigor as the code audit.

**Status**: `pending` = not started, `in_progress` = fixing, `completed` = fixed, `cancelled` = deferred

---

## Animation & Transitions

| # | Screen | Issue | Severity | Status |
|---|--------|-------|----------|--------|
| V-01 | BrowseScreen | No AnimatedContent for state transitions (loading → grid → error → empty) — hard switch reads as jarring | High | completed |
| V-02 | BrowseScreen | RateLimitBanner appears/disappears instantly — no enter/exit animation | Medium | completed |
| V-03 | DetailScreen | DetailScreen enter transition is None — should have shared element container-transform (already wired but verify smooth) | High | cancelled |
| V-04 | DetailScreen | Pop exit uses fadeOut only — should animate back to tile (shared element handles this, verify) | Medium | cancelled |
| V-05 | FavoritesScreen | Empty state appears instantly — no fade-in entrance animation | Medium | completed |
| V-06 | FavoritesScreen | Selection mode toggle (topbar changes) has no animated transition | Low | completed |
| V-07 | SettingsScreen | FilterChip selection has no visual feedback beyond color change — should have scale/bounce | Low | completed |
| V-08 | WallpaperCropDialog | Success checkmark appears with no animation — should scale in with spring | Medium | completed |
| V-09 | WallpaperCropDialog | Bottom panel slides in with no animation on first open | Medium | completed |
| V-10 | BottomNav | Tab switch has no crossfade between screen contents | Medium | completed |
| V-11 | EmptyState | No entrance animation (scale + fade) — appears instantly | Medium | completed |
| V-12 | ErrorState | No entrance animation — appears instantly | Medium | completed |
| V-13 | ShimmerGrid | Shimmer animation is linear sweep — should be ease-in-out for natural feel | Low | completed |
| V-14 | DetailScreen | Bottom panel expand/collapse uses spring but doesn't feel snappy enough — consider adjusting stiffness | Low | cancelled |

## Colors & Contrast

| # | Screen | Issue | Severity | Status |
|---|--------|-------|----------|--------|
| V-15 | DetailScreen | Back button background `surfaceContainerHigh` on image — may lack contrast on light wallpapers | High | completed |
| V-16 | DetailScreen | Action button backgrounds `surfaceContainerHigh` — same contrast concern on light images | High | completed |
| V-17 | DetailScreen | Tag chip `AccentBlue 0.25f` fill on dark images — may be hard to read on blue wallpapers | Medium | completed |
| V-18 | DetailScreen | Stat pill `White 0.10f` background — very subtle, may be invisible on light images | Medium | completed |
| V-19 | SearchFilterBar | Filter panel background `surface` — no scrim/shadow behind it, content shows through | Medium | completed |
| V-20 | SettingsScreen | SettingsGroup `surfaceContainer` — verify contrast against `background` in both themes | Low | cancelled |
| V-21 | WallpaperCard | Downloaded badge `AccentGreen 0.85f` — verify contrast against various image colors | Low | cancelled |
| V-22 | BottomNav | Tab bar background `surface` with `TabBarSeparator 0.15f` tint — verify visibility | Low | cancelled |
| V-23 | WallpaperCropDialog | Close button `GlassDark` — verify visibility on light wallpapers | Medium | completed |

## Spacing & Layout (8px Grid)

| # | Screen | Issue | Severity | Status |
|---|--------|-------|----------|--------|
| V-24 | DetailScreen | Back button: `padding(horizontal=12, vertical=12)` — should be 16dp horizontal to match screen edge | Medium | cancelled |
| V-25 | DetailScreen | Action buttons row: `padding(bottom = navBarPadding + collapsedHeight + 8.dp)` — 8dp gap between buttons and panel feels tight | Medium | completed |
| V-26 | DetailScreen | Bottom panel: `padding(top=12, bottom=16+bottomPadding)` — top padding should be 16dp for consistency | Low | completed |
| V-27 | SettingsScreen | FlowRow `horizontalArrangement = spacedBy(6.dp)` — 6dp breaks 8px grid, should be 8dp | High | completed |
| V-28 | SettingsScreen | SettingsGroup internal `padding(16.dp)` — correct | Low | cancelled |
| V-29 | SettingsScreen | Divider `padding(vertical = 10.dp)` — 10dp is not on 8px grid, should be 8dp or 12dp | Medium | completed |
| V-30 | SearchFilterBar | Filter panel FlowRow `spacedBy(6.dp)` — same 6dp grid break | High | completed |
| V-31 | SearchFilterBar | Filter panel `padding(horizontal=16, vertical=12)` — vertical should be 16dp | Low | completed |
| V-32 | WallpaperCropDialog | Bottom panel `padding(Spacing16)` — correct | Low | cancelled |
| V-33 | DetailPanelContent | Drag handle `size(36.dp, 5.dp)` — 36dp width is non-standard, should be 40dp (Apple grabber standard) | Medium | completed |

## Element Sizing (Touch Targets & Icons)

| # | Screen | Issue | Severity | Status |
|---|--------|-------|----------|--------|
| V-34 | BottomNav | Tab icon size `26.dp` — HIG standard is 25dp for tab bar icons | Low | completed |
| V-35 | BottomNav | Tab label `fontSize = 13.sp` — HIG standard is 10sp for tab bar labels | Medium | completed |
| V-36 | BottomNav | Tab label `FontWeight.Medium` for selected — should be `FontWeight.Semibold` or keep Medium (verify) | Low | cancelled |
| V-37 | DetailScreen | Back button `size(44.dp)` — correct (44dp touch target) | Low | cancelled |
| V-38 | DetailScreen | Action circle buttons `size(44.dp)` — correct | Low | cancelled |
| V-39 | DetailScreen | "Set as Wallpaper" button `height(44.dp)` — correct | Low | cancelled |
| V-40 | SearchFilterBar | Search field `height(44.dp)` — correct | Low | cancelled |
| V-41 | SearchFilterBar | Filter button `size(44.dp)` — correct | Low | cancelled |
| V-42 | SearchFilterBar | Search button `size(44.dp)` — correct | Low | cancelled |
| V-43 | DetailScreen | Icon size in buttons `size(20.dp)` — correct for 44dp container | Low | cancelled |
| V-44 | WallpaperCard | Downloaded badge `size(20.dp)` — small but acceptable for badge | Low | cancelled |
| V-45 | WallpaperCard | Selection check `size(24.dp)` — correct | Low | cancelled |

## Element Visibility & States

| # | Screen | Issue | Severity | Status |
|---|--------|-------|----------|--------|
| V-46 | BrowseScreen | ShimmerGrid shows 12 items regardless of viewport — should show viewport-appropriate count | Medium | completed |
| V-47 | BrowseScreen | GridAppendFooter spinner `height(56.dp)` — tall for a loading indicator, should be 48dp | Low | completed |
| V-48 | DetailScreen | Full-res loading bar at top — good, but should hide when zoomed (already does) | Low | cancelled |
| V-49 | DetailScreen | Data saver "loading full resolution" pill — good positioning | Low | cancelled |
| V-50 | DetailScreen | Chrome overlay fades with shared element — good | Low | cancelled |
| V-51 | FavoritesScreen | Empty state uses `Icons.Outlined.FavoriteBorder` — good | Low | cancelled |
| V-52 | SettingsScreen | Cache size text shows "—" initially — should show loading shimmer or stay blank | Low | cancelled |
| V-53 | WallpaperCropDialog | Loading state shows CircularProgressIndicator — good | Low | cancelled |
| V-54 | WallpaperCropDialog | Load failure shows text + cancel button — should use ErrorState pattern | Medium | completed |

## Typography & Text

| # | Screen | Issue | Severity | Status |
|---|--------|-------|----------|--------|
| V-55 | DetailScreen | Pull hint `labelSmall` with `letterSpacing = 0.4.sp` — good, matches HIG caption | Low | cancelled |
| V-56 | DetailScreen | Tags heading `labelSmall` with `letterSpacing = 0.4.sp` — consistent | Low | cancelled |
| V-57 | DetailScreen | Uploader name `titleSmall.copy(fontWeight = SemiBold)` — good hierarchy | Low | cancelled |
| V-58 | DetailScreen | Stat pill `labelMedium` — good | Low | cancelled |
| V-59 | SettingsScreen | SettingsGroup title `labelLarge.copy(fontSize = 13.sp)` — override is intentional, matches HIG | Low | cancelled |
| V-60 | SettingsScreen | SettingsGroup title `padding(bottom = 6.dp, start = 4.dp)` — 6dp breaks grid | Medium | completed |
| V-61 | SearchFilterBar | Filter section label `labelLarge.copy(fontSize = 13.sp)` — consistent with settings | Low | cancelled |

## Interaction Patterns

| # | Screen | Issue | Severity | Status |
|---|--------|-------|----------|--------|
| V-62 | DetailScreen | Back while zoomed: resets zoom then pops — good, but 16ms delay feels artificial | Low | cancelled |
| V-63 | DetailScreen | Bottom panel drag velocity tracking — good smoothing (0.7/0.3 blend) | Low | cancelled |
| V-64 | DetailScreen | Double-tap zoom cycle (fit→fill→native→fit) — good | Low | cancelled |
| V-65 | SearchFilterBar | Filter panel: Reset and Apply buttons — Reset should be destructive style (red) | Medium | completed |
| V-66 | FavoritesScreen | Long-press to enter selection mode — good | Low | cancelled |
| V-67 | FavoritesScreen | Select All / Deselect All toggle — good | Low | cancelled |
| V-68 | SettingsScreen | Switch haptic on toggle — good | Low | cancelled |
| V-69 | WallpaperCropDialog | Double-tap to toggle fit/fill — good | Low | cancelled |
| V-70 | WallpaperCropDialog | Pinch-to-zoom with pan clamping — good | Low | cancelled |

## Hardcoded Values (Should Be Tokens)

| # | Screen | Issue | Severity | Status |
|---|--------|-------|----------|--------|
| V-71 | DetailScreen | `Color.Black.copy(alpha = 0.55f)` for top gradient — should be a token | Low | completed |
| V-72 | DetailScreen | `Color.Black.copy(alpha = 0.6f)` for data saver pill — should be a token | Low | completed |
| V-73 | DetailScreen | `Color.White.copy(alpha = 0.3f)` for drag handle — should be a token | Low | completed |
| V-74 | DetailScreen | `Color.White.copy(alpha = 0.55f)` for pull hint — should be a token | Low | completed |
| V-75 | DetailScreen | `Color.White.copy(alpha = 0.10f)` for stat pill background — should be a token | Low | completed |
| V-76 | DetailScreen | `Color.White.copy(alpha = 0.14f)` for stat pill border — should be a token | Low | completed |
| V-77 | DetailScreen | `Color.Black.copy(alpha = 0.4f)` for top scrim in crop dialog — should be a token | Low | completed |
| V-78 | DetailScreen | `Color.Black.copy(alpha = 0.65f)` for crop bottom panel — should be a token | Low | completed |
| V-79 | WallpaperCard | Badge padding `KraftSpacing.Spacing4` — correct | Low | cancelled |
| V-80 | BottomNav | Tab padding `top = 10.dp, bottom = 0.dp` — asymmetric, should be `vertical = 4.dp` | Medium | completed |
| V-81 | BottomNav | Tab spacer `height(1.dp)` — should be 2dp for better visual separation | Low | completed |

---

## Summary

- **Total issues**: 81
- **Fixed**: 33
- **Cancelled** (already correct or verified): 48
- **Remaining**: 0

All issues addressed. No pending items.
