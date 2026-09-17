package com.abel.wallpaper.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import com.abel.wallpaper.world.IExtendedBlockAccess;
import com.gtnewhorizons.angelica.rendering.celeritas.world.WorldSlice;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.chunk.Chunk;

@Pseudo
@Mixin(WorldSlice.class)
public abstract class MixinWorldSlice implements IExtendedBlockAccess {
    @Shadow(remap = false)
    private final WorldClient world = null;

    @Override
    public Chunk getChunkFromChunkCoords(int chunkX, int chunkZ) {
        // This might not be thread-safe. If so, I'll probably need to change the interface to retrieve the painted faces status directly,
        // then patch WorldSlice to copy it in `prepare` as well.
        // UPD: Yes, that's the case. Otherwise it's always null, it seems.
        try {
            return world.getChunkFromChunkCoords(chunkX, chunkZ);
        } catch (Exception e) {
            // LOG.warn("Error getting chunk from chunk coords in Angelica WorldSlice", e);
            return null;
        }
    }
}
