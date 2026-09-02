# Visual & Interaction Audit — WallKraft

Every issue follows Apple HIG as the design DNA. Same rigor as the code audit.

**Status**: `pending` = not started, `in_progress` = fixing, `completed` = fixed, `cancelled` = deferred

---

## Animation & Transitions

| # | Screen | Issue | Severity | Status |
|---|--------|-------|----------|--------|
| V-01 | BrowseScreen | No AnimatedContent for state transitions (loading → grid → error → empty) — hard switch reads as jarring | High | pending |
| V-02 | BrowseScreen | RateLimitBanner appears/disappears instantly — no enter/exit animation | Medium | pending |
| V-03 | DetailScreen | DetailScreen enter transition is None — should have shared element container-transform (already wired but verify smooth) | High | pending |
| V-04 | DetailScreen | Pop exit uses fadeOut only — should animate back to tile (shared element handles this, verify) | Medium | pending |
| V-05 | FavoritesScreen | Empty state appears instantly — no fade-in entrance animation | Medium | pending |
| V-06 | FavoritesScreen | Selection mode toggle (topbar changes) has no animated transition | Low | pending |
| V-07 | SettingsScreen | FilterChip selection has no visual feedback beyond color change — should have scale/bounce | Low | pending |
| V-08 | WallpaperCropDialog | Success checkmark appears with no animation — should scale in with spring | Medium | pending |
| V-09 | WallpaperCropDialog | Bottom panel slides in with no animation on first open | Medium | pending |
| V-10 | BottomNav | Tab switch has no crossfade between screen contents | Medium | pending |
| V-11 | EmptyState | No entrance animation (scale + fade) — appears instantly | Medium | pending |
| V-12 | ErrorState | No entrance animation — appears instantly | Medium | pending |
| V-13 | ShimmerGrid | Shimmer animation is linear sweep — should be ease-in-out for natural feel | Low | pending |
| V-14 | DetailScreen | Bottom panel expand/collapse uses spring but doesn't feel snappy enough — consider adjusting stiffness | Low | pending |

## Colors & Contrast

| # | Screen | Issue | Severity | Status |
|---|--------|-------|----------|--------|
| V-15 | DetailScreen | Back button background `surfaceContainerHigh` on image — may lack contrast on light wallpapers | High | pending |
| V-16 | DetailScreen | Action button backgrounds `surfaceContainerHigh` — same contrast concern on light images | High | pending |
| V-17 | DetailScreen | Tag chip `AccentBlue 0.25f` fill on dark images — may be hard to read on blue wallpapers | Medium | pending |
| V-18 | DetailScreen | Stat pill `White 0.10f` background — very subtle, may be invisible on light images | Medium | pending |
| V-19 | SearchFilterBar | Filter panel background `surface` — no scrim/shadow behind it, content shows through | Medium | pending |
| V-20 | SettingsScreen | SettingsGroup `surfaceContainer` — verify contrast against `background` in both themes | Low | pending |
| V-21 | WallpaperCard | Downloaded badge `AccentGreen 0.85f` — verify contrast against various image colors | Low | pending |
| V-22 | BottomNav | Tab bar background `surface` with `TabBarSeparator 0.15f` tint — verify visibility | Low | pending |
| V-23 | WallpaperCropDialog | Close button `GlassDark` — verify visibility on light wallpapers | Medium | pending |

## Spacing & Layout (8px Grid)

| # | Screen | Issue | Severity | Status |
|---|--------|-------|----------|--------|
| V-24 | DetailScreen | Back button: `padding(horizontal=12, vertical=12)` — should be 16dp horizontal to match screen edge | Medium | pending |
| V-25 | DetailScreen | Action buttons row: `padding(bottom = navBarPadding + collapsedHeight + 8.dp)` — 8dp gap between buttons and panel feels tight | Medium | pending |
| V-26 | DetailScreen | Bottom panel: `padding(top=12, bottom=16+bottomPadding)` — top padding should be 16dp for consistency | Low | pending |
| V-27 | SettingsScreen | FlowRow `horizontalArrangement = spacedBy(6.dp)` — 6dp breaks 8px grid, should be 8dp | High | pending |
| V-28 | SettingsScreen | SettingsGroup internal `padding(16.dp)` — correct | Low | pending |
| V-29 | SettingsScreen | Divider `padding(vertical = 10.dp)` — 10dp is not on 8px grid, should be 8dp or 12dp | Medium | pending |
| V-30 | SearchFilterBar | Filter panel FlowRow `spacedBy(6.dp)` — same 6dp grid break | High | pending |
| V-31 | SearchFilterBar | Filter panel `padding(horizontal=16, vertical=12)` — vertical should be 16dp | Low | pending |
| V-32 | WallpaperCropDialog | Bottom panel `padding(Spacing16)` — correct | Low | pending |
| V-33 | DetailPanelContent | Drag handle `size(36.dp, 5.dp)` — 36dp width is non-standard, should be 40dp (Apple grabber standard) | Medium | pending |

## Element Sizing (Touch Targets & Icons)

| # | Screen | Issue | Severity | Status |
|---|--------|-------|----------|--------|
| V-34 | BottomNav | Tab icon size `26.dp` — HIG standard is 25dp for tab bar icons | Low | pending |
| V-35 | BottomNav | Tab label `fontSize = 13.sp` — HIG standard is 10sp for tab bar labels | Medium | pending |
| V-36 | BottomNav | Tab label `FontWeight.Medium` for selected — should be `FontWeight.Semibold` or keep Medium (verify) | Low | pending |
| V-37 | DetailScreen | Back button `size(44.dp)` — correct (44dp touch target) | Low | pending |
| V-38 | DetailScreen | Action circle buttons `size(44.dp)` — correct | Low | pending |
| V-39 | DetailScreen | "Set as Wallpaper" button `height(44.dp)` — correct | Low | pending |
| V-40 | SearchFilterBar | Search field `height(44.dp)` — correct | Low | pending |
| V-41 | SearchFilterBar | Filter button `size(44.dp)` — correct | Low | pending |
| V-42 | SearchFilterBar | Search button `size(44.dp)` — correct | Low | pending |
| V-43 | DetailScreen | Icon size in buttons `size(20.dp)` — correct for 44dp container | Low | pending |
| V-44 | WallpaperCard | Downloaded badge `size(20.dp)` — small but acceptable for badge | Low | pending |
| V-45 | WallpaperCard | Selection check `size(24.dp)` — correct | Low | pending |

## Element Visibility & States

| # | Screen | Issue | Severity | Status |
|---|--------|-------|----------|--------|
| V-46 | BrowseScreen | ShimmerGrid shows 12 items regardless of viewport — should show viewport-appropriate count | Medium | pending |
| V-47 | BrowseScreen | GridAppendFooter spinner `height(56.dp)` — tall for a loading indicator, should be 48dp | Low | pending |
| V-48 | DetailScreen | Full-res loading bar at top — good, but should hide when zoomed (already does) | Low | pending |
| V-49 | DetailScreen | Data saver "loading full resolution" pill — good positioning | Low | pending |
| V-50 | DetailScreen | Chrome overlay fades with shared element — good | Low | pending |
| V-51 | FavoritesScreen | Empty state uses `Icons.Outlined.FavoriteBorder` — good | Low | pending |
| V-52 | SettingsScreen | Cache size text shows "—" initially — should show loading shimmer or stay blank | Low | pending |
| V-53 | WallpaperCropDialog | Loading state shows CircularProgressIndicator — good | Low | pending |
| V-54 | WallpaperCropDialog | Load failure shows text + cancel button — should use ErrorState pattern | Medium | pending |

## Typography & Text

| # | Screen | Issue | Severity | Status |
|---|--------|-------|----------|--------|
| V-55 | DetailScreen | Pull hint `labelSmall` with `letterSpacing = 0.4.sp` — good, matches HIG caption | Low | pending |
| V-56 | DetailScreen | Tags heading `labelSmall` with `letterSpacing = 0.4.sp` — consistent | Low | pending |
| V-57 | DetailScreen | Uploader name `titleSmall.copy(fontWeight = SemiBold)` — good hierarchy | Low | pending |
| V-58 | DetailScreen | Stat pill `labelMedium` — good | Low | pending |
| V-59 | SettingsScreen | SettingsGroup title `labelLarge.copy(fontSize = 13.sp)` — override is intentional, matches HIG | Low | pending |
| V-60 | SettingsScreen | SettingsGroup title `padding(bottom = 6.dp, start = 4.dp)` — 6dp breaks grid | Medium | pending |
| V-61 | SearchFilterBar | Filter section label `labelLarge.copy(fontSize = 13.sp)` — consistent with settings | Low | pending |

## Interaction Patterns

| # | Screen | Issue | Severity | Status |
|---|--------|-------|----------|--------|
| V-62 | DetailScreen | Back while zoomed: resets zoom then pops — good, but 16ms delay feels artificial | Low | pending |
| V-63 | DetailScreen | Bottom panel drag velocity tracking — good smoothing (0.7/0.3 blend) | Low | pending |
| V-64 | DetailScreen | Double-tap zoom cycle (fit→fill→native→fit) — good | Low | pending |
| V-65 | SearchFilterBar | Filter panel: Reset and Apply buttons — Reset should be destructive style (red) | Medium | pending |
| V-66 | FavoritesScreen | Long-press to enter selection mode — good | Low | pending |
| V-67 | FavoritesScreen | Select All / Deselect All toggle — good | Low | pending |
| V-68 | SettingsScreen | Switch haptic on toggle — good | Low | pending |
| V-69 | WallpaperCropDialog | Double-tap to toggle fit/fill — good | Low | pending |
| V-70 | WallpaperCropDialog | Pinch-to-zoom with pan clamping — good | Low | pending |

## Hardcoded Values (Should Be Tokens)

| # | Screen | Issue | Severity | Status |
|---|--------|-------|----------|--------|
| V-71 | DetailScreen | `Color.Black.copy(alpha = 0.55f)` for top gradient — should be a token | Low | pending |
| V-72 | DetailScreen | `Color.Black.copy(alpha = 0.6f)` for data saver pill — should be a token | Low | pending |
| V-73 | DetailScreen | `Color.White.copy(alpha = 0.3f)` for drag handle — should be a token | Low | pending |
| V-74 | DetailScreen | `Color.White.copy(alpha = 0.55f)` for pull hint — should be a token | Low | pending |
| V-75 | DetailScreen | `Color.White.copy(alpha = 0.10f)` for stat pill background — should be a token | Low | pending |
| V-76 | DetailScreen | `Color.White.copy(alpha = 0.14f)` for stat pill border — should be a token | Low | pending |
| V-77 | DetailScreen | `Color.Black.copy(alpha = 0.4f)` for top scrim in crop dialog — should be a token | Low | pending |
| V-78 | DetailScreen | `Color.Black.copy(alpha = 0.65f)` for crop bottom panel — should be a token | Low | pending |
| V-79 | WallpaperCard | Badge padding `KraftSpacing.Spacing4` — correct | Low | pending |
| V-80 | BottomNav | Tab padding `top = 10.dp, bottom = 0.dp` — asymmetric, should be `vertical = 4.dp` | Medium | pending |
| V-81 | BottomNav | Tab spacer `height(1.dp)` — should be 2dp for better visual separation | Low | pending |

---

## Summary

- **Total issues**: 81
- **High**: 6 (contrast issues on detail screen overlays, grid spacing breaks)
- **Medium**: 24 (animations, spacing, element sizing, typography)
- **Low**: 51 (token extraction, minor tweaks, already-correct values)
- **Cancelled**: 0

## Priority Order

1. **High contrast issues** (V-15, V-16) — detail screen overlays need glass treatment or better alpha
2. **Grid spacing breaks** (V-27, V-30, V-29, V-33, V-60, V-80) — 6dp → 8dp
3. **Missing animations** (V-01, V-02, V-05, V-08, V-09, V-10, V-11, V-12) — state transitions need motion
4. **Typography** (V-35) — tab bar labels 13sp → 10sp
5. **Token extraction** (V-71 through V-78) — hardcoded alphas → constants
6. **Minor tweaks** (everything else)
