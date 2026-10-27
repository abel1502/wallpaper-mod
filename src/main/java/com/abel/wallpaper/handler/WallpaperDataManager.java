package com.abel.wallpaper.handler;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.abel.wallpaper.WallpaperMod;
import com.abel.wallpaper.network.IBlockPacket;
import com.abel.wallpaper.world.IHasWallpaperTable;
import com.abel.wallpaper.world.PaletteEntry;
import com.abel.wallpaper.world.WallpaperTable;
import com.falsepattern.chunk.api.DataManager;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.world.chunk.Chunk;

public final class WallpaperDataManager implements DataManager.ChunkDataManager, DataManager.PacketDataManager, DataManager.BlockPacketDataManager {
    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public String newInstallDescription() {
        return null;  // Compatible with vanilla saves
    }

    @Override
    public String uninstallMessage() {
        return "Abel's Wallpaper Mod has been removed. Wallpaper data should theoretically be retained in the savefile.";
    }

    @Override
    public String versionChangeMessage(String priorVersion) {
        // Only 1.0 exists for now.
        return null;
    }

    @Override
    public String domain() {
        return WallpaperMod.MODID;
    }

    @Override
    public String id() {
        return "wallpaper";
    }

    @Override
    public void writeChunkToNBT(Chunk chunk, NBTTagCompound nbt) {
        WallpaperTable wpt = IHasWallpaperTable.of(chunk).getWallpaperTable();
        if (wpt != null) {
            wpt.writeToNBT(nbt);
        }
    }

    @Override
    public void readChunkFromNBT(Chunk chunk, NBTTagCompound nbt) {
        // Note: nbt may be null or empty. This is correctly handled by WallpaperTable.readFromNBT
        WallpaperTable wpt = IHasWallpaperTable.of(chunk).getWallpaperTable();
        if (wpt != null) {
            wpt.readFromNBT(nbt);
        }
    }

    @Override
    public void cloneChunk(Chunk from, Chunk to) {
        IHasWallpaperTable.of(to).setWallpaperTable(IHasWallpaperTable.of(from).getWallpaperTable());
    }

    @Override
    public int maxPacketSize() {
        return WallpaperTable.MAX_BUFFER_SIZE;
    }

    @Override
    public void writeToBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, ByteBuffer buffer) {
        IHasWallpaperTable.of(chunk).getWallpaperTable().writeToBuffer(buffer);
    }

    @Override
    public void readFromBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, ByteBuffer buffer) {
        WallpaperTable wpt = new WallpaperTable();
        wpt.readFromBuffer(buffer);

        if (wpt.empty()) {
            wpt = null;
        }
        IHasWallpaperTable.of(chunk).setWallpaperTable(wpt);
    }

    @Override
    public void writeBlockToPacket(Chunk chunk, int x, int y, int z, S23PacketBlockChange packet) {
        // Note: x and z are local
        WallpaperTable wpt = IHasWallpaperTable.of(chunk).getWallpaperTable();
        if (wpt == null) {
            return;
        }

        IBlockPacket myPacket = (IBlockPacket)packet;
        PaletteEntry[] faceWallpapers = myPacket.abel$getFaceWallpapers();
        for (int face = 0; face < 6; face++) {
            faceWallpapers[face] = wpt.getWallpaperEntry(x, y, z, face);
        }
    }

    @Override
    public void readBlockFromPacket(Chunk chunk, int x, int y, int z, S23PacketBlockChange packet) {
        IBlockPacket myPacket = (IBlockPacket)packet;
        PaletteEntry[] faceWallpapers = myPacket.abel$getFaceWallpapers();

        boolean hasWallpaper = false;
        for (int face = 0; face < 6; face++) {
            if (faceWallpapers[face] != null) {
                hasWallpaper = true;
                break;
            }
        }
        if (!hasWallpaper) {
            return;
        }

        WallpaperTable wpt = IHasWallpaperTable.of(chunk).getWallpaperTable();
        if (wpt == null) {
            wpt = new WallpaperTable();
            IHasWallpaperTable.of(chunk).setWallpaperTable(wpt);
        }

        for (int face = 0; face < 6; face++) {
            wpt.setWallpaperEntry(x, y, z, face, faceWallpapers[face]);
            // Note: no render updates scheduled here. Render update should happen due to the block update in the first place, right?
        }

        if (wpt.empty()) {
            IHasWallpaperTable.of(chunk).setWallpaperTable(null);
        }
    }

    @Override
    public void writeBlockPacketToBuffer(S23PacketBlockChange packet, PacketBuffer buffer) throws IOException {
        IBlockPacket myPacket = (IBlockPacket)packet;
        PaletteEntry[] faceWallpapers = myPacket.abel$getFaceWallpapers();

        short mask = 0;
        for (int face = 0; face < 6; face++) {
            if (faceWallpapers[face] != null) {
                mask |= 1 << face;
            }
        }

        buffer.writeShort(mask);
        for (int face = 0; face < 6; face++) {
            if (faceWallpapers[face] != null) {
                faceWallpapers[face].writeToBuffer(buffer);
            }
        }
    }

    @Override
    public void readBlockPacketFromBuffer(S23PacketBlockChange packet, PacketBuffer buffer) throws IOException {
        IBlockPacket myPacket = (IBlockPacket)packet;
        PaletteEntry[] faceWallpapers = myPacket.abel$getFaceWallpapers();

        short mask = buffer.readShort();
        for (int face = 0; face < 6; face++) {
            if ((mask & (1 << face)) != 0) {
                PaletteEntry paletteEntry = new PaletteEntry();
                paletteEntry.readFromBuffer(buffer);
                faceWallpapers[face] = paletteEntry;
            } else {
                faceWallpapers[face] = null;
            }
        }
    }
}
