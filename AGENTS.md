# AGENTS.md

Notes for anyone (human or AI) working on Abel's Wallpaper Mod in a fresh context.
This file is committed; keep it free of anything machine- or user-specific.

## What the mod is

A Minecraft **1.7.10** (Forge 10.13.4.1614) mod that lets you paint a texture from one block
onto an individual face of another block, via a "wallpaper roll" (sampling + applying) and a
"scraper" (removing). It also supports **connected textures (CTM)** on painted faces, and
integrates with HBM's Nuclear Tech Mod and Chisel/CTMLib.

## Building

- Requires **JDK 8**. The build fails on newer JDKs (`Could not determine java version from 'NN'`).
  Point `JAVA_HOME` at a JDK 8 before building. The `just` recipe is the normal entry point:
  `just build` → `./gradlew build`.
- ForgeGradle 1.2 (the `com.anatawa12.forge` fork), Gradle wrapper 4.4.1.
- `just` recipes: `build`, `run` (runClient), `clean`, `deploy`, `deploy-release`.
  (`deploy*` copy `build/libs/*.jar` into a local mods folder; their targets are environment
  specific and shouldn't be relied on elsewhere.)

## Dependencies (all `compileOnly`)

- `com.falsepattern:chunkapi-mc1.7.10` — chunk data persistence (`required-after`).
- `com.github.LegacyModdingMC.UniMixins:unimixins-all` — Mixin loader (`required-after`).
- `curse.maven:angelica` — optional; client rendering (late mixins only when present).
- `mega:fluidlogged-mc1.7.10` — needed at compile time for the patch to angelica.
- `com.hbm:HBM-NTM` — optional; CTM source provider + host rendering. Runtime modid `hbm`.
- `team.chisel.ctmlib:CTMLib` — optional; CTM source provider + host rendering. At runtime
  CTMLib classes are **bundled inside Chisel** (modid `chisel`), not shipped separately.

## Key files

- `world/WallpaperTable.java` — per-chunk sparse map of face → palette entry (open-addressing
  hash table with bit-packed keys, NBT/byte-buffer serialization).
- `world/PaletteEntry.java` — a wallpaper's source: `block + meta + side`, plus a cached `IIcon`.
- `world/IHasFaceOverride.java` / `world/IHasWallpaperTable.java` — mixin interfaces for looking
  up wallpaper data through a `World`/`ChunkCache`/Angelica `WorldSlice`.
- `world/WallpaperCompat.java` — common (side-independent) wallpaper lookup helper used by mixins.
- `handler/WallpaperHandler.java` — decides which blocks can be painted on / used as a source.
- `handler/WallpaperDataManager.java` — ChunkAPI integration (save/load/sync).
- `mixin/MixinRenderBlocks.java` — flat + CTM rendering hooks into `RenderBlocks`.
- `client/ctm/` — all CTM logic: `WallpaperCTM` (connectivity/dispatch), `WallpaperCTMRenderer`
  (face splitting + AO), `HbmCTM` and `CtmlibCTM` (fragment providers).
- `mixin/hbm/` and `mixin/chisel/` — HBM/Chisel integration mixins.
- `LateMixins.java` — conditionally registers the `hbm`/`chisel`/`angelica` mixins.

## Data model

- A chunk stores a `WallpaperTable`. Each entry maps `(localX, y, localZ, face)` → palette index.
- The palette deduplicates `(block, meta, side)` sources; `PaletteEntry.icon` caches
  `block.getIcon(side, meta)`. Call `WallpaperTable.refreshIcons()` after a texture stitch/reload.
- Faces use the vanilla side numbering (0=DOWN, 1=UP, 2=NORTH, 3=SOUTH, 4=WEST, 5=EAST).
- Server-side authority lives in `ItemRoll`/`ItemScraper` `onItemUse`; the client only predicts.

## Rendering

- **Flat wallpaper**: `MixinRenderBlocks.getBlockIcon` overrides the icon per face. This is
  enough for a single texture; non-CTM sources render this way.
- **CTM wallpaper**: a face is split into 4 quadrants (with ambient-occlusion interpolation).
  `WallpaperCTMRenderer` does the geometry (adapted from HBM's `RenderBlocksCT`), hooked via
  `renderFace*` injections.
- Wallpapers only ever apply to full cubes, so `WallpaperCTMRenderer` hardcodes 0..1 bounds
  (do **not** rely on `RenderBlocks.renderMaxX/Y/Z` — HBM's `RenderBlocksCT` never calls
  `setRenderBoundsFromBlock`, leaving them at 0.0).

## CTM architecture

Two fragment providers, both accessed only when the mod is present:

- **HBM** (`IBlockCT`): `getFragments(...)` returns `IIcon[20]`; `canConnect(...)` for real blocks.
- **Chisel/CTMLib** (`ICTMBlock`): compact CTM (4x4 `-ctm` + 2x2 base). Submaps are fetched by
  calling `ISubmapManager.createRenderContext(...)` and reading the public `submap`/`submapSmall`
  fields off the returned `RenderBlocksCTM`.

Connectivity is supported in all three directions, each via a different hook:

- painted ↔ painted / painted → real: `HbmCTM`/`CtmlibCTM` (read the wallpaper table themselves).
- real → painted: `hbm.MixinCTContext` (redirect of `IBlockCT.canConnect`) and
  `chisel.MixinCTM` (inject of `CTM.isConnected`).
- wallpaper rendered *on* a CTM host: `hbm.MixinRenderBlocksCT` / `chisel.MixinRenderBlocksCTM`.

Rule enforced by the connectivity hooks: **a painted neighbor's wallpaper takes precedence over
its underlying block type** (so a deco_steel block painted with planks no longer connects as
deco_steel).

## Mixin conventions (important)

1.7.10 runs with **MCP class names + SRG method names** (only method/field names are obfuscated;
class names stay `net.minecraft.*`).

- Vanilla method: `remap = true` (default) + MCP name (`renderFaceXPos`). The annotation
  processor generates a refmap entry mapping it to the SRG name (`func_147764_f`).
- Mod's *own* method (not an override): `remap = false` + MCP name — it is **not** renamed at
  runtime (`isConnected`, `loadContext`, `canConnect`, …).
- Mod class *overriding* a vanilla method: use `remap = true` (default) + MCP name. The processor
  is inheritance-aware and emits the correct `owner.func_*` refmap entry.

- Use `@Pseudo` on mixins targeting mod classes so they're silently skipped when the mod is absent.
- Conditional mod mixins go through `LateMixins.getMixins(loadedMods)`, listed under the
  `mixins.abel_wallpaper.late.json` config (package `com.abel.wallpaper.mixin`).
- `@Optional.Method(modid = ...)` **removes** the method entirely when the mod is absent, so every
  call site must be guarded with `Loader.isModLoaded(modid)` (short-circuit), and `@Optional`
  references must stay inside those methods.

## Gotchas

- HBM's `CTContext.loadContext` is hooked with a `@Redirect` on `IBlockCT.canConnect` plus
  MixInExtras `@Local` captures of `i`, `x`, `y`, `z`. These local-variable names must exist in
  HBM's bytecode (they do in the current release); a rename there breaks the hook.
- HBM's `BlockEnumMultiCT.getFragments` reads metadata from the *host* position, so `HbmCTM`
  special-cases it and indexes `receivers[sourceMeta]` directly.
- `ItemRoll` `onItemUse` runs on both sides; the client path only predicts, the server path does
  the mutation and plays the sound.

## Known limitations

- Only **compact CTM** (4x4 + 2x2) is rendered; Chisel CTMV/CTMH/V9/V4/R* types fall back to flat.
- CTMLib submaps are obtained via `createRenderContext` + public fields (mutates Chisel's shared
  renderer singleton) — a bit fragile.
- CTMLib connectivity omits the "obscured face" and `IConnectable` checks.
- HBM connectivity hook depends on local-variable names (see above).
- No connectivity across `IBlockAccess` cache boundaries (`ChunkCache`/`WorldSlice` edge).
- Log/quartz `uvRotate*` isn't applied to CTM quadrants.
- Not yet verified on a dedicated server / without Chisel.

## Sounds

Vanilla events only: roll sampling = `step.cloth`; roll applying = `dig.cloth`; scraper =
`dig.cloth` (lower pitch). Played server-side via `World.playSoundEffect` at the block center.
