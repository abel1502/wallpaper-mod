package com.abel.wallpaper;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@LateMixin
public class LateMixins implements ILateMixinLoader {
    @Override
    public String getMixinConfig() {
        return "mixins.abel_wallpaper.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        ArrayList<String> mixins = new ArrayList<>();

        if (loadedMods.contains("angelica")) {
            mixins.add("angelica.MixinClonedChunkSection");
            mixins.add("angelica.MixinWorldSlice");
        }

        if (loadedMods.contains("hbm")) {
            mixins.add("hbm.MixinCTContext");
            mixins.add("hbm.MixinRenderBlocksCT");
        }

        if (loadedMods.contains("chisel")) {
            mixins.add("chisel.MixinCTM");
            mixins.add("chisel.MixinRenderBlocksCTM");
        }

        return mixins;
    }
}
