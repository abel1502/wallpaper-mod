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

        return mixins;
    }
}
