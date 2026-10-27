package com.abel.wallpaper.handler;

import static com.abel.wallpaper.WallpaperMod.LOG;

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
        LOG.debug("[ABEL] WallpaperDataManager.writeChunkToNBT chunk.x={} chunk.z={} world.isRemote={}", chunk.xPosition, chunk.zPosition, chunk.worldObj.isRemote);
        WallpaperTable wpt = IHasWallpaperTable.of(chunk).getWallpaperTable();
        if (wpt != null) {
            wpt.writeToNBT(nbt);
        }
    }

    @Override
    public void readChunkFromNBT(Chunk chunk, NBTTagCompound nbt) {
        LOG.debug("[ABEL] WallpaperDataManager.readChunkFromNBT chunk.x={} chunk.z={} world.isRemote={}", chunk.xPosition, chunk.zPosition, chunk.worldObj.isRemote);
        // Note: nbt may be null or empty. This is correctly handled by WallpaperTable.readFromNBT
        WallpaperTable wpt = new WallpaperTable();
        wpt.readFromNBT(nbt);

        if (wpt.empty()) {
            wpt = null;
        }
        IHasWallpaperTable.of(chunk).setWallpaperTable(wpt);
    }

    @Override
    public void cloneChunk(Chunk from, Chunk to) {
        LOG.debug("[ABEL] WallpaperDataManager.cloneChunk from.x={} from.z={} to.x={} to.z={} world.isRemote={}", from.xPosition, from.zPosition, to.xPosition, to.zPosition, from.worldObj.isRemote);
        IHasWallpaperTable.of(to).setWallpaperTable(IHasWallpaperTable.of(from).getWallpaperTable());
    }

    @Override
    public int maxPacketSize() {
        return WallpaperTable.MAX_BUFFER_SIZE;
    }

    @Override
    public void writeToBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, ByteBuffer buffer) {
        LOG.debug("[ABEL] WallpaperDataManager.writeToBuffer chunk.x={} chunk.z={} world.isRemote={} forceUpdate={}", chunk.xPosition, chunk.zPosition, chunk.worldObj.isRemote, forceUpdate);

        WallpaperTable wpt = IHasWallpaperTable.of(chunk).getWallpaperTable();
        if (wpt == null) {
            return;
        }

        wpt.writeToBuffer(buffer);
    }

    @Override
    public void readFromBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, ByteBuffer buffer) {
        LOG.debug("[ABEL] WallpaperDataManager.readFromBuffer chunk.x={} chunk.z={} world.isRemote={} forceUpdate={}", chunk.xPosition, chunk.zPosition, chunk.worldObj.isRemote, forceUpdate);

        WallpaperTable wpt;
        if (buffer.hasRemaining()) {
            wpt = new WallpaperTable();
            wpt.readFromBuffer(buffer);

            if (wpt.empty()) {
                wpt = null;
            }
        } else {
            wpt = null;
        }

        IHasWallpaperTable.of(chunk).setWallpaperTable(wpt);

        if (chunk.worldObj.isRemote) {
            int sectionX = chunk.xPosition << 4;
            int sectionZ = chunk.xPosition << 4;
            chunk.worldObj.markBlockRangeForRenderUpdate(
                sectionX, 0, sectionZ,
                sectionX + 15, 255, sectionZ + 15
            );
            // WallpaperHandler.onWallpaperUpdate(chunk.worldObj, chunk.xPosition << 4, 0, chunk.zPosition << 4);
        }
    }

    @Override
    public void writeBlockToPacket(Chunk chunk, int x, int y, int z, S23PacketBlockChange packet) {
        LOG.debug("[ABEL] WallpaperDataManager.writeBlockToPacket x={} y={} z={} world.isRemote={}", x, y, z, chunk.worldObj.isRemote);
        // Note: x and z are local

        IBlockPacket myPacket = (IBlockPacket)packet;
        PaletteEntry[] faceWallpapers = myPacket.abel$getFaceWallpapers();

        WallpaperTable wpt = IHasWallpaperTable.of(chunk).getWallpaperTable();
        if (wpt == null) {
            for (int face = 0; face < 6; face++) {
                faceWallpapers[face] = null;
            }

            return;
        }

        LOG.debug("[ABEL] (WallpaperDataManager.readBlockFromPacket) faceWallpapers=[{} {} {} {} {} {}]", faceWallpapers[0], faceWallpapers[1], faceWallpapers[2], faceWallpapers[3], faceWallpapers[4], faceWallpapers[5]);

        for (int face = 0; face < 6; face++) {
            faceWallpapers[face] = wpt.getWallpaperEntry(x, y, z, face);
        }
    }

    @Override
    public void readBlockFromPacket(Chunk chunk, int x, int y, int z, S23PacketBlockChange packet) {
        LOG.debug("[ABEL] WallpaperDataManager.readBlockFromPacket x={} y={} z={} world.isRemote={}", x, y, z, chunk.worldObj.isRemote);
        // Note: x and z are local

        IBlockPacket myPacket = (IBlockPacket)packet;
        PaletteEntry[] faceWallpapers = myPacket.abel$getFaceWallpapers();

        LOG.debug("[ABEL] (WallpaperDataManager.readBlockFromPacket) faceWallpapers=[{} {} {} {} {} {}]", faceWallpapers[0], faceWallpapers[1], faceWallpapers[2], faceWallpapers[3], faceWallpapers[4], faceWallpapers[5]);

        boolean hasWallpaper = false;
        for (int face = 0; face < 6; face++) {
            if (faceWallpapers[face] != null) {
                hasWallpaper = true;
                break;
            }
        }

        WallpaperTable wpt = IHasWallpaperTable.of(chunk).getWallpaperTable();
        if (wpt == null) {
            LOG.debug("[ABEL] (WallpaperDataManager.readBlockFromPacket) wpt=null hasWallpaper={}", hasWallpaper);
            if (!hasWallpaper) {
                return;
            }

            wpt = new WallpaperTable();
            IHasWallpaperTable.of(chunk).setWallpaperTable(wpt);
        }

        LOG.debug("[ABEL] (WallpaperDataManager.readBlockFromPacket) wpt={} faceWallpapers=[{} {} {} {} {} {}]", wpt, faceWallpapers[0], faceWallpapers[1], faceWallpapers[2], faceWallpapers[3], faceWallpapers[4], faceWallpapers[5]);

        boolean changed = false;
        for (int face = 0; face < 6; face++) {
            changed |= wpt.setWallpaperEntry(x, y, z, face, faceWallpapers[face]);
            // Note: no render updates scheduled here. Render update should happen due to the block update in the first place, right?
        }

        if (wpt.empty()) {
            IHasWallpaperTable.of(chunk).setWallpaperTable(null);
            wpt = null;
        }

        if (changed) {
            int sectionX = chunk.xPosition << 4;
            int sectionY = y >> 4 << 4;
            int sectionZ = chunk.zPosition << 4;
            chunk.worldObj.markBlockRangeForRenderUpdate(
                sectionX, sectionY, sectionZ,
                sectionX + 15, sectionY + 15, sectionZ + 15
            );
            // WallpaperHandler.onWallpaperUpdate(chunk.worldObj, x, y, z);
        }
    }

    @Override
    public void writeBlockPacketToBuffer(S23PacketBlockChange packet, PacketBuffer buffer) throws IOException {
        LOG.debug("[ABEL] WallpaperDataManager.writeBlockPacketToBuffer");

        IBlockPacket myPacket = (IBlockPacket)packet;
        PaletteEntry[] faceWallpapers = myPacket.abel$getFaceWallpapers();

        LOG.debug("[ABEL] (WallpaperDataManager.writeBlockPacketToBuffer) faceWallpapers=[{} {} {} {} {} {}]", faceWallpapers[0], faceWallpapers[1], faceWallpapers[2], faceWallpapers[3], faceWallpapers[4], faceWallpapers[5]);

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
        LOG.debug("[ABEL] WallpaperDataManager.readBlockPacketFromBuffer");

        IBlockPacket myPacket = (IBlockPacket)packet;
        PaletteEntry[] faceWallpapers = myPacket.abel$getFaceWallpapers();

        LOG.debug("[ABEL] (WallpaperDataManager.readBlockPacketFromBuffer) faceWallpapers=[{} {} {} {} {} {}]", faceWallpapers[0], faceWallpapers[1], faceWallpapers[2], faceWallpapers[3], faceWallpapers[4], faceWallpapers[5]);

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
