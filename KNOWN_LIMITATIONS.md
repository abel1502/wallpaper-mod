# Connected Textures — Known Limitations

Notes on the connected-textures (CTM) support added to Abel's Wallpaper Mod.
These are not committed to the repository; kept for later reference.

## Source providers

Two CTM fragment providers are supported, both accessed only when the corresponding
mod is present (guarded with Forge `@Optional`):

- **HBM Nuclear Tech Mod** — blocks implementing `com.hbm.render.block.ct.IBlockCT`
  (`getFragments(...)` returns the `IIcon[20]` fragments, `canConnect(...)` decides
  real-block connectivity).
- **Chisel / CTMLib** — blocks implementing `team.chisel.ctmlib.ICTMBlock`, using the
  *compact* CTM scheme (4x4 `-ctm` texture + 2x2 base texture).

## Connectivity & host rendering

Connectivity is supported in all directions (painted↔painted, painted↔real, real↔painted),
and wallpapers now render *on* CTM host blocks (both flat and connected):

- painted face rendering: `MixinRenderBlocks` (vanilla `RenderBlocks.renderFace*`).
- real block connecting to painted faces: `hbm.MixinCTContext` (redirect of `IBlockCT.canConnect`)
  and `chisel.MixinCTM` (inject of `CTM.isConnected`).
- wallpapers on CTM hosts: `hbm.MixinRenderBlocksCT` and `chisel.MixinRenderBlocksCTM`
  (inject of the `renderFace*` overrides).

## Limitations

1. **Only compact CTM is rendered.**
   Chisel `TextureType` variants other than compact CTM (CTMV, CTMH, V9, V4, R9, R4, R16)
   are not split into connected quadrants; painting those textures falls back to the
   normal flat wallpaper.

2. **CTMLib submap access is a bit of a hack.**
   The 4x4/2x2 submaps are obtained by calling `ISubmapManager.createRenderContext(...)`
   and reading the public `submap`/`submapSmall` fields off the returned
   `RenderBlocksCTM`. This mutates Chisel's shared renderer singleton and relies on the
   public field layout, so it's more fragile than the HBM path.

3. **CTMLib connectivity checks are simplified.**
   The "obscured face" check and `IConnectable.canConnectCTM` from CTMLib's `CTM` are not
   implemented. Connections are plain `block + meta` equality (CTMLib) or `canConnect`
   (HBM), plus the wallpaper-aware matching described above.

4. **HBM connectivity hook depends on local-variable names.**
   `hbm.MixinCTContext` redirects the `IBlockCT.canConnect` call inside
   `CTContext.loadContext`, capturing the locals `i`, `x`, `y`, `z` by name. If HBM renames
   those locals or the `canConnect` signature, the hook stops matching.

5. **No connectivity across render-cache boundaries.**
   Neighbor lookups go through the current `IBlockAccess` (usually a `ChunkCache` or
   Angelica `WorldSlice`). Positions outside that cache resolve to "not connected", so a
   face at the very edge of the cache may temporarily fail to connect until the adjacent
   chunk is also in the cache.

6. **Log/quartz UV rotation is not applied to CTM quadrants.**
   `renderBlockLog`/`renderBlockQuartz` set `uvRotate*`, which the flat wallpaper path
   respects but the CTM quadrant renderer ignores. Only relevant when painting a CTM
   texture onto a log/quartz host face.

7. **Not yet verified on a dedicated server / without Chisel.**
   The `@Optional` stripping and `@SideOnly` class-loading behavior hasn't been tested on
   a dedicated server, or in a client without Chisel installed.
